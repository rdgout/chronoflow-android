# Shared by the performance runners; source after setting ADB.
resolve_perf_device() {
  local attached
  attached="$("$ADB" devices | grep -Ec '[[:space:]]device$' || true)"
  if [ "$attached" != 1 ]; then
    echo "${PERF_SUITE}: exactly one device must be attached, found $attached" >&2
    return 1
  fi
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    SERIAL="$ANDROID_SERIAL"
  else
    SERIAL="$("$ADB" get-serialno | tr -d '\r')" || return 1
  fi
  case "$SERIAL" in
    '' | unknown | error:*) echo "${PERF_SUITE}: adb could not select a device: '$SERIAL'" >&2; return 1 ;;
  esac
  export ANDROID_SERIAL="$SERIAL"
}
