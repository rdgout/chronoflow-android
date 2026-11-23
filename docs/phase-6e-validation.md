# Phase 6e validation

Branch preflight: `feat/phase-6e-trip-wide`. Upstream names and all P1 metrics are present. The brief refresh records the day-tag owner and the allocation-free px pin adaptation. OQ-A305–OQ-A323 record the nineteen brief defaults; no new copy.

## Review round 1 — finding 1

Starting HEAD 2bf39214. The day-command collector retained its opening TripPageFrame across breakpoint changes. It now reads the current layout through rememberUpdatedState, beside the existing current geometry/appearance values.

- Test first: `make targeted ONLY='feature:trip:TripPageResizeTest#listModeUnfoldThenJumpUsesTheWideTimelineIndex feature:trip:TripPageResizeTest#wideFoldThenToggleToListAndJumpUsesTheListIndex'` failed both cases before the fix (8 s Gradle wall): the unfolded target header was absent; the folded/list target was at y=365 instead of scroll top y=168.
- These checks use S1 and assert that the list and timeline header indices differ. The initial S3 checks passed because its first day has exactly one list row, making both day-1 indices equal. The final tests use the existing window/container resize fixture without extra hooks.
- After the fix, `make targeted ONLY='feature:trip:TripPageResizeTest'`: PASS, all 9 cases, 7.04 s host wall, no budget overruns.

Red/green XML and timings: `build/test-results/phase-6e-review1/finding1/`. Profiles and docs/perf-timeline.md remain untouched.

## Review round 1 — finding 2 / OQ-A311

The wide invalidation guard now disables mainClock.autoAdvance, dispatches each raw delta on the UI thread and advances exactly one frame. Its four pin assertions follow the final delta in each group without settling or advancing another frame. The original settled invalidation/day-boundary checks run afterwards, and finally always restores automatic advancement.

- `make targeted ONLY='feature:trip:TimelineInvalidationTest#wideScrollInvalidatesNoStaticLayerOrRow'` with the tightened test and unchanged production pin: PASS (6 s Gradle wall, 2.949 s test time).
- Negative control: a temporary placement fault explicitly deferred the pin offset through frame callbacks. The same command failed the first pin assertion, y=392 versus compact-band bottom y=395 (5 s Gradle wall). A simpler measure-time offset variant passed and was not used as proof of lag sensitivity. Both mutations were removed in finally; no hooks or product changes remain from this finding.
- Restored production source, `make targeted ONLY='feature:trip:TimelineInvalidationTest'`: PASS, all 12 cases, 7.57 s host wall, no budget overruns. Static/clock/targets/header/list/row invalidation and the within-day composition budget remain green.

OQ-A311 result: retain the per-item pin implementation; the hosted first-frame check passes and rejects an explicitly delayed offset. The page-overlay fallback is unnecessary for this evidence. This is not emulator visual or Perfetto evidence.

Mutation XML and restored-source results: `build/test-results/phase-6e-review1/finding2/`.

## Review round 1 — finding 3

PinnedDayList no longer caches intrinsic content height by width/revision. It measures the existing verticalScroll container once with a zero minimum height and the live pin cap as maximum height. The scroll container wraps short content and scrolls taller content, so child-only row growth updates the viewport immediately. The unused revision parameter/cache and intrinsic-height walk were removed. The pin path still uses cached item keys, its cached mutable position and primitive sizes; this change adds no per-frame objects or state publication of measured sizes.

- Test first: `make targeted ONLY='feature:trip:PinnedDayListContentHeightTest'` failed before the fix (5 s Gradle wall): the last row ended at 217.5 dp while the viewport retained its original 144.8 dp height.
- The guard uses a Dutch profile, the 1.3 font cap and a 280 dp list column. A deterministic child row grows after first layout while the rows lambda, width and parent composition remain unchanged. It proves the content still fits below the cap, then checks the last row's unclipped bottom is within the viewport. This isolates measurement invalidation without depending on a particular string wrapping.
- The new guard passes after the fix (5 s Gradle wall, 1.870 s test time).
- `make targeted ONLY='feature:trip:PinnedDayListContentHeightTest feature:trip:PinnedDayListTest feature:trip:PinnedDayListFontScaleTest feature:trip:WidePinSourceTest feature:trip:WideDayRowTest feature:trip:WideDayRowBreakpointTest feature:trip:TimelineInvalidationTest feature:trip:TripPageWideGuardTest'`: PASS, all 56 cases, 11.19 s host wall, no budget overruns. This includes the tightened first-frame pin guard and unchanged invalidation budgets after the measurement change.
- Formatting required the fixture child to accept a value-provider lambda rather than State directly (compose:state-param-check). `make format` then passed (11 s Gradle wall); the final guard rerun passed (4.40 s host wall).

