import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Dict, Optional, Tuple, Union

from dotenv import load_dotenv

from utils.llm_env import env_or_fallback
from utils.llm_utils import (
    clean_llm_output,
    collect_dependency_constraints,
    collect_dependency_ensures,
    crysl_to_json_lines,
    format_dependency_constraints,
    format_dependency_ensures,
    format_sanitized_rule_for_prompt,
    lines_to_text,
    load_json,
    rule_path,
    validate_and_fill,
)


@dataclass(frozen=True)
class WriterCLIConfig:
    """Provider-specific knobs injected into the shared writer CLI/runtime flow."""

    description: str
    model_default: str
    model_help: str
    pdf_default: Union[str, Path]
    emb_model_default: str
    emb_model_help: str
    model_env_var: Optional[str] = None
    emb_model_env_var: Optional[str] = None
    k_default: int = 6


def extract_completion_text(resp) -> Optional[str]:
    """
    Pull the assistant text out of a chat completion, tolerating refusals.

    A safety-filtered or otherwise empty completion has `message.content is None`
    (and may even carry no choices at all); returning None here lets callers fail
    cleanly instead of raising TypeError deeper in the pipeline.
    """
    choices = getattr(resp, "choices", None) or []
    if not choices:
        return None
    message = getattr(choices[0], "message", None)
    if message is None:
        return None
    return getattr(message, "content", None)


