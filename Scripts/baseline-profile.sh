#!/bin/bash
# Regenerates the baseline and startup profiles on the connected device.
# BASELINE_PROFILE_DRY_RUN=1 prints the Gradle command instead of running it.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
PERF_SUITE="baseline-profile"
source Scripts/perf-device.sh
resolve_perf_device || exit $?
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


IS_EMU=0
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$(dev shell getprop "$prop" | tr -d '\r')" = 1 ]; then
    IS_EMU=1
    GPU=unknown
    if [ -f "$STATE_DIR/gpu-mode" ]; then GPU="$(tr -d '[:space:]' <"$STATE_DIR/gpu-mode")"; fi
    if [ "$GPU" != host ]; then
      echo "baseline-profile: the emulator must run with -gpu host (found '$GPU'); boot it with make emu" >&2
      exit 1
    fi
    break
  fi
done

if [ "$IS_EMU" != 1 ] && [ "${PERF_CLEAR:-0}" != 1 ]; then
  echo "baseline-profile clears app data: set PERF_CLEAR=1" >&2
  exit 2
fi

cleanup() {
  dev shell wm size reset
  "$EMULATOR_SH" prep-functional "$SERIAL"
}
trap cleanup EXIT
"$EMULATOR_SH" prep-perf "$SERIAL"
prep_status=$?
if [ "$prep_status" != 0 ]; then
  echo "baseline-profile: prep-perf failed with status $prep_status" >&2
  exit "$prep_status"
fi
if [ "$IS_EMU" = 1 ] || [ "${PERF_CLEAR:-0}" = 1 ]; then
  step "$ADB" -s "$SERIAL" shell pm clear net.chronoflow.app.native || true
else
  echo "state: as found (set PERF_CLEAR=1 to clear app data)"
fi

# Three collection attempts; the generator derives its deadline from the per-iteration caps.
args=(
  --console=plain :app:generateBaselineProfile
  -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.BaselineProfileGenerator
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
  -Pandroid.testInstrumentationRunnerArguments.timeout_msec=900000
)
if [ "${PERF_CLEAR:-0}" = 1 ]; then args+=(-Pandroid.testInstrumentationRunnerArguments.cf.perfClear=1); fi
step "$GRADLEW" "${args[@]}"
status=$?
if [ "$status" != 0 ]; then exit "$status"; fi
if [ -n "${BASELINE_PROFILE_DRY_RUN:-}" ]; then exit 0; fi
for f in baseline-prof.txt startup-prof.txt; do
  if [ ! -s "$OUT/$f" ]; then
    echo "baseline-profile: $OUT/$f is missing or empty" >&2
    exit 1
  fi
done

python3 -I - "$OUT/baseline-prof.txt" "$OUT/startup-prof.txt" <<'PYTHON'
from pathlib import Path
import sys
profile = Path(sys.argv[1]).read_text()
required = ("TripPageKt", "TripPageScreenKt", "DayHeaderKt", "SummaryCardKt")
missing = [name for name in required if f"Lnet/chronoflow/feature/trip/page/{name};" not in profile]
if missing:
    raise SystemExit("baseline-profile: missing trip page coverage: " + ", ".join(missing))
for name in ("WideDayRowKt", "PinnedDayListKt", "WideListRowsKt", "TripScrollTracker"):
    if f"Lnet/chronoflow/feature/trip/wide/{name};" not in profile:
        raise SystemExit("baseline-profile: missing wide coverage: " + name)
if "Lnet/chronoflow/feature/trip/list/DayListRowKt;" not in profile:
    raise SystemExit("baseline-profile: missing list row coverage: DayListRowKt")
required_journeys = (
    "app/MainActivity", "core/plan/drawing/DayDrawingBuilder", "feature/trip/timeline/DayDrawingReplayKt",
    "feature/today/ui/TodayScreenKt", "feature/trips/TripsScreenKt",
    "feature/trip/page/TripPageScreenKt", "feature/trip/list/DayListRowKt",
    "core/ui/plan/StepSheetContentKt", "feature/wizard/ready/WizardReadyStepKt",
    "feature/launch/welcome/WelcomeScreenKt", "feature/auth/model/SignInModel",
)
for name in required_journeys:
    if f"Lnet/chronoflow/{name};" not in profile:
        raise SystemExit("baseline-profile: missing main journey coverage: " + name)
startup = Path(sys.argv[2]).read_text()
for name in ("app/MainActivity", "feature/today/ui/TodayScreenKt", "feature/launch/welcome/WelcomeScreenKt"):
    if f"Lnet/chronoflow/{name};" not in startup:
        raise SystemExit("baseline-profile: missing startup coverage: " + name)
PYTHON
