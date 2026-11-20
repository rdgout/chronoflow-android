# Test diet

Branch `chore/app-test-diet`, stacked on `feat/phase-7-today`. Preflight matched. Audit begun 2026-11-19 and completed 2026-11-20. Sources outside this repository were not touched.

## Measurement

The baseline is the parent of `864f944f` (before the diet). The last check's XML/timing records were copied into `build/test-diet-before/` before running anything. A targeted run of every app debug test then measured 405 cases; XML and the listener records are preserved in `build/test-diet-before/full/`. The broad `app:*` selector initially selected instrumented tests; that empty instrumentation invocation failed. All reported suite measurements use explicitly enumerated JVM class selectors instead.

Times below are seconds **per case**, read from JUnit XML, not class totals. Warm ranges exclude the listener's first sandbox case for each SDK/fork. A temporary, uncommitted `AColdSandboxProbeTest` rendered/captured a tagged Box before graph construction, hand-off, flight-path and forced-exit measurements; the release probe warmed Android resources without launching an Activity. Before/after runs used the original/current test sources respectively and restored all current sources in `finally`. Their XML lives in `build/test-diet-probe/{before,after}/`; the probes were removed. This supplies warm observations for single-case classes rather than subtracting an estimated startup cost.

A first sandbox cost is not a warm case regression. The last check explicitly recorded these as `warmup`: CfRootHandOffTest 4.671; AppGraphConstructionTest (release) 2.260; FlightPathStrokeTest 2.102; ConfigurationPolicyTest SDK 29/32/33 0.796/1.071/0.870. Layout first cases were TripsGuardsTest 4.678, TripActionsGuardsTest 4.107, ForgotGuardsTest 4.125 and TodayAllHiddenLayoutTests 4.515. Subsequent cases in each sandbox are in the XML. Parameterized display names can be classified as SDK `default` by the listener, so XML and process/SDK ordering were checked together.

The ordinary full app baseline also recorded 1.287 for its first native Compose hand-off after an earlier non-graphics Robolectric case. The render probe measured that same original case at 0.514 (a repeat at 0.525); Android sandbox and first renderer initialization are reported separately. Original ReleaseSeedExtrasTest was 1.475 in the last check, 1.895 when first in a targeted release fork, and 0.654 after the resource probe. Original forced-data exit was about 1.7 in the last check and 0.151 after the render probe. These are measured observations, not estimates of cold overhead.

The XML guard cases agree with the per-fork warm-up markers; the single-case Today guard has no separate warm observation and is not assigned an invented warm time:

| Guard | First XML case (s) | Subsequent warm cases (s) |
|---|---:|---:|
| `TripsGuardsTest` | 4.676 | 0.076–0.386 |
| `TripActionsGuardsTest` | 4.107 | 0.047–0.289 |
| `ForgotGuardsTest` | 4.123 | 0.102–0.264 |
| `TodayAllHiddenLayoutTests` | 4.515 | single case; no separate warm sample |

## Inventory and decisions

The inventory follows direct calls, inherited fixtures, seed helpers and implicit `ChronoFlowApp.onCreate` graph construction. There are 47 app classes, including manifest/reflection-only references for completeness. No feature test constructs the app graph/root; two named launch performance suspects are included as additional audit rows. The quick run identified one extra core data identity test that also launched unnecessary Activity windows. Fixtures without test methods (`SessionTestApplication`, `AppSessionFixture`, `AuthResetActivityFixture`, `ShellActivityFixture`, `LinkActivityFixture`, `RootContentTestHost`, the custom entry Activities) are accounted for through their concrete test classes.

Decisions across the 50 audited classes: **27 shrink, 17 keep, 3 delete duplicate, 3 move to JVM**. A class with both moved logic and retained native cases is counted as shrink.

