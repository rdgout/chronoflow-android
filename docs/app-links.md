# Android App Links

Phase 5c, 2026-11-18. The app declares links for password reset and email verification. Server association (S1) is still required before Android can open ordinary web links in a release installation automatically.

## App declaration and delivery

`MainActivity` uses `singleTask` and exactly one `android:autoVerify="true"` filter:

```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="app.chronoflow.net" />
    <data android:pathPrefix="/reset-password/" />
    <data android:pathPrefix="/email/verify/" />
</intent-filter>
```

The final release package is `net.chronoflow.app` (user, 2026-11-23). The server association will list this production package and `net.chronoflow.app.debug` for local/staging, each with its matching certificate. There is no custom scheme, HTTP alternative, wildcard host or explicit port. The trailing slashes are deliberate: a tokenless `/reset-password?email=…` stays on the web even though the in-app parser can represent it as an expired link (OQ-A207).

`verifyReleaseManifest` enforces the exact filter. `AppLinkIntentFilterTest` exercises Android intent resolution for both prefixes. The parser independently validates the full origin and link shape; a matching manifest prefix alone does not make a valid credential.

Cold launch captures a link in memory, then clears data from the activity's launch intent. A warm `onNewIntent` captures its parameter without calling `setIntent`, so the original launch intent remains unchanged. Restored and history launches cannot capture the link again. See [the reset flow](auth.md#password-reset-and-app-links-phase-5c-2026-11-18) for hand-off and session routing.

## Server association (S1)

Serve `GET https://app.chronoflow.net/.well-known/assetlinks.json` like the AASA route: HTTP **200**, `Content-Type: application/json`, no redirect, cookies or authentication, and available outside maintenance mode.

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

Replace the placeholder with the **Play app-signing certificate** fingerprint. The debug package entry is for local/staging; the committed debug key stays outside the production association (OQ-A38). The upload certificate is a different key; add it only if locally signed release builds must also verify (OQ-A208). Update this file when the application ID or signing key changes.

`common.get_login_creds` also associates the app for passkeys (A15). It does not enable the shipped passkey capability: that remains off until S1–S3 and the checks in [Passkey sign-in](passkeys.md) are complete. Until S1 ships, ordinary links fall back to the existing web reset and verification pages.

## Testing and observed result

Read association state without changing it:

```sh
adb shell pm get-app-links <pkg>
```

For a dedicated verification test, the reset, re-verification and user-selection commands are:

```sh
adb shell pm set-app-links --package <pkg> 0 all
adb shell pm verify-app-links --re-verify <pkg>
adb shell pm set-app-links-user-selection --user cur --package <pkg> true app.chronoflow.net
```

Run those state-changing commands only inside the test that needs them. A user selection is not proof of server verification. The emulator verifier cannot reach the real domain in this environment, so Phase 5c did not reset verification state, force re-verification or change user selection.

To exercise delivery explicitly from the host shell, substitute a test URL and package:

```sh
adb shell "am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://app.chronoflow.net/reset-password/<token>?email=<encoded>' <pkg>"
```

Use synthetic or debug mock links; do not retain a live reset secret in shell history. Inside `UiAutomation.executeShellCommand`, pass the URL without shell quotes: that API does not perform host-shell quote removal. The journey reads command output only in memory and does not print the link.

On `pixel` / API 36 / debug, `make targeted ONLY="app:PasswordResetLinkJourneyTest"` passed in **3.435 s** (tier **9.43 s**), under the device lock. It delivered the mock link to the running activity through explicit-package `am start`, reached Reset done and Sign in, and asserted signed-out presence, no session blob and zero StrictMode IO violations. Manifest resolution and this delivery are the Phase 5c proof; real-domain association was not verified.

The separate read-only state probe reported an unavailable package. The command and error below use the final package name (user, 2026-11-23); the archived log retains the identity at that commit:

```sh
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 shell pm get-app-links net.chronoflow.app.debug
```

```text
Error: package net.chronoflow.app.debug unavailable
```

The original output is retained in `build/phase5c-validation/app-link-state.txt`; it provides no association-state evidence. The journey's passing result is retained separately in `build/phase5c-validation/password-reset-journey-fixed.xml` and its timing JSON.