def build_explanation_prompt(
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
    include_utf8_line: bool,
) -> str:
    """
    Build the user prompt consumed by backend LLM adapters.

    The returned text is intentionally strict about section headings and style so
    downstream rendering can expect consistent markdown structure.
    """
    # Select backend-specific response tail instructions (UTF-8 line is optional).
    response_tail = (
        fr"""Respond in **{explanation_language}** and be as precise as possible.
Make sure that the response is in **utf-8** charset only."""
        if include_utf8_line
        else fr"""Respond in **{explanation_language}** and be as precise as possible."""
    )

    # Keep prompt body centralized here so both OpenAI and gateway wrappers share
    # identical task framing and section requirements.
    prompt = fr"""
You are a cryptography expert who explains complex CrySL rules to Java developers in clear, natural language.

You are analyzing the CrySL specification for: `{class_name}`

Raw CrySL Data:
- OBJECTS: {objects}
- EVENTS: {events}
- ORDER: {order}
- CONSTRAINTS: {constraints}
- REQUIRES: {requires}
- ENSURES: {ensures}
- FORBIDDEN: {forbidden}
- Dependency Constraints (reference only): {dep_constraints_text}
- Dependency Guarantees (ENSURES):
{dep_ensures_text}
- Additional context: {sanitized_summary}
- Original CrySL: {raw_crysl_text}

Your Task: Create a developer-friendly guide that explains how to correctly use this cryptographic class WITHOUT using technical CrySL notation, event labels, or abstract parameter names.

CRITICAL RULES:
1. NO TABLES containing event labels (like g1, i2, u3) or parameter lists
2. NO technical identifiers from CrySL should appear in the output
3. NO abstract variable names (like prePlainText, preCipherTextOffset) in explanations
4. EVERYTHING must be explained in natural, conversational language
5. Use concrete, meaningful examples that developers can relate to
6. Focus on practical usage, not formal specifications
7. Use any provided reference material ONLY to inform your understanding; do NOT quote it, cite it, or mention its existence in the output.

Output Structure - Use these EXACT section headings (start with ## and no leading spaces):

## Overview
Write 2-3 paragraphs explaining what this class does, its role in Java cryptography, and why developers would use it. Make it conversational and informative.

## Correct Usage
Explain the complete workflow as a narrative story. Use phrases like:
- "To start, you'll need to..."
- "The first step is to..."
- "After obtaining an instance..."
- "Finally, complete the operation by..."

Don't just list methods - explain the logical flow and the purpose of each step. Include method names naturally within sentences.

## Parameters and Constraints
Group related constraints into logical categories and explain them in full sentences. For each constraint:
- Explain WHAT is restricted and WHY
- List allowed values with explanations of when to use each
- Describe the security implications

Format as grouped bullet points with full explanations, like:
- **Algorithm choices:** [Full explanation of which algorithms are allowed and why]
- **Key requirements:** [Explanation of what types of keys are needed]
- **Data handling rules:** [Explanation of buffer sizes, offsets, etc. in practical terms]

When listing allowed values (algorithms, modes, etc.), explain what each means and when to use it.

## Method Variations and Use Cases
Group methods by their purpose and explain when to use each variation. Structure as:

### [Functional Group Name]
Explanation of what this group of methods does, followed by:
- **Scenario 1:** Description and when to use this approach
- **Scenario 2:** Description and when to use this alternative
- etc.

For example, instead of listing "init variants i1-i8", group them as "Initialization Options" and explain each scenario where you'd use different parameters.

## Security Requirements
Translate all REQUIRES/ENSURES predicates into plain English requirements that developers can understand:
- Instead of "REQUIRES: generatedKey[key]", write something like "Before using this method, ensure your key was generated using a cryptographically secure key generator"
- Explain what security guarantees the class provides after operations
- Describe any dependencies on other cryptographic operations

**Also incorporate the Dependency Guarantees below:** connect each related class's guarantees to the steps where they matter for `{class_name}`.

## Related Components & Their Guarantees
Use this section to list and explain guarantees from related classes, and explicitly tie them to `{class_name}` usage decisions. Start by summarizing the list below, then expand with plain-English implications for initialization, parameter selection, and error handling.

---
{dep_ensures_text}
---

## Common Mistakes to Avoid
Convert all FORBIDDEN items and constraint violations into practical warnings:
- Explain what NOT to do and the consequences
- Include common programming errors related to this class
- Describe what happens if required steps are skipped

## Quick Reference Checklist
Create a practical checklist written as questions a developer can verify:
- Have you [specific action]?
- Did you ensure [specific requirement]?
- Is your [component] properly [configured/initialized/etc.]?

Make each item actionable and specific to this class's requirements.

WRITING STYLE GUIDELINES:

1. **Replace Technical Terms:**
   - Event labels (g1, i2) → Describe the actual operation
   - Parameter names from CrySL → Meaningful descriptions
   - Regex patterns → Step-by-step explanations
   - Predicates → Security requirements in plain English

2. **Use Natural Language Patterns:**
   - "When you need to..." instead of "Event x1 with parameters..."
   - "This ensures that..." instead of "ENSURES: predicate[...]"
   - "You must first..." instead of "REQUIRES: predicate[...]"
   - "Never call..." instead of "FORBIDDEN: method[...]"

3. **Make It Practical:**
   - Relate to real-world scenarios
   - Explain the 'why' behind each requirement
   - Use analogies where helpful
   - Focus on what developers need to do, not on formal specifications

4. **Be Specific About Security:**
   - Explain why each constraint exists
   - Describe attack scenarios that constraints prevent
   - Clarify the security impact of each requirement

Remember: Your audience is Java developers who need to use this cryptographic class correctly but may not be security experts. Every explanation should help them understand not just WHAT to do, but WHY it matters for security.

The goal is to produce documentation that a developer can read like a tutorial, not a reference manual. They should understand how to use the class correctly without ever seeing CrySL syntax or formal notation.
{response_tail}
"""
    return prompt


def build_system_messages(rag_block: str) -> list[dict[str, str]]:
    """
    Compose the system-role message, optionally including hidden RAG reference material.

    Returns a single system message rather than one per section. The UPB gateway rejects a
    second system message with "System message must be at the beginning" (HTTP 400 wrapped
    in a 500), which silently emptied every explanation when running against it. One system
    message is accepted by every OpenAI-compatible backend, so both providers share it.
    """
    instructions = (
        "You are a patient teacher who excels at explaining complex technical concepts in simple, practical terms. "
        "Use any reference material provided to interpret CrySL syntax precisely, but do NOT quote it, cite it, "
        "or mention its existence in your final answer."
    )

    if rag_block:
        instructions += (
            "\n\nREFERENCE MATERIAL (do not quote, cite, or mention this explicitly):\n" + rag_block
        )

    return [{"role": "system", "content": instructions}]


