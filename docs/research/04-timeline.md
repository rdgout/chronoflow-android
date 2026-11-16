# 04 — The trip page timeline: facts, and the Android design

Research for the native Android app (Kotlin + Jetpack Compose). The Android app must behave exactly like the native iOS app, be idiomatic Android, and keep the timeline (the performance-critical screen) smooth on a Pixel 8 and on a low-end phone.

Date: 2026-11-15. All source repos were read only; nothing was built, generated or modified there.

**Tags used throughout**
- **[F]** a fact verified in source, with the path (and line numbers where they help).
- **[R]** a recommendation for Android.
- **[OQ-Tn]** an open question. Section 14 gives the default the plan proceeds on.

**Path prefixes**
- `I/` = `/Users/rickgout/Sites/chronoflow-ios/`
- `PC/` = `I/Packages/ChronoFlowKit/Sources/PlanCore/`
- `PU/` = `I/Packages/ChronoFlowKit/Sources/PlanUI/`
- `RN/` = `/Users/rickgout/Sites/chronoflow-mobile/`
- `W/` = `/Users/rickgout/Sites/chronoflow-new/` (branch `feat/mobile-api-passkey-management` @ `1f4a2684`)
- `DC/` = `/Users/rickgout/Sites/chronoflow-android/_design/canvas/`

**Sibling research docs this one leans on:** `01-ios-plan-reuse.md`, `03-api-contract.md`, `08-design-system.md`, `09-rn-android-lessons.md`, `10-copy-l10n.md`, `11-android-stack.md`, `13-testing-tooling.md`. Where they already decide something (minSdk 29, Compose BOM 2026.09.00, lucide icons, the font-scale cap, the M3 role mapping, gate wording), this doc follows them and says so.

---

## 0. Answer first: what the iOS research and plan already give us

Yes. Most of the timeline is already specified, implemented, measured and tested on iOS in a platform-neutral way. The Android work is mostly porting and re-plumbing the renderer.

### 0.1 Port 1:1 (pure logic, values, tests)

| What | iOS source [F] | Android use [R] |
|---|---|---|
| Timeline algorithms: `timelineBlocksByDay`, `indexTimeline`, `layoutDay`, `packLane`, `tickPositions`, `zoneChangePositions`, `nowLineOffset`, `layoutPlan` | `PC/Timeline.swift` (229 lines) | Kotlin in the pure-JVM timeline module, with the same names and the same tests |
| Geometry: lane width, tile frames, hit frames, min width | `PC/TimelineGeometry.swift`, `PC/TimelineAccessibilityGeometry.swift` | Same; the only change is a 48 dp minimum target instead of 44 (OQ-T5) |
| Labels and plan logic: `dayDateLabel`, `nowLabel`, `blockInterval`, `localDate`, `utcOffset`, `isOptionalStep`, `noticeItems`, `dayZoneLabel`, `summaryIntervals`, `stepLabelKey`, `dayLabelKey`, `dayStatusLabelKey`, `buildCheckState`, `checkIdentity` | `PC/TimelineLabels.swift`, `PC/PlanLogic.swift`, `PC/CheckState.swift` | Same |
| Copy formatting: `CopyLineFormatter` (argument kinds date, instant, offset, minutes, count, milligrams, place, text, Drinks) | `PC/CopyLineFormatter.swift` | Same behaviour, but **cached** formatters. iOS allocates a formatter per call; Android must not (§9.3) |
| Metric and type tokens | `I/Tools/TokenGen/Plan.source.json` → `PlanMetrics.generated.swift` | Feed the same JSON to the Android token generator (08 §3). The Swift output is the golden |
| Presentation model: `PreparedPlan` / `PreparedDay` / `PreparedBlock`, block order, the accessibility-label recipe, end-state keys | `PU/TripPage/PreparedPlan.swift` | Same fields, as immutable Kotlin classes |
| Page state machines: `resolve`, `openingDay`, `replacementAnchor`; wide-layout math | `PU/TripPage/TripPageState.swift`, `PU/TripPage/TripPageLayout.swift` | Same |
| Fixtures: S1–S6 (RN JSON originals) and the large30/large60 generator | `RN/src/data/mock/fixtures/plans/*.json`, `I/Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockTimelineFixtures.swift` | Load the JSON as-is; port the generator; assert the same invariants (§12) |
| Perf protocol | `I/Docs/perf-timeline.md` | The four guarantee layers and their numbers (40/100/200 ms host; 0.8 s first frame; ≤ 5 ms/s hitches; ≤ 150 MiB; ≤ 512 KiB per drawing; ≤ 4 MiB cache). Also the invalidation gates (0/0, 0/1, 0/2), the 15-gesture scroll test, "never hundreds of swipes", and the content-height math |
| Test designs | `PU/../Tests/PlanUITests/*` | Ports of: geometry; accessibility targets in bounds at three widths; 60-day traversal at two widths; compact-tile and alpha tests; invalidation; intl goldens; DST 23/25/31 h days |
| Product decisions | `I/plan.md` | Optional steps never shown. Timeline is the default; the toggle is not persisted. Airport-code title. Width-only breakpoints 600/900 dp. Tiles show only icons below a 640 dp window. Day picker = system menu. Opaque compact header. Test identifiers `trip.block.<id>`, `trip.plan.end`, `trip.viewToggle`, `trip.dayMenu.day.<i>`, `trip.timeline.ready|preparing` |

### 0.2 Adapt

| iOS | Android [R] | Why |
|---|---|---|
| A worker records each day as a vector PDF display list (`CGPDFDocument`). It is drawn by `Canvas(opaque:)` strips no taller than half the viewport | A worker builds each day as a compact, immutable **op list** (pure data). The op list is replayed into the day item's own Compose layer (RenderNode). An off-main `android.graphics.Picture` is an optional optimisation | Android RenderNodes are vector display lists, not bitmaps. The iOS reason for strips (bounding raster backing stores) does not apply |
| Per-day `DayClockState` (@Observable) | A per-day `State<NowLine?>` holder, read only by one small `NowLine` composable | Same invalidation shape: one tiny scope per tick |
| `TripScrollTracker`: plain class, UIView alpha, CADisplayLink | `LazyListState` read only in the layout and draw phases. `graphicsLayer { alpha }` for the fade. `derivedStateOf` for boundaries. `requestScrollToItem` for restoration | Compose-native, with no per-frame recomposition |
| SwiftUI `Menu`/`ToolbarTitleMenu`, `.sheet`, `.refreshable` | M3 `TopAppBar` + `DropdownMenu`, `ModalBottomSheet`, `PullToRefreshBox` | The board says "Compose DropdownMenu on Android" |
| 44 pt targets | 48 dp targets, visuals unchanged | Android accessibility guidance (08 §12) |
| Instruments (Hitches, Time Profiler, Allocations) | Perfetto + Macrobenchmark + `dumpsys gfxinfo/meminfo` | §11 |

### 0.3 Do not reuse

PDF display lists; SF Symbol rasters (`TimelineSymbols`); the CADisplayLink 60-tick resize-restoration loop; the UIView-alpha hack; the iOS 26 `ToolbarTitleMenu` fork; the "strip ≤ viewport/2" rule as a memory measure; iOS-specific signpost plumbing.

**From RN, keep only the lessons (§8).** Do not copy the per-tile view tree, screen-level `now`, JS-thread scroll work, or variable-height cells without known offsets.

---

## 1. Sources read

- **iOS docs:** `I/Docs/perf-timeline.md` (all); `I/plan.md` (decisions l.25–58, §1.1 ≈l.1323–1400, §1.2 ≈l.1401–1447, trips gaps ≈l.1498–1540, Phase 14 l.1623–1658, Phase 15 from l.1659, Phase 17 from l.1909, pull to refresh ≈l.2252, perf budget l.2286, open questions l.2376).
- **iOS PlanCore:** `Timeline.swift`, `TimelineGeometry.swift`, `TimelineLabels.swift`, `TimelineAccessibilityGeometry.swift`, `CheckState.swift`, `PlanLogic.swift`, `CopyLineFormatter.swift`, `PlanClock.swift`, `FixedPlanClock.swift`, `PlanMetrics.generated.swift`.
- **iOS PlanUI:** `Models/PlanDecoder.swift`, `Models/SystemPlanClock.swift`, `Models/TripPlanModel.swift`; `TripPage/` (`PreparedPlan`, `TimelineRenderer`, `PlanAppearance`, `DayTimelineView`, `TripDayRow`, `DayHeader`, `DayListView`, `TripScrollTracker`, `TripPageView`, `TripPageModel`, `TripPageComponents`, `TripPageLayout`, `TripPageState`, `TripHeaderText`); `Steps/StepSheet.swift`, `Steps/LegendSheet.swift`, `Steps/TripPageStepSheet.swift`; string catalogs `PU/Resources/Localizable.xcstrings` (104 keys), `Screens.xcstrings` (121), `PC/Resources/PlanCopy.xcstrings` (313).
- **iOS contracts:** `I/Sources/AppCore/Contracts/Generated/TripPlan.swift`.
- **iOS tests:** `TimelinePerformanceTests`, `LargeTimelineFixtureTests`, `TimelineRenderingTests`, `TimelineInvalidationTests`, `TimelineAlphaTests`; fixtures `MockTimelineFixtures.swift`; tokens `I/Tools/TokenGen/Plan.source.json`.
- **Design canvas:** `DC/Plan.dc.html`, `PlanWide.dc.html`, `ItemDetail.dc.html`, `PlanLegend.dc.html`, the Plan* wrappers, `ds/chronoflow/components/bundle.js` and `bundle.css`, `ds/chronoflow/tokens.json`, and the notes in `canvas.json` (`reqWideTrip`, `reqSystemChrome`, `dirNative`, `reqContinuousPlan`, `reqPlanFlow`, `reqTripOffline`, `planCompactNote`, `planCompactQuestions`, `decOffline`).
- **RN:**
  - `RN/plan.md` (Phase 8 l.404–415, Phase 17 l.1014–1038, Phase 25 l.1451–1478); `RN/docs/plan-gate.md` l.1353–1366.
  - `src/components/plan/DayTimeline.tsx`, `TimelineBlock.tsx`, `tileModel.ts`, `TripScreen.tsx`, `TripTitle.android.tsx`, `useTripTitle.ts`; `src/plan/timeline.ts`, `categoryStyles.ts`, `usePlanNow.ts`.
  - Commits `6e7cc0b` and `2d9ea94`.
- **Web:** `W/routes/api.php` l.95–112; `W/app/Data/Api/ApiTripDetail.php`, `ApiCheckOffInput`, `ApiTripCheckResult`; `W/resources/js/lib/timeline-layout.ts`, `lib/timeline-scroll.ts`, `pages/app/trips/show.tsx`.

---

## 2. Data model [F]

### 2.1 Endpoint shapes

**`GET /v1/trips/{trip}`** (`W/routes/api.php:95–112`)
- Middleware: `auth:sanctum`, `throttle:api`, VaryOnAuthorization, `cache.headers:private;no_cache;etag`.
- Body `ApiTripDetail` (`W/app/Data/Api/ApiTripDetail.php`):

```text
{ trip: TripDetailView, plan: PlanView, notices: PlanNoticesView,
  timeline: TimelinePresentationView, checkState: CheckStateView, detailVersion: string }
```

- Plan copy is always English `CopyLine {key, args}`. The API ignores `Accept-Language` (`I/plan.md` §1.1). The phone renders copy through the catalogs (10).
- Size: plan JSON is 28–185 KB. Among the RN fixtures, `S5.json` is 28,090 B and `S6-RETURN-10N.json` is 185,066 B.
- The detail returns **all** optional items. The phone filters them (03 §12).

**`PUT /v1/trips/{trip}/checks`** (03 §5.3 row 39, §12)
- Body, exactly:

```text
{ plan_input_hash: <64 hex>, type: BlockType, local_date: "YYYY-MM-DD",
  ordinal: int 1–255, status: "done" | "skipped" | null }
```

- `status` must be **present**, even when it is `null`.
- 200 → `ApiTripCheckResult {checkState, detailVersion}`, with the same `detailVersion` as GET, so no re-pull is needed.
- 422 `form`: `"This plan has changed. Refresh it before marking this step."` when the hash is stale; `"This step cannot be checked off."` for an unknown key.

**`GET /v1/trips`:** list items carry `keepOffline`, plus `detailVersion|null` and `checkState|null`. The last two are set only when `keepOffline` (03 §12).

**Contract types** (`I/Sources/AppCore/Contracts/Generated/TripPlan.swift`)

| Type (line) | Fields / cases |
|---|---|
| `PlanView` (2496) | status, reasons, prompts, meta, summary?, days, blocks, warnings |
| `DayView` (1739) | id, labelDate, labelRange?, labelPhase, startUtc, endUtc, zones, phase, postArrivalIndex?, status?, noLightText?, notes, headline? |
| `DayZoneView` (1785) | tz, startUtc, endUtc |
| `BlockView` (1178) | id, lane, type, startUtc, endUtc, displayTz, secondaryTz?, priority, text, secondaryText?, params, actionable, checkKey? |
| `CheckKeyView` (1402) | type, localDate, ordinal |
| `TickView` (2028) | atUtc, tz, offsetMin |
| `TimelinePresentationView` (3454) | days (dayId + ticks), blockReasons (blockId + lines), transferWarnings (blockId + warningIndexes) |
| `NoteView` (2411) | role, clinicianLink, lines, endState? |
| `CopyLine` (1583) | key, args |
| `Lane` (2084) | travel, sleep, light, caffeine, info |
| `BlockType` (1102) | flight, layover, transfer, movement_break, zone_change, sleep, nap, rest, avoid_light, seek_light, light_ok, light_neutral, caffeine_serving, caffeine_ok, caffeine_routine, no_caffeine, note, unknown |
| `Priority` (2577) | core, recommended, optional, info |
| `DayPhase` (1668) | pre, travel, post |
| `LabelPhase` (2044) | pre_departure, travel, travel_arrival_night, arrival_day_0, day |
| `DayStatus` (1702) | adapting, partially_adapted, adapted, adapted_slower_clock |

- Params of note: flight `overnight`, `legIndex`; sleep `kind` (`main`, `inFlightMain`, …); zone_change `offsetMin`.
- Instant parsing (`planDate`, `PC/CopyLineFormatter.swift`): a 10-character date gets `T12:00:00Z` appended. A 17-character `…THH:MMZ` gets `:00` inserted before `Z`.

