# Phase 4e results — 2026-11-18

Session acceptance, sign-out and forced wipe, stored-data readiness, the session gate router, lazy app/session graphs, foreground activation, Nav3 root routing and debug session seeds are implemented. All Phase 4e plan tasks are ticked. Device smoke validation and performance measurements remain pending with the user; they are not passing evidence.

## Commits

One signed commit completes each task, following the signed brief refresh. No committed task was rewritten after its successor started. Nothing was pushed.

| Task | Commit | Subject |
|---|---|---|
| Refresh | `69cc387` | `docs(phases): refresh the 4e brief against 4d` |
| T1 | `335b374` | `refactor(model): add session value types and unify session presence` |
| T2 | `5b615f5` | `feat(sync): port the session controller` |
| T3 | `e312afa` | `feat(sync): port sign-out and the forced wipe` |
| T4 | `0cd4005` | `feat(data): add the account wiper and session account database` |
| T5 | `c42ce8c` | `feat(sync): start session sync for the current epoch` |
| T6 | `d291577` | `feat(sync): port stored-data readiness` |
| T7 | `5fd252b` | `feat(sync): port the session gate router` |
| T8 | `f098510` | `feat(navigation): add routes and the root navigator` |
| T9 | `7fc507b` | `feat(app): wire the app graph and per-epoch session graph` |
| T10 | `45a3384` | `feat(app): launch on stored-data readiness and render root routes` |
| T11 | `4370a0a` | `feat(debug): seed signed-in sessions and route seeds` |
| T12 | `1681620` | `test(app): prove release ships no session seed or mock code` |
| T13 | `ceff4a8` | `test(app): prove cold launches route by session and gate` |
| T14 | This document's commit | `docs(session): record the session trace and phase 4e results` |

## Targeted validation

Every selector below was run through `make targeted ONLY="<selectors>"`. Times marked **wall** come from the command/tier timer; **Gradle** times are the elapsed duration printed by Gradle, including compilation. Exact wrapper wall times were not retained for some early runs, so the printed build durations are reported explicitly rather than presented as exact wall measurements. Later per-class runs avoid ambiguous attribution after a hang.

