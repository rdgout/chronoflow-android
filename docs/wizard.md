# Wizard shell and Where acceptance

Phase 9c, 2026-11-20. Implementation and device checks are complete; the rendered-board comparison and profile/performance measurements remain for the orchestrator. [Brief](phases/phase-9c-wizard-shell-where.md).

## Shell metrics

Values are dp unless marked otherwise. Geometry uses `LocalSafeInsets`, window constraints and IME padding. The font-scale cap remains the root's 1.3.

| Element | Value |
|---|---|
| Header | 54 high; 10 horizontal inset; 48 targets; back 22, close 18, stroke 2.4 |
| Progress | safe top + 60; 4 high; 20 horizontal inset; 250 ms, snap under Reduce Motion |
| Content | safe top + 76; 12 top and bottom padding; 20 horizontal; 14 item gap |
| Title | safe top + 88; Fraunces 30/36; title/subtitle gap 6; extra group gap 4 |
| Column | full width below 669; centered 520 from 669 |
| Footer | fixed; 20 horizontal and 12 vertical; 52 CTA; disabled route alpha 0.4 |
| Step and overlay transitions | 300 ms slide; none under Reduce Motion; root routes still have no transition |
| Route card | radius 16; border 1; rows at least 68; label gap 2 |
| Swap | 40 visual, 48 target; end inset 14; glyph 16, stroke 2.2 |
| Stop remove | 36 visual, 48 target; glyph 14, stroke 2.4 |
| Search sheet | safe top + 56; native width cap 640; gutter 20; native drag handle |
| Search header | title capped at 220; target at least 48 |
| Search field | 40 visual in 48 target; radius 12; inset 12; glyph 18, stroke 2.2 |
| Airport results | at least 60; code column 44; card radius 16 |

Measured native Robolectric sheet top: **106.286 dp** on `pixel8` and `foldInner` (50.286 safe top + 56). Widths: **410.667** and **640 dp** respectively. These are fixture geometry measurements, not physical-device performance claims.

## Settled values versus boards

| Board value | Android | Why |
|---|---|---|
| Glass circles: back and × at 44 px, swap at 40 px | Back and × use `CfIconButton(style = Ghost, size = Touch)`: no fill, glyph `foreground`, 48 dp target. Swap is a 40 dp circle filled `wizardPickerFill`, inside a 48 dp target. | OQ-A6 (no glass) [R]; OQ-9c-7 |
| Back glyph: chevron `m15 18-6-6 6-6`, 22 px, stroke 2.4 | lucide `chevron-left`, 22 dp | board [F] |
| × glyph: 18 px, stroke 2.4 | lucide `x`, 18 dp | board [F] |
| Where row min height 68 | **68 dp** (iOS token `wizardAirportHeight` 76 is not used) | board over iOS [R]; OQ-9c-9 |
| `.ios-seg` segmented control at 44 px | `CfSegmentedButtons` with M3 typography and its check icon (Phase 2). The visible height is M3's; the row is ≥ 44 dp. | plan A9 / Material mapping (`plan.md` l.423: `SingleChoiceSegmentedButtonRow`); the designer's note "on Android it takes our colours" is in `DC/canvas.json` (the 2 Nov system-components request), not in WizardCopy [F] |
| iOS alert at 300 px wide, radius 34, two buttons | `CfAlertDialog`: M3 `AlertDialog` with `popover` container; confirm "Discard" in `destructive`, dismiss "Keep editing" in `dawnText` | plan Material mapping [F] |
| Search field visible height 40 | Visible box **40 dp**, inside a 48 dp clickable band (4 dp above and below, taken from the 14 dp gaps, so the box sits where the board draws it) | A10 48 dp targets [R]; OQ-9c-8 |
| Sheet grabber 36 × 5 | M3 `BottomSheetDefaults.DragHandle()` | Phase 2 OQ-2-3 [F] |
| Search placeholder: iOS "Search airports" | "Airport or city" / "Luchthaven of stad" (WizardCopy) as Android-only copy | WizardCopy is the binding copy deck [R]; OQ-9c-2 |
| `TripAirportSearch` rows (Fraunces code 20/28, 64 high, radius 22, count line) | `WizardSheet` rows (code 15/700, 60 high, radius 16, no count line) | `WizardSheet` is newer and iOS built it [R]; OQ-9c-4 |
| Board progress on the same-airport state and sheet/alert backdrops (`WizardWhereSame` 15 despite an incomplete route, `WizardAirportFrom` 15 over the empty Where, `WizardAirportHome` and `WizardStopSearchHome` 55, `WizardDiscard` 30) | The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 15 on Discard | progress comes from 9a, never per board [R]; OQ-9c-6 |
| `WizardDiscard` backdrop = Outbound | In 9c the alert is captured over Where with something entered; 9d re-captures it over Outbound | Outbound is 9d [R]; OQ-9c-5 |
| `WizardAirportHome` backdrop = flight-home **times** | Captured over the flight-home route (`WizardHomeRoute`), which is where the To row opens it | iOS opens it from the route [F]; times are 9d |
| No audience notice on the Where boards | Built per iOS NH5, below the route card on Where (not on the home route) | iOS user decision, no-health-data [F `MemberWizardRouteView.swift` l.23–33]; OQ-9c-1 |
| Board CTA bottom 34 (iPhone home indicator) | Footer bottom padding is 12 dp above the larger of the safe bottom inset and the IME | iOS `wizardShell` footer `padding(.vertical, space3)` [F] |
| Board title/sub group `margin-bottom: 4` | The gap from title group to card is 14 + 4 = **18 dp** (iOS has 14) | board over iOS [R] |
| Stop-question text 17/22 at weight **600** (iOS draws 400) | `CfTheme.type.wizardButton` (Figtree600 17/22) | board over iOS [R] |


