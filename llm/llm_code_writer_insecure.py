import argparse
import json
import os
import sys
from typing import Optional

from openai import OpenAI

from utils.gateway_rate_limit import call_with_concurrency_backoff, wait_for_gateway_slot
from utils.llm_env import (
    client_kwargs,
    get_gateway_base_url,
    get_gateway_chat_model,
    get_openai_chat_model,
    load_llm_env,
)

"""
    @author: Roshan Samantaray
"""

# Load environment variables for API access.
load_llm_env()


def _require_env(var_name: str) -> str:
    value = os.getenv(var_name, "").strip()
    if not value:
        raise RuntimeError(f"{var_name} is not set.")
    return value


def _build_client(backend: str) -> OpenAI:
    if backend == "openai":
        return OpenAI(api_key=_require_env("OPENAI_API_KEY"), **client_kwargs())
    api_key = _require_env("GATEWAY_API_KEY")
    base_url = get_gateway_base_url()
    return OpenAI(api_key=api_key, base_url=base_url, **client_kwargs())


def _resolve_chat_model(backend: str, cli_model: Optional[str]) -> str:
    if cli_model and cli_model.strip():
        return cli_model.strip()
    if backend == "openai":
        return get_openai_chat_model()
    return get_gateway_chat_model()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate insecure Java examples from CrySL payloads.")
    parser.add_argument("json_path", help="Path to the temp JSON produced by the Java pipeline.")
    parser.add_argument(
        "--rules-dir",
        default="",
        help="Unused in insecure writer; accepted for Java-side argument compatibility.",
    )
    parser.add_argument(
        "--backend",
        choices=["openai", "gateway"],
        required=True,
        help="LLM backend selected by the Java pipeline.",
    )
    parser.add_argument(
        "--model",
        default=None,
        help=(
            "Override chat model. If unset, resolve from OPENAI_CHAT_MODEL or "
            "GATEWAY_CHAT_MODEL in llm/.env (with built-in fallbacks)."
        ),
    )
    return parser.parse_args()


# Build a prompt that instructs the LLM to violate the CrySL rule intentionally.
def build_insecure_prompt(rule: dict) -> str:
    return f'''
    You are a Java coding assistant.

    Your task is to generate an **insecure** Java code example using the class `{rule.get('className', 'the target class')}`.
    
    The CrySL rule below defines correct and secure usage. However, your goal is to create a code snippet that violates this rule while still being syntactically valid:
    
    Objects: {rule.get('objects', 'N/A')}
    Events: {rule.get('events', 'N/A')}
    Order: {rule.get('order', 'N/A')}
    Constraints: {rule.get('constraints', 'N/A')}
    Requires: {rule.get('requires', 'N/A')}
    Ensures: {rule.get('ensures', 'N/A')}
    Forbidden Methods: {rule.get('forbidden', 'N/A')}
    
    Guidelines:
    - Use parameter values that are *not* listed as valid (e.g., for RSA key size, use 1024 or 2048 instead of 3072 or 4096).
    - Break the expected method call order (e.g., call `generateKeyPair()` before `initialize()`).
    - Use any forbidden methods mentioned, if applicable.
    - Do NOT satisfy the required conditions or methods in the rule.
    
    Output Style:
    - The code must be valid Java and look realistic.
    - Include inline comments using `//` to explain **why each choice is insecure**.
      - Example: `// 2048-bit RSA is too weak for secure usage, even though valid Java`
    - Output only the annotated Java code — no extra explanation or text.
    
    Your goal is to help demonstrate **what insecure code might look like** to compare with a secure version.
    '''.strip()


# CLI entrypoint: load rule JSON, build prompt, call LLM, print Java.
def main():
    args = parse_args()
    try:
        with open(args.json_path, "r", encoding="utf-8") as f:
            rule = json.load(f)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Error in Insecure Code Generation: cannot read {args.json_path}: {exc}", file=sys.stderr)
        sys.exit(1)
    if not isinstance(rule, dict):
        print("Error in Insecure Code Generation: payload is not a JSON object.", file=sys.stderr)
        sys.exit(1)

    # Decide secure or insecure (this script expects insecure by default)
    example_type = rule.get("exampleType", "insecure").lower()
    label = "insecure" if "insecure" in example_type else "secure"

    if label != "insecure":
        print("Error in Insecure Code Generation: expected insecure payload.", file=sys.stderr)
        sys.exit(1)

    prompt = build_insecure_prompt(rule)
    try:
        model = _resolve_chat_model(args.backend, args.model)
        client = _build_client(args.backend)
    except Exception as exc:
        print(f"Backend/model configuration error: {exc}", file=sys.stderr)
        sys.exit(1)

    if args.backend == "gateway":
        wait_for_gateway_slot("chat.completions")
    try:
        response = call_with_concurrency_backoff(
            lambda: client.chat.completions.create(
                model=model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
            ),
            "chat.completions",
        )
    except Exception as exc:
        print(f"Error in Insecure Code Generation: request failed: {exc}", file=sys.stderr)
        sys.exit(1)

    # A refused or filtered completion has content=None; printing it would hand the
    # Java pipeline the literal text "None" as if it were generated code.
    choices = getattr(response, "choices", None) or []
    message = getattr(choices[0], "message", None) if choices else None
    content = getattr(message, "content", None) if message is not None else None
    if not content or not content.strip():
        print(
            "Error in Insecure Code Generation: the model returned no content "
            "(refused, filtered, or empty completion).",
            file=sys.stderr,
        )
        sys.exit(1)

    # Output generated Java code
    print(content)


# Standard entry guard for CLI usage.
if __name__ == "__main__":
    main()
