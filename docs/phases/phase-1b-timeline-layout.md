# Phase 1b brief: Timeline layout, geometry and large fixtures

- **Branch:** `feat/phase-1b-timeline-layout`, from the stack top once 1a is in the stack (1a brings 0c and 0d with it; 0e must also be in the stack for `@TimingGate` and `DeviceProfiles`, see §4).
- **Depends on:** 1a. **Wave:** 4. **Milestone:** M1. **Consumers:** 1c (drawing), 1d (host page), 1e (benchmarks), 6b–6e (trip page).
- **Plan entry:** `plan.md` "### Phase 1b: Timeline layout, geometry and large fixtures" (l.1035–1088).
- **Re-verified:** 2026-11-16 (third pass, no subagents): `Timeline.swift` l.63–229, `TimelineGeometry.swift`, `TimelineAccessibilityGeometry.swift`, `TripPageState.swift`, `TripPageLayout.swift` and `PreparedPlan.swift` l.62–167 re-read at the pin; every rule quoted in Tasks 3, 4, 7 and 9 matches.
- **Checked:** 2026-11-16 (adversarial checker, no subagents): every Swift file, test and line range of §2.2, the PlanUI catalog (104 keys, 2 plurals, all nl values of §6 Task 6), `PlanMetrics` values, the seed literal, the boards of §2.3, `ApiTripDetail` and every [G] golden (an independent Python re-implementation of the large30/large60 recipe) re-checked. Fixed: test-helper reuse from 1a, the S1 `seekLight` assertion 1a left to 1b, the seed closing-delimiter line, a wrong edge-case width, 0e profile field names, task ordering (`TripPageLayout`, template S3 before Task 10), the labels checkbox moved to Task 7, the `PlanPreparer` default-parameter form, and stale line references.
- **Brief written:** 2026-11-16 by the orchestrator, from iOS read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`).
- **Tests:** follow AGENTS.md "Tests" for every test. This phase is **JVM only**: JUnit 4.13.2, assertk 0.28.1, kotlinx-coroutines-test 1.11.0. No Robolectric, no emulator, no Android API, no Compose.

Tags: **[F]** fact verified in source (path and line). **[R]** decision for Android adopted by this brief. **[G]** golden value computed on 2026-11-16 by a stdlib Python re-implementation of the iOS Swift code, cross-checked against every number iOS itself reports (54,000 pt, 740 rows, 20 zone rows, min widths 252/268, "Day 2 · Tue 8 Dec"). A second, independent re-implementation (2026-11-16, second pass) reproduced the hashes, the day and block counts, the per-lane counts, the zone-change and overnight-flight counts, the first and last day and block ids and instants, the caffeine source day and the zone set exactly. If your Kotlin disagrees with a [G] value, re-read the Swift first; do not edit the golden to match.

**Traversal order (scope note).** 1b owns the *order* of accessibility targets: `PreparedDay.blocks` (tiles + markers + zone changes, by start, then id; Task 7) and the frames (`timelineBlockFrame`, Task 4). 1c owns the `traversalIndex` *numbers* (blocks `i`, dividers `10000 + j`, ticks `20000 + k`; `phase-1c-timeline-drawing.md` l.381–385) and 1d the now node (−1).

---

## 1. Goal

Port the geometry half of iOS `PlanCore` and the pure trip-page math of iOS `PlanUI/TripPage` into the JVM module `:core:plan`, so that every later timeline phase consumes exact iOS geometry and prepared copy:

1. **Timeline layout** (`Timeline.swift`): `indexTimeline`, `timelineBlocksByDay`, `layoutDay`, `packLane`, `tickPositions`, `zoneChangePositions`, `nowLineOffset`, `layoutPlan`.
2. **Geometry** (`TimelineGeometry.swift`, `TimelineAccessibilityGeometry.swift`): lane width, tile frames, hit frames with a **48 dp** minimum (iOS 44 pt), marker and zone-change frames, horizontal overflow when `minWidth > available`.
3. **Labels:** `nowLabel` (the one `TimelineLabels.swift` function 1a left out, OQ-1a-1) and `TripHeaderText.swift` (`tripDatesText`, `tripMetaText`).
4. **Prepared model:** `PageCopy`, `preparePlan` → `PreparedPlan`/`PreparedDay`/`PreparedBlock` (copy, a11y labels in traversal order, progress, notices, zone instruction and comparison, end text), and `PlanPreparer` (the `layouts`/`prepare`/`nowTexts`/`evict` half of iOS `PlanDecoder`, on top of 1a's `PlanDecoder`) with per-input caching and **stable day identity**.
5. **Trip-page state math:** `TripPageState.resolve`, `openingDay`, `replacementAnchor`, `TripPageLayout` (wide list widths) and `TripWideListPosition`.
6. **Large fixtures:** `LargeTimelineFixtures` in `:core:testing` reproducing the iOS large30/large60 ids, hashes and labels.
7. **Budgets:** `TimelinePreparationBudgetTest`: decode + index/layout + prepare within S3 ≤ 40 ms, large30 ≤ 100 ms, large60 ≤ 200 ms on **every** one of 10 samples after three warm-ups, in the serial `make timing` tier (`@TimingGate`, 0e OQ-0e-22).

Nothing in this phase draws, measures text or touches Android.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md`: A3 (modules, Konsist rules), A10 (48 dp targets, 280 dp minimum width), A12 (timeline architecture), A19 (tiers), "Performance budget" row "Plan preparation", "Android spec: window policy" (trip page row), Phase 1b (l.1035–1088), 1c and 1d (what consumes your API) | Scope |
| Rules | `AGENTS.md` "Working rules", "Performance", "Tests", "Git"; `CLAUDE.md` | How to work |
| Research | `docs/research/04-timeline.md` §2.1–§2.13, §4.1, §4.8, §5, §7.3, §7.4, §10.1, §12, §14 (OQ-T1, T4, T5, T19); `docs/research/09-rn-android-lessons.md` §4.4–§4.5; `docs/research/10-copy-l10n.md` §3, OQ-L7 | Facts quoted below |
| Sibling briefs | `docs/phases/phase-1a-plan-core.md` §4–§6 and §10 (what 1a gives you), `docs/phases/phase-1c-timeline-drawing.md` §4 (the API 1c expects from you), `docs/phases/phase-1e-timeline-perf.md` (P8, the `Timeline host:` output line) | Contracts with neighbours |
| 1a code | `core/plan/src/main/kotlin/net/chronoflow/core/plan/` (`PlanTime.kt`, `CopyCatalog.kt`, `CopyLineFormatter.kt`, `TimelineLabels.kt`, `CheckState.kt`, `PlanLogic.kt`, `PlanDecoder.kt`, `PlanClock.kt`) and `core/model/…/{CheckOffChange.kt,store/}` | What you build on |
| 0d/0e code | `core/model/…/contracts/*.generated.kt`, `ApiJson`, `core/testing/src/main/resources/fixtures/**`, `fixtures.sha256`, `FixtureManifestTest`, `DeviceProfiles.kt`, `profiles.json`, the `@PerfBudget` and `@TimingGate` annotations | Types, fixtures, profiles |

### 2.2 iOS at `f391a1fa` (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

Paths are relative to `Packages/ChronoFlowKit/` unless they start with `plan.md` or `Docs/`.

| Path | Lines | Port to |
|---|---|---|
| `Sources/PlanCore/Timeline.swift` | 1–229 (all) | `Timeline.kt` |
| `Sources/PlanCore/TimelineGeometry.swift` | 1–46 (all) | `TimelineGeometry.kt` |
| `Sources/PlanCore/TimelineAccessibilityGeometry.swift` | 1–17 (all) | `TimelineAccessibilityGeometry.kt` |
| `Sources/PlanCore/TimelineLabels.swift` | 14–19 (`nowLabel` only; the rest is 1a) | `TimelineLabels.kt` (append) |
| `Sources/PlanCore/PlanMetrics.generated.swift` | 1–103 | Already emitted by 0b; Task 1 adds `minTouchTarget` only |
| `Sources/AppCore/Contracts/PlanContracts.swift` | 115–125 (`isSupported`, `renderableBlocks`) | `PlanLogic.kt` (append, if 1a/0d lack them) |
| `Sources/PlanUI/TripPage/PageCopy.swift` | 1–27 | `PageCopy.kt` |
| `Sources/PlanUI/TripPage/TripHeaderText.swift` | 1–30 | `TripHeaderText.kt` |
| `Sources/PlanUI/TripPage/PreparedPlan.swift` | 1–240 | `PreparedPlan.kt` |
| `Sources/PlanUI/Models/PlanDecoder.swift` | 29–44, 98–149 (`layouts`, `evict`, `prepare`, `nowTexts`) | `PlanPreparer.kt` |
| `Sources/PlanUI/TripPage/TripPageState.swift` | 1–66 | `trippage/TripPageState.kt` |
| `Sources/PlanUI/TripPage/TripPageLayout.swift` | 1–30 | `trippage/TripPageLayout.kt` |
| `Sources/DataAccess/Mock/MockTimelineFixtures.swift` | 1–135 | `core/testing/…/LargeTimelineFixtures.kt` |
| `Sources/DataAccess/Mock/MockTripFixtures.swift` | 5–13 (`Template`, `templates()`) | `core/testing/…/MockTripTemplates.kt` |
| `Sources/DataAccess/Mock/MockAppSeed+Fixtures.swift` | the JSON literal between `#"""` and `"""#` | `core/testing/src/main/resources/fixtures/mock-seed.json` |
| `Sources/PlanUI/Resources/Localizable.xcstrings` | all (104 keys, 2 plurals) | `core/plan/src/main/resources/page_copy.json` (generated, Task 6) |
| Tests: `Tests/PlanCoreTests/TimelineTests.swift`, `TimelineGeometryTests.swift`, `LabelTests.swift` (l.25–36, 140–143); `Tests/PlanUITests/LargeTimelineFixtureTests.swift`, `TimelinePerformanceTests.swift`, `TimelineAccessibilityTests.swift`, `TimelineRenderingTests.swift` (l.94–114 only), `TripPageTests.swift`, `TripPageLayoutTests.swift` (l.1–49; l.51–111 are `TripScrollRestoration`, out of scope), `TripPageCopyTests.swift` | as listed per task | ported tests, Swift names |
| `plan.md:1705` "15d: Timeline drawing"; `plan.md:1713` "15 performance extension (user request, worktree B)"; `plan.md:1730` "15 performance enforcement (worktree B, 2026-11-08)" | — | decisions quoted in §3.3 |
| `Docs/perf-timeline.md` | "Fixtures and direct launch", "Why the old end-to-end swipe test timed out", "Host benchmarks" | fixture recipe, height math, budget protocol |

### 2.3 Boards (look, do not build)

`_design/canvas/Plan.dc.html`, `PlanTimelineTop.dc.html` (a wrapper, l.18: `<dc-import name="Plan" from-top="{{yes}}" open-day="{{zero}}" …>` with `yes: true, zero: 0`, l.22), `PlanWide.dc.html`. No UI is built in this phase. Use them only to confirm the geometry you port:

- **[F]** Plan: day grid `grid-template-columns: 44px repeat(4, minmax(0, 1fr)); gap: 4px` and `y = (h − sh) × 40`, tile height `max(24, …)`. This is the same lane math as iOS: board lane width `(w − 44 − 4·4)/4 = (w − 60)/4` equals iOS `(w − 44)/4 − 4`.
- **[F]** PlanWide: `hourHeight: 52`, `y = (h − sh) × 52`; columns `grid-template-columns: minmax(0, 1fr) 280px; gap: 16px` (l.116, 121, 312); side gutter 20 (`margin: 0 -20px; padding: 0 20px`, l.312). **[R]** Hour scale stays **40 dp/h everywhere** (`04` OQ-T1, iOS parity); 280/320/16/20 come from `PlanMetrics` (`wideListWidth`, `wideLargeListWidth`, `wideColumnGap`, `wideGutter`).
- **[F]** PlanTimelineTop opens at the top of the opening day (`04` OQ-T4 default: iOS, top of the opening day, no animation). This is what `openingDay` feeds.

### 2.4 API

None is called. For context only, the shape you consume is `GET /v1/trips/{trip}` → `ApiTripDetail {trip, plan, notices, timeline, checkState, detailVersion}` (`04 §2.1`), already decoded by 1a's `PlanDecoder` into the 0d `TripDetail`.

