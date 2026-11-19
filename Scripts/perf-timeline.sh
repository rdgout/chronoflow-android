#!/bin/bash
# Runs TimelineBenchmark on the connected device and checks it with check-benchmarks.py.
# PERF_TIMELINE_DRY_RUN=1 prints the Gradle and checker commands instead of running them.
# THEME=light|dark and FONT_SCALE=<x> pick the configuration; DEVICE_CLASS=reference|lowEnd applies on a physical device.
# PERF_GRADLE_ARGS adds Gradle words; PERF_NO_APPEND=1 keeps the row out of the history.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
PERF_SUITE="perf-timeline"
source Scripts/perf-device.sh
resolve_perf_device || exit $?
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
CHECKER="${CHECK_BENCHMARKS:-python3 -I Scripts/check-benchmarks.py}"
STATE_DIR="${CF_EMULATOR_STATE_DIR:-$ROOT/build/emulator}"
PERF_OUTPUT_DIR="${PERF_OUTPUT_DIR:-benchmark/build/outputs/connected_android_test_additional_output}"
source Scripts/perf-archive.sh
HISTORY="docs/perf-timeline.md"
THEME="${THEME:-light}"
FONT_SCALE="${FONT_SCALE:-1.0}"
export THEME FONT_SCALE

dev() { "$ADB" -s "$SERIAL" "$@"; }
step() {
  if [ -n "${PERF_TIMELINE_DRY_RUN:-}" ]; then
    echo "+ $*"
  else
    "$@"
  fi
}



IS_EMU=0
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$(dev shell getprop "$prop" | tr -d '\r')" = 1 ]; then IS_EMU=1; fi
done
GPU=unknown
if [ "$IS_EMU" = 1 ]; then
  CLASS=emulator
  HZ=60
  if [ -f "$STATE_DIR/gpu-mode" ]; then GPU="$(tr -d '[:space:]' <"$STATE_DIR/gpu-mode")"; fi
  if [ "$GPU" != host ]; then
    echo "perf-timeline: the emulator must run with -gpu host (found '$GPU'); boot it with make emu" >&2
    exit 1
  fi
else
  CLASS="${DEVICE_CLASS:-reference}"
  HZ="$(dev shell dumpsys display | grep -Eo 'renderFrameRate=[0-9.]+' | head -1 | cut -d= -f2 | cut -d. -f1)"
  HZ="${HZ:-unknown}"
fi

# The benchmark is defined in en; any other locale would change the text the timeline draws.
case "$(dev shell getprop persist.sys.locale | tr -d '\r')" in
  nl*)
    echo "perf-timeline: the device locale is nl; the timeline benchmark runs in en" >&2
    exit 1
    ;;
esac

cleanup() {
  dev shell wm size reset
  "$EMULATOR_SH" prep-functional "$SERIAL"
}
trap cleanup EXIT
"$EMULATOR_SH" prep-perf "$SERIAL"
prep_status=$?
if [ "$prep_status" != 0 ]; then
  echo "perf-timeline: prep-perf failed with status $prep_status" >&2
  exit "$prep_status"
fi
scale="$(dev shell settings get global animator_duration_scale | tr -d '\r')"
if [ "$scale" != 1 ] && [ "$scale" != 1.0 ]; then
  echo "perf-timeline: animator_duration_scale is '$scale', not 1; a perf run with the scales off measures nothing" >&2
  exit 1
fi

args=(
  ":benchmark:connectedBenchmarkReleaseAndroidTest"
  "-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.TimelineBenchmark"
  "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark"
  "-Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000"
)
if [ "$IS_EMU" = 1 ]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR")
fi
if [ -n "${PERF_GRADLE_ARGS:-}" ]; then
  # shellcheck disable=SC2206
  args+=(${PERF_GRADLE_ARGS})
fi

commit="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
if [ -n "$(git status --porcelain 2>/dev/null | grep -v " $HISTORY\$" | grep -v '^?? docs/phases/' || true)" ]; then commit="$commit-dirty"; fi
checker_args=(--suite timeline --device-class "$CLASS" --history "$HISTORY"
  --theme "$THEME" --font-scale "$FONT_SCALE" --refresh-hz "$HZ" --gpu "$GPU" --commit "$commit")
if [ -z "${PERF_NO_APPEND:-}" ]; then checker_args+=(--append); fi

run_once() {
  begin_perf_archive timeline "$THEME-$FONT_SCALE-bounded-swipe-v3" "$commit" || exit 1
  step "$GRADLEW" --console=plain "${args[@]}"
  gradle_status=$?
  finish_perf_archive || exit 1
  # shellcheck disable=SC2086
  step $CHECKER "${checker_args[@]}" --results-dir "$RESULTS_DIR" --row-file "$PERF_ARCHIVE/row.md"
  check_status=$?
}

run_once
if [ "$check_status" = 4 ] && [ "$gradle_status" = 0 ]; then
  echo "perf-timeline: band miss; re-running once"
  run_once
fi

if [ "$gradle_status" != 0 ]; then exit "$gradle_status"; fi
exit "$check_status"
