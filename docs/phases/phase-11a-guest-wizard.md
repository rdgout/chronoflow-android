# Phase 11a: Guest wizard

- **Branch:** `feat/phase-11a-guest-wizard`. Start it from the top of the stack once **9g** (Ready and save) and **3c** (Welcome) are in it. Through 9g the stack also carries 9f, 9e, 9d, 9c, 9b, 9a (the whole wizard, guest branches of the model included), 6a, 5a (`Route.SignIn`, `sign-in.guest` → `Route.GuestWizard`), 4e (`RootNavigator`, `AppGraph`, `AccountWiper`, `GuestSecrets`, `SessionHooks`, `TestSeeds`), 4c (`MockDataSource` with the guest review **already ported**), 4b (`ApiDataSource.reviewGuestTrip` **already built**), 4a (`SessionCipher`, `DeviceIdentityStore`), 0d (`ApiGuestTripReviewInput`), 0c (strings), 0e (harness). If one is missing, stop and report it.
- **Depends on:** 9g, 3c. **Wave:** 14. **Plan entry:** `plan.md` "### Phase 11a: Guest wizard" (l.3302–3342). Also read in `plan.md`: A5 (l.92, storage), A6 (l.93, timeouts), OQ 44 (l.164), OQ-A18 (l.617, credits from the server), OQ-A36 (l.635, the `06` wizard defaults), OQ-A227 (l.825, the `RootContent` entry decorators), "Android spec: state survival" (l.373–383), "Android spec: storage layout" (l.385–398), "Android spec: navigation and routes" (l.400–405), "Android spec: seeds and test hooks" (l.466–477), "Android-only copy awaiting approval" (l.487). *[Refresh R1]*
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. This is the first version of this brief; no earlier 11a brief existed. Before you code, open the merged code of 3c, 4a–4e, 5a and 9a–9g and use **their** names. Where a name in this brief differs from the merged code, keep the merged name and note it in the PR. Never rename earlier code except where a task below says so.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier; budgets (JVM and warm Robolectric < 1 s with the AGENTS first-case allowances, emulator < 10 s; *[Refresh R9]*); no copy asserted on screen (AGENTS "Tests" 1; *[Refresh R9]*); exact tag queries; `waitUntil`; never `Thread.sleep`; hang caps (`runTest(timeout = 60.seconds)`, `timeout_msec=60000`). Tier annotations are 0e's: Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Ported tests keep the Swift test names; a parameterised Swift test becomes a loop inside the Swift-named method with the argument in every assertion message (the 4c/9b precedent).
- **Tags:** **[F]** fact read in source (path and line given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `KT/` = `I/Packages/ChronoFlowKit/Tests`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`. `DC/` = `_design/canvas/`.
- **Paths:** `…/cm/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`; `…/cw/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`; `…/cwt/` = `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/`; `…/cds/` = `core/datastore/src/main/kotlin/net/chronoflow/core/datastore/`; `…/fw/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`; `…/fwd/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`; `…/fwt/` = `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/apptd/` = `app/src/testDebug/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-21, against 5781004d)

Checked against the code at `5781004d` (phases 0a–10d), iOS at `f391a1fa`, web at `e36368ec`, `plan.md` and the boards. **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name) and stops on any mismatch not listed here. Scope and deliverables are unchanged. Extra path aliases: `…/fwdt/` = `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/cd/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`; `…/app/shell/`, `…/app/wizard/` as named.

