# Phase 3c: Welcome validation

Worker, 2026-11-18. Branch `feat/phase-3c-welcome`; pixel AVD API 36 for device tests, debug builds, functional animator scales 0 unless a test explicitly seeds Reduce Motion false. JVM renders use Robolectric native graphics SDK 36. Performance results and physical-device claims are not inferred from these checks.

Tasks 1–10 are implemented. The manual TalkBack focus check and quiet-host benchmark/profile runs remain open with the user. The final documentation commit contains the Task 10 plan checkbox, OQ-A101–112, the seed/font-gate specification and the five contrast exceptions in `docs/design-system.md`. OQ-A68 remains unused.

## Commits

All implementation, test and refresh commits are GPG-signed and contain no trailers. The Task 10 commit containing this record is `docs(launch): record phase 3c decisions`.

```text
6fc11f6 docs(phases): refresh the 3c brief against 3b
8a4d023 feat(launch): port the launch intro and moon anchor store
753434f feat(launch): port the welcome exit and moon target
bf86412 feat(launch): build the welcome screen
8df655f feat(launch): play the welcome intro on the launch clock
bd32c51 feat(launch): hand the launch over to welcome
3c29c88 feat(launch): add the diagnostic welcome mode
c7c3783 feat(app): route welcome actions to debug seams
b7450f2 test(launch): capture the welcome boards
27daea5 perf(launch): measure welcome interactive and the last intro item
0bdb530 fix(launch): follow the compose modifier convention
c1393ed test(app): measure the slow hand-off from its exit start
```

## Brief refresh and reconciliations

The Step 0 refresh commit records each patch in its body:

- Refresh the revision date, phase plan lines and BoardCapture references.
- Locate HandOffPlan, private controller methods, LaunchRect and test fixtures.
- Replace the absent LaunchProbe name with the landed composition observer.
- Locate font warm-up in CfRoot rather than AppGraph.
- Extend the landed LaunchTracing seam and LaunchOverrides.timeline method.
- Remove the absent interim KDoc claim and preserve status when replacing the exit.
- Preserve following-frame ReportDrawnWhen behavior and the existing navigation icons.
- Keep Welcome probes in debug variants and remove forbidden screen-copy assertions.
- Record OQ-A101 as next, leaving the removed OQ-A68 unused.
- Update benchmark placeholder references and preparation waits in Task 9.
- Reserve performance/profile runs for the orchestrator and avoid repeated feature journeys.
- Clarify existing contrast coverage without changing tokens.

Subsequent resolved discrepancies are recorded in the brief beside their owning tasks:

- Headless startup keeps 3b's separate callback, route publication and absence of an exit timeline. Its non-zero 28×28 fallback is only an invariant seed; its KDoc explains why nothing draws it.
- The existing launch StrictMode crash guard still ends at first commit. A debug-only disk/network counter filters ChronoFlow frames at any time and is zero after the Welcome journey reaches route.signIn.
- Android Fraunces retains boundary padding with Trim.None: three 48 sp lines measured 155.33 dp. The layout-only exact-line-height correction follows the pinned iOS ExactLineText behavior, with no token changes, clipping or state feedback.
- The frozen-position test's manually advanced single frame had not composed its new intro: hasPlayed was false. Automatic advancement plus waitForIdle fixes this frozen-only harness, with the original 0.5 px tolerance and semantic tags inside layers. No production animation correction was needed.
- The landed MainTestClock has no pending-frame query. A counting test LaunchClock proves live/frozen loops stop, without a production frame hook.
- DiagnosticWelcomeAnchorTest belongs beside the controller in the feature's Robolectric logic tier; parser and fallback argument tests remain in app JVM tests.
- Standalone pre-Welcome overlay fixtures must provide ProfileHost now that LaunchOverlay consumes LocalSafeInsets. ProfiledLaunchOverlay supplies the fixture in tests; production receives no fallback inset source.
- Startup, jank and profile collection wait for welcome.signIn to exist before the bounded 900 ms disappearance wait. Otherwise initially invisible controls could end capture early. The longer capture includes the final 3200 ms item.
- WelcomeBrand gained its conventional root modifier, retaining the intro modifier on the wordmark. This fixes Compose lint without moving the moon or semantic tag into the intro layer.
- Final docs make the Step 0 API inventory historical after interim removal, align remaining StrictMode wording with the resolved counter, and clarify quiet-host profile ownership. Plan rows now describe the real Welcome, enabled diagnostic mode, placeholder backgrounds and explicit am start -W relaunch.

