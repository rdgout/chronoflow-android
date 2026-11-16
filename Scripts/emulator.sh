#!/bin/bash
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="${ADB:-$ANDROID_HOME/platform-tools/adb}"
EMULATOR="${EMULATOR:-$ANDROID_HOME/emulator/emulator}"
AVDMANAGER="${AVDMANAGER:-$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager}"
SDKMANAGER="${SDKMANAGER:-$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager}"
PGREP="${PGREP:-pgrep}"
LOCK="python3 -I $ROOT/Scripts/device_lock.py"

port_of() {
  case "$1" in
    pixel) echo 5554 ;;
    fold) echo 5556 ;;
    small) echo 5558 ;;
    api29) echo 5560 ;;
    *) echo "emulator.sh: unknown avd '$1' (pixel, fold, small, api29)" >&2; exit 2 ;;
  esac
}

under_lock() {
  if [ "${CF_DEVICE_LOCK_HELD:-}" != 1 ]; then
    exec $LOCK -- "$0" "$@"
  fi
}

booted() {
  "$ADB" devices | grep -Eq "^$1[[:space:]]+device" &&
    [ "$("$ADB" -s "$1" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]
}

prep_functional() {
  local s="${1:-emulator-5554}"
  for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$ADB" -s "$s" shell settings put global "$scale" 0
  done
  "$ADB" -s "$s" shell settings put secure show_ime_with_hard_keyboard 1
  "$ADB" -s "$s" shell settings put secure autofill_service null
  "$ADB" -s "$s" shell cmd uimode night no
  "$ADB" -s "$s" shell input keyevent 82
}

prep_perf() {
  local s="${1:-emulator-5554}"
  for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$ADB" -s "$s" shell settings put global "$scale" 1
  done
}

boot() {
  local avd="$1" port="$2" serial="emulator-$2" extra="${3:-}"
  mkdir -p "$ROOT/build/emulator"
  local log="$ROOT/build/emulator/$avd.log"
  # shellcheck disable=SC2086
  nohup "$EMULATOR" -avd "$avd" -port "$port" -no-window -no-audio -no-boot-anim -gpu host $extra >"$log" 2>&1 &
  disown || true
  "$ADB" -s "$serial" wait-for-device
  local i
  for i in $(seq 1 90); do
    if [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
      grep -iE "renderer|gpu" "$log" | head -3 || true
      return 0
    fi
    sleep 2
  done
  echo "emulator.sh: $avd did not boot within 180 s; last log lines:" >&2
  tail -20 "$log" >&2
  return 1
}

cmd_ensure() {
  under_lock ensure "$@"
  local avd="${1:-pixel}" port serial
  port="$(port_of "$avd")"
  serial="emulator-$port"
  if ! booted "$serial"; then
    boot "$avd" "$port"
  fi
  prep_functional "$serial"
}

cmd_down() {
  under_lock down "$@"
  local serials=() s i
  if [ $# -gt 0 ]; then
    serials=("emulator-$(port_of "$1")")
  else
    while read -r s _; do serials+=("$s"); done < <("$ADB" devices | grep -E '^emulator-[0-9]+' || true)
  fi
  for s in ${serials[@]+"${serials[@]}"}; do
    "$ADB" -s "$s" emu kill || true
    for i in $(seq 1 30); do
      "$ADB" devices | grep -Eq "^$s[[:space:]]" || break
      sleep 2
    done
  done
}

is_busy() {
  if [ "${CF_DEVICE_LOCK_HELD:-}" != 1 ] && $LOCK --probe; then
    return 0
  fi
  if "$PGREP" -f 'GradleWorkerMain|run_tier.py' >/dev/null 2>&1; then
    return 0
  fi
  if "$ADB" devices 2>/dev/null | grep -Eq '^emulator-5554[[:space:]]+device'; then
    # The bracket keeps the on-device sh -c command line from matching itself.
    if [ -n "$("$ADB" -s emulator-5554 shell "pgrep -f 'instrumen[t]'" 2>/dev/null | tr -d '\r')" ]; then
      return 0
    fi
  fi
  return 1
}

cmd_wipe() {
  if is_busy; then
    echo "emulator.sh: tests are running; not wiping" >&2
    exit 1
  fi
  if [ "${CF_DEVICE_LOCK_HELD:-}" != 1 ]; then
    exec $LOCK --wait 0 -- "$0" wipe
  fi
  cmd_down pixel
  boot pixel 5554 -wipe-data
  prep_functional emulator-5554
}

cmd_create() {
  under_lock create "$@"
  local avd="${1:-}" image="system-images;android-36;google_apis;arm64-v8a" device
  case "$avd" in
    fold) device=pixel_9_pro_fold ;;
    small) device=small_phone ;;
    api29)
      device=pixel_8
      image="system-images;android-29;google_apis;arm64-v8a"
      ;;
    *) echo "usage: emulator.sh create fold|small|api29" >&2; exit 2 ;;
  esac
  if "$AVDMANAGER" list avd -c | grep -qx "$avd"; then
    echo "emulator.sh: avd '$avd' already exists" >&2
    exit 1
  fi
  if [ "$avd" = api29 ]; then
    "$SDKMANAGER" "$image"
  fi
  echo no | "$AVDMANAGER" create avd -n "$avd" -k "$image" -d "$device"
}

sub="${1:-}"
[ $# -gt 0 ] && shift
case "$sub" in
  ensure) cmd_ensure "$@" ;;
  down) cmd_down "$@" ;;
  wipe) cmd_wipe ;;
  create) cmd_create "$@" ;;
  prep-functional) under_lock prep-functional "$@"; prep_functional "$@" ;;
  prep-perf) under_lock prep-perf "$@"; prep_perf "$@" ;;
  busy) is_busy ;;
  *) echo "usage: emulator.sh ensure [avd] | down [avd] | wipe | create <avd> | prep-functional [serial] | prep-perf [serial] | busy" >&2; exit 2 ;;
esac