| Task | Selectors | Result / elapsed time |
|---|---|---|
| T1 | `core:model:SessionValueTypesTest core:model:SignedOutReadinessTest feature:launch:LaunchReadinessTests konsist:KonsistRulesTest` | Pass; model 3 s, launch 4 s, Konsist 3 s Gradle; 8 model cases |
| T2 | `core:sync:SessionControllerTests` | 22 pass; 2 s Gradle after coroutine-test opt-in compile correction (initial compile 7 s) |
| T3 | `core:sync:SignOutTests` | 12 pass; 2 s Gradle |
| T4 | `core:data:AccountWiperTests core:data:SessionAccountDatabaseTests core:data:DevicePreferencesWipeTests` | 13 cases; initial 11 pass / 2 fixture failures in 4 s Gradle |
| T4 fixes | `core:data:AccountWiperTests#markerSurvivesPartialFailureAndRestartRemovesDirectoryAndCredentials core:data:DevicePreferencesWipeTests#devicePreferencesSurviveDeletingAllAccountFiles` | Both pass; 4 s Gradle |
| T5 | `core:data:SessionSyncReadinessTests core:data:SessionWipeTests core:data:SessionSyncStartTest core:data:SyncTriggerTests` | All pass; 3 s Gradle; includes unchanged 4d trigger tests |
| T6 | `core:sync:StoredDataReadinessTests core:data:AccountReadinessTests core:data:SessionSyncReadinessTests app:StoredDataReadinessLaunchTests` | Sync 6 pass / 4 s Gradle; data 4 pass / 1 fixture failure / 2 s; app not reached |
| T6 fixture fix | `core:data:AccountReadinessTests#sqliteMarkerAndGatePrecedenceAreObservedTogether app:StoredDataReadinessLaunchTests` | Pass; data 1 s / app 2 s Gradle |
| T6 gate observation completion | `core:sync:StoredDataReadinessTests app:StoredDataReadinessLaunchTests` | Pass; 1 s per group Gradle |
| T7 | `core:sync:SessionGateRouterTests` | 6 pass; 2 s Gradle |
| T8 | `core:navigation:RouteSerializationTest core:navigation:ResetRouteSerializationTest core:navigation:RouteDebugNameTest core:navigation:RootNavigatorTest core:navigation:GateRouteTest core:navigation:PendingLaunchIntentTest feature:launch:LaunchReadinessTests konsist:KonsistRulesTest` | All pass; navigation 72 cases / 3 s, launch 42 / 3 s, Konsist 3 s Gradle |
| T9 original | `app:SessionActivationTest app:SessionGraphsTest app:DataSourceSelectionTests app:AppGraphConstructionTest app:ViewModelExtrasTest app:SessionLifecycleTests core:data:AccountWiperTests` | App 15 pass / 1 timeout; 66.94 s wall; stopped before data group |
| T9 authorized diagnostic | `app:SessionActivationTest#backgroundCancelsAFlightAndForegroundReenablesAndResumes` | Temporary 10 s timeout located the initial await; 13 s Gradle; details below |
| T9 fixed | `app:SessionActivationTest` | 5 pass; 2 s Gradle / 0.247 s JUnit suite |
| T9 lazy-root wiper | `core:data:AccountWiperTests` | Pass; 2 s Gradle |
| T10 original aggregate | `app:LaunchConnectionTests app:LaunchSessionBridgeTest app:FirstCommitReadinessTest app:PendingLaunchIntentsTest app:RootContentTest app:CfRootHandOffTest app:RoutePlaceholderGuardTest app:StubReadinessTests` | Hung without a new result file; stopped daemon; 104.56 s wall; not passing evidence |
| T10 authorized diagnostic | `app:PendingLaunchIntentsTest` | Short-timeout marker run also blocked in cleanup; daemon stopped; no published markers or fresh XML |
| T10 fixed | `app:PendingLaunchIntentsTest` | Pass; 9 s Gradle |
| T10 root structure/recreation | `app:RootContentTest` | Initial 1 pass / 2 theme-fixture failures; 7 s Gradle. Recreation case passed |
| T10 root fixture fix | `app:RootContentTest#termsRendersOneTaggedRoot app:RootContentTest#repeatingARouteReplacesItsRememberedSubtree` | Structure passed; retained-entry revision failure remained; 5 s Gradle |
| T10 revision fix | `app:RootContentTest#repeatingARouteReplacesItsRememberedSubtree` | Pass; 5 s Gradle |
| T10 hand-off | `app:CfRootHandOffTest` | Pass; 5 s Gradle |
| T10 placeholder | `app:RoutePlaceholderGuardTest` | Pass; 4 s Gradle |
| T10 stub | `app:StubReadinessTests` | Pass; 2 s Gradle |
| T10 construction | `app:AppGraphConstructionTest` | Pass; 3.68 s wall |
| T10 first commit | `app:FirstCommitReadinessTest` | Pass; 1.47 s wall |
| T10 navigation ownership | `app:LaunchConnectionTests` | Pass; 1.37 s wall |
| T10 bridge | `app:LaunchSessionBridgeTest` | Pass; 1.37 s wall |
| T10 trace | `feature:launch:LaunchTraceTests` | Pass; 3.75 s wall |
| T11 parser | `app:TestSeedSessionKeysTest` | Pass; 5 s Gradle |
| T11 gate matrix | `app:MockSessionSeedGateTests` | All 8 account/gate rows pass; 4.51 s wall |
| T11 seed | `app:MockSessionSeedTests` | Pass; 2.84 s wall |
| T11 production reseed path | `app:ReseedEqualsColdSeedTest` | Pass; 2.84 s wall |
| T11 apply | `app:TestSeedsApplyTest` | Pass; 4.16 s wall |
| T11 existing parser | `app:TestSeedTest` | Pass; 2.50 s wall |
| T12 | `app:ReleaseReadinessTest` | Pass; 6 s Gradle |
| T12 extras rejection | `app:ReleaseSeedExtrasTest` | Pass; 4 s Gradle |
| T13 initial | `app:LaunchGateRoutingTest` | 6 StrictMode disk-write fixture failures; 6 s Gradle; fixed IO dispatcher injection |
| T13 fixed | `app:LaunchGateRoutingTest` | 6 pass; 15.38 s wall; individual cases 0.122–4.849 s, no budget violation |
| T13 device | `app:SignedInColdLaunchSmokeTest` | APK compiled; no connected device; exit 1 in 7.42 s wall; no test result |