## Verification

Exact commands, console logs and timing JSON are preserved locally under `build/phase3c-validation/`. The tables distinguish whole-command wall time from test or Gradle duration where an intermediate wall measurement was not archived. No command hung. No required test command was sandbox-blocked.

| Run | Result | Time |
|---|---|---|
| Task 1: LaunchIntroTests, MoonAnchorStoreTests | 4 passed | 2.24 s wall |
| Task 2: WelcomeTimelineTests, MoonTargetTests, WelcomeGeometryTests, WelcomeCompositionTest | 19 passed | 2.17 s wall |
| Task 3: WelcomeLayoutTest, WelcomeAccessibilityTest, WelcomeGuardsTest | 24 passed, one headline-height failure; affected layout method then passed all 3 profiles | 6.30 s initial + 4.48 s scoped fix |
| Task 4: WelcomeIntroRenderTest, WelcomeRecompositionTest | 3 passed, one frozen-position failure; stopped after failed position fixes, then resumed with the user's diagnosis | 4.50 s initial; one fix log reports 4 s Gradle; second fix 3.74 s wall |
| Task 4: hasPlayed diagnosis, then frozen-position harness fix | Diagnosis confirmed false; corrected method passed | 3.64 s + 3.69 s wall |
| Task 5: WelcomeHandOverTests, WelcomeIntroTimingTest, LaunchHandOffTimelineTests, LaunchControllerLifecycleTest, LaunchTraceTest | 32 passed, one newly exposed font timer cleanup failure; the scoped lifecycle fix passed (also covered by final quick) | 4.64 s initial; scoped feature fix 3 s Gradle, separate wall not archived |
| Task 5: WelcomeHandOffTest, CfRootHandOffTest, RoutePlaceholderGuardTest | 20 passed after an unused-expression compile warning was corrected | 6.09 s final wall; preceding compile failure 1.61 s |
| Task 6: DiagnosticWelcomeAnchorTest | Passed | 3 s Gradle; separate wall not archived |
| Task 6: LaunchDiagnosticsTests, LaunchModeTimelineTests | 6 passed | 2.34 s wall |
| Task 7: WelcomeSeamTest, StrictModeViolationCounterTest | Passed | 5 s Gradle; separate wall not archived |
| Task 7: WelcomeDestinationsReleaseTest, ReleaseReadinessTest | 7 passed | 2.88 s wall |
| Task 7: make targeted ONLY="app:WelcomeJourneyTest" | One run, passed, zero I/O violations; test 0.835 s | 5.53 s wall |
| Task 8: make captures ONLY=Main,SplashWelcome | All 42 Welcome cases passed and PNGs produced; 58 older overlay cases failed on missing LocalSafeInsets, 65 unrelated cases skipped | 12.70 s wall |
| Task 8: affected older overlay fixtures, captures and render tests | 108 passed after adding ProfileHost | 17.85 s wall |
| Task 9: python3 -I -m unittest discover -s Scripts/tests -p test_check_benchmarks.py -k CheckBenchmarksTest | 15 passed, including missing/non-finite and per-iteration edge checks | 0.807 s test time |
| Task 9: ./gradlew --console=plain :benchmark:compileBenchmarkReleaseKotlin | Passed | approximately 1 s command/Gradle time |
| Initial final make quick | Passed: 1,139 main checks, 26 build-logic checks, 107 script / 25 contract / 49 l10n / 9 icon tests, 6 serial timing checks | 22.13 s wall |
| Lint, full release tests, manifest verification, R8 analysis | Initial lint ModifierParameter failure; resumed checks passed, 32 release tests | 8.40 s initial + 6.42 s resumed wall |
| Full Robolectric layout tier | 363 passed, zero over budget | 11.47 s wall |
| make generators-check | Passed | 2.10 s wall |
| Remaining smoke classes (excluding the already-run Welcome journey) | 15 passed; one slow hand-off timing failure; stopped without retry | 24.50 s wall |
| Corrected make targeted ONLY="app:LaunchHandOffTest" | Both cases passed in one emulator class run; tests 2.342 s ready / 4.122 s slow | 10.87 s command wall (10.80 s tier) |
| Final resumed make quick | Passed: 1,139 main checks, 26 build-logic checks, all 190 tooling tests and 6 serial timing checks; zero over budget | 21.82 s wall |

