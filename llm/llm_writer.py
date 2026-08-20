import os
import sys
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
from openai import OpenAI

from paper_index import build_pdf_index
from utils.llm_env import client_kwargs
from utils.writer_core import (
    WriterCLIConfig,
    build_explanation_prompt,
    build_system_messages,
    extract_completion_text,
    process_rule_core,
    run_writer_main,
)

try:
    # ensure Python stdout uses UTF-8 (Python 3.7+)
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    # fallback: rely on caller to set PYTHONIOENCODING
    pass


# Resolve project root and important folders
PROJECT_ROOT = Path(__file__).resolve().parents[1]
RULES_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "CrySLRules"
PDF_PATH = PROJECT_ROOT / "tse19CrySL.pdf"


# Embed text with OpenAI embeddings and return a float32 matrix.
def _embed_texts(client: OpenAI, texts: List[str], model: str = "text-embedding-3-small") -> np.ndarray:
    """Return float32 embeddings for a list of strings using an OpenAI embedding model."""
    resp = client.embeddings.create(model=model, input=texts)
    return np.asarray([d.embedding for d in resp.data], dtype="float32")


# Build RAG context using the CrySL paper index and this rule's sections.
def make_rag_context(
    client: OpenAI,
    idx,  # from paper_index.build_pdf_index
    chunks,  # list of DocChunk (must have .text)
    emb_model: str,
    rule_sections_txt: Dict[str, str],
    k: int = 6,
    per_chunk_max: int = 900,  # optional: trim long chunks
) -> str:
    """
    Build a retrieval context block from top-k CrySL-paper chunks.

    Inputs:
    - client: OpenAI client used only for query embedding.
    - idx/chunks: FAISS-backed index and aligned chunk metadata from paper_index.
    - rule_sections_txt: normalized CrySL sections for the current rule.
    Returns:
    - rag_block: concatenated snippets tagged [C1], [C2], ...
    """

    # 1) Bias retrieval toward CrySL grammar/semantics (so we explain the SYNTAX)
    syntax_boost = """
    CRYSL language syntax and semantics:
    - Sections: SPEC, OBJECTS, EVENTS, ORDER, CONSTRAINTS, REQUIRES, ENSURES, FORBIDDEN
    - Typestate / usage protocols: ORDER as a regex over EVENTS; use of aggregates
    - Predicates: REQUIRES / ENSURES; NEGATES; 'after' placement for predicate generation
    - Helper functions: alg(), mode(), padding(), length(), neverTypeOf(), callTo(), noCallTo()
    - Examples to retrieve: KeyGenerator (Fig. 2), Cipher (Fig. 3), PBEKeySpec (Fig. 4)
    - EBNF grammar and formal semantics
    """.strip()

    # 2) Build the query using BOTH the syntax boost and this rule's actual sections
    query_text = "\n".join(
        [
            syntax_boost,
            "THIS RULE:",
            "SPEC: " + (rule_sections_txt.get("SPEC") or ""),
            "OBJECTS: " + (rule_sections_txt.get("OBJECTS") or ""),
            "EVENTS: " + (rule_sections_txt.get("EVENTS") or ""),
            "ORDER: " + (rule_sections_txt.get("ORDER") or ""),
            "CONSTRAINTS: " + (rule_sections_txt.get("CONSTRAINTS") or ""),
            "REQUIRES: " + (rule_sections_txt.get("REQUIRES") or ""),
            "ENSURES: " + (rule_sections_txt.get("ENSURES") or ""),
        ]
    ).strip()

    if not hasattr(idx, "index") or idx.index is None or not chunks:
        return ""

    # 3) Embed and search through the shared abstraction.
    # Using `idx.search(...)` aligns OpenAI and gateway adapters on one retrieval contract:
    # both receive ordered `(chunk_id, score)` hits from EmbeddingIndex.
    # RAG is best-effort: degrade to no context rather than aborting the whole run
    # (the index build step already follows this contract).
    try:
        qvec = _embed_texts(client, [query_text], model=emb_model)[0]
        hits = idx.search(qvec, k)
    except Exception as exc:
        print(f"[WARN] RAG retrieval failed, continuing without context: {exc}", file=sys.stderr)
        return ""

    # 4) Build tagged snippets; normalize common PDF ligatures for safer display
    def _normalize_pdf_text(s: str) -> str:
        """Normalize common PDF ligatures and soft hyphens for cleaner markdown output."""
        return s.replace("\ufb01", "fi").replace("\ufb02", "fl").replace("\u00ad", "").strip()

    # Resolve FAISS hit ids to chunk objects via a dictionary instead of positional indexing.
    # This avoids accidental coupling to list order and keeps mapping stable by chunk id.
    chunk_by_id = {getattr(c, "id", None): c for c in chunks}
    rag_snippets: List[str] = []

    for rank, (hid, _score) in enumerate(hits, start=1):
        c = chunk_by_id.get(hid)
        if not c:
            continue
        tag = f"[C{rank}]"
        text = _normalize_pdf_text(getattr(c, "text", ""))
        if per_chunk_max and len(text) > per_chunk_max:
            text = text[:per_chunk_max].rsplit(" ", 1)[0] + " ..."
        rag_snippets.append(f"{tag} {text}")

    rag_block = "\n\n".join(rag_snippets) if rag_snippets else ""
    return rag_block


