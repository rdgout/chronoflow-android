# 03 — The API contract the mobile app uses

Research for the native Android (Kotlin + Jetpack Compose) ChronoFlow app. Written 2026-11-15.

Sources, all read-only:

- `W` = `/Users/rickgout/Sites/chronoflow-new` (Laravel; product source of truth). Checked-out branch: `feat/mobile-api-passkey-management` at `1f4a2684`, 12 commits ahead of `origin/main` = `e36368ec`.
- `I` = `/Users/rickgout/Sites/chronoflow-ios` (native iOS; the behavioural reference). Contract-relevant code is under `Packages/ChronoFlowKit/Sources/{AppCore/Contracts,DataAccess/API}`. The iOS `plan.md` cites the server as `W`.
- `M` = `/Users/rickgout/Sites/chronoflow-mobile` (React Native; earlier attempt).

How to read this document:

- **[F]** marks a fact checked in source, with a file and line reference.
- **[R]** marks a recommendation for Android.
- Ambiguous points are collected in §20, *Open questions*. Each one has a default, so the plan can go ahead without an answer.

---

## 0. Summary

1. **[F] One versioned JSON API on its own host.** Production is `https://api.chronoflow.net/v1` (`W docs/ui-plan/phase-12-mobile-api.md` P12-D16). The website stays on `app.chronoflow.net`, and the marketing site is on `www.chronoflow.net`.
   - All endpoints are in `W routes/api.php` (114 lines), under `Route::domain(config('domains.api'))->prefix('v1')->name('api.v1.')`.
   - Auth uses **Sanctum personal access tokens as bearer tokens only**: no cookies, no CSRF, no session, no CORS.
2. **[F] 46 route registrations on `/v1`.** Of these, 39 are API controllers and 7 are auth and public routes (counted from `routes/api.php`).
   - On `origin/main` (`e36368ec`, the commit the iOS contracts are pinned to), the four `account/passkeys*` routes do not exist yet. They are on the unmerged branch `feat/mobile-api-passkey-management` (web Phase 20).
3. **[F] Every error has the same envelope:** `{code, message, errors?, args?, retryAfterSeconds?}`.
   - `message` and each `errors[field]` value is an **untranslated English sentence key** that exists in both `W lang/en.json` and `W lang/nl.json`.
   - The client translates these keys.
4. **[F] Plan generation is synchronous. There is no polling, no job and no websocket.**
   - `POST /v1/trips/review` computes the plan inside the request (server medians are in the tens of ms).
   - `POST /v1/trips` saves it.
   - `GET /v1/trips/{id}` reads it.
   - iOS uses a 90 s timeout for these writes.
5. **[F] No purchases on mobile.**
   - Billing (Lemon Squeezy checkout, portal, cancel, resume) is web-only, behind the session in `W routes/billing.php`.
   - The API exposes only `billing.subscription` (read-only) and `settings.planCredits` in `GET /v1/account`.
   - iOS shows "Not enough credits" with **no** purchase action and **no** link out (iOS OQ 41). There is no StoreKit.
   - **[R]** Android should do the same: no Google Play Billing for now (§14).
6. **[F] The server does not support Android passkeys or Android App Links yet:**
   - `W` has no `assetlinks.json` route.
   - `config/fortify.php:148-153` sets `allowed_origins` to `[APP_URL]` only, so there is no `android:apk-key-hash:` origin.
   - Phase 18 and Phase 20 explicitly scope Android out (P18-D1, P20-D1).
   - The WebAuthn library already accepts `android:apk-key-hash:…` origins verbatim (`W vendor/web-auth/webauthn-lib/src/CeremonyStep/CheckAllowedOrigins.php`). Two small server changes would make Android passkeys work (§9.4).
7. **[F] Contracts are generated from PHP DTOs as TypeScript, not OpenAPI.**
   - `spatie/laravel-typescript-transformer` writes `W resources/js/types/App/**/index.ts`.
   - iOS turns that TypeScript into Swift with a 196-line Python script pinned to a web commit (`I Tools/contracts-gen/generate.py`, `PIN = e36368ec…`). It checks the result in CI (`make contracts-check`).
   - **[R]** Port that script to emit Kotlin `@Serializable` classes from the same pin (§16).
8. **[F] iOS has already done most of this research.**
   - `I plan.md` §§ "Auth and offline-store spec", "API contract and offline sync" and "Trips source contracts and behavior" (lines 362–470 and 1317–1540) are an accurate, source-checked description of this API, with line references.
   - **Android can reuse it almost as written** (§17).

---

## 1. Hosts, versions and pins

| Item | Value | Source |
|---|---|---|
| API base URL (production) | `https://api.chronoflow.net/v1` | P12-D16; `I Config/Base.xcconfig:3` |
| API host config | `config('domains.api')`. `API_DOMAIN` overrides it; otherwise it is `api.` + the `APP_URL` host. | `W config/domains.php` |
| Web host (RP ID, links, AASA) | `app.chronoflow.net` (`APP_URL`) | P18-D2 |
| Marketing host (terms, privacy) | `www.chronoflow.net`: `/terms`, `/privacy`, and the Dutch versions under `/nl/...` | `W routes/www.php` |
| Local dev | Herd: `https://api.chronoflow.test/v1` with `API_DOMAIN=api.chronoflow.test` | P12 "Mobile and local setup" |
| Path rule | Any API-host path outside `v1` or `v1/*` returns the 404 envelope, before routing | `W bootstrap/app.php:66`; `RestrictApiPaths` |
| Version policy | Still `/v1`, even after breaking changes, because "the iOS app has not shipped" | P19-D12 |
| iOS contract pin | `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19` (= `origin/main` here) | `I Tools/contracts-gen/generate.py:11`; commit `4ae26579` "build(contracts): pin web main and drop the merged overlays" |
| iOS plan-copy pin | `afd80dc0` (trip-wizard branch) for `lang/*.json` → `PlanCopy.xcstrings` | `I Tools/PlanCopyGen/generate.py:11`; iOS OQ 34 |
| Phase 20 (passkey management) | Branch `feat/mobile-api-passkey-management` `1f4a2684`, **not merged**. iOS hand-writes these DTOs. | `I …/Contracts/PasskeyManagementContracts.swift:3-5` |

**[R]** Pin Android contracts to `e36368ec`, as iOS does. Add a hand-written overlay for the Phase 20 passkey-management DTOs. Move the pin when Phase 20 merges.

---

## 2. Transport, headers and caching

**[F] Requests**

- Send `Accept: application/json`. The server forces JSON responses anyway (`ForceJsonResponse`).
- Send `Content-Type: application/json` with every body, including the `DELETE` bodies.
- Send `Authorization: Bearer <token>` on authenticated routes.
- Bodies are **snake_case**. Responses are **camelCase**.
  - Exceptions in auth inputs: `challengeId` (2FA and passkeys) and `credential.*` (WebAuthn member names).
  - Exception in responses: `ApiTripInputSnapshot.input` is snake_case, so it can be sent back as it is (P17 handoff).
- **No `Accept-Language`.** The API is language-free since Phase 15 (P15-D16, P17-D9). Every response presents in English keys and in `AmountStyle::Drinks`.

**[F] Responses**

- Authenticated **GETs** are in one middleware group (`W routes/api.php:105-113`): `auth:sanctum`, `throttle:api`, `VaryOnAuthorization`, `cache.headers:private;no_cache;etag`.
  - So: `Cache-Control: no-cache, private`, an `ETag` (xxh128 of the body) and `Vary: Authorization`.
  - A matching `If-None-Match` gets an empty **304** (P12-D15, P13-D8).
  - Error responses have no ETag.
- Writes are outside that group: no ETag, no Vary.
- Responses that carry secrets or reviews send `Cache-Control: private, no-store`: 2FA setup and codes, trip review, guest review, trip save.
- `GET /v1/airports` sends `Cache-Control: public, max-age=3600`.

**[F] iOS client behaviour** (`I DataAccess/API/APIClient.swift`)

- Ephemeral `URLSession`: no cookie store, no URL cache, no credential store.
- **Refuses every redirect** (`APIRedirectDelegate.swift`).
- Timeouts:
  - 15 s by default.
  - 90 s for register, member review, guest review and trip save/update (`APIDataSource.swift:37`, `+TripWizard.swift`).
  - 3 s for logout.
- A transport error or timeout maps to `offline`.
- Public routes never carry a bearer token: login, two-factor, register, forgot, reset, passkey options, passkey login, airports and guest review (`APIEndpoint.Access.publicRoute`).
- `expectedStatus` is checked exactly:
  - 201 for `POST trips` and `POST account/passkeys`.
  - 200 for `PUT trips/{id}`.
  - 204 with an empty body for `requestNoContent`.
- **iOS does not use ETags.** There is no `If-None-Match` anywhere in `I`.

**[R] Android client**

- OkHttp with `followRedirects(false)` and `followSslRedirects(false)`, no `CookieJar`, and **no OkHttp disk `Cache`**. That cache would write plan bodies unencrypted and does not know about account scoping.
- Optional: store ETags yourself in Room per `(accountId, resource)` and send `If-None-Match` on `GET trips` and `GET trips/{id}`.
  - Worth it for the list: the server median for 20 trips is 126.9 ms (P15 timings), and bodies are large.
  - A 304 still uses up the 60/min `api` budget.
- Use per-call timeouts with the same values as iOS (15 s, 90 s, 3 s).
- Use an interceptor that adds the bearer only to authenticated endpoints. Model public and authenticated access as a type, as iOS does.
- Never log bodies, `Authorization`, passwords, challenge ids, TOTP or recovery codes, guest secrets or signed links. This is the web's redaction list (P12-D13, P15-D19).

