# Phase 7 validation

## Task 1

`make targeted ONLY="core:ui:TripCardEyebrowTest core:ui:TripCardInvalidationTest core:data:CheckOffWriterTest feature:trips:TripCardFormattingTest feature:trips:TripsViewModelTest feature:trips:TripsContentTest feature:trip:StepCheckTest feature:trip:TripPageSelectionTest feature:trip:CustomActionAnnouncesTest feature:trip:TimelineInvalidationTest feature:trip:ListInvalidationTest"` passes: 171 cases, 34.76 s wall including builds. The eyebrow invalidation case takes 2.53 s. `make format` passes after resolving formatting rules. The JVM and Robolectric result XMLs contain zero failures and errors.

An additional direct check of persisted marks passes in `core:data:CheckOffWriterTest#setCheckInsertsOneOutboxRowAndTheLocalMark` (2.19 s wall); its first compile used the wrong enum property (`rawValue`, 2.54 s), corrected to the landed `raw`. This checks the stored mark itself rather than relying only on the pending-outbox merge.

`make perf-timeline` and its Task 1 ±25% comparison remain with the user, as requested. The serial drawing/preparation budgets run in the single final `make quick`.

## Task 2

`make targeted ONLY="core:plan:TodayTests core:plan:TodayEdgeTests core:plan:TodayViewBudgetTest"` passes all 24 cases in 3.96 s wall. The existing `Today.kt` matches the pinned port line by line, including all 17 hidden categories, optional filtering, next-step mark exclusion, ceil minutes and the UTC fallback; no earlier logic was changed. Selection now ports the active-end/upcoming-start ordering and identity ties.

On the local JVM (MacBook Pro, debug test compilation), `todayView` large60 takes 0.7365–0.8724 ms across 10 samples after one warm-up, below 20 ms each. This is a JVM measurement, not an emulator/device claim. OQ-A325 adds this guard to the serial timing tier and plan performance table.

## Task 3

`make targeted ONLY="feature:today:TodayReducerTest feature:today:TodayReducerQuietTickTest feature:today:TodayViewModelTest"` passes 25 cases in 3.47 s wall. An initial compile stopped on a redundant null check under `-Werror` (2.78 s); the check was removed. The new catalog-failure case then passes on its own in 3.83 s. `./gradlew :app:compileDebugKotlin --console=plain` also passes, including `DbTodaySource`.

The tests cover quiet/boundary ticks, language-only reformatting, concurrent-write exclusion, foreign cancellation, IO failure, plan eviction and sheet/write identity across a hero change. Opening the sheet retains one hero-detail subscription. Copy-catalog load failures retain Loading, as pinned iOS does.

The brief records the dispatcher API adaptation: `AppDispatchers` lives in `:app`; Today receives its main/default values through `TodayDependenciesKey`, and the app source receives IO plus the current mutation-permit provider. No earlier-phase dispatcher API changes.