The search glyph uses generated `mutedForeground`: the board's `rgba(120,120,128,0.8)` has no matching generated color. This is the explicit fallback in brief §5.5. The M3 segmented group's shared 1 dp border is exempt from the no-overlap guard; its truncation, inset and target guards remain active.

## Back, close and persistence

| State/action | Result |
|---|---|
| Sheet + back, swipe or Cancel | Hide the native sheet; cancel search and clear its stored sheet |
| Dialog + back or Keep editing | Hide the dialog; keep draft values |
| Where + back, or × | Changes open the dialog; an untouched draft closes immediately |
| Later step + back/predictive back | Model `popNavigation`; the Nav3 stack is derived from the model |
| Discard | Guard repeats with `closing`; await model discard and recovery deletion before the one exit |
| Discard failure | Reset closing and remain on screen; caller cancellation propagates |
| Activity recreation | Entry-owned wizard ViewModel is retained; dialog is memory-only and is absent from recovery |
| Overlay closes | Clear its store; keep the already-created VM during the outgoing slide; rotate owner on the next opening |
| Quiet recovery | At most once per session epoch, off main on injected IO; skip capture seeds |

`TripWizardViewModel.onCleared()` retains the 9b `WizardRecoveryWriter.close()` path. Form and sheet values remain in the model/store. Only the 9b instance id belongs in saved state; query and debug initial presentation stay in memory. The wizard carries no credentials or link secrets.

## Search states

| Shared search state | Presentation |
|---|---|
| Hint | Minimum-two-letters hint |
| Searching | Status plus three static skeletons; stale rows omitted |
| Results | Preserve service order; filter with `airportIsAllowed`; zero allowed rows becomes Empty |
| Empty | Empty title and suggestion body |
| Failed | Error card and enabled retry |
| Throttled | Same error card; retry disabled until retryAt, driven by one delay |

The shared 9b service owns debounce, pacing, cache, cancellation and 429 handling. The feature adds no pipeline. Opening, choosing and canceling reset its query. One choice is accepted per opening. A test-only wrapper observed **0 header and 0 route-card recompositions for ten query edits**; a separate JVM guard verifies search changes emit neither chrome nor route UI state. Airport country names use the supplied application locale.

## Seeds

`CF_WIZARD_AUTO_OPEN=1` uses the guarded launcher. `CF_WIZARD_BOARD_CAPTURE=<name>` uses a debug-only memory draft store, fixed wizard time **2026-10-01T09:00Z**, fixed airport results and no durable recovery writes. `TripActions` is ignored. Suffixes `DarkNL`, `Dark`, `NL` select presentation after stripping for draft lookup. A separate advancing search clock avoids a pacing spin with frozen wizard time.

Base draft: AMS → NRT, entered, habitual sleep 23:30–07:15, intermediate chronotype. The airport fixtures retain countries and timezones. Query and discard-dialog seed presentation are Android memory-only additions to the pinned seed.

