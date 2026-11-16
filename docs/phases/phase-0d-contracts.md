# Phase 0d brief: API contract generator and fixtures

- **Branch:** `feat/phase-0d-contracts`, from the stack top once 0a is in the stack. It runs in parallel with 0b, 0c and 0e (wave 2), in its own worktree.
- **Depends on:** 0a. **Unblocks:** 1a, 4b (and through 4b: 4c, 4d, 4e), 9a.
- **Plan entry:** `plan.md` "### Phase 0d: API contract generator and fixtures (`feat/phase-0d-contracts`)" (l.871–919). Decisions A6 (JSON rules), A18 (contracts), A3 (module rules).
- **Brief written:** 2026-11-16 by the orchestrator; revised the same day against the final 0a brief (Makefile `+=` wiring, `make targeted` syntax, `Scripts/test_scripts.sh`) and the 1a/1b briefs (they append to this phase's fixture manifest). iOS read at `f391a1fa` (`tools/ios-pin.txt`), web at `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19` (`tools/web-pin.txt`).
- **Checked 2026-11-16** (adversarial pass against iOS `f391a1fa`, web `e36368ec`, plan.md, AGENTS.md and the 0a/0c/0e/1a/1b/4b briefs): line references corrected (iOS `plan.md` Phase 13, `SessionController.swift`, `APIDataSource.swift`); pin abbreviation rule for the manifest; Task 1 no longer edits the catalog; the six `JsonValues` cases; generator mapping order, synthetic-source rules and `Wire` twins excluded from parity; `ApiReviewPrompt` null omission; Swift optional accessors → `as?`; one `TripWizardContractTests` and one `ContractsTests` class (package `api`); the `guest-review-request.json` failure is caused by `override`; custom-serializer failures are `SerializationException`; Python test env vars; `ApiJson.response` hand-off for 1a and 1b.
- **Tests:** every test you write follows AGENTS.md "Tests". This phase is **JVM and Python only**: JUnit 4.13.2 (`libs.junit4`) and assertk 0.28.1, both already added by 0a's `chronoflow.jvm.library` plugin; Python `unittest` (stdlib). No Robolectric, no emulator, no UI, no tier annotation (plain JVM tests carry none, 0e).
- **Boards:** none. This phase has no UI. Do not create any screen, string resource or design token.

Tags: **[F]** fact verified in source (path and line at the pin). **[R]** decision for Android adopted by this brief.

---

## 1. Goal

Give every later phase one typed, pinned, tested API contract in `:core:model`, identical in behaviour to iOS `AppCore/Contracts`:

1. **Generated contracts.** `tools/contracts-gen/generate_kotlin.py` is a port of iOS `Tools/contracts-gen/generate.py`. It reads the web TypeScript types at `e36368ec` through `git show` and writes one committed Kotlin file, `TripPlan.generated.kt`, with the **same 130 declarations as iOS `TripPlan.swift`** (79 data classes, 50 enums, 1 typealias), the same roots, renames, overrides and the `ScreeningQuestion = 'shift_work'` overlay. It has a `--check` mode.
2. **Hand-written contracts.** The Kotlin twins of the iOS hand-written files: auth, session, account, account security, passkeys, passkey management (web Phase 20, unmerged), trip-wizard requests, guest review request, the check-off request, the error envelope, `ProfilePreferences` (no-health-data cleaning), `BlockParams`, `CopyArgument`, `TripDetailResponse`, plus `DataFailure`, `FieldMessage`, `OutboxKind` and `AccountID`.
3. **One JSON configuration**, `ApiJson`: tolerant decoding, request encoding with explicit nulls where the wire needs them.
4. **Fixtures.** The 46 iOS `DataAccessTests` fixture JSON files, byte-identical, in `:core:testing`, with a SHA-256 manifest and a loader.
5. **Proof.** Every fixture decodes; every request DTO encodes to exactly its fixture's keys and values; unknown enum values survive as `Unknown(raw)`; the generator rejects unsupported TypeScript and drifts are caught by `make contracts-check`.

No networking (4b), no database (4a), no mock (4c), no outbox (4d).

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` l.871–919 (Phase 0d), A3, A6, A18, "What we reuse from iOS" R10/R11, "Android spec: build and repository layout" | Scope and decisions |
| Agent rules | `AGENTS.md` ("Sources", "Working rules": Secrets, Comments; "Tests"; "Git"; "Traps": rethrow `CancellationException`, configuration cache) | How to work |
| Research | `docs/research/03-api-contract.md` §2, §3, §5, §16 | Wire rules, envelope, endpoint shapes, generator design |
| Research | `docs/research/02-ios-architecture.md` §4.11, §4.12, §5.6 | `DataFailure`, contracts, fixtures |
| 0a output | `Makefile` (placeholder target `contracts`; `generators-check`, `generate`, `quick` with the empty `GENERATORS`/`GENERATOR_CHECKS` lists; there is **no** 0a `contracts-check` stub, you add it), `gradle/libs.versions.toml`, `build-logic/convention`, `core/model/build.gradle.kts`, `core/testing/build.gradle.kts`, Spotless config | Where you wire things |
| Pins | `tools/ios-pin.txt` (0a writes the **full** SHA `f391a1fac33eb0f0ea9e3f565206b91118e793d0` plus a newline, OQ-0a-9; print it as `pin[:8]` = `f391a1fa`, as 0c does), `tools/web-pin.txt` (`e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19`) | Inputs of both scripts |

### 2.2 iOS (read-only): `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`

| iOS file (relative to the iOS root) | Port to (Kotlin, under `core/model/src/main/kotlin/net/chronoflow/core/model/`) |
|---|---|
| `Tools/contracts-gen/generate.py` (196 lines; every line matters) | `tools/contracts-gen/generate_kotlin.py` |
| `Tools/contracts-check`, `Makefile:101-105` | Make targets `contracts`, `contracts-check` |
| `Tests/test_contracts_generator.py` | `tools/contracts-gen/test_generate_kotlin.py` |
| `Packages/ChronoFlowKit/Sources/AppCore/Contracts/Generated/TripPlan.swift` | Parity reference for `contracts/TripPlan.generated.kt` (never copied) |
| `…/AppCore/Contracts/PlanContracts.swift` | `contracts/PlanContracts.kt` |
| `…/AppCore/Contracts/CopyLine.swift` | `contracts/CopyLine.kt` |
| `…/AppCore/Contracts/ProfilePreferences.swift` | `contracts/ProfilePreferences.kt` |
| `…/AppCore/Contracts/TripContracts.swift` | `contracts/TripContracts.kt` |
| `…/AppCore/Contracts/JSONValue.swift` | not ported: use kotlinx `JsonElement`/`JsonObject`/`JsonPrimitive`; only `JSONValue.int` → `contracts/JsonValues.kt` |
| `…/AppCore/Contracts/AuthContracts.swift` | `api/AuthContracts.kt` |
| `…/AppCore/SessionController.swift` l.1–70 (`AccountPolicies`, `SessionAccount`, `SessionResult`) | `api/SessionContracts.kt` |
| `…/AppCore/Contracts/AccountTripContracts.swift` | `api/AccountTripContracts.kt` |
| `…/AppCore/Contracts/AccountSecurityContracts.swift` l.1–15 (the two DTOs only) | `api/AccountSecurityContracts.kt` |
| `…/AppCore/Contracts/PasskeyContracts.swift` | `api/PasskeyContracts.kt` |
| `…/AppCore/Contracts/PasskeyManagementContracts.swift` | `api/PasskeyManagementContracts.kt` |
| `…/AppCore/Contracts/TripWizardContracts.swift` | `api/TripWizardContracts.kt` |
| `…/AppCore/Contracts/GuestTripReviewInput.swift` | `api/GuestTripReviewInput.kt` |
| `…/DataAccess/API/APIDataSource.swift` l.86–105 (inline check-off `Body`) | `api/ApiCheckOffInput.kt` |
| `…/DataAccess/API/APIClient.swift` l.108–116 (`ErrorEnvelope`) | `api/ApiErrorEnvelope.kt` |
| `…/AppCore/DataFailure.swift` | `DataFailure.kt` (`FieldMessage`, `DataFailure`) |
| `…/AppCore/OutboxChange.swift` l.41–48 (`OutboxKind` only) | `OutboxKind.kt` |
| `…/AppCore/AccountID.swift` | `AccountID.kt` |
| `…/Tests/DataAccessTests/Fixtures/{account,auth,errors,plans,trips,wizard}/*.json` (46 files) | `core/testing/src/main/resources/fixtures/…` (byte copies) |
| `…/Tests/DataAccessTests/Fixtures/{README.md,plans/SOURCE.md,trips/SOURCE.md,wizard/SOURCE.md}` | Provenance, summarised in `fixtures/SOURCE.md` |
| Tests to port: `…/Tests/DataAccessTests/{PlanDecodingTests,ContractsTests,PasskeyManagementContractsTests,ReviewPriceContractTests,NoHealthDataTests,TripWizardAPITests,GuestTripReviewAPITests}.swift` | see Appendix B |

iOS `plan.md` sections (read at the pin): l.435 "API contract and offline sync"; l.1323 "1.1 How the API handles trips"; l.1448 "1.3 What iOS already has"; l.1555 "Phase 12: Typed trip contracts" (12a: typed DTOs, raw bytes, contracts-gen); l.1580–1598 "Phase 13 … 13a" (wizard DTOs; l.1598 **fix round 1: "generate explicit nulls for required nullable request fields, preserve omission for optional keys"**).

### 2.3 Web (read-only): `git -C /Users/rickgout/Sites/chronoflow-new show e36368ec:<path>`

`resources/js/types/App/{Data/Api,Data/Trips,Data/Plan,Planner/Output,Planner/Input,Enums,Planning/Copy}/index.ts` (333, 214, 301, 22, 10, 25 and 2 lines). The checkout is on another branch (`feat/mobile-api-passkey-management`); **never** read the working tree, always `git show` at the pin.

### 2.4 Research quotes this brief relies on

- `03 §2` [F]: "Bodies are **snake_case**. Responses are **camelCase**. Exceptions in auth inputs: `challengeId` (2FA and passkeys) and `credential.*` (WebAuthn member names). Exception in responses: `ApiTripInputSnapshot.input` is snake_case, so it can be sent back as it is."
- `03 §3` [F]: "Every error has the same envelope: `{code, message, errors?, args?, retryAfterSeconds?}` … Empty optional members are **absent**, not null … `errors` holds the **first** message for each field, as a string."
- `03 §5.3` #39 [F]: `PUT /v1/trips/{uuid}/checks` body "exactly `{plan_input_hash(64 hex), type: BlockType, local_date(Y-m-d), ordinal(int 1–255, strict), status: 'done'|'skipped'|null}`. `status` must be **present**."
- `03 §16` [R]: "Emit `@Serializable data class` with `@SerialName` for snake_case … `BlockView` gets a custom `KSerializer` that reads `type` before it decodes `params`. `CopyArg` and `CaffeineCopyArg` are told apart by the `drinks` key. Prompt `answer` becomes `Map<String, JsonPrimitive>`."
- `02 §4.12` [F]: "`TripDetailResponse` keeps the raw bytes (`response.data`), so the DB stores the exact server JSON."
- `02 §4.11` [F]: `DataFailure` cases `reviewExpired, notEnoughCredits, saveCapacity, profileInvalid(fields), offline, unauthorized(status), emailUnverified(status), validation(fields,status), throttled(retryAfterSeconds,status), conflict(status), server(status?), unsupported(kind)`, plus `status`.

### 2.5 API endpoints whose bodies this phase types (no calls are made)

| Endpoint | Request DTO | Response DTO |
|---|---|---|
| `POST auth/login` | `PasswordSignInInput` | `SignInResult` (`{kind:"session",…}` or `{kind:"two-factor",challengeId}`) |
| `POST auth/two-factor` | `TwoFactorInput` | `SessionResult` |
| `POST auth/register` | `RegisterInput` | `SessionResult` |
| `POST auth/reset-password` | `ResetPasswordInput` | 204 |
| `POST email/verify` | `SignedVerificationInput` | `EmailVerificationStatus` |
| `POST email/verification-notification` | none | `EmailVerificationStatus` |
| `POST auth/passkey/options` | none | `ApiPasskeyChallenge` |
| `POST auth/passkey/login` | `ApiPasskeyLoginInput` | `SessionResult` |
| `GET users/me` | — | `SessionAccount` (= `AccountIdentity`) |
| `GET account` | — | `AccountSnapshot` |
| `POST account/two-factor`, `…/recovery-codes[/reveal]`, `…/confirmation` | built in 4b | `ApiTwoFactorSetup`, `ApiRecoveryCodes` |
| `GET account/passkeys`, `POST account/passkeys/options`, `POST account/passkeys` (web Phase 20: **not at the pin**, only on branch `feat/mobile-api-passkey-management`, `routes/api.php:87-109` there) | `ApiPasskeyRegistrationInput` | `ApiPasskeyList`, `ApiPasskeyRegistrationChallenge`, `ApiPasskey` |
| `GET trips` | — | `TripListResponse` `{trips: [TripListItem]}` |
| `GET trips/{id}` | — | `TripDetailResponse` (raw bytes + `TripDetail`) |
| `PUT trips/{id}/checks` | `ApiCheckOffInput` | `TripCheckResult` |
| `GET airports?query=&limit=` | — | `ApiAirportSearchResults` |
| `POST trips/review`, `POST trips/{id}/review` | `ApiTripReviewInput` | `ApiTripReview` |
| `POST guest/trips/review` | `ApiGuestTripReviewInput` | `ApiTripReview` |
| `POST trips`, `PUT trips/{id}` | `ApiTripSaveInput` (+ `source_revision` added in 4b for PUT) | `ApiTripSaveResult` |
| `GET trips/{id}/input` | — | `ApiTripInputSnapshot` |
| any error | — | `ApiErrorEnvelope` |

---

## 3. Out of scope (owner in brackets)

- `ApiClient`, `ApiDataSource`, the `DataSource` interface, error-envelope → `DataFailure` mapping, `Retry-After` parsing, `SettingsPatchValidation`, `TripWizardErrors.journeyPath`, inline bodies such as `{email}`, `{current_password}`, `{accepted, version}` [4b].
- `OutboxChange`, `CheckOffChange`, `SettingsSection`, `ProfileChange`, `MutationReceipt`, `AccountSecurityChange/Result` [4d / 4b]. Only `OutboxKind` lands here because `DataFailure.Unsupported` needs it.
- `PlanDecoder`, caching per `detailVersion`, `CopyCatalog`, formatting, `intl-cases.json` [1a]. Mock seeds, `MockSettings.readiness`, rebasing [4c]. `profiles.json` [0e]. large30/large60 [1b].
- Typing account presentation objects (`bodyClock`, `preferences`, `reminders`, `planCredits`, `readiness`, `billing`, `security` stay `JsonObject`, as iOS).
- Generating the Phase 20 passkey-management DTOs (unmerged on web; they stay hand-written, as iOS).
- ETags, HTTP caching [no, A6]. Compose stability config for model types [2 / 6b].
- Any string resource, `CopyIndex` entry, UI or board.

---

## 4. Module and package layout

- `:core:model` (plugin `chronoflow.jvm.library`): apply `alias(libs.plugins.kotlin.serialization)` (0a's catalog alias `kotlin-serialization` = `org.jetbrains.kotlin.plugin.serialization`, Kotlin 2.4.21, declared `apply false` in the root build) and add `api(libs.kotlinx.serialization.json)` (1.13.0, already in 0a's catalog). Do not add catalog entries. Opt in module-wide to `kotlinx.serialization.ExperimentalSerializationApi` through `compilerOptions.optIn` (needed for `explicitNulls` and `SerialDescriptor(name, original)`; `allWarningsAsErrors` stays on). Tests: `testImplementation(project(":core:testing"))`, JUnit 4, assertk.
- `:core:testing` (plugin `chronoflow.jvm.library`): no dependency on `:core:model` in this phase.
- Packages:
  - `net.chronoflow.core.model`: `DataFailure.kt` (with `FieldMessage`), `OutboxKind.kt`, `AccountID.kt`.
  - `net.chronoflow.core.model.contracts`: `TripPlan.generated.kt` (generated), `RawEnum.kt`, `JsonValues.kt`, `CopyLine.kt`, `PlanContracts.kt`, `ProfilePreferences.kt`, `TripContracts.kt`.
  - `net.chronoflow.core.model.api`: `ApiJson.kt`, `ApiErrorEnvelope.kt`, `AuthContracts.kt`, `SessionContracts.kt`, `AccountTripContracts.kt`, `AccountSecurityContracts.kt`, `PasskeyContracts.kt`, `PasskeyManagementContracts.kt`, `TripWizardContracts.kt`, `GuestTripReviewInput.kt`, `ApiCheckOffInput.kt`.
  - `net.chronoflow.core.testing`: `Fixtures.kt`.
- Spotless/ktlint already skips `**/*.generated.kt` (0a `SpotlessConventionPlugin` `targetExclude`). Check it is there; do not add another exclusion. The generated file must still compile with `allWarningsAsErrors`.
- Ported code keeps the Swift **type and property names** (`deviceID`, `clientDataJSON`, `lightOK`), so a reviewer can diff against Swift. Enum cases become PascalCase objects: the Swift case name with its first letter upper-cased (`movementBreak` → `MovementBreak`, `lightOK` → `LightOK`, `value12` → `Value12`, `unknownValue` → `UnknownValue`).

### 4.1 Three serialization rules for every `@Serializable` class in this phase [R]

These rules make one data class serve both decode (tolerant) and encode (exact wire keys). Breaking any of them breaks a fixture test.

1. **Defaults only for omissible keys.** A constructor property has a default **only** when the wire key may be absent and must be omitted when null: TypeScript `key?:`, or a Swift `encodeIfPresent`/synthesized `Optional` on a request DTO. That default is always `= null`. No other default anywhere (not `= emptyList()`, not `= CredentialType.PublicKey`, not `= 60_000`). Reason: kotlinx uses a default instead of failing when a key is missing (iOS throws), and `encodeDefaults = false` would drop a defaulted value from a request.
2. **Required-nullable keys have no default.** `ApiJson.response` (`explicitNulls = false`) then decodes an absent key as `null`, and `ApiJson.request` (`explicitNulls = true`) writes `"key": null`. This is how `review-request.json`'s `"flight_number": null`, `"return": null` and the check-off `"status": null` are produced.
3. **Immutable values.** Properties are `val`; collections are `List`/`Map`. Tests build variants with `copy(...)`.

### 4.2 Table-driven tests in JUnit 4 [R]

JUnit 4 cannot mix parameterized and plain tests in one class, and AGENTS.md "Tests" 5 wants one test case per row, not a loop. So:

- A class annotated `@RunWith(Parameterized::class)` holds **only** parameterized tests. Its `@Parameters(name = "{0}")` factory returns the rows, and the first column is a readable label (the fixture path or the block type), so each row shows as its own case in the Gradle report.
- Where this brief names a parameterized case inside an otherwise plain class, it gets its own parameterized class, named in the task (for example `ErrorFixtureDecodeTests`, `BlockParameterVariantTests`).
- Short fixed lists (the seven `plans/`) are written as one plain method per row, each a one-line call to a private helper (`completePlansS1()` … `completePlansS6Return10N()`), so the Swift name stays the prefix.
- Never loop over rows inside one `@Test`, and never use `Enclosed` (nested classes do not match `make targeted`'s `--tests '*.<Class>'`).

---

## 5. Tasks

Each task is one commit, in this order. Every commit compiles and passes `make quick`. Tick the matching `plan.md` checkbox in the commit named in the task.

### Task 1. JSON foundation: `ApiJson`, raw enums, JSON helpers, `AccountID`

Commit: `feat(model): add api json, raw enum serializer and account id`. Ticks plan.md "**ApiJson.**".

**Files**
- `core/model/build.gradle.kts` (serialization plugin and library, as §4; the catalog already has both aliases from 0a, so `gradle/libs.versions.toml` is not touched).
- `api/ApiJson.kt`:
  ```kotlin
  object ApiJson {
      val response: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
      val request: Json = Json { explicitNulls = true; encodeDefaults = false }
  }
  ```
  `response` decodes every response and encodes values for storage and round-trip tests. `request` encodes every request body. No other `Json { }` instance exists in main code.
- `contracts/RawEnum.kt`:
  ```kotlin
  interface RawEnum { val raw: String }
  open class RawEnumSerializer<T : RawEnum>(serialName: String, private val of: (String) -> T) : KSerializer<T> {
      override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
      override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.raw)
      override fun deserialize(decoder: Decoder): T = of(decoder.decodeString())
  }
  ```
- `contracts/JsonValues.kt`: `val JsonElement?.int: Int?`, the port of `JSONValue.int` (`JSONValue.swift:38-43`): non-null only for a JSON number (not a string) that is a whole, non-negative value that fits `Int`. The six cases (`PasskeyManagementContractsTests.swift:68-75`): `2` → 2; `0` → 0; a missing key (`null` receiver) → null; `1.5` → null; `-1` → null; `JsonPrimitive("2")` (string) → null.
- `AccountID.kt`: `@Serializable(with = AccountID.Serializer::class) @JvmInline value class AccountID private constructor(val rawValue: String)` with `companion fun of(value: String): AccountID` that throws `AccountID.InvalidUUID` (an `IllegalArgumentException`) unless `value.length == 36` **and** it matches `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`; `rawValue` is `value.lowercase()`. Do **not** rely on `java.util.UUID.fromString`: it accepts `1-1-1-1-1`. Serializer: a JSON string, decoded through `of`. (Port of `AccountID.swift`; 4a reuses it for storage paths.)

**Tests** (`core/model/src/test/kotlin/net/chronoflow/core/model/…`)
- `api/ApiJsonTests`, with test-local `@Serializable` classes:
  - `responseIgnoresUnknownKeysAndTreatsAbsentNullableAsNull`: `{"a":"x","extra":1}` decodes into `(a: String, b: String?)` with `b == null`.
  - `responseFailsOnAbsentRequiredKey`: `{}` into `(a: String)` throws `SerializationException`.
  - `requestWritesRequiredNullsAndOmitsOptionalKeys`: `(a: String?, b: String? = null)` with both null encodes to exactly `{"a":null}`; with `b = "y"` to `{"a":null,"b":"y"}`.
- `contracts/RawEnumSerializerTests`: a test-local sealed enum written in the generated shape (5.4.2): known raw → its object; `"future"` → `Unknown("future")`; both round-trip through `ApiJson.response`.
- `contracts/JsonValuesTests.wholeNonNegativeNumbersOnly`: the six cases above.
- `AccountIDTests`: upper-case input is lower-cased; 35 and 37 characters, `1-1-1-1-1` and a non-hex 36-character string throw; JSON round trip.

### Task 2. Fixtures, manifest and loader

Commit: `test(testing): copy ios api fixtures with a checksum manifest`. Ticks plan.md "**Fixtures.**".

**Files**
- `tools/contracts-gen/copy_fixtures.py` (Python 3 stdlib, run with `python3 -I`):
  - Arguments: `--ios-repo` (default: env `IOS_REPO`; if neither is set, exit 2 with `set IOS_REPO or pass --ios-repo`), `--check`.
  - Never derive a repo path from this file's location: worktrees live in `../chronoflow-android-wt/phase-<id>/`, so `ROOT.parent` is wrong there.
  - Pin: first token of `tools/ios-pin.txt` (the full 40-hex SHA); `git show` uses it in full, while the manifest header and the `--check` message print `pin[:8]` (`f391a1fa`).
  - Structure for testability (as 0c): `ROOT = Path(__file__).resolve().parents[2]` is **this** repo (where files are written; it is never used to find iOS); `main(argv=None, read=None, root=ROOT)`, where `read(repo, pin, args) -> bytes` is the only function that runs `git` (`ls-tree` and `show`). Tests pass a fake `read` and a temporary `root`.
  - `SOURCES = [("Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures", ["account", "auth", "errors", "plans", "trips", "wizard"])]`. An entry is either a sub-directory (list files with `git -C <repo> ls-tree -r --name-only <pin> <dir>/<sub>/`, keep `*.json`, write to `fixtures/<sub>/<name>.json`) or, when it ends in `.json`, one file written to the fixtures root (`fixtures/<name>.json`). This phase uses only sub-directories; 1a appends a file row for `intl-cases.json`. Keep it a table and test both entry kinds.
  - Read bytes with `subprocess.check_output(["git", "-C", repo, "show", f"{pin}:{path}"])` (**bytes**, no `text=True`, no newline normalisation). Write to `core/testing/src/main/resources/fixtures/<sub>/<name>.json`.
  - Write `core/testing/src/main/resources/fixtures/fixtures.sha256`, exactly:
    ```
    # ios-pin f391a1fa
    # sha256  resource  ios-source
    1c9c8778…  trips/detail.json  Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/trips/detail.json
    ```
    One line per file, two spaces between columns, sorted by resource path, LF endings, final newline.
  - **The manifest is shared** (1a appends `intl-cases.json` through a `SOURCES` row; 1b's `tools/fixtures/extract_mock_seed.py` writes `mock-seed.json` and adds its own manifest line). So this script **owns only** the files and manifest lines produced from its `SOURCES` table. When writing, it keeps every other manifest line unchanged (re-sorted with its own); it never deletes a file it does not own.
  - `--check`: regenerate its own files in memory; exit 1 with the differing paths if any owned byte or owned manifest line differs, an owned file is missing, or a `*.json` sits under one of its `SOURCES` sub-directories (`fixtures/account/` … `fixtures/wizard/`) without coming from iOS. Files and lines it does not own are ignored here (`FixtureManifestTest` covers them). Otherwise print `Fixtures match iOS f391a1fa: <n> files.` (`n` = owned files, 46 in this phase) and exit 0.
  - Only `git ls-tree` and `git show` may touch the iOS repo.
- 46 JSON files: `account/` 2, `auth/` 5, `errors/` 13, `plans/` 7 (`S1`–`S6`, `S6-RETURN-10N`), `trips/` 3, `wizard/` 16. Do not copy the iOS `*.swift`, `README.md` or `SOURCE.md` files.
- `core/testing/src/main/resources/fixtures/SOURCE.md` (hand-written, short): iOS pin and source directory; that `plans/` and `trips/` were built by iOS from RN `e24872af` at web `afd80dc0` (iOS `plans/SOURCE.md`), and `wizard/` hand-built from web `afd80dc0` (iOS `wizard/SOURCE.md`); all tokens, ids and secrets are artificial; the files are never edited by hand (`make contracts`).
- `core/testing/src/main/kotlin/net/chronoflow/core/testing/Fixtures.kt`:
  ```kotlin
  object Fixtures {
      fun bytes(path: String): ByteArray   // "plans/S1.json"; throws IllegalArgumentException("missing fixture: $path")
      fun text(path: String): String       // bytes(path).decodeToString()
      fun manifest(): List<FixtureEntry>   // parsed fixtures.sha256, comments skipped
  }
  data class FixtureEntry(val sha256: String, val resource: String, val iosSource: String)
  ```
  Resources load through `Fixtures::class.java.getResourceAsStream("/fixtures/$path")`.
- `Makefile` (0b, 0c and 0e edit it in parallel: every change below goes on its own line):
  - `contracts:` replaces 0a's stub (`make contracts: lands in Phase 0d …; exit 1`) with `python3 -I tools/contracts-gen/copy_fixtures.py --ios-repo "$(IOS_REPO)"`.
  - `contracts-check:` (new, `.PHONY`) runs it with `--check`.
  - Directly under 0a's empty definitions, one line each: `GENERATORS += contracts` and `GENERATOR_CHECKS += contracts-check`. Do not edit the `generate` or `generators-check` recipes.
  - `IOS_REPO ?= /Users/rickgout/Sites/chronoflow-ios` and `WEB_REPO ?= /Users/rickgout/Sites/chronoflow-new`, each on its own line, unless the stack top already has them (0b/0c add `IOS_REPO`); then one line `export IOS_REPO WEB_REPO`, so the Python tests that `make quick` runs can read them.
- `Scripts/test_scripts.sh` (0a; run by `make quick`) gains one line after its existing discover lines: `python3 -I -m unittest discover -s "$ROOT/tools/contracts-gen" -t "$ROOT/tools/contracts-gen" -p 'test_*.py'`. `-t` puts the directory on `sys.path`, so the tests `import copy_fixtures` and `import generate_kotlin` directly (file names use underscores; only the directory has a hyphen).

**Tests**
- `core/testing/src/test/kotlin/net/chronoflow/core/testing/FixtureManifestTest`:
  - `everyManifestEntryMatchesItsBytes`: SHA-256 of `Fixtures.bytes(resource)` equals the manifest for all entries; the first comment line is `# ios-pin f391a1fa`.
  - `apiFixtureDirectoriesHoldTheIosSet`: the entries under `account/`, `auth/`, `errors/`, `plans/`, `trips/`, `wizard/` number exactly 2, 5, 13, 7, 3, 16 (46). Do not assert the manifest's total: 1a and 1b add entries at the root.
  - `everyFixtureFileIsInTheManifest`: walk the `fixtures/` resource directory (through the URL of `/fixtures/fixtures.sha256` from the test class loader; it is a `file:` URL in Gradle's JVM tests); every `*.json` is listed (no orphan).
  - Spot hashes: `plans/S1.json` = `cb2f1fa5e47997e1397985a142344139f359dceaf60043413ed00bca0c1947eb`, `plans/S6-RETURN-10N.json` = `8bb0af18c3e4099bcb8359026b50ffa16472ea3bd77f4198c8c17ec4a6f558c7`, `trips/detail.json` = `1c9c87785e608df0d86dbd482568e6a4ce081aac085aed683190f7d98d71d50d`.
- `tools/contracts-gen/test_copy_fixtures.py` (`import copy_fixtures`; feeds a fake `git` through an injected reader function and writes into a `tempfile.TemporaryDirectory`, never the real repo): manifest format and sort order; bytes are written unchanged (a fixture containing `\r\n` and non-ASCII stays byte-identical); `--check` fails on a changed byte, a missing file and an extra `*.json` under `wizard/`; a foreign manifest line (`… mock-seed.json …`) and a foreign root file survive a write and do not fail `--check`.

### Task 3. Error envelope, `FieldMessage`, `DataFailure`, `OutboxKind`

Commit: `feat(model): add error envelope and data failure`.

**Files**
- `api/ApiErrorEnvelope.kt` (port of `APIClient.swift:110-116`):
  ```kotlin
  @Serializable
  data class ApiErrorEnvelope(
      val code: String,                                            // String, not ApiErrorCode: unknown codes must decode
      val message: String,
      val errors: Map<String, String>? = null,
      val args: Map<String, Map<String, JsonElement>>? = null,     // 4b validates "strings or numbers only"
      val retryAfterSeconds: JsonElement? = null,
  )
  ```
- `OutboxKind.kt`: `@Serializable enum class OutboxKind` with `@SerialName`s `check-off`, `settings`, `complete-trip`, `profile`, `data-export`, `security`, `walkthrough` (strict: no unknown case, as Swift).
- `DataFailure.kt` (port of `DataFailure.swift`):
  - `@Serializable data class FieldMessage(val key: String, val arguments: Map<String, JsonPrimitive>)` plus `constructor(key: String) : this(key, emptyMap())`.
    - `interpolationNames`: argument names `n` where `key` contains `":$n"` or `"{{$n}}"`, sorted.
    - `catalogArguments: List<JsonPrimitive>`: `arguments[n]` for each interpolation name, in that order.
    - `interpolationKey: String`: for each name at index `i`, the placeholder type is `lld` when the argument is a non-string JSON number with an integral value that fits `Long`, `f` for other numbers, and `@` otherwise (strings, booleans, null). The placeholder is `%<type>` when there is one name, else `%<i+1>$<type>`. Replace `":$n"` and `"{{$n}}"` with it, name by name in sorted order. The result uses **iOS** placeholder syntax on purpose: it is the lookup key into 0c's `CopyIndex`, whose resources already carry the Android `%1$d` forms. Port the replacement order as is (a name that prefixes another, `:min`/`:minutes`, misbehaves on iOS too).
  - `sealed class DataFailure : Exception(null, null, false, false)` (no stack trace, no suppression: failures are values and some are singletons) with `abstract val status: Int?` and:
    `data object ReviewExpired` (422), `data object NotEnoughCredits` (422), `data object SaveCapacity` (422), `data class ProfileInvalid(val fields: Map<String, FieldMessage>)` (422), `data object Offline` (null), `data class Unauthorized(override val status: Int)`, `data class EmailUnverified(override val status: Int)`, `data class Validation(val fields: Map<String, FieldMessage>, override val status: Int)`, `data class Throttled(val retryAfterSeconds: Double, override val status: Int)`, `data class Conflict(override val status: Int)`, `data class Server(override val status: Int?)`, `data class Unsupported(val kind: OutboxKind)` (null).

**Tests**
- `api/ErrorFixtureDecodeTests` (parameterized, §4.2; one row per `errors/*.json`, 13) `everyErrorFixtureDecodes`: `code` equals the file stem, except `wrong_credentials.json` → `validation_failed`; absent members are null.
- `api/ErrorEnvelopeTests`:
  - `validationFailedKeepsFieldKeysAndArgs`: `errors/validation_failed.json` → `errors["password"] == "Use no more than :max characters."`, `args["password"]["max"]` is the number 255.
  - `throttledCarriesRetryAfter`: `errors/throttled.json` → 37; `wizard/throttled.json` → 42.
  - `wizardErrorsKeepTheirFieldPaths`: `wizard/unknown-field.json` keeps `outbound.legs.0.surprise`; `journey.json` has exactly its 3 keys; `profile.json` has `profile`, `profile.age_band`, `profile.habitual_bed`; `expired.json` has `code == "validation_failed"` and exactly the `errors` key `review_expired`; `credits.json` → `credits == "Not enough credits"`; `capacity.json` → `review == "Try saving again shortly."`.
- `DataFailureTests.statusMatchesSwift`: the status of every case as listed above (`Server(null)` → null, `Server(500)` → 500).
- `api/ContractsTests` (Swift name; this class grows in Tasks 5 and 6): `unknownJSONAndFailureInterpolation` (`JSONValue.swift` round trip becomes: `{"future":{"null":null,"list":[true,3.25,"x"]}}` round-trips as `JsonObject`; `FieldMessage("Use no more than :max characters.", {max: 255}).interpolationKey == "Use no more than %lld characters."`; `FieldMessage("At least {{min}}.", {min: 12}).interpolationKey == "At least %lld."`) and the interpolation half of `inputWireKeysAndOutboxRoundTrips` as `failureInterpolationOrdersArgumentsByName`: `"From :min to {{max}} for :attribute."` with `min 1, max 12, attribute "name"` → `"From %3$lld to %2$lld for %1$@."` and `catalogArguments == ["name", 12, 1]`.

### Task 4. The generator, the generated contracts and their hand-written companions

Commit: `feat(model): generate trip contracts from the pinned web types`. Ticks plan.md "**Generator**" and "**Make targets**".

This is one commit because the generated file calls `BlockParams`, `CopyArgument`, `CopyArgValue` and `ProfilePreferences`, and those refer to generated types.

#### 5.4.1 `tools/contracts-gen/generate_kotlin.py` (Python 3 stdlib, `python3 -I`)

Port `generate.py` line by line; keep its structure and constant names so the two can be diffed.

- **Inputs.** `--web-repo` (default env `WEB_REPO`; neither set → exit 2), `--ios-repo` (default env `IOS_REPO`; used only by the parity test), `--check`. `PIN` = contents of `tools/web-pin.txt`, stripped (must be the 40-hex `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19`). `ROOT = Path(__file__).resolve().parents[2]`.
- **Structure for testability.** `read_sources(repo, pin) -> dict[str, str]` (the only function that runs `git`, `git -C <repo> show <pin>:resources/js/types/App/<path>/index.ts` for each of `PATHS`) and a pure `generate(sources: dict[str, str]) -> str`. Tests call `generate` with synthetic sources.
- **Constants, exactly as iOS** (`generate.py:12-23`): `PATHS`, `RENAMES`, `OVERRIDES` (the three union strings must match **byte for byte**, else `ValueError("hand-written union changed: <Type>.<field>")`), `ROOT_TYPES`, `REQUEST_ROOT_TYPES = ['ApiTripInput']`. The overlay `definitions['ScreeningQuestion'] = "'shift_work'"` is applied before the graph walk. Every type under `Planner/Output` and `Enums` is included.
- **Parsing, exactly as iOS:** `export type (\w+) = (.*?);` with `re.S`; duplicate names → `ValueError("duplicate type: …")`; literal unions `'[^']+'(?:\s*\|\s*'[^']+')*`; struct bodies line by line with `readonly (\w+)(\?)?: (.+),`; anything else → `ValueError` (`unsupported declaration in …`, `unsupported field type: …`, `unsupported type: …`).
- **Type mapping** (`kotlin_type(ts)`). Apply the rules in the iOS order of `swift_type` (`generate.py:67-82`): `Record<string, …>` first, then the exact `boolean | number | string | null` union, then a trailing ` | null`, then `[]`, then the four primitives, then a selected name, else `ValueError("unsupported field type: …")`. The order matters: `Record<string, X | null>` must not be split at its ` | null`. Overrides are checked before `kotlin_type` is called; the `ApiReviewPromptShape` bound rule runs after it, as iOS.

  | TypeScript | Kotlin |
  |---|---|
  | `string` / `number` / `boolean` / `object` | `String` / `Int` / `Boolean` / `JsonObject` |
  | `boolean \| number \| string \| null` | `JsonPrimitive` |
  | `T \| null` | `kotlin_type(T)?` |
  | `T[]` | `List<kotlin_type(T)>` |
  | `Record<string, T>` | `Map<String, kotlin_type(T)>` |
  | a selected type name | its `RENAMES` name |
  | override `BlockView.params` / `CopyArg.value` / `CopyLine.args` | `BlockParams` / `CopyArgValue` / `List<CopyArgument>` |
  | `ApiReviewPromptShape.min`, `.max` (must be `number \| null`, else `ValueError("prompt numeric bound changed: …")`) | `Double?` |

  `number` → `Int` is safe: no fixture holds a number above 2³¹−1 or a fraction outside `min`/`max`/`CopyArg.value` (checked on all 46 files). Optional keys (`key?:`) become nullable with `= null`; no other default is ever emitted (rule 4.1).
- **Names.** Property names use iOS `identifier()` (`generate.py:26-32`) with the Kotlin hard-keyword set instead of Swift's: `as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while` → backticks (in this graph only `ApiTripInput.return` → `` `return` ``). Emit `@SerialName("<wire>")` only when the property name differs from the wire key. Enum case objects: the **Swift** case name (iOS `identifier(literal)` with Swift's `KEYWORDS`, then backticks stripped, so `none` → `none`, `12` → `value12`, `light_ok` → `lightOK`, `return` → `return`) with the first letter upper-cased (`None`, `Value12`, `LightOK`, `Return`; all four occur in iOS `TripPlan.swift`); the literal `unknown` becomes `UnknownValue`. Colliding names → `ValueError("colliding enum cases: …")`; a case named `Unknown`, `Serializer` or `Companion` (they clash with the emitted members) → `ValueError("reserved enum case: …")`. Keep two keyword sets: Swift's (`generate.py:24`) for case names, Kotlin's (above) for property names.
- **Output.** One file, `core/model/src/main/kotlin/net/chronoflow/core/model/contracts/TripPlan.generated.kt`, declarations sorted by TypeScript name (Python `sorted`, as iOS), emitted under their renamed names, 4-space indent, LF, one blank line between declarations, trailing commas in parameter lists, final newline. Header:
  ```kotlin
  // Generated by tools/contracts-gen/generate_kotlin.py from web e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19, with the Phase 19 mobile contract overlay. Do not edit.
  package net.chronoflow.core.model.contracts
  ```
  then only the imports the file uses. No formatter runs; the generator's own output must be deterministic (byte-identical on every run and working directory).
