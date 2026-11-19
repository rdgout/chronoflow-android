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

## Task 6

`make targeted ONLY="feature:today:TodayInvalidationTest"` passes both cases in 7.37 s wall. The complete fixture is composed in a test-only tall viewport so no missing off-screen row can produce a false zero. On one quiet minute, SCREEN, HERO, PANEL and all seven ROW scopes have delta **0**; the five unmarked row subtitles and the future hero eyebrow each have delta **1**. Done/skipped rows have no clock leaf. The negative control passes an Instant directly into a test-only row and records ROW delta **1**, proving the counter detects the bad scope.

## Task 7

`make targeted ONLY="feature:today:TodayControlTests#testEachRowStateKeepsOneLargeTapTargetAtXXLarge"` passes in 6.50 s wall after removing an unnecessary member-function import (first compile: 2.56 s). All three Dutch rows at the capped large font have one clickable node, height ≥56 dp and 16 dp gutters.

`make targeted ONLY="app:TodayTests"` passes on **pixel AVD / Android 16** in 9.72 s command wall, including build/install. The result XML records **4.323 s** for the test; it enforces <10 s and asserts **0** debug main-thread IO violations. It launches seeded into Today, opens the trip twice, and returns through system Back and an actual predictive edge swipe, checking the Today structural tag and debug route probe after each return. This was the only executed emulator journey. An earlier command stopped before execution on the absent Espresso import (3.19 s); the existing UiAutomator system-Back API resolved it without adding a dependency. The landed 6a tree has no edge helper, so the new helper is Android-test-only and derives coordinates from the display. Pixel remains running.

## Task 8

`make targeted ONLY="feature:today:TodayFullyDrawnTest app:FixtureTodaySourceTest app:ReleaseReadinessTest#releaseShipsNoPerfCode"` passes all eight cases. The two reporter cases take 2.806 s together, the five fixture/gate cases 0.188 s, and release exclusion 0.002 s. The three Gradle invocations report 7 s, 4 s and 7 s respectively (release tier wall 7.34 s). They prove both reporter orderings, a usable selected/detail source, Done/Undo, strict perf-stub selection and classpath exclusion.

`python3 -I -m unittest discover -s Scripts/tests -p test_check_benchmarks.py` passes 56 cases in 2.650 s. The initial new history-table assertion used the wrong row index (56 cases, 2.826 s); correcting that index resolved it. Missing and non-finite signed-in TTFD cases fail for the real reason, and a synthetic history append records TTID, TTFD, device/API/Hz and source. Synthetic numbers never enter performance history.

`./gradlew :benchmark:compileBenchmarkReleaseKotlin :app:compileBenchmarkReleaseKotlin` passes in 3.14 s wall; neither benchmark nor profile collection was executed. `make format` passes (11.05 s for the final Task 8 format run). The root reporter remains unchanged; merged code confirms Today is composed under the overlay before its exit completes. The explicit deferred commands and requested measurements are in `docs/perf-startup.md`.

## Brief refresh and open questions

The signed Task 0 refresh corrects four mechanical assumptions: `PlanClock.values()` has no `now()`; Compose card models and formatting stay together in core UI with core preferences; CheckOffWriter preserves the landed trip-page enqueue callback; and standard acceptance #5 supplies the reduced geometry matrix. Task commits also record the landed targeted-selector syntax, app-owned dispatcher type, CategorySwatch argument order, separate 390 × 844 capture frame, suspending mutation-permit provider, app-host test location, missing predictive-back helper and absent Espresso dependency. Each resolution preserves earlier-phase behaviour. There is one brief-refresh commit; later notes are in their task commits.

Added OQ-A324–OQ-A336 (13 rows): shared formatter home, Today budget, board differences, shared primitives, omitted glass icon, Settings destination, refresh, sheet survival, perf fixture, retention, native progress, wizard seam and the unresolved JVM title ink. No Android-only copy was needed. The highest row is A336; A68 remains unused.

## Final validation — stopped on a hang

- App, Today, My trips and trip-page Lint plus `verifyReleaseManifest` pass: `./gradlew :app:lintDebug :feature:today:lintDebug :feature:trips:lintDebug :feature:trip:lintDebug :app:verifyReleaseManifest`, **25.30 s wall**. A subsequent small Task 8 change keeps the source-readiness reporter stable across the source appearing; that exact edit has not had another Lint pass.
- The layout tier ran once for `:app`, `:core:ui`, `:core:designsystem`, `:feature:today`, `:feature:trips`, `:feature:trip`, using `Scripts/run-tier.sh layout` with those modules' result/timing globs and `./gradlew <module>:testDebugUnitTest … -Pcf.tier=layout`. **698 cases pass, 38.67 s command wall** (38.60 s tier). Counts: app 72, core UI 257, design system 7, Today 40, My trips 18, trip page 304. No failed cases or timing budgets.
- `make quick` ran **once**, and failed/stalled: **188.64 s command wall**, exit 2 from Make. Its first failure was `LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached`: `expected:<41> but was:<43>`. Both the Kotlin and Python generator count assertions are now corrected to 43 in the working tree; this correction has not been rerun.
- The same quick run then stopped producing app test results with `:app:testDebugUnitTest` active. Its in-progress binary stopped growing at 17:47:57 UTC. No per-process timing JSON was produced for the app, so this tier is a failure. After the Gradle call stalled past three minutes, `./gradlew --stop` ran once (**0.27 s wall**), with exact output:

