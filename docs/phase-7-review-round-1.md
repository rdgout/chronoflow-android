# Phase 7 review round 1

Preflight: `feat/phase-7-today`, HEAD `96736893`. The existing modification to `docs/perf-timeline.md` and the untracked later-phase briefs belong to the user and are left untouched.

## 1. Cancelled refresh

Changed the JVM regression to `TodayViewModelTest.cancelledRefreshStopsRefreshingWithoutFeedback`. It fails before the fix because cancellation produced a SomethingWentWrong capsule (8.02 s command wall). Today now delegates directly to RefreshFeedbackModel like My trips: the shared model rethrows cancellation, clears refreshing in finally, and creates no feedback.

The same targeted method passes after the fix (4.62 s command wall). Command: `make targeted ONLY="feature:today:TodayViewModelTest#cancelledRefreshStopsRefreshingWithoutFeedback"`. Logs: `/tmp/phase7-r1-1-{red,green}.log`.