# Build the LLM prompt and request a structured explanation.
def generate_explanation(
    client: OpenAI,
    model: str,
    class_name: str,
    objects: str,
    events: str,
    order: str,
    constraints: str,
    requires: str,
    ensures: str,
    forbidden: str,
    dep_constraints_text: str,
    dep_ensures_text: str,
    sanitized_summary: str,
    raw_crysl_text: str,
    explanation_language: str,
    rag_block: str = "",
) -> str:
    """Generate a full natural-language rule explanation via OpenAI chat completion."""
    # Build the strict user prompt that includes CrySL-derived fields and section requirements.
    prompt = build_explanation_prompt(
        class_name=class_name,
        objects=objects,
        events=events,
        order=order,
        constraints=constraints,
        requires=requires,
        ensures=ensures,
        forbidden=forbidden,
        dep_constraints_text=dep_constraints_text,
        dep_ensures_text=dep_ensures_text,
        sanitized_summary=sanitized_summary,
        raw_crysl_text=raw_crysl_text,
        explanation_language=explanation_language,
        include_utf8_line=True,
    )

    # Add base system guidance and optional hidden RAG reference material.
    sys_msgs = build_system_messages(rag_block)

    # Single completion call for the final explanation text.
    resp = client.chat.completions.create(
        model=model,
        messages=sys_msgs + [{"role": "user", "content": prompt}],
        temperature=0.3,
        max_tokens=4000,
        # No stop sequence to avoid accidental truncation on ``` blocks
    )
    return extract_completion_text(resp)


# Orchestrate a single rule's explanation generation pipeline.
def process_rule(
    crysl_path: str,
    language: str,
    client: OpenAI,
    model: str,
    target_fqcn: str,
    idx=None,
    chunks=None,
    k: int = 6,
    emb_model: str = "text-embedding-3-small",
):
    """Run the shared single-rule pipeline with OpenAI-specific callbacks."""
    return process_rule_core(
        crysl_path=crysl_path,
        language=language,
        client=client,
        model=model,
        target_fqcn=target_fqcn,
        make_rag_context_fn=make_rag_context,
        generate_explanation_fn=generate_explanation,
        idx=idx,
        chunks=chunks,
        k=k,
        emb_model=emb_model,
    )


# CLI entrypoint: parse args, init OpenAI client, optional RAG index, and run.
def main():
    """CLI entrypoint for OpenAI-backed explanation generation."""
    # Provider-specific defaults are injected into the shared CLI/runtime orchestrator.
    cli_config = WriterCLIConfig(
        description="Generate CrySL rule explanations via LLM (with dependency ENSURES + constraints, optional RAG)",
        model_default="gpt-4o-mini",
        model_help="OpenAI model to use for completions",
        pdf_default=PDF_PATH,
        emb_model_default="text-embedding-3-small",
        emb_model_help="Embedding model for RAG queries",
    )

    # Delegate argument parsing, CrySL resolution, optional RAG indexing, and processing.
    run_writer_main(
        rules_dir=RULES_DIR,
        cli_config=cli_config,
        init_client_fn=lambda: OpenAI(api_key=os.getenv("OPENAI_API_KEY"), **client_kwargs()),
        build_pdf_index_fn=build_pdf_index,
        process_rule_fn=process_rule,
    )


# Standard entry guard for CLI usage.
if __name__ == "__main__":
    main()
