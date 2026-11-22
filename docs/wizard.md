# Wizard shell, Where and flight times acceptance

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
| Board progress on the same-airport state and sheet/alert backdrops (`WizardWhereSame` 15 despite an incomplete route, `WizardAirportFrom` 15 over the empty Where, `WizardAirportHome` and `WizardStopSearchHome` 55, `WizardDiscard` 30) | The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d) | progress comes from 9a, never per board [R]; OQ-9c-6 |
| `WizardDiscard` backdrop = Outbound | Captured over the base Outbound draft from 9d; `fromLastTrip = false` (9c used Where temporarily) | Outbound is 9d [R]; OQ-9c-5 |
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
| `WizardDiscard*` | `WizardOutbound`, `fromLastTrip = false`, plus [R] `discardDialogVisible = true` (OQ-A374; changed in 9d) |

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

## Phase 9d: flight times and return

2026-11-20. [Brief](phases/phase-9d-wizard-flights.md). Outbound and flight-home Times, the native dialogs and Coming back? are implemented. The external rendered-board comparison remains pending; the local browser could not start. There is no wizard performance-budget row, and this phase runs no startup, timeline or profile tier. The wizard profile journey remains Phase 13's.

### Half-values and recovery

`MemberWizardState.pendingDateTimes` stores `WizardPendingDateTime(date, time)` under `<leg.id>.departLocal` or `<leg.id>.arriveLocal`. A complete value takes precedence when reading a half. Setting the second half composes `YYYY-MM-DDTHH:mm`, removes the pending entry and uses `setDateTime`; editing a complete value preserves its other half. Both paths call `changed()`, mark the draft entered, clear every field error (including the review banner) and enqueue recovery. Missing pending maps decode as empty for old recovery rows.

Airport selection, swap, stop removal, making the route direct and choosing the trip end drop the pending map. Complete wall values retain the landed model's route-edit rules. Pending values never enter the review request or its fingerprint. Completion still requires complete wall values, so a half pair keeps Continue disabled without producing a required-field error.

The screen is stateless. The ViewModel owns date/time events, account time-format updates, Continue validation and its failure counter. Each equal-by-value flight card can skip composition independently. A failed Continue leaves the step in place; the composable handles each counter increment once, plays Warning, waits one frame and brings the first error group above the fixed footer. Reduce Motion sets the scroll coroutine's motion scale to zero. The handled counter survives recreation without replaying feedback.

### Picker contract

- UTC-midnight millis represent an airport wall date. `WizardPickerDates` delegates to 9a's `pickerMillis` / `fromPickerMillis`; there is no phone-zone conversion.
- Only the immediately preceding field can establish the date lower bound, converted through its instant into this field's airport zone. Equal dates and every later date/year remain selectable. There is no upper bound or time-of-day bound. Stored values below a changed bound remain intact for validation.
- Initial values prefer the stored half, then 9a's nearest earlier airport-local instant, then the injected airport-local clock.
- The native time dialog reads `DateFormat.is24HourFormat`. Pills and their accessibility values use account `TimeFormat`, defaulting to 24 hours. The session reads and parses settings on injected IO. The dialogs use the app locale; their semantics node reads `LocalConfiguration` inside its own window.
- Only Done sends a part event and one Selection haptic. Cancel, outside dismissal and Back close the local picker without a write or haptic. M3 saves the in-progress picker state for configuration recreation; an open dialog is absent from the recovery record.
- Date pills use the shared short-month tables. Spoken dates use fixed long tables. Twelve-hour text shares 9a's wizard clock formatter: English `2:40 PM`, Dutch `02:40 p.m.`; account unknown values use 24 hours. Durations use the catalog's `{{hours}} h {{minutes}} min` for every value. Field messages resolve through the generated wizard index, including its singular plural resource; unknown keys use the generated generic error, never the raw key. The later plan-catalog lookup is 9f's (OQ-A399).

The existing `CfDatePickerDialog` / `CfTimePickerDialog` gained supporting text, custom action tags and resource-id export on their windows; the date wrapper also gained confirm-enabled state. New parameters are appended to preserve positional callers. The two local `ModifierParameter` suppressions and the Compose parameter-order suppression record this compatibility exception. Defaults preserve earlier callers.

`WizardBanner` uses the wizard's info glyph, 18 dp radius, 14/12 padding and 10 dp gap. `CfBanner` has a different glyph and auth geometry, so it is not used. Designed pill targets are 48 dp with an 8 dp visual radius; only a date pill carries its field's error ring. Labels and visible values retain their semantics, routes are headings, and error lines/banners are polite live regions. Connection rules are cached in `drawWithCache` with no per-frame allocation.

### Coming back?

One way reflects the default model end. Add return is selected only while its choice is pending. Both cards retain their visual style while disabled; only `selected` determines the border. The model's injected clock owns the 250 ms pause. A second tap is ignored, Back cancels the job, One way goes to Sleep or Checking, and Add return starts the home route at the final outbound arrival with To following the origin. There is no Continue button; the answer hint is fixed above the safe bottom inset.

At sixteen flights, the ViewModel rejects the disabled Add return event before calling the landed model. Inspection found that the model checks a failed add after candidate route mutations; the new event guard matches iOS's disabled control without changing that earlier core behavior.

### Flight board drafts and captures

All times below are airport-local in 2026. These extend the earlier seed table; every row is entered, `fromLastTrip = false`, `sleepNeeded = false`, with the pinned 23:30 / 07:15 / intermediate profile and the fixed 1 October clock. Dutch seeds use Tokio; real airport data retains the server city.

| Base board | Draft |
|---|---|
| `WizardOutbound` | Outbound; AMS → NRT, 12 Oct 14:40 → 13 Oct 09:15; flight 695 min |
| `WizardOutboundEmpty` | Outbound; both complete wall values empty |
| `WizardOutboundErrors` | Outbound; arrival 11 Oct 09:15; `legs.0.arriveLocal` error |
| `WizardOutboundTwoErrors` | Two outbound flights via DXB; second arrival 12 Oct 16:35; `legs.1.arriveLocal` error |
| `WizardOutboundStop` | AMS → DXB, 12 Oct 14:40 → 23:25; DXB → NRT, 13 Oct 02:05 → 16:35; connection 160 min |
| `WizardOutboundStopEmpty` | Same two-flight route; all four complete wall values empty; Stop in Dubai |
| `WizardOutboundOvernight` | First arrival 12 Oct **22:25**; second 14 Oct 02:05 → 16:35; one complete night |
| `WizardOutboundThree` | AMS → DXB → BKK → NRT; second arrival 13 Oct 11:20; third flight empty; second chip Stop in Bangkok |
| `WizardOutboundPast` | 2 Sep 14:40 → 3 Sep 09:15; member review banner |
| `WizardOutboundScrollErrors` | Five flights via DXB/BKK/DXB/BKK; errors on first arrival and last departure; no board |
| `WizardReturn` | `returnChoice`, default one-way end |
| `WizardReturnFlight` | `returnFlight`, return boundary 1, confirmed route; NRT → AMS with empty times |
| `WizardReturnFlightPast` | Home 22 Oct 10:25 → 15:30, 725 min; home review banner |
| `WizardDiscard*` | Base Outbound with discard presentation; progress **30**, completing OQ-A374/OQ-A375's handoff |

`make captures ONLY=WizardOutbound,WizardReturn,WizardDiscard` ran once and wrote **29 PNGs** at 390 × 844 dp, density 3, zero insets. The existing capture class handles all new names; no stored references or second capture class were added. Files are in `build/captures/`:

- `WizardOutbound`, `WizardOutboundDark`, `WizardOutboundNL`;
- `WizardOutboundEmpty`, `WizardOutboundEmptyDarkNL`;
- `WizardOutboundErrors`, `WizardOutboundErrorsDarkNL`;
- `WizardOutboundTwoErrors`, `WizardOutboundTwoErrorsDarkNL`;
- `WizardOutboundStop`, `WizardOutboundStopDarkNL`;
- `WizardOutboundStopEmpty`, `WizardOutboundStopEmptyDarkNL`;
- `WizardOutboundOvernight`, `WizardOutboundOvernightDarkNL`;
- `WizardOutboundThree`, `WizardOutboundThreeDarkNL`;
- `WizardOutboundPast`, `WizardOutboundPastDarkNL`;
- `WizardReturn`, `WizardReturnDark`, `WizardReturnNL`;
- `WizardReturnFlight`, `WizardReturnFlightDarkNL`;
- `WizardReturnFlightPast`, `WizardReturnFlightPastDarkNL`;
- `WizardOutboundScrollErrors_noBoard`;
- `WizardDiscard`, `WizardDiscardDarkNL`.

`make captures DEVICE=1 ONLY=PickerDateTime` ran once on pixel and wrote four PNGs to `build/captures/device/`: `PickerDateTime_date_en_light`, `PickerDateTime_time24_en_light`, `PickerDateTime_date_nl_dark`, `PickerDateTime_time12_nl_dark`. Each case restores the phone's original hour-cycle setting in `finally`, including deletion when the setting was absent. The script routes this selector to the app capture class. The native cases took 3.081 / 1.211 / 1.203 / 1.093 s respectively.

The 29-image contact sheet and four-image native contact sheet were inspected for missing content, clipping, themes, native windows, errors, chips and selection. The following are recorded differences, not a rendered-board sign-off:

| Board(s) | Difference / disposition |
|---|---|
| Every wizard wrapper | Native header/system progress and Android safe insets follow 9c's settled mapping; native targets are at least 48 dp |
| `WizardOutboundOvernight*` | 22:25 first arrival, matching the pinned seed and a real complete night (OQ-A391) |
| `WizardOutboundStopEmpty*`, `WizardOutboundThree*` | Model progress 22 while incomplete, instead of the illustrated 30 (OQ-A393) |
| `WizardReturn*` | One way selected by default, following iOS (OQ-A388) |
| `WizardOutbound`, `WizardOutboundDark`, `WizardOutboundErrors`, `WizardOutboundPast`, `WizardOutboundStop`, `WizardOutboundOvernight`, `WizardOutboundThree`, `WizardOutboundTwoErrors`, `WizardReturnFlightPast` | Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill |
| Connection/overnight rows | Catalog duration always includes hours and minutes; longer chips can wrap only when space requires it (OQ-A386) |
| `PickerDateTime` | Separate Material date/time dialogs replace the combined iOS-shaped sheet; custom title/context, native calendar/dial, Cancel/Done, no summary chip. M3 marks the phone's today (OQ-A394, OQ-A396, OQ-A397) |
| `WizardDiscard*` | Now Outbound at 30, replacing the 9c temporary Where backdrop |

The board render was attempted once with `node /private/tmp/chronoflow-9d-render.cjs`. It failed before writing a board PNG:

```text
browserType.launch: Target page, context or browser has been closed
[pid=47643][err] [1120/143007.406861:FATAL:base/apple/mach_port_rendezvous_mac.cc:159] Check failed: kr == KERN_SUCCESS. bootstrap_check_in org.chromium.Chromium.MachPortRendezvousServer.47643: Permission denied (1100)
```

Exact full output: `build/phase9d-evidence/board-render.log`. The reviewer must still render the matching boards at 390 × 844 and complete the side-by-side comparison. No Opus review or rendered-board approval is claimed.

### Test map and brief refresh

| Pinned Swift test / behavior | Kotlin evidence |
|---|---|
| `testNativePickerReceivesOnlyTheAirportLocalLowerBound` | `WizardPickerDatesTest.Inputs#nativePickerReceivesOnlyTheAirportLocalLowerBound`, `WizardSelectableDatesTest#nativePickerReceivesOnlyTheAirportLocalLowerBound`, and the Dutch native case |
| `testNativePickerKeepsTheAppLanguageAndPhoneHourCycle` | `WizardFlightPickerDeviceTest#nativePickerKeepsTheAppLanguageAndPhoneHourCycle_nl12` / `_en24` |
| `testErrorsScrollAboveContinue<Profile>` | `WizardErrorScrollTest#errorsScrollAboveContinue`, twelve separately named profile/language/direction cases; includes offscreen/visible preconditions and whole-last-card containment |
| Existing 9a `WizardFlightTimingTests`, `WizardPresentationTests`, `WizardValidationTests`, `WizardDateTimeFormatTest` | Unchanged; targeted with Task 2 and included in final quick |
| Android separate halves / formats / connection facts | `WizardDateTimePartsTest`, `WizardDateFormatTest`, `WizardConnectionFactTest` |
| Android UI derivation / events | `WizardTimesUiTest`, `TripWizardViewModelTimesTest`, `WizardReturnUiTest`, `TripWizardViewModelReturnChoiceTest` |
| Board seed fidelity | `WizardFlightBoardSeedTest`, existing filtered `WizardBoardCaptureSeedTest` |
| Structure / geometry / composition | `WizardTimesStepTest`, `WizardDateTimeRowLayoutTest`, `WizardTimesRecompositionTest`, `WizardReturnStepTest`, `WizardFlightsGuardsTest` |
| Native captures | `PickerDateTimeDeviceCaptureTest` |

The additional mechanical refresh is in `30316f03`: `DeviceProfiles.small` is 360 dp at scale 1.0, so the narrow row-layout case uses a local 280 dp / 1.3 copy. Shared profiles and the 62-case matrix are unchanged. Task 7 records two later integration notes in its own commit: preserve the old outbound structural tag in debug for the unchanged header/journey checks, and guard the disabled sixteen-flight event in the ViewModel. There is no debug seam text on Times, and release adds no boundary tag. No prior test assertion was changed except the explicitly authorized Discard seed expectation and the seed-registry filter.

A real geometry failure was observed before correcting the pill target: the inline label started at 8 dp while the pill tag covered the inner visual at 14.857142 dp. The target/tag now covers the full 48 dp clickable box, and both row-layout cases pass. The first ScrollErrors derivation expectation incorrectly treated its complete values as incomplete; the pinned seed has all values and its expectation was corrected. Native dialogs were never opened by the new Robolectric tests.

OQ-9d-1…17 are recorded as **OQ-A383…OQ-A399**, using the brief defaults. No Android-only copy or dependency was added. No secret storage, route key, initialization, timeline or credential-capability code changed.

### 9d verification results

Full outputs, the attempted renderer and contact sheets are retained locally under `build/phase9d-evidence/`; tier JSON files retain wall times and per-case measurements. No hang or missing-result failure occurred. Gradle seconds below are the reported build wall time where a command has no tier wrapper.

| Command / selection | Result | Wall time |
|---|---|---:|
| `make targeted ONLY="core:wizard:WizardDateTimePartsTest"` | 14 pass | Gradle 5 s |
| `make targeted ONLY="core:wizard:WizardPickerDatesTest core:wizard:WizardDateFormatTest core:wizard:WizardConnectionFactTest core:wizard:WizardFlightTimingTests core:wizard:WizardPresentationTests core:wizard:WizardValidationTests core:wizard:WizardDateTimeFormatTest"` | 125 pass; old 9a assertions unchanged | 3.65 s |
| `make targeted ONLY="core:ui:CfDatePickerDialogGuardsTest core:ui:CfTimePickerDialogGuardsTest"` | 12 pass | 8.91 s |
| `make targeted ONLY="feature:wizard:WizardFlightBoardSeedTest feature:wizard:WizardBoardCaptureSeedTest"` | 56 pass | 4.18 s |
| Times UI + ViewModel, first run | 30 pass; one incorrect ScrollErrors completeness expectation corrected | Gradle 2 s |
| Corrected Times derivation + four screen/layout/composition classes | 34 pass; one real pill-target geometry failure corrected | 6.95 s |
| `make targeted ONLY="feature:wizard:WizardDateTimeRowLayoutTest#pillsReflowWithoutChangingTheirIdentity"` | Both corrected cases pass | 5.55 s |
| `make targeted ONLY="feature:wizard:WizardSelectableDatesTest"` | Both pass after removing an unused `Unit` expression exposed by formatting | 3.30 s |
| Return UI, ViewModel and screen, first run | 13 pass; disabled sixteen-flight event exposed the core candidate mutation | Gradle 7 s |
| `make targeted ONLY="feature:wizard:TripWizardViewModelReturnChoiceTest#addReturnIsInertAtSixteenFlights feature:wizard:WizardHeaderTest"` | Corrected boundary + four unchanged header cases pass | 5.54 s |
| `make targeted ONLY="feature:wizard:WizardFlightsGuardsTest"` | All 62 pass; class 4.459 s, first SDK case 2.334 s | 8.12 s |
| `make captures ONLY=WizardOutbound,WizardReturn,WizardDiscard`, once | 29 PNGs, no stored references | Gradle 16 s |
| `make targeted ONLY="app:WizardFlightPickerDeviceTest"`, once | Both pass: en24 2.662 s, nl12 1.630 s; checked StrictMode IO counter 0 | 11.19 s |
| `make captures DEVICE=1 ONLY=PickerDateTime`, once | Four cases pass and four PNGs written | Gradle 11 s |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug :core:designsystem:lintDebug :core:ui:lintDebug :app:verifyReleaseManifest` | Pass after the two compatibility-related ModifierParameter suppressions; warnings remain fatal | Gradle 21 s |
| Affected layout tier (`app`, `feature:wizard`, `core:designsystem`, `core:ui`, `-Pcf.tier=layout`) through `Scripts/run-tier.sh layout-9d` | 535 pass, zero over-budget cases; unchanged 97 Where guards, 62 new guards, 12 error-scroll cases | 18.97 s |
| `make targeted ONLY="app:ReleaseReadinessTest"` | 13 pass; debug seed/mock absence proved in release | 4.30 s |
| `make quick`, exactly once | Exit 2: 4,178 freshly measured cases, 4,544 collected/reused results; one literal-copy rule failure; zero timing overruns or hangs | 42.08 s |
| `make targeted ONLY="konsist:KonsistRulesTest#noLiteralTextInText"` | Pass after deriving the same airport-code route before passing it to `Text`; the rule is unchanged | Gradle 4 s |
| Missed wizard/trips quick tasks through `Scripts/run-tier.sh quick-9d-remainder` | 321 pass, zero timing overruns | 9.56 s |
| `spotlessCheck :build-logic:convention:test` through `Scripts/run-tier.sh build-logic-9d` | 37 pass, formatting clean | 9.94 s |
| `Scripts/test_scripts.sh`, with `IOS_REPO` / `WEB_REPO` exported as make does | All groups pass: 168 scripts, 25 contracts, 49 localization, 9 icons | Reported groups 20.047 / 0.117 / 0.018 / 0.065 s |
| `make timing` | Seven freshly measured cases pass, zero overruns; no device performance claim | 2.00 s |

The failing quick invocation prevented wizard and trips tasks from starting and prevented the later Make recipe stages. Only those missed modules, the failed rule, and the remaining build-logic/tooling/timing stages were run separately; quick itself was never repeated. All corrected assertions and all remaining stages pass. The original command's failed exit is retained rather than described as a successful full quick run. The final route-label fix is a separate signed commit; no earlier commit was amended or rewritten.

Final wizard class measurements (XML totals; warm maxima from the listener):

| Class | Cases | Class total | Warm case maximum | Separate class warm-up |
|---|---:|---:|---:|---:|
| `WizardFlightsGuardsTest` | 62 | 1.944 s | 72 ms | 47 ms |
| `WizardGuardsTest` (unchanged) | 97 | 3.161 s | 63 ms | 28 ms |
| `WizardErrorScrollTest` | 12 | 0.611 s | 56 ms | 83 ms |
| `WizardDateTimeRowLayoutTest` | 2 | 0.061 s | ≤ 36 ms | Within class allowance |
| `TripWizardViewModelTimesTest` | 7 | 0.019 s | 3 ms | JVM, none |
| `TripWizardViewModelReturnChoiceTest` | 6 | 0.058 s | 38 ms | JVM, none |
| `WizardTimesUiTest` | 24 | 0.006 s | < 1 ms | JVM, none |
| `WizardReturnUiTest` | 5 | 0.001 s | < 1 ms | JVM, none |
| `WizardTimesStepTest` | 8 | 0.199 s | 30 ms | 26 ms |
| `WizardReturnStepTest` | 3 | 0.080 s | 25 ms | 32 ms |
| `WizardTimesRecompositionTest` | 1 | 0.032 s | — | 31 ms (single case) |

The initial new-guards run paid its SDK warm-up once at 2.334 s (warm maximum 108 ms); the final layout/logic measurements above were warm. TestTimingListener budgets, fixture profiles and test-boundary allowlists were not widened. The new raw-size-state and phone-zone/process-locale scans find no matches in `core/wizard/src/main` or `feature/wizard/src/main`. No dependency changed, so no new native library is introduced.

Remaining external acceptance: the rendered-board comparison described above and the orchestrator's full `make check`. Performance/profile commands were deliberately not run; 9d adds no applicable metric. Pixel remains running. The final signature audit checks each commit object's PGP signature header and Conventional Commit subject; no push, amend, rebase or history rewrite was performed.

## Phase 9d review round 1

1. Failure feedback now silently resynchronizes the saved handled count when the ViewModel counter resets. A restoration with the same count still does not replay feedback. `WizardTimesStepTest#firstFailureAfterRestorationWarnsOnceAndRevealsTheFirstError` consumes one failure, restores with a fresh zero counter, proves no restoration haptic, starts with the first error offscreen, then requires exactly one new Warning and the entire first error group above the footer. Before the fix it failed with `[Warning]` instead of `[Warning, Warning]`; after the fix it passes. The isolated runs pay their SDK cold cost; final warm measurements are recorded below. Logs and timing JSON are in `build/phase9d-review1/`.

