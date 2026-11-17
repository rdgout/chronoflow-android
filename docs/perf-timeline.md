# Timeline performance

## JVM budgets (1b, 1c), in quick

Decode + `layoutPlan` + `preparePlan`, cold (no cache), after three warm-ups; the limit applies to every one of 10 samples. Command: `make timing` (`./gradlew :core:plan:timingTest --max-workers=1`), test class `TimelinePreparationBudgetTest` (`@TimingGate`). Host process on a Mac, not a device claim. Same limits for every build type.

Environment: macOS arm64 (`uname -m` = `arm64`), Gradle launcher Zulu 17.0.21, test JVM Eclipse Adoptium 21.0.13 from the JDK 21 toolchain (`~/.gradle/jdks`, foojay). Run on 2026-11-17 on the Phase 1b branch (`04c9e62`), three consecutive `--rerun` runs on a machine that also ran an idle emulator.

| Fixture | Budget (every sample) | Run 1 mean / max (ms) | Run 2 mean / max (ms) | Run 3 mean / max (ms) | iOS mean (ms) |
|---|---|---|---|---|---|
| S3 | 40 | 2.9 / 3.8 | 3.0 / 3.8 | 2.9 / 4.1 | 19 |
| large30 | 100 | 4.5 / 5.2 | 4.6 / 5.2 | 4.4 / 5.2 | 50 |
| large60 | 200 | 7.2 / 8.2 | 7.5 / 8.4 | 7.0 / 8.8 | 92 |
