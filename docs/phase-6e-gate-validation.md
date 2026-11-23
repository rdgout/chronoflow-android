# Phase 6e orchestrator gate round

Branch preflight: `feat/phase-6e-trip-wide`. Baseline profile files are excluded from these fixes and remain owned by the orchestrator.

## Fix 1 — older wide layout guards

Root cause: TripPageBoardGuardTest predates 6e and assumed the compact band always exposes one semantic child (phone lanes). The wide implementation matches iOS at f391a1fa: day/date and zone above lane headings. The PlanWideScrolled board's Steps label is deliberately absent under OQ-A305. The guard now requires the phone lane child or all three wide tagged children, according to window width; production UI is unchanged.

- `make targeted ONLY=feature:trip:TripPageBoardGuardTest`: PASS, 64 cases, 11.94 s wall, no budget overruns. The initial compile needed the collection filter extension import.
- `Scripts/run-tier.sh layout --results 'feature/trip/build/test-results/testDebugUnitTest/*.xml' --results 'app/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/trip/build/cf-timings/testDebugUnitTest/*.json' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:trip:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout`: PASS, all 376 layout cases across both modules, 35.58 s wall, no budget overruns.
- `make format`: PASS, 20 s Gradle wall.

Evidence: `build/test-results/phase-6e-gate/fix1/`.

## Fix 2 — wide benchmark window lifecycle

The original failure was caused by the perf tier's cleanup, not a missing list column. Its timing evidence starts at 15:01:24 UTC and ends after 652.31 s; device logcat switches from 701 dp to 411 dp at 15:12:16 UTC, during the first wide measured iteration. The next launch at 15:12:34 UTC is therefore a phone window and cannot expose the wide list tag. Fix 3 addresses that premature tier termination. The wide benchmark now also reapplies its window override in each iteration's setup before launching, matching the wide profile generator's fixture lifecycle. Both its finally block and after hook retain window cleanup.

The root window seam uses LocalWindowInfo.containerSize, which reads current Activity window metrics. ConfigurationPolicy does not freeze width or density. A direct cold launch of the same benchmark APK, large60 fixture, entry and frozen clock on pixel exposed `trip.wide.list.day-29864460-tour-0` as a resource id at bounds [1052,571][1787,2208], beside the opening timeline. The app window was 1840×2208 at 420 dpi (701 dp wide). The probe reset it to 1080×2400 in finally.

- Ran the requested single benchmark once: `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.TimelineBenchmark#timelineScrollWide -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000 -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR`: PASS, one test / three measured iterations, 238.692 s test time, 277.66 s host wall including build and cleanup (Gradle 4 m 35 s).
- The host wrapper held the device lock, ran prep-perf first, then reset the window and ran prep-functional in finally. Final `adb -s emulator-5554 shell wm size`: `Physical size: 1080x2400`. Pixel remains running. No baseline profile was regenerated.

Evidence: `build/test-results/phase-6e-gate/fix2/`, including the original connected test log.

## Fix 3 — quiet connected task trips the idle cap

The five methods share one connected Gradle invocation, which emits no output between its start and completion. Their aggregate duration exceeds the tier's 600 s idle cap even though each method continues making progress. Scripts/perf-timeline.sh now wraps that same invocation with a TestRunner logcat stream. Only started/finished records for the exact TimelineBenchmark class are forwarded and flushed. There is no timer heartbeat; idle and total caps are unchanged. The monitor is terminated and reaped when Gradle succeeds, fails or is interrupted. Archives, band-miss reruns, checker arguments, device selection and window/functional cleanup retain their existing behavior. Dry-run prints the wrapper and its complete Gradle command without starting the stream.

- `python3 -I -m unittest discover -s Scripts/tests -p 'test_perf_timeline.py' -v`: PASS, 17 tests in 4.362 s.
- `python3 -I -m unittest discover -s Scripts/tests -p 'test_test_runner_progress.py' -v`: PASS, 5 tests in 0.913 s. Includes actual child-process cleanup after success/failure and a stalled-method integration test that is killed by the original idle cap despite its initial TestRunner event.