2. Picker defaults are now separate from the equal-by-value flight cards. `OpenFlightPicker` samples the injected clock once; the entry-scoped ViewModel keeps that request and opening instant in memory, and `DismissFlightPicker`/Back close it. Configuration recreation retains the ViewModel and M3 picker state; durable recovery contains no open dialog. UI emissions use the fixed opening instant rather than reading the clock. `WizardTimesClockTest` failed before the fix because an empty card's minute changed; both cards now remain equal at clocks one minute apart, while an explicitly opened picker receives the current airport-local minute. The JVM ViewModel test also proves no clock reads on unrelated format/pill changes, one read per opening, unchanged empty-card equality, and Back dismissing before navigation. Existing Times derivation and recomposition tests pass.

3. Removed the max-line-length suppression from all 32 phase-touched files that carried it, wrapping the one 127-character subtitle expression in `WizardUiState.kt`. Every affected line now fits 120 characters. `CfPickers.kt`'s justified parameter-order/modifier suppressions are unchanged. The change is formatting only; `make format` passes, and the final quick/layout runs cover the affected source and test classes.

4. `RootFontWarmup` accepts the injected worker; `CfRoot` supplies `graph.dispatchers.io` (production still uses IO). The root-theme test drives that worker's test scheduler and asserts the awaited barrier and exactly four calls directly, replacing both polling waits. Its motion/barrier/count/fonts-ready assertions remain. The fake checks the injected dispatcher in the controlled case; the companion real-IO frame-commit test retains its physical off-main-looper assertion. Quiet baseline: 439 ms, including activity setup 124 ms, `setContent` 240 ms, and the final preloader wait 17 ms. After: 378 ms in the same three-case class run. The host's reported 1.20–1.74 s overruns did not reproduce here. Both class runs pass; no real font-loading work is added to the fixture.

5. The seed-refresh fixture now warms a complete build/save/load/refresh/trip-read path once per JVM in `@BeforeClass`, rather than only decoding the seed inputs. It creates and deletes a disposable file store on IO and retains only `Unit`; every real case still gets a fresh file store, and all existing assertion bodies are unchanged. Temporary per-step timers were removed. CurrentAnchor fell from 428 ms to 253 ms; LegacyAnchor from 257 ms to 174 ms. The host's 1.07 s overrun did not reproduce here. An intermediate JSON-only warm-up left first-use data-source/file-path costs and measured 311/180 ms, which is why the final warm-up exercises the actual fixture path.

| Seed step | Baseline CurrentAnchor | Baseline LegacyAnchor | After JSON-only warm-up, CurrentAnchor |
|---|---:|---:|---:|
| Build old seed | 62.406 ms | 18.461 ms | 29.366 ms |
| Edit fixture state | 3.006 ms | 0.130 ms | 3.826 ms |
| Save old seed (legacy includes removing the anchor key) | 37.605 ms | 36.977 ms | 33.833 ms |
| First identity read / restore / refresh | 162.779 ms | 102.445 ms | 118.599 ms |
| Read trips | 23.563 ms | 19.883 ms | 12.482 ms |
| Read persisted state | 18.395 ms | 9.152 ms | 14.651 ms |
| Build expected seed | 20.966 ms | 14.246 ms | 21.654 ms |
| Compare refreshed accounts | 2.460 ms | 1.282 ms | 2.088 ms |
| Reopen source / read trips | 55.790 ms | 44.464 ms | 29.234 ms |
| Reload / compare state | 13.094 ms | 9.198 ms | 12.525 ms |

6. The endpoint transport flake remains unresolved. The saved host XML identifies only `DataFailure.Offline`; it contains no underlying IOException or transport events. Reading `MockApi` and `ApiClient` establishes a new JUnit rule/server per parameter, an ephemeral listening port from `server.start()`, a fresh default client/connection pool per API client, and no connection reuse between parameterized cases. `reply` registers a route rather than enqueuing responses. Dispatch matches `(method, encodedPath)`, so the airports query is checked by the contract assertion but cannot change route selection; unmatched routes return 599 and map to a server failure. `RecordingDeadline.await` suspends forever. These paths do not explain the observed Offline failure.

   A temporary, failure-only OkHttp EventListener recorded connection addresses, transport phases and sanitized IOException classifications/stack frames without headers, bodies or credentials. Five rounds (210 cases) passed in 2.38 s tier wall / 0.666 s XML class total. A final bounded twenty-round run (840 cases) with sixteen CPU workers also passed, in 7.20 s tier wall / 4.064 s XML total. Every temporary diagnostic and repetition was removed, and the original 42-case class passed again (0.321 s XML total). There is no evidence identifying whether the original IOException came from connect, response headers, body read, OS resource pressure or another external cause. Per the review stopping rule, no speculative support change, retry or swallowed failure was added. Item 6 needs a future reproduction retaining the underlying transport failure.

### Review round 1 validation

Commands ran from `feat/phase-9d-wizard-flights`. All five fixes have separate signed commits; the sixth review item has this investigation record because its cause was not identified. There is no new OQ, brief refresh, copy, dependency or test-boundary allowlist entry. No emulator, performance or profile tier ran, and Pixel was left running. Full command output and copied timing files are under `build/phase9d-review1/`.

| Command / selection | Result | Wall time |
|---|---|---:|
| `make targeted ONLY="feature:wizard:WizardTimesStepTest#firstFailureAfterRestorationWarnsOnceAndRevealsTheFirstError"` before / after | Required haptic regression fails before; passes after | 7.04 / 4.78 s, separate SDK cold costs 3.089 / 2.394 s |
| `make targeted ONLY="feature:wizard:WizardTimesClockTest#emptyCardsStayEqualWhenTheClockMovesByOneMinute"` before | Required card-equality regression fails before | 3.00 s |
| `make targeted ONLY="feature:wizard:WizardTimesClockTest feature:wizard:WizardTimesUiTest feature:wizard:TripWizardViewModelTimesTest feature:wizard:WizardTimesRecompositionTest"` | All 35 pass | 7.89 s |
| `make targeted ONLY="feature:wizard:TripWizardViewModelTimesTest#clockIsSampledOnlyWhenAPickerOpens"` after adding Back assertions | Pass | Gradle 2 s |
| `make targeted ONLY="app:RootThemeTest"` before / after | All three pass; final targeted root case 378 ms | 7.76 / 4.60 s |
| `make targeted ONLY="app:RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier"` | Pass with controlled IO scheduler | Gradle 6 s |
| `make targeted ONLY="core:data-mock:MockSeedRefreshTests"` before / final | All four pass; CurrentAnchor 428 / 253 ms | 3.32 / 2.70 s |
| Same seed class, intermediate JSON-only warm-up | All four pass; CurrentAnchor 311 ms | 2.70 s |
| `make targeted ONLY="core:network:EndpointContractTest"`, temporary five-round diagnostic | 210 pass; no IOException | 2.38 s |
| Same endpoint command, temporary twenty-round CPU-load diagnostic | 840 pass; no IOException | 7.20 s |
| `make targeted ONLY="core:network:EndpointContractTest core:wizard:WizardPickerDatesTest feature:wizard:WizardTimesStepTest feature:wizard:WizardReturnStepTest feature:wizard:TripWizardViewModelReturnChoiceTest feature:wizard:WizardBoardCaptureSeedTest feature:wizard:WizardFlightBoardSeedTest"` | Original endpoint 42, picker dates six, feature 74 pass; restoration case 140 ms warm | JVM Gradle 1 + 2 s; feature tier 6.86 s |
| `Scripts/run-tier.sh layout-9d-review1 --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'app/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout` | Once; all 264 pass, zero overruns or hangs | 12.02 s |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug` | Once; pass | 10.39 s |
| `make quick` | Once; pass, zero overruns or hangs; 1,634 freshly measured cases / 4,669 collected or reused results; all four tooling groups pass (168 / 25 / 49 / 9); build-logic 37 and timing seven collected from up-to-date outputs | 45.15 s total; main tier 22.32 s |

Final quick measurements: root-theme barrier case **352 ms**, seed CurrentAnchor **242 ms**, seed LegacyAnchor **214 ms**, restoration feedback **77 ms**. The clock/ViewModel case is **4 ms**, and the existing card recomposition guard is **31 ms** including its class warm-up. RootThemeTest's class warm-up is 89 ms, WizardTimesStepTest's 26 ms; the first isolated SDK costs are kept separate above. No test budget was widened, no required command was blocked by the sandbox, and no test was retried after a final-quick failure.

## Phase 9d review round 2

1. Failed Continue counts are held in a ViewModel map keyed by `WizardStepKey`; each active Times UI receives only its step's count. The saved handled value and round 1's silent reset handling are unchanged. The JVM regression runs a successful outbound Continue, chooses the existing home route, fails home validation, returns to Outbound, and requires its counter to remain zero; it failed before the fix (actual one). This stack includes the existing ReturnRoute entry, so two individual Back events reach ReturnChoice and the third reaches Outbound; no navigation behavior changed. The redundant screen-level Back regression was removed during final cleanup: its custom host supplied Outbound's count as zero and could not detect the ViewModel bug (AGENTS Tests §6). `homeFailureDoesNotFollowBackToOutbound` covers the actual counter regression. The restoration regression remains required.

2. The width discrepancy came from using `wizardRow` (Figtree, zero tracking) for the board's `.ios` pills (platform font, −0.01 em). Size 17/22, regular weight, `tnum` and 6/12 padding already matched. `wizardPicker` is now generated from `Wizard.dc.html` and `theme.css` and maps the platform face to Android's default system family; all other styles are unchanged. Pill row-fit uses the visual intrinsic width without a 48 dp width floor; the target keeps a 48 dp height and Compose's minimum touch area, while the fill remains 34 dp tall at font scale 1. The geometry regression uses the exact 390 × 844 / density-3 capture fixture and actual card/shell gutters. Before the fix, Departs' label started at 207.33 dp and the pills at 255.33 dp (stacked); after it they share the row. It also checks 48 dp targets and the 34 dp drawn pill. The existing 280 dp / 1.3 stacked case remains. The previous documented 390 dp stacking exception above is corrected; OQ-A398's fit default itself is unchanged. Pixel-identical SF Pro and Android system glyphs are not assumed; the final capture measurements are recorded below.

3. Inline rows now center the label and both target boxes within their measured row height. The visual fill is centered in each target, so its center coincides with the label's center rather than aligning the label's top to the 48 dp target. The geometry assertions compare the label directly with both `.visual` bounds (±0.5 dp), on the English board fixture and Dutch pixel profile. Stacked placement is unchanged. At font scale 1 the previous 40 dp label started at the top of the 48 dp target, putting its center 4 dp above the 34 dp pill's center; that offset is removed.

4. No matching chevron/iOS tertiary-label color exists in `ds/chronoflow/tokens.json`, `theme.css`, the Android overlay, or generated `CfLightColors` / `CfDarkColors`. The board's `rgba(120,120,128,0.7)` is a literal on the choice/row chevrons, not a named color token. Material's generated mapping uses dawn for tertiary and does not supply this label color. Added **OQ-A400**, default **keep `mutedForeground` until a token exists**, as requested. `WizardChoiceCard` remains unchanged, and `WizardReturn`, `WizardReturnDark`, `WizardReturnNL` retain this recorded color difference. No literal, alpha adjustment or new color token was invented.

5. The root-theme test now drains `ioDispatcher.scheduler.runCurrent()` before asserting zero preload calls, while the commit Deferred is still incomplete. All existing barrier/motion/count/fonts-ready assertions remain. A temporary mutation moved `warmFonts` before `firstFrame.await()`; the strengthened test failed at the intended assertion, **expected 0, actual 4** (isolated SDK-cold case 2.052 s). The mutation was restored byte-for-byte. The complete three-case class then passes; final warm measurements are recorded below. No production font-ordering code is committed for this item.

6. The connection rule was a real mismatch: the board declares `border-top: 2px dotted var(--border)`, while the Kotlin rule used a 2/4 dp dash effect with round caps. At @3x the host board's interior marks are 6 × 6 px on a 12 px pitch; the old capture has elongated marks on an 18 px pitch (10 fully opaque horizontal pixels plus antialiased ends). The rule now uses the generated 2 dp dot width for stroke/height, equal 2 dp marks and spaces, and butt caps, matching the rendered board's square dots. The existing `border` token is already correct in both appearances. The effect and geometry stay inside `drawWithCache`, with no per-frame allocation. No new literal or geometry token was needed.

### Review round 2 captures and validation

`make captures ONLY=WizardOutbound,WizardReturn` ran **once**, writing 27 PNGs (26 board counterparts plus `WizardOutboundScrollErrors_noBoard`), tier wall 16.05 s / command wall 16.12 s. The host renders and Android captures were read directly for `WizardOutbound`, `WizardOutboundErrors`, `WizardOutboundStop` and `WizardReturnFlightPastDarkNL`; all 26 matched-name pairs were then read in seven comparison sheets (`build/phase9d-review2/comparison-*.png`, board left / Android right, names in `comparison-index.json`). There is no new missing content or clipping in these boards. Settled 9c native header/progress/footer mapping, the existing iOS-selected One way state, model progress on incomplete multi-flight drafts, and the pinned Overnight values remain the recorded exceptions. The chevron remains OQ-A400's explicit token exception.

Full-resolution pixel scans of `WizardOutbound`'s filled Arrives pills (all images 1170 × 2532; measurements below in pixels, divide by three for dp):

| Measurement | Host board | Before round 2 | After round 2 |
|---|---:|---:|---:|
| Date pill width | 360 | 377 | 347 |
| Time pill width | 207 | 213 | 199 |
| Solid date glyph run | 279 | 293 | 266 |
| Solid time glyph run | 129 | 133 | 120 |
| Pill fill RGB | 234,237,240 | 234,237,240 | 234,237,240 |
| Departs row at 390 dp | Inline | Stacked | Inline |
| Connection rule interior pitch | 12 | 18 | 12 |
| Connection rule fully opaque mark width | 6 | 10 plus antialiased ends | 6 |

The Android system face is slightly narrower than the host's SF Pro (date −4.33 dp / time −2.67 dp); both use the board's system-face mapping, regular 17/22, −0.01 em, tabular digits and 6/12 padding. No width clamp, font shrinking, date abbreviation or font literal compensates for platform glyph metrics. The draw/touch geometry is separately guarded (34 dp fill, ≥48 dp target), and both label centers match the fills within 0.5 dp. The final connection dots are 6 × 6 px on a 12 px pitch in the same border RGB as the board. Raw spans are retained in `pixel-measurements.json`.

| Command / selection | Result | Wall time |
|---|---|---:|
| `make targeted ONLY="feature:wizard:TripWizardViewModelTimesTest#homeFailureDoesNotFollowBackToOutbound"` before the fix | Required regression fails, expected zero / actual one | Gradle 2 s |
| `make targeted ONLY="feature:wizard:TripWizardViewModelTimesTest feature:wizard:WizardTimesStepTest feature:wizard:WizardTimesRecompositionTest"` final | 19 retained cases covering process restoration, the ViewModel counter regression and card invalidation; the historical run included one now-removed screen test | 6.62 s before cleanup |
| `make targeted ONLY="feature:wizard:WizardDateTimeRowLayoutTest#pillsReflowWithoutChangingTheirIdentity"` at the actual board density, before the fix | New English board case fails (pills 48 dp below label); existing Dutch cases pass | Gradle 5 s |
| `make targeted ONLY="tools:tokengen:TypeScaleGoldenTest core:designsystem:CfTypeTest feature:wizard:WizardDateTimeRowLayoutTest"` | Generator/style checks and corrected inline cases pass | Gradle 2 + 2 + 5 s |
| `make targeted ONLY="feature:wizard:WizardDateTimeRowLayoutTest"` final | Three cases pass: centered English/Dutch inline, narrow stacked; target/fill geometry | 5.00 s |
| `make targeted ONLY="app:RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier"`, temporary mutation | Intended failure: zero calls expected, four actual; source restored | Gradle 5 s; 2.052 s SDK-cold case |
| `make targeted ONLY="app:RootThemeTest"` after restoring ordering | Three pass; strengthened case 307 ms warm, separate SDK cold 1.515 s | 3.95 s |
| Layout tier (`:feature:wizard`, `:core:designsystem`, `:app`, `-Pcf.tier=layout`, through `Scripts/run-tier.sh layout-9d-review2`) | Once; all 273 pass, zero timing overruns/hangs. Existing 62 flight guards and 97 Where guards unchanged | 11.91 s |
| `./gradlew :feature:wizard:lintDebug :app:lintDebug :core:designsystem:lintDebug :tools:tokengen:verifyTokens` | Once; all pass | 15.95 s |
| `make quick` | Once; assertions all pass, one touched timing overrun: root-theme barrier **1,242 ms > 1,000 ms**; 3,018 fresh measurements; no hangs. Later recipe stages did not start | 36.91 s command / 36.85 s main tier |

The quick timing failure is retained. Quick, captures, layout and Lint are not rerun. A narrowly scoped fixture correction and only the missed quick stages are recorded in the follow-up below. No emulator, performance or profile tier ran; no dependency or native library was added. The only new OQ is OQ-A400, and there is no Android-only copy or brief refresh.

### Root-fixture follow-up after quick

The only quick failure was the touched root-theme case's 1,242 ms budget overrun; its assertions passed. Draining the shared test scheduler can also run the process lifecycle's session-activation work, and the default `LaunchPlatform.controller` is cached outside the recording application. The root fixture now detaches its unrelated session-activation observer before starting the host and supplies its own lazy controller through the existing `AppGraph.launchProvider`. It asserts that the overlay is inactive, retaining the real `CfRoot`, first-commit Deferred, motion local, dispatcher checks, zero/four preload assertions and fonts-ready assertion. The allowlist and production code are unchanged.

Removing only the session observer measured 321 ms warm; adding the independent controller measured **298 ms warm** (307 ms before these fixture changes). These quiet runs do **not** establish the cause of, or prove removal of, the 1,242 ms parallel-load overrun. No budget is widened and the failed quick run is not described as passing. All three cases pass after isolation (SDK cold 1.576 s; other warm case 47 ms), through `make targeted ONLY="app:RootThemeTest"` in **4.36 s**. The intermediate observer-only run took 4.71 s. No further timing trials were run. The fixture change is a separate signed follow-up commit because Task 6 had already started; no completed commit was rewritten.

Only the quick recipe stages that had not started were run afterward:

| Command | Result | Wall time |
|---|---|---:|
| `Scripts/run-tier.sh build-logic-9d-review2 --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | Pass; 37 collected from up-to-date outputs | 1.18 s |
| `Scripts/test_scripts.sh` with the same exported source paths as make | 168 scripts, 25 contracts, 49 localization and nine icons pass | 20.24 s total (groups 19.768 / 0.116 / 0.017 / 0.062 s) |
| `make timing` | Pass; seven collected metrics from up-to-date outputs | 0.76 s |

Final status: all functional assertions, captures, layout guards, Lint and token verification pass; the single full quick result retains its touched root-theme timing failure. A full run under parallel load remains unverified after the test-only fixture isolation. Commands were not blocked by the sandbox. Pixel was left running; no push or history rewrite occurred.

### Final cleanup

The host's `make check` passed on `eb0edba7`. Removed `returningToOutboundDoesNotReplayHomeFailureFeedback` and its test-only saveable host/imports because they supplied a pre-corrected count and could not fail for the production ViewModel bug. The remaining `WizardTimesStepTest` has nine cases; together with nine `TripWizardViewModelTimesTest` cases and the recomposition case, the retained group has 19. Historical execution totals above remain measurements of their original runs. `make targeted ONLY="feature:wizard:WizardTimesStepTest"` ran once: all nine cases pass in 6.53 s tier wall, with zero budget overruns. No other test or build command ran for this cleanup.

## Phase 9e

2026-11-20. [Brief](phases/phase-9e-wizard-sleep-questions.md). Sleep, age/shift-work questions, excluded states, the free-day answer sheet and server route errors are implemented. The rendered-board comparison remains an external acceptance step; the captures below are ready for the orchestrator.

### Review round 1

Host `make check` passed on `e33a4799`. Reviewed the host-rendered boards and all 13 supplied board/capture pairs.

1. Chronotype text now aligns at the top; Return and Question retain their centered alignment. CSS includes a 2 dp border outside its 10/14 dp padding, while Compose's drawn border reserved no space. Adding that inset removes 4 dp of extra text width and restores 4 dp of missing natural height: a two-line hint uses 22 + 2 + 36 + 20 + 4 = 84 dp. The generated line heights and padding already matched the board. The layout regression failed first (early title 403.43 dp, intermediate 412.57 dp), then all three profiles passed, including 12 dp top and 16 dp horizontal content insets. `make targeted ONLY='feature:wizard:WizardChronotypeGridLayoutTest#gridUsesEqualCellsAndFitsTheLongestHint'`: 5.97 s tier wall, 2.692 s SDK warm-up, 114/73 ms warm cases.
2. Free-day and airport/stop-search sheet headings now share generated `wizardSheetTitle`: Fraunces 600, 17/22, −0.01 em. Both previously used Figtree `wizardButton`. `WizardSheet.dc.html`'s form h2 has no family override; `bundle.css` supplies the display family and tracking. Its declared Fraunces range ends at 500, so Android requests 600 from the existing Fraunces family with normal weight synthesis, matching the browser rather than adding a font asset. The generator reads and validates that CSS inheritance. Generator, font mapping and both production sheet tests pass; the screen tests inspect typography through tagged layout semantics, without checking copy.
3. `CfTimePickerSheet` remembers whether an observed dial value has changed, instead of comparing its final value with the initial clock. Returning to the initial 12:00 can therefore store noon for an unset field; a genuinely untouched Done still reports false, and the existing rounding helper still suppresses a write equal to a stored value. The new regression failed with `(12, 0, false)` before the fix and passed afterward. All seven `CfTimePickerSheetGuardsTest` cases pass: 5.86 s tier wall, 2.230 s SDK warm-up, 64–94 ms warm cases. No feature/model confirm rules changed.
4. PickerTime's capture setup alone sets `Settings.System.TIME_12_24` to `24`, asserts the production sheet's `PickerIs24Hour` semantics, and restores the previous setting in teardown. The seed and production hour-cycle behavior are unchanged. The refreshed image shows 23:30 with the 24-hour dial and no AM/PM selector.

