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
| `WizardOutbound`, `WizardOutboundDark`, `WizardOutboundErrors`, `WizardOutboundPast`, `WizardOutboundStop`, `WizardOutboundOvernight`, `WizardOutboundThree`, `WizardOutboundTwoErrors`, `WizardReturnFlightPast` | At 390 dp, English Amsterdam rows with filled pills stack when their intrinsic widths do not fit. This follows the specified fit rule (OQ-A398); empty pills and shorter Dutch labels can remain inline. No font shrinking or date abbreviation was added |
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