---

## 3. Facts and decisions you must not get wrong

### 3.1 Quoted research

- `04 §2.3`: "Height = minutes × 40/60 pt … The day length is the real UTC duration, never 24 × 40. Days of 23 h and 25 h (DST) and 30 h or more (travel) occur. The large60 cycle is 24, 24, 13.5, 25.5, 24, 24 h → 960, 960, 540, 1,020, 960, 960 pt."
- `04 §2.4`: "Timeline lanes, in order: `[sleep, light, caffeine, travel]`. `info`-lane blocks become gutter markers. `zone_change` blocks become zone-change positions, not tiles." Optional steps "are never shown".
- `04 §2.12`: "If `minWidth > available`, the day scrolls horizontally. The fixture minimum lane widths are 252 and 268 pt against a 358 pt content width … A 320 dp phone (288 dp content) can overflow."
- `04 §2.13`: "Block order inside a day: placed tiles + markers + zone changes, sorted by `startUtc`, then `id`." Accessibility label = title, priority, interval, "Marked done"/"Skipped", "Continues from the previous plan day", "Continues into the next plan day", joined with ", ".
- `04 §5`: targets are "one invisible node each … bounds = hit frame with a **48 dp** minimum (OQ-T5)". Tests "assert targets ≥ 44 and in bounds at widths 288, 358 and 637" on iOS; Android uses 48.
- `04 §7.3`: host budgets "S3 40 ms (latest mean 19), large30 100 ms (50), large60 200 ms (92)", per sample, "Debug and Release alike".
- `04 §9.3`: "`PreparedDay` … Unchanged days **keep their instance** across revisions." "Marks are merged separately, so a check-off rebuilds only progress and marks …, never the parse." "Hidden categories and `timeFormat` re-run layout and labels, not the decode."
- `04 §12` and iOS `MockTimelineFixtures.swift`: the large-fixture recipe (§6 Task 10 below transcribes it from the Swift).
- `09 §4.4`: lane order, `PX_PER_HOUR` 40, `MIN_TILE_HEIGHT` 24, `TINY_TILE_HEIGHT` 56, `GUTTER_WIDTH` 44, `GUTTER_MARKER_SIZE` 24, `LANE_GAP` 4, `TILE_INSET` 2, `MIN_LANE_WIDTH` 44, `MIN_COLUMN_WIDTH` 30, `MIN_TICK_SPACING` 30. "The server supplies day bounds, zones and ticks. The client never computes days."

### 3.2 What 1a already decided (do not redo)

- `fun planDate(value: String): Instant?` in `PlanTime.kt`; `null` where iOS returns the NaN date. **[R]** Treat a `null` instant like NaN: every comparison with it is false, and an offset from it is `null`/skipped.
- `TimelineLabels.kt` already has `dayDateLabel`, `blockInterval`, `localDate`, `utcOffset` (OQ-1a-1). You add only `nowLabel`.
- `CheckOffChange` is in `:core:model`; `mergedCheckState(...)`, `checkIdentity`, `buildCheckState` are in `CheckState.kt`; `PlanDecoder` (suspend, `Mutex`, injected `worker` dispatcher, `detail(record)`, `detail(snapshot)`, `evict`, `decodeCount`) is in `PlanDecoder.kt`; `CopyCatalogProvider` loads `plan_copy.json` off-main.
- `ModelTests.versionAndRawPayloadInvalidateCache` was ported in 1a. Do not port it again.
- The Konsist ban on `Locale.getDefault`, `ZoneId.systemDefault`, `System.currentTimeMillis`, `Instant.now(`, `String.format(` etc. in `:core:plan` is live. Never format with `String.format`; build strings by hand.

### 3.3 iOS decisions quoted (`plan.md` at the pin)

- l.1714: "Add opt-in 30/60-day multi-zone stress fixtures built from repeated S3 travel/day structure plus S1 caffeine blocks."
- l.1715: host benchmarks of "full decode + `layoutPlan`/`indexTimeline` + the model's presentation preparation on S3 and both large trips, with per-sample budgets and fixture invariants. Bucket blocks once per plan … compare the bucketed results with full scans on every S1–S6 day and hidden-lane/reversed-input variants."
- l.1718: "Replacements preserve the existing day ID when its date still matches, otherwise the same label date or nearest date, and restore the relative scroll offset."
- l.1734: "Tighten monotonic per-sample decode/layout/preparation host budgets to S3 40 ms, 30 days 100 ms and 60 days 200 ms, unchanged between Debug and Release. `make quick` includes these tests."

### 3.4 Conventions for this phase [R]

- **Module and package:** `:core:plan`, package `net.chronoflow.core.plan` (trip-page math in `net.chronoflow.core.plan.trippage`, generated code in `net.chronoflow.core.plan.generated`). `core/plan/…/X.kt` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/X.kt`. Tests in `core/plan/src/test/kotlin/net/chronoflow/core/plan/`. Fixtures code in `core/testing/src/main/kotlin/net/chronoflow/core/testing/`.
- **Names:** Swift type, function, property and test-method names are kept (`blockID`, `dayID` stay as in Swift where they are Swift property names). Swift free functions → Kotlin top-level functions. Swift `inout` → return the new value.
- **Numbers:** every geometry value is a `Double` in dp (iOS `Double`/`CGFloat`). No `Float` in `:core:plan` geometry (tests compare with `1e-8`).
- **Time:** `Instant` from 1a's `planDate`. `offsetFrom(start, at) = secondsBetween(start, at) / 60 × PlanMetrics.pointsPerMinute`, where `secondsBetween = (at.epochSecond − start.epochSecond) + (at.nano − start.nano) / 1e9` as `Double` (sub-millisecond precision is tested). For speed, parse each block's and day's instants **once per `layoutPlan`/`preparePlan` call** (a local `HashMap<String, Instant?>` or precomputed arrays), not inside comparators.
- **Values:** every metric comes from `PlanMetrics` (generated). The only Android-only value is `PlanMetrics.minTouchTarget = 48.0` from the generator overlay (Task 1). No literal dp in geometry code. The traversal-index bases of 1c are not your concern.
- **Immutability:** all result types are Kotlin `data class`es with `List`/`Map` fields (iOS `Equatable` structs). Equality is structural, so `layoutPlan(...)[id] == layoutDay(...)` works.
- **Sorting:** Kotlin `sortedWith` is stable; iOS results rely on that (the `packLane` and marker cases below). Never sort on a `Float` key or with `compareBy` over parsed instants inside the comparator.
- **No state at class load:** no top-level `val` that does work (D26). Lazy loading of resources happens on the worker.
- **Threading:** `PlanPreparer` is called from a worker dispatcher; it never touches the main thread itself and contains no Android check (§6 Task 8).

---

## 4. Check what earlier phases gave you (before Task 1)

Run these on the stack top and list the results in the body of your first commit. If one fails, stop and report: the phase cannot start.

1. 1a: `core/plan/…/PlanTime.kt` has `planDate`; `TimelineLabels.kt` has `dayDateLabel`, `blockInterval`, `localDate`, `utcOffset`; `PlanLogic.kt` has `isOptionalStep`, `withoutOptionalSteps`, `noticeItems`, `dayZoneLabel`, `summaryIntervals`, `dayLabelKey`, `dayStatusLabelKey`; `CopyLineFormatter.kt` has `formatInstant`, `formatDate`, `formatWeekday`, `formatDayMonth`, `formatOffset`, `formatMinutes`, `zoneLabel`, `CopyFormat`; `CheckState.kt` has `checkIdentity`; `PlanDecoder.kt` and `CopyCatalogProvider` exist; `CheckOffChange` exists in `:core:model`.
2. 0d: contracts `TripDetail`, `TripDetailView`, `PlanView`, `DayView`, `DayZoneView`, `BlockView`, `TickView`, `TimelinePresentationView`, `CopyLine`, `Lane`, `BlockType`, `Priority`, `CheckStatus`, `EndState`, `AdaptationView`, `AssumedReturnBasis`, `TripCardView`, `Language`, `TimeFormat`; `ApiJson`; fixtures `plans/{S1,…,S6,S6-RETURN-10N}.json` and `trips/detail.json` with `fixtures.sha256`. Use 0d's enum spellings (`Lane.Sleep` vs `Lane.SLEEP`, the `Unknown(raw)` case) as generated.
3. 0e: `@PerfBudget`, `@TimingGate`, `DeviceProfiles.kt` loading `core/testing/src/main/resources/profiles.json` with `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` (width dp and insets). If 0e is not in the stack, stop: the budget and profile tests need it.
4. 0b: `core/plan/src/main/kotlin/net/chronoflow/core/plan/generated/PlanMetrics.generated.kt` (`object PlanMetrics`, package `net.chronoflow.core.plan.generated`) exists with every value of `PlanMetrics.generated.swift` (0b brief §6.7 and OQ-0b-1 put it there), and 0b's `MetricsGoldenTest` passes. Task 1 then only adds `minTouchTarget`. If the file is missing, stop and report: 0b is not in the stack. `TimelinePalette.generated.kt` is not used in this phase.

---

## 5. Out of scope (owner in brackets)

- Anything that draws, measures text or uses `TextMetrics`; `DayDrawing`, `DayDrawingCache`, `HitIndex`, `TimelineTarget` lists and their traversal-index values, divider pill frames [1c]. You provide `timelineBlockFrame` and the block order that 1c turns into targets.
- `LazyColumn`, `NowLine`, `SystemPlanClock`, `TimelineProbe`, the `cf.trip-page` perf entry [1d]. Macrobenchmarks, `docs/perf-timeline.md` structure [1e].
- `TripScrollRestoration` and the iOS 60-tick resize restoration (`TripPageLayoutTests.swift` l.51–111: `resizeRestoresTheSameDayOffsetAfterLateReflow`, `resizeUsesTheLiveContentOriginAndWaitsForContentSizeToSettle`, `resizeMeasuresTheAnchorBelowTheTopInset`, `resizeClampsToTheAvailableScrollRange`): iOS-only (`04 §0.3`); Android restores with `requestScrollToItem` [6e].
- Trip-page UI, day headers, compact header, list mode, wide layout UI, step sheet [6b–6e]. `TripListValue`, `PlanDecoder.trips/card/adaptation` [6a]. `Today.swift` [7].
- The default mock seed and `MockAppSeed.make` (`defaultMockHasNoPerformanceTrips`) [4c]. You only copy the seed JSON as a fixture (Task 2).
- Database rows and the snapshot merge [1a/4a/4d]. No API call.
- Any board without a geometry role here. No screen is built, so no capture is produced.

---

## 6. Tasks

Each task is one commit on `feat/phase-1b-timeline-layout`, Conventional Commits, lower-case imperative subject, **no trailers**. Tick the `plan.md` Phase 1b checkbox named in the task in the same commit. Run `make targeted ONLY=…` for what you touched in each fix round, and `make quick` once before handing back.

### Task 1. JVM `PlanMetrics` with the Android touch overlay

Commit: `build(tokens): add the 48 dp minimum touch target to plan metrics`. Ticks nothing.

**[F] What exists.** 0b already emits `core/plan/…/generated/PlanMetrics.generated.kt` from `Tools/TokenGen/Plan.source.json` at the pin: one `const val <name>: Double` for each of the **99** numeric `static var`s of `PlanMetrics.generated.swift:3–101`, plus `pointsPerMinute = pointsPerHour / 60` (l.102). 0b's `MetricsGoldenTest` asserts that every iOS value is equal and that "the Kotlin model has no extra names". The values this phase reads: `pointsPerHour 40`, `gutterWidth 44`, `laneGap 4`, `tileInset 2`, `minLaneWidth 44`, `minColumnWidth 30`, `minTickSpacing 30`, `minTileHeight 24`, `tinyTileHeight 56`, `gutterMarkerSize 24`, `touch 44`, `gutter 16`, `sectionGap 12`, `wideBreakpoint 600`, `wideLargeBreakpoint 900`, `wideListWidth 280`, `wideLargeListWidth 320`, `wideListMaxHeight 700`, `wideColumnGap 16`, `wideGutter 20`, `timelineLabelBreakpoint 640`, `compactHeaderHeight 36`.

**Files**
- `tools/tokengen/android-overlay.json` (new if absent): `{"planMetrics": {"minTouchTarget": {"value": 48, "replaces": "PlanMetrics.touch (44 pt) as the minimum hit target, 04 OQ-T5, plan A10"}}}`.
- The 0b `PlanMetrics` emitter (`:tools:tokengen`): after the iOS-derived values, append every `planMetrics` overlay entry as `const val minTouchTarget: Double = 48.0`, preceded by a one-line comment `// Android overlay: replaces PlanMetrics.touch (44 pt) as the minimum hit target, 04 OQ-T5, plan A10`. Regenerate and commit the output with the generator change.
- **`touch` stays 44.0** (iOS value, 0b golden). Nothing in Android geometry reads `touch`; every minimum target reads `minTouchTarget`.