---

## 3. The error envelope

**[F] Wire shape** (`W resources/js/types/App/Data/Api/index.ts`, `ApiError`; renderer `W app/Http/Responses/Api/ApiExceptionResponse.php`):

```json
{ "code": "validation_failed", "message": "The given data was invalid.",
  "errors": { "email": "These sign-in details don't match our records." },
  "args": { "name": { "max": 255 } },
  "retryAfterSeconds": 37 }
```

- Empty optional members are **absent**, not null (`ApiExceptionResponse.php` `errorResponse`).
- `errors` holds the **first** message for each field, as a string.
- `args[field]` holds the placeholder values for `:name` placeholders in that key. If a key contains `:attribute` and has no args, the server adds `{attribute: field}`.
- A 429 always carries `retryAfterSeconds` **and** a matching `Retry-After` header. The server falls back to 60.
- A 500 is always the generic message. No exception detail ever leaks.

**[F] Status → code** (`ApiExceptionResponse.php`, `match ($status)`):

| Status | `code` | `message` key (EN) | NL (`W lang/nl.json`) |
|---|---|---|---|
| 401 | `unauthenticated` | Please sign in to continue. | Log in om verder te gaan. |
| 403 (verification) | `email_unverified` | Your email address is not verified. | Je e-mailadres is niet geverifieerd. |
| 403 (other) | `forbidden` | You do not have permission to do this. | Je hebt geen toestemming om dit te doen. |
| 404 | `not_found` | The requested resource was not found. | De gevraagde gegevens zijn niet gevonden. |
| 405 | `method_not_allowed` | This request method is not allowed. | Deze aanvraagmethode is niet toegestaan. |
| 409 | `conflict` | This request conflicts with the current state. Please try again. (2FA overrides it, see below) | Deze aanvraag conflicteert met de huidige situatie. Probeer het opnieuw. |
| 413 | `payload_too_large` | The request is too large. | De aanvraag is te groot. |
| 422 | `validation_failed` | The given data was invalid. | De opgegeven gegevens zijn ongeldig. |
| 429 | `throttled` | Too many attempts. Please try again later. | Te veel pogingen. Probeer het later opnieuw. |
| 500 | `server_error` | Something went wrong. Please try again later. | Er is iets misgegaan. Probeer het later opnieuw. |
| 503 | `unavailable` (maintenance, also before routing) | The service is temporarily unavailable. Please try again later. | De dienst is tijdelijk niet beschikbaar. Probeer het later opnieuw. |
| other ≥ 400 | `http_error` | The request could not be completed. | De aanvraag kon niet worden voltooid. |

The 2FA state conflicts are 409 envelopes whose **message** is the specific sentence (`W app/Actions/Api/TwoFactorConflicts.php`):

- `Two-factor authentication is already on.`
- `Two-factor authentication is off.`
- `Start two-factor setup again.`

**[F] Domain errors are ordinary `validation_failed` fields.** There is no extra `code` for them.

| Field | Key | Meaning |
|---|---|---|
| `review_expired` | `This review has expired. Update your plan before saving.` (`W app/Planning/TripDrafts.php:73`) or `Review your trip again before creating your account.` (`RegisterWizardTrip.php:101`) | The draft or guest secret is stale. Start a fresh review from the same input. |
| `credits` | `Not enough credits` (`PresentMobileReview::NOT_ENOUGH_CREDITS`) | Save is blocked by the balance |
| `review` | `Try saving again shortly.` (50 live receipts) or the past-arrival keys | Capacity, or a whole-trip error |
| `form` | `This plan has changed. Refresh it before marking this step.` | The check-off hash is stale |
| `profile`, `profile.*` | Body-clock keys | Invalid account answers in a review |
| `credential`, `challengeId` | Passkey keys | §9 |

**[F] iOS mapping** (`I APIClient.swift` `mapError`, `TripWizardErrors.swift`):

- 401 + `unauthenticated` → `unauthorized`. This wipes the session, but only for the same session epoch.
- 403 + `email_unverified` → verification gate.
- 422 + `validation_failed` → field messages `{key, args}`. Then:
  - `review_expired` → `reviewExpired`.
  - `credits == "Not enough credits"` → `notEnoughCredits`.
  - `review == "Try saving again shortly."` → `saveCapacity`.
  - Any `profile*` field → `profileInvalid`.
- 409 with one of the three 2FA sentences → a `form` field message. Any other 409 → `conflict`.
- 429 → `throttled(retryAfter)`. Priority: body `retryAfterSeconds`, then the `Retry-After` header (seconds or HTTP-date), then 60 s.
- Anything else, malformed JSON or an unexpected success shape → `server(status)`.
- Journey error paths are rewritten to form paths by `TripWizardErrors.journeyPath`:
  - `return.legs.N…` → `legs.(N+outboundCount)…`
  - `…stay` → `stays.N`
  - `trip_end_*` → `end.*`
  - and so on.

**[R]** Mirror the iOS mapping as a sealed `DataFailure` Kotlin hierarchy.

- Show copy by looking up the English key in a generated `en`/`nl` resource map, never by showing the server sentence. Convert Laravel `:max` placeholders into positional format args, as iOS `FieldMessage.interpolationKey` does.
- For Dutch, unknown server English must **never** be shown. Fall back to the generic server error (iOS OQ 28).

---

## 4. Rate limits (all session-free)

Defined in `W app/Providers/AppServiceProvider.php:105-228` and `W app/Providers/BillingServiceProvider.php:25-34`.

| Limiter | Limits | Key | Applies to |
|---|---|---|---|
| `api` | 60/min | user id (or IP) | Every authenticated route, stacked |
| `api-login` + `api-login-ip` + `api-login-email` | 5/min, 20/min, 20/h (the hourly cap counts **422s only**) | email+IP; IP; email | `auth/login` |
| `api-two-factor` | 5/min + 10/h | the challenged user (or IP) | `auth/two-factor` |
| `api-registration` | 5/min | IP | `auth/register` |
| `api-password-email` | 1/min + 5/min | email+IP; IP | `auth/forgot-password` |
| `api-password-reset` | 5/min | IP | `auth/reset-password` |
| `api-verification-send` | 1/min | user | `email/verification-notification` |
| `api-verification` | 6/min | user+IP | `email/verify` |
| `api-passkey-options` | 10/min | IP | `auth/passkey/options` |
| `api-passkey-login` | 5/min, 20/min, 20/h (the hourly cap counts 422s only) | sha256(credential.id)+IP; IP; sha256(credential.id) | `auth/passkey/login` |
| `api-credential` | 6/min + 20/h (the hourly cap counts 422s only), **one budget shared** by every route that uses it | user | `PATCH users/me`, `PUT account/password`, 2FA enable, disable, regenerate and reveal, `DELETE account`, passkey options, store and delete |
| `api-two-factor-confirm` | 5/min + 10/h | user | `account/two-factor/confirmation` |
| `trip-review` | 10/min per user (5/min guest session + 30/min IP; not reached from the API) | user | `trips/review`, `trips/{id}/review` |
| `trip-save` (inside the action) | 10/min | user | `POST trips`, `PUT trips/{id}`: 429 with `Retry-After` |
| `api-guest-review` | 5/min + 30/min | IP+`device_id`; IP | `guest/trips/review` |
| `airport-search` | 60/min | IP, shared with the web search | `GET airports` |

**[R]**

- Debounce sync triggers: foreground, reconnect, outbox and pull-to-refresh.
- One full sync costs `users/me` + `account` + `trips` + N details. That fits inside 60/min for normal accounts.
- Treat 429 as retryable, and show a "Try again in N s" countdown in the wizard, as iOS does.

---

## 5. Endpoint table

Notes for the table:

- "Auth": **P** = public (no bearer), **B** = bearer, **V** = bearer + `verified` (otherwise 403 `email_unverified`).
- Every **B** and **V** route also has `throttle:api`.
- "iOS user" names the `I` data-source method and the screen or feature.
- Line numbers are from `W routes/api.php`.

### 5.1 Auth and identity