The single review capture command was `make captures ONLY=WizardSleep,PickerTime,WizardSheetFreeSleep,WizardAirport,WizardStopSearch`. It passed in **16.18 s tier wall**, writing exactly **16 PNGs**: three Sleep states (two `_noBoard`), PickerTime, two free-day sheets, four airport sheets and six stop-search sheets. No second capture run. Inspected the refreshed Sleep, PickerTime, light/dark free-day and airport/stop titles. Exact border-color runs in the full-density Sleep images measure **84 dp** for both chronotype rows in both board and capture; the first row starts at 378.00 dp in the board and 378.33 dp in the capture. The previous header, footer, server wording, native sheet and Fine-tune seam differences remain as recorded below. Evidence and PNG hashes/dimensions are under `build/phase9e-review1-evidence/`.

| Verification | Result |
|---|---|
| Targeted chronotype layout regression | Three profiles pass; red/green and wall times above |
| `make targeted ONLY='tools:tokengen:TypeScaleGoldenTest core:designsystem:CfTypeTest feature:wizard:AirportSearchSheetTest feature:wizard:WizardAnswerSheetTest'` | 6 + 2 + 12 + 8 cases pass; Gradle walls 2 s / 2 s / 7 s (rounded), wizard tier wall 7.88 s |
| Targeted time-sheet regression and complete class | Seven cases pass; red/green and wall times above |
| Wizard + core UI layout tier, once | 246 + 263 = **509 cases pass**, **19.55 s** tier wall, zero overruns/hangs; SDK warm-ups 2.627/2.231 s, slowest warm case 190 ms |
| Touched-module lint + token verification, once | Pass; 10 s Gradle wall (rounded) |
| App lint, once | Pass; **11.06 s** command wall |
| `make quick`, once | **Pass, 61.67 s** total command wall. Main tier **37.80 s**, 3,167 measured / 4,838 collected or reused results, no overruns/hangs (slowest warm case 808 ms). Build-logic 37 reused results, 1.38 s; tooling 168 + 25 + 49 + 9 tests pass; serial timing tier seven reused results, 0.88 s. Total includes tooling and exceeds the 60 s warm target by 1.67 s. No retry. |

The layout command was:

```sh
Scripts/run-tier.sh 9e-review1-layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'core/ui/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/ui/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest :core:ui:testDebugUnitTest -Pcf.tier=layout
```

Lint and generation verification used `./gradlew --console=plain :feature:wizard:lintDebug :core:designsystem:lintDebug :core:ui:lintDebug :tools:tokengen:verifyTokens`, then `./gradlew --console=plain :app:lintDebug`. `make tokens` regenerated the token output with its generator change. No new copy, OQ or brief patch was needed. No emulator, perf, profile or `make check` tier ran in this review round.

### Review round 2

1. The time sheet saves its interaction flag alongside M3's saveable dial state. `StateRestorationTester` reconstructs a new dial instance: changing it to 07:13, restoring and confirming failed with `touched = false` before the fix and now retains true. Opening, recomposing, restoring and confirming an untouched dial retains false. The first `snapshotFlow` emission remains ignored, so restoration itself never marks the dial touched. The saveable flag contains no copy, identity or secret.
2. OQ-A419 records the orchestrator's decision superseding round 1's synthesized 600: sheet headings use shipped Fraunces 500 opsz20 at 17/22, −0.01 em, with synthesis explicitly disabled by the generated token. Tokengen resolves requested Fraunces weights above 500 to 500 while retaining the optical size. The unsupported 600 face was removed; no font was added. Both header tests now assert the concrete Fraunces20 family, weight 500 and `FontSynthesis.None`, independently of the token's requested name. Both failed against round 1's 600 before this fix. Generator goldens cover the resolved face and synthesis flag, plus higher-weight resolution at all three optical sizes.
3. `AirportSearchSheetGeometryTest` measures a title line against `wizardSheetTitle.lineHeight`, so it follows the rendered heading's token instead of the button token. Both currently specify 22 sp; the three profiles × four geometry cases pass.

Round 2 verification (command wall times unless marked tier):

| Command | Result |
|---|---|
| `make targeted ONLY='core:ui:CfTimePickerSheetGuardsTest'` | Nine pass, **6.10 s** (5.99 s tier); SDK warm-up 2.602 s, warm cases 55–138 ms. The two restoration methods were first run alone: changed failed before the fix, untouched passed; both passed after the fix. |
| `make targeted ONLY='tools:tokengen:TypeScaleGoldenTest tools:tokengen:DeterminismTest tools:tokengen:OutputsTest core:designsystem:CfTypeTest feature:wizard:AirportSearchSheetTest feature:wizard:WizardAnswerSheetTest'` | 11 generator + 3 type + 20 sheet cases pass, **10.59 s**; wizard tier 6.47 s, zero overruns. Both header methods were first proven red (500 expected, 600 actual), then green. |
| `make targeted ONLY='feature:wizard:AirportSearchSheetGeometryTest'` | 12 pass, **6.68 s** (6.60 s tier); SDK warm-up 2.547 s, slowest warm case 92 ms. |
| `make captures ONLY=WizardSheetFreeSleep,WizardAirport,WizardStopSearch`, once | Pass, **16.38 s** (16.32 s tier); exactly **12 PNGs**: two free-day, four airport and six stop-search states. Inspected light/dark free-day and airport/stop headings. No second capture run. |
| Wizard + core UI + design-system layout tier, once | 246 + 263 + 8 = **517 pass**, **19.19 s** (19.15 s tier), zero overruns/hangs; SDK warm-ups 2.528/2.124/2.273 s, slowest warm case 178 ms. |
| App + touched-module lint and token verification, once | Pass, **16.62 s**. |
| `make quick`, once | Gradle assertions and Spotless pass; the tier fails only two untouched timing gates, listed below. **37.59 s** command wall. No retry; build-logic, tooling and serial timing were not reached. |

Layout used:

```sh
Scripts/run-tier.sh 9e-review2-layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'core/ui/build/test-results/testDebugUnitTest/*.xml' --results 'core/designsystem/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/ui/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/designsystem/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest -Pcf.tier=layout
```

The quick tier reported exactly:

```text
run-tier: over budget: robolectric net.chronoflow.app.RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier 1273 ms > 1000 ms
run-tier: over budget: robolectric net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed 3356 ms > 3000 ms
make: *** [quick] Error 3
```

The second gate is the first-case-per-class warm-up allowance, not the 1 s warm-case budget. Neither test was changed; neither was retried. The tier measured 3,171 cases and collected or reused 4,842 results; there were no assertion failures or hangs. The slowest warm case among the touched classes was 393 ms.

Lint and token verification used `./gradlew --console=plain :app:lintDebug :feature:wizard:lintDebug :core:designsystem:lintDebug :core:ui:lintDebug :tools:tokengen:verifyTokens`. `make tokens` regenerated the token output in 1.77 s. Evidence, timing ledgers and the capture manifest are under `build/phase9e-review2-evidence/`. No new copy or brief patch was needed; OQ-A419 is the only added OQ. No emulator, perf, profile or `make check` tier ran in this review round.

### Sleep clocks and state

`WizardClockRounding` rounds to the nearest five minutes, with remainders 0–2 down and 3–4 up, wrapping midnight: `07:12 → 07:10`, `07:13 → 07:15`, `23:58 → 00:00`. It writes ASCII `HH:mm`, independently of the process locale. Only `habitual_bed`, `habitual_wake`, `free_day_bed` and `free_day_wake` use it. Flight times retain 9d's minute precision.

The ViewModel and composable both use `WizardClockRounding.change(stored, hour, minute, touched)`. Untouched Done, a result equal to the stored clock, Cancel, Back and scrim dismissal write nothing and produce no Selection haptic. Only a changed confirm writes and produces one Selection. An off-grid stored value such as `07:12` survives untouched Done. Empty clocks show the generated Time placeholder; the unstored dial default is 12:00.

`CfTimePickerSheet` stays neutral: no rounding or feature copy. Its normal, defaulted `TimePickerState` parameter supports state hoisting without a test-only callback. M3 saves the in-progress dial across recreation; the ViewModel holds the open field and answer-sheet edits. Unsaved sheet edits and an open time picker do not survive process death. A recovered `promptSheetID` reloads the answer sheet from the recovered review and saved profile.

Pills follow the account time format. The native dial follows `DateFormat.is24HourFormat` and the application language. The title takes focus after `SheetValue.Expanded`. Time rows are single merged targets with label and value/not-set semantics. The chronotype grid measures equal-height rows inside layout and becomes one column below 320 dp available width. Not sure retains the `chronotype` key with JSON null. Only the existing model validates the 180–840 minute night, clears errors on edits and advances to Checking. Failed Continue retains the per-step counter and emits one Warning without replay on recreation.

The free-day header centers its title against the sheet edges, independent of the action widths, and stacks the title above the actions when the inline space is insufficient. Its geometry regression failed before the fix (pixel8 title center 207.43 dp versus sheet center 205.33 dp), then passed within 0.5 dp. No measured size is stored in state.

### Board drafts and capture files

Every capture is under `build/captures/`. Except PickerTime, profiles are 390 × 844 dp, density 3, zero insets. PickerTime is 375 × 812 dp. The Large board uses Dutch and font scale 1.3. Board names without NL use English; DarkNL uses Dutch and dark appearance.

| Capture name | Draft |
|---|---|
| `WizardSleep` | Guest Sleep, 23:30–07:15, intermediate, progress 0.30 |
| `PickerTime` | Guest Sleep with the bedtime sheet open; stored time 23:30 |
| `WizardSheetFreeSleep`, `WizardSheetFreeSleepDarkNL` | Member Fine-tune seam, `free_day_times::` open, saved camel keys declared / 00:30 / 09:00; progress 0.80 |
| `WizardQAge`, `WizardQAgeDarkNL` | Guest question 0, age only, no selection, disclaimer; progress 0.54 |
| `WizardQShift`, `WizardQShiftNL`, `WizardQShiftLarge` | Guest question 1 of [age, shift], age 18–64, no shift selection; progress 0.64 |
| `WizardExcluded`, `WizardExcludedDarkNL` | Guest, session age under 18, default note; progress 0.60 |
| `WizardExcludedShift`, `WizardExcludedShiftDarkNL` | Member, shift answer only in savedProfile (`shiftWork = true`), Saved note; progress 0.60 |
| `WizardSleepMember_noBoard` | Member Sleep, saved-for-next-time subtitle; progress 0.50 |
| `WizardSleepError_noBoard` | Member Sleep, 23:00–01:00, `profile.habitual_bed` error; progress 0.50 |
| `WizardQAgeMember_noBoard` | Member age question, no disclaimer; progress 0.60 |

Guest board drafts are render-only until 11a. No guest board capture triggers generation. PickerTime is Robolectric-only; device checks seed WizardSleepMember. No retired board is registered.

The single command `make captures ONLY=WizardSleep,PickerTime,WizardSheetFreeSleep,WizardQAge,WizardQShift,WizardExcluded` wrote exactly **13 board PNGs + 3 `_noBoard` PNGs** (tier wall **19.13 s**, zero timing overruns). After inspecting the images, the header-centering regression above was fixed, the 42-case matrix was rerun, and only the two free-day PNG outputs were refreshed with:

```sh
Scripts/run-tier.sh 9e-header-captures --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest -Pcf.tier=capture -Pcf.captureOnly=WizardSheetFreeSleep --tests '*WizardBoardCaptureTest'
```

That refresh passed in **8.36 s command wall**, selected the existing capture class and wrote two outputs (68 other cases skipped at the output filter). An attempted parameter-level Gradle filter matched no tests; it produced no capture evidence and was replaced by the class/output filter above. The PNG manifest, dimensions and hashes are in `build/phase9e-evidence/capture-manifest.json`; PickerTime is 1125 × 2436 px and the other files are 1170 × 2532 px. No stored Roborazzi references were recorded.

### Copy and behavior decisions

Server questions and reasons use the plan-copy catalog, with these substitutions for the raw board wording:

| State | Server key used |
|---|---|
| Age | Which age group are you in? |
| Shift work | In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule? |
| Under-18 exclusion | This plan is not available for travellers under 18. |
| Shift exclusion | This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule. |
| Free-day sheet | What time do you go to bed and wake up on free days? |

Dutch comes from the same catalog, including “In welke leeftijdsgroep zit je?” and “Hoe laat ga je op vrije dagen naar bed en sta je op?”. The question eyebrow uses the generated wizard resource, uppercased with the application locale. Titles longer than 80 characters use `wizardLongTitle` (24/30); the shift title is long in both languages. Option order follows the age shape, or No then Yes for shift work. An unknown age option retains its raw label and value. Pending answers disable semantics without changing the drawing and advance through the model's existing 250 ms clock. Only guest age has the disclaimer. Back cancels the pause and skips Checking on the first question.

The formatter travels as an explicit parameter through a compatible `WizardScreen` overload. Existing positional and trailing-lambda calls retain their signature. `TripWizardDependencies.copyFormatter()` defaults to null; the ViewModel loads it once. Production dependencies cache the formatter behind a Mutex and share AppGraph's existing `CopyCatalogProvider`, whose resource read and decode run on injected IO. No second catalog loader was added. A null formatter renders empty server copy; unknown server keys keep their key text. Unknown wizard-table keys retain the existing generic-error fallback.

`fieldCopy` checks the plan catalog first, preserving FieldMessage arguments, then the generated wizard table. It serves Where/home-route errors and answer-sheet errors. This closes OQ-A399 for those surfaces; Times retains its 9d behavior until 9f. Route errors are restricted to the route's global leg range; their rows are invalid and their lines are polite live regions. Any route edit clears them through the existing model.

The answer sheet loads review answers before saved camel keys, normalizes error paths with the existing journey mapper and preserves error arguments. Only FreeDayTimes is supported here. Declared clocks must exist before Done is enabled; validation uses `WizardOptionalAnswer`. Validation and ReviewExpired map to the existing error. A final submit regression also proved that malformed trip answer types can throw JsonDecodingException; the submit path now maps runtime failures to that same field error, after rethrowing caller cancellation. The stale-review fixture uses a real stale trip-leg path: the model does not check identity expiry in `answerOptional`.

Excluded note precedence is Edit, then Saved, then Default. Footer Close always closes directly; header × closes a guest exclusion directly and asks a member with changes first. Change my answers calls the existing model: members clear only excluding answers (including saved ones) and recheck; guests restart at their first question. Only Excluded opts into the secondary footer and plain primary fill. `CfButton` gained a defaulted container override; existing button styles and 9d's primary remain unchanged.

OQ-A418 settles OQ-A350: Sleep displays the model's **0.50**, even after ReturnFlight **0.55**. The existing progress bar already animates both directions; no progress-floor behavior was added. The former hold-at-55 text was a default, not a user decision.

| Android-only copy (needs review) | en | nl |
|---|---|---|
| `android_wizard_time_sheet_steps` (OQ-A404) | Steps of 5 minutes. | Stappen van 5 minuten. |

The string input, generated English and Dutch resources and plan approval row landed together. OQ-9e-1 through OQ-9e-18 are recorded as **OQ-A401–OQ-A418**.

### Guard scope and comparison hand-off

`WizardSleepQuestionGuardsTest` has exactly **42** cases; `WhereServerErrorGuardsTest` has exactly **3**. The light-only matrices are unchanged from the brief. The final 42-case header matrix passed in **7.99 s tier wall**, with zero overruns. The complete 45-case matrix passed in **9.33 s tier wall** before the header adjustment. The full module layout tier below rechecks all cases after that change.

Where keeps 9c's shared-border exemption for `wizard.route.stops`. M3's AM/PM cells have 40 dp layout height and effective 48 dp hit areas. The new defaulted `nativeTouchTargets` matcher checks those actual hit areas only for Role.Button nodes exposing Selected inside `wizard.time-sheet`; every designed control retains the strict 48 dp layout guard. A wrapper regression checks both native period buttons' hit bounds. All existing guard callers retain their behavior.

The PNGs were checked for clipping, missing content and native-window placement. **Rendered-board sign-off is pending with the orchestrator.** No Chromium launch was attempted. Compare all 13 board counterparts; fix or record every further difference. Expected differences are the server wording above, Material dial and drag handle, native 48 dp actions, the 64/72 dp option-height rule under Android font scaling, 9c's shell/progress, the Fine-tune seam behind the free-day sheet until 9f, and Sleep replacing GuestSleep behind PickerTime. PickerTime uses the seed's stored 23:30 rather than the raw board's 23:00. Phase 9f owns re-capturing the free-day sheet over the completed Fine-tune screen.

| Swift source name | Kotlin surface |
|---|---|
| `sleepScreen` | `WizardSleepStep` |
| `questionScreen` | `WizardQuestionStep` |
| `excludedScreen` | `WizardExcludedStep` |
| `WizardAnswerSheet` | `WizardAnswerSheet` / `WizardAnswerSheetState` |
| `WizardTimeControl` | `WizardTimeRow` + `CfTimePickerSheet` |

### Phase 9e verification

`pixel` ran both seeded `WizardSleepTimeSheetDeviceTest` cases once: **en/24 h 2.501 s**, **nl/12 h 1.393 s**; command wall **12.57 s** / tier wall **12.46 s**. Both assertions observed zero IO StrictMode violations. The helper retains WizardOutbound as its default board for 9d; 9e passes WizardSleepMember explicitly. `pixel` remains running.

Targeted results and final gate results are recorded below. Per-run JSON evidence and logs live in `build/phase9e-evidence/` and `build/phase9e-*.log`. First SDK/class warm-ups are distinct from warm-case budgets. No test budget was widened. No perf, timeline perf, baseline-profile, full check or other emulator tier was run: 9e has no wizard perf-budget row, and wizard profiles belong to Phase 13.

| Targeted checks | Final result and elapsed evidence |
|---|---|
| `core:wizard:WizardClockRoundingTest` | 16 cases pass; final isolated Gradle build 1 s (rounded) |
| `core:ui:CfTimePickerSheetGuardsTest`, `CfTimePickerDialogGuardsTest` | Sheet's five original cases and six unchanged dialog cases pass after the action target fix; 7 s initial build / 5 s isolated fix (Gradle rounded). Native period-hit regression passes separately in 6 s. |
| `WizardSleepQuestionSeedTest`, `WizardBoardCaptureSeedTest`, `WizardFlightBoardSeedTest` | 72 cases pass, 3.74 s tier wall |
| `TripWizardViewModelSleepTest`, `WizardSleepBoundaryTest`, `WizardSleepCompletenessTest`, `WizardSleepUiDerivationTest`, `WizardSleepStepTest`, `WizardChronotypeGridLayoutTest` | 29 cases pass, 7.49 s tier wall |
| Existing `WizardHeaderTest`, `WizardReturnStepTest`, `WizardTimesStepTest`, untouched sleep confirm, picker wrapper | Pass unchanged; wizard / core-ui builds 7 s each (Gradle rounded), wrapper tier wall 7.16 s |
| `WizardAnswerSheetStateTest`, `TripWizardViewModelAnswerSheetTest`, `WizardAnswerSheetTest` | Initial 15 JVM cases pass after the isolated error-path fix (3 s rounded fix build); six screen cases pass in 6.37 s tier wall; final answer-precedence + screen run passes in 7.00 s. Header regression added later; all seven screen cases pass in final quick. |
| `TripWizardViewModelQuestionTest`, `WizardQuestionStepTest`, `WizardQuestionLargeTextTest`, `WizardServerCopyTest`, existing `app:SessionGraphsTest` | All pass: isolated last-answer fix 2 s (rounded); 15 screen/layout cases 7.31 s tier wall; pending-border/copy/app follow-up 12.70 s command wall |
| `TripWizardViewModelExcludedTest`, `WizardExcludedStepTest`, existing `core:ui:CfButtonColorsTest` | All pass; wizard 6 s / button colors 5 s rounded builds, color tier wall 5.40 s |
| `WhereServerErrorUiTest`, `WhereServerErrorStepTest` | All six pass after correcting the test-catalog placeholder; isolated field-copy fix 2.49 s tier wall |
| Three new `KonsistRulesTest` methods, `QuestionKindsTest`, retired-board seed checks | All pass, 8.79 s command wall; 29 feature cases + 3 Konsist cases |
| `WizardSleepQuestionGuardsTest`, `WhereServerErrorGuardsTest` | 42 + 3 pass, 9.33 s tier wall; final full 42-case run after header fix 7.99 s |
| Malformed optional-answer regression | Fails before the runtime mapping with a suppressed JsonDecodingException; passes after, 2 s rounded build; final quick also passes it |

