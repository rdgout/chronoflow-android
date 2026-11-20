# Phase 9c: Wizard shell and Where

- **Checked 2026-11-16** against iOS `f391a1fa`, the boards, `chronoflow-new` and the revised 4e, 6a, 9a and 9b briefs. Changes: the wizard now lives in 6a's shell overlay (no `RouteSeam`, no root-route replacement, no `RouteProbe` for the wizard); search maps 9b's `AirportSearchSession` instead of a second pipeline; title top is `safeTop + 88`; progress percent truncates; the last-trip hint uses 9a's `state.mode`; `removeStop` takes the route index; the full `airportIsAllowed` rule; the limit seed and suffix handling; the Robolectric IME test was removed (AGENTS "Tests" 6); OQ-9c-10 follows iOS; OQ-9c-11 and OQ-9c-12 were added.

- **Stack check (2026-11-20)** against `feat/phase-9c-wizard-shell-where` at `6a190cf1` (9b on top). **Worker: repeat these checks first (`git grep`, read the files) and stop and report on any mismatch.**
  - **Verified as written:** `TripWizardViewModel(entry, deps)` with `state: StateFlow<TripWizardUiState>` (`Loading`, `Active(wizard: MemberWizardSnapshot, progress: Double, path: List<MemberWizardDestination>)`, `LoadFailed`, `Closed`), public `search: AirportSearchSession`, `suspend fun discard()`, `factory(entry)`, `onCleared()` → `wizard?.close()` (`feature/wizard/…/TripWizardViewModel.kt`); `TripWizardDependencies` (`store`, `airportSearch`, `source`, `now`, `writerScope`, `checkingClock`, `start()`, `startTrip()`, `newGeneration()`) and `TripWizardDependenciesKey` (`TripWizardDependencies.kt`); `SessionTripWizardDependencies` (`app/…/wizard/`, `store: MemberWizardStore`) built lazily as `SessionGraph.wizard` (null without a permit); `AirportSearchSession`/`AirportSearchState`/`AirportSearchService(search, now, sleep, cache?)` in `:core:wizard` `net.chronoflow.core.wizard.model` (not `:core:data`); 9a's `openAirport`, `chooseAirport`, `airportIsAllowed`, `swapAirports`, `removeStop(at)` (leg index), `makeDirect(home)`, `discard()`, `close()`, plus the extensions `finishRoute(home)` (`MemberWizardCompletion.kt` l.132), `navigationPath`, `popNavigation(to, from)`, `firstScreen`, `showsBack` (`MemberWizardNavigation.kt`), `progress` (`MemberWizardReview.kt`), `hasChanges` (`TripWizardActions.kt`); `ShellNav.push` sets `overlay` for `Route.Wizard`, `ShellNav.back` clears it first; seed keys `CF_WIZARD_AUTO_OPEN`/`CF_WIZARD_BOARD_CAPTURE` already parsed into `TestSeed.wizardAutoOpen`/`wizardBoardCapture`; `app_debug_route_x` in `core/ui/src/debug/res`; the design-system names in "Names from earlier phases" (except below); tags `trips.create`, `trips.create-empty`, `tab.trips`; the 4c mock's 8 airports; the boards (unchanged since the first docs commit).
  - **Changed in this brief:** 6a's holder is **`ShellViewModel`** (`app/…/shell/ShellViewModel.kt`, local name `holder`), not `ShellHolder`; **the overlay replaces the tab scaffold** (`AppShellScaffold.kt` l.124–161 composes either the scaffold or the overlay `Box`), so no `clearAndSetSemantics` is needed and OQ-9c-12 shrinks to the store owner; `WizardAirportSheet(index, side, replacingStop: Boolean?)` with lower-case `Side.from/to/home/stop`, and lower-case `MemberWizardStep`/`MemberWizardDestination` names (read every `From`/`Stop`/`outbound` in this brief as those); `:core:wizard`'s `WizardMode { member, guest }` clashes with `:core:navigation`'s `WizardMode` — 9b imports the latter as `WizardEntry`, keep that alias; `showBack` = 9a `showsBack`, first screen = 9a `firstScreen`; reduce motion is `LocalReduceMotion` (no `CfTheme.reduceMotion`); 5a's IME footer lives in `AuthScreen.kt` (no `AuthScaffold`); the app locale is `LocalConfiguration.current.locales[0]` (no locale holder); the in-memory store is `InMemoryWizardDraftStore` (`:core:testing`); the recovery check reads `MemberWizardStore.recover()` (no `peek()`; expired rows are deleted there); the plan section is l.2692–2732; the next free OQ number is **OQ-A370**; `CF_WIZARD_BOARD_CAPTURE=TripActions` already belongs to the trip page (`LaunchPlatform.initialTripSheet`) and must not open the wizard; `:feature:wizard` needs `libs.androidx.navigation3.ui` and `testImplementation(project(":core:testing-compose"))`; 9b's `FakeWizardDependencies` is `private` in `TripWizardViewModelTest.kt` — move it to a shared test-support file in a `test(wizard)` commit before reuse (user, 2026-11-20).
  - **Repeat check (2026-11-20):** `CfType` exposes wizard typography through `CfTheme.type.style(CfTypeScale.<name>)`, not direct `wizard*` / `screenPageTitle` properties; use the existing generated specs without changing the type system. `CfAlertDialog` buttons have the documented tags, but its separate window does not yet export them as resource ids; add that semantics export to its window in Task 5, preserving the existing dialog behavior. Tasks run in the main checkout in one lane; waves/worktrees are historical.
  - **Tests rewritten for AGENTS "Tests":** ViewModel logic moved from Robolectric to JVM tests on the ViewModel's UI state; no test asserts copy on screen (strings and content descriptions are checked as keys on the JVM or in captures); no test composes `AppShell`, `CfRoot`, `MainActivity` or the graphs (the Konsist `keptAppTestBoundaries` allowlist in `konsist/…/AppTestBoundaryRules.kt` has no wizard entry; do not add one); Robolectric renders the stateless `WizardScreen(state, onEvent)` only; budgets are AGENTS's (warm JVM/Robolectric < 1 s; first-case warm-up recorded separately); one emulator journey, and the keyboard check launches seeded.
  - **Not built in 9c:** OQ-A350 says "9c's bar never animates downward"; no 9c step reaches Sleep, so the bar shows the model value as iOS does (`MemberWizardSteps.swift` l.19–23) and 9e owns that case. OQ-A275 (the no-plan button "9c/9h wires it") stays with 9h.

- **Branch:** `feat/phase-9c-wizard-shell-where`. Preflight: `git -C <repo> rev-parse --abbrev-ref HEAD` prints this name, or stop. Start it from the top of the stack once **9b** (generation, airport search, recovery store, `TripWizardViewModel` scaffold) and **6a** (tab shell, My trips, the New trip entry) are in it. It also relies on:
  - 9a (the `:core:wizard` model);
  - 2 (primitives);
  - 0c (strings, `CopyIndex`, `android-only.json`);
  - 0e (profiles, `Guards`, `BoardCapture`, `ScreenTest`, seeds);
  - 4c (`MockDataSource.searchAirports`);
  - 4e (`Route.Wizard`, `WizardMode`, `RootNavigator`, `RoutePlaceholder(route, label)`, `DebugSeedRouting`);
  - 6a (`ShellNavState.overlay`, `ShellNav.push`/`back`, `ShellViewModel` (`holder`), `AppShell(root, onRootBack, placeholder)`, the `trips.create` / `trips.create-empty` New trip buttons). **6a shows the wizard as a full-screen overlay in place of the tab scaffold (`push(Route.Wizard)` → `overlay = route`; `AppShellScaffold` then composes only the overlay `Box`), not as a root route.** 4e has no `RouteSeam`; a seam is `RoutePlaceholder(route, label)` (6a brief §5.7 "Seams").

  If one of these is missing, stop and report it.
- **Plan entry:** the `plan.md` section "Phase 9c: Wizard shell and Where" (l.2692–2732). Also read:
  - "Android spec: window policy" (the Wizard row: column max 520 dp, centred from a 669 dp window);
  - "Android spec: state survival" (the Wizard row);
  - the A10 window policy (48 dp targets, font cap 1.3);
  - OQ-A6 (no glass), OQ-A12 (Android-only copy), OQ-A36 (the wizard defaults);
  - "Android-only copy awaiting approval".
- **Tests:** every test follows `AGENTS.md` "Tests": the cheapest tier, the time budgets, exact tag queries, no `Thread.sleep`, and the hang caps. Read that section before you write a test. The tier annotations are the 0e ones:
  - Robolectric: `@Logic`, `@Layout`, `@Snapshot`, `@Capture`;
  - emulator: `@Smoke`, `@Capture`.