- **`--check`:** compare `generate(read_sources(...))` with the committed file; on difference write a unified diff to stderr and exit 1; else print `Contracts match web e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19; generated fields and enum literals verified.` and exit 0.
- **Make:** `contracts` also runs `python3 -I tools/contracts-gen/generate_kotlin.py --web-repo "$(WEB_REPO)"`; `contracts-check` also runs it with `--check` (both lines in each recipe; the fixture line from Task 2 stays first).

#### 5.4.2 Emitted shapes

String enum (every literal union), for example `CheckStatus = 'done' | 'skipped'`:
```kotlin
@Serializable(with = CheckStatus.Serializer::class)
sealed interface CheckStatus : RawEnum {
    data object Done : CheckStatus {
        override val raw: String = "done"
    }

    data object Skipped : CheckStatus {
        override val raw: String = "skipped"
    }

    data class Unknown(override val raw: String) : CheckStatus

    companion object {
        val known: List<CheckStatus> = listOf(Done, Skipped)

        fun of(raw: String): CheckStatus = when (raw) {
            "done" -> Done
            "skipped" -> Skipped
            else -> Unknown(raw)
        }
    }

    object Serializer : RawEnumSerializer<CheckStatus>("net.chronoflow.core.model.contracts.CheckStatus", { CheckStatus.of(it) })
}
```