| Board | Draft |
|---|---|
| `WizardWhere`, `WizardWhereDark`, `WizardWhereNL` | step `whereTo`, `fromLastTrip = true` |
| `WizardWhereEmpty*` | empty form, `entered = false`, `fromLastTrip = false` |
| `WizardWhereSame` | To = AMS, `fromLastTrip = true` |
| `WizardWhereStop*` | step `whereTo`, `fromLastTrip = true`, DXB inserted before the destination of leg 0; all times cleared |
| `WizardWhereStops*` | as `WizardWhereStop`, plus BKK inserted before the destination of leg 1; all times cleared |
| `WizardWhereLimit` (no board) | as `WizardWhereStop`, then while `legs.size < 16` insert BKK when `legs.size` is even, else DXB, before the destination of leg `returnBoundary − 1` [F l.54–59]; all times cleared |
| `WizardHomeRoute*` | `addReturn`, `legs[1].to = AMS`, step `returnRoute`, `returnRouteConfirmed = false` |
| `WizardHomeRouteStop*` | as `WizardHomeRoute`, plus DXB inserted at the home route's position 1 |
| `WizardAirportFrom*` | `WizardWhereEmpty`, plus [R] `sheet = (0, From)`, query "Am" |
| `WizardAirportHome*` | `WizardHomeRoute`, plus [R] `sheet = (returnBoundary, Home)`, query "Am" |
| `WizardStopSearch*` | base with step `whereTo` (`fromLastTrip = false`, as iOS l.66), plus [R] `sheet = (0, Stop)`, query "Du" |
| `WizardStopSearchSecond*` | `WizardWhereStop`, plus [R] `sheet = (1, Stop)`, query "Ban" |
| `WizardStopSearchHome*` | `WizardHomeRoute`, plus [R] `sheet = (legs.size − 1, Stop)`, query "Du" |
| `WizardDiscard*` | `WizardWhere`, plus [R] `discardDialogVisible = true` (OQ-9c-5) |

Fixed result groups: Am → AMS, AMM, AMA, AMD; Du → DXB, DWC, DUS, DUB; Ban → BKK, DMK. All seed and fixed-search types live in debug sources and are absent from release, as checked by `ReleaseReadinessTest`.

## Captures and comparison handoff

`make captures ONLY=Wizard,TripSearchStates` ran **once**, passed in **19.15 s** wall time and wrote all **37 PNGs**. Wizard captures use 390 × 844 dp at density 3 and zero insets. Search-state captures use 375 × 812. Sheets wait for expanded focus and hide the IME. Native sheet/dialog windows are composited over the real activity bitmap using their own window dim and gravity. No stored references were recorded.

The captures were inspected for clipping, missing content, light/dark colors, native-window placement and the documented backdrop differences. This is **not** the brief's rendered-board sign-off: the external reviewer still needs to compare all matching boards side by side. `WizardWhereLimit_noBoard` and the two throttled-state images have no board. The native dialog, segmented controls, drag handle, audience notice and backdrop percentages differ exactly as listed above. Board geometry fixes included stroke widths, the title cap, 4 dp progress semantics, cumulative pixel rounding and the 669 dp breakpoint.

The local board-render command failed before producing a board PNG:

```text
node /private/tmp/chronoflow-9c-browser/render.cjs
browserType.launch: Target page, context or browser has been closed
[pid=15902][err] [1120/123634.209776:FATAL:base/apple/mach_port_rendezvous_mac.cc:159] Check failed: kr == KERN_SUCCESS. bootstrap_check_in org.chromium.Chromium.MachPortRendezvousServer.15902: Permission denied (1100)
```

Full output: `build/phase9c-evidence/9c-board-render.log`. Browser rendering was not retried around the sandbox restriction.