| Class | Before warm case range (s) | Decision | What it proves / change |
|---|---:|---|---|
| `AppGraphConstructionTest` | 0.029 | shrink | StrictMode and guarded Android directories prove lazy graph construction; remove the unrelated Application graph and redundant directory listing. |
| `BackupRulesTest` | 0.007–0.016 | shrink | Merged manifest and resource XML exclude backup domains; plain Application. |
| `ConfigurationPolicyTest` | 0.003–0.015 | shrink | Real SDK configurations apply locale, direction and capped density, and read the small device-config file only below API 33; plain Application. |
| `LauncherIconInflationTest` | 0.028 | shrink | Android drawable inflation has all adaptive icon layers; plain Application. |
| `OkHttpStartupTest` | 0.082 | shrink | Android client construction runs on IO without an initializer or Application context; plain Application. |
| `PendingLaunchIntentsTest` | 0.007 | keep | Keep the graph integration boundary: intents stay buffered before finishLaunch and are drained afterward. No Activity or composition; direct buffer/policy logic has separate JVM tests. |
| `SessionActivationTest` | 0.002–0.171 | keep | Direct JVM activation ordering/cancellation; one JDBC SessionGraphs fixture proves sync resume uses the current session. No Android app. |
| `SessionGraphsTest` | 0.011–0.019 | keep | Direct JVM scope/epoch ownership, cancellation and account-model detachment are the graph contract itself. |
| `SessionLifecycleTests` | 0.012–0.022 | keep | Direct JVM JDBC restore/readiness and graph attach/detach integration; no Activity or app composition. |
| `SplashThemeTest` | 0.004–0.009 | shrink | Platform theme resources across SDK 29/31/36 and night mode; plain Application. |
| `StoragePathsTest` | 0.004–0.006 | shrink | Android no-backup path integration and validated account paths; plain Application. |
| `ViewModelExtrasTest` | 0.027 | shrink | MainActivity supplies its real default creation extras; seeded signed-out, create only, no attached window. |
| `CfRootHandOffTest` | 0.050–0.514 | shrink | Accessibility shielding/overlay teardown, plus immediate accessible Welcome. Signed-in presentation uses a tagged child instead of AppShell/Today; remove duplicate route and Welcome hand-off cases. |
| `CfRootTest` | 0.020–0.123 | keep | Root background pixels, pointer interception, accessibility shielding and actual system-bar flags. These assertions need composition/window state. |
| `LaunchGateRoutingTest` | 0.049–0.520 | delete duplicate | Only route/revision and account wipe assertions; despite its name the first case never asserted notification permission. Routing predicates remain on JVM; review round 1 restores real AppGraph teardown wiring coverage (mapping below). |
| `LaunchIntentRestorationTest` | 0.022–0.036 | delete duplicate | Activity link capture/history/recreation duplicates the stronger signed-out LinkRecreationTest, which also checks intent scrubbing. |
| `MainActivityConfigChangesTest` | 0.004 | keep | Manifest configuration flags only; already uses a plain Application and never starts MainActivity. |
| `ProcessDeathRestoreTest` | 0.012–0.013 | keep | Actual saved-state registry restoration versus mismatched process token, overlay claim and splash hold; seeded signed-out lifecycle. |
| `RootContentConstructionTest` | 0.023–0.025 | shrink | Nav3 subtree identity is a composition contract. Separate bridge/router scheduling into LaunchConnectionTests; keep only minimal route/revision composition fixtures. |
| `RootContentTest` | 0.020–0.038 | shrink | Tagged destination structure, repeated-route remember identity, and actual Activity recreation without another navigation; isolate the two composition cases from the Application graph. |
| `RootRouteTransitionTest` | 0.024 | shrink | Nav3 removes the outgoing subtree before a crossfade could mask overlap; plain Application. |
| `RootThemeTest` | 0.037 | keep | Real Activity frame-commit callback gates IO font preloading and provides system reduce-motion state; seeded signed-out. |
| `RoutePlaceholderGuardTest` | 0.015–0.020 | shrink | Profile geometry and safe bounds only; plain Application. |
| `SeededScreenRecreationTest` | 0.181 | shrink | Actual frame-commit routing is not reapplied on recreation. Use signed-out Welcome → SignIn rather than signed-in Today → Settings. |
| `WarmLaunchIntentTest` | 0.042–0.043 | shrink | Real warm Activity hand-off of link/reminder intents drains the buffer without changing the process route. Seed signed-out and retain Terms instead of rendering Today. |
| `WelcomeHandOffTest` | 0.047–0.073 | keep | Measured Welcome moon anchor/landing geometry, font visibility, accessibility and exactly-once fully-drawn reporting need real layout. |
| `WelcomeSeamTest` | 0.089 | keep | Measured Welcome position after the real back-handler seam returns; the sign-in/guest actions exercise those seams. |
| `ForgotRecreationTest` | 0.084–0.158 | keep | Real recreation preserves the process model/form/sent state, and the installed system back-handler returns to sign-in; seeded signed-out. |
| `PasskeyGraphTest` | 0.093–0.216 | shrink | Keep capability injection and Activity anchor attach/detach, using lifecycle callbacks without an attached Compose window. Move unavailable-provider banner logic to AuthServicesTest. |
| `ResetRecreationTest` | 0.171–0.234 | shrink | Real locale recreation drops editable passwords, retains the in-memory link and saves no secrets; keep native back. Done-continue is already a JVM model assertion. |
| `SignUpEntryScopeTest` | 0.126 | shrink | Nav3 entry removal clears the ViewModel and recreation retains the current entry. Replace AppGraph with AuthServices and a fake source. |
| `TwoFactorRoutingTest` | 0.156–0.477 | shrink | Keep native back and recreation of the challenge/code. Move replacement, cancel and re-entry routing to TwoFactorRoutingModelTest; acceptance already has stored-gate JVM coverage. |
| `VerificationSessionAdapterTest` | 0.005–0.006 | keep | Concrete adapter must not initialize lazy sync for stale confirmation, and stopping initialized sync releases held plans; graph wiring/laziness, no Activity. |
| `VerifyRoutingTest` | 0.406–0.615 | shrink | Keep the real system back-handler that leaves an unverified session signed in. Review round 1 restores real AppGraph teardown wiring coverage; the lower-layer tests alone do not prove it. |
| `AppLinkIntentFilterTest` | 0.002–0.008 | shrink | Real merged manifest resolves only the allowed auth prefixes; plain Application. |
| `LinkRecreationTest` | 0.016–0.035 | keep | Actual cold/onNewIntent/history/recreation callbacks scrub secrets and capture once; seeded signed-out and no attached Compose window. |
| `AppShellRefusalGuardTest` | 0.043–0.063 | shrink | Actual snackbar geometry stays within fixture insets and bar/rail bounds; plain Application. |
| `AppShellTest` | 0.017–0.059 | shrink | Nav3 owners, remembered scroll, reselect handling, predictive/system back, overlay coverage and bar/rail/inset geometry require composition. Uses tagged placeholder pages and plain Application. |
| `ShellRecreationTest` | 0.053 | shrink | Activity/Nav3 retain tab/page/ViewModels across recreation, then cancel removed entry work; fake dependencies and plain Application. |
| `ShellSeedTest` | 0.145–0.189 | move to JVM | DebugShellStart single consumption plus ShellViewModel state, directly on JVM; no database, Activity, root or live clock. |
| `TodayNavigationTest` | 0.097 | move to JVM | Hero route push and pop move to TodayActionsTest; the real back-handler already has AppShellTest coverage. |
| `TripsTabTest` | 0.150–0.212 | delete duplicate | Demo data and card/create callbacks duplicate MockAppSeedTests, TripsContentTest and ShellNavStateTest; the existing seeded emulator shell journey retains app wiring coverage. |
| `TripPageSheetRouteTest` | 0.047 | shrink | Nav3 must retain the same entry ViewModel/list/scroll across sheet-route replacement; fake source and plain Application. |
| `TripPageViewModelFactoryTest` | 0.087 | keep | Actual Android creation extras/entry factory hold and release the real session sync lease; no MainActivity or app root. |
| `TestSeedsApplyTest` | 0.005–0.021 | keep | Android seed hooks recreate the test directory, apply device configuration and forward intent flags. Keep the real hook/graph integration, no Activity. |
| `ReleaseReadinessTest` | 0.000–0.046 | keep | Classpath/reflective release boundary only (listed for completeness); never creates MainActivity or AppGraph. |
| `ReleaseSeedExtrasTest` | 0.654 | shrink | Call the real release Intent override boundary and assert no mock source directly; classpath exclusion tests cover absence of all seed writers. Remove the indirect Activity/directory check. |
| `LaunchForcedExitTest` | 0.099–0.152 | move to JVM | Controller/readiness/frame-loop deadlines and force count without an Activity, composition or rasterization. Review round 1 keeps stored data genuinely unknown and tests real controller completion; native overlay teardown remains in LaunchReadyExitRenderTest. |
| `FlightPathStrokeTest` | 0.016 | keep | Android native PathMeasure matches the sampled flight path; keep the framework geometry check. Its large first result was sandbox initialization. |
| `PasskeyAnchorTest` | 1.439 | shrink | Identity/weak-reference ownership only. Construct two Activity handles without starting their lifecycle or windows; retain every assertion. |

