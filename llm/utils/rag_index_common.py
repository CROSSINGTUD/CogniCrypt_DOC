import hashlib
import io
import json
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple

import faiss
import numpy as np
from pypdf import PdfReader


@dataclass
class DocChunk:
    """Container for one retrievable paper chunk."""

    # Stable ID for RAG references (e.g., "C12")
    id: str
    # Text content of the chunk
    text: str


class EmbeddingIndex:
    """FAISS-backed cosine-similarity index over chunk embeddings."""

    def __init__(self):
        """Initialize an empty in-memory index and aligned id/vector storage."""
        # Parallel arrays of IDs and FAISS vectors
        self.ids: List[str] = []
        self.index = None
        self.vectors = None  # np.ndarray float32

    def build(self, embeddings: np.ndarray, ids: List[str]):
        """
        Build a cosine-similarity FAISS index from embeddings and IDs.

        Invariants enforced here:
        - embeddings is 2D float32 with one row per chunk id.
        - ids length matches row count.
        - empty embedding sets are represented as a valid "no-index" state.
        """
        # copy(): normalize_L2 below works in place, and callers persist the array
        # they passed in as the cached vectors.
        embeddings = np.array(embeddings, dtype="float32", copy=True)
        if embeddings.ndim != 2:
            raise ValueError("Embeddings must be a 2D float32 array.")
        if len(ids) != embeddings.shape[0]:
            raise ValueError("IDs length must match number of embedding rows.")
        if embeddings.shape[0] == 0:
            self.index = None
            self.vectors = embeddings
            self.ids = list(ids)
            return
        if embeddings.shape[1] == 0:
            raise ValueError("Embedding dimension must be greater than 0.")
        # Normalize for cosine similarity using inner product index
        faiss.normalize_L2(embeddings)
        dim = embeddings.shape[1]
        self.index = faiss.IndexFlatIP(dim)
        self.index.add(embeddings)
        self.vectors = embeddings
        self.ids = list(ids)

    def search(self, vec: np.ndarray, k: int) -> List[Tuple[str, float]]:
        """
        Return top-k (id, score) pairs for a query embedding.

        Accepts 1D or 2D query vectors and validates dimension compatibility with
        the built index before searching.
        """
        if self.index is None or not self.ids or k <= 0:
            return []
        q = np.asarray(vec, dtype="float32")
        if q.ndim == 1:
            q = q.reshape(1, -1)
        elif q.ndim != 2:
            raise ValueError("Query vector must be 1D or 2D.")
        if q.shape[1] != self.index.d:
            raise ValueError(f"Query dimension {q.shape[1]} does not match index dimension {self.index.d}.")
        q = np.array(q, dtype="float32", copy=True)
        faiss.normalize_L2(q)
        top_k = min(k, len(self.ids))
        D, I = self.index.search(q, top_k)
        # Flatten across query rows: a batched 2D query previously returned row 0 only.
        results: List[Tuple[str, float]] = []
        for row in range(I.shape[0]):
            for col, i in enumerate(I[row]):
                if i != -1 and i < len(self.ids):
                    results.append((self.ids[i], float(D[row][col])))
        return results


# Extract text from all pages of a PDF (best effort).
def _extract_pdf_text(pdf_path: str) -> str:
    """Best-effort text extraction for all pages in a PDF."""
    reader = PdfReader(pdf_path)
    pages = []
    for p in reader.pages:
        try:
            pages.append(p.extract_text() or "")
        except Exception:
            pages.append("")
    return "\n".join(pages)


# Chunk text by paragraph with overlap to preserve context.
def _chunk_text(text: str, max_chars=1800, overlap=300) -> List[str]:
    """Split text into paragraph chunks and add tail overlap for retrieval continuity."""
    paragraphs = [p.strip() for p in text.split("\n") if p.strip()]
    chunks, buf = [], ""
    for para in paragraphs:
        if len(buf) + len(para) + 1 <= max_chars:
            buf = (buf + "\n" + para).strip()
        else:
            if buf:
                chunks.append(buf)
            buf = para
    if buf:
        chunks.append(buf)
    merged = []
    # Add overlap from previous chunk to improve retrieval continuity.
    for c in chunks:
        if not merged:
            merged.append(c)
        else:
            tail = merged[-1][-overlap:]
            # Trim the overlap, not the chunk: slicing the concatenation dropped
            # the tail of every non-first chunk from the corpus.
            budget = max_chars - len(c) - 1
            if budget <= 0:
                merged.append(c[:max_chars])
            else:
                merged.append(tail[-budget:] + "\n" + c)
    return merged


