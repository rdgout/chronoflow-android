#!/bin/bash
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"
export ADB
PERF_SUITE="perf-compare-rn"
source Scripts/perf-device.sh
resolve_perf_device || exit $?
GRADLEW="${GRADLEW:-./gradlew}"
EMULATOR_SH="${EMULATOR_SH:-Scripts/emulator.sh}"
CHECKER="${CHECK_BENCHMARKS:-python3 -I Scripts/check-benchmarks.py}"
STATE_DIR="${CF_EMULATOR_STATE_DIR:-$ROOT/build/emulator}"
PERF_OUTPUT_DIR="${PERF_OUTPUT_DIR:-benchmark/build/outputs/connected_android_test_additional_output}"
source Scripts/perf-archive.sh
HISTORY="${PERF_COMPARE_HISTORY:-docs/perf-startup.md}"
dev() { "$ADB" -s "$SERIAL" "$@"; }
step() {
  if [ -n "${PERF_COMPARE_RN_DRY_RUN:-}" ]; then echo "+ $*"; else "$@"; fi
}
IS_EMU=0
for prop in ro.boot.qemu ro.kernel.qemu; do
  if [ "$(dev shell getprop "$prop" | tr -d '\r')" = 1 ]; then IS_EMU=1; fi
done
GPU=unknown
REFRESH_HZ=unknown
if [ "$IS_EMU" = 1 ]; then
  CLASS=emulator
  REFRESH_HZ=60
  if [ -f "$STATE_DIR/gpu-mode" ]; then GPU="$(tr -d '[:space:]' < "$STATE_DIR/gpu-mode")"; fi
  if [ "$GPU" != host ]; then echo "perf-compare-rn: emulator must run with -gpu host (found '$GPU')" >&2; exit 1; fi
else
  CLASS="${DEVICE_CLASS:-reference}"
  if [ "${PERF_CLEAR:-0}" != 1 ]; then echo "perf-compare-rn clears app data: set PERF_CLEAR=1" >&2; exit 2; fi
fi
RN_APK="${RN_APK:-/Users/rickgout/Sites/chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk}"
expected_sha="${RN_APK_SHA:-4ee240d3754a7bba12761ddcb4b8317b15d7131f3c55f93f314d64e89df3b1f5}"
refuse_apk() { echo "RN comparison open: needs a prebuilt RN release APK from the user (OQ-A591)" >&2; exit 3; }
[ -f "$RN_APK" ] || refuse_apk
sha="$(shasum -a 256 "$RN_APK" | awk '{print $1}')" || refuse_apk
printf '%s  %s\n' "$sha" "$RN_APK"
[ "$sha" = "$expected_sha" ] || refuse_apk
NATIVE_APK="${NATIVE_APK:-app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk}"
BENCHMARK_APK="${BENCHMARK_APK:-benchmark/build/outputs/apk/benchmarkRelease/benchmark-benchmarkRelease.apk}"
PACKAGE=net.chronoflow.app
uninstall_current() {
  if [ -n "${PERF_COMPARE_RN_DRY_RUN:-}" ]; then
    step "$ADB" -s "$SERIAL" uninstall "$PACKAGE"
  else
    local installed
    installed="$(dev shell pm path "$PACKAGE")"
    if [[ "$installed" == package:* ]]; then dev uninstall "$PACKAGE"; fi
  fi
}
restore_native=0
cleanup() {
  local status=$?
  if [ "$restore_native" = 1 ]; then
    if uninstall_current && step "$ADB" -s "$SERIAL" install -r "$NATIVE_APK"; then
      echo "perf-compare-rn: native benchmarkRelease reinstalled (app data reset)"
    else
      echo "perf-compare-rn: failed to restore native; the next native tier must reinstall it" >&2
      status=1
    fi
  fi
  "$EMULATOR_SH" prep-functional "$SERIAL" || status=1
  exit "$status"
}
trap cleanup EXIT
step "$GRADLEW" --console=plain :app:assembleBenchmarkRelease :benchmark:assembleBenchmarkRelease || exit $?
step "$ADB" -s "$SERIAL" install -r "$BENCHMARK_APK" || exit $?
"$EMULATOR_SH" prep-perf "$SERIAL" || exit $?
scale="$(dev shell settings get global animator_duration_scale | tr -d '\r')"
case "$scale" in 1|1.0) ;; *) echo "perf-compare-rn: animator_duration_scale is '$scale', not 1" >&2; exit 1 ;; esac
suppression=""
if [ "$IS_EMU" = 1 ]; then suppression=EMULATOR; fi
if [ "${RN_NOT_PROFILEABLE:-0}" = 1 ]; then suppression="${suppression:+$suppression,}NOT-PROFILEABLE"; fi
commit="$(git rev-parse --short HEAD)"
if [ -n "$(git status --porcelain 2>/dev/null | grep -v " $HISTORY\$" || true)" ]; then commit="$commit-dirty"; fi
begin_perf_archive compare "light-${FONT_SCALE:-1.0}" "$commit" || exit 1
step rm -f build/perf-rn/Native.xml build/perf-rn/Rn.xml
pass_status=0
restore_native=1
for target in Native Rn; do
  uninstall_current || exit $?
  apk="$NATIVE_APK"
  if [ "$target" = Rn ]; then apk="$RN_APK"; fi
  step "$ADB" -s "$SERIAL" install -r "$apk" || exit $?
  python3 -I Scripts/perf-compare-pass.py "$target" \
    --output "$PERF_OUTPUT_DIR/benchmarkRelease/$target" --report "build/perf-rn/$target.xml" \
    --suppression "$suppression"
  status=$?
  if [ "$status" != 0 ]; then pass_status="$status"; fi