Object type, for example `ApiTripInputLeg`:
```kotlin
@Serializable
data class ApiTripInputLeg(
    @SerialName("from_iata") val fromIata: String,
    …
    @SerialName("airport_transfer_min") val airportTransferMin: Int?,
    val stay: ApiTripInputStay,
)
```

`object` body: `typealias MovementBreakParamsView = JsonObject`.

**Surrogate types.** Four types get a custom serializer that delegates to an `internal` generated twin `<Name>Wire` (same properties; `params` is `JsonElement` for `BlockView`), with `descriptor = SerialDescriptor("net.chronoflow.core.model.contracts.<Name>", <Name>Wire.serializer().descriptor)`. They port the iOS hooks in `generate.py:129-160`:

| Type | On decode | On encode |
|---|---|---|
| `BlockView` | `params = BlockParams.decode(wire.type, wire.params, (decoder as JsonDecoder).json)` | `params = value.params.encode((encoder as JsonEncoder).json)` |
| `PlanView` | `prompts = wire.prompts.filter { ProfilePreferences.isSupported(it) }` | as is |
| `ApiTripReview` | `prompts = ProfilePreferences.reviewPrompts(wire.prompts)` | as is |
| `ApiReviewPrompt` | `answer = ProfilePreferences.promptAnswers(wire.answer.filterKeys { it in wire.fields })` | `answer = ProfilePreferences.promptAnswers(value.answer)`; its nullable fields `side` and `deviceMin` are **omitted when null under both `ApiJson` configurations** (iOS `encodeIfPresent`, `generate.py:142`; iOS `TripPlan.swift:392-393`): encode the wire with the encoder's `Json` to a `JsonObject`, drop those two keys when they are `JsonNull`, then `encodeJsonElement` |