Isolated SDK-36 first-case costs were **2,694 ms** (chronotype grid), **2,271 ms** (time-sheet wrapper), **2,910 ms** (answer sheet), **2,988 ms** (large-text questions), and **2,774 ms** (route-error guards). These use the 20 s cold-SDK allowance. Subsequent class warm-ups included Sleep 69 ms, Question 152 ms and the sleep/question matrix 100 ms; they use the separate 3 s allowance. In final quick, Phase 9e warm cases were at most **268 ms**.

Final Lint passed once for `:app`, `:feature:wizard`, `:core:designsystem`, `:core:ui` and `:core:testing-compose`: **20.28 s command wall**. The same modules' layout-tier invocation passed in **21.33 s command wall / 21.25 s tier wall**, with **587 collected/measured cases**, zero overruns and zero hangs (app 70, wizard 246, core-ui 263, design-system 8; testing-compose has no local layout cases and is exercised through its consumers). All layout warm cases were ≤ **251 ms**. The existing wizard guard matrices and 9c/9d screens passed unchanged.

`make quick` ran **once**. Gradle's test assertions and Spotless passed, but the timing collector exited 3 after **55.41 s command wall / 55.35 s tier wall**, with exactly these two untouched overruns against 1,000 ms:

- `net.chronoflow.core.datamock.MockSeedRefreshTests#restoresTenDayOldSeedWithoutLosingUserStateLegacyAnchor`: **1,012 ms**.
- `net.chronoflow.app.RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier`: **1,576 ms**.

There were no hangs or other collector errors. The quick collector contains 4,667 measured/collected or reused cases; no Phase 9e case exceeded its budget. Per the user's host-load rule, neither test nor quick was retried and neither untouched test was changed. Quick stopped before build-logic, tooling and serial timing, so those remaining stages were run separately once; their results follow.

| Remaining quick stage | Result |
|---|---|
| Build logic | 37 passing XML results collected from the up-to-date task; 1.91 s command wall / 1.86 s tier wall |
| Script tooling | 168 tests pass in 20.693 s (20.94 s command including the following failed parity invocation) |
| Contracts | 24 cases passed initially; the parity case initially lacked Makefile's `IOS_REPO` / `WEB_REPO` environment. Only that case was rerun with those variables: pass, 0.14 s command wall. |
| Strings | 49 tests pass; 0.08 s command wall |
| Icons | 9 tests pass; 0.11 s command wall |
| Serial timing | 7 cases pass, zero overruns; 3.10 s command wall / 3.05 s tier wall |

The manual parity command was corrected to:

```sh
IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new python3 -I -m unittest discover -s tools/contracts-gen -t tools/contracts-gen -p 'test_*.py' -k test_matches_ios_generated_swift
```

Its original output was `AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. No source change was needed. The earlier targeted capture-filter attempt was:

```sh
make targeted ONLY="feature:wizard:WizardBoardCaptureTest#capturesTheBoardState*WizardSheetFreeSleep*"
```

It returned `No tests found for given includes: [*.WizardBoardCaptureTest.capturesTheBoardState*WizardSheetFreeSleep*](--tests filter)`. The successful class/output-filter capture command above replaced it; no required command remains blocked by the sandbox.

Step 0 found no additional upstream-name mismatch and needed no standalone brief-refresh commit. Later brief notes landed with their tasks: normal state hoisting instead of a main-source test callback; copy-render helpers needed by Task 5; non-null prompt answers, normalized error paths and a real stale-leg ReviewExpired fixture; narrowly scoped native AM/PM hit-area checks; and runtime submit-failure mapping. The final malformed-answer fix is its own signed commit because it was discovered after the task commits; no history was rewritten.

Tracked changes in all three read-only source repositories remain absent. Existing untracked future-phase briefs in this checkout were left untouched. No new external dependency, initializer, manifest entry, native library, secret-storage path or startup/timeline implementation was added.

## Phase 9f: checking and fine-tune

2026-11-20. [Brief](phases/phase-9f-wizard-checking-finetune.md). The shell now renders checking, its three failure cards, fine-tune and every optional-answer sheet. Screen state is mapped from the landed model; the model owns checking and retry time. Answer drafts stay in the entry-scoped ViewModel. Only Done applies them; closing a sheet discards edits. Password and verification-link storage paths are unchanged.

### Checking

| Checking clock | Lines (Done / Busy / Open) | Behaviour |
|---|---|---|
| 0–399 ms | Busy, Open, Open | Request running |
| 400–799 ms | Done, Busy, Open | First tick |
| 800–1,199 ms | Done, Done, Busy | Second tick |
| From 1,200 ms | Done, Done, Done | Completed request waits until the 1,500 ms presentation minimum |
| From 10,000 ms | Model progress retained | Polite slow line |
| 30,000 ms without a response | Failure | Server card and Error cue |

The tick table describes a request completed before the minimum display interval; a pending request keeps the final line busy. The normal member order is flights/time zones, saved answers, tailoring. Guests replace saved answers with their answers. Checking Again orders answers, flights/time zones, tailoring. Back cancels checking and returns to the last input step without a late cue. The visible back button is absent while checking; a failure restores it outside editing.

| Failure | Card | Retry |
|---|---|---|
| `Offline` | Cloud-off | Enabled |
| `Throttled` | Clock | Disabled until the model countdown reaches zero; rounded-up seconds and `LockoutWait` body |
| Other checking failures | Circle-alert | Enabled |
| Validation routed to an input step | That input step's catalog-first field error | No checking failure card or outcome cue |

A 41.2-second throttle shows 42, then 41 after one checking-clock second. Retry before zero makes no request. The failure-card secondary Close requests discard for a member with changes; a guest closes directly. Toolbar × asks to discard whenever there are changes, except for a guest on Excluded. Success, no-plan and failure cues map to Success, Warning and Error respectively. A saveable handled sequence prevents replay after recreation.

The arc uses the board's cubic SVG with uniform centred scaling. Its cached path, dash and stroke are built by `drawWithCache`; only draw reads the animation state. Animation is 3.6 seconds. Reduce Motion creates no transition, fixes the arc dot at t = 0.65 and replaces the line spinner with a static dot. Each line announces its status politely; decorative arc and icon semantics are cleared.

### Fine-tune and sheets

Card order is optional answers, preparation, preferences. Preferences stay after the two designed cards and below their first viewport on the standard board size. Prompt order comes from `optionalPrompts`; multi-leg rows retain the iOS route/city suffixes. Required rows carry the filled star and “Helps a lot”. Preparation clamps to 0–4. Sleep override prefill keeps an existing value, then uses profile, then saved profile. Guests have no override switch. Reviewing disables edits and shows the footer spinner; no-prompt Fine-tune uses Done.

| Sheet | Controls and commit rule |
|---|---|
| Seat side | Nullable radio selection; aisle/not sure commits JSON null |
| Room | Mode radios; declared time uses landing date and minute precision |
| Late check-out | Nullable radio selection |
| Latest wake | Native inline time picker; phone hour cycle, app language; compact input below 640 dp height |
| Busy hours | Editable/removable ranges; From date → From time → To date → To time; final confirm disabled for To ≤ From |
| Drinks | Habit radios and four 0–max steppers; None resets every drink; a step selects cups; empty cups rejected |
| Free-day sleep | Existing 9e options and rounded time sheets, now over Fine-tune |
| Light device | Radios and the iOS safety notice below the question |
| Blue blockers | Radios and decimal rating; numeric loads use the language separator; edited text is preserved; submission normalizes comma to dot |

Override and room use `CfTimePickerSheet` at minute precision, with no five-minute footnote. The free-day sheet keeps 9e rounding. Busy ranges keep the date/time dialogs. The answer header remains fixed while the body scrolls, including above the keyboard. Steppers expose a single adjustable accessibility node and separate 48 dp touch targets. All new remove targets and the rating field measure at least 48 dp.

### Seeds and captures

| Base seed | State |
|---|---|
| `WizardChecking` | Checking, tick 1, progress 0.60 |
| `WizardCheckingAgain` | Answers-first checking, tick 1, progress 0.70 |
| `WizardCheckingSlow` | Tick 2 plus slow line |
| `WizardCheckOffline`, `WizardCheckServer`, `WizardCheckThrottle` | Frozen failure; throttle 42 seconds |
| `WizardFineTune`, `WizardFineTuneMulti` | Single/multi-leg prompt order, progress 0.80 |
| `WizardFineTuneSleep` | Override enabled |
| `WizardFineTuneBusy` | Review in place; edits disabled |
| `WizardFineTuneNone` | No prompts; seed floor 0.95 |
| `WizardFineTuneSheet`, `WizardSheet` | Seat sheet over Fine-tune (same render state) |
| `WizardSheetRoom`, `WizardSheetCheckout`, `WizardSheetWake`, `WizardSheetBusy` | Corresponding open sheet |
| `WizardSheetDrinks`, `WizardSheetDrinksError` | Drinks and validation ring |
| `WizardSheetLight`, `WizardSheetBlue`, `WizardSheetBlueError` | Light and rating, including validation ring |
| `WizardSheetFreeSleep*` | 9e seed handed forward over real Fine-tune, OQ-A412 |

`WizardCompletionBoardSeed` and its hand-built prompts live in debug; a test compares them with `MockWizardPrompts.make`. The frozen checking clock and neutral UI override prevent requests during captures. Release readiness rejects the seed/prompt/clock classes and verifies the shipped override is null. `:feature:wizard` has no main/debug dependency on data-mock.

Capture inventory (48 PNGs, board names, in `build/captures/`):

- `WizardChecking`, `WizardCheckingDark`, `WizardCheckingNL`.
- `WizardCheckingAgain`, `WizardCheckingSlow`, `WizardCheckOffline`, `WizardCheckServer`, `WizardCheckThrottle`: base and `DarkNL` each.
- `WizardFineTune`: base, `Dark`, `NL`, `DarkNL`.
- `WizardFineTuneMulti`, `WizardFineTuneSleep`, `WizardFineTuneBusy`, `WizardFineTuneNone`, `WizardFineTuneSheet`: base and `DarkNL` each.
- `WizardSheet`.
- `WizardSheetRoom`, `WizardSheetCheckout`, `WizardSheetWake`, `WizardSheetBusy`, `WizardSheetDrinks`, `WizardSheetDrinksError`, `WizardSheetLight`, `WizardSheetBlue`, `WizardSheetBlueError`: base and `DarkNL` each.
- `WizardSheetFreeSleep`, `WizardSheetFreeSleepDarkNL`.

All use Reduce Motion and account time format Value24. The wake picker also sets/restores the phone's 24-hour setting. No stored Roborazzi references were added. `make captures ONLY=WizardChecking,WizardCheck,WizardFineTune,WizardSheet` ran once (19 seconds): 46 images succeeded; both wake cases failed on a fixture that queried the time-sheet tag. The corrected fixture reran only those two via:

```sh
env 'ORG_GRADLE_PROJECT_cf.captureOnly=WizardSheetWake' make targeted ONLY='feature:wizard:WizardBoardCaptureTest#capturesTheBoardState'
```

That passed in 10.06 seconds including skipped parameters and produced the remaining two images. The full capture command was not repeated.

### Guards and board comparison hand-off

`WizardCheckingFineTuneGuardsTest` has exactly 107 uniquely named light-only cases: 21 states × Dutch × small/pixel8/foldInner/largeFont (84); those states × English × small (21); Fine-tune/seat × Dutch × landscapeSideNav (2). All pass. The class took 7.23 seconds (10.78-second command); first case 2.502 seconds, warm maximum 0.186 seconds. Guards cover text, measured targets, safe area, overlap and headings. Native AM/PM buttons use framework touch bounds, as existing picker guards do. Small/large-font Dutch cases also check body scrolling and fixed visible actions. There are 22 base boards and 46 wrappers; the plan's former “26 boards” count was corrected. Seat's two board names share one guard state.

The orchestrator compares the 48 captures with the board renders at 390 × 844. This sandbox did not launch Chromium. Record visual differences from that comparison against these expected adaptations:

- Native M3 sheet handle/header actions, switches and inline time picker; time sheets for override/room and date/time dialogs for busy ranges.
- Static busy dot under Reduce Motion, preferences after the designed cards, and the existing 16 dp button spinner (board 18 dp).
- iOS model answers take precedence over board placeholders: default room is “When I land”, false checkout is “No”, and unanswered busy hours show zero ranges. No model behaviour was changed to force “Add”.
- Prompt questions use server catalog copy; free-day question therefore follows the catalog, as recorded in 9e. Requested Fraunces weights above 500 resolve to the shipped 500 face with synthesis off (OQ-A419).

### Swift → Kotlin coverage

| Swift helper/test | Kotlin coverage |
|---|---|
| `assertFineTune` | `WizardFineTuneStepTest` |
| `assertAnswerSheet` | `WizardAnswerSheetTest` |
| `testCheckOfflineScreenKeepsEditingAndGoesBack` | `closeOnFailureAsksOnlyWithChanges`, `backCancelsCheckingAndReturnsToTheLastInputStep` |
| `testDuoResizeKeepsFineTuneAndOpenAnswer` | `ratingSurvivesRestoration` |

The new model/ViewModel cases use `CheckingTestClock`, fake repositories, 60-second `runTest` caps and owned scopes. No screen-copy assertions, app-root logic tests, main-source test callbacks or clock-reading Compose mappers were added. Outcome counters and restoration tests cover transient feedback without racing animations. Catalog-first Times/Sleep field errors close OQ-A399.

### Validation record

Device: `pixel` AVD, Android 16/API 36, debug, functional animator settings. `make targeted ONLY="app:WizardAnswerSheetDeviceTest"` ran once: wake passed in 2.346 seconds; rating's geometry passed but activity-window keyboard teardown timed out (6.476-second case, 16-second Gradle command). The test now settles the focused sheet's IME, closes the sheet, then lets the base fixture close the activity. Only that method reran:

```sh
make targeted ONLY="app:WizardAnswerSheetDeviceTest#ratingFieldAndDoneStayAboveTheKeyboard"
```

It passed in 2.456 seconds (7-second Gradle command). Neither run logged a StrictMode disk/network violation. The two tests check native controls/IME only; they do not repeat model validation or add a journey.

The brief refresh corrected iOS/landed answer-default expectations and replaced private `ApiReviewPrompt.copy` use with its public constructor. Task 3 also recorded the additive `CfTimePickerDialog.confirmEnabled` parameter because only the date dialog previously had it. No landed model behaviour changed. OQ-A420–OQ-A438 carry the nineteen 9f defaults; no Android-only copy was added. No new runtime dependency, manifest initializer or native library was introduced. Wizard perf/profile tiers belong to Phase 13; none were run or requested for this phase.

Final checks (macOS host; JVM/Robolectric values are host results, not device performance):

| Command / scope | Result | Wall time |
|---|---|---|
| Task 0 `make targeted ONLY="feature:wizard:WizardServerCopyTest"` | 4 passed; disabling catalog lookup made the new case fail, restored case passed | 2.55 s |
| Task 1 checking mapper + checking/fine-tune ViewModels | 40 passed | 3.06 s |
| Task 2 answer-state + answer ViewModel suites | 51 passed; drinks-reset mutation failed, restored case passed | 3.21 s |
| Task 3 `CfTimePickerGuardsTest` | 4 passed; first 2.445 s, warm 0.049–0.184 s | 6.43 s |
| Task 4 seed suites / registry / release readiness | 82 / 75 / 13 passed | 4.28 / 3.45 / 5.00 s |
| Task 4 app debug seed wiring | 6 passed | 3 s Gradle |
| Task 5 checking structure and recomposition | All 9 pass in final quick; cleared-semantics mutation failed as intended, restored case passed | 0.212 s test sum in final quick |
| Task 6 failure cards / icon-generator tests | 4 / 11 passed | 5.95 s / 0.080 s Python |
| Task 7 fine-tune structure / layouts | 13 / 2 passed (final quick / final layout) | 0.306 s structure test sum; layouts included in final layout tier |
| Task 8 answer UI + ViewModel expansion | Initial 49-case run found duplicate wake tag and 44 dp busy target; both corrected cases passed | 7 s initial Gradle; 5 s correction Gradle |
| Task 8 final edited controls + Task 9 haptics | 5 passed | 6.71 s |
| Task 10 new guards | 107 passed, no overruns | 10.78 s |
| Task 11 device checks | Both passed as detailed above | Wake 2.346 s; rating 2.456 s |
| App, wizard, design-system, UI `lintDebug` | Passed after replacing configuration height with root `LocalWindowSize` (backed by `LocalWindowInfo`) | 9 s Gradle |
| Touched modules' layout tier | 700 passed, zero overruns/hangs | 19.96 s |
| `make quick` (once only) | First Gradle stage failed on the stale glyph count and two untouched timing cases; all new wizard cases passed | 37.19 s |
| `LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached` after correction | Passed (50 glyphs; filled star and clock hands also checked) | 1.39 s |
| Remaining quick build-logic stage | 37 certified passing results (up to date) | 1.61 s |
| Remaining quick script/tooling stage | Scripts 168 passed; contracts 24 passed, parity corrected environment then passed; l10n 49 passed; icons 11 passed | 19.959 / 0.020 + 0.100 / 0.018 / 0.080 s Python |
| Remaining quick `make timing` | 7 certified passing results (up to date), zero overruns | 1.62 s |

The full quick command was not repeated. Its untouched timing overruns were `RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier` at **1.290 s** (1 s budget) and `TermsScreenTest#policyRowStaysEnabledWhileFailed` at **3.544 s** (first-class 3 s budget). Neither test was retried or modified. The first stage collected 5,035 passing results and the one stale-glyph failure, with 3,220 newly measured cases; no hangs. Final quick passed all 23 `WizardAnswerSheetTest` cases (1.772-second sum) and both haptic cases (0.081-second sum), including the earlier corrections.

The manual tooling continuation initially omitted the `IOS_REPO`/`WEB_REPO` environment that Make exports, so only the parity test failed with `set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. The corrected command was:

```sh
IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new python3 -I -m unittest discover -s tools/contracts-gen -t tools/contracts-gen -p 'test_*.py' -k test_matches_ios_generated_swift
```

It passed; the script suite was not repeated. Layout and lint ran with:

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :app:lintDebug :feature:wizard:lintDebug :core:designsystem:lintDebug :core:ui:lintDebug
Scripts/run-tier.sh layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'core/ui/build/test-results/testDebugUnitTest/*.xml' --results 'core/designsystem/build/test-results/testDebugUnitTest/*.xml' --results 'app/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/ui/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/designsystem/build/cf-timings/testDebugUnitTest/*.json' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew :feature:wizard:testDebugUnitTest :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout
```

The final lint correction and glyph-test update are separate signed acceptance-fix commits; no task commit was rewritten. Source scans found no new screen-copy assertions, size callbacks, checking/sheet timers, locale formatters or accessibility announcement calls. The sole checking `Path()` is inside `drawWithCache`. There was no sandbox-blocked command. `make check`, perf, perf-timeline, baseline-profile and other emulator tiers were intentionally not run. The host board comparison and an interactive checking → fine-tune → sheet StrictMode observation remain for the orchestrator; the seeded device checks reported no disk/network violation. The `pixel` emulator remains running.

## Phase 9f review round 1

2026-11-20. All eight findings are addressed in separate signed commits, with two further test-fixture commits after the one permitted quick run exposed remaining activity overhead. No history was rewritten, no budgets were changed, and no test-boundary allowlist entry was added.

### Behaviour, geometry and mapping

- Toolbar `RequestClose` follows pinned iOS `MemberWizardSteps.swift` l.141–148: guest + Excluded closes; otherwise changes require discard confirmation. The checking failure card emits `CloseFromFooter` and follows l.69–75: Excluded, guest or unchanged closes; a changed member draft asks to discard. The guest dialog resolves the existing welcome-screen message. The guest-checking toolbar regression failed before the fix; all four requested guest/member cases pass afterward. Failure-card screen tests assert the distinct footer intent.
- Busy ranges now place their natural-height text inside a vertically centred 52 dp control. The geometry test measures the actual paragraph centre against the remove target, rather than accepting the old text node's padded bounds; it failed before the fix and passes within 1 dp afterward.
- The inline wake picker sits in a full-width centred container inside its card. Before the fix, pixel8's card/picker centres were 205.333 / 160.381 dp (44.952 dp apart), and foldInner's were 420.571 / 374.857 dp (45.714 dp apart). Both geometry cases now pass the ±1 dp requirement.
- OQ-A420–OQ-A438 retain their questions/defaults and now have four table cells. No new OQ or copy was added.
- Fine-tune uses one custom layout for the heading/designed cards and preferences. It measures both children against the current constraints and places preferences after the greater of the natural card height and viewport minimum. `WizardFineTuneViewport` and its plain mutable measurements were deleted. The unchanged-width resize test initially left preferences at 770 / 518 dp after an 80 dp height reduction, instead of 690 / 438 dp. Both cases now move by 80 ±1 dp. Existing card-fit, interaction and checking-recomposition tests remain green.
- `WizardUiState.from` builds Checking/Fine-tune once and passes both projections to chrome. Chrome no longer invokes either mapper. JVM tests verify supplied projections (including values different from the source snapshot) and matching screen/footer fields.
- The glyph test was formatted with `make format`, including its filled-star assertion.

### Test infrastructure measurements

