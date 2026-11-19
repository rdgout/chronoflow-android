#!/bin/bash
# Runs StartupBenchmark on the connected device and checks it with check-benchmarks.py.
# PERF_STARTUP_DRY_RUN=1 prints the Gradle and checker commands instead of running them.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
CHECKER="${CHECK_BENCHMARKS:-python3 -I Scripts/check-benchmarks.py}"
STATE_DIR="${CF_EMULATOR_STATE_DIR:-$ROOT/build/emulator}"
RESULTS_DIR="benchmark/build/outputs/connected_android_test_additional_output"
HISTORY="docs/perf-startup.md"

dev() { "$ADB" -s "$SERIAL" "$@"; }
step() {
  if [ -n "${PERF_STARTUP_DRY_RUN:-}" ]; then
    echo "+ $*"
  else
    "$@"
  fi
}

attached="$("$ADB" devices | grep -Ec '[[:space:]]device$' || true)"
if [ "$attached" != 1 ]; then
  echo "perf-startup: exactly one device must be attached, found $attached" >&2
  exit 1
fi

IS_EMU=0
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$(dev shell getprop "$prop" | tr -d '\r')" = 1 ]; then IS_EMU=1; fi
done
GPU=unknown
if [ "$IS_EMU" = 1 ]; then
  CLASS=emulator
  REFRESH_HZ=60
  if [ -f "$STATE_DIR/gpu-mode" ]; then GPU="$(tr -d '[:space:]' <"$STATE_DIR/gpu-mode")"; fi
  if [ "$GPU" != host ]; then
    echo "perf-startup: the emulator must run with -gpu host (found '$GPU'); boot it with make emu" >&2
    exit 1
  fi
else
  CLASS="${DEVICE_CLASS:-reference}"
  REFRESH_HZ=unknown
fi

rm -rf "$RESULTS_DIR"
trap '"$EMULATOR_SH" prep-functional' EXIT
"$EMULATOR_SH" prep-perf
scale="$(dev shell settings get global animator_duration_scale | tr -d '\r')"
if [ "$scale" != 1 ] && [ "$scale" != 1.0 ]; then
  echo "perf-startup: animator_duration_scale is '$scale', not 1; a perf run with the scales off measures nothing" >&2
  exit 1
fi

args=(
  ":benchmark:connectedBenchmarkReleaseAndroidTest"
  "-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.StartupBenchmark"
  "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark"
  "-Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000"
)
if [ "$IS_EMU" = 1 ]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR")
fi
step "$GRADLEW" --console=plain "${args[@]}"
gradle_status=$?

commit="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
if [ -n "$(git status --porcelain 2>/dev/null | grep -v " $HISTORY\$" || true)" ]; then commit="$commit-dirty"; fi
font="$(dev shell settings get system font_scale | tr -d '\r')"
case "$font" in null | "") font=1.0 ;; esac
# shellcheck disable=SC2086
step $CHECKER --suite startup --device-class "$CLASS" --results-dir "$RESULTS_DIR" --history "$HISTORY" \
  --theme light --font-scale "$font" --refresh-hz "$REFRESH_HZ" --gpu "$GPU" --commit "$commit" --append
check_status=$?

if [ "$gradle_status" != 0 ]; then exit "$gradle_status"; fi
exit "$check_status"
