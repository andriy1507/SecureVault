#!/usr/bin/env bash
# Runs ktlint + detekt as standalone CLIs (project uses Kotlin Toolchain/Amper, not Gradle).
# Usage:
#   ./scripts/lint.sh          -> check (what CI runs)
#   ./scripts/lint.sh --fix    -> auto-format with ktlint, then run detekt
#   ./scripts/lint.sh --baseline -> write detekt baseline for existing issues
set -euo pipefail

KTLINT_VERSION="1.8.0"
DETEKT_VERSION="1.23.8"
COMPOSE_RULES_VERSION="0.6.8"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/.lint-cache"
REPORTS="$ROOT/build/reports/lint"
mkdir -p "$CACHE" "$REPORTS"

fetch() { [ -f "$2" ] || curl -fsSL -o "$2" "$1"; }

KTLINT="$CACHE/ktlint-$KTLINT_VERSION"
DETEKT="$CACHE/detekt-cli-$DETEKT_VERSION-all.jar"
DETEKT_COMPOSE="$CACHE/detekt-compose-$COMPOSE_RULES_VERSION-all.jar"
KTLINT_COMPOSE="$CACHE/ktlint-compose-$COMPOSE_RULES_VERSION-all.jar"

fetch "https://github.com/ktlint/ktlint/releases/download/$KTLINT_VERSION/ktlint" "$KTLINT"
chmod +x "$KTLINT"
fetch "https://github.com/detekt/detekt/releases/download/v$DETEKT_VERSION/detekt-cli-$DETEKT_VERSION-all.jar" "$DETEKT"
fetch "https://github.com/mrmans0n/compose-rules/releases/download/v$COMPOSE_RULES_VERSION/detekt-compose-$COMPOSE_RULES_VERSION-all.jar" "$DETEKT_COMPOSE"

cd "$ROOT"

# All Kotlin source folders: src, src@android, src@ios, test, test@android ...
INPUTS=$(find . -type d \( -name 'src' -o -name 'src@*' -o -name 'test' -o -name 'test@*' \) \
  -not -path './build/*' -not -path '*/build/*' -not -path './.lint-cache/*' -not -path './iosApp/*' \
  -not -path './.git/*' | sed 's|^\./||' | sort | paste -sd, -)

MODE="${1:-check}"
STATUS=0

[ "${SKIP_KTLINT:-}" = 1 ] || {
echo "▶ ktlint $KTLINT_VERSION"
KTLINT_ARGS=(--relative "**/src*/**/*.kt" "**/test*/**/*.kt" "!**/build/**" "!.lint-cache/**")
if [ "$MODE" = "--fix" ]; then
  "$KTLINT" -F "${KTLINT_ARGS[@]}" || STATUS=1
else
  "$KTLINT" "${KTLINT_ARGS[@]}" --reporter=plain --reporter=sarif,output="$REPORTS/ktlint.sarif" || STATUS=1
fi

}
echo "▶ detekt $DETEKT_VERSION"
DETEKT_ARGS=(--input "$INPUTS" --config config/detekt/detekt.yml --build-upon-default-config
  --plugins "$DETEKT_COMPOSE" --excludes "**/build/**" --base-path "$ROOT"
  --report "sarif:$REPORTS/detekt.sarif" --report "html:$REPORTS/detekt.html")
[ -f config/detekt/baseline.xml ] && DETEKT_ARGS+=(--baseline config/detekt/baseline.xml)
if [ "$MODE" = "--baseline" ]; then
  java -jar "$DETEKT" "${DETEKT_ARGS[@]}" --create-baseline --baseline config/detekt/baseline.xml || true
  echo "Baseline written to config/detekt/baseline.xml"
else
  java -jar "$DETEKT" "${DETEKT_ARGS[@]}" || STATUS=1
fi

[ $STATUS -eq 0 ] && echo "✅ Lint passed" || echo "❌ Lint failed (reports in build/reports/lint)"
exit $STATUS
