# Phase 7 review round 1

Preflight: `feat/phase-7-today`, HEAD `96736893`. The existing modification to `docs/perf-timeline.md` and the untracked later-phase briefs belong to the user and are left untouched.

## 1. Cancelled refresh

Changed the JVM regression to `TodayViewModelTest.cancelledRefreshStopsRefreshingWithoutFeedback`. It fails before the fix because cancellation produced a SomethingWentWrong capsule (8.02 s command wall). Today now delegates directly to RefreshFeedbackModel like My trips: the shared model rethrows cancellation, clears refreshing in finally, and creates no feedback.

The same targeted method passes after the fix (4.62 s command wall). Command: `make targeted ONLY="feature:today:TodayViewModelTest#cancelledRefreshStopsRefreshingWithoutFeedback"`. Logs: `/tmp/phase7-r1-1-{red,green}.log`.

## 2. Future formatting context

Ported the Future card's options boundary from pinned TodayFutureTripCard.options: language, time format and display zone, without trip context. Active rows retain their trip context. The regression uses an instant in America/New_York while displaying Europe/Amsterdam and an airport city deliberately named Queens. It checks that Future options carry no trip, annotated formatting uses the IANA city New York, and the actual future subtitle retains the pinned unannotated 05:00 output. Both platforms default annotateInstantZone to false, so the redundant trip argument did not previously change that unannotated output; the explicit options contract supplies the failing assertion.

`make targeted ONLY="feature:today:TodayReducerTest#futureInstantInAnotherZoneUsesNoAirportCityContext"` fails before removal (4.78 s wall) and passes after it (2.59 s wall, 0.129 s case). A mechanical extraction of the unchanged options preceded the failing test. `make format` also formats the prior refresh assertion; one long test expression needed splitting before formatting passed. Logs: `/tmp/phase7-r1-2-{red,green}.log`.
