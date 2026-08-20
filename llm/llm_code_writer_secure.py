import argparse
import json
import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import subprocess
import tempfile
from shutil import which

import numpy as np
from openai import OpenAI

from utils.gateway_rate_limit import call_with_concurrency_backoff, wait_for_gateway_slot
from utils.llm_env import (
    client_kwargs,
    get_gateway_base_url,
    get_gateway_chat_model,
    get_openai_chat_model,
    get_openai_emb_model,
    load_llm_env,
)

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

load_llm_env()

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RULES_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "CrySLRules"
SANITIZED_DIR = PROJECT_ROOT / "llm" / "sanitized_rules"
PDF_PATH = PROJECT_ROOT / "tse19CrySL.pdf"
FILENAME_TEMPLATE = "sanitized_rule_{fqcn}_{lang}.json"
SANITIZED_DIR.mkdir(parents=True, exist_ok=True)

# Cache for sanitized rules to avoid repeated disk IO.
_SANITIZED_CACHE: Dict[Tuple[str, str], Optional[Dict]] = {}

# Prompt sizing/limits to keep LLM context bounded.
MAX_DEPENDENCIES = 3          # include at most 3 dependencies in the prompt
MAX_ITEMS_PER_DEP = 6         # include at most 6 items per dependency
MAX_DEP_TEXT_CHARS = 1200     # hard cap on dependency text size
MAX_CONTRACT_CHARS = 3500      # total contract size in the prompt
MAX_SECTION_CHARS = 1200       # per-section cap
MAX_SECTION_LINES = 25         # per-section line cap (keeps it readable)

# Fallback primer used when PDF-based RAG is unavailable.
FALLBACK_CRYSL_PRIMER = """
CrySL is a rule language that specifies correct (secure) API usage for crypto libraries.

How to interpret sections:
- OBJECTS: variables that exist in the usage protocol (types + names).
- EVENTS: method calls that matter for correct usage.
- ORDER: the allowed sequence of EVENTS (typestate). Your code must follow this call order.
- CONSTRAINTS: restrictions on parameters or choices (e.g., algorithm, mode, key size). Must be enforced.
- REQUIRES: preconditions that must hold before specific calls (e.g., initialized keys, nonces, SecureRandom).
- ENSURES: postconditions / guarantees after correct usage.
- FORBIDDEN: calls/usages that must never appear in the secure example.

Important: the rule contract below is the source of truth; do not invent extra steps not required by the contract.
""".strip()

SECTION_NAMES = [
    "SPEC",
    "OBJECTS",
    "EVENTS",
    "ORDER",
    "CONSTRAINTS",
    "REQUIRES",
    "ENSURES",
    "FORBIDDEN",
]


def _require_env(var_name: str) -> str:
    value = os.getenv(var_name, "").strip()
    if not value:
        raise RuntimeError(f"{var_name} is not set.")
    return value


def _build_client_for_backend(backend: str) -> OpenAI:
    if backend == "openai":
        return OpenAI(api_key=_require_env("OPENAI_API_KEY"), **client_kwargs())
    api_key = _require_env("GATEWAY_API_KEY")
    base_url = get_gateway_base_url()
    return OpenAI(api_key=api_key, base_url=base_url, **client_kwargs())


def _resolve_models_for_backend(backend: str, chat_model_arg: Optional[str], emb_model_arg: Optional[str]) -> Tuple[str, str]:
    chat_model_cli = (chat_model_arg or "").strip()
    emb_model_cli = (emb_model_arg or "").strip()

    if backend == "openai":
        chat_model = chat_model_cli or get_openai_chat_model()
        emb_model = emb_model_cli or get_openai_emb_model()
        return chat_model, emb_model

    chat_model = chat_model_cli or get_gateway_chat_model()
    emb_model = emb_model_cli or os.getenv("GATEWAY_EMB_MODEL", "").strip()
    if not emb_model:
        raise RuntimeError("GATEWAY_EMB_MODEL is not set (or pass --emb-model) for gateway backend.")
    return chat_model, emb_model


def _maybe_throttle_gateway(backend: str, operation: str) -> None:
    if backend == "gateway":
        wait_for_gateway_slot(operation)


def _resolve_pdf_index_builder(backend: str):
    """Lazy-load provider index builders so backend validation can fail fast without optional FAISS imports."""
    if backend == "gateway":
        from paper_index_gateway import build_pdf_index as build_pdf_index_impl
    else:
        from paper_index import build_pdf_index as build_pdf_index_impl
    return build_pdf_index_impl


# Normalize whitespace in large text blocks.
def _clean_text(s: str) -> str:
    s = (s or "").strip()
    s = re.sub(r"\n{3,}", "\n\n", s)   # collapse huge blank blocks
    return s

# Cap a text block by line count.
def _cap_lines(s: str, max_lines: int) -> str:
    lines = (s or "").splitlines()
    if len(lines) <= max_lines:
        return s
    return "\n".join(lines[:max_lines]) + "\n... (truncated)"

# Cap a text block by character count.
def _cap_chars(s: str, max_chars: int) -> str:
    s = s or ""
    if max_chars <= 0 or len(s) <= max_chars:
        return s
    head = s[:max_chars]
    if "\n" in head:
        head = head.rsplit("\n", 1)[0]
    return head + "\n... (truncated)"


# Clean a list item value for display.
def clean_item(value) -> str:
    if not isinstance(value, str):
        return str(value)
    cleaned = value.strip()
    return cleaned.lstrip(",").strip() if cleaned.startswith(",") else cleaned

# Convert a section list to newline text (or fallback).
def lines_to_text(section) -> str:
    if isinstance(section, list):
        return "\n".join(section) if section else "_no entries_"
    if section is None:
        return "_no entries_"
    return str(section)

# Compact a list to max_items while de-duplicating.
def _compact_list(items, max_items: int) -> list[str]:
    out = []
    seen = set()
    for x in items or []:
        s = str(x).strip()
        if not s or s in seen:
            continue
        seen.add(s)
        out.append(s)
        if len(out) >= max_items:
            break
    return out