The Makefile check components ran separately so the already-passing Welcome journey was not replayed. The top-level make check command was not invoked. The initial quick passed before the final modifier convention and slow-test changes; the final resumed quick validates those changes under the user's 2026-11-18 instruction.

The intermediate `chronoflow-3c-task5-lifecycle-fix-timings.json` was copied after the following app compile failed: its XML entries are stale and are not used as passing evidence. The feature fix's console log and the passing final quick provide its verification instead.

## Slow hand-off test correction

The previous test assumed a fixed 3900 ms removal. The recorded failure was 3919.013669 ms against 3900…3918.666665607029 ms (0.347003 ms over the bound); the user also reported an earlier 3920.19 ms miss. The slow stub sleeps until 3500 ms, wakes at or after that deadline, and the reducer commits exitStart from the actual wake. Comparing exitStart + 400 with the display phase determines removal's frame.

Only instrumented test code changed. LaunchScreenTest reads private LaunchState on the main thread through read {}, without adding an app/debug accessor. The assertions separately require exitStart in [3500, 3500 + frame + 2] and removal in [exitStart + 400, exitStart + 400 + frame + 2]. The ready case explicitly requires exitStart == 1500 ms and keeps its fixed [1900, 1900 + frame + 2] removal window. The reducer's max(1500 ms, now) gives that fixture its fixed start because readiness arrives before 1500 ms.

In the single corrected class run, frame length was 16.666665607028605 ms:

| Case | Exit start | Removal | Allowed removal window |
|---|---|---|---|
| Ready | 1500 ms (exact assertion passed) | 1909.887709 ms | 1900…1918.666665607029 ms |
| Slow | 3500.553168 ms | 3912.205793 ms | 3900.553168…3919.219833607029 ms |

Slow readiness was inside 3500…3518.666665607029 ms. App, reducer, timeline durations and tolerances are unchanged. Evidence: `slow-test-exit-start.log`, `slow-test-exit-start-timings.json`, `slow-test-exit-start-proof.log`.

## Board captures

The 42 Welcome PNGs are local evidence in `build/captures/`: 12 Main_final profile/language/theme combinations; 10 pixel8 frozen intro frames; 20 measured SplashWelcome frames across pixel8/foldInner and both themes. The controller drives the diagnostic sampler and measured moon anchor. Main_final_pixel8_en_light, Main_final_foldInner_nl_dark and SplashWelcome_welcome-1950_pixel8_en_light were visually inspected against the board source composition. Intentional differences remain those listed in the brief §5.1: plain sky, no stars/dots, measured target, iOS easing/timing and the corrected guest destination. Board390 geometry verifies the exact three-line 144 dp headline box.

## Open manual check

- [ ] **OQ-A103 / OQ-3c-3 — user-owned, pending:** with TalkBack enabled on pixel, cold-launch signed out with cf.reduceMotion=false. After the dissolve, verify the first accessibility-focused item is ChronoFlow (the wordmark). Automated reading-order/heading/button-role tests pass, but do not prove TalkBack focus. No focus override was introduced without a failed manual check.

## Quiet-host work for the user

Run make baseline-profile and then make perf on the final phase commit. Do not interpret the configured bounds as results. Record pixel AVD API 36, benchmarkRelease, Partial(Require), commit, refresh rate, cold TTID median/min/max, warm TTID, TTFD, welcomeInteractiveMs, introLastItemMs and launch frameOverrunMs P50/P90/P99 in docs/perf-startup.md. Flag a cold-TTID change beyond ±25% against the last comparable row.

At the observed 60.000004 Hz, per-iteration emulator moment bounds are 2549…2567.66666556 ms interactive and 3199…3217.66666556 ms last item. The metric uses app-process async first-commit/exit/intro slices and fails on missing values through the checker. Physical-device gates stay open under OQ-A22. No make perf, make perf-timeline or make baseline-profile command was run in this phase session; measurement rows and regenerated profiles remain pending as requested.