```text
Stopping Daemon(s)
1 Daemon stopped
```

The quick command ended with:

```text
> Task :app:testDebugUnitTest

FAILURE: Build failed with an exception.

* What went wrong:
Gradle build daemon has been stopped: stop command received

run-tier: app/build/cf-timings/testDebugUnitTest wrote no timing files
run-tier: app/build/cf-timings/testDebugUnitTest has no matching passed stamp; rerun to measure it
make: *** [quick] Error 1
```

Diagnostic attachment (`/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/jcmd 32236 Thread.print`) failed with `java.io.IOException: Operation not permitted`; `ps` also returned `operation not permitted: ps`. The active method cannot be established from completed results. The log is `/tmp/phase7-final-quick.log`, and the failed attachment output is `/tmp/phase7-quick-daemon-threads.log`.

Per the user's instruction to stop on a hang, no hung test is retried and no further implementation/checks run. Task 8 is **uncommitted**, its plan box remains unchecked, and the final commit body is prepared at `/tmp/phase7-task8-commit.txt`. The remaining quick steps (build-logic tests, complete tooling suite, serial `make timing`) never started. Consequently the final serial preparation/drawing budget evidence is still missing. The new icon assertion correction also remains unverified. Eight signed commits cover the brief refresh and Tasks 1–7; no commit was rewritten after the next task began. No push occurred. Pixel was not stopped.

## Fix round 0 — Task 8 and link recreation (2026-11-19)

The earlier stop report above is historical. Task 8, including both icon-count corrections, is now committed as `ac474613`; its TTFD plan box is ticked. Performance/profile measurements remain explicitly deferred.

One confirmation run, `TIER_TOTAL_TIMEOUT=60 TIER_IDLE_TIMEOUT=40 make targeted ONLY="app:LinkRecreationTest#signedInLinkOpensOnceAfterTheRealSignOutCompletes"`, reached `:app:testDebugUnitTest` and hit `run-tier: targeted stopped after 60 s (total cap)` (60.84 s command wall, Make exit 2 / tier exit 124). This confirms the specific method. No other method was run during confirmation.

Removed that Activity test. Its pending-link, signed-out presence, reset-model identity and exactly-once assertions live in `AuthLinkRoutingTest.coldSignedInDefersUntilSignOutAndOpensOnce`; `warmSignedInKeepsTheLinkPending` already covers warm deferral. Real sign-out presence/ordering is independently covered by the JVM `core:sync` `SignOutTests`. Four Activity-only checks remain: recreation, Recents/history, onNewIntent's original intent and debug-secret forwarding. They seed signed out and drive real MainActivity lifecycle callbacks without attaching a Compose window. There are no screen assertions or UI waits.

No production ticker bug was found or changed: TodayViewModel shares only `PlanClock.values()`, MinuteClock wraps State, and refresh has no perpetual ticker. SystemPlanClock's minute-boundary timer is the existing real-time clock implementation, cancelled when collection stops. The old fixture composed Today on its StandardTestDispatcher while running a live SystemPlanClock against a real wall clock, then used `runTest` automatic time advancement for sign-out. A perpetual external clock cannot become idle under that fixture. JVM model tests already use FakePlanClock; the remaining Activity tests never compose Today. Existing SystemPlanClock tests explicitly control wall time and bound/cancel collection.

`make targeted ONLY="app:LinkRecreationTest app:AuthLinkRoutingTest"` passes all 12 cases, **16.86 s command wall** (includes a fresh daemon and compilation after the confirmation cap). Per-case LinkRecreationTest XML times:

| Case | Seconds | Sandbox |
|---|---:|---|
| debugSeedCapturesInMemoryAndDoesNotForwardTheSecret | 2.167 | first API 36 cold sandbox |
| historyRelaunchDoesNotCaptureTheLink | 0.061 | warm |
| coldViewIsCapturedAndRecreationDoesNotReplayIt | 0.470 | warm |
| warmViewKeepsTheLaunchIntentAndCapturesTheLinkOnce | 0.055 | warm |

All warm Activity cases are below 1 s. The eight router JVM cases take 0.001–0.048 s each; the extended signed-in case takes 0.002 s.