## Assertion coverage before shrinking/removal

These are assertion mappings, not claims that lower layers render a screen. Kept native checks cover the Android wiring that actually needs a window or lifecycle.

| Old assertions | JVM coverage / retained framework proof |
|---|---|
| LaunchGateRoutingTest signed-out route and unchanged revision after foreground/no intent | `SessionGateRouterTests#initiallySignedOutKeepsWelcomeRoute`, `SessionActivationTest#foregroundRunsTheIosStepsAfterEvictingIdleConnections`, `PendingLaunchIntentsTest#viewAndReminderIntentsAreHeldUntilLaunchFinishes`. The old case did not check a permission API. |
| LaunchGateRoutingTest Today, Verify, Terms, Walkthrough routes | `LaunchSessionBridgeTest#todayMapsToToday`, `#verifyMapsToVerify`, `#signedInTermsRoutesToTerms`, `#walkthroughMapsToFirstRunWalkthrough`; `StoredDataReadinessLaunchTests#verificationIsReadyWithoutMarkerAndSignedOutDoesNotReadDatabase`; `MockSessionSeedGateTests#signedInSeedUsesRestorableCredentialsAndAccountGates` validates every seeded stored gate on JVM. |
| LaunchGateRoutingTest unauthorized → SignIn and erased account files | Corrected in review round 1: those lower-level tests do not prove AppGraph callback wiring. `AppGraphSignOutWiringTest#signOutNavigatesErasesAccountFilesAndRejectsTheOldToken[unauthorized=true]` now exercises the real graph, with the gate observer stopped so it cannot mask a missing teardown detach/navigation callback. |
| LaunchIntentRestorationTest capture, history suppression, no replay after consume/finish | `AuthLinkIntentPolicyTest#capturesOnlyFreshHttpsViewIntents`, `AuthLinkDispatcherTest#attachTwiceDeliversOnce`, `PendingLaunchIntentTest` buffer assertions; retained `LinkRecreationTest#coldViewIsCapturedAndRecreationDoesNotReplayIt`, `#historyRelaunchDoesNotCaptureTheLink`, `#warmViewKeepsTheLaunchIntentAndCapturesTheLinkOnce` prove the actual lifecycle and scrubbed Intent. |
| ShellSeedTest selected tab, trip on Today, invalid route, consumption only once | Same four names now test `TestSeed.parse`, `DebugShellStart.take` and `ShellViewModel` directly; they fail if parsing, consumption or selected stack changes. `ShellNavStateTest#seedSelectsATab` / `#seedTripUsesTodayWhileKeepingOtherStacks` remain. |
| TripsTabTest demo active trip, card push, create wizard | `MockAppSeedTests#defaultAppMockSignsInDemoWithRNSettingsAndTrips`, `TripsViewModelTest#loadingUntilSynced`, `ShellNavStateTest#openTripUsesTheSelectedStack`, `#wizardPushOpensAnOverlay`; `TripsContentTest#cardAndCreateActions` checks the component callbacks. The existing `ShellJourneySmokeTest#tripsRefreshNavigationReselectAndSystemBack` checks the actual card → app route wiring. No new emulator journey. |
| TodayNavigationTest hero push, Today selected, back to Today | New `TodayActionsTest#heroPushesOnTodayAndBackReturns` calls the production action factory and shell model; retained `AppShellTest#backPopsPushedPageFirst` checks the installed back-handler and Nav3 subtree. |
| TwoFactorRoutingTest route replacement and route contains no challenge | New `TwoFactorRoutingModelTest#twoFactorReplacesSignInWithoutAChallengeInTheRoute` uses real AuthServices + SignInModel and a memory mock source. `AuthServicesTest.Navigation#routerNavigatesWithoutPuttingASecretInTheRoute` remains. |
| TwoFactorRoutingTest drawn back clears challenge, retains email, disables old model; re-entry creates empty fresh model | New `TwoFactorRoutingModelTest#backDiscardsChallengeAndKeepsTheEmail` / `#reenteringGetsAFreshModel`; `TwoFactorViewModelTest#backClearsTheChallengeAndDisablesTheOldModel` / `#secondVisitUsesTheCurrentModel` cover the ViewModel handler. Native system back/recreation stay. |
| TwoFactorRoutingTest accepted code clears challenge and routes through gate | `SignInModelRoutingTests.Committed#committedSessionUsesStoredGateWithAndWithoutCancellation` covers TwoFactor × each stored gate × cancellation; `TwoFactorModelRoutingTests#onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof` covers clearing and persistence. |
| PasskeyGraphTest enabled capability, server banner, no native prompt | Capability binding remains in `#activityAnchorAttachesAndDetaches`. New `AuthServicesTest.Ownership#unavailablePasskeyShowsTheServerBannerWithoutAcceptingASession` supplies valid options, counts the deterministic provider invocation and proves no login/session acceptance. No Android credential client is involved. The provider count prevents an earlier options failure from making the test pass. |
| VerifyRoutingTest sign-out → SignIn, inactive/disposed model, signed-out session, revoked token, wiped files | Corrected in review round 1: the coordinator/mock/wiper tests cover their own layers, but did not prove real AppGraph logout/erase/detach wiring. `AppGraphSignOutWiringTest#signOutNavigatesErasesAccountFilesAndRejectsTheOldToken[unauthorized=false]` now signs out through the real graph and checks SignIn, actual account deletion, and 401 from a fresh source with the old token. Existing coordinator tests retain model disposal assertions; VerifyRoutingTest retains native system back. |
| ResetRecreationTest done-continue → SignIn | `OpenResetTest#resetSuccessNeverAcceptsOrRoutesASession` calls real reset success and `backToSignIn`; the locale/secret saved-state test and done-state system back remain. |
| RootContentConstructionTest bridge + gate router choose Today, advance one revision, construct Today once | New `LaunchConnectionTests#coldLaunchBridgeAndGateRouterSelectTheSameToday` covers actual bridge/router emissions. `RootContentConstructionTest#gateRevisionKeepsTheCurrentTodaySubtree` independently proves Nav3 remembers one subtree across that revision. Existing incoming-route construction check stays. |
| CfRootHandOffTest Terms exists after hand-off; Welcome exists and overlay is gone | `LaunchSessionBridgeTest#signedInTermsRoutesToTerms`, `RootContentTest#termsRendersOneTaggedRoot`; retained `WelcomeHandOffTest#teardownMakesWelcomeAccessibleAndReportsFullyDrawnOnce` covers real Welcome/overlay teardown. The retained signed-in case still checks hidden→accessible semantics and overlay removal around a tagged child. |
| LaunchForcedExitTest forced route/count, completion and 8.400/8.950 endpoints | Review round 1 uses the real LaunchController with ManualLaunchClock and runLaunchOpening. Stored data never emits, no status starts at 2 s, no early exit commits at 7.9 s, readiness forces at 8 s, and the real frame-loop callback completes the controller at 8.400/8.950 s. No Activity or composition is involved. `LaunchReadyExitRenderTest#readyFadesAndLeavesOnTheFirstFrameAt1900` / `LaunchReduceMotionEndingTest#reduceMotionFadesWithoutScalingAtReadiness` retain the real render→controller completion bridge. |
| ReleaseSeedExtrasTest no test/mock directories after receiving extras | Direct `LaunchPlatform.overrides(realIntent) == LaunchOverrides.None` and `mockSources == null` replace the indirect directory observation. `ReleaseReadinessTest#releaseShipsNoSeedCode`, `#sessionSeedCodeIsAbsent`, `#mockDataSourceIsAbsent` prove the code which can create those directories is absent. No Activity is needed for the release policy. |
| Removed incidental graph construction in resource/layout tests | Every original assertion is retained, with `Application` instead of `ChronoFlowApp`. `AppGraphConstructionTest` retains guarded files/no-backup access and StrictMode death; the removed directory listing inspected a different Application from the guarded graph. |