| # | Method + path | Auth | Request body | Success | Notable errors | Limits | Idempotency | iOS user |
|---|---|---|---|---|---|---|---|---|
| 1 | `POST /v1/auth/register` (:48) | P | `ApiRegisterInput {name, email, password, password_confirmation, device_name, device_id(uuid), language?('en'\|'nl'), draft_token?, input_revision?, review_revision?, guest_secret?}`. Email is trimmed and lowercased. | 200 `ApiSession {kind:'session', token, account: ApiAccount, tripId: string\|null}`. The account is unverified. | 422 field keys (`AuthMessageKeys`), `This email address is already in use.`, password policy keys, `review_expired` (claim) | `api-registration` | A claim retry with the same `draft_token`+`guest_secret`+email+password returns the same session and `tripId` for 7 days (P17-D15) | `signUp` → SignUp (11c), guest wizard claim (18) |
| 2 | `POST /v1/auth/login` (:51) | P | `ApiLoginInput {email, password, device_name(≤255), device_id(uuid), draft_token?, guest_secret?}` | 200 `ApiSession` **or** `ApiTwoFactorChallenge {kind:'two-factor', challengeId}` | 422 `errors.email` = `These sign-in details don't match our records.` | 3 login limiters | Replaces this device's token | `signIn` → Sign in (10a) |
| 3 | `POST /v1/auth/two-factor` (:53) | P | `ApiTwoFactorInput {challengeId, code(6 digits)}` **xor** `{challengeId, recovery_code}` | 200 `ApiSession` | 422 `code`: `The authentication code was not accepted. Try again.`; `recovery_code`: `This recovery code was not accepted. Try another saved code.`; an unknown or expired challenge reports on the submitted field | `api-two-factor` | Challenge: 5 min, 5 failures, single use | `completeTwoFactor` → Two-factor (10b) |
| 4 | `POST /v1/auth/passkey/options` (:54) | P | none | 200 `ApiPasskeyChallenge {challengeId(64), options: ApiPasskeyRequestOptions}` | — | `api-passkey-options` | New challenge on every call (120 s TTL) | `passkeyOptions` → Sign in passkey button |
| 5 | `POST /v1/auth/passkey/login` (:55) | P | `ApiPasskeyLoginInput {challengeId, credential: ApiPasskeyAssertion, device_name, device_id, draft_token?, guest_secret?}` | 200 `ApiSession`. **Never** a 2FA step. | 422 `credential`: `We couldn't sign you in with that passkey. Please try again.`; `challengeId`: `The passkey request expired. Please try again.` | `api-passkey-login` | The challenge is consumed even when the attempt fails | `passkeyLogin` |
| 6 | `POST /v1/auth/forgot-password` (:49) | P | `{email}` | 204, always (no enumeration) | 422 email format | `api-password-email` | — | `requestPasswordReset` → Forgot (11a) |
| 7 | `POST /v1/auth/reset-password` (:50) | P | `{token, email, password, password_confirmation}` | 204. **No session.** Revokes **all** tokens and web sessions. | 422 `token`: `This password reset link is invalid or has expired.` | `api-password-reset` | — | `resetPassword` → Reset (11b) |
| 8 | `POST /v1/auth/logout` (:56) | B | none | 204. Revokes only the **current** token. | 401 | `api` | — | `signOut` (3 s timeout) |
| 9 | `GET /v1/users/me` (:107) | B (**not** verified) | — | 200 `ApiAccount {id, name, email, hasVerifiedEmail, policies: PolicyVersionsView, walkthroughSeen}` + ETag | 401 | `api` | — | `fetchIdentity`, `verificationStatus` → launch gate, Verify (11d), sync step 1 |
| 10 | `PATCH /v1/users/me` (:71) | B (**not** verified) | `{name?, email?, current_password?}`. `current_password` is required only when the email changes (case-insensitive compare). | 200 `ApiAccount`. After an email change, `hasVerifiedEmail:false`, a verification mail goes out, other tokens and web sessions are revoked, and the calling token is kept. | 422 `current_password`: `The provided password was incorrect.`; email keys | `api-credential` | Absolute values | `send(.profile)` → Settings Account (17h) |
| 11 | `POST /v1/email/verification-notification` (:66) | B | none | 200 `ApiEmailVerificationStatus {email, hasVerifiedEmail, resendAfterSeconds}` | 429 | `api-verification-send` | Already verified → true, 0, no mail | `resendVerificationEmail` → Verify |
| 12 | `POST /v1/email/verify` (:68) | B | `ApiEmailVerificationInput {id(uuid), hash(40), expires(int), signature(64)}`, parsed from the web link | 200 `ApiEmailVerificationStatus` | 422 (all fields): `This email verification link is invalid or has expired.` | `api-verification` | Verifies once | `verifyEmail` → Verify (link entry) |

### 5.2 Account, settings and security (all verified unless marked)

| # | Method + path | Request | Success | Notable errors | Limits | Idempotency | iOS user |
|---|---|---|---|---|---|---|---|
| 13 | `GET /v1/account` (:108) | — | 200 `ApiAccountSnapshot` (§13.1) + ETag | 403 unverified | `api` | — | `fetchAccount` → sync; Settings home, Plan & credits, Security |
| 14 | `PATCH /v1/account/body-clock` (:72) | snake_case partial diff of 20 fields (§13.2); `null` clears | 200 `ApiAccountSnapshot` | 422 per field; unknown field → `This setting cannot be changed here.`; empty body → `form`: `Choose one of the available options.` | `api` | Absolute and commutative across fields | `send(.settings(.bodyClock))` → Body clock (17h), outbox |
| 15 | `PATCH /v1/account/preferences` (:73) | `{time_format?('12'\|'24'), hidden_categories?(full list of distinct BlockType), show_optional_items?}` | 200 snapshot | 422 `Choose one of the available options.` | `api` | Absolute | `send(.settings(.preferences))` → Preferences |
| 16 | `PATCH /v1/account/reminders` (:74) | `{light?, avoid?, caffeine?, winddown?, lead_min?(15\|30\|60)}` | 200 snapshot | as above | `api` | Absolute | `send(.settings(.reminders))` → Reminders (19) |
| 17 | `POST /v1/terms/acceptance` (:69) | `{accepted: true, version: "1.0"}` (`PolicyVersions::CURRENT_TERMS = '1.0'`) | 204 | 422 `accepted`: `Please agree to continue.`; `version`: `The terms have changed. Read the current version and agree again.` | `api` | Idempotent 204 | `send(.policies(version))` → Terms gate (17g) |
| 18 | `POST /v1/walkthrough` (:70) | none | 204 | — | `api` | Idempotent | `send(.walkthrough)` → Walkthrough (17f) |
| 19 | `PUT /v1/account/password` (:75) | `{current_password, password, password_confirmation}` | 204. Keeps the calling token, revokes the others and all web sessions. | 422 `current_password`, password policy keys | `api-credential` | — | `accountSecurity(.password)` → Security |
| 20 | `POST /v1/account/two-factor` (:77) | `{current_password}` | 200 `ApiTwoFactorSetup {secretKey, otpauthUrl}`, `no-store` | 409 `Two-factor authentication is already on.` | `api-credential` | Idempotent while setup is pending | `.enableTwoFactor` |
| 21 | `POST /v1/account/two-factor/confirmation` (:79) | `{code}` (6 digits) | 200 `ApiRecoveryCodes {recoveryCodes: string[]}`, `no-store`. Revokes the other tokens. | 422 `code`; 409 `Start two-factor setup again.` | `api-two-factor-confirm` | — | `.confirmTwoFactor` |
| 22 | `DELETE /v1/account/two-factor` (:81) | `{current_password}` | 200 `ApiAccountSnapshot` | 422 | `api-credential` | Idempotent when off | `.disableTwoFactor` |
| 23 | `POST /v1/account/two-factor/recovery-codes` (:83) | `{current_password}` | 200 `ApiRecoveryCodes` (new set), `no-store` | 409 off | `api-credential` | — | `.regenerateRecoveryCodes` |
| 24 | `POST /v1/account/two-factor/recovery-codes/reveal` (:85) | `{current_password}` | 200 `ApiRecoveryCodes` (current set), `no-store` | 409 off | `api-credential` | — | `.revealRecoveryCodes` |
| 25 | `DELETE /v1/account` (:93) | `{password}` | 204 | 422 `password`: `The provided password was incorrect.`; `billing`: `Your subscription could not be cancelled. Please try deleting your account again.` | `api-credential` | — | `deleteAccount` → Delete account (17h) |
| 26 | `GET /v1/account/passkeys` (:109) **Phase 20** | — | 200 `ApiPasskeyList {passkeys: ApiPasskey[]}`, newest first, + ETag | — | `api` | — | `.listPasskeys` → Passkeys page |
| 27 | `POST /v1/account/passkeys/options` (:87) **Phase 20** | `{current_password}` | 200 `ApiPasskeyRegistrationChallenge {challengeId, options: ApiPasskeyCreationOptions}` | 422 `current_password` | `api-credential` | New challenge on every call (120 s), bound to user + token | `.passkeyCreationOptions` |
| 28 | `POST /v1/account/passkeys` (:89) **Phase 20** | `ApiPasskeyRegistrationInput {challengeId, name, credential: ApiPasskeyAttestation}` | **201** `ApiPasskey` | 422 `credential`: `We couldn't register that passkey. Please try again.`; `challengeId`: `The passkey registration request expired. Please try again.` | `api-credential` | Consumes the challenge | `.addPasskey` |
| 29 | `DELETE /v1/account/passkeys/{id}` (:91, `[1-9][0-9]{0,17}`) **Phase 20** | `{current_password}` | 204. Revokes the other tokens and all web sessions. | 404 (unknown or foreign, checked before the password) | `api-credential` | — | `.removePasskey` |

### 5.3 Trips, wizard and guest