[R]
- Kotlin DTOs come from the contract generator in 03 §16.
- Enums must be tolerant: an unknown value becomes `Unknown(raw)` and never throws. Unknown types are skipped only at render time (`I/plan.md` trips gaps).
- Keep the raw bytes in the DB.
- In layout code, hold instants as epoch-millisecond `Long`s, not `Instant` objects inside hot arrays.

### 2.2 Identities and cache keys

- **`planInputHash`** decides whether a check mark is stale.
- **`detailVersion`** is xxh128 of `trip`, `plan`, `notices` and `timeline`. It excludes `checkState`.
- **ETag** covers the whole body (03 §12).
- **iOS `PreparedPlan.revision`** = `inputHash|detailVersion`.
- **`PlanDecoder`** (an actor; it asserts it is not on the main thread) keeps the latest result per trip, keyed by `(tripId, inputHash, detailVersion)`, with a raw-byte guard.
  - `detail(snapshot)` merges current-hash marks with pending outbox checks. A `nil` status removes a mark.
  - It also exposes `layouts`, `prepare`, `nowTexts` (only for days that contain now) and `evict` (`PU/Models/PlanDecoder.swift`).
- **`keepOffline`** = active, or upcoming with plan start ≤ now + N days. N is the server flag `OFFLINE_MOBILE_PREFETCH_DAYS`, default 7. Never hard-code 7.

### 2.3 Days

- A day is the UTC interval `[startUtc, endUtc)`.
- Height = minutes × 40/60 pt (`offsetFrom`, `PC/Timeline.swift:74–76`). The day length is the real UTC duration, never 24 × 40 (`I/plan.md` trips gaps).
  - Days of 23 h and 25 h (DST) and 30 h or more (travel) occur.
  - The large60 cycle is 24, 24, 13.5, 25.5, 24, 24 h → 960, 960, 540, 1,020, 960, 960 pt (`I/Docs/perf-timeline.md`).
- Labels (`PC/PlanLogic.swift`, `PC/TimelineLabels.swift`):
  - `dayLabelKey` by `labelPhase`: "Before departure", "Travel day", "Travel · arrival night", "Arrival day", "Day :count".
  - `dayDateLabel`: "EEE d MMM". A `labelRange` gives "EEE d MMM – EEE d MMM", with the first month omitted when both dates share it.
  - `dayZoneLabel` joins the zone labels with " → ".
  - `zoneLabel` is the airport city, else the last tz path component with "_" replaced by a space.
  - `dayStatusLabelKey`: "Adjusting", "Partly adapted", "Expected adapted", "Protect a slower clock".

### 2.4 Lanes, categories and what is shown

- Timeline lanes, in order: `[sleep, light, caffeine, travel]` (`PC/Timeline.swift:63`).
  - `info`-lane blocks become **gutter markers**.
  - `zone_change` blocks become **zone-change positions**, not tiles.
- `isOptionalStep` (`PC/PlanLogic.swift`) is true for:
  - `avoid_light` or `seek_light` with priority `optional`;
  - `caffeine_ok` always;
  - `light_ok` or `light_neutral` with priority `info`.

  Optional steps are **never shown**: RN decision 32; `layoutDay` and `summaryIntervals` both enforce it.
- Hidden categories (settings `hiddenCategories`) are skipped too. When any are hidden, the page ends with the button "Some steps are hidden in your preferences."

### 2.5 Bucketing blocks into days

`timelineBlocksByDay` (`PC/Timeline.swift:201–229`):
1. Sort blocks by `(start, original index)`.
2. Sweep the days in start order, keeping an active set.
3. Drop blocks with `end ≤ dayStart`, unless they are zero-length at or after the start.
4. Keep the original plan order inside a day.

The plan is scanned once, not once per day.

### 2.6 `layoutDay` (`PC/Timeline.swift:136–187`), exact rules

1. **Skip** a block if it is unsupported, optional, of a hidden type, or a `zone_change`.
2. With `a = start`, `b = end`, **skip** if `a ≥ dayEnd || (b ≤ dayStart && !(a == b && a ≥ dayStart))`.
3. `exactTop = offset(max(dayStart, a))`.
4. **Info lane → marker:**
   - `top = exactTop + gutterMarkerSize/2` (= +12).
   - After sorting by `exactTop`, de-overlap: `top[i] = max(top[i], top[i−1] + 24)`.
5. **Tile:**
   - `tileHeight = min(dayHeight, max(24, offset(clampedStart → clampedEnd)))`;
   - `top = min(exactTop, dayHeight − tileHeight)`;
   - `tiny = tileHeight ≤ 56`;
   - `continuesBefore = a < dayStart`, `continuesAfter = b > dayEnd`;
   - `presentation = (blockReasons[id], transferWarnings[id])`.
6. `packLane` runs per lane (2.7).
7. `minWidth = 44 + 4·4 + Σ_lanes max(44, cols·30)`, where `cols` = the lane's maximum column count.
8. `changes` = the zone-change positions (empty if `zone_change` is hidden).
9. `dividers` = one per zone after the first whose start lies in `[dayStart, dayEnd)` (2.8).
10. `ticks` = `tickPositions` (2.9).

### 2.7 `packLane` (`PC/Timeline.swift:114–134`)

1. Sort by `top`, then by `exactTop`.
2. Walk the items, keeping the current overlap group and its column end positions.
3. A new group starts when `top ≥ groupEnd`.
4. Each item takes the first column whose end ≤ its top, else a new column.
5. When a group is flushed, every member gets `cols = number of columns in that group`.

The result is stable: the same input always gives the same columns.

### 2.8 Zones

- **Zone-change positions** are `zone_change` blocks with start in `[dayStart, dayEnd)`, sorted by start, with `top = offset(start)` (`PC/Timeline.swift:83–89`).
- **Dividers** are `ZoneDivider(before = zones[i−1], after = zones[i], top)` for `zones[1...]` that start inside the day.
- **Pill copy:** "%@ time from here".
- **List mode** shows each zone change as a `ZoneChangeCard` (3.14).

### 2.9 Ticks (`PC/Timeline.swift:91–112`)

1. Take the server ticks with `atUtc ∈ [dayStart, dayEnd)`.
2. A tick is **mandatory** if it is the first one, or if its `tz` or `offsetMin` differs from the previous tick.
3. Thin greedily with `minTickSpacing = 30` pt:
   - a mandatory tick evicts any preceding **non-mandatory** visible ticks closer than 30;
   - a non-mandatory tick is kept only if it is ≥ 30 from the last visible one.
4. Every tick keeps `visible: Bool`. Only visible ticks are drawn and exposed to accessibility.

### 2.10 Now

- `nowLineOffset(day, now)` = `offset(now)` if `dayStart ≤ now < dayEnd`, else `nil` (`PC/Timeline.swift:78–81`).
- `nowLabel` formats now in the day zone that contains now, else in the last zone (`PC/TimelineLabels.swift`).
  - Time format: `"%02d:%02d"`, or 12-hour with the AM/PM catalog key, following the user's `timeFormat` preference.

### 2.11 Overnight flights, multi-timezone days, DST

- **A block crossing a plan-day boundary** is drawn in each day, clipped to that day. Each clipped edge gets:
  - `continuesBefore` / `continuesAfter`;
  - squared corners on that edge;
  - a 4 pt chevron;
  - the spoken suffix "Continues from the previous plan day" / "Continues into the next plan day".
- **Overnight flights:** `params.overnight` on the flight. In-flight sleep is `kind = inFlightMain`; `summaryIntervals` counts only `main` and `inFlightMain` sleep, split per zone and merged.
- **`blockInterval`:**
  - Each block is formatted in its own `displayTz` (and `secondaryTz` when present).
  - When the local dates differ in `displayTz`, dates are prefixed.
  - When the UTC offset changes inside the interval, " (GMT±hh:mm)" is appended.
  - The separator is " – " (the step sheet uses " → ").
  - `utcOffset` returns "GMT" or "GMT±HH:MM".
- **A multi-zone day** has several `zones`. Ticks are mandatory at each change, and each divider gets a pill. The header zone label reads "A → B".
- **DST goldens** (Phase 14): Europe/Amsterdam on 2026-10-25 and America/New_York on 2026-11-01. Tests cover 23-, 25- and 31-hour days.

### 2.12 Geometry (`PC/TimelineGeometry.swift`)

```text
width       = max(layout.minWidth, availableWidth)
laneWidth   = (width − 44) / 4 − 4
column      = laneWidth / cols
tile.x      = 44 + 4 + laneIndex·(laneWidth + 4) + col·column + 2
tile.width  = max(0, column − 4);  tile.y = placed.top;  tile.height = placed.height
hitFrame    = max(44, tile.width) × max(44, tile.height), centred on the tile,
              clamped into [0, width] × [0, height]
```

- Marker targets: x 0, width 44, height 44.
- Zone-change targets: x 48 to the full width, height 44 (`PC/TimelineAccessibilityGeometry.swift`).
- If `minWidth > available`, the day scrolls horizontally (`PU/TripPage/DayTimelineView.swift`). The fixture minimum lane widths are 252 and 268 pt against a 358 pt content width, so there is no overflow on common phones. A 320 dp phone (288 dp content) can overflow.

### 2.13 Prepared model (`PU/TripPage/PreparedPlan.swift`)

- `PreparedPlan.revision = inputHash|detailVersion`. Per day it holds labels, zone label, progress and notices. Per block it holds title, interval, priority, mark and accessibility label.
- **Block order inside a day:** placed tiles + markers + zone changes, sorted by `startUtc`, then `id`.
- **Accessibility label:** these parts joined with ", ":
  1. title;
  2. priority;
  3. interval;
  4. "Marked done" or "Skipped";
  5. "Continues from the previous plan day";
  6. "Continues into the next plan day".
- **Other copy built here:**
  - Zone instruction: "From here on all times are %@ time. Set your watch to %@."
  - Comparison: "From time → Dest interval · offset".
  - Progress: "%@ of %@ steps done".
  - Divider: "%@ time from here".
  - Priorities: Important / Recommended / Optional / Information.
  - Clock shift: "No clock shift planned" / "We advance your clock %@" / "We delay your clock %@" / "%@ hours".
  - End states: "Expected adapted", "Likely adapted", "Still adjusting", "Partial shift", "Keep your usual rhythm", "Return plan starts", "Stay closer to home time", "Holding this shift". Fallback: "There are no more steps in this plan."
  - Assumed trip end: "Assumed trip end", with "Based on your check-out date" or "Based on %@ nights".

### 2.14 Check state

- `checkIdentity = "type|localDate|ordinal"`.
- `buildCheckState` counts per-day done, skipped and total by the day in which the block **starts** (`PC/CheckState.swift`).
- A check-off enqueues `.checkOff(CheckOffChange(tripID, planInputHash, type, localDate, ordinal, status))` in the outbox (`PU/Models/TripPlanModel.swift:132–141`).

---

## 3. Visual spec [F]

Units: iOS pt = Android dp, and type pt = sp (08). Colours are named tokens (3.13; sRGB values in 08 §4.1).

### 3.1 Page structure (`PU/TripPage/TripPageView.swift`)

A `LazyVStack` with spacing 12 (`sectionGap`), in this order:
1. metadata line (only when the plan is not ready);
2. summary `PlanCard`;
3. `DayNavigation`;
4. one row per day (`.equatable().id(day.id)`);
5. `EndOfPlanCard`;
6. the hidden-steps button.

- Each row gets `compact` (window width < 640), `stickyTop` and `viewportHeight`.
- The gutter is 16, or 20 when wide.
- Board: main padding `8px 16px 120px`, gap 12 (`DC/Plan.dc.html`).
- There is no card around a day; each day shows its whole span; the end card appears once, below the last day (`reqPlanFlow`).

### 3.2 Top app bar and day menu

