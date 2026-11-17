#!/bin/bash
# Regenerates the baseline and startup profiles on the connected device.
# BASELINE_PROFILE_DRY_RUN=1 prints the Gradle command instead of running it.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
STATE_DIR="${CF_EMULATOR_STATE_DIR:-$ROOT/build/emulator}"
OUT="app/src/main/generated/baselineProfiles"

dev() { "$ADB" -s "$SERIAL" "$@"; }
step() {
  if [ -n "${BASELINE_PROFILE_DRY_RUN:-}" ]; then
    echo "+ $*"
  else
    "$@"
  fi
}

attached="$("$ADB" devices | grep -Ec '[[:space:]]device$' || true)"
if [ "$attached" != 1 ]; then
  echo "baseline-profile: exactly one device must be attached, found $attached" >&2
  exit 1
fi
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$(dev shell getprop "$prop" | tr -d '\r')" = 1 ]; then
    GPU=unknown
    if [ -f "$STATE_DIR/gpu-mode" ]; then GPU="$(tr -d '[:space:]' <"$STATE_DIR/gpu-mode")"; fi
    if [ "$GPU" != host ]; then
      echo "baseline-profile: the emulator must run with -gpu host (found '$GPU'); boot it with make emu" >&2
      exit 1
    fi
    break
  fi
done

trap '"$EMULATOR_SH" prep-functional' EXIT
"$EMULATOR_SH" prep-perf

# Each collect runs up to 15 launches, far over the 60 s default; raise the cap rather than lower maxIterations.
step "$GRADLEW" --console=plain :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000
status=$?
if [ "$status" != 0 ]; then exit "$status"; fi
if [ -n "${BASELINE_PROFILE_DRY_RUN:-}" ]; then exit 0; fi
for f in baseline-prof.txt startup-prof.txt; do
  if [ ! -s "$OUT/$f" ]; then
    echo "baseline-profile: $OUT/$f is missing or empty" >&2
    exit 1
  fi
done
