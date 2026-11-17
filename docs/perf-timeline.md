# Timeline performance

## JVM budgets (1b, 1c), in quick

Decode + `layoutPlan` + `preparePlan`, cold (no cache), after three warm-ups; the limit applies to every one of 10 samples. Command: `make timing` (`./gradlew :core:plan:timingTest --max-workers=1`), test class `TimelinePreparationBudgetTest` (`@TimingGate`). Host process on a Mac, not a device claim. Same limits for every build type.

Environment: macOS arm64 (`uname -m` = `arm64`), Gradle launcher Zulu 17.0.21, test JVM Eclipse Adoptium 21.0.13 from the JDK 21 toolchain (`~/.gradle/jdks`, foojay). Run on 2026-11-17 on the Phase 1b branch (`04c9e62`), three consecutive `--rerun` runs on a machine that also ran an idle emulator.

| Fixture | Budget (every sample) | Run 1 mean / max (ms) | Run 2 mean / max (ms) | Run 3 mean / max (ms) | iOS mean (ms) |
|---|---|---|---|---|---|
| S3 | 40 | 2.9 / 3.8 | 3.0 / 3.8 | 2.9 / 4.1 | 19 |
| large30 | 100 | 4.5 / 5.2 | 4.6 / 5.2 | 4.4 / 5.2 | 50 |
| large60 | 200 | 7.2 / 8.2 | 7.5 / 8.4 | 7.0 / 8.8 | 92 |

### DayDrawing build (1c)

Run on 2026-11-17 at `9dbcd07` (Phase 1c branch). Host process on a Mac, not a device claim. `uname -m` = `arm64`; test JVM Eclipse Adoptium 21 from the JDK 21 toolchain (Gradle launcher `openjdk version "17.0.21.0.3"`). Command: `make timing` (`./gradlew :core:plan:timingTest --max-workers=1`), class `DayDrawingBudgetTest` (`@TimingGate`, `FakeTextMetrics`, density 2.625, textScale 1.0, light, 3 warm-ups, 5 measured passes). Gates: day median max ≤ 5 ms, per-width total median ≤ 150 ms, drawing ≤ 512 KiB.

```
DayDrawing metric: W=360 dayMedianMax=0.06 dayMax=0.10 totalMedian=0.9 bytesMax=11795
DayDrawing metric: W=411 dayMedianMax=0.04 dayMax=0.08 totalMedian=0.6 bytesMax=11795
DayDrawing metric: W=841 dayMedianMax=0.04 dayMax=0.06 totalMedian=1.3 bytesMax=14089
```

Cache and sizes (`TimelineRenderingTests`, large60, density 3, `FakeTextMetrics`; the cache stayed within 4 MiB and 8 entries after every put):

```
DayDrawing metric: largest 11795 bytes   (content 358)
DayDrawing metric: largest 13995 bytes   (content 637)
```

Real `TextPaint` on Robolectric (`TextPaintDayDrawingBudgetTest`, large60, window 411; bytes gated, times reported only):

```
DayDrawing metric: robolectric W=411 dayMax=0.02 total=1.1 bytesMax=11795
```

Pre-composite versus a `saveLayerAlpha` reference (`PrecompositedAlphaTest`, 17 types x marked/optional): without text the largest channel difference is 3/255; with text, 16/255 on glyph edges (OQ-A52). 1e re-sets the limits to about twice the measured means.
