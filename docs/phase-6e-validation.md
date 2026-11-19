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
