# Phase 6d brief: Step sheet, check-offs, legend and trip actions

- **Branch:** `feat/phase-6d-trip-interactions`, stacked on 6c.
- **Depends on:** 6c (and through it 6b, 6a, 4e, 4d, 2, 1d, 1c, 1b, 1a, 0c). **Wave:** 11. Later consumers: 6e keeps an open sheet across size changes; 7 (Today) reuses the step sheet and the full legend; 9h wires Edit and Duplicate.
- **Plan entry:** `plan.md` "### Phase 6d" (l.1979–2015); "Android spec: state survival" (l.373–383); "navigation and routes" (l.400–405, "Sheets are part of the route key"); window policy (bottom sheets `sheetMaxWidth = 640.dp`); Material mapping "Sheets" row; never-build list (`TripDelete`, `Calendar*`); A24; OQ-A5 (Snackbar), OQ-A11 (`more_vert` trip-actions glyph).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec`, and the boards in `_design/canvas/`.
- **Checked 2026-11-16** against iOS `f391a1fa`, web `e36368ec`, the boards and the 4d/4e/6a/6b/6c/6e/7 briefs. Corrected: the mark lookup (actionable only), the stacked-button order, the refusal text for a step without a label, the PriorityKey description colour, the `CheckOffRequest.php` and `PreparedPlan.swift` pins, the TripActionsNoPlan subtitle, the Lucide manifest `first` format, the list-row outline (6e owns it), 6a's existing `CategorySwatch`, 6b's `selectedStepId`/`selectBlock` seam, the 6c list row name (`DayListRow`) and its label, `LocalRefusedChanges`, the `:core:sync` dependency, `TripActionsUi`, the missing `sheetGesturesEnabled` parameter, the trip-actions seed (`CF_WIZARD_BOARD_CAPTURE=TripActions`, iOS), and the selection test's badge. Added: focus after a day jump (deferred to 6d by 6b OQ-6b-10 and 1d).
- **Tests:** follow AGENTS.md "Tests" for every test. Logic is tested on the JVM, layout with Robolectric, and the emulator gets one journey plus seeded captures.

Tags: **[F]** is a fact verified in source (path and line at the pin). **[R]** is a decision for Android that this brief adopts. Paths written `feature/trip/…/X.kt` mean `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`; `core/ui/…` means `core/ui/src/main/kotlin/net/chronoflow/core/ui/…`; `core/plan/…` means `core/plan/src/main/kotlin/net/chronoflow/core/plan/…`. iOS paths are relative to `chronoflow-ios/Packages/ChronoFlowKit/Sources/` unless they start with `App/` (`chronoflow-ios/App/`) or `plan.md`. Read iOS with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. **Every source repo and `_design/canvas` is read-only.**

---

- **Execution refresh (2026-11-19):** work in the main checkout, one task at a time. `TripPageViewModel` and `ScrollCommand` live under `feature/trip/…/page/`; `SessionGraph.epoch` is a `Long`, with `sync` (not `sessionSync`) and `SessionController.mutationPermit(snapshot)` (suspending; acquire it for the captured snapshot before enqueue). `DayCheckActions` is a pure JVM model: use the Compose stability configuration rather than importing `@Immutable` into `:core:plan` (A3). Screen-test copy assertions below are superseded by AGENTS.md and the user: use tags and structural assertions, with exact copy checked through resource resolution. Each of the ten commits adds its own completion checkbox under the matching Phase 6d plan task. Perf/profile runs and `make check` stay with the user.

- **Stack check (2026-11-19, against 3788143, the 6c top):** the §4 items exist. Deltas:
  - There is no `entry<Route.TripPage>`. The shell renders the trip page through its `page(route)` slot (`app/…/shell/AppShell.kt:76`; `CfRoot.kt:227` routes `is Route.TripPage` into the shell).
  - The legend button `trip.legend` lives in 6b's `SummaryCard.kt`, and `onLegend` is threaded through `TripPage.kt`.
  - The 4c mock fixtures are in `core/testing/src/main/resources/fixtures/` (`mock-seed.json`, and `plans/S2.json` for `caffeine-caffeine_serving-29864460`), not in `core/data-mock`.
  - `:core:ui` already depends on `:core:plan` and `:core:sync`, so Tasks 3 and 4 add no dependency.
  - `RefusedChangeModel` and `PoliteAnnouncer` don't exist yet; Tasks 2 and 4 build them. The polite live regions in use are in `RefreshFeedbackCapsule` and `CfBanner`.
  - `ScrollCommand` is in `TripPageArgs.kt`.
  - 6c scoped the fetch model to the page's STARTED lifecycle (`runFetch`) and reads scroll state only through `snapshotFlow`. Sheets must not read `LazyListState` in composition.
  - OQ rows end at OQ-A291. A timeline perf re-measure is pending under OQ-A291, because the host environment ran slow on 2026-11-19.

## 1. Goal

Make the trip page interactive, with iOS 17a and 17c parity:

1. **Step sheet** (board `ItemDetail`): a tap on a timeline block or a list row opens it. It shows the category swatch, the priority tag, the Now/Done/Skipped badge, the title, "`<interval>` `<zone>` time" with "· N min left" while running, Why and How, and the footer. Actionable steps with a `checkKey` get **Skip** and **Mark as done**, or **Undo** once marked. These are written optimistically through 4d's outbox. A block that disappears while the sheet is open shows the **step gone** state (`SheetStates`).
2. **Refused change message**: when sync finally refuses a check-off, a Snackbar says `Couldn't save "<step>" as done, so we undid it.`
3. **Legend**: the trip page's legend button opens the iOS "Legend" sheet (PriorityKey). The full "What the steps mean" legend (`PlanLegend`) is built as a reusable sheet for Phase 7 and reachable now through the debug seed.
4. **Trip actions sheet** (`TripActions`, `TripActionsNoPlan`): a header plus **Edit trip** and **Duplicate trip**. Both are disabled with "Needs a connection." when offline or when the card does not allow them. They go to the wizard route seam until 9h. There is no calendar, complete or delete row.
5. **Accessibility and input** (Android additions): TalkBack custom actions Mark as done / Skip / Undo on timeline targets and list rows, a polite announcement, focus moves after a check-off, focus moves to the target day header after a day jump (04 OQ-T24, deferred here by 6b OQ-6b-10), and keyboard and D-pad traversal and activation. **No haptic on check-off** (iOS).

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` Phase 6a–6e (l.1856–2030), l.365–405, Material mapping, A24, OQ-A5, OQ-A11 | Scope |
| Agent rules | `AGENTS.md` "Working rules", "Tests" 1–9, "Git", "Traps" (polite live regions; rethrow `CancellationException`; measured sizes stay in layout; secrets never in `SavedStateHandle`/`rememberSaveable`/logs) | How to work |
| Research | `docs/research/04-timeline.md` **§3.19, §3.20, §4.4, §4.5, §5, §9.9, §9.13**, Appendix A, OQ-T5, T21, T22, T24, T25, T28 | Sheet, check-off flow, accessibility mapping |
| Research | `docs/research/07-day-to-day-settings.md` **§5.1–§5.4**, §11 (endpoints), §14 OQ-1, OQ-2 | Trip actions |
| Earlier briefs | `phase-6a-shell-trips.md` (`CategorySwatch(type, size: CategorySwatchSize)` in `core/ui/…/plan/`, OQ-6a-12; `AppShell`), `phase-6b-trip-page.md` (`selectedStepId`, `selectBlock`, `onLegend`/`onActions`, `ScrollCommand`, `DayHeader` tag `trip.dayHeader.<id>`, OQ-6b-10), `phase-6c-trip-states-list.md` (`DayListRow`, its label with "Marked done"/"Skipped", `online`), `phase-6e-trip-wide.md` Task 3 (owns the selected-row outline and reads `vm.selectedStep`), `phase-2-design-system.md` (CfButton, CfBadge, CfModalBottomSheet, CfSnackbarHost, cfClickable, cfFocusRing, Haptics, Lucide manifest l.305–330, `@BoardMetrics`), `phase-4d-sync.md` (OutboxRepository.enqueue, SyncResult, SyncRefusal(Notice), SessionSync.events, CheckOffReconcilerTests), `phase-4e-session.md` §5.7 (routes) and T11 (seeds), `phase-1a-plan-core.md` (CopyLineFormatter, checkIdentity, stepLabelKey, PlanDecoder merge), `phase-1b-timeline-layout.md` (PageCopy, tripDatesText), `phase-1c-timeline-drawing.md` §5.1 (category styles), `phase-1d-timeline-host.md` Task 5 (DayTimeline, TimelineTargets) and the invalidation test, `phase-0c-strings.md` (resource names, CopyIndex), `phase-0e-test-harness.md` T8 (Guards), T9 (captures) | What you build on |
| 6a–6c output | `app/…/shell/AppShell.kt`, `feature/trip/**` (TripPageViewModel, TripPageSource, the TripPage Nav3 entry, DayListRow, ZoneChangeCard, DayHeader, TripPagePathMonitor, the top bar and summary card) | Read before Task 1 (§4) |
| Strings | `core/ui/src/main/res/values{,-nl}/strings_ios.xml`, `tools/l10n/names.lock.json` | Resource names in §5.6 |

### 2.2 Boards (`_design/canvas/`)

| Board | What it fixes |
|---|---|
| `ItemDetail.dc.html` | The step sheet. States running / done / skipped. Section `gap 16; padding 12px 20px 32px; radius 38 38 0 0; card`. Head gap 12; swatch `avoid_light`; badges row gap 8 wrap. Title Fraunces 400 24/32. Interval 14/20 muted tabular "07:30 → 10:30 Tokyo time · 45 min left". Why/How h3 14/20 600 uppercase muted; How is a bulleted list 16/24. Open: a 2-column grid gap 8 with outline lg "Skip" and default lg "Mark as done" (16 px check, stroke 2.5). Marked: outline lg "Undo". Footer 12/16 muted centred |
| `TodayRowStates.dc.html` | Done/Skipped row treatment and spoken form ("Avoid bright light, done"). The Today rows are Phase 7. 6d only reuses the rule "no check control on the row; tapping opens the sheet with Undo" |
| `PlanLegend.dc.html` | The full legend over Plan. Header padding 12 12 8 20: Fraunces 24/32 "What the steps mean" plus a 44 × 44 round `surface-2` close button with an 18 px lucide `x`. Body padding 4 20 32, gap 20: intro 14/20 muted; groups gap 8; list gap 10; rows min-height 40, gap 12, swatch, name 16/22 500, hint 14/20 muted; "How important" with a priority grid |
| `SheetStates.dc.html` | **Only** "Item detail · step no longer in the plan" is built: a 22 px lucide `search-x` in a 48 `muted` circle tinted `dawn-text`; h2 Fraunces 24/32; p 16/24 muted; primary button height 48 "See today's steps"; panel gap 14, padding 12/20/24. **Not built:** Trip actions loading, "This trip isn't on this phone yet", Item detail loading, Delete-trip alerts (see §3) |
| `TripActions.dc.html` | Sheet gap 12, padding 12 16 32. Header padding 0 4: "Amsterdam → Tokyo" Fraunces 24/32 plus subtitle 14/20 muted tabular. Card radius 22 `surface-2`. Rows min-height 56, padding 8 16, gap 14, 20 px icons: `pen-line` "Edit trip"; `copy` "Duplicate trip" with "Same flights and answers, new dates". 1 px `border` dividers. Cancel `h-10 px-6 text-sm font-medium rounded-full`. The calendar row, "Mark trip as completed" and the Delete card are **omitted** |
| `TripActionsNoPlan.dc.html` | Wraps TripActions with `no-plan=true`, which only changes the omitted calendar row. On Android it differs from TripActions only by the header subtitle: no end date (the plan has no days, so `tripDatesText(departLocal, null)`); "with return" still shows when a stored detail's legs contain a return leg, and is absent without a detail |

### 2.3 iOS sources to mirror (read-only, `f391a1fa`)

| iOS file | What to port |
|---|---|
| `PlanUI/Steps/StepSheet.swift` (1–204) | Content, gone state, badges, sections, actions, announcements and focus (l.80–204) |
| `PlanUI/Steps/TripPageStepSheet.swift` | Busy guard around `setCheck` |
| `PlanUI/Steps/LegendSheet.swift`, `PlanUI/Steps/LegendCopy.swift` | Full legend: groups, `legendDescription`, `priorityDescription` |
| `PlanUI/Components/PlanComponents.swift` (7–175) | `CategorySwatch`, `PlanBadge`, `PriorityTag`, `PrioritySample` |
| `PlanUI/TripPage/TripPageComponents.swift:118` | `PriorityKey` (the trip-page "Legend" sheet content) |
| `PlanUI/TripPage/TripPageView.swift` | Step sheet `.sheet(item:)` (l.91–93), Legend sheet (l.95–103), legend button `trip.legend`, toolbar `trip.actions.open` |
| `PlanUI/TripPage/TripPageModel.swift` | `StepSelection`, `selectBlock`, `stop()` clears the selection, `online` |
| `PlanUI/Models/TripPlanModel.swift:132–141` | `setCheck` guards and the `enqueue` call |
| `PlanUI/Models/PlanDecoder.swift:76–96` | Pending checks merged over marks (already in 1a) |
| `PlanUI/Models/RefusedChangeModel.swift`, `PlanUI/Components/RefusedChangeFeedback.swift` | Refusal message |
| `PlanUI/TripPage/TripActionsSheet.swift`, `PlanUI/TripPage/TripActionsState.swift` | Trip actions sheet, availability and header |
| `App/TripPageDestination.swift` (l.103–111 notification step-open; actions presentation) | One-shot notification open; `onActions` only with a card |
| `App/AppTabs.swift` (l.60–66, 76) | Debug seed `CF_PLAN_TEST_SHEET=legend` opens the full LegendSheet |
| `PlanCore/PlanLogic.swift:86` `stepLabelKey`; `PlanUI/TripPage/PreparedPlan.swift:208` `priorityKey` | Labels |
| Tests: `Tests/PlanUITests/StepCheckTests.swift`, `TripActionsTests.swift`, `TripPageSelectionTests.swift`, `RefusedChangeTests.swift`; `AppTests/StepSheetLayoutTests`, `StepSheetSnapshotTests`, `TripActionsLayoutTests`; `AppUITests/TripPageStepTests.swift` | Cases to port (§5) |

### 2.4 Endpoint (web `e36368ec`, already implemented by 4d; 6d only enqueues)

- [F] `routes/api.php:91–92`: `PUT /v1/trips/{trip}/checks` → `TripCheckController`, middleware `verified`, `whereUuid`.
- [F] Body (`app/Data/Api/ApiCheckOffInput.php`): `{"plan_input_hash": "<64 hex>", "type": "<BlockType raw>", "local_date": "YYYY-MM-DD", "ordinal": 1–255, "status": "done"|"skipped"|null}`. `status` must be present, and `null` means undo.
- [F] 200 → `{"checkState": …, "detailVersion": …}` (`ApiTripCheckResult`).
- [F] 422 messages: "This plan has changed. Refresh it before marking this step." (`form`, `TripCheckController.php:34`; also the `plan_input_hash` field message, `CheckOffRequest.php:57`) and "This step cannot be checked off." (`form` at `CheckOffRequest.php:48`; the `type` and `ordinal` field messages at l.58 and l.60).
- [F, 4d] The first message, or a 409, or `plan_input_hash` in the fields, counts as stale: one pull and one retry, never refused. Any other 422 is refused: `SyncRefusal` with `StepDone(type)` for a `done` change, and `detail_version` is nulled so the next pull restores the server marks. 429 and 408 stay queued.

---

## 3. Out of scope

| Item | Where |
|---|---|
| Today rows, Today's use of the step sheet and full legend | 7 (6d builds them reusable in `:core:ui`) |
| Real Edit/Duplicate flows, wizard preparation, the busy state while preparing, failure toast `trip.actions.failure` ("We could not duplicate this trip. Try again." etc.) | 9h. 6d only navigates to the wizard route seam |
| Complete, reopen and delete trip, Delete-trip alerts (`SheetStates` "Delete trip · failed/offline"), Add to calendar | Never (A24, OQ-T21, never-build list) |
| `SheetStates` "Trip actions · loading", "This trip isn't on this phone yet", "Item detail · loading" | Not built: iOS never shows them (the sheets open only when their data exists, §5.4) |
| A sync-failure UI other than the step-done message (for example the sleep-times message outside the trip page) | The model handles `SleepTimes`, but 6d presents refusals only where its hosts exist (§5.3). Settings (10a) reuses the hosts |
| Side-by-side or wide sheet layouts, keeping the sheet across folds, the selected-row outline (`DayListView.swift:29–33`, OQ-T28; wide only) | 6e |
| Notification routing into `TripPage(blockId, notificationInputHash, notificationToken)` | 12b builds the route. 6d only consumes it (Task 6) |
| Any haptic | None on check-off (iOS) |

No screen without a board is added. If a state seems to need one (for example a loading state inside a sheet), do not draw it: follow §5.4 and record it in the PR.

---

## 4. Before Task 1: check what 6a–6c landed

Run these and adapt names (not behaviour) to what exists. Record each adaptation in the PR body.

1. `grep -rn "class TripPageViewModel" feature/trip`: note its constructor, the `SavedStateHandle`, the state type (6c's `TripPageState`), the detail/prepared-plan flows, the clock flow and the `TripPagePathMonitor` online flow.
2. `grep -rn "entry<Route.TripPage>\|Route.TripPage ->" app feature`: the Nav3 entry, and which back stack it lives in (6a: one per tab). Note how the stack is held (`NavBackStack` or `SnapshotStateList`).
3. `grep -rn "trip.legend\|trip.actions.open" feature/trip`: whether 6b/6c drew the legend button and the trip-actions button, and with what `onClick`.
4. `grep -rn "fun DayListRow\|fun ZoneChangeCard" feature/trip`: 6c's list rows (`feature/trip/…/list/`), their `clearAndSetSemantics` block and their `onBlock`.
5. `grep -rn "WizardMode.Create" app feature`: how 6a's New trip opens the wizard seam. Edit and Duplicate use the same mechanism.
6. `grep -rn "LiveRegionMode.Polite" core feature app`: an existing announcer (6a pull to refresh). Reuse it; otherwise Task 4 adds `PoliteAnnouncer`.
7. `grep -rn "CF_PLAN_TEST_SHEET\|CF_PLAN_TEST_FREEZE_CLOCK\|DebugSeedRouting" app core`: the seed parser (4e T11) and whether 6b added the frozen clock (OQ-1d-2).
8. `core/ui/build.gradle.kts`: whether `:core:ui` depends on `:core:plan` and `:core:sync` (`RefusedChangeModel`, `SyncRefusalNotice` are used by Task 4). Add the missing `implementation(project(":core:plan"))` in Task 3 and `implementation(project(":core:sync"))` in Task 4. All are core modules, so the dependency rule (plan A3) allows it.
9. 4c's mock fixtures: `grep -rln "0199a3c0-0000-7000-8000-000000000002" core/data-mock` and the block `caffeine-caffeine_serving-29864460`, for the emulator journey (Task 10).
10. `grep -rn "fun CategorySwatch\|enum class CategorySwatchSize" core/ui`: 6a already built `CategorySwatch(type, size: CategorySwatchSize = Regular, modifier)` at `core/ui/…/plan/CategorySwatch.kt` (OQ-6a-12). 6d reuses it and does not build a second one.
11. `grep -rn "selectedStepId\|fun selectBlock\|onLegend\|onActions" feature/trip`: 6b's `selectedStepId: StateFlow<String?>` and `selectBlock(id)` (6b l.297, l.344), and the nullable `onLegend`/`onActions` callbacks 6b/6c left `null` for 6d. Task 6 folds them into `sheet`.
12. `grep -rn "data class ScrollCommand\|fun jump(" feature/trip`: 6b's jump path (`ScrollCommand(dayId, offset, animated)`), for focus after a jump (Task 9).

If a named deliverable differs mechanically, adapt its name to the landed tree and record it. Stop only when every resolution changes earlier-phase behaviour or contradicts pinned iOS, plan.md or AGENTS.md (user, 2026-11-19).

---

## 5. Design (shared by the tasks)

### 5.1 Where things live [R]

| Piece | Module / package | Why |
|---|---|---|
| `StepSheetState` builder, `LegendCopy`, `TripActionsAvailability`, `TripActionsHeader`, `CheckActions` | `:core:plan` `net.chronoflow.core.plan.step` / `.actions` | Pure, JVM-tested |
| `RefusedChangeModel` | `:core:sync` `net.chronoflow.core.sync.refusal` | Pure and per session |
| `CategorySwatch` (6a's, reused), `PlanBadge` use, `PriorityTag`, `PrioritySample`, `PriorityKey`, `StepSheetContent`, `StepGoneContent`, `LegendSheetContent`, `PriorityLegendContent`, `RefusedChangeText`, `RefusedChangeFeedback` | `:core:ui` `net.chronoflow.core.ui.plan` | Phase 7 reuses them, and features never depend on features |
| Selection, check-off writer, sheet hosting, trip actions sheet, timeline/list wiring | `:feature:trip` `net.chronoflow.feature.trip.step` / `.actions` | Trip page only |
| Shell refusal host | `:app` `…/shell/AppShell.kt` | Shell owns the bottom Snackbar slot |

### 5.2 Sheet state and the route key [R, plan l.403 and l.2001]

- [F, plan] "Sheets are part of the route key (`sheet = Step(blockId) | Legend | TripActions | …`), so they reopen after recreation."
- Extend `TripSheet` (4e, `:core:navigation`) with one case: `@Serializable data object StepsLegend : TripSheet`. This is the full `PlanLegend` legend, used by the seed. `Legend` stays the trip-page PriorityKey sheet (OQ-6d-1).
- **The open sheet is `TripPageViewModel.sheet: StateFlow<TripSheet?>`**, initialised from the route's `sheet`. Every change is written back into the route key in place: the entry calls `onRouteChange(route.copy(sheet = s))`, which does `stack[stack.lastIndexOf(route)] = new`.
- **The entry's content key ignores `sheet`**: key the shell `NavEntry` with `route.copy(sheet = null).stateKey()`. The landed `stateKey()` already excludes notification fields. Mark the notification token transient and redact `TripPage.toString()` so serialization and diagnostics cannot persist it. Dropping `notificationToken` too keeps it out of the saved-state `Bundle`, which the saveable-state decorator keys by `contentKey` (4e §5 [F, navigation3 1.2.1]). Then the saveable-state and ViewModel-store decorators keep the same entry, so the ViewModel, the `LazyListState` and the anchor survive a sheet opening or closing. [verify against navigation3 1.2.1: both decorators key by `contentKey`.] Task 7's `TripPageSheetRouteTest` proves it. If it fails, use OQ-6d-2's fallback.
- Never put the notification token into a sheet or `SavedStateHandle`. `TripSheet` holds only ids.

### 5.3 Refused changes [F `RefusedChangeModel.swift`, `RefusedChangeFeedback.swift`; R for presentation]

- [F] `receive(result)`: take the **last** refusal in `result.refusals` whose `notice != null` as pending. Publish it only when `result.state == Idle && result.retryAfter == null`; otherwise keep it pending and publish nothing. `text(language)`: `SleepTimes` → "Couldn't save your sleep times, so we undid the change."; `StepDone(type)` → `Couldn't save "{{step}}" as done, so we undid it.` with the step label, or **no text** when `stepLabelKey(type) == null` (`RefusedChangeModel.swift:23–24`: `guard let key … else { return nil }`). `clear()` resets.
- [F] iOS owns it per session, feeds it from sync `.finished` events and clears it on stop. It shows the toast `sync.refusal` (success false) with an announcement at the bottom of each tab and inside the StepSheet.
- [R] Android: one `RefusedChangeModel` per `SessionGraph`, collecting `sessionSync.events()` in the graph's `scope`. A new epoch drops the graph, which clears it. It is presented as a **Snackbar** (OQ-A5), `SnackbarDuration.Long`, no action, through `CfSnackbarHost`. A Snackbar is announced politely by M3.
- [R] **Show once.** `RefusedChangeModel.visible: StateFlow<SyncRefusal?>` and `consume(id)`. A host shows the refusal, then calls `consume(id)`, which clears `visible` when the id matches. `SessionSync.events()` replays the latest event to a new subscriber (4d), so `receive` ignores any refusal id already consumed (keep a bounded `ArrayDeque` of the last 32 ids).
- [R] **One host at a time.** `RefusedChangeHosts` (in `:core:ui`) is a main-thread stack of host tokens. A host registers in `DisposableEffect` and shows the refusal only while it is on top. The shell host registers first; the step sheet's host registers while the sheet is composed, because a `ModalBottomSheet` is its own window above the shell's Snackbar.
- Test tag `sync.refusal` on the Snackbar. Debug counter `RefusedChangeFeedback.presentedCount` (an `AtomicInteger` in a debug-only object) is incremented from `CfSnackbarHost(onPresented)`, per AGENTS Tests 8.

### 5.4 When each sheet opens [F iOS, R where marked]

| Sheet | Opens from | Condition |
|---|---|---|
| `Step(blockId)` | Timeline tap or semantic click, list row tap, keyboard activation, route key (seed or recreation), notification one-shot | Presented only while the page state is Ready (6c `TripPageState`). [R] **Initial** value (from the route at VM creation): wait for the first non-loading state. If it is Ready and the block exists, present it; otherwise set `sheet = null` silently (iOS opens a seeded or notified step only when the block exists). Once presented, a missing block or a null detail shows the **gone** state (iOS `StepSheet` with detail nil or block missing) |
| `Legend` | `trip.legend` button in the summary card (6b) | Always (static content) |
| `StepsLegend` | Seed `CF_PLAN_TEST_SHEET=legend` only | Always |
| `TripActions` | `trip.actions.open` top-bar button | Only when the trip card exists. [F] iOS sets `onActions` only when `card != nil`, so the button is disabled without a card. Available in every page state that has a card, including no plan and plan-not-on-this-phone. A route key with `TripActions` and no card at the first resolved state → `sheet = null` |

- [F `App/TripPageDestination.swift:103–111`] **Notification step-open**, once: when the route has `blockId` and `notificationInputHash`, the detail's `plan.meta.inputHash == notificationInputHash`, and the block exists → open `Step(blockId)`. [R] The once-flag is `SavedStateHandle["trip.notificationStepHandled"] = true`, set on the first Ready state whether or not it opened. The token is never stored.
- [F `TripPageModel.swift`] `stop()` (sign-out or epoch end) clears the selection. On Android the session graph's end disposes the page, so nothing extra is needed.
- [F] Selecting a day, toggling Timeline/List, a re-plan and a removed block all **keep** the selection (`TripPageSelectionTests`).

### 5.5 Check-off write [F `TripPlanModel.swift:132–141`, `TripPageStepSheet.swift`]

```kotlin
// TripPageViewModel (feature/trip/…/TripPageViewModel.kt), added
val checkBusy: StateFlow<Boolean>
fun setCheck(blockId: String, status: CheckStatus?)  // status null = Undo
```

1. If `checkBusy.value`, return (busy guard; the buttons are also disabled).
2. Guards, each a silent no-op: the session graph is current; the current detail is non-null; the block exists in `detail.plan.blocks`; `block.actionable`; `block.checkKey != null`.
3. `checkBusy = true`; in `viewModelScope.launch`: `outbox.enqueue(OutboxChange.CheckOff(CheckOffChange(tripID = tripId, planInputHash = detail.plan.meta.inputHash, type = key.type, localDate = key.localDate, ordinal = key.ordinal, status = status)), permit = sessionGraph.mutationPermit)`; `finally { checkBusy = false }`.
4. Errors: rethrow `CancellationException`. An invalid permit or epoch throws a `CancellationException` subtype (4d), which ends the job without UI. Any other exception: `Log.w("CfTrip", "check-off not saved: ${e::class.simpleName}")` in debug only, with no UI (iOS `try?`). Never log the change body.
5. Nothing else. The outbox commit triggers sync (4d lifecycle). The DB flow re-emits; 1a's `PlanDecoder` merges the pending check; 6b's preparer updates only day k's marks; the sheet reads the mark live.

### 5.6 Copy (0c resources in `:core:ui`; no new strings)

All names were computed with 0c's slug rule. Confirm each in `tools/l10n/names.lock.json` before use. **If one is missing, stop and report: do not add strings by hand.**

| Use | iOS key (table) | Resource | en / nl |
|---|---|---|---|
| Gone title | "This step is no longer in your plan" (Screens) | `screens_this_step_is_no_longer_in_your_plan` | / Deze stap staat niet meer in je plan |
| Gone body | "Your plan changed after you opened it, …" | `screens_your_plan_changed_after_you_opene_3344b8` | / Je plan is veranderd nadat je het opende, … |
| Gone action | "See today's steps" | `screens_see_today_s_steps` | / Stappen van vandaag bekijken |
| Badges, announcements | "Done", "Skipped", "Now", "Check-off undone" | `screens_done`, `screens_skipped`, `screens_now`, `screens_check_off_undone` | Klaar, Overgeslagen, Nu, Afvinken ongedaan gemaakt |
| Time line | "{{time}} {{zone}} time" | `screens_time_zone_time` (`%1$s %2$s time`) | nl `%1$s (tijd in %2$s)` |
| Time left | "{{duration}} left" | `screens_duration_left` (`%1$s left`) | nl `Nog %1$s` |
| Sections | "Why", "How" | `screens_why`, `screens_how` | Waarom, Hoe |
| Buttons, custom actions | "Mark as done", "Skip", "Undo" | `screens_mark_as_done`, `screens_skip`, `screens_undo` | Markeer als gedaan, Overslaan, Ongedaan maken |
| Footer | "Checking off changes your progress, never your plan." | `screens_checking_off_changes_your_progres_37ae7d` | Afvinken verandert je voortgang, nooit je plan. |
| Fallback title | "Note" | `screens_note` | Notitie |
| Step labels | `stepLabelKey(type)` | `screens_sleep`, `screens_nap`, `screens_rest`, `screens_seek_bright_light`, `screens_normal_light_is_fine`, `screens_indoor_light`, `screens_avoid_bright_light`, `screens_caffeine_serving`, `screens_caffeine_routine`, `screens_caffeine_window`, `screens_avoid_caffeine`, `screens_flight`, `screens_layover`, `screens_airport_transfer`, `screens_movement_break`, `screens_time_zone_change`, `screens_note` | resolve at runtime with `CopyIndex.find(CopyTable.Screens, key)` |
| Priority tag | `priorityKey(priority)`: core "Important", recommended "Recommended", optional "Optional", else "Information" | `screens_important`, `screens_recommended`, `screens_optional`, `screens_information` | |
| Refusal | `Couldn't save "{{step}}" as done, so we undid it.` / "Couldn't save your sleep times, so we undid the change." | `screens_couldn_t_save_step_as_done_so_we_687bfc` (`%1$s`), `screens_couldn_t_save_your_sleep_times_so_bbbd9b` | nl `"%1$s" kon niet als gedaan worden opgeslagen, dus hebben we dat teruggedraaid.` / `Je slaaptijden konden niet worden opgeslagen, dus hebben we de wijziging teruggedraaid.` |
| Full legend | "What the steps mean", "Close", intro, group headings "Sleep", "Light", "Caffeine", "Travel", "How important", "Priority" | `screens_what_the_steps_mean`, `screens_close`, `screens_each_kind_of_step_has_its_own_ico_47f13c`, `screens_sleep`, `screens_light`, `screens_caffeine`, `screens_travel`, `screens_how_important`, `screens_priority` | |
| Legend descriptions | `legendDescription(type)` | `screens_your_main_sleep_window`, `screens_a_planned_nap`, `screens_time_set_aside_for_rest`, `screens_follow_the_light_source_shown_in_da7fe3`, `screens_no_special_action_needed`, `screens_ordinary_indoor_or_cabin_light_is_fine`, `screens_follow_your_plan_s_guidance_to_ke_6e59ab`, `screens_a_timed_serving_in_your_plan`, `screens_your_daily_caffeine_guidance`, `screens_a_time_window_for_caffeine_in_your_plan`, `screens_a_time_to_avoid_caffeine_in_your_plan`, `screens_your_flight`, `screens_time_between_flights`, `screens_the_trip_to_or_from_the_airport_a_2f1cbd`, `screens_time_to_get_up_and_move`, `screens_a_change_in_local_time_during_your_trip`, `screens_extra_guidance_in_your_plan` | |
| Priority descriptions (full legend) | `priorityDescription` | `screens_your_plan_depends_on_this_step`, `screens_helps_your_clock_shift_as_planned`, `screens_nice_if_you_can`, `screens_good_to_know_nothing_to_do` | |
| Trip-page legend | "Legend", "Priorities", "Important"…"Information", four descriptions, legend button label | `plan_legend`, `plan_priorities`, `plan_important`, `plan_recommended`, `plan_optional`, `plan_information`, `plan_the_steps_that_matter_most_for_shift_107475`, `plan_helpful_when_they_fit_your_day`, `plan_extra_support_if_you_want_it`, `plan_useful_context_not_a_task`, `plan_legend_what_the_colours_and_patterns_mean` | Legenda, Prioriteiten, … |
| Trip actions | "Trip actions", "Edit trip", "Duplicate trip", "Needs a connection.", "Same flights and answers, new dates", "Cancel", `%@ → %@`, "via %@", "with return" | `plan_trip_actions`, `plan_edit_trip`, `plan_duplicate_trip`, `plan_needs_a_connection`, `plan_same_flights_and_answers_new_dates`, `plan_cancel`, `plan_x_x`, `plan_via_x`, `plan_with_return` | Reisacties, Reis bewerken, Reis dupliceren, Verbinding nodig., …, Annuleren, …, met terugreis |
| List row state (6c already) | "Marked done", "Skipped" (Plan) | `plan_marked_done`, `plan_skipped` | |

### 5.7 Values

Use the generated tokens (0b) through `CfTheme` and the token objects. The table gives each iOS value and its token; a value that exists only on a board goes in a `@BoardMetrics("<Board>") private object …Metrics`.

| iOS / board value | Token |
|---|---|
| 4, 8, 12, 16, 20, 24, 32, 56 | `PlanUITokens.space1/2/3/4/5/6/8/14` |
| radius 14, 22, 26 | `PlanUITokens.radiusLg`, `radius2Xl`, `radius3Xl` |
| action min height 48 | `PlanUITokens.actionMinHeight` |
| gone circle 48, icon 22 | `PlanUITokens.stepGoneCircleSize`, `stepGoneIconSize` |
| opacities 0.5 / 0.4 / 0.7 | `PlanUITokens.priorityOptionalOpacity`, `priorityTagOpacity`, `priorityCoreOpacity` |
| priority sample 40 × 28 | `PlanUITokens.prioritySampleWidth/Height` |
| row gap 14 | `PlanUITokens.wizardContentGap` |
| pattern dots/hatch | `PlanUITokens.patternDotSpacing/Diameter/Opacity`, `patternHatchSpacing/Width/Opacity` |
| border 1, coreBorder 4, emphasisBorder 2, touch 44 | `PlanMetrics.border`, `coreBorder`, `emphasisBorder`, `touch`; `AuthTokens.borderWidth` |
| Text styles | `CfTheme.type.screenCardTitle` (Fraunces 400 24/32), `screenBodyLg` 16/24, `screenBody` 14/20, `screenLabel` 600 14/20, `screenMeta` 12/16, `screenPriorityTag` 500 10/14, `wizardSecondary` 500 16/22, `authStrong` 600 14/20, `planTitle` 600 17/22, `planSubtitle` 400 12/15 |
| Board only | `ItemDetailMetrics` (none expected), `PlanLegendMetrics { listGap = 10.dp; rowMinHeight = 40.dp; closeVisual = 44.dp; closeIcon = 18.dp }`, `TripActionsMetrics { rowIcon = 20.dp }` (if no token equals 20 for icons, `space5` is a spacing token, so do not reuse it for an icon size), `StepSheetMetrics { swatchIcon = 16.dp }` |

### 5.8 Accessibility and input [R, plan; iOS has neither custom actions nor key handling]

- **Custom actions** go on a BLOCK target (timeline) and a list row when `block.actionable && block.checkKey != null`:
  - unmarked → `[CustomAccessibilityAction(<Mark as done>) { onCheck(id, Done) }, CustomAccessibilityAction(<Skip>) { onCheck(id, Skipped) }]`;
  - marked (any status) → `[CustomAccessibilityAction(<Undo>) { onCheck(id, null) }]`;
  - otherwise none.
  
  The lists come from a precomputed `CheckActions` per `PreparedDay` (Task 1). The labels are read once per composition of the day's targets.
- **Announcement after a custom action** (no sheet is open): the page-level polite live region gets "Done", "Skipped" or "Check-off undone" when the block's status changes **because of** that action (the VM keeps `pendingAnnouncement: blockId → expected status` and emits when the prepared block reaches it).
- **Keyboard and D-pad**: each BLOCK target gets `Modifier.focusable()` plus `cfFocusRing(RoundedCornerShape(PlanUITokens.radiusLg))`, drawn at the hit frame. `onKeyEvent` handles `KeyEventType.KeyUp` of `Key.Enter`, `Key.NumPadEnter`, `Key.DirectionCenter` and `Key.Spacebar`, calls `onBlock(id)` and returns true. Children are emitted in `d.targets` order (PreparedDay order), so Tab follows the TalkBack order. List rows already use `cfClickable`, which is focusable and handles Enter. Add `cfFocusRing` if 6c did not.
- **Focus after a check-off in the sheet**: Undo gets focus after a mark; Mark as done gets focus after an undo (`FocusRequester`). [F `StepSheet.swift:82–88`, iOS `AccessibilityFocusState`]
- **Focus after a day jump** [R, 04 §5 and §9.10, OQ-T24 default yes; iOS unverified; deferred here by 6b OQ-6b-10 and 1d]: after a user jump (day menu, pill, previous/next: 6b's `ScrollCommand` with `animated = true`) finishes scrolling, request focus on the target day header's title (`trip.dayHeader.<dayId>`, already a `heading()`). Replacement and restore commands (`animated = false`) never move focus.
- **No haptic**: no `LocalHaptics.current.perform(…)` anywhere in this phase.

---

## 6. Tasks

Each task is one commit (Conventional Commits, lower-case imperative, **no trailers**). Tick the matching plan item in the same commit. Run only the tests you touched (`make targeted ONLY=…`); run `make quick` once before the last push.

### Task 1. Pure models: step sheet, check actions, legend copy, trip actions
**Commit:** `feat(plan): add step sheet, legend and trip actions models`

Files:
- `core/plan/…/step/StepSheetState.kt`:
  ```kotlin
  sealed interface StepSheetState {
      data object Gone : StepSheetState
      data class Step(
          val blockId: String, val type: BlockType, val titleKey: String,       // stepLabelKey(type) ?: "Note"
          val priorityKey: String,                                               // "Important" | "Recommended" | "Optional" | "Information"
          val badge: StepBadge?,                                                 // Done | Skipped | Now | null
          val interval: String, val zone: String, val durationLeft: String?,     // durationLeft only while running
          val reasons: List<String>, val instructions: List<String>,
          val actions: StepActions,                                              // None | Open | Marked
      ) : StepSheetState
  }
  enum class StepBadge { Done, Skipped, Now }
  enum class StepActions { None, Open, Marked }
  fun stepSheetState(detail: TripDetail?, blockId: String, nowMs: Long, formatter: CopyLineFormatter, language: Language, timeFormat: TimeFormat): StepSheetState
  ```
  Behaviour [F `StepSheet.swift:80–204`]:
  - `detail == null`, or no block with `id == blockId` → `Gone`.
  - `key = if (block.actionable) block.checkKey else null` [F `StepSheet.swift:99`]; `mark` = the first `m` in `detail.checkState.marks` with `checkIdentity(m.key) == checkIdentity(key)`, only if `key != null`. A non-actionable block never shows a Done/Skipped badge, even if a mark exists for its key.
  - `running = planDate(start) <= now && now < planDate(end)`. An unparseable instant gives `false`.
  - `options = CopyFormat(language, timeFormat, displayZone = block.displayTz, annotateInstantZone = true, trip = detail.trip)`.
  - `interval` = `formatter.formatInstant(block.startUtc, options)` for `zone_change`, else `formatter.blockInterval(block, options, separator = " → ")`.
  - `zone = zoneLabel(block.displayTz, detail.trip)`.
  - `durationLeft` = when running, `formatter.formatMinutes(max(0, ceil((end − now) / 60 000.0)), language, duration = true)`, else null.
  - `badge`: mark Done → `Done`; any other mark → `Skipped`; else running → `Now`; else null.
  - `reasons` = `detail.timeline.blockReasons.firstOrNull { it.blockId == block.id }?.lines`, each through `formatter.format(line, options)`, or empty.
  - `instructions` = for a `note` block, `params.note.lines`; else `[block.text] + listOfNotNull(block.secondaryText)`. Each is formatted.
  - `actions` = `None` unless `key != null` (i.e. `block.actionable && block.checkKey != null`); then `Marked` if `mark != null`, else `Open`.
- `core/plan/…/step/CheckActions.kt`: `data class DayCheckActions(val byBlock: Map<String, StepActions>)` and `fun checkActions(day: PreparedDay): DayCheckActions`, from each `PreparedBlock` (`actionable`, `checkKey != null`, `status`). Keep this JVM model free of Compose (A3), and declare it immutable in `stability_config.conf` if it is passed to a skippable composable (Task 9). Compute it in the preparer next to the day, so an unchanged day keeps the same instance.
- `core/plan/…/step/LegendCopy.kt` [F `LegendCopy.swift`, `LegendSheet.swift`]: `val legendGroups: List<Pair<String, List<BlockType>>>` = Sleep `[sleep, nap, rest]`; Light `[seek_light, light_ok, light_neutral, avoid_light]`; Caffeine `[caffeine_serving, caffeine_routine, caffeine_ok, no_caffeine]`; Travel `[flight, layover, transfer, movement_break, zone_change, note]`. Also `fun legendDescription(type): String` (iOS keys, §5.6) and `fun priorityDescription(priority): String` (core "Your plan depends on this step.", recommended "Helps your clock shift as planned.", optional "Nice if you can.", default "Good to know, nothing to do.").
- `core/plan/…/actions/TripActions.kt` [F `TripActionsState.swift`]:
  ```kotlin
  enum class TripAction { Edit, Duplicate }
  data class TripActionsAvailability(val canEdit: Boolean, val canDuplicate: Boolean, val online: Boolean, val busy: Boolean) {
      val edit = canEdit && online && !busy; val duplicate = canDuplicate && online && !busy
      fun enabled(a: TripAction) = if (a == TripAction.Edit) edit else duplicate
  }
  data class TripActionsHeader(val title: String, val subtitle: String)
  fun tripActionsHeader(card: TripCardView, detail: TripDetail?, copy: PageCopy): TripActionsHeader
  ```
  - `title = copy("%@ → %@", card.from.city ?: card.from.iata, card.to.city ?: card.to.iata)`.
  - `parts = [tripDatesText(start = card.departLocal, end = detail?.plan?.days?.lastOrNull()?.labelDate, language)]` (1b's port).
  - If `card.connections` is not empty, add `copy("via %@", connections.joinToString(", ") { it.city ?: it.iata })`.
  - If `detail?.trip?.legs` contains a return leg (the 1a/iOS `returnLeg` test), add `copy("with return")`.
  - `subtitle = parts.joinToString(" · ")`.

Edge cases: a `zone_change` block shows only the start instant. A block ending exactly at `now` is not running. A block with a `checkKey` but `actionable == false` has `None`. An unknown `BlockType` gives the title key "Note". A duplicate identity: the mark lookup uses identity only (1a dedups).

Tests (plain JVM, `core/plan/src/test/…`):
- `StepSheetStateTest`, from 1a's `trips/detail.json` fixture with a fixed clock:
  - `runningShowsNowAndRoundedUpDurationLeft`: now = start + 30 s on a 3 h block → badge Now, `durationLeft` "3 hr" (ceil of 179.5 min = 180), en; nl "3 uur" (use the 1a catalog value).
  - `upcomingHasNoBadge`: now = start − 60 s → badge null, `durationLeft` null.
  - `doneAndSkippedMarksPickBadgeAndUndo`: detail with a Done mark → `Done`/`Marked`; Skipped → `Skipped`/`Marked`.
  - `informationStepHasNoActions`: a non-actionable info block → `None`, priority "Information".
  - `nonActionableBlockIgnoresItsMark`: a block with a `checkKey`, `actionable = false` and a Done mark for that key → badge null (or Now while running), actions `None`.
  - `missingBlockOrDetailIsGone`: both cases.
  - `zoneChangeShowsStartOnly`, `reasonsAndInstructionsAreFormatted` (a note block uses `note.lines`; another uses text plus secondary text).
- `TripActionsTests` (port of iOS `TripActionsTests`):
  - `flagsConnectionAndBusyGateBothActionsIndependently`: the full 16-case matrix over (canEdit, canDuplicate, online, busy).
  - `headerIncludesDatesConnectionsAndReturnWithPairedCopy`: en contains " · via <city> · " and ends with "with return"; nl ends with "met terugreis"; with `detail == null` there is no "with return" and the subtitle starts with `tripDatesText(departLocal, null)`.
- `CheckActionsTest`: actionable unmarked → Open; done or skipped → Marked; no `checkKey` → absent; the same `PreparedDay` instance yields an equal value.
- `LegendCopyTest`: groups cover 17 types once each (all except unknown); every `legendDescription` key is one of the 17 iOS strings.

### Task 2. Refused-change model
**Commit:** `feat(sync): track refused check-offs for the undo message`

Files:
- `core/sync/…/refusal/RefusedChangeModel.kt`: `class RefusedChangeModel { val visible: StateFlow<SyncRefusal?>; fun receive(result: SyncResult); fun consume(id: String); fun clear() }`, per §5.3. It is thread-safe: guard with a `Mutex` or update `MutableStateFlow` atomically; `receive` is called from the collector only.
- `app/…/SessionGraph.kt`: `val refusedChanges = RefusedChangeModel()` and in `scope`: `sessionSync.events().filterIsInstance<SyncEvent.Finished>().filter { it.epoch == epoch }.collect { refusedChanges.receive(it.result) }`.

Tests (plain JVM, `core/sync/src/test/…`), `RefusedChangeModelTests` (port of `RefusedChangeTests.serverRefusalProducesTheDrawnMessage` minus the text, which is Task 3):
- `failedWithRetryShowsNothingThenIdleExposes`: `Failed`, `retryAfter = 1.0`, one `StepDone` refusal → `visible == null`; then `Idle`, `retryAfter = null`, no refusals → `visible` is that refusal.
- `lastNoticeWins`: two refusals with notices → the second; a refusal without notice is ignored.
- `consumeHidesAndReplayDoesNotReshow`: `consume(id)` → null; `receive` of the same result again → still null.
- `clearResets`.

### Task 3. Shared plan components and icons
**Commit:** `feat(ui): add priority tag, priority sample and priority key`

Files:
- `tools/icons/lucide.json`: add the board entries in the Phase 2 manifest form `{"source": "board", "board": "<Board>", "first": "<exact d of the first path, or the exact tag of a non-path first child>"}` (Phase 2 l.330–333; e.g. `chevron-right` uses `"m9 18 6-6-6-6"`, `lock` uses the `<rect …>` tag): `"pen-line": {"source": "board", "board": "TripActions", "first": "M12 20h9"}`, `"copy": {"source": "board", "board": "TripActions", "first": "<rect width=\"14\" height=\"14\" x=\"8\" y=\"8\" rx=\"2\" ry=\"2\">"}`, `"search-x": {"source": "board", "board": "SheetStates", "first": "m13.5 8.5-5 5"}`, `"x": {"source": "board", "board": "PlanLegend", "first": "M18 6 6 18"}`. Skip any name an earlier phase already added. Run `make generate`. Raise the glyph count in `LucideGlyphsTest` by the number added (read the merged assertion first).
- **Landed type API:** generated screen styles such as `screenPriorityTag` are accessed as `CfTheme.type.style(CfTypeScale.screenPriorityTag)`; only a subset has convenience properties. No earlier typography changes.
- **`CategorySwatch` is 6a's** (`core/ui/…/plan/CategorySwatch.kt`, `CategorySwatch(type, size: CategorySwatchSize = Regular, modifier)`, OQ-6a-12). Reuse it; do not add a second swatch. Check only that its mapping matches iOS `PlanComponents.swift:59–80` (fill → foreground: `catSleep`→`catSleepFg`, `catLight`→`catLightFg`, `catModerate`→`catModerateFg`, `catDark`→`catDarkFg`, `catCaffeine`→`catCaffeineFg`, `card`→`foreground`, else `catTravelFg`; dots for nap/light_ok/caffeine_serving; hatch for rest/avoid_light/caffeine_routine/layover; dashed for light_ok/no_caffeine/note). If it differs, fix it in this commit and say so in the body.
- `core/ui/…/plan/PriorityTag.kt` [F l.137–150]: text in `screenPriorityTag`; padding horizontal 6 (`space1 + space1 / 2`), vertical 1 (`borderWidth`); fill `background` at 0.4; 1 dp stroke `foreground` at 0.4; capsule.
- `core/ui/…/plan/PrioritySample.kt` [F l.152–175]: 40 × 28, radius 14; fill `muted` for info, else `catSleep`; stroke 1 dp `border`, or for optional `foreground` at 0.5 dashed 4; core adds a 4 dp (`space1`) left bar of `foreground` at 0.7; info desaturates to `PlanUITokens.priorityInfoSaturation` (use a `ColorMatrix` saturation on the layer). Hidden from accessibility.
- `core/ui/…/plan/PriorityKey.kt` [F `TripPageComponents.swift:118`]: title "Priorities" (`plan_priorities`, `planTitle`, heading). Four rows: Important / Recommended / Optional / Information with the `plan_*` descriptions. Each row is a sample (`RoundedCornerShape(PlanMetrics.tileRadius)`, 40 × 28, stroke `foreground`, width `coreBorder` 4 for Important else `border` 1, dashed `smallGap` 4 for Optional), then the title (`authStrong`) and description (`planSubtitle`, inherited `foreground`: iOS sets only the font, `TripPageComponents.swift:143`). Column spacing `rowGap` 8. Each row merges its semantics.
- `CfBadge` (Phase 2) already covers PlanBadge: Now = `Dawn`, Done = `Secondary`, Skipped = `Outline`. Do not add a second badge.

Tests:
- `PlanComponentsTest` (`@Logic`, Robolectric, `pixel8`, en, light): the tag text "Important" exists; a `PrioritySample` has no semantics node; `PriorityKey` has heading "Priorities" and four merged rows whose text includes "Recommended" plus its description; nl gives "Prioriteiten".
- `LegendCopyResolutionTest` (`@Logic`): every `legendGroups` type's `stepLabelKey`, `legendDescription` and every `priorityDescription` resolves through `CopyIndex.find(CopyTable.Screens, key)` in en and nl to a non-empty string.

### Task 4. Step sheet content, gone state and refusal feedback
**Commit:** `feat(ui): add the step sheet content`

Files (`core/ui/…/plan/`):
- `StepSheetContent.kt`:
  ```kotlin
  @Composable fun StepSheetContent(state: StepSheetState, busy: Boolean, onCheck: (CheckStatus?) -> Unit, onSeeToday: () -> Unit, modifier: Modifier = Modifier)
  ```
  - Root: `Column(verticalScroll, padding(start = space5, end = space5, top = space3, bottom = space8), spacedBy(space4))`, tag `step.sheet`. The sheet drag handle is the caller's (CfModalBottomSheet). [R: board padding 12/20/32 and gap 16; iOS uses space5 + an extra top space4 under the native grabber, but the M3 handle already adds height.]
  - **Header**: `Row(spacedBy(space3), verticalAlignment = Top)` holding `CategorySwatch(type)` and a `Column(spacedBy(space1))`.
    - Badges: `FlowRow(spacedBy(space2))` (iOS `ViewThatFits` H/V) of `PriorityTag(text)`, then `CfBadge` for the badge, if any.
    - Title: the resource of `CopyIndex.find(CopyTable.Screens, titleKey)` (0c `CopyEntry`) through `stringResource`, in `screenCardTitle`, `heading()`, tag `step.title`, `heightIn(min = <screenCardTitle line height, font-scaled>)`. It wraps and is never ellipsized.
    - Time: `stringResource(screens_time_zone_time, interval, zone)` + (if `durationLeft != null`) `" · " + stringResource(screens_duration_left, durationLeft)`, in `screenBody`, `mutedForeground`, `fontFeatureSettings = "tnum"`, tag `step.time`.
  - **Why** (only when `reasons` is not empty) and **How** (always): `Column(spacedBy(space2))`. Heading: `screenLabel`, `mutedForeground`, `text.uppercase(locale)`, `heading()`. Rows: Why lines have no bullet; How lines have a 4 dp (`space1`) `foreground` circle bullet with top padding `space2`, in `Row(spacedBy(space3))`. Body in `screenBodyLg`, `foreground`.
  - **Actions**: `Marked` → `CfButton(stringResource(screens_undo), style = Outline, size = Lg, fillWidth = true, minHeight = actionMinHeight, enabled = !busy)`, tag `step.undo`. `Open` → a two-column `Row(spacedBy(space2))` with equal weights. Skip is `Outline`, `step.skip`; "Mark as done" is `Primary`, `leadingIcon = LucideGlyphs.Check.imageVector(2.5f)`, `step.done`. If either label does not fit on one line at the current width and font scale, stack them in a `Column(spacedBy(space2))`, Skip first, then Mark as done [F `StepSheet.swift:156–158`: the `VStack` fallback keeps the `actions` order, Skip then Mark as done]. Decide this with a `SubcomposeLayout` or measure-based `Layout`; never store a measured size in state. `None` → no actions.
  - **Footer**: `screens_checking_off_changes_your_progres_37ae7d`, `screenMeta`, muted, centred.
  - **Announcement and focus** [F `StepSheet.swift`]: remember the previous `(blockId, badge in {Done, Skipped, null})`. When the block is the same and the mark status changes, announce "Done" / "Skipped" / "Check-off undone" through the polite announcer, and request focus on `step.undo` (now marked) or `step.done` (now unmarked). Do nothing on first composition.
  - **Refusal**: `RefusedChangeFeedback()` as the bottom slot, so a refusal shows inside the sheet (§5.3).
- `StepGoneContent.kt` (`SheetStates`; [F] iOS gone): `Column(padding(start = space5, end = space5, top = space3, bottom = space6), spacedBy(wizardContentGap 14))`, tag `step.gone`. [R: board gap 14 and padding 12/20/24; iOS uses the regular sheet stack, gap `space4` 16. The board draws this exact state, so it wins here; list it in the capture review.]
  - A 48 dp (`stepGoneCircleSize`) `muted` circle with lucide `search-x` at 22 dp (`stepGoneIconSize`) in `dawnText`, start-aligned (board), hidden from accessibility.
  - Title `screens_this_step_is_no_longer_in_your_plan` in `screenCardTitle`, heading.
  - Body `screens_your_plan_changed_after_you_opene_3344b8` in `screenBodyLg` (16/24), muted.
  - `CfButton(screens_see_today_s_steps, Primary, fillWidth = true, minHeight = actionMinHeight)` → `onSeeToday()` [F: iOS only dismisses].
- `PoliteAnnouncer.kt` (only if §4 step 6 found none): `class PoliteAnnouncer { fun announce(text: String) }` plus `@Composable fun PoliteAnnouncerHost(announcer)`: a zero-size `Box` with `semantics { liveRegion = LiveRegionMode.Polite; contentDescription = current }`. It alternates a trailing invisible U+200B between equal texts, so a repeated "Done" is re-announced. Provide it through `LocalPoliteAnnouncer`.
- `RefusedChangeText.kt`: `@Composable fun refusedChangeText(notice: SyncRefusalNotice): String?`. `SleepTimes` → `screens_couldn_t_save_your_sleep_times_so_bbbd9b`; `StepDone(type)` → `screens_couldn_t_save_step_as_done_so_we_687bfc` with the step label from `CopyIndex.find(Screens, key)` where `key = stepLabelKey(type)`; when `stepLabelKey(type) == null`, return `null` (iOS shows nothing, §5.3).
- `RefusedChangeFeedback.kt`: `val LocalRefusedChanges = staticCompositionLocalOf<RefusedChangeModel?> { null }` (iOS environment default `nil`) and `@Composable fun RefusedChangeFeedback(model: RefusedChangeModel? = LocalRefusedChanges.current, modifier)`. A `null` model draws nothing. In the same file, `RefusedChangeHosts` (§5.3): an `object` holding a main-thread `ArrayList<Any>` of host tokens with `register(token)`, `unregister(token)` and `isTop(token)` backed by a `mutableStateOf` revision so readers recompose. The feedback registers a `remember { Any() }` token in `DisposableEffect`; while on top and `visible != null`: `LaunchedEffect(id) { val text = …; if (text != null) hostState.showSnackbar(text, duration = Long); model.consume(id) }`. `CfSnackbarHost(hostState, Modifier.testTag("sync.refusal"), onPresented = { RefusedChangeDebug.presented() })`. `RefusedChangeDebug.presentedCount` is debug only (`BuildConfig.DEBUG`-guarded no-op in release).

Edge cases: very long Dutch titles wrap; at `largeFont` the actions stack and the content scrolls. A block with no reasons hides Why. `busy` disables all three buttons, with alpha 0.5 per CfButton. A non-actionable block shows no actions but keeps the footer [F: the footer is unconditional].

Tests:
- `StepSheetContentTest` (`@Logic`, `pixel8`, en, light; `LocalHaptics provides RecordingHaptics()`):
  - `doneCallsCheckDoneWithoutHaptic`: click `step.done` → `onCheck(Done)` once; `RecordingHaptics.events` is empty.
  - `skipAndUndoCallCheck`: `step.skip` → `Skipped`; with `Marked`, `step.undo` → `null`.
  - `busyDisablesActions`: `assertIsNotEnabled()` on `step.done` and `step.skip`.
  - `statusChangeAnnouncesAndMovesFocus`: recompose from Open to Marked(Done) → the announcer node's content description is "Done" and `step.undo` `assertIsFocused()`; back to Open → "Check-off undone" and `step.done` is focused.
  - `goneSeeTodayCallsBack`: `step.gone` exists, the heading is present, and the button calls `onSeeToday`.
  - `refusalShowsOnceOnTopHost`: two hosts, the inner one registered last → only the inner presents (`presentedCount` +1); after consume, a replayed result does not present again.
- `RefusedChangeTextTest` (`@Logic`): exact en and nl strings for `StepDone(avoid_light)` and `SleepTimes`, as in §5.6 (iOS `RefusedChangeTests`).
- `StepSheetGuardsTest` (`@Layout`, parameterised: states `running`, `upcoming`, `done`, `skipped`, `information`, `gone` × profiles `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl × light, dark). Host the content in a `Box(widthIn(max = 640.dp)).background(card)` (a `ModalBottomSheet` is a separate window, which guards cannot read). Call `Guards.assertAll(rule, profile, headingTags = setOf("step.title") or "step.gone")`. To fit the 5 s budget, split one test per profile × state (AGENTS Tests 5).
- `ItemDetailCaptureTest` (`@Capture`, Robolectric): `ItemDetail_running_pixel8_en_light`, `ItemDetail_done_pixel8_en_light`, `ItemDetail_skipped_pixel8_en_light`, `SheetStates_stepGone_pixel8_en_light`, plus `_nl_dark` for running.

### Task 5. Legend sheets
**Commit:** `feat(ui): add the steps legend and the priority legend`

Files (`core/ui/…/plan/`):
- `LegendSheetContent.kt` (`PlanLegend`, [F `LegendSheet.swift`]): `@Composable fun LegendSheetContent(onClose: () -> Unit, modifier)`, tag `legend.sheet`.
  - Header `Row(padding(start = space5, top = space3, end = space3, bottom = space2))`: the title `screens_what_the_steps_mean` (`screenCardTitle`, heading, weight 1) and a close button. The button is a 44 dp (`PlanLegendMetrics.closeVisual`) round `surface2` with lucide `x` at 18 dp, `contentDescription = screens_close`, a 48 dp touch target (`minimumInteractiveComponentSize`), and tag `legend.close`.
  - Body `Column(verticalScroll, padding(start = space5, end = space5, top = space1, bottom = space8), spacedBy(space5))`. The intro is `screens_each_kind_of_step_has_its_own_ico_47f13c` (`screenBody` muted). Then one section per `legendGroups` entry:
    - each section is a `Column(spacedBy(space2))` (board `section` gap 8; iOS `space3`, board wins for this board-only sheet layout);
    - the heading (`screenLabel` muted uppercase, heading);
    - a list with `spacedBy(PlanLegendMetrics.listGap)` of rows, each `Row(heightIn(min = rowMinHeight), spacedBy(space3))`: `CategorySwatch`, then the name (`screenBodyLg`, weight 500, from `stepLabelKey`) over the hint (`legendDescription`, `screenBody` muted). Each row merges its semantics.
  - Then "How important" (`screens_how_important`, label style, heading), the "Priority" label (`screens_priority`, `screenLabel`), and a 2-column grid (`spacedBy(space2)` both axes) of `PrioritySample` + `priorityKey` (`screenBody` weight 500) + `priorityDescription` (`screenMeta` muted). Each cell merges its semantics. [F iOS copy wins over the board's sample hints.]
- `PriorityLegendContent.kt` (trip-page "Legend", [F `TripPageView.swift:95–103`]): the same header pattern with the title `plan_legend` and close button label `plan_done`, tag `legend.priorities`, then `PriorityKey()` with padding `PlanMetrics.gutter`. [R: the board-style header replaces the iOS navigation bar; OQ-6d-3.]

Tests:
- `LegendSheetContentTest` (`@Logic`): `legend.close` calls `onClose`; the headings "What the steps mean", "SLEEP"…"TRAVEL" (uppercased) and "How important" exist; the row "Avoid bright light" has merged text containing its description; nl uses "Wat de stappen betekenen".
- `LegendGuardsTest` (`@Layout`, both contents × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl × light, dark): `assertAll` with heading tags.
- `PlanLegendCaptureTest` (`@Capture`): `PlanLegend_pixel8_en_light`, `PlanLegend_pixel8_nl_dark`, `PlanLegend_priorities_pixel8_en_light`.

### Task 6. Selection and check-off writer in the trip page ViewModel
**Commit:** `feat(trip): select steps and write check-offs through the outbox`

Files:
- `feature/trip/…/TripPageViewModel.kt` (6b), additions:
  - `val sheet: StateFlow<TripSheet?>`, initialised from `initialRoute.sheet`, with `fun openStep(blockId)`, `fun openLegend()`, `fun openTripActions()` and `fun dismissSheet()`. `openTripActions` is a no-op without a card.
  - **6b's seam** (§4 step 11): `selectBlock(id)` becomes `openStep(id)` (keep `selectBlock` as the name the timeline already passes, delegating, or rename the callers; one entry point either way). Replace 6b's `selectedStepId: StateFlow<String?>` with `val selectedStep: StateFlow<TripSheet.Step?> = sheet.map { it as? TripSheet.Step }.stateIn(viewModelScope, Eagerly, initial)`; 6e Task 3 reads `vm.selectedStep.value?.blockId`. Update 6b's tests that read `selectedStepId`.
  - `val stepSheet: StateFlow<StepSheetState?>` = `combine(sheet, detail, clock, settings)`: for `Step(id)`, `stepSheetState(…)`; otherwise null. Run it on `Dispatchers.Default` with `stateIn(viewModelScope, WhileSubscribed(5_000), null)`.
  - `val checkBusy`, `fun setCheck(blockId, status)` per §5.5. `outbox` and `mutationPermit` come from the session graph the factory captured (4e).
  - The initial-sheet rule and the notification one-shot of §5.4.
  - `val tripActions: StateFlow<TripActionsUi?>`, with `data class TripActionsUi(val tripId: String, val header: TripActionsHeader, val availability: TripActionsAvailability)` in `feature/trip/…/actions/TripActionsUi.kt`: null without a card; else the header (Task 1) and the availability (`canEdit = card.canEdit`, `canDuplicate = card.canDuplicate`, `online` from 6c's `online` flow, `busy = false`).
  - `pendingAnnouncement` for custom actions (§5.8): `fun checkFromAccessibility(blockId, status)` calls `setCheck` and records the expectation. `val announcements: Flow<StepBadge?>` emits when the prepared block's status reaches it.
- `feature/trip/…/step/TripPageCheckOff.kt` (optional): keep `setCheck` testable without Android.

Tests (plain JVM in `feature/trip/src/test/…`, no Robolectric; they use 6b's fixture-backed `TripPageSource` and a fake `OutboxRepository` port, or 4d's JDBC repositories when `:feature:trip` tests can reach them; follow how 6b's VM tests are built):
- `TripPageSelectionTest` (port of `TripPageSelectionTests.selectionKeepsTheTappedBlockAndLiveChecksThroughReplacementAndRemoval`):
  - `openStep(id)`; select another day; toggle the view → `sheet` unchanged.
  - With the fixed clock at the block's start (as iOS), live marks done → null → skipped are reflected in `stepSheet`: badge Done, then **Now** (the block is running, so an unmarked step shows Now), then Skipped.
  - Reselecting the same block keeps it.
  - Removing the block from the plan → `stepSheet == Gone`, and `setCheck` is a no-op (no enqueue).
  - Deleting the plan versions → detail null → `Gone`, with the selection kept.
- `StepCheckTest` (port of `StepCheckTests.localCheckUndoAndReplanAreObservedThroughTheOutbox`):
  - `setCheck(Done)` → one enqueued `CheckOffChange` with the detail's `inputHash`, the key's type, localDate and ordinal, and `status = Done`. The merged detail shows the mark, the prepared block status is Done, and the pending count is 1.
  - A replacement detail with a new hash keeps the pending mark (1a merge).
  - Undo enqueues `status = null` and the marks become empty.
  - An invalidated permit → the cancellation is swallowed by the job; the pending count is unchanged and no error state appears.
  - `checkBusy` blocks a second call while the first is suspended (gate the fake `enqueue` with a `CompletableDeferred`).
  - Non-actionable or no-checkKey blocks → nothing is enqueued.
- `InitialSheetTest`:
  - A seed `Step(existing)` opens on the first Ready; `Step(missing)` → `sheet == null`.
  - A notification hash match opens once; after `dismissSheet()` and a new emission it does not reopen; a hash mismatch does not open.
  - `TripActions` without a card → null.
- `CheckOffOutcomeTest` (the 422/429/408 cases from the plan). The landed `CheckOffReconcilerTests` and JDBC `SyncStore` are in `:core:data`; put this test beside them there (the JVM `:core:sync` cannot depend on Android data fixtures), reusing their scripted `DataSource` and `SyncDatabase` fixtures (`grep -rn "class CheckOffReconcilerTests" core/`). Drive `SyncEngine` + `RefusedChangeModel`:
  - `throttled429KeepsMarkAndShowsNoMessage` and `timeout408KeepsMarkAndShowsNoMessage`: the mark stays in the merged detail; `visible == null`.
  - `stale422PullsOnceRetriesOnceNoMessage`.
  - `nonStale422RefusesUndoesAndShowsMessage`: after the refusal and the next Idle pull, the mark is gone and `visible` is the `StepDone(type)` refusal.
- `CheckOffRestartTest` (`:core:data` JDBC tests, next to 4d's `OutboxRepository` tests): enqueue Done on a file-backed JDBC database, close it, reopen it → the pending row is there and the merged detail (1a `PlanDecoder`) shows Done; enqueue undo, reopen → no mark. This is the cheapest proof of "survive an app restart".

### Task 7. Trip page: open the step and legend sheets
**Commit:** `feat(trip): open the step and legend sheets from the trip page`

Files:
- The TripPage Nav3 entry (§4 step 2): `clazzContentKey = { it.copy(sheet = null).toString() }` and `onRouteChange` per §5.2. Mirror `vm.sheet` into the route with `LaunchedEffect(sheet) { if (route.sheet != sheet) onRouteChange(route.copy(sheet = sheet)) }`.
- `feature/trip/…/step/TripPageSheets.kt`: `@Composable fun TripPageSheets(vm: TripPageViewModel, …)`, composed once by the page.
  - `Step` → `CfModalBottomSheet(onDismissRequest = vm::dismissSheet, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false))` with `StepGoneContent` or `StepSheetContent(state, busy, onCheck = { vm.setCheck(id, it) }, onSeeToday = hide-then-dismiss)`. "Hide then dismiss" is `scope.launch { sheetState.hide() }.invokeOnCompletion { vm.dismissSheet() }`. While `stepSheet == null` (detail still resolving), compose nothing.
  - `Legend` → `PriorityLegendContent`; `StepsLegend` → `LegendSheetContent`. Both use `skipPartiallyExpanded = false` (iOS medium/large).
  - `TripActions` → Task 8.
  - Wrap the sheets' content in `PoliteAnnouncerHost`.
- Timeline: pass `onBlock = vm::openStep` (a stable reference) where 6b passed its placeholder.
- List mode (6c `DayListRow`/`ZoneChangeCard`): their `onBlock` is the same `vm.openStep` reference. **No selected-row outline in 6d**: 6e Task 3 owns it, wide only ("never in phone list mode", 6e checklist; 6c §3).
- `trip.legend` → pass `onLegend = vm::openLegend` (6b left it `null`, which disabled the button).
- Shell: put `RefusedChangeFeedback()` in `AppShell`'s Scaffold `snackbarHost` slot, and provide `LocalRefusedChanges` from the session graph.
- Seeds (4e `DebugSeedRouting`): `CF_PLAN_TEST_SHEET=legend` → `TripSheet.StepsLegend` (iOS `AppTabs.swift:60,76` opens the full `LegendSheet` for this seed; iOS opens it over the tabs, Android over the seeded trip page [R]); add the Android-only debug value `priorities` → `TripSheet.Legend` for its capture (no iOS seed; record it in the PR). Trip actions use the iOS seed `CF_WIZARD_BOARD_CAPTURE=TripActions`: once the page has a card, open `TripSheet.TripActions` once (iOS `TripPageDestination.swift:132–138`). Update 4e's parse test table.

Tests (`@Logic`, `pixel8`, fixture-backed source):
- `TripPageSheetsTest`:
  - `blockSemanticClickOpensStepSheet`: `performSemanticsAction(OnClick)` on `trip.block.<id>` → `step.sheet` exists and the title is the block's label.
  - `listRowTapOpensSheet`: toggle to list, click the row → `step.sheet` exists and `vm.selectedStep.value?.blockId` is that id.
  - `legendButtonOpensPriorityLegend`: `trip.legend` → `legend.priorities` exists; `legend.close` dismisses.
  - `doneThenUndoInSheet`: `step.done` → `step.undo` appears (fake outbox applied synchronously) → click → `step.done` returns.
  - `noHapticOnCheckOff` with `RecordingHaptics`.
- `TripPageSheetRouteTest` (in `:app`, exercising the actual shell decorators): open a step → the stack top equals `route.copy(sheet = Step(id))`; the `TripPageViewModel` instance is the same (`===`) as before; `LazyListState.firstVisibleItemIndex` is unchanged; dismiss → the stack top has `sheet == null`. **If this fails, apply OQ-6d-2's fallback and record it.**
- Re-run `TimelineInvalidationTest` unchanged; it must still pass.

### Task 8. Trip actions sheet
**Commit:** `feat(trip): add the trip actions sheet`

Files (`feature/trip/…/actions/`):
- `TripActionsSheet.kt` [F `TripActionsSheet.swift`; boards `TripActions`, `TripActionsNoPlan`]: `@Composable fun TripActionsContent(ui: TripActionsUi, onAction: (TripAction) -> Unit, onCancel: () -> Unit)`, tag `trip.actions`.
  - Column `spacedBy(space3)`, padding `start/end = space4`, `top = space3` (board 12; iOS space6 under its own grabber), `bottom = space8`.
  - Header `Column(padding(horizontal = space1))`: the title (`screenCardTitle`, heading, tag `trip.actions.title`) and the subtitle (`screenBody`, muted, `tnum`).
  - A card (`surface2`, `RoundedCornerShape(radius2Xl)`) holding the Edit row, a `CfDivider` (`border`), then the Duplicate row.
  - Row: `Row(heightIn(min = space14), padding(horizontal = space4, vertical = space2), spacedBy(wizardContentGap), verticalAlignment = CenterVertically)`, then `cfClickable(enabled)` and `cfFocusRing`.
    - Icon: lucide `pen-line` or `copy`, `TripActionsMetrics.rowIcon` (20), `foreground`.
    - Title: `plan_edit_trip` or `plan_duplicate_trip`, `screenBodyLg` weight 500.
    - Subtitle: `plan_needs_a_connection` when disabled; for an enabled Duplicate, `plan_same_flights_and_answers_new_dates`. `screenBody`, muted.
    - Disabled: the whole row at alpha `priorityOptionalOpacity` (0.5), `disabled()` semantics.
    - Tags `trip.actions.edit`, `trip.actions.duplicate`. Role Button, merged semantics.
  - Cancel: a plain text button `plan_cancel` in `wizardSecondary`, full width, `heightIn(min = AuthTokens.touch)` (44, iOS `TripActionsSheet.swift:52`) with a 48 dp touch target, centred, tag `trip.actions.cancel`. It is disabled while busy (always false in 6d).
- In `TripPageSheets`: `TripActions` → `CfModalBottomSheet(skipPartiallyExpanded = true)` (iOS content-height detent) with `TripActionsContent`. `onAction` → hide the sheet, then `vm.dismissSheet()`, then open the wizard seam with `Route.Wizard(WizardMode.Edit(tripId))` or `Duplicate(tripId)`, the same way 6a opens `WizardMode.Create` (§4 step 5). No dismiss gating: `busy` is always false in 6d and Phase 2's `CfModalBottomSheet` has no gesture parameter; 9h adds the iOS `interactiveDismissDisabled(busy)` equivalent together with its busy state.
- `trip.actions.open` (6b/6c top bar, `more_vert` per OQ-A11, already drawn with a nullable `onActions`): pass `onActions = if (tripActions != null) vm::openTripActions else null`, so the button is disabled without a card (iOS `TripPageDestination.swift:234–239`). Its content description stays 6b's `plan_trip_actions`.

Tests:
- `TripActionsContentTest` (`@Logic`):
  - `offlineDisablesBothWithNeedsConnection`: `trip.actions.edit` and `trip.actions.duplicate` are `assertIsNotEnabled()`, and their merged text contains "Needs a connection.".
  - `onlineEnabledDuplicateShowsSubtitle`.
  - `cardFlagFalseDisablesOnlyThatAction`.
  - `editClickNavigatesToWizardSeam`: through the page with a fake navigator, the click → the sheet is dismissed and the navigator receives `Wizard(Edit(tripId))`; the same for Duplicate.
  - `noCardDisablesOpenButton`.
- `TripActionsGuardsTest` (`@Layout`; with plan, no plan, offline × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl × light, dark): `assertAll` with heading `trip.actions.title`.
- `TripActionsCaptureTest` (`@Capture`): `TripActions_pixel8_en_light` (header "Amsterdam → Tokyo", online), `TripActionsNoPlan_pixel8_en_light`, `TripActions_offline_pixel8_nl_dark`.

### Task 9. Custom actions, keyboard and D-pad
**Commit:** `feat(trip): add check-off custom actions and keyboard activation`

Files:
- `feature/trip/…/timeline/TimelineTargets.kt` (1d): new stable parameters `actions: DayCheckActions` and `onCheck: (String, CheckStatus?) -> Unit`. For each BLOCK child:
  - `customActions` from `actions.byBlock[id]` per §5.8; labels via `stringResource` read once at the top of `TimelineTargets`;
  - `focusable()`, `cfFocusRing(…)` and the `onKeyEvent` of §5.8.
  
  DIVIDER and TICK children stay unfocusable.
- `DayTimeline` passes `actions` and `onCheck` through. The page passes `onCheck = vm::checkFromAccessibility` (a stable reference). `DayCheckActions` comes from the prepared day (Task 1), so an untouched day gets an equal instance and stays skipped.
- `DayListRow` (6c, `feature/trip/…/list/`): new parameters `actions: StepActions` (from the day's `DayCheckActions`) and `onCheck`. Add the same `customActions` **inside** 6c's `clearAndSetSemantics { … }` block (anything outside it is cleared). Keep 6c's content description, which already ends with "Marked done"/"Skipped" (6c §5 label rule); add no `stateDescription`. `ZoneChangeCard` gets none (a zone change is not actionable).
- Page: a `PoliteAnnouncerHost` fed from `vm.announcements` (Done → `screens_done`, Skipped → `screens_skipped`, undo → `screens_check_off_undone`).
- **Focus after a day jump** (§5.8): in 6b's `DayHeader`, give the node tagged `trip.dayHeader.<id>` (the one carrying `heading()`) `cfFocusRing(RectangleShape).focusRequester(requester).focusable()`. The page holds `var focusDay by remember { mutableStateOf<String?>(null) }`: after the scroll of a `ScrollCommand` with `animated == true` finishes (the coroutine that runs `animateScrollToItem`/`scrollToItem` returns), set `focusDay = dayId`. `DayHeader(day, requestFocus = focusDay == day.id, onFocusRequested = { focusDay = null })` runs `LaunchedEffect(requestFocus) { if (requestFocus) { requester.requestFocus(); onFocusRequested() } }`. Pass `requestFocus` as a `Boolean`, so only the target header recomposes.

Edge cases: a marker or zone-change target of a non-actionable block has no custom actions. Activation while the placeholder shows does nothing (no targets yet). Custom actions on a marked row offer only Undo.

Tests (`@Logic`, `pixel8`):
- `TimelineTargetActionsTest`:
  - `unmarkedActionableOffersMarkAndSkip`: `SemanticsProperties`/`SemanticsActions.CustomActions` on `trip.block.<id>` has labels ["Mark as done", "Skip"]; invoking "Skip" calls `onCheck(id, Skipped)`.
  - `markedOffersUndoOnly`.
  - `informationBlockHasNone`.
  - `enterKeyOpensBlock`: `requestFocus()` on the node, `performKeyInput { pressKey(Key.Enter) }` → `onBlock(id)`. The same for `Key.DirectionCenter`.
  - `nlLabels`: "Markeer als gedaan", "Overslaan".
- `DayListRowActionsTest`: the same three label cases on a list row (`trip.block.<id>` in list mode); the content description still ends with "Marked done" for a done row.
- `DayJumpFocusTest`: pick `trip.dayMenu.day.3` (Reduce Motion on, as 6b's `menuJumpReachesTheEnd`) → `trip.dayHeader.<day3>` `assertIsFocused()`; a replacement plan that sends `ScrollCommand(animated = false)` moves no focus.
- `CustomActionAnnouncesTest`: invoke "Mark as done" on a target with the fake outbox applied → the page announcer's content description becomes "Done"; no haptic.
- `TimelineInvalidationTest`: add `customActionCheckOffRedrawsOneDay`, the existing check-off case driven through `checkFromAccessibility`. Only day k's STATIC and TARGETS keys change (deltas as in 1d). Keep the positive control.
- `./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true`: `TimelineTargets` and `DayTimeline` stay `restartable skippable`, with all parameters stable.

### Task 10. Emulator journey, captures and perf
**Commit:** `test(trip): add the step check-off journey and sheet captures`

Files:
- `app/src/androidTest/…/trip/TripPageStepTest.kt` (`@Smoke`, extends `ScreenTest`; port of `AppUITests/TripPageStepTests.testTripBlockActionsAndListReopen`). Seeds: `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000002`, `CF_PLAN_TEST_FREEZE_CLOCK=1` (or 6b's equivalent), and a fixed `CF_AUTH_TEST_ID`.
  1. Wait for `trip.timeline.ready`; `performSemanticsAction(OnClick)` on `trip.block.caffeine-caffeine_serving-29864460` (the visible tap path is covered by 1d's `tapResolvesTile`).
  2. `step.sheet` → click `step.done` → wait for `step.undo` → click → wait for `step.done` → click `step.skip` → wait for `step.undo`.
  3. Press back (`Espresso.pressBack()`) → `step.sheet` is gone.
  4. Toggle to list, click that block's row → `step.sheet` with `step.undo`.
  5. **Restart:** `scenario.close()`, then `launch(sameSeeds)` with the same `CF_AUTH_TEST_ID` → open the block → `step.undo` exists (the skip survived through the outbox and DB). If 4e's seeding resets the account DB on every launch, drop step 5, rely on Task 6's `CheckOffRestartTest`, and record it in the PR.
  
  If the trip or block id is not in the Android mock fixtures (§4 step 9), use the first actionable block with a `checkKey` of the seeded trip and record it. The test must stay under 10 s; split step 5 into its own method if needed.
- `app/src/androidTest/…/trip/TripSheetsCaptureTest.kt` (`@Capture`, device). Seeds `CF_PLAN_TEST_SHEET=step:<id>` (running and done, with the frozen clock), `legend` and `priorities`; for trip actions, `CF_WIZARD_BOARD_CAPTURE=TripActions` (Task 7). Names: `ItemDetail_running_device_en_light`, `ItemDetail_done_device_en_light`, `PlanLegend_device_en_light`, `TripActions_device_en_light`, `TripActionsNoPlan_device_en_light` (a no-plan trip), `SheetStates_stepGone_device_en_light`. The step-gone capture needs a debug-only route with a missing block that opens after first Ready: seed `CF_PLAN_TEST_SHEET=step:missing` together with a debug flag `CF_PLAN_TEST_FORCE_GONE=1` that skips the initial-sheet check. Add that flag only if it stays a one-line debug branch; otherwise capture step gone on Robolectric only (Task 4) and record it.
- Perf: run `make perf-timeline`. The results must pass every emulator gate of plan.md "Where gates apply" (the CPU band against the last three `trip` rows included); list each ±25 % `FLAGGED` line with a one-line cause, which does not fail. Record them in `docs/perf-timeline.md` under "Phase 6d", with the compose-report lines of Task 9. If the timeline targets changed the startup or the scroll path, regenerate the baseline profile (`make baseline-profile`, or 1e's command) and commit it in this task.

---

## 7. Acceptance criteria

1. `make check` passes. It includes every new `@Logic`, `@Layout` and plain JVM test, and the `@Smoke` `TripPageStepTest` on `pixel`. Each test is within budget in `build/test-results/*/timings.json`.
2. `make targeted ONLY=":core:plan:StepSheetStateTest :core:plan:TripActionsTests :core:sync:RefusedChangeModelTests :core:sync:CheckOffOutcomeTest :core:data:CheckOffRestartTest :feature:trip:StepCheckTest :feature:trip:TripPageSelectionTest :feature:trip:TripPageSheetRouteTest :feature:trip:TimelineInvalidationTest"` passes.
3. **Mark, Skip and Undo survive an app restart through the outbox**: `CheckOffRestartTest` passes, and `TripPageStepTest` step 5 passes (or its recorded fallback).
4. **TalkBack offers the custom actions**: `TimelineTargetActionsTest` and `DayListRowActionsTest` pass. Also do one manual TalkBack pass on `pixel` (Actions menu on a block shows "Mark as done" and "Skip"; after Mark as done, "Done" is spoken) and note it in the PR.
5. **The sheets match their boards**: `make captures ONLY=ItemDetail,PlanLegend,SheetStates,TripActions,TripActionsNoPlan` and `make captures DEVICE=1 ONLY=…` write the PNGs listed in Tasks 4, 5, 8 and 10. Review them side by side with the boards and list every deliberate difference (§8 checklist) in the PR.
6. Guards pass at `small`, `pixel8`, `foldInner`, `landscapeSideNav` and `largeFont`, in en and nl, light and dark, for every sheet state.
7. `make perf-timeline` meets the gates, and `docs/perf-timeline.md` has the "Phase 6d" row. `TimelineInvalidationTest` shows that a check-off (button or custom action) redraws day k only.
8. No raw color, no `MaterialTheme.colorScheme` and no unannotated dp literal in `feature/trip` or `core/ui` (Konsist). There are no new strings, and no `LocalHaptics.current.perform` call is reachable from a check-off.
9. Debug runs are StrictMode-clean while opening sheets and checking off.

---

## 8. Review checklist (Opus reviewer)

- [ ] **Route key.** The sheet lives in `TripPage.sheet`. The entry's content key drops `sheet`; the VM and the scroll position survive opening and closing (`TripPageSheetRouteTest`). No token or secret is in `TripSheet` or `SavedStateHandle`.
- [ ] **Initial sheet.** Seeds and recreation open a step only when the block exists at the first Ready. The notification one-shot compares `notificationInputHash` with the detail hash, runs once and never stores the token. The open sheet shows Gone (not a dismissal) when its block or detail disappears.
- [ ] **setCheck** matches `TripPlanModel.swift:132–141`: guards, `planInputHash` from the **current detail**, the key's type/localDate/ordinal, `status = null` for undo, the permit passed, `CancellationException` rethrown, no UI on error, no body logged. The busy guard is present.
- [ ] **No extra writes.** The sheet does not write marks to the DB directly. The only path is `OutboxRepository.enqueue`, and sync runs from 4d's outbox-commit trigger.
- [ ] **Refusal.** It is published only on `Idle` with `retryAfter == null`; the last notice wins; it shows once (replay-safe); it shows in exactly one host (sheet over shell). The text is byte-exact in en and nl. The counter is debug only.
- [ ] **Sheet content** vs ItemDetail/iOS: swatch 32 (iOS) vs board 36: iOS kept. Section gap 8 (iOS `space2`) vs board 6: iOS kept. Heading tracking 0 (iOS `screenLabel`) vs board 0.05em: iOS kept. M3 drag handle vs the 36 × 5 grabber (OQ-2-3). Content padding 20/12/32. Title never ellipsized and has min height 32 sp scaled. Badges: Done secondary, Skipped outline, Now dawn; a non-actionable block shows no Done/Skipped badge even with a mark. Why is hidden when empty; How always shows. The footer is always shown. Step gone: board gap 14 vs iOS 16 (board kept).
- [ ] **Actions layout.** Two columns when both labels fit, stacked otherwise (Skip first, as iOS); no measured size stored in state.
- [ ] **Announcements** use a polite live region (no `announceForAccessibility`), only on a status change of the same block, not on first composition. Focus goes to Undo or Mark as done. After a user day jump, focus lands on the target `trip.dayHeader.<id>`; restores and replacements move no focus.
- [ ] **Legend.** The trip page opens the PriorityKey "Legend" (OQ-T22); the full legend is reachable by seed; iOS copy is used, not the board's sample hints.
- [ ] **Trip actions.** Only Edit and Duplicate plus Cancel: no calendar, complete or delete. Disabled rows show "Needs a connection." whatever the reason (iOS). Duplicate's subtitle shows only when it is enabled. The header has no end date and no "with return" without a detail. Edit/Duplicate dismiss the sheet before navigating. The open button is disabled without a card.
- [ ] **Custom actions** only on actionable blocks with a `checkKey`. They are prebuilt per day, and stable parameters keep untouched days skipped (compose report). Key handling is on KeyUp; Space, Enter, NumPadEnter and DirectionCenter work.
- [ ] **No haptics** anywhere in the check-off path (RecordingHaptics asserts).
- [ ] **Tests** are at the cheapest tier. There is one emulator journey. Tags are the iOS ids (`step.sheet`, `step.title`, `step.time`, `step.done`, `step.skip`, `step.undo`, `step.gone`, `legend.sheet`, `legend.close`, `trip.legend`, `trip.actions`, `trip.actions.open`, `trip.actions.edit`, `trip.actions.duplicate`, `trip.actions.cancel`, `sync.refusal`). No `Thread.sleep`, no node loops.
- [ ] **Unboarded states** were not added: no sheet loading skeletons and no "trip isn't on this phone" sheet.
- [ ] **Reuse, not copies.** 6a's `CategorySwatch` is reused; 6b's `selectedStepId` is replaced by `selectedStep` (6e reads it); no selected-row outline in 6d (6e owns it).
- [ ] **Commits** follow Conventional Commits with no trailers, one per task, with the plan ticks in the same commits.

---

## 9. Open questions (proceed on the default)

These thirteen defaults are recorded in plan.md as OQ-A292–OQ-A304.

| ID | Question | Default |
|---|---|---|
| OQ-6d-1 | The trip page's legend button: the iOS "Legend" sheet (PriorityKey only) or the board's full `PlanLegend`? | **iOS parity** (OQ-T22). `TripSheet.Legend` = PriorityKey. The full legend is built as `TripSheet.StepsLegend`, reachable by the `legend` seed (iOS `AppTabs` opens the full legend for that seed), and ready for Phase 7. Switching later is a one-line change |
| OQ-6d-2 | Changing `TripPage.sheet` in place could recreate the Nav3 entry (new ViewModel, lost scroll) if the decorators do not key by `contentKey` | **Default:** `clazzContentKey` without `sheet`. **Fallback** if `TripPageSheetRouteTest` fails: keep the sheet only in `SavedStateHandle["trip.sheet"]` (a JSON-encoded `TripSheet`), read the route's `sheet` once as the initial value, and never write the route. Record the deviation from plan l.403 in the PR |
| OQ-6d-3 | Legend sheet header: the iOS navigation bar (inline title plus a "Done"/"Close" text button) or the `PlanLegend` board header (Fraunces title plus a round X)? | **Board header** for both legends; the X is labelled "Close" (full) or "Done" (Legend), keeping iOS's keys |
| OQ-6d-4 | iOS keeps the refusal toast until the session ends; a Snackbar times out | **Snackbar, `Long`, shown once per refusal id** (OQ-A5). It is not re-shown after a timeout |
| OQ-6d-5 | Where the refusal shows outside the trip page | **Shell Snackbar slot** (all tabs, like iOS `AppTabs`), plus inside the step sheet. Settings (10a) adds nothing |
| OQ-6d-6 | Announcement after a TalkBack custom action (no sheet open) | **Yes**: polite "Done" / "Skipped" / "Check-off undone" when the status lands, reusing the sheet's copy |
| OQ-6d-7 | Step sheet detents: iOS medium/large (large only in a debug option) | `skipPartiallyExpanded = false` for the step and legend sheets; `true` for Trip actions (content height). Captures use the seeded full-height state |
| OQ-6d-8 | Board ItemDetail padding top 12 under the grabber vs iOS space5 + space4 | **Board 12** (`space3`), because the M3 drag handle already adds its own 22 dp band |
| OQ-6d-9 | "See today's steps" | **Dismiss only** (iOS). It does not switch to the Today tab |
| OQ-6d-10 | A mark with an unknown status | Treated like Skipped for the badge and offers Undo (iOS `status == .done ? Done : Skipped`) |
| OQ-6d-11 | The step-gone capture needs a debug path | `CF_PLAN_TEST_FORCE_GONE=1` only if it is a one-line debug branch; otherwise the Robolectric capture of Task 4 is the evidence |
| OQ-6d-12 | The emulator journey's restart step depends on 4e seeding keeping the account DB across a relaunch | Keep it if it does; otherwise the JDBC `CheckOffRestartTest` is the evidence, recorded in the PR |
| OQ-6d-13 | Keyboard focus on invisible timeline targets adds one focus node per block | **Accept.** Re-check `TimelineInvalidationTest`, the compose report and `make perf-timeline`; if a gate fails, make targets focusable only when `LocalInputModeManager.inputMode == Keyboard` and record it |
