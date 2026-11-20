# Phase 9g: Ready and save

- **Branch:** `feat/phase-9g-wizard-ready-save`. Start it from the top of the stack once **9f** (Checking, Fine-tune and its sheets) is in it. Through 9f the stack also carries 9e, 9d, 9c (shell, Where, `WizardBoardCaptureSeed`, `WizardExit`, `ShellViewModel.closeWizard`), 9b (generation service, `MemberWizardStore`, `FirstTripRemindersOffer.queue`, the conflated recovery writer, the `TripWizardViewModel` scaffold), 9a (the `:core:wizard` model, `WizardCredits`, `WizardPresentation`), 6a (`ShellNav.wizardSaved`), 6b/6c (trip page with `awaitingSync`), 4c (`MockDataSource`), 2 (primitives, haptics), 0c (strings), 0e (profiles, guards, captures, seeds). If one is missing, stop and report it.
- **Depends on:** 9f. **Wave:** 13. **Plan entry:** `plan.md` "### Phase 9g: Ready and save" (l.2920–2952). Also read in `plan.md`: A14 (l.101, billing: none), OQ 41 and OQ 43 (l.161, l.163), OQ-A18 (l.614, credits from the server only), OQ-A36 (l.632, the `06` wizard defaults), OQ-A356 (l.951, the via-flight copy, owned by 9g), OQ-A365 (l.960, rating display "revisit with the Ready/sheet UI", owned by 9g), OQ-A368 (l.963, first-trip offer), OQ-A435 (l.1030, = OQ-9f-16, the `WizardUiOverride` seam), "Android spec: state survival" (Wizard row l.379, section l.373–383), "Android spec: navigation and routes" (`TripPage(… awaitingSync …)`, l.402), "Android spec: Material mapping" (Haptics table, l.439–448), "Android spec: seeds and test hooks" (l.466–476), "Android-only copy awaiting approval" (l.487).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. Checked against `docs/phases/phase-9f-wizard-checking-finetune.md` (9f's board freeze is the `WizardUiOverride` seam, §5.10 there, not a model method; its step entries are `WizardCheckingStep`/`WizardFineTuneStep`). Refreshed 2026-11-20 against the merged 9a–9f code (see "Refresh notes" below): 9f's ViewModel has **no** `haptics: Flow<HapticEvent>`; it emits a `WizardHapticCue` instead. Before you code, open the merged code of 9a–9f and use **their** names. Where a name differs, keep theirs and note it in the PR. Never rename earlier code.
- **Tests:** every test follows `AGENTS.md` "Tests": the cheapest tier, the budgets (a JVM or warm Robolectric case < 1 s; warm-up allowance 20 s for the first Robolectric case per SDK and 3 s for the first case per class/SDK, recorded separately; emulator < 10 s), no copy assertions on screen (tags and structure only), exact tag queries, waits with `waitUntil`, never `Thread.sleep`, hang caps (`runTest(timeout = 60.seconds)`, `timeout_msec=60000`). Tier annotations are 0e's (`net.chronoflow.core.testing.tiers`): Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`.
- **Tags:** **[F]** fact read in source (path and line given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`. Board coordinates are CSS px and map 1:1 to dp.
- **Paths:** `…/cw/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`; `…/fw/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`; `…/fwd/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`; `…/fwt/` = `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-20, against `1cba33d4`)

These facts override the body wherever they disagree. Paths use the abbreviations above.

1. **Events, not VM methods.** The screen is stateless: `WizardScreen(state: WizardUiState, onEvent)`, and `TripWizardViewModel.onEvent(WizardEvent)` dispatches (`…/fw/shell/WizardEvent.kt`; VM l.696–954). Read every `vm.makePlan()`, `vm.readyTick()`, `vm.readyForeground()`, `vm.revisitFromReady(row)` in this brief as a new `WizardEvent` case handled in `onEvent`. These exist already: `WizardEvent.OpenPrompt(id)`, `RequestClose`, `CloseFromFooter` (iOS's no-credits Close rule: close at once for guest or no changes, else the discard dialog, VM l.575–585), `Continue(step)`, `Back`.
2. **UI state shape.** Step UI models are fields of `WizardUiState` (`…/fw/ui/WizardUiState.kt`, l.446–605), built in `WizardUiState.from(active, …, uiOverride)` from `effective`. `effective` already has the override's `checking`/`retrySeconds`/`reviewing` applied (l.482–490). Add `ready: ReadyUi?` and `calculating: CalculatingUi?` there. Do not add separate `readyUi`/`calculatingUi` VM StateFlows. The steps are dispatched in `WizardStepContent` (`…/fw/shell/WizardScreen.kt` l.435–486). Steps without a title still render `WizardStepSeam(key)` (debug tag `wizard.seam.ready`). `WizardStepKey` (`…/fw/shell/WizardStepKey.kt`) has no `Ready`/`Calculating` constants yet.
3. **No `TextUi`, `CopyLineUi` or `WizardFieldCopy`.** Copy is `WizardCopy(key, arguments)` resolved by `WizardCopy.resolve()` (`…/fw/ui/WizardCopy.kt`, Wizard table, `count == "1"` → `one`). Server `CopyLine`s go through `rememberServerCopy(formatter)`/`serverCopy(…)` (`…/fw/components/WizardServerCopy.kt`). `WizardPresentation` functions take `(…, language: Language, t: Translate)`: `flightSummary(form, home, language, t)`, `returnSummary(form, stayEnd, language, t)`, `extrasSummary(form, review, input, language, t)`, `planDateSummary(form, summary, language, t)` and `promptAnswerText(prompt, input, language, t)`. Use 9f's `AnswerText` capture pattern for these values (`…/fw/finetune/WizardFineTuneUi.kt` l.160–184).
4. **Haptics.** 9f has no `haptics: Flow<HapticEvent>`. `checkingOutcome(previous, snapshot)` (`…/fw/checking/WizardCheckingUi.kt` l.162–180) returns `CheckingOutcome { Success, Warning, Error }` and returns null when `previous == null`. The VM wraps the outcome in `WizardHapticCue(outcome, sequence)` on `TripWizardUiState.Active.cue` (VM l.335–343). `WizardScreen` plays the cue once per sequence, guarded by a `rememberSaveable` counter (`WizardScreen.kt` l.132–147). Extend that cue for the §5.4 `saveFailure` Error. `Closed` carries no cue, and `WizardRoot` renders a blank `Box` once `uiState` is null, so fire the §5.7 `Success` haptic where the `Saved` exit is delivered.
5. **Exit today drops the trip id.** `enum class WizardExit { Trips }` (`…/fw/shell/WizardStepKey.kt:31`). When the save finishes (`snapshot.closed && createdTripID != null`), the VM sets `Closed(createdTripID)` and calls `exitOnce()`, which always emits `WizardExit.Trips` (VM l.374–377, l.438–440). A saved trip therefore lands on the Trips list today. `WizardRoot` collects the `exit: StateFlow<WizardExit?>` in `LaunchedEffect(viewModel)` (`WizardScreen.kt:110`), so the exit is delivered again whenever WizardRoot is recreated.
6. **"ShellHolder" is `ShellViewModel`.** It is `app/src/main/kotlin/net/chronoflow/app/shell/ShellViewModel.kt` (AppShell passes `holder::closeWizard`, `AppShell.kt:140`). `closeWizard(exit)` (l.77–81) handles `Trips` only when `overlay is Route.Wizard`. `ShellViewModel.wizardSaved(tripId)` already exists (l.67–69) and wraps `ShellNav.wizardSaved(state, tripId)` (`core/navigation/…/shell/ShellNav.kt:71–81`), but nothing calls it yet. Keep the `overlay is Route.Wizard` guard for `Saved`, so a re-delivered exit (note 5) does not push a second TripPage. The closed overlay's store is cleared by the `state` setter (l.22–30). The JVM test to extend is `app/src/test/kotlin/net/chronoflow/app/shell/ShellViewModelOverlayTest.kt`.
7. **Footer.** No `WizardScaffold` footer API exists. The footer is `WizardFooter(chrome, step, onEvent)` (`WizardScreen.kt` l.283–371), driven by `WizardChromeUi` (`WizardUiState.kt` l.252–347): `footerId`, `continueEnabled`, `routeComplete` (alpha 1 when true), `primaryLabel`, `primaryBusy`, `secondaryClose`, `secondaryRequestsClose`, `closeEnabled`. 9f's busy footer is already present: `primaryBusy` gives a spinner, `ProgressBarRangeInfo.Indeterminate` and alpha `WizardMetrics.ctaBusyAlpha` (0.7); the disabled alpha is `PlanUITokens.wizardDisabledOpacity`. Do not add a `busy` parameter. Today `ready` maps to `footerId = null` (l.320–330), `primaryBusy` is fine-tune only, and `continueFrom` ignores ready (VM l.537 `else -> {}`). The existing secondary close is a Ghost `CfButton` tagged `wizard.<id>.secondary-close` and drawn **above** a primary, so the Close-only footer (`wizard.ready.footer` / `wizard.ready.no-credits`, no primary) is a new footer variant.
8. **Calculating chrome is already in place.** `showClose = step != calculating` (`WizardUiState.kt:314`). The VM sets `showBack = wizard.showsBack` (VM l.170–178; `showsBack` is false on calculating, `MemberWizardNavigation.kt:126`). `footerId` and `footerHint` are null, so no footer is drawn. `popNavigation` rejects calculating (`MemberWizardNavigation.kt:104`). Missing: a consuming `BackHandler` for calculating. Back handlers live in `WizardBackHandling.kt`, and `NavDisplay(onBack = { onEvent(Back) })` sits in `WizardStepHost`.
9. **Banner and lockout.** The signature is `WizardBanner(body, testTag, modifier, tone: WizardBannerTone = Error, title)` (`…/fw/components/WizardBanner.kt:32`). There is no `WizardStrings.lockout`. 9f builds `LockoutWait(retrySeconds)` → `WizardCopy(wait.localizationKey, mapOf("count" to …))` inline in `WizardCheckingUi.failureBody` (l.75–83). Reuse that pattern. `LockoutWait` is `net.chronoflow.core.model.auth.LockoutWait`.
10. **Seeds.**
    - `WizardDebugSeeds.prepare` (`app/src/debug/kotlin/net/chronoflow/debug/WizardDebugSeeds.kt`) takes the override from `WizardBoardCaptureSeed.uiOverride(board)` → `WizardCompletionBoardSeed.uiOverride(baseName)`. Put each Ready board's `WizardUiOverride` there, not in `WizardDebugSeeds`. `WizardDebugSeeds` swaps in `FrozenCheckingClock.clock` whenever the override is non-null.
    - `WizardCompletionBoardSeed.apply` acts only on boards whose `uiOverride` is non-null.
    - Prompts come from `WizardCompletionPrompts.make` (`…/fwd/WizardCompletionPrompts.kt`, OQ-A438: Konsist forbids `:core:data-mock` in `:feature:wizard`), not `MockWizardPrompts.make`.
    - Every known board gets `WizardMemoryDraftStore`, `now = WizardBoardCaptureSeed.now` and `FixedAirportSearch`. There is no "board-capture recovery-write disable". `WizardSaveJourney` needs its own branch in `WizardDebugSeeds.prepare` that keeps the session's `store`, `now`, `airportSearch` and `checkingClock`, with `uiOverride = null`.
    - `knows(board)` checks `WizardBoardCaptureSeed.names`. `WizardSaveJourney` must be accepted by `knows` but stay **out of** `names`: `WizardBoardCaptureTest` and the seed tests are parameterised over `names`.
11. **Captures already exist.** `WizardBoardCaptureTest` (`feature/wizard/src/testDebug/…/WizardBoardCaptureTest.kt`) captures every name in `WizardBoardCaptureSeed.names`. Adding the Ready names to `WizardCompletionBoardSeed.names` adds their captures there. It composes with `ChronoFlowTheme(reduceMotion = true)` and suffixes a fixed set with `_noBoard`. Under that flag Calculating would draw the Reduce-Motion clock, not the board's spinner (§5.6).
12. **Test locations.**
    - JVM tests use the flat package `net.chronoflow.feature.wizard` in `feature/wizard/src/test/kotlin/…/wizard/`. VM fixtures: `WizardTestSupport.kt` (`FakeWizardDependencies`, `WizardViewModelFixture`, `withWizard`, `WizardMainDispatcherRule`) and `WizardCompletionFixtures.kt`.
    - Robolectric tests that need debug seeds use the same flat package in `feature/wizard/src/testDebug/kotlin/…/wizard/`: `WizardCompletionScreenFixture.completionBoard(board, locale)` and `WizardBoardStates.boardState`.
    - `WizardCompletionBoardSeedTest` already exists there (9f). `RecordingHaptics` is in `:core:testing-compose`.
    - The discard dialog has no `wizard.discard` tag; `CfAlertDialog` exposes `cf.dialog.confirm` / `cf.dialog.dismiss`. `WizardJourneyTest` (`app/src/androidTest/…/app/wizard/`) has one method, `newTripRouteReachesTheNextStep`. The tab tag is `tab.trips`.
13. **Already covered by 9b (`core/data/src/test/kotlin/net/chronoflow/core/data/wizard/`).** Do not re-port these:
    - `WizardSaveRecoveryTests.savedTripIDIsDurableBeforePullAndRecoverySkipsCreate`
    - `WizardSaveRecoveryTests.lostResponseAndManualRetryCreateOneMockTripAndSpendOnce`
    - `WizardSaveRecoveryTests.onlySuccessfulFirstTripSaveQueuesReminders`
    - `WizardCompletionTests.expiredOrCreditFailureClearsIdentityBeforeReadyRecheck`
    - `WizardCompletionTests.calculatingRejectsBackCloseAndDoubleSave`
    - `WizardFixtureScenarioTests.saveIsNeverRetriedWithoutUserAction`

    Task 7's matching rows prove the same facts at screen/VM level and may reuse those fixtures.
14. **Mock.**
    - Save receipts are `MockServerState.wizardReceipts` with a 1-day expiry (`MockDataSource.kt` l.1406, l.1509–1510).
    - The demo account comes from `core/testing/src/main/resources/fixtures/mock-seed.json`: `sleepAnswered: true`, `ageBand 18_64`, `shiftWork false`, credits 3. It already has trips, so the journey's `firstTrip` is false.
    - There is no call counter. Use `failNext(method, error)` and a `DataSource` decorator.
15. **Design system.**
    - The soft shadow is `Modifier.softShadow(…)` (`core/designsystem/…/effect/SoftShadow.kt:32`).
    - `CfSpinner(modifier, size, color)` has no stroke parameter.
    - Every token named in §5 exists (`CfDimens.generated.kt`, `CfTypeScale.generated.kt`).
    - The card chevron is `LucideGlyphs.ChevronRight` at `WizardMetrics.chevronGlyph` (`…/fw/finetune/WizardExtraRow.kt:97–99`).
16. **Open questions.** OQ-9g-1 … OQ-9g-11 become **OQ-A439 … OQ-A449** in that order. The next free number is OQ-A439, because 9f used A420–A438. OQ-A365 (rating display) also names 9g: decide it with the Extras/Latest-wake row values, or carry it forward in `plan.md`.

17. **Task 5 seed value check.** The landed 9f board seed stores `blueBlockerMdfd` as a JSON number (`1.5`); the pinned iOS completion seed stores the string `"1.5"`. Keep 9f's existing boards unchanged and use the pinned string for the new Ready/Save/Calculating boards. Both already pass through the presentation's numeric/string reader; OQ-A365's whole-number display remains unchanged.

This phase adds **UI, wiring, seeds and tests only**. The Ready, save, re-check, retry and recovery rules are already ported (9a model, 9b service and store). If a rule below is missing from 9a/9b, port it into `:core:wizard` (or `:core:data`) with its iOS test, in a separate `fix(wizard): …` commit that names the Swift line, and report it.

---

## 1. Goal

A member who finished Fine-tune sees **Ready** ("Ready to plan"): the server's plan summary card, five tappable rows (Route, Flight, Return, Sleep, Extras, plus "Latest wake-up" when the plan was adjusted), the price block and "Make my plan". Every price state is driven by the server review (`price`, `canSave`, `saveBlockedReason`): paid, unlimited, free, one direction free, adjusted, re-checking and no credits. No credits shows **Close only**, never a website or purchase button (A14, iOS OQ 41). Ready re-checks silently after 30 minutes, after recovery and, for no credits, when the app returns to the foreground.

"Make my plan" goes to **Calculating** (no ×, no back, system back does nothing). A failed save returns to Ready with the offline, server or throttle banner and never retries by itself. A process killed during the save reopens on Calculating and re-sends the **same identity**, so the server receipt returns the same trip. A successful save closes the wizard, opens the new trip's page with `awaitingSync = true` on the My trips stack, and, for the account's first trip, leaves the first-trip reminders offer `pending` for 12c.

Done means:
- every Ready and save board (23 wrappers) is guarded on Robolectric and captured;
- the price-state mapping is proven on the JVM against the fixtures;
- a Robolectric test kills and recreates the wizard during the save and gets exactly one trip;
- one emulator journey (`CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney`) creates a trip on the mock and lands on its page.

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it: "Working rules", "Performance", "Tests", "Traps").
2. `plan.md` sections listed in the header.
3. `docs/research/06-wizard.md`: §2.1 (the Ready/Save/Calculating rows, l.137–152), §3.3 (Calculating: × and back hidden), §3.6 (back to Ready, re-check rules, l.251–261), §3.7 (haptics), §3.8 (Decisions), §4.4 (credits, l.462–477), §4.7 (recovery, l.511–527), §7.1–§7.3 (endpoints, call order, errors, l.670–770), §11 item 3, §12 OQ 3, Appendix A.4 (Ready values, l.1234).
4. Earlier briefs and their merged code: `docs/phases/phase-9a-wizard-model.md` (OQ-9a-8, §11 table row 3), `phase-9b-wizard-data.md` (§6.5 recovery and `didSave`, §6.7 save, §6.8 ViewModel, §12 hand-off "9g"), `phase-9c-wizard-shell-where.md` (§5.1 shell and footer, §5.4 back/close/discard, §5.8 seeds and `WizardBoardCaptureSeed`, Tasks 10–11), `phase-9d-wizard-flights.md` §5.5 (`WizardBanner`), `phase-6a-shell-trips.md` §5.6 (`wizardSaved`), `phase-6c-trip-states-list.md` (route `awaitingSync`), `phase-0c-strings.md` §11 (resources, the wizard `_one` plural rule), `phase-0e-test-harness.md` (Guards, BoardCapture, ScreenTest, seeds).

### 2.2 Boards (`DC/`, read-only; open the HTML in a browser to render)

Every wrapper is a 0.8 KB file that `<dc-import name="Wizard" screen=… v=…>`s `DC/Wizard.dc.html` at 390 × 844. Ready's logic is `Wizard.dc.html` l.430–470 (rows, price lines, banners) and l.483–490 (title, CTA, secondary); the markup is l.177–198 (Ready card, rows, price block; Calculating card) and l.201–210 (footer). Every `*DarkNL` twin is the same state in `theme=dark lang=nl`.

| Board (and twins) | Props | `pct` | State |
|---|---|---|---|
| `WizardReady`, `WizardReadyDark`, `WizardReadyNL` | `ready` | 95 | One way, "Price **1 credit**", "You have 3 credits", "Make my plan" |
| `WizardReadyReturn`, `…ReturnDarkNL` | `ready v=retnew` | 95 | Return: Flight "… · via DXB", Return "Thu 22 Oct 10:25 → 15:30", summary-return line, dates "· return" |
| `WizardReadyUnlimited`, `…DarkNL` | `ready v=unlimited` | 95 | "**Included in your subscription.**" with no "Price" label, no balance |
| `WizardReadyFree`, `…DarkNL` | `ready v=free` | 95 | "Price **Free**" + "This trip is free: only a small body-clock shift is needed." |
| `WizardReadyOneFree`, `…DarkNL` | `ready v=onefree` | 95 | Return trip, "1 credit", "Your flight home is free: only a small shift is needed that way.", then balance |
| `WizardReadyAdjusted`, `…DarkNL` | `ready v=adjusted` | 95 | Info banner "We adjusted your plan" + extra row "Latest wake-up · 08:00 · assumed", Extras "1 answered · 3 days prep" |
| `WizardReadyChecking`, `…DarkNL` | `ready v=checking` | 95 | Button spinner + "Checking…", disabled (board alpha 0.7) |
| `WizardReadyNoCredits`, `…DarkNL` | `ready v=nocredits` | 95 | Red semibold "You have 0 credits"; board button "Buy credits on the website" + Close → **Android: Close only** (A14) |
| `WizardSaveOffline`, `…DarkNL` | `ready v=saveoffline` | 95 | Error banner "Saving needs a connection" |
| `WizardSaveServer`, `…DarkNL` | `ready v=saveserver` | 95 | Error banner "We couldn't save your plan" |
| `WizardCalculating`, `…DarkNL` | `calc` | 100 | Spinner card, "AMS → NRT · 6 days of steps"; no ×, no back, no footer |

Boards that do **not** exist (do not invent): a save-throttle banner board (iOS builds it, §5.4 — build it, capture as `WizardSaveThrottle_noBoard`), a member "Updated" banner on create (iOS builds it, §5.3 — build it, no capture), and every guest (`WizardReadyGuest*`, 11a/11b) and edit (`WizardEdit*`, 9h) Ready variant.

### 2.3 iOS (`f391a1fa`) — behaviour to mirror

| File | What to take |
|---|---|
| `Kit/TripWizardUI/MemberWizardReadyView.swift` (whole, 343 lines; banners l.19–34, summary l.35–57, rows l.58–110, tick/foreground/error haptic l.113–124, `readyButton` l.126–132, `routeSummary` l.133–136, `sleepSummary` l.137–156, `price` l.157–166, `updatedSummary` l.167–172, `priceBlock` l.173–215, row l.216–250, `saveFailureBanner` l.274–300, `calculatingScreen` l.301–326) | Ready layout order, banner priority, rows and their targets, `readyButton`, `routeSummary`, `sleepSummary`, `price`, `updatedSummary`, `priceBlock`, `saveFailureBanner`, the 1 s `readyTick` loop, foreground re-check, error haptic, `calculatingScreen` |
| `Kit/TripWizardUI/MemberWizardSteps.swift` l.1–160 | Shell footer (button l.65–107, the no-credits footer l.108–121): the no-credits footer with Close only (tags `wizard.ready.no-credits`, `wizard.ready.footer`), the primary button's disabled rule, no back on Calculating |
| `Kit/TripWizardUI/MemberWizardView.swift` l.60–110 | `resumeChecking()` + `resumeCompletion()` once on appear; `createdTripID` → success haptic → `saved(id)` |
| `Kit/TripWizard/Model/MemberWizardCompletion.swift` (whole) | `WizardCredits`, `needsReadyReview`, `canSave`, `readyTick`, `resumeCompletion`, `savePlan`, `saveFailed`. Already ported by 9a/9b — verify only |
| `Kit/TripWizard/Generation/TripGenerationService.swift` l.152–210 | `save`: a stored `savedTripId` skips the POST; otherwise the same identity is POSTed; `didCreate` before the pull. Ported by 9b — verify only |
| `Kit/TripWizard/Model/WizardBoardCapture.swift` | `freezeCompletionBoard(reviewing, answerError, retry, discardConfirmation)`. Android ports it as 9f's `WizardUiOverride` seam (9f §5.10, OQ-9f-16), not as a model method |
| `App/Debug/WizardCompletionBoardSeed.swift` (whole) | The Ready/Save/Calculating seed drafts (prices, banners, summary) |
| `App/Debug/WizardBoardCaptureSeed.swift` l.114–124, l.211–219 | `WizardSaveJourney` seed (HND, now + 30 days, 18 h flight, step `returnChoice`) and the freeze dispatch |
| `App/AppTabs.swift` l.170–186 | Saved → `TripPageRoute(tripID: id, awaitingSync: true)` on the Trips tab |
| `AppUITests/TripWizardTests.swift`, `AppUITests/WizardCompletionScreenTests.swift`, `AppTests/WizardCompletionLayoutTests.swift` l.62–90 | The one journey; seeded Ready/no-credits/Calculating checks; the no-credits Close inside the pinned footer |
| `plan.md` l.1866–1909 ("16c", "16c first simulator-run repairs", "16c wizard test harness rework"), l.2419–2422 (OQ 40–43) | Decisions; harness lessons |
| `Docs/wizard-16c.md`, `Docs/wizard-save-journey-t8.md` | Save semantics; why the journey uses HND (the mock has no NRT) |

### 2.4 API (web `e36368ec`; this phase calls no endpoint directly — 9b's service does)

- `POST /v1/trips/review` (`routes/api.php:85`, `verified`, `throttle:trip-review` 10/min/user) → 200 `ApiTripReview`. The Ready re-check is this call through `readyTick` → `reviewInPlace`.
- `POST /v1/trips` (`routes/api.php:87`, `verified`) body `{"draft_token":"…","input_revision":1,"review_revision":1}` (fixture `wizard/save-request.json`) → **201** `{"tripId":"019a1000-…0001"}` (`wizard/save.json`). A retry with the same identity returns the same `tripId` (server receipt, 7 days).
- Price shape [F `app/Actions/Api/PresentMobileReview.php:61–80`]: `price` is `null` unless the review can save; `price = {state: unlimited|free|create|edit|paid, cost, balance, unlimited, freeDirections: [outbound|return]}`; `balance` is the ledger balance; `canSave = review.canSave && affordable`; `saveBlockedReason = "Not enough credits"` when it can save but is not affordable.
- Save errors the UI sees (mapped by 4b `TripWizardErrors`, handled by 9a `saveFailed`): transport → `Offline` → banner offline; 429 → `Throttled(retryAfter)` → banner throttle + countdown; 5xx/other and 422 `review = "Try saving again shortly."` (`wizard/capacity.json`) → banner server; 422 `review_expired` (`wizard/expired.json`) and 422 `credits = "Not enough credits"` (`wizard/credits.json`) → identity cleared, Ready re-checks (the latter lands on no credits).

---

## 3. Out of scope (owner)

| Item | Owner |
|---|---|
| Edit Ready ("Edit your trip", "Update my plan", edit price notes, `sourceChange` "On another device" banner, `WizardEdit*`) | 9h |
| Guest Ready, guest price lines, Questions row, sign-up hand-off (`WizardReadyGuest*`, `GuestSignup`) | 11a, 11b |
| The "Turn on reminders?" sheet and `FirstTripRemindersOffer.consume` | 12c |
| Any billing, purchase UI, `SetPaywall`, website link, Play Billing (A14, OQ 41) | never |
| Fine-tune, its sheets, `WizardFineTuneNone`, Checking and its failure cards | 9f (already in the stack) |
| Wizard baseline-profile journey | 13 (plan l.3361, "Profiles"). Run `make baseline-profile` only if the generator already has a wizard journey; say which in the PR |
| Dutch/English copy changes to iOS catalog strings | never (generated by `make strings`) |

---

## 4. Hand-ins to verify before Task 1 (no commit)

Check each with `grep -rn`. If one is missing and has no fallback, **stop and report**.

1. **9a** (`:core:wizard`): `WizardCredits { unlimited, free, oneFree, credits, unavailable }` with the iOS precedence (`MemberWizardCompletion.swift:22–41`): `price == null` → unavailable; `!canSave && saveBlockedReason != null` → unavailable; `price.unlimited` → unlimited; `cost == 0` → free; `freeDirections` contains `return` → oneFree; else credits. `MemberWizardModel.credits`, `needsReadyReview`, `canSave`, `readyTick(force)`, `resumeCompletion()`, `savePlan(recovering)`, `revisit(step, sleep)`, `openPrompt(id)`, `optionalPrompts`, `reviewInput`, `isEditing`, `hasChanges`, `showsReturnRow`; snapshot fields `reviewing`, `retrySeconds`, `createdTripID`, `closed`, `persistenceFailed`; state fields `review`, `saveFailure: WizardSaveFailure?` (`kind: offline|server|throttle`, `retryAt`), `updated`, `profile`, `savedProfile`, `savedTripId`, `firstTrip`; `WizardPresentation.flightSummary`, `returnSummary`, `extrasSummary`, `planDateSummary`, `planDayCount`, `promptAnswerText`; the board progress `ready = 0.95`, `calculating = 1.0`. [Verified 2026-11-20: all present in `…/cw/model/` (`WizardCredits.kt`, `WizardSaveFailure.kt`, `MemberWizardCompletion.kt` l.49–345, `WizardMode.kt:90` `showsReturnRow`, `WizardPresentation.kt` l.182–392, `MemberWizardReview.kt:59–60`). The presentation functions also take `language, t` (Refresh note 3). `readyTick` has no freeze guard, as this brief expects.]
2. **9b:** `TripWizardViewModel` with `state` (`Loading`/`Active`/`LoadFailed`/`Closed(createdTripId)`, `…/fw/TripWizardUiState.kt`), `wizard`, and an `onCleared` that never discards (it calls `wizard?.close()` and `generation?.cancelNow()`); `MemberWizardStore.didSave(instance, firstTrip)` = `FirstTripRemindersOffer.queue(firstTrip)` then `discard(instance)`; `TripGenerationEffects.storing` (pull created trip, request sync); the conflated writer; 9b's `WizardSaveRecoveryTests`, `WizardCompletionTests`, `WizardFixtureScenarioTests.saveIsNeverRetriedWithoutUserAction` green (all in `core/data/src/test/kotlin/net/chronoflow/core/data/wizard/`). `MemberWizardStore` is `core/data/…/wizard/MemberWizardStore.kt` (`didSave` l.114–120). `FirstTripRemindersOffer` is `core/data/…/reminders/FirstTripRemindersOffer.kt`. `TripGenerationEffects.storing` is `core/data/…/wizard/TripGenerationEffectsStore.kt:24`. **Do not re-port or duplicate these model tests** (Refresh note 13).
3. **9c** (actual shapes in Refresh notes 2, 5–8 and 10):
   - `WizardRoot(viewModel, onExit: (WizardExit) -> Unit)` and `WizardScaffold(state, onEvent, copyFormatter)`. The footer is `WizardFooter(chrome, step, onEvent)` with tags `wizard.<id>.footer` / `wizard.<id>.continue`, `continueEnabled`, and alpha `PlanUITokens.wizardDisabledOpacity` (0.4) when disabled and not `routeComplete`.
   - `WizardChromeUi(navTitle, showBack, showClose, …)`.
   - `WizardStepHost` (`NavDisplay` over `listOf(Where) + state.path`).
   - `WizardStepSeam` (the `ready` and `calculating` keys are still seams).
   - `enum class WizardExit { Trips }`.
   - `ShellViewModel.closeWizard(exit)` (the shell `holder`; no class is named `ShellHolder`).
   - `vm.requestClose()` and the `RequestClose`/`CloseFromFooter` events with the discard dialog.
   - `WizardBoardCaptureSeed` (fixed clock 2026-10-01T09:00Z; `WizardMemoryDraftStore`).
   - `WizardStartOverride` (`app/src/main/kotlin/net/chronoflow/app/wizard/WizardStartOverride.kt`).
   - `WizardGuardsTest`, `WizardBoardCaptureTest` (both in `feature/wizard/src/testDebug`) and `WizardJourneyTest` (`app/src/androidTest/…/app/wizard/`).

   All present.
4. **9d:** `WizardBanner(body, testTag, modifier, tone: WizardBannerTone = Error, title)` (`…/fw/components/WizardBanner.kt:32`; `testTag` is the required second argument). Server `CopyLine`s use `rememberServerCopy(formatter)`/`serverCopy(line, formatter, language)`, and `FieldMessage`s use `fieldCopy(…)` (`…/fw/components/WizardServerCopy.kt`). There is no `WizardFieldCopy`.
5. **9f** (brief `phase-9f-wizard-checking-finetune.md`):
   - The Fine-tune step (`wizard.fine-tune`, `wizard.fine-tune.continue`). `continueFrom(FineTune)` → `finishFineTune()` → `ready`, which today renders the `wizard.seam.ready` placeholder.
   - The busy footer is `WizardChromeUi.primaryBusy` + `primaryLabel` (`CfButton(loading = true)`, alpha `WizardMetrics.ctaBusyAlpha` 0.7, Indeterminate semantics). There is no `WizardFineTuneBusy` composable; that name is only the board.
   - The completion-board seeds for Fine-tune and the sheets (`…/fwd/WizardCompletionBoardSeed.kt`).
   - The board freeze seam `WizardUiOverride(checking, retrySeconds, reviewing, answerError)` in `…/fw/shell/WizardUiOverride.kt`, with `TripWizardDependencies.uiOverride` (default null). The value comes from `WizardCompletionBoardSeed.uiOverride(board)`, forwarded by `WizardDebugSeeds` in `app/src/debug`.
   - The ViewModel rule: a non-null override is applied in `WizardUiState.from` (`effective`), and the VM skips `resumeChecking()`/`resumeCompletion()` (VM l.331–334).
   - Haptics are a `WizardHapticCue` played by `WizardScreen` (Refresh note 4), not a `haptics` flow.
   - The lockout text is built inline in `WizardCheckingUi.failureBody` from `LockoutWait` (Refresh note 9).

   If the seam is missing, stop and report (do not add a model-level freeze).
6. **6a/6c:** `ShellNav.wizardSaved(state, tripId)` = `overlay = null`, `selected = Trips`, Trips stack `[Trips, TripPage(tripId, awaitingSync = true)]` (`ShellNav.kt:71–81`). `ShellViewModel.wizardSaved(tripId)` wraps it, and nothing calls it yet. `Route.TripPage(tripId, dayId, blockId, awaitingSync, …)` is in `core/navigation/…/Route.kt:60`. The trip page root tag `trip.page` is at `feature/trip/…/page/TripPageScreen.kt:315`. All present.
7. **4c:** `MockDataSource` save receipts ("a retry with the same identity returns the same id"; `MockServerState.wizardReceipts`, 1-day expiry). `credits` 3 on the demo account (`fixtures/mock-seed.json`). `MockMethod.SaveTrip` and `failNext(method, error)` exist. There is **no** call counter, so count through a `DataSource` decorator in the test.
8. **0c:** resources exist for every key in §5.9 (`tools/l10n/names.lock.json`, table `wizard`); `{{count}} credits`, `You have {{count}} credits` and `{{count}} days of steps` are `<plurals>` (folded `_one` pairs). [Verified 2026-11-20: every §5.9 key is in `names.lock.json` as `wizard\t<key>`; "Buy credits on the website" is absent.]

---

## 5. Spec

### 5.1 Ready layout (`WizardReadyStep`, `ReadyMetrics` `@BoardMetrics("Wizard")`)

Rendered inside 9c's `WizardScaffold` with footer id `ready` (`WizardChromeUi.footerId`) and root tag `wizard.ready`. The step composable sets its own root tag, as `WizardCheckingStep` (`wizard.checking`) and `WizardFineTuneStep` (`wizard.fine-tune`) do. Title "Ready to plan" (no subtitle). Content items in this order, gap `wizardContentGap` (14):

1. **Banner** (optional, §5.3), 9d `WizardBanner`.
2. **Summary card** (tag `wizard.ready.summary`), only when `review.summary != null` [F `MemberWizardReadyView.swift:35–57`; board l.178–182]:
   - Column, gap 6 (`wizardControlGap`, board; iOS uses `space2` 8, OQ-9g-7), padding 16 vertical (`space4`) / 18 horizontal (`wizardReadySummaryPadding`), radius 26 (`radius3Xl`), fill `card`, border **2 dp `dawn`**, soft shadow (Phase 2's `Modifier.softShadow`, `core/designsystem/…/effect/SoftShadow.kt`).
   - Eyebrow "Your plan" upper-cased with the app language's locale, `screenEyebrow` (Figtree 600 12/16, tracking 0.1 em), `dawnText`.
   - Summary: the server `summary.line` through the PlanCopy resolver, `wizardReadySummary` (Fraunces 20/27), `foreground`.
   - Dates: `WizardPresentation.planDateSummary(form, summary, language, t)`, `wizardHint` (13/18), `mutedForeground`, tabular numbers (`fontFeatureSettings = "tnum"`).
3. **Rows card** [board l.183–185, iOS l.58–110]: radius 16 (`wizardCardRadius`), fill `card`, border 1 dp `border`, clip. Rows separated by a 1 dp `border` divider (none above the first). Each row:
   - `Row`, min height 48 (`actionMinHeight`), padding start 16, end 10, vertical 5, gap 8, vertically centred;
   - label width 92 (`wizardReadyLabelWidth`), `screenBody` (14/20), `mutedForeground`;
   - value weight 1, `screenBody`, `foreground`, tabular numbers, wraps;
   - trailing lucide `chevron-right` 16 dp in the same colour 9d/9f use for card chevrons;
   - **large text** [R]: when `LocalDensity.current.fontScale >= 1.2f`, label above value (gap `wizardLabelGap` 2) instead of side by side (iOS: vertical above `.xLarge`, `Docs/wizard-16c.md`);
   - whole row is one `cfClickable` with `Role.Button`; `semantics(mergeDescendants = true)`; `onClickLabel` = "Opens this step"; the content description is "<label>, <value>" (board `a11y` l.444: `label + ', ' + value + '. ' + rowHint`). The Latest wake-up row has no "Opens this step" hint (iOS l.95–108 sets none);
   - disabled while `reviewing` (iOS `.disabled(model.reviewing)`);
   - tap → `Selection` haptic → the row action. Exception: the Latest wake-up row fires **no** haptic (iOS l.95–98 calls `revisit` + `openPrompt` only).

   | Row | Tag | Value | Action |
   |---|---|---|---|
   | Route | `wizard.ready.route` | `routeSummary` = `legs[0].fromIata + " → " + legs[returnBoundary − 1].toIata` | `revisit(whereTo)` |
   | Flight | `wizard.ready.flight` | `WizardPresentation.flightSummary(form, language, t)` | `revisit(outbound)` |
   | Return (only if `showsReturnRow`) | `wizard.ready.return` | `WizardPresentation.returnSummary(form, stayEnd = review.summary?.stayEnd ?: snapshotStayEnd, …)` | `revisit(returnChoice)` |
   | Sleep | `wizard.ready.sleep` | `sleepSummary` (§5.2) | `revisit(fineTune, sleep = true)` |
   | Extras | `wizard.ready.extras` | `WizardPresentation.extrasSummary(form, review, reviewInput, …)`; a throw → "" | `revisit(fineTune)` |
   | Latest wake-up (only if `review.status == modified` and `optionalPrompts` has a `latest_wake` prompt) | `wizard.ready.latest-wake-up` [R; iOS has no id] | "{{time}} · assumed" with `time = promptAnswerText(prompt, reviewInput)` (a throw → "") | `revisit(fineTune)` then `openPrompt(prompt.id)` |

4. **Price block** (tag `wizard.ready.price`), only when `review.price != null`; padding horizontal 4, gap 2 [board l.186–189]. §5.2.

The 1 s tick and the foreground re-check (§5.5) run while this step is composed.

### 5.2 Ready values (pure; `ReadyUiMapper`)

**`sleepSummary`** (member; port of iOS l.137–156): `bed` = override enabled ? `override.habitualBed` : `profile["habitual_bed"] ?: savedProfile["habitualBed"]`; `wake` likewise with `habitual_wake`/`habitualWake`. `times` = the non-null of `[bed, wake]` joined with "–" (U+2013). Result: override enabled → "This trip · {{times}}"; else `times`.

**Price value** (iOS `price`, l.157–166; create mode): `price.unlimited` → "Included in your subscription."; `cost == 0` → "Free"; `cost == 1` → "{{count}} credit"; else "{{count}} credits" (plural resource, quantity per 0c §11: `countText == "1"` → one, else other).

**Price block lines**, in this order (iOS `priceBlock`, l.173–215, member create). Board look: gap 2, padding horizontal 4, line 0 in `foreground` (iOS uses gap `wizardControlGap` 6 and inherits `mutedForeground` for line 0; board wins on looks, OQ-9g-7):

| # | Line | Shown when | Style |
|---|---|---|---|
| 0 | `["Price" + " "] + **value**` | always (the "Price " prefix is omitted when `credits == unlimited`) | `wizardSubtitle` (16/22), `foreground`; value weight 600 |
| 1 | "This trip is free: only a small body-clock shift is needed." | `credits == free` | `wizardHint` (13/18), `mutedForeground` |
| 2 | "Your flight home is free: only a small shift is needed that way." | `credits == oneFree` | same |
| 3 | "You have {{count}} credits" (plural, `count = price.balance`) | `!price.unlimited && price.cost > 0` | `credits == unavailable` → weight 600, `destructive`; else `mutedForeground` |

Never hard-code the balance or a sign-up credit count (OQ-A18). The fixture `wizard/review.json` (cost 1, balance 1) must render "Price **1 credit**" and "You have 1 credit".

**Expected results per board** (the JVM test table, Task 2):

| Board | `price` (state, cost, balance, unlimited, freeDirections), `canSave`, `saveBlockedReason` | `credits` | Line 0 | Lines |
|---|---|---|---|---|
| WizardReady | create, 1, 3, false, [], true, null | credits | Price **1 credit** | You have 3 credits |
| WizardReadyReturn | create, 2, 3, false, [], true, null | credits | Price **2 credits** (OQ-9g-3) | You have 3 credits |
| WizardReadyUnlimited | unlimited, 0, 3, true, [] | unlimited | **Included in your subscription.** | — |
| WizardReadyFree | free, 0, 3, false, [] | free | Price **Free** | free line |
| WizardReadyOneFree | create, 1, 3, false, [return] | oneFree | Price **1 credit** | one-free line; You have 3 credits |
| WizardReadyNoCredits | create, 1, 0, false, [], false, "Not enough credits" | unavailable | Price **1 credit** | **You have 0 credits** (destructive 600) |
| (no board) `price == null` | — | unavailable | no price block | — |

### 5.3 Banner slot (priority, first match wins; iOS l.19–34)

| Priority | Condition | Tone | Title | Body | Tag |
|---|---|---|---|---|---|
| 1 | `state.saveFailure != null` | Error | per §5.4 | per §5.4 | `wizard.save.<offline\|server\|throttle>` |
| 2 | `state.sourceChange != null` | Info | — | **9h** (do not build; skip this branch) | — |
| 3 | `review.status == modified` | Info | "We adjusted your plan" | `review.reasons` through the PlanCopy resolver, joined with one space | `wizard.ready.notice` [R] |
| 4 | `state.updated == true` | Info | "Updated" | `updatedSummary` = `[summaryLine, "Price" + ": " + priceValue]` dropping empties, joined with " " | `wizard.ready.notice` [R] |

The adjusted body is the server reason, not the board's illustrative "We assumed you can wake up at 08:00 …" (iOS parity; record the visual difference in `docs/wizard.md`).

### 5.4 Save-failure banner copy (iOS `saveFailureBanner`, l.274–300, create mode)

| Kind | Title | Body |
|---|---|---|
| offline | "Saving needs a connection" | "Connect to the internet and tap Make my plan again. Keep this screen open: closing discards the trip." |
| server | "We couldn't save your plan" | "Something went wrong on our side. Tap Make my plan to try again." |
| throttle (no board) | "Wait a moment" | `retrySeconds > 0` → the lockout string "Too many attempts. Try again in %lld seconds." / minutes / hours via 5a's `LockoutWait` (reuse 9f's inline pattern in `WizardCheckingUi.failureBody`: `WizardCopy(LockoutWait(retrySeconds).localizationKey, count)`; there is no `WizardStrings.lockout`); else "Too many attempts. Please try again later." |

When `saveFailure` changes to a non-null value (null → failure, or one failure to a different one), fire the `Error` haptic once (iOS `onChange(of: saveFailure)`, l.122–124: it fires on every change whose new value is non-nil). Emit it through 9f's haptic cue: extend `checkingOutcome(previous, snapshot)` (or a sibling the VM calls beside it) so the VM sets a new `WizardHapticCue(CheckingOutcome.Error, sequence + 1)`; it compares consecutive snapshots and returns null for the first one, so the first snapshot (a recovered draft that already holds a failure) does not buzz.

### 5.5 Footer, button and ticks

**Primary button** (9c footer, id `ready`, tag `wizard.ready.continue`), shown unless `credits == unavailable`:

| State | Label | Enabled | Alpha | Spinner |
|---|---|---|---|---|
| `reviewing` | "Checking…" | no | **0.7** (board `ctaBusy`, l.522; 9f's busy footer) | yes, `primaryForeground` |
| `saveFailure.kind == throttle` and `retrySeconds > 0` | "Try again in {{count}} s" (`count = retrySeconds`) | no | 0.4 | no |
| `saveFailure.kind == throttle` and `retrySeconds == 0` | "Try again" | per `canSave` | 1 / 0.4 | no |
| otherwise | "Make my plan" | `canSave` | 1 / 0.4 | no |

- `enabled = canSave && credits != unavailable` plus 9c's shell guards (`closing`, `continuing`, `persistenceFailed`, `reviewing`). Under a board override (9f `WizardUiOverride`) the mapper uses the override's `reviewing` and `retrySeconds` for the label, spinner and `enabled` (model `canSave` alone would still say true for `WizardReadyChecking`).
- Tap → `Medium` haptic → `onEvent(WizardEvent.Continue(Ready))` (or a new `MakePlan` event) → VM → `wizard.savePlan()`. `savePlan` itself re-reviews in place when `needsReadyReview` (9a); never call the service from the UI.
- Read `retrySeconds` only inside the button's label lambda, so the 1 s countdown recomposes the button and nothing else.

**No-credits footer** (`credits == unavailable`) [F `MemberWizardSteps.swift:108–121`; A14]:
- Footer tag `wizard.ready.footer` (padding horizontal 20 / vertical 12, iOS `space5`/`space3`); one full-width text button "Close", tag `wizard.ready.no-credits`, min height 48 [R; iOS `AuthTokens.touch` 44], `wizardSecondary` (Figtree 500 16), `foreground`, disabled while `closing`, above the safe bottom (9c footer insets).
- No primary button, no "Buy credits on the website", no link, no `SetPaywall` route.
- Tap → `onEvent(WizardEvent.CloseFromFooter)` (existing; it mirrors iOS `MemberWizardSteps.swift:111`: close at once for guest or no changes, else the discard alert). Create has changes, so the discard alert opens; Keep editing returns to Ready.

**Ready tick** [F iOS l.113–118]: `LaunchedEffect(Unit) { while (true) { onEvent(WizardEvent.ReadyTick); delay(1_000) } }` inside `WizardReadyStep` (new event; Refresh note 1). It stops when the step leaves composition. The VM's handler is a no-op while `deps.uiOverride != null` (port of iOS `if boardFrozen { return }`, `MemberWizardCompletion.swift:155`), so frozen boards never re-review.

**Foreground re-check** [F iOS l.119–121; WizardSpec "A no-credits Ready checks silently when the app comes back to the front"]: on each `Lifecycle.Event.ON_START` **after the first** one seen by this composition (iOS `onChange(of: scenePhase)` does not fire on appear), send the new foreground event, which the VM maps to `readyTick(force = credits == unavailable)`. Use `LifecycleEventEffect` with a `remember { BooleanArray(1) }` first-event guard, or `lifecycle.currentStateFlow` with `drop(1)`.

### 5.6 Calculating (`WizardCalculatingStep`, `CalculatingMetrics` `@BoardMetrics("Wizard")`)

- Root tag `wizard.calculating`, set by the step composable (no footer id: `footerId` stays null). Title "Calculating your plan…", subtitle "This can take up to a minute. You can keep this screen open." No footer. Progress 1.0.
- **No ×, no back chevron** (9c `WizardChromeUi.showClose = step != calculating`; 9a `showsBack` is false). **System and predictive back do nothing**: `BackHandler(enabled = true) {}` inside `WizardCalculatingStep` or beside the others in `WizardBackHandling.kt` (non-predictive, so no preview animation), and the step host's `onBack` ignores the step. Already present: × hidden (`WizardUiState.kt:314`), chevron hidden (VM sets `showBack = showsBack`, false here), no footer (`footerId` null), and `popNavigation` rejects `calculating` (`MemberWizardNavigation.kt:104`). Only the consuming `BackHandler` is new.
- Card (tag `wizard.calculating.card`) [board l.193–198]: column, centred, gap 16, padding 32 vertical / 20 horizontal, radius 26, fill `card`, border 1 dp `border`, soft shadow, text centred.
  - Icon disc 64 dp, `CircleShape`, fill `dawn` at 16 % alpha (a named constant in `CalculatingMetrics`), content `dawnText`: Phase 2 `CfSpinner(size = 30.dp, color = dawnText)` (it has no stroke parameter; keep its stroke). Under Reduce Motion (`LocalReduceMotion`): a static lucide `clock` at 24 dp (`wizardFailureIconSize`) in `dawnText` instead (iOS l.307–309).
  - Line: `routeSummary + " · " + "{{count}} days of steps"` (plural, `count = WizardPresentation.planDayCount(review?.summary) ?: 0`), `wizardFailureBody` (15/22), `mutedForeground`.
- Accessibility: 9c moves focus to the title on step change. Also mark the title `liveRegion = LiveRegionMode.Polite` on this step so TalkBack reads "Calculating your plan…" (iOS posts an announcement, l.325; `announceForAccessibility` is deprecated, AGENTS Traps).

### 5.7 Save, resume and hand-off

- **Save**: 9a `savePlan()` (Ready) → `step = calculating`, persist, `saveJob` (§6.7 of the 9b brief). The UI only renders `step`.
- **Resume after process death** (plan "state survival"; `06 §4.7`): 9b's VM `init` calls `start()` (recover the `member-wizard` row, ≤ 30 min) and then `resumeChecking()` + `resumeCompletion()` once. A row at `calculating` re-runs `savePlan(recovering = true)` with the stored `savedTripId` (skips the POST) or the stored identity (same `draft_token`, `input_revision`, `review_revision` → the receipt returns the same trip). A row at `ready` forces one re-check. Nothing new to code here; Task 7 proves it end to end. Quiet recovery reopens the wizard (9c `WizardRecoveryCheck`), with no message.
- **Hand-off** [F `MemberWizardView.swift:102–107`, `AppTabs.swift:170–186`]: when `TripWizardViewModel.state` becomes `Closed(createdTripId != null)`:
  1. fire `Success` haptic once;
  2. call `onExit(WizardExit.Saved(tripId))` exactly once (guard with a `rememberSaveable` flag keyed by the trip id, or consume a one-shot in the VM);
  3. `ShellViewModel.closeWizard(WizardExit.Saved(id))` → `ShellViewModel.wizardSaved(id)` → `ShellNav.wizardSaved(state, id)`, only while `overlay is Route.Wizard` (Refresh note 6): overlay cleared, My trips selected, Trips stack `[Trips, TripPage(id, awaitingSync = true)]`. 6c's trip page shows Loading/remote states until the record arrives (`awaitingSync` suppresses NotFound).
- **First-trip reminders offer**: 9b's `MemberWizardStore.didSave(instance, firstTrip)` writes settings `wizardReminders = "\"pending\""` when `state.firstTrip` (no trip existed when the wizard opened) and deletes the recovery row. 9g adds no code; Task 7 proves it at app level. 12c consumes it.
- `Closed(null)` (× or discard) keeps 9c's `WizardExit.Trips`.

### 5.8 Haptics (through `LocalHaptics`, plan "Haptics" table)

| Event | `HapticEvent` | Android constant |
|---|---|---|
| Ready row tap (not Latest wake-up) | Selection | `SEGMENT_TICK` |
| "Make my plan" / "Try again" tap | Medium | `LONG_PRESS` |
| `saveFailure` changes to a non-null value (via 9f's `WizardHapticCue`) | Error | `REJECT` |
| Trip saved (`Closed(id)`) | Success | `CONFIRM` |

### 5.9 Copy (all from generated resources; `Text` receives resources only)

Wizard catalog keys used (en / nl, verified in `Kit/TripWizardUI/Resources/Localizable.xcstrings` at the pin):

| Key | nl |
|---|---|
| Ready to plan | Klaar om te plannen |
| Your plan | Je plan |
| Route / Flight / Return / Sleep / Extras / Latest wake-up | Route / Vlucht / Terug / Slaap / Extra's / Uiterlijk wakker |
| {{time}} · assumed | {{time}} · aangenomen |
| This trip · {{times}} | Deze reis · {{times}} |
| Opens this step | Opent deze stap |
| Price / Free | Prijs / Gratis |
| {{count}} credit | {{count}} credit |
| {{count}} credits (+ `_one` "{{count}} credit") | {{count}} credits / {{count}} credit |
| Included in your subscription. | Inbegrepen in je abonnement. |
| This trip is free: only a small body-clock shift is needed. | Deze reis is gratis: je klok hoeft maar een klein beetje te verschuiven. |
| Your flight home is free: only a small shift is needed that way. | Je vlucht naar huis is gratis: die kant op is maar een kleine verschuiving nodig. |
| You have {{count}} credits (+ `_one` "You have {{count}} credit") | Je hebt {{count}} credits / Je hebt {{count}} credit |
| Make my plan | Maak mijn plan |
| Checking… | Controleren… |
| Try again / Try again in {{count}} s | Opnieuw proberen / Opnieuw proberen over {{count}} s |
| Close | Sluiten |
| We adjusted your plan / Updated | We hebben je plan aangepast / Bijgewerkt |
| Saving needs a connection | Voor opslaan is een verbinding nodig |
| Connect to the internet and tap Make my plan again. Keep this screen open: closing discards the trip. | Maak verbinding met internet en tik opnieuw op Maak mijn plan. Laat dit scherm open: sluiten gooit de reis weg. |
| We couldn't save your plan | We konden je plan niet opslaan |
| Something went wrong on our side. Tap Make my plan to try again. | Er ging iets mis bij ons. Tik op Maak mijn plan om het opnieuw te proberen. |
| Wait a moment | Even geduld |
| Too many attempts. Please try again later. | Te veel pogingen. Probeer het later opnieuw. |
| Calculating your plan… | Je plan wordt berekend… |
| This can take up to a minute. You can keep this screen open. | Dit kan tot een minuut duren. Je kunt dit scherm open laten. |
| {{count}} days of steps (+ `_one` "{{count}} day of steps") | {{count}} dagen met stappen / {{count}} dag met stappen |

Model-emitted keys (row values, dates) are resolved with `CopyIndex.find(CopyTable.Wizard, key)`; server `CopyLine`s (summary, reasons) with the PlanCopy resolver (9d's field-copy resolver order). Unknown keys show the generic fallback (0c §11), never raw English.

**Android-only copy** (OQ-9a-8 = plan OQ-A356): iOS `flightSummary` emits `"{{flight}} · via {{airports}}"` (`…/cw/model/WizardPresentation.kt:289`), but the iOS catalog lacks it. Add an entry to `tools/l10n/android-only.json` (`{"entries": [...]}`):
- name `android_wizard_flight_via`;
- en `{{flight}} · via {{airports}}` and nl `{{flight}} · via {{airports}}` (board nl `rv.flightVia` "ma 12 okt 14:40 → di 16:35 · via DXB");
- `replaces: []`;
- `review: "needs review"`;
- a `comment` naming OQ-A356.

[Refresh 2026-11-20] Two generator rules shape this entry:
- **Placeholders.** The generator's placeholders are `{{name}}` or iOS `%N$@`/`%lld` (`tools/l10n/xcstrings2android.py:54–55`). `%1$s` is not a placeholder and would be emitted literally.
- **`replaces`.** It must name a key that exists in the iOS catalog, or the generator fails ("replaces unknown iOS key", l.352–355). This key is absent at the pin, so `replaces` stays empty.

The generated `CopyIndex` lists overlay entries under `CopyTable.Android`, keyed by their en text (for example `"Airport or city"`), so this entry's key equals the model key. `WizardCopy.resolve()` (`…/fw/ui/WizardCopy.kt`) searches only `CopyTable.Wizard` and needs a fallback to `CopyTable.Android` (or an explicit mapping) for this key.

Then: remove 9a's test exception for that key (`WizardModelCopyKeys`/`WizardCopyKeysTest`); add the row to `plan.md` "Android-only copy awaiting approval". The board writes `· via DXB` with the airports comma-separated ("via DXB, HEL", `06` A.4).

No other new string. "Buy credits on the website" is **not** used.

---

## 6. Kotlin shape [R]

| Class / function | Module, package | Notes |
|---|---|---|
| `ReadyUi(summary: ReadySummaryUi?, rows: List<ReadyRowUi>, price: ReadyPriceUi?, banner: ReadyBannerUi?, footer: ReadyFooterUi)` | `:feature:wizard`, `…wizard.ready` | `@Immutable`; a new `ready: ReadyUi?` field of `WizardUiState`, built in `WizardUiState.from` from `effective` (Refresh note 2; that is how `checking`/`fineTune` are built) |
| `ReadySummaryUi(line: CopyLine, dates: …)`, `ReadyRowUi(kind: ReadyRow, value: …)`, `enum ReadyRow { Route, Flight, Return, Sleep, Extras, LatestWake }` | same | No `TextUi`/`CopyLineUi` exists (Refresh note 3). Use `WizardCopy(key, args)` for catalog/model keys, `CopyLine` for server lines (resolved by `rememberServerCopy`) and 9f's `AnswerText` capture pattern for `WizardPresentation` values. Never pre-formatted English |
| `ReadyPriceUi(showLabel: Boolean, value: PriceValue, lines: List<PriceLine>)`; `sealed PriceValue { Unlimited; Free; OneCredit; Credits(n) }`; `sealed PriceLine { FreeTrip; FreeHome; Balance(n, alert: Boolean) }` | same | Pure mapping of §5.2 |
| `ReadyBannerUi` sealed: `SaveFailed(kind, retrySeconds)`, `Adjusted(reasons: List<CopyLine>)`, `Updated(summary: CopyLine?, price: PriceValue?)` | same | §5.3 |
| `ReadyFooterUi` sealed: `Primary(label: ReadyButtonLabel, enabled: Boolean, busy: Boolean)`, `CloseOnly` | same | §5.5 |
| `object ReadyUiMapper { fun map(snapshot, model facts…): ReadyUi; fun sleepSummary(…); fun routeSummary(form) }` | same | Pure Kotlin, no Compose import (JVM test) |
| `CalculatingUi(route: String, days: Int)` | `…wizard.calculating` | |
| `WizardReadyStep`, `ReadyRowsCard`, `ReadySummaryCard`, `ReadyPriceBlock`, `ReadyFooter`, `ReadyMetrics` | `…wizard.ready` | |
| `WizardCalculatingStep`, `CalculatingMetrics` | `…wizard.calculating` | Step entries named like 9f's `WizardCheckingStep`/`WizardFineTuneStep` |
| `TripWizardViewModel` + `WizardEvent` additions | `…wizard`, `…wizard.shell` | New `WizardEvent` cases handled in `onEvent` (Refresh note 1): make plan (or `Continue(Ready)` handled in `continueFrom`), revisit from a Ready row (`row: ReadyRow`), ready tick, ready foreground. Each is guarded on `closing`. Tick and foreground return at once while `uiOverride != null` (the VM's private `uiOverride`, VM l.106). `ready`/`calculating` come through `uiState` (not separate StateFlows) and use `effective`, so the override's `reviewing`/`retrySeconds` apply (9f rule). The saved exit replaces `exitOnce()`'s hard-coded `Trips` with `Saved(createdTripID)` when the save finished (Refresh note 5); no separate `savedEvents` flow is needed |
| `WizardExit.Saved(tripId: String)` | `…wizard.shell` (`WizardStepKey.kt:31`) | 9c's `WizardExit` **is** `enum class WizardExit { Trips }`: turn it into a sealed interface (`data object Trips`, `data class Saved`). Callers: `ShellViewModel.closeWizard` and `ShellViewModelOverlayTest` |
| `ShellViewModel.closeWizard(WizardExit.Saved)` → `ShellViewModel.wizardSaved` (exists) | `:app` `net.chronoflow.app.shell` | `ShellViewModel.kt`, edited |
| `WizardCompletionBoardSeed` (extend 9f's `baseNames`/`names`/`uiOverride`/`apply`), `WizardSaveJourney` in `WizardBoardCaptureSeed` (accepted by `knows`, kept out of `names`) plus its own branch in `WizardDebugSeeds.prepare` | `…fwd/`, `app/src/debug/…/debug/WizardDebugSeeds.kt` | debug only (Refresh note 10) |
| `WizardUiOverride(reviewing = …, retrySeconds = …)` for the Ready boards | returned by `WizardCompletionBoardSeed.uiOverride(board)`, which `WizardDebugSeeds` forwards | 9f's seam, reused; no new model method |

---

## 7. Tasks (one commit each, in order; Conventional Commits, lower-case imperative, **no trailer**)

Tick the plan box named in brackets in the same commit. Tests live in `…/fwt/` unless a task says otherwise.

### Task 1: `feat(l10n): add the android-only via flight copy` [no box]

- `tools/l10n/android-only.json` row (§5.9), regenerate with `make strings`, commit the generated `values/` and `values-nl/` files with it.
- Map the model key `{{flight}} · via {{airports}}` to `android_wizard_flight_via` in `WizardCopy.resolve()` (`…/fw/ui/WizardCopy.kt`). `CopyIndex.find(CopyTable.Wizard, key)` misses it, so fall back to `CopyIndex.find(CopyTable.Android, key)`: the overlay row is keyed by its en text, which equals the model key.
- Remove that key from 9a's test exception. It lives in `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/model/WizardCopyKeysTest.kt` l.49–53 (`absent`, asserted **absent** from `names.lock.json`) and is listed in `WizardModelCopyKeys.kt:53`. `everyListedKeyResolves` must then accept the key through the overlay. `names.lock.json` holds no overlay rows, so read `tools/l10n/android-only.json` `entries[].en` (declare it as a task input beside the other two files).
- `plan.md` "Android-only copy awaiting approval": add the row (context "Ready flight with stops").
- **Tests:** `WizardCopyKeysTest.everyListedKeyResolves` (9a's, JVM) passes with no exception; `make generators-check` passes.

### Task 2: `feat(wizard): map ready and calculating ui state` [part of "Ready"]

- Files: `…/fw/ready/ReadyUi.kt`, `ReadyUiMapper.kt`, `…/fw/calculating/CalculatingUi.kt`. `WizardUiState` gains `ready`/`calculating`, built in `WizardUiState.from`; `WizardChromeUi.from` gains the Ready footer (`footerId = "ready"`, label, busy, enabled). `WizardEvent` gains make plan, revisit-from-ready, ready tick and ready foreground, handled in `TripWizardViewModel.onEvent`. Tick and foreground are no-ops while `uiOverride != null`. 9f's `WizardHapticCue` gains the §5.4 `Error` for `saveFailure` (Refresh notes 1, 2, 4, 7).
- `revisitFromReady`: `Route` → `revisit(whereTo)`; `Flight` → `revisit(outbound)`; `Return` → `revisit(returnChoice)`; `Sleep` → `revisit(fineTune, sleep = true)`; `Extras` → `revisit(fineTune)`; `LatestWake` → `revisit(fineTune)` then `openPrompt(latestWakePrompt.id)`.
- `readyForeground()` = `readyTick(force = credits == unavailable)`.
- Edge cases: `review == null` on Ready (only after a decode of an old row) → no summary, no price, `credits == null` → treat as `Primary("Make my plan", enabled = canSave)`; `summary == null` → no summary card and Calculating `days = 0`; `price == null` → no price block and `CloseOnly` footer (`credits == unavailable`); `isEditing == true` → this phase maps nothing edit-specific (9h); the mapper must not crash.
- **Tests (JVM, plain, `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/`, the flat package the 9c–9f tests use; VM tests reuse `WizardTestSupport.kt`):**
  - `ReadyPriceUiTest` (parameterised over the §5.2 board table, one case per row): builds an `ApiTripReview` from `fixtures/wizard/review.json` with the row's `price`/`canSave`/`saveBlockedReason`; asserts `credits`, `showLabel`, `value`, `lines` (order and `alert`). Plus `reviewFixtureRendersOneCreditAndSingularBalance` (unchanged `review.json`: `OneCredit`, `Balance(1, alert = false)`) and `priceNullHasNoPriceBlockAndCloseOnly`.
  - `ReadyCreditsSaveFailureTest`: a fake generation whose `save` returns 9b's `TripGenerationSaveResult.Outcome.ReReviewNeeded(Reason.notEnoughCredits)` (the `wizard/credits.json` path) and whose next review returns the no-credits review → `footer == CloseOnly`, price line `Balance(0, alert = true)`, `form.identity` was cleared once. Run the model in `backgroundScope` with 9a's manual clocks (9b brief §5.4 #12).
  - `ReadyBannerUiTest`: priority order (save failure beats adjusted beats updated); adjusted joins two reasons with one space; `updatedSummary` drops an empty summary; throttle with `retrySeconds = 42` → `SaveFailed(throttle, 42)`; 0 → later-copy variant.
  - `ReadyFooterUiTest`: the §5.5 table, including `reviewing` → busy, throttle 42 → disabled countdown label, unavailable → `CloseOnly`.
  - `ReadyRowsUiTest`: row order and presence (Return only when `showsReturnRow`; LatestWake only for `modified` with a `latest_wake` prompt); `routeSummary` uses `legs[returnBoundary − 1]` on a return trip ("AMS → NRT", not "… → AMS"); `sleepSummary` cases: override on → "This trip · 00:30–08:30", profile only, saved profile fallback, one missing value → no dash.
  - `TripWizardViewModelReadyTest`: `revisitFromReady` maps each row to the right model call (a recording fake model or assert the resulting `step`); `readyForeground` passes `force = true` only for unavailable; with `deps.uiOverride` set, the tick and foreground events make no review call, and `uiState.value?.ready?.footer` follows the override (`reviewing = true` → busy, `retrySeconds = 42` → disabled countdown). The `Active.cue` gets a new `Error` sequence for `saveFailure` null → offline and offline → server, and none when the first snapshot already holds a failure. A finished save sets `exit.value == WizardExit.Saved(id)` (not `Trips`).

### Task 3: `feat(wizard): add the ready step` [ticks "Ready"]

- Files: `WizardReadyStep.kt`, `ReadySummaryCard.kt`, `ReadyRowsCard.kt`, `ReadyPriceBlock.kt`, `ReadyFooter.kt`, `ReadyMetrics.kt` in `…/fw/ready/`. Replace the `ready` seam in `WizardStepContent` (`WizardScreen.kt` l.435–486, which `WizardStepHost` renders). The step is stateless (`WizardReadyStep(ui, onEvent, …)`).
- Behaviour exactly §5.1–§5.5 and §5.8. Banner via 9d `WizardBanner`. Footer via `WizardFooter`/`WizardChromeUi`: 9f's `primaryBusy`/`primaryLabel` are **already present**, so add no `busy` parameter. Add only the Close-only variant (Refresh note 7).
- The no-credits footer replaces the whole primary footer (§5.5) and must sit inside the safe bottom inset.
- **Tests (Robolectric, `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`; seeded states via `completionBoard(board)`). Per AGENTS "Tests" rule 1, no copy is asserted on screen: tags and structure only.**
  - `ReadyStepTest` (`@Logic`):
    - With the seeded `WizardReadyNoCredits` state: `wizard.ready.continue` does not exist; `wizard.ready.no-credits` exists, has a click action, and is the only node with a click action inside `wizard.ready.footer`. The "no website button" rule is proven by this structure, by acceptance #8's grep and by the JVM footer mapping, not by text. Tapping it sends `WizardEvent.CloseFromFooter` (recording `onEvent`). The discard alert itself is 9c's, tested by `cf.dialog.confirm`/`cf.dialog.dismiss` in `WizardBackHandlingTest`.
    - With `WizardReadyChecking`: `wizard.ready.continue` is not enabled and carries `ProgressBarRangeInfo.Indeterminate` (9f's busy semantics). The label is covered by `ReadyFooterUiTest`.
    - With `WizardSaveOffline`: `wizard.save.offline` exists.
    - A Route row tap sends the revisit event for `ReadyRow.Route`. The model's routing is covered by `TripWizardViewModelReadyTest`.
  - `ReadyForegroundTest` (`@Logic`): a `TestLifecycleOwner` going STARTED → STOPPED → STARTED makes the step send the foreground event exactly once; the first STARTED sends nothing. The `force` value is a VM/JVM fact, asserted in `TripWizardViewModelReadyTest`.
  - `ReadyTickTest` (`@Logic`, `mainClock.autoAdvance = false`): advancing the compose clock 3 000 ms sends the tick event 3 or 4 times (initial + one per second); leaving the step stops it.

### Task 4: `feat(wizard): add the calculating step` [part of "Save"]

- Files: `…/fw/calculating/WizardCalculatingStep.kt`, `CalculatingMetrics.kt`; replace the `calculating` seam (in `WizardStepContent`).
- §5.6 exactly.
- **Tests:**
  - JVM (`feature/wizard/src/test/…/wizard/CalculatingUiTest`): `CalculatingUi` for the seeded/fixture summary has `route == "AMS → NRT"` (`legs[returnBoundary − 1]`) and `days == WizardPresentation.planDayCount(summary)` (computed in the test, not a literal). `summary == null` → `days == 0`.
  - Robolectric `@Logic` (`feature/wizard/src/testDebug/…/wizard/CalculatingStepTest`), with the seeded `WizardCalculating` state:
    - `wizard.close` and `wizard.back` do not exist.
    - `activity.onBackPressedDispatcher.onBackPressed()` twice leaves `wizard.calculating` shown, and the recording `onEvent` received no `Back`/`RequestClose`.
    - `wizard.calculating.card` exists.
    - With `ChronoFlowTheme(reduceMotion = true)` the spinner node is absent and the clock icon is present; with `false` the reverse (tags `wizard.calculating.spinner`, `wizard.calculating.static` [R]).

### Task 5: `feat(debug): seed the ready, save and calculating boards` [part of "Tests"]

- Extend 9f's `WizardCompletionBoardSeed` (port of `App/Debug/WizardCompletionBoardSeed.swift`) with every Ready/Save/Calculating board; strip the `Dark`/`NL`/`DarkNL` suffix before lookup (9c rule). Values exactly as iOS:
  - `WizardReadyReturn` and `WizardFineTuneMulti`: DXB stop before the destination of leg 0, `legs[0].arriveLocal = 2026-10-12T23:25`, `legs[1].departLocal = 2026-10-13T02:05`, `legs[1].arriveLocal = 2026-10-13T16:35`.
  - `WizardReadyReturn`, `WizardReadyOneFree`: `addReturn()`, home leg to AMS ("Amsterdam"), `2026-10-22T10:25` → `2026-10-22T15:30`, `returnRouteConfirmed = true`.
  - `preparation.latestWake` = `08:00` for `WizardReadyAdjusted`, else `08:30`.
  - `savedProfile` = `{freeDayStatus: declared, freeDayBed: 00:30, freeDayWake: 09:00, lightDevice: box, blueBlockers: rated, blueBlockerMdfd: "1.5", caffeineHabit: cups, cupsFilterCoffee: 2}`; prompts from `WizardCompletionPrompts.make(input, profile)` (9f's hand-built port, OQ-A438; `:feature:wizard` may not use 4c's `MockWizardPrompts`). Most of these values are already what 9f's `WizardCompletionBoardSeed.apply` writes for Fine-tune (savedProfile, identity `capture`, summary, `reviewedAt`, `lastInputStep`; DXB/return legs for `WizardFineTuneMulti`/`WizardSheetCheckout`). Reuse that path and override only `step`, `price`, `canSave`, `saveBlockedReason`, `status`/`reasons`, `latestWake` and `saveFailure`. Today `apply` hard-codes `step = fineTune` and `ApiReviewPrice(Create, 1, 3, false, [])`.
  - identity `{token: "capture", inputRevision: 1, reviewRevision: 1}`; review `status = modified` for Adjusted with the reason key "You gave no latest wake-up time, so the plan wakes you at most an hour later than usual.", else `ok`; summary line key "We shift your clock :shift earlier, starting :count day before you leave.|We shift your clock :shift earlier, starting :count days before you leave." with `shift` minutes 420 and `count` 3; `adaptation {goal: full, direction: earlier, shiftMin: 420}`, `preparationDays 3`, `planStartUtc 2026-10-09T05:15:00Z`, `planStartZone Europe/Amsterdam`, `planEndDate 2026-10-18`, `endCopy []`.
  - price per the §5.2 table (`state` `unlimited`/`free`/`create`; cost 2 for Return); `canSave = !noCredits`; `saveBlockedReason = "Not enough credits"` for NoCredits.
  - `reviewedAt` = the seed clock's now; `lastInputStep = returnChoice`; `step` = `calculating` for `WizardCalculating`, else `ready`.
  - `WizardSaveOffline` → `saveFailure = offline`; `WizardSaveServer` → `server`; `WizardSaveThrottle` (no board) → `throttle`, `retryAt = now + 42 s`.
- **Freeze** (port of `WizardBoardCapture.swift` `freezeCompletionBoard` and `WizardBoardCaptureSeed.freeze` l.211–219, through 9f's seam, OQ-9f-16 = OQ-A435): for every Ready/Save/Calculating board the seed sets `checkedInput = inputFingerprint` in the draft (as 9f does), and `WizardCompletionBoardSeed.uiOverride(board)` returns `WizardUiOverride(reviewing = board == WizardReadyChecking, retrySeconds = if (board == WizardSaveThrottle) 42 else 0)`. `WizardDebugSeeds.prepare` already forwards it as `TripWizardDependencies.uiOverride` and switches to `FrozenCheckingClock.clock` whenever it is non-null (no `WizardDebugSeeds` change for these boards). With the override non-null the ViewModel skips `resumeChecking()`/`resumeCompletion()` (9f) and `readyTick()`/`readyForeground()` (this phase, §5.5), so a frozen `WizardCalculating` never saves and a frozen Ready never re-reviews (iOS `boardFrozen`, `MemberWizardCompletion.swift:155, 166`). Do **not** add a model-level freeze to `:core:wizard`. `ReleaseReadinessTest` keeps asserting that release `uiOverride` is null and no `*BoardCapture*Seed*` class ships.
- **`WizardSaveJourney`** in 9c's `WizardBoardCaptureSeed` (port of l.114–124, Docs `wizard-save-journey-t8.md`): base draft; destination replaced with `ApiAirportSuggestion(iata = "HND", name = "Tokyo Haneda Airport", city = <NRT's city>, country = "JP", timezone = "Asia/Tokyo")` via `form.setRouteAirport(airport, 1)`, added to `state.airports`; `departure = now + 30 days`; `legs[0].departLocal = WizardDateTime.string(departure)`, `arriveLocal = WizardDateTime.string(departure + 18 h)`; `step = returnChoice`. **Unlike the other boards** [R]: this seed uses the session's **real clock**, the real `MemberWizardStore` (recovery on), the real `TripGenerationService` and the real airport search; it is never frozen and never passed to `WizardCompletionBoardSeed` (iOS `WizardCompletionBoardSeed.swift:19` `guard board != "WizardSaveJourney"`). `WizardDebugSeeds` leaves `uiOverride` null for it. Today every known board gets `WizardMemoryDraftStore`, `WizardBoardCaptureSeed.now` and `FixedAirportSearch` (there is no separate "recovery-write disable"), so `WizardSaveJourney` needs its own branch in `WizardDebugSeeds.prepare` that overrides only `start()` and keeps the session's `store`, `now`, `airportSearch` and `checkingClock`. The save then writes the recovery row as in production. `WizardBoardCaptureSeed.knows` must accept it, but it must stay out of `names` (Refresh note 10). The demo mock account already has `sleepAnswered = true`, `ageBand = 18_64`, `shiftWork = false` and 3 credits, so no sleep or blocking question appears.
- **Tests (JVM, `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`):** extend 9f's existing parameterised `WizardCompletionBoardSeedTest`. Its `draftMatchesTheCompletionBoard` asserts `step == fineTune` for every non-check board, so branch it for Ready/Save/Calculating: one case per board — step, `saveFailure`, `credits` of the seeded review, `checkedInput == inputFingerprint` after freeze; `WizardSaveJourney` (a separate test, since it is not in `names`; `WizardBoardCaptureSeed.uiOverride("WizardSaveJourney") == null`) is not frozen, its step is `returnChoice`, its destination is HND and its departure is ≥ 29 days after the injected now.

### Task 6: `feat(app): open a saved trip awaiting sync` [ticks "After save"]

- `WizardExit.Saved(tripId)` (enum → sealed interface, §6). The VM's `exitOnce()` emits `Saved(createdTripID)` when the save finished, and `Trips` otherwise (VM l.374–377, l.438–440). `WizardRoot` already collects `viewModel.exit` (`WizardScreen.kt:110`) → `Success` haptic → `onExit(WizardExit.Saved(id))`. The StateFlow is re-collected after recreation, so guard the haptic and keep the shell's `overlay is Route.Wizard` guard (Refresh notes 5–6).
- `ShellViewModel.closeWizard(exit)`: `Saved(id)` → `wizardSaved(id)` when `overlay is Route.Wizard`; `Trips` unchanged.
- The overlay ViewModel store (`ShellViewModel.overlayStoreOwner`) is cleared by the `state` setter because the overlay becomes null (`ShellViewModel.kt` l.22–30, already present); the closed model's pending writes finish in `writerScope` (9b).
- **Tests:**
  - Extend `app/src/test/kotlin/net/chronoflow/app/shell/ShellViewModelOverlayTest.kt` (JVM; it already has `closeWizardKeepsTripsAndEveryStack`/`closeWizardKeepsTodayAndEveryStack`). New case: `closeWizard(Saved("t1"))` from a state with an overlay on Today → `overlay == null`, `selected == Trips`, Trips stack `[Trips, TripPage("t1", awaitingSync = true)]`; `closeWizard(Trips)` keeps 9c behaviour; a second `closeWizard(Saved("t1"))` after the overlay closed changes nothing.
  - The exit value is a JVM fact: assert it in `TripWizardViewModelReadyTest` (Task 2). `TripWizardViewModel` is a final class with no fake.
  - `feature/wizard/src/testDebug/…/wizard/WizardSavedExitTest` (Robolectric `@Logic`): a real VM over `FakeWizardDependencies`, driven to a finished save. Recreate `WizardRoot` once (the `exit` StateFlow re-delivers). `onExit` receives `Saved("t1")`, and the shell-side guard is covered by the JVM case above. `RecordingHaptics` saw exactly one `Success`.

### Task 7: `test(app): resume a killed save in calculating with the same identity` [ticks "Save" with Task 4]

`…/appt/wizard/WizardProcessDeathTest` (Robolectric `@Logic`; the tier that has a real Android SQLite). It builds the store, service and VM directly and does **not** boot `AppGraph`/`SessionGraph` (AGENTS "Tests" rule 1). Three rows overlap 9b's store-level tests (Refresh note 13): `savedTripIdRowSkipsTheCreate`, `lostResponseResumeReturnsTheSameTrip` and `firstTripSaveLeavesTheRemindersOfferPending`. Reuse 9b's fixtures there and keep only what 9b cannot show: graph recreation and the screen/exit. Each test:
- builds **graph A**: an account DB file under the Robolectric files dir, a `MockDataSource` whose server state lives in its own file (`FileMockStateStore`, `core/data-mock/…/MockServerState.kt:220`), a `MemberWizardStore`, a `TripGenerationService`, and a `TripWizardViewModel` via the real factory; drives the model to Ready with a real mock review (`start()` → a seeded one-way AMS → HND draft at `ready` → `readyTick(force = true)` → review applied);
- simulates the kill: cancels graph A's scopes **without** calling `discard()` (no × and no `onCleared` discard), closes the DB driver;
- builds **graph B** from the same two files (new driver, new mock instance, new store, new VM) and composes `WizardRoot` with it.

| Method | Setup before the kill | Asserts after graph B starts |
|---|---|---|
| `calculatingRowResumesTheSaveWithTheSameIdentity` | `savePlan()` with the mock's `SaveTrip` held open (a gate in a `DataSource` decorator) so the row is at `calculating` with `savedTripId == null`; kill | `wizard.calculating` shows first; then exit `Saved(id)`; the decorator recorded exactly one `saveTrip` from graph B whose `ApiTripSaveInput` equals graph A's identity; the mock has exactly one new trip; the `member-wizard` row is gone |
| `lostResponseResumeReturnsTheSameTrip` | the gate lets the mock create the trip and its receipt, then throws `Offline` to the caller before `didCreate` (lost response); kill | graph B's resume POSTs the same identity once; the returned id equals the id the mock created in graph A; trip count +1 in total, not +2 |
| `savedTripIdRowSkipsTheCreate` | let `didCreate` persist `savedTripId`, then kill before `didSave` (gate on the pull effect) | graph B makes **zero** `saveTrip` calls; exit `Saved(<that id>)` |
| `readyRowRechecksOnceAfterRecovery` | row at `ready`, no save; kill | graph B performs exactly one `reviewTrip`; `wizard.ready` shows; no save |
| `firstTripSaveLeavesTheRemindersOfferPending` | an account DB with **no trips**, so `firstTrip == true`; save completes in graph A | settings `wizardReminders` == `"\"pending\""`; with one existing trip instead → the section is absent |
| `expiredRowDoesNotResume` | row at `calculating`, then advance the injected wall clock to `updatedAt + 1_800_000 ms` | no `saveTrip`; the row is deleted; the wizard starts fresh on Where (or closes quietly on a recovery launch, per 9c) |

Every test < 1 s warm (the first case per class/SDK has the AGENTS warm-up allowance); use 9a/9b manual clocks; `CF_MOCK_DELAY = 0`; rethrow `CancellationException`. If the mock's receipt is keyed so that graph B's new `MockDataSource` instance cannot see graph A's receipt, the mock is wrong (receipts live in `MockServerState.wizardReceipts`, persisted by the `MockStateStore`, 4c); report it, do not work around it.

### Task 8: `test(wizard): guard and capture the ready and save boards` [part of "Tests"]

- **`WizardReadyGuardsTest`** (`@Layout`, parameterised, one test case per parameter set) in `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`, modelled on 9f's `WizardCheckingFineTuneGuardsTest`, runs 0e `Guards` (no truncation, 48 dp targets, inside `safeDrawing`, no overlap, headings) on `WizardScreen` with the frozen seeded states (`completionBoard(board, locale)`), each case < 1 s warm:

  | Cases | States | Matrix | Count |
  |---|---|---|---|
  | (a) | the 11 states with a DarkNL twin: `Ready`, `ReadyReturn`, `ReadyUnlimited`, `ReadyFree`, `ReadyOneFree`, `ReadyAdjusted`, `ReadyChecking`, `ReadyNoCredits`, `SaveOffline`, `SaveServer`, `Calculating` (11) | `small`, `pixel8`, `foldInner`, `largeFont` × en, nl × light, dark | 176 |
  | (b) | `SaveThrottle_noBoard` | the 4 profiles × en, nl, light | 8 |
  | (c) | `ReadyNoCredits`, `ReadyAdjusted` | `landscapeSideNav`, nl, light and dark | 4 |

  Extra assertions: in `ReadyNoCredits` the bounds of `wizard.ready.no-credits` lie inside `wizard.ready.footer` and above the profile's safe bottom (iOS `testNoCreditsCloseStaysInsideThePinnedFooterAboveTheHomeIndicator`); at `largeFont` + nl the Ready rows use the stacked label layout and the content scrolls while the footer stays fixed. Record the class run time in the PR.
- **Captures** go through 9f's existing `WizardBoardCaptureTest` (`@Capture`, Robolectric, 390 × 844 dp, density 3, zero insets; theme and language from the name). It is parameterised over `WizardBoardCaptureSeed.names`, so adding these names to `WizardCompletionBoardSeed.names` captures them, with no new capture class: `WizardReady`, `WizardReadyDark`, `WizardReadyNL`, `WizardReadyReturn`, `WizardReadyReturnDarkNL`, `WizardReadyUnlimited`, `WizardReadyUnlimitedDarkNL`, `WizardReadyFree`, `WizardReadyFreeDarkNL`, `WizardReadyOneFree`, `WizardReadyOneFreeDarkNL`, `WizardReadyAdjusted`, `WizardReadyAdjustedDarkNL`, `WizardReadyChecking`, `WizardReadyCheckingDarkNL`, `WizardReadyNoCredits`, `WizardReadyNoCreditsDarkNL`, `WizardSaveOffline`, `WizardSaveOfflineDarkNL`, `WizardSaveServer`, `WizardSaveServerDarkNL`, `WizardCalculating`, `WizardCalculatingDarkNL` (23). Add `WizardSaveThrottle` (1) to `names` and to the test's `_noBoard` set, so it is written as `WizardSaveThrottle_noBoard`. The test composes `ChronoFlowTheme(reduceMotion = true)`, which would draw Calculating's static clock. Give `WizardCalculating*` (and the `WizardReadyChecking*` button spinner, if it reads Reduce Motion) `reduceMotion = false` with `mainClock.autoAdvance = false`, so the spinner matches the board.
- No stored Roborazzi references (opt-in only).

### Task 9: `test(app): walk a one-way trip from review to the trip page` [ticks "Tests"]

Both tests in `…/appat/wizard/`, extend 0e `ScreenTest`, `@Smoke`, < 10 s each, `timeout_msec=60000`, `waitUntil`/`waitUntilExactlyOneExists` only, never sleep.

- **`WizardJourneyTest.oneWayReviewSavesAndOpensTheTripPage`** (port of iOS `TripWizardTests.testOneWayReviewAndSaveOpensTripPage`). Seeds: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_MOCK_DELAY=0`, `CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney`. Steps:
  1. wait for and click `wizard.choice.one-way` (9d tag);
  2. wait until `wizard.fine-tune.continue` is displayed and enabled (timeout 5 s), click it;
  3. wait until `wizard.ready.continue` is enabled (timeout 3 s), click it;
  4. wait for `trip.page` (timeout 5 s);
  5. assert `wizard.ready` and `wizard.calculating` no longer exist, and the selected tab is My trips (`tab.trips`, `AppShellScaffold.kt:107`, `assertIsSelected()`).
  Record the duration in the PR. This is the wizard feature's journey; it is added to 9c's `WizardJourneyTest` class (`app/src/androidTest/kotlin/net/chronoflow/app/wizard/WizardJourneyTest.kt`, class-level `@Smoke`, today one method `newTripRouteReachesTheNextStep`; OQ-9g-5).
- **`WizardCalculatingBackTest.systemBackKeepsCalculating`** (seeded screen check, not a journey): seed `CF_WIZARD_BOARD_CAPTURE=WizardCalculating` (frozen) → wait for `wizard.calculating` → `UiDevice.pressBack()` twice → `wizard.calculating` still exists and `wizard.close` does not. This is the system-navigation proof the Robolectric dispatcher test cannot give (AGENTS Tests 1).

If the worker sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:WizardJourneyTest#oneWayReviewSavesAndOpensTheTripPage app:WizardCalculatingBackTest"`) and their exact output; the orchestrator runs them.

### Task 10: `docs(wizard): record ready and save acceptance` [no box; ticks any remaining 9g box]

- `docs/wizard.md`: the Ready metrics table, the price-state table (§5.2), the banner priority (§5.3), the footer table (§5.5), the seed table (Task 5), the capture list with the reviewer's board comparison (each difference fixed or recorded with its board name: at least ReadyReturn price "2 credits" vs board "1 credit", ReadyAdjusted body from the server reason, Ready dates from `planDateSummary` of the seeded summary vs the board's sample dates, NoCredits without the website button), the process-death results.
- `plan.md`: append OQ-9g-1 … OQ-9g-11 to "Open questions for the user" as **OQ-A439 … OQ-A449** in that order (9f ended at OQ-A438; renumber upward only if another row has taken one meanwhile). Any new 9g question takes the next number after the last one used. Record what 9g decides for OQ-A365. Tick the Phase 9g boxes not yet ticked.

---

## 8. Acceptance criteria

All of these must pass and be pasted into the PR (command and the last lines of output):

1. `make quick` (Spotless, Konsist, JVM tests, Robolectric logic tier) — green, < 60 s warm.
2. `make targeted ONLY="feature:wizard:ReadyPriceUiTest feature:wizard:ReadyCreditsSaveFailureTest feature:wizard:ReadyBannerUiTest feature:wizard:ReadyFooterUiTest feature:wizard:ReadyRowsUiTest feature:wizard:TripWizardViewModelReadyTest feature:wizard:ReadyStepTest feature:wizard:ReadyForegroundTest feature:wizard:ReadyTickTest feature:wizard:CalculatingUiTest feature:wizard:CalculatingStepTest feature:wizard:WizardSavedExitTest feature:wizard:WizardCompletionBoardSeedTest app:ShellViewModelOverlayTest app:WizardProcessDeathTest"` — green.
3. `make targeted ONLY="feature:wizard:WizardReadyGuardsTest"` — 188 cases green, each < 1 s warm (warm-up cases recorded separately).
4. `make captures ONLY=WizardReady,WizardSave,WizardCalculating` (comma list of name prefixes, `BoardCapture.kt:18`; `*` is not a wildcard) — 24 PNGs in `build/captures/`, named after the boards; the reviewer compares each with its board.
5. `make check` with `pixel` booted — green, including the two `@Smoke` tests of Task 9; < 5 min warm. Journey duration recorded.
6. 9b's `WizardSaveRecoveryTests`, `WizardCompletionTests`, `WizardFixtureScenarioTests` still green with no edit.
7. `testReleaseUnitTest` proves no board seed or `WizardSaveJourney` ships in release and that release `TripWizardDependencies.uiOverride` is null (9f's `ReleaseReadinessTest`, extended).
8. `git grep -n "Buy credits\|Credits kopen\|SetPaywall\|chronoflow.net/billing" -- feature app core` returns nothing in main sources.
9. Performance: no main-thread disk or network (StrictMode clean during Task 9's journey: no `StrictMode` violation in logcat for the app process); the countdown recomposes only the button (a Robolectric recomposition counter on `ReadyRowsCard` stays constant while `retrySeconds` counts 42 → 40, asserted in `ReadyTickTest`).

---

## 9. Review checklist (Opus reviewer)

Behaviour:
- [ ] No-credits Ready has **no** primary button and no website/purchase/link anywhere (A14, OQ 41); Close opens the discard alert in Create.
- [ ] Price states come only from `review.price`, `canSave`, `saveBlockedReason`; nothing hard-codes 1 or 3 credits (OQ-A18). `credits` precedence equals iOS (unavailable before unlimited before free before oneFree).
- [ ] The balance line shows only when `!unlimited && cost > 0`; destructive + semibold only when unavailable; singular "You have 1 credit".
- [ ] "Price " prefix omitted only for unlimited; free line only for `credits == free` (not for oneFree).
- [ ] Banner priority is save failure → (9h source change, skipped) → adjusted → updated. The adjusted body is the server reasons, joined with one space.
- [ ] The button: "Checking…" with spinner while `reviewing` (alpha 0.7), "Try again in N s" disabled while counting, "Try again" at 0, else "Make my plan"; disabled whenever `canSave` is false.
- [ ] The UI never calls the generation service or `DataSource`; it calls only `savePlan`, `readyTick`, `revisit`, `openPrompt`.
- [ ] No automatic save retry anywhere: no `LaunchedEffect` that calls `savePlan`, no retry on reconnect, no retry when the countdown reaches 0.
- [ ] `readyTick` runs every 1 s only while Ready is composed; the foreground check skips the first ON_START and forces only for unavailable.
- [ ] Calculating: no ×, no chevron, back consumed with no predictive preview; the route line uses `legs[returnBoundary − 1]`.
- [ ] A saved trip calls `wizardSaved` once: My trips tab, `TripPage(id, awaitingSync = true)`; `Success` haptic once.
- [ ] Process death: a `calculating` row resumes with the **same identity** (`draft_token`, `input_revision`, `review_revision` unchanged); a stored `savedTripId` skips the POST; exactly one trip results in every Task 7 case.
- [ ] First-trip offer is written only by 9b's `didSave` and only when `firstTrip`; 9g adds no second writer.
- [ ] `WizardSaveJourney` is never frozen and uses the real clock, store, service and search; every other completion board is frozen.

Android and code:
- [ ] Every string is a resource with en and nl; the via-flight Android-only row is in `android-only.json` and `plan.md`.
- [ ] Every dp, colour and type value comes from generated tokens or a `@BoardMetrics` object; no literal colours; the 16 % dawn disc is a named constant.
- [ ] `retrySeconds` is read inside the button label lambda only; `ReadyUi` is `@Immutable`; rows have stable keys if in a lazy list (they should be a plain `Column`).
- [ ] `catch (e: Exception)` / `runCatching` never swallow `CancellationException` (AGENTS Traps).
- [ ] Announcements use `liveRegion`/pane semantics, never `announceForAccessibility`.
- [ ] Tags match §5 and the iOS ids (`wizard.ready.*`, `wizard.save.*`, `wizard.calculating`).
- [ ] Tests: cheapest tier; no `Thread.sleep`; no text-prefix matching; exact tags; JVM and warm Robolectric < 1 s; the journey < 10 s; no copy asserted on screen; parameterised guards are one case per parameter set.

Typical mistakes to look for:
- Using `String.format` on the iOS `{{count}}` key, or `pluralStringResource` with the wrong quantity rule (must follow 0c §11).
- Showing "Buy credits on the website" because the board draws it.
- Calling `readyTick(force = true)` on every ON_START, which doubles the review on opening a no-credits Ready.
- `LaunchedEffect(createdTripId)` that calls `onExit` again after recomposition.
- A `BackHandler` on Calculating that is a `PredictiveBackHandler` (shows a preview).
- Freezing `WizardSaveJourney`, or seeding it with the 2026-10-01 capture clock (the mock's now is real; the flight would be in the past).
- Testing the price mapping through the screen instead of the JVM mapper.

---

## 10. Open questions (proceed on the default; append each to `plan.md` with the next free `OQ-A` number)

| # | Question | Default |
|---|---|---|
| OQ-9g-1 | `WizardReadyNoCredits` draws "Buy credits on the website" → SetPaywall; A14/iOS OQ 41 say no hand-off. | Close only; red "You have 0 credits"; silent re-check on foreground. Revisit when a backend hand-off endpoint exists. |
| OQ-9g-2 | A save throttled by 429 has no board; iOS builds it ("Wait a moment" + lockout copy + "Try again in N s"). | Build it with the WizardSaveServer banner look; capture as `WizardSaveThrottle_noBoard`; ask the designer for a board. |
| OQ-9g-3 | `WizardReadyReturn` draws "1 credit"; the iOS seed (and per-direction pricing) uses cost 2 → "2 credits". | Follow the iOS seed (2 credits); the price is server data, not a look. |
| OQ-9g-4 | The member "Updated" banner (re-check changed the price or summary on create) has no board. | Build it as iOS (info tone, `wizard.ready.notice`); no capture. |
| OQ-9g-5 | 9c says 9d–9g extend `WizardJourneyTest` and add no other journey; the plan asks 9g for a seeded `WizardSaveJourney` journey. | Add `oneWayReviewSavesAndOpensTheTripPage` as a second method of 9c's `WizardJourneyTest` (seeded at Coming back?, as iOS's single remaining journey); keep 9c's short route walk. If the reviewer counts two journeys, drop 9c's method, since Where is covered by its seeded checks. |
| OQ-9g-6 | iOS stacks Ready labels above values above Dynamic Type `.xLarge`; Android has a font-scale cap of 1.3. | Stack when `fontScale >= 1.2f`. |
| OQ-9g-7 | Board Ready geometry differs from iOS: rows 14/20 label and 14/19 value with 5/10/5/16 padding (iOS `screenBody`, 8/16); summary card gap 6 (iOS `space2` 8); price block gap 2 and line 0 in `foreground` (iOS gap 6, line 0 inherits `mutedForeground`). | Board geometry and colours (looks come from the boards) with `screenBody` (14/20) for both row texts; record each difference in `docs/wizard.md`. |
| OQ-9g-8 | Board `WizardReadyChecking` dims the busy button to 0.7; iOS uses the 0.4 disabled alpha. | Board: 0.7 while busy (same as 9f's FineTuneBusy), 0.4 when disabled for any other reason. |
| OQ-9g-9 | The board Calculating card draws a 64 dp dawn-tinted disc behind the spinner, radius 26, padding 32/20 and a soft shadow; iOS shows a bare large spinner in `.wizardCard()` (radius 16, padding 32 all round, no shadow). | Board card and disc with `CfSpinner` 30 dp; static clock under Reduce Motion (iOS). |
| OQ-9g-10 | "Latest wake-up" row has no iOS accessibility id. | `wizard.ready.latest-wake-up`. |
| OQ-9g-11 | iOS freezes completion boards with a `#if DEBUG` model method; 9f replaced it with the `WizardUiOverride` seam (OQ-9f-16), which does not stop `readyTick`. | Reuse 9f's seam; `readyTick()`/`readyForeground()` in the ViewModel return at once while the override is set. No model-level freeze. |