- [TripSearchStates_empty_dark](../build/captures/TripSearchStates_empty_dark.png)
- [TripSearchStates_empty_light](../build/captures/TripSearchStates_empty_light.png)
- [TripSearchStates_error_dark](../build/captures/TripSearchStates_error_dark.png)
- [TripSearchStates_error_light](../build/captures/TripSearchStates_error_light.png)
- [TripSearchStates_hint_dark](../build/captures/TripSearchStates_hint_dark.png)
- [TripSearchStates_hint_light](../build/captures/TripSearchStates_hint_light.png)
- [TripSearchStates_searching_dark](../build/captures/TripSearchStates_searching_dark.png)
- [TripSearchStates_searching_light](../build/captures/TripSearchStates_searching_light.png)
- [TripSearchStates_throttled_dark](../build/captures/TripSearchStates_throttled_dark.png)
- [TripSearchStates_throttled_light](../build/captures/TripSearchStates_throttled_light.png)
- [WizardAirportFrom](../build/captures/WizardAirportFrom.png)
- [WizardAirportFromDarkNL](../build/captures/WizardAirportFromDarkNL.png)
- [WizardAirportHome](../build/captures/WizardAirportHome.png)
- [WizardAirportHomeDarkNL](../build/captures/WizardAirportHomeDarkNL.png)
- [WizardDiscard](../build/captures/WizardDiscard.png)
- [WizardDiscardDarkNL](../build/captures/WizardDiscardDarkNL.png)
- [WizardHomeRoute](../build/captures/WizardHomeRoute.png)
- [WizardHomeRouteDarkNL](../build/captures/WizardHomeRouteDarkNL.png)
- [WizardHomeRouteStop](../build/captures/WizardHomeRouteStop.png)
- [WizardHomeRouteStopDarkNL](../build/captures/WizardHomeRouteStopDarkNL.png)
- [WizardStopSearch](../build/captures/WizardStopSearch.png)
- [WizardStopSearchDarkNL](../build/captures/WizardStopSearchDarkNL.png)
- [WizardStopSearchHome](../build/captures/WizardStopSearchHome.png)
- [WizardStopSearchHomeDarkNL](../build/captures/WizardStopSearchHomeDarkNL.png)
- [WizardStopSearchSecond](../build/captures/WizardStopSearchSecond.png)
- [WizardStopSearchSecondDarkNL](../build/captures/WizardStopSearchSecondDarkNL.png)
- [WizardWhere](../build/captures/WizardWhere.png)
- [WizardWhereDark](../build/captures/WizardWhereDark.png)
- [WizardWhereEmpty](../build/captures/WizardWhereEmpty.png)
- [WizardWhereEmptyDarkNL](../build/captures/WizardWhereEmptyDarkNL.png)
- [WizardWhereLimit_noBoard](../build/captures/WizardWhereLimit_noBoard.png)
- [WizardWhereNL](../build/captures/WizardWhereNL.png)
- [WizardWhereSame](../build/captures/WizardWhereSame.png)
- [WizardWhereStop](../build/captures/WizardWhereStop.png)
- [WizardWhereStopDarkNL](../build/captures/WizardWhereStopDarkNL.png)
- [WizardWhereStops](../build/captures/WizardWhereStops.png)
- [WizardWhereStopsDarkNL](../build/captures/WizardWhereStopsDarkNL.png)

## Android-only copy awaiting approval

All seven entries are generated together for en/nl and carry `review: "needs review"` in `tools/l10n/android-only.json`. They also appear in the plan's approval table. Dutch uses “luchthaven”.

| Name | en | nl | Replaces | Source |
|---|---|---|---|---|
| `android_wizard_airport_field` | Airport or city | Luchthaven of stad | wizard "Search airports" | WizardCopy |
| `android_wizard_search_hint` | Type at least 2 letters to search. | Typ minstens 2 letters om te zoeken. | — | TripSearchStates; nl proposed |
| `android_wizard_search_searching` | Searching airports… | Luchthavens zoeken… | — | TripSearchStates; nl web `lang/nl.json` |
| `android_wizard_search_empty_title` | No airports found | Geen luchthavens gevonden | — | TripSearchStates; nl web (without the period, as the board) |
| `android_wizard_search_empty_body` | Try the city name, like “Tokyo”, or the 3-letter code on your ticket, like “NRT”. | Probeer de naam van de stad, zoals “Tokio”, of de code van 3 letters op je ticket, zoals “NRT”. | — | TripSearchStates; nl proposed |
| `android_wizard_search_error_title` | We couldn't search airports. | We konden geen luchthavens zoeken. | — | TripSearchStates; nl web |
| `android_wizard_search_error_body` | Check your connection and try again. | Controleer je verbinding en probeer het opnieuw. | — | TripSearchStates; nl proposed |

## Verification

Commands and full outputs are retained locally under `build/phase9c-evidence/`. `build/test-results/<tier>/timings.json` contains machine-readable measurements. Targeted commands ran only the named classes/methods; below, seconds beside Gradle builds are their reported build wall times.