| # | Method + path | Auth | Request | Success | Notable errors | Limits | Idempotency | iOS user |
|---|---|---|---|---|---|---|---|---|
| 30 | `GET /v1/airports?query=&limit=` (:59) | P | `query` 2–60 characters; `limit` 1–20 (default 8) | 200 `ApiAirportSearchResults {airports: [{iata, name, city\|null, country, timezone}]}`, `public, max-age=3600` | 422 | `airport-search` | Cacheable | `searchAirports` → wizard Where steps |
| 31 | `POST /v1/guest/trips/review` (:62) | P | `ApiGuestTripReviewInput`: a **one-way** journey body plus `device_id`, `guest_secret?` and a required `profile` (§10) | 200 `ApiTripReview`; `identity.guestSecret` is non-null only when a secret is minted. `no-store`. | 422 `return` → `Choose one of the available options.`; `review_expired` | `api-guest-review` | Max 5 drafts per device; the 6th evicts the oldest | `reviewGuestTrip` → Guest wizard (18) |
| 32 | `POST /v1/trips/review` (:95) | V | `ApiTripReviewInput` (§11). **Writes `profile` before calculating.** | 200 `ApiTripReview`, `no-store` | 422 journey paths, `profile.*`, `review`, `review_expired` | `trip-review` 10/min | New revision on every call | `reviewTrip` → Member wizard (16) |
| 33 | `POST /v1/trips` (:97) | V | `{draft_token(uuid), input_revision(≥1), review_revision(≥1)}` only | **201** `ApiTripSaveResult {tripId}`, `no-store` | 422 `credits`, `review_expired`, `review`; 429 `trip-save` | `api` + `trip-save` | **Receipt**: the same identity returns the same `tripId` for 7 days (max 50 live receipts) | `saveTrip` |
| 34 | `GET /v1/trips/{uuid}/input` (:112) | V | — | 200 `ApiTripInputSnapshot {input: ApiTripInput (snake_case), sourceRevision(64), airports: {IATA: ApiAirportSuggestion}, stayEnd: ApiStayEnd\|null}` + ETag | 404 when not the owner | `api` | — | `tripInput` → Edit, Duplicate |
| 35 | `POST /v1/trips/{uuid}/review` (:98) | V | `ApiTripReviewInput` + `source_revision` | 200 `ApiTripReview` | as #32, plus `review_expired` on a stale source | `trip-review` | — | `reviewTripEdit` |
| 36 | `PUT /v1/trips/{uuid}` (:100) | V | `{draft_token, input_revision, review_revision, source_revision}` | **200** `ApiTripSaveResult` | as #33 | `api` + `trip-save` | Receipt | `updateTrip` |
| 37 | `GET /v1/trips` (:110) | V | — | 200 `ApiTripList {trips: ApiTripListItem[]}`: **every** trip, unpaginated, sorted by status rank, then `departureUtc` descending, then id descending. + ETag. | 403 | `api` | — | `fetchTrips` → sync; Today, My trips |
| 38 | `GET /v1/trips/{uuid}` (:111) | V | — | 200 `ApiTripDetail {trip, plan, notices, timeline, checkState, detailVersion}` + ETag | 404 when not the owner, unknown or deleted | `api` | — | `fetchTripDetail` → Trip page, **Timeline** |
| 39 | `PUT /v1/trips/{uuid}/checks` (:101) | V | exactly `{plan_input_hash(64 hex), type: BlockType, local_date(Y-m-d), ordinal(int 1–255, strict), status: 'done'\|'skipped'\|null}`. `status` must be **present**. | **200** `ApiTripCheckResult {checkState, detailVersion}`. Also 200 for no-ops. | 422 `form` (stale or unknown field), `plan_input_hash`, `type`, `ordinal`, `local_date`, `status`; 404 | `api` | Absolute state; last writer wins; server time | `send(.checkOff)` → Today, Trip page, outbox |

**[F] What is not in the API.** These are web-only and session-bound (`W routes/trips.php`, `settings.php`, `billing.php`):

- trip delete, complete, reopen and duplicate (as a server copy);
- `calendar.ics`, `plan.csv` and the calendar feed;
- `settings/data.json` (data export);
- push devices (web-push only);
- the calendar prompt;
- account language;
- all billing.

API cards send `canEdit:true, canDuplicate:true, canComplete:false, canReopen:false, canDelete:false` (`W app/Actions/Api/ReadMobileTrips.php:21` with `TripCards::forUser` parameter order `:23-31`).

---

## 6. Token lifecycle

**[F]**

- **Issue.** `IssueMobileToken` (`W app/Actions/Api/IssueMobileToken.php`) does the following under an account row lock:
  - deletes the user's earlier tokens with the same `device_id`;
  - creates a token with ability `mobile` and `expires_at = now + 90 days`;
  - stores `device_id`;
  - fires `Login('sanctum')` after commit.
  - Token text is Sanctum `"<uuid-v7 id>|<secret>"` (`token_prefix` is empty, `W config/sanctum.php:14`). The plaintext is returned only once.
- **Sliding refresh.** `RefreshMobileTokenExpiry` (`W app/Listeners/RefreshMobileTokenExpiry.php`) runs on every authenticated use. If `last_used_at` is null or more than one day old, it sets `last_used_at = now` and `expires_at = now + 90 days`.
  - It never revives an expired token.
  - There is **no refresh token**. Any 90 days of inactivity means a 401.
- **Pruning.** `sanctum:prune-expired --hours=24` runs daily.
- **Revoke.**

  | Event | Effect |
  |---|---|
  | `POST auth/logout` | Revokes only the current token |
  | Password reset | Revokes **all** tokens and web sessions |
  | API password change, email change, 2FA confirm/disable/regenerate, passkey removal | Revokes every **other** token and all web sessions; **keeps the calling token** (P15-D10, P20-D11) |
  | The same changes made on the web | Revoke all API tokens, so the app gets a 401 |
  | Account deletion | Deletes the tokens |
  | A new login on the same `device_id` | Replaces that device's token |
  | Adding a passkey | Revokes nothing |

- **Unverified accounts** still get a token. `users/me`, the email routes, `PATCH users/me` and logout work. Every `verified` route answers 403 `email_unverified`, and verification is checked **before** route-model binding, so the response does not reveal whether a trip exists (P13-D2).
- **iOS storage.** The session is in the Keychain (`I App/Readiness/KeychainSessionStore.swift`). `api.deviceId` is stored separately and survives sign-out. The account DB is excluded from backup (iOS OQ 26 default).

**[R] Android**

- Store the token encrypted, with a key from Android Keystore, in DataStore or a small file. Do not use the deprecated `EncryptedSharedPreferences`.
- Keep `device_id` (a random UUID v4, lower-case) in app-private storage.
- **Exclude both from Auto Backup and device transfer** with `dataExtractionRules` and `fullBackupContent`. Reason: a restored `device_id` on a new phone makes that phone's login **delete the old phone's token** (same user and device key). A restored token is also unwanted.
- `device_name`: `Settings.Global.DEVICE_NAME`, falling back to `Build.MANUFACTURER + " " + Build.MODEL`, at most 255 characters.
- On a 401 from an authenticated call, sign out and wipe local data for that account, but only when the 401 belongs to the current session epoch. Mirror iOS: "an old 401 must never wipe a newly signed-in session".

---

## 7. Two-factor

**[F] Sign-in** (`W app/Actions/Api/LoginMobile.php`, `MobileTwoFactorChallenges.php`, P12-D8, P15-D13):

1. `POST auth/login`. A confirmed-2FA user gets `{kind:'two-factor', challengeId}`.
   - The challenge id is `Str::random(64)`.
   - Only its sha256 is cached, bound to the user and the device fields. The TTL is 5 minutes.
   - It survives fewer than 5 failures and is deleted on success, expiry or the 5th failure.
   - It also stores a fingerprint `sha256(password hash | email | two_factor_confirmed_at)`. A credential change in between makes the challenge read as expired.
2. `POST auth/two-factor` with `challengeId` plus either `code` (`/^[0-9]{6}$/`) or `recovery_code`, never both.
   - Fortify replay protection applies.
   - A used recovery code is replaced by a new one (Fortify), so the number of codes never drops.
3. The response is `ApiSession`. **Keep `challengeId` in memory only**, never in navigation arguments or saved state. This follows the iOS rule; on Android, `SavedStateHandle` survives process death, so keep it out of there.
4. A passkey sign-in **never** asks for 2FA (P18-D6).

**[F] Management.** §5.2 #20–#24. Use `otpauthUrl` to draw the QR code; the server sends no SVG. Confirm returns the codes once. A 409 after confirm means 2FA is already on, so offer reveal.

**[R]**

- Draw the QR code on the device with ZXing core or similar from `otpauthUrl`. Also show `secretKey` for manual entry and offer to open the `otpauth://` intent.
- Mark the recovery-code screens `FLAG_SECURE`.

---

## 8. Email verification, password reset and links

**[F]**

- Email links always point at the **web origin** `APP_URL` (`AppUrls::web`):
  - reset: `https://app.chronoflow.net/reset-password/{token}?email={urlencoded}`
  - verify: `https://app.chronoflow.net/email/verify/{id}/{hash}?expires={int}&signature={64hex}`

  Shapes from `I plan.md:502`; routes from `W vendor/laravel/fortify/routes/routes.php`.
- The app parses each link and calls:
  - `POST /v1/auth/reset-password {token, email, password, password_confirmation}` (public);
  - `POST /v1/email/verify {id, hash, expires, signature}` (bearer, the same account).
- **Product decision (2 Nov):** "Universal links and Android App Links on `app.chronoflow.net`, with the web pages as the fallback; no `chronoflow://` handoff" (`M docs/web-api-requests.md` §1).
- The server's AASA (`W app/Http/Controllers/AppleAppSiteAssociationController.php`) currently serves **only** `webcredentials`, with no `applinks`. iOS declares `applinks:app.chronoflow.net` (`I Config/Device.entitlements`), but the server does not answer it yet.
- **There is no `assetlinks.json` at all** (grep of `W app config routes public docs .ai`: no hits outside the phase-18 and phase-20 "out of scope" lines).
- iOS also polls `GET users/me` when the app becomes active, so a verification done in the browser is picked up. Android should do this too.

**[R]**

- Declare `autoVerify` intent filters for `https://app.chronoflow.net/reset-password/*` and `/email/verify/*`. This needs a server `assetlinks.json` with `delegate_permission/common.handle_all_urls` (§9.4).
- Until the server ships it, links open in the browser and the web completes them. The app picks the verification up through `users/me`.
- Also offer a manual "paste link" path for testing only.

