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

## Task 4

All seven structural screen cases and five pure label cases pass across the targeted runs. Initial UI compilation found positional swatch arguments (3.51 s); the first edit fixed only the future card (2.43 s), then named arguments fixed the row. The first completed screen run (7.59 s) found three tags cleared by accessibility semantics. Defining the tags inside the replacement semantics passed loading/marked controls (7.09 s); the progress-only correction passed in 5.65 s. No screen test asserts rendered copy.

The layout run covers 39 distinct cases: 15 state guards, 12 Swift-named large-font cases, eight row-state combinations and four width cases (including 280 dp). The initial run (7.59 s) exposed missing insets in standalone row fixtures and stale lazy-node IDs during guard-driven scrolling. The corrected fixtures use safe insets, real shell chrome for state guards, and current-viewport text checks before/after an explicit tagged scroll. All 35 affected cases pass in 8.94 s. The four width cases pass in 7.67 s, including asymmetric side insets and the minimum width.

`make generate` passes in 3.53 s; only the two new board-sourced Lucide glyphs change generated output. `make captures ONLY=Today,TodayEmpty,TodayFuture,TodayLoading,TodayCompleted,TodayRowStates` ran once, passed in 14.50 s and wrote 24 PNGs. A targeted capture-class rerender plus the non-null future-Next screen case passed in 9.02 s after adding the already-documented 6a idle/frame/idle wait. All 24 images were reviewed in both themes and languages.

The rerender did **not** restore native title ink for the three short-content states; OQ-A336 and `docs/board-deltas.md` retain this unresolved difference for device verification. The native app bar stays unconditionally composed, with a structural tag. The Swift future fixture deliberately carries stale active detail, so its optional Next row is absent in the capture; live source selection and the separate screen case cover the populated branch. Other sample-content/platform differences are recorded in the board-delta table. OQ-A326–OQ-A335 record the brief defaults; OQ-A44 already covers row-to-sheet navigation.

## Task 5

The live Today route uses the session source, entry-scoped model, existing sheet content/refusal host, and the same refresh capsule policy. It retains the existing `route.today` container tag for earlier shell checks, with real Today content inside. The hero pushes on the Today stack; Settings selects its tab, past trips selects the My trips root, and create opens the existing wizard seam.

The sheet integration and two added JVM refresh-policy cases pass (2.957 s for the sheet case; 0.102 s combined for the model cases). The first compile needed the native sheet API opt-in (3.01 s). The next invocation passed these feature cases but app compilation exposed the suspending mutation-permit API (11.08 s overall). After making the source provider suspending, the app navigation cases plus unchanged `TripsTabTest` and `ShellRecreationTest` pass in 7.76 s wall. All queries are structural; the navigation test uses the real RootContent/AppShellScaffold and production action wiring with a fake Today source, and asserts the actual tab stack plus the debug root probe.