**Tests**
- Amend 0b's `MetricsGoldenTest`: for `PlanMetrics`, the "no extra names" rule exempts exactly the names listed in `android-overlay.json` `planMetrics` (plus `pointsPerMinute`). Add `overlayValuesAreEmitted` (new): `PlanMetrics.minTouchTarget == 48.0`, `PlanMetrics.touch == 44.0`, and every overlay entry has a non-empty `replaces`.
- `make generators-check` regenerates and diffs `PlanMetrics.generated.kt`.

### Task 2. Mock seed templates in `:core:testing`

Commit: `test(testing): add the ios mock trip templates as a fixture`. Ticks nothing.

**Why [F]:** iOS large fixtures are built from `MockTripFixtures.templates()`, which decodes the JSON literal embedded in `Sources/DataAccess/Mock/MockAppSeed+Fixtures.swift` (695,566 B Swift file; `{"settings":…, "trips":[{card, detail}×7]}`), **not** from `plans/S3.json`. Template `[2]` is S3 (AMS→SYD, 18 days, 138 blocks) with a full `trip`, `checkState` and `detailVersion` `288dd5aa…`; template `[0]` is S1 (AMS→HND, 13 days, 139 blocks). Their `plan`, `notices` and `timeline` are equal to `plans/S3.json` and `plans/S1.json` (verified), but the `trip` and `card` parts exist only in the seed.

**Files**
- `tools/fixtures/extract_mock_seed.py` (stdlib, `python3 -I`, `--check`): reads the Swift file at `tools/ios-pin.txt` via `git -C $IOS_REPO show`, takes the lines strictly between the line ending in `#"""` (l.6) and the closing line, which is `            """#.utf8)` (l.16; match it as the first line whose `trimStart()` starts with `"""#`), strips the 12-space indentation of the closing delimiter from every line (Swift multi-line literal rule), validates it with `json.loads`, writes `core/testing/src/main/resources/fixtures/mock-seed.json` (UTF-8, LF, trailing newline).
- Add the file's line (pin, source path, SHA-256) to 0d's `fixtures.sha256`; `make generators-check` runs the script with `--check`.
- `core/testing/…/MockTripTemplates.kt`: `@Serializable data class Template(val card: TripCardView, val detail: TripDetail)`; `object MockTripTemplates { fun templates(): List<Template> }` decoding `trips` with `ApiJson` (unknown fields such as `settings` and `detail.revision` are ignored). Memoize per JVM with `by lazy` inside the object; no work at class load other than declaring the lazy.

**Tests** (`core/testing/src/test/kotlin/net/chronoflow/core/testing/`, JVM)
- `MockTripTemplatesTest`: 7 templates; card ids `0199a3c0-0000-7000-8000-00000000000{1…7}`; template 2 is AMS→SYD with 18 days and 138 blocks and `detailVersion` starting `288dd5aa`; template 0 has 13 days and 139 blocks; templates 0 and 2 `plan`/`notices`/`timeline` equal the decoded `plans/S1.json`/`plans/S3.json`.
- `FixtureManifestTest` (0d) now covers `mock-seed.json`.

### Task 3. `Timeline.kt`: bucketing, layout, packing, ticks, zones, now

Commit: `feat(plan): port timeline bucketing, day layout, lane packing, ticks and zones`. Ticks `plan.md` Phase 1b "**Timeline.kt.**".

**File:** `core/plan/…/Timeline.kt` (+ `val BlockView.isSupported` and `val PlanView.renderableBlocks` in `PlanLogic.kt` if absent: supported ⇔ `type` is not `Unknown` **and** `lane` is not `Unknown`, `PlanContracts.swift:115–125`).

**Types** (data classes, `Timeline.swift:4–61`): `BlockPresentation(reasons: List<CopyLine>, warningIndexes: List<Int>)`; `PlacedBlock(block, top, height, exactTop, tiny, col, cols, continuesBefore, continuesAfter, presentation)`; `PlacedMarker(block, exactTop, top, presentation)`; `ZoneChangePosition(block, top)`; `ZoneDivider(before: DayZoneView, after: DayZoneView, top)`; `TickPosition(tick: TickView, top, visible)`; `TimelineIndex(ticks: Map<String, List<TickView>>, blockReasons: Map<String, List<CopyLine>>, transferWarnings: Map<String, List<Int>>)`; `DayLayout(day, height, minWidth, placed: Map<Lane, List<PlacedBlock>>, markers, changes, dividers, ticks)` where `placed` always holds all four timeline lanes, in `timelineLanes` order (`LinkedHashMap`). `val timelineLanes: List<Lane> = listOf(sleep, light, caffeine, travel)` (l.63; a `get()` or a constant list, no work).

**Functions, exact rules** (`d0 = planDate(day.startUtc)`, `d1 = planDate(day.endUtc)`, `a`/`b` = block start/end):
1. `indexTimeline(timeline)` (l.65–72): three maps keyed by `dayId`/`blockId`; on a duplicate key the **last** entry wins.
2. `nowLineOffset(day, now: Instant): Double?` (l.78–81): `offsetFrom(d0, now)` if `d0 <= now < d1`, else `null` (also `null` when either day bound is unparseable).
3. `zoneChangePositions(day, blocks)` (l.83–89): blocks with `type == zone_change` and `d0 <= start < d1`, sorted by start (stable), `top = offsetFrom(d0, start)`.
4. `tickPositions(day, ticks)` (l.91–112): `inside` = ticks with `d0 <= atUtc < d1` (input order kept); `mandatory[i] = i == 0 || tz != previous.tz || offsetMin != previous.offsetMin` (previous = previous **inside** tick); greedy thinning with `minTickSpacing = 30`: a mandatory tick first pops every trailing **non-mandatory** visible tick closer than 30 (`positions[i] − positions[last] < 30 && !mandatory[last]`), then is appended; a non-mandatory tick is appended only if `positions[i] − positions[last] >= 30` (and never when nothing is visible yet). Every inside tick is returned, with `visible`.
5. `packLane(items: List<PlacedBlock>): List<PlacedBlock>` (l.114–134): stable sort by `top`, then `exactTop`; walk with `groupEnd = −∞`; when `top >= groupEnd`, flush (every member of the group gets `cols = columnEnds.size`); `col` = first index with `columnEnds[i] <= top`, else a new column; update that column's end to `top + height`; `groupEnd = max(groupEnd, top + height)`; flush at the end. Returns the sorted, packed list.
6. `layoutDay(day, blocks, presentation, hidden: Set<BlockType> = emptySet())` (l.136–187):
   - `height = offsetFrom(d0, d1)`.
   - For each block in input order: skip unless `isSupported && !isOptionalStep(block) && type !in hidden && type != zone_change`; skip if `a >= d1 || (b <= d0 && !(a == b && a >= d0))`.
   - `exactTop = offsetFrom(d0, max(d0, a))`; `presentation = BlockPresentation(blockReasons[id] ?: [], transferWarnings[id] ?: [])`.
   - `lane == info` → `PlacedMarker(top = exactTop + gutterMarkerSize/2)` (= +12).
   - Else `tileHeight = min(height, max(minTileHeight, offsetFrom(max(d0, a), min(d1, b))))`; `PlacedBlock(top = min(exactTop, height − tileHeight), height = tileHeight, tiny = tileHeight <= tinyTileHeight` (≤ 56, inclusive), `col = 0, cols = 1, continuesBefore = a < d0, continuesAfter = b > d1)`.
   - `packLane` per lane. Markers: stable sort by `exactTop`, then for `i ≥ 1`: `top[i] = max(top[i], top[i−1] + gutterMarkerSize)` (uses the **previous adjusted top**).
   - `minWidth = gutterWidth + 4 × laneGap + Σ_lanes max(minLaneWidth, cols_lane × minColumnWidth)`, `cols_lane = max(1, max cols in the lane)` (empty plan day = 236).
   - `changes = if (zone_change in hidden) [] else zoneChangePositions(day, blocks.filter { it.isSupported })`.
   - `dividers`: for `i in 1 until zones.size`, `at = planDate(zones[i].startUtc)`; if `d0 <= at < d1` → `ZoneDivider(zones[i−1], zones[i], offsetFrom(d0, at))`. Dividers are kept when `zone_change` is hidden.
   - `ticks = tickPositions(day, presentation.ticks[day.id] ?: [])`.
7. `layoutPlan(plan, timeline, hidden: List<BlockType> = emptyList()): Map<String, DayLayout>` (l.189–199): one `indexTimeline`, one `timelineBlocksByDay`, then `layoutDay` per day with `hidden.toSet()`.
8. `timelineBlocksByDay(plan): Map<String, List<BlockView>>` (l.201–229): one sweep. Order blocks by `(start, original index)`; iterate days sorted by start (stable); add every block with `start < dayEnd` from the cursor to `active`; remove from `active` blocks with `end <= dayStart && !(start == end && start >= dayStart)`; the day's list = `active.filter { start < dayEnd }` in **original plan index** order. **Do not** add the optional/hidden filtering here; `layoutDay` does it.

**Edge cases:** zero-length block at the day start is kept, at the day end is dropped; a block crossing both edges is clamped with both continuation flags; a day shorter than 24 dp makes every tile `top = 0, height = day height`. Unparseable instants (`planDate == null`) never crash (OQ-1b-6, **[R]**; iOS would produce NaN geometry):
- a block whose start or end is `null` is dropped in `timelineBlocksByDay`, `layoutDay` and `zoneChangePositions`;
- a tick whose `atUtc` is `null`, or a zone whose `startUtc` is `null`, is dropped;
- a day whose `startUtc` or `endUtc` is `null` gets an empty layout: `height = 0.0`, every lane empty (so `minWidth = 236.0`), no markers, changes, dividers or ticks; `nowLineOffset` returns `null`. In `timelineBlocksByDay` such a day maps to an empty list and does not move the cursor; it is sorted after every parseable day.

