# CrySL DOC
CrySL DOC generates HTML documentation for cryptographic APIs from CrySL rules.

It converts formal CrySL usage specifications into developer-facing pages (overview, call order, constraints, predicates, dependency trees, CrySL rule text), and can optionally enrich those pages with:
- LLM explanations (English, Portuguese, German, French)
- secure and insecure Java code examples

The documentation entry page is `rootpage.html` inside your configured output directory.

## What This Project Contains
- Java documentation pipeline (`src/main/java/de/upb/docgen/**`)
- FreeMarker templates for HTML rendering (`src/main/resources/FTLTemplates/**`)
- Language templates used to build natural-language rule text (`src/main/resources/Templates/**`)
- Bundled CrySL rules (`src/main/resources/CrySLRules/**`)
- Python LLM sidecar for explanation/code generation (`llm/**`)

## Prerequisites
- Java 21 (JDK)
- Maven
- Python 3 (required only for LLM features)

If your system default Java is older than 21, set `JAVA_HOME` before build/run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build
```bash
mvn clean install
```

or

```bash
mvn -DskipTests package
```

Generated JAR:
- `target/DocGen-0.0.1-SNAPSHOT.jar`
- runtime dependencies in `target/lib/` (keep this folder next to the JAR when running outside the project root)

## Quick Start
Recommended first run (no API keys required, faster local sanity check):

```bash
mvn -DskipTests package && \
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=off
```

Open:
- `/absolute/path/to/Output/rootpage.html`

Minimal run command (only required flag is `--reportPath`):

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar --reportPath /absolute/path/to/Output
```

Note:
- By default, LLM explanations/examples are enabled. If backend/API configuration is missing, the run may still complete but emit LLM errors/placeholders.

## Common Copy-Paste Runs
Generate docs with LLM disabled:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=off
```

Generate docs with LLM enabled using OpenAI backend:

```bash
set -a; source llm/.env; set +a
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=on \
  --llm-backend=openai
```

Generate docs with LLM enabled using Gateway backend:

```bash
set -a; source llm/.env; set +a
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=on \
  --llm-backend=gateway
```

## Run Consistency (IDE + CLI)
Canonical reproducible run path:

```bash
mvn clean compile
mvn -DskipTests package
java -jar target/DocGen-0.0.1-SNAPSHOT.jar --reportPath /absolute/path/to/Output
```

For IntelliJ runs:
- Reload Maven project after dependency changes.
- Rebuild the project before running (`Build > Rebuild Project`).
- If startup preflight reports missing classes, rebuild first and ensure the Maven classpath is active.
## Running CrySLDoc from the Command Line

The IntelliJ run configuration can be reproduced with a small Bash script.

### Prerequisite

Make sure the project is run with **Java 21**.  
The main class `de.upb.docgen.DocumentGeneratorMain` is compiled for class file version **65.0**, which corresponds to Java 21.

You can verify your Java version with:

```bash
java -version
mvn -version
````
Make it executable and run it with:

```bash
chmod +x run_crysldoc.sh
./run_crysldoc.sh
```

### Notes

* This script is the command-line equivalent of the IntelliJ **CrySLDoc Generation** run configuration.
* If Java 17 or lower is used, the application will fail with `UnsupportedClassVersionError`.
* Adjust `JAVA_HOME` if Java 21 is installed in a different location on your system.

## CLI Usage
### Required
- `--reportPath <output_dir>`

### Optional input/template overrides
- `--rulesDir <path_to_crysl_rules>`
- `--ftlTemplatesPath <path_to_ftl_templates>`
- `--langTemplatesPath <path_to_lang_templates>`

### Optional UI/content toggles
These are switch flags (pass the flag to toggle behavior):
- `--booleanA` hide state machine graph
- `--booleanB` hide help button
- `--booleanC` hide dependency tree sections
- `--booleanD` hide CrySL rule section
- `--booleanE` turn off Graphviz state-machine generation (no DOT is emitted and the graph section is omitted)
- `--booleanF` copy CrySL rules into `<reportPath>/rules/`
- `--booleanG` use fully qualified method labels in state machine edges

### Optional LLM toggles
- `--disable-llm-explanations`
- `--disable-llm-examples`
- `--llm=<on|off|true|false|1|0>`
- `--llm-explanations=<on|off|true|false|1|0>`
- `--llm-examples=<on|off|true|false|1|0>`
- `--llm-backend=<openai|gateway>`

## Environment Variables

These are read directly by the code and are not exposed as CLI flags. All are optional;
the defaults are what a normal run uses.

### Secure-example compile gate

Generated secure examples are compiled with `javac` before being published, and are
regenerated if they fail. These control that gate.

| Variable | Default | Effect |
|---|---|---|
| `CRYSLDOC_COMPILE_CHECK` | `1` | Set to `0` to **skip the compile check entirely**. Generated code is then published without ever being compiled — the gate is the only thing preventing broken examples from reaching the documentation, so turn it off deliberately or not at all. |
| `CRYSLDOC_MAX_REPAIRS` | `7` | Repair rounds attempted when an example fails to compile. Each round is another chat completion, so this is the main lever on API cost per failing rule. |
| `CRYSLDOC_COMPILE_CLASSPATH` | *(empty)* | Extra classpath entries for `javac`, separated by the platform path separator. The gate otherwise compiles against the JDK alone, so a rule documenting a non-JDK type needs its jar here — `javax.servlet.http.Cookie` is the one bundled rule in that position. `run_crysldoc.sh` sets this automatically. |
| `CRYSLDOC_COMPILE_USE_RUNTIME_CLASSPATH` | unset | Set to `1` to compile against this project's own full dependency tree instead of the JDK. Weakens the check — an example can then compile here but not for a reader — and exists only as an escape hatch. |
| `CRYSLDOC_JAVAC_REQUIRED` | `1` | Set to `0` to continue when no `javac` is found rather than failing. |
| `CRYSLDOC_COMPILE_STRICT` | `0` | Set to `1` to treat a missing compiler as a hard error even when `CRYSLDOC_JAVAC_REQUIRED=0`. |
| `JAVAC_BIN` | resolved from `java.home` | Explicit path to `javac`, for CI images where it is not alongside `java`. |

### Timeouts and retries

| Variable | Default | Effect |
|---|---|---|
| `CRYSLDOC_LLM_EXPLANATION_TIMEOUT_SECONDS` | `300` | Wall-clock ceiling for one explanation sidecar call. |
| `CRYSLDOC_LLM_EXAMPLE_TIMEOUT_SECONDS` | `900` | Ceiling for one example call. Larger because a secure example may run the compile-and-repair loop, which is several completions plus several `javac` invocations. |
| `LLM_MAX_RETRIES` | `4` | Client-side retries on transient API errors (429, 5xx). |
| `LLM_TIMEOUT_SECONDS` | `90` | Per-request client timeout. |
| `GATEWAY_RPM` | `10` | Request-rate ceiling for the gateway backend, enforced across processes. |

### Diagnostics

| Variable | Default | Effect |
|---|---|---|
| `CRYSLDOC_DEBUG` | unset | Set to `1` to print the shaped CrySL contract sent to the model, per rule, on stderr. |

Model and credential variables (`OPENAI_API_KEY`, `GATEWAY_API_KEY`, `OPENAI_CHAT_MODEL`,
and so on) belong in `llm/.env` — see [LLM Setup](#llm-setup-optional).

## Example Commands
Disable all LLM features:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=off
```

Enable LLM with OpenAI backend:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=on \
  --llm-backend=openai
```

Use custom rule/template directories:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --rulesDir /absolute/path/to/rules \
  --ftlTemplatesPath /absolute/path/to/ftl \
  --langTemplatesPath /absolute/path/to/lang/templates
```

