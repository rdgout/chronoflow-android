# Phase 9h: Edit and duplicate

- **Branch:** `feat/phase-9h-wizard-edit-duplicate`. Start it from the top of the stack once **9g** (Ready, save, Calculating, `WizardExit.Saved`) and **6d** (trip actions sheet, `TripActionsAvailability`, `TripAction`) are both in it. Through them the stack also carries 9f (Checking, failure cards, Fine-tune, `WizardUiOverride`), 9e (Excluded with the edit note), 9d (Times, Coming back?, `WizardChoiceCard`, `WizardBanner`), 9c (shell, discard alert, `WizardLauncher`, `WizardRecoveryCheck`, `WizardBoardCaptureSeed`), 9b (`TripWizardViewModel`, `MemberWizardStore.startTrip`, generation service with edit refresh and `PUT`), 9a (model, `TripWizardActions.kt`, `loadTrip`, `clearDuplicateDates`, `acceptSourceReload`, `SourceChange`, `sourceChangeSummary`), 6a–6c (shell, `ShellNav.wizardSaved`, trip page), 4c (mock with `tripInput`, `reviewTripEdit`, `updateTrip`), 2, 0c, 0e. If one is missing, stop and report it.
- **Depends on:** 9g, 6d. **Wave:** 14. **Plan entry:** `plan.md` "### Phase 9h: Edit and duplicate" (l.2966–2996). Also read in `plan.md`: A14 (l.101, no billing), A24 (l.111), OQ-A18 (l.615), OQ-A36 (l.633), "Android spec: state survival" (Wizard row l.379), "Android spec: navigation and routes" (`Wizard(mode: Create | Edit(tripId) | Duplicate(tripId))`, l.402), "Android spec: seeds and test hooks" (l.466–477; mock trips `…0001` AMS→HND active, `…0002` AMS→JFK upcoming, l.476), "Android-only copy awaiting approval" (l.487), OQ-A5 (l.602, toast = Snackbar).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. Checked against the 6d, 9a, 9b, 9c, 9d, 9e, 9f and 9g briefs in `docs/phases/`. **Before you code, open the merged code of those phases and use their names.** Where a name here differs from the merged one, keep the merged one and note it in the PR. Never rename earlier code; additive parameters with defaults are allowed.
- **Checked 2026-11-16** (adversarial pass) against iOS `f391a1fa`, web `e36368ec`, the boards and the earlier briefs. Corrected: the plan line refs (entry l.2359–2388, boxes l.2375/2380/2381), the iOS availability re-check line (`TripPageDestination.swift:205–210`), edit progress after a revisit (`go()` clears the floor, so `WizardReturnWeb` shows 0.45 = board; OQ-9h-10 resolved, seed without a floor), Task 9's mock assertions (`MockDataSource` has no call recorder; the ported tests use iOS's trip `…0001`), the layout-test scope of the no-credits check, the seed test's `hasChanges` expectations (the Checking/Excluded edit seeds are unchanged edits). Everything else (copy en/nl, seeds, routes, board props, line refs) was verified as written.
- **Tests:** every test follows `AGENTS.md` "Tests": the cheapest tier, the budgets (JVM and warm Robolectric < 1 s per case, one-off cold costs warmed once in `@BeforeClass` through `measureClassWarmup`; emulator < 10 s; refreshed 2026-11-20 — this replaces every "< 5 s" below), exact tag queries, waits with `waitUntil`, never `Thread.sleep`, hang caps (`runTest(timeout = 60.seconds)`, `timeout_msec=60000`). Tier annotations are 0e's: Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`.
- **Tags:** **[F]** fact read in source (path and line given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`. Board coordinates are CSS px and map 1:1 to dp.
- **Paths:** `…/cw/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`; `…/fw/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`; `…/fwd/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`; `…/fwt/` = `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/`; `…/fwtd/` = `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`; `…/ft/` = `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/`; `…/ftt/` = `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/apptd/` = `app/src/testDebug/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-20, against 2e1c86fb)

These notes **override the body**. They were checked with `git grep` and file reads on `feat/phase-9h-wizard-edit-duplicate` at `2e1c86fb`. Repeat these checks before Task 1, and stop and report on any mismatch.

**R1. Stack.** 9a–9g and 6d are all in the stack (6d: `d3fdac32 feat(trip): add the trip actions sheet`). No dependency is missing.

**R2. Already built by 9a–9g/6d (verify; do not rebuild):**
- **Edit/Duplicate load in the VM.** `TripWizardViewModel.init` (`…/fw/TripWizardViewModel.kt:299–436`) already calls `deps.startTrip(entry.tripId, TripWizardAction.edit|duplicate)` for `WizardEntry.Edit/Duplicate` (`WizardEntry` = `core.navigation.WizardMode`, aliased because `core.wizard.model.WizardMode` is guest/member). Task 1 changes only that branch.
- **Quiet close on a failed load (OQ-9h-4).** `loadFailed()` (l.440–447) sets `LoadFailed`, logs a `java.util.logging` warning ("Wizard could not start") and calls `exitOnce()` → `WizardExit.Trips`, for every mode. Task 1 only adds the Edit/Duplicate test.
- **Nav title.** `WizardChromeUi.from` uses `NavTitle.EditTrip` when `state.edit != null`, else `NewTrip` (`…/fw/ui/WizardUiState.kt:315`). Duplicate therefore shows "New trip".
- **Back chevron.** The VM replaces `chrome.showBack` with the model's `showsBack`. `MemberWizardModel.firstScreen` (`core/wizard/src/main/kotlin/net/chronoflow/core/wizard/model/MemberWizardNavigation.kt:117–124`) covers Duplicate's first Times, edit Ready and edit Checking failure. `popNavigation` refuses on first screens (l.104).
- **Discard alert copy per mode (§5.3 alert table).** `WizardUiState.from` sets `discardTitle`, `discardConfirmLabel` and `discardMessage` from `isEditing` (`WizardUiState.kt:542–557`). Dismiss is "Keep editing" (`…/fw/shell/WizardBackHandling.kt:39`). Nothing to build.
- **Close rules (§5.3).**
  - `requestClose()` (`TripWizardViewModel.kt:587`): `hasChanges` → dialog; otherwise close.
  - `closeFromFooter()` (l.599): excluded, guest or `!hasChanges` → close; otherwise dialog.
  - The no-credits Close sends `WizardEvent.CloseFromFooter` (`…/fw/ready/ReadyFooter.kt:23`).
- **Edit Checking and Excluded.** `WizardCheckingUi` holds the `editing` flag, the edit failure bodies and "Checking your changes" (`…/fw/checking/WizardCheckingUi.kt:47, 68–69, 87–88, 127`). The edit Excluded note is `ExcludedNote.Edit` (`WizardUiState.kt:589`, `…/fw/excluded/WizardExcludedUi.kt:18`).
- **Ready.** `ReadyUiMapper.map` already gates `enabled` on `(!active.isEditing || active.hasChanges)`. Its Return row already falls back to `state.snapshotStayEnd`.
- **Tags and seams.**
  - `wizard.subtitle` already exists (`…/fw/shell/WizardScreen.kt:563`); do not add it.
  - The ready save haptic is `HapticEvent.Medium` on the Ready primary (`WizardScreen.kt:369`); "Update my plan" needs nothing new.
- **Store.** `MemberWizardStore` is in `:core:data`, not `:core:wizard` (`core/data/src/main/kotlin/net/chronoflow/core/data/wizard/MemberWizardStore.kt`). It already has `recover()` (l.42) and `startTrip(id, action, source)` (l.65), and both switch to the store's own `io` dispatcher.
- **Mock.** `MockDataSource` has `tripInput`, `reviewTripEdit` and `updateTrip` (`core/data-mock/src/main/kotlin/net/chronoflow/core/datamock/MockDataSource.kt:1230, 1244, 1381`), plus `failNext(method, error)` (l.111) with `MockMethod.TripInput`. In the mock seed (`core/testing/src/main/resources/fixtures/mock-seed.json`), `…0001` (AMS→HND) and `…0002` (AMS→JFK) both have `canEdit = canDuplicate = true`. All 7 seeded trips do. `tripInput` builds a snapshot from the stored detail.
- **Strings.** Every §5.8 key resolves in `tools/l10n/names.lock.json`. "Ends after {{count}} nights" is a plain string (`wizard_ends_after_count_nights`). "{{count}} credits", "{{count}} nights" and "You have {{count}} credits" are plurals.
- **9b tests.**
  - `TripWizardViewModelTest.editAndDuplicateLoadTripInput` (l.82) and `editLoadFailureIsExposed` (l.104) exist.
  - `TripWizardActionsTests` = 1 case in `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/model/` plus 4 in `core/data/src/test/kotlin/net/chronoflow/core/data/wizard/`.

**R3. Merged names and shapes that differ from the body (use these):**
- **`ShellHolder` does not exist.** It is `ShellViewModel` (`app/src/main/kotlin/net/chronoflow/app/shell/ShellViewModel.kt`: `back()` l.71, `closeWizard(exit)` l.77 (`Saved` → `wizardSaved`, `Trips` → `back()`), `wizardSaved`), held as `holder` in `AppShell`.
- **Overlay back (§5.2).** There is no `BackHandler(enabled = overlay != null && !(… Create))` in `AppShell`. Instead:
  - `AppShell.kt:59` passes `overlayOwnsBack = overlay is Route.Wizard && mode == WizardMode.Create` to `AppShellScaffold`.
  - `AppShellScaffold.kt:195` has `BackHandler(enabled = currentOverlay != null && !overlayOwnsBack)`.
  - The §5.2 change is `overlayOwnsBack = holder.state.overlay is Route.Wizard`.
- **Overlay slot (§5.2).**
  - `AppShell.kt:111–144` renders `WizardRoot(rememberOverlayViewModel(route.mode, TripWizardViewModel.factory(route.mode), extras), holder::closeWizard)` only for `WizardMode.Create`.
  - Edit and Duplicate fall to `render.value(route)`, which is the `placeholder` lambda.
  - The §5.2 change is to drop the `mode == WizardMode.Create` condition.
- **`WizardLauncher`** (`app/src/main/kotlin/net/chronoflow/app/wizard/WizardLauncher.kt`).
  - Today it is `internal class WizardLauncher(overlay: () -> Route?, push: (Route) -> Unit)` with `pending: AtomicBoolean`, `open()` and `overlayChanged()`. `AppShell.kt:44` builds it as `remember(holder) { WizardLauncher({ holder.state.overlay }, holder::push) }`.
  - `WizardLauncherTest.closingAnOverlayAllowsAnotherOpen` uses the original trailing-lambda constructor. Preserve that two-argument overload when adding the defaulted dependency/dispatcher parameters; keep the earlier test unchanged (Task 6 check, 2026-11-20).
  - It has no session dependencies or dispatchers. Add defaulted constructor parameters: the session's `TripWizardDependencies?` (from `SessionGraph.wizard`, `SessionGraph.kt:40`, implemented by `SessionTripWizardDependencies`) and the IO dispatcher.
  - `AppDispatchers` is `app/src/main/kotlin/net/chronoflow/app/AppDispatchers.kt`, reached as `graph.dispatchers`. Keep the `withContext(io)` that Task 6 asserts, even though the store also switches.
- **`TripPageViewModel`** (`feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageViewModel.kt`, package `…feature.trip.page`).
  - It has **no dependency holder**. It takes constructor parameters, and `tripPageViewModelFactory(…)` builds it (`app/src/main/kotlin/net/chronoflow/app/trip/TripPageRoute.kt:95–155`).
  - Add `preparer: TripWizardPreparer? = null` as a defaulted constructor parameter. Thread it from `AppShell` (`launcher`) through `TripPageRoute(…)` into `tripPageViewModelFactory`. The factory is `remember`ed on `args, appContext, language, freeze`, so pass a stable instance.
  - `tripActions` (l.175–187) is `combine(ui, online)` with `busy = false`; add `openingWizard` to that `combine`. `openTripActions()` is at l.436.
- **`TripPageSheets`** (`feature/trip/src/main/kotlin/net/chronoflow/feature/trip/step/TripPageSheets.kt`, package `…feature.trip.step`): `TripPageSheets(vm, onWizard: (WizardMode) -> Unit = {})`.
  - 6d's `TripActions` branch (l.56–80) already uses its own `actionState = rememberModalBottomSheetState(skipPartiallyExpanded = true)` and passes it to `CfModalBottomSheet(vm::dismissSheet, Modifier.testTag("trip.sheet"), sheetState = actionState)`. On an enabled action it hides the sheet, calls `dismissSheet()`, then calls `onWizard(WizardMode.Edit/Duplicate(ui.tripId))`.
  - The body's `openWizard(route)` is this `onWizard(mode)`. `AppShell.kt:102` wires it to `holder.push(Route.Wizard(it))`, and `TripPage.kt:107/223` and `TripPageRoute.kt:53/89` already forward it.
  - Make `wizardRequests` a `Flow<WizardMode>` (not `Route.Wizard`), and call `onWizard(mode)` after the hide.
- **Rows and Cancel while busy.** `TripActionRow` already shows the `plan_needs_a_connection` subtitle on a disabled row (`…/ft/actions/TripActionsSheet.kt:131`). Cancel is already `enabled = !ui.availability.busy`.
- **6d's navigation tests change.** `TripActionsContentTest.editClickNavigatesToWizardSeam` and `duplicateClickNavigatesToWizardSeam` (`feature/trip/src/test/kotlin/net/chronoflow/feature/trip/actions/TripActionsContentTest.kt:60–94`) assert the direct navigation that Task 5 removes.
  - With a null preparer (`Ignored`) they would fail.
  - Update them in Task 5 to use a fake preparer that returns `Ready`; the expected `onWizard` mode is unchanged.
  - Report this as an edited earlier test.
- **No `wizard.discard` tag.** `CfAlertDialog(title, text, confirm, onDismissRequest, dismiss)` takes no modifier, and its buttons are tagged `cf.dialog.confirm` and `cf.dialog.dismiss` (`core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/system/CfDialogs.kt:84–95`; used by `WizardBackHandlingTest.kt:69–70` and `WizardJourneyTest.kt:66`).
  - Wherever the body says `wizard.discard`, use `cf.dialog.confirm` / `cf.dialog.dismiss`: "dialog shown" means `cf.dialog.confirm` exists.
  - Do not add a tag parameter to `CfAlertDialog`.
