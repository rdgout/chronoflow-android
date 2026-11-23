# Passkey sign-in

`PasskeyCapability.SERVER_READY` is **false**. Release and perf use `PasskeyCapability.Shipped`; debug alone can supply a seed override. Sign in composes neither its passkey button nor its divider while the capability is off, and submission is a no-op. Mock builds use `DeterministicAssertionProvider { throw Unavailable }`, so they show the designed Server banner without opening Credential Manager. API builds use the lazy Credential Manager client and its weak Activity anchor.

The Android ceremony cannot be verified against the server yet (A15, OQ-A12/OQ-A21). JVM/mock acceptance does not close device verification.

## Server prerequisites

| Item | Required evidence before enabling the capability |
|---|---|
| S1 | `https://app.chronoflow.net/.well-known/assetlinks.json` serves `delegate_permission/common.get_login_creds` for `net.chronoflow.app`, with the approved release/app-signing certificate SHA-256 fingerprint. Never publish the committed debug-key fingerprint (OQ-A38). Record the deployed response and certificate identity. |
| S2 | The server's allowed origins include `android:apk-key-hash:<base64url(sha256(cert DER))>` for that same signing certificate. Record the deployed configuration and a successful Android assertion verification. |
| S3 | Passkey management is merged and deployed. Record the server revision and management verification. |

Compute the S2 hash from the certificate, not its hexadecimal fingerprint text:

```sh
keytool -exportcert -alias <alias> -keystore <ks> | openssl sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '='
```

Keep keystore passwords outside commands, source control and logs. This phase changes no server file and makes no deployment claim. App Links documentation belongs to Phase 5c.

## Device verification

Use a Play-enabled physical device with a configured credential provider and a passkey for `app.chronoflow.net`. The pixel `google_apis` AVD proves functional UI behavior, not a real Credential Manager/server ceremony.

The test APK's actual package and certificate must match S1/S2. The ordinary debug APK is `net.chronoflow.app.debug`; its package and local/staging certificate must match that environment’s S1/S2 configuration (user, 2026-11-23). It does not satisfy the production package/certificate association. Prepare an explicitly approved matching device-test signing/package configuration before attempting the ceremony; do not add the debug fingerprint to the production association or change the shipped gate to work around this prerequisite.

Build that debug-capable test configuration with `-Pcf.dataSource=api` (the normal debug compile entry is `./gradlew :app:assembleDebug -Pcf.dataSource=api`). Apply the override through the debug seed receiver so `DebugSeedRouting.current` is populated before the new auth model is constructed:

```sh
adb shell am start -n net.chronoflow.app/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 0199a3c0-0000-7000-8000-000000000005 \
  --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_AUDIT_SCREEN sign-in \
  --es cf.passkeyCapability 1
```

This command assumes the approved test APK uses the canonical package above. The extra accepts `1`/`true` or `0`/`false`; other values mean no override. The receiver and override are absent from release. Seed application rebuilds the graph, then installs the seed; constructing the graph does not construct auth. The capability is read when the new auth model is built.

Verify the system prompt, correct RP and required user verification; a successful session must pass through the stored verify → terms → walkthrough → app gate. Cancel and no-credential responses must remain silent. Retry must fetch fresh options. Check a non-cancel failure against the designed Server banner. Retain device/build/server revisions and redacted outcomes, never request/response bodies, assertions, challenge IDs or credentials.

## Request and failure behavior

Requests serialize the five parsed server options. Responses are parsed into `ApiPasskeyAssertion`: binary fields use canonical unpadded base64url, `id == rawId`, empty `userHandle` is omitted, and unrecognized members are dropped. Assertion and response descriptions redact their contents.

`GetCredentialCancellationException` and `NoCredentialException` become `Canceled`; other Credential Manager failures become `Unavailable`. Missing Activity and a non-public-key credential also become `Unavailable`; overlapping client calls become `Busy`. Coroutine cancellation is rethrown. Every sign-in attempt requests fresh options; backend credential/challenge errors use the existing Server banner.

Only `:core:data` depends on `androidx.credentials` and its Play-services adapter. Credential Manager is created on its first call, with UI attached to the current Activity. The AAR consumer rule and `app/src/main/keepRules/credentials.keep` both appear in the merged release configuration. Phase 5b's dependency size measurement and native-library gate are in [Startup performance](perf-startup.md#release-size).

## Capability flip

After S1–S3 and the physical-device checks have recorded evidence, change `SERVER_READY = true` and update `ReleaseReadinessTest.passkeyCapabilityIsShippedOff` and its assertions in the same signed commit, linking that evidence here. Until then keep the constant false and every shipped entry point hidden. A debug seed or mock test is not evidence for flipping the shipped capability.