- **Tags:**
  - **[F]** marks a fact read in source; the path is given.
  - **[R]** marks a rule this brief sets for Android.
  - `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. Never write there.
  - `Kit/` = `I/Packages/ChronoFlowKit/Sources`.
  - `DC/` = `_design/canvas/`.
  - Board coordinates are CSS px and map 1:1 to dp. On the boards, y = 0 is the top of the app content, below the status bar.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.
- **Paths:**
  - `…/wizard/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`
  - `…/wizard-debug/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`
  - `…/core-wizard/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`
  - `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`
  - `…/app-debug/` = `app/src/debug/kotlin/net/chronoflow/debug/`
- **Names from earlier phases.** This brief uses the names the plan, the research and iOS give, for example:
  - `TripWizardViewModel`, `MemberWizardStore`, `AirportSearchService`, `WizardForm`, `WizardAirportSheet`, `navigationPath`, `popNavigation`, `chooseAirport`, `airportIsAllowed`, `swapAirports`, `removeStop`, `makeDirect`, `finishRoute`, `discard`, `hasChanges`;
  - `RootNavigator`, `Route.Wizard(WizardMode.Create)`, `RoutePlaceholder`, `DebugSeedRouting`, `ShellViewModel`, `ShellNavState.overlay`;
  - 9b: `TripWizardViewModel.state` (`Active(wizard, progress: Double, path)`), `TripWizardViewModel.search: AirportSearchSession` (`setQuery`, `retry`, `state`), `AirportSearchState`, `TripWizardDependencies.start()`; the iOS `MemberWizardStore` is `:core:data` `MemberWizardStore` (production) and `InMemoryWizardDraftStore` (tests);
  - `CfModalBottomSheet`, `CfAlertDialog`, `CfLinearProgress`, `CfSegmentedButtons` (no per-option `enabled`), `CfIconButton`, `CfButton`, `CfSkeleton`, `CfSpinner`, `LocalSafeInsets`, `LocalHaptics`, `HapticEvent`, `LocalReduceMotion`, `RecordingHaptics` (`:core:testing-compose`).

  Before you code, open the merged code of 2, 4e, 6a, 9a and 9b and use **their exact names**. Where a name differs, keep theirs and note it in the PR. Do not rename earlier code.

  This phase adds **UI and wiring only**. It adds no model rule. If a route rule below is missing from 9a or 9b, port it into `:core:wizard` with its iOS test in a separate `fix(wizard): …` commit, and report it.

---

## 1. Goal

A signed-in member taps **New trip** on My trips. The wizard opens full screen on **Where**, in 6a's shell overlay slot (it covers the tab bar, as the iOS `fullScreenCover`). The member:
- picks From and To in a full-height search sheet;
- optionally swaps them;
- adds, replaces or removes stops, up to 16 flights;
- sees the same-airport error when From and To are equal;
- taps Continue.

The wizard then shows the next step. Times are built in 9d, so in 9c the next step is a debug step seam. The same screen also renders the **flight-home route** ("Your flight home"): From is locked to where the member landed, and stops are allowed.

Done means:

**The shell matches the `Wizard` board at any window size.**
- A centred top bar: a back chevron on the left (never on the first screen) and × on the right.
- A 4 dp dawn progress bar at the board percentages.
- A scrolling step column with title and subtitle.
- A fixed footer whose 52 dp Continue rises with the keyboard.
- A 520 dp centred column from a 669 dp window.

**Close and back behave as in `WizardSpec`.**
- × and system back on the first screen close at once when nothing was entered.
- Otherwise they open the "Discard this trip?" alert.
- Discard deletes the recovery row and returns to My trips.

**Quiet recovery works.**
- A wizard killed within 30 minutes reopens by itself after the shell is up.
- It comes back on the same step, with the same values and the same open sheet, and shows no message.

**The search sheet has every state.** Hint, searching, results, empty, error with retry, and throttled.

**Seeds and tests.**
- The `CF_WIZARD_AUTO_OPEN` and `CF_WIZARD_BOARD_CAPTURE` seeds work.
- Every Where, home-route, search and discard board has a Robolectric guard set and a capture.
- One emulator check proves that the search field and the first result stay above a real keyboard.
- One emulator journey goes from New trip to the step after Where.

## 2. Inputs to read (in this order)

### 2.1 This repo

1. **Plan and rules.**
   - `plan.md`: the sections listed in the header.
   - `AGENTS.md`: "Working rules", "Performance", "Tests" and "Traps". In particular:
     - rethrow `CancellationException`;
     - no measured size into state;
     - `liveRegion`, never `announceForAccessibility`;
     - debug code only in `src/debug`;
     - Conventional Commits with no trailer.
2. **Earlier phase briefs.**
   - `docs/phases/phase-2-design-system.md`, Tasks 7, 11, 12, 15 and 16:
     - `CfButton`, `CfIconButton` (`CfButtonSize.Touch`), `CfSkeleton`, `CfSpinner`;
     - `CfSegmentedButtons` (it fires its own `Selection` haptic);
     - `CfLinearProgress`, `CfModalBottomSheet`, `CfAlertDialog`/`CfDialogAction`;
     - the Konsist rule `@BoardMetrics`;
     - `LocalHaptics`/`RecordingHaptics`.
   - `docs/phases/phase-0c-strings.md`: resource names come from `tools/l10n/names.lock.json`; `CopyIndex.find(CopyTable.Wizard, key)`; the `android-only.json` schema (T2).
   - `docs/phases/phase-0e-test-harness.md`: profiles, `Guards`, `BoardCapture`, `ScreenTest`, `TestSeeds`.
   - `docs/phases/phase-4e-session.md` §5.7–§5.8 and T11: `Route.Wizard`, `WizardMode`, `RootNavigator`, `RoutePlaceholder`, `DebugSeedRouting`, `app_debug_route_x` (`core/ui/src/debug/res`, never named from `src/main`).
   - `docs/phases/phase-6a-shell-trips.md` §5.6–§5.7 and T11, and the merged `app/…/shell/AppShell.kt`, `AppShellScaffold.kt`, `ShellViewModel.kt`, `TabViewModelStoreOwner.kt`: `ShellNavState.overlay`, `push`, `back`, the overlay `Box`, `AppShell(root, onRootBack, placeholder)`, the New trip lambdas, and `RouteProbe` (root route only; it never reports the overlay).
   - `docs/phases/phase-5a-sign-in.md`: the IME-above-footer technique of `AuthScreen` (`feature/auth/…/shell/AuthScreen.kt`), and the keyboard-check pattern.
   - The 6a, 9a and 9b briefs, and their merged code.
3. **Research.** `docs/research/06-wizard.md`:
   - §2.1–§2.3 (board → state map);
   - §3.1–§3.4, §3.7 (spec, haptics);
   - §4.2 and §4.6 (steps, progress);
   - §4.7 (recovery);
   - §5.2 (route rules);
   - §7.1 (`GET /v1/airports`);
   - §9.2–§9.4, §9.6, §9.7, §9.9;
   - §12 (OQ 1, 2, 8, 10, 11).

### 2.2 Boards

The boards are read-only. Open the HTML in a browser to render them; the frame is 390 × 844.

**`DC/Wizard.dc.html`.**
- Props: `screen`, `v`, `pct`, `theme`, `lang`, `alert`.
- Lines 23–72: header, progress, main and the Where card.
- Lines 280–312: the Where row builder.
- Lines 505–520: the alert and main styles.
- Line 228: the `T` copy table.

**`DC/WizardSheet.dc.html`.**
- Props: `kind` (`airport` | `stop`) and `field` (`from` | `home` | `second`).
- Line 51: search field. Line 52: results.
- Line 87: copy and sample data.
- Lines 98–121: the per-kind backdrops (`K`) and the sheet style (`sheetStyle`, l.121: `top: 56px`, padding `10px 20px 34px`, gap 14).

**Wrappers.** Each is a `dc-import` of the two boards above.

| Board | State | Board pct |
|---|---|---|
| `WizardWhere`, `WizardWhereDark`, `WizardWhereNL` | `where` | 15 |
| `WizardWhereEmpty`, `WizardWhereEmptyDarkNL` | `where v=empty` | 8 |
| `WizardWhereSame` (no DarkNL) | `where v=same` | 15 |
| `WizardWhereStop`, `…DarkNL` | `where v=stop1` | 15 |
| `WizardWhereStops`, `…DarkNL` | `where v=stop2` | 15 |
| `WizardHomeRoute`, `…DarkNL` | `where v=home` | 50 |
| `WizardHomeRouteStop`, `…DarkNL` | `where v=homestop` | 50 |
| `WizardAirportFrom`, `…DarkNL` | sheet `airport field=from` over `where v=empty` | 15 |
| `WizardAirportHome`, `…DarkNL` | sheet `airport field=home` over `outbound v=retflight` | 55 |
| `WizardStopSearch`, `…DarkNL` | sheet `stop` over `where` | 15 |
| `WizardStopSearchSecond`, `…DarkNL` | sheet `stop field=second` over `where v=stop1` | 15 |
| `WizardStopSearchHome`, `…DarkNL` | sheet `stop field=home` over `where v=home` | 55 |
| `WizardDiscard`, `…DarkNL` | `outbound alert=discard` | 30 |

**Reference boards.**
- `DC/WizardCopy.dc.html`: the "Airports and search", "Accessibility", "Android" and "Layout" tables.
- `DC/WizardSpec.dc.html`: no drafts, quiet recovery, header, haptics.
- `DC/TripSearchStates.dc.html`: 375 wide, en only, light and dark.
- `DC/TripAirportSearch.dc.html`: an older search visual (§4).
- `DC/theme.css`: `.ios-navbar` line 187, `.ios-seg`, `.ios-alert`, the colour tokens.

### 2.3 iOS (`f391a1fa`)

**Docs and plan.**
- `Docs/wizard-fidelity.md`.
- `I/plan.md`: l.1791 "16a", l.1807 "16a–b board-fidelity follow-up", l.1822 "16b.2 Route first, then times", and l.2508 NH5 (the audience notice).

**`Kit/TripWizardUI/`.**
- `MemberWizardView.swift`: navigation stack, sheet, discard alert, `close()`.
- `MemberWizardSteps.swift`, l.10–120: `wizardShell`.
- `MemberWizardRouteView.swift`: the whole file.
- `WizardAirportSearchView.swift`.
- `MemberWizardReadyView.swift`, l.251–273: `readyBanner`, used for the audience notice.

**`Kit/TripWizard/Model/`.**
- `MemberWizardModel.swift`: `airportIsAllowed` l.152, `swapAirports` l.167, `chooseAirport`, `removeStop`, `makeDirect`, `discard`.
- `MemberWizardNavigation.swift`: `navigationPath`, `popNavigation`.
- `MemberWizardStore.swift`.
- `AirportSearchService.swift`.

**App.**
- `App/AppTabs.swift`: `openWizard`, recovery on session start, the seeds.
- `App/Debug/WizardBoardCaptureSeed.swift`.

### 2.4 API

`GET /v1/airports?query=&limit=` (`03-api-contract.md` §11, `06` §7.1) [F].
- Public, throttled at 60 requests per minute per IP.
- `query` is 2–60 characters; `limit` is 1–20 (default 8).
- Response: `{airports:[{iata, name, city?, country, timezone}]}`, where `country` is ISO 3166-1 alpha-2. `Cache-Control: public, max-age=3600`.
- A 429 carries `retryAfterSeconds` (default 60).
- The app reaches it only through 9b's `AirportSearchService`, which wraps 4b `DataSource.searchAirports(query, limit)`.
- The 4c mock has 8 airports: AMS, DXB, HND, JFK, LAX, LHR, SIN and SYD. It matches the query as a substring of the IATA code, name, city or country.

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| Outbound and flight-home **times**, the **Coming back?** step (`WizardOutbound*`, `WizardReturn*`, `WizardReturnFlight*`). In 9c these steps show the debug step seam (§5.3). | 9d |
| Sleep, Checking, questions, route **field errors from a server review** (`legs.i.fromIata`/`toIata` lines and their red outline; they need server copy formatting) | 9e |
| Fine-tune, Ready, Calculating, save, credits | 9f, 9g |
| Edit and Duplicate entry, "Discard your changes?" / "Discard changes" / "Your trip stays as it was." (`WizardDiscardEdit`). `Route.Wizard(Edit/Duplicate)` keeps rendering 6a's `placeholder(route)` in the overlay | 9h |
| Guest wizard, `WizardWhereGuest`, `WizardDiscardGuest` (guest alert body) | 11a |
| The airport count line "5 airports" and the older row style of `TripAirportSearch` (OQ-9c-4) | not built |
| Superseded `TripDiscard*` boards | never built |
| Baseline profile for wizard entry | 13 |

## 4. Settled values that override the raw boards [F] / [R]

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
| Board progress on sheet and alert backdrops (`WizardAirportFrom` 15 over the empty Where, `WizardAirportHome` and `WizardStopSearchHome` 55, `WizardDiscard` 30) | The model's value for the state shown: 8, 50, 50 and 15 | progress comes from 9a, never per board [R]; OQ-9c-6 |
| `WizardDiscard` backdrop = Outbound | In 9c the alert is captured over Where with something entered; 9d re-captures it over Outbound | Outbound is 9d [R]; OQ-9c-5 |
| `WizardAirportHome` backdrop = flight-home **times** | Captured over the flight-home route (`WizardHomeRoute`), which is where the To row opens it | iOS opens it from the route [F]; times are 9d |
| No audience notice on the Where boards | Built per iOS NH5, below the route card on Where (not on the home route) | iOS user decision, no-health-data [F `MemberWizardRouteView.swift` l.23–33]; OQ-9c-1 |
| Board CTA bottom 34 (iPhone home indicator) | Footer bottom padding is 12 dp above the larger of the safe bottom inset and the IME | iOS `wizardShell` footer `padding(.vertical, space3)` [F] |
| Board title/sub group `margin-bottom: 4` | The gap from title group to card is 14 + 4 = **18 dp** (iOS has 14) | board over iOS [R] |
| Stop-question text 17/22 at weight **600** (iOS draws 400) | `CfTheme.type.wizardButton` (Figtree600 17/22) | board over iOS [R] |

## 5. The spec

### 5.1 Shell geometry (`WizardMetrics`, `@BoardMetrics("Wizard","WizardCopy")`)

All y values are measured from `safeTop` (`LocalSafeInsets`). Never read `WindowInsets.safeDrawing` or `statusBars` in the feature: the 0e Konsist rule forbids it.

**Window and column.**
- The wizard fills 6a's overlay `Box` (the whole activity window). While `overlay != null`, `AppShellScaffold` does not compose the tab scaffold at all, so the tab bar is outside the semantics tree without extra work.
- **Insets.** The overlay reads the activity-level `LocalSafeInsets` (all four sides; `CfRoot` provides `WindowInsets.safeDrawing`). 6a re-provides a reduced `LocalSafeInsets` only inside the scaffold branch (`AppShellScaffold.kt` l.133–135); the overlay `Box` (l.160) is outside it. Keep it outside.
- Column [F WizardCopy]: when the window width is **≥ 669 dp**, the column is 520 dp wide and centred. Below 669 dp it fills the window width. The column holds the progress bar, the step content and the footer.
- The header spans the window, between `safeStart` and `safeEnd`.

**Header**, 54 dp high (tag `wizard.header`).
- Start/end padding is 10 dp. With 48 dp buttons, the glyph centres sit 34 dp from each edge, as on the board (12 + 22).
- Start slot: the back button (tag `wizard.back`), lucide `chevron-left` at 22 dp, `contentDescription` = Auth "Back"/"Terug" via `CopyIndex.find(CopyTable.Auth, "Back")`. The Wizard catalog has no "Back".
- Centre: the nav title (tag `wizard.nav-title`), `CfTheme.type.wizardNavTitle` (Figtree500 15/20), colour `mutedForeground`, one line with ellipsis. It is "New trip" / "Nieuwe reis" for Create and Duplicate, and "Edit trip" for Edit.
- End slot: × (tag `wizard.close`), lucide `x` at 18 dp, `contentDescription` "Close"/"Sluiten".
- An empty slot keeps its 48 dp width, so the title stays centred.

**Progress** (tag `wizard.progress`).
- `CfLinearProgress(progress = { shown })`.
- Top at `safeTop + 60`, height 4 dp, horizontal padding 20 dp inside the column.
- `shown` is `animateFloatAsState(uiState.progress, tween(250, easing = FastOutSlowInEasing))` [R], where `uiState.progress` is 9a's `Double` converted to `Float`. Under Reduce Motion (`LocalReduceMotion`, `core/designsystem/…/theme/ChronoFlowTheme.kt`, plan A9) it is `snap()`.
- The never-backwards floor is the model's (9a); the UI shows the model value.
- Semantics:
  - `contentDescription` = Wizard "Progress, {{pct}} percent" with `pct = (progress * 100).toInt()` (truncation, as iOS `String(Int(model.progress * 100))` [F `MemberWizardSteps.swift` l.23]) ("Progress, 15 percent" / "Voortgang, 15 procent");
  - `progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)`.

**Step content.**
- A `verticalScroll` column whose top is at `safeTop + 76` (12 dp below the progress bar), with content padding top 12 dp, horizontal 20 dp and bottom 12 dp; item gap 14 dp. So the title's top is at **`safeTop + 88`** [F board `mainStyle` l.516: `top: 76px; padding: 12px 20px 0`].
- The first item is the title group:
  - Column gap 6 dp, followed by an extra 4 dp, so the next item starts 18 dp below.
  - Title (tag `wizard.title`): `CfTheme.type.screenPageTitle` (Fraunces 400 30/36, −0.01 em). When the title is longer than 80 characters it uses `wizardLongTitle` (24/30). Colour `foreground`. `semantics { heading() }`.
  - Subtitle: `wizardSubtitle` (16/22), `mutedForeground`.
- On every step change, focus moves to the title [F WizardCopy "Accessibility"]: `FocusRequester` on a `focusable()` title, requested in `LaunchedEffect(stepKey)`.

**Footer** (tag `wizard.<id>.footer`).
- Outside the scroll; fixed.
- Padding: horizontal 20 dp, top 12 dp, bottom 12 dp above `max(safeBottom, imeBottom)`. Use the composition 5a's `AuthScreen` merged with (`imePadding` plus the safe bottom, with the IME excluded from the safe inset), so the button sits 12 dp above whichever is higher.
- The Continue button (tag `wizard.<id>.continue`):
  - `CfButton(style = Primary, fillWidth = true, minHeight = 52.dp, textStyle = CfTheme.type.wizardButton)`, pill-shaped;
  - when disabled it draws the primary container at **alpha 0.4** (`wizardDisabledOpacity`), not CfButton's 0.5. Add the parameter in Task 4 if `CfButton` has none.
  - it is enabled only when the step's rule holds **and** the wizard is not `closing`, not continuing, and not in 9a/9b's `persistenceFailed` or `reviewing` state if they expose them [F `MemberWizardSteps.swift` l.98–101]. Only the step rule changes the alpha.
- `<id>` is `where` or `return-route` (iOS ids).

**Step host.**
- An inner `NavDisplay` (Navigation 3; add `implementation(libs.androidx.navigation3.ui)` to `feature/wizard/build.gradle.kts`, which the feature convention does not include). Its back stack is derived from state: `listOf(WizardStepKey.Where) + navigationPath.map(::WizardStepKey)`. It is never pushed by hand.
- `onBack` calls `vm.back()` (§5.4).
- Transitions [R]:
  - forward: `slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }`;
  - pop and predictive pop: the reverse;
  - `tween(300)`; `EnterTransition.None` under Reduce Motion.
- Step keys rendered in 9c:
  - `Where` → `WhereStep(home = false)`;
  - `ReturnRoute` → `WhereStep(home = true)`;
  - every other key → `WizardStepSeam(key)` (§5.3).

**Keyboard.** The step column never moves the footer above the keyboard by hand. `imePadding` on the footer does it, and the scroll column gets the same bottom inset.

### 5.2 Where step (`WhereStep`, `WhereMetrics`, `@BoardMetrics("Wizard")`)

**Title and subtitle.**

| | Title | Subtitle |
|---|---|---|
| `home = false` | "Where are you flying?" / "Waar vlieg je heen?" | "Pick the airports from your booking." |
| `home = true` | "Your flight home" | "It leaves from where you landed. Add any stops on the way." |

All copy comes from the Wizard catalog.

**Content, in order** [F iOS `routeScreen`]:
1. the route card;
2. the audience notice (only when `home = false`);
3. the same-airport line (only when same);
4. the stop question, shown when the route is complete (`routeComplete(home)`, which is false when same) and not same [F board `ask`].

**Route card** (tag `wizard.route`).
- Shape `RoundedCornerShape(16)` (`wizardCardRadius`), fill `card`, 1 dp `border`, clipped.
- Rows are separated by a 1 dp `border` divider.
- Row order: From, then stops 1…n, then To. Route = `form.route(home)`.

**Row** (min 68 dp).
- Padding: top and bottom 10, start 16, end **64** when swap shows, else 12. The row's children have a gap of 10.
- A column with gap 2 (`wizardLabelGap`):
  - **Label**, `wizardHint` 13/18, `mutedForeground`. "From" / "Van" at position 0; "To" / "Naar" last. A stop is "Stop" / "Tussenstop" when the route has exactly 3 airports, else "Stop {{number}}" (number = position).
  - **Value.** When set: `"<IATA> · <city>"` (city omitted with its dot when null) in `wizardAirport` (600 20/26), `foreground`. When empty: "Choose an airport" in `wizardAirportPlaceholder` (400 20/26), `mutedForeground`.
  - **Hint**, `wizardHint`:
    - "From your last trip" in `dawnText`: at position 0 when `draft.state.mode == WizardMode.member` (9a's persisted `net.chronoflow.core.wizard.model.WizardMode`, not the route's `net.chronoflow.core.navigation.WizardMode`, which 9b imports as `WizardEntry`), not home, `state.fromLastTrip` and not same [F iOS `MemberWizardRouteView.swift` l.95]. The board also hides it on Duplicate (`v !== 'dup'`, `Wizard.dc.html` l.290); that holds because only a fresh Create draft has `fromLastTrip = true` (iOS `MemberWizardStore.swift` l.200);
    - "Where you landed" in `mutedForeground`: on the locked home From.
- Trailing:
  - a stop row has the **remove** button (tag `wizard.remove-stop.<position>`): a 36 dp circle filled `wizardPickerFill`, lucide `x` at 14 dp in `mutedForeground`, a 48 dp target, end padding 12 (`space3`), `contentDescription` "Remove stop in {{city}}" (city ?: IATA). Tap → `Light` haptic, then `vm.removeStop(start + position)` (the **route index**, `start = home ? returnBoundary : 0`) [F iOS l.106–108];
  - the locked home From has a 16 dp lucide `lock` in `mutedForeground`.
- The whole row (except the trailing button) is one `cfClickable(role = Button)` with tag `wizard.from`, `wizard.stop.<position>` or `wizard.to`.
- Merged `contentDescription` = `label + ", " + (empty ? "Choose an airport" : "$iata · ${city ?: ""}")` [F iOS l.200–215].
- **Locked row:** not clickable, `semantics { disabled() }`. `contentDescription` = "Leaves from {{city}}, where you landed. You can't change this." (city ?: IATA).
- **Tap** → `vm.openAirport(WizardAirportSheet(index = start + max(0, position − 1), side = …, replacingStop = isStop))`:
  - `start = home ? returnBoundary : 0`;
  - side: position 0 → `From`; a stop → `Stop`; the last row → `Home` when home, else `To` [F iOS row tap].

**Invalid outline.**
- When same, the **last** row draws a 2 dp `destructive` stroke (`AuthTokens.focusWidth`) inside its bounds.
- Its shape follows the card corners: bottom corners 16 for the last row; top corners 16 too if it is also row 0 [F iOS `UnevenRoundedRectangle`].
- It is drawn with `drawWithContent`, never with a layout-changing border.

**Swap** (tag `wizard.swap`).
- Shown only when `!home && route.size == 2`.
- Aligned to the card's end at an inset of 14 dp, vertically centred on the card.
- A 40 dp circle filled `wizardPickerFill` with the board's swap glyph (lucide `arrow-down-up` matches `m21 16-4 4-4-4`, `M17 20V4`, `m3 8 4-4 4 4`, `M7 4v16`) at 16 dp, `foreground`. 48 dp target.
- `contentDescription` "Swap airports".
- Disabled at **alpha 0.35** (`wizardEmptyOpacity`) when either airport is empty.
- Tap → `Light` haptic, then `vm.swapAirports()`.

**Audience notice** (tag `wizard.audienceNotice`) [F `MemberWizardReadyView.swift` l.251–273].
- A row with gap 10 (`wizardNoticeGap`) and padding 14 (`wizardContentGap`).
- Fill `wizardNoticeBackground`, 1 dp `wizardNoticeBorder`, radius 18 (`radiusXl`).
- Lucide `info` at 18 dp (`wizardNoticeIconSize`), `foreground`, hidden from accessibility.
- A column with gap 2:
  - title "ChronoFlow is intended for adults with a regular nightly sleep pattern. It does not offer a plan if you have recently done shift work, or do not usually sleep in one regular nightly episode." in `screenLabel`;
  - body "ChronoFlow is not suitable if you have bipolar disorder or have had a manic or hypomanic episode: sleep loss, bright light and long journeys can trigger mood episodes. Talk to your doctor before you travel." in `screenBody`.
- Both strings are Wizard catalog keys (nl present). `Modifier.semantics(mergeDescendants = true) {}`.

**Same-airport line** (tag `wizard.where.same`).
- A row with gap 6: lucide `circle-alert` at 16 dp in `destructive`, offset 2 dp down; then the text "From and To are the same airport. Choose where you land." in 14/20 `destructive`.
- 14/20 uses the generated `screen*` style that is Figtree400 14/20, or a `WhereMetrics.errorText` style if none exists.
- `semantics { liveRegion = LiveRegionMode.Polite }`.

**Stop question** (tag `wizard.route.question`).
- A column with gap 10 and top margin 4.
- The question "Do you change planes on the way?" in `wizardButton` (600 17/22) with 4 dp horizontal padding.
- `CfSegmentedButtons(options = ["No, direct", "Yes"], selectedIndex = if (stops > 0 || sheet?.side == Stop) 1 else 0, testTag = "wizard.route.stops")`, fillMaxWidth, height ≥ 44 dp. `stops = max(0, route.size − 2)` [F iOS l.138].
- The segmented group's `contentDescription`/label is the question.
- `onSelect(1)` with no stops and `canAdd` → `vm.openAirport(WizardAirportSheet(index = last, side = Stop))`. Here `last = home ? legs.size − 1 : returnBoundary − 1` and `canAdd = legs.size < 16`.
- `onSelect(0)` → if stops > 0, a `Light` haptic, then `vm.makeDirect(home)`.
- "Yes" is disabled when `!canAdd && stops == 0` (iOS `.disabled(!canAdd && stops == 0)` l.158): `StopQuestionUi.yesEnabled = false`; render that segment disabled if `CfSegmentedButtons` supports per-option `enabled`, and in every case ignore `onSelect(1)`. This state is reachable: an outbound direct flight plus a 15-leg flight home is 16 legs with 0 outbound stops.

**Below the question.**
- When `!canAdd`: the line (tag `wizard.route.limit`) "That's the most we can plan: 16 flights." in `wizardHint`, `mutedForeground`, padding horizontal 4, top margin −4.
- Else, when stops > 0: "Add another stop" (tag `wizard.add-stop`), start-aligned.
  - A plus glyph (lucide `plus`, 16 dp) with gap 6, `dawnText`, in `wizardFieldLabel` (400 15/20).
  - Min height 48, horizontal padding 4.
  - Tap → `vm.openAirport(WizardAirportSheet(index = last, side = Stop))`.

**Continue.** Enabled = `routeComplete(home)`. Click → `vm.continueFrom(stepKey)`, which calls `finishRoute(home)` only if the current step still equals the step the button belongs to (the double-tap guard; iOS `continuing`).

**Haptics** [F WizardSpec §3.7, iOS] through `LocalHaptics`:

| Action | Haptic |
|---|---|
| Swap | `Light` |
| Remove stop | `Light` |
| "No, direct" with stops | `Light` |
| Choosing a stop in the sheet | `Light` |
| Choosing a From, To or Home airport | `Selection` |
| Segmented change | `Selection` (from `CfSegmentedButtons`) |

### 5.3 Step seam (`WizardStepSeam`)

- `…/wizard/shell/WizardStepSeam.kt` declares `@Composable fun WizardStepSeam(key: WizardStepKey)` as an `expect`-like seam with two implementations:
  - **debug** (`feature/wizard/src/debug/…`): centred `Text(stringResource(net.chronoflow.core.ui.R.string.app_debug_route_x, key.debugName))` ("Debug route: %1$s" / "Debugroute: %1$s", the existing 0c/3b debug-only resource in `core/ui/src/debug/res`; 4e forbids adding `app_debug_route` or any new string), with tag `wizard.seam.<debugName>`. The debug names are lowerCamel, one per 9a `MemberWizardDestination`: `outbound`, `returnChoice`, `returnFlight`, `sleep`, `checking`, `question<i>` (`question(i)`), `excluded`, `continuation`, `fineTune`, `ready`, `calculating`. The resource may be named only from `src/debug`.
  - **release**: an empty full-size `Box`.
- The seam step still shows the shell: header, progress and back. Back works.
- The footer is hidden on a seam.

### 5.4 Back, close and discard (`WizardBackHandling`)

| State | System or predictive back | × |
|---|---|---|
| A sheet is open | The sheet consumes it: Cancel (`vm.cancelAirport()`), no write | not reachable (behind the scrim) |
| The discard dialog is open | The dialog consumes it: Keep editing | not reachable |
| First screen (9a `firstScreen`: Where, not home; OQ-A353) | Same as × | `hasChanges` → dialog; else close |
| Any later step (home route, seams) | The inner `NavDisplay` pops: `vm.back()` → `popNavigation(path.dropLast(1), path)` | same as above |

- **First-screen back.** A `BackHandler(enabled = isFirstScreen && sheet == null && !dialogVisible)` at the wizard root calls `vm.requestClose()`. It is not predictive: the screen does not preview leaving.
- **`vm.requestClose()`**: if `closing`, ignore. If `hasChanges` (Create: `entered`), set `discardDialogVisible = true`. Otherwise call `close()`.
- **`close()`**:
  - set `closing = true`;
  - 9b's `TripWizardViewModel.discard()` (which calls 9a `MemberWizardModel.discard()`: cancel jobs, flush, delete the recovery row, close the writer; it rethrows on failure after resetting `closed`, and is a no-op on `calculating`);
  - then `onExit(WizardExit.Trips)` exactly once.
  - Run it in the ViewModel scope. Rethrow `CancellationException`. If `discard()` fails with any other exception, set `closing = false`, stay on screen and log it (iOS resets `closing` and asserts).
  - [F iOS `MemberWizardView.swift` l.295–307: guarded, `await model.discard()`, `dismiss()`; on failure `closing = false`.]
- **Discard dialog** (Phase 2's `CfAlertDialog` takes no `modifier`, so there is no `wizard.discard` tag; its buttons carry `cf.dialog.confirm` and `cf.dialog.dismiss` [F `CfDialogs.kt`]):
  - `CfAlertDialog(title = "Discard this trip?", text = "What you entered is not saved.", confirm = CfDialogAction("Discard", destructive = true) { vm.confirmDiscard() }, dismiss = CfDialogAction("Keep editing") { vm.dismissDiscard() }, onDismissRequest = vm::dismissDiscard)`;
  - nl: "Deze reis weggooien?", "Wat je hebt ingevuld, wordt niet bewaard.", "Weggooien", "Verder bewerken".
  - `discardDialogVisible` is **not** persisted: recovery never reopens the dialog.
  - `confirmDiscard()` = hide the dialog, then `close()`.
- **Exit target.** `WizardExit.Trips` → `ShellViewModel` clears the overlay (6a `ShellNav.back()` with `overlay != null`), which shows the tab the wizard was opened over: My trips after New trip [F iOS `dismiss()` sets `wizard = nil`; board `goHref: 'Trips.dc.html'`; OQ-9c-10]. 9g uses 6a's `wizardSaved`; 9h and 11a add their exits.
- **No back on Where** (Create) [F board §3.4, OQ-A36]. Back shows on `ReturnRoute` (it pops to `returnChoice`, a seam in 9c) and on every seam step.

### 5.5 Airport and stop search sheet (`AirportSearchSheet`, `SheetMetrics`, `@BoardMetrics("WizardSheet","TripSearchStates")`)

- **Driven by state.** `uiState.sheet: AirportSheetUi?` comes from `draft.state.sheet`, which 9b persists, so recovery reopens the sheet. It is composed only while non-null.
- **Container.**
  - `CfModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = vm::cancelAirport, modifier = Modifier.padding(top = safeTop + 56.dp))`, with content `fillMaxHeight()`. The board's full sheet sits at `top: 56px`.
  - If M3 ignores the top padding on the sheet surface, accept M3's full expansion and record the measured top in `docs/wizard.md`.
  - Tag `wizard.airport-search`.
- **Content column.** Padding horizontal 20, top 0 (below the drag handle), bottom = `max(safeBottom, imeBottom)`; gap 14.
- **Header** (min height 48).
  - Three columns `1fr auto 1fr`.
  - Start: "Cancel" / "Annuleren" (tag `wizard.airport.cancel`), `wizardRow` 17/22 in `dawnText`, a 48 dp text button.
  - Centre: the title, `wizardButton` (600 17/22), max width 220 dp, centred, up to 2 lines, `heading()`.
  - End: empty (no Done).
- **Titles.**

  | Side | en | nl |
  |---|---|---|
  | `From` | "Where do you fly from?" | "Waar vlieg je vandaan?" |
  | `To` | "Where do you fly to?" | "Waar vlieg je naartoe?" |
  | `Home` | "Where do you fly home to?" | "Waar vlieg je naar huis?" |
  | `Stop` | "Where's your stop?" | "Waar is je tussenstop?" |

- **Caption** (stops only; tag `wizard.airport.caption`).
  - "Between {{from}} and {{to}}" in `wizardHint` `mutedForeground`, top margin −6.
  - `from = legs[index].fromCity ?: fromIata`.
  - `to = (if (replacingStop) legs[index + 1] else legs[index]).toCity ?: toIata` [F `WizardAirportSearchView.swift`].
- **Search field** (tag `wizard.airport.query`).
  - A 48 dp band that focuses the field on tap, holding a 40 dp box:
    - fill `wizardPickerFill`, radius 12, horizontal padding 12, gap 8;
    - lucide `search` at 18 dp in the `rgba(120,120,128,0.8)` token (use the generated colour with that value if it exists, else `mutedForeground`, and record it);
    - `BasicTextField(state = queryState)` in `wizardRow` (17 sp), cursor `SolidColor(dawn)`;
    - placeholder "Airport or city" / "Luchthaven of stad" (Android-only, §5.7) in `mutedForeground`.
  - `KeyboardOptions(capitalization = None, autoCorrectEnabled = false, keyboardType = Text, imeAction = Search)`.
  - `onKeyboardAction` hides the keyboard.
  - `inputTransformation = InputTransformation.maxLength(60)`.
  - `queryState = rememberTextFieldState()` keyed by the sheet id. The query is not secret; process death reopens the sheet with an empty field (iOS keeps the query in `@State`).
  - The field requests focus and shows the keyboard once the sheet's `currentValue == Expanded`.
- **Query flow.** `snapshotFlow { queryState.text.toString() }` feeds `vm.onQuery(it)` from a `LaunchedEffect(sheetId)`.
- **State area** (fills the rest; tag `wizard.airport.state.<name>`). Exactly one of the following:

  | `AirportSearchUi` | When | Content |
  |---|---|---|
  | `Hint` | trimmed query has 0–1 characters | "Type at least 2 letters to search." in `wizardHint` `mutedForeground`, top margin −6 (8 dp below the field box). Tag `wizard.airport.hint`. |
  | `Searching` | a search for the current query is in flight, including the 350 ms debounce and the 1.02 s pacing wait | A status row with gap 8: `CfSpinner(16.dp)` and "Searching airports…" in 14/20 `mutedForeground`, `liveRegion = Polite`. 16 dp below it, 3 × `CfSkeleton(Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(14))` with gap 10. Tag `wizard.airport.searching`. |
  | `Results(list)` | ≥ 1 allowed airport | The results card (below). Tag `wizard.airport.results`. |
  | `Empty` | the service returned 0 airports, or all were filtered out by `airportIsAllowed` | A dashed card: 2 dp dashed `border` (dash 6, gap 4, drawn with `drawBehind`), radius 22, padding 20 × 12, centred column with gap 8. Title "No airports found" (16/24 600) and body "Try the city name, like “Tokyo”, or the 3-letter code on your ticket, like “NRT”." (14/20 `mutedForeground`, centred). `liveRegion = Polite`. Tag `wizard.airport.empty`. |
  | `Failed(retryAt: Instant?)` | offline, a server error or any other failure; `retryAt != null` when throttled | A card with padding 16, radius 22, 1 dp `border`, fill `surface2`, column gap 12. First a row with gap 8: lucide `circle-alert` at 16 dp in `destructive`, offset 2 dp down; then 14/20 text, `buildAnnotatedString`: **"We couldn't search airports."** (600) + " " + "Check your connection and try again.". Then "Try again" / "Opnieuw proberen": `CfButton(style = Outline, size = Lg, fillWidth = true)`, tag `wizard.airport.retry`, enabled only when `retryAt == null || now ≥ retryAt`, which `vm` re-evaluates with a single `delay` until `retryAt`. The card has `liveRegion = Polite`. Tag `wizard.airport.error`. |

- **Results card.**
  - Radius 16, 1 dp `border`, fill `background`, clipped.
  - A `LazyColumn` with `key = { it.iata }` and `Modifier.weight(1f, fill = false)`. Rows are separated by 1 dp `border` dividers.
- **Result row** (tag `wizard.airport.<IATA>`).
  - Min height 60, padding 8 vertical × 16 horizontal, gap 12.
  - Code: 44 dp wide, `wizardSearchCode` (700 15/20, tracking 0.6).
  - A column: name in `wizardSearchName` (16/21) `foreground`; place in `wizardHint` `mutedForeground`.
  - `place = listOfNotNull(city, countryName).joinToString(", ")`, where `countryName = Locale("", country).getDisplayCountry(appLocale)`. The app locale is `LocalConfiguration.current.locales[0]` (coerced to en/nl by `ConfigurationPolicy`; the pattern of `AppShell.kt` l.50); never `Locale.getDefault()`. Put the formatting in a pure `airportPlace(airport, locale)` function so it is JVM-testable.
  - `cfClickable(role = Button)`; the texts are merged.
- **Selecting.**
  - Guard: the first selection wins (`selected` flag, iOS).
  - Haptic: `Light` for `side == Stop`, else `Selection`.
  - `scope.launch { sheetState.hide() }.invokeOnCompletion { vm.chooseAirport(airport, sheet) }`.
- **Cancel.** `sheetState.hide()`, then `vm.cancelAirport()` (= `openAirport(null)`). Swipe-down, a scrim tap and back reach `onDismissRequest`, which is `vm.cancelAirport()`.
- **Filtering.** Results pass 9a's `airportIsAllowed(iata, sheet)` [F `MemberWizardModel.swift` l.152–165]; the UI never re-implements it. For reference:
  - a `sheet.index` outside `legs` → never allowed;
  - a new stop (`side == Stop`, not replacing) must not equal either of the last two airports of the route;
  - the home From (position 0 on the home route) is never allowed;
  - on a 2-airport route every airport is allowed (so From = To is possible and shows the same-airport line);
  - otherwise an airport must not equal a neighbour of its position.

### 5.6 Search state in the ViewModel

9b already owns the search pipeline: `TripWizardViewModel.search: AirportSearchSession` (`:core:wizard` `net.chronoflow.core.wizard.model`; `setQuery(raw)`, `retry()`, `state: StateFlow<AirportSearchState>`, built on `viewModelScope` with `stateIn(Eagerly, Hint)`; 9b brief §6.4). It also maps a query over 60 code points to `Empty`, and a `DataFailure.Throttled` to `Throttled(query, now + retryAfterSeconds)`. It owns the trim, the 2–60 code-point rule, the single 350 ms debounce, the ≥ 1.02 s pacing, the 429 wait, the LRU cache, stale-search cancellation (`transformLatest`) and `CancellationException` rethrow. **9c does not build a second pipeline and never adds `debounce`** (9b rule 7: it would make 700 ms).

- `vm.onQuery(text)` calls `search.setQuery(text)`; `vm.retrySearch()` calls `search.retry()` (9b already ignores it while throttled and `now < retryAt`).
- `searchState: StateFlow<AirportSearchUi>` = `combine(search.state, sheet)` mapped as follows, then `stateIn(viewModelScope, WhileSubscribed(5_000), Hint)`:

  | 9b `AirportSearchState` | `AirportSearchUi` |
  |---|---|
  | `Hint` | `Hint` |
  | `Searching(query, stale)` | `Searching` (status row and skeletons; `stale` is not shown, OQ-9c-11) |
  | `Results(query, airports)` | `airports.filter { wizard.airportIsAllowed(it.iata, sheet) }` → `Results(rows)` if non-empty, else `Empty` |
  | `Empty(query)` | `Empty` |
  | `Failed(query)` | `Failed(retryAt = null)` |
  | `Throttled(query, retryAt)` | `Failed(retryAt)` |

- Opening a sheet and closing it (choose, Cancel, swipe, back) both call `search.setQuery("")`, so the state returns to `Hint` and 9b's `transformLatest` cancels any in-flight search. (The session is `Eagerly` on `viewModelScope`; it does not stop when the sheet leaves composition.)
- If 9b's merged names differ (for example the session lives elsewhere), use 9b's names and note it in the PR; keep the mapping above.

### 5.7 Copy

- **Catalog keys.** All copy is from the Wizard catalog via generated resources: `CopyIndex.find(CopyTable.Wizard, key)` for keys held in the model, and `stringResource(R.string.<name from names.lock.json>)` in UI code. "Back" comes from the Auth table. "Try again" and "Cancel" are Wizard keys (verified: "Opnieuw proberen", "Annuleren").
- **Android-only copy.** New entries in `tools/l10n/android-only.json`, all with `review: "needs review"`:

  | Name | en | nl | Replaces | Source |
  |---|---|---|---|---|
  | `android_wizard_airport_field` | Airport or city | Luchthaven of stad | wizard "Search airports" | WizardCopy |
  | `android_wizard_search_hint` | Type at least 2 letters to search. | Typ minstens 2 letters om te zoeken. | — | TripSearchStates; nl proposed |
  | `android_wizard_search_searching` | Searching airports… | Luchthavens zoeken… | — | TripSearchStates; nl web `lang/nl.json` |
  | `android_wizard_search_empty_title` | No airports found | Geen luchthavens gevonden | — | TripSearchStates; nl web (without the period, as the board) |
  | `android_wizard_search_empty_body` | Try the city name, like “Tokyo”, or the 3-letter code on your ticket, like “NRT”. | Probeer de naam van de stad, zoals “Tokio”, of de code van 3 letters op je ticket, zoals “NRT”. | — | TripSearchStates; nl proposed |
  | `android_wizard_search_error_title` | We couldn't search airports. | We konden geen luchthavens zoeken. | — | TripSearchStates; nl web |
  | `android_wizard_search_error_body` | Check your connection and try again. | Controleer je verbinding en probeer het opnieuw. | — | TripSearchStates; nl proposed |

- **Dutch terms.** Dutch uses "luchthaven", never "vliegveld" [F WizardCopy].
- Add the same rows to `plan.md` "Android-only copy awaiting approval".

### 5.8 Entry, quiet recovery and seeds (`:app`)

**Entry** (6a overlay; 6a brief §5.6–§5.7).
- 6a's `ShellViewModel.push(Route.Wizard(mode))` sets `ShellNavState.overlay`, and `AppShellScaffold` (l.124–161) then composes, **instead of** the tab scaffold, a full-screen `Box` that calls `overlay(currentOverlay)`, which `AppShell` (l.97) routes to `placeholder(route)` (`RoutePlaceholder(it, LaunchPlatform.routeLabel)`, from `CfRoot`). 9c changes only that overlay lambda in `AppShell`: `Route.Wizard(WizardMode.Create)` renders `WizardRoot`; `Edit` and `Duplicate` (also pushed by the trip page's `onWizard`, `AppShell.kt` l.86) keep `placeholder(route)` until 9h.
- The overlay enters with `slideInVertically { it }` and leaves with `slideOutVertically { it }`, `tween(300)`, no animation under `LocalReduceMotion` [R]. Because 6a's `if (overlay == null) scaffold else overlay` drops the scaffold at once, wrap that branch in `AnimatedContent(targetState = currentOverlay, contentKey = { it != null })` so the outgoing scaffold stays composed for the transition; keep 6a's `saveable`, tab entries and `LocalViewModelStoreOwner` providers exactly as they are.
- **ViewModel scope.** The wizard's `TripWizardViewModel` lives in a `ViewModelStoreOwner` owned by `ShellViewModel` for the overlay (`overlayOwner`, a second `TabViewModelStoreOwner`, provided as `LocalViewModelStoreOwner` around the overlay). `ShellViewModel` clears its store whenever a state change sets `overlay` from non-null to null (and in `onCleared`), so a later New trip never reuses a closed wizard's ViewModel; clearing runs `TripWizardViewModel.onCleared()` (writer close).
- **Accessibility.** Nothing to add: the tab scaffold is not composed while the wizard shows (above) [F `AppShellScaffold.kt`].
- `WizardRoot(viewModel, onExit)` takes a ViewModel that `AppShell` creates in the overlay owner with 9b's `TripWizardViewModel.factory(mode)` and `MutableCreationExtras(owner.defaultViewModelCreationExtras).apply { set(TripWizardDependenciesKey, deps) }`, the same pattern as the Trips tab (`AppShell.kt` l.62–73), where `deps = sessionGraph?.wizard` (a null `deps` makes 9b's VM `LoadFailed`). Add `ShellViewModel.closeWizard(exit: WizardExit)`: for `WizardExit.Trips` it calls `back()` while the overlay is a `Route.Wizard` (which clears the overlay). `WizardExit` lives in `:feature:wizard`; `:app` already depends on it.
- **9b's non-active states** have no board. `Loading`: draw only the `background` fill (iOS presents the wizard only after `start()` returns, `AppTabs.swift` l.515–538). `LoadFailed`: the ViewModel emits `WizardExit.Trips` once (through the same once-only exit path as `close()`) with no message and logs it (iOS never presents on a start failure and asserts in debug). `Closed`: the same exit (9g replaces this with `wizardSaved` when `createdTripId != null`).
- 6a's overlay `BackHandler(enabled = currentOverlay != null, onBack = onBack)` (`AppShellScaffold.kt` l.165) is composed after the overlay content, so it registers last and wins over the wizard's handlers. While `WizardRoot` is shown, disable it: `enabled = currentOverlay != null && !(currentOverlay is Route.Wizard && currentOverlay.mode == WizardMode.Create)`, so §5.4 decides.

**New trip** [F iOS `openWizard` `openingWizard` guard].
- 6a already wires New trip as `{ holder.push(Route.Wizard(WizardMode.Create)) }` in two places of `AppShell.kt` (l.58, signed-out `TripsContent`; l.74, `TripsScreen`; tags `trips.create`, `trips.create-empty`). 9c routes both through `WizardLauncher.open()`, which returns at once when `holder.state.overlay != null` or when an open is pending (an `AtomicBoolean` reset when `overlay` changes).
- Two taps within one frame give one open.

**Quiet recovery** [F `AppTabs.swift` l.295–311; WizardSpec "Quiet recovery"].
- `WizardRecoveryCheck` (`…/app/wizard/`, JVM-testable) has `suspend fun shouldOpen(epoch: Long): Boolean`. It returns true at most once per session epoch, never while a wizard board capture is seeded (`DebugSeedRouting.current?.wizardBoardCapture`, read from `src/main` through a new `LaunchPlatform` getter: debug returns it, `app/src/release/…/LaunchPlatform.kt` returns null), and only when 9b's store has a live row. 9b exposes no `peek()`: take `suspend () -> Boolean` = `{ (session.wizard as? SessionTripWizardDependencies)?.store?.recover() != null }`. `MemberWizardStore.recover()` already runs on its injected IO dispatcher and deletes expired or undecodable rows. Inject `AppDispatchers` anyway for the check's own work.
- `AppShell` (which exists only on app-root routes, never on auth, terms or walkthrough) runs `LaunchedEffect(sessionGraph?.epoch) { val epoch = sessionGraph?.epoch ?: return@LaunchedEffect; if (check.shouldOpen(epoch)) launcher.open() }` (`SessionGraph.epoch: Long`).
- The ViewModel then restores the step, the values and the open sheet (9b `start()` = recover ?: initial draft), with no message.

**Seeds** (debug only; read through 4e `DebugSeedRouting.current: TestSeed?`, whose parsed fields `wizardAutoOpen: Boolean` and `wizardBoardCapture: String?` already exist in `app/src/debug/…/TestSeed.kt` l.47–48 and are covered by `TestSeedSessionKeysTest`; do not re-parse the extras).
- `CF_WIZARD_AUTO_OPEN=1`: on the shell's first composition in an epoch, call `WizardLauncher.open()` once (iOS opens it after session start).
- `CF_WIZARD_BOARD_CAPTURE=TripActions` is the trip page's existing seed (`LaunchPlatform.initialTripSheet`, `TripSheetsCaptureTest`). Only names that `WizardBoardCaptureSeed` knows open the wizard; `TripActions` and other non-`Wizard*` names are ignored here (no warning).
- `CF_WIZARD_BOARD_CAPTURE=<board>`:
  - install a debug `WizardStartOverride` that returns `WizardBoardCaptureSeed.draft(board, locale)` instead of `start()`;
  - disable recovery writes (an in-memory store);
  - set a fixed clock of 2026-10-01T09:00Z;
  - replace airport search with `FixedAirportSearch(board)`;
  - then open the wizard.
  - An unknown `Wizard*` board name logs one debug warning and opens a normal wizard.
- The seam for the override is a nullable `WizardStartOverride` in `AppGraph` (debug sets it; release leaves it null). It returns a debug `TripWizardDependencies` that wraps the session's (`SessionGraph.wizard`): `start()` = the seed draft, `store` = `InMemoryWizardDraftStore`-style in-memory store (debug source, not `:core:testing`), `now` = the fixed clock, `airportSearch` = `AirportSearchService({ FixedAirportSearch.results(board, it) }, now, sleep = {})`. `AppShell` uses it in place of `sessionGraph.wizard` when non-null. Extend `app/src/testRelease/…/ReleaseReadinessTest.kt` to assert it is null and that no class named `*BoardCapture*` or `FixedAirportSearch` is on the release classpath (its `missing(name)` helper).

**`WizardBoardCaptureSeed`** (`…/wizard-debug/`) [F port of `App/Debug/WizardBoardCaptureSeed.swift`].
- **Board name.** The seed strips a trailing `DarkNL`, `Dark` or `NL` before the lookup; the suffix only sets theme and language (§Task 10). iOS matches exact base names (`board == "WizardWhere"`), so an unstripped `WizardWhereDark` would lose the last-trip hint.
- **Base** [F l.10–37]. AMS → NRT, with city "Tokio" in nl. `airports` = AMS (Amsterdam Airport Schiphol, Amsterdam, NL, Europe/Amsterdam), NRT (Narita International, Tokyo, JP, Asia/Tokyo), DXB (Dubai International, Dubai, AE, Asia/Dubai), BKK (Suvarnabhumi, Bangkok, TH, Asia/Bangkok). Times 2026-10-12T14:40 / 2026-10-13T09:15. `entered = true`, `fromLastTrip = false`, `sleepNeeded = false`, profile `habitual_bed 23:30`, `habitual_wake 07:15`, `chronotype intermediate`.
- The rows marked [R] below (open sheets, queries, the dialog) are Android additions: iOS's seed sets no sheet, query or dialog for these boards.

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

- **Query.** The seed's query is typed into the field by the capture harness: the seed returns it, and the sheet fills `queryState` once.
- **`FixedAirportSearch`** returns `Results` immediately for the board query, with ISO countries:
  - "Am": AMS Amsterdam Airport Schiphol (Amsterdam, NL), AMM Queen Alia International (Amman, JO), AMA Rick Husband Amarillo International (Amarillo, US), AMD Sardar Vallabhbhai Patel International (Ahmedabad, IN);
  - "Du": DXB Dubai International (Dubai, AE), DWC Al Maktoum International (Dubai, AE), DUS Düsseldorf Airport (Düsseldorf, DE), DUB Dublin Airport (Dublin, IE);
  - "Ban": BKK Suvarnabhumi Airport (Bangkok, TH), DMK Don Mueang International (Bangkok, TH).

  The board place strings ("Amsterdam, Netherlands" / "Amsterdam, Nederland", "Dubai, Verenigde Arabische Emiraten" …) must come out of the `Locale` country names. A mismatch is a finding.

## 6. Kotlin shape [R]

| Class | Module and package | Notes |
|---|---|---|
| `WizardUiState(chrome: WizardChromeUi, step: WizardStepKey, where: WhereUi?, sheet: AirportSheetUi?, discardDialogVisible: Boolean)` | `:feature:wizard` `…wizard.ui` | `@Immutable`. Derived with `map`/`stateIn`; one `StateFlow` per slice (`chrome`, `where`, `sheet`, `searchState`), so typing does not recompose the route card. |
| `WizardChromeUi(navTitle: NavTitle, showBack: Boolean, showClose: Boolean, progress: Float, footerId: String?, continueEnabled: Boolean)` | same | `NavTitle { NewTrip, EditTrip }`; `showBack` = 9a `showsBack`; `progress` = 9a `progress.toFloat()`; also expose `progressPercent: Int` (`(progress * 100).toInt()`) so the description is JVM-tested |
| `WhereUi(home: Boolean, rows: List<RouteRowUi>, showSwap: Boolean, swapEnabled: Boolean, same: Boolean, showNotice: Boolean, question: StopQuestionUi?, limitReached: Boolean, showAddStop: Boolean)` | same | `StopQuestionUi(selected: Direct/WithStops, yesEnabled: Boolean, addStopSheet: WizardAirportSheet)` |
| `RouteRowUi(position: Int, kind: From/Stop/To, label: RowLabel, iata: String?, city: String?, hint: RowHint?, locked: Boolean, removable: Boolean, invalid: Boolean, sheet: WizardAirportSheet?)` | same | `RowLabel { From, To, Stop, StopN(n) }`; `RowHint { LastTrip, WhereYouLanded }` |
| `AirportSheetUi(sheet: WizardAirportSheet, side, caption: Pair<String, String>?, seedQuery: String?)` | same | |
| `AirportSearchUi` sealed: `Hint`, `Searching`, `Results(List<AirportRowUi>)`, `Empty`, `Failed(retryAt: Instant?)` | same | Mapped from 9b's `AirportSearchState` (§5.6). `AirportRowUi(iata, name, city, country)`; the place string is formatted in UI with the app locale |
| `WizardStepKey` | `…wizard.shell` | `@Serializable` `NavKey`: `Where`, plus one per 9a `MemberWizardDestination` (`outbound` … `calculating`, `question(index)`, including `continuation`) |
| `WizardScreen(state: WizardUiState, onEvent: (WizardEvent) -> Unit)` and `WizardEvent` | `…wizard.shell` | The stateless screen (AGENTS "Tests" 1). `WizardRoot` only collects the ViewModel and forwards events; every Robolectric test renders `WizardScreen` with a state derived from a seed draft, never the ViewModel, `AppShell` or a graph |
| `WizardExit { Trips }` | `…wizard.shell` | 9h and 11a add cases |
| `TripWizardViewModel` additions | `…wizard` (9b scaffold, `net.chronoflow.feature.wizard`) | `openAirport`, `cancelAirport`, `chooseAirport`, `swapAirports`, `removeStop(routeIndex)`, `makeDirect(home)`, `continueFrom(step)`, `back()`, `requestClose()`, `confirmDiscard()`, `dismissDiscard()`, `onQuery`, `retrySearch`; each delegates to the 9a/9b model (`wizard`, `search`) and guards on `closing`. Keep 9b's `state` and its existing tests unchanged |
| `WizardRoot`, `WizardScaffold`, `WizardHeader`, `WizardFooter`, `WizardStepHost`, `WizardStepSeam`, `WizardMetrics` | `…wizard.shell` | |
| `WhereStep`, `RouteCard`, `RouteRow`, `SwapButton`, `AudienceNotice`, `StopQuestion`, `WhereMetrics` | `…wizard.where` | |
| `AirportSearchSheet`, `AirportSearchField`, `AirportSearchStates`, `AirportResultRow`, `SheetMetrics` | `…wizard.search` | |
| `WizardLauncher`, `WizardRecoveryCheck` | `:app` `net.chronoflow.app.wizard` | JVM-testable; inject `AppDispatchers` |
| `ShellViewModel.closeWizard(exit)`, `ShellViewModel.overlayOwner`, the overlay lambda in `AppShell`, the `AnimatedContent` and `BackHandler` condition in `AppShellScaffold` | `:app` `net.chronoflow.app.shell` (6a files, edited) | §5.8 |
| `WizardBoardCaptureSeed`, `FixedAirportSearch`, debug `WizardStepSeam` | `feature/wizard/src/debug` | debug only |
| `WizardDebugSeeds` (reads `DebugSeedRouting`, sets `AppGraph.wizardStartOverride`) | `app/src/debug` `net.chronoflow.debug` | debug only |

## 7. Tasks

Each task is one commit: Conventional Commits, lower-case imperative, **no trailer**. Tick the plan box named in brackets in the same commit. Tests live in `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/…` unless a task says otherwise.

### Task 1: Android-only search copy (`feat(l10n): add android-only airport search copy`)

**Change.**
- Add the 7 entries of §5.7 to `tools/l10n/android-only.json`, each with a `comment` naming OQ-9c-2 or OQ-9c-3.
- Regenerate with the 0c generator and commit the output.
- Add the rows to `plan.md` "Android-only copy awaiting approval".

**Test.** Robolectric `WizardSearchCopyTest` (`@Logic`, `feature/wizard`). It resolves resources and `CopyIndex` entries only; it composes nothing and never looks for copy on a screen:
- every `android_wizard_*` name resolves in en and nl to the exact §5.7 text;
- `CopyIndex.find(CopyTable.Wizard, k)` resolves in en and nl for each catalog key 9c uses: "New trip", "Close", "Continue", "Progress, {{pct}} percent", "Where are you flying?", "Pick the airports from your booking.", "From", "To", "Stop", "Stop {{number}}", "Choose an airport", "From your last trip", "From and To are the same airport. Choose where you land.", "Swap airports", "Do you change planes on the way?", "No, direct", "Yes", "Add another stop", "That's the most we can plan: 16 flights.", "Remove stop in {{city}}", "Your flight home", "It leaves from where you landed. Add any stops on the way.", "Where you landed", "Leaves from {{city}}, where you landed. You can't change this.", the four sheet titles, "Between {{from}} and {{to}}", "Cancel", "Try again", "Discard this trip?", "What you entered is not saved.", "Keep editing", "Discard", and the two audience-notice strings;
- `CopyIndex.find(CopyTable.Auth, "Back")` resolves.
- nl spot checks: "Waar vlieg je heen?", "Stap je onderweg over?", "Deze reis weggooien?", "Voortgang, 15 procent".

### Task 2: Where UI state and route intents (`feat(wizard): map the route to where ui state`) [ticks "Where" with Task 6]

**Change.**
- §6 UI types.
- The derivation `WhereUi.from(draft, mode)` follows §5.2 exactly: label rule, hint rule, swap rule, same rule, `canAdd` (`legs.size < 16`), the question rule.
- `WizardChromeUi.from(...)`: `showBack = navigationPath.isNotEmpty()`; `showClose = step != calculating`; `progress` = the 9a value.
- The VM intents of §6 with the `closing` guard and the `continueFrom` step guard.

**JVM tests** (`runTest(timeout = 60.seconds)`, `InMemoryWizardDraftStore`, `WizardTestClock`, 9b's `WizardMainDispatcherRule` and its `FakeWizardDependencies`/`withWizard` fixture moved out of `TripWizardViewModelTest.kt` into a shared test-support file first; each case < 1 s warm):

`WhereUiStateTest`. All cases are pure: draft → `WhereUi`.

| Test | Asserts |
|---|---|
| `emptyRouteShowsPlaceholdersAndDisablesSwap` | two rows; values null; `showSwap` true; `swapEnabled` false; `question` null; continue disabled |
| `completeDirectRouteAsksAboutStops` | `question.selected == Direct`; `showAddStop` false; `limitReached` false |
| `lastTripHintOnlyOnMemberFromWhenNotSame` | hint `LastTrip` at row 0 when `mode == member` and `fromLastTrip`; none when `fromLastTrip` is false, for `mode == guest`, on the home route, or when same |
| `sameRouteMarksTheLastRowAndHidesTheQuestion` | `same`; the last row `invalid`; `question` null |
| `oneStopIsLabelledStopAndHidesSwap` | labels From, Stop, To; `showSwap` false; row 1 removable; `showAddStop` true |
| `twoStopsAreNumbered` | `StopN(1)`, `StopN(2)` |
| `sixteenFlightsReachesTheLimit` | `limitReached`; `showAddStop` false |
| `yesIsDisabledAtTheLimitWithoutOutboundStops` | outbound direct + a 15-leg flight home (16 legs): on Where, `question.yesEnabled` false and `limitReached` true |
| `homeRouteLocksFromAndHidesSwapAndNotice` | row 0 locked with hint `WhereYouLanded`; `showSwap` false; `showNotice` false |
| `rowTapTargetsMatchIos` | for route AMS-DXB-NRT the rows' `sheet`s are `(0, From)`, `(0, Stop, replacing = true)`, `(1, To)`; on the home route AMS→… with boundary 2 the To row is `(legs.size − 1, Home)` |
| `progressFollowsTheBoardValues` | empty Where 0.08; complete 0.15; home route 0.50; `progressPercent` 8, 15, 50 |
| `rowDescriptionsUseTheIosKeys` | the row, locked-row, remove, swap and stop-group descriptions are built from the Wizard keys and arguments of §5.2 (assert key + args, e.g. `"Leaves from {{city}}, where you landed. You can't change this."` with `city = Tokyo`, falling back to the IATA); never on a screen |
| `sheetTitleAndCaptionPerSide` | `AirportSheetUi` for `from`, `to`, `home`, new `stop` (caption Amsterdam/Tokyo), second `stop` (Dubai/Tokyo), replacing `stop`, home `stop` (Tokyo/Amsterdam): title key and caption pair |
| `placeUsesTheGivenLocale` | `airportPlace(AMS, en)` = "Amsterdam, Netherlands"; `nl` = "Amsterdam, Nederland" (a pure function, not on screen) |

`TripWizardViewModelWhereTest`:

| Test | Asserts |
|---|---|
| `choosingFromAndToCompletesTheRouteAndMarksEntered` | `hasChanges` false → true; recovery row written (revision increases) |
| `swapExchangesTheEndsAndClearsTheLastTripHint` | |
| `yesOpensAStopSheetBetweenTheEnds` | `sheet == (0, Stop)`; the question shows Yes while the sheet is open |
| `choosingAStopInsertsIt` | |
| `replacingAStopKeepsTheLength` | |
| `removingTheOnlyStopRestoresDirect` | |
| `noDirectRemovesEveryStop` | |
| `homeToFollowsTheOutboundFromUntilChanged` | with `return_legs`, changing From changes home To; after a home To choice it does not |
| `openSheetIsRestoredFromRecovery` | a stored row with `sheet = (1, Stop)` → `uiState.sheet` equal after init |
| `continueFromWhereRunsOnce` | two `continueFrom(Where)` calls → step `outbound` once; `navigationPath == [outbound]` |
| `backFromTheHomeRoutePopsToComingBack` | |
| `cancelAirportWritesNothingButTheClosedSheet` | |

`TripWizardViewModelCloseTest` (the logic formerly in Robolectric `WizardBackTest`/`WizardDiscardTest`; a `RecordingWizardExit`):

| Test | Asserts |
|---|---|
| `requestCloseOnAnEmptyWhereExitsAtOnce` | one exit `Trips`; the store saw `discard` once; `discardDialogVisible` stays false |
| `requestCloseWithChangesShowsTheDialog` | `discardDialogVisible`; no exit |
| `dismissDiscardKeepsEveryValue` | dialog hidden; draft unchanged; no exit |
| `confirmDiscardExitsOnceAndClosingBlocksRepeats` | two `confirmDiscard()` + a later `requestClose()` → one exit, one `discard` |
| `discardFailureStaysOnScreen` | a failing store → `closing` false again, no exit; `CancellationException` is rethrown |
| `dialogIsNotRestoredByRecovery` | a recovered draft starts with `discardDialogVisible == false` |
| `loadFailedExitsQuietly` | `deps == null` → one exit `Trips` |

### Task 3: Search state in the ViewModel (`feat(wizard): map airport search states for the sheet`)

**Change.** §5.6: the mapping from 9b's `AirportSearchSession.state` to `AirportSearchUi`, the `airportIsAllowed` filter, and `setQuery("")` on sheet open and close. No new pipeline.

**JVM test** `AirportSearchStateTest` builds the VM over 9b's real `AirportSearchService` with its primary constructor `AirportSearchService(search, now, sleep, cache = null)` (`search` = a test lambda scriptable per query: results, failure, `DataFailure.Throttled` with `retryAfterSeconds`; `sleep` advances virtual time), `WizardTestClock` and a `StandardTestDispatcher`. No `FakeAirportSearch` class exists; do not add one to `:core:testing`. Pacing, debounce and cancellation are 9b's tests; do not repeat them.

| Test | Asserts |
|---|---|
| `shortQueriesShowTheHint` | "", "A", " a " → `Hint` |
| `searchingThenResults` | "Am" → `Searching`, then `Results` in service order |
| `emptyAndFilteredOutAreEmpty` | 0 results → `Empty`; for a new stop on AMS→NRT, `[AMS, NRT]` → `Empty`; `[AMS, DXB, NRT]` → `[DXB]` |
| `failureMapsToFailedWithoutRetryAt` | a non-throttle failure → `Failed(null)`; `retrySearch()` repeats the query (the fake saw it twice) |
| `throttleMapsToFailedWithRetryAt` | `Failed(retryAt = now + 60 s)` |
| `openingAndClosingTheSheetResetToHint` | after a result, `cancelAirport()` → `Hint`; reopening → `Hint` |
| `throttledRetryEnablesAtRetryAt` | `Failed(retryAt)`: the VM's `retryEnabled` is false, then true after the virtual clock passes `retryAt` (one `delay`) |

### Task 4: Wizard shell (`feat(wizard): add the wizard shell`) [ticks "Shell" with Task 5]

**Change.**
- `WizardRoot`, `WizardScaffold`, `WizardHeader`, `WizardFooter`, `WizardStepHost` (inner `NavDisplay`), `WizardStepSeam` (debug and release) and `WizardMetrics`, per §5.1 and §5.3.
- If `CfButton` cannot draw a primary container at a disabled alpha of 0.4, add `disabledAlpha: Float = AuthTokens.disabledOpacity` to it in the same commit, with a `CfButtonTest` case.

**Robolectric `WizardScaffoldGeometryTest`** (`@Layout`), one test per profile (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`), rendering `WizardScreen` with the `WizardWhere` seed state. All values ± 0.5 dp.
- The header top is `safeTop`, its height 54.
- `wizard.close` centre x = `w − safeEnd − 34`. `wizard.back` does not exist.
- Progress top = `safeTop + 60`, height 4, start = column start + 20, end = column end − 20.
- Title top = `safeTop + 88` (scroll column top `safeTop + 76` + 12).
- `wizard.where.continue` height ≥ 52, start = column start + 20, bottom = `h − safeBottom − 12`.
- Column width: 520 and centred at `foldInner` (841 dp) and `landscapeSideNav` (914 dp); the full width at `pixel8` and `small`.
- An extra case at a 668 × 900 dp window gives full width; at 669 × 900 it gives 520.