Red/green XML and timings: `build/test-results/phase-6e-review1/finding3/`. No profile regeneration or perf tier is run in this review round; device follow-up remains `make perf-timeline` and, for the orchestrator-owned generated profiles, `make baseline-profile`. Neither profiles nor docs/perf-timeline.md are changed here.

Final review-round checks: `./gradlew :app:lintDebug :feature:trip:lintDebug` PASS (14 s Gradle wall). `make quick` ran once and passed (55.65 s host wall), including all JVM/Robolectric, tooling and timing checks with no budget overruns. Evidence: `build/test-results/phase-6e-review1/`.

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

## Task 10

- `./gradlew :benchmark:compileBenchmarkReleaseKotlin`: PASS (2 s), compiling both benchmark and profile generator changes.
- `python3 -I -m unittest discover -s Scripts/tests -p test_check_benchmarks.py`: PASS, 53 tests / 2.454 s, including required wide results, RSS failure, missing report metric, incomplete iterations and independent wide history/report-only CPU values.
- `python3 -I -m unittest discover -s Scripts/tests -p test_perf_timeline.py`: PASS, 16 fixture tests / 3.716 s. These use fake adb/Gradle and never access the emulator. The failure cleanup test verifies size reset before functional prep.
- `python3 -I -m unittest discover -s Scripts/tests -p test_perf_startup.py`: PASS, 8 fake runner tests / 1.976 s.
- `bash -n Scripts/perf-timeline.sh Scripts/baseline-profile.sh`: PASS. `make format`: PASS (11 s).
- Deferred by explicit user instruction: `make baseline-profile`, `make perf-timeline`, the three-fling screen recording/Perfetto pin-lag inspection and the StrictMode debug smoke count. Exact commands and every requested metric are listed in the Phase 6e entry of `docs/perf-timeline.md`. No performance numbers or regenerated-profile claim is made.

## Task 11 and final checks

- `bash -n Scripts/fold-check.sh` and `python3 -m py_compile Scripts/fold_check.py`: PASS. The fold script was **not run**. It is on request only and is not wired into quick/check/matrix.
- `python3 -I -m unittest discover -s Scripts/tests -p test_fold_check.py`: PASS, 14 parser tests / 0.004 s. Fixtures reject a missing sheet, changed/far-away day, unchanged width mode, absent activity identity and unknown device states. Tests also cover stable block selection and duplicate relaunch event counting, with no adb access.
- Brief refresh in this task: replace invalid `CF_AUTH_TEST_ID=fold-check` with canonical UUID `019a56e0-0000-7000-8000-00000000006e`; keep the landed parser unchanged. A preparatory seed observes the opening day before the actual sheet-seeded fold journey. All-window dumps use `--windows` so sheet and page can be checked together.
- `./gradlew :app:lintDebug :feature:trip:lintDebug`: PASS, 34.60 s wall (Gradle 34 s), warnings treated as errors.
- `make quick`: PASS **once**, 55.37 s wall. The quick tier records 3595 passing cases, zero budget overruns; build logic has 37 passing cached cases; serial timing has 6 passing cached gates. All requested new classes appear in `build/test-results/quick/timings.json`. The tooling stage passes 158 + 25 + 49 + 9 tests (20.395 s combined test time). Existing first-sandbox warm-ups use their declared 20 s cap.
- New warmed case maxima: TripPageFrameTest/TripScrollTrackerTest/WidePinSourceTest ≤0.001 s; window helper 0.071 s; wide row 0.023 s; pin/font 0.027 s; remote 0.024 s; menu 0.044 s; manifest 0.006 s; resize 0.058 s; invalidation 0.183 s.
- The ready-plan invariant filters out any day lacking a DayLayout in `preparePlan`; no missing-layout day reaches the shared row. No earlier-phase fallback behavior was changed.

No real emulator tier was run in this phase; the brief adds no ordinary pixel emulator class. Perf/profile/T2 commands were explicitly deferred by the user. No new dependency or native library was added. `pixel` was left running; no emulator was stopped, resized or wiped by the agent. No push, merge, rebase or history rewrite occurred after a subsequent task started. The preflight docs commit was re-signed before Task 1 began; each task then has its own signed commit.

One optional host diagnostic was sandbox-blocked:

```text
$ ps -axo pid,etime,command | rg 'git log|gpg --|gpg-agent'
zsh:1: operation not permitted: ps
```

Every implementation commit was created successfully with `git commit -S`; commit objects were also inspected for `gpgsig -----BEGIN PGP SIGNATURE-----`. The untracked later-phase briefs supplied with the checkout remain untouched.
