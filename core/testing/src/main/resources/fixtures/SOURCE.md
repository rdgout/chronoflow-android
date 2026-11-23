# API fixtures

Byte copies of the iOS fixtures in `Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/` at iOS `f391a1fa` (`tools/ios-pin.txt`). `fixtures.sha256` records the hash of each file and its iOS path.

- `plans/` and `trips/` were built by iOS from RN `e24872af` at web `afd80dc0` (iOS `plans/SOURCE.md`). The contract generator is pinned at web `e36368ec`; iOS decodes them under the same pin.
- `wizard/` was hand-built from web `afd80dc0` (iOS `wizard/SOURCE.md`).
- All tokens, ids and secrets in these files are artificial.
- The files are never edited by hand: `make contracts` rewrites them and `make contracts-check` fails on any drift.

`reminders/selection.json` contains the 110 RN selection evaluations exported on the iOS side with `node Tools/ReminderParity/export-selection.cjs /Users/rickgout/Sites/chronoflow-mobile` from the iOS repo root. The exporter writes `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json`; its `source` and `commit` record `src/reminders/__tests__/selection.test.ts` at `e24872afd47ff671fbbab315665920f2805054df`. Android copies these bytes through `make contracts`, never runs the exporter. To re-pin, move `tools/ios-pin.txt` and rerun `make contracts`.
