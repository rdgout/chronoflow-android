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

## I2: edge-aware pool and foreground eviction (re-decided in round 2)

Round 2 supersedes the four-second decision. The pinned web deployment sources (`e36368ec`, `.ai/rules/deploy.md:12–29`, `docker/Caddyfile:8–9`, `docker/nginx/maintenance.conf`) establish **Cloudflare → nginx → Caddy/FrankenPHP**. There is no Node tier, so the previous Node five-second comparison does not apply. Cloudflare documents a [400-second client-side idle limit for HTTP/1.1 and HTTP/2](https://developers.cloudflare.com/fundamentals/reference/connection-limits/); [HTTP/2 is enabled by default](https://developers.cloudflare.com/speed/optimization/protocol/http2/).

The effective client owns a dedicated **`ConnectionPool(5, 300, TimeUnit.SECONDS)`**: five idle connections and OkHttp's default 300-second keep-alive, below that edge limit. An injected base client's pool remains unchanged. Retries stay disabled under OQ-A138/A149.

The cached OkHttp 5.5.1 `RealConnectionPool.kt` measures idle time with `System.nanoTime()`, which tracks awake time on Android. A short pool cap cannot cover deep sleep. `ApiClient.evictIdleConnections()` is suspending and closes idle sockets on the injected `io` dispatcher, because TLS close can send `close_notify`. It reads the volatile already-built client reference, without building a client or acquiring its initialization Mutex, and does nothing if unbuilt. Phase 4e awaits it as ON_START step 0, covering sleep and network changes while backgrounded. OQ-A150 records this decision and that the user may revisit it; the untracked 4e brief remains uncommitted.

The renamed pool-policy test checks 300 seconds below the 400-second edge limit, five idle connections, disabled retries and an unchanged injected pool. A MockWebServer regression proves that two requests reuse one connection before eviction and a subsequent request opens a new connection after eviction. A separate test proves eviction before client construction is a no-op. Round 2 results follow below; the round 1 timing table records the superseded policy's historical run.

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

## Round 2 I2 evidence

The targeted selectors were `core:network:ApiClientTests#dedicatedPoolUsesTheDefaultKeepAliveBelowTheCloudflareEdgeLimitWithoutRetries core:network:ApiClientTests#evictIdleConnectionsIsANoOpBeforeTheClientIsBuilt core:network:ApiClientTests#evictIdleConnectionsPreventsReuseOfAnIdleConnection`.

Written first, the tests ran red against the four-second pool and an eviction no-op: three cases ran, with the expected failures for 300 s versus 4 s and one idle connection remaining after eviction (2.32 s command wall time). The implementation then passed all three cases on its first green run (1.25 s). MockWebServer observed reuse before eviction and a new connection afterwards, with exchange indices `[0, 1, 0]`; no sleeps or wall-clock expiry waits are used. The already-built client reference remains volatile, and eviction never enters the construction Mutex; round 3 makes eviction suspend and moves socket closure to the injected IO dispatcher. Logs and JUnit/timing evidence are in `build/test-results/phase-4b-review-r2/`.

## Round 3 H1/L2 evidence

Closing an idle TLS socket can send `close_notify`. The round-2 synchronous eviction therefore allowed network work on the foreground caller's main thread and could crash release builds. `ApiClient.evictIdleConnections()` now snapshots the volatile built-client reference, returns immediately if absent, and otherwise suspends in `withContext(io)` while calling `connectionPool.evictAll()`. It never builds a client or acquires the construction Mutex. ON_START eviction covers sleep and network changes while backgrounded.

The new `ApiClientTests.evictIdleConnectionsClosesSocketsOnTheInjectedIoDispatcher` injects a `SocketFactory` whose sockets record `Thread.currentThread()` in `close()`, and a dedicated single-thread IO dispatcher distinct from the caller. It creates a real idle MockWebServer connection, evicts it from the caller, and requires exactly one close on that injected IO thread, with an empty pool afterwards.

The first harness run stopped at `Unauthorized` because the test omitted its artificial bearer (2.14 s command wall time); that setup was corrected before collecting the regression evidence. `make targeted ONLY='core:network:ApiClientTests#evictIdleConnectionsClosesSocketsOnTheInjectedIoDispatcher'` then failed against the unchanged production implementation with `Socket close must not run on the caller thread expected not same` (1.46 s). After the production fix, `make targeted ONLY='core:network:ApiClientTests'` passed all 28 cases on the first green run (3.05 s); the new regression took 0.007 s. The existing reuse and unbuilt-client tests also passed. No sleeps, emulator runs or TLS-specific test dependencies were needed.

OQ-A150 and the 4b brief now require suspending IO eviction and qualify the foreground guarantee to changes while backgrounded. The user-maintained, untracked 4e brief is unchanged. Logs, JUnit XML and per-run timing JSON are archived in `build/test-results/phase-4b-review-r3/`.

`make quick` ran once and passed in **23.71 s total** (`/usr/bin/time -p`): 1,480 main-tier cases in 9.03 s, 26 build-logic cases in 0.65 s, all tooling suites and six serial timing cases in 0.53 s. There were no budget violations; the new close-thread regression took 0.012 s. No required command was blocked, and no performance/profile commands were run.

## Phase commit list through the round-3 network fix

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
cc5c324 docs(phases): record the first api client review fixes
18fb46a fix(network): keep pooled connections under the edge idle limit and evict on foreground
3d08bf3 docs(phases): match the 4b verification record to the rewritten history
7e2779b fix(network): evict pooled connections on io
```

The separate round-3 record commit is titled `docs(phases): record the 4b round-3 fixes`.