def process_rule_core(
    crysl_path: str,
    language: str,
    client: Any,
    model: str,
    target_fqcn: str,
    make_rag_context_fn: Callable[..., str],
    generate_explanation_fn: Callable[..., str],
    idx: Any = None,
    chunks: Any = None,
    k: int = 6,
    emb_model: str = "text-embedding-3-small",
) -> Optional[str]:
    """
    Shared single-rule processing pipeline used by both backend wrappers.

    Backend-specific behavior is injected through:
    - make_rag_context_fn
    - generate_explanation_fn
    """
    # Load raw CrySL text from disk and normalize into sectioned data.
    try:
        with open(crysl_path, "r", encoding="utf-8") as f:
            content = f.read()
    except Exception as e:
        print(f"Error loading CrySL from {crysl_path}: {e}", file=sys.stderr)
        return None

    # Derive class name and build human-readable sections for prompting.
    crysl_data = crysl_to_json_lines(content)
    rule = validate_and_fill(crysl_data, language)

    # Fallback to FQCN if SPEC missing
    if isinstance(rule["SPEC"], list):
        class_name = rule["SPEC"][0] if rule["SPEC"] else target_fqcn
    else:
        class_name = rule["SPEC"] or target_fqcn

    # Prepare human-readable blocks for prompt assembly.
    objects_txt = lines_to_text(rule["OBJECTS"])
    events_txt = lines_to_text(rule["EVENTS"])
    order_txt = lines_to_text(rule["ORDER"])
    constraints_txt = lines_to_text(rule["CONSTRAINTS"])
    requires_txt = lines_to_text(rule["REQUIRES"])
    ensures_txt = lines_to_text(rule["ENSURES"])
    forbidden_txt = lines_to_text(rule.get("FORBIDDEN", "N/A"))

    # Dependency constraints (reference context for explanations).
    deps_order_c, dep_to_constraints = collect_dependency_constraints(target_fqcn, language)
    dep_constraints_text = format_dependency_constraints(deps_order_c, dep_to_constraints)

    # Dependency ENSURES (core cross-rule security context).
    deps_order_e, dep_to_ensures = collect_dependency_ensures(target_fqcn, language, depth=1)
    dep_ensures_text = format_dependency_ensures(target_fqcn, deps_order_e, dep_to_ensures)

    # Load primary sanitized rule and format its human-friendly fields.
    primary_sanitized = load_json(rule_path(target_fqcn, language))
    sanitized_summary = (
        format_sanitized_rule_for_prompt(primary_sanitized) if primary_sanitized else "No sanitized fields supplied."
    )

    # Optional RAG context from the CrySL paper (if index/chunks are available).
    rag_block = ""
    if idx is not None and chunks is not None and hasattr(idx, "index"):
        sect: Dict[str, str] = {
            "SPEC": class_name,
            "OBJECTS": objects_txt,
            "EVENTS": events_txt,
            "ORDER": order_txt,
            "CONSTRAINTS": constraints_txt,
            "REQUIRES": requires_txt,
            "ENSURES": ensures_txt,
        }
        rag_block = make_rag_context_fn(client, idx, chunks, emb_model=emb_model, rule_sections_txt=sect, k=k)

    # Call backend LLM adapter and return cleaned text for downstream rendering.
    try:
        raw_out = generate_explanation_fn(
            client=client,
            model=model,
            class_name=class_name,
            objects=objects_txt,
            events=events_txt,
            order=order_txt,
            constraints=constraints_txt,
            requires=requires_txt,
            ensures=ensures_txt,
            forbidden=forbidden_txt,
            dep_constraints_text=dep_constraints_text,
            dep_ensures_text=dep_ensures_text,
            sanitized_summary=sanitized_summary,
            raw_crysl_text=content,
            explanation_language=language,
            rag_block=rag_block,
        )
    except Exception as e:
        print(f"LLM explanation error for {class_name}: {e}", file=sys.stderr)
        return None

    if raw_out is None or not str(raw_out).strip():
        print(
            f"LLM explanation for {class_name} returned no content "
            "(refused, filtered, or empty completion).",
            file=sys.stderr,
        )
        return None

    cleaned = clean_llm_output(raw_out)
    print(cleaned)
    return cleaned