`PasskeyAnchorTest#detachOnlyClearsTheAttachedActivity` retained all attach, foreign-detach, same-detach assertions and now takes 0.035 s after the existing clock test warms SDK 36. Its unnecessary two `.setup()` calls were the only non-cold quick case above 1 s (1.439 s).

No production code changed. All retained MainActivity checks are signed out unless verification/session persistence is their subject; none composes Today's live clock on a virtual-time dispatcher.

## Watchdog proof

`TestHangWatchdogTest#theFirstPlainJvmTestKeepsTheNormalCap` starts worker mode `first-jvm`. Its first test id is `[engine:watchdog-proof]/[test:firstJvmTest]`, it blocks on a latch for 200 ms under a 100 ms cap, and the parent requires exit 86 and a hang file naming that id/cap with the blocked stack. It never starts a preceding test. Removing `isRobolectric()` from the first-SDK condition would give this test 300 ms, let the worker finish normally, and fail the proof. A deliberate mutation removing that condition was run: the worker returned 0 instead of 86, the new proof failed, and the restored full watchdog proof suite passed.

## Before/after and validation

All changed test classes are listed below; ranges are warm **individual cases**, never whole-class totals. A dash in the before column means a newly added class. For a parameterized SDK-default warm-up marker, only the first matching parameter case is excluded, not every case of that method. The two application/launch probe pairs above supply warm results where a single test would otherwise be first in its fork. Removed cases map to the coverage table. First-class/cold costs remain recorded above.