**Tests** (`core/plan/src/test/…`, JVM). Reuse 1a's `PlanCoreTestSupport.kt` (1a brief l.368, port of `TimelineTests.swift:7–37`): `PlanFixture`, `fixture(name = "S1")` from 0d's `plans/`, `at(minute)` (`at(0) == "2026-01-01T00:00:00.000Z"`), `sampleDay(hours = 24.0)`, `block(id, start, end)` = S1's first `sleep` block with new id and times. Do not re-create them. Add only `fun emptyIndex(): TimelineIndex = indexTimeline(TimelinePresentationView(days = emptyList(), blockReasons = emptyList(), transferWarnings = emptyList()))` (`TimelineTests.swift:39`) to that file. Line numbers below are `TimelineTests.swift` lines:
- `TimelineTests.dimensionsPackingAndTinyTiles`: metrics `pointsPerHour 40, minTileHeight 24, tinyTileHeight 56, gutterWidth 44, laneGap 4, tileInset 2`; lane order; blocks a(0,150) b(15,90) c(90,210) d(225,300) e(225,300) f(300,360) → `col [0,1,1,0,1,0]`, `cols [2,2,2,2,2,1]`, `result[2]` top 60, height 80, not tiny; the "short" day (before −60…20, short 25…26, end 1439…1441, instant 1439…1439) → tile 0 continuesBefore, height 24, cols 2, tiny; tile 1 col 1; tile 2 continuesAfter, `top + height == day height`, `exactTop > top`; tile 3 col 1; durations 0 / 84 / 84.01 min → tiny true / true / false; `minWidth` 236 empty, 282 for a(0,100) b(10,60) c(20,50).
- `TimelineTests.boundariesShortDaysAndLongDays`: only `start` survives of ends/before/start/end/after; a 10-minute day with a(−1,2) b(9,11) → height `20/3` (±1e-8), every tile `top 0`, full height, `cols 2`; 23/25/31 h days → height `hours × 40`, `nowLineOffset` at half day = `hours × 20`; now at minutes −1/0/60/1439/1440/1441 → null/0/40/1439·2/3/null/null; `nowLineOffset(day, start + 1 ms) ≈ pointsPerMinute / 60_000` (±1e-8). The iOS NaN-date case becomes: a day whose `startUtc` is `"garbage"` → `null`.
- `TimelineTests.markersZonesAndTickThinning`: the full Swift case (zones Amsterdam/Dubai/Tokyo at 0/90/180 min, changes at −2, 0, 90, 90, 270 → ids `change-1, change-2, change-3`; shown height 180, span tile height 140; dividers `[60, 120]`; hidden zone_change → no changes, same dividers; markers order `first, equal, close, later` and each ≥ previous + 24; hidden `note` → no markers; ticks `[-1,0,60,65 Tokyo 540,70 Tokyo 540,120 Tokyo 540,270]` → visible `[true,false,true,false,true]`; three close ticks with changing tz/offset → all visible; DST fold day `2026-10-25T00:00Z…2026-10-26T00:00Z` ticks at 0 and `01:00Z` → tops `[0, 40]`).
- `TimelineTests.emittedLanesPresentationAndEmptyPlan`: presentation reasons/warnings `[1, 0]` reach the tile; the tick at the day start has top 0; movement_break→travel, rest→sleep, caffeine_routine→caffeine, note→info marker; an empty plan → empty map; and the S1 assertion of l.191–196, which 1a left to 1b (1a brief l.334): `summaryIntervals(S1.days[2], S1.blocks, seek_light) == [DisplayInterval(planDate("2026-10-15T06:05:00Z"), planDate("2026-10-15T07:35:00Z"), "Asia/Tokyo")]`.
- `TimelineTests.everyFixturePreservesLayoutAndResolvesCopy`: the layout half, for S1–S6 and S6-RETURN-10N, hidden `[]`, `[sleep, note, zone_change]` and all types: one layout per day; height = UTC hours × 40; per lane the tile ids equal the brute-force filter of the Swift test (l.215–220: supported, not optional, not hidden, not `zone_change`, same lane, `start < dayEnd && (end > dayStart || (startUtc == endUtc string && start >= dayStart))`, compared as sorted id lists); tiles inside `[0, height + 1e-6]`, `0 <= col < cols`, continuation flags, reasons; overlapping tiles share `cols` and differ in `col`; ticks equal `tickPositions` of the day's ticks; the all-hidden layout has no tiles, markers or changes but the same dividers and ticks; `withoutOptionalSteps` is idempotent. (The copy half was ported by 1a.)
- `LabelTests.optionalRulesAndUnknowns`: append the two `layoutDay` assertions of `LabelTests.swift:25–36` (unknown type, then unknown lane → every lane empty) to 1a's test.
- `TimelineGeometryTests.bucketedLayoutMatchesFullScan` (here, because it tests bucketing): for every S fixture with days **and** blocks reversed, hidden `[]` and `[sleep, zone_change]`: `layoutPlan(...)[day.id] == layoutDay(day, plan.blocks, index, hidden)` for every day.

### Task 4. Geometry and accessibility frames

Commit: `feat(plan): port timeline geometry and accessibility frames with 48 dp targets`. Ticks `plan.md` Phase 1b "**TimelineGeometry.kt**".

**Files:** `core/plan/…/TimelineGeometry.kt`, `core/plan/…/TimelineAccessibilityGeometry.kt`.

**Behaviour**
- `data class TimelineRect(x, y, width, height)` with `minX = x`, `maxX = x + width`, `minY`, `maxY`, `midX`, `midY`, and `contains(px, py) = px >= minX && px < maxX && py >= minY && py < maxY` (CGRect semantics, half-open).
- `data class TimelineTileFrame(blockID: String, frame: TimelineRect, hitFrame: TimelineRect, tiny, continuesBefore, continuesAfter)`.
- `data class TimelineGeometry(width, height, laneWidth, tiles: List<TimelineTileFrame>)` with `companion operator fun invoke(layout: DayLayout, availableWidth: Double): TimelineGeometry` (so callers write `TimelineGeometry(layout, availableWidth)`, as 1c expects):
  - `width = max(layout.minWidth, availableWidth)`; `height = layout.height`; `laneWidth = (width − gutterWidth) / 4 − laneGap`.
  - Tiles in lane order (`timelineLanes`), then `layout.placed[lane]` order. For lane index `i`: `column = laneWidth / cols`; `x = gutterWidth + laneGap + i·(laneWidth + laneGap) + col·column + tileInset`; `frame = (x, top, max(0, column − 2·tileInset), height)`.
  - Hit frame **[R] Android delta:** `tw = max(minTouchTarget, frame.width)`, `th = max(minTouchTarget, frame.height)` (iOS uses `gutterWidth` = 44 here); `hx = max(0, min(x − (tw − frame.width)/2, width − tw))`; `hy = max(0, min(top − (th − frame.height)/2, height − th))`.
- `timelineBlockFrame(blockID, geometry, layout): TimelineRect?` (port, `TimelineAccessibilityGeometry.swift:3–17`), in this order:
  1. a tile with that id → its `hitFrame`;
  2. a marker → `(0, clampY(marker.top), max(gutterWidth, minTouchTarget) = 48, minTouchTarget)`;
  3. a zone change → `(gutterWidth + laneGap = 48, clampY(change.top), width − 48, minTouchTarget)`;
  4. else `null`.
  `clampY(t) = max(0, min(t − minTouchTarget/2, geometry.height − minTouchTarget))`. iOS uses `touch` (44) for both; Android uses 48 (OQ-T5).
- When `minWidth > availableWidth`, `geometry.width = minWidth`: the caller (1d) scrolls the day horizontally. Nothing else changes.

**Edge cases:** `availableWidth` below `minWidth` (e.g. 248 dp content on a 280 dp window); a day shorter than 48 dp (frames then start at 0 and exceed the height: allowed, iOS behaves the same; no real day is that short); a lane with `cols = 2` at minimum width (`minWidth = 44 + 16 + 60 + 3 × 44 = 252`, the fixtures' narrow case: `laneWidth = (252 − 44)/4 − 4 = 48`, column 24 dp → frame 20 dp wide → hit 48 dp wide, centred, clamped).

**Tests**
- `TimelineGeometryTests.allScenarioDaysFitDeviceWidths` (port, Android widths): for every S fixture day and every profile of `profiles.json`, `available = widthDp − insets.left − insets.right − 2 × PlanMetrics.gutter` (0e `DeviceProfile.widthDp: Int`, `insets: SafeInsetsDp` with `Float` fields; convert to `Double`); `geometry.height == layout.height`; `width >= available && width >= minWidth`; `laneWidth >= minColumnWidth`; every tile `frame.width > 0 && frame.height > 0`, `frame.minX >= gutterWidth`, `frame.maxX <= width`, `0 <= frame.minY`, `frame.maxY <= height + 0.001`; `hitFrame.width >= 48 && hitFrame.height >= 48`; `0 <= hitFrame.minX`, `hitFrame.maxX <= width`.
- `TimelineGeometryProfilesTest` (plan name; port of `TimelineRenderingTests.accessibilityTargetsFollowTilesAndGutterMarkers`, kept as that method name): S3 (from `LargeTimelineFixtures.fixture("S3")` once Task 10 lands; until then from `MockTripTemplates.templates()[2].detail`) prepared in **nl**, plus large60 in en once Task 10 lands, at content widths **288, 358, 637** and at every profile's `available` (formula above) and, for profiles ≥ 600 dp wide, also the wide timeline width `windowWidth − insets − 2 × wideGutter − wideColumnGap − listWidth` (until Task 9 lands, `listWidth = if (windowWidth >= PlanMetrics.wideLargeBreakpoint) PlanMetrics.wideLargeListWidth else PlanMetrics.wideListWidth`; switch to `TripPageLayout.resolve(windowWidth).listWidth!!` in Task 9's commit; foldInner 841 → 505 dp, landscapeSideNav 914 → 914 − 48 − 40 − 16 − 320 = 490 dp): for every `PreparedBlock` of every day, `timelineBlockFrame` is non-null, `height >= 48`, `width >= 48`, inside `[0, width] × [0, height]`, and equals the tile's `hitFrame` when the block is a tile. Until Task 7 lands, iterate `placed + markers + changes` of each `DayLayout` (no preparation, so no language) instead of `PreparedDay.blocks`; switch to `PreparedDay.blocks` in Task 7's commit. AGENTS "Tests" 5: JUnit `Parameterized`, one case per (fixture, width or profile); prepare each fixture once in a companion `lazy`, so each case stays < 1 s. The same applies to `allScenarioDaysFitDeviceWidths` (one case per profile).
- `TimelineGeometryTests.overflowWhenMinWidthExceedsAvailable` (new): the synthetic 24 h `sampleDay()` with sleep blocks a(0,100) b(10,60) c(20,50) (`minWidth 282`, as in `dimensionsPackingAndTinyTiles`) at available 248 → `width 282`, `laneWidth 55.5`; at 400 → `width 400`, `laneWidth 85`. Both hit-frame minimum and in-bounds rules hold at 248.

### Task 5. `nowLabel` and trip header text

Commit: `feat(plan): port now label and trip header date text`. Ticks nothing (the "**Labels and accessibility geometry.**" box also covers traversal order and the §2.13 accessibility labels, which land in Task 7; Task 7 ticks it).

**Files:** `core/plan/…/TimelineLabels.kt` (append), `core/plan/…/TripHeaderText.kt`.

**Behaviour**
- `CopyLineFormatter.nowLabel(layout: DayLayout, now: Instant, options: CopyFormat): String` (`TimelineLabels.swift:14–19`): zone = the first `layout.day.zones` entry with `start <= now < end`, else the last zone's `tz`, else `null`; return `formatInstant(<now as ISO string>, options, zone)`. Build the ISO string by hand or pass the `Instant` through an internal `formatInstant` overload; no `DateTimeFormatter` per call.
- `tripDatesText(start: String, end: String?, language: Language, long: Boolean = false): String` (`TripHeaderText.swift:5–22`): `s = start.take(10)`, `e = (end ?: start).take(10)`; `sameYear = s.take(4) == e.take(4)`, `sameMonth = s.take(7) == e.take(7)`; `date(v, first)` = day of month without padding, then `""` if `first && sameMonth`, else `" " + month` (long: full name; short: abbreviation), then `""` if `sameYear` else `" " + year`; `s == e` → `date(s)`; else `PageCopy(language)(if (long) "%@ to %@" else "%@–%@", date(s, first = true), date(e))`. The date is the calendar date of the 10-character string (noon UTC; no zone shift). Strip `.` (the tables have none).
  - **[R] Fixed tables** (A17, `10` OQ-L7; iOS strips dots and prints `Sep`): short months from 1a's tables (en `Jan … Dec`, `Sep` not `Sept`; nl `jan feb mrt apr mei jun jul aug sep okt nov dec`); long months **new**: en `January February March April May June July August September October November December`, nl `januari februari maart april mei juni juli augustus september oktober november december`. Put the long tables next to 1a's in `PlanCalendarText.kt`.
- `tripMetaText(start, end, flights: List<String>, language)` (l.24–30): `[tripDatesText(start, end, language)] + flights.map { it.trim() }.filter { it.isNotEmpty() }`, joined with `" · "`.

**Tests**
- `LabelTests.summaryClippingSplittingAndMerging`: append the `nowLabel` assertion (`LabelTests.swift:140–143`): the three-zone day, empty layout, now `2026-10-15T01:00Z`, en → `10:00`.
- `TripPageCopyTests.compactDateRangesAndOutboundFlightNumbers` (date half; the subtitle half lands in Task 7): `("2026-10-12","2026-10-18", en)` → `12–18 Oct`; same day → `12 Oct`; `("2026-10-12","2026-11-02")` → `12 Oct–2 Nov`; `("2026-12-30","2027-01-02")` → `30 Dec 2026–2 Jan 2027`; long en → `12 to 18 October`; long nl → `12 tot 18 oktober`. Add (new, `10` OQ-L7): `("2026-09-01","2026-09-03", en)` → `1–3 Sep`; long nl `("2026-03-01","2026-03-02")` → `1 tot 2 maart`.
- `TimelineLabelsNowTests` (new): a 25 h DST day: now inside the second zone uses that zone; now after the last zone's end uses the last zone; a day without zones formats in UTC (1a's `formatInstant(zone = null)` rule).