def run_writer_main(
    rules_dir: Path,
    cli_config: WriterCLIConfig,
    init_client_fn: Callable[[], Any],
    build_pdf_index_fn: Callable[..., Tuple[Any, Any]],
    process_rule_fn: Callable[..., Optional[str]],
) -> None:
    """
    Shared CLI orchestration for OpenAI/gateway writer entrypoints.

    Responsibilities:
    - environment loading and CLI default resolution
    - CrySL file lookup
    - optional RAG index build/load
    - delegation into provider-specific process_rule wrapper
    """
    # Load .env before constructing CLI defaults (some wrappers compute defaults from env).
    load_dotenv()

    # Resolve model defaults, optionally from provider-specific environment keys.
    # env_or_fallback treats a present-but-blank variable as unset, so an empty
    # GATEWAY_CHAT_MODEL= in .env falls back instead of becoming an empty model id.
    model_default = (
        env_or_fallback(cli_config.model_env_var, cli_config.model_default)
        if cli_config.model_env_var
        else cli_config.model_default
    )
    emb_model_default = (
        env_or_fallback(cli_config.emb_model_env_var, cli_config.emb_model_default)
        if cli_config.emb_model_env_var
        else cli_config.emb_model_default
    )

    # Shared CLI surface used by both backend scripts.
    parser = argparse.ArgumentParser(description=cli_config.description)
    parser.add_argument(
        "class_name_full",
        help="Fully qualified class name of the CrySL rule (e.g., java.security.AlgorithmParameters)",
    )
    parser.add_argument("language", help="Explanation language (e.g., English)")
    parser.add_argument("--model", "-m", default=model_default, help=cli_config.model_help)
    parser.add_argument(
        "--pdf",
        default=cli_config.pdf_default,
        help=f"Path to the CrySL paper PDF for RAG (default: {cli_config.pdf_default})",
    )
    parser.add_argument(
        "--k",
        type=int,
        default=cli_config.k_default,
        help="How many chunks to retrieve from the paper for context",
    )
    parser.add_argument(
        "--emb-model",
        default=emb_model_default,
        help=cli_config.emb_model_help,
    )
    args = parser.parse_args()

    class_name_full = args.class_name_full
    language = args.language

    # Java side provides FQCN; rules are stored by simple class name.
    simple_name = class_name_full.rsplit(".", 1)[-1]
    crysl_filename = f"{simple_name}.crysl"
    crysl_full_path = rules_dir / crysl_filename

    if not crysl_full_path.is_file():
        print(f"{crysl_filename} not found in {rules_dir}.", file=sys.stderr)
        return

    # Create provider client before rule processing; RAG remains optional.
    client = init_client_fn()
    idx = None
    chunks = None
    try:
        if args.pdf and Path(args.pdf).exists():
            # Build/load provider-specific paper index for optional contextual grounding.
            idx, chunks = build_pdf_index_fn(args.pdf, emb_model=args.emb_model)
        else:
            print(f"[INFO] Skipping RAG: PDF not found at {args.pdf}", file=sys.stderr)
    except Exception as e:
        print(f"[WARN] RAG disabled (index build/load failed): {e}", file=sys.stderr)

    result = process_rule_fn(
        str(crysl_full_path),
        language,
        client,
        args.model,
        class_name_full,
        idx=idx,
        chunks=chunks,
        k=args.k,
        emb_model=args.emb_model,
    )

    # Exit non-zero when nothing was produced. process_rule_core already reported the cause
    # on stderr, but returning 0 with empty stdout made Java treat the failure as a valid
    # (empty) explanation and cache it, so a run where every call failed still looked like
    # a clean run. A non-zero exit makes runSidecar raise, which the per-rule handler logs.
    if result is None or not str(result).strip():
        sys.exit(1)