| Changed class | Before warm per case (s) | After warm per case (s) |
|---|---:|---:|
| `AppGraphConstructionTest` | 0.029 | 0.019 |
| `AppLinkIntentFilterTest` | 0.002–0.008 | 0.003–0.008 |
| `AppShellRefusalGuardTest` | 0.043–0.063 | 0.046–0.089 |
| `AppShellTest` | 0.017–0.059 | 0.018–0.069 |
| `AuthServicesTest` | <0.001–0.006 | <0.001–0.004 |
| `BackupRulesTest` | 0.007–0.016 | 0.007–0.017 |
| `CfRootHandOffTest` | 0.050–0.514 | 0.163–0.286 |
| `ConfigurationPolicyTest` | 0.003–0.015 | 0.003–0.010 |
| `LaunchConnectionTests` | 0.002 | 0.001–0.009 |
| `LaunchForcedExitTest` | 0.099–0.152 | 0.002–0.025 |
| `LaunchGateRoutingTest` | 0.049–0.520 | deleted; assertion mapping above |
| `LaunchIntentRestorationTest` | 0.022–0.036 | deleted; assertion mapping above |
| `LauncherIconInflationTest` | 0.028 | 0.025 |
| `OkHttpStartupTest` | 0.082 | 0.055 |
| `PasskeyAnchorTest` | 1.439 | 0.035 |
| `PasskeyGraphTest` | 0.093–0.216 | 0.021 |
| `ReleaseSeedExtrasTest` | 0.654 | 0.027 |
| `ResetRecreationTest` | 0.171–0.234 | 0.217–0.259 |
| `RootContentConstructionTest` | 0.023–0.025 | 0.020–0.029 |
| `RootContentTest` | 0.020–0.038 | 0.020–0.038 |
| `RootRouteTransitionTest` | 0.024 | 0.025 |
| `RoutePlaceholderGuardTest` | 0.015–0.020 | 0.016–0.019 |
| `SeededScreenRecreationTest` | 0.181 | 0.229 |
| `ShellRecreationTest` | 0.053 | 0.054 |
| `ShellSeedTest` | 0.145–0.189 | <0.001–0.001 |
| `SignUpEntryScopeTest` | 0.126 | 0.120 |
| `SplashThemeTest` | 0.004–0.009 | 0.003–0.010 |
| `StoragePathsTest` | 0.004–0.006 | 0.003–0.005 |
| `TestHangWatchdogTest` | 0.001–0.894 | <0.001–0.771 |
| `TodayActionsTest` | <0.001–0.003 | <0.001–0.002 |
| `TodayNavigationTest` | 0.097 | moved: hero action 0.002 |
| `TodayTestBoundaryTest` | 0.008 | 0.004 |
| `TripPageSheetRouteTest` | 0.047 | 0.069 |
| `TripsTabTest` | 0.150–0.212 | deleted; assertion mapping above |
| `TwoFactorRoutingModelTest` | — | 0.001–0.003 |
| `TwoFactorRoutingTest` | 0.156–0.477 | 0.165–0.476 |
| `VerifyRoutingTest` | 0.406–0.615 | 0.527 |
| `ViewModelExtrasTest` | 0.027 | 0.007 |
| `WarmLaunchIntentTest` | 0.042–0.043 | 0.026–0.031 |

| Measurement/check | Before | After/result |
|---|---:|---:|
| App debug cases, all JVM/Robolectric tiers | 405 | 389 |
| `:app:testDebugUnitTest` full task wall, Gradle profile | 17.801 s | 13.148 s (−26.1%) |
| App debug total XML suite wall | 18.299 s | 12.386 s (−32.3%) |
| App debug sum of listener per-case times | 17.659 s | 11.789 s |
| App debug warm cases ≥ 1 s in final full run | first native-renderer case 1.287 s (0.514 s with render probe) | 0; first cold sandbox 1.264 s excluded |
| `make quick`, executed once | — | 34.617 s, exit 2: stale TodayTestBoundaryTest file assertion |
| Fixed boundary guard, targeted | — | pass |
| Final full watchdog suite, targeted | — | 8 cases pass; deliberate forbidden mutation rejected |
| Warm anchor identity check, targeted with existing clock control | 1.439 s | 0.035 s; pass |
| Build-logic tests, remaining quick step | — | pass (`make targeted ONLY='build-logic:convention:*'`) |
| Tooling scripts, remaining quick step | — | pass with make's IOS_REPO/WEB_REPO environment |
| Serial timing tier, remaining quick step | — | pass, 1.07 s command; reused stamped measurements (no budget-test changes) |
| Final Spotless check | — | pass |
| Touched app/launch layout tier, executed once | — | 170 cases pass, 13.330 s wall, no warm case ≥ 1 s (max 0.288 s) |

The XML suite wall is the span from the first suite timestamp to the last suite end, including inter-class gaps. To measure the complete Gradle task separately, two controlled targeted runs used `--profile`: original app test sources, then current app test sources, with all current files restored in `finally`. The task durations above come directly from the `:app:testDebugUnitTest` rows in `build/test-diet-task-wall/{before,after}/profile-*.html`. Their containing invocations took 20.642 s and 14.860 s respectively; both passed. The temporary profiling flag and original test sources were fully restored. No cold-start cost was subtracted from either task wall or XML span.

