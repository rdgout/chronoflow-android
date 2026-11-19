# Phase 6e orchestrator gate round

Branch preflight: `feat/phase-6e-trip-wide`. Baseline profile files are excluded from these fixes and remain owned by the orchestrator.

## Fix 1 — older wide layout guards

Root cause: TripPageBoardGuardTest predates 6e and assumed the compact band always exposes one semantic child (phone lanes). The wide implementation matches iOS at f391a1fa: day/date and zone above lane headings. The PlanWideScrolled board's Steps label is deliberately absent under OQ-A305. The guard now requires the phone lane child or all three wide tagged children, according to window width; production UI is unchanged.

- `make targeted ONLY=feature:trip:TripPageBoardGuardTest`: PASS, 64 cases, 11.94 s wall, no budget overruns. The initial compile needed the collection filter extension import.
- `Scripts/run-tier.sh layout --results 'feature/trip/build/test-results/testDebugUnitTest/*.xml' --results 'app/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/trip/build/cf-timings/testDebugUnitTest/*.json' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:trip:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout`: PASS, all 376 layout cases across both modules, 35.58 s wall, no budget overruns.
- `make format`: PASS, 20 s Gradle wall.

Evidence: `build/test-results/phase-6e-gate/fix1/`.
