#!/bin/bash
# Runs StartupBenchmark on the connected device and checks it with check-benchmarks.py.
# PERF_STARTUP_DRY_RUN=1 prints the Gradle and checker commands instead of running them.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
PERF_SUITE="perf-startup"
source Scripts/perf-device.sh
resolve_perf_device || exit $?
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
CHECKER="${CHECK_BENCHMARKS:-python3 -I Scripts/check-benchmarks.py}"
STATE_DIR="${CF_EMULATOR_STATE_DIR:-$ROOT/build/emulator}"
PERF_OUTPUT_DIR="${PERF_OUTPUT_DIR:-benchmark/build/outputs/connected_android_test_additional_output}"
source Scripts/perf-archive.sh
HISTORY="docs/perf-startup.md"

dev() { "$ADB" -s "$SERIAL" "$@"; }
step() {
  if [ -n "${PERF_STARTUP_DRY_RUN:-}" ]; then
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


trap '"$EMULATOR_SH" prep-functional "$SERIAL"' EXIT
"$EMULATOR_SH" prep-perf "$SERIAL"
prep_status=$?
if [ "$prep_status" != 0 ]; then
  echo "perf-startup: prep-perf failed with status $prep_status" >&2
  exit "$prep_status"
fi
if [ "$IS_EMU" = 1 ] || [ "${PERF_CLEAR:-0}" = 1 ]; then
  step "$ADB" -s "$SERIAL" shell pm clear net.chronoflow.app.native || true
else
  echo "state: as found (set PERF_CLEAR=1 to clear app data)"
fi
scale="$(dev shell settings get global animator_duration_scale | tr -d '\r')"
if [ "$scale" != 1 ] && [ "$scale" != 1.0 ]; then
  echo "perf-startup: animator_duration_scale is '$scale', not 1; a perf run with the scales off measures nothing" >&2
  exit 1
fi

args=(
  ":benchmark:connectedBenchmarkReleaseAndroidTest"
  "-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.StartupBenchmark,net.chronoflow.benchmark.LaunchJankBenchmark,net.chronoflow.benchmark.ProcessDeathLaunchTest"
  "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark"
  "-Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000"
)
if [ "$IS_EMU" = 1 ]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR")
fi
commit="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
if [ -n "$(git status --porcelain 2>/dev/null | grep -v " $HISTORY\$" || true)" ]; then commit="$commit-dirty"; fi
begin_perf_archive startup "light-${FONT_SCALE:-1.0}" "$commit" || exit 1
step "$GRADLEW" --console=plain "${args[@]}"
gradle_status=$?
finish_perf_archive || exit 1

font="$(dev shell settings get system font_scale | tr -d '\r')"
case "$font" in null | "") font=1.0 ;; esac
# shellcheck disable=SC2086
step $CHECKER --suite startup --device-class "$CLASS" --results-dir "$RESULTS_DIR" --history "$HISTORY" \
  --theme light --font-scale "$font" --refresh-hz "$REFRESH_HZ" --gpu "$GPU" --commit "$commit" --append
check_status=$?
# shellcheck disable=SC2086
step $CHECKER --suite launch --device-class "$CLASS" --results-dir "$RESULTS_DIR" --history "$HISTORY" \
  --theme light --font-scale "$font" --refresh-hz "$REFRESH_HZ" --gpu "$GPU" --commit "$commit" --append
launch_status=$?
if [ "$launch_status" != 0 ]; then check_status="$launch_status"; fi

if [ "$gradle_status" != 0 ]; then exit "$gradle_status"; fi
exit "$check_status"