The internal `<Name>Wire` twins are not part of the 130-declaration parity count (§5.4.1 `test_matches_ios_generated_swift`). Keep these four in a `CUSTOM` table in the generator, next to `OVERRIDES`. A non-JSON decoder or encoder throws `SerializationException`.

#### 5.4.3 Hand-written companions (package `contracts`)

- `CopyLine.kt` (port of `CopyLine.swift`):
  - `@Serializable(with = …) sealed interface CopyArgValue { data class Text(val value: String); data class Numeric(val value: BigDecimal) }`. Decode a `JsonPrimitive`: `isString` → `Text`; else `Numeric(BigDecimal(content))`. Anything else throws `SerializationException`: a `JsonObject`/`JsonArray`, `JsonNull`, and a boolean (Swift fails to decode `Decimal` from them; in Kotlin `BigDecimal("null")`/`BigDecimal("true")` throw `NumberFormatException`, so check `content.toBigDecimalOrNull()` and throw `SerializationException` yourself). Encode `Text` as a string and `Numeric` as `JsonPrimitive(value)`.
  - `@Serializable(with = …) sealed interface CopyArgument { data class Standard(val value: CopyArg); data class Caffeine(val value: CaffeineCopyArg) }`. Decode the element as a `JsonObject`; **key presence** of `drinks` → `Caffeine`, else `Standard`. Encode the wrapped value.
