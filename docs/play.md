# Play readiness

## Identity

ApplicationId `net.chronoflow.app`; versionName **0.1.0**, versionCode **10000**, from version.properties. minSdk **29**, targetSdk **36**, compileSdk **37**. OQ-A1 is resolved (user, 2026-11-23): RN never shipped on Play, so this is the final applicationId. Debug remains `net.chronoflow.app.debug`; server assetlinks will list production and local/staging debug with their matching certificates. The RN comparison now uses separate installation passes ([protocol](perf-rn.md)). No version was changed.

Enrol in Play App Signing: Google holds the app-signing key; the upload key stays outside the repository. [Release signing](release.md#upload-key) describes CF_UPLOAD_* and the signed/unsigned proofs. No production key was created by the worker.

## Data safety

Answers from research 14 §9.2 and OQ-A41:

| Play category → type | Collected | Shared | Optional | Purposes |
|---|---|---|---|---|
| Personal info → Name | Yes | No | No | App functionality, Account management |
| Personal info → Email address | Yes | No | No | App functionality, Account management |
| Personal info → User IDs | Yes | No | No | App functionality, Account management |
| Personal info → Other info (age band) | Yes | No | No (it blocks plans for under-18s) | App functionality |
| Health and fitness → Health info (sleep schedule, shift work, caffeine habits) | Yes (conservative) | No | No | App functionality |
| App activity → Other user-generated content (trips, check-offs) | Yes | No | No | App functionality |
| Device or other IDs (app-generated device id) | Yes | No | No | App functionality, Fraud prevention / security (one token per device) |

Health info: Yes (conservative) follows OQ-A41's default for the Console category. Product wording stays within the no-health-data decision: age band and shift work are the only questions, and copy promises a clock shift without symptom-relief claims.

Security: encrypted in transit **yes** (HTTPS only); deletion **yes**, through Settings → Account → Delete account and `https://app.chronoflow.net/settings/account`, subject to the S4 verified-email gap below; Families policy **no**. Privacy policy: `https://chronoflow.net/privacy`, the English URL in both app languages, matching iOS at the pin.

Not collected: location, contacts, photos, calendar, audio, messages, financial details, advertising ID, analytics or crash logs. Debug-only exit metadata stays on the device; no crash/analytics SDK is shipped.

Encryption at rest: file-based encryption is mandatory on devices that launch with Android 10+ (minSdk 29); no SQLCipher. Session credentials use Keystore encryption. No new health-data collection or service was added by this phase.

## Policy checklist P1–P11

| # | Requirement | Status | Evidence |
|---|---|---|---|
| P1 | Target API: targetSdk 36 required since 2026-08-31 | done | Final merged-manifest gate checks SDK 29/36; compileSdk 37. [Release manifest](release.md#release-manifest). |
| P2 | 16 KB memory pages: new apps and updates targeting Android 15+ must support 16 KB pages | done | Size gate allows only libandroidx.graphics.path.so (OQ-0a-22), with no other native library. `zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release-unsigned.apk` → `Verification successful` (worker, local host, 2026-11-22). [Size evidence](release.md#size). |
| P3 | Account deletion: an app that creates accounts must offer deletion in the app and a web link in the Data safety form | pending (server S4) | Verified accounts delete in Settings → Account (10d); web URL above. At web pin e36368ec, routes/api.php:83–84 and routes/settings.php:22–26 both require verified email. An unverified account cannot use these deletion routes. |
| P4 | Health apps declaration: every app must complete it, even with no health features | pending (user) | Console wording: sleep/wellness management for clock-shift trip planning; no medical-device, symptom-relief or Health Connect claim. Keep the no-health-data decision and conservative category answer above together. |
| P5 | App access: reviewers need credentials when the app is behind login | pending (server S5) | Supply a production demo account with 2FA off, verified email, accepted terms, an active trip and credits. The perf mock account does not satisfy this requirement. |
| P6 | Target audience and content | pending (user) | Enter 18+, IARC answers all “no”, no ads; the app is outside the Families policy. |
| P7 | Payments | done | No Play Billing, purchase or link-out; credits read-only and NoLink variants. Real release APK checker: `INFO billing: absent` (Task 6). |
| P8 | Exact alarms / notifications | done | Manifest gate excludes exact-alarm permissions. POST_NOTIFICATIONS is requested in context by the landed reminders flow. API 29–30 exact scheduling requires no sensitive exact-alarm permission; API 31+ uses inexact alarms. |
| P9 | Data safety | pending (user) | Prepared answer table and deletion/privacy URLs in this document; enter them in Console. |
| P10 | Credential Manager / passkeys | pending (server S1) | S1/S2/S3 remain dependencies. Shipped PasskeyCapability.SERVER_READY is false; release-readiness tests keep every shipped entry point hidden until those dependencies and acceptance evidence exist. |
| P11 | Pre-launch report | pending (user) | Read accessibility and stability findings after the first internal upload and before every promotion; App access needs S5. |

## assetlinks.json for S1

Serve `https://app.chronoflow.net/.well-known/assetlinks.json` as HTTP 200 / application/json, without a redirect or authentication. JSON from [App Links](app-links.md#server-association-s1):

```json
[{
  "relation": [
    "delegate_permission/common.handle_all_urls",
    "delegate_permission/common.get_login_creds"
  ],
  "target": {
    "namespace": "android_app",
    "package_name": "net.chronoflow.app",
    "sha256_cert_fingerprints": [
      "<Play app-signing certificate SHA-256, AA:BB:… form>"
    ]
  }
}, {
  "relation": [
    "delegate_permission/common.handle_all_urls",
    "delegate_permission/common.get_login_creds"
  ],
  "target": {
    "namespace": "android_app",
    "package_name": "net.chronoflow.app.debug",
    "sha256_cert_fingerprints": [
      "<Local/staging signing certificate SHA-256, AA:BB:… form>"
    ]
  }
}]
```

The debug entry is for local/staging; never the committed debug key in the production association (OQ-A38). Add the upload key only if sideloaded release builds must verify links (OQ-A208, formerly 5c OQ-5c-4). Redo the association on an applicationId or key change. Publishing this file does not enable the shipped passkey capability; S1–S3 and Play-signed acceptance remain required. No server association or verifier state was changed by this phase.

## Server dependencies S1–S6

| # | Change | Needed for | Status |
|---|---|---|---|
| S1 | Serve assetlinks.json with handle_all_urls and get_login_creds for the final package and app-signing SHA-256 | Reset/verify App Links and passkeys | pending (server S1) |
| S2 | Add android:apk-key-hash:base64url(sha256(cert)) to passkeys.allowed_origins | Passkey sign-in and registration | pending (server S2) |
| S3 | Merge mobile API passkey management | Passkey management | pending (server S3) |
| S4 | Drop verified from DELETE /v1/account and the web delete route | Account deletion for unverified accounts, P3 | pending (server S4) |
| S5 | Create the production demo account described in P5 | Play review | pending (server S5) |
| S6 | Add the iOS AASA applinks entry | iOS universal links | pending (server S6) |

## Tracks

Internal testing first (at most 100 testers), then closed testing: at least 12 opted-in testers for 14 continuous days when required for a personal developer account created after 2023-11-13 (OQ-G11). Open testing is unused. Promote production in stages **10% → 50% → 100%**, halting on vitals thresholds: crash **1.09%**, ANR **0.47%**, **8% per model** (research 14 §8.2/§11.5). Read each pre-launch report before promotion.

Phase 13 evidence (2026-11-22): host `make release-check` passed, and `make release-smoke` passed after 40ba540f and again after 3f3b5155 with all three methods < 10 s, unchanged pids and no native crash. Host `make check` passed at 0660f83a and again at 4d1abfea; the upload gates below apply to the final upload artifact.

## Pre-upload checklist

- [ ] Complete the remaining Phase 13 host functional/performance evidence; every missing or failed hard gate is resolved. Profiles and their strict guard are committed in c8e7cfb3; their refresh after 89f3dd7f remains open. Host standalone make check passed. R9 quick median is 44.33 s; check has two passing runs (245.89 / 284.79 s), followed by a LaunchHandOffTest failure, so the full timing series remains open.
- [ ] `make release-check` passes (release unit tests, manifest, APK content, size).
- [ ] `make release-smoke` passes on the minified build; all three methods < 10 s, unchanged pids and no native crash.
- [ ] The host supplied all required metrics for `make perf`, `make perf-timeline` (light/dark/font 1.3) and `make perf-rn` at c8e7cfb3. Refresh launch/profile verification after 89f3dd7f; emulator measurements do not close device gates.
- [x] Host `make size` is OK, 4,156,988 B; enableSplit=true rejected `splits/base-en.apk`, the plugin was restored to false, and normal size passed. Proof: `build/phase13-host-evidence/t13-split.log` and `t13-size.log`.
- [ ] Every later upload runs `make version-bump PART=build` or a larger part, then commits `chore(release): bump version to <name> (<code>)`. The first upload remains 0.1.0 / 10000 (OQ-13-10); this phase does not bump it.
- [ ] Set all CF_UPLOAD_* outside the repo, then `./gradlew :app:bundleRelease`; verify with `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab` and inspect the intended upload certificate.
- [ ] Complete Console answers, production App access (S5), deletion-policy work (S4) and Play App Signing; upload manually to internal testing.
- [ ] On the internal build against the real API: sign in, open a trip, check off a step (OQ-13-18). The mock smoke does not prove API DTO serialization at runtime.
- [ ] Read the pre-launch report before promotion.
- [ ] **Open blocker before the first production upload:** one passing `PERF_CLEAR=1 DEVICE_CLASS=reference make perf-timeline` at 120 Hz and one passing `PERF_CLEAR=1 DEVICE_CLASS=lowEnd make perf-timeline`, or the user's explicit waiver recorded in plan.md OQ-A45. These device proofs have not been supplied.
