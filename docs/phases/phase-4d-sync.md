# Phase 4d brief: Sync engine and outbox

- **Branch:** `feat/phase-4d-sync`, already created from the landed Phase 4c top. Work directly in the main checkout, one phase at a time; no waves or worktrees.
- **Depends on:** 4a (account database, epoch, permit, records, UUIDv7), 4c (MockDataSource; it brings 4b's `DataSource`, `ApiDataSource`, `DataFailure` mapping). **Execution:** single lane. **Milestone:** M2.
- **Plan entry:** `plan.md` "### Phase 4d: Sync engine and outbox" (find by heading; currently l.1819–1858), decision A7 (l.94), OQ-A17 and OQ-A40.
- **Brief written:** 2026-11-16 by the orchestrator, from iOS read at `f391a1fa` (verified: `git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`) and web `e36368ec`. **Revised the same day** against the final 4a, 4b and 4c briefs (their hand-off sections are binding; §4 lists the exact names): 4b already ships `OutboxChange`/`MutationReceipt`; 4a ships `SyncEpoch.child()`, the guarded writes, `RecordMapping` and the "no UPSERT" SQL rule; 4a moved `AccountModel`, two `RecordsTests` and six `RepositoryObservationTests` cases here (OQ-4a-11), plus the plan-version/check-mark mapping (OQ-4a-16) and the shared JDBC driver factory (OQ-4a-13). **Checked** 2026-11-16 against iOS `f391a1fa`, web `e36368ec` and the 0a/0d/0e/4a–4c briefs: test counts, line references, `SyncEvent` field names, the receipt query name, the `applyDetail` hash source, `enqueue` guards, `SessionSync` dispatch rules, the iOS login-upsert tests, the Konsist layout and the test-naming rule were corrected.
- **Tests:** follow AGENTS.md "Tests" for every test. This phase is JVM only (plain JVM tests in `:core:model`, `:core:sync`, `:core:testing`; local unit tests on the SQLDelight JDBC driver in `:core:data` and `:core:database`), plus **one** Robolectric `@Logic` class (Task 9). No emulator test, no UI.

Tags: **[F]** fact verified in source (path and line at the pin). **[R]** decision for Android adopted by this brief. Paths written `core/sync/…/X.kt` mean `core/sync/src/main/kotlin/net/chronoflow/core/sync/X.kt`. iOS paths are relative to `chronoflow-ios/Packages/ChronoFlowKit/` unless they start with `plan.md`. Read every iOS file at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`.

---

## 1. Goal

Port the iOS offline data engine (iOS Phase 9c plus the pure parts of 9d) to Kotlin so the app keeps cached trips and settings usable offline and pushes local edits durably, exactly as iOS does:

1. **Outbox rows**: the stored form of 4b's six `OutboxChange` cases, dependency keys, and `OutboxRepository.enqueue`, which commits the local edit and its UUIDv7 row in one transaction.
2. **Full pull** (`FullPull`): `users/me` → `account` → `trips` → one transaction (snapshot, cards, tombstones, `synced_at`, eviction) → retained details, with pending-value protection.
3. **Outbox flush** (`OutboxFlusher`, `CheckOffReconciler`, `OutboxFailurePolicy`): FIFO `(created_at, id)`, dependency blocking, rekeying, the stale-plan rule, final refusal → `settings.syncFailure` + undo, transient retry metadata, parked unsupported kinds, the walkthrough 401 exception.
4. **Engine** (`SyncEngine`): one flight per epoch, coalesced rerun, 1 s debounce, `refreshNow`, shared-flight waiters, sanitized events, `cancelAndWait`, trip-page `fetchDetail`.
5. **Lifecycle** (`SyncLifecycle`, `SessionSync`): triggers (start, outbox commit, reconnect, activation), backoff `min(2^n, 300)` s, the server retry-after deadline, retry state across pauses, epoch-scoped unauthorized/verification dispatch.
6. **Repositories** in `:core:data` (with the plan-version/check-mark mapping, repository observation and `AccountModel` moved from 4a) and **plan retention leases** with eviction.
7. **Preserving login upsert** (OQ-A40): same-account sign-in never overwrites a value that has a pending outbox row.
8. **Ported tests**: every Phase-9c/9d sync case of iOS `Tests/SyncTests` that does not need the session controller (63 Swift methods, 107 cases), plus the 8 LocalStore cases moved from 4a (`RecordsTests` 2, `RepositoryObservationTests` 6), under their Swift names: **71 methods, 115 cases**.

No UI. No WorkManager. No Android lifecycle wiring (4e).

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` A3 (module rules), A4 (manual DI), A5 (persistence), A6 (networking), **A7 (sync and outbox)**, "Android spec: storage layout", Phase 4a–4e entries (find by heading; currently l.1685ff), OQ-A17, OQ-A40 | Scope and decisions |
| Agent rules | `AGENTS.md` "Working rules", "Performance" (no disk/network on main), "Tests", "Git", "Traps" (rethrow `CancellationException`) | How to work |
| Research | `docs/research/02-ios-architecture.md` §4.3, §4.7, **§6.1–§6.6**, §7.2, §7.3, §8 | Engine layers, outbox, epochs, retention, concurrency mapping. **Ignore its Room recommendation (§7.5): A5 chose SQLDelight.** |
| Research | `docs/research/09-rn-android-lessons.md` **§6** | RN semantics iOS ported. **Ignore §6.6's WorkManager background flush: A7/OQ-A17 say no.** |
| Research | `docs/research/07-day-to-day-settings.md` **§11** (endpoint table), §10.1 (refresh summary semantics) | Endpoint shapes; `SyncChanges` meaning |
| Earlier briefs | `docs/phases/phase-0d-contracts.md` §9 "Hand-off" (4d adds `OutboxChange`), Task 3 (`DataFailure` Kotlin shape), `ProfilePreferences`; `docs/phases/phase-1a-plan-core.md` (`CheckOffChange` already in `:core:model`, OQ-1a-6 store value types); `docs/phases/phase-0e-test-harness.md` §5 (tier filter: plain JVM tests carry no tier annotation; Robolectric needs `@Logic`) | What you build on |
| Upstream briefs (binding hand-off) | `docs/phases/phase-4a-store.md` §4.1 (layout), §4.3 (rules 1–7, **rule 6: no UPSERT/`INSERT OR REPLACE`**), Task 1 (`SyncEpoch`, `StaleSessionException`), Task 4 (`StoreFailure`, `AccountDatabase`, `JdbcAccountDriverFactory`), Task 5 (guarded writes, `observe`), Task 6 (`RecordMapping`), OQ-4a-11/-12/-13/-16 and its "Hand-off" (4d bullet); `phase-4b-api-client.md` `OutboxChange.kt` (l.126–157), `send` (Task 6, l.455ff), OQ-4b-5, OQ-4b-7 and its "Hand-off" (4d bullet, l.671); `phase-4c-mock-data-source.md` constructor (§3.2, l.135ff), check-off and remote update implementation in `MockDataSource.kt` | The exact names you build on |
| 4a–4c output | `core/database/**`, `core/datastore/**`, `core/network/**`, `core/data-mock/**`, `core/model/**` | Read before Task 1 (§4). Where code and brief differ, the code wins; note it in the commit body. |

### 2.2 iOS behaviour to mirror (read-only, at `f391a1fa`)

| iOS file | Lines | Port to |
|---|---|---|
| `Sources/AppCore/OutboxChange.swift` | 1–63 | `core/model/…/outbox/OutboxChange.kt` |
| `Sources/LocalStore/LocalMutations.swift` | 1–181 (`change` 18–36, `dependency` 38–45, `referencedTripID` 47–50, `enqueue` 55–131, `record` 133–158, `safeSettings` 160–180) | `OutboxPayload.kt` (pure), `OutboxRepository.enqueue` (DB) |
| `Sources/LocalStore/StoreApplication.swift` | 1–133 | `core/data/…/store/StoreApplication.kt` |
| `Sources/LocalStore/TripRepository.swift` | 1–140 (`applyCards` 68–103, `applyDetail` 105–123, `evictPlans` 125–139) | `core/data/…/repository/TripRepository.kt` |
| `Sources/LocalStore/{AccountRepository,SettingsRepository,OutboxRepository}.swift` | all | `core/data/…/repository/` |
| `Sources/LocalStore/SessionAccountDatabase.swift` | `upsert` 29–48 | `core/data/…/store/LoginUpsert.kt` |
| `Sources/LocalStore/SyncEpoch.swift`, `Sources/AppCore/SessionMutationPermit.swift` | all | 4a's types (check `child()` exists, §4) |
| `Sources/LocalStore/AccountDatabase.swift` | `validate` 93–97, `write(permit:)` 99–113, `write(epoch:permit:)` 115–141 | 4a's `AccountDatabase` (semantics to rely on) |
| `Sources/Sync/SyncEngine.swift` | 1–324 | `core/sync/…/SyncEngine.kt` |
| `Sources/Sync/SyncStatus.swift` | 1–43 | `SyncStatus.kt` |
| `Sources/Sync/SyncChanges.swift` | 1–70 | `SyncChanges.kt` (diff) + `SqlSyncDatabase.snapshot()` (read) |
| `Sources/Sync/FullPull.swift` | 1–104 | `FullPull.kt` |
| `Sources/Sync/OutboxFlusher.swift` | 1–151 | `OutboxFlusher.kt` |
| `Sources/Sync/OutboxFailurePolicy.swift` | 1–63 | `OutboxFailurePolicy.kt` |
| `Sources/Sync/CheckOffReconciler.swift` | 1–70 | `CheckOffReconciler.kt` + `SqlSyncDatabase.rekeyCheckOff` |
| `Sources/Sync/PlanRetention.swift` | 1–33 | `PlanRetention.kt` |
| `Sources/Sync/SyncLifecycle.swift` | 1–211 | `SyncLifecycle.kt` |
| `Sources/Sync/SessionSync.swift` | 1–206, **except `requestStart` 41–44** (needs `SessionController`, 4e) | `SessionSync.kt` |
| `Sources/DataAccess/API/APIDataSource.swift` | `send` 83–135 | verify 4b's `ApiDataSource.send` against it (present; `ApiErrorMapper` retains envelope/status mapping, and suspending `ApiClient.evictIdleConnections()` remains 4e wiring) |
| `Sources/DataAccess/Mock/MockDataSource.swift` | `send` 575–640, `fetchTrips` 462–481 (`remoteTripUpdate`), `replaceRemoteTrips` 483–490 | verify 4c's mock against it |
| `Tests/SyncTests/**` | all (table in §7) | ported tests |
| `Sources/LocalStore/AccountModel.swift` | 1–61 | `core/data/…/repository/AccountModel.kt` (moved from 4a, OQ-4a-11) |
| `Sources/LocalStore/TripRepository.swift`, `TripRepository+PlanObservation.swift` | `store(plan:checks:)`, `planValues`, `checkValues` | `TripRepository` reads/observation |
| `Tests/LocalStoreTests/RecordsTests.swift` | `allRecordsRoundTripUnknownJSONNullsAndCompositeKeys`, `repositoriesBindKeysAndOrderOutboxByTimestampThenID` (**without** their `DraftRecord`/`DraftRepository` halves: 9b) | Task 3b |
| `Tests/LocalStoreTests/RepositoryObservationTests.swift` | all six (the draft half of `everyRepositoryEmitsInitialEmptyAndCommittedValues` goes to 9b) | Task 3b |
| `plan.md` | l.53 D23, l.54 D24, l.55 D25; l.435–467 "API contract and offline sync" incl. "Sync sequence for one epoch" 1–5; l.1104–1133 Phase 9c and its review fixes; l.1134–1171 Phase 9d trigger task and review fixes; l.2327 Deferred fix "Keep pending local edits on login upsert"; l.2376ff OQ 26, 29, **30**, **31** | Decisions and the reviewed edge cases |

Quotes that bind this port (iOS `plan.md`):
- D23 (l.53): "full pull (no cursors), flush outbox, identity, account, trip cards, `synced_at`, eviction, changed plan details. One flight per session epoch; coalesce concurrent requests into one rerun, debounce ordinary requests 1 s, expose immediate `refreshNow()`. Retry transient failures at `min(1 s × 2^n, 5 min)`, respecting a larger server retry-after deadline." "a 401 from the optional walkthrough POST retains its durable row and ends that flight before the pull" "A 403 `email_unverified` switches to verify and suspends protected pulls, never wipes. Every awaited response checks epoch/account before applying."
- D24 (l.54): "A local mutation and its UUIDv7 outbox row commit together; FIFO `(created_at,id)`, dependency blocking, pending-value protection, retry and final refusal rules below. Passwords, bearer tokens, two-factor codes and link secrets are never written to outbox/DB/logs."
- Sync sequence 5 (l.465): "No toast/banner exists for sync failure in these boards: expose a typed event and defer its visual presentation (OQ 30). Unsupported kinds remain pending/blocked without hot-loop retries and cannot be produced by these phases (OQ 31)."
- OQ 31: "Preserve schema/kind decoding. Implement supported check-off/settings (including policy acceptance)/profile/walkthrough handlers now; do not create unsupported producers, fake endpoints or discard parked rows."

### 2.3 Boards

**None.** This phase has no UI. There is no board for a sync-failure toast or banner (iOS OQ 30): a refusal is stored data and a typed result only. Do not add any visible string. The pending-changes sign-out sheet is Phase 10a; the refresh capsule is Phase 6a.

### 2.4 API endpoints (base `https://api.chronoflow.net/v1`, bearer auth; web `routes/api.php` at `e36368ec` l.65–101) [F]

| Use | Request | Success → receipt |
|---|---|---|
| Identity | `GET users/me` | `SessionAccount` (= `AccountIdentity`) `{id,name,email,hasVerifiedEmail,policies{termsVersion,privacyVersion,acceptedTermsVersion?,acceptedTermsAt?},walkthroughSeen}` |
| Account | `GET account` (`verified`) | `AccountSnapshot` `{id,name,email,hasVerifiedEmail,settings{bodyClock,sleepAnswered,preferences,reminders,planCredits,policies},readiness,billing,security}` |
| Trips | `GET trips` (`verified`) | `{trips:[TripListItem{card,planInputHash,planStartUtc,planStartZone,planEndUtc,planEndDate,preparationDays,adaptation?,keepOffline,detailVersion?,checkState?}]}` |
| Detail | `GET trips/{uuid}` (`verified`) | `TripDetailResponse` (raw bytes + `{trip,plan,notices,timeline,checkState,detailVersion}`) |
| Check-off | `PUT trips/{uuid}/checks` body exactly `{"plan_input_hash","type","local_date","ordinal","status"}`, `status` = `"done"`/`"skipped"`/**explicit `null`** (undo) | `.checks(TripCheckResult{checkState, detailVersion})` |
| Settings | `PATCH account/body-clock` · `PATCH account/preferences` · `PATCH account/reminders`, body = the row's snake_case `patch` object | `.account(AccountSnapshot)` |
| Policies | `POST terms/acceptance` `{"accepted":true,"version":"<v>"}` → 204 | `.acknowledged` |
| Profile | `PATCH users/me` `{"name","email"}` (iOS encodes `ProfileChange`; email is always the unchanged local email) | `.identity(SessionAccount)` |
| Walkthrough | `POST walkthrough` (no body) → 204 | `.acknowledged` |
| Unsupported (`complete-trip`, `data-export`, `security`) | never sent | `DataFailure.Unsupported(kind)` |

These are 4b's `ApiDataSource.send` and 4c's `MockDataSource.send`. 4d does not change the network layer unless §4 finds them missing.

---

## 3. Out of scope (owner in brackets)

- `SessionController`, `SessionSync.requestStart`, `AccountWiper`, the teardown order, `StoredDataReadiness`, `AppGraph`/`SessionGraph`, `ProcessLifecycleOwner` ON_START/ON_STOP wiring, seeds [4e]. iOS tests `SessionWipeTests`, `SessionSyncReadinessTests`, `MockSessionSeedTests` and `Sources/Sync/MockSessionSeed.swift` [4e].
- `TripGenerationEffects+Store.swift` (wizard `mergeProfile`/`pullCreatedTrip`) [9b].
- Any producer UI: check-off taps [6d], body clock/preferences/reminders/profile/terms/walkthrough screens [8, 10a–10b, 12c]. Any sync-failure presentation (`RefusedChangeModel`) [6d, after a board exists].
- Pull-to-refresh UI and the capsule [6a] (this phase delivers `refreshNow()` and `SyncChanges`).
- Background sync or flush of any kind: **no WorkManager, no JobScheduler, no foreground service** (A7, OQ-A17).
- ETags (OQ-A16), cursors, idempotency headers (iOS: "no invented idempotency header").
- Producers for `complete-trip`, `data-export`, `security` (OQ 31). Enqueueing them throws.
- Schema changes. The v1 DDL is 4a's; you add **named queries** only.
- `DraftRecord`, `DraftRepository`, `drafts` queries and every draft half of a ported LocalStore test [9b].
- `SessionAccountStore`/`SessionAccountDatabase` (open/close/attach, `markEmailVerified`) [4e]. 4d delivers only the upsert body they call (Task 8).

---

## 4. What 4a, 4b and 4c hand you (check before Task 1)

These are the names the final 4a/4b/4c briefs promise. Check each in code (`grep -rn`). Where a fallback is given, apply it in the task named and say so in the commit body. If an item fails and has no fallback, stop and report.

1. **Epoch and permit** (4a Task 1, `:core:model`, package `net.chronoflow.core.model.session`). `class SyncEpoch(accountID: AccountID, generation: Long)` with `child()`, `invalidate()`, `isValid` (non-throwing), `suspend fun check()` (`ensureActive()` then `whileValid { }`), `fun <T> whileValid(operation: () -> T): T` (parent first; **not** suspend). An invalid epoch or permit throws `StaleSessionException : CancellationException`. `SessionMutationPermit` with `invalidate()` and `whileValid`. Note the spelling **`accountID`** (not `accountId`) everywhere in this brief's Kotlin.
   - `child()` is in 4a. Do not re-add it.
2. **AccountDatabase** (4a Tasks 4–5, `:core:database`, package `net.chronoflow.core.database`):
   - `AccountDatabase(root: File, namespace: StorageNamespace = Api, driverFactory: AccountDriverFactory, io: CoroutineDispatcher)`; `accountID: AccountID?`; `open(accountID: String)`; `close()`; `observationCount`.
   - `read(body: (AccountSql) -> T)`, `write(body)`, `write(permit: SessionMutationPermit?, body)`, `write(epoch: SyncEpoch, permit: SessionMutationPermit? = null, body)`, `validate(epoch)`. Bodies are **non-suspend** `(AccountSql) -> T`; `AccountSql.queries` is the generated `AccountDb`.
   - Lock order is **epoch → permit → transaction** (locks held through COMMIT, OQ-4a-1). Only the caller's cancellation invalidates the epoch.
   - `observe(vararg tables: String, fetch: (AccountSql) -> T): Flow<T>`: initial value, then one value per **committed** write to those tables, conflated, fetched on `io`, completes on `close()`. Table keys: `"account"`, `"trips"`, `"settings"`, `"outbox"`, `"plan_versions"`, `"check_marks"`.
   - `StoreFailure` (`Closed`, `AlreadyOpen`, `InvalidAccountID`, `Unavailable`, `Migration`, `StorageAttributes`) lives in `:core:database`. **`:core:sync` must throw `InvalidAccountID` (FullPull step 3) and cannot see that module** → Task 0 moves it (OQ-4d-12).
   - `JdbcAccountDriverFactory`, `TestRoots`, `Accounts`, `SqlProbe` live in `core/database/src/test` → Task 0 moves them to the new `:core:database-testing` module (OQ-4a-13, OQ-4d-13).
3. **SQL rules** (4a §4.3 rule 6, enforced by 4a's `SqlSafetyTest`): no `ON CONFLICT … DO`, no `RETURNING`, no `INSERT OR REPLACE`/`REPLACE INTO`, no window functions. **"Save" = `UPDATE … WHERE <pk>`, then `INSERT` when `changes()` is 0**, in the same transaction (a REPLACE would cascade-delete `plan_versions`). 4a's labelled queries: `selectAccount/updateAccount/insertAccount/changes`, `selectTrip/selectTrips/updateTrip/insertTrip/deleteTrip`, `selectSetting/selectSettings/updateSetting/insertSetting/deleteSetting`, `selectOutboxRow/selectPendingOutbox/updateOutboxRow/insertOutboxRow/deleteOutboxRow`. `PlanVersions.sq`/`CheckMarks.sq` hold DDL only: you add their queries.
4. **RecordMapping** (4a Task 6, `net.chronoflow.core.database.store`): extension functions on `AccountSql`: `fetchAccount/saveAccount`, `fetchTrip/fetchTrips/saveTrip/deleteTrip`, `fetchSetting/fetchSettings/saveSetting/deleteSetting`, `fetchOutboxRow/fetchPendingOutbox/saveOutboxRow/deleteOutboxRow`. Saves run `JSONDocument.requireValid` on every JSON column first. Build every repository and `StoreApplication` on these; add the plan-version and check-mark functions yourself (Task 3, OQ-4a-16).
5. **Records** (`:core:model`, `net.chronoflow.core.model.store`): 4a's `AccountRecord`, `TripRecord`, `SettingsRecord`, `OutboxRecord(id, kind: String, payload: String, createdAt, attempts = 0, lastError = null)` (JSON columns are `String`); 1a's `PlanVersionRecord(tripId, inputHash, detail: ByteArray, receivedAt, detailVersion?)` (content `equals`), `CheckMarkRecord(tripId, inputHash, type, localDate, ordinal: Int, status, markedAt)`, `TripPlanSnapshot`. `JSONDocument.requireValid(text): String`.
6. **UUIDv7** (4a Task 2, `net.chronoflow.core.model.device.UUIDv7`): `generate(millisecondsSince1970: Long, randomBytes: ByteArray): java.util.UUID`.
7. **Contracts and the outbox values** (0d, 1a, 4b; packages `net.chronoflow.core.model`, `.api` and `.contracts`): `DataSource` (25 suspend methods, including `send(change: OutboxChange): MutationReceipt`, `fetchIdentity`, `fetchAccount`, `fetchTrips`, `fetchTripDetail(id)`, `deleteAccount(password)`, `accountSecurity(change)`); 4b's **`OutboxChange`** = `CheckOff(val change: CheckOffChange)`, `Settings(val section: SettingsSection, val patch: JsonObject)`, `Policies(val version: String)`, `Profile(val change: ProfileChange)`, `Walkthrough`, `Unsupported(val kind: OutboxKind, val payload: JsonObject)`; **`MutationReceipt`** = `Acknowledged`, `Account(val snapshot: AccountSnapshot)`, `Identity(val identity: AccountIdentity)`, `Checks(val result: TripCheckResult)`; `ProfileChange(name, email)` (`@Serializable`), `SettingsSection`; 1a's `CheckOffChange(tripID, planInputHash, type: BlockType, localDate, ordinal: Int, status: CheckStatus?)`. 0d: `DataFailure` (`Offline`, `Unauthorized(status)`, `EmailUnverified(status)`, `Validation(fields, status)`, `Throttled(retryAfterSeconds: Double, status)`, `Conflict(status)`, `Server(status?)`, `Unsupported(kind)`, …; `.status`), `FieldMessage`, `OutboxKind` (raw `check-off`, `settings`, `complete-trip`, `profile`, `data-export`, `security`, `walkthrough`), `AccountID`, `AccountIdentity`/`SessionAccount`, `AccountPolicies`, `AccountSnapshot`, `TripListItem`, `TripDetailResponse`, `TripCheckResult`, `CheckState`, `CheckStatus`, `BlockType` (`Unknown(raw)`), `ProfilePreferences` (`requestFields`, `bodyClock`, `readiness`). **Use these exact names; do not create a second `OutboxChange`.**
   - Fallback: only if `send` or one of these types is missing, add it in Task 1b.
8. **Mock** (4c, `:core:data-mock`, JVM): `MockDataSource(store, seed: MockServerState?, offline, mutationsOffline, remoteTripUpdate: suspend () -> Boolean, now: () -> Instant, delay, sleep: suspend (Double) -> Unit, id: () -> String, bearer, …, io)`, `setOffline(Boolean)`, `failNext(method: MockMethod, error: DataFailure)`, `replaceRemoteTrips(trips: List<TripListItem>, details: Map<String, TripDetail>)`, `MockServerState`, `MockServerAccount`, `MemoryMockStateStore`; `send` stale hash → `Validation({"form": "This plan has changed. Refresh it before marking this step."}, 422)`. Construct it in tests with `now = { syncDate }`, `sleep = { }`, `zone = ZoneId.of("Europe/Amsterdam")`, `io = <injected test dispatcher>` (no `MockClock` needed; OQ-4c-14).
9. **Testing**: `libs.sqldelight.sqlite.driver` and `libs.sqldelight.coroutines.extensions` in the catalog (0a); `kotlinx-coroutines-test`, assertk, Turbine, mockwebserver3. 0e's `make quick` runs `testDebugUnitTest -Pcf.tier=quick` on every module; untagged local unit tests (JDBC) run in quick. Verify in Task 10 that `:core:data` and `:core:database` results appear in `build/test-results/quick/`.

---

## 5. Architecture [R]

### 5.1 Module split

| Module | Holds | Depends on |
|---|---|---|
| `:core:model` (JVM) | 4b's `OutboxChange`/`MutationReceipt` (reused), **new** `OutboxPayload` (stored form, pure), `LocalMutationFailure`, `CanonicalJson`, `StoreTime`, and `StoreFailure` (moved from `:core:database` in Task 0) | — |
| `:core:sync` (JVM) | `SyncEngine`, `SyncLifecycle`, `SessionSync`, `FullPull`, `OutboxFlusher`, `CheckOffReconciler`, `OutboxFailurePolicy`, `SyncRefusal(Notice)`, `PlanRetention`/`PlanLease`, `SyncState/Result/Event/Scheduler`, `SyncChanges`/`SyncSnapshot`, `SyncPathMonitoring`, and the **port interface `SyncDatabase`** | `:core:model` |
| `:core:database` (Android) | 4a's schema + **new named queries** (`.sq`) + plan-version/check-mark `RecordMapping` | `:core:model` |
| `:core:database-testing` (Android library, **new**, Task 0) | `JdbcAccountDriverFactory`, `TestRoots`, `Accounts`, `SqlProbe` moved from `core/database/src/test` (OQ-4a-13); consumed only through `testImplementation` | `:core:database`, `sqlite-driver` |
| `:core:data` (Android) | `SqlSyncDatabase : SyncDatabase`, `StoreApplication`, repositories, `AccountModel`, `LoginUpsert`, `ConnectivitySyncPathMonitor`; **the ported DB-bound tests** (local JVM unit tests on JDBC) | `:core:sync`, `:core:database`, `:core:model`; `testImplementation`: `:core:database-testing`, `:core:network`, `:core:data-mock`, `:core:testing`, `libs.okhttp.mockwebserver3`, `kotlinx-coroutines-test`, Turbine, assertk, Robolectric + `androidx.test.ext:junit` (Task 9) — add only what the 0a/0e convention plugin does not already add, with the catalog aliases exactly as 0a wrote them |
| `:core:testing` (JVM) | `SyncFixtures`, `ScriptedSource`, `TestGate`, `ManualSyncScheduler`, `WriteBarrier`, `eventually` (reused by 4e) | `:core:model`, `:core:plan` (already an API dependency), coroutines; sync helpers must not depend on `:core:sync` (avoid a test dependency cycle) |

Why: A3 makes `:core:sync` a JVM module, and a JVM module cannot depend on the Android `:core:database`. Every iOS `database.write(epoch:) { db in … }` closure used by the sync layer becomes one `SyncDatabase` method whose body is the same SQL, run in one transaction by `SqlSyncDatabase`. The iOS sync tests run against real SQLite, so they live in `:core:data`. (OQ-4d-1.)

### 5.2 `SyncDatabase` (port interface, `core/sync/…/SyncDatabase.kt`)

Each write method runs **one** `AccountDatabase.write(epoch)` transaction; each read runs one `read`. The iOS source of each body is given; port it line for line.

```kotlin
interface SyncDatabase {
    val accountID: AccountID?
    suspend fun validate(epoch: SyncEpoch)                                   // AccountDatabase.validate
    suspend fun account(): AccountRecord?
    suspend fun pending(): List<OutboxRecord>                                // ORDER BY created_at, id
    fun outboxValues(): Flow<List<OutboxRecord>>                             // observed, same order
    suspend fun outboxRow(id: String): OutboxRecord?
    suspend fun trip(id: String): TripRecord?
    suspend fun snapshot(): SyncSnapshot                                     // SyncChanges.swift:26–51
    suspend fun applyIdentity(epoch: SyncEpoch, identity: AccountIdentity)   // StoreApplication.identity
    suspend fun markEmailUnverified(epoch: SyncEpoch)                       // SyncEngine.swift:252–256
    suspend fun applyPull(epoch: SyncEpoch, account: AccountSnapshot, cards: List<TripListItem>,
                          at: String, retention: PlanRetention)              // FullPull.swift:50–55
    suspend fun detailNeeded(item: TripListItem, retention: PlanRetention): Boolean // FullPull.swift:59–69
    suspend fun applyDetail(epoch: SyncEpoch, response: TripDetailResponse, tripId: String, at: String)
    suspend fun applyReceipt(epoch: SyncEpoch, receipt: MutationReceipt, row: OutboxRecord)
    suspend fun refuse(epoch: SyncEpoch, row: OutboxRecord, refusal: SyncRefusal) // OutboxFlusher.swift:100–108
    suspend fun recordTransientFailure(epoch: SyncEpoch, rowId: String, lastError: String): Int // :111–116, returns attempts
    suspend fun deleteOutboxRow(epoch: SyncEpoch, id: String)               // CheckOffReconciler.swift:15–17
    suspend fun rekeyCheckOff(epoch: SyncEpoch, row: OutboxRecord, change: CheckOffChange): OutboxRecord? // :23–68
}
fun interface ActiveSyncDatabase { suspend fun activeDatabase(): SyncDatabase? }  // SessionAccountDatabase.activeDatabase
```

### 5.3 Concurrency mapping (port of `02 §8`)

- **Swift `actor` → class whose mutable state is touched only on a private `limitedParallelism(1)` view of an injected dispatcher** (`dispatchers.default` in production). Every public suspend method is `withContext(confined) { … }`. This keeps Swift actor **reentrancy**: state may change at every suspension point, so re-check exactly where the Swift code re-checks. **Never hold a `Mutex` across a suspension** (the engine awaits its own flight; a held mutex deadlocks).
- `Task { }` stored → `Job`/`Deferred` launched in a `CoroutineScope(SupervisorJob() + confined)` owned by the object. The flight is a `Deferred<SyncResult>` in that scope, **not** a child of the caller.
- `try Task.checkCancellation()` → `currentCoroutineContext().ensureActive()`. `CancellationError` → `CancellationException`. Every `catch` that maps failures first does `catch (e: CancellationException) { throw e }` (AGENTS Traps; never `runCatching`).
- `withTaskCancellationHandler(onCancel:)` → `try { … } catch (e: CancellationException) { onCancel(); throw e }`.
- `AsyncStream` per observer → one `Channel` per observer, closed on stop; `events(): Flow<SyncEvent>` = `channel.receiveAsFlow()`. Engine: `Channel(32, BufferOverflow.DROP_OLDEST)`; `SessionSync`: `Channel(Channel.CONFLATED)` with the latest event replayed on subscribe.
- Timers: the debounce and retry timers are `Job`s calling `scheduler.sleep(seconds)`. **Do not use `Flow.debounce`.** A cancelled timer is kept until it has completed so `cancelAndWait`/`stop` can `join` it (iOS `canceledTimers`).
- Time: `class SyncScheduler(val now: () -> Instant, val sleep: suspend (Double) -> Unit)` with **no defaults** in `:core:sync` (1a's Konsist clock rule). `:core:data` provides `fun SyncScheduler.Companion.system() = SyncScheduler({ Instant.now() }, { delay((it * 1000).toLong()) })`. All delays and retry values are `Double` seconds, as `DataFailure.Throttled.retryAfterSeconds`.
- No disk or network on the main thread: DB work runs on 4a's writer/IO dispatchers, the engine on `default`.

### 5.4 Stored-form rules (Task 1)

- **JSON text** in DB columns is written by `CanonicalJson.encode(element: JsonElement): String`: keys sorted recursively by `String.compareTo`, no whitespace, explicit `null` kept where the element holds `JsonNull`. Byte parity with iOS DB files is not required; determinism is.
- **Times** (`StoreTime`, `:core:model`): `millis(instant)` → `yyyy-MM-ddTHH:mm:ss.SSSZ`, **always three fraction digits** (iOS `.iso8601(timeZone: .gmt, includingFractionalSeconds: true)`), used for `outbox.created_at` and `check_marks.marked_at` of local marks. `seconds(instant)` → `yyyy-MM-ddTHH:mm:ssZ` (Swift `ISO8601Format()`), used for `synced_at`, `received_at`, `deleted_at` (tombstones), `SyncRefusal.failedAt`. Build both by hand from UTC fields (no `DateTimeFormatter.ISO_INSTANT`, which drops a zero fraction). Golden: `Instant.ofEpochSecond(1_800_000_000)` → `2027-01-15T08:00:00.000Z` and `2027-01-15T08:00:00Z`.
  - Why it matters: FIFO is a **string** order on `created_at`. `"…:00Z"` sorts after `"…:00.001Z"`, so a missing fraction reorders rows.
- **Outbox payloads** (sorted keys; iOS `LocalMutations.swift:133–158`). `OutboxPayload` builds and parses every payload **by hand** with `buildJsonObject`/`JsonObject` reads. It does not rely on `@Serializable` on `CheckOffChange` (the landed class is not annotated; keep it unchanged, OQ-4d-15):

  | Change | `kind` | `payload` |
  |---|---|---|
  | `CheckOff(c)` | `check-off` | `{"local_date","ordinal","plan_input_hash","status"?,"tripID","type"}`; **`status` omitted when null** (Swift synthesized `encodeIfPresent`; OQ-4d-3) |
  | `Settings(section, patch)` | `settings` | `{"patch":{<snake keys>},"section":"bodyClock"\|"preferences"\|"reminders"}` |
  | `Policies(version)` | `settings` | `{"section":"policies","version":"<v>"}` |
  | `Profile(p)` | `profile` | `{"email","name","previousName"}` (`previousName` added by `enqueue`) |
  | `Walkthrough` | `walkthrough` | `{}` |
  | `Unsupported(kind, payload)` | its kind | never written by Android; decoded from legacy rows |

- **Decoding** a row (`OutboxPayload.change(record)`, `LocalMutations.swift:18–36`): unknown kind → `LocalMutationFailure.InvalidPayload`; `settings` with `section == "policies"` and a string `version` → `Policies`; other sections must be a `SettingsSection` with an object `patch`; `complete-trip`/`data-export`/`security` → `Unsupported(kind, payloadObject)`.
- **Dependency key** (`:38–45`): `settings` → `"settings:" + (payload.section ?: "unknown")`; `check-off` and `complete-trip` → `"trip:" + (payload.tripID ?: payload.tripId ?: "unknown")`; else the kind. `referencedTripId` = `tripID ?: tripId` (both spellings, for legacy rows).

---

## 6. Tasks

One task = one commit (Conventional Commits, lower-case imperative, no trailer). Tick the matching `plan.md` Phase 4d checkbox in the commit named.

### Task 0 — `refactor(database): share the store failure and the jdbc test driver`

Mechanical moves only; no behaviour change. 4a's tests must pass unchanged apart from imports.
- **`StoreFailure`** (OQ-4d-12): `git mv core/database/src/main/kotlin/net/chronoflow/core/database/StoreFailure.kt core/model/src/main/kotlin/net/chronoflow/core/model/store/StoreFailure.kt`; package `net.chronoflow.core.model.store`; same cases, same `Exception(null, cause)` constructor, message stays null. Update every import (`grep -rln "core.database.StoreFailure" core app feature`). Skip if 4a already put it in `:core:model`.
- **`:core:database-testing`** (OQ-4a-13, OQ-4d-13): new module `core/database-testing` (`chronoflow.android.library`, namespace `net.chronoflow.core.database.testing`, distinct per `android.uniquePackageNames`), registered in `settings.gradle.kts`. `git mv` 4a's `core/database/src/test/kotlin/net/chronoflow/core/database/testing/{JdbcAccountDriverFactory,TestRoots,Accounts,SqlProbe}.kt` into its `src/main/kotlin/net/chronoflow/core/database/testing/` (same package, so 4a's tests keep their imports). `SqlProbe` needs `AccountSql.driver`, which is public; `JdbcAccountDriverFactory` needs `AccountDb.Schema` (public, generated). Dependencies: `api(project(":core:database"))`, `implementation(libs.sqldelight.sqlite.driver)`. `:core:database` and `:core:data` add `testImplementation(project(":core:database-testing"))`. The move also requires the public `SqlDriver.checkConfiguration()` helper (its body is unchanged; refreshed before Task 0). No `implementation`/`api` consumer anywhere: add Konsist rule `databaseTestingIsTestOnly` (no file outside `core/database-testing` whose `sourceSetOf` is not a test source set imports `net.chronoflow.core.database.testing`), following 0a §6b exactly: a function in `KonsistRules.kt`, tests `databaseTestingIsTestOnly()` and `databaseTestingIsTestOnlyRejectsPlantedViolation()` in `KonsistRulesTest.kt`, planted file `konsist/src/test/resources/violations/databaseTestingIsTestOnly/core/data/src/main/kotlin/net/chronoflow/core/data/UsesJdbc.kt` importing `net.chronoflow.core.database.testing.JdbcAccountDriverFactory`.
- Run `make targeted ONLY="core:database:SchemaParityTest core:database:AccountDatabaseTests core:database:RecordMappingTests"` and the remaining database test classes through `make targeted ONLY=…` (including `SqlSafetyTest`).

### Task 1 — `feat(model): store outbox changes`

4b already defines `OutboxChange`, `MutationReceipt`, `ProfileChange`, `SettingsSection` in `core/model/…/OutboxChange.kt` (package `net.chronoflow.core.model`). **Reuse them unchanged** (property names `change`, `snapshot`, `identity`, `result`). This task adds only the stored form.

Files (`core/model/src/main/kotlin/net/chronoflow/core/model/`):
- `store/OutboxPayload.kt`: `object OutboxPayload { fun record(change: OutboxChange, id: String, createdAt: String): OutboxRecord; fun change(record: OutboxRecord): OutboxChange; fun dependency(record: OutboxRecord): String; fun referencedTripId(record: OutboxRecord): String?; fun safeSettings(patch: JsonObject, section: SettingsSection): Boolean }` and `sealed class LocalMutationFailure : Exception(null, null, false, false) { data object Unsupported; data object InvalidPayload; data object MissingAccount; data object MissingTrip; data object OnlineOnly }` (each case extends `LocalMutationFailure()`).
  - Payloads per §5.4, built by hand, written with `CanonicalJson.encode`.
  - `record` throws `InvalidPayload` for an empty settings patch or one failing `safeSettings`, and `Unsupported` for `OutboxChange.Unsupported`.
  - `safeSettings` (`LocalMutations.swift:160–180`): allowed keys — preferences `time_format`, `hidden_categories`, `show_optional_items`; reminders `light`, `avoid`, `caffeine`, `winddown`, `lead_min`; bodyClock `ProfilePreferences.requestFields` (20 keys). Every key must be allowed; `exclude_caffeine` and `flight_movement_breaks` must be JSON booleans; no value may be an object; an array must contain only strings.
  - `change` decodes per §5.4 "Decoding"; a malformed payload (not a JSON object, wrong field types, unknown `status` raw for a check-off) → `InvalidPayload`. `BlockType` decodes through 0d's serializer so an unknown raw becomes `BlockType.Unknown(raw)` (then the payload is still valid; iOS decodes unknown block types too).
- `store/CanonicalJson.kt`, `store/StoreTime.kt` (§5.4).

Tests (`core/model/src/test/kotlin/net/chronoflow/core/model/store/`, plain JVM):
- `inputWireKeysAndOutboxRoundTripsOutbox`, added to 0d's existing `ContractsTests` class in `:core:model` (`grep -rln "class ContractsTests" core/model/src/test`; replace 0d's one-line "lands with 4d" comment; Swift name + `Outbox` suffix, 0d left this half): each of `Walkthrough`, `Profile(ProfileChange("Test","test@example.com"))`, `Policies("1.0")`, `Settings(Reminders, {"light":true})`, `CheckOff(CheckOffChange(tripID "test", planInputHash "fake", Sleep, "2026-11-10", 1, status null))` survives `change(record(it, "id", "now"))`; a row `kind = "complete-trip"`, payload `{"future":null}` decodes to `Unsupported(CompleteTrip, {"future":null})`.
- `OutboxPayloadTests` (Android): exact payload strings for all five kinds, e.g. `{"local_date":"2026-11-10","ordinal":1,"plan_input_hash":"fake","tripID":"test","type":"sleep"}` for the null-status check-off and `{"patch":{"light":true},"section":"reminders"}`; dependency keys for each kind incl. a legacy `{"tripId":"x"}` check-off row → `trip:x` and a settings row without section → `settings:unknown`; `safeSettings` rejects `password`, `token`, `recovery_code`, `signature`, `current_password`, a nested object, a non-bool `exclude_caffeine`, an array with a number; a payload `"[]"` decodes to `InvalidPayload`.
- `StoreTimeTests` (Android): the two goldens in §5.4; `millis` of `…:00.001` and `…:00.010` keep three digits; string order equals instant order for 1,000 consecutive milliseconds.
- `CanonicalJsonTests` (Android): nested keys sorted; `JsonNull` kept; no whitespace; `1.000` from a parsed element is written as parsed (do not re-format numbers).

### Task 1b (only if §4 item 7 or 8 found a gap) — `feat(network): send outbox changes`

Add `send` to `DataSource`, `ApiDataSource` (§2.4 exactly; `SettingsPatchValidation.validate` before a settings PATCH, as iOS `APIDataSource.swift:114`) and `MockDataSource` (iOS `MockDataSource.swift:575–640`), plus `remoteTripUpdate`/`replaceRemoteTrips` on the mock. Tests: one deny-by-default MockWebServer case per kind in 4b's suite (method, path, exact body keys, `status: null` present on undo) and the mock's stale-hash `form` error. Skip this task entirely when nothing is missing.

### Task 2 — `feat(sync): add sync values, failure policy and plan retention`

Files (`core/sync/src/main/kotlin/net/chronoflow/core/sync/`):
- `SyncStatus.kt`: `enum class SyncState { Idle, Offline, VerificationRequired, Unauthorized, Failed }`; `data class SyncResult(val state, val pendingCount: Int, val retryAfter: Double? = null, val serverRetryAfter: Double? = null, val refusals: List<SyncRefusal> = emptyList(), val changes: SyncChanges = SyncChanges())`; `sealed interface SyncEvent { data class Started(val accountID: AccountID, val epoch: Long); data class Finished(val accountID: AccountID, val epoch: Long, val result: SyncResult) }` (Swift `SyncStatus.swift:28–31`: `accountID: AccountID`, the engine's own epoch, not the flight's child); `SyncScheduler` (§5.3).
- `SyncChanges.kt`: `data class SyncChanges(tripsAdded = 0, tripsUpdated = 0, tripsRemoved = 0, planVersionsChanged = 0)`; `class SyncSnapshot(trips: Map<String, Value>)` with `data class Value(val trip: TripRecord /*normalized*/, val planVersion: String?)` and `fun changes(to: SyncSnapshot): SyncChanges` (port `SyncChanges.swift:53–69` exactly: plan change = input hash differs, or both versions non-null and differ; updated = record differs or plan changed; added/removed by id).
- `OutboxFailurePolicy.kt`: `isFinal(f)` = `f !is EmailUnverified && f.status in 400..499 && f.status !in {401, 408, 425, 429}`; `sanitized(f)` → `review_expired`, `not_enough_credits`, `save_capacity`, `profile_invalid`, `offline`, `unauthorized`, `email_unverified`, `validation`, `throttled`, `conflict`, `server`, `unsupported`; `retryDelay(attempts: Int, retryAfter: Double = 0.0) = max(min(2^min(max(0, attempts), 9), 300), retryAfter)`.
- `SyncRefusal.kt`: `@Serializable data class SyncRefusal(id, kind, status: Int? /*omitted when null*/, failedAt, lastError, notice: SyncRefusalNotice? = null)`; `SyncRefusalNotice` = `SleepTimes` | `StepDone(type: BlockType)`, JSON `{"sleepTimes":{}}` / `{"stepDone":{"_0":"<type raw>"}}` (Swift synthesized enum form); `fun SyncRefusalNotice.Companion.from(change): SyncRefusalNotice?` per `OutboxFailurePolicy.swift:46–61` (body-clock patch non-empty and all keys in `habitual_bed, habitual_wake, free_day_bed, free_day_wake, free_day_status` → `SleepTimes`; check-off with status `done` and a known type → `StepDone(type)`; else null). A legacy refusal without `notice` decodes with `notice == null`.
- `PlanRetention.kt`: `class PlanRetention { fun hold(tripId): PlanLease; internal fun clear(); fun <T> withHeldPlans(action: (Set<String>) -> T): T }` (a `ReentrantLock` guarding `Map<UUID, String>`); `class PlanLease { fun release() }` idempotent. **No finalizer** (JVM has no `deinit`): the holder must call `release()` (OQ-4d-8).
- `SyncPathMonitoring.kt`: `interface SyncPathMonitoring { fun updates(): Flow<Boolean>; suspend fun stop() }`.
- `SyncDatabase.kt` (§5.2). `SyncEpoch.child()` already exists (4a).

Tests (`core/sync/src/test/…`, plain JVM, Android names): `SyncChangesDiffTests` (added/removed/updated; equal values → no change; normalization of `keepOffline` and JSON text is `SqlSyncDatabase.snapshot()`'s job and is covered by `SyncChangesTests.unchangedPullIgnoresReceiptTimes` in Task 6; hash change → updated + planVersionsChanged; version nil→v1 is not a plan change); `RetryDelayTests` (`retryDelay(0..10)` = 1, 2, 4, …, 256, 300, 300; `retryDelay(0, 900.0)` = 900); `PlanLeaseTests` (two holds of one trip keep it held until both release; double release is harmless; `clear()` empties); `SyncRefusalJsonTests` (round trip of both notices and of a null status; the legacy object `{"id":"old","kind":"settings","failedAt":"then","lastError":"validation"}` decodes with null notice — this is the second half of Swift `refusalNoticesCoverOnlyDrawnChanges`, which Task 5 ports whole).

### Task 3 — `feat(data): port the store application and outbox repository`

Files:
- `core/database/src/main/sqldelight/net/chronoflow/core/database/*.sq`: add labelled queries only (no DDL change), in 4a's naming style, **obeying 4a rule 6** (no UPSERT, no `INSERT OR REPLACE`, no `RETURNING`; 4a's `SqlSafetyTest` scans them):
  - `Outbox.sq` (4a has `selectOutboxRow`, `selectPendingOutbox`, `updateOutboxRow`, `insertOutboxRow`, `deleteOutboxRow`): add `incrementOutboxAttempts(lastError, id)` (`attempts = attempts + 1`) and `selectOutboxAttempts(id)`. The enqueue path uses 4a's **plain `insertOutboxRow`**: a duplicate id must fail and roll back the whole mutation, which `durableRestartAndRollback` relies on. `OutboxRepository.upsert` and payload rewrites use `saveOutboxRow` (update-then-insert).
  - `PlanVersions.sq`: `selectPlanVersion(tripId, inputHash)`, `selectPlanVersionsOfTrip(tripId)`, `updatePlanVersion`, `insertPlanVersion`, `deletePlanVersionsOfTrip(tripId)`, `clearDetailVersionsOfTrip(tripId)`, `clearDetailVersionIfDifferent(tripId, inputHash, detailVersion)` (exactly `UPDATE plan_versions SET detail_version = NULL WHERE trip_id = ? AND input_hash = ? AND detail_version != ?`, `StoreApplication.swift:127–129`; SQL three-valued logic is intended: a stored NULL or a NULL argument updates nothing), `selectCurrentPlanVersions` (the join in `SyncChanges.swift:31–34`).
  - `CheckMarks.sq`: `selectCheckMarks(tripId, inputHash)` (`ORDER BY type, local_date, ordinal`), `selectCheckMark(pk…)`, `updateCheckMark`, `insertCheckMark`, `deleteCheckMark(pk…)`, `deleteCheckMarksOfPlan(tripId, inputHash)`, `deleteCheckMarksOfTrip(tripId)`, `deleteCheckMarkIfNotNewer(pk…, markedAt)` (`… AND marked_at <= ?`).
  - `Account.sq`: `setSyncedAt(at, id)`, `setEmailUnverified(id)`, `setAccountName(name, id)`.
- `core/database/src/main/kotlin/net/chronoflow/core/database/store/RecordMapping.kt` (4a's file; OQ-4a-16): add `fetchPlanVersion(tripId, inputHash): PlanVersionRecord?`, `savePlanVersion(record)`, `fetchCheckMarks(tripId, inputHash): List<CheckMarkRecord>`, `saveCheckMark(record)`, `deleteCheckMark(record key)`. **`detail` is stored as strict UTF-8 text**: write `record.detail.decodeToString(throwOnInvalidSequence = true)` after `JSONDocument.requireValid`; read with `encodeToByteArray()`. The bytes are never re-encoded or normalized (1a: `detail` is what the server sent). Save = update, then insert when `changes()` is 0.
- `core/data/…/store/StoreApplication.kt`: `internal object StoreApplication` with `pending`, `identity`, `snapshot`, `setting`, `checks`, `setMark`, `refuse`, `receipt`, each taking the transaction's `AccountSql` (4a; called only inside `AccountDatabase.write`), ported from `StoreApplication.swift:4–132` line for line:
  - `identity`: id must equal the account (`StoreFailure.InvalidAccountID`); insert-or-load the account; **keep name and email when any `profile` row is pending**; `hasVerifiedEmail` from the server; `walkthroughSeen = server || a pending walkthrough row`; then `setting("policies", identity.policies)`.
  - `snapshot`: id check; account must exist (`MissingAccount`); same name/email rule; write sections `bodyClock`, `preferences`, `reminders`, `planCredits`, `policies`, `sleepAnswered`, `billing`, `security` through `setting`; if a `settings:bodyClock` row is pending delete `readiness`, else `setting("readiness", snapshot.readiness)`.
  - `setting(section, value)`: for `policies` with a pending `Policies(version)` row, write the server value with `acceptedTermsVersion = version` and `acceptedTermsAt = <latest pending row createdAt>`; otherwise, **skip if any row with dependency `settings:<section>` is pending**; else write (`bodyClock` through `ProfilePreferences.bodyClock`, `readiness` through `ProfilePreferences.readiness(value, storedBodyClock)`).
  - `checks(state, tripId)`: **skip entirely while any `check-off` row for that trip is pending**; else delete the marks of `(trip, state.planInputHash)` and insert the state's marks.
  - `setMark(change, at)`: status non-null → save the mark (update, then insert) with `marked_at = at`; null → delete that mark.
  - `refuse(row)`: delete the row; for `profile`, take `previousName` and hand it to the next pending profile row's payload, or, if none, `UPDATE account SET name = previousName`.
  - `receipt(receipt, row)`: delete the row; `Account` → `snapshot`; `Identity` → `identity`; `Checks` → the row's `referencedTripId` (null → `LocalMutationFailure.InvalidPayload`), `checks(result.checkState, tripId)`, then `clearDetailVersionIfDifferent(tripId, checkState.planInputHash, result.detailVersion)` (`StoreApplication.swift:124–130`).
- `core/data/…/repository/OutboxRepository.kt`: `pending()`, `values(): Flow`, `upsert(record)`, `remove(id)`, and `enqueue(change, permit: SessionMutationPermit? = null, now: Instant = Instant.now(), randomBytes: ByteArray = ByteArray(10).also { SecureRandom().nextBytes(it) }): OutboxRecord` — port of `LocalMutations.swift:55–131` (`Instant.now()` is allowed here: the clock rule covers `:core:sync`, not `:core:data`):
  - `ms = now.toEpochMilli()`; `ms < 0` or `ms >= 1L shl 48` → `InvalidPayload` (iOS l.59–62); id = `UUIDv7.generate(ms, randomBytes).toString().lowercase()`; `createdAt = StoreTime.millis(now)`; build the row with `OutboxPayload.record` **before** the transaction; `database.accountID == null` → `StoreFailure.Closed` (iOS l.67).
  - In one `write(permit)` transaction: account must exist (`MissingAccount`); then per kind:
    - CheckOff: trip exists, not `deleted_locally`, `deleted_at` null (`MissingTrip`); `setMark(change, createdAt)`.
    - Settings: merge each snake key into the stored section as camelCase (`time_format` → `timeFormat`), removing it from `invalidDrafts`; for bodyClock, store `invalidDrafts`, sanitize with `ProfilePreferences.bodyClock`, set `sleepAnswered = true` when the patch has `habitual_bed` or `habitual_wake`, and delete `readiness`.
    - Policies: stored `policies.termsVersion` must equal `version` and `version` must be non-empty (`InvalidPayload`); set `acceptedTermsVersion` and `acceptedTermsAt` (= an equal pending row's `createdAt`, else this `createdAt`); **return the existing pending row instead of inserting** when one exists.
    - Profile: `email.lowercase() == account.email.lowercase()` else `OnlineOnly`; add `previousName = account.name` to the payload; set `account.name`.
    - Walkthrough: set `walkthroughSeen`; **return the existing pending walkthrough row** when one exists.
    - Unsupported: `Unsupported`.
  - Insert the row; return it. No producer calls `requestSync`: the lifecycle observes the outbox (Task 7).
- `core/data/…/repository/{AccountRepository,SettingsRepository,TripRepository}.kt`: the iOS read/observe/upsert APIs (`account()`, `values()`, `upsert`; `settings()`, `values()`, `upsert` with the bodyClock/readiness sanitizing, `upsertSecurity(value, permit)`, `remove(section)`; `trips()` ordered `departure_utc, id`, `values()`, `upsert`, `remove`, `plan(tripId, inputHash)`, `checks(tripId, inputHash)`, `planValues`, `checkValues`, `store(plan, checks)`), plus the `TripRepository` companion `applyCards`, `applyDetail`, `evictPlans` (`TripRepository.swift:68–139`):
  - `applyCards(items, at)`: ids must be unique and each card must have a non-empty `id` and `departureUtc`, else `DataFailure.Server(null)` (the transaction rolls back); tombstone (`deleted_at = at`) every stored trip not in the list whose `deleted_at` is null — **an empty list tombstones everything**; for each item skip `deleted_locally` trips (never resurrect); a pending `complete-trip` row for the trip forces `card.progress.status = completed` and `canComplete = false`; write card, `adaptation`, `current_input_hash = planInputHash`, `keep_offline`, `departure_utc`, the four plan columns, `preparation_days`, `deleted_at = null`; apply `item.checkState` through `StoreApplication.checks`.
  - `applyDetail(response, tripId, at)`: with `value = response.detail` and `hash = value.plan.meta.inputHash`: `value.trip.id == tripId`, `hash` non-empty and equal to `value.checkState.planInputHash`, else `Server(null)`; silently return if the trip is missing, `deleted_locally` or tombstoned; upsert `plan_versions(trip_id, input_hash, detail = response raw bytes as UTF-8, received_at = at, detail_version)`; set `current_input_hash`; `StoreApplication.checks`.
  - `evictPlans(held)`: for each trip not referenced by a pending row (`referencedTripId`) and not held, and (`deleted_at` set or `deleted_locally` or not `keep_offline`): delete its plan versions and marks; for tombstoned/locally deleted trips also set `current_input_hash = null`, `keep_offline = false`.
- `core/data/…/sync/SqlSyncDatabase.kt`: `class SqlSyncDatabase(private val database: AccountDatabase) : SyncDatabase` implementing every method of §5.2 with the iOS bodies named there. `snapshot()` reads trips not `deleted_locally` and not tombstoned, sets `keepOffline = false`, and parses `card`, `adaptation`, `form`, `revision` to `JsonElement` before comparison (key order and whitespace are not changes). `applyPull` calls `retention.withHeldPlans { held -> evictPlans(held) }` **inside** the transaction.
- `core/testing/src/main/kotlin/net/chronoflow/core/testing/sync/`: `SyncFixtures.kt` (port `Support/SyncFixtures.swift:7–64`: `syncAccount = "019a562b-8000-7000-8000-000000000001"`, `syncOther = "…0002"`, `syncTrip = "…0010"`, `syncDate = Instant.ofEpochSecond(1_800_000_000)`, `syncPolicies = AccountPolicies("1", "1")`, `identity(id, verified)`, `snapshot(id)`, `check(hash = "hash", status = Done)`, `card(id, hash, version, keep)`, `detail(hash, version, blocks)` with the same values); `ScriptedSource.kt` (port `:94–179`; records `calls`, `maximumActiveCalls`, `changes`; `configure(...)` replaces `failures`, `failMethod`, `methodFailure`, `receipts`, `hook` exactly as Swift; every method not used by sync throws `DataFailure.Server(null)`); `TestGate.kt`, `ManualSyncScheduler.kt` (independent of `:core:sync`; expose `now`/`sleep` functions for constructing `SyncScheduler`; `sleep` registers a `CompletableDeferred`, records `requestedDelays`, removes itself on cancellation; `waitForSleep()`, `advance()`, `pendingCount`), `WriteBarrier.kt` (a hand-drained dispatcher barrier for a queued writer; no blocking latch on the test scheduler), `Eventually.kt` (drain the injected test scheduler to the next observable condition; no wall-clock polling or real-time sleeps).
- `core/data/src/test/…/sync/support/SyncStore.kt`: port of `SyncFixtures.swift:66–92` (`SyncStore(id = syncAccount)`): opens `AccountDatabase(root = temporaryStoreRoot(), namespace = StorageNamespace.Api, driverFactory = JdbcAccountDriverFactory(), io = <injected test dispatcher>)` (all from 4a / `:core:database-testing`: file-backed, WAL, foreign keys on) and `open(id)`, an `epoch = SyncEpoch(AccountID.of(id), 1)`, upserts `AccountRecord(id, "Local", "demo@example.com")` and `TripRecord(syncTrip, card(), currentInputHash = "hash", departureUtc = "2026-11-10T10:00:00Z")`; `enqueue(change, index: Byte = 0)` = `OutboxRepository.enqueue(change, now = syncDate, randomBytes = ByteArray(10) { index })`; `close()` deletes the temp dir.

Tests (`core/data/src/test/…`): `LocalMutationsTests.durableRestartAndRollback`, `LocalMutationsTests.supportedValuesAndSensitiveInputs` (port `LocalMutationsTests.swift` exactly: UUID version nibble 7; row survives close/reopen; the second check-off with the same id throws and leaves the mark `done`; secrets rejected with `InvalidPayload`; nested body-clock object rejected; a different email → `OnlineOnly`; `Unsupported` → `Unsupported`; 4 pending rows).

Tick: "Repositories".

### Task 3b — `feat(data): port repository observation and the account model`

Moved here from 4a (OQ-4a-11). Builds on Task 3's repositories and 4a's `AccountDatabase.observe`.
- Repository flows (finish them here if Task 3 left them as stubs): `AccountRepository.values(): Flow<AccountRecord?>` (`observe("account")`), `TripRepository.values(): Flow<List<TripRecord>>` (`observe("trips")`, order `departure_utc, id`), `TripRepository.planValues(tripId, inputHash): Flow<PlanVersionRecord?>` (`observe("plan_versions")`), `TripRepository.checkValues(tripId, inputHash): Flow<List<CheckMarkRecord>>` (`observe("check_marks")`), `SettingsRepository.values(): Flow<List<SettingsRecord>>`, `OutboxRepository.values(): Flow<List<OutboxRecord>>` (`created_at, id`), `TripRepository.store(plan, checks)` (one write: save the plan version, save each mark). Every flow emits the initial value (including a legitimate `null`) and completes when the database closes.
- `core/data/…/repository/AccountModel.kt` (port of `AccountModel.swift:1–61`; iOS `@MainActor @Observable` → a plain class confined to an injected `scope: CoroutineScope` whose dispatcher is `Dispatchers.Main.immediate` in production and a test dispatcher in tests):
  - `sealed interface LoadState { data object Unknown; data class Loaded(val account: AccountRecord?); data object Failed }`; `val state: StateFlow<LoadState>` (starts `Unknown`); `var epoch: Long` (read-only outside); `class AttachmentToken internal constructor(val generation: Long)` with value equality; `val attachmentToken`.
  - `suspend fun attach(repository: AccountRepository, epoch: Long, ifCurrent: AttachmentToken? = null)`: return when the caller is cancelled or `ifCurrent` is non-null and not equal to `attachmentToken`; `previous = cancelSubscription()`; set `epoch`; remember `generation`; `previous?.join()`; return if cancelled or the generation moved on; launch the subscription in `scope`: collect `repository.values()` → `Loaded(value)` while the generation is current; on normal completion (database closed) → `Unknown`; on a non-cancellation exception (`StoreFailure.Closed` at subscribe time) → `Failed`. Rethrow `CancellationException`.
  - `suspend fun detach()`: `cancelSubscription()` then join it.
  - `cancelSubscription()`: `generation += 1`; cancel and return the job; `state = Unknown`.
  - No finalizer (iOS `deinit` cancel): the owner calls `detach()` (4e `SessionGraph` teardown).
- Tests (`core/data/src/test/kotlin/net/chronoflow/core/data/repository/`, JDBC through `:core:database-testing`, `runTest(timeout = 60.seconds)`, Swift names):
  - `RecordsTests.allRecordsRoundTripUnknownJSONNullsAndCompositeKeys` (Swift l.8–67 **without** the `DraftRecord`/`DraftRepository` lines and without the last `JSONEncoder` round trip of `nullable`, unless 4a made `TripRecord` `@Serializable`): save account, trip, plan version (`detail` = the JSON's UTF-8 bytes, `detailVersion "v"`), mark, settings and outbox through `RecordMapping`; each fetches equal; `SELECT card FROM trips` equals the JSON text byte for byte; `TripRepository.store(plan, [other])` → `checks(trip, "hash") == [mark, other]`; the `nullable` trip (card `"null"`) upserts and is in `trips()`.
  - `RecordsTests.repositoriesBindKeysAndOrderOutboxByTimestampThenID` (without the draft lines): rows `b`,`a` (`createdAt "1"`) and `c` (`"0"`) → pending ids `[c, a, b]`; `remove("' OR 1=1 --")` removes nothing; `remove("a")` → `[c, b]`; settings upsert then remove → empty.
  - `RepositoryObservationTests` (6): `initialFetchIsOffMainAndValuesAppearAfterAtomicCommitOnly` ("off main" = the fetch runs on the injected `io` thread, as 4a's `ObservationTests`; then `half`→`complete` in one transaction emits only `complete`; a rolled-back write emits nothing; close completes the flow), `cancellationAndCloseEndSubscriptionsBeforeAnotherAccountOpens` (`observationCount == 0`; a second database for `accountB` sees no trips), `accountModelSeparatesUnknownEmptyAndLoadedAndClearsOnDetach`, `everyRepositoryEmitsInitialEmptyAndCommittedValues` (without drafts), `closingAnAttachedDatabaseClearsTheLoadedAccount`, `modelRejectsOlderAccountAfterReplacementAndSurfacesStoreFailure` (attaching to a closed database ends in `Failed`). Wait for model states with `eventually { model.state.value == … }`, never a sleep.

Tick: nothing (the "Repositories" box is ticked in Task 3).

### Task 4 — `feat(sync): port the full pull`

`core/sync/…/FullPull.kt`: `class FullPull(database: SyncDatabase, source: DataSource, retention: PlanRetention = PlanRetention(), now: () -> Instant)` with `suspend fun run(epoch): FullPullResult(changes, verificationRequired, retryAfter: Double?)` and `suspend fun fetchDetail(tripId, epoch)`. Port `FullPull.swift:28–103` step for step, with `epoch.check()` after **every** await:
1. `validate(epoch)`; `before = snapshot()`.
2. `fetchIdentity()` → `applyIdentity`. Unverified → `verificationRequired = true`, return (no `synced_at`).
3. `fetchAccount()`: `account.id != epoch.accountID.rawValue` → `StoreFailure.InvalidAccountID` (moved to `:core:model` in Task 0); `!hasVerifiedEmail` → `DataFailure.EmailUnverified(403)`.
4. `fetchTrips()`; `at = StoreTime.seconds(now())`; `applyPull(epoch, account, cards, at, retention)` (snapshot + cards + `synced_at = at` + eviction, **one transaction**).
5. For each card in list order: if `detailNeeded` (trip exists, not deleted, `keepOffline` or held, and the stored `(trip, planInputHash)` version is missing or its `detail_version != item.detailVersion`) → `fetchDetail`. `Unauthorized`, `EmailUnverified`, `Offline`, `Throttled` → rethrow; `Server(404)` → skip (no tombstone, no retry); any other failure (including non-`DataFailure`) → `retryAfter = 1.0`, continue.
6. `changes = before.changes(snapshot())`.

`fetchDetail`: `validate`, `fetchTripDetail(id)`, `applyDetail(epoch, response, id, StoreTime.seconds(now()))`.

Tests (`core/data/src/test/…`, same names, port the Swift bodies):
- `FullPullTests`: `orderMarkerAndDetailVersion` (calls `identity, account, trips, detail`; `syncedAt == "2027-01-15T08:00:00Z"`; unchanged version → no second detail; `v2` → refetch), `unverifiedStopsBeforeProtectedPulls`, `accountAndCardsRollbackTogether` (empty `departureUtc` → throws; `syncedAt` null; settings sections == `["policies"]`; trip not tombstoned), `markerSurvivesPartialAndOfflineFailure`, `emptyTombstonesAndNoLocalResurrection`, `detail404KeepsListedCardAndQueuedCheckOffWithoutRetry`.
- `PendingProtectionTests`: `protectLocalValuesEvenWhenQueuedDuringPull` (rows enqueued from the `trips` hook — i.e. between the account fetch and the transaction — still win: name `Edited`, walkthrough seen, no `readiness`, `habitualBed` `22:00`, `timeFormat` `12`, `acceptedTermsVersion` `new`, mark `done`), `queuedCompletionPreservedFromLegacyRow`.
- `PlanRetentionTests`: `leaseReferenceCountsAndPendingPreventEviction`, `pendingDeletedTripKeepsPlan`.

### Task 5 — `feat(sync): port the outbox flusher and check-off reconciler`

- `core/sync/…/CheckOffReconciler.kt` (`CheckOffReconciler.swift:9–69`): `suspend fun prepare(row, epoch, refresh = false): OutboxRecord?`. Non-check-off rows pass through. Missing or deleted trip → `deleteOutboxRow`, return null. Not refreshing and `trip.currentInputHash == change.planInputHash` → return the row. Otherwise `pull.fetchDetail(tripId)` then `rekeyCheckOff`, whose transaction body is:
  - reload the row, the trip and its current plan version (missing → `DataFailure.Server(null)`, which the flusher treats as transient);
  - decode the detail with 0d's `TripDetailResponse.decode(plan.detail).detail` (a decode failure is a non-`DataFailure`, which the flusher maps to transient `Server(null)`); if no block in `plan.blocks` is `actionable` with `checkKey == (type, localDate, ordinal)` → delete the row, delete that mark under the **old** hash if `marked_at <= row.createdAt`, return null;
  - rewrite the payload with the current hash; if the hash changed reset `attempts = 0`, `lastError = null`; save;
  - set the mark under the new hash with `marked_at = row.createdAt` **only if** no newer pending check-off for the same `(trip, type, localDate, ordinal)` exists (newer = later `(createdAt, id)`) and the stored mark is absent or not newer than `row.createdAt`.
- `core/sync/…/OutboxFlusher.kt` (`OutboxFlusher.swift:38–150`): `class OutboxFlusher(database, source, now, prepareCheck?, refreshCheck?)`, `suspend fun run(epoch): OutboxFlushResult(refusals, retryAfter, serverRetryAfter, parked, offline, deferredWalkthroughAuthorization)`, `companion fun isStale(f) = f is Conflict || (f is Validation && ("plan_input_hash" in f.fields || f.fields["form"]?.key == "This plan has changed. Refresh it before marking this step."))`.
  - `validate`; `rows = pending()`; `blocked = mutableSetOf<String>()`. For each row in order: skip if its dependency is blocked; **re-read it by id** (skip if gone).
  - `Unsupported` → `parked += 1`, block its dependency, continue (never sent, never deleted).
  - `check-off` → `prepareCheck` (null → continue).
  - Up to two attempts: `send` → `applyReceipt` in one transaction → break. A stale failure on a check-off: on the second attempt rethrow; else `refreshCheck` (null → break) and retry.
  - On failure (after `CancellationException` is rethrown and `epoch.check()`): non-`DataFailure` → `Server(null)`. `Unauthorized` on a non-walkthrough row → throw. `EmailUnverified` → throw. `Unsupported` → park and block.
  - **Final**: `isFinal(f) && kind != walkthrough && !(check-off && isStale(f))` → `SyncRefusal(id, kind, f.status, StoreTime.seconds(now()), sanitized(f), SyncRefusalNotice.from(change))`; `refuse(epoch, row, refusal)` writes `settings.syncFailure`, calls `StoreApplication.refuse`, and for a check-off with a notice nulls `detail_version` of every plan version of that trip (the next pull refetches and restores the server's marks: that is the "undo"). Append to `refusals`.
  - **Transient**: `attempts = recordTransientFailure(epoch, row.id, sanitized(f))`; `delay = retryDelay(attempts − 1, f.retryAfterSeconds if Throttled else 0)`; `serverRetryAfter = max(…)` for throttled; `retryAfter = max(…)`; block the dependency. `Unauthorized` (walkthrough only) → `deferredWalkthroughAuthorization = true`, **return now**. `Offline` → `offline = true`, **return now**.

Tests (`core/data/src/test/…`, Swift names). **Test naming** (0d §4.2, OQ-4d-18): a Swift `@Test(arguments:)` with ≤ 5 rows becomes one plain method per row named `<swiftName><Row>` (camel-case row label), each a one-line call to a private helper; a longer list becomes its own `@RunWith(Parameterized::class)` class holding only that test, whose method keeps the Swift name, with `@Parameters(name = "{0}")` and a readable first column. Never loop over rows inside one `@Test`.
- `OutboxFlusherTests`: `refusalNoticesCoverOnlyDrawnChanges`; `fifoDependencyBlockingAndParkedKinds` (3 parked kinds with the first row's `createdAt`; first send fails 500 → its `preferences` twin is blocked, walkthrough still sent; `changes.size == 2`; `retryAfter == 1.0`; `parked == 3`; 5 rows left; first row `attempts 1`, `lastError "server"`); the two 11-row tables of `OutboxFlusherTests.swift:61–107` as their own Parameterized classes `OutboxFlusherFailureClassificationTests.failureClassification` and `OutboxFlusherPolicyClassificationTests.policyClassification` (row labels `server400`, `validation422`, `server403`, `server404`, `server408`, `server425`, `throttled429`, `server500`, `offline`, `unauthorized401`, `emailUnverified403`), with the exact table (400, 422 validation, 403 server, 404 → final, 0 attempts; 408, 425 → `server`, 429 → `throttled` with `retryAfter 900.0`, 500 → `server`, offline → `offline`, each 1 attempt; 401 and `email_unverified` → thrown, row kept with 0 attempts; the setting `syncFailure` exists iff final; no setting contains `"secret response"`); `responseRollbackReplaysAbsoluteWrite` (an `Identity(syncOther)` receipt fails the transaction → row kept, `retryAfter != null`; second run succeeds; 2 sends); `repeatedProfileRefusalRestoresLastAcceptedName` (two refused profile rows → name `Local`).
- `CheckOffReconcilerTests`: `stableRekeyOrDisappearedStepTrue`/`…False`, `newerLocalClearSurvivesOldHashRekeyAndReceipt`, `receiptCannotReplaceNewerMarkQueuedWhileSending`, `staleResponseRefreshesOnceAndThenDefersConflict409`/`…PlanInputHash422`/`…Form422` (calls `send, detail, send`; `retryAfter 1.0` then `2.0`; attempts 1 then 2; `lastError` `conflict` or `validation`; no `syncFailure`), `nonStaleValidationRefusesCheckOff` (one send; refusal status 422, notice `StepDone(Sleep)`; `detailVersion` null; the next pull refetches and clears the mark), `malformedDetailCannotProveStepDisappeared` (`Server(200)` on detail → retained, nothing sent), `detail404ResolvesRefusalWithoutSpin` (refusal, `retryAfter == null`, row gone), `transientCheckOffFailuresKeepTheOptimisticMarkQueuedOffline`/`…Throttled`/`…Server408`.

Tick: "Outbox".

### Task 6 — `feat(sync): port the sync engine`

`core/sync/…/SyncEngine.kt`, port of `SyncEngine.swift:1–324`:

```kotlin
class SyncEngine(
    database: SyncDatabase, source: DataSource, epoch: SyncEpoch,
    val retention: PlanRetention = PlanRetention(), scheduler: SyncScheduler,
    isOnline: suspend () -> Boolean = { true }, dispatcher: CoroutineDispatcher,
) {
    fun events(): Flow<SyncEvent>
    suspend fun requestSync()
    suspend fun refreshNow(): SyncResult
    suspend fun fetchDetail(tripId: String)
    suspend fun deleteAccount(password: String)
    suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult
    suspend fun cancelAndWait()
    internal val waitingRefreshCount: Int   // test hook, as Swift `waitingRefreshCount`
}
```

Behaviour (exact):
- `requestSync()`: stopped → return. A flight is running → `rerun = true`, return. Else cancel the timer and start a new one: `scheduler.sleep(1.0)`, then, if its id is still current and not stopped, `refreshNow()` (errors ignored).
- `refreshNow()`: `ensureActive`; `epoch.check()`; stopped → `CancellationException`; `database.validate(epoch)` (a wrong account throws `StoreFailure.InvalidAccountID` **before any source call**); re-check; cancel the timer. Join the running flight (`rerun = true`) or start one under `runEpoch = epoch.child()` with fresh `RefreshWaiters`. Register a waiter id; await; on caller cancellation remove the waiter and, **only if it was the last**, invalidate `runEpoch` and cancel the flight. After the await: `ensureActive`, `epoch.check()`.
- `runFlights(runEpoch)` (finally: clear flight, flightEpoch, waiters, `rerun = false`): `before = snapshot()`; `runEpoch.check()`; emit `Started(epoch.accountID, epoch.generation)`; loop `do { rerun = false; result = runOnce(runEpoch); after = snapshot(); merge retryAfter (max), serverRetryAfter (max), refusals (append); break on Unauthorized or VerificationRequired } while (rerun)`; the merged result takes the **last** run's `state` and `pendingCount` and `changes = before.changes(after)`; emit `Finished`; return it.
- `runOnce(epoch)` (`:200–272`): `isOnline()`; if online: when the local account has `hasVerifiedEmail == false`, `fetchIdentity` → `applyIdentity`, and still unverified → return `VerificationRequired` with the pending count; `OutboxFlusher.run` → refusals/retry values; `offline` → `Offline`; `deferredWalkthroughAuthorization` → `Failed`; else `FullPull.run` (`verificationRequired` → `VerificationRequired`; merge its `retryAfter`). Not online → `Offline`. Errors: `Offline` → `Offline`; `Unauthorized` → `Unauthorized`; `EmailUnverified` → `VerificationRequired` and `markEmailUnverified(epoch)`; `Throttled(s)` → `Failed`, `retryAfter = serverRetryAfter = max(…, s)`; anything else → `Failed`, `retryAfter = max(…, 1.0)`. Always finish with `pendingCount = pending().size`.
- `emit`: only when not stopped and the epoch is valid.
- `fetchDetail(tripId)`: under `requestEpoch = epoch.child()`, tracked in `detailTasks`; caller cancellation invalidates it and cancels the task; after it: `ensureActive`, `requestEpoch.check()`.
- `deleteAccount`/`accountSecurity`: `epoch.check()`, stopped → `CancellationException`, call the source, check again (`SyncEngine.swift:50–65`).
- `cancelAndWait()`: `stopped = true`; invalidate and cancel detail tasks; cancel the timer; invalidate `flightEpoch`, cancel the flight and **join** it; join the cancelled timers and detail tasks; drop the source; close every observer channel.

Tests (`core/data/src/test/…`, Swift names, bodies from `SyncEngineTests.swift`): `concurrentRefreshesCoalesceOneRerun` (identity calls in 2..3, `maximumActiveCalls == 1`), `cancellingOneWaiterPreservesSharedFlightCancelStarter`/`…CancelJoiner`, `queuedWriteDuringPullRunsAgainAndFlushes`, `queuedCheckOffAfterDetailFailureRunsAgain` (exact call list `identity, account, trips, detail, send, identity, account, trips, detail`; `retryAfter 1.0`; state `Idle`), `nonIdleFlightHonorsJoinedRefreshOffline`/`…Server500`, `protectedFailureStopsCoalescedRerunUnauthorized`/`…EmailUnverified` (calls == `[identity]`), `coalescedResultKeepsMaximumRetryAndRefusals900Then1`/`…1Then900` (`retryAfter 900.0`, 1 pending, 1 refusal, 3 sends, exactly one `Finished` event equal to the result), `invalidationRejectsEveryLateResponse{Send,Identity,Account,Trips,Detail}` (one event only; nothing written), `callerCancellationDrainsLateResponse{…}` × 5, `tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWriteFalse`/`…True`, `cancelAndWaitStopsWorkAndPreservesQueue`, `wrongAccountAndPreCancelledRequestNeverCallSource`, `firstPullThenOfflineRestartAndAccountIsolation`, `sanitizedResultsRetainTransientWork{Unauthorized,EmailUnverified,Offline,Server500}`, `debounceWithInjectedSchedulerAndRefreshBypass` (`requestedDelays == [1.0]`, one identity call after `advance()`), `cancelledEpochCannotEnterQueuedResponseTransaction` (`WriteBarrier`), `cancelDrainsDebounceWithoutFiring`.
`SyncChangesTests` (6 Swift methods, 7 cases, with 4c's `MockDataSource` over an in-memory state seeded like `SyncChangesTests.swift:10–26`): `unchangedPullIgnoresReceiptTimes`, `mockRefreshSwitchChangesServerTripAndRevisionOnce` (`SyncChanges(tripsUpdated = 1, planVersionsChanged = 1)` once; flight number `CF123`; detail version `refresh-CF123`), `oneTripUpdatedAndThenRemovedOnMockServer`, `planHashOrDetailVersionCountsAsOneUpdatedTripFalse`/`…True`, `downloadingOrEvictingAnUnchangedPlanIsNotAnUpdate`, `joinedRefreshCountsAChangedTripOnceAcrossReruns`.
`DataSourceIntegrationTests`: `mockServerAbsoluteReplayAndOfflineRestart`; `apiSourcePullAndOutboxThroughDenyByDefaultProtocol` with mockwebserver3 and 4b's `ApiDataSource(ApiClient(baseUrl = ApiBaseUrl.parse(server.url("/v1").toString(), release = false), bearer = { "fixture-bearer" }, io = Dispatchers.IO))`. 4b's `MockApi` rule lives in `core/network/src/test/kotlin/net/chronoflow/core/network/support/MockApi.kt` (package `net.chronoflow.core.network.support`) and is **not** visible from `:core:data`: write a private deny-by-default `mockwebserver3.Dispatcher` in this test (exact `(method, path)` routes answering the encoded `identity()`, `snapshot()`, `{"trips":[card()]}` and `detail()` fixtures plus `204` for the walkthrough; anything else is recorded as unmatched, answered 599, and fails the test). Use the 5.5.1 API names (check the sources jar, 4b note). Expectations: exact request list `POST /v1/walkthrough` (204), `GET /v1/users/me`, `GET /v1/account`, `GET /v1/trips`, `GET /v1/trips/<syncTrip>`; every request has `Authorization: Bearer fixture-bearer`; `syncedAt` set; outbox empty).

Test harness rules for every engine test: `runTest(timeout = 60.seconds)`; the engine and DB use injected test dispatchers sharing the test scheduler; all sleeps go through `ManualSyncScheduler` (or `SyncScheduler({ syncDate }, { delay(…) })` where no timer fires); wait with `TestGate`, event gates and test-scheduler advancement (`waitingRefreshCount` is internal to `:core:sync` and is not exposed to `:core:data` tests); no `Thread.sleep`, no real-time `delay` in a test body. Each test < 1 s.

### Task 7 — `feat(sync): port the sync lifecycle and session sync`

- `core/sync/…/SyncLifecycle.kt` (`SyncLifecycle.swift:10–211`): `data class SyncRetryState(failures = 0, serverDeadline: Instant? = null, lastResult: SyncResult? = null)`; `class SyncLifecycle(database, source, epoch, scheduler, retryState = SyncRetryState(), path: SyncPathMonitoring? = null, retention = PlanRetention(), unauthorized: suspend (Long) -> Unit = {}, verification: suspend (Long) -> Unit = {}, dispatcher)` exposing `engine`, `start()`, `requestSync()`, `refreshNow()`, `retryState`, `stop()`.
  - `start()`: once; `failures = max(failures, max attempts of pending rows)`; listen to engine `Finished` events; observe `outboxValues()` and call `requestSync()` whenever a **new id** appears (compare id sets with the previous emission; the first emission counts against an empty set); if `path` is set, call `requestSync()` only on an **offline → online** transition; finally `requestSync()`.
  - `requestSync()`: stopped or unauthorized already sent → return; deadline in the future **and** a retry pending → return; cancel the retry; deadline in the future → schedule a retry after `max(1.0, deadline − now)`; else `engine.requestSync()`.
  - `refreshNow()`: `epoch.check()`; stopped/unauthorized → `CancellationException`; deadline in the future with a `lastResult` → return `lastResult` (no source call); else cancel the retry and `engine.refreshNow()`.
  - `finished(result)` (ignored once stopped or the epoch is invalid): `lastResult = result`; `Unauthorized` → once: mark sent, cancel the retry, launch `unauthorized(epoch.generation)` **outside** the event listener; `VerificationRequired` → `failures = 0`, cancel the retry, launch `verification(generation)`; `Offline` → cancel the retry (wait for a trigger); `Idle`/`Failed`: if `Idle` and `retryAfter == null` → `failures = 0`, `serverDeadline = null`, cancel the retry; else `delay = max(min(2^min(failures, 9), 300), retryAfter ?: 0)`, `failures += 1`, `serverDeadline = serverRetryAfter?.let { now + it }`, schedule the retry.
  - Retry fire: if the id is current and not stopped: clear `retry` and `serverDeadline`, `engine.refreshNow()` (errors ignored).
  - `stop()`: `stopped = true`; `epoch.invalidate()`; cancel the retry; cancel the listeners; `path.stop()`; `engine.cancelAndWait()`; join the listeners and cancelled timers.
- `core/sync/…/SessionSync.kt` (`SessionSync.swift`, all but `requestStart`): `class SessionSync(accounts: ActiveSyncDatabase, scheduler, makePath: () -> SyncPathMonitoring? = { null }, source: suspend (StoredSession) -> DataSource, isCurrent: suspend (StoredSession, Long) -> Boolean = { _, _ -> true }, unauthorized: suspend (Long) -> Unit, verification: suspend (Long) -> Unit, dispatcher)` with `val retention`, `start(session, epoch)`, `resume(session, epoch)`, `setEnabled(Boolean)`, `requestSync()`, `refreshNow(): SyncResult?`, `stop()`, `deleteAccount(password, session, epoch)`, `accountSecurity(change, session, epoch)`, `fetchDetail(tripId, session, epoch)` and `events()` (conflated, replays the latest). Keep the generation guards, `attachedEpoch`, and `retryContext` exactly: `setEnabled(false)` detaches **preserving** the retry state for that epoch; `stop()` detaches without it and clears the retention leases; a lifecycle for the same epoch reuses the preserved state, a different epoch starts fresh. `StoredSession` is 4a's type in `net.chronoflow.core.model.session` (`{token, accountId}`); never log it.
- `deleteAccount(password, session, epoch)`, `accountSecurity(change, session, epoch)`, `fetchDetail(tripId, session, epoch)` (`SessionSync.swift:108–163`): not enabled or not `isCurrent` → `CancellationException`; if `attachedEpoch != epoch` or no lifecycle, `start(session, epoch)` first, then re-check (enabled, attached to `epoch`, lifecycle present, `isCurrent`) or throw `CancellationException`; call `lifecycle.engine.<method>`. On a `DataFailure`: `Unauthorized` → `unauthorized(epoch)`, `EmailUnverified` → `verification(epoch)`, then rethrow. `accountSecurity` and `fetchDetail` first re-check `isCurrent` and throw `CancellationException` instead when it is false; `deleteAccount` dispatches without that re-check (iOS l.116–119). `accountSecurity` also re-checks `isCurrent` after a success.
- [R] **Ownership vs 4e** (OQ-4d-14): 4e's brief (§5.5, T5) also describes `SessionSync` and a `NetworkSyncHint`, and checks whether 4d already ported `SyncLifecycle`. 4d ports **both** `SyncLifecycle` and `SessionSync` here (the iOS type takes `accounts`, not a session graph; 6b, 6c and 6d are written against 4d's `SessionSync.fetchDetail/events/retention`). 4e then only wires them: its `SessionAccountStore` implements `ActiveSyncDatabase`, and it adds `requestStart`. Keep `events()` as a `Flow<SyncEvent>` that replays the latest event to a new collector (iOS `latestEvent`); a 4e `StateFlow<SyncEvent?>` view, if it wants one, is a one-line `stateIn` in 4e.

Tests (`core/data/src/test/…`, Swift names, bodies from `SyncTriggerTests.swift`; `TriggerPath` = a fake `SyncPathMonitoring` over a `Channel<Boolean>`, `TriggerEvents`, `TriggerTime` (a mutable clock)): `openOutboxReconnectAndSceneBurstsDebounceAndStopDrains` (one debounced cycle; exact calls `send, identity, account, trips, detail`; path stopped; no pending sleeps; `refreshNow` after stop throws `CancellationException`), `cappedBackoffResetAndServerDeadline` (delays `1, 2, 4, 8, 16, 32, 64, 128, 256, 300, 300`; reset to 1 after success; a 600 s throttle blocks 5 requests and a refresh without source calls), `coalescedRecoveryPreservesServerDeadline`, `offlineWaitsForHintAndVerificationProbesIdentityOnly`, `unauthorizedDispatchesOnceAndInvalidEpochCannotDispatch` (`unauthorized == [1]`), `invalidatedUnauthorizedResponseCannotDispatchWipe`, `sessionPausePreservesServerDeadlineAndReplacementEpochDropsIt` (290 after 10 s; epoch 6 → 1), `sessionPausePreservesFailureExponent`, `restartSeedsBackoffFromPersistedOutboxAttempts` (attempts 8 → 256), `newTriggerPreemptsExponentialRetryActivation`/`…Outbox`/`…Reconnect`, `outboxBackoffCanBePreemptedButServerDeadlineCannot`, `reconnectAloneStartsOneDebouncedCycle`. For `SessionSync` tests, `ActiveSyncDatabase` returns the `SyncStore`'s `SqlSyncDatabase`.

Tick: "SyncEngine".

### Task 8 — `feat(data): keep pending local edits on login upsert`

- `core/data/…/store/LoginUpsert.kt`: `suspend fun AccountDatabase.upsertSessionAccount(account: SessionAccount)` (4e's `SessionController.accept` calls it as "DB upsert", before the secure write). If 4a already has a `SessionAccountDatabase.upsert`, make it delegate here. One `write` transaction (no epoch: there is none yet at accept; iOS `SessionAccountDatabase.swift:33`):
  - `account.id` must equal the open database's account, else `StoreFailure.InvalidAccountID`;
  - load or create the `account` row (`name`, `email` from the server for a new row);
  - **a pending `profile` row** → keep the local `name` and `email`; else take the server's;
  - `hasVerifiedEmail` from the server;
  - `walkthroughSeen = account.walkthroughSeen || a pending walkthrough row exists`;
  - `StoreApplication.setting("policies", account.policies)` (re-applies the latest pending `Policies` acceptance);
  - never touches `synced_at`, other settings sections, trips, marks or the outbox.
- [F] At the pin, iOS `SessionAccountDatabase.upsert` (l.29–48) already implements these three protections (commits `b1d16416` walkthrough, `52321ef2` policies, `9c9267b5` profile name/email), and tests each one in a feature suite: `SettingsTests/SettingsProfileTests.sessionUpsertPreservesPendingProfileAcrossRelaunch`, `PlanUITests/WalkthroughTests.relaunchPendingOverlaySurvivesSessionUpsertAndStaleFullPull`, `SettingsTests/TermsAcceptanceTests.relaunchSessionUpsertAndStaleFullPullKeepPendingAcceptance`. Those tests need the settings, walkthrough and terms models (Phases 8, 10a–10b), the Deferred-fix checkbox (`plan.md:2327`) is still open, and no store-level iOS test covers the RN scenario with all pending kinds at once. Android ports the behaviour and adds `LoginUpsertTests` at the store level (OQ-4d-4); the three feature tests stay with their phases.

Tests: `LoginUpsertTests` (Android, `core/data/src/test/…`):
- `sameAccountLoginKeepsQueuedEdits` (the RN `store-account.ts:10–23,46–64` scenario): enqueue `Profile("Edited")`, `Walkthrough`, a stored `policies {termsVersion "new"}` + `Policies("new")`, `Settings(Preferences, {"time_format":"12"})`; upsert a server account `name "Remote"`, `walkthroughSeen false`, `policies(termsVersion "new", accepted null)`, `hasVerifiedEmail true` → name `Edited`, walkthrough `true`, `acceptedTermsVersion "new"` with `acceptedTermsAt` = the pending row's `createdAt`, preferences `timeFormat "12"` untouched, 4 rows still pending, `synced_at` unchanged.
- `loginWithoutPendingRowsTakesServerValues` (the reverse): server name, `walkthroughSeen false`, server policies.
- `flushedEditsNoLongerProtect`: after removing the pending rows, a second upsert takes server values.
- `loginUpsertRejectsAnotherAccount`: `syncOther` → `InvalidAccountID`, nothing written.
- `unverifiedLoginUpdatesTheFlagOnly`: `hasVerifiedEmail false` from the server sets the flag even with pending rows.

Tick: "Preserving login upsert". In the same commit, append to the `plan.md` OQ-A40 row, which already records that iOS at `f391a1fa` preserves the edits (review 2026-11-16): "4d: store-level `LoginUpsertTests` added."

### Task 9 — `feat(data): add the network path monitor`

- `core/data/…/sync/ConnectivitySyncPathMonitor.kt`: `class ConnectivitySyncPathMonitor(context: Context) : SyncPathMonitoring`. `updates()` = `callbackFlow` that **first sends the current state** (`getNetworkCapabilities(activeNetwork)` has `INTERNET` and `VALIDATED`, else `false`; iOS `NWPathMonitor` also reports the current path first, `App/Session/NetworkPathMonitor.swift`; 4e's and 6c's tests expect it), then registers `ConnectivityManager.registerDefaultNetworkCallback`: `onCapabilitiesChanged` → `trySend(caps.hasCapability(NET_CAPABILITY_INTERNET) && caps.hasCapability(NET_CAPABILITY_VALIDATED))`; `onLost` → `trySend(false)`; `awaitClose { unregisterNetworkCallback }`; then `distinctUntilChanged()`. `stop()`: see below. It is a **hint only** (D23): no pre-request offline check anywhere.
- Manifest: `ACCESS_NETWORK_STATE` must be in the merged manifest (it is on the release allow-list); add it to `core/data/src/main/AndroidManifest.xml` only if missing.
- Wiring into `SessionSync(makePath = …)` is 4e.

`stop()` unregisters the callback registered by the current collection (keep a reference to it) and closes that producer; a later `updates()` registers afresh (iOS `NetworkPathMonitor.stop`). [R] iOS yields `path.status == .satisfied`; Android adds `VALIDATED` so a captive portal is not "online".

Test: `ConnectivitySyncPathMonitorTest` (`core/data/src/test/…`, `@RunWith(AndroidJUnit4::class) @Config(sdk = [36]) @Logic` per 0e §5, < 5 s, `runTest(timeout = 60.seconds)`): `currentStateComesFirst` (no active network → first emission `false`; with a validated default network → first emission `true`); `transitionsAreDistinct` (through `shadowOf(connectivityManager).networkCallbacks`, from no network: validated, `onLost`, validated → emissions `[false, true, false, true]`; a duplicate `true` is not re-emitted); `stopUnregisters` (after `stop()` the callback set is empty). If Robolectric's shadow cannot report `VALIDATED` for the active network, build `NetworkCapabilities` with `ShadowNetworkCapabilities.newInstance()` + `addCapability`; if that still cannot fail for the real reason, keep only the callback cases (AGENTS Tests rule 6) and say so in the commit body.

### Task 10 — `test(konsist): forbid background work outside reminders`

Both rules follow 0a §6b exactly: one function each in `KonsistRules.kt`; tests `<rule>()` and `<rule>RejectsPlantedViolation()` in `KonsistRulesTest.kt`; planted files under `konsist/src/test/resources/violations/<rule>/<module path>/src/…`.
- `noWorkManagerOutsideReminders`: no file whose package is not `net.chronoflow.core.data.reminders` (or a subpackage) imports `androidx.work.` (plan 4d acceptance). Planted: `violations/noWorkManagerOutsideReminders/core/sync/src/main/kotlin/net/chronoflow/core/sync/BackgroundFlush.kt` importing `androidx.work.WorkManager`. Allowed control (0a pattern, test `noWorkManagerOutsideRemindersAllowsControl()`): `allowed/noWorkManagerOutsideReminders/core/data/src/main/kotlin/net/chronoflow/core/data/reminders/RescheduleWorker.kt` importing `androidx.work.CoroutineWorker`. (12b's `Configuration.Provider` on the Application: OQ-4d-17.)
- `noFlowDebounceInSync`: no file in `core/sync` imports `kotlinx.coroutines.flow.debounce` (the timers must be ported, §5.3). Planted: `violations/noFlowDebounceInSync/core/sync/src/main/kotlin/net/chronoflow/core/sync/Debounced.kt`.
- Verify that `make quick` runs the untagged JDBC tests of `:core:data` and `:core:database` (`:core:database-testing` has no tests of its own; §4 item 9: read `build/test-results/quick/`); only if they are missing, add those modules' `testDebugUnitTest` to the quick recipe.
- `plan.md`: tick "Ported tests"; add the open questions of §10 that need the user or another phase (OQ-4d-4, OQ-4d-5, OQ-4d-12, OQ-4d-14, OQ-4d-17) to "Open questions for the user" as the next free `OQ-A` numbers with their defaults.

---

## 7. iOS → Kotlin test map

| iOS `Tests/SyncTests/…` | Methods (cases) | Kotlin (module) | Task |
|---|---|---|---|
| `LocalMutationsTests` | 2 (2) | same name (`:core:data`) | 3 |
| `FullPullTests` | 6 (6) | same | 4 |
| `PendingProtectionTests` | 2 (2) | same | 4 |
| `PlanRetentionTests` | 2 (2) | same | 4 |
| `OutboxFlusherTests` | 6 (26) | same, plus the Parameterized `OutboxFlusherFailureClassificationTests` and `OutboxFlusherPolicyClassificationTests` | 5 |
| `CheckOffReconcilerTests` | 8 (13) | same | 5 |
| `SyncEngineTests` | 17 (33) | same | 6 |
| `SyncChangesTests` | 6 (7) | same | 6 |
| `DataSourceIntegrationTests` | 2 (2) | same | 6 |
| `SyncTriggerTests` | 12 (14) | same | 7 |
| `Support/SyncFixtures.swift`, `Support/TestGate.swift` | — | `:core:testing` `…testing.sync` + `core/data/src/test/…/support/SyncStore.kt` | 3 |
| `SessionWipeTests`, `SessionSyncReadinessTests`, `MockSessionSeedTests` | 5 | — | **4e** |
| `LocalStoreTests/RecordsTests` (moved from 4a, draft halves → 9b) | 2 (2) | same (`:core:data`) | 3b |
| `LocalStoreTests/RepositoryObservationTests` (moved from 4a, draft half → 9b) | 6 (6) | same (`:core:data`) | 3b |
| **Total ported** | **71 (115)** | | |

Android-only: Konsist `databaseTestingIsTestOnly` (0); `OutboxPayloadTests`, `StoreTimeTests`, `CanonicalJsonTests`, `ContractsTests.inputWireKeysAndOutboxRoundTripsOutbox` (1); `SyncChangesDiffTests`, `RetryDelayTests`, `PlanLeaseTests`, `SyncRefusalJsonTests` (2); `LoginUpsertTests` (8); `ConnectivitySyncPathMonitorTest` (9); Konsist rules (10).

---

## 8. Acceptance criteria

1. These pass (the worker runs them; record the output in the final report):
   - `make targeted ONLY=…` for each task's changed test classes (including the full existing database suite in Task 0)
   - `make quick` once at the end. Per the implementation request, do not run `make check`, emulator tiers, performance benchmarks or profile generation in this phase.
2. All 115 ported cases (§7) and every Android-only test pass; the JUnit XML under `build/test-results/quick/` lists each Swift method name (with its `<Row>` suffix for a short parameterized list; the two 11-row tables as their own Parameterized classes, §6 Task 5).
3. Budgets from `timings.json`: every JVM test < 1 s, the Robolectric test < 5 s; no test uses `Thread.sleep`, `SystemClock.sleep` or a real-time `delay` in its body.
4. `grep -rn "androidx.work" core/sync core/data/src/main/kotlin/net/chronoflow/core/data/sync core/data/src/main/kotlin/net/chronoflow/core/data/repository core/data/src/main/kotlin/net/chronoflow/core/data/store` returns nothing.
5. No `runCatching` and no bare `catch (e: Exception)` / `catch (e: Throwable)` without a preceding `catch (e: CancellationException) { throw e }` in `:core:sync` and `core/data/…/{sync,store,repository}`.
6. `plan.md` Phase 4d boxes are ticked in the commits named; OQ-A40 carries the Task 8 note; new OQ rows exist.
7. 4a's `SqlSafetyTest` passes with the new `.sq` queries (no UPSERT, no `OR REPLACE`, no `RETURNING`).
8. `:core:database-testing` appears only in `testImplementation` blocks (`grep -rn "database-testing" --include=build.gradle.kts .`, plus its `include` in `settings.gradle.kts`), and Konsist `databaseTestingIsTestOnly`, `noWorkManagerOutsideReminders` and `noFlowDebounceInSync` pass and reject their planted violations.
9. No screenshot or perf number is required (no UI, no timeline change).

---

## 9. Review checklist (Opus reviewer)

Verify against the iOS file and line named; typical mistakes first.

- [ ] **Time strings.** `outbox.created_at` always has three fraction digits (`StoreTime.millis`); `synced_at`, tombstones, `received_at`, `failedAt` use the seconds form. A test proves string order = instant order.
- [ ] **Outbox insert is a plain `INSERT`** (duplicate id rolls back the mark: `durableRestartAndRollback`).
- [ ] **4a SQL rule 6.** No `ON CONFLICT`, `INSERT OR REPLACE`, `REPLACE INTO` or `RETURNING` in any new query; every save is update-then-insert in one transaction (a REPLACE on `trips` would cascade-delete `plan_versions`).
- [ ] **No duplicate types.** `OutboxChange`/`MutationReceipt`/`ProfileChange`/`SettingsSection` are 4b's, unchanged (`grep -rn "sealed interface OutboxChange" core` → one hit); `SyncEpoch.child()` is 4a's; `StoreFailure` exists once, in `:core:model`.
- [ ] **Plan bytes.** `plan_versions.detail` is written from `PlanVersionRecord.detail` as strict UTF-8 without re-encoding; `allRecordsRoundTrip…` compares bytes.
- [ ] **`receipt` predicate** is exactly `detail_version != ?` (no `IS NULL OR`).
- [ ] **AccountModel** ignores values from a superseded generation, goes `Unknown` on close and `Failed` on a store error, and has no finalizer.
- [ ] **Test-only driver.** `:core:database-testing` (xerial `sqlite-driver`, native libs) is never on a main classpath; `make size` stays free of `.so` files.
- [ ] **Cancellation.** Every `catch` rethrows `CancellationException` first; no `runCatching`; `epoch.check()` after every `await` in `FullPull`, `OutboxFlusher`, `CheckOffReconciler`, `SyncEngine.runOnce`; DB writes go through `write(epoch)` so an invalidated epoch cannot commit (`cancelledEpochCannotEnterQueuedResponseTransaction`).
- [ ] **Actor semantics.** No `Mutex` held across a suspension; state confined to a `limitedParallelism(1)` view; the flight is not a child of the caller; cancelling one of two waiters keeps the flight (`RefreshWaiters`).
- [ ] **Timers.** Debounce and retry are `Job`s through `SyncScheduler.sleep`, not `Flow.debounce`; cancelled timers are joined in `cancelAndWait`/`stop` (`cancelDrainsDebounceWithoutFiring`).
- [ ] **Rerun semantics.** `requestSync` during a flight sets `rerun`; reruns continue after `Offline`/`Failed`, stop only on `Unauthorized`/`VerificationRequired`; merged result = last state, max retry values, all refusals; one `Finished` event per flight.
- [ ] **Failure table** equals `OutboxFlusherTests.swift:61–107`; walkthrough rows are never refused and a walkthrough 401 returns `deferredWalkthroughAuthorization` without throwing; a stale check-off is never refused; a non-stale 422 on a check-off is refused with `StepDone`.
- [ ] **Stale rule** exactly: 409 `Conflict`, or 422 with `plan_input_hash`, or `form` == "This plan has changed. Refresh it before marking this step."; refresh once, then defer.
- [ ] **Undo.** Refusal writes `syncFailure`, deletes the row, restores `previousName` (or hands it to the next profile row), and nulls `detail_version` for a refused check-off with a notice. No toast, no string (OQ 30).
- [ ] **Parked kinds** are never sent, never deleted, block only their own dependency, and cannot be enqueued.
- [ ] **Dependency keys** read both `tripID` and `tripId`; blocking is per key, not global.
- [ ] **Pull.** Order `users/me → account → trips → transaction → details`; `synced_at` set in the cards transaction; empty list tombstones; `deleted_locally` never resurrected; detail 404 skipped without retry in the pull but refused in the reconciler; other detail errors set `retryAfter = 1` and continue; `Offline`/`Throttled`/401/403 abort.
- [ ] **Pending protection** in `identity`, `snapshot`, `setting`, `checks` (skip while a check-off for the trip is pending), and `readiness` deleted while a body-clock row is pending.
- [ ] **Eviction** keeps held, pending-referenced and keep-offline plans; retention lock held during eviction.
- [ ] **Backoff** `1, 2, …, 256, 300`; seeded from stored attempts; server deadline blocks requests and refreshes (returns `lastResult`); activation/outbox/reconnect preempt exponential backoff but not a server deadline; unauthorized dispatched once, never from an invalidated epoch; verification resets failures.
- [ ] **Login upsert** keeps name/email (pending profile), walkthrough (pending row) and accepted policies (pending policies) and touches nothing else.
- [ ] **Secrets.** `safeSettings` whitelists keys; no password, token, code or signature can reach an outbox payload, a `SyncRefusal` or a log (`failureClassification` asserts "secret response" is absent).
- [ ] **No background work.** No WorkManager/JobScheduler/foreground service; Konsist rules present with planted violations.
- [ ] **Module rules.** `:core:sync` imports no `android.*`, no SQLDelight driver and no `System.currentTimeMillis`/`Instant.now()`; `:core:data-mock` and `sqlite-driver` are test dependencies only.
- [ ] **Test fidelity.** Swift names kept (`<Row>` suffix or a Parameterized class, 0d §4.2); literal expectations copied (call lists, delays, attempts, `lastError` strings), not loosened.

---

## 10. Open questions (proceed on the default)

| # | Question | Default |
|---|---|---|
| OQ-4d-1 | A3 makes `:core:sync` JVM, but the sync logic needs the Android SQLDelight database. | Port interface `SyncDatabase` in `:core:sync`; SQL bodies in `:core:data` (`SqlSyncDatabase`, `StoreApplication`); DB-bound ported tests in `:core:data` on the JDBC driver (§5.1). |
| OQ-4d-2 | The plan names repositories "trips, plans, marks, settings and walkthrough"; iOS has `AccountRepository`, `TripRepository`, `SettingsRepository`, `OutboxRepository` (+ `enqueue`). | Keep the iOS types: plans and marks are `TripRepository` reads; marks, settings, profile, policies and walkthrough are written through `OutboxRepository.enqueue`. No extra wrapper classes. |
| OQ-4d-3 | How is a null check-off `status` stored? (1a deferred it here.) | Omitted from the stored payload (Swift synthesized `encodeIfPresent`); the API body sends explicit `"status": null` (4b). |
| OQ-4d-4 | iOS's login-upsert Deferred fix is open, but the code at the pin already preserves the edits (and feature suites test each kind). | Port the pin behaviour and add store-level `LoginUpsertTests`. plan.md OQ-A40 already records the iOS state (review 2026-11-16); Task 8 appends only the test note. |
| OQ-4d-5 | Is the Android network-callback path monitor in 4d or 4e? | Built and tested in 4d (Task 9), wired in 4e. Add to plan.md so 4e's brief picks it up. |
| OQ-4d-6 | iOS session-level sync tests need `SessionController`. | `SessionWipeTests`, `SessionSyncReadinessTests`, `MockSessionSeedTests` move to 4e; `TripGenerationEffects+Store` to 9b. |
| OQ-4d-7 | Sync-failure presentation. | None (iOS OQ 30): `settings.syncFailure` and `SyncResult.refusals` only; no string, no event UI. |
| OQ-4d-8 | `PlanLease` relies on Swift `deinit`; the JVM has none. | Explicit `release()` only, no `Cleaner`/finalizer; 6b's ViewModel releases in `onCleared`. |
| OQ-4d-9 | Parameterized Swift tests. | 0d §4.2: ≤ 5 rows → one plain method per row `<swiftName><Row>` calling a private helper; longer lists → their own `Parameterized` class keeping the Swift method name (OQ-4d-18). |
| OQ-4d-10 | Engine event buffer. | One channel per observer, capacity 32, drop oldest (iOS `bufferingNewest(32)`); `SessionSync` conflated with replay of the latest. |
| OQ-4d-11 | Byte parity of stored JSON with iOS (`JSONEncoder` escapes `/`, sorts keys). | Not required (no cross-platform DB migration); sorted keys via `CanonicalJson` for determinism only. |
| OQ-4d-12 | `StoreFailure` is in the Android `:core:database` (4a), but `:core:sync` (JVM) must throw `InvalidAccountID` in `FullPull` and 4e's JVM `SessionController` maps store failures. | Move it to `:core:model` (`net.chronoflow.core.model.store`) in Task 0, same cases and names; a pure move. |
| OQ-4d-13 | Where does the shared JDBC driver factory live (OQ-4a-13: `testFixtures` or a module)? AGP 9 Kotlin test fixtures need an experimental flag. | A small Android library `:core:database-testing`, consumed only through `testImplementation`, guarded by Konsist `databaseTestingIsTestOnly`. |
| OQ-4d-14 | 4e's brief (§5.5, T5) also plans `SessionSync` and a `NetworkSyncHint`; 6b–6d expect 4d's `SessionSync` and `ConnectivitySyncPathMonitor`. | 4d ports `SyncLifecycle`, `SessionSync` (iOS constructor shape with `ActiveSyncDatabase`) and `ConnectivitySyncPathMonitor` (current state first). 4e reuses them, adds `requestStart`, and makes `SessionAccountStore` implement `ActiveSyncDatabase`. The orchestrator updates 4e's brief before 4e starts. |
| OQ-4d-15 | The landed `CheckOffChange` has no `@Serializable`; 4b owns its wire body and 4d owns its storage codec. | `OutboxPayload` builds and parses stored payloads by hand (§5.4), so the annotation does not matter. Leave `CheckOffChange` as it is in code. |
| OQ-4d-16 | iOS `AccountModel` is `@MainActor @Observable`; there is no board or screen for it in 4d. | Port it as a `StateFlow` holder in `:core:data` confined to an injected scope (main in production); 4e/6a attach it. No UI. |
| OQ-4d-17 | Plan 4d's acceptance forbids `androidx.work` outside the reminders package of `:core:data`, but plan 12b initializes WorkManager on demand through `Configuration.Provider`, which the `Application` class in `:app` must implement (an `androidx.work` import outside that package). | 4d ships the rule as the plan words it (nothing imports WorkManager yet). 12b adds a single named exception for the `:app` Application file and records it. Add to plan.md "Open questions for the user" in Task 10. |
| OQ-4d-18 | Swift parameterized tests: 0d §4.2 (binding for 4c) names short lists `<swiftName><Row>` and gives long lists their own `Parameterized` class. | 4d follows 0d §4.2 ("Test naming" in §6 Task 5 applies to Tasks 5–7). |


## Appendix B. Table-driven test rule

Use the Task 5 naming rule throughout Tasks 1–10: up to five rows become independent `<swiftName><Row>` methods calling a private helper; longer tables get their own JUnit4 `Parameterized` class with the Swift method name and readable row names. Never loop over test cases inside a test. Keep every listed test class in §7 and the Android-only list.

## Refresh against landed 4c (2026-11-18)

Checked the current tree: epochs/guarded writes, schema/query labels and RecordMapping match §4; StoreFailure and the four JDBC helpers still need Task 0's moves. `checkConfiguration` needed public visibility for the moved factory; its SQL and failure behavior are unchanged. `HandDrainedDispatcher` remains local to database tests. All 25 DataSource methods, outbox values, ApiDataSource.send, MockDataSource.send, remoteTripUpdate and replaceRemoteTrips are present: skip Task 1b. MockScenarios/MockClock are internal test helpers in data-mock/src/test; do not copy or import them. MockApi is also test-local. Existing Konsist helpers are `moduleOf`, `sourceSetOf`, `isTestSourceSet`, `projectScope`, `projectRoot`; keep planted controls under the rule-specific resource paths. JvmLibraryConventionPlugin supplies JUnit4/assertk only; add coroutines/serialization dependencies where used. Android libraries supply no test dependencies automatically. `make quick` already selects untagged local JVM tests through testDebugUnitTest; verify its result files in Task 10. Highest question is OQ-A166; new rows start at OQ-A167 (A68 stays unused).