**R1. Plan and OQ references.**
- `plan.md` "### Phase 11a" is at **l.3302–3342** (not 2551–2591). Its five boxes: Guest mode l.3331, Guest review endpoint l.3335, GuestSecretStore l.3336, Discard and recovery l.3337, Tests l.3338. Other lines are corrected in the header.
- The last plan OQ is **OQ-A532** (10d). Task 14 appends §10 as **OQ-A533 … OQ-A543**, in order: OQ-11a-1 → A533, -2 → A534, -3 → A535, -4 → A536, -5 → A537, -6 → A538, -7 → A539, -8 → A540, -9 → A541, -10 → A542, -11 → A543. Each row starts "(11a OQ-11a-n)", as the 10a–10d rows do. A further new question (R12's gaps, if the worker raises one) takes **A544** and up. 11b's brief cites "OQ-11a-n" by its local number; leave those citations, the plan rows carry both numbers.
- OQ-11a-6's premise is void (R4): 5d already added `rememberSaveableStateHolderNavEntryDecorator()` + `rememberViewModelStoreNavEntryDecorator()` to `RootContent` (OQ-A227, commit `fd22c545`), superseding OQ-A186/OQ-5a-5. Its plan row (A538) records the R4 default instead of the body's.
- OQ-11a-8 is confirmed: `TripWizardDependencies.newGeneration()` returns `TripGenerationService` (R3).
- OQ-A461 (9h progress floor) is an **edit-only** rule in `MemberWizardModel`; it does not touch guest progress (9a's `guestStepProgress`, `WizardMode.kt` l.104). Do not change it.
- Board canvas: `_design/canvas/canvas.json` reads `"version": "1793387116-7ce3"`; the boards have not changed since `363dca19`. All 12 wrappers, `Wizard.dc.html` (line refs in §2.2 hold), `WizardSleep`, `WizardQAge`, `WizardQShift*`, `WizardExcluded*`, `GuestSignup*` and `GuestExcluded` exist. `WizardWhereGuestEmpty` and `WizardGuestJourney` are seed names only (no board), as §2.2 says.

**R2. The wizard as 9a–9h built it (reuse under these names).** Paths under `…/fw/` unless marked.
- **Screen and events.** `shell/WizardScreen.kt`: `WizardRoot(viewModel: TripWizardViewModel, onExit: (WizardExit) -> Unit)` (collects `viewModel.uiState`, `copyFormatter`, `searchState`, `retryEnabled`; collects `viewModel.exit` and calls `onExit` for every non-null value) and the stateless `WizardScreen(state: WizardUiState, onEvent)` / `WizardScreen(state, copyFormatter, onEvent)`; `internal fun WizardFooter(…)` in the same file draws the footer from `WizardChromeUi` (`ui/WizardUiState.kt` l.252: `secondaryClose`, `secondaryRequestsClose`, `readyFooter`, …). `TripWizardViewModel.onEvent(WizardEvent)` is the only entry from the screen; `shell/WizardEvent.kt` is a sealed interface (`RequestClose`, `CloseFromFooter`, `CloseNow`, `ConfirmDiscard`, `DismissDiscard`, `Continue(step)`, `Back`, `RevisitFromReady(row)`, `ReadyTick`, `ReadyForeground`, …). `WizardHapticCue(outcome, sequence)` is in `checking/WizardCheckingUi.kt`.
- **Close rules already exist** (`TripWizardViewModel.kt` l.588–651): `requestClose()` (toolbar ×, `RequestClose`) = guest **and** `excluded` → `close()`; else `hasChanges` → dialog; else `close()`. `closeFromFooter()` (`CloseFromFooter`, failure/Excluded "Close") = `excluded || mode == guest || !hasChanges` → `close()`. These are iOS `MemberWizardSteps.swift` l.140–150 and l.69–76; **No-credits Close** (`ReadyFooter.kt` l.23) also sends `CloseFromFooter`, so the guest term of iOS l.110 is covered. Tests already exist (`TripWizardViewModelCheckingTest.guestClosesFailureDirectly`, `guestToolbarDuringCheckingAsksToDiscardChanges`, `guestExcludedToolbarClosesDirectly`; `TripWizardViewModelExcludedTest.headerCloseOnGuestExcludedClosesDirectly`). The guest discard body is already in `WizardUiState.from` (`ui/WizardUiState.kt` l.544–561) with `WizardDiscardUiTest.newGuestTripKeepsTheWelcomeMessage`. §5.2's × / Close / body bullets and Task 7's "fix 9e/9f if missing" are therefore **verify-only**; Task 7 adds only `WizardExit.Welcome` and its routing (R3).
- **Exit.** `WizardExit` (`shell/WizardStepKey.kt` l.33) is `sealed interface { data object Trips; data class Saved(tripId) }`. The VM holds it in `exit: StateFlow<WizardExit?>` and **never clears it** (`exitOnce`, l.459); `WizardRoot` re-delivers it after every recomposition/recreation, so every host must make its exit handler idempotent. The member host guards this in `ShellViewModel.closeWizard(exit)` (`…/app/shell/ShellViewModel.kt` l.96–110: acts only while `state.overlay is Route.Wizard`). `ShellViewModel.closeWizard` has an exhaustive `when`: adding `WizardExit.Welcome` needs a no-op branch there.
- **Start and load failure.** `TripWizardViewModel(entry: core.navigation.WizardMode, deps: TripWizardDependencies?, presentation)`, `factory(entry)`; keys `TripWizardDependenciesKey`, `WizardInitialPresentationKey`. Init calls `deps.start()` for `WizardMode.Create`, then `deps.newGeneration()`, then builds `MemberWizardModel(draft, deps.store, viewModelScope, deps.now, service, deps.checkingClock, writerScope = deps.writerScope)`. Every start exception is already caught (l.417–437: `CancellationException` while the VM is active, `DataFailure`, `SessionFailure`, `DeviceIdentityFailure`, `StoreFailure`, `RecoveryRejected`, `IOException`, `GeneralSecurityException`, `RuntimeException`) and becomes `loadFailed(…)`: state `LoadFailed`, **and `exitOnce()` emits `WizardExit.Trips`**. So §5.1 step 1 ("the VM stays `Loading`") and the "Start failure" rewrap into `DataFailure.Server` are wrong: do not rewrap; a start failure already reaches the host as an exit (R3).
- **Make my plan** is `WizardEvent.Continue(WizardStepKey.Ready)` → `continueFrom` → `model.savePlan()` (l.520). There is no `vm.makePlan()`. 9a's guest `savePlan` (`MemberWizardCompletion.kt` l.216–227) sets `saveOutcome = WizardSaveOutcome.signUpRequested`.
- **Ready.** `ready/ReadyUi.kt`: `enum class ReadyRow(label, id)` = Route, Flight, Return, Sleep, Extras, LatestWake; the row tag is `"wizard.ready.${row.kind.id}"` (`ReadyRowsCard.kt` l.79). `ReadyRowUi(kind, value: ReadyText)`. `PriceLine` (FreeTrip, FreeHome, NoRefund, EditReturnNote, EditNote, `Balance(n, alert)`) with `copy: WizardCopy`; `ReadyPriceUi(showLabel, value, lines)`; `ReadyUiMapper.price(price, credits, editing, originalEndKind, endKind)` (l.244) has no mode parameter; `ReadyUiMapper.sleepSummary(draft)` (l.206) has no guest branch. The mapper already omits Return for guests (`state.mode == member`, l.47; `ReadyRowsUiTest.returnIsNotShownForGuest`). So Task 8 adds **`ReadyRow.Questions("Questions", "questions")`** (not `ReadyRowUi.Questions`; the tag `wizard.ready.questions` follows), the guest `sleepSummary` label, a mode argument to `price(…)`, and `PriceLine.GuestPaid(cost, balance)` / `PriceLine.GuestFree`. `RevisitFromReady` maps rows with `else -> fineTune` (VM l.723–744): the Questions row must get its **own** branch calling 9a's `revisitGuestQuestions()` (distinct iOS rule → distinct mapping; never let it fall into `else`).
- **Copy lookups.** `WizardCopy(key, args).resolve()` (`ui/WizardCopy.kt`) looks the en key up in `CopyIndex` (`CopyTable.Wizard`, then `CopyTable.Android`) and resolves plurals itself from the `count` argument. So "{{count}} answered" is `WizardCopy("{{count}} answered", mapOf("count" to n.toString()))` (`R.plurals.wizard_count_answered`), not a hand-written `pluralStringResource`, and the Android-only price pair is reached through its en key once `make strings` adds it to `CopyIndex`. Every §5.10 string exists in `core/ui/src/main/res/values/strings_ios.xml` (`wizard_what_you_entered_is_not_saved_you_c2c819`, `wizard_your_sign_up_credits_stay_unused`, `wizard_questions`, `wizard_none_needed`, `wizard_count_answered`, `wizard_early_bird`, `wizard_in_between`, `wizard_night_owl`, `wizard_not_sure`, `wizard_on_a_normal_work_day`, `wizard_your_answers`, `wizard_a_new_plan_needs_an_upcoming_arriv_f424a3`, `wizard_ready_to_plan`, `wizard_make_my_plan`, `wizard_new_trip`); the iOS "of the 3 credits" key is `wizard_paid_with_cost_of_the_3_credits_yo_c69be5`. `android-only.json` is `{"entries": [{name, en, nl, replaces: [{table, key}], review, comment}]}`.
- **Back.** `shell/WizardBackHandling.kt` registers its `BackHandler`s **after** the wizard's inner `NavDisplay` (`WizardScreen.kt` l.457–459, comment says why): first screen/Where → `RequestClose`; Checking (no failure) → `Back`; sheet → `CancelAirport`; dialog → `DismissDiscard`; Calculating swallows back; other steps go back through the inner `NavDisplay`'s `onBack`. The discard dialog is `CfAlertDialog` with **no** test tag; its buttons are `cf.dialog.confirm` / `cf.dialog.dismiss` (as `WizardBackHandlingTest` and `WizardJourneyTest` use). There is no `wizard.discard` tag; tests use the `cf.dialog.*` tags (adding `testTag = "wizard.discard"` through `CfAlertDialog`'s existing `testTag` parameter is allowed but not required).
- **Guest UI that already exists** (render it; do not rebuild): Where hides the last-trip hint for guests (`WhereUiStateTest`); Checking line 2 "Your answers" (`WizardCheckingUi.kt` l.120); Sleep subtitle "On a normal work day." (`sleep/WizardSleepUi.kt` l.55); the Age disclaimer (`questions/WizardQuestionUi.kt` l.93); no sleep switch (`TripWizardViewModelFineTuneTest.guestHasNoSleepSwitch`); `WizardSleepQuestionBoardSeed` already seeds `WizardSleep`, `PickerTime`, `WizardQAge`, `WizardQShift`, `WizardExcluded` in guest mode (`docs/wizard.md` l.699–704).
- **Member host (the pattern 11a mirrors).** `…/app/wizard/WizardLauncher.kt` is retained by `ShellViewModel.wizardLauncher(deps, io)` (re-created only when the deps instance changes); `…/app/shell/WizardOverlay.kt` builds the extras and calls `WizardRoot(rememberOverlayViewModel(mode, TripWizardViewModel.factory(mode), extras), onExit)`; `ShellViewModel` owns the overlay `TabViewModelStoreOwner` and clears it when the overlay closes. `…/app/wizard/SessionTripWizardDependencies.kt` is the member `TripWizardDependencies`; `…/app/wizard/WizardRecoveryCheck.kt` (`shouldOpen(epoch)`, skipped while `LaunchPlatform.wizardBoardCapture != null`) is the member recovery; `WizardStartOverride` + `LaunchPlatform.prepareWizard` (debug `WizardDebugSeeds`) install member board seeds. `TripPageViewModel` holds its pending wizard request in `mutablePendingWizardRequest` until `acknowledgeWizardRequest()` (`feature/trip/…/page/TripPageViewModel.kt` l.186, l.495): the one-shot pattern for anything 11a adds.
- **Captures.** `…/fwdt/WizardBoardCaptureTest.kt` is parameterised over `WizardBoardCaptureSeed.names` and uses `ProfileRule`, `wizardBoardProfile` (`WizardBoardStates.kt` l.37) and `captureWizard` (`WizardCapture.kt`). Do **not** add guest wrappers to `WizardBoardCaptureSeed.names`: that list drives the member capture parameters and `WizardDebugSeeds.knows`. Give the guest captures their own list (R8).

**R3. What 11a adds in `:feature:wizard` / `:core:wizard`, corrected.**
- **Generation type (OQ-11a-8 applies).** `TripWizardDependencies.newGeneration(): TripGenerationService` and the VM field `generation: TripGenerationService?` (calls `cancelNow()` in `loadFailed` and `onCleared`). `MemberWizardGeneration` (`…/cw/generation/MemberWizardGeneration.kt`) has `review(instance, input, edit = null)`, `save(state, didCreate)`, `suspend cancelReviews(instance)`, `suspend cancel()` and **no** `cancelNow()`. The `refactor(wizard): …` commit widens `newGeneration()` and the field to `MemberWizardGeneration` and adds a non-suspending `fun cancelNow()` to the interface (implemented by `TripGenerationService`, l.255). `MemberWizardModel` already takes `generation: MemberWizardGeneration?`. Update every `TripWizardDependencies` implementer and caller in the same commit (`git grep -l TripWizardDependencies`: `SessionTripWizardDependencies`, the two anonymous objects in `…/appd/WizardDebugSeeds.kt`, `…/fwt/WizardTestSupport.kt`, `TripWizardViewModelEntryTest`, and in `:app` tests `WizardLauncherPrepareTest`, `WizardOverlayModesTest`, `TripActionsWizardFixture`, `WizardSaveGraphFixture`, `WizardDebugSeedsTest`).
- **Kotlin has no protocol defaults:** `GuestWizardGeneration` implements `review` with the `edit != null → DataFailure.Server(null)` guard and `save` throwing `DataFailure.Server(null)` itself (iOS `MemberWizardModel.swift` l.15–28 holds as cited).
- **Timeout.** There is no `race` helper. `TripGenerationService.review` (l.91–112) runs the request inside `coroutineScope { val deadline = launch { sleep(30.0); ensureActive(); throw DataFailure.Server(null) }; try { performReview(…) } finally { deadline.cancel() } }` inside a LAZY `scope.async`, keyed by a **private** `ReviewKey(instance, fingerprint, token, reviewRevision, edit)`. Copy that pattern into `GuestWizardGeneration` with its own private key; do not make `TripGenerationService` internals public. `WizardFingerprint.canonical(input: ApiTripReviewInput)` exists (`…/cw/model/WizardFingerprint.kt`).
- **`WizardExit.Welcome`.** Add it; `close()` → `exitOnce()` must emit `Welcome` when the model's mode is guest. `loadFailed` has no model, so the VM cannot know the mode there: emit `Welcome` from `exitOnce` when `deps` declares guest (add `val guest: Boolean get() = false` to `TripWizardDependencies`, true in `GuestWizardDependencies`), or let the guest host treat `Trips` as Welcome. Either way member exits stay `Trips`/`Saved` byte for byte, and `ShellViewModel.closeWizard` gets a no-op `Welcome` branch.
- **Sign-up seam (§5.8)** stays as written, but put the "presented" one-shot in the VM (held until `onDismissed`), not in the route, and react to `snapshot.saveOutcome` inside the existing `model.snapshots.collect` (l.354). `setGuestAuthenticationPresented`, `guestAuthenticationPresented`, `flush()`, `closed`, `persistenceFailed` exist on `MemberWizardModel` (l.24–59, l.287–297).
- **`GuestWizardStore`** goes in `:core:wizard` (`…/cw/model/`): Konsist lists `core/wizard` as a JVM core module and only bans Android imports and wall-clock reads there (`timeHygienePatterns`: no `Instant.now(` — inject `now`). No `:core:data` fallback is needed. Note that 9b's **member** store is `:core:data` `net.chronoflow.core.data.wizard.MemberWizardStore` (not `:core:wizard`); `RecoveryRejected` is `…/cw/model/RecoveryRejected.kt` (`class RecoveryRejected : Exception()`). `MemberWizardRecord` has `Payload(form, state, fieldErrors)` strings, `encode(draft)`, `decode(payload)` (returns null on bad legs/stays) and `EXPIRY_MILLIS = 1_800_000L` / `isExpired(updatedAtMillis, nowMillis)`; reuse `EXPIRY_MILLIS` in `GuestRecoveryGate` rather than a second literal. `WizardDraftStore` (`MemberWizardState.kt` l.111) is `persist`, `discard`, `didSave` — no `recover()`.
- **`ProfilePreferences`** (`…/cm/contracts/ProfilePreferences.kt`): `request(value: JsonObject): JsonObject` (l.76), `containsRetiredValues(json: String)` (l.177), `cleanLegacyJSON(data: ByteArray): ByteArray` (l.181). `MemberWizardState.discardRetiredValues()` (l.93).
- **Generated constructor arguments (implementation check).** `ApiTripInputOverride` and `ApiReviewIdentity` have no Kotlin defaults for nullable fields. Supply those fields explicitly in ported calls (`ApiTripInputOverride(false, null, null, null)`); this changes no earlier-phase behavior.
- **DTO path.** `ApiGuestTripReviewInput(input, identity, profile, deviceID, guestSecret)` is `…/cm/api/ApiGuestTripReviewInput.kt` (not `GuestTripReviewInput.kt`); its encoder already forces `trip_end_kind = "one_way"`, drops `return`/`override` and runs `ProfilePreferences.request`; `toString()` redacts `guestSecret`. The fixture is `core/testing/src/main/resources/fixtures/wizard/guest-review-request.json`.

**R4. The guest host in `:app` (§5.1, §6), corrected.**
- **Today:** `Route.GuestWizard` (`core/navigation/…/Route.kt` l.20, state key `"guestWizard"`) falls into `CfRoot`'s `else -> RoutePlaceholder(route, …)` (`…/app/CfRoot.kt` l.264), tag `route.guestWizard`; `RootContent` keys its entry as `"guestWizard:<revision>"` (`RootContent.kt` l.25–40) and every `RootNavigator.navigate` bumps `revision`. Welcome "Plan my trip": debug `DebugWelcomeDestinations.planMyTrip()` sets `seam = "guestWizard"` (`…/appd/DebugWelcomeDestinations.kt`), which `CfRoot` shows as a placeholder over Welcome with `BackHandler(enabled = seam != null)` (l.81–86, l.143–146); release `RoutedWelcomeDestinations.planMyTrip()` is **a no-op** (`…/app/WelcomeDestinations.kt`). Debug/release choose through `LaunchPlatform.welcomeDestinations(navigator)` (debug and release `LaunchPlatform.kt`). `sign-in.guest` already navigates to `Route.GuestWizard` (`…/app/auth/AuthServices.kt` l.62; `AuthServicesTest`).
- **Entry change (Task 10):** `RoutedWelcomeDestinations.planMyTrip()` navigates to `Route.GuestWizard`; delete `DebugWelcomeDestinations` and make `LaunchPlatform.welcomeDestinations` return `RoutedWelcomeDestinations` in every variant (or drop the indirection); remove `seam`/`closeSeam` from `WelcomeDestinations` and the seam placeholder and `BackHandler` from `CfRoot`; update `WelcomeSeamTest` (l.87), `RootContentTest` (l.70–75 assert the `route.guestWizard` placeholder today), `WelcomeDestinationsReleaseTest`, `ReleaseReadinessTest` l.168, and plan.md l.472 ("Welcome destinations": "`welcome.plan` opens the debug-only `route.guestWizard` seam until 11a").
- **ViewModel scope (OQ-11a-6 → A538).** `RootContent` already decorates each entry with a `ViewModelStore` (OQ-A227), cleared when the entry leaves and kept across recreation; `SignUpRoute` uses it through `viewModel(factory, extras)`. So `GuestWizardRoute` gets `TripWizardViewModel` from the entry store with `TripWizardViewModel.factory(WizardMode.Create)` and extras carrying `TripWizardDependenciesKey` (as `WizardOverlay` builds them). **Keep the name `GuestWizardHolder`** (11b's brief greps it and hangs its sign-up coordinator on it), but make it an **entry-scoped `ViewModel`** created in the same entry store that owns `GuestWizardDependencies` (the airport search, the generation factory), not a process-scoped `AppGraph` member with its own `ViewModelStore` and route collector. A new Plan my trip bumps the revision and gets a fresh entry, holder and VM. Drop `AppGraph.guestWizard` from §6.
- **Back.** `RootContent` has no `BackHandler`; its `NavDisplay(onBack = onRootBack)` (activity `finish`) holds a single entry. The wizard's handlers register later and win on Where/first screen, Checking, sheets and the dialog; other steps pop the inner `NavDisplay`. Replace §5.1's "disable the root handler" with a Robolectric check that system back on Where shows/close-routes (never finishes the activity) and that back on Outbound returns to Where.
- **Dependencies.** `source` is a non-suspending `val` on `TripWizardDependencies`, while `AppGraph.dataSources.public()` is `suspend`: use `LazyPublicDataSource { dataSources.public() }` (`…/app/auth/LazyPublicDataSource.kt`, `internal`; `AppGraph` l.210 already builds one for `AuthServices` — reuse it or build one beside it). `AirportSearchService(source, now, sleep = { delay(…) }, cache = AirportSearchCache())` as in `SessionTripWizardDependencies`. `AppGraph.deviceIdentity` is `DeviceIdentityStore(devicePreferences)` with `suspend fun identity()`. Presence: `controller.presence` is `@Volatile var presence: SessionPresence?` (`core/sync/…/SessionController.kt` l.50), `null` before restore; treat anything but `SessionPresence.SignedOut` as signed in (abort). `GuestResetLinkRouter({ controller.presence }, controller.updates, …)` (`AppGraph` l.251) is the precedent.
- **Start abort when signed in.** iOS returns silently (`GuestWizardDestination.swift` l.83, `catch is CancellationError { return }`). Here the VM turns the abort into `loadFailed` + an exit (R2), so the guest exit handler must navigate to Welcome **only** while `rootNavigator.state.value.route == Route.GuestWizard` with the revision the route was composed with **and** `presence == SignedOut`; otherwise do nothing. This is the guest analogue of `ShellViewModel.closeWizard`'s overlay guard and also makes the re-delivered `exit` (R2) harmless. Put this logic in a JVM-testable class (e.g. `GuestWizardExits` in `…/app/guest/`), not in the composable.
- **AppGraph today:** `wiper = AccountWiper(storageRoot, namespace, sessionStore, io = dispatchers.io)` (l.168, `guestSecrets` defaults to `NoGuestSecrets`); `hooks: SessionHooks by lazy { NoopSessionHooks }` (l.187), called from `activation` (`hooks.onForeground()`, l.391) and the controller's `stopSync` (`hooks.onSessionEnding()`, l.319). `SessionHooks` (`…/cd/session/SessionHooks.kt`) has **two** members, `onForeground()` and `onSessionEnding()`: the guest hooks implement both (`onSessionEnding` = no-op) and keep 12b able to compose its own. `storageRoot: suspend () -> File` (l.107, resolves `overrides.storageDir ?: app.noBackupFilesDir` on io). `GraphOverrides(storageDir, mockDelay, sessionAlias, mockSeed, sessionCipher, dispatchers, mutationsOffline)` is called positionally (e.g. `TestSeeds.apply`): append any new field at the end with a default.
- **Recovery scheduler (§5.7).** `controller.launchIntro` is `graph.launch.launchIntro: LaunchIntro?` (`feature/launch/…/LaunchController.kt` l.107; `LaunchIntro(start, clock, frozenTime)`, `start = 2300 ms` on a normal launch, offsets 0–450 ms + 450 ms item duration, so the last item lands at `start + 900 ms`); `handOffComplete` and `overlayActive` are on `LaunchController` (l.159–161); `LaunchClock.sleepUntil(deadline)` (`feature/launch/…/model/LaunchClock.kt` l.12); `ManualLaunchClock` is in `feature/launch` **testFixtures** (`:app` tests already depend on it). The "session epoch" (iOS `connectionGeneration`) is `controller.updates: StateFlow<SessionUpdate?>` (`SessionUpdate(presence, epoch)`); `SessionController.generation` is `internal`, do not use it.

**R5. Guest storage: what exists and what 11a adds.**
- **Exists (4a/4e):** `GuestSecrets { suspend fun deleteAll() }` + `NoGuestSecrets` (`…/cd/session/GuestSecrets.kt`); `AccountWiper.erase(id)` deletes `Accounts/<id>`, calls `guestSecrets.deleteAll()`, then deletes `<root>/<ns>/guest-wizard.json` (`AccountWiper.kt` l.60–66); `SessionCipher { encrypt(alias, plaintext): Sealed(iv, ciphertext); decrypt(alias, sealed); deleteKey(alias) }` + `PermanentSessionKeyFailure` (`core/datastore/…/SessionCipher.kt`); `AndroidKeystoreSessionCipher`; `KeystoreSessionStore` (the retry-once-on-permanent-failure precedent); `StorageNamespace(directoryName, keychainService)` with `sessionKeyAlias` (`core/model/…/session/StorageNamespace.kt`) — add `val guestKeyAlias get() = "$keychainService.guest"` beside it rather than building the string in two places; `DevicePreferences` (one shared `PreferenceDataStoreFactory` store). `SoftwareSessionCipher` exists twice as a **test** class: `core/datastore/src/test/…` and `app/src/test/…/SoftwareSessionCipher.kt` (visible to `testDebug`).
- **Does not exist (11a adds, as §5.4–§5.6 say):** `GuestSecretStore`/`MemoryGuestSecretStore`/`GuestRecoveryLocation` (`:core:model`), `KeystoreGuestSecretStore` (`:core:datastore`), `GuestWizardStore`/`GuestRecoveryGate`/`GuestWizardGeneration` (`:core:wizard`), and the `AppGraph` members. Nothing in `core/sync` or `core/plan` is guest-specific; do not add guest code there.
- **Backup:** `app/src/main/res/xml/data_extraction_rules.xml` excludes every domain (`root`, `file`, `database`, `sharedpref`, `device_*`) for cloud backup and device transfer, so `filesDir/datastore/guest_secrets_*.preferences_pb` is already excluded; Task 2's check is read-only.
- **Secrets rules (A23, AGENTS "Secrets"):** the guest secret lives in memory inside one review call and Keystore-encrypted in its DataStore value (the plan's A5 / "storage layout" row 06 OQ 12); never in `guest-wizard.json`, the member DB, a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route, a seed extra, a log or an exception message; `toString()` redacts.
- **Leave for 11b (do not pre-empt):** `GuestWizardStore.claim(secrets)` / `reviewExpired(claim)` (iOS l.92–112), `GuestClaim`, `GuestSessionContinuation`, `GuestSessionAcceptance`, the GuestSignup sheet/coordinator, "I already have an account". 11b's brief expects, by these names: `GuestSecretStore`/`MemoryGuestSecretStore` in `net.chronoflow.core.model.guest`; `class GuestWizardStore` in `:core:wizard` with `start`/`recover`/`persist`/`discard` and a private record reader it can extend; `GuestSignUpLauncher` + `TripWizardDependencies.signUpLauncher` + the VM's `signUpRequested` handling in `…wizard.guest`; `GuestWizardHolder`, `GuestWizardRoute`, `GuestWizardBoardSeed` (with `GuestSignup*` → `WizardReady`); `GuestWizardDebugCounters.signUpRequested`. Keep those names and keep `GuestSecretStore.get(token)` public (the claim reads it).

**R6. Seeds and test hooks.**
- Seeds are parsed in `…/appd/TestSeed.kt` (`accepted[...]`, `flag(…)`, `parsed(…)`; `wizardBoardCapture = accepted["CF_WIZARD_BOARD_CAPTURE"]`, l.211) and applied by `TestSeeds.apply` (`…/appd/TestSeeds.kt`) from `net.chronoflow.debug.SeedReceiverActivity`. `DebugSeedRouting.current` holds the applied seed; `LaunchPlatform.wizardBoardCapture` (debug l.38; release `null`) reads it. `CF_GUEST_WIZARD`, `CF_GUEST_WIZARD_JOURNEY`, `CF_GUEST_RECOVERY_BOARD` **do not exist yet**: add them as `TestSeed` fields and rows in `TestSeedSessionKeysTest`, plus plan.md l.466 seed names.
- **Reset:** with a `CF_AUTH_TEST_ID`, `TestSeeds.apply` uses storage `filesDir/uitests/<id>/` and `SeedRunner.reseed` → `MockSessionSeed.reset` deletes that whole directory, so `<dir>/ChronoFlowMock/guest-wizard.json` is already gone on every re-seed. The guest secret DataStore and its Keystore alias are **outside** that directory and per namespace, so the reset still calls `guestSecrets.deleteAll()` (mock namespace); `ReseedEqualsColdSeedTest` (`app/src/testDebug/kotlin/net/chronoflow/debug/`) asserts it.
- `CF_GUEST_WIZARD` navigation follows iOS `prepareUITestRoute` (`AuthSessionServices.swift` l.251–262, only with the `screen` profile's fast launch). The guest board seed persists into the **real** guest store (iOS) — unlike the member board seeds, which swap in `WizardMemoryDraftStore` (`WizardBoardCaptureSeed.kt` l.278) through `WizardStartOverride`; the guest route does not use `WizardStartOverride`/`prepareWizard` (they run only inside `AppShell`).
- `WizardBoardCaptureSeed.draft(board, locale)` (two parameters; it builds its own draft) and `WizardCompletionBoardSeed` (`internal object`, `apply(…)`, `uiOverride(board)`) are in `…/fwd/`. `GuestWizardBoardSeed` lives in the same package, must be **public** (the `:app` debug code calls it) and maps names as §5.9 says.
- Debug counters precedent is a **debug/release pair** of the same object (`SettingsDebugCounters`, `TripActionsDebugCounters`, `RefusedChangeDebug`). §5.8's debug-only `GuestWizardDebugCounters` plus a `LaunchPlatform` hook is acceptable (11b keeps the name), but the release side must then pass `{}` through `LaunchPlatform` in **release and perf** source sets.

**R7. Lessons from 9f–10d the worker carries.**
- Distinct iOS rules get distinct events or branches (`RequestClose` ≠ `CloseFromFooter` ≠ `CloseNow`; the Questions row ≠ the Fine-tune rows).
- One-shot events live in the ViewModel until acknowledged (`acknowledgeWizardRequest`); the exit `StateFlow` is never cleared, so hosts guard it (R2, R4). The sign-up "presented" flag is model state, reset only by `onDismissed`.
- Retained objects live in a retained owner (`WizardLauncher` in `ShellViewModel`; R4's entry-scoped `GuestWizardHolder`), never in `remember` or a process singleton keyed by nothing.
- One test scheduler per test: build the VM, store, generation and fake clock on the `runTest` scheduler (`StandardTestDispatcher(testScheduler)` for io/main), and cancel/join scopes before resetting Main.
- Never seed "now" with `Instant.EPOCH`: use `WizardTestClock` (`core/testing/…/wizard/WizardTestSupport.kt` l.21, starts at `1_800_000_000` s; it is in `:core:testing`, not 9a) or the seed's fixed instant.
- Spacing and sizes come from production components and tokens (`PlanUITokens`, `ReadyMetrics`, `WizardMetrics`), never re-declared in a test or a guest-only copy.
- Every commit leaves `make targeted` for what it touched green; the `refactor(wizard)` widening commit compiles every implementer on its own.
- Secrets: R5.

**R8. Tests, corrected (Tasks 7, 8, 10, 13, 14).**
- **No copy on screen.** Drop every on-screen text assertion: §7 Task 7 `GuestDiscardDialogTest` (body text en/nl), Task 8 `GuestReadyScreenTest` text, Task 13 `GuestWizardScreenTest` ("past text exact", "On a normal work day.", "Your answers", "23:30–07:15 · In between", "2 answered", guest price, `testReadyGuestDarkNL` strings), Task 14 "`wizard.ready.questions` contains "2 answered"". Assert the same facts as JVM tests on UI state (`WizardUiState.discardMessage.key`, `ReadyUi` rows and `PriceLine`s, `WizardCheckingUi` lines, `WizardSleepUi`) and let the captures show the copy. Screen tests find nodes by tag and check structure only (tag present/absent, enabled, click outcome).
- **Cheapest tier.** The guest close/expiry/secret-deletion logic (`closeDeletesSecretAndDraftAndShowsWelcome`, `foregroundAfterExpiryReturnsToWelcome`, `startFailureReturnsToWelcome`, the signed-in abort) is JVM on R4's exit/host class with fakes; keep the Robolectric `GuestWizardHostTest` (real `AppGraph`, `GraphOverrides(sessionCipher = SoftwareSessionCipher())`) only for route wiring: `planMyTripOpensGuestWhere`, `rotationKeepsTheViewModel`, `leavingTheRouteClearsTheViewModel`, back on Where. Task 7's `TripWizardViewModelGuestExitTest` keeps only the new cases (`guestCloseExitsToWelcome`, `memberCloseStillExitsToTrips`, `guestCrossOnEmptyWhereClosesAtOnce`, `exitIsEmittedOnce`); the other four exist (R2).
- **Guards and captures** belong in `:feature:wizard` `testDebug` on the stateless `WizardScreen(state, onEvent)` with seeded states (the `WizardGuardsTest`/`WizardBoardCaptureTest` precedent), not on `AppGraph`. `GuestWizardScreenTest` (Task 13) drops its `AppGraph`/`CF_GUEST_WIZARD` setup and goes to `…/fwdt/` too.
- **Budgets:** JVM and warm Robolectric < 1 s each (AGENTS first-case allowances 20 s/3 s recorded separately), emulator < 10 s; acceptance 2 and 4's "< 5 s" become "< 1 s warm".
- **Guest secret Keystore test:** `app/src/androidTest/kotlin/net/chronoflow/app/store/` exists beside `KeystoreSessionStoreTest.kt` and `StoreThreadingSmokeTest.kt` ✓.
- **Mock test location (Task 5):** `:core:data-mock` tests do not depend on `:core:wizard`. Put `GuestGenerationMockTest` in `core/data/src/test/kotlin/net/chronoflow/core/data/wizard/` (`:core:data` tests already see `:core:data-mock` and `:core:wizard`, where 9f put `GuestWizardModeTests`) instead of adding a module edge. Fakes: `core/testing/…/wizard/GenerationSource.kt` already records `guestInputs` and takes a `guestHandler` for `reviewGuestTrip`.
- **Existing guest tests (stay green):** 9a's `GuestWizardModeTests` are **two** classes — `core/wizard/src/test/…/model/GuestWizardModeTests.kt` (9 cases) and `core/data/src/test/…/wizard/GuestWizardModeTests.kt` (1 case, 9f) — 10 in all; 4b `GuestTripReviewAPITests` (`core/network`), 0d `GuestTripReviewContractTests` (`core/model`), 4c `MockGuestReviewTests`, `MockGuestPossessionTests`, `MockGuestRegistrationClaimTests` (`core/data-mock`). There is no `WizardBackTest`/`WizardDiscardTest`: the 9c tests are `WizardBackHandlingTest` and `WizardDiscardUiTest`.
- **iOS test files:** there is no `GuestWizardDarkDutchScreenTests.swift` and no `GuestSignupScreenTests.swift` at the pin. `testReadyGuestDarkNL` is in `AppUITests/GuestWizardCompletionScreenTests.swift` l.35 (with `testFineTuneGuest`, `testReadyGuest`, `testReadyGuestFree`); its `testGuestSignup*` and `testSignInInstead` (l.46–87) are 11b's.
- **Captures:** `make captures ONLY=` takes a comma list of name **prefixes**, not globs (`BoardCapture.kt` l.18). Acceptance 5 is `make captures ONLY=WizardWhereGuest,WizardOutboundGuest,WizardOutboundPastGuest,WizardCheckingGuest,WizardFineTuneGuest,WizardReadyGuest,WizardDiscardGuest` (12 PNGs).

**R9. Perf (§8 item 10 replaced).** 11a changes the wizard path (guest entry, Ready, generation) and the launch path (the `CfRoot` recovery effect on Welcome, new `AppGraph` members, Welcome's Plan my trip). Per AGENTS "Performance" and CLAUDE.md "Commands": run **`make baseline-profile`** and commit the regenerated profiles (`perf(app): regenerate the baseline and startup profiles for the guest wizard`; 9a–9h and 10d did this, `382b0b98`, `dc7b481a`, although the generator has no wizard journey); run **`make perf`** (startup) and record cold/warm in `docs/perf-startup.md` with device, build type and command, checking that `Application.onCreate` and the pre-first-frame path gained no I/O (the new `AppGraph` members are `by lazy`, nothing resolves `noBackupFilesDir`, DataStore or the Keystore on main). **`make perf-timeline` does not apply** (no timeline change). No new budget row.

**R10. iOS references.** Every cited path exists at `f391a1fa`, with these corrections: "Path" and "No health data" are iOS `plan.md` l.2066–2067 (not l.2070, which is "Review call"); "Shared Wizard properties" l.2079; `GuestWizardDestination.swift` start failure is l.83–86 (`catch is CancellationError { return }` l.83) and `signupDismissed` l.121–128; `MemberWizardSteps.swift` nav title l.131 and × l.141–151 (guest-excluded term l.143), footer Close l.69–76; `MemberWizardReadyView.swift` `sleepSummary` l.137–156 (guest label l.150–154), price block from l.173 (guest branch l.177–185); `GuestWizardStore.swift` `claim`/`reviewExpired` l.92–112, `recover`+`recoverRecord` l.34–75. `GuestSecretStore.swift` 33 lines, `KeychainGuestSecretStore.swift` 68, `GuestRecoveryGate.swift` 14, `GuestWizardBoardSeed.swift` 39, `GuestSessionContinuation.swift` 7 (all as cited).

**R11. Research references.** `06-wizard.md` §10 l.1053–1073, §4.6 l.489, §4.7 l.511, §12 l.1089; `05-launch-auth.md` §12 l.728–766; `03-api-contract.md` §10 l.503–539 — as cited.

**R12. Genuine gaps the worker must handle (raise as OQ-A544+ only if the default below does not work).**
1. Guest exit routing when the VM cannot know the mode (`loadFailed`) and the re-delivered `exit` (R2, R3, R4): default as R3/R4.
2. A signed-in start abort must not navigate (R4).
3. The RevisitFromReady `else -> fineTune` fall-through (R2).
4. Guest captures must not join `WizardBoardCaptureSeed.names` (R2, R6).
5. The guest secret store is namespace-global while seeds isolate storage per test id; the reset must call `deleteAll()` (R6).
6. `SessionHooks` has two members; 12b will add reminders hooks to the same seam — compose, do not replace (R4).

This phase is mostly **wiring, storage and a little UI**. The guest *logic* of the wizard (`WizardMode.guest`, the guest path, progress, `savePlan` → `WizardSaveOutcome.signUpRequested`, `revisitGuestQuestions`, `guestAnsweredQuestionCount`, `guestReviewExpired`, `enforceGuestMode`, `MemberWizardDrafts.guest()`, the guest past-arrival copy) is **9a's and already merged**. The mock guest review and the API route are **4c's and 4b's and already merged**. Do not port them again. If a rule below is missing from 9a/4b/4c, port it into that module with its iOS test in a separate `fix(<scope>): …` commit that names the Swift line, and report it.

---

## 1. Goal

A signed-out person taps **Plan my trip** on Welcome (or "New here? Plan a trip first" on Sign in) and gets the same wizard a member gets, in **guest mode**: Where → Times → Your sleep (always) → Checking → [Age, Shift work] → Fine-tune → Ready. Guests plan one-way trips only. (plan.md 11a's line "Questions are excluded for guests, per 9e" means the five retired health questions: 9e builds only Age and Shift work, and guests do answer those two when the review asks; iOS `plan.md:2066–2067`, 18 "Path" and "No health data"; *[Refresh R10]*.) The review goes to the public `POST /v1/guest/trips/review`. The secret the server mints on the first review is kept only in a Keystore-encrypted store, one entry per draft token, and rotated when the server returns a new one. The guest draft is one JSON file, `noBackupFilesDir/<ns>/guest-wizard.json`, with a 30-minute sliding expiry; it never contains the secret.

On guest Ready, the rows are Route, Flight, Sleep ("23:30–07:15 · In between"), Questions ("2 answered") and Extras, and the price block says "Paid with 1 of the 3 credits you get when you sign up." (or "Your sign-up credits stay unused." when free). "Make my plan" runs the 9a gate and raises `signUpRequested`; the sign-up sheet itself is **11b**, so in 11a that request goes to a seam (§5.8).

× (with the guest discard alert when something was entered), the excluded Close, the failure Close and the No-plan Close all return to **Welcome** and delete the draft and its secret. On a cold launch, a live guest draft (< 30 min) reopens the wizard on its step **after** the Welcome intro has finished, with no message.

Done means:
- a guest reaches guest Ready on the mock (one emulator journey);
- the guest draft survives a process kill and is recovered quietly (one seeded emulator check plus a recorded manual `adb` kill);
- the ported iOS cases (18b–18h, 18k–18m) pass at the cheapest tier;
- every guest board and its DarkNL wrapper is guarded on Robolectric and captured.

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it: "Working rules", "Performance", "Tests", "Traps").
2. `plan.md` sections listed in the header.
3. Research: `docs/research/06-wizard.md` §10 (guest specifics, l.1053–1072), §4.6 (progress), §4.7 (recovery), §12 OQ 1 and OQ 12; `docs/research/05-launch-auth.md` §12 (l.728–766; the sign-up half is 11b); `docs/research/03-api-contract.md` §10 (l.503–539).
4. Earlier briefs (and their merged code): `phase-9a-wizard-model.md` (§Task 5 items 7–8: `MemberWizardDrafts.guest()`, `MemberWizardRecord`; Task 7 item 4 member list; Task 8 guest progress; OQ-9a-4, OQ-9a-10), `phase-9b-wizard-data.md` (§6.1 `TripGenerationService`, §6.5 `MemberWizardStore`, §6.8 `TripWizardDependencies` / `TripWizardViewModel`, §12 hand-off "11a/11b"), `phase-9c-wizard-shell-where.md` (§5.4 back, close, discard; §5.8 entry, quiet recovery, seeds, `WizardBoardCaptureSeed`; OQ-9c-5), `phase-9e-wizard-sleep-questions.md` (Excluded × and Close rules, guest seeds render-only, OQ-9e-7), `phase-9f-wizard-checking-finetune.md` (guest Checking line 2, `WizardUiOverride` §5.10, `WizardCompletionBoardSeed`), `phase-9g-wizard-ready-save.md` (§5.1–§5.5 Ready, `ReadyUiMapper`, `PriceLine`, Tasks 8–9), `phase-3c-welcome.md` (§5.5 `LaunchIntro`, `WelcomeDestinations`, OQ-3c-2), `phase-3b-launch-readiness.md` (`handOffComplete`, `overlayActive`), `phase-3a-launch-overlay.md` (`LaunchClock.sleepUntil`), `phase-4a-store.md` (`StorageNamespace`, `SessionCipher`, `AndroidKeystoreSessionCipher`, `SoftwareSessionCipher`, `DeviceIdentityStore`), `phase-4b-api-client.md` (Task 8 `reviewGuestTrip`, `GuestTripReviewAPITests`), `phase-4c-mock-data-source.md` (`MockGuestReview`, `MockGuestReviewTests`, OQ-4c-1), `phase-4e-session.md` (`AccountWiper`, `GuestSecrets`/`NoGuestSecrets`, `SessionHooks.onForeground`, `RootNavigator.revision`, `AppGraph.dataSources.public()`, `TestSeeds`, `ReseedEqualsColdSeedTest`, OQ-4e-14), `phase-5a-sign-in.md` (`Route.GuestWizard` from `sign-in.guest`, OQ-5a-5), `phase-0c-strings.md` (T2 `android-only.json` schema, §11 rules), `phase-0e-test-harness.md` (Guards, BoardCapture, ScreenTest, seeds, presented counters).

### 2.2 Boards (`DC/`, read-only; open the HTML in a browser to render)

Every wrapper is a ~0.85 KB file that `<dc-import name="Wizard" … guest="{{guest}}" pct="{{pct}}" theme="{{theme}}">`s `DC/Wizard.dc.html` at 390 × 844 with `guest: true`. Guest logic in `Wizard.dc.html`: exits l.243–246, back map l.249–257, Where hint suppressed for guests l.290, past copy l.349, Checking lines l.375, disclaimer l.403, Fine-tune guest rows l.416, no switch l.428, Ready guest rows and price lines l.435–453, CTA l.483–488, discard alert l.505–509. Copy is in `T` (l.228; en and nl).

| Board | Wrapper props | `pct` | State |
|---|---|---|---|
| `WizardWhereGuest` | `screen=where guest` | 9 | AMS → NRT filled, **no** "From your last trip" hint, Continue on |
| `WizardOutboundGuest` | `screen=outbound guest` | 18 | Times; Continue → Sleep (board `ctaHref: WizardSleep`) |
| `WizardOutboundPastGuest` (+ `…DarkNL`) | `screen=outbound v=past guest` | 18 | Error banner "A new plan needs an upcoming arrival. Check your flight dates." / nl "Voor een nieuw plan is een aankomst nodig die nog moet plaatsvinden. Controleer je vluchtdata." |
| `WizardCheckingGuest` | `screen=checking guest` | 42 | Lines "Flights and time zones" ✓, **"Your answers"** ✓, "What we can tailor" (busy) |
| `WizardFineTuneGuest` | `screen=finetune guest` | 86 | Rows Dark room, Latest you can wake up (both marked), Side of the plane, Coffee tea and soft drinks, Light box or light glasses, Blue-blocking glasses; **no** sleep switch, no Late check-out, no Busy |
| `WizardReadyGuest` (+ `…DarkNL`) | `screen=ready guest` | 95 | Rows Route / Flight / Sleep "23:30–07:15 · In between" / Questions "3 answered" / Extras "3 days prep"; "Price **1 credit**"; "Paid with 1 of the 3 credits you get when you sign up."; CTA "Make my plan" |
| `WizardReadyGuestFree` (+ `…DarkNL`) | `screen=ready v=free guest` | 95 | "Price **Free**"; "Your sign-up credits stay unused." |
| `WizardDiscardGuest` (+ `…DarkNL`) | `screen=outbound alert=guest guest` | 18 | Alert "Discard this trip?" / "What you entered is not saved. You go back to the welcome screen." / "Keep editing" + "Discard" over Outbound |

Guest boards owned by **earlier phases** (render them in guest mode; 11a only routes their exits): `WizardSleep` (9e, the guest sub "On a normal work day."), `WizardQAge`/`WizardQShift*` (9e, the guest disclaimer), `WizardExcluded*` (9e, guest pct 60). Boards owned by **11b**: `GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`, `GuestExcluded`.

Boards that do **not** exist (do not invent): any interim screen between guest Ready and the sign-up sheet; any message for a recovered or expired guest draft (WizardSpec "quiet recovery"); a guest `WizardWhereGuestEmpty` (iOS seeds it for a test only; no capture). The shared `Wizard` props cover the missing Dark/NL twins of WhereGuest, OutboundGuest, CheckingGuest and FineTuneGuest (iOS 18 "Shared Wizard properties"): guard them in dark and nl, capture only the 12 named wrappers.

### 2.3 iOS (`f391a1fa`) — behaviour to mirror

| Source | What | Android target |
|---|---|---|
| `I/plan.md:2062–2090` | Phase 18 decisions and board resolutions | this brief §5 |
| `I/plan.md:2094–2103` (18b) | guest review contract and API | already 0d + 4b; verify (Task 5) |
| `I/plan.md:2104–2114` (18c) | mock guest review | already 4c (OQ-4c-1); verify (Task 5) |
| `I/plan.md:2124–2127` (18e), `Kit/AppCore/GuestSecretStore.swift` (l.1–33), `I/App/Readiness/KeychainGuestSecretStore.swift` (l.1–68), `I/AppTests/GuestSecretKeychainTests.swift`, `KT/AppCoreTests/GuestSecretStoreTests.swift` | secret store contract, memory fake, platform store, tests | Tasks 1–2 |
| `I/plan.md:2128–2136` (18f), `Kit/TripWizard/Model/GuestWizardStore.swift` (l.1–156; **not** `claim`/`reviewExpired`, l.92–112, which are 11b), `KT/TripWizardTests/GuestWizardStoreTests.swift` | guest draft file | Task 3 |
| `I/plan.md:2137–2146` (18g), `Kit/TripWizard/Model/WizardMode.swift` | mode logic | 9a (verify only) |
| `I/plan.md:2147–2157` (18h), `Kit/TripWizard/Generation/GuestWizardGeneration.swift` (l.1–147), `KT/TripWizardTests/GuestWizardGenerationTests.swift`, `Kit/TripWizard/Model/MemberWizardModel.swift:15–28` (protocol defaults) | guest generation | Task 4 |
| `I/plan.md:2172–2186` (18k, 18l), `Kit/TripWizardUI/MemberWizardReadyView.swift` l.58–110, 137–156, 173–215, `Kit/TripWizardUI/MemberWizardSteps.swift` l.67–80, 108–121, 140–154, `Kit/TripWizardUI/MemberWizardView.swift` l.108–137 | guest screens, exits, discard body, Ready rows and price | Tasks 7–9 |
| `I/plan.md:2187–2191` (18m), `Kit/TripWizard/Model/GuestRecoveryGate.swift` (l.1–14), `KT/TripWizardTests/GuestRecoveryGateTests.swift`, `I/App/SceneDelegate.swift:569–596` | quiet recovery after the intro | Tasks 3, 11 |
| `I/App/GuestWizardDestination.swift` (l.1–129) | host: start, close → delete secret → Welcome, foreground expiry, sign-up presentation (sheet = 11b) | Tasks 9–10 |
| `I/App/Session/AuthSessionServices.swift:294–325` (`makeGuestWizard`), l.250–262 (debug guest seeds) | guest wizard factory | Tasks 10, 12 |
| `I/App/Session/NativeSessionController.swift:20–21, 57–59, 155` | store/secret ownership; `recover()` on activation | Task 10 |
| `I/App/Debug/GuestWizardBoardSeed.swift` (l.1–38), `I/App/Debug/WizardCompletionBoardSeed.swift` | guest board seeds | Task 6 |
| `I/AppUITests/GuestWizardScreenTests.swift`, `GuestWizardCompletionScreenTests.swift` (l.1–45 only; its `testGuestSignup*`/`testSignInInstead`, l.46–87, are 11b; *[Refresh R8]*), `GuestRecoveryScreenTests.swift`, `GuestWizardJourneyTests.swift` (up to `wizard.ready.continue`), `KT/TripWizardTests/GuestJourneyCatalogTests.swift` | UI and copy tests | Tasks 13–14 |
| `Kit/AppCore/GuestSessionContinuation.swift` (7 lines) | claim continuation enum | **read only; ported in 11b** (OQ-11a-3) |
| `I/plan.md` "Open questions for the user": OQ 12 (resolved by 18: Plan my trip → `WizardWhereGuest`), OQ 17 (quiet recovery after the Welcome intro), OQ 44 (guest now) | decisions | §5 |

### 2.4 API (web `e36368ec`; contract already in 0d/4b)

`POST /v1/guest/trips/review` — public (no `Authorization`), **90 s** client deadline (A6), rate limits 5/min per IP+device and 30/min per IP (`W/app/Providers/AppServiceProvider.php:201–208`).

Request (`ApiGuestTripReviewInput`, 0d `…/cm/api/ApiGuestTripReviewInput.kt` *[Refresh R3]*; fixture `wizard/guest-review-request.json`): `outbound {legs[1..16]}`, `trip_end_kind: "one_way"` (always), `preparation_days`, `fast_delay_opt_in`, the other journey keys of `ApiTripInput` **without** `return`, `trip_end_nights`, `trip_end_check_out_date` and `override` (`return`/`override` are `prohibited`, `W/app/Http/Requests/Api/V1/GuestReviewTripRequest.php`), `profile` (required; `habitual_bed`, `habitual_wake` required; retired keys → 422), `device_id` (UUIDv7), `guest_secret?` (≤ 128), `draft_token` / `input_revision` / `review_revision` (`null` / `0` / `null` on the first call).

Response: `ApiTripReview` (same as member). `identity.guestSecret` is a 64-hex string minted when `draft_token` is `null` and the sent `guest_secret` is missing or unknown to the server for that device; else `null` (a non-null `draft_token` with no `guest_secret` is stale → `review_expired`) (`W/app/Actions/Api/ReviewGuestTrip.php:40–48`). `price.balance` = `CreditLedger::SIGNUP_CREDITS` = **1** on the server (`W/app/Planning/Credits/CreditLedger.php:16`), **3** on the mock (iOS 18c; 4c); `price.unlimited = false`; `canSave = true` whenever a plan exists.

Errors (already mapped by 4b through `TripWizardErrors.map(f, outboundCount)`): 422 `review_expired` (also inside `validation_failed`) → `DataFailure.ReviewExpired` (a wrong or missing secret with a non-null token looks exactly like expiry); 429 / `throttled` → `DataFailure.Throttled(retryAfterSeconds, 429)`; I/O or deadline → `DataFailure.Offline`.

---

## 3. Already delivered by earlier phases (verify; do not rebuild)

Run these greps first. Each must find exactly the named thing in the merged code. If one fails, stop and report it.

| Thing | Where (expected) | Grep |
|---|---|---|
| `enum class WizardMode { member, guest }` (model) | `…/cw/model/` (9a) | `grep -rn "enum class WizardMode" core/wizard/src/main` |
| `MemberWizardDrafts.guest()` (23:00/07:00, `lastInputStep = sleep`, `firstTrip = false`) | `…/cw/model/MemberWizardDrafts.kt` (9a) | `grep -n "fun guest" core/wizard/src/main -r` |
| `MemberWizardRecord.encode/decode` (strips `guestSecret` in every mode, OQ-9a-10) | `…/cw/model/MemberWizardRecord.kt` | `grep -rn "object MemberWizardRecord" core/wizard/src/main` |
| `savePlan` → `saveOutcome = signUpRequested` (guest), `setGuestAuthenticationPresented`, `setGuestSigningIn`, `guestReviewExpired`, `revisitGuestQuestions`, `guestAnsweredQuestionCount`, `showsReturnRow`, `allowsSleepOverride`, `finishOutboundStep` | `MemberWizardModel*.kt` (9a) | `grep -rn "signUpRequested\|revisitGuestQuestions\|guestAnsweredQuestionCount" core/wizard/src/main` |
| `interface MemberWizardGeneration`, `interface WizardDraftStore`, `class RecoveryRejected` | `…/cw/generation/`, `…/cw/model/` (9a, 9b) | `grep -rnE "interface (MemberWizardGeneration\|WizardDraftStore)\|class RecoveryRejected" core/wizard/src/main` |
| `TripWizardDependencies`, `TripWizardViewModel`, `uiOverride` seam | `…/fw/` (9b, 9f) | `grep -rn "interface TripWizardDependencies" feature/wizard/src/main` |
| `DataSource.reviewGuestTrip` + `ApiDataSource` route (public, 90 s) + `GuestTripReviewAPITests` | `:core:model`, `:core:network` (4b) | `grep -rn "reviewGuestTrip" core/` |
| `MockGuestReview`, `MockGuestReviewTests` (13) + `MockGuestPossessionTests` | `:core:data-mock` (4c) | `grep -rln "MockGuestReview" core/data-mock` |
| `GuestSecrets` (+ `NoGuestSecrets`), `AccountWiper.erase` deleting `guest-wizard.json` | `:core:data` `…/data/session/` (4e) | `grep -rn "interface GuestSecrets" core/data` |
| `SessionHooks.onForeground()` no-op seam (OQ-4e-14) | `:core:data` (4e) | `grep -rn "onForeground" core/data/src/main app/src/main` |
| `Route.GuestWizard`, `RootNavigator.revision` | `:core:navigation` (4e) | `grep -rn "GuestWizard" core/navigation/src/main` |
| `WelcomeDestinations.planMyTrip()` + debug `guestWizard` seam | `…/app/` + `…/appd/DebugWelcomeDestinations.kt` (3c, 5a) | `grep -rn "guestWizard" app/src` |
| `SessionCipher`, `AndroidKeystoreSessionCipher`, `SoftwareSessionCipher`, `DeviceIdentityStore`, `StorageNamespace.keychainService` | `:core:datastore`, `:core:model` (4a) | `grep -rn "interface SessionCipher" core/` |
| `ProfilePreferences.request`, `cleanLegacyJSON`, `containsRetiredValues` | `:core:model` (0d) | `grep -rn "fun cleanLegacyJSON" core/model` |

---

## 4. Out of scope

| Item | Owner |
|---|---|
| The GuestSignup sheet and its failed/retry/review states, `GuestExcluded`, "I already have an account", the claim fields on register/login/passkey, `GuestWizardStore.claim(secrets)` and `reviewExpired(claim)`, `GuestSessionAcceptance`, `GuestSessionContinuation`, `GuestClaim`, `MemberWizardStore.start(seed)` (9b §Task list says "11a's", but it is iOS 18j, the claim; plan.md lists 18j under 11b), `GuestClaimStoreTests`, `GuestSessionAcceptanceTests`, `GuestWizardClaimIntegrationTests`, the Valet claim gate | 11b |
| Anything that sets `state.signingIn = true` (only "I already have an account" does) | 11b (11a only honours the flag on recovery and clears it on start) |
| Re-porting the mock guest review or the API route | done in 4c / 4b |
| Re-porting guest-mode model logic (`WizardMode.swift`) | done in 9a |
| A billing, paywall or website hand-off | never (A14) |
| A recovered/expired-draft message or any screen without a board | none (WizardSpec "quiet recovery") |

---

## 5. Behaviour spec

### 5.1 Entry and host (`:app`)

- **Entry points.**
  - Welcome "Plan my trip" (`welcome.plan`) → `rootNavigator.navigate(Route.GuestWizard)` in **every** build type. Replace the debug `guestWizard` seam (3c/5a). If `DebugWelcomeDestinations` then has no seam left, delete it and its `ReleaseReadinessTest` line, and make `WelcomeDestinations` a single main implementation (5a already made `signIn()` real). Update `WelcomeSeamTest` so that Plan my trip asserts `RootNavigator.state.value.route == Route.GuestWizard` instead of the seam text.
  - Sign in footer "New here? Plan a trip first" (`sign-in.guest`) already navigates to `Route.GuestWizard` (5a). Verify only.
- **Route rendering** *[Refresh R4: today `Route.GuestWizard` falls into `CfRoot`'s `RoutePlaceholder` branch]*. `CfRoot`/`RootContent` renders `Route.GuestWizard` with `GuestWizardRoute` (`…/app/guest/GuestWizardRoute.kt`), which shows 9c's `WizardRoot` with a guest `TripWizardViewModel`.
- **ViewModel scope** [R; OQ-11a-6] *[Refresh R4 supersedes this bullet: the VM and an entry-scoped `GuestWizardHolder` come from `RootContent`'s entry `ViewModelStore` (OQ-A227); no `AppGraph` holder]*. `GuestWizardHolder` (`…/app/guest/`, a lazy `AppGraph` member, process-scoped like 6a's `ShellHolder`) owns one `ViewModelStore`. It is cleared when `RootNavigator.state.route` stops being `Route.GuestWizard` (collect on `appScope` with `dispatchers.main`). Rotation, fold and locale recreation keep the ViewModel (plan "state survival", Wizard row). A later Plan my trip after an exit gets a fresh ViewModel.
- **Back** *[Refresh R4: `RootContent` has no `BackHandler`; verify instead of adding a condition]*. The guest wizard decides its own back (9c §5.4). The root-level back handler of `RootContent` must be disabled while the route is `Route.GuestWizard` (same pattern as 9c's overlay `BackHandler` condition), so system/predictive back never finishes the activity from inside the wizard. First screen (Where) back = ×.
- **Start** (port `makeGuestWizard`, `AuthSessionServices.swift:294–325`), in `GuestWizardDependencies.start()` on `dispatchers.io`:
  1. *[Refresh R2/R4: the VM turns this into `LoadFailed` + `WizardExit.Trips`, not `Loading`; the host must ignore it while signed in.]* If `controller.presence != SignedOut` → throw `CancellationException` (iOS `CancellationError`, `AuthSessionServices.swift:295`). 9b's VM rethrows it, so the VM stays `Loading` and the route shows only the `background` colour (iOS `GuestWizardDestination.swift:31–32`); the host navigates nowhere. A signed-in user cannot reach this route through the gate anyway.
  2. `draft = guestWizardStore.start()` (§5.5).
  3. If `draft.state.signingIn == true`: copy with `signingIn = false`, `revision + 1`, then `guestWizardStore.persist(draft)`.
  4. (debug only) when a board seed is set (§5.9), apply it and `persist` it (iOS l.306–313 persists every seeded board; freezing happens later, on the model, l.320–322).
  5. `ensureActive()`; check `presence == SignedOut` again (else `CancellationException`).
- **Start failure** [F `GuestWizardDestination.swift:83–86`: any non-cancellation error → `navigate(to: .welcome)`] *[Refresh R2: the VM already catches every start exception and emits an exit; do not rewrap; route on the exit]*. `GuestWizardDependencies.start()` catches every exception except `CancellationException` and `DataFailure` and rethrows it as `DataFailure.Server(null)` (9b's VM only maps `DataFailure` to `LoadFailed`; anything else would crash `viewModelScope`). `GuestWizardRoute` navigates to `Route.Welcome` when the VM state becomes `LoadFailed` (no message, no screen: there is no board for it). Test it in `GuestWizardHostTest.startFailureReturnsToWelcome` (a store whose file is a directory).
- **Dependencies** (`GuestWizardDependencies : TripWizardDependencies`, `…/app/guest/`):
  - `store = AppGraph.guestWizardStore`; `source = LazyPublicDataSource { dataSources.public() }` (the signed-out source 5a's `AuthServices` uses; `public()` is `suspend`, *[Refresh R4]*);
  - `airportSearch` = one `AirportSearchService(source)` per `GuestWizardHolder` lifetime (9b pacing and cache rules unchanged; the airport route is public);
  - `newGeneration()` = `GuestWizardGeneration(source, AppGraph.guestSecrets, deviceIdentity = { AppGraph.deviceIdentity.identity() }, parentContext = dispatchers.default)`;
  - `now`, `writerScope = appScope + dispatchers.io`, `checkingClock = WizardCheckingClock.continuous`, `uiOverride` from debug seeds only.
  - If 9b typed `newGeneration()` as `TripGenerationService`, widen it to `MemberWizardGeneration` in a separate `refactor(wizard): …` commit and make `onCleared()` call a non-suspending cancel on any generation (OQ-11a-8). Never make the member path slower or change its behaviour.
- **Close → Welcome** (port `GuestWizardDestination.swift:23–30`). When the VM emits `WizardExit.Welcome` (§5.2): on `appScope + dispatchers.io`, read the draft token the VM last held (`snapshot.draft.form.identity?.token`), call `guestSecrets.delete(token)` when non-null (errors swallowed except `CancellationException`), then on main `rootNavigator.navigate(Route.Welcome)`. The token is not a secret; the secret never leaves the store.
- **Foreground expiry** (port `GuestWizardDestination.swift:56–70`). On each `Lifecycle.Event.ON_START` **after the first** seen by `GuestWizardRoute` (same first-event guard as 9g §5.5): if the VM is active and not closed, call `guestWizardStore.recover()` on io. If it returns a draft → nothing. If it returns `null` (the record expired in the background) **and** the route is still `Route.GuestWizard`: `vm.discard()` (errors swallowed), delete the secret of the last token, navigate to `Route.Welcome`. No message.

### 5.2 Exits, ×, Close and the discard alert (`:feature:wizard`)

- *[Refresh R2/R3: the ×, footer-Close and No-credits guest terms and the guest discard body already exist; this section is verify-only except `WizardExit.Welcome`. There is no `wizard.discard` tag: use `cf.dialog.confirm`/`cf.dialog.dismiss`.]* Add `WizardExit.Welcome` to 9c's `WizardExit` (`…/fw/shell/WizardStepKey.kt`; `ShellViewModel.closeWizard` needs a no-op branch). `close()` emits `if (mode == guest) WizardExit.Welcome else WizardExit.Trips` (9h's edit exit, if merged, keeps its own case).
- × rules (port `MemberWizardSteps.swift:142–150`): guest **and** step `excluded` → `close()`; else `hasChanges` → discard alert; else `close()`. For a guest, `hasChanges` is 9a's (Create: `entered`), so × on an empty Where closes at once (`Wizard.dc.html:244–246`).
- Footer Close on Excluded and on a Checking failure (`MemberWizardSteps.swift:69–76`): `excluded || mode == guest || !hasChanges` → `close()`; else the alert. Verify 9e/9f implemented the `mode == guest` term; if not, fix it in this task.
- No-credits Close (`MemberWizardSteps.swift:108–112`): `mode == guest || !hasChanges` → `close()`. Unreachable for guests (guest `canSave` is true), but keep the term.
- Discard alert body (port `MemberWizardView.swift:129–136`): edit → 9h's body; else guest → **"What you entered is not saved. You go back to the welcome screen."** / nl **"Wat je hebt ingevuld, wordt niet bewaard. Je gaat terug naar het welkomstscherm."**; else member "What you entered is not saved.". Title "Discard this trip?", buttons "Keep editing" / "Discard" (destructive) unchanged. Tag `wizard.discard`.
- Nav title stays "New trip" / "Nieuwe reis" for guests (iOS `MemberWizardSteps.swift:131`).

### 5.3 Guest Ready (`:feature:wizard`, extend 9g's `ReadyUiMapper`, `PriceLine`, `WizardReadyStep`)

**Rows** (order; port `MemberWizardReadyView.swift:58–110`; row look, haptics, large-text stacking and `onClickLabel` "Opens this step" exactly as 9g §5.1):

| Row | Tag | Value | Action |
|---|---|---|---|
| Route | `wizard.ready.route` | 9g `routeSummary` | `revisit(whereTo)` |
| Flight | `wizard.ready.flight` | 9g `flightSummary` | `revisit(outbound)` |
| Return | — | **absent** (`showsReturnRow == false`) | — |
| Sleep | `wizard.ready.sleep` | guest `sleepSummary` (below) | `revisit(fineTune, sleep = true)` (9a sends a guest to `sleep`, `MemberWizardCompletion.swift:128`) |
| Questions (guest only) | `wizard.ready.questions` | `guestAnsweredQuestionCount == 0` → "None needed" / "Niet nodig"; else "{{count}} answered" / "{{count}} beantwoord" with `count` as text. The catalog has a `{{count}} answered_one` twin, so 0c folded it into `R.plurals.wizard_count_answered`; `WizardCopy("{{count}} answered", mapOf("count" to n))` resolves the plural itself *[Refresh R2]* | `Selection` haptic → `revisitGuestQuestions()` through its own `RevisitFromReady` branch (`ReadyRow.Questions`; *[Refresh R2]*) |
| Extras | `wizard.ready.extras` | 9g `extrasSummary` | `revisit(fineTune)` |
| Latest wake-up | `wizard.ready.latest-wake-up` | 9g rule (status `modified` + a `latest_wake` prompt) | 9g |

**Guest `sleepSummary`** (iOS l.137–154): `bed = profile["habitual_bed"] ?: savedProfile["habitualBed"]`, `wake` likewise (a guest has no override); `times` = non-null of `[bed, wake]` joined with "–" (U+2013); then `times + " · " + label`, where `label` = `profile["chronotype"]`: `early` → "Early bird"/"Ochtendmens", `intermediate` → "In between"/"Ertussenin", `late` → "Night owl"/"Avondmens", anything else or missing → "Not sure"/"Weet ik niet". The times are the raw `"HH:mm"` profile strings, unformatted, exactly as iOS l.137–154 and 9g §5.2 `sleepSummary` (no `time_format` conversion).

**Price block** (tag `wizard.ready.price`, only when `review.price != null`; port iOS l.173–215, guest branch):

| # | Line | Shown when | Style |
|---|---|---|---|
| 0 | 9g line 0 ("Price " + **value**) | always | 9g |
| 1 | 9g free line "This trip is free: only a small body-clock shift is needed." | **member only** (`mode == member && !isEditing && credits == free`; iOS l.186 `else if`): a guest never sees it, line G replaces it | 9g |
| G | `credits == free` → "Your sign-up credits stay unused." / "Je credits van je nieuwe account blijven ongebruikt."; else the guest paid line (below) | `mode == guest` | `wizardHint` 13/18, `mutedForeground`; tag `wizard.ready.guest-price` |
| 2 | 9g one-free line | `credits == oneFree` (unreachable one-way; keep) | 9g |
| 3 | 9g "You have {{count}} credits" | **member only** (`mode == member && !price.unlimited && price.cost > 0`, iOS l.192–193) | 9g |

**Guest paid line** [R; OQ-11a-1, OQ-A18]: iOS hard-codes "3" in the key "Paid with {{cost}} of the 3 credits you get when you sign up." (`MemberWizardReadyView.swift:182`; iOS `plan.md:2088` says the singular is "never needed" because iOS assumes the balance is always 3). The server grants `SIGNUP_CREDITS = 1`, and the plan says credits come only from the server. Render `cost = price.cost` and `balance = price.balance` through two Android-only strings, a caller-selected pair (two separate `<string>`s, 0c §5 T1 "caller-selected pairs"; pick `…_one` when `balance == 1`, else the plural one). Both texts come from the canvas: the plural is the board's `guestBalance` with the number made a placeholder, the singular is the canvas note `wiz24cCopy` item 4 ("singular credit line", `DC/canvas.json`). Write the placeholders as `{{cost}}`/`{{balance}}` in `android-only.json` (0c T1 turns them into `%1$s`/`%2$s`; never write `%1$s` into the JSON):
- `android_wizard_guest_price_paid`: en "Paid with {{cost}} of the {{balance}} credits you get when you sign up." / nl "Betaald met {{cost}} van de {{balance}} credits die je krijgt als je een account maakt."; `replaces: [{"table": "wizard", "key": "Paid with {{cost}} of the 3 credits you get when you sign up."}]` (0c table prefix `wizard`, row 13).
- `android_wizard_guest_price_paid_one`: en "Paid with {{cost}} of the {{balance}} credit you get when you sign up." / nl "Betaald met {{cost}} van de {{balance}} credit die je krijgt als je een account maakt."; `replaces: []`; `comment`: "OQ-11a-1: canvas note wiz24cCopy item 4".
- Both `review: "needs review"`; add both rows to plan.md "Android-only copy awaiting approval". With the mock (balance 3) the screen reads exactly as the board and iOS: "Paid with 1 of the 3 credits you get when you sign up." / "Betaald met 1 van de 3 credits die je krijgt als je een account maakt.".

**Button** (9g §5.5, unchanged): "Make my plan" / "Maak mijn plan"; `Medium` haptic → `WizardEvent.Continue(WizardStepKey.Ready)` → `model.savePlan()` *[Refresh R2]*. 9a's guest `savePlan` runs `needsReadyReview` (token present, no failure, unchanged fingerprint, check < 30 min; else back to Checking) and then sets `saveOutcome = signUpRequested`; it never calls `generation.save`. The 1 s `readyTick` and the foreground re-check run as for members.

### 5.4 Guest secret store

- **Contract** (port `GuestSecretStore.swift:3–9`; `…/cm/guest/GuestSecretStore.kt`, package `net.chronoflow.core.model.guest`):
  ```kotlin
  interface GuestSecretStore {
      suspend fun set(secret: String, token: String)
      suspend fun get(token: String): String?
      suspend fun delete(token: String)
      suspend fun rotate(oldToken: String?, newToken: String, secret: String) // set(new) then delete(old) when old != null && old != new
      suspend fun deleteAll()
  }
  class MemoryGuestSecretStore : GuestSecretStore   // Mutex + HashMap; the test fake (iOS l.11–26)
  object GuestRecoveryLocation { fun file(noBackupRoot: File, namespace: StorageNamespace): File = File(File(noBackupRoot, namespace.directoryName), "guest-wizard.json") }
  ```
  `toString()` of anything holding a secret prints `***` (0d rule).
- **Android store** (`KeystoreGuestSecretStore`, `…/cds/KeystoreGuestSecretStore.kt`) [R; plan A5, 06 OQ 12; OQ-11a-4]:
  - Keystore alias `"${namespace.keychainService}.guest"` (`net.chronoflow.app.native.guest`, `net.chronoflow.app.native.mock.guest`), through 4a's `SessionCipher` (AES-256-GCM, AAD = alias, no user authentication). Production passes `AndroidKeystoreSessionCipher`; Robolectric tests pass `SoftwareSessionCipher`.
  - Values in a Preferences DataStore named `"guest_secrets_${namespace.directoryName.lowercase()}"` (one per namespace, created once per process in `AppGraph`, never two instances for one file). Key `"t.$token"`. Value = Base64 (`NO_WRAP`) of `[0x01][12-byte IV][ciphertext + 16-byte tag]` (4a's session format).
  - `token` must be non-empty and ≤ 64 chars (`draft_token` max); otherwise `set`/`rotate` throw `IllegalArgumentException` and `get` returns `null`.
  - `get`: missing → `null`; wrong version byte or < 29 bytes → remove the entry, `null`; `PermanentSessionKeyFailure` → remove **every** entry, `cipher.deleteKey(alias)`, `null` (the next review expires and re-mints, §5.6). Any other exception propagates.
  - `set`: on `PermanentSessionKeyFailure` from `cipher.encrypt`, remove every entry, `cipher.deleteKey(alias)` and retry the encrypt once (4a: the cipher itself does not retry; `KeystoreSessionStore.write` does the same). A second failure propagates.
  - `rotate` = `set(new)` then `delete(old)` (same order as iOS, so a crash between the two keeps the new secret).
  - `deleteAll` clears the DataStore; the key alias stays.
  - Every call runs in `withContext(io)`; never logs; never puts a secret into an exception message.
  - Backup: the DataStore file lives under `filesDir/datastore/`, which A5's `dataExtractionRules` already exclude (verify the rule covers it; if not, add it in this task).
- **Sign-out and teardown.** `AppGraph` builds `AccountWiper(…, guestSecrets = object : GuestSecrets { override suspend fun deleteAll() = guestSecrets.deleteAll() })` instead of `NoGuestSecrets`. 4e's `erase` already deletes `guest-wizard.json` after `deleteAll()` (iOS 18e: member sign-out and account teardown delete every guest secret and the guest record).

### 5.5 Guest draft file (`GuestWizardStore`, port `GuestWizardStore.swift:1–90, 114–155`)

Location [R; OQ-11a-5]: `…/cw/model/GuestWizardStore.kt`, pure JVM (java.io/nio only), so its tests are plain JVM. If a Konsist rule forbids file I/O in `:core:wizard`, put it in `:core:data` with the same API and still test it as plain JVM.

```kotlin
class GuestWizardStore(
    private val file: suspend () -> File,           // { GuestRecoveryLocation.file(storageRoot(), namespace) }; resolved inside withContext(io) on first use, never in a constructor (4e §5.5 "Storage root")
    private val io: CoroutineDispatcher,
    private val now: () -> Instant,
) : WizardDraftStore {
    suspend fun start(): MemberWizardDraft          // recover() ?: write(record(MemberWizardDrafts.guest()))
    suspend fun recover(): MemberWizardDraft?
    suspend fun recoveryDecision(handoffFinished: Boolean, introFinished: Boolean): GuestRecoveryGate.Decision
    override suspend fun persist(draft: MemberWizardDraft)
    override suspend fun discard(instance: String)
    override suspend fun didSave(instance: String, firstTrip: Boolean)   // = discard(instance); no reminders offer, no profile
}
```

- **One `Mutex`** serializes every call; every call runs in `withContext(io)`.
- **File JSON** (`ApiJson.response`): `{"form": <JsonElement>, "state": <JsonElement>, "fieldErrors": <JsonElement>, "updatedAt": "<ISO-8601 UTC with milliseconds>"}`. `form`/`state`/`fieldErrors` are 9a's `MemberWizardRecord.encode(draft)` payload strings parsed back into `JsonElement` (so the guest file and the member row share one codec and one secret strip). `updatedAt` = `now().truncatedTo(MILLIS).toString()`.
- **Write** (`write(record)`): `parentFile.mkdirs()`; bytes = `ProfilePreferences.cleanLegacyJSON(encoded)`; write to `guest-wizard.json.tmp` in the same directory, `fsync`, then `Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)`. No backup flag is needed (it is under `noBackupFilesDir`).
- **`record(draft, updatedAt = null)`** (iOS l.114–128), applied before every write: `form.identity.guestSecret = null`, `state.review.identity.guestSecret = null` (9a's encode already does this; keep it, it is cheap), `mode = guest`, `lastInputStep = sleep`, `sleepNeeded = true`, `savedProfile = null`, `firstTrip = false`, `discardRetiredValues()`; `updatedAt` = the given value or now.
- **`recover()`** (iOS l.34–75):
  1. No file → `null`.
  2. Read bytes; decode through `cleanLegacyJSON` then `MemberWizardRecord.decode`. Any decode failure, or `null` → delete the file, `null`.
  3. `updatedAt` unparsable, or `GuestRecoveryGate.decision(updatedAt, now(), handoffFinished = false, introFinished = false, signingIn) == expire`, or `legs` ∉ 1…16, or `stays.size != legs.size`, or `form.end.kind != "one_way"` → delete the file, `null`.
  4. Re-write the cleaned record **with the same `updatedAt`** (recovery never extends the expiry), return the draft (`airports = state.airports`).
- **`persist(draft)`** (iOS l.77–83): `current = recover()`; `current == null || current.state.instance != draft.state.instance` → throw 9b's `RecoveryRejected` (iOS throws `CancellationError`; Kotlin must not use `CancellationException` for this, 9b §6.5); `current.state.revision > draft.state.revision` → return without writing; else write `record(draft)` with `updatedAt = now` (sliding expiry).
- **`discard(instance)`**: delete the file only when `recover()?.state.instance == instance`; never fails when absent.
- **`recoveryDecision`**: `recoverRecord()` → `null` → `none`; else `GuestRecoveryGate.decision(updatedAt, now(), handoffFinished, introFinished, signingIn = state.signingIn == true)`.
- **Never** writes a secret, a retired profile key, `savedProfile`, `firstTrip = true` or a reminders offer.
- The start draft (iOS l.23–31 via 9a `MemberWizardDrafts.guest()`): step `whereTo`, `form.end.kind = "one_way"`, `preparation.days = 3`, `profile = {"habitual_bed":"23:00","habitual_wake":"07:00"}`, `savedProfile = null`, `firstTrip = false`.

**`GuestRecoveryGate`** (`…/cw/model/GuestRecoveryGate.kt`, verbatim port of `GuestRecoveryGate.swift`):
```kotlin
object GuestRecoveryGate {
    enum class Decision { none, wait, expire, wizard, signIn }
    fun decision(updatedAt: Instant?, now: Instant, handoffFinished: Boolean, introFinished: Boolean, signingIn: Boolean): Decision
    // updatedAt == null → none; now − updatedAt >= 30 min (1_800_000 ms) → expire; !(handoff && intro) → wait; signingIn → signIn else wizard
}
```

### 5.6 Guest generation (`GuestWizardGeneration`, port `GuestWizardGeneration.swift:1–147`)

`…/cw/generation/GuestWizardGeneration.kt`:
```kotlin
class GuestWizardGeneration(
    private val source: DataSource,
    private val secrets: GuestSecretStore,
    private val deviceIdentity: suspend () -> String,          // DeviceIdentityStore.identity()
    private val sleep: suspend (Double) -> Unit = { delay((it * 1000).roundToLong()) },
    parentContext: CoroutineContext = Dispatchers.Default,
) : MemberWizardGeneration
```
- **`review(instance, input, edit)`**: `edit != null` → throw `DataFailure.Server(null)` (iOS protocol default, `MemberWizardModel.swift:15–21`). Then:
  1. `ensureActive()`; `cancelled` → throw `CancellationException`.
  2. Normalize: `profile = ProfilePreferences.request(input.profile ?: JsonObject(emptyMap()))`; `identity = input.identity?.copy(guestSecret = null)`; `input.return = null`; `tripEndKind = "one_way"`; `tripEndNights = null`; `tripEndCheckOutDate = null`; `override = ApiTripInputOverride(use = false)`.
  3. `key = ReviewKey(instance, fingerprint = <canonical sorted-key JSON of the whole normalized request, identity included>)` (iOS encodes the full input with `.sortedKeys`; use 9a's `WizardFingerprint.canonical` if it accepts the identity, else the same canonical encoder).
  4. Under a `Mutex`: an existing in-flight `Deferred` for `key` is joined; else `current[instance] = key` and a new LAZY `Deferred` (fresh id) races `performReview` against `sleep(30.0); throw DataFailure.Server(null)` (the deadline-`launch` pattern of `TripGenerationService.review` l.91–112; there is no `race` helper, *[Refresh R3]*).
  5. After `await()`: if the caller is cancelled, or `cancelled`, or `current[instance] != key` → `secrets.delete(result.review.identity.token)` and throw `CancellationException`. Else return. In `finally` (`NonCancellable`, under the mutex) remove the key only if its id is still this one. Caller cancellation cancels the flight only if it is still this id (iOS `onCancel` → `cancelReview(key, id)`).
- **`performReview`** (iOS l.83–116):
  1. `oldToken = input.identity?.token`; `secret = oldToken?.let { secrets.get(it) }`; `deviceID = deviceIdentity()`; `ensureActive()`.
  2. `request = ApiGuestTripReviewInput(input.input, input.identity, input.profile ?: {}, deviceID, secret)`; `review = send(request)`.
  3. On `DataFailure.ReviewExpired`: if `oldToken != null && isCurrent(key, id) && active` → `review = send(request.copy(identity = null))` **with the same `guestSecret`** (the server reuses a known device secret for a fresh draft); else rethrow. A second `ReviewExpired` propagates.
  4. `token = review.identity.token`; `next = review.identity.guestSecret ?: secret`; `next != null` → `secrets.rotate(oldToken, token, next)`.
  5. Not `isCurrent` or not active → `secrets.delete(token)`, throw `CancellationException`.
  6. Return `TripGenerationReviewResult(input = input.copy(identity = review.identity.copy(guestSecret = null)), review = review.copy(identity = review.identity.copy(guestSecret = null)))`.
- `send(i)` = `source.reviewGuestTrip(i)`, catching `DataFailure` → `throw TripWizardErrors.map(f, i.input.outbound.legs.size)`.
- `isCurrent(key, id)` = `!cancelled && current[key.instance] == key && reviews[key]?.id == id`.
- **`save(…)`** → throw `DataFailure.Server(null)` (protocol default); never reached in guest mode.
- **`cancelReviews(instance)`**: cancel and remove every flight of that instance; `current.remove(instance)`.
- **`cancel()` / `cancelNow()`**: `cancelled = true` (`@Volatile`), cancel the job, clear both maps.
- **Never** touches the account profile, settings, sync or `TripGenerationEffects`.

### 5.7 Quiet recovery after the Welcome intro (port `SceneDelegate.swift:569–596`; OQ 17)

`GuestRecoveryScheduler` (`…/app/guest/GuestRecoveryScheduler.kt`, JVM-testable; inject `AppDispatchers`, the store, the navigator, a `presence: suspend () -> SessionPresence` and the session epoch):
- **When it runs:** once per process (`checked` flag, OQ-11a-7), the first time all hold: 3b `handOffComplete` is true (the overlay is gone), `RootNavigator.state.route == Route.Welcome`, and `controller.launchIntro?.frozenTime == null` (a frozen diagnostic intro never recovers). Start it from `CfRoot` with a `LaunchedEffect` keyed on those values.
- **Steps:**
  1. Capture `revision = rootNavigator.state.value.revision` and the session epoch.
  2. If `launchIntro != null`: `launchIntro.clock.sleepUntil(launchIntro.start + 900.milliseconds)` (the last intro item lands at `start + 900`, 3200 ms on a normal launch; 3c §5.5). No intro (Reduce Motion, warm, `screen` profile) → no wait.
  3. Abort unless: not cancelled, same epoch, same `revision`, route still `Welcome`, `presence() == SignedOut`.
  4. `decision = guestWizardStore.recoveryDecision(handoffFinished = true, introFinished = true)` on io.
  5. Abort unless same epoch and same `revision`.
  6. `wizard` → `navigate(Route.GuestWizard)` (the VM's `start()` recovers the step, values and sheet); `signIn` → `navigate(Route.SignIn)`; `none`/`wait`/`expire` → nothing (`expire` already deleted the file inside the store).
  7. Any exception except `CancellationException` → nothing.
- The scheduler's job is cancelled when the session epoch changes (iOS `connectionGeneration`) or the activity is destroyed.
- **Never** pushes Welcome away during the moon flight, never shows a message, never runs when the board-capture or recovery seeds want a fixed screen (`CF_WIZARD_BOARD_CAPTURE` set without `CF_GUEST_RECOVERY_BOARD`).
- **Foreground sweep.** `SessionHooks.onForeground()` (4e) calls `guestWizardStore.recover()` first (iOS `NativeSessionController.swift:155`, `_ = try? await guestWizardStore.recover()`), swallowing every error except `CancellationException`, then the existing hooks (12b's reminders) in their order.

### 5.8 Sign-up request seam (until 11b)

- `GuestSignUpLauncher` (`…/fw/guest/GuestSignUpLauncher.kt`): `fun interface GuestSignUpLauncher { fun request(onDismissed: () -> Unit) }`. `TripWizardDependencies` gains `val signUpLauncher: GuestSignUpLauncher? get() = null` (member: null).
- The VM watches `snapshot.saveOutcome`: when it becomes `signUpRequested` and `!guestAuthenticationPresented` (port `GuestWizardDestination.presentSignup` l.90–98): `setGuestAuthenticationPresented(true)`; `wizard.flush()`; if not closed and not `persistenceFailed` → `signUpLauncher.request(onDismissed)`; else `setGuestAuthenticationPresented(false)`.
- `onDismissed` (port `signupDismissed`, l.121–128, without the 11b review branch): `setGuestAuthenticationPresented(false)`; if the route is still the guest wizard → `readyTick()`.
- **11a binding** [R; OQ-11a-2]: `:app` binds a launcher that calls a `onRequested: () -> Unit` hook and then `onDismissed()` at once. Debug passes a hook that increments `GuestWizardDebugCounters.signUpRequested` (an `AtomicInteger` in a debug-only `object`, `app/src/debug/kotlin/net/chronoflow/debug/GuestWizardDebugCounters.kt`; AGENTS Tests 8, the same per-feature pattern as 6d's `RefusedChangeDebug.presentedCount` and 9h's `TripActionsDebugCounters`; 0e has no shared counter). Debug supplies the hook through the variant hook 4e already uses for debug-only wiring; release and perf pass `{}`. No UI is shown: there is no board for anything between Ready and GuestSignup. 11b replaces the binding with the sheet.

### 5.9 Seeds (debug only; iOS names)

| Seed | Effect |
|---|---|
| `CF_GUEST_WIZARD=1` | After `TestSeeds.apply`, signed out: `rootNavigator.navigate(Route.GuestWizard)` (iOS `AuthSessionServices.swift:259–262`). |
| `CF_WIZARD_BOARD_CAPTURE=<board>` with `CF_GUEST_WIZARD=1` or `CF_GUEST_WIZARD_JOURNEY=1` | `start()` applies `GuestWizardBoardSeed.draft(draft, board, locale)` and persists it to the real guest store. Completion and Checking boards are frozen through 9f's `WizardUiOverride` exactly as 9f/9g freeze the member twins; `WizardGuestJourney` is never frozen. |
| `CF_GUEST_WIZARD_JOURNEY=1` | Like `CF_GUEST_WIZARD` but stays on Welcome (the journey taps Plan my trip). 11b adds the sign-up prefill. |
| `CF_GUEST_RECOVERY_BOARD=<board>` | `guestWizardStore.start()`, apply `GuestWizardBoardSeed.draft(…, Locale.UK)`, `persist`, then a normal launch (iOS l.253–257). The quiet recovery then opens it. |

**`GuestWizardBoardSeed`** (`…/fwd/GuestWizardBoardSeed.kt`, port `GuestWizardBoardSeed.swift`):
- Map the board to the shared seed: `WizardGuestJourney` → `WizardSaveJourney`; `WizardWhereGuest` → `WizardWhere`; `WizardWhereGuestEmpty` → `WizardWhereEmpty`; `WizardOutboundGuest` → `WizardOutbound`; `WizardOutboundPastGuest` → `WizardOutboundPast`; `WizardCheckingGuest` → `WizardChecking`; `WizardDiscardGuest` → `WizardOutbound`; `WizardReadyGuest` → `WizardReady`; `WizardReadyGuestFree` → `WizardReadyFree`; any `GuestSignup*` → `WizardReady`; anything else → itself (`WizardFineTuneGuest`, `WizardSleep`, `WizardQAge`, `WizardQShift*`, `WizardExcluded*`). Strip a trailing `DarkNL`/`Dark`/`NL` first (9c rule).
- `draft = WizardBoardCaptureSeed.draft(shared, locale)` with 9f's `WizardCompletionBoardSeed` applied, then: `mode = guest`, `fromLastTrip = false`, `sleepNeeded = true`, `lastInputStep = sleep`, `firstTrip = false`.
- `WizardGuestJourney`: `step = whereTo`, `profile.shift_work = false` (so the mock asks only the age question).
- Step `fineTune` or `ready`: `sleepCompleted = true`, `profile.age_band = "18_64"`, `profile.shift_work = false` (so Questions reads "2 answered").
- `WizardOutboundPastGuest`: `fieldErrors["review"] = FieldMessage("A new plan needs an upcoming arrival. Check your flight dates.")`.
- `WizardDiscardGuest`: also `discardDialogVisible = true` (9c [R] precedent, OQ-9c-5).
- Then run 9a's `enforceGuestMode()` semantics (the model does it at init).
- **`TestSeeds.apply` reset** [R]: delete `<storage root>/ChronoFlowMock/guest-wizard.json` (+ `.tmp`; the storage root is 4e's resolved `storageRoot()`, which `GraphOverrides.storageDir` may replace) and call `guestSecrets.deleteAll()` for the mock namespace on every seed, so a re-seed equals a cold seed.

### 5.10 Copy (every string en + nl, from resources)

Generated from the iOS catalogs (0c) — find each name with `grep -rn "<en text>" core/ui/src/main/res/values/`: "What you entered is not saved. You go back to the welcome screen.", "Your sign-up credits stay unused.", "Questions", "None needed", "{{count}} answered", "Early bird", "In between", "Night owl", "Not sure", "On a normal work day.", "Your answers", "A new plan needs an upcoming arrival. Check your flight dates.", "Ready to plan", "Make my plan", "New trip". Android-only: the two `android_wizard_guest_price_paid*` strings (§5.3). Nothing else is new.

### 5.11 Edge cases (each has a test in §7)

1. × on an empty guest Where → Welcome at once, no alert, no file left.
2. Discard while a first review is in flight → the minted secret is deleted, the result dropped, no file, Welcome.
3. A second review with a new token → old secret deleted, new kept; same token → secret replaced.
4. Server returns `guestSecret: null` for a known device secret → the store keeps the known secret under the new token.
5. `ReviewExpired` with a token → one retry without identity, same secret; a second expiry surfaces; a first-call expiry is not retried.
6. Keystore key permanently invalidated → `get` returns `null`, all entries removed; the next review expires and re-mints.
7. Guest file at 29:59.999 recovers; at 30:00.000 it is deleted (`expire`).
8. Corrupt or legacy file (retired keys, a plaintext `guestSecret`) → cleaned on recovery, never re-written with them.
9. A stale writer (old instance) after discard or after a new `start()` → `RecoveryRejected`, file unchanged.
10. App backgrounded > 30 min with the wizard open → on return, Welcome, secret deleted, no message.
11. Cold launch with a live draft → Welcome intro plays fully, then the wizard opens on the saved step; with `signingIn` → Sign in.
12. Navigation during the intro (user taps Sign in) → revision changed → no recovery.
13. Signed in (presence ≠ SignedOut) → guest start aborts; recovery never runs.
14. Sign-out (member) → every guest secret and the guest file are deleted (4e `erase`).
15. Mock (balance 3) vs server (balance 1) → "of the 3 credits" vs "of the 1 credit" (OQ-11a-1).

---

## 6. Kotlin shape [R]

| Class / change | Module, package | Notes |
|---|---|---|
| `GuestSecretStore`, `MemoryGuestSecretStore`, `GuestRecoveryLocation` | `:core:model` `net.chronoflow.core.model.guest` | JVM |
| `KeystoreGuestSecretStore(dataStore, alias, cipher: SessionCipher, io)` + `companion fun forNamespace(context, namespace, cipher, io)` | `:core:datastore` `net.chronoflow.core.datastore` | Android lib |
| `GuestWizardStore`, `GuestRecoveryGate` | `:core:wizard` `net.chronoflow.core.wizard.model` | JVM |
| `GuestWizardGeneration` | `:core:wizard` `net.chronoflow.core.wizard.generation` | JVM |
| `WizardExit.Welcome`; guest × / Close rules; guest discard body | `:feature:wizard` `…wizard.shell` | edit 9c files |
| `ReadyUiMapper` guest rows, `PriceLine.GuestPaid(cost, balance)`, `PriceLine.GuestFree`, `ReadyRow.Questions` *[Refresh R2]* | `:feature:wizard` `…wizard.ready` | edit 9g files |
| `GuestSignUpLauncher`, `TripWizardDependencies.signUpLauncher` | `:feature:wizard` `…wizard.guest` | |
| `GuestWizardBoardSeed` | `feature/wizard/src/debug` `…wizard.debug` | debug only |
| `GuestWizardDependencies`, `GuestWizardHolder`, `GuestWizardRoute`, `GuestRecoveryScheduler` | `:app` `net.chronoflow.app.guest` | |
| `AppGraph.guestSecrets`, `AppGraph.guestWizardStore` (no `AppGraph` holder; *[Refresh R4]*) | `:app` (edit) | `by lazy`, no I/O in construction |
| `AccountWiper(guestSecrets = …)`, `SessionHooks.onForeground` | `:app` wiring (edit) | |
| `CF_GUEST_WIZARD`, `CF_GUEST_WIZARD_JOURNEY`, `CF_GUEST_RECOVERY_BOARD` in `TestSeeds` / `DebugSeedRouting` | `app/src/debug` (edit) | |
| `GuestWizardDebugCounters` (`signUpRequested: AtomicInteger`) | `app/src/debug` `net.chronoflow.debug` | debug only (§5.8) |

Module edges you may add: `:core:wizard` → `:core:model` (exists); `:core:datastore` → `:core:model` (exists); `:app` → all (exists). Do not add `:core:wizard` → `:core:datastore` or `:core:data`.

---

## 7. Tasks (one commit each, in order)

Run `make targeted ONLY=…` for each task's tests, and `make quick` once before the last commit.

### Task 1. `feat(model): add the guest secret store contract`
- Files: `…/cm/guest/GuestSecretStore.kt` (interface, `MemoryGuestSecretStore`, `GuestRecoveryLocation`).
- Tests (JVM, `core/model/src/test/…/guest/GuestSecretStoreTests.kt`, Swift names, against `MemoryGuestSecretStore`): `rotationStoresNewSecretAndDeletesOnlyOldToken`, `firstMintAndSameTokenRotationRetainNewSecret`, `discardIsPerTokenAndIdempotent`, `deleteAllRemovesEveryTokenAndAllowsFreshMint` (exact values from `KT/AppCoreTests/GuestSecretStoreTests.swift`). Android: `GuestRecoveryLocationTest.pathPerNamespace` → `…/ChronoFlow/guest-wizard.json` and `…/ChronoFlowMock/guest-wizard.json`.
- Move `MemoryGuestSecretStore` to `:core:testing` only if `:core:model` test fixtures are not visible to `:core:wizard` tests (9b Task 0 precedent).

### Task 2. `feat(datastore): keep guest secrets under a keystore key`
- Files: `…/cds/KeystoreGuestSecretStore.kt`; `AppGraph.guestSecrets` (lazy; `AndroidKeystoreSessionCipher`, `dispatchers.io`); `AccountWiper` wiring (§5.4); `dataExtractionRules` check; plan.md "Android spec: storage layout" row "Guest secret" → `Keystore alias <keychainService>.guest; Preferences DataStore guest_secrets_<dir> (key t.<token>)`.
- Tests:
  - Robolectric `@Logic` `KeystoreGuestSecretStoreTest` (`core/datastore/src/test/…`, `SoftwareSessionCipher`, a temp DataStore): the four Swift cases above against the real store; `fileHoldsNoPlaintextSecret` (read the `.preferences_pb` bytes: no `"minted-secret"` substring); `aliasIsNamespaced` (`net.chronoflow.app.native.guest`, `…mock.guest`); `permanentKeyFailureForgetsEverySecret` (`failNext = PermanentSessionKeyFailure`) → `get` null, both entries gone, `deleteKey(alias)` called once; `badVersionByteDropsOnlyThatEntry`; `rejectsEmptyAndOverlongTokens`; `runsOnTheInjectedDispatcher` (a `StandardTestDispatcher` must be advanced for `get` to complete).
  - Emulator `@Smoke` `GuestSecretKeystoreTest.testGuestSecretsRotateDiscardAndClearWithDeviceOnlyAccessibility` (`app/src/androidTest/kotlin/net/chronoflow/app/store/GuestSecretKeystoreTest.kt`, beside 4a's `KeystoreSessionStoreTest`, which `make check` already runs; real `AndroidKeystoreSessionCipher`, a temporary DataStore under `cacheDir` resolved in `@Before`, alias `test.<uuid>.guest` deleted in `@After`; < 5 s; port of `I/AppTests/GuestSecretKeychainTests.swift:9`: set → rotate → delete → deleteAll; the Android "device only" check = the key's `KeyInfo.isInsideSecureHardware` is **not** asserted (emulators vary), instead assert `KeyInfo.isUserAuthenticationRequired == false` and the alias name).
  - **Implementation note (test-cleanup boundary):** extend the already approved `AppGraphSignOutWiringTest` with the real guest secret and draft assertions; do not add a graph-booting class to the Konsist allowlist.
  - JVM `AccountWiperTests` (4e) stays green; add `memberSignOutClearsRealGuestSecretStore` only if 4e's test cannot already observe the adapter (it uses its own fake: then add an `:app` Robolectric `AppGraphGuestWiringTest.signOutDeletesGuestSecretsAndDraft`).

### Task 3. `feat(wizard): add the guest recovery gate and the guest draft file`
- Files: `…/cw/model/GuestRecoveryGate.kt`, `…/cw/model/GuestWizardStore.kt`; `AppGraph.guestWizardStore` (`file = { GuestRecoveryLocation.file(storageRoot(), namespace) }` with 4e's `storageRoot: suspend () -> File`, `dispatchers.io`, the app clock; `noBackupFilesDir` is resolved on io inside the first call, never on main or in construction, 4a/4e rule).
- Tests (JVM, `…/cwt/model/`, a temp dir under `build/guest-store-tests/<uuid>`, 9a's `WizardTestClock`):
  - `GuestRecoveryGateTests` (Swift names): `noRecordDoesNotRecover` (signingIn false/true), `liveRecordResumesItsDestination` (ages 0, 1740 s, 1799.999 s → `wizard`; with signingIn → `signIn`), `waitsForBothLaunchStages` (4 handoff×intro combos × signingIn), `expiryWinsDuringTheIntroAndInTheBackground` (1800, 1860 × finished false/true → `expire`).
  - `GuestWizardStoreTests` (Swift names): `roundTripAndInitialValues` (step `whereTo`, `one_way`, preparation 3, `savedProfile == null`, `firstTrip == false`, bed 23:00 / wake 07:00; persist step `sleep`, revision 2, sheet `(0, to)`, `fieldErrors["review"]` → a new store instance recovers an equal draft; the `Api` namespace file does not exist), `recoveryExpiry` (1740, 1799.999, 1800, 1860: recovers below 1800, else `null`, and `persist` throws `RecoveryRejected`), `stripsRetiredValuesAndSecretsOnWriteAndLegacyRecovery` (`"SECRET"` and `pregnancy` never in the bytes; a hand-edited legacy file with `"LEGACY_SECRET"` and `pregnancy: true` recovers clean and is re-written clean; use `ProfilePreferences.containsRetiredValues`), `recoveryRemembersSignInWithoutExtendingTheExpiry` (persist `ready` + `signingIn`; +1740 s: `wait` then `signIn`, `recover()?.step == ready`; +60 s more: `none` and the file is gone), `guardsRevisionsInstancesDiscardAndSave` (exact sequence of the Swift test, `RecoveryRejected` for `CancellationError`).
  - Android: `GuestWizardStoreTests.memberStoreBehaviourUnchanged` is not needed (9b's tests cover the member store); instead `oneWayOnly` (a file whose `form.end.kind == "return_legs"` is deleted, `null`), `writeIsAtomic` (a leftover `.tmp` does not break `recover()`).
- Ticks plan.md 11a "**GuestSecretStore**" (store + file are both done now).

### Task 4. `feat(wizard): port guest generation`
- Files: `…/cw/generation/GuestWizardGeneration.kt`; if needed, the `refactor(wizard): widen the generation dependency` commit first (OQ-11a-8).
- Tests (JVM, `…/cwt/generation/GuestWizardGenerationTests.kt`, all 11 Swift cases by name, with a recording fake `DataSource` whose `reviewGuestTrip` dequeues results, `MemoryGuestSecretStore`, a fixed device id, a virtual-time `sleep`):
  `firstCallMintsSecretAndUsesSleepAnswersAndDeviceWithoutAccountEffects` (sent identity and secret null; profile bed 23:00, wake 07:00, chronotype intermediate, age 18_64, shift false, **no** `pregnancy`; device id sent; secret stored for "first"; result identities have no secret; `return == null`, `trip_end_kind == one_way`, `override.use == false`; only one source call; `save` throws `DataFailure`), `knownSecretIsReusedAndANewTokenRotatesIt`, `expiryRetriesOnceWithoutIdentityAndRetainsKnownSecretForNewToken`, `secondExpirySurfacesAndFirstCallExpiryIsNotRetried`, `failuresSurfaceWithoutRetry` (loop: `Offline`, `Throttled(42, 429)`), `discardDuringRequestDeletesMintedSecretAndDropsResult`, `cancelledReviewDoesNotRetryExpiryAndAllowsNextCheck`, `staleTokenDoesNotRetryAfterAnotherReviewChangesIt`, `expiryInsideValidationUsesSharedErrorMapping`, `discardWhileSecretIsBeingStoredCleansItUp`, `duplicateReviewsShareOneRequestAndTimeoutIsThirtySeconds` (two concurrent identical reviews → one source call; a hung source fails with `Server(null)` at exactly 30.0 s virtual time). Port the fixtures `guestGenerationInput()` / `guestReview(token, secret)` from the Swift file (l.254–273).

### Task 5. `test(wizard): run guest generation against the mock server`
- No production code. Proves 4b/4c cover 18b/18c for this phase (OQ-4c-1).
- Test (JVM, `core/data/src/test/kotlin/net/chronoflow/core/data/wizard/GuestGenerationMockTest.kt`, *[Refresh R8]*; `:core:data` tests already see `:core:data-mock` and `:core:wizard`): `MockDataSource` (4c, zero latency) + `GuestWizardGeneration` + `MemoryGuestSecretStore`: `firstReviewMintsA64HexSecretAndBalanceIsThree`, `secondReviewReusesTheStoredSecret` (response `guestSecret == null`; store unchanged under the new token), `forgottenSecretExpiresThenMintsAFreshDraft` (delete the secret, review with the old identity → one retry → new token, new secret), `onlyAgeAndShiftPromptsBlock`, `pastArrivalUsesGuestCopy` (`review` field = "A new plan needs an upcoming arrival. Check your flight dates.").
- Run 4b's `GuestTripReviewAPITests` and 0d's `GuestTripReviewContractTests` unchanged. Tick plan.md 11a "**Guest review endpoint.**" and append "(API in 4b, mock in 4c; verified by `GuestGenerationMockTest`)".

### Task 6. `feat(wizard): add guest board seeds`
- Files: `…/fwd/GuestWizardBoardSeed.kt` (§5.9).
- Tests (JVM, `feature/wizard/src/testDebug/…/GuestWizardBoardSeedTest.kt`): one case per name — `WizardWhereGuest` (step `whereTo`, `fromLastTrip == false`, `mode == guest`), `WizardWhereGuestEmpty` (`entered == false`), `WizardOutboundGuest`, `WizardOutboundPastGuest` (+ the guest review error), `WizardCheckingGuest` (step `checking`), `WizardFineTuneGuest` (step `fineTune`, `sleepCompleted`, age `18_64`, shift false, no prompt of kind `lateCheckOut` or `preDepartureBusyHours`), `WizardReadyGuest` (step `ready`, `price.cost == 1`, `balance == 3`, `guestAnsweredQuestionCount == 2`), `WizardReadyGuestFree` (`credits == free`), `WizardDiscardGuest` (step `outbound`, `discardDialogVisible`), `WizardGuestJourney` (step `whereTo`, HND, departure ≥ 29 days after now, shift false, not frozen); every seed has `lastInputStep == sleep`, `firstTrip == false`, `form.end.kind == "one_way"`.

### Task 7. `feat(wizard): route guest exits to welcome`
- Files: 9c's `WizardExit` (+ the no-op branch in `ShellViewModel.closeWizard`); the close/× code and the discard body already have the guest terms (*[Refresh R2]*, verify only).
- Tests:
  - JVM `TripWizardViewModelGuestExitTest` (`…/fwt/`; *[Refresh R8: add only the cases that do not exist yet]*): `guestCloseExitsToWelcome`, `memberCloseStillExitsToTrips`, `guestExcludedCrossClosesWithoutAlert`, `guestCrossWithChangesAsks`, `guestCrossOnEmptyWhereClosesAtOnce`, `guestFailureCloseNeverAsks`, `exitIsEmittedOnce` (double tap).
  - *[Refresh R8: no copy on screen; the body key is already covered by `WizardDiscardUiTest`; keep only the Keep-editing structure check with `cf.dialog.dismiss`.]* Robolectric `@Logic` `GuestDiscardDialogTest`: the guest body text in en and nl; "Keep editing" returns to Outbound (`wizard.outbound.continue` exists); member body unchanged.

### Task 8. `feat(wizard): show the guest ready rows and price`
- Files: 9g `ReadyUiMapper`, `ReadyUi.kt` (`ReadyRow.Questions`, `PriceLine`), `WizardReadyStep`, the VM's `RevisitFromReady` branch (*[Refresh R2]*); `tools/l10n/android-only.json` (+ `make strings`, generated files committed with it); plan.md "Android-only copy awaiting approval" (two rows).
- Tests:
  - JVM `GuestReadyUiMapperTest`: `rowsAreRouteFlightSleepQuestionsExtras` (no Return); `sleepSummaryNamesTheChronotype` (early/intermediate/late/missing → the 4 labels; "23:30–07:15 · In between"); `questionsCountsOnlyAnsweredScreeningFields` (0 → "None needed", 1, 2; a `null` value does not count); `priceLinesForPaidAndFree` (cost 1/balance 3 → `GuestPaid(1, 3)`; free → `GuestFree`; never the member balance line); `balanceOnePicksTheSingularString` (cost 1/balance 1 → the `_one` resource); `latestWakeRowStillFollowsTheMemberRule`.
  - Robolectric `@Logic` `GuestReadyScreenTest`: tags `wizard.ready.questions`, `wizard.ready.guest-price` exist, `wizard.ready.return` absent; tapping Questions records one `Selection` haptic and shows the age question; tapping "Make my plan" records one `Medium` haptic.
  - 0c's `StringParityTest` stays green (two new names, en and nl, same placeholders).

### Task 9. `feat(wizard): hand guest ready to a sign-up launcher`
- Files: `…/fw/guest/GuestSignUpLauncher.kt`; VM wiring (§5.8).
- Tests (JVM `TripWizardViewModelGuestSignUpTest`, port of the VM half of `GuestWizardModeTests.readyWaitsForTheSignupSheetAndDismissalAllowsAnotherRequest` and `liveReadyRequestsSignUpWithoutSaving`): one request per `signUpRequested`; a second tap while presented requests nothing; after `onDismissed` a new tap requests again; a stale Ready (fingerprint changed) re-checks instead of requesting; `generation.save` is never called; `persistenceFailed` → no request and `guestAuthenticationPresented == false`.

### Task 10. `feat(app): open the guest wizard from welcome`
- Files: `…/app/guest/GuestWizardDependencies.kt`, `GuestWizardHolder.kt` (entry-scoped, *[Refresh R4]*), `GuestWizardRoute.kt`, the guest exit class; the `CfRoot` route branch (no `RootContent` back condition, *[Refresh R4]*); `WelcomeDestinations` (§5.1); `AppGraph` members; `SessionHooks.onForeground` (§5.7, last bullet); the 11a sign-up launcher binding.
- Tests:
  - JVM `GuestWizardDependenciesTest` (`…/apptd/guest/`): `startClearsSigningInAndBumpsTheRevision`; `startAbortsWhenSignedIn`; `generationIsGuestAndSourceIsPublic`.
  - *[Refresh R8: move close/expiry/start-failure logic to a JVM test of the guest exit class; keep this Robolectric class for route wiring only.]* Robolectric `@Logic` `GuestWizardHostTest` (`…/apptd/guest/`, a real `AppGraph` with `GraphOverrides(sessionCipher = SoftwareSessionCipher())`, mock source, `CF_MOCK_DELAY=0`): `planMyTripOpensGuestWhere` (`welcome.plan` → `wizard.where` shown, no "From your last trip"); `closeDeletesSecretAndDraftAndShowsWelcome` (seed a secret for the draft token; × on an empty Where → `welcome.plan`, secret gone, file gone); `excludedCloseShowsWelcome`; `foregroundAfterExpiryReturnsToWelcome` (advance the store clock 31 min, send `ON_STOP`/`ON_START` → `welcome.plan`, secret gone); `rotationKeepsTheViewModel` (`ActivityScenario.recreate()` keeps step and values); `leavingTheRouteClearsTheViewModel` (`onCleared` called once); `startFailureReturnsToWelcome` (§5.1 "Start failure").
  - Update 3c's `WelcomeSeamTest` (§5.1) and 3c/5a's `WelcomeDestinationsReleaseTest` (`app/src/testRelease`): `planMyTrip()` now navigates to `Route.GuestWizard` in release too. Remove the `route.guestWizard` seam from plan.md "Android spec: seeds and test hooks" if 3c added it there.
  - JVM `SessionHooksGuestTest`: `onForegroundSweepsAnExpiredGuestDraft` (expired file deleted; errors swallowed).
- Ticks plan.md 11a "**Guest mode**".

### Task 11. `feat(app): recover a guest draft quietly after the welcome intro`
- Files: `…/app/guest/GuestRecoveryScheduler.kt`; the `LaunchedEffect` in `CfRoot` (§5.7).
- Tests (JVM `GuestRecoverySchedulerTest`, 3a's `ManualLaunchClock`, a fake navigator with a real revision counter, `runTest(timeout = 60.seconds)`): `waitsForTheLastIntroItem` (intro start 2300 ms: nothing at 3199 ms; `GuestWizard` at 3200 ms); `noIntroRecoversAtOnce`; `frozenIntroNeverRecovers`; `signingInOpensSignIn`; `expiredDraftOpensNothingAndIsDeleted`; `navigationDuringTheIntroCancels` (revision bump at 2800 ms → no navigation); `signedInNeverRecovers`; `runsOncePerProcess` (a second Welcome does not recover); `epochChangeCancels`.
- Ticks plan.md 11a "**Discard and recovery.**".

### Task 12. `feat(debug): add the guest seeds`
- Files: `TestSeeds` / `DebugSeedRouting` (§5.9), `WizardDebugSeeds` (freeze through `WizardUiOverride`), the seed reset; plan.md "Android spec: seeds and test hooks" gains `CF_GUEST_WIZARD`, `CF_GUEST_WIZARD_JOURNEY`, `CF_GUEST_RECOVERY_BOARD`.
- Tests: extend `ReseedEqualsColdSeedTest` (a guest file and a guest secret before the re-seed are gone after it); extend `ReleaseReadinessTest` (no `GuestWizardBoardSeed` class, no `CF_GUEST_*` handling, launcher has no counter in release).

### Task 13. `test(wizard): guard, capture and check the guest boards`
- **`GuestWizardScreenTest`** (Robolectric `@Logic`, `…/apptd/guest/`, signed-out `AppGraph`, `CF_GUEST_WIZARD=1` + board seed; Swift names from `GuestWizardScreenTests`/`GuestWizardCompletionScreenTests` (no `GuestWizardDarkDutchScreenTests` exists; *[Refresh R8: no copy on screen, move to `…/fwdt/` on `WizardScreen`]*); use the tags the merged 9c–9g code exposes for the iOS ids): `testWhereGuest` (from row shows AMS, no last-trip hint, `wizard.where.continue` enabled), `testOutboundPastGuest` (past text exact), `testSleepGuest` ("On a normal work day.", `wizard.sleep.continue`), `testCheckingGuest` (line 1 = "Your answers", "Your saved answers" absent), `testAgeGuest` (`wizard.choice.question.under_18`, disclaimer exact), `testExcludedGuest` (`wizard.excluded.secondary-close` → `welcome.plan`), `testDiscardGuest` (the `WizardDiscardGuest` seed opens with the alert already shown (§5.9, 9c precedent; iOS taps × instead): assert `wizard.discard` with the guest body → Keep editing → `wizard.outbound.continue` exists; then × (`wizard.close`) → the alert shows again, because the Outbound seed has changes), `testEmptyWhereClosesImmediately` (`WizardWhereGuestEmpty`; × → `welcome.plan`, no alert), `testFineTuneGuest` (`wizard.fine-tune.continue`, `wizard.extra.room.0` exist; `wizard.sleep-override`, "Late check-out", "Busy hours before you leave" absent), `testReadyGuest` (sleep row contains "23:30–07:15 · In between", questions row contains "2 answered", no return row, guest price exact; tapping `wizard.ready.continue` increments `GuestWizardDebugCounters.signUpRequested` once; iOS instead waits for `guest-signup.name`, which is 11b), `testReadyGuestFree` (exact free line, continue enabled), `testReadyGuestDarkNL` ("Klaar om te plannen", "2 beantwoord", "Betaald met 1 van de 3 credits die je krijgt als je een account maakt.").
- **`GuestWizardGuardsTest`** (`@Layout`, parameterised, one case per parameter set; 0e `Guards`: no truncation, 48 dp targets, inside `safeDrawing`, no overlap, headings) on `WizardRoot` with the frozen guest seeds:

  | Cases | States | Matrix | Count |
  |---|---|---|---|
  | (a) | `WhereGuest`, `OutboundGuest`, `OutboundPastGuest`, `CheckingGuest`, `FineTuneGuest`, `ReadyGuest`, `ReadyGuestFree`, `DiscardGuest` (8) | `small`, `pixel8`, `foldInner`, `largeFont` × en, nl × light, dark | 128 |
  | (b) | `ReadyGuest`, `FineTuneGuest` | `landscapeSideNav`, nl, light and dark | 4 |

  Extra: at `largeFont` + nl the guest Ready rows stack (9g rule) and the footer stays fixed. Record the class run time in the PR.
- **`GuestWizardCaptureTest`** (`@Capture`, Robolectric, 390 × 844 dp, density 3, zero insets, 0e `BoardCapture.capture(rule, name)`; theme and language from the name): exactly the 12 wrappers `WizardWhereGuest`, `WizardOutboundGuest`, `WizardOutboundPastGuest`, `WizardOutboundPastGuestDarkNL`, `WizardCheckingGuest`, `WizardFineTuneGuest`, `WizardReadyGuest`, `WizardReadyGuestDarkNL`, `WizardReadyGuestFree`, `WizardReadyGuestFreeDarkNL`, `WizardDiscardGuest`, `WizardDiscardGuestDarkNL`. Spinners with `mainClock.autoAdvance = false`. No stored references.
- **`GuestJourneyCatalogTests.welcomeWizardSignupAndTripCopyStayPaired`** (Robolectric `@Logic`, `core/ui` or `:app` test; port of `KT/TripWizardTests/GuestJourneyCatalogTests.swift`): for each key — Welcome "Plan my trip", "Sign in"; wizard "On a normal work day.", "Your answers", "Ready to plan", "Make my plan", "Your sign-up credits stay unused.", the two `android_wizard_guest_price_paid*`; auth "Save your plan", "Create account and save", "I already have an account"; plan "Loading your plan", "Downloading this trip’s plan…" — the en resource equals the iOS key (the Android-only en values for the price pair), the nl resource is non-empty and differs from en, and the nl price strings keep both placeholders.

### Task 14. `test(app): walk a guest to ready and recover a killed draft`
- Both in `…/appat/guest/`, extend 0e `ScreenTest`, `@Smoke`, < 10 s each, `timeout_msec=60000`, `waitUntil`/`waitUntilExactlyOneExists` only.
- **`GuestWizardJourneyTest.testWelcomeThroughReadyRequestsSignUp`** (first half of iOS `testWelcomeThroughSignupOpensClaimedTrip`; 11b extends it to the claimed trip and restores the Swift name). Seeds: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_MOCK_DELAY=0`, `CF_GUEST_WIZARD_JOURNEY=1`, `CF_WIZARD_BOARD_CAPTURE=WizardGuestJourney`. Steps: click `welcome.plan`; `wizard.where.continue`; `wizard.outbound.continue`; assert the bed control `wizard.sleep.habitual_bed` exists; `wizard.sleep.continue`; wait `wizard.checking.line.1` (2 s); click `wizard.choice.question.18_64` (5 s); `wizard.fine-tune.continue`; wait until `wizard.ready.continue` is enabled (3 s), click; assert `GuestWizardDebugCounters.signUpRequested` is 1 and `wizard.ready.questions` exists (*[Refresh R8]*: no copy on screen; the count is a JVM `ReadyUi` assertion) (age was asked; `shift_work = false` came from the seed, and `guestAnsweredQuestionCount` counts every non-null screening field).
- **`GuestRecoveryScreenTest.testWelcomeHandsOverToRecoveredSleep`** (port): seeds `CF_UI_TEST_PROFILE=screen`, `CF_GUEST_RECOVERY_BOARD=WizardSleep`; wait until `wizard.sleep.continue` is displayed (5 s); `wizard.sleep.habitual_bed` exists; `welcome.plan` absent.
- Docs: extend `docs/wizard.md` with the guest section (path, exits, the guest board → seed table, the Ready guest rows and price pair, the secret store format, the recovery gate timing, the seeds, the visual differences of OQ-11a-10/11). Add OQ-11a-1…11 to plan.md "Open questions for the user" as OQ-A533…A543 with their defaults (*[Refresh R1]*). Tick plan.md 11a "**Tests.**".

---

## 8. Acceptance criteria

1. All greps of §3 succeed before you start; `grep -rn "guestSecret" core/wizard/src/main app/src/main feature/wizard/src/main` shows only stripping, request building and the store (never a log, a `SavedStateHandle`, a `Bundle` or a file write).
2. `make targeted ONLY="core:model:GuestSecretStoreTests core:model:GuestRecoveryLocationTest core:datastore:KeystoreGuestSecretStoreTest core:wizard:GuestRecoveryGateTests core:wizard:GuestWizardStoreTests core:wizard:GuestWizardGenerationTests core:data-mock:GuestGenerationMockTest feature:wizard:GuestWizardBoardSeedTest feature:wizard:TripWizardViewModelGuestExitTest feature:wizard:GuestDiscardDialogTest feature:wizard:GuestReadyUiMapperTest feature:wizard:GuestReadyScreenTest feature:wizard:TripWizardViewModelGuestSignUpTest app:GuestWizardDependenciesTest app:GuestWizardHostTest app:SessionHooksGuestTest app:GuestRecoverySchedulerTest app:GuestWizardScreenTest app:GuestJourneyCatalogTests"` is green (module prefixes follow R8's relocations: `core:data:GuestGenerationMockTest`, `feature:wizard:GuestWizardScreenTest`); every JVM and warm Robolectric test < 1 s (`build/test-results/*/timings.json`; *[Refresh R8]*).
3. 9a's `GuestWizardModeTests` (10), 4b's `GuestTripReviewAPITests`, 0d's `GuestTripReviewContractTests`, 4c's `MockGuestReviewTests` and `MockGuestPossessionTests`, 9c's `WizardBackHandlingTest`/`WizardDiscardUiTest` *[Refresh R8]*, 9g's Ready tests and 3c's `WelcomeSeamTest` stay green.
4. `make targeted ONLY="feature:wizard:GuestWizardGuardsTest"` — 132 cases green, each < 1 s warm *[Refresh R8]*.
5. `make captures ONLY=WizardWhereGuest,WizardOutboundGuest,WizardOutboundPastGuest,WizardCheckingGuest,WizardFineTuneGuest,WizardReadyGuest,WizardDiscardGuest` (prefixes, not globs; *[Refresh R8]*) — exactly 12 PNGs in `build/captures/`, named after the boards; the reviewer compares each with its board.
6. `make quick` and `make check` (with `pixel` booted) pass. `make check` runs `GuestWizardJourneyTest`, `GuestRecoveryScreenTest` and `GuestSecretKeystoreTest` (`@Smoke`), each < 10 s.
7. `testReleaseUnitTest` proves no guest board seed, no `CF_GUEST_*` handling and no presented counter ship in release.
8. **Process kill (manual, recorded in the PR with the commands and one screenshot):** on `pixel` with the debug mock build: open Plan my trip, pick AMS → NRT, Continue to Sleep; press Home; `adb shell am kill net.chronoflow.app.native.debug` (debug `applicationIdSuffix`, plan A2); relaunch from the launcher; after the moon flight and the Welcome intro (≈ 3.2 s) the wizard opens on Sleep with the same airports and no message. Save the screenshot as `build/captures/GuestRecoveryAfterKill.png`.
9. StrictMode: during `GuestWizardJourneyTest`, `adb logcat -d -s StrictMode` shows no violation whose stack contains `net.chronoflow` guest classes (`GuestWizardStore`, `KeystoreGuestSecretStore`, `GuestRecoveryScheduler`).
10. *[Refresh R9 replaces this item]* No new perf budget row. Run `make baseline-profile` (commit the profiles) and `make perf` (record in `docs/perf-startup.md`); `make perf-timeline` is not applicable.
11. plan.md 11a: all five boxes ticked in the commits named above; OQ-11a-1…11 added as OQ-A533…A543 (*[Refresh R1]*); the two Android-only copy rows added; the seed names added.

---

## 9. Review checklist (for the Opus reviewer)

- [ ] **No re-port.** `WizardMode.swift` logic, the mock guest review and `reviewGuestTrip` are 9a/4c/4b code, used as they are. No second `GuestWizardModeTests`, no second `MockGuestReview`.
- [ ] **Secrets.** The secret exists only in `KeystoreGuestSecretStore` (encrypted) and in memory inside one review call. It is never in `guest-wizard.json` (assert the bytes), never in `MemberWizardSnapshot` identities after a review (stripped in step 6 of §5.6), never in a `toString()`, log, `Bundle`, `SavedStateHandle`, route or exception message.
- [ ] **Rotation order.** `rotate` sets the new secret before deleting the old one. A non-null `identity.guestSecret` always replaces the stored one; a `null` keeps the known secret under the new token.
- [ ] **Expiry retry.** One retry, only with a token, only while current, keeping the same `guest_secret`; never retried on the first call; a second expiry surfaces.
- [ ] **Discard mid-flight.** The new token's secret is deleted and the result dropped (two places: inside `performReview` and after `await`).
- [ ] **Store guards.** Instance mismatch → `RecoveryRejected` (not `CancellationException`); lower revision → silent no-op; recovery re-writes with the **old** `updatedAt`; persist writes `now`; expiry at exactly 30:00.000; `one_way` check; atomic replace.
- [ ] **Recovery timing.** Never before `handOffComplete`; waits until `intro.start + 900 ms` on the **launch clock** (not `delay(3200)` from an arbitrary moment); aborts on any navigation (revision) or epoch change; once per process; frozen intro never recovers.
- [ ] **Exits.** Every guest exit (×, discard, excluded Close, failure Close, No plan Close, background expiry) ends on Welcome and deletes the draft and the token's secret. × on an empty Where never shows the alert. Member exits are unchanged.
- [ ] **Ready.** No Return row; Questions row present with its haptic; Sleep "times · chronotype"; the member balance line never shows for guests; the price pair uses `price.cost`/`price.balance` from the review (mock 3 → "of the 3 credits"); no hard-coded 1 or 3 in Kotlin.
- [ ] **Sign-up seam.** One request per `signUpRequested`; `guestAuthenticationPresented` resets on dismissal; no UI is invented; `generation.save` is never called for guests.
- [ ] **Threading.** File, DataStore, Keystore and `noBackupFilesDir` access run on `io`; nothing in `AppGraph` construction does I/O; one DataStore instance per file.
- [ ] **Scoping.** The guest ViewModel survives rotation and is cleared when the route leaves `GuestWizard`; the root back handler is disabled on that route.
- [ ] **Copy.** Every visible string comes from resources with nl in the same commit; the guest discard body and the price pair are exact; the Android-only pair is in `android-only.json` and in plan.md.
- [ ] **Tests.** Swift names kept; parameterised Swift cases are loops with messages; JVM for logic, Robolectric for layout and route changes, emulator only for the journey, the recovery hand-over and the real Keystore; no `Thread.sleep`; exact tags.
- [ ] Typical mistakes: storing the secret in the draft "temporarily"; using `delay` instead of the launch clock; recovering on every Welcome; deleting the Keystore alias on `deleteAll`; two `DataStore` instances for one file; reading `noBackupFilesDir` on main; forgetting the `TestSeeds` reset (flaky re-seeds); capturing boards that do not exist.

---

## 10. Open questions (proceed on the default; add them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-11a-1 | iOS hard-codes "3" in "Paid with {{cost}} of the 3 credits…"; the server grants 1 sign-up credit (OQ-A18). | Render `price.cost` and `price.balance` through the Android-only pair `android_wizard_guest_price_paid` / `…_one` (needs review). The mock (3) matches the board and iOS. |
| OQ-11a-2 | What does "Make my plan" do before 11b builds the GuestSignup sheet? | A `GuestSignUpLauncher` seam: debug presented counter, immediate dismissal, no UI (no board for anything else). 11b replaces it. |
| OQ-11a-3 | The plan lists `GuestSessionContinuation.swift` as an 11a reference; nothing in 11a consumes it. | Port it in 11b with its consumers; 11a only reads it. |
| OQ-11a-4 | Secret storage format for "Keystore alias `<ns>.guest` + DataStore key". | Alias `"${keychainService}.guest"`; one Preferences DataStore per namespace (`guest_secrets_<dir>`); key `t.<token>`; value Base64 of 4a's `[0x01][IV][ct+tag]`. |
| OQ-11a-5 | Where the guest draft file code lives. | `:core:wizard`, pure JVM with an atomic rename; `:core:data` only if Konsist forbids file I/O there. |
| OQ-11a-6 | ~~4e's `RootContent` has no ViewModel entry decorator (OQ-5a-5).~~ 5d added it (OQ-A227). *[Refresh R4]* | The guest VM and an entry-scoped `GuestWizardHolder` ViewModel come from the `RootContent` entry store (keyed `guestWizard:<revision>`), kept across recreation and cleared when the entry leaves. |
| OQ-11a-7 | iOS checks guest recovery once per scene. | Once per process, on the first Welcome after the hand-off. |
| OQ-11a-8 | 9b may type `newGeneration()` as `TripGenerationService`. | Widen it to `MemberWizardGeneration` in a separate refactor commit; `onCleared` cancels without suspending. |
| OQ-11a-9 | The board's guest Checking/Fine-tune/Where/Outbound have no Dark/NL wrappers. | Guard them in dark and nl through the shared props; capture only the 12 named wrappers. |
| OQ-11a-10 | `WizardReadyGuest` draws "Questions 3 answered" (from the retired 3-question era); guests now answer at most 2. | Render the model count ("2 answered" with the seed); record the difference in `docs/wizard.md`. |
| OQ-11a-11 | `WizardFineTuneGuest` draws a fixed row list (room, wake, seat, drinks, light, blue). | Render the rows the review and preferences produce (iOS); the seed matches the board as closely as the mock allows; record differences in `docs/wizard.md`. |

---

## 11. Hand-off to 11b

- `GuestWizardStore` (add `claim(secrets)` and `reviewExpired(claim)`, iOS l.92–112), `GuestSecretStore` / `KeystoreGuestSecretStore`, `AppGraph.guestSecrets` / `guestWizardStore`.
- `GuestSignUpLauncher` and the VM's `signUpRequested` handling (§5.8): 11b binds the GuestSignup sheet (520 dp column on wide windows), adds the `review` branch of `signupDismissed` (`guestReviewExpired()`), and "I already have an account" (`setGuestSigningIn(true)`, `flush()`, navigate to Sign in carrying the email).
- `GuestRecoveryGate.Decision.signIn` already routes to `Route.SignIn`.
- `GuestWizardJourneyTest` continues from `wizard.ready.continue` to the claimed trip and takes back the Swift name `testWelcomeThroughSignupOpensClaimedTrip`.
- `CF_GUEST_WIZARD_JOURNEY` gains the sign-up prefill (name "Traveller", `guest-journey@example.com`, `NewPassword123!`).