### Task 6. `PageCopy`: the PlanUI catalog for JVM preparation

Commit: `feat(l10n): generate the plan ui page copy table for jvm preparation`. Ticks nothing.

**Why [R]:** iOS `preparePlan` reads ~35 strings through `PageCopy(language:)`, which opens the PlanUI `Localizable` table of the `<lang>.lproj` bundle (`PageCopy.swift:8–26`). `:core:plan` is JVM and cannot read Android resources, and preparation runs off the main thread on the JVM in tests and budgets. The same catalog therefore also ships as a JVM runtime table, generated by the same `make strings` run that writes `strings_ios.xml`, exactly like `plan_copy.json` (OQ-1b-1).

**Files**
- `tools/l10n/xcstrings2android.py` (0c): one more output, `core/plan/src/main/resources/page_copy.json`, from `Packages/ChronoFlowKit/Sources/PlanUI/Resources/Localizable.xcstrings` at the pin. Shape: `{"source": "<path>", "pin": "f391a1fa", "entries": {"<key>": {"en": "<value>", "nl": "<value>"}, "%lld notes": {"en": {"one": "%lld note", "other": "%lld notes"}, "nl": {…}}}}`, keys sorted, UTF-8, LF, trailing newline. A missing `en` localization means `en = key`. A missing `nl` fails the generator. `--check` covers the file.
- `core/plan/…/PageCopy.kt`:
  - `class PageCopyTable(entries)` with `companion fun load(): PageCopyTable` reading `/page_copy.json` from the classpath (called on the worker, memoized by the caller; no static initialiser).
  - `class PageCopy(val language: Language, private val table: PageCopyTable)`: `operator fun invoke(key: String, vararg args: String): String` → value for `language` (`Language.Unknown` → en; missing key → the key itself, iOS fallback); if `args` is empty return the value; else replace each `%@` left to right with the next argument (no positional forms exist in this table; `%%` → `%`). `fun count(key: String, count: Int): String` → plural form `one` if `count == 1` else `other` (CLDR en/nl), `%lld` → the decimal count.
  - `equals` compares `language` only (iOS l.15).

**Copy keys used by this phase** (PlanUI `Localizable`, en = key) **[F]**:

| Key | nl |
|---|---|
| `Marked done` | Als gedaan gemarkeerd |
| `Skipped` | Overgeslagen |
| `Continues from the previous plan day` | Loopt door vanaf de vorige plandag |
| `Continues into the next plan day` | Loopt door in de volgende plandag |
| `From here on all times are %@ time. Set your watch to %@.` | Vanaf hier zijn alle tijden in de tijdzone van %@. Zet je horloge op %@. |
| `Sleep` / `Light` | Slapen / Licht |
| `%@ of %@ steps done` | %@ van %@ stappen gedaan |
| `%@ time from here` | Vanaf hier %@-tijd |
| `Important` / `Recommended` / `Optional` / `Information` | Belangrijk / Aanbevolen / Optioneel / Informatie |
| `Expected adapted` / `Likely adapted` / `Still adjusting` / `Partial shift` | Naar verwachting aangepast / Waarschijnlijk aangepast / Nog aan het verschuiven / Gedeeltelijke verschuiving |
| `Keep your usual rhythm` / `Return plan starts` / `Stay closer to home time` / `Holding this shift` | Behoud je gebruikelijke ritme / Terugreisplan begint / Blijf dichter bij je thuistijd / Deze verschuiving vasthouden |
| `Assumed trip end` / `Based on your check-out date` / `Based on %@ nights` | Aangenomen einde van de reis / Op basis van je uitcheckdatum / Op basis van %@ nachten |
| `There are no more steps in this plan.` | Er zijn geen stappen meer in dit plan. |
| `No clock shift planned` / `%@ hours` | Geen klokverschuiving gepland / %@ uur |
| `We advance your clock %@` / `We delay your clock %@` | We zetten je klok %@ vooruit / We zetten je klok %@ terug |
| `%@ to %@` / `%@–%@` | %@ tot %@ / %@–%@ |
| `Now, %@` (1c/1d) | Nu, %@ |

No new string is introduced: every key already exists in en and nl in the iOS catalog, and therefore in 0c's `strings_ios.xml`.

**Tests**
- `tools/l10n/test_xcstrings2android.py` (extend): `page_copy.json` has 104 entries, 2 of them plural (`%lld notes`, `%lld warnings`); every entry has en and nl; the `%@` count of en equals nl for every entry; `--check` fails after a planted edit.
- `TripPageCopyTests.warningAndNotePluralsFollowTheSelectedLanguage` (port, `TripPageCopyTests.swift:10–23`): for en (`warning`/`warnings`, `note`/`notes`) and nl (`waarschuwing`/`waarschuwingen`, `notitie`/`notities`): `count("%lld warnings", 1) == "1 " + singular`, `count("%lld notes", 1) == "1 " + singular`, and for counts 0, 2, 12 → `"<n> " + plural`.
- `PageCopyTests` (new, JVM; cases from `TripPageCopyTests.swift:41–47` and `TripPageTests.swift:116`): nl `Go to a day` → `Ga naar een dag`; nl `Home · %@`(Amsterdam) → `Thuis · Amsterdam`; nl `There · %@`(Tokyo) → `Daar · Tokyo`; nl `%@ · now`(`10:30 AM`) → `10:30 AM · nu`; nl `%@ to %@, %@, %@. Go to a day`(Amsterdam, Tokyo, Reisdag, `ma 12 okt`) → `Amsterdam naar Tokyo, Reisdag, ma 12 okt. Ga naar een dag`; `count("%lld warnings", 1)` en → `1 warning`, nl 2 → `2 waarschuwingen`; an unknown key with one arg in en → the key with `%@` replaced.

### Task 7. `PreparedPlan.kt`: the prepared model

Commit: `feat(plan): port the prepared plan model and its copy`. Ticks `plan.md` Phase 1b "**Labels and accessibility geometry.**" (labels from Task 5, frames from Task 4, traversal order and accessibility labels here). The "PlanPreparer.kt and PreparedPlan.kt" box is ticked by Task 8.

**File:** `core/plan/…/PreparedPlan.kt` (port of `PreparedPlan.swift:1–240`).

**Types** (data classes, Swift field names and order):
- `PreparedBlock(block, title, interval, priority, reason: String?, instructions, accessibilityLabel, status: CheckStatus?, continuesBefore, continuesAfter, zoneInstruction: String?, zoneComparison: String?)`, `val id get() = block.id`.
- `PreparedNotice(text, warning)`.
- `PreparedDay(day, title, date, weekday, dayMonth, zone, status: String?, summary, headline: String?, notices, warningCount, progress, blocks, dividerLabels, ticks)`, `val id get() = day.id`, `fun shownNotices(expanded, notesOpen) = if (expanded && (warningCount > 0 || notesOpen)) notices else emptyList()`.
- `PreparedPlan(revision, days, title, subtitle, spokenDates, shift: String?, summary, endText, assumedEnd: String?, returnDayID: String?)`.

**`fun preparePlan(detail: TripDetail, layouts: Map<String, DayLayout>, formatter: CopyLineFormatter, pageCopy: PageCopyTable, language: Language, timeFormat: TimeFormat = Value24, marks: List<CheckMark> = emptyList(), pendingChecks: List<CheckOffChange> = emptyList()): PreparedPlan`**. `copy = PageCopy(language, pageCopy)`, `options = CopyFormat(language, timeFormat, trip = detail.trip)`.
1. **Status index:** `checkIdentity(mark.key) → status` over `detail.checkState.marks + marks` (last wins); then each pending check sets its identity to its `status`, and a `null` status **removes** the entry (Swift dictionary assignment of `nil`).
2. **Days:** for each `plan.days` entry with a layout (skip days without one):
   - `placed` = tiles of all lanes in `timelineLanes` order (index them by block id once).
   - `blocks` = placed blocks + marker blocks + change blocks, sorted with this exact comparator: if the two `startUtc` **strings** are equal → `id` ascending (`String.compareTo`); else by parsed start instant. (Keep it: `compareBy(instant).thenBy(id)` differs when two different strings denote the same instant.)
   - Per block: `title = formatter.format(block.text, options)`; `interval = if (zone_change) formatInstant(startUtc, options, displayTz) else blockInterval(block, options)`; `priority = copy(priorityKey(priority))`; `status = if (actionable) checkKey?.let { index[checkIdentity(it)] } else null`; `before/after` from the placed tile (false for markers and changes); `accessibilityLabel = [title, priority, interval, status?.let { copy(if (it == Done) "Marked done" else "Skipped") }, before → copy("Continues from the previous plan day"), after → copy("Continues into the next plan day")].filterNotNull().joinToString(", ")` (an `Unknown` status reads "Skipped", as iOS); `reason = placed?.presentation?.reasons?.firstOrNull()?.let(format)`; `instructions = ([text] + secondaryText).map(format).joinToString(" ")`.
   - Zone-change texts, only when `block.params` is the zone-change params variant (Swift `if case .zoneChange(let params) = block.params`; selected by params, not by `type`; otherwise both fields are `null`): `from = secondaryTz ?: trip.from.zone`, `to = displayTz`, `destination = zoneLabel(to, trip)`; `zoneInstruction = copy("From here on all times are %@ time. Set your watch to %@.", destination, formatInstant(startUtc, options, to))`; `zoneComparison = "${zoneLabel(from, trip)} ${formatInstant(startUtc, options, from)} → $destination $interval · ${formatOffset(offsetMin.toDouble(), language)}"` (`→` U+2192, `·` U+00B7).
   - `notices = noticeItems(plan, detail.notices, day)` → `PreparedNotice(note.lines.map(format).joinToString(" "), warning)`; `warningCount` = warnings.
   - `summary` = for `[sleep, seek_light]`: `intervals = summaryIntervals(day, dayBlocks[day.id] ?: [], type)` with `dayBlocks = timelineBlocksByDay(withoutOptionalSteps(plan))`; if empty and `seek_light` and `day.noLightText != null` → `format(noLightText)`; else `copy("Sleep"|"Light") + " " + intervals.joinToString(", ") { formatInstant(start, tz) + "–" + formatInstant(end, tz) }`, where `start`/`end` are the interval `Instant`s passed as Swift `Date.ISO8601Format()` strings (`yyyy-MM-ddTHH:mm:ssZ`) or through an internal `Instant` overload of `formatInstant`, and `–` is U+2013 (note the **trailing space** when empty, iOS); the two parts joined with `" · "`.
   - `title = formatter.catalog.text(dayLabelKey(labelPhase) ?: "", language, mapOf("count" to (postArrivalIndex ?: 0).toString()), count = (postArrivalIndex ?: 0).toDouble())`; `date = dayDateLabel(day, language)`; `weekday = formatWeekday(labelDate, language)`; `dayMonth = formatDayMonth(labelDate, language)`; `zone = dayZoneLabel(day, trip)`; `status = day.status?.let(::dayStatusLabelKey)?.let { catalog.text(it, language) }`; `headline = day.headline?.let(format)`.
   - `progress = copy("%@ of %@ steps done", done, total)` over blocks that are `actionable && checkKey != null`, `done` = status `Done`.
   - `dividerLabels = layout.dividers.map { copy("%@ time from here", zoneLabel(it.after.tz, trip)) }`; `ticks = layout.ticks.map { formatInstant(it.tick.atUtc, options, it.tick.tz) }` (**all** ticks, index-aligned with `layout.ticks`, visible or not).
