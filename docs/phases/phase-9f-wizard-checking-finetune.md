# Phase 9f: Checking and fine-tune

- **Stack check (2026-11-20)** against `feat/phase-9f-wizard-checking-finetune` at `2979b332` (9e on top). **Worker: repeat these checks first (`git grep`, read the files) and stop and report on any mismatch.** Preflight: `git -C <repo> rev-parse --abbrev-ref HEAD` prints `feat/phase-9f-wizard-checking-finetune`. One lane, main checkout. Where this bullet and a later section disagree, this bullet wins.
  - **Verified as written:** `MemberWizardSnapshot(draft, closed, persistenceFailed, checking: WizardCheckingProgress(ticks, slow, failure, retryAt), retrySeconds, choicePending, reviewing, …)`; `WizardCheckingClock(nowMillis, sleepUntilMillis)` + `.continuous`; `WizardChecking.run` (ticks 400/800, `Tick(3)` only after the response and ≥ 1 200 ms, 300 ms pause, slow 10 000, `Server(null)` at 30 000; `WizardChecking.kt`); the retry countdown runs on `checkingClock` (`MemberWizardRetry.kt` l.61–82); extensions `beginChecking`/`resumeChecking`/`cancelCheck` (`MemberWizardReview.kt` l.136–170), `lastInputStep` (l.23), `progress` (l.39–61: Checking/Excluded 0.60 or 0.70 with `checkingAnswers`, FineTune 0.80, Ready 0.95; `progressFloor` max), `checkingTitle` (returns the English key; `TripWizardActions.kt` l.117), `isEditing`, `hasChanges`, `showsBack` (false on Checking without failure), `navigationPath`/`popNavigation`, `go()` cancels the check when leaving Checking; `reviewInput`, `inputFingerprint`, `optionalPrompts` (required first), `preference`, `setPreference`, `editForm((WizardForm) -> WizardForm)`, `openPrompt`, `answerOptional`, `finishFineTune` (`MemberWizardCompletion.kt`); `allowsSleepOverride` (`WizardMode.kt` l.91); `WizardForm.maxPreparationDays = 4`; `WizardOptionalAnswer.drinkFields`/`error`; `WizardPresentation.promptAnswers`/`promptAnswerText(prompt, input, language, t)`/`promptFieldError`; `LockoutWait(seconds)` (`core/model/…/auth/LockoutWait.kt`, rounds up); `PromptKind` objects (`SeatSide`, `Room`, `LateCheckOut`, `LatestWake`, `PreDepartureBusyHours`, `FreeDayTimes`, `Cups`, `LightDevice`, `BlueBlockers`, `.raw`); every token and type style named in §4–§5 (`wizardCheckingPadding` 22, `wizardCheckingGap` 18, `wizardArcHeight` 40, `wizardArcDotRadius` 5, `wizardSpinnerSize` 14, `wizardFailure*`, `radius3Xl` 26, `wizardCardRadius` 16, `wizardButtonHeight` 52, `wizardLabelGap` 2, `wizardEmptyOpacity` 0.35, `wizardDisabledOpacity` 0.4, `wizardControlGap` 6, `wizardContentGap` 14, colour `wizardPickerFill`; `wizardArc`, `wizardFailureTitle`/`Body`, `wizardSecondary`, `wizardSearchName`, `wizardRow`, `wizardButton`, `wizardChronotype`, `wizardFieldLabel`, `wizardHint`, `wizardSubtitle`, `screenMeta`); every §5.11 key is generated (wizard table, incl. the three `%lld` throttle plurals, "Checking…", "Dark room", `wizard.room.from`, the light notice); `CfButton(…, loading, loadingDescription, …, disabledAlpha, contentColor, containerColor)`; `CfSwitch`, `Modifier.cfToggleable`, `CfSpinner(modifier, size, color)`, `CfDatePickerDialog`/`CfTimePickerDialog(…, confirmEnabled, …)` (`CfPickers.kt`), `CfModalBottomSheet`; `HapticEvent { Selection, ToggleOn, ToggleOff, Light, Success, Warning, Error, Medium }`; `CfTheme.reduceMotion`/`LocalReduceMotion`; the iOS files and line ranges of §2.4 at `f391a1fa`; boards unchanged since the docs commit (line refs hold).
  - **Changed in this brief (read before §5):**
    - **Task 6 verification follow-up:** `LucideGlyphsTest.everyBoardPathParsesAndVectorsAreCached` fixes the registry size at 47. Adding the three glyphs requires 50; update that existing guard with the generated glyphs. This changes no earlier screen or model behaviour.
    - Plan lines: Phase 9f is l.2868–2899; OQ-A8 l.604, OQ-A36 l.632, OQ-A399 l.994; the next free number is **OQ-A420**.
    - **Shell pattern (9c–9e).** The screen is the stateless `WizardScreen(state: WizardUiState, copyFormatter, onEvent: (WizardEvent) -> Unit)` (`…/fw/shell/WizardScreen.kt`); `WizardRoot(viewModel, onExit)` only collects. Per-step UI is a **nullable field of `WizardUiState`** built in `WizardUiState.from(active, closing, dialog, timeFormat, now, continueFailures, picker, openTime, answerSheet)` (`…/fw/ui/WizardUiState.kt`); add `checking: WizardCheckingUi?` and `fineTune: WizardFineTuneUi?` there (additive params with defaults), **not** separate `checkingUi`/`fineTuneUi` StateFlows. `WizardStepContent` takes the title from the step field chain (`where ?: times ?: returnChoice ?: sleep ?: excluded`) and the subtitle likewise, then dispatches the step composable; a null title falls back to the debug `WizardStepSeam(key)` — that seam is the "Fine-tune placeholder" 9e left; 9f replaces it for `checking` and `fineTune`. Intents are `WizardEvent`s handled in `TripWizardViewModel.onEvent` (`…/fw/shell/WizardEvent.kt`), not public VM methods. `Continue(step)` → `continueFrom(step)` (holds the `continuing` guard): add the `checking` branch (`beginChecking()`, only with a failure) and the `fineTune` branch (`finishFineTune()`). New events: `OpenPrompt(id: String?)`, `SetPreparationDays(days)`, `SetSleepOverride(enabled)`, `SetPreference(field, value)`; override times reuse `OpenTimeSheet`/`ConfirmTimeSheet` (below). `TripWizardUiState.Active` carries `wizard`, `progress`, `path`, `sleepComplete`, `exclusionWasSaved`, `isEditing`, `optionalPrompts`; add what the mappers need (e.g. `checkingTitle`, `allowsSleepOverride`, preferences) as defaulted fields filled in the VM's `snapshots.collect`.
    - **Footer (9c/9e).** `WizardFooter(chrome, step, onEvent)` renders the primary only when `WizardChromeUi.footerId != null` (tag `wizard.<id>.footer` / `.continue` / `.secondary-close`); `secondaryClose` and `primaryPlain` exist (9e Excluded), the secondary Close emits `CloseNow`, and the primary label is `wizard_continue` or "Change my answers". Add defaulted `WizardChromeUi` fields (e.g. `primaryLabel: WizardCopy?`, `primaryBusy: Boolean`) and set `footerId = "checking"` only while `checking.failure != null`, `"fine-tune"` on Fine-tune. On Checking the secondary Close emits **`CloseFromFooter`** (iOS `MemberWizardSteps.swift` l.69–75: guest or no changes → close, else the discard dialog). Toolbar `RequestClose` closes directly only for guest + excluded or no changes; otherwise it asks to discard (l.141–148). Throttle disables Continue with `routeComplete = false` so the 0.4 alpha applies.
    - **Back.** All BackHandlers live in `WizardBackHandling(state, onEvent)` (composed after the scaffold, so it wins over `NavDisplay`). Add one enabled while `state.checking?.failure == null` on the Checking step that emits `WizardEvent.Back`; `vm.back()` already pops through `popNavigation` → `go()` → `cancelCheck()`. No BackHandler in the step composable.
    - **Haptics are played by composables only**, from monotonic counters in UI state (9d `continueFailures`, 9e `WizardAnswerSheetState.failures`, both guarded with a `rememberSaveable` "handled" value). Replace §5.3's `haptics: Flow` with e.g. `WizardCheckingUi.outcome: CheckingOutcome?` + `outcomeSeq: Int` set by the VM when it observes the transition, played by `WizardScreen`.
    - **Answer sheet (9e).** 9e merged the scaffold: `…/fw/answers/WizardAnswerSheet.kt` (`WizardAnswerSheetHost`, `WizardAnswerSheet`, private `WizardAnswerHeader` — Cancel `wizardRow`, title `wizardSheetTitle` Fraunces 500 17/22, Done `wizardButton` with `disabledAlpha = wizardEmptyOpacity`; title focused after `Expanded`), `WizardAnswerSheetState(prompt, values, error: FieldMessage?, failures)` with `missingTimes`, `doneEnabled`, `update(field, value)`, `validate()`, `load(prompt, reviewInput, savedProfile, fieldErrors, form)` (`…/fw/answers/WizardAnswerSheetState.kt`), `SUPPORTED_SHEET_KINDS = setOf(FreeDayTimes)`, `WizardPromptLabels.key` (FreeDayTimes only). The **VM holds the sheet state** (`mutableAnswer`, loaded when `promptSheetID` changes; events `UpdateAnswer(field, value)` — only fields in `prompt.fields` — `AnswerDone`, `CancelAnswer`); `answerDone()` already validates, bumps `failures` (→ one `Warning`) and maps `DataFailure`/`RuntimeException` to "Choose one of the available options.". **So §5.6–§5.7 become: extend `WizardAnswerSheetState` in `…/fw/answers/` with the kinds, options and helpers** (no `WizardAnswerDraft` in `:core:wizard`, no `rememberSaveable` draft — VM state survives configuration changes; process death reloads from `promptSheetID`, as 9e documents). Widen `SUPPORTED_SHEET_KINDS` and `WizardPromptLabels.key` to every §5.3 kind.
    - **Times.** Time pills open 9e's VM-driven time sheet (`OpenTimeSheet(field)` → `mutableOpenTime` → `WizardSleepTimeSheet` → `CfTimePickerSheet`, `ConfirmTimeSheet(field, hour, minute, touched)`), with `WizardTimeRow(label, value, timeFormat, onClick, tag, minHeight)` as the row. Use it for the override pills and the room time (fields e.g. `override.habitual_bed`, `override.habitual_wake`, `stay.bed_available_from_local`) **at minute precision**: no `WizardClockRounding` and no "Steps of 5 minutes." footnote for those fields (add a defaulted flag on `WizardTimeSheetUi`); untouched Done writes nothing. The busy-hours chain keeps 9d's `CfDatePickerDialog`/`CfTimePickerDialog` (`…/fw/times/WizardFlightPickers.kt` pattern). There is no inline `CfTimePicker` yet (Task 3 stands).
    - **Copy (OQ-A399 carried in).** `rememberServerCopy(formatter)` and the catalog-first `fieldCopy(message, formatter)` live in `…/fw/components/WizardServerCopy.kt`; `:feature:wizard` depends on `:core:plan` and `TripWizardDependencies.copyFormatter()` exists. Times still renders `wizardCopy().resolve()` at `times/WizardFlightCard.kt` l.98–103, `times/WizardDateTimeRow.kt` l.136 and the banner at `times/WizardTimesStep.kt` l.89, and Sleep at `sleep/WizardSleepStep.kt` l.106; iOS routes all of them through `fieldCopy` (`MemberWizardView.swift` l.235, l.267; `MemberWizardSteps.swift` l.277). **Task 0** switches them (threading `copyFormatter` as an additive parameter) and closes OQ-A399.
    - **Glyphs.** `LucideGlyphs` has `Check`, `ChevronRight`, `CircleAlert`, `Plus`, `X` but **no `CloudOff`, `Clock` or `Star`**. Add `cloud-off`, `clock` and `star` (filled) to `tools/icons/lucide.json` as `"source": "board"`, `"board": "Wizard"` (the boards inline them; the bundle lacks them), run `make vectors`, and commit the generated file with the manifest.
    - **Seeds.** Registry: `WizardBoardCaptureSeed` (`object`: `names`, `baseName`, `knows`, `presentation`, `draft(board, locale)`, `now = 2026-10-01T09:00Z`; base AMS → NRT 2026-10-12T14:40 → 2026-10-13T09:15 verified), extended per phase by a sibling object (9e `WizardSleepQuestionBoardSeed`); 9f adds `WizardCompletionBoardSeed` there. **Konsist forbids `:core:data-mock` in `:feature:wizard` main/debug** (`ModuleGraphTest.dataMockOnlyInDebugAndPerf`, `onlyDebugAndPerfImportTheMock`): build the prompts by hand as 9e did, porting `I/Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockWizardPrompts.swift`; a `testImplementation(project(":core:data-mock"))` JVM test may cross-check them against `MockWizardPrompts.make`. The release check is `app/src/testRelease/…/ReleaseReadinessTest.kt` (`wizardCaptureCodeAndStartOverrideAreAbsent`). The device router `app/src/debug/…/WizardDebugSeeds.kt` overrides deps via `object : TripWizardDependencies by session`. Captures: `WizardBoardCaptureTest` iterates `names`, file name = board name (no `_en_light` suffix), `_noBoard` for board-less names, 390 × 844 profile `wizardBoardProfile`, NL only for names ending `NL`, `reduceMotion = true`; states come from `boardState(board, locale)` (`…/testDebug/…/WizardBoardStates.kt`), which builds `WizardUiState.from` without a VM — pass the debug UI override there too. OQ-A412: **9f re-captures `WizardSheetFreeSleep`/`…DarkNL` over the real Fine-tune** and owns that comparison.
    - **Test support.** VM tests use `withWizard(entry, configure) { fixture -> }` with `FakeWizardDependencies` (`GenerationSource` as `source`, `CheckingTestClock(testScheduler)` as `checkingClock`, `checkedDraft(step)`) and `WizardMainDispatcherRule` (`feature/wizard/src/test/…/WizardTestSupport.kt`); delay a review with `source.configure(beforeReview = { delay(100) })`, fail it with `reviewFailure`, count with `source.count("review")`. There is no `FakeWizardGeneration` in these tests and no shared `RecompositionCounter` (9d's `WizardTimesRecompositionTest` counts with a `SideEffect` per item). Robolectric hosts with `WizardTestHost` (`WizardScreenFixture.kt`). Guards are separate `Guards.assertTextFits/assertTouchTargets/assertInteractiveInSafeArea/assertNoInteractiveOverlap/assertHeadings` calls on a `LayoutCase` (no `Guards.assertAll`). No `WizardSeedRoutingTest` exists. Device helpers: `wizardPickerSeed(language, board)`, `withWizardPhoneHourCycle`, `assertWizardKeyboardGeometry(decor)`.
  - **Tests rewritten for AGENTS "Tests" and the user's rules:** logic as JVM tests on `WizardAnswerSheetState`, `WizardUiState.from` and the ViewModel (`withWizard`, virtual time on `CheckingTestClock`; never `WizardCheckingClock.continuous` or a wall clock under an auto-advancing dispatcher); Robolectric renders only the stateless `WizardScreen(state, formatter, onEvent)` (or a component) from board states and checks tags, roles, semantics properties, structure and geometry — **no test asserts copy on screen** (no `onNodeWithText`/`hasText`/text or content-description equality; copy keys are asserted on the UI state in JVM tests; copy is covered by the generator tests and the captures); no test boots `MainActivity`, `AppGraph`, `SessionGraph`, `RootContent`, `CfRoot` or `WizardRoot` with a live VM (`keptAppTestBoundaries` has no wizard entry; do not add one); warm cases < 1 s (first Robolectric case per class recorded separately); no `Thread.sleep`; transient UI (the arc, the spinner, haptics) is asserted through state counters, never by racing animation; 9c's `WizardJourneyTest` stays the wizard's only journey; the emulator gets only seeded single-screen checks of native behaviour (IME, the inline picker's phone hour cycle); guard matrices are lean (light only; nl on `small`, `pixel8`, `foldInner`, `largeFont`; en on `small`; `landscapeSideNav` for two states).
  - **Worker re-check (2026-11-20):** `WizardPresentation.promptAnswerText` reads non-null form values before `prompt.answer` on both platforms (`I/…/WizardPresentation.swift` l.84–120; `I/…/WizardForm.swift` l.35–43, 63–69; `I/…/WizardJourney.swift` l.6–37). The default room therefore reads "When I land", check-out reads "No", and unanswered busy hours read "0 time ranges". These differ from the boards' "Add". Keep the landed model and the iOS behavior; Task 1's row assertion and the capture hand-off use these answers. The nullable "Add" path is still tested with genuinely missing answers (e.g. seat). `ApiReviewPrompt.copy` is private; hand-built fixtures use its public constructor.
  - **Task 3 wrapper correction:** `CfTimePickerDialog` has `supporting` and custom action tags, but only `CfDatePickerDialog` has `confirmEnabled` in 9e. Add a default-true `confirmEnabled` after the existing parameters in Task 3 and pass it to `DialogAction`; existing callers keep their behavior. The inline picker reuses the landed `cfTimePickerColors` helper.
  - **Fonts.** Any requested weight above a shipped face resolves through the generated tokens (OQ-A419: Fraunces stops at 500, synthesis off), never synthesized. Figtree ships 400–700, so "Helps a lot" 600 and the sheet question 600 use the Figtree 600 face (9e's question already does: `wizardRow.copy(fontWeight = SemiBold)`).
  - **Boards:** your sandbox cannot launch Chromium (9c–9e logs). Write the captures under the board names; the orchestrator renders the boards on the host and does the side-by-side comparison. Do not retry the browser.

- **Branch:** `feat/phase-9f-wizard-checking-finetune`. Start it from the top of the stack once **9e** (sleep and questions) is in it. 9e brings 9d (times, `WizardDateTimePill`, `WizardErrorLine`, `WizardBanner`, `WizardPickerDates`, `WizardDateFormat`, picker wrapper `confirmEnabled`/`supporting`), 9c (shell, `WizardMetrics`, step host, `WizardBoardCaptureSeed`, `WizardStartOverride`, discard), 9b (`TripWizardViewModel`, `TripWizardDependencies`, generation, recovery), 9a (`:core:wizard` model, `WizardChecking`, `WizardOptionalAnswer`, `WizardPresentation`, `WizardCredits`), 4c (`:core:data-mock` `MockWizardPrompts`), 2 (primitives, `CfModalBottomSheet`, `CfSwitch`, `cfToggleable`, `CfTimePickerDialog`, `CfDatePickerDialog`, `CfSpinner`, `CfButton`, `LocalHaptics`, `HapticEvent`, `LocalReduceMotion`), 0c (strings, `CopyIndex`, `names.lock.json`), 0e (profiles, `Guards`, `BoardCapture`, `ScreenTest`, `RecordingHaptics`), 1a (`CopyLineFormatter` for server `CopyLine`s), 5a (`LockoutWait` in `net.chronoflow.core.model.auth`). If one is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 9f: Checking and fine-tune" (l.2868–2899). Also: "Android spec: Material mapping" (Switch, Sheets, Date and time, the **Haptics** table l.439–448), "Android spec: navigation and routes" ("predictive back everywhere except the wizard's no-back steps (Checking, Calculating)"), "Android spec: window policy" (Wizard row), "Android spec: state survival" (Wizard row), OQ-A8, OQ-A36, A9, A10.
- **Tests** follow `AGENTS.md` "Tests": cheapest tier, budgets (JVM and warm Robolectric < 1 s, first Robolectric case per class recorded separately, emulator < 10 s), exact tag queries, never assert copy on screen, no `Thread.sleep`, hang caps (`runTest(timeout = 60.seconds)`). Tier annotations are 0e's: Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke` / `@Capture`.
- **Tags:** **[F]** fact read in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` at `f391a1fa` (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`. Board CSS px = dp.
- **Paths:** `…/cw/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`; `…/fw/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`; `…/fwd/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`; `…/ds/` = `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`.
- **Names from earlier phases.** This brief uses the Swift names 9a ports (`MemberWizardModel`, `MemberWizardSnapshot` with `checking: WizardCheckingProgress`, `retrySeconds`, `reviewing`, `closed`, `persistenceFailed`; `beginChecking`, `resumeChecking`, `cancelCheck`, `checkingTitle`, `optionalPrompts`, `openPrompt`, `answerOptional`, `preference`, `setPreference`, `editForm`, `finishFineTune`, `allowsSleepOverride`, `isEditing`, `mode`, `navigationPath`, `popNavigation`, `reviewInput`, `inputFingerprint`; `WizardOptionalAnswer.error`/`drinkFields`; `WizardPresentation.promptAnswers`/`promptAnswerText`/`promptFieldError`; `WizardForm.maxPreparationDays = 4`; `DataFailure.Offline`/`Server`/`Throttled`), 9b's (`TripWizardViewModel`, `TripWizardUiState.Active`, `TripWizardDependencies`), 9c's (`WizardRoot`, `WizardScaffold`, `WizardFooter`, `WizardStepHost`, `WizardStepKey`, `WizardStepSeam`, `WizardMetrics`, `vm.back()`, `vm.requestClose()`, `WizardBoardCaptureSeed`, `WizardStartOverride`, `WizardDebugSeeds`), 9d's and 9e's. **Before coding, open the merged code and use its exact names.** Where a name differs, keep the merged one and say so in the PR. Never rename earlier code; additive parameters with defaults are allowed where this brief says so.
- **This phase adds UI, ViewModel wiring and one pure sheet-state class.** It adds no model rule. If a model rule below is missing from 9a/9b, port it into `:core:wizard` with its iOS test in a separate `fix(wizard): …` commit that cites the Swift line, and report it.

---

## 1. Goal

After the last input step, **Checking** shows "Checking your trip" with a dashed arc from the first origin to the outbound destination, a moving dot, and three lines that tick at 0.4 s, 0.8 s and when the review returns (never before 1.2 s), then a 0.3 s pause and the next screen. After 10 s a slow line appears; after 30 s it becomes a server failure. After blocking answers it reads "Checking your answers" with "Your answers" first. A failure replaces the card with an **offline**, **server** or **throttle** card, a "Try again" button (counting down "Try again in 42 s" while throttled) and a Close above it. There is no back chevron while checking, but system back cancels the check and returns to the last input step.

**Fine-tune** ("Fine-tune your plan") lists the server's optional prompts as rows (required ones first, with a "Helps a lot" star). With several flights or stays the rows carry the route or city. It also has a **Preparation days** stepper (0–4), the **"Use different sleep times for this trip"** switch with its two time pills and note, and the two neutral preference toggles (iOS design exception). Continue re-checks inline ("Checking…" spinner, **Busy**) when something changed, else goes straight to Ready. With no prompts the sub and button change (**None**: "Done").

Each row opens its **answer sheet**: seat, room, check-out, latest wake-up, busy hours, drinks (four steppers), light device, blue blockers (with a rating field). A sheet edits a local copy of its prompt's values (one nullable selection plus its controls) and holds one validation error. Only Done writes; Done with an invalid answer shows the error, fires a Warning haptic and disables Done until the next change.

Done means:
- Checking timing matches `06 §3.5` on a fake clock at the ViewModel layer, and the UI lines follow `ticks` exactly.
- Every listed board, with its Dark/NL wrapper, is seeded, guarded in Robolectric and captured (46 PNGs, plus 9e's two `WizardSheetFreeSleep*` re-captured over the real Fine-tune: 48).
- Haptics follow WizardSpec "Haptics" through `LocalHaptics`.
- TalkBack hears each line through a polite live region; Reduce Motion freezes the dot and replaces the spinner with a static dot without changing the timing.

## 2. Inputs to read (in this order)

1. **This repo:** `plan.md` sections above; `AGENTS.md` ("Working rules", "Performance", "Tests", "Traps": announcements, `CancellationException`, measured sizes); `docs/phases/phase-2-design-system.md` Task 6 (Haptics), Task 15 (`CfSwitch`, `cfToggleable`), Task 16 (`CfModalBottomSheet`, `CfTimePickerDialog`, `CfDatePickerDialog`), `CfButton`/`CfSpinner`/`CfCard`; `docs/phases/phase-0c-strings.md` §11 (resource access, the wizard `_one` plural rule); `docs/phases/phase-0e-test-harness.md` T5–T9; `docs/phases/phase-5a-sign-in.md` (`LockoutWait`); the 9a, 9b, 9c, 9d and 9e briefs and their merged code (9a §6 Tasks 6, 7, 9; 9b §6.3, §6.4 ViewModel, §12 hand-off "9f"; 9c §5.1, §5.3, §5.4, §5.8, Task 9; 9d §5.5, §5.6, §6).
2. **Research:** `docs/research/06-wizard.md` §2.1 (board → state map, Checking and Fine-tune rows), §2.2 (WizardSheet kinds), §3.3 (back on Checking), §3.5 (Checking), §3.6 (re-check), §3.7 (Haptics), §4.3 "Checking", "Fine-tune", §6.1 (sheet validation rows), §7.1 (`ApiReviewPrompt`, prompt kinds table), §7.3 (errors), §9.6 (bottom sheets), §9.8 (Checking, accessibility, haptics), §9.9 (sizes), §A.2, §A.3.
3. **Boards** (read-only; open the HTML in a browser to render them). Wrappers `<dc-import name="Wizard" …>` or `<dc-import name="WizardSheet" …>` at 390 × 844. `pct` is the wrapper's `renderVals`.

| Board | Props | `pct` | State |
|---|---|---|---|
| `WizardChecking` / `Dark` / `NL` | `screen=checking` | 60 | "Checking your trip", sub "This takes a few seconds.", lines 1–2 done, line 3 busy (spinner), no button, no back |
| `WizardCheckingAgain` / `…DarkNL` | `checking v=again` | 70 | "Checking your answers"; order Your answers, Flights and time zones, What we can tailor |
| `WizardCheckingSlow` / `…DarkNL` | `checking slow` | 60 | Adds the slow line inside the card |
| `WizardCheckOffline` / `…DarkNL` | `fail v=offline` | 60 | Dashed card, cloud-off icon, "Checking needs a connection" / "Connect to the internet and try again."; Close + "Try again"; back shown |
| `WizardCheckServer` / `…DarkNL` | `fail v=server` | 60 | Alert-circle icon, "We couldn't check your trip" / "Something went wrong on our side. Try again in a moment." |
| `WizardCheckThrottle` / `…DarkNL` | `fail v=throttle` | 60 | Clock icon, "Wait a moment" / "Too many attempts. Try again in 42 seconds."; button "Try again in 42 s" disabled (alpha 0.4) |
| `WizardFineTune` / `Dark` / `NL` / `DarkNL` | `finetune` | 80 | Rows: Dark room at your stay ★ (Add), Latest you can wake up ★ (08:30), Side of the plane (Window left), Late check-out (Add), Busy hours before you leave (Add), Coffee, tea and soft drinks (2 a day); then Preparation days 3 with −/+; switch off |
| `WizardFineTuneMulti` / `…DarkNL` | `finetune v=multi` | 80 | Dark room · Dubai ★, Dark room · Tokyo ★, Side of the plane · AMS → DXB (Window left), Side of the plane · DXB → NRT (Add), Late check-out · Dubai (Add), Latest you can wake up (08:30, not starred) |
| `WizardFineTuneSleep` / `…DarkNL` | `finetune v=sleep` | 80 | Rows room ★ (From 15:00), wake ★ (08:30), Sleep on free days (Same as work days); switch on; "Bedtime on this trip" 00:30, "Wake-up on this trip" 08:30, note |
| `WizardFineTuneBusy` / `…DarkNL` | `finetune v=busy` | 80 | Member rows; button spinner + "Checking…", alpha 0.7, disabled |
| `WizardFineTuneNone` / `…DarkNL` | `finetune v=none` | 95 | No rows card; sub "No extra questions for this trip. You can still change these."; button "Done" |
| `WizardFineTuneSheet` / `…DarkNL`, `WizardSheet` | `WizardSheet kind=seat` | — | Over Fine-tune: "Side of the plane", question, caption "AMS → NRT", Window left ✓ / Window right / Aisle or not sure ("We plan without the window") |
| `WizardSheetRoom` / `…DarkNL` | `kind=room` | — | When I land / From a time ✓ + time row "From 15:00" |
| `WizardSheetCheckout` / `…DarkNL` | `kind=checkout` | — | Yes ✓ / No |
| `WizardSheetWake` / `…DarkNL` | `kind=wake` | — | Inline hour/minute wheel, 08:30, 180 high |
| `WizardSheetBusy` / `…DarkNL` | `kind=busy` | — | Rows "Fri 9 Oct · 09:00–17:30", "Mon 12 Oct · 08:00–11:00" with 32 dp circular × + "Add busy hours" |
| `WizardSheetDrinks` / `…DarkNL` | `kind=drinks` | — | "None" (unchecked) + "Cups a day" + 4 steppers Coffee 2, Tea 0, Soft drinks 0, Energy drinks 0 (− at 0 dimmed .35) |
| `WizardSheetDrinksError` / `…DarkNL` | `kind=drinks err` | — | All 0, 2 dp destructive ring around the stepper group, error line, Done disabled (.35) |
| `WizardSheetLight` / `…DarkNL` | `kind=light` | — | None / Light box ✓ / Light glasses |
| `WizardSheetBlue` / `…DarkNL` | `kind=blue` | — | No / Yes, with a rating ✓ / Yes, no rating + row "Rating on the label 1.5" (nl "1,5") |
| `WizardSheetBlueError` / `…DarkNL` | `kind=blue err` | — | Rating "12", ring on the rating row, error "Enter a rating between 0.1 and 9.9.", Done disabled |

   - `DC/Wizard.dc.html`: template l.116–130 (checking card), l.132–138 (failure card), l.155–185 (fine-tune cards), l.205–212 (footer: secondary + CTA, `ctaBusy` spinner); logic l.238 (`slow`), l.254–260 (back targets, `nav.back` false on `checking`, true on non-edit `fail`), l.373–390 (`lines`, `checks` styles, failure kinds), l.414–428 (`ftDefs`, row styles, `sheetHref`, `ft` prep/switch), l.466–482 (page CTAs), l.516–522 (`mainStyle` bottom 140 with a secondary, CTA styles: disabled alpha 0.4, busy alpha 0.7); preparation stepper l.163; copy table `T` l.228.
   - `DC/WizardSheet.dc.html`: template l.25–81 (form header l.43–47, question l.48, caption l.49, options l.54, time rows l.57, wheel l.60, busy ranges l.67, steppers l.71–74, rating l.75–77, error l.78), logic l.84–136 (`K` per kind l.98–113, `steppers` l.116, `hasErr` l.117, `sheetStyle` l.121, `doneStyle` l.125, `stepGroupStyle` l.131, `ratingStyle` l.132), copy table l.87.
   - `DC/WizardCopy.dc.html`: "Fine-tune rows and answers", "Sheet errors", "Checking line", "Countdown button", "Accessibility", "Android" ("Preparation days: drawn − and + buttons (48 dp), no native stepper."), "Layout" ("On 375×667: Fine-tune and Ready scroll").
   - `DC/WizardSpec.dc.html`: "Checking", "Re-checks", "Too many attempts", "Haptics", "Back on Checking".
4. **iOS references** (`I/` at `f391a1fa`). Port behaviour from these:
   - `plan.md:1800` "16b: Return, Sleep, Checking and Questions"; `plan.md:1866` "16c: Fine-tune, Ready, Save and create acceptance" (Review round 3 copy and fidelity bullets); `plan.md:2477` "No health data" (the **explicit design exception**: preferences and notices are built without a board, NH5).
   - `Docs/wizard-checking-t10.md` (request-thrown cancellation is a server failure; no restart loop; duplicate starts rejected — all 9a's, verify only).
   - `Kit/TripWizardUI/MemberWizardCheckingView.swift:9–157, 254–319` (`checkingLines`, `checkingScreen`, `checkLineLabel`, `checkFailure`, `WizardCheckLine`, `WizardCheckingArc`).
   - `Kit/TripWizardUI/MemberWizardSteps.swift:10–133` (`wizardShell`: secondary Close on Checking failure, button disable rules, back hidden while checking without failure).
   - `Kit/TripWizardUI/MemberWizardFineTuneView.swift:8–193` (`preferenceToggle`, `fineTuneScreen`, `overrideTime`, `optionalLabel`, `WizardPromptLabels`).
   - `Kit/TripWizardUI/WizardAnswerSheet.swift:8–385` (options per kind, `missingTimes`, `load`, `controls`, drinks, busy, `done`).
   - `Kit/TripWizardUI/MemberWizardView.swift:108–121` (sheet presentation from `promptSheetID`; title = `optionalLabel`, question = server copy of `prompt.text`).
   - `Kit/TripWizardUI/WizardStrings.swift` + `Kit/AppCore/LockoutWait.swift` (throttle body).
   - `Kit/TripWizard/Model/WizardOptionalAnswer.swift`, `WizardPresentation.swift:102–162` (`promptAnswerText`), `MemberWizardCompletion.swift:78–160` (`preference`, `setPreference`, `editForm`, `openPrompt`, `answerOptional`, `finishFineTune`) — 9a's; read to call them correctly.
   - Debug seeds: `I/App/Debug/WizardBoardCaptureSeed.swift:163–167, 211–231` (`WizardCheck*`, `freeze`), `I/App/Debug/WizardCompletionBoardSeed.swift:1–142`, `Kit/TripWizard/Model/WizardBoardCapture.swift` (`freezeBoardCapture`, `freezeCompletionBoard`).
   - Tests to mirror: `I/AppUITests/WizardCompletionScreenTests.swift` (`testFineTune*`, `testSheet*`, `assertFineTune`, `assertAnswerSheet`), `I/AppUITests/TripWizardReviewTests.swift:33–46` (`testCheckOfflineScreenKeepsEditingAndGoesBack`), `I/AppTests/WizardCompletionLayoutTests.swift:90–113` (`testDuoResizeKeepsFineTuneAndOpenAnswer`), `I/Packages/ChronoFlowKit/Tests/TripWizardTests/WizardCheckingTests.swift` (9a ported it; reuse `CheckingTestClock`).
5. **API.** 9f calls no endpoint directly. "Try again" calls `beginChecking()` and Continue on Fine-tune calls `finishFineTune()`; both end in `POST /v1/trips/review` (W `routes/api.php:95–96`, `sanctum` + `verified`, `throttle:trip-review` 10/min/user, `AppServiceProvider.php:184–199`), or in edit mode `POST /v1/trips/{trip}/review` (l.98–99, same middleware; 9h owns the edit boards, the code path is 9b's service either way), with `ApiTripReviewInput` (`06 §5.1`) through 9b's service. Answers go through 9a `answerOptional` → `TripGenerationAnswers.applyingAnswers` into the input (`06 §7.1` prompt table):

| `kind` (raw) | target | fields the sheet edits |
|---|---|---|
| `seat_side` | trip (leg) | `seat_window` ∈ `"left"`, `"right"`, `null` |
| `room` | trip (stay) | `stay.bed_available_mode` ∈ `"arrival"`, `"declared"`; `stay.bed_available_from_local` `YYYY-MM-DDTHH:mm` |
| `late_check_out` | trip (stay) | `stay.late_check_out_or_day_room` ∈ `true`, `false` |
| `latest_wake` | trip | `latest_wake` `HH:mm` |
| `pre_departure_busy_hours` | trip | `pre_departure_busy_answered: true`, `pre_departure_busy_intervals: [{start_local, end_local}]` |
| `free_day_times` | profile | 9e's sheet (`free_day_status`, `free_day_bed`, `free_day_wake`) |
| `cups` | profile | `caffeine_habit` ∈ `"non_user"`, `"cups"`; `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink` integers 0…`shape.max` (20) |
| `light_device` | profile | `light_device` ∈ `"none"`, `"box"`, `"glasses"` |
| `blue_blockers` | profile | `blue_blockers` ∈ `"no"`, `"rated"`, `"unrated"`; `blue_blocker_mdfd` 0.1…9.9 |

   Errors (`06 §7.3`): transport → `DataFailure.Offline` → offline card; any 429 → `Throttled(retryAfterSeconds)` (body `retryAfterSeconds` wins over `Retry-After`; default 60) → throttle card; 5xx, timeout at 30 s, a second `review_expired`, and everything else that reaches `checking.failure` → server card. 422 field errors never reach a card: 9a routes them to the owning step (`override.*`, `preparation.*`, `stays.*`, `seatWindow` → Fine-tune).

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| Shell, header, progress bar, footer composition, step host, discard dialog, close | 9c (reuse) |
| Sleep step, the free-day sheet (`WizardSheetFreeSleep`), questions, excluded | 9e. 9f only routes the `free_day_times` prompt to 9e's sheet body inside the shared answer-sheet scaffold (§5.6). |
| Ready, the 1 s `readyTick`, Calculating, save failures, price states | 9g. Until 9g lands, `ready` renders 9c's seam. |
| `WizardCheckingEdit`, `WizardCheckOfflineEdit`, `WizardCheckServerEdit` (edit titles, the "If you close now, your trip stays as it was." bodies, no back on edit failure) | 9h captures and guards them. 9f already maps the edit copy (§5.2) because it is the same code path; 9f does not seed or capture them. |
| `WizardCheckingGuest`, `WizardFineTuneGuest` (guest line 2, guest rows, no switch) | 11a. Build nothing guest-specific; the code must not break when `mode == guest` (no switch, `allowsSleepOverride == false`). |
| `WizardCheckPractice` / `…DarkNL` | **Not built** (no practice mode; `06 §1`, `I/plan.md:1801`). |
| `WizardReminders` (sheet `kind=reminders`) | 12c |
| A "Busy time must end after it starts." error state in the busy sheet | No board. The To time dialog's Done is disabled instead (§5.9, OQ-9f-5). |
| Wizard baseline-profile journey | Phase 13 |

## 4. Settled values that override the raw boards

| Board says | Build | Source |
|---|---|---|
| Arc: SVG cubic `M4 34 C 60 2, 140 2, 196 34` in a 200 × 40 viewBox (`width: 100%; height: 40px`, default `preserveAspectRatio` = `xMidYMid meet`), dot r 5 at (128, 11), static | The board cubic drawn as the SVG renders it: uniform scale `s = min(w / 200, 1)` (at 390 dp the box is narrower than 200, so `s < 1`), the scaled 200 s × 40 s drawing centred in the w × 40 dp box (offset `((w − 200 s)/2, (40 − 40 s)/2)`), stroke and dot radius scaled by `s` too; the dot moves along it with the iOS phase `(sin(t·π/1.8 s) + 1)/2` (period 3.6 s); Reduce Motion pins it at t = 0.65 (≈ the board's (128, 11)) | Looks from the board; motion from iOS `WizardCheckingArc` (`MemberWizardCheckingView.swift:282–318`); OQ-9f-11 |
| Busy line spinner keeps spinning under Reduce Motion (Phase 2 `CfSpinner` rule) | Under Reduce Motion the busy dot shows a static 8 dp `mutedForeground` dot, not a spinner | WizardSpec "Reduce Motion: … the spinner becomes a static dot"; iOS `WizardCheckLine` l.268–270; OQ-9f-15 |
| `<section aria-live="polite">` on the whole card; `role="status"` on the slow line | Each line node has its own `liveRegion = Polite` + `contentDescription`; the slow line and the failure card are polite live regions | AGENTS "Traps" (announcements); iOS posts per-line announcements |
| iOS `Stepper` for preparation days and drinks | Drawn 94 × 32 dp "− \| +" group (board), each half a ≥ 48 dp touch target, adjustable semantics (§5.5) | WizardCopy "Android": "drawn − and + buttons (48 dp), no native stepper"; `Wizard.dc.html` l.163, `WizardSheet.dc.html` l.73 |
| iOS sheet `presentationDetents([.medium, .large])` | `CfModalBottomSheet` (Phase 2: `skipPartiallyExpanded = true`), content wraps its height and scrolls | Phase 2; OQ-9f-13 |
| iOS sheet uses a nav bar (Cancel/title/Done) | 9e's `WizardAnswerHeader`, unchanged: Cancel (`wizardRow`, `dawnText`), title (`wizardSheetTitle` Fraunces 500 17/22, max 220 dp, 2 lines, stacks above the actions when narrow), Done (`wizardButton`, `dawnText`, disabled alpha `wizardEmptyOpacity` 0.35); 48 dp targets | Board `WizardSheet.dc.html` l.43–47; 9e review rounds 1–2; OQ-A419 |
| iOS fine-tune puts the two preference toggles **first** | A third card **after** the preparation card | No board draws them (explicit design exception, `I/plan.md:2484`); keeping the drawn cards on top keeps every Fine-tune capture equal to its board; OQ-9f-1 |
| Light sheet board has no notice | The iOS light-device notice under the question (NH5) | `WizardAnswerSheet.swift:59–67`; user design exception; OQ-9f-2 |
| Fine-tune `pct` 80 / None 95 | The 9a model's `progress` (9a Task 8 already holds the board values: Checking 60, Again 70, FineTune 80; FineTuneNone reaches 95 only through `progressFloor`, because a prompt-free Fine-tune is only reachable from Ready — WizardSpec "Re-checks": "Fine-tune is skipped when the server suggests no extras", iOS `MemberWizardReview.swift:228`). The `WizardFineTuneNone` seed therefore sets `progressFloor = 0.95` (§5.10) | OQ-A36 ("board pct"); progress comes from the model, never per board |
| "Helps a lot" 12/16 **600** (board) vs iOS `screenMeta` 400 | `screenMeta` with `FontWeight.SemiBold` (shipped Figtree 600 face; same weight-copy pattern as 9e's sheet question) | OQ-9f-9 |
| Sheet question 17/24 600 | 9e's question style: `wizardRow` with `FontWeight.SemiBold` (shipped Figtree 600 face, 17/22) | No 17/24 600 token; 9e precedent; OQ-9f-10 |
| Stepper divider `rgba(120,120,128,0.3)`, chevron `rgba(120,120,128,0.7)` | Divider `border`; chevron `mutedForeground` (iOS) | No such tokens; values only from tokens (AGENTS); OQ-9f-8 |
| Fine-tune "Busy" CTA alpha 0.7 with an 18 px spinner | 9c's `WizardFooter` primary with `loading = true`, `loadingDescription = <same text>`, `disabledAlpha = WizardMetrics.ctaBusyAlpha /* 0.7 */` (`CfButton` already has `loading`, `loadingDescription` and `disabledAlpha`; drive them from the new `WizardChromeUi.primaryBusy`). The spinner stays Phase 2's 16 dp (`AuthTokens.icon`), recorded as a difference | Board `ctaBusy` l.521–522; iOS `ProgressView` in the button; `phase-2-design-system.md` `CfButton` "States" |
| Rating "1.5" in nl boards shows "1,5" | Numbers load with the app language's decimal separator (iOS `load()` formats `.number`); seeds store numbers (iOS seed stores the string "1.5") | `WizardAnswerSheet.swift:142–146`; OQ-9f-14 |

## 5. The spec

### 5.1 Checking screen (`…/fw/checking/WizardCheckingStep.kt`) [F board l.116–130; iOS `MemberWizardCheckingView.swift:17–102`]

**Shell use (9c).** Title and sub come from `state.checking` through `WizardStepContent`'s title/subtitle chain (add `checking` and `fineTune` to it). Title = `checkingTitle` (an English key) resolved as a `WizardCopy` ("Checking your trip" / "We bekijken je reis"; "Checking your answers" / "We bekijken je antwoorden"; "Checking your changes" / "We bekijken je wijzigingen"). Sub "This takes a few seconds." / "Dit duurt een paar seconden." **only while `checking.failure == null`**. No footer while there is no failure. Back chevron hidden while there is no failure (9a `navigationPath`/9c chrome; `06 §3.4`). Screen tag `wizard.checking`.

**Lines** [F `checkingLines` l.9–16]:
- `state.checkingAnswers == true` → `["Your answers", "Flights and time zones", "What we can tailor"]`.
- else → `["Flights and time zones", mode == guest ? "Your answers" : "Your saved answers", "What we can tailor"]`.
- Line `i` is **done** when `checking.ticks > i`, **busy** when `checking.ticks == i`, **open** otherwise. (At `ticks == 0` line 0 is busy; at `ticks == 3` all are done.)
- nl: "Je antwoorden", "Vluchten en tijdzones", "Je opgeslagen antwoorden", "Wat we kunnen afstemmen".

**Card** (tag `wizard.checking.card`): `CfCard` look with radius 26 (`radius3Xl`), fill `card`, 1 dp `border`, `softShadow`; padding 22 dp vertical (`PlanUITokens.wizardCheckingPadding`), 20 dp horizontal; column gap 18 dp (`wizardCheckingGap`).
- **Arc row** (cleared from semantics): `Row(gap 12)`: origin IATA `wizardArc` (Fraunces 26/32) = `legs[0].fromIata`; arc box `weight(1f)`, height 40 dp (`wizardArcHeight`); destination IATA = `legs[returnBoundary - 1].toIata` [F l.31–33].
- **Arc drawing** (`WizardCheckingArc`): `Modifier.drawWithCache { val path = cubic(…); onDrawBehind { … } }`. Path in board coordinates mapped like SVG `meet` (§4): `s = min(size.width / 200dp, 1f)`, `ox = (size.width − 200dp·s)/2`, `oy = (40dp − 40dp·s)/2`, point (x, y) → (ox + x·s, oy + y·s): move (4, 34) → cubic (60, 2), (140, 2), (196, 34). Stroke 2 dp × s, `dawn`, `StrokeCap.Round`, `PathEffect.dashPathEffect([4dp·s, 6dp·s])` (created once in the cache). Dot radius 5 dp × s (`wizardArcDotRadius`), `dawn`, at `B(t)` (mapped the same way) (evaluate the cubic with floats; no `PathMeasure`, no allocation per frame).
  - `t` comes from `rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)))` mapped through `(sin(2π·v) + 1)/2`, read **only inside `onDrawBehind`** (the State object is captured; `.value` is read in draw), so no recomposition happens per frame.
  - Reduce Motion (`CfTheme.reduceMotion`): no transition is created; `t = 0.65f`.
- **Line list**: `Column(gap 12 dp, PlanUITokens.space3)`. Each line (tag `wizard.checking.line.<i>`): `Row(gap 12)`: a 24 dp circle (`space6`) then the label `wizardSubtitle` (16/22).
  - done: circle `dawn`, lucide `check` 14 dp in `dawnForeground` (stroke 3); label `foreground`.
  - busy: circle `muted`; `CfSpinner(size = 14.dp, color = mutedForeground)` (`wizardSpinnerSize`); under Reduce Motion an 8 dp `mutedForeground` dot instead; label `mutedForeground`.
  - open: circle `muted`, empty; label `mutedForeground`.
  - Semantics: `Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite; contentDescription = label }` where label is "{{line}}, done" / "{{line}}, klaar", "{{line}}, in progress" / "{{line}}, bezig", or the plain line [F `checkLineLabel` l.97–102]. Never `announceForAccessibility`.
- **Slow line** (tag `wizard.checking.slow`) when `checking.slow`: `wizardHint` (13/18), `mutedForeground`, inside the card under the list, polite live region. Copy "This is taking longer than usual. You can keep this screen open." / "Dit duurt langer dan normaal. Je kunt dit scherm open laten."

**Back** [F WizardSpec "Back on Checking"; `06 §3.3`; plan "navigation and routes"]: while the step is `checking` and `checking.failure == null`, `WizardBackHandling` installs `BackHandler { onEvent(WizardEvent.Back) }` (non-predictive; it is composed after 9c's `NavDisplay`, so it wins). `vm.back()` → 9a `popNavigation` → `go()` → `cancelCheck()` and the last input step (`lastInputStep`: return choice, return flight or sleep). With a failure, 9c's normal back applies (the chevron shows for non-edit; `06 §3.4`).

**Timing** is entirely 9a's `WizardChecking.run` on `checkingClock` (ticks 400 / 800 / ≥ 1200 ms, 300 ms pause, slow 10 000 ms, `Server(null)` at 30 000 ms). The UI only reads `snapshot.checking`. Nothing in `:feature:wizard` reads a clock or `delay`s for Checking.

### 5.2 Checking failure cards (`…/fw/checking/WizardCheckFailure.kt`) [F board l.132–138, l.382–390, l.476–478; iOS `checkFailure` l.104–157]

Shown instead of the checking card when `checking.failure != null`. Card (tag `wizard.checking.<kind>`): centred column, gap 12 dp (`space3`), padding 28 dp vertical (`wizardFailurePadding`) / 20 dp horizontal, radius 26, **dashed** 1 dp `border` stroke (dash 3 dp on, 3 dp off — iOS `wizardFailureDashLength`; drawn with `drawWithCache` + `dashPathEffect`, no fill), polite live region, `mergeDescendants`.
- Icon: 52 dp circle (`wizardFailureIconCircle`) `muted`, glyph (`LucideGlyphs.CloudOff`/`Clock`/`CircleAlert`; add the first two, see Stack check "Glyphs") 24 dp (`wizardFailureIconSize`) `mutedForeground`, cleared from semantics.
- Title `wizardFailureTitle` (Fraunces 22/28) `foreground`; body `wizardFailureBody` (15/22) `mutedForeground`; both centred.

| `failure` | kind / tag | lucide glyph | Title (en / nl) | Body (en / nl) |
|---|---|---|---|---|
| `DataFailure.Offline` | `offline` | `cloud-off` | Checking needs a connection / Voor controleren is een verbinding nodig | Connect to the internet and try again. / Maak verbinding met internet en probeer het opnieuw. Edit (`isEditing`): "Connect to the internet and try again. If you close now, your trip stays as it was." |
| `DataFailure.Throttled` | `throttle` | `clock` | Wait a moment / Even geduld | `retrySeconds > 0`: `LockoutWait(retrySeconds)` → "Too many attempts. Try again in %d seconds." (`_one` "…second."), ≥ 60 s → "…minutes" / "…minute", ≥ 3600 s → "…hours" / "…hour" (counts rounded **up**: 61 s → "2 minutes"); nl "Te veel pogingen. Probeer het over %d seconden opnieuw." etc. `retrySeconds == 0`: "Too many attempts. Please try again later." / "Te veel pogingen. Probeer het later opnieuw." |
| anything else | `server` | `circle-alert` | We couldn't check your trip / We konden je reis niet bekijken | Something went wrong on our side. Try again in a moment. / Er ging iets mis bij ons. Probeer het zo opnieuw. Edit: "… Try again in a moment. If you close now, your trip stays as it was." |

**Footer** (9c `WizardFooter` with `footerId = "checking"` only while `failure != null`, tag `wizard.checking.footer`), top to bottom with 6 dp gap:
- **Close** (tag `wizard.checking.secondary-close`): text button, `wizardSecondary` (Figtree500 16/22), `foreground`, full width, ≥ 48 dp → `WizardEvent.CloseFromFooter` (guest or no changes → close; else the discard dialog "Discard this trip?") [F `MemberWizardSteps.swift:69–80`]. Excluded keeps `CloseNow`.
- **Continue** (tag `wizard.checking.continue`): `retrySeconds > 0` → "Try again in {{count}} s" / "Opnieuw proberen over {{count}} s", **disabled** (alpha 0.4); else "Try again" / "Opnieuw proberen", enabled → `WizardEvent.Continue(checking)` → `continueFrom` → `beginChecking()`. The label updates every second from `snapshot.retrySeconds` (9a's `retryJob` on `checkingClock`; the UI keeps no timer).
- Disabled also while 9c's `closing` / `continuing` or `persistenceFailed` (9c rules).

### 5.3 ViewModel additions (`…/fw/TripWizardViewModel.kt` + `…/fw/checking/WizardCheckingUi.kt`, `…/fw/finetune/WizardFineTuneUi.kt`) [R]

> **Stack adaptation:** the shapes below become nullable `WizardUiState` fields built by pure `WizardCheckingUi.from(…)` / `WizardFineTuneUi.from(…)` called from `WizardUiState.from`; `sheet: AnswerSheetUi?` is 9e's `WizardAnswerSheetUi` (keep it on `WizardUiState.answerSheet`, not inside `WizardFineTuneUi`); intents are `WizardEvent`s; haptics are counters (see Stack check).

UI models are `@Immutable` data classes of raw values (keys, enums, numbers), never formatted strings; formatting happens in composables.

```kotlin
enum class CheckLineState { Done, Busy, Open }
enum class CheckLine { YourAnswers, FlightsAndTimeZones, YourSavedAnswers, WhatWeCanTailor }
enum class CheckFailureKind { Offline, Server, Throttle }
@Immutable data class WizardCheckingUi(
    val title: CheckingTitle /* Trip, Answers, Changes */, val from: String, val to: String,
    val lines: List<Pair<CheckLine, CheckLineState>>, val slow: Boolean,
    val failure: CheckFailureKind?, val retrySeconds: Int, val editing: Boolean)
@Immutable data class WizardFineTuneUi(
    val rows: List<ExtraRowUi>, val none: Boolean /* optionalPrompts.isEmpty() */,
    val preparationDays: Int, val showSleepSwitch: Boolean, val sleepOverride: Boolean,
    val overrideBed: String?, val overrideWake: String?, val preferences: Map<String, Boolean>,
    val fineTuneErrors: List<Pair<String, FieldMessage>> /* override.* and preparation.*, sorted by key */,
    val reviewing: Boolean, val sheet: AnswerSheetUi?)
@Immutable data class ExtraRowUi(
    val promptId: String, val kind: PromptKind, val index: Int, val labelKey: String,
    val suffix: String? /* " · AMS → DXB" or " · Dubai" (city), null when single */,
    val required: Boolean, val answer: AnswerText? /* null → "Add" */, val error: FieldMessage?)
```

- `WizardUiState.checking` / `.fineTune`, set only on their step (as `sleep`, `excluded`). A tick changes only `lines`; Compose then recomposes only the line list (Task 5 test).
- Failure mapping: `is DataFailure.Offline` → `Offline`; `is DataFailure.Throttled` → `Throttle`; else `Server`.
- Row label (port of `optionalLabel` + `WizardPromptLabels.key`, `MemberWizardFineTuneView.swift:163–192`): key by kind — `seat_side` "Side of the plane", `room` "Dark room at your stay", `late_check_out` "Late check-out", `latest_wake` "Latest you can wake up", `pre_departure_busy_hours` "Busy hours before you leave", `free_day_times` "Sleep on free days", `cups` "Coffee, tea and soft drinks", `light_device` "Light box or light glasses", `blue_blockers` "Blue-blocking glasses", other → `prompt.text.key`. When `legs.size > 1` and `index = (scope.journey == return ? returnBoundary : 0) + (scope.legIndex ?: scope.stayIndex ?: 0)` is a valid leg: `seat_side` → label + " · " + `from` + " → " + `to`; `room` → **"Dark room"** + " · " + (`toCity ?: toIata`); `late_check_out` → label + " · " + city. Otherwise the plain label.
- Row answer: 9a `WizardPresentation.promptAnswerText(prompt, reviewInput, language, t)`; a failure (exception) or the "Add" sentinel → `null` (the row shows "Add" in `dawnText`).
- Row error: 9a `WizardPresentation.promptFieldError(prompt, form, fieldErrors)`.
- **Intents** (`WizardEvent`s in `onEvent`, each guarded on `closing`):
  - `Continue(checking)` → `beginChecking()`; `Continue(fineTune)` → `finishFineTune()` (both in `continueFrom`).
  - `OpenPrompt(id: String?)` → model `openPrompt(id)`.
  - Sheet Done is 9e's `AnswerDone` → `answerDone()` (already finds the prompt and calls `answerOptional`; keep its catches). `answerOptional` throws what 9a's `TripGenerationAnswers.applyingAnswers` throws (`DataFailure.Validation(mapOf("review" to …), 422)` for an unsupported prompt or foreign field, `DataFailure.ReviewExpired` for a path through a missing index; 9a brief Task 6 item 5) and whatever `WizardForm.from(reviewInput, retained)` throws. iOS catches every error (`WizardAnswerSheet.swift:381–383`), so: `catch (e: DataFailure)` → `AnswerResult.Error("Choose one of the available options.")`; if 9a's `WizardForm.from` throws another named type, catch that type too (name it in the PR). Never `catch (e: Exception)` (AGENTS "Traps": `CancellationException`).
  - `SetPreparationDays(days)` → `editForm { it.copy(preparation = it.preparation.copy(days = days.coerceIn(0, WizardForm.maxPreparationDays))) }`; ignored when unchanged.
  - `SetSleepOverride(enabled)` (port of the iOS toggle setter, `MemberWizardFineTuneView.swift:102–121`): `editForm` sets `override.enabled`; when enabling, `habitualBed = override.habitualBed ?: state.profile["habitual_bed"] ?: savedProfile["habitualBed"]` and the same for wake. Disabling keeps the stored times.
  - Override times: `ConfirmTimeSheet` on the override fields (minute precision) → `editForm` writes `override.habitualWake`/`habitualBed`.
  - `SetPreference(field, value)` → model `setPreference` (only `exclude_caffeine`, `flight_movement_breaks`; 9a ignores others).
  - `Continue(fineTune)` → model `finishFineTune()` (unchanged fingerprint → Ready; else inline review).
- **Haptic cues** [R]: the VM compares consecutive snapshots in its `snapshots.collect` and bumps a sequence number with the cue (`Success`/`Warning`/`Error`) in UI state; `WizardScreen` plays it once per new sequence (`LaunchedEffect` + `rememberSaveable` handled value, as 9e's sheet `failures`). Rules:
  - step was `checking` **with `checking.ticks == 3` and `failure == null` in the previous snapshot** (the review completed: 9a `WizardChecking.run` emits `tick(3)` only after a response, then `complete` 300 ms later; `WizardChecking.swift:48–54`) and is now something else → `Success`, or `Warning` when the new step is `excluded` [F `MemberWizardCheckingView.swift:87–91`; WizardSpec "Success: Checking finishes and the next screen appears"]. Leaving `checking` without a completed review (system back → `cancelCheck`, a 422 routed to its owning step) fires nothing here;
  - `checking.failure` changes from `null` to non-null → `Error` [F l.92–94].
  - The cue survives the step change (it lives outside `WizardCheckingUi`, e.g. a top-level `WizardUiState.cue`), so the next screen plays it.
- **Debug UI override seam** (§5.10): `TripWizardDependencies.uiOverride: WizardUiOverride? get() = null` (additive, default null). `WizardUiState.from` takes an optional `uiOverride` and replaces `checking`, `retrySeconds` and `reviewing` in the UI models; the VM passes it and seeds the sheet's initial `error` with `answerError`, and does not call `resumeChecking()`/`resumeCompletion()` when it is set. `boardState()` passes the same override for Robolectric captures.

### 5.4 Fine-tune screen (`…/fw/finetune/WizardFineTuneStep.kt`) [F board l.155–185, l.414–428, l.481–482; iOS `fineTuneScreen` l.22–145]

Screen tag `wizard.fine-tune`. Title "Fine-tune your plan" / "Stem je plan af". Sub: `none` → "No extra questions for this trip. You can still change these." / "Geen extra vragen voor deze reis. Je kunt deze nog wel aanpassen."; else "All optional. The ones marked help your plan most." / "Allemaal optioneel. De gemarkeerde helpen je plan het meest." Content scrolls; cards are separated by 14 dp (9c item gap).

**Card 1, extras** (tag `wizard.extras`; absent when `none`): radius 16 (`wizardCardRadius`), fill `card`, 1 dp `border`, clipped; rows separated by a 1 dp `border` top line.
- Row (tag `wizard.extra.<kind>.<index>`, `Role.Button`, `cfClickable`, merged semantics): min height 52 dp (`wizardButtonHeight`), padding 6 dp vertical, 16 dp start, 12 dp end, gap 8 dp.
  - Start column (`weight(1f)`): label (+ suffix) `wizardSearchName` (Figtree400 16/21) `foreground`; if `required`: a row with lucide `star` 12 dp **filled** `dawnText` + "Helps a lot" / "Helpt veel" (12/16, §4) `dawnText`, gap 4 dp, top margin 2 dp; if `error`: 9d `WizardErrorLine` (tag `wizard.extra.error.<promptId>`).
  - Value: `wizardFieldLabel` (Figtree400 15/20), one line, no wrap; answered → `foreground`; unanswered → "Add" / "Toevoegen" in `dawnText`.
  - Chevron: lucide `chevron-right` 16 dp `mutedForeground`, cleared from semantics.
  - Tap → `OpenPrompt(row.promptId)`. Disabled (semantics only, no visual change) while `reviewing` [F `.disabled(model.reviewing)`].
- Row order = 9a `optionalPrompts` order (required first). No sorting in the UI.

**Card 2, preparation and sleep** (same card style):
- **Preparation days** row (tag `wizard.preparation`): min 52 dp, padding as above, gap 10 dp. Start column: "Preparation days" / "Voorbereidingsdagen" (`wizardSearchName`) + "Before you leave" / "Voor vertrek" (`wizardHint`, `mutedForeground`). Value: digits `wizardButton` (17, 600, tabular numbers via `fontFeatureSettings = "tnum"`), min width 14 dp, centred. Then the **stepper** (§5.5) for 0…4. Each step → `vm.setPreparationDays(±1)` + `Selection` haptic.
- **Sleep switch** row (only when `showSleepSwitch` = `allowsSleepOverride`; tag `wizard.sleep-override`): top border, min 52 dp, label "Use different sleep times for this trip" / "Andere slaaptijden voor deze reis" (`wizardSearchName`), `CfSwitch(checked, onCheckedChange = null)` with the row `cfToggleable(value, vm::setSleepOverride)` (Phase 2 fires `ToggleOn`/`ToggleOff`).
- When `sleepOverride`: two rows (top border, min 48 dp, padding 6/16/6/12, gap 8): "Bedtime on this trip" / "Naar bed op deze reis" and "Wake-up on this trip" / "Wakker op deze reis" (`wizardSearchName`), each a 9e `WizardTimeRow` (tags `wizard.override.bed`, `wizard.override.wake`; value via `WizardDateFormat.time(value, account timeFormat, language)`). Tap → `OpenTimeSheet` → 9e's time sheet (`CfTimePickerSheet`; phone 12/24 h via `DateFormat.is24HourFormat`, app language, title = the row label, no footnote), **minute precision, no rounding** (iOS `WizardTimeControl`; OQ-9f-3, OQ-9f-18). A changed Done → `editForm` writes `override.habitualBed`/`habitualWake` + one `Selection`; untouched Done and Cancel write nothing. Then the note "Only for this trip. Your usual sleep stays saved." / "Alleen voor deze reis. Je gewone slaaptijden blijven bewaard." (`wizardHint`, `mutedForeground`, padding 0/16/12).
- Field errors (`fineTuneErrors`, keys starting `override.` or `preparation.`, sorted): one 9d `WizardErrorLine` each at the end of card 2 (tag `wizard.fine-tune.error.<key>`, padding 16) [F l.133–142].
- The whole card is disabled (semantics) while `reviewing`.

**Card 3, preferences** (explicit design exception, §4; tag `wizard.preferences`): same card style, two `cfToggleable` rows (padding 16, `Role.Switch`), separated by a 1 dp `border`:
- "Leave caffeine out of my plan" / "Laat cafeïne weg uit mijn plan" + hint "Your plan won't suggest any caffeine." / "Je plan stelt geen cafeïne voor." (tag `wizard.preference.exclude_caffeine`);
- "Add movement breaks during long-flight sleep" / "Voeg beweegpauzes toe tijdens slaap op lange vluchten" + hint "Your plan breaks up long sleep on long flights with short movement breaks. If you're at higher risk of blood clots, choose this and ask your doctor about compression stockings and an aisle seat." / nl from the catalog (tag `wizard.preference.flight_movement_breaks`).
- Title `wizardChronotype` (16/22 600), hint `wizardHint` `mutedForeground`, gap 2 dp (`wizardLabelGap`); value from `preference(field)`; change → `vm.setPreference`. Disabled while `reviewing`.

**Footer** (tag `wizard.fine-tune.footer`, button tag `wizard.fine-tune.continue`) [F iOS l.30–31; board l.481–482]:
- `reviewing` → `CfButton(loading = true, loadingDescription = label, disabledAlpha = WizardMetrics.ctaBusyAlpha)` (§4), label "Checking…" / "Controleren…", spinner first, alpha 0.7, disabled.
- else `none` → "Done" / "Klaar"; else "Continue" / "Doorgaan". Enabled → `vm.finishFineTune()`.

**Layout** [F WizardCopy "Layout"]: at 390 × 844 dp en the member Fine-tune (cards 1–2) fits without scrolling at font scale 1.0; at 375 × 667 it scrolls with the footer fixed. Card 3 lies below the fold on 390 × 844 (expected; §4).

### 5.5 Drawn stepper (`…/fw/components/WizardStepper.kt`) [F `Wizard.dc.html` l.163, `WizardSheet.dc.html` l.73, `minusStyle` l.116; WizardCopy "Android"]

`@Composable fun WizardStepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit, modifier, testTag: String)`:
- Visible group 94 × 32 dp, radius 9 dp, fill `wizardPickerFill`; two halves "−" (U+2212) and "+" (20 sp, `foreground`, centred, line height 32), separated by a 1 dp `border` vertical line on the minus half's end.
- Each half is clickable with a 48 × 48 dp touch target centred on the half (`minimumInteractiveComponentSize()` on each half inside a `Box` that does not grow the drawn group; the targets may overlap the 1 dp gap — the guard tolerates the drawn 32 dp height because the target is 48).
- Minus disabled at `range.first` (alpha 0.35); plus disabled at `range.last` (alpha 0.35, symmetric) [F board `minusStyle` `opacity: 0.35` at 0].
- Semantics (no new copy): the group is one node, `contentDescription = label`, `stateDescription = value.toString()`, `progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1)`, `setProgress { target -> onChange(target.roundToInt().coerceIn(range)); true }`; the two halves are `clearAndSetSemantics {}` for TalkBack but keep their test tags `<testTag>.minus` / `<testTag>.plus` through `testTag` on the clickable (use `Modifier.semantics { testTag = … }` merged into the cleared node only for tests: put the tag before `clearAndSetSemantics`). OQ-9f-7.
- Every accepted change fires `Selection` (the caller does it, so the sheet's local edits and the form edits stay symmetric).

### 5.6 Answer sheet scaffold (9e's `…/fw/answers/WizardAnswerSheet.kt`, extended) [F `WizardSheet.dc.html` l.25–81; iOS `WizardAnswerSheet.swift:50–111`; `MemberWizardView.swift:112–121`]

- **9e reuse (settled).** 9e merged the scaffold; extend it: per-kind bodies in `…/fw/answers/` (the free-day body stays as it is), `SUPPORTED_SHEET_KINDS` and `WizardPromptLabels.key` widened. Keep 9e's names, header, padding and focus behaviour.
- Shown while `draft.state.promptSheetID != null` **and** the id is in `optionalPrompts` (sheets are driven by state, so recovery reopens them; plan "state survival"). Hosted at the Fine-tune step level.
- `CfModalBottomSheet(onDismissRequest = { onEvent(CancelAnswer) })` (drag, scrim tap and system back = Cancel; nothing written). Tag `wizard.answer.<kind raw>`.
- Content: 9e's column (scroll, `navigationBarsPadding`, padding 10/20, gap `wizardContentGap` 14) [F `sheetStyle` padding 10 20 34, gap 14]; add `imePadding()` for the rating field.
  1. Header row (9e `WizardAnswerHeader`): Cancel (tag `wizard.answer.cancel`) → `CancelAnswer`; title = the row label **with suffix** (`wizard.answer.title`, `heading()`; pass the suffixed label in, 9e passes the bare key); Done (tag `wizard.answer.done`) enabled iff `state.doneEnabled` → `AnswerDone`.
  2. Question (tag `wizard.answer.question`): `rememberServerCopy(formatter)(prompt.text)` (with its args, e.g. city "Tokyo"), 9e's style, `foreground`.
  3. Caption: `seat_side` → `"$from → $to"` of the prompt's leg (`wizardHint`, `mutedForeground`, top margin −6 dp → use a 8 dp gap instead of 14 for this pair) [F `WizardSheet.dc.html` l.49]; `light_device` → the iOS notice "Don't use a light box or light glasses if you have an eye disease, have had recent eye surgery or take medicine that makes your eyes sensitive to light. Ask your doctor first." / nl from the catalog (`wizardHint`, `mutedForeground`, tag `wizard.answer.lightNotice`).
  4. Options card (if the kind has options): radius 16, 1 dp `border`, fill **`background`** (not card), `selectableGroup()`. Rows (tag `wizard.answer.option.<i>`): min 52 dp, padding 8/16, gap 12; label `wizardRow` (17/22); optional sub `wizardHint` `mutedForeground`; selected → lucide `check` 20 dp `dawnText` (stroke 2.6) at the end; `Modifier.selectable(selected, role = Role.RadioButton)`; tap → `draft.select(value)` + `Selection` haptic. 1 dp `border` between rows.
  5. Kind controls (§5.8).
  6. Error line (tag `wizard.answer.error`): 9e's `WizardErrorLine(fieldCopy(error, formatter), …)`, polite live region.
- **Sheet state**: 9e's VM-held `WizardAnswerSheetState` (survives rotation, fold and locale recreation with the ViewModel). Process death reopens the sheet with values reloaded from the input (iOS parity; the open sheet itself comes back from `promptSheetID`). The rating `TextFieldState` is seeded from the state and writes every edit back through `UpdateAnswer`.

### 5.7 `WizardAnswerSheetState` additions (pure, 9e's `…/fw/answers/WizardAnswerSheetState.kt`) [F port of the logic in `WizardAnswerSheet.swift`]

"One nullable selection and its validation error" [plan]. Keep 9e's type and fields (`prompt`, `values`, `error: FieldMessage?`, `failures`) and its `load`/`update`/`validate`/`missingTimes`/`doneEnabled`; add the options and helpers below as members or pure functions. The `@Serializable WizardAnswerDraft` shape and its `Saver` below are **superseded** (VM state; no saver). Read "draft" below as this state. `UpdateAnswer(field, value)` stays the only edit event: `update` applies the per-kind side effects below (`select`, `setCount`, `setRoomTime`, `putBusy`/`removeBusy` as pure helpers that produce the field writes), so the VM's `field in prompt.fields` filter keeps working.

```kotlin
@Serializable data class WizardAnswerDraft(
    val promptId: String, val kind: PromptKind,
    val values: Map<String, JsonElement>, val error: String? /* iOS English key */)
data class AnswerOption(val value: JsonElement, val labelKey: String, val hintKey: String? = null)
```

- `optionField(kind)` = `"stay.bed_available_mode"` for `room`, else `prompt.fields.first()`.
- `options(kind)` exactly [F l.22–49]:
  - `seat_side`: `"left"` "Window left", `"right"` "Window right", `JsonNull` "Aisle or not sure" (hint "We plan without the window");
  - `room`: `"arrival"` "When I land", `"declared"` "From a time";
  - `late_check_out`: `true` "Yes", `false` "No";
  - `free_day_times`: `"same_as_workdays"` "Same as work days", `"declared"` "Different times";
  - `light_device`: `"none"` "None", `"box"` "Light box", `"glasses"` "Light glasses";
  - `blue_blockers`: `"no"` "No", `"rated"` "Yes, with a rating", `"unrated"` "Yes, no rating";
  - `cups`: `"non_user"` "None";
  - others: none.
- `isSelected(option)` = `values[optionField] == option.value`, where a missing key counts as `JsonNull` (so an unanswered seat shows "Aisle or not sure" checked, as iOS).
- `load(prompt, reviewInput, savedProfile, fieldErrors, form, language = En, initialError: String? = null)` (9e's signature plus two defaulted params) [F `load()` l.129–155]: 9e already does `promptAnswers` (fallback `prompt.answer`), the saved-profile camel fallback and `promptFieldError`; add: a numeric `blue_blocker_mdfd` becomes a string with the app language's decimal separator ("1.5" en, "1,5" nl; build it by hand — no `String.format`, no `NumberFormat`); `error = promptFieldError(…) ?: initialError?.let(::FieldMessage)`.
- Every update clears `error` [F `update` l.367–370]:
  - `select(option)`: `values[optionField] = option.value`; for `cups` also every `drinkFields` field = `0`.
  - `setCount(field, n)` (cups): `values["caffeine_habit"] = "cups"`, `values[field] = n` (as a JSON integer), `n in 0..(prompt.shape.max ?: 20)`.
  - `setTime(field, "HH:mm")` (`latest_wake`, and 9e's free-day fields).
  - `setRoomTime("HH:mm", landingDate)`: `values["stay.bed_available_from_local"] = "${landingDate}T$time"`, `landingDate = form.legs[legIndex].arriveLocal.take(10)`; ignored when `legIndex` is out of range.
  - `setRating(text)`: raw text as a string.
  - `putBusy(index: Int?, start, end)`: replaces row `index` or appends `{start_local, end_local}`; sets `pre_departure_busy_answered = true`. `removeBusy(index)`: removes and sets `answered = true`.
- `missingTimes` [F l.121–128]: `free_day_times` with status `declared` and either time not `HH:mm`; `latest_wake` not `HH:mm`; `room` with mode `declared` and `bed_available_from_local` not a valid wall date-time.
- Done [F `done()` l.371–384] stays 9e's `answerDone()`: `validate()` non-null → `error` + `failures + 1` (one `Warning`); else add a pure `submittedValues()` that, for `blue_blockers == "rated"`, replaces `,` by `.` in `blue_blocker_mdfd`, and pass that to `answerOptional`; failures map to "Choose one of the available options." (already there).

### 5.8 Kind controls (`…/fw/answers/*`) [F `controls` l.186–236; `WizardSheet.dc.html` `K` l.98–113]

- **Seat** (`seat_side`): options only.
- **Room** (`room`): options; when mode `declared`, a 9e `WizardTimeRow` in a card (radius 16, `border`, fill `background`, `minHeight = sheetRowMinHeight`): label `wizard.room.from` ("From" / "Vanaf"), tag `wizard.answer.room-time`, the time part via `WizardDateFormat.time`. Tap → `OpenTimeSheet` → the time sheet (title = sheet title, subtitle "Time in {{city}}" of the landing city or "Local time", minute precision) → `setRoomTime`. When the landing leg has no arrival date the pill is disabled (OQ-A36 "room From disable").
- **Check-out** (`late_check_out`): options only.
- **Latest wake** (`latest_wake`): an inline time picker in a card (radius 16, 1 dp `border`, fill `background`), tag `wizard.answer.latest_wake`: `CfTimePicker` (Task 3) with `is24Hour = DateFormat.is24HourFormat(context)`; `TimeInput` instead of the dial when the window height is < 640 dp (OQ-A36 "TimeInput at short height"; OQ-9f-6). Initial value = `latest_wake` or 12:00 when empty. `snapshotFlow { state.hour to state.minute }.drop(1)` → `draft.setTime("latest_wake", "%02d:%02d" built by hand)` + `Selection`. Until the user changes it, an empty value keeps Done disabled (iOS parity).
- **Busy hours** (`pre_departure_busy_hours`) [F l.276–357]:
  - When there are intervals: a card (radius 16, `border`, fill `background`), rows (min 52, padding 8/16, gap 12, 1 dp `border` between): text `wizardSearchName` (16/21, tabular), tap → edit that range; a 32 dp circle (`wizardPickerFill`) with lucide `x` 14 dp `mutedForeground` inside a 48 dp target, `contentDescription` "Remove {{range}}" / "Verwijder {{range}}", tag `wizard.answer.remove-busy.<i>` → `removeBusy(i)`.
  - Range text: start `EEE d MMM` (9a's fixed weekday/month tables; en "Fri 9 Oct", nl "vr 9 okt") + " · " + start `HH:mm` + "–" + (same date ? end `HH:mm` : end `EEE d MMM` + " · " + end `HH:mm`); invalid → "Choose" / "Kies". Times are the raw `HH:mm` wall strings (iOS; OQ-9f-4).
  - "Add busy hours" / "Drukke uren toevoegen" (tag `wizard.answer.add-busy`): lucide `plus` 16 dp + text 16/21 in `dawnText`, ≥ 48 dp, start-aligned.
  - **Picking a range** (Android form of iOS's two `PickerDateTime` sheets; OQ-9f-5): From date → From time → To date → To time, each through `CfDatePickerDialog`/`CfTimePickerDialog` with title "From" / "Van" or "To" / "Naar" and supporting "Time in {{city}}" of `legs[0].fromCity` (else "Local time"). Dates use 9d's `WizardPickerDates` UTC-millis adapter. Initial From = the row's start, else `legs[0].departLocal`, else airport-local now on the injected clock. To date: lower bound = From date; initial = previous end's date or From. To time: initial = previous end, else From + 1 h; **Done disabled while To ≤ From** (9d's `confirmEnabled`). Cancel at any stage writes nothing. The last Done → `putBusy`. The dialog chain state is `rememberSaveable`.
  - No cap in the UI (the server and 9a validation cap at 32).
- **Drinks** (`cups`) [F l.237–260; `WizardSheet.dc.html` `K.drinks` l.108, template l.71–74, `stepGroupStyle` l.131]: the "None" option card, then "Cups a day" / "Kopjes per dag" (`wizardHint`, `mutedForeground`, margin 0 4 −6), then a card (radius 16, `border`, fill `background`; tag `wizard.answer.drinks`) with four rows (min 52, padding 8/16, gap 12): label `wizardRow` ("Coffee"/"Koffie" → `cups_filter_coffee`, "Tea"/"Thee" → `cups_tea`, "Soft drinks"/"Frisdrank" → `cups_cola`, "Energy drinks"/"Energiedrank" → `cups_energy_drink`), count `wizardButton` tabular min 20 dp, `WizardStepper(range = 0..(shape.max ?: 20), testTag = "wizard.answer.<field>")`. Error state: a 2 dp `destructive` ring **outside** the card (`border` stays) when `draft.error != null`.
- **Light** (`light_device`): options + the notice (§5.6 item 3).
- **Blue** (`blue_blockers`): options; when `rated`, a rating row (radius 16, `border`, fill `background`, min 52, padding 8/16, gap 12): label "Rating on the label" / "Waarde op het etiket" (`wizardRow`) + a right-aligned `BasicTextField(TextFieldState)` (tag `wizard.answer.rating`, `wizardRow` tabular, `KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = Done)`, cursor `dawn`, accepts "," and "."; every edit → `setRating`). Error: 2 dp `destructive` ring, border transparent [F board `ratingStyle`].
- **Free days** (`free_day_times`): 9e's body.

### 5.9 Haptics (WizardSpec "Haptics" through `LocalHaptics`) [F `06 §3.7`; plan l.439–448]

| Event (9f) | `HapticEvent` |
|---|---|
| Sheet option tap; each stepper step (prep, drinks); wake picker value change; time dialog Done (override, room, busy) | `Selection` |
| Sleep-override switch, preference toggles | `ToggleOn` / `ToggleOff` (Phase 2 `cfToggleable`; plan Material mapping "Switch"; OQ-9f-12) |
| Checking ends and the next screen appears | `Success` (`Warning` when it is `excluded`) |
| Sheet Done with an invalid answer | `Warning` |
| Checking failed (offline, server, throttle) | `Error` |

Cancel, dismiss, Close, Try again and Continue fire nothing here (Continue's `Medium` belongs to "Make my plan", 9g).

### 5.10 Board seeds and the UI override (`…/fwd/`, debug only) [F port of `WizardBoardCaptureSeed.swift:163–167, 211–231`, `WizardCompletionBoardSeed.swift`]

Extend 9c's `WizardBoardCaptureSeed` registry with a sibling `internal object WizardCompletionBoardSeed` (`names`, `apply(base, board)`), as 9e's `WizardSleepQuestionBoardSeed` (the suffixes `Dark`, `NL`, `DarkNL` set theme/language only). Base = 9c's base draft (AMS → NRT 2026-10-12T14:40 → 2026-10-13T09:15, `entered`, profile 23:30/07:15/intermediate; NRT city "Tokio" in nl).

**Checking boards** (`WizardChecking`, `WizardCheckingAgain`, `WizardCheckingSlow`, `WizardCheckOffline`, `WizardCheckServer`, `WizardCheckThrottle`): step `checking`, `lastInputStep = returnChoice`, `checkingAnswers = (board == WizardCheckingAgain)`. `WizardUiOverride(checking = WizardCheckingProgress(ticks = 2, slow = board == WizardCheckingSlow, failure = Offline | Server(500) | Throttled(42.0, 429) | null), retrySeconds = if (Throttle) 42 else 0)`.

**Completion boards** (`WizardFineTune`, `…Multi`, `…Sleep`, `…Busy`, `…None`, `WizardFineTuneSheet`, `WizardSheet`, `WizardSheetRoom`, `…Checkout`, `…Wake`, `…Busy`, `…Drinks`, `…DrinksError`, `…Light`, `…Blue`, `…BlueError`), exactly as `WizardCompletionBoardSeed.apply` l.18–140:
- `Multi`: DXB inserted before the destination of leg 0; `legs[0].arriveLocal = 2026-10-12T23:25`, `legs[1].departLocal = 2026-10-13T02:05`, `legs[1].arriveLocal = 2026-10-13T16:35`.
- `SheetCheckout`: `addReturn()`; home leg To AMS / Amsterdam, `2026-10-22T10:25` → `2026-10-22T15:30`; `returnRouteConfirmed = true`.
- `Sleep`: `override` enabled 00:30 / 08:30.
- `preparation.latestWake = "08:30"` for all.
- `SheetRoom`, `Sleep`: `stays[0]` mode `declared`, from `2026-10-13T15:00`.
- `SheetBusy`: `busyAnswered = true`, intervals `2026-10-09T09:00–2026-10-09T17:30`, `2026-10-12T08:00–2026-10-12T11:00`.
- `WizardSheet`, `FineTuneSheet`, `FineTune`, `FineTuneBusy`, `FineTuneMulti`: `legs[0].seatWindow = "left"`.
- `savedProfile = { freeDayStatus: "declared" ("same_as_workdays" for Sleep), freeDayBed "00:30", freeDayWake "09:00", lightDevice "box", blueBlockers "rated", blueBlockerMdfd: number 1.5 (12 for BlueError; §4; iOS stores strings), caffeineHabit "cups", cupsFilterCoffee: 2 (0 for DrinksError) }`. 9e's `WizardSheetFreeSleep` keeps its saved values but now renders over the real Fine-tune (its prompts as this list).
- Prompts: the output of iOS `MockWizardPrompts.make(input, savedProfile merged with state.profile)` (`I/Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockWizardPrompts.swift`), **built by hand in the seed** (ids `kind:journey:index`, fields, shapes, text keys and args exactly as the Swift; 9e precedent). Konsist forbids `:core:data-mock` in `:feature:wizard` main/debug; Task 4's seed test may compare against Kotlin `MockWizardPrompts.make` through `testImplementation(project(":core:data-mock"))`. For FineTune/Busy/Multi also append the `late_check_out` prompt of the same input with `return = outbound`, then order by `[room, seat_side, late_check_out, latest_wake]` (Multi) or `[room, latest_wake, seat_side, late_check_out, pre_departure_busy_hours, cups]`; Multi sets `latest_wake.required = false`. Sleep keeps only `room`, `latest_wake`, `free_day_times`. None → `[]`.
- Review: identity `("capture", 1, 1, null)` (also `form.identity`), status `ok`, the iOS summary and price (`create`, cost 1, balance 3, `canSave = true`), empty itinerary; `reviewedAt` = the seed clock; `lastInputStep = returnChoice`; step `fineTune`; `promptSheetID` = the first prompt of the sheet's kind (`WizardSheet`/`FineTuneSheet` → `seat_side`, `Room` → `room`, `Checkout` → `late_check_out`, `Wake` → `latest_wake`, `Busy` → `pre_departure_busy_hours`, `Drinks*` → `cups`, `Light` → `light_device`, `Blue*` → `blue_blockers`); `checkedInput` = the draft's input fingerprint (iOS `freezeCompletionBoard`).
- `FineTuneNone` only [R]: `progressFloor = 0.95` (no iOS line; OQ-9f-17; §4 progress row — the state is reached from Ready, so the floor is what a user sees; the board draws 95).
- `WizardUiOverride(reviewing = board == WizardFineTuneBusy, answerError = "Enter a rating between 0.1 and 9.9." for BlueError / "Add at least one drink to save this answer." for DrinksError)`.

**Seam** [R]: `data class WizardUiOverride(val checking: WizardCheckingProgress? = null, val retrySeconds: Int? = null, val reviewing: Boolean? = null, val answerError: String? = null)` lives in `…/fw/shell/WizardUiOverride.kt` (main: a neutral data holder, like 9c's `WizardStartOverride` seam). Only `WizardDebugSeeds` (`app/src/debug/…/WizardDebugSeeds.kt`, inside its `object : TripWizardDependencies by session`) sets `uiOverride` and, for these boards, a frozen `checkingClock` (`WizardCheckingClock(nowMillis = { 0 }, sleepUntilMillis = { awaitCancellation() })`, `…/fwd/FrozenCheckingClock.kt`) so no tick, timeout or countdown runs; `boardState()` passes the override straight to `WizardUiState.from`. Extend `app/src/testRelease/…/ReleaseReadinessTest.wizardCaptureCodeAndStartOverrideAreAbsent`: no `WizardCompletionBoardSeed`/`FrozenCheckingClock` class, and a release `TripWizardDependencies` has `uiOverride == null`.

### 5.11 Copy

All copy comes from the iOS wizard catalog (0c generated en + nl; look names up in `tools/l10n/names.lock.json`) or, for `prompt.text`, from the PlanCopy catalog via 1a. **No Android-only copy is added.** Keys used (en key → nl):

| Key | nl |
|---|---|
| Checking your trip / Checking your answers / Checking your changes | We bekijken je reis / We bekijken je antwoorden / We bekijken je wijzigingen |
| This takes a few seconds. | Dit duurt een paar seconden. |
| Flights and time zones / Your saved answers / Your answers / What we can tailor | Vluchten en tijdzones / Je opgeslagen antwoorden / Je antwoorden / Wat we kunnen afstemmen |
| {{line}}, done / {{line}}, in progress | {{line}}, klaar / {{line}}, bezig |
| This is taking longer than usual. You can keep this screen open. | Dit duurt langer dan normaal. Je kunt dit scherm open laten. |
| Checking needs a connection / Connect to the internet and try again. (+ edit variant) | Voor controleren is een verbinding nodig / Maak verbinding met internet en probeer het opnieuw. |
| We couldn't check your trip / Something went wrong on our side. Try again in a moment. (+ edit variant) | We konden je reis niet bekijken / Er ging iets mis bij ons. Probeer het zo opnieuw. |
| Wait a moment / Too many attempts. Please try again later. | Even geduld / Te veel pogingen. Probeer het later opnieuw. |
| Too many attempts. Try again in %lld seconds/minutes/hours. (plurals, via `LockoutWait`) | Te veel pogingen. Probeer het over %lld seconden/minuten/uur opnieuw. |
| Try again / Try again in {{count}} s / Close | Opnieuw proberen / Opnieuw proberen over {{count}} s / Sluiten |
| Fine-tune your plan / All optional. The ones marked help your plan most. / No extra questions for this trip. You can still change these. | Stem je plan af / Allemaal optioneel. … / Geen extra vragen voor deze reis. … |
| Helps a lot / Add / Continue / Done / Checking… / Cancel | Helpt veel / Toevoegen / Doorgaan / Klaar / Controleren… / Annuleren |
| Preparation days / Before you leave | Voorbereidingsdagen / Voor vertrek |
| Use different sleep times for this trip / Bedtime on this trip / Wake-up on this trip / Only for this trip. Your usual sleep stays saved. | Andere slaaptijden voor deze reis / Naar bed op deze reis / Wakker op deze reis / Alleen voor deze reis. … |
| Leave caffeine out of my plan / Your plan won't suggest any caffeine. / Add movement breaks during long-flight sleep / (hint) | Laat cafeïne weg uit mijn plan / Je plan stelt geen cafeïne voor. / Voeg beweegpauzes toe tijdens slaap op lange vluchten / (catalog) |
| Row labels (§5.3) and "Dark room" | Kant van het vliegtuig / Donkere kamer op je verblijf / Laat uitchecken / Uiterlijk wakker om / Drukke uren voor vertrek / Slaap op vrije dagen / Koffie, thee en frisdrank / Lichtbox of lichtbril / Blauwfilterbril / Donkere kamer |
| Option labels (§5.7), "We plan without the window" | Raam links / Raam rechts / Gangpad of weet ik niet / We plannen zonder het raam / Als ik land / Vanaf een tijd / Ja / Nee / Geen / Lichtbox / Lichtbril / Ja, met een waarde / Ja, zonder waarde |
| Answer texts (9a `promptAnswerText`): From {{time}}, {{count}} time ranges (+`_one`), {{count}} a day (+`_one`), Same as work days, Different times, Rated {{rating}} | Vanaf {{time}} / {{count}} tijdvakken / {{count}} per dag / Zoals op werkdagen / Andere tijden / Waarde {{rating}} |
| wizard.room.from / From / To / Time in {{city}} / Local time / Time | Vanaf / Van / Naar / Tijd in {{city}} / Lokale tijd / Tijd |
| Cups a day / Coffee / Tea / Soft drinks / Energy drinks | Kopjes per dag / Koffie / Thee / Frisdrank / Energiedrank |
| Rating on the label / Add busy hours / Remove {{range}} / Choose | Waarde op het etiket / Drukke uren toevoegen / Verwijder {{range}} / Kies |
| Add at least one drink to save this answer. / Enter a rating between 0.1 and 9.9. / Check your bedtime and wake-up time. / Choose one of the available options. | Voeg minstens één drankje toe om dit antwoord te bewaren. / Vul een waarde tussen 0,1 en 9,9 in. / Controleer je bedtijd en opstatijd. / Kies een van de beschikbare opties. |
| The light-device notice "Don't use a light box or light glasses if …" | catalog nl |

The board copy ("Helpt veel", "Kopjes per dag" …) equals the catalog; where they differ, the catalog wins (0c rule; WizardCopy "the app wins").

## 6. Kotlin shape [R]

```
tools/icons/lucide.json + core/designsystem/…/icon/LucideGlyphs.generated.kt   + cloud-off, clock, star (board source; `make vectors`)
core/designsystem/…/system/CfTimePicker.kt          inline TimePicker/TimeInput with CfTimePickerDialog's colors (none exists yet)
feature/wizard/…/shell/WizardUiOverride.kt          the neutral seam (§5.10) + TripWizardDependencies.uiOverride default null
feature/wizard/…/shell/WizardEvent.kt               (9c's) + OpenPrompt, SetPreparationDays, SetSleepOverride, SetPreference
feature/wizard/…/shell/WizardScreen.kt              (9c's) title/subtitle chain + dispatch for checking/fineTune; footer label/busy; haptic cue
feature/wizard/…/shell/WizardBackHandling.kt        (9c's) + the Checking BackHandler
feature/wizard/…/ui/WizardUiState.kt                (9c's) + checking, fineTune, cue fields; WizardChromeUi primaryLabel/primaryBusy
feature/wizard/…/TripWizardViewModel.kt             (9b's) + continueFrom branches, the new events, cue sequencing, override
feature/wizard/…/checking/WizardCheckingUi.kt       CheckLine, CheckLineState, CheckFailureKind, CheckingTitle, WizardCheckingUi.from
feature/wizard/…/checking/WizardCheckingStep.kt     stateless step body (replaces the seam for `checking`)
feature/wizard/…/checking/WizardCheckingArc.kt
feature/wizard/…/checking/WizardCheckLine.kt
feature/wizard/…/checking/WizardCheckFailure.kt
feature/wizard/…/finetune/WizardFineTuneUi.kt        WizardFineTuneUi, ExtraRowUi, AnswerText, WizardFineTuneUi.from
feature/wizard/…/finetune/WizardFineTuneStep.kt     stateless step body (replaces the seam for `fineTune`)
feature/wizard/…/finetune/WizardExtraRow.kt
feature/wizard/…/finetune/WizardPreparationCard.kt  prep stepper, sleep switch, override rows, errors
feature/wizard/…/finetune/WizardPreferencesCard.kt
feature/wizard/…/components/WizardStepper.kt
feature/wizard/…/answers/WizardAnswerSheetState.kt  (9e's) + options, kind side effects, rating text, submittedValues
feature/wizard/…/answers/WizardAnswerSheet.kt       (9e's) + per-kind dispatch; WizardPromptLabels widened
feature/wizard/…/answers/WizardAnswerOptions.kt
feature/wizard/…/answers/WizardWakeControl.kt
feature/wizard/…/answers/WizardRoomControl.kt
feature/wizard/…/answers/WizardBusyControl.kt       + the four-dialog chain
feature/wizard/…/answers/WizardDrinksControl.kt
feature/wizard/…/answers/WizardRatingControl.kt
feature/wizard/…/shell/WizardMetrics.kt             (9c's, already has sheetTitleMaxWidth 220, sheetRowMinHeight 52) + the 9f literals:
                                                    arcBoxWidth 200, arcBoxHeight 40, arcDash 4/6, stepperWidth 94,
                                                    stepperHeight 32, stepperRadius 9, removeCircle 32, ctaBusyAlpha 0.7,
                                                    errorRing 2 (prefer existing tokens: wizardFailureDashLength 3,
                                                    wizardEmptyOpacity 0.35 for disabled minus/Done)
feature/wizard/src/debug/…/debug/WizardCompletionBoardSeed.kt  + names registered in WizardBoardCaptureSeed (Checking boards too)
feature/wizard/src/debug/…/debug/FrozenCheckingClock.kt
app/src/debug/kotlin/net/chronoflow/debug/WizardDebugSeeds.kt  (9c's) + uiOverride and the frozen clock for 9f boards
```

Every dp literal in `:feature:wizard` comes from `PlanUITokens`/`AuthTokens` or an annotated `WizardMetrics` constant (Konsist `featureDpLiteralsComeFromBoardMetrics`).

## 7. Tasks

Each task is one commit (Conventional Commits, lower-case imperative, **no trailer**). Tick the plan box named in brackets in the same commit. Run `make targeted ONLY=…` for each task's tests and `make quick` once before the last commit. Every test name below states what it checks; every assertion must be able to fail for the real reason (break the code once and watch it fail before committing).

### Task 0: Server error copy on Times and Sleep (`fix(wizard): resolve times and sleep errors through the plan catalog`) [closes OQ-A399]
- Route `WizardFlightCard` (l.98–103), `WizardDateTimeRow` (l.136), the Times banner (`WizardTimesStep` l.89) and the Sleep error lines (`WizardSleepStep` l.106) through 9e's `fieldCopy(message, formatter)`; thread `copyFormatter` from `WizardStepContent` as an additive defaulted parameter. iOS: `MemberWizardView.swift` l.235, l.267; `MemberWizardSteps.swift` l.277.
- **JVM** (extend `WizardServerCopyTest`, the non-composable `fieldCopy(message, formatter, language, wizard)`): a plan-catalog key on a `legs.0.departLocal` error resolves from the catalog with its arguments; a wizard-only key falls back to the wizard table. No screen test (the resolver is the logic).
- Record OQ-A399 as closed in `plan.md` (default column) in this commit.

### Task 1: UI state and intents (`feat(wizard): map checking and fine-tune ui state`)
- §5.3 with the Stack-check adaptation: `WizardCheckingUi.from`/`WizardFineTuneUi.from` (pure), the `WizardUiState` fields, the new `WizardEvent`s, the `continueFrom` branches, the haptic cue, the `WizardUiOverride` seam, the Checking `secondaryClose` → `CloseFromFooter` rule.
- **JVM `WizardCheckingUiTest`** (pure mapper, no VM; `feature/wizard/src/test`): line order/state for `ticks` 0–3 (`ticks > i` done, `== i` busy); default vs `checkingAnswers` vs guest line 2; title key per `checkingTitle`; failure mapping `Offline`/`Throttled`/`Server(500)`/`Validation` → `Offline`/`Throttle`/`Server`/`Server`; throttle body = `LockoutWait` key + count for 42, 1, 61 (→ 2 minutes), 3 601 (→ 2 hours) and the "Please try again later." key at 0; button key "Try again in {{count}} s" + disabled while `retrySeconds > 0`; edit bodies for offline/server; `footerId == "checking"` only with a failure.
- **JVM `TripWizardViewModelCheckingTest`** (`withWizard`, `CheckingTestClock`, `runTest(timeout = 60.seconds)`; each < 1 s; the review is held with `source.configure(beforeReview = { delay(100) })`, or never answers with a `CompletableDeferred` that is never completed):
  - `linesFollowTicks`: from `Continue` on the last input step at t0: 0 ms → `[Busy, Open, Open]`; 399 → same; 400 → `[Done, Busy, Open]`; 800 → `[Done, Done, Busy]`; 1 199 → same; 1 200 → all `Done`; 1 499 → step still `checking`; 1 500 → step left `checking`.
  - `slowLineAtTenSeconds` (never answers): 9 999 → `slow == false`; 10 000 → `true`.
  - `serverFailureAtThirtySeconds`: 30 000 → `failure == Server`, `retrySeconds == 0`, `footerId == "checking"`.
  - `throttleCountsDownAndRetryWaits`: `reviewFailure = Throttled(41.2, 429)` → `failure == Throttle`, `retrySeconds == 42`; +1 000 ms → 41; `Continue(checking)` before 0 starts no review (`source.count("review")` unchanged); at 0 it starts exactly one.
  - `offlineFailureMapsToOfflineCard`; `validationFailureLeavesChecking` (a 422 `legs.0.toIata` → step `whereTo`, no failure).
  - `backCancelsCheckingAndReturnsToTheLastInputStep`: `Back` at 600 ms → step = `lastInputStep`; advancing to 2 000 ms changes nothing; no cue.
  - `hapticCuesOnCheckingEnd`: success → exactly one `Success` cue; `no_plan` → one `Warning`; offline → one `Error`; a 422 routed to `whereTo` → none (read the cue sequence from `uiState`).
  - `closeOnFailureAsksOnlyWithChanges`: `CloseFromFooter` with a failure → discard dialog for a member with changes, close for a guest.
  - `uiOverrideFreezesCheckingWithoutARequest`: `uiOverride = WizardUiOverride(WizardCheckingProgress(2, slow = true))` on a checking draft → lines `[Done, Done, Busy]`, `slow`, `source.count("review") == 0`.
- **JVM `TripWizardViewModelFineTuneTest`** (same setup; drafts from `checkedDraft(fineTune)` plus hand-built prompts from Task 4's seed helpers):
  - `rowsKeepPromptOrderAndLabels`: FineTune draft → kinds `[room, latest_wake, seat_side, late_check_out, pre_departure_busy_hours, cups]`, `required` `[true, true, false, false, false, false]`, answers `["When I land", "08:30", "Window left", "No", "{{count}} time ranges"(0), "{{count}} a day"(2)]`.
  - `multiFlightRowsCarryRouteAndCity`: Multi → label keys + suffixes " · Dubai", " · Tokyo", " · AMS → DXB", " · DXB → NRT", "Late check-out" + " · Dubai"; single leg → no suffix.
  - `noneStateHasNoRows`: `none`, rows empty, primary label key "Done".
  - `preparationDaysClampAndPersist`: 3 → +1 → 4 → +1 stays 4 (no write) → −5 clamps 0.
  - `enablingSleepOverridePrefillsFromProfileThenSaved`: profile bed 23:30 → 23:30; profile empty, `savedProfile.habitualBed = "22:45"` → 22:45; an existing value is kept; disabling keeps values; guest → no switch.
  - `overrideTimeKeepsMinutePrecision`: confirm 07:03 touched → `"07:03"`; untouched Done → no write.
  - `preferenceIgnoresRetiredKeys` (`SetPreference("pregnancy", true)` changes nothing).
  - `unchangedFineTuneGoesStraightToReady` / `changedFineTuneReviewsInPlace` (`reviewing` → primary busy while the review is held, then step `ready`, `source.count("review") == 1`).
  - `fineTuneErrorsAreSortedOverrideAndPreparation`: `preparation.latestWake`, `override.habitualBed`, `legs.0.toIata` → only the first two, sorted.
  - `uiOverrideReviewingShowsBusy`.
- Ticks nothing yet.

### Task 2: Answer-sheet state (`feat(wizard): add the fine-tune answer kinds`)
- §5.7 on 9e's `WizardAnswerSheetState`.
- **JVM `WizardAnswerSheetStateTest`** (extend 9e's class in `feature/wizard/src/test`; prompts built by hand with `fields` and `shape` as `06 §7.1`; each < 1 s):
  - `optionsMatchIOS` (one assertion block per kind: values, label keys, hint keys).
  - `unansweredSeatSelectsAisle`; `profileFieldsFallBackToSavedProfile` (`light_device` → "box").
  - `ratingNumberLoadsWithLanguageSeparator`: 1.5 → "1.5" (en), "1,5" (nl); 12 → "12".
  - `noneResetsAllDrinks`; `stepSetsCupsHabit` (cap at `shape.max` 20).
  - `doneRejectsNoDrinks` → error key "Add at least one drink to save this answer.", `failures + 1`; `anyUpdateClearsTheError`.
  - `doneRejectsRatingOutOfRange` for "12", "0", "", "abc", "9,95"; accepts "1,5" → submitted `"1.5"`; accepts "0.1", "9.9".
  - `roomDeclaredNeedsATime`: `missingTimes` until `setRoomTime("15:00")` with landing `2026-10-13T09:15` → `"2026-10-13T15:00"`.
  - `latestWakeNeedsHHmm` (`""`, `"8:30"` missing; `"08:30"` ok).
  - `busyPutAndRemoveMarkAnswered`.
  - `fieldErrorPreloads` (`stays.0.bedAvailableFromLocal`) and `initialErrorPreloads`.
- **JVM `TripWizardViewModelAnswerSheetTest`** (extend 9e's): every kind opens from `OpenPrompt`; `UpdateAnswer` for a foreign field is ignored; Done on BlueError/DrinksError → one more `failures`, no write; Seat "Aisle or not sure" → Done → `seatWindow == null` and the row answer is null ("Add"); `CancelAnswer` writes nothing.
- Ticks nothing.

### Task 3: Inline time picker (`feat(designsystem): add the inline time picker`)
- `CfTimePicker(state: TimePickerState, modifier, compact: Boolean = false)`: `TimePicker` (or `TimeInput` when `compact`) with the exact colours of `CfTimePickerDialog` (`CfPickers.kt`). Add it to the `designSystemPrimitivesHaveGuards` coverage.
- **Robolectric `CfTimePickerGuardsTest` (`@Layout`, `core/ui/src/test/…/designsystem/`)**, beside `CfTimePickerSheetGuardsTest`: `compact` renders the `TimeInput` structure (an editable hour node), the dial otherwise; guards on `small`, `pixel8`, `largeFont`, `landscapeSideNav` (4 cases).

### Task 4: Board seeds (`test(wizard): seed the checking and fine-tune boards`)
- §5.10: `WizardCompletionBoardSeed`, the Checking boards, `FrozenCheckingClock`, `WizardUiOverride` wiring in `WizardDebugSeeds` and `boardState()`, the `ReleaseReadinessTest` extension.
- **JVM `WizardCompletionBoardSeedTest`** (`feature/wizard/src/testDebug`, parameterised one case per base name, 22 cases, plus `WizardSheetFreeSleep`): step, leg count, `returnBoundary`, `checkingAnswers`, `promptSheetID`'s kind, prompt kinds in order, `required` flags, `override`, `stays[0]`, busy intervals, `seatWindow`, savedProfile numbers, `progressFloor` (0.95 only for FineTuneNone) with the resulting model `progress` (Checking/Check* 0.60, CheckingAgain 0.70, FineTune* and sheets 0.80, FineTuneNone 0.95), and the `WizardUiOverride` values; `Dark`/`NL`/`DarkNL` resolve to the same draft. One case compares the hand-built prompts with Kotlin `MockWizardPrompts.make` (testImplementation only).
- Extend 9c's `WizardBoardCaptureSeedTest` for the new names.

### Task 5: Checking screen (`feat(wizard): build the checking screen`) [ticks the first, third and fourth bullets of "Checking"]
- §5.1: the step body, arc, lines, slow line, the `WizardBackHandling` entry, the cue player in `WizardScreen`.
- **Robolectric `WizardCheckingStepTest` (`@Logic`)**, through `WizardScreen(boardState(…), formatter, record)` only, exact tags, no text matchers:
  - structure: three `wizard.checking.line.<i>`; `wizard.checking.slow` only on the Slow board; no `wizard.checking.footer` and no `wizard.back` without a failure; `wizard.subtitle` absent with a failure.
  - `linesAnnouncePolitely`: each line has `LiveRegion == Polite` and a defined `ContentDescription` (the label copy itself is asserted on `WizardCheckingUi` in Task 1).
  - `reduceMotionShowsAStaticDot`: with `reduceMotion = true` the busy line has `wizard.checking.line.2.dot` and no `.spinner`; otherwise the reverse.
  - `arcAndCodesAreHidden`: the arc row node exposes no semantics children (`clearAndSetSemantics`).
  - `systemBackEmitsBack`: `Espresso.pressBack()` on the Checking board records exactly `[WizardEvent.Back]` (the VM half is Task 1's `backCancels…`).
- **Robolectric `WizardCheckingRecompositionTest` (`@Logic`)**, counting with a `SideEffect` per item as 9d's `WizardTimesRecompositionTest`: `mainClock.advanceTimeBy(2_000)` recomposes the arc 0 times; changing `ticks` 1 → 2 recomposes the arc 0 times and only the changed lines.
- Tick "Checking" bullets: "A canvas arc …", "No back while checking", "TalkBack hears progress …" (the failure bullet in Task 6).

### Task 6: Failure cards (`feat(wizard): add the checking failure cards`) [ticks "Checking"]
- §5.2 and the glyphs (`tools/icons/lucide.json` + `make vectors`, generated output in the same commit).
- **Robolectric `WizardCheckFailureTest` (`@Logic`)**, from the three failure boards (+ an `editing` variant): card tag `wizard.checking.<kind>`, glyph node `wizard.checking.<kind>.icon` cleared from semantics, `LiveRegion == Polite` on the card, `wizard.checking.secondary-close` and `wizard.checking.continue` present, `wizard.back` present (non-edit); on Throttle `continue` `assertIsNotEnabled()`, enabled otherwise; tapping Close records `CloseFromFooter`, tapping Try again records `Continue(checking)`; no haptic on either tap (`RecordingHaptics` empty).

### Task 7: Fine-tune screen (`feat(wizard): build the fine-tune screen`) [ticks "Fine-tune"]
- §5.4, §5.5, `WizardStepper`, the three cards, the footer states.
- **Robolectric `WizardFineTuneStepTest` (`@Logic`)** (ports `assertFineTune` structurally: `wizard.fine-tune.continue` exists, enabled except Busy, `wizard.extras` absent for None):
  - row tags `wizard.extra.<kind>.<i>` in prompt order; the required-star node `wizard.extra.<kind>.<i>.star` only on required rows; the "Add" value carries `dawnText` (`GetTextLayoutResult` colour, not its text);
  - Busy: `continue` `assertIsNotEnabled()` with `ProgressBarRangeInfo` of the spinner present; rows `assertIsNotEnabled()`;
  - taps record `OpenPrompt(id)`, `SetPreparationDays(4)` (+ one `Selection` in `RecordingHaptics`), `SetSleepOverride(true)` (+ `ToggleOn`), `SetPreference("exclude_caffeine", true)`, `OpenTimeSheet(<override field>)`; plus at 4 is disabled; `performSemanticsAction(SetProgress) { it(1f) }` on `wizard.preparation` records `SetPreparationDays(1)`;
  - Sleep board: `wizard.override.bed`/`.wake` exist; guest state: no `wizard.sleep-override`;
  - `wizard.fine-tune.error.override.habitualBed` exists for that error.
- **Robolectric `WizardFineTuneLayoutTest` (`@Layout`)**: at 390 × 844 dp en (font 1.0) the FineTune board's cards 1–2 end above the footer top with no scroll; on `small` the content scrolls and `wizard.fine-tune.continue` stays inside the window.

### Task 8: Answer sheets (`feat(wizard): add the fine-tune answer sheets`) [ticks "Sheets"]
- §5.6, §5.8.
- **Robolectric `WizardAnswerSheetTest` (`@Logic`)** (extend 9e's; ports `assertAnswerSheet` structurally: cancel and done exist; on `*Error` boards done disabled and `wizard.answer.error` exists), one test per kind from its board, no text matchers:
  - option nodes `wizard.answer.option.<i>` with `Role.RadioButton` inside a selectable group; the seeded selection `assertIsSelected()`; a tap records `UpdateAnswer(field, value)` and one `Selection`;
  - Room: `wizard.answer.room-time` exists only while `declared`; Wake: `wizard.answer.latest_wake` exists; Busy: two remove targets `wizard.answer.remove-busy.<i>` ≥ 48 dp and `wizard.answer.add-busy`; the dialog chain opens `wizard.date-dialog` then `wizard.time-dialog` in order and To ≤ From keeps the confirm disabled; Drinks: `wizard.answer.cups_filter_coffee.minus` enabled, `wizard.answer.cups_tea.minus` disabled, the ring node `wizard.answer.drinks.ring` only on DrinksError; Light: `wizard.answer.lightNotice` exists; Blue: `wizard.answer.rating` exists, ring only on BlueError;
  - `dismissIsCancel`: `Espresso.pressBack()` with the Seat sheet records `CancelAnswer` and no haptic;
  - `ratingSurvivesRestoration` (port of `testDuoResizeKeepsFineTuneAndOpenAnswer` at the layer that can fail): `StateRestorationTester` with the Blue sheet, type "2" → after `emulateSavedInstanceStateRestore()` the field still holds the state's value and `UpdateAnswer("blue_blocker_mdfd", "2")` was recorded once.

### Task 9: Haptics (`feat(wizard): fire fine-tune and checking haptics`) [ticks "Haptics"]
- If any §5.9 row is not yet wired by Tasks 5–8, wire it here; otherwise this commit only adds the test.
- **Robolectric `WizardHapticsTest` (`@Logic`, `RecordingHaptics`)** on `WizardScreen` with hand-set states: a new cue sequence plays exactly one event (`Success`, `Warning`, `Error`); re-composition and state restoration with the same sequence play nothing; Cancel plays nothing. The Selection/Toggle rows are already covered in Tasks 7–8.

### Task 10: Guards and captures (`test(wizard): guard and capture the checking and fine-tune boards`) [ticks "Tests"]
- **`WizardCheckingFineTuneGuardsTest` (`@Layout`, parameterised, one case per parameter set)**, shaped like 9e's `WizardSleepQuestionGuardsTest`: `WizardTestHost(case.profile) { WizardScreen(boardState(…), formatter, {}) }`, then `Guards.assertTextFits`, `assertTouchTargets`, `assertInteractiveInSafeArea(rootTag = "wizard.window")`, `assertNoInteractiveOverlap`, `assertHeadings("wizard.title" or "wizard.answer.title")`.
  - plan.md's "guards for all 26 boards" is a miscount: the phase's board list has 22 base boards (46 wrappers); `WizardSheet` and `WizardFineTuneSheet` render the same seat sheet over Fine-tune, so they share the `sheetSeat` state. Say so in the PR when ticking "Tests".
  - States (21): `checking`, `checkingAgain`, `checkingSlow`, `checkOffline`, `checkServer`, `checkThrottle`, `fineTune`, `fineTuneMulti`, `fineTuneSleep`, `fineTuneBusy`, `fineTuneNone`, `sheetSeat`, `sheetRoom`, `sheetCheckout`, `sheetWake`, `sheetBusy`, `sheetDrinks`, `sheetDrinksError`, `sheetLight`, `sheetBlue`, `sheetBlueError`.
  - Matrix (light only): every state × nl × `small`, `pixel8`, `foldInner`, `largeFont` = 84; every state × en × `small` = 21; `fineTune` and `sheetSeat` × nl × `landscapeSideNav` = 2. **107 cases** (`check(it.size == 107)`), warm cases < 1 s; record the class run time in the PR.
  - On `small` + nl and `largeFont` + nl: `fineTuneSleep` scrolls and `wizard.fine-tune.continue` stays inside the window; `sheetBusy` and `sheetDrinks` scroll inside the sheet and `wizard.answer.done` stays visible (inside the same cases, no extra parameters).
- **Captures**: register the names so 9e's `WizardBoardCaptureTest` captures them (one PNG per wrapper, file name = board name, `reduceMotion = true` so the dot sits at t = 0.65 and the busy line shows the static dot; record that the board draws a spinner there): the 46 board wrappers of plan.md plus `WizardSheetFreeSleep` and `WizardSheetFreeSleepDarkNL` re-captured over the real Fine-tune (OQ-A412) = **48 PNGs**. Account time format `Value24`. No stored Roborazzi references.

### Task 11: Device check (`test(app): check the rating field and the inline picker on device`)
- **`WizardAnswerSheetDeviceTest` (`@Smoke`, `app/src/androidTest/kotlin/net/chronoflow/app/wizard/`, extends 0e `ScreenTest`, each < 10 s, `timeout_msec=60000`, `waitUntilExactlyOneExists`, no sleeps)**, seeded with `wizardPickerSeed(language, board = …)`:
  - `ratingFieldAndDoneStayAboveTheKeyboard` (`WizardSheetBlue`, en): tap `wizard.answer.rating`; `assertWizardKeyboardGeometry(focusedWizardWindow())`-style check that the field's and `wizard.answer.done`'s bounds sit above the IME top. Nothing else (validation is Task 2's JVM test).
  - `inlineWakePickerFollowsThePhoneHourCycle` (`WizardSheetWake`, nl, `withWizardPhoneHourCycle("12")`): the inline picker exposes 9d's `PickerIs24Hour == false` and `PickerLanguage == "nl"` semantics.
- If the sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:WizardAnswerSheetDeviceTest"`) and their output; the orchestrator runs them.

### Task 12: Docs (`docs(wizard): record checking and fine-tune acceptance`)
- Extend `docs/wizard.md` with a "Phase 9f" section: the checking line/tick table, the failure mapping, the fine-tune card order (and why card 3 is last), the sheet kinds table, the seed table, the capture list (48), the guard scope (107), the comparison hand-off for the orchestrator (expected differences, see §8.5), and the Swift → Kotlin test-name map (`assertFineTune` → `WizardFineTuneStepTest`, `assertAnswerSheet` → `WizardAnswerSheetTest`, `testCheckOfflineScreenKeepsEditingAndGoesBack` → `closeOnFailureAsksOnlyWithChanges` + `backCancelsCheckingAndReturnsToTheLastInputStep`, `testDuoResizeKeepsFineTuneAndOpenAnswer` → `ratingSurvivesRestoration`).
- Append OQ-9f-1 … OQ-9f-19 to `plan.md` "Open questions for the user" as **OQ-A420 onward**.

## 8. Acceptance criteria

1. `make quick` passes (< 60 s warm). `make check` passes on the phase branch with `pixel` booted (the orchestrator runs it if the sandbox cannot).
2. `make targeted ONLY="feature:wizard:WizardServerCopyTest feature:wizard:WizardCheckingUiTest feature:wizard:WizardAnswerSheetStateTest feature:wizard:TripWizardViewModelCheckingTest feature:wizard:TripWizardViewModelFineTuneTest feature:wizard:TripWizardViewModelAnswerSheetTest feature:wizard:WizardCompletionBoardSeedTest"` is green, every JVM test < 1 s (`build/cf-timings/…`). 9a's `WizardCheckingTests` and 9b's checking/retry suites are still green unchanged.
3. **Checking timing matches `06 §3.5`**: `linesFollowTicks`, `slowLineAtTenSeconds`, `serverFailureAtThirtySeconds` and `throttleCountsDownAndRetryWaits` pass on the fake clock with the exact millisecond boundaries above.
4. All Robolectric classes of Tasks 3 and 5–10 pass, warm cases < 1 s (first case per class recorded separately); the guard class has 107 cases; no test asserts copy on screen (`rg -n "onNodeWithText|hasText\(|assertTextEquals|assertContentDescriptionEquals" feature/wizard/src/test feature/wizard/src/testDebug` finds nothing new).
5. `make captures ONLY=WizardChecking,WizardCheck,WizardFineTune,WizardSheet` writes the 48 PNGs of Task 10 (file name = board name). The orchestrator renders the boards on the host at 390 × 844 (light/dark, en/nl) and compares; every difference is fixed or recorded in `docs/wizard.md`. Expected differences only: §4, the M3 sheet drag handle and header buttons, the M3 switch, the M3 inline time picker instead of the wheel, the M3 time sheet for override and room times, the static busy dot under Reduce Motion, card 3 below the fold, the 16 dp CfButton spinner on FineTuneBusy (board 18). Progress must equal each board's `pct` (9a Task 8 values; FineTuneNone 95 through its seed floor).
6. On `pixel`: `WizardAnswerSheetDeviceTest` passes (< 10 s); a debug run through Checking → Fine-tune → a sheet is StrictMode-clean.
7. Performance: `WizardCheckingRecompositionTest` passes (the arc animates by draw only; a tick recomposes only the line list); `rg -n "onSizeChanged|onGloballyPositioned" feature/wizard/src/main` finds nothing new; `rg -n "PathMeasure|Path\(\)" feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/checking` finds the path only inside `drawWithCache`. No perf-budget row applies to the wizard.
8. `rg -n "Thread.sleep|SystemClock.sleep|delay\(" feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/{checking,finetune,answers}` finds nothing (all timing is 9a's clock); `rg -n "String.format|NumberFormat|DecimalFormat|Locale.getDefault|announceForAccessibility" core/wizard/src/main feature/wizard/src/main` finds nothing new; Konsist passes.
9. `app/src/testRelease/…/ReleaseReadinessTest` (release): `uiOverride` null, no `WizardBoardCaptureSeed`/`WizardCompletionBoardSeed`/`FrozenCheckingClock`, no `net.chronoflow.core.datamock` class; Konsist `dataMockOnlyInDebugAndPerf` passes (no data-mock dependency in `:feature:wizard` outside tests).
10. `git -C /Users/rickgout/Sites/chronoflow-ios status --porcelain --untracked-files=no`, and the same for `…/chronoflow-new` and `…/chronoflow-mobile`, print nothing (no tracked change; untracked files that predate the phase are not this phase's and must be left alone). Every commit is Conventional Commits with no trailer; the plan boxes "Checking", "Fine-tune", "Sheets", "Haptics" and "Tests" are ticked; the new OQs are in `plan.md`.

## 9. Review checklist (Opus reviewer)

- [ ] **Timing is the model's.** The UI reads `snapshot.checking` and `retrySeconds` only; no `delay`, `LaunchedEffect` timer or clock read in `:feature:wizard` for Checking or the countdown. Line state = `ticks > i` / `== i` / else.
- [ ] **Again ordering** and guest line 2 exactly as `checkingLines`; the title comes from `checkingTitle`.
- [ ] **Back on Checking**: no chevron without a failure, a non-predictive `BackHandler` that cancels the check (no late transition, no haptic), chevron present on a non-edit failure.
- [ ] **Failure mapping**: Offline → offline, Throttled → throttle, everything else → server; `LockoutWait` rounding up and its seconds/minutes/hours keys; "Try again in N s" disabled until 0; Close goes through 9c's discard rule.
- [ ] **Accessibility**: per-line polite live regions with "…, done"/"…, in progress"; arc and icons cleared; slow line and failure card polite; no `announceForAccessibility`; steppers adjustable with `setProgress`; sheet options are radio buttons in a selectable group; the sheet title is a heading.
- [ ] **Reduce Motion**: no infinite transition is created; dot at t = 0.65; static busy dot; timing unchanged.
- [ ] **Arc perf**: path and dash effect built in `drawWithCache`; the animated value read only in draw; no allocation per frame (no `Path()`/`PathEffect`/`Offset` lists in `onDrawBehind` besides primitives).
- [ ] **Fine-tune**: row order = `optionalPrompts`; suffix rules (seat route, room "Dark room · city", check-out city) only with > 1 leg; "Add" in `dawnText`; rows and cards disabled (semantics) while reviewing with no visual change; button states Continue/Done/Checking… (alpha 0.7); the switch prefill order profile → savedProfile; preparation clamped 0–4; override times at minute precision.
- [ ] **Shell pattern**: step UI as `WizardUiState` fields, intents as `WizardEvent`s, the Checking BackHandler in `WizardBackHandling`, footer through `WizardChromeUi`; OQ-A399 closed (Times and Sleep errors through `fieldCopy`).
- [ ] **Sheets**: only Done writes; Cancel/drag/scrim/back write nothing; any edit clears the error; invalid Done → error + Warning + Done disabled; rating accepts "," and is sent with "."; None resets all four drinks; a stepper step sets `caffeine_habit = cups`; "Aisle or not sure" writes JSON `null`; room time = landing date + time; busy To ≤ From cannot be confirmed; the sheet reopens after recreation with edits and after process death from `promptSheetID`.
- [ ] **No new copy**; every string resolves through `names.lock.json` or PlanCopy; no English literal in composables; numbers formatted by hand.
- [ ] **Geometry**: only tokens and annotated `WizardMetrics`; 48 dp targets on stepper halves, remove buttons, Close, header buttons; works at 280 dp and font scale 1.3 (guards).
- [ ] **Seeds** are debug-only, port the Swift seed values exactly (Multi 23:25/02:05/16:35, checkout return 10:25/15:30, busy intervals, savedProfile), and the frozen clock prevents any request in captures.
- [ ] Typical mistakes: reading `infiniteTransition.value` in composition; building the stepper with M3 `IconButton`s that grow the drawn 32 dp group; a rating `TextFieldState` not keyed on the prompt id, so a different prompt reuses the old text; a haptic cue replayed after recreation; a test asserting copy on screen or booting `WizardRoot` with a live VM; `catch (e: Exception)` around `answerOptional` swallowing `CancellationException`; formatting the rating with `String.format`/`NumberFormat`; firing Selection on sheet open; a ripple on designed rows that the board does not draw (use `cfClickable`'s press style); deriving the failure kind from HTTP codes in the UI instead of the `DataFailure` type; calling `resumeChecking()` when `uiOverride` is set; modifying 9a model semantics.

## 10. Open questions (proceed on the default; append each to `plan.md` as OQ-A420 onward)

| # | Question | Default |
|---|---|---|
| OQ-9f-1 | Where do the two preference toggles (exclude caffeine, movement breaks) go? iOS puts them first; no board draws them. | A third card after the preparation card, so the drawn cards match the boards; same copy and behaviour as iOS. |
| OQ-9f-2 | The light-device notice has no board (iOS NH5 adds it). | Build it under the question (user design exception, `I/plan.md:2484`). |
| OQ-9f-3 | Override bed/wake precision: OQ-A8's 5-minute rounding (9e) or iOS `WizardTimeControl` minute precision? | Minute precision (iOS); OQ-A8/OQ-A403 name 9e and 10b only. |
| OQ-9f-4 | Fine-tune row values and busy ranges show raw `HH:mm` (iOS); pills follow the account time format (9d). | Keep both as iOS/9d: row values and range texts raw 24 h, pills via `WizardDateFormat.time`. |
| OQ-9f-5 | Busy range picking: iOS uses two `PickerDateTime` sheets; Android has separate date and time dialogs. | Four-dialog chain From date → From time → To date → To time; To time Done disabled while ≤ From; no error state (no board). |
| OQ-9f-6 | Latest-wake inline picker: dial or `TimeInput`? | Dial; `TimeInput` when the window height is < 640 dp. |
| OQ-9f-7 | Stepper accessibility without new copy. | One adjustable node (`setProgress`, `stateDescription`); the drawn halves are touch-only. Recommend Android-only copy "Decrease"/"Increase" later if TalkBack users need buttons. |
| OQ-9f-8 | Board-only colours `rgba(120,120,128,0.3/0.7)` have no token. | `border` for the stepper divider, `mutedForeground` for chevrons (iOS). |
| OQ-9f-9 | "Helps a lot" weight: board 600, iOS `screenMeta` 400. | `screenMeta` + SemiBold (shipped Figtree 600; 9e weight-copy precedent). |
| OQ-9f-10 | Sheet question style 17/24 600 has no token. | 9e's `wizardRow` + SemiBold (17/22, Figtree 600). |
| OQ-9f-11 | Arc: board cubic vs iOS quadratic with 8 dp inset. | Board cubic scaled as the SVG renders it (uniform `meet` scale, centred); iOS animation phase (3.6 s, Reduce Motion t = 0.65). |
| OQ-9f-12 | Switch haptic: WizardSpec says Selection; the plan's Material mapping says `ToggleOn`/`ToggleOff`. | Plan mapping through `cfToggleable`. |
| OQ-9f-13 | Sheet detents: iOS medium/large; Phase 2 skips the partial state. | Phase 2 default: full content height, scrollable. |
| OQ-9f-14 | Rating text in nl: iOS seed stores "1.5" as a string, the board shows "1,5". | Load numbers with the app-language separator; seeds store numbers. |
| OQ-9f-15 | Busy line under Reduce Motion: Phase 2 keeps `CfSpinner` spinning; WizardSpec and iOS show a static dot. | Static dot (WizardSpec, iOS). |
| OQ-9f-16 | The debug freeze: iOS adds `#if DEBUG` methods to the model; Kotlin has no conditional compilation. | A neutral `WizardUiOverride` seam in the ViewModel plus a frozen checking clock, both set only from `src/debug`; the model is unchanged. |
| OQ-9f-17 | `WizardFineTuneNone` draws 95 %, but the model gives a fresh Fine-tune 0.80 and reaches 0.95 only through `progressFloor` (opened from Ready); the iOS seed sets no floor. | The None seed sets `progressFloor = 0.95`, so the capture matches the board and shows what a user sees (a prompt-free Fine-tune is only reachable from Ready). |
| OQ-9f-18 | Override and room times: Phase 2 dialog (brief draft) or 9e's PickerTime sheet (`CfTimePickerSheet`, OQ-A413/OQ-A416)? | 9e's sheet, at minute precision, without the "Steps of 5 minutes." footnote; busy-hour ranges keep 9d's date and time dialogs. |
| OQ-9f-19 | The iOS seed calls `MockWizardPrompts.make`; Konsist forbids `:core:data-mock` in `:feature:wizard` main/debug. | Hand-built prompts in `WizardCompletionBoardSeed` (9e precedent), cross-checked against Kotlin `MockWizardPrompts.make` in a JVM test. |

Review round 1: toolbar `RequestClose` follows iOS’s guest-excluded exception only; checking-failure secondary `CloseFromFooter` closes guests directly. These are separate intents. The guest toolbar discard body uses the existing welcome-screen catalog key.
