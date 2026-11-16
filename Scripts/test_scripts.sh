#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 -I -m unittest discover -s "$ROOT/Scripts/tests" -p 'test_*.py'