T9's other original app suites passed: construction (1), source selection (2), session graphs (3), lifecycle (4), ViewModel extras (1). Test fixtures were also corrected for actual `SettingsRecord.section` and nullable `parentFile` during T4 compilation, and explicit property types during T9 compilation. T4's WAL fixture retains a real reader; its preference-wipe test uses the Android driver under Robolectric. T6 explicitly seeds an unverified account instead of relying on a fixture default. These corrections preserve production behavior.

`make format` passed for each completed task. Kotlin daemon marker writes under the sandbox's protected Library directory sometimes print `Operation not permitted`; the affected compilations fall back and exit successfully. This was not counted as a failed test.

### Hang evidence and corrections

The authorized T9 diagnostic printed:

```text
CF-STEP 1 accepted
CF-STEP 2 configured
CF-STEP cleanup begin
```

It was stuck at the **initial** `entered.await()`. Asynchronous `SessionSync.requestStart()` had not attached a lifecycle when the test called `refreshNow()`, so no request reached the source hook. The test now uses the existing bounded wait to establish the initial debounce before forcing refresh. ON_STOP cancellation does not create a retry; ON_START resume requests a normal debounced flight. The resumed order is asserted as `evict → enabled:true → foreground → unauthorized → retry → current → resume`. Production sync behavior was unchanged. The final class run passed; all markers were removed and the 60 s cap restored.

The user isolated T10's hang to `PendingLaunchIntentsTest`. Its authorized marker run did not publish system-out because non-cancellable teardown also stalled. The real graph used a dispatcher confined to Robolectric's paused main looper while `runTest` occupied that thread. `GraphOverrides.dispatchers` now lets suspend graph tests share the test scheduler for main; actual disk uses IO under StrictMode. The class then passed. UI-only Compose fixtures continue to use their looper-driving rules. No production lifecycle behavior was changed to satisfy the fixture.

T10's repeat-navigation assertion additionally exposed a retained Nav3 entry reading an old revision. `RootContent` now supplies the current revision and renderer through `rememberUpdatedState`, so a repeated route resets its subtree. The scoped regression passed.

## Brief patches

The refresh commit body records its mechanical patches:

1. Updated plan locations, the teardown-list description, single-lane execution and user test/perf overrides.
2. Corrected `AccountIdentity` alias direction, failure constructors and redaction descriptions.
3. Identified both `SessionPresence` declarations, including the one embedded in `LaunchReadiness.kt`, and same-package users that must migrate.
4. Corrected repository/sync/store packages and the login-upsert extension location; replaced nonexistent `SyncFixtures` references with landed top-level helpers.
5. Recorded private launch callbacks, native diagnostic readiness injection and the trace integration seam.
6. Preserved AppGraph's existing font/launch constructor seams and corrected the release-only source folder to `releaseNoPerf`.
7. Recorded non-cancellable AccountModel/SessionSync teardown, suspending IO connection eviction, cipher threading, device identity and mock latency/controls.
8. Corrected dispatcher type usage, concrete seed database access, the suspend lazy-root constructor, Gradle aliases and existing Konsist conventions.
9. Retained the landed placeholder surface, removed the conflicting screen-copy assertion, corrected ScreenTest's package/signature and restored Appendix B's table rule.
10. Required cancellation propagation in logout racers and non-cancellable teardown completion.

No earlier-phase behavior required a preparatory change during refresh. Later task commits document T5's forward readiness-test dependency, the user-authorized dispatcher override and debug seed seam, and the separate eight-row seed matrix class. T14 corrects the brief's remaining OQ maximum: the landed plan includes A170 and A171, so this phase starts at A172, preserving A68's intentional gap.

## Plan defaults

Added **OQ-A172–A181**, corresponding in order to OQ-4e-2, -3, -4, -5, -7, -10, -13, -15, -16 and -17. They bind production readiness in all variants, shared SessionSync ownership, the two missing account flags, memory-only reset secrets, process-only seeded roots, reminder extra names, variant mock construction, route mapping ownership, readiness test placement and software-cipher/dispatcher graph seams. A4, the seeds/navigation specifications and the 3b/4e task descriptions now match these defaults.

## Release and pending device/performance evidence