The quick invocation produced 2,397 measured cases and no existing harness-budget overruns. Its only failing case was the architecture guard still reading the deleted TodayNavigationTest path. The guard now checks that TodayActionsTest has no AppGraph/RootContent, Robolectric or Compose-test dependency. It passed targeted after the fix. The remaining quick steps were then completed separately, preserving the requested single `make quick` invocation. The identity outlier found by quick was also fixed and verified targeted. The full app test set had already passed before quick; all other quick test tasks passed during that invocation.

The first standalone tooling-script attempt lacked the environment variables normally exported by make; its exact failure was `AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. Re-running with those read-only source paths passed. Sandbox Kotlin-daemon/FSEvents warnings occurred, but Gradle compilation and all final checks completed. The temporary release rendering probe initially could not resolve ComponentActivity because the release test manifest does not declare that debug test host; its corrected resource-only probe passed. No failed fix was repeated blindly and no unexpected watchdog hang occurred.

After the one layout invocation, the two additional targeted full-app runs were used only to obtain the requested Gradle task-wall comparison; no tests or production behavior were changed.

Commands and complete outputs are retained under `build/test-diet-*.log`, the original/warm comparison XML under `build/test-diet-before/`, `build/test-diet-after/full/`, `build/test-diet-probe/`, and the final layout tier under `build/test-results/layout/`. `:core:data`, `:core:testing-junit` and `:konsist` contain no layout cases affected by this change; the one layout invocation selected `:app` and `:feature:launch`:

```sh
Scripts/run-tier.sh layout \
  --results 'app/build/test-results/testDebugUnitTest/*.xml' \
  --results 'feature/launch/build/test-results/testDebugUnitTest/*.xml' \
  --timings 'app/build/cf-timings/testDebugUnitTest/*.json' \
  --timings 'feature/launch/build/cf-timings/testDebugUnitTest/*.json' \
  -- ./gradlew --console=plain :app:testDebugUnitTest :feature:launch:testDebugUnitTest -Pcf.tier=layout
```

## Commits

All commits are signed, have no trailers, and remain local on `chore/app-test-diet`. The default verifier cannot reach the host keybox service from the sandbox. `%G?` below was obtained with `git -c gpg.program=... log`, using a temporary verifier home containing only the public key for the user's configured full signing fingerprint `BA0F82C4F60B6014D7281E76A057A81A19909681` and local owner trust for that known identity; private keys and host configuration were untouched.

| Commit | `%G?` | Area |
|---|:---:|---|
| `864f944f` | G | test(testing): prove the first jvm test keeps its hang cap |
| `10b93c83` | G | test(app): test shell routes without launching the signed-in app |
| `8a149102` | G | test(launch): check forced exits on the coordinator and timeline |
| `5a60304b` | G | test(app): move authentication routing checks to models |
| `ed18cf1b` | G | test(launch): isolate hand-offs and remove duplicate routing flows |
| `79398a7f` | G | test(data): check passkey anchor identity without activity windows |
| `79257ed7` | G | test(app): use lightweight hosts for resources and layout guards |
| `4e881461` | G | test(app): enforce the jvm action boundary and warm case budget |

The documentation commit containing this report is reported separately in the hand-off to avoid a self-referential commit hash. No merge, push or sibling-repository write was performed.




## Review round 1

The coverage claim for the two removed sign-out cases was incomplete: AppSessionFixture copies teardown wiring and omits the production logout/detach callbacks. A new Activity-free real-AppGraph test now covers both unauthorized and voluntary sign-out as separate parameter cases. Both begin with a signed-in demo, prove the account directory and token are valid, route through ScreenSessionFixture, stop the independent gate router, then assert SignedOut, SignIn, erased account files, and 401 through a newly constructed mock source. Unauthorized intentionally skips logout in SessionController; its token is first revoked server-side to model the received 401. Only the voluntary case claims to prove the production logout callback.

Mutations of real AppGraph callbacks were rejected: no erase → both cases leave the account directory; no logout → voluntary old token still works; no detach → both stay on Today; disabled onSignedOut navigation → both stay on Today. Each mutation was restored, and the unmodified test passed. Initial warm XML cases were 0.682 s (unauthorized) and 0.201 s (voluntary), with AppGraphConstructionTest warming SDK 36 first. Mutation XML/logs are under `build/test-diet-review-1/mutation-signout-*`.

The warm Robolectric gate now enforces 1,000 ms in TestTimingListener and the tier metadata; the existing 20,000 ms first-case-per-SDK allowance remains. `TimingClassifyTest#secondRobolectricOnSameSdkIsOver` checks 1,500 ms is over. Restoring the 5,000 ms constant made both warm-budget classifier tests fail.

The former Today-only guard is replaced by a Konsist rule over `app/src/test*`, `feature/*/src/test*` and `core/*/src/test*` (including testFixtures). It checks executable tokens in every top-level declaration, so fully-qualified and imported-alias calls are covered; comments, strings and manifest-only type references are not calls. The explicit class/fixture allowlist and one-line reasons are in `konsist/src/test/kotlin/net/chronoflow/konsist/AppTestBoundaryRules.kt`; supporting fixtures are named separately instead of exempting entire files or modules.

Proofs: planting a fully-qualified AppGraph construction in a real feature test source made the project guard fail. Eleven parameterized planted controls cover graph construction, MainActivity launch, ActivityScenario launch and root composition across app/feature/core test, testDebug, testRelease and testFixtures. Disabling the rule made all eleven rejection proofs fail. Each mutation was restored and the complete targeted guard and classifier suites passed. Evidence: `build/test-diet-review-1/mutation-{project-boundary,disabled-boundary-rule,five-second-budget}*`.