| Area / targeted selection | Result / wall time |
|---|---|
| `WizardSearchCopyTest` | 94 resource/catalog cases pass; 5 s |
| Shared fixture, original `TripWizardViewModelTest` | 8 original methods pass; 2 s; nested entries also pass in quick |
| `WhereUiStateTest`, `TripWizardViewModelWhereTest`, `TripWizardViewModelCloseTest`, original VM | 41 cases pass; 2 s; additional stale-question-index guard passes |
| `AirportSearchStateTest` | 7 original cases pass; 4 s; additional slice-emission guard passes |
| `WizardScaffoldGeometryTest` | 7 profiles/breakpoints pass; 5 s |
| `WizardHeaderTest`, `CfButtonTest` | Structure/focus/slide/alpha checks pass; final targeted builds 4–5 s |
| `WizardBackHandlingTest` | 5 cases pass; 8 s |
| `WhereStepTest`, `WhereSemanticsTest`, `WhereGeometryTest` | 14 cases pass; 6 s; changed controls rechecked in 7 s |
| `AirportSearchSheetTest`, `AirportSearchSheetGeometryTest` | 11 sheet and 2 geometry cases pass; fixes rechecked in 6 s; geometry/title cap later rechecked in 4 s |
| `WizardLauncherTest`, `WizardRecoveryCheckTest`, `ShellViewModelOverlayTest`, `ShellNavStateTest` | Pass; grouped builds 9 s + 2 s; epoch checks 4 s |
| Existing `AppShellTest#wizardOverlayCoversTabsAndBackCloses` | Pass unchanged; 8 s |
| `WizardBoardCaptureSeedTest`, `WizardDebugSeedsTest`, `ReleaseReadinessTest` | 27 seed cases, 4 app-seed cases, release-isolation checks pass; builds 5 s, 5 s, 10 s |
| `WizardBoardCapturePresentationTest`, `OverlayViewModelTest` | Presentation and cleared-store guards pass; builds 8 s and 9 s |
| `WizardWindowCaptureTest` | Native sheet and alert produce full 1170 × 2532 frames; 5 s |
| `WizardRecompositionTest` | Pass, zero unrelated recompositions; 7 s |
| `WizardGuardsTest` | 97 separate cases pass; initial corrected run 10 s, class 6.533 s, first SDK case 2.452 s, warm maximum 0.224 s; final layout results below |
| `make emu` | Pass, existing pixel prepared; output `Scripts/emulator.sh ensure` / `Night mode: no` |
| `./gradlew :app:compileDebugAndroidTestKotlin` | Pass; 5 s |
| `make targeted ONLY="app:WizardSearchKeyboardTest app:WizardJourneyTest"` | Both run once and pass; tier wall 18.52 s; IME 2.479 s, journey 5.247 s; StrictMode IO counter 0 |
| `make quick` (once) | 35.74 s; 2,778 cases, three integration assertions failed; zero timing overruns or hangs. Corrections and remaining checks below |
| `make targeted ONLY="konsist:KonsistRulesTest#viewModelsEndWithViewModel core:designsystem:LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached core:ui:StringParityTest#countsAreRecorded app:ShellViewModelOverlayTest app:OverlayViewModelTest"` | Pass after naming test ViewModels and updating generated inventories; four grouped builds 4 s + 2 s + 3 s + 6 s |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug :core:designsystem:lintDebug :core:ui:lintDebug :app:verifyReleaseManifest` | Pass; 44 s; warnings treated as errors |

Quick was not rerun. Its three failures were the two fixture class names checked in one Konsist assertion, the glyph count 44 → 47, and the iOS resource count after replacing Search airports with Android-only copy (en 922 → 921; nl 921 → 920). No allowlist or behaviour guard was relaxed. A mutation check removed the empty-route swap guard and made `emptyRouteShowsPlaceholdersAndDisablesSwap` fail; restoring it passed. An accidentally concurrent targeted invocation lost its timing stamp; it was rerun serially and produced results. All later Gradle invocations were serial.

Final checks that quick did not reach:

| Command | Result / wall time |
|---|---|
| Layout tier, `:app:testDebugUnitTest :feature:wizard:testDebugUnitTest :core:designsystem:testDebugUnitTest :core:ui:testDebugUnitTest -Pcf.tier=layout` through `Scripts/run-tier.sh layout-9c` | 443 pass in 20.30 s; zero over-budget cases; all 97 wizard guards pass, class 4.389 s, warm maximum 0.128 s |
| `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew :build-logic:convention:test` | Pass in 2.36 s; certified existing test outputs reused |
| `Scripts/test_scripts.sh` | Script tests: 168 pass in 19.289 s. Contract parity needed the environment normally exported by make; resumed below without repeating these 168 tests |
| Contract tests with `IOS_REPO` and `WEB_REPO` exported | 25 pass in 0.109 s |
| Localization generator tests | 49 pass in 0.016 s |
| Icon generator tests | One inventory assertion still expected 44 glyphs; changed to 47. The other 8 cases pass; targeted corrected method passes in 0.018 s |
| `./gradlew spotlessCheck` after the inventory fixes | Pass; 11 s |
| `make timing` | Pass in 2.28 s; seven certified existing timing cases reused; no budget overrun. Preparation 195/155/178 ms, Today 9 ms, drawings 65/37/44 ms |

The preparation/drawing timings are JVM harness results, not device frame measurements. The commit-message and signature audit inspects commit objects without changing history; every phase commit contains a PGP signature and no trailer. The optional `git log e6574cd1..HEAD --format='%h %G? %s'` verification command produced no output and was interrupted (exit 130); signature headers were checked directly with `git cat-file commit` instead.

## Remaining external acceptance

- Render the boards and complete the side-by-side review above. The external rendered-board review remains pending because browser rendering is blocked.
- Run `make check` as the orchestrator's full commit gate; it was deliberately not run here.
- The wizard open path changed. Regenerate with `make baseline-profile`; the dedicated wizard profile journey is scheduled in Phase 13. The existing generator still covers startup and timeline, not the wizard journey.
- Quiet recovery adds work after entering AppShell, on injected IO. Run `make perf` to check startup against the existing budget and record numbers in `docs/perf-startup.md`. No startup, timeline or profile performance command was run in this phase. No timeline implementation changed.

OQ-9c-1 through OQ-9c-12 are recorded as **OQ-A370–OQ-A381**, using the brief's defaults. No credential capability was enabled, no new native library dependency was added, and the pixel emulator remains running.

## Review round 1

1. The airport header now measures the title's intrinsic width, caps it at 220 dp and reserves the side slots and 8 dp gaps in a custom `Layout`. The remaining side slots are equal, so the title stays centered. No measured size enters state. The From, To and Home line-height guards use the generated wizardButton line height and tags only. Before the fix the pixel8 Home title measured 44.190475 dp and failed; all eight geometry cases now pass (targeted tier 7.58 s). `make captures ONLY=WizardAirport,WizardStopSearch` ran once, regenerated ten sheet PNGs and passed in 18.07 s; unrelated earlier captures were preserved. The 390 dp From capture now has a single-line title; Dutch Home uses two lines within the space left by Cancel.

2. The stop question now supplies each segment's enabled state and haptic kind to `CfSegmentedButtons`. Its defaults remain enabled + Selection for every other caller. The pinned `MemberWizardRouteView.routeStopQuestion` explicitly plays Light when making a route direct with existing stops; the native wrapper now plays exactly that one event. Disabled Yes has M3 disabled semantics, no callback and no haptic. `WhereStepTest` (10 cases, RecordingHaptics) and `CfSegmentedButtonsTest` (2 default/disabled contract cases) pass.

3. `airportPlace` catches only `IllformedLocaleException` from region construction and omits that country, matching the pinned iOS city-only fallback. `WhereUiStateTest` adds separate JVM cases for a whitespace code and the country name “Netherlands”; all 17 cases pass.

4. All Konsist test classes, including the enclosed parameterized classes, inherit `KonsistTest`. Its inherited `@BeforeClass` forces one tiny fixture parse through a JVM-local lazy value before any timed test case. TestTimingListener and every budget are unchanged. The same seven-class targeted selection runs 130 cases before and after: both pass (5.90 s and 5.77 s tier wall times). The reported 1.05 s overrun did not reproduce in this baseline; the following are the three slowest measured baseline cases and their corresponding times after warm-up.

| Case | Before | After |
|---|---:|---:|
| `TodayTestBoundaryTest$Planted#rejectsPlantedViolation[app/src/testDebug/kotlin/AppGraphCallTest.kt]` | 0.529 s | 0.016 s |
| `HapticsRulesTest#noHapticsInLaunch` | 0.365 s | 0.164 s |
| `HapticsRulesTest#noHapticsInLaunchRejectsPlantedViolation` | 0.342 s | 0.010 s |

