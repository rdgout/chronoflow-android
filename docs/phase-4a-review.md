# Phase 4a review round 1

Each finding is fixed in its own signed commit. Reproductions supplied for findings 1–4 were read in place and were not copied into the repository. Local command logs and tier JSON snapshots are under `build/phase4a-review/`. No device, performance or profile command is needed for these fixes.

| Finding | Red evidence | Green wall time |
|---|---|---:|
| 1 | Two nested Turbine observers make close throw ConcurrentModificationException; the regression now completes both observers, clears the count and clears accountID. (red run 1.98 s). | 1.23 s |
| 2 | Cancellation between IO registration and producer resumption leaves one observer/listener; the hand-drained regression now verifies both are removed. The whole ObservationTests class passes. (red run 2.00 s). | 1.78 s |
| 3 | observe times out behind the lifecycle write lock, and validate completes without IO dispatch; both caller-thread regressions now pass, together with the epoch/permit guards and eager-closed check. (red run 2.00 s). | 2.32 s |