- **Title:** "IATA → IATA" (codes avoid truncation).
- **Subtitle:**
  - At rest: `prepared.subtitle`, the trip meta from `tripMetaText`, e.g. "12–18 Oct · KL 861".
  - Once scrolled (the anchor day's top is above the viewport): "\<day title\> · \<date\>".
  - Board comment: "Subtitle: trip dates at the top; the day at the top of the screen once scrolled. Tapping the title opens the day menu."
- **Day menu:**
  - Section "Go to a day". Items read "title · date"; on the boards they are two-line: label 17/22 + date 13/18 tabular, minimum height 52.
  - A check marks the current day. Identifiers `trip.dayMenu.day.<i>`.
  - The board specifies "the system menu (SwiftUI Menu on iOS, Compose DropdownMenu on Android), not a custom popover". The Done note (5 Nov) says "Android: Compose DropdownMenu with the same two-line items."
- **Spoken title:** "%@ to %@, %@, %@. Go to a day" (scrolled) or "%@ to %@, %@. Go to a day" (at rest).
- **Actions:**
  - View toggle: "Show as list" (list icon) / "Show as timeline" (timeline icon). Identifier `trip.viewToggle`. Phone only; there is no toggle on wide layouts.
  - "Trip actions" (more): Edit and Duplicate, disabled offline with "Needs a connection." Complete, Delete and Add to calendar are omitted (iOS 17c, OQ 42).
- **RN Android reference** (`RN/src/components/plan/TripTitle.android.tsx`):
  - Compose `DropdownMenu` (colour card).
  - A TextButton holds a Column: title in Figtree 600 17/22, `maxLines 1`; then a Row (`spacedBy 3`) with the subtitle in Figtree 400 12/15 muted and a 12 dp chevron.
  - `contentDescription` = the spoken label.
  - Items are `SelectableDropdownMenuItem` with the date as supporting text.

### 3.3 Summary card and day navigation

- **`PlanCard`** (padding 16, radius 26, shadow-soft):
  - "Your plan" eyebrow (12/16, 600, 0.1em tracking, dawn-text);
  - the clock-shift line in Fraunces (board 20/28);
  - a 44 legend button on surface-2, labelled "Legend: what the colours and patterns mean";
  - a 14/20 muted summary;
  - `DualClocks`:
    - labels "Home · %@" / "There · %@" (remote), or "You’re here · %@" / "Home time · %@" / "Destination time · %@" (local);
    - the active clock has a 2 pt dawn border on card; the inactive one sits on surface2;
    - radius 22 and padding 12 on iOS (board: radius 18, padding 10 × 12); times 20/28 600 tabular.
- **`DayNavigation`** (`PU/TripPage/TripPageComponents.swift`):
  - "Previous day" / "Next day" chevron buttons, 44.
  - A lazy horizontal row of capsule pills (title + date, padding 16, minimum height 44). The selected pill uses dawn with dawnForeground.
  - Container label "Plan days". The selected pill auto-centres.
  - The pills scroll away with the content (`planCompactNote`).

### 3.4 Day header (`PU/TripPage/DayHeader.swift`, `DC/Plan.dc.html`)

- **Collapsed:**
  - Fraunces 400 18/26 title + " · date" (planListLine, muted; board 14 tabular);
  - a zone pill (planSubtitle 12/15, padding 4, muted capsule; board 28 high, 12/16 500);
  - a "%lld warnings" button (plural) with a warning triangle in dawnText (board: 28 high, dawn 40% border, dawn 10% fill).
- **Expanded:**
  - status (dawnText);
  - summary ("sleep · light", 12/16);
  - progress ("%@ of %@ steps done");
  - headline (dawnText 12/16 500);
  - a "%lld notes" button, only when there are no warnings;
  - `DayNotice` cards (warning: dawn 0.1 fill + dawn 0.4 border; info: card + border; radius 22, padding 12).
- "Show more" / "Show less" + chevron, minimum height 44 (board 28 high, 12 px).
- All headers start collapsed (`reqPlanFlow`).
- Height ≈ 26 + 4 + 44 + 2 × 8 = 90 pt collapsed. A warning header adds 24 (`perf-timeline.md` content-height math).

### 3.5 Compact header line (`PU/TripPage/TripScrollTracker.swift:126–134`)

- An overlay pinned at the top of the scroll viewport, minimum height 36 (`compactHeaderHeight`), with an opaque `card` background.
  - Phone timeline: the lane headings with the date in the gutter (weekday Figtree 600 10/12 over "d MMM" Figtree 400 10/12, tabular).
  - Phone list: the compact day title (13/18: label 600 + date 400 muted; zone 12).
  - Wide: title + lanes + a list-width spacer (board: "Steps" over the list).
- `opacity = clamp(1 − headerMaxY / 24, 0, 1)`, where `headerMaxY` is the current day's `DayHeader` bottom relative to the sticky top (`compactFadeDistance 24`).
  - With Reduce Motion it is an instant switch at 0.
  - While invisible it is hidden from accessibility.
- `planCompactNote` (5 Nov): this gives about 650 pt of timeline instead of about 325.
- The board draws glass and a 0.5 hairline; iOS ships an opaque card (OQ-T13).

### 3.6 Lane headings (`DayTimelineView.swift`, `LaneHeadings`)

- "Sleep", "Light", "Caffeine", "Travel" in planLane 11/14 500, mutedForeground.
- Each has a 6 pt dot (`dot` token) in its lane colour: catSleep, catLight, catCaffeine, catTravel (RN `laneColorKey`).
- The board uses an 8 px dot, a grid of 44px + 4 × 1fr, gap 4, padding-top 4 (OQ-T2).
- They add 14 pt per day.

### 3.7 Timeline drawing spec (`PU/TripPage/TimelineRenderer.swift`, 462 lines)

Drawn in this order:

1. **Background:** the page `background`.
2. **Lanes:** four `surface2` rects, one per lane, full day height. The gutter and lane gaps show the background.
3. **Ticks** (visible ones only):
   - a 1 pt line in `border` at 0.6 alpha (`grid` opacity) across each lane;
   - a label right-aligned in a 40 pt gutter rect (board: `right: 4`), 14 pt high, centred on the tick (board: `translateY(−50%)`), in mutedForeground, Figtree 400 at scaled 11, tabular per the board.
4. **Tiles**, per lane and placed block:
   - **Shape:** radius 14; a capsule for `caffeine_serving`. Top corners are square if `continuesBefore`, bottom corners if `continuesAfter`.
   - **Group alpha:** the whole tile is drawn inside a transparency layer at **0.6** when marked (done or skipped) or **0.75** when optional.
   - **Fill**, then a **pattern** clipped to the shape:
     - dots: radius 1 on a 7 pt grid, fg at 0.16;
     - hatch: 2 pt lines at 45°, horizontal spacing 8·√2 (perpendicular period 8), fg at 0.12.
   - **Core priority:** a left bar 4 pt wide, foreground at 0.7.
   - **Outlines:**

     | Case | Colour | Width | Style |
     |---|---|---|---|
     | optional | foreground at 0.5 | 1 | dashed [3, 3] × thickness |
     | `no_caffeine` | catCaffeine | 2 | dashed |
     | `light_ok` | catModerateFg at 0.4 | 1 | dashed |
     | `note` | border | 1 | solid |

   - **Icon:** 14 × fontScale.
     - **Icon-only when compact (window < 640) or tiny.** Icons are centred, and `caffeine_serving` is also centred vertically.
     - `no_caffeine` gets a 2 pt diagonal strike.
   - **Text colour:** cardForeground when marked or optional, else the category foreground.
   - **Title** (Figtree 600, planBlockLabel 11/14) at the icon's maxX + 4. Each text line sits on a fill-coloured backing plate. The interval line is 4 pt below the title. `caffeine_serving` instead has one centred line, "title · interval".
   - **Details,** only when `cols == 1`, height > 76 (`detailsThreshold`), not icon-only and not `caffeine_serving`:
     - a priority tag pill (Figtree 500 10, height 14 + 2, background at 0.4 (`tag`), 1 pt stroke);
     - then the first reason line.
   - **Status icon** top right: done = checkmark, skipped = "forward.end".
   - **Continuation chevrons:** 4 pt.
5. **Zone dividers:**
   - a full-width line with round caps, dash [2, 4], 2 pt, catTravel;
   - a pill at x 48, 18 pt high (14 + 2 × 2), 8 pt horizontal padding, catTravel fill, catTravelFg text "%@ time from here";
   - divider frames are recorded for accessibility.
   - The board draws `border-top: 2px dotted var(--cat-travel)` and a 10/14 500 pill.
6. **Info markers:** a 24 pt `card`-coloured circle at x 20, holding an "i" in Figtree 600.
7. **Text** is always single-line, truncated with "…".

**The now line is not part of the static drawing.** It is a separate view (3.10).

**Cache** (same file):
- LRU of ≤ 4 entries **and** ≤ 4 MiB in total (`cacheByteLimit = 4 * 1_024 * 1_024`, l.18; eviction loop l.58), keyed by day id.
- An entry is reused only if `(day, layout, width, scale, fontSize, appearance, compact)` are equal.
- Symbols: SF Symbol rasters per appearance and scale (`TimelineSymbols`).

### 3.8 Category styles (`PU/TripPage/PlanAppearance.swift`; RN `categoryStyles.ts`; `DC/ds/chronoflow/components/bundle.js`)

| Type | Fill / foreground | Pattern | Outline | iOS symbol | lucide (RN, boards) | Label key |
|---|---|---|---|---|---|---|
| sleep | catSleep / catSleepFg | – | – | moon | Moon | Sleep |
| nap | catSleep / catSleepFg | dots | – | bed.double | Bed | Nap |
| rest | catSleep / catSleepFg | hatch | – | chair.lounge | Armchair | Rest |
| seek_light | catLight / catLightFg | – | – | sun.max | Sun | Seek bright light |
| avoid_light | catDark / catDarkFg | hatch | – | eyeglasses | Glasses | Avoid bright light |
| light_ok | catModerate / catModerateFg | dots | dashed catModerateFg 0.4, 1 | cloud.sun | CloudSun | Normal light is fine |
| light_neutral | catModerate / catModerateFg | – | – | lamp.table | Lamp | Indoor light |
| caffeine_serving | catCaffeine / catCaffeineFg | dots | – (capsule) | cup.and.saucer | Coffee | Caffeine serving |
| caffeine_routine | catCaffeine / catCaffeineFg | hatch | – | cup.and.saucer | Coffee | Caffeine routine |
| caffeine_ok | catCaffeine / catCaffeineFg | – | – | cup.and.saucer | Coffee | Caffeine window (never shown: optional) |
| no_caffeine | card / foreground | – | dashed catCaffeine 2 + icon strike | cup.and.saucer | Coffee + 45° strike | Avoid caffeine |
| flight | catTravel / catTravelFg | – | – | airplane | Plane | Flight |
| layover | catTravel / catTravelFg | hatch | – | timer | Timer | Layover |
| transfer | catTravel / catTravelFg | – | – | arrow.left.arrow.right | ArrowLeftRight | Airport transfer |
| movement_break | catTravel / catTravelFg | – | – | figure.walk | Footprints | Movement break |
| zone_change | catTravel / catTravelFg | – | – | globe | Globe | Time-zone change |
| note | card / foreground | – | border 1 solid | info.circle | Info | Note |
| default / unknown | card / foreground | – | – | info.circle | Info | – (skipped in layout) |

- Priority styles in the bundle (`me`):
  - core: `border-l-4 border-l-foreground/70`;
  - optional: `border-dashed border-foreground/50` + `opacity-75`;
  - info: `saturate-50` (iOS has no saturate; not ported).
- Priority descriptions: on iOS, PriorityKey uses "The steps that matter most for shifting your clock." etc. The bundle has different text. Follow the iOS catalog.

### 3.9 Tile content rules (iOS = RN `tileModel.ts:60–68`)

- `iconOnly = compact || tiny`. **On phones (< 640 dp window) every tile is icon-only.** Tile text appears only on unfolded foldables and tablets. The window-width class is independent of the lane or scroll width.
- `narrow` when the tile width is < 36 (RN).
- `details` = not iconOnly, `cols == 1`, height > 76, not `caffeine_serving`.
- Pixel tests (`TimelineAlphaTests`):
  - compact or tiny tiles hide all copy, checked by pixel equality at 320/390/393/430/639/640/669 pt, light and dark;
  - the core bar changes pixels only inside the bar region.

### 3.10 Now line (`DayTimelineView.swift`)

- A separate view, positioned with `.offset(y: offset − h/2)` and hidden from accessibility.
- **Line:** 2 pt in `now`. The `now` token is "never used for anything else" (tokens.json).
- **Pill:**
  - planNow 10/16 600 with monospaced digits;
  - primaryForeground on `now`;
  - horizontal padding 6;
  - scaled minimum height 16;
  - board: radius full, left 0.
- The accessibility element "Now, %@" is a separate child that covers the line and pill.

### 3.11 Metrics (`PlanMetrics.generated.swift`)

| Group | Values |
|---|---|
| Timeline | pointsPerHour 40, gutterWidth 44, laneGap 4, tileInset 2, minLaneWidth 44, minColumnWidth 30, minTickSpacing 30, minTileHeight 24, tinyTileHeight 56, gutterMarkerSize 24 |
| Tiles | tileRadius 14, tilePadding 6, blockIcon 14, icon 16, detailsThreshold 76, coreBorder 4, emphasisBorder 2, border 1, line 1 |
| Patterns | patternStep 8, patternLine 2, dotStep 7, dotRadius 1, dot 6 |
| Header and zones | compactHeaderHeight 36, compactFadeDistance 24, zonePillPadding 8, zonePillVerticalPadding 2, zonePadding 20, textPlateRadius 10 |
| Breakpoints | timelineLabelBreakpoint 640, wideBreakpoint 600, wideLargeBreakpoint 900 |
| Wide | wideListWidth 280, wideLargeListWidth 320, wideListMaxHeight 700, wideColumnGap 16, wideGutter 20 |
| Page | sectionGap 12, rowGap 8, smallGap 4, rowRadius 22, cardRadius 26, cardPadding 16, gutter 16, touch 44 |
| Skeletons | skeletonSummary 120, skeletonTimeline 380 (board skeleton: 120 / 44 / 380) |
| Priority sample | 40 × 28 |
| Opacities | coreBorder 0.7, dot 0.16, grid 0.6, hatch 0.12, marked 0.6, markedRow 0.7, optionalBorder 0.5, optional 0.75, tag 0.4, warning 0.1 |

Source of truth: `I/Tools/TokenGen/Plan.source.json`.
- `spacingMultipliers` are ×4 (pointsPerHour 10 → 40; timelineLabelBreakpoint 160 → 640).
- `spacingTokens`: minTileHeight space-6, tinyTileHeight space-14, gutterMarkerSize space-6, laneGap space-1.

### 3.12 Typography (`Plan.source.json` `type`)

| Token | Size / line | Weight | Family |
|---|---|---|---|
| planTitle | 17/22 | 600 | Figtree |
| planSubtitle | 12/15 | 400 | Figtree |
| planLane | 11/14 | 500 | Figtree |
| planBlockLabel | 11/14 | 600 | Figtree |
| planCompactDate | 10/12 | 400 | Figtree |
| planCompactWeekday | 10/12 | 600 | Figtree |
| planListLine | 13/18 | 400 | Figtree |
| planListTitle | 13/18 | 600 | Figtree |
| planNow | 10/16 | 600 (tabular) | Figtree |
| planPriority | 10/14 | 500 | Figtree |
| planDayTitle | 18/26 | 400 | Fraunces |
| planCardTitle | 24/32 | 400 | Fraunces |
| planRemoteTitle | 22/28 | 400 | Fraunces |

- `tokens.json` gives day-title as 18/28; iOS and the boards use 26 (08 OQ 8: use 26).
- Time and date numerals are tabular on the boards.

### 3.13 Colour tokens (`DC/ds/chronoflow/tokens.json`, OKLCH L C H; light / dark)

| Token | Light | Dark |
|---|---|---|
| background | 0.975 0.008 250 | 0.17 0.035 272 |
| foreground | 0.22 0.05 270 | 0.94 0.015 260 |
| card | 0.995 0.004 250 | 0.21 0.04 272 |
| surface-2 (lanes) | 0.955 0.012 255 | 0.19 0.038 272 |
| primary | 0.32 0.09 272 | 0.8 0.11 60 |
| primary-foreground | 0.98 0.01 250 | 0.2 0.04 272 |
| muted | 0.94 0.012 255 | 0.25 0.04 272 |
| muted-foreground | 0.34 0.04 265 | 0.74 0.03 262 |
| border | 0.88 0.02 260 | 0.32 0.04 272 |
| dawn | 0.72 0.14 55 | 0.8 0.11 60 |
| dawn-text | 0.53 0.13 55 | = dawn |
| dawn-foreground | 0.22 0.05 45 | 0.2 0.04 272 |
| now | 0.6 0.19 30 | 0.74 0.16 35 |
| cat-sleep / -fg | 0.42 0.12 275 / 0.98 0.01 270 | 0.5 0.13 278 / 0.98 0.01 270 |
| cat-light / -fg | 0.85 0.15 85 / 0.28 0.07 60 | 0.86 0.14 88 / 0.24 0.06 60 |
| cat-moderate / -fg | 0.93 0.06 90 / 0.32 0.06 70 | 0.72 0.07 90 / 0.2 0.04 70 |
| cat-dark / -fg | 0.36 0.03 260 / 0.97 0.01 260 | 0.3 0.025 265 / 0.95 0.01 260 |
| cat-caffeine / -fg | 0.5 0.08 50 / 0.98 0.01 60 | 0.6 0.08 55 / 0.15 0.03 50 |
| cat-travel / -fg | 0.52 0.1 200 / 0.99 0.01 200 | 0.66 0.1 200 / 0.15 0.03 200 |
| sky-top / -mid / -horizon | 0.3 0.09 272 / 0.42 0.1 290 / 0.75 0.12 50 | 0.15 0.04 272 / 0.25 0.07 290 / 0.55 0.12 45 |
| sky-copy-background | background at 0.9 | background at 0.9 |

- Convert to sRGB with the 08 §3.2 algorithm; the values are in 08 §4.1.
- `bg-sky` = a vertical gradient: sky-top 0% → sky-mid 55% → sky-horizon 100%.

### 3.14 List mode (`PU/TripPage/DayListView.swift`)

- **Row:**
  - a 28 × 28 swatch;
  - the title (authStrong);
  - the interval (planListLine, monospaced digits, muted), which reads "%@ · now" while the step runs;
  - a trailing tag only for core or optional;
  - a status icon.
- Row padding 14 × 12, radius 22. Selected: 2 pt dawn outline. Optional: dashed. Marked: opacity 0.7 (`markedRow`).
- Rows are separated by 8; an ordinary row is (20 + 4 + 18) + 2 × 12 = 66 pt.
- Board: rows at least 64 (56 wide), padding 8/12, gap 12, title 16/22 500, time 14/20.
- **`ZoneChangeCard`:**
  - a map pin in a 44 dawn circle;
  - planCardTitle;
  - text plates in skyCopy on skyCopyBackground (radius 10);
  - padding 20; CfSky gradient; radius 26;
  - copy: "From here on all times are %@ time. Set your watch to %@." and "From time → Dest interval · offset".

### 3.15 End of plan and hidden steps (`TripPageComponents.swift`)

- **`EndOfPlanCard`:**
  - a moon in a 44 dawn circle;
  - "End of this plan";
  - the end-state line, or the fallback "There are no more steps in this plan.";
  - "See your return plan";
  - identifier `trip.plan.end`.
- Board: padding 20, a 48 px circle, "That's the end of this plan" in Fraunces 24/32 (OQ-T6).
- Hidden-steps button: "Some steps are hidden in your preferences."

### 3.16 Wide layout (`PU/TripPage/TripPageLayout.swift`; `reqWideTrip`)

- Applies at a window width ≥ 600 (width only, either orientation; user decision 2026-11-11, l.9–14). The list is 280 wide, or 320 from 900. The column gap is 16 and the gutter 20. There is no view toggle.
- The timeline on the left fills the space; the list on the right sits beside its own day and scrolls independently when tall (`TripWideListPosition`, l.18–28):

```text
offset = min(max(0, stickyTop − dayTop), dayHeight)
height = min(dayHeight − offset, viewport − stickyTop, 700, contentHeight)
```

- A tap opens the step sheet and outlines the selected list row in dawn.
- iOS restores the scroll position after a resize using consecutive stable display frames, and gives up after 60 ticks.
- Widths tested on iOS: 599/600/899/900/669/951.
- Board `DC/PlanWide.dc.html`:
  - **hourHeight 52** (`y = (h − sh) × 52`) (OQ-T1);
  - compact grid `minmax(0,1fr) 280px`, gap 16, height 36, padding 0 20;
  - "Steps" above the list; rows at least 56;
  - main padding `12px 20px 40px`, gap 16.

### 3.17 Dark mode

- Every colour token has a dark value.
- On iOS the drawing cache key includes appearance, and a test asserts that a dark-mode switch invalidates the cache (`TimelineRenderingTests`).
- Now-pill text: primaryForeground on iOS vs `dark:text-background` in the bundle (OQ-T14).

### 3.18 Loading, empty and error states (`TripPageView.swift`)

`TripPageState.resolve`, in order:
1. tombstoned → notFound;
2. stored → noPlan or ready;
3. missing after sync and not awaiting sync → notFound;
4. keepOffline → loading;
5. offline → remoteOffline;
6. 404 → notFound;
7. failed → server;
8. otherwise → remoteLoading.

Copy (Dutch in Appendix A):
- **Loading:** "Downloading this trip’s plan…", "Loading your plan".
- **No plan:**
  - "No plan yet";
  - "Your flights are saved. Check your answers to make the plan. Once it’s made, it’s stored on this phone and works offline.";
  - button "Check answers and make plan";
  - "Making the plan needs a connection.".
- **Remote offline:**
  - "This plan downloads when you’re online";
  - "Plans for active trips and trips in the next 7 days are always on your phone. This one is further away, so its plan opens once you’re connected.";
  - "Try again";
  - "Your other trips keep working offline.".
  - Server N may differ from 7. The copy is fixed, so flag it to the copy owner, not the app.
- **Errors:**
  - Titles: "Not found" / "Something went wrong".
  - Headings: "We can’t find that" / "We’re having trouble".
  - Bodies: "The link may be old, or the trip was deleted." / "It’s on our side, not yours, and your plans are safe. Try again in a few minutes.".
  - Buttons: "Go to my trips" / "Try again" and "Open Today" / "Open my plan for today".

### 3.19 Step sheet and legend

**StepSheet** (`PU/Steps/StepSheet.swift`; board `DC/ItemDetail.dc.html`):
- **Layout:** sheet radius 38 at the top, a 36 × 5 drag handle, padding 12/20/32, gap 16.
- **Head:** a medium swatch, the PriorityTag, and the badges "Now" (dawn), "Done" (secondary) or "Skipped" (outline).
- **Title:** Fraunces 400 24/32.
- **Interval:** 14/20 muted tabular, e.g. "07:30 → 10:30 Tokyo time · 45 min left". Keys: "{{time}} {{zone}} time", and while running "{{duration}} left" (minutes rounded up; nl "Nog {{duration}}").
- **Sections:** "Why" and "How", headings 14/20 600 uppercase 0.05em muted; body 16/24.
- **Buttons:** "Skip" (outline) and "Mark as done" in a two-column grid with gap 8. Once marked, a single "Undo".
- **Footer:** "Checking off changes your progress, never your plan.", 12/16 muted, centred.
- **Gone state:**
  - "This step is no longer in your plan";
  - "Your plan changed after you opened it, for example because you edited the trip. Your other check-offs are kept.";
  - "See today's steps".
- **Announcements:** "Done", "Skipped", "Check-off undone". Focus moves to Undo or to Done.
- The sheet is a single `.sheet(item:)` selection read live (`TripPageView.swift:91–93`), with a busy guard (`TripPageStepSheet.swift`).

**Legend:**
- The trip page's legend button opens a sheet titled **"Legend"** containing only `PriorityKey` ("Priorities" + four descriptions), with a "Done" button and medium/large detents (`TripPageView.swift:95–102`).
- The full category legend "What the steps mean" (`PU/Steps/LegendSheet.swift:66`) is opened from `I/App/AppTabs.swift:76`, not from the trip page.
- The board `DC/PlanLegend.dc.html` shows that full legend over the Plan page: groups with uppercase headings, swatch + name 16/22 500 + hint 14/20, and "How important" (OQ-T22).

### 3.20 Board vs iOS: where they differ

| Topic | iOS (parity target) | Board, RN or web | Default |
|---|---|---|---|
| Hour scale on wide | 40 pt/h everywhere | PlanWide 52 px/h; web 48 px/h | 40 dp/h (OQ-T1) |
| Lane dot | 6 pt | 8 px (board, RN Phase 25) | 6 dp (OQ-T2) |
| Prev/next day buttons | present | removed on the board | keep (OQ-T3) |
| Opening position | top of the opening day | "at the now line" (`reqContinuousPlan`, RN, web) | iOS (OQ-T4) |
| End card title | "End of this plan" | "That's the end of this plan" | iOS (OQ-T6) |
| Divider pill | "%@ time from here" | "Now on Tokyo time" | iOS (OQ-T7) |
| Tall-tile tag | any priority | bundle: "Optional" only | iOS (OQ-T8) |
| Day menu title | "Go to a day" / "Ga naar een dag" | "Go to day" / "Ga naar dag" | iOS (OQ-T9) |
| Icons | SF Symbols | lucide | lucide (08 OQ 14; OQ-T10) |
| Compact header | opaque card | glass + 0.5 hairline | iOS (OQ-T13) |
| Dark now-pill text | primaryForeground | background | iOS (OQ-T14) |
| Trip-page legend | "Legend" = PriorityKey only | full legend + priorities | iOS (OQ-T22) |

---

## 4. Interactions [F]

### 4.1 Opening the page (`TripPageState.swift`, `TripPageView.swift`)

- **`openingDay`, in order:**
  1. the requested day (deep link);
  2. the requested block's day;
  3. the kept day;
  4. the day containing now;
  5. the last day, if the trip is past;
  6. the first day.
- On appear, the page scrolls to the anchor day aligned `.top`, without animation.
- Timeline is the default view; the toggle is transient and not persisted.
- **`replacementAnchor`** (for a new plan revision), in order:
  1. the same id and date;
  2. the same `labelDate`;
  3. the nearest date (at noon UTC);
  4. the first day.

### 4.2 Scrolling, current day, subtitle and fade

- **Current day** = the last day whose top ≤ the compact height (36) from the sticky top.
- `select(dayID, offset)` changes the observable `anchor` **only when the day changes** (`TripPageModel.swift:159–164`). `scrollOffset` is `@ObservationIgnored`.
- `headerScrolled` = the anchor day's top < 0. It switches the subtitle between trip meta and "title · date".
- Compact fade: see 3.5.

### 4.3 Jumps

- The day menu, the pills and previous/next all jump to a day's top. The jump is animated unless Reduce Motion is on.
- The selected pill centres itself.
- Web reference: `scrollToDay` is smooth unless reduced motion is on, and **focuses the day heading** (`W/resources/js/lib/timeline-scroll.ts`).
- RN left "jumps do not move screen-reader focus" open (`RN/plan.md` Phase 17).

### 4.4 Taps (`DayTimelineView.swift`)

- Tap resolution, in order:
  1. tiles whose hit frame contains the point (the closest `midY` wins);
  2. markers (`|Δy| ≤ 22` and `x ≤ 44`);
  3. zone changes.
- A hit opens the step sheet for that block.

### 4.5 Check-offs (Phase 17a)

- Mark as done, Skip and Undo write optimistically to the outbox.
- `PlanDecoder.detail` merges pending checks with current-hash marks.
- On a 422 stale hash: one pull and one retry. A 429 or 408 stays queued.
- Tests cover offline → sync → re-plan with re-keying.

### 4.6 Pull to refresh (`I/plan.md` ≈l.2252)

- Native refresh control.
- A capsule (`refresh.feedback`) shows for about 2 s.
- An announcement is posted **only after a user pull**, with plurals for added, updated and removed.
- The fetch is `FullPull.fetchDetail(tripID:epoch:)`.

### 4.7 View toggle

- Phone only. It switches Timeline ⇄ List and keeps the current day.
- Not persisted.

### 4.8 Resize, fold and replacement

- Folding must keep the selected day, the scroll anchor and any open sheet (`I/plan.md` trips gaps).
- Replacement keeps the day by `labelDate` (15c).

---

## 5. Accessibility model [F → R]

**Facts (iOS)**
- **Day container:**
  - label "Plan timeline";
  - identifier `trip.timeline.preparing` or `trip.timeline.ready`;
  - `accessibilityChildren`, in this order:
    1. "Now, %@" (only the day containing now; it covers the drawn line and pill);
    2. one **Button** per block at its hit frame, with identifier `trip.block.<id>` and the 2.13 label;
    3. divider labels ("%@ time from here") at the divider pill;
    4. the visible tick labels.
- **Targets:**
  - Hit frames are ≥ 44 and clamped inside the day (`TimelineGeometry`).
  - Markers: x 0, 44 × 44.
  - Zone changes: from x 48 to the full width, height 44.
  - Tests assert targets ≥ 44 and in bounds at widths 288, 358 and 637 (`TimelineRenderingTests`).
- **Day header title** is a heading. The compact header is hidden while invisible. The pill sizes to localized text (scaled 600 tabular, including 12-hour time).
- **Step sheet:** announcements "Done", "Skipped", "Check-off undone"; focus moves to Undo or Done.
- **Dynamic Type** is capped at xxLarge at the root.

**Android mapping [R]**
- **Day container:** `Modifier.semantics { contentDescription = "Plan timeline"; isTraversalGroup = true; testTag = "trip.timeline.ready" }`. Enable `testTagsAsResourceId = true` once at the root (13 §4.3).
- **Targets:** one invisible node each, with `Role.Button`, `contentDescription`, an `onClick` action, `testTag("trip.block.<id>")`, and bounds = hit frame with a **48 dp** minimum (OQ-T5). Order them with `traversalIndex`:
  - now = −1;
  - blocks = 0…n in PreparedDay order;
  - dividers = 10,000 + j;
  - ticks = 20,000 + k.
- **Sighted taps** never go through these nodes. A single `pointerInput` per day resolves taps against the precomputed hit frames (4.4).
- **Compact header:** while invisible, `clearAndSetSemantics {}` (or `hideFromAccessibility()`, Compose ≥ 1.8), driven by a boolean that flips only at boundaries.
- **Headings:** `semantics { heading() }` on day titles.
- **List rows:** merged semantics (`mergeDescendants`), with `stateDescription` "Marked done" / "Skipped".
- **Announcements:** polite live regions. `View.announceForAccessibility` is deprecated on API 36 [verify]; use it nowhere.
- **Focus:**
  - After a check-off, move focus with a `FocusRequester` (Undo or Mark as done).
  - After a menu jump, move focus to the target day header (OQ-T24, default yes).
- **Font scale:** capped at 1.3 at the root (08 §5.4).
- **Reduce Motion:** "Remove animations" (`ANIMATOR_DURATION_SCALE == 0`) is treated as Reduce Motion (08 §12). It gives an instant fade switch and non-animated jumps.

---

## 6. Minute tick and now-line update model [F]

- **`SystemPlanClock`** (`PU/Models/SystemPlanClock.swift`):
  - yields `now`, then sleeps `60 − (epoch % 60)` s (l.35–37), so ticks land on minute boundaries;
  - also yields on `NSSystemTimeZoneDidChange`, `NSCalendarDayChanged`, `willEnterForeground` and `significantTimeChange` (l.53–55).
  - Tests use `FixedPlanClock`. The protocol is `PlanClock.values() -> AsyncStream<Date>`.
- **`TripPlanModel.dayClocks`** (l.42–56):
  - one `DayClockState` (@Observable `now` and `text`) per day;
  - only days containing now get the real `now`; the others get `distantPast` and "";
  - values are assigned only when they change (`TripDayRow.swift:104–112`).
- **`DayClockContent`** is the **only** reader of the clock. Day rows are Equatable (`TripDayRow.swift:25–33`), so static rows never invalidate.
- **Now text** is computed off-main once per tick (`PlanDecoder.nowTexts`), only for days containing now.
- **Invalidation gates** (`TimelineInvalidationTests`, all 60 days, Timeline and List modes):

  | Event | Static rows invalidated | Clock children invalidated |
  |---|---|---|
  | 120 offset changes + an anchor boundary | 0 | 0 |
  | one minute tick | 0 | 1 |
  | crossing a day boundary | 0 | 2 |

- **RN reference:** `usePlanNow` uses a setTimeout to the minute boundary plus AppState active. But `now` lived in screen state and was passed to every row (§8).

---

## 7. iOS drawing, caching and measured budgets [F] (`I/Docs/perf-timeline.md`)

### 7.1 Pipeline

1. `TripPageView` observes SQLite.
2. The `PlanDecoder` actor decodes, lays out and prepares off-main into an immutable `PreparedPlan` (revision-guarded `rebuildLayouts`, `TripPlanModel.swift:180–211`).
3. Blocks are bucketed once. Local dates are computed with Calendar components, without allocating date formatters.
4. Day rows are Equatable, with an isolated clock child per day.
5. The `TimelineRenderer` actor records each day's **static** drawing as a vector display list holding text, patterns, symbols, ticks, tiles and labelled dividers.
6. `DayTimelineView` renders through `.task(id: RenderKey)` off-main and releases the drawing `onDisappear`.
7. `Canvas(opaque: true)` strips of `max(44, viewport/2)` draw slices of it (`DayTimelineView.swift:77, 166–183`). There are no full-height RGBA day images.
8. Fallbacks: a surface2 placeholder until the drawing is ready; the list view on render failure.

### 7.2 Cache and memory

- Cache: ≤ 4 entries and ≤ 4 MiB, replacing four rasters of 13–16 MB.
- Host tests traverse all 60 days at phone and Duo widths (358 and 637 at scale 3) and check:
  - every drawing is < **512 KiB**;
  - cached bytes ≤ the limit;
  - cache identity is reused;
  - dark mode invalidates.
- **The latest drawing peak is 39,081 B.**

### 7.3 Four guarantee layers and their numbers

1. **Host budgets** (in `make quick`): cold decode + `layoutPlan` + `preparePlan`, per sample, Debug and Release alike.

   | Fixture | Budget | Latest mean (2026-11-08) | Earlier means |
   |---|---|---|---|
   | S3 | 40 ms | 19 ms | 14 / 20 |
   | large30 | 100 ms | 50 ms | 41 / 57 |
   | large60 | 200 ms | 92 ms | 83 / 99 |

2. **Optimised UI perf** (`make perf-timeline`, mandatory for every PlanUI or PlanCore change). Gates per iteration:

   | Metric | Hard | Aim | Measured |
   |---|---|---|---|
   | First 60-day timeline frame (signpost `time-to-first-timeline-frame`) | ≤ 0.8 s | ≤ 0.45 s | 17e sim 0.34–0.41 s; supplied pre-change run 0.528/0.525/0.536 s |
   | Hitch ratio | ≤ 5 ms/s | ≤ 1 ms/s | device pending (never verified) |
   | Peak physical memory | ≤ 150 MiB | ≤ 80 MiB | sim 57.9 MB |

   - `test60DayScrollEndToEnd`: **15 fixed fast upward gestures × 3 iterations**, a fresh launch each time.
   - `test60DayLastDayReachesEnd`: select day 59 in the menu, then reach `trip.plan.end` within 6 swipes.
   - The old 240-gesture traversal (about 2.7 s per gesture, about 689 s) **timed out at 600 s** and was removed.
   - App launch in the supplied run: 2.09 s.
   - The metric adapter fails on missing or non-finite metrics. An expected-failure test injects 0.81 s to prove the gate fires.
   - The hitch gate applies only where the metric is reported, i.e. on a device.
3. **Invalidation tests:** §6 table.
4. **On-device Instruments review** before release: Animation Hitches, Time Profiler and Allocations on S3 and large60; 16.67 ms (60 Hz) and 8.33 ms (120 Hz) frame budgets.

`I/plan.md:1733` once said "interval physical-memory growth ≤ 40 MiB". The perf doc replaced that with peak ≤ 150 MiB (13 §2.5).

### 7.4 Content-height math (large60)

- Timeline: ten six-day cycles = **54,000 pt**.
  - 63 outer children give 62 × 12 = 744 pt of gaps, plus 16 pt bottom padding.
  - Headers are ≈ 90 each, +24 per warning header, +14 per lane heading.
  - Total ≈ **61,224 pt**, plus the summary, day navigation and end card.
- List mode: **740 rows** (720 blocks + 20 zone cards).
  - Row gaps: (740 − 60) × 8 = 5,440.
  - Ordinary rows: 66 pt each, 47,520 pt in total.

---

## 8. Why the RN timeline was slow on Android [F]

RN shipped a native iOS day body (`modules/timeline-canvas`, commit `2d9ea94`). Its README says "Android uses the React Native body", and the Android path was never measured: `RN/docs/plan-gate.md:1353–1366` is still "pending (no Android device or SDK)".

**Per tile** (`RN/src/components/plan/TimelineBlock.tsx`, 314 lines):
- a `Pressable` with a 44 target;
- a `View`;
- an `Svg` with its own `<Defs><Pattern>`;
- Lucide SVG icons and `Text`;
- the hooks `useTranslation`, `usePlanContext` and `useId`;
- **copy formatted on every render**.

**Per day** (`RN/src/components/plan/DayTimeline.tsx:140–286`):
- a `Text` per tick, formatted during render;
- an Svg grid path per lane;
- Svg + Text dividers;
- a View + Text now line;
- a nested horizontal `ScrollView` whose `onLayout` writes state.

**Screen** (`RN/src/components/plan/TripScreen.tsx`):
- `const now = usePlanNow()` (l.148) is passed through an inline `renderItem`, so **every minute re-renders all mounted days**. React Compiler is on (`app.config.ts:101`) but does not help here.
- FlatList: `initialNumToRender 3`, `maxToRenderPerBatch 3`, `windowSize 5`, **no `getItemLayout`** for variable cells of about 1,000 pt. `scrollToIndex` therefore needs retries with an `averageItemLength` estimate.
- `scrollEventThrottle 16` with JS `onScroll` work.
- `onViewableItemsChanged` sets state.
- Sticky headers depend on the private `ScrollViewStickyHeader`.

**What helped on iOS** (`6e7cc0b`, simulator, 12 swipes):
- rasterizing the pinned-header shadow;
- one SVG path per lane grid instead of a view per line.

Dropped frames went from about 40 to about 15, and the longest stall from about 330 to 125 ms.

**Lessons [R]:**
1. No per-tile view tree.
2. No formatting or allocation during render or draw.
3. `now` must reach exactly one tiny scope.
4. Item offsets must be known: fixed per-item heights and index-based jumps.
5. No scroll work on a non-UI thread hop.
6. No screen-level state updates per scroll frame.

---

## 9. Android design [R]

### 9.1 Goals, constraints and the core decision

- **Frame deadlines:** 8.33 ms on a 120 Hz Pixel 8; 16.67 ms on a 60 Hz low-end device.
- **Steady-scroll targets:**
  - main thread ≤ 2 ms per frame;
  - ≤ 4 ms in a frame where a day enters (first draw of a prepared op list);
  - RenderThread ≤ 4 ms.
- **Invariants:**
  1. Scrolling causes **no recomposition** of day content and **no re-recording** of day drawings.
  2. A minute tick recomposes **one** tiny `NowLine`.
  3. All parsing, layout, formatting and text measurement happens **off-main** (`Dispatchers.Default`). This is iOS D26.
  4. Nothing in a draw lambda allocates.

**Decision: Compose `LazyColumn` with stable keys and content types, where each day's timeline is one item.** That item draws its static content from a precomputed, immutable op list into its own layer.

Why not a single custom View (RecyclerView or one giant View with ExploreByTouchHelper)?
- It would also work: virtual accessibility nodes are cheap and the control is total.
- But it loses direct composition with the rest of the page (M3 app bar, `PullToRefreshBox`, the pill row, sheets, the wide list column).
- It re-implements scrolling, anchoring, nested scroll and saved state.
- It needs a View/Compose interop boundary on the hottest screen.

It stays the **fallback** if the Compose gates in §10 cannot be met (alternatives in 9.18).

### 9.2 Module placement (follows 11 §6.4)

| Module | Contents |
|---|---|
| `:core:model` (JVM) | DTOs (generated), tolerant enums |
| `:core:timeline` (JVM) | PlanCore port (§0.1); `PlanPreparer`; `PreparedPlan`; the `DayDrawing` op-list builder behind a `TextMetrics` interface; geometry; accessibility targets |
| `:core:testing` (JVM) | S1–S6 JSON, the large30/large60 generator, a fixed clock, a fake `TextMetrics` |
| `:core:designsystem` | generated tokens (JVM-friendly ARGB constants for the builder), fonts, lucide `ImageVector`s and raw path data |
| `:feature:trip` (Compose) | trip page, `DayTimeline`, op-list replay, `NowLine`, compact header, list mode, wide layout, sheets |
| `:benchmark` / `:microbenchmark` | Macrobenchmarks, the baseline-profile generator, on-device builder benchmarks |

Siblings 02 and 13 call these `:feature:plan` / `:feature:timeline` / `:core:plan`. Pick one set during plan synthesis (OQ-T23; default: 11's names).

### 9.3 Data pipeline

```text
SQLDelight/Room flows: trip row, plan bytes, check marks, pending outbox checks, settings (timeFormat, hiddenCategories, language)
  → combine → mapLatest { withContext(Dispatchers.Default) { preparer.prepare(input) } }   // cancels stale work
  → revision guard (drop results whose revision ≠ latest)                                   // iOS rebuildLayouts guard
  → StateFlow<TripPageUiState> (Loading | Ready(PreparedPlan) | NoPlan | RemoteOffline | NotFound | Failed)
```

**`PlanPreparer`** (the iOS `PlanDecoder` analogue):
- Caches the latest decode **per trip**, keyed by `(tripId, inputHash, detailVersion)`, with a raw-bytes hash guard.
- **Marks are merged separately**, so a check-off rebuilds only progress and marks for the affected day, never the parse (03 §12).
- Hidden categories and `timeFormat` re-run layout and labels, not the decode.

**Formatting rules:**
- `HH:mm` is built by hand from `ZoneRules.getOffset(instant)` and epoch-second arithmetic into a reused `CharArray`. Never use `String.format` or `DateTimeFormatter` per block.
- Cache one `DateTimeFormatter` per `(pattern, locale)` for "EEE d MMM"-style labels; they are immutable and thread-safe.
- Cache `ZoneRules` per tz id.

**`PreparedPlan` / `PreparedDay` / `PreparedBlock`** are immutable Kotlin classes.
- `PreparedDay.equals` compares `(id, revision, marksRevision)` only. This is the analogue of iOS's custom `==`: cheap, so strong skipping never deep-compares lists.
- Unchanged days **keep their instance** across revisions.

**Retention:** acquire a lease on the trip detail while the page is on screen, so the sync engine's eviction skips it (iOS `retention`). Observe connectivity for the offline states.

### 9.4 The day op list (`DayDrawing`)

**The render key** (one drawing per key):

```kotlin
// :core:timeline — no Android types. One drawing per (day content, presentation inputs).
data class RenderKey(
    val dayId: String,
    val dayRevision: Int,        // layout inputs + marks of this day's blocks + hidden categories
    val widthPx: Int,            // content width actually used: max(minWidth, available)
    val density: Float,
    val text: ResolvedTextSizes, // sp → px resolved on main via LocalDensity (Android 14 non-linear scaling), capped scale 1.3
    val dark: Boolean,
    val compact: Boolean,        // window width < 640 dp → icon-only tiles
    val language: Lang,          // en | nl (tick/pill copy)
    val hour12: Boolean,         // timeFormat preference (tick labels)
)
```

**The drawing** (struct-of-arrays, no per-op objects):

```kotlin
class DayDrawing(
    val key: RenderKey,
    val widthPx: Int, val heightPx: Int,
    val op: ByteArray,            // RECT, RRECT4 (per-corner radii), CAPSULE, DOTS, HATCH, STROKE_RRECT(dash), LINE, DASH_LINE,
                                  // CIRCLE, ICON, STRIKE, TEXT, PLATE, CLIP_RRECT/RESTORE
    val opTop: FloatArray,        // sorted top of each op's bounds (binary search for clip culling if needed)
    val f: FloatArray,            // coordinates/radii/stroke widths, consumed in op order
    val color: IntArray,          // ARGB, already pre-composited for marked/optional tiles (below)
    val text: Array<String>,      // pre-ellipsized single-line runs
    val textStyle: ByteArray,     // style index: lane label, block label, interval, priority tag, divider pill, marker "i"
    val icon: ShortArray,         // lucide glyph ids + status glyphs
    val targets: List<A11yTarget>,// hit frames (48 dp min), labels, test tags, traversal order (§5)
    val hits: HitIndex,           // tiles/markers/changes for tap resolution (§4.4)
    val estimatedBytes: Int,      // Σ array sizes + strings; asserted ≤ 512 KiB
)
```

**Builder** (`buildDayDrawing(layout, prepared, key, palette, metrics: TextMetrics)`). It is pure, so it runs on `Dispatchers.Default.limitedParallelism(2)` and in JVM tests. It emits ops in the 3.7 order:
1. **No page background.** The list background already paints it, which saves one full-width fill per day per frame.
2. Lanes, then ticks. All visible tick lines of a day go into one `LINES` op.
3. Tiles, then dividers, then markers.

**Text:**
- **`TextMetrics` (Android):** one `TextPaint` per style, created **per build job** (Paint is not thread-safe). It uses:
  - the Figtree `Typeface` (loaded once at app start, off-main; 08 §5.2);
  - `textSize` from `ResolvedTextSizes`;
  - `fontFeatureSettings = "'tnum' 1"` for times;
  - `letterSpacing` from tokens.
- Ellipsize with `TextUtils.ellipsize(text, paint, maxWidth, END)`. Measure with `measureText`.
- Baseline = `lineTop + (lineHeight − (descent − ascent)) / 2 − ascent`, from `paint.fontMetrics`.
- **No StaticLayout is needed:** every canvas label is single-line (3.7, item 7).
- **No `TextMeasurer`** in the canvas: it lays out paragraphs on the main thread.
- **JVM tests** use a deterministic fake (fixed advance per character) for geometry and byte budgets, and Roborazzi for real glyphs.

**Icons:**
- Pinned lucide path data (08 §11.1) is parsed **once** into `android.graphics.Path`, at 24-unit scale.
- Replay draws each icon with `save / translate / scale(size/24) / drawPath(strokePaint) / restore`, using stroke 2 (units), round caps and round joins.
- The `no_caffeine` strike is a 2 dp line drawn after the icon.

**Patterns** (exact, resolution-independent, one op per tile):
- **Hatch:** a `LinearGradient` from `(0,0)` to `(8/√2, 8/√2)` dp, colours `[c, c, 0, 0]` at `[0, 0.25, 0.25, 1]`, `TileMode.REPEAT`, with `c` = category fg at 0.12. This is the CSS `repeating-linear-gradient(135deg, currentColor 0 2px, transparent 2px 8px)`.
- **Dots:** a `BitmapShader` of one 7 × 7 dp cell (radius 1 dp, fg at 0.16) with a local matrix that gives an exact 7 dp period, anchored at the tile origin (CSS background origin).
  - Alternative: one `drawPoints` op with round caps.
  - A unit test compares both against a reference rendering.

**Group alpha without `saveLayer`:**
- iOS draws each marked or optional tile in a transparency layer. On Android, a `saveLayer` per tile means an offscreen render pass per tile **per frame**, expensive on tiled mobile GPUs.
- Instead, **pre-composite every colour drawn inside the tile** against the lane colour: `c' = α·c + (1−α)·surface2` (α = 0.6 marked, 0.75 optional). Translucent sub-layers (pattern 0.16/0.12, core bar 0.7, tag 0.4) keep their own alpha over the pre-composited fill.
- This is algebraically exact, including antialiased edges, wherever the tile sits on plain `surface2`.
- The one difference: a tick line under a marked tile does not show through. True contrast is 0.6 × 0.4 of a faint line, about 1–2 levels in 8-bit.
- Verify with a Robolectric pixel test against a `saveLayerAlpha` reference: ≤ 2/255 per channel, except tick-line pixels under marked tiles (OQ-T26).

**Size:** a day is about 150–300 ops and about 30 strings. Expect 10–40 KB per day, against the 512 KiB ceiling.

### 9.5 Page composition (phone, timeline mode)

`Scaffold(topBar = TripTopBar)` → `PullToRefreshBox` → `Box { LazyColumn(...); CompactHeader(...) }`.
- The list viewport starts **below** the app bar, so `stickyTop = 0`.
- Content padding: bottom = navigation-bar inset + 16; horizontal = 16 dp (20 wide) + `WindowInsets.safeDrawing` horizontal.

| Item key | `contentType` | Content | Height |
|---|---|---|---|
| `meta` | meta | metadata line (not ready only) | text |
| `summary` | summary | `PlanCard` | variable |
| `days` | dayNav | prev/next + `LazyRow` of pills keyed by day id | 44 |
| `h:<dayId>` | dayHeader | `DayHeader` (collapsible) | ≈ 90 collapsed |
| `t:<dayId>` | dayTimeline | `LaneHeadings` + `DayTimeline` (static layer + `NowLine` + targets) | 14 + day height |
| `end` | end | `EndOfPlanCard` (`trip.plan.end`) | variable |
| `hidden` | hidden | hidden-steps button | 44 |

**List mode** replaces `t:<dayId>` with one item per row:
- `r:<blockId>` (contentType `listRow`, ≈ 66);
- `z:<blockId>` (`zoneCard`).

Rows are separate items so a 15-row day never composes in one frame (large60 has 740 rows).

**Spacing:**
- `verticalArrangement = spacedBy(0.dp)`.
- The 12 dp section gap and the 8 dp row gap become item top padding, so the timeline item has **no seams**.
- The index of each day's header and timeline item lives in a precomputed `TripListIndex` (dayId ↔ indices), so jumps never search.

**The `LazyListState` is created when the plan first becomes ready,** with `initialFirstVisibleItemIndex = index(h:openingDay)`. The first frame composes only the opening day and its neighbours, with no scroll animation and no retries (RN lesson 4).

### 9.6 Drawing a day (`DayTimeline`)

```kotlin
@Composable
fun DayTimeline(day: PreparedDay, key: RenderKey, cache: DayDrawingCache, clock: DayClock, onBlock: (String) -> Unit) {
    // Initial value = cache.peek(key) → cached days draw in the first frame; otherwise built off-main (iOS .task(id:)).
    val drawing = rememberDayDrawing(day, key, cache)            // State<DayDrawing?>, not read in composition
    val probe = LocalTimelineProbe.current                        // null in production
    Box(Modifier.size(key.widthDp, day.heightDp).dayContainerSemantics(day)) {
        Spacer(
            Modifier.matchParentSize()
                .graphicsLayer()                                  // own RenderNode: scrolling moves it, never re-records it
                .drawWithCache {
                    val res = TimelineResources.get(key.dark, density) // cached paints, shaders, icon paths
                    onDrawBehind {
                        probe?.drew("static", day.id)
                        val d = drawing.value                     // draw-phase read: ready → re-record this layer only
                        drawIntoCanvas { c -> if (d == null) drawPlaceholder(c.nativeCanvas, res) else d.replay(c.nativeCanvas, res) }
                    }
                }
                .pointerInput(day.id) { detectTapGestures { p -> drawing.value?.hits?.resolve(p)?.let(onBlock) } }
        )
        NowLine(clock)                                            // the only clock reader (9.7)
        TimelineTargets(drawing)                                  // semantics-only nodes (§5), recomposes once when ready
    }
}
```

- **`replay`** walks the arrays with zero allocation: preallocated `RectF`, `Path` and `float[]` scratch live in `TimelineResources`, one per theme × density, created on main.
- **`Picture` option:** the same builder can record a `android.graphics.Picture` off-main per day. `Picture` is safe to record on a worker thread; hardware `drawPicture` is supported from API 23, and minSdk is 29. The main-thread draw then becomes a single op.
  - Adopt it only if the first-draw slice in §10 exceeds 1 ms on the low-end device (OQ-T27).
  - `RenderNode` cannot be recorded off-main: it must be used on the thread it is drawn with.
- **Placeholder:** surface2 lanes only. Its frequency is a tracked counter, `cf.timeline.placeholder`, with target 0 during scripted flings on a Pixel 8.
  - Prefetch makes it rare: LazyColumn composes the next item ahead of time, and composing starts the off-main build.
  - Jumps wait up to 50 ms for the target day's drawing before scrolling.
- **Horizontal overflow** (`minWidth > available`, e.g. a 320 dp phone): wrap `LaneHeadings + DayTimeline` in `Modifier.horizontalScroll(rememberScrollState())` per day, as iOS does. The compact overlay keeps fixed lanes (minor; matches iOS).
- **Why one item per day, not iOS strips:** iOS strips bound SwiftUI Canvas raster backing stores. Compose layers are display lists, so a 1,020 dp day costs about 20–50 KB of native display list, not 11.6 MB of pixels.
  - Strips as separate lazy items would split the per-day accessibility order and the now line across items.
  - **Fallback (OQ-T27):** split a day into `s:<dayId>:<i>` items of ≤ viewport/2 only if the first draw of a day exceeds the §10 slice on the low-end device even with `Picture`.

**Cache:** `DayDrawingCache` is a thread-safe LRU.
- ≤ **4 MiB** of `estimatedBytes`; **≤ 512 KiB** per drawing (an assert in debug, a host test in CI).
- ≤ **8 entries.** Two days can be visible at once on wide screens, plus prefetch in both directions. iOS uses 4.
- Composed items hold strong references while composed and release them on disposal (iOS `onDisappear`).

### 9.7 Now line: isolated invalidation

```kotlin
@Stable class DayClock { val state = mutableStateOf<NowLine?>(null) }   // one per day, identity never changes
data class NowLine(val offsetPx: Float, val text: String)               // text computed off-main per tick

@Composable
private fun NowLine(clock: DayClock) {          // restartable + skippable; reading state here invalidates only this scope
    val now = clock.state.value ?: return
    Box(Modifier.fillMaxWidth()
        .offset { IntOffset(0, (now.offsetPx - pillHalfPx).roundToInt()) }
        .graphicsLayer()                                                // its own layer: redraw never touches the static layer
        .clearAndSetSemantics { contentDescription = nowDescription(now.text); traversalIndex = -1f }
    ) { /* 2 dp `now` line + pill: planNow 10/16 600 tnum, primaryForeground on `now`, padding 0×6, min height 16 (scaled) */ }
}
```

- **Gotcha:** read the state **inside `NowLine`**, never inside an inline lambda of `DayTimeline` such as `Box { … }` content. Inline lambdas belong to the caller's recompose scope, so the whole day would recompose.
- **The ViewModel's clock collector:**
  1. receives `now`;
  2. on `Default`, computes `(dayId → NowLine)` only for days with `start ≤ now < end`;
  3. on main, writes `dayClocks[id].state.value`, only when the value changed.
  - Other days get `null`. Crossing a day boundary writes two holders (iOS gate "2").
- **List mode:** a row's "%@ · now" suffix reads `remember { derivedStateOf { clock.state.value?.let { it.atMs in row.start until row.end } } }`. It is re-evaluated on each tick but notifies only when the running state flips.

### 9.8 Scroll-dependent values: where each is read

| Value | Changes | Read in | Per-frame cost |
|---|---|---|---|
| Current day (last day whose top ≤ 36 dp) | at boundaries | `derivedStateOf { index.currentDay(listState.layoutInfo) }` → composition of subtitle, pills, compact-header content | none between boundaries |
| `headerScrolled` (subtitle meta vs "title · date") | at boundaries | `derivedStateOf` → app-bar title composable | none |
| Compact-header alpha `clamp(1 − headerBottom/24)` | per frame near a boundary | `Modifier.graphicsLayer { alpha = … }` lambda reading `layoutInfo` | layer property only |
| Compact header in a11y tree | at boundaries | `derivedStateOf { alpha > 0 }` → `clearAndSetSemantics` toggle | none |
| Wide list column offset and height | per frame inside a day | `Modifier.layout` (placement offset; height clamps only near the day end) | placement (+ measure of one item near the end) |
| Selected pill and auto-centre | at boundaries | `LaunchedEffect(currentDay)` → `animateScrollToItem` on the pill row (instant with Reduce Motion) | none |
| Anchor `(dayId, offsetInDay)` for restoration | per frame | `snapshotFlow { isScrollInProgress }.filter { !it }` → ViewModel + `SavedStateHandle` | none |

- **Reduce Motion:** alpha = `if (headerBottom ≤ 0) 1f else 0f`.
- **App bar:** M3 `TopAppBar` with `pinnedScrollBehavior()`. Its container-colour switch is thresholded internally [verify in M3 1.4], so it does not recompose per frame.
- **No `onScroll`-style listeners** that write state per frame.

### 9.9 Hit testing and semantics overlay

- **Taps:** one `pointerInput` per day, resolved against `DayDrawing.hits` with the iOS order (4.4).
- **Semantics:** `TimelineTargets` emits one invisible `Layout` child per target with only `Modifier.semantics` (no `clickable`, no indication). About 20–40 nodes per day; slots are reused by `contentType`.
- **Always composed by default** (OQ-T25). UiAutomator, Macrobenchmark, Switch Access and Voice Access need the nodes.
  - If §10 shows > 1 ms composition per day on the low-end device, gate them on `AccessibilityManager.isEnabled` (observed as state). UiAutomation counts as enabled.

### 9.10 Day menu, pills and jumps

- **Title slot:** a clickable `Column`:
  - "IATA → IATA" (planTitle 17/22 600, 1 line);
  - a `Row` with the subtitle (planSubtitle 12/15 muted) and a 12 dp chevron.
- **Menu:**
  - `semantics { contentDescription = spokenLabel; role = Role.Button }` on the title;
  - anchors an M3 `DropdownMenu` with a header "Go to a day";
  - two-line `DropdownMenuItem`s (label + date in `text`; trailing check on the current day; `testTag("trip.dayMenu.day.$i")`).
  - Pass a `scrollState` pre-scrolled to the current day.
  - Opening composes all 60 items. Measure it; it is acceptable at ≤ 2 frames on the low-end device.
- **Jump:**

```kotlin
scope.launch {
    cache.awaitOrTimeout(key(target), 50.ms)
    if (reduceMotion) listState.scrollToItem(idx) else listState.animateScrollToItem(idx)
}
```

  After the jump, request focus on the target day header (OQ-T24).
- **Previous / Next** use the same path. Pills are a `LazyRow` keyed by day id.

### 9.11 List mode

- Rows are ordinary composables with stable `PreparedBlock` parameters.
- Text uses `Text` with precomputed strings; nothing is formatted in composition.
- Swatches are tiny `drawBehind` composables.
- `ZoneChangeCard` uses the sky gradient (`Brush.verticalGradient` with stops 0 / 0.55 / 1).
- Selected rows (wide) get a 2 dp dawn outline. The selection is a separate state read only by the row's outline modifier.

### 9.12 Wide and foldable layouts

- **Breakpoints** (window width, not layout constraints; iOS uses window width): read `LocalWindowInfo.current.containerSize` [verify API in Compose 1.12] and convert to dp.
  - < 600 → phone;
  - ≥ 600 → wide with a 280 list;
  - ≥ 900 → 320 list;
  - `compact` (icon-only tiles) = < 640.
- **Wide item** `w:<dayId>` = `Row(timeline weight 1, gap 16, list column 280/320)`.
  - The list column is a nested scrollable `Column`, or a `LazyColumn` when it has more than 12 rows.
  - It is positioned with `Modifier.layout`, using the `TripWideListPosition` math against this item's offset from `layoutInfo`.
  - No view toggle. A tap on either side opens the step sheet and outlines the list row.
- **Fold / unfold / rotate:**
  - Handle `screenSize|smallestScreenSize|screenLayout|orientation` config changes in place, so state is kept and recreation avoided (coordinate with 11).
  - Keep the anchor `(dayId, offsetInDay)` in the ViewModel and `SavedStateHandle` (process death).
  - On a width change, every `RenderKey` changes. Rebuild off-main, and restore with `listState.requestScrollToItem(index(t:dayId), offsetInDayPx)`.
  - Timeline height does not depend on width, so this restores exactly in one pass. iOS needs a 60-tick retry loop.
  - The open step sheet survives because the selection lives in the ViewModel.
- **No orientation lock** (11 OQ-5). A landscape phone ≥ 600 dp wide gets the wide layout; the list height clamps to the viewport (`min(..., viewport − stickyTop, 700, …)`).

### 9.13 Step sheet and check-offs

- **Sheet:** `ModalBottomSheet` with partially expanded / expanded (≈ the iOS medium/large detents). Content per 3.19.
- **Selection:** a single ViewModel `selectedStep: StateFlow<StepSelection?>` read live, so Done, Undo, Skip and the "gone" state update in place. A busy guard prevents double submits.
- **Check-off flow:**
  1. optimistic outbox write;
  2. the DB flow re-emits marks;
  3. `PlanPreparer` updates only that day's marks (`marksRevision`);
  4. that day's `RenderKey` changes;
  5. the day is rebuilt off-main and its layer re-records once.
- **Errors:** 422 stale → one pull and one retry; 429 and 408 stay queued (4.5).
- **Feedback:** focus moves with `FocusRequester`. "Done" / "Skipped" / "Check-off undone" go to a polite live region.

### 9.14 Pull to refresh and states

- `PullToRefreshBox(isRefreshing, onRefresh = vm::refresh)` around the list. The indicator colour follows the 08 M3 mapping.
- On completion after a **user** pull:
  - show the `refresh.feedback` capsule for about 2 s (or the app-wide feedback component, 08 OQ 12);
  - post one polite live-region announcement with the added/updated/removed plurals.
- **Skeleton:** summary 120 / day navigation 44 / timeline 380.
- State pages and copy as in 3.18.

### 9.15 Clock (Android)

```kotlin
fun systemPlanClock(context: Context, wallMs: () -> Long = System::currentTimeMillis): Flow<Long> = callbackFlow {
    val receiver = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { trySend(wallMs()) } }
    ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
        addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_DATE_CHANGED)
    }, ContextCompat.RECEIVER_NOT_EXPORTED)
    val ticker = launch {
        while (isActive) {
            val now = wallMs(); trySend(now)
            delay(60_000 - now % 60_000 + 15)  // +15 ms slack: land after the boundary, never one minute late
        }
    }
    awaitClose { ticker.cancel(); context.unregisterReceiver(receiver) }
}.conflate()
```

- Collect with `flowWithLifecycle(STARTED)`. A restart re-emits, matching the iOS "enter foreground" refresh.
- Inject a `PlanClock` interface so tests use a fixed or virtual clock (`StandardTestDispatcher`).
- Benchmark and debug builds accept a `cf.now=<ISO>` frozen clock, like RN's `EXPO_PUBLIC_MOCK_NOW`.
- `ACTION_TIME_TICK` is an alternative ticker; it works for dynamically registered receivers only. The coroutine ticker is preferred because it is testable.

### 9.16 Fonts and font scale

- Figtree Typefaces (400/500/600) are loaded once, off-main, at app start (08 §5.2).
- `ResolvedTextSizes` are computed on main with `LocalDensity`. This respects Android 14+ non-linear scaling, capped at 1.3 (08 §5.4).
- Icons scale by the same factor as the 11 sp label (iOS: 14 × fontScale).
- A font-scale change gives a new `RenderKey`, so the days rebuild.

### 9.17 Memory budget

| Item | Budget | Notes |
|---|---|---|
| `PreparedPlan` (large60) | ≤ 8 MiB retained | 720 blocks with labels; measure with a heap dump |
| `DayDrawingCache` | ≤ 4 MiB estimated, ≤ 512 KiB per day, ≤ 8 entries | expect 10–40 KB per day |
| Composed day layers | display lists only, about tens of KB each | visible + prefetch + reuse pool |
| Bitmaps | pattern cells only (< 1 KB each) | **no day bitmaps** |
| Raw plan JSON | not retained after decode | stays in the DB |
| Process | **peak RSS anon ≤ 150 MiB hard, ≤ 80 MiB aim** during the 15-fling run (13 §2.5) | also report heap max and GPU max |

### 9.18 Alternatives considered

| Option | Verdict | Reason |
|---|---|---|
| A. Composables per block, per tick and per pattern | Rejected | The RN Android design: 30–60 nodes per day, text layout per node, recomposition fan-out (§8) |
| **B. LazyColumn, one item per day, own layer, off-main op list replayed in `drawWithCache`** | **Chosen** | Compose-native; draw-only invalidation; tiny memory; testable on the JVM |
| B′. As B, but replay an off-main `Picture` | Optimisation (OQ-T27) | The main-thread draw becomes ~1 op; adopt if B's first draw is > 1 ms on the low-end device |
| C. Strips (≤ viewport/2) as separate lazy items, like iOS | Fallback | Bounds first-draw work but splits accessibility order and the now line; only if B/B′ miss the gate |
| D. Custom View / RecyclerView + `ExploreByTouchHelper` | Fallback | Full control and cheap virtual a11y nodes, but loses Compose integration and state handling; only if Compose cannot meet §10 |
| E. `rememberGraphicsLayer()` + `GraphicsLayer.record` | Not needed | Equivalent to B's layer; records on main anyway |
| F. `RenderNode` recorded off-main | Rejected | `RenderNode` must be used on the thread it is drawn with |
| G. A bitmap per day or strip | Rejected | ≈ 1,081 × 2,678 px × 4 B ≈ 11.6 MB for a 1,020 dp day at 2.625×; iOS dropped the equivalent 13–16 MB rasters |
| H. `TextMeasurer` / `drawText` in the canvas | Rejected | Main-thread paragraph layout; canvas text is single-line |
| I. Layout in composition (`remember`) | Rejected | Large60 prepare is ~92 ms on the iOS host and would block frames; iOS D26 keeps I/O and layout off-main |
| J. LazyColumn `stickyHeader` for day headers | Rejected | iOS fades a separate compact line; it does not pin the full header (3.5) |

---

## 10. Budgets and gates [R]

Wording and gate mechanics follow 13 §4.5. The timeline-specific numbers are below.

### 10.1 Layer 1: JVM host (in `make quick`)

- **`TimelinePreparationBudgetTest`:** decode + `layoutPlan` + `indexTimeline` + `prepare`. One warm-up, then **each** of 10 samples:
  - S3 ≤ 40 ms;
  - large30 ≤ 100 ms;
  - large60 ≤ 200 ms.
- **`DayDrawingBudgetTest`:** build all 60 days at the phone width (`small` 360 and `pixel8` 411 dp) and the `foldInner` width (841 dp), with the fake `TextMetrics`.
  - Per day ≤ 5 ms (max of all days).
  - ≤ 150 ms per width in total.
  - `estimatedBytes` ≤ 512 KiB per day.
  - The cache never exceeds 4 MiB or 8 entries.
  - Cache identity is reused for equal keys; changing dark, width or font scale misses.
  - These limits are provisional. Re-set them to about 2× the measured means after the first measurement (the iOS practice).
- **Geometry and a11y:**
  - all targets ≥ 48 dp and inside bounds at content widths 288/358/637 dp;
  - the large60 total height is 54,000 dp;
  - the six-day cycle heights match;
  - 740 list rows.
- **Compact rule:** no `TEXT` op inside tile bounds at window widths < 640 or for tiny tiles (the iOS alpha test, done on data).

### 10.2 Layer 2: invalidation (Robolectric, native graphics)

A production `staticCompositionLocalOf<TimelineProbe?> { null }` counts compositions and **static draw executions** per day key. Over all 60 days, in Timeline and List modes, assert:

| Event | Static recompositions | Static re-draws | `NowLine` recompositions |
|---|---|---|---|
| 120 scroll offsets + one current-day boundary | 0 | 0 (days newly entering may draw once) | 0 |
| one minute tick | 0 | 0 | 1 |
| a day boundary tick | 0 | 0 | 2 |
| a check-off on day k | only day k | only day k | 0 |
| positive control: anchor change | observed by subtitle, pills, compact header | – | – |

### 10.3 Layer 3: Macrobenchmark (`make perf-timeline`, `benchmark` build type)

- `timelineFirstFrame`:
  - `startActivityAndWait` with extra `cf.trip-page=large60`;
  - an async trace section `cf.timeline.firstFrame` from trip-page ViewModel start to two frames after the first real (non-placeholder) day draw;
  - `TraceSectionMetric` [verify async-slice support] + `StartupTimingMetric`; 3 iterations.
- `timelineScroll`:
  - setup: launch and wait for `trip.timeline.ready`;
  - measured: **exactly 15 flings** on `trip.page` with a gesture margin of width/5;
  - `FrameTimingMetric` + `MemoryUsageMetric(Max)`;
  - 3 iterations, each a fresh launch;
  - `CompilationMode.Partial(BaselineProfileMode.Require)`;
  - after measuring, assert that the day label advanced.
- `timelineLastDay` (functional): open the menu, select `trip.dayMenu.day.59`, reach `trip.plan.end` within ≤ 6 flings.
- **Never** run a traversal of hundreds of swipes (iOS incident).

**Gates.** The hitch ratio is Σ positive `frameOverrunMs` ÷ scroll seconds, and jank % is the share of frames with overrun > 0. A results checker computes both from the per-frame `runs` in `*-benchmarkData.json` (13 §4.5).

| Metric | Pixel 8, physical (120 Hz, 8.33 ms) hard / aim | Low-end reference (60 Hz, 16.67 ms) hard / aim | Emulator (Pixel 8 AVD) |
|---|---|---|---|
| First large60 timeline frame | ≤ 800 ms / ≤ 450 ms | ≤ 1,500 ms / ≤ 800 ms | ≤ 800 ms (functional gate) |
| Hitch ratio | ≤ 5 ms/s / ≤ 1 ms/s | ≤ 5 ms/s / ≤ 2 ms/s | report only |
| `frameOverrunMs` P99 | ≤ 0 (13) | ≤ 4 ms / ≤ 0 | report only |
| `frameDurationCpuMs` P50 / P90 / P99 | aim ≤ 3 / 5 / 8 ms | aim ≤ 6 / 10 / 16 ms | trend ±25% flagged |
| Jank % | ≤ 3% / ≤ 1% | ≤ 5% / ≤ 3% | report only |
| Peak `memoryRssAnonMaxKb` | ≤ 150 MiB / ≤ 80 MiB | ≤ 150 MiB / ≤ 100 MiB | ≤ 150 MiB |
| Placeholder frames (`cf.timeline.placeholder`) | ≤ 2 / 0 | ≤ 6 / ≤ 2 | report only |

**Notes:**
- `MemoryUsageMetric` and `TraceSectionMetric` need `@OptIn(ExperimentalMetricApi::class)`.
- On the emulator, set `androidx.benchmark.suppressErrors=EMULATOR` and use `-gpu host`.
- **Perf runs must keep animator scales at 1.** With "Remove animations" on, Compose flings and animations end instantly and the scroll metrics become meaningless. The headless functional script in 11 §10.3 sets the scales to 0; reset them before perf runs.
- The low-end reference device is unknown (OQ-T15). Until one exists, low-end numbers are "open: device only".

### 10.4 Layer 4: manual device review (before release)

A Perfetto review of S3 and large60 on the Pixel 8 and the low-end device (protocol in §11). Also:
- a Memory Profiler heap dump after the 15 flings: no day drawings beyond the cache; no bitmaps other than pattern cells;
- a TalkBack pass over one day: order, labels, the Now element, jump focus.

### 10.5 Baseline profiles and JankStats

- **Baseline profile** (`:benchmark`, `BaselineProfileRule`):
  - startup journey (`includeInStartupProfile = true`): launch → Today;
  - trip journey: My trips → S3 → 5 flings down and up → list toggle → scroll → toggle back → day menu → day 59 → open a step sheet → dismiss;
  - plus `cf.trip-page=large60` → 15 flings.
  - Commit the generated profile. Regenerate it when the PlanCore port, the builder, the replay or the page structure change. The benchmark's `BaselineProfileMode.Require` makes a missing profile fail.
- **JankStats** (`androidx.metrics:metrics-performance` 1.0.0) in **benchmark and debug** builds only:
  - `JankStats.createAndTrack(window)`;
  - state `PerformanceMetricsState.putState("timeline", "day=$i;mode=$mode")`;
  - log jank frames to logcat for diagnosis.
  - Never in release.

---

## 11. Profiling protocol [R]

1. **Build:** the `benchmark` build type (release-like, R8, non-debuggable, `<profileable android:shell="true"/>`). Install it with its baseline profile, via the Macrobenchmark install or `./gradlew :app:installBenchmark` plus profile installation.
2. **Prepare the device:**
   - airplane mode; battery saver off; charged above 50%; cooled;
   - animator scales **1**; font scale 1.0 (plus one run at 1.3);
   - fixed theme per run (light, then dark);
   - refresh rate recorded (`adb shell dumpsys display | grep -i refresh`).
3. **Run** `./gradlew :benchmark:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=…TimelineBenchmark`.
   - Results: `*-benchmarkData.json`; traces under `build/outputs/connected_android_test_additional_output/…`.
   - The results checker prints every raw value as `Timeline metric: …` and fails on missing, non-finite or over-budget values (13 §4.5).
4. **Composition tracing:** add `androidx.compose.runtime:runtime-tracing` (BOM-managed) to the benchmark and debug variants only. Run with `-P android.testInstrumentationRunnerArguments.androidx.benchmark.fullTracing.enable=true`, so traces show composable names.
5. **Read the Perfetto trace** (ui.perfetto.dev):
   - main thread: `Choreographer#doFrame` → `Recomposer:recompose` (should be **absent** during steady flings), `compose:lazy:prefetch`, measure/layout, `draw`;
   - custom sections: `cf.timeline.build`, `cf.timeline.replay`, `cf.timeline.firstFrame`, `cf.timeline.placeholder`;
   - RenderThread `DrawFrame`;
   - the Expected/Actual frame-timeline tracks for janky frames (API 31+);
   - GC events (`HeapTaskDaemon`) during flings: should be none.
6. **Quick checks without Macrobenchmark:**
   - `adb shell dumpsys gfxinfo <pkg> reset`, run the scripted flings, then `dumpsys gfxinfo <pkg>` (janky %, P50/P90/P95/P99) and `dumpsys gfxinfo <pkg> framestats`;
   - `adb shell dumpsys meminfo <pkg>` (Java and native heap, Graphics, TOTAL RSS);
   - optionally `debug.hwui.overdraw show` to confirm the day layer does not repaint the page background.
7. **Recomposition counts:**
   - automated by the §10.2 probe tests;
   - manually, Layout Inspector "show recomposition counts" on a debug build while flinging (day items must stay at 0 / skip);
   - compiler reports (`-PcomposeReports=true`, 11 §6.3) confirm that `DayHeader`, `DayTimeline`, `NowLine`, list rows and pills are restartable and skippable with stable parameters.
8. **Record** every run in `docs/perf-timeline.md`: date, commit, device, OS, refresh rate, font scale, theme, build type, the raw metrics, and links to the traces. Mirror the iOS doc's "latest means" table.

---

## 12. Fixtures [F → R]

**S1–S6** [F]:
- The JSON originals are `RN/src/data/mock/fixtures/plans/S1.json` … `S6.json`, plus `S6-RETURN-10N.json`: 28,090–185,066 B, in exactly the API detail shape.
- iOS embedded them as Swift literals (`MockAppSeed+Fixtures.swift`, about 695 KB).
- [R] Copy the JSON into `:core:testing` resources and decode it with the production decoder.

**large30 / large60** (`MockTimelineFixtures.swift`) [F]:
1. Take S3 (`templates[2]`). Use its **first six days** as the cycle: `duration` = cycle end − cycle start.
2. Keep the source blocks that overlap the cycle.
3. Clone the cycle `count / 6` times:
   - suffix `-tour-<i>` on day and block ids;
   - shift every ISO instant by `i × duration`, and 10-character dates via `T12:00:00Z`;
   - clamp block start and end to the shifted cycle;
   - copy the timeline days, blockReasons, transferWarnings and notices for the cloned ids.
4. Caffeine overlay from S1 (`templates[0]`):
   - take the first `post`-phase day with ≥ 3 caffeine-lane blocks starting inside it;
   - for every result day `i`, shift that day's caffeine blocks to the day's start with suffix `-caffeine-day-<i>`;
   - clamp start and end to the day end;
   - set `displayTz = day.zones.first.tz`.
5. Identity:
   - trip id `0199a3c0-0000-7000-8000-0000000000<count>`;
   - hash = SHA-256 hex of `"timeline-<count>-S3-prefix6-S1-caffeine-v2"`, used for `meta.inputHash`, `checkState.planInputHash` and `detailVersion`;
   - `trip.endUtc` = the last day's end;
   - `meta.assumedReturn = nil`;
   - check state and marks emptied.

**Invariants** (`LargeTimelineFixtureTests`) [F]:
- day counts are right and ids unique;
- ≥ 3 zones, and ≥ count/6 zone changes;
- the sleep, light and caffeine lanes each have ≥ count blocks;
- at least one overnight flight;
- days are contiguous;
- all copy keys resolve;
- 60-day heights total **54,000**;
- the last label is "Day 2 · Tue 8 Dec" (the perf doc says "Day 2.0"; OQ-T19).

[R] Port the generator to Kotlin in `:core:testing` with the same suffixes and hash string, so ids and hashes match iOS byte for byte. Assert the same invariants.

**Seeded launch** [R]: in debug and benchmark builds, an intent extra `cf.trip-page=S1…S6|large30|large60|<uuid>` opens an isolated trip page backed by mock data (iOS `-cf.trip-page`; 02/13). Also `cf.now=<ISO>` for a frozen clock.

---

## 13. Implementation order and test plan [R]

1. **PlanCore port** (`:core:timeline`):
   - DTO decode with tolerant enums;
   - bucketing, `layoutDay`, `packLane`, ticks, zones, now, geometry, labels, check state, prepared model;
   - JVM tests ported from iOS (DST days, intl goldens, fixtures, budgets 10.1).
2. **Op-list builder and replay:**
   - `DayDrawing`; Android `TextMetrics`; lucide paths; pattern shaders; pre-composited group alpha;
   - Roborazzi screenshots of S3 days at 360/411/639/640/841 dp in light and dark, en and nl, at font scale 1.0 and 1.3;
   - the `saveLayer` reference pixel test.
3. **Page:**
   - Scaffold, app bar, menu, LazyColumn items, headers, pills, summary, end card, states, compact header;
   - scroll anchoring and restoration;
   - invalidation tests (10.2).
4. **Clock and `NowLine`;** list mode; wide layout; fold restore tests (fake `WindowLayoutInfo`, 13 §4.4).
5. **Step sheet, check-offs** (outbox, 422/429/408 behaviour), legend, pull to refresh, announcements and focus.
6. **Macrobenchmarks,** results checker, baseline profile, JankStats; the first device numbers recorded in `docs/perf-timeline.md`; then reset provisional limits.

---

## 14. Open questions (the plan proceeds on the default)

| # | Question | Default |
|---|---|---|
| OQ-T1 | Hour scale on wide layouts: PlanWide board 52 px/h, iOS 40 everywhere, web 48 | **40 dp/h everywhere** (iOS parity; heights stay width-independent, which makes fold restoration exact) |
| OQ-T2 | Lane heading dot: iOS 6 pt (`dot`), board and RN 8 | **6 dp** (iOS, generated token) |
| OQ-T3 | DayNavigation previous/next buttons: on iOS, removed in the board's Done note | **Keep** (iOS parity) |
| OQ-T4 | Opening position: iOS = top of the opening day; board, RN and web = at the now line | **iOS: top of the opening day**, no animation |
| OQ-T5 | Minimum target 44 (iOS) or 48 dp (Android) | **48 dp invisible targets**, visuals unchanged (08 §12) |
| OQ-T6 | End card title: "End of this plan" (iOS) or "That's the end of this plan" (board) | **iOS catalog** |
| OQ-T7 | Divider pill: "%@ time from here" (iOS) or "Now on Tokyo time" (board) | **iOS** |
| OQ-T8 | Tall-tile tag: any priority (iOS) or only "Optional" (bundle) | **iOS** |
| OQ-T9 | Day menu title: "Go to a day" / "Ga naar een dag" (iOS) or "Go to day" / "Ga naar dag" (board) | **iOS catalog** |
| OQ-T10 | Icon set: SF Symbols mapping on iOS; lucide on boards and RN | **lucide** for categories (08 OQ 14); accept glyph differences (e.g. Glasses vs eyeglasses, Footprints vs figure.walk) |
| OQ-T11 | Font-scale cap | **1.3** at the root (08 OQ 5) |
| OQ-T12 | Orientation | **No lock;** width-only breakpoints 600/640/900 dp (11 OQ-5) |
| OQ-T13 | Compact header surface: opaque card (iOS) or glass + hairline (board) | **Opaque `card`** (iOS); no blur |
| OQ-T14 | Now-pill text in dark: primaryForeground (iOS) or background (bundle) | **primaryForeground** (iOS) |
| OQ-T15 | Low-end reference device | **A 60 Hz, 4 GB, Cortex-A76-class phone** (e.g. Galaxy A15/A16 or Pixel 4a class). Until available, low-end gates are "open: device only" and emulator numbers are trend-only |
| OQ-T16 | Memory metric: peak or growth | **Peak RSS anon ≤ 150 MiB hard / ≤ 80 MiB aim** (13 OQ-T5); growth reported only |
| OQ-T17 | Bottom navigation minimise on scroll | **No** (RN decision 40: Android keeps its full bar) |
| OQ-T18 | Persist the Timeline/List choice | **No** (iOS) |
| OQ-T19 | Last-day label in the large fixtures: "Day 2" or "Day 2.0" | **Assert the iOS test string "Day 2 · Tue 8 Dec"** |
| OQ-T20 | Pull-to-refresh feedback component | **A capsule `refresh.feedback` for ~2 s,** or the app-wide feedback component once 08 OQ 12 is settled; polite announcement after user pulls only |
| OQ-T21 | Add to calendar in Trip actions | **Omitted** (iOS OQ 42) |
| OQ-T22 | Trip-page legend: iOS "Legend" sheet with PriorityKey only; board shows the full "What the steps mean" legend | **iOS parity** (PriorityKey sheet). The full `LegendSheet` exists anyway for Today, so switching later is trivial |
| OQ-T23 | Module names differ across 02, 11 and 13 | **11's names** (`:core:timeline`, `:feature:trip`) |
| OQ-T24 | Move screen-reader focus to the day header after a jump (web does; RN left it open; iOS unverified) | **Yes** |
| OQ-T25 | Accessibility target nodes always composed, or only when a service is on | **Always**; gate them only if §10 shows > 1 ms composition per day on the low-end device |
| OQ-T26 | Group alpha by pre-compositing or `saveLayer` | **Pre-composite;** accept faint tick lines vanishing under marked tiles; revisit if the designer objects |
| OQ-T27 | `Picture` playback and/or strips | **Neither initially;** add `Picture` (B′) if first draw > 1 ms on the low-end device, and strips (C) only if that still misses |
| OQ-T28 | Selected-step outline on wide: list row only, or tile too | **List row only** (iOS draws it in `DayListView`) |
| OQ-T29 | Dot-pattern phase: tile origin or lane origin | **Tile origin** (CSS background origin); confirm against an iOS screenshot |

---

## 15. Risks

1. **Layer boundaries:** if the static drawing is not in its own layer, or reads scroll state, every scroll frame re-records day content. Guard: the §10.2 draw-count probe; a Perfetto check that `Recomposer:recompose` and `cf.timeline.replay` are absent during steady flings.
2. **Font metric drift** between CoreText and Android Minikin: baselines, ellipsis points, and Dutch strings that run about 20% longer. Guard: Roborazzi goldens and a side-by-side review with iOS screenshots at matching widths.
3. **Group-alpha emulation** is exact except over tick lines (OQ-T26). Guard: the pixel test against a `saveLayer` reference.
4. **Anchor precision** across plan replacement, fold, rotation and process death. Guard: restore by `(dayId, offsetInDay)` with `requestScrollToItem` and the replacement-anchor rules; Robolectric resize tests at 599/600/899/900 dp.
5. **Semantics overlay cost** and TalkBack order across lazy items (`traversalIndex` behaviour). Guard: measure composition cost; a TalkBack walkthrough in the device review.
6. **No physical devices:** the emulator uses software or host GL at 60 Hz; there is no thermal model and no 120 Hz. Numbers there are not device claims (OQ-T15).
7. **The 120 Hz Pixel 8** leaves an 8.33 ms deadline. Any allocation or GC in draw, or main-thread formatting, shows up. Guard: no allocation in `replay`; formatting off-main.
8. **Animator scale 0** (used in functional emulator runs) silently invalidates perf metrics. Guard: the benchmark asserts `ANIMATOR_DURATION_SCALE == 1` in setup.
9. **Horizontal overflow** on 320 dp phones: a nested horizontal scroll can conflict with vertical flings and system back-gesture edges.
10. **tz data:** device tzdata (Mainline-updated on API 29+) may differ from the server's tzdb for display offsets. Day boundaries come from server UTC instants, so the risk is display only.
11. **Copy plurals and numbered ranges** (Laravel `|` plurals) must map to Android plurals (10).
12. **Backend branch drift:** the API branch is unmerged; contract details may move (03).
13. **M3 internals** may recompose more than expected: app-bar colour transitions, `PullToRefreshBox`, and a `DropdownMenu` with 60 two-line items. Guard: Perfetto plus the probe on the chrome scopes.
14. **Baseline-profile staleness** brings back JIT jank on first scroll. Guard: `BaselineProfileMode.Require` and regeneration on hot-path changes.
15. **Config-change recreation** on fold, if not handled in place, adds a frame gap and must not lose the anchor or the sheet.
16. **`TraceSectionMetric` and async sections:** if async slices are not supported, the first-frame section must be emitted on one thread [verify on benchmark 1.5].

---

## Appendix A — copy keys (iOS catalogs, en / nl) [F]

Sources: `PU/Resources/Localizable.xcstrings`, `PU/Resources/Screens.xcstrings`, `PC/Resources/PlanCopy.xcstrings`. On Android, iOS `%@` / `%lld` become `%1$s` / `%1$d`, and plural entries become `<plurals>` (10).

| Key (en) | nl |
|---|---|
| Plan timeline | Tijdlijn van het plan |
| Now, %@ | Nu, %@ |
| Go to a day | Ga naar een dag |
| %@ to %@, %@, %@. Go to a day | %@ naar %@, %@, %@. Ga naar een dag |
| %@ to %@, %@. Go to a day | %@ naar %@, %@. Ga naar een dag |
| Show as list | Als lijst tonen |
| Show as timeline | Als tijdlijn tonen |
| Trip actions | Reisacties |
| Your plan | Je plan |
| Legend | Legenda |
| Legend: what the colours and patterns mean | Legenda: wat de kleuren en patronen betekenen |
| What the steps mean | Wat de stappen betekenen |
| Done | Klaar |
| Previous day / Next day | Vorige dag / Volgende dag |
| Plan days | Plandagen |
| Show more / Show less | Meer tonen / Minder tonen |
| %lld warning / %lld warnings | %lld waarschuwing / %lld waarschuwingen |
| %lld note / %lld notes | %lld notitie / %lld notities |
| %@ of %@ steps done | %@ van %@ stappen gedaan |
| %@ time from here | Vanaf hier %@-tijd |
| From here on all times are %@ time. Set your watch to %@. | Vanaf hier zijn alle tijden in de tijdzone van %@. Zet je horloge op %@. |
| Continues from the previous plan day | Loopt door vanaf de vorige plandag |
| Continues into the next plan day | Loopt door in de volgende plandag |
| Marked done | Als gedaan gemarkeerd |
| Skipped | Overgeslagen |
| Sleep / Light / Caffeine / Travel | Slapen / Licht / Cafeïne / Reizen |
| Steps | Stappen |
| %@ · now | %@ · nu |
| End of this plan | Einde van dit plan |
| There are no more steps in this plan. | Er zijn geen stappen meer in dit plan. |
| See your return plan | Bekijk je terugreisplan |
| Some steps are hidden in your preferences. | Sommige stappen zijn verborgen in je voorkeuren. |
| Downloading this trip’s plan… | Het plan van deze reis wordt gedownload… |
| Loading your plan | Je plan laden |
| Mark as done / Skip / Undo | Markeer als gedaan / Overslaan / Ongedaan maken |
| Checking off changes your progress, never your plan. | Afvinken verandert je voortgang, nooit je plan. |
| Why / How / Now | Waarom / Hoe / Nu |
| {{duration}} left | Nog {{duration}} |

- The board-only "Choose a day" hint ("Kies een dag") is **not** in the iOS catalogs. iOS uses the spoken title label above.
- Board Dutch from `planCompactQuestions` that differs from iOS: "Ga naar dag" (iOS "Ga naar een dag").
- Dutch for the offline states (`reqTripOffline`): "Het plan van deze reis wordt gedownload…", "Dit plan wordt gedownload zodra je online bent", "Opnieuw proberen", "Je andere reizen werken gewoon offline."