LaunchForcedExitTest now drives LaunchController and its real frame-loop exit callback, without composing an overlay or booting an Activity. Both `hasStoredData()` and `storedDataUpdates()` remain suspended; assertions at 2 s require a null stored-data value and no status. The test checks forced readiness at 8 s, stays active at 8.399/8.949 s, and completes at the exact 8.400/8.950 s frame with the controller timestamp, route and overlay state. Robolectric supplies the controller's Android Rect type; no pixels, fonts or window are built.

Mutating the fixture back to `emit(false)` fails the unknown-data test at its null-state assertion. Disabling LaunchController's completion assignment fails both controller cases. Both mutations were restored; the targeted controller cases passed with an existing lifecycle test warming SDK 36 first. Evidence: `build/test-diet-review-1/mutation-{known-absent-data,controller-exit}*` and `forced-exit-restored.log`.

Fixture refinement after the quick run: the test now creates the real AppGraph with a plain Android Application, software cipher, zero mock delay and an isolated temporary root. It accepts the real demo API session through `graph.controller.accept`; background sync is disabled because account teardown does not need the UI seeder's full trip/plan pull. All real teardown callbacks remain intact and all four wiring mutations were rerun and rejected. Final targeted warm XML times: unauthorized 0.545 s, voluntary 0.132 s, with SDK 36 warmed by AppGraphConstructionTest. The temporary store is removed after graph close.

Enforcement refinements after quick: candidate files are cheaply filtered by the protected identifiers before requesting only top-level/non-local Konsist declarations. This preserves nested-body checks while avoiding unrelated/nested declaration traversal; the project rule fell from 1.782 s under quick contention to 0.089 s targeted. The planted project violation and all eleven negative proofs were rerun after the optimization.

The listener also now recognizes an SDK marker before trailing JUnit parameters (`case[36][unauthorized=true]` → SDK 36), preventing a second `default` cold allowance in an already-warm SDK. The new parser test fails against the old end-only regex; the restored classifier and full watchdog suites pass. The first plain JVM cap, per-SDK cold allowance and 1,000 ms warm budget remain unchanged.

### Review round 1 validation

| Check | Result |
|---|---|
| Real graph sign-out, final targeted warm cases | unauthorized 0.545 s; voluntary 0.132 s; both pass |
| Konsist project rule / planted cases | project 0.089 s; eleven rejection cases pass (0.000–0.279 s) |
| Classifier and watchdog proofs | 8 classifier and 8 watchdog cases pass; all deliberate mutations rejected |
| Controller forced exits, warm cases | unknown stored data 0.020 s; unknown session 0.011 s; both pass |
| `make quick`, exactly once | 38.519 s; Gradle assertions/build pass, tier gate exits 3 (make exits 2) on timing overruns |
| Remaining quick steps after the timing failure | build-logic targeted suite, tooling scripts and serial timing tier pass |
| Touched-module layout tier, exactly once | 170 cases pass; 12.943 s; no warm case over 1 s |
| Final Spotless | pass |

Warm Robolectric overruns from the quick run, retained for follow-up (no budget increase or exemption):

| Class#method | Measured warm time | Disposition |
|---|---:|---|
| `ProcessDeathRestoreTest#processDeathDropsTheRegistryAndClaimsAColdOverlay` | 1.009 s | Existing real saved-state/splash lifecycle check retained; no coverage-reducing shortcut. |
| `RootThemeTest#activityProvidesReduceMotionAndPreloadsOnlyAfterItsFrameCommit` | 1.318 s | Existing real frame-commit/font-IO/window check retained; no trivial reduction identified. |

The quick run also measured the original graph fixture's first parameter at 1.995 s and mislabeled it `default` SDK warm-up. SDK 36 was already initialized in that fork, so this is recorded rather than accepted as another cold exemption. The fixture and SDK parsing refinements above address that issue; the current graph cases are both classified SDK 36, budget 1,000 ms, status `ok`. The initial new Konsist rule's 1.782 s JVM overrun was likewise fixed and verified targeted. Quick was not rerun.

There were no unexpected hangs and no relaxed budgets. The initial guard compilation error and formatter long-line errors were corrected before quick. Logs, comparison XML and mutation evidence are retained in `build/test-diet-review-1/`; the original quick timing report is copied to `quick-timings.json`, and final targeted XML to `targeted-final/`. No application production behavior changed.


## Review round 2: process restoration

The SDK 36 native-graphics sandbox is warmed by AppGraphConstructionTest before the two changed classes in the controlled targeted comparisons. Its first-case XML cost was 1.395 s before and 1.189 s after; those values are separate from the warm cases below. Temporary `System.nanoTime` probes measured actual lifecycle calls and were removed before final validation. These are isolated targeted measurements, not a decomposition of the parallel quick run.

ProcessDeathRestoreTest now creates, saves and destroys its first real MainActivity without starting/resuming it. SavedStateRegistry restores in onCreate; the checks still use the real Activity's decision to pass or drop the saved Bundle, and retain every registry, launch-kind, overlay and splash assertion. A test Application supplies a paused graph main dispatcher and the ready diagnostic seed, avoiding unrelated session readiness/foreground jobs. Cleanup cancels and closes the graph. No application production behavior changes.