---

## 9. Passkeys (WebAuthn)

### 9.1 Relying party

**[F]**

- RP ID is `app.chronoflow.net`.
- `allowed_origins = [APP_URL]`.
- Timeout is 60 000 ms.
- `user_handle_secret = PASSKEYS_USER_HANDLE_SECRET` or `APP_KEY`.
- Source: `W config/fortify.php:148-153`.
- The passkey feature is on, with `confirmPassword: true`.
- Libraries: laravel/passkeys v0.2.1 and web-auth/webauthn-lib 5.3.x.
- A passkey made on the web works in the app, and the reverse.

### 9.2 Sign-in (Phase 18)

**[F]** `POST /v1/auth/passkey/options` (no body) returns:

```json
{"challengeId":"<64 chars [A-Za-z0-9]>","options":{"challenge":"<32 bytes, unpadded base64url, 43 chars>",
 "rpId":"app.chronoflow.net","timeout":60000,"userVerification":"required","allowCredentials":[]}}
```

Then `POST /v1/auth/passkey/login`:

```json
{"challengeId":"…","credential":{"id":"<unpadded b64url>","rawId":"…","type":"public-key",
 "response":{"clientDataJSON":"<unpadded b64url>","authenticatorData":"…","signature":"…","userHandle":"…"}},
 "device_id":"<uuid>","device_name":"…","draft_token":null,"guest_secret":null}
```

The rules (`W app/Http/Requests/Api/V1/LoginWithPasskeyRequest.php`, P18-D9/D10):

- `id` and `clientDataJSON` must be unpadded base64url.
- `authenticatorData`, `signature` and `userHandle` may be base64 or base64url, padded or not. The server normalises them (`credentialAsBase64Url`).
- `userHandle` is optional and may be null.
- Each string is at most 8 192 characters.
- The **whole** `credential` array is passed to `WebAuthn::fromJson`.
- Checks run in this order: request fields (the challenge is kept on failure), then the challenge (pulled under a lock, consumed even on failure), then the assertion.
- There is one error key per field and no hint about which check failed.

**[F] iOS.** It checks the options strictly: `challengeId.count == 64`, `rpId == "app.chronoflow.net"`, `timeout == 60000`, a 43-character challenge that decodes to 32 bytes (`I APIDataSource.swift:13-24`). Device acceptance is still open because the team ID is not confirmed (iOS OQ 4, OQ 20).

### 9.3 Management (Phase 20, unmerged)

**[F]** The flow (`W docs/ui-plan/phase-20-mobile-api-passkey-management.md`, "iOS handoff"):

1. Ask for the password.
2. `POST account/passkeys/options {current_password}`. The password is checked **here**, so a typo cannot leave an orphan passkey in the password manager.
3. Run the ceremony.
4. Within 120 s, `POST account/passkeys {challengeId, name, credential}` → **201** `ApiPasskey`.
5. Refetch `GET account` for `security.passkeyCount`.

The options, exactly:

```json
{"challenge":"<43>","timeout":60000,"rp":{"id":"app.chronoflow.net","name":"app.chronoflow.net"},
 "user":{"id":"<32-byte HMAC handle, b64url>","name":"<email>","displayName":"<name>"},
 "pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],
 "authenticatorSelection":{"requireResidentKey":true,"userVerification":"required","residentKey":"required"},
 "attestation":"none","excludeCredentials":[{"type":"public-key","id":"<b64url>"}]}
```

Store rules (`W app/Http/Requests/Api/V1/StorePasskeyRequest.php`):

- `credential.id` and `credential.response.clientDataJSON` must match `^[A-Za-z0-9_-]+$` (unpadded base64url).
- `rawId` must decode to the same bytes as `id`.
- `attestationObject` is at most 16 384 characters and must be attestation format `none`.
- `transports` is optional. Send at most 8 strings of at most 32 characters each. **`null` is a 422; leave the member out instead.**
- The server rebuilds the credential from those fields only, so extra members such as `authenticatorAttachment`, `clientExtensionResults`, or `publicKey` inside `response` are ignored.
- Key: ES256 on P-256, or RS256.
- Attested credential id: at most 191 bytes.
- `name`: trimmed, 1–255 characters.

`ApiPasskey` is `{id:int, name, authenticator: string|null (AAGUID label, e.g. "Apple Passwords"), createdAt, lastUsedAt|null}`. Dates use `yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'` (always 6 fraction digits).

### 9.4 Android-specific gaps (server changes needed)

**[F]**

- Android Credential Manager puts `origin = "android:apk-key-hash:<base64url(sha256(signing cert))>"` in `clientDataJSON` for native apps. **[R] — verify against current Credential Manager docs.**
- The server's `allowed_origins` contains only `https://app.chronoflow.net`.
- `CheckAllowedOrigins` puts any parseable entry with a scheme but no host into `rawOrigins` and **matches it verbatim**. P18-D2 already says: "Android later adds `android:apk-key-hash:` entries, which `CheckAllowedOrigins` already matches verbatim."
- Credential Manager also checks that the app is associated with the RP ID through **Digital Asset Links** at `https://app.chronoflow.net/.well-known/assetlinks.json`. That route does not exist. RN never had one either; RN passkeys were mock-only (`M src/config/api.ts:34` per `I plan.md:376`).

**[R] Required server work before Android passkeys can work.** Small, and it mirrors the AASA:

1. **`GET /.well-known/assetlinks.json`** on the app host.
   - Same pattern as the AASA route: in `withRouting(then:)`, with `RejectApiHost`, no session, `application/json`, `Cache-Control: max-age=3600, public`, exempt from maintenance mode.
   - Body: `[{"relation":["delegate_permission/common.handle_all_urls","delegate_permission/common.get_login_creds"],"target":{"namespace":"android_app","package_name":"<pkg>","sha256_cert_fingerprints":["<Play app-signing SHA-256>","<upload/debug SHA-256 for dev>"]}}]`.
   - Read the values from config, as `services.apple.webcredentials.apps` is read.
2. **Add `android:apk-key-hash:<b64url-sha256>`** to `fortify.passkeys.allowed_origins`, one entry per signing certificate: Play app signing, plus debug for development. Read them from config or env.
3. Phase 18's `HostBoundaryTest` and `ApiRouteBoundaryTest` guards need the new route listed, as was done for the AASA.

Without these, Android ships **password + 2FA only**. Hide the passkey button and the Passkeys row, as iOS hides the row when `passkeyCount` is absent.

**[R] Client mapping.** Use `androidx.credentials` (Credential Manager; Play Services provider on API 28+).

- **Sign-in:** `GetPublicKeyCredentialOption(requestJson)`, where `requestJson` is the server `options` object re-serialized: `{challenge, rpId, timeout, userVerification, allowCredentials:[]}`.
- **Registration:** `CreatePublicKeyCredentialRequest(requestJson)` with the server `options` as they are. They are already in `toBrowserArray` form, which is the JSON format Credential Manager expects.
- **Build the server bodies from parsed fields**, not by forwarding `authenticationResponseJson` or `registrationResponseJson` raw:
  - Login forwards the whole `credential` to `WebAuthn::fromJson`, and the strictness of 5.3.12 towards extra members such as `clientExtensionResults` was not verified.
  - Re-encode every binary field as **unpadded base64url**.
- Treat `NoCredentialException` or user cancel as "no server call". The challenge just expires.

---

## 10. Guest wizard and claim

**[F]** Source: P17-D14/D15; `W app/Http/Requests/Api/V1/GuestReviewTripRequest.php`, `app/Actions/Api/ReviewGuestTrip.php`, `ResolveMobileGuestClaim.php`, `app/Planning/GuestClaims.php`.

- **Guest review.** `POST /v1/guest/trips/review` is public.
  - Body: the journey body with **outbound only**.
    - `return` is `prohibited` and fails with `Choose one of the available options.`.
    - `override` is `prohibited`.
    - `trip_end_kind` ∈ `nights | check_out | one_way`.
  - Plus `device_id` (required uuid), `guest_secret?` (≤128) and **`profile` (required)**.
    - `profile` holds the body-clock fields.
    - `habitual_bed` and `habitual_wake` are required.
    - Guest sleep becomes the trip override.
  - Identity: `draft_token`, `input_revision`, `review_revision`.
- **Secret.**
  - The first review mints a 256-bit `guestSecret`, returned **once** in `identity.guestSecret`.
  - Replace the stored secret **whenever** `identity.guestSecret` is non-null.
  - A wrong or missing secret with a non-null `draft_token` is 422 `review_expired`. It looks exactly like an expired draft, so it gives no oracle.
  - A fresh review (null token) whose secret matches a live draft or receipt for that `device_id` reuses it and returns `guestSecret: null`.
- **Guest price.**
  - `balance = CreditLedger::SIGNUP_CREDITS = 1` (`W app/Planning/Credits/CreditLedger.php:16`).
  - `unlimited:false`.
  - `canSave:true` whenever a plan exists. Here it means "you may continue to create an account".
- **Past arrival.** A guest gets `A new plan needs an upcoming arrival. Check your flight dates.`.
- **Claim on register.** Send `draft_token`, `input_revision`, `review_revision` and `guest_secret` with the registration.
  - The account, guest answers, signup credit and saved trip are created in **one transaction**.
  - `ApiSession.tripId` is set.
  - The claim is checked **before** the account fields. An expired claim returns `review_expired` with `Review your trip again before creating your account.` and creates nothing.
  - A durable claim receipt lasts 7 days (`api_guest_claims`). A retry with the same email and password returns the same session and `tripId`.
