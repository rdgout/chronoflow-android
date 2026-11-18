# Phase 4b verification

2026-11-18, branch `feat/phase-4b-api-client`. Tasks 1–10 are complete in order, each in a signed commit. The two Konsist guards formerly listed as Task 11 belong to Task 10, following the requested ten-task sequence. All five phase checkboxes are complete. Defaults are recorded as OQ-A132–OQ-A147; OQ-A68 remains unused.

## Brief refresh

The initial refresh commit is `c9fe81c` (`docs(phases): refresh the 4b brief against 4a`). It:

- Replaces wave-4 worktrees with the single lane from Phase 4a and updates the plan references.
- Uses `libs.okhttp.mockwebserver3`, the concrete `Fixtures.kt` path and the existing model dependencies.
- Retains the scaffold's existing app dependency on `:core:network`, without duplicating it as a test dependency.
- Uses `TripDetailResponse.detail.trip` and preserves its legacy-health sanitizer. Clean payloads retain their exact bytes.
- Uses the landed `ProfilePreferences.requestFields` set, the existing perf build types and current test harness.
- Folds the Konsist guards into Task 10 and aligns verification with targeted tests per task and one final quick run.
- Starts new OQ rows at OQ-A132.

Later clarifications document the temporary abstract data source during Tasks 6–7 (concrete since Task 8), refresh the line references after adding OQs, and disable injected HTTP/proxy authenticators to preserve the single-request/public-route contract. No earlier phase's runtime behavior changed.

## Tests

All selectors below were run through `make targeted ONLY="…"`. Times marked “Gradle” are the wall times reported by Gradle; the other times are the complete targeted tier's `wallSeconds`.

| Task | Selectors | Result | Wall time |
|---|---|---|---|
| 1 | `core:model:DataSourceContractTests core:model:OutboxChangeTests core:model:SecretRedactionTest` | Pass | 1 s Gradle |
| 2 | `core:model:TripWizardJourneyPathTests core:model:TripWizardErrorsTests core:model:SettingsPatchValidationTests core:model:NoHealthDataTests` | Pass | 2 s Gradle |
| 3 | `core:network:DataSourceSelectionTests core:network:ApiEndpointTests core:network:ApiOriginTests` | Pass | 2 s Gradle |
| 4 | `core:network:ApiErrorMapperTests core:network:ApiErrorFixtureMappingTests core:network:ApiErrorMapperMalformedTests core:network:ApiErrorMapperRetryAfterTests` | 37 pass | 1.59 s |
| 5 | `core:network:ApiClientTests core:network:ApiClientErrorTests core:network:ApiClientRedirectTests core:network:ApiClientTransportTests` | Pass | 1 s Gradle |
| 5 body cancellation | `core:network:ApiClientTests#explicitCancellationInterruptsTheBodyRead` | 1 pass | 1.72 s |
| 6 | `core:network:ApiDataSourceTests core:network:NoHealthDataTests core:network:GuestClaimWireTests core:network:ApiClientTests#registrationValidationEnvelopePreservesVisibleFieldMessages core:network:ApiClientTests#malformedSuccessEmptyObject core:network:ApiClientTests#malformedSuccessHtml core:network:ApiClientTests#malformedSuccessSessionWithoutAccount` | 19 pass | 1.73 s |
| 7 | `core:network:AccountSecurityAPITests core:network:PasskeyManagementAPITests core:network:SettingsDeleteAPITests` | 6 pass | 2.02 s |
| 8 | `core:network:TripWizardAPITests core:network:TripWizardTypedErrorTests core:network:GuestTripReviewAPITests` | 26 pass | 2.23 s |
| 9 | `core:network:EndpointContractTest core:network:EndpointContractCoverageTest` | 43 pass | 2.10 s |
| 10 | `app:DebugApiConfigTest app:ReleaseApiConfigTest konsist:KonsistRulesTest` | All pass; Konsist 65 cases | Debug 3 s Gradle; release 1 s Gradle; Konsist 2.87 s |
| Final audit | `core:network:ApiClientTests#injectedAuthenticatorsCannotRetryPublicCalls core:model:SecretRedactionTest#securityRemovePasskey` | Both pass | Network 2 s Gradle; model 1.71 s |