# Normalize list-like values into a string list.
def _normalize_listish(value) -> List[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [clean_item(v) for v in value if str(v).strip()]
    if isinstance(value, str):
        val = value.strip()
        return [val] if val else []
    return [clean_item(value)]

# Convert a FQCN into a filesystem-safe name.
def safe_class_name(fqcn: str) -> str:
    return re.sub(r"[^a-zA-Z0-9.\-]", "_", fqcn)


# Compute the sanitized rule JSON path for a class/language.
def rule_path(fqcn: str, lang: str) -> Path:
    return SANITIZED_DIR / FILENAME_TEMPLATE.format(fqcn=fqcn, lang=lang)


# Load JSON quietly (returns None on error).
def load_json_quiet(path: Path) -> Optional[Dict]:
    if not path.exists():
        return None
    try:
        with path.open(encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as exc:
        print(f"[WARN] Could not read {path}: {exc}", file=sys.stderr)
        return None
    
# Load the first available sanitized rule for any language in preferred order.
def load_sanitized_rule(fqcn: str, languages: List[str]) -> Optional[Dict]:
    for lang in languages:
        key = (fqcn, lang)
        if key in _SANITIZED_CACHE:
            data = _SANITIZED_CACHE[key]
            if data is not None:
                return data
            continue
        data = load_json_quiet(rule_path(fqcn, lang))
        _SANITIZED_CACHE[key] = data
        if data is not None:
            return data
    return None
    
# Parse a CrySL rule into section -> lines dict.
def crysl_to_json_lines(crysl_text: str) -> Dict[str, List[str]]:
    sections = ["SPEC", "OBJECTS", "EVENTS", "ORDER", "CONSTRAINTS", "REQUIRES", "ENSURES", "FORBIDDEN"]

    # Match headers at start of line, allow optional ":" and trailing whitespace
    pattern = re.compile(r"(?m)^(%s)\b\s*:?\s*" % "|".join(sections))

    matches = list(pattern.finditer(crysl_text))
    parsed: Dict[str, List[str]] = {}

    for idx, match in enumerate(matches):
        header = match.group(1)
        start = match.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(crysl_text)

        chunk = crysl_text[start:end].strip()
        lines = [line.strip() for line in chunk.splitlines() if line.strip()]
        parsed[header] = lines

    return parsed

# Shape the CrySL contract to be stable, readable, and within size caps.
def shape_crysl_contract(crysl_summary: str) -> str:
    """
    Make contract stable + readable + bounded.
    Preserve ORDER and FORBIDDEN as much as possible.
    Truncate ENSURES/CONSTRAINTS first if needed.
    """
    s = _clean_text(crysl_summary)

    # If headings aren’t present, fallback to a hard cap
    if not any(f"{name}:" in s for name in SECTION_NAMES):
        return _cap_chars(s, MAX_CONTRACT_CHARS)

    # Split into sections by headings like "ORDER:"
    parts: dict[str, str] = {}
    current = None
    buf = []

    def flush():
        nonlocal buf, current
        if current and buf:
            parts[current] = _clean_text("\n".join(buf))
        buf = []

    for line in s.splitlines():
        m = re.match(r"^([A-Z_]+):\s*$", line.strip())
        if m and m.group(1) in SECTION_NAMES:
            flush()
            current = m.group(1)
        else:
            buf.append(line)
    flush()

    # Rebuild with per-section caps
    def cap_section(name: str) -> str:
        txt = parts.get(name, "").strip()
        if not txt:
            return ""
        txt = _cap_lines(txt, MAX_SECTION_LINES)
        txt = _cap_chars(txt, MAX_SECTION_CHARS)
        return f"{name}:\n{txt}"

    # Priority order: keep these strongest
    priority = ["SPEC", "OBJECTS", "ORDER", "REQUIRES", "CONSTRAINTS", "FORBIDDEN", "ENSURES", "EVENTS"]

    blocks = [cap_section(n) for n in priority if cap_section(n)]
    out = _clean_text("\n\n".join(blocks))

    # If still too long, truncate the least important sections first
    if len(out) <= MAX_CONTRACT_CHARS:
        return out

    # Drop EVENTs first, then ENSURES, then CONSTRAINTS (as last resort)
    for drop in ["EVENTS", "ENSURES", "CONSTRAINTS"]:
        blocks = [b for b in blocks if not b.startswith(f"{drop}:")]
        out = _clean_text("\n\n".join(blocks))
        if len(out) <= MAX_CONTRACT_CHARS:
            return out

    # Final hard cap (should rarely happen)
    return _cap_chars(out, MAX_CONTRACT_CHARS)

# Collect dependency constraints for prompt context.
def collect_dependency_constraints(target_fqcn: str, languages: List[str]) -> Tuple[List[str], Dict[str, List[str]]]:
    dep_map: Dict[str, List[str]] = {}
    order: List[str] = []
    primary = load_sanitized_rule(target_fqcn, languages)
    if not primary:
        return order, dep_map
    deps = primary.get("dependency") or []
    seen = set()
    for dep in deps:
        if dep in seen or dep == target_fqcn:
            continue
        seen.add(dep)
        order.append(dep)
        data = load_sanitized_rule(dep, languages)
        if not data:
            dep_map[dep] = []
            continue
        constraints = data.get("constraints") or data.get("constraint") or []
        if not isinstance(constraints, list):
            constraints = [constraints]
        dep_map[dep] = [clean_item(c) for c in constraints if str(c).strip()]
    return order, dep_map


# Format dependency constraints into a compact list string.
def format_dependency_constraints(
    dep_order: list[str],
    dep_map: dict[str, list[str]],
    max_deps: int = MAX_DEPENDENCIES,
    max_items_per_dep: int = MAX_ITEMS_PER_DEP,
    max_chars: int = MAX_DEP_TEXT_CHARS,
) -> str:
    if not dep_order:
        return "(none)"

    lines: list[str] = []
    for dep in dep_order[:max_deps]:
        constraints = _compact_list(dep_map.get(dep, []), max_items_per_dep)
        if not constraints:
            continue
        lines.append(f"- {dep}: " + "; ".join(constraints))

    out = "\n".join(lines).strip() or "(none)"

    if len(out) > max_chars:
        out = out[:max_chars].rsplit("\n", 1)[0] + "\n- (truncated)"
    return out



# Collect dependency ENSURES for prompt context.
def collect_dependency_ensures(target_fqcn: str, languages: List[str], depth: int = 1) -> Tuple[List[str], Dict[str, List[str]]]:
    dep_map: Dict[str, List[str]] = {}
    order: List[str] = []
    primary = load_sanitized_rule(target_fqcn, languages)
    if not primary:
        return order, dep_map
    roots = primary.get("dependency") or []
    seen = {target_fqcn}

    def visit(fqcn: str, current_depth: int):
        if fqcn in seen:
            return
        seen.add(fqcn)
        if fqcn not in order:
            order.append(fqcn)
        data = load_sanitized_rule(fqcn, languages)
        if not data:
            dep_map[fqcn] = []
            return
        dep_map[fqcn] = _normalize_listish(data.get("ensures"))
        if current_depth < depth:
            for nxt in _normalize_listish(data.get("dependency")):
                visit(nxt, current_depth + 1)

    for dep in roots:
        if isinstance(dep, str) and dep:
            visit(dep, 1)
    return order, dep_map


# Format dependency ENSURES into a compact list string.
def format_dependency_ensures(
    dep_order: list[str],
    dep_map: dict[str, list[str]],
    max_deps: int = MAX_DEPENDENCIES,
    max_items_per_dep: int = MAX_ITEMS_PER_DEP,
    max_chars: int = MAX_DEP_TEXT_CHARS,
) -> str:
    if not dep_order:
        return "(none)"

    lines: list[str] = []
    for dep in dep_order[:max_deps]:
        ensures = _compact_list(dep_map.get(dep, []), max_items_per_dep)
        if not ensures:
            continue
        lines.append(f"- {dep}: " + "; ".join(ensures))

    out = "\n".join(lines).strip() or "(none)"

    if len(out) > max_chars:
        out = out[:max_chars].rsplit("\n", 1)[0] + "\n- (truncated)"
    return out

# Normalize a PDF chunk to text.
def _chunk_to_text(chunk) -> str:
    """Normalize a PDF chunk to text (supports str or objects with .text)."""
    if chunk is None:
        return ""
    if isinstance(chunk, str):
        return chunk
    txt = getattr(chunk, "text", None)
    if isinstance(txt, str):
        return txt
    return str(chunk)


# Retrieve top-k chunks from FAISS for a query embedding.
def retrieve_top_k(idx, chunks, query_embedding, k: int = 2, per_chunk_max: int = 900) -> list[str]:
    """
    Minimal FAISS retrieval helper:
    - idx may be a wrapper with `.index` or the FAISS index itself
    - chunks may be list[str] OR list[objects with `.text`]
    """
    if not idx or not chunks:
        return []

    faiss_index = getattr(idx, "index", idx)

    q = np.asarray([query_embedding], dtype="float32")
    _, I = faiss_index.search(q, k)

    results: list[str] = []
    for j in I[0]:
        if j < 0 or j >= len(chunks):
            continue

        chunk_text = _chunk_to_text(chunks[j]).strip()
        if not chunk_text:
            continue

        if len(chunk_text) > per_chunk_max:
            chunk_text = chunk_text[:per_chunk_max].rsplit("\n", 1)[0] + "\n... (truncated)"

        results.append(chunk_text)

    return results
    
# Build or load a short CrySL primer (semantics-only), optionally augmented via RAG.
def load_crysl_primer(
    pdf_path: Optional[Path],
    emb_model: str,
    cache_dir: Path,
    backend: str,
    client: OpenAI,
) -> str:
    """
    Returns a short, stable CrySL primer (semantics-only).
    Uses the built-in primer as a stable scaffold, then optionally appends a few
    retrieved snippets from the CrySL paper (NOT rule-specific).
    """
    try:
        cache_dir.mkdir(parents=True, exist_ok=True)
        model_tag = re.sub(r"[^A-Za-z0-9_.-]+", "_", emb_model).strip("._-") or "model"
        cache_file = cache_dir / f"crysl_primer_{backend}_{model_tag}.txt"
        if cache_file.exists():
            return cache_file.read_text(encoding="utf-8").strip()

        scaffold = FALLBACK_CRYSL_PRIMER.strip()

        # If no PDF, return scaffold-only (still cached for stability)
        if not pdf_path or not pdf_path.exists():
            primer = (
                "CrySL Primer (general semantics; not rule-specific):\n\n"
                + scaffold
            ).strip()[:3000]
            cache_file.write_text(primer, encoding="utf-8")
            return primer

        build_pdf_index_fn = _resolve_pdf_index_builder(backend)
        idx, chunks = build_pdf_index_fn(str(pdf_path), emb_model=emb_model)

        topic_queries = [
            "CrySL overview for developers: what are SPEC, OBJECTS, EVENTS, ORDER, CONSTRAINTS, REQUIRES, ENSURES, FORBIDDEN?",
            "CrySL ORDER meaning: usage protocol / typestate; how it constrains the sequence of EVENTS in code.",
            "CrySL CONSTRAINTS meaning: parameter restrictions and helper functions like alg(), mode(), length().",
            "CrySL predicates meaning: REQUIRES and ENSURES; dependencies/contracts between classes/objects.",
            "CrySL FORBIDDEN meaning: insecure calls/usages that must not occur.",
            "CrySL constraints + predicates in practice: how ORDER/CONSTRAINTS relate to satisfying REQUIRES/ENSURES.",
        ]

        def is_noise(text: str) -> bool:
            t = (text or "").lower().strip()
            if not t:
                return True

            # boilerplate
            if any(x in t for x in ["ieee transactions", "personal use is permitted", "doi", "arxiv"]):
                return True

            # grammar/BNF-ish chunks
            if any(x in t for x in [":=", "bnf", "usagepattern", "fmethods", "aggregate", "predicate :="]):
                return True

            # paper navigation / cross references
            if re.search(r"\bsection\s+\d", t):
                return True
            if re.search(r"\bfig(ure)?s?\s+\d", t):
                return True

            # formal semantics sections (too research-y for codegen)
            if "formal semantics" in t:
                return True

            # tooling / implementation chatter (noise for your goal)
            if any(x in t for x in [
                "xtext", "syntax highlighter", "type checker", "compiler", "implementation",
                "we implemented", "our implementation", "framework", "evaluation", "experiment",
                "dataset", "cognicrypt", "sast", "static analysis", "integration"
            ]):
                return True

            return False

        GOOD_TERMS = [
            "spec", "objects", "events", "order", "constraints", "requires", "ensures", "forbidden",
            "usage", "protocol", "call", "sequence", "typestate", "regular expression",
            "predicate", "predicates", "dependency", "dependencies",
            "alg(", "mode(", "length(", "secure random", "iv", "nonce"
        ]
        BAD_TERMS = [
            "xtext", "type checker", "compiler", "implementation", "framework",
            "evaluation", "experiment", "dataset", "cognicrypt", "sast", "static analysis"
        ]

        def score_chunk(text: str) -> int:
            tt = (text or "").lower()
            good = sum(1 for w in GOOD_TERMS if w in tt)
            bad = sum(1 for w in BAD_TERMS if w in tt)
            return good - 2 * bad

        def clean_excerpt(text: str) -> str:
            t = (text or "")

            # normalize common PDF artifacts
            t = t.replace("\ufb01", "fi").replace("\ufb02", "fl").replace("\u00ad", "")

            # join hyphenated line breaks: "secu-\nrity" -> "security"
            t = re.sub(r"-\s*\n\s*", "", t)

            # normalize whitespace/newlines
            t = re.sub(r"\s*\n\s*", " ", t)
            t = re.sub(r"[ \t]{2,}", " ", t).strip()

            # remove inline source references like "(Line 72)" or "Line 72"
            t = re.sub(r"\(\s*line\s*\d+\s*\)", "", t, flags=re.IGNORECASE)
            t = re.sub(r"\bline\s*\d+\b", "", t, flags=re.IGNORECASE)
            t = re.sub(r"[ \t]{2,}", " ", t).strip()

            # strip leading punctuation/junk and fix mid-word chunk starts like "d not ..."
            t = t.lstrip("–—-•*,:;)]} ").strip()
            t = re.sub(r"^[a-z]{1,2}\s+", "", t)

            return t

        def trim_to_sentence(text: str, max_chars: int = 900) -> str:
            text = (text or "").strip()
            if len(text) <= max_chars:
                return text
            cut = text[:max_chars]
            for sep in [". ", "? ", "! ", "\n"]:
                pos = cut.rfind(sep)
                if pos > 200:
                    return cut[:pos + 1].strip()
            return cut.rsplit(" ", 1)[0].strip() + " ..."

        collected: list[str] = []
        seen = set()

        for q in topic_queries:
            _maybe_throttle_gateway(backend, "embeddings")
            q_emb = call_with_concurrency_backoff(
                lambda: client.embeddings.create(model=emb_model, input=q), "embeddings"
            ).data[0].embedding
            candidates = retrieve_top_k(idx, chunks, q_emb, k=8, per_chunk_max=900)

            best = None
            best_score = -10**9

            for c in candidates:
                c = (c or "").strip()
                if not c:
                    continue
                c = clean_excerpt(c)
                if not c or is_noise(c):
                    continue

                key = c[:200].lower()
                if key in seen:
                    continue

                s = score_chunk(c)
                if s > best_score:
                    best, best_score = c, s

            if best and best_score >= 2:
                best = trim_to_sentence(best, 900)
                key = best[:200].lower()
                if key not in seen:
                    seen.add(key)
                    collected.append(best)

        if not collected:
            primer = (
                "CrySL Primer (general semantics; not rule-specific):\n\n"
                + scaffold
            ).strip()[:3000]
            cache_file.write_text(primer, encoding="utf-8")
            return primer

        primer = (
            "CrySL Primer (general semantics; not rule-specific):\n\n"
            + scaffold
            + "\n\n--- Retrieved notes from the CrySL paper (semantics-only) ---\n\n"
            + "\n\n---\n\n".join(collected[:5])
        ).strip()

        primer = primer[:3300]

        cache_file.write_text(primer, encoding="utf-8")
        return primer

    except Exception:
        return FALLBACK_CRYSL_PRIMER

# Build the secure-code prompt from the contract + primer + dependency context.
def build_secure_prompt(context: Dict[str, str]) -> str:
    dep_ensures = context.get("dep_ensures_text") or "(no dependency guarantees)"
    dep_constraints = context.get("dep_constraints_text") or "(no dependency constraints)"

    return f"""
You are a senior Java security engineer specializing in JCA/JCE and secure API usage.

Goal:
Generate a single, self-contained, production-quality **secure** Java usage example for `{context['class_name']}`.

Authority rules (very important):
- The **CrySL contract** below is the SOURCE OF TRUTH for required calls, order, constraints, and forbidden usage.
- The **CrySL primer** is only to help you understand CrySL semantics. It MUST NOT override the contract.
- If general best practices conflict with the CrySL contract, FOLLOW THE CONTRACT.

Compilation contract (non-negotiable):
- Target Java: 17+. Standard library only. No external dependencies.
- Output MUST be a single file with NO package declaration.
- The public class MUST be named SecureUsageExample (so it compiles as SecureUsageExample.java).
- Exactly ONE top-level public type: `public class SecureUsageExample`. No other public classes/interfaces/enums/records.
- Any helper code MUST be `private static` methods inside `SecureUsageExample` (no extra top-level types).
- Every referenced non-java.lang type MUST be either:
  (a) imported explicitly, or
  (b) used via fully-qualified name in code.
- If unsure about an import, use the fully-qualified name (compile correctness > style).

CRYSL PRIMER (GENERAL SEMANTICS — NOT AUTHORITATIVE):
{context['crysl_primer']}

CRYSL CONTRACT (AUTHORITATIVE — MUST FOLLOW EXACTLY):
{context['crysl_summary']}

SUPPORTING INFO (do not override the CrySL contract):
- Dependency Guarantees:
{dep_ensures}

- Dependency Constraints:
{dep_constraints}

Hard requirements (must follow all):
1) Follow ORDER exactly as implied by the contract (`ORDER: {context['order_txt']}`). Use real JCA/JCE calls (no invented APIs).
2) Enforce every CONSTRAINT (algorithms, key sizes, modes/paddings, provider requirements, parameter domains). If multiple are allowed, choose the strongest allowed option.
3) Implement REQUIRES as concrete setup/preconditions (correct initialization, SecureRandom usage, IV/nonce generation, parameter generation, key generation/loading).
4) Ensure FORBIDDEN calls/usages never occur anywhere in the code (not even in comments).
5) No placeholders like TODO/null. Generate real keys/IVs/nonces, and handle exceptions properly.
6) Output **Java code only**, wrapped in a single ```java fenced block. No prose before/after.
7) The code must be compilable (include imports + a minimal public class with a runnable `main`).
8) Import pass (mandatory):
    - No wildcard imports.
    - Ensure every referenced non-java.lang type is imported OR fully-qualified.
    - Remove unused imports.

9) Do NOT declare `main` as `throws Exception` / `throws Throwable`. Handle errors inside `main` using try/catch and fail safely.

General security guardrails (apply unless the CrySL contract explicitly implies otherwise):
A) Never print, log, or expose secrets (passwords, keys, private key material, raw plaintext, nonces/IVs). Do not hex/Base64-print keys, IVs, nonces, ciphertext, or plaintext.
B) Avoid converting secrets from `char[]` to `String`. If a secret is needed, keep it as `char[]` and clear it after use (`Arrays.fill(secret, '\\0')`).
C) Secret input policy (no hardcoded secrets, ever):
   - Prefer `System.console().readPassword()` when available (no echo).
   - Else prefer reading from an environment variable (e.g., `APP_PASSWORD`).
   - Else last resort: `Scanner` input (echoes; add a brief warning comment).
   - If none are available, throw `IllegalStateException`.
D) ABSOLUTE BAN — no secret literals:
   - The code MUST NOT contain ANY hardcoded secret literal anywhere (even for “demo” or “fallback”).
   - This bans: `new char[]{...}` for passwords, `'p','a','s'...`, `"password"`, `"secret"`, `"fallbackPassword"`, `"1234"`, or any literal credential/token/key material.
   - If a secret is missing, do NOT fabricate one. Use Scanner or throw `IllegalStateException`.
   - This ban applies ONLY to credentials/key material. Non-secret demo literals (e.g., plaintext "Example message") are allowed.

E) Do not make false security/semantic claims in comments or output. Do NOT say “authenticated”, “verified”, “securely stored”, etc. unless the code truly performs that operation.
F) Prefer no console output. If you print anything, it must be non-sensitive and strictly accurate (e.g., “Operation completed.”).
G) Use `SecureRandom` for all randomness. Prefer `SecureRandom.getInstanceStrong()` when available; otherwise use `new SecureRandom()`.
H) Prefer authenticated encryption (AEAD) like GCM if permitted by the contract constraints. If not permitted, follow the contract and still generate a correct IV/nonce.
I) Always specify explicit charset when converting text to bytes. Prefer `StandardCharsets.UTF_8` over `"UTF-8"`.
J) Use try-with-resources for streams/Closeables where relevant and close resources reliably.
K) If comparing secret bytes is needed, use constant-time comparison (`MessageDigest.isEqual`) rather than `Arrays.equals`.
L) Minimize secret lifetime:
   - Do NOT store secrets in `static` fields or long-lived object fields.
   - Keep secrets in local variables with the smallest scope possible.
   - Clear secrets in a `finally` block or immediately after last use.
M) Callback secrets rule (applies to any callback/lambda/handler that supplies credentials or other secrets):
   - If the code uses any callback/handler/lambda that supplies credentials or other secrets, obtain the secret inside the callback on each invocation into local variables and clear them in a finally block; never store or reuse secrets from outer scope/fields.
   - Never store secrets in fields (static or instance) of the callback/handler object.
   - Obtain secrets inside the callback each time it is invoked (console/env/scanner as needed), store only in local variables, and clear them in a finally block.
   - Do not clear a shared outer secret that might be reused on a later callback invocation.

Final self-check before output (do this mentally, then output only code):
- Compiles: no unresolved symbols; imports match all referenced types; no unused imports.
- If `StandardCharsets` appears anywhere, confirm `import java.nio.charset.StandardCharsets;` is present.
- No secret literals anywhere (no hardcoded fallback passwords, no `new char[]{...}` credentials, no `"password"`/`"secret"` literals).
- No printing/logging/encoding of secrets/IVs/nonces/ciphertext/plaintext; if output exists, it is non-sensitive and strictly accurate.
- No forbidden APIs; ORDER and constraints satisfied.
- `main` does not throw; errors are handled with try/catch.


Output rules:
- Return exactly one Java file in one fenced block.
- Put everything inside one public class.
""".strip()

IMPORT_WHITELIST = {
    "Arrays": "java.util.Arrays",
    "StandardCharsets": "java.nio.charset.StandardCharsets",
    "MessageDigest": "java.security.MessageDigest",
    "Base64": "java.util.Base64",
    "SecureRandom": "java.security.SecureRandom",
    "GeneralSecurityException": "java.security.GeneralSecurityException",
}

CANONICAL_IMPORT_REWRITES = {
    "import java.security.spec.InvalidAlgorithmParameterException;":
        "import java.security.InvalidAlgorithmParameterException;",
    "import javax.crypto.spec.MGF1ParameterSpec;":
        "import java.security.spec.MGF1ParameterSpec;",
    "import java.security.spec.PSource;":
        "import javax.crypto.spec.PSource;",
}

# Extract fenced Java code if present.
def _extract_fenced_java(text: str) -> tuple[str, bool]:
    # Prefer ```java ... ```
    m = re.search(r"```java\s*(.*?)\s*```", text, flags=re.DOTALL | re.IGNORECASE)
    if m:
        return m.group(1).strip(), True
    # Fallback: ``` ... ```
    m = re.search(r"```\s*(.*?)\s*```", text, flags=re.DOTALL)
    if m:
        return m.group(1).strip(), True
    return text.strip(), False

# Rewrap Java code in a fenced block (if needed).
def _rewrap_fenced_java(java_code: str, had_fence: bool) -> str:
    if not had_fence:
        return java_code.strip()
    return "```java\n" + java_code.strip() + "\n```"

# Normalize the public class name to the required name.
def _normalize_public_class_name(java_code: str, desired: str = "SecureUsageExample") -> str:
    # Normalize any "public [final|abstract] class X" to "public class SecureUsageExample"
    return re.sub(
        r"\bpublic\s+(?:final\s+|abstract\s+)?class\s+[A-Za-z_][A-Za-z0-9_]*\b",
        f"public class {desired}",
        java_code,
        count=1,
    )


def _dedupe_imports(java_code: str) -> str:
    lines = java_code.splitlines()
    import_lines = [i for i, ln in enumerate(lines) if re.match(r"^\s*import\s+[\w.]+\s*;\s*$", ln)]
    if not import_lines:
        return java_code

    start, end = import_lines[0], import_lines[-1]
    merged = []
    seen = set()
    for i in range(start, end + 1):
        imp = lines[i].strip()
        if imp not in seen:
            merged.append(imp)
            seen.add(imp)
    lines[start:end + 1] = merged
    return "\n".join(lines)


def normalize_known_api_mistakes(java_code: str) -> str:
    for bad, good in CANONICAL_IMPORT_REWRITES.items():
        java_code = java_code.replace(bad, good)

    # Prefer singleton constant for PSource when model invents invalid constructor usage.
    java_code = re.sub(
        r"(\bPSource\s+\w+\s*=\s*)new\s+PSource\s*\(\s*PSource\.PSpecified\.DEFAULT\s*\)",
        r"\1PSource.PSpecified.DEFAULT",
        java_code,
    )
    java_code = re.sub(
        r"new\s+PSource\s*\(\s*PSource\.PSpecified\.DEFAULT\s*\)",
        "PSource.PSpecified.DEFAULT",
        java_code,
    )

    # NOTE: a rewrite of two-argument TrustAnchor construction used to live here. It was
    # removed because TrustAnchor(X509Certificate, byte[]) is a real constructor, so the
    # rewrite silently replaced a caller's genuine nameConstraints argument with
    # "(byte[]) null" - dropping a PKIX security control from otherwise correct code. It
    # also failed to repair the hallucination it targeted: TrustAnchor(String, byte[])
    # does not exist either. The compile-and-repair loop handles such mistakes without
    # risking correct code.

    # Frequent over-specific checked catches in constructor-only snippets.
    java_code = re.sub(
        r"catch\s*\(\s*NoSuchAlgorithmException\s*\|\s*InvalidAlgorithmParameterException\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)",
        r"catch (Exception \1)",
        java_code,
    )
    java_code = re.sub(
        r"catch\s*\(\s*InvalidAlgorithmParameterException\s*\|\s*NoSuchAlgorithmException\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)",
        r"catch (Exception \1)",
        java_code,
    )

    # If code calls getSubjectX500Principal() on Certificate, force X509Certificate typing.
    if "getSubjectX500Principal()" in java_code:
        java_code = java_code.replace(
            "import java.security.cert.Certificate;",
            "import java.security.cert.X509Certificate;",
        )
        # Replace raw Certificate type usage with X509Certificate (covers params, locals, fields).
        java_code = re.sub(r"\bCertificate\b", "X509Certificate", java_code)

    return _dedupe_imports(java_code)


# Add missing imports for whitelisted symbols and normalize class name.
def auto_import_patch(llm_text: str) -> str:
    java_code, had_fence = _extract_fenced_java(llm_text)
    java_code = _normalize_public_class_name(java_code, "SecureUsageExample")
    java_code = normalize_known_api_mistakes(java_code)

    needed = []
    for sym, fq in IMPORT_WHITELIST.items():
        # Match a bare use of the simple name ("catch (GeneralSecurityException e)")
        # as well as member access ("Arrays."), but never "java.util.Arrays".
        if not re.search(rf"(?<![\w.]){re.escape(sym)}(?![\w])", java_code):
            continue
        # Respect an import the model already chose for this simple name; adding a
        # second import of the same simple name would not compile.
        if re.search(rf"^\s*import\s+[\w.]*\.{re.escape(sym)}\s*;\s*$", java_code, re.MULTILINE):
            continue
        needed.append(f"import {fq};")

    if not needed:
        return _rewrap_fenced_java(java_code, had_fence)

    lines = java_code.splitlines()
    import_lines = [i for i, ln in enumerate(lines) if re.match(r"^\s*import\s+[\w.]+\s*;\s*$", ln)]
    existing = set(
        ln.strip() for ln in lines if re.match(r"^\s*import\s+[\w.]+\s*;\s*$", ln)
    )
    missing = [imp for imp in needed if imp not in existing]
    if not missing:
        return _rewrap_fenced_java(java_code, had_fence)

    if import_lines:
        start, end = import_lines[0], import_lines[-1]
        current = [lines[i].strip() for i in range(start, end + 1)]
        merged = sorted(set(current + missing))
        lines[start:end + 1] = merged
    else:
        pkg_idx = next((i for i, ln in enumerate(lines)
                        if re.match(r"^\s*package\s+[\w.]+\s*;\s*$", ln)), None)
        insert_at = (pkg_idx + 1) if pkg_idx is not None else 0

        to_insert = missing[:]
        # Add spacing so imports don't glue to package/class
        if insert_at > 0:
            to_insert = [""] + to_insert
        to_insert = to_insert + [""]

        lines[insert_at:insert_at] = to_insert

    patched = "\n".join(lines).strip()
    patched = normalize_known_api_mistakes(patched)
    return _rewrap_fenced_java(patched, had_fence)

def _completion_text(response) -> str:
    """
    Assistant text from a chat completion, or "" when the model returned nothing.

    A refused or safety-filtered completion has `message.content is None`, which
    used to raise AttributeError on `.strip()`.
    """
    choices = getattr(response, "choices", None) or []
    if not choices:
        return ""
    message = getattr(choices[0], "message", None)
    content = getattr(message, "content", None) if message is not None else None
    return content.strip() if content else ""


# Resolve javac binary (with optional CI override).
def _javac_cmd() -> Optional[str]:
    # Allow overriding in CI
    cmd = os.getenv("JAVAC_BIN", "").strip() or "javac"
    return cmd if which(cmd) else None

# Compile generated Java and return (ok, error_text).
def compile_java(java_code: str, compile_classpath: Optional[str], java_release: str) -> tuple[bool, str]:
    javac = _javac_cmd()
    if not javac:
        return False, "javac not found on PATH (or JAVAC_BIN)."

    java_code = _normalize_public_class_name(java_code, "SecureUsageExample")
    java_code = normalize_known_api_mistakes(java_code)

    with tempfile.TemporaryDirectory() as td:
        src = Path(td) / "SecureUsageExample.java"
        src.write_text(java_code, encoding="utf-8")

        cmd = [javac, "--release", str(java_release).strip(), str(src)]
        if compile_classpath and compile_classpath.strip():
            cmd = [javac, "--release", str(java_release).strip(), "-cp", compile_classpath.strip(), str(src)]

        try:
            proc = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=20,
            )
        except subprocess.TimeoutExpired:
            # Report as an ordinary compile failure so the repair loop and the
            # hard gate handle it, instead of crashing the generator.
            return False, "javac timed out after 20s."
        except OSError as exc:
            return False, f"javac could not be executed: {exc}"

        if proc.returncode == 0:
            return True, ""
        return False, (proc.stderr or proc.stdout or "").strip()


# End-to-end secure example generation with compile/repair loop.
def process_rule(
    json_path: Path,
    language: str,
    backend: str,
    model: Optional[str],
    pdf_path: Optional[Path],
    emb_model: Optional[str],
    rules_dir: Path,
    compile_classpath: Optional[str],
    java_release: str,
) -> Optional[str]:
    """
    Primer-only mode:
    - NO rule-specific PDF RAG
    - Use CrySL primer (semantics-only, cached) + shaped CrySL contract (authoritative)
    - Keep dependency constraints/ensures (bounded) to help cross-class contracts
    """
    try:
        with json_path.open(encoding="utf-8") as handle:
            rule_payload = json.load(handle)
    except Exception as exc:
        print(f"Failed to read rule JSON {json_path}: {exc}", file=sys.stderr)
        return None

    class_name = rule_payload.get("className")
    if not class_name:
        print("rule JSON missing className", file=sys.stderr)
        return None

    try:
        client = _build_client_for_backend(backend)
        resolved_model, resolved_emb_model = _resolve_models_for_backend(backend, model, emb_model)
    except Exception as exc:
        print(f"Backend/model configuration error: {exc}", file=sys.stderr)
        return None

    preferred_langs = [language]
    if language.lower() != "english":
        preferred_langs.append("English")

    # --- Build CrySL contract (authoritative) ---
    simple_name = class_name.rsplit(".", 1)[-1] # CrySL rule files are named by simple class name convention, e.g., "Cipher.crysl"
    crysl_path = rules_dir / f"{simple_name}.crysl"
    raw_crysl = crysl_path.read_text(encoding="utf-8") if crysl_path.exists() else ""
    crysl_sections = crysl_to_json_lines(raw_crysl) if raw_crysl else {}

    def prefer(section: str, fallback_key: str) -> str:
        if section in crysl_sections and crysl_sections[section]:
            return lines_to_text(crysl_sections[section])
        return rule_payload.get(fallback_key, "N/A")

    objects_txt = prefer("OBJECTS", "objects")
    events_txt = prefer("EVENTS", "events")
    order_txt = prefer("ORDER", "order")
    constraints_txt = prefer("CONSTRAINTS", "constraints")
    requires_txt = prefer("REQUIRES", "requires")
    ensures_txt = prefer("ENSURES", "ensures")
    forbidden_txt = prefer("FORBIDDEN", "forbidden")

    crysl_summary = "\n\n".join([
        f"SPEC:\n{class_name}",
        f"OBJECTS:\n{objects_txt}",
        f"EVENTS:\n{events_txt}",
        f"ORDER:\n{order_txt}",
        f"CONSTRAINTS:\n{constraints_txt}",
        f"REQUIRES:\n{requires_txt}",
        f"ENSURES:\n{ensures_txt}",
        f"FORBIDDEN:\n{forbidden_txt}",
    ])

    crysl_summary = shape_crysl_contract(crysl_summary)

    # Behind a flag: stderr is merged into the captured output, so this dump was being
    # embedded verbatim in failure messages (and, before that was fixed, in the pages).
    if os.getenv("CRYSLDOC_DEBUG", "").strip() == "1":
        print("\n[debug] ===== SHAPED CRYSL CONTRACT START =====", file=sys.stderr)
        print(crysl_summary, file=sys.stderr)
        print("[debug] ===== SHAPED CRYSL CONTRACT END =====\n", file=sys.stderr)

    # --- Dependency context (bounded) ---
    dep_order_constraints, dep_map_constraints = collect_dependency_constraints(class_name, preferred_langs)
    dep_constraints_text = format_dependency_constraints(dep_order_constraints, dep_map_constraints)

    dep_order_ensures, dep_map_ensures = collect_dependency_ensures(class_name, preferred_langs, depth=1)
    dep_ensures_text = format_dependency_ensures(dep_order_ensures, dep_map_ensures)

    # --- Primer (semantics-only; cached) ---
    effective_pdf_path = pdf_path if (pdf_path and pdf_path.exists()) else PDF_PATH
    crysl_primer = load_crysl_primer(
        pdf_path=effective_pdf_path,
        emb_model=resolved_emb_model,
        cache_dir=PROJECT_ROOT / "rag_cache",
        backend=backend,
        client=client,
    )

    prompt_ctx = {
        "class_name": class_name,
        "crysl_primer": crysl_primer,
        "crysl_summary": crysl_summary,
        "dep_ensures_text": dep_ensures_text,
        "dep_constraints_text": dep_constraints_text,
        "order_txt": order_txt,
    }

    prompt = build_secure_prompt(prompt_ctx)

    system_messages = [
        {
            "role": "system",
            "content": (
                "You are a meticulous secure Java cryptography assistant. Produce production-quality code, "
                "prefer constant-time primitives, and never invent APIs outside the official JCA/JCE surface."
            ),
        }
    ]

    _maybe_throttle_gateway(backend, "chat.completions")
    try:
        response = call_with_concurrency_backoff(
            lambda: client.chat.completions.create(
                model=resolved_model,
                messages=system_messages + [{"role": "user", "content": prompt}],
                temperature=0.0,
                max_tokens=2000,
            ),
            "chat.completions",
        )
    except Exception as exc:
        raise RuntimeError(f"[ERROR] Secure example request failed: {exc}") from exc

    raw = _completion_text(response)
    if not raw:
        raise RuntimeError(
            "[ERROR] The model returned no content for the secure example "
            "(refused, filtered, or empty completion)."
        )

    # 1) deterministic post-pass (imports + class-name normalize)
    patched = auto_import_patch(raw)
    java_only, _ = _extract_fenced_java(patched)

    # 2) compile gate (enable by default; you can turn off with env)
    compile_check = os.getenv("CRYSLDOC_COMPILE_CHECK", "1").strip() == "1"
    strict = os.getenv("CRYSLDOC_COMPILE_STRICT", "0").strip() == "1"

    if compile_check:
        javac_missing_is_fatal = os.getenv("CRYSLDOC_JAVAC_REQUIRED", "1").strip() == "1"
        max_repairs = int(os.getenv("CRYSLDOC_MAX_REPAIRS", "7"))

        ok, err = compile_java(java_only, compile_classpath=compile_classpath, java_release=java_release)

        # If javac isn't available, decide whether to fail or skip
        if (not ok) and ("javac not found" in (err or "").lower()):
            msg = f"[ERROR] Compile check cannot run: {err}"
            if javac_missing_is_fatal or strict:
                raise RuntimeError(msg)
            print("[WARN] " + msg, file=sys.stderr)
            ok = True  # only if you explicitly allow skipping

        attempt = 0
        while (not ok) and (attempt < max_repairs):
            attempt += 1
            err = _cap_chars(err or "", 2000)

            repair_prompt = (
                prompt
                + f"\n\nCompilation failed (repair attempt {attempt}/{max_repairs}).\n"
                "Fix ONLY compilation errors. Do NOT change CrySL ORDER/CONSTRAINTS/REQUIRES logic.\n"
                "Do NOT add forbidden APIs.\n"
                f"Target Java release is {java_release}. Use exact JDK/API signatures visible in compiler diagnostics.\n"
                "For constructor/spec classes, prefer minimal API-accurate examples instead of extra helper logic.\n"
                "Return the full corrected file in one ```java fenced block.\n\n"
                "Compiler output:\n"
                + err
                + "\n\nPrevious code:\n```java\n"
                + java_only
                + "\n```"
            )

            _maybe_throttle_gateway(backend, "chat.completions")
            try:
                repair_resp = call_with_concurrency_backoff(
                    lambda: client.chat.completions.create(
                        model=resolved_model,
                        messages=system_messages + [{"role": "user", "content": repair_prompt}],
                        temperature=0.0,
                        max_tokens=2000,
                    ),
                    "chat.completions",
                )
            except Exception as exc:
                err = f"repair request failed: {exc}"
                continue

            repaired_raw = _completion_text(repair_resp)
            if not repaired_raw:
                err = "the model returned no content for the repair attempt."
                continue
            repaired_patched = auto_import_patch(repaired_raw)
            repaired_java, _ = _extract_fenced_java(repaired_patched)
            repaired_java = normalize_known_api_mistakes(repaired_java)

            # compile repaired candidate
            ok, err = compile_java(
                repaired_java,
                compile_classpath=compile_classpath,
                java_release=java_release,
            )
            if ok:
                java_only = repaired_java
                break

            # carry forward the latest candidate for the next attempt
            java_only = repaired_java

        if not ok:
            # HARD GATE: do not return broken code
            msg = f"[ERROR] Still not compilable after {max_repairs} repair attempts:\n{_cap_chars(err or '', 2000)}"
            raise RuntimeError(msg)

        # Compile passed: ALWAYS return fenced
        patched = _rewrap_fenced_java(java_only, True)

    
    final_java, _ = _extract_fenced_java(patched)
    patched = _rewrap_fenced_java(final_java, True)
    print(patched)
    return patched


# Parse CLI arguments for code generation.
def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate secure Java examples grounded in an authoritative CrySL contract "
            "plus a short semantics-only CrySL primer."
        )
    )
    parser.add_argument(
        "--rules-dir",
        default=str(RULES_DIR),
        help="Path to the CrySLRules directory containing *.crysl files.",
    )

    parser.add_argument("json_path", help="Path to the temp JSON produced by the Java pipeline.")
    parser.add_argument(
        "--backend",
        choices=["openai", "gateway"],
        required=True,
        help="LLM backend selected by the Java pipeline.",
    )
    parser.add_argument(
        "--language",
        default="English",
        help="Language used for sanitized rules (used only for dependency context).",
    )
    parser.add_argument(
        "--model",
        default=None,
        help=(
            "Override chat model. If unset, resolve from OPENAI_CHAT_MODEL or "
            "GATEWAY_CHAT_MODEL in llm/.env (with built-in fallbacks)."
        ),
    )
    parser.add_argument(
        "--pdf",
        default=str(PDF_PATH),
        help="Path to the CrySL PDF used only to build the semantics-only primer.",
    )
    parser.add_argument(
        "--emb-model",
        default=None,
        help=(
            "Override embedding model. If unset, OpenAI uses OPENAI_EMB_MODEL and "
            "gateway requires GATEWAY_EMB_MODEL."
        ),
    )
    parser.add_argument(
        "--compile-classpath",
        default="",
        help="Classpath passed to javac for compile validation.",
    )
    parser.add_argument(
        "--java-release",
        default="21",
        help="Java release flag for javac compile validation (e.g., 21).",
    )
    return parser.parse_args()



# CLI entry point: wire arguments into process_rule.
def main() -> None:
    args = parse_args()
    json_path = Path(args.json_path)
    language = args.language
    pdf_path = Path(args.pdf) if args.pdf else None

    rules_dir = Path(args.rules_dir)
    compile_classpath = args.compile_classpath
    java_release = str(args.java_release)

    result = process_rule(
        json_path=json_path,
        language=language,
        backend=args.backend,
        model=args.model,
        pdf_path=pdf_path,
        emb_model=args.emb_model,
        rules_dir=rules_dir,
        compile_classpath=compile_classpath,
        java_release=java_release,
    )
    if result is None:
        sys.exit(1)

if __name__ == "__main__":
    main()
