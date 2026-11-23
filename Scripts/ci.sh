#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
make generate check
git diff --exit-code
test -z "$(git status --porcelain)" || { git status --porcelain; echo "ci.sh: untracked or changed files after generate" >&2; exit 1; }
