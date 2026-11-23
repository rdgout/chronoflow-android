# Phase 4a review round 1

Each finding is fixed in its own signed commit. Reproductions supplied for findings 1–4 were read in place and were not copied into the repository. Local command logs and tier JSON snapshots are under `build/phase4a-review/`. No device, performance or profile command is needed for these fixes.

| Finding | Red evidence | Green wall time |
|---|---|---:|
| 1 | Two nested Turbine observers make close throw ConcurrentModificationException; the regression now completes both observers, clears the count and clears accountID. (red run 1.98 s). | 1.23 s |
| 2 | Cancellation between IO registration and producer resumption leaves one observer/listener; the hand-drained regression now verifies both are removed. The whole ObservationTests class passes. (red run 2.00 s). | 1.78 s |
| 3 | observe times out behind the lifecycle write lock, and validate completes without IO dispatch; both caller-thread regressions now pass, together with the epoch/permit guards and eager-closed check. (red run 2.00 s). | 2.32 s |
| 4 | Mutation proof: removing ensureActive from SyncEpoch.check makes the rewritten test fail with check returned after cancellation; restoring production code makes it pass. The test also proves the call was reached and the epoch remains valid. (red run 2.22 s). | 1.27 s |
| 5 | Three failures prove the broken signed-out alias, absent retry and retained key on delete. The store now deletes and retries a normalized permanent encryption failure once, delete removes the key, and all file-store cases pass. The brief is patched. (red run 3.36 s). | 3.06 s |
| 6 | Code inspection found no sync before rename. Per the requested verification exception, no power-loss red test was manufactured: the existing round-trip passed before the change (1.68 s). FileOutputStream.fd.sync now precedes stream close and atomic move; all marker tests pass. The brief is patched. | 1.56 s |
| 7 | The deterministic ownership-graph test fails at preferences.context before the fix. Both the preferences value and produceFile closure now use weak application references; the structural guard, preference persistence/mirror/singleton cases and existing seed tests pass without GC. (red run 2.82 s). | 6.53 s |
| 8 | The factory-only guard raises no error for the two coordinator entry points. The expanded guard now rejects the aliased creator and direct constructor as well as factory imports, while allowing single-process stores. OQ-A131 keeps the same exclusion condition and native allow-list. (red run 1.77 s). | 2.24 s |


The single final `make quick` invocation passed in **22.89 s** overall: 1,254 quick-tier tests (8.48 s), 26 build-logic tests (0.66 s), 190 Python tests and 6 serial timing tests (0.53 s), with zero budget overruns. Each fix passed on its first green run; the red failures above are intentional regression/mutation evidence. No requested command was blocked. No GC timing assertion or power-loss simulation was used.

Exact targeted commands and outputs are in the numbered `build/phase4a-review/<finding>-red.log` / `-green.log` files and matching JSONs. Finding 6 instead has a passing `6-before.log`, as permitted by its code-inspection verification requirement. The final run is `review-quick.log`. The supplied reproductions remain outside the repository.
