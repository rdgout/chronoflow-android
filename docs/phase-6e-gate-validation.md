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
