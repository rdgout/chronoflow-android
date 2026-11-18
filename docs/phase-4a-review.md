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