Initial compilation failures in Tasks 1, 5 and 6 were resolved: imports/string escaping in test scaffolding, OkHttp builder interceptor method syntax, and test-only imports of the plan extension/health-test class. Each fix ran its affected selectors. No test assertion failed and no test run hung. Formatting issues were resolved with `make format`.

`make quick` ran **once**, successfully, in **32.35 s total** (`/usr/bin/time -p`):

- Main tier: 1,474 passing tests, 15.92 s, including all 179 network tests.
- Build logic: 26 passing tests, 0.96 s.
- Tooling: four passing Python suites (107, 25, 49 and 9 tests).
- Serial timing tier: six passing declared timing-budget cases, 2.00 s.

The final main-tier evidence confirms 42 endpoint cases covering 25/25 methods, 13/13 error fixtures, ten error cases, eight redirect cases, four transport modes, seven typed wizard errors and six guest-claim cases. The five slowest phase-related cases in this run were:

| Test | Seconds |
|---|---:|
| `ApiClientTests.registrationValidationEnvelopePreservesVisibleFieldMessages` | 0.528 |
| `ApiDataSourceTests.fetchTripsRejectsEmptyIds` | 0.420 |
| `GuestTripReviewAPITests.mapsGuestFailuresBare429` | 0.409 |
| `SettingsDeleteAPITests.deletionSendsPasswordUnderTheBackendFieldAndPreservesBillingFailure` | 0.397 |
| `AccountSecurityAPITests.everySecurityRouteUsesTheRealMethodAndBody` | 0.386 |

## Build configuration

Each command below used `./gradlew --console=plain`. Configuration generation checks passed, including the intentional invalid-value failures:

| Arguments | Result | Wall time |
|---|---|---:|
| `:app:generateDebugBuildConfig -Pcf.dataSource=api -Pcf.apiBaseUrl=http://10.0.2.2:8000/v1` | `api`, local URL | 0.87 s |
| `:app:generateDebugBuildConfig -Pcf.dataSource=other` | Fails: `cf.dataSource must be mock or api, was other` | 0.88 s |
| `:app:generateDebugBuildConfig '-Pcf.apiBaseUrl=https://api.test/"'` | Fails: `cf.apiBaseUrl must not contain quotes or backslashes` | 0.89 s |
| `:app:generateReleaseBuildConfig -Pcf.dataSource=mock -Pcf.apiBaseUrl=http://evil.invalid/v1` | Override ignored | 0.95 s |
| `:app:generateBenchmarkReleaseBuildConfig -Pcf.dataSource=api -Pcf.apiBaseUrl=http://evil.invalid/v1` | Override ignored | 0.88 s |
| `:app:generateNonMinifiedReleaseBuildConfig -Pcf.dataSource=api -Pcf.apiBaseUrl=http://evil.invalid/v1` | Override ignored | 0.86 s |
| `:app:generateDebugBuildConfig` | Defaults restored | 0.96 s |

Generated fields after those checks:

```java
// debug, explicit override
public static final String DATA_SOURCE = "api";
public static final String API_BASE_URL = "http://10.0.2.2:8000/v1";
// debug, restored defaults
public static final String DATA_SOURCE = "mock";
public static final String API_BASE_URL = "https://api.chronoflow.net/v1";
// release, despite attempted override
public static final String DATA_SOURCE = "api";
public static final String API_BASE_URL = "https://api.chronoflow.net/v1";
// benchmarkRelease and nonMinifiedRelease, despite attempted override
public static final String DATA_SOURCE = "mock";
public static final String API_BASE_URL = "https://api.chronoflow.net/v1";
```

## Other checks and limits

`make size` passed in 9.48 s, including its native-library gate:

```text
Bundle size: 1508092 bytes (1.51 MB), arm64-v8a/xxhdpi/en: OK
```

