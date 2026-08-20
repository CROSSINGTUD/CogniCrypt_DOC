import os
from typing import List

import numpy as np
from openai import OpenAI

from utils.gateway_rate_limit import wait_for_gateway_slot
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


DEFAULT_GATEWAY_BASE_URL = "https://ai-gateway.uni-paderborn.de/v1/"


def get_gateway_client() -> OpenAI:
    """Return an OpenAI-compatible client configured for the UPB gateway."""
    api_key = os.getenv("GATEWAY_API_KEY")
    if not api_key:
        raise RuntimeError("GATEWAY_API_KEY is not set.")
    base_url = os.getenv("GATEWAY_BASE_URL", DEFAULT_GATEWAY_BASE_URL)
    return OpenAI(api_key=api_key, base_url=base_url, **client_kwargs())


PLACEHOLDER_EMB_MODEL = "YOUR_EMBEDDING_MODEL"


def _require_emb_model(model: str) -> str:
    """Reject the unfilled placeholder before it reaches the embeddings endpoint."""
    resolved = (model or "").strip()
    if not resolved or resolved == PLACEHOLDER_EMB_MODEL:
        raise RuntimeError(
            "GATEWAY_EMB_MODEL is not set (or pass --emb-model) for the gateway backend."
        )
    return resolved


def _embed_texts(client: OpenAI, texts: List[str], model: str) -> np.ndarray:
    """Return a float32 embedding matrix for `texts` using one batched request."""
    if not texts:
        return np.empty((0, 0), dtype="float32")
    model = _require_emb_model(model)
    wait_for_gateway_slot("embeddings")
    resp = client.embeddings.create(model=model, input=texts)
    return np.asarray([d.embedding for d in resp.data], dtype="float32")


def build_pdf_index(pdf_path: str, cache_dir: str = "rag_cache", emb_model: str = PLACEHOLDER_EMB_MODEL):
    """
    Load or build a gateway-backed PDF embedding index.

    Cache keys are provider/model/pdf specific, so gateway artifacts remain isolated
    from OpenAI and any other provider.
    """
    vec_p, ids_p, chunks_p = get_cache_paths(
        cache_dir=cache_dir,
        pdf_path=pdf_path,
        provider="gateway",
        emb_model=emb_model,
    )
    cached = load_cached_index(vec_p, ids_p, chunks_p)
    if cached is not None:
        return cached

    raw_text = _extract_pdf_text(pdf_path)
    raw_chunks = _chunk_text(raw_text)
    chunks = [DocChunk(id=f"C{i}", text=t) for i, t in enumerate(raw_chunks)]

    if not chunks:
        idx = EmbeddingIndex()
        empty_embeddings = np.empty((0, 0), dtype="float32")
        idx.build(empty_embeddings, [])
        save_cached_index(vec_p, ids_p, chunks_p, empty_embeddings, chunks)
        return idx, chunks

    client = get_gateway_client()
    embeddings = _embed_texts(client, [c.text for c in chunks], emb_model)
    idx = EmbeddingIndex()
    idx.build(embeddings, [c.id for c in chunks])
    save_cached_index(vec_p, ids_p, chunks_p, embeddings, chunks)
    return idx, chunks
