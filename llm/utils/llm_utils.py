import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple


PROJECT_ROOT_DEFAULT = Path(__file__).resolve().parents[2]
SANITIZED_DIR_DEFAULT = PROJECT_ROOT_DEFAULT / "llm" / "sanitized_rules"
FILENAME_TEMPLATE_DEFAULT = "sanitized_rule_{fqcn}_{lang}.json"
# No mkdir here: this module only reads sanitized rules, and the Java side creates
# the directory before invoking the sidecar. Creating it at import time meant that
# merely importing this module - for --help, a test, or an editor - wrote to disk.


# Build the sanitized rule file path for a class/language pair.
def rule_path(
    fqcn: str,
    lang: str,
    sanitized_dir: Path = SANITIZED_DIR_DEFAULT,
    filename_template: str = FILENAME_TEMPLATE_DEFAULT,
) -> Path:
    """Return the sanitized-rule JSON path for a fully-qualified class and language."""
    sanitized_name = sanitized_dir / filename_template.format(fqcn=fqcn, lang=lang)
    return sanitized_name


# Read a JSON file with utf-8 and return dict or None.
def load_json(path: Path):
    """Load JSON from disk and return None with stderr warnings on failure."""
    try:
        with path.open(encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"[WARN] Missing file: {path}", file=sys.stderr)
    except Exception as e:
        print(f"[ERROR] Could not read {path}: {e}", file=sys.stderr)
    return None


# Normalize list items for display.
def clean_item(s):
    """Normalize a scalar/list item into a trimmed display-friendly string."""
    if not isinstance(s, str):
        return str(s)
    s2 = s.strip()
    if s2.startswith(","):
        s2 = s2.lstrip(",").strip()
    return s2


# Load dependency constraints for direct dependencies of the target rule.
def collect_dependency_constraints(
    target_fqcn: str,
    language: str,
    sanitized_dir: Path = SANITIZED_DIR_DEFAULT,
    filename_template: str = FILENAME_TEMPLATE_DEFAULT,
) -> Tuple[List[str], Dict[str, List[str]]]:
    """
    Load direct dependency constraint lists for `target_fqcn`.

    Returns:
    - deps_order: stable dependency traversal order for prompt rendering.
    - dep_to_constraints: dependency fqcn -> normalized list of constraints.
    """
    dep_to_constraints: Dict[str, List[str]] = {}
    deps_order: List[str] = []

    primary_path = rule_path(
        target_fqcn,
        language,
        sanitized_dir=sanitized_dir,
        filename_template=filename_template,
    )
    primary = load_json(primary_path)
    if not primary:
        return deps_order, dep_to_constraints

    deps = primary.get("dependency") or []
    seen = set()
    for dep in deps:
        if dep == target_fqcn or dep in seen:
            continue
        seen.add(dep)
        deps_order.append(dep)

        dep_path = rule_path(
            dep,
            language,
            sanitized_dir=sanitized_dir,
            filename_template=filename_template,
        )
        dep_json = load_json(dep_path)
        if not dep_json:
            dep_to_constraints[dep] = []
            continue

        constraints = dep_json.get("constraints")
        if constraints is None:
            constraints = dep_json.get("constraint")
        if constraints is None:
            constraints = []
        if not isinstance(constraints, list):
            constraints = [constraints]
        dep_to_constraints[dep] = [clean_item(c) for c in constraints]

    return deps_order, dep_to_constraints


# Render dependency constraints into a readable block for the prompt.
def format_dependency_constraints(deps_order: List[str], dep_to_constraints: Dict[str, List[str]]) -> str:
    """Render dependency constraints into a deterministic prompt block."""
    if not deps_order:
        return "No dependency constraints supplied."
    parts = []
    for dep in deps_order:
        constraints = dep_to_constraints.get(dep, []) or []
        if not constraints:
            parts.append(f"Dependency: {dep}\n  - (no constraints)")
        else:
            lines = "\n".join(f"  - {c}" for c in constraints)
            parts.append(f"Dependency: {dep}\n{lines}")
    return "\n\n".join(parts)


# Normalize inputs that can be list-or-string into a list of strings.
def _normalize_listish(value) -> List[str]:
    """Normalize list-or-scalar fields into a clean list of non-empty strings."""
    if value is None:
        return []
    if isinstance(value, list):
        return [clean_item(v) for v in value if str(v).strip()]
    if isinstance(value, str):
        v = value.strip()
        return [v] if v else []
    return [clean_item(value)]


