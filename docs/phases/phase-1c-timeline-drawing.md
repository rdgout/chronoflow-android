# Phase 1c: DayDrawing builder and replay

- **Branch:** `feat/phase-1c-timeline-drawing`, from the stack top once 1b and 0b are in it. 0e is in an earlier wave and must also be in the stack, because the Robolectric tests need its tier annotations (`@Logic`, `@Layout`, `@Capture`), `@PerfBudget`, `BoardCapture` and the JDK 21 test launcher. If 0e is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 1c: DayDrawing builder and replay" (l.1090–1130). Decisions **A3** (module graph), **A9** (tokens, fonts, icons), **A12** (timeline architecture) and **A19** (tests). Performance budget rows "`DayDrawing` build" and "Drawing cache" (l.522–523). Trade-off row `DayDrawingCache` (l.198: 8 entries, not iOS's 4).
- **Tests:** every test follows AGENTS.md "Tests": use the cheapest tier, respect the budgets, never use `Thread.sleep`, and stay under the hang caps. Read that section before you write a test.
- **Tags:** **[F]** is a fact verified in source, with the path given. **[R]** is a rule this brief sets for Android. `I/` stands for `/Users/rickgout/Sites/chronoflow-ios` read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`). `DC/` stands for `_design/canvas/`. `PU/` = `I/Packages/ChronoFlowKit/Sources/PlanUI/`, `PC/` = `I/Packages/ChronoFlowKit/Sources/PlanCore/`.
- **Revision:** 2026-11-16, third pass: re-checked line by line against `TimelineRenderer.swift`, `PlanAppearance.swift`, `DayTimelineView.swift`, `PlanMetrics.generated.swift`, `TimelineAlphaTests.swift` and `TimelineRenderingTests.swift` at `f391a1fa`, the `Plan` board and `bundle.css`, and the current 0b, 0e, 1b, 1d and Phase 2 briefs (including Phase 2's Konsist rules). §11 lists what changed.

---

## 1. Goal

Build each timeline day's **static** content as an immutable, struct-of-arrays op list, `DayDrawing`. A pure JVM builder in `:core:plan` produces it. It draws exactly what iOS `TimelineRenderer` draws, in the same order, with the same category styles and tile content rules. The builder also produces the day's `HitIndex` (tap resolution) and its `TimelineTarget` list (accessibility). On the Android side the phase adds:
- `TextPaintMetrics`, the real text measurement;
- `TimelineResources`, the shared paints, pattern shaders and lucide glyph paths, created once;
- `replayDayDrawing`, a zero-allocation walk of the op list onto `android.graphics.Canvas`;
- `DayDrawingCache`, a bounded LRU.

The phase has no UI host. The LazyColumn, `NowLine`, clock, semantics nodes and `pointerInput` belong to 1d.

**Done means:**
- the JVM build budgets hold: ≤ 5 ms per day, ≤ 150 ms per width for large60, ≤ 512 KiB per drawing;
- the cache never exceeds 4 MiB or 8 entries;
- the Robolectric replay guards pass in light and dark, en and nl, at font scale 1.0 and 1.3;
- the S3 day-1 golden op list has been reviewed against the `Plan` and `PlanDark` boards.

## 2. Inputs to read (in this order)

1. `plan.md`: A3, A9, A12, A19, A21; "Performance budget"; Phase 1b (what you build on), 1c (this phase) and 1d (what consumes your API).
2. `AGENTS.md` "Tests", "Performance" and "Traps".
3. Sibling briefs (contracts with neighbours):
   - `docs/phases/phase-1b-timeline-layout.md` Task 1 (`PlanMetrics.minTouchTarget`), Task 4 (`TimelineRect`, `TimelineGeometry`, `timelineBlockFrame`), Task 7/8 (`PreparedDay`, `PreparedBlock`, `PreparedPlan.revision`, stable day identity), Task 10 (`LargeTimelineFixtures`);
   - `docs/phases/phase-1d-timeline-host.md` §2 rows P5–P7 (**the exact API 1d calls; do not rename anything listed there**), Task 4 (repository) and Task 5 (`DayTimeline`);
   - `docs/phases/phase-0b-tokens.md` §5 table (`TimelinePalette`, `PlanMetrics`, fonts);
   - `docs/phases/phase-0e-test-harness.md` §5 (tiers), T9 (`BoardCapture`);
   - `docs/phases/phase-2-design-system.md` Task 10 (the lucide generator that Task 8 reuses) and Task 17 (the Konsist rules `noRawColorsOutsideDesignSystem` and `featureDpLiteralsComeFromBoardMetrics`, which apply to your `:feature:trip` main code).
4. `docs/research/04-timeline.md`: §2.6–§2.13 (layout you consume), §3.7 (draw spec), §3.8 (category styles), §3.9 (tile content rules), §3.11 (metrics), §3.13 (colors), §3.17 (dark mode), §3.20 (board vs iOS), §5 (accessibility), §7 (iOS pipeline and budgets), §9.4 (op list), §9.6 (replay and cache), §9.16 (fonts), §9.17 (memory budget), §10.1 (JVM gates), §14 (OQ-T5, T10, T26, T27, T29).
5. `docs/research/08-design-system.md`: §4.1 (the sRGB token table, your color golden); §5.2–§5.4 (fonts, font-scale cap); §11.1 (lucide).
6. **iOS sources**, read at `f391a1fa`. These are the behavior reference: mirror their logic and do not port their rendering technology (PDF, CoreText, SF Symbols).
   - `PU/TripPage/TimelineRenderer.swift`, all 462 lines. **This is the spec for the builder**, and §5 below transcribes it with line numbers.
   - `PU/TripPage/PlanAppearance.swift`: `PlanCategoryStyle` (l.6–47) and `PlanPattern` (l.70–91).
   - `PU/TripPage/DayTimelineView.swift`: tap resolution (l.102–113), accessibility children (l.117–158), `RenderKey` (l.189–197).
   - `PC/PlanMetrics.generated.swift`: every metric used below.
   - `PC/TimelineGeometry.swift` and `PC/TimelineAccessibilityGeometry.swift`. 1b ported both; reuse that port.
   - `I/Packages/ChronoFlowKit/Tests/PlanUITests/TimelineAlphaTests.swift` (l.11–132) and `TimelineRenderingTests.swift` (l.14–52), ported in Tasks 4, 6 and 11.
   - `I/plan.md:1705` "15d: Timeline drawing", including its "Implementation decisions" and "15 integration fixes" paragraphs.
7. **Boards**, compared visually; nothing is built from their DOM:
   - `DC/Plan.dc.html`: tile `ChronoFlow.TimelineBlock` (l.143), zone pill (l.147: left 48, padding 2 × 8, 10/14 500, `cat-travel`/`cat-travel-fg`), data script (l.216–379): tick labels (l.307: `right: 4px`, 11/14, `muted-foreground`, tabular), grid lines (l.309: 1 px `border` at 60%), lanes (l.311: `surface-2`), zone line (l.348: `2px dotted var(--cat-travel)`);
   - `DC/PlanDark.dc.html`: a wrapper for `Plan` in dark (390 × 844);
   - `DC/PlanTimelineTop.dc.html`: a wrapper for `Plan` from the top of day 0, 390 × 2600;
   - `DC/PlanLegend.dc.html`: the category swatches (pattern, icon and fill per category), used for visual checks only. Do not build the legend sheet; it belongs to 6d.
   - `DC/ds/chronoflow/components/bundle.js`: `TimelineBlock` (`function qe`), `categoryStyles` (`le={…}`), and the lucide icon node arrays (`var X=[[…]],Y=r("<name>",X)`), lucide-react **0.511.0**;
   - `DC/ds/chronoflow/components/bundle.css`: `.pattern-dots` = `radial-gradient(currentColor 1px, transparent 1.5px)` at 16%, `background-size: 7px 7px`; `.pattern-hatch` = `repeating-linear-gradient(135deg, currentColor 0 2px, transparent 2px 8px)` at 12%.
8. **iOS screenshots** for review: `docs/research/screens/ios/04-trip-timeline-top.png` (light), `30-trip-timeline-dark.png` and `34-trip-timeline-nl.png`.

**API endpoints:** none. This phase does no I/O and no networking.

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| `TimelinePage`, `LazyColumn`, `DayTimeline` composable, `drawWithCache`/`graphicsLayer` host, placeholder drawing, `pointerInput`, semantics nodes, `TimelineDrawingRepository`, prefetch, `NowLine`, clock, `TimelineProbe`, perf entry, trace sections | 1d |
| Now line and pill ("Now, %@") | 1d (not part of the static drawing) |
| Lane headings, day header, compact header | 1d (plain) / 6b (designed) |
| Macrobenchmarks, baseline profile, the `Picture` (B′) and strips (C) fallbacks | 1e |
| Step sheet, legend sheet (`PlanLegend`) | 6d |
| List mode | 6c |
| Wide/foldable layout, including the wide content width | 6e. For budget tests, use content width = window − 32 dp (§6). |
| The lucide generator itself, Lucide `ImageVector`s (`Lucide.kt`), the other bundle glyphs, board-sourced glyphs, Material Symbols | Phase 2 Task 10. Task 8 only checks that the 16 timeline names are generated; it creates a subset generator only if Phase 2 is not in the stack |
| New copy | none: every string drawn comes from 1b's `PreparedDay`/`PreparedBlock`. The marker glyph `"i"` is a constant, not copy (§5 step 6). |

## 4. What 1b and 0b give you (verify before Task 1)

Run these checks on the stack top and list the results in your first commit message body. If any check fails, stop and report it. Do not re-implement 1b.

| Need | Location | Swift origin |
|---|---|---|
| `object PlanMetrics` (`const val … : Double`, dp), package `net.chronoflow.core.plan.generated`, with every value of the Swift file (0b `MetricsGoldenTest`) **plus** `minTouchTarget = 48.0` (1b Task 1 overlay). `touch` stays 44.0 and is **not** read by 1c. | `core/plan/src/main/kotlin/net/chronoflow/core/plan/generated/PlanMetrics.generated.kt` | `PC/PlanMetrics.generated.swift` |
| `class TimelinePalette(…52 Int ARGB fields)` with `companion object { val Light; val Dark }`, fields camelCase (`background`, `foreground`, `card`, `cardForeground`, `surface2`, `border`, `mutedForeground`, `catSleep`, `catSleepFg`, `catLight`, `catLightFg`, `catModerate`, `catModerateFg`, `catDark`, `catDarkFg`, `catCaffeine`, `catCaffeineFg`, `catTravel`, `catTravelFg`, …) | `core/plan/…/plan/generated/TimelinePalette.generated.kt` (0b) | `08 §4.1` |
| `DayLayout(day, height, minWidth, placed: Map<Lane, List<PlacedBlock>>, markers, changes, dividers, ticks)`; `PlacedBlock(block, top, height, exactTop, tiny, col, cols, continuesBefore, continuesAfter, presentation)`; `PlacedMarker(block, exactTop, top, presentation)`; `ZoneChangePosition(block, top)`; `ZoneDivider(before, after, top)`; `TickPosition(tick, top, visible)`; `timelineLanes = [sleep, light, caffeine, travel]` | `core/plan/…/plan/Timeline.kt` (1b) | `PC/Timeline.swift:1–63` |
| `data class TimelineRect(x, y, width, height)` (`minX…midY`, half-open `contains`); `TimelineTileFrame(blockID, frame, hitFrame, tiny, continuesBefore, continuesAfter)`; `TimelineGeometry(layout, availableWidth: Double)` → `width`, `height`, `laneWidth`, `tiles` (hit frames ≥ `minTouchTarget`) | `…/TimelineGeometry.kt` (1b) | `PC/TimelineGeometry.swift` |
| `timelineBlockFrame(blockID, geometry, layout): TimelineRect?` (48 dp minimum; marker `(0, clampY, 48, 48)`; zone change `(48, clampY, width − 48, 48)`) | `…/TimelineAccessibilityGeometry.kt` (1b) | `PC/TimelineAccessibilityGeometry.swift` |
| `PreparedDay(day, title, date, weekday, dayMonth, zone, status, summary, headline, notices, warningCount, progress, blocks, dividerLabels, ticks)` with `id`; `PreparedBlock(block, title, interval, priority /*label String*/, reason: String?, instructions, accessibilityLabel, status: CheckStatus?, continuesBefore, continuesAfter, zoneInstruction, zoneComparison)` with `id`. `day.ticks` is index-aligned with `layout.ticks` (all ticks, visible or not); `day.dividerLabels` with `layout.dividers`. | `…/PreparedPlan.kt` (1b) | `PU/TripPage/PreparedPlan.swift:5–47` |
| `PreparedPlan.revision` (= `inputHash + "|" + detailVersion`); unchanged days keep their `PreparedDay` instance (1b "stable day identity") | same | — |
| `PlanPreparer` over S1–S6, `LargeTimelineFixtures` (`large30`, `large60`, `fixture("S3")`) | `:core:plan`, `:core:testing` (1b) | `MockTimelineFixtures` |
| Contract enums from 0d: sealed `BlockType` (`Sleep`, `Nap`, `Rest`, `SeekLight`, `AvoidLight`, `LightOk`, `LightNeutral`, `CaffeineServing`, `CaffeineRoutine`, `CaffeineOk`, `NoCaffeine`, `Flight`, `Layover`, `Transfer`, `MovementBreak`, `ZoneChange`, `Note`, `Unknown(raw)`), `Priority` (`Core`, `Recommended`, `Optional`, `Unknown`), `CheckStatus` (`Done`, `Skipped`, `Unknown(raw)`), `Lane`, `Language`. **Use 0d's generated spellings**; this brief writes wire names (`caffeine_serving`) in tables. | `:core:model` (0d) | — |
| Fonts `figtree_400.ttf`, `figtree_500.ttf`, `figtree_600.ttf` (hash manifest) | `core/designsystem/src/main/res/font/` (0b) | iOS fonts |

If a metric or a palette field this brief names is missing, Task 1 adds it through the generator. Never hand-type a token.

## 5. The drawing spec [F, transcribed from `TimelineRenderer.swift` at `f391a1fa`]

**Units [R].**
- Every metric below is written in dp. The builder works in **px `Float`** throughout: multiply each `PlanMetrics` value and each 1b dp value by `key.density` once, when read. Every `DayDrawing` coordinate is px.
- `widthPx = round(geometry.width × density)`, `heightPx = round(layout.height × density)` (Compose `roundToPx` semantics).
- **Text scale:** `s = key.textScale` (= `laneLabelPx / (11 × density)`, where `laneLabelPx` is 11 sp resolved through `Density`, see Task 9). On iOS this is `fontSize / laneLabelSize` (l.124–127, 296).
- **Scaled sizes** (iOS l.124–127, 296):
  - `lineHeight = laneLabelLineHeight(14) · s`;
  - `titleSize = laneLabelSize(11) · s`;
  - `tagSize = planPrioritySize(10) · s`;
  - `tagHeight = planPriorityLineHeight(14) · s`;
  - `iconSize = blockIcon(14) · s`.
- Everything else (radii, paddings, strokes, the 76 dp details threshold, chevrons, marker size) is **unscaled**, as on iOS.

**Geometry.** Use 1b's `TimelineGeometry(layout, availableWidth = key.availableWidthPx / key.density)`. Lane `i` (0..3) has `laneX(i) = gutterWidth(44) + laneGap(4) + i·(laneWidth + laneGap)` = `48 + i·(laneWidth + 4)` dp (l.192–197).

**Text primitive** (iOS `text(...)`, l.138–180). Use it for every text run:
- Skip the run if `rect.width ≤ 0` or `rect.height ≤ 0`, **or if `rect.height < lineHeight − 0.01 px`**. iOS draws a line only while `y + lineHeight ≤ rect.maxY` (l.154), so a run that does not fit at full height is dropped, not clipped. The 0.01 px epsilon absorbs float rounding only.
- Always one line (no call site passes `wrap: true`). When `measure(text) > rect.width`, truncate the end with "…" (`TextMetrics.ellipsize`, iOS `CTLineCreateTruncatedLine(.end)`, l.157–161).
- **Too narrow for "…" [R]:** if the ellipsized text is empty, or `measure(ellipsized) > rect.width` (only possible when "…" alone does not fit), emit nothing for the run (no plate, no `TEXT`). iOS clips every run to its rect (l.143–144 `context.clip(to: rect)`) and, when `CTLineCreateTruncatedLine` returns nil, draws the clipped full line; the replay has no per-run clip, so Android drops the run instead (a sliver of at most one glyph in rects narrower than "…").
- `measured` = width of the (possibly ellipsized) text. `x = rect.left + max(0, rect.width − measured) × alignment`, alignment 0 (left), 0.5 (centre) or 1 (right).
- **Baseline = `rect.top + ascent(style)`** (iOS l.169 `y + CTFontGetAscent(font)`; Android `ascent = −paint.fontMetrics.ascent`). The line box is `[rect.top, rect.top + lineHeight]`. See OQ-1c-15.
- **Backing plate:** when the run has one, emit `RECT(x, rect.top, x + min(rect.width, measured), rect.top + lineHeight)` in the backing color **before** the `TEXT` op (l.164–167).

**Text styles** (`TimelineTextStyle`):

| Style | Face | Size | Features | Used for | iOS |
|---|---|---|---|---|---|
| `TICK` | Figtree 400 | `titleSize` | `'tnum' 1` (OQ-1c-14) | tick labels | `font`, l.123 |
| `TITLE` | Figtree 600 | `titleSize` | – | tile title, tile interval, caffeine label, marker "i" | `titleFont`, l.137 |
| `TAG` | Figtree 500 | `tagSize` | – | priority tag, reason line, divider pill text | `tagFont`, l.125 |

**Draw order.** Emit the ops in exactly this order:

0. **No page background.** iOS fills `background` (l.121–122); Android omits it (`04 §9.4`, OQ-1c-11): the list paints it. The replay starts with `clipRect(0, 0, widthPx, heightPx)`, matching the iOS media box: the first tick label of a day is cut at y = 0, as on iOS (screenshot 04, "07:30").
1. **Lanes** (l.192–197): for `i` in 0..3, `RECT(laneX(i), 0, laneX(i) + laneWidth, layout.height)` in `surface2`.
2. **Tick lines** (l.198–207): for each `layout.ticks[k]` with `visible`, one line per lane from `(laneX, top)` to `(laneX + laneWidth, top)`, `line`(1) wide, butt cap, solid, color `border` at alpha `gridOpacity` **0.6**. Batch every tick line of the day into **one `LINES` op**.
3. **Tick labels** (l.208–213): for each visible tick `k`, text `day.ticks[k]` in rect `(0, top − lineHeight/2, gutterWidth − smallGap = 40, lineHeight)`, style `TICK`, `mutedForeground`, alignment 1, no backing.
   iOS interleaves lines and labels per tick. Labels sit in the gutter and lines in the lanes, so they never overlap, and batching is pixel-identical [R].
4. **Tiles** (l.215–403): for each tile in `geometry.tiles`, in order. Skip a tile whose `blockID` is not in `day.blocks` (l.217). Let `b` = the `PreparedBlock`, `p` = the `PlacedBlock` with the same id (first match over `layout.placed` in `timelineLanes` order, l.344), `t = b.block.type`, `style = TimelineCategoryStyle(t)` (§5.1), `w, h` = tile frame size in px, `optional = b.block.priority == Optional`.
   - **Radius** (l.220–221): `R = min(w, h)/2` for `caffeine_serving`, else `tileRadius` (14). `rTop = continuesBefore ? 0 : R`, `rBottom = continuesAfter ? 0 : R`; each is then clamped to `min(·, w/2, h/2)` (l.448–449).
   - **Corners are quadratic curves**, not arcs (`tilePath`, l.447–462): move to `(rTop, 0)`; line to `(w − rTop, 0)`; `quadTo(w, 0, w, rTop)`; line to `(w, h − rBottom)`; `quadTo(w, h, w − rBottom, h)`; line to `(rBottom, h)`; `quadTo(0, h, 0, h − rBottom)`; line to `(0, rTop)`; `quadTo(0, 0, rTop, 0)`; close. Keep this start point: it sets the dash phase of outlines.
   - **Group alpha** (l.223–226): `α` = `markedOpacity` **0.6** if `b.status != null` (any status, including `Unknown`); else `optionalOpacity` **0.75** if `optional`; else 1. **Pre-composite** every color drawn inside the tile (§5.2). Never use `saveLayer`.
   - The tile emits these ops in this order, all in **tile-local px** (the replay translates to the tile origin):
     1. `TILE_BEGIN(left, top, w, h, rTop, rBottom)`.
     2. `FILL_TILE` in `style.fill` (l.227–229).
     3. **Pattern** (l.233–255), if `style.pattern` is set:
        - **dots** → `DOTS_TILE`, color = `style.foreground` at `dotOpacity` **0.16**: dot centres at `(3.5 + 7i, 3.5 + 7j)` dp from the tile origin, radius `dotRadius` 1 dp, inside the tile shape;
        - **hatch** → `HATCH_TILE(tableIndex)`, color = `style.foreground` at `hatchOpacity` **0.12**: "/" stripes with a perpendicular period of `patternStep` 8 dp, `patternLine` 2 dp wide, inside the tile shape. Phase per §5.3.
     4. **Core bar** if `b.block.priority == Core` (l.257–266): `CORE_BAR(barW = coreBorder 4 dp)` in `foreground` at `coreBorderOpacity` **0.7**: the tile shape ∩ x ∈ [0, 4 dp] (iOS strokes an 8 pt line on `minX`, clipped to the path).
     5. **Outline** (l.267–291). First matching row:

        | Case | Color | Width `bw` | Alpha | Dash |
        |---|---|---|---|---|
        | `optional` (any type) | `foreground` | `t == no_caffeine ? 2 : 1` | `optionalBorderOpacity` 0.5 | `[3·bw, 3·bw]` |
        | `t == no_caffeine` | `catCaffeine` | `emphasisBorder` 2 | 1 | `[6, 6]` |
        | `t == light_ok` | `catModerateFg` | `border` 1 | `tagOpacity` 0.4 | `[3, 3]` |
        | `t == note` | `border` | 1 | 1 | solid |
        | otherwise (incl. `Unknown`) | none | | | |

        Width follows the **type**, not the case (l.275), and the dash is on when `optional || style.dashed` (l.283), so optional no_caffeine is 2 dp `[6, 6]` in `foreground` at 0.5. The outline path is `tilePath` of the rect inset by `bw/2`, with radii `continuesBefore ? 0 : max(0, R − bw/2)` and `continuesAfter ? 0 : max(0, R − bw/2)` (then the same clamp). Emit `OUTLINE_TILE(inset = bw/2, bw, rTop', rBottom', dashKind)`; dash kinds: 0 solid, 1 `[3,3]`, 2 `[6,6]`.
     6. **Content clip** (l.292–293, iOS always clips the rest of the tile to `path`). Emit `CLIP_TILE` **only when needed** [R, OQ-1c-17]: compute the bounding box `B` of every op from steps 7–11 below (glyph squares; the strike plus 1 dp; text line boxes `[x, x + min(rect.width, measured)] × [top, top + lineHeight]`; tag pill rect; chevron segments plus 0.5 dp) **before** emitting them. Emit `CLIP_TILE` if `B` is not inside `[0, w] × [0, h]`, or if any corner fails `√a + √b ≥ √r`, where `r` is that corner's radius (`rTop` for the top corners, `rBottom` for the bottom ones; skip the test when `r == 0`) and `(a, b)` are the distances from that tile corner to `B`'s nearest corner (for top-left: `a = B.left`, `b = B.top`; top-right: `a = w − B.right`, `b = B.top`; bottom-left: `a = B.left`, `b = h − B.bottom`; bottom-right: `a = w − B.right`, `b = h − B.bottom`). This test is exact for the quadratic corner: its inner boundary is `√x + √y = √r` (testing `B`'s corner is conservative: it may clip when not strictly needed, never the reverse). A normal 120 × 80 dp tile needs no clip. The clip appears when the glyph square overflows the tile: thin tiles (`w < tilePadding + iconSize`, e.g. 16 dp wide) and tiles lower than the icon (e.g. a 10 dp tiny tile, icon at y = −2). Worked negative: a 30 × 20 dp `caffeine_serving` capsule (`R = 10`, icon at (6, 3), `√6 + √3 = 4.18 ≥ √10 = 3.16`) needs **no** clip; a 16 × 16 dp iconOnly capsule (`R = 8`, icon at (1, 1), `√1 + √1 = 2 < √8`) does.
     7. **Icon** (l.294–310):
        - `iconOnly = key.compact || tile.tiny`;
        - `contentTop = tilePadding(6) + (continuesBefore ? iconSize + smallGap(4) : 0)`;
        - `iconX = iconOnly ? w/2 − iconSize/2 : 6`;
        - `iconY = (iconOnly || t == caffeine_serving) ? h/2 − iconSize/2 : contentTop`.

        `GLYPH(iconX, iconY, iconSize, style.glyph)` in **`style.foreground`**, not the text color, even when faded (iOS `TimelineSymbols`, l.80–85). For `BlockType.Unknown` emit **no** glyph: iOS has no symbol for an unknown raw type (l.76–79, 303).
     8. **Strike** for `no_caffeine` (l.311–315): `LINE(iconX, iconY + iconSize) → (iconX + iconSize, iconY)`, `emphasisBorder` 2 dp, butt cap, `style.foreground`.
     9. **Text** (l.316–382), only when `!iconOnly`:
        - `textColor` = `cardForeground` if `b.status != null || optional`, else `style.foreground`.
        - `titleX = iconX + iconSize + 4`; `statusWidth = b.status == null ? 0 : iconSize + 4`; `contentWidth = max(0, w − 12)`.
        - **`caffeine_serving`:** text `b.title + " · " + b.interval` in rect `(titleX, h/2 − lineHeight/2, max(0, w − 6 − statusWidth − titleX), lineHeight)`, `TITLE`, `textColor`, alignment 0.5, backing `style.fill`. No interval line, no details.
        - **Every other type:**
          - **Title:** rect `(titleX, contentTop, max(0, w − 6 − statusWidth − titleX), lineHeight)`, `TITLE`, `textColor`, backing `style.fill`.
          - **Interval:** `intervalTop = contentTop + lineHeight + 4`; rect `(6, intervalTop, contentWidth, min(lineHeight, max(0, h − 6 − intervalTop)))`, `TITLE`, `textColor`, backing `style.fill`. The primitive drops it when it does not fit.
          - **Details** only if `p.cols == 1` **and** `h > detailsThreshold(76) dp`:
            - `tagTop = intervalTop + lineHeight + 4`;
            - `tagW = min(contentWidth, measure(b.priority, TAG) + 12)`;
            - `tagRect = (6, tagTop, tagW, tagHeight + 2·border(1))`;
            - only if `tagRect.bottom ≤ h − 6`:
              - pill = `tagRect` inset by 0.5 dp, radius `tagRect.height/2`;
              - `RRECT_FILL(pill)` in `background` at `tagOpacity` **0.4**;
              - `RRECT_STROKE(pill, 1 dp)` in `textColor` at **0.4**;
              - text `b.priority` in `tagRect` inset by (6, 1), `TAG`, `textColor`, alignment 0, no backing;
              - if `b.reason != null`: `y = tagRect.bottom + 4`, rect `(6, y, contentWidth, min(tagHeight, max(0, h − 6 − y)))`, `TAG`, `textColor`, backing `style.fill`.
     10. **Status glyph** if `b.status != null` (l.383–388): `GLYPH(w − 6 − iconSize, iconOnly ? 6 : contentTop, iconSize, status == Done ? CHECK : SKIP_FORWARD)` in **`foreground`** (iOS l.90–96). `CheckStatus.Unknown` draws `SKIP_FORWARD` (iOS `== .done ? … : skipped`).
     11. **Chevrons** (l.389–400), two `line`(1) dp `LINE`s each, butt cap, `style.foreground`, `mx = w/2`:
         - if `continuesBefore`, `y = 4`: `(mx−4, y+4)→(mx, y)` and `(mx, y)→(mx+4, y+4)`;
         - if `continuesAfter`, `y = h − 8`: `(mx−4, y)→(mx, y+4)` and `(mx, y+4)→(mx+4, y)`.
     12. `TILE_END`.
5. **Zone dividers** (l.404–431), not faded: for each `layout.dividers[j]`:
   - **Line:** `DASH_LINE (0, top)→(geometry.width, top)`, `emphasisBorder` 2 dp, round cap, dash `[2, 4]` (dash kind 3), `catTravel`.
   - **Pill:** `pillH = tagHeight + 2·zonePillVerticalPadding(2)`; `pill = (48, top − pillH/2, min(geometry.width − 48, measure(dividerLabels[j], TAG) + 2·zonePillPadding(8)), pillH)`; `RRECT_FILL` radius `pillH/2`, `catTravel`; text `dividerLabels[j]` in the pill inset by (8, 2), `TAG`, `catTravelFg`, alignment 0, no backing; record the pill rect (px) as divider frame `j`.
6. **Info markers** (l.432–439): for each `layout.markers`:
   - `CIRCLE` inscribed in `(gutterWidth − gutterMarkerSize = 20, top − 12, 24, 24)`, in `card`;
   - text `INFO_MARKER_GLYPH = "i"` in that rect inset by (4, 0), `TITLE`, `foreground`, alignment 0 (the line box sits at the circle's top, as on iOS; OQ-1c-4). `"i"` is a glyph, not copy: keep it a constant with a *why* comment.

### 5.1 Category styles [F `PlanAppearance.swift:14–37`; glyphs from `DC/…/bundle.js` `le={…}`, OQ-T10]

| `BlockType` | fill / foreground | pattern | dashed | `TimelineGlyph` (lucide name) |
|---|---|---|---|---|
| sleep | catSleep / catSleepFg | – | – | MOON (`moon`) |
| nap | catSleep / catSleepFg | dots | – | BED (`bed`) |
| rest | catSleep / catSleepFg | hatch | – | ARMCHAIR (`armchair`) |
| seek_light | catLight / catLightFg | – | – | SUN (`sun`) |
| avoid_light | catDark / catDarkFg | hatch | – | GLASSES (`glasses`) |
| light_ok | catModerate / catModerateFg | dots | yes | CLOUD_SUN (`cloud-sun`) |
| light_neutral | catModerate / catModerateFg | – | – | LAMP (`lamp`) |
| caffeine_serving | catCaffeine / catCaffeineFg | dots | – | COFFEE (`coffee`) |
| caffeine_routine | catCaffeine / catCaffeineFg | hatch | – | COFFEE |
| caffeine_ok | catCaffeine / catCaffeineFg | – | – | COFFEE |
| no_caffeine | card / foreground | – | yes | COFFEE, plus the strike |
| flight | catTravel / catTravelFg | – | – | PLANE (`plane`) |
| layover | catTravel / catTravelFg | hatch | – | TIMER (`timer`) |
| transfer | catTravel / catTravelFg | – | – | ARROW_LEFT_RIGHT (`arrow-left-right`) |
| movement_break | catTravel / catTravelFg | – | – | FOOTPRINTS (`footprints`) |
| zone_change | catTravel / catTravelFg | – | – | GLOBE (`globe`) |
| note | card / foreground | – | – | INFO (`info`) |
| Unknown(raw) | card / foreground | – | – | none (no glyph op) |

Status glyphs: CHECK (`check`) and SKIP_FORWARD (`skip-forward`); the bundle's `TimelineBlock` uses `I = check` and `K = skip-forward`. `enum class TimelineGlyph(val lucideName: String)` has these 16 entries.

### 5.2 Pre-composited group alpha [R, A12, `04 §9.4`, OQ-T26]

For a tile with α < 1, every color drawn inside it, `c` (an opaque token), becomes:
- `c' = round(α·c + (1−α)·surface2)` per sRGB channel, opaque;
- then the element's own alpha applies unchanged: dots 0.16, hatch 0.12, core bar 0.7, tag fill and stroke 0.4, optional outline 0.5, light_ok outline 0.4. The ARGB alpha byte is `round(255·a)`.

