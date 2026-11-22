# Phase 13 brief: Release build, profiles and performance gates

- **Branch:** `feat/phase-13-release-perf`, from the stack top once 5b, 5c, 6e, 7, 9h, 10d, 11b and 12c are all in it (and through them every earlier phase). **Wave:** 16. **Milestone:** M6.
- **Plan entry:** `plan.md` "### Phase 13: Release build, profiles and performance gates" (l.3598–3645 at `1c0333db`; R1). This brief expands it. Where they differ, plan.md wins; report the difference in your hand-back.
- **Boards:** none. This phase adds no screen, no visible string and no token. `_design/canvas/canvas.json` has no Play, store or release board (checked 2026-11-16: no title matching Play/Store/Release/Listing/Privacy/Safety). Never draw a store listing, a privacy screen or any UI here.
- **API endpoints:** none are called by new code. The perf variants use the mock data source (4b OQ-4b-13), never a server. `docs/play.md` cites endpoints only as data-inventory facts.
- **Tests:** every test you write follows AGENTS.md "Tests" (cheapest layer; JVM < 1 s, Robolectric < 5 s, emulator < 10 s; exact queries; `waitUntil…`/`device.wait(Until…)`, never `Thread.sleep`/`SystemClock.sleep`; seeded launches; one fix round that changes nothing means stop and rethink). Read it before Task 1.
- **Who runs what:** your sandbox may be unable to run the Gradle daemon, `adb` or the emulator (`docs/tooling.md`). Then write the code, run what you can, and hand the exact commands to the orchestrator, who runs them and feeds back the output (CLAUDE.md). "No emulator was run" never closes this phase.
- **Brief written:** 2026-11-16, against plan.md, AGENTS.md, CLAUDE.md, research 04/11/12/13/14, the 0a, 0e, 1e, 3a, 3b, 3c, 4c, 4e, 5b, 5c, 6b–6e, 7, 9g, 11a, 11b briefs, iOS at `f391a1fa`, the RN release APK and `adb devices` on this Mac.
- **Checked 2026-11-16 (adversarial pass against iOS `f391a1fa`, web `e36368ec`, the RN APK, research 04/11/12/13/14, plan.md, AGENTS.md and the 0a, 1e, 3a–3c, 4c, 4e, 5c, 6a, 6c, 7, 9c, 9g, 12b, 12c briefs):** 12b's receivers are not exported, so the exported allowlist is MainActivity plus the `DUMP`/`BIND_JOB_SERVICE` guards; the wizard journey no longer moves 9c/9g debug code (it lives in `feature/wizard/src/debug`, and `WizardStartOverride` is a main seam) but adds a perf copy with an equality test; perf-only tests moved to `src/testDebug` (`src/test` also compiles for `testReleaseUnitTest`); `PerfJourneyExtras.parse` no longer calls `Log`; 7's `startup` journey is kept and `startupSignedOut` added; every new profile block cold-starts and the wizard block tolerates quiet recovery; the 12c first-trip offer is dismissed; forbidden seed strings use a `CF_` prefix rule and debug packages a `.debug.` segment rule; the billing line moved into Task 6; the repo-key grep no longer matches itself; `pm clear` tolerates a missing package and in-test clears refuse physical devices; `KiB` units; the web delete route is cited at the pin; OQ-13-16…18 record the plan.md "same `StartupBenchmark` harness" difference, the S3→S1 trip choice and the mock-only R8 smoke.

Tags: **[F]** a fact verified in a source file or on this Mac (path given). **[R]** a decision of the plan or this brief. **[verify]** a library or tool detail you confirm on the first run; if it differs, take the stated fallback and record what you found in `docs/release.md` "Tooling notes" (release items) or `docs/perf-startup.md` "Tooling notes" (perf items).

---

## 1. Goal

When this phase ends:

1. **The release build is shippable.** `bundleRelease` signs with an upload key read from `CF_UPLOAD_*` Gradle properties or environment variables, and builds unsigned on a fresh clone. No key other than the committed `app/debug.keystore` is in the repository. `version.properties` has a scripted bump rule. The R8 keep rules are reviewed, guarded by a test, and proven on a minified build by a release smoke test. The merged release manifest matches the final allowlist exactly. The bundle has no language split, no `.so` file except the OQ-0a-22 allow-list (`libandroidx.graphics.path.so`; R4) and a download size ≤ 8,000,000 B for arm64/xxhdpi/en. The release APK contains no debug, seed, mock, perf or test code and no seed strings, and `Log.v/d/i` calls from our code are stripped. `ApplicationExitInfo` logging exists and is debug-only.
2. **Profiles cover the main journeys.** `BaselineProfileGenerator` covers the signed-out start to Welcome, the signed-in start to Today, the trip journey (`04 §10.5`), large60 with the day jump, and the wizard up to Ready. The regenerated `baseline-prof.txt` and `startup-prof.txt` are committed, and every benchmark passes with `BaselineProfileMode.Require`.
3. **The full performance budget has been run** on the `pixel` emulator (and on a reference or low-end device if one is attached), and compared with the RN release APK through one shared Macrobenchmark harness. Every row of plan.md "Performance budget" is recorded in `docs/perf-startup.md` or `docs/perf-timeline.md` with its command, device and build type. Every functional and emulator gate passes; device rows pass or read "open: device only (OQ-A22)". A missed hard gate became a fix task in this phase, never a waiver.
4. **Play readiness is written down** in `docs/play.md`: the Data safety answers, the status of P1–P11, the `assetlinks.json` content with the release fingerprint only, tracks and the pre-upload checklist.

---

## 2. Preconditions (check first; do not rebuild earlier phases)

Run each check. If one fails, **stop and report it to the orchestrator** with the command and its output. Use the names you find; the ones below are the earlier briefs' names.

