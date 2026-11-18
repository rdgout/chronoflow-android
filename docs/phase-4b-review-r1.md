# Phase 4b review round 1

2026-11-18. F1, F2, F3 and the I2 decision are complete. All changed behavior was tested red then green; the F3 test hardening used a controlled serializer mutation because the original production wire output was already correct.

## F2: rebuild the mixed commit first

Rebuilt from `39c4323^` on a temporary branch, applied `39c4323` without committing, then committed the authenticator fix, redaction, Task 11 Konsist guards, Task 10 build configuration and verification documentation separately. Replayed `170266c` as signed commit `0b3c526`.

Before changing the phase branch, `git diff --exit-code 170266c 0b3c526` succeeded with no output. Both trees are `9459ed57310ff07a1ffe0d8c57412ed89745800b`. Only then was `feat/phase-4b-api-client` moved to the rebuilt tip; the temporary branch was deleted. Existing phase checkboxes stayed complete. No runtime or generated-file differences were introduced by this history operation.

The former Task 10 commit's pieces are now:

| Commit | Scope |
|---|---|
| `72eb0ee` | Authenticator/proxy authenticator sanitation, its regression test, OQ-A147 and the matching brief sentence |
| `d931d9e` | Remove-passkey redaction/visible id and its test |
| `d9d7b84` | Task 11 Konsist rules, tests and planted/control fixtures |
| `9eff4fd` | Task 10 BuildConfig selection, debug/release tests, OQ-A145/A146 and mechanical documentation corrections |
| `712a8b2` | Initial phase verification documentation |
| `0b3c526` | Replayed startup-initializer fix, Android client-construction test and OQ-A148 |

The earlier report's `39c4323`/`170266c` references describe the original history. This report and the brief record the replacement structure.

## F1: prevent the implicit 503 retry

The new `ApiClientTests.serviceUnavailableWithZeroRetryAfterIsNotRetried` sends POST `terms/acceptance` to a route that answers 503 with `Retry-After: 0`, then 204 if contacted again. Red evidence was `Result: Server(status=204) expected:<1> but was:<2>`.

`ServiceUnavailableInterceptor` now removes `Retry-After` only from 503 responses before OkHttp's follow-up interceptor sees them. The cached OkHttp 5.5.1 `RetryAndFollowUpInterceptor.kt` checks 503 independently of `retryOnConnectionFailure`; an absent header stops that follow-up. The mapper never uses that header for 503, so it preserves `Server(503)` without changing domain mapping. 429 responses retain their headers.

Injected network interceptors are cleared first. Sanitation tests require exactly this one internal interceptor. OQ-A149 records the decision.

## F3: independent literal bodies and Swift throttle fixtures

The native-link wire test now compares explicit `JsonObject` values, independent of the request DTO serializers. They preserve `a%2Fb`, `a+b@example.com`, both password fields and the signed verification id/hash/signature, with numeric `expires: 1800000000`.

After writing those assertions, temporarily changed only `SignedVerificationInput.expires`' wire name to `expiry`. The red test rejected `expiry` where `expires` was required; the previous self-referential expectation would have changed alongside the request and accepted that error. Restored the entire production source file byte-for-byte before the green run and commit. No mutation is present in the final tree.

Retry-after rows `date`, `invalid`, `negative`, `string` and `past` now carry the Swift `code: throttled` / `message: fake` envelope wrapper. Deliberately bare/error-body rows remain bare. All ten parameter cases pass.

## I2: short idle reuse, retries remain disabled