All figures below are macOS-host JVM/Robolectric measurements. They are not device performance claims. The review supplied host failures of 1.112 s for RootTheme and 3.190 s for the Terms class-first case; the measurements here retain their own run context.

| Case | Initial targeted class run | Single parallel quick run, after first fixture changes | Final targeted verification |
|---|---:|---:|---:|
| `RootThemeTest#rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier` | 393 ms warm | 1,037 ms, over 1,000 | **63 ms warm** in the restored three-case class |
| `TermsScreenTest#policyRowStaysEnabledWhileFailed` | 1,989 ms first case | 3,330 ms, over the 3,000 class allowance | **1,734 ms first case** in the four-case class; restored mutation case 1,465 ms in its own fork |

RootTheme's initial steps were application/graph 0.343 ms, activity setup 116.477, set-content 220.719, frames 4.357, blocked/commit assertions 22.228, font/ready assertions 2.511 and teardown 11.502. Extracting the production `RootThemeContent` boundary and removing AppGraph/CfRoot reduced the comparable warm case to 283 ms, but the parallel quick run still spent 398.587 ms in activity setup and 476.178 in set-content. The final fixture therefore uses runtime composition with a manual `BroadcastFrameClock` and test scheduler, with no activity host. It still runs the production boundary and checks reduce motion, awaiting the supplied commit, four preloads on the injected dispatcher, and the actual `LaunchController.fontsReady` flag. Its final warm steps were fixture/resolver 15.784 ms, composition 29.092, first frame 2.399, commit/font assertions 0.518 and teardown 1.803. Composition/recomposer teardown completes under `NonCancellable`.

Both versions of the narrowed font fixture rejected the commit-barrier mutation (`if (firstFrame.isCompleted) firstFrame.await()`): expected zero preloads, observed four. The original unconditional await was restored each time; the final three-case RootTheme class passes unchanged activity-frame-commit and system-motion checks too. No mutation remains in production.

Terms' detailed baseline measured 313.878 ms in the body, 770.655 ms in its enclosing Compose rule, and 776.127 ms including profile setup; that isolated case totalled 2.029 s including Robolectric startup. Native font-runtime initialization is warmed once per JVM in `BeforeClass` and logged separately (626.336 ms in the first successful warm-up run). Attempting to warm an entire Compose activity there was rejected immediately by Robolectric (`no display 0`), so that approach was removed. In quick the native runtime was already warm (0.018 ms), while the Compose rule still took 2,981.431 ms, including 1,152.342 ms in the body. Warming native fonts alone therefore did not solve the parallel case.

The final Terms fixture directly hosts the unchanged screen in `ComponentActivity` under an empty Compose rule, removing ActivityScenario orchestration. It retains real fonts, profile/theme providers, the policy custom-tab intent assertion, failure nodes, disabled action checks and confirmation-slot assertion. In its isolated four-case run, font-runtime warm-up took 816.001 ms separately; the first case measured activity setup 418.725 ms, set-content 410.156, failure query 5.721 and scroll/enabled check 4.152. Subsequent cases were 85 / 42 / 36 ms. A production mutation disabling the policy row made the existing enabled assertion fail; restoration passed. Assertions and timing limits were not weakened.

The full quick command was not repeated after these last two fixture changes. Their final targeted results establish the serial case costs above; a second parallel quick/check run remains for the host gate.

### Review validation

| Scope / command | Result | Wall time |
|---|---|---:|
| Checking VM + failure-card tests | 17 passed; guest toolbar negative test failed before correction | 9.53 s |
| Busy paragraph/remove-centre guard | 1 passed after failing before correction | 5.24 s |
| Wake picker/card-centre guards | pixel8 + foldInner passed after both failed before correction | 4.65 s |
| Fine-tune layout/interaction + checking recomposition | 18 passed; both resize cases failed before correction | 6.43 s |
| Chrome mapper + checking/fine-tune/return/where JVM tests | 68 passed | 3.92 s |
| Formatted `LucideGlyphsTest` | Passed | 1 s Gradle |
| Final RootTheme class | 3 passed, 63 ms warm target case | 4.40 s |
| Final Terms direct-host class | 4 passed | 5 s Gradle |
| Restored Terms policy case | Passed | 3.95 s |
| Captures (once) | All 18 requested PNGs written, zero timing overruns | 17.98 s |
| Layout tier (once) | 463 passed across wizard/app/onboarding/design-system, zero overruns | 16.68 s |
| Touched modules' lint (once) | Passed | 10 s Gradle |
| `make quick` (once) | Assertions passed; the two touched timing overruns above failed its first-stage gate | 36 s Gradle |

Commands for the final capture, layout and lint runs:

```sh
make captures ONLY=WizardSheetBusy,WizardSheetWake,WizardFineTune
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
Scripts/run-tier.sh layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --results 'app/build/test-results/testDebugUnitTest/*.xml' --results 'feature/onboarding/build/test-results/testDebugUnitTest/*.xml' --results 'core/designsystem/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' --timings 'feature/onboarding/build/cf-timings/testDebugUnitTest/*.json' --timings 'core/designsystem/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew :feature:wizard:testDebugUnitTest :app:testDebugUnitTest :feature:onboarding:testDebugUnitTest :core:designsystem:testDebugUnitTest -Pcf.tier=layout
./gradlew :app:lintDebug :feature:wizard:lintDebug :feature:onboarding:lintDebug :core:designsystem:lintDebug
```

The 107-case checking/fine-tune matrix is unchanged; the new geometry and resize cases are separate focused tests. Busy/wake captures were inspected alongside the supplied board pair. No Chromium render, emulator tier, perf/profile command or `make check` was run. The last two fixes affect test fixtures only, so captures, layout and production lint were not repeated. `make format` preceded quick and formatted the later fixture changes as well.

Quick collected 5,042 passing assertion results (3,226 newly measured cases), zero assertion failures and the two timing overruns; its first-stage wall time was 36.82 s. After the final fixture changes, `TodayTestBoundaryTest` passed all 12 cases in 2.58 s. The skipped quick stages were completed separately: `spotlessCheck` plus build-logic passed (37 certified results, 9.70 s), tooling passed (168 script tests in 19.747 s, 25 contract tests in 0.110 s, 49 l10n tests in 0.017 s, 11 icon tests in 0.073 s), and the final serial timing stage passed with seven certified up-to-date results (0.88 s). No stage was retried for host timing noise. The separate tooling invocation exported the same source-pin environment as Make.

```sh
make targeted ONLY='app:RootThemeTest'
make targeted ONLY='feature:onboarding:TermsScreenTest'
make targeted ONLY='feature:onboarding:TermsScreenTest#policyRowStaysEnabledWhileFailed'
make targeted ONLY='konsist:TodayTestBoundaryTest'
Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew spotlessCheck :build-logic:convention:test
IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new Scripts/test_scripts.sh
make timing
```

No required command was blocked by the sandbox. An optional image-pair assembly helper lacked Pillow (`ModuleNotFoundError: No module named 'PIL'`); existing pair/capture images were inspected directly, without installing a dependency. No production mutation, temporary configuration or new dependency remains. The `pixel` emulator was left running.

## Phase 9f review round 2

2026-11-20. The host confirmed round 1's `make check` passed. This round restores graph-wiring coverage, records explicit fixture warm-ups, removes profiling stdout, and completes the edit-mode discard labels.

`CfRoot` now delegates all font/motion bindings to `RootThemeGraph.rootThemeInputs`. `AppGraph` implements that narrow source interface using its existing getters; the factory selects `dispatchers.io`, preserves the preloader/commit/motion flag and binds the actual launch controller's ready callback. `RootThemeInputsTest` runs three plain JVM cases against a fake source, without constructing AppGraph. Mutating the factory to select `dispatchers.main` failed the identity assertion; mutating motion to constant false failed the enabled-motion assertion. Both mutations were restored and the affected cases passed. The runtime-composition case is now named `themeContentProvidesReduceMotionAndUsesTheSuppliedFontBarrier`, distinguishing its rendering responsibility from the graph factory's tests.

Baseline root rendering was 88 ms warm; the first factory-backed run was 87 ms, and the final isolated run after removing profiling was 92 ms. In the parallel quick run it was 133 ms, below the 1 s limit. The three factory cases were 13 / 1 / 1 ms in quick.

`:core:testing-junit` had no explicit BeforeClass measurement API; its existing allowances apply to test windows. `measureClassWarmup` now writes separate class/SDK/name/ms evidence beside the listener's output, crossing Robolectric's sandbox class-loader boundary through files. `run-tier` exposes it in `timings.json` under `warmups`, separate from `tests`, `measuredTests` and `budgetChecks`. It does not spend, extend or replace an existing test allowance. The writer tests cover returned values, escaping and failed setup; collector tests prove that warm-up evidence cannot stand in for missing test metrics. See [test-diet.md](test-diet.md) for the API.

Terms' baseline native warm-up was 525 ms, printed only to stdout. Its structured isolated records were 740 ms initially and 529 ms after profiling removal. The four-case class still produces exactly one warm-up record. The final quick record was:

```json
{"class":"net.chronoflow.feature.onboarding.terms.TermsScreenTest","sdk":"36","name":"native-font-runtime","ms":0}
```

Zero means the runtime had already been initialized in that worker. Both RootTheme and Terms XML outputs contain no per-case stdout. Terms' first isolated case changed from 1.492 s before this round to 1.462 s after cleanup; its final quick class-first case was 1.835 s, below 3 s, with the remaining cases at 156 / 113 / 86 ms. These are macOS JVM/Robolectric figures, not device performance claims.

The public alternative was tested with `@GraphicsMode(NATIVE)` and `Typeface.create` alone:

```sh
make targeted ONLY='core:testing-junit:ClassWarmupTest feature:onboarding:TermsScreenTest'
```

The writer tests passed, but Terms failed immediately in BeforeClass:

```text
java.lang.NullPointerException: Cannot enter synchronized block because "android.graphics.Typeface.SYSTEM_FONT_MAP_LOCK" is null
```

Robolectric 4.18 defers Typeface's static initialization, so NATIVE alone cannot perform this pre-class warm-up. The internal loader remains, with that reason in one comment. Restoring it passed all four Terms cases; no unsupported public warm-up or mutation remains.

Discard title and confirm copy now come from UI state alongside the existing message, matching pinned `MemberWizardView.swift` l.125–136. Editing uses `Discard your changes?` / `Discard changes`; a new trip uses `Discard this trip?` / `Discard`. Guest welcome-screen and edit-preservation messages remain intact. Before implementation, all four generated IDs were verified in both `values/strings_ios.xml` and `values-nl/strings_ios.xml`. `WizardDiscardUiTest` checks editing/new member/new guest state on the JVM. No screen-copy assertion, new string, Android-only copy or OQ was added.

Validation:

| Scope | Result | Timing |
|---|---|---:|
| Root input factory and RootTheme class | All six passed; both binding mutations failed as required and restored cases passed | 8 s Gradle initial run |
| Warm-up writer/listener/classification | 15 passed | 2 s Gradle |
| New collector tests | 2 passed | 0.312 s |
| Clean RootTheme/factory and Terms classes | Passed; stdout empty in both Robolectric classes | Root warm 92 ms; Terms first 1.462 s |
| Discard UI + checking VM | 16 passed | 4 s Gradle |
| Touched app/wizard/onboarding lint | Passed | 12 s Gradle |
| Touched Android modules' layout tier | 455 passed; zero overruns | 16.98 s |
| `make format` | Passed before quick | 10 s Gradle |
| `make quick`, once | 5,050 measured cases passed; zero overruns/hangs; class warm-up recorded separately | 44.77 s first stage |
| Quick build-logic | 37 certified passing results | 1.82 s |
| Quick tooling | 170 scripts / 25 contracts / 49 l10n / 11 icons passed | 20.423 / 0.114 / 0.017 / 0.077 s |
| Quick serial timing | 7 measured budget cases passed | 2.83 s |

The changed test-runtime library caused quick to measure every selected case afresh. The stage times above include that validation and the separate tooling stage; the full quick command was not repeated. No required command was sandbox-blocked. Lint/layout ran once for touched Android modules; no emulator, capture, perf/profile tier or `make check` was run. The `pixel` emulator remains running.

## Phase 9f review round 3

2026-11-20. Test-only fixes; production files are unchanged.

`RootThemeTest#cfRootForwardsMotionAndFirstCommit` invokes the real `CfRoot` with a minimal actual AppGraph, distinct main/IO test dispatchers, a non-started launch controller and explicit routed destinations. Two fresh compositions in the one case check initial motion true and false. Each asserts zero preloads before completing its supplied commit, then four preloads on the graph's IO dispatcher and the launch fonts-ready flag. Graph and launch scopes are cancelled/joined during teardown. This avoids unrelated session/navigation initialization without bypassing the CfRoot call site. The first paused-clock draft used an in-place motion toggle; it was replaced with fresh compositions so the test checks both initial values directly.

Both requested mutations were applied to the actual call in `CfRoot.kt`, not the factory:

- `rootThemeInputs(false, firstCommit)` failed: `expected:<true> but was:<false>`.
- `rootThemeInputs(reduceMotion, CompletableDeferred(Unit))` failed: `expected:<0> but was:<4>`.

Both were restored byte-for-byte before validation. The restored RootTheme class passed all four cases; the new case took **119 ms warm** (4.29-second targeted command) and **312 ms under parallel quick load**, below the 1 s budget. Its isolated first-SDK run was 2.229 s and is a cold-start cost, not a warm-case claim. No budget or production seam was changed.

`ClassWarmupTest` now owns its directories through `TemporaryFolder` with deletion assurance, retaining the existing property-restoration finally block. All six old `core/testing-junit/build/class-warmup-proof-*` directories were removed. Both targeted cases passed (1.49-second command), and zero proof directories remained after targeted and quick runs.

`make format` passed (10 seconds). `make quick` ran once: 5,051 collected assertion results passed, 3,259 cases were newly measured, and no test hung. The first-stage timing gate failed only on untouched `TermsScreenTest#policyRowStaysEnabledWhileFailed`: **3,264 ms > 3,000 ms**. That case was not retried or modified. First-stage wall time was 39.58 seconds. The skipped quick stages passed separately: build-logic 37 certified results (1.38 s); tooling 170 scripts / 25 contracts / 49 l10n / 11 icons (20.158 / 0.112 / 0.017 / 0.074 s); seven certified up-to-date serial timing results (0.87 s). These are macOS-host JVM/Robolectric measurements, not device claims.

```sh
make targeted ONLY='app:RootThemeTest'
make targeted ONLY='app:RootThemeTest#cfRootForwardsMotionAndFirstCommit'
make targeted ONLY='core:testing-junit:ClassWarmupTest'
make format
make quick
```

No required command was sandbox-blocked. No second quick run, emulator tier, perf/profile tier or `make check` was run. The `pixel` emulator remains running.

## Phase 9g: ready and save

2026-11-20. [Brief](phases/phase-9g-wizard-ready-save.md). Ready now projects the landed model's summary, rows, server price, banners and footer through `WizardUiState.from`. Its events call the existing model. Calculating consumes back, hides both toolbar actions and has no footer. A saved exit selects My trips and opens `TripPage(id, awaitingSync = true)`; the overlay guard protects later navigation from a replayed exit. The entry-scoped ViewModel consumes the success haptic once.

### Ready geometry and values

| Surface | Values / source |
|---|---|
| Content gap | `wizardContentGap` 14 dp |
| Summary | Card fill, dawn 2 dp border, soft shadow, radius 26 dp; vertical padding 16, horizontal 18; gap 6 dp; `screenEyebrow`, `wizardReadySummary` 20/27, `wizardHint` 13/18 |
| Rows | Card fill, border 1 dp, radius 16 dp; minimum target 48 dp; padding start/end/top/bottom 16/10/5/5; gap 8; label width 92 dp; `screenBody` 14/20 for labels and values |
| Large text | Labels stack over values at capped font scale ≥ 1.2, with 2 dp gap; the content scrolls and the footer remains pinned |
| Price | Horizontal padding 4 dp, gap 2; first line `wizardSubtitle` 16/22 in foreground, price value semibold; other lines `wizardHint` 13/18 |
| No-credits footer | One full-width Close action, minimum 48 dp, `wizardSecondary`; shell footer padding 20/12, above the supplied safe bottom inset |
| Calculating | Card fill, border 1 dp, soft shadow, radius 26, padding vertical/horizontal 32/20, gap 16; dawn disc 64 dp at 0.16 alpha; native `CfSpinner` 30 dp or a static 24 dp clock under Reduce Motion |

The row route ends at `legs[returnBoundary - 1]`. Flight, Return, dates, Extras and Latest wake-up use the ported `WizardPresentation` functions. `ReadyText` records catalog calls symbolically, including nested arguments, until resources resolve them in the application language. Catalog equality remains stable while counting answered prompts. The via-flight key falls back from `CopyTable.Wizard` to `CopyTable.Android`; its en/nl overlay uses `{{flight}} · via {{airports}}` and replaces no iOS key (OQ-A356).

Sleep takes enabled trip overrides first, then `profile.habitual_bed` / `habitual_wake`, then the saved camel-case profile fields. Missing times do not add a dash. Extras failures produce an empty value. OQ-A365 retains the existing half-even, whole-number rating display. Ready exposes an answer count and wake time rather than a rating. OQ-A433 continues to require numeric rating seeds.

| Price state | Classification / first line | Following lines / footer |
|---|---|---|
| Paid one way | Server cost 1: Price **1 credit** | Server balance (including singular 1); primary |
| Paid return | Seed cost 2: Price **2 credits** | Server balance; primary |
| Unlimited | **Included in your subscription.**; no Price prefix | No balance; primary |
| Free | Price **Free** | Free-trip explanation; primary |
| One direction free | Price **1 credit** | Free-home explanation, then server balance; primary |
| Unavailable | Server price still shown when present | Destructive, semibold balance when cost > 0 and not unlimited; Close only |
| Null price | No price block; unavailable | Close only |
| Null review | No summary or price block | Primary projection, enabled only if the model facts permit save |

Classification uses `WizardCredits.of`: missing price, then blocked save, then unlimited, then zero cost, then return in `freeDirections`, then paid. No balance or cost comes from the screen.

| Banner priority | Tone / content | Tag |
|---|---|---|
| Save failure | Error; offline / server / throttle catalog copy; positive throttle seconds use `LockoutWait` | `wizard.save.offline`, `.server`, `.throttle` |
| Adjusted | Info; server reasons joined with one space | `wizard.ready.notice` |
| Updated | Info; non-empty server summary and resolved Price/value joined with one space | `wizard.ready.notice` |

The edit source-change branch belongs to 9h. Banner joining is pure UI-state code and has JVM assertions; screens have no copy assertions.

| Footer condition | Label | Enabled / appearance |
|---|---|---|
| Unavailable | Close only | Existing footer-close/discard policy; no primary |
| Reviewing | Checking… | Disabled, native spinner, alpha 0.7 |
| Throttle, positive seconds | Try again in N s | Disabled, alpha 0.4 |
| Throttle, zero seconds | Try again | Model `canSave` and shell guards |
| Otherwise | Make my plan | Model `canSave` and shell guards |

Ready sends its initial tick and then one each second while composed. The foreground observer skips its first STARTED event; later starts force a check only for unavailable credits. A UI override suppresses both events in the ViewModel. The countdown never submits a save. The production rows' Compose tracer count stays constant for 42 → 41 → 40. Row taps use Selection, except Latest wake-up; submit uses Medium; a changed non-null save failure emits Error. Saved uses Success once across root recreation.

### Seeds and captures

All completion boards use the existing debug override and frozen clock. `WizardSaveJourney` is accepted outside the capture name list and overrides only the starting draft: HND, now + 30 days, an arrival 18 local hours later, Return choice. It retains the session store, airport search, generation service and clock. The release test proves the journey seed class and board helpers are absent and the production override is null.

`make captures ONLY=WizardReady,WizardSave,WizardCalculating` ran once and wrote these 24 PNGs under `build/captures/`: 23 designer boards and the supplemental throttle state. The host rendered and reviewed all 23 boards. The results below record that review: no other layout, spacing, colour or type differences were found beyond the accepted differences listed here and the spinner capture phase refined in review rounds 1–2. There are no stored snapshot references.

For every Ready and Save board below, the footer sits lower in the capture because the capture supplies zero safe insets. Their ‹/× toolbar chip difference predates 9g and belongs to the landed 9c chrome. Calculating has neither footer nor toolbar actions.