Production-source checks found no new logging, broad exception catches, `runCatching`, string request bodies or `addPathSegment` calls. `git diff --check` passed. Every phase commit was created with `git commit -S`; commit objects contain GPG signatures, with no trailers.

An optional signature-status query, `git log -10 --format='%h %G? %s'`, produced no output while waiting in GPG verification and was interrupted (exit 130). Signature presence was checked directly through `git cat-file commit`; signing itself succeeded for every commit.

No required command was blocked by the sandbox. `make check` was deferred under the phase's targeted-tests-plus-one-quick instructions. No emulator tests are needed for this JVM-only phase. `make perf`, `make perf-timeline` and `make baseline-profile` were not run as instructed; this phase needs no startup/timeline performance numbers.

Logs, configuration results and copied timing evidence are in `build/test-results/phase-4b/`. The normal final evidence remains under `build/test-results/{quick,build-logic,timing}/` and the app's debug/release JUnit XML directories.

## Post-phase fix: remove OkHttp's startup initializer

The release gate reported `STARTUP_INITIALIZER: okhttp3.internal.platform.PlatformInitializer` on `39c4323`. The app manifest now removes that metadata with `tools:node="remove"`. The merged release provider retains exactly `ProcessLifecycleInitializer` and `ProfileInstallerInitializer`. `Application.onCreate` and the production client construction code are unchanged.

The decision is OQ-A148. Inspected the cached `okhttp-android-5.5.1-sources.jar`, its matching AAR manifest and compiled classes:

- `androidMain/okhttp3/internal/platform/PlatformInitializer.kt` sets `PlatformRegistry.applicationContext` and forces `Platform.get()`.
- `androidMain/okhttp3/internal/platform/PlatformRegistry.kt` enables Android logging and selects the Android platform lazily. `Android10Platform.kt` implements API 29+ TLS, certificate-chain cleaning and cleartext policy using platform APIs without that context.
- `androidMain/okhttp3/internal/publicsuffix/AssetPublicSuffixList.kt` needs the context to read `PublicSuffixDatabase.list` from assets. Without it, public suffix loading fails; the callers are cookie parsing and `HttpUrl.topPrivateDomain()`.
- `commonJvmAndroid/okhttp3/internal/http/HttpHeaders.kt` returns before cookie parsing when the jar is `NO_COOKIES`. Our client enforces that jar and never calls `topPrivateDomain()`, so it currently needs no manual context initialization. If either context-dependent API is introduced, initialize with `OkHttp.initialize(applicationContext)` at the existing lazy IO boundary before using it.

`OkHttpStartupTest.clientBuildsOnIoWithoutTheStartupInitializerOrApplicationContext` checks the actual merged manifest, then exercises `ApiClient` with the real Android OkHttp artifact. It proves construction is lazy and off main, application context remains null, Android TLS socket factory/trust manager construction succeeds, and an injected DNS failure reaches `Offline` without external traffic. Its file fixture resolves through AGP's `test_config.properties` and the project root.

Validation for this fix:

| Command | Result | Wall time |
|---|---|---:|
| `./gradlew :app:verifyReleaseManifest` | Pass; report `OK`, only the two allowed initializers | 1.00 s |
| `make targeted ONLY='app:OkHttpStartupTest#clientBuildsOnIoWithoutTheStartupInitializerOrApplicationContext'` | One Robolectric case passes | 2.65 s |
| `make quick` (once for this fix) | Pass; 1,475 main-tier cases, 26 build-logic cases, tooling suites and six serial timing cases | 21.84 s total |

The new case took 0.096 s in the quick run. An initial test compilation required OkHttp's internal-API opt-in for checking the platform type. The first executable test attempt exposed an unsupported Robolectric provider-info overload; the final test uses this repo's existing merged-manifest fixture pattern instead. No test hung. No sandbox command was blocked.

The brief's Task 11 Konsist work remains in Task 10 commit `39c4323`. History was left intact; this fix is a new signed commit.