| # | Must exist | From | Check |
|---|---|---|---|
| P1 | `verifyReleaseManifest` with `ManifestRules.check(manifestXml, applicationId, allowedExportedComponents)`, 9 rules (`PERMISSION`, `CUSTOM_SCHEME`, `EXPORTED_COMPONENT`, `DEBUGGABLE_OR_TEST_ONLY`, `CLEARTEXT`, `MISSING_LOCALE_CONFIG`, `BACKUP`, `DIRECT_BOOT`, and 3a's `STARTUP_INITIALIZER`), planted manifests, `VerifyReleaseManifestTest`. 12b declared both reminder receivers `exported="false"` (`phase-12b-reminders-scheduler.md` l.389, l.415, OQ-12b-8), so they are **not** in `allowedExportedComponents`; note what that set holds today (expected: empty, or WorkManager's `SystemJobService` if 12b needed it to keep the check green) | 0a Task 7a, 12b | `ls build-logic/convention/src/main/kotlin/net/chronoflow/buildlogic/manifest/ManifestRules.kt build-logic/convention/src/test/resources/manifests/`; `grep -rn "allowedExportedComponents" build-logic app/build.gradle.kts` |
| P2 | `measureBundleSize` (`make size`), `BundleSize.kt` (`parseGetSizeTotal`, `verdict`, `nativeLibraries`), `BundleSizeTest`, device spec `{"supportedAbis":["arm64-v8a"],"supportedLocales":["en"],"screenDensity":480,"sdkVersion":36}`; `bundle { language { enableSplit = false } }` in `AndroidApplicationConventionPlugin` | 0a Task 7b, Task 4 (OQ-0a-16) | `grep -n "enableSplit" build-logic -r` |
| P3 | `version.properties` (`versionMajor/Minor/Patch/Build`), `VersionProperties.parse(text): AppVersion` in build-logic | 0a Task 4 | `cat version.properties` |
| P4 | `app/src/main/keepRules/app.keep` (the `-assumenosideeffects class android.util.Log { v; d; i }` block) and `credentials.keep` (5b); `:app:analyzeReleaseR8Config` in `make check`, or its recorded absence | 0a Task 6a, 5b Task 4 | `ls app/src/main/keepRules/`; `grep -n "analyzeReleaseR8Config" Makefile docs/tooling.md` |
| P5 | `ReleaseReadinessTest` (`app/src/testRelease/kotlin/net/chronoflow/app/`), run by `testReleaseUnitTest` | 0e T13, extended by 1e, 3b, 4c, 4e, 9g, 11a, 11b | `ls app/src/testRelease/kotlin/net/chronoflow/app/` |
| P6 | `:benchmark` with `StartupBenchmark` (`startupCold` with `first-commit`, `ready`, `exit`, `session` trace metrics and `WelcomeMomentMetric`; `startupWarm`; `startupColdSignedIn`), `LaunchJankBenchmark`, `ProcessDeathLaunchTest`, `TimelineBenchmark` (`timelineFirstFrame`, `timelineScroll`, `timelineLastDay`, `listScroll`, `timelineScrollWide`), `BaselineProfileGenerator` (`startup`, `timelineLarge60`, 6c's list-toggle extension), the `targetAppId` argument and the `?: "net.chronoflow.app.native"` fallback | 0e T16, 1e, 3a, 3b, 3c, 4e, 6c, 6e, 7 | `ls benchmark/src/main/kotlin/net/chronoflow/benchmark/` |
| P7 | `Scripts/check-benchmarks.py` (`--suite startup\|timeline`, `RAW_KEYS`, `jank_percent`, `hitch_ms_per_s`, `min` budgets, `--append`, trend flagging), `benchmark/budgets.json` (`"schema": 1`), `Scripts/tests/test_check_benchmarks.py`, `Scripts/fixtures/benchmarks/` | 0e T14, 1e, 3a | `python3 -I Scripts/check-benchmarks.py --help` |
| P8 | `Scripts/perf-startup.sh`, `Scripts/perf-timeline.sh`, `Scripts/baseline-profile.sh` (each: one device, emulator GPU `host` check, `prep-perf` with trap to `prep-functional`, `timeout_msec=300000`, `suppressErrors=EMULATOR` on the AVD), their dry-run tests in `Scripts/tests/`; `make perf`, `make perf-timeline`, `make baseline-profile` | 0e T16, 1e Tasks 5–6 | `grep -n "^perf\|^perf-timeline\|^baseline-profile" Makefile` |
| P9 | Committed `app/src/main/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt`; `BaselineProfileFilesTest` (`app/src/test/kotlin/net/chronoflow/app/`) | 1e Task 6, regenerated by 6b–6e | `wc -l app/src/main/generated/baselineProfiles/*.txt` |
| P10 | Perf variants `benchmarkRelease` and `nonMinifiedRelease` (or the OQ-A28 fallback `benchmark`, as `docs/tooling.md` records) with `app/src/perf`; `PerfMockSources` (4e) reading `GraphOverrides.mockSeed`; 1d/6b's `cf.trip-page` entry; 3b's perf `LaunchDiagnosticsParser` (`cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay`, `cf.mode`) | 0e T15, 1d, 3b, 4e, 6b | `ls app/src/perf/kotlin/net/chronoflow/app/perf/` |
| P11 | 9g's `WizardSaveJourney` entry in 9c's `WizardBoardCaptureSeed` (`feature/wizard/src/debug`, package `net.chronoflow.feature.wizard.debug`; `phase-9c-wizard-shell-where.md` l.604, `phase-9g-wizard-ready-save.md` l.309); the nullable `WizardStartOverride` seam on `AppGraph` (`AppGraph.wizardStartOverride`, **main** code, null in release; 9c l.554); `WizardDebugSeeds` (`app/src/debug`, sets the override; 9c l.605) | 9c, 9g | `grep -rn "WizardSaveJourney\|WizardStartOverride\|wizardStartOverride" app/src feature/wizard/src` |
| P12 | Test tags: `welcome.plan`, `welcome.signIn` (3c); `sign-in.email`, `sign-in.password`, `sign-in.submit` (5a); `tab.today`, `tab.trips`, `tab.settings`, `trips.list`, `trip.<tripId>` card (6a); `today.screen`, `today.trip` (7); `trip.page`, `trip.scroll`, `trip.timeline.ready`, `trip.viewToggle`, `trip.block.<id>`, `trip.dayMenu`, `trip.dayMenu.day.<n>`, `trip.plan.end` (1d, 6b, 6c); `step.sheet` (6d); `settings.signOut` (10a); `wizard.choice.one-way`, `wizard.fine-tune.continue`, `wizard.ready`, `wizard.ready.continue` (9d, 9f, 9g); `reminders.firstTrip` (12c's first-trip offer sheet root, `phase-12c-reminders-ui.md` §5.6) | listed phases | `grep -rn "\"<tag>\"" feature app` for each; use the exact spelling you find |

Also read `docs/tooling.md` (sandbox capabilities, perf-variant names, `timeout_msec`), `docs/perf-startup.md` and `docs/perf-timeline.md` (their `## Runs` tables and last rows) before Task 1.

---

## 3. Inputs to read

### 3.1 This repository

- `AGENTS.md`: "Performance", "Tests", "Commands", "Git", "Traps" (animator scales, `src/perf`, release ships no seed or mock code, configuration cache, `testReleaseUnitTest`).
- `plan.md`:
  - A1 (versions), A2 (identity, versionCode formula), A5 (backup), A6 (release data source `api`, `https://api.chronoflow.net/v1`, no mock), A15 (passkeys hidden until S1–S3), A16 (App Links), A21 (performance rules, R8, `-assumenosideeffects`), A22 (observability: `ApplicationExitInfo` in debug only), A23, A24.
  - "Android spec: build and repository layout" (l.282–319), "manifest and permissions" (l.450–463), "seeds and test hooks" (l.465–471), "release" (l.473–479).
  - "Performance budget" (l.491–538), "Risks" 2, 3, 4, 15, 17.
  - "Open questions": OQ-A1, OQ-A20, OQ-A22, OQ-A28, OQ-A38, OQ-A39, OQ-A41.
  - Phase 13 entry (l.2737–2783): the five checkboxes you tick.
- Briefs: `phase-0a-scaffold.md` Tasks 4, 6a, 7 and §5.4–§5.6; `phase-0e-test-harness.md` T13–T16; `phase-1e-timeline-perf.md` §5, Tasks 5–7, OQ-1e-1…14; `phase-3a-launch-overlay.md` Task 10; `phase-3b-launch-readiness.md` Tasks 8–9; `phase-3c-welcome.md` Task 9; `phase-4c-mock-data-source.md` OQ-4c-4; `phase-4e-session.md` §5.5–§5.6 (`GraphOverrides`, `MockSources`, `PerfMockSources`); `phase-5b-two-factor-passkeys.md` §4 (Credential Manager R8 rule); `phase-5c-password-reset-links.md` Task 10 (`docs/app-links.md`, the `assetlinks.json` shape); `phase-6c-trip-states-list.md` Task 9; `phase-6e-trip-wide.md` Task 10; `phase-7-today.md` (`startupColdSignedIn`, OQ-7-8); `phase-9g-wizard-ready-save.md` (`WizardSaveJourney`, `WizardJourneyTest`).

### 3.2 Research, quoted

- **`14 §11.2` Keys [R]:** "Play App Signing (mandatory for AAB). Google holds the app signing key. We hold an upload key: a PKCS12 keystore outside the repo, path and passwords in `~/.gradle/gradle.properties` (`CF_UPLOAD_*`) or the environment; CI reads secrets from the environment; the release `signingConfig` is wired only when those properties exist, so `assembleRelease` on a fresh clone builds unsigned instead of failing." Fingerprints the server needs: "the app signing key (from Play Console → App integrity), for production assetlinks and the passkey origin; the upload key, only if sideloaded release builds must verify links". "Never put the debug key's fingerprint or `apk-key-hash` into the production `assetlinks.json` or `passkeys.allowed_origins`."
- **`14 §11.3` Versions:** iOS `MARKETING_VERSION: "0.1.0"`, `CURRENT_PROJECT_VERSION: "1"`. "`versionCode = major*1_000_000 + minor*10_000 + patch*100 + build` (build 0–99) … It increases strictly for every upload."
- **`14 §11.4`:** "Play splits an AAB's resources by language by default. A user whose phone is English and who picks Dutch in the app would get no `values-nl` strings … Fix: `android { bundle { language { enableSplit = false } } }`. Keep the density and ABI splits on."
- **`14 §11.5` tracks:** internal (≤ 100 testers); closed — "required before production for personal developer accounts created after 2023-11-13: at least 12 testers opted in for the 14 continuous days"; open not used; production staged 10% → 50% → 100% with a halt on the vitals thresholds (crash 1.09%, ANR 0.47%, 8% per model, `14 §8.2`).
- **`14 §11.6` R8:** "kotlinx.serialization ships its own [rules]; our `@Serializable` Navigation 3 keys need `-keep` only if accessed reflectively (avoid that); `-assumenosideeffects` for `android.util.Log` v/d/i." Size: fonts ≈ 598 KB, plan copy ≈ 142 KB; target ≤ 8 MB, fail above 12 MB; "RN's universal APK was 125 MB".
- **`14 §6.1/§6.3`:** allowed release permissions `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (merged by WorkManager). Exported components allowed: "`MainActivity`, the boot/time receivers (exported, with system-only intent actions) and the WorkManager and AndroidX startup internals."
- **`14 §8.2`:** "No third-party SDK in v1 … Debug builds only: … On API 30+, log the last `ApplicationExitInfo` at startup. Release logging: R8 strips `Log.v/d/i` with `-assumenosideeffects`. Never log tokens, emails, guest secrets, codes or plan contents."
- **`14 §9.2` Data safety answers** (copy this table into `docs/play.md` unchanged):

  | Play category → type | Collected | Shared | Optional | Purposes |
  |---|---|---|---|---|
  | Personal info → Name | Yes | No | No | App functionality, Account management |
  | Personal info → Email address | Yes | No | No | App functionality, Account management |
  | Personal info → User IDs | Yes | No | No | App functionality, Account management |
  | Personal info → Other info (age band) | Yes | No | No (it blocks plans for under-18s) | App functionality |
  | Health and fitness → Health info (sleep schedule, shift work, caffeine habits) | Yes (conservative) | No | No | App functionality |
  | App activity → Other user-generated content (trips, check-offs) | Yes | No | No | App functionality |
  | Device or other IDs (app-generated device id) | Yes | No | No | App functionality, Fraud prevention / security (one token per device) |

  Security section: encrypted in transit yes (HTTPS only); deletion yes, in app Settings → Account → Delete account, on the web `https://app.chronoflow.net/settings/account`; Families policy no. Privacy policy `https://chronoflow.net/privacy` (iOS links the English URL in both languages, `K/Auth/SignUpModel.swift:134–137`). Not collected: location, contacts, photos, calendar, audio, messages, financial details, advertising ID, analytics or crash logs.
- **`14 §10` P1–P11** (copy the requirement column; you fill the status): P1 target API 36; P2 16 KB pages / no `.so`; P3 account deletion in app and on the web, with the gap that both delete routes require a verified email (`W/routes/api.php:83–84` at the web pin `e36368ec`; research 14 cites l.93–94, the current web HEAD; `W/routes/settings.php:22–26`), server item S4; P4 health apps declaration (a sleep/wellness feature, not a medical device, no Health Connect); P5 app access (a production demo account with 2FA off, verified, terms accepted, an active trip and credits; server item S5); P6 audience 18+, IARC all "no", no ads; P7 no Play Billing, no purchase or link-out; P8 no exact alarms, `POST_NOTIFICATIONS` in context; P9 Data safety; P10 Credential Manager needs server `assetlinks.json`; P11 pre-launch report read before every promotion.
- **`14 §12` server items S1–S6** (S1 `assetlinks.json` with `handle_all_urls` and `get_login_creds`; S2 `android:apk-key-hash` origin; S3 passkey management merge; S4 delete routes without `verified`; S5 production demo account; S6 iOS AASA `applinks`).
- **`11 §7`:** baseline profiles "generate from a non-minified variant; ship in R8-optimized release"; startup profiles only for the start journey; "Macrobenchmark refuses emulators unless `androidx.benchmark.suppressErrors=EMULATOR`; emulator numbers are regression signals, not device claims"; "Measure APK/AAB size and startup on the release+profile build only."
- **`13 §4.5` gates** (startup to Today: cold TTID ≤ 400 ms on the reference device, below RN Android) and **`13 §4.8`** (release readiness: seed code in `src/debug`; `testReleaseUnitTest` asserts `Class.forName` throws; `verifyReleaseManifest`; "the mock data source, when explicitly constructed, seeds no accounts").
- **`04 §10.5` baseline profile journeys:** "startup journey (`includeInStartupProfile = true`): launch → Today; trip journey: My trips → S3 → 5 flings down and up → list toggle → scroll → toggle back → day menu → day 59 → open a step sheet → dismiss; plus `cf.trip-page=large60` → 15 flings. … Regenerate it when the PlanCore port, the builder, the replay or the page structure change. The benchmark's `BaselineProfileMode.Require` makes a missing profile fail."
- **`04 §11` profiling protocol:** benchmark build type; device: airplane mode, battery saver off, charged above 50%, cooled, animator scales 1, font scale 1.0 plus one run at 1.3, light then dark, refresh rate recorded (`adb shell dumpsys display | grep -i refresh`); record date, commit, device, OS, refresh rate, font scale, theme, build type, raw metrics, trace paths.
- **`12 §1` [F]:** the RN release APK is `/Users/rickgout/Sites/chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk`, `applicationId net.chronoflow.app`, versionName 1.0.0, versionCode 1, minSdk 24, targetSdk 36, compileSdk 37, built 2026-11-05, mock data source, ships baseline profiles.

### 3.3 Facts verified while writing this brief [F]

- **RN APK** (read-only, `aapt2 dump badging`/`xmltree`, `shasum`, 2026-11-16): SHA-256 `4ee240d3754a7bba12761ddcb4b8317b15d7131f3c55f93f314d64e89df3b1f5`; 125,310,093 B; 124 `.so` entries; `assets/dexopt/baseline.prof` (14,483 B) and `baseline.profm`; `androidx.profileinstaller.ProfileInstallReceiver` (permission `DUMP`); launchable activity `net.chronoflow.app.MainActivity`; **no** `profileable` and **no** `debuggable`; requests `READ_CALENDAR` among others. It is signed with the debug key (`14 §11.1`, `M/android/app/build.gradle:100–115`).
- **Attached devices:** `adb devices` listed none (2026-11-16). Device rows therefore start as "open: device only (OQ-A22)"; check again at Task 12.
- **Build tools:** `$ANDROID_HOME/build-tools/36.0.0/` has `aapt2`, `apksigner`, `dexdump`, `zipalign`; `$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer` exists (0a). `jarsigner` and `keytool` come with Zulu 17.
- **Perf variants have no account** (4c OQ-4c-4, 4e §5.6): `PerfMockSources` passes `seed = overrides.mockSeed?.let { MockAppSeed.make(...) }`, and perf builds set no `mockSeed`, so the perf mock has no demo user. Signed-in perf launches today use 3b's stub readiness (`cf.session=in cf.stored=yes cf.gate=app`), which has no real session (7 may have added a perf-only `FixtureTodaySource` behind that stub, `phase-7-today.md` l.446; it serves Today only). Task 8 adds the perf-only demo account the journeys need.
- **Perf variants share the release applicationId** `net.chronoflow.app.native` (0e T15). Installing one over another keeps app data, so a session created by a journey would leak into a "signed-out" benchmark. 4e's `session` row is defined as "signed out: a fresh install, no seed" (`phase-4e-session.md:759`). Task 8 makes every perf script clear app data first.
- **Mock demo data** (4c `MockAppSeedTests`): 7 trips (destinations `[HND, JFK, SYD, LHR, DXB, AMS, AMS]`), `credits == 3`, terms `2026-10-01` accepted; account `demo@example.com` / `password`; trip `0199a3c0-0000-7000-8000-000000000001` is AMS→HND active (plan "seeds and test hooks").

### 3.4 iOS references (read-only, `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

- **`plan.md:1020` "Phase 7: Performance and comparison with RN (`feat/phase-7-performance`)"** [F]: `PerfUITests` with "target bundle from `CF_TARGET_BUNDLE_ID`, 10 iterations, `Perf` scheme, signed out: `XCTApplicationLaunchMetric()`, `XCTOSSignpostMetric` for the native intervals, `XCTMemoryMetric`, `XCTCPUMetric` (process), `XCTClockMetric` until 'Plan my trip' hittable (coarse 'below RN' only)", "runs against both bundles". RN baseline method: "RN repo stays untouched … record `git rev-parse HEAD` and `git status --short` … same device, charged, not Low Power, rebooted and idle 2 min; fresh signed-out install per app; record median and p90." **Android form:** one parameterized Macrobenchmark against both packages (Task 11), signed out, app data cleared before each iteration, the RN APK installed from its existing build output (never rebuilt), median and P90 recorded.
- **`plan.md:2286` "Performance budget"** [F]: launch to first frame "≤ 400 ms on the reference device and below RN median"; scene connect → overlay committed ≤ 16 ms; hand-off end 1900 ms ± 1 frame; Welcome interactive 2550 ms ± 1 frame; last intro item 3200 ms ± 1 frame; hitches 0–3.2 s "ratio < 1 ms/s, none longer than 1 frame"; "Peak memory to Welcome: below RN (`XCTMemoryMetric`)"; "CPU to Welcome: process below RN"; field metrics "tracked after release (later)". Android's rows are plan.md "Performance budget"; this table only explains them.
- **`plan.md:2248` "Release device entitlements fix (2026-11-09)"** [F]: production capabilities (passkeys, universal links) attach only when an explicit opt-in **and** a team and real certificate exist; "default phone builds carry no entitlements"; a signing guard refuses otherwise; "Physical paid-team passkey/universal-link acceptance remains open." **Android form:** the upload signing config attaches only when all four `CF_UPLOAD_*` values exist and the store lies outside the repo (Task 1); the debug key never appears in production `assetlinks.json` (OQ-A38); App Link verification and passkeys stay open until S1–S3 and a Play-signed build (Task 13).
- **`plan.md:2427` "Build and runner speed audit (user, 2026-11-09)"** [F]: separate build outputs per configuration, release-like builds kept out of the inner loop, "User-host acceptance: … execute first/warm `make check` and Perf commands … collect build/pass/overall timings". **Android form:** the new release targets stay out of `make check` (OQ-13-9), and Task 12 re-measures warm `make quick` and `make check` (three runs each) and records them.
- **`Docs/perf-timeline.md`** [F]: four guarantee layers; "Optimized UI budgets … for every change touching PlanUI or PlanCore"; gates first 60-day frame ≤ 0.8 s (aim 0.45), hitch ≤ 5 ms/s (aim 1), peak memory ≤ 150 MiB (aim 80), drawing ≤ 512 KiB, cache ≤ 4 MiB; "Simulator budgets give repeatable regression gates, not a guarantee for every device"; layer 4 "On-device Instruments review by the user. Before release …". **Android form:** layers 1–3 already run (1b–1e); layer 4 is Task 12's device step, "open: device only" while no device is attached.
- `App/LaunchArguments.swift`, `App/Debug/WizardBoardCaptureSeed.swift` l.114–124 (`WizardSaveJourney`): context for Task 8 only.

---

## 4. Out of scope

- Any screen, string, token or board work. No store listing graphics, no screenshots for Play.
- Changing any value in plan.md "Performance budget". If a budget looks wrong, add an open question; never edit the row.
- Changing the `applicationId` (stays `net.chronoflow.app.native`, OQ-A1) or creating real upload keys. The user creates the upload key; you only wire and test with a throwaway key outside the repo.
- Uploading to Play, publishing `assetlinks.json`, any server change (S1–S6), any change in `chronoflow-ios`, `chronoflow-new` or `chronoflow-mobile` (read-only; installing the existing RN APK with `adb install` is reading it).
- Play Billing, in-app review, in-app updates, crash or analytics SDKs (A22, A24).
- LeakCanary (A1 lists it; no phase has added it). Not added here (OQ-13-12).
- The board audit, accessibility audit, T2 matrix and process-death sweep (Phase 14).
- An RN timeline comparison: RN has no large60 fixture; the comparison is launch only (plan "Performance budget" rows "below RN").
- Microbenchmark modules, Gradle Managed Devices, CI services.
- Bumping the version: the first upload uses `0.1.0` / `10000` (OQ-13-10).

---

## 5. Fixed values

### 5.1 Upload signing (Task 1)

| Key (Gradle property, then environment variable of the same name) | Meaning |
|---|---|
| `CF_UPLOAD_STORE_FILE` | Absolute path (a leading `~/` is expanded to `user.home`) to a PKCS12 keystore **outside** the repository root |
| `CF_UPLOAD_STORE_PASSWORD` | Store password |
| `CF_UPLOAD_KEY_ALIAS` | Key alias |
| `CF_UPLOAD_KEY_PASSWORD` | Key password |

Resolution (pure function, §6 Task 1):
- none set → `null` (release unsigned) and one lifecycle log line `release is unsigned: CF_UPLOAD_* not set`;
- all four set, file exists, path not under the repo root → a signing config named `upload`, store type `PKCS12`;
- one to three set → configuration fails: `CF_UPLOAD_* incomplete: missing <names, comma-separated, in table order>`;
- file missing → fails: `CF_UPLOAD_STORE_FILE does not exist: <path>`;
- path under the repo root → fails: `CF_UPLOAD_STORE_FILE must be outside the repository: <path>`.

The perf build types keep the debug signing the baselineprofile plugin gives them; the debug build type keeps `app/debug.keystore`. Only `release` takes `upload`.

### 5.2 Version rule (Task 2)

`version.properties` keys `versionMajor`, `versionMinor`, `versionPatch`, `versionBuild`. `versionName = "<major>.<minor>.<patch>"`; `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build`; minor, patch, build in 0–99; major in 0–2099 (0a §5.6). Today: `0.1.0`, `10000`.

| `PART` | Change | Error |
|---|---|---|
| `build` | build + 1 | build = 99 → `versionBuild is 99: bump patch` |
| `patch` | patch + 1, build = 0 | patch = 99 → `versionPatch is 99: bump minor` |
| `minor` | minor + 1, patch = build = 0 | minor = 99 → `versionMinor is 99: bump major` |
| `major` | major + 1, minor = patch = build = 0 | major = 2099 → `versionMajor is 2099` |

Every Play upload bumps at least `build`. The new code must be strictly greater than the old one (asserted by the script).

### 5.3 Final release manifest (Task 4)

- **Required permissions** (each must be present): `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, `android.permission.POST_NOTIFICATIONS`, `android.permission.RECEIVE_BOOT_COMPLETED`, `android.permission.WAKE_LOCK`.
- **Allowed in addition, not required:** `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (0a OQ-0a-20).
- **Banned** (already failing as `PERMISSION`): everything else, including `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `FOREGROUND_SERVICE*`, calendar, `CAMERA`, `READ_PHONE_STATE`, `com.google.android.gms.permission.AD_ID`, c2dm, badges.
- **SDK levels:** `<uses-sdk android:minSdkVersion="29" android:targetSdkVersion="36"/>` [verify that AGP 9.5.1 writes `<uses-sdk>` into the merged manifest; if not, read the levels from `aapt2 dump badging` of the release APK in Task 6 instead and record it].
- **Version:** `android:versionCode` and `android:versionName` equal the values computed from `version.properties` [verify they appear on `<manifest>` in the merged manifest; same fallback].
- **Exported components:** exactly `net.chronoflow.app.MainActivity`, plus components guarded by `android:permission="android.permission.DUMP"` (0a: profileinstaller's receiver, WorkManager's diagnostics receiver). **Superseded at `1c0333db` (R3):** WorkManager's `SystemJobService` is overridden to `exported="false"` in `app/src/main/AndroidManifest.xml:55–58` (12b, OQ-A587), so there is no `BIND_JOB_SERVICE` exemption. 12b's `ReminderAlarmReceiver` and `ReminderUpkeepReceiver` are `exported="false"` (OQ-12b-8) and must stay so; research `14 §6.3` allowed them exported, 12b chose not to. Freeze the set as named constants (Task 4).

### 5.4 Release content (Task 6)

- **Forbidden original class prefixes** (read through the R8 mapping): `net.chronoflow.debug.`, any `net.chronoflow.` package with a `debug` segment (regex `^net\.chronoflow\.(\w+\.)*debug\.`, which covers 9c's `net.chronoflow.feature.wizard.debug.`), `net.chronoflow.app.perf.` (except the release no-op stubs defined in `app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf/`, R7), `net.chronoflow.core.datamock.`, `net.chronoflow.core.testing`, `leakcanary.`, `androidx.metrics.performance.`, `androidx.test.`, `org.junit.`, `org.robolectric.`, `mockwebserver3.`, `okhttp3.mockwebserver.`, `androidx.compose.ui.tooling.` (except the package `androidx.compose.ui.tooling.preview`, allowed if present [verify]).
- **Forbidden string constants** (any `const-string` in the dex): every value that **starts with `CF_`** (all seed names are debug-only, plan "seeds and test hooks"; later phases added more, e.g. `CF_PLAN_TEST_POSITION`, `CF_AUTH_TEST_AUDIT_SCREEN`, `CF_GUEST_WIZARD`, `CF_SETTINGS_BOARD_STATE`, so a prefix rule beats a list; the build-only `CF_UPLOAD_*` names never reach the dex), plus these exact substrings: `SeedReceiverActivity`, `demo@example.com`, `twofactor@example.com`, `AbCdEf1234-GhIjKl5678`, `cf.trip-page`, `cf.timeline-fixtures`, `cf.perf-account`, `cf.perf-wizard`, `uitests/`.
- **Forbidden calls from our code** (methods whose class maps back to `net.chronoflow.*`): `Landroid/util/Log;.v:`, `Landroid/util/Log;.d:`, `Landroid/util/Log;.i:`, `Landroid/app/ActivityManager;.getHistoricalProcessExitReasons:`.

### 5.5 Perf-only journey extras (Task 8)

| Intent extra on `MainActivity` | Value | Effect (perf variants and debug; never release) |
|---|---|---|
| `cf.perf-account` | `demo` | When the perf mock has no state file yet, it is seeded with `MockAppSeed` for `MockSeedFlags(unverified = false, walkthroughSeen = true, termsPending = false)`: the demo account (`demo@example.com` / `password`), its 7 trips and 3 credits. Signing in still happens through the real form. |
| `cf.perf-wizard` | `WizardSaveJourney` | Once the session is signed in and the shell is shown, opens `Wizard(mode = Create)` with `PerfWizardJourney.saveJourney` (Task 8): the same start state as 9g's `WizardSaveJourney` (HND, departure = now + 30 days, 18 h flight, step `returnChoice`, real clock, real store, real generation). |

Any other value is ignored: `parse` records `"<key>=<value>"` in `ignored`, and the perf hook (not `parse`, which stays free of `android.util.Log` so a plain JVM test can call it) logs one `Log.w("CfPerf", "ignored <key>=<value>")` per entry.

### 5.6 Benchmark identities

- Native perf package `net.chronoflow.app.native` (no suffix in perf variants). RN package `net.chronoflow.app`.
- Demo trip card tag `trip.0199a3c0-0000-7000-8000-000000000001` (AMS→HND, active).
- Perf clock freeze for fixture pages: `cf.freezeAt=2026-10-13T08:00:00Z` (1e OQ-1e-7).

### 5.7 Budget rows and where Phase 13 gets each number

Every row of plan.md "Performance budget" (l.507–537). "Gate" = must pass on `pixel` in this phase; "report" = recorded, ±25% change flagged; "device" = "open: device only (OQ-A22)" unless a device is attached.

| Row | Measured by | Emulator | Phase 13 adds |
|---|---|---|---|
| Cold start TTID, signed out (≤ 400 ms ref, ≤ 800 low-end, below RN) | `StartupBenchmark.startupCold`; RN via `LaunchComparisonBenchmark` | report; below-RN report | the RN comparison (Task 11) |
| Warm start TTID | `StartupBenchmark.startupWarm` | report | — |
| `onCreate` → overlay frame 0 | `first-commit` in `startupCold` | report | — |
| Overlay frame pacing 0–3.2 s | `LaunchJankBenchmark` | report | — |
| First frame → first animated change | JVM `LaunchTimeline.sample` test (3a) | gate (in quick) | — |
| Launch jank 0–3.2 s | `LaunchJankBenchmark` `frameOverrunMs`, `frameCount` min 75 | report; `frameCount` gate | — |
| Main thread before hand-off | debug StrictMode `penaltyDeath` launch test (0a, 3a) | gate (in check) | — |
| Stall behaviour | `LaunchStallTest` (3b) | gate (in check) | — |
| Session known, signed out | `session` in `startupCold` | report | — |
| Hand-off end 1900 ms | `LaunchHandOffTest` (3b) | gate (in check) | — |
| Welcome interactive 2550 ms | `welcomeInteractiveMs` (3c) | gate | — |
| Last intro item 3200 ms | `introLastItemMs` (3c) | gate | — |
| TTFD | `timeToFullDisplayMs` in `startupCold`, `startupColdSignedIn` | report (must be finite) | — |
| Plan preparation, JVM | `TimelinePreparationBudgetTest` | gate (in `make timing`, the last step of quick) | — |
| `DayDrawing` build | `DayDrawingBudgetTest` | gate (in `make timing`, the last step of quick) | — |
| Drawing cache | 1c unit test | gate (in quick) | — |
| Invalidation | `TimelineInvalidationTest` | gate (in quick) | — |
| First large60 timeline frame | `timelineFirstFrame` | **gate ≤ 800 ms** | — |
| Scroll CPU P90/P99 | `timelineScroll` | **gate: the CPU band** (plan.md "Where gates apply") | — |
| Scroll CPU P50, overrun P99, jank %, hitch | `timelineScroll` (+ `listScroll`, `timelineScrollWide` report) | report / trend | — |
| Placeholders | `timelineScroll` | **gate ≤ 6 per iteration** | — |
| Jump to the last day | `timelineLastDay` | gate | — |
| Peak memory, large60 scroll | `timelineScroll` `memoryRssAnonMaxKb` | **gate ≤ 153,600 KiB** (150 MiB; 1e §5) | — |
| Peak memory to Welcome (below RN) | `LaunchComparisonBenchmark` `memoryRssAnonMaxKb`, both apps, 0–3.5 s | report; below-RN report | yes (Task 11, OQ-13-1) |
| Download size | `make size` | **gate ≤ 8,000,000 B (OK verdict), no `.so` except `libandroidx.graphics.path.so` (OQ-0a-22)** | the language-split check (Task 5) |
| `make quick` < 60 s warm | `Scripts/measure-tiers.sh` | gate | re-measure (Task 12) |
| `make check` < 5 min warm | same | gate | re-measure (Task 12) |

---

## 6. Tasks

One task is one commit unless it says otherwise. Commit messages are Conventional Commits, lower-case imperative subject, **no trailer of any kind**. The commit that completes a plan.md checkbox ticks it (`- [ ]` → `- [x]`) in the same commit (§7). Paths: `app/…/X.kt` = `app/src/main/kotlin/net/chronoflow/app/X.kt`; `build-logic/…/X.kt` = `build-logic/convention/src/main/kotlin/net/chronoflow/buildlogic/X.kt`; `benchmark/…/X.kt` = `benchmark/src/main/kotlin/net/chronoflow/benchmark/X.kt`.

Per task, run only what it touched (`make targeted ONLY=…`, or the build-logic/Python test named). Run `make quick` after Tasks 3, 7 and 10, and `make check` once before hand-off.

### Task 1: Upload signing from `CF_UPLOAD_*`

**Commit:** `build(app): sign release bundles with an upload key from properties`.

- `build-logic/…/signing/UploadSigning.kt`:
  ```kotlin
  data class UploadSigning(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)
  object UploadSigningResolver {
      val KEYS = listOf("CF_UPLOAD_STORE_FILE", "CF_UPLOAD_STORE_PASSWORD", "CF_UPLOAD_KEY_ALIAS", "CF_UPLOAD_KEY_PASSWORD")
      fun resolve(values: Map<String, String?>, repoRoot: File, home: File, exists: (File) -> Boolean): UploadSigning?
  }
  ```
  Rules exactly as §5.1. Blank strings count as unset. `resolve` throws `IllegalArgumentException` with the §5.1 messages. Compare paths with `canonicalFile`.
- `AndroidApplicationConventionPlugin`: build `values` from `providers.gradleProperty(key).orElse(providers.environmentVariable(key))` for each key (configuration-cache safe; no `System.getenv`, no `findProperty`). When `resolve` returns non-null, create `signingConfigs.create("upload") { storeFile; storePassword; keyAlias; keyPassword; storeType = "PKCS12" }` and set it on the `release` build type only. When it returns null, log the §5.1 line once with `logger.lifecycle`.
- `Scripts/tests/test_repo_keys.py` (unittest, run by `Scripts/test_scripts.sh` in quick): `git ls-files -- '*.jks' '*.keystore' '*.p12' '*.pk8' '*.pem' '*.der'` prints exactly `app/debug.keystore`. Planted control: run the same matcher function on the list `["app/debug.keystore", "upload.p12"]` → fails naming `upload.p12`.
- `docs/release.md` (new), section "Upload key": how the user creates it, outside the repo:
  ```sh
  keytool -genkeypair -v -storetype PKCS12 -keystore ~/keys/chronoflow-upload.p12 -alias upload \
    -keyalg RSA -keysize 4096 -validity 9125 -dname "CN=ChronoFlow Upload"
  ```
  then the four lines for `~/.gradle/gradle.properties`, how to print its SHA-256 (`keytool -list -v -keystore … -alias upload`), and "enrol in Play App Signing; Google holds the app signing key".
- **Tests:** `UploadSigningTest` (build-logic JVM, one method per case): none set → null; all set, file exists, outside → the four values; `~/k.p12` expands against `home`; two set → message names the two missing keys in table order; blank password counts as missing; file missing → message; file under `repoRoot` (`<root>/app/upload.p12`) → message; environment-only values (simulate by the map) work the same.
- **Proof, not committed:** generate a throwaway key under `$TMPDIR/cf-upload-test/upload.p12` with `-dname "CN=ChronoFlow Test Upload"`; run `./gradlew :app:bundleRelease -PCF_UPLOAD_STORE_FILE=$TMPDIR/cf-upload-test/upload.p12 -PCF_UPLOAD_STORE_PASSWORD=… -PCF_UPLOAD_KEY_ALIAS=upload -PCF_UPLOAD_KEY_PASSWORD=…`; then `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | grep -m1 "CN=ChronoFlow Test Upload"` must print a line. Run `./gradlew :app:bundleRelease` without the properties: it succeeds and `jarsigner -verify` reports the bundle unsigned. Delete the throwaway directory. Record both commands and outputs in `docs/release.md` "Verification".

**Edge cases:** a key in `gradle.properties` of the repo (not the user's) must also be read but never committed: `test_repo_keys.py` also fails if `git grep -n -E '^[[:space:]]*CF_UPLOAD_[A-Z_]+=[^[:space:]]' -- '*.properties'` prints anything (an assignment with a value in a committed properties file). Matching only `*.properties` keeps the test file, `docs/` and the Kotlin sources from matching themselves. Planted control: the same matcher on the text `CF_UPLOAD_STORE_PASSWORD=secret` fails, on `CF_UPLOAD_STORE_PASSWORD=` (no value) passes.

### Task 2: Version bump rule

**Commit:** `build: add a version bump rule`.

- `Scripts/bump-version.py` (stdlib, `python3 -I`): `bump-version.py <build|patch|minor|major> [--file version.properties] [--dry-run]`. Reads the four keys (any missing or non-integer → exit 2 naming the key), applies §5.2, asserts new code > old code, rewrites the file with the four keys in the same order and nothing else, and prints `version: 0.1.0 (10000) -> 0.1.0 (10001)`. `--dry-run` prints without writing. Usage errors exit 2; the §5.2 errors exit 1.
- `Makefile`: `version-bump:` → `python3 -I Scripts/bump-version.py $(PART)`; fails with usage when `PART` is empty.
- `docs/release.md` "Versioning": the formula, the table of §5.2, "every upload bumps at least build", and the commit message convention `chore(release): bump version to <name> (<code>)`.
- **Tests:** `Scripts/tests/test_bump_version.py` (on temporary copies): each of the four parts from `0.1.0/0`; `build` at 99 → exit 1 with the message; `patch` resets build; `minor` resets patch and build; `major` resets all three; missing `versionBuild` → exit 2; `--dry-run` leaves the file byte-identical; the computed code equals `VersionProperties.parse` for the same file (call `./gradlew` is not allowed in unit tests: re-implement the formula once in the test and compare with the 0a rule `major*1_000_000 + minor*10_000 + patch*100 + build`).

### Task 3: Keep-rule review and guard

**Commit:** `build(app): review and guard the r8 keep rules`.

- Build `./gradlew :app:assembleRelease :app:assembleBenchmarkRelease` (use the OQ-A28 build-type name if it differs). R8 writes `app/build/outputs/mapping/<variant>/configuration.txt`, `mapping.txt`, `seeds.txt` and `usage.txt` [verify the file names under AGP 9.5.1; if `configuration.txt` is missing, add `-printconfiguration build/outputs/mapping/release/configuration.txt` to `app.keep` and record it].
- Run `./gradlew :app:analyzeReleaseR8Config` if 0a found it, and keep its output.
- **Review** in `docs/release.md` "R8 keep rules":
  - list every file in `app/src/main/keepRules/` with each rule and its *why* (`app.keep`: Log stripping; `credentials.keep`: the Credential Manager Play-services provider is loaded by reflection, 5b);
  - the count of library consumer rule files in `configuration.txt` and any of them that keeps a whole package (`-keep class X.** { *; }`): name it and say whether it matters for size;
  - Navigation 3 and kotlinx.serialization: `grep -rn "rememberNavBackStack\|SavedStateConfiguration\|serializer<\|Json.encodeToString" app feature core --include=*.kt`. If routes or sheets are saved through reflective `serializer<T>()` lookups, record that kotlinx.serialization's bundled consumer rules cover `@Serializable` classes with companion `serializer()`; do **not** add a broad `-keep` for routes. Task 9's smoke test is the proof; add a narrow rule only for a failure it shows, with the failing stack in the commit body;
  - confirm the release and `benchmarkRelease` configurations are identical apart from profileable/debug signing: `diff <(grep '^-' app/build/outputs/mapping/release/configuration.txt | sort) <(grep '^-' app/build/outputs/mapping/benchmarkRelease/configuration.txt | sort)` and quote the result. A difference in a `-keep` or `-assumenosideeffects` line is a finding (the smoke and benchmarks would then not prove release).
- **Guard test** `konsist/src/test/kotlin/net/chronoflow/konsist/KeepRulesTest.kt` (JVM, plain file reading like 0a's `ModuleGraphTest`; finds the root the same way):
  - `keepRuleFilesAreKnown`: the files in `app/src/main/keepRules/` are exactly `app.keep`, `credentials.keep` and any file added by a Task 9 fix (update the list in that fix's commit);
  - `logStrippingIsPresent`: `app.keep` contains the `-assumenosideeffects class android.util.Log` block with `v(...)`, `d(...)`, `i(...)` and not `w(` or `e(`;
  - `noOptimisationEscapes`: no keep file contains `-dontoptimize`, `-dontshrink`, `-dontobfuscate`, `-keepattributes *`, `-ignorewarnings` or `-dontwarn **`;
  - `noBroadAppKeeps`: no line matches `-keep[a-z]*\s+class\s+net\.chronoflow\.\*\*`;
  - `…RejectsPlantedViolation` for the last three, over `konsist/src/test/resources/violations/keepRules/app/src/main/keepRules/broad.keep` (holds `-dontobfuscate` and `-keep class net.chronoflow.** { *; }`) and a `log.keep` whose Log block lacks `i(...)`.
- Change no rule in this task unless the review finds one that is wrong; then name it and the reason in the commit body.

### Task 4: Final manifest allowlist

**Commit:** `build: freeze the release manifest allowlist`.

- Extend `ManifestRules` (0a) with three rules and the inputs they need:
  - `REQUIRED_PERMISSION_MISSING`: a §5.3 required permission is absent (input `requiredPermissions: Set<String>`);
  - `SDK_LEVELS`: `<uses-sdk>` `minSdkVersion` ≠ `29` or `targetSdkVersion` ≠ `36`, or `<uses-sdk>` missing (inputs `minSdk`, `targetSdk`);
  - `VERSION_MISMATCH`: `<manifest android:versionCode/versionName>` ≠ the values from `version.properties` (inputs `versionCode`, `versionName`).
  If the [verify] of §5.3 fails for `<uses-sdk>` or the version attributes, do not add that rule; move the check to Task 6's script (read `aapt2 dump badging` of the release APK: `sdkVersion:'29'`, `targetSdkVersion:'36'`, `versionCode='10000' versionName='0.1.0'`) and record it.
- Move the exported-component set into a named constant `ReleaseManifestAllowlist.EXPORTED = setOf("net.chronoflow.app.MainActivity")` in `build-logic/…/manifest/ReleaseManifestAllowlist.kt`, together with `PERMISSIONS_REQUIRED`, `PERMISSIONS_OPTIONAL` (`.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` with the applicationId prefix) and `GUARD_PERMISSIONS = setOf("android.permission.DUMP")` (**R3: not `BIND_JOB_SERVICE`; the next bullet and `job-service.xml` are superseded by R3**). Never use a wildcard.
- Change `EXPORTED_COMPONENT` (0a: only `DUMP` exempts a component) so that a component whose `android:permission` is in `GUARD_PERMISSIONS` passes. If P1 found `SystemJobService` (or anything else) in `allowedExportedComponents`, remove it there in this commit (the guard rule now covers it) and say so in the commit body; `allowedExportedComponents` then equals `EXPORTED`.
- `VerifyReleaseManifestTask` passes the new inputs (`@Input` each), still `@CacheableTask`.
- Planted manifests in `build-logic/convention/src/test/resources/manifests/`: update `valid.xml` to hold all five required permissions, `<uses-sdk android:minSdkVersion="29" android:targetSdkVersion="36"/>`, `versionCode="10000"`, `versionName="0.1.0"` and 12b's two receivers with `android:exported="false"`; add `missing-post-notifications.xml`, `target-sdk-37.xml`, `version-mismatch.xml` (`versionCode="10001"`), `exported-unknown-receiver.xml` (an exported `net.chronoflow.app.UnknownReceiver`), `job-service.xml` (WorkManager's `androidx.work.impl.background.systemjob.SystemJobService` exported with `BIND_JOB_SERVICE`: must **pass**).
- `VerifyReleaseManifestTest`: `valid.xml` → no violation; each new planted file → exactly its rule; `job-service.xml` → no violation; every old case still exactly its rule.
- Run `./gradlew :app:verifyReleaseManifest` on the real app; it passes.
- `docs/release.md` "Release manifest": paste the `<uses-permission>` lines and every exported component of `app/build/intermediates/merged_manifest/release/…/AndroidManifest.xml` (path as AGP 9.5.1 writes it), and every `androidx.startup.InitializationProvider` `<meta-data>` entry (exactly A21's two, `ProcessLifecycleInitializer` and `ProfileInstallerInitializer`; 3a's `STARTUP_INITIALIZER` rule enforces it).

### Task 5: Size gate with the language-split check

**Commit:** `build: fail the size gate on language splits`. **Ticks:** "Release build".

- `BundleSize.kt`: add `fun languageSplits(apksEntryNames: List<String>): List<String>` returning entries matching `^splits/base-[a-z]{2,3}(_[A-Z]{2})?\.apk$` (bundletool's language split names, such as `splits/base-en.apk`) [verify the naming on one run by temporarily setting `enableSplit = true`, running `make size`, listing `build/outputs/size/app.apks`, then reverting; record the listing].
- `MeasureBundleSizeTask`: after `build-apks`, list the zip entries of `app.apks`; fail with `language split present: <entries>` when `languageSplits` is non-empty. Keep the `.so` check and the verdicts unchanged. Write the entry list to `build/outputs/size/apks-entries.txt`.
- **Tests:** `BundleSizeTest` gains: `["toc.pb", "splits/base-master.apk", "splits/base-xxhdpi.apk"]` → none; `["splits/base-master.apk", "splits/base-en.apk"]` → `splits/base-en.apk`; `splits/base-nl.apk` and `splits/base-pt_BR.apk` → found; `splits/base-arm64_v8a.apk` → none.
- Run `make size` and quote the line. **Acceptance for this phase is the `OK` verdict** (≤ 8,000,000 B). A `WARN` is a fix task in this phase: find the largest contributors (`bundletool get-size total --dimensions=ALL`, `apkanalyzer files list` sorted by size), fix in a separate `perf(app): …` or `build: …` commit, and record before/after in `docs/release.md` "Size".

### Task 6: Release APK content check

**Commit:** `build: check the release apk for debug code and strings`.

- `Scripts/check-release-apk.py` (stdlib, `python3 -I`): `check-release-apk.py --apk <apk> --mapping <mapping.txt> [--apkanalyzer <path>] [--dexdump <path>]`. Defaults: `$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer`, `$ANDROID_HOME/build-tools/36.0.0/dexdump`.
  1. `apkanalyzer dex packages --defined-only --proguard-mappings <mapping> <apk>` → fail on any package or class whose name starts with a §5.4 forbidden prefix.
  2. Parse `mapping.txt` (class lines `original -> obfuscated:`) into obfuscated → original.
  3. Unzip every `classes*.dex` into a temporary directory; run `dexdump -d` on each; fail on any `const-string` whose value contains a §5.4 forbidden string (report the deobfuscated class); fail on any §5.4 forbidden call inside a method whose class maps to `net.chronoflow.*`.
  4. Informational, never a failure: print `INFO billing: absent` or `INFO billing: present (<first class>)` for classes starting with `com.android.billingclient.` (Task 13's P7 evidence).
  5. Print `release apk: OK (<n> dex files, <m> classes checked)` or one `FAIL <kind>: <detail>` line per finding; exit 1 on any finding, 2 on usage or a missing tool.
- `Makefile` target `release-check`: `$(GRADLE) :app:assembleRelease :app:testReleaseUnitTest :app:verifyReleaseManifest`, then `python3 -I Scripts/check-release-apk.py --apk <the release APK under app/build/outputs/apk/release/, signed or -unsigned> --mapping app/build/outputs/mapping/release/mapping.txt`, then `$(MAKE) size`. Not part of `make check` (OQ-13-9).
- **Tests:** `Scripts/tests/test_check_release_apk.py` with fixtures in `Scripts/fixtures/release-apk/` (small hand-written `apkanalyzer` output, `mapping.txt` and `dexdump -d` text; no real APK): clean fixture → exit 0 and `INFO billing: absent`; a package line `net.chronoflow.debug` → exit 1; a package line `net.chronoflow.feature.wizard.debug` → exit 1; `androidx.compose.ui.tooling.preview` alone → exit 0; a `const-string … "CF_AUTH_TEST_ID"` → exit 1 naming the original class; a `const-string … "CF_GUEST_WIZARD"` (not in any list, caught by the prefix) → exit 1; a class `com.android.billingclient.api.BillingClient` → exit 0 with `INFO billing: present`; an `invoke-static … Landroid/util/Log;.d:` in a class that maps to `net.chronoflow.app.X` → exit 1; the same call in a class that maps to `androidx.core.Y` → exit 0; `Log.w` in our class → exit 0; a missing `dexdump` path → exit 2.
- Run `make release-check` (or hand it to the orchestrator) and quote the result in `docs/release.md` "Release content". Every finding is a fix in this phase (a separate `fix(<scope>): …` commit that moves the code to `src/debug`/`src/perf` or removes the string), never an allowlist entry, except the [verify] preview package.

### Task 7: `ApplicationExitInfo` logging, debug only

**Commit:** `feat(app): log the previous exit reason in debug builds`. Skip the commit if `grep -rn "getHistoricalProcessExitReasons" app/src` already finds a debug-only implementation; then only extend `ReleaseReadinessTest` (below) in Task 10's commit and say so.

- `app/src/debug/kotlin/net/chronoflow/debug/ExitInfoLogger.kt`:
  ```kotlin
  internal object ExitInfoLogger {
      fun logPrevious(context: Context) {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
          val am = context.getSystemService(ActivityManager::class.java) ?: return
          val info = am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull() ?: return
          Log.i("CfExit", "reason=${info.reason} status=${info.status} importance=${info.importance} " +
              "pss=${info.pss} rss=${info.rss} timestamp=${info.timestamp} description=${info.description}")
      }
  }
  ```
  Never log `info.traceInputStream` or process state summaries (they may hold user data).
- Call it once per process from the debug side of the variant hook that runs after `first-commit` on `AppDispatchers.io` (the hook 3a/4e use for debug wiring; find it with `grep -rn "first-commit\|onFirstCommit" app/src`). Release and perf call nothing.
- `ReleaseReadinessTest`: `Class.forName("net.chronoflow.debug.ExitInfoLogger")` throws `ClassNotFoundException`.
- **Tests:** `app/src/testDebug/kotlin/net/chronoflow/debug/ExitInfoLoggerTest.kt` (Robolectric, `@Logic`): `@Config(sdk = [30])` with `ShadowActivityManager.addApplicationExitInfo(…)` of reason `REASON_CRASH` → `ShadowLog.getLogsForTag("CfExit")` has one entry containing `reason=4`; no exit info → no entry; `@Config(sdk = [29])` → no entry and no `ActivityManager` call [verify `ShadowActivityManager.addApplicationExitInfo` exists in Robolectric 4.18; if not, inject a `() -> List<ApplicationExitInfo>` supplier and test with a fake list built through Robolectric's `ApplicationExitInfoBuilder`, or drop the SDK 30 case and record it].

### Task 8: Perf-only journey extras and clean perf state

**Commit:** `test(app): add perf-only demo account and wizard journey extras`.

- **Spike first (10 min, not committed):** find where perf variants build `GraphOverrides`, where 9c's `CF_WIZARD_AUTO_OPEN` path opens the wizard and how `WizardDebugSeeds` sets `AppGraph.wizardStartOverride` (`grep -rn "GraphOverrides(\|wizardStartOverride\|WizardStartOverride\|CF_WIZARD_AUTO_OPEN" app/src feature/wizard/src`). If wiring §5.5 needs a change in `app/src/main` beyond adding one nullable member to the existing variant hook interface, stop and apply the fallback in OQ-13-5 / OQ-13-6, and record it.
- `app/src/perf/kotlin/net/chronoflow/app/perf/PerfJourneyExtras.kt`:
  ```kotlin
  data class PerfJourneyExtras(val demoAccount: Boolean, val wizard: PerfWizard?, val ignored: List<String> = emptyList()) {
      companion object { val None = PerfJourneyExtras(false, null); fun parse(extras: Map<String, String>): PerfJourneyExtras }
  }
  enum class PerfWizard { WizardSaveJourney }
  internal object PerfJourneyState { @Volatile var current: PerfJourneyExtras = PerfJourneyExtras.None }
  ```
  `MainActivity`'s perf hook sets `PerfJourneyState.current` from the intent in `onCreate` (before `setContent`) and `onNewIntent`, then logs each `ignored` entry. Parsing reads strings only; no I/O, no `Log`.
- `PerfMockSources` (4e, `app/src/perf`; 4e l.402–411): `seed = (overrides.mockSeed ?: if (PerfJourneyState.current.demoAccount) MockSeedFlags(unverified = false, walkthroughSeen = true, termsPending = false) else null)?.let { MockAppSeed.make(Instant.now(), it.unverified, it.walkthroughSeen, it.termsPending) }`. Keep `debugAppSeed = BuildConfig.DEBUG` as it is. The seed only applies when the state file is absent (4c `load()`: `state = restored ?? seed`), so it never resets an existing perf session. The source is built after `MainActivity.onCreate` has set the flag (4e's `SessionActivation` runs on `ProcessLifecycleOwner` ON_START); if `PerfMockSourcesSeedTest` or the journey shows otherwise, read the flag when the seed is first needed and record it.
- **Wizard start (no code move).** 9c's `WizardBoardCaptureSeed` lives in `feature/wizard/src/debug`, which cannot call `:app` code, and `WizardStartOverride` is a **main** seam on `AppGraph` (9c l.554), so neither can move to `app/src/perf`. Instead:
  - `app/src/perf/kotlin/net/chronoflow/app/perf/PerfWizardJourney.kt`: `object PerfWizardJourney { fun saveJourney(now: Instant, zone: ZoneId): <the draft type WizardStartOverride returns> }`, a perf copy of 9g's `WizardSaveJourney` entry built only from public `:core:wizard`/`:feature:wizard` main APIs: 9c's base draft (AMS → NRT, the four airports, profile `habitual_bed 23:30`, `habitual_wake 07:15`, `chronotype intermediate`; 9c §"`WizardBoardCaptureSeed`" Base, iOS `WizardBoardCaptureSeed.swift` l.10–37), then iOS l.114–124 (HND `Tokyo Haneda Airport`, `JP`, `Asia/Tokyo`, city of the base NRT airport, via `setRouteAirport(…, at = 1)`, added to `airports`; `departure = now + 30 days`; `legs[0].departLocal = WizardDateTime.string(departure)`, `arriveLocal = WizardDateTime.string(departure + 18 h)`; `step = returnChoice`). Real clock, not frozen, no `uiOverride`, recovery writes on (9g l.368). If the base draft needs a debug-only helper, copy the few lines; never make `feature/wizard/src/debug` code part of main.
  - When `PerfJourneyState.current.wizard == WizardSaveJourney`, the perf hook sets `AppGraph.wizardStartOverride` to return `PerfWizardJourney.saveJourney(Instant.now(), ZoneId.systemDefault())` once, and, when the session gate reaches the app shell, opens the wizard the way the debug `CF_WIZARD_AUTO_OPEN` path does, once per process. Debug keeps using `WizardDebugSeeds` unchanged.
- **Clean state in perf scripts:** `Scripts/perf-startup.sh`, `Scripts/perf-timeline.sh` and `Scripts/baseline-profile.sh` run `adb shell pm clear net.chronoflow.app.native || true` after the device checks and before Gradle (so a session left by a journey never reaches a signed-out benchmark; `|| true` because the package is absent on a fresh AVD and `pm clear` then fails). Not on a physical device unless `PERF_CLEAR=1` is set (OQ-13-15). Their dry-run tests assert the line appears exactly once, after `prep-perf`.
- `ReleaseReadinessTest`: `Class.forName` throws for `net.chronoflow.app.perf.PerfJourneyExtras`, `…PerfJourneyState`, `…PerfWizardJourney`.
- **Tests:**
  - `app/src/testDebug/kotlin/net/chronoflow/app/perf/PerfJourneyExtrasTest.kt` (plain JVM). It must be in `src/testDebug`, not `src/test`: `src/test` is also compiled for `testReleaseUnitTest` (AGENTS "Traps"), where `src/perf` is absent. Cases: empty → `None`; `cf.perf-account=demo` → `demoAccount`; `cf.perf-account=other` → not, `ignored == ["cf.perf-account=other"]`; `cf.perf-wizard=WizardSaveJourney` → the enum; unknown wizard → null and one `ignored` entry.
  - `app/src/testDebug/kotlin/net/chronoflow/app/perf/PerfWizardJourneyTest.kt` (plain JVM; the debug unit-test classpath sees `feature/wizard/src/debug`): for one injected `now` and zone, `PerfWizardJourney.saveJourney(now, zone)` equals 9c's `WizardBoardCaptureSeed` draft for `WizardSaveJourney` built with the same `now` in route, airports, legs, profile and step (if 9c's builder takes no `now`, compare everything except `legs[0]` times and assert the departure is 30 days after `now` and the arrival 18 h later). 9g's `WizardCompletionBoardSeedTest` stays green unchanged.
  - `PerfMockSourcesSeedTest` in `app/src/testDebug` (JVM or Robolectric `@Logic`, whichever 4e used for `PerfMockSources`): with `demoAccount = true` and a fresh temporary root, `signIn("demo@example.com", "password")` on the returned source succeeds; with `false`, it fails as an unknown account. In a debug test `BuildConfig.DEBUG` is true, so 4e's `debugAppSeed = BuildConfig.DEBUG` would seed the demo anyway: give `PerfMockSources` a constructor parameter `debugAppSeed: Boolean = BuildConfig.DEBUG` (behaviour unchanged) and pass `false` in this test, as the perf variants get; with an existing state file, the flag changes nothing.
- plan.md "Android spec: seeds and test hooks": add `cf.perf-account=demo` and `cf.perf-wizard=WizardSaveJourney` (perf variants and debug only).

### Task 9: Release smoke on the minified build

**Commit:** `test(benchmark): smoke-test the minified release build`.

- `benchmark/…/Journeys.kt` (shared helpers, used here and by Task 10):
  - `fun UiDevice.clearApp(pkg: String)` = `executeShellCommand("pm clear $pkg")`;
  - `fun MacrobenchmarkScope.launch(extras: Map<String, String> = emptyMap())` = `startActivityAndWait { intent -> extras.forEach { (k, v) -> intent.putExtra(k, v) } }` (a bound `it::putExtra` reference is ambiguous across the overloads);
  - `fun MacrobenchmarkScope.signInDemo()`: launch with `cf.perf-account=demo`; one wait for either screen, `device.wait(Until.hasObject(By.res(Pattern.compile("welcome\\.signIn|today\\.screen"))), 6_000)` (a signed-in iteration must not wait 6 s for a Welcome that never comes); if `welcome.signIn` exists: click it, wait `sign-in.email` (3 s), `findObject(By.res("sign-in.email")).text = "demo@example.com"`, same for `sign-in.password` with `"password"`, click `sign-in.submit`; then `check(device.wait(Until.hasObject(By.res("today.screen")), 10_000))`;
  - `fun UiDevice.dismissFirstTripOffer()`: `if (wait(Until.hasObject(By.res("reminders.firstTrip")), 1_000)) { pressBack(); check(wait(Until.gone(By.res("reminders.firstTrip")), 3_000)) }`. 12c shows this sheet once on a trip page while the account's `wizardReminders` setting is `"pending"` and notification permission is undetermined, which is the case on a fresh install (12c §5.6; back = "Not now", `onDismissRequest = onNotNow`); call it after every `trip.timeline.ready` wait, before touching the page;
  - `fun UiDevice.flingPage(times: Int, dir: Direction)` on `By.res("trip.page")` with `setGestureMargin(displayWidth / 5)`;
  - `fun UiDevice.firstVisibleBlock(): UiObject2` = `findObjects(By.res(Pattern.compile("trip\\.block\\..*"))).minBy { it.visibleBounds.top }` (one query).
  For the non-Macrobenchmark smoke test, write the same helpers as `UiDevice` extensions that start the activity with `am start -n net.chronoflow.app.native/net.chronoflow.app.MainActivity --es <k> <v>` through `executeShellCommand`.
- `benchmark/…/ReleaseSmokeTest.kt` (`@Perf`, `@RunWith(AndroidJUnit4::class)`, plain JUnit4 + UiAutomator against the installed `benchmarkRelease`, which R8 minifies with the release rules):
  - `@BeforeClass`: `logcat -b crash -c`; `pm clear`; sign in once (`signInDemo` equivalent); ≤ 20 s.
  - `todayTripsAndSettingsOpen`: launch; `today.screen`; click `tab.trips` → `trips.list`; `tab.settings` → `settings.signOut`; `tab.today` → `today.screen`.
  - `tripSheetSurvivesRecreation`: launch; `tab.trips`; click `trip.0199a3c0-0000-7000-8000-000000000001`; wait `trip.timeline.ready` (10 s); `dismissFirstTripOffer()`; click `firstVisibleBlock()`; wait `step.sheet` (5 s); read the old `settings get system font_scale`; `settings put system font_scale 1.15` (font scale recreates the activity, plan A8); wait `step.sheet` again (5 s) and `trip.page`; in `finally` restore the old value (`settings delete system font_scale` when it was `null`).
  - `wizardSavesToTripPage`: launch with `cf.perf-wizard=WizardSaveJourney`; the 9g steps: click `wizard.choice.one-way`; wait until `wizard.fine-tune.continue` is enabled (5 s), click; wait until `wizard.ready.continue` is enabled (10 s, mock generation), click; wait `trip.page` (5 s).
  - Every method: record `pidof net.chronoflow.app.native` after the first wait and assert it is unchanged at the end; `@After` asserts `logcat -b crash -d` has no line containing `net.chronoflow.app.native`. Each method < 10 s on `pixel` (record durations); hang cap `timeout_msec=60000`.
- `Scripts/release-smoke.sh`, run by `make release-smoke` through `$(RUN_TIER) release-smoke --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/release-smoke.sh`: one device; on a physical device exit 2 with `release-smoke clears app data: set PERF_CLEAR=1` unless `PERF_CLEAR=1` (OQ-13-15; the test runs `pm clear`); functional prep (animator scales 0 are fine here); `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.ReleaseSmokeTest -Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000` (no `enabledRules`, as 3b's `ProcessDeathLaunchTest`); `PERF_DRY_RUN=1` prints instead of running. Not in `make check` (OQ-13-9).
- **Tests:** `Scripts/tests/test_release_smoke.py` (dry run with stub `adb`/`gradlew` on `PATH`): the Gradle line names the class and `timeout_msec=60000`; no `suppressErrors` and no `enabledRules`; exit code follows the stub Gradle; a stub physical device (`ro.kernel.qemu` empty) without `PERF_CLEAR` → exit 2 before Gradle.
- A crash in this test is an R8 finding: fix it with the narrowest keep rule in a new file under `app/src/main/keepRules/` (register it as 0a registered `app.keep`, add it to `KeepRulesTest.keepRuleFilesAreKnown`), with the stack trace (deobfuscated with `retrace`) in the commit body: `fix(app): keep <what> for <why>`.

### Task 10: Baseline and startup profiles across the main journeys

**Commit:** `perf(app): profile the today, trip and wizard journeys`. **Ticks:** "Profiles".

Extend `benchmark/…/BaselineProfileGenerator.kt` (keep `@Perf`, `BaselineProfileRule`, the `packageName` source). Every method has `@Before fun clean() = device.clearApp(packageName)` (a fresh state per journey; `pm clear` kills the process and resets the mock, credits and any wizard recovery row) [verify that `pm clear` before `collect` does not empty the collected profile; if it does, clear inside the first iteration of the block instead and record it]. Every block of the new methods starts with `killProcess()` (`MacrobenchmarkScope`), so each iteration is a cold start whatever the previous iteration left on screen. Steps use the §2 P12 tags; every wait is bounded; no sleeps.

| Method | `includeInStartupProfile` | Block (each iteration) |
|---|---|---|
| `startup` (exists; **keep as 7 left it**) | true | 1e's block as extended by 7 (`phase-7-today.md` l.448): it launches into Today (with 3b's signed-in stub extras) and waits for `today.trip` or `today.empty`. Do not change it |
| `startupSignedOut` (new) | true | `killProcess()`; `pressHome()`; `startActivityAndWait()` (no extras); wait `welcome.signIn` (6 s) |
| `startupSignedIn` (new) | true | `killProcess()`; `signInDemo()` (signs in through the form on the first iteration only; later iterations restore the stored session and reach `today.screen` directly); wait `today.trip` (10 s) |
| `tripJourney` (new, `04 §10.5`; trip choice OQ-13-17) | false | `killProcess()`; `signInDemo()`; click `tab.trips`; wait `trips.list`; click `trip.0199a3c0-0000-7000-8000-000000000001`; wait `trip.timeline.ready` (10 s); `dismissFirstTripOffer()`; `flingPage(5, DOWN)`, `flingPage(5, UP)`; click `trip.viewToggle`; `flingPage(3, DOWN)`; click `trip.viewToggle`; wait `trip.timeline.ready`; click `firstVisibleBlock()`; wait `step.sheet` (5 s); `pressBack()`; wait gone `step.sheet` (3 s); click `trip.dayMenu`; click the highest-index visible `trip.dayMenu.day.<n>` (one `findObjects` with the pattern `trip\.dayMenu\.day\.\d+`); `pressBack()` to My trips; click `tab.today`; click `today.trip`; wait `trip.scroll` (5 s); `pressBack()` |
| `timelineLarge60` (exists, with 6c's list toggle; extend) | false | as it is, then: click `trip.dayMenu`, click `trip.dayMenu.day.59` (scroll the menu as 1e's `timelineLastDay` does), then up to 6 `flingPage(1, DOWN)` until `trip.plan.end` exists |
| `wizardToReady` (new) | false | `killProcess()`; `launch(mapOf("cf.perf-account" to "demo", "cf.perf-wizard" to "WizardSaveJourney"))`; one wait (10 s) for `welcome\.signIn\|wizard\.choice\.one-way\|wizard\.ready` (one `By.res(Pattern)`); if `welcome.signIn`: sign in through the form as in `signInDemo` (no relaunch), then the same wait for `wizard\.choice\.one-way\|wizard\.ready`; if `wizard.choice.one-way`: click it; wait until `wizard.fine-tune.continue` is enabled (5 s), click; wait until `wizard.ready.continue` is enabled (10 s); if `wizard.ready` is already shown, the iteration ends there (from the second iteration 9c's quiet recovery reopens the saved Ready step on launch, and the launcher's pending guard ignores the second open). Do not save (keeps the 3 credits) |

- `Scripts/baseline-profile.sh`: keep 1e's steps; on a physical device exit 2 unless `PERF_CLEAR=1` (the generator clears app data, OQ-13-15); keep `timeout_msec=900000` and the Makefile's `TIER_IDLE_TIMEOUT := 1200` as they are (already above OQ-13-7's 600000; R12). Do not lower `maxIterations`.
- Run `make baseline-profile` on `pixel` and commit both regenerated files in this commit. Never hand-edit them.
- Then run `make perf` and `make perf-timeline` once each to prove `BaselineProfileMode.Require` passes with the new files (the full recorded runs are Task 12).
- **Test:** extend `BaselineProfileFilesTest`. `baseline-prof.txt` holds at least one line for each of these classes (use the FQNs the code actually has; find them with `grep -rn "fun TodayScreen\|fun TripsScreen\|fun TripPage\|fun DayListRow\|fun StepSheet\|fun WizardReadyScreen\|fun WelcomeScreen\|class SignInModel" feature app --include=*.kt`): the Today screen, the My trips screen, the trip page, the list row, the step sheet, the wizard Ready screen, Welcome, the sign-in model, plus 1e's four. `startup-prof.txt` holds `Lnet/chronoflow/app/MainActivity;`, the Today screen's class and the Welcome screen's class. This guards against an empty or partial commit, not against coverage drift.

### Task 11: Launch comparison with the RN release APK

**Commit:** `perf(benchmark): compare launch with the rn release build`.

- `benchmark/…/LaunchComparisonBenchmark.kt`:
  ```kotlin
  @Perf
  @RunWith(Parameterized::class)
  class LaunchComparisonBenchmark(private val target: Target) {
      enum class Target(val packageName: String) { Native("net.chronoflow.app.native"), Rn("net.chronoflow.app") }
      companion object { @JvmStatic @Parameterized.Parameters(name = "{0}") fun targets() = Target.entries.toList() }
      @get:Rule val rule = MacrobenchmarkRule()

      @OptIn(ExperimentalMetricApi::class)
      @Test fun coldSignedOut() = rule.measureRepeated(
          packageName = target.packageName,
          metrics = listOf(StartupTimingMetric(), FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
          iterations = 10,
          startupMode = StartupMode.COLD,
          compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
          setupBlock = { device.executeShellCommand("pm clear ${target.packageName}"); pressHome() },
      ) {
          startActivityAndWait()
          // Fixed 3.5 s capture for both apps: covers the native Welcome intro (ends at 3200 ms).
          device.wait(Until.hasObject(By.res("cf.never")), 3_500)
      }
  }
  ```
  Both apps get the same mode (`UseIfAvailable`: each installs the profile its APK embeds; the RN APK has `assets/dexopt/baseline.prof` and `ProfileInstallReceiver`, §3.3). The native budget runs stay on `Require` (P6).
- `Scripts/perf-compare-rn.sh`, run by `make perf-rn` through run-tier like `make perf` (with `--device`):
  1. one device; `IS_EMU`/`CLASS` as `perf-startup.sh`; GPU `host` on the emulator; on a physical device exit 2 unless `PERF_CLEAR=1` (the benchmark runs `pm clear` on both apps, OQ-13-15);
  2. `RN_APK=${RN_APK:-/Users/rickgout/Sites/chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk}`; refuse if missing; print `shasum -a 256`; refuse (exit 3, `RN comparison open: needs a prebuilt RN release APK from the user (OQ-A591)`) unless the sha equals §3.3's `4ee240d3…b3b1f5` or `RN_APK_SHA` names the new one; `adb install -r "$RN_APK"` (reading the file only; never build, install, prebuild or run Gradle/npm in the RN repo; R15);
  3. trap `prep-functional`, then `prep-perf`; clear the additional-output directory;
  4. `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.LaunchComparisonBenchmark -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000`, plus `…androidx.benchmark.suppressErrors=EMULATOR` on the AVD. [verify] whether Macrobenchmark refuses the non-profileable RN APK; if it does, append `NOT-PROFILEABLE` to `suppressErrors` (comma-separated) and record it; if `StartupTimingMetric` still yields no RN value, fall back for RN only to 10 × `am start -W -S -n net.chronoflow.app/.MainActivity` after `pm clear`, parse `TotalTime`, and record those as `rnAmStartTotalTimeMs` (report, OQ-13-3);
  5. `python3 -I Scripts/check-benchmarks.py --suite compare --device-class $CLASS --results-dir … --history docs/perf-startup.md --rn-apk-sha "<sha>" --commit … --append`;
  6. exit non-zero if Gradle or the checker failed. `PERF_DRY_RUN=1` prints.
- `Scripts/check-benchmarks.py --suite compare`: matches benchmarks named `coldSignedOut[Native]` and `coldSignedOut[Rn]` [verify the parameter suffix in `name`; adapt the matcher to what the JSON shows and record it]. `budgets.json` gains:
  ```json
  "compare": [
    {"test": "coldSignedOut", "metric": "timeToInitialDisplayMs", "stat": "median", "unit": "ms", "rule": "nativeBelowRn",
     "emulator": {"report": true}, "reference": {"gate": true}, "lowEnd": {"gate": true}},
    {"test": "coldSignedOut", "metric": "memoryRssAnonMaxKb", "stat": "median", "unit": "KiB", "rule": "nativeBelowRn",
     "emulator": {"report": true}, "reference": {"gate": true}, "lowEnd": {"gate": true}},
    {"test": "coldSignedOut", "metric": "frameOverrunMs", "stat": "P99", "unit": "ms", "rule": "report",
     "emulator": {"report": true}, "reference": {"report": true}, "lowEnd": {"report": true}}
  ]
  ```
  Use the raw keys the JSON shows (`RAW_KEYS`). Output per row: `Compare metric: coldSignedOut <metric> <stat> native=<x> rn=<y> delta=<±z%> <BELOW|ABOVE> <PASS|FAIL|REPORT>`; also P90 for TTID. `nativeBelowRn` with `gate` fails on `ABOVE`; with `report` only prints. A missing side fails (`missing Rn result`). History: append rows to a `## RN comparison` table in `docs/perf-startup.md` with header exactly `| Date | Commit | Class | Device | API | RN APK sha256 | Metric | Stat | Native | RN | Δ % | Verdict |` (sha shortened to 12 characters).
- **Tests:** `Scripts/tests/test_check_benchmarks.py` gains fixtures `Scripts/fixtures/benchmarks/compare/{below,above,missing-rn}/` (both packages' JSON in one file as Macrobenchmark writes them): `below` with `--device-class reference` → exit 0, `BELOW PASS`; `above` with `reference` → exit 1; `above` with `emulator` → exit 0, `ABOVE REPORT`; `missing-rn` → exit 1 naming `Rn`; `--append` adds exactly one row per metric with 12 cells. `Scripts/tests/test_perf_compare_rn.py` (dry run, stubs): refuses a missing APK; prints the sha; `suppressErrors=EMULATOR` only on the AVD; the checker runs after a stubbed Gradle failure; the trap restores the scales.
- `docs/perf-startup.md` "RN comparison protocol": signed out, app data cleared before every iteration on both apps, same device and harness, 10 iterations, fixed 0–3.5 s capture, `UseIfAvailable` for both, the RN APK identity of §3.3, and that clearing RN's data removes the demo sign-in that research left on the AVD (`12 §1`).

### Task 12: Run the full budget and record it

**Commit:** `docs(perf): record the phase 13 performance budget run`. **Ticks:** "Performance budget".

Runs, in order, on `pixel` (`make emu`, `-gpu host`, the scripts set the animator scales to 1 and restore them). The orchestrator runs them if you cannot; never edit numbers by hand.

| # | Command | Gated on the emulator |
|---|---|---|
| R1 | `make perf` | `frameCount` min, `welcomeInteractiveMs`, `introLastItemMs`, finite TTFD; the rest report |
| R2 | `make perf-timeline` (light, 1.0) | first frame ≤ 800 ms, peak RSS ≤ 153,600 KiB, `timelineLastDay`, `timelineBuildOnMainCount` = 0, at most 6 placeholders per iteration, the CPU band (plan.md "Where gates apply"), `listScroll` present |
| R3 | `THEME=dark make perf-timeline` | same |
| R4 | `FONT_SCALE=1.3 make perf-timeline` | same |
| R5 | `make perf-rn` | report (emulator) |
| R6 | `make size` | `OK` verdict, no `.so`, no language split |
| R7 | `make release-check` | all |
| R8 | `make release-smoke` | all |
| R9 | `Scripts/measure-tiers.sh` (three warm runs each, `pixel` booted) | `make quick` < 60 s, `make check` < 5 min |
| R10 | Only if `adb devices` lists a physical device (stop the emulator first): `DEVICE_CLASS=reference make perf`, `DEVICE_CLASS=reference make perf-timeline`, `DEVICE_CLASS=reference make perf-rn` (or `lowEnd`), after the `04 §11` device preparation (airplane mode, battery saver off, > 50% charge, cooled, refresh rate from `dumpsys display`) | the device class's hard limits; then the Perfetto device review of `04 §10.4` (S3 and large60) |

- A gate that misses: re-run that configuration once to rule out noise and record both rows. If it still misses, it is a **fix task in this phase**: a separate `perf(<scope>): …` commit with the cause from the trace, then re-run the affected rows (and `make baseline-profile` if code on a profiled path changed). Never change a budget, never mark a gate "waived".
- A ±25% trend flag is recorded with a one-line cause; it does not fail the phase.
- **`docs/perf-startup.md`** "Phase 13": the rows the checker appended for R1, R5 and R10, plus a table mapping each startup row of §5.7 to its value and verdict (PASS / REPORT / open: device only (OQ-A22)). Add "Peak memory to Welcome" and the RN comparison summary (TTID median/P90, memory median, overrun P99, for both apps).
- **`docs/perf-timeline.md`** "Phase 13": the rows for R2–R4 (and R10), the same row-to-verdict table for the timeline rows, and the device section: "Layer 4 device review: open: device only (OQ-A22)" unless R10 ran.
- **`docs/tooling.md`** "Phase 13": R9's medians (and the slowest 10 tests of `quick`), every [verify] outcome of this brief that concerns tooling, and the commands the orchestrator ran for you.

### Task 13: Play readiness

**Commit:** `docs(release): record data safety, policy checklist and asset links`. **Ticks:** "Play readiness".

Write `docs/play.md` with exactly these sections:
1. **Identity:** applicationId `net.chronoflow.app.native` (OQ-A1: switch to `net.chronoflow.app` before the first upload only if the user confirms the RN app was never on Play; the switch changes `assetlinks.json` and the passkey origin), versionName/Code, minSdk 29, targetSdk 36, compileSdk 37, Play App Signing with an upload key (`docs/release.md`).
2. **Data safety:** the §3.2 table verbatim, the security section, the not-collected list, the privacy URL, and "Encryption at rest: file-based encryption is mandatory on devices that launch with Android 10+ (minSdk 29); no SQLCipher" (`14 §9.2`). Mark "Health info: Yes (conservative)" as matching OQ-A41's default.
3. **Policy checklist P1–P11:** a table `| # | Requirement | Status | Evidence |` with status `done`, `pending (user)` or `pending (server S<n>)`. Fill evidence from this phase: P1 from Task 4 (`targetSdkVersion 36`); P2 from Task 5 (no `.so` except the OQ-0a-22 `libandroidx.graphics.path.so`, which must be 16 KB aligned: record `zipalign -c -P 16 -v 4` on the release APK); P3 in-app delete (10d) + the web URL + S4 gap; P4 wording to enter in the Console, kept within the no-health-data decision; P5 S5 pending; P6 18+, IARC all "no", no ads; P7 Task 6's `INFO billing: absent` line (`com.android.billingclient` absent from the release APK); P8 Task 4 (no exact-alarm permission); P9 section 2; P10 S1/S2 pending, passkeys hidden behind `PasskeyCapability` (A15); P11 to run after the first internal upload.
4. **`assetlinks.json` for S1:** the JSON from `phase-5c-password-reset-links.md` Task 10 with `package_name` `net.chronoflow.app.native` and `"<Play app-signing certificate SHA-256, AA:BB:… form>"` as the only fingerprint, plus: "Never the committed debug key (OQ-A38); add the upload key only if sideloaded release builds must verify links (5c OQ-5c-4); redo on an applicationId or key change."
5. **Server dependencies S1–S6:** the `14 §12` table with a status column.
6. **Tracks:** internal → closed (12 testers × 14 days if the account is personal, OQ-G11) → production staged 10/50/100% with the vitals halt rule (crash 1.09%, ANR 0.47%, 8% per model).
7. **Pre-upload checklist:** `make release-check`, `make release-smoke`, `make perf`, `make perf-timeline`, `make size`, `make version-bump PART=build`, `./gradlew :app:bundleRelease` with `CF_UPLOAD_*` set, `jarsigner -verify`, upload to internal testing, then on the internal build against the real API: sign in, open a trip, check off a step (OQ-13-18). Before the first production upload (not internal testing), one passing `DEVICE_CLASS=reference make perf-timeline` (120 Hz) and one passing `DEVICE_CLASS=lowEnd make perf-timeline` are required, or the user's waiver recorded in plan.md OQ-A45; `docs/play.md` lists this item as open until then.

### Task 14: Plan, agents and open questions

**Commit:** `docs(plan): record phase 13 decisions`. **Ticks:** "Release readiness".

- plan.md: append OQ-13-1 … OQ-13-18 (§10) to "Open questions for the user", as the next free `OQ-A` numbers at the time of the commit (check the table; never reuse a number), with their defaults. Add `make release-check`, `make release-smoke`, `make perf-rn` and `make version-bump` to "Android spec: release" as one bullet.
- AGENTS.md "Commands": add four rows: `make release-check` (assembles release and checks tests, manifest, APK content and size), `make release-smoke` (the minified release smoke test on `pixel`), `make perf-rn` (launch comparison with the RN release APK), `make version-bump PART=build|patch|minor|major`.
- `docs/release.md`: final "Tooling notes" with every release [verify] outcome.
- Tick "Release readiness" here: by now `testReleaseUnitTest` and `verifyReleaseManifest` pass (Tasks 4, 7, 8), `make release-check` passed (Task 6) and exit-info logging is debug-only (Task 7). If any of these is not green, do not tick; report.

---

## 7. Plan boxes and commits

| plan.md checkbox | Ticked in |
|---|---|
| Release build | Task 5 (Tasks 1–5 deliver it) |
| Profiles | Task 10 |
| Performance budget | Task 12 |
| Play readiness | Task 13 |
| Release readiness | Task 14 (Tasks 6–9 deliver it) |

Fix commits created by Tasks 5, 6, 9 or 12 sit between the task commits and tick nothing.

---

## 8. Acceptance criteria

1. `make quick` passes (< 60 s warm), including `KeepRulesTest`, `test_repo_keys.py`, `test_bump_version.py`, `test_check_release_apk.py`, `test_release_smoke.py`, `test_perf_compare_rn.py` and the extended `test_check_benchmarks.py`.
2. `./gradlew :build-logic:convention:test` (or the module path 0a uses) passes: `UploadSigningTest`, `VerifyReleaseManifestTest`, `BundleSizeTest`.
3. `make targeted ONLY="app:PerfJourneyExtrasTest app:PerfWizardJourneyTest app:PerfMockSourcesSeedTest app:ExitInfoLoggerTest app:BaselineProfileFilesTest konsist:KeepRulesTest"` passes; every JVM test < 1 s, every Robolectric test < 5 s (`build/test-results/*/timings.json`).
4. `./gradlew :app:testReleaseUnitTest :app:verifyReleaseManifest` passes; `ReleaseReadinessTest` covers `ExitInfoLogger`, `PerfJourneyExtras`, `PerfJourneyState`, `PerfWizardJourney`.
5. `make release-check` prints `release apk: OK …` and a size line with `OK` (≤ 8,000,000 B), no `.so`, no language split.
6. `make release-smoke` passes on `pixel`: three methods, each < 10 s, no crash line, pid unchanged.
7. `./gradlew :app:bundleRelease` succeeds unsigned without `CF_UPLOAD_*`, and signed with the throwaway key (recorded in `docs/release.md`; the key was deleted).
8. `make baseline-profile` regenerated both files in Task 10's commit; `make perf` and `make perf-timeline` pass with `BaselineProfileMode.Require`.
9. R1–R9 of Task 12 ran; every functional and emulator gate of §5.7 passes; device rows pass or read "open: device only (OQ-A22)"; `make perf-rn` produced both apps' TTID and memory, recorded in `docs/perf-startup.md`. Unless they ran, the OQ-A45 device runs are listed in `docs/play.md` as a pre-upload blocker.
10. `make check` passes with `pixel` booted, < 5 min warm (R9).
11. `docs/release.md`, `docs/play.md`, `docs/perf-startup.md` "Phase 13", `docs/perf-timeline.md` "Phase 13" and `docs/tooling.md` "Phase 13" exist and are complete.
12. All five Phase 13 boxes in plan.md are ticked in the commits of §7; `git log` shows atomic Conventional Commits without trailers; `git ls-files` shows no key but `app/debug.keystore`.
13. StrictMode is clean in debug during `make check`'s smoke set (no new violation lines in `adb logcat -d -s StrictMode` with `net.chronoflow` frames).

---

## 9. Review checklist (Opus reviewer)

- [ ] **No secrets in the repo or logs.** `git ls-files` has no keystore but `app/debug.keystore`; no `CF_UPLOAD_*` value is committed; the signing code never prints a password (search the build output of a signed build for the throwaway password).
- [ ] **Signing guard.** Partial `CF_UPLOAD_*` fails configuration; a store inside the repo fails; only `release` gets `upload`; perf variants stay debug-signed; reads go through `providers` (run `./gradlew help --configuration-cache` twice: the second reuses the cache).
- [ ] **Version rule.** The script's formula equals `VersionProperties.parse`; resets are correct; the file keeps its key order; no version was bumped in this phase.
- [ ] **Keep rules.** No broad `-keep` for `net.chronoflow.**`, no `-dontobfuscate`/`-dontoptimize`; any new rule names the R8 smoke failure that needed it; the release and `benchmarkRelease` configurations match (Task 3 diff quoted). Typical mistake: adding `-keep` for all `@Serializable` routes "to be safe".
- [ ] **Manifest.** The allowlist is exact (required set present, nothing else), exported components are named, not wildcards; 12b's two receivers are `exported="false"`; guard permissions are exactly `DUMP` and `BIND_JOB_SERVICE`; `job-service.xml` passes and `exported-unknown-receiver.xml` fails.
- [ ] **Size and splits.** `make size` verdict is `OK`, not `WARN`; the language-split check was proven once against a real `base-en.apk`, then reverted.
- [ ] **Release content.** The APK check deobfuscates through `mapping.txt` (a check on obfuscated names proves nothing); `Log.v/d/i` from our classes is zero; seed strings and mock accounts are absent; `ExitInfoLogger` only in debug.
- [ ] **Perf extras stay out of release.** `PerfJourneyExtras`, `PerfJourneyState`, `PerfWizardJourney` live in `app/src/perf`, their tests in `app/src/testDebug`; no 9c/9g debug file moved or changed; `PerfWizardJourneyTest` proves the perf draft equals 9g's; 9g's seed test is unchanged and green; the debug board captures still work.
- [ ] **Clean perf state.** Every perf script clears app data before Gradle; the signed-out `startupCold` cannot start signed in after a profile run (check the order in the dry-run tests).
- [ ] **Profiles.** Generated, not hand-edited; both files changed in Task 10's commit; `BaselineProfileFilesTest` names real classes; `startup-prof.txt` holds only the three startup journeys' classes (`startup`, `startupSignedOut`, `startupSignedIn`; no wizard or trip-page classes from other journeys); every new block starts with `killProcess()`; `maxIterations` not lowered.
- [ ] **Smoke and journeys.** No `Thread.sleep`; waits are bounded; the font-scale change is restored in `finally`; the smoke test asserts the sheet *after* recreation, not before.
- [ ] **RN comparison fairness.** Same harness, same iterations, same `UseIfAvailable` mode, same 3.5 s window, data cleared for both; the RN APK was installed from its build output (sha recorded) and the RN repo was not touched (`git -C /Users/rickgout/Sites/chronoflow-mobile status --short` unchanged before and after).
- [ ] **Numbers, not impressions.** Every row in the docs has date, commit, device, API, refresh rate, font scale, theme, build type and command. Emulator numbers are never written as device claims. No budget was edited; a missed gate has a fix commit and a re-run row.
- [ ] **Data safety and checklist** match `14 §9.2`/`§10` exactly; `assetlinks.json` holds one placeholder fingerprint and never the debug key; the health wording stays within the no-health-data decision (no symptom-relief promise).
- [ ] **Tests rules.** Cheapest tier: script logic in Python unit tests with fixtures, build-logic rules in JVM tests, only the smoke and benchmarks on the emulator; budgets met; tier annotations present (`@Perf` on the new benchmark classes, `@Logic` on the Robolectric test).

---

## 10. Open questions (proceed on the default; Task 14 adds them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-13-1 | Plan "Peak memory to Welcome" names `MemoryUsageMetric` in `StartupBenchmark`, whose measured block ends at the first frame, before Welcome. | Measure it in `LaunchComparisonBenchmark` (0–3.5 s, covers Welcome's 3200 ms, same harness for both apps). `StartupBenchmark` is unchanged. |
| OQ-13-2 | The RN comparison clears app data before each iteration, which removes the RN demo sign-in research left on the AVD (`12 §1`). | Clear it (iOS Phase 7: "fresh signed-out install per app"). Record it in `docs/perf-startup.md`. |
| OQ-13-3 | The RN APK is not profileable; Macrobenchmark may refuse it or return no startup timing. | Add `NOT-PROFILEABLE` to `suppressErrors`; if RN timing is still missing, measure RN with `am start -W` `TotalTime` (report only) and say so. Never rebuild the RN app. |
| OQ-13-4 | Compilation mode for the comparison. | `Partial(UseIfAvailable)` for both apps (each installs its own embedded profile). Budget runs stay `Require`. |
| OQ-13-5 | Perf variants have no account (iOS Perf never seeds, 4c OQ-4c-4); the trip and wizard journeys and the release smoke need one. | A perf-only `cf.perf-account=demo` extra seeds the perf mock with the demo account; sign-in still goes through the real form. Fallback if the wiring needs main-code changes beyond one nullable hook member: run those journeys on fixture pages only (`cf.trip-page=S3`, the 1e large60 entry) and record the reduced coverage. |
| OQ-13-6 | Wizard journey "up to Ready" without seeds in perf. | A perf copy `PerfWizardJourney.saveJourney` of 9g's `WizardSaveJourney` draft in `app/src/perf`, installed through the main `AppGraph.wizardStartOverride` seam and opened with `cf.perf-wizard`; 9c/9g's debug code stays where it is (`feature/wizard/src/debug` cannot call `:app`), and `PerfWizardJourneyTest` keeps the copy equal to it. Fallback: the profile covers Welcome → `welcome.plan` → the guest wizard's first screen only; recorded. |
| OQ-13-7 | Profile generation with five journeys exceeds 1e's 300 s `timeout_msec`. | `600000` for `baseline-profile.sh` only; other caps unchanged. |
| OQ-13-8 | The release smoke types credentials, while AGENTS.md allows that only in auth tests. | Allowed for this one class: the minified build must exercise the real sign-in path, and perf variants have no seed receiver. Sign in once per class in `@BeforeClass`. |
| OQ-13-9 | Should `release-check`, `release-smoke` and `perf-rn` run in `make check`? | No: they build minified variants and would break the 5 min budget (iOS build-speed audit kept release builds out of the inner loop). They run in this phase and before every upload (`docs/play.md` checklist). |
| OQ-13-10 | Bump the version for the first upload? | No. The first upload is `0.1.0` (`10000`); every later upload bumps at least `build`. |
| OQ-13-11 | Partial `CF_UPLOAD_*` values: unsigned or fail? | Fail configuration, naming the missing keys; none set → unsigned. |
| OQ-13-12 | LeakCanary is in A1 (debug) but no phase added it. | Not in Phase 13. Phase 14 or the user decides; it adds an eager initializer to debug. |
| OQ-13-13 | `androidx.compose.ui.tooling.preview` in the release APK. | Allowed if present (annotations only); every other `ui.tooling` package is forbidden. |
| OQ-13-14 | The language split check relies on bundletool's split names. | Proven once against a temporary `enableSplit = true` build; recorded. |
| OQ-13-15 | `pm clear` before perf runs on a physical device would wipe a tester's data. | Clear on the emulator always; on a device only with `PERF_CLEAR=1`. The scripts' own `pm clear` is skipped without it and the run records "state: as found"; `release-smoke`, `perf-rn` and `baseline-profile`, whose tests clear app data themselves, refuse a physical device without it. |
| OQ-13-16 | plan.md Phase 13 says "Measure the RN release APK with the same `StartupBenchmark` harness". `StartupBenchmark` carries native-only trace metrics (`first-commit`, `session`, `WelcomeMomentMetric`), the signed-in stub case and `BaselineProfileMode.Require`; a missing metric fails (AGENTS "Tests" 9), so RN cannot run through it unchanged. | `LaunchComparisonBenchmark`: the same Macrobenchmark harness, module, `StartupTimingMetric`, `StartupMode.COLD`, 10 iterations and signed-out state as `StartupBenchmark`, parameterized over both packages; `StartupBenchmark` stays native-only. Report this difference from plan.md in the hand-back. |
| OQ-13-17 | `04 §10.5`'s trip journey opens S3. In the demo seed S3 is `…0003` (AMS→SYD, **completed**, last section of My trips, below the fold). | Use the active S1 trip `…0001` (AMS→HND, 13 days): it is on screen without scrolling the list and is also the Today hero, so one journey covers both entries. Large60 keeps the long-trip coverage. |
| OQ-13-18 | The release smoke runs on `benchmarkRelease`, which uses the mock data source, so R8 is not proven on the `ApiClient` DTO (de)serialization path that release uses. | Accept for this phase: the generated DTOs are `@Serializable` with kotlinx.serialization's bundled rules, and the mock's state file goes through the same `ApiJson`. The first internal-track build against the real API is the proof; add it to the `docs/play.md` pre-upload checklist as "sign in, open a trip, check off a step on the internal build". |

Inherited questions on their defaults for this phase: OQ-A1 (`.native` id), OQ-A20 (startup budgets are proposals until a device run), OQ-A22 (device gates open until a device is attached), OQ-A28 (perf variant names as `docs/tooling.md` records), OQ-A38 (never the debug key in production asset links), OQ-A39 (no language splits), OQ-A41 (Data safety answers of `14 §9.2`), 1e OQ-1e-1…14.

---

## Refresh notes (2026-11-22, against 1c0333db)

Refreshed by the orchestrator against `feat/phase-13-release-perf` at `1c0333db` (top of 12c; every phase through 12c is in the stack). Each note gives what the body said, what HEAD has, and the instruction you follow. **Where a note and the body disagree, the note wins.** Before Task 1, repeat these checks (the greps are in each note) and stop on any further mismatch. The body was patched inline only where a stale fact would mislead (marked "R<n>").

**R1. plan.md line numbers.** Body: Phase 13 entry l.2737–2783; "Performance budget" l.491–538 (rows 507–537); spec "seeds and test hooks" l.465–471, "release" l.473–479. HEAD: Phase 13 entry `plan.md:3598–3645`; "Performance budget" `plan.md:511–564` (table rows 527–563, "Where gates apply" l.518); "Android spec: build" l.282–320; "manifest and permissions" l.450–464; "seeds and test hooks" l.466–479; "release" l.481–487; "Risks" l.566; "Open questions for the user" l.598. The five checkboxes and their wording are unchanged. **Instruction:** cite these numbers. The highest OQ-A number at HEAD is **OQ-A590**. This refresh adds OQ-A591…OQ-A598 (below). Task 14 appends them first, then OQ-13-1…18 as the next free numbers at commit time (expected OQ-A599…OQ-A616; re-check the table, never reuse a number).

**R2. P1 `verifyReleaseManifest`.** Body: 9 rules. HEAD: 10 rules: the body's 9 plus 5c's `APP_LINK`, which needs exactly one verified HTTPS auth-link filter on MainActivity (`ManifestRules.kt:8–19`, `:114–128`). Other facts:
- `allowedExportedComponents.set(emptySet())` (`AndroidApplicationConventionPlugin.kt:68`).
- Only `DUMP` exempts an exported component (`ManifestRules.kt:94–95`).
- `basePermissions` already holds exactly the five required permissions (`ManifestRules.kt:39–46`).
- The planted manifests are `backup, cleartext, custom-scheme, debuggable, direct-boot, exported-debug, missing-locale-config, network-security-config, permission, startup-initializer, test-only, valid`.
- Tests: `VerifyReleaseManifestTest` and `AppLinkManifestRulesTest`.

**Instruction:** every new planted manifest and the updated `valid.xml` keep MainActivity's `autoVerify` filter exactly as `app/src/main/AndroidManifest.xml:41–49` has it, or `APP_LINK` fires. `AppLinkManifestRulesTest` stays green unchanged.

**R3. Exported components and `SystemJobService` (OQ-A587).** Body §5.3 and Task 4 assume WorkManager merges `SystemJobService` with `exported="true"`, and add `BIND_JOB_SERVICE` to `GUARD_PERMISSIONS`. HEAD: 12b overrides it to `android:exported="false"` with `tools:replace` (`app/src/main/AndroidManifest.xml:55–58`, commit `87e7aa94`, OQ-A587: "keep the verifier unchanged"). 12b's receivers live in `core/data/src/main/AndroidManifest.xml:6–15`, not in `:app`; both are `exported="false"`. That manifest also adds `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`. **Instruction (OQ-A592):**
- `GUARD_PERMISSIONS = setOf("android.permission.DUMP")`. Do not widen `EXPORTED_COMPONENT`, and keep the `SystemJobService` override.
- Replace the body's `job-service.xml` with `job-service-exported.xml`: `SystemJobService` with `exported="true"` and `BIND_JOB_SERVICE` must fail with exactly `EXPORTED_COMPONENT`. This guards OQ-A587.
- `valid.xml` holds `SystemJobService` with `exported="false"` and both reminder receivers with `exported="false"`.
- `allowedExportedComponents` stays empty. `EXPORTED` = MainActivity, which `MAIN_ACTIVITY` already exempts.
- Task 4's "Release manifest" section also lists every `tools:node="remove"` the app manifest applies:
  - the permissions `FOREGROUND_SERVICE`, `USE_BIOMETRIC` and `USE_FINGERPRINT`;
  - the services `RevocationBoundService` and Room's `MultiInstanceInvalidationService`;
  - the initializers `WorkManagerInitializer`, `EmojiCompatInitializer` and okhttp `PlatformInitializer`.

  WorkManager initializes on demand (`ChronoFlowApp`).

**R4. Size and `.so` (OQ-0a-22, OQ-A131).** Body: "no `.so` file". HEAD: `BundleSize.allowedNativeLibraries = setOf("libandroidx.graphics.path.so")` (`BundleSize.kt:25`); `MeasureBundleSizeTask` fails on any other `.so` (`MeasureBundleSizeTask.kt:45–56`). `app/build.gradle.kts` excludes `**/libdatastore_shared_counter.so` (OQ-A131). `bundle.language.enableSplit = false` is at `AndroidApplicationConventionPlugin.kt:52`. The last recorded size is 2,445,635 B (5b, `docs/perf-startup.md` "Release size"); phases 6–12c added WorkManager 2.12.0 and more since. **Instruction:** acceptance is "no `.so` except the allow-list" (body patched). Record the new size next to 5b's row. In `docs/play.md`, P2 records the allow-listed library's 16 KB alignment check.

**R5. Signing and version (P3).** HEAD: `version.properties` is `0/1/0/0`; `VersionProperties.parse` matches §5.2, including `major` 0..2099 (`VersionProperties.kt:13–21`). The application plugin configures only the `debug` signing config (`AndroidApplicationConventionPlugin.kt:38–43`), and `release` sets `optimization.enable = true` (l.49, the AGP 9 DSL, not `isMinifyEnabled`). No release signing config, `CF_UPLOAD_*` reader or bump script exists. `git ls-files` lists only `app/debug.keystore`; no committed properties file assigns `CF_UPLOAD_*`. **Instruction:** Tasks 1–2 as written.

**R6. Keep rules (P4).** HEAD:
- `app/src/main/keepRules/` holds `app.keep` (the Log v/d/i block) and `credentials.keep` (`-if class androidx.credentials.CredentialManager` / `-keep class androidx.credentials.playservices.** { *; }`).
- **Also** `app/src/nonMinifiedRelease/keepRules/profile.keep` holds `-dontobfuscate` and `-dontoptimize`, on purpose: profiles need real names, and AGP 9 ignores the plugin's legacy rules.
- `make check` runs `:app:analyzeReleaseR8Config` (`Makefile:47`; report at `build/reports/r8/r8-config-analyzer-release.html`, `docs/tooling.md:58`).

**Instruction (OQ-A595):** `KeepRulesTest`'s `noOptimisationEscapes`, `noBroadAppKeeps` and `keepRuleFilesAreKnown` scan `app/src/main/keepRules/` and every `app/src/<buildType>/keepRules/` other than `nonMinifiedRelease`. A separate `nonMinifiedProfileKeepIsExact` asserts that `profile.keep` holds exactly those two directives (comments aside), and that it is the only keep file under `src/nonMinifiedRelease`. `credentials.keep`'s package keep is the 5b rule; name it in the review and do not broaden it. Task 3's release/`benchmarkRelease` diff stays as written; `nonMinifiedRelease` is excluded from it.

**R7. Release APK content (Task 6), `net.chronoflow.app.perf` stubs.** Body §5.4 forbids every `net.chronoflow.app.perf.` class. HEAD: the release variant compiles four no-op stubs in that package from `app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf/`: `LaunchDiagnosticsParser`, `MainThreadStall`, `PerfJank` and `TripPageEntry`. `MainActivity` calls `TripPageEntry.content` and `PerfJank` (`MainActivity.kt:85–99`). `app/build.gradle.kts` adds `src/releaseNoPerf` to release only; `src/release` is also compiled into both perf variants. Other release-side facts:
- `app/src/release/kotlin/net/chronoflow/app/LaunchPlatform.kt:71–72` holds the literal `"cf.trip-page"` behind `BuildConfig.PERF_ENTRY` (a compile-time `false` in release).
- `app/src/main/.../LaunchDependency.kt:44–51` legitimately lists `cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay`, `cf.mode`, `cf.readyAt`, `cf.mainThreadStall` and `cf.reduceMotion`; release strips them.

**Instruction (OQ-A593):**
- `check-release-apk.py` builds the allowed stub set by listing the class names declared in `app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf/*.kt`, using an `--allowed-perf-stubs <dir>` argument that `make release-check` passes. It fails on any other `net.chronoflow.app.perf.*` class.
- Add a fixture: `net.chronoflow.app.perf.TripPageEntry` → exit 0; `net.chronoflow.app.perf.PerfMockSources` → exit 1.
- If the dex still holds `cf.trip-page`, the fix is to move that branch into a perf-side object. Never allowlist the string.
- Add these exact forbidden substrings, the 12b/12c and 5b debug seeds parsed only in `app/src/debug/.../TestSeed.kt`: `cf.notificationAuthorization`, `cf.notificationPermissionRequested`, `cf.firstTripReminders`, `cf.passkeyCapability`, `cf.holdSplash`.
- `cf.timeline-fixtures` no longer appears anywhere in `app/src`. Keep it in the list; it is harmless.

**R8. `ReleaseReadinessTest` (P5).** HEAD: `app/src/testRelease/kotlin/net/chronoflow/app/` holds seven classes: `ReleaseReadinessTest`, `LaunchOverridesReleaseTest`, `ReleaseApiConfigTest`, `ReleaseAppIdentityTest`, `ReleaseSeedExtrasTest`, `RootOverridesReleaseTest` and `WelcomeDestinationsReleaseTest`. `ReleaseReadinessTest` already asserts as missing:
- `WizardSaveJourney`, `WizardDebugSeeds`, `PerfMockSources`, `FixtureTodaySource`, `StubReadiness*`, `PerfEntry` and `JankStatsInstaller`;
- the guest and reminder debug classes (`net.chronoflow.debug.reminders.MockReminderCenter`, `…guest.GuestSignupSeed`/`Prefill`, `GuestWizardDebugSecrets`);
- the mock source.

It uses a private `missing(name)` helper (l.140). **Instruction:** add the new `missing(...)` lines (ExitInfoLogger, PerfJourneyExtras, PerfJourneyState, PerfWizardJourney, plus R10's hook classes) to the existing test methods' style. Do not duplicate assertions that already exist.

**R9. `ApplicationExitInfo` hook (Task 7).** Body: call it from "the debug side of the variant hook that runs after `first-commit`". HEAD: no exit-info code exists. `AppGraph.onFirstCommit()` (`AppGraph.kt:569–574`) completes `firstCommit` and launches `reminderRuntime.ensureChannel()` on `dispatchers.io`. There is no debug hook in it. The variant seam is the per-build-type object `LaunchPlatform`: `app/src/debug/kotlin/net/chronoflow/app/LaunchPlatform.kt`, and `app/src/release/...` (which is also compiled into the perf variants). **Instruction:**
- Add `fun onFirstCommit(app: Application)` to both `LaunchPlatform` objects. Debug calls `ExitInfoLogger.logPrevious(app)`. Release is a no-op, which also makes perf a no-op.
- Call it from the same `appScope.launch(dispatchers.io)` in `onFirstCommit`.
- Robolectric is 4.18 (`gradle/libs.versions.toml:31`), so the `[verify]` stands.

**R10. Perf journey wiring (Task 8, OQ-13-5/-6).** Body: `MainActivity`'s "perf hook" sets `PerfJourneyState`, and the override returns a draft from `PerfWizardJourney.saveJourney(now, zone)`. HEAD:
- **`WizardStartOverride`** (`app/src/main/kotlin/net/chronoflow/app/wizard/WizardStartOverride.kt:6–10`) is an interface `{ val presentation: WizardInitialPresentation; fun dependencies(session: TripWizardDependencies): TripWizardDependencies }`, not a draft supplier. `AppGraph.wizardStartOverride` has an `internal set` (`AppGraph.kt:98–102`).
- **Opening:** the wizard opens when `LaunchPlatform.prepareWizard(graph, epoch, locale)` returns true (`app/src/main/kotlin/net/chronoflow/app/shell/AppShell.kt:55`). Debug delegates to `WizardDebugSeeds.prepare` (`app/src/debug/.../LaunchPlatform.kt:24–35`). Release returns `false` (`app/src/release/.../LaunchPlatform.kt:14–18`); that file is shared with the perf variants.
- **9g's journey** is its own debug object, `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/WizardSaveJourney.kt`: `apply(base: MemberWizardDraft, locale: Locale, now: Instant)`. It starts from `WizardBoardCaptureSeed.draft("WizardReturn", locale)` and sets the leg times in `ZoneOffset.UTC`, not the system zone. `WizardDebugSeeds` wraps it as `object : TripWizardDependencies by session { uiOverride = null; start() = WizardSaveJourney.apply(session.start(), locale, session.now()) }`, with presentation `WizardInitialPresentation()` (`WizardDebugSeeds.kt:50–63`, `WizardBoardCaptureSeed.kt:92–103`). Its test is `feature/wizard/src/testDebug/.../WizardSaveJourneySeedTest.kt`, not "`WizardCompletionBoardSeedTest`".
- **Perf sources:** `PerfMockSources(io)` is chosen through `platformMockSources` (`app/src/perf/.../MockSourcesPlatform.kt`, with a `null` stub in `src/releaseNoPerf`). `MockAppSeed.make(now, unverified, walkthroughSeen, termsPending, includeTimelinePerformanceTrips = false, zone)` (`MockAppSeed.kt:35–42`). `MockSeedFlags` lives in `app/src/main/.../GraphOverrides.kt:7`. The perf mock delays each call by a random 0.3–0.8 s unless `mockDelay` is set.
- `app/src/perf` is also compiled into **debug** (`app/build.gradle.kts` `onVariants(selector().withBuildType("debug"))`), so `src/perf` classes exist in debug and `app/src/testDebug` sees them.

**Instruction (OQ-A596):** follow the existing releaseNoPerf stub pattern.
- Add one object, `net.chronoflow.app.perf.PerfJourneyHook`: real in `app/src/perf`, a no-op stub in `app/src/releaseNoPerf`, allowed by R7's stub rule.
  - `MainActivity` calls `PerfJourneyHook.onIntent(intent)` in `onCreate` (before `setContent`) and in `onNewIntent`. These two lines are the only `src/main` change.
  - The release `LaunchPlatform.prepareWizard` returns `PerfJourneyHook.prepareWizard(graph, epoch, locale)`. The stub returns `false`.
  - In perf, `prepareWizard` installs a `WizardStartOverride` built like `WizardDebugSeeds`' WizardSaveJourney branch, with `PerfWizardJourney.apply(base, locale, now)`. It returns `true` once per process when `cf.perf-wizard=WizardSaveJourney`.
- `PerfWizardJourney.apply` mirrors 9g's `WizardSaveJourney.apply` signature and UTC leg times exactly. Copy the `WizardReturn` base-draft lines it needs into `app/src/perf`. `PerfWizardJourneyTest` asserts `PerfWizardJourney.apply(b, l, n) == WizardSaveJourney.apply(b, l, n)` for one fixed base draft, locale and instant. Exact equality is possible because both take `now`.
- This counts as within OQ-13-5's "one nullable hook member" budget. Anything beyond it (another `src/main` change) means stop and take the OQ-13-5/-6 fallback.
- In debug, `src/perf` is present, so the debug `MainActivity` also runs `PerfJourneyHook`. Debug seeds stay authoritative: the debug `LaunchPlatform.prepareWizard` keeps using `WizardDebugSeeds` unchanged.

**R11. The 12c first-trip offer cannot appear in these journeys.** Body: `reminders.firstTrip` shows "on a fresh install". It adds `dismissFirstTripOffer()` with a 1 s wait after every `trip.timeline.ready`. HEAD: the offer is queued only when the wizard saves the account's **first** trip (`MemberWizardStore.kt:140` → `FirstTripRemindersOffer.queue(firstTrip)`, `FirstTripRemindersOffer.kt:17–24`). It is consumed once (`wizardReminders` `"pending"` → `"seen"`) and shown only with undetermined permission and reminders enabled. The demo account has 7 trips, so `firstTrip` is false for every perf journey and for the smoke's wizard save. **Instruction (OQ-A594):**
- Drop the 1 s wait. Keep `dismissFirstTripOffer()` as a non-waiting guard: `if (hasObject(By.res("reminders.firstTrip"))) { pressBack(); check(wait(Until.gone(...), 3_000)) }`.
- Call it only after `wizardSavesToTripPage` reaches `trip.page`, and once in `tripJourney`.
- `reminders.firstTrip` exists at `feature/reminders/.../FirstTripRemindersSheet.kt:73`.

**R12. `:benchmark` module (P6) and profile generation (P8, Task 10).** HEAD files: `BaselineProfileGenerator`, `LaunchJankBenchmark` (`launchCold`), `ProcessDeathLaunchTest`, `StartupBenchmark` (`startupCold`, `startupColdSignedIn`, `startupWarm`; `signedInStartupIntent` at l.90–97), `TimelineBenchmark` (`timelineFirstFrame`, `timelineScroll`, `timelineScrollWide`, `listScroll`, `timelineLastDay`), `TimelineJourneys.kt`, `TimelineTraceMetric.kt` and `WelcomeMomentMetric.kt`. `BaselineProfileGenerator`:
- Its methods are `startup` (l.60), `timelineWide` (6e, l.91) and `timelineLarge60` (l.139).
- All three run through a private `collectProfile(startup, block)` (l.28–55), which wraps `rule.collect` in `BoundedProfileInput`.
- Its log label is hard-coded `if (startup) "startup" else "timelineLarge60"` (l.42).
- Budgets come from `ProfileCollectionBudget()`: `maxIterations = 3`, `stableIterations = 2`, a journey timeout of 120 s (`core/testing/.../ProfileCollectionBudget.kt:3–8`).
- `timelineLarge60` already covers the summary, the day menu (day 0), a step sheet with `step.done`/`step.undo`, three bounded swipes, the list toggle and the return to the timeline. It asserts that the gestures do **not** reach `trip.plan.end` (l.234–238).
- Gestures use `UiDevice.swipeTimeline(bounds)` ("bounded-swipe-v3", `TimelineJourneys.kt:129`), not flings. The day-59 jump exists in `TimelineBenchmark.timelineLastDay` (l.199–240; `MENU_SCROLLS = 12`, `END_FLINGS = 6`).
- The step sheet window tag is `trip.sheet`; its content is `step.sheet` (`core/ui/.../StepSheetContent.kt`).

`Scripts/baseline-profile.sh` runs `:app:generateBaselineProfile` with `timeout_msec=900000`, and the Makefile sets `TIER_IDLE_TIMEOUT := 1200`. After generation the script asserts the trip, wide and list classes are present. It already has `wm size reset` cleanup.

**Instruction:**
- Every new method goes through `collectProfile` with a `name` parameter (replace the hard-coded label). Each step is wrapped in `BoundedProfileInput.step(...)` and starts with `cancelStaleTouch()`.
- Replace the body's `flingPage(n, dir)` with `swipeTimeline(bounds)` throughout Tasks 9–10. Do not add a fling helper.
- Append the day-59 jump to the **end** of `timelineLarge60`, after the existing return-to-timeline step, so the "must not reach plan end" check stays where it is. Reuse `timelineLastDay`'s menu-scroll loop: extract it into `TimelineJourneys.kt` and have both call it.
- Keep `timeout_msec=900000` (body patched).
- Extend the script's post-generation Python check with the Task 10 classes, as well as `BaselineProfileFilesTest`.
- `timelineWide` stays as it is.

**R13. Checker and perf scripts (P7, P8).** HEAD:
- **Checker CLI:** `check-benchmarks.py --suite {launch,startup,timeline} --device-class {emulator,reference,lowEnd,unknown} --results-dir … [--budgets] [--history] [--theme] [--font-scale] [--refresh-hz] [--gpu] [--commit] [--append] [--row-file]`.
- **`budgets.json`** (`"schema": 1`) has the sections `startup`, `timeline`, `timelinePage`, `timelineReportOnly`, `launch`, `timelineWidePage`, `welcomeEmulatorRefreshHz` and `labels`. `startup` has no `sessionFirstMs` row, so the "Session known, signed out" budget row is never printed. 4e recorded it by hand (`docs/perf-startup.md:238–250`).
- **Dry-run variables** are per script: `PERF_STARTUP_DRY_RUN`, `PERF_TIMELINE_DRY_RUN` and `BASELINE_PROFILE_DRY_RUN`, not `PERF_DRY_RUN`.
- **Shared helpers:** all three scripts source `Scripts/perf-device.sh` (`resolve_perf_device`: exactly one device, sets `SERIAL`/`ANDROID_SERIAL`). `perf-startup.sh` and `perf-timeline.sh` also source `Scripts/perf-archive.sh`, which moves the results into `build/perf/<suite>/<stamp>…`; the checker reads `$RESULTS_DIR` from there.
- **`perf-startup.sh`** runs `StartupBenchmark`, `LaunchJankBenchmark` and `ProcessDeathLaunchTest`, then the checker twice (`startup`, then `launch`).
- **`perf-timeline.sh`** refuses the nl locale, runs under `Scripts/test_runner_progress.py` and re-runs once on a band miss (checker exit 4).
- **Dry-run tests:** the `baseline-profile.sh` ones live in `Scripts/tests/test_perf_startup.py`; there is no `test_baseline_profile.py`.
- **Makefile:** `Scripts/tests/test_makefile.py::test_device_recipes_take_the_lock` fails any Makefile recipe that touches a device without `$(RUN_TIER) … --device` or `device_lock.py`.

**Instruction (OQ-A598):**
- `perf-compare-rn.sh` and `release-smoke.sh` source `perf-device.sh` (and `perf-archive.sh` for the compare run). They use their own `PERF_COMPARE_RN_DRY_RUN` / `RELEASE_SMOKE_DRY_RUN` variables in the existing style.
- `make perf-rn` and `make release-smoke` go through `$(RUN_TIER) <tier> --device …`. Add both to `.PHONY`.
- The `pm clear` line (Task 8) goes after `prep-perf` in all three existing scripts. Assert it in `test_perf_startup.py` and `test_perf_timeline.py`.
- Add the `startup` budget row `{"test":"startupCold","metric":"sessionFirstMs","stat":"median","unit":"ms","reference":{"hard":50},"lowEnd":{"hard":100},"emulator":{"report":true}}`. This copies plan.md's row values without changing any of them.
- Add a checker fixture case for `sessionFirstMs`.

**R14. Budget rows missing from §5.7.** HEAD's "Performance budget" also has these rows: "Today view, JVM" (`TodayViewBudgetTest`, `core/plan/src/test/...`, 7), "Timeline builds on the main thread" (gate 0), "Main-thread frame time P50/P99", "RenderThread frame time P99", "`TimelineTargets` composition per day" and "First-frame breakdown" (all report, `TimelineTraceMetric`). `budgets.json` and the checker already emit all of them except the JVM row. **Instruction (OQ-A597):** add them to Task 12's row-to-verdict tables. Take each value from the checker output or the JVM test result. "Today view, JVM" is gated by `make quick`/`make timing`. Nothing new is measured.

**R15. RN comparison and the read-only RN repo.** plan.md says "Measure the RN release APK with the same `StartupBenchmark` harness (`12 §1`)". The body (Task 11) installs it with `adb install` from `/Users/rickgout/Sites/chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk`. HEAD and host state, checked read-only on 2026-11-22:
- The prebuilt APK exists there: 125,310,093 B, dated 5 Nov 12:33, SHA-256 `4ee240d3754a7bba12761ddcb4b8317b15d7131f3c55f93f314d64e89df3b1f5`. That equals §3.3, so it is the same build research 12 §1 used.
- It sits in the RN repo's ignored build output. **No copy exists outside the read-only repos.**
- **No RN number has been taken with any Macrobenchmark harness.** Neither `docs/perf-startup.md` nor `docs/perf-timeline.md` mentions RN.
- The only RN startup numbers are research 12 §5's `am start -W` TotalTime on the Pixel 8 AVD: 755 ms first, then 341/394/416 ms. That is a different harness and can be cited only as context.

**Instruction (OQ-A591):**
- Never build, prebuild, install dependencies, run Gradle/npm/expo or run git commands that change anything in `/Users/rickgout/Sites/chronoflow-mobile`. Reading the existing APK file (`shasum`, `aapt2 dump`, `adb install -r <path>`) is reading.
- `perf-compare-rn.sh` refuses unless the file exists and its SHA-256 equals the value above (or a user-given `RN_APK`/`RN_APK_SHA` pair). Body patched.
- If the APK is missing or its sha differs, do not build it. Record every RN row as "open: needs a prebuilt RN release APK from the user (OQ-A591)", keep Task 11's code and fixture tests, and still tick "Performance budget" for the native rows. The hand-back names the gap.
- The reviewer checks `git -C /Users/rickgout/Sites/chronoflow-mobile status --short` before and after; it must be unchanged (§9).
- OQ-13-16's `LaunchComparisonBenchmark` stands.

**R16. Smaller facts.**
- **P9:** `baseline-prof.txt` has 36,317 lines and `startup-prof.txt` 28,988. `BaselineProfileFilesTest` (`app/src/test/kotlin/net/chronoflow/app/`) has three methods.
- **Task 10 class names:**
  - `feature/today/.../ui/TodayScreen.kt`
  - `feature/trips/.../TripsScreen.kt`
  - `feature/trip/.../page/TripPageScreen.kt`
  - `feature/trip/.../list/DayListRow.kt`
  - `core/ui/.../plan/StepSheetContent.kt`: the step sheet is in `:core:ui`, not `:feature:trip`.
  - `feature/wizard/.../ready/WizardReadyStep.kt`: there is no `WizardReadyScreen`.
  - `feature/launch/.../welcome/WelcomeScreen.kt`
  - `feature/auth/.../model/SignInModel.kt` and `.../signin/SignInViewModel.kt`
- **P12 tags:** every tag exists in `src/main`. `wizard.fine-tune.continue` and `wizard.ready.continue` are built as `"wizard.$step.continue"`, so a literal grep misses them. `WizardJourneyTest.oneWayReviewSavesAndOpensTheTripPage` uses both. Trip cards are `trip.<id>` (`core/ui/.../trip/TripCard.kt:79`). Today's future-trip card uses the same pattern (`TodayFutureTripCard.kt:57`), so on Today click `today.trip`, never `trip.<id>`.
- **Demo seed:** trip ids `…0001`–`…0007` come from `core/testing/src/main/resources/fixtures/mock-seed.json`, with credits 3.
- **Task 13:** take the `assetlinks.json` JSON from `docs/app-links.md:30–44`. The upload-key rule is OQ-A208 (formerly 5c OQ-5c-4). The web delete route is confirmed at `W/routes/api.php:83–84` (pin `e36368ec`).
- **`make check`** now also runs a `reminders-permission` device tier (`Scripts/reminders_permission.py`, `Makefile:52`). Task 12's R9 timings include it.
- **Toolchain:** AGP 9.5.1, benchmark 1.5.0, uiautomator 2.4.1, bundletool 1.18.3 and WorkManager 2.12.0 (`gradle/libs.versions.toml`).

**R17. Who runs what.** The sandboxed worker writes code and runs the JVM, Python and build-logic tests (`make targeted`, `Scripts/test_scripts.sh`, `:build-logic:convention:test`). If the sandbox can, it also runs `make quick`. **The orchestrator runs every device or emulator tier, plus every release Gradle build the sandbox cannot**, and feeds back the exact output. Device tiers take the host-wide device lock and run one at a time:
- `make baseline-profile` (Task 10)
- `make perf` and `make perf-timeline` (Task 10 proof; Task 12 R1–R4, with `THEME=dark` and `FONT_SCALE=1.3`)
- `make perf-rn` (R5)
- `make release-smoke` (Task 9, R8)
- `Scripts/measure-tiers.sh` (R9)
- `make check` (Task 14 / acceptance 10)
- any physical-device run (R10)
- the `enableSplit = true` proof run of `make size` (Task 5)
- `make release-check` and the signed and unsigned `:app:bundleRelease` proofs (Task 1), if Gradle cannot run in the sandbox

Hand each of these over as an exact command. Never fabricate numbers.

### New open questions (Task 14 appends them before OQ-13-1…18)

| # | Question | Default |
|---|---|---|
| OQ-A591 | (13) The RN comparison needs the RN release APK, and the RN repo is read-only. | Install only the existing prebuilt `chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk`, verified by SHA-256 `4ee240d3…b3b1f5`. Never build in the RN repo. If it is missing or differs, every RN row reads "open: needs a prebuilt RN release APK from the user". |
| OQ-A592 | (13) `SystemJobService` is now unexported (OQ-A587). Should the verifier still exempt `BIND_JOB_SERVICE`? | No. `GUARD_PERMISSIONS` = `DUMP` only. A planted exported `SystemJobService` must fail `EXPORTED_COMPONENT`. |
| OQ-A593 | (13) Release ships four no-op `net.chronoflow.app.perf.*` stubs from `src/releaseNoPerf`. | The APK check allows exactly the classes declared in that directory and forbids every other `net.chronoflow.app.perf.*` class. |
| OQ-A594 | (13) The 12c first-trip offer is queued only on an account's first saved trip, so it never shows for the 7-trip demo account. | No waiting dismissal in profile iterations. A non-waiting guard runs after the smoke's wizard save and once in `tripJourney`. |
| OQ-A595 | (13) `nonMinifiedRelease/keepRules/profile.keep` holds `-dontobfuscate`/`-dontoptimize` by design. | `KeepRulesTest` excludes it from the escape rules and asserts it holds exactly those two directives and is the only keep file in that source set. |
| OQ-A596 | (13) Perf journey extras need hooks in `MainActivity` and in the wizard start. | One `net.chronoflow.app.perf.PerfJourneyHook` object (perf real, releaseNoPerf no-op stub): two `MainActivity` call lines, plus the release `LaunchPlatform.prepareWizard` delegating to it. Anything more takes the OQ-13-5/-6 fallback. |
| OQ-A597 | (13) Six "Performance budget" rows are missing from the brief's §5.7. | Record them in Task 12's verdict tables from the existing checker and JVM outputs. Nothing new is measured. |
| OQ-A598 | (13) `sessionFirstMs` has no `budgets.json` row, so "Session known, signed out" is never checked. | Add a `startup` row with plan.md's values (reference 50 ms, low-end 100 ms, emulator report). No plan.md value changes. |

### Task 3 review finding (2026-11-22)

The first real R8 configuration comparison found extra consumer rules from `kotlinx-coroutines-test-jvm` in the perf variants, transitively through `:core:testing`. Perf imports only `LargeTimelineFixtures`; the coroutine test dispatcher is unused. Task 3 excludes `kotlinx-coroutines-test` and its JVM artifact on the two perf runtime classpaths (both the direct fixture edge and the transitive edge through `:core:data-mock` reach the test helpers). Debug and test dependencies stay unchanged. The final R8 comparison and builds are recorded in `docs/release.md`; no app keep rule is broadened.
