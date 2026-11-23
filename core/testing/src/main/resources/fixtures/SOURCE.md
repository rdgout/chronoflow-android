# API fixtures

Byte copies of the iOS fixtures in `Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/` at iOS `f391a1fa` (`tools/ios-pin.txt`). `fixtures.sha256` records the hash of each file and its iOS path.

- `plans/` and `trips/` were built by iOS from RN `e24872af` at web `afd80dc0` (iOS `plans/SOURCE.md`). The contract generator is pinned at web `e36368ec`; iOS decodes them under the same pin.
- `wizard/` was hand-built from web `afd80dc0` (iOS `wizard/SOURCE.md`).
- All tokens, ids and secrets in these files are artificial.
- The files are never edited by hand: `make contracts` rewrites them and `make contracts-check` fails on any drift.