- **Phase 2 wrappers.** `CfModalBottomSheet(onDismissRequest, modifier, sheetState, content)` and `CfSnackbarHost(hostState, modifier, onPresented)` match the body (`CfDialogs.kt:37, 106`).
- **9g Ready types** (`…/fw/ready/ReadyUi.kt`, `ReadyUiMapper.kt`).
  - `PriceLine` = `FreeTrip`, `FreeHome` (the body's "one-free line") and `Balance(n, alert)`.
  - `ReadyBannerUi` = `SaveFailed(kind, retrySeconds)`, `Adjusted(reasons)` and `Updated(summary, price)`. There is **no skipped branch-2 placeholder**: insert `SourceChanged` into `ReadyUiMapper.map`'s banner `when`, between `failure != null` and `PlanStatus.Modified`.
  - `ReadyButtonLabel` = `MakePlan`, `Checking`, `Retry` and `Countdown(seconds)`.
  - `ReadyUi.title` is a constant getter (`WizardCopy("Ready to plan")`). Add a defaulted constructor field, such as `editing: Boolean = false`, and branch on it.
  - `ReadyUiMapper.price(price, credits)` has no edit input; add defaulted parameters.
  - `SaveFailed.body` keys only on `kind`; add a defaulted `editing` field for the edit offline and server bodies.
  - The Translate mechanism is `ReadyText.capture { t -> … }`. It returns `ReadyText(pattern, copies)`, which is resolved at render time.
  - Build `ReadyBannerUi.SourceChanged(body: ReadyText)` with that mechanism, not as a pre-formatted `String`; this supersedes the §5.4 note about a formatted string. Wrap it in the mapper's `safely { }` to cover the `extrasSummary` throw.
  - `WizardReadyStep.kt`'s private `ReadyBanner` renders the info banners with the tag `wizard.ready.notice`.
- **Return choice (9d).** The step is `WizardReturnStep(ui, onEvent)` in `…/fw/returnchoice/WizardReturnStep.kt`; there is no `WizardReturnChoiceStep`.
  - Its model is `WizardReturnUi(oneWay, addReturn)` with `WizardReturnChoiceUi(kind, title, hint, selected, enabled)`, built by `WizardReturnUi.from(draft, choicePending, enabled)`.
  - Add a nullable `web: WizardReturnChoiceUi?`. `from` reads `draft.state.edit`, `originalForm`, and the stay end and language for the `check_out` title.
  - Render `web` first with the tag `wizard.choice.web`. A tap is `WizardEvent.ChooseReturn(kind)` after `HapticEvent.Selection`, like the other two cards.
  - The add-return hint is currently always "We plan the shift back home too".
- **Times subtitle (9d).** `WizardTimesUi.subtitle` is a constant getter, "Times are local at each airport." (`…/fw/times/WizardTimesUi.kt:75`), and `WizardTimesUi.from(draft, home, …)` builds the UI.
  - Put the copied-route rule there; editing = `draft.state.edit != null`.
  - `WizardScreen.kt:475–477` renders the subtitle into `wizard.subtitle`.
- **System back on a first screen is not fully built (gap).** OQ-9a-5 says system back on a first screen acts as ×, but `WizardBackHandling.kt:18` sends `RequestClose` only on Where.
  - Elsewhere, `NavDisplay(onBack = { … WizardEvent.Back })` (`WizardScreen.kt:428`) calls `back()` → `popNavigation`, which does nothing on a first screen.
  - Today the shell pops an Edit/Duplicate overlay without the discard check. After §5.2, nothing would handle back there.
  - In Task 1, add a wizard `BackHandler` that sends `RequestClose` on every first screen: edit Ready, edit Checking failure and Duplicate's first Times, keeping Where. Register it so it wins over `NavDisplay`, and disable it while a sheet or the discard dialog is up.
  - `WizardUiState` has no first-screen flag. Add one from the model's `firstScreen` (through `TripWizardUiState.Active`, additive) rather than inferring it from `chrome.showBack`, which is also false on Checking and Calculating.
  - Cover it in Task 1's `WizardOverlayModesTest` and on the JVM in `WizardBackHandlingTest`.
- **`TripWizardDependencies`** (`…/fw/TripWizardDependencies.kt`) has no `recover()`.
  - Add `suspend fun recover(): MemberWizardDraft? = null` with a default, so 9b's fakes and `WizardDebugSeeds`' `by session` delegates compile unchanged.
  - `SessionTripWizardDependencies` (`app/src/main/kotlin/net/chronoflow/app/wizard/SessionTripWizardDependencies.kt`) overrides it with `store.recover()`.
- **Board seeds and freeze** (`…/fwd/WizardBoardCaptureSeed.kt`).
  - **What exists.**
    - `names` (l.20–76) appends `WizardSleepQuestionBoardSeed.names + WizardCompletionBoardSeed.names`.
    - `baseName` (l.78–85) strips `DarkNL`, `Dark`, `NL` and `Large`.
    - `uiOverride(board)` (l.87–88) delegates to `WizardCompletionBoardSeed.uiOverride`.
    - `knows` is at l.90.
    - `presentation(board)` (l.92–103) sets `discardDialogVisible` only for `WizardDiscard`.
    - `draft(board, locale)` ends at l.268–271 with `WizardCompletionBoardSeed.apply(WizardSleepQuestionBoardSeed.apply(…), board)`.
  - **What 9h changes.**
    - Append `TripWizardActionsBoardSeed.names` to `names`.
    - Wrap l.268–271 in `TripWizardActionsBoardSeed.apply(…, board)`.
    - Make `uiOverride` consult `TripWizardActionsBoardSeed.uiOverride(baseName)` first.
    - Set `discardDialogVisible` for `WizardDiscardEdit` in `presentation`.
  - **`WizardDebugSeeds` needs no edit.** The freeze does not live there: `WizardDebugSeeds` only reads `WizardBoardCaptureSeed.uiOverride` and swaps in `FrozenCheckingClock` when that is non-null (`app/src/debug/kotlin/net/chronoflow/debug/WizardDebugSeeds.kt:62–69`).
  - **Edit boards need their own override.** `WizardCompletionBoardSeed.uiOverride` returns null for names outside its `baseNames`.
    - `WizardCheckingEdit` → `WizardUiOverride(checking = WizardCheckingProgress(ticks = 2, slow = false, failure = null), retrySeconds = 0)`.
    - `WizardCheckOfflineEdit` → `failure = DataFailure.Offline`.
    - `WizardCheckServerEdit` → `failure = DataFailure.Server(500)`.
    - The edit Ready and Excluded boards → `WizardUiOverride(reviewing = false, retrySeconds = 0)`.
    - `WizardDuplicate` and `WizardReturnWeb` → null, like 9d's input boards.
  - **Kotlin `WizardForm` is immutable.** `removeReturn()` returns `ReturnRemoval(form, retained)`, so use `.form`. `addReturn()` and `updateLeg(i) { … }` return a new form.
  - **Seeded boards always open through `WizardMode.Create`** (`launcher.open()` plus the overridden `start()`), never through the Edit/Duplicate entry. That is fine, because the draft carries `edit` and `copiedRoute`.
- **Test helper gap.** `boardState(board, locale)` (`…/fwtd/WizardBoardStates.kt:47`) builds `TripWizardUiState.Active` **without `hasChanges`**, so it defaults to `true`. `WizardBoardCaptureTest` and `completionBoard(…)` both use it.
  - As a result, an untouched edit would render "Update my plan" enabled.
  - Pass `hasChanges = model.hasChanges`. This is additive, and create boards are unaffected because the mapper reads it only when editing.
- **Captures.** No new `WizardEditCaptureTest` is needed.
  - 9f's `WizardBoardCaptureTest` (`…/fwtd/WizardBoardCaptureTest.kt`) captures every `WizardBoardCaptureSeed.names` entry through `boardState` + `WizardScreen`. It appends `_noBoard` for the names in its set (l.112–121).
  - Task 8: add the 25 wrappers, `WizardEditSaveOffline` and `WizardEditSaveServer` to the seed names, and add the last two to that `_noBoard` set.
  - The trip actions captures extend `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/actions/TripActionsCaptureTest.kt` (it exists).
- **Guards.** Use the pattern of 9g's `WizardReadyGuardsTest` (`…/fwtd/WizardReadyGuardsTest.kt`), not `Guards.assertAll` on `WizardRoot`:
  - `@Layout` with `ParameterizedRobolectricTestRunner`;
  - `completionBoard(board, locale)` → `WizardScreen(state, formatter, {})`;
  - `Guards.assertTextFits`, `assertTouchTargets`, `assertInteractiveInSafeArea(…, rootTag = "wizard.window")`, `assertNoInteractiveOverlap(…, rootTag = "wizard.window")` and `assertHeadings(compose, "wizard.title")`;
  - a `check(it.size == N)` on the cases (228 here).
- **Test locations.**
  - `feature/wizard` tests are flat in package `net.chronoflow.feature.wizard`. Pure JVM tests go in `…/fwt/`. Anything that uses the `src/debug` seeds goes in `…/fwtd/`. Ignore the body's `ready/`, `edit/`, `returnchoice/`, `outbound/` and `debug/` subfolders.
  - You may extend existing classes instead of adding new ones: `ReadyPriceUiTest`, `ReadyBannerUiTest`, `WizardReturnUiTest`, `WizardDiscardUiTest` and `WizardBackHandlingTest` (`…/fwt/`), and `WizardTimesUiTest` and `WizardBoardCaptureSeedTest` (`…/fwtd/`).
  - App Robolectric tests that need debug code go in `…/apptd/<shell|wizard>/`, where `AppShellTest` and `WizardProcessDeathTest` live. `WizardLauncherTest` is in `…/appt/wizard/`.
- **Task 9 fixture.** 9g's `WizardProcessDeathTest` does **not** boot the real `AppGraph`.
  - It uses `WizardSaveGraphFixture` (`…/apptd/wizard/WizardSaveGraphFixture.kt`): a `MockDataSource(store = FileMockStateStore(root), seed = …)` plus a store and the VM. AGENTS "Tests" forbids booting `AppGraph` or `MainActivity` for logic, so follow that fixture style.
  - If shell, trip page and wizard must be composed together and that is not possible without the real graph, use the body's fallback: move the check to Task 10's emulator class as a seeded single-screen check, and record it.
- **Debug counter.** Follow `RefusedChangeDebug`:
  - The debug `object` with `presentedCount: AtomicInteger` is in `core/ui/src/debug/kotlin/net/chronoflow/core/ui/plan/RefusedChangeDebug.kt`, and a no-op `object` with the same name is in `core/ui/src/release/…`.
  - `ReleaseReadinessTest.kt:78–84` (`app/src/testRelease`) asserts that the release class lacks the field.
  - So `TripActionsDebugCounters` gets a `feature/trip/src/debug/…/actions/` version with `failurePresented` and a `feature/trip/src/release/…` no-op. `:feature:trip` has no `src/debug` or `src/release` yet.
  - Acceptance 7 checks that the release class has no `failurePresented` field; the class itself exists in release.
- **Class warm-up.** Warm one-off cold costs once in `@BeforeClass` through `measureClassWarmup(javaClass, sdk, label) { … }` (`core/testing-junit/src/main/kotlin/net/chronoflow/core/testing/junit/ClassWarmup.kt`).
- **Strings.**
  - The PlanUI table is named `plan`, not `planui`: `plan_needs_a_connection`, `plan_we_could_not_duplicate_this_trip_try_again` and `plan_something_went_wrong_on_our_side_try_208515`, in `net.chronoflow.core.ui.R`.
  - Wizard copy goes through `WizardCopy(key, args).resolve()`.
- **Test tags.**
  - **Confirmed:**
    - wizard chrome: `wizard.title`, `wizard.subtitle`, `wizard.nav-title`, `wizard.back`, `wizard.close`;
    - Ready: `wizard.ready`, `wizard.ready.notice`, `wizard.ready.price`, `wizard.ready.no-credits`, and `wizard.ready.<row id>` (e.g. `wizard.ready.extras`);
    - footer: `wizard.<id>.footer`, `wizard.<id>.continue` and `wizard.<id>.secondary-close`, so `wizard.ready.footer`, `wizard.ready.continue`, `wizard.outbound.continue` and `wizard.fine-tune.continue` exist;
    - other wizard steps: `wizard.0.departure.date`, `wizard.preparation`, `wizard.calculating`, `wizard.choice.one-way`, `wizard.choice.add-return`, `wizard.return.choices`;
    - trip page: `trip.actions`, `trip.actions.edit`, `trip.actions.duplicate`, `trip.actions.cancel`, `trip.actions.open`, `trip.sheet`, `trip.page`, `route.tripPage`.
  - **To add:** `wizard.choice.web` and `trip.actions.failure`.
  - **Not present:** `wizard.discard` (see above).
- **No copy assertions on screen (AGENTS "Tests" 1).** Several body tests check visible text:
  - Task 3: `wizard.title` text "Je reis bewerken", `onAllNodesWithText("Credits kopen op de website")`, and the dialog showing "Wijzigingen verwijderen" / "Verder bewerken";
  - Task 4: the `wizard.subtitle` and `wizard.nav-title` texts;
  - Task 5: `trip.actions.failure` showing the nl text;
  - Task 9: the failure messages;
  - Task 10: "its text is 'Update my plan'".

  Drop those text checks. Find nodes by tag and check only structure, enabled state and existence. Prove the copy on the JVM through the UI state's `WizardCopy` keys and arguments instead: `ready.title`, `discardTitle`, `discardConfirmLabel`, `times.subtitle`, `ReadyButtonLabel.Update`, `TripActionFailure`. The en/nl values are covered by the string generator tests and the captures. For example:
  - the no-billing check becomes "only `wizard.ready.no-credits` is in `wizard.ready.footer`" (plus acceptance 8's grep);
  - the dialog check becomes "`cf.dialog.confirm` exists" plus a JVM assertion on `discardConfirmLabel == WizardCopy("Discard changes")`.
- **Seeds.**
  - `CF_AUTH_TEST_ID`, `CF_UI_TEST_PROFILE`, `CF_AUTH_TEST_SIGNED_IN`, `CF_AUTH_TEST_WALKTHROUGH_SEEN`, `CF_MOCK_DELAY`, `CF_PLAN_TEST_ROUTE` (`trip:<uuid>`) and `CF_WIZARD_BOARD_CAPTURE` all parse in `…/appd/TestSeed.kt`.
  - `CF_WIZARD_BOARD_CAPTURE=TripActions` opens the actions sheet through `LaunchPlatform.initialTripSheet` (`app/src/debug/kotlin/net/chronoflow/app/LaunchPlatform.kt:34–43`).
  - `WizardDebugSeeds.prepare` ignores boards whose names do not start with `Wizard`.
  - `ScreenTest` is `app/src/androidTest/kotlin/net/chronoflow/app/testing/ScreenTest.kt`.

**R4. plan.md line refs at `2e1c86fb`** (these replace the body's):
- **Phase 9h entry:** l.2966–2996. Its boxes are l.2982 ("**Edit**"), l.2987 ("**Duplicate**") and l.2988 ("**Tests.**"), all unticked.
- **Decisions:** A14 is l.101 and A24 is l.111.
- **Open questions:** OQ-A5 is l.602, OQ-A18 l.615 and OQ-A36 l.633. "Open questions for the user" starts at l.592, and its last row is OQ-A449.
- **Spec sections:**
  - "Android spec: state survival" is l.373, with the Wizard row at l.379.
  - "navigation and routes" is l.400, with the routes at l.402.
  - "seeds and test hooks" is l.466, with the mock trips at l.476.
  - "Android-only copy awaiting approval" is l.487.

**R5. OQ numbers.** In Task 11, append the questions to plan.md with these numbers (the next free one is OQ-A450):

| Brief | plan.md |
|---|---|
| OQ-9h-1 | OQ-A450 |
| OQ-9h-2 | OQ-A451 |
| OQ-9h-3 | OQ-A452 |
| OQ-9h-4 | OQ-A453 |
| OQ-9h-5 | OQ-A454 |
| OQ-9h-6 | OQ-A455 |
| OQ-9h-7 | OQ-A456 |
| OQ-9h-8 | OQ-A457 |
| OQ-9h-9 | OQ-A458 |
| OQ-9h-11 | OQ-A459 |
| OQ-9h-12 | OQ-A460 |

OQ-9h-10 is already resolved and is not appended.

**R7. Progress settlement (orchestrator, 2026-11-20).** OQ-A351's all-edit-step 1.0 was a plan default. OQ-A461 settles it: keep the initial edit floor 1.0; use pinned iOS's `max(progressFloor ?: 0, stepProgress)` so the existing `go()` clearing rule takes effect after revisiting an input step. A revisited Coming back? is 0.45, including `WizardReturnWeb`; later Checking/Ready use their step weights. Port and JVM-test this in a separate signed `fix(wizard): clear the edit progress floor on revisited input steps` commit before completing Task 7.

**R6. Blockers.** None. Every behaviour has an iOS source or a board. The gaps named in R3 are build work inside this phase: first-screen system back, `recover()`, the `boardState` `hasChanges`, the edit seed overrides and the preparer wiring.

This phase adds **wiring, the edit/duplicate variants of existing screens, seeds and tests**. The edit and duplicate *model* is already ported: 9a has `loadTrip`, `clearDuplicateDates`, `inputFingerprint`, `hasChanges`, `isEditing`, `checkingTitle`, `acceptSourceReload`, `SourceChange`, `sourceChangeSummary`; 9b has `MemberWizardStore.startTrip`, the edit source refresh (`refreshSource`, one re-review on a revision mismatch), `reviewTripEdit`, `PUT` through `updateTrip`, and all 5 iOS `TripWizardActionsTests` (1 in 9a, 4 in 9b). **Do not re-port or duplicate those model tests.** If a model rule below is missing from 9a/9b, port it into `:core:wizard` with its iOS test in a separate `fix(wizard): …` commit that names the Swift line, and report it.

---

## 1. Goal

From a trip page, **Trip actions → Edit trip** loads the trip's stored input while the actions sheet stays open and busy, then opens the wizard in edit mode. It starts on **Checking** ("Checking your changes", bar at 100 %), reviews against the trip's `sourceRevision`, and lands on **Ready** titled "Edit your trip", where "Update my plan" stays disabled until something changed. Edit Ready covers every board variant: unchanged (`WizardEdit`), changed on another device (`WizardEditChanged`, "Updated" banner), return removed (`WizardEditNoReturn`), return added (`WizardEditReturn`, 1 credit), return added with no credits (`WizardEditReturnNoCredits`, Close only), and a web trip that ends after nights (`WizardEditWeb`, plus `WizardReturnWeb` on Coming back?). The edit Checking and its offline/server failure cards (`WizardCheckingEdit`, `WizardCheckOfflineEdit`, `WizardCheckServerEdit`), the edit No-plan note (`WizardExcludedEdit`) and the edit discard alert (`WizardDiscardEdit`, "Discard your changes?") all work. Update saves with `PUT /v1/trips/{id}` + `source_revision`, then opens that trip's page awaiting sync.

**Trip actions → Duplicate trip** loads the same input, clears every date-bound answer and opens the wizard on **Times** ("New trip", no back chevron) with empty date pills and the subtitle "Copied from Amsterdam → Tokyo. Choose the new dates." It then follows the create path and saves a **new** trip with `POST /v1/trips`.

A load failure keeps the actions sheet open and shows a snackbar: "Needs a connection." (offline), "We could not duplicate this trip. Try again." (duplicate, other errors) or "Something went wrong on our side. Try again in a moment." (edit, other errors).

Done means:
- every edit/duplicate board (25 wrappers) is guarded on Robolectric and captured;
- the edit Ready price lines, labels, banner and button rules are proven on the JVM;
- the iOS `TripWizardActionsScreenTests` (4) and `TripActionsScreenTests` (2) are ported at the cheapest tier;
- one emulator journey edits the mock's upcoming trip from its actions sheet, saves and lands on its trip page.

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it: "Working rules", "Performance", "Tests", "Traps").
2. `plan.md` sections listed in the header.
3. `docs/research/06-wizard.md`: §2.1 (the edit/duplicate rows of the board map, l.82–152), §3.1 last bullet (Duplicate), §3.2 (edits ask "Discard your changes?"), §3.4 (first screens: Duplicate's Times, Edit's Ready, edit failure), §3.8 "Decisions" (editing opens on Ready; editing is free except adding a return; web trip ends), §4.1 (Edit and Duplicate entries, l.306–319), §4.3 "Coming back?" (web end, edit only), §4.4 (edit price rows, l.462–477), **§4.5** (edit specifics, l.479–487), §4.7 (recovery holds the edit context), §7.1–§7.3 (edit endpoints and errors), §11 (discrepancy 2: back on Duplicate/edit failure), §12 OQ 9.
4. `docs/research/03-api-contract.md` **§11** (l.540–618: request/response shapes, the edit flow, `source_revision`, Duplicate has no endpoint).
5. Earlier briefs and their merged code:
   - `phase-9a-wizard-model.md` §Task 5 item 6 (`loadTrip`, edit `progressFloor = 1.0`, OQ-9a-3), Task 7 (`acceptSourceReload`, `checkingTitle`), Task 9 (`WizardPresentation.sourceChangeSummary`), §10.2, OQ-9a-5 (back on Duplicate's Times and edit failure = first screens; system back acts as ×);
   - `phase-9b-wizard-data.md` §6.1–§6.2 (edit review, `refreshSource`, save with `updateTrip`), §6.5 (`startTrip`), §6.8 (`TripWizardViewModel`, `TripWizardDependencies`, `Loading`/`LoadFailed`), OQ-9b-12 ("9h decides the presentation" of a failed load), §12 "9h";
   - `phase-9c-wizard-shell-where.md` §5.1 (nav title "Edit trip" for Edit), §5.4 (back, close, discard: `requestClose`, `confirmDiscard`, `closing`), §5.8 (overlay slot, `overlayStoreOwner`, overlay `BackHandler` condition, `WizardLauncher`, `WizardRecoveryCheck`, seeds), out-of-scope row l.204;
   - `phase-9d-wizard-flights.md` §5.8 (Coming back? cards), out-of-scope row l.65;
   - `phase-9e-wizard-sleep-questions.md` §5.6 (Excluded: the edit note is already implemented), Task 3 seeds;
   - `phase-9f-wizard-checking-finetune.md` §5.2 (failure cards already map the edit bodies), §5.10 (`WizardUiOverride`), out-of-scope row l.96;
   - `phase-9g-wizard-ready-save.md` §5.1–§5.7 (Ready layout, `ReadyUiMapper`, price block, banner slot with branch 2 left for 9h, footer, `WizardExit.Saved`), Task 5 (`WizardCompletionBoardSeed`), §6 (types), §10 (OQ-9g-*);
   - `phase-6d-trip-interactions.md` Task 1 (`TripActionsAvailability`, `enum class TripAction { Edit, Duplicate }`), Task 8 (`TripActionsContent`, the sheet in `TripPageSheets`, the wizard seam), out-of-scope row l.86;
   - `phase-0c-strings.md` §11 (resources, `_one` plurals), `phase-0e-test-harness.md` (Guards, BoardCapture, ScreenTest, seeds), `phase-4c-mock-data-source.md` (mock wizard state, receipts with `tripID`/`sourceRevision` in the intent).

### 2.2 Boards (`DC/`, read-only; open the HTML in a browser to render)

Every wrapper is a 0.8 KB file that `<dc-import name="Wizard" screen=… v=… [alert=…] [lang=nl]>`s `DC/Wizard.dc.html` at 390 × 844; the wrapper's `renderVals()` sets `pct`. Every `*DarkNL` twin is the same state in `theme=dark lang=nl`. `isEdit` = `v` starts with `edit` (`Wizard.dc.html:239`).

| Board (and twin) | Props | `pct` | State |
|---|---|---|---|
| `WizardEdit`, `…DarkNL` | `ready v=edit` | 100 | Nav "Edit trip"; title "Edit your trip"; rows Route AMS → NRT, Flight "Mon 12 Oct 14:40 → Tue 09:15", Return "One way", Sleep "23:30–07:15", Extras "2 answered · 3 days prep"; "Price **Free**"; note "Changes recalculate the plan. Check-offs on steps that stay in your plan are kept."; "Update my plan" **disabled**; no back chevron; × closes at once |
| `WizardEditChanged`, `…DarkNL` | `ready v=editchanged` | 100 | Info banner "Updated" / "On another device: the return flight moved to Thu 22 Oct. The price is still 1 credit."; Return row "Thu 22 Oct 10:25 → 15:30"; return summary and dates; "Price **1 credit**", "You have 3 credits", edit-return note |
| `WizardEditNoReturn`, `…DarkNL` | `ready v=editnoreturn` | 100 | Return removed: Return row "One way"; "Price **Free**"; "Removing a flight doesn't give a credit back."; edit note |
| `WizardEditReturn` (no twin) | `ready v=editreturn` | 100 | Return added: Return row "Thu 22 Oct 10:25 → 15:30"; "Price **1 credit**", "You have 3 credits", "Adding a return flight costs 1 credit, because credits are per direction. Other changes are free." |
| `WizardEditReturnNoCredits`, `…DarkNL` | `ready v=editreturnnone` | 100 | As EditReturn but red semibold "You have 0 credits"; board button "Buy credits on the website" + Close → **Android: Close only** (A14) |
| `WizardEditWeb`, `…DarkNL` | `ready v=editweb` | 100 | Return row "9 nights · until Wed 21 Oct"; dates "Fri 9 Oct – Thu 22 Oct · 9 nights"; "Price **Free**"; edit note |
| `WizardReturnWeb`, `…DarkNL` | `ret v=editweb` | 45 | Nav "Edit trip"; "Coming back?"; three cards: **"Ends after 9 nights"** / "Set on the website. Keep it, or add your flight home." (selected, first), "One way" / "Just this flight", "Add my return flight" / "We plan the shift back home too · 1 credit"; "Tap an answer to continue." |
| `WizardCheckingEdit`, `…DarkNL` | `checking v=edit` | 100 | "Checking your changes"; no back chevron |
| `WizardCheckOfflineEdit`, `…DarkNL` | `fail v=editoffline` | 100 | Offline card, body "Connect to the internet and try again. If you close now, your trip stays as it was."; Try again + Close; **no back chevron** |
| `WizardCheckServerEdit`, `…DarkNL` | `fail v=editserver` | 100 | Server card, body "Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was."; no back chevron |
| `WizardExcludedEdit`, `…DarkNL` | `excluded v=edit` | 100 | "No plan available"; shift-work reason line (+ doctor line on the board); note "Your trip stays as it was." |
| `WizardDiscardEdit`, `…DarkNL` | `ready v=editreturn alert=edit` | 100 | Alert over EditReturn: "Discard your changes?" / "Your trip stays as it was." / "Keep editing" / "Discard changes" (board grid: one column because "Discard changes" > 12 chars) |
| `WizardDuplicate`, `…DarkNL` | `outbound v=dup` | 22 | Nav "New trip"; "When is your flight?"; sub "Copied from Amsterdam → Tokyo. Choose the new dates." (nl "Gekopieerd van Amsterdam → Tokio. Kies de nieuwe data."); empty pills ("Date", "Time"); Continue disabled; **no back chevron** |

Board logic to read: `Wizard.dc.html` l.239–263 (`isEdit`, `exitHref` = Plan for edit, `closeHref`: unchanged edit closes at once, `firstScreen`, `back`, `title`), l.324 (dup empties the pills), l.362–364 (web card, edit `retSub`), l.383–386 (edit failure bodies), l.409 (edit excluded lines and note), l.430–461 (Ready rows, price lines, `noCredits`, edit lines, `editchanged` banner), l.472 (`dupSub`), l.475–488 (titles, CTA, `ctaOn: v !== 'edit'`), l.505–511 (edit alert), copy table `T` at l.228.

**Boards that do not exist (do not invent):** an edit save-failure banner (iOS builds it with edit bodies; build it, capture as `WizardEditSaveOffline_noBoard` and `WizardEditSaveServer_noBoard`, §5.4), an edit Ready re-checking state (reuse 9g's `WizardReadyChecking` look with the edit label; no capture), a busy trip actions sheet and its failure toast (iOS builds both; build them, capture as `TripActionsBusy_noBoard` and `TripActionsFailure_noBoard`, §5.1), and any loading or error screen for a failed Edit/Duplicate load inside the wizard (none is drawn: §5.2 keeps the load on the sheet; the rare in-wizard failure closes quietly, OQ-9h-4).

### 2.3 iOS (`f391a1fa`) — behaviour to mirror

| File | What to take |
|---|---|
| `Kit/TripWizard/Model/TripWizardActions.swift` (whole, 113 lines) | `clearDuplicateDates` l.7–27, `inputFingerprint`/`hasChanges` l.29–40, `loadTrip` l.42–73 (edit: step `checking`, `progressFloor`, `returnToReady`, `originalInput`; duplicate: `clearDuplicateDates`, `entered`, step `outbound`; both: `copiedRoute`), `isEditing`, `checkingTitle` l.76–82, `acceptSourceReload` l.84–112. Ported by 9a — verify only |
| `App/TripPageDestination.swift` l.37–47, l.136–155, l.191–228 | Prepare-then-present: the actions sheet stays open with `busy = openingWizard`, the wizard is presented only after the sheet dismissed (`onDismiss: presentPreparedWizard`); failure copy `actionFailure` (offline → "Needs a connection.", duplicate → "We could not duplicate this trip. Try again.", edit → "Something went wrong on our side. Try again in a moment."); `FeedbackToast` with id `trip.actions.failure`; `actionFailure = nil` when the sheet opens and when a new action starts |
| `App/AppTabs.swift` l.170–186, l.494–513 | `prepareTripWizard`: guards (`!openingWizard`, `wizard == nil`, session), `startTrip(id, action, source)`, the generation service cancelled on failure; saved → `tripsPath = [TripPageRoute(id, awaitingSync: true)]` on the Trips tab (edit and duplicate alike) |
| `Kit/PlanUI/TripPage/TripActionsSheet.swift` l.13–66 | `busy` disables Cancel (l.53) and interactive dismiss (`interactiveDismissDisabled(busy)`, l.66); the rows use `TripActionsAvailability(…, busy:)` |
| `Kit/TripWizardUI/MemberWizardReadyView.swift` l.9–34, l.126–132, l.157–215, l.274–300 | Edit title "Edit your trip"; button "Update my plan"; banner priority with `sourceChange` second and the "Updated" title; `price` (edit + cost 0 → "Free" **before** the unlimited check); `priceBlock` edit lines; edit save-failure bodies |
| `Kit/TripWizardUI/MemberWizardSteps.swift` l.65–150, l.201–236 | Close/× rules (`mode == guest || !hasChanges` → close, else discard alert; `excluded` Close closes directly); nav title "Edit trip"/"New trip"; Coming back? web card (title "Ends after {{count}} nights" or the check-out `returnSummary`, hint "Set on the website. Keep it, or add your flight home.", selected, id `web`, action `chooseReturn(end.kind)`); edit hint "We plan the shift back home too · 1 credit" when `isEditing && originalForm.end.kind != "return_legs"` |
| `Kit/TripWizardUI/MemberWizardView.swift` l.125–136, l.143–160 | Discard alert copy per mode; Times subtitle "Copied from {{route}}. Choose the new dates." when `!home && copiedRoute != nil && !isEditing` |
| `Kit/TripWizardUI/MemberWizardCheckingView.swift` l.104–157, l.222–257 | Edit failure bodies and the edit excluded note. Already built by 9f/9e — verify only |
| `Kit/TripWizard/Model/WizardPresentation.swift` l.4–74 | `SourceChange`, `sourceChangeSummary` sentence rules. Ported by 9a — verify only |
| `App/Debug/TripWizardActionsBoardSeed.swift` (whole, 101 lines) | The edit/duplicate board drafts (§5.6) |
| `App/Debug/WizardBoardCaptureSeed.swift` l.125–131, l.208–225 | `WizardReturnWeb` (`nights = 9`, step `returnChoice`), the dispatch order (`TripWizardActionsBoardSeed.apply(WizardCompletionBoardSeed.apply(draft))`), the freeze: `CheckOfflineEdit` → offline, `CheckServerEdit` → server(500), `discardConfirmation` for `WizardDiscardEdit` |
| `AppUITests/TripWizardActionsScreenTests.swift` (whole, 58 lines) | The 6 seeded/navigation checks to port (Task 9, Task 10) |
| `AppTests/TripWizardActionsLayoutTests.swift` l.18–51 | Footer containment for `WizardEditReturnNoCredits` and `WizardEditChanged` at xxLarge, nl (Task 8) |
| `Packages/ChronoFlowKit/Tests/TripWizardTests/TripWizardActionsTests.swift` | Already ported by 9a/9b (5 of 5). Read only to know what is covered |
| `plan.md` l.1926–1939 ("17c", "17d") | Decisions: the per-card `canEdit`/`canDuplicate` flags; one actions-to-wizard smoke each for Edit and Duplicate |

### 2.4 API (web `e36368ec`; this phase calls no endpoint directly — 9b's store and service do)

| Call | Route (`routes/api.php`) | Request | Success | Used by |
|---|---|---|---|---|
| `GET /v1/trips/{trip}/input` | l.101, `verified`, `api` | — | 200 `ApiTripInputSnapshot {input (snake_case, sent back verbatim), sourceRevision (64 chars), airports: {IATA: ApiAirportSuggestion}, stayEnd?: {checkOutLocalDate, leaveUtc}}` | `MemberWizardStore.startTrip` (prepare, §5.2), and again before **every** edit review (9b `refreshSource`) |
| `POST /v1/trips/{trip}/review` | l.88–89, `verified`, `throttle:trip-review` 10/min/user | `ApiTripReviewInput` + `"source_revision": "<64 chars>"` | 200 `ApiTripReview` with `identity.sourceRevision` | edit Checking, edit Ready re-check |
| `PUT /v1/trips/{trip}` | `verified`, `api` + `trip-save` 10/min | `{"draft_token","input_revision","review_revision","source_revision"}` | **200** `{"tripId": "<same id>"}` | "Update my plan" |
| `POST /v1/trips/review`, `POST /v1/trips` | l.85, l.87 | create shapes (9g §2.4) | 200 / **201** `{tripId}` | Duplicate (fresh identity, no `source_revision`) |

[F `03 §11`, `06 §4.5`] A completion, reopen or edit on another device changes `sourceRevision`. 9b's service then reloads the input, drops `identity` and `profile`, sets `edit.sourceRevision` to the new one, and 9a's `acceptSourceReload` records `sourceChange` (fields route/flight/returnFlight/sleep/extras) when a review already existed — that is the `WizardEditChanged` banner. A second revision mismatch → `ReviewExpired`. Edit errors are the create ones (9g §2.4): `Offline` → offline card/banner; `Throttled(retryAfter)` → throttle; 5xx/other → server; 422 `review_expired` / `Not enough credits` at save → identity cleared, Ready re-checks. Adding a return costs 1 credit (server `price.state = edit`, `cost = 1`); removing it gives nothing back.

---

## 3. Out of scope (owner)

| Item | Owner |
|---|---|
| The edit/duplicate model, edit refresh, `PUT`, the 5 `TripWizardActionsTests` | 9a/9b (done; verify only) |
| Any billing hand-off; "Buy credits on the website"; `SetPaywall` (board `WizardEditReturnNoCredits` draws it) | never (A14, OQ 41) |
| Complete, reopen, delete trip; Add to calendar rows in the actions sheet | never (A24, OQ 31, OQ 42) |
| Re-keying queued check-offs whose `planInputHash` changed after a `PUT` | 4d already treats the 422 `form` error as stale (pull + retry); nothing new here. Verify in the PR that the journey produces no refused-change UI |
| Guest Edit/Duplicate (none exist) | — |
| The trip page's own refresh after sync | 6b/6c (`awaitingSync`) |
| Wizard baseline-profile journey | 13. Run `make baseline-profile` only if the generator already has a wizard journey; say which in the PR |
| Changing iOS catalog copy | never (generated by `make strings`) |

---

## 4. Hand-ins to verify before Task 1 (no commit)

Check each with `grep -rn` in the merged code. If one is missing and has no fallback named here, **stop and report**.

1. **9a** (`:core:wizard`): `TripWizardAction { edit, duplicate }`; `WizardForm.clearDuplicateDates()`; `MemberWizardDraft.loadTrip(snapshot, id, action)` with edit `progressFloor = 1.0`; `inputFingerprint`, `hasChanges`; `MemberWizardModel.isEditing`, `hasChanges`, `checkingTitle`, `acceptSourceReload`; state fields `edit: TripEditContext?`, `originalForm`, `originalInput`, `copiedRoute`, `snapshotStayEnd`, `sourceChange: WizardPresentation.SourceChange?`, `entered`; `WizardPresentation.sourceChangeSummary(change, review, stayEnd, language, t)` and `returnSummary`; `firstScreen` true for Duplicate's first Times, edit Ready and an edit Checking failure (9a `WizardNavigationTest`).
2. **9b:** `MemberWizardStore.startTrip(id, action, source)` (fetch `tripInput`, `loadTrip`, `replaceRecovery`) and `recover()`; `TripWizardDependencies.startTrip(id, action)`; `TripWizardViewModel(entry: core.navigation.WizardMode, deps)` with `Loading`/`Active`/`LoadFailed`/`Closed`; the service's edit refresh and `updateTrip` on save; 9b's `TripWizardActionsTests` (4 methods) and `TripWizardViewModelTest.editAndDuplicateLoadTripInput` / `editLoadFailureIsExposed` green.
3. **9c:** `AppShell`'s overlay slot renders `WizardRoot` only for `WizardMode.Create` and `placeholder(route)` for `Edit`/`Duplicate`; the overlay `BackHandler` condition `enabled = overlay != null && !(overlay is Route.Wizard && overlay.mode == WizardMode.Create)`; `WizardLauncher` (`open()`, pending guard); `WizardRecoveryCheck`; `ShellHolder.closeWizard(exit)`; `WizardExit` (a sealed interface after 9g: `Trips`, `Saved(tripId)`); `requestClose()` / discard dialog (`wizard.discard`) / `confirmDiscard()`; `WizardChromeUi(showBack, showClose, navTitle…)`; the seed dispatcher with `WizardStartOverride`, the 9c `discardDialogVisible` seed flag, the suffix stripping.
4. **9d:** `WizardChoiceCard`, the Coming back? step (`wizard.choice.one-way`, `wizard.choice.add-return`, `vm.chooseReturn(kind)`), the Times step title/subtitle mapping (9d brief l.127: sub "Times are local at each airport.").
5. **9e/9f:** Excluded with the `isEditing` note (`wizard.excluded.note`); Checking with `checkingTitle`; failure cards with the edit bodies (`editing = true` in `WizardCheckingUi`); `WizardUiOverride(checking, retrySeconds, reviewing, answerError)` and its `TripWizardDependencies.uiOverride` seam; `WizardCompletionBoardSeed`.
6. **9g:** `ReadyUi`, `ReadyUiMapper`, `PriceValue`, `PriceLine`, `ReadyBannerUi` (with the skipped branch 2), `ReadyFooterUi`, `ReadyButtonLabel`, the save-failure banner copy table, `WizardExit.Saved`, `ShellHolder.closeWizard(Saved)` → `ShellNav.wizardSaved`, the Translate/copy mechanism the mapper uses for `WizardPresentation.flightSummary`.
7. **6d:** `enum class TripAction { Edit, Duplicate }` and `TripActionsAvailability(canEdit, canDuplicate, online, busy)` in `:core:plan` (`…plan.actions`); `TripActionsUi(tripId, header, availability)` with `busy = false`; `TripActionsContent(ui, onAction, onCancel)` (Cancel tag `trip.actions.cancel`, disabled while busy); `TripPageSheets`' `TripActions` branch that hides, dismisses and then pushes `Route.Wizard(WizardMode.Edit/Duplicate(tripId))` (find it with `grep -rn "WizardMode.Edit\|WizardMode.Duplicate" feature app`); the `CF_WIZARD_BOARD_CAPTURE=TripActions` seed that opens the sheet once.
8. **4c:** `MockDataSource` implements `tripInput`, `reviewTripEdit` and `updateTrip`; the mock's upcoming trip (plan: `…0002`, AMS→JFK; confirm the id in `MockScenarios`) returns a snapshot from `tripInput` and has `canEdit = canDuplicate = true` on its card (iOS 17c decision: mock fixtures emit both true). If `tripInput` throws for every seeded trip, report it (the journey needs it).
9. **0c:** resources exist for every key in §5.8 (`tools/l10n/names.lock.json`, tables `wizard` and `plan`; the iOS PlanUI catalog is the `plan` table); "Ends after {{count}} nights" is a plain string (it has no `_one` in the iOS catalog); "{{count}} credits" is a plural.
10. **Phase 2:** `CfModalBottomSheet(onDismissRequest, modifier, sheetState, content)` and `CfSnackbarHost(hostState, modifier, onPresented)`.

---

## 5. Spec

### 5.1 Trip actions: prepare, busy, failure (`:feature:trip` + `:app`)

[F `TripPageDestination.swift` l.136–155, l.209–228; `AppTabs.swift` l.494–513; `TripActionsSheet.swift` l.53, l.66]

**Why prepare on the sheet.** iOS loads the input *before* presenting the wizard, so a failure shows on the sheet and the wizard never shows a loading or error screen (neither has a board). Android does the same [R].

**Seam** (in `:feature:trip`, `…/ft/actions/TripWizardPreparer.kt`) [R]:

```kotlin
fun interface TripWizardPreparer {
    /** Loads the trip input and writes the wizard recovery row. Never throws except CancellationException. */
    suspend fun prepare(tripId: String, action: TripAction): TripWizardPrepareOutcome
}
sealed interface TripWizardPrepareOutcome {
    data object Ready : TripWizardPrepareOutcome       // row written; push Route.Wizard next
    data object Ignored : TripWizardPrepareOutcome     // a wizard is open or opening, or no session; do nothing
    data class Failed(val offline: Boolean) : TripWizardPrepareOutcome
}
```

Provide it to `TripPageViewModel` through the same dependency holder its factory already reads (6b; find it with `grep -rn "TripPageViewModel(" feature app`). `:app` implements it in `WizardLauncher` (§5.2). A null preparer (tests, no session) behaves as `Ignored`.

**`TripPageViewModel` additions** (6b/6d file, edited):
- `private val openingWizard = MutableStateFlow(false)`; `tripActions` maps it into `TripActionsUi.availability.busy` (6d set `busy = false`).
- `val actionFailure: StateFlow<TripActionFailure?>` (backed by a private `MutableStateFlow`; "set `actionFailure`" below means that backing flow) with `enum class TripActionFailure { Offline, Duplicate, Edit }` (`…/ft/actions/TripActionFailure.kt`).
- `val wizardRequests: Flow<Route.Wizard>` — a one-shot (`Channel(Channel.BUFFERED)` → `receiveAsFlow()`), consumed once by the sheet host.
- `fun startAction(action: TripAction)`:
  1. return if `openingWizard.value`, or `tripActions.value == null`, or `!tripActions.value.availability.copy(busy = false).enabled(action)` (iOS re-checks availability with `busy: false`, `TripPageDestination.swift:205–210`);
  2. `actionFailure.value = null`; `openingWizard.value = true`;
  3. `viewModelScope.launch { try { when (val o = preparer.prepare(tripId, action)) { Ready -> wizardRequests.send(Route.Wizard(if (action == Edit) WizardMode.Edit(tripId) else WizardMode.Duplicate(tripId))); Ignored -> Unit; is Failed -> actionFailure.value = if (o.offline) Offline else if (action == Duplicate) Duplicate else Edit } } finally { openingWizard.value = false } }`. Do not catch `CancellationException`.
- `openTripActions()` (6d) also sets `actionFailure.value = null` (iOS l.234–237).

**Sheet host** (`TripPageSheets`, the `TripActions` branch from 6d, edited):
- `val busy = ui.availability.busy`. The sheet state is `rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busyState.value })` with `busyState = rememberUpdatedState(busy)`, and `onDismissRequest = { if (!busy) vm.dismissSheet() }`. This is the `interactiveDismissDisabled(busy)` equivalent: drag, scrim tap and back do nothing while busy. If `CfModalBottomSheet` does not forward `sheetState`, it does (Phase 2 signature); do not add a new wrapper parameter.
- `onAction = vm::startAction` (6d's direct navigation is removed).
- `LaunchedEffect(Unit) { vm.wizardRequests.collect { route -> sheetState.hide(); vm.dismissSheet(); openWizard(route) } }` where `openWizard` is the callback 6d already uses to push the wizard route (iOS presents the wizard only after the sheet dismissed, `onDismiss: presentPreparedWizard`).
- Cancel (`trip.actions.cancel`) is already disabled while busy (6d).
- **Rows while busy** [F `TripActionsAvailability` with `busy = true`; 6d checklist "Disabled rows show 'Needs a connection.' whatever the reason"]: both rows disabled with the "Needs a connection." subtitle, exactly as 6d draws a disabled row. No spinner (iOS shows none).
- **Failure snackbar** [OQ-A5: Snackbar, not the designed toast]: a `CfSnackbarHost` placed **inside** the sheet content (a `ModalBottomSheet` is its own window, so the page's host would sit under the scrim), bottom of the column, `Modifier.testTag("trip.actions.failure")`. `LaunchedEffect(failure) { if (failure != null) hostState.showSnackbar(message, duration = SnackbarDuration.Short) }`. Messages (PlanUI catalog, en / nl):
  - `Offline` → "Needs a connection." / "Verbinding nodig.";
  - `Duplicate` → "We could not duplicate this trip. Try again." / "We konden deze reis niet dupliceren. Probeer het opnieuw.";
  - `Edit` → "Something went wrong on our side. Try again in a moment." / "Er ging iets mis bij ons. Probeer het zo opnieuw.".
  - `onPresented` increments a Debug-only "was presented" counter (`TripActionsDebugCounters.failurePresented`, in `src/debug`; AGENTS Tests 8). No haptic (iOS has none).
- Recreation: `openingWizard` and `actionFailure` live in the ViewModel and survive rotation. A process death mid-prepare loses the in-flight load; the sheet route reopens idle. If `startTrip` had already written the row, 9c's quiet recovery opens the wizard on the next shell start (iOS behaves the same).

### 5.2 Wizard entry for Edit and Duplicate (`:app` + `:feature:wizard`)

**`WizardLauncher` additions** (9c's `…/app/wizard/WizardLauncher.kt`, edited; it implements `TripWizardPreparer`):
- `override suspend fun prepare(tripId, action)`:
  1. `Ignored` if `holder.state.overlay != null`, or an open/prepare is pending (9c's `AtomicBoolean`, set here for the duration), or the session's `TripWizardDependencies` is null.
  2. `withContext(dispatchers.io) { deps.startTrip(tripId, if (action == TripAction.Edit) TripWizardAction.edit else TripWizardAction.duplicate) }` (9b; fetches `GET trips/{id}/input`, `loadTrip`, `replaceRecovery`). No generation service is created here (9b creates one only when the ViewModel builds the model), so nothing needs cancelling on failure.
  3. Success → `Ready`. `DataFailure.Offline` → `Failed(offline = true)`. Any other `Exception` (other `DataFailure`s, a `loadTrip` shape failure, a recovery write failure) → `Failed(offline = false)`. `CancellationException` rethrows.
  4. Reset the pending flag in `finally`.
- The `push` happens in the trip page after the sheet hid (§5.1), through the existing `holder.push(Route.Wizard(mode))`.

**Overlay slot** (`AppShell`, 9c §5.8, edited):
- Render `WizardRoot` for **every** `Route.Wizard(mode)`; drop the `placeholder(route)` branch for `Edit`/`Duplicate`.
- The overlay `BackHandler` condition becomes `enabled = overlay != null && overlay !is Route.Wizard` (the wizard handles its own back in every mode).

**`TripWizardViewModel` start** (9b `init`, edited) [R, OQ-9h-1]:
- `Create` → `deps.start()` (unchanged).
- `Edit(id)` / `Duplicate(id)` → first `deps.recover()` (add `suspend fun recover(): MemberWizardDraft?` to `TripWizardDependencies`, implemented by `MemberWizardStore.recover()`). Accept the recovered draft when it matches the entry:
  - `Edit(id)`: `draft.state.edit?.tripID == id`;
  - `Duplicate(id)`: `draft.state.edit == null && draft.state.copiedRoute != null`.
- Otherwise (no row, expired, or a different wizard) fall back to `deps.startTrip(id, action)` exactly as 9b does today (so 9b's `editAndDuplicateLoadTripInput` and `editLoadFailureIsExposed` stay green). This is what a restored overlay route after process death hits.
- **`LoadFailed` for Edit/Duplicate** (only reachable through the fallback): no board exists, so the wizard closes quietly with `WizardExit.Trips` (the trip page under the overlay shows again) and logs once at debug level [R, OQ-9h-4]. `Create` keeps 9c's handling.

**Exit targets** (no new `WizardExit` case [R, OQ-9h-5]):
- × / Close / discard / edit failure Close → `WizardExit.Trips` = clear the overlay. The wizard was pushed over the trip page, so the trip page shows again (board `exitHref` for edit = `Plan.dc.html`).
- Save (edit and duplicate) → 9g's `WizardExit.Saved(tripId)` → `ShellNav.wizardSaved(id)`: overlay cleared, My trips selected, Trips stack `[Trips, TripPage(id, awaitingSync = true)]` (iOS `tripsPath = [savedTrip]`, `AppTabs.swift:176`). For an edit `id` is the edited trip's id (the `PUT` returns it).

### 5.3 Edit chrome, back and discard

Most of this is already driven by 9a/9c. Verify, and add only what is missing:

| Item | Rule | Source |
|---|---|---|
| Nav title (`wizard.nav-title`) | `isEditing` → "Edit trip" / "Reis bewerken"; Duplicate → "New trip" / "Nieuwe reis" | 9c §5.1; iOS `MemberWizardSteps.swift:131–137` |
| Progress | Initial edit path: 1.0 on Checking, its failures and the first Ready (`loadTrip` sets `progressFloor = 1.0`, OQ-9a-3; boards pct 100). After a revisit from Ready, `go()` clears the floor for input steps (`MemberWizardModel.swift:98–112`), so Coming back? shows the step weight 0.45 (= `WizardReturnWeb` pct 45, no difference) and a later Checking/Ready shows 0.52–0.70/0.95. Nothing to add: 9a's model already does this | 9a; iOS `MemberWizardModel.swift:107–110`, `MemberWizardReview.swift:28–46` |
| Back chevron | Hidden on edit Ready, edit Checking (always), edit Checking failure, Duplicate's first Times (9a `firstScreen`) | board l.258–260 |
| System back on a first screen | Acts as × (OQ-9a-5) | 9c §5.4 |
| × on any edit step | `hasChanges` → discard alert; else close at once (unchanged edit = `originalInput == inputFingerprint`) | iOS `MemberWizardSteps.swift:143–149`; board `closeHref` |
| Footer Close on an edit Checking failure | Same as × (9f: `vm.requestClose()`) | iOS l.69–80 |
| Footer Close on edit Excluded | Close directly, never the alert (9e) | iOS l.71 |
| No-credits Close on edit Ready | `!hasChanges` → close; else discard alert (EditReturnNoCredits has changes → alert) | iOS l.111 |
| Back from a revisited edit step | The model's `navigationPath` (e.g. Coming back? → Times), **not** the board's "back to Ready" (`backHref` l.262). iOS behaviour wins; record it | `MemberWizardNavigation.swift:27–56` |

**Discard alert** (9c's `CfAlertDialog`, tag `wizard.discard`, edited to read the mode) [F `MemberWizardView.swift:125–136`]:

| Mode | Title | Text | Confirm (destructive) | Dismiss |
|---|---|---|---|---|
| Edit (`isEditing`) | "Discard your changes?" / "Je wijzigingen weggooien?" | "Your trip stays as it was." / "Je reis blijft zoals hij was." | "Discard changes" / "Wijzigingen verwijderen" | "Keep editing" / "Verder bewerken" |
| Create, Duplicate | 9c's "Discard this trip?" / "What you entered is not saved." / "Discard" / "Keep editing" | | | |

The nl confirm label is the iOS catalog's "Wijzigingen verwijderen", not the board's "Wijzigingen weggooien" (copy comes from the catalog; iOS's own test asserts it). Confirm → 9c `confirmDiscard()` → `discard()` (deletes the recovery row; the trip on the server is untouched) → `WizardExit.Trips`. Buttons stack vertically when either label does not fit side by side (M3 `AlertDialog` does this; board grid is one column here because "Discard changes" > 12 chars).

### 5.4 Edit Ready (`ReadyUiMapper` + 9g composables)

Same layout as 9g §5.1; only values change. Every rule is a pure mapping in `ReadyUiMapper` (JVM-tested), keyed on `isEditing`, `originalForm?.end?.kind`, `form.end.kind` and `review.price`.

**Title:** `isEditing` → "Edit your trip" / "Je reis bewerken"; else 9g's "Ready to plan".

**Price value** [F `MemberWizardReadyView.swift:157–166`; order matters]:
1. `isEditing && price.cost == 0` → "Free" (this check comes **before** `unlimited`, so an unlimited account editing for free shows "Free");
2. `price.unlimited` → "Included in your subscription.";
3. `cost == 0` → "Free"; `cost == 1` → "{{count}} credit"; else "{{count}} credits".

**Price block lines** (9g §5.2 table, plus the edit rows; in this order) [F l.173–215; board l.447–455]:

| # | Line | Shown when | Style |
|---|---|---|---|
| 0 | `["Price" + " "] + **value**` | always; prefix omitted when `credits == unlimited` | 9g |
| 1 | free-trip line | `credits == free && !isEditing` | 9g |
| 2 | one-free line | `credits == oneFree` | 9g |
| 3 | "You have {{count}} credits" | `!price.unlimited && price.cost > 0` | 9g (destructive 600 when `unavailable`) |
| 4 | "Removing a flight doesn't give a credit back." / "Een vlucht verwijderen levert geen credit op." | `isEditing && originalForm.end.kind == "return_legs" && form.end.kind != "return_legs"` | `wizardHint`, `mutedForeground` |
| 5 | "Adding a return flight costs 1 credit, because credits are per direction. Other changes are free." / "Een terugvlucht toevoegen kost 1 credit, want credits gelden per richting. Andere wijzigingen zijn gratis." **or** "Changes recalculate the plan. Check-offs on steps that stay in your plan are kept." / "Wijzigingen berekenen het plan opnieuw. Afgevinkte stappen die in je plan blijven, blijven bewaard." | `isEditing`; the first when `form.end.kind == "return_legs" && originalForm.end.kind != "return_legs"`, else the second | same |

Add `PriceLine.NoRefund`, `PriceLine.EditReturnNote`, `PriceLine.EditNote` to 9g's sealed type. Never hard-code a balance (OQ-A18).

**Expected results per board** (Task 2 JVM table; values from the §5.6 seeds):

| Board | price (state, cost, balance), `canSave`, `saveBlockedReason` | `credits` | Line 0 | Lines | Footer |
|---|---|---|---|---|---|
| WizardEdit | paid, 0, 3, true, null | free | Price **Free** | EditNote | Primary "Update my plan", **disabled** (`hasChanges == false`) |
| WizardEditChanged | edit, 1, 3, true, null | credits | Price **1 credit** | Balance(3); EditReturnNote **only if** the baseline had no return — the seed's baseline has one, so **EditNote** (board draws EditReturnNote; OQ-9h-6) | Primary, enabled iff `hasChanges` (seed: true) |
| WizardEditNoReturn | paid, 0, 3, true, null | free | Price **Free** | NoRefund, EditNote | Primary, enabled |
| WizardEditReturn | edit, 1, 3, true, null | credits | Price **1 credit** | Balance(3), EditReturnNote | Primary, enabled |
| WizardEditReturnNoCredits | edit, 1, 0, false, "Not enough credits" | unavailable | Price **1 credit** | Balance(0, alert), EditReturnNote | **CloseOnly** (`wizard.ready.no-credits`) |
| WizardEditWeb | paid, 0, 3, true, null | free | Price **Free** | EditNote | Primary, enabled (seed changes `seatWindow`) |

**Button label** (9g §5.5 table, last row): `isEditing` → "Update my plan" / "Mijn plan bijwerken" (add `ReadyButtonLabel.Update`); "Checking…", "Try again in {{count}} s", "Try again" unchanged. `enabled` is 9g's rule; `canSave` already contains `(!isEditing || hasChanges)` (9b). Tap → `Medium` haptic → `makePlan()` (9g).

**Rows** (9g): unchanged mapping. The Return row value comes from `WizardPresentation.returnSummary(form, stayEnd = review?.summary?.stayEnd ?: state.snapshotStayEnd, …)`, which gives "9 nights · until Wed 21 Oct" (`{{nights}} · until {{date}}` with `{{count}} nights`) for `end.kind == nights` and "Check-out · {{date}}" for `check_out`. Verify 9g passes `snapshotStayEnd` as the fallback.

**Banner slot, branch 2** (9g §5.3, now built) [F l.19–28]:

| Priority | Condition | Tone | Title | Body | Tag |
|---|---|---|---|---|---|
| 2 | `saveFailure == null && state.sourceChange != null && summary != null` | Info | "Updated" / "Bijgewerkt" | `WizardPresentation.sourceChangeSummary(change, review, stayEnd = review?.summary?.stayEnd ?: snapshotStayEnd, language, t)` | `wizard.ready.notice` |

`sourceChangeSummary` returns null when `change.price == null`; then fall through to branch 3. It throws only through `extrasSummary`; treat a throw as null (iOS `try?`), never `CancellationException`. Build `t` the way 9g's mapper builds it for `flightSummary` (catalog lookup in the wizard language). Add `ReadyBannerUi.SourceChanged(body: String)` (a formatted string is acceptable here because the summary is assembled from up to six catalog sentences by the 9a port; note it in the PR). Board sentence for `WizardEditChanged`: "On another device: the return flight moved to Thu 22 Oct. The price is still 1 credit." / "Op een ander apparaat: de terugvlucht is verplaatst naar do 22 okt. De prijs is nog steeds 1 credit."

**Edit save-failure banner** (9g §5.4, edit bodies; no board → `_noBoard` captures) [F l.274–300]:

| Kind | Title | Edit body (en / nl) |
|---|---|---|
| offline | "Saving needs a connection" | "Connect to the internet and try again. If you close now, your trip stays as it was." / "Maak verbinding met internet en probeer het opnieuw. Sluit je nu, dan blijft je reis zoals hij was." |
| server | "We couldn't save your plan" | "Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was." / "Er ging iets mis bij ons. Probeer het zo opnieuw. Sluit je nu, dan blijft je reis zoals hij was." |
| throttle | unchanged (9g) | unchanged |

### 5.5 Coming back? web choice and Duplicate's Times

**Web choice** (9d's `WizardReturnChoiceStep`, edited) [F `MemberWizardSteps.swift:201–236`; board l.99, l.362–364]:
- When `form.end.kind` is `nights` or `check_out`, add a **first** `WizardChoiceCard` (same metrics as 9d §5.8), tag `wizard.choice.web`, `selected = true`, `role = RadioButton`:
  - title: `nights` → "Ends after {{count}} nights" / "Eindigt na {{count}} nachten" (`count = form.end.nights ?: 0`); `check_out` → `WizardPresentation.returnSummary(form, stayEnd = review?.summary?.stayEnd ?: snapshotStayEnd, …)` ("Check-out · Wed 21 Oct");
  - hint: "Set on the website. Keep it, or add your flight home." / "Ingesteld op de website. Laat het zo, of voeg je vlucht naar huis toe.";
  - tap → `Selection` haptic → `vm.chooseReturn(form.end.kind)` (9a keeps the nights/check-out end and runs `finishFlightInput` after 250 ms).
- One way stays `selected = end.kind == "one_way"` (so false here); Add my return flight unchanged (9d).
- **Edit hint on Add my return flight:** `isEditing && originalForm?.end?.kind != "return_legs"` → "We plan the shift back home too · 1 credit" / "We plannen ook de verschuiving terug naar huis · 1 credit"; else 9d's hint.
- The app never creates a `nights`/`check_out` end; the card appears only for a loaded web trip (`06 §3.8`).

**Duplicate subtitle** (9d's Times step, edited) [F `MemberWizardView.swift:151–153`]:
- `!home && state.copiedRoute != null && !isEditing` → "Copied from {{route}}. Choose the new dates." / "Gekopieerd van {{route}}. Kies de nieuwe data." with `route = state.copiedRoute`; else 9d's "Times are local at each airport.". This holds on every visit to Times while `copiedRoute` is set (iOS parity).
- Add `Modifier.testTag("wizard.subtitle")` to 9c's subtitle `Text` if it has no tag (additive).
- `copiedRoute` is built by 9a from city names (`fromCity ?: fromIata` → `toCity ?: toIata`); it is model data, shown verbatim.
- Duplicate never shows the "From your last trip" hint (9a sets `fromLastTrip = false`).
- Coming back? is preselected by the model when the original was `return_legs` (9a keeps it in `clearDuplicateDates`).

### 5.6 Board seeds (`…/fwd/`, debug only)

Port `App/Debug/TripWizardActionsBoardSeed.swift` as `TripWizardActionsBoardSeed` (Kotlin `object`), applied after 9g's `WizardCompletionBoardSeed`, exactly as iOS dispatches (`WizardBoardCaptureSeed.swift:208`). Suffixes `Dark`/`NL`/`DarkNL` are stripped first (9c rule).

Boards: `WizardEdit`, `WizardEditChanged`, `WizardEditNoReturn`, `WizardEditReturn`, `WizardEditReturnNoCredits`, `WizardEditWeb`, `WizardCheckingEdit`, `WizardCheckOfflineEdit`, `WizardCheckServerEdit`, `WizardExcludedEdit`, `WizardDiscardEdit`, `WizardDuplicate`, plus Android `WizardReturnWeb` (below) and the `_noBoard` names `WizardEditSaveOffline`, `WizardEditSaveServer`.

`apply(original, board)` [F l.15–99], with `addingReturn = board in {EditReturn, EditReturnNoCredits, DiscardEdit}` and `returning = addingReturn || board == EditChanged`:
1. `draft = WizardCompletionBoardSeed.apply(original, "WizardReady")`.
2. If `returning`: `form.addReturn()`; home leg (`index = returnBoundary`): `toIata = "AMS"`, `toCity = "Amsterdam"`, `departLocal = "2026-10-22T10:25"`, `arriveLocal = "2026-10-22T15:30"`.
3. `WizardDuplicate`: `form.clearDuplicateDates()`; `copiedRoute = "Amsterdam → " + (airports["NRT"]?.city ?: "Tokyo")` (nl seed city "Tokio"); `step = outbound`; `review = null`; return.
4. `review.summary.planEndDate` = `returning` ? "2026-10-26" : EditWeb ? "2026-10-22" : "2026-10-14". If `returning`, `summary.line.key` = "We shift your clock :shift earlier, starting :count day before you leave. Your trip home is planned too.|We shift your clock :shift earlier, starting :count days before you leave. Your trip home is planned too.".
5. `baseline = form`; `addingReturn` → `baseline.removeReturn()`; `EditNoReturn` → `baseline.addReturn()` and `baseline.legs[returnBoundary].toIata = "AMS"`; `EditWeb` → `form.end = (kind "nights", nights 9)`, `baseline = form`, `snapshotStayEnd = ApiStayEnd("2026-10-21", "2026-10-21T03:00:00Z")`, then `baseline.legs[0].seatWindow = "right"`; `EditChanged` → `baseline` home leg `departLocal = "2026-10-21T10:25"`, `arriveLocal = "2026-10-21T15:30"`.
6. `edit = TripEditContext("capture-trip", "capture-source")`; `firstTrip = false`; `profile = {}`; `originalForm = baseline`; `originalInput` = the fingerprint of the draft with `form = baseline`; restore `form`.
7. `review = <step 4 review>`; EditWeb → `review.summary.stayEnd = snapshotStayEnd`; `review.identity.sourceRevision = "capture-source"`; `form.identity = review.identity`; `price.state = returning ? edit : paid`; `price.cost = returning ? 1 : 0`; `price.balance = EditReturnNoCredits ? 0 : 3`; `canSave = board != EditReturnNoCredits`; `saveBlockedReason = EditReturnNoCredits ? "Not enough credits" : null`.
8. `progressFloor = 1.0` (Android, OQ-9a-3; iOS 0.95); `returnToReady = true`; `returnRouteConfirmed = returning`; `entered = hasChanges`.
9. `EditChanged` → `sourceChange = SourceChange(form, fields = [returnFlight], price, previousPrice = price)`.
10. Step: `ExcludedEdit` → `excluded`, `review.status = no_plan`, `summary = null`, `price = null`, `reasons = [CopyLine("This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule.")]` [R: the board draws the shift-work reason; iOS's seed uses the under-18 line, OQ-9h-7]; `CheckingEdit`/`CheckOfflineEdit`/`CheckServerEdit` → `checking`; else `ready`.

**Android additions** [R]:
- `WizardReturnWeb`: base draft (9c) with `form.end = (nights, 9)` and `step = returnChoice` (iOS l.125–131), **plus** the edit context of step 6 with `originalForm` = that form and `snapshotStayEnd` as EditWeb, so the nav title reads "Edit trip" and the Add-return hint carries "· 1 credit" as the board draws (iOS's seed omits the edit context, OQ-9h-8). Leave `progressFloor = null` (a revisited input step has no floor, as after iOS `go()`), so the bar shows 0.45 = board pct 45.
- `WizardEditSaveOffline` / `WizardEditSaveServer`: the `WizardEditReturn` draft with `saveFailure = offline` / `server`.
- `WizardDiscardEdit`: 9c's `discardDialogVisible = true` (iOS `discardConfirmation`).

**Freeze** (9f's `WizardUiOverride`, set by `WizardDebugSeeds` in `app/src/debug`; no model method): every board above is frozen (`reviewing = false`, `retrySeconds = 0`); `WizardCheckingEdit` uses 9f's frozen checking progress for `WizardChecking`; `WizardCheckOfflineEdit` → the override's checking failure `Offline`; `WizardCheckServerEdit` → `Server(500)` (iOS l.219–225). `WizardDuplicate` and `WizardReturnWeb` are non-completion steps: use the same override/in-memory store as 9c/9d boards.

### 5.7 Haptics (through `LocalHaptics`)

| Event | `HapticEvent` |
|---|---|
| Web choice tap | Selection (9d's choice rule) |
| "Update my plan" tap | Medium (9g) |
| Edit save failure | Error (9g's `saveFailure` rule, unchanged) |
| Edit saved | Success (9g) |
| Trip actions prepare / failure | none (iOS) |

### 5.8 Copy (all from generated resources; `Text` receives resources only)

Wizard catalog (`Kit/TripWizardUI/Resources/Localizable.xcstrings`), verified at the pin:

| Key | nl |
|---|---|
| Edit trip | Reis bewerken |
| Edit your trip | Je reis bewerken |
| Update my plan | Mijn plan bijwerken |
| Free | Gratis |
| Removing a flight doesn't give a credit back. | Een vlucht verwijderen levert geen credit op. |
| Changes recalculate the plan. Check-offs on steps that stay in your plan are kept. | Wijzigingen berekenen het plan opnieuw. Afgevinkte stappen die in je plan blijven, blijven bewaard. |
| Adding a return flight costs 1 credit, because credits are per direction. Other changes are free. | Een terugvlucht toevoegen kost 1 credit, want credits gelden per richting. Andere wijzigingen zijn gratis. |
| Updated | Bijgewerkt |
| On another device: the route changed to {{from}} → {{to}}. | Op een ander apparaat: de route is gewijzigd naar {{from}} → {{to}}. |
| On another device: the outbound flight changed to {{flight}}. | Op een ander apparaat: de heenvlucht is gewijzigd naar {{flight}}. |
| On another device: the return flight moved to {{date}}. | Op een ander apparaat: de terugvlucht is verplaatst naar {{date}}. |
| On another device: the trip end changed to {{end}}. | Op een ander apparaat: het einde van de reis is gewijzigd naar {{end}}. |
| On another device: the sleep times changed to {{times}}. | Op een ander apparaat: de slaaptijden zijn gewijzigd naar {{times}}. |
| On another device: the trip now uses your usual sleep times. | Op een ander apparaat: de reis gebruikt nu je gewone slaaptijden. |
| On another device: the extras changed to {{extras}}. | Op een ander apparaat: de extra’s zijn gewijzigd naar {{extras}}. |
| The price is still {{price}}. / The price is now {{price}}. | De prijs is nog steeds {{price}}. / De prijs is nu {{price}}. |
| Copied from {{route}}. Choose the new dates. | Gekopieerd van {{route}}. Kies de nieuwe data. |
| Ends after {{count}} nights | Eindigt na {{count}} nachten |
| Set on the website. Keep it, or add your flight home. | Ingesteld op de website. Laat het zo, of voeg je vlucht naar huis toe. |
| We plan the shift back home too · 1 credit | We plannen ook de verschuiving terug naar huis · 1 credit |
| {{nights}} · until {{date}} / {{count}} nights (+ `_one`) / Check-out · {{date}} | {{nights}} · tot {{date}} / {{count}} nachten / Uitchecken · {{date}} |
| Checking your changes | We bekijken je wijzigingen |
| Connect to the internet and try again. If you close now, your trip stays as it was. | Maak verbinding met internet en probeer het opnieuw. Sluit je nu, dan blijft je reis zoals hij was. |
| Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was. | Er ging iets mis bij ons. Probeer het zo opnieuw. Sluit je nu, dan blijft je reis zoals hij was. |
| Your trip stays as it was. | Je reis blijft zoals hij was. |
| Discard your changes? | Je wijzigingen weggooien? |
| Discard changes | Wijzigingen verwijderen |
| Keep editing | Verder bewerken |

PlanUI catalog (`Kit/PlanUI/Resources/Localizable.xcstrings`; Android table `plan`: `plan_needs_a_connection`, `plan_we_could_not_duplicate_this_trip_try_again`, `plan_something_went_wrong_on_our_side_try_208515`): "Needs a connection." / "Verbinding nodig."; "We could not duplicate this trip. Try again." / "We konden deze reis niet dupliceren. Probeer het opnieuw."; "Something went wrong on our side. Try again in a moment." / "Er ging iets mis bij ons. Probeer het zo opnieuw.".

Server `CopyLine`s (summary, reasons) go through the PlanCopy resolver (9d/9g). Look every name up in `tools/l10n/names.lock.json`. **No new string and no Android-only copy** in this phase; if a key above has no resource, stop and report (0c owns generation).

---

## 6. Kotlin shape [R]

| Class / function | Module, package | Notes |
|---|---|---|
| `TripWizardPreparer`, `TripWizardPrepareOutcome` | `:feature:trip`, `net.chronoflow.feature.trip.actions` | §5.1; no dependency on `:feature:wizard` or `:core:wizard` |
| `TripActionFailure { Offline, Duplicate, Edit }` | same | §5.1 |
| `TripPageViewModel` additions | `net.chronoflow.feature.trip.page` (6b file) | `startAction`, `actionFailure`, `wizardRequests: Flow<WizardMode>` (R3); `busy` into `TripActionsUi`; `preparer` as a defaulted constructor parameter (R3) |
| `TripActionsDebugCounters` | `feature/trip/src/debug/…/actions/` | `failurePresented: AtomicInteger`; absent in release |
| `WizardLauncher : TripWizardPreparer` | `:app`, `net.chronoflow.app.wizard` (9c file) | `prepare`; injects `AppDispatchers` |
| `TripWizardDependencies.recover()` | `:feature:wizard` (9b interface) + its `:app` implementation | §5.2 |
| `TripWizardViewModel` start change | `:feature:wizard` (9b file) | §5.2 matching rule, quiet close on `LoadFailed` for Edit/Duplicate |
| `PriceLine.NoRefund`, `PriceLine.EditReturnNote`, `PriceLine.EditNote`, `ReadyButtonLabel.Update`, `ReadyBannerUi.SourceChanged(body)`, `ReadyUi.title` (edit or create) | `:feature:wizard`, `…wizard.ready` (9g files) | additive |
| `WizardReturnStep` / `WizardReturnUi.web` web card | `…wizard.returnchoice` (9d files) | §5.5, R3 |
| Times subtitle rule | `WizardTimesUi` (`…wizard.times`, 9d) | §5.5, R3 |
| Discard alert copy per mode | already built (`WizardUiState.from`, `WizardBackHandling`) | R2; verify only |
| First-screen system back → `RequestClose` | `…wizard.shell` (`WizardBackHandling`) + a first-screen flag on `WizardUiState` | R3 gap |
| `TripWizardActionsBoardSeed` | `…/fwd/` | §5.6 |

UI models stay `@Immutable` raw values except `SourceChanged.body` (above).

---

## 7. Tasks (one commit each, in order; Conventional Commits, lower-case imperative, **no trailer**)

Tick the plan box named in brackets in the same commit. Plan boxes (l.2982, l.2987, l.2988): "**Edit**, from the trip actions sheet", "**Duplicate**", "**Tests.**".

### Task 1: `feat(wizard): open edit and duplicate wizards from a prepared row` [part of "Edit"]

- Files: 9b's `TripWizardDependencies` (+ `recover()`), its `:app` implementation, `TripWizardViewModel` start (§5.2), `AppShell` overlay slot and `BackHandler` condition (§5.2).
- Edge cases: a recovered draft for another trip id → fallback `startTrip`; a recovered Create draft under `Duplicate(id)` (`copiedRoute == null`) → fallback; `LoadFailed` for Edit/Duplicate → `Closed(null)`-equivalent exit `Trips` once.
- **Tests (JVM, `…/fwt/TripWizardViewModelEntryTest`, fakes, `runTest(timeout = 60.seconds)`):**
  - `editUsesThePreparedRowWithoutReloading`: fake `recover()` returns an edit draft for `"t1"` → `Active`, `startTrip` call count 0, `isEditing`.
  - `editFallsBackToStartTripForAnotherTrip`: recovered edit draft for `"t2"` under `Edit("t1")` → `startTrip("t1", edit)` once.
  - `duplicateAcceptsOnlyACopiedDraft`: recovered Create draft → `startTrip(id, duplicate)` once; recovered draft with `copiedRoute` and no edit → used.
  - `editLoadFailureClosesQuietly`: no row, `startTrip` throws `Offline` → the exit recorder sees `Trips` once.
  - 9b's `TripWizardViewModelTest` stays green unchanged.
- **Robolectric `…/appt/shell/WizardOverlayModesTest` (`@Logic`):** `push(Route.Wizard(Edit("t1")))` with a fake deps whose `recover()` returns the `WizardEdit` seed renders `wizard.ready` (no placeholder node); `activity.onBackPressedDispatcher.onBackPressed()` on edit Ready (unchanged) closes the overlay and leaves the Trips stack unchanged.

### Task 2: `feat(wizard): map the edit ready price, banner and button` [part of "Edit"]

- Files: 9g's `ReadyUi.kt`, `ReadyUiMapper.kt` (+ the edit rules of §5.4), the banner branch 2, the edit save-failure bodies.
- **Tests (JVM, `…/fwt/ready/`):**
  - `ReadyEditPriceUiTest` (parameterised, one case per §5.4 board row, built from the §5.6 seeds through `TripWizardActionsBoardSeed.apply` — the seed lives in `src/debug`, so this test lives in `src/testDebug`): title, `credits`, line 0 value and prefix, the ordered `lines`, footer type, label `Update`, `enabled`.
  - `editUnlimitedFreeShowsFreeWithoutPriceLabel`: editing, `price = (edit, 0, 3, unlimited = true)` → value Free, no "Price " prefix (iOS order).
  - `editFreeNeverShowsTheFreeTripLine`: editing with `credits == free` → no `FreeTrip` line.
  - `ReadySourceChangeBannerTest`: the EditChanged seed → `SourceChanged` body equals "On another device: the return flight moved to Thu 22 Oct. The price is still 1 credit." (en) and the nl sentence of §5.4; save failure beats it; `change.price == null` falls through to `Adjusted`/`Updated`; a different previous cost → "The price is now 1 credit."; a `[route, sleep]` change with override off → "On another device: the route changed to AMS → NRT. On another device: the trip now uses your usual sleep times. The price is still Free." (cost 0).
  - `ReadyEditSaveFailureCopyTest`: editing + offline/server → the edit bodies (en, nl); create keeps 9g's bodies.

### Task 3: `feat(wizard): show edit ready and the edit discard alert` [ticks "Edit" with Tasks 1, 2, 4, 6]

- Files: 9g's `WizardReadyStep` (title, notes, banner), 9c's discard dialog (mode copy), any chrome item of §5.3 the merged code lacks.
- **Tests (Robolectric `@Logic`, `…/fwt/edit/TripWizardActionsScreenTest`** — port of iOS `TripWizardActionsScreenTests`, Swift names kept; seeded with the §5.6 drafts through 9c's `WizardStartOverride`, nl unless stated):
  - `testUntouchedEditHasDisabledUpdateButton`: `wizard.ready.continue` exists and `assertIsNotEnabled()`; `wizard.title` text "Je reis bewerken"; `wizard.back` does not exist.
  - `testEditNoCreditsHasCloseWithoutBilling`: `wizard.ready.no-credits` has a click action; `wizard.ready.continue` does not exist; `onAllNodesWithText("Credits kopen op de website").assertCountEquals(0)`.
  - `testEditDiscardUsesNativeAlert`: `wizard.discard` shows with "Wijzigingen verwijderen" and "Verder bewerken"; confirm → the exit recorder sees `Trips` once and the recovery fake saw `discard` once.
  - (Android) `untouchedEditClosesAtOnce`: click `wizard.close` on `WizardEdit` → no `wizard.discard`, exit `Trips` once.
  - (Android) `changedEditAsksBeforeClosing`: `WizardEditReturn` + `wizard.close` → `wizard.discard` with the edit title; "Keep editing" keeps `wizard.ready`.

### Task 4: `feat(wizard): add the web end choice and the duplicate subtitle` [ticks "Duplicate" with Task 7]

- Files: 9d's Coming back? step (web card, edit hint), Times subtitle mapping, 9c's subtitle tag (if missing).
- **Tests:**
  - JVM `…/fwt/returnchoice/ReturnChoiceUiTest`: `nights 9` → a web card first, selected, title count 9; `check_out` → title from `returnSummary` ("Check-out · Wed 21 Oct" with the seed's stay end); `one_way` → no web card; edit with a one-way baseline → add-return hint "· 1 credit"; edit with a `return_legs` baseline or create → plain hint.
  - JVM `…/fwt/outbound/TimesSubtitleTest`: `copiedRoute != null && !isEditing && !home` → the copied subtitle with the route; home → 9d's home title and sub; editing with `copiedRoute` set → default sub.
  - Robolectric `@Logic` in `TripWizardActionsScreenTest`: `testDuplicateOpensOutboundWithEmptyDates` (Swift name): `wizard.outbound.continue` `assertIsNotEnabled()`; `wizard.0.departure.date` exists and shows the empty placeholder; `wizard.subtitle` text "Gekopieerd van Amsterdam → Tokio. Kies de nieuwe data."; `wizard.back` does not exist; `wizard.nav-title` "Nieuwe reis". Plus `webChoiceKeepsTheWebEnd`: on `WizardReturnWeb` click `wizard.choice.web`, advance the injected clock 250 ms → the model's `end.kind == "nights"` and step left `returnChoice`.

### Task 5: `feat(trip): prepare the wizard from the trip actions sheet` [part of "Edit"]

- Files: `…/ft/actions/TripWizardPreparer.kt`, `TripActionFailure.kt`, `TripPageViewModel` additions, `TripPageSheets` `TripActions` branch (§5.1), `feature/trip/src/debug/…/TripActionsDebugCounters.kt`.
- **Tests:**
  - JVM `…/ftt/actions/TripActionsPrepareTest` (`Dispatchers.setMain(StandardTestDispatcher())`, a gated fake preparer):
    - `busyWhilePreparingAndClearedAfter`: `startAction(Edit)` → `tripActions.value.availability.busy == true` until the gate opens → false;
    - `readyEmitsOneWizardRequest`: `Ready` → exactly one `Route.Wizard(WizardMode.Edit("t"))`; Duplicate → `WizardMode.Duplicate("t")`;
    - `failuresMapToTheIosCopy`: `Failed(offline = true)` → `Offline` for both actions; `Failed(false)` → `Duplicate` for duplicate, `Edit` for edit;
    - `ignoredDoesNothing`: no request, no failure;
    - `secondTapWhileBusyIsIgnored`: two `startAction` calls → one `prepare`;
    - `offlineOrNotAllowedNeverPrepares`: `online = false` or `canEdit = false` → zero `prepare` calls;
    - `openingActionsClearsTheFailure`.
  - Robolectric `@Logic` `…/ftt/actions/TripActionsBusyTest`: with `busy = true`, `trip.actions.edit`, `trip.actions.duplicate` and `trip.actions.cancel` are not enabled, and pressing back leaves `trip.actions` shown; with a failure set, `TripActionsDebugCounters.failurePresented` becomes 1 and `trip.actions.failure` shows the nl text. If Robolectric cannot render the sheet window, move both checks to Task 10's emulator class and record it (AGENTS Tests 6).

### Task 6: `feat(app): load the trip input before opening edit or duplicate` [part of "Edit"]

- Files: 9c's `WizardLauncher` (+ `prepare`), the wiring of the preparer into the trip page's dependency holder.
- **Tests (JVM, `…/appt/wizard/WizardLauncherPrepareTest`, fakes, `AppDispatchers` test dispatchers):**
  - `prepareCallsStartTripOnIoAndReturnsReady` (records the dispatcher);
  - `offlineMapsToOfflineFailure`; `serverAndShapeFailuresMapToOtherFailure` (`DataFailure.Server(500)` and an `IllegalStateException` from the fake);
  - `cancellationIsRethrown`;
  - `ignoredWhileAnOverlayIsOpenOrPending` (overlay set; a second `prepare` while the first is gated);
  - `noSessionIsIgnored`.

### Task 7: `feat(debug): seed the edit and duplicate boards` [part of "Tests"]

- Files: `…/fwd/TripWizardActionsBoardSeed.kt`, the dispatch in 9c's `WizardBoardCaptureSeed`, the freeze entries in `WizardDebugSeeds` (§5.6).
- **Tests (JVM, `src/testDebug`, `…/fwt/debug/TripWizardActionsBoardSeedTest`, one parameterised case per board):** step; `isEditing` (false only for Duplicate); `edit?.sourceRevision == "capture-source"`; `hasChanges` (true for `EditChanged`, `EditNoReturn`, `EditReturn`, `EditReturnNoCredits`, `EditWeb`, `DiscardEdit` and the two `EditSave*` drafts; **false** for `Edit`, `CheckingEdit`, `CheckOfflineEdit`, `CheckServerEdit`, `ExcludedEdit` and `ReturnWeb`, whose baseline equals the form in the iOS seed l.42–67; not asserted for Duplicate); `credits`; `sourceChange?.fields == [returnFlight]` only for EditChanged; `form.end.kind`; for Duplicate: every leg's `departLocal`/`arriveLocal` empty, `review == null`, `copiedRoute == "Amsterdam → Tokyo"` (en) / "Amsterdam → Tokio" (nl), `progress == 0.22`; for ExcludedEdit: status `no_plan`, reason key = the shift line; for ReturnWeb: `nights == 9`, step `returnChoice`, `isEditing`, `progressFloor == null`, `progress == 0.45`.

### Task 8: `test(wizard): guard and capture the edit and duplicate boards` [part of "Tests"]

- **`WizardEditGuardsTest`** (`@Layout`, parameterised, one test case per parameter set; 0e `Guards.assertAll(rule, profile, headingTags = setOf("wizard.title"))` on `WizardRoot` with the frozen seeds):

  | Cases | States | Matrix | Count |
  |---|---|---|---|
  | (a) | `Edit`, `EditChanged`, `EditNoReturn`, `EditReturn`, `EditReturnNoCredits`, `EditWeb`, `ReturnWeb`, `CheckingEdit`, `CheckOfflineEdit`, `CheckServerEdit`, `ExcludedEdit`, `DiscardEdit`, `Duplicate` (13) | `small`, `pixel8`, `foldInner`, `largeFont` × en, nl × light, dark | 208 |
  | (b) | `EditSaveOffline_noBoard`, `EditSaveServer_noBoard` | the 4 profiles × en, nl, light | 16 |
  | (c) | `EditReturnNoCredits`, `EditChanged` | `landscapeSideNav`, nl, light and dark | 4 |

  Extra assertions (port of iOS `TripWizardActionsLayoutTests` l.18–51: iOS profiles `se` and `duo-asymmetric` at xxLarge → Android `small` and `landscapeSideNav` with `largeFont`): for `EditReturnNoCredits` and `EditChanged`, nl, the bounds of `wizard.ready.footer` lie vertically inside the profile's safe area (tolerance 1 px); for `EditReturnNoCredits` only (the one with a no-credits footer), `wizard.ready.no-credits` also lies inside `wizard.ready.footer`. For `DiscardEdit`, guard the dialog only if Robolectric renders the dialog window; otherwise record it and rely on the device capture (AGENTS Tests 6). Record the class run time in the PR.
- **`WizardEditCaptureTest`** (`@Capture`, Robolectric, 390 × 844 dp, density 3, zero insets, 0e `BoardCapture.capture(rule, name)`; theme and language from the name): the 25 wrappers of §2.2 (`WizardEdit`, `WizardEditDarkNL`, `WizardEditChanged`, `WizardEditChangedDarkNL`, `WizardEditNoReturn`, `WizardEditNoReturnDarkNL`, `WizardEditReturn`, `WizardEditReturnNoCredits`, `WizardEditReturnNoCreditsDarkNL`, `WizardEditWeb`, `WizardEditWebDarkNL`, `WizardReturnWeb`, `WizardReturnWebDarkNL`, `WizardCheckingEdit`, `WizardCheckingEditDarkNL`, `WizardCheckOfflineEdit`, `WizardCheckOfflineEditDarkNL`, `WizardCheckServerEdit`, `WizardCheckServerEditDarkNL`, `WizardExcludedEdit`, `WizardExcludedEditDarkNL`, `WizardDiscardEdit`, `WizardDiscardEditDarkNL`, `WizardDuplicate`, `WizardDuplicateDarkNL`) plus `WizardEditSaveOffline_noBoard`, `WizardEditSaveServer_noBoard` (27). Spinners with `mainClock.autoAdvance = false`.
- **Trip actions captures** (extend 6d's `TripActionsCaptureTest`): `TripActionsBusy_noBoard_pixel8_en_light` (busy = true) and `TripActionsFailure_noBoard_pixel8_nl_light` (failure `Duplicate`, snackbar shown). Skip either if Robolectric cannot render the sheet window, and capture it on the device instead (Task 10).
- No stored Roborazzi references (opt-in only).

### Task 9: `test(app): open edit and duplicate from the trip actions sheet` [part of "Tests"]

Port of iOS `TripActionsScreenTests` at the cheapest tier that has the shell, the trip page and the wizard together: **Robolectric `@Logic` in `:app`** (as 9g's `WizardProcessDeathTest`), with the real `AppGraph`, a `MockDataSource` (`CF_MOCK_DELAY = 0`), the demo account signed in and a trip page on the Trips stack (seed through 4e's `DebugSeedRouting`: `CF_PLAN_TEST_ROUTE=trip:<id>`, `CF_WIZARD_BOARD_CAPTURE=TripActions`). The two ported iOS tests use iOS's id `0199a3c0-0000-7000-8000-000000000001` (`PlanUIFlow.tripID`, the active S1 trip); the other three use the upcoming `…0002` (hand-in 8). `MockDataSource` has no call recorder (4c): assert only observable state (UI, `tripInput` answers, the account DB), never "the mock recorded …".

`…/appt/wizard/TripActionsWizardEntryTest`:
- `testEditOpensMemberWizardFromActions`: click `trip.actions.edit` → `waitUntilExactlyOneExists(hasTestTag("wizard.title"), 4_000)`; `wizard.close` exists; `trip.actions` is gone. (The edit review's `source_revision` is 9b's `TripWizardActionsTests`; do not re-prove it here.)
- `testDuplicateOpensOutboundFromActions`: click `trip.actions.duplicate` → `wizard.outbound.continue` exists and is not enabled; `wizard.back` does not exist.
- `editLoadFailureKeepsTheSheetWithTheServerMessage`: the mock's `failNext(MockMethod.TripInput, DataFailure.Server(500))` → `trip.actions` still shown, `trip.actions.failure` with "Something went wrong on our side. Try again in a moment."; no `wizard.title`.
- `duplicateOfflineShowsNeedsAConnection`: `failNext(TripInput, Offline)` on Duplicate → "Needs a connection.".
- `editSaveReplacesTheStoredDetail` (the "new revision" proof): open Edit, wait for `wizard.ready`, change the preparation days through the model's `vm.wizard.editForm` test hook (or `performSemanticsAction(SetProgress)` on `wizard.preparation` after clicking `wizard.ready.extras` and returning with `wizard.fine-tune.continue`), wait until `wizard.ready.continue` is enabled, click it → `trip.page` shows; `mock.tripInput(<id>).sourceRevision` read after the save differs from the value read before the edit (the mock's `wizardSave` stores the new input under the same id, iOS `MockDataSource.swift:873–893`); the stored trip detail in the account DB differs from the one before the edit (compare its `plan.meta.inputHash`, or its `detailVersion` if the hash happens to match; record which); the Trips stack top is `TripPage(<id>, awaitingSync = true)`. Get the `MockDataSource` handle from the debug graph the test builds (find it with `grep -rn "MockDataSource(" app/src`).

Each test < 1 s warm (cold costs through `measureClassWarmup`). If the shell cannot be composed on Robolectric with the real graph, move only that test to Task 10's emulator class as a seeded single-screen check and record it.

### Task 10: `test(app): edit a trip from its actions and land on its page` [ticks "Tests"]

The feature's one emulator journey, in `…/appat/wizard/TripEditJourneyTest`, extends 0e `ScreenTest`, `@Smoke`, < 10 s, `timeout_msec=60000`, `waitUntil`/`waitUntilExactlyOneExists` only, never sleep.

- **`editFromActionsSavesAndOpensTheTripPage`.** Seeds: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_MOCK_DELAY=0`, `CF_PLAN_TEST_ROUTE=trip:<upcoming mock trip id>`, `CF_WIZARD_BOARD_CAPTURE=TripActions` (6d: opens the sheet once when the card exists). Steps:
  1. wait for `trip.actions.edit` enabled (4 s), click;
  2. wait for `wizard.ready` (5 s; Checking runs ≥ 1.2 s); assert `wizard.ready.continue` is not enabled (untouched edit);
  3. click `wizard.ready.extras`; wait for `wizard.preparation`; read its `ProgressBarRangeInfo.current` as `p` and `performSemanticsAction(SemanticsActions.SetProgress) { it(((p.toInt() + 1) % 5).toFloat()) }`;
  4. click `wizard.fine-tune.continue`; wait until `wizard.ready.continue` is enabled (5 s) and its text is "Update my plan"; click;
  5. wait for `trip.page` (5 s); assert `wizard.ready` and `wizard.calculating` no longer exist and the selected tab is My trips.
  Record the duration in the PR.
- If Task 5 or Task 8 moved checks here, add them as **seeded single-screen** methods (not journeys): `TripActionsBusyScreenTest` etc.
- If the worker sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:TripEditJourneyTest"`) and their exact output; the orchestrator runs them.

### Task 11: `docs(wizard): record edit and duplicate acceptance` [no box; ticks any remaining 9h box]

- `docs/wizard.md`: the edit price table (§5.4), banner branch 2, the trip actions prepare flow (§5.1), the seed table (§5.6), the capture list with the reviewer's board comparison (each difference fixed or recorded with its board name: at least EditChanged note EditNote vs board EditReturnNote, ExcludedEdit shift reason without the doctor line, DiscardEdit nl "Wijzigingen verwijderen", EditReturnNoCredits without the website button, back from revisited edit steps follows the model).
- `plan.md`: append OQ-9h-1 … OQ-9h-12 (except the resolved OQ-9h-10) to "Open questions for the user" as OQ-A450–OQ-A460 (mapping in R5); tick the Phase 9h boxes not yet ticked.

---

## 8. Acceptance criteria

All of these must pass and be pasted into the PR (command and the last lines of output):

1. `make quick` — green, < 60 s warm.
2. `make targeted ONLY="feature:wizard:TripWizardViewModelEntryTest feature:wizard:ReadyEditPriceUiTest feature:wizard:ReadySourceChangeBannerTest feature:wizard:ReadyEditSaveFailureCopyTest feature:wizard:ReturnChoiceUiTest feature:wizard:TimesSubtitleTest feature:wizard:TripWizardActionsScreenTest feature:wizard:TripWizardActionsBoardSeedTest feature:trip:TripActionsPrepareTest feature:trip:TripActionsBusyTest app:WizardOverlayModesTest app:WizardLauncherPrepareTest app:TripActionsWizardEntryTest"` — green.
3. `make targeted ONLY="feature:wizard:WizardEditGuardsTest"` — 228 cases green, each < 1 s warm.
4. `make captures ONLY=WizardEdit*,WizardReturnWeb*,WizardCheckingEdit*,WizardCheckOfflineEdit*,WizardCheckServerEdit*,WizardExcludedEdit*,WizardDiscardEdit*,WizardDuplicate*,TripActions*` — 27 wizard PNGs (+ the 2 trip actions `_noBoard` PNGs, or their device versions) in `build/captures/`; the reviewer compares each with its board.
5. `make check` with `pixel` booted — green, including `TripEditJourneyTest`; < 5 min warm; journey duration recorded.
6. 9a's and 9b's suites (including `TripWizardActionsTests`, `TripWizardViewModelTest`) and 9g's Ready/Save tests still green with no assertion edited.
7. `testReleaseUnitTest`: no `TripWizardActionsBoardSeed`, no `TripActionsDebugCounters` class in release; release `uiOverride` is null (9f's `ReleaseReadinessTest`, extended).
8. `git grep -n "Buy credits\|Credits kopen\|SetPaywall" -- feature app core` returns nothing in main sources.
9. Performance: `prepare` runs `startTrip` on `AppDispatchers.io` (asserted in Task 6); StrictMode is clean in logcat during Task 10's journey; the trip page timeline is not touched (no `make perf-timeline` needed — state in the PR that no timeline file changed).

---

## 9. Review checklist (Opus reviewer)

Behaviour:
- [ ] Edit and Duplicate load the input **before** the wizard opens; the actions sheet is busy meanwhile (rows and Cancel disabled; drag, scrim and back do not dismiss); the wizard route is pushed only after the sheet hid.
- [ ] Failure copy per iOS: offline → "Needs a connection." for both; other → duplicate "We could not duplicate this trip. Try again.", edit "Something went wrong on our side. Try again in a moment."; the failure clears when the sheet reopens or a new action starts; the snackbar host is inside the sheet.
- [ ] `TripWizardViewModel` uses the prepared row (no second `GET input` on the normal path); a mismatching row falls back to `startTrip`; a fallback failure closes quietly; nothing shows a loading or error screen without a board.
- [ ] Edit starts at Checking ("Checking your changes"), progress 100 %, no back chevron; then Ready "Edit your trip"; untouched edit: "Update my plan" disabled and × closes at once; changed edit: × asks "Discard your changes?".
- [ ] Every edit review and the `PUT` carry the current `source_revision` (9b); a changed source shows the "Updated" banner with `sourceChangeSummary` (branch 2, after save failure, before adjusted).
- [ ] Edit price: edit + cost 0 → "Free" before the unlimited check; no free-trip line on edits; NoRefund only when the baseline had a return and the form has none; EditReturnNote only when a return was added; else EditNote; no-credits → Close only, no website button.
- [ ] The web end card appears only for `nights`/`check_out`, first and selected, and keeps that end; the "· 1 credit" hint only when editing a trip whose baseline has no return.
- [ ] Duplicate: Times first screen (no chevron), empty pills, "Copied from … Choose the new dates.", nav "New trip", Continue disabled; × with `entered` asks "Discard this trip?"; save is a fresh `POST /v1/trips` (no `source_revision`) and opens the **new** trip.
- [ ] Edit save → `wizardSaved(<same id>)`: My trips selected, stack `[Trips, TripPage(id, awaitingSync = true)]`; Success haptic once; no automatic retry.
- [ ] Edit save-failure banners use the edit bodies; Excluded edit shows "Your trip stays as it was." and Close closes directly.

Android and code:
- [ ] `:feature:trip` does not depend on `:feature:wizard`/`:core:wizard`; the seam is `TripWizardPreparer`.
- [ ] Every string is a resource with en and nl from the iOS catalogs; no new Android-only copy; nl discard confirm is "Wijzigingen verwijderen".
- [ ] No hard-coded credit counts, colours or dp; all from tokens or 9g/9d metrics.
- [ ] `CancellationException` is rethrown in `prepare`, `startAction` and the VM start; no `runCatching` around suspend calls.
- [ ] Seeds and counters are debug-only; release has none.
- [ ] Tags: `trip.actions.failure`, `wizard.choice.web`, `wizard.subtitle`, plus the 9c/9g ids; Swift test names kept for the ported tests.
- [ ] Tests at the cheapest tier: price/banner/choice/subtitle logic on the JVM, screens on Robolectric, one emulator journey; no `Thread.sleep`; exact tags.

Typical mistakes to look for:
- Pushing `Route.Wizard(Edit)` straight from `onAction` (6d's seam) and letting the wizard VM load, which shows a blank overlay and an unboarded error.
- Calling `startTrip` twice (once in `prepare`, again in the VM), which replaces the row and drops a quiet recovery.
- Forgetting the overlay `BackHandler` change, so system back on edit Ready closes the overlay without the discard check.
- Showing "Wijzigingen weggooien" (board) instead of the catalog's "Wijzigingen verwijderen".
- Showing the free-trip line or "You have N credits" on a free edit; checking `unlimited` before the edit-free rule.
- Building a website/purchase button for `WizardEditReturnNoCredits`.
- A snackbar host on the page under the sheet (invisible behind the scrim).
- Testing the price mapping through the screen instead of the JVM mapper.

---

## 10. Open questions (proceed on the default; append each to `plan.md` with the `OQ-A` number in R5)

| # | Question | Default |
|---|---|---|
| OQ-9h-1 (OQ-A450) | 9b's ViewModel loads Edit/Duplicate itself (`startTrip` in `init`), which would show the wizard before the load and needs an unboarded loading/error state; iOS loads first, on the sheet. | Prepare on the sheet (`TripWizardPreparer` → `startTrip`), then push the route; the VM recovers the prepared row and falls back to `startTrip` only when the row does not match. |
| OQ-9h-2 (OQ-A451) | iOS shows the action failure as `FeedbackToast` inside the sheet. | M3 Snackbar inside the sheet (OQ-A5), `SnackbarDuration.Short`, tag `trip.actions.failure`; capture `TripActionsFailure_noBoard`. |
| OQ-9h-3 (OQ-A452) | The busy actions sheet has no board; iOS disables rows and Cancel with no spinner. | iOS: disabled rows ("Needs a connection." subtitle, as 6d draws disabled rows), Cancel disabled, no dismiss; capture `TripActionsBusy_noBoard`. |
| OQ-9h-4 (OQ-A453) | A failed in-wizard load (fallback path only, e.g. a restored overlay route after the 30-minute row expired) has no board (`06` OQ 9, OQ-9b-12). | Close the wizard quietly (`WizardExit.Trips`), log at debug level. |
| OQ-9h-5 (OQ-A454) | 9c expected 9h to add a `WizardExit` case for edit. Clearing the overlay already returns to the trip page under it (board `exitHref` Plan). | No new case; `Trips` = clear the overlay; save uses 9g's `Saved`. |
| OQ-9h-6 (OQ-A455) | `WizardEditChanged` draws the edit-return note; the iOS seed's baseline already has the return, so iOS shows the edit note. | iOS seed and rule (EditNote); record the difference. |
| OQ-9h-7 (OQ-A456) | `WizardExcludedEdit` draws the shift-work line plus "Your doctor can tell you what's safe when you travel."; the iOS seed uses the under-18 reason; the server sends one shift reason and no doctor line. | Seed the server's shift-work reason; no doctor line (server copy wins, as 9e); record it. |
| OQ-9h-8 (OQ-A457) | The iOS `WizardReturnWeb` seed has no edit context, so it shows "New trip" and the plain add-return hint; the board shows "Edit trip" and "· 1 credit". | Seed the edit context (board); the rule is iOS's. |
| OQ-9h-9 (OQ-A458) | Board back targets from revisited edit steps go to Ready (`backHref` l.262); iOS's `navigationPath` goes to the previous input step (Coming back? → Times). | iOS (9a's model, unchanged); record it. |
| OQ-9h-10 | (Resolved in the check.) Board `WizardReturnWeb` pct 45 vs the edit progress floor 1.0 (OQ-9a-3). | No conflict: iOS `go()` clears the floor on input steps, so a revisited Coming back? shows 0.45 = board. Seed it without a floor (§5.6); nothing to record or append to `plan.md`. |
| OQ-9h-11 (OQ-A459) | "Ends after {{count}} nights" has no singular in the iOS catalog ("Ends after 1 nights"). | Use the catalog string as is; report to the iOS session; nights < 2 is rare on web trips. |
| OQ-9h-12 (OQ-A460) | iOS checks `canEdit`/`canDuplicate` from the card; the plan and 6d keep that. A trip whose `tripInput` shape fails `loadTrip` (legs > 16, bad boundary) throws `Server(null)`. | Treated as "other" failure (edit: server copy; duplicate: duplicate copy). |