The three slowest cases after warm-up are `TodayTestBoundaryTest$Project#allUnitTestsKeepAppBootsAtApprovedBoundaries` (0.195 s), `HapticsRulesTest#noHapticsInLaunch` (0.164 s), and `KonsistRulesTest#routesAreSerializable` (0.107 s). These are JVM test times, not app performance measurements.


Review-round commands and final results:

```sh
make targeted ONLY="feature:wizard:AirportSearchSheetGeometryTest#fromTitleFitsOnOneLine feature:wizard:AirportSearchSheetGeometryTest#toTitleFitsOnOneLine feature:wizard:AirportSearchSheetGeometryTest#homeTitleFitsOnOneLine"
make targeted ONLY="feature:wizard:AirportSearchSheetGeometryTest"
make captures ONLY=WizardAirport,WizardStopSearch
make targeted ONLY="feature:wizard:WhereStepTest core:designsystem:CfSegmentedButtonsTest"
make targeted ONLY="feature:wizard:WhereUiStateTest"
make targeted ONLY="konsist:HapticsRulesTest konsist:KonsistRulesTest konsist:ModuleGraphTest konsist:StringResourceLocationTest konsist:TierRegistryTest konsist:TodaySessionKeyTest konsist:TodayTestBoundaryTest"
Scripts/run-tier.sh layout-9c-review1 --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'core/designsystem/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/designsystem/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew :feature:wizard:testDebugUnitTest :core:designsystem:testDebugUnitTest -Pcf.tier=layout
./gradlew :feature:wizard:lintDebug :core:designsystem:lintDebug
make quick
```