Use Gateway backend:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm-backend=gateway
```

## LLM Setup (Optional)
The Java pipeline invokes Python scripts in `llm/` for LLM explanations/examples.

Create and install Python environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### OpenAI backend
Copy `llm/.env.example` to `llm/.env`, then set:

```bash
OPENAI_API_KEY=<your_key>
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_EMB_MODEL=text-embedding-3-small
```

Set:

```bash
export OPENAI_API_KEY=<your_key>
```

Use:
- `--llm-backend=openai`

Notes:
- OpenAI backend is used for explanations and code examples.
- `OPENAI_API_KEY` is required when running with `--llm-backend=openai`.
- `OPENAI_CHAT_MODEL` and `OPENAI_EMB_MODEL` can live in `llm/.env`; the code writers use them as defaults when `--model` / `--emb-model` are not provided.

### Gateway backend (UPB AI-Gateway)
Set in `llm/.env` or export directly:

```bash
GATEWAY_API_KEY=<your_gateway_key>
GATEWAY_BASE_URL=https://ai-gateway.uni-paderborn.de/v1/
GATEWAY_CHAT_MODEL=gwdg.qwen3-30b-a3b-instruct-2507
GATEWAY_EMB_MODEL=<gateway_embedding_model>
GATEWAY_RPM=10
```

Set:

```bash
export GATEWAY_API_KEY=<your_gateway_key>
# optional (defaults to UPB gateway URL):
export GATEWAY_BASE_URL=https://ai-gateway.uni-paderborn.de/v1/
# optional (fallback default is used if unset):
export GATEWAY_CHAT_MODEL=<gateway_chat_model>
# required for secure example generation:
export GATEWAY_EMB_MODEL=<gateway_embedding_model>
# optional (default 10):
export GATEWAY_RPM=10
```

Use:
- `--llm-backend=gateway`

Discover available gateway models:

```bash
python3 llm/llm_writer_gateway.py --list-models
```

Notes:
- Gateway backend is used for both explanations and secure/insecure code examples.
- Example scripts are invoked internally with `--backend=<openai|gateway>` from Java; no fallback to OpenAI is performed in gateway mode.
- For gateway-backed secure examples, `GATEWAY_EMB_MODEL` is required.
- The secure/insecure code writers read `GATEWAY_BASE_URL` and `GATEWAY_CHAT_MODEL` from `llm/.env` first, then fall back to the built-in UPB defaults.
- Gateway requests (chat + embeddings, including examples) are throttled client-side using a shared cross-process limiter with default `GATEWAY_RPM=10` (set `GATEWAY_RPM` to override).

### First-time run (required for LLM features)

Before setting API keys, run a one-time preprocessing pass to generate **sanitized CrySL rule JSONs** (one per `.crysl` file per language). These are written to `llm/sanitized_rules/` and are consumed by the Python sidecar.

Important: sanitized JSON generation is tied to the LLM explanation flow in `DocSettings`, so it must run with `--llm=on` (or explicitly `--llm-explanations=on`).

1. **Run once without API keys, but with LLM enabled** (this generates `llm/sanitized_rules/*`):

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=on \
  --llm-examples=off
```

Expected behavior in this step:
- Sanitized rule JSON files are generated under `llm/sanitized_rules/`.
- Explanation calls may fail due to missing API keys; this is expected for this preprocessing run.

2. **Delete the generated output folder** (so the next run starts clean):

```bash
rm -rf /absolute/path/to/Output
```

3. **Configure your backend/API keys, then run again with LLM features as needed**:

```bash
java -jar target/DocGen-0.0.1-SNAPSHOT.jar \
  --reportPath /absolute/path/to/Output \
  --llm=on
```

> Tip: you can enable/disable explanations/examples independently with `--llm-explanations=...` and `--llm-examples=...`.

## Output and Cache Directories
Primary output is written to `--reportPath`.

Common generated folders:
- `<reportPath>/composedRules/` (one HTML page per class)
- `<reportPath>/resources/llm_cache/` (cached explanations)
- `<reportPath>/resources/code_cache/` (cached secure/insecure examples)
- `llm/sanitized_rules/` (sanitized rule JSON for LLM scripts)
- `rag_cache/` (cached embeddings/chunks for PDF retrieval)

Code cache cleanup helper (optional):
- `python3 scripts/delete_disabled_code_cache_files.py --report-path <reportPath>`
- Also remove explanation placeholders (`LLM explanations disabled by flag.`): `python3 scripts/delete_disabled_code_cache_files.py --report-path <reportPath> --also-delete-disabled-explanations`
- Optional explicit override: `python3 scripts/delete_disabled_code_cache_files.py --cache-dir <absolute_cache_dir>`

## Tests
Java tests:

```bash
mvn -q test
```

Python tests:

```bash
pytest -q llm/tests
```

## Notes
- If no override paths are provided, bundled resources are used from `src/main/resources/**`.
- LLM flags can overlap; later CLI args may override earlier ones.
- The project includes historical thesis context, but this README reflects current implementation behavior.
