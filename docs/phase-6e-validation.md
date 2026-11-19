# Phase 6e validation

Branch preflight: `feat/phase-6e-trip-wide`. Upstream names and all P1 metrics are present. The brief refresh records the day-tag owner and the allocation-free px pin adaptation. OQ-A305–OQ-A323 record the nineteen brief defaults; no new copy.

## Task 1

- `make targeted ONLY="core:testing-compose:WindowSizeFakeHostTest core:testing-compose:ProfileHostTest konsist:KonsistRulesTest#featureSizesComeFromTheWindow konsist:KonsistRulesTest#featureSizesComeFromTheWindowRejectsPlantedViolation"`: first run caught two fixture expectations (inherited top inset and fractional-density pixel rounding).
- Focused rerun of both affected methods and the two Konsist methods: PASS (4 helper cases, 2 Konsist methods; Gradle 4 s + 5 s). Helper cases 0.027–1.757 s, Konsist 0.007–0.066 s. Evidence copied to `build/test-results/phase-6e/task1/`.
- Window size uses public Compose `LocalWindowInfo.containerSize` at `CfRoot`, beside the existing inset provider. It shares the shell's window bounds source; `WindowPolicy.wideBreakpointDp` remains the shell policy. The profile helper supplies the same local. No feature configuration-size reads existed.
- Kotlin daemon cannot create its timestamp in the sandbox (`java.nio.file.FileSystemException: /Users/rickgout/Library/Application Support/kotlin/daemon/kotlin-daemon-client-tsmarker….tmp: Operation not permitted`); Gradle's in-process fallback succeeds.

## Task 2

`make targeted ONLY="feature:trip:TripPageFrameTest"`: PASS, 13 independent parameter cases, Gradle 4 s. Height is absent from the pure frame API. Narrow content floors the timeline width at zero. Task 1 app compilation also passed (4 s); `make format` passed (11 s).

## Task 3

`make targeted ONLY="feature:trip:WideDayRowTest feature:trip:WideDayRowBreakpointTest feature:trip:DayTimelineTest feature:trip:ListInvalidationTest"`: PASS, 13.64 s wall (Gradle 13 s), no budget overruns. Initial compilation needed the `DpRect.width` extension import. WideDayRowTest: 2 cases, 0.048 s; WideDayRowBreakpointTest: 4 cases, 0.12 s; DayTimelineTest: 6 cases, 0.165 s; ListInvalidationTest: 124 cases, 7.945 s. `make format`: PASS (11 s).

The day tag moved from `DayTimeline` to `WideDayRow`; the standalone timeline test host supplies its own tag. Selection changes are read only from the cached border draw. Phone outline call sites retain the false default.

## Task 4

- `make targeted ONLY="feature:trip:WidePinSourceTest feature:trip:PinnedDayListTest feature:trip:PinnedDayListFontScaleTest feature:trip:WideDayRowTest feature:trip:WideDayRowBreakpointTest"`: 13/14 PASS (Gradle 7 s). The font-cap assertion exposed platform `sp.toDp()` producing 36.19 dp instead of the fixed linear 46.8 dp required by the brief.
- `make targeted ONLY="feature:trip:PinnedDayListFontScaleTest#bandHeightFollowsTheFontScale"`: PASS (Gradle 5 s; test 2.171 s including Robolectric warm-up) after using generated dp metrics × the existing capped fontScale for both band and pin. This adaptation is recorded in the brief; phone scaling and threshold are unchanged.
- `make format`: PASS (11 s). The no-lag Perfetto/three-fling recording remains with the user’s deferred wide benchmark. Pin formula tests cover negative viewport start, bottom inset, invisible day, density conversion and cached result identity.

## Task 5

