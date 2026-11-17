# Phase 4a brief: account store, session secrets and device identity

- **Branch:** `feat/phase-4a-store`, from the stack top once 0d is in the stack (0a and 0e are always assumed). Wave 3: 1a, 2 and 9a run beside you in their own worktrees, so keep edits to shared files (`plan.md`, `Makefile`, `gradle/libs.versions.toml`, `settings.gradle.kts`) small.
- **Depends on:** 0d (`AccountID`, `ApiJson`), 0e (tier annotations, `DeviceConfig`, `LanguageChoice`, `ThemeChoice`, `DeviceConfigFile`, `TestSeeds`). **Unblocks:** 4d (repositories, sync), 4e (session controller, wiper), 5a (device identity), 9b (drafts), 10a/10b (device preferences), 12b/12c (reminder tables, permission flag).
- **Plan entry:** `plan.md` "### Phase 4a" (l.1463–1514) and decision A5 (l.92).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`). Revised the same day so that it hands 1a, 4d, 4e, 5a and 9b exactly what their briefs expect (§4.2). Checked the same day against iOS at the pin, web `e36368ec`, `plan.md` and AGENTS.md: the hashes, line citations, schema, records, Swift test names and cases were confirmed; test isolation, cross-module visibility, the backup-rules test and the DataStore singleton were corrected.
- **Boards:** none. This phase has no UI and no screen. Do not add any.
- **Tests:** AGENTS.md "Tests". Three tiers:
  - plain JVM (JUnit 4, assertk, kotlinx-coroutines-test, Turbine): no tier annotation;
  - Robolectric `@Logic` (anything that touches `Context`, `AtomicFile`, DataStore or the framework SQLite);
  - `@Smoke` instrumented on `pixel`: the AndroidKeyStore suite (≤ 6 tests) and one main-thread guard test. Nothing else.

Tags: **[F]** fact verified in source (path and line). **[R]** decision for Android adopted by this brief.

---

## 1. Goal

Give later phases a safe local store and the device-level files. There is no UI.

1. **Account database.** One SQLDelight database per account, holding the iOS schema v1 verbatim plus two Android-only reminder tables. It lives at `noBackupFilesDir/<ns>/Accounts/<uuid>/account-<uuid>.db`, and only a validated UUID becomes a path component. It runs in WAL mode with foreign keys on. Writes go through one serial writer. A guarded write holds the sync epoch and the mutation permit **through the commit**. Observation emits only committed values and completes when the database closes.
2. **Session and store primitives in `:core:model`.** `StorageNamespace`, `StoredSession`, `SessionStore`, `SessionPresence`, `SyncEpoch`, `SessionMutationPermit`, `UUIDv7`, and the four records `AccountRecord`, `TripRecord`, `SettingsRecord`, `OutboxRecord`. The record ↔ row mapping (save and fetch) lives in `:core:database`.
3. **Session secret.** `KeystoreSessionStore`: `{token, accountId}` encrypted with AES-256-GCM under an AndroidKeyStore key and written atomically to `noBackupFilesDir/<ns>/session.bin`. A permanent key or decrypt failure deletes the session, which signs out. A transient failure throws and keeps the file.
4. **Device files.** The UUIDv7 `api.deviceId` (D22) and the device-name rule; DataStore `device_prefs` (language, theme, notification-permission flag); the `device-config` mirror below API 33 (language) and API 31 (theme); the wipe marker `pending-wipe.json`.
5. **Backup exclusion**, proved by tests, and every 4a path proved to sit under `noBackupFilesDir`.
6. **Ported tests** for everything 4a ships, with the Swift names. The LocalStore cases that need repositories, `AccountModel`, `AccountWiper` or `SessionAccountDatabase` move to the phases that ship those classes (Appendix B).

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` l.1463–1514 (Phase 4a); A3 (module rules, l.90); A5 (persistence, l.92); "Android spec: seeds and test hooks" (l.465); "Open questions for the user" (l.570); Phase 4d (l.1597–1637), 4e, 9b entries | Scope; where new OQ rows go; the bullets you edit in Task 10 |
| Agent rules | `AGENTS.md` "Tests", "Commands", "Git" (no trailers), "Traps" (rethrow `CancellationException`; Robolectric needs the JDK 21 toolchain; no JUnit `Timeout` rule under Robolectric) | How to work |
| Research | `docs/research/02-ios-architecture.md` §4.1–§4.4, §4.9–§4.10, §7; `docs/research/14-gaps.md` §3; `docs/research/09-rn-android-lessons.md` §5 | iOS store and session design; RN storage lessons |
| 0a brief | `docs/phases/phase-0a-scaffold.md`: module table l.127–128; `chronoflow.sqldelight` l.336; OQ-0a-6 l.805; `data_extraction_rules.xml` l.393; `DataExtractionRulesTest` l.413; Konsist rules l.495–504; `Scripts/targeted.py` l.663–667 | What exists |
| 0d brief | `docs/phases/phase-0d-contracts.md`: `ApiJson` l.167–173; `AccountID` l.185; `OutboxKind` l.260; `ProfilePreferences` l.379; `SessionAccount`/`AccountPolicies` l.432–433 | Types you reuse |
| 0e brief | `docs/phases/phase-0e-test-harness.md`: tiers §5 l.126–134; T1 tier registry l.166–179; T4 `LanguageChoice`/`ThemeChoice`/`DeviceConfig` l.227–243; T10 `DeviceConfigFile` l.392–405; T11 `TestSeeds.apply` l.461–468; `TestSeedsApplyTest` l.479 | Types and files you move or extend |
| Downstream briefs | `phase-1a-plan-core.md` l.262–270 and OQ-1a-6 (l.421); `phase-4d-sync.md` §4 "What 4a, 4b and 4c hand you" items 1–6 and §5.1 "Module split" (find them by heading: that brief is revised in parallel, so line numbers move); `phase-4e-session.md` §5.1 (l.148–163), §5.3 AccountWiper (l.264–272), T4 (l.488–507); `phase-9b-wizard-data.md` §4 item 5 (l.158–166) and §6.5 (l.424–457); `phase-5a-sign-in.md` l.368 | What they expect from you (§4.2). Do not implement their parts. |

Read the landed code, not only the briefs: `core/model/src/main/kotlin/net/chronoflow/core/model/` (`AccountID.kt`, `api/ApiJson.kt`, `api/SessionContracts.kt`, `contracts/ProfilePreferences.kt`, `config/*`), `app/src/main/kotlin/net/chronoflow/app/config/DeviceConfigFile.kt`, `app/src/debug/kotlin/net/chronoflow/debug/TestSeeds.kt`, `app/src/main/res/xml/data_extraction_rules.xml`, `core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/`, `build-logic/`, `Makefile`. Before Task 2, run `ls core/model/src/main/kotlin/net/chronoflow/core/model/store/ 2>/dev/null` to see whether 1a has landed. **If a landed signature differs from this brief, follow the landed code and say so in your hand-off.**

### 2.2 iOS (read-only): `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`

`P` = `Packages/ChronoFlowKit/Sources`, `T` = `Packages/ChronoFlowKit/Tests`.

| iOS file | Android target |
|---|---|
| `P/LocalStore/SchemaV1.swift` (DDL l.7–38) | `.sq` files (Task 3) |
| `P/LocalStore/AccountDatabase.swift` (`open` l.31–74, `read`/`write` l.82–91, `validate` l.93–97, `write(permit:)` l.99–114, `write(epoch:permit:)` l.116–141, `observe` l.143–161, `close` l.165–172) | `AccountDatabase` (Tasks 4–5) |
| `P/LocalStore/ObservationLifetime.swift` | inside `AccountDatabase.observe` (Task 5) |
| `P/LocalStore/SyncEpoch.swift` (l.5–45), `P/AppCore/SessionMutationPermit.swift` (l.3–18) | `SyncEpoch`, `SessionMutationPermit` (Task 1) |
| `P/AppCore/SessionStore.swift` (l.3–43), `P/AppCore/StorageNamespace.swift` | `:core:model` `session/` (Task 1) |
| `P/LocalStore/Records/{Account,Trip,Settings,Outbox}Record.swift`, `Records/JSONDocument.swift` | `:core:model` `store/` (Task 2); mapping in `:core:database` (Task 6) |
| `P/AppCore/DeviceIdentityStore.swift` (`UUIDv7` l.12–26, `identity()` l.49–84, `deviceName` l.86–95) | `UUIDv7` (Task 2); `DeviceIdentityStore` (Task 8) |
| `P/LocalStore/AccountWiper.swift` (marker part only) | `WipeMarker` (Task 8) |
| `App/Readiness/KeychainSessionStore.swift` | `KeystoreSessionStore` (Task 7) |
| `T/LocalStoreTests/{AccountDatabaseTests,SchemaV1Tests,RecordsTests}.swift`, `T/LocalStoreTests/Fixtures/rn-schema-v1.json`, `T/AppCoreTests/{SessionStoreTests,DeviceIdentityTests}.swift`, `AppTests/{KeychainSessionStoreTests,AccountStorageLifecycleTests}.swift` | Appendix B |
| iOS `plan.md` l.25 (D18, D19, D21, D22), l.382 "Account schema v1 and observation", l.1064 "Phase 9a", l.2405 (OQ 26) | Background (§2.3) |

### 2.3 Plan and iOS statements this brief relies on

- **A5** [F, `plan.md:92`]: one DB per account at `noBackupFilesDir/<ns>/Accounts/<uuid>/account-<uuid>.db`; WAL and `foreign_keys=ON`; never replay RN migrations 0000–0014; `plan_versions.detail` holds the raw server bytes; accept order is DB upsert → secure write → publish; `allowBackup=false` plus rules that exclude everything.
- **D21** [F, iOS `plan.md:25`]: malformed session data is deleted; temporary failures throw and never delete; there is no refresh token.
- **D22** [F, iOS `plan.md:52`]: the device id is a UUIDv7 stored outside account storage and reused across sign-out and account wipe; the device name is at most 255 Unicode scalars. The server side agrees [F, web `app/Http/Requests/Api/V1/LoginRequest.php:25–26` at `e36368ec`]: `device_name` is `required|string|max:255`, `device_id` is `required|uuid`.
- **iOS OQ 26** [F, iOS `plan.md:2405`]: every account file is excluded from backup, unsent outbox rows included; a device restore loses unsent changes. Still open for shipping (OQ-4a-14).
- **iOS Phase 9a review fixes** [F, iOS `plan.md:1064`]: the credential write is the commit point; hide a closing database. (Both are implemented by 4e on top of what you build.)

### 2.4 Endpoints

None. This phase makes no network call and adds no DTO.

---

## 3. Out of scope (owner in brackets)