| Capture names (each `.png`) | Seed / expected source differences | Board comparison |
|---|---|---|
| WizardReady, WizardReadyDark, WizardReadyNL | Create, cost 1, balance 3 | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips described here |
| WizardReadyReturn, WizardReadyReturnDarkNL | DXB stop and confirmed return; pinned seed cost **2**, board sample **1** | Host reviewed: uses the pinned one-way summary instead of the board's return summary; end date **Sun 18 Oct / zo 18 okt**, versus **Mon 26 Oct / ma 26 okt**; zero-inset footer and 9c chips |
| WizardReadyUnlimited, WizardReadyUnlimitedDarkNL | Unlimited, no balance | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips |
| WizardReadyFree, WizardReadyFreeDarkNL | Free-trip line | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips |
| WizardReadyOneFree, WizardReadyOneFreeDarkNL | Return direction free, balance 3 | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips |
| WizardReadyAdjusted, WizardReadyAdjustedDarkNL | Modified review, Latest wake-up 08:00; body is the server reason, rather than the board's illustrative sentence | Host reviewed: server reason and seed data follow iOS; zero-inset footer and 9c chips |
| WizardReadyChecking, WizardReadyCheckingDarkNL | Busy primary at 0.7 alpha | Host layout review passed with the documented seed, footer and chip differences; round 2 captures use the closest native upper-right gap: start **348.5°**, sweep **307.3°**, versus board **0°**, **288°** (clockwise from 3 o'clock) |
| WizardReadyNoCredits, WizardReadyNoCreditsDarkNL | Close only under A14; website action intentionally absent | Host reviewed: Close only follows A14; pinned seed data, zero-inset footer and 9c chips as above |
| WizardSaveOffline, WizardSaveOfflineDarkNL | Offline failure | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips |
| WizardSaveServer, WizardSaveServerDarkNL | Server failure | Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips |
| WizardCalculating, WizardCalculatingDarkNL | Native spinner; computed **10** days from 9–18 Oct inclusive, versus board sample **6** | Host layout review passed with the seed's day count; round 2 captures use the closest native upper-right gap: start **348.5°**, sweep **307.3°**, versus board **0°**, **288°** (clockwise from 3 o'clock) |
| WizardSaveThrottle_noBoard | 42-second lockout with the server-failure banner look | No designer board |

All Ready date lines use `planDateSummary` on the pinned seed summary (9–18 Oct), rather than the board's sample end date. The pinned CopyLine contains `shift` of kind minutes with value 420, so PlanCore formats **420 min**, as pinned iOS `CopyLineFormatter.formatMinutes(duration: false)` does, rather than the board's illustrative 7-hour wording. Extras counts all answered non-blocking prompts from the pinned seed and saved-profile defaults: 7 in the base capture and 11 with the multi-flight return; these differ from the boards' illustrative answer counts. These are source-data differences, not substitute visual designs.

Specifically, `WizardReadyReturn` and `WizardReadyReturnDarkNL` retain the same one-way summary line as the other seeds: the Dutch capture says “420 min vroeger, vanaf 3 dagen voor je vertrek” rather than the board's return line. Pinned iOS `App/Debug/WizardCompletionBoardSeed.swift:111–122` supplies that line and `planEndDate = 2026-10-18` even for the return seed. Its dates therefore end on **zo 18 okt**, rather than the board's **ma 26 okt**. The host accepted these pinned-source differences.

### Recovery and release evidence

`WizardProcessDeathTest` builds account SQLite, file-backed mock server state, `MemberWizardStore`, `TripGenerationService` and the ViewModel factory directly under a plain Android Application. It never builds the app/session graph. Each recreation closes the old driver and cancels and joins ViewModel, service and writer scopes under `NonCancellable` before opening fresh instances over the same files. Its injected dispatchers use the test scheduler; it never derives an IO dispatcher from paused Android Main.

| Process-death method | Result |
|---|---|
| calculatingRowResumesTheSaveWithTheSameIdentity | Fresh graph B first renders Calculating; its single POST has the same full identity; one trip results, exit carries its id, recovery row disappears |
| lostResponseResumeReturnsTheSameTrip | Kill after the remote receipt is durable and before response delivery; B's same-identity POST returns A's id; total trip count rises by one |
| savedTripIdRowSkipsTheCreate | Kill during pull after durable `savedTripId`; B sends zero save POSTs and exits with that id |
| readyRowRechecksOnceAfterRecovery | B reviews once, renders Ready and does not save |
| firstTripSaveLeavesTheRemindersOfferPending | Saved exit renders and `wizardReminders` remains JSON `"pending"` |
| existingTripSaveDoesNotQueueTheOffer | Saved exit renders and the offer section remains absent |
| expiredRowDoesNotResume | At 1,800 seconds, the old instance expires; B starts fresh at Where without review or save |

The seven recovery cases passed. The six-case completion run took 7.36 s, with a 2.481 s cold first case and 0.109–0.242 s warm cases. The same-identity case passed separately in 5 s Gradle wall time. All 33 unchanged cases of `WizardSaveRecoveryTests`, `WizardCompletionTests` and `WizardFixtureScenarioTests` passed in 3.95 s wrapper wall time.

The shell guard has a mutation proof: removing its `overlay is Route.Wizard` guard reset a later `TripPage("t2")` back to t1 and failed `savedWizardSelectsTripsAndOpensItsPageOnlyOnce`. Restoring the guard passed. The success-haptic test recreates `WizardRoot` and receives the replayed Saved exit with exactly one Success.

### Brief refresh and decisions

The branch preflight printed `feat/phase-9g-wizard-ready-save`. The cited upstream symbols were present; 63 primary identifiers were checked together, with signatures and call sites read separately. No further upstream-name mismatch required a pre-implementation refresh commit. Later task-specific clarifications were committed with their task (and the numeric-seed correction):

- Refresh note 17: the pinned rating string versus 9f's numeric representation; OQ-A433 requires numeric seeds throughout. A separate signed fix corrected the new seed and its assertion after the plan cross-check. Rendered values are unchanged.
- Refresh note 18: the process-death test belongs in `app/src/testDebug`, because the mock is a debug dependency.
- Refresh note 19: the lost-response kill is after durable receipt creation and before response delivery. Delivered Offline already belongs to 9b's manual-retry coverage and would persist Ready.

OQ-9g-1…11 are recorded as OQ-A439…OQ-A449 in order. OQ-A365 is resolved for this UI as described above. The via-flight overlay is in the Android-only copy approval table. All Phase 9g task boxes are ticked.

### Validation record

Commands and their complete logs/timing evidence are retained under `build/phase-9g-acceptance/`. Times below are wrapper wall time where a timing JSON was retained; otherwise they are explicitly labelled Gradle wall time. They are host/emulator measurements, not physical-device performance claims.

| Command / selectors passed to `make targeted ONLY=…` | Result / wall time |
|---|---|
| `core:wizard:WizardCopyKeysTest#everyListedKeyResolves` | Passed; 4 s Gradle |
| `make generators-check` | Passed; generator test output includes 49 l10n tests in 0.019 s; total wrapper wall time was not recorded |
| `feature:wizard:ReadyPriceUiTest feature:wizard:ReadyCreditsSaveFailureTest feature:wizard:ReadyBannerUiTest feature:wizard:ReadyFooterUiTest feature:wizard:ReadyRowsUiTest feature:wizard:TripWizardViewModelReadyTest feature:wizard:WizardChromeUiTest feature:wizard:TripWizardViewModelFineTuneTest feature:wizard:TripWizardViewModelCheckingTest` | 65 passed; 7.62 s; slowest 85 ms |
| `app:ShellViewModelOverlayTest` (after exit-type wiring) | 3 passed; 6.34 s |
| `feature:wizard:ReadyStepTest feature:wizard:ReadyForegroundTest feature:wizard:ReadyTickTest` | Seven initially passed; ticker-removal fixture failed. Final targeted method passed after settling removal before measuring later ticks (5.23 s; cold case 1.999 s) |
| `feature:wizard:CalculatingUiTest feature:wizard:CalculatingStepTest feature:wizard:WizardBackHandlingTest` | Screen/back cases passed; the day-count test initially supplied a null-summary fixture. Corrected targeted method passed (3.84 s); summary-null remains a separate zero-day case |
| `feature:wizard:WizardCompletionBoardSeedTest feature:wizard:WizardBoardCaptureSeedTest feature:wizard:WizardSaveJourneySeedTest app:WizardDebugSeedsTest feature:wizard:ReadyStepTest feature:wizard:CalculatingStepTest` | Passed; feature group 17 s Gradle, app group 5.60 s wrapper |
| `feature:wizard:WizardCompletionBoardSeedTest app:ReleaseReadinessTest` | Passed; feature group 3 s Gradle; release 13 cases, 9.34 s wrapper |
| `app:ShellViewModelOverlayTest feature:wizard:WizardSavedExitTest feature:wizard:TripWizardViewModelReadyTest#savedExitKeepsTheTripId` | Passed; app 3 s Gradle; feature 2 cases, 6.92 s wrapper; cold Robolectric 2.377 s |
| `app:ShellViewModelOverlayTest#savedWizardSelectsTripsAndOpensItsPageOnlyOnce` mutation / restoration | Expected mutation failure, 4 s Gradle; restored pass, 1 s Gradle |
| `app:WizardProcessDeathTest#calculatingRowResumesTheSaveWithTheSameIdentity` | Initial fixture used the manifest Application and hit launch StrictMode under virtual IO. Selecting a plain Application removed that implicit app startup; pass, 5 s Gradle |
| Other six `WizardProcessDeathTest` methods, selected individually in one call | Passed; 7.36 s; methods listed in recovery table |
| `core:data:WizardSaveRecoveryTests core:data:WizardCompletionTests core:data:WizardFixtureScenarioTests` | 33 passed unchanged; 3.95 s |
| `feature:wizard:WizardReadyGuardsTest` | 188 passed; 20.49 s; cold first 2.225 s; warm max 97 ms |
| `make captures ONLY=WizardReady,WizardSave,WizardCalculating` | 24 passed captures, 482 prefix-filtered skips; 24 PNGs; 22.97 s |
| `feature:wizard:WizardCompletionBoardSeedTest#draftMatchesTheCompletionBoard` (numeric seed correction) | Passed; 4 s Gradle |
| `feature:wizard:ReadyBannerUiTest` (pure joining coverage) | 3 passed; 4 s Gradle |
| `app:WizardJourneyTest#oneWayReviewSavesAndOpensTheTripPage app:WizardCalculatingBackTest` | Both passed on `pixel`, API 36; 17.03 s wrapper; journey **4.350 s**, seeded back check **3.522 s** |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug :app:verifyReleaseManifest :app:analyzeReleaseR8Config` | Passed; 46 s Gradle |
| `Scripts/run-tier.sh phase-9g-layout … -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout` | 618 passed; 20.68 s |
| `Scripts/run-tier.sh phase-9g-ui-layout … -- ./gradlew --console=plain :core:ui:testDebugUnitTest -Pcf.tier=layout` | 267 passed; 18.78 s |
| `make quick` (once) | Failed first stage: wizard worker exited 137 before timing output. 4,417 collected assertions passed, with no case failure or hang; 39.36 s |
| `Scripts/run-tier.sh phase-9g-wizard-quick … -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest -Pcf.tier=quick --max-workers=1` | Missing wizard tier passed separately: 731 cases, 12.83 s; cold first 2.338 s; no overrun/hang |
| `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test --max-workers=1` | 37 passed; 2.70 s |
| `Scripts/test_scripts.sh` and the environment correction below | 170 scripts passed in 20.077 s; 24/25 contracts initially passed; final parity case passed in 0.101 s; remaining 49 l10n in 0.017 s and 11 icons in 0.074 s |
| `make timing` | 7 certified up-to-date results; 2.83 s; no overrun/hang |

The original quick failure is preserved; `make quick` was not rerun. Its exact failure was:

```text
Execution failed for task ':feature:wizard:testDebugUnitTest' (registered by plugin 'com.android.internal.library').
> Test process encountered an unexpected problem.
   > Process 'Gradle Test Executor 51' finished with non-zero exit value 137 (this value may indicate that the process was terminated with the SIGKILL signal, which is often caused by the system running out of memory)
BUILD FAILED in 38s
run-tier: feature/wizard/build/cf-timings/testDebugUnitTest wrote no timing files
run-tier: feature/wizard/build/cf-timings/testDebugUnitTest has no matching passed stamp; rerun to measure it
make: *** [quick] Error 1
```

The standalone tooling-stage invocation initially lacked the environment normally exported by Make. Its sole failed case printed `AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. After exporting `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios` and `WEB_REPO=/Users/rickgout/Sites/chronoflow-new`, only that failed method and the two skipped groups ran:

```sh
python3 -I -m unittest discover -s tools/contracts-gen -t tools/contracts-gen -p 'test_generate_kotlin.py' -k test_matches_ios_generated_swift
python3 -I -m unittest discover -s tools/l10n -t tools/l10n -p 'test_*.py'
python3 -I -m unittest discover -s tools/icons -t tools/icons -p 'test_*.py'
```

All three passed. The original quick worker's exit 137 remains recorded as a failed result; the host subsequently ran the complete `make check` successfully in **3:18** before review round 1. The phase did not change test budgets or build-wide worker settings.

The emulator journey asserted the debug main-thread IO counter was zero; its per-test logcat also contains no StrictMode/disk/network/launch-path violation. Both native tests ran once. No required command was blocked by adb or the sandbox. `pixel` remains running.

The brief's `make check` acceptance command and all perf/profile tiers are excluded from the sandbox run instructions. The host's complete `make check` and board comparison are recorded above. Wizard profiles belong to Phase 13. This phase adds no runtime dependency or manifest entry; release manifest and R8 configuration analysis passed.

The optional signature-status read `git log cf403752..HEAD --format='%h %G? %s'` produced no output and was interrupted with Ctrl-C (exit 130). Every `git commit -S` succeeded, and direct `git cat-file commit` inspection confirmed a PGP signature header on every phase commit. Cryptographic status verification was not completed by that optional log command.

### Review round 1

2026-11-20. Host acceptance before this round: `make check` passed in **3:18**, and all 23 designer boards were rendered and compared as recorded above.

The Calculating back lock now registers immediately after `NavDisplay` inside `WizardStepHost`. Navigation 3 1.2.1 exposes no enable flag on this `NavDisplay` overload. Registering the consuming handler in the same subcomposition, after the display, gives it precedence over the display's handler and prevents the previous scene from owning predictive back.

`CalculatingStepTest.calculatingOwnsTheNavigationBackHandler` checks the dispatcher's selected navigation history: its current entry is not a Nav3 `SceneInfo`, and it contains one entry, with no previous scene to preview. Restoring both production files to their original handler order made this exact test fail at the `SceneInfo` assertion (**4 s Gradle**, expected failure). Restoring the fix passed. The existing consumed-back and no-toolbar/footer checks remain unchanged.

Calculating and ReadyChecking captures keep motion enabled and pause the test clock. Round 1 established the animation's first frame, then advanced **2,532 ms** to the native Material 3 indicator's three-quarter sweep before capture. `make captures ONLY=WizardCalculating,WizardReadyChecking` ran once and refreshed `WizardCalculating.png`, `WizardCalculatingDarkNL.png`, `WizardReadyChecking.png` and `WizardReadyCheckingDarkNL.png`; the other reviewed PNGs were preserved. The light captures showed a three-quarter sweep, but round 2 review identified its upper-left gap, roughly 90° from the boards' upper-right gap. The refined phase and its remaining native arc difference are recorded below and in the board table.

Round logs, the expected-failure XML and timing JSON are retained in `build/phase-9g-review-round-1/`. No brief patch, new OQ, runtime dependency or manifest change was needed.

| Command | Result / wall time |
|---|---|
| `make targeted ONLY='feature:wizard:CalculatingStepTest feature:wizard:WizardBackHandlingTest'` | 10 passed; **5.46 s**; regression 33 ms warm, cold first case 2.048 s; no timing overrun |
| `make captures ONLY=WizardCalculating,WizardReadyChecking` | Four passed captures, 502 prefix-filtered skips; **18.57 s**; four PNGs refreshed |
| `make format` | Passed; **12 s Gradle** final formatting run |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug --console=plain` | Passed; **17.16 s** |
| `Scripts/run-tier.sh phase-9g-review-layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest -Pcf.tier=layout` | 548 passed; **18.56 s**; slowest warm case 91 ms; no timing overrun |
| `make quick` (once this round) | Passed; **61.29 s** total: 5,148 passing JVM/Robolectric results (3,330 measured this run), 37 build-logic results, tooling tests and seven certified timing cases; no overrun or hang; slowest warm case 630 ms |

### Review round 2

The host's complete `make check` passed after round 1. This round computes the capture phase from Material 3 1.4.0's `ProgressIndicator.kt` and `MotionTokens.kt`, rather than selecting it through repeated captures. Both boards use the `loader-circle` SVG `M21 12a9 9 0 1 1-6.219-8.56`: its centreline starts at **0°** (3 o'clock) and sweeps approximately **288°** clockwise, leaving an upper-right gap.

The native animation repeats every 6,000 ms. Global rotation is **0.18° × t**; additional rotation rises by 90° over the first 300 ms of each 1,500 ms interval, with cubic Bézier easing `(0.05, 0.7, 0.1, 1)`, and holds between rises. Sweep grows linearly from **36°** to **313.2°** in the first 3,000 ms, then shrinks with cubic Bézier easing `(0.2, 0, 0, 1)`. The exact 288° sweep occurs near **2,727 ms** and **3,241 ms**, with start angles **310.9°** and **132.9°**; neither matches the board's 0° start.

Minimizing the sum of squared circular errors at the two arc endpoints over one full cycle gives **2,935.6 ms**, rounded to **2,936 ms**. In that held-rotation interval, start is `0.18t + 180` modulo 360 and sweep is `36 + 0.0924t`. The captured start is **348.48°** and the sweep **307.2864°**, ending at **295.7664°**. The gap is now upper-right; the remaining start and end errors are approximately **−11.52°** and **+7.77°**, so the native arc is **19.29° longer** than the board. This difference applies to **WizardCalculating**, **WizardCalculatingDarkNL**, **WizardReadyChecking** and **WizardReadyCheckingDarkNL**, as their table rows record.

The save-lock why-comment now identifies `BoxWithConstraints` as the component that subcomposes the whole `WizardStepHost`. The handler shares that subcomposition and registers after `NavDisplay` to win back dispatch; its behaviour is unchanged.

`make captures ONLY=WizardCalculating,WizardReadyChecking` ran once this round. All four new PNGs were produced, their light variants were inspected for the upper-right gap, and the other reviewed captures were preserved. Logs, timing JSON and the phase calculation are retained under `build/phase-9g-review-round-2/`. No brief patch or new OQ was needed.

| Command | Result / wall time |
|---|---|
| `make targeted ONLY='feature:wizard:CalculatingStepTest feature:wizard:ReadyStepTest feature:wizard:WizardBackHandlingTest'` | 15 passed; **7.77 s**; cold first case 2.044 s; slowest warm case 54 ms; no timing overrun |
| `make captures ONLY=WizardCalculating,WizardReadyChecking` | Four passed captures, 502 prefix-filtered skips; **16.77 s**; four PNGs refreshed |
| `make format` | Passed; **14 s Gradle** |
| `./gradlew :app:lintDebug :feature:wizard:lintDebug --console=plain` | Passed; **23.23 s** |
| `Scripts/run-tier.sh phase-9g-review-2-layout --results 'feature/wizard/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/wizard/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:wizard:testDebugUnitTest -Pcf.tier=layout` | 548 passed; **18.16 s**; slowest warm case 92 ms; no timing overrun |
| `make quick` (once this round) | Passed; **62.01 s** total; 5,148 passing JVM/Robolectric results (3,330 measured this run), 37 build-logic results, all 255 tooling tests and seven certified timing cases; no overrun or hang |

## Phase 9h: edit and duplicate

2026-11-20. [Brief](phases/phase-9h-wizard-edit-duplicate.md). Trip actions prepare the stored trip input before opening an entry-scoped wizard. Edit starts at Checking and updates the same trip; Duplicate clears date-bound answers, starts at Times and uses the existing create save path. No endpoint or generation rule is duplicated in the feature layer.

### Preparation, recovery and navigation

`:feature:trip` depends on the `TripWizardPreparer` interface, without a wizard feature/core dependency. `TripPageViewModel.startAction` rechecks the card flags and connectivity, prevents a second request, clears the previous failure and publishes busy state. The rows and Cancel are disabled while preparing. The native sheet rejects drag, scrim and back dismissal while busy. A successful preparation stores `WizardMode.Edit` or `Duplicate` in the ViewModel’s `pendingWizardRequest: StateFlow<WizardMode?>`. The window receives that current value and its `LaunchedEffect(pending)` calls `acknowledgeWizardRequest()` immediately before clearing the sheet route and opening the wizard, with no intervening suspension. The acknowledgement atomically clears the pending value and returns false if another window already acknowledged it. Disposing the window before acknowledgement leaves the request available for recreation. A second action cannot overwrite an unacknowledged request. The page observes only whether actions exist; busy changes stay inside the actions sheet.

`WizardLauncher` is retained by the shell ViewModel and shared with the trip ViewModel across recreation. Its pending guard resets on actual overlay changes, rather than on a new composition. `WizardLauncher.prepare` runs `startTrip` on the injected IO dispatcher. An open/pending overlay or missing session is ignored. Offline maps to the connection copy for both actions; other failures use the edit server copy or duplicate-specific copy. Cancellation of the caller propagates; foreign cancellation maps to failure. The declared session, data, device identity, store, recovery, I/O, security and runtime exceptions are covered. No generation service is started by the preparer.

Failures use a `CfSnackbarHost` inside the native sheet window, with `trip.actions.failure` and the debug-only presentation counter. Each failed attempt creates a distinct consumable event retained by the trip ViewModel. Its atomic claim survives recreation, preventing replay while allowing an identical later failure to present again. Reopening the sheet or starting another action clears the failure. The release counter implementation has no `failurePresented` field.

The wizard recovers a prepared Edit only for the requested trip id. Duplicate accepts only a copied draft without an edit context. Otherwise it falls back to `startTrip`; failure closes quietly through the existing `WizardExit.Trips`. R2's generic warning log is retained, without an exception message. Create entry is unchanged.

