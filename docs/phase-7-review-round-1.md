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
