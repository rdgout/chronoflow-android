# Phase 4c brief: Mock data source and seeds

- **Branch:** `feat/phase-4c-mock-data-source`, already created from the Phase 4b top. Work in the main checkout, one phase at a time. Phases 0a–0e, 1a–1e, 2, 3a–3c, 4a and 4b have landed below this branch; the earlier wave/worktree scheduling no longer applies (user, 2026-11-18).
- **Depends on:** 4b, 1b (plan; OQ-4c-2). **Execution:** single lane. **Milestone:** M2. **Consumers:** 4d (sync tests), 4e (AppGraph wiring, `CF_*` seeds, `MockSessionSeed`), 5a–5d (auth flows and the mock inbox), 9b–9h (wizard on the mock), 10c–10d (security, passkeys, delete), 11a–11b (guest review and claim).
- **Plan entry:** `plan.md` "### Phase 4c: Mock data source and seeds (`feat/phase-4c-mock-data-source`)" (find by heading; line numbers move as earlier phases land).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`). **Revised the same day** against the final 0a, 0d, 0e, 1b, 1d and 4b briefs: the JUnit 4 table rule (0d §4.2), `make targeted` syntax without a leading colon (0a), the dependency lines 0a and 0e already wrote, the Konsist rule conventions (0a 6b), the two mock tests 4b hands over (4b §9), and the 1b fixture API. **Checked** the same day by an adversarial pass against the iOS sources and tests at the pin, the web routes at `e36368ec`, plan.md and the sibling briefs: corrected the `base64URL("a+b")` expectation, the non-shareable retired-key list, the missing `wizardInput`/`guestReviewInput` helpers, the `MockAppSeed.Fixtures` name clash, the method-control setup order, the 409/422 two-factor split, the preferences/reminders validation rules and the iOS Perf claim in OQ-4c-4.
- **Refresh 2026-11-18:** rechecked the landed contracts, fixtures, module/plugin wiring, test helper and Konsist guard (§4). New plan open questions start at **OQ-A151**; OQ-A68 stays unused. Every commit, including this refresh, must be GPG-signed; stop if signing fails.
- **Tests:** follow AGENTS.md "Tests". This phase is **JVM only** (`:core:data-mock`, plus one release-readiness edit in `:app` and one Konsist rule): JUnit 4.13.2 and assertk 0.28.1 (both come with 0a's `chronoflow.jvm.library` plugin), kotlinx-coroutines-test 1.11.0, mockwebserver3 5.5.1 (parity tests only). No Robolectric, no emulator, no Compose, no Android API. Plain JVM test classes carry **no tier annotation** (0e).
- **Table-driven tests (0d §4.2, binding here):** a class annotated `@RunWith(Parameterized::class)` holds **only** parameterized tests, with `@Parameters(name = "{0}")` and a readable first column. A short list (≤ 5 rows) becomes one plain method per row, named `<swiftName><Row>`, each a one-line call to a private helper. Never loop over rows inside one `@Test`; never use `Enclosed`. Appendix B names the Kotlin form of every Swift `@Test(arguments:)`.
- **Boards:** none. No UI, no screen, no visible string, no capture. Every message the mock produces is a server copy **key** (an English sentence) that the UI later resolves through the generated catalogs; this phase adds nothing to `strings_*.xml`.

Tags: **[F]** fact verified in source (path and line). **[R]** decision for Android adopted by this brief.

---

## 1. Goal

Port the iOS mock server, `Packages/ChronoFlowKit/Sources/DataAccess/Mock/*` (≈ 2.6k lines Swift), into the JVM module `:core:data-mock` (package `net.chronoflow.core.datamock`), so that debug builds, perf builds and every later JVM test have a server that behaves exactly like the iOS mock:

1. **`MockDataSource`** implements all 25 `DataSource` methods with the iOS server rules: injectable latency (clamped 0.3–0.8 s; tests inject a recording sleep, so zero wall time), a one-shot `failNext` for **each** of the 25 methods, an offline switch (plus `mutationsOffline` for `send`), the rate windows, sessions per device with a 90-day sliding expiry, two-factor challenges, password policy, reset and HMAC-signed verification links in a mail **inbox**, passkey sign-in and management, settings validation, check-offs, wizard review/save/edit, guest review and the guest claim.
2. **`MockServerState`** schema **2**, persisted by `FileMockStateStore` at `<root>/ChronoFlowMock/Server/state.json` (the app passes `noBackupFilesDir` as `<root>` in 4e) with atomic writes, a tolerant read and a reseed on a schema mismatch; `MemoryMockStateStore` shares one state across instances.
3. **`MockAppSeed`**: the demo and two-factor accounts, the seven fixture trips (S1–S6 and S6-RETURN-10N) rebased to "now" (one active, one completed, five upcoming), large30/large60 on request, and the daily `refresh` that rebases seeded trips without losing user changes.
4. **Wiring:** `:core:data-mock` reaches only the debug and perf variants of `:app` (0a wrote the `debugImplementation` line and 0e the two perf lines; this phase verifies them and adds nothing to Gradle). Release-readiness tests and an `apkanalyzer` probe prove no mock class ships; a Konsist rule keeps `net.chronoflow.core.datamock` imports out of release source sets.
5. **Ported tests:** every iOS mock test (11 Swift files, listed in §2.2) with the same method names (Appendix B for parameterized ones), plus the mock cases of `DataSourceParityTests`, `NoHealthDataTests`, `PlanDecodingTests`, `LargeTimelineFixtureTests`, and the two mock halves 4b hands over: `AccountSecurityAPITests.mockRunsSetupCodesManageAndPasswordAndRejectsOffline` and `SettingsDeleteAPITests.mockDeletionRequiresPasswordAndActuallyRemovesAccount`.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md`: A3 (modules, Konsist), A5 (storage: "mock server `ChronoFlowMock/Server/state.json`"), A6 (data-source selection: "`:core:data-mock` is only a `debugImplementation` and perf dependency. There is no silent fallback"), "Android spec: storage layout" (l.385–398), "Android spec: seeds and test hooks (debug only)", Phase 4c (find by heading; line numbers move as earlier phases land), 4d, 4e, 11a, 11b | Scope and consumers |
| Rules | `AGENTS.md` "Working rules", "Tests", "Git", "Traps" ("Release builds ship no seed or mock code", "Rethrow `CancellationException`") | How to work |
| Research | `docs/research/02-ios-architecture.md` §4.1, §5.1–§5.6; `docs/research/13-testing-tooling.md` §3.3, §4.8, §5.2–§5.3; `docs/research/01-ios-plan-reuse.md` §6.10 | Quoted in §3 |
| Sibling briefs | `docs/phases/phase-0d-contracts.md` §4.1–§4.2 (serialization rules, table-driven tests), Tasks 2–3 (`Fixtures`, `DataFailure`, `FieldMessage`), 5–7 (DTO names and wire keys); `phase-0a-scaffold.md` 6b (Konsist: `KonsistRules.kt`, `KonsistRulesTest.kt`, `ProjectPaths`, `violations/<rule>/`, `allowed/<rule>/`) and the Makefile (`JVM_MODULES`, `Scripts/targeted.py`); `phase-0e-test-harness.md` T13 (`ReleaseReadinessTest`), T15 (perf variants `benchmarkRelease`/`nonMinifiedRelease`, the `:core:data-mock` perf lines, `ModuleGraphTest.dataMockOnlyInDebugAndPerf`, OQ-A28); `phase-1b-timeline-layout.md` Tasks 2 and 10 (`MockTripTemplates`, `mock-seed.json`, `LargeTimelineFixtures`); `phase-1d-timeline-host.md` l.434–435 (`:core:testing` debug + perf only); `phase-4b-api-client.md` §4, Task 1 (`DataSource`, `OutboxChange`, `MutationReceipt`, `AccountSecurityChange`/`Result`), Task 2 (`TripWizardErrors`, `SettingsPatchValidation`), Task 5 "Test support" (`MockApi`), §9 hand-off, Appendix B | Contracts you build on |
| Existing code | `core/model/…` (0d contracts, `DataFailure`, `FieldMessage`, `ProfilePreferences`, `TripDetailResponse`, `ApiJson`; 4b `DataSource`, `OutboxChange`, `MutationReceipt`, `AccountSecurityChange`/`Result`, `SettingsPatchValidation`, `TripWizardErrors`), `core/network/…` (4b `ApiDataSource` and its MockWebServer test helper), `core/testing/…` (fixtures, top-level `Template`, `MockTripTemplates`, `LargeTimelineFixtures`), `app/build.gradle.kts`, `app/src/testRelease/…/ReleaseReadinessTest.kt`, `docs/tooling.md` (0e's perf-variant mechanism) | What you extend |

### 2.2 iOS at `f391a1fa` (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

Paths are relative to `Packages/ChronoFlowKit/`. **Port each source file line by line**; the Kotlin file name is the Swift file name.

| Swift source | Lines | Kotlin (`core/data-mock/src/main/kotlin/net/chronoflow/core/datamock/`) |
|---|---|---|
| `Sources/DataAccess/Mock/MockDataSource.swift` | 1–1106 | `MockMethod.kt` (l.5–11), `MockDataSource.kt` (l.13–1106) |
| `Sources/DataAccess/Mock/MockServerState.swift` | 1–225 | `MockServerState.kt` |
| `Sources/DataAccess/Mock/MockAppSeed.swift` | 1–161 | `MockAppSeed.kt` |
| `Sources/DataAccess/Mock/MockAppSeed+Rebase.swift` | 1–24 | `MockAppSeed.kt` (`rebase(fixture, index, now)`) |
| `Sources/DataAccess/Mock/MockAppSeed+Fixtures.swift` | 1–18 (the JSON literal) | already `core/testing/src/main/resources/fixtures/mock-seed.json` (1b Task 2); do not copy again |
| `Sources/DataAccess/Mock/MockTripFixtures.swift` | 1–149 | `MockTripFixtures.kt` (`rebase`; `Template`/`templates()` are 1b's `MockTripTemplates`) |
| `Sources/DataAccess/Mock/MockTimelineFixtures.swift` | 1–135 | already `core/testing/…/LargeTimelineFixtures.kt` (1b Task 10); call it, do not port again |
| `Sources/DataAccess/Mock/MockSettings.swift` | 1–165 | `MockSettings.kt` |
| `Sources/DataAccess/Mock/MockTripGeneration.swift` | 1–463 | `MockTripGeneration.kt` (incl. `MockWizardDraft`, `MockWizardReceipt`) |
| `Sources/DataAccess/Mock/MockWizardPrompts.swift` | 1–77 | `MockWizardPrompts.kt` |
| `Sources/DataAccess/Mock/MockGuestReview.swift` | 1–69 | `MockGuestReview.kt` (incl. `MockGuestDraft`) |
| `Sources/DataAccess/Mock/MockInbox.swift` | 1–7 | `MockInbox.kt` (`MockMail`) |
| `Sources/DataAccess/Mock/MockRateLimiter.swift` | 1–19 | `MockRateLimiter.kt` |
| `Sources/DataAccess/Mock/MockScenarioValues.swift` | 1–11 | `MockScenarioValues.kt` |
| `Sources/DataAccess/API/APIDataSource.swift` | 25–28 (`base64URL`) | `MockCrypto.kt` (local copy, so main code does not depend on `:core:network`) |
| `App/Session/AppServices.swift` | 49–94, 116–131 | Read only: how the app constructs the mock (4e ports it) |
| `Sources/DataAccess/AuthUITestConfiguration.swift` | 23–24 | Read only: `CF_MOCK_DELAY` finite in `0…60` s, default 0 (0e parses it; 4e wires it) |

Tests to port (same method names; Kotlin class = Swift suite name; `core/data-mock/src/test/kotlin/net/chronoflow/core/datamock/`):

| Swift test file | Methods |
|---|---|
| `Tests/DataAccessTests/Fixtures/MockScenarios.swift` | helpers → `MockScenarios.kt` |
| `Tests/DataAccessTests/MockAuthTests.swift` | 17 (l.8–343) |
| `Tests/DataAccessTests/MockAccountTripsTests.swift` | 7 (l.8–170) |
| `Tests/DataAccessTests/MockPersistenceTests.swift` | 4 (+ `FailingMockStore`) |
| `Tests/DataAccessTests/SharedMockServerTests.swift` | 2 |
| `Tests/DataAccessTests/MockPasskeyManagementTests.swift` | 7 |
| `Tests/DataAccessTests/MockAppSeedTests.swift` | 8 `#if DEBUG` + `releaseDefaultMockHasNoDemoSeed` (the `#else` branch, l.202) (`apiFactoryNeverConstructsMockSeed` is 4b/4e: out of scope). The Swift file is an `extension DataSourceSelectionTests` (l.7), not its own suite; the Kotlin class is `MockAppSeedTests` (the file name) because `DataSourceSelectionTests` itself is factory work for 4b/4e. Say so in the class KDoc. |
| `Tests/DataAccessTests/MockRebaseTests.swift` | 1 |
| `Tests/DataAccessTests/MockSeedRefreshTests.swift` | 2 |
| `Tests/DataAccessTests/MockTripGenerationTests.swift` | 6 (+ `wizardProfile`, `wizardMockSource`) |
| `Tests/DataAccessTests/MockGuestReviewTests.swift` | 13 (+ `guestMockSource`, `continuedGuestInput`) |
| `Tests/DataAccessTests/MockGuestRegistrationClaimTests.swift` | 12 (+ `guestRegistration`) |
| `Tests/DataAccessTests/DataSourceParityTests.swift` | 3 |
| `Tests/DataAccessTests/AccountSecurityAPITests.swift` | `mockRunsSetupCodesManageAndPasswordAndRejectsOffline` (l.69–100; the two API tests are 4b's) |
| `Tests/DataAccessTests/SettingsDeleteAPITests.swift` | `mockDeletionRequiresPasswordAndActuallyRemovesAccount` (l.32–48; the API test is 4b's) |
| `Tests/DataAccessTests/NoHealthDataTests.swift` | `mockRejectsEveryRetiredReviewProfileKeyWith422` (l.135), `readinessOnlyAgeAndShiftWorkBlockAndPreferencesAdjust` (l.174) |
| `Tests/DataAccessTests/PlanDecodingTests.swift` | `mockTripsRebaseEveryTimestampAndDate` (l.168–257, with its `strings(in:)` helper) |
| `Tests/PlanUITests/LargeTimelineFixtureTests.swift` | `defaultMockHasNoPerformanceTrips` (l.63–70) |

### 2.3 API endpoints

None are called. The mock models the server behind these `DataSource` methods; the endpoint table is `02 §5.3` and 4b's `ApiDataSource`. The mock must return the same `DataFailure` the API path maps for the same server answer (proved by `DataSourceParityTests`).

---

## 3. Facts and decisions you must not get wrong

### 3.1 Verified facts [F]

- **25 methods, 25 `MockMethod` cases** (`MockDataSource.swift:5–11`): `accountSecurity, deleteAccount, passkeyOptions, passkeyLogin, signIn, completeTwoFactor, signUp, requestPasswordReset, resetPassword, verificationStatus, resendVerificationEmail, verifyEmail, signOut, fetchIdentity, fetchAccount, fetchTrips, fetchTripDetail, send, searchAirports, reviewTrip, reviewGuestTrip, reviewTripEdit, saveTrip, updateTrip, tripInput`. (`02 §5.4` says 26; the source has 25.)
- **Latency** (`MockDataSource.swift:99–109`, `AppServices.swift:65–68, 86, 93`): every call sleeps `min(0.8, max(0.3, delay()))`; the default `delay` is `Double.random(in: 0.3...0.8)`. UI-test launches replace `sleep` with "sleep exactly `CF_MOCK_DELAY` seconds if > 0", ignoring the clamped value. Unit tests inject `MockClock.sleep`, which only records.
- **`begin(method)` order** (l.99–109): cancellation check → sleep → cancellation check → `bearer()` (skipped for `reviewGuestTrip`) → cancellation check → `offline || (mutationsOffline && method == send)` ⇒ `Offline` → remove-and-throw `failures[method]` → `load()` → `requestToken = bearer == nil ? currentToken : token`. Offline wins over and does **not** consume a pending `failNext` (`MockAuthTests.latencyOfflineAndFailOnce`).
- **`load()`** (l.62–98): with a `MemoryMockStateStore`, every call re-reads the store's latest state (instances share one server). Otherwise the state is cached per instance after the first load. First load: `restored = store.load()`, where a **decode** error counts as "no file" only when `seed == nil`; a restored state is saved back at once; with a `seed`, `state = restored ?? seed`; without a seed, in DEBUG: `refresh(restored)` when it has accounts and `schemaVersion == 2`, else `MockAppSeed.make(now)` and save; in Release: `restored ?? MockServerState()`. Any error → `DataFailure.server(nil)`.
- **`update`** (l.110–121): the change runs on a copy; the copy is saved; only then does it replace `state`. A save failure throws `server(nil)` and leaves the state unchanged.
- **Persisted path** (`MockServerState.swift:153–157`): `<applicationSupport>/ChronoFlowMock/Server/state.json`, hard-coded (not derived from the storage namespace). `schemaVersion = 2` (l.39).
- **Accounts** (`MockScenarioValues.swift:3–9`): demo `0199a3c0-0000-7000-8000-000000000000`, `demo@example.com`; two-factor `0199a3c0-0000-7000-8000-000000000001`, `twofactor@example.com`; password `password`; code `123456`; recovery `AbCdEf1234-GhIjKl5678`.
- **Seed trips** (verified 2026-11-16 by comparing `MockAppSeed+Fixtures.swift` with `Tests/DataAccessTests/Fixtures/plans/*.json`): template `i` has card id `0199a3c0-0000-7000-8000-00000000000{i+1}` and its `plan` equals: 0 S1 AMS→HND (13 days, 139 blocks), 1 S2 AMS→JFK (10, 109), 2 S3 AMS→SYD (18, 138), 3 S4 JFK→LHR (9, 115), 4 S5 AMS→DXB (5, 40), 5 S6 LAX→AMS (16, 166), 6 S6-RETURN-10N LAX→AMS (27, 250). Seed `settings`: `bodyClock.ageBand "18_64"`, `preferences.showOptionalItems true`, `timeFormat "24"`, `reminders.leadMin 15`, `winddown true`, `planCredits {credits 3, unlimited false}`, `policies` all `"2026-10-01"`, `acceptedTermsAt "2026-10-01T12:00:00Z"`.
- **Rebase targets** (`MockAppSeed+Rebase.swift:15–20`): index 0 → first day starts 1 day before `now` (active); index 2 → **last day ends** 3 days before `now` (completed); index `i ≥ 1, i ≠ 2` → first day starts `3 + i` days after `now` (upcoming). The day count is the difference of local dates in the card's `from.zone`.
- **`MockClock`** (`MockScenarios.swift:14–32`): starts at `1_793_923_200` s = `2026-11-06T00:00:00Z`; `id()` pre-increments a counter from 200 and formats it as `"0199a3c0-0000-7000-8000-%012d"` → first id `0199a3c0-0000-7000-8000-000000000201`; `sleep(delay)` checks cancellation **first**, then records the delay; `advance(seconds)` moves `now`.
- **Release behaviour** (`13 §3.3`): "Release builds can still construct the mock by explicit setting, but **never seed accounts**". On Android the module is absent from release, and the "never seeds" branch is the `debugAppSeed = false` constructor flag (§3.2).

### 3.2 Conventions for this phase [R]

- **Module:** `:core:data-mock` (exists since 0a, plugin `chronoflow.jvm.library`, package `net.chronoflow.core.datamock`). `core/data-mock/build.gradle.kts`:
  ```kotlin
  plugins {
      id("chronoflow.jvm.library")
      alias(libs.plugins.kotlin.serialization)   // MockServerState and friends are @Serializable (0d uses the same alias)
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
      compilerOptions.optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
  } // copy core/model/build.gradle.kts; explicitNulls in MockJson
  dependencies {
      api(project(":core:model"))
      implementation(project(":core:testing"))   // Fixtures, MockTripTemplates, LargeTimelineFixtures, mock-seed.json
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      testImplementation(project(":core:network")) // ApiDataSource, ApiClient for the parity tests only
      testImplementation(libs.okhttp.mockwebserver3)
      testImplementation(libs.kotlinx.coroutines.test)
      // junit4 and assertk come from the chronoflow.jvm.library plugin; do not add them again
  }
  ```
  The alias spellings are illustrative: use the ones in 0a's `libs.versions.toml` and copy the plugin/opt-in lines from `core/model/build.gradle.kts` as 0d wrote them. Main code never depends on `:core:network` or OkHttp (4b's `okhttpOnlyInCoreNetwork` exempts test source sets only).
- **Runs on Android too.** It is a JVM module but executes on ART in debug and perf builds (minSdk 29), and Lint does not check JVM modules for `NewApi`. Use only JDK APIs present on API 29: `java.time`, `java.util.Base64`, `java.security.MessageDigest`, `javax.crypto.Mac`, `java.nio.file` are fine; **never** `java.util.HexFormat` (API 34), `List.of`/`Map.of`/`Set.of` (API 30; use Kotlin `listOf`), `Stream.toList` (34), `Optional.isEmpty` (33). Hex goes through a hand-written helper in `MockCrypto.kt`.
- **Value semantics.** Swift structs are copied on assignment; Kotlin objects are shared. All state types are **immutable** `data class`es (`val`, `List`, `Map`, `Set`); every mutation builds a new value with `copy()`. Provide small private helpers (`MockServerState.updateAccount(id) { … }` that is a no-op when the account is absent, mirroring Swift `?.` chaining; `JsonObject.with(key, value)`). Never put a `ByteArray` or `Array` in a data class (broken `equals`): `signingKey` is a `String` and the HMAC key is `signingKey.encodeToByteArray()` (iOS: `Data(UUID().uuidString.utf8)`; tests use `"artificial-mock-signing-key"`).
- **Concurrency.** Swift `actor` → one `kotlinx.coroutines.sync.Mutex` per `MockDataSource` guarding `state`, `currentToken`, `requestToken`, `offline`, `failures`, `pendingTwoFactor`. Sleep and `bearer()` run **outside** the lock; everything from the offline check onward runs inside `withContext(io) { mutex.withLock { … } }`. The mutex is not reentrant: `accountSecurity(.requestPasswordReset)` (l.954–956) must do its checks under the lock, leave it, then call the public `requestPasswordReset(email)` (which runs its own `begin`). `MemoryMockStateStore` guards its value with `synchronized` and implements `update(initial, change)` atomically (l.141–150).
- **Constructor** (Swift l.32–54; parameter names kept):
  ```kotlin
  class MockDataSource(
      private val store: MockStateStore = MemoryMockStateStore(),
      private val seed: MockServerState? = null,
      offline: Boolean = false,
      private val mutationsOffline: Boolean = false,
      private val remoteTripUpdate: suspend () -> Boolean = { false },
      private val now: () -> Instant = { Instant.now() },
      private val delay: () -> Double = { Random.nextDouble(0.3, 0.8) },
      private val sleep: suspend (Double) -> Unit = { kotlinx.coroutines.delay((it * 1000).roundToLong()) },
      private val id: () -> String = { UUID.randomUUID().toString() },
      private val bearer: (suspend () -> String?)? = null,
      private val assertionProvider: AssertionProvider = { _, _ -> null },
      private val debugAppSeed: Boolean = true,     // Android: iOS `#if DEBUG` in load(), OQ-4c-4
      private val zone: ZoneId = ZoneId.systemDefault(), // Android: iOS `TimeZone.current` in the seed anchor, OQ-4c-10
      private val io: CoroutineDispatcher = Dispatchers.IO,
  ) : DataSource
  typealias AssertionProvider = (ApiPasskeyAssertion, ApiPasskeyChallenge) -> String?
  ```
  Plus `suspend fun setOffline(Boolean)`, `suspend fun failNext(method: MockMethod, error: DataFailure)`, `suspend fun inbox(): List<MockMail>` (load only; no `begin`), `suspend fun replaceRemoteTrips(trips, details)` (l.483–490). These are the only two system reads in the module (`Instant.now()`, `ZoneId.systemDefault()`), and only as defaults.
- **`MockLatency`** (Android, new, `MockLatency.kt`): `fun exact(seconds: Double): suspend (Double) -> Unit` = ignore the clamped argument, `delay(seconds)` when `seconds > 0`, else return at once (iOS `AppServices.swift:66–68`). 4e passes it as `sleep` for seeded launches with the parsed `CF_MOCK_DELAY`.
- **Time.** `MockTime.kt`: `seconds(now: Instant): Double` (epoch seconds with millisecond fraction; Swift `TimeInterval`), `iso(instant)` = Swift `Date.ISO8601Format()`: `yyyy-MM-dd'T'HH:mm:ss'Z'` in UTC, **truncated to whole seconds**, never fractions (`Instant.toString()` prints fractions when present: do not use it on `now()`), `instant(text)` = Swift `instant(_:)`: a 17-character `…THH:mmZ` gets `:00` inserted before `Z`. Calendar math uses `ZonedDateTime` in the zone the Swift code names (`plusDays` keeps the wall time across DST, as `Calendar.date(byAdding: .day…)` does); "start of day difference" = `ChronoUnit.DAYS.between(localDateA, localDateB)`.
- **JSON numbers.** Swift `JSONValue.number(Decimal)` compares numerically; kotlinx `JsonPrimitive` compares by content (`3` ≠ `3.0`). Rules: (1) values the mock **writes** that are whole numbers are `JsonPrimitive(Int/Long)` (`credits`, `passkeyCount`, `leadMin`); (2) validation **compares numbers numerically** (`doubleOrNull`, and `!isString`), never by `JsonPrimitive` equality; (3) Swift `.string` is only a JSON string: use `(v as? JsonPrimitive)?.takeIf { it.isString }?.content`, never `.content` on `JsonNull` (it is `"null"`).
- **Copy keys** are copied **character for character** from the Swift literals (curly apostrophes stay straight `'` where Swift has `'`). §8 has a diff command.
- **State file JSON.** `MockJson = Json(ApiJson.response) { encodeDefaults = true; explicitNulls = false }`. It need not be byte-compatible with iOS (OQ-4c-6) but uses the Swift property names (`schemaVersion`, `seedAnchor`, `accounts`, `sessions`, `twoFactor`, `passkeys`, `passkeyRegistrations`, `resets`, `rateWindows`, `mail`, `wizardDrafts`, `wizardReceipts`, `wizardPaidDirections`, `wizardInputs`, `guestDrafts`, `signingKey`, `claims`). Swift's synthesized decoder **requires** every non-optional property even when it has a default, so a file without `schemaVersion` (or `"{}"`) fails to decode and is reseeded. Reproduce this with `@Required` on `schemaVersion`, `accounts`, `sessions`, `twoFactor`, `passkeys`, `resets`, `rateWindows`, `mail`, `signingKey`, `claims`; the optional ones (`seedAnchor`, `passkeyRegistrations`, `wizard*`, `guestDrafts`) are nullable with `= null`.
- **Errors.** Every expected failure is a `DataFailure`. Catch specific types only (`SerializationException`, `IllegalArgumentException`, `IOException`, `DataFailure`); never `runCatching` or `catch (e: Exception)` around suspend code (AGENTS "Traps"); `CancellationException` always propagates.
- **Names.** Swift type, method, property and test-method names are kept. Swift `enum` cases → Kotlin `PascalCase` constants (`MockMethod.SignIn`) or sealed subclasses (`MockMail.PasswordReset`). Swift `@Test(arguments:)` → the table rule in the header: ≤ 5 rows become plain methods `<swiftName><Row>` in the Swift-named class; longer lists become their own `Parameterized` class whose single test method keeps the Swift name. Appendix B fixes every one of them; use exactly those names.
- **Serialization of the state file is not a wire DTO.** 0d §4.1 rule 1 ("defaults only for omissible keys") governs API DTOs. The mock state types follow Swift's synthesized `Codable` instead: Swift-defaulted properties keep their Kotlin defaults **and** carry `@Required` (so a missing key fails, as Swift does); Swift optionals are nullable with `= null`.
- **Password character classes** (Swift `CharacterSet`s, `MockDataSource.swift:158–176`): length = Swift `String.count` = grapheme clusters, counted with `java.text.BreakIterator.getCharacterInstance(Locale.ROOT)`; iterate code points and use `Character.getType(cp)`: upper = `UPPERCASE_LETTER`, `TITLECASE_LETTER`; lower = `LOWERCASE_LETTER`; digit = `DECIMAL_DIGIT_NUMBER`; symbol or punctuation = `MATH_SYMBOL`, `CURRENCY_SYMBOL`, `MODIFIER_SYMBOL`, `OTHER_SYMBOL`, `CONNECTOR_PUNCTUATION`, `DASH_PUNCTUATION`, `START_PUNCTUATION`, `END_PUNCTUATION`, `INITIAL_QUOTE_PUNCTUATION`, `FINAL_QUOTE_PUNCTUATION`, `OTHER_PUNCTUATION`. Never `Char.isUpperCase()` on UTF-16 units. Device-name length = `codePointCount` (Swift `unicodeScalars.count`).
- **Comments** only for a why the code cannot show (e.g. the `debugAppSeed` flag, the non-reentrant mutex split).

### 3.3 Research quoted

- `13 §5.3`: "Port the mock data source (`MockDataSource`, `MockServerState`, `MockAppSeed`, `MockInbox`, `MockRateLimiter`, trip fixtures rebased to `now`, `large30`/`large60`) into a `:core:data-mock` module. Keep the same account IDs, emails, password, 2FA code and recovery code … Shared in-memory state: every data-source instance reads and writes through one `MockServerState` holder; mutations are atomic against the latest state (I4); mail and credentials are shared across instances. Delay: ungated debug runs use 300–800 ms; seeded runs use exactly `CF_MOCK_DELAY` (0–60 s), default 0 (I5). … Release never seeds."
- `02 §5.4` [R]: "a Kotlin class whose state is guarded by a `Mutex` … **Contract parity tests:** feed the same JSON fixtures through `ApiClient` + `MockWebServer` and through `MockDataSource`, and require equal results and `DataFailure`s."
- `13 §4.8`: "`testReleaseUnitTest` (`:app`) asserts: `Class.forName("…SeedReceiverActivity")` throws; the mock data source, when explicitly constructed, seeds no accounts …"

---

## 4. Check what earlier phases gave you (refreshed 2026-11-18)

Rechecked on this branch against landed code on 2026-11-18. Record these results in the refresh commit body. Mechanical drift is resolved here; follow the user’s Step 0 rule for any further mismatch. No contract or dependency line below is missing.

1. **4b** (`grep -rn "interface DataSource" core/`): `core/model/src/main/kotlin/net/chronoflow/core/model/DataSource.kt` with the 25 suspend methods of `AppCore/DataSource.swift` (names and parameters as in Swift: `signIn(input)`, `requestPasswordReset(email)`, `searchAirports(query, limit: Int?)`, `reviewTripEdit(input, id, sourceRevision)`, `saveTrip(identity)`, `updateTrip(id, identity, sourceRevision)`, `send(change)`, …). Also present: `OutboxChange` (`CheckOff`, `Settings(section, patch)`, `Policies(version)`, `Profile`, `Walkthrough`, `Unsupported(kind, payload)`), `CheckOffChange`, `ProfileChange`, `SettingsSection`, `MutationReceipt` (`Acknowledged`, `Account`, `Identity`, `Checks`), `AccountSecurityChange` (11 cases) and `AccountSecurityResult` (7 cases), `SettingsPatchValidation.validate(section, patch)`, `TripWizardErrors` (`map(failure, outboundCount)`, `camel`), `ApiDataSource` and the deny-by-default MockWebServer helper in `core/network/src/test`. Use 4b's exact Kotlin names and packages. `OutboxChange` and `MutationReceipt` are in `net.chronoflow.core.model`; security contracts and both validation helpers are in `.model.api`. Receipt payload properties are `snapshot`, `identity`, `result`; `CheckOff` and `Profile` wrap `change`. `ApiClient.evictIdleConnections()` is a suspending client lifecycle hook (OQ-A150), not a `DataSource` method; do not add a 26th `MockMethod`. **If a pure contract type is missing**, add it to `:core:model` in a preparatory commit `feat(model): add <type> for the mock` ported from its Swift file at the pin, with its Swift contract test, and record it (OQ-4c-8).
2. **0d**: `DataFailure` (with `Validation(fields, status)`, `Throttled(retryAfterSeconds: Double, status)`, …), `FieldMessage(key, arguments: Map<String, JsonPrimitive>)`, `OutboxKind`, `AccountID`; `PasswordSignInInput`, `RegisterInput`, `ResetPasswordInput`, `SignedVerificationInput` (`expires: Long`), `EmailVerificationStatus`, `TwoFactorInput` (`Proof.Code`/`RecoveryCode`), `SignInResult`, `SessionResult`, `SessionAccount`/`AccountIdentity`, `AccountPolicies`, `AccountSettings`, `AccountSnapshot`, `AuthLanguage`, `ApiTwoFactorSetup`, `ApiRecoveryCodes`, the passkey and passkey-management DTOs (`ApiPasskey.id: Long`), `ApiTripReviewInput`, `ApiTripSaveInput`, `ApiGuestTripReviewInput`, the generated `TripListItem`, `TripDetail`, `TripCardView`, `TripProgress`, `ApiTripReview`, `ApiReviewIdentity`, `ApiTripInput`, `ApiTripInputSnapshot`, `ApiAirportSuggestion`, `TripDirection`, `CheckKey`, `CheckMark`, `CheckStatus`, `BlockType`; `ProfilePreferences` (`bodyClock`, `readiness`, `request`), `TripDetailResponse.encoding(detail)`/`decode(bytes)`, `TripListResponse`, `ApiJson.request`/`response`, the `JsonElement?.int` extension in `contracts/JsonValues.kt`; fixtures `account/{snapshot,identity}.json`, `trips/{list,detail}.json`, `auth/{session,passkey-options}.json`, `errors/wrong_credentials.json`, `wizard/*.json` and the fixture loader `FixtureManifestTest` uses.
3. **0e**: `app/src/testRelease/kotlin/net/chronoflow/app/ReleaseReadinessTest.kt`; in `app/build.gradle.kts`, `debugImplementation(project(":core:data-mock"))` plus the landed deferred block `configurations.matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }.configureEach { project.dependencies.add(name, project(":core:data-mock")) }` (OQ-0e-26, `docs/tooling.md` “Perf variants (OQ-A28)”). Both plugin-created perf variants exist; there is no `benchmark` fallback build type; `konsist/…/ModuleGraphTest.dataMockOnlyInDebugAndPerf`; the timings listener (JVM test > 1 s fails unless `@PerfBudget`). Quote the debug dependency and the matching/configureEach lines in the refresh commit body. **If a line is missing, stop and report** (0e owns it; do not add it here).
4. **1b**: `core/testing/src/main/kotlin/net/chronoflow/core/testing/MockTripTemplates.kt` (`@Serializable data class Template(val card: TripCardView, val detail: TripDetail)` is top-level in `net.chronoflow.core.testing`; `object MockTripTemplates { fun templates(): List<Template> }` memoizes the decoded list), `core/testing/src/main/resources/fixtures/mock-seed.json` (read with `Fixtures.bytes("mock-seed.json")`), `LargeTimelineFixtures.fixture(named): LargeTimelineFixtures.Fixture` (`Fixture(card, detail)`, a different type from `Template`).
5. **0a**: `:core:data-mock` is in `settings.gradle.kts` and in the Makefile's `JVM_MODULES` (so `make quick` runs its tests; it also lists `core:testing-junit`); `Scripts/targeted.py` takes `<module>:<Class>[#method]` with the module **without** a leading colon (`core:data-mock:MockAuthTests`).

6. **Konsist fixture boundary:** `testingNotInMain` currently rejects all `core/data-mock/src/main` imports of `net.chronoflow.core.testing`. Task 1 must allow exactly the three fixture imports `Fixtures`, `MockTripTemplates` and `Template` in `MockAppSeed.kt`, `MockTripFixtures.kt` and `MockTripGeneration.kt`, plus `LargeTimelineFixtures` in `MockAppSeed.kt`, while preserving the existing rule everywhere else. Add positive controls for these exact file/import combinations and negative controls for an unrelated mock-main file and a non-fixture testing import. This extends the guard for the already-planned debug/perf fixture consumer; no landed runtime behavior changes. Record the default in the next free OQ-A row when applying it.
7. **Parity test support:** `core/network/src/test/kotlin/net/chronoflow/core/network/support/MockApi.kt` is present, deny-by-default and uses `ApiDeadline`; copying it requires no lifecycle-hook change. The actual alias is `libs.okhttp.mockwebserver3`. `core/network/build.gradle.kts` exports `api(libs.okhttp)`, so its `OkHttpClient` signatures already resolve transitively in mock tests; no extra OkHttp dependency is needed.

---

## 5. Out of scope (owner in brackets)

- Choosing mock vs API at runtime, `DataSourceFactory`, base-URL validation, `apiFactoryNeverConstructsMockSeed` [4b/4e]. `AppGraph` construction of the mock, `MockSessionSeed`, `CF_AUTH_TEST_*` handling (`unverified`, `walkthroughSeen`, `termsPending`, `offline`, `mutationsOffline` from seeds), `CF_REFRESH_TEST_EMPTY_TRIPS`, `-cf.timeline-fixtures`/`-cf.isolatedMockServer`, wiring `MockLatency.exact(CF_MOCK_DELAY)`, `ReseedEqualsColdSeedTest` [4e].
- Reading the inbox to open reset or verify links in the app [5c, 5d]. `SettingsBoardCaptureFixture`, `wizardUITestSeed` board-capture seeds [9x, 10x]. `MockReminderCenter` [12b].
- The sync engine, outbox replay and `FakeDataSource` [4d].
- Any UI, string resource, board or capture.

---

## 6. Tasks

Each task is one commit on `feat/phase-4c-mock-data-source`, Conventional Commits, lower-case imperative subject, **no trailers**. Tick the `plan.md` Phase 4c checkbox named in the task in the same commit. Fix rounds run `make targeted ONLY="core:data-mock:<Class>[#method]"` (no leading colon); run `make quick` once before handing back. Every coroutine test uses `runTest(timeout = 60.seconds)`.

Package path shorthand: `dm/X.kt` = `core/data-mock/src/main/kotlin/net/chronoflow/core/datamock/X.kt`; `dmt/X.kt` = the same under `src/test`.

### Task 1. Module dependencies and small helpers

Commit: `feat(data-mock): add mock time, crypto, inbox and rate limiter`. Ticks nothing.

**Files**
- `core/data-mock/build.gradle.kts`: the dependencies of §3.2.
- Extend `testingNotInMain` and its planted/control tests as §4 item 6 specifies; run only those targeted methods. Do not broaden the allowance to all testing helpers or all mock-main files.
- `dm/MockScenarioValues.kt`: `object MockScenarioValues` with the 7 values of §3.1 (Swift is `#if DEBUG`; the whole module is debug/perf-only, so no guard).
- `dm/MockMethod.kt`: `enum class MockMethod` with the 25 cases in Swift order.
- `dm/MockTime.kt`: `seconds`, `iso`, `instant` (§3.2).
- `dm/MockCrypto.kt`: `sha256Hex(String)`, `sha256Bytes(String)`, `sha1Hex(String)`, `hmacSha256Hex(key: ByteArray, message: String)`, lowercase hex via a hand-written table; `base64Url(bytes)` = standard Base64 with `+`→`-`, `/`→`_`, `=` removed (Swift l.1013–1016); `base64URL(value: String): ByteArray?` = port of `APIDataSource.swift:25–28` (replace `-`/`_`, pad with `=` to a multiple of 4, strict decode, `null` on failure).
- `dm/MockInbox.kt`: `@Serializable sealed interface MockMail` with `PasswordReset(id, to, sentAt: Double, input: ResetPasswordInput)` and `Verification(id, to, sentAt: Double, input: SignedVerificationInput)`.
- `dm/MockRateLimiter.kt`: `object MockRateLimiter { @Serializable data class Window(val count: Int, val expiresAt: Double); fun hit(key, limit = 5, seconds = 60.0, now: Double, windows: Map<String, Window>): Map<String, Window> }`: an expired window (`expiresAt <= now`) restarts at `count 0, expiresAt now + seconds`; `count >= limit` throws `DataFailure.Throttled(max(0.0, expiresAt − now), 429)`; else returns the map with `count + 1` and the **same** `expiresAt`.
- `dm/MockLatency.kt`: `MockLatency.exact(seconds)` (§3.2).

**Tests** (all new, JVM)
- `MockTimeTest`: `iso(Instant.parse("2026-11-06T00:00:00.789Z")) == "2026-11-06T00:00:00Z"`; `instant("2026-10-14T12:30Z") == Instant.parse("2026-10-14T12:30:00Z")`; a 20-character value parses unchanged.
- `MockCryptoTest`: `sha256Hex("abc")` = `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`; `sha1Hex("abc")` = `a9993e364706816aba3e25717850c26c9cd0d89d`; `hmacSha256Hex("key".toByteArray(), "The quick brown fox jumps over the lazy dog")` = `f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8`; `base64URL("AA") contentEquals byteArrayOf(0)`, `base64URL("AA==")` the same, `base64URL("AB")` also `[0]` (non-zero trailing bits are accepted, as Foundation does), `base64URL("A")` null, `base64URL("A*B")` null; `base64URL("a+b")` is **not** null but `[107, -26]` (`0x6B, 0xE6`): the Swift helper only maps `-`/`_` and lets a standard `+` through (verified by running the Swift helper and `java.util.Base64` on 2026-11-16); the URL-alphabet check lives in `validateAssertion`, not here. `base64Url` of 32 bytes is 43 characters without `=`.
- `MockRateLimiterTest`: five hits pass, the sixth throws `Throttled(60.0, 429)`; after `now + 60` the window restarts; a hit at `now + 10` inside the window reports `retryAfterSeconds == 50.0`.
- `MockLatencyTest` (`runTest`): `exact(0.0)(0.5)` advances virtual time by 0; `exact(2.5)(0.3)` advances it by exactly 2,500 ms.

### Task 2. `MockServerState` and the state stores

Commit: `feat(data-mock): add the mock server state and its stores`. Ticks nothing (the "tolerant read" and "reset on mismatch" land in Task 6).

**Files** (port of `MockServerState.swift`, plus the data types the state references)
- `dm/MockServerState.kt`:
  - `MockServerAccount(snapshot, password, twoFactorCode: String? = null, recoveryCodes: Set<String> = emptySet(), walkthroughSeen = false, resendAvailableAt: Double = 0.0, trips: List<TripListItem> = emptyList(), details: Map<String, TripDetail> = emptyMap(), passkeys: List<ApiPasskey>? = null, passkeyCredentialIDs: List<String>? = null)` with `val identity: AccountIdentity` (l.31–35). The Swift defaults live only in `init`, so Swift's synthesized decoder requires every non-optional property: put `@Required` on `recoveryCodes`, `walkthroughSeen`, `resendAvailableAt`, `trips` and `details` (§3.2 "Serialization"); `passkeys` and `passkeyCredentialIDs` stay nullable. `toString()` redacts `password`, `twoFactorCode`, `recoveryCodes` (AGENTS "Secrets").
  - `MockServerState` with `schemaVersion = currentSchemaVersion` (`const val currentSchemaVersion = 2` in the companion), every field of l.40–56, `@Required` per §3.2, and the secondary constructor `MockServerState(accounts: List<MockServerAccount> = emptyList(), signingKey: String = UUID.randomUUID().toString().uppercase())` keyed by `snapshot.id` (l.57–60; Swift `uuidString` is upper-case).
  - `MockClaim` (l.63–102): stores `secretHash`; `companion fun of(accountID, tripID, secret, deviceID = null, expiresAt = null)` hashes with `MockGuestReview.hash`; `isLive(at) = (expiresAt ?: +∞) > at`; a custom serializer that decodes `secretHash` or, when absent, hashes a legacy `secret`, and **never encodes** `secret`.
  - `MockSession(accountID, deviceID, expiresAt)`, `MockTwoFactor(accountID, deviceID, expiresAt, attempts)`, `MockPasskey(challenge: ApiPasskeyChallenge, expiresAt)`, `MockPasskeyRegistration(accountID, challenge, expiresAt, token: String?)`, `MockReset(token, expiresAt)`.
  - `interface MockStateStore { fun load(): MockServerState?; fun save(state: MockServerState) }` (blocking; always called on the `io` dispatcher).
  - `MemoryMockStateStore(initial: MockServerState? = null)` with `synchronized` `load`, `save` and `internal fun <T> update(initial: MockServerState, change: (MockServerState) -> Pair<MockServerState, T>): Pair<T, MockServerState>` (l.141–150: the change applies to the stored value, or to `initial` when empty, and stores the result atomically).
  - `FileMockStateStore(root: File)` with `val file = File(root, "ChronoFlowMock/Server/state.json")`; `load()` returns `null` when the file is missing, else decodes with `MockJson` (a decode failure throws `SerializationException`); `save()` creates the directory, writes `state.json.tmp` in the same directory, then `Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)`. No backup attributes are needed: `noBackupFilesDir` is already excluded (A5).
  - `mockRegistration(id, name, email, language: AuthLanguage, now: Instant): AccountSnapshot`: port of l.181–224 with every key and value exactly (body-clock nulls list, `habitualBed "23:00"`, `habitualWake "07:00"`, `freeDayStatus "unconfirmed"`, the four `false` booleans, `invalidDrafts []`; preferences `hiddenCategories []`, `showOptionalItems false`, `timeFormat "24"`, `language`; reminders `light/avoid/caffeine true`, `winddown false`, `leadMin 30`; `planCredits {credits 3, unlimited false}`; policies `"1.0"`/`"1.0"`/`"1.0"`/`iso(now)`; `readiness = MockSettings.readiness(body)`; billing `subscription {tier "free", status "free", endsAt null, renewsAt null, canResume false, canCancel false, paymentFailed false, unlimited false}`; security `twoFactorEnabled false`, `twoFactorConfirmedAt null`, `passwordRules "minlength: 12; required: lower; required: upper; required: digit; required: special;"`). Until Task 3 lands, `readiness` may call `ProfilePreferences.readiness` directly with the same arguments as `MockSettings.readiness`.
- `dm/MockJson.kt`: `MockJson` (§3.2).
- Data-only declarations needed by the state (logic in Task 5): `MockWizardDraft(owner, tripID: String?, input: ApiTripInput, review: ApiTripReview, profile: JsonObject, expiresAt: Double)` whose factory and serializer apply `ProfilePreferences.bodyClock(profile)` on creation, decode and encode (`MockTripGeneration.swift:5–47`); `MockWizardReceipt(intent, tripID, expiresAt)`; `MockGuestDraft(deviceID, secretHash, draft: MockWizardDraft)`; `object MockGuestReview { fun hash(secret) = sha256Hex(secret); fun secret(): String = 32 random bytes as lowercase hex (SecureRandom) }`.

**Tests**
- `MockServerStateCodecTest` (new): `MockServerState(accounts = [mockRegistration(…)])` round-trips through `MockJson` to an equal value; decoding the same JSON with `schemaVersion` removed throws `SerializationException`; `"{}"` throws; unknown keys are ignored.
- `MockGuestRegistrationClaimTests.legacyClaimDecodesAndReencodesWithoutPlaintextSecret` (Swift l.221–235): port it here; the rest of that class lands in Task 9.
- `FileMockStateStoreTest` (new, `@get:Rule TemporaryFolder`): `file` ends with `ChronoFlowMock/Server/state.json`; `load()` is `null` before the first save; save then load is equal; no `.tmp` file remains; a corrupt file makes `load()` throw `SerializationException`.
- `MemoryMockStateStoreTest` (new): 8 coroutines on `Dispatchers.Default` each call `update` 50 times, every call appending one mail; afterwards `load()!!.mail` has all 400 entries.

### Task 3. `MockSettings`

Commit: `feat(data-mock): port mock settings validation and readiness`. Ticks nothing.

**File:** `dm/MockSettings.kt`, port of `MockSettings.swift:1–165`: `camel(key)` (`habitual_bed` → `habitualBed`; Swift `split(separator: "_")` **drops empty pieces**, so use `key.split('_').filter { it.isNotEmpty() }`; this differs from 4b's `TripWizardErrors.camel`, which keeps them, so do not reuse that one here), `invalid(field, key = "Choose one of the available options.")`, `validate(section, patch, current)` (calls 4b's `SettingsPatchValidation.validate` first; visits the patch keys **in sorted order** and collects every invalid key into one `Validation(…, 422)`; message `"Enter a valid time."` when the key contains `bed` or `wake`, else `"Choose one of the available options."`. Per-key validity (l.19–44): **preferences** `time_format` ∈ {`"12"`, `"24"`}; `show_optional_items` a boolean; any other key a JSON array whose every element is a string naming a known `BlockType` (not the unknown case), with no duplicates. **reminders** `lead_min` numerically equal to 15, 30 or 60; any other key a boolean. **bodyClock** `bodyValue`. Then the body-clock cross-field rules on `current.bodyClock` merged with the camel-cased patch, in Swift order: duration `(wake − bed + 1440) % 1440` in `180…840` for `habitual_*` and, when `freeDayStatus == "declared"`, `free_day_*` → `"Check your bedtime and wake-up time."` on `habitual_bed`/`free_day_bed`; declared without both free-day times → `free_day_bed`; a `cups_*` key **in the patch** > 0 while the merged `caffeineHabit != "cups"` → that key, `"Add your usual drinks per day, or leave your caffeine habits unanswered."`; `caffeineHabit == "cups"` with a zero total of the four merged cup numbers → `cups_filter_coffee` `"Add at least one drink to save this answer."`; `blueBlockers == "rated"` with `blueBlockerMdfd` **present and JSON null** (Swift `== .null`; an absent key passes) → `blue_blocker_mdfd` `"Enter a rating between 0.1 and 9.9."`), `bodyValue` (the time regex `^(?:[01][0-9]|2[0-3]):[0-5][0-9]$`, null allowed only for `free_day_*` times; the choice table of l.104–109, null allowed except for `free_day_status`; cups whole numbers `0…20` or null; the rating rules of l.118–130; booleans; null allowed except for `caffeine_sensitive`, `caffeine_non_user_opt_in`, `exclude_caffeine`, `flight_movement_breaks`), `apply(patch, section, settings): AccountSettings` (rating number → its decimal string; `sleepAnswered = true` when one of `habitual_bed`, `habitual_wake`, `free_day_status`, `free_day_bed`, `free_day_wake` is patched (five keys; `wizardReview` in Task 6 uses these plus `chronotype`); clear free-day times unless declared; clear the rating unless rated), `readiness(body) = ProfilePreferences.readiness(JsonObject(mapOf("invalidSections" to JsonArray(emptyList()))), body)`.

**Tests**
- `NoHealthDataTests.readinessOnlyAgeAndShiftWorkBlockAndPreferencesAdjust` (Swift l.174–192, verbatim expectations: `ready`, `adjusted` with `missing == []` and no `lightDevicesAllowed`, `65_plus` → `adjusted`, `under_18`/`shiftWork true` → `excluded`). The settings validation cases are covered through the data source in Tasks 7 and 10.

### Task 4. Fixture rebase and `MockAppSeed`

Commit: `feat(data-mock): port the mock app seed and trip rebasing`. Ticks `plan.md` Phase 4c "**MockAppSeed.**". **Needs 1b** (OQ-4c-2).

**Files**
- `dm/MockTripFixtures.kt`: `object MockTripFixtures { fun rebase(fixture: Template, days: Int, minutes: Int = 0): Template }` (1b's `Template`), port of `MockTripFixtures.swift:14–148`, line by line:
  - Encode `card` and `detail` with `ApiJson.response` to `JsonElement`, transform, decode back.
  - Transform rule for objects (l.29–55; the root call inherits the card's `from.zone`): the object's zone is `displayTz` ?: `tz` ?: `zones[0].tz` ?: the inherited zone; per key: `markedAt` unchanged; `departureUtc`/`departLocal` use the **same object's** `from.zone` ?: the object's zone; `arrivalUtc`/`arriveLocal` use the same object's `to.zone` ?: the object's zone; `endUtc` uses the same object's last `legs[].to.zone` ?: `zones.last.tz` ?: the object's zone; `arrivalLandingUtc`/`assumedReturn` use the **card's** `to.zone`; anything else the object's zone. The chosen zone is passed down into nested values; arrays pass the inherited zone to each element. Strings matching `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?Z$` → `iso(zoned(instant, zone).plusDays(days).toInstant() + minutes min)`; strings matching `^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2})?$` → the same local value shifted by `days` in that zone (plus `minutes` only when it has a time), formatted back with the same pattern; other strings unchanged.
  - Then: each day's `labelDate` = local date of `startUtc` in `zones.first().tz`; `labelRange` (only when non-null) = local date of `endUtc − 1 s` in `zones.last().tz` if it is after `labelDate`, else `null`; each block's `checkKey.localDate` = local date of `startUtc` in `displayTz`; `zone_change` params' `offsetMin` recomputed from the airports of card, connections and legs at the block's start; marks re-keyed by block index; `hash = sha256Hex("${inputHash}:$days" + (if minutes == 0 "" else ":$minutes"))` → `plan.meta.inputHash`, `checkState.planInputHash`, `detailVersion = "${old detailVersion}-$hash"`; card `departLocal`/`arriveLocal`/`offsetDifferenceMinutes`, `trip.departLocal`, `trip.offsetDifferenceMin`, every tick's `offsetMin` (l.132–146).
- `dm/MockAppSeed.kt`: `object MockAppSeed`, port of `MockAppSeed.swift` and `+Rebase.swift`:
  - `@Serializable data class Fixtures(val settings: AccountSettings, val trips: List<Template>)`, decoded with `ApiJson.response` **once per process** (`by lazy`) from 0d's `net.chronoflow.core.testing.Fixtures.bytes("mock-seed.json")` (1b's resource; `ApiJson.response` ignores unknown keys such as `detail.revision`), mirroring Swift `decodedFixtures`. Inside `object MockAppSeed` the simple name `Fixtures` means the nested data class, so import the loader with an alias (`import net.chronoflow.core.testing.Fixtures as TestFixtures`) or call it fully qualified. Decoded values are immutable, so rebasing never mutates the cache.
  - `make(now: Instant, unverified = false, walkthroughSeen = false, termsPending = false, includeTimelinePerformanceTrips = false, zone: ZoneId = ZoneId.systemDefault()): MockServerState`, exactly l.15–68: rebase each template with `rebase(fixture, index, now)`; when asked, append `LargeTimelineFixtures.fixture("large30")` and `("large60")` **unrebased**, each converted as `Template(f.card, f.detail)` (Swift l.25–28); `listItem` for each; the demo snapshot from `mockRegistration(demoID, "Demo Traveller", demoEmail, En, now)` with `hasVerifiedEmail = !unverified`, `settings = fixtures.settings`, `termsPending` → `AccountPolicies(termsVersion, privacyVersion)` with nothing accepted, `readiness`, `billing["checkout"] = JsonNull`, the security object of l.46–51 (`canManagePasskeys true`, `canManageTwoFactor true`, `passkeyCount 2`, `passwordRules "minlength: 10;"`, `twoFactorEnabled false`, `twoFactorConfirmedAt null`, `requiresConfirmation true`, `retryAfterSeconds null`); demo passkeys `demoPasskeys(now)`; the two-factor account copies the snapshot with its id and email, `passkeyCount 0`, `twoFactorEnabled true`, `twoFactorConfirmedAt iso(now)`, code `123456`, recovery set `{AbCdEf1234-GhIjKl5678}`, the **same** trips and details, `passkeys = []`; `seedAnchor = anchor(now, zone)`.
  - `demoPasskeys(now)`: `ApiPasskey(1, "iPhone 16", createdAt = iso(now − 60 d), lastUsedAt = iso(now))`, `ApiPasskey(2, "MacBook Pro", iso(now − 42 d), iso(now − 15 d))` (OQ-4c-11).
  - `anchor(now, zone)` = `yyyy-MM-dd` of `now` in `zone`.
  - `refresh(stored, now, zone)`, exactly l.90–128 (passkeys back-fill, `passkeyCount`, rebase seeded trips only when the anchor changed, re-key marks by block id, keep `checkState.days`, always recompute `listItem`, set the anchor).
  - `listItem(fixture, now)` l.130–154 and `rebase(fixture, index, now)` l.6–22 (§3.1 targets; local dates in `card.from.zone`).

**Tests** (ported, Swift names; row methods per Appendix B)
- `MockRebaseTests.preservesWallTimesAcrossAmsterdamAndNewYorkDST20261025`, `…20261101`, `…20261107` (`now` = `2026-10-25T12:00:00Z`, `2026-11-01T12:00:00Z`, `2026-11-07T12:00:00Z`).
- `PlanDecodingTests.mockTripsRebaseEveryTimestampAndDate20000229`, `…20260329`, `…20261025`, `…20350715` (`now` = `2000-02-29T23:59:00Z`, `2026-03-29T01:30:00Z`, `2026-10-25T01:30:00Z`, `2035-07-15T12:00:00Z`; statuses are exactly `{Active, Upcoming, Completed}`).
- `MockSeedRefreshTests.walkthroughOverrideCoversBothSeededAccountsSeen`, `…NotSeen`.
- `MockAppSeedTests.cachedFixtureTemplatesRebaseWithoutSharingSeedMutations`.
- `MockAppSeedTests.defaultMockHasNoPerformanceTrips` (from `LargeTimelineFixtureTests.swift:63–70`: 7 trips by default, 9 with the flag).
- Seed tests pass `zone = ZoneId.of("Europe/Amsterdam")`. Warm `MockAppSeed`'s lazy fixtures in a `@BeforeClass` so the first test does not pay the decode (OQ-4c-9).

### Task 5. Wizard and guest generation helpers

Commit: `feat(data-mock): port mock trip generation, prompts and guest review`. Ticks nothing.

**Files**
- `dm/MockTripGeneration.kt`, port of `MockTripGeneration.swift:55–463`: `airports()` (8 airports from the templates' legs: AMS, DXB, HND, JFK, LAX, LHR, SIN, SYD with the names and countries of l.57–62, sorted by IATA), `instant(local, airport)` (strict `yyyy-MM-dd'T'HH:mm` in the airport zone, round-trip check, else `"review"` → `"Check the departure and arrival times."`), `itinerary`, `template` (score = 100 × best first-leg match + other matching legs; ties keep the earlier template; rebase by local-day difference and minutes), `prompts`, `review` (status, price states, `canSave`, `saveBlockedReason "Not enough credits"`, the three reason keys), `saved` (inputHash = `sha256Hex` of the canonical JSON of `input` followed by that of `identity`: keys sorted recursively, like Swift `.sortedKeys`), `snapshot`, `summaryLine`, `layovers`, `stayEnd`.
- `dm/MockWizardPrompts.kt`, port of `MockWizardPrompts.swift:1–77`.
- `dm/MockGuestReview.kt`: add `prepare(request, now)` (`MockGuestReview.swift:20–69`) to the object from Task 2.

**Tests**
- `MockTripGenerationHelpersTest` (new): `airports().map { it.iata } == [AMS, DXB, HND, JFK, LAX, LHR, SIN, SYD]` (SIN comes from template 2's AMS→SIN→SYD legs; verified in the seed JSON); `airports()` and `template()` read 1b's memoized `MockTripTemplates.templates()` (Swift re-decodes on every call; do not); `instant("2026-03-29T02:30", AMS)` (a DST gap) throws the `review` validation; `saved` gives the same `inputHash` twice for the same draft. Behaviour is covered through the data source in Task 9.

### Task 6. `MockDataSource`

Commit: `feat(data-mock): port the mock data source`. Ticks `plan.md` Phase 4c "**MockDataSource**" and "**MockServerState**".

**File:** `dm/MockDataSource.kt`, a line-by-line port of `MockDataSource.swift:13–1106` with the constructor, mutex and helpers of §3.2. Exact values (reviewer checks each against the Swift line):

| Rule | Value | Swift |
|---|---|---|
| Latency clamp | `min(0.8, max(0.3, delay()))` before every call | l.101 |
| Session token | `"mock-" + id()`; a login replaces only the same account **and** device (lower-cased) | l.180–197 |
| Session expiry | `now + 90 × 86400`, extended on **every** authenticated call and persisted | l.133–138, 194 |
| Unauthorized / unverified | `Unauthorized(401)`; `EmailUnverified(403)` for `fetchAccount`, `fetchTrips`, `fetchTripDetail`, `tripInput`, wizard calls, `deleteAccount`, `accountSecurity`, and `send` except `Profile` | l.131–139 |
| Rate windows (count / seconds) | `login:<email key>` 5/60; `two-factor:<challengeId>` 5/60; `registration` 5/60; `forgot` 5/60; `reset` 5/60; `passkey-options` 10/60; `passkey-login:<sha256(credential.id)>` 5/60; `passkey-login-ip` 20/60; `passkey-failed:<hash>` 20/3600 (validation failures only, checked before work); `guest-review:<device>` 5/60; `review:<owner>` 30/60; `trip-save:<owner>` 10/60; `credential:<accountID>` 6/60; `two-factor-confirm:<accountID>` 5/60 | l.122–127 and call sites |
| Email key | trim + lower-case | l.140–142 |
| Device checks | `device_id` must be a UUID (`"The :attribute field must be a valid UUID."`, `attribute = "device_id"`); `device_name` non-blank (`"Please fill this in."`), ≤ 255 code points (`"Use no more than :max characters."`, `max = 255`) | l.146–157 |
| Wrong credentials | `email` → `"These sign-in details don't match our records."` | l.208 |
| Password policy (one message, in order) | `< 12` chars `"Use at least :min characters."` (`min 12`); no upper or no lower `"Use both uppercase and lowercase letters."`; no digit `"Use at least one number."`; no symbol or punctuation `"Use at least one symbol."`; mismatch adds `password_confirmation` `"The passwords don't match yet."` | l.158–176 |
| Two-factor challenge | 64 hex (`id()` twice without dashes); expires `now + 300`; a wrong proof increments `attempts`, and removes the challenge when `attempts >= 4` already | l.209–216, 258–266 |
| Two-factor messages | blank proof (after trimming whitespace) → one `Validation` with both `code` `"Enter the 6-digit code from your authenticator app."` and `recovery_code` `"Enter one of the recovery codes you saved when you set up two-factor authentication."`; a code that is not exactly 6 ASCII digits → `code` `"Enter the 6-digit code from your authenticator app."`; unknown or expired challenge, or a wrong proof → `"The authentication code was not accepted. Try again."` (`code`) / `"This recovery code was not accepted. Try another saved code."` (`recovery_code`); a used recovery code is removed | l.229–274 |
| Sign-up | `name` blank → `"Please fill this in."`, > 255 code points → `"Use no more than :max characters."` (`max 255`); `email` without `@` → `"Enter a valid email address, like name@example.com."`; taken → `email` `"This email address is already in use."`; any claim field present but the guest draft not provably live and saveable → `review_expired` `"Review your trip again before creating your account."`; the email is stored trimmed and lower-cased; sends the verification mail | l.276–357 |
| Reset | request: `email` blank → `"Please fill this in."`, no `@` → `"Enter a valid email address, like name@example.com."`; non-enumerating (unknown email returns silently after the limit and the field checks); link `token = id()`, expires `now + 3600`; mail `PasswordReset(id(), normalizedEmail, now, ResetPasswordInput(token, email, "", ""))`. Reset: the password policy is checked **before** the link; success revokes every session and two-factor challenge of the account and deletes the link; failure `token` → `"This password reset link is invalid or has expired."` | l.358–393 |
| Verification | link `https://app.chronoflow.net/email/verify/<id>/<sha1(email)>?expires=<expires>` with `expires = floor(now seconds) + 3600`, signed with HMAC-SHA256 (hex) under `signingKey`; sending sets `resendAvailableAt = now + 60`; `resendAfterSeconds = max(0, ceil(resendAvailableAt − now))`, 0 when verified or for `verificationStatus`; resend while > 0 → `Throttled(resendAfterSeconds, 429)`; verify accepts `expires >= floor(now seconds)`; failure `signature` → `"This email verification link is invalid or has expired."` | l.394–446 |
| Passkey challenge | `challengeId = sha256Hex(id() + id())`; challenge = `base64Url(sha256("challenge:" + material))` (43 chars); `rpId "app.chronoflow.net"`, `timeout 60000`; expires `now + 120`; consumed only after the assertion passes `validateAssertion` | l.498–574 |
| Passkey login sizes | `challengeId` blank → `"Please fill this in."`; `challengeId` ≤ 64, `draft_token` ≤ 64, `guest_secret` ≤ 128 (`"Use no more than :max characters."`), every assertion field ≤ 8192 | l.546–557, 524 |
| Passkey messages | malformed assertion or no account from the provider → `credential` `"We couldn't sign you in with that passkey. Please try again."`; unknown or expired challenge → `challengeId` `"The passkey request expired. Please try again."` | l.531, 560, 566 |
| Send: unsupported | `OutboxChange.Unsupported(kind, _)` → `DataFailure.Unsupported(kind)` right after `begin`, **before** the session check | l.577 |
| Airport search | no session needed; raw query ≤ 60 characters and trimmed query `2…60`, `limit ?: 8` in `1…20`, else `Validation` on `limit` (when the trimmed query length is fine) or `query`, `"Choose one of the available options."`; matches IATA, name, city or country case-insensitively, in `airports()` order, first `limit` | l.721–735 |
| Check-off | `ordinal` in `1…255` (`"This step cannot be checked off."`), `local_date` `^\d{4}-\d{2}-\d{2}$` (`"Choose a valid local date."`), unknown trip → `Server(404)`, stale hash or no **actionable** block with that key → `form` `"This plan has changed. Refresh it before marking this step."`; same status replays the current state without a new version; else `detailVersion = id()`, `markedAt = iso(now)`, mirrored into the list item | l.616–650 |
| Policies | version must equal `termsVersion` (`"The terms have changed. Read the current version and agree again."`) | l.591–601 |
| Profile | name non-blank; the email must equal the current email, else `current_password` `"Please fill this in."` | l.582–590 |
| Two-factor setup | wrong current password → `current_password` `"The provided password was incorrect."` (422); secret `JBSWY3DPEHPK3PXP`, URL `otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP&issuer=ChronoFlow`; confirm code `123456` (else `code` `"The authentication code was not accepted. Try again."`); 8 recovery codes `"<first 8 of a random UUID, lower-case>-<i>"`, reveal/regenerate return them sorted. `form` failures: enable while on → `"Two-factor authentication is already on."` **409**; confirm while on → the same key but **422**; confirm without a pending setup → `"Start two-factor setup again."` 409; reveal/regenerate while off → `"Two-factor authentication is off."` 409 | l.927–1011 |
| Passkey management | list needs no limiter; every other action hits `credential:<accountID>` 6/60; registration challenge `base64Url(sha256("registration:" + material))`, user id `base64Url("user:" + accountID)`, expires `now + 120`, bound to the request token; add validation l.1048–1074 (`"Please fill this in."`, `"Use no more than :max characters."` with 64/255, `"The passkey registration request expired. Please try again."`, `"We couldn't register that passkey. Please try again."`); new id `max + 1`, inserted first; `passkeyCount` updated; remove of an unknown id → `Server(404)`; remove revokes every other session of the account | l.1018–1105 |
| Guest review | ≤ 5 live drafts per device (oldest by `expiresAt`, then key, evicted); drafts expire `now + 1800`; a minted secret only on the first review of an unknown device secret; prompts filtered to `["age_band"]`/`["shift_work"]` | l.657–719 |
| Wizard | drafts `now + 1800`; receipts `now + 86400`; the save intent = canonical JSON of `ApiTripSaveInput` + `":" + (tripID ?: "") + ":" + (sourceRevision ?: "")`; a receipt replay with another intent → `ReviewExpired`; a review that cannot be saved → `NotEnoughCredits` (checked before the `trip-save` limiter); credits `balance − cost`; paid directions as l.880–898 (use `distinct()` to keep order) | l.745–907 |
| Sign-up claim | `credits = 3 − cost` when a guest draft is claimed; claim lives `now + 7 × 86400` | l.276–357 |
| Delete | `password` → `"The provided password was incorrect."`; removes the account and its sessions | l.912–925 |
| Remote update hook | `fetchTrips` toggles the first trip's flight `CF123` ↔ `CF456` and `detailVersion = "refresh-<flight>"` when `remoteTripUpdate()` is true | l.465–479 |

`load()` follows §3.1 with `debugAppSeed` in place of `#if DEBUG` and `MockAppSeed.make(now(), zone = zone)` / `refresh(restored, now(), zone)`.

**Tests** (ported, Swift names)
- `dmt/MockScenarios.kt`: `demoID`, `twoFactorID`, `demoTripID = "0199a3c0-0000-7000-8000-000000000010"`, `mockDeviceID = "0199a3c0-0000-7000-8000-000000000100"`, `fixturePassword`, `fixtureRecovery`, `newPassword = "NewPassword12!"`; `class MockClock` (§3.1; `sleep` records the delay and calls `currentCoroutineContext().ensureActive()`; thread-safe); `scenarioState()` (0d fixtures `account/snapshot.json`, `trips/list.json`, `trips/detail.json`: demo gets the list's trips and `details = {demoTripID: detail}`; the two-factor account copies the snapshot with its id and email, code and recovery code, no trips; `signingKey = "artificial-mock-signing-key"`); `mockSource(clock = MockClock(), store = MemoryMockStateStore(), seed = null, bearer = null, io = Dispatchers.Unconfined)` building `MockDataSource(store, seed ?: scenarioState(), now = clock::now, delay = { 0.5 }, sleep = clock::sleep, id = clock::id, bearer, assertionProvider = …, zone = Europe/Amsterdam, io = io)` with the assertion provider of l.58–67 (`signature == "c2ln"` and `clientDataJSON == challenge.options.challenge`; `"AA"` → demo, `"AQ"` → two-factor, else null); `passwordInput(email = demo, password = "password", device = mockDeviceID)` with `deviceName "iPhone"`, `login` (throws `Server(null)` unless the result is a session), `passkeyInput(challenge, credentialID = "AA", signature = "c2ln")` (`rawId` `"AA=="`/`"AQ=="`, `clientDataJSON = challenge.options.challenge`, `authenticatorData "AA=="`, `deviceName "iPhone"`), `registration(email = "new@example.com")` (name `"New Traveller"`, password and confirmation `newPassword`, `deviceName "iPhone"`, language `Nl`).
- `MockAuthTests`: all 17 Swift tests (Appendix B: `emptyTwoFactorProofReturnsBothRequiredWithoutErrors` becomes 4 row methods; `confirmationKeepsIndependentPasswordErrors` becomes the Parameterized class `MockPasswordConfirmationTests`, 6 rows labelled by the password value), plus the Android test `passwordPolicyUsesUnicodeCategories` (new, OQ-4c-15). In `latencyOfflineAndFailOnce` the clamped sources are built with `seed = null`, so `fetchIdentity` reaches `Unauthorized(401)`; recorded delays are `[0.3, 0.3, 0.8, 0.8]` for `-1.0, 0.3, 0.8, 10.0`. Swift builds them with the DEBUG app seed (four `MockAppSeed.make` calls); build them with `debugAppSeed = false` instead, which keeps the asserted behaviour (no token → 401, the same clamped delays) and the 1 s budget, and say so in the test's KDoc. `canceledMockCallNeverChangesState`: start `async { currentCoroutineContext().cancel(); source.signUp(registration()) }`, expect `CancellationException`, inbox empty.
- Coroutine tests use `runTest(timeout = 60.seconds)`.

### Task 7. Ported account, persistence and passkey-management tests

Commit: `test(data-mock): port the mock account, persistence, passkey and security tests`. Ticks nothing.

- `MockAccountTripsTests`: all 7 (`claimIsAccountAndSecretBound` included: claims are core sign-in behaviour).
- `SharedMockServerTests`: both. `concurrentMemoryMutationsRetainEveryMail` runs the two calls in `withContext(Dispatchers.Default)` so they truly overlap.
- `MockPersistenceTests`: all 4 with `FailingMockStore` (wraps a `MemoryMockStateStore` but is not one, so the source caches state per instance as with a file store; `save` throws `DataFailure.Server(null)` while failing). "Off main" becomes "on the injected I/O dispatcher": the test passes `mockSource(…, io = Executors.newSingleThreadExecutor { Thread(it, "mock-io") }.asCoroutineDispatcher())` (closed in `@After`) and `FailingMockStore` asserts `Thread.currentThread().name == "mock-io"` in `load` and `save`. File stores use `TemporaryFolder`. `concurrentRegistrationsKeepAllAccounts` runs the five sign-ups in `withContext(Dispatchers.Default)`.
- `MockPasskeyManagementTests`: all 7 (`addRejectsEachBadInputWithoutRegistering` is the Parameterized class `MockPasskeyAddFaultTests`, 7 rows `challenge`, `expired`, `attestation`, `padded`, `duplicate`, `name`, `long`; its setup helpers move to a shared `dmt/MockPasskeyFixtures.kt`).
- `AccountSecurityAPITests.mockRunsSetupCodesManageAndPasswordAndRejectsOffline` (Swift l.69–100; only this method in the `:core:data-mock` class of that name): signed in as demo; `EnableTwoFactor("wrong")` → `Validation({"current_password": FieldMessage("The provided password was incorrect.")}, 422)`; `EnableTwoFactor("password")` → `Setup`; `ConfirmTwoFactor("123456")` → `Codes` with 8 recovery codes; `fetchAccount().security["twoFactorEnabled"] == JsonPrimitive(true)`; reveal, regenerate and disable with `"password"` succeed; then `twoFactorEnabled == JsonPrimitive(false)` and `twoFactorConfirmedAt == JsonNull`; `Password("password", "New password1!", "New password1!")` succeeds; `setOffline(true)` → `EnableTwoFactor("New password1!")` throws `Offline`.
- `SettingsDeleteAPITests.mockDeletionRequiresPasswordAndActuallyRemovesAccount` (Swift l.32–48; only this method): `deleteAccount("wrong")` → `Validation({"password": FieldMessage("The provided password was incorrect.")}, 422)` and `fetchIdentity().id` is still the session's; offline → `Offline`, identity unchanged; back online `deleteAccount("password")` succeeds and `fetchIdentity()` → `Unauthorized(401)`.

### Task 8. Ported seeding-through-the-source tests

Commit: `test(data-mock): port the app seed and refresh tests through the data source`. Ticks nothing.

- `MockAppSeedTests`: `defaultAppMockSignsInDemoWithRNSettingsAndTrips` (7 trips, destinations `[HND, JFK, SYD, LHR, DXB, AMS, AMS]`, passkeys `["iPhone 16", "MacBook Pro"]`, `credits == JsonPrimitive(3)`, `leadMin == JsonPrimitive(15)`, `acceptedTermsVersion "2026-10-01"`, readiness `ready`), `defaultAppMockChallengesTwoFactorAndAcceptsRNProofCode` / `…Recovery`, `authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequestVerified` / `…Unverified`, `defaultAppMockReseedsEmptyAndOldStatesAndPreservesCurrentChanges`, `defaultAppMockKeepsRecoveryConsumptionAcrossRestart`, `defaultAppMockReseedsIncompatibleFilesEmptyObject` / `…Corrupt` / `…Legacy` (payloads `"{}"`, `"corrupt"`, and `"legacy"` = a seed with `schemaVersion` removed), `explicitEmptyAndCustomSeedsBypassAppReseeding`, and `releaseDefaultMockHasNoDemoSeed` with `debugAppSeed = false`. `appMock(store)` is `MockDataSource(store, now = MockClock().now, sleep = {}, zone = Europe/Amsterdam)` (the iOS factory indirection is out of scope; `apiFactoryNeverConstructsMockSeed` is 4b/4e).
- `MockSeedRefreshTests.restoresTenDayOldSeedWithoutLosingUserStateLegacyAnchor` / `…CurrentAnchor` (Swift `legacyAnchor: true` / `false`; file store in `TemporaryFolder`; `now = 2026-11-07T12:00:00Z`).

### Task 9. Ported wizard and guest tests

Commit: `test(data-mock): port the mock wizard and guest tests`. Ticks nothing. Also amend `plan.md` Phase 11a "**Guest review endpoint.**" and Phase 11b "**Claim**" with "(mock side ported in 4c, OQ-A<n>)" (OQ-4c-1).

- `dmt/MockWizardFixtures.kt` (shared test helpers; the Swift ones live in other test files that are not in this module): `wizardInput()` = `wizard/input.json` decoded as `ApiTripInput` with `ApiJson.response` (Swift `TripWizardAPITests.swift:218–220`); `guestReviewInput()` = `ApiGuestTripReviewInput(input = wizardInput(), identity = null, profile = wizard/guest-review-request.json["profile"], deviceID = …["device_id"], guestSecret = null)` (Swift `GuestTripReviewAPITests.swift:94–98`, same as 4b's form); `wizardProfile()` = `{age_band "18_64", shift_work false, exclude_caffeine false, flight_movement_breaks false}` and `wizardMockSource(store)` = `mockSource(store, seed = scenarioState()` with demo `planCredits = {credits 3, unlimited false}`) (Swift `MockTripGenerationTests.swift:158–171`).
- `MockTripGenerationTests`: all 6, using `MockWizardFixtures.kt`; `unknownAirportIsAFieldErrorBeforePlanGeneration` becomes `…Origin` / `…Destination` (Swift `destination: false` / `true`).
- `MockGuestReviewTests`: all 13, with `guestMockSource` and `continuedGuestInput` in a shared `dmt/MockGuestFixtures.kt`; `missingWrongOrForeignPossessionExpires` is the Parameterized class `MockGuestPossessionTests` (6 rows); the other three parameterized tests are row methods (Appendix B).
- `MockGuestRegistrationClaimTests`: the remaining 11, with `guestRegistration` (also in `MockGuestFixtures.kt`); `staleClaimsCreateNothing` is the Parameterized class `MockStaleClaimTests` (10 rows) and `loginReplaysOnlyLiveMatchingReceipt` the Parameterized class `MockClaimReplayTests` (6 rows); `retryMustProveAccountAndPossession` becomes three row methods.
- `NoHealthDataTests.mockRejectsEveryRetiredReviewProfileKeyWith422` (Swift l.135–152, with `wizardMockSource()` and `wizardInput()` from the wizard fixtures): signed in, `reviewTrip(ApiTripReviewInput(wizardInput(), profile = {key: false}))` for each key of the Swift literal `retired` (`NoHealthDataTests.swift:8`) plus `caffeine_ruled_out`, i.e. exactly `bipolar`, `eye_disease`, `pregnancy`, `vte`, `sleep_phase_disorder`, `oral_contraception`, `caffeine_ruled_out` → `ProfileInvalid(["profile.form": FieldMessage("This setting cannot be changed here.")])` with status 422. Declare the six retired keys as a private constant in this class: test sources are not shared between modules, so 0d's copy in `:core:model` tests cannot be reused.

### Task 10. Parity with the API path and per-method controls

Commit: `test(data-mock): prove api parity and per-method mock controls`. Ticks `plan.md` Phase 4c "**Ported tests.**".

- `DataSourceParityTests` (Swift names, all 3): `invalidSettingsReturnEveryFieldReminders` / `…Preferences` / `…BodyClock` (Swift l.8–38: patches reminders `{lead_min 45, light "x", winddown true}`, preferences `{time_format "13", show_optional_items "x"}`, bodyClock `{habitual_bed "25:00", habitual_wake "99:00"}`; the expected `Validation` lists every patch key except `winddown`, message `"Enter a valid time."` for bodyClock else `"Choose one of the available options."`, status 422; the API side is answered with the 422 envelope `{"code":"validation_failed","message":"The given data was invalid.","errors":{<field>: <message>}}` on `PATCH /v1/account/reminders`, `/v1/account/preferences` or `/v1/account/body-clock` (4b Appendix A rows 25b–25d); inside the method both sources × both key orders, as Swift does; the mock account is unchanged afterwards), `wrongCredentialsMatchBackendValidation` (`POST /v1/auth/login` answered 422 with `errors/wrong_credentials.json`; both sources throw `Validation({"email": "These sign-in details don't match our records."}, 422)`), `sharedRecordedContract` (routes `POST /v1/auth/login` and `POST /v1/auth/passkey/login` → `auth/session.json`, `POST /v1/auth/passkey/options` → `auth/passkey-options.json`, `GET /v1/users/me` → `account/identity.json`, `GET /v1/account` → `account/snapshot.json`, `GET /v1/trips` → `trips/list.json`, `GET /v1/trips/<demoTripID>` → `trips/detail.json`, all 200; for each source: `login`, identity, account, first trip id, detail, passkey options (`rpId "app.chronoflow.net"`, `timeout 60000`) and `passkeyLogin(passkeyInput(options))` equal the fixtures, Swift l.53–84). The API side is 4b's `ApiDataSource(ApiClient(…))` against mockwebserver3 through a **deny-by-default** rule: copy 4b's `core/network/src/test/kotlin/net/chronoflow/core/network/support/MockApi.kt` (test sources are not shared between modules) to `dmt/support/MockApi.kt` unchanged except the package; an unmatched request fails the test. `MockApi.client(…)` names `OkHttpClient`; 4b exposes it with `api(libs.okhttp)`, so it resolves transitively without another dependency.
- `dmt/MockMethodCalls.kt` (new, test helper): `data class MockMethodCall(val setup: suspend (MockDataSource) -> Unit, val call: suspend (MockDataSource) -> Unit)` and `val MOCK_METHOD_CALLS: Map<MockMethod, MockMethodCall>` with one minimal `call` per method (inputs from `MockScenarios`, `MockWizardFixtures`). `setup` does whatever the call needs first (for most methods `login(source)` as demo; `{}` for `signIn`, `signUp`, `passkeyOptions`, `requestPasswordReset`, `searchAirports`, `reviewGuestTrip`; `passkeyOptions()` first for `passkeyLogin`; `signIn` of the two-factor account for `completeTwoFactor`). Every control test runs `setup` **before** `failNext`, `setOffline` or the first call, so a setup never trips over the offline switch and never consumes the injected failure (e.g. `signIn` of the two-factor account before `CompleteTwoFactor`). Since this table depends on Task 9's wizard fixtures, Task 10 comes after Task 9.
- `MockMethodCoverageTest` (new, plain): `tableCoversEveryMethod`: `MOCK_METHOD_CALLS.keys == MockMethod.entries.toSet()` (25).
- `MockMethodControlTests` (new, **Parameterized**, 25 rows labelled by `MockMethod.name`), one fresh `mockSource()` per case (`MockClock` sleep, scenario seed), `setup` run first:
  - `failNextIsOneShot`: `failNext(m, Server(599))` → the call throws `Server(599)`; the next identical call does **not** throw `Server(599)` (it may throw another `DataFailure`, e.g. a rate limit or validation).
  - `offlineFailsBeforeFailNext`: `failNext(m, Server(599))`, `setOffline(true)` → `Offline`; `setOffline(false)` → the pending `Server(599)` is thrown now (offline did not consume it).
  - `mutationsOfflineBlocksOnlySend`: a source built with `mutationsOffline = true` → the call throws `Offline` exactly when `m == Send`.

### Task 11. Release readiness and the import boundary

Commit: `test(app): prove release ships no mock code`. Ticks `plan.md` Phase 4c "**Wiring.**".

The dependency lines already exist (`debugImplementation(project(":core:data-mock"))` and OQ-0e-26’s deferred `configurations.matching { … }.configureEach { … }` block for `benchmarkReleaseImplementation`/`nonMinifiedReleaseImplementation`), and 0e's `ModuleGraphTest.dataMockOnlyInDebugAndPerf` guards them. **Do not edit `app/build.gradle.kts`.** Never add `implementation`, `releaseImplementation`, `testImplementation` or `androidTestImplementation` of `:core:data-mock` to `:app`: a test-classpath dependency would put the classes on the `testReleaseUnitTest` classpath and make the lookups below find them.

- `ReleaseReadinessTest` (`app/src/testRelease/kotlin/net/chronoflow/app/ReleaseReadinessTest.kt`, plain JVM, runs in `testReleaseUnitTest`): add one test, `mockDataSourceIsAbsent`, asserting `Class.forName(name)` throws `ClassNotFoundException` for each of `net.chronoflow.core.datamock.MockDataSource`, `net.chronoflow.core.datamock.MockAppSeed`, `net.chronoflow.core.datamock.MockServerState`, `net.chronoflow.core.datamock.MockScenarioValues` (four assertions written out, not a loop over a table: it is one fact, "no mock class ships"). 4e T12 lists the first two again; OQ-4c-13.
- Konsist rule `onlyDebugAndPerfImportTheMock`, following 0a 6b exactly: one function in `konsist/src/test/kotlin/net/chronoflow/konsist/KonsistRules.kt`; in `KonsistRulesTest.kt` the trio `onlyDebugAndPerfImportTheMock()` (project scope, must pass), `onlyDebugAndPerfImportTheMockRejectsPlantedViolation()` (scope from `konsist/src/test/resources/violations/onlyDebugAndPerfImportTheMock/`, must throw) and `onlyDebugAndPerfImportTheMockAllowsControl()` (scope from `konsist/src/test/resources/allowed/onlyDebugAndPerfImportTheMock/`, must pass). Rule: for every file with `moduleOf(path) != null`, an import equal to `net.chronoflow.core.datamock` or starting with `net.chronoflow.core.datamock.` is allowed only when `moduleOf(path) == "core/data-mock"`, or `isTestSourceSet(sourceSetOf(path))`, or `moduleOf(path) == "app"` and `sourceSetOf(path)` is `debug` or `perf`. Planted: `violations/onlyDebugAndPerfImportTheMock/app/src/main/kotlin/net/chronoflow/app/UsesMock.kt` and `violations/onlyDebugAndPerfImportTheMock/feature/today/src/main/kotlin/net/chronoflow/feature/today/UsesMock.kt`, each importing `net.chronoflow.core.datamock.MockDataSource`. Control: `allowed/onlyDebugAndPerfImportTheMock/app/src/debug/kotlin/net/chronoflow/debug/UsesMock.kt`, `allowed/onlyDebugAndPerfImportTheMock/app/src/perf/kotlin/net/chronoflow/app/perf/UsesMock.kt` and `allowed/onlyDebugAndPerfImportTheMock/core/data/src/test/kotlin/net/chronoflow/core/data/UsesMockTest.kt` with the same import. Planted files stay outside every compiled source set, as 0a set up.
- `docs/tooling.md`, section "Phase 4c": the commands and outputs of the dependency probes and the `apkanalyzer` probe of §7 item 5.

---

## 7. Acceptance

All commands from the checkout root with `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`. If the sandbox cannot run one, report the command and its exact output; the orchestrator runs it.

1. `./gradlew :core:data-mock:test :konsist:test` passes, and so does `make targeted ONLY="core:data-mock:MockTimeTest core:data-mock:MockCryptoTest core:data-mock:MockRateLimiterTest core:data-mock:MockLatencyTest core:data-mock:MockServerStateCodecTest core:data-mock:FileMockStateStoreTest core:data-mock:MemoryMockStateStoreTest core:data-mock:NoHealthDataTests core:data-mock:MockRebaseTests core:data-mock:PlanDecodingTests core:data-mock:MockSeedRefreshTests core:data-mock:MockAppSeedTests core:data-mock:MockTripGenerationHelpersTest core:data-mock:MockAuthTests core:data-mock:MockPasswordConfirmationTests core:data-mock:MockAccountTripsTests core:data-mock:SharedMockServerTests core:data-mock:MockPersistenceTests core:data-mock:MockPasskeyManagementTests core:data-mock:MockPasskeyAddFaultTests core:data-mock:AccountSecurityAPITests core:data-mock:SettingsDeleteAPITests core:data-mock:MockTripGenerationTests core:data-mock:MockGuestReviewTests core:data-mock:MockGuestPossessionTests core:data-mock:MockGuestRegistrationClaimTests core:data-mock:MockStaleClaimTests core:data-mock:MockClaimReplayTests core:data-mock:DataSourceParityTests core:data-mock:MockMethodCoverageTest core:data-mock:MockMethodControlTests"` (module paths **without** a leading colon, 0a `Scripts/targeted.py`).
2. Every Swift test of §2.2 maps to the Kotlin test(s) Appendix B names, and each asserts every Swift expectation: 17 + 7 + 4 + 2 + 7 + 9 + 1 + 2 + 6 + 13 + 12 + 3 + 2 + 1 + 1 + 1 + 1 = **89** Swift tests. From the JUnit XML under `build/test-results/targeted/`: `MockMethodControlTests` has **75** cases (25 methods × 3), `MockPasskeyAddFaultTests` 7, `MockPasswordConfirmationTests` 6, `MockGuestPossessionTests` 6, `MockStaleClaimTests` 10, `MockClaimReplayTests` 6; `MockMethodCoverageTest.tableCoversEveryMethod` passes (25/25).
3. Every `:core:data-mock` test is < 1 s in `timings.json`; none is annotated `@PerfBudget` (OQ-4c-9). List the five slowest in the hand-off.
4. `make quick` passes and stays under 60 s warm; report the measured time.
5. Release proof (record commands and outputs in `docs/tooling.md`, Task 11):
   - `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c "project :core:data-mock"` prints `0`, and the same with `"project :core:testing"` prints `0` (this phase makes `:core:testing` a main dependency of `:core:data-mock`, so it now also reaches debug and perf transitively; it must still never reach release);
   - the same for `debugRuntimeClasspath`, `benchmarkReleaseRuntimeClasspath` and `nonMinifiedReleaseRuntimeClasspath` prints ≥ `1` (run without the configuration cache if the task refuses it, and say so);
   - `./gradlew :app:assembleRelease`, then `$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer dex packages "$(find app/build/outputs/apk/release -name '*.apk' | head -1)" | grep -c "net.chronoflow.core.datamock"` prints `0`;
   - `./gradlew :app:testReleaseUnitTest :app:verifyReleaseManifest` passes, including `ReleaseReadinessTest.mockDataSourceIsAbsent`.
6. Konsist: `onlyDebugAndPerfImportTheMock` passes on the project, rejects both planted files and accepts its controls; `jvmCoreModulesImportNoAndroidOrCompose`, `noRunBlockingOutsideTests`, `noSleepInTests`, 4b's `okhttpOnlyInCoreNetwork` and 0e's `ModuleGraphTest.dataMockOnlyInDebugAndPerf` still pass.
7. `git diff <stack-top>.. -- app/build.gradle.kts` is empty (the wiring was 0a's and 0e's).
8. The user runs `make check` on the phase branch with `pixel` booted; this phase runs only targeted JVM checks and `make quick` once (user, 2026-11-18). No emulator test, screenshot, capture or perf number is produced by this phase (no UI, no timeline change).
9. Each `plan.md` Phase 4c checkbox is ticked in the commit named in its task (Appendix A); new open questions are in `plan.md` "Open questions for the user"; commits are atomic Conventional Commits without trailers.
10. Nothing was written inside `/Users/rickgout/Sites/chronoflow-ios`, `/Users/rickgout/Sites/chronoflow-new` or `/Users/rickgout/Sites/chronoflow-mobile`: `git -C <repo> status --porcelain` is unchanged before and after (record both).

---

## 8. Review checklist (Opus reviewer)

Port fidelity (diff each Kotlin function against the Swift, line by line):
- [ ] `begin` order exactly as §3.1; offline does not consume `failNext`; `failNext` is removed when thrown; bearer skipped for `reviewGuestTrip`; `requestToken` comes from `currentToken` only when no `bearer` was injected.
- [ ] `load` branches: memory store re-reads every call; file store caches per instance; decode error tolerated only without a `seed`; restored state saved back; `debugAppSeed` refresh vs make vs empty; corrupt file plus seed → `Server(null)` and the file is untouched.
- [ ] `update` mutates a copy and saves before replacing the state; a failed save leaves the account unchanged (`lazyLoadOffMainAndSaveFailurePreservesState`). No mutable collection is shared between `state`, the store and a returned value.
- [ ] Every row of the Task 6 table, value by value (limits, expiries, token formats, messages, field names).
- [ ] Copy keys identical to Swift. Run:
  ```sh
  S=/Users/rickgout/Sites/chronoflow-ios; P=Packages/ChronoFlowKit/Sources/DataAccess/Mock
  for f in MockDataSource MockSettings MockTripGeneration MockWizardPrompts MockGuestReview MockServerState MockAppSeed; do git -C $S show f391a1fa:$P/$f.swift; done | grep -oE '"[A-Z][^"\\]*[.?!]"' | sort -u > "$TMPDIR/swift-keys"
  cat core/data-mock/src/main/kotlin/net/chronoflow/core/datamock/*.kt | grep -oE '"[A-Z][^"\\]*[.?!]"' | sort -u > "$TMPDIR/kotlin-keys"
  comm -23 "$TMPDIR/swift-keys" "$TMPDIR/kotlin-keys"   # must print nothing
  ```
- [ ] Rebase: the zone chosen per key (`endUtc`, `arrivalLandingUtc`, `assumedReturn`, `markedAt` untouched); instants written with whole seconds and `Z`; `labelRange` uses `endUtc − 1 s` and becomes `null` when not after `labelDate`; hash string `"<hash>:<days>"` (+ `":<minutes>"` only when non-zero); `detailVersion` suffix; tick offsets recomputed.
- [ ] Seed: index targets (−1 start, −3 **end** for index 2, `3 + i` start), large trips appended unrebased, two-factor account shares the trips, demo security object exact, `seedAnchor` in the injected zone, refresh keeps user changes and re-keys marks.
- [ ] `MockServerState`: `schemaVersion` 2 and `@Required` on the Swift non-optional fields; `MockClaim` never writes `secret`; secrets redacted in `toString`.

Android rules:
- [ ] `app/build.gradle.kts` is unchanged by this phase; `:core:data-mock` appears only in 0a's debug line and 0e's perf lines, never in a test classpath of `:app`; main code of the module does not depend on `:core:network` or OkHttp; the module applies the serialization plugin itself (a missing plugin compiles `@Serializable` classes without serializers and fails only at run time).
- [ ] No JDK API above Android API 29 (`HexFormat`, `List.of`, `Map.of`, `Stream.toList`, `Optional.isEmpty`, `String.repeat` from Java, …). `grep -rnE "HexFormat|List\.of\(|Map\.of\(|Set\.of\(|\.toList\(\)" core/data-mock/src/main` → only Kotlin `toList()` on collections.
- [ ] No `Instant.now()`/`ZoneId.systemDefault()` outside the two constructor defaults; no `Random`/`UUID.randomUUID` where Swift uses the injected `id()` (recovery codes and the guest secret use real randomness, as in Swift).
- [ ] Store I/O only inside `withContext(io)`; the mutex is never re-entered (the `requestPasswordReset` delegation); no `runBlocking`; `CancellationException` never swallowed; no `runCatching`/`catch (e: Exception)`.
- [ ] Whole-number JSON written as `Int`/`Long` primitives; numeric validation compares numbers, not `JsonPrimitive`s; no `.content` read on `JsonNull`.
- [ ] No `ByteArray` in a data class.

Tests:
- [ ] Swift method names kept; every Swift `@Test(arguments:)` has exactly the Kotlin form Appendix B names (row methods `<swiftName><Row>` or its own `Parameterized` class); no loop over rows inside one `@Test`, no plain test inside a `Parameterized` class, no `Enclosed`; plain JVM classes carry no tier annotation; Android-only tests are marked "(new)" in their KDoc.
- [ ] No `Thread.sleep`, no real delays (only `MockClock.sleep` or virtual time); `runTest(timeout = 60.seconds)`; the two concurrency tests really run on `Dispatchers.Default`.
- [ ] `MockMethodCoverageTest.tableCoversEveryMethod` compares against `MockMethod.entries` (fails if the table misses one); `MockMethodControlTests` has 25 rows × 3 tests.
- [ ] The Konsist rule matches whole package segments (`== "net.chronoflow.core.datamock" || startsWith("net.chronoflow.core.datamock.")`), uses 0a's `ProjectPaths` helpers, and has its planted and control files under `violations/onlyDebugAndPerfImportTheMock/` and `allowed/onlyDebugAndPerfImportTheMock/`.
- [ ] The two 4b hand-off tests (`AccountSecurityAPITests.mockRuns…`, `SettingsDeleteAPITests.mockDeletion…`) exist in `:core:data-mock` and assert every Swift expectation.
- [ ] Parity tests use a deny-by-default server; no test touches the network.

Typical mistakes to look for: `Instant.toString()` producing `…:00.123Z`; `make targeted ONLY=":core:data-mock:…"` (leading colon: `targeted.py` rejects it); `Char.isUpperCase()` on UTF-16 units or `String.length` for the password minimum (Swift counts grapheme clusters); `JsonPrimitive(3.0)` for credits; Kotlin default values making a schema-less file decode as schema 2; a `data class` holding `ByteArray`; mutating a `MutableMap` taken from the shared memory store; taking the mutex inside `requestPasswordReset` while `accountSecurity` holds it (deadlock); `ZonedDateTime.plusHours(24)` instead of `plusDays(1)`; sorting validation fields instead of visiting keys in sorted order and keeping the map; counting a malformed passkey assertion as a consumed challenge; forgetting that `reviewGuestTrip` ignores the bearer.

---

## 9. Open questions (proceed on the default; add each to `plan.md` "Open questions for the user" with the next free OQ-A number in the commit that relies on it)

| # | Question | Default |
|---|---|---|
| OQ-4c-1 | Plan 11a/11b list the mock guest review (iOS 18c) and the mock registration claim (18d) as their own tasks, but the claim is woven into `signIn`, `signUp`, `passkeyLogin` and `authenticate`, and `MockAccountTripsTests.claimIsAccountAndSecretBound` tests it. Split the port? | Port the whole `MockDataSource` in 4c, guest review and claim included (Tasks 6 and 9). 11a/11b keep their API and UI work; their mock items become "verify". Task 9 notes this in `plan.md`. |
| OQ-4c-2 | Plan 4c depended on 4b only, yet it needs 1b's `MockTripTemplates`, `mock-seed.json` and `LargeTimelineFixtures`. | Resolved (review 2026-11-16): plan.md now lists 4b and 1b, wave 5. If 1b is not in the stack, stop and report it. |
| OQ-4c-3 | Plan says "latency from `CF_MOCK_DELAY`, default 0"; iOS uses a clamped 0.3–0.8 s random delay for ordinary debug runs and exactly `CF_MOCK_DELAY` (default 0) for seeded runs. | Follow iOS: the default `MockDataSource` latency is the clamped random delay; seeded runs (4e) pass `MockLatency.exact(CF_MOCK_DELAY)`; unit tests inject `MockClock.sleep` (zero wall time). |
| OQ-4c-4 | iOS chooses demo seeding with `#if DEBUG`; on Android the module exists in debug and perf builds only. How do perf builds behave? | A constructor flag `debugAppSeed` (default `true`). 4e passes `true` in debug and `false` in perf variants. Verified iOS behaviour: the Perf configuration (`project.yml:66–68`) compiles without `DEBUG` (only `CHRONOFLOW_TIMELINE_PERF`), so `MockAppSeed` does not exist there, the mock takes the `restored ?? MockServerState()` branch and never seeds, and the perf trip page reads fixtures directly (`App/Debug/TripPageDiagnosticRoot.swift`, `#if DEBUG \|\| CHRONOFLOW_TIMELINE_PERF`). Android keeps `MockAppSeed` in the module, so a perf path that needs seeded data may pass an explicit `seed` (e.g. `MockAppSeed.make(…, includeTimelinePerformanceTrips = true)`); that is an Android choice for 4e/1e, not iOS parity. |
| OQ-4c-5 | `02 §5.4` says `MockMethod` has 26 cases. | 25, as in `MockDataSource.swift:5–11` (one per `DataSource` method). |
| OQ-4c-6 | Must the Android state file be readable by iOS or byte-compatible? | No. Android-only JSON with the Swift property names and `schemaVersion = 2`; a file from any other shape is reseeded in debug. |
| OQ-4c-7 | Plan lists "rate windows for forgot, resend and verify"; iOS has a limiter for forgot (and 13 other keys), a 60 s `resendAvailableAt` for resend, and none for verify. | Port the iOS set exactly (Task 6 table). |
| OQ-4c-8 | A contract type 4b should have added (`OutboxChange`, `MutationReceipt`, `AccountSecurityChange/Result`, `SettingsPatchValidation`, `TripWizardErrors`) is missing. | Add it to `:core:model` in a preparatory commit ported from the Swift file at the pin, with its Swift contract test, and tell the orchestrator so the 4b brief is corrected. |
| OQ-4c-9 | Decoding the 695 KB seed and rebasing seven templates may push a JVM test over the 1 s budget. | Memoize the decoded fixtures per JVM and warm them in `@BeforeClass` (or a companion `by lazy` touched there). If a single case still exceeds 1 s, report its timing; do not add `@PerfBudget`. |
| OQ-4c-10 | iOS uses the device zone (`TimeZone.current`) for the seed anchor. | Inject `zone: ZoneId` (default `ZoneId.systemDefault()` in production); tests pin `Europe/Amsterdam`. |
| OQ-4c-11 | The demo passkeys are named "iPhone 16" and "MacBook Pro" (copied from the RN demo). | Keep them for iOS parity (tests assert them); they are debug mock data and never ship. Revisit if the designer wants Android device names in captures. |
| OQ-4c-12 | `:core:data-mock` runs on ART but Lint's `NewApi` check does not cover JVM modules. | Review rule (§8) plus the grep; no desugaring or Animal Sniffer added in this phase. |
| OQ-4c-13 | The plan gives the mock's release-readiness assertion to 4c, and 4e T12 also lists `MockDataSource` and `MockAppSeed` lookups. | 4c adds `ReleaseReadinessTest.mockDataSourceIsAbsent` (four classes). 4e keeps its other lookups and does not repeat these two; note it in the hand-off so the orchestrator corrects the 4e brief. |
| OQ-4c-14 | Later JVM tests (4d, 5a–5d, 9b) may want `MockClock` and the `MockScenarios` helpers, but test sources are not shared between modules. | Keep them in `core/data-mock/src/test` for now. A later phase that needs them moves them to a `testFixtures` source set of `:core:data-mock` (Gradle `java-test-fixtures`) in its own commit; no phase copies them. |
| OQ-4c-15 | Swift `String.count` counts grapheme clusters and its `CharacterSet`s are Unicode categories; Kotlin `length` counts UTF-16 units. | Count graphemes with `BreakIterator.getCharacterInstance(Locale.ROOT)` and classify code points by `Character.getType` (§3.2). Ported tests use ASCII; add one Android test `MockAuthTests.passwordPolicyUsesUnicodeCategories` (new, Task 6): `signUp` with password and confirmation `"Äbcdefghijk1!"` succeeds (`Ä` is `UPPERCASE_LETTER`), and with `"ÄBCDEFGHIJK1!"` fails with `"Use both uppercase and lowercase letters."`. |

---

## Appendix A. Plan checkbox mapping

| `plan.md` Phase 4c checkbox | Ticked by |
|---|---|
| **MockDataSource** | Task 6 |
| **MockServerState** | Task 6 |
| **MockAppSeed.** | Task 4 |
| **Wiring.** | Task 11 |
| **Ported tests.** | Task 10 |

## Appendix B. Swift `@Test(arguments:)` → Kotlin (table rule, 0d §4.2)

Row methods live in the Swift-named class and each call one private helper. Parameterized classes hold only that test, with `@Parameters(name = "{0}")` and the label column shown.

| Swift test (file:line) | Rows | Kotlin |
|---|---|---|
| `MockAuthTests.emptyTwoFactorProofReturnsBothRequiredWithoutErrors(recovery:value:)` (l.33) | 4: `recovery` false/true × `""`/`" \n"` | `MockAuthTests.emptyTwoFactorProofReturnsBothRequiredWithoutErrorsCodeEmpty`, `…CodeWhitespace`, `…RecoveryEmpty`, `…RecoveryWhitespace` |
| `MockAuthTests.confirmationKeepsIndependentPasswordErrors(value:passwordError:)` (l.254) | 6: `NewPassword12!` (no password error), `short`, `alllowercase12!`, `ALLUPPERCASE12!`, `NoNumbersHere!`, `NoSymbolsHere12` | `MockPasswordConfirmationTests.confirmationKeepsIndependentPasswordErrors` (Parameterized, label = the value) |
| `MockPasskeyManagementTests.addRejectsEachBadInputWithoutRegistering(_:)` (l.82) | 7: `challenge`, `expired`, `attestation`, `padded`, `duplicate`, `name`, `long` | `MockPasskeyAddFaultTests.addRejectsEachBadInputWithoutRegistering` (Parameterized) |
| `MockAppSeedTests.defaultAppMockChallengesTwoFactorAndAcceptsRNProof(recovery:)` (l.64) | 2 | `…Code`, `…Recovery` |
| `MockAppSeedTests.authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequest(unverified:)` (l.90) | 2 | `…Verified`, `…Unverified` |
| `MockAppSeedTests.defaultAppMockReseedsIncompatibleFiles(payload:)` (l.151) | 3: `{}`, `corrupt`, `legacy` | `…EmptyObject`, `…Corrupt`, `…Legacy` |
| `MockRebaseTests.preservesWallTimesAcrossAmsterdamAndNewYorkDST(nowText:)` (l.9) | 3 | `…20261025`, `…20261101`, `…20261107` |
| `MockSeedRefreshTests.walkthroughOverrideCoversBothSeededAccounts(seen:)` (l.9) | 2 | `…NotSeen`, `…Seen` |
| `MockSeedRefreshTests.restoresTenDayOldSeedWithoutLosingUserState(legacyAnchor:)` (l.18) | 2 | `…CurrentAnchor`, `…LegacyAnchor` |
| `MockTripGenerationTests.unknownAirportIsAFieldErrorBeforePlanGeneration(destination:)` (l.8) | 2 | `…Origin`, `…Destination` |
| `MockGuestReviewTests.missingWrongOrForeignPossessionExpires(kind:)` (l.34) | 6: `missing`, `wrong`, `token-as-secret`, `foreign-device`, `revision`, `review-revision` | `MockGuestPossessionTests.missingWrongOrForeignPossessionExpires` (Parameterized) |
| `MockGuestReviewTests.freshReviewReusesOnlyAKnownDeviceSecret(kind:)` (l.50) | 3 | `…Known`, `…Unknown`, `…ForeignDevice` |
| `MockGuestReviewTests.guestSleepIsRequired(field:)` (l.67) | 2 | `…HabitualBed`, `…HabitualWake` |
| `MockGuestReviewTests.exclusionGivesNoPlanOrPrompts(field:)` (l.102) | 2 | `…AgeBand`, `…ShiftWork` |
| `MockGuestRegistrationClaimTests.staleClaimsCreateNothing(kind:)` (l.54) | 10: `input-revision`, `review-revision`, `wrong-secret`, `missing-secret`, `foreign-device`, `unknown-token`, `missing-token`, `missing-review-revision`, `missing-input-revision`, `expired` | `MockStaleClaimTests.staleClaimsCreateNothing` (Parameterized) |
| `MockGuestRegistrationClaimTests.retryMustProveAccountAndPossession(kind:)` (l.137) | 3 | `…Password`, `…Secret`, `…TwoFactor` |
| `MockGuestRegistrationClaimTests.loginReplaysOnlyLiveMatchingReceipt(kind:)` (l.171) | 6: `matching`, `no-claim`, `wrong-secret`, `missing-secret`, `other-account`, `expired` | `MockClaimReplayTests.loginReplaysOnlyLiveMatchingReceipt` (Parameterized) |
| `DataSourceParityTests.invalidSettingsReturnEveryField(section:)` (l.8) | 3 | `…Reminders`, `…Preferences`, `…BodyClock` |
| `PlanDecodingTests.mockTripsRebaseEveryTimestampAndDate(nowText:)` (l.168) | 4 | `…20000229`, `…20260329`, `…20261025`, `…20350715` |

Every other Swift test in §2.2 is one plain Kotlin method with the Swift name, in the Kotlin class of the Swift suite name (`LargeTimelineFixtureTests.defaultMockHasNoPerformanceTrips` goes to `MockAppSeedTests`, KDoc naming its iOS origin, because 1b's `LargeTimelineFixtureTests` lives in `:core:plan`). Shared test helpers: `MockScenarios.kt` (Task 6), `MockPasskeyFixtures.kt` (Task 7), `MockWizardFixtures.kt` and `MockGuestFixtures.kt` (Task 9), `MockMethodCalls.kt` and `support/MockApi.kt` (Task 10).