- **Claim on login or passkey login.** Send `draft_token` and `guest_secret`. `tripId` is set only if a receipt matches **this** user.
- **Signing in instead of registering.** Re-send the trip input to the **member** review, without the guest `profile` and override.
- The trip can be read only after email verification, because trip reads are `verified`.

**[R]** Store `guestSecret` and the draft identity in the encrypted store, scoped to "no account". Delete them after a successful claim.

---

## 11. Trip wizard: review, save and edit (plan generation)

**[F] Request body `ApiTripReviewInput`** (`W app/Http/Requests/Api/V1/TripJourneyRequest.php` and `ReviewTripRequest.php`):

```text
outbound: { legs: Leg[1..16] }            return: { legs: Leg[1..15] } | null
trip_end_kind: 'one_way'|'return_legs'|'nights'|'check_out' | null
trip_end_nights: 1..60 (req. if nights)   trip_end_check_out_date: Y-m-d (req. if check_out)
preparation_days: 0..4                     fast_delay_opt_in: bool
override: { use, habitual_bed 'H:i', habitual_wake 'H:i', chronotype early|intermediate|late }
latest_wake 'H:i'   feasible_night_bed/wake 'H:i' (paired)
pre_departure_busy_answered: bool   pre_departure_busy_intervals: [{start_local, end_local}] ≤32
profile: { body-clock patch fields } (member only; saved BEFORE calculating)
draft_token: uuid|null   input_revision: int≥0   review_revision: int≥1|null
Leg: from_iata, to_iata (exists:airports, upper-cased, different), depart_local, arrive_local ('Y-m-d\TH:i' airport-local),
     flight_number ≤12, seat_lie_flat, seat_window left|right, leave_for_airport_local, airport_transfer_min 0..65535,
     stay: { bed_available_mode arrival|declared, bed_available_from_local (req. if declared), check_out_local,
             late_check_out_or_day_room, arrival_transfer_min, transfer_mode passenger|self_driven|unknown, busy_intervals ≤32 }
```

- Unknown top-level keys fail with `This setting cannot be changed here.`.
- There are **no open jaws**: the first return leg must leave from the last outbound arrival. Otherwise `return.legs.0.from_iata` gets `Your flight home must leave from the airport you landed at.`.
- A new trip's last landing must not be in the past. Otherwise `review` gets `A new plan needs an upcoming arrival. Check your flight dates or add your return flight.`.
- There is no request DTO type for the review body (P17 handoff). iOS hand-writes `ApiTripReviewInput`, `GuestTripReviewInput.swift` and `TripWizardContracts.swift`.

**[F] Response `ApiTripReview`:**

- `identity {token, inputRevision, reviewRevision, sourceRevision, guestSecret}`.
- `status`: `ok | modified | needs_answers | no_plan`.
- `reasons: CopyLine[]`.
- `prompts: ApiReviewPrompt[]`. Each prompt has:
  - `id` = `kind:question:stayIndex:legIndex`;
  - `kind`, `blocking`, `required`, `text`;
  - `scope {journey, legIndex, stayIndex}`;
  - `target` `profile|trip`;
  - `fields[]`;
  - `answer` (a map of `bool|number|string|null`);
  - `shape {kind options|boolean|clock_time|intervals, options[], min, max}`;
  - `side`, `deviceMin`.
- `summary {line, adaptation, preparationDays, planStartUtc, planStartZone, planEndDate, endCopy, stayEnd}` or null.
- `price {state unlimited|free|create|edit|paid, cost, balance, unlimited, freeDirections}`. It is null when there is no plan.
- `canSave`, `saveBlockedReason` (only `Not enough credits`).
- `itinerary {legs[{journey, legIndex, departUtc, arriveUtc, flightMin}], layovers[…]}`.

Rules:

- `blocking` covers only `age_band` and `screening` (now only `shift_work`, P19). All missing ones arrive at once.
- One re-review after the answers returns either a plan or `no_plan`.
- Under-18 or recent shift work → `no_plan` with reasons.

**[F] Flow, synchronous** (`I plan.md:1360-1375`):

1. `POST trips/review`, first with `draft_token:null, input_revision:0, review_revision:null`. Then always send back `identity`.
2. `POST trips` with the identity → 201 `{tripId}`.
3. `GET trips/{tripId}`.

Lifetimes and limits:

- Drafts live 30 minutes, at most 5 per owner. The oldest is evicted, and its token then gets `review_expired`.
- Save receipts live 7 days.
- The plan is computed inline. The web review and save POST took 22–83 ms on S1–S6; the planner core takes 33 ms median and 227 ms worst case (`W research.md`).
- Review is **not read-only**: it writes `profile` first, and a later failure does not undo that.

**[F] Edit.**

1. `GET trips/{id}/input` returns the full stored input. This includes fields the wizard never shows. Send them all back, because a save replaces every leg and stay.
2. `POST trips/{id}/review` + `source_revision`.
3. `PUT trips/{id}`.

More rules:

- A completion, reopen or edit from another device changes `sourceRevision` → `review_expired`.
- Check-off rows survive an edit by step key. After a `PUT`, the `planInputHash` changes, so queued marks against the old hash get the 422 `form` error and must be re-keyed.
- Adding a return costs 1 credit when that direction needs a plan.
- **Duplicate** has no endpoint. iOS opens the member wizard prefilled from `trips/{id}/input` (WizardDuplicate board).

**[R]** Port `TripWizardErrors.journeyPath` and the iOS `MemberWizardStore` rules (30-minute "Ready" re-check, `savedTripId` persisted before the pull, no automatic retry) unchanged.

---

## 12. Trip reads, offline window and check-offs

**[F] `ApiTripListItem`:**