| Selection | Exact result | Reported wall time |
|---|---|---:|
| New header guards before the fix | Six cases; five pass, pixel8 Home fails at 44.190475 dp | Gradle 6 s |
| Full sheet geometry class after the fix | Eight pass; no timing overrun | Tier 7.58 s |
| Sheet captures, once | Ten matching PNGs written | Tier 18.07 s |
| WhereStepTest + CfSegmentedButtonsTest | Ten + two cases pass | Gradle builds 7 s + 5 s |
| WhereUiStateTest | Seventeen pass; no timing overrun | Tier 4.07 s |
| All seven Konsist classes, before / after | 130 pass each; zero over-budget cases | Tiers 5.90 / 5.77 s |
| Layout, once | 122 pass including all 97 wizard guards; zero over-budget cases | Tier 11.81 s |
| Requested lint tasks, once | Pass, warnings treated as errors | Gradle 22 s |
| Quick, once | 2,783 freshly measured cases pass their assertions; all 4,551 collected/reused results pass. Gradle exits 0; the quick tier budget check exits 3, make exits 2 | Whole command 40.058 s; Gradle 39 s |

Quick's only failures are unchanged cases exceeding the 1 s budget: `net.chronoflow.app.RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier` at **1,001 ms**, and `net.chronoflow.feature.onboarding.OnboardingSessionTests#enqueueRejectsAStaleSnapshotWithSessionChanged` at **1,009 ms**. They were not retried, per p9c-i.md. No hangs or missing result files occurred. Quick did not reach its later build-logic/tooling/serial-timing commands after the timing gate failed; no successful full-quick claim is made.

In the final quick run, the slowest three Konsist cases are `HapticsRulesTest#noHapticsInLaunch` (0.554 s), `TodayTestBoundaryTest$Project#allUnitTestsKeepAppBootsAtApprovedBoundaries` (0.447 s), and `KonsistRulesTest#routesAreSerializable` (0.325 s), all within the unchanged budget. Detailed logs, the before/after XML and timing files are retained in `build/phase9c-review1/`. No emulator, performance or profile command ran in this review round. No copy or OQ rows were added.


## Review round 2

The header follows the board's auto/min-content grid: the title uses its max-content width, capped at 220 dp and `W − 2 × gap − cancelMinimum − trailingMinimum`. Each side track starts with half the remaining space, keeps its own minimum and gives the remainder to the other side. The title is placed after the leading track and gap; it can sit slightly off-center when Cancel needs more space than the empty trailing track. This corrects round 1's symmetric minimum-width assumption. All measurements stay in `Layout`.

The geometry class now parameterizes language as well as profile, adding Dutch at 390 × 844 dp and density 3. Before the fix, `homeTitleFitsOnOneLine` ran three cases: both existing English cases passed and Dutch Home failed at **44 dp** high. After the fix all twelve geometry cases pass; Dutch Home is **22 dp** high at **105 dp** from the sheet's left edge, matching the independently asserted grid placement and the 8 dp gap after Cancel. No copy is asserted on screen, and the 97-case WizardGuards matrix is unchanged.

`make captures ONLY=WizardAirport,WizardStopSearch` ran once and wrote ten sheet PNGs. The regenerated `WizardAirportHomeDarkNL.png` was inspected: both Annuleren and the title occupy one line. Other existing captures were preserved.