Every wizard mode owns overlay back handling. The model's first-screen flag reaches `WizardUiState`; its close handler registers inside the step host after `NavDisplay`, because `BoxWithConstraints` subcomposition otherwise registers the navigation handler last. The original placement produced `Back` in all three new first-screen tests; the corrected placement produces `RequestClose` for edit Ready, edit Checking failure and Duplicate's first Times. Sheets and the discard dialog take precedence. Calculating retains 9g's back lock.

An untouched edit closes immediately. Changed edits ask for the native discard alert; confirmation discards recovery once, and dismissal preserves the form. Excluded closes directly; the no-credits footer applies the normal edit discard rule. Save emits `Saved(id)`, selects My trips and replaces its stack with `[Trips, TripPage(id, awaitingSync = true)]`. An edit keeps its id; a duplicate uses the new id from the existing create service.

### Ready mapping and web ends

| Variant | Credits / price | Ordered extra lines | Footer |
|---|---|---|---|
| WizardEdit | free / Free | EditNote | Update, disabled |
| WizardEditChanged | credits / 1 credit | Balance(3), EditNote | Update, enabled |
| WizardEditNoReturn | free / Free | NoRefund, EditNote | Update, enabled |
| WizardEditReturn | credits / 1 credit | Balance(3), EditReturnNote | Update, enabled |
| WizardEditReturnNoCredits | unavailable / 1 credit | Balance(0, alert), EditReturnNote | Close only |
| WizardEditWeb | free / Free | EditNote | Update, enabled |

A free edit shows Free before testing unlimited entitlement. Unlimited omits the Price prefix. Edits omit the free-trip line; the return-free line retains its existing rule. NoRefund requires a return in the baseline and none in the current form. EditReturnNote requires an added return; otherwise the edit note applies. Balances and costs remain server values.

Ready title and primary copy are Edit your trip and Update my plan. Reviewing and throttle labels keep 9g's priority. Banner order is save failure, source change, modified-plan reasons, then the ordinary Updated banner. `SourceChanged` records nested catalog calls as `ReadyText`; a missing source price or presentation failure falls through. Edit offline/server save failures use the bodies that promise the existing trip stays as it was. No billing link or website action is added.

Coming back? shows a selected first `wizard.choice.web` card only for a loaded nights/check_out end. Nights uses the catalog count key; checkout uses `returnSummary` in the wizard language. Its event keeps the web end and uses the model's 250 ms choice transition. The add-return credit hint appears only for edits whose baseline lacks return legs. Duplicate's outbound subtitle keeps the copied route on every visit; home Times and edits use the ordinary local-time subtitle.

OQ-A461 settles OQ-A351: initial edit Checking, failures and first Ready keep the 1.0 floor. `go()` already clears input-step floors as pinned iOS does; removing the unconditional editing override makes that rule effective. A revisited Coming back? is 0.45, later Checking uses the landed Android weight 0.60, and Ready uses 0.95. The initial floor remains the recorded Android difference from iOS's 0.95. The prior Android-only all-edit-step test was replaced with initial-floor cases and a revisit regression; the other 9a/9b/9g assertions remain unchanged.

### Seeds, guards and capture hand-off

`TripWizardActionsBoardSeed` is debug-only and runs after completion seeding. It supplies 27 names, with source revision `capture-source`, the pinned baselines, costs and return dates. ReturnWeb adds the edit context required by the board and has no progress floor. Checking edit variants use the existing frozen checking override; ready/excluded variants suppress review; Duplicate and ReturnWeb use normal input-step overrides. The common `boardState` helper now forwards `hasChanges` and `firstScreen` from the model, so an untouched edit capture has a disabled Update button.

`WizardEditGuardsTest` has 228 separately named cases: 208 main profile/language/theme combinations, 16 save-failure cases and four Dutch landscape cases. The two Dutch small/landscape footer stress states use the large-font input, capped at the root. The ported `testSEEditFooterAtXXLarge` / `testDuoEditFooterAtXXLarge` checks keep the footer above safe insets, and the no-credits Close inside the footer. The small profile supplies a non-zero bottom inset. The native discard dialog renders on Robolectric and is checked there. No stored snapshot references were recorded.

The initial guards run completed in 8.120 s (228 passed); its first native-render case was 2.026 s, within the cold SDK allowance. The final module layout run repeated all 228 in 4.692 s, with a slowest warm case of 0.052 s. The warm-up is measured separately through `measureClassWarmup`. Parameterized Robolectric BeforeClass runs outside the Android sandbox, so that warm-up prepares pure copy/model data rather than calling framework Typeface APIs.

The one `make captures` invocation produced 32 PNGs: the following 27 wizard captures, two new trip-actions captures, and the three existing trip-actions captures. `ONLY` takes literal prefixes, not wildcard expressions. Checking spinners use a paused clock, one frame and a deterministic 2,936 ms advance, as the existing native-spinner captures do. The host renders the HTML boards for comparison; that review remains a hand-off, not an approval claimed by this worker.

| Capture names (each `.png`) | Recorded board differences / review context |
|---|---|
| WizardEdit, WizardEditDarkNL | Disabled Update; pinned seed clears the profile, so Sleep is empty instead of the board's sample times. Runtime uses the stored body-clock fallback. |
| WizardEditChanged, WizardEditChangedDarkNL | Pinned baseline already has a return, so EditNote replaces the board's EditReturnNote (OQ-A455). Source-change sentence and return date use catalog copy. The summary uses the iOS seed `summary.line.key` ending “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” |
| WizardEditNoReturn, WizardEditNoReturnDarkNL | NoRefund then EditNote; source baseline has a return while the current form is one way. |
| WizardEditReturn | Added return and EditReturnNote; no DarkNL wrapper exists. Its iOS seed `summary.line.key` ends “Your trip home is planned too.”; the board ends “and back again after your flight home.” |
| WizardEditReturnNoCredits, WizardEditReturnNoCreditsDarkNL | Close only; the board's website purchase action is omitted under A14. Its iOS seed `summary.line.key` ends “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” |
| WizardEditWeb, WizardEditWebDarkNL | Nine-night end and stay-end summary; the pinned seat change makes this edit changed. |
| WizardReturnWeb, WizardReturnWebDarkNL | Edit context follows the board, unlike the pinned iOS seed; selected web card with the generated dawnText check at `space5` (other cards retain muted `space4` chevrons), and 45% progress. Review round 1 re-captures both boards after correcting the chevron and compares the host renders. Native text metrics wrap the web hint differently; the Dutch catalog also omits the board’s comma after “zo”. The earlier toolbar-chip difference remains. Back follows the model to Times, rather than the board link to Ready (OQ-A458). |
| WizardCheckingEdit, WizardCheckingEditDarkNL | 100% initial floor, edit title, deterministic native spinner frame. |
| WizardCheckOfflineEdit, WizardCheckOfflineEditDarkNL | Edit failure body, no back chevron. |
| WizardCheckServerEdit, WizardCheckServerEditDarkNL | Edit failure body, no back chevron. |
| WizardExcludedEdit, WizardExcludedEditDarkNL | Server shift-work reason without the board's doctor line; replaces the pinned seed's under-18 reason (OQ-A456). |
| WizardDiscardEdit, WizardDiscardEditDarkNL | Native M3 dialog; Dutch confirmation is Wijzigingen verwijderen from the catalog. Native button arrangement replaces the board's custom vertical arrangement. The underlying summary uses the iOS seed `summary.line.key` ending “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” |
| WizardDuplicate, WizardDuplicateDarkNL | Empty dates, copied-route subtitle, disabled Continue and no back chevron. |
| WizardEditSaveOffline_noBoard, WizardEditSaveServer_noBoard | Edit save-failure bodies in 9g's existing banner layout; no designer boards. |
| TripActionsBusy_noBoard_pixel8_en_light | Native sheet; rows/Cancel disabled, no spinner; no designer board. |
| TripActionsFailure_noBoard_pixel8_nl_light | Native sheet with the Dutch duplicate-failure snackbar inside its window; no designer board. |

All Ready edit capture variants retain the pinned completion seed's **420 min** summary argument and **7 answered** Extras value, rather than the boards' illustrative shift/answer count. All those edit variants also retain the empty Sleep seed value described above. The named Ready variants use their ported summary end dates: 14 Oct without return, 26 Oct with return, 22 Oct for the web end. Existing 9c toolbar-chip differences and the zero-safe-inset footer placement still apply. Long source copy scrolls above the pinned footer.

### Test wiring and acceptance

`TripActionsWizardEntryTest` builds the file-backed mock, account database, member wizard store and service directly, with test IO dispatchers and clock. Its four screen cases render the production shell scaffold, trip page, sheets and wizard overlay using those dependencies, without constructing AppGraph/SessionGraph or the app root. Both ported Swift entry names are retained. Failure presentation is checked through the debug counter, with copy mappings checked separately on the JVM.

`editSaveReplacesTheStoredDetail` stays at the ViewModel/store layer: after changing preparation days, it proves the mock source revision, stored plan **inputHash**, and stored detail bytes all differ. It feeds the actual saved exit into `ShellViewModel.closeWizard` and verifies the awaiting-sync trip-page stack. It does not rely on a mock call recorder. The test advances the injected checking clock through 1,200 ms plus the 300 ms completion pause. Every created model/service/writer scope is cancelled and joined before resetting Main or closing the database, under `NonCancellable` where teardown must complete.

The two existing `TripActionsContentTest` seam tests now inject a successful preparer; their navigation and sheet-dismissal expectations are unchanged. A deliberate Ignored-preparer mutation made the Edit seam test fail, then was restored before the Task 5 commit. No test allow-list was widened.

The one emulator journey, `TripEditJourneyTest.editFromActionsSavesAndOpensTheTripPage`, passed on pixel API 36 in **4.799 s** on its single run. It launched seeded at the upcoming trip's actions sheet, proved the untouched Update disabled, changed preparation through slider semantics, saved and landed on the trip page with My trips selected. StrictMode I/O violations and the refused-change presentation counter both stayed at zero. Pixel remains running.

The Phase 9h questions occupy OQ-A450–OQ-A460, excluding resolved OQ-9h-10; OQ-A461 records the progress settlement. Copy uses existing generated English/Dutch resources throughout, with no Android-only additions. The singular nights catalog limitation remains OQ-A459. The only added dependencies are test-only `:core:testing-junit`; no runtime dependency, native library, initializer or manifest entry was added. The initial implementation changed no timeline file. Review round 1 changes the presentation marker’s semantics read; its two-frame trace, drawing and clock paths stay the same. Perf, profile and full `make check` remain host-run commands as instructed; wizard profiles remain Phase 13.

### Initial validation record (superseded by review round 1 below)

Measured command wall times below are from the worker's monotonic clock; JVM/emulator results are not physical-device performance claims.

| Command / scope | Result | Wall time |
|---|---|---|
| `make targeted ONLY="core:wizard:WizardEditProgressTest core:wizard:WizardProgressTest core:wizard:MemberWizardDraftsTest"` | 35 JVM cases pass | 3 s Gradle-reported |
| Task 7 targeted seed/price/screen classes plus the two release-readiness methods | 40 debug cases and both release checks pass | 13.417 s |
| `make targeted ONLY="feature:wizard:WizardEditGuardsTest"` | 228 pass; 8.120 s test-class total | 11.238 s |
| `make captures ONLY=WizardEdit,WizardReturnWeb,WizardCheckingEdit,WizardCheckOfflineEdit,WizardCheckServerEdit,WizardExcludedEdit,WizardDiscardEdit,WizardDuplicate,TripActions` | 32 PNGs, including all 29 new outputs | 20.839 s |
| Task 9 targeted entry classes / saved-detail fix | All five pass in the final quick result; class 1.478 s, cases 0.145–0.586 s | Final targeted save/routing check 5.410 s |
| `make targeted ONLY="app:TripEditJourneyTest"` | One journey passes; XML duration 4.799 s | 14.455 s |
| `./gradlew --console=plain :app:lintDebug :feature:wizard:lintDebug :feature:trip:lintDebug` | Pass | 44.173 s |
| `:app:testDebugUnitTest :feature:trip:testDebugUnitTest :feature:wizard:testDebugUnitTest -Pcf.tier=layout`, through `Scripts/run-tier.sh phase-9h-layout` | 1,150 cases pass: app 70, trip 304, wizard 776; results in `build/test-results/phase-9h-layout/` | 38.678 s |
| `:app:testReleaseUnitTest -Pcf.tier=quick`, through `Scripts/run-tier.sh phase-9h-release` | 185 cases pass; results in `build/test-results/phase-9h-release/` | 7.549 s |
| `make format` (final) | Pass | 14.858 s |
| `make quick` (single run) | **Fails on the existing timeline marker guard below**; 5,091 measured cases, zero budget overruns, zero hangs | 47.025 s |
| `make targeted ONLY="feature:trip:TimelinePerfMarkersTest#theMarkerFlipsTwoFramesAfterTheFirstRealReplay"` | Pass without code changes; one diagnostic isolation run | 7.844 s |
| `:build-logic:convention:test`, through `Scripts/run-tier.sh phase-9h-build-logic` | 37 cases pass | 3.475 s |
| `Scripts/test_scripts.sh` / remaining tooling cases | Scripts 170 pass, contracts 25 pass, l10n 49 pass, icons 11 pass; see environment correction below | 19.863 s initial call, then 0.150 / 0.093 / 0.130 s |
| `make timing` | All seven serial JVM timing cases pass, including the timeline preparation/drawing budgets | 4.654 s |

The initial aggregate quick gate was **red**. Its only failed test is unchanged `TimelinePerfMarkersTest.theMarkerFlipsTwoFramesAfterTheFirstRealReplay[36]`, with:

```text
Failed: assertExists.
Reason: Expected exactly '1' node but could not find any node that satisfies:
(TestTag = 'trip.timeline.presented')
```

The failure took 0.036 s, so it is not a timing-budget overrun. It passed unchanged in the single-method isolation run. The guard drives the native main looper for two fixed 16 ms intervals, while the production marker uses two Choreographer callbacks. At that point the cause was not established; review round 1 below records the reproduced publication race and fix. No timeline code or marker assertion was changed to hide it, and `make quick` was not rerun. Full evidence remains in `build/test-results/quick/`, with the isolated result in `build/test-results/targeted/`. All Phase 9h cases and the existing 9a/9b/9g suites passed in the quick result, as did the unchanged timeline invalidation guards.

Because quick stopped at that test task, its later build-logic, tooling and serial timing stages were run separately. Invoking `Scripts/test_scripts.sh` directly first omitted Make's `IOS_REPO` / `WEB_REPO` exports, so the contracts parity method failed with `set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. With those exports restored, only that method was rerun, followed by the two tooling suites the script had not reached. All passed. No production or test source fix was needed for that environment error.

No command was blocked by adb or the emulator sandbox. No perf/profile tier, full check, extra emulator journey or push was attempted. The initial board comparison was handed to the host; review round 1 addresses its findings below.


### Review round 1: resolved findings and validation