`./gradlew :app:verifyReleaseManifest :app:assembleRelease` passed in 31 s Gradle time. The release readiness/extras classes passed separately. `apkanalyzer dex packages app/build/outputs/apk/release/app-release-unsigned.apk` succeeded and `grep -c datamock` printed `0` (grep exit 1 means no matches). APK inspection found only the existing `libandroidx.graphics.path.so` under the four ABIs, allowed by OQ-0a-22; no new `.so`. The bundle size target itself was not run.

`./gradlew :benchmark:compileBenchmarkReleaseKotlin` passed in 2 s Gradle time after adding the session metric. This compiles the measurement but does not execute a device tier.

Preparation command and exact output (exit 1):

```sh
Scripts/emulator.sh prep-functional emulator-5554
```

```text
adb: device 'emulator-5554' not found
```

Device query and output (exit 0; empty device list):

```sh
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices -l
```

```text
List of devices attached

```

The sole smoke attempt:

```sh
make targeted ONLY="app:SignedInColdLaunchSmokeTest"
```

```text
> Task :app:connectedDebugAndroidTest FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:connectedDebugAndroidTest' (registered by plugin 'com.android.internal.application').
> com.android.builder.testing.api.DeviceException: No connected devices!

BUILD FAILED in 7s
385 actionable tasks: 17 executed, 368 up-to-date
Configuration cache entry stored.
make: *** [targeted] Error 1
```

Full logs, XML copies and available tier timings are retained under ignored `build/phase4e/`, including the earlier stop reports as historical evidence. No missing/stale XML is treated as a pass. The smoke command needs the user's visible pixel emulator; the manual unseeded cold launch is also unverified.

User-run measurements remain **Session known, signed out**, reference ≤50 ms / low-end ≤100 ms after first-commit, report only, through `make perf`. Baseline/startup profiles need `make baseline-profile` after the startup change. These measurements and profile regeneration were explicitly delegated; none were run and no number is fabricated. `make check`, `make perf-timeline` and other emulator tiers were not run. See [startup performance](perf-startup.md) and [tooling](tooling.md) for setup.

## Final quick result

`make quick` was invoked **once**, taking **27.61 s wall** (27.48 s for its primary tier). All **2,185 cases passed**, but the wrapper exited 2 because the timing gate found `KonsistRulesTest.routesAreSerializable` at **2,350 ms**, above the 1,000 ms JVM budget. The original quick invocation is therefore a **failure**, not a passing command. No full quick rerun was performed.

The rule previously resolved ancestry even for declarations already annotated `Serializable`, which cannot violate it. It now filters those declarations before the unchanged indirect-NavKey check. The scoped project check and its planted violation/allowed control passed in **3.72 s wall**:

```sh
make targeted ONLY="konsist:KonsistRulesTest#routesAreSerializable konsist:KonsistRulesTest#routesAreSerializableRejectsPlantedViolation konsist:KonsistRulesTest#routesAreSerializableAllowsControl"
```

Recorded method times: project **507 ms**, planted violation **772 ms**, allowed control **1 ms**, all under budget. The serialization requirement and inheritance coverage are unchanged; no threshold was relaxed.

The remaining quick steps then ran once each, with the Make environment exported:

| Command | Result | Wall time |
|---|---|---:|
| `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | 26 pass | 1.50 s |
| `Scripts/test_scripts.sh` | 107 script, 25 contract, 49 l10n and 9 icon cases pass | 14.05 s |
| `make timing` | 6 pass, zero budget violations | 2.64 s |

The initial invocation, scoped correction and remaining steps total **49.52 s wall**. `make format` and `git diff --check` pass. The quick tier, its initial timing failure and the scoped passing timings are retained in `build/phase4e/`. No assertion failure, new phase test budget overrun or hang occurred during this final sequence.

An optional signature-display probe, `git log -14 --format='%h %G? %s'`, produced no output and was interrupted (exit 130). All commits were created with `git commit -S`; commit objects were inspected directly and contain GPG signature headers. No unsigned fallback was used. An earlier read-only process probe, `ps -Ao pid,etime,command | rg 'Gradle Test Executor|targeted.py app:SessionActivation'`, was sandbox-blocked with exact output `zsh:1: operation not permitted: ps`. Neither probe supplies test evidence.