This is exact for every element, including antialiased edges, wherever the tile sits on plain `surface2`. Tick lines under a marked tile disappear; that is accepted (OQ-T26). Implement it once as `TimelineColors.precomposite(color, alpha, under)` and `TimelineColors.withAlpha(color, a)`. A typical bug is applying the sub-alpha first, or α twice.

### 5.3 Patterns [R, A12]

**Dots:**
- one `BitmapShader` per density on an **ALPHA_8** bitmap, so the paint color tints it;
- `N = ceil(7·density)` px square, one antialiased circle of radius `N/7` px at `(N/2, N/2)`;
- `Shader.TileMode.REPEAT`, paint flags `ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG`;
- local matrix `setScale(7·density/N, 7·density/N)`: the period is exactly 7 dp, and the centres land at 3.5 dp. It is anchored at the tile origin because the replay translates there (OQ-T29);
- the replay sets `dotPaint.color = c` and calls `drawPath(tilePath, dotPaint)`.
- Accepted difference: iOS draws a dot only when its centre is inside the rect (l.238–239); the shader also shows a partial dot whose centre is just past the bottom/right edge (≤ 1 dp sliver).

**Hatch:**
- a `LinearGradient` from `(0, 0)` to `(8/√2·density, 8/√2·density)`, colors `[c, c, TimelineColors.TRANSPARENT, TimelineColors.TRANSPARENT]` at `[0, 0.25, 0.25, 1]`, `REPEAT`. (Not `android.graphics.Color.TRANSPARENT`: Phase 2's `noRawColorsOutsideDesignSystem` bans every `Color.<Name>` in feature code except Compose `Color.Transparent`/`Color.Unspecified`.) This is the board CSS: a 2 dp stripe starting at the line through the tile's top-left corner (OQ-1c-2; iOS centres it on that line).
- Gradient colors are baked into the shader, so `TimelineResources` prebuilds one hatch `Paint` per entry of `TimelineColors.hatchColorTable(palette)`: the foreground slots of the hatch styles, `[catSleepFg, catDarkFg, catCaffeineFg, catTravelFg]`, × α `[1.0, 0.75, 0.6]`, each pre-composited over `surface2` and then at alpha 0.12. That is **12 entries**, index = `slot·3 + alphaIndex`.
- `HATCH_TILE` carries the table index from `TimelineColors.hatchIndex(fgSlot, alpha)`. Builder and resources call the same functions, so they always agree.

## 6. Window and width conventions for tests [R]

- A test parameter `W` is a **window** width in dp.
- Content width = `W − 2·gutter(16)`: 360 → 328, 390 → 358, 411 → 379, 639 → 607, 640 → 608, 841 → 809.
- `compact = W < timelineLabelBreakpoint (640)` (`04 §3.9`; iOS `I/plan.md` "15 integration fixes": the safe-area-inclusive window width, independent of the lane width).
- The iOS ports use the iOS content widths directly (288, 358 and 637, window = content + 32).
- The wide-layout content width is 6e's; do not model it here.

---

## 7. Tasks

Each task is one commit. Paths are abbreviated: `core/plan/…/X.kt` means `core/plan/src/main/kotlin/net/chronoflow/core/plan/X.kt`; `feature/trip/…/X.kt` means `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`. New `:core:plan` code goes in package `net.chronoflow.core.plan.drawing`; new `:feature:trip` code in `net.chronoflow.feature.trip.timeline`. Tick plan.md checkboxes in the commits named in brackets. Commit messages follow AGENTS.md "Git" (no trailers).

### Task 1: Missing tokens, only if §4 found gaps (`feat(tokens): add timeline drawing tokens`)

- Extend `:tools:tokengen` so `TimelinePalette.generated.kt` holds every palette field this brief names, light and dark, and `PlanMetrics.generated.kt` every metric it names.
- Sources: `DC/ds/chronoflow/tokens.json` for colors; iOS `Tools/TokenGen/Plan.source.json` at the pin for metrics.
- Commit the generator change and the regenerated output together.
- **Tests:** extend 0b's golden tests: each palette ARGB equals `08 §4.1` (e.g. light `surface2 #EBF1F8`, dark `surface2 #0E1225`, light `catTravel #007980`); each metric equals `PlanMetrics.generated.swift`.
- Skip this task when nothing is missing, and record the skip in Task 2's commit body.

### Task 2: Op list, text metrics and key (`feat(plan): add day drawing op list`) [ticks "DayDrawing.kt"]

Files:
- `core/plan/…/drawing/DrawOp.kt`: `object DrawOp` with `const val` Byte codes. The table below is the whole contract.
- `core/plan/…/drawing/DayDrawing.kt`:
  - `class DayDrawing internal constructor(...)` with `@JvmField val`s: `key: DayDrawingKey`, `widthPx: Int`, `heightPx: Int`, `ops: ByteArray`, `coords: FloatArray`, `colors: IntArray`, `args: IntArray`, `texts: Array<String>`, `textStyles: ByteArray`, `glyphs: ByteArray`, `dividerFrames: FloatArray` (4 per divider: l, t, r, b px), `targets: List<TimelineTarget>`, `hits: HitIndex`, `estimatedBytes: Int`.
  - Immutable by contract: only the builder writes the arrays, and nothing exposes a mutator. Add its FQN to the root `stability_config.conf`.
  - `estimatedBytes` = `ops.size + 4·(coords.size + colors.size + args.size + dividerFrames.size) + textStyles.size + glyphs.size + Σtexts(40 + 2·len) + Σtargets(64 + 2·(label.length + (testTag?.length ?: 0))) + hits.estimatedBytes + 128`.
- `core/plan/…/drawing/TextMetrics.kt`:
  - `interface TextMetrics { fun measure(style: TimelineTextStyle, text: String, sizePx: Float): Float; fun ellipsize(style: TimelineTextStyle, text: String, sizePx: Float, maxWidthPx: Float): String; fun ascent(style: TimelineTextStyle, sizePx: Float): Float }`. `ellipsize` truncates the end with "…" (U+2026) and returns `text` unchanged when it fits. `ascent` is positive (distance from line top to baseline, §5 text primitive).
  - `enum class TimelineTextStyle { TICK, TITLE, TAG }`.
  - Line height is not a metrics call: it is `14·s` from `PlanMetrics` (iOS l.126; OQ-1c-16).
- `core/plan/…/drawing/DayDrawingKey.kt`: `data class DayDrawingKey(val dayId: String, val revision: String, val availableWidthPx: Int, val density: Float, val textScale: Float, val dark: Boolean, val compact: Boolean, val language: Language)`. This exact shape is what 1d constructs (1d P5). `density` and `compact` extend the plan's six fields because both change the output (iOS `RenderKey`, `DayTimelineView.swift:189–197`; OQ-1c-10).
- `core/testing/src/main/kotlin/net/chronoflow/core/testing/FakeTextMetrics.kt` (1d uses it, P7), deterministic: advance = `0.55·sizePx` per char (`0.6·sizePx` for every char of `TICK`); `ascent = 0.8·sizePx`; `ellipsize` mirrors `TextUtils.ellipsize(…, END)`: the text unchanged if it fits; else the longest prefix `p` with `measure(p + "…") ≤ max`, returned as `p + "…"` (possibly just `"…"`); `""` when `"…"` alone does not fit. `:core:testing` needs `implementation(project(":core:plan"))` for the interface; add it if absent (no cycle: `:core:plan` uses `:core:testing` only as `testImplementation`).

Op table. Each op reads its operands in order from the listed arrays; every coordinate is px. "Tile-local" ops are valid only between `TILE_BEGIN` and `TILE_END`. **Cursors [R]:** the replay keeps one running cursor per array (`coords`, `colors`, `args`, `texts`, `glyphs`); `texts` and `textStyles` share one cursor (one entry per `TEXT` op), and `glyphs` advances once per `GLYPH` op. No op stores an index into another array, so the walk is a single forward pass and `DrawingTestSupport.walk` can prove that every array is consumed exactly to its end.

| Code | coords | colors | args | other | Replay |
|---|---|---|---|---|---|
| `RECT` | l,t,r,b | 1 | – | – | `drawRect`, fill |
| `LINES` | strokeW, then 4n | 1 | n | – | `drawLines(coords, offset, 4n, paint)`, butt cap |
| `TILE_BEGIN` | l,t,w,h,rTop,rBottom | – | – | – | `save(); translate(l,t)`; rebuild scratch `tilePath` (quad corners, local) |
| `FILL_TILE` | – | 1 | – | – | `drawPath(tilePath, fill)` |
| `DOTS_TILE` | – | 1 | – | – | `dotPaint.color = c; drawPath(tilePath, dotPaint)` |
| `HATCH_TILE` | – | – | tableIndex | – | `drawPath(tilePath, hatchPaints[i])` |
| `CORE_BAR` | barW | 1 | – | – | `save(); clipRect(0, 0, barW, h); drawPath(tilePath, fill); restore()` (rect clip only) |
| `OUTLINE_TILE` | inset, strokeW, rTop, rBottom | 1 | dashKind 0/1/2 | – | rebuild scratch `outlinePath` on the inset rect; `drawPath`, stroke |
| `CLIP_TILE` | – | – | – | – | `clipPath(tilePath)` (undone by `TILE_END`) |
| `TILE_END` | – | – | – | – | `restore()` |
| `GLYPH` | x,y,size | 1 | – | glyph ordinal | `save(); translate(x,y); scale(size/24)`; stroke each path (width 2 units, round cap and join); `restore()` |
| `LINE` | x0,y0,x1,y1,strokeW | 1 | cap (0 butt, 1 round) | – | `drawLine` |
| `DASH_LINE` | x0,y0,x1,y1,strokeW | 1 | dashKind 3 (`[2,4]`, round) | – | `drawLine`, dashed paint |
| `TEXT` | x, baselineY, sizePx | 1 | – | text index, style | `textPaint[style].textSize = size; .color = c; drawText(s, x, y, paint)` |
| `RRECT_FILL` | l,t,r,b,radius | 1 | – | – | `drawRoundRect` |
| `RRECT_STROKE` | l,t,r,b,radius,strokeW | 1 | – | – | `drawRoundRect`, stroke |
| `CIRCLE` | cx,cy,radius | 1 | – | – | `drawCircle` |

Dash intervals are dp × density, built once in `TimelineResources`.

**Tests:** `DayDrawingTest` (JVM, `:core:plan` `src/test`). An empty-day drawing reports `estimatedBytes` by the formula. Put the shared helpers in `core/plan/src/test/kotlin/net/chronoflow/core/plan/drawing/DrawingTestSupport.kt`: `DayDrawing.walk(visitor)` (a reader that consumes every operand array exactly to its end and fails otherwise), `DayDrawing.serialize(): String` (one line per op: name, coords to 2 decimals, colors `#AARRGGBB`, args, text in quotes, style, glyph name), and `opListEquals(a, b)` (ops, coords, colors, args, texts, textStyles, glyphs, dividerFrames; **not** targets).

### Task 3: Category styles and colors (`feat(plan): add timeline category styles and colour rules`)

Files:
- `core/plan/…/drawing/TimelineCategoryStyle.kt`: the §5.1 table, ported from `PlanCategoryStyle`. Fields `fill`, `foreground` (palette slot enums or getters on `TimelinePalette`), `pattern: Pattern?` (`DOTS`, `HATCH`), `dashed`, `glyph: TimelineGlyph?` (null for `Unknown`).
- `core/plan/…/drawing/TimelineGlyph.kt`: the 16 entries with `lucideName`.
- `core/plan/…/drawing/TimelineColors.kt`: `precomposite`, `withAlpha`, `groupAlpha(status, priority)`, `hatchColorTable(palette): IntArray`, `hatchIndex(fgSlot, alpha)`, and `const val TRANSPARENT: Int = 0` (the gradient gap colour, with a one-line *why*: the feature Konsist rule bans `Color.TRANSPARENT`).

**Tests:**
- `TimelineCategoryStyleTest`: every `BlockType` (including `Unknown("x")`) maps per §5.1.
- `TimelineColorsTest`: α = 1 returns the color unchanged; light `catSleep` at 0.6 over `surface2` equals the hand-computed value, channel by channel; the hatch table has 12 entries and is deterministic; `groupAlpha(Done, Core) = 0.6`, `(null, Optional) = 0.75`, `(Skipped, Optional) = 0.6`, `(Unknown("x"), Recommended) = 0.6`, `(null, Recommended) = 1`.

### Task 4: Builder (`feat(plan): build day drawings in the iOS draw order`)

File: `core/plan/…/drawing/DayDrawingBuilder.kt`: `object DayDrawingBuilder { fun build(day: PreparedDay, layout: DayLayout, key: DayDrawingKey, palette: TimelinePalette, metrics: TextMetrics, precomposite: Boolean = true): DayDrawing }` (1d calls the first five; P5).
- Implements §5 exactly. `precomposite = false` exists only for `PrecompositedAlphaTest`'s reference; say so in a *why* comment. With `precomposite = false` every in-tile colour is the raw token at its sub-alpha, and `HATCH_TILE` carries `hatchIndex(fgSlot, 1.0)` (the α = 1 entry), so the reference uses the same 12-entry table.
- Grows primitive arrays with a small internal `FloatList`/`IntList`/`ByteList` (amortized growth, trimmed copy at the end). No boxed collections and no per-op objects. One `HashMap` from block id to `PreparedBlock` and to `PlacedBlock` per build is fine.
- For `CLIP_TILE`, compute each tile's content rects and measurements first, decide the clip, then emit.
- Never calls `String.format`, `DateTimeFormatter` or `Locale`. Every string comes from `PreparedDay`/`PreparedBlock`, except the `" · "` joiner (iOS l.323) and `INFO_MARKER_GLYPH`.
- A pure function, safe on any thread as long as each call gets its own `TextMetrics`.
- In this task `targets` and `hits` are empty; Task 5 fills them.

**Tests** (JVM):
- `DrawOrderGoldenTest`: S3 day 1 (the first `PreparedDay`), en, light, density 1.0, `FakeTextMetrics`, textScale 1.0, at `W = 411` (compact) and `W = 841` (labels shown).
  - Compare `serialize()` with `core/plan/src/test/resources/golden/drawing/S3-day1-W411.txt` and `-W841.txt`. On a mismatch, write the actual output to `build/golden/` and fail naming the path.
  - The golden is committed only after the reviewer signs it off against `Plan`/`PlanDark` and the iOS screenshots. Never re-record it to make a test pass.
- `TimelineAlphaTests` (Swift class name kept). `compactAndTinyTilesHideAllCopyAndInvalidateTheCache`, ported on op lists (`TimelineAlphaTests.swift:11–75`):
  - S3 first day; the first entry of its `layout.placed[sleep]` (any type in that lane, as iOS `layout.placed[.sleep]?.first`) forced to top 0, height 120, col 0, cols 1, not tiny, no continuation; `status = null`; `day.blocks = [that block]`, no ticks, divider labels, markers, changes or dividers; `layout.height = 120`; `placed = {sleep: [it], light: [], caffeine: [], travel: []}`.
  - `changed` = the day with title `"Changed title"`, interval `"00:00 – 23:59"`, priority `"Changed priority"`, reason `"Changed reason"`.
  - One test case per (light, dark) × window `[320, 390, 393, 430, 639, 640, 669]` (content = W − 32, compact = W < 640, density 3): `opListEquals(original, changed) == compact`.
  - With `tiny = true` at content 637, not compact: equal.
  - `day.blocks[0].accessibilityLabel` contains its title and its interval.
  - The cache half is in Task 6.
- `DayDrawingRulesTest`, one method per rule:
  - `caffeine_serving` gets `R = min(w, h)/2` and exactly one centred `TEXT` in the tile;
  - continued edges get radius 0 and two chevron `LINE`s each;
  - a 70 dp tile gets no tag; a 77 dp tile with cols 1 gets the tag `RRECT_FILL`; with cols 2, none;
  - the interval is absent when `h − 6 − intervalTop < lineHeight`;
  - `no_caffeine` gets the strike plus a 2 dp dash-kind-2 outline in `catCaffeine`; **optional `no_caffeine` gets a 2 dp dash-kind-2 outline in `foreground` at 0.5**; optional `sleep` gets 1 dp dash-kind-1;
  - the marked text color is pre-composited `cardForeground`; the status glyph color is pre-composited `foreground`; the icon color is pre-composited `style.foreground`;
  - `Unknown` type emits no `GLYPH` for the tile; `CheckStatus.Unknown` emits `SKIP_FORWARD`;
  - a 120 × 80 dp tile and a non-compact 30 × 20 dp `caffeine_serving` capsule emit no `CLIP_TILE`; a tiny 10 dp-high tile, a 16 × 16 dp compact `caffeine_serving` capsule and a 16 dp-wide (120 dp high, not tiny) tile do (numbers in §5 step 4.6);
  - a run whose rect is narrower than "…" emits no `TEXT` and no plate: a 35 × 120 dp tile at density 1, not compact, not tiny, has a 5 dp title rect (`35 − 6 − 24`), and "…" is `0.55 · 11 = 6.05` px with `FakeTextMetrics`, so the title is absent while the interval (23 dp rect) is present;
  - when compact, no `TEXT` op lies inside any tile (`04 §10.1`).

### Task 5: Hit index and accessibility targets (`feat(plan): add timeline hit index and targets`) [ticks "DayDrawingBuilder.kt"]

Files:
- `core/plan/…/drawing/HitIndex.kt`: arrays only (tile hit frames `l, t, r, b` and frame `midY` as `FloatArray`, ids as `Array<String>`; marker tops plus ids; change tops plus ids; `gutterPx = gutterWidth·density`, `halfTouchPx = PlanMetrics.minTouchTarget/2·density` = 24 dp). `fun resolve(x: Float, y: Float): String?` (px) allocates nothing and resolves in iOS order (`DayTimelineView.swift:102–113`):
  1. among tiles whose hit frame contains the point (half-open, like `TimelineRect.contains`), the one with the smallest `|frame.midY − y|`, the first on ties;
  2. else the first marker with `|top − y| ≤ halfTouch` and `x ≤ gutterPx` (iOS 44; OQ-1b-3: targets grow, the tap band stays). **[R] Android delta:** iOS uses `touch/2 = 22` for the y band (l.107, 110; `04 §4.4`); Android uses `minTouchTarget/2 = 24` (OQ-T5, OQ-1b-11: 1d's tap bands read `minTouchTarget`);
  3. else the first zone change with `|top − y| ≤ halfTouch`;
  4. else `null`.

  It reports `estimatedBytes`.
- `core/plan/…/drawing/TimelineTarget.kt`: `class TimelineTarget(val kind: Kind /*BLOCK, DIVIDER, TICK*/, val blockId: String?, val label: String, val left: Float, val top: Float, val right: Float, val bottom: Float /*px*/, val traversalIndex: Float, val testTag: String?)` (1d P5). Add its FQN to `stability_config.conf`.
- The builder now emits targets in this order (`DayTimelineView.swift:117–158`; `04 §5`):
  - **Blocks:** `day.blocks` order, frame = `timelineBlockFrame(id, geometry, layout)` × density (skip `null`), label `accessibilityLabel`, `traversalIndex = i`, `testTag = "trip.block.<id>"`.
  - **Dividers:** frame = the recorded pill frame, label `dividerLabels[j]`, `traversalIndex = 10000 + j`, no tag. Not enlarged: it is not interactive (iOS parity, OQ-1c-12).
  - **Ticks:** visible ticks only, frame `(0, top − lineHeight/2, gutterWidth, top + lineHeight/2)`, label `day.ticks[k]`, `traversalIndex = 20000 + k`, no tag.
  - The "Now" target (−1) is 1d's.

**Tests** (JVM):
- `HitIndexTest`: overlapping tiles return the nearest midY; on a tie the first wins; the marker band hits at x = 44 dp and misses at x = 45 dp; the zone-change band hits at any x; a miss returns null; a tile wins over a marker; `resolve` at the hit frame's right edge misses (half-open).
- `TimelineTargetsTest` (new; 1b already ports `accessibilityTargetsFollowTilesAndGutterMarkers` as `TimelineGeometryProfilesTest`, so do not duplicate it): S3 in **nl**, content widths 288, 358 and 637, density 3: every `BLOCK` target equals `timelineBlockFrame × 3`; the `DIVIDER` count equals `layout.dividers.size` and each frame equals `dividerFrames`; `TICK` targets exist only for visible ticks; traversal indices increase strictly; tags are unique.

### Task 6: Cache (`feat(plan): add bounded day drawing cache`) [ticks "DayDrawingCache.kt"]

File: `core/plan/…/drawing/DayDrawingCache.kt`: `class DayDrawingCache(val maxBytes: Long = 4L * 1024 * 1024, val maxEntries: Int = 8, val maxDrawingBytes: Int = 512 * 1024)` (1d constructs it with larger limits in tests):
- a thread-safe (`synchronized`) LRU; an entry holds `(key, day: PreparedDay, drawing)`;
- `get(key, day)` hits only when the key is equal **and** (`entry.day === day || entry.day == day`): the iOS full-equality guard (`TimelineRenderer.swift:41–43`), with reference equality as the fast path. The layout is not compared (OQ-1c-18);
- `peek(key, day)` behaves like `get` but does not touch LRU order;
- `put(key, day, drawing)`: a drawing with `estimatedBytes > maxDrawingBytes` is **not stored** (a debug-build `assert`, and the caller still has the drawing); a put on an existing key replaces it;
- eviction after each put: while `totalBytes > maxBytes`, evict the LRU entry; then, while `size > maxEntries`, evict the LRU entry;
- `getOrBuild(key, day, build: () -> DayDrawing): DayDrawing`: never run `build()` under the lock; on a race, keep and return the first stored entry;
- read-only counters `totalBytes`, `size`, `buildCount` (incremented by `getOrBuild` builds), and `clear()`.

**Tests** (JVM):
- `DayDrawingCacheTest`: an equal key with the same day returns the same instance and `buildCount` is unchanged; changing `dark`, `availableWidthPx`, `textScale`, `density`, `compact`, `language` or `revision` misses; an equal key with an unequal `PreparedDay` misses; 9 puts leave 8 entries; byte overflow evicts by LRU; a 600 KiB drawing is returned but not cached; 4 coroutines on `Dispatchers.Default` calling `getOrBuild` concurrently (`runTest(timeout = 60.seconds)`) never exceed the caps.
- `TimelineRenderingTests` (Swift class name kept), `vectorDrawingsStayWithinTraversalBudgetAndInvalidateCorrectly` (`TimelineRenderingTests.swift:14–52`): large60, en, content widths 358 and 637 (compact when `width + 32 < 640`), density 3, `FakeTextMetrics`, through one `DayDrawingCache()`:
  - every drawing has `|heightPx − layout.height × 3| ≤ 1`;
  - `dividerFrames.size / 4 == layout.dividers.size`;
  - every drawing is < 512 KiB, and `totalBytes ≤ 4 MiB` after each put;
  - then, for the last day at 637: a repeated `getOrBuild` returns the same instance with `buildCount` unchanged, and the dark key raises `buildCount` by 1;
  - print `DayDrawing metric: largest <n> bytes`.
- Add to `TimelineAlphaTests` the cache half of the iOS test: through a cache, a repeated `getOrBuild` of `changed` returns the same instance, and toggling `compact` yields a new instance whose op list differs.

### Task 7: JVM budgets (`test(plan): hold day drawing build budgets`)

**`DayDrawingBudgetTest`** (JVM, `@TimingGate`: it runs only in the serial `make timing` tier, 0e OQ-0e-22; 1b already put `core:plan` in `TIMING_MODULES`):
- large60; window widths **360, 411 and 841** (content = W − 32; compact per §6); density 2.625; textScale 1.0; light; `FakeTextMetrics`. One test method per width.
- Three full warm-up passes at the width, then five measured passes with `System.nanoTime` per day; call `System.gc()` between passes, outside the timed region.
- Gates: for each day, the median of its five samples; the largest of these per-day medians **≤ 5 ms**; the median of the five per-width totals **≤ 150 ms**; every `estimatedBytes` **≤ 512 KiB** (524,288 B). The single largest sample is printed, never gated: one GC pause or preemption must not fail the commit gate.
- Feed every drawing into a fresh `DayDrawingCache()` and assert `totalBytes ≤ 4 MiB` and `size ≤ 8` after each put.
- Print one line per width, exactly: `DayDrawing metric: W=<w> dayMedianMax=<ms, 2 decimals> dayMax=<ms, 2 decimals> totalMedian=<ms, 1 decimal> bytesMax=<B>` (1e Task 8 reads it).

### Task 8: Lucide glyph data (`build(icons): generate timeline lucide glyphs`)

The glyph source is the **boards' lucide-react 0.511.0** in `DC/ds/chronoflow/components/bundle.js`, the same source and output Phase 2 Task 10 specifies (OQ-1c-8). `moon` and `skip-forward` differ in lucide-static 1.66.0, so do not use the RN `node_modules`.
- **If Phase 2 Task 10 is already in the stack** (the expected case: Phase 2 is wave 3, 1c wave 5): skip the generator; check that `LucideGlyphs` has all 16 names of §5.1. **[F]** All 16 are among the bundle's 30 `r("<name>", …)` nodes, and Phase 2's initial manifest lists all 30, so nothing should be missing. If one is, add it to `tools/icons/lucide.json` as `{"source": "bundle"}`, run `make generate`, raise the glyph count in Phase 2's `LucideGlyphsTest` (32 + the added names), and commit the manifest, test and regenerated output in this task as `build(icons): add timeline lucide glyphs`. Otherwise this task has no commit: record "Task 8: all 16 glyphs present, no commit" in Task 10's commit body.
- **Otherwise** create the Phase 2 generator with only the timeline subset:
  - `tools/icons/lucide.json`: `{"version": "0.511.0", "glyphs": {"<name>": {"source": "bundle"}}}` for the 16 names (sorted).
  - `tools/icons/generate_icons.py` (`python3 -I`, stdlib only, with a `--check` mode that regenerates into `build/` and diffs): read each icon node array `var X=[[…]],Y=r("<name>",X)` from `bundle.js`; convert `path` (d as is), `circle` (two arcs), `line` (`M…L…`), `rect` (with `rx`/`ry` arcs), `polyline`, `polygon` (+`Z`) to path data in the 24-unit space; keep a per-element `filled` flag (`fill="currentColor"`). A manifest name that matches nothing fails, naming it.
  - Output `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/icon/LucideGlyphs.generated.kt`, exactly Phase 2's shape: `class LucideGlyph(val name: String, val paths: List<LucidePath>)`, `class LucidePath(val d: String, val filled: Boolean)`, `object LucideGlyphs { val Moon = LucideGlyph("moon", …) … }` (PascalCase names, "generated, do not edit" header, no Compose import). Do **not** write `Lucide.kt` (`ImageVector`); that is Phase 2.
  - Wire it exactly as Phase 2 Task 10 does, so Phase 2 later extends it without renaming anything: Makefile `vectors:` = `python3 -I tools/icons/generate_icons.py` and `.PHONY` `vectors-check:` = the same with `--check` followed by the `tools/icons` unittest discover, one `+=` line each, `GENERATORS += vectors` and `GENERATOR_CHECKS += vectors-check`, directly under 0a's definitions (never name them `icons`: 0b's `make icons` builds the launcher icons); and one line in `Scripts/test_scripts.sh` after 0c's `tools/l10n` line: `python3 -I -m unittest discover -s "$ROOT/tools/icons" -t "$ROOT/tools/icons" -p 'test_*.py'`. Record "lucide 0.511.0 (ISC), source: boards bundle" in `docs/third-party.md` (create it with heading `# Third-party notices` if absent; Phase 2 extends it) with the ISC text (copy it from `/Users/rickgout/Sites/chronoflow-mobile/node_modules/lucide-static/LICENSE`, read-only; the license is the same across versions).
- **Tests:** `tools/icons/test_generate_icons.py` (discovered through the `Scripts/test_scripts.sh` line above): `circle cx=12 cy=12 r=10` → the exact arc path your converter emits; `skip-forward`'s polygon ends in `Z`; `moon` = `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`; two runs are byte-identical; an unknown name fails. The Android parse test is in Task 10.

### Task 9: TextPaint metrics (`feat(trip): measure timeline text with TextPaint`)

Files in `feature/trip/…/timeline/`:
- **`TimelineTypefaces`** (`class TimelineTypefaces(val regular: Typeface, val medium: Typeface, val semibold: Typeface)`): `companion object { suspend fun load(context: Context, io: CoroutineDispatcher = Dispatchers.IO): TimelineTypefaces; fun cachedOrNull(): TimelineTypefaces? }`. `load` loads Figtree 400/500/600 with `ResourcesCompat.getFont(context, net.chronoflow.core.designsystem.R.font.figtree_{400,500,600})` inside `withContext(io)`, cached once per process (a `Mutex`-guarded load writing a `@Volatile` field). `cachedOrNull()` reads that field without suspending (`TimelineResources` uses it, Task 10). Never load on main (StrictMode). 1d calls `TimelineTypefaces.load(appContext)`.
- **`TextPaintMetrics(typefaces)`**: implements `TextMetrics` with **one `TextPaint` per style per instance**; create one instance per build job (`Paint` is not thread-safe).
  - `isAntiAlias = true`, `isSubpixelText = true`; `TICK` sets `fontFeatureSettings = "'tnum' 1"`;
  - every call (`measure`, `ellipsize`, `ascent`) sets `textSize = sizePx` first;
  - `measure` = `paint.measureText(text)`;
  - `ellipsize` = `TextUtils.ellipsize(text, paint, maxWidthPx, TextUtils.TruncateAt.END).toString()`;
  - `ascent` = `−paint.fontMetrics.ascent`, read into one reused `Paint.FontMetrics`.
- **`fun timelineTextScale(density: Density): Float`** = `with(density) { PlanMetrics.laneLabelSize.sp.toPx() } / (PlanMetrics.laneLabelSize.toFloat() * density.density)`. 1d calls it on main. It respects Android 14+ non-linear font scaling under the root cap of 1.3. Write no `11.sp` or `11f`: Phase 2's `featureDpLiteralsComeFromBoardMetrics` rejects a numeric `.sp`/`.dp` literal in `feature/*/src/main`, and the value is a token.

**Tests:** `TextPaintMetricsTest` (Robolectric, `@RunWith(AndroidJUnit4::class)`, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, `@Logic`):
- `measure(TICK, "11:11") == measure(TICK, "00:00")` (tnum);
- an ellipsized result fits within the maximum and ends with "…"; a fitting text comes back unchanged;
- `0 < ascent(TITLE, 11f) < 14f`;
- after `RuntimeEnvironment.setFontScale(1.3f)`, `timelineTextScale(Density(context))` is in `(1.0, 1.3]`; at 1.0 it equals 1.0 within 1e-3.

### Task 10: Shared resources (`feat(trip): add shared timeline resources`)

File `feature/trip/…/timeline/TimelineResources.kt`: `@Stable class TimelineResources(density: Float, dark: Boolean, palette: TimelinePalette, typefaces: TimelineTypefaces? = null)`. The first three parameters are exactly 1d's P6 call `TimelineResources(density, dark, palette)`; keep `typefaces` last and defaulted. It is created once per (density, dark), on main or at setup, never inside a draw (1d gets it from `TimelineResources.Cache`). It holds:
- fill, stroke and text `Paint`s (antialiased; one text paint per `TimelineTextStyle`, `isSubpixelText = true` and, for `TICK`, `'tnum' 1`, exactly as `TextPaintMetrics`, so replayed runs have the measured widths);
- **text typeface [R]:** 1d obtains the resources in composition, possibly before `TimelineTypefaces.load` has finished, yet every real drawing was measured with the loaded Figtree. So the typeface is resolved at replay, not at construction: before each `TEXT` op, `tf = typefaces ?: TimelineTypefaces.cachedOrNull()`; if `tf != null` and `paint.typeface !== styleFace(tf)`, set it (an identity check, no allocation). `null` (not loaded, or a test with `FakeTextMetrics` that never loads fonts) leaves the default typeface. Tests that need real glyphs pass `typefaces` explicitly;
- the dashed stroke paints for dash kinds 1–3 (`DashPathEffect` built once, intervals × density; kind 3 round cap);
- the dot `BitmapShader` paint (§5.3);
- the 12 hatch `Paint`s (§5.3), from `TimelineColors.hatchColorTable(palette)`; the gap colour is `TimelineColors.TRANSPARENT` (never `Color.TRANSPARENT` or a hex literal);
- glyph `Path`s parsed **once per process** with `androidx.core.graphics.PathParser.createPathFromPathData`, as `Array<Array<Path>>` indexed by `TimelineGlyph.ordinal`, mapped through an exhaustive `when` from `TimelineGlyph` to `LucideGlyphs.<Name>` (`TimelineGlyphPaths.kt`). Filled paths (none expected) use a fill paint;
- scratch `tilePath` and `outlinePath`, reused through `rewind()`.

Add `object Cache { fun get(density: Float, dark: Boolean, palette: TimelinePalette): TimelineResources }` (a small map keyed by (density, dark), main-thread only; it constructs with `typefaces = null`, so text resolves through `cachedOrNull()` as above). This matches 1d's "`TimelineResources.Cache` for `(density, dark)`".

**Tests:** `TimelineResourcesTest` (Robolectric NATIVE, `@Config(sdk = [36])`, `@Logic`):
- each of the 16 glyphs parses to a non-empty path whose bounds lie inside `[0, 24]²` (allow 1 unit for arcs);
- a 70 × 70 dp dotted rect at density 2.625 rendered to a bitmap has alpha > 0 at the 100 centres `(3.5 + 7i, 3.5 + 7j)` dp and alpha 0 at `(0, 0)`;
- the hatch paint gives alpha > 0 at perpendicular distance 1 dp and 0 at 5 dp from the corner line;
- `Cache.get(2.625f, false, TimelinePalette.Light)` twice returns the same instance; `dark = true` returns another.

### Task 11: Replay and render guards (`feat(trip): replay day drawings on a native canvas`) [ticks "Android side" and "Tests"]

File `feature/trip/…/timeline/DayDrawingReplay.kt`:
- `fun replayDayDrawing(canvas: android.graphics.Canvas, drawing: DayDrawing, res: TimelineResources)`: `save(); clipRect(0, 0, widthPx, heightPx)`; one `while` loop over `ops` with running cursors into each array; `restore()`. It allocates nothing: no lambdas, iterators, boxing, `String` building, `Path()`/`Paint()`/`RectF()`, and no measuring.
- `fun DrawScope.replay(drawing: DayDrawing, resources: TimelineResources) = drawIntoCanvas { replayDayDrawing(it.nativeCanvas, drawing, resources) }` (1d P6).

**Tests** (Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, real `TextPaintMetrics` with `TimelineTypefaces.load` run in `runTest(timeout = 60.seconds)`, and `TimelineResources(density, dark, palette, typefaces)` built with the **same** loaded typefaces; the bitmap is prefilled with `background`, then replayed). `:feature:trip` needs `testImplementation(project(":core:testing"))` (tier annotations, fixtures, `FakeTextMetrics`) and `testImplementation(project(":core:testing-compose"))` (`BoardCapture`, 0e OQ-0e-1); add them if absent.
- **`TypefaceResolutionTest`** (`@Logic`): replaying a drawing with one `TITLE` run through resources built with `typefaces` leaves the `TITLE` text paint's typeface `=== typefaces.semibold`; through resources built with `typefaces = null` after `load` completed, the same (via `cachedOrNull()`).
- **`ReplayGuardTest`** (`@Layout`, parameterized: one test case per combination): S3, windows **360, 411, 639, 640 and 841**, × **en, nl**, × **light, dark**, × font scale **1.0, 1.3** (40 cases, each < 5 s), density 2.625. Build every S3 day and replay it. Assert:
  1. every `TEXT` run (x to x + measured, its line box) inside a tile lies within that tile's frame; every gutter label lies within x ∈ [0, 44 dp];
  2. every tile frame lies within its lane rect;
  3. the op list's colors are a subset of the allowed set, computed in the test from the palette: tokens, tokens at the §5.2 sub-alphas, and pre-composites at 0.6/0.75 with and without sub-alpha;
  4. a pixel that no op's bounds cover equals `background` in the gutter or a lane gap and `surface2` inside a lane, exactly; choose sample points from the op bounds, never hard-coded. Inflate every stroked op's bounds by half its stroke width plus 1 px (round caps and antialiasing reach past the geometry) and every `TEXT` op's bounds to its full line box plus 1 px;
  5. windows < 640 have no `TEXT` op in any tile; 640 and 841 have at least one (positive control).

  Keep each case under the 5 s Robolectric budget: prepare S3 once per language in a companion `lazy` (outside the timed body), load the typefaces once, and reuse one `Bitmap` per case (`eraseColor(background)` between days) instead of allocating one per day.
- **`PrecompositedAlphaTest`** (`@Layout`): one test case per `BlockType` (known ones) × {marked `Done`, optional}: a single 120 dp tile on `surface2`, not compact, no ticks. (a) the pre-composited build; (b) the reference: the `precomposite = false` build with the tile's ops wrapped in `canvas.saveLayerAlpha(tileBounds, round(α·255))` — the only `saveLayer` in the codebase, test-only. Gate: every channel of every pixel within **1/255** (OQ-1c-9).
- **`TimelineAlphaPixelTests`** (`@Layout`; ports `coreBarDoesNotFadeTheTitleOrInterval`, `TimelineAlphaTests.swift:77–131`, method name kept): the first S3 day whose sleep lane holds a block of type `sleep`, and the first such placed block (iOS l.83–88); top 40, height 120, col 0, cols 2, not tiny, no continuation, `status = null`, `day.blocks = [it]`, no ticks, markers, changes or dividers, `layout.height = 200`; content 358, density 3, light, textScale 1.0, **`compact = false`** (iOS passes the default `false`; do not derive it from §6, which would make 390 compact and hide the text this test guards). Set the priority on both the `PreparedBlock` and the `PlacedBlock` (iOS l.111–112). Render once with priority `Core` and once `Recommended`: no pixel differs outside x ∈ [frame.minX − 1, frame.minX + 4 + 1] dp, and some pixel differs.
- **`TextPaintDayDrawingBudgetTest`** (`@Layout`, `@PerfBudget`): large60 at window 411, real metrics. Gate: every drawing ≤ 512 KiB. Report per-day and per-width build times as `DayDrawing metric: robolectric …` lines, not gated (OQ-1c-7).
- **Captures for review** (`@Capture`, `TimelineDrawingCaptureTest`): S3 days 1–2 at window **390** (the board width) and 411, en, light and dark, density 2.625. Add `fun capture(bitmap: Bitmap, name: String)` to 0e's `BoardCapture` (same `cf.capturesDir`, name regex and `cf.captureOnly` rules as the existing overload) and write `timeline/S3-day<i>-W<w>-<theme>`.

### Task 12: Record numbers (`docs(perf): record day drawing build numbers`)

- Append to `docs/perf-timeline.md`, section `## JVM budgets (1b, 1c), in quick` (1b created it; create the file with heading `# Timeline performance` and that section if it is absent): date, commit, `java -version` vendor and version, `uname -m`, and the `DayDrawing metric` lines from Tasks 6, 7 and 11. Write "host process, not a device claim". 1e re-sets the limits to about twice the measured means.
- In the same commit, append to plan.md "Open questions for the user" the plan-changing defaults of §10, one row each with the next free `OQ-A` number (check the table first: the 2026-11-16 review took OQ-A44–A47, and Phase 2 and other merged phases may have taken more), the default and "Affects 1c": OQ-1c-1 (7 dp dots; Task 10's commit already amended the plan.md 1c "8 × 8" text), OQ-1c-2 (hatch phase), OQ-1c-9 (tolerance, with the measured maximum), OQ-1c-11 (no background in the drawing), OQ-1c-17 (conditional clip) and OQ-1c-20 (runs narrower than "…" dropped). The others follow iOS or an existing plan decision and need no row.

---

## 8. Acceptance criteria

1. These targeted runs pass, each with its result file in `build/test-results/`:
   - `make targeted ONLY=":core:plan:DayDrawingTest :core:plan:TimelineCategoryStyleTest :core:plan:TimelineColorsTest :core:plan:DrawOrderGoldenTest :core:plan:TimelineAlphaTests :core:plan:DayDrawingRulesTest :core:plan:HitIndexTest :core:plan:TimelineTargetsTest :core:plan:DayDrawingCacheTest :core:plan:TimelineRenderingTests :core:plan:DayDrawingBudgetTest"`;
   - `make targeted ONLY=":feature:trip:TextPaintMetricsTest :feature:trip:TimelineResourcesTest :feature:trip:TypefaceResolutionTest :feature:trip:ReplayGuardTest :feature:trip:PrecompositedAlphaTest :feature:trip:TimelineAlphaPixelTests :feature:trip:TextPaintDayDrawingBudgetTest"`.
2. `make quick` is green and stays < 60 s warm.
3. `make check` is green with `pixel` booted (the orchestrator runs it if your sandbox cannot; report the exact command and output).
4. `make generators-check` is green and covers the icon generator (plus tokengen if Task 1 ran).
5. `make captures ONLY=timeline` writes 8 PNGs under `build/captures/timeline/`.
6. **Numbers**, all recorded in `docs/perf-timeline.md`: max ≤ 5 ms per day; ≤ 150 ms per width at 360, 411 and 841; largest drawing ≤ 512 KiB (expected 10–40 KB, `04 §9.4`; iOS peak 39,081 B); cache ≤ 4 MiB and ≤ 8 entries.
7. Konsist: `:core:plan` imports no `android.*` or Compose; no raw color literal outside generated files; no `saveLayer` in any `src/main`. With Phase 2 in the stack, `noRawColorsOutsideDesignSystem` and `featureDpLiteralsComeFromBoardMetrics` pass on the new `:feature:trip` files (no `Color.TRANSPARENT`, no `11.sp`).
8. The `DrawOrderGoldenTest` goldens and the captures have been reviewed against `Plan`, `PlanDark`, `PlanTimelineTop` and iOS screenshots 04, 30 and 34. Every difference is fixed or recorded with its board name in the phase report.
9. The five plan.md checkboxes of Phase 1c are ticked, in the commits named in §7.

## 9. Review checklist (Opus reviewer)

- **Draw order and geometry vs `TimelineRenderer.swift`.** Hand-trace l.192–439 against the S3 W841 golden: lanes, tick lines, labels, tiles, dividers, markers; quad corners, not `addRoundRect`; outline inset by bw/2 with radius R − bw/2; caffeine capsule radius; chevrons at 4 and h − 8; pill at x = 48.
- **Outline table.** Width follows the type (optional `no_caffeine` = 2 dp `[6,6]` in `foreground` at 0.5). `note` is solid; `Unknown` has none.
- **Clip.** `CLIP_TILE` appears exactly when the √ rule says (thin tiles, tiles lower than the icon, very small capsules) and not for normal tiles or a 30 × 20 capsule; the √ test uses the right corner radius; `CORE_BAR` uses `clipRect` + `drawPath`, never `clipPath`.
- **Scaling.** Only text sizes, line heights and the icon size scale with `s`. A scaled 76 dp threshold, padding or stroke is a bug.
- **Text.** A run with height < lineHeight is absent, not clipped (check the interval on a 40 dp tile at 841); so is a run narrower than "…". Baseline = top + ascent. The caffeine label is centred with one backing plate. Replay text paints match `TextPaintMetrics` (typeface resolved by identity, subpixel, `tnum` on `TICK`).
- **Compact rule.** Below 640 dp: no text in tiles; tick labels and divider pills still show; icons centred in both axes.
- **Colors.** Icons `style.foreground`, status glyph `foreground`, text `cardForeground` only when marked or optional; every in-tile color pre-composited when α < 1; sub-alphas 0.16, 0.12, 0.7, 0.4, 0.5, 0.4; tick lines `border` at 0.6. Look for α applied twice or a sub-alpha applied before pre-compositing.
- **Patterns.** Dot period 7 dp (plan's "8 × 8" is a slip, OQ-1c-1); anchored at the tile origin through `translate`; 12 hatch paints prebuilt, none created per draw.
- **Zero allocation in `replayDayDrawing`.** No `forEach`/`for (x in list)` over boxed collections, no `Path()`/`Paint()`/`RectF()`/`DashPathEffect` in the loop, no string concatenation, `measureText`, `TextUtils`, lambdas or `CharSequence` slices.
- **Glyphs.** Paths come from lucide 0.511.0 (the board bundle), not 1.66.0; `moon` is `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`. Parsed once per process.
- **Threading.** `TextPaintMetrics` per job, never shared; `getOrBuild` does not hold its lock during `build`; typefaces load off main.
- **Cache.** `PreparedDay` equality guard present; 512 KiB reject; bytes-then-count eviction; key includes `density` and `compact`; constructor limits have the plan's defaults.
- **Targets and hits.** Blocks, dividers, ticks; traversal `i` / `10000 + j` / `20000 + k`; frames from 1b's `timelineBlockFrame` (no re-implementation); `halfTouch` from `PlanMetrics.minTouchTarget` (not `touch`); the marker band `x ≤ 44 dp`; ties go to the first.
- **API names** match 1d §2 P5–P7 exactly.
- **Tests** follow AGENTS.md: JVM where possible; Robolectric only for `TextPaint` and pixels; one parameter combination per case; no `Thread.sleep`; `@PerfBudget` only on budget tests; a tier annotation on every Robolectric class; the golden is not blindly re-recorded.
- **Op-list walk.** Every op consumes its operands from the shared forward cursors only (§7 Task 2 "Cursors"); `walk` ends with every array exactly consumed. A typical bug is a `TEXT` op that reads `textStyles` with a different cursor from `texts`, or a `LINES` op whose `n` disagrees with its coordinate count.
- **Konsist in feature code.** `TimelineColors.TRANSPARENT`, not `Color.TRANSPARENT`; `PlanMetrics.laneLabelSize.sp`, not `11.sp`; no hex colour anywhere outside generated files.
- **Pre-composite reference.** `precomposite = false` builds emit raw tokens and the α = 1 hatch entry; the `saveLayerAlpha` wrapper exists only in that test.
- **Scope.** No composable, `LazyColumn`, `NowLine`, new string or board-less UI; no `Lucide.kt`/`ImageVector`.

## 10. Open questions (proceed on the default; add any new one to plan.md "Open questions for the user")

| # | Question | Default |
|---|---|---|
| OQ-1c-1 | plan.md 1c says "an 8 × 8 dot-cell `BitmapShader`"; iOS `dotStep = 7`, board `background-size: 7px 7px` | **7 dp** period; amend plan.md 1c in Task 10's commit |
| OQ-1c-2 | Hatch phase: iOS centres the 2 pt stripe on the line through the tile corner (l.248–251); the board CSS (and `04 §9.4`) runs it from 0 to 2 px | **Board/`04 §9.4`** (a 1 dp phase difference). iOS's own 15d entry (`I/plan.md` "15d: Timeline drawing") says "Category patterns and dashes stay exactly as the Plan boards draw them", so the board is the stated intent |
| OQ-1c-3 | Tile interval weight: iOS Figtree 600; board `TimelineBlock` 400 with tnum. Visible only at ≥ 640 dp | **iOS 600** |
| OQ-1c-4 | Info marker "i": iOS draws it left-aligned with its line box at the circle top; no board shows markers | **iOS parity** |
| OQ-1c-5 | Text plates: iOS square plates in the tile fill; for faded tiles the board uses a `card` plate with `rounded-sm` | **iOS** |
| OQ-1c-6 | Status glyph color: iOS `foreground`; board `currentColor` (text color) | **iOS** |
| OQ-1c-7 | Gate the Robolectric real-`TextPaint` build time? | **No;** gate bytes and report times. The time gate is the JVM fake (plan perf table) |
| OQ-1c-8 | Lucide version: lucide-static 1.66.0 (RN) or the boards' lucide-react 0.511.0 | **0.511.0 from the board bundle**, Phase 2's source (moon and skip-forward differ in 1.66.0; Phase 2 OQ-2-15). Phase 2 (wave 3) normally lands first and owns the generator; 1c only checks or adds its 16 names to the manifest. Only if Phase 2 is not in the stack does 1c create the subset generator in Phase 2's exact format, and Phase 2 Task 10 then extends it (orchestrator: tell the Phase 2 implementer in that case) |
| OQ-1c-9 | Pre-composite tolerance: plan 1/255 vs research 2/255 | **1/255.** If it fails only through double 8-bit rounding (diff = 2 on < 0.5% of pixels), use 2/255 and record the measured maximum in `docs/perf-timeline.md` |
| OQ-1c-10 | Cache key adds `density` and `compact` to the plan's six fields, plus a `PreparedDay` equality guard | **Yes** (iOS `RenderKey` parity); 1d already constructs this shape |
| OQ-1c-11 | iOS fills `background` inside the drawing | **Omit** (`04 §9.4`); 1d's host paints it |
| OQ-1c-12 | Divider accessibility frame: the 18 dp pill (iOS) or ≥ 48 dp | **The pill frame;** not interactive |
| OQ-1c-13 | Phase 2 is not a declared dependency of 1c (plan: 1c depends on 1b, 0b), although Phase 2 is wave 3 and usually in the stack | 1c is self-contained: Task 8 reuses Phase 2's generator when present, else creates the subset |
| OQ-1c-20 | Text runs narrower than "…": iOS clips the full line to its rect (l.143–144); the replay has no per-run clip | **Drop the run** (§5 text primitive); at most a one-glyph sliver differs |
| OQ-1c-21 | Marker/zone-change tap band: iOS `touch/2` = 22 (DayTimelineView l.107, 110) | **24 dp** (`minTouchTarget/2`), consistent with OQ-T5 and OQ-1b-11 |
| OQ-1c-22 | 1d P6 constructs `TimelineResources(density, dark, palette)` and caches by (density, dark), but text must replay in the measured typeface, which may load after composition | **`typefaces` is an optional 4th parameter; replay resolves `TimelineTypefaces.cachedOrNull()` per `TEXT` op by identity** (Task 10). No 1d change needed |
| OQ-1c-14 | Tick labels: iOS Figtree 400 without tnum (l.123); board and `04 §3.7` tabular | **tnum** (board look; no layout effect because labels are right-aligned in a fixed rect) |
| OQ-1c-15 | Baseline: iOS `top + ascent` (l.169); `04 §9.4` centres the font box in the line | **iOS** (≈ 0.4 dp difference at s = 1) |
| OQ-1c-16 | plan.md lists `lineHeight` in `TextMetrics`; iOS derives it from `PlanMetrics` × scale | **Derived** (`14·s`); `TextMetrics` has `measure`, `ellipsize`, `ascent` |
| OQ-1c-17 | iOS clips every tile's content to the tile path; a `clipPath` per tile costs on the render thread | **Conditional `CLIP_TILE`** by the exact √ test (§5 step 4.6): pixel-identical to always clipping |
| OQ-1c-18 | iOS also compares `layout` on a cache hit; 1d's API is `(key, day)` | **No layout compare:** 1b derives `PreparedDay.blocks`/`ticks` from the layout, so a layout change changes the day |
| OQ-1c-19 | `revision` changes with `detailVersion` even when the content is identical, which rebuilds every visible day | **Keep it** (plan key); the rebuild is lazy and within budget. 1e may drop it if traces show the cost |

## 11. Changes from the first version of this brief

- The outline width now follows the type (optional `no_caffeine` = 2 dp `[6,6]`); the first version had 1 dp for every optional tile.
- Added `CLIP_TILE` (iOS l.292–293 clips the tile content) with a conditional, exact rule.
- Baseline is iOS `top + ascent`; `TextMetrics.baselineOffset` became `ascent`.
- Lucide source moved from lucide-static 1.66.0 to the board bundle's 0.511.0, generated in Phase 2's format.
- The hatch table has 12 entries (the 4 hatch foreground slots), not 15.
- `CORE_BAR` replays with `clipRect`, not `clipPath`.
- `Unknown` type draws no glyph; `CheckStatus.Unknown` draws `SKIP_FORWARD`.
- Names aligned with 1b/1d: `PlanMetrics.minTouchTarget`, `TimelineRect`, `PreparedPlan.revision`, the `DayDrawingCache` constructor limits, `@Stable TimelineResources`, `TimelineTypefaces.load(context)`.
- Task 5 now ticks the "DayDrawingBuilder.kt" checkbox; the first version left it unticked.
- `TimelineTargetsTest` no longer re-ports the iOS test that 1b already ports.
- Captures go through a `BoardCapture.capture(bitmap, name)` overload.
- Board line references corrected (`Plan.dc.html` l.143, 147, 216–379).
- Third pass (2026-11-16, re-verified against the iOS sources and current sibling briefs; no drawing rule changed): the hatch gap colour is `TimelineColors.TRANSPARENT` and the text scale reads `PlanMetrics.laneLabelSize.sp`, so the new `:feature:trip` code passes Phase 2's `noRawColorsOutsideDesignSystem` and `featureDpLiteralsComeFromBoardMetrics`; the op list's forward cursors are explicit (`texts`/`textStyles` share one); `FakeTextMetrics.ellipsize` mirrors `TextUtils` (`""` when "…" does not fit) and `:core:testing` gets its `:core:plan` dependency; the `precomposite = false` reference uses the α = 1 hatch entry; Task 8 names Phase 2's `vectors`/`vectors-check` targets and test discovery line, and has no commit when all 16 glyphs exist; `ReplayGuardTest` inflates stroke and text bounds and reuses its fixtures and bitmap; Task 12 adds the plan-changing defaults to plan.md's open questions; OQ-1c-2 cites the iOS 15d "as the Plan boards draw them" rule.
- Checker pass (2026-11-16): runs narrower than "…" are dropped (OQ-1c-20); the clip examples were corrected (a 30 × 20 capsule needs no clip by the √ rule; a 16 × 16 one does); `TimelineAlphaPixelTests` pins `compact = false` and the iOS block selection; `TimelineAlphaTests` takes `placed[sleep].first()`; `TimelineResources` matches 1d P6 (`typefaces` optional, `Cache.get(density, dark, palette)`, typeface resolved at replay, OQ-1c-22) with a new `TypefaceResolutionTest`; replay paints are subpixel like the metrics; the 24 dp tap band is marked as an Android delta (OQ-1c-21); Task 8 expects Phase 2's generator and says how to add missing names; `docs/third-party.md` is created if absent; test dependencies on `:core:testing`/`:core:testing-compose` are named.