Chose **4 seconds**, with **five maximum idle connections**. The pinned web deployment files (`e36368ec`, `docker/Caddyfile`, `compose.yaml`, `Dockerfile`, `docker/entrypoint.sh`) specify no HTTP keep-alive/idle timeout; the image runs Caddy/FrankenPHP. Therefore the orchestrator's four-second default applies. The five-second reference is Node's documented [`server.keepAliveTimeout`](https://nodejs.org/docs/latest-v24.x/api/http.html#serverkeepalivetimeout), not a claim about the deployed API server's timeout.

The cached OkHttp 5.5.1 `ConnectionPool.kt` documents/defaults to five idle connections and five minutes. The effective client now owns `ConnectionPool(5, 4, TimeUnit.SECONDS)`, constructed on its existing IO boundary. An injected base client's pool is unchanged. Retries stay off under OQ-A138/A149; OQ-A150 records that the user may revisit the policy. Shorter idle reuse reduces exposure to stale connections; abrupt peer closes can still become `Offline`.

The test reads the pinned pool's actual policy fields through Java reflection, avoiding a multi-second timing test. Red observed the injected pool's unchanged 120-second duration. Green proves the effective pool uses four seconds, below the five-second bound, with five idle connections, no connection retries and the original base pool unchanged.

## Red → green evidence

All entries below use `make targeted ONLY='…'`. Times are tier `wallSeconds` from the archived timing JSON.

| Change | Selectors | Red | Green |
|---|---|---|---|
| F1 | `core:network:ApiClientTests#serviceUnavailableWithZeroRetryAfterIsNotRetried` | Expected failure: two requests, `Server(204)`; 2.04 s | One request, `Server(503)`; included in the four-case run below |
| F1 sanitation/429 | F1 selector plus `core:network:ApiClientTests#noCookieOrCacheReuseAndMissingBearer core:network:ApiClientTests#sanitizesInjectedConfiguration core:network:ApiClientTests#retryAfterEndToEnd` | — | Four pass; 1.94 s |
| F3 | `core:network:ApiDataSourceTests#nativeLinkInputsReachExactPublicResetAndAuthenticatedSignedVerificationBodies` | Expected mutation failure: `expiry` instead of `expires`; 2.34 s | Wire case plus ten retry-after cases pass; 2.04 s |
| F3 fixtures | F3 selector plus `core:network:ApiErrorMapperRetryAfterTests` | — | Eleven pass in the same 2.04 s run |
| I2 | `core:network:ApiClientTests#poolKeepAliveStaysBelowTheServerIdleBoundWithoutRetries` | Expected failure: 120000000000 ns idle duration; 1.72 s | One pass; 1.39 s |

Each green production fix passed on its first attempt. No tests hung or needed a second fix attempt. Formatting was applied before green runs.

`make quick` ran once after all fixes and passed in **24.08 s total** (`/usr/bin/time -p`): 1,477 main-tier cases in 9.30 s, 26 build-logic cases in 0.65 s, all tooling suites and six serial timing cases in 0.54 s. The new 503 case took 0.014 s, the pool case 0.004 s, and the literal-body case 0.011 s in the quick run.

Logs, JUnit XML, per-run timings and history-tree evidence are archived under `build/test-results/phase-4b-review-r1/`. No required command was sandbox-blocked. No emulator or performance/profile commands were run.

## Final phase commit list

All commits below are GPG-signed Conventional Commits without trailers. Commits through `4c87ac8` are unchanged.

```text
c9fe81c docs(phases): refresh the 4b brief against 4a
f0982ce feat(model): add the data source contract and outbox change types
b84f499 feat(model): port trip wizard error paths and settings patch validation
81882be feat(network): add api base url, data source factory and endpoint
42e29d8 feat(network): map the error envelope to data failures
44cdaef feat(network): add the okhttp api client
7efe043 feat(network): implement auth, reads and outbox writes in the api data source
94558e4 feat(network): implement account security and passkey management routes
285b217 feat(network): implement trip wizard and guest review routes
4c87ac8 test(network): pin every endpoint with a deny-by-default contract table
72eb0ee fix(network): disable injected http and proxy authenticators
d931d9e fix(model): redact the current password in remove-passkey
d9d7b84 test(konsist): keep okhttp inside core network
9eff4fd build(app): select the data source and api base url per build type
712a8b2 docs(phases): record phase 4b verification
0b3c526 fix(app): keep okhttp's startup initializer out of the release manifest
21e573b fix(network): prevent silent retries after service unavailable
8929407 test(network): assert literal auth bodies and restore throttle fixtures
8d08963 fix(network): retire idle connections before common server timeouts
(this commit) docs(phases): record the first api client review fixes
```