| ProcessDeathRestoreTest stage | Original ms | Revised ms |
|---|---:|---:|
| Lazy graph construction | 0.044 | 0.049 |
| Launch resets, both Activities | 13.731 | 11.518 |
| Build first Activity | 81.760 | 81.617 |
| First onCreate | 143.262 | 143.728 |
| First start/resume | 22.234 | removed |
| Save registry | 2.826 | 2.516 |
| First pause/stop | 1.175 | removed |
| First destroy | 1.223 | 1.514 |
| Build / create second Activity | 0.954 / 4.968 | 0.936 / 4.920 |
| Second destroy | 0.166 | 0.185 |

There are no frame advances, idle loops or UI waits in this test. Stage values do not include all JUnit/Robolectric overhead. The first Activity's class resolution and onCreate dominate; graph construction itself is lazy and negligible.

| Warm XML case | Original isolated | Revised isolated, profiled | Final uninstrumented targeted | Previous quick → this quick |
|---|---:|---:|---:|---:|
| processDeathDropsTheRegistryAndClaimsAColdOverlay | 0.319 s | 0.294 s | 0.294 s | 1.009 → 0.788 s |
| recreationRestoresTheRegistryWithoutClaimingOrHoldingSplash | 0.026 s | 0.022 s | 0.021 s | — → 0.130 s |

Mutation proofs preserve the integration coverage: passing the saved Bundle after process death fails with a non-null registry Bundle; dropping it during recreation fails with expected 1 / actual null. Both mutations were restored. The final targeted run passed. Evidence is under `build/test-diet-round-2/{baseline,final-profile,targeted-final,mutation-process-registry-retained,mutation-recreation-registry-dropped}`.


## Review round 2: root theme and frame commit

RootThemeTest retains the real MainActivity, CfRoot, window frame-commit callback and background font preloader. Its graph main dispatcher is paused, leaving Compose's UI/frame work and font IO running. The ready diagnostic seed avoids unrelated readiness work. The redundant explicit waitForIdle before runOnIdle is removed. Auto-advance is disabled and exactly one frame is advanced before checking zero preloads, so the font effect reaches its frame-commit barrier before the assertion. All original reduce-motion, zero-before-commit, real-callback, four-after-commit and off-main-thread assertions remain. Cleanup cancels and closes the graph.

| RootThemeTest stage | Original ms | Revised ms |
|---|---:|---:|
| Lazy graph construction | 0.037 | 0.034 |
| Build / create Activity | 2.459 / 4.604 | 2.297 / 4.500 |
| Start / resume | 0.084 / 0.423 | 1.725 / 15.301 |
| visible: native root composition/layout | 270.274 | 268.860 |
| Explicit waitForIdle | 4.801 | removed |
| Explicit frame advance | none | 3.144 (one 16 ms virtual frame) |
| runOnIdle, including assertions and callback | 20.141 | 10.600 |
| Real frame-commit callback, included above | 11.669 | 0.235 |
| Font completion wait | 14.818 | 12.977 (two polls, no further frames) |
| Activity cleanup | 9.098 | 11.603 |

The stages are isolated wall measurements; nested callback time must not be counted twice. There is no custom idle loop. The complete XML case was 0.386 s before, 0.389 s after with probes, and 0.398 s in the final uninstrumented targeted run. This does not demonstrate an isolated speedup. Native first composition remains the main cost. Splitting the current assertions would still require this same Activity/root composition to prove each real binding; the existing pure ReduceMotionTest and FontWarmupOrderingTest do not prove that MainActivity supplies the actual values/barrier.

Bypassing CfRoot's firstCommit barrier made the retained zero-preload assertion fail with expected 0 / actual 4. The mutation was restored and the final targeted run passed. An earlier explicit LEGACY graphics experiment was rejected: Robolectric keys its sandbox on graphics mode, causing a second cold sandbox and a 1.031 s process-restore case classified warm. Final code retains NATIVE graphics, with no new allowance.

### Review round 2 validation and remaining failures

| Check | Result |
|---|---|
| Final targeted AppGraphConstructionTest + both changed classes | pass, 4.809 s command wall |
| Mutation proofs | both saved-registry mutations and the font-barrier bypass fail for the intended assertions; all restored |
| make quick, exactly once | **fail**, 37.912 s wall, make exit 2 (Gradle test failure and two timing overruns) |
| Process-death case, quick before → after | **1.009 → 0.788 s**, now under budget |
| Matching-process recreation case, this quick | 0.130 s |
| Root-theme case, quick before → after | **1.318 → 1.104 s**, still over budget |
| First app SDK 36 sandbox, this quick | 4.048 s, existing 20 s cold allowance; excluded from warm comparisons |

Optimization stopped with RootTheme still above 1 s under normal quick contention. The isolated breakdown above cannot attribute the parallel quick run's additional 0.715 s to individual stages. No warm budget was raised, no exemption was added, and no coverage or production behavior was changed. The work remains incomplete for RootTheme's quick timing gate.

Quick also reported AppGraphSignOutWiringTest#signOutNavigatesErasesAccountFilesAndRejectsTheOldToken[36][unauthorized=true] at **1.445 s** (the voluntary parameter was 0.399 s). StubReadinessTests#nativeSessionDelegates failed with UncaughtExceptionsBeforeTest: a background coroutine completion accessed Dispatchers.Main after the test dispatcher was unset. The originating background job was not identified; this report does not attribute the failure to StubReadinessTests or claim it is unrelated to the current changes. No additional quick invocation or broader fix was attempted after the failed gate.

Spotless passed during quick. Round 2 did not request another layout tier. Full quick output, XML and timing metadata are preserved under `build/test-diet-round-2/quick-results/`; targeted, stage and mutation evidence share that parent directory. Both class changes are separate signed commits with no trailers.
