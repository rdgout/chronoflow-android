#!/bin/bash
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
PERF_SUITE="release-smoke"
source Scripts/perf-device.sh
resolve_perf_device || exit $?
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
IS_EMU=0
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$("$ADB" -s "$SERIAL" shell getprop "$prop" | tr -d '\r')" = 1 ]; then IS_EMU=1; fi
done
if [ "$IS_EMU" != 1 ] && [ "${PERF_CLEAR:-0}" != 1 ]; then
  echo "release-smoke clears app data: set PERF_CLEAR=1" >&2
  exit 2
fi
trap '"$EMULATOR_SH" prep-functional "$SERIAL"' EXIT
"$EMULATOR_SH" prep-functional "$SERIAL" || exit $?
args=(
  :benchmark:connectedBenchmarkReleaseAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.ReleaseSmokeTest
  -Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000
)
if [ "${PERF_CLEAR:-0}" = 1 ]; then args+=(-Pandroid.testInstrumentationRunnerArguments.cf.perfClear=1); fi
if [ -n "${RELEASE_SMOKE_DRY_RUN:-}" ]; then
  echo "+ $GRADLEW --console=plain ${args[*]}"
else
  "$GRADLEW" --console=plain "${args[@]}"
fi