The full device tier is left for the orchestrator: `make perf-timeline`. The single wide benchmark above was the only benchmark run in this gate round. Baseline profiles remain untouched; the orchestrator regenerates them.

## Final verification

- `make quick`, once: PASS, about 55 s overall. JVM/Robolectric: 3,595 cases, 31.78 s wall; build logic: 37 cases, 1.72 s wall; tooling: 247 cases (164 script, 25 contract, 49 localization, 9 icon), 19.745 s combined unittest wall; timing: 6 cases, 1.08 s wall. No budget overruns. This includes TimelineInvalidationTest and the existing timing gates.
- `./gradlew :app:lintDebug :feature:trip:lintDebug`, once: PASS, 12 s Gradle wall.
- `git diff --check`: PASS. No brief patches or OQ rows were needed in this round.

Evidence for Fix 3 and final verification: `build/test-results/phase-6e-gate/fix3/`.

Device follow-up commands for the orchestrator (not run here):

```sh
make baseline-profile
make perf-timeline
```

## Gate round 2 — phone list-to-timeline anchor

Preflight: branch feat/phase-6e-trip-wide, starting HEAD 4dfa6642. The regenerated profiles and the orchestrator's uncommitted perf-timeline.md results are left untouched.

The phone exposed zero matching travel-day nodes, not two. One diagnostic `make targeted ONLY='app:TripListSmokeTest'` run before the fix replaced the timeout temporarily with exact tag counts. It failed with `prep=1, travel=0, arrival=0 expected:<1> but was:<0>` in 2.210 s test time (7 s Gradle wall). The phone had switched to timeline but scrolled to the previous preparation day. Those diagnostic test edits were removed; the existing smoke test is unchanged.

Root cause: TripScrollTracker.SizeKey includes timelineMode, and onSize treated a mode-only change as a resize. The phone toggle already requests the target day's header, as in 6c/6d. That request can remeasure the old list before the new item table is composed. The tracker then captured that transient old-list position and overwrote the requested travel day with the preparation day. Tag placement on WideDayRow was correct.

The tracker now restores only when window width, density or resolved layout changes. It still updates its observed mode, and actual resizes that change modes still restore the day header. This matches iOS TripScrollTracker.resize at f391a1fa, whose restoration trigger is viewport width/layout, while the phone toggle owns its scroll request.

- Added JVM regression `phoneViewToggleDoesNotOverwriteTheRequestedDayWithTheOldListPosition`: the old code failed with `expected null, but was:<Restore(index=2, offsetPx=0)>`; the fix passes. Targeted red/green commands: `make targeted ONLY='feature:trip:TripScrollTrackerTest#phoneViewToggleDoesNotOverwriteTheRequestedDayWithTheOldListPosition'` (3 s / 2 s Gradle wall).
- Added the inverse phone mode-change guard and an S1 Robolectric structural check for one same-day timeline node and the correct header after a list-seeded toggle. The structural check also passes before the fix; the JVM stale-position case reproduces the actual race deterministically.
- `make format`: PASS, 10 s Gradle wall.
- `make targeted ONLY='feature:trip:TripScrollTrackerTest feature:trip:ViewToggleTest feature:trip:TripPageResizeTest'`: PASS, all 32 cases, 8.72 s host wall, no budget overruns.
- `./gradlew :app:lintDebug :feature:trip:lintDebug`: PASS, 16 s Gradle wall.
- `make quick`, once: PASS, 54.91 s host wall, no budget overruns.
- Final structure-only S1 check, `make targeted ONLY='feature:trip:ViewToggleTest#seededListTogglesToTheSameDayTimeline'`: PASS, 5.32 s host wall, no budget overrun.

The single allowed emulator invocation was used for diagnosis before the fix. Post-fix device handoff: `make targeted ONLY='app:TripListSmokeTest'`. Pixel remains running at 1080×2400 with animator scales 0. No perf tier or profile generator is run in this round.

Evidence: `build/test-results/phase-6e-gate/round2/`.