- `card: TripCardView`;
- `planInputHash`;
- `planStartUtc` (the first plan day, **including preparation**), `planStartZone`;
- `planEndUtc`, `planEndDate` (the last day's `labelDate`; never derive it from `planEndUtc`);
- `preparationDays`;
- `adaptation|null`;
- `keepOffline`;
- `detailVersion|null`, `checkState|null`. Both are set **only** when `keepOffline`.

**[F] Offline window** (`W app/Actions/Api/ReadMobileTrips.php:22-31`; `W config/offline.php`):

- `keepOffline = completed_at == null && (status == active || (status == upcoming && planStartUtc ≤ now + N days))`.
- `N = OFFLINE_MOBILE_PREFETCH_DAYS`, default **7**. The server may change N without an app release, so **use the flag; never hard-code 7**.

**[F] The three identities** (P14-D16/D17):

- `planInputHash`: whether a check mark is stale.
- `detailVersion`: xxh128 of the JSON of `trip`, `plan`, `notices` and `timeline`. It **excludes** `checkState`. Use it to decide whether to re-download a detail.
- HTTP `ETag`: the whole body, including marks.

**[F] Detail.**

- Returns **all** optional items, whatever `show_optional_items` is. The phone filters by `hiddenCategories` and `showOptionalItems` (P13-D3).
- Not included: `nowUtc`, `progress`, `categories`, `printGuide`, feed or export info.
- The list is complete and unpaginated. **A trip missing from a successful list means it was deleted.**

**[F] Check-off** (`W app/Http/Controllers/Api/V1/TripCheckController.php`):

- The response returns `checkState` and the same `detailVersion` as GET, so **no re-pull is needed**.
- The server stamps `marked_at`, so last writer wins.
- `null` undoes a mark. An absent mark with null is a no-op 200.
- Completed trips can still be marked.
- Unknown keys → `form`: `This step cannot be checked off.`.
- **`status` must be present in the JSON**, including as `null`.

**[F] iOS sync algorithm** (`I plan.md` "Sync sequence for one epoch", lines 449-460). Reuse it as written:

1. Flush the outbox in `(created_at, id)` order. A failed key blocks later rows with the same key: settings by section, check-offs by trip.
2. Pull `users/me`. If unverified, stop.
3. Pull `account`.
4. Pull `trips`. Tombstone missing cards. Never resurrect `deleted_locally`.
5. Set `synced_at`.
6. Evict non-`keepOffline` details that are not leased on screen and not referenced by pending outbox rows. Evict only after a **successful** list.
7. Fetch details only when they are missing or their `detailVersion` changed. Store the digest from the **detail** you actually received.
8. Re-key stale check-offs by `(type, local_date, ordinal)`.
9. Final 4xx → `syncFailure`.

**[R] For the timeline's performance** (the critical screen):

- Store detail JSON and parse it into an immutable domain model **off the main thread**, once per `detailVersion`.
- Keep `checkState` separate, so a check-off updates only progress and marks, not the parsed plan.
- `PlanView.blocks` is a flat array that refers to days by time. Pre-bucket blocks by `DayView.id` once, at parse time.

---

## 13. Account snapshot and settings

### 13.1 `GET /v1/account` → `ApiAccountSnapshot`

**[F]** Source: `W resources/js/types/App/Data/Api/index.ts`, `Data/Settings/index.ts`, `Data/Billing/index.ts`.

```text
{ id, name, email, hasVerifiedEmail,
  settings: { bodyClock: BodyClockAnswersView, sleepAnswered: bool, preferences: {hiddenCategories: BlockType[], showOptionalItems, timeFormat '12'|'24', language 'en'|'nl'},
              reminders: {light, avoid, caffeine, winddown, leadMin}, planCredits: {credits:int, unlimited:bool},
              policies: {termsVersion, privacyVersion, acceptedTermsVersion|null, acceptedTermsAt|null} },
  readiness: { variant 'needs-answers'|'excluded'|'adjusted'|'ready', missing[{question}], exclusions: CopyLine[], exclusionTarget, adjustments: CopyLine[], invalidSections[] },
  billing: { subscription: { tier free|monthly|annual, status free|active|trial|cancelled|past_due|unpaid|paused|expired, endsAt, renewsAt, canResume, canCancel, paymentFailed, unlimited } },
  security: { twoFactorEnabled, passwordRules, twoFactorConfirmedAt|null, passkeyCount (Phase 20) } }
```

Notes:

- When no rows exist, defaults are returned without writes: `timeFormat '24'`, `hiddenCategories []`, `showOptionalItems false`, language from the account or `en`.
- Show the Sleep step while `settings.sleepAnswered` is false.

### 13.2 Body-clock PATCH fields

**[F]** Source: `W app/Http/Requests/Settings/BodyClockRules.php:18-41`.

| Field | Rule |
|---|---|
| `habitual_bed`, `habitual_wake` | `HH:MM` |
| `free_day_status` | `unconfirmed`, `same_as_workdays` or `declared` |
| `free_day_bed`, `free_day_wake` | `HH:MM`, nullable |
| `chronotype` | `early`, `intermediate` or `late`, nullable |
| `age_band` | `under_18`, `18_64` or `65_plus`, nullable |
| `shift_work` | bool, nullable |
| `exclude_caffeine`, `flight_movement_breaks` | bool, required when present |
| `caffeine_habit` | `non_user` or `cups`, nullable |
| `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink` | 0–20, nullable |
| `caffeine_sensitive`, `caffeine_non_user_opt_in` | bool |
| `light_device` | `none`, `box` or `glasses`, nullable |
| `blue_blockers` | `no`, `rated` or `unrated`, nullable |
| `blue_blocker_mdfd` | 0.1–9.9, one decimal place |

- The legacy health keys (`bipolar`, `eye_disease`, `pregnancy`, `vte`, `sleep_phase_disorder`, `oral_contraception`, `caffeine_ruled_out`) are **ignored** by PATCH but **rejected** by review `profile` (P19-D12).
- The server validates the **merged** section. A first-ever partial sleep patch therefore fails: send the whole section while it is incomplete (P15 handoff).
- iOS validates locally before sending: an allowed key set, at least one key, booleans for the two preferences (`I DataAccess/SettingsPatchValidation.swift`).

**[R]** Build PATCH bodies as a `JsonObject` diff, as iOS does with `JSONObject`. Leave absent fields out and send explicit `JsonNull` to clear. Do not use data classes with defaults here, because they cannot tell "absent" from "null".

---

## 14. Billing, credits and paywall

**[F] Server**

- `W routes/billing.php`: `POST billing/checkout/{monthly|annual|credit}`, `portal`, `payment-method`, `cancel`, `resume`.
  - These run on the **app host behind the web session**. They are not on the API.
- Gateway: Lemon Squeezy (`BillingProvider::LemonSqueezy` is the only enum case). Webhooks: `POST billing/webhooks/{provider}`.

**[F] API**

- `ApiBillingSummary {subscription: SubscriptionView}`, `settings.planCredits {credits, unlimited}` and the review `price`.
- **No** checkout, prices, portal or hand-off endpoint (P14-D12, `I plan.md:1348`).

**[F] iOS**

- No StoreKit.
- "Plan & credits" shows the cached balance and subscription read-only (17h).
- Purchase, cancel and resume rows are deferred.
- Wizard "no credits" shows WizardReadyNoCredits with **no action** (iOS OQ 41).

**[F] RN** had `openBillingHandOff` (a one-time signed-in web link) behind `EXPO_PUBLIC_BILLING_LINK_OUT`, off by default. The backend never shipped that endpoint (`M plan.md:273,430`; `M docs/web-api-requests.md` §3 "billing magic link").

**[R] Android**

- Match iOS: read-only balance and subscription, no purchase UI, no external purchase link.
- This also keeps the app clear of Play's payments policy. Linking out to buy digital goods is restricted outside Google's alternative-billing and external-offers programs. **[R] — verify the current Play policy before release.**
- If in-app purchase is wanted later, Play Billing needs the following (none of it exists):
  - a new `BillingProvider` (`google-play`);
  - a bearer endpoint that accepts `purchaseToken`/`productId` and verifies it with the Google Play Developer API (`purchases.products` / `purchases.subscriptionsv2`);
  - Real-Time Developer Notifications (Pub/Sub push) feeding `WebhookTranslatorRegistry`;
  - acknowledging or consuming within 3 days;
  - mapping to `CreditLedger` and subscriptions;
  - a decision on cross-platform entitlement precedence.

---

## 15. Data export, terms, walkthrough, push and calendar

**[F]**

- **Data export:** only the web session route `GET settings/data.json` (`W routes/settings.php:45`). There is **no API**. iOS deferred it, and its `data-export` outbox kind is "unsupported, never acknowledged" (`I plan.md:2014`).
- **Terms:** gate on `ApiAccount.policies` (`acceptedTermsVersion` vs `termsVersion`). Accept with `POST terms/acceptance`. The policy text is on the website: `https://www.chronoflow.net/terms` and `/nl/terms`.
- **Walkthrough:** `ApiAccount.walkthroughSeen` and `POST walkthrough`. It works across devices.
- **Push:** there is no FCM or APNs endpoint. `settings/push-devices` is web-push with VAPID and the session. iOS uses **local** reminders (Phase 19). **[R]** Use local notifications through `AlarmManager` exact alarms or WorkManager, from `keepOffline` details only.
- **Calendar:** no API. RN and iOS do "Add to calendar" locally (iOS OQ 42 defers it). **[R]** On Android, use a `CalendarContract` insert intent if that is ever in scope.

---

## 16. Contract generation (for Kotlin)

**[F]**

- The source of truth is PHP `final readonly` DTOs with `#[TypeScript]` under `W app/Data/Api`, `Data/Plan`, `Data/Trips`, `Data/Settings`, `Data/Billing`, plus backed enums.
- `php artisan typescript:transform` writes `W resources/js/types/App/**/index.ts`; the manifest is `resources/js/types/typescript-transformer-manifest.json`.
- There is **no OpenAPI or JSON Schema**. P12-D12: "No second generator … OpenAPI dependency … is needed."
- Request bodies for the review and save endpoints have **no** TypeScript type (they are FormRequests).
- **The iOS generator** (`I Tools/contracts-gen/generate.py`):
  1. Reads each `index.ts` via `git show <PIN>:resources/js/types/App/<path>/index.ts` for 7 paths.
  2. Parses `export type X = {readonly k?: T, …}` and literal unions.
  3. Walks the dependency graph from the roots `ApiTripListItem, ApiTripDetail, ApiTripInput, ApiTripReview, ApiTripSaveResult, ApiTripInputSnapshot, ApiAirportSearchResults`, plus every enum under `Planner/Output` and `Enums`.
  4. Emits Swift structs and string enums, each with an `unknown(String)` fallback case. It **rejects unsupported TypeScript syntax**.
  5. Hand-checked overrides:
     - `BlockView.params`: a 20-way union decoded by the sibling `type`;
     - `CopyArg.value`: `string | number`;
     - `CopyLine.args`: `(CopyArg | CaffeineCopyArg)[]`;
     - `ApiReviewPromptShape.min/max` → `Double?`.
  6. Overlay: `ScreeningQuestion = 'shift_work'` only.
  7. Renames, for example `ApiTripListItem → TripListItem`.
  8. `make contracts-check` diffs the regenerated output against the committed file.
  - Auth, account, passkey and wizard-input DTOs are **hand-written** Swift (`I AppCore/Contracts/*.swift`, about 1.1k lines).

**[R]** Write `tools/contracts-gen/generate_kotlin.py`, a port of the iOS script.

- Same pin, same roots, plus the auth and account DTOs, which are also in `Data/Api/index.ts` and parse with the same grammar.
- Emit `@Serializable data class` with `@SerialName` for snake_case.
- Enums become `@Serializable(with = …)` value classes or sealed types with an `Unknown(raw)` fallback.
- `BlockView` gets a custom `KSerializer` that reads `type` before it decodes `params`.
- `CopyArg` and `CaffeineCopyArg` are told apart by the `drinks` key.
- Prompt `answer` becomes `Map<String, JsonPrimitive>`.
- Check in the output, and add a Gradle `contractsCheck` task that runs the script in `--check` mode in CI.
- Use one `Json { ignoreUnknownKeys = true; explicitNulls = false }` for decoding.
- For requests, encode with `explicitNulls = true` where null has meaning: `ApiCheckOffInput.status`, the identity fields. Build PATCHes as `JsonObject`.
- Also generate `PlanCopy` `strings.xml` (en and nl) from `W lang/{en,nl}.json` at a pinned commit, as `I Tools/PlanCopyGen` does.
  - Laravel `|` plurals and `{1}`/`[5,*]` ranges need a small selector, because Android `plurals` cannot express ranges.

---

## 17. What can be reused from the iOS research and plan

The user asked about this directly. The iOS repository already holds a lot of verified, Android-relevant material:

| iOS artefact | Reuse for Android |
|---|---|
| `I plan.md` §"API contract and offline sync" (lines 435-470) | **Directly.** The transport rules (no cookies, no redirects, 15/90/3 s), the error mapping, the operation→endpoint table, the 5-step sync algorithm, eviction and outbox rules, and `synced_at` readiness. All of it is platform-neutral. |
| `I plan.md` §"Account schema v1" (lines 382-433) | **Directly as the Room schema.** The tables `account`, `trips`, `plan_versions`, `check_marks`, `settings`, `drafts` and `outbox`, with their columns, primary keys and outbox kinds. These were derived from RN's final schema. |
| `I plan.md` §"Trips source contracts" 1.1–1.3 and the gaps (lines 1317-1540) | **Directly.** Trip routes, the synchronous generation flow, response shapes, the three identities, trip error keys, draft and receipt lifetimes and the offline window, all with `W` line references. |
| `I plan.md` "Open questions" (lines 2376-2426) | Decisions to inherit: OQ 21 (server password policy, no client policy), 22/27 (reset gives no session; say that other devices are signed out), 25 (link entry), 28 (Dutch copy source order), 29 (sign-out with pending rows), 31 (no fake endpoints for unsupported outbox kinds), 34 (plan-copy generation), 40 (one recovery draft row), 41 (no billing hand-off), 42 (calendar deferred). |
| `I Tools/contracts-gen/generate.py` | Port it to Kotlin output (§16). The TypeScript parser, graph walk, overrides and check mode carry over unchanged. |
| `I Tools/PlanCopyGen/*` (`format-units.json`, `support-keys.json`, `no-health-data.json`) | Port it to emit `strings.xml`. The key allowlists can be reused as they are. |
| `I DataAccess/API/*.swift` (508 lines) | A line-by-line template for the Retrofit/OkHttp layer: endpoint paths, bodies, expected status codes, options validation and error mapping. |
| `I DataAccess/TripWizardErrors.swift`, `SettingsPatchValidation.swift` | Port them verbatim to Kotlin. |
| `I DataAccess/Mock/*` (MockDataSource, fixtures, rate limiter, guest review) | Shapes for a Kotlin `FakeDataSource` (300–800 ms latency, offline switch, `failNext`). The fixture JSON in `Tests/DataAccessTests/Fixtures` can be reused as contract test inputs. |
| `I AppCore/Contracts/ProfilePreferences.swift` | The legacy-key cleaning (`cleanLegacyJSON`) and the prompt filtering for the No-health-data change. |
| `I Docs/*.md` (wizard, settings, terms, reminders) | Decisions about board fidelity. The behaviour is the same on Android. |

What does **not** carry over: the AASA, Keychain, `URLSession` and `ASAuthorization` specifics. Their Android counterparts are in §6 and §9.

---

## 18. Android implementation recommendations (summary)

**[R]**

1. **Stack.**
   - OkHttp 4/5 + Retrofit with the kotlinx.serialization converter, or Ktor client with the OkHttp engine. Retrofit is the simpler fit.
   - Redirects off, no cookie jar, no disk cache.
   - Timeouts per call: 15 s, 90 s, 3 s.
2. **One `ApiDataSource` interface** that mirrors iOS `DataSource`, with API and fake implementations chosen by build flavor (`api` / `mock`). This mirrors `CHRONOFLOW_DATA_SOURCE`.
3. **Base URL** comes from `BuildConfig`: `https://api.chronoflow.net/v1` by default. Release must be HTTPS. Reject URLs with userinfo, a query or a fragment.
4. **Errors.** A sealed `DataFailure` with `Offline`, `Unauthorized`, `EmailUnverified`, `Validation(fields)`, `Throttled(retryAfter)`, `Conflict`, `ReviewExpired`, `NotEnoughCredits`, `SaveCapacity`, `ProfileInvalid` and `Server(status)`.
5. **Outbox.** WorkManager unique work for flushes, with `NetworkType.CONNECTED`. In the foreground, trigger sync directly and debounce it. Do not run hot retry loops.
6. **Security.**
   - Keystore-backed token storage.
   - Exclude the token, `device_id`, guest secret and account DB from backup.
   - `FLAG_SECURE` on recovery codes and the 2FA secret.
   - Never log secrets.
7. **Passkeys and App Links.** Blocked by the server's assetlinks route and Android origins (§9.4). Ship with password + 2FA first and keep the passkey UI hidden behind a capability flag.

---

## 19. Risks

- **The Phase 20 contract is unmerged.** If it changes before merge, the Android passkey-management overlay drifts. Mitigation: hide it behind `passkeyCount` being present, as iOS does.
- **`/v1` breaks in place.** P19-D12 changed body-clock fields without bumping the version. An Android build that ships could break on a later server change. Mitigation:
  - unknown-tolerant decoding everywhere;
  - enum `Unknown` fallbacks;
  - the CI `contractsCheck` against the pin;
  - ask the web side to version once a mobile app ships.
- **No server-side Android association** (assetlinks.json, apk-key-hash origins). This blocks passkeys and App Links on Android. It needs a web change, which is outside this repository.
- **The AASA lacks `applinks`.** iOS universal links for reset and verify will not work until the server adds them. The same gap applies to Android.
- **Auto Backup restoring `device_id`** silently revokes the old device's token (§6).
- **A shared limiter budget.** `api-credential` (6/min) is shared across the password, 2FA, profile, delete and passkey routes. A user who adds a passkey right after changing their password can hit a 429. Show the countdown.
- **A large list body.** `GET trips` presents every trip's plan for window trips (server median 126.9 ms for 20 trips). Use ETag revalidation and parse off the main thread.
- **Dutch completeness.** Every server key must exist in the generated Android string set. A missing key must fall back to the generic error, never to the English sentence.

---

## 20. Open questions (with recommended defaults)

| # | Question | Recommended default |
|---|---|---|
| A1 | Which web commit should the Android contracts pin to? | `e36368ec` (the same as iOS, = `origin/main`), plus a hand-written Phase 20 passkey-management overlay. Move the pin when Phase 20 merges. |
| A2 | Android `applicationId`? RN used `net.chronoflow.app` (`M android/app/build.gradle:90-92`); iOS develops under `net.chronoflow.app.native`. | Use `net.chronoflow.app.native` during development, so it can sit beside an RN Android build, mirroring iOS OQ 4. Decide the release ID together with the Play listing. Feed the decision into assetlinks. |
| A3 | Should Android support passkeys at launch? | Not until the web adds `/.well-known/assetlinks.json` and `android:apk-key-hash:` allowed origins (§9.4). Build the Credential Manager code path behind a flag, and ship password + 2FA. |
| A4 | Are App Links for the reset and verify emails in scope? | Yes, as the product decision of 2 Nov. Add intent filters for `/reset-password/*` and `/email/verify/*` with `autoVerify`. They only take effect once the server serves assetlinks with `handle_all_urls`. Until then, the browser fallback plus a `users/me` poll on resume. |
| A5 | In-app purchases (Google Play Billing)? | No. Read-only balance and subscription, and the "Not enough credits" state without an action, as iOS does (OQ 41). |
| A6 | Use HTTP ETags (iOS does not)? | Yes, for `GET trips` and `GET trips/{id}`, stored in Room per account. They are optional for correctness. |
| A7 | How do we test against a local server from the emulator? `api.chronoflow.test` does not resolve inside the emulator. | Point the debug flavor at `https://api.chronoflow.test/v1`, mapped through `adb reverse` or the emulator's `10.0.2.2` with a hosts entry or a Host header, and trust the Herd CA through a debug-only `network_security_config`. Otherwise use the fake data source. Never point at production from tests. |
| A8 | Is `device_name` user-visible? | It appears only in server token rows today; no screen lists sessions. Use `Settings.Global.DEVICE_NAME`, falling back to manufacturer + model. |
| A9 | Data export, trip delete, complete and reopen, calendar? | Out of scope until endpoints exist (iOS OQ 31 and OQ 42). Never fake success. |
| A10 | Does `WebAuthn::fromJson` in passkey login reject extra members such as `clientExtensionResults`? | Unverified. Send only the documented members, re-encoded as unpadded base64url. |
| A11 | Should Android send `Accept-Language`? | No. The API ignores it since P15-D16. Translate on the device. |

---

## Appendix A: Key file index

| Purpose | Path |
|---|---|
| API routes | `W routes/api.php` |
| Rate limiters | `W app/Providers/AppServiceProvider.php:105-228`, `BillingServiceProvider.php:25-34` |
| Error renderer | `W app/Http/Responses/Api/ApiExceptionResponse.php` |
| API DTOs (TypeScript, generated) | `W resources/js/types/App/Data/Api/index.ts` (396 lines), `Data/Plan`, `Data/Trips`, `Data/Settings`, `Data/Billing`, `Enums`, `Planner/{Input,Output}` |
| API requests (validation) | `W app/Http/Requests/Api/V1/*.php` (27 files) |
| API actions | `W app/Actions/Api/*.php` (31 files) |
| Specs and handoffs | `W docs/ui-plan/phase-12…20-*.md`, `W .ai/rules/api.md` |
| Token issue and refresh | `W app/Actions/Api/IssueMobileToken.php`, `W app/Listeners/RefreshMobileTokenExpiry.php` |
| Passkey config, AASA | `W config/fortify.php:148-153`, `W config/services.php:29-36`, `W app/Http/Controllers/AppleAppSiteAssociationController.php` |
| Offline window | `W config/offline.php`, `W app/Actions/Api/ReadMobileTrips.php` |
| iOS client | `I Packages/ChronoFlowKit/Sources/DataAccess/API/*.swift` |
| iOS contracts | `I Packages/ChronoFlowKit/Sources/AppCore/Contracts/**` (generated: `Generated/TripPlan.swift`, 129 types) |
| iOS generator | `I Tools/contracts-gen/generate.py`, `I Tools/contracts-check`, `I Makefile:101-105` |
| iOS API spec | `I plan.md:362-470`, `:1317-1540`, `:2376-2426` |