3. **End text** (l.156–179): `hasReturn = trip.legs.any { returnLeg }`; `endNote = plan.days.flatMap { notes }.lastOrNull { role == end }`; `endSummary = if (hasReturn) null else plan.summary`; `endLines = endNote?.lines ?: endSummary?.endCopy ?: []`; `end = listOfNotNull(endSummary?.expectedAdaptedText) + endLines.filter { key != "How this plan ends" }`; `stateKey` from `endNote?.endState ?: endSummary?.endState`: `adapted` → `"Expected adapted"` only when `endSummary?.expectedAdaptedText == null`, `likely_adapted` → `"Likely adapted"`, `still_adapting` → `"Still adjusting"`, `partial_target` → `"Partial shift"`, `small_shift` → `"Keep your usual rhythm"`, `return_plan_starts` → `"Return plan starts"`, `home_time` → `"Stay closer to home time"`, `hold` → `"Holding this shift"`, else none; `ending` = `end` de-duplicated (first occurrence of an equal `CopyLine` wins), formatted, joined `" "`; `endText = listOfNotNull(stateKey?.let(copy), ending.ifEmpty { null }).joinToString(" · ")`, or `copy("There are no more steps in this plan.")` when empty.
4. **Assumed end:** `zone = plan.days.lastOrNull()?.zones?.lastOrNull()?.tz ?: trip.to.zone`; when `!hasReturn` and `plan.meta.assumedReturn != null`: `[copy("Assumed trip end"), formatDate(leaveUtc, language, zone), formatInstant(leaveUtc, options, zone), zoneLabel(zone, trip), if (basis == check_out) copy("Based on your check-out date") else copy("Based on %@ nights", nights.toString())].joinToString(" · ")`.
5. **Plan fields:** `revision = plan.meta.inputHash + "|" + detail.detailVersion`; `title = "${from.city ?: from.iata} → ${to.city ?: to.iata}"`; `subtitle = tripMetaText(trip.departLocal, plan.days.lastOrNull()?.labelDate, trip.legs.filter { !returnLeg }.mapNotNull { flightNumber }, language)`; `spokenDates = tripDatesText(trip.departLocal, last labelDate, language, long = true)`; `shift = plan.summary?.let { clockShiftText(it.adaptation, formatter, options) }`; `summary = listOfNotNull(summary?.rationale, summary?.expectedAdaptedText).map(format).joinToString(" ")`; `returnDayID` (l.217–228): positions of return legs; the first plan block with flight params whose `legIndex` is a return position; the first day with `start <= flight.start < end`, else `null`.
6. `priorityKey(priority)`: core → `Important`, recommended → `Recommended`, optional → `Optional`, anything else → `Information`. `clockShiftText(adaptation, formatter, options)` (l.230–240): direction `none` → `No clock shift planned`; duration = `copy("%@ hours", (shiftMin / 60).toString())` when `shiftMin % 60 == 0`, else `formatMinutes(shiftMin.toDouble(), language, duration = true)`; `earlier` → `We advance your clock %@`, any other direction (including `Unknown`) → `We delay your clock %@`.

**Performance:** no `Regex`, `String.format` or `DateTimeFormatter` per block; parse each instant once; look placed tiles up by id through a map, not `first {}` per block.

**Tests**
- `TimelineAccessibilityTests.timeOrderLabelsOptionalFilteringAndContinuations` (port): `trips/detail.json`, en and nl: per day the block starts are sorted; no block is optional; each label contains its title, priority and interval, and the continuation phrase in that language when flagged.
- `TripPageCopyTests.compactDateRangesAndOutboundFlightNumbers` (subtitle half): S3 detail (`MockTripTemplates.templates()[2].detail` until Task 10, then `LargeTimelineFixtures.fixture("S3").detail`; `plans/S3.json` has no `trip`) with legs `" KL 861 "`, `""`, `"JL 1"` and a return leg `"RETURN"` → `subtitle` ends with `" · KL 861 · JL 1"` and does not contain `RETURN`.
- `TripPageCopyTests.preparationPreservesNoticeTones` (port): S3 (the same template detail), en and nl: tones equal `noticeItems`, `warningCount`, non-empty texts, the three `shownNotices` cases.
- `PreparedPlanTests` (new, Android): `priorityKey` table; `clockShiftText` for none / earlier 120 min (`We advance your clock 2 hours`) / later 90 min (formatMinutes duration text) and nl `We zetten je klok 2 uur vooruit`; end text: return trip ignores `plan.summary`; `adapted` with and without `expectedAdaptedText`; duplicate end lines appear once; `How this plan ends` is dropped; empty → `There are no more steps in this plan.`; assumed end for `check_out` and `nights = 10` (`Based on 10 nights`); a pending check with `status = null` removes a stored `Done` mark; an `Unknown` status labels "Skipped"; zone-change instruction and comparison strings on S3's first zone change (assert the exact en strings you compute from the rule, with `→` and `·`); the summary trailing space when a day has no sleep.
- Switch `TimelineGeometryProfilesTest` to iterate `PreparedDay.blocks` (Task 4 note).

### Task 8. `PlanPreparer`: caching and stable day identity

Commit: `feat(plan): add the plan preparer with per-input caches and stable days`. Ticks `plan.md` Phase 1b "**PlanPreparer.kt and PreparedPlan.kt.**".

**File:** `core/plan/…/PlanPreparer.kt` (port of `PlanDecoder.swift:29–44, 98–149`; Android name from plan.md).

**Behaviour**
- `class PlanPreparer(private val decoder: PlanDecoder, private val catalogs: CopyCatalogProvider, private val worker: CoroutineDispatcher, private val pageCopy: () -> PageCopyTable = { PageCopyTable.load() })` (a lambda, not a callable reference to the companion function; the defaulted parameter last). State behind one `Mutex`; all work inside `withContext(worker)` (production `Dispatchers.Default`; 1d passes it). The formatter and page-copy table are created lazily on the first call, on the worker.
- `suspend fun layouts(detail: TripDetail, hidden: List<BlockType>): Map<String, DayLayout>`: key `(trip.id, plan.meta.inputHash, detailVersion)`; a hit requires the cached `plan == detail.plan` (check `===` first), `timeline ==`, `hidden ==`. Miss → `layoutPlan(withoutOptionalSteps(detail.plan), detail.timeline, hidden)`, drop all entries of that trip, store, `layoutCount++`.
- `suspend fun prepare(detail, hidden, language, timeFormat, marks: List<CheckMark>, pendingChecks: List<CheckOffChange> = emptyList()): PreparedPlan`: key as above; a hit requires equal `detail`, `language`, `timeFormat`, `marks`, `hidden`, `pendingChecks` → return the cached `PreparedPlan` instance. Miss → `preparePlan(detail, layouts(detail, hidden), …)`, then **stable identity** (below), drop that trip's entries, store.
- **Stable day identity [R]** (`04 §9.3`, Android addition): keep, per trip, the last `Map<dayId, PreparedDay>`. After `preparePlan`, replace every new day that is `==` (structural) to the previous day with the same id by **the previous instance**, and keep the list order of the new plan. If no day changed and the other `PreparedPlan` fields are equal, return the previous `PreparedPlan` instance. A check-off therefore yields new instances only for the days whose progress or block statuses changed; a language or time-format change yields new instances for all days. Add no `revision` field to `PreparedDay` (1c guards its cache with `PreparedDay` equality, OQ-1c-10).
- `suspend fun nowTexts(layouts: Map<String, DayLayout>, now: Instant, language, timeFormat): Map<String, String>` (l.142–149): only days with `nowLineOffset != null`, value `nowLabel(layout, now, CopyFormat(language, timeFormat))`.
- `suspend fun evict(tripID: String)`: drops layout, prepared and identity entries of that trip and calls `decoder.evict(tripID)`.
- `val layoutCount: Int`, `val prepareCount: Int` (test visibility, as iOS).
- Decoding stays in 1a's `PlanDecoder`; `PlanPreparer` never decodes.

**Tests** (`runTest(timeout = 60.seconds)`, worker = `StandardTestDispatcher(testScheduler)` or a single-thread executor; never `Thread.sleep`)
- `TripPageTests.pendingOldHashMarksAndUndoOverrideTheNewPlan` (port): `trips/detail.json`, first actionable non-optional block with a `checkKey` marked `Done` in `checkState`; a pending `Skipped` under hash `previous-hash` → status `Skipped`; adding the undo (`status = null`) → status `null`; `layoutCount == 1`; `detailVersion += "-replacement"` → `layoutCount == 2`.
- `TripPageTests.preparationCachesLayoutsAndPreservesMarkLabels` (port): second identical `prepare` → `layoutCount == 1`; `prepare(nl, Value12, marks = [Done])` → still 1 layout, same day count, the block's status `Done`; `PageCopy(nl)("Go to a day") == "Ga naar een dag"`; after `evict` the next `prepare` → `layoutCount == 2`.
- `StableDayIdentityTests` (new): large60 (Task 10; until then S3 from the templates) prepared twice with equal inputs → the same `PreparedPlan` instance; then with one extra `Done` mark on an actionable block of day k (pick one whose `checkKey` identity is shared by no other block of the plan and that is not a continuation tile, so exactly one `PreparedDay` contains it) → `days[i] === previous.days[i]` for every `i != k`, `days[k] !== previous.days[k]`, and `days[k].progress` changed; then with `language = nl` → no day is the same instance; then with a new `detailVersion` but identical content → every day is the same instance, `revision` differs; `hidden = [sleep]` → `layoutCount` increments and days with sleep tiles are new instances.
- `PlanPreparerTests.nowTextsOnlyForDaysContainingNow` (new): S3 (template 2 detail), now = `planDate(days[2].startUtc)` + 1 h, inside day index 2 → exactly one entry, keyed by `days[2].id`, equal to `nowLabel(layouts[days[2].id], now, CopyFormat(En, Value24))`; now before the plan → empty map.

### Task 9. Trip-page state and layout math

Commit: `feat(plan): port trip page state, opening day, replacement anchor and wide layout`. Ticks `plan.md` Phase 1b "**Trip-page state**".

**Files:** `core/plan/…/trippage/TripPageState.kt`, `core/plan/…/trippage/TripPageLayout.kt`.

**Behaviour**
- `enum class TripPageState { Loading, RemoteLoading, RemoteOffline, Ready, NoPlan, NotFound, Server }` with `companion fun resolve(keepOffline, stored, noPlan = false, online, fetch: TripPageFetch, missingAfterSync = false, tombstoned = false, awaitingSync = false)` (`TripPageState.swift:8–23`), checks **in this order**: `tombstoned` → NotFound; `stored` → `noPlan ? NoPlan : Ready`; `missingAfterSync && !awaitingSync` → NotFound; `keepOffline` → Loading; `!online || fetch == Offline` → RemoteOffline; `fetch == NotFound` → NotFound; `fetch == Failed` → Server; else RemoteLoading.
- `enum class TripPageFetch { Idle, Loading, Loaded, Offline, NotFound, Failed }`.
- `data class TripPageAnchor(val dayID: String? = null, val labelDate: String? = null, val offset: Double = 0.0)`.
- `openingDay(days: List<DayView>, blocks: List<BlockView>, requestedDay: String?, blockID: String?, keptDay: String?, now: Instant): DayView?` (l.39–54), in order: the day with `id == requestedDay` (only when non-null); the day containing the start of the block with `id == blockID` (`start <= blockStart < end`; only when `blockID` is non-null and the block exists); the day with `id == keptDay` (only when non-null; a `null` id never matches, Swift `String == String?`); the day containing `now`; the last day if `now >= last.end`; the first day; `null` for no days.
- `replacementAnchor(anchor, days): TripPageAnchor` (l.56–66): if `anchor.labelDate != null`, find the day with `id == anchor.dayID && labelDate == date`, else the first with `labelDate == date`, else the day minimising `|noon(labelDate) − noon(date)|` (noon = `planDate(x + "T12:00:00Z")`, the first minimum wins) → `TripPageAnchor(day.id, day.labelDate, anchor.offset)`. Otherwise (no labelDate, or no days) → `TripPageAnchor(days.firstOrNull()?.id, days.firstOrNull()?.labelDate)` with offset 0.
- `data class TripPageLayout(val listWidth: Double?)`, `isWide = listWidth != null`, `gutter = if (isWide) wideGutter else gutter` (20 / 16), `companion fun resolve(width: Double)`: `< wideBreakpoint (600)` → `null`; `>= wideLargeBreakpoint (900)` → `wideLargeListWidth (320)`; else `wideListWidth (280)`. Width only, never height or orientation.
- `data class TripWideListPosition(offset, height)` with `companion operator fun invoke(dayTop, dayHeight, viewportHeight, stickyTop, contentHeight)`: `offset = min(max(0, stickyTop − dayTop), max(0, dayHeight))`; `height = max(0, min(dayHeight − offset, viewportHeight − stickyTop, wideListMaxHeight (700), contentHeight))`. Public (iOS internal) because 6e uses it from `:feature:trip`.