- `PlanContracts.kt` (port of `PlanContracts.swift`):
  - `sealed interface BlockParams` with `Flight(FlightParamsView)`, `Layover`, `Transfer`, `MovementBreak(JsonObject)`, `ZoneChange`, `Sleep`, `Nap`, `Rest`, `AvoidLight`, `SeekLight`, `LightOK`, `LightNeutral`, `CaffeineServing`, `CaffeineOK`, `CaffeineRoutine`, `NoCaffeine`, `Note(NoteBlockParamsView)`, `CaffeineServingDrinks`, `CaffeineOKDrinks`, `NoCaffeineDrinks`, `Unknown(JsonElement)`, each `data class X(val value: <ParamsView>)`.
  - `fun decode(type: BlockType, element: JsonElement, json: Json): BlockParams` (in the companion): `caffeine_serving` and `caffeine_ok` pick the Drinks variant when `element.jsonObject["amount"] is JsonObject`; `no_caffeine` picks `NoCaffeineDrinks` when `element.jsonObject["exception"]` is a `JsonObject` whose `"amount"` is a `JsonObject`; `BlockType.Unknown` → `Unknown(element)`; a known type whose params fail to decode throws (the whole detail fails, as iOS). `fun encode(json: Json): JsonElement`.
  - `val BlockView.isSupported: Boolean` (false when `type` or `lane` is `Unknown`) and `val PlanView.renderableBlocks: List<BlockView>`.
- `ProfilePreferences.kt`: `object ProfilePreferences`, a verbatim port of `ProfilePreferences.swift` (every string byte-identical): `requestFields` (20 snake keys), `camel`, `bodyClock`, `request`, `promptAnswers` (via the private recursive `cleanLegacyValue`; an answer map is converted with `jsonPrimitive` after cleaning), `reviewPrompts`, `isSupported(ApiReviewPrompt)`, `isSupported(PromptView)`, `readiness`, `containsRetiredValues`, `cleanLegacyJSON(data: ByteArray): ByteArray`.
  - Absent versus `null`: Swift `value["excludeCaffeine"] == nil` means the key is **absent**. A JSON `null` is present. Use `"excludeCaffeine" !in value`.
  - Swift's typed accessors (`.object`, `.string`, `case .array(let …) = …`) return nil for any other JSON type. Port them as `as? JsonObject`, `as? JsonArray`, `(x as? JsonPrimitive)?.takeIf { it.isString }?.content`; **never** `.jsonObject`/`.jsonArray`/`.jsonPrimitive`, which throw. This is live: `account/snapshot.json` has `"invalidDrafts": []` (an array), which `bodyClock` must turn into `{}` (Swift `value["invalidDrafts"]?.object ?? [:]`), not throw on.
  - `cleanLegacyValue` returning nil removes the entry (Swift assigns nil to a dictionary key); in an array it drops the element.
  - `cleanLegacyJSON` returns **the same `ByteArray` instance** when `containsRetiredValues` is false, and also when cleaning changes nothing; otherwise `ApiJson.response.encodeToString(JsonElement.serializer(), cleaned).encodeToByteArray()`.
- `TripContracts.kt` (port of `TripContracts.swift`): typealiases `CheckKey = CheckKeyView`, `CheckStateView = CheckState`, `CheckMarkView = CheckMark`, `TripCardProgress = TripProgress`, `GutterTickView = TickView`; `@Serializable data class TripCheckResult(val checkState: CheckState, val detailVersion: String)`; `@Serializable data class TripListResponse(val trips: List<TripListItem>)` (the `GET trips` envelope; iOS keeps an equivalent private type in `APIDataSource`); and
  ```kotlin
  class TripDetailResponse private constructor(val data: ByteArray, val detail: TripDetail) {
      companion object {
          fun decode(data: ByteArray): TripDetailResponse  // cleaned = ProfilePreferences.cleanLegacyJSON(data); detail = ApiJson.response.decodeFromString(TripDetail.serializer(), cleaned.decodeToString()); data = cleaned
          fun encoding(detail: TripDetail): TripDetailResponse  // decode(ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray())
      }
      // equals/hashCode: data.contentEquals(other.data) && detail == other.detail
  }
  ```
  `data` is what 4a stores in `plan_versions.detail`.

**Tests**
- `tools/contracts-gen/test_generate_kotlin.py` (ports of iOS `Tests/test_contracts_generator.py` plus Android cases; synthetic sources unless stated). Synthetic sources follow the iOS tests: a `dict` with every `PATHS` key, all `''` except `Data/Api`, which defines **every** `ROOT_TYPES` name (default body `{\nreadonly value: string,\n}`), because `include` looks each root up and raises `KeyError` otherwise. Bodies use the web layout: one `readonly …,` field per line.
  - `test_output_is_deterministic`: two `generate` calls, and one from another working directory, are identical.
  - `test_wizard_records_request_keys_and_fractional_bounds` (Swift name): output contains `@SerialName("trip_end_kind") val tripEndKind: String`, `@SerialName("seat_lie_flat") val seatLieFlat: Boolean`, `val min: Double?`, `val max: Double?`, `val answer: Map<String, JsonPrimitive>`, `val airports: Map<String, ApiAirportSuggestion>`.
  - `test_request_graph_encodes_nulls_but_omits_optional_keys` (Swift name): for `RequestChild` with `required: string | null`, `optional?: string`, `optional_nullable?: string | null`: `val required: String?,` has no default; the other two end in `= null`.
  - `test_unknown_enum_fallback`, `test_unknown_literal_becomes_unknown_value`, `test_enum_case_collision_is_rejected`, `test_keyword_field_is_escaped` (`return`).
  - `test_rejects_unsupported_syntax`: a generic other than `Record`, an intersection body, a changed `BlockView.params` union, and a non-`number | null` prompt bound each raise `ValueError`.
  - `test_screening_overlay`: the synthetic `Enums` source declares `export type ScreeningQuestion = 'shift_work' | 'bipolar' | 'pregnancy';` (the overlay only replaces a body; the type is selected because every `Enums` type is); the output's `ScreeningQuestion` has only `ShiftWork` and `Unknown`.
  - `test_planted_change_fails_check`: generate from sources A into a temp file, then run the check against sources B (one field renamed): exit 1 and a diff on stderr. Never modify a real repo.
  - `test_matches_ios_generated_swift` (integration, real repos through `IOS_REPO`/`WEB_REPO`; fails with a clear message if either is unset): the top-level declarations of the generated Kotlin and of iOS `Packages/ChronoFlowKit/Sources/AppCore/Contracts/Generated/TripPlan.swift` at `f391a1fa` have **the same names** (Kotlin top-level declarations other than the `internal` `*Wire` twins), 79 data classes ↔ 79 `public struct`, 50 sealed enums ↔ 50 `public enum`, 1 typealias ↔ 1; every data class has the same property names in the same order as the Swift `public var` lines (backticks stripped); every enum has the same cases (Swift `case x` → Kotlin object `X`, first letter upper-cased, plus `Unknown`).
- `contracts/PlanDecodingTests` (Swift names; port from `PlanDecodingTests.swift`):
  - `completePlans` as seven plain methods (§4.2) `completePlansS1` … `completePlansS6`, `completePlansS6Return10N`, each calling one private helper: decode `plans/<name>.json` into `@Serializable data class Parts(val plan: PlanView, val notices: PlanNoticesView, val timeline: TimelinePresentationView)`, declared once in the test source set as `contracts/PlanParts.kt` (Task 8 reuses it; iOS `PlanDecodingTests.swift:13-17`); a block of type `Flight` exists; `days` is non-empty; encode then decode gives an equal value.
  - `fullEnvelopesAndMissingNullableFields`: `trips/list.json` as `TripListResponse` and `trips/detail.json` as `TripDetailResponse`; first card `id` == `detail.trip.id`; `adaptation` == `detail.plan.summary?.adaptation`; `planInputHash` == `detail.plan.meta.inputHash`; the detail with every JSON `null` removed (recursively) decodes to an equal `detail`; `TripDetailResponse.decode("{}".encodeToByteArray())` throws.
  - `blockParameterVariants` lives in its own parameterized class `contracts/BlockParameterVariantTests` (§4.2; rows = the 20 `(type, params)` pairs of `PlanDecodingTests.swift:46-76`, built with the block template of l.279-283): decode, encode, decode is equal; `caffeine_routine` yields `BlockParams.CaffeineRoutine` with `amountPerDrink == "one"` and `cups == 2`; the three object-`amount` cases yield the Drinks variants.
  - `copyArgumentShapesAndFutureEnums` (l.89-103): three args; the second is `Standard(CopyArg(name = "min", kind = CopyArgKind.Unknown("future"), value = CopyArgValue.Numeric(BigDecimal("1.5")), tz = null))`; the third is `Caffeine` whose first drink is `Drink.Unknown("future")`; round trip; `"future"` as `CheckStatus` is `Unknown("future")`.
  - `unknownLaneOnKnownBlockIsNotRenderable` (l.159-165).
  - `rawBytesAreKeptWhenNothingIsRetired` (Android): `TripDetailResponse.decode(bytes).data` is the very same array as `bytes` for `trips/detail.json` (`assertThat(response.data).isSameInstanceAs(bytes)`), and an unknown block `type` and `lane` added to the JSON decode to `Unknown(raw)` and are excluded from `renderableBlocks`.
- `contracts/ReviewPriceContractTests`: all five Swift cases (`needsAnswersHasNoPriceAndNoSummary`, `priceKeyMayBeAbsent`, `noPlanHasReasonsAndNoPrompts`, `notEnoughCreditsKeepsThePrice`, `okReviewKeepsThePrice`), patching the fixture as a `JsonObject`.
- `contracts/NoHealthDataTests` (this class grows in Tasks 5 and 7): `preferencesDefaultFalseAndOnlyExplicitMovementPreferenceCarriesOver`, `legacyReviewDecodingDiscardsRetiredPrompts`, `tripCacheDropsRetiredValuesAndPromptsWhilePreservingUnknownPlanFields` (all as in `NoHealthDataTests.swift`).
- `api/TripWizardContractTests.promptsAndFractionalBounds` (Swift name, `TripWizardAPITests.swift:82-90`; this is the one `TripWizardContractTests` class, Task 7 adds four methods to it, so `make targeted ONLY=core:model:TripWizardContractTests` matches one class).

### Task 5. Auth, session and account DTOs

Commit: `feat(model): add auth, session and account contracts`.

**Files** (package `api`; wire keys are exactly the Swift `CodingKeys`)

