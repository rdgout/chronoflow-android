#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
if [ "${CF_DEVICE_LOCK_HELD:-}" != 1 ]; then
  exec python3 -I Scripts/device_lock.py -- "$0" "$@"
fi
exec python3 -I Scripts/fold_check.py "$@"