def _safe_cache_label(value: str, fallback: str) -> str:
    """Sanitize a cache-label component so it is filesystem-safe and readable."""
    label = re.sub(r"[^A-Za-z0-9_.-]+", "_", str(value)).strip("._-")
    return label or fallback


def get_cache_paths(cache_dir: str, pdf_path: str, provider: str, emb_model: str) -> Tuple[Path, Path, Path]:
    """
    Return cache artifact paths namespaced by provider/model/pdf signature.

    The derived bucket includes:
    - provider name
    - embedding model
    - stable hash over provider/model/pdf absolute path and PDF size/mtime signature
    """
    cache_root = Path(cache_dir)
    cache_root.mkdir(parents=True, exist_ok=True)

    pdf = Path(pdf_path)
    pdf_abs = str(pdf.resolve())
    try:
        # Signature captures content changes in typical local workflows.
        st = pdf.stat()
        pdf_sig = f"{st.st_size}:{int(st.st_mtime)}"
    except OSError:
        pdf_sig = "missing"

    key_material = f"{provider}|{emb_model}|{pdf_abs}|{pdf_sig}"
    key = hashlib.sha256(key_material.encode("utf-8")).hexdigest()[:16]
    provider_tag = _safe_cache_label(provider, "provider")
    model_tag = _safe_cache_label(emb_model, "model")[:48]
    cache_bucket = cache_root / f"{provider_tag}__{model_tag}__{key}"
    cache_bucket.mkdir(parents=True, exist_ok=True)
    return cache_bucket / "vectors.npy", cache_bucket / "ids.json", cache_bucket / "chunks.json"


def load_cached_index(vec_p: Path, ids_p: Path, chunks_p: Path) -> Optional[Tuple[EmbeddingIndex, List[DocChunk]]]:
    """
    Load cached index artifacts if they pass structural integrity checks.

    Validation criteria:
    - all files exist
    - vectors are 2D
    - ids/chunks are lists
    - lengths align with vector row count
    - each chunk object has at least {id, text}
    """
    if not (vec_p.exists() and ids_p.exists() and chunks_p.exists()):
        return None
    try:
        vectors = np.load(vec_p)
        ids = json.loads(ids_p.read_text(encoding="utf-8"))
        raw_chunks = json.loads(chunks_p.read_text(encoding="utf-8"))
        if not isinstance(ids, list) or not isinstance(raw_chunks, list):
            return None
        if vectors.ndim != 2:
            return None
        if len(ids) != vectors.shape[0] or len(raw_chunks) != vectors.shape[0]:
            return None
        chunks: List[DocChunk] = []
        for item in raw_chunks:
            if not isinstance(item, dict) or "id" not in item or "text" not in item:
                return None
            chunks.append(DocChunk(**item))
        idx = EmbeddingIndex()
        idx.build(vectors, ids)
        return idx, chunks
    except Exception:
        return None


def _atomic_write_bytes(target: Path, payload: bytes) -> None:
    """Write via a temp file in the same directory, then rename into place."""
    tmp = target.with_name(target.name + ".tmp")
    try:
        tmp.write_bytes(payload)
        os.replace(tmp, target)
    finally:
        if tmp.exists():
            try:
                tmp.unlink()
            except OSError:
                pass


def save_cached_index(vec_p: Path, ids_p: Path, chunks_p: Path, embeddings: np.ndarray, chunks: List[DocChunk]) -> None:
    """
    Persist vectors, chunk ids, and chunk payloads as the canonical cache triplet.

    Each file is written atomically, so a concurrent or interrupted writer cannot
    leave behind a mismatched triplet that still passes the load-time length checks.
    """
    buffer = io.BytesIO()
    np.save(buffer, embeddings)
    _atomic_write_bytes(vec_p, buffer.getvalue())
    _atomic_write_bytes(ids_p, json.dumps([c.id for c in chunks]).encode("utf-8"))
    _atomic_write_bytes(chunks_p, json.dumps([c.__dict__ for c in chunks]).encode("utf-8"))