| Kotlin type | Wire keys (`?` = `= null`, omitted when null) | Notes |
|---|---|---|
| `PasswordSignInInput` | `email`, `password`, `device_id`, `device_name`, `draft_token?`, `guest_secret?` | |
| `RegisterInput` | `name`, `email`, `password`, `password_confirmation`, `device_id`, `device_name`, `language?` (`AuthLanguage`), `draft_token?`, `input_revision?` (Int), `review_revision?` (Int), `guest_secret?` | |
| `ResetPasswordInput` | `token`, `email`, `password`, `password_confirmation` | |
| `SignedVerificationInput` | `id`, `hash`, `expires` (**Long**: a Unix time), `signature` | |
| `EmailVerificationStatus` | `email`, `hasVerifiedEmail`, `resendAfterSeconds` | |
| `AuthLanguage` | strict `enum class` `en`, `nl` | |
| `TwoFactorInput(challengeId, proof: Proof)` | `challengeId` + exactly one of `code` / `recovery_code` | `sealed interface Proof { data class Code(val value: String); data class RecoveryCode(val value: String) }`. Custom serializer: decoding with both or neither key throws `SerializationException`. |
| `SignInResult` | `kind` = `"session"` → `Session(SessionResult)`; `"two-factor"` → `TwoFactor(challengeId)` | Custom serializer. An empty `challengeId`, a missing one, or any other `kind` throws. Encodes `{kind:"two-factor",challengeId}` or the session. |
| `AccountPolicies` | `termsVersion`, `privacyVersion`, `acceptedTermsVersion` (String?), `acceptedTermsAt` (String?) | `SessionContracts.kt` |
| `SessionAccount`, `typealias AccountIdentity = SessionAccount` | `id`, `name`, `email`, `hasVerifiedEmail`, `policies`, `walkthroughSeen` | `SessionContracts.kt` |
| `SessionResult` | `kind` (must be `"session"`), `token`, `account`, `tripId` (String?; may be absent, Swift `decodeIfPresent`) | Custom serializer: throws unless `kind == "session"`, `token.isNotBlank()`, and `AccountID.of(account.id)` succeeds. Encodes `kind`, `token`, `account` and `tripId` always, `"tripId": null` included (Swift `encode`, not `encodeIfPresent`): build a `JsonObject` with `JsonNull` and `encodeJsonElement` it, so the null survives `ApiJson.response` (`explicitNulls = false`) too. |
| `AccountSettings` | `bodyClock`, `sleepAnswered`, `preferences`, `reminders`, `planCredits` (all `JsonObject` except `sleepAnswered: Boolean`), `policies` | Serializer applies `ProfilePreferences.bodyClock` on decode **and** encode (iOS init and `encode`). |
| `AccountSnapshot` | `id`, `name`, `email`, `hasVerifiedEmail`, `settings`, `readiness`, `billing`, `security` (`JsonObject`s) | Serializer applies `ProfilePreferences.readiness(readiness, settings.bodyClock)` on decode and encode. |
| `ApiTwoFactorSetup` | `secretKey`, `otpauthUrl` | `AccountSecurityContracts.kt` |
| `ApiRecoveryCodes` | `recoveryCodes: List<String>` | |

**Failures in custom serializers** [R]. Swift throws `DataFailure.server(status: nil)` from `SignInResult`, `SessionResult`, `TwoFactorInput` and `EmptyCredential` decoding (`AuthContracts.swift:137,139,176`, `SessionController.swift:49,55`, `PasskeyContracts.swift:92`), but `APIClient.swift:45-47` turns every decode failure into `.server(status: <HTTP status>)`, so the thrown type is never observed. Every custom serializer in this phase therefore throws `SerializationException` on bad input (4b maps it to `DataFailure.Server(status)`); never throw `DataFailure` from a serializer.

**Secrets never in `toString`** [R] (AGENTS.md "Secrets"; OQ-0d-3). Data-class `toString()` would print secrets into any log that touches the object. Every DTO in this phase that carries a secret overrides `toString()` and prints `***` for these properties when non-null: `password`, `passwordConfirmation`, `token` (session and reset), `guestSecret`, `hash` and `signature` (signed link), the `TwoFactorInput` proof value, `challengeId` (two-factor and passkey), `secretKey`, `otpauthUrl`, `recoveryCodes`. Other properties print normally. The generator does the same for `ApiReviewIdentity.guestSecret`: in **this** commit, add a `REDACTED = {('ApiReviewIdentity', 'guestSecret')}` table to `generate_kotlin.py` (an emitted `override fun toString()` that prints every property, with `***` for a redacted one when non-null), regenerate `TripPlan.generated.kt`, add `test_redacted_field_prints_stars` to `test_generate_kotlin.py` (synthetic root `ApiTripReview` = `{\nreadonly identity: ApiReviewIdentity,\n}` and `ApiReviewIdentity` with `readonly guestSecret: string | null,`; assert the emitted `toString` redacts it), and commit all three together (AGENTS.md "Git": generated files go with their generator change).

**Tests**
- `api/ContractsTests` (Swift names): `signInVariantsAndMalformed`, `proofIsExclusive`, and `inputWireKeysAndOutboxRoundTrips` without the `OutboxChange` part (that half lands with 4d; add a one-line comment saying so): the `RegisterInput` of the Swift test encodes `device_id`, `password_confirmation`, `draft_token`, no `deviceID`, and round-trips.
- `api/AuthContractsTests` (Android): `auth/session.json` and `auth/unverified.json` decode as `SignInResult.Session` (`hasVerifiedEmail` true / false, `tripId` null); `auth/two-factor.json` → `TwoFactor("artificial-two-factor-challenge")`; `auth/verification.json` → `EmailVerificationStatus`; `account/identity.json` → `SessionAccount`; `SessionResult` with account id `"not-a-uuid"` throws; `PasswordSignInInput` without draft and guest values encodes to exactly `{email, password, device_id, device_name}`.
- `api/AccountContractsTests` (Android): `account/snapshot.json` decodes; `settings.bodyClock` keys are a subset of `ProfilePreferences.requestFields.map(camel)` plus `excludeCaffeine`, `flightMovementBreaks`, `invalidDrafts`; `invalidDrafts` is `JsonObject(emptyMap())` (the fixture holds `[]`); it keeps `excludeCaffeine == false`, `flightMovementBreaks == false`; `readiness.variant == "needs-answers"`; encode then decode is equal.
- `contracts/NoHealthDataTests.legacyAccountDecodingDiscardsHealthAnswersAndReadinessTargets` (Swift case, verbatim expectations).
- `api/SecretRedactionTest` (Android): build each secret-bearing DTO with the artificial value `"s3cr3t-value"` in every secret property; `toString()` does not contain `s3cr3t-value` and does contain `***`.

### Task 6. Passkey DTOs

Commit: `feat(model): add passkey contracts`.

**Files** (package `api`)
- `PasskeyContracts.kt` (port of `PasskeyContracts.swift`): `ApiPasskeyChallenge(challengeId, options)`; `ApiPasskeyRequestOptions(challenge, rpId, timeout: Int, userVerification: UserVerification, allowCredentials: List<EmptyCredential>)`; `ApiPasskeyLoginInput` keys `challengeId`, `credential`, `device_id`, `device_name`, `draft_token?`, `guest_secret?`; `ApiPasskeyAssertion(id, rawId, type: CredentialType, response)`; `ApiPasskeyAssertionResponse(clientDataJSON, authenticatorData, signature, userHandle: String? = null)`.
  - Strict enums (no `Unknown`): `UserVerification { @SerialName("required") Required }`, `CredentialType { @SerialName("public-key") PublicKey }`. Another value throws.
  - `EmptyCredential`: a type with a private constructor whose serializer **always throws** on decode, so `allowCredentials` decodes only when empty (iOS uninhabited enum).
- `PasskeyManagementContracts.kt` (port of `PasskeyManagementContracts.swift`, keep its 3-line header comment about Phase 20 being unmerged): `ApiPasskey(id: Long, name, authenticator: String?, createdAt, lastUsedAt: String?)` (**`id` is Long**: the route allows 18 digits, `[1-9][0-9]{0,17}`), `ApiPasskeyList`, `ApiPasskeyRegistrationChallenge`, `ApiPasskeyCreationOptions(challenge, timeout, rp, user, pubKeyCredParams, authenticatorSelection, attestation, excludeCredentials)`, `ApiPasskeyRelyingParty(id, name)`, `ApiPasskeyUserEntity(id, name, displayName)`, `ApiPasskeyCredentialParameters(type: CredentialType, alg: Int)`, `ApiPasskeyAuthenticatorSelection(requireResidentKey: Boolean, userVerification: String, residentKey: String)`, `ApiPasskeyCredentialDescriptor(type: CredentialType, id: String)`, `ApiPasskeyAttestation(id, rawId, type: CredentialType, response)`, `ApiPasskeyAttestationResponse(clientDataJSON, attestationObject, transports: List<String>? = null)`, `ApiPasskeyRegistrationInput(challengeId, name, credential)`. Swift's convenience defaults (`timeout = 60_000`, `alg -7/-257`, `name ?: id`, `displayName ?: name`) are **not** ported (rule 4.1); every key is required on decode.

**Tests**
- `api/ContractsTests` (Swift names): `passkeyNullAndEncoding`, `nullablePasskeyUserHandleAndDiscriminators`.
- `api/PasskeyManagementContractsTests` (Swift names): `registrationChallengeDecodesTheDocumentedOptions` (including: the options without `timeout` and the rest throw; the Swift test compares with `ApiPasskeyRelyingParty(id:)` and `ApiPasskeyAuthenticatorSelection()`, which rely on the unported convenience defaults, so write the expected values out: `ApiPasskeyRelyingParty("app.chronoflow.net", "app.chronoflow.net")`, `ApiPasskeyAuthenticatorSelection(true, "required", "required")`, `ApiPasskeyCredentialDescriptor(CredentialType.PublicKey, "AQID")`), `registrationInputEncodesTheDocumentedBodyWithoutAPassword` (top-level keys exactly `challengeId`, `name`, `credential`; credential keys exactly `id`, `rawId`, `type`, `response`; `transports` absent when null), `passkeyListItemDecodesTheDocumentedFields`, `securitySummaryCountIsOptionalAndWholeNumbersOnly` (via `JsonValues.int`).
- `api/PasskeyFixtureTests.passkeyOptionsFixtureDecodes` (Android): `auth/passkey-options.json` → 64-character `challengeId`, `rpId == "app.chronoflow.net"`, `timeout == 60000`, empty `allowCredentials`.
- Extend `SecretRedactionTest` with `ApiPasskeyLoginInput`.

### Task 7. Trip-wizard, guest and check-off request DTOs

Commit: `feat(model): add trip wizard and check-off request contracts`. Ticks plan.md "**Hand-written DTOs**".

**Files** (package `api`)
- `TripWizardContracts.kt` (port of `TripWizardContracts.swift`):
  - `typealias ApiTripPrice = ApiReviewPrice`, `typealias ApiTripItinerary = ApiReviewItinerary`.
  - `ApiTripReviewInput(input: ApiTripInput, identity: ApiReviewIdentity?, profile: JsonObject?)`, **encode-only** custom serializer (decode throws). Body = the `ApiTripInput` object encoded with the encoder's `Json` (explicit nulls), plus `draft_token` = `identity?.token` (explicit `null` when absent), `input_revision` = `identity?.inputRevision ?: 0`, `review_revision` = `identity?.reviewRevision` (explicit `null`), `source_revision` only when `identity?.sourceRevision != null`, `profile` only when `profile != null`, through `ProfilePreferences.request(profile)`. No `identity`, `input` or `guest_secret` key, ever.
  - `@Serializable data class ApiTripSaveInput(@SerialName("draft_token") val draftToken: String, @SerialName("input_revision") val inputRevision: Int, @SerialName("review_revision") val reviewRevision: Int)` (Swift names) with `companion fun from(identity: ApiReviewIdentity): ApiTripSaveInput` that throws `DataFailure.ReviewExpired` when `token` is empty, `inputRevision <= 0`, or `reviewRevision` is null or `<= 0`.