done
finish_perf_archive build/perf-rn || exit 1
font="$(dev shell settings get system font_scale | tr -d '\r')"
case "$font" in ''|null) font=1.0 ;; esac
# shellcheck disable=SC2086
step $CHECKER --suite compare --device-class "$CLASS" --results-dir "$RESULTS_DIR" --history "$HISTORY" \
  --theme light --font-scale "$font" --refresh-hz "$REFRESH_HZ" --gpu "$GPU" --commit "$commit" --rn-apk-sha "$sha" --append
check_status=$?
fallback_status=0
if [ "${RN_AM_START_FALLBACK:-0}" = 1 ] && [ -z "${PERF_COMPARE_RN_DRY_RUN:-}" ]; then
  if python3 -I - "$RESULTS_DIR" <<'PYTHON'
import json
import math
import sys
from pathlib import Path
found = False
for path in Path(sys.argv[1]).rglob("*-benchmarkData.json"):
    for bench in json.loads(path.read_text()).get("benchmarks", []):
        if bench.get("name") == "coldSignedOut[Rn]":
            value = bench.get("metrics", {}).get("timeToInitialDisplayMs", {}).get("median")
            found = isinstance(value, (int, float)) and math.isfinite(value)
raise SystemExit(1 if found else 0)
PYTHON
  then
    fallback_file="$PERF_ARCHIVE/rn-am-start.jsonl"
    : > "$fallback_file"
    for iteration in 1 2 3 4 5 6 7 8 9 10; do
      dev shell pm clear net.chronoflow.app >/dev/null || true
      output="$(dev shell am start -W -S -n net.chronoflow.app/.MainActivity)" || exit 1
      total="$(printf '%s\n' "$output" | sed -n 's/^[[:space:]]*TotalTime:[[:space:]]*\([0-9][0-9]*\).*$/\1/p' | head -1)"
      case "$total" in ''|*[!0-9]*) echo "RN fallback: missing TotalTime" >&2; exit 1 ;; esac
      printf '{"iteration":%s,"rnAmStartTotalTimeMs":%s}\n' "$iteration" "$total" >> "$fallback_file"
    done
    python3 -I - "$fallback_file" "$HISTORY" "$commit" "$sha" "$CLASS" "$SERIAL" <<'PYTHON'
import datetime
import json
import math
import statistics
import sys
from pathlib import Path
file, history, commit, sha, device_class, serial = sys.argv[1:]
values = sorted(json.loads(line)["rnAmStartTotalTimeMs"] for line in Path(file).read_text().splitlines())
if len(values) != 10 or any(not math.isfinite(value) or value < 0 for value in values):
    raise SystemExit("RN fallback: missing or invalid samples")
position = 0.9 * (len(values) - 1)
low, high = math.floor(position), math.ceil(position)
p90 = values[low] + (values[high] - values[low]) * (position - low)
summary = "rnAmStartTotalTimeMs median=%.1f ms P90=%.1f ms, 10 samples, REPORT" % (statistics.median(values), p90)
print("RN fallback: " + summary + " (different harness; never a below-RN TTID verdict)")
path = Path(history)
with path.open("a") as stream:
    stream.write("\nRN am start fallback (%s, %s, %s/%s, APK %s): %s. Raw: `%s`. Comparison remains open.\n" % (
        datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d"), commit, device_class, serial, sha[:12], summary, file))
PYTHON
    fallback_status=1
  fi
fi
if [ "$pass_status" != 0 ]; then
  echo "RN tooling: if the failure is NOT-PROFILEABLE, rerun with RN_NOT_PROFILEABLE=1 (OQ-13-3)" >&2
fi
if [ "$pass_status" != 0 ]; then exit "$pass_status"; fi
if [ "$fallback_status" != 0 ]; then exit "$fallback_status"; fi
exit "$check_status"