The loaded suite reproduced the marker failure with `presented == true` after both native frame callbacks, while semantics still exposed `trip.timeline.opening`. Diagnostic uptime values were replay 310 ms, first callback 325 ms, second callback 340 ms, assertion 372 ms. The marker’s tag depended on a later recomposition. Reading the flag inside the semantics lambda publishes it without that extra composition. The two-frame trace and the existing test’s frames, waits and assertions are unchanged. `TripPageTestRig` renders `TripPageScreen` directly, so the new `TripPage` busy collection was a separate invalidation issue, not evidence of a delayed first real replay. Temporary diagnostics were removed. Detailed evidence and host performance follow-up are in [perf-timeline.md](perf-timeline.md#phase-9h-review-round-1-marker-publication-2026-11-20).

The page now observes only whether actions exist; the full action state is collected inside its open sheet. The real-page busy-toggle guard failed with 16 composition passes before the fixes and records zero afterward. The wizard hand-off no longer suspends after consuming its request: a paused native animation and removal of the sheet composition still open exactly one wizard. Failure events claim presentation once across composition recreation; the counter proves that an identical later event presents again. The launcher lives in the retained shell ViewModel, and a retained-owner test keeps its pending guard shared across shell rebinding. Duplicate now has the same quiet fallback-load-failure coverage as Edit.

`ApiClientTransportTests` previously used real loopback sockets for disconnect/truncation and an ephemeral port for refused connections. The host’s one-off disconnect failure was not reproduced separately; its external scheduling dependency is removed. Scripted socket streams exercise the real OkHttp codec, while injected connect/DNS errors cover the other cases. All four still require `DataFailure.Offline`; request bytes and one transport attempt prove the failure originates at the intended stage. No network production code changed.

| Review validation | Result | Command wall time |
|---|---|---|
| Five consecutive loaded marker runs | 5/5 pass; unchanged marker case 2.714 / 3.340 / 3.191 / 2.865 / 2.749 s, each a cold SDK case | 20.438 / 21.262 / 18.513 / 26.438 / 26.013 s |
| `make targeted ONLY=feature:trip:TimelineInvalidationTest` | 13 pass; new busy case 0.091 s | 7.923 s |
| `make captures ONLY=WizardReturnWeb` (once) | Two PNGs regenerated and compared with host board renders; the output directory now holds these two captures | 18.951 s |
| App/trip/wizard layout tier (once) | 1,150 pass: 70 / 304 / 776 | 35.418 s |
| `./gradlew --console=plain :app:lintDebug :feature:trip:lintDebug :feature:wizard:lintDebug` (once) | Pass | 16.765 s |
| `make targeted ONLY=app:TripEditJourneyTest` (once) | Pixel API 36 journey passes in 4.828 s; StrictMode/refusal checks pass | 12.522 s |
| `make format` (final) | Pass | 17.883 s |
| `make quick` (once) | Pass: 5,268 XML cases including 171 skipped; 5,097 measured, zero budget overruns/hangs; 37 build-logic and 255 tooling cases, seven serial timing cases pass | **71.281 s**, above the 60 s warm target |

The final quick marker takes 0.031 s; the new busy guard takes 0.041 s; the repeated-failure counter case takes 0.302 s. The first Robolectric SDK case in the trip fork costs 6.152 s and is recorded separately as a cold warm-up. The four deterministic transport cases take 0.011 / 0.004 / 0.004 / 0.003 s. Quick’s main Gradle stage reports 46 s and the Scripts suite 19.798 s; the aggregate wall time is reported as measured and was not retried.

Five-run evidence is archived in `/private/tmp/9h-review-proof/review-loaded-1` through `review-loaded-5`. The original failure with temporary diagnostics is `/private/tmp/9h-review-marker-diagnostic.xml`. Gate results are in `build/test-results/quick`, `phase-9h-review-layout`, `targeted-emu` and `timing`; the complete command/wall-time report is `/private/tmp/phase-9h-review-1-report.md`.

The brief records the orchestrator’s direct-overlay hand-off override; no new OQ row or Android-only copy is needed. Host-only follow-up remains `make check`, `make perf-timeline` and `make baseline-profile`; none was run here. No required command was blocked by the sandbox. Pixel remains running. No runtime dependency, native library or launch-path change was added.


### Review round 2: retain the pending wizard request (2026-11-21)

`TripPageViewModel` retains the prepared mode in `pendingWizardRequest: StateFlow<WizardMode?>`. Observing or cancelling collection cannot consume it. Only `acknowledgeWizardRequest()` atomically replaces it with null and reports whether a request was acknowledged. The window receives the pending value as state; one `LaunchedEffect(pending)` acknowledges, dismisses and opens without suspending. Recreated windows see an unacknowledged request, while a request already acknowledged cannot open again. Starting another action cannot overwrite a pending prepared request. The shell overlay guard is unchanged.

The new JVM cases use the existing fixture’s single `StandardTestDispatcher(testScheduler)`, fake clock and fake repositories, with `runTest(timeout = 60.seconds)`. They cancel and join the disposed/recreated collectors; fixture teardown cancels and joins the ViewModel before resetting Main. Cases prove retention without acknowledgement, replay to a fresh collector, no second open after acknowledgement, and no overwrite by a second action. No extra Robolectric case was added. The existing paused-clock hand-off fixture now passes state and an acknowledgement callback; it explicitly applies the changed snapshot before advancing its frame.

All 12 `TripActionsPrepareTest` cases pass. The three new cases take 4 / 15 / 6 ms in the final quick run. The existing six content cases, four busy/window cases and five app entry cases pass; the entry and all 13 timeline invalidation cases ran in one targeted command (14.260 s). The corrected window method passed its targeted check in 6.772 s. No new scheduler or Main looper was introduced in the JVM tests, and no resumed run hung.

`make format` passes (17.437 s). The single `make quick` passes (64.543 s), with zero timing-budget overruns; its aggregate wall time remains above the 60 s warm target and was not retried. Its report contains 5,271 XML cases (171 skipped) and 3,452 recorded budget checks; unchanged tasks may remain up to date. The first trip Robolectric SDK case is recorded separately as a 5.572 s cold warm-up. The busy invalidation guard takes 42 ms. Results remain in `build/test-results/quick/`; the full command record is `/private/tmp/phase-9h-round2-report.md`.

The orchestrator verified round 1’s host `make check` and `make perf-timeline`; the latter is recorded in `bb5a22cc`. This pass ran the requested targeted, formatting and quick checks only. No timeline rendering, copy, runtime dependency or native library changed, and no OQ row was added.

## Phase 11a — guest wizard

Welcome's Plan my trip and Sign in's guest action open the existing wizard in guest mode. The path is Where → Outbound → Sleep → Checking → requested Age/Shift work → Fine-tune → Ready. There is no return flight or sleep override. Age and shift work are the only screening questions; the five retired health questions never return. The member path continues to use its existing dependencies and generation service.

`RootContent` owns the guest entry's ViewModel store. `GuestWizardHolder` and `TripWizardViewModel` survive recreation under the entry revision and are cleared when it leaves. A new Plan my trip creates a new revision. Header close asks to discard entered changes, except on Excluded; an empty Where closes immediately. Excluded, checking failure and No plan footer closes return directly to Welcome. Every exit discards the draft and deletes its stored token secret. Route/revision/presence guards prevent a late exit, foreground expiry or failed start from replacing another destination. Exits remain acknowledged in the ViewModel across recreation.

Ready shows Route, Flight, Sleep (times plus chronotype), Questions and Extras. Questions revisits the screening branch rather than an optional prompt. The guest price uses the review's cost and balance, with the Android-only `android_wizard_guest_price_paid_one` / `android_wizard_guest_price_paid` pair selected by balance. The mock's balance is 3; a server balance of 1 selects singular copy. A free plan retains the existing sign-up-credits-unused copy. The member balance line is hidden. Make my plan raises a retained sign-up request after flushing the draft; it never calls save. Phase 11a binds the launcher to a debug counter and immediate dismissal. Phase 11b supplies the designed sign-up sheet.

Guest review uses the public data source, device identity and a 30-second deadline. A token reuses its secret. An expired token gets exactly one retry without identity, retaining that secret. Rotation writes the replacement before removing the old entry. Canceled or discarded reviews delete a newly returned token's secret and drop their result. Drafts, snapshots, routes and diagnostics contain no secret.

The draft lives at `noBackupFilesDir/<namespace>/guest-wizard.json`, written on the injected IO dispatcher through a synced temporary file and atomic replacement. The shared record codec strips secrets and retired fields. Recovery normalizes without refreshing `updatedAt`; persistence advances the sliding 30-minute expiry. Instance mismatches fail, older revisions do nothing, and only one-way drafts recover. The secret store uses the namespace's `<keychainService>.guest` Keystore alias and Preferences DataStore file `guest_secrets_<directory>.preferences_pb`. Keys are `t.<token>`; values are Base64 `[0x01][12-byte IV][ciphertext + GCM tag]`. `deleteAll()` clears entries while retaining the alias. Invalidated keys clear entries and replace the key once. Account wipe includes the guest store.

Recovery runs once per process on signed-out Welcome after launch hand-off, waiting until the launch clock reaches `intro.start + 900 ms`. Navigation, revision, session or epoch changes invalidate the attempt. A frozen diagnostic intro never recovers. There is no message or transition between root destinations. Foreground hooks sweep expiry while composing with the existing hook; a visible expired wizard leaves through the same guarded exit.

The guest capture list is independent of the member list:

| Board / seed | State |
| --- | --- |
| `WizardWhereGuest` | AMS → NRT; guest notice, no last-trip hint |
| `WizardOutboundGuest` | Outbound times; Continue leads to Sleep |
| `WizardOutboundPastGuest`, `WizardOutboundPastGuestDarkNL` | Guest upcoming-arrival error |
| `WizardCheckingGuest` | Frozen checking lines, guest answers wording |
| `WizardFineTuneGuest` | Six review-driven extras, preparation, no sleep override |
| `WizardReadyGuest`, `WizardReadyGuestDarkNL` | Paid Ready, guest rows and dynamic price |
| `WizardReadyGuestFree`, `WizardReadyGuestFreeDarkNL` | Free Ready |
| `WizardDiscardGuest`, `WizardDiscardGuestDarkNL` | Outbound with native discard alert already open |

All twelve were captured once with `make captures ONLY=WizardWhereGuest,WizardOutboundGuest,WizardOutboundPastGuest,WizardCheckingGuest,WizardFineTuneGuest,WizardReadyGuest,WizardDiscardGuest`; Gradle took 22 s. The PNGs are in `build/captures/`. The 132 guest layout cases cover light/dark, en/nl and small/pixel8/foldInner/largeFont, plus the two wide-page states with side insets. They passed in 6.220 s of test time (10 s Gradle); the first SDK/class case took 2.092 s and all warm cases stayed below 1 s. Large-font Dutch Ready stacks its rows and keeps its footer fixed during scrolling.

Board differences pending rendered-board review:

- `WizardReadyGuest`, `WizardReadyGuestDarkNL`, `WizardReadyGuestFree`, `WizardReadyGuestFreeDarkNL`: Questions shows the model's two answered questions, replacing the board's retired three-question count (OQ-A542). Extras also uses the model count, seven for this seed.
- `WizardReadyGuest`, `WizardReadyGuestDarkNL`, `WizardReadyGuestFree`, `WizardReadyGuestFreeDarkNL`: the summary retains **420 min** rather than the boards' **7 hours / 7 uur**. The shared pinned iOS `CopyLineFormatter.formatMinutes(duration: false)` formats the 420-minute argument this way, as the member Ready section records. The guest one-way summary end date is **Wed 14 Oct / wo 14 okt**, matching the one-way variants described above; it no longer inherits the base member capture's 18 Oct end date.
- `WizardFineTuneGuest`: rows follow the review and preferences (OQ-A543). The current seed produces the board's six extras: room, wake, seat, drinks, light and blue glasses. No fixed guest-only row filter is applied. Values and optional rows can change with the review.
- `WizardDiscardGuest`, `WizardDiscardGuestDarkNL`: native Material dialog geometry and actions follow the shared `CfAlertDialog` wrapper.
- All twelve inherit the existing Android wizard typography, safe-area layout, 48 dp targets and native controls. Checking's spinner is frozen only for its capture. No Chromium board rendering was attempted; the orchestrator compares the rendered boards on the host.

Debug seeds require `CF_AUTH_TEST_ID=<uuid>` and `CF_UI_TEST_PROFILE=screen`. `CF_GUEST_WIZARD=1` plus `CF_WIZARD_BOARD_CAPTURE=<board>` opens that guest state. `CF_GUEST_WIZARD_JOURNEY=1` plus `CF_WIZARD_BOARD_CAPTURE=WizardReadyGuest` stays on Welcome and then opens frozen Ready when Plan my trip is tapped; this journey checks the sign-up hand-off and performs no review. The legacy board name `WizardGuestJourney` is used by `GuestWizardReviewIoTest` with `CF_GUEST_WIZARD=1`: it opens live Sleep directly, with current-clock flight dates 30 days ahead and shift work false, then exercises the real first guest review and secret mint. Other capture seeds remain frozen. `CF_GUEST_RECOVERY_BOARD=WizardSleep` writes a recoverable draft before Welcome and runs the recovery scheduler. The screen fixture completes the hand-off skipped by its overlay-free profile. Every reseed deletes the draft, temporary file and all guest secrets. Seeds and counters remain debug-only; release-readiness tests guard their absence.

Manual process-kill evidence (pixel AVD, API 36, debug mock build): opened Plan my trip, selected AMS → HND through real search, set upcoming departure/arrival dates and times, and continued to Sleep. The landed mock search has HND rather than NRT. Installed with `python3 -I Scripts/device_lock.py -- "$ANDROID_HOME/platform-tools/adb" install -r -t app/build/outputs/apk/debug/app-debug.apk`. Then:

```sh
adb shell input keyevent KEYCODE_HOME
adb shell uiautomator dump /sdcard/guest-home.xml
adb shell am kill net.chronoflow.app.native.debug
adb shell pidof net.chronoflow.app.native.debug
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity
adb exec-out screencap -p > build/captures/GuestRecoveryAfterKill.png
```

The idle dump allowed the activity to stop before killing; `pidof` returned no PID (status 1), and relaunch reported `LaunchState: COLD`, 2,553 ms. Sleep reappeared without a message. Draft form and instance were identical before/after, and the recovered step was `sleep`. Screenshot: `build/captures/GuestRecoveryAfterKill.png`. `adb logcat -d -s StrictMode` contained zero stacks mentioning the three guest storage/recovery classes. These are functional emulator observations, not device performance claims.

The two new emulator classes each ran once, as requested. `GuestWizardJourneyTest.testWelcomeThroughReadyRequestsSignUp` passed its journey, single counter, IO and internal 10-second assertions, but the tier rejected its 10.360 s wall time including setup/teardown (`run-tier: over budget: emulator net.chronoflow.app.guest.GuestWizardJourneyTest#testWelcomeThroughReadyRequestsSignUp 10360 ms > 10000 ms`). Its debug timeline spent 3.013 s applying the cold seed before activity launch. No budget was relaxed and no timing retry was made.

`GuestRecoveryScreenTest.testWelcomeHandsOverToRecoveredSleep` failed waiting for `wizard.sleep.continue` after 5,000 ms; its 12.189 s wall time also failed the tier. The screen profile's skipped overlay left `handOffComplete` false. The debug recovery fixture now completes that skipped hand-off after first commit. Seed/reseed and scheduler JVM checks passed after the correction; emulator re-verification remains with the orchestrator under the one-run instruction. Exact command: `make targeted ONLY="app:GuestRecoveryScreenTest#testWelcomeHandsOverToRecoveredSleep"`. The manual process-kill check above independently proved production recovery with the real hand-off.

The real-Keystore smoke case passed earlier in 0.797 s. The app, wizard, datastore, UI and data lint tasks plus `:app:verifyReleaseManifest` passed together in 69 s. No runtime dependency or native library was added. Startup/Welcome and wizard paths changed; the orchestrator must run `make baseline-profile` and `make perf`, commit regenerated profiles and record startup metrics in `docs/perf-startup.md`. No timeline rendering or benchmark path changed, so `make perf-timeline` does not apply.

Final verification on the implementation checkout:

| Check | Result | Wall time |
| --- | --- | --- |
| Touched-module layout tasks | 1,251 cases passed, zero budget overruns; datastore/data have no layout cases | 28.00 s including collection |
| Layout evidence recollection | Certified app/UI/wizard outputs accepted; no tests rerun | < 1 s |
| Single `make quick` invocation | 5,715 executed cases, two Konsist rule failures; no hangs or budget overruns | 49.13 s |
| Targeted rule corrections | Canonical `60.seconds` in RootContent/Welcome tests; exact-file `GuestWizardHolder` naming exception; planted same-name class elsewhere rejected | 6 s Gradle for rules, 8.03 s app tier |
| Build-logic tests | 37 cached certified cases accepted | 2.79 s |
| Script tests | 171 passed | 19.961 s |
| Contract generator tests | 24 passed initially; parity case needed the Makefile's `IOS_REPO`/`WEB_REPO` environment, then passed alone | 0.020 s + 0.091 s |
| String / icon tooling | 49 / 11 passed | 0.016 s / 0.088 s |
| Serial timing tier | Seven passed, including both timeline budget classes | 2.82 s |

The initial layout collector glob accidentally included unrun timing directories; a second attempt still included datastore/data's zero-case layout outputs. Restricting recollection to the three modules with layout cases accepted all existing certified outputs. No layout test was rerun. The quick invocation stopped before build-logic/tooling/timing, so those components ran separately after its two rule failures were corrected. `make quick` was not repeated. The required holder name is the only ViewModel-suffix exception, scoped to its exact file and class; the test-boundary allowlist is unchanged. All 13 `TimelineInvalidationTest` cases passed in the quick run (0.796 s total).

OQ-A533–A543 record the eleven defaults from the brief. No further open question was needed. The two generated English/Dutch Android-only price resources remain awaiting approval in the plan. Host follow-up still includes a fresh `make quick`, the corrected recovery emulator case, the journey's wall-clock budget and the deferred startup profiles/metrics; no all-green commit gate is claimed for this sandbox run.

### Review round 1 (2026-11-22)

The host verified `GuestRecoveryScreenTest.testWelcomeHandsOverToRecoveredSleep` in 0.639 s. The journey now seeds guest Ready behind Welcome and checks Plan my trip → Ready → one sign-up request; JVM/ViewModel and seeded screen cases cover the intermediate steps. It omits appearance overrides and their unused reset, and uses one structural wait per action. The final pixel emulator run passed in **5.693 s**, below the unchanged 10 s budget. An earlier full-step trial passed in 9.658 s but lacked margin and was replaced; its software-cipher experiment was removed.

`WizardFineTuneGuest` now has all six extras at Add. The four guest Ready captures end on 14 Oct and retain the pinned iOS formatter's 420 min wording, recorded above by board name. All five affected captures were regenerated once (21 s Gradle); Fine-tune and Dutch dark Ready were inspected directly.

Guest expiry sweeps now observe the process lifecycle, once per real `ON_STOP` → `ON_START` return. The holder retains that observer across configuration changes and removes it when the entry clears. Three plain JVM cases prove active-process observer reattachment, foreground return and cleanup (3 ms total); the separate host rotation and exit cases also passed.

Targeted checks passed: seed application (3 cases, 6 s Gradle), Fine-tune seed/screen (15 cases, 7 s), Ready seed plus layout guards (146 cases, 11 s), and foreground/host/exits (14 cases, 8 s). The single `make quick` passed in 67.081 s overall, with 42.43 s in its JVM/Robolectric tier and zero budget overruns. Build-logic, tooling and serial timing checks passed as part of that command. No perf or `make check` was run; pixel remains running.

### Review round 2 (2026-11-22)

The short `GuestWizardJourneyTest` remains the frozen Welcome → Ready → sign-up hand-off check; it was not changed or rerun this round. It performs no review and its capture identity is not a minted server draft. Phase 11b's claim tests will need a live reviewed guest draft. The legacy `WizardGuestJourney` seed is retained because the new seeded IO screen test uses it: it now opens live Sleep, with future AMS → HND times, the current clock, shift work false and no UI override. The seed roles are also corrected in the plan and 11a brief.

The earlier journey's coverage is now split at the cheapest layer:

| Former journey step | Current proof |
| --- | --- |
| Welcome → guest wizard | `GuestWizardJourneyTest` checks the Ready/sign-up hand-off; `GuestWizardHostTest.planMyTripOpensGuestWhere` checks the empty entry |
| Where and Outbound → Sleep | `GuestWizardScreenTest.testWhereGuest`, route/times ViewModel tests, and `GuestWizardModeTests.GuestPaths.guestPathBackTargetsAndSleepAreMandatory` |
| Sleep edits and per-step guest draft writes | `GuestWizardIoTest.sleepReviewAgeAndRecheckKeepGuestStorageOffMain` uses the real ViewModel/dependencies and file store with guarded persistence; existing Sleep ViewModel tests cover controls/validation |
| Checking and first review/secret mint | The JVM test checks mint/dispatcher behavior; the concrete Keystore test checks get/set/rotate/delete IO hops; the emulator test now also requires a stored review secret after age appears (host rerun pending); Checking ViewModel/screen tests cover presentation |
| Age answer → recheck | `GuestWizardIoTest` checks the second request, age profile, known-secret reuse, token rotation and subsequent Fine-tune persistence; `KeystoreGuestSecretStoreTest.runsOnTheInjectedDispatcher` checks the concrete store's IO hop |
| Fine-tune → Ready → sign-up request | Existing Fine-tune/Ready ViewModel and guest screen tests plus the short hand-off journey |

`GuestWizardIoTest` uses one scheduler with separately identifiable Main, IO and worker dispatchers. Guarded stores reject Main calls, with a negative control proving both guards fail on Main. The added `guestFileStoreHopsToIoFromMain` case invokes concrete guest-file persistence from Main and exercises the supplier's IO assertion. Its original two cases passed (0.171 s initially, 0.073 s in quick). Scopes are canceled and joined before resetting Main. The original seeded screen emulator case reached the age question after review and passed in **2.663 s**, including a zero debug IO-violation assertion. That version did not assert a stored secret; the added debug probe awaits a host emulator rerun. `adb logcat -d -s StrictMode` had zero matches for guest store/recovery classes. The unchanged short journey therefore retains its round-1 time and scope without taking on this IO coverage.

The foreground unit test is renamed to `reattachingToAnActiveProcessDoesNotSweep`; disconnected activity lifecycle events were removed. `GuestWizardHostTest.leavingTheRouteClearsTheViewModel` now checks that the real process registry loses one observer when the entry clears and gains one on a new entry. Both targeted cases passed (8 s Gradle; the host case's cold SDK run was 2.692 s).

Verification: the 14 seed cases passed (4 s Gradle), and the corrected concrete Keystore dispatcher target passed (3 s). An initial command used a nonexistent method name and reported no matching tests; it was corrected to `runsOnTheInjectedDispatcher`. The single `make quick` completed its JVM/Robolectric run in **42.75 s**, with zero assertion failures and no hangs, but failed one untouched onboarding class warm-up budget:

```text
run-tier: over budget: robolectric net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed 3266 ms > 3000 ms
```

That case was not retried or changed. The skipped quick components ran separately and passed: build-logic (37 certified cases, 2.46 s), script/contract/string/icon tooling (171/25/49/11 cases; 19.941/0.111/0.017/0.088 s), and serial timing (7 certified cases, 1.42 s). No perf or `make check` was run; pixel remains running.

**Final coverage fix after review round 3 (2026-11-22).** The concrete Keystore dispatcher test now exercises `set`, `rotate` and `delete` as well as `get`. Its checked DataStore asserts the caller's injected IO dispatcher before DataStore can perform its own dispatch; separate wrong-dispatcher controls for all three mutations must fail that boundary. Mutation counts also prove the writes/deletions happened. `GuestWizardIoTest.guestFileStoreHopsToIoFromMain` now calls concrete guest persistence from Main, so removing that store's IO hop makes the file-supplier assertion fail.

The debug-only `GuestWizardDebugSecrets.hasReviewSecret` probe returns only whether the persisted review token resolves to a non-empty secret, with every read on injected IO. JVM checks prove both presence after mint/recheck and absence after clearing secrets. `ReleaseReadinessTest` proves the hook is absent from release. The seeded emulator test now waits for this probe after the age question, before checking zero IO violations; this enhanced assertion has been compiled but has not been run on the emulator. Run on the host:

```sh
make targeted ONLY="app:GuestWizardReviewIoTest#firstGuestReviewFromSleepDoesNoMainThreadIo"
```

All five targeted JVM/Robolectric/release cases passed: Keystore dispatcher (1.280 s cold SDK case, 6 s Gradle), three guest VM cases (0.171 s total, 4 s Gradle), and release exclusion (0.007 s, 4 s Gradle). `:app:compileDebugAndroidTestKotlin` passed in 3 s. The single `make quick` passed in **67.756 s** overall, including a **42.92 s** JVM/Robolectric tier with zero budget overruns; the concrete dispatcher case took 39 ms and the Main-caller file-store case 6 ms warm. Build-logic, tooling and serial timing checks passed within quick. No emulator, perf or `make check` run was made for this fix.


## Phase 11b: guest sign-up and claim

The guest holder retains a single sign-up coordinator; Ready opens the native sheet, and leaving the root entry clears it. Registration carries the live review claim. The per-account continuation waits through Verify, Terms and Walkthrough, then opens the claimed trip through the existing awaiting-sync route. Password/passkey sign-in without a receipt seeds the member wizard; two-factor continues to My trips.

| Board | Debug seed (also requires `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`) |
| --- | --- |
| `GuestSignup` | `CF_GUEST_WIZARD=1`, `CF_WIZARD_BOARD_CAPTURE=GuestSignup` |
| `GuestSignupFailed` | `CF_GUEST_WIZARD=1`, `CF_WIZARD_BOARD_CAPTURE=GuestSignupFailed` |
| `GuestSignupRetry` | `CF_GUEST_WIZARD=1`, `CF_WIZARD_BOARD_CAPTURE=GuestSignupRetry` |
| `GuestSignupReview` | `CF_GUEST_WIZARD=1`, `CF_WIZARD_BOARD_CAPTURE=GuestSignupReview` |

The captures use the real `GuestWizardRoute(holder, revision)` and native sheet with injected dependencies and the board presentation seed. The board frame is 375 × 812 dp, density 3, English, light, zero insets. Robolectric's `BoardCapture` captures the dialog window, so these are **sheet-only captures without the guest Ready backdrop**. No stored references or Chromium board renders are used.

Recorded differences for **each of `GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`**:

- In the app, the backdrop is guest Ready, replacing the board's retired GuestReview. It is outside the Robolectric dialog capture.
- The password hint uses the server's 12-character policy from iOS, replacing the board's 10-character hint.
- The native M3 drag handle occupies a 48 dp slot; content begins directly below it, replacing the board's 5 px grabber and 12 px top padding (and iOS's overlaid grabber with 20 pt content padding).
- Fields and buttons retain Phase 2's native Android field decoration and 48 dp interaction targets. The sheet uses the shared card fill and 38 dp top corners.

All 80 guest sign-up layout cases passed: Ready/Failed/Retry/Review × small/pixel8/foldInner/largeFont/landscapeSideNav × en/nl × light/dark. They measure the production content and shared native handle; foldInner fields are 480 dp wide and centred, and pixel8 fields are the frame width minus 40 dp.

Capture command (run once): `make captures ONLY=GuestSignup,GuestSignupFailed,GuestSignupRetry,GuestSignupReview`. It passed in 22 s Gradle wall time and wrote exactly four 1125 × 2436 PNGs in `build/captures/`, named after those boards. Ready and Review were inspected directly: outside the sheet the dialog capture has no Ready backdrop, and the taller Review form scrolls to its lower actions/consent. The host still compares these with its rendered boards.
