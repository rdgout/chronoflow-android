# Phase 9b brief: Wizard generation, airport search and recovery

- **Branch:** `feat/phase-9b-wizard-data`, stacked on the stack top once 9a and 4e are in the stack (4e carries 4a–4d; review 2026-11-16).
- **Depends on:** 9a (the whole wizard model, JVM: types, `MemberWizardModel`, `WizardChecking`, codec, test support), 4e (`SessionGraph`, the ViewModel factory pattern, `Route.Wizard`; it brings 4d: repositories, `StoreApplication`, `StoreTime`, `CanonicalJson`, `SyncEpoch.child()`). Also uses 0d (contracts, fixtures), 4b (`DataSource`, timeouts, `TripWizardErrors`), 4c (`MockDataSource`, `MockWizardPrompts`). **Wave:** 8. **Milestone:** M4.
- **Plan entry:** `plan.md` "### Phase 9b: Wizard generation, airport search and recovery" (l.2649–2687), decisions A3 (modules, l.90), A4 (manual DI, l.91), A5 (persistence, l.92), A6 (timeouts, "the airport search cache is an in-memory LRU, not an OkHttp disk cache", l.93), A8 (process death: "Only wizard recovery (30 min) survives, through its `drafts` row", l.95), OQ 40 (l.160), OQ-A36 (l.623), 9a's OQ-A349–A366 (l.935–952).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa` and web at `e36368ec`.
- **Tests:** follow AGENTS.md "Tests" for every test. This phase is **JVM only**: plain JVM tests in `:core:wizard`; local unit tests on the SQLDelight JDBC driver in `:core:data` (the 4d pattern); plain JVM ViewModel tests in `:feature:wizard`. No Robolectric, no emulator, no screenshots, no UI, no strings. The current rules (test diet, 2026-11-20) override every budget written below; see "Stack check (2026-11-20)".
- **Stack check (2026-11-20, against `c312a5a6`: 0a–9a plus the test-diet branch).** 9a, 4e (with 4a–4d), 0d, 4b and 4c are in the stack. Nothing of 9b exists: no `TripGenerationService`, `AirportSearch*`, `DraftRecord`, `DraftRepository`, `MemberWizardStore`, `WizardRecoveryWriter` or `RecoveryRejected`; `Drafts.sq` holds only the DDL; `feature/wizard/` holds only `build.gradle.kts` (`chronoflow.android.feature`, namespace `net.chronoflow.feature.wizard`; the plugin already adds `:core:navigation`, `:core:model`, `:core:ui`, `:core:designsystem` and lifecycle-viewmodel-compose). Deltas, each patched in place below:
  - **plan.md lines.** 9a l.2613–2647, 9b l.2649–2687, 9c–9h l.2689–2885. 9b ticks: "**Generation**" l.2671, "**Airport search**" l.2675, "**Recovery**" l.2681, "**TripWizardViewModel scaffold**" l.2682, "**Tests**" l.2683; 9g "**After save.**" l.2844. The time-hygiene Konsist rule is l.1432 (was "l.1028").
  - **9a's real API** (read `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`): `MemberWizardDrafts.memberInitial(hasTrip: Boolean, latestOrigin: AirportRef?, savedProfile: JsonObject?, sleepAnswered: Boolean?)` — 9a added `hasTrip` (`firstTrip = !hasTrip`), so OQ-9b-13's override is no longer needed (§6.5, §11). `inputFingerprint`, `hasChanges` and `loadTrip(snapshot, id, action)` are **extension** members on `MemberWizardDraft` in `model/TripWizardActions.kt` (with `enum class TripWizardAction { edit, duplicate }`); `enforceGuestMode()` is `internal`. `TripGenerationState` has a **private constructor with `@ConsistentCopyVisibility`**: `copy` is private, so 9b builds every new state through `TripGenerationState(instance, input, edit, review, savedTripId)` (the `invoke` factory, which sets `input.identity = review?.identity ?: input.identity`) (§6.1). `MemberWizardModel` keeps its flow as `private val mutableSnapshots`; each `internal set` (e.g. `persistenceFailed`) already writes through `mutableSnapshots.update { }` (§6.7). `persist()` chains `internal var pending: Job?`; `flush()` joins it.
  - **9a's cancellation rules (5ae30d3c, 78e2c225, OQ-A366).** `persist()` treats a `CancellationException` from `store.persist` while its own job is active as a rejected/stale write: dropped, **no** `persistenceFailed` (test `MemberWizardReviewTest.foreignCancellationOnPersistIsNotAPersistenceFailure`). A foreign `CancellationException` from `generation.save` or `didCreate` while `saveJob` is active → `wizardOperation` maps it to `Server(null)`, step `ready`, manual retry only (`savePlanForeignCancellationReturnsToReadyWithServerFailure`). The writer (§6.5) and the service (§6.1) are patched to match.
  - **OQ-A366 duplicate-trip risk → new requirement R-A366 (§6.1, Task 1, §9 item 9).**
  - **`WizardMode` clash (OQ-A363).** `net.chronoflow.core.navigation.WizardMode` (`Create`, `Edit(tripId)`, `Duplicate(tripId)`, `core/navigation/…/Route.kt:89`) vs `net.chronoflow.core.wizard.model.WizardMode { member, guest }`. `:feature:wizard` imports the navigation one with an alias (`import net.chronoflow.core.navigation.WizardMode as WizardEntry`).
  - **Factory pattern.** `AppGraphKey` lives in `:app` (`app/…/AppGraphKey.kt`), so a feature cannot read it. Features declare their own `CreationExtras.Key` and `Factory` (`TripsViewModel.Factory` + `TripsDependenciesKey`; `:app` builds `SessionTripsDependencies(graph, session)` and sets the extra in `AppShell.kt:64–72`). §6.8/Task 10 now follow that pattern. `AppDispatchers` is `:app`'s (`graph.dispatchers.io`); `SessionGraph` (a `data class`, built only in `SessionGraphs.kt`) has no permit, no dispatchers and no `wizard`.
  - **`SqlSafetyTest`** (`core/database/src/test/…/SqlSafetyTest.kt`) has **two** REPLACE patterns, `OR\s+REPLACE` and `\bREPLACE\s+INTO` (an `INSERT OR REPLACE INTO` matches both), plus `assertEquals(9, files.size)`. Task 4 narrows both for `Drafts.sq` only.
  - **Records.** 4a's record↔row mappings live in `core/database/src/main/kotlin/net/chronoflow/core/database/store/RecordMapping.kt` (`AccountSql.fetchX`/`saveX`, `JSONDocument.requireValid`, `Boolean.sql()`); records in `core/model/…/model/store/` (package `net.chronoflow.core.model.store`). Put `fetchDraft`/`saveDraft`/`insertDraft`/`deleteDraft`/`fetchListedDrafts` there. 4a's `RecordsTests` is `core/data/src/test/…/data/repository/RecordsTests.kt` (with `RepositoryStore`).
  - **Other names.** `SyncEpoch.isValid` exists (no fallback needed); `StaleSessionException : CancellationException` is what an invalid epoch or revoked `SessionMutationPermit` throws. `AccountDatabase.write` bodies are non-suspending `(AccountSql) -> T`. `StoreApplication` is `internal object` in `:core:data` (`pending(sql)`). `eventually` is `net.chronoflow.core.testing.sync.eventually`. 4c's package is `net.chronoflow.core.datamock`; `MockDataSource(store, seed, offline, …, now, delay, sleep, id, …, zone, io)`. 4b's envelope mapper `ApiErrorMapper` is `internal` → Task 9 uses the literal failures. 4b's wizard routes use `ApiTimeouts.Generation` (`ApiEndpoint.kt:52`, `ApiDataSource.kt` review/save/update): verified, no fallback.
  - **Recovery JSON.** 9a's `MemberWizardRecord.encode` uses the shared `ApiJson.response` (`encodeDefaults` off). `instance` still encodes (its default is a fresh UUID), but `revision = 0` is **omitted**, so `recoveryJsonAlwaysCarriesInstanceAndRevision` will fail as written. Use §5.4 #6's fix commit with a codec-local `Json(ApiJson.response) { encodeDefaults = true }` (not a change to `ApiJson`), and have `updateRecovery` read a missing `"revision"` as 0.
  - **Test diet (AGENTS "Tests" 1, 5).** JVM and warm Robolectric cases < 1 s; 60 s hang cap (`TestTimingListener`). Parameterized Swift tests follow 9a's pattern: `@RunWith(Enclosed::class)` with a nested `@RunWith(Parameterized::class)` class whose method keeps the Swift name (each parameter is its own case), not a loop. No new test may construct `AppGraph`, `SessionGraph(s)`, `RootContent`, `CfRoot` or `MainActivity` (Konsist `AppTestBoundaryRules.kt` allowlist); Task 10's tests use fakes only.
  - **Orchestrator decision (2026-11-20, Task 6 continuation).** The conflated writer stands. The Android-only serial-chain assertion is replaced, in its own commit, by `persistenceNeverGoesBackwardsAndEndsOnTheLatestRevision`: persisted revisions strictly increase, gated persistence writes nothing, and flush ends at revision 2; revision 1 may be omitted. Mutations must prove regression and omission are detected. This is the only authorized 9a test change and supersedes the unchanged-suite wording below for that one test.
  - **Task dependency (repeat check, 2026-11-20).** Task 1’s required duplicate-trip test constructs `RecoveryRejected`, but Task 4 originally introduced that type. Introduce its specified `class RecoveryRejected : Exception()` in Task 1; Task 4 reuses it. This only orders new 9b code and changes no landed behaviour.
  - **OQ numbering.** OQ rows end at **OQ-A366**; next free is **OQ-A367** (Task 11).
  - **Flagged (keep the brief's behaviour; report, do not silently change):**
    1. `wizardOperation` (`model/WizardOperation.kt`) catches `RuntimeException` but not a checked-style `Exception`; `class RecoveryRejected : Exception()` reaching it (e.g. from `didCreate` if the service ever rethrew it) would escape the model's job. The writer and service catch it first, so this is latent; say so in the PR.
    2. 9a's `discard()` uses `catch (failure: Exception)` inside `NonCancellable` (it restores `closed` and rethrows, cancellation included). §10's "no `catch (e: Exception)` without a preceding rethrow" applies to 9b's code only.
    3. Same-named classes: 9a's `:core:wizard` already has `WizardReviewFlowTests`, `WizardCompletionTests`, `TripWizardActionsTests` and `GuestWizardModeTests` (package `net.chronoflow.core.wizard.model`). 9b's `:core:data` classes of the same names (package `net.chronoflow.core.data.wizard`) are fine, but always select them with the module prefix.

Tags: **[F]** fact verified in source (path and line at the pin). **[R]** decision for Android adopted by this brief. Paths written `core/wizard/…/X.kt` mean `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/X.kt` (same scheme for `core/data/…` → `net/chronoflow/core/data/`, `feature/wizard/…` → `net/chronoflow/feature/wizard/`). iOS paths are relative to `chronoflow-ios/Packages/ChronoFlowKit/` unless they start with `plan.md` or `App/`. Read every iOS file at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`.

---

## 1. Goal

Port the iOS wizard's data layer (network, database, search), which has no UI, and host 9a's model in a ViewModel, so that phases 9c–9h only draw screens.

**Ownership with 9a (binding).** 9a (`docs/phases/phase-9a-wizard-model.md`, OQ-9a-1, §6 Tasks 5–7, §11) already ships, in `:core:wizard`: the whole `MemberWizardModel` (every member of `MemberWizardModel*.swift`, `WizardMode.swift`, `TripWizardActions.swift`, with its jobs `checkJob`, `reviewJob`, `saveJob`, `choiceJob`, `retryJob`, the 250 ms choice delay and the retry countdown), `WizardChecking.run` and its 4 tests, the generation value types and the `MemberWizardGeneration` interface (`generation/TripGenerationTypes.kt`, `generation/MemberWizardGeneration.kt`), `TripGenerationAnswers.applyingAnswers`, `WizardDraftStore`, the recovery codec `MemberWizardRecord`, `MemberWizardDrafts`, `WizardFingerprint`, `WizardSaveFailure`, and its test support. **9b never re-ports any of these.** Where this brief describes one of them (§6.3, §6.7), it is a checklist to verify 9a's port against, not work to redo. A Swift line 9a missed is fixed in place in 9a's file, in its own `fix(wizard): …` commit.

What 9b builds:

1. **Generation** (`TripGenerationService`, iOS 13b): review, edit review, prompt answers (through 9a's `TripGenerationAnswers`) and save. In-flight dedup keyed by `(instance, fingerprint, token, reviewRevision, edit)` and guarded by a `Mutex`. A 30 s cap on the review operation, and one silent retry on `review_expired`. Edit source refresh. Saves are **never** retried automatically. Effects merge the profile into the account DB, pull the created trip and request a sync. The 90 s HTTP timeout is 4b's; you only verify it (§4).
2. **Checking timers**: 9a's `WizardChecking.run` (ticks at 0.4 s and 0.8 s, tick 3 at ≥ 1.2 s, a 0.3 s pause, slow at 10 s, a failure at 30 s) and 9a's 250 ms choice delay. 9b only drives them end to end through the service, the DB store and the fixtures (Tasks 7–9).
3. **Airport search** (`AirportSearchService`): the query is trimmed and must be 2–60 characters long; 350 ms debounce; stale searches cancelled; requests spaced ≥ 1.02 s apart; a 429 pushes the next request back; `limit = 8`. Plus an in-memory LRU cache and a sealed search-state type, both Android additions.
4. **Recovery** (`MemberWizardStore`, `DraftRepository`): one hidden `drafts` row with id `member-wizard` (OQ 40). It expires after 30 min, is deleted on ×, on save and on expiry, and is guarded by instance and revision. It is written through a **conflated writer** (`WizardRecoveryWriter`), which replaces the serial `pending` chain in 9a's `MemberWizardModel.persist()`.
5. **`TripWizardViewModel` scaffold** in `:feature:wizard`. It owns 9a's model, starts or recovers it, resumes Checking and Calculating, and exposes state. It draws no screen.
6. **Ported tests**: every iOS generation, search, recovery and DB-backed member-wizard test that 9a leaves to 9b (9a §10.2; 72 methods, table §8), the draft halves of two iOS `RecordsTests` (4a hand-off), plus Android tests for the cache, search session, writer, fixture scenarios and the never-retry rule.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` A3–A8 (l.90–95), "Android spec: state survival" (l.373), "Android spec: storage layout" (l.385), Phase 9a (l.2613), **9b (l.2649–2687)**, 9c–9h (l.2689–2885, to see what they expect from you), OQ 40, OQ-A36, **OQ-A349–A366** (9a's decisions; A363 and A366 bind this phase) | Scope |
| Rules | `AGENTS.md` "Working rules", "Performance", "Tests", "Git", "Traps" (rethrow `CancellationException`) | How to work |
| Research | `docs/research/06-wizard.md` **§3.2, §3.3, §3.5, §3.6, §4.3 (Checking, Ready, Calculating), §4.7, §7, §9.1, §9.3, §9.7**, §11, §12 | Behaviour summary (the iOS sources below win where they differ) |
| Research | `docs/research/03-api-contract.md` §3 (envelope), §4 (rate limits), §5.3 (rows 30, 32–38), **§11** | Wire contract |
| 9a brief | `docs/phases/phase-9a-wizard-model.md` §3, §6 Tasks 1 and 5–8, §10.2, §11 | Exact names 9a ships (they win over any name in this brief) |
| Earlier briefs | `docs/phases/phase-4a-store.md` Task 3 (`SqlSafetyTest`, `Drafts.sq`), Task 6 (`JSONDocument.requireValid`), §11 ("9b" hand-off); `docs/phases/phase-0d-contracts.md` Task 7 (`ApiTripReviewInput`, `ApiTripSaveInput.from`), §fixtures (`core/testing/src/main/resources/fixtures/wizard/*.json`); `phase-4b-api-client.md` Task 2 (`TripWizardErrors`), Task 8 (wizard routes, 90 s); `phase-4c-mock-data-source.md` §3.1, Task 5 (`MockTripGeneration`, `MockWizardPrompts`); `phase-4d-sync.md` §5 (module split, `SyncStore` JDBC pattern, `StoreTime`, `CanonicalJson`, repositories); `phase-4e-session.md` §5.6–5.7 (`SessionGraph`, `Route.Wizard(mode)`, factory) | What you build on |
| 9a output | `core/wizard/**` | Read **before** Task 1 (§4) |

### 2.2 iOS behaviour to mirror (read-only, at `f391a1fa`)

| iOS file | Lines | Port to |
|---|---|---|
| `Sources/TripWizard/Generation/TripGenerationService.swift` | 60–241 (the actor; l.5–58 types are 9a's `TripGenerationTypes.kt`) | `core/wizard/…/generation/TripGenerationService.kt` |
| `Sources/TripWizard/Generation/TripGenerationReview.swift` | 20–77 (`performReview`, `refreshSource`, `sendReview`; l.4–18 `TripGenerationReviewResult` is 9a's) | `…/generation/TripGenerationReview.kt` |
| `Sources/TripWizard/Generation/TripGenerationAnswers.swift` | 5–11 (`answerPrompts` only; `applyingAnswers` is 9a's `TripGenerationAnswers` object) | a member of `TripGenerationService` |
| `Sources/TripWizard/Model/AirportSearchService.swift` | 1–44 | `…/model/AirportSearchService.kt` |
| `Sources/TripWizard/Model/MemberWizardModel.swift` | 236–271 (`discard`, `changed`, `persist`: verify 9a's port and swap `persist`'s chain for the writer, Task 6) | 9a's `…/model/MemberWizardModel.kt` |
| `Sources/TripWizard/Model/MemberWizardCompletion.swift`, `MemberWizardReview.swift`, `MemberWizardRetry.swift`, `MemberWizardNavigation.swift`, `WizardChecking.swift`, `WizardMode.swift`, `TripWizardActions.swift` | — | **9a's.** Read them only to check 9a's port while porting the tests |
| `Sources/TripWizard/Model/MemberWizardStore.swift` | 114–244 (`MemberWizardStore`; `WizardDraftStore` l.108–112 is 9a's) | `core/data/…/wizard/MemberWizardStore.kt` |
| `Sources/LocalStore/DraftRepository.swift` | 1–83 | `core/data/…/wizard/DraftRepository.kt` |
| `Sources/LocalStore/Records/DraftRecord.swift` | 1–69 | `core/model/…/store/DraftRecord.kt` |
| `Sources/LocalStore/SchemaV1.swift` | 29–33 (`drafts` DDL, already 4a's) | named queries only, in 4a's `Drafts.sq` |
| `Tests/LocalStoreTests/RecordsTests.swift` | 27–33, 49–50, 62–64 (`allRecordsRoundTrip…` draft half), 93–96 (`repositoriesBindKeys…` draft half) | Task 4 (4a hand-off, 4a l.577 and §11) |
| `Sources/Sync/TripGenerationEffects+Store.swift` | 1–63 | `core/data/…/wizard/TripGenerationEffectsStore.kt` |
| `Sources/Reminders/FirstTripRemindersOffer.swift` | 15–21 (`queue` only) | `core/data/…/reminders/FirstTripRemindersOffer.kt` |
| `App/AppTabs.swift` | 296–335, 495–540 | wiring reference: store and search per session, **one `TripGenerationService` per wizard opening**, cancelled when the wizard ends or the session changes |
| `App/Session/NativeSessionController.swift` | 175–186 | `wizardGenerationService`: effects `.storing(database, source, epoch, permit, requestSync: sync.requestSync)` |
| `Tests/TripWizardTests/*.swift` | the suites in §8 | ported tests (Swift names) |
| `plan.md` | l.1580–1640 (Phase 13 + implementation decisions), l.1791–1806 (16a, 16b), l.2419 (OQ 40) | Decisions you must keep (quoted below) |

Quotes that bind this port (iOS `plan.md`):
- l.1593: "Wizard writes use the 90 s timeout … transport or timeout failures map to `offline` with no pre-check (D23)."
- l.1606–1611: "review, with in-flight dedup by `(instance, input fingerprint, token, reviewRevision)` and a 30 s review timeout; retry `review_expired` once without identity … save with the same identity. There is no automatic retry: failures surface as typed results. A retry is the caller's call with the same identity. `savedTripId` is returned and kept by the caller so a retry skips the POST; on success, pull the created trip (list plus detail, errors swallowed) and request a background sync".
- l.1615 (decision): "The 30 s deadline covers source refresh and recovery requests; saves retain caller-controlled retries."
- l.1625 (decision): "The actor deduplicates in-flight saves as well as reviews, caches successful identities in memory … Expiry/credit failures return a state with the review/identity cleared and a typed re-review reason; other failures retain it. … `cancel()` permanently ends a session-scoped service."
- l.1792: "persisted in the `drafts` table under one fixed id as a hidden 30-minute recovery record (OQ 40). It is deleted on ×, on save and on expiry, and never listed. A new wizard prefills From with the latest trip's origin and records `firstTrip`."
- l.1798: "The recovery row uses `member-wizard`, millisecond timestamps, session permits and instance/revision guards. … search adds a cancellable 350 ms debounce, shared pacing below 60/min and the server's throttle wait."
- l.1802: "Checking: ticks at 400 and 800 ms, a 1.2 s minimum, slow at 10 s, failure at 30 s. CheckThrottle counts down from `retryAfterSeconds`."
- l.1805: "choices pause 250 ms, Checking pauses 300 ms after tick three, back/swipe cancels Checking and the first question skips it."

### 2.3 Boards

**None.** This phase has no UI and adds **no visible string**. `TripSearchStates` (states: "Fewer than 2 characters", "Searching", "No results", "Error with retry") only names the states of the sealed search type in §6.4. 9c draws them. Do not add strings to `tools/l10n/android-only.json`.

### 2.4 API endpoints [F] (web `routes/api.php` at `e36368ec`, read with `git -C /Users/rickgout/Sites/chronoflow-new show e36368ec:<path>` because the checkout has moved on; base `https://api.chronoflow.net/v1`; requests snake_case, responses camelCase)

| Use | Request | Auth / throttle | Timeout (4b) | Success | Typed failures (4b mapping) |
|---|---|---|---|---|---|
| Airport search (l.55–56) | `GET airports?query=<trimmed>&limit=8` | public (no bearer); `airport-search` 60/min/IP; `Cache-Control: public, max-age=3600` | 15 s | 200 `{airports:[{iata,name,city\|null,country,timezone}]}` | 422 for a query of < 2 or > 60 characters (`app/Http/Requests/Api/V1/SearchAirportsRequest.php`: `'query' => required, string, min:2, max:60`; `limit` `between:1,20`, default 8); 429 → `Throttled(retryAfterSeconds, 429)` |
| Review (l.85–86) | `POST trips/review`, body `ApiTripReviewInput` (fixture `wizard/review-request.json`): first call `draft_token:null, input_revision:0, review_revision:null`, later calls echo `identity`; `profile` only when non-empty | bearer + `verified`; `api` 60/min + `trip-review` 10/min/user (`AppServiceProvider.php:184–199`) | **90 s** | 200 `ApiTripReview {identity, status ok\|modified\|needs_answers\|no_plan, reasons, prompts, summary?, price?, canSave, saveBlockedReason?, itinerary}` | 422 `review_expired` → `ReviewExpired`; `profile*` → `ProfileInvalid`; other fields → `Validation` (journey paths rewritten to form paths); 429 → `Throttled`; transport/timeout → `Offline`; 403 `email_unverified` → `EmailUnverified(403)`; else `Server(status)` |
| Edit review (l.88–89) | `POST trips/{id}/review`, same body + `"source_revision"` (64 chars) | as review | **90 s** | 200 `ApiTripReview` (`identity.sourceRevision` set) | as review |
| Save (l.87) | `POST trips`, body **exactly** `{draft_token, input_revision, review_revision}` (fixture `wizard/save-request.json`) | `api` 60/min + in-action `trip-save` 10/min | **90 s** | **201** `{tripId}`; a repeat with the same identity returns the same id (receipt) | `ReviewExpired`, `NotEnoughCredits` (`credits: "Not enough credits"`), `SaveCapacity` (`review: "Try saving again shortly."`), `Throttled`, `Offline`, `Server` |
| Update (l.90) | `PUT trips/{id}`, identity + `source_revision` | as save | **90 s** | **200** `{tripId}` | as save |
| Edit input (l.101) | `GET trips/{id}/input` | `verified` | 15 s | 200 `ApiTripInputSnapshot {input (snake_case), sourceRevision, airports{IATA:ApiAirportSuggestion}, stayEnd?}` | `Server(404)`, `Offline`, … |
| Pull created trip (l.99–100) | `GET trips`, then `GET trips/{id}` | `verified` | 15 s | `ApiTripList`, `ApiTripDetail` | errors swallowed by the service (§6.1) |

---

## 3. Out of scope (owner in brackets)

- Every screen, sheet, dialog, string, haptic and board capture, plus the `CF_WIZARD_AUTO_OPEN` and `CF_WIZARD_BOARD_CAPTURE` seeds [9c–9h]. Opening the wizard automatically when a recovery row exists at shell start [9c]. Rendering `navigationPath` with Navigation 3 [9c].
- The Ready screen's 1 s `readyTick` loop and the foreground re-check trigger [9g]. `readyTick` itself is 9a's; the UI calls it.
- Guest generation (`GuestWizardGeneration.swift`), the guest store and file, `GuestRecoveryGate`, the guest secret, `MemberWizardStore.start(seed:)`, `GuestSessionAcceptance` and the claim [11a, 11b]. The guest *branches* inside the `MemberWizardModel*` files are 9a's (§5.2).
- `FirstTripRemindersOffer.consume` and the reminders sheet [12c]. You port `queue` only (OQ-9b-7).
- Trip-actions entry for Edit and Duplicate [9h]. You port `MemberWizardStore.startTrip`, and the ViewModel calls it.
- Any OkHttp `Cache` (A6), connectivity pre-check (D23), ETag or idempotency header.
- Schema changes. The `drafts` DDL is 4a's. You add **named queries** only (and narrow 4a's `SqlSafetyTest` for them).
- Re-porting anything 9a ships (§4 item 1) [9a].
- Baseline-profile regeneration: no UI path changes.

---

## 4. What 0d, 4a–4e and 9a hand you (check before Task 1)

Check each item with `grep -rn`. Where a fallback is given, apply it in the task named and say so in the commit body. If an item fails and has no fallback, **stop and report**: these are earlier phases' deliverables, and re-porting them here would fork the code.

1. **9a, `:core:wizard`** (names as 9a's brief writes them; 9a's names win over any name elsewhere in this brief):
   - `model/`: `WizardForm` (with `legs`, `stays`, `end`, `override`, `preparation`, `identity`, `itinerary`, `returnBoundary`, `maxFlights == 16`, the route functions, `reviewInput(identity, profile)`, `WizardForm.from(input)` / `from(snapshot)`), `WizardLeg`, `WizardStay`, `MemberWizardStep` (13 cases: `whereTo`, `outbound`, `returnChoice`, `returnRoute`, `returnFlight`, `sleep`, `checking`, `question`, `excluded`, `continuation`, `fineTune`, `ready`, `calculating`), `WizardMode { member, guest }`, `WizardAirportSheet`, `MemberWizardState` (every field of `MemberWizardStore.swift:36–68`, `instance` a random UUID string, `revision = 0`, `reviewedAt: Instant?`, `checkedInput: String?`) with `discardRetiredValues()`, `MemberWizardDraft(form, state, fieldErrors)` with `airports`; the extensions `MemberWizardDraft.inputFingerprint` (a canonical JSON **`String`**, `WizardFingerprint.canonical(ApiTripReviewInput)`), `.hasChanges` and `.loadTrip(snapshot, id, action)` in `model/TripWizardActions.kt`; `enforceGuestMode()` (`internal`, `WizardMode.kt`).
   - `model/WizardSaveFailure.kt`: `WizardSaveFailure(kind, retryAt: Instant?)` with `seconds(at)`, `WizardSaveOutcome`; `TripWizardAction { edit, duplicate }` (in `model/TripWizardActions.kt`); `WizardCredits` (`model/WizardCredits.kt`).
   - `model/MemberWizardState.kt`: `interface WizardDraftStore { persist; discard(instance); didSave(instance, firstTrip) }`.
   - `model/MemberWizardRecord.kt`: `object MemberWizardRecord` with `ID = "member-wizard"`, `EXPIRY_MILLIS = 1_800_000L`, `isExpired(updatedAtMillis, nowMillis)`, `encode(draft): Payload(form, state, fieldErrors)` (strips `guestSecret`; encodes with the shared `ApiJson.response`, so `revision = 0` is omitted — see Stack check) and `decode(payload): MemberWizardDraft?` (null on `SerializationException`/`IllegalArgumentException` or bad shape; applies `discardRetiredValues()` and the `continuation`/`returnFlight` migrations of `MemberWizardStore.swift:143–146`).
   - `model/MemberWizardDrafts.kt`: `MemberWizardDrafts.memberInitial(hasTrip: Boolean, latestOrigin: AirportRef?, savedProfile: JsonObject?, sleepAnswered: Boolean?)` (`firstTrip = !hasTrip`; an origin also sets `firstTrip = false`), `seeded(seed, initial)`, `guest()`.
   - `model/WizardChecking.kt`: `WizardCheckingClock(nowMillis: () -> Long, sleepUntilMillis: suspend (Long) -> Unit)` with `companion val continuous`, `WizardCheckingProgress(ticks, slow, failure, retryAt: Long?)` with `retrySeconds(atMillis)`, and `WizardChecking.run(clock, request, receive)` (OQ-9a-12: **Long milliseconds**, not Swift `Double` seconds).
   - `model/MemberWizardModel.kt` and its extension files (`MemberWizardNavigation.kt`, `MemberWizardReview.kt`, `MemberWizardRetry.kt`, `MemberWizardCompletion.kt`, `WizardMode.kt`, `TripWizardActions.kt`): `MemberWizardModel(draft, store: WizardDraftStore, scope: CoroutineScope, now: () -> Instant, generation: MemberWizardGeneration? = null, checkingClock = WizardCheckingClock.continuous)`, state exposed as `val snapshots: StateFlow<MemberWizardSnapshot>` (`draft`, `closed`, `persistenceFailed`, `checking`, `retrySeconds`, `choicePending`, `reviewing`, `createdTripID`, `saveOutcome`, `guestAuthenticationPresented`), every Swift member (9a Task 7 list), the board `progress` (Task 8), `firstScreen`, `showsBack`. It has **no** `search` property (9a §3). `persist()` is a serial `pending` chain; `flush()` joins it; a `CancellationException` from `store.persist` while the job is active is a dropped (rejected) write, not a failure (5ae30d3c). The flow is `private val mutableSnapshots`; fields have `internal set` that `update { }` it.
   - `generation/TripGenerationTypes.kt`: `TripEditContext(tripID, sourceRevision)`, `TripGenerationReviewResult(input, review, edit = null, sourceSnapshot = null)`, `TripGenerationState` (private constructor, `@ConsistentCopyVisibility` so `copy` is private; build it with `TripGenerationState(instance, input, edit = null, review = null, savedTripId = null)`, whose `invoke` sets `input.identity = review?.identity ?: input.identity`; `identity` is derived from `review`), `TripGenerationSaveResult(state, outcome)` with `Outcome { Saved(tripId), ReReviewNeeded(reason), Failed(failure) }` and `Reason { reviewExpired, notEnoughCredits }`.
   - `generation/MemberWizardGeneration.kt`: `review(instance, input, edit = null)`, `save(state, didCreate)`, `suspend cancelReviews(instance)`, `suspend cancel()`.
   - `generation/TripGenerationAnswers.kt`: `object TripGenerationAnswers { fun applyingAnswers(input, review, answers) }`.
   - Test support in `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/`: `WizardTestSupport.kt` (`wizardAirport`, `generationInput(): ApiTripInput` (not an `ApiTripReviewInput`), `generationReview()`, `WizardTestClock`), `model/CheckingTestClock.kt` (`CheckingTestClock(testScheduler)`, `clock`, `advance(toMillis)`), `model/InMemoryWizardDraftStore.kt` (`persisted`, `failure`, `beforePersist`), `model/FakeWizardGeneration.kt`. Task 0 moves them to `:core:testing` so `:core:data` tests can use them. `model/WizardModelFixture.kt` and `model/WizardPromptFixture.kt` (`internal fun wizardPrompt`) are `internal` and stay in `:core:wizard`.
   - 9a's own Android tests that 9b must keep green unchanged after Task 6: `MemberWizardReviewTest` (incl. `foreignCancellationOnPersistIsNotAPersistenceFailure`, `persistenceNeverGoesBackwardsAndEndsOnTheLatestRevision`, `savePlanForeignCancellationReturnsToReadyWithServerFailure`, `saveIsNeverRetriedAutomatically`), `MemberWizardRecordTests`, `MemberWizardDraftsTest`, `TripGenerationAnswersTest`.
   - 9a's tests already cover `WizardCheckingTests` (4), `WizardAirportChoiceTests` (2), `WizardCompletionTests.creditsCoverEveryReadyVariant` and `sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks`, `WizardReviewFlowTests.returnPastUsesFinalArrivalAndAirportZones` and `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys`, and `TripWizardActionsTests.duplicateClearsEveryDateBoundAnswerAndRetainsRouteAndOtherAnswers`. Do not port them again.
   - No fallback: if any of these is missing or named differently, adapt this brief's code to 9a's name; if it is missing, **stop and report** (re-porting 9a would fork the model).
2. **0d (`:core:model`):**
   - `ApiTripReviewInput(input, identity, profile)` (a data class with an encode-only serializer).
   - `ApiTripSaveInput.from(identity)`, which throws `DataFailure.ReviewExpired`.
   - `ApiTripReview`, `ApiReviewIdentity`, `ApiReviewPrompt` (`kind`, `blocking`, `required`, `target` `profile|trip|Unknown`, `scope{journey, legIndex, stayIndex}`, `fields`, `shape`), `ApiTripInput` (encodable and decodable), `ApiTripInputSnapshot`, `ApiTripSaveResult`, `ApiAirportSuggestion`.
   - `ProfilePreferences.request`, `bodyClock`, `isSupported`, `promptAnswers`, `reviewPrompts` and `camel`.
   - `DataFailure`, an `Exception` with `Offline`, `Server(status)`, `Throttled(retryAfterSeconds: Double, status)`, `ReviewExpired`, `NotEnoughCredits`, `SaveCapacity`, `ProfileInvalid(fields)`, `Validation(fields, status)` and `EmailUnverified(status)`.
   - `FieldMessage(key, arguments)` and `ApiJson.request`.
   - The 16 fixtures in `core/testing/src/main/resources/fixtures/wizard/` (loader `net.chronoflow.core.testing.Fixtures.text("wizard/<name>.json")`). Their `input.json` and `review.json` are byte-identical to iOS `Tests/TripWizardTests/Fixtures/{input,review}.json` (SHA-256 `c6b6852b…287d` and `cc96738a…c000`, verified), so do not copy them again.
3. **4b:**
   - `DataSource` has `searchAirports(query, limit: Int?)`, `reviewTrip(input)`, `reviewTripEdit(input, id, sourceRevision)`, `saveTrip(identity)`, `updateTrip(id, identity, sourceRevision)`, `tripInput(id)`, `fetchTrips()` and `fetchTripDetail(id)`.
   - `ApiTimeouts.Generation == 90.seconds` applies to exactly `POST trips/review`, `POST trips/{id}/review`, `POST trips` and `PUT trips/{id}` (plus guest review and register), and `searchAirports`/`tripInput` use 15 s. **Verify** this by reading `ApiDataSource` and 4b's `deadlinesCancelUnderlyingTask` test.
   - Fallback: a wizard route without 90 s → fix it in a separate `fix(network): use the generation timeout for wizard writes` commit, with a MockWebServer case asserting the requested deadline.
   - `TripWizardErrors.map/journeyPath/camel` exist in `:core:model` (`model/api/TripWizardErrors.kt`). 4b's envelope → `DataFailure` mapper `ApiErrorMapper` is `internal` to `:core:network`.
4. **4c** (package `net.chronoflow.core.datamock`): `MockDataSource(store, seed, …, now, delay, sleep, id, …, zone, io)` with `failNext(MockMethod, DataFailure)`, `setOffline`, wizard review/save, `MockWizardPrompts.make(input, profile)`, `MockAppSeed.make(now, zone)` and `MockMethod.ReviewTrip`/`SaveTrip`.
5. **4a/4d:**
   - `AccountDatabase`: `read { }`, `write(permit) { }`, `write(epoch, permit) { }` and `validate(epoch)`.
   - `SyncEpoch` (`core/model/…/model/session/SyncEpoch.kt`): `child()`, `check()`, `invalidate()`, `whileValid { }`, and `val isValid: Boolean` (present).
   - `SessionMutationPermit` (`whileValid`, `invalidate`; a revoked permit throws `StaleSessionException`, a `CancellationException`) and `StoreFailure` (`core/model/…/model/store/StoreFailure.kt`).
   - `AccountDatabase.write { }`, `write(permit) { }`, `write(epoch, permit) { }` take **non-suspending** `(AccountSql) -> T` bodies; row mappings are `AccountSql` extensions in `core/database/…/database/store/RecordMapping.kt`.
   - The `TripRecord`, `SettingsRecord` and `OutboxRecord` records.
   - `TripRepository` (`trips()` ordered `departure_utc, id`; `upsert`; `plan(tripId, inputHash)`; companion `applyCards(items, at, …)` and `applyDetail(response, tripId, at, …)`), `SettingsRepository` (`settings()`, `upsert`) and `OutboxRepository.enqueue`.
   - `StoreApplication.pending(sql)` (`internal object`, `core/data/…/data/store/StoreApplication.kt`), `StoreTime.millis`/`seconds`, `CanonicalJson.encode` (`:core:model` `store`).
   - The JDBC test support (`core/data/src/test/…/sync/support/SyncStore.kt`, `repository/RepositoryStore.kt`, and `:core:database-testing`'s `JdbcAccountDriverFactory`) and `eventually` (`net.chronoflow.core.testing.sync.eventually`) in `:core:testing`.
   - `SyncEpoch.isValid` exists, so the old "no `isValid`" fallback does not apply.
6. **4e:** `data class SessionGraph(epoch: Long, snapshot: SessionSnapshot, database, accounts, trips, settings, outbox, sync: SessionSync, dataSource, scope)` (`app/…/app/SessionGraph.kt`; it holds **no** permit, no `SyncEpoch`, no dispatchers; built only in `SessionGraphs.kt` inside its `controller.updates` collector), `SessionController.mutationPermit(snapshot): SessionMutationPermit?` (`core/sync/…/sync/session/SessionController.kt:95`; non-null only while the active snapshot equals `snapshot`; iOS `NativeSessionController.swift:173–176`), `SyncEpoch(AccountID.of(snapshot.session.accountId), snapshot.epoch)`, `SessionSync.requestSync()`, `AppDispatchers` (`:app`, `graph.dispatchers.io`), the feature factory pattern (feature-owned `CreationExtras.Key` + `Factory`, e.g. `TripsViewModel.Factory`/`TripsDependenciesKey`, with `:app` setting the extra from `graph.sessionGraphs.current.value`, `AppShell.kt:64–72`; `AppGraphKey` is `:app`-only), and `Route.Wizard(mode: net.chronoflow.core.navigation.WizardMode)` with `Create`, `Edit(tripId)` and `Duplicate(tripId)`.

---

## 5. Architecture [R]

### 5.1 Modules and packages

| Module | New code | New dependencies |
|---|---|---|
| `:core:model` (JVM) | `store/DraftRecord.kt` | — |
| `:core:wizard` (JVM) | `generation/TripGenerationService.kt` (service, `TripGenerationEffects`), `generation/TripGenerationReview.kt` (`performReview`); `model/AirportSearchService.kt`, `AirportSearchCache.kt`, `AirportSearchState.kt`, `AirportSearchSession.kt`, `RecoveryRejected.kt`, `WizardRecoveryWriter.kt`; **one edit** to 9a's `MemberWizardModel.kt` (Task 6) | `implementation(project(":core:model"))`, coroutines core, serialization json (9a already declares them); test: `project(":core:testing")`, coroutines-test, assertk, Turbine |
| `:core:testing` (JVM) | `wizard/` test helpers: 9a's support moved here (Task 0), plus `GenerationSource`, `GenerationGate`, `EffectProbe`, `WizardSearchProbe` | `implementation(project(":core:wizard"))`, `api(libs.kotlinx.coroutines.test)` if not already there (no cycle: `:core:wizard` main never depends on `:core:testing`) |
| `:core:database` | `drafts` named queries in 4a's `Drafts.sq`; `SqlSafetyTest` narrowed (Task 4) | — |
| `:core:data` (Android) | `wizard/DraftRepository.kt`, `wizard/MemberWizardStore.kt`, `wizard/TripGenerationEffectsStore.kt`, `reminders/FirstTripRemindersOffer.kt`; DB-bound and model tests | `implementation(project(":core:wizard"))` |
| `:feature:wizard` | `TripWizardViewModel.kt`, `TripWizardUiState.kt`, `TripWizardDependencies.kt` (with `TripWizardDependenciesKey`) | `implementation(project(":core:wizard"))`, coroutines; test: `:core:testing`, coroutines-test (the `chronoflow.android.feature` plugin already adds `:core:model`, `:core:navigation` and lifecycle-viewmodel-compose); **not** `:core:data` |
| `:app` | `app/…/app/wizard/SessionTripWizardDependencies.kt` (implements `TripWizardDependencies` from `AppGraph` + `SessionGraph`); `SessionGraphs` passes the permit; `implementation(project(":core:wizard"))`, `:feature:wizard` | — |

Why: A3 makes `:core:wizard` JVM-only, and `drafts` lives in the Android `:core:database`. So the store interface (9a's `WizardDraftStore`) is in `:core:wizard`, and its SQL implementation (`MemberWizardStore`) is in `:core:data`. This is the same split 4d used (`SyncDatabase` / `SqlSyncDatabase`).

### 5.2 Which Swift members 9a owns and which you own

- **9a:** everything in §4 item 1, including every `MemberWizardModel` member (guest branches included) and 9a's deviations (board progress, first screens, Long-millisecond checking clock, `MemberWizardSnapshot`, no `search` on the model, the `#if DEBUG` board members not ported).
- **9b:** `TripGenerationService` (actor body, `performReview`, `answerPrompts`), `TripGenerationEffects` + `storing`, `AirportSearchService` + the Android search additions, `DraftRecord`, `DraftRepository`, `MemberWizardStore`, `FirstTripRemindersOffer.queue`, `WizardRecoveryWriter`, the writer hook in 9a's `persist()`/`flush()`, and `TripWizardViewModel`.
- **`checkingTitle`** is 9a's (`TripWizardActions.kt`); whatever 9a returns (an iOS English copy key or an enum), 9b does not change it. 9f maps it to resources.

### 5.3 Concurrency and lifetimes [R]

- **9a's `MemberWizardModel` is confined** (9a Task 7 item 3). Every public call and every job continuation runs on its `scope`, which must be single-threaded: `viewModelScope` (`Dispatchers.Main.immediate`) in the app, `backgroundScope` of `runTest` in tests. This is the Kotlin form of Swift `@MainActor`.
  - All observable fields live in 9a's one `MutableStateFlow<MemberWizardSnapshot>`. The writer's `onFailure` runs on `writerScope` (another thread), so it must set `persistenceFailed` through the flow's atomic `update { }`, never through a read-modify-write of the property.
- **Jobs** (9a's): `checkJob`, `reviewJob`, `saveJob`, `choiceJob` and `retryJob` (Swift `checkTask`, `reviewTask`, `saveTask`, `choiceTask`, `retryTask`), with 9a's `checkID`/`retryID` guards. 9b adds no job to the model.
- **Recovery writes** run in a separate `writerScope`: the session scope in the app. The ViewModel must not cancel a pending write in `onCleared`. A write rejected after × or save is harmless (the instance guard).
- **`TripGenerationService` is per wizard opening** (iOS `AppTabs.swift:501, 522`). Its internal `SupervisorJob` is a child of the session scope, so a session change cancels it. `TripWizardViewModel.onCleared()` and `MemberWizardModel.discard()` call `cancel()`.
- **`AirportSearchService`** (pacing, cache) and **`MemberWizardStore`** are per session (the lazy wizard holder on `SessionGraph`, Task 10), shared by every wizard opening and every sheet.
- **No disk or network on main:**
  - the store and `AccountDatabase` run on 4a's IO writer;
  - the service's network calls run in its own scope on `AppDispatchers.io`;
  - JSON encoding for recovery happens in the writer coroutine (`writerScope` on IO).

### 5.4 Kotlin traps for this phase (each one is a review item)

1. **Cancellation.**
   - Never use `runCatching` or `catch (e: Exception)` before a `catch (e: CancellationException) { throw e }`.
   - The Swift store throws `CancellationError` for a rejected recovery write. In Kotlin, throw **`RecoveryRejected`** (`class RecoveryRejected : Exception()`, in 9b's `core/wizard/…/model/RecoveryRejected.kt`, Task 4; 9a's `WizardDraftStore` file is not touched) instead. A `CancellationException` thrown for business reasons breaks structured concurrency.
2. **Swift `Task.value` vs Kotlin `await()`.**
   - A cancelled Swift waiter still waits for the shared task. A cancelled Kotlin waiter returns at once.
   - So the in-flight map entry must be removed **inside the shared `Deferred`'s own `finally`** (under the mutex, in `withContext(NonCancellable)`), never in the caller.
   - Create the `Deferred` with `CoroutineStart.LAZY`, register it under the lock, then `start()` it outside the lock.
3. **`withTimeout` throws `TimeoutCancellationException`**, which is a `CancellationException`. Do not use it for the service's 30 s review cap. Port the Swift race (§6.1), which throws `DataFailure.Server(null)`. (9a's `WizardChecking.run` already avoids it.)
4. **A request that throws `CancellationException` while Checking itself is still active** is a failure, `Server(null)` (Swift test `requestCancellationWhileCheckingIsActiveShowsServerFailure`). 9a's `WizardChecking.run` handles it; the service must therefore let a cancelled review surface as a `CancellationException` (never wrap it), so 9a's run can classify it.
5. **No `ByteArray`** in keys or data classes. Fingerprints are canonical JSON `String`s (4d `CanonicalJson`).
6. **Recovery JSON must always contain `"instance"` and `"revision"`.**
   - With `encodeDefaults = false`, `revision = 0` is dropped, and a missing `instance` decodes as a **new random UUID**. Every later write would then be rejected.
   - 9a's `MemberWizardRecord.encode` must keep both. Task 4's `recoveryJsonAlwaysCarriesInstanceAndRevision` proves it; if it fails, fix 9a's codec (`encodeDefaults = true` for the state) in a separate `fix(wizard): always encode the recovery instance and revision` commit.
   - **Stack check:** it *will* fail today — `encode` uses the shared `ApiJson.response` (no `encodeDefaults`), so `"revision":0` is omitted (`instance` survives only because its default is a fresh UUID). Fix with a codec-local `Json(ApiJson.response) { encodeDefaults = true }`; never change `ApiJson` itself (it shapes every API request/response). Independently, `updateRecovery` treats a missing stored `"revision"` as 0.
7. **Single debounce.** `AirportSearchService.results` already waits 350 ms. The search session uses `transformLatest`/`mapLatest` only and **never** `Flow.debounce`, which would make 700 ms.
8. **Query length** counts **code points** (`codePointCount`), matching the server's `mb_strlen` (OQ-9b-4). Never `String.length`.
9. **Clocks.**
   - `:core:wizard` reads no system time (the Konsist rule from plan l.1432; `timeHygieneModules` already lists `core/wizard`).
   - Wall time is an injected `now: () -> Instant`.
   - Checking and countdown time is 9a's injected `WizardCheckingClock` (monotonic **Long milliseconds**, OQ-9a-12). The service's own `sleep` stays `suspend (Double) -> Unit` seconds (Swift `APIClient.Sleep`), and the search's `sleep` too.
10. **SQLite on API 29 is 3.22.** No `ON CONFLICT … DO UPDATE` (UPSERT needs 3.24). Use `INSERT OR REPLACE` for "save" and plain `INSERT` for "create".
11. **Settings values are JSON text.** `wizardReminders` is stored as `"\"pending\""` (a JSON string), as Swift `.encoding("pending")` does.
12. **`runTest` auto-advances virtual time when idle.**
    - In ported tests, run the model in `backgroundScope` and use manual clocks only (9a's `CheckingTestClock(testScheduler)`, `WizardTestClock`). Never use `WizardCheckingClock.continuous` in a test.
    - Run `TripGenerationService` on `Dispatchers.Default`, so its real 30 s deadline never fires inside a short test, except in the one deadline test (it injects `sleep`).
    - Call `service.cancel()` in `finally` for tests that leave a `GenerationGate` closed.

---

## 6. Behaviour spec (exact values)

### 6.1 `TripGenerationService` (port `TripGenerationService.swift`, `TripGenerationReview.swift`)

```kotlin
class TripGenerationEffects(                             // 9b, generation/TripGenerationService.kt
    val mergeProfile: suspend (JsonObject) -> Unit,
    val pullCreatedTrip: suspend (String) -> Unit,
    val requestSync: suspend () -> Unit,
) { companion object }                                   // companion: :core:data adds `storing(...)`
// 9a's (generation/TripGenerationTypes.kt, MemberWizardGeneration.kt), used as they are:
//   TripEditContext, TripGenerationState (+ its factory), TripGenerationSaveResult (Outcome, Reason { reviewExpired, notEnoughCredits }),
//   TripGenerationReviewResult, interface MemberWizardGeneration { review; save; suspend cancelReviews; suspend cancel }
class TripGenerationService(source: DataSource, effects: TripGenerationEffects,
    sleep: suspend (Double) -> Unit = { delay((it * 1000).roundToLong()) },
    parentContext: CoroutineContext = Dispatchers.Default) : MemberWizardGeneration {
    suspend fun answerPrompts(instance: String, input: ApiTripReviewInput, review: ApiTripReview,
        answers: Map<String, Map<String, JsonElement>>, edit: TripEditContext? = null): TripGenerationReviewResult
    fun cancelNow()                                      // non-suspending body of cancel(); onCleared() calls it
    override suspend fun cancel() = cancelNow()
}
```

**`review(instance, input, edit)`:**
1. `ensureActive()`; `cancelled` → throw `CancellationException`.
2. `request = input.copy(profile = input.profile?.let(ProfilePreferences::request))`.
3. `key = ReviewKey(instance, fingerprint = WizardFingerprint.canonical(request.copy(identity = null)) /* 9a; a String */, token = request.identity?.token, reviewRevision = request.identity?.reviewRevision, edit)`.
4. Under the mutex, take the existing `Deferred`, or create a LAZY one in the service scope and register it with a fresh id. Its body:
   - `result = race(30.0) { performReview(source, request, edit) }`, where `race` is
     `coroutineScope { val d = launch { sleep(30.0); throw DataFailure.Server(null) }; try { block() } finally { d.cancel() } }`;
   - `ensureActive()`;
   - if `result.input.profile` is non-null and non-empty: `try { effects.mergeProfile(it) } catch (CancellationException) { throw } catch (e: Exception) { effects.requestSync(); throw e }`, then `effects.requestSync()`;
   - in `finally` (NonCancellable, under the mutex): remove the key only if its id is still this one.
5. `start()`; `await()`; `ensureActive()`; return.

**`performReview` (Swift `TripGenerationReview.swift:27–49`):**
1. `refreshSource`; then `sendReview`.
2. On `ReviewExpired`: `refreshSource` again, set `identity = null`, and `sendReview` once more. A second `ReviewExpired` propagates.
3. If `edit != null` and `review.identity.sourceRevision != edit.sourceRevision`: `refreshSource` and `sendReview` again. A repeated mismatch → `throw DataFailure.ReviewExpired`.
4. Finally `input.identity = review.identity`.

`refreshSource`: no edit → unchanged. Otherwise `snapshot = source.tripInput(edit.tripID)`. If `snapshot.sourceRevision == edit.sourceRevision` → unchanged. Else replace `input.input` with `snapshot.input`, set `identity = null`, `profile = null`, `edit = TripEditContext(edit.tripID, snapshot.sourceRevision)`, and `sourceSnapshot = snapshot`.

`sendReview`: `edit != null` → `source.reviewTripEdit(input, edit.tripID, edit.sourceRevision)`; else `source.reviewTrip(input)`.

**`save(initial, didCreate)`:**
1. `tripID = state.savedTripId`. If it is null:
   - `review == null || !review.canSave` → `throw ReviewExpired`;
   - `identity = ApiTripSaveInput.from(review.identity)`;
   - `key = SaveKey(instance, identity, edit)`;
   - a cached `savedTrips[key]` wins; else join `saves[key]`; else start one LAZY `Deferred` that calls `updateTrip(edit.tripID, identity, edit.sourceRevision)` when editing, else `saveTrip(identity)`;
   - on success, `savedTrips[key] = id` and remove `saves[key]`; on any error, remove `saves[key]` and rethrow. `savedTrips[key]` is written **before** step 2 calls `didCreate` (Swift l.190–193).
2. `state = TripGenerationState(state.instance, state.input, state.edit, state.review, savedTripId = tripID)` (9a's factory; `copy` is private); `didCreate(tripID)`.
3. Then `pullCreatedTrip(tripID)` with errors swallowed (except cancellation); then `requestSync()`; return `Saved(tripID)`.
4. Error handling:
   - `CancellationException` → rethrow **when the caller is cancelled** (`!currentCoroutineContext().isActive`) or the service is `cancelled`. A **foreign** `CancellationException` thrown by `didCreate` while the caller is still active (e.g. `StaleSessionException` from a revoked permit, or a rejected write) → `Failed(Server(null))` with the state, **including `savedTripId`** (R-A366 below; Swift rethrows here — Android deviation, OQ-9b-15). A foreign cancellation before a trip id exists may still propagate; 9a's model maps it to `Server(null)` (OQ-A366).
   - `DataFailure`, with `state.savedTripId != null` → `Failed(f)` and the state kept.
   - `ReviewExpired`/`NotEnoughCredits` → `TripGenerationState(instance, input.copy(identity = null), edit, review = null, savedTripId = null)` and `ReReviewNeeded(reason)`.
   - Other `DataFailure` → `Failed(f)` with the state kept.
   - Any other exception (for example a `StoreFailure` or `RecoveryRejected` from `didCreate`) → `Failed(Server(null))` with the state, including `savedTripId`.
5. **Nothing in `save` retries.**

**R-A366 (required; plan OQ-A366 risk).** 9a's model (`MemberWizardCompletion.kt` `savePlan`) copies `result.state.savedTripId` into its state before handling `Failed`, persists it in `saveFailed`, and builds the next `TripGenerationState` from `state.savedTripId`. A failure that hits `didCreate` after the server created the trip must therefore never lose the created id, so a **manual** retry from Ready reuses it and never POSTs `trips` (or PUTs) a second time:
- (a) every non-caller failure after the POST/PUT returned — any exception from `didCreate`, foreign cancellation included — returns a `TripGenerationSaveResult` whose `state.savedTripId == tripID` (never a throw);
- (b) `savedTrips[key]` is set before `didCreate`, so even a path that loses the id in the model (a cancellation that escapes) hits the cache on the same service instance and makes no request;
- (c) the server's receipt (`POST trips` with the same identity returns the same `tripId`, §2.4) is the last line of defence after process death; 9b relies on it and says so, it does not add a client-side idempotency key (§3).
- Proven by Task 1's `didCreateFailureRetryReusesCreatedTrip` and Task 8's `foreignDidCreateCancellationRetryDoesNotCreateASecondTrip` (§9 item 9).

**`cancelReviews(instance)`:** cancel and remove every review whose key has that instance.

**`cancelNow()` / `cancel()`:** non-suspending body. Set `cancelled = true` (`@Volatile`), `job.cancel()` (which cancels every review **and** save `Deferred`, as Swift cancels both maps), and clear `savedTrips` (Swift `TripGenerationService.swift:233–238`). Every later call throws `CancellationException`.

### 6.2 `answerPrompts` (port `TripGenerationAnswers.swift:5–11`)

- `answerPrompts(instance, input, review, answers, edit)` = `review(instance, TripGenerationAnswers.applyingAnswers(input, review, answers), edit)`.
- `applyingAnswers` is **9a's** `object TripGenerationAnswers` (9a Task 6 item 5: unsupported prompt or foreign field → `Validation(mapOf("review" to FieldMessage("Choose one of the available options.")), 422)`; a missing array index → `ReviewExpired`; explicit nulls kept). Do not re-port it. If `answeringPromptsPreservesExplicitNulls` or `answersFollowReturnLegStayAndTopLevelScopes` (Task 1) fails inside it, fix 9a's object in a `fix(wizard): …` commit.

### 6.3 `WizardChecking` (9a's; verify only)

9a Task 6 ports `WizardChecking.swift` with Long-millisecond clocks. Its timing, which Tasks 7 and 9 rely on [F `WizardChecking.swift:40–75`]:

| Event | When |
|---|---|
| Tick 1 | 400 ms |
| Tick 2 | 800 ms |
| Tick 3 | `max(response, 1200 ms)` |
| Complete | tick 3 + 300 ms |
| Slow | 10 000 ms |
| Failure | 30 000 ms, as `Server(null)` |

When the response child finishes first, the timers are cancelled; when the timers finish first, the response is cancelled. A cancelled run emits nothing. 9b writes no checking code and no `WizardCheckingTests`.

### 6.4 Airport search

**`AirportSearchService`** (port `AirportSearchService.swift`, plus the cache):
- Constructors:
  - `(search: suspend (String) -> List<ApiAirportSuggestion>, now: () -> Instant, sleep: suspend (Double) -> Unit, cache: AirportSearchCache? = null)`, which the tests use;
  - `(source: DataSource, now, sleep, cache = AirportSearchCache())`, where `search = { source.searchAirports(it, 8) }`.
- `suspend fun results(raw: String): List<ApiAirportSuggestion>`:
  1. `query = raw.trim()`. Its code-point count must be in `2..60`, else return `emptyList()` with no wait.
  2. `sleep(0.35)`; `ensureActive()`.
  3. `cache?.get(query, now())` → return the hit (no pacing, no request).
  4. Pacing loop:
     ```kotlin
     while (true) {
         val wait = mutex.withLock {
             val n = now()
             if (n >= nextRequest) { nextRequest = n + 1.02 s; null } else nextRequest − n
         }
         if (wait == null) break
         sleep(wait); ensureActive()
     }
     ```
     `nextRequest` starts at `Instant.MIN`. The mutex is never held across `sleep`.
  5. `ensureActive()`; `search(query)`; on success, `cache?.put(query, results, now())`.
  6. `DataFailure.Throttled(s)` → under the mutex `nextRequest = max(nextRequest, now() + s)`; rethrow. [R] Skip the update unless `s.isFinite() && s > 0` (Swift `AirportSearchService.swift:38–40` has no guard; a non-finite `Double` would make `Instant.plus` throw).
  7. Every other failure rethrows and is not cached.
- **Cache [R]** (`AirportSearchCache(capacity = 64, ttl = 3600 s)`):
  - key = `query.lowercase(Locale.ROOT)` (the server lower-cases the term, `Airport::searchKey`);
  - a `LinkedHashMap(accessOrder = true)` under `synchronized`;
  - an entry is fresh while `now − storedAt < 3600 s`. That is the server's `max-age=3600`;
  - empty results are cached, failures never are;
  - the oldest entry is evicted beyond 64.

**`AirportSearchState`** (sealed, `model/AirportSearchState.kt`) [R]:

| State | Meaning | Board state (`TripSearchStates`) |
|---|---|---|
| `Hint` | trimmed code points < 2 | "Fewer than 2 characters" |
| `Searching(query, stale: List<ApiAirportSuggestion>)` | a request is pending; `stale` = the last shown results (empty → the skeleton) | "Searching" |
| `Results(query, airports)` | non-empty result | (result list) |
| `Empty(query)` | empty result, or > 60 code points (no request; iOS returns `[]`) | "No results" |
| `Failed(query)` | any failure but throttled | "Error with retry" |
| `Throttled(query, retryAt: Instant)` | 429; `retryAt = now + retryAfterSeconds` | "Error with retry", with Try again disabled until `retryAt` (`06 §9.7`) |

**`AirportSearchSession(service, scope, now)`**: `fun setQuery(raw: String)`, `fun retry()`, `val state: StateFlow<AirportSearchState>` (initial `Hint`).
- `combine(query, retryCount)`, `distinctUntilChanged`, then `transformLatest`:
  - invalid query → `Hint`/`Empty`;
  - else emit `Searching(q, lastResults)`, then `service.results(raw)` → `Results`/`Empty`;
  - `Throttled` → `Throttled`; any other `DataFailure` or `Exception` → `Failed`; `CancellationException` rethrows.
- `stateIn(scope, Eagerly, Hint)`.
- `retry()` is ignored while `state` is `Throttled` and `now() < retryAt`.
- Filtering by `airportIsAllowed` is not done here; that is 9c's job.

### 6.5 Recovery

**`DraftRecord`** (`:core:model` `store`, port `DraftRecord.swift`) has the fields:

| Field | Type | Column |
|---|---|---|
| `id` | `String` | `id` |
| `tripId` | `String?` | `trip_id` |
| `form` | `String` | `form` |
| `baseForm` | `String?` | `base_form` |
| `dirty` | `Boolean` | `dirty` |
| `review` | `String?` | `review` |
| `token` | `String?` | `token` |
| `inputRevision` | `Int` | `input_revision` |
| `reviewRevision` | `Int?` | `review_revision` |
| `wizardSourceRevision` | `String?` | `wizard_source_revision` |
| `wizardReview` | `String?` | `wizard_review` |
| `furthestStep` | `String?` | `furthest_step` |
| `wizardState` | `String?` | `wizard_state` |
| `sourceRevision` | `String?` | `source_revision` |
| `saveFailure` | `String?` | `save_failure` |
| `fieldErrors` | `String?` | `field_errors` |
| `updatedAt` | `String` | `updated_at` |

JSON columns hold raw JSON text (4a OQ-4a-12: `String` columns). `DraftRecord(id, tripId = null, form, baseForm = null, dirty = true, review = null, token = null, inputRevision = 0, reviewRevision = null, wizardSourceRevision = null, wizardReview = null, furthestStep = null, wizardState = null, sourceRevision = null, saveFailure = null, fieldErrors = null, updatedAt)` — the Swift init defaults (`DraftRecord.swift:22–30`). The id constant is 9a's `MemberWizardRecord.ID` (`"member-wizard"`); do not declare a second one.

**Named queries:** add them to 4a's `core/database/src/main/sqldelight/net/chronoflow/core/database/Drafts.sq` (DDL only until now; 4a Task 3). No DDL change.
- **4a hand-off (binding):** 4a's `SqlSafetyTest.noSqlNewerThanApi29` (`core/database/src/test/kotlin/net/chronoflow/core/database/SqlSafetyTest.kt`) fails on **two** REPLACE patterns, `OR\s+REPLACE` and `\bREPLACE\s+INTO`, in every `.sq` file (an `INSERT OR REPLACE INTO` matches both), and carries a `// 9b:` comment at them; it also asserts `files.size == 9` (no new `.sq` file in this phase). In the same commit as the queries, narrow **only those two** REPLACE patterns to skip `Drafts.sq` (4a l.287, §11 "9b"), keep every other pattern for `Drafts.sq`, and add a `scannerRejectsPlantedViolations`-style case proving `OR REPLACE` / `REPLACE INTO` in any other file still fails. `drafts` has no FK children, so `REPLACE` deletes nothing else.
- `draftById`;
- `listedDrafts`: `WHERE id != 'member-wizard' ORDER BY updated_at, id`;
- `insertDraft`: plain `INSERT`;
- `saveDraft`: `INSERT OR REPLACE`;
- `deleteDraft`.

4a typed `dirty` as `INTEGER` without an adapter (4a §11 "9b"): map `Long ↔ Boolean` in the `AccountSql` draft mappings you add to `core/database/…/database/store/RecordMapping.kt` (the 4a home of every record↔row mapping, with its `Boolean.sql()` helper), which `DraftRepository` calls. Pass every non-null JSON column through 4a's `JSONDocument.requireValid` before any SQL runs (4a Task 6 rule), so invalid JSON never enters a row.

**`DraftRepository(database)`** (port `DraftRepository.swift`):
- `drafts()` and `values(): Flow` read the **listed** rows only.
- `upsert(record)` and `remove(id, permit)`.
- `recovery(now, permit)`. In **one** `write(permit)` transaction:
  - fetch `member-wizard`;
  - delete it and return `null` unless `updatedAt` parses (`Instant.parse`, with or without a fraction), `!MemberWizardRecord.isExpired(updated.toEpochMilli(), now.toEpochMilli())` (9a: expired at `>= 1_800_000` ms, so 1799.999 s recovers and 1800 s deletes), and `wizardState != null`.
- `createRecovery(record, permit)`: the id must be `member-wizard` and no row may exist, else `RecoveryRejected`. Uses `insertDraft`.
- `replaceRecovery(record, permit)`: the id must be `member-wizard`, else `RecoveryRejected`. Uses `saveDraft`.
- `updateRecovery(record, instance, revision, permit)`:
  - `record.id` must be `member-wizard`, the row must exist, and its `wizard_state` JSON's `"instance"` must equal `instance`, else `RecoveryRejected`;
  - if the stored `"revision"` (compared numerically) is `> revision`, return without writing;
  - else `saveDraft`.
- `deleteRecovery(instance, permit)`: delete only if the stored instance matches; otherwise do nothing and do not fail.

**`WizardDraftStore`** is 9a's interface (`persist`, `discard(instance)`, `didSave(instance, firstTrip)`). `class RecoveryRejected : Exception()` is 9b's, in `core/wizard/…/model/RecoveryRejected.kt`.

**`MemberWizardStore(database, permit: SessionMutationPermit? = null, now: () -> Instant) : WizardDraftStore`** (`:core:data`, port `MemberWizardStore.swift:114–244`):

`recover()` (Swift l.134–156):
1. Call `drafts.recovery(now(), permit)`; `null` → `null`.
2. `MemberWizardRecord.decode(Payload(form = record.form, state = record.wizardState!!, fieldErrors = record.fieldErrors ?: "{}"))`. 9a's decode already applies `discardRetiredValues()`, the shape checks (legs 1…16, `stays.size == legs.size`, `returnBoundary in 1..legs.size`) and the `continuation`/`returnFlight` migrations; do not repeat them.
3. A `null` decode, or any exception except `CancellationException`, → `drafts.remove(record.id, permit)` and return `null`.

`start()`:
- Return `recover()` if there is one.
- Else build `initialDraft()`, call `createRecovery(record(draft))`, and return it.

`startTrip(id, action, source)`:
- `snapshot = source.tripInput(id)` and `ensureActive()`.
- `draft = initialDraft().loadTrip(snapshot, id, action)` (9a's data classes are immutable and return a copy, OQ-9a-11; a throw from `loadTrip` propagates, as Swift `try`).
- `replaceRecovery`.

`initialDraft()` (Swift l.190–216; the pure half is 9a's `MemberWizardDrafts.memberInitial`):
- **Latest trip.** `latest` = the last trip in `trips()` (`TripRepository.trips()` / `AccountSql.fetchTrips()`, ordered `departure_utc, id`) with `deletedAt == null && !deletedLocally`.
- **Origin.** `origin` = `latest?.card` decoded as `TripCardView`, then `.from` (an `AirportRef`); a decode failure → `null`, never an exception.
- **Settings.** `savedProfile` = the settings `bodyClock` value as a `JsonObject` (or `null`); `sleepAnswered` = the settings `sleepAnswered` value decoded as `Boolean` (a decode failure → `null`).
- `draft = MemberWizardDrafts.memberInitial(hasTrip = latest != null, origin, savedProfile, sleepAnswered)` (prefill, `fromLastTrip`, `sleepNeeded`, `firstTrip` and the `habitual_bed`/`habitual_wake`/`chronotype` copy are 9a's).
- **First trip, exactly as Swift.** 9a's `hasTrip` parameter gives `firstTrip = latest == null`, also when `latest` exists but its card fails to decode (Swift: no prefill, `firstTrip` stays false). No override is needed (Stack check; OQ-9b-13 settled).

`persist(draft)` = `updateRecovery(record(draft), draft.state.instance, draft.state.revision)`. `discard(instance)` = `deleteRecovery(instance)`. `didSave(instance, firstTrip)` = `FirstTripRemindersOffer.queue(firstTrip)`, then `discard(instance)`.

`record(draft)`: apply `discardRetiredValues()` to a copy of the state, then build the row:

| Column | Value |
|---|---|
| `id` | `member-wizard` |
| `trip_id` | `edit?.tripID` |
| `form` | JSON of `form` |
| `base_form` | JSON of `originalForm` |
| `dirty` | `draft.hasChanges` |
| `token` | `form.identity?.token` |
| `input_revision` | `form.identity?.inputRevision ?: 0` |
| `review_revision` | `form.identity?.reviewRevision` |
| `wizard_source_revision` | `edit?.sourceRevision` |
| `furthest_step` | the step's raw name |
| `wizard_state` | JSON of the state |
| `field_errors` | JSON of `fieldErrors` |
| `updated_at` | `StoreTime.millis(now())` |

Every other column is `null`. `form`, `wizard_state` and `field_errors` come from 9a's `MemberWizardRecord.encode(draft)` (`Payload.form`, `.state`, `.fieldErrors`; it applies `discardRetiredValues()` and strips `guestSecret`). `base_form` is `originalForm` encoded with the same `Json` instance 9a's codec uses (no second `Json` configuration).

**`FirstTripRemindersOffer(database, permit).queue(firstTrip)`**: return unless `firstTrip`. Otherwise, in `write(permit)`, insert settings `wizardReminders = "\"pending\""` if that section is absent.

**`WizardRecoveryWriter(store, writerScope, onFailure: () -> Unit)`** (the conflated writer):
- `fun enqueue(draft)`: `seq++`; `latest.set(seq to draft)` (`AtomicReference`); `signal.trySend(Unit)`, where `signal = Channel<Unit>(CONFLATED)`.
- One consumer coroutine in `writerScope` loops over `signal`:
  - `val (s, draft) = latest.get()`, call `store.persist(draft)`;
  - `catch (RecoveryRejected)` → drop it silently (Swift `catch is CancellationError { return }`, `MemberWizardModel.swift:266`);
  - `catch (CancellationException)` → rethrow if the consumer itself is cancelled (`!currentCoroutineContext().isActive`); otherwise (a foreign cancellation such as `StaleSessionException` from a revoked permit) drop it silently like a rejection, **without** `onFailure` — 9a's rule (5ae30d3c, `foreignCancellationOnPersistIsNotAPersistenceFailure`);
  - `catch (Exception)` → `onFailure()`;
  - then `written.value = max(written.value, s)` (`written` is a `MutableStateFlow<Long>`; `s` is the sequence number **read with** the draft, not the current `seq`).
- `suspend fun flush()` returns when `written.value >= seqAtCall`, or when the consumer job has completed (`select { written.first { … }; job.onJoin }`), so a cancelled session never hangs × or save.
- Only older revisions are dropped, and the DB revision guard rejects older revisions anyway. The final row is always the latest revision (OQ-9b-2).

### 6.6 `TripGenerationEffects.storing` (`:core:data`, port `TripGenerationEffects+Store.swift`)

`fun TripGenerationEffects.Companion.storing(database, source, epoch, permit = null, requestSync: suspend () -> Unit, now: () -> Instant)`:

- **`mergeProfile(patch)`:**
  - `e = epoch.child()`; `database.validate(e)`.
  - One `write(e, permit)` transaction:
    - `body` = settings `bodyClock` as a JSON object, or `{}`;
    - `merge(p)`: for each `(k, v)` in `ProfilePreferences.request(p)`, set `body[camel(k)] = v`;
    - `merge(patch)`, then `merge(pending)` for every pending outbox row (`StoreApplication.pending`) that is `Settings(BodyClock, pending)`, in FIFO order;
    - save `bodyClock = ProfilePreferences.bodyClock(body)` and delete `readiness`;
    - if the patch keys intersect `{habitual_bed, habitual_wake, free_day_status, free_day_bed, free_day_wake, chronotype}`, save `sleepAnswered = true`.
- **`pullCreatedTrip(id)`:**
  - `e = epoch.child()`; `validate(e)`;
  - `trips = source.fetchTrips()`; `e.check()`; `at = StoreTime.seconds(now())`;
  - `write(e, permit) { applyCards(trips, at) }`;
  - `detail = source.fetchTripDetail(id)`; `e.check()`;
  - `write(e, permit) { applyDetail(detail, id, at) }`.
- **`requestSync()`:** if `epoch.isValid` (non-throwing, §4 item 5), call `requestSync()`.

### 6.7 `MemberWizardModel` (9a's class; 9b's one change, then a verification checklist)

**9b's change (Task 6).** 9a's `persist()` is a serial `pending` chain (9a Task 7 item 5). Replace that chain, and nothing else, with the conflated writer:

```kotlin
class MemberWizardModel(/* 9a's parameters unchanged */, writerScope: CoroutineScope = scope) {   // new last parameter
    private val writer = WizardRecoveryWriter(store, writerScope,
        onFailure = { persistenceFailed = true })   // 9a's internal setter already does mutableSnapshots.update { } (atomic); runs on writerScope
    internal fun persist() { state = state.copy(revision = state.revision + 1); writer.enqueue(draft) }
    suspend fun flush() = writer.flush()
}
```

- 9a's real names: `private val mutableSnapshots: MutableStateFlow<MemberWizardSnapshot>`, `val snapshots`, `var draft` / `internal var state` with `internal set`. Remove `internal var pending: Job?` with the chain.
- The default `writerScope = scope` keeps every 9a test working unchanged. Run 9a's `:core:wizard` suite after the change: it must stay green with no test edit (notably `MemberWizardReviewTest.persistenceNeverGoesBackwardsAndEndsOnTheLatestRevision` and `foreignCancellationOnPersistIsNotAPersistenceFailure`; the authorized replacement asserts increasing persisted revisions and the latest final row rather than every intermediate revision).
- 9a's `discard()` (`MemberWizardModel.kt:300–320`) keeps the Swift order (`MemberWizardModel.swift:236–250`): `closed = true`, stop the countdown, cancel the inline review, `cancelCheck()`, cancel `choiceJob`, then in `NonCancellable`: `cancelAndJoin` the captured jobs, `generation?.cancel()`, **`flush()`**, `store.discard(instance)`; a throw sets `closed = false`, restarts the countdown and rethrows. 9b does not change it.

**Verification checklist.** The rest of this section is what Tasks 7–9 prove against 9a's port. Where a ported test fails because 9a's port differs from the Swift line cited, fix 9a's file in a `fix(wizard): …` commit that names the Swift line. Clock values below are 9a's Long milliseconds.

**Guards, exactly as in Swift:**
- `go`: `!closed && !persistenceFailed && step != calculating`.
- `discard`: `!closed && step != calculating`.
- `beginChecking` starts one check at a time:
  - it refuses while `closed`, `persistenceFailed`, `choicePending` or `retrySeconds != 0`;
  - and while a check is running without failure (`step == checking && checkJob != null && failure == null`).
- `chooseReturn` and `answerQuestion` reject while `choicePending`.
- `savePlan` (member): `!closed && saveJob == null && (recovering || (step == ready && canSave))`.
- `canSave = !reviewing && !persistenceFailed && !closed && retrySeconds == 0 && (!isEditing || hasChanges) && (savedTripId != null || review?.canSave == true)`.

**Timings:**
- **Choice delay.** `advanceChoice` sets `choicePending = true` and waits `sleepUntilMillis(checkingClock.nowMillis() + 250)` in `choiceJob` (`MemberWizardReview.swift:329–341`); then, if `!closed`, it sets `choicePending = false` and runs the advance. `go` cancels `choiceJob`.
- **Ready expiry.** `needsReadyReview` (member) is true when `savedTripId == null && (identity == null || checkedInput != inputFingerprint || reviewedAt == null || now − reviewedAt >= 1800 s)`. Compute it in milliseconds: `>= 1_800_000`.
- **Retry countdown** (`MemberWizardRetry`):
  - Checking with a failure → `checking.retrySeconds(checkingClock.nowMillis())`;
  - Ready → `saveFailure?.seconds(now())`;
  - the loop sleeps `min(1000 ms, remaining)` on `checkingClock` and stops at 0, which clears `retryAt`.
- **Throttle → `retryAt`**: only when `seconds.isFinite() && seconds > 0`. Checking uses `checkingClock.nowMillis() + (s * 1000).roundToLong()`; a save uses `now() + s` (`Instant`, millisecond precision).

**Check instance and cancellation:**
- Each check uses instance `"${state.instance}/${checkID}"`.
- `cancelCheck()` bumps `checkID`, cancels `checkJob`, and calls `generation.cancelReviews(instance)` in a **fire-and-forget** `scope.launch`. Swift test `resumedQuestionChecksOfflineWithoutWaitingForPreviousCancellation` requires that a new check never waits for it.

**Save** (`MemberWizardCompletion.swift:219–300`). `savePlan` (when not recovering and `needsReadyReview`, it reviews in place instead and returns) clears `saveFailure`, sets `step = calculating` and persists. `saveJob` then runs:
1. `flush()`; if `persistenceFailed` → `step = ready` and stop.
2. `generation.save(state) { id -> store.persist(snapshot.copy(state = snapshot.state.copy(savedTripId = id, revision = snapshot.state.revision + 1))) }`. This is awaited, so the `savedTripId` row is durable **before** the pull.
3. Outcomes:
   - `Saved` → `try { store.didSave(instance, firstTrip) } catch (CancellationException) { throw } catch (Exception) { }`, then `createdTripID = id` and `closed = true`;
   - `ReReviewNeeded` → clear `form.identity`, `savedTripId`, `reviewedAt` and `checkedInput`, set `step = ready`, `persist()`, `readyTick(force = true)`;
   - `Failed` → `saveFailed(f)`: `Offline` → `offline`; `Throttled` → `throttle(retryAt)`; anything else → `server`. Then `step = ready`, `restartRetryCountdown()`, `persist()`.
4. **There is no other caller of `generation.save`.**

**Resume and navigation:**
- `resumeCompletion()` runs once: Calculating → `savePlan(recovering = true)`; Ready → `readyTick(force = true)`.
- `resumeChecking()` restarts a Checking step that has no job and no failure.
- `navigationPath` and `popNavigation(path, presented)` are verbatim, including the stale-pop guard (`presented == navigationPath`, Calculating rejected).

### 6.8 `TripWizardViewModel` scaffold (`:feature:wizard`)

```kotlin
interface TripWizardDependencies {
    val store: WizardDraftStore                      // MemberWizardStore of the session
    val airportSearch: AirportSearchService          // session-shared pacing + cache
    val source: DataSource
    val now: () -> Instant
    val writerScope: CoroutineScope                  // SessionGraph.scope + AppDispatchers.io
    suspend fun start(): MemberWizardDraft           // MemberWizardStore.start()
    suspend fun startTrip(id: String, action: TripWizardAction): MemberWizardDraft   // MemberWizardStore.startTrip(id, action, source)
    fun newGeneration(): TripGenerationService       // Task 10 code; one per wizard opening
    val checkingClock: WizardCheckingClock get() = WizardCheckingClock.continuous   // [R] test seam; fakes pass CheckingTestClock
}
sealed interface TripWizardUiState {
    data object Loading : TripWizardUiState
    data class Active(val wizard: MemberWizardSnapshot /* 9a */, val progress: Double, val path: List<MemberWizardDestination> /* 9a */) : TripWizardUiState
    data class LoadFailed(val failure: DataFailure) : TripWizardUiState
    data class Closed(val createdTripId: String?) : TripWizardUiState
}
class TripWizardViewModel(entry: WizardEntry /* import net.chronoflow.core.navigation.WizardMode as WizardEntry (OQ-A363) */, deps: TripWizardDependencies?) : ViewModel() {
    val state: StateFlow<TripWizardUiState>
    val search: AirportSearchSession                 // created on viewModelScope from deps.airportSearch
    val wizard: MemberWizardModel?                   // null until Active; 9c+ add intent methods that forward to it
    suspend fun discard()                            // model.discard(); success → Closed(null); a thrown failure keeps Active and is rethrown (9c shows it)
    override fun onCleared()                         // generation.cancelNow(); never discards; pending writes finish in writerScope
}
```

- **Start.** In `init`, `viewModelScope.launch`:
  - `Create` → `deps.start()`; `Edit(id)` → `startTrip(id, edit)`; `Duplicate(id)` → `startTrip(id, duplicate)`.
  - `DataFailure` → `LoadFailed`; `CancellationException` rethrows.
  - Otherwise build 9a's model (`MemberWizardModel(draft, deps.store, scope = viewModelScope, now = deps.now, generation = deps.newGeneration(), checkingClock = deps.checkingClock, writerScope = deps.writerScope)`), then call `resumeChecking()` and `resumeCompletion()` once (iOS `MemberWizardView.swift:94–95`).
  - The generation service is created only after the draft loaded, so a failed Edit/Duplicate load leaves nothing to cancel (iOS `AppTabs.swift:507–512` cancels it on that path).
- **State mapping.** `state` maps 9a's `model.snapshots` → `Active(snapshot, model.progress, model.navigationPath)`, and `closed && createdTripID != null` → `Closed(createdTripID)`.
- **No secret** or draft goes into `SavedStateHandle`. The entry comes from the route.
- **Factory.** `AppGraphKey` is `:app`-only, so follow the feature pattern of `TripsViewModel.Factory`/`TripsDependenciesKey` (Stack check): `:feature:wizard` declares `object TripWizardDependenciesKey : CreationExtras.Key<TripWizardDependencies>` and `fun TripWizardViewModel.Companion.factory(entry: WizardEntry) = viewModelFactory { initializer { TripWizardViewModel(entry, this[TripWizardDependenciesKey]) } }`. `:app` builds `SessionTripWizardDependencies(graph, session)` from `graph.sessionGraphs.current.value` and sets the extra (as `AppShell.kt:64–72`), once 9c registers the `Route.Wizard` entry; 9b ships the class and the per-session lazies (§5.3, Task 10).
  - A missing extra (no session, or a null permit) yields `LoadFailed(DataFailure.Unauthorized(401))`, so the dependencies type is nullable at the key read.

---

## 7. Tasks (one commit each, in order)

Run `make targeted ONLY=…` for each task's tests, and `make quick` once before the last commit. Each test method keeps its Swift name.
- **Parameterized Swift tests** (`@Test(arguments:)`) follow 9a's pattern (AGENTS "Tests" 5: each parameter is its own case): `@RunWith(Enclosed::class)` on the class, a nested `@RunWith(Parameterized::class)` class with `@Parameterized.Parameters(name = …)`, and the Swift-named method inside it (see `core/wizard/src/test/…/model/WizardCompletionTests.kt`). No loops over arguments.
- **Swift `eventually { }`** becomes `assertThat(eventually { … }).isTrue()` (4d's helper).
- **Every coroutine test** uses `runTest(timeout = 60.seconds)`.

### Task 0. `refactor(testing): share the wizard test support`

- Move, unchanged except the package (`net.chronoflow.core.testing.wizard`), 9a's test support from `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/` to `core/testing/src/main/kotlin/net/chronoflow/core/testing/wizard/`: `WizardTestSupport.kt` (`wizardAirport`, `generationInput`, `generationReview`, `WizardTestClock`), `model/CheckingTestClock.kt`, `model/InMemoryWizardDraftStore.kt`, `model/FakeWizardGeneration.kt`. Update 9a's test imports. `:core:data` tests (Tasks 4–9) cannot see another module's test sources, which is why they move.
- `:core:testing` (`core/testing/build.gradle.kts`, today: coroutines-core, serialization-json, `api(project(":core:plan"))`): add `implementation(project(":core:wizard"))` and `api(libs.kotlinx.coroutines.test)` (absent; 9a's `CheckingTestClock` wraps `TestCoroutineScheduler`). `:core:data-mock` already depends on `:core:testing`; that is fine (`:core:wizard` main never depends on either). `:core:data` already has `testImplementation(project(":core:testing"))`. Keep `WizardModelFixture.kt` and `WizardPromptFixture.kt` (`internal`) in `:core:wizard`.
- Tests: none new. `./gradlew :core:wizard:test` (9a's whole suite) stays green with no assertion changed.
- Ticks nothing.

### Task 1. `feat(wizard): port the trip generation service`

- Files:
  - `core/wizard/…/generation/TripGenerationService.kt`, holding `TripGenerationEffects` (with its `companion object`) and `TripGenerationService` (§6.1, §6.2). It implements 9a's `MemberWizardGeneration` and uses 9a's `TripEditContext`, `TripGenerationState`, `TripGenerationSaveResult`, `TripGenerationReviewResult` and `TripGenerationAnswers`;
  - `TripGenerationReview.kt` (`performReview`, `refreshSource`, `sendReview`);
  - `core/wizard/…/model/RecoveryRejected.kt` (`class RecoveryRejected : Exception()`), needed by this task’s duplicate-trip cases and reused by Task 4;
  - the module build deps of §5.1 for `:core:wizard`, if 9a lacks one.
- Test helpers in `core/testing/…/wizard/`:
  - `GenerationSource.kt`, a port of `Tests/TripWizardTests/GenerationSource.swift:1–125`, implementing 4b's `DataSource`. `mock` is a `DataSource?` (tests pass 4c's `MockDataSource`; `:core:testing` does not depend on `:core:data-mock`). Every `DataSource` method it does not implement throws `DataFailure.Server(null)` (Swift l.109–125). It keeps the per-method counters (`count("review")`, `"save"`, `"update"`, `"trips"`, `"detail"`, `"input"`), `reviewInputs`, `editRequests`, `operations`, plus `queueReviews`, `queueSnapshots`, `loseSaveResponseOnce`, `configure(...)` and `waitForReviews`/`waitForSaves`, built on `CompletableDeferred`.
  - `GenerationGate.kt` and `EffectProbe.kt` (`TripGenerationTests.swift:316–347`).
  - `expiredIdentity()` (`TripGenerationRecoveryTests.swift:200–202`: `ApiReviewIdentity(token = "expired", inputRevision = 1, reviewRevision = 1)`), in `GenerationSource.kt`.
  - `generationInput()`, `generationReview()` and `wizardAirport(iata, city, zone)` are 9a's (moved in Task 0; `wizardAirport` has `country = ""`, Swift `WizardPortTests.swift:350–352`). Do not write a second copy.
- Tests (`core/wizard/src/test/…/generation/`):
  - **`TripGenerationTests`**, 9 methods: all 10 of `TripGenerationTests.swift` except `profileReviewAnswersAndSavedPlanReachSQLite`, which goes to Task 5. `saveFailuresKeepOrClearIdentityAndNeverRetry` is a Swift `@Test(arguments:)` (Parameterized, §7 intro). `reviewDeadlineCancelsRequestAndDoesNotRetry` injects `sleep = { s -> assertThat(s).isEqualTo(30.0); source.waitForReviews(1) }` (Swift l.46–62).
  - **`TripGenerationRecoveryTests`**, 9 methods: all. Where Swift throws `StoreFailure.unavailable` from `didCreate`, throw 4d's `StoreFailure.Unavailable` (`:core:model`, 4d Task 0).
  - **`TripGenerationDuplicateTests`** (new, R-A366): `didCreateFailureRetryReusesCreatedTrip` — for each of `StoreFailure.Unavailable`, `RecoveryRejected()` and a foreign `CancellationException` (`StaleSessionException()`) thrown by the first `didCreate` (one Parameterized case each): the result is `Failed(Server(null))` with `state.savedTripId == "created"` (no throw); a second `save` with that state makes **no** request (`count("save") == 1`, `count("update") == 0` for an edit); and a second `save` with the *original* state (`savedTripId = null`, same identity) on the same service also makes no request (the `savedTrips` cache). A caller-cancelled save still throws `CancellationException`.
- Tick plan 9b "**Generation**" (all three bullets; the checking timers are 9a's, already green).

### Task 2. Removed

9a ships `WizardChecking`, `CheckingTestClock` and `WizardCheckingTests` (9a Task 6). Nothing to commit; the number is kept so cross-references stay valid. The plan's "**Generation**" box (whose third bullet is the checking timers) is ticked in Task 1's commit once Task 1's tests pass, with the commit body naming 9a Task 6 for the timers.

### Task 3. `feat(wizard): port airport search with pacing and a cache`

- Files: `core/wizard/…/model/AirportSearchService.kt`, `AirportSearchCache.kt`, `AirportSearchState.kt` and `AirportSearchSession.kt` (§6.4); `core/testing/…/wizard/WizardSearchProbe.kt` (Swift `WizardSearchPresentationTests.swift:69–75`). The clock is 9a's `WizardTestClock` (moved in Task 0: `now` starts at `Instant.ofEpochSecond(1_800_000_000)`, `advance(seconds: Double)` in exact milliseconds).
- Tests:
  - **`WizardSearchTests`**, the Swift suite inside `WizardSearchPresentationTests.swift:6–67`, 3 methods:
    - `debouncesAndPacesRequestsAcrossSheetsBelowSixtyPerMinute`: `""`, `"A"` and 61 × `"A"` return empty; 65 × `" AMS "` → 65 requests, all `"AMS"`; the first at +0.35 s (±0.0001); gaps ≥ 1.019 s; ≤ 60 in any 60 s window; every `sleep` argument is > 0. Build the service **without a cache**.
    - `cancelledDebounceNeverMakesRequest`.
    - `serverThrottleSetsSharedWait`: the first `"AMS"` throws `Throttled(42.0, 429)`; the following `"HND"` request is ≥ 42 s after the first.
  - **`AirportSearchCacheTests`** (new):
    - `hitSkipsPacingAndRequest` (the second `"ams"` after `"AMS"` makes no request and asks for only the 0.35 s sleep);
    - `entryExpiresAtExactlyOneHour` (hit at 3599.999 s, miss at 3600 s);
    - `evictsLeastRecentlyUsedBeyondSixtyFour`;
    - `failuresAreNotCached`;
    - `emptyResultsAreCached`.
  - **`AirportSearchSessionTests`** (new; fake `search`, `sleep = delay`, virtual time):
    - `shortQueryIsHintWithoutRequest` (`"D"`);
    - `typingWithinDebounceSendsOnlyLastQuery` (`"A"`, `"AM"`, `"AMS"` at 100 ms steps → one request, `"AMS"`);
    - `emptyResultIsEmptyState`;
    - `overSixtyCodePointsIsEmptyWithoutRequest`;
    - `failureThenRetrySearchesAgain`;
    - `throttledCarriesRetryAtAndIgnoresEarlyRetry` (42 s);
    - `searchingKeepsStaleResults`;
    - `codePointLengthCountsSurrogatePairsOnce` (two emoji = 2 code points → searches).
- Tick "**Airport search**".

### Task 4. `feat(data): port the wizard recovery store`

- Files:
  - `core/model/…/store/DraftRecord.kt`;
  - the `drafts` named queries in `Drafts.sq`, their `AccountSql` mappings in `core/database/…/database/store/RecordMapping.kt`, and the two narrowed REPLACE patterns (`OR\s+REPLACE`, `\bREPLACE\s+INTO`) in 4a's `SqlSafetyTest` (§6.5; same commit, or `make quick` fails);
  - reuse Task 1’s `core/wizard/…/model/RecoveryRejected.kt` (`class RecoveryRejected : Exception()`; Task 6’s writer imports it);
  - `core/data/…/wizard/DraftRepository.kt` and `MemberWizardStore.kt` (§6.5, everything except `start(seed:)`, which is 11a's), on 9a's `MemberWizardRecord` and `MemberWizardDrafts`;
  - `core/data/…/reminders/FirstTripRemindersOffer.kt` (`queue` only);
  - module deps for `:core:data`.
- Test support: `core/data/src/test/…/wizard/support/WizardStore.kt`:
  - `wizardDatabase()`: a file-backed JDBC `AccountDatabase` for `019a562b-8000-7000-8000-000000000001`, as 4d's `SyncStore` / 4a's `RepositoryStore` (`:core:database-testing` `JdbcAccountDriverFactory`); close it in `finally`;
  - `wizardTrip(id, origin, departure)`: port `WizardRecoveryTests.swift:126–137`, with card values verbatim.
- Tests (`core/data/src/test/…/wizard/`):
  - **`WizardRecoveryTests`**, the 3 store methods:
    - `recoveryExpiresAtExactlyThirtyMinutes` (cases 0, 1799, 1799.999, 1800, 1801 s; also `drafts()` stays empty and `values()` emits `[]`);
    - `firstTripAndLatestOriginPrefillExcludeDeletedTrips`;
    - `discardSaveAndExpiredRecordCannotBeResurrectedByAnOldWrite` (Swift `CancellationError` → `RecoveryRejected`).
  - **`DraftRepositoryTests`** (new):
    - `updateIgnoresOlderRevisionAndRejectsOtherInstance`;
    - `deleteRecoveryIgnoresOtherInstance`;
    - `undecodableRowIsDeletedOnRecover`;
    - `updatedAtWithAndWithoutFractionParses`;
    - `recoveryJsonAlwaysCarriesInstanceAndRevision` (a default state through 9a's `MemberWizardRecord.encode`: the `wizard_state` text has `"instance"` and `"revision":0`; see §5.4 #6 for the fix if it fails);
    - `firstTripOfferIsQueuedOnceAsJsonString` (value `"\"pending\""`; a second queue keeps it; `firstTrip = false` writes nothing);
    - `firstTripStaysFalseWhenTheLatestCardDoesNotDecode` (a trip whose `card` is `"null"` → no prefill, `firstTrip == false`; §6.5 `initialDraft`, through 9a's `hasTrip`);
    - `missingStoredRevisionCountsAsZero` (a row whose `wizard_state` lacks `"revision"` accepts an update at revision 1).
  - **Draft halves of iOS `RecordsTests`** (4a hand-off; 4a ported the rest without them into `core/data/src/test/…/data/repository/RecordsTests.kt`), in `DraftRepositoryTests` under the Swift names:
    - `allRecordsRoundTripUnknownJSONNullsAndCompositeKeys` (draft half, `RecordsTests.swift:27–33, 49–50, 62–64`): `json = {"future":{"big":123456789012345678901234567890,"list":[null,true,"é"]},"n":1.000}`; `DraftRecord(id = "draft", tripId = "trip'", form = json, baseForm = json, dirty = false, review = json, token = null, inputRevision = 5, reviewRevision = 4, wizardSourceRevision = "r", wizardReview = json, furthestStep = "sleep", wizardState = json, sourceRevision = json, saveFailure = json, fieldErrors = json, updatedAt = "now")` inserted and read back equal (byte-equal JSON text); then `DraftRecord(id = "empty", form = json, updatedAt = "later")` through `upsert` is in `drafts()`. Insert the `trips` row `"trip'"` first only if `drafts.trip_id` has a foreign key (it has none in iOS `SchemaV1.swift:29–33`).
    - `repositoriesBindKeysAndOrderOutboxByTimestampThenID` (draft half, l.93–96): `upsert(DraftRecord(id = "draft", form = "{}", updatedAt = "now"))`, `remove("draft")` → `drafts()` is empty.

### Task 5. `feat(data): store wizard generation effects`

- Files: `core/data/…/wizard/TripGenerationEffectsStore.kt` (§6.6).
- Tests:
  - **`TripGenerationTests.profileReviewAnswersAndSavedPlanReachSQLite`** (class `TripGenerationTests` in package `net.chronoflow.core.data.wizard`). Port the Swift body (`TripGenerationTests.swift:233–297`):
    - `MockAppSeed.make(now = 1_793_750_400 s)`, sign in the non-two-factor account, `MockDataSource(sleep = {})`;
    - apply identity + snapshot with `StoreApplication`;
    - `needs_answers` → answer → `inputRevision + 1`; `ageBand == "18_64"`; syncs == 2;
    - saved trip with its plan in `TripRepository`; counts save 1, trips 1, detail 1;
    - a pending body-clock row wins over the merge;
    - an invalidated epoch → a `CancellationException` subtype.
  - **`WizardGenerationSessionTests.reviewEffectsRequireTheOriginalSessionPermitThroughCommit`**, using 4e's session controller. If it cannot be built in a JDBC test, use a `SessionMutationPermit` and revoke it, and say so in the commit body (OQ-9b-11).

### Task 6. `feat(wizard): write wizard recovery through a conflated writer`

- Files in `core/wizard/…/model/`:
  - `WizardRecoveryWriter.kt` (§6.5);
  - 9a's `MemberWizardModel.kt`: the `writerScope` parameter and the `persist()`/`flush()` swap of §6.7, nothing else. 9a's `:core:wizard` suite must pass unchanged.
- Tests:
  - **`WizardRecoveryWriterTests`** (new, `:core:wizard`, fake in-memory store):
    - `burstOfPersistsWritesLatestRevisionLast`;
    - `flushWaitsForQueuedWrite`;
    - `rejectedWriteIsSilent`;
    - `foreignCancellationIsSilentAndKeepsWriting` (a `StaleSessionException` from one `persist` sets no `persistenceFailed`, and a later enqueue is still written);
    - `storeFailureReportsPersistenceFailed`;
    - `flushReturnsWhenWriterScopeIsCancelled`.
  - **`WizardRecoveryTests.editingAndClosingFlushQueuedWritesAndRememberStepAndSheet`** (`:core:data`, Swift `WizardRecoveryTests.swift:86–110`; 9a's model has no `search` argument, so Swift's `noAirportSearch()` is dropped).
- Tick "**Recovery**".

### Task 7. `test(data): port the wizard review flow tests`

- **Port adaptation (2026-11-20):** Swift `@testable` calls to `applyReview` and job handles remain internal in Kotlin. The DB suites drive reviews through the public model and `GenerationSource`, then assert snapshots, failures, requests and timer cleanup. `ApiReviewPrompt.copy` is private; helpers use its public factory. Inject the test IO dispatcher into mock sources, and keep wait helpers on the test dispatcher so they do not auto-advance the checking clock. No production test hooks or earlier-phase behaviour change.

- `core/data/src/test/…/wizard/WizardReviewFlowTests.kt`: the 17 methods of `WizardReviewFlowTests.swift` that 9a leaves (all 19 except `returnPastUsesFinalArrivalAndAirportZones` and `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys`, which 9a ported), with its private `SuspendedReviewCancellation` (l.531), `ReviewFixture`/`reviewFixture` (l.561–588, with `now = { Instant.parse("0001-01-01T00:00:00Z") }` for Swift `.distantPast`), `finishCheck` (l.590) and `screeningPrompt` (l.598). `requestCancellationShowsFailureWithoutAutomaticRestart` and `resumedQuestionChecksOfflineWithoutWaitingForPreviousCancellation` are `@Test(arguments: [false, true])` (Parameterized, §7 intro).
- The fixture uses `MemberWizardStore` on `wizardDatabase()` (Task 4) and `TripGenerationService` over `GenerationSource` (Task 1). Swift passes `search: noAirportSearch()`; 9a's model has no `search`, so drop it.
- Wherever the Swift fixture or test uses `.continuous`, use 9a's `CheckingTestClock(testScheduler)` and never advance it (§5.4 #12). Swift `finishCheck(start:)` seconds become milliseconds (`advance(toMillis = …)`).

### Task 8. `test(data): port the wizard completion, retry and save recovery tests`

- **`WizardCompletionTests`**: the 10 methods 9a leaves (all 12 except `creditsCoverEveryReadyVariant` and `sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks`). Includes `CompletionFixture`/`completionFixture(optional:, firstTrip:, effects:)`.
- **`WizardRetryTests`**: 6 methods.
- **`WizardSaveRecoveryTests`**: 7 methods, with `FailingSavedIDWriteGeneration` (l.191). Plus the Android case **`foreignDidCreateCancellationRetryDoesNotCreateASecondTrip`** (R-A366, OQ-A366): real `TripGenerationService` over `GenerationSource`, `MemberWizardStore` on `wizardDatabase()` whose permit is revoked inside `didCreate` (or a store wrapper that throws `StaleSessionException` once) → the model returns to `ready` with `saveFailure.kind == server` and `savedTripId == <created id>`; after the countdown, a manual `savePlan()` closes with `createdTripID == <created id>` and `count("save") == 1`.
- **`TripWizardActionsTests`**: the 4 methods 9a leaves (9a §10.2): `editStartsAtCheckingReviewsImmediatelyAndUntouchedEditCannotSave`, `changedSourceReloadsInputAirportsBaselineAndRevisionBeforeUpdate`, `editSaveFailureKeepsChangesAndRetryUpdatesTheSameTrip`, `duplicateStartsOutboundAndSavesFreshWithoutEditingOriginalAgainstMock` (the last on 4c's `MockDataSource(seed, now, sleep = {})`). They use `wizardDatabase()`, `MemberWizardStore.startTrip` and `GenerationSource`.
- **`GuestWizardModeTests.guestFineTuneUsesOnlyTheDrawnRowsAndLeavesMemberRowsAvailable`** (9a §10.2; needs 4c's `MockWizardPrompts.make(input, profile)`), in `:core:data` tests.
- **Adaptation: `consume` is 12c's.** Swift's `offer.consume(permission: .undetermined)` asserts become reads of the settings section `wizardReminders` (absent ≙ `false` before any save; `"\"pending\""` ≙ the first `true`). A Swift `consume` that follows a `true` marks the row `seen`; with no `consume`, the row simply stays `"\"pending\""`.
  - `onlySuccessfulFirstTripSaveQueuesReminders` (`@Test(arguments: [true, false])`, l.67–84): absent before the save; absent after the offline failure; after the successful save, `"\"pending\""` when `firstTrip`, absent otherwise.
  - `savedTripIDIsDurableBeforePullAndRecoverySkipsCreate` (l.86–120): absent before `didSave`; `"\"pending\""` after the recovered model completes; still `"\"pending\""` after the first model completes.

### Task 9. `test(data): cover wizard fixture scenarios and the no-retry rule`

`WizardFixtureScenarioTests` (new, `:core:data`, `completionFixture`/`reviewFixture` from Tasks 7–8; Swift declares them `private`, so in Kotlin declare them `internal` in `core/data/src/test/…/wizard/support/` in Tasks 7 and 8 so this class can reuse them).

Each failure comes from its fixture. 4b's envelope → `DataFailure` mapper (`ApiErrorMapper`) is `internal` to `:core:network` (Stack check), so use these literal values; they are exactly what 4b's `TripWizardTypedErrorTests.typedErrors` asserts. Still load each fixture once (status in brackets) so a renamed fixture fails the test.

| Fixture (status) | Failure |
|---|---|
| `throttled.json` (429) | `Throttled(42.0, 429)` |
| `capacity.json` (422) | `SaveCapacity` |
| `expired.json` (422) | `ReviewExpired` |
| `credits.json` (422) | `NotEnoughCredits` |
| `unknown-field.json` (422) | `Validation(mapOf("legs.0.surprise" to FieldMessage("This setting cannot be changed here.")), 422)` |

Methods:

| Method | Asserts |
|---|---|
| `throttledReviewCountsDownFromFixture` | Checking failure `Throttled`, `retrySeconds == 42`, `beginChecking()` ignored until the checking clock reaches 42 000 ms after the failure, then one new review |
| `throttledSaveCountsDownWithoutRetry` | `saveFailure.kind == throttle`, `retrySeconds == 42`, save count stays 1 |
| `capacitySaveIsServerFailureWithoutRetry` | `saveFailure.kind == server`; after 10 `readyTick()` calls save count == 1 |
| `expiredSaveClearsIdentityAndReReviews` | identity null, review count +1, save count 1 |
| `creditsSaveClearsIdentityAndReReviews` | same, for `NotEnoughCredits` |
| `doubleExpiredReviewShowsFailure` | review count 2, Checking failure `ReviewExpired` |
| `needsAnswersFixtureAsksAgeThenShift` | `wizard/needs-answers.json` queued as the review → step `question`, question ids `["age_band:::", "screening:shift_work::"]` |
| `unknownFieldReturnsToOwningStep` | `fieldErrors["legs.0.surprise"]` present, step == `WizardJourney.errorStep(fieldErrors, form)` and ≠ `checking` |
| `saveIsNeverRetriedWithoutUserAction` | for each of `Offline`, `Server(500)`, `Throttled(31.0, 429)`, `SaveCapacity`, `EmailUnverified(403)` (fresh fixture per case): `savePlan()`; advance the wall clock and the checking clock by 3600 s in 60 s steps, calling `readyTick()` at each step → **save count == 1** (reviews may happen after 1800 s); then a manual `savePlan()` once `retrySeconds == 0` → save count 2 |

Tick "**Tests**".

### Task 10. `feat(wizard): add the trip wizard view model scaffold`

- Files:
  - `feature/wizard/…/TripWizardViewModel.kt`, `TripWizardUiState.kt` and `TripWizardDependencies.kt` (§6.8, with `TripWizardDependenciesKey` and the factory);
  - `:feature:wizard` build deps (§5.1);
  - `:app`: `app/src/main/kotlin/net/chronoflow/app/wizard/SessionTripWizardDependencies.kt`, plus the per-session wizard services. 4e's `SessionGraph` is a `data class` with no permit and no dispatchers, built only in `SessionGraphs.kt` (inside its `controller.updates` collector). Add a `permit: SessionMutationPermit?` to `SessionGraph` (read `controller.mutationPermit(snapshot)` there, beside the existing `activeSession() != snapshot` re-check) and a lazy `wizard` holder on it; a `null` permit → no wizard dependencies (iOS `NativeSessionController.swift:173–177` throws `CancellationError` in that case). `AppDispatchers` comes from `AppGraph` (`graph.dispatchers.io`), so pass it when building the holder. With a permit, the holder is built lazily from:
    - `store = MemberWizardStore(database, permit, now = { Instant.now() })` (per session);
    - `airportSearch = AirportSearchService(dataSource, now = { Instant.now() }, sleep = { delay((it * 1000).roundToLong()) }, cache = AirportSearchCache())` (per session);
    - `writerScope = scope + graph.dispatchers.io`;
    - `newGeneration()`, a new service per call (per wizard opening):
    ```kotlin
    TripGenerationService(
        dataSource,
        TripGenerationEffects.storing(database, dataSource,
            epoch = SyncEpoch(AccountID.of(snapshot.session.accountId), snapshot.epoch),   // SessionSnapshot(session: StoredSession, epoch); iOS NativeSessionController.swift:178
            permit = permit, requestSync = { sync.requestSync() }, now = { Instant.now() }),
        parentContext = scope.coroutineContext + graph.dispatchers.io,
    )
    ```
  - No test constructs `SessionGraph`/`SessionGraphs`/`AppGraph` for this (Konsist `AppTestBoundaryRules.kt`); the `:app` wiring is covered by `make check`'s compile and Lint, and the behaviour by `TripWizardViewModelTest` on fakes.
- Tests: **`TripWizardViewModelTest`** (new, plain JVM in `feature/wizard/src/test`, a `MainDispatcherRule` like `feature/trips/src/test/…/FakeTripsDependencies.kt:40`, fake dependencies; cancel the ViewModel store before resetting Main, AGENTS "Tests" 1):
  - `createStartsOrRecoversThroughStore`;
  - `editAndDuplicateLoadTripInput`;
  - `editLoadFailureIsExposed` (`Offline` → `LoadFailed`);
  - `recoveredCalculatingResumesSaveOnce` (save count 1 → `Closed("created")`);
  - `recoveredCheckingRunsCheckOnce`;
  - `clearingCancelsGenerationButFinishesPendingWrite` (create the VM through a `ViewModelStore`, persist, `store.clear()` → a later `review` on the service `newGeneration()` returned throws `CancellationException`, and the fake store, whose `persist` is gated until after `clear()`, eventually holds the last revision);
  - `searchQueryReachesSharedService`.
- Tick "**TripWizardViewModel scaffold**".

### Task 11. `docs: record phase 9b decisions`

- Add to `plan.md` "Open questions for the user" (rows end at OQ-A366, l.952), tagged "(9b OQ-9b-n)" as 9a did: OQ-9b-2 → **OQ-A367**, OQ-9b-7 → **OQ-A368**, OQ-9b-15 → **OQ-A369**, with their defaults. If another row lands first, take the next free numbers in the same order. (OQ-9b-1 is 9a's OQ-9a-1 = OQ-A349; OQ-9b-13 is settled by 9a's `memberInitial(hasTrip, …)` and is not recorded.) Also append "Resolved by 9b: R-A366 (`TripGenerationDuplicateTests`, `foreignDidCreateCancellationRetryDoesNotCreateASecondTrip`)" to OQ-A366's default.
- Add one line to 9g "After save": "(queued by 9b's `MemberWizardStore.didSave`)".
- Hand-off note at the end of this brief: what 9c, 9f, 9g, 9h, 11a and 12c call (§12).

---

## 8. Ported test inventory (iOS `Tests/TripWizardTests` at `f391a1fa`)

Counts verified with `grep -c "@Test"` at the pin; the split with 9a follows 9a §10.

| Swift suite | iOS total | 9b ports | Kotlin home | Task |
|---|---|---|---|---|
| `TripGenerationTests` | 10 | 10 | `:core:wizard` (9), `:core:data` (1, SQLite) | 1, 5 |
| `TripGenerationRecoveryTests` | 9 | 9 | `:core:wizard` | 1 |
| `WizardSearchTests` (in `WizardSearchPresentationTests.swift:6–67`) | 3 | 3 | `:core:wizard` | 3 |
| `WizardRecoveryTests` | 4 | 4 | `:core:data` | 4 (3), 6 (1) |
| `WizardGenerationSessionTests` | 1 | 1 | `:core:data` | 5 |
| `WizardReviewFlowTests` | 19 | 17 (9a has 2) | `:core:data` | 7 |
| `WizardCompletionTests` | 12 | 10 (9a has 2) | `:core:data` | 8 |
| `WizardRetryTests` | 6 | 6 | `:core:data` | 8 |
| `WizardSaveRecoveryTests` | 7 | 7 | `:core:data` | 8 |
| `TripWizardActionsTests` | 5 | 4 (9a has 1) | `:core:data` | 8 |
| `GuestWizardModeTests` | 10 | 1 (9a has 9) | `:core:data` | 8 |
| `LocalStoreTests/RecordsTests` | 3 | draft halves of 2 (4a/4d have the rest) | `:core:data` (`DraftRepositoryTests`) | 4 |
| **Total** | | **72** + 2 draft halves | | |
| 9a's, not here | `WizardCheckingTests` (4), `WizardAirportChoiceTests` (2), `WizardPresentationTests`, `WizardRouteTests`, `WizardPortTests`, `WizardFlightTimingTests`, `WizardHomeRouteTests`, `WizardCreditsTests` | — | — | — |
| Later phases | `WizardCopyTests` (9c–9g), other `Guest*` suites (11a, 11b) | — | — | — |

Android-only: `AirportSearchCacheTests` (5), `AirportSearchSessionTests` (8), `DraftRepositoryTests` (8 + the 2 draft halves), `WizardRecoveryWriterTests` (6), `TripGenerationDuplicateTests` (1 method, 3 parameter cases), `WizardSaveRecoveryTests.foreignDidCreateCancellationRetryDoesNotCreateASecondTrip` (1), `WizardFixtureScenarioTests` (9), `TripWizardViewModelTest` (7).

---

## 9. Acceptance criteria

1. `make quick` passes. `build/test-results/quick/timings.json` shows `"overBudget": 0`, and every new case runs in < 1 s (JVM; AGENTS "Tests" 5 — no first-case allowance on plain JVM).
2. `./gradlew :core:wizard:test :core:data:testDebugUnitTest :feature:wizard:testDebugUnitTest :konsist:test` passes. The XML results list every class in §8 with the Swift method names.
3. `make targeted ONLY="core:wizard:WizardCheckingTests core:wizard:WizardSearchTests core:wizard:AirportSearchCacheTests core:data:WizardRecoveryTests core:data:WizardFixtureScenarioTests"` passes (module without a leading colon, 4c §4 item 5). These are the "timer, pacing and recovery cases on a fake clock" (the checking timers being 9a's suite, still green after Task 6) and the never-retry rule.
4. `make check` passes, including Lint, `:app:testReleaseUnitTest` (no `:core:testing` or mock class in release) and `@Smoke` on `pixel`. If the sandbox cannot run the emulator, report the command and its output.
5. Static checks, each printing nothing:
   - `grep -rn "runCatching" core/wizard/src/main core/data/src/main/kotlin/net/chronoflow/core/data/wizard feature/wizard/src/main`
   - `grep -rnE "Instant\.now\(\)|System\.currentTimeMillis|ZoneId\.systemDefault|Clock\.system" core/wizard/src/main`
   - `grep -rn "\.debounce(" core/wizard/src/main feature/wizard/src/main`
   - `grep -rn "withTimeout" core/wizard/src/main`
   - `grep -rn "ON CONFLICT" core/database/src/main/sqldelight`
   - `grep -rliE "OR\s+REPLACE" core/database/src/main/sqldelight | grep -v "/Drafts.sq$"`
   - no second definition of a 9a type: `grep -rnE "class (MemberWizardModel|WizardCheckingClock|TripGenerationState|TripEditContext|TripGenerationReviewResult)\b|object (WizardChecking|TripGenerationAnswers|MemberWizardRecord)\b|interface (MemberWizardGeneration|WizardDraftStore)\b" core/ feature/ --include=*.kt | grep -v "core/wizard/src/main"` prints nothing, and inside `core/wizard/src/main` each appears exactly once.
6. `grep -rn "\.save(" core/wizard/src/main` shows `generation.save` / `generation?.save` called only from `MemberWizardCompletion.kt` `savePlan`.
7. No new string resource, no new file under `res/`, and no Compose code in this phase (`git diff --stat` against the stack base).
8. No screenshots and no perf numbers are required: this phase has no UI and touches no timeline or startup path.
9. **R-A366 (OQ-A366): a manual retry after a `didCreate` failure never creates a second trip.** `make targeted ONLY="core:wizard:TripGenerationDuplicateTests core:data:WizardSaveRecoveryTests#foreignDidCreateCancellationRetryDoesNotCreateASecondTrip"` passes: after a `StoreFailure`, `RecoveryRejected` or foreign `CancellationException` from `didCreate`, the model is on Ready with a server failure and the created `savedTripId`, and the retry closes on the same trip id with exactly one `POST trips` (or `PUT trips/{id}`).
10. 9a's `:core:wizard` suite passes with no test edited (Task 6), including `MemberWizardReviewTest`.

---

## 10. Review checklist (Opus reviewer)

- [ ] **Dedup key** has all five parts. The fingerprint is a canonical `String` with identity removed and profile filtered. The entry is removed in the `Deferred`'s own `finally` under the mutex and only for its own id. A cancelled waiter does not cancel the shared review (`reviewDeduplicates…` makes 5 requests, then a 6th after completion).
- [ ] **The 30 s review cap** throws `DataFailure.Server(null)`, not a `TimeoutCancellationException`. It covers source refresh and the expiry retry. Saves have **no** cap.
- [ ] **Expiry retry** happens exactly once and without identity. Edit refresh drops `profile` and `identity` only when the source revision changed. A repeated revision mismatch → `ReviewExpired`.
- [ ] **Save** has no loop, no retry and no `delay`:
  - `savedTripId` short-circuits the POST, and `savedTrips` caches by `SaveKey`;
  - `didCreate` is awaited **before** `pullCreatedTrip`;
  - pull errors are swallowed;
  - `ReviewExpired`/`NotEnoughCredits` clear identity, review and `savedTripId` only when no `savedTripId` exists;
  - a non-`DataFailure` from `didCreate` → `Failed(Server(null))` with `savedTripId` kept;
  - **R-A366**: a foreign `CancellationException` from `didCreate` (caller still active) also → `Failed(Server(null))` with `savedTripId` kept; only caller/service cancellation rethrows; `savedTrips[key]` is set before `didCreate`; a manual retry makes no second POST/PUT (acceptance 9);
  - every new `TripGenerationState` is built with 9a's factory (its `copy` is private).
- [ ] **9a ownership**: no 9a type, model member, `WizardChecking` or 9a test was re-ported or copied (acceptance 5). The only 9a edits are Task 0's move, Task 6's `persist()`/`flush()`/`writerScope` hook, and named `fix(wizard): …` commits each citing the Swift line 9a missed. 9a's `:core:wizard` suite passes unchanged.
- [ ] **`WizardChecking`** (9a's, exercised end to end here): tick 3 never before 1 200 ms, then 300 ms; slow at exactly 10 000 ms; `Server(null)` at 30 000 ms. The service lets a cancelled review surface as `CancellationException`, so 9a's run classifies it.
- [ ] **Choice delay** (9a's) is 250 ms on `checkingClock`; Tasks 7–8 exercise it.
- [ ] **Airport search**:
  - code-point length 2…60;
  - exactly one 350 ms wait (no `Flow.debounce`);
  - the pacing mutex is not held across `sleep`;
  - the first request is at +0.35 s, gaps ≥ 1.02 s;
  - a 429 extends `nextRequest` to `max(…)`;
  - the cache is checked after the debounce and before pacing, failures are never cached, the key is lower-cased with `Locale.ROOT`, and TTL is exactly 3600 s.
- [ ] **Recovery**:
  - the id is `member-wizard`;
  - the expiry comparison is `< 1_800_000` ms (1799.999 s recovers, 1800 s deletes), inside one write transaction;
  - `updateRecovery` checks the instance and drops revisions older than the stored one (`>`, not `>=`);
  - `deleteRecovery` checks the instance;
  - a decode failure deletes the row;
  - the listed queries exclude `member-wizard`;
  - 9a's codec keeps `instance` and `revision` (`recoveryJsonAlwaysCarriesInstanceAndRevision`);
  - `firstTrip` is `latest == null` (passed as 9a's `hasTrip`), even when the latest card fails to decode;
  - a missing stored `"revision"` reads as 0, and 9a's codec writes `"revision"` (codec-local `encodeDefaults`, `ApiJson` untouched);
  - no `ON CONFLICT` (SQLite 3.22 on API 29), and `SqlSafetyTest` exempts only `Drafts.sq` from the two REPLACE patterns;
  - every non-null JSON column goes through `JSONDocument.requireValid`.
- [ ] **Writer**:
  - conflated, with one consumer;
  - `RecoveryRejected` and a foreign `CancellationException` (consumer still active) are silent, other errors set `persistenceFailed`, and the consumer's own cancellation rethrows (9a's rule);
  - `flush` cannot hang after the session scope is cancelled;
  - it runs in `writerScope`, **not** `viewModelScope`;
  - `discard()` flushes before deleting.
- [ ] **Model confinement** (9a's model): the writer's `onFailure` sets `persistenceFailed` through the flow's atomic `update`; `discard()` keeps the Swift order (… `generation?.cancel()`, `flush()`, `store.discard`) and restores `closed = false` on a store failure.
- [ ] **No business `CancellationException`**: there is no `catch (e: Exception)` without a preceding `CancellationException` rethrow, and no `runCatching`.
- [ ] **Effects**:
  - `mergeProfile` re-applies pending body-clock rows in FIFO order inside the same transaction, deletes `readiness`, and sets `sleepAnswered` only for sleep fields;
  - `pullCreatedTrip` calls `epoch.check()` after every network await;
  - `requestSync` is skipped for a dead epoch, without swallowing coroutine cancellation.
- [ ] **No UI leakage**: 9b adds no visible string and no English UI text to `:core:wizard`; `checkingTitle` stays as 9a ported it.
- [ ] **Naming**: `core.navigation.WizardMode` (Create/Edit/Duplicate) and `core.wizard.model.WizardMode` (member/guest) are never confused. The ViewModel imports the navigation one with an alias.
- [ ] **ViewModel**: `onCleared` calls `generation.cancelNow()` and never calls `discard`; `discard()` stays `Active` and rethrows when the store fails; the permit comes from `SessionController.mutationPermit(snapshot)` and a null permit yields no wizard dependencies (`LoadFailed(Unauthorized(401))`). The factory uses a feature-owned `TripWizardDependenciesKey`, never `AppGraphKey`. No draft or secret goes into `SavedStateHandle`. `:feature:wizard` does not depend on `:core:data`. No new test builds `AppGraph`/`SessionGraph(s)`/`RootContent`/`CfRoot`/`MainActivity`.
- [ ] **Tests**: Swift method names are kept, with no `Thread.sleep`, no `runBlocking` and no `WizardCheckingClock.continuous` in tests. The model runs in `backgroundScope` and the service on `Dispatchers.Default`. Gates are closed or services cancelled in `finally`. `timings.json` shows no budget overrun.
- [ ] **Generated and copied files**: fixtures are not re-copied, and nothing under `core/testing/src/main/resources/fixtures` changed.

---

## 11. Open questions (defaults to proceed on)

| # | Question | Default |
|---|---|---|
| OQ-9b-1 | The plan puts the cancellable jobs in `TripWizardViewModel`. Should they live there, or in a JVM model the ViewModel owns? | Settled by 9a (OQ-9a-1): **9a** ports `MemberWizardModel` with its jobs into `:core:wizard`; 9b hosts it on `viewModelScope` and only swaps its persistence for the conflated writer. The DB-backed suites stay JVM (cheapest tier) and diff 1:1 against Swift. Nothing to record (9a Task 10 does). |
| OQ-9b-2 | iOS writes every revision sequentially. The plan says a conflated writer. | (→ OQ-A367) **Conflated**, with one consumer. Only superseded revisions are skipped, the DB guard keeps order, and `flush` + `didCreate`'s awaited write keep the iOS guarantees (discard flushes and then deletes; `savedTripId` is durable before the pull). |
| OQ-9b-3 | The airport cache's size, TTL and key are not specified anywhere. | 64 entries, TTL 3600 s (the server's `max-age`), key = trimmed query lower-cased with `Locale.ROOT`. Empty results are cached, failures are not. A hit still waits the 350 ms debounce, but skips pacing. |
| OQ-9b-4 | Query length: Swift counts grapheme clusters, the server counts code points (`mb_strlen`). | **Code points.** Grapheme counting could send 61 code points and get a 422. |
| OQ-9b-5 | A query of more than 60 characters: iOS returns `[]` and shows nothing special. | `AirportSearchState.Empty`, with no request. 9c may draw "No airports found". |
| OQ-9b-6 | Should `Searching` hide the previous results? (The board shows the skeleton.) | `Searching` carries the `stale` results. 9c shows them while it has some (iOS keeps its list), and shows the skeleton when there are none. |
| OQ-9b-7 | 9g says "set the first-trip reminders offer to `pending`". iOS does this in `MemberWizardStore.didSave`. | (→ OQ-A368) 9b ports `FirstTripRemindersOffer.queue` and calls it from `didSave`. 12c ports `consume`. Record this as an OQ-A row (Task 11). |
| OQ-9b-8 | Generation service lifetime. | One per wizard opening (iOS `AppTabs.swift:501, 522`), as a child of the session scope. Cancelled by `discard()`, `onCleared()` or a session change. |
| OQ-9b-9 | `checkingTitle` returns English text on iOS. | 9a's call (it ports `TripWizardActions.swift`); 9b leaves it as 9a ships it. 9f maps it to resources. |
| OQ-9b-10 | Swift uses `CancellationError` for a rejected recovery write. | `RecoveryRejected : Exception`. Ported assertions change accordingly. |
| OQ-9b-11 | `WizardGenerationSessionTests` builds a full session controller. | Use 4e's controller if it builds on JDBC. Otherwise use a revoked `SessionMutationPermit`, which keeps the asserted behaviour: the second `mergeProfile` fails with a `CancellationException` subtype, and the body clock keeps `23:30`. |
| OQ-9b-12 | Edit/Duplicate when `tripInput` fails (06 OQ 9). | The ViewModel exposes `LoadFailed(failure)`. 9h decides the presentation; there is no board for an editor error state. |
| OQ-9b-13 | ~~9a's `memberInitial` derives `firstTrip` from the origin.~~ | **Settled by 9a** (Stack check 2026-11-20): `memberInitial(hasTrip, latestOrigin, …)` sets `firstTrip = !hasTrip`; `initialDraft` passes `hasTrip = latest != null`. The test `firstTripStaysFalseWhenTheLatestCardDoesNotDecode` stays. Not recorded. |
| OQ-9b-14 | 9a's model has no test seam for its checking clock in the ViewModel. | `TripWizardDependencies.checkingClock` (default `WizardCheckingClock.continuous`); test fakes pass 9a's `CheckingTestClock`. |
| OQ-9b-15 | (OQ-A366) Swift's `save` rethrows any `CancellationError`, including one thrown by `didCreate` after the server created the trip; with 9a's Android rule (foreign cancellation → Ready + server failure) the created id would be lost and a manual retry could create a duplicate. | The service returns `Failed(Server(null))` with `savedTripId` kept for a foreign cancellation from `didCreate` (caller still active), keeps `savedTrips[key]` before `didCreate`, and relies on the server receipt after process death (R-A366, §6.1). Record as OQ-A369 (Task 11). |

---

## 12. Hand-off (what later phases call)

- **All:** the intent members below are 9a's `MemberWizardModel` members; 9b only exposes the model through `TripWizardViewModel.wizard`.
- **9c:** `TripWizardViewModel.state` (`Active.path` = `navigationPath`), `wizard.popNavigation`, `go`, `openAirport`, `chooseAirport`, `swapAirports`, `removeStop`, `makeDirect`, `finishRoute`, `airportIsAllowed`, `discard()`, `search.setQuery`/`retry`/`state`, and `hasChanges` for the discard alert.
- **9d:** `setDateTime`, `finishOutbound`, `finishReturn`, `finishOutboundStep` and `chooseReturn` (250 ms).
- **9e:** `setSleep`, `finishSleep`, `answerQuestion` (250 ms), `backFromQuestion`, `changeExcludedAnswers` and `exclusionWasSaved`.
- **9f:** `checking` (ticks, slow, failure, `retryAt`), `retrySeconds`, `checkingTitle`, `beginChecking` (Try again), `optionalPrompts`, `openPrompt`, `answerOptional`, `setPreference`, `editForm` and `finishFineTune`.
- **9g:** `readyTick` (the UI runs it every 1 s and on foreground with `force = credits == unavailable`), `savePlan`, `canSave`, `credits`, `saveFailure`, `createdTripID`/`Closed`, and `revisit`.
- **9h:** `Route.Wizard(Edit|Duplicate)` → `startTrip`, `isEditing`, `sourceChange` and `acceptSourceReload`.
- **11a/11b:** the guest members ported here, plus `MemberWizardGeneration` for `GuestWizardGeneration`.
- **12c:** `FirstTripRemindersOffer.consume`, which reads the `wizardReminders` row this phase writes.

### Delivered by 9b (2026-11-20)

`SessionGraph.wizard` lazily supplies the feature-owned `TripWizardDependencies`; a missing session permit supplies no dependencies. Each entry uses `TripWizardViewModel.factory(entry)` with `TripWizardDependenciesKey` in its current creation extras. The factory captures only the entry. Services are session-owned; each opening gets its own generation service and each ViewModel its own search session over the shared airport service.

9c hosts `state`, `wizard` and `search`, forwarding stateless-screen events through the members above. 9f drives checking/fine-tune intents and the injected-clock model. 9g calls `readyTick` and manual `savePlan`, then handles `Closed(createdTripId)`; successful first-trip saves already queue the reminders offer. 9h passes Edit/Duplicate entries to the factory and renders `LoadFailed` from trip-input loading. 11a reuses the member/guest model and generation protocol. 12c adds `FirstTripRemindersOffer.consume` over the queued `wizardReminders` row.

The latest-wins recovery contract is pinned by `persistenceNeverGoesBackwardsAndEndsOnTheLatestRevision`; clearing the ViewModel cancels generation without discarding recovery, and session-owned pending writes finish. Failed explicit discard stays Active and rethrows. R-A366 is covered at both the service and model/store boundaries: a failed `didCreate` never causes a second server trip on manual retry. Recovery JSON always encodes instance and revision, including revision zero. Decisions are recorded as OQ-A367–OQ-A369, and OQ-A366 records its resolution.