# Collect ENSURES from dependency rules (depth-limited, cycle-safe).
def collect_dependency_ensures(
    primary_fqcn: str,
    language: str,
    depth: int = 1,
    sanitized_dir: Path = SANITIZED_DIR_DEFAULT,
    filename_template: str = FILENAME_TEMPLATE_DEFAULT,
) -> Tuple[List[str], Dict[str, List[str]]]:
    """Return (deps_order, dep_to_ensures) where dep_to_ensures maps fqcn -> list[str].
    depth=1 means direct dependencies only. Cycle-safe.
    """
    dep_to_ensures: Dict[str, List[str]] = {}
    deps_order: List[str] = []

    primary = load_json(
        rule_path(
            primary_fqcn,
            language,
            sanitized_dir=sanitized_dir,
            filename_template=filename_template,
        )
    )
    if not primary:
        return deps_order, dep_to_ensures

    # Start from direct dependencies declared by the primary sanitized rule.
    roots = primary.get("dependency") or []
    seen = {primary_fqcn}

    def visit(fqcn: str, cur_depth: int):
        """Depth-limited DFS that is cycle-safe via `seen`."""
        if fqcn in seen:
            return
        seen.add(fqcn)
        if fqcn not in deps_order:
            deps_order.append(fqcn)

        data = load_json(
            rule_path(
                fqcn,
                language,
                sanitized_dir=sanitized_dir,
                filename_template=filename_template,
            )
        )
        if not data:
            dep_to_ensures[fqcn] = []
            return

        dep_to_ensures[fqcn] = _normalize_listish(data.get("ensures"))

        # Recurse if requested
        if cur_depth < depth:
            for sub in _normalize_listish(data.get("dependency")):
                visit(sub, cur_depth + 1)

    for dep in roots:
        if isinstance(dep, str) and dep:
            visit(dep, 1)

    return deps_order, dep_to_ensures


# Render dependency ENSURES in a readable, developer-friendly block.
def format_dependency_ensures(primary_fqcn: str, deps_order: List[str], dep_to_ensures: Dict[str, List[str]]) -> str:
    """Render dependency ENSURES into a developer-facing explanatory markdown block."""
    if not deps_order:
        return f"No dependent component guarantees were available for {primary_fqcn}."

    lines = [
        f"### How related components influence {primary_fqcn}",
        (
            "Below are the guarantees (postconditions) that related classes provide when used correctly. "
            "Use these to explain why and how the primary class depends on them.\n"
        ),
    ]
    for fqcn in deps_order:
        ensures = dep_to_ensures.get(fqcn, []) or []
        if not ensures:
            lines.append(f"- **{fqcn}**: *(no ensures available or file missing)*")
            continue
        lines.append(f"- **{fqcn}**:")
        lines.extend([f"  - {e}" for e in ensures])
    return "\n".join(lines)


# Split raw CrySL text into a dict of section -> lines.
def crysl_to_json_lines(crysl_text: str) -> Dict[str, List[str]]:
    """Split raw CrySL text into canonical section -> non-empty lines mapping."""
    sections = [
        "SPEC",
        "OBJECTS",
        "EVENTS",
        "ORDER",
        "CONSTRAINTS",
        "REQUIRES",
        "ENSURES",
        "FORBIDDEN",
    ]
    pat = re.compile(r"\b(" + "|".join(sections) + r")\b")
    matches = list(pat.finditer(crysl_text))
    out: Dict[str, List[str]] = {}
    for i, m in enumerate(matches):
        header = m.group(1)
        start = m.end()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(crysl_text)
        raw_lines = crysl_text[start:end].strip().splitlines()
        lines = [line.strip() for line in raw_lines if line.strip()]
        out[header] = lines
    return out


# Remove stray code fences from LLM output while keeping markdown headings.
def clean_llm_output(text: str) -> str:
    """Strip stray markdown code fences while preserving regular heading/content text."""
    # Keep Markdown headings; just strip stray code fences
    text = re.sub(r"^```(?:\w+)?\s*$", "", text, flags=re.MULTILINE)
    text = re.sub(r"^```\s*$", "", text, flags=re.MULTILINE)
    return text.strip()


# Convert a list-or-string section into displayable text.
def lines_to_text(section) -> str:
    """Convert section payloads (list/scalar/empty) into printable prompt text."""
    if isinstance(section, list):
        return "\n".join(section) if section else "_no entries_"
    return str(section) if section else "_no entries_"


# Ensure all expected rule fields exist and fill defaults where missing.
def validate_and_fill(rule: dict, language: str) -> dict:
    """Ensure expected CrySL sections exist and fill missing keys with defaults."""
    defaults = {
        "SPEC": "",
        "OBJECTS": "None",
        "EVENTS": "N/A",
        "ORDER": "N/A",
        "CONSTRAINTS": "N/A",
        "REQUIRES": "None",
        "ENSURES": "N/A",
        "FORBIDDEN": "N/A",
        "LANGUAGE": language,
    }
    for key, default in defaults.items():
        if key not in rule:
            rule[key] = default
    return rule


# Format sanitized rule fields into a readable prompt block.
def format_sanitized_rule_for_prompt(sanitized: dict) -> str:
    """Render sanitized JSON fields into a compact prompt-friendly multi-line summary."""
    if not sanitized:
        return "No sanitized fields supplied."
    exclude = {"dependency"}
    parts = []
    for key, val in sanitized.items():
        if key in exclude or val is None:
            continue
        k = str(key)
        if isinstance(val, list):
            if not val:
                continue
            lines = "\n".join(f"- {clean_item(it)}" for it in val)
            parts.append(f"{k}:\n{lines}")
        elif isinstance(val, dict):
            if not val:
                continue
            lines = "\n".join(f"- {ik}: {clean_item(iv)}" for ik, iv in val.items())
            parts.append(f"{k}:\n{lines}")
        else:
            parts.append(f"{k}: {clean_item(val)}")
    return "\n\n".join(parts) if parts else "No sanitized fields supplied."
