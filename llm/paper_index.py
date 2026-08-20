import os
from typing import List
import numpy as np
from openai import OpenAI
from utils.llm_env import client_kwargs
from utils.rag_index_common import (
    DocChunk,
    EmbeddingIndex,
    _chunk_text,
    _extract_pdf_text,
    get_cache_paths,
    load_cached_index,
    save_cached_index,
)

# Embed text with OpenAI embeddings and return a float32 matrix.
def _embed_texts(client: OpenAI, texts: List[str], model="text-embedding-3-small") -> np.ndarray:
    """Return a float32 embedding matrix for `texts` using a single batched OpenAI call."""
    if not texts:
        return np.empty((0, 0), dtype="float32")
    resp = client.embeddings.create(model=model, input=texts)
    return np.asarray([d.embedding for d in resp.data], dtype="float32")

# Build (or load) a cached FAISS index over the CrySL paper PDF.
def build_pdf_index(pdf_path: str, cache_dir="rag_cache", emb_model="text-embedding-3-small"):
    """
    Load or build the OpenAI-backed PDF embedding index.

    The cache key includes provider/model/pdf identity via get_cache_paths(), so this
    function can safely coexist with other embedding providers in the same cache root.
    Returns:
    - (EmbeddingIndex, List[DocChunk])
    """
    # Resolve provider/model/pdf-specific cache artifact paths.
    vec_p, ids_p, chunks_p = get_cache_paths(
        cache_dir=cache_dir,
        pdf_path=pdf_path,
        provider="openai",
        emb_model=emb_model,
    )
    # Reuse cached vectors/chunks when artifacts pass integrity validation.
    cached = load_cached_index(vec_p, ids_p, chunks_p)
    if cached is not None:
        return cached
    # Cache miss: extract text and create paragraph-overlap chunks from the PDF.
    raw_text = _extract_pdf_text(pdf_path)
    raw_chunks = _chunk_text(raw_text)
    chunks = [DocChunk(id=f"C{i}", text=t) for i, t in enumerate(raw_chunks)]
    if not chunks:
        # Empty-text PDFs still produce a stable empty cache entry so repeated runs
        # do not repeatedly rebuild or fail.
        idx = EmbeddingIndex()
        empty_embeddings = np.empty((0, 0), dtype="float32")
        idx.build(empty_embeddings, [])
        save_cached_index(vec_p, ids_p, chunks_p, empty_embeddings, chunks)
        return idx, chunks
    # Build embeddings and FAISS index, then persist artifacts for later reuse.
    client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"), **client_kwargs())
    embeddings = _embed_texts(client, [c.text for c in chunks], emb_model)
    idx = EmbeddingIndex()
    idx.build(embeddings, [c.id for c in chunks])
    save_cached_index(vec_p, ids_p, chunks_p, embeddings, chunks)
    return idx, chunks