| Command | Exact result | Wall time |
|---|---|---:|
| `make targeted ONLY="feature:wizard:AirportSearchSheetGeometryTest#homeTitleFitsOnOneLine"` before the fix | Three cases: English pixel8 and fold pass; Dutch 390 Home fails at 44 dp, as required | Tier 7.88 s |
| `make targeted ONLY="feature:wizard:AirportSearchSheetGeometryTest"` after the fix | Twelve pass; zero timing overruns | Tier 7.33 s |
| `make format` | Pass | Gradle 15 s |
| `make captures ONLY=WizardAirport,WizardStopSearch`, once | Ten matching PNGs regenerated; zero timing overruns | Tier 17.39 s |
| `Scripts/run-tier.sh layout-9c-review2 --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew :feature:wizard:testDebugUnitTest -Pcf.tier=layout`, once | 118 pass, including the unchanged 97 WizardGuards cases; zero timing overruns | Tier 11.43 s |
| `./gradlew :feature:wizard:lintDebug`, once | Pass, warnings treated as errors | Gradle 9 s |
| `make quick`, once | All assertions pass; Gradle exits 0. Budget check exits 3 and make exits 2 on the untouched Terms case below | Whole command 37.802 s; Gradle 37 s |

Quick's only failure is the untouched `net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed`: **3,464 ms > 3,000 ms** class warm-up allowance. It was not retried. Quick did not reach its later build-logic/tooling/serial-timing commands. Full command output and before/after XML/timings are in `build/phase9c-review2/`. No hangs or missing results occurred. No emulator, performance or profile commands ran; no copy or OQ rows were added.


## Onboarding fixture cold-start budget fix

The stale-session case paid JVM first-use work in the shared onboarding fixture, rather than a wait in `enqueue`. A temporary step timer and an ordered two-case probe measured the original stale-session case first, then in a separate test JVM after the policies case. Both probe cases called the existing test bodies unchanged. Timers, the probe and forced ordering were removed before the final validation.

| Step | Stale-session case first | After policies in the same JVM |
|---|---:|---:|
| Fixture setup, total | 316.026 ms | 43.078 ms |
| Mock-seed construction, within setup | 141.097 ms | 36.017 ms |
| JDBC open/configuration/schema, within setup | 148.758 ms | 3.313 ms |
| `session()` | 1.498 ms | 0.014 ms |
| Sign-out | 10.077 ms | 4.130 ms |
| Rejected enqueue | 0.200 ms | 0.191 ms |
| Database file check | 0.034 ms | 0.034 ms |

With policies first, its fixture setup paid the cold cost instead: 354.890 ms, including 150.847 ms for the mock seed and 178.796 ms for JDBC. The fixtures use `StandardTestDispatcher`, an injected empty mock sleep and an in-memory session store; there is no real delay or Keystore initialization to remove. Every fixture still creates a fresh schema, so the 3.313 ms warm JDBC measurement includes per-case schema creation. The large difference is first-use JDBC and mock-seed/JSON initialization. The host's >1 s overrun did not reproduce in these isolated diagnostic runs.

`OnboardingFixtureTest` uses inherited `@BeforeClass` setup and a JVM-local lazy value to create, exercise and fully close one disposable fixture before timed cases. All six classes using `withOnboardingFixture` inherit it: `OnboardingSessionTests`, `WalkthroughModelTests`, `WalkthroughSkipTests`, `WalkthroughFlushTests`, `TermsAcceptanceTests` and `TermsViewModelTests`. The warm-up retains no fixture; each case still creates and cleans up its own database. All existing assertion bodies and TestTimingListener budgets are unchanged.

| OnboardingSessionTests case | Before, stale-session case first | After once-per-JVM warm-up |
|---|---:|---:|
| `enqueueRejectsAStaleSnapshotWithSessionChanged` | 353 ms | 42 ms |
| `policiesFlowDecodesTheStoredSection` | 48 ms | 44 ms |

The reversed-order diagnostic measured policies first at 398 ms and the stale-session case second at 50 ms. `make targeted ONLY="feature:onboarding:OnboardingSessionTests"` after the fix passed both cases in 2.81 s tier wall time. The one final `make quick` invocation passed all stages in **35.890 s** with zero timing overruns. The regenerated profiles, benchmark gate, plan and perf docs at `553398ee` were preserved byte-for-byte. Detailed step logs, temporary instrumentation, XML and timing results are retained locally in `build/onboarding-fixture-evidence/`.