- `GuestTripReviewInput.kt` (port of `GuestTripReviewInput.swift`): `ApiGuestTripReviewInput(input, identity: ApiReviewIdentity?, profile: JsonObject, deviceID: String, guestSecret: String?)`, encode-only. Keys exactly: `outbound`, `trip_end_kind` (always `"one_way"`), `preparation_days`, `fast_delay_opt_in`, `latest_wake`, `feasible_night_bed`, `feasible_night_wake`, `pre_departure_busy_answered`, `pre_departure_busy_intervals`, `draft_token` (explicit null), `input_revision` (`?: 0`), `review_revision` (explicit null), `profile` (`ProfilePreferences.request`), `device_id`, and `guest_secret` only when non-null. Never `return`, `trip_end_nights`, `trip_end_check_out_date`, `override`, `source_revision`.
- `ApiCheckOffInput.kt`: `@Serializable data class ApiCheckOffInput(planInputHash, type: BlockType, localDate, ordinal: Int, status: CheckStatus?)`, keys `plan_input_hash`, `type`, `local_date`, `ordinal`, `status`; `status` has **no default**, so `ApiJson.request` writes `"status": null` (the server requires the key, `03 §5.3` #39). Android formalisation of iOS's inline `Body` (`APIDataSource.swift:86-105`).

**Tests**
- `api/TripWizardContractTests` (the class from Task 4; Android names; the encoding half of `TripWizardAPITests.wireContractsRequestsAndResponses`):
  - `reviewRequestMatchesFixture`: `ApiTripReviewInput(input = decode(wizard/input.json), identity = null, profile = review-request.json["profile"])` encoded with `ApiJson.request` equals `wizard/review-request.json` as a `JsonObject` (same keys, same values, including every explicit `null`).
  - `sourceRevisionOnlyWhenPresent`: an identity with `sourceRevision = "a".repeat(64)` adds `source_revision`; with `null` the key is absent; `profile = null` omits `profile`.
  - `saveRequestMatchesFixture`: `ApiTripSaveInput.from(review.json identity)` encodes to exactly `wizard/save-request.json`; `from` throws `ReviewExpired` for an empty token, `inputRevision = 0` and `reviewRevision = null`.
  - `snapshotRoundTripsTheInput`: `wizard/snapshot.json` decodes; its `input` equals `wizard/input.json`; `input.outbound.legs[0].stay.bedAvailableMode == null`; `airports["AMS"].timezone == "Europe/Amsterdam"`.
- `api/GuestTripReviewContractTests` (Swift names): `firstCallMatchesGuestRequestRules`, `subsequentIdentityAndSecretAreFlattened` (as in `GuestTripReviewAPITests.swift:8-37`; port the helper `guestReviewInput()` of l.94-98: `input` = `wizard/input.json`, `profile` and `deviceID` from `wizard/guest-review-request.json`). The first test compares the whole body with `wizard/guest-review-request.json` as a `JsonObject`, which is plan.md's guest key-set check.
- `api/CheckOffInputTests.statusIsAlwaysOnTheWire`: `status = null` encodes `"status":null` and exactly five keys; `CheckStatus.Done` encodes `"done"`; `BlockType.Unknown("future_step")` encodes `"future_step"`.
- `contracts/NoHealthDataTests.reviewEncodingDropsRetiredFieldsEvenAfterMutation` (Swift case).
- Extend `SecretRedactionTest` with `ApiGuestTripReviewInput`.

### Task 8. Every fixture decodes

Commit: `test(model): decode every api fixture`. Ticks plan.md "**Decode and encode tests**".

- `contracts/FixtureDecodeTests` (Android), a parameterized class (§4.2) over an `internal val API_FIXTURE_DECODERS: Map<String, (ByteArray) -> Any>` table (in the test source set), one test case per fixture, `decodes`:

  | Fixture | Decodes as |
  |---|---|
  | `account/identity.json` | `SessionAccount` |
  | `account/snapshot.json` | `AccountSnapshot` |
  | `auth/passkey-options.json` | `ApiPasskeyChallenge` |
  | `auth/session.json`, `auth/two-factor.json`, `auth/unverified.json` | `SignInResult` |
  | `auth/verification.json` | `EmailVerificationStatus` |
  | `errors/*.json` (13), `wizard/{capacity,credits,expired,journey,profile,throttled,unknown-field}.json` | `ApiErrorEnvelope` |
  | `plans/*.json` (7) | `Parts` (Task 4's `contracts/PlanParts.kt`) |
  | `trips/checks.json` | `TripCheckResult` |
  | `trips/detail.json` | `TripDetailResponse.decode` |
  | `trips/list.json` | `TripListResponse` |
  | `wizard/airports.json` | `ApiAirportSearchResults` |
  | `wizard/input.json`, `wizard/review-request.json`, `wizard/guest-review-request.json` | `ApiTripInput` (`guest-review-request.json` lacks `return`, `trip_end_nights`, `trip_end_check_out_date` and `override`. The first three are `… \| null` in TypeScript (web `Data/Api/index.ts:238-241`), so rule 4.1 decodes them as `null`; `override: ApiTripInputOverride` (l.244) is required and non-null, so decoding **must fail** with a `SerializationException` (`MissingFieldException`). This row's lambda asserts that failure itself (`assertFailure { … }.isInstanceOf<SerializationException>()`) and returns `Unit`; it proves required keys are enforced.) |
  | `wizard/needs-answers.json`, `wizard/review.json` | `ApiTripReview` |
  | `wizard/save-request.json` | `ApiTripSaveInput` |
  | `wizard/save.json` | `ApiTripSaveResult` |
  | `wizard/snapshot.json` | `ApiTripInputSnapshot` |

- `contracts/FixtureDecoderCoverageTest.everyApiFixtureHasADecoder` (plain class): the table's keys equal the set of `Fixtures.manifest()` resources under `account/`, `auth/`, `errors/`, `plans/`, `trips/`, `wizard/` (46). An API fixture added later without a decoder fails here; the root fixtures that 1a (`intl-cases.json`) and 1b (`mock-seed.json`) add are not API payloads and are out of this test's scope.

---

## 6. Acceptance criteria

All commands from the worktree root, with `ANDROID_HOME` set (AGENTS.md "Commands").

1. `make contracts-check` exits 0 and prints both lines:
   `Contracts match web e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19; generated fields and enum literals verified.` and `Fixtures match iOS f391a1fa: 46 files.`
2. `make contracts` followed by `git status --porcelain` shows no change (the committed output is fresh and deterministic).
3. Prove the checks once, then revert, and report the exact output: (a) add a blank line to `TripPlan.generated.kt` → `make contracts-check` exits 1 with a diff; (b) change one byte of `fixtures/errors/conflict.json` → `make contracts-check` exits 1 naming that file and `./gradlew :core:testing:test --tests '*FixtureManifestTest'` fails.
4. `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new python3 -I -m unittest discover -s tools/contracts-gen -t tools/contracts-gen -p 'test_*.py'` passes (outside `make` the two variables are not exported, and the integration test fails by design without them), including `test_matches_ios_generated_swift` (130 declarations: 79 / 50 / 1).
5. `./gradlew :core:model:test :core:testing:test` passes. Every JVM test is under 1 s (read `build/test-results/quick/timings.json`, written by 0a's `run-tier.sh` during `make quick`); list the five slowest in your hand-off.
6. `make targeted ONLY="core:model:PlanDecodingTests core:model:BlockParameterVariantTests core:model:FixtureDecodeTests core:model:FixtureDecoderCoverageTest core:model:ContractsTests core:model:NoHealthDataTests core:model:TripWizardContractTests core:model:GuestTripReviewContractTests core:testing:FixtureManifestTest"` passes (module paths without the leading colon, 0a `Scripts/targeted.py`).
7. `make quick` passes, runs the `tools/contracts-gen` Python tests through `Scripts/test_scripts.sh`, and stays under 60 s warm; `make generators-check` passes and runs `contracts-check`; `make generate` runs `contracts`; `./gradlew :konsist:test` passes (no `android.*` import in `:core:model` or `:core:testing`).
8. `make check` passes (standard acceptance; no emulator work is added by this phase).
9. The six 0d checkboxes in `plan.md` are ticked, each in the commit named in its task. Commits are the eight above, Conventional Commits, no trailers.
10. Nothing was written inside `/Users/rickgout/Sites/chronoflow-ios` or `/Users/rickgout/Sites/chronoflow-new`: `git -C <repo> status --porcelain` is unchanged before and after the phase (record both outputs).

No screenshots and no performance numbers are produced by this phase.

---

## 7. Review checklist (Opus reviewer)

**Source safety**
- [ ] `generate_kotlin.py` and `copy_fixtures.py` call only `git ls-tree` and `git show <pin>:<path>` on the source repos. No `checkout`, `fetch`, `stash`, `worktree`, no file reads from the source working trees.
- [ ] No default repo path is derived from `__file__` (`ROOT.parent` breaks in `../chronoflow-android-wt/`).
- [ ] `copy_fixtures.py` keeps foreign manifest lines and files (1a, 1b) on write and ignores them in `--check`; `FixtureManifestTest` asserts per-directory counts, not a manifest total.
- [ ] Makefile changes are only: the `contracts`/`contracts-check` recipes, `GENERATORS += contracts`, `GENERATOR_CHECKS += contracts-check`, the `IOS_REPO`/`WEB_REPO` lines and `export`; plus one line in `Scripts/test_scripts.sh`.
- [ ] The pin comes from `tools/web-pin.txt` / `tools/ios-pin.txt`, not a literal.

**Generator parity**
- [ ] `PATHS`, `RENAMES`, `OVERRIDES`, `ROOT_TYPES`, `REQUEST_ROOT_TYPES`, the overlay and the graph walk match `generate.py` line for line; the regexes are identical.
- [ ] `test_matches_ios_generated_swift` really reads both repos and compares names, property order and enum cases (not just counts).
- [ ] Unsupported TypeScript raises `ValueError`; nothing is silently mapped to `JsonElement`/`Any`.
- [ ] The generated file is byte-stable: run `make contracts` twice.

**Serialization rules (4.1)**
- [ ] `grep -nE '= (emptyList|emptyMap|listOf|mapOf|[0-9]|"|CredentialType|true|false)' core/model/src/main` finds nothing in `@Serializable` constructors; every `= null` sits on an omissible key.
- [ ] `review-request.json` equality holds with every explicit `null`; `ApiCheckOffInput` writes `"status":null`.
- [ ] `ApiTripSaveInput`, `ApiPasskeyCreationOptions`, `ApiPasskeyRequestOptions` decode fails on a missing key.
- [ ] Only `ApiJson` creates `Json` instances in main code.

**Behaviour copied from Swift**
- [ ] `CopyArgument` splits on the **presence** of `drinks`, not its value.
- [ ] `BlockParams` Drinks detection: `amount` is an object for `caffeine_serving`/`caffeine_ok`; `exception.amount` is an object for `no_caffeine`.
- [ ] `ProfilePreferences` strings are byte-identical to Swift (diff the string literals); absent-versus-null is handled with `in`, not `== null`.
- [ ] `cleanLegacyJSON` returns the same array instance for clean input (`rawBytesAreKeptWhenNothingIsRetired`).
- [ ] `SessionResult` validates `kind`, a non-blank token and a 36-character UUID (`AccountID` does not use `UUID.fromString`).
- [ ] `FieldMessage.interpolationKey` produces iOS placeholders (`%lld`, `%@`, `%3$lld`), names sorted.
- [ ] `DataFailure` extends `Exception(null, null, false, false)`; `status` matches the Swift table; `Unsupported` carries `OutboxKind`.
- [ ] Long, not Int: `SignedVerificationInput.expires`, `ApiPasskey.id`.

**Hygiene**
- [ ] No secret appears in any `toString()` (`SecretRedactionTest` covers every secret-bearing DTO listed in Task 5, plus `ApiReviewIdentity`).
- [ ] No `android.*`, Compose or coroutine import in `:core:model`; no `runCatching`/`catch (e: Exception)`.
- [ ] Comments only state a why (the Phase 20 header, the 4d note in `ContractsTests`, the `UUID.fromString` note).
- [ ] Tests: JVM/Python only; Swift names kept where ported (Appendix B); table rows are separate test cases per §4.2 (no loop inside one `@Test`, no `Enclosed`, no plain test inside a `Parameterized` class); no `Thread.sleep`; each < 1 s.
- [ ] Enum case names come from the Swift identifier rules (`None`, `Value12`, `LightOK`, `Return`), and the reserved-name guard exists.
- [ ] Fixtures are byte copies (compare three hashes against `git show f391a1fa:… | shasum -a 256`).
- [ ] Generated file excluded from Spotless, yet compiles with `allWarningsAsErrors`.

**Typical mistakes to look for:** giving `= null` to required-nullable fields (drops explicit nulls from requests); Kotlin `enum class` with `@SerialName` for generated enums (fails on unknown values; iOS keeps them); `ignoreUnknownKeys` on the request `Json` hiding a misspelt key in tests; `decodeFromString` on a `String` built with the platform charset; comparing JSON as strings instead of `JsonObject`s (key order differs); `Map<String, JsonPrimitive>` failing on a JSON `null` value (it must decode to `JsonNull`).

---

## 8. Open questions (proceed on the default; add each to plan.md "Open questions for the user" in the commit that applies it)

| # | Question | Default |
|---|---|---|
| OQ-0d-1 | `number` → `Int` (32-bit) for generated types, while Swift `Int` is 64-bit | `Int`; no fixture value exceeds 2³¹−1. Hand-written exceptions use `Long`: `SignedVerificationInput.expires`, `ApiPasskey.id`. |
| OQ-0d-2 | Enum shape: sealed interface with `data object`s and `Unknown(raw)`, or an `enum class Known` nested in a sealed interface (fewer classes, more verbose call sites) | Sealed interface with `data object`s (reads like Swift `.flight`; about 400 small classes, which R8 shrinks). Revisit if `make size` warns. |
| OQ-0d-3 | Redact secrets in `toString()` (Android-only; iOS has no equivalent) | Yes, `***`, for the fields listed in Task 5. |
| OQ-0d-4 | A18 names a Gradle `contractsCheck` task | Use the `make contracts-check` target only; no Gradle task. |
| OQ-0d-5 | `plans/` and `trips/` fixtures were built by iOS at web `afd80dc0`, the generator is pinned at `e36368ec` | Copy as is (iOS decodes them under the same pin); record both pins in `fixtures/SOURCE.md`. |
| OQ-0d-6 | `GET trips` envelope type | Hand-written `TripListResponse` in `TripContracts.kt` (iOS keeps an equivalent private). |
| OQ-0d-7 | Type the `ApiTripInput` string fields (`trip_end_kind`, `seat_window`, `bed_available_mode`, `transfer_mode`) as enums | No: they are `string` in TypeScript and `String` on iOS. |
| OQ-0d-8 | plan.md's 0d "Hand-written DTOs" box lists "the settings sections"; iOS keeps `SettingsSection` beside `OutboxChange`, and the 4b brief already owns it | `SettingsSection` and the PATCH `JsonObject` diffs land in 4b; 0d ticks the box once auth, account, passkey, wizard-input and error-envelope DTOs exist, and the commit body says where settings went. |
| OQ-0d-9 | `fixtures.sha256` is shared with 1a (`intl-cases.json`) and 1b (`mock-seed.json`) | One manifest; each script owns its own lines (Task 2); `FixtureManifestTest` checks every line. |

---

## 9. Hand-off to later phases

- **1a** (brief already written): its `PlanDecoder(json: Json = ApiJson)` becomes `json: Json = ApiJson.response`; every other `ApiJson` decode or encode in 1a means `ApiJson.response`. `TripDetailResponse` (raw bytes plus cleaning) is in `contracts/TripContracts.kt`; build on it. Add `intl-cases.json` by appending a `SOURCES` entry in `tools/contracts-gen/copy_fixtures.py` (`("Packages/ChronoFlowKit/Tests/PlanCoreTests/Fixtures", ["intl-cases.json"])`; Task 2 already supports a row that names a file at the fixtures root, so no script change) and rerunning `make contracts`, so `FixtureManifestTest` covers it. `FixtureDecoderCoverageTest` only covers the six API directories, so no decoder row is needed.
- **1b:** its `ApiJson.encodeToString(…)` / `ApiJson.decodeFromString(…)` calls (large30/60, `MockTripTemplates`, `shifted`) mean `ApiJson.response`. `extract_mock_seed.py` adds its own `mock-seed.json` line to `fixtures.sha256`; `copy_fixtures.py` keeps it (Task 2). `CopyArgValue.Numeric` holds a `BigDecimal` built from the raw JSON number text.
- **4b:** maps `ApiErrorEnvelope` + status to `DataFailure` (`APIClient.swift:118-168`); adds `source_revision` to the `PUT trips/{id}` body; builds the inline bodies; decodes `GET trips` with `TripListResponse`; maps any decode failure of these types to `DataFailure.Server(status)` (iOS `APIClient.swift:45-47`); catch `IllegalArgumentException`, the supertype of `SerializationException`, which also covers kotlinx's `jsonObject`-style accessors and `AccountID.InvalidUUID`.
- **4d:** adds `OutboxChange` and `CheckOffChange` beside `OutboxKind`, and the outbox half of `ContractsTests.inputWireKeysAndOutboxRoundTrips`.

---

## Appendix A. Facts verified for this brief [F]

- iOS `TripPlan.swift` at `f391a1fa`: header `// Generated by Tools/contracts-gen from web e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19, with the Phase 19 mobile contract overlay.`; 79 `public struct`, 50 `public enum`, 1 `public typealias` (`MovementBreakParamsView = JSONObject`).
- The generated graph has **no** optional (`?:`) keys; the only Kotlin-keyword field is `ApiTripInput.return`; the only `object` type is `MovementBreakParamsView`; `Record` appears in `ApiReviewPrompt.answer` and `ApiTripInputSnapshot.airports` (and in `ApiError`, which is not in the graph).
- `ApiTripInput` and its children have only required keys; nullable ones are sent as explicit `null` (`wizard/review-request.json`). Of the keys missing from `wizard/guest-review-request.json`, only `override` is non-null (web `Data/Api/index.ts:236-250`).
- iOS `TripPlan.swift` uses `encodeIfPresent` only for `ApiReviewPrompt.side` and `.deviceMin` (l.392-393).
- Fixtures: 46 JSON files (account 2, auth 5, errors 13, plans 7, trips 3, wizard 16); no integer above 2³¹−1 and no fractional number anywhere in them.
- `BlockType` has 17 literals; `BlockParams` has 20 variants (17 + three Drinks shapes) plus `unknown`.
- `ScreeningQuestion` at the pin is `'shift_work' | 'bipolar' | 'eye_disease' | 'pregnancy' | 'caffeine_ruled_out' | 'vte'`; the overlay narrows it to `'shift_work'`.

## Appendix B. iOS → Kotlin test map

| iOS test (`Tests/DataAccessTests/…`) | Kotlin | Task |
|---|---|---|
| `PlanDecodingTests.completePlans`, `fullEnvelopesAndMissingNullableFields`, `blockParameterVariants`, `copyArgumentShapesAndFutureEnums`, `unknownLaneOnKnownBlockIsNotRenderable` | `contracts/PlanDecodingTests`, same names | 4 |
| `PlanDecodingTests.rawAPIBytesAndUnknownCheckSurviveStorageAndReconciliation` | split: decode half → `rawBytesAreKeptWhenNothingIsRetired` (4); storage half → 4a/4d | 4 |
| `PlanDecodingTests.mockTripsRebaseEveryTimestampAndDate` | 4c | — |
| `ReviewPriceContractTests.*` (5) | `contracts/ReviewPriceContractTests`, same names | 4 |
| `TripWizardAPITests.promptsAndFractionalBounds` | `api/TripWizardContractTests`, same name | 4 |
| `TripWizardAPITests.wireContractsRequestsAndResponses` | encoding half → `api/TripWizardContractTests` (4 methods); HTTP half → 4b | 7 |
| `ContractsTests.unknownJSONAndFailureInterpolation` | `api/ContractsTests`, same name | 3 |
| `ContractsTests.signInVariantsAndMalformed`, `proofIsExclusive`, `inputWireKeysAndOutboxRoundTrips` (without outbox) | `api/ContractsTests`, same names | 5 |
| `ContractsTests.passkeyNullAndEncoding`, `nullablePasskeyUserHandleAndDiscriminators` | `api/ContractsTests`, same names | 6 |
| `PasskeyManagementContractsTests.*` (4) | `api/PasskeyManagementContractsTests`, same names | 6 |
| `GuestTripReviewAPITests.firstCallMatchesGuestRequestRules`, `subsequentIdentityAndSecretAreFlattened` | `api/GuestTripReviewContractTests`, same names | 7 |
| `NoHealthDataTests.preferencesDefaultFalse…`, `legacyReviewDecoding…`, `tripCacheDrops…` | `contracts/NoHealthDataTests`, same names | 4 |
| `NoHealthDataTests.legacyAccountDecoding…` | same class | 5 |
| `NoHealthDataTests.reviewEncodingDropsRetiredFieldsEvenAfterMutation` | same class | 7 |
| `NoHealthDataTests.patch*` (2) | 4b (`SettingsPatchValidation`) | — |
| `NoHealthDataTests.mockRejects…`, `readinessOnlyAgeAndShiftWork…` | 4c | — |
| `Tests/test_contracts_generator.py` (3) | `tools/contracts-gen/test_generate_kotlin.py` (2 same names; the formatter test becomes `test_output_is_deterministic`) | 4 |