- `make targeted ONLY="feature:trip:WideRemoteStatesTest feature:trip:TripPageStatesTest core:ui:CfCircularProgressGuardsTest"`: the feature run passed 18/22 cases (Gradle 7 s); four wide edge assertions exposed pixel rounding. The second module did not run after this failure.
- `make targeted ONLY="feature:trip:WideRemoteStatesTest#remoteLoadingUsesTheWideSkeleton feature:trip:WideRemoteStatesTest#remoteOfflineCardSpansTheGutter"`: PASS (four en/nl cases, Gradle 6 s). The skeleton timeline now fills remaining width so rounded gutters cannot shrink the fixed list column; offline edges compare against measured viewport and rounded padding, within 0.5 px.
- `make targeted ONLY="core:ui:CfCircularProgressGuardsTest"`: PASS (Gradle 12 s). The new wrapper delegates to the M3 system spinner.
- Screen tests check the downloading semantics structure; they do not assert copy (user/AGENTS takes precedence over the brief's copy assertion). Existing generated en/nl resources supply every label. Wide page bottom padding retains iOS's 16 dp; side gutters are 20 dp.

## Task 6

- `make targeted ONLY="feature:trip:DayMenuWideTest feature:trip:TripPageWideGuardTest"`: menu and 27/29 layout cases initially passed (Gradle 8 s). Compile preparation corrected the ComposeContentTestRule import (the rule factory is in v2, its interface is in junit4).
- `make targeted ONLY="core:testing-compose:GuardsSelfTest#scopedGuardsIgnoreTheScrimAndStillRejectUnsafeContent feature:trip:TripPageWideGuardTest#widePageFitsAndKeepsItsWindowPolicy"`: scoped guard control PASS (5 s); the full-window ink assumption then rejected the two cropped sheet images. The safe-area control has a non-zero top inset and deliberately rejects an unsafe control.
- `make targeted ONLY="feature:trip:TripPageWideGuardTest#widePageFitsAndKeepsItsWindowPolicy"`: PASS, all 29 independent guard cases (Gradle 10 s). Sheet interactivity is scoped to its content, keeping the native dismiss scrim outside content geometry checks. Ink uses the full-page root. Step cap and centring are checked on the real sheet. Ready cases explicitly guard the right inset and gutter, including landscape's non-zero 48 dp edge.
- A requested parameter-suffix filter produced `No tests found for given includes`; the supported method filter above ran the affected method instead.
- `make captures ONLY=PlanWide,PlanRemoteWide`: PASS, exactly once, 17 PNGs (Gradle 13 s, capture cases 2.109 s total). `make format`: PASS (11 s). No stored references.

Local visual review inspected all 17 outputs against the six boards' HTML and the iOS defaults. Board differences retained by the brief: all PlanWide variants use 40 dp/hour, no Steps label, iOS spacing and compact title/lanes, generated fixture copy/times/dates, IATA title and M3 chrome. PlanWideDayMenu_title captures the title with the menu closed; DayMenuWideTest separately opens and selects the native menu. PlanRemoteWide uses the 22 dp iOS bar radius, static fills and the M3 spinner. PlanRemoteWideOffline uses generated catalog copy and the known S2 fixture summary. The inherited screen fixture leaves page action callbacks disabled; actual TripPage supplies them. Independent visual acceptance remains with the user.

The brief's menu geometry assertion now uses `trip.headerItem.<id>` (outer lazy item), rather than the padded `trip.dayHeader.<id>` title. OQ-A323's wording now reflects the Stack check: landed phone rows have no selection outline.

PNG files under `build/captures/`:

- `PlanRemoteWideOffline_board669-xxl_nl_dark.png`
- `PlanRemoteWideOffline_board669_en_dark.png`
- `PlanRemoteWideOffline_board669_en_light.png`
- `PlanRemoteWideOffline_board669_nl_dark.png`
- `PlanRemoteWideOffline_board669_nl_light.png`
- `PlanRemoteWide_board669-xxl_nl_dark.png`
- `PlanRemoteWide_board669_en_dark.png`
- `PlanRemoteWide_board669_en_light.png`
- `PlanRemoteWide_board669_nl_dark.png`
- `PlanRemoteWide_board669_nl_light.png`
- `PlanWideDark_board669_en_dark.png`
- `PlanWideDayMenu_title_board669_en_light.png`
- `PlanWideScrolled_board669_en_light.png`
- `PlanWide_board669-xxl_nl_light.png`
- `PlanWide_board669_en_light.png`
- `PlanWide_foldInner_en_light.png`
- `PlanWide_landscapeSideNav_en_light.png`

## Task 7

`make targeted ONLY="feature:trip:TripScrollTrackerTest feature:trip:TripListIndexTest feature:trip:TripScrollMathTest feature:trip:ListModeIndexTest"`: PASS, 25 JVM cases (Gradle 5 s). Tracker 12 cases / 0.003 s; other class totals 0.001–0.216 s. The common index gains key-to-day lookup rather than parsing row ids, plus the timeline index seam. No display-link retry loop is ported. `make format`: PASS.

## Task 8

- `make targeted ONLY="feature:trip:TripPageResizeTest app:MainActivityConfigChangesTest"`: six of seven resize methods PASS (Gradle 8 s); trip-actions had no card in the generic fixture (`{}`).
- `make targeted ONLY="feature:trip:TripPageResizeTest#legendAndTripActionsSurviveResize app:MainActivityConfigChangesTest"`: PASS (Gradle 6 s + 4 s). The test now seeds the S3 card. Manifest flags include every A8 in-place change and exclude locale/fontScale/layoutDirection; production manifest unchanged.
- `make targeted ONLY="app:RootContentConstructionTest"`: PASS (Gradle 19 s) after providing the root's new LocalWindowSize in the test host. `make format`: PASS (12 s).
- Hosted resize evidence covers 411 → 841 → 411, exact day top (±1 px), saved offset (<1 dp), scroll/day semantics identity, step/legend/actions survival, phone-list preference and day-top restoration, density conversion, summary position, and accepted negative requestScrollToItem offsets. The root test viewport is 1000×1100 dp so the 841×914 dp fake still fits after the 2.625 → 3.0 density increase; qualifiers never change mid-test.

## Task 9

- `make targeted ONLY="feature:trip:TimelineInvalidationTest feature:trip:TripPageResizeTest"`: 18/19 PASS (Gradle 12 s). One global composition was a legitimate second-day lazy entry during the new measurement. The wide scroll now starts at 360 dp, asserts both measured days are already composed, then performs the required 120 deltas with four explicit pin checks. Wide list inputs were narrowed to clock/selection/rows, and the frame is remembered independently of anchor chrome.
- `make targeted ONLY="feature:trip:TimelineInvalidationTest#wideScrollInvalidatesNoStaticLayerOrRow"`: PASS (Gradle 7 s); zero composition passes within day, zero per-key static/header/clock/targets/list/row changes, positive anchor/compact update across the boundary.
- Planted a temporary layoutInfo read in WideDayRow composition: the same command failed as required (`no composition during within-day scroll: {} expected:<0> but was:<240>`, Gradle 5 s). The mutation was removed in a finally block and its failing XML archived at `build/test-results/phase-6e/task9/planted-composition-read.xml`.
- Restored-source rerun of that method: PASS (Gradle 5 s, test 2.833 s including Robolectric warm-up). Existing phone assertions remain unchanged; the host now gives each width an explicit in-place fake inside a larger root. Wide prewarm keys use the actual timeline column. `make format`: PASS (13 s).