**Robolectric `WizardHeaderTest`** (`@Logic`, `WizardScreen` with seed states; structure only, no copy):
- On Where there is no `wizard.back`; `wizard.close` exists and has a non-empty `contentDescription`.
- On `WizardHomeRoute`, `wizard.back` exists.
- `wizard.progress` has range info 0.15 (the description's percent is `WizardChromeUi.progressPercent`, JVM-tested in Task 2).
- On a seam step (`outbound`), `wizard.seam.outbound` exists and the footer does not.
- Focus on step change: swap the state from `WizardWhere` to the `outbound` seam (no title) and back; `wizard.title` `assertIsFocused()`.

No Robolectric IME test: AGENTS "Tests" 6 puts IME behaviour on the emulator, and in 9c no step has an inline text field (the only field is in the sheet, which Task 11 checks). The footer's `imePadding` is reviewed in code; the first phase with an inline field adds the emulator check.

### Task 5: Back, close and the discard alert (`feat(wizard): add back handling and the discard alert`) [ticks "Shell"]

**Change.** §5.4.

The close and discard logic is JVM-tested in Task 2's `TripWizardViewModelCloseTest`. Robolectric checks only the native back wiring and the dialog's structure.

**Robolectric `WizardBackHandlingTest`** (`@Logic`, `WizardScreen` with seed states and a recording `onEvent`; system back through the activity's `OnBackPressedDispatcher`):

| Test | Asserts |
|---|---|
| `backOnWhereRequestsClose` | one `RequestClose` event |
| `backWithTheDialogOpenDismissesIt` | one `DismissDiscard`; no `RequestClose` |
| `backWithTheSheetOpenCancelsTheSheet` | one `CancelAirport`; no `RequestClose` |
| `backOnTheHomeRoutePops` | one `Back` (the inner `NavDisplay`) |
| `discardDialogHasBothActions` | with `discardDialogVisible`, `cf.dialog.confirm` and `cf.dialog.dismiss` exist; clicking confirm sends `ConfirmDiscard` once |

### Task 6: Where step (`feat(wizard): add the where step`) [ticks "Where"]

**Change.** `WhereStep` and the components of §5.2, including the home-route variant and the audience notice.

**Robolectric `WhereStepTest`** (`@Logic`, `WizardScreen` with seed states, a recording `onEvent`, `RecordingHaptics`; the route logic itself is Task 2's JVM tests):

| Test | Asserts |
|---|---|
| `tappingRowsOpensTheMatchingSheet` | clicking `wizard.from`, `wizard.to`, `wizard.stop.1` and the home `wizard.to` sends `OpenAirport` with the row's `sheet` from the state |
| `lockedHomeFromIsNotClickable` | `assertIsNotEnabled`; no event on click (its description is Task 2's `rowDescriptionsUseTheIosKeys`) |
| `swapFiresLightAndSwaps` | disabled at alpha 0.35 on Empty (read the `graphicsLayer` alpha through a test semantics property or the capture) |
| `removeFiresLightAndRemoves` | |
| `noDirectWithStopsFiresLight` | |
| `yesWithoutStopsOpensTheStopSheet` | |
| `limitLineReplacesAddStop` | on the `WizardWhereLimit` seed, `wizard.route.limit` exists and `wizard.add-stop` does not (the disabled-Yes rule is covered in `WhereUiStateTest`) |
| `continueIsDisabledWhenEmptyOrSame` | |
| `sameLineIsAPoliteLiveRegion` | |

**Robolectric `WhereSemanticsTest`** (`@Logic`, structure only; the description strings are Task 2's JVM test, and the captures show the copy):
- each row (`wizard.from`, `wizard.stop.1`, `wizard.to`) is one merged node with `Role.Button` and a non-empty `contentDescription`;
- `wizard.remove-stop.1` and `wizard.swap` have a non-empty `contentDescription`;
- `wizard.route.stops` has a non-empty `contentDescription`;
- `wizard.title` has `Heading`;
- `wizard.audienceNotice` is one merged node.

**Robolectric `WhereGeometryTest`** (`@Layout`, `pixel8`, en, `WizardScreen` with the `WizardWhereStop` seed state):
- every row is ≥ 68;
- the row label's start = card start + 16;
- the card's top = subtitle bottom + 18;
- remove and swap targets are ≥ 48;
- the swap visual is 40 and its end = card end − 14 (on `WizardWhere`).

**Task 7 implementation notes (2026-11-20):** `AirportRowUi` also retains the airport timezone so choosing a result passes the complete suggestion to the model. The existing `CfButton` gains an optional `contentColor` (default unchanged) for the dawn Cancel ghost button. The search glyph is generated from `WizardSheet`; swap and plus are generated from `Wizard`.

### Task 7: Search sheet (`feat(wizard): add the airport and stop search sheet`) [ticks "Search sheets"]

**Change.** §5.5.

**Robolectric `AirportSearchSheetTest`** (`@Logic`, `WizardScreen` with a sheet state and a given `AirportSearchUi`, a recording `onEvent`, `RecordingHaptics`; titles, captions, the place string and the retry timing are Task 2/3 JVM tests):

| Test | Asserts |
|---|---|
| `fieldIsFocusedWhenTheSheetOpens` | |
| `eachSearchStateShowsItsTag` | one case per `AirportSearchUi`: `wizard.airport.hint`, `.searching`, `.results` (with `wizard.airport.AMS`), `.empty`, `.error` with `wizard.airport.retry` enabled for `Failed(null)` and disabled when the state says retry is not yet allowed |
| `selectingAStopFiresLightAndChoosesOnce` | double click → one `ChooseAirport` event; `Light` recorded |
| `selectingFromFiresSelection` | |
| `cancelSendsCancelAirport` | `wizard.airport.cancel` → one `CancelAirport` |
| `queryIsCappedAt60` | 61 typed characters leave 60 in `wizard.airport.query` |

**Robolectric `AirportSearchSheetGeometryTest`** (`@Layout`, `pixel8` and `foldInner`):
- the sheet top = `safeTop + 56` ± 1, or the recorded fallback;
- the sheet width is ≤ 640 and centred at `foldInner`;
- the field box is 40 high inside a band ≥ 48;
- result rows are ≥ 60;
- the code column is 44 wide;
- the results card radius 16 is checked in the capture.

### Task 8: Entry and quiet recovery (`feat(app): open the wizard from my trips and recover it quietly`) [ticks "Entry" with Task 9]

**Change.** §5.8 entry (the overlay lambda, `overlayOwner`, `closeWizard`, the `AnimatedContent`, the overlay `BackHandler` condition), `WizardLauncher` and `WizardRecoveryCheck`.

**JVM `WizardLauncherTest`** (`app/src/test`, over a fake holder):
- two `open()` calls before `overlay` changes → one `push`;
- `open()` while `overlay` is a `Route.Wizard` → none.

**JVM `ShellViewModelOverlayTest`** (`app/src/test`; `ShellViewModel` is a plain `ViewModel` over `mutableStateOf`, so no composition):
- `closeWizard(Trips)` after `push(Wizard)` from Trips → `overlay == null`, `selected == Trips`, stacks unchanged; from Today → `selected == Today`;
- a ViewModel put into `overlayOwner` is cleared (its `onCleared` ran) when the overlay closes, and a new `push(Wizard)` gets an empty store.

Add to `core/navigation/…/shell/ShellNavStateTest.kt` only what it lacks (it already covers `push(Wizard)` → overlay and `back()` clearing it).

**JVM `WizardRecoveryCheckTest`:**
- a live row on the first call of an epoch → true;
- a second call in the same epoch → false;
- a new epoch with a live row → true once more;
- an expired row → false (and the store deleted it);
- a capture seed set → false, and the store is not read;
- the check's own work runs on the injected IO dispatcher.

No Robolectric entry test: composing `AppShell` needs `ChronoFlowApp.graph` and a session, which AGENTS "Tests" 1 and the Konsist app-boot allowlist forbid for this logic. The launcher, the overlay owner and `closeWizard` are covered on the JVM above, and the real New trip → wizard → close path is Task 11's emulator journey.

### Task 9: Debug seeds (`feat(debug): seed wizard board captures and auto open`) [ticks "Entry"]

**Change.**
- `WizardBoardCaptureSeed` and `FixedAirportSearch` (`feature/wizard/src/debug`).
- `WizardDebugSeeds` (`app/src/debug`): reads `DebugSeedRouting.current?.wizardAutoOpen` / `wizardBoardCapture` (already parsed; 4e added both keys), installs the `WizardStartOverride` and the recovery-write disable, and ignores `TripActions`.

**Tests.**
- JVM `WizardBoardCaptureSeedTest` (`feature/wizard/src/testDebug`): each of the 27 seed names in §5.8 builds a draft whose derived `WhereUi`/`AirportSheetUi` matches the table (one assertion per name). The limit seed has 16 legs and no equal neighbours.
- JVM `WizardDebugSeedsTest` (`app/src/testDebug`, no Activity, no graph; builds `TestSeed` values directly):
  - `wizardAutoOpen = true` asks the launcher to open once per epoch;
  - `wizardBoardCapture = "WizardStopSearchSecond"` yields an override whose `start()` draft has `sheet = (1, stop)`, whose search returns BKK and DMK for "Ban", whose clock is 2026-10-01T09:00Z, and whose store writes nothing durable;
  - `wizardBoardCapture = "TripActions"` yields no override and no open.
- The end-to-end seed path runs on the emulator in Task 11's keyboard check (`CF_WIZARD_BOARD_CAPTURE=WizardStopSearch`).
- `app/src/testRelease/…/ReleaseReadinessTest.kt` (extend it): release has no `WizardBoardCaptureSeed`, `FixedAirportSearch` or debug `WizardStepSeam`, and `wizardStartOverride` is null.

### Task 10: Guards and board captures (`test(wizard): guard where and search states and capture the boards`) [ticks "Tests" with Task 11]

**`WizardGuardsTest`** (`@Layout`, parameterised, one test case per parameter set). It runs 0e `Guards` (no truncation, 48 dp targets, inside `safeDrawing`, no overlap, headings) on `WizardScreen` with states derived from the seed drafts and `FixedAirportSearch` results (no ViewModel).

| Cases | States | Matrix | Count |
|---|---|---|---|
| (a) | the 12 states: `Where`, `WhereEmpty`, `WhereStop`, `WhereStops`, `HomeRoute`, `HomeRouteStop`, `AirportFrom`, `AirportHome`, `StopSearch`, `StopSearchSecond`, `StopSearchHome`, `Discard` | `small`, `pixel8`, `foldInner`, `largeFont` × nl (the longer copy), light | 48 |
| (b) | `WhereSame`, `WhereLimit` and the five search states (`Hint`, `Searching`, `Empty`, `Failed`, `Failed` throttled) inside the `StopSearch` sheet | the same 4 profiles × nl, light | 28 |
| (c) | all 19 states of (a) and (b) | `small` × en, light | 19 |
| (d) | `Where` and `StopSearch` | `landscapeSideNav` × nl, light | 2 |

- Orchestrator trim (2026-11-20, user rule "tests under a second" and AGENTS "Tests" 5): the guards check geometry only (truncation, targets, insets, overlap, headings), which the theme does not change, so dark runs only in the board captures; nl is the longer copy, so en runs on the narrowest profile only.
- Each warm case runs in < 1 s (AGENTS "Tests" 5); the first case per SDK and per class is warm-up and is recorded separately. Record the class run time in the PR. Total: 97 cases.
- `WizardWhereLimit` at `small` + `largeFont` + nl must scroll to the limit line, and the footer must stay fixed.

**`WizardBoardCaptureTest`** (`@Capture`, Robolectric).
- Setup: 390 × 844 dp, density 3, zero insets, `BoardCapture.capture(rule, name)` (`:core:testing-compose`), rendering `WizardScreen` with the seed state. The theme and language come from the name: `Dark` = dark, `NL` = nl, `DarkNL` = both.
- Names: `WizardWhere`, `WizardWhereDark`, `WizardWhereNL`, `WizardWhereEmpty`, `WizardWhereEmptyDarkNL`, `WizardWhereSame`, `WizardWhereStop`, `WizardWhereStopDarkNL`, `WizardWhereStops`, `WizardWhereStopsDarkNL`, `WizardHomeRoute`, `WizardHomeRouteDarkNL`, `WizardHomeRouteStop`, `WizardHomeRouteStopDarkNL`, `WizardAirportFrom`, `WizardAirportFromDarkNL`, `WizardAirportHome`, `WizardAirportHomeDarkNL`, `WizardStopSearch`, `WizardStopSearchDarkNL`, `WizardStopSearchSecond`, `WizardStopSearchSecondDarkNL`, `WizardStopSearchHome`, `WizardStopSearchHomeDarkNL`, `WizardDiscard`, `WizardDiscardDarkNL` (26), plus `WizardWhereLimit_noBoard` (1).
- Sheet captures wait until `sheetState.currentValue == Expanded` and the IME is hidden (call `hideKeyboard()` before capturing).

**`TripSearchStatesCaptureTest`** (`@Capture`): the `StopSearch` sheet at 375 × 812 per state, light and dark, en:
- `TripSearchStates_hint_{light,dark}`;
- `_searching_…`;
- `_empty_…`;
- `_error_…`;
- `_throttled_…` (no board).

That is 10 PNGs. Skeletons are static (Phase 2).

No stored Roborazzi references in this phase (opt-in only).

### Task 11: Emulator keyboard check and journey (`test(app): check the search sheet above the keyboard and walk the route`) [ticks "Tests"]

Both tests live in `app/src/androidTest/kotlin/net/chronoflow/app/wizard/`. They extend 0e `ScreenTest` (`app/src/androidTest/…/testing/ScreenTest.kt`), launch seeded (AGENTS "Tests" 3), are `@Smoke`, run in < 10 s each with `timeout_msec=60000`, wait with `waitUntil`/`waitUntilExactlyOneExists`, and never sleep.

**`WizardSearchKeyboardTest.fieldAndFirstResultStayAboveTheKeyboard`** (the one IME check).
- Seed: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_MOCK_DELAY=0`, `CF_WIZARD_BOARD_CAPTURE=WizardStopSearch`.
- Wait for `wizard.airport.DXB`.
- Click `wizard.airport.query`.
- Wait until the IME is really visible: `ViewCompat.getRootWindowInsets(decorView)!!.isVisible(WindowInsetsCompat.Type.ime())` and its bottom ≥ 100 dp. Otherwise fail with "software keyboard not visible — check hw.keyboard / show_ime_with_hard_keyboard".
- Assert in window coordinates:
  - the bottom of `wizard.airport.query` ≤ the IME top;
  - the bottom of `wizard.airport.DXB` ≤ the IME top;
  - the top of `wizard.airport-search` ≥ the status-bar bottom.

**`WizardJourneyTest.newTripRouteReachesTheNextStep`** (the wizard feature's one journey; 9d–9g extend this test and add no other).
- Seed as above without `CF_WIZARD_BOARD_CAPTURE`, plus `CF_PLAN_TEST_ROUTE=trips` (opens My trips through `DebugShellStart`).
- Steps:
  1. Click `trips.create` (6a), then wait for `wizard.title`.
  2. Click `wizard.from`, then type "Amsterdam" (the mock matches AMS), then wait for and click `wizard.airport.AMS`.
  3. Click `wizard.to`, then type "Dubai", then click `wizard.airport.DXB`.
  4. Assert that `wizard.where.continue` is enabled, then click it.
  5. Wait until `wizard.seam.outbound` exists.
  6. Press back, then click `wizard.close`, then click `cf.dialog.confirm`; wait until `trips.create` exists again (the overlay closed onto My trips).
- Record the duration in the PR.
- If the demo account has trips, the From may be prefilled ("From your last trip"). The test still taps From and chooses AMS.

If the worker sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:WizardSearchKeyboardTest app:WizardJourneyTest"`) and their output; the orchestrator runs them.

### Task 12: Docs (`docs(wizard): record shell and where acceptance`)

**`docs/wizard.md`** (new or extended):
- the shell metrics table;
- §4's settled-vs-board table;
- the back and close table;
- the search state table;
- the seed table;
- the capture list with the reviewer's board comparison: each difference fixed, or recorded with its board name;
- the measured sheet top;
- the Android-only copy.

**`plan.md`:**
- append OQ-9c-1 … OQ-9c-12 to "Open questions for the user", with the next free `OQ-A` numbers (from **OQ-A370**; OQ-A369 is the last used at `6a190cf1`);
- tick the remaining Phase 9c boxes.

## 8. Acceptance criteria

1. `make quick` passes in under 60 s warm. `make check` passes on the phase branch with `pixel` booted; the orchestrator runs it if the sandbox cannot.
2. This command is green, and every JVM test runs in under 1 s (`build/test-results/*/timings.json`):

   `make targeted ONLY="feature:wizard:WhereUiStateTest feature:wizard:TripWizardViewModelWhereTest feature:wizard:TripWizardViewModelCloseTest feature:wizard:AirportSearchStateTest feature:wizard:WizardBoardCaptureSeedTest app:WizardLauncherTest app:WizardRecoveryCheckTest app:ShellViewModelOverlayTest app:WizardDebugSeedsTest core:navigation:ShellNavStateTest"`
3. All Robolectric classes of Tasks 1 and 4–10 pass, each warm test in under 1 s, with first-case warm-up recorded separately (AGENTS "Tests" 5). The guard count is 97. No test composes `AppShell`, `CfRoot`, `RootContent`, `MainActivity` or a graph, and none asserts copy on screen (`rg -n "onNodeWithText|hasText|assertTextEquals" feature/wizard app/src/test* ` finds no new hit).
4. `make captures ONLY=Wizard,TripSearchStates` writes the 37 PNGs of Task 10 to `build/captures/`.
   - The Opus reviewer compares them side by side with the boards rendered at 390 × 844 (TripSearchStates at 375). Every difference is fixed or recorded in `docs/wizard.md` with its board name.
   - Only the §4 rows are accepted differences: no glass, the M3 segmented control and dialog, the audience notice, backdrop progress, the Discard and AirportHome backdrops, the footer bottom, and the drag handle.
5. On `pixel`, `WizardSearchKeyboardTest` and `WizardJourneyTest` pass, each in under 10 s. The journey duration is in the PR.
6. A debug run of the journey is StrictMode-clean: no disk or network on the main thread. The recovery check and writes run on IO.
7. Performance:
   - typing 10 characters in the search field recomposes neither `RouteCard` nor `WizardHeader`. A Robolectric `@Logic` test, `WizardRecompositionTest`, counts with a `SideEffect` counter in a test-only wrapper; the count is 0 after the first frame.
   - No size from `onGloballyPositioned`/`onSizeChanged` is written to state.
8. `rg -n "WindowInsets\.(safeDrawing|statusBars|navigationBars)" feature/wizard` finds nothing. Konsist passes (`@BoardMetrics` objects hold every dp literal). `rg -n "announceForAccessibility|Thread\.sleep" feature/wizard app/src/androidTest/kotlin/net/chronoflow/app/wizard` finds nothing.
9. `git -C /Users/rickgout/Sites/chronoflow-ios status`, and the same for `…/chronoflow-new` and `…/chronoflow-mobile`, are clean.
   - Every commit is a Conventional Commit with no trailer.
   - The Phase 9c boxes (Shell, Where, Search sheets, Entry, Tests) are ticked.
   - `plan.md` carries the new copy rows and the OQs.

## 9. Review checklist (Opus reviewer)

- [ ] **The model is not reimplemented.** Route edits go through the 9a/9b functions (`chooseAirport`, `airportIsAllowed`, `swapAirports`, `removeStop`, `makeDirect`, `finishRoute`, `navigationPath`, `popNavigation`, `discard`). The UI derives, never decides. Any missing rule was ported in a separate `fix(wizard)` commit with its iOS test.
- [ ] **Shell numbers** match §5.1:
  - header 54, progress at `safeTop + 60` and 4 dp with 20 dp insets;
  - scroll column top 76, title top 88, gap 14, title group +4;
  - CTA 52 at alpha 0.4 when disabled, 12 dp above the IME or nav bar;
  - column 520 only from 669 dp.

  Every dp is in a `@BoardMetrics` object. Insets come only from `LocalSafeInsets` and the IME.
- [ ] **Back and close** follow the §5.4 table:
  - no back on Where;
  - × and first-screen back close at once when `!hasChanges`;
  - the dialog is never restored by recovery;
  - discard deletes the row before exit;
  - `closing` blocks repeats;
  - a sheet or dialog consumes back first;
  - predictive back pops inner steps only.
- [ ] **No invented screens.**
  - Unbuilt steps render only the debug `WizardStepSeam` (release: empty).
  - No Outbound, Return or edit UI was added.
  - The audience notice is the only no-board element, and it is iOS copy.
  - `WizardWhereLimit` and the throttled state are labelled "no board".
- [ ] **Search.**
  - Debounce, pacing, 429 handling and cache live in 9b's service. The VM never calls `DataSource` directly.
  - Stale searches are cancelled (`transformLatest`).
  - `CancellationException` is rethrown, never mapped to `Failed`.
  - Results are filtered with `airportIsAllowed`.
  - The place string uses the app locale, not `Locale.getDefault()`.
  - One choice per sheet open.
- [ ] **State survival.**
  - The sheet comes from the persisted `draft.state.sheet`; the query is in `rememberTextFieldState`.
  - No route or form value is in `SavedStateHandle` beyond 9b's `instanceId`.
  - No `rememberSaveable` holds the draft.
- [ ] **Recovery auto-open** runs once per epoch, off the main thread, only from `AppShell` (app-root routes), and never with a capture seed.
- [ ] **Overlay.** The wizard renders in 6a's overlay slot; its ViewModel store (`ShellViewModel.overlayOwner`) is cleared when the overlay closes; the tab scaffold is not composed while it shows (except during the slide); 6a's overlay `BackHandler` does not pre-empt §5.4.
- [ ] **Search** is 9b's `AirportSearchSession`, mapped; there is no second debounce or pipeline in `:feature:wizard`.
- [ ] **Haptics** match §5.2 exactly: `Light` for swap, remove, direct-with-stops and choosing a stop; `Selection` for the other choices. None fire twice (`CfSegmentedButtons` already fires `Selection`).
- [ ] **Accessibility.**
  - Row descriptions follow the iOS strings.
  - The locked row is disabled with its sentence.
  - The progress description is "Progress, N percent".
  - The same line, the search status and the error card are polite live regions.
  - The title is a heading and is focused on a step change.
  - Every target is ≥ 48 dp.
- [ ] **Strings.**
  - Only generated resources reach `Text`.
  - Names come from `names.lock.json`.
  - The 7 Android-only entries carry `review: "needs review"` and appear in the plan table.
  - "luchthaven", never "vliegveld".
- [ ] **Debug isolation.** Seeds, `FixedAirportSearch` and the debug seam are in `src/debug`. The release readiness test covers them.
- [ ] **Tests** run at the cheapest tier: model mapping on the JVM, UI in Robolectric, and only the IME check and the one journey on the emulator. Queries use exact tags; there is no tree traversal and no sleep. Guard cases are separate test cases.
- [ ] **Typical mistakes to look for:**
  - `catch (e: Exception)` swallowing `CancellationException`;
  - `imePadding()` applied twice (scaffold and footer) so the button floats too high;
  - `MaterialTheme.colorScheme` in the feature;
  - the M3 `SearchBar` instead of the designed field;
  - the sheet composed with `sheet == null` and hidden by alpha;
  - `NavDisplay` stack mutated by hand (it must be derived from `navigationPath`);
  - `LaunchedEffect(Unit)` focus requests that refire after recovery;
  - `weight` inside `verticalScroll`;
  - a `data class` UI state holding lambdas (unstable).

## 10. Open questions (proceed on the default; append each to `plan.md` with the next free `OQ-A` number)

| # | Question | Default |
|---|---|---|
| OQ-9c-1 | iOS shows the audience notice ("ChronoFlow is intended for adults…" / "…not suitable if you have bipolar disorder…") on Where (NH5, the no-health-data decision). No Where board draws it. | Build it per iOS, below the route card on Where only, and record it as an accepted capture difference. Ask the designer to add it to `WizardWhere`. |
| OQ-9c-2 | The search placeholder: WizardCopy "Airport or city" / "Luchthaven of stad" vs iOS and TripAirportSearch "Search airports". | WizardCopy, as Android-only copy, marked "needs review". |
| OQ-9c-3 | TripSearchStates copy (hint, searching, empty, error) has no iOS catalog keys, because iOS never built those states. | Ship §5.7's en (board) and nl (web `lang/nl.json` where it exists; proposed otherwise), marked "needs review". |
| OQ-9c-4 | `TripAirportSearch` draws a count line ("5 airports") and older rows. | Not built. `WizardSheet` rows only; the count line has no copy. |
| OQ-9c-5 | The `WizardDiscard` backdrop is Outbound, which 9d builds. | 9c captures the alert over Where with something entered; 9d re-captures `WizardDiscard*` over Outbound. |
| OQ-9c-6 | Backdrop percentages on the sheet boards (15 over the empty Where, 55 on the home sheets) disagree with the model. | Show the model value for the state under the sheet (8 or 50); record it. |
| OQ-9c-7 | Glass circles (header buttons, swap) under OQ-A6 "no glass". | Header: ghost icon buttons with no fill. Swap: an opaque 40 dp circle in `wizardPickerFill`. |
| OQ-9c-8 | The board search field is 40 high, below the 48 dp target. | A 40 dp visible box inside a 48 dp clickable band, with gaps adjusted so the box sits where the board draws it. |
| OQ-9c-9 | Where row height: board 68 vs iOS token `wizardAirportHeight` 76. | Board, 68 dp. |
| OQ-9c-10 | After Discard, or × with nothing entered, the board exits to Trips (`goHref`), but iOS `dismiss()` returns to whatever tab the wizard covered (quiet recovery can open it over Today). | Follow iOS: clear 6a's overlay and show the tab underneath (My trips after New trip). |
| OQ-9c-11 | 9b's `Searching(query, stale)` carries the last results; `TripSearchStates` draws only the status row and skeletons. | Show the board's Searching state (status + skeletons) and ignore `stale`. |
| OQ-9c-12 | 6a's overlay `Box` owns no `ViewModelStore` (it uses the root entry's owner), and it replaces the tab scaffold without a transition. | 9c adds `ShellViewModel.overlayOwner`, cleared when the overlay closes, and an `AnimatedContent` slide between scaffold and overlay (§5.8). The scaffold is already out of the semantics tree while the overlay shows. |