- `OutboxChange` and `MutationReceipt` [4b/4d; the current 4d brief expects 4b's].
- **Repositories** (`AccountRepository`, `TripRepository`, `SettingsRepository`, `OutboxRepository` with `enqueue`), `AccountModel`, `StoreApplication`, `LocalMutations`, `CanonicalJson`, `StoreTime`, the preserving login upsert (OQ-A40), the plan-version and check-mark mapping, and the LocalStore tests that use them [4d; `plan.md` 4d "Repositories" puts them in `core/data/…/repository/`].
- `DraftRecord`, `DraftRepository`, the `drafts` named queries and the draft halves of `RecordsTests` [9b].
- `AccountWiper`, `SessionAccountDatabase`, `SessionAccountStore`, `AccountReadinessRepository`, `SessionController`, the teardown order, `AppGraph` wiring, and their tests (`AccountWiperTests`, `SessionAccountDatabaseTests`, `AccountReadinessTests`) [4e]. You ship only the `WipeMarker` file helper that 4e's wiper uses.
- `PlanVersionRecord`, `CheckMarkRecord`, `TripPlanSnapshot`, `CheckOffChange` [1a, OQ-1a-6]. Do not create them, even if 1a has not landed (OQ-4a-16).
- Guest secrets (Keystore alias `<keychainService>.guest`) and `guest-wizard.json` [11a/11b].
- Queries on `scheduled_reminder`/`reminder_ledger` [12b]; reading `notificationPermissionRequested` [12c]; mirroring language to `LocaleManager` [10a] and theme to `UiModeManager` [10b] from the settings screens.
- `NoHealthDataMigration` and its test: Android never has a pre-v2 database (OQ-4a-8).
- The mock server's `ChronoFlowMock/Server/state.json` [4c].
- Any network, DTO or UI.

---

## 4. Modules, packages and the contract with other phases

### 4.1 Layout

| Module | Package | New in 4a |
|---|---|---|
| `:core:model` (JVM) | `net.chronoflow.core.model.session` | `StorageNamespace`, `StoredSession`, `SessionStore` (+ `readStoredSession`, `loadSession`), `SessionPresence`, `SyncEpoch`, `SessionMutationPermit`, `StaleSessionException` |
| `:core:model` | `net.chronoflow.core.model.device` | `UUIDv7`, `DeviceIdentityFailure`, `DeviceIdentityPersistence` |
| `:core:model` | `net.chronoflow.core.model.store` | `AccountRecord`, `TripRecord`, `SettingsRecord`, `OutboxRecord`, `JSONDocument` (validator object) |
| `:core:database` (`chronoflow.sqldelight`) | `net.chronoflow.core.database` | `.sq` files, generated `AccountDb`, `StoreFailure`, `AccountSql`, `AccountDriverFactory`, `AndroidAccountDriverFactory`, `AccountDatabase` |
| `:core:database` | `net.chronoflow.core.database.store` | `RecordMapping.kt` (save/fetch for the four records) |
| `:core:database` `src/test` | `net.chronoflow.core.database.testing` | `JdbcAccountDriverFactory`, `TestRoots`, `Accounts` (`accountA`, `accountB`), `SqlProbe` |
| `:core:datastore` (`chronoflow.android.library`) | `net.chronoflow.core.datastore` | `SessionCipher`, `AndroidKeystoreSessionCipher`, `KeystoreSessionStore`, `DevicePreferences`, `DeviceIdentityStore`, `DeviceConfigFile` (moved from `:app`), `AndroidDeviceName`, `WipeMarker` |
| `:app` | `app/src/test`, `app/src/androidTest` | `BackupRulesTest`, `StoragePathsTest`, `KeystoreSessionStoreTest`, `StoreThreadingSmokeTest`; edits to `ConfigurationPolicy` imports and `TestSeeds` |

Dependencies (add only what is missing; use 0a's catalog aliases, never new versions):
- `:core:model`: `implementation(libs.kotlinx.coroutines.core)` (OQ-4a-18); `testImplementation(libs.kotlinx.coroutines.test)`.
- `:core:database`: `api(project(":core:model"))`, `implementation(libs.sqldelight.android.driver)`, `implementation(libs.kotlinx.coroutines.core)`; `testImplementation(libs.sqldelight.sqlite.driver)`, `testImplementation(project(":core:testing"))` (tier annotations), Robolectric, `androidx.test:core`, Turbine, assertk, coroutines-test.
- `:core:datastore`: `api(project(":core:model"))`, `implementation(libs.androidx.datastore.preferences)`, coroutines; the same test libraries.
- `:app`: `implementation(project(":core:datastore"))`, `implementation(project(":core:database"))`.

**[R] `sqlite-driver` (xerial, which ships native libraries) is `testImplementation` only.** `make size` fails on any `.so`.

### 4.2 What other briefs expect from you (check each before you finish)

| Consumer | Expects | Where you deliver it |
|---|---|---|
| 4d §4 items 1–6 (it now quotes this brief's names, including the spelling `accountID`) | `SyncEpoch` with `child()`, `check()` (throws a `CancellationException` subtype when invalid **or** when the coroutine is cancelled), `whileValid`, `invalidate()`, `generation: Long`, in a JVM module; `SessionMutationPermit.whileValid`; `AccountDatabase` with `accountID`, `read`, `write(permit)`, `write(epoch, permit)`, `observe`, `validate(epoch)`, `open`, `close`, constructible over an injected `(File) -> SqlDriver`; `AccountRecord`, `TripRecord`, `SettingsRecord`, `OutboxRecord` in `net.chronoflow.core.model.store` with JSON columns as `String`; `UUIDv7.generate(millisecondsSince1970: Long, randomBytes: ByteArray): UUID` | Tasks 1, 2, 4, 5 |
| 9b §4 item 5 | `SyncEpoch.isValid` (non-throwing), `StoreFailure`, `TripRecord`/`SettingsRecord`/`OutboxRecord`; `dirty` as an `INTEGER` column without an adapter | Tasks 1, 3, 4 |
| 4e §5.1, §5.3, T4 | `StoredSession` (`toString()` → `token=<redacted>`), `SessionPresence.SignedIn(accountId)`, `SessionStore` (JVM interface that `KeystoreSessionStore` implements), `SessionMutationPermit` with a public `invalidate()`, a `WipeMarker` writing the JSON string `"<uuid>"` atomically, an `AccountDatabase` taking `root: File` and a driver factory; a Keystore store whose file and alias can be overridden (`ChronoFlowMock.uitest.<id>`) | Tasks 1, 4, 7, 8 |
| 5a l.368 | `DeviceIdentityStore.identity()` and the device-name rule | Task 8 |
| 1a OQ-1a-6 | 4a does **not** define `PlanVersionRecord`/`CheckMarkRecord` (1a owns them in `…model.store`) | §3 |

### 4.3 Rules for every class in this phase [R]

1. **Secrets** (session token, key bytes) never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the database, the outbox, DataStore, a log line, an exception message or a `toString()`.
2. **No main-thread I/O.** Every function that touches disk, SQLite, DataStore or the Keystore is `suspend` and switches to an injected dispatcher (`io`). Only production wiring passes `Dispatchers.IO`. **Never call `context.noBackupFilesDir` on the main thread**: it may create the directory. Classes take `root: File`; their caller resolves it off the main thread. `runBlocking` stays out of main code (Konsist rule 5).
3. **Cancellation.** Never `runCatching` or `catch (e: Exception)` without a preceding `catch (e: CancellationException) { throw e }`. Catch the specific types listed per task.
4. **One `Json`.** Use `ApiJson.response` for every parse and encode in this phase. Create no `Json {}` instance.
5. **Paths.** Only `AccountID.rawValue` (validated, lower-case) becomes a path component. Never `UUID.fromString` (it accepts `1-1-1-1-1`).
6. **No SQL that API 29 cannot run** (API 29 ships SQLite 3.22.0): no UPSERT (`ON CONFLICT … DO`, 3.24), no `RETURNING` (3.35), no `DEFAULT true/false` (keywords since 3.23; on 3.22 they become the strings `'true'`/`'false'`), no window functions, and no `INSERT OR REPLACE`/`REPLACE INTO` in 4a's queries. REPLACE deletes the row, which fires `ON DELETE CASCADE` on `plan_versions`. "Save" means `UPDATE … WHERE <pk>`, then `INSERT` when `changes()` is 0, in the same transaction.
7. **Locks** (`ReentrantLock`, `ReentrantReadWriteLock`) are held only across blocking code, never across a suspension point.

---

## 5. Tasks

One commit per task, in this order. Each commit builds and passes `make quick`. Conventional Commits, lower-case imperative subject, **no trailer of any kind**. Test constants (iOS `T/LocalStoreTests` helpers): `accountA = "019a562b-8000-7000-8000-000000000001"`, `accountB = "019a562b-8000-7000-8000-000000000002"`.

### Task 1. Session primitives in `:core:model`

Commit: `feat(model): add session store, sync epoch and mutation permit`. Ticks nothing (the plan's "Ports" box is ticked in Task 8).

**Files** (`core/model/src/main/kotlin/net/chronoflow/core/model/session/`)
- `StorageNamespace.kt` [F `StorageNamespace.swift`]:
  ```kotlin
  enum class StorageNamespace(val directoryName: String, val keychainService: String) {
      Api("ChronoFlow", "net.chronoflow.app.native"),
      Mock("ChronoFlowMock", "net.chronoflow.app.native.mock");
      val sessionKeyAlias: String get() = "$keychainService.session"   // Android-only (A5 "<ns>.session")
  }
  ```
  [R, from F `docs/research/02-ios-architecture.md` §4.1–§4.2] The `<ns>` of the plan's alias `<ns>.session` is the namespace's key alias, which research 02 sets to the iOS Keychain service (the iOS session item is service `keychainService`, account `session`). The `<ns>` of a **path** is `directoryName`.
- `StoredSession.kt` [F `SessionStore.swift:3–19`]:
  - `data class StoredSession(val token: String, val accountId: String)`; `toString()` = `"StoredSession(token=<redacted>, accountId=$accountId)"`.
  - `fun encode(): ByteArray`: `{"token":…,"accountId":…}` through `ApiJson.response` (a `buildJsonObject`, key order token then accountId).
  - `companion fun parse(bytes: ByteArray): StoredSession?`: `ApiJson.response.parseToJsonElement(bytes.decodeToString())`; `SerializationException` and `IllegalArgumentException` → `null`. The element must be a `JsonObject`; `token` and `accountId` must each be a `JsonPrimitive` with `isString == true`; each must be non-blank after `trim()`. The stored values are returned **untrimmed** (Swift keeps them). Extra keys are ignored. Anything else → `null`.
- `SessionStore.kt` [F `SessionStore.swift:21–43`]:
  - `interface SessionStore { suspend fun read(): ByteArray?; suspend fun write(data: ByteArray); suspend fun delete() }`.
  - `suspend fun SessionStore.readStoredSession(): StoredSession?`: `read()` null → null; `parse` null → `delete()`, then null.
  - `suspend fun SessionStore.loadSession(): SessionPresence`: `readStoredSession()` → `SignedIn(accountId)` or `SignedOut`. Rethrow `CancellationException`; any other `Exception` → `SignedOut` (iOS `catch { return .signedOut }`).
- `SessionPresence.kt`: `sealed interface SessionPresence { data object SignedOut : SessionPresence; data class SignedIn(val accountId: String) : SessionPresence }` (4e §5.1 spelling).
- `StaleSessionException.kt`: `class StaleSessionException : CancellationException("stale session")` (`kotlinx.coroutines.CancellationException`). It is the Swift `CancellationError` thrown by an invalid epoch or permit, so `launch` treats it as cancellation and the §4.3 rethrow rule passes it through.
- `SyncEpoch.kt` [F `SyncEpoch.swift:5–45`]:
  ```kotlin
  class SyncEpoch private constructor(val accountID: AccountID, val generation: Long, private val parent: SyncEpoch?) {
      constructor(accountID: AccountID, generation: Long) : this(accountID, generation, null)
      private val lock = ReentrantLock()
      private var valid = true                                  // guarded by lock
      fun child(): SyncEpoch = SyncEpoch(accountID, generation, this)
      fun invalidate() { lock.withLock { valid = false } }
      val isValid: Boolean get() = (parent?.isValid ?: true) && lock.withLock { valid }   // Android-only, for 9b
      suspend fun check() { currentCoroutineContext().ensureActive(); whileValid { } }
      // Hold validity through SQLite's commit so detach cannot race response application.
      fun <T> whileValid(operation: () -> T): T =
          if (parent != null) parent.whileValid { ownValidity(operation) } else ownValidity(operation)
      private fun <T> ownValidity(operation: () -> T): T = lock.withLock {
          if (!valid) throw StaleSessionException()
          operation()
      }
  }
  ```
  Use `if/else`, not `parent?.whileValid { … } ?: …`: the Elvis form runs the operation twice when `T` is nullable and the result is null. `operation` is not `suspend`.
- `SessionMutationPermit.kt` [F `SessionMutationPermit.swift`]: `class SessionMutationPermit { fun invalidate(); fun <T> whileValid(operation: () -> T): T }`, the same lock pattern, no parent. The constructor and `invalidate()` are public because the issuer (4e `SessionController`) lives in `:core:sync`. KDoc, from the iOS comment: "Only the session controller invalidates a permit. Publication waits for an admitted SQLite transaction to commit before revoking the session." (OQ-4a-15)

**Tests** (`core/model/src/test/kotlin/net/chronoflow/core/model/session/`, plain JVM, `runTest(timeout = 60.seconds)`)
- `SessionStoreTests` (Swift names; an in-memory `MemorySessionStore` fake with a `deletes` counter):
  - `nativeSessionRoundTripsWithoutDeleting`: `value = StoredSession("secret", "account")`; `StoredSession.parse(value.encode()) == value`; an empty store's `loadSession()` is `SignedOut`; after `write(value.encode())`, `loadSession()` is `SignedIn("account")`; `deletes == 0`.
  - `malformedSessionsAreDeletedAndSignedOut`, JUnit `Parameterized`, one case per input: `""`, `"null"`, `"[]"`, `"{}"`, `"{bad"`, `{"token":"a"}`, `{"token":12,"accountId":"a"}`, `{"token":"","accountId":"a"}`, `{"token":"a","accountId":"  "}`, `{"token":"a","accountID":"a"}`. Each: `loadSession()` is `SignedOut`; `deletes == 1`; `read()` is null; a second `loadSession()` is `SignedOut` and `deletes` is still 1.
  - Android-only: `toStringRedactsTheToken` (`"secret"` is not in `toString()`); `loadSessionRethrowsCancellation` (a fake whose `read()` throws `CancellationException` → `loadSession()` throws it); `parseKeepsUntrimmedValues` (`{"token":" t ","accountId":"a"}` → token `" t "`).
- `SyncEpochTests` (Android-only names):
  - `invalidEpochRefusesAndChildFollowsParent`: `child().whileValid` runs; after `parent.invalidate()` both throw `StaleSessionException` and both `isValid` are false; invalidating only a child leaves the parent valid.
  - `invalidateWaitsForAnAdmittedOperation`: thread A enters `whileValid` and blocks on a `CountDownLatch`; thread B calls `invalidate()`; poll `threadB.state` until `WAITING` (spin with `Thread.onSpinWait()`, never a sleep); B is still alive until A's latch is released; afterwards `whileValid` throws.
  - `nullableResultsPassThrough`: `whileValid<String?> { null }` returns null and runs the operation once (count it).
  - `checkThrowsWhenTheCallerIsCancelled`: inside a cancelled `Job`, `check()` throws `CancellationException` even though the epoch is valid.
- `SessionMutationPermitTests`: `invalidPermitRefuses`, `invalidateWaitsForAnAdmittedOperation` (same shape as above).
- `StorageNamespaceTests.namespacesAreDistinct`: the two `directoryName`s differ; `keychainService` values as above; `sessionKeyAlias` = `net.chronoflow.app.native.session` and `net.chronoflow.app.native.mock.session`.

### Task 2. `UUIDv7` and the store records in `:core:model`

Commit: `feat(model): add uuidv7 and account store records`. Ticks nothing.

**Files**
- `device/UUIDv7.kt` [F `DeviceIdentityStore.swift:12–26`]: `object UUIDv7 { fun generate(millisecondsSince1970: Long, randomBytes: ByteArray): java.util.UUID }`:
  - `ms !in 0 until (1L shl 48)` → throw `DeviceIdentityFailure.InvalidTimestamp`; `randomBytes.size != 10` → `InvalidRandomness`.
  - Bytes: `ms` big-endian in 6 bytes, then `0x70 or (r[0] and 0x0f)`, `r[1]`, `0x80 or (r[2] and 0x3f)`, then `r[3]…r[9]`. Build `UUID(msb, lsb)` from bytes 0–7 and 8–15. `toString()` is already lower-case.
- `device/DeviceIdentityFailure.kt`: `sealed class DeviceIdentityFailure : Exception(null, null, false, false)` with `data object InvalidTimestamp`, `data object InvalidRandomness`, `data object InvalidPersistedIdentity`.
- `device/DeviceIdentityPersistence.kt`: `interface DeviceIdentityPersistence { suspend fun readDeviceID(): String?; suspend fun writeDeviceID(value: String) }`.
- `store/JSONDocument.kt`: `object JSONDocument { fun requireValid(text: String): String }`. It validates with `ApiJson.response.parseToJsonElement(text)` (fragments allowed: `"1"`, `"null"` and `"\"x\""` are valid) and returns `text` unchanged. `SerializationException` → `IllegalArgumentException("invalid JSON")`. The message never contains the input. (The iOS `JSONDocument` type becomes plain `String` columns; OQ-4a-12.)
- `store/AccountRecord.kt`, `TripRecord.kt`, `SettingsRecord.kt`, `OutboxRecord.kt`: `data class`es with the iOS fields and defaults [F `Records/*.swift`]. JSON columns are `String` holding raw JSON text.
  - `AccountRecord(id: String, name: String, email: String, hasVerifiedEmail: Boolean = true, syncedAt: String? = null, walkthroughSeen: Boolean = false)`.
  - `TripRecord(id: String, card: String, adaptation: String? = null, form: String? = null, revision: String? = null, currentInputHash: String? = null, keepOffline: Boolean = false, departureUtc: String, planStartUtc: String? = null, planStartZone: String? = null, preparationDays: Int? = null, planEndUtc: String? = null, planEndDate: String? = null, deletedAt: String? = null, deletedLocally: Boolean = false)` [F `TripRecord.swift` l.6–20].
  - `SettingsRecord(section: String, value: String)`.
  - `OutboxRecord(id: String, kind: String, payload: String, createdAt: String, attempts: Int = 0, lastError: String? = null)`. `kind` stays a `String`, as in iOS; 4d maps it to `OutboxKind`.
  - No serialization annotations, no database code.

**Tests** (plain JVM)
- `device/UUIDv7Tests.uuidv7Encodes48BitTimestampVersionVariantAndRandomness` (Swift name): `0x0123_4567_89ab` with ten `0xff` → `01234567-89ab-7fff-bfff-ffffffffffff`; `0` with ten `0x00` → `00000000-0000-7000-8000-000000000000`; `1L shl 48` → `InvalidTimestamp`; empty bytes → `InvalidRandomness`. Android-only extra case in the same class: `negativeTimestampIsRejected` (`-1` → `InvalidTimestamp`).
- `store/JSONDocumentTests` (Android-only): `requireValidKeepsRawText` (`{"n":1.000}` returns `{"n":1.000}`; `"null"` is valid); `invalidJsonMessageHidesTheInput` (`"{secret"` → message does not contain `secret`).

### Task 3. Schema v1 in SQLDelight

Commit: `feat(database): add account schema v1 and local reminder tables`. Ticks plan.md "**Schema**". In the same commit:
- In the 4a "**Schema**" bullet, replace "which is copied from the iOS plan at the pin with its hash recorded" with "which is extracted from iOS `Packages/ChronoFlowKit/Sources/LocalStore/SchemaV1.swift` at the pin with its hash recorded". [F] The iOS plan's "Account schema v1 and observation" (iOS `plan.md:382`) is a table of columns, not DDL; `SchemaV1.swift` holds the DDL.
- Add the rows OQ-4a-2, -6 and -8 to plan.md "Open questions for the user", in the table's format `| OQ-4a-2 | <question> | <default> | 4a |`.

**Files**
- `core/database/build.gradle.kts`:
  ```kotlin
  sqldelight { databases { create("AccountDb") { packageName.set("net.chronoflow.core.database") } } }
  ```
  Keep the default dialect (`sqlite-3-18`); it rejects 3.24+ syntax at compile time, which rule 6 wants. If `chronoflow.sqldelight` fell back under OQ-0a-6, follow the row 0a added to plan.md and report it.
- `core/database/src/main/sqldelight/net/chronoflow/core/database/`, one file per table: `Account.sq`, `Trips.sq`, `PlanVersions.sq`, `CheckMarks.sq`, `Settings.sq`, `Drafts.sq`, `Outbox.sq`, `ScheduledReminder.sq`, `ReminderLedger.sq`.
  - Copy each iOS `CREATE TABLE` from `ios-schema-v1.sql` (below) **column for column, in iOS order, with iOS types, constraints and defaults**, with one change: `DEFAULT true` → `DEFAULT 1` and `DEFAULT false` → `DEFAULT 0` (OQ-4a-2). The DDL is in Appendix A.1.
  - Indexes: `CREATE INDEX outbox_created_at_id ON outbox(created_at, id);` in `Outbox.sq`; `CREATE INDEX trips_deleted_at ON trips(deleted_at);` in `Trips.sq`.
  - SQLDelight's default types: `TEXT` → `String`, `INTEGER` → `Long`. **No `AS Boolean` or other adapters**, so the created DDL equals the `.sq` text; the mapping in Task 6 converts.
  - Android-only tables (OQ-4a-6):
    ```sql
    CREATE TABLE scheduled_reminder (
      identifier TEXT PRIMARY KEY NOT NULL,
      request_code INTEGER NOT NULL UNIQUE,
      send_at INTEGER NOT NULL,
      title TEXT NOT NULL,
      body TEXT NOT NULL,
      trip_id TEXT NOT NULL,
      block_id TEXT NOT NULL,
      input_hash TEXT NOT NULL,
      member_keys TEXT NOT NULL,
      account_id TEXT
    );
    CREATE TABLE reminder_ledger (
      id INTEGER PRIMARY KEY,
      trip_id TEXT NOT NULL,
      input_hash TEXT NOT NULL,
      send_at_ms INTEGER NOT NULL,
      member_keys TEXT NOT NULL
    );
    CREATE INDEX reminder_ledger_send_at_ms ON reminder_ledger(send_at_ms);
    ```
    `send_at` and `send_at_ms` are epoch milliseconds; `member_keys` is a JSON array of strings; `account_id` is nullable, as in iOS `ReminderRequest.accountID?`. 4a adds no queries for these two tables. **[R] Until the first release, 12b may edit this v1 DDL in place; no Android database exists in the field.**
  - Named queries for Task 6 only (four tables). Labels, so 4d and 9b can tell them apart from theirs:
    - `Account.sq`: `selectAccount` (`WHERE id = ?`), `updateAccount` (every non-key column, `WHERE id = ?`), `insertAccount` (every column), `changes: SELECT changes();`.
    - `Trips.sq`: `selectTrip`, `selectTrips` (`ORDER BY departure_utc, id`), `updateTrip`, `insertTrip`, `deleteTrip`.
    - `Settings.sq`: `selectSetting`, `selectSettings` (`ORDER BY section`), `updateSetting`, `insertSetting`, `deleteSetting`.
    - `Outbox.sq`: `selectOutboxRow`, `selectPendingOutbox` (`ORDER BY created_at, id`), `updateOutboxRow`, `insertOutboxRow`, `deleteOutboxRow`.
    - `PlanVersions.sq`, `CheckMarks.sq`, `Drafts.sq`: DDL only (4d and 9b add their queries).
- `core/database/src/test/resources/ios-schema-v1.sql`, produced with exactly:
  ```sh
  git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:Packages/ChronoFlowKit/Sources/LocalStore/SchemaV1.swift \
    | sed -n '7,38p' | sed 's/^                //' > core/database/src/test/resources/ios-schema-v1.sql
  ```
  SHA-256 must be `6014a032b63a97f72d24858d4c08f1fcb496bb0a44dcd70af25b70688ee4b583` (verified for this brief).
- `core/database/src/test/resources/rn-schema-v1.json`: a byte copy of `T/LocalStoreTests/Fixtures/rn-schema-v1.json`, SHA-256 `937c93693e8dc17c60572404a6cb0825860bb6aafb9840b3bde0ee9b2e8dd54a` (verified).
- `core/database/src/test/resources/SOURCE.md`: both source paths, the pin, both hashes and the `sed` command.

**Tests** (`core/database/src/test/kotlin/net/chronoflow/core/database/`, plain JVM, on `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` plus `AccountDb.Schema.create(driver)`; the file-backed factory arrives in Task 4)
- `SchemaParityTest`:
  - `resourceHashesMatchThePin`: SHA-256 of both resources equals the values above.
  - `iosTablesMatchVerbatim`: split `ios-schema-v1.sql` into its 7 `CREATE TABLE` statements. Read `SELECT name, sql FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'`. Normalize both sides: collapse whitespace runs to one space, drop spaces next to `(`, `)` and `,`, strip a trailing `;`, lower-case, then `default true` → `default 1` and `default false` → `default 0`. Assert equality per table.
  - `onlyTheDeclaredExtrasExist`: the table set is the 7 iOS tables plus exactly `scheduled_reminder` and `reminder_ledger`; the index set (`type = 'index' AND name NOT LIKE 'sqlite_autoindex%'`) is exactly `outbox_created_at_id`, `trips_deleted_at`, `reminder_ledger_send_at_ms`.
  - `booleanDefaultsAreIntegers`: `PRAGMA table_info` gives `dflt_value` `"1"` for `account.has_verified_email` and `drafts.dirty`, and `"0"` for `account.walkthrough_seen`, `trips.keep_offline` and `trips.deleted_locally`. Insert an `account` row without those two columns; they read back as `Long` `1` and `0`.
- `SqlSafetyTest`:
  - `noSqlNewerThanApi29`: scan every `.sq` file under `src/main/sqldelight` (declare that directory as a test input in Gradle, so the task reruns on change). Fail on the case-insensitive patterns `OR\s+REPLACE`, `\bREPLACE\s+INTO`, `ON\s+CONFLICT`, `\bRETURNING\b`, `DEFAULT\s+(TRUE|FALSE)`, `\bOVER\s*\(`.
  - `scannerRejectsPlantedViolations`: one string per pattern through the same scanner function; each fails.
  - Note in a KDoc: 9b adds a `drafts` query with `INSERT OR REPLACE` (no FK children; allowed). When 9b lands, it narrows the `OR REPLACE` pattern to every file except `Drafts.sq`. Leave a `// 9b:` comment at the pattern.

### Task 4. `AccountDatabase` open/close and the driver factories

Commit: `feat(database): open per-account databases under no-backup storage`. Ticks nothing.

**Files** (`core/database/src/main/kotlin/net/chronoflow/core/database/`)
- `StoreFailure.kt` [F `AccountDatabase.swift:5–7`]:
  ```kotlin
  sealed class StoreFailure(cause: Throwable? = null) : Exception(null, cause) {
      data object Closed : StoreFailure()
      data object AlreadyOpen : StoreFailure()
      data object InvalidAccountID : StoreFailure()
      class Unavailable(cause: Throwable? = null) : StoreFailure(cause)
      class Migration(cause: Throwable? = null) : StoreFailure(cause)
      class StorageAttributes(cause: Throwable? = null) : StoreFailure(cause)
  }
  ```
  Tests compare with `isInstanceOf`. The message stays null: no path or SQL text goes into it.
- `AccountSql.kt`: `class AccountSql internal constructor(val queries: AccountDb, val driver: SqlDriver)`. A `read`, `write` or `observe` body receives it. Main code uses `queries` only. `driver` exists for `PRAGMA` reads and test seeding; a raw write through `driver` skips the change notifications, so main code never makes one.
- `AccountDriverFactory.kt`: `fun interface AccountDriverFactory { fun open(file: File): SqlDriver }`. Contract (port of `AccountDatabase.swift:44–66`):
  - Returns an **opened, configured and migrated** driver (`PRAGMA user_version` = 1, every v1 table present).
  - `PRAGMA foreign_keys` not `1` → close the driver, throw `Unavailable` (iOS `prepareDatabase` failure inside the pool constructor).
  - `PRAGMA journal_mode` not `wal` → close the driver, throw `Migration` (iOS l.56–66 maps it inside the migration `catch`).
  - Cannot open, or not a database → `Unavailable(cause)`. Schema creation failed → `Migration(cause)`, with the creation rolled back.
  - **Never deletes, truncates or rewrites an existing file.**
- `AndroidAccountDriverFactory.kt`: `class AndroidAccountDriverFactory(private val context: Context, private val schema: SqlSchema<QueryResult.Value<Unit>> = AccountDb.Schema) : AccountDriverFactory`:
  - `AndroidSqliteDriver(schema, context, file.absolutePath, callback = Callback(schema))`. `ContextImpl.getDatabasePath` accepts an absolute path, so the file lands exactly there.
  - `Callback : AndroidSqliteDriver.Callback(schema)` overrides:
    - `onConfigure(db)`: `db.enableWriteAheadLogging()`; `db.setForeignKeyConstraintsEnabled(true)` (applies to every pooled connection).
    - `onCreate(db)`: `super.onCreate(db)`, mapping `SQLException`/`RuntimeException` to `StoreFailure.Migration(cause)`. `SQLiteOpenHelper` runs `onCreate` and the `user_version = 1` write in one transaction, so a throw rolls both back.
    - `onUpgrade`: unreachable at version 1; throw `Migration()`. `onDowngrade`: throw `Migration()`.
    - `onCorruption(db)`: **do nothing.** The default deletes the file.
  - `AndroidSqliteDriver` opens lazily. Force it by running the two PRAGMA checks right away. Map `android.database.SQLException` (including `SQLiteCantOpenDatabaseException` and `SQLiteDatabaseCorruptException`) to `Unavailable`. Let `StoreFailure.Migration` through; it may arrive wrapped (androidx's internal `CallbackException`), so walk the `cause` chain to find it.
- `core/database/src/test/kotlin/net/chronoflow/core/database/testing/` (test source set only):
  - `JdbcAccountDriverFactory(schema: SqlSchema<QueryResult.Value<Unit>> = AccountDb.Schema) : AccountDriverFactory`: `JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true"); put("journal_mode", "WAL") })` (applied to every connection; the file-URL driver keeps one connection per thread). When `PRAGMA user_version` is 0: inside one `driver.newTransaction()`, `schema.create(driver)` then `PRAGMA user_version = 1`; on any throw roll back and throw `Migration(cause)`. The same PRAGMA checks as above. `java.sql.SQLException` → `Unavailable`.
  - `TestRoots.kt`: `fun temporaryStoreRoot(): File = Files.createTempDirectory("store").toFile()`; tests delete it in `@After`.
  - `Accounts.kt`: `accountA`, `accountB`.
  - `SqlProbe.kt`: `fun AccountSql.long(sql: String): Long?`, `fun AccountSql.string(sql: String): String?`, `fun AccountSql.exec(sql: String)` over `driver.executeQuery`/`driver.execute`. Tests only.
- `AccountDatabase.kt`:
  ```kotlin
  class AccountDatabase(
      private val root: File,                        // production: noBackupFilesDir, resolved off the main thread
      val namespace: StorageNamespace = StorageNamespace.Api,
      private val driverFactory: AccountDriverFactory,
      private val io: CoroutineDispatcher,           // production: Dispatchers.IO
  ) {
      private val writer = io.limitedParallelism(1)
      private val lifecycle = ReentrantReadWriteLock()
      @Volatile var accountID: AccountID? = null; private set   // null after close
      private var boundID: AccountID? = null                     // never cleared
      val observationCount: Int                                  // live observers
      fun directory(id: AccountID): File = File(root, "${namespace.directoryName}/Accounts/${id.rawValue}")
      fun file(id: AccountID): File = File(directory(id), "account-${id.rawValue}.db")
      suspend fun open(accountID: String)
      suspend fun <T> read(body: (AccountSql) -> T): T
      suspend fun <T> write(body: (AccountSql) -> T): T            // unguarded; Task 5 adds the guarded forms
      suspend fun close()
  }
  ```
  Port of `AccountDatabase.swift` l.31–91 and l.165–172:
  - `open`, inside `withContext(writer)`:
    1. `AccountID.of(value)` throwing `AccountID.InvalidUUID` → `InvalidAccountID`. Nothing touches disk before this check.
    2. `boundID != null && boundID != id` → `AlreadyOpen`.
    3. Already open: same id → return (no-op); another id → `AlreadyOpen`.
    4. `directory(id).mkdirs()` false and the directory does not exist → `StorageAttributes`.
    5. `driverFactory.open(file(id))`; a `StoreFailure` passes through.
    6. Under the lifecycle write lock: store the driver and `AccountDb(driver)`; set `accountID = id` and `boundID = id`.
  - `read`: `withContext(io) { lifecycle.read { val sql = current ?: throw StoreFailure.Closed; body(sql) } }`. Not transacted (OQ-4a-5).
  - `write(body)`: `withContext(writer) { lifecycle.read { val sql = current ?: throw Closed; sql.queries.transactionWithResult { body(sql) } } }`.
  - `close`: `withContext(io) { lifecycle.write { … } }`: (1) close every observer channel (Task 5); (2) `driver.close()`; (3) null the driver and `AccountDb`, set `accountID = null`. A second `close()` is a no-op. The write lock waits for in-flight reads and writes.

**Tests**
- `AccountDatabaseTests` (plain JVM, `JdbcAccountDriverFactory`, `io = Dispatchers.IO`; Swift names):
  - `rejectsInvalidPaths`, `Parameterized`: `""`, `"../account"`, `"/tmp/escape"`, `"account"`, `"$accountA/x"`, `" $accountA"`, `"{$accountA}"`. Each throws `InvalidAccountID`; the temporary root is still empty.
  - `walForeignKeysIsolationAndBackupExclusion`: for each `StorageNamespace` × `[accountA, accountB]`, a new `AccountDatabase`:
    - `open(id.uppercase())` succeeds; `directory(AccountID.of(id))` ends with `<directoryName>/Accounts/<id>` and lies under `root` (the "backup exclusion" half; the real `noBackupFilesDir` check is in `StoragePathsTest`);
    - `journal_mode` is `wal`;
    - six concurrent `read`s (`async(Dispatchers.IO)`) each see `foreign_keys` = 1 (every pooled connection);
    - a `write` sees `foreign_keys` = 1; `count(*) FROM account` is 0;
    - insert an account row (`exec`), then `open(the other id)` throws `AlreadyOpen`;
    - `close()` twice succeeds, then `read { }` throws `Closed`.
  - `failedMigrationRollsBackWithoutDeletingExistingFile`:
    1. Create `ChronoFlow/Accounts/<accountA>/account-<accountA>.db` with plain JDBC holding `CREATE TABLE sentinel (value TEXT); INSERT INTO sentinel VALUES ('preserved')`.
    2. Open with `JdbcAccountDriverFactory(schema = brokenSchema)`. `brokenSchema` is an `SqlSchema` whose `create` executes `CREATE TABLE partial (id TEXT)` and then throws `StoreFailure.Migration()`. `open` throws `Migration`.
    3. The file exists; there is no `partial` table; `sentinel` still holds `preserved`; `user_version` is 0.
    4. A fresh `AccountDatabase` with the real schema opens it and closes it.
    5. A second root (`root/corrupt`) whose db file holds `"not a sqlite database"`: `open` throws `Unavailable`; the bytes are unchanged.
- `SchemaV1Tests` (plain JVM; Swift names):
  - `matchesFinalRNSchemaAndReopensIdempotently`: twice, open accountA and compare `PRAGMA table_info`/`foreign_key_list` per table with `rn-schema-v1.json`, with the substitutions in Appendix A.2; close.
  - `foreignKeysCascadeAndRollbackRetainsUnrelatedCheckMarks`: through `exec` inside `write`, insert a trip (`id, card, departure_utc` = `trip`, `{}`, `date`), a plan version and a check mark (`trip, hash, sleep, date, 0, done, date`). A write that runs `DELETE FROM trips` and then throws rolls back (1 plan version left). A committed `DELETE FROM trips` leaves 0 plan versions and 1 check mark.
- `AccountStorageLifecycleTests` (Robolectric `@Logic`, `@RunWith(AndroidJUnit4::class) @Config(sdk = [36])`, `AndroidAccountDriverFactory`, `root = context.noBackupFilesDir`):
  - `directoryDatabaseAndSidecarsRetainProtectionAndBackupExclusionAfterReopen` (Swift `test…` name without the prefix): twice: open accountA; write an account row (plain `INSERT` on the first pass, `UPDATE` on the second; never `INSERT OR REPLACE`); the directory, `account-<id>.db`, `-wal` and `-shm` exist under `noBackupFilesDir/ChronoFlow/Accounts/<id>/`; `foreign_keys` = 1; `journal_mode` = `wal`; close. On the second pass the row from the first is still there. (iOS file-protection attributes have no Android equivalent; `noBackupFilesDir` is the exclusion.)
  - Android-only: `everyPooledConnectionHasForeignKeys` (six concurrent `read`s on `Dispatchers.IO` each see `foreign_keys` = 1 on the framework driver's WAL connection pool; the JVM test only covers JDBC); `corruptFileIsKeptAndReportedUnavailable` (step 5 above, on the framework driver); `failedCreationLeavesNoPartialTable` (the broken schema throws `Migration`; no `partial` table; `user_version` 0); `frameworkSqliteIsAtLeastTheApi29Floor` (`SELECT sqlite_version()` ≥ 3.22.0; this guards Robolectric's native library, not the device).

### Task 5. Guarded writes and observation

Commit: `feat(database): guard writes by epoch and permit and observe commits`. Ticks plan.md "**Account database factory**". In the same commit:
- In A5 and in the 4a "Account database factory" bullet, replace `transaction { epoch.whileValid { permit?.whileValid { body() } } }` with `epoch.whileValid { permit?.whileValid { transaction { body() } } }` (OQ-4a-1).
- Replace "Reads are `asFlow().mapToList(io)` with `conflate()`" (A5) and "Reads as `asFlow().mapToList(io).conflate()`" (4a bullet) with "Reads go through `AccountDatabase.observe` (SQLDelight table listeners, conflated, fetched on io; completes on close)" (OQ-4a-4).
- Add the rows OQ-4a-1, -4, -5 to plan.md "Open questions for the user", in the table's format `| OQ-4a-1 | <question> | <default> | 4a |`. Task 3 adds -2, -6, -8; Task 7 adds -9; Task 8 adds -7, -10, -17; Task 10 adds every row left over.

**Files** — `AccountDatabase.kt` gains:
- `suspend fun <T> write(permit: SessionMutationPermit?, body: (AccountSql) -> T): T` [F l.99–114]: `permit == null` → `write(body)`; else `withContext(writer) { lifecycle.read { val sql = current ?: throw Closed; permit.whileValid { sql.queries.transactionWithResult { body(sql) } } } }`.
- `suspend fun validate(epoch: SyncEpoch)` [F l.93–97]: `epoch.check()`; `accountID != epoch.accountID` → `InvalidAccountID`; closed → `Closed`.
- `suspend fun <T> write(epoch: SyncEpoch, permit: SessionMutationPermit? = null, body: (AccountSql) -> T): T` [F l.116–141]:
  1. `epoch.check()`; account mismatch → `InvalidAccountID`; closed → `Closed`.
  2. `withContext(writer) { lifecycle.read { val sql = …; epoch.whileValid { if (permit != null) permit.whileValid { tx(sql) } else tx(sql) } } }`, where `tx` = `sql.queries.transactionWithResult { body(sql) }`.
  3. Wrap it: `try { … } catch (e: CancellationException) { if (!currentCoroutineContext().isActive) epoch.invalidate(); throw e }`. Only the **caller's** cancellation invalidates the epoch (iOS `onCancel`); a `StaleSessionException` from an invalid permit leaves the epoch valid.

  The lock order is **epoch, then permit, then transaction**, so both locks are held through COMMIT (iOS l.124–137). Never put the locks inside the transaction.
- `fun <T> observe(vararg tables: String, fetch: (AccountSql) -> T): Flow<T>` [F l.143–161]. Not `suspend` (iOS `observe` is synchronous and throws):
  - Closed at call time → throw `Closed` eagerly.
  - Otherwise return a `callbackFlow<Unit>` with `.buffer(Channel.CONFLATED)`, mapped through a fetch:
    - On collection, register the producer channel in the observer set (`observationCount` counts it).
    - Send one initial `Unit`, then register `Query.Listener { trySend(Unit) }` with `driver.addListener(*tables, listener = it)`.
    - `awaitClose { driver.removeListener(*tables, listener = it); unregister() }`.
    - For each signal, fetch on `io`: `lifecycle.read { current?.let { Box(fetch(it)) } }`. Null (closed) emits nothing. Emit `box.value`. The private `Box` wrapper lets a legitimately null value (an absent account) through; never `mapNotNull` on `T` itself.
  - `close()` calls `channel.close()` on every registered observer, so collectors **complete normally** (iOS `lifetime.cancel()` finishes the stream). A collector cancelled by its owner unregisters through `awaitClose`.
  - SQLDelight notifies listeners only after the outermost transaction commits, and never for a rollback: that is the "committed values only" property (iOS `ValueObservation`). Keys are table names (`"account"`, `"trips"`, `"settings"`, `"outbox"`, `"plan_versions"`, `"check_marks"`, `"drafts"`).
  - Fetch errors propagate to the collector (iOS `lifetime.fail`).

**Tests** (plain JVM, `JdbcAccountDriverFactory`, `runTest(timeout = 60.seconds)`; Android-only names)
- `AccountDatabaseGuardTests`:
  - `writeAfterItsEpochEndedIsRefused`: open accountA; `epoch.invalidate()`; `write(epoch) { insert an outbox row }` throws `StaleSessionException`; outbox is empty. **This is the plan acceptance case.**
  - `childEpochOfAnEndedParentIsRefused`.
  - `epochForAnotherAccountIsRejected`: an epoch for accountB on accountA's database → `InvalidAccountID`.
  - `invalidPermitRefusesAndLeavesNoRow`: `write(permit)` after `permit.invalidate()` throws `StaleSessionException`; no row.
  - `invalidPermitDoesNotEndTheEpoch`: `write(epoch, invalidPermit)` throws `StaleSessionException`; `epoch.isValid` is still true.
  - `invalidationWaitsForTheAdmittedCommit`: (1) a `write(epoch)` body inserts a row, then blocks on latch L1; (2) a plain thread calls `epoch.invalidate()`; (3) poll that thread's `state` until `WAITING` (spin, no sleep); (4) release L1: the write returns, the row is committed, the invalidating thread finishes; (5) the next `write(epoch)` is refused.
  - `cancelledEpochWriteInvalidatesTheEpoch`: cancel the job running `write(epoch)` while its body waits on a latch, release the latch; then `epoch.isValid` is false.
  - `writesAreSerial`: 20 concurrent writes, each reading and incrementing a counter row in its transaction, end at 20; an `AtomicInteger` of active bodies never exceeds 1.
  - `rollbackLeavesNoRow`: a body that inserts and then throws `IllegalStateException` → the exception propagates and no row remains.
- `ObservationTests` (Turbine):
  - `fetchRunsOnTheInjectedDispatcher`: `io = Executors.newSingleThreadExecutor { Thread(it, "store-io") }.asCoroutineDispatcher()`; the fetch records its thread name, which is `store-io`. (A JVM test has no main thread; `StoreThreadingSmokeTest` in Task 10 is the device check.)
  - `initialValueThenOnlyCommittedValues`: the first emission is the empty state; a write that inserts a half row and then updates it in one transaction emits only the final value; a rolled-back write emits nothing.
  - `nullValuesAreEmitted`: `observe("account") { it.queries.accountQueries.selectAccount(accountA).executeAsOneOrNull() }` emits `null` first. (`AccountDb` exposes one `<File>Queries` property per `.sq` file: `accountQueries`, `tripsQueries`, `settingsQueries`, `outboxQueries`, …. Task 5 tests write through these generated queries, not `exec`: a raw `driver.execute` sends no change notification, and `RecordMapping` arrives only in Task 6.)
  - `closeCompletesTheFlowAndClearsObservers` (`awaitComplete()`, then `observationCount == 0`); `observeOnAClosedDatabaseThrowsClosed`; `cancellingTheCollectorUnregisters`.

### Task 6. Record mapping for the four records

Commit: `feat(database): map account, trip, settings and outbox records`. Ticks nothing.

**Files** — `core/database/src/main/kotlin/net/chronoflow/core/database/store/RecordMapping.kt`, public extension functions on `AccountSql`, each to be called inside `AccountDatabase.read`/`write` (4d builds its repositories on them):
- `fetchAccount(id: String): AccountRecord?`, `saveAccount(record)`.
- `fetchTrip(id): TripRecord?`, `fetchTrips(): List<TripRecord>` (`departure_utc, id`), `saveTrip(record)`, `deleteTrip(id)`.
- `fetchSetting(section): SettingsRecord?`, `fetchSettings(): List<SettingsRecord>` (`section`), `saveSetting(record)`, `deleteSetting(section)`.
- `fetchOutboxRow(id): OutboxRecord?`, `fetchPendingOutbox(): List<OutboxRecord>` (`created_at, id`), `saveOutboxRow(record)`, `deleteOutboxRow(id)`.
- Rules:
  - **Save** = `update…` then `changes()`; when 0, `insert…` (rule 6). It never cascades.
  - Every non-null JSON column is passed through `JSONDocument.requireValid` **before** any SQL runs, so invalid JSON never enters a row (iOS `JSONDocument.init`). Fetches do not re-validate (OQ-4a-12).
  - Booleans `1L`/`0L` ↔ `true`/`false`; `Int` ↔ `Long` with `toInt()`/`toLong()`.
  - Text is stored as received: never re-encode a JSON string.

**Tests** (plain JVM, `JdbcAccountDriverFactory`)
- `RecordMappingTests`:
  - `invalidJSONCannotEnterARecord` (Swift name; the record half of iOS `RecordsTests`): `JSONDocument.requireValid("{bad")` throws; `requireValid("[null,1,{}]") == "[null,1,{}]"`; `saveSetting(SettingsRecord("s", "{bad"))` throws and leaves no row.
  - `fourRecordsRoundTripUnknownJsonAndNulls` (Android-only; the Swift `allRecordsRoundTripUnknownJSONNullsAndCompositeKeys` moves to 4d): with `json = {"future":{"big":123456789012345678901234567890,"list":[null,true,"é"]},"n":1.000}`, save and fetch `AccountRecord(accountA, "Dutch ' name", "a@example.test", hasVerifiedEmail = false)`, `TripRecord("trip'", card = json, adaptation = json, form = json, revision = json, currentInputHash = "hash", keepOffline = true, departureUtc = "date", planStartUtc = "start", planStartZone = "Europe/Amsterdam", preparationDays = 2, planEndUtc = "end", planEndDate = "day", deletedAt = "deleted", deletedLocally = true)`, `SettingsRecord("futureSection", json)`, `OutboxRecord("id", "futureKind", json, "now", 2, "offline")`; each equals the original, and `SELECT card FROM trips` is `json` byte for byte. Then `TripRecord("nullable", card = "null", departureUtc = "later")` round-trips with every nullable field null.
  - `outboxOrdersByTimestampThenIdAndBindsKeys` (Android-only; mirrors the outbox half of `repositoriesBindKeysAndOrderOutboxByTimestampThenID`): ids `b`, `a`, `c` with `createdAt` `1`, `1`, `0` → `[c, a, b]`; `deleteOutboxRow("' OR 1=1 --")` leaves 3; `deleteOutboxRow("a")` → `[c, b]`.
  - `saveNeverCascades`: insert a trip, a plan version and a check mark (`exec`), then `saveTrip` with a new `card`; the plan version is still there.
  - `saveUpdatesInPlace`: saving the same key twice leaves one row with the second values.

### Task 7. `KeystoreSessionStore`

Commit: `feat(datastore): store the session in an aes-gcm keystore file`. Ticks plan.md "**KeystoreSessionStore**". In the same commit, add the row OQ-4a-9 to plan.md "Open questions for the user".

**Files** (`core/datastore/src/main/kotlin/net/chronoflow/core/datastore/`)
- `SessionCipher.kt`:
  ```kotlin
  interface SessionCipher {
      fun encrypt(alias: String, plaintext: ByteArray): Sealed       // creates the key if absent
      fun decrypt(alias: String, sealed: Sealed): ByteArray
      fun deleteKey(alias: String)
      class Sealed(val iv: ByteArray, val ciphertext: ByteArray)
  }
  class PermanentSessionKeyFailure(cause: Throwable?) : Exception(null, cause)   // "delete and sign out"
  ```
- `AndroidKeystoreSessionCipher.kt`:
  - Provider `AndroidKeyStore`. The key: `KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT).setBlockModes(BLOCK_MODE_GCM).setEncryptionPaddings(ENCRYPTION_PADDING_NONE).setKeySize(256).build()`. No `setUserAuthenticationRequired`; randomized encryption stays on (the Keystore picks the IV); no StrongBox; no `setUnlockedDeviceRequired` (OQ-4a-9).
  - `Cipher.getInstance("AES/GCM/NoPadding")`, `updateAAD(alias.encodeToByteArray())` on both sides; decrypt with `GCMParameterSpec(128, iv)`. The IV must be 12 bytes.
  - Throw `PermanentSessionKeyFailure` on `KeyPermanentlyInvalidatedException`, `UnrecoverableKeyException`, `AEADBadTagException` (any `BadPaddingException`), and a missing alias on decrypt.
  - Any other `GeneralSecurityException`, `ProviderException` or `KeyStoreException` propagates unchanged (transient).
  - `encrypt` hitting `KeyPermanentlyInvalidatedException` deletes the alias, generates a new key and retries once.
- `KeystoreSessionStore.kt`: `class KeystoreSessionStore(val file: File, private val alias: String, private val cipher: SessionCipher, private val io: CoroutineDispatcher) : SessionStore`, plus `companion fun forNamespace(root: File, namespace: StorageNamespace, cipher: SessionCipher, io: CoroutineDispatcher) = KeystoreSessionStore(File(File(root, namespace.directoryName), "session.bin"), namespace.sessionKeyAlias, cipher, io)`. 4e passes its own file and alias for UI tests (`ChronoFlowMock.uitest.<id>`).
  - Writes go through `android.util.AtomicFile(file)`. Format: `[0x01][12-byte IV][ciphertext with its 16-byte tag]`.
  - A `Mutex` serializes the three operations, each inside `withContext(io)`.
  - `file` is public (no secret; `StoragePathsTest` in `:app` reads it, and `internal` is invisible across modules).
  - `read()`: read with `AtomicFile(file).readFully()` (it recovers a leftover backup). `FileNotFoundException` → `null`; catch it **before** the general `IOException`, which it extends. Any other `IOException` while reading → throw (keep the file); fewer than 29 bytes, or a first byte other than `0x01` → delete the file, `null`; `PermanentSessionKeyFailure` → delete the file **and** the alias, `null`; anything else → throw (keep the file).
  - `write(data)`: `parentFile.mkdirs()`, encrypt, `startWrite`/`write`/`finishWrite`; on `IOException` call `failWrite` and rethrow.
  - `delete()`: `AtomicFile.delete()` (it removes the base and the backup). The key stays.
  - Never logs; never puts bytes in an exception.

**Tests**
- `core/datastore/src/test/…/SoftwareSessionCipher.kt`: a `javax.crypto` AES-GCM fake with in-memory keys per alias, the same IV/AAD/tag rules, a `failNext: Throwable?` hook and `keys` exposed for assertions. (Robolectric has no AndroidKeyStore provider.)
- `KeystoreSessionStoreFileTests` (Robolectric `@Logic`, `@Config(sdk = [36])`, `SoftwareSessionCipher`, a temporary root):
  - `roundTripWritesTheVersionedFormat`: byte 0 is `1`; size is 1 + 12 + plaintext + 16; the plaintext bytes are not in the file.
  - `missingFileReadsNull`; `truncatedOrUnknownVersionIsDeleted`; `permanentKeyFailureDeletesFileAndAlias`.
  - `transientFailureThrowsAndKeepsTheFile` (`failNext = ProviderException()`, then an `IOException`; the file bytes are unchanged both times).
  - `deleteRemovesTheFileOnly`; `namespacesAreIsolated` (Api and Mock use different files and aliases).
  - `wrongAliasCannotDecrypt`: a file copied from Mock to Api fails the AAD check → deleted, `null`.
- `app/src/androidTest/kotlin/net/chronoflow/app/store/KeystoreSessionStoreTest.kt` (`@Smoke`, the real `AndroidKeystoreSessionCipher`, a temporary root under `cacheDir` resolved in `@Before`, aliases `test.<uuid>.session` deleted in `@After`; **at most 6 tests**):
  1. `hostedKeystoreRoundTripAndMalformedDeletion` (Swift `testHostedKeychainRoundTripAndMalformedDeletion`, [F] `AppTests/KeychainSessionStoreTests.swift:9–29`): `delete()`, then `read()` is null; `write(StoredSession("test", "account").encode())` → `loadSession()` is `SignedIn("account")`; `write("invalid".encodeToByteArray())` → `loadSession()` is `SignedOut`, then `read()` is null and the file is gone.
  2. `legacyShapeKeyPropertiesAndSourceNamespaces` (Swift `testLegacyShapeAccessibilityAndSourceNamespaces`, [F] same file l.30–63): two stores `api`/`mock` with aliases `test.<uuid>.api.session`/`test.<uuid>.mock.session` and their own files. `api.write` of the raw `{"token":"legacy-native","accountId":"019a562b-8000-7000-8000-000000000001"}` → `api.readStoredSession()?.token == "legacy-native"`; `mock.readStoredSession()` is null; `mock.write` of `{"token":"mock","accountId":"019a562b-8000-7000-8000-000000000002"}` → `api` still reads `legacy-native`. The api key's `KeyInfo` (the Android stand-in for the Keychain accessibility check) has `keySize == 256`, `isUserAuthenticationRequired == false`, purposes encrypt and decrypt, block mode GCM. `api.delete()` → `mock` still reads `mock`.
  3. `lostKeyDeletesTheSession`: delete the alias behind the store's back → `read()` is null and the file is deleted.
  4. `tamperedFileDeletesTheSession`: flip one ciphertext byte → null, file deleted.
  5. `swappedAliasFileDeletesTheSession`: copy store B's file to store A's path → null.

  Five tests; a sixth only if one of these must split.

### Task 8. Device identity, device preferences, the device-config mirror and the wipe marker

Commit: `feat(datastore): add device identity, device preferences and wipe marker`. Ticks plan.md "**Ports**" and "**Device files**". In the same commit, rename `SessionEpoch` to `SyncEpoch` in the 4a "Ports" bullet and add OQ-4a-7 (and OQ-4a-10, -17).

**Files** (`core/datastore/src/main/kotlin/net/chronoflow/core/datastore/`)
- `DeviceIdentityStore.kt` [F `DeviceIdentityStore.swift:28–95`]: `class DeviceIdentityStore(private val persistence: DeviceIdentityPersistence, private val clock: () -> Double = { System.currentTimeMillis() / 1000.0 }, private val randomBytes: () -> ByteArray = { ByteArray(10).also(SecureRandom()::nextBytes) })`. `clock` returns seconds since 1970.
  - `@Volatile private var cached: String? = null`; `private val mutex = Mutex()`.
  - `suspend fun identity(): String`: `currentCoroutineContext().ensureActive()`; return `cached` if set; otherwise `mutex.withLock { cached ?: load().also { cached = it } }`. A failure is not cached, so the next call retries. Concurrent callers wait on the mutex and then see `cached`: exactly one write (OQ-4a-17).
  - `load()`:
    - `persistence.readDeviceID()` non-null: it must be 36 characters, match `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`, have version nibble `7` (index 14) and a variant character in `89abAB` (index 19). Otherwise throw `DeviceIdentityFailure.InvalidPersistedIdentity` and never overwrite it. Return it lower-cased.
    - Otherwise `ms = floor(clock() * 1000)`; it must be finite, ≥ 0 and < 2^48, else `InvalidTimestamp`. Then `value = UUIDv7.generate(ms.toLong(), randomBytes()).toString()`; `persistence.writeDeviceID(value)`; return `value`.
  - `companion fun deviceName(name: String, model: String): String` (Swift `DeviceIdentityStore.deviceName`): `trimmed = name.trim()`, else `model.trim()` when empty. Walk grapheme clusters with `java.text.BreakIterator.getCharacterInstance(Locale.ROOT)`; keep the longest prefix of whole clusters whose cumulative `codePointCount` is ≤ 255.
- `AndroidDeviceName.kt`: `suspend fun androidDeviceName(context: Context, io: CoroutineDispatcher): String` = `withContext(io) { DeviceIdentityStore.deviceName(Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: "", Build.MODEL ?: "") }`. 4b/5a send it as the login `device_name` (D22).
- `DeviceConfigFile.kt`: **move** `app/src/main/kotlin/net/chronoflow/app/config/DeviceConfigFile.kt` here with `git mv`, keeping it an `object` with the same members (`FILE_NAME = "device-config"`, `readForLaunch(context, sdkInt)` under the StrictMode allowance and the 65-byte read, `suspend fun write(context, config, io)` through `AtomicFile`). Add `suspend fun update(context: Context, io: CoroutineDispatcher, transform: (DeviceConfig) -> DeviceConfig)`: read-modify-write under a private `Mutex`, inside `withContext(io)`; it reads with the same 65-byte limit (no StrictMode allowance needed on io). Update the imports in `:app` (`ConfigurationPolicy`, `TestSeeds`, 0e's tests). The encoded size stays ≤ 64 bytes (0e `DeviceConfig.encode`).
- `DevicePreferences.kt`: `class DevicePreferences(private val context: Context, private val dataStore: DataStore<Preferences>, private val io: CoroutineDispatcher) : DeviceIdentityPersistence`.
  - Keys (A5): `api.deviceId` (String), `cf.settings.language` (String, `LanguageChoice.raw`), `cf.settings.theme` (String, `ThemeChoice.raw`), `notificationPermissionRequested` (Boolean).
  - `val language: Flow<LanguageChoice>` and `val theme: Flow<ThemeChoice>`: `fromRaw(…) ?: System` (an unknown or absent value is `System`, iOS `SettingsDevicePreferences`); `val notificationPermissionRequested: Flow<Boolean>` (absent → false). Each `distinctUntilChanged()`. The `data` flow uses `catch { if (it is IOException) emit(emptyPreferences()) else throw it }`.
  - `suspend fun setLanguage(choice: LanguageChoice, sdkInt: Int = Build.VERSION.SDK_INT)`: write DataStore first; then, when `sdkInt < 33`, `DeviceConfigFile.update(context, io) { it.copy(language = choice) }`.
  - `suspend fun setTheme(choice: ThemeChoice, sdkInt: Int = Build.VERSION.SDK_INT)`: the same with `theme` and the threshold `sdkInt < 31`.
  - `suspend fun setNotificationPermissionRequested(value: Boolean)`.
  - `suspend fun reconcileDeviceConfig(sdkInt: Int = Build.VERSION.SDK_INT)`: when `sdkInt < 33`, rewrite `device-config` from DataStore (language always; theme only when `sdkInt < 31`, else keep the file's value). This repairs a crash between the two writes. 10a/10b call it at start; 4a only provides it.
  - `readDeviceID()`: `dataStore.data.first().asMap().entries.firstOrNull { it.key.name == "api.deviceId" }?.value`: null → null; a `String` → it; any other type → throw `DeviceIdentityFailure.InvalidPersistedIdentity`. `writeDeviceID(value)`: `edit { it[stringPreferencesKey("api.deviceId")] = value }`.
  - `companion object { const val FILE_NAME = "device_prefs.preferences_pb"; fun file(root: File): File = File(root, FILE_NAME) }` (public, for `StoragePathsTest` and tests that build their own DataStore).
  - `companion fun shared(context: Context): DevicePreferences`: one instance **per `Application` instance** (a `synchronized` `WeakHashMap` keyed by `context.applicationContext`, compared by identity). In production that is one per process; Robolectric gives every test a new `Application` and a new `noBackupFilesDir`. **Do not key by a path:** resolving `noBackupFilesDir` may create the directory, and `canonicalPath` stats the disk, so either would be main-thread I/O. Created with `io = Dispatchers.IO` and:
    ```kotlin
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { file(appContext.noBackupFilesDir) },
    )
    ```
    `produceFile` runs lazily on the DataStore scope, so `shared` touches no disk. DataStore forbids two active instances on one file. 4e may move the instance into `AppGraph`.
- `WipeMarker.kt` (for 4e's `AccountWiper`; [F `AccountWiper.swift`, marker part]): `class WipeMarker(private val namespaceRoot: File, private val io: CoroutineDispatcher)`, where `namespaceRoot = root/<namespace.directoryName>`. **Pure `java.nio`, no `android.*`**, so 4e's plain JVM tests can use it.
  - `val file: File = File(namespaceRoot, "pending-wipe.json")`.
  - `suspend fun write(id: AccountID)`: `mkdirs`; write `ApiJson.response.encodeToString(String.serializer(), id.rawValue)` (the JSON string `"019a…01"`, with quotes) to `pending-wipe.json.tmp`; `Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)`.
  - `suspend fun read(): AccountID?`: no file → null. Otherwise decode a JSON string and `AccountID.of` it; anything else throws `CorruptWipeMarkerException` (`class CorruptWipeMarkerException : Exception(null, null)`), touching nothing. 4e maps it to `DatabaseUnavailable`.
  - `suspend fun clear()`: delete the file if present.
- `app/src/debug/kotlin/net/chronoflow/debug/TestSeeds.kt`: in `apply` steps 2 and 3, call `DevicePreferences.shared(context).setLanguage(choice, sdkInt)` / `setTheme(choice, sdkInt)`. Below the threshold that call now writes `device-config`, so remove TestSeeds' own `device-config` write there (it happens once). At or above the threshold, keep the `LocaleManager`/`UiModeManager` calls. The 0e `TestSeedsApplyTest` assertions stay unchanged and must still pass.

**Tests**
- `DeviceIdentityTests` (plain JVM in `core/datastore/src/test`, with an in-memory `IdentityMemoryPreferences` fake counting writes, with a `writeFails` switch; Swift names):
  - `concurrentCreationPersistsOnceAndRestartAndAccountWipeReuseIdentity`: 40 `async(Dispatchers.Default) { store.identity() }` with clock `1_700_000_000.123` and random bytes `0..9`. All results are equal and start with `018bcfe5-687b`; writes == 1. A new `DeviceIdentityStore` on the same fake returns the same id. `StorageNamespace.Api.directoryName != Mock.directoryName`; the two `keychainService` values as in Task 1. The first store still returns the same id.
  - `failedPersistenceRetriesWithoutCachingAnUnpersistedID`: writes fail → `identity()` throws, the fake holds null; writes succeed → `identity()` equals the stored value, writes == 1.
  - `corruptedPreferencesDoNotRotateDeviceIdentity`, `Parameterized`: `"garbage"`, a random v4 UUID string, `"00000000-0000-7000-0000-000000000000"`. Each → `InvalidPersistedIdentity`, writes == 0.
  - `invalidClockAndRandomnessFailBeforePersistence`, `Parameterized` over the clock (seconds) `-1.0`, `Double.POSITIVE_INFINITY`, `Double.NaN`, `2.0.pow(48)` → `InvalidTimestamp`, writes == 0.
  - `deviceNameTrimsAndLimitsUnicodeCharactersWithModelFallback`: `("  Ada's Pixel\n", "Pixel 8")` → `"Ada's Pixel"`; `(" \n ", " Pixel 8 ")` → `"Pixel 8"`; `"👨‍👩‍👧‍👦".repeat(256)` (7 scalars per cluster) → exactly the first 36 clusters (252 scalars, ≤ 255). If the JDK 21 `BreakIterator` splits the ZWJ family, stop and report it; do not write a segmenter.
  - Android-only: `upperCasePersistedIdentityIsLowerCased`.
- `DevicePreferencesTests` (Robolectric `@Logic`, `@Config(sdk = [36])`; DataStore on `noBackupFilesDir/device_prefs.preferences_pb`, built by the test with its own scope, which the test cancels and `join()`s before re-creating a store on the same file):
  - `realPreferencesPersistOutsideAccountNamespaces` (Swift name): set language `Nl`; `identity()` on a fresh store; after re-creating the DataStore a new `DeviceIdentityStore` returns the same id, the stored `api.deviceId` equals it, and the language is still `Nl`. Then write `intPreferencesKey("api.deviceId")` = 42 → `identity()` throws `InvalidPersistedIdentity`.
  - `unknownStoredValuesReadAsSystem`.
  - `languageAndThemeMirrorToDeviceConfigOnlyBelowTheirThresholds`: `setLanguage(Nl, sdkInt = 32)` → `device-config` = `language=nl\ntheme=system\n`; `setTheme(Dark, sdkInt = 30)` → `language=nl\ntheme=dark\n`; `setLanguage(En, sdkInt = 33)` and `setTheme(Light, sdkInt = 31)` leave the file unchanged.
  - `reconcileRewritesDeviceConfigFromDataStore`.
  - `corruptFileResetsToDefaults`: garbage bytes in the `.preferences_pb` → the flows give defaults and the next write succeeds.
  - `sharedIsOnePerApplication`: `shared(application)` and `shared(ContextWrapper(application))` return the same instance. This test does not build its own DataStore.
- `AndroidDeviceNameTests` (Robolectric `@Logic`): `Settings.Global.putString(DEVICE_NAME, "  Ada's Pixel\n")` → `"Ada's Pixel"`; no setting → Robolectric's `Build.MODEL`, trimmed.
- `WipeMarkerTests` (plain JVM, temporary directory; declare `private const val accountA = "019a562b-8000-7000-8000-000000000001"` in the test file, because `:core:database`'s test helpers are not visible in `:core:datastore`): `markerIsAJsonString` (after `write(accountA)` the bytes are exactly `"\"$accountA\""`); `readRoundTripsAndClearRemoves`; `corruptMarkerThrowsAndIsKept` (`"../outside"`, `not json`, `42` → `CorruptWipeMarkerException`, file unchanged); `noTmpFileIsLeft`.

### Task 9. Backup exclusion and storage paths

Commit: `test(app): prove backup exclusion and no-backup storage paths`. Ticks plan.md "**Backup exclusion**".

**Files** — tests only. If a test fails, fix the manifest or rules in the same commit and say so in your hand-off.
- `app/src/test/kotlin/net/chronoflow/app/BackupRulesTest.kt` (Robolectric `@Logic`, `@Config(sdk = [36])`; it also runs in `testReleaseUnitTest`; needs `testOptions.unitTests.isIncludeAndroidResources = true`, which 0e set):
  - `allowBackupIsOff`: `context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP == 0`.
  - `mergedManifestPointsAtTheRules`: load `com/android/tools/test_config.properties` from the test class loader (AGP writes it when `isIncludeAndroidResources` is on) and read its `android_merged_manifest` path. Parse that file with a namespace-aware `DocumentBuilderFactory` (`disallow-doctype-decl` = true, as 0a's `ManifestRules`). `<application>` has `android:allowBackup="false"` and `android:dataExtractionRules="@xml/data_extraction_rules"`. [R] Do not use `ApplicationInfo.dataExtractionRulesRes`: it is `@hide`, so it does not compile against the SDK stubs. If the properties file or the key is missing, stop and report it; do not fall back to the source manifest.
  - `dataExtractionRulesExcludeEveryDomain`: parse the rules with `context.resources.getXml(R.xml.data_extraction_rules)`: `<cloud-backup>` and `<device-transfer>` each hold `<exclude path=".">` for `root`, `file`, `database`, `sharedpref`, `external`, `device_root`, `device_file`, `device_database`, `device_sharedpref` (minus any domain 0a recorded as rejected by Lint), and there is no `<include>`.
  - 0a's `DataExtractionRulesTest` parses the source file; keep it. `BackupRulesTest` checks the merged manifest; do not duplicate its assertions.
- `app/src/test/kotlin/net/chronoflow/app/StoragePathsTest.kt` (Robolectric `@Logic`): build each production path the way 4e will wire it, from `root = context.noBackupFilesDir`:
  - `AccountDatabase(root, Api, AndroidAccountDriverFactory(context), io).file(AccountID.of(accountA))`;
  - `KeystoreSessionStore.forNamespace(root, Api, …).file`;
  - `WipeMarker(File(root, StorageNamespace.Api.directoryName), io).file`;
  - `DevicePreferences.file(root)` and `File(root, DeviceConfigFile.FILE_NAME)`.

  `everyStoreFileLivesUnderNoBackupFilesDir`: each `canonicalPath` starts with `noBackupFilesDir.canonicalPath + "/"`. `accountPathsUseTheValidatedId`: `…/ChronoFlow/Accounts/<accountA>/account-<accountA>.db` exactly, and `AccountID.of(accountA.uppercase())` gives the same path. Read each path from the **public** members named above (`internal` is invisible from `:app`); do not repeat the file names in the test. Declare `accountA` locally, as in `WipeMarkerTests`.

### Task 10. Main-thread guard, plan bookkeeping, porting map and tooling notes

Commit: `test(app): guard store calls against main-thread disk access`. Ticks plan.md "**Ported tests**". In the same commit:
- Replace "on SQLDelight's in-memory JDBC driver" with "on SQLDelight's file-backed JDBC driver (WAL, sidecars and the corrupt-file cases need a file)" in the 4a bullet (OQ-4a-3).
- Record the moved tests in plan.md (OQ-4a-11):
  - In the 4d "**Repositories**" bullet, append: "Also port `AccountModel` and the LocalStore cases moved from 4a: `RecordsTests.allRecordsRoundTripUnknownJSONNullsAndCompositeKeys` and `repositoriesBindKeysAndOrderOutboxByTimestampThenID` (without their draft halves), and all six `RepositoryObservationTests`. Build on 4a's `RecordMapping` and `AccountDatabase.observe`."
  - In the 9b "**Recovery**" bullet, append: "Add the draft halves of iOS `RecordsTests` (the `DraftRecord` round trip and the draft upsert/remove)."
- Add the remaining OQ-4a rows to plan.md "Open questions for the user".
- Add `docs/porting/local-store.md`: Appendix B with the final class names.
- `docs/tooling.md`, under a "Phase 4a" heading: the SQLite versions seen (`sqlite_version()` under Robolectric and on `pixel`), the `make quick` warm time after this phase, and the instrumented suite time.

**Files**
- `app/src/androidTest/kotlin/net/chronoflow/app/store/StoreThreadingSmokeTest.kt` (`@Smoke`, one test `storeCallsFromMainDoNoMainThreadDiskIo`), run on `pixel`:
  [R] The test runs inside the installed debug app, so it must not touch that app's real state: no `Mock`/`Api` namespace directory, no real session alias, no `device_prefs` file. Declare `accountA` locally.
  1. **Before** installing the policy, resolve `root = File(context.noBackupFilesDir, "store-smoke-<uuid>")` (resolving `noBackupFilesDir` may create a directory) and create the instances: `AccountDatabase(root, Mock, AndroidAccountDriverFactory(context), Dispatchers.IO)`; `KeystoreSessionStore(File(root, "session.bin"), "test.<uuid>.session", AndroidKeystoreSessionCipher(), Dispatchers.IO)`; a test DataStore `PreferenceDataStoreFactory.create(scope = testScope /* Dispatchers.IO + SupervisorJob() */, produceFile = { DevicePreferences.file(root) })` wrapped as `DevicePreferences(context, dataStore, Dispatchers.IO)`; `DeviceIdentityStore(thatDevicePreferences)`; `WipeMarker(File(root, StorageNamespace.Mock.directoryName), Dispatchers.IO)`. Every production path code is the same as with the real root.
  2. In `runBlocking(Dispatchers.Main)`, keep the old policy and install `StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()`.
  3. From Main, call: `open(accountA)`, a `write` with `saveAccount`, a `read` with `fetchAccount`, the first value of `observe("account") { it.fetchAccount(accountA) }`; the session store's `write`, `read`, `delete`; `setTheme(Dark)` and `language.first()` (on `pixel`, API 36, neither writes `device-config`); `identity()`; the marker's `write`, `read`, `clear`.
  4. In `finally`: restore the policy, close the database, cancel and `join` the DataStore scope, delete `root` recursively and the alias `test.<uuid>.session`.

  The process surviving is the assertion: the plan's "a debug run shows no main-thread database access".

---

## 6. Acceptance criteria

Run from the worktree root with `ANDROID_HOME` set, and `pixel` booted for items 3 and 5.

1. `./gradlew :core:model:test :core:database:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest :app:testReleaseUnitTest` passes.
2. `make targeted ONLY="core:database:SchemaParityTest core:database:SqlSafetyTest core:database:AccountDatabaseGuardTests#writeAfterItsEpochEndedIsRefused"` passes: the plan's "the schema matches iOS" and "a write after its epoch ended is refused".
3. `make targeted ONLY="app:KeystoreSessionStoreTest app:StoreThreadingSmokeTest"` passes on `pixel`, and `grep -c '@Test' app/src/androidTest/kotlin/net/chronoflow/app/store/KeystoreSessionStoreTest.kt` prints ≤ 6.
4. `make quick` passes in under 60 s warm, and `build/test-results/quick/timings.json` lists classes from `:core:database` and `:core:datastore` with `"overBudget": 0`.
5. `make check` passes (Lint as errors, `verifyReleaseManifest`, the R8 analysis, the `@Smoke` set including the two new classes).
6. `make size` passes and reports no `.so` file (`sqlite-driver` stayed test-only).
7. `./gradlew :konsist:test` passes.
8. `shasum -a 256 core/database/src/test/resources/ios-schema-v1.sql core/database/src/test/resources/rn-schema-v1.json` prints the two hashes in Task 3.
9. `grep -rnE "Log\.|println|Timber" core/database/src/main core/datastore/src/main core/model/src/main/kotlin/net/chronoflow/core/model/session` prints nothing.
10. `grep -rn "class AccountRepository\|class AccountWiper\|class SessionAccountDatabase\|class DraftRecord\|class PlanVersionRecord\|class CheckMarkRecord\|class AccountModel" core/ app/ --include=*.kt` prints nothing from this branch's commits (ownership, §3).
11. The seven 4a checkboxes in `plan.md` are ticked in the commits named above; the wording fixes, the 4d/9b bullet notes and the OQ rows are in. Ten commits, Conventional Commits, no trailers.
12. Nothing was written to `/Users/rickgout/Sites/chronoflow-ios`, `/Users/rickgout/Sites/chronoflow-new` or `/Users/rickgout/Sites/chronoflow-mobile`: `git -C <repo> status --porcelain` is the same before and after (record both).

No screenshots and no perf numbers belong to this phase.

---

## 7. Review checklist (Opus reviewer)

**Correctness against iOS**
- [ ] Guarded writes nest epoch → permit → transaction; both locks are held until `transactionWithResult` returns (after COMMIT). `invalidationWaitsForTheAdmittedCommit` really blocks the invalidating thread.
- [ ] `write(epoch)` invalidates the epoch only when the caller was cancelled, not on a permit's `StaleSessionException`.
- [ ] `open` validates the id before any `mkdirs`; `boundID` is never cleared; same-id open is a no-op; `close` twice is fine; `journal_mode` ≠ wal → `Migration`, `foreign_keys` ≠ 1 → `Unavailable`.
- [ ] No factory path can delete or rewrite an existing file: `onCorruption` is a no-op; no `deleteDatabase`/`File.delete` near the factories.
- [ ] Observation: initial value, then committed values only; rollback emits nothing; `close()` completes collectors normally; `observationCount` returns to 0; null values are emitted.
- [ ] `StoredSession.parse` rejects non-string fields, blank values and the wrong key case, and keeps untrimmed values; `toString()` redacts.
- [ ] `DeviceIdentityStore` caches only after a successful write; a corrupt stored value throws and is never overwritten; `UUIDv7.generate` returns `java.util.UUID`.
- [ ] `WipeMarker` uses no `android.*`; content is a JSON string; a corrupt marker throws without touching anything.

**Ownership (the most likely mistake in this phase)**
- [ ] No repository, `AccountModel`, `AccountWiper`, `SessionAccountDatabase`, `SessionAccountStore`, `DraftRecord`, `PlanVersionRecord` or `CheckMarkRecord` was created (§3).
- [ ] The four records are in `net.chronoflow.core.model.store`, with `String` JSON columns and the iOS defaults.

**Schema and SQL**
- [ ] The `.sq` DDL equals iOS except `DEFAULT 1/0`; `SchemaParityTest` compares real `sqlite_master` text, not the `.sq` files.
- [ ] No `INSERT OR REPLACE`, `ON CONFLICT`, `RETURNING` or `DEFAULT true/false` in 4a's `.sq` files or mapping; saves are update-then-insert.
- [ ] Booleans are `Long` columns mapped in `RecordMapping`; no adapter changed the generated DDL.

**Secrets and storage**
- [ ] No token or session bytes in logs, exception messages, `toString()`, the database or DataStore.
- [ ] Keystore key: 256-bit GCM, no user authentication, Keystore-generated IV, AAD = alias; file version byte `0x01`.
- [ ] Permanent failures delete; transient ones throw and keep the file.
- [ ] Every 4a path is under `noBackupFilesDir`; `allowBackup` is false; the rules exclude every domain in both sections.
- [ ] `sqlite-driver` is `testImplementation` only.

**Threading and hygiene**
- [ ] Every disk, SQLite, DataStore or Keystore call runs inside `withContext(io)`/`writer`; no lock is held across a suspension; no constructor touches `noBackupFilesDir`.
- [ ] No `runCatching`/`catch (e: Exception)` without a preceding `CancellationException` rethrow; no `runBlocking` in main code; no `GlobalScope`; no `Thread.sleep`/`delay`/`withTimeout` used as a wait in tests.
- [ ] One `DataStore` per file per process; `shared` is keyed by the `Application` instance and touches no disk; tests cancel and join their DataStore scope before re-creating it.
- [ ] `StoreThreadingSmokeTest` and `KeystoreSessionStoreTest` use a temporary root and `test.<uuid>` aliases only.
- [ ] Tests keep the Swift names (Appendix B); parameterized cases are separate cases; JVM < 1 s, Robolectric < 5 s, instrumented < 10 s each.

**Typical mistakes**
- `transaction { epoch.whileValid { … } }` (the lock is released before COMMIT).
- `INSERT OR REPLACE` as "save": it cascades and deletes plan versions.
- `DEFAULT true` copied verbatim: on API 29 it stores the string `'true'`.
- `asFlow().mapToOneOrNull()` without close handling, so collectors hang after `close()`.
- The default `onCorruption`, which deletes the account file.
- `UUID.fromString` for paths or identity validation.
- `catch (e: CancellationException) { epoch.invalidate() }` without the `isActive` check.
- `StrictMode` violations hidden by calling store code from `Dispatchers.IO` in the smoke test, or by resolving `noBackupFilesDir` inside the policy window.
- A second `DataStore` on `device_prefs` created by a test or by `TestSeeds`.
- `DevicePreferences.shared` keyed by `noBackupFilesDir` or a canonical path, which is disk I/O on the caller's thread.
- An instrumented test that writes the debug app's real `ChronoFlowMock` directory, Mock session alias or `device_prefs`.
- `FileNotFoundException` caught by the general `IOException` branch of `KeystoreSessionStore.read()`, so a signed-out device throws instead of reading null.

---

## 8. Open questions (proceed on the default; add each to plan.md "Open questions for the user" in the commit that applies it)

| # | Question | Default |
|---|---|---|
| OQ-4a-1 | The plan writes `transaction { epoch.whileValid { permit?.whileValid { body() } } }`, which releases the locks before COMMIT. iOS holds them through COMMIT (`AccountDatabase.swift:124–137`, `SyncEpoch.swift:33`). | Follow iOS: `epoch.whileValid { permit?.whileValid { transaction { body() } } }`; fix A5 and the 4a bullet (Task 5). |
| OQ-4a-2 | iOS DDL says `DEFAULT true/false`; on API 29 (SQLite 3.22) these become the strings `'true'`/`'false'`. | `DEFAULT 1`/`DEFAULT 0`; the parity test normalizes. |
| OQ-4a-3 | The plan says the ported tests run on "in-memory JDBC"; WAL, sidecars, reopen and the corrupt-file cases need a file. | File-backed `JdbcSqliteDriver` in a temporary directory, plus Robolectric native-SQLite tests for the Android factory; fix the wording (Task 10). |
| OQ-4a-4 | The plan says reads are `asFlow().mapToList(io).conflate()`, which cannot end streams on `close()`, count observers or fetch across tables. | `AccountDatabase.observe(tables…)` with SQLDelight listeners, conflated, fetched on io; fix the wording (Task 5). |
| OQ-4a-5 | Plain `read`s are not transacted; a multi-statement read may see two commits. | Keep them untransacted, like iOS `pool.read` for single queries; a later phase wraps a multi-query snapshot in `transactionWithResult`. |
| OQ-4a-6 | Reminder storage: iOS keeps the ledger as settings section `reminderLedger` with `sendAtMs: Double`; A5 asks for local tables. | The tables in Task 3 (`send_at`/`send_at_ms` INTEGER ms, `account_id` nullable, `request_code UNIQUE`); 12b may edit them in place before the first release. |
| OQ-4a-7 | The plan calls the epoch `SessionEpoch`; iOS calls it `SyncEpoch`. | `SyncEpoch`; fix the bullet (Task 8). |
| OQ-4a-8 | Port `NoHealthDataMigration` (iOS v2)? | No: Android starts at v1 with no health data stored; `user_version` stays 1. |
| OQ-4a-9 | Keystore key with `setUnlockedDeviceRequired(true)`? It blocks session reads while the device is locked (reminder taps, background sync). | No. iOS uses `WhenUnlockedThisDeviceOnly`; Android keeps background access and relies on device-bound, non-exportable keys. Revisit before release. |
| OQ-4a-10 | A corrupt `device_prefs` file: reset or fail? A reset loses `api.deviceId`, so the device gets a new id once. | `ReplaceFileCorruptionHandler { emptyPreferences() }`. A corrupt **value** still throws (D22). |
| OQ-4a-11 | The plan's 4a "Ported tests" covers all of `LocalStoreTests`, but the plan and the 1a/4d/4e/9b briefs put records, repositories, `AccountModel`, the wiper and the session database elsewhere. | 4a ports the cases for what it ships. Repository and `AccountModel` cases → 4d; draft halves → 9b; wiper, session-database and readiness cases → 4e (already in its brief). Recorded in the plan (Task 10). |
| OQ-4a-12 | iOS types JSON columns as `JSONDocument` and validates on read and write. 4d and 9b expect `String` columns. | `String` columns; `JSONDocument.requireValid` on every save; no re-validation on fetch (the app is the only writer, and plan JSON is decoded by its reader anyway). |
| OQ-4a-13 | `JdbcAccountDriverFactory` sits in `:core:database/src/test`, so 4d/4e tests in `:core:data` cannot use it. | 4d moves it to a `testFixtures` source set or a small `:core:database-testing` module, whichever AGP 9 supports without disabling built-in Kotlin. |
| OQ-4a-14 | iOS OQ 26: excluding everything from backup loses unsent outbox rows on device restore. | Keep excluding everything (A5); revisit before shipping. |
| OQ-4a-15 | `SessionMutationPermit.invalidate()` is public (iOS: internal to AppCore), because 4e's issuer lives in `:core:sync`. | Public, with KDoc; the 4e reviewer checks that only `SessionController` calls it. |
| OQ-4a-16 | `plan_versions`/`check_marks` mapping needs 1a's `PlanVersionRecord` (`detail: ByteArray`) and `CheckMarkRecord`, and 1a runs in parallel. | 4a maps only the four records; 4d (after 1a is in the stack) adds the plan and check-mark mapping, storing `detail` as strict UTF-8 text. |
| OQ-4a-17 | iOS `DeviceIdentityStore` shares one in-flight task, so concurrent callers see the same error; a `Mutex` makes each waiting caller retry in turn. | `Mutex`; the observable contract (one write, no caching of a failure) is the same. |
| OQ-4a-18 | Should `:core:model` stay free of `kotlinx.coroutines`? 4d needs `SyncEpoch.check()` to see coroutine cancellation. | Add `kotlinx-coroutines-core` to `:core:model`; Konsist rule 2 forbids only `android.*` and Compose there. |

---

## 9. Hand-off to later phases

- **4b/5a:** `androidDeviceName(context, io)` for the login `device_name`; `DeviceIdentityStore(DevicePreferences.shared(context)).identity()` for `device_id`.
- **4d:** builds `:core:data` repositories on `AccountDatabase` and `RecordMapping`; adds the plan-version and check-mark mapping (OQ-4a-16), `StoreApplication`, `OutboxRepository.enqueue`, `AccountModel` and the moved LocalStore tests (OQ-4a-11); moves `JdbcAccountDriverFactory` (OQ-4a-13). `SyncEpoch` names: `accountID`, `generation`, `child()`, `check()` (suspend), `isValid`, `whileValid`, `invalidate()`.
- **4e:** builds `SessionController` on `SessionStore`, `SyncEpoch` and `SessionMutationPermit`; `AccountWiper` on `WipeMarker` (map `CorruptWipeMarkerException` to `DatabaseUnavailable`); `SessionAccountDatabase` on `AccountDatabase(root, namespace, driverFactory, io)`. It wires production instances in `AppGraph` (resolving `noBackupFilesDir` off the main thread, `AndroidAccountDriverFactory`, `AndroidKeystoreSessionCipher`, `KeystoreSessionStore.forNamespace`, `DevicePreferences.shared`).
- **9b:** adds the `drafts` queries (`INSERT OR REPLACE` allowed there) and narrows `SqlSafetyTest`'s `OR REPLACE` pattern to exclude `Drafts.sq`.
- **10a/10b:** read `DevicePreferences.language`/`theme`; call `reconcileDeviceConfig()` at start; mirror to `LocaleManager` (33+) / `UiModeManager` (31+).
- **11a/11b:** a Keystore-backed guest secret store (alias `<keychainService>.guest`) and `guest-wizard.json`.
- **12b/12c:** queries on `scheduled_reminder`/`reminder_ledger`; `notificationPermissionRequested`.

---

## Appendix A. Facts verified for this brief [F]

### A.1 iOS schema v1 (`SchemaV1.swift` l.7–38 at `f391a1fa`, whitespace collapsed)

```sql
CREATE TABLE account (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, has_verified_email INTEGER NOT NULL DEFAULT true, synced_at TEXT, walkthrough_seen INTEGER NOT NULL DEFAULT false);
CREATE TABLE trips (id TEXT PRIMARY KEY NOT NULL, card TEXT NOT NULL, adaptation TEXT, form TEXT, revision TEXT, current_input_hash TEXT, keep_offline INTEGER NOT NULL DEFAULT false, departure_utc TEXT NOT NULL, plan_start_utc TEXT, plan_start_zone TEXT, preparation_days INTEGER, plan_end_utc TEXT, plan_end_date TEXT, deleted_at TEXT, deleted_locally INTEGER NOT NULL DEFAULT false);
CREATE TABLE plan_versions (trip_id TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE, input_hash TEXT NOT NULL, detail TEXT NOT NULL, received_at TEXT NOT NULL, detail_version TEXT, PRIMARY KEY (trip_id, input_hash));
CREATE TABLE check_marks (trip_id TEXT NOT NULL, input_hash TEXT NOT NULL, type TEXT NOT NULL, local_date TEXT NOT NULL, ordinal INTEGER NOT NULL, status TEXT NOT NULL, marked_at TEXT NOT NULL, PRIMARY KEY (trip_id, input_hash, type, local_date, ordinal));
CREATE TABLE settings (section TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);
CREATE TABLE drafts (id TEXT PRIMARY KEY NOT NULL, trip_id TEXT, form TEXT NOT NULL, base_form TEXT, dirty INTEGER NOT NULL DEFAULT true, review TEXT, token TEXT, input_revision INTEGER NOT NULL DEFAULT 0, review_revision INTEGER, wizard_source_revision TEXT, wizard_review TEXT, furthest_step TEXT, wizard_state TEXT, source_revision TEXT, save_failure TEXT, field_errors TEXT, updated_at TEXT NOT NULL);
CREATE TABLE outbox (id TEXT PRIMARY KEY NOT NULL, kind TEXT NOT NULL, payload TEXT NOT NULL, created_at TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, last_error TEXT);
```

Write the `.sq` files from the resource file (multi-line, as in iOS), not from this collapsed form. The only foreign key is `plan_versions.trip_id → trips.id ON DELETE CASCADE`; `check_marks` has none. iOS defines no indexes.

### A.2 `rn-schema-v1.json`

Per table it records `columns` (`cid`, `name`, `type`, `notnull`, `default`, `pk`) and `foreignKeys` (`table`, `from`, `to`, `on_delete`). The defaults are the strings `'true'`/`'false'`/`'0'`. The iOS test sorts columns by name and normalizes `true` → `1` and `false` → `0` before comparing [F `SchemaV1Tests.swift:8–48`]. The Kotlin port makes three substitutions:
- the same normalization, with quotes stripped;
- `PRAGMA user_version` = 1 instead of the `grdb_migrations` rows (`v1`, `v2-no-health-data`);
- the table set = the fixture's tables ∪ {`scheduled_reminder`, `reminder_ledger`} instead of ∪ {`grdb_migrations`}.

### A.3 iOS behaviour quoted above

- `AccountDatabase.open` [F l.31–74]: invalid id → `invalidAccountID`; another bound id → `alreadyOpen`; the same id while open → return; directory creation failure → `storageAttributes`; `foreign_keys` ≠ 1 or pool failure → `unavailable`; `journal_mode` ≠ `wal` or a migration error → close the pool, `migration`. `close()` cancels observations, closes the pool and sets `accountID = nil` (`boundID` kept) [l.165–172].
- `write(epoch:permit:)` [l.116–141]: `epoch.check()`, account match, open; then `pool.writeWithoutTransaction { epoch.whileValid { permit?.whileValid { inTransaction { … } } } }`; `onCancel: epoch.invalidate()`.
- `SyncEpoch.whileValid` checks the parent first, then its own validity, and throws `CancellationError`; `check()` also runs `Task.checkCancellation()` [l.28–44]. `SessionMutationPermit` has the same lock with no parent [F].
- `StoredSession.parse` rejects blank values after trimming and keeps the original values; `readStoredSession` deletes unparseable data; `loadSession` maps any error to `signedOut` [F `SessionStore.swift`].
- `UUIDv7.generate` returns `UUID`; `DeviceIdentityStore` validates 36 characters, version 7 and variant `10`, returns lower case, caches only after success; `deviceName` keeps a grapheme prefix with cumulative scalars ≤ 255 [F `DeviceIdentityStore.swift`].
- `KeychainSessionStore`: service `namespace.keychainService`, account `session`, accessibility `WhenUnlockedThisDeviceOnly`; `errSecItemNotFound` → nil; other statuses throw [F].
- `StorageNamespace`: `ChronoFlow`/`ChronoFlowMock`; `net.chronoflow.app.native`/`net.chronoflow.app.native.mock` [F].

### A.4 Android facts

- API 29 ships SQLite 3.22.0. The `TRUE`/`FALSE` keywords arrived in 3.23.0, UPSERT in 3.24.0, `RETURNING` in 3.35.0 (sqlite.org release notes).
- `SupportSQLiteOpenHelper.Callback.onCorruption` deletes the database file by default.
- SQLDelight 2 notifies `Query.Listener`s after the outermost transaction commits.
- DataStore allows one active instance per file per process; `PreferenceDataStoreFactory` requires the `.preferences_pb` extension.
- `Context.getNoBackupFilesDir()` may create the directory on first call (disk I/O).

## Appendix B. iOS → Kotlin test map

| iOS test | Kotlin class (module) | Tier | Task |
|---|---|---|---|
| `AppCoreTests/SessionStoreTests` (2) | `SessionStoreTests` (`:core:model`), same names | JVM | 1 |
| `AppCoreTests/DeviceIdentityTests.uuidv7Encodes…` | `UUIDv7Tests` (`:core:model`) | JVM | 2 |
| `AppCoreTests/DeviceIdentityTests` (5 more) except `realPreferences…` | `DeviceIdentityTests` (`:core:datastore`), same names | JVM | 8 |
| `AppCoreTests/DeviceIdentityTests.realPreferencesPersistOutsideAccountNamespaces` | `DevicePreferencesTests` (`:core:datastore`), same name | Robolectric `@Logic` | 8 |
| `LocalStoreTests/AccountDatabaseTests` (3) | `AccountDatabaseTests` (`:core:database`), same names | JVM | 4 |
| `LocalStoreTests/SchemaV1Tests` (2) | `SchemaV1Tests` (`:core:database`), same names | JVM | 4 |
| `AppTests/AccountStorageLifecycleTests` (1) | `AccountStorageLifecycleTests` (`:core:database`), name without `test` | Robolectric `@Logic` | 4 |
| `LocalStoreTests/RecordsTests.invalidJSONCannotEnterARecord` | `RecordMappingTests` (`:core:database`), same name | JVM | 6 |
| `LocalStoreTests/RecordsTests` (other 2) | — | — | 4d (draft halves 9b) |
| `LocalStoreTests/RepositoryObservationTests` (6) | — (Android-only `ObservationTests` cover the database layer in Task 5) | — | 4d |
| `AppTests/KeychainSessionStoreTests` (2) | `KeystoreSessionStoreTest` (`app/src/androidTest`), renamed `hostedKeystore…`/`legacyShapeKeyProperties…` | `@Smoke` | 7 |
| `LocalStoreTests/AccountWiperTests` (4), `SessionAccountDatabaseTests` (5), `AccountReadinessTests` (1) | — | — | 4e |
| `LocalStoreTests/NoHealthDataMigrationTests` (1) | not ported (OQ-4a-8) | — | — |

Kotlin substitutions: `#expect(!Thread.isMainThread)` → a check of the injected io thread name (JVM) plus `StoreThreadingSmokeTest` (device); `isBackupExcluded(url)` → "path under `noBackupFilesDir`/root"; `temporaryStoreRoot()` → `TestRoots.temporaryStoreRoot()`; `UserDefaults(suiteName:)` → a DataStore file in a Robolectric `noBackupFilesDir`; GRDB `Row.fetchAll(sql:)` → `SqlProbe` over `AccountSql.driver`.
