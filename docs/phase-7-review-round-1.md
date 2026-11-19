# Phase 7 review round 1

Preflight: `feat/phase-7-today`, HEAD `96736893`. The existing modification to `docs/perf-timeline.md` and the untracked later-phase briefs belong to the user and are left untouched.

## 1. Cancelled refresh

Changed the JVM regression to `TodayViewModelTest.cancelledRefreshStopsRefreshingWithoutFeedback`. It fails before the fix because cancellation produced a SomethingWentWrong capsule (8.02 s command wall). Today now delegates directly to RefreshFeedbackModel like My trips: the shared model rethrows cancellation, clears refreshing in finally, and creates no feedback.

The same targeted method passes after the fix (4.62 s command wall). Command: `make targeted ONLY="feature:today:TodayViewModelTest#cancelledRefreshStopsRefreshingWithoutFeedback"`. Logs: `/tmp/phase7-r1-1-{red,green}.log`.

## 2. Future formatting context

Ported the Future card's options boundary from pinned TodayFutureTripCard.options: language, time format and display zone, without trip context. Active rows retain their trip context. The regression uses an instant in America/New_York while displaying Europe/Amsterdam and an airport city deliberately named Queens. It checks that Future options carry no trip, annotated formatting uses the IANA city New York, and the actual future subtitle retains the pinned unannotated 05:00 output. Both platforms default annotateInstantZone to false, so the redundant trip argument did not previously change that unannotated output; the explicit options contract supplies the failing assertion.

`make targeted ONLY="feature:today:TodayReducerTest#futureInstantInAnotherZoneUsesNoAirportCityContext"` fails before removal (4.78 s wall) and passes after it (2.59 s wall, 0.129 s case). A mechanical extraction of the unchanged options preceded the failing test. `make format` also formats the prior refresh assertion; one long test expression needed splitting before formatting passed. Logs: `/tmp/phase7-r1-2-{red,green}.log`.

## 3. Session epoch binding

The Today tab now passes a session-epoch key to the existing viewModel call. The key contains only the epoch; the perf/no-session path retains the default key. A pure JVM test uses the real ViewModelProvider/TodayViewModel factory and SessionController, fake stores/clock/source, and one retained ViewModelStore. It proves same-epoch reuse, rejection of the previous snapshot's permit, a fresh model after publish, and a successful write through the new epoch's source. With the original default key it reaches the real failure: the second source sees zero writes. No Activity, Compose renderer or database is needed.

Pinned iOS PlanHomeModel records mutationError, but a grep of all Swift sources finds no UI use. Android therefore retains its existing silent error policy rather than inventing a banner.

`make targeted ONLY="app:TodaySessionModelTest"`: initial test compilation needed the explicit SessionTeardown sleep fake (4.93 s). The compiled regression fails before the key fix at the second source's write count (3.46 s); it passes after keying (2.69 s wall, 0.173 s case). The unchanged default-key behavior was first extracted into the helper before the failing run. Logs: `/tmp/phase7-r1-3-red-compiled.log`, `/tmp/phase7-r1-3-green.log`.

## 4. Debugger and cold-fork hang caps

The watchdog checks RuntimeMXBean input arguments for JDWP and does not arm in that JVM. Its first test gets 3× the configured deadline (180 s at the default 60 s), because Vintage's start event precedes Robolectric sandbox/android-all resolution. Later tests retain the ordinary cap, and the timing tier remains unchanged. AGENTS.md records both exceptions.

Two new proof-worker tests fail before the change (4.09 s command wall): a JVM running a real loopback JDWP agent is incorrectly halted with exit 86, and the first test is halted before it can finish cold initialization. With the fix, the debug child survives past its cap; a 100 ms configured cap lets the first child test finish its 200 ms warm-up and then correctly halts the second test at 100 ms. The existing halt/evidence and cancellation proofs remain green.

`make targeted ONLY="core:testing-junit:TestHangWatchdogTest"` passes all five cases (3.84 s wall). XML seconds: cancellation 0.369; configuration 0.000; real JDWP 0.763; normal halt/evidence 0.248; cold-then-warm cap 0.455. Logs: `/tmp/phase7-r1-4-{red,green}.log`.

## 5. Navigation test boundary

Moved Settings, past trips and wizard-create assertions to plain JVM TodayActionsTest using the actual todayActions/ShellViewModel/ShellNav. The screen test now uses a plain Application and the real shell only; it retains hero tap plus system Back, with no AppGraph, RootContent or route-probe bootstrap. A cheap JVM architecture guard fails before this move because the screen suite boots AppGraph; it also enforces that this suite keeps only the hero/Back boundary. The action behavior was already correct, so the failing regression checks the misplaced test boundary rather than inventing a behavioral failure.

`make targeted ONLY="konsist:TodayTestBoundaryTest"` fails first with `Today action logic must not boot AppGraph` (5.59 s wall). After the move, the final touched-only command passes **38 cases**, **19.88 s wall**:

```sh
make targeted ONLY="feature:today:TodayViewModelTest feature:today:TodayReducerTest app:TodaySessionModelTest app:TodayActionsTest app:TodayNavigationTest core:testing-junit:TestHangWatchdogTest konsist:TodayTestBoundaryTest"
```

The remaining screen case takes 4.119 s on the cold targeted sandbox. The warm case times from the final quick run's copied XML are:

| Case | Seconds |
|---|---:|
| TodayNavigationTest.heroPushesOnTodayAndSystemBackReturns | 0.136 |
| TodayActionsTest.pastTripsSelectsTheMyTripsRoot | 0.003 |
| TodayActionsTest.settingsSelectsItsTabAndKeepsTheTodayStack | 0.001 |
| TodayActionsTest.createOpensTheWizardOverlay | 0.000 |
| TodaySessionModelTest.aNewSessionEpochReplacesTheModelAndUsesItsCurrentMutationPermit | 0.030 |
| TodayTestBoundaryTest.todayNavigationKeepsOnlyTheHeroBackScreenBoundary | 0.005 |

## Final quick run

`make quick` ran **once**, **54.59 s command wall** (54.51 s tier), Make exit 2 / tier exit 3. All **3,691** assertion cases pass; **no hang files** were produced. The tier fails on four timing overruns in untouched tests:

| Untouched case | Measured ms | Budget ms |
|---|---:|---:|
| HapticsRulesTest.noHapticsInLaunchRejectsPlantedViolation | 1,114 | 1,000 |
| MockSeedRefreshTests.restoresTenDayOldSeedWithoutLosingUserStateCurrentAnchor | 1,104 | 1,000 |
| ProfileFixtureCoverageTest.firstSixDayTourCoversEveryLarge60RowStyle | 1,090 | 1,000 |
| LaunchGateRoutingTest.pendingTermsLaunchesTerms | 6,062 | 5,000 |

Per the user's existing instruction for untouched timing overruns, there is no retry. The subsequent build-logic, tooling and serial timing steps did not start in this quick run. The focused watchdog proof was already green; no full layout, emulator, performance or profile tier was run in this review round. Evidence: `/tmp/phase7-r1-targeted.log`, `/tmp/phase7-r1-quick.log`, `build/test-results/quick/timings.json` and its copied `junit/` XMLs (the tier invalidates original task outputs when timing fails).