**Tests**
- `TripPageTests.stateGatingTable` (port, the full 2 × 2 × 2 × 2 × 6 table and the four extra cases).
- `TripPageTests.openingOrderAndReplacement` (port, `trips/detail.json`, now = day 2 start: requested beats kept; block day; kept; now's day; after the end → last day; before the start → first day; renamed day id keeps offset 180; duplicate label date prefers the same id; only the last day left → last day).
- `TripPageLayoutTests.breakpoints` (599/600/899/900/1200), `phoneWidthsStayPhone` (320, 375, 390, 393, 402, 420, 430, 440, 466), `unfoldedDuoIsWideInBothOrientations` (669 → 280, 951 → 320), `listSticksBelowTheWholeHeaderAndShrinksAtTheEndOfItsDay` (offset 446, height 314), `shortListStaysBesideItsDayAndTallListScrollsWithinTheViewport` (0/220 and 180/570), `listDisappearsWhenItsDayPassesTheHeader` (760/0). Add Android widths 280, 360, 411 (phone) and 841, 914 (wide, 280 and 320).
- `TripPageStateTests.replacementWithoutLabelDateFallsBackToFirstDay` (new): anchor without labelDate → first day, offset 0; empty days → `(null, null, 0)`.
- Switch `TimelineGeometryProfilesTest`'s wide width to `TripPageLayout.resolve(windowWidth).listWidth` (Task 4 note).

### Task 10. `LargeTimelineFixtures`: large30 and large60

Commit: `test(testing): port the large30 and large60 timeline fixtures`. Ticks `plan.md` Phase 1b "**Large fixtures:**".

**File:** `core/testing/…/LargeTimelineFixtures.kt` with `object LargeTimelineFixtures` (iOS `MockTimelineFixtures`; the name follows plan.md and the 1c/1e briefs, OQ-1b-5).
- `data class Fixture(val card: TripCardView, val detail: TripDetail)`.
- `fun fixture(named: String): Fixture`: `"large30"`/`"large60"` → generated; `"S<n>"` → `templates[n − 1]` (S1 … S7, 7 templates); otherwise the template whose `card.id == named`; else throw `IllegalArgumentException` (iOS throws `DataFailure.server(404)`).
- `val large30: Fixture by lazy`, `val large60: Fixture by lazy` (memoized per JVM), `fun detailBytes(named): ByteArray` = `ApiJson.encodeToString(TripDetail.serializer(), fixture.detail)` bytes, memoized (1d's perf entry and the budget test decode these).

**Recipe** (port of `MockTimelineFixtures.swift:24–134`, line by line):
1. `source = templates[2].detail` (S3); `cycle = source.plan.days.take(6)`; `start = instant(cycle[0].startUtc)`, `end = instant(cycle.last().endUtc)`, `duration = end − start` (135 h).
2. `sourceBlocks = source.plan.blocks.filter { start(b) < end && end(b) >= start }` (note `>=`); `sourceDayIDs`, `sourceBlockIDs` = their id sets.
3. `result = source` with `plan.days`, `plan.blocks`, `timeline.days`, `timeline.blockReasons`, `timeline.transferWarnings`, `notices.days`, `checkState.days`, `checkState.marks` emptied. Every other field (trip, plan meta/summary/warnings/prompts, other notices fields) stays as in S3, **unshifted**.
4. For `index in 0 until count / 6`: `suffix = "-tour-$index"`, `shift = duration × index`; `clone = shifted(source, shift, suffix, sourceDayIDs, sourceBlockIDs)`; append clone days whose id without the suffix is in `sourceDayIDs`; append clone blocks whose id without the suffix is in `sourceBlockIDs`, each with `startUtc = iso(max(instant(startUtc), start + shift))`, `endUtc = iso(min(instant(endUtc), end + shift))`; append clone `timeline.days` (by `dayId`), `blockReasons` and `transferWarnings` (by `blockId`) and `notices.days` (by `dayId`) the same way.
5. Caffeine overlay from `caffeinePlan = templates[0].detail.plan` (S1): `caffeineDay` = the first day with `phase == post` and at least 3 `lane == caffeine` blocks with `day.start <= block.start < day.end`; `caffeine` = those blocks. For `(index, day)` in `result.plan.days` (indexed from 0): `shift = day.start − caffeineDay.start`; `blocks = shifted(caffeine, shift, "-caffeine-day-$index", dayIDs = ∅, blockIDs = caffeine ids)`; each with `startUtc = iso(min(start, day.end))`, `endUtc = iso(min(end, day.end))`, `displayTz = day.zones.first().tz`; append to `result.plan.blocks`.
6. `id = "0199a3c0-0000-7000-8000-0000000000$count"`; `hash = sha256Hex("timeline-$count-S3-prefix6-S1-caffeine-v2")` (UTF-8, lower-case hex); `trip.id = id`, `trip.endUtc = last day endUtc`, `plan.meta.inputHash = hash`, `checkState.planInputHash = hash`, `detailVersion = hash`, `plan.meta.assumedReturn = null`; `card = templates[2].card` with `id`.
- `shifted(value, seconds, suffix, dayIDs, blockIDs)` (l.110–129): encode the value with `ApiJson` to a `JsonElement` (iOS encodes its decoded model), transform recursively, decode back with `ApiJson`. For every JSON **string** (object keys untouched): if it is in `dayIDs` or `blockIDs` → `string + suffix` (anywhere in the tree, e.g. `dayId`, `blockId`); else if `length >= 17 && 'T' in it && it.endsWith("Z")` and `parse(it)` succeeds → `iso(parse + seconds)`; else if `length == 10` and `parse(it + "T12:00:00Z")` succeeds → `iso(parse + seconds).take(10)`; else unchanged. Numbers, booleans and nulls are untouched.
- `parse(s)` (l.130–133, `ISO8601DateFormatter` defaults): if `s.length == 17`, replace `Z` with `:00Z`; then accept only `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$` → `Instant`; else `null`. `iso(i)` = `yyyy-MM-ddTHH:mm:ssZ` (`2026-10-13T05:00:00Z`, always with seconds, no fraction; `DateTimeFormatter.ISO_INSTANT` prints exactly this for whole seconds). **[F]** Every instant in S1/S3 is the 17-character `…THH:MMZ` form, so every cloned or clamped instant becomes the 20-character form, also for tour 0.
- Use `java.security.MessageDigest` for SHA-256. No randomness, no clock, no default locale.

**Goldens [G]** (assert all of them in `LargeTimelineFixtureTests.idsHashesAndLabelsMatchIos`):

| | large30 | large60 |
|---|---|---|
| trip id | `0199a3c0-0000-7000-8000-000000000030` | `0199a3c0-0000-7000-8000-000000000060` |
| hash = inputHash = planInputHash = detailVersion | `1cc3eb625fba1af74dcdd6beefc9e777716d9e0a541323caa1ca498c75b8c0af` | `0de45f266582126384ac3cd1cca4e776b9e569acbbe92bdc1580565ceae374c5` |
| days / blocks (all unique) | 30 / 490 | 60 / 980 |
| blocks per lane sleep / light / caffeine / travel | 35 / 210 / 210 / 35 | 70 / 420 / 420 / 70 |
| `zone_change` blocks / overnight flights | 10 / 5 | 20 / 10 |
| first day | `day-29864460-tour-0`, `2026-10-13T05:00:00Z` → `2026-10-14T05:00:00Z`, labelDate `2026-10-13`, `pre_departure` | same |
| last day | `day-29871120-tour-4`, `2026-11-09T08:00:00Z` → `2026-11-10T08:00:00Z`, labelDate `2026-11-10`, `day`, postArrivalIndex 2 | `day-29871120-tour-9`, `2026-12-07T11:00:00Z` → `2026-12-08T11:00:00Z`, labelDate `2026-12-08`, `day`, postArrivalIndex 2 |
| `trip.endUtc` | `2026-11-10T08:00:00Z` | `2026-12-08T11:00:00Z` |
| last block | `caffeine-no_caffeine-29867390-caffeine-day-29` | `caffeine-no_caffeine-29867390-caffeine-day-59`, `2026-12-07T18:35:00Z` → `2026-12-08T03:15:00Z` |
| first two blocks | `sleep-sleep-29865420-tour-0`, `light-avoid_light-29865900-tour-0` | same |
| caffeine source day / blocks per day | `day-29866935` / 7 | same |
| zones (distinct tz) | `Asia/Singapore`, `Australia/Sydney`, `Europe/Amsterdam` | same |
| day heights (dp) | the cycle `960, 960, 540, 1020, 960, 960` × 5 = 27,000 | × 10 = **54,000** |
| prepared rows (Σ `PreparedDay.blocks`) / zone rows | 370 / 10 | **740** / **20** (720 ordinary) |
| distinct `minWidth` | `{252, 268}` | `{252, 268}` |
| last label `title + " · " + date`, en | `Day 2 · Tue 10 Nov` | **`Day 2 · Tue 8 Dec`** (iOS test string, OQ-T19) |

**Tests** (`core/plan/src/test/…/LargeTimelineFixtureTests.kt`; it needs the catalog and the preparer, so it lives in `:core:plan` tests)
- `generatedTripsStayDenseAndResolveExistingCopy` (port, l.10–43): for 30 and 60: day count; unique day and block ids; ≥ 3 zones; ≥ count/6 zone changes; sleep, light and caffeine lanes each ≥ count blocks; an overnight flight exists; `a.endUtc == b.startUtc` for consecutive days; `start <= end` for every block; every `text.key` and `secondaryText.key` is in the `CopyCatalog`; `preparePlan` in en and nl gives `count` days, none with empty `blocks`.
- `sixtyDayGridHeightMatchesTheRepeatedUTCSpans` (port, l.45–61): heights equal the cycle × 10 and sum to 54,000; last label `Day 2 · Tue 8 Dec`; print the iOS summary line (grids, gaps `62 × sectionGap = 744`, warning headers, list rows, zone rows, widths) to stdout.
- `idsHashesAndLabelsMatchIos` (new): every [G] row above.
- `templatesAreUnchanged` (new): generating large60 does not mutate `MockTripTemplates.templates()` (S3 still 18 days).
- Budget: generation must be linear. If the first test to build a fixture exceeds the 1 s JVM limit only because of class loading, build both fixtures in a shared `@BeforeClass`/companion lazy; if the tier listener still charges it to a test, annotate only `LargeTimelineFixtureTests` with `@PerfBudget` and state the measured time in the commit body.

Then switch `TimelineGeometryProfilesTest` and `StableDayIdentityTests` to the fixture (`fixture("S3")`, `large60`) in this commit.

### Task 11. Preparation budget test and the JVM record

Commit: `test(plan): enforce timeline preparation budgets on the jvm`. Ticks `plan.md` Phase 1b "**Budget and geometry tests.**".

**File:** `core/plan/src/test/…/TimelinePreparationBudgetTest.kt`, annotated `@TimingGate` (0e): it runs only in the serial `timingTest` task (`make timing`). In the same commit, set `TIMING_MODULES := core:plan` in the Makefile. Methods keep the Swift names: `testS3DecodeLayoutAndPreparation` (40 ms), `test30DayDecodeLayoutAndPreparation` (100 ms), `test60DayDecodeLayoutAndPreparation` (200 ms).

**Behaviour** (port of `TimelinePerformanceTests.swift`)
- Outside timing: `bytes = LargeTimelineFixtures.detailBytes(name)` (`"S3"` = template 2); load the `CopyCatalog`, the `CopyLineFormatter` and the `PageCopyTable` once.
- One **sample** = `t0 = System.nanoTime()`; decode `bytes` to `TripDetail` with `ApiJson` directly (cold: no `PlanDecoder` cache); `layouts = layoutPlan(detail.plan, detail.timeline)`; `prepared = preparePlan(detail, layouts, formatter, pageCopy, Language.En)`; `sink = Σ days.blocks.size`; `elapsed = System.nanoTime() − t0`. Assert `sink > 0`.
- Run 3 warm-up samples (not asserted), then 10 samples; call `System.gc()` between samples, **outside** the timed region. Assert **every** sample `< budget` (not the mean) and report all failures at once.
- Print one line per fixture, exactly: `Timeline host: <S3|large30|large60> mean=<ms, 1 decimal> max=<ms, 1 decimal> samples=[…]` (1e parses `mean`/`max`).
- The same limits apply to every build type; never loosen a limit to make a run pass (iOS rule, `Docs/perf-timeline.md` "Host benchmarks").

**Record:** in `docs/perf-timeline.md` (create it if absent with the heading `# Timeline performance` and a section `## JVM budgets (1b, 1c), in quick`; 1e restructures the file later), add a table row per fixture: date, commit, `java -version` vendor and version, `uname -m`, mean and max of the 10 samples from one `make timing` run on an otherwise idle Mac, next to the iOS means (S3 19, large30 50, large60 92 ms). Write "host process, not a device claim".

---

## 7. Acceptance

1. `make targeted ONLY=":tools:tokengen:MetricsGoldenTest :core:testing:MockTripTemplatesTest :core:plan:TimelineTests :core:plan:TimelineGeometryTests :core:plan:TimelineGeometryProfilesTest :core:plan:LabelTests :core:plan:TimelineLabelsNowTests :core:plan:TripPageCopyTests :core:plan:TimelineAccessibilityTests :core:plan:PreparedPlanTests :core:plan:PageCopyTests :core:plan:TripPageTests :core:plan:StableDayIdentityTests :core:plan:PlanPreparerTests :core:plan:TripPageLayoutTests :core:plan:TripPageStateTests :core:plan:LargeTimelineFixtureTests :core:plan:TimelinePreparationBudgetTest"` passes.
2. `make quick` passes (Spotless, Konsist, every JVM test of `:core:*` and `:tools:*`, the tooling tests including `tools/l10n` and `tools/fixtures`) and stays under 60 s warm. Report the measured time.
3. `make generators-check` passes: `PlanMetrics.generated.kt`, `page_copy.json` and `mock-seed.json` regenerate byte-identically.
4. Budgets: every one of the 10 samples is within S3 ≤ 40 ms, large30 ≤ 100 ms, large60 ≤ 200 ms, in three consecutive `make timing` runs; the numbers are in `docs/perf-timeline.md`.
5. The large fixtures meet every invariant and every [G] golden (ids, hashes, labels, counts, 54,000 dp, 740 rows).
6. Every block, marker and zone-change frame is ≥ 48 × 48 dp and inside its day at content widths 288, 358, 637 and at every profile width.
7. Konsist: `:core:plan` and `:core:testing` import no `android.*`, `androidx.*` or Compose; no banned time/locale API (1a rule).
8. `make check` passes on the phase branch (orchestrator runs it with `pixel` booted if the sandbox cannot). No emulator test is added; no screenshot is produced (no UI). `make perf-timeline` does not exist yet (Phase 1e adds it and prints so); do not try to run it.
9. Each `plan.md` Phase 1b checkbox is ticked in the commit named above; commits are atomic Conventional Commits without trailers.

---

## 8. Review checklist (Opus reviewer)

Port fidelity (diff each function against the Swift line by line):
- [ ] `layoutDay` skip rule is exactly `a >= d1 || (b <= d0 && !(a == b && a >= d0))`; zero-length at start kept, at end dropped (`boundariesShortDaysAndLongDays`).
- [ ] Marker `top = exactTop + 12`, sorted by `exactTop` (stable), de-overlapped against the **previous adjusted top** + 24.
- [ ] Tile `height = min(dayHeight, max(24, clipped duration))`, `top = min(exactTop, dayHeight − height)`, `tiny` is `<= 56` (inclusive), continuation flags from the unclipped instants.
- [ ] `packLane` sorts by `(top, exactTop)` stably, starts a group at `top >= groupEnd`, assigns `cols` only on flush, picks the first column with `end <= top`.
- [ ] `minWidth` uses `4 × laneGap` (16) and `max(44, cols × 30)` per lane; empty day = 236.
- [ ] Ticks: mandatory = first or tz/offset change versus the previous **inside** tick; mandatory evicts only non-mandatory ticks closer than 30; non-mandatory kept at `>= 30`; all inside ticks returned with `visible`.
- [ ] `changes` empty when `zone_change` is hidden, but `dividers` kept; dividers only for zones `1…` starting inside `[d0, d1)`.
- [ ] `indexTimeline` duplicates: last wins. `timelineBlocksByDay` returns blocks in original plan order and does not filter optional or hidden blocks.
- [ ] Day height comes from the real UTC duration; no `24 × 40` anywhere.
- [ ] Hit frames use `PlanMetrics.minTouchTarget` (48), not `gutterWidth`/`touch` (44); marker frame width 48, zone-change frame from x = 48; clamping as specified. `TimelineRect.contains` is half-open. `PlanMetrics.touch` is still 44.0 in the generated file (0b golden); the overlay is the only Android-only metric and was added through the generator, not by hand.
- [ ] Unparseable instants follow OQ-1b-6 exactly (dropped blocks, ticks and zones; empty day layout); no `!!` on a `planDate` result anywhere in `:core:plan` main code.
- [ ] `PreparedPlan` block comparator: equal **strings** → id, else instant. Status index: pending `null` removes. Summary keeps the trailing space. `ticks` covers all ticks, not only visible ones. End-text de-duplication and the `How this plan ends` filter. `Unknown` direction → "We delay your clock".
- [ ] `replacementAnchor` keeps the anchor offset for every found day and falls back to the first day with offset 0; `openingDay` never matches a `null` requested id.
- [ ] `TripPageLayout.resolve` depends on width only; 600 → 280, 900 → 320.
- [ ] Large fixtures: the transform touches every JSON string in the encoded tree (ids anywhere, ≥ 17-char `T…Z` instants, 10-char dates), `sourceBlocks` uses `end >= start`, the caffeine index runs over **result** days, `displayTz = day.zones.first().tz`, hash string and trip-id format exact, check state emptied, `assumedReturn = null`, templates unmutated.

Android rules:
- [ ] No `String.format`, `DateTimeFormatter` per call, `Locale.getDefault`, `ZoneId.systemDefault`, `Instant.now` or `System.currentTimeMillis` in `:core:plan`/`:core:testing` main code. `%02d`-style output is built by hand.
- [ ] No literal dp in geometry code; everything from `PlanMetrics`. No hand edit of a generated file; generator changes are committed with their outputs.
- [ ] No `Float` geometry; no allocation-heavy work inside comparators (instants parsed once).
- [ ] No top-level `val` doing work; resources loaded lazily on the worker; `PlanPreparer` state behind a `Mutex`, work inside `withContext(worker)`; `CancellationException` never swallowed (no `runCatching`, no `catch (e: Exception)`).
- [ ] Stable identity: unchanged days are the **same instances**; changed days are new; `PreparedDay` has no extra identity field.

Tests:
- [ ] Ported tests keep Swift method names and assert every Swift expectation (compare with the Swift file); Android-only tests are marked "(new)" in their KDoc.
- [ ] Budget test: three warm-ups excluded, cold samples (no cache), every sample asserted, `@TimingGate` present (not `@PerfBudget`), `core:plan` in `TIMING_MODULES`, output line format exact.
- [ ] No `Thread.sleep`, no loops searching for something, `runTest(timeout = 60.seconds)` for coroutine tests; every JVM test except declared `@PerfBudget` and `@TimingGate` ones < 1 s in `timings.json`.
- [ ] Goldens: the [G] table is asserted literally, not recomputed by the code under test.

Typical mistakes to look for: integer division in `laneWidth`; `sortedBy` on `Double?`; comparing `Instant`s parsed in every comparator call (too slow for 200 ms); `compareBy(start).thenBy(id)` for the prepared order; using `layout.ticks.filter { visible }` for `PreparedDay.ticks`; 44 instead of 48 in targets; hard-coded `"Day "`; reading `Localizable` strings from Android resources in `:core:plan`; shifting dates in the fixture with calendar days instead of `duration × index` seconds; emitting `…:00.000Z` instead of `…:00Z`; mutating the shared template lists.

---

## 9. Open questions (proceed on the default; add each to plan.md "Open questions for the user" with the next free OQ-A number in the commit that relies on it)

| # | Question | Default |
|---|---|---|
| OQ-1a-1 (carried) | `TimelineLabels.kt` was split between 1a and 1b | 1a ported `dayDateLabel`, `blockInterval`, `localDate`, `utcOffset`; 1b adds only `nowLabel` |
| OQ-1b-1 | How does JVM `preparePlan` read the PlanUI `Localizable` strings (`PageCopy`)? Android resources need a `Context` and are not reachable from `:core:plan` | Generate `core/plan/src/main/resources/page_copy.json` from the same iOS catalog in `make strings`, like `plan_copy.json`; Compose UI keeps using `strings_ios.xml`. Alternative: a `PageCopy` interface with a `Resources` implementation in `:core:ui` |
| OQ-1b-2 | Where does the JVM `PlanMetrics` come from, and how is the 48 dp target expressed? | 0b's `PlanMetrics.generated.kt` in `:core:plan` keeps the iOS values (golden against the Swift file, `touch = 44`); 1b adds `minTouchTarget = 48` from `tools/tokengen/android-overlay.json`, naming `touch` as what it replaces |
| OQ-1b-11 | `phase-1d-timeline-host.md` l.43 (row P9) lists "`PlanMetrics` … `touch` 48", which contradicts 0b's golden (`touch = 44`) | 1d reads `PlanMetrics.minTouchTarget` (48) wherever it needs the minimum target (its tap bands, the port of the Swift quoted at 1d l.594–595); `touch` stays 44. The orchestrator corrects the 1d brief before 1d starts |
| OQ-1b-3 | Marker accessibility frame: iOS 44 × 44 at x 0; Android minimum 48 | 48 × 48 at x 0 (it overlaps the 4 dp lane gap); the tap band in 1c's `HitIndex` stays iOS `x ≤ 44` (OQ-T5: targets grow, visuals unchanged) |
| OQ-1b-4 | Stable day identity: instance reuse or a revision counter on `PreparedDay` (`04 §9.3`)? | Instance reuse by structural equality; no new field (1c guards its cache with `PreparedDay` equality) |
| OQ-1b-5 | Swift `MockTimelineFixtures` vs plan.md `LargeTimelineFixtures.kt` | `object LargeTimelineFixtures` (plan and 1c/1e briefs), same API as the Swift type; tests keep the Swift name `LargeTimelineFixtureTests` |
| OQ-1b-6 | An instant that cannot be parsed (iOS: NaN geometry) | Drop the block, tick or zone; a day with an unparseable bound gets an empty layout (`height 0`, `minWidth 236`, nothing placed, no ticks) and `nowLineOffset` null. Never crash (Task 3 edge cases) |
| OQ-1b-7 | Large fixtures come from the iOS embedded mock seed (S3 with trip and card), not `plans/S3.json` | Copy the seed literal as `fixtures/mock-seed.json` with a pinned extractor; 4c reuses it for `MockAppSeed` |
| OQ-1b-8 | Long month names for `tripDatesText(long = true)` | Fixed en/nl tables (A17), never locale data; September golden `Sep`/`September` |
| OQ-1b-9 | One warm-up before 10 JVM samples may leave JIT noise in sample 1 | Three warm-ups, every sample asserted, in the serial `make timing` tier (plan.md budget row, review 2026-11-16). If a sample still fails on an idle Mac, report all samples to the orchestrator; do not raise limits |
| OQ-1b-10 | Wide-layout timeline width with a navigation rail (≥ 600 dp) | Not decided here; the geometry test uses `window − insets − 2·20 − 16 − listWidth`. Phase 6e measures the real column width from constraints |

---

## Appendix. Plan checkbox mapping

| plan.md Phase 1b checkbox | Ticked by |
|---|---|
| **Timeline.kt.** | Task 3 |
| **TimelineGeometry.kt** | Task 4 |
| **Labels and accessibility geometry.** | Task 7 |
| **PlanPreparer.kt and PreparedPlan.kt.** | Task 8 |
| **Trip-page state** | Task 9 |
| **Large fixtures:** | Task 10 |
| **Budget and geometry tests.** | Task 11 |
