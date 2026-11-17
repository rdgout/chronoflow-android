#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 -I -m unittest discover -s "$ROOT/Scripts/tests" -p 'test_*.py'
python3 -I -m unittest discover -s "$ROOT/tools/contracts-gen" -t "$ROOT/tools/contracts-gen" -p 'test_*.py'
python3 -I -m unittest discover -s "$ROOT/tools/l10n" -t "$ROOT/tools/l10n" -p 'test_*.py'
exec python3 -I -m unittest discover -s "$ROOT/tools/icons" -t "$ROOT/tools/icons" -p 'test_*.py'
