#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run_crysldoc.sh                 # build + generate with the defaults below
#   ./run_crysldoc.sh --llm=off       # extra args are appended, and later flags win,
#                                     # so this gives a fast run with no API calls
#   SKIP_BUILD=1 ./run_crysldoc.sh    # reuse the existing build (repeat timing runs)

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

MAIN_CLASS="de.upb.docgen.DocumentGeneratorMain"
VM_OPTS="-Dfile.encoding=UTF-8"
REPORT_PATH="Output"
PROGRAM_ARGS=(
  --FTLtemplatesPath src/main/resources/FTLTemplates
  --reportPath "${REPORT_PATH}"
  --llm=on
  --llm-backend=gateway
  "$@"
)

now() { date +%s.%N; }

# Print a duration as seconds, plus m/s breakdown once it passes a minute.
fmt() {
  awk -v s="$1" -v e="$2" 'BEGIN {
    d = e - s
    printf "%.2fs", d
    if (d >= 60) printf " (%dm %02ds)", int(d/60), int(d%60)
  }'
}

run_start=$(now)

# ---------------------------------------------------------------- build
build_elapsed="skipped"
if [[ "${SKIP_BUILD:-0}" == "1" && -d target/classes ]]; then
  echo "==> Build skipped (SKIP_BUILD=1)"
  CP="target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)"
else
  echo "==> Building"
  build_start=$(now)
  mvn -q clean compile
  CP="target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)"
  build_end=$(now)
  build_elapsed=$(fmt "$build_start" "$build_end")
  echo "    build took ${build_elapsed}"
fi

# The secure-example compile gate validates generated code against the JDK alone, which is
# what makes it meaningful - a reader of the documentation does not have this project's
# dependencies. One bundled rule documents a non-JDK type (javax.servlet.http.Cookie), so
# hand javac that one jar, taken from the resolved classpath rather than hardcoded.
SERVLET_JAR=$(printf '%s' "${CP}" | tr ':' '\n' | grep -i 'javax\.servlet-api' | head -1 || true)
if [[ -n "${SERVLET_JAR}" ]]; then
  export CRYSLDOC_COMPILE_CLASSPATH="${SERVLET_JAR}"
  echo "==> Compile gate: JDK + $(basename "${SERVLET_JAR}")"
else
  echo "==> Compile gate: JDK only (javax.servlet-api not found; the Cookie example will fail)"
fi

# ------------------------------------------------------------ generate
echo "==> Generating documentation into '${REPORT_PATH}'"
gen_start=$(now)
status=0
java ${VM_OPTS} -cp "${CP}" "${MAIN_CLASS}" "${PROGRAM_ARGS[@]}" || status=$?
gen_end=$(now)
run_end=$(now)

# ------------------------------------------------------------- summary
pages=$(find "${REPORT_PATH}/composedRules" -name '*.html' 2>/dev/null | wc -l | tr -d ' ')
per_page=""
if [[ "${pages}" -gt 0 ]]; then
  per_page=$(awk -v s="$gen_start" -v e="$gen_end" -v n="$pages" \
    'BEGIN { printf "%.2fs", (e - s) / n }')
fi

echo
echo "──────────────── timing ────────────────"
printf '  build      : %s\n' "${build_elapsed}"
printf '  generation : %s\n' "$(fmt "$gen_start" "$gen_end")"
printf '  total      : %s\n' "$(fmt "$run_start" "$run_end")"
if [[ -n "${per_page}" ]]; then
  printf '  pages      : %s  (%s per page)\n' "${pages}" "${per_page}"
else
  printf '  pages      : %s\n' "${pages}"
fi
echo "────────────────────────────────────────"
echo "(the generator prints its own internal total above; it excludes JVM startup and the build)"

exit "${status}"
