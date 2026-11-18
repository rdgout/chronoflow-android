# Phase 4b brief: API client and error mapping

- **Branch:** `feat/phase-4b-api-client`, already created from the Phase 4a stack top. Work in the main checkout, one phase at a time; the former wave-4 parallel-worktree scheduling no longer applies.
- **Depends on:** 0d (contracts, `ApiJson`, `DataFailure`, fixtures), 1a (`CheckOffChange` in `net.chronoflow.core.model`). **Stack:** 0a–0e, 1a–1e, 2, 3a–3c and 4a have landed. **Unblocks:** 4c (mock data source), and through it 4d and 4e.
- **Plan entry:** `plan.md` "### Phase 4b: API client and error mapping (`feat/phase-4b-api-client`)" (l.1702–1745). Decisions **A3** (modules), **A6** (networking and data-source selection), **A23** (security), **A24** (scope), OQ-A16 (no ETags). "Android spec: build and repository layout" (BuildConfig in `:app` only).
- **Brief written:** 2026-11-16 by the orchestrator; revised the same day against the final 0a, 0d and 0e briefs (JUnit 4 table rules, `make targeted` syntax, convention-plugin test dependencies, perf variants) and re-verified against the iOS sources at the pin. iOS read at `f391a1fa` (`tools/ios-pin.txt`), web at `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19` (`tools/web-pin.txt`). Checked the same day against the iOS sources and tests, the web routes and the 0a/0d/0e briefs (adversarial pass): line references, the wizard wire test, the mid-body `ProtocolException`, the device-id header (OQ-4b-15) and the `args` rule corrected.
- **Refreshed 2026-11-18:** checked landed contracts, fixtures, `CheckOffChange`, Gradle aliases/plugins, perf build types and test harness against the Phase 4a tree. `TripDetailResponse` exposes `detail.trip`, not `trip`; preserve its legacy-health sanitizer. New OQ rows start at OQ-A132.
- **Tests:** every test follows `AGENTS.md` "Tests". This phase is **JVM only**: JUnit 4.13.2 and assertk 0.28.1 (both already added by 0a's `chronoflow.jvm.library` plugin), plus kotlinx-coroutines-test 1.11.0 (already available in `:core:model`) and mockwebserver3 5.5.1, and two plain-JVM `:app` unit tests (`testDebugUnitTest`, `testReleaseUnitTest`). No Robolectric, no emulator, no UI, no tier annotation (plain JVM tests carry none, 0e).
- **Table-driven tests (0d §4.2, binding here too):** a class annotated `@RunWith(Parameterized::class)` holds **only** parameterized tests, with `@Parameters(name = "{0}")` and a readable first column. A short fixed list (0d §4.2 sets no number; here: ≤ 5 rows, unless the task names a **Parameterized** class for it, as for the 4 transport modes) is written as one plain method per row, each a one-line call to a private helper, with the Swift name as the prefix (`deadlinesCancelUnderlyingTask3s`). Never loop over rows inside one `@Test`; never use `Enclosed`. Every class name below says which kind it is.
- **Boards:** none. This phase has no UI. Do not add a screen, a string resource, a `CopyIndex` entry or a design token. Server message keys travel as `FieldMessage.key` and are translated by later UI phases.
- **Tags:** **[F]** fact verified in source (path and line at the pin). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>` (never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `KitTests/` = `I/Packages/ChronoFlowKit/Tests/DataAccessTests`. `W/` = `/Users/rickgout/Sites/chronoflow-new`, read with `git -C … show e36368ec:<path>`.
- **Paths:** `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`; `…/network/` = `core/network/src/main/kotlin/net/chronoflow/core/network/`; tests mirror them under `src/test/kotlin/`.

---

## 1. Goal

Give the app one `DataSource` contract and its production implementation, `ApiDataSource`, behaving exactly like iOS `APIDataSource` + `APIClient` at `f391a1fa`:

1. **`DataSource`** (`:core:model`): the 25 iOS methods as `suspend` functions, plus the in-memory types its signatures need (`OutboxChange`, `ProfileChange`, `SettingsSection`, `MutationReceipt`, `AccountSecurityChange`, `AccountSecurityResult`; `CheckOffChange` is 1a's and is used as it is). Expected failures are thrown as `DataFailure` (0d), never as a raw `IOException` or `SerializationException`.
2. **Pure helpers ported 1:1** (`:core:model`, shared with 4c, 5d and 9b): `TripWizardErrors` (error-path rewriting and domain failures) and `SettingsPatchValidation`.
3. **`ApiClient`** (`:core:network`, OkHttp 5.5.1): no redirects, no cookies, no cache, no silent retries, sanitized injected client; `Accept: application/json` on every call, exact JSON `Content-Type` on bodies, bearer only on authenticated endpoints (a missing bearer fails before sending); per-call deadlines **15 s / 90 s / 3 s** that cancel the underlying call and become `Offline`; explicit cancellation stays cancellation; origin check (plan A6 and RN; iOS has none, so it is an Android addition [R]); exact expected-status checks; decoding off the caller's thread; no device-id header (OQ-4b-15).
4. **`ApiErrorMapper`**: the iOS `mapError` table (envelope, status codes, field errors with arguments, the three 2FA conflict sentences, throttling with retry-after from body, header seconds or HTTP-date, default 60 s).
5. **`ApiDataSource`**: all 25 methods (40 HTTP routes) with the iOS paths, verbs, bodies, timeouts, expected statuses and post-decode validation.
6. **Selection** (`:core:network` + `:app`): `ApiBaseUrl` validation (HTTPS-only for release), `DataSourceFactory` (`mock|api`, no silent fallback), and the `:app` `BuildConfig.DATA_SOURCE` / `API_BASE_URL` fields: debug defaults to mock and can be overridden by Gradle properties; `release` is fixed to `api` at `https://api.chronoflow.net/v1`; the perf build types are fixed to `mock` (OQ-4b-13).
7. **Proof**: deny-by-default MockWebServer suites pin every route's method, path, query, headers, body and decoding; the ported iOS `DataAccessTests` API cases pass with their Swift names; all 13 `errors/*.json` fixtures map to one expected `DataFailure`.

No mock (4c), no database, outbox persistence or sync (4a, 4d), no session wiring or `AppGraph` (4e), no UI.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` l.1702–1745 (Phase 4b), A3, A6, A23, A24, "Android spec: build and repository layout" (BuildConfig), "Android spec: seeds and test hooks" (release readiness), OQ-A16 | Scope and decisions |
| Agent rules | `AGENTS.md` "Working rules" (Secrets, Comments), "Tests", "Commands", "Git", "Traps" (rethrow `CancellationException`; configuration cache; release ships no mock) | How to work |
| 0d brief | `docs/phases/phase-0d-contracts.md` §4 (packages, serialization rules 4.1), Task 3 (`ApiErrorEnvelope`, `FieldMessage`, `DataFailure`), Tasks 5–7 (DTO names and wire keys), §9 "Hand-off to later phases" (4b items) | The types you call; do not change them unless a task below says so |
| Research | `docs/research/03-api-contract.md` §2–§13 (transport, envelope, rate limits, endpoint table §5, tokens, 2FA, links, passkeys, guest, wizard, trips, settings) | Wire contract |
| Research | `docs/research/02-ios-architecture.md` §5.1–§5.3 (DataSource, factory, APIClient, endpoint table) | iOS structure |
| Research | `docs/research/09-rn-android-lessons.md` §7 (RN client: `redirect: error`, origin check, timeouts) | Lessons |
| 0a output | `gradle/libs.versions.toml` (`okhttp`, `okhttp-mockwebserver3` 5.5.1; `kotlinx-coroutines-core`/`-test` 1.11.0; use the aliases exactly as 0a wrote them), `core/network/build.gradle.kts` (plugin `chronoflow.jvm.library`), `app/build.gradle.kts`, `konsist/src/test/kotlin/net/chronoflow/konsist/{KonsistRules,KonsistRulesTest}.kt` and `konsist/src/test/resources/violations/` (rule + planted-violation convention), `Scripts/targeted.py`, `Makefile` | Where you wire things |
| 0e output | `docs/tooling.md` "perf variants" (OQ-A28: the build-type names actually used, `benchmarkRelease`/`nonMinifiedRelease` or the fallback `benchmark`), `app/src/testRelease/…/ReleaseReadinessTest.kt` | Task 10 needs the perf build-type names |
| 0d output | `…/model/DataFailure.kt`, `…/model/OutboxKind.kt`, `…/model/api/*.kt`, `…/model/contracts/*.kt`, `core/testing/src/main/kotlin/net/chronoflow/core/testing/Fixtures.kt`, `core/testing/src/main/resources/fixtures/**` | Inputs |

### 2.2 iOS (read-only, at `f391a1fa`): port behaviour from these files

| iOS file | Lines | Port to |
|---|---|---|
| `Kit/AppCore/DataSource.swift` | 1–39 | `…/model/DataSource.kt` |
| `Kit/AppCore/OutboxChange.swift` | 1–39, 49–63 (`OutboxKind` l.41–48 is already in 0d) | `…/model/OutboxChange.kt` |
| `Kit/AppCore/Contracts/AccountSecurityContracts.swift` | 17–39 (the two DTOs l.1–15 are already in 0d) | `…/model/api/AccountSecurityContracts.kt` (append) |
| `Kit/AppCore/DataFailure.swift` | all (already ported in 0d; read for `status`) | — |
| `Kit/DataAccess/TripWizardErrors.swift` | 1–46 | `…/model/api/TripWizardErrors.kt` |
| `Kit/DataAccess/SettingsPatchValidation.swift` | 1–23 | `…/model/api/SettingsPatchValidation.kt` |
| `Kit/DataAccess/DataSourceFactory.swift` | 1–38 | `…/network/ApiBaseUrl.kt`, `…/network/DataSourceFactory.kt` |
| `Kit/DataAccess/API/APIEndpoint.swift` | 1–45 | `…/network/ApiEndpoint.kt` |
| `Kit/DataAccess/API/APIRedirectDelegate.swift` | 1–10 | `followRedirects(false)` in `ApiClient` |
| `Kit/DataAccess/API/APIClient.swift` | 1–180 (`mapError` 118–168, `retryAfter` 170–178) | `…/network/ApiClient.kt`, `…/network/ApiErrorMapper.kt` |
| `Kit/DataAccess/API/APIDataSource.swift` | 1–135 | `…/network/ApiDataSource.kt` |
| `Kit/DataAccess/API/APIDataSource+AccountSecurity.swift` | 1–63 | same file (or `ApiDataSourceAccountSecurity.kt`) |
| `Kit/DataAccess/API/APIDataSource+TripWizard.swift` | 1–75 | same file (or `ApiDataSourceTripWizard.kt`) |
| `KitTests/{APIClientTests,APIDataSourceTests,AccountSecurityAPITests,PasskeyManagementAPITests,SettingsDeleteAPITests,TripWizardAPITests,GuestTripReviewAPITests,GuestClaimAuthTests,NoHealthDataTests,DataSourceSelectionTests,PlanDecodingTests}.swift`, `StubProtocol.swift` (the deny-by-default stub) | | Appendix B |

iOS `plan.md` sections (at the pin): l.25 "Decisions" (**D20**: one `DataSource`, mock/api factory, mock by default in debug, base URL default `https://api.chronoflow.net/v1`, release rejects non-HTTPS, **no silent fallback**); l.435 "### API contract and offline sync" (transport rules, the operation → endpoint table; **"Cancel the underlying task on timeout/cancellation; timeout/transport failure → offline, explicit task cancellation remains cancellation"**; **"Never log passwords, tokens, recovery codes, signed links or response bodies with secrets"**); l.1082 "#### Phase 9b: Data-source contract, mock and API" (its tasks 1–2 and review fix 1 (redirects) are this phase; task 3 and review fixes 3–5 are mock work for 4c (fix 3's API half is pinned here by the `wrong_credentials` fixture row); review fix 2 is iOS tooling only). l.449 (Verify row): "status adapter adds no invented resend delay".

### 2.3 Web (read-only, at `e36368ec`)

- `W/routes/api.php` l.42–103 (the `v1` group; the file ends at l.103): every path below exists at the pin, except the four `account/passkeys*` routes, which are on the unmerged branch `feat/mobile-api-passkey-management` `1f4a2684` (`routes/api.php` l.87–92, 109). iOS builds them ahead of the server (iOS `plan.md` l.2055, "Passkey management addendum", 2026-11-12); so do we. No controller or middleware reads a device-id header (`git grep -inE "x-device|header\('device|->header\(.*device"` over `app/`, `routes/` and `config/` at the pin finds nothing); `device_id` is a body key of rows 2, 3 and 5.
- `W/app/Http/Responses/Api/ApiExceptionResponse.php`: status → `code` table (`03 §3`).

### 2.4 Research facts this brief relies on

- `03 §2` [F]: "Send `Accept: application/json` … `Content-Type: application/json` with every body, including the `DELETE` bodies … `Authorization: Bearer <token>` on authenticated routes … **No `Accept-Language`**."
- `03 §3` [F]: "A 429 always carries `retryAfterSeconds` **and** a matching `Retry-After` header. The server falls back to 60." Status table: 401 `unauthenticated`, 403 `email_unverified`/`forbidden`, 404 `not_found`, 405 `method_not_allowed`, 409 `conflict`, 413 `payload_too_large`, 422 `validation_failed`, 429 `throttled`, 500 `server_error`, 503 `unavailable`, other ≥ 400 `http_error`.
- `03 §3` [F]: 2FA 409 sentences: `Two-factor authentication is already on.`, `Two-factor authentication is off.`, `Start two-factor setup again.`
- `09 §7` [F]: RN uses `redirect: 'error'` and rejects "a response from another origin" (`chronoflow-mobile/src/data/api/client.ts:108` `redirect: 'error'`; l.115–116: `if (response.url && !response.url.startsWith(`${this.baseUrl}/`)) throw new ServerError(response.status, serverMessage);`).
- `02 §5.3` [R]: "Do not use Retrofit. A small hand-written client keeps per-call timeouts, error mapping and the raw bytes the detail store needs."

---

## 3. Out of scope (owner in brackets)

- `MockDataSource`, `MockServerState`, `MockAppSeed`, `failNext`, latency, rate windows, the mock halves of `AccountSecurityAPITests`, `SettingsDeleteAPITests` and all of `DataSourceParityTests` [4c].
- `OutboxChange` serialization for storage, the outbox half of `ContractsTests.inputWireKeysAndOutboxRoundTrips`, `SyncEngine`, final-refusal classification, backoff using `retryAfterSeconds` [4d].
- Choosing the data source at runtime, `StorageNamespace` mapping, the bearer provider backed by the Keystore session, `AppDispatchers`, building `ApiClient` in `AppGraph`, the 401 forced wipe [4e]. 4b only provides the constructors and the `BuildConfig` fields.
- `AuthLink` parsing (iOS uses it in `nativeLinkInputsReach…`; here the test uses the literal parsed values) [5c]. Sign-in models and the model half of `GuestClaimAuthTests` [5a, 5d]. Passkey ceremonies, `PasskeyCapability` [5b, 10d].
- ETags / `If-None-Match` (OQ-A16: no), HTTP disk cache (A6: no), background work, certificate pinning (A24: no), a logging interceptor (never).
- Any string resource, UI or board.

---

## 4. Module and package layout

- **`:core:model`** (`chronoflow.jvm.library`, set up in 0d). New files: `DataSource.kt`, `OutboxChange.kt` (package `net.chronoflow.core.model`); `api/TripWizardErrors.kt`, `api/SettingsPatchValidation.kt`; append to `api/AccountSecurityContracts.kt`. No new dependency: serialization JSON and coroutines core are already `api` dependencies, and coroutines-test and `:core:testing` are already test dependencies.
- **`:core:network`** (`chronoflow.jvm.library`, plugin-only skeleton from 0a; package `net.chronoflow.core.network`). `build.gradle.kts`:
  ```kotlin
  dependencies {
      api(project(":core:model"))
      api(libs.okhttp)                       // ApiClient's constructor takes an OkHttpClient
      implementation(libs.kotlinx.coroutines.core)
      testImplementation(project(":core:testing"))
      testImplementation(libs.okhttp.mockwebserver3)
      testImplementation(libs.kotlinx.coroutines.test)
      // junit4 and assertk come from the chronoflow.jvm.library plugin; do not add them again
  }
  ```
  These aliases match the current `libs.versions.toml`. No serialization compiler plugin (this module declares no `@Serializable` type). No `android.*`, no Compose (A3 Konsist rule 2 already covers `core/network`). No `kotlin-reflect` (not in the catalog).
- **`:app`**: `BuildConfig.DATA_SOURCE`, `BuildConfig.API_BASE_URL` (Task 10) and plain JVM configuration tests. `implementation(project(":core:network"))` already exists from the scaffold; keep it as landed and do not add a duplicate test dependency.
- **Why the helpers go to `:core:model` [R, OQ-4b-1]:** `TripWizardErrors` and `SettingsPatchValidation` are used by the mock (4c, `:core:data-mock`), sign-up (5d) and the wizard (9b, `:core:wizard`). In `:core:model` they need no OkHttp.
- Ported code keeps the **Swift type, method and case names** (`passkeyOptions`, `fetchTripDetail`, `AccountSecurityChange.EnableTwoFactor`, `MutationReceipt.Acknowledged`), so a reviewer can diff against Swift. Swift enum cases become PascalCase (`checkOff` → `CheckOff`).

---

## 5. Tasks

Each task is one commit, in this order. Run `make targeted ONLY=…` for each task; run `make quick` once at the end (user instruction). Tick the named `plan.md` checkbox in the commit that completes it.

### Task 1. The DataSource contract and its in-memory types

Commit: `feat(model): add the data source contract and outbox change types`. Ticks plan.md 4b "**DataSource interface**".

**Files**

`…/model/OutboxChange.kt` (port of `OutboxChange.swift`; `CheckOffChange` is 1a's, in the same package `net.chronoflow.core.model` (`…/model/CheckOffChange.kt`): use it, never declare it here; the storage serialization of `OutboxChange` **and of `CheckOffChange`** is 4d's, OQ-4b-5):
```kotlin
@Serializable
data class ProfileChange(val name: String, val email: String)   // PATCH users/me body, exactly {name, email}

enum class SettingsSection(val raw: String) { BodyClock("bodyClock"), Preferences("preferences"), Reminders("reminders") }

sealed interface OutboxChange {
    data class CheckOff(val change: CheckOffChange) : OutboxChange
    data class Settings(val section: SettingsSection, val patch: JsonObject) : OutboxChange
    data class Policies(val version: String) : OutboxChange
    data class Profile(val change: ProfileChange) : OutboxChange
    data object Walkthrough : OutboxChange
    data class Unsupported(val kind: OutboxKind, val payload: JsonObject) : OutboxChange
}

sealed interface MutationReceipt {
    data object Acknowledged : MutationReceipt
    data class Account(val snapshot: AccountSnapshot) : MutationReceipt
    data class Identity(val identity: AccountIdentity) : MutationReceipt
    data class Checks(val result: TripCheckResult) : MutationReceipt
}
```

Append to `…/model/api/AccountSecurityContracts.kt` (port of l.17–39):
```kotlin
sealed interface AccountSecurityChange {
    data class RequestPasswordReset(val email: String) : AccountSecurityChange
    data class Password(val current: String, val password: String, val confirmation: String) : AccountSecurityChange
    data class EnableTwoFactor(val current: String) : AccountSecurityChange
    data class ConfirmTwoFactor(val code: String) : AccountSecurityChange
    data class DisableTwoFactor(val current: String) : AccountSecurityChange
    data class RevealRecoveryCodes(val current: String) : AccountSecurityChange
    data class RegenerateRecoveryCodes(val current: String) : AccountSecurityChange
    data object ListPasskeys : AccountSecurityChange
    data class PasskeyCreationOptions(val current: String) : AccountSecurityChange
    data class AddPasskey(val input: ApiPasskeyRegistrationInput) : AccountSecurityChange
    data class RemovePasskey(val id: Long, val current: String) : AccountSecurityChange   // Long: route allows 18 digits (0d OQ-0d-1)
}

sealed interface AccountSecurityResult {
    data object Changed : AccountSecurityResult
    data class Setup(val setup: ApiTwoFactorSetup) : AccountSecurityResult
    data class Codes(val codes: ApiRecoveryCodes) : AccountSecurityResult
    data class Snapshot(val snapshot: AccountSnapshot) : AccountSecurityResult
    data class Passkeys(val passkeys: List<ApiPasskey>) : AccountSecurityResult
    data class PasskeyCreation(val challenge: ApiPasskeyRegistrationChallenge) : AccountSecurityResult
    data class PasskeyAdded(val passkey: ApiPasskey) : AccountSecurityResult
}
```
**Secrets in `toString` (0d OQ-0d-3):** every `AccountSecurityChange` case that holds `current`, `password`, `confirmation` or `code` overrides `toString()` to print `***` for those properties. `email` and `id` print normally.

`…/model/DataSource.kt` (port of `DataSource.swift`; same order, same names):
```kotlin
interface DataSource {
    suspend fun passkeyOptions(): ApiPasskeyChallenge
    suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult
    suspend fun signIn(input: PasswordSignInInput): SignInResult
    suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult
    suspend fun signUp(input: RegisterInput): SessionResult
    suspend fun requestPasswordReset(email: String)
    suspend fun resetPassword(input: ResetPasswordInput)
    suspend fun verificationStatus(): EmailVerificationStatus
    suspend fun resendVerificationEmail(): EmailVerificationStatus
    suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus
    suspend fun signOut()
    suspend fun deleteAccount(password: String): Unit = throw DataFailure.Unsupported(OutboxKind.Security)
    suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult =
        throw DataFailure.Unsupported(OutboxKind.Security)
    suspend fun fetchIdentity(): AccountIdentity
    suspend fun fetchAccount(): AccountSnapshot
    suspend fun fetchTrips(): List<TripListItem>
    suspend fun fetchTripDetail(id: String): TripDetailResponse
    suspend fun searchAirports(query: String, limit: Int?): List<ApiAirportSuggestion>
    suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview
    suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview =
        throw DataFailure.Unsupported(OutboxKind.Profile)
    suspend fun reviewTripEdit(input: ApiTripReviewInput, id: String, sourceRevision: String): ApiTripReview
    suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult
    suspend fun updateTrip(id: String, identity: ApiTripSaveInput, sourceRevision: String): ApiTripSaveResult
    suspend fun tripInput(id: String): ApiTripInputSnapshot
    suspend fun send(change: OutboxChange): MutationReceipt
}
```
That is exactly 25 functions. Use the 0d type names as they exist in the repo (`AccountIdentity` = `SessionAccount`, `TripListItem`, `ApiAirportSuggestion`, …); if a name differs, use 0d's and note it in the commit body.

**Tests** (`core/model/src/test/kotlin/net/chronoflow/core/model/…`)
- `DataSourceContractTests.hasTheTwentyFiveIosMethods` (Android, plain): `DataSource::class.java.declaredMethods` (plain Java reflection; **no kotlin-reflect**), filtered to `!isSynthetic`, names with a `$default` suffix dropped, `distinct()`: exactly the 25 names above.
- `DataSourceContractTests.defaultsThrowUnsupported` (Android, plain): an `object` implementing only the 22 abstract members with `error("unused")`; `deleteAccount("x")` and `accountSecurity(ListPasskeys)` throw `DataFailure.Unsupported(OutboxKind.Security)`; `reviewGuestTrip(…)` throws `Unsupported(OutboxKind.Profile)`. Use `runTest(timeout = 60.seconds)`.
- `OutboxChangeTests.profileChangeIsExactlyNameAndEmail` (Android, plain): `ProfileChange("Test","demo@example.com")` encoded with `ApiJson.request` is exactly `{"name":"Test","email":"demo@example.com"}` (compare as `JsonObject`). No `CheckOffChange` encoding test here (4d).
- Extend 0d's `api/SecretRedactionTest` with every `AccountSecurityChange` case that carries a secret.

### Task 2. TripWizardErrors and SettingsPatchValidation

Commit: `feat(model): port trip wizard error paths and settings patch validation`.

**`…/model/api/TripWizardErrors.kt`** (`object TripWizardErrors`, verbatim port of `TripWizardErrors.swift`):
- `fun map(failure: DataFailure, outboundCount: Int? = null): DataFailure`
  1. Not `DataFailure.Validation` → return `failure` unchanged.
  2. `fields` has key `review_expired` → `DataFailure.ReviewExpired`.
  3. `fields["credits"]?.key == "Not enough credits"` → `NotEnoughCredits`.
  4. `fields["review"]?.key == "Try saving again shortly."` → `SaveCapacity`.
  5. Rewrite every key with `journeyPath(key, outboundCount)` **only when `outboundCount != null`**; on a key collision keep the first entry in the original map's iteration order (Swift `uniquingKeysWith: { first, _ in first }`).
  6. Any **original** key equal to `profile` or starting with `profile.` → `ProfileInvalid(rewrittenFields)`; else `Validation(rewrittenFields, status)` with the original status.
- `fun journeyPath(path: String, outboundCount: Int): String`
  - `parts = path.split('.').filter { it.isNotEmpty() }` (**Swift `split` drops empty pieces; Kotlin's keeps them**).
  - If `parts.size >= 3`, `parts[0]` is `outbound` or `return`, `parts[1] == "legs"`, and `parts[2].toIntOrNull()` is an `index`: `flat = index + (if (parts[0] == "return") outboundCount else 0)`; `stay = parts.size > 3 && parts[3] == "stay"`; result = `[if (stay) "stays" else "legs", flat.toString()] + parts.drop(if (stay) 4 else 3).map(::camel)` joined by `.`.
  - Else if `path == "override.use"` → `override.enabled`.
  - Else, with `top = mapOf("trip_end_kind" to "end.kind", "trip_end_nights" to "end.nights", "trip_end_check_out_date" to "end.checkOutDate", "preparation_days" to "preparation.days", "latest_wake" to "preparation.latestWake", "pre_departure_busy_answered" to "preparation.busyAnswered", "pre_departure_busy_intervals" to "preparation.busyIntervals", "feasible_night_bed" to "preparation.feasibleNightBed", "feasible_night_wake" to "preparation.feasibleNightWake", "fast_delay_opt_in" to "preparation.fastDelayOptIn")`: if `parts` is empty return `path`; else `([top[parts[0]] ?: parts[0]] + parts.drop(1).map(::camel)).joinToString(".")`.
- `fun camel(key: String): String` (public; 4c uses it): `parts = key.split("_")` (empty pieces **kept**, as Swift `omittingEmptySubsequences: false`); `parts[0] + parts.drop(1).joinToString("") { it.take(1).uppercase() + it.drop(1) }`.

**`…/model/api/SettingsPatchValidation.kt`** (`object SettingsPatchValidation`, port of `SettingsPatchValidation.swift`):
- `fun validate(section: SettingsSection, patch: JsonObject)`, throwing `DataFailure.Validation(…, 422)`:
  - `allowed`: Preferences → `time_format`, `hidden_categories`, `show_optional_items`; Reminders → `light`, `avoid`, `caffeine`, `winddown`, `lead_min`; BodyClock → `ProfilePreferences.requestFields` (0d; use the landed set).
  - First, for `key` in `["exclude_caffeine", "flight_movement_breaks"]` in that order: if `key in patch` and the value is **not a JSON boolean** → throw `Validation(mapOf(key to FieldMessage("Choose one of the available options.")), 422)`. A JSON boolean is `value is JsonPrimitive && !value.isString && value.booleanOrNull != null`. **`JsonPrimitive("true").booleanOrNull` is `true` and `JsonNull` is a `JsonPrimitive`**: both must fail.
  - Then: `patch.isEmpty()` or any key not in `allowed` → throw `Validation(mapOf("form" to FieldMessage("This setting cannot be changed here.")), 422)`.

**Tests**
- `api/TripWizardJourneyPathTests` (Android, **Parameterized**, `core/model/src/test`):
  - `journeyPathRewritesLegsStaysAndTopLevelKeys` (one row each; outboundCount 1; label = the input, `"<empty>"` for `""`):

    | input | output |
    |---|---|
    | `outbound.legs.0.stay.bed_available_from_local` | `stays.0.bedAvailableFromLocal` |
    | `return.legs.0.arrive_local` | `legs.1.arriveLocal` |
    | `outbound.legs.0.surprise` | `legs.0.surprise` |
    | `trip_end_nights` | `end.nights` |
    | `trip_end_check_out_date` | `end.checkOutDate` |
    | `preparation_days` | `preparation.days` |
    | `pre_departure_busy_intervals.2.start_local` | `preparation.busyIntervals.2.startLocal` |
    | `override.use` | `override.enabled` |
    | `override.habitual_bed` | `override.habitualBed` |
    | `profile.habitual_bed` | `profile.habitualBed` |
    | `return.legs.x.arrive_local` | `return.legs.x.arriveLocal` |
    | `outbound..legs.0` | `legs.0` (empty piece dropped) |
    | `""` | `""` |
- `api/TripWizardErrorsTests` (Android, plain):
  - `camelKeepsEmptyPieces`: `a__b` → `aB`; `_a` → `A`; `abc` → `abc` (three assertions, written out).
  - `mapReturnsDomainFailuresFirst`: `review_expired` (with other fields) → `ReviewExpired`; `credits: "Not enough credits"` → `NotEnoughCredits`; `credits: "other"` stays `Validation`; `review: "Try saving again shortly."` → `SaveCapacity`; `Offline` and `Server(500)` pass through unchanged.
  - `mapRewritesOnlyWithAnOutboundCount`: the `wizard/profile.json` fields without a count → `ProfileInvalid` keyed `profile`, `profile.age_band`, `profile.habitual_bed`; with count 1 → `profile`, `profile.ageBand`, `profile.habitualBed`; `wizard/journey.json` with count 1 → `Validation` with exactly `stays.0.bedAvailableFromLocal`, `legs.1.arriveLocal`, `end.nights`, status 422.
- `api/SettingsPatchValidationTests` (Android): each section accepts one allowed key; `{}` and an unknown key throw the `form` failure; `lead_min` in BodyClock throws `form`.
- `contracts/NoHealthDataTests.patchAllowsOnlyNeutralPreferencesAndRequiresBooleans` (Swift name, add to 0d's class): `{exclude_caffeine: true, flight_movement_breaks: false}` passes; each of `bipolar`, `eye_disease`, `pregnancy`, `vte`, `sleep_phase_disorder`, `oral_contraception`, `caffeine_ruled_out` with `false` throws; `exclude_caffeine` and `flight_movement_breaks` with `null` throw; Android additions: with `"true"` (a string) throw.

### Task 3. Base URL, data-source factory, endpoint and origin

Commit: `feat(network): add api base url, data source factory and endpoint`.

**`…/network/ApiBaseUrl.kt`** (port of `APIBaseURL`):
```kotlin
class ApiBaseUrl private constructor(val url: HttpUrl) {
    companion object { fun parse(value: String, release: Boolean): ApiBaseUrl }   // throws DataSourceConfigurationFailure.InvalidBaseUrl
}
```
Parse with `java.net.URI(value)` (catch `URISyntaxException` → invalid), then require all of: `scheme` is `http` or `https` (exact, lower case); `!release || scheme == "https"`; `host` non-null and non-empty; `port == -1 || port in 1..65535`; `rawUserInfo == null`; `rawQuery == null`; `rawFragment == null`; no segment of the **decoded** `uri.path` (split on `/`, empty pieces dropped) equal to `.` or `..` (Swift checks the decoded `c.path`, so `%2E%2E` is refused too); then `value.toHttpUrlOrNull()` is non-null. Store that `HttpUrl`. Equality by `url`.

**`…/network/DataSourceFactory.kt`** (port of `DataSourceFactory`):
```kotlin
sealed class DataSourceConfigurationFailure : Exception(null, null, false, false) {
    data object InvalidSource : DataSourceConfigurationFailure()
    data object InvalidBaseUrl : DataSourceConfigurationFailure()
}
class DataSourceFactory(setting: String = "mock", baseUrl: String = "https://api.chronoflow.net/v1", release: Boolean, testing: Boolean) {
    enum class Source(val raw: String) { Mock("mock"), Api("api") }
    val source: Source           // Source matched by exact raw value, else InvalidSource; testing forces Mock
    val baseUrl: ApiBaseUrl      // ApiBaseUrl.parse(baseUrl, release), validated even when the source is Mock
    fun <T> make(mock: () -> T, api: () -> T): T
}
```
The setting is validated **before** the URL (Swift order). `StorageNamespace` (Swift `namespace`) is not ported here; 4e maps `source` to it.

**`…/network/ApiEndpoint.kt`** (port of `APIEndpoint`):
```kotlin
class ApiEndpoint(
    val method: String = "GET",
    val path: List<String>,
    val access: Access = Access.Authenticated,
    val body: ByteArray? = null,
    val timeout: Duration = ApiTimeouts.Default,
    val query: Map<String, String> = emptyMap(),
) {
    enum class Access { PublicRoute, Authenticated }
    fun url(base: ApiBaseUrl): HttpUrl
    override fun toString(): String = "$method /${path.joinToString("/")}"   // never prints the body
}
object ApiTimeouts { val Default = 15.seconds; val Generation = 90.seconds; val Logout = 3.seconds }
```
`url(base)`:
- Every segment must be non-empty and must not be `.` or `..`; otherwise throw `DataFailure.Server(null)` (iOS throws for empty, `APIEndpoint.swift:28-30`; **Android also refuses dot segments [R, OQ-4b-2]** because OkHttp treats `.`, `..`, `%2E` and `%2E%2E` as dot segments and resolves them, so iOS's `/%2E%2E` cannot be sent).
- Percent-encode each segment yourself with **exactly the iOS allowed set** `A–Z a–z 0–9 - _ ~` (`APIEndpoint.swift:26`; `.` is **not** in it, so `a.b` → `a%2Eb`); every other UTF-8 byte becomes `%XX` with upper-case hex. Append with `HttpUrl.Builder.addEncodedPathSegment` (never `addPathSegment`), after the base path (trailing `/` of the base dropped, so `https://h/v1/` and `https://h/v1` both give `/v1/<segments>`).
- Query: entries sorted by key, each added with `addQueryParameter(key, value)` (OkHttp encodes space as `%20` and `+` as `%2B`; verify it in the test).

**`…/network/ApiOrigin.kt`**: `internal fun ApiBaseUrl.owns(url: HttpUrl): Boolean` = same `scheme`, `host` and `port`, and `url.encodedPath` starts with the base `encodedPath` (trailing `/` removed) followed by `/`.

**Tests** (`core/network/src/test/kotlin/net/chronoflow/core/network/`)
- `DataSourceSelectionTests` (Swift name, plain; each Swift test stays one test, its inputs written out as consecutive `assertFailure { … }` lines, no loop):
  - `defaultsAndIsolation`: `DataSourceFactory(release = false, testing = false).source == Mock`; `("api", release = true, testing = true)` → `Mock`; `("api", release = false, testing = false)` → `Api`; `make({ "m" }, { "a" })` returns per source. (The namespace half is 4e.)
  - `rejectInvalidSettings`: settings `""`, `"API"`, `"other"`, `"$(CHRONOFLOW_DATA_SOURCE)"` with `release = false, testing = true` (as Swift: `testing` does not hide an invalid setting) → `InvalidSource`; URLs `/v1`, `https://`, `file:///v1`, `https://u:p@api.test/v1`, `https://api.test/v1?q=x`, `https://api.test/v1#x`, `https://api.test/../v1` → `InvalidBaseUrl` (release false); `http://localhost/v1` → invalid with release true, valid with release false. Android additions: `https://api.test:0/v1`, `https://api.test:70000/v1` and `https://api.test/%2E%2E/v1` invalid; `HTTPS://api.test/v1` invalid (scheme must be lower case, as `URLComponents` keeps case and Swift compares exactly).
- `ApiEndpointTests` (Android names, plain; port of the path half of `APIDataSourceTests.protectedReadsMutationsAndPathEncoding`):
  - `encodesEverySegment`: base `https://example.invalid/v1/`, path `["trips", "a/b?# %"]` → `https://example.invalid/v1/trips/a%2Fb%3F%23%20%25`; `["trips", "a.b", "é"]` → `…/v1/trips/a%2Eb/%C3%A9`; `HttpUrl.encodedPath` keeps those escapes as written.
  - `refusesEmptyAndDotSegments`: `""`, `"."`, `".."` → `DataFailure.Server(null)`, and nothing is built.
  - `sortsQueryAndEncodesPlus`: query `{"query": "São Paulo & city+airport", "limit": "7"}` → encoded query `limit=7&query=S%C3%A3o%20Paulo%20%26%20city%2Bairport`; decoding it gives back the original value.
  - `toStringNeverPrintsTheBody`.
- `ApiOriginTests` (Android): owns `/v1/trips`; not `/v2/trips`, not `/v1x/trips`, not another host, port or scheme.

### Task 4. Error envelope → DataFailure

Commit: `feat(network): map the error envelope to data failures`. Ticks plan.md 4b "**ApiErrorMapper**".

**`…/network/ApiErrorMapper.kt`**: `internal object ApiErrorMapper { fun map(status: Int, body: ByteArray, retryAfterHeader: String?, now: Instant): DataFailure }` — a pure port of `APIClient.swift:118-178`, in this order:
1. `envelope` = `ApiJson.response.decodeFromString(ApiErrorEnvelope.serializer(), body.decodeToString())`, or `null` on `IllegalArgumentException` (which covers `SerializationException`).
2. **`status == 429`** (independent of the envelope): `body` hint = parse the body as a `JsonObject` (null on failure) → `["retryAfterSeconds"]` used only when it is a `JsonPrimitive` with `!isString` and a finite `doubleOrNull >= 0`. Else the header: `^[0-9]+(\.[0-9]+)?$` → that many seconds; else `ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)` → `max(0, seconds between now and it)` as `Double` (catch exactly `java.time.format.DateTimeParseException` for "not a date"; no broader catch); else `60.0`. Return `Throttled(retryAfterSeconds, 429)`.
3. `envelope == null` → `Server(status)`.
4. `when (status to envelope.code)`:
   - `401 to "unauthenticated"` → `Unauthorized(401)`
   - `403 to "email_unverified"` → `EmailUnverified(403)`
   - `422 to "review_expired"` → `ReviewExpired`
   - `422 to "validation_failed"` → `errors` must be non-null and non-empty, every key and every value non-empty, and every value inside **every** entry of `args` (also entries whose field is not in `errors`, as Swift `(envelope.args ?? [:]).values.allSatisfy`) a non-null **string or number** (`JsonPrimitive`, not `JsonNull`, and either `isString` or `doubleOrNull != null` with `booleanOrNull == null`); otherwise `Server(422)`. Result: `Validation(errors.mapValues { (field, key) -> FieldMessage(key, args[field].orEmpty().mapValues { it.value.jsonPrimitive }) }, 422)`.
   - `409 to "conflict"` → when `message` is one of `Two-factor authentication is already on.`, `Two-factor authentication is off.`, `Start two-factor setup again.` → `Validation(mapOf("form" to FieldMessage(message)), 409)`; else `Conflict(409)`.
   - anything else → `Server(status)`.

**Tests** (pure JVM, `now = Instant.EPOCH`; the mapper is pure, so this is where the error logic is tested, AGENTS "Tests" 1)
- `ApiErrorFixtureMappingTests` (**Parameterized**, 13 rows, label = fixture name) `everyErrorFixtureMapsToOneFailure` (body = `Fixtures.bytes("errors/<name>.json")`, header null):

  | fixture | status | expected |
  |---|---|---|
  | `conflict` | 409 | `Conflict(409)` |
  | `email_unverified` | 403 | `EmailUnverified(403)` |
  | `forbidden` | 403 | `Server(403)` |
  | `http_error` | 418 | `Server(418)` |
  | `method_not_allowed` | 405 | `Server(405)` |
  | `not_found` | 404 | `Server(404)` |
  | `payload_too_large` | 413 | `Server(413)` |
  | `server_error` | 500 | `Server(500)` |
  | `throttled` | 429 | `Throttled(37.0, 429)` |
  | `unauthenticated` | 401 | `Unauthorized(401)` |
  | `unavailable` | 503 | `Server(503)` |
  | `validation_failed` | 422 | `Validation({"password": FieldMessage("Use no more than :max characters.", {"max": 255})}, 422)` |
  | `wrong_credentials` | 422 | `Validation({"email": FieldMessage("These sign-in details don't match our records.")}, 422)` |
- `ApiErrorMapperMalformedTests` (**Parameterized**, 10 rows) `malformedErrors` (Swift name; status 422 → `Server(422)`): the 6 Swift bodies (`APIClientTests.swift:69-74`) `{}`, `<html>Error</html>`, `{"code":"validation_failed","message":"bad"}`, `…"errors":{}}`, `…"errors":{"a":[]}}`, `…"errors":{"a":"x"},"args":{"a":{"max":null}}}`; Android additions: `"errors":{"":"x"}`, `"errors":{"a":""}`, `"args":{"a":{"max":true}}`, and `"errors":{"a":"x"},"args":{"b":{"max":null}}` (an invalid `args` entry for a field that is not in `errors` still fails).
- `ApiErrorMapperRetryAfterTests` (**Parameterized**, 10 rows; columns: label, body, header, expected) `retryAfter` (Swift name). The 5 Swift rows wrap the member as Swift does, body = `{<member>"code":"throttled","message":"fake"}`: member `"retryAfterSeconds":37,` + header `12` → 37.0; no member + `12` → 12.0; no member + `Thu, 01 Jan 1970 00:02:00 GMT` → 120.0; no member + `bad` → 60.0; member `"retryAfterSeconds":-1,` + `23` → 23.0. Android rows: member `"retryAfterSeconds":"37",` (a string) + `12` → 12.0; body `not json` + `17` → 17.0; body `{"retryAfterSeconds":23}` (no code) + no header → 23.0; empty body + no header → 60.0; no member + `Thu, 01 Jan 1970 00:00:00 GMT` with `now = Instant.ofEpochSecond(120)` → 0.0 (a past date clamps to 0).
- `ApiErrorMapperTests` (plain):
  - `conflictSentencesBecomeFormFields` (port of `AccountSecurityAPITests.backendConflictKeysSurviveErrorMapping` at the mapper level): each of the three sentences in a `{"code":"conflict","message":<sentence>}` body → `Validation({"form": FieldMessage(<sentence>)}, 409)`; `Artificial envelope message.` → `Conflict(409)`.
  - `codeMustMatchStatus` (Android): `unauthenticated` with 403 → `Server(403)`; `validation_failed` with 400 → `Server(400)`; `review_expired` with 422 → `ReviewExpired`; `unauthenticated` with 401 but no decodable envelope (`<html>`) → `Server(401)` (OQ-4b-7).

### Task 5. ApiClient

Commit: `feat(network): add the okhttp api client`. Ticks plan.md 4b "**ApiClient**".

**`…/network/ApiClient.kt`** (port of `APIClient` actor):
```kotlin
fun interface ApiDeadline {
    suspend fun await(timeout: Duration)
    companion object { val Real = ApiDeadline { delay(it) } }
}

class ApiClient(
    private val baseUrl: ApiBaseUrl,
    private val baseClient: () -> OkHttpClient = { OkHttpClient() },
    private val bearer: suspend () -> String? = { null },
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val deadline: ApiDeadline = ApiDeadline.Real,
    private val now: () -> Instant = { Instant.now() },
) {
    internal suspend fun effectiveClient(): OkHttpClient          // built once, lazily, inside withContext(io)
    suspend fun <T> request(endpoint: ApiEndpoint, deserializer: DeserializationStrategy<T>, expectedStatus: Int = 200): T
    suspend fun <T> request(endpoint: ApiEndpoint, expectedStatus: Int = 200, decode: (ByteArray) -> T): T
    suspend fun requestNoContent(endpoint: ApiEndpoint)
}
```
**Effective client** (built on first use, on `io`, never in a constructor: building an `OkHttpClient` loads TLS classes and `AppGraph` stays I/O-free, AGENTS "Performance"): `baseClient().newBuilder()` then
`followRedirects(false)`, `followSslRedirects(false)`, `cookieJar(CookieJar.NO_COOKIES)`, `authenticator(Authenticator.NONE)`, `proxyAuthenticator(Authenticator.NONE)` (iOS clears credential storage; injected authenticators must not retry or attach credentials to public routes), `cache(null)`, `retryOnConnectionFailure(false)` [R, OQ-4b-3], `interceptors().clear()`, `networkInterceptors().clear()`, and `connectTimeout`, `readTimeout`, `writeTimeout`, `callTimeout` all `Duration.ZERO`. **OkHttp's default 10 s read timeout would kill every 90 s generation call**; the deadline below is the only limit. The injected base client is never mutated (`newBuilder` copies). Keep its `dns`, `eventListenerFactory` and `socketFactory` (tests use them).

**One call** (`perform`), in this order:
1. `currentCoroutineContext().ensureActive()`.
2. Build the request: `url = endpoint.url(baseUrl)`; header `Accept: application/json` (set with `header`, exactly once); body = `endpoint.body?.toRequestBody("application/json".toMediaType())` — **a `ByteArray` body, never `String.toRequestBody`, which appends `; charset=utf-8`**; a POST/PUT/PATCH/DELETE without a body sends `ByteArray(0).toRequestBody(null)` (no `Content-Type`, `Content-Length: 0`); GET has no body. No `Accept-Language`, no `If-None-Match`, no `Cookie`, no custom `User-Agent`, no device-id header (OQ-4b-15; `device_id` travels only in the auth bodies, as iOS `APIClient.swift:68-76`).
3. `Authenticated`: `token = bearer()`; null or empty → throw `Unauthorized(401)` **without sending**. A token OkHttp rejects as a header value (`IllegalArgumentException` from `Headers` validation) → `Unauthorized(401)` without sending [R, OQ-4b-4]. Then `Authorization: Bearer <token>`. `PublicRoute` never gets `Authorization`, even when a token exists.
4. Race the call against the deadline, inside `withContext(io)`:
   ```kotlin
   coroutineScope {
       val timedOut = AtomicBoolean(false)
       val work = async { call.await() /* + read body bytes */ }
       val timer = launch { deadline.await(endpoint.timeout); timedOut.set(true); work.cancel() }
       try { work.await() }
       catch (e: CancellationException) { if (timedOut.get()) throw DataFailure.Offline else throw e }
       finally { timer.cancel() }
   }
   ```
   `Call.await()` is a `suspendCancellableCoroutine` around `enqueue` with `invokeOnCancellation { call.cancel() }`; `onResponse` resumes with a close-on-cancel handler; `onFailure` resumes with the `IOException`. Read `response.body.bytes()` inside `work` and close the response (`use`).
   Map exceptions **inside `work`**, so only a `DataFailure` or a `CancellationException` ever leaves it (and `coroutineScope` rethrows a failed child's exception unchanged):
   - a `java.net.ProtocolException` from `call.await()` itself, i.e. before a `Response` exists (malformed status line, OkHttp's "HTTP 204 had non-zero Content-Length") → `DataFailure.Server(null)` (a protocol violation is a server fault) [R, OQ-4b-10];
   - any other `IOException` from `call.await()` (connect, DNS, TLS, reset, EOF before headers) → `DataFailure.Offline`;
   - **any** `IOException` while reading the body → `DataFailure.Offline`, `ProtocolException` included: OkHttp reports a body cut short by a dropped connection as `ProtocolException("unexpected end of stream")`, and iOS maps every transport failure to offline (iOS plan l.438). Mapping every `ProtocolException` to `Server` would turn a mid-body disconnect into a server error;
   - `CancellationException` of the caller → rethrown unchanged (AGENTS trap: never `runCatching`, never `catch (e: Exception)`).
5. Origin [R, A6; iOS has no origin check, its redirect refusal is the equivalent]: `response.priorResponse != null` or `!baseUrl.owns(response.request.url)` → `Server(response.code)`.
6. `code !in 200..299` → throw `ApiErrorMapper.map(code, bytes, response.header("Retry-After"), now())`.
7. `request`: `code != expectedStatus` → `Server(code)`; decode on `io` (`decodeFromString(deserializer, bytes.decodeToString())` with `ApiJson.response`, or the `decode` lambda); `IllegalArgumentException` (includes `SerializationException`) → `Server(code)`. A `DataFailure` thrown by `decode` (post-decode validation) propagates unchanged.
8. `requestNoContent`: `code == 204 && bytes.isEmpty()`, else `Server(code)`.

Never log anything: no `println`, no logging interceptor, no exception messages carrying a body.

**Test support** (`core/network/src/test/kotlin/net/chronoflow/core/network/support/`)
- `MockApi.kt`: a JUnit 4 `ExternalResource` (port of `StubProtocol.swift`'s deny-by-default stub):
  - starts one `mockwebserver3.MockWebServer`; `baseUrl = ApiBaseUrl.parse(server.url("/v1").toString(), release = false)`;
  - `route(method, path) { recorded -> MockResponse }` registers an exact `(method, encodedPath)` match; a request with no route is **recorded as unmatched** and answered `599` with an empty body; `after()` fails the test if any request was unmatched, then closes the server;
  - `client(bearer = { "artificial-old-token" }, deadline = ApiDeadline { awaitCancellation() }, base: () -> OkHttpClient = { OkHttpClient() }, now = { Instant.EPOCH })` builds an `ApiClient` with `io = Dispatchers.IO`. **The default deadline never fires**, so no test depends on wall time; the timeout tests inject their own.
  - helpers `requestJson(recorded): JsonObject?` (null when the body is empty) and `json(name) = Fixtures.text(name)`.
- `RecordingDeadline`: records each `timeout` it is asked for, then `awaitCancellation()`.
- `EventRecorder : EventListener` counting `canceled(call)` (pass it through the base client's `eventListenerFactory`).
- mockwebserver3 5.5.1 API names (`MockResponse.Builder`, `RecordedRequest.url`/`target`/`body`, the socket-effect names for "disconnect after request" and "stall") changed between 4.x and 5.x: **check them in the 5.5.1 sources jar before writing helpers**; do not guess 4.x names.

**Tests** (Swift names; each `runTest(timeout = 60.seconds)`; fixtures from `:core:testing`). Swift's parameterized cases become either their own **Parameterized** class (long lists) or one plain method per row (short lists), per the table rule in the header.
- `ApiClientErrorTests` (**Parameterized**, 10 rows, as Swift `errors(code:status:)`) `errors`: `GET users/me` answered with `errors/<code>.json` and its status (`unauthenticated` 401, `email_unverified` 403, `conflict` 409, `forbidden` 403, `not_found` 404, `method_not_allowed` 405, `payload_too_large` 413, `unavailable` 503, `server_error` 500, `http_error` 418) → `Unauthorized`, `EmailUnverified`, `Conflict`, else `Server`, each with that status; `failure.status == status`.
- `ApiClientRedirectTests` (**Parameterized**, 8 rows: 301, 302, 307, 308 × same-origin / cross-origin) `refusesEveryRedirect`: the response carries `Location: /v1/next` (same origin) or `Location: <second MockWebServer>/v1/next` (cross origin) and `Set-Cookie: secret=fake; Path=/` → `Server(status)`; the first server saw exactly 1 request, carrying `Authorization: Bearer artificial-old-token` and no `Cookie`; the second server saw 0 requests. (Swift's control-session half proved the stub itself followed redirects; OkHttp needs no such control.)
- `ApiClientTransportTests` (**Parameterized**, 4 Android rows replacing Swift's 6 `URLError` codes) `transportFailuresMapToOffline`: server disconnects after reading the request; server disconnects during the response body (a 200 that declares `Content-Length: 100` and sends 10 bytes; OkHttp throws `ProtocolException` while reading the body, which must still be `Offline`); connection refused (a server started and closed, its URL reused); unknown host (base client with `dns { throw UnknownHostException("artificial") }`) → all `Offline`; where a server saw the call, exactly 1 request (no silent retry).
- `ApiClientTests` (plain):
  - `fieldArguments`: 422 + `errors/validation_failed.json` → `Validation({"password": FieldMessage("Use no more than :max characters.", {"max": 255})}, 422)`.
  - `malformedErrorsEndToEnd`: 422 with `<html>Error</html>` → `Server(422)` (the full list is Task 4's `ApiErrorMapperMalformedTests`).
  - `retryAfterEndToEnd`: 429 with `{"code":"throttled","message":"fake"}` and `Retry-After: 12` → `Throttled(12.0, 429)` (proves the header reaches the mapper; the full list is `ApiErrorMapperRetryAfterTests`).
  - `explicitCancellationIsNotAnOfflineFailure`: the route stalls; launch the request, wait until the server has it (the dispatcher completes a `CompletableDeferred`), cancel the job → `CancellationException` (not `Offline`), `EventRecorder.canceled == 1`.
  - `deadlinesCancelUnderlyingTask3s`, `…15s`, `…90s` (Swift `deadlinesCancelUnderlyingTask(seconds:)`; endpoints with those timeouts): the route stalls; the injected deadline asserts it was asked for exactly that duration, waits until the server has the request, then returns → `Offline`, `canceled == 1`.
  - `noContentRequiresExactShape200`, `…201`, `…204WithBody` (Swift `noContentRequiresExactShape`, its 3 rows, `POST auth/logout` through `requestNoContent`), plus the Android row `…204Empty`: `(200, "{}")` → `Server(200)`; `(201, "{}")` → `Server(201)`; `(204, "{}")` → any `DataFailure.Server` (OkHttp may reject a 204 with a body as a protocol error, giving `Server(null)`, OQ-4b-10); `(204, "")` succeeds.
  - `noCookieOrCacheReuseAndMissingBearer`: two `GET users/me` answered with `account/identity.json` plus `Set-Cookie: secret=fake` and `Cache-Control: max-age=3600` → the server saw 2 requests, the second without `Cookie`; `effectiveClient()` has `followRedirects == false`, `followSslRedirects == false`, `cookieJar === CookieJar.NO_COOKIES`, `cache == null`, `retryOnConnectionFailure == false`, empty `interceptors` and `networkInterceptors`, `readTimeoutMillis == 0`, `callTimeoutMillis == 0`; a client with `bearer = { null }` and one with `{ "" }` → `Unauthorized(401)` and **0** requests.
  - `sanitizesInjectedConfiguration`: a base client with `followRedirects(true)`, a storing `CookieJar`, a `Cache` in a `TemporaryFolder`, an interceptor adding `Authorization: Bearer stale` and `Cookie: secret=fake`, `retryOnConnectionFailure(true)` and a 10 s read timeout → the effective client has none of them; the base client still has all of them; a public request carries neither header.
  - `headersAreExact` (Android): an authenticated POST with a body has `Accept: application/json` (one value), `Content-Type: application/json` (no charset), `Authorization`; a bodyless POST has `Content-Length: 0` and no `Content-Type`; a GET has no body; no request ever has `Accept-Language`, `If-None-Match`, `Cookie` or any header whose name contains `device` (case-insensitive), even after a response with an `ETag`.
  - `decodingFailureIsServerWithTheStatus` (Android): 200 with `{"id":1}` decoded as `SessionAccount` → `Server(200)`.
  - `requestStatusMustMatchExpected` (Android): a 201 for an endpoint that expects 200 → `Server(201)`, without decoding.

### Task 6. ApiDataSource: auth, identity, reads and outbox writes

Commit: `feat(network): implement auth, reads and outbox writes in the api data source`.

**`…/network/ApiDataSource.kt`**: final shape `class ApiDataSource(private val client: ApiClient) : DataSource`. During Tasks 6–7 it is abstract so each commit compiles while wizard methods remain for Task 8; test-only subclasses supply unused members. Task 8 makes it concrete and removes those test stubs. Request bodies are encoded with `ApiJson.request` into `ByteArray`; inline bodies are `buildJsonObject { … }`. Rows below are from Appendix A (the full table): implement rows 1–11, 14–17 and 25a–25h here.
- `passkeyOptions()`: decode `ApiPasskeyChallenge`, then require `challengeId.length == 64`, `options.rpId == "app.chronoflow.net"`, `options.timeout == 60_000`, `options.challenge.length == 43`, every char in `[A-Za-z0-9_-]`, and `Base64.getUrlDecoder().decode(challenge).size == 32` (catch `IllegalArgumentException`); otherwise `Server(200)`. Run these checks in the `decode` lambda so they stay off the caller's thread.
- `verificationStatus()` = `fetchIdentity()` mapped to `EmailVerificationStatus(email, hasVerifiedEmail, resendAfterSeconds = 0)` (no invented resend delay, iOS plan l.449; `APIDataSource.swift:47-51`).
- `fetchTrips()`: decode `TripListResponse`; any trip with an empty `card.id` or `card.departureUtc` → `Server(200)`; return `trips`.
- `fetchTripDetail(id)`: `request(endpoint) { bytes -> TripDetailResponse.decode(bytes) }`; a decode failure → `Server(200)`; `detail.detail.trip.id != id` → `Server(200)`. **The exact server bytes are kept** (`response.data`) for clean payloads. Preserve 0d's `TripDetailResponse.decode` legacy-health sanitization, which matches iOS at the pin; never bypass it.
- `send(change)`:
  - `CheckOff(c)` → body `ApiCheckOffInput(c.planInputHash, c.type, c.localDate, c.ordinal, c.status)` (0d), path `trips/<c.tripID>/checks`, `PUT` → `Checks(TripCheckResult)`.
  - `Settings(section, patch)` → `SettingsPatchValidation.validate(section, patch)` **before any I/O**; path `account/body-clock` for BodyClock, else `account/<raw>`; `PATCH`; body = `patch` as is → `Account(AccountSnapshot)`.
  - `Profile(p)` → `PATCH users/me` body `ProfileChange` → `Identity(AccountIdentity)`.
  - `Policies(v)` → `POST terms/acceptance` `{"accepted": true, "version": v}` → 204 → `Acknowledged`.
  - `Walkthrough` → `POST walkthrough`, no body → 204 → `Acknowledged`.
  - `Unsupported(kind, _)` → throw `DataFailure.Unsupported(kind)`, no request.

**Tests** (Swift names where ported; MockApi; `runTest(timeout = 60.seconds)`)
- Added to `ApiClientTests` (plain) in this commit, because they go through `ApiDataSource`:
  - `registrationValidationEnvelopePreservesVisibleFieldMessages`: `signUp(…)` → `POST /v1/auth/register`, deadline asked for 90 s, answered 422 with a `validation_failed` envelope whose three fields (`name`, `email`, `password`) each carry `Please fill this in.` → `Validation` with exactly those three `FieldMessage`s, status 422.
  - `malformedSuccessEmptyObject`, `…Html`, `…SessionWithoutAccount` (Swift `malformedSuccess(body:)`): `signIn(PasswordSignInInput("x","x","x","x"))` answered 200 with `{}`, `<html>OK</html>`, `{"kind":"session","token":"fake"}` → `Server(200)`.
- `ApiDataSourceTests` (plain):
  - `nativeLinkInputsReachExactPublicResetAndAuthenticatedSignedVerificationBodies`: with the values `AuthLink` would produce (5c ports the parser): `resetPassword(token = "a%2Fb", email = "a+b@example.com", password = confirmation = "NewPassword123!")` → public, body exactly those four keys and values, 204; `verifyEmail(id = "0199a3c0-0000-7000-8000-000000000001", hash = "a"×40, expires = 1_800_000_000, signature = "b"×64)` → bearer, body exactly `{"id","hash","expires": 1800000000 (a number),"signature"}`, `auth/verification.json`. 2 requests.
  - `publicAuthRoutesAndSignedVerification`: the ten calls of the Swift test (signIn → `auth/two-factor.json` → `SignInResult.TwoFactor("artificial-two-factor-challenge")`; completeTwoFactor with `Code("123456")` → body `code`, no `recovery_code`; signUp with `language = nl` → body `"language":"nl"`, deadline 90 s; passkeyOptions → no body, no `Content-Type`; passkeyLogin with `rawId = "AA=="` → `credential.rawId == "AA=="`; requestPasswordReset; resetPassword; resendVerificationEmail; verifyEmail; signOut → deadline 3 s, 204): `/v1/auth/*` except `logout` carry no `Authorization`; the others carry the bearer; 10 requests.
  - `protectedReadsMutationsAndPathEncoding` (HTTP half): fetchIdentity, verificationStatus, fetchAccount, fetchTrips (`count == 1`), fetchTripDetail(`0199a3c0-0000-7000-8000-000000000010`), send CheckOff (`tripID = "test"`, `status = null` → body has no `tripID`, has `"status": null`), the three Settings sections (`habitual_bed`, `time_format`, `lead_min` = `"test"`), Profile, Policies(`"1.0"`) → body exactly `{"accepted":true,"version":"1.0"}`, Walkthrough; `Unsupported(Security, {})` → `Unsupported(Security)` with no request. Every request carries the bearer.
  - `passkeyErrorsAndUnverified`: `auth/unverified.json` → `passkeyLogin(…).account.hasVerifiedEmail == false`; 422 `credential` / `challengeId` messages → the matching `Validation`.
  - `rejectsInvalidPasskeyOptionsAndSessionDiscriminator`: `auth/passkey-options.json` with `app.chronoflow.net` → `foreign.invalid`, `60000` → `120000`, the 43 `A`s → 43 `+`, `required` → `preferred` → each `Server(200)` (the last one fails at decode, through 0d's strict `UserVerification` enum, not in the post-decode check); `passkeyLogin` answered with `auth/two-factor.json` → `Server(200)`.
  - `fetchTripDetailKeepsTheExactServerBytes` (HTTP half of `PlanDecodingTests.rawAPIBytesAndUnknownCheckSurviveStorageAndReconciliation`; storage half is 4a/4d): serve `" \n {\"futureEnvelope\":{\"preserve\":true}," + <detail JSON with an extra block of type future_step, lane future_lane> .drop(1) + " \n"`; `response.data` equals those bytes exactly; the extra block decodes with `BlockType.Unknown("future_step")` and lane `Unknown("future_lane")` (use 0d's generated lane type name), and is not in `renderableBlocks` (`PlanDecodingTests.swift:105-118`).
  - `fetchTripDetailRejectsAnotherTrip` and `fetchTripsRejectsEmptyIds` (Android): id mismatch → `Server(200)`; a card with `"id": ""` → `Server(200)`.
- `NoHealthDataTests` (plain; `core/network/src/test/kotlin/net/chronoflow/core/network/NoHealthDataTests.kt`, a second class of the Swift name in another module, so `make targeted ONLY="core:network:NoHealthDataTests"` selects only this one) `patchEncodingSendsOnlyNeutralPreferencesAndRejectsHealthFieldsBeforeTransport` (Swift name): the neutral patch `{exclude_caffeine: true, flight_movement_breaks: false}` → `PATCH /v1/account/body-clock` with exactly that body; each of the six retired keys of the Swift test (`bipolar`, `eye_disease`, `pregnancy`, `vte`, `sleep_phase_disorder`, `oral_contraception`, value `false`; `caffeine_ruled_out` is only in the model-level test) → a `DataFailure.Validation` before transport; the server saw exactly 1 request in total.
- `GuestClaimWireTests` (**Parameterized**, 6 rows: `signIn`/`signUp`/`passkeyLogin` × live/absent) `requestUsesOnlyTheClaimFieldsForItsMethod` (Swift name; the HTTP half of `GuestClaimAuthTests`): `signIn`, `signUp`, `passkeyLogin` with and without claim values (`draft_token = "guest-token"`, `guest_secret = "secret"`, and for register `input_revision = 4`, `review_revision = 7`): no `Authorization`; when live, those keys and values are present; when absent, **the keys are absent** (not `null`); `device_id == "device"`, `device_name == "phone"`; `password` present except for passkey; `password_confirmation` for register.

### Task 7. ApiDataSource: account security, delete and passkey management

Commit: `feat(network): implement account security and passkey management routes`.

Rows 12 and 13a–13k of Appendix A. Bodies are `{"current_password": …}`, `{"code": …}`, `{"password": …}` or the three password keys, built with `buildJsonObject`. `RequestPasswordReset(email)` calls `requestPasswordReset(email)` and returns `Changed`. `AddPasskey` expects **201**; anything else (including 200) → `Server(status)`.

**Tests** (Swift names; all plain classes)
- `AccountSecurityAPITests.everySecurityRouteUsesTheRealMethodAndBody` (bodies compared as string maps, as Swift; DELETE `account/two-factor` answers `account/snapshot.json` → `Snapshot`).
- `AccountSecurityAPITests.backendConflictKeysSurviveErrorMapping` (end to end, through `EnableTwoFactor("password")`).
- `PasskeyManagementAPITests.everyPasskeyRouteUsesThePhase20MethodPathBodyAndStatus`, `storeAnsweringAnythingButCreatedIsAFailure`, `validationAndThrottleErrorsKeepTheirMapping` (inline JSON bodies copied verbatim from the Swift file). The Swift `attestation` relies on Swift's default `type: "public-key"`; 0d does not port that default, so pass the public-key `type` explicitly when building `ApiPasskeyAttestation(id = "AQID", rawId = "AQID", …, response = ApiPasskeyAttestationResponse("Y2Q", "YXR0", listOf("internal")))`. Expected results: list → `Passkeys([ApiPasskey(1, "iPhone", "Apple Passwords", "2026-10-02T10:00:00.000000Z", null)])`; options → `challengeId == "c1"`, `excludeCredentials` ids `["AQID"]`; add → 201 → `PasskeyAdded(ApiPasskey(12, …))`, body has no `current_password`; remove `id = 12` → `DELETE /v1/account/passkeys/12`, 204 → `Changed`; `storeAnswering…` → `Server(200)`; `validationAndThrottle…` → `Validation` with `current_password` (422) and `Throttled(30.0, 429)`.
- `SettingsDeleteAPITests.deletionSendsPasswordUnderTheBackendFieldAndPreservesBillingFailure`.

### Task 8. ApiDataSource: trip wizard and guest review

Commit: `feat(network): implement trip wizard and guest review routes`. Ticks plan.md 4b "**ApiDataSource**".

Rows 18–24 of Appendix A.
- `searchAirports(query, limit)`: public `GET airports`, query `query` always, `limit` only when non-null → `results.airports`.
- `reviewTrip` / `reviewTripEdit`: body = `ApiJson.request.encodeToJsonElement(ApiTripReviewInput.serializer(), input).jsonObject`; for edit, set `"source_revision"` to the parameter (overwriting). `POST`, 90 s. Wrap: `catch (f: DataFailure) { throw TripWizardErrors.map(f, input.input.outbound.legs.size) }`.
- `reviewGuestTrip`: public, 90 s, same mapping with the outbound count.
- `saveTrip` / `updateTrip`: body = encoded `ApiTripSaveInput` (+ `source_revision` for PUT); `POST trips` expects **201**, `PUT trips/{id}` expects **200**; 90 s; an empty `tripId` → `Server(expectedStatus)`; `catch (f: DataFailure) { throw TripWizardErrors.map(f) }` (no outbound count: `profile.habitual_bed` stays snake case).
- `tripInput(id)`: `GET trips/{id}/input`, 15 s.

**Tests** (Swift names; inputs built as Swift does: `wizardInput()` = decode `wizard/input.json`; `ApiTripReviewInput(input = wizardInput(), identity = null, profile = null)`; save identity = `ApiTripSaveInput.from(decode(wizard/review.json).identity)`)
- `TripWizardAPITests` (plain):
  - `wireContractsRequestsAndResponses` (HTTP half; the encoding half is 0d's `TripWizardContractTests`), in the Swift order (`TripWizardAPITests.swift:8-80`):
    1. `reviewTrip(ApiTripReviewInput(input = wizardInput(), identity = null, profile = review-request.json["profile"]))` (**with** the fixture's profile) → `POST /v1/trips/review`, answered 200 with `wizard/review.json`; body == `wizard/review-request.json` as a `JsonObject` (no `input`, `identity`, `draftToken`, `guest_secret` keys; `draft_token` null, `input_revision` 0, `review_revision` null; `outbound.legs[0].from_iata == "AMS"`); the result has `status == Ok`, `canSave`, non-null `summary`.
    2. `saveTrip(ApiTripSaveInput.from(review.identity))` → `POST /v1/trips`, answered **201** with `wizard/save.json`; body keys exactly `draft_token`, `input_revision`, `review_revision`, with `draft_token` equal to `wizard/save-request.json`'s and both revisions `1`; result `tripId == review.identity.token`.
    3. `tripInput("trip")` → `GET /v1/trips/trip/input`, answered with `wizard/snapshot.json`; `.input == wizardInput()` and `input.outbound.legs[0].stay.bedAvailableMode == null`.
    4. `reviewTripEdit(ApiTripReviewInput(input = editInput.input, identity = null, profile = null), id = "trip", sourceRevision = editInput.sourceRevision)` (the snapshot's `sourceRevision` is `"a"×64`) → `POST /v1/trips/trip/review`; body == `review-request.json` minus `profile` plus `source_revision = "a"×64`.
    5. `updateTrip("trip", same identity, sourceRevision = "a"×64)` → `PUT /v1/trips/trip`, answered **200** with `wizard/save.json`; body keys as step 2 plus `source_revision`.
    6. `searchAirports("São Paulo & city+airport", 7)` → public `GET /v1/airports` (no `Authorization`), decoded query `query` and `limit=7` as in Task 3, answered with `wizard/airports.json`; `first().timezone == "Europe/Amsterdam"`.
    Every call but airports carries the bearer; no request has `Cookie`; the four generation calls asked the deadline for 90 s (`RecordingDeadline`); 6 requests.
  - `every429RetainsRetryDeadlineReview`, `…Save`, `…Update` (Swift `every429RetainsRetryDeadline(operation:)`): 429, body `not json`, `Retry-After: 17` → `Throttled(17.0, 429)`; update uses `id = "trip"`, `sourceRevision = "source"`.
  - `bodyRetryHintSurvivesMissingEnvelopeCode`: `reviewTrip` answered 429 `{"retryAfterSeconds":23}`, no header → `Throttled(23.0, 429)`.
  - `saveBodyThrottleAndEmailVerification`: `saveTrip` answered 429 with `wizard/throttled.json` and `Retry-After: 99` → `Throttled(42.0, 429)` (body wins); `reviewTrip` answered 403 with `errors/email_unverified.json` → `EmailUnverified(403)`.
  - `saveMapsDomainValidationCapacity`, `…Expired`, `…Credits`, `…Profile` (Swift `saveMapsDomainValidation(name:)`): `saveTrip` answered 422 with `wizard/<name>.json` → `SaveCapacity`, `ReviewExpired`, `NotEnoughCredits`, and `ProfileInvalid` whose fields contain **`profile.habitual_bed`** (save passes no outbound count, so no camel-casing); 1 request each.
  - `transportAndDeadlineAreOfflineWithoutRetry`: `reviewTrip` against a server that disconnects after reading the request → `Offline`, 1 request; against a stalled route with a deadline that asserts 90 s and returns once the server has the request → `Offline`, 1 request.
- `TripWizardTypedErrorTests` (**Parameterized**, 7 rows, label = fixture stem) `typedErrors` (Swift name): `reviewTrip` answered with `wizard/<stem>.json`, status 422 except `throttled` 429 → `unknown-field` → `Validation({"legs.0.surprise": FieldMessage("This setting cannot be changed here.")}, 422)`; `journey` → `Validation` with `stays.0.bedAvailableFromLocal`, `legs.1.arriveLocal`, `end.nights` present and `preparation.days` absent; `capacity` → `SaveCapacity`; `profile` → `ProfileInvalid` with `profile` and `profile.habitualBed`; `expired` → `ReviewExpired`; `credits` → `NotEnoughCredits`; `throttled` → `Throttled(42.0, 429)`; 1 request each.
- `GuestTripReviewAPITests` (plain). Input as Swift `guestReviewInput()`: `ApiGuestTripReviewInput(input = wizardInput(), identity = null, profile = guest-review-request.json["profile"], deviceID = guest-review-request.json["device_id"], guestSecret = null)`.
  - `publicRouteDecodesMintedOrNullSecretMinted`, `…Null` (Swift `publicRouteDecodesMintedOrNullSecret(minted:)`): the reply is `wizard/review.json` with `identity.guestSecret` set to `"a"×64` or JSON `null`; the request is `POST /v1/guest/trips/review`, no `Authorization`, deadline 90 s, body == `wizard/guest-review-request.json` as a `JsonObject`; the result's `identity.guestSecret` is `"a"×64` or null; 1 request.
  - `mapsGuestFailuresExpired`, `…ValidationExpired`, `…Throttled`, `…Bare429`, `…Profile` (Swift `mapsGuestFailures(kind:)`, every reply with `Retry-After: 17`): 422 `{"code":"review_expired","message":"Review expired."}` → `ReviewExpired`; 422 `wizard/expired.json` → `ReviewExpired`; 429 `wizard/throttled.json` → `Throttled(42.0, 429)`; 429 empty body → `Throttled(17.0, 429)`; 422 `{"code":"validation_failed","message":"Invalid.","errors":{"profile.habitual_bed":"Check your bedtime and wake-up time."}}` → `ProfileInvalid({"profile.habitualBed": FieldMessage("Check your bedtime and wake-up time.")})`; 1 request each.

### Task 9. Deny-by-default endpoint contract table

Commit: `test(network): pin every endpoint with a deny-by-default contract table`. Ticks plan.md 4b "**MockWebServer suite**".

`EndpointContractTest` (Android, **Parameterized** only, label = `"<row #> <METHOD> <path>"`, **one test case per row of Appendix A that sends a request: 40 cases**, plus `Unsupported` and `RequestPasswordReset` as two extra cases = 42). Each row declares: the `DataSource` call, method, encoded path, query, access, deadline, request body (`null` = no body; else the exact `JsonObject`), reply (status + fixture or inline JSON) and a result assertion. Each case asserts, from the single recorded request:
- method and encoded path exactly; query exactly (or none);
- `Accept: application/json`; `Authorization: Bearer artificial-old-token` iff Authenticated; no `Cookie`, `Accept-Language` or `If-None-Match`;
- `Content-Type: application/json` iff a body is expected, and then the body parsed as `JsonObject` **equals** the expected one (not just its keys);
- the `RecordingDeadline` was asked for the row's timeout (15, 90 or 3 s);
- the result: the decoded value (or `Unit`) passes the row's assertion;
- the server saw exactly 1 request (0 for `Unsupported`), and nothing unmatched.

Use inputs built from fixtures where one exists (`wizard/input.json`, `wizard/review.json` identity, `auth/passkey-options.json`), and artificial values elsewhere (`"artificial"`, `"test-device"`, `"Phone"`).

Keep the rows in one `internal val ENDPOINT_ROWS: List<EndpointRow>` (test source set, `EndpointRows.kt`), each row naming its `DataSource` method in a `method: String` field. Add the plain class `EndpointContractCoverageTest.coversEveryDataSourceMethod` (a plain test cannot live in the `Parameterized` class): `ENDPOINT_ROWS.map { it.method }.toSet()` equals the method names of `DataSource` read the same way as `DataSourceContractTests` (`DataSource::class.java.declaredMethods`, non-synthetic, `$default` suffixes dropped; 25 names), so a new method without a row fails. It also asserts that the rows with timeout 90 s are exactly the six 90 s routes of §7 (`POST auth/register`, `POST trips/review`, `POST trips/{id}/review`, `POST guest/trips/review`, `POST trips`, `PUT trips/{id}`) and the only 3 s row is `POST auth/logout`.

### Task 10. BuildConfig selection in `:app`

Commit: `build(app): select the data source and api base url per build type`.

**`app/build.gradle.kts`** [R, A6, OQ-4b-6, OQ-4b-13]. One rule per build type, set in `androidComponents { onVariants { v -> … } }` through `v.buildConfigFields` (a `MapProperty`; `buildFeatures.buildConfig = true` is already on from 0a), keyed on `v.buildType`:

| Build type | `DATA_SOURCE` | `API_BASE_URL` | Overridable |
|---|---|---|---|
| `debug` | `providers.gradleProperty("cf.dataSource").orElse("mock")` | `providers.gradleProperty("cf.apiBaseUrl").orElse("https://api.chronoflow.net/v1")` | yes, by `-P` |
| `release` | `"api"` | `"https://api.chronoflow.net/v1"` | **no** (the `-P` properties are ignored) |
| the perf build types named in `docs/tooling.md` (`benchmarkRelease`, `nonMinifiedRelease`, or the OQ-A28 fallback `benchmark`) | `"mock"` | `"https://api.chronoflow.net/v1"` | **no** |
| anything else | — | — | `GradleException("No DATA_SOURCE rule for build type <name>")` |

- Debug value checks, mapped lazily on the provider: `cf.dataSource` must be `mock` or `api`, else `GradleException("cf.dataSource must be mock or api, was <value>")`; `cf.apiBaseUrl` must not contain `"` or `\`, else `GradleException("cf.apiBaseUrl must not contain quotes or backslashes")`. Full URL validation happens at runtime in `ApiBaseUrl.parse`.
- Values become `BuildConfigField("String", "\"<value>\"", null)`. Read the properties only through `providers` (configuration cache, AGENTS trap). No `buildConfigField` in `defaultConfig` (a second source of truth).
- Why perf is `mock`: the perf variants exist to measure the app on seeded data (`:core:data-mock` is a perf dependency, A6; 4c OQ-4c-4 seeds large60 there; iOS `Config/Base.xcconfig` sets `CHRONOFLOW_DATA_SOURCE = mock` for every configuration, Perf included). A perf build never talks to a server.
- Keep the existing `implementation(project(":core:network"))`; it also supplies the test classpath.
- Local server testing (no code here): `./gradlew :app:assembleDebug -Pcf.dataSource=api -Pcf.apiBaseUrl=http://10.0.2.2:<port>/v1`; 0a's debug network config allows cleartext only to `10.0.2.2`.

**Tests**
- `app/src/testDebug/kotlin/net/chronoflow/app/DebugApiConfigTest` (plain JVM): without properties, `BuildConfig.DATA_SOURCE == "mock"` and `API_BASE_URL == "https://api.chronoflow.net/v1"`; `DataSourceFactory(BuildConfig.DATA_SOURCE, BuildConfig.API_BASE_URL, release = false, testing = false).source == Mock`.
- `app/src/testRelease/kotlin/net/chronoflow/app/ReleaseApiConfigTest` (plain JVM, runs in `testReleaseUnitTest`): `DATA_SOURCE == "api"`, `API_BASE_URL == "https://api.chronoflow.net/v1"`, and `ApiBaseUrl.parse(BuildConfig.API_BASE_URL, release = true)` succeeds; `ApiBaseUrl.parse("http://api.chronoflow.net/v1", release = true)` throws `InvalidBaseUrl`; `DataSourceFactory(BuildConfig.DATA_SOURCE, BuildConfig.API_BASE_URL, release = true, testing = false).source == Api`.
- The perf build types have no unit-test task of their own; acceptance 5 checks their generated `BuildConfig.java` instead.

#### Task 10 continued. Konsist: OkHttp stays inside `:core:network`

Include these guards in Task 10's commit; the user requested Tasks 1–10 in ten atomic commits.

Two rules, following 0a's convention exactly: one function per rule in `KonsistRules.kt`; in `KonsistRulesTest.kt` the pair `<rule>()` (project scope, must pass) and `<rule>RejectsPlantedViolation()` (scope from `konsist/src/test/resources/violations/<rule>/`, must throw); module and source set from `ProjectPaths.moduleOf`/`sourceSetOf`/`isTestSourceSet`; whole-segment import matching (`import == "okhttp3" || import.startsWith("okhttp3.")`).
- `okhttpOnlyInCoreNetwork`: no file in a **non-test** source set of a module other than `core/network` imports `okhttp3` or anything under it. Test source sets are exempt (4c and 4d tests use mockwebserver3 and OkHttp types). Planted: `violations/okhttpOnlyInCoreNetwork/core/sync/src/main/kotlin/net/chronoflow/core/sync/UsesOkHttp.kt` importing `okhttp3.OkHttpClient`. Allowed control (`<rule>AllowsControl()`, must pass): `allowed/okhttpOnlyInCoreNetwork/core/sync/src/test/kotlin/net/chronoflow/core/sync/UsesOkHttpTest.kt` importing `okhttp3.OkHttpClient`.
- `noHttpLoggingInterceptor`: no file in any source set imports `okhttp3.logging` or anything under it. Planted: `violations/noHttpLoggingInterceptor/core/network/src/main/kotlin/net/chronoflow/core/network/Logs.kt` importing `okhttp3.logging.HttpLoggingInterceptor`.
- The planted files are outside every compiled source set and excluded from Spotless and the project scope, as 0a set up; do not change that wiring.

---

## 6. Acceptance criteria

All commands from the worktree root with `ANDROID_HOME` set (AGENTS.md "Commands"). Report the exact output of any command the sandbox cannot run; the orchestrator runs it.

1. `./gradlew :core:model:test :core:network:test` passes.
2. `make targeted ONLY="core:network:ApiClientTests core:network:ApiClientErrorTests core:network:ApiClientRedirectTests core:network:ApiClientTransportTests core:network:ApiErrorMapperTests core:network:ApiErrorFixtureMappingTests core:network:ApiErrorMapperMalformedTests core:network:ApiErrorMapperRetryAfterTests core:network:ApiEndpointTests core:network:ApiOriginTests core:network:DataSourceSelectionTests core:network:ApiDataSourceTests core:network:NoHealthDataTests core:network:GuestClaimWireTests core:network:AccountSecurityAPITests core:network:PasskeyManagementAPITests core:network:SettingsDeleteAPITests core:network:TripWizardAPITests core:network:TripWizardTypedErrorTests core:network:GuestTripReviewAPITests core:network:EndpointContractTest core:network:EndpointContractCoverageTest core:model:DataSourceContractTests core:model:OutboxChangeTests core:model:TripWizardErrorsTests core:model:TripWizardJourneyPathTests core:model:SettingsPatchValidationTests core:model:NoHealthDataTests core:model:SecretRedactionTest"` passes (module paths without the leading colon, 0a `Scripts/targeted.py`).
3. Coverage counts, read from the JUnit XML under `build/test-results/targeted/`: `EndpointContractTest` has **42** cases and `EndpointContractCoverageTest.coversEveryDataSourceMethod` passes (25/25 methods); `ApiErrorFixtureMappingTests` has **13** cases (13/13 fixtures); `ApiClientRedirectTests` 8, `ApiClientErrorTests` 10, `TripWizardTypedErrorTests` 7, `GuestClaimWireTests` 6.
4. `./gradlew :app:testDebugUnitTest --tests '*DebugApiConfigTest' :app:testReleaseUnitTest` passes.
5. Generated `BuildConfig.java` (quote the two lines of each in the hand-off):
   - `./gradlew :app:generateDebugBuildConfig -Pcf.dataSource=api -Pcf.apiBaseUrl=http://10.0.2.2:8000/v1` writes those two values into the debug file;
   - `-Pcf.dataSource=other` fails with `cf.dataSource must be mock or api, was other`;
   - `./gradlew :app:generateReleaseBuildConfig -Pcf.dataSource=mock -Pcf.apiBaseUrl=http://evil.invalid/v1` still writes `"api"` and `"https://api.chronoflow.net/v1"`;
   - the perf build type's task (`:app:generateBenchmarkReleaseBuildConfig`, or the name from `docs/tooling.md`) writes `"mock"` and `"https://api.chronoflow.net/v1"`.
6. `./gradlew :konsist:test` passes; both new rules reject their planted files and the control passes.
7. `grep -rn "okhttp3.logging\|HttpLoggingInterceptor\|println(\|runCatching\|catch (e: Exception)\|catch (e: Throwable)" core/network/src/main core/model/src/main` finds nothing new from this phase; `grep -rn "toRequestBody" core/network/src/main` shows only `ByteArray` receivers; `grep -rn "addPathSegment(" core/network/src/main` finds nothing (only `addEncodedPathSegment`).
8. Every JVM test is under 1 s (`build/test-results/quick/timings.json` from `make quick`, or `build/test-results/targeted/timings.json`); list the five slowest in the hand-off.
9. `make quick` passes and stays under 60 s warm; `make check` is deferred to the orchestrator under the user's testing instructions; this phase adds no emulator or Robolectric test.
10. The five 4b checkboxes in `plan.md` are ticked, each in the commit its task names, and OQ-4b-1 … OQ-4b-15 are added to plan.md "Open questions for the user" in the commits that apply them. Commits are the ten above, preceded by the signed brief-refresh commit, Conventional Commits, no trailers.
11. Nothing was written inside `/Users/rickgout/Sites/chronoflow-ios`, `/Users/rickgout/Sites/chronoflow-new` or `/Users/rickgout/Sites/chronoflow-mobile`: `git -C <repo> status --porcelain` is unchanged before and after (record both).

No screenshots and no performance numbers are produced by this phase.

---

## 7. Review checklist (Opus reviewer)

**Transport**
- [ ] The effective client: `followRedirects(false)`, `followSslRedirects(false)`, `CookieJar.NO_COOKIES`, `cache(null)`, `retryOnConnectionFailure(false)`, interceptors cleared, **all four OkHttp timeouts zero**. A leftover default 10 s read timeout breaks 90 s generation without any test noticing unless `noCookieOrCacheReuse…` asserts it.
- [ ] The client is built lazily on `io`, not in a constructor or on the caller's thread; the injected base client is unchanged.
- [ ] Bodies are `ByteArray.toRequestBody("application/json".toMediaType())` (header is exactly `application/json`); bodyless POST/PUT/PATCH/DELETE send an empty body with no `Content-Type`; GET sends none.
- [ ] `Authorization` only on Authenticated endpoints; a missing/empty/invalid token fails with `Unauthorized(401)` **before** a request is made (server count 0).
- [ ] No `Accept-Language`, `If-None-Match`, `Cookie` or device-id header anywhere (OQ-4b-15).
- [ ] Path segments are encoded by our own encoder with exactly the iOS set `[A-Za-z0-9-_~]` (`.` becomes `%2E`) and added with `addEncodedPathSegment`; empty, `.` and `..` are refused before any request; `addPathSegment` is not used.
- [ ] Origin check (`priorResponse == null`, `owns(response.request.url)`) runs before status handling.

**Deadlines and cancellation**
- [ ] Timeouts: 90 s for exactly `POST auth/register`, `POST trips/review`, `POST trips/{id}/review`, `POST guest/trips/review`, `POST trips`, `PUT trips/{id}`; 3 s for `POST auth/logout`; 15 s for everything else (the contract table proves it per row).
- [ ] A deadline cancels the OkHttp `Call` (`EventRecorder.canceled == 1`) and yields `Offline`; caller cancellation yields `CancellationException`, never `Offline`.
- [ ] No `runCatching`, no `catch (e: Exception)`, no `catch (e: Throwable)`; `CancellationException` is never swallowed; `IllegalArgumentException` is caught only around decoding and header building, `DateTimeParseException` only around the `Retry-After` date; `IOException`s are mapped inside `work` (before-response `ProtocolException` → `Server(null)`, everything else → `Offline`).
- [ ] Tests never wait on wall time: the default test deadline is `awaitCancellation()`; no `Thread.sleep`, no `delay` used as a wait, no `withTimeout` racing real I/O under `runTest`'s virtual clock.

**Error mapping (compare with `APIClient.swift:118-178` line by line)**
- [ ] 429 is handled before the envelope check; body `retryAfterSeconds` only when a non-string finite number ≥ 0; then header seconds, then RFC 1123 date against the injected clock, then 60.
- [ ] `(status, code)` pairs, not codes alone; 401 without an envelope is `Server(401)` (parity, OQ-4b-7).
- [ ] `validation_failed` rejects empty `errors`, empty keys or values, and non-string/number args (including `true` and `null`).
- [ ] The three 2FA sentences map to `form` with status 409.
- [ ] `TripWizardErrors.map` is applied with the outbound count for the three reviews and without it for save/update; `journeyPath` drops empty pieces and `camel` keeps them.

**Data source**
- [ ] All 40 routes match Appendix A (method, path, access, status, body); `send(Unsupported)` makes no request; `SettingsPatchValidation` runs before transport.
- [ ] Post-decode checks: passkey options (64 / `app.chronoflow.net` / 60000 / 43 chars / 32 bytes), trip list ids, detail id; clean raw detail bytes are returned untouched; legacy-health sanitization remains in `TripDetailResponse.decode`.
- [ ] `AddPasskey` expects 201, `saveTrip` 201, `updateTrip` 200, every 204 route requires an empty body.
- [ ] Decoding (including `TripDetailResponse.decode`) runs on `io`.

**Hygiene**
- [ ] No logging of any kind; `ApiEndpoint.toString()` omits the body; secret-bearing `AccountSecurityChange` cases redact `toString`.
- [ ] `:core:model` and `:core:network` have no `android.*` import; only `:core:network` imports `okhttp3` (Konsist).
- [ ] `BuildConfig` per build type: release fixed to `api` + the production HTTPS URL and deaf to `-P`; perf build types fixed to `mock`; debug reads `providers.gradleProperty` with the default `mock`; an unknown build type fails the configuration. No `buildConfigField` in `defaultConfig`.
- [ ] `CheckOffChange` is 1a's (used, never redeclared) and not `@Serializable` (4d owns its storage codec); the check-off wire body is 0d's `ApiCheckOffInput` with `"status": null` present.
- [ ] Ported tests keep their Swift names (Appendix B); the table rule holds (no plain test inside a `Parameterized` class, no loop over rows inside one `@Test`, no `Enclosed`); MockApi fails a test on any unmatched request; every server is closed; no test imports `kotlin.reflect.full`.

**Typical mistakes to look for:** `String.toRequestBody` (adds `; charset=utf-8`); `Request.Builder().post(null)` or `method("POST", null)` (throws); `addPathSegment("..")` (pops the base path); `split(".")` keeping empty pieces in `journeyPath`; `JsonPrimitive("true").booleanOrNull` accepted as a boolean; leaving `retryOnConnectionFailure` on (a disconnect becomes two requests); mapping a `ProtocolException` from `call.await()` to `Offline`, or one from the body read to `Server` (a mid-body disconnect is `Offline`); mapping exceptions outside `work`; catching `Exception` around the `Retry-After` date parse instead of `DateTimeParseException`; decoding with a `Json` other than `ApiJson.response`; comparing request bodies as strings instead of `JsonObject`s; reading `Retry-After` with `toDouble()` (accepts `NaN`, `1e3`, `12d`); a `MockWebServer` left running; mockwebserver 4.x API names.

---

## 8. Open questions (proceed on the default; add each to plan.md "Open questions for the user", numbered from OQ-A132 (OQ-A68 stays unused), in the commit that applies it)

| # | Question | Default |
|---|---|---|
| OQ-4b-1 | Where do `TripWizardErrors` and `SettingsPatchValidation` live (iOS: `DataAccess`)? | `:core:model` package `api`, so the mock, auth and wizard modules use them without OkHttp. |
| OQ-4b-2 | iOS sends a `..` path segment as `%2E%2E`; OkHttp resolves `.`/`..` and their `%2E` forms as dot segments. | Refuse `.` and `..` segments with `Server(null)` before sending (ids are UUIDs in practice); otherwise encode exactly as iOS, `.` → `%2E`. |
| OQ-4b-3 | OkHttp retries some failed connections silently; iOS makes exactly one attempt ("no automatic retry"). | `retryOnConnectionFailure(false)`. |
| OQ-4b-4 | A bearer token with characters OkHttp rejects in a header. | `Unauthorized(401)` without sending. |
| OQ-4b-5 | `DataSource.send` needs `OutboxChange` in 4b, but 0d's hand-off gives `OutboxChange`/`CheckOffChange` to 4d. | 4b adds the in-memory types (only `ProfileChange` is `@Serializable`, because it is a request body); 4d adds the storage codec (including `@Serializable` on `CheckOffChange`, status omitted when null, 4d OQ-4d-3) and the outbox round-trip test. |
| OQ-4b-6 | Can a release build be pointed at another server or the mock? | No: `release` is fixed to `api` at `https://api.chronoflow.net/v1` (A6); only debug reads `-Pcf.dataSource` / `-Pcf.apiBaseUrl`. |
| OQ-4b-7 | A 401 or 403 without a decodable envelope maps to `Server(status)` on iOS, so it never wipes or gates. | Keep iOS parity; 4d decides retry/refusal from the status. |
| OQ-4b-8 | `DataSourceParityTests` compare mock and API. | Port all of it in 4c, which has the mock; 4b ports only API-only cases. |
| OQ-4b-9 | `DataSourceFactory.testing` (iOS forces mock under tests). | Port the parameter; 4e passes `false` in the app (instrumented tests run the debug build, which defaults to mock). |
| OQ-4b-10 | A 204 that carries a body: iOS gives `Server(204)`; OkHttp may raise a protocol error first. OkHttp also uses `ProtocolException` for a body cut short by a dropped connection. | A `ProtocolException` raised before a `Response` exists → `Server(null)`; any `IOException` while reading the body, `ProtocolException` included → `Offline` (iOS: transport failure → offline). The 204 test accepts any `Server`. |
| OQ-4b-11 | `Retry-After` header grammar: Swift `Double()` also accepts exponents. | Digits with an optional fraction, then RFC 1123 date, then 60 s. |
| OQ-4b-12 | User-Agent: OkHttp sends `okhttp/5.5.1`. | Keep the default; it carries no personal data. |
| OQ-4b-13 | Which data source do the perf variants (`benchmarkRelease`, `nonMinifiedRelease`) use? A6 says only "release is always api". | `mock`, fixed: they carry `:core:data-mock` and measure seeded data (4c OQ-4c-4), and iOS's Perf configuration inherits `CHRONOFLOW_DATA_SOURCE = mock` (`Config/Base.xcconfig`). Not overridable. |
| OQ-4b-14 | Plan A6 says per-call `withTimeout`; iOS races the request against an injectable `sleep`. | Race against an injectable `ApiDeadline` (default `delay`): same semantics (the deadline cancels the OkHttp call and yields `Offline`; the caller's cancellation stays cancellation), and tests never wait on wall time. |
| OQ-4b-15 | plan.md 4b "ApiClient" lists "the device id" among the request headers, but `03 §2`, iOS `APIClient.swift:68-76` and the web API (no controller reads such a header at `e36368ec`) have no device-id header; `device_id` is a body key of login, passkey login and register. | No device-id header (iOS parity); `device_id` only in those bodies. `headersAreExact` asserts no header name contains `device`. |

---

## 9. Hand-off to later phases

- **4c:** `MockDataSource` implements the same `DataSource`, reuses `SettingsPatchValidation`, `TripWizardErrors.map` and `TripWizardErrors.camel`, and ports `DataSourceParityTests`, `AccountSecurityAPITests.mockRunsSetupCodesManageAndPasswordAndRejectsOffline` and `SettingsDeleteAPITests.mockDeletionRequiresPasswordAndActuallyRemovesAccount`.
- **4d:** adds `OutboxChange` serialization (iOS synthesized `Codable` shape, including `@Serializable` on `CheckOffChange` with `status` omitted when null) and the outbox half of `ContractsTests.inputWireKeysAndOutboxRoundTrips`; reuses 4b's `OutboxChange`/`MutationReceipt` as they are (property names `change`, `snapshot`, `identity`, `result`); classifies refusals from `DataFailure.status` (final = 4xx except 401, 408, 425, 429 and email-unverified) and backs off with `Throttled.retryAfterSeconds`.
- **4e:** builds `ApiClient(baseUrl = DataSourceFactory(BuildConfig.DATA_SOURCE, BuildConfig.API_BASE_URL, release = !BuildConfig.DEBUG, testing = false).baseUrl, bearer = { session token }, io = AppDispatchers.io)` lazily in `AppGraph` (without passing `baseClient`, so `:app` main never imports OkHttp), maps `source` to `StorageNamespace`, and wipes on `Unauthorized` only for the current epoch. Note for the 4e brief: a `SessionResult` that fails decoding on a 200 maps to `DataFailure.Server(200)` (iOS parity: decode failures carry the response status), not `Server(null)`.
- **5a–5d, 9b, 10c–10d:** call `DataSource`; translate `FieldMessage` keys through `CopyIndex` with `interpolationKey`; never show a server sentence directly in Dutch.

---

## Appendix A. Route table (every `DataSource` call) [F `Kit/DataAccess/API/*.swift`, `W/routes/api.php` at `e36368ec`]

"Bearer" = `Authenticated`; "Public" = `PublicRoute`. Timeouts in seconds. Body keys marked `?` are omitted when null (0d rule 4.1). "none" = no body.

| # | Call | Method | Path (under `/v1/`) | Access | Timeout | Body | Success | Returns |
|---|---|---|---|---|---|---|---|---|
| 1 | `passkeyOptions()` | POST | `auth/passkey/options` | Public | 15 | none | 200 | `ApiPasskeyChallenge` (validated) |
| 2 | `passkeyLogin(i)` | POST | `auth/passkey/login` | Public | 15 | `challengeId`, `credential{id,rawId,type,response{clientDataJSON,authenticatorData,signature,userHandle?}}`, `device_id`, `device_name`, `draft_token?`, `guest_secret?` | 200 | `SessionResult` |
| 3 | `signIn(i)` | POST | `auth/login` | Public | 15 | `email`, `password`, `device_id`, `device_name`, `draft_token?`, `guest_secret?` | 200 | `SignInResult` |
| 4 | `completeTwoFactor(i)` | POST | `auth/two-factor` | Public | 15 | `challengeId` + exactly one of `code` / `recovery_code` | 200 | `SessionResult` |
| 5 | `signUp(i)` | POST | `auth/register` | Public | **90** | `name`, `email`, `password`, `password_confirmation`, `device_id`, `device_name`, `language?`, `draft_token?`, `input_revision?`, `review_revision?`, `guest_secret?` | 200 | `SessionResult` |
| 6 | `requestPasswordReset(e)` | POST | `auth/forgot-password` | Public | 15 | `{email}` | 204 | — |
| 7 | `resetPassword(i)` | POST | `auth/reset-password` | Public | 15 | `token`, `email`, `password`, `password_confirmation` | 204 | — |
| 8 | `verificationStatus()` | GET | `users/me` | Bearer | 15 | none | 200 | `EmailVerificationStatus(email, hasVerifiedEmail, 0)` |
| 9 | `resendVerificationEmail()` | POST | `email/verification-notification` | Bearer | 15 | none | 200 | `EmailVerificationStatus` |
| 10 | `verifyEmail(i)` | POST | `email/verify` | Bearer | 15 | `id`, `hash`, `expires` (number), `signature` | 200 | `EmailVerificationStatus` |
| 11 | `signOut()` | POST | `auth/logout` | Bearer | **3** | none | 204 | — |
| 12 | `deleteAccount(p)` | DELETE | `account` | Bearer | 15 | `{password}` | 204 | — |
| 13a | `accountSecurity(RequestPasswordReset)` | = row 6 | | | | | | `Changed` |
| 13b | `accountSecurity(Password)` | PUT | `account/password` | Bearer | 15 | `{current_password, password, password_confirmation}` | 204 | `Changed` |
| 13c | `accountSecurity(EnableTwoFactor)` | POST | `account/two-factor` | Bearer | 15 | `{current_password}` | 200 | `Setup(ApiTwoFactorSetup)` |
| 13d | `accountSecurity(ConfirmTwoFactor)` | POST | `account/two-factor/confirmation` | Bearer | 15 | `{code}` | 200 | `Codes(ApiRecoveryCodes)` |
| 13e | `accountSecurity(DisableTwoFactor)` | DELETE | `account/two-factor` | Bearer | 15 | `{current_password}` | 200 | `Snapshot(AccountSnapshot)` |
| 13f | `accountSecurity(RevealRecoveryCodes)` | POST | `account/two-factor/recovery-codes/reveal` | Bearer | 15 | `{current_password}` | 200 | `Codes` |
| 13g | `accountSecurity(RegenerateRecoveryCodes)` | POST | `account/two-factor/recovery-codes` | Bearer | 15 | `{current_password}` | 200 | `Codes` |
| 13h | `accountSecurity(ListPasskeys)` | GET | `account/passkeys` | Bearer | 15 | none | 200 | `Passkeys(list.passkeys)` |
| 13i | `accountSecurity(PasskeyCreationOptions)` | POST | `account/passkeys/options` | Bearer | 15 | `{current_password}` | 200 | `PasskeyCreation(ApiPasskeyRegistrationChallenge)` |
| 13j | `accountSecurity(AddPasskey)` | POST | `account/passkeys` | Bearer | 15 | `challengeId`, `name`, `credential{id,rawId,type,response{clientDataJSON,attestationObject,transports?}}` | **201** | `PasskeyAdded(ApiPasskey)` |
| 13k | `accountSecurity(RemovePasskey)` | DELETE | `account/passkeys/{id}` | Bearer | 15 | `{current_password}` | 204 | `Changed` |
| 14 | `fetchIdentity()` | GET | `users/me` | Bearer | 15 | none | 200 | `AccountIdentity` |
| 15 | `fetchAccount()` | GET | `account` | Bearer | 15 | none | 200 | `AccountSnapshot` |
| 16 | `fetchTrips()` | GET | `trips` | Bearer | 15 | none | 200 | `TripListResponse.trips` (ids validated) |
| 17 | `fetchTripDetail(id)` | GET | `trips/{id}` | Bearer | 15 | none | 200 | `TripDetailResponse` (raw bytes; `trip.id == id`) |
| 18 | `searchAirports(q, l)` | GET | `airports?limit=&query=` (`limit` only when non-null) | Public | 15 | none | 200 | `ApiAirportSearchResults.airports` |
| 19 | `reviewTrip(i)` | POST | `trips/review` | Bearer | **90** | `ApiTripReviewInput` (0d) | 200 | `ApiTripReview` |
| 20 | `reviewGuestTrip(i)` | POST | `guest/trips/review` | Public | **90** | `ApiGuestTripReviewInput` (0d) | 200 | `ApiTripReview` |
| 21 | `reviewTripEdit(i, id, src)` | POST | `trips/{id}/review` | Bearer | **90** | row 19 + `source_revision` | 200 | `ApiTripReview` |
| 22 | `saveTrip(s)` | POST | `trips` | Bearer | **90** | `{draft_token, input_revision, review_revision}` | **201** | `ApiTripSaveResult` (non-empty `tripId`) |
| 23 | `updateTrip(id, s, src)` | PUT | `trips/{id}` | Bearer | **90** | row 22 + `source_revision` | 200 | `ApiTripSaveResult` |
| 24 | `tripInput(id)` | GET | `trips/{id}/input` | Bearer | 15 | none | 200 | `ApiTripInputSnapshot` |
| 25a | `send(CheckOff)` | PUT | `trips/{tripID}/checks` | Bearer | 15 | `{plan_input_hash, type, local_date, ordinal, status}` (`status` always present) | 200 | `Checks(TripCheckResult)` |
| 25b | `send(Settings(BodyClock))` | PATCH | `account/body-clock` | Bearer | 15 | the patch | 200 | `Account(AccountSnapshot)` |
| 25c | `send(Settings(Preferences))` | PATCH | `account/preferences` | Bearer | 15 | the patch | 200 | `Account` |
| 25d | `send(Settings(Reminders))` | PATCH | `account/reminders` | Bearer | 15 | the patch | 200 | `Account` |
| 25e | `send(Profile)` | PATCH | `users/me` | Bearer | 15 | `{name, email}` | 200 | `Identity(AccountIdentity)` |
| 25f | `send(Policies(v))` | POST | `terms/acceptance` | Bearer | 15 | `{"accepted": true, "version": v}` | 204 | `Acknowledged` |
| 25g | `send(Walkthrough)` | POST | `walkthrough` | Bearer | 15 | none | 204 | `Acknowledged` |
| 25h | `send(Unsupported(kind))` | — | — | — | — | — | — | throws `Unsupported(kind)`, no request |

Rows sending a request: 1–12 (12), 13b–13k (10), 14–24 (11), 25a–25g (7) = **40**.

## Appendix B. iOS → Kotlin test map

| iOS test (`KitTests/…`) | Kotlin (module: class) | Task |
|---|---|---|
| `APIClientTests.errors(code:status:)` | `ApiClientErrorTests.errors` (Parameterized, 10) | 5 |
| `APIClientTests.refusesEveryRedirect(status:)` | `ApiClientRedirectTests.refusesEveryRedirect` (Parameterized, 8: 4 statuses × same/cross origin) | 5 |
| `APIClientTests.transportFailuresMapToOffline(_:)` | `ApiClientTransportTests.transportFailuresMapToOffline` (Parameterized, 4 OkHttp failure modes) | 5 |
| `APIClientTests.malformedErrors(body:)`, `retryAfter(bodyMember:header:expected:)` | `ApiErrorMapperMalformedTests.malformedErrors` (10), `ApiErrorMapperRetryAfterTests.retryAfter` (10), both Parameterized at the pure mapper; one end-to-end case each in `ApiClientTests` (`malformedErrorsEndToEnd`, `retryAfterEndToEnd`) | 4, 5 |
| `APIClientTests.fieldArguments`, `explicitCancellationIsNotAnOfflineFailure`, `noCookieOrCacheReuseAndMissingBearer`, `sanitizesInjectedConfiguration` | `ApiClientTests`, same names | 5 |
| `APIClientTests.deadlinesCancelUnderlyingTask(seconds:)`, `noContentRequiresExactShape` | `ApiClientTests.deadlinesCancelUnderlyingTask3s/15s/90s`, `noContentRequiresExactShape200/201/204WithBody/204Empty` | 5 |
| `APIClientTests.registrationValidationEnvelopePreservesVisibleFieldMessages`, `malformedSuccess(body:)` | `ApiClientTests`, same name / `malformedSuccessEmptyObject/Html/SessionWithoutAccount` | 6 |
| `APICookieTests.realRedirectCookieHandlingIsDisabled` (loopback server) | covered by `refusesEveryRedirect` + `noCookieOrCacheReuse…` (MockWebServer is a real loopback server) | 5 |
| `APIDataSourceTests.*` (5) | `ApiDataSourceTests`, same names | 6 |
| `AccountSecurityAPITests.everySecurityRouteUsesTheRealMethodAndBody`, `backendConflictKeysSurviveErrorMapping` | `AccountSecurityAPITests`, same names | 7 |
| `AccountSecurityAPITests.mockRuns…` | 4c | — |
| `PasskeyManagementAPITests.*` (3) | `PasskeyManagementAPITests`, same names | 7 |
| `SettingsDeleteAPITests.deletionSends…` / `mockDeletion…` | `SettingsDeleteAPITests` / 4c | 7 |
| `TripWizardAPITests.wireContractsRequestsAndResponses` (HTTP half), `every429RetainsRetryDeadline`, `bodyRetryHintSurvivesMissingEnvelopeCode`, `saveBodyThrottleAndEmailVerification`, `saveMapsDomainValidation`, `transportAndDeadlineAreOfflineWithoutRetry` | `TripWizardAPITests`, same names (row cases as suffixed methods; `promptsAndFractionalBounds` is 0d) | 8 |
| `TripWizardAPITests.typedErrors(name:)` | `TripWizardTypedErrorTests.typedErrors` (Parameterized, 7) | 8 |
| `GuestTripReviewAPITests.publicRouteDecodesMintedOrNullSecret(minted:)`, `mapsGuestFailures(kind:)` | `GuestTripReviewAPITests`, suffixed methods (`…Minted/Null`, `…Expired/ValidationExpired/Throttled/Bare429/Profile`; the two contract tests are 0d) | 8 |
| `GuestClaimAuthTests.requestUsesOnlyTheClaimFieldsForItsMethod(_:_:)` (HTTP half) | `GuestClaimWireTests.requestUsesOnlyTheClaimFieldsForItsMethod` (Parameterized, 6) | 6 |
| `GuestClaimAuthTests` (model half), `registrationFailureClearsBothPasswords…` | 5a / 5d | — |
| `NoHealthDataTests.patchAllowsOnlyNeutralPreferencesAndRequiresBooleans` | `:core:model: NoHealthDataTests` | 2 |
| `NoHealthDataTests.patchEncodingSendsOnlyNeutralPreferences…` | `:core:network: NoHealthDataTests` | 6 |
| `DataSourceSelectionTests.defaultsAndIsolation`, `rejectInvalidSettings` | `:core:network: DataSourceSelectionTests` (namespace half → 4e) | 3 |
| `DataSourceParityTests.*` | 4c | — |
| `PlanDecodingTests.rawAPIBytesAndUnknownCheckSurviveStorageAndReconciliation` (HTTP half) | `ApiDataSourceTests.fetchTripDetailKeepsTheExactServerBytes` | 6 |
| — (Android) | `DataSourceContractTests`, `OutboxChangeTests` (1); `TripWizardJourneyPathTests` (Parameterized), `TripWizardErrorsTests`, `SettingsPatchValidationTests` (2); `ApiEndpointTests`, `ApiOriginTests` (3); `ApiErrorFixtureMappingTests` (Parameterized), `ApiErrorMapperTests` (4); `EndpointContractTest` (Parameterized), `EndpointContractCoverageTest` (9); `DebugApiConfigTest`, `ReleaseApiConfigTest` (10) | 1–4, 9, 10 |
