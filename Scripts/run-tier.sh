#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 -I "$ROOT/Scripts/run_tier.py" "$@"
