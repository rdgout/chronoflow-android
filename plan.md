# ChronoFlow Android plan

## Context

ChronoFlow plans sleep, light and caffeine for each trip. This plan builds the native Android app in `/Users/rickgout/Sites/chronoflow-android`. The app must behave exactly like the native iOS app, follow the boards, and be idiomatic Android: Kotlin, Jetpack Compose, Material 3 system components tinted by the generated tokens, and designed surfaces drawn exactly as on the boards. The trip timeline is the performance-critical screen. Phases 1a–1e (milestone M1) prove its architecture with synthetic 30- and 60-day plans and Macrobenchmarks before any UI polish.

Tags: **[F]** is a fact verified in source (path and line given). **[R]** is an Android recommendation adopted by this plan. Research files are cited as `NN §x`; they live in `docs/research/NN-*.md` and were written on 2026-11-15.

**Sources (read-only, never write, build, prebuild or git-modify them):**
- iOS app `/Users/rickgout/Sites/chronoflow-ios` at `f391a1fa`: the behavior reference. Its `plan.md` holds the user decisions, specs and open-question answers; its catalogs, generators and fixtures are reused (next section).
- Web and API `/Users/rickgout/Sites/chronoflow-new`: product source of truth. Contracts are pinned at `origin/main` `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19`. Passkey management lives on `feat/mobile-api-passkey-management` at `1f4a2684`.
- Expo / React Native app `/Users/rickgout/Sites/chronoflow-mobile` at `e24872a`: plan fixtures S1–S6, mock data and lessons. Its Android build was never verified on a phone (`09 §1`).
- Boards in `_design/canvas/`: 338 `*.dc.html`, `canvas.json`, `ds/chronoflow/tokens.json`, `theme.css`, `icon/android/` (adaptive icon layers) and `icon/colors.json`. `_design/icon/` holds the icon sources.
- Research `docs/research/01–14` and reference screenshots `docs/research/screens/{ios,rn}/`.

**This plan covers** everything iOS ships at `f391a1fa`:
- the system splash, animated launch overlay and readiness hand-off, and Welcome;
- sign-in, two-factor, passkey sign-in, password reset, sign-up and email verification, with App Links;
- the account store, mock and API data sources, session lifecycle and offline sync;
- the tab shell, My trips, the trip page (timeline, list, wide, step sheet, check-offs, legend, trip actions) and Today;
- walkthrough and terms;
- the member trip wizard (create, edit, duplicate) and the guest wizard with sign-up claim;
- Settings: home, account, profile, language, preferences, body clock, security, password prompt, two-factor, passkeys, delete account, plan and credits;
- local reminders;
- English and Dutch, light and dark, every phone and foldable, font scale up to the 1.3 cap, TalkBack, keyboard and Switch Access.

**It deliberately does not cover** (iOS parity, see A24): calendar export, Live Activity or Live Updates, practice mode, trip complete/reopen/delete, data download, email change, billing, paywall and cancel, health questions (no-health-data), RN user migration, widgets, push, analytics or crash SDKs, in-app review or updates, Wear OS, TV, Auto and ChromeOS.

**Status:** no code yet. `chronoflow-android` is not a git repository; Phase 0a creates it.

### What we reuse from iOS (answers "Does the iOS research/plan contain things you can already re-use?")

Yes, most of it. About two thirds of the iOS plan is platform-neutral, about a fifth needs adapting, and the rest is iOS-only (`01`, summary). Reuse falls into the six kinds in the table below. Every phase under "Tasks" lists its iOS references (exact `plan.md` line and heading, or a repository path), and its brief must cite them.

**Rules for reuse.**
- iOS is read-only. Copied artifacts carry the iOS pin `f391a1fa` in `tools/ios-pin.txt` and a SHA-256 check in `make generators-check`.
- Generators read iOS files through `git -C <ios> show <pin>:<path>`, so a moving iOS checkout never changes Android output.
- Never edit a copied artifact by hand. Regenerate it, or add an Android overlay file that names what it replaces.

| # | What | iOS source (`chronoflow-ios`, read-only) | Android use | Phase |
|---|---|---|---|---|
| **Decisions, rules and specs** |  |  |  |  |
| R1 | User decisions D1–D27 and the dated user decisions | `plan.md:25–107` (Decisions, navigation titles, test tiers, conflict rules) | Inherited, translated in A25 | all |
| R2 | Platform-neutral spec sections | `plan.md` "## The spec" (l.137) and "## Auth and offline-store spec" (l.362) | Cited by exact heading under "The spec"; only Android deltas are written here | 1–12 |
| R3 | Open-question answers OQ 1–46 | `plan.md:2376–2476` | Inherited where A25 says so | all |
| R4 | Working rules, tests rules (2026-11-10), cleanup rules (2026-11-10/11) | `AGENTS.md`, `CLAUDE.md` | Translated into this repo's `AGENTS.md` and `CLAUDE.md` | 0a |
| **Generated or copied artifacts** |  |  |  |  |
| R5 | 13 String Catalogs: 1,281 keys, 31 plural keys, en + nl (`10 §1`) | `App/Localizable.xcstrings`, `App/InfoPlist.xcstrings`, `Packages/ChronoFlowKit/Sources/*/Resources/*.xcstrings` | `tools/l10n/xcstrings2android.py` → `res/values{,-nl}/strings_ios.xml` | 0c |
| R6 | Plan copy: 289 entries, 141,652 B, generated at web `afd80dc0` | `Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json` (generator `Tools/PlanCopyGen`) | Copied verbatim to `core/plan/src/main/resources/plan_copy.json`, SHA-256 checked | 0c |
| R7 | TokenGen: OKLCH → sRGB math, sky ramp, golden report | `Tools/TokenGen/Sources/TokenGenCore/ColorMath.swift`, `Tools/TokenGen/TokenGenReport.md`, `Tools/TokenGen/{Plan,PlanUI,Settings}.source.json` | Kotlin port `:tools:tokengen` reproduces all 84 hex values; the iOS report is its golden test (`08 §3`) | 0b |
| R8 | Fonts: Figtree 400–700, Fraunces 400/500 at opsz 20/32/48 (google/fonts `24ddc1f324cac5d9f5aedca126d298f56e3f1640`) | `Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts`, `Tools/generate_fonts.py` | Same TTF bytes in `core/designsystem/src/main/res/font/`, hash test | 0b |
| R9 | Launch moon and wordmark geometry | `Assets/moon/launch-moon-{light,dark}.svg`, `App/Assets.xcassets/LaunchMoon.imageset`, `Tools/AssetGen` | `drawable-xxxhdpi/launch_moon.png` and `drawable-night-xxxhdpi/launch_moon.png`, wordmark as a vector path | 0b, 3a |
| R10 | Contract generator design (roots, overrides, overlays) | `Tools/contracts-gen/generate.py`, `Tools/contracts-check` | `tools/contracts-gen/generate_kotlin.py` with Kotlin output (`03 §16`) | 0d |
| **Fixtures and goldens** |  |  |  |  |
| R11 | Plan fixtures S1–S6, account identity/snapshot, wizard wire fixtures | `Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/` | `core/testing/src/main/resources/fixtures/` with a checksum manifest | 0d |
| R12 | 44 date/number goldens | `Packages/ChronoFlowKit/Tests/PlanCoreTests/Fixtures/intl-cases.json` | `:core:plan` tests | 1a |
| R13 | 110 reminder vectors (1.0 MB) and their exporter | `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json`, `Tools/ReminderParity/export-selection.cjs` | `:core:reminders` parity test | 12a |
| R14 | Device profiles | `Packages/ChronoFlowKit/Tests/SnapshotSupport/profiles.json` | `core/testing/src/main/resources/profiles.json` with Android dp, density and insets (`13 §4.4`) | 0e |
| R15 | large30/large60 recipe: ids, hashes, labels | `plan.md` 15d (l.1705), `Docs/perf-timeline.md` | `:core:testing` generator with byte-identical output (`04 §12`) | 1b |
| **Logic ported 1:1 (Swift → Kotlin, same names, same cases)** |  |  |  |  |
| R16 | PlanCore: `Timeline`, `TimelineGeometry`, `TimelineLabels`, `TimelineAccessibilityGeometry`, `CheckState`, `PlanLogic`, `CopyCatalog`, `CopyLineFormatter`, `Today`, `PlanClock` | `Packages/ChronoFlowKit/Sources/PlanCore/*.swift` | `:core:plan` | 1a, 1b, 7 |
| R17 | Trip page state: `PreparedPlan`, `TripPageState` (`resolve`, `openingDay`, `replacementAnchor`), `TripPageLayout`, wide list position | `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/{PreparedPlan,TripPageState,TripPageLayout,TripScrollTracker}.swift` | `:core:plan` (state, layout math), `:feature:trip` (UI) | 1b, 6b, 6e |
| R18 | Launch: `CubicBezier`, `LaunchLayout`, `FlightPath` (128 samples), `LaunchTimeline(+Ready,+Welcome)`, `StatusLineLayout`, `LaunchReadiness`, `LaunchCoordinator`, `LaunchClock` | `Packages/ChronoFlowKit/Sources/Launch/`, `Packages/ChronoFlowKit/Sources/AppCore/` | `:feature:launch` (pure parts are JVM-tested) | 3a, 3b |
| R19 | Data layer: schema v1, transactions with epoch and permit, `SyncEngine`, outbox rules (`OutboxChange`), `SessionController` and its teardown order, `SessionGateRouter`, `DataSource` (25 methods), `MockDataSource`/`MockServerState`/`MockAppSeed` | `Sources/LocalStore`, `Sources/Sync`, `Sources/AppCore/{SessionController,SessionGateRouter,SessionMutationPermit,OutboxChange,DataSource,StorageNamespace}.swift`, `Sources/DataAccess/Mock/` | `:core:database`, `:core:sync`, `:core:network`, `:core:data-mock`, `:core:data` | 4a–4e |
| R20 | Auth: `AuthLink` parsing, form rules, lockout wait | `Sources/AppCore/{AuthLink,LockoutWait}.swift`, `Sources/Auth/` | `:feature:auth` | 5a–5d |
| R21 | Wizard model: `WizardForm` … `MemberWizardStore`, `AirportSearchService`, `Generation/*`, guest claim and continuation | `Sources/TripWizard/{Model,Generation}/`, `Sources/AppCore/{GuestClaim,GuestSecretStore,GuestSessionContinuation}.swift` | `:core:wizard` | 9a, 9b, 11a, 11b |
| R22 | Settings sync line and home state | `Sources/Settings/SettingsHomeState.swift:113-140` | `:feature:settings` | 10a |
| R23 | Reminder engine, schedule, ledger, tap routing, first-trip offer | `Sources/Reminders/{ReminderSelection,ReminderSchedule,ReminderLedger,ReminderTap,FirstTripRemindersOffer}.swift` | `:core:reminders`, `:core:data` | 12a–12c |
| **Test suites as specifications** |  |  |  |  |
| R24 | Case lists of the iOS suites: sync (35+), wizard (172 tests in 28 suites), copy (caffeine tables), timeline invariants, readiness, session teardown | `Packages/ChronoFlowKit/Tests/*Tests/` | Same cases as JVM tests, named after the Swift test they port | per phase |
| **Process lessons and docs** |  |  |  |  |
| R25 | Harness lessons (689 s swipe test, 150 GB of worktrees, 203 GB of simulators, harness artifacts) | `01 §8`, `13 §1`, iOS `AGENTS.md` "Tests" | AGENTS.md tests rules, CLAUDE.md cleanup rules | 0a |
| R26 | Fidelity and decision docs | `Docs/{no-health-data,phase-19-reminders,settings-board-fidelity,wizard-fidelity,perf-timeline,auth-gate,app-icon,ui-test-tiers}.md` | Read before the matching phase; cited in its brief | per phase |
| R27 | Seeds and identifiers: `CF_*` launch keys, `-cf.*` arguments, dotted test ids | `App/LaunchArguments.swift`, `App/Debug/`, `12 §2` | Identical names as intent extras and test tags (A19) | 0e, 4e |

**Not reused:** SwiftUI and UIKit views, the Core Animation layer tree (only its geometry and timing), GRDB, Keychain and XcodeGen specifics, Liquid Glass, SF Symbols, the simulator scripts, Dynamic Type's xxLarge cap (font scale 1.3 instead), and iOS-only copy such as Face ID or the two-factor intro b1. Android copy variants wait for approval (OQ-A12).

**iOS and RN defects we must not port.**
- **Login upsert.** Keep pending local edits on login upsert. iOS at the pin already does this: `SessionAccountDatabase.upsert` (`SessionAccountDatabase.swift:29–48`) keeps name and email while a profile row is pending and ORs `walkthroughSeen` with a pending walkthrough row, and `StoreApplication.swift:51–60` keeps a pending policies version and skips settings sections that have pending rows. Its "Deferred fixes" checkbox (iOS `plan.md:2327`) is stale. Phase 4d ports the upsert as it is. The defect not to port is RN's (`src/db/store-account.ts:10–23,46–64`).
- **RN readiness bug.** The iOS spec fixes it ("### Readiness rules (RN Phase 21, with the RN bug fixed)"); port the fixed rules.
- **Missing policies.** They stay loading, never pass the gate (iOS behavior; RN `gate.ts:78–79`).
- **RN Android mistakes** (`09 §3`): a giant `configChanges` list, hard-coded 80 dp insets, cleartext traffic in `main`, and unused permissions.

## Decisions

| # | Decision | Reason |
|---|---|---|
| A1 | **Toolchain and SDK levels [R].** Kotlin 2.4.21 (K2) and AGP 9.5.1 with built-in Kotlin: never apply `org.jetbrains.kotlin.android`, and use no kapt or KSP. Gradle wrapper 9.9.1 `bin`, SHA-256 `d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`; fallback 9.6.0 `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`. Build on JDK 17 (Zulu 17.0.21 is installed). Robolectric test tasks use a JDK 21 toolchain through `org.gradle.toolchains.foojay-resolver-convention` 1.0.0. **compileSdk 37, targetSdk 36** (Play requires it since 2026-08-31; always set it explicitly, because AGP 9 otherwise defaults targetSdk to compileSdk). **minSdk 29.** Compose BOM 2026.09.00 (ui 1.12.1, material3 1.4.0, adaptive 1.3.0, navigation-suite 1.4.0). AndroidX: activity-compose 1.13.0, core-ktx 1.19.1, core-splashscreen 1.2.0, lifecycle 2.11.0, navigation3 1.2.1, adaptive-navigation3 1.3.0, window 1.5.1, DataStore 1.3.0, WorkManager 2.12.0, tracing 2.0.5, profileinstaller 1.4.1, metrics-performance 1.0.0 (JankStats, debug and benchmark only), credentials and credentials-play-services-auth 1.6.0. Others: SQLDelight 2.4.1, kotlinx-serialization-json 1.13.0, kotlinx-coroutines 1.11.0, OkHttp 5.5.1 and mockwebserver3 5.5.1. Tests and tooling: benchmark and `androidx.baselineprofile` 1.5.0, uiautomator 2.4.1, androidx.test.ext:junit 1.3.0, Robolectric 4.18, Roborazzi 1.79.0, Turbine 1.2.1, assertk 0.28.1, MockK 1.14.12 (edges only), LeakCanary 2.14 (debug), Spotless 8.11.0 with ktlint 1.8.0 and Compose rules 0.6.8, Konsist 0.18.1. No alpha, beta or RC artifacts. Verify KGP with `./gradlew buildEnvironment \| grep kotlin-gradle-plugin`. | Versions are from `11 §1` (checked 2026-11-15). minSdk 29 reaches 93.9% and brings system dark theme, gesture insets, TLS 1.3, Mainline tzdata and Credential Manager passkeys. `01` said 28 and `09`/`10` said 26; 29 removes a font and inset fallback tier (OQ-A2). Built-in Kotlin drops the RN opt-outs `android.builtInKotlin=false` and `android.newDsl=false`; never set them. |
| A2 | **App identity [R].** `applicationId = "net.chronoflow.app.native"` for every variant until the first Play upload (C6, OQ-A1). Debug adds `applicationIdSuffix = ".debug"`. Code namespaces are `net.chronoflow.app` (`:app`), `net.chronoflow.core.<module>` and `net.chronoflow.feature.<module>`. **`native` is a Java keyword:** it is legal in an applicationId but must never appear in a `namespace` or a Kotlin package. `versionName` starts at `0.1.0`; `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build` from `version.properties` (`14 §11`). No custom URL scheme (D15, OQ 15); links are App Links (A16). The app name "ChronoFlow" is in `strings_untranslatable.xml`. | Mirrors D15: the app installs beside the RN build `net.chronoflow.app`, so the two can be compared. Phase 0a proves that a build with this id installs and launches. |
| A3 | **Module graph [R].** `build-logic/convention` holds the plugins listed in "Android spec: build". Modules: <br>• **`:app`**: Application, MainActivity, `AppGraph`/`SessionGraph` wiring, NavDisplay, `ConfigurationPolicy`, debug seeds in `src/debug`, perf entry in `src/perf`. <br>• **JVM modules:** `:core:model` (generated contracts, hand DTOs, domain types, `Language`, clock), `:core:plan` (PlanCore port: copy, Today logic, check state, timeline layout and the `DayDrawing` builder), `:core:sync` (SyncEngine, outbox rules, session gate reducer), `:core:reminders` (engine), `:core:wizard` (wizard model and generation), `:core:network` (OkHttp `ApiClient`), `:core:data-mock`, `:core:testing` (fixtures, large30/60 generator, fake clock, fake `TextMetrics`, `profiles.json`). <br>• **Android libraries:** `:core:database` (SQLDelight), `:core:datastore` (DataStore, Keystore session store, device identity), `:core:data` (repositories, session controller wiring, Android reminder scheduler), `:core:designsystem` (tokens, theme, fonts, icons, primitives), `:core:navigation` (routes, `Navigator`), `:core:ui` (shared screen pieces). <br>• **Features:** `:feature:launch` (splash, overlay, Welcome), `:feature:auth`, `:feature:onboarding` (walkthrough, terms), `:feature:today`, `:feature:trips`, `:feature:trip`, `:feature:wizard`, `:feature:settings`, `:feature:reminders`. <br>• **Test and tools:** `:benchmark` (`com.android.test`), `:konsist`, `:tools:tokengen`. <br>Rules, enforced by Konsist in `:konsist`: features never depend on features; JVM core modules import no `android.*` or Compose; only `:app` knows every feature; no `GlobalScope`; no `runBlocking` outside tests; no `Thread.sleep` in tests; ViewModels are named `*ViewModel`; routes are `@Serializable`. | `:core:plan` mirrors iOS `PlanCore`, so ported files keep their home. It covers more than the timeline. `04 OQ-T23` and `11` proposed `:core:timeline`; OQ-A26 keeps that open, default `:core:plan`. Pure JVM modules make the expensive logic testable without Robolectric (`04 §9.2`). |
| A4 | **Manual DI [R]** (C2). `AppGraph` is process-wide and fully lazy: it is constructed in `Application.onCreate` without touching disk. `SessionGraph` is built per session epoch: DB, repositories, SyncEngine, reminder scheduler. ViewModels are created through `viewModelFactory { initializer { … } }`, reading the graph from `CreationExtras`. `AppDispatchers` is injected everywhere. | D26 forbids eager singletons, and manual DI makes them visible. Without kapt or KSP the build is faster. It mirrors iOS `AppServices`/`AuthSessionServices`. Hilt was rejected in `14 §2` C2. |
| A5 | **Persistence [R]** (C1, D18, D19, D21, D22). <br>• **Database:** SQLDelight 2.4.1 with `AndroidSqliteDriver` (framework SQLite, no native library). One DB per account at `noBackupFilesDir/<ns>/Accounts/<uuid>/account-<uuid>.db`; only a validated UUID becomes a path component. WAL and `foreign_keys=ON`. <br>• **Schema v1** is iOS "### Account schema v1 and observation" verbatim in `.sq` files, plus indexes `outbox(created_at,id)` and `trips(deleted_at)`, plus the Android-only local tables `scheduled_reminder` and `reminder_ledger` (never synced). Never replay RN migrations 0000–0014. `plan_versions.detail` holds the raw server bytes. <br>• **Writes and reads:** writes go through one `Dispatchers.IO.limitedParallelism(1)` writer as `epoch.whileValid { permit?.whileValid { transaction { body() } } }`. Reads go through `AccountDatabase.observe` (SQLDelight table listeners, conflated, fetched on io; completes on close). <br>• **Session `{token, accountId}`:** AES-256-GCM with an AndroidKeyStore key, alias `<ns>.session`, no user-authentication requirement. The ciphertext goes to `noBackupFilesDir/<ns>/session.bin` through `AtomicFile`. Accept order is DB upsert → secure write → publish. `KeyPermanentlyInvalidatedException` or a failed decrypt deletes the session and signs out; an `IOException` retries. <br>• **Device preferences** (DataStore, outside account storage): `api.deviceId` (UUIDv7, D22), `cf.settings.language`, `cf.settings.theme`, `notificationPermissionRequested`. <br>• **Other files:** wipe marker `pending-wipe.json`, guest draft `guest-wizard.json`, mock server `ChronoFlowMock/Server/state.json`. <br>• **Backup:** `allowBackup=false` plus `dataExtractionRules` that exclude everything. | No native library, so 16 KB page alignment is trivial; no Room/KSP; schema parity with iOS keeps the sync logic portable. Reminder rows live in the account DB so the D25 erase removes them. EncryptedSharedPreferences is deprecated; Tink is not needed for one blob. |
| A6 | **Networking and data-source selection [R]** (D20). <br>• **Client:** OkHttp with `followRedirects(false)`, `followSslRedirects(false)`, `CookieJar.NO_COOKIES` and no `Cache`. <br>• **Timeouts** per call via `withTimeout`: 15 s default; 90 s for `POST trips`, `POST trips/review`, `POST trips/{id}/review`, `PUT trips/{id}`, `POST guest/trips/review` and `POST auth/register`; 3 s for logout. A timeout or I/O error becomes `DataFailure.Offline`. Responses from another origin are rejected. <br>• **Headers:** no `Accept-Language` (03 OQ A11). No ETags in v1, for iOS parity; this supersedes 03 OQ A6 (OQ-A16). <br>• **JSON:** decode with `Json { ignoreUnknownKeys = true; explicitNulls = false }`. Requests encode explicit nulls where null matters (check `status`), and PATCH bodies are `JsonObject` diffs. <br>• **Selection:** Gradle properties `-Pcf.dataSource=mock\|api` and `-Pcf.apiBaseUrl=…` feed `BuildConfig.DATA_SOURCE` and `API_BASE_URL` in `:app`. Debug defaults to mock; release is always `api` at `https://api.chronoflow.net/v1`, rejects non-HTTPS, and contains no mock code. `:core:data-mock` is only a `debugImplementation` and perf dependency. There is no silent fallback. | No product flavors: flavors would double every variant and test task for one string. The airport search cache is an in-memory LRU, not an OkHttp disk cache. |
| A7 | **Sync and outbox [R]**, a port of D23–D25 and iOS `SyncEngine`. <br>• **Pull:** full pull users/me → account → trips → one transaction → details. One flight per epoch, 1 s debounce, `refreshNow`. Backoff `min(1 s × 2^n, 5 min)`, or a larger `retryAfterSeconds` when the server sends one. Epoch check on every response. <br>• **Auth failures:** 401 → forced wipe, except that a walkthrough POST 401 keeps its row (iOS 17f). 403 `email_unverified` → verify, never wipe. <br>• **Outbox:** each mutation and its outbox row commit in one transaction. FIFO `(created_at,id)`; chains `settings:<section>`, `trip:<tripId>`, kind. Unsupported kinds are parked (OQ 31). A final refusal (4xx except 401/408/425/429, and not email-unverified) is recorded in `settings.syncFailure` and undoes the change (`StoreApplication.refuse`); a refused check-off with a notice also sets its trip's `plan_versions.detail_version` to NULL, so the next pull fetches the server's marks. Walkthrough rows are never refused, and a stale check-off (409, or 422 on `plan_input_hash` or with the `form` sentence "This plan has changed. Refresh it before marking this step.") is kept for rekeying (iOS `OutboxFlusher.swift:93–107`, `isStale` l.143–150). <br>• **Triggers:** `ProcessLifecycleOwner` ON_START syncs; ON_STOP only disables sync, as iOS does (`SessionSync.setEnabled(false)`: it cancels the running flight, keeps the retry state and makes no source call). The network callback is a hint only. **No WorkManager sync and no background flush.** <br>• **Login upsert:** preserves pending local edits (see "defects we must not port"). | iOS parity; offline edits are durable and flush on the next start. This supersedes 09 OQ-A9.5, 11 §4.7 and 03's WorkManager outbox (OQ-A17). |
| A8 | **Navigation [R].** <br>• **Routes:** Navigation 3 with `@Serializable` `NavKey` routes mirroring iOS `Route.swift`. `RootNavigator` exposes `StateFlow<RootState(route, revision)>`; screens sit in `key(revision)`; UI tests assert the route through a debug-only probe (D17). <br>• **Tabs:** Today, My trips and Settings, one back stack each. Reselecting a tab pops it to the root and scrolls to the top. System back on a non-Today root goes to Today, then exits. <br>• **Shell:** `NavigationSuiteScaffold` shows a bar below 600 dp and a rail from 600 dp. The bar never hides on scroll (user, 2026-11-05, RN plan l.1460). <br>• **Flows and overlays:** auth, wizard, walkthrough and terms replace the shell; sheets are `ModalBottomSheet`; alerts are `AlertDialog`. <br>• **Back:** predictive back is on (`enableOnBackInvokedCallback="true"`, table in `05 §15.5`). <br>• **Process death:** always a cold launch with the splash (iOS OQ 16, C4). `MainActivity` sees `savedInstanceState != null` with a freshly created `AppGraph` and ignores the saved stack. Only wizard recovery (30 min) survives, through its `drafts` row. <br>• **Config changes:** `configChanges="orientation\|screenSize\|screenLayout\|smallestScreenSize\|density\|keyboard\|keyboardHidden\|navigation\|uiMode"`. `locale`, `fontScale` and `layoutDirection` still recreate the activity, so `ConfigurationPolicy` runs again. | Nav3's back stack is plain state that we own, so per-tab stacks and route assertions are simple. Handling size changes ourselves keeps fold and resize transitions smooth, and the timeline keeps its scroll position (6e). |
| A9 | **Design-system mapping [R].** <br>• **Tokens:** `:tools:tokengen` writes committed `CfColors.generated.kt`, `CfSky.generated.kt`, `CfTypeScale.generated.kt`, `CfDimens.generated.kt`, `TimelinePalette.generated.kt` (JVM ARGB for `:core:plan`), `res/values{,-night}/colors_launch.xml` and `TokenGenReport.md`. They are sRGB only (OQ-A7). <br>• **Theme:** `ChronoFlowTheme` provides `LocalCfColors` (static), `LocalCfType`, `LocalCfShapes`, `LocalCfSpacing`, `LocalReduceMotion`, ripple and selection colors, and `MaterialTheme(colorScheme = colors.toM3())` using the `08 §7` table. It is not M3 Expressive and uses no dynamic color. <br>• **System components** are Material 3, tinted by tokens: top app bars (`LargeTopAppBar` collapsing, system font bold), navigation bar and rail, `ModalBottomSheet`, `AlertDialog`, Switch (dawn track, `dawnForeground` thumb), date and time picker dialogs, Snackbar, segmented buttons, the pull-to-refresh indicator. <br>• **Designed surfaces** follow the boards' dp exactly: launch overlay, Welcome, auth cards, Today cards and rows, trip header, day headers, timeline, list rows and end card, wizard cards, pills and checking arc, settings cards and rows, sheet content, the refresh capsule. `CfButton` has 5 sizes, each with a ≥ 48 dp target. Text fields are `BasicTextField(TextFieldState)` with a designed decoration. <br>• **Effects:** no backdrop blur. Shadows use `Modifier.dropShadow` with the two soft-shadow tokens, never on the timeline or sticky elements. `cfPress` is a `Modifier.Node`: scale 0.97, alpha 0.85, 120 ms, `cubic-bezier(0.25,0.1,0.25,1)`; with Reduce Motion, alpha only. <br>• **Fonts:** static TTFs, one `FontFamily` per Fraunces optical size (≥ 40 → 48, ≥ 26 → 32, else 20), warmed off the main thread after the overlay's first frame. <br>• **Icons:** lucide `ImageVector`s from a pinned release (ISC) for designed glyphs; Material Symbols Rounded vectors for system chrome. No icon font, no `material-icons-extended`. | D1b translated: system controls are native (TalkBack, IME, predictive back and large screens for free), and designed surfaces are drawn as designed, never a hand-built look-alike of a system control. |
| A10 | **Window and device policy [R]** (D1c, D16, `14 §4`). <br>• **Geometry** comes from window size and insets only; no 390 × 844 or 411 × 914 constant reaches layout. Minimum width 280 dp; height-constrained windows scroll. Per-screen widths are in "Android spec: window policy". <br>• **Font scale** is capped at 1.3 at the root (iOS xxLarge cap, OQ 10) by `ConfigurationPolicy` in `attachBaseContext`; tests run at 1.0, 1.3 and 2.0, where 2.0 must render as 1.3. <br>• **Layout direction:** LTR always (`supportsRtl="false"`, locale coerced to en/nl). <br>• **Orientation:** `screenOrientation="portrait"` on phones. Android 16 ignores it on large screens, so every screen must work in landscape there; no compatibility opt-out. <br>• **Edge-to-edge** through `enableEdgeToEdge()` and `WindowInsets.safeDrawing`; touch targets ≥ 48 dp. | "Every phone + foldables + large font" is the Android form of D1c. Android 16 removes orientation locks for apps targeting 36 on sw ≥ 600 dp. |
| A11 | **Launch [R]** (D2–D8, D12, D14, D26). <br>• **System splash:** `installSplashScreen()` runs before `super.onCreate`. Theme `Theme.App.Starting`: `windowSplashScreenBackground` = skyTop (`#1F285A` light, `#06091C` night), icon = `launch_moon` (288 dp icon without background, 192 dp safe circle). <br>• **Hand-over to the overlay:** `setKeepOnScreenCondition { !launch.overlayPlaced }` keeps the system splash until the Compose overlay has been laid out (`Modifier.onPlaced`). The condition must never wait for a draw: it cancels draws while true, so a draw-based flag deadlocks (`05 §15.1` says `overlayFrameDrawn`; corrected here). The overlay's frame 0 matches the system icon pixel for pixel (parity test against `iconView` bounds); `setOnExitAnimationListener { it.remove() }` removes the system view at once. <br>• **Overlay:** one Canvas driven by a `withFrameNanos` clock that plays the ported `LaunchTimeline` (one track per property, D4) with `LaunchLayout`, `FlightPath` (128 samples; strokeEnd by arc length, S5), radial gradients without blur, and the wordmark as a vector path (D8). <br>• **Timing:** `t0` is the overlay's first drawn frame (trace `first-commit`, D3). Known endings are scheduled ahead (D5); readiness follows D12 (1500/2000/8000 ms). <br>• **Once per process:** the overlay plays only on the first activity start of the process (D14). A warm start within the process gets a 300 ms fade (05 OQ A2). <br>• **Before the first frame:** `Application.onCreate` builds the lazy graph only. Before the overlay's first frame there is no disk, no network, no DataStore and no fonts. The single exception is the ≤ 64 B `device-config` file read in `attachBaseContext` on API < 33 (language) and < 31 (theme). On newer APIs the system persists both: `LocaleManager.applicationLocales` and `UiModeManager.setApplicationNightMode`. | iOS needed a separate overlay window to beat SwiftUI; on Android the splash screen API plus the keep condition gives the same guarantee: frame 0 equals the system splash, so there is no flash. The `device-config` exception is the price of per-app language on API 29–32; it is one tiny file, read under a scoped StrictMode allowance. |
| A12 | **Timeline architecture [R]** (`04` design B). <br>• **List:** a `LazyColumn` with one item per day, stable keys and `contentType`. <br>• **Drawing:** each day's static content is an immutable struct-of-arrays op list, `DayDrawing`. It is built off the main thread on `Dispatchers.Default.limitedParallelism(2)` behind a `TextMetrics` interface: Android `TextPaint` per build job, a fake in JVM tests. It is replayed in `drawWithCache` inside its own `graphicsLayer`. <br>• **Clock and input:** `NowLine` is an isolated composable and the only clock reader. An invisible semantics overlay, `TimelineTargets`, carries accessibility; one `pointerInput` per day resolves taps through `HitIndex`. <br>• **Painting:** group alpha is pre-composited, so there is no `saveLayer`. Hatch uses a `LinearGradient` with `REPEAT`, dots a `BitmapShader`. Lucide paths are parsed once. No `TextMeasurer` inside the canvas. <br>• **Cache:** `DayDrawingCache` ≤ 4 MiB, ≤ 512 KiB per drawing, ≤ 8 entries. <br>• **Invalidation gates:** scroll 0 static / 0 clock; minute tick 0 / 1; day boundary 0 / 2. <br>• **Fallbacks, in order, each a bounded spike:** B′ record a `Picture` per day; C fixed-height strips; D a custom `View`. They are tried only on replay or scroll evidence: `timelineReplayMaxMs` above 1 ms on a low-end run (OQ-T27), a miss of the emulator CPU band, or a missed device scroll row. A first-frame, memory or build-on-main miss goes to the fix its trace sections name (1e Task 9), never to a fallback. | iOS proved a per-day drawn surface (`TimelineRenderer`), and RN proved the opposite: one view per block janked, and only the native day merged (`09 §4`). Building the drawing off-main and replaying it should keep frame CPU under 8 ms P99 on the reference device; that is the aim, unproven until the first reference run (OQ-A22, OQ-A45). Phase 1e (2026-11-17): design B stands on the emulator, see `docs/perf-timeline.md`; device rows open. |
| A13 | **Reminders [R]** (C5, iOS Phase 19). <br>• **Alarms:** `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` on API 29–30, where exact alarms need no permission, and inexact `setAndAllowWhileIdle(RTC_WAKEUP, …)` on API 31+. No `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`. <br>• **Engine values (port):** 48 h window, ≤ 60 registrations, 15 min coalescing, ≤ 6 a day, 20 min `setTimeoutAfter`, 5 s margin, 36 h lookback. <br>• **Channel:** `reminders` ("Reminders on this phone", `IMPORTANCE_DEFAULT`), re-localized at start. <br>• **Receivers:** `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` and `LOCALE_CHANGED` enqueue expedited unique work `reminders-reschedule` (REPLACE). A 12 h periodic refill keeps the window full. WorkManager is used only for this. <br>• **Delivery:** the alarm receiver posts with `notify(tag = identifier, id = requestCode)`, then deletes its row and appends to the ledger inside `goAsync`. <br>• **Taps:** an explicit `PendingIntent` to `MainActivity` with `tripId`, `blockId`, `inputHash` and `accountId`, routed like iOS `AppTabs.swift:370–383`. <br>• **Permission:** POST_NOTIFICATIONS on API 33+ through the SetReminders states. Below API 33 there is no runtime permission, so `notificationPermissionRequested` is the in-app opt-in: until the person turns reminders on (the Reminders page or the first-trip offer), the state is "ask" and nothing is scheduled, as on iOS (`ReminderSelection.swift:28`). <br>• **Accepted losses:** on API 29–30, Doze delays of up to about 9 min (one allow-while-idle alarm per app per 9 min); on API 31+, an inexact alarm may arrive up to 60 min late (AOSP `maxTriggerTime`: a window of 75 % of the time ahead, capped at 1 h), and the receiver drops a reminder that arrives after its expiry (at most 20 min after the step starts) instead of showing it stale; reminders between a reboot and the first unlock (no Direct Boot). This supersedes the "about 9 min" Doze bullet of `14 §7`. | From API 31, exact alarms need a sensitive permission and Play declarations; on API 29–30 they need neither, and inexact alarms there have no delivery cap. A 20 min tolerance is fine for "seek light from 14:00". iOS reminders are also delivered by the system without background code. |
| A14 | **Billing: none [R]** (OQ 41). No Play Billing, no purchase UI, no web links for buying. Credits and plan come from the server. Plan & credits shows the NoLink copy; WizardReadyNoCredits shows no website button; `BILLING_LINK_OUT=false`. | iOS parity. Steering to web purchases is restricted on Play. |
| A15 | **Passkeys [R]** (iOS OQ 20, passkey addendum l.2055). <br>• **Library:** Credential Manager (`androidx.credentials` 1.6.0 + play-services-auth). <br>• **Sign-in:** `GetCredentialRequest(GetPublicKeyCredentialOption(requestJson))`. <br>• **Registration:** `CreatePublicKeyCredentialRequest` in Settings › Security › Passkeys. <br>• **Parameters:** rpId `app.chronoflow.net`, timeout 60000, challenge valid 120 s. Bodies are built from parsed fields as unpadded base64url. A user cancel is silent. <br>• **Gate:** everything sits behind `PasskeyCapability`, which stays false until server items S1 (assetlinks `get_login_creds`), S2 (`android:apk-key-hash` origin) and S3 (management merged) are live (OQ-A21). Until then every passkey entry point is hidden. <br>• **Passwords:** autofill through `contentType` semantics; commit only after a successful sign-in (05 OQ A14). | The server cannot verify Android passkeys yet (`03 §9.4`). Hiding the entry points beats shipping a button that fails. |
| A16 | **App Links [R]** (iOS OQ 25). <br>• **Filters:** `autoVerify` intent filters for `https://app.chronoflow.net/reset-password/…` and `/email/verify/…`, parsed by the ported `AuthLink`: verify needs 5 segments, a UUID id, a 40-hex hash, `expires` digits and a 64-hex signature; reset needs 3 segments and exactly one `email` query item. <br>• **Delivery:** `singleTask` plus `onNewIntent`. A reset link waits until the user is signed out; verify links go to a per-account inbox. <br>• **Notifications** use explicit component intents, never URLs. Server S1 publishes `assetlinks.json`. | No custom scheme, so the links cannot be hijacked. |
| A17 | **Localization pipeline [R]** (`10`). <br>• **Generator:** `tools/l10n/xcstrings2android.py` (`python3 -I`, stdlib only) reads the iOS catalogs at `tools/ios-pin.txt`. It writes `res/values{,-nl}/strings_ios.xml` with names `<table>_<slug>` (48-char cap, sha1-6 on collision), keeping `names.lock.json` append-only. Android-only strings live in `tools/l10n/android-only.json` (each with `replaces`) → `strings_android.xml`. A generated `CopyIndex.kt` maps keys; `plan_copy.json` is copied verbatim. <br>• **Locales:** `generateLocaleConfig = true`, and `resources.properties` sets `unqualifiedResLocale=en`. <br>• **Date and number formats:** fixed month and weekday tables (en `Jan…Dec`, `Mon…Sun`; nl `jan feb mrt apr mei jun jul aug sep okt nov dec`, `ma di wo do vr za zo`); 12-hour `AM`/`PM` from the catalog (nl `a.m.`/`p.m.`); the minus sign is U+2212. The plan's 12/24 h follows the account `timeFormat`; wizard pickers follow `DateFormat.is24HourFormat`. <br>• **Lint as errors:** `MissingTranslation`, `StringFormatInvalid`, `StringFormatCount`, `PluralsCandidate`. No string literal reaches `Text()`. | One source of copy for both apps; 9 keys share an English string but differ in Dutch per table, so names are namespaced per table (`10 §2`). The prototype produced 928 strings and 31 plurals per language with 0 mismatches. |
| A18 | **API contracts [R]** (`03 §16`). <br>• **Generator:** `tools/contracts-gen/generate_kotlin.py` reads `resources/js/types/App/**/index.ts` at web `e36368ec` through `git show`. Roots: `ApiTripListItem`, `ApiTripDetail`, `ApiTripInput`, `ApiTripReview`, `ApiTripSaveResult`, `ApiTripInputSnapshot`, `ApiAirportSearchResults` and the enums. <br>• **Output:** `@Serializable` classes with `@SerialName`, enums with `Unknown(raw)`, a `BlockView` serializer keyed by `type` (20 cases), `CopyArg` vs `CaffeineCopyArg` told apart by the `drinks` key, `answer` as `Map<String, JsonPrimitive>`. Overlay: `ScreeningQuestion = 'shift_work'`. <br>• **Hand-written:** auth, account, passkey and wizard-input DTOs (about 1.1k lines on iOS). <br>• **Check:** output is committed; `contractsCheck` runs `--check`. | There is no OpenAPI. Mirroring the iOS generator keeps both apps on one pinned contract. |
| A19 | **Test tiers, budgets, devices, seeds [R]** (`13`). <br>• **Tiers:** T0 JVM (logic, geometry over every profile in `profiles.json`); Robolectric + Roborazzi (layout, reference-free guards); T1 emulator `pixel` (Pixel 8, API 36) in `make check`; T2 `fold`, `small`, `api29` on request only; T3 a physical reference device and a low-end device for perf gates. <br>• **Budgets:** emulator test < 10 s, Robolectric < 5 s, JVM < 1 s except declared perf tests. `runTest(timeout = 60.seconds)`; instrumented `timeout_msec=60000`; `Scripts/run-tier.sh` idles out at 600 s and stops at 3,600 s. <br>• **Seeds:** identical `CF_*` names as intent extras on a debug-only exported `SeedReceiverActivity`; `TestSeeds.apply` follows `13 §5` steps 1–7; profile `screen` skips splash and readiness, profile `launch` is real. <br>• **Snapshots:** reference-free guards by default (small, nl, largeFont, light and dark, foldInner); stored references are opt-in, named `<Screen>_<state>_<profile>_<lang>_<theme>`. JVM renders are never compared with emulator renders. <br>• **Other:** no Test Orchestrator; MockWebServer deny-by-default; test tags are the iOS dotted ids, exposed through `testTagsAsResourceId` at the root. | Translates the iOS tiers (user, 2026-11-06) and tests rules (user, 2026-11-10). iOS `make check` never met its 5 min budget (408 s, 470 s, 1,104 s cold, `13 §1`), so Android measures its tiers in Phase 0a and keeps them inside budget from the start. |
| A20 | **Code quality [R].** Spotless with ktlint and the Compose rules; Android Lint with warnings as errors (`abortOnError`, `warningsAsErrors`, a baseline only for generated code); Konsist rules (A3); Kotlin `allWarningsAsErrors`. Compose stability config `stability_config.conf`; compiler reports behind `-PcomposeReports=true`. **No detekt.** | detekt 1.23.8 is built on Kotlin 2.0.21 and 2.0 is alpha. Mirrors iOS "No SwiftLint" (D9). `01 A9` and `13` proposed detekt. |
| A21 | **Performance rules [R].** <br>• **Debug StrictMode:** `ThreadPolicy.detectAll().penaltyLog()`, with `penaltyDeath` for disk reads on the launch path; `VmPolicy.detectLeakedClosableObjects()`. <br>• **Startup:** a minimal `Application.onCreate`; no disk or network on main. App Startup keeps only `ProcessLifecycleInitializer` (A7's sync triggers) and `ProfileInstallerInitializer` (the profiles); Phase 3a measures both in a startup trace and removes every other initializer with `tools:node="remove"`, and `verifyReleaseManifest` rejects any other (OQ-A47). <br>• **Profiles:** baseline and startup profiles generated by `:benchmark` and committed; `BaselineProfileMode.Require` in benchmarks. <br>• **Trace sections:** `launch`, `first-commit`, `session`, `ready`, `exit`, `intro`, `cf.timeline.firstFrame`; `reportFullyDrawn()` after the hand-off. <br>• **Debug and benchmark only:** JankStats and LeakCanary (LeakCanary debug only). <br>• **Release:** R8 full mode (`optimization { enable = true }`), `-assumenosideeffects` for `Log.v/d/i`. | Measured budgets need reproducible builds; StrictMode makes D26 enforceable instead of a review item. |
| A22 | **Observability [R].** No crash or analytics SDK. On the next launch, debug builds log `ApplicationExitInfo` for the previous exit. The field signal is Play vitals (crash 1.09%, ANR 0.47%, 8% per device model, `14 §8`). Never log secrets. | iOS has none either (`14 §15`); privacy declarations stay minimal. |
| A23 | **Security and privacy [R].** <br>• **Secure screens:** `FLAG_SECURE` on the recovery-codes and two-factor-setup screens (OQ-A13). <br>• **Clipboard:** recovery codes are copied with `EXTRA_IS_SENSITIVE` on API 33+ and cleared after 60 s. <br>• **Secret storage:** passwords, tokens, two-factor codes and link secrets never enter a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the DB, the outbox or logs. <br>• **Network:** cleartext only in the debug network config, for `10.0.2.2`. <br>• **No WebView:** policy pages open in a Custom Tab (05 OQ A6, `androidx.browser`). <br>• **Downloads:** recovery codes are saved through `CreateDocument("text/plain")` as "Recovery codes.txt", UTF-8 without BOM and with LF line ends; the fallback is `ACTION_SEND`. | D24 extended to Android's state-saving mechanisms. |
| A24 | **Scope parity [F/R].** <br>• **Omitted, as on iOS:** calendar (OQ 42); trip complete, reopen and delete (OQ 31: no API endpoint, `07 §1`); billing hand-off (OQ 41); data download (session-only web route); email change (D27); Live Activity and practice mode; the optional-items switch (`show_optional_items` is never sent). <br>• **Not added on Android either** (`14 §15`): widgets, Live Updates, app shortcuts, Assistant, in-app review, FCM, analytics, background sync, in-app updates, notification actions, certificate pinning. | "No design, no invention": no board, no feature. |
| A25 | **Inherited iOS user decisions.** See the table below. | The user decided these for the product; only their platform form changes. |

### Inherited iOS decisions (D-numbers, dated user decisions, OQ answers)

| iOS | Decision on iOS | Android form |
|---|---|---|
| D1 | iOS 17 floor, Swift 6 | A1: minSdk 29, Kotlin 2.4 K2, no `if (SDK_INT …)` below 29 |
| D1b | Liquid Glass via system controls only; designed surfaces as drawn | A9: M3 system components tinted by tokens; designed surfaces exact; no imitation of glass (OQ-A6) |
| D1c | Every iPhone incl. Duo; geometry from window and four insets; 7-simulator matrix skipped (user, 2026-11-06) | A10, A19: every phone + foldables; profiles in JVM tests, one emulator in `make check`, T2 AVDs on request |
| D2–D5 | Overlay committed before the root; `t0` = first commit; one track per key path; endings committed ahead | A11 |
| D6 | Welcome in the UI toolkit, delays from the shared `LaunchClock` | Compose Welcome reading the same `LaunchClock` (3c) |
| D7, D8 | Storyboard launch screen with a 288 pt moon; baked wordmark | A11: SplashScreen API, 288 dp moon PNG, wordmark vector |
| D9 | XcodeGen, `swift format`, no SwiftLint | A20: Gradle convention plugins, Spotless/ktlint, no detekt |
| D10 | TokenGen OKLCH → P3, committed, stale test | A9: TokenGen port → sRGB (OQ-A7), committed, `verifyTokens` |
| D11 | Static TTFs registered off-main after the overlay flush | A9: `res/font`, warmed off-main after the first frame |
| D12 | Readiness reducer with 1500/2000/8000 ms | Ported pure reducer in `:feature:launch` (3b) |
| D13 | Snapshots via `sample(at:)` | `LaunchTimeline.sample(t)` drives JVM geometry tests and Roborazzi frames (3a) |
| D14 | Overlay on the first connection of the process only | A11 |
| D15 | `.native` id, no URL scheme | A2, A16 |
| D16 | iPhone, portrait only | A10: phones portrait; large screens in any orientation (Android 16 rule) |
| D17 | `Route` enum + `Navigator`; UI tests assert the route | A8 |
| D18–D22 | GRDB 7.8 per-account DB; storage attributes; DataSource mock/api; Keychain session; UUIDv7 device id | A5, A6 |
| D23–D25 | SyncEngine; outbox and reads; sign-out wipe | A7 |
| D26 | No I/O before the overlay commit; no eager singletons | A4, A11, A21 |
| D27 | Auth follows D1c, plain sky, root cap, en/nl, no invented screens | 5a–5d |
| Nav titles (2026-11-10) | System large title, system font bold, collapses | `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`, system font (07 OQ-10) |
| Test tiers (2026-11-06) | quick/targeted/check/duo/matrix; `Scripts/ci.sh` runs `make generate check`, then `git diff --exit-code` | A19 and the commands table in AGENTS.md |
| Tests rules (2026-11-10) | Cheapest layer, one journey, seeded launch, exact queries, budgets, run only failing | AGENTS.md "Tests" |
| Cleanup (2026-11-10/11) | Remove finished worktrees and simulators | CLAUDE.md "Cleanup" (worktrees, Gradle caches, emulators, AVDs) |
| Plain sky, composition (2026-11-06) | No stars or dots; moon centred; composition offsets kept (S1, S2, OQ 11) | 3a, 3c, 5a |
| No health data (2026-11-10) | Only `age_band` and `shift_work` block; `exclude_caffeine`/`flight_movement_breaks` default false | 9e, 10b |
| Pull to refresh (2026-11-09) | Capsule copy, about 2 s, top inset | 6a |
| Reminders (2026-11-09) | Port RN local notifications; master switch; native 15/30/60 picker | 12a–12c |
| OQ 1 | No commit trailers | AGENTS.md Git |
| OQ 10 | xxLarge cap | font scale cap 1.3 |
| OQ 11 | Vertical composition `h − k → h/2 + (422 − k)` | 3a, 3c (all heights) |
| OQ 16 | Cold start always shows the splash | A8 process death |
| OQ 20, addendum l.2055 | Passkeys, management built ahead of the server | A15 |
| OQ 21 | Server password policy (12+, mixed case, symbols, not compromised) | 5d, 10c |
| OQ 22, 27 | Honest reset copy; no session from the 204 | 5c |
| OQ 23 | Offline banner "Needs a connection" / "Verbinding nodig" | 5a–5d |
| OQ 25 | Links open the app | A16 |
| OQ 26 | Backup exclusion | A5 |
| OQ 28 | Dutch source order | A17 |
| OQ 29 | Sign-out pending boards | 10a |
| OQ 31 | Unsupported outbox kinds parked | A7 |
| OQ 34 | Plan copy generated at web `afd80dc0` | A17 |
| OQ 38, 39 | Tab bar as drawn; walkthrough first | 6a, 8 |
| OQ 40 | One recovery `drafts` row | 9b |
| OQ 41 | No-credits without hand-off | A14 |
| OQ 42 | Calendar deferred | A24 |
| OQ 43 | Reminders sheet (superseded by Phase 19 decision) | 12c |
| OQ 44 | Guest now | 11a, 11b |
| OQ 46 | iOS one date/time pill; Android separate date and time dialogs per WizardCopy | 9d |

### How conflicts between sources are settled

1. A user decision recorded in the iOS plan or here wins.
2. Next comes iOS behavior at `f391a1fa`, including its recorded overrides of board values: S1–S6 (`plan.md:88–100`) and `05 §2`. These include: no stars or dots, moon centred, ready 1500–1900, moon flight ease-in-out, strokeEnd by arc length, stagger 2300/2400/2450/2600/2700/2750, a 12-character password hint, honest reset copy, two consent links, Verify back = sign out, and Walkthrough first run → Today, replay → Preferences.
3. Then current boards; superseded boards are never built (`14` Appendix A).
4. Then web/API code at the pin.
5. RN only as history.

An Android-only deviation needs an open question with a default, recorded below, before it is built.

### Where research disagreed (resolved here)

| Topic | Positions | Resolution |
|---|---|---|
| C1 database | Room (`02 OQ-7`) vs SQLDelight (`11`, `14`) | SQLDelight 2.4.1, framework driver (A5) |
| C2 DI | Hilt vs manual | Manual (A4) |
| C3 wide windows | Phone layout everywhere (`05 OQ A5`) vs per-screen | Per-screen policy (`14 §4`, A10) |
| C4 process death | Restore vs cold launch | Cold launch, iOS OQ 16 (A8) |
| C5 alarms | Exact (`02 OQ-17`) vs inexact | Exact on API 29–30 (no permission there), inexact on 31+; no exact-alarm permissions (A13) |
| C6 applicationId | `.native` vs `net.chronoflow.app` | `.native` until the first Play upload (A2, OQ-A1) |
| C7 memory gate | PSS vs RSS | Peak RSS anon ≤ 150 MiB; growth reported, > 40 MiB warns |
| minSdk | 26 (`09`, `10`), 28 (`01`), 29 (`11`) | 29 (A1, OQ-A2) |
| Data source | Product flavors (`02 OQ-3`) vs property | Gradle property + BuildConfig (A6) |
| ETags | Yes (`03 OQ A6`) vs no (`02 OQ-4`) | No in v1 (A6, OQ-A16) |
| Background sync | WorkManager outbox (`03`, `09`, `11`) vs foreground | Foreground only (A7) |
| Device id | UUIDv4 (`03`) vs UUIDv7 | UUIDv7 (D22) |
| Lint | detekt (`01 A9`, `13`) vs none | No detekt (A20) |
| Language override | AppCompat (`07`, `10`) vs platform | Platform `LocaleManager` + `ConfigurationPolicy`, spike in 0e (OQ-A24) |
| Timeline module | `:core:timeline` (`04`, `11`) vs `:core:plan` | `:core:plan` (A3, OQ-A26) |
| Navigation bar on scroll | Hide (`12 Q4`) vs keep | Keep, never hide (user, 2026-11-05) |
| Trace name | `cf:timeline:firstFrame` (`11`) vs `cf.timeline.firstFrame` (`04`, `13`) | `cf.timeline.firstFrame` |
| `DayDrawingCache` | 4 entries (iOS) vs 4 MiB / 8 entries (`04`) | ≤ 4 MiB, ≤ 512 KiB each, ≤ 8 entries |

## Trade-offs & Alternatives Considered

| Choice | Alternatives | Why not |
|---|---|---|
| Compose for everything, M3 system components | Views + XML; Compose with fully custom controls | Views double the UI stack; custom controls lose TalkBack, IME, predictive-back and large-screen behavior and would break "native components". |
| Timeline design B (per-day op list replayed in `drawWithCache`) | One composable per block (RN-like); `TextMeasurer` in `Canvas`; one giant canvas; `RecyclerView` + custom `View` | Per-block nodes janked in RN (`09 §4`, 40 → 15 dropped frames only after the native day). `TextMeasurer` in draw allocates and measures on main. A giant canvas breaks lazy loading and accessibility. A View is fallback D only. |
| SQLDelight | Room; raw `SQLiteOpenHelper` | Room needs KSP and generates DAO code we would fight for the v1 DDL; raw SQL loses typed queries and `Flow`. |
| Manual DI | Hilt, Koin, Metro | Hilt needs KSP or kapt and builds its component in `Application.onCreate`; Koin resolves at runtime; Metro is new. The graph is small. |
| Navigation 3 | Navigation-Compose 2.x; hand-rolled stack | Nav3's back stack is our own state, which makes per-tab stacks, route assertions and the process-death policy straightforward; a hand-rolled stack loses predictive-back integration. |
| Foreground-only sync | WorkManager periodic sync and outbox flush | iOS has no background sync; WorkManager adds a second session entry point, wipe races and Play background scrutiny for no user-visible gain. |
| Inexact alarms on API 31+ (exact on 29–30) | Exact alarms everywhere; WorkManager per reminder | From API 31, exact needs a sensitive permission and Play declarations (below 31 it needs neither, A13); WorkManager delays are deferrable and not meant for user-visible times. |
| Keystore AES-GCM blob | EncryptedSharedPreferences; Tink; DataStore + Tink | ESP is deprecated; Tink is a large dependency for one blob. |
| Gradle property for data source | Product flavors | Flavors double the variants and the test tasks. |
| No detekt | detekt 1.23 / 2.0-alpha | The stable release lags Kotlin 2.4; ktlint, Lint and Konsist cover what we need. |
| sRGB tokens | Display P3 `Color(…, ColorSpaces.DisplayP3)` | Wide-gamut rendering on Android depends on window color mode and device; sRGB is exact on every phone. Revisit in OQ-A7. |
| Material Symbols vectors | `material-icons-extended`; icon font | The extended artifact is very large and slows builds; a font needs text layout for icons. |
| Robolectric + Roborazzi | Paparazzi; emulator-only screenshots | Roborazzi runs inside Robolectric, so one test can assert semantics, touch targets and pixels; Paparazzi only renders. Emulator-only screenshots are slow (iOS lesson). |
| Cold launch after process death | Restore the back stack | iOS behavior (OQ 16); restoring half a session against a store that may have been wiped is the bigger risk. |

## Constraints and working rules

- **Sources are read-only** (see Context). Never run builds, `expo prebuild` or git commands that write inside them.
- **No design, no invention.** If a screen or state has no board, stop and say so; deferring beats inventing. Android-only UI (permission rationale, date dialogs, Snackbar) follows Material and the tokens, and its copy goes through OQ-A12.
- **Values come only from generated tokens and assets**, light and dark. No hard-coded color, dp size, font size or device geometry outside generated files. Touch targets are at least 48 dp.
- **Every phone, foldables and large font:** window size and insets only; font scale capped at 1.3 at the root; tested at small, pixel8, foldInner, landscapeSideNav and largeFont.
- **Native components:** Material 3 for system controls, tinted by tokens; never a hand-built look-alike of a system control.
- **English and Dutch together:** every visible string goes into the catalogs with its Dutch in the same commit. Lint `MissingTranslation` fails from Phase 0c.
- **Copy promises a clock shift, never symptom relief.**
- **No unneeded comments;** comment only the why. No static initialisers or eager singletons.
- **Git:**
  - `git init -b main`; the first commit holds only `plan.md`, `AGENTS.md`, `CLAUDE.md`, `docs/research/`, `_design/` and `.gitignore`.
  - Then one branch per phase, `feat/phase-<id>-<slug>`, stacked: a phase starts from the stack top once its dependencies are in the stack, and parallel phases are rebased onto the stack top in completion order ("Tasks"). Never commit to `main` after the first docs commit.
  - Atomic Conventional Commits (`type(scope): subject`, lower-case imperative) that tick the task here in the same commit.
  - No `Co-Authored-By` or any other trailer (iOS OQ 1).
  - Never merge, push or force-push unless asked. There is no remote yet (OQ-A31).
- **Local builds only;** no CI service yet. `Scripts/ci.sh` runs `make generate check`, then fails if generated files changed (`git diff --exit-code`).
- **Board updates** come from the designer into `_design/`; never edit boards. If a board changes mid-phase, finish against the old one and record the difference.
- **Who writes what:** Sonnet workers implement one phase brief; Opus reviewers check; the orchestrator runs emulator tiers the worker sandbox cannot (CLAUDE.md).


## The spec

The iOS spec sections below are platform-neutral, so they are the Android spec too, read at `f391a1fa`. Each row gives the exact iOS heading, its line, and the Android delta. When the delta is "none", implement the iOS text as written. When translating iOS terms:
- pt → dp; Dynamic Type → font scale (capped at 1.3);
- safe-area insets → `WindowInsets.safeDrawing`;
- VoiceOver → TalkBack;
- `UIAccessibility.isReduceMotionEnabled` → `LocalReduceMotion`;
- String Catalog → the generated string resources.

The Android-only spec follows the table.

| iOS heading (exact, `chronoflow-ios/plan.md`) | Line | Android delta |
|---|---|---|
| `### Tokens (TokenGen output; P3 as \`displayP3\` r g b)` | 141 | sRGB hex from `08 §4` (OQ-A7); derived colors from `Tools/TokenGen/Sources/TokenGenCore/Source.swift:66-113`; M3 roles per "Android spec: Material mapping". |
| `### Typography` | 173 | Same scale in sp (`08 §5`); `LineHeightStyle(Center, Trim.None)`, `includeFontPadding = false`, letter spacing in em; Fraunces optical size by rule (A9). |
| `### Splash layer tree and geometry (any \`w × h\`)` | 188 | The layer tree becomes the draw order of one Canvas; the geometry is identical for any `w × h` in dp, including the vertical composition `h − k → h/2 + (422 − k)` (OQ 11). |
| `### Timeline: shared opening (all modes)` | 205 | None. Keyframes in seconds: `12 §3`. |
| `### Endings` | 221 | None. "Committed ahead" means scheduled on the frame clock when known (A11). |
| `### Welcome intro (first launch of the process only)` | 243 | Process-level flag in `AppGraph` (D14). |
| `### Welcome layout (on \`cf-sky\`)` | 258 | Insets from `WindowInsets.safeDrawing`; ≥ 600 dp: copy card max 560 dp, centred (`14 §4.2`). |
| `### Readiness rules (RN Phase 21, with the RN bug fixed)` | 267 | None. Port of `AppCore/LaunchReadiness.swift`. |
| `### Reduce Motion` | 277 | `LocalReduceMotion = Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (or `!ValueAnimator.areAnimatorsEnabled()`), read once at activity creation; the seed `cf.reduceMotion` overrides it. Perf runs set the animator scale to 1 (`04 §10.3`). |
| `### Appearance` | 285 | The splash uses the appearance the system splash used, captured once in `attachBaseContext`. On API 31+ that includes the app night mode set through `UiModeManager`; on API 29–30 it is the system night mode. Welcome and the app follow `cf.settings.theme` (system/light/dark, per device, `SettingsDevicePreferences.swift:7,13`). The status bar stays light until the hand-off ends. |
| `### Accessibility` | 289 | The overlay is one node, "ChronoFlow is opening", with `paneTitle`; the app content is hidden with `hideFromAccessibility()` until the hand-off ends (`invisibleToUser` is deprecated in Compose UI 1.12.1 and fails `allWarningsAsErrors`). Status lines use `liveRegion = Polite` (no `announceForAccessibility`, deprecated in API 36). After teardown, focus moves to Welcome's first node. Headlines use `heading()`. No haptics. |
| `### Copy (String Catalog; English is the key)` | 296 | A17: generated resources, the same keys. |
| `### Core seams` | 311 | Same seams as Kotlin interfaces in `:core:model`/`:core:sync`, injected through `AppGraph` (A4). |
| `### Verified source map` | 366 | Reference only. Web paths are valid at `e36368ec`. |
| `### Account schema v1 and observation` | 382 | A5: `.sq` files, verbatim DDL, Flow observation. |
| `### API contract and offline sync` | 435 | A6, A7. Endpoint table in `03 §5`. |
| `### Auth geometry, tokens and typography` | 471 | Reference frame 375 × 812; insets top `safeTop + 16`, sides `+16`, bottom `+24`; card padding 20, radius 26, gap 14 (`05 §8.1`). |
| `### Form behavior, routing and accessibility` | 494 | IME and form behavior in "Android spec: forms and IME". |
| `### Auth copy (String Catalog; English sentence keys)` | 505 | Generated resources; Android variants only through OQ-A12. |
| `### Trips source contracts and behavior (reviewed plan v2, integrated 2026-11-07)` incl. `#### 1.1 How the API handles trips`, `#### 1.2 How the RN app handles trips`, `#### 1.3 What iOS already has`, `#### Trips gaps and risks` | 1317–1539 | None. Android starts from "1.3 What iOS already has" as the target. |
| `### Trips execution order (after Phase 11d)` | 1540 | Informs this plan's phase order (Phases 6–9). |
| `### Settings board fidelity (2026-11-09)` | 2040 | None. Also read `Docs/settings-board-fidelity.md`. |
| `### Pull to refresh and sync feedback (user, 2026-11-09)` | 2252 | M3 `PullToRefreshBox` indicator tinted by tokens, plus the designed capsule as a top overlay (`07 §10`). |
| `## No health data — server Phase 19 (user, 2026-11-10)` | 2477 | None. Also read `Docs/no-health-data.md`. |
| `### Navigation titles (user decision 2026-11-10)` | 59 | `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`, system font bold. |
| `### Test tiers (user decision 2026-11-06)` | 68 | A19. |
| Passkey management addendum (bold paragraph in 17h) | 2055 | A15. |
| 15d "Timeline drawing" (bold sub-phase) | 1705 | A12; the invariants and fixtures are reused, the renderer is not. |

### Android spec: build and repository layout

```
chronoflow-android/
  plan.md  AGENTS.md  CLAUDE.md  Makefile  version.properties  gradle.properties  settings.gradle.kts
  gradle/libs.versions.toml  gradle/wrapper/  stability_config.conf
  build-logic/convention/            convention plugins (below)
  app/  benchmark/  konsist/
  core/{model,plan,sync,reminders,wizard,network,data-mock,testing,database,datastore,data,designsystem,navigation,ui}/
  feature/{launch,auth,onboarding,today,trips,trip,wizard,settings,reminders}/
  tools/tokengen/  tools/l10n/  tools/contracts-gen/  tools/ios-pin.txt
  Scripts/{ci.sh,emulator.sh,run-tier.sh,check-benchmarks.py,measure-tiers.sh}
  docs/{research/,phases/,perf-timeline.md,perf-startup.md,device-matrix.md}
  _design/                            boards (read-only copy)
```

- **gradle.properties:**
  ```
  org.gradle.jvmargs=-Xmx8g -XX:+UseParallelGC -Dfile.encoding=UTF-8
  kotlin.daemon.jvmargs=-Xmx4g
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configuration-cache=true
  org.gradle.configuration-cache.parallel=true
  org.gradle.configuration-cache.problems=fail
  kotlin.code.style=official
  android.useAndroidX=true
  ```
- **Convention plugins:** `chronoflow.android.application`, `chronoflow.android.application.compose`, `chronoflow.android.library`, `chronoflow.android.library.compose`, `chronoflow.android.feature`, `chronoflow.jvm.library`, `chronoflow.sqldelight`, `chronoflow.android.test`, `chronoflow.spotless`, `chronoflow.roborazzi`. Each sets:
  - compileSdk 37, minSdk 29, targetSdk 36, Java 17;
  - `allWarningsAsErrors`, the Compose stability config and reports behind `-PcomposeReports=true`;
  - `testOptions.unitTests.isIncludeAndroidResources = true`;
  - the JDK 21 launcher for test tasks.
- **Build types:**
  - `debug`: suffix `.debug`, StrictMode, seeds, debug network config.
  - `release`: `optimization { enable = true }`, keep rules in `src/main/keepRules/*.keep`, `:app:analyzeReleaseR8Config` in `make check`.
  - **Perf variants:** the `androidx.baselineprofile` plugin's `benchmarkRelease` and `nonMinifiedRelease`, which are release-like, debug-signed and `<profileable android:shell="true"/>`. Perf-only code (the `cf.trip-page` fixture entry, `:core:data-mock`) is added to those two only, through `src/perf/` and their dependency configurations. If the plugin's build types cannot take extra sources, fall back to a hand-written `benchmark` build type: `initWith(release)`, `matchingFallbacks += "release"` (OQ-A28).
- **BuildConfig** is enabled in `:app` only: `DATA_SOURCE`, `API_BASE_URL`, `PERF_ENTRY` (perf variants).

### Android spec: process, activity and launch sequence

1. **`ChronoFlowApp.onCreate`.** Install StrictMode in debug. Create `AppGraph` lazily: no I/O, no `DataStore` access, no channel creation. Register the `ProcessLifecycleOwner` observer. Trace `launch` starts with the process (`Trace.beginAsyncSection`).
2. **`MainActivity.attachBaseContext`.** `ConfigurationPolicy.wrap(base)` applies three rules:
   - font scale `min(system, 1.3f)`;
   - locale coerced to en/nl, keeping our region;
   - LTR.

   On API < 33 it reads the ≤ 64 B `noBackupFilesDir/device-config` file (language choice). This is the only pre-frame read. It also captures the splash appearance (A11).
3. **`MainActivity.onCreate`**, in this order:
   - classify saved state with `launch.onActivityCreated` before `super.onCreate` (A8);
   - `installSplashScreen()`, then `super.onCreate(null)` for a process-death restore, otherwise pass the original bundle;
   - `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT))` and `window.isNavigationBarContrastEnforced = false`;
   - `setContent { CfRoot() }`;
   - `splash.setKeepOnScreenCondition { launch.holdsSystemSplash() }`; only the claiming activity holds it until placement. The exit listener removes a claimed/recreated splash immediately; an unclaimed warm splash fades for 300 ms.
4. **First frame.** `CfRoot` composes the overlay first. Its first layout (`onPlaced`) sets `overlayPlaced`, which releases the keep condition in the same traversal; its first draw marks `first-commit` (= `t0`). App content composes below it on the next frame. The overlay clock is wall time from `t0` (`System.nanoTime`), so a main-thread stall skips frames and never slows the timeline (`05 §15.2`).
5. **After `first-commit`**, on `Dispatchers.IO`:
   - the session restore (Keystore decrypt, then DB open; trace `session`);
   - DataStore preference load;
   - font warm-up;
   - notification channel upkeep;
   - the wipe-marker retry (D25).

   The readiness reducer receives these events and owns the 1500/2000/8000 ms rules (`ready`, `exit` traces).
6. **Hand-off.** Signed in: the app at `max(1500, ready)`. Signed out: Welcome at `max(1600, ready)`, then the intro (`intro` trace), then `reportFullyDrawn()` when Welcome is interactive. When signed in, `reportFullyDrawn()` follows the first Today content frame.
7. **Warm start** (new activity, same process): no overlay, a 300 ms system-splash fade. A configuration recreation resumes a running overlay at the same t0 with the same timeline and coordinator; it never holds the system splash. **Process death:** a cold launch (A8), detected by a mismatched or missing saved `cf.launch.processToken`; saved UI state is discarded.

### Android spec: configuration and appearance

- **Language.** `Language` (`en`, `nl`) is explicit in all logic, passed to `CopyCatalog` and the formatters, and never read from `Locale.getDefault()` in logic.
  - The choice `cf.settings.language` is `system|en|nl`. With `system`, `nl*` resolves to Dutch and anything else to English.
  - API 33+: `LocaleManager.applicationLocales` is the source of truth; `system` = empty list.
  - API < 33: the `device-config` file holds the choice. A change recreates the activity.
- **Theme.** `cf.settings.theme` (`system|light|dark`) lives in DataStore and is applied as `ChronoFlowTheme(darkTheme = choice.resolve(isSystemInDarkTheme()))`. On API 31+ it is also mirrored to `UiModeManager.setApplicationNightMode` so the next system splash matches. The splash appearance rule is in the spec table ("### Appearance").
- **Reduce Motion:** see the spec table. It also disables `cfPress` scaling, the Welcome intro, sheet springs (M3 defaults stay) and the refresh capsule slide; the capsule fades instead.

### Android spec: window policy (from `14 §4.2`, breakpoint 600 dp as iOS 600 pt)

| Screen | < 600 dp | ≥ 600 dp | Board |
|---|---|---|---|
| Launch overlay, Welcome | Formula for any w × h | Same formula; Welcome copy card max 560 dp, centred | — |
| Walkthrough, Terms, auth screens, guest sign-up | Phone layout | Column max 560 dp centred (guest sign-up 520) | GuestSignup |
| Shell | `NavigationBar` | `NavigationRail` | Main |
| Today, My trips | Phone | Cards max 640 dp, centred | — |
| Trip page | Timeline/list toggle | Side by side: list 280 dp, 320 dp from 900 dp; no toggle | PlanWide*, PlanRemoteWide* |
| Wizard | Phone | Column max 520 dp, centred from 669 dp | WizardCopy |
| Settings | Phone | Column max 560 dp | Set2FAWide, Set2FAManageWide |
| Password prompt | Bottom sheet | Centred 560 dp dialog, max height 90% | PasswordPromptWide |
| Bottom sheets | `ModalBottomSheet` | `sheetMaxWidth = 640.dp` | — |

Every screen scrolls when height-constrained. The minimum width is 280 dp; below the timeline's minimum lane widths, a day scrolls horizontally, as on iOS. A fold is a width change: no posture UI, and the anchor and sheets survive (`14 §3.3`).

### Android spec: state survival (from `14 §3.4`)

| State | Rotation, fold, split | Locale or font-scale recreation | Process death |
|---|---|---|---|
| Launch overlay | Re-lays out for the new w × h | Not replayed | Cold launch |
| Auth forms | Fields kept (`remember`) | Text kept; password fields clear | Empty form at the root route |
| Wizard | ViewModel | ViewModel; step from the saved key | Recovery row (≤ 30 min), or save resumes in Calculating |
| Trip page | Anchor `(dayId, offsetInDay)` re-applied | Anchor from `SavedStateHandle`; sheet route reopens | Cold launch → Today |
| Password prompt | Stays open, field kept | Reopens empty | Gone |
| 2FA setup, recovery codes | ViewModel memory only | ViewModel | Gone (server setup is idempotent; codes need the password again) |
| Sign-out pending sheet | Kept | Kept | Gone; the outbox keeps the changes |

### Android spec: storage layout

| Item | Location |
|---|---|
| Namespaces | `Api("ChronoFlow", "net.chronoflow.app.native")`, `Mock("ChronoFlowMock", "net.chronoflow.app.native.mock")` (`02 §4.1`) |
| Account DB | `noBackupFilesDir/<ns>/Accounts/<uuid>/account-<uuid>.db` (+ `-wal`, `-shm`) |
| Session blob | `noBackupFilesDir/<ns>/session.bin`; Keystore alias `<ns>.session` |
| Guest secret | Keystore alias `<ns>.guest` + DataStore key (06 OQ 12) |
| Wipe marker | `noBackupFilesDir/<ns>/pending-wipe.json` |
| Guest wizard draft | `noBackupFilesDir/<ns>/guest-wizard.json` |
| Mock server state | `noBackupFilesDir/ChronoFlowMock/Server/state.json` (schema 2) |
| Device config (API < 33) | `noBackupFilesDir/device-config` |
| Device prefs | DataStore `device_prefs`: `api.deviceId`, `cf.settings.language`, `cf.settings.theme`, `notificationPermissionRequested` |
| Test seeds | `filesDir/uitests/<CF_AUTH_TEST_ID>/` (debug only) |

### Android spec: navigation and routes

- **Routes** (`:core:navigation`, `@Serializable`): `Welcome`, `SignIn`, `TwoFactor`, `Forgot`, `Reset(email, token)`, `SignUp`, `Verify`, `GuestWizard`, `Terms`, `Walkthrough(replay)`, `Today`, `Trips`, `Settings`, the settings pages (`Profile`, `Account`, `Language`, `Security`, `Password`, `TwoFactorSettings`, `Passkeys`, `BodyClock`, `Preferences`, `Reminders`, `Plan`), `TripPage(tripId, dayId?, blockId?, awaitingSync, notificationInputHash?, notificationToken?)` and `Wizard(mode: Create | Edit(tripId) | Duplicate(tripId))`.
- **Sheets** are part of the route key (`sheet = Step(blockId) | Legend | TripActions | …`), so they reopen after recreation.
- **Gate order** (`SessionGateRouter`): verify → terms → walkthrough → app. Missing policies keep the gate loading.
- **Back:** predictive back everywhere except the wizard's no-back steps (Checking, Calculating) and the launch overlay. Auth back goes per `05 §15.5`; Verify back = sign out.

### Android spec: forms and IME (`05 §15`)

- **Fields:** `BasicTextField(TextFieldState)` and `BasicSecureTextField` (reveal last typed), with autofill `contentType` (Username, Password, NewPassword, SmsOtpCode).
- **Keyboards:** `KeyboardType.Number` for codes, `Ascii` for recovery codes.
- **IME:** `imePadding()` and `BringIntoViewRequester` keep the focused field and the primary button visible; `imeAnimationTarget` drives no layout feedback (never feed measured sizes back into state, `01 §8`).
- **Secrets:** never `rememberSaveable`.

### Android spec: Material mapping (system components, `08 §10`)

| Board element | Android component | Tint |
|---|---|---|
| Tab bar | `NavigationSuiteScaffold` (bar/rail) | Selected icon and label `primary`, indicator `muted`, unselected `mutedForeground` (OQ-A3) |
| Large titles | `LargeTopAppBar`, collapsing | `background` container, `foreground` text |
| Sheets | `ModalBottomSheet` | `popover` container, `border` drag handle |
| Alerts | `AlertDialog` | `popover`, actions `dawnText`, destructive `destructive` |
| Switch | `Switch` | Track `dawn` (on) / `input`, thumb `dawnForeground` |
| Segmented control | `SingleChoiceSegmentedButtonRow` | Selected `systemSelected` |
| Date and time | `DatePickerDialog`, `TimePicker` in a dialog or sheet | `toM3()` roles |
| Toast | `Snackbar` with inverse tokens, bottom (OQ-A5) | `foreground` container, `background` text |
| Pull to refresh | `PullToRefreshBox` indicator + the designed capsule overlay | `dawn` |
| Progress | `LinearProgressIndicator` (wizard 4 dp dawn bar) | `dawn` on `muted` |

`toM3()`:
- `primary` = dawnText; `onPrimary` = primaryForeground (light) / dawnForeground (dark);
- primary, secondary and tertiary containers = systemSelected; `tertiary` = dawn;
- `background`, `surface` = background; `surfaceVariant` = muted;
- `surfaceContainerLowest`/`Low` = card; `surfaceContainer`/`High` = popover; `surfaceContainerHighest` = muted;
- `error` = destructive; `errorContainer` = authErrorBackground;
- `outline` = mutedForeground; `outlineVariant` = border;
- inverse roles: foreground / background / dawn;
- `surfaceTint` transparent.

**Haptics** (behind a `Haptics` interface; none on splash and Welcome):

| Event | Android constant |
|---|---|
| Selection | `SEGMENT_TICK` |
| Switch | `TOGGLE_ON` / `TOGGLE_OFF` |
| Light | `CONTEXT_CLICK` |
| Success | `CONFIRM` |
| Warning and error | `REJECT` |
| Medium | `LONG_PRESS` |

### Android spec: manifest and permissions (`14 §6`)

- **Permissions:**
  - Release allows only: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (merged by WorkManager), plus androidx.core's `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (OQ-0a-20).
  - Banned: `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `FOREGROUND_SERVICE*`, calendar, `CAMERA`, `READ_PHONE_STATE`, `AD_ID` (`tools:node="remove"`), Firebase/c2dm, badges.
- **Application attributes:** `allowBackup="false"`, `dataExtractionRules`, `usesCleartextTraffic="false"`, `enableOnBackInvokedCallback="true"`, `localeConfig`, `supportsRtl="false"`.
- **MainActivity:** `singleTask`, exported, `configChanges` (A8), `screenOrientation="portrait"`, `windowSoftInputMode="adjustResize"`, App Link filters. No `directBootAware`.
- **Enforcement:** `:app:verifyReleaseManifest` fails on:
  - any permission outside the allowlist;
  - a custom scheme;
  - an exported debug component;
  - `debuggable` or `testOnly`;
  - cleartext;
  - a missing `localeConfig`.
  - an App Startup initializer other than `ProcessLifecycleInitializer` and `ProfileInstallerInitializer` (A21; Phase 3a adds this rule, `STARTUP_INITIALIZER`).

### Android spec: seeds and test hooks (debug only)

- **Seed names** are identical to iOS (`12 §2`): `CF_AUTH_TEST_ID`, `CF_UI_TEST_PROFILE=screen|launch`, `CF_AUTH_TEST_SIGNED_IN=demo|twofactor`, `CF_AUTH_TEST_WALKTHROUGH_SEEN`, `CF_AUTH_TEST_AUDIT_SCREEN`/`STATE`, `CF_PLAN_TEST_ROUTE=trips|trip:<id>[:list]`, `CF_PLAN_TEST_POSITION=summary`, `CF_PLAN_TEST_SHEET=step:<id>|legend`, `CF_SETTINGS_ROUTE`, `CF_WIZARD_AUTO_OPEN`, `CF_WIZARD_BOARD_CAPTURE`, `CF_MOCK_DELAY` (default 0).
- **Intent extras:** `cf.trip-page=S1…S6|large30|large60|<uuid>`, `cf.timeline-fixtures`, `cf.settings.theme`, `cf.settings.language`, `cf.reduceMotion`, `cf.freezeAt`, `cf.readyAt`, `cf.mainThreadStall`, `cf.holdSplash`, `cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay`, `cf.mode`.
- **Launch diagnostics** (String extras, debug and perf only): `cf.session=in|signedIn|out|signedOut|never` (default native), `cf.gate=verify|terms|walkthrough|app` (default app), `cf.stored=yes|no|never` (default yes), `cf.readyDelay` in finite seconds `0…60` (default 0). Presence of any stub key injects `StubReadiness`; otherwise `SignedOutReadiness` remains the default until 4e. `cf.readyDelay` is an absolute time on the shared launch clock, not a second delay from collection.
- **Fixed diagnostic endings:** `cf.mode=ready|slow|welcome` bypasses readiness. Ready/slow leave the route empty after removal; welcome publishes Welcome under the overlay, with a measured moon target and optional frozen intro. `cf.readyAt` is finite ms `0…60000`, default 4600, for slow mode. `cf.mainThreadStall` uses the same ms range and blocks from 300 ms after t0. Invalid values use the defaults. Release parsing and stalls are no-ops. `cf.freezeAt` still freezes a debug frame; `cf.holdSplash` remains debug only.
- **Welcome seams:** `welcome.signIn` and `welcome.plan` open debug-only `route.signIn` / `route.guestWizard` placeholders using the existing debug route resource. Back returns to final Welcome without replaying its claimed intro. Release and perf use `InertWelcomeDestinations`.
- **Welcome font gate:** while the overlay is active, compose Welcome when font warm-up completes or 1500 ms after the awaited first draw, whichever comes first. Without an overlay, compose immediately. The all-time debug StrictMode ChronoFlow disk/network counter covers the Welcome journey; the existing launch crash guard still ends at first commit.
- **Entry point:** `net.chronoflow.debug.SeedReceiverActivity` (debug-exported, in `app/src/debug`) applies `TestSeeds.apply` (`13 §5`), then starts `MainActivity`.
- **Mock accounts:** `demo@example.com` / `password`; `twofactor@example.com` / `password`, code `123456`, recovery `AbCdEf1234-GhIjKl5678`. Account ids `0199a3c0-0000-7000-8000-000000000000` / `…0001`; trips `…0001` (AMS→HND, active), `…0002` (AMS→JFK, upcoming).
- **Release readiness:** `testReleaseUnitTest` asserts that `Class.forName("net.chronoflow.debug.SeedReceiverActivity")` throws and that the mock never seeds.

### Android spec: release (`14 §11`)

- **Signing:** the upload key stays outside the repo; `CF_UPLOAD_*` live in `~/.gradle/gradle.properties`. The committed `app/debug.keystore` is never listed in production `assetlinks.json`.
- **Bundle:** `bundle { language { enableSplit = false } }`, so in-app language switching works offline.
- **Size:** download ≤ 8 MB, fail above 12 MB (`bundletool get-size total`); no `.so` except the OQ-0a-22 allow-list.
- **Tracks:** internal, then closed (12 testers × 14 days for a personal account), then staged 10/50/100%.
- **Play:** Data safety answers from `14 §9.2`; policy checklist P1–P11 (`14 §10`).

### Android-only copy awaiting approval (OQ-A12)

| Context | en | nl | Replaces (iOS) |
|---|---|---|---|
| 2FA intro (b1) | "You need an authenticator app." | "Je hebt een authenticator-app nodig." | settings "You need an authenticator app, like the one built into Passwords on iPhone." |
| Passkeys intro | "Sign in with your fingerprint, face or screen lock instead of your password." | "Log in met je vingerafdruk, gezicht of schermvergrendeling in plaats van je wachtwoord." | settings "Sign in with Face ID, a fingerprint or your screen lock instead of your password." |
| Notifications blocked step | "Tap Open phone settings and turn on notifications for ChronoFlow." | "Tik op Telefooninstellingen openen en zet meldingen voor ChronoFlow aan." | reminders "Open your phone's Settings.", "Tap Notifications, then ChronoFlow." and "Turn on Allow notifications." |
| Recovery-codes file name | "Recovery codes.txt" | "Herstelcodes.txt" | — |


## Performance budget

**Devices.**
- **Reference device:** Pixel 8 class (120 Hz).
- **Low-end:** Pixel 4a or Galaxy A15/A16 class (60 Hz).
- **Emulator:** the `pixel` AVD with `-gpu host`, animator scale 1 for perf runs.

**Where gates apply.**
- Hard device gates apply only on physical devices (tier T3). Until a device is attached they stay "open: device only" (OQ-A22). Emulator numbers are never device claims (iOS lesson, `13 §2`).
- On the emulator, these fail: the functional rows, the 800 ms first timeline frame, the 150 MiB peak RSS, builds on main = 0, at most 6 placeholder frames per `timelineScroll` iteration, and the CPU band. The CPU band: `timelineScroll` `frameDurationCpuMs` P90 or P99 fails when it exceeds max(1.25 × baseline, baseline + 1 ms), where the baseline is the median of the last three `PASS` rows with the same Class, Theme, Font and Page in `docs/perf-timeline.md` (fewer rows: the median of those; none: no band). A band miss counts only when one immediate re-run of the same configuration misses too (`make perf-timeline` re-runs it once). Everything else is reported. A change of more than ±25% against the same baseline is flagged and never fails; a counter whose baseline is 0 is flagged on an absolute rise instead (placeholders +2, GC +1). `docs/perf-startup.md` keeps its ±25% flag against its last row.

**Tooling.** `Scripts/check-benchmarks.py` reads `*-benchmarkData.json` from `build/outputs/connected_android_test_additional_output/`. It fails on a missing, non-finite or over-budget value and prints `Timeline metric: …` lines. Its own fixture injects 0.81 s and must fail.

**Build.** All Macrobenchmarks run on the perf variant (`benchmarkRelease`) with `CompilationMode.Partial(BaselineProfileMode.Require)`.

| Metric | Reference (hard / aim) | Low-end | Emulator | How it is measured | Phase |
|---|---|---|---|---|---|
| Cold start TTID, signed out | median ≤ 400 ms and below RN | ≤ 800 ms (proposal) | report | `StartupBenchmark`: `StartupTimingMetric`, `StartupMode.COLD`, 10 iterations, signed-out seed. RN is measured with the same harness against its release APK (`12 §1`) | 3a, 13 |
| Warm start TTID | ≤ 200 ms (proposal, OQ-A20) | ≤ 400 ms | report | Same, `StartupMode.WARM` | 3b, 13 |
| `onCreate` → overlay frame 0 | ≤ 50 ms (proposal) | ≤ 100 ms | report | `TraceSectionMetric("first-commit")` | 3a |
| Overlay frame pacing 0–3.2 s | ≥ 95% of display-rate frames (8.3 ms at 120 Hz) | ≥ 95% at 60 Hz | report | `FrameTimingMetric` frame count over `LaunchJankBenchmark` | 3a, 13 |
| First frame → first animated change | 300 ms + ≤ 1 frame | same | functional | JVM `LaunchTimeline.sample` test; trace marker | 3a |
| Launch jank 0–3.2 s | P99 `frameOverrunMs` ≤ 0; no frame over 1 frame budget | P99 ≤ 4 ms | report | `FrameTimingMetric` in `LaunchJankBenchmark` (cold start, 3.5 s capture) | 3a, 13 |
| Main thread before hand-off | 0 disk or network operations | same | functional | Debug StrictMode with `penaltyDeath` on the launch path; an instrumented launch test must survive | 0a, 3a |
| Stall behavior | With `cf.mainThreadStall=500` the overlay resumes at the wall-clock position; never stretched (iOS no-hitch-under-stall is not required, OQ-A19) | same | functional | Debug instrumented test reading `LaunchTimeline` position | 3b |
| Session known, signed out | ≤ 50 ms after `first-commit` | ≤ 100 ms | report | `TraceSectionMetric("session")` | 4e |
| Hand-off end, ready before 1500 | 1900 ms ± 1 frame from `t0` | same | functional | `exit` trace end − `t0` | 3b |
| Welcome interactive | 2550 ms ± 1 frame | same | functional | `intro` trace | 3c |
| Last intro item | 3200 ms ± 1 frame | same | functional | `intro` trace | 3c |
| TTFD | Hand-off end + ≤ 2 frames | same | report | `StartupTimingMetric.timeToFullDisplayMs` via `reportFullyDrawn()` | 3b, 7 |
| Plan preparation (decode + layout + prepare), JVM | S3 ≤ 40 ms, large30 ≤ 100, large60 ≤ 200, every one of 10 samples after 3 warm-ups | — | — | `TimelinePreparationBudgetTest` (`@TimingGate`, the serial `make timing` tier), `System.nanoTime`, same limits for every build type | 1b |
| `DayDrawing` build | median of 5 passes ≤ 1 ms per day and ≤ 5 ms per width for large60, ≤ 512 KiB per drawing at 360/411/841 dp | — | — | JVM `@TimingGate` test with the fake `TextMetrics` (serial `make timing`, 3 warm-up passes; the max is reported, never gated); Robolectric test with real `TextPaint` | 1c |
| Drawing cache | ≤ 4 MiB, ≤ 8 entries | — | — | Unit test | 1c |
| Invalidation (static/clock draws) | Scroll 0/0; minute 0/1; day boundary 0/2; check-off redraws day k only | — | — | Robolectric `TimelineProbe` test | 1d |
| First large60 timeline frame | ≤ 800 / ≤ 450 ms | ≤ 1500 / ≤ 800 ms | ≤ 800 ms (gate) | `timelineFirstFrame`: `TraceSectionMetric("cf.timeline.firstFrame")`, start into `cf.trip-page=large60`, 10 iterations | 1e |
| Scroll `frameDurationCpuMs` P50/P90/P99 | ≤ 3/5/8 ms (aim) | ≤ 6/10/16 ms | P50 trend; P90/P99 CPU band (gate, "Where gates apply") | `timelineScroll`: exactly 15 flings × 3 iterations on large60, `FrameTimingMetric` | 1e |
| Scroll `frameOverrunMs` P99 | ≤ 2 (hard) / ≤ 0 (aim) | ≤ 4 (hard) / ≤ 0 (aim) | report | Same run | 1e |
| Jank % (frames with overrun > 0 ÷ frames) | ≤ 3 / ≤ 1 | ≤ 5 / ≤ 3 | report | Same run, computed by `check-benchmarks.py` | 1e |
| Hitch ratio (Σ positive overrun ÷ scroll time) | ≤ 5 / ≤ 1 ms/s | ≤ 5 / ≤ 2 ms/s | report | Same run | 1e |
| Placeholder frames while scrolling | ≤ 2 / 0 | ≤ 6 / ≤ 2 | ≤ 6 per iteration (gate) | Trace counter `cf.timeline.placeholder` (`TraceSectionMetric`, count mode) | 1e |
| Timeline builds on the main thread | 0 | 0 | 0 (gate) | `cf.timeline.build` slices on the main thread (`TimelineTraceMetric`) | 1e |
| Main-thread frame time while scrolling, P50 / P99 | ≤ 2 / ≤ 4 ms (aim; `04 §9.1`: ≤ 4 ms in a frame where a day enters) | same | report | `TimelineTraceMetric`: `Choreographer#doFrame` slices on the main thread | 1e |
| RenderThread frame time while scrolling, P99 | ≤ 4 ms (aim, `04 §9.1`) | same | report | `TimelineTraceMetric`: `DrawFrame` slices on `RenderThread` | 1e |
| `TimelineTargets` composition per day, max | report | ≤ 1 ms (aim; above it, OQ-A46) | report | `cf.timeline.targets` section (`TraceSectionMetric`, max mode) | 1e |
| First-frame breakdown: decode, prepare, typeface load | report | report | report | async sections `cf.timeline.decode`, `cf.timeline.prepare`, `cf.timeline.typeface` (`TimelineTraceMetric`) | 1e |
| Jump to the last day | ≤ 6 flings | same | functional | `timelineLastDay` | 1e |
| Peak memory, large60 scroll | RSS anon ≤ 150 / ≤ 80 MiB | ≤ 150 / ≤ 100 MiB | ≤ 150 MiB | `MemoryUsageMetric(Mode.Max)` in `timelineScroll`; growth from first to last iteration is reported, > 40 MiB warns | 1e |
| Peak memory to Welcome | below RN | — | report | `MemoryUsageMetric` in `StartupBenchmark` | 13 |
| Download size | ≤ 8 MB; fail above 12 MB; no `.so` except the OQ-0a-22 allow-list | — | — | `bundletool build-apks --device-spec` (arm64, xxhdpi, en) + `get-size total`; `make size` | 0a (gate), 13 |
| `make quick` | < 60 s warm | — | — | `Scripts/measure-tiers.sh` → `build/tier-timings.json` | 0a |
| `make check` | < 5 min warm, with the `pixel` emulator running | — | — | Same | 0a, 13 |

Every perf run is recorded in `docs/perf-timeline.md` or `docs/perf-startup.md`: date, commit, device, OS, refresh rate, font scale, theme, build type, raw metrics and trace links (`04 §11`).

## Risks

1. **The timeline architecture misses a gate.** Phase 1e decides with data: a miss goes to the fix its evidence names (decode, prepare, typeface, build, cache or host; 1e Task 9). Fallbacks B′ (Picture), C (strips) and D (custom View) are bounded spikes with the same benchmarks, tried only on replay or scroll evidence (A12). On the emulator, scroll smoothness is gated only as a regression band; the device scroll rows need one reference and one low-end run before the first production upload, or the user's waiver (OQ-A45). The decision is recorded in `docs/perf-timeline.md`.
2. **No physical device yet.** Device gates stay open, and emulator numbers are not claims (iOS lesson: AppLaunch 2.09 s on the simulator, `13 §2.3`). Mitigation: functional gates and the scroll regression band on the emulator; ask for a reference and a low-end device (OQ-A22). The first production upload needs one run on each, or the user's waiver (OQ-A45).
3. **Server dependencies** (`14 §12`):
   - S1 `assetlinks.json` (`handle_all_urls`, `get_login_creds`);
   - S2 `android:apk-key-hash` origin;
   - S3 passkey management merge;
   - S4 delete routes without `verified`;
   - S5 production demo account;
   - S6 iOS AASA.

   Passkeys and App Links stay hidden or untestable end-to-end until then. Publishing assetlinks before the applicationId and signing key are final means redoing it (`14` risk 5).
4. **Play account-deletion policy:** the delete routes require a verified email (S4), so an unverified account cannot delete in-app. Track it in the Play checklist (P-items, `14 §10`).
5. **Configuration override mistakes affect every screen** (font cap, locale coercion, LTR, `14` risk 4). Mitigation: the 0e spike plus Robolectric locale tests (`ar-rEG`, `de-rDE`) and font-scale tests.
6. **Slow tiers.** iOS `make check` never met 5 min. Mitigation: measure in 0a, keep the cheapest layer, and fail review when a test needs more time.
7. **Worker sandbox.** It may not run Gradle daemons, adb or the emulator (`13` I15). Mitigation: prove `make quick` in the sandbox in 0a; otherwise the orchestrator runs those tiers.
8. **Disk blow-ups** (iOS: 150 GB of worktrees, 203 GB of simulators). Mitigation: CLAUDE.md cleanup rules, and `make du` after each phase.
9. **Robolectric on SDK 36 needs JDK 21,** and only JDK 17 is installed. Mitigation: foojay toolchain provisioning; fallback per OQ-A27.
10. **AGP 9 and baseline-profile plugin wiring** with perf-only sources is unproven. A spike in 0e covers it (OQ-A28).
11. **No render server on Android.** A main-thread stall freezes the overlay, where iOS keeps animating (OQ-A19). Mitigation: zero main-thread I/O enforced by StrictMode; a wall clock so stalls never stretch the timeline.
12. **Copied iOS artifacts drift** after `f391a1fa`. They are pinned; a re-pin is a deliberate task with regenerated output and review.
13. **Dutch at font scale 1.3 on 280–360 dp** may overflow. Mitigation: guards at small, nl and largeFont in every UI phase.
14. **The `native` applicationId segment** is a Java keyword. Phase 0a proves install and launch; fallback per OQ-A1.
15. **Stale baseline profiles** bring back JIT jank on the first scroll. Mitigation: `BaselineProfileMode.Require`; regenerate when PlanCore, the builder, replay or page structure change.
16. **Process-death policy** (always cold launch) looks unusual on Android (`14` risk 6). It is deliberate (iOS OQ 16); the wizard recovery covers the data-loss case.
17. **Health apps declaration wording** (`14` risk 7). Keep it consistent with no-health-data.
18. **The M3 role mapping is unusual:** M3 `primary` = `dawnText`. Designed code must read `CfTheme` only. Konsist forbids `MaterialTheme.colorScheme` outside `:core:designsystem` and its system-component wrappers.
19. **`dropShadow` fidelity** against CSS is unverified (`08 §14`). Mitigation: compare one card per theme against the board render before building more cards (Phase 2).

## Open questions for the user

Each has a default; the plan proceeds on the default and records the answer here.

| # | Question | Default | Affects |
|---|---|---|---|
| OQ-A1 | Final applicationId? (`14` OQ-G1) | `net.chronoflow.app.native` until the first Play upload; then `net.chronoflow.app` if RN never shipped on Play. If 0a finds the `native` segment rejected, use `net.chronoflow.app.android`. | 0a, 13 |
| OQ-A2 | minSdk 29 or 28? | 29 | 0a |
| OQ-A3 | Selected tab color: iOS ships `.tint(primary)` (indigo light, dawn dark; `AppTabs.swift:143-168`); the boards and dirNative say amber (`dawnText` + `systemSelected` indicator, `08` OQ 3). | iOS parity: icon and label `primary`, indicator `muted` | 6a |
| OQ-A4 | Tab icons | Material Symbols Rounded `space_dashboard`, `flight`, `settings`; outlined when unselected, filled when selected | 6a |
| OQ-A5 | Toast: M3 Snackbar or the designed floating toast? | Snackbar with inverse tokens, bottom; the refresh capsule stays designed at the top | 6a, 10a |
| OQ-A6 | Glass or blur anywhere? | No; opaque surfaces | 2 |
| OQ-A7 | Display P3 colors? | sRGB only; P3 values recorded in the report | 0b |
| OQ-A8 | Bedtime and time pickers with 5-minute steps (no native Compose wheel) | M3 `TimePicker` dial in a sheet, rounded to 5 min on Done (`08` OQ 11, `06` OQ 6) | 9e, 10b |
| OQ-A9 | `dayTitle` line height: tokens 18/28 vs iOS 18/26 | 18/26, as iOS | 0b |
| OQ-A10 | Ship Fraunces 500 cuts (unused on iOS)? | Yes, for parity; drop later if still unused | 0b |
| OQ-A11 | More-button glyph | `more_vert` | 6c |
| OQ-A12 | Android-only copy (table above) | Ship the proposed en/nl, marked "needs review" in `android-only.json` | 5b, 10c, 12c |
| OQ-A13 | `FLAG_SECURE` on recovery codes and 2FA setup? | Yes | 10c |
| OQ-A14 | 12 h refill and 20 min `setTimeoutAfter`? | Yes | 12b |
| OQ-A15 | Exact alarms? | No on API 31+. Yes on API 29–30, where `setExactAndAllowWhileIdle` needs no permission and inexact alarms have no delivery cap (review 2026-11-16) | 12b |
| OQ-A16 | ETags in v1? | No (iOS parity) | 4b |
| OQ-A17 | Background sync or flush? | No. ON_STOP only disables sync, as iOS (A7) | 4d, 4e |
| OQ-A18 | Sign-up credits: server `SIGNUP_CREDITS = 1` (`CreditLedger.php:16`) vs 3 on boards | Render server values; never hard-code | 9g, 11b |
| OQ-A19 | Must the overlay keep animating through a main-thread stall, as on iOS? (`01` OQ-A13) | No: zero main-thread I/O plus frame metrics; a RenderThread `AnimatedVectorDrawable` moon is a later option | 3a |
| OQ-A20 | Android-only startup budgets (warm ≤ 200 ms, `onCreate` → frame 0 ≤ 50 ms, low-end ≤ 800 ms) | Proposals; confirm after the first device run | 3a, 13 |
| OQ-A21 | Passkeys before server S1–S3 | Hidden behind `PasskeyCapability` | 5b, 10d |
| OQ-A22 | Reference and low-end devices | Pixel 8 (or 8a) and Galaxy A15/A16 or Pixel 4a class; device gates stay open until attached | 1e, 13, 14 |
| OQ-A23 | Splash appearance on API 29–30 | Follows the system appearance | 3a |
| OQ-A24 | Language override mechanism | `LocaleManager` on 33+, the `device-config` file below 33, no AppCompat; spike in 0e; fallback AppCompat `setApplicationLocales`, and a font cap through the root `LocalDensity` | 0e |
| OQ-A25 | With the `system` choice, follow a later phone language change? (`10` OQ-L5) | Yes | 10a |
| OQ-A26 | Module name for the PlanCore port | `:core:plan` (alternative `:core:timeline`) | 1a |
| OQ-A27 | JDK 21 for Robolectric | foojay auto-provisioning; fallback: the user installs `zulu@21`, or Robolectric `sdk = 34` on JDK 17 | 0a |
| OQ-A28 | Perf variant wiring | The plugin's `benchmarkRelease`/`nonMinifiedRelease` plus `src/perf`; fallback a hand-written `benchmark` build type | 0e |
| OQ-A29 | Instrumentation `timeout_msec` support (`13` OQ-T6) | Verify in 0e; fallback per-test timeouts plus run-tier caps | 0e |
| OQ-A30 | Invalidation guard in `make quick` (`13` OQ-T8) | Yes | 1d |
| OQ-A31 | Remote and pushing | No remote; never push unless asked | all |
| OQ-A32 | Policy URLs (`05` OQ A19) | `https://chronoflow.net/terms` and `/privacy`, in a Custom Tab | 5d, 8 |
| OQ-A33 | Live Updates or an ongoing notification for Live Activity? | No | 12b |
| OQ-A34 | ChromeOS and desktop windowing | Not targeted; size classes only | 14 |
| OQ-A35 | Gradle 9.9.1 | Yes; 9.6.0 if AGP 9.5.1 rejects it | 0a |
| OQ-A36 | Wizard behavior questions (`06` OQ 1–14) | `06` defaults: board pct; no back on the first screen; Close only; separate date and time dialogs; minute precision; inline TimePicker; pickers follow the phone 12/24 h while displayed values follow the app; build the search states; haptics behind an interface; Nav3 steps; Keystore + DataStore guest secret; `d MMM yyyy`; room "From" disabled until arrival | 9a–9h |
| OQ-A37 | Settings column on wide windows | 560 dp (`07` OQ-11) | 10a |
| OQ-A38 | Debug key in production `assetlinks.json`? | Never | 13 |
| OQ-A39 | AAB language splits | Off (in-app language switch) | 13 |
| OQ-A40 | iOS deferred "keep pending edits on login upsert" | Already done at the pin (`SessionAccountDatabase.swift:29–48`, `StoreApplication.swift:51–60`); 4d ports it as it is, with store-level `LoginUpsertTests`. Tell iOS only that its deferred-fix checkbox (`plan.md:2327`) can be ticked | 4d |
| OQ-A41 | Play Data safety answers, shared with the undecided iOS privacy label | `14 §9.2` answers | 13 |
| OQ-A42 | Material chrome has no Android boards (`08` OQ 1) | Build per the Material mapping, capture each component light/dark en/nl in Phase 14, and ask the designer to confirm | 2, 14 |
| OQ-A43 | Adaptive icon horizon crop (`08` OQ 15) | Ship `_design/canvas/icon/android/*` unchanged | 0b |
| OQ-A44 | What does tapping a Today row open? (7 OQ-7-1) | The step sheet over Today, as iOS (`AppTabs.swift:443–448`) and the `TodayRowStates` board; only the hero card opens the trip page, without a block | 7 |
| OQ-A45 | Release while the timeline scroll rows are still "open: device only"? | No: the pre-upload checklist in `docs/play.md` needs one passing `DEVICE_CLASS=reference make perf-timeline` (120 Hz) and one passing `DEVICE_CLASS=lowEnd make perf-timeline` before the first production upload, or the user's waiver recorded here. Phases 13 and 14 may still close with device rows open | 13, 14 |
| OQ-A46 | Compose `TimelineTargets` (20–40 semantics nodes per day) always, or only while accessibility is on? (research `04` OQ-T25) | Always: UiAutomator, Macrobenchmark, Switch Access and Voice Access need them. `cf.timeline.targets` excludes bodies deferred by pausable prefetch, so `timelineTargetsMaxMs` is a lower bound; a value below 1 ms cannot prove the aim. Inspect full composition traces before changing behavior. If a low-end run shows `timelineTargetsMaxMs` above 1 ms, compose them only while `AccessibilityManager.isEnabled` (observed as state; UiAutomation counts as enabled) | 1d, 1e, 13 |
| OQ-A47 | App Startup initializers other than ProcessLifecycle and ProfileInstaller (for example EmojiCompat, merged by Compose text) | Remove them (AGENTS "Performance"). Compose then draws emoji with the phone's own font, so emoji newer than that font show as boxes on old phones. 3a records the inventory and the measured saving | 3a, 13 |
| OQ-A48 | The JVM preparation budgets (S3 40 ms, large30 100 ms, large60 200 ms) sit 5 to 25 times above the first measurements (means 3, 4.5, 7 ms). Tighten them? | No: keep the iOS limits (iOS `plan.md:1734`); measured numbers live in `docs/perf-timeline.md` | 1b, 1e |
| OQ-A49 | `tools/fixtures/extract_mock_seed.py` has no Python unit test | Covered by `make mock-seed-check` (in `generators-check`) and `MockTripTemplatesTest`; 4c adds a Python test when `MockAppSeed` lands | 1b, 4c |
| OQ-A50 | Dot period: plan 1c said an 8 × 8 cell; iOS `dotStep` and the board use 7 dp (OQ-1c-1). | 7 dp (the plan text is amended). Affects 1c. |
| OQ-A51 | Hatch phase: iOS centres the 2 dp stripe on the line through the tile corner; the board runs it from 0 to 2 dp (OQ-1c-2). | The board (a 1 dp difference). Affects 1c. |
| OQ-A52 | Pre-composite tolerance (OQ-1c-9). Measured on Robolectric: shapes, patterns, glyphs and outlines differ from a `saveLayerAlpha` reference by at most 3/255 (double 8-bit rounding, up to 25% of pixels on hatch stripes); with text, glyph edge pixels differ by up to 16/255 on about 2-3% of pixels because Skia's glyph contrast pre-blend depends on the text colour. | Gate 3/255 everywhere outside the TEXT line boxes; inside them 20/255, with at most 5% of the tile area beyond 3/255; not the plan's 1/255. The marked case uses core priority, so the core bar is under group alpha. Affects 1c. |
| OQ-A53 | The drawing carries no page background: iOS fills `background` inside its PDF (OQ-1c-11). | Omit it; the list paints the background. Affects 1c, 1d. |
| OQ-A54 | `CLIP_TILE` only when a tile's content can leave its quadratic shape, instead of always clipping as iOS does (OQ-1c-17). | Conditional, by the exact square-root test; pixel-identical. Affects 1c. |
| OQ-A55 | A text run narrower than "…" is dropped; iOS clips the full line to its rect (OQ-1c-20). | Drop the run (at most a one-glyph sliver differs). Affects 1c. |
| OQ-A56 | `DayDrawing`'s constructor is public (the brief said internal) so the `saveLayerAlpha` reference test can slice an op list from `:feature:trip` tests. | Public, documented as builder and test use only. Affects 1c. |
| OQ-A57 | Sheet container: mapping says popover; pinned iOS and ItemDetail use card (Phase 2 OQ-2-2) | Use card; preserve the 38 dp top corners and 640 dp maximum width | 2 |
| OQ-A58 | Drag handle: mapping says border; iOS uses its system grabber (OQ-2-3) | Use the default Material DragHandle, with the mapped mutedForeground role and Material alpha, plus a 48 dp minimum interactive target | 2, 14 |
| OQ-A59 | Snackbar dawn action has only 1.61:1 contrast on foreground in dark (OQ-2-5) | Use background for the Snackbar action; retain dawn for inversePrimary in toM3 | 2 |
| OQ-A60 | Skeleton bundle pulses while pinned iOS is static (OQ-2-6) | Static skeletons in both motion modes; the spinner remains animated | 2 |
| OQ-A61 | The planned offline banner has no in-scope board (OQ-2-14) | CfBanner is the auth error banner; info uses CfNotice; the feature owning an offline board builds that state | 2, 4d |
| OQ-A62 | Lucide version and shapes differ between the boards and newer releases (OQ-2-15) | Extend the existing generator with board bundle and inline SVG sources, pinned to the boards' 0.511.0 shapes | 2 |
| OQ-A63 | Dawn fills on light surfaces fail 3:1 (OQ-2-7) | Keep the board's dawn fill on switch, progress and refresh; retain the explicit contrast-gap tests; use dawnText for text and selected checks | 2, 14 |
| OQ-A64 | Native Material calendar cells can round one physical pixel below 48 dp, while segments share borders and clock-dial hit rectangles overlap | Preserve Material controls. Allow one physical pixel of native-calendar rounding; keep native window placement and circular targets under Material; verify native rendering on the emulator and retain whole-display system-chrome review in Phase 14; keep strict guards for designed controls | 2, 14 |
| OQ-A65 | The brief combines a 32 dp Small button with 20 dp text and 8 dp vertical padding, which makes 36 dp | Follow the bundle's 32 dp Small visual height with 6 dp vertical padding and a separate 48 dp target; other button sizes retain 8 dp padding and text can wrap | 2 |
| OQ-A66 | Phase 2 cold start is 95.8 ms: +8.0% against 0e 88.7 ms (Partial UseIfAvailable), +0.8% against M1 95.0 ms (Partial Require). Which baseline should future theme comparisons use? | Use M1's 95.0 ms with Partial(Require) as the like-for-like reference (user, Phase 2 review round 1). Phase 2's 95.8 ms is +0.8% and passes the 5% limit. Retain the older 0e result as history; Phase 3a measures the real launch overlay separately | 2, 3a, 13 |
| OQ-A67 | Who sets system-bar icon appearance after the Phase 3a hand-off? Research 08 row 24 says themed surfaces follow the theme; review currently observes white status-bar icons on the light background. | Resolved by Phase 3a Task 7b: light theme → dark status-bar icons, dark theme → light icons through enableEdgeToEdge; sky screens retain light icons. Review round 1: CfRoot paints CfTheme.colors.background behind content (generated CfLightColors/CfDarkColors.background, from theme.css --background and Trips.dc.html:24); the navy window background remains for splash handover | 3a |
| OQ-A69 | (3a-1) What does the app show after the opening until 3b lands? | Answered by Phase 3b: readiness schedules the ending, removes the overlay on its completion frame and reveals the route placeholder; the opening no longer holds forever. | 3a, 3b |
| OQ-A70 | (3a-2) Activity recreated within the process during or after the splash. | Answered by Phase 3b: a configuration recreation resumes a running overlay with the same t0, timeline and coordinator and never holds the system splash; a new warm activity gets no overlay and a 300 ms system-splash fade. | 3a, 3b |
| OQ-A71 | (3a-3) Navigation-bar icon color on the sky (`05` OQ A9). | From the sky's bottom per captured appearance: light → dark icons (`SystemBarStyle.light`), dark → light icons; contrast enforcement off; verify on the Task 11 recording. | 3a |
| OQ-A72 | (3a-4) Android-only debug hook `cf.holdSplash` for the parity screenshot. | Add it as a `TestSeed` field, debug only, listed in "Android spec: seeds and test hooks"; the in-process test releases it through `LaunchController.splashHeld`, so no exported receiver exists; release has no `TestSeed` (`ReleaseReadinessTest`). | 3a |
| OQ-A73 | (3a-5) Wordmark vector source. | Generated once from the Fraunces 400 opsz32 TTF with HarfBuzz `hb-view` 15.1.0 (kerning) plus CoreText-style tracking; committed with a hash manifest. Fallback if `hb-view` output proves unusable: build the path at runtime off the main thread with `Paint.getTextPath` before 1000 ms. | 3a |
| OQ-A74 | (3a-6) Parity tolerances. | Centres ±1 px; centre pixel ≤ 2/255; in the 192 dp circle, flat pixels ≥ 99.5% within 4/255 and none > 16/255; edge pixels (3 × 3 channel span > 32 in either screenshot) unbounded per pixel; distance-weighted ink centroids within 0.5 px. | 3a |
| OQ-A75 | (3a-7) `TraceSectionMetric` and async sections. | Async `first-commit`; fallback a `TraceMetric` query, recorded. | 3a |
| OQ-A76 | (3a-8) Moon decode on the main thread during the first composition. | Keep it in composition, traced as `cf.launch.moonDecode`. If it takes > 16 ms on the `pixel` emulator, start the decode on `Dispatchers.IO` at the top of `onCreate` and join it in the first composition; record the numbers. | 3a |
| OQ-A77 | (3a-9) Sky banding in dark. | Compose gradient draw; if the Task 11 frames show bands, draw the sky through `drawIntoCanvas` with a native `Paint` that has `isDither = true` and the same shader. | 3a |
| OQ-A78 | (3a-10) API 29–30 parity (compat splash). | Not in `make check`; run `LaunchFrameZeroTest` on the `api29` T2 AVD on request; accept ± a few px until then. | 3a |
| OQ-A79 | (3a-11) Does the status text scale with font scale (affects `LaunchStatusFitTest`)? | Yes, in sp up to the 1.3 cap, like iOS Dynamic Type up to xxLarge; 3b confirms when it draws the line. | 3a |
| OQ-A80 | (3a-12) Theme names and files: plan says `Theme.App.Starting` / `Theme.App` in `values{,-night}/themes.xml`. | The tree's `Theme.ChronoFlow` stays; add `Theme.ChronoFlow.Starting` in `values/themes.xml` (+ `-v33`); the existing `values-night/themes.xml` repeats the `Theme.ChronoFlow` items (a style in `values-night` replaces the one in `values`); night colors and the night moon come from the `-night` color and drawable resources. | 3a |
| OQ-A81 | (3a-13) Instrumented tests share one process, but the overlay is once per process. | Debug-checked `AppGraph.resetLaunchForTest()` gives each launch-profile `@Smoke` test a fresh `LaunchController`; traces are not re-armed. Later phases may reuse the hook. | 3a |
| OQ-A82 | (3a-14) `TimelinePresentedMarker` uses `PRESENTED_MARKER_SIZE_DP.dp`, which the extended dp rule (Task 8b) would flag. | Rewrite it as a 1 px layout node, no exemption; `make perf-timeline` once to prove the benchmark still finds it. Fallback: one documented file exemption in the rule. | 3a |
| OQ-A83 | Launch sun on windows shorter than 667 dp | Keep iOS geometry unchanged; no sun-visibility rule below 667 dp. Shorter windows retain moon/wordmark containment and finite-value guards | 3a |
| OQ-A84 | (3b-1) Signed-out ending before Welcome (3c) exists. | Interim: the welcome exit's container row only (opacity 1 → 0 over W+700 → W+950, ease-out; rm → the ready exit), then the Welcome placeholder. Replaced in 3c by `withWelcomeExit` and the real Welcome screen. | 3b |
| OQ-A85 | (3b-2) Plan says the stub is "driven by `cf.readyAt` and a stub outcome extra". iOS drives the stub with `-cf.session/-cf.gate/-cf.stored/-cf.readyDelay` (seconds) and uses `-cf.readyAt` (ms) only for `-cf.mode slow`. | Port iOS exactly: the four stub extras plus `cf.mode`/`cf.readyAt` with iOS meanings and units. | 3b |
| OQ-A86 | (3b-3) Port `cf.mode` (fixed endings without readiness)? | Yes, `ready`, `slow` and (from 3c) `welcome`, debug and perf only. iOS keeps `-cf.mode ready` in Release for the device proof; Android uses the perf variant for that. | 3b |
| OQ-A87 | (3b-4) `cf.mainThreadStall` in which variants? | Debug and perf; never release. | 3b |
| OQ-A88 | (3b-5) Activity recreated (locale, font scale) while the overlay runs (OQ-3a-2). | Resume at the wall-clock position with the same `t0`, timeline and coordinator; no keep condition; no restart. If the status raster was prepared for another locale or font scale, prepare it again (the line is hidden until it is ready). | 3b |
| OQ-A89 | (3b-6) Activity finished during the splash. | Complete the launch at once (iOS `sceneDidDisconnect`); a later warm start shows the known route, or runs a headless coordinator (rm semantics) until the route is known. | 3b |
| OQ-A90 | (3b-7) How to detect a process-death restore. | A per-process token (`pid:startElapsedRealtime`) in the saved state; a mismatch or absence with a non-null bundle is a restore; `super.onCreate(null)`. | 3b |
| OQ-A91 | (3b-8) Route type before 4e. | `LaunchRoute` (Welcome, Today, Verify, Terms, Walkthrough) in `:feature:launch`; 4e maps it onto `Route`. | 3b |
| OQ-A92 | (3b-9) `PendingLaunchIntent` in `LaunchState`. | Omitted until 4e, which adds the field and the assertion in `endingIsChosenOnceAndIntentNeverRoutes`. | 3b |
| OQ-A93 | (3b-10) Placeholder look (no board). | Debug: the catalog text "Debug route: %@" (`app_debug_route_x`, drawn by `app/src/debug` `DebugRouteLabel`, OQ-0c-4) with id `route.<name>` (iOS OQ 8); release/perf: no text. Background: `background` token for every placeholder. From 3c, Welcome has its own real CfSky screen. | 3b |
| OQ-A94 | (3b-11) System bars after the hand-off (the spec only says "light until the hand-off ends"). | Welcome route: light icons (sky). Other routes: icons follow the resolved app theme (`SystemBarStyle.light` in light, `dark` in dark), applied once through `enableEdgeToEdge` at `handOffComplete`. Status icons follow this rule; navigation icons retain OQ-A71. 3c amendment (OQ-A109): Welcome status icons remain light; navigation icons are dark in light theme and light in dark theme, through the existing sky-bottom mechanism. | 3b |
| OQ-A95 | (3b-12) `CF_UI_TEST_PROFILE=screen` "skips the splash and readiness", but something must pick the route before 4e. | No overlay; a headless coordinator (rm semantics) picks the route from the stub or signed-out default; 4e's session seeds take over. | 3b |
| OQ-A96 | (3b-13) Who emits the `session` trace? | 4e (with `StoredDataReadiness`); 3b reserves cookie 5. | 3b |
| OQ-A97 | (3b-14) Process-death proof needs a second process. | `ProcessDeathLaunchTest` in `:benchmark` on the perf variant (UiAutomator, `am kill`, explicit `am start -W -n <pkg>/net.chronoflow.app.MainActivity` relaunch), run with `make perf`; the restore branch itself is proven by `ProcessDeathRestoreTest` on Robolectric. | 3b |
| OQ-A98 | (3b-15) plan.md lists a "failure" readiness event; iOS has none. | No `Failure` event; failures resolve through the 8 s rule (unknown → signed out, signed in → forced). 4e may revisit if its session source needs one. | 3b |
| OQ-A99 | (3b-16) plan.md's stall test reads positions "through a debug `LaunchProbe`"; 3a's `compositionObserver` callback only counts compositions. | Read from the debug `controller.frameLog` ring buffers; leave `compositionObserver` unchanged. | 3b |
| OQ-A100 | (3b-17) Diagnostic `cf.mode` `ready`/`slow`: which route after removal? | None, as iOS (`SceneDelegate.swift:161–171` navigates only for `welcome`): the content stays empty and `reportFullyDrawn()` is not called. | 3b |
| OQ-A101 | (3c-1) The plan says "Welcome copy card max 560 dp, centred" on windows ≥ 600 dp. Only the card, or the whole column? | **Only the card**, as the plan writes it. The heading, the buttons, `contentBounds` and the moon fallback keep the iOS formula unchanged (iOS has no cap). No wide board exists. If the user wants a capped column, `WelcomeGeometry` gains the cap in one place. | 3c |
| OQ-A102 | (3c-2) What do Plan my trip and Sign in do before 11a and 5a? | Debug: the iOS route seam (`route.guestWizard`, `route.signIn`, "Debug route: %@"); back returns to Welcome. Release and perf: they do nothing (iOS OQ 8). | 3c |
| OQ-A103 | (3c-3) How does TalkBack focus reach Welcome's first node after teardown (iOS `.screenChanged`)? Compose has no public way to request accessibility focus. | Rely on the overlay pane disappearing and `hideFromAccessibility` lifting, and check once by hand. If focus does not land on the wordmark, add a `FocusRequester` on the wordmark used only while touch exploration is on, and record it. | 3c |
| OQ-A104 | (3c-4) If Welcome composes before the fonts are warm, it loads them on the main thread under the overlay (StrictMode `penaltyDeath`). | Compose Welcome after the Phase 2 warm-up completes, or at 1500 ms, whichever comes first. The measured anchor still arrives before W − 50 in normal runs; otherwise the fallback is used. | 3c |
| OQ-A105 | (3c-5) The plan reads both moments "through the `intro` trace", but `intro` spans 2300–3200, and interactive (2550) is the end of `exit`. | `welcomeInteractiveMs = exit.end − first-commit.end`; `introLastItemMs = intro.end − first-commit.end`, through one `TraceMetric`. Emulator tolerance: + 1 frame at the measured refresh rate. | 3c |
| OQ-A106 | (3c-6) The buttons are clickable from 2550, while they still fade in until 3150/3200. | Yes (iOS parity: interactive = the overlay is gone). | 3c |
| OQ-A107 | (3c-7) The intro's time source: the frame time or `clock.now`? | `clock.now`, sampled in each `withFrameNanos` callback (the same `LaunchClock` as the overlay; they differ by less than 1 frame). | 3c |
| OQ-A108 | (3c-8) Five Welcome contrast pairs fail audits at the board values (iOS `plan.md:845–849`). | Keep the board colors and record them as accepted exceptions (research `05` OQ A10, l.1041; not plan.md's OQ-A10, which is about Fraunces 500). | 3c |
| OQ-A109 | (3c-9) Navigation-bar icon color on Welcome (OQ-3b-11 said light icons for Welcome). | Status bar: light icons. Navigation bar: from the sky bottom, so dark icons in the light theme (`skyHorizon` L 0.75) and light icons in the dark theme. This amends OQ-3b-11. | 3c |
| OQ-A110 | (3c-10) The plan's task text says the overlay "reads `MoonAnchorStore` at draw time". iOS instead sends the anchor to the reducer, which rebuilds the timeline before W. | Follow iOS: store → `LaunchEvent.MoonAnchor` → `CommitExit` → a rebuilt `handOff` sampler, which the overlay reads at draw time. This keeps the W − 50 fallback deadline and the "no changes after W" rule. | 3c |
| OQ-A111 | (3c-11) The overlay uses the captured system appearance, and Welcome uses `cf.settings.theme`. If they differ, the dissolve crosses themes. | Accept it (it only happens when the app theme overrides the system); add no extra transition. | 3c |
| OQ-A112 | (3c-12) Is the diagnostic `cf.mode=welcome` needed on Android? | Yes, in debug and perf: the board captures and the frozen-frame checks use it, as iOS does. Release keeps `LaunchDiagnostics.None`. | 3c |
| OQ-A113 | (4a-1) The plan writes `transaction { epoch.whileValid { permit?.whileValid { body() } } }`, which releases the locks before COMMIT. iOS holds them through COMMIT (`AccountDatabase.swift:124–137`, `SyncEpoch.swift:33`). | Follow iOS: `epoch.whileValid { permit?.whileValid { transaction { body() } } }`; fix A5 and the 4a bullet (Task 5). | 4a |
| OQ-A114 | (4a-2) iOS DDL says `DEFAULT true/false`; on API 29 (SQLite 3.22) these become the strings `'true'`/`'false'`. | `DEFAULT 1`/`DEFAULT 0`; the parity test normalizes. | 4a |
| OQ-A115 | (4a-3) The plan says the ported tests run on "in-memory JDBC"; WAL, sidecars, reopen and the corrupt-file cases need a file. | File-backed `JdbcSqliteDriver` in a temporary directory, plus Robolectric native-SQLite tests for the Android factory; fix the wording (Task 10). | 4a |
| OQ-A116 | (4a-4) The plan says reads are `asFlow().mapToList(io).conflate()`, which cannot end streams on `close()`, count observers or fetch across tables. | `AccountDatabase.observe(tables…)` with SQLDelight listeners, conflated, fetched on io; fix the wording (Task 5). | 4a |
| OQ-A117 | (4a-5) Plain `read`s are not transacted; a multi-statement read may see two commits. | Keep them untransacted, like iOS `pool.read` for single queries; a later phase wraps a multi-query snapshot in `transactionWithResult`. | 4a |
| OQ-A118 | (4a-6) Reminder storage: iOS keeps the ledger as settings section `reminderLedger` with `sendAtMs: Double`; A5 asks for local tables. | The tables in Task 3 (`send_at`/`send_at_ms` INTEGER ms, `account_id` nullable, `request_code UNIQUE`); 12b may edit them in place before the first release. | 4a |
| OQ-A119 | (4a-7) The plan calls the epoch `SessionEpoch`; iOS calls it `SyncEpoch`. | `SyncEpoch`; fix the bullet (Task 8). | 4a |
| OQ-A120 | (4a-8) Port `NoHealthDataMigration` (iOS v2)? | No: Android starts at v1 with no health data stored; `user_version` stays 1. | 4a |
| OQ-A121 | (4a-9) Keystore key with `setUnlockedDeviceRequired(true)`? It blocks session reads while the device is locked (reminder taps, background sync). | No. iOS uses `WhenUnlockedThisDeviceOnly`; Android keeps background access and relies on device-bound, non-exportable keys. Revisit before release. | 4a |
| OQ-A122 | (4a-10) A corrupt `device_prefs` file: reset or fail? A reset loses `api.deviceId`, so the device gets a new id once. | `ReplaceFileCorruptionHandler { emptyPreferences() }`. A corrupt **value** still throws (D22). | 4a |
| OQ-A123 | (4a-11) The plan's 4a "Ported tests" covers all of `LocalStoreTests`, but the plan and the 1a/4d/4e/9b briefs put records, repositories, `AccountModel`, the wiper and the session database elsewhere. | 4a ports the cases for what it ships. Repository and `AccountModel` cases → 4d; draft halves → 9b; wiper, session-database and readiness cases → 4e (already in its brief). Recorded in the plan (Task 10). | 4a |
| OQ-A124 | (4a-12) iOS types JSON columns as `JSONDocument` and validates on read and write. 4d and 9b expect `String` columns. | `String` columns; `JSONDocument.requireValid` on every save; no re-validation on fetch (the app is the only writer, and plan JSON is decoded by its reader anyway). | 4a |
| OQ-A125 | (4a-13) `JdbcAccountDriverFactory` sits in `:core:database/src/test`, so 4d/4e tests in `:core:data` cannot use it. | 4d moves it to a `testFixtures` source set or a small `:core:database-testing` module, whichever AGP 9 supports without disabling built-in Kotlin. | 4a |
| OQ-A126 | (4a-14) iOS OQ 26: excluding everything from backup loses unsent outbox rows on device restore. | Keep excluding everything (A5); revisit before shipping. | 4a |
| OQ-A127 | (4a-15) `SessionMutationPermit.invalidate()` is public (iOS: internal to AppCore), because 4e's issuer lives in `:core:sync`. | Public, with KDoc; the 4e reviewer checks that only `SessionController` calls it. | 4a |
| OQ-A128 | (4a-16) `plan_versions`/`check_marks` mapping needs 1a's `PlanVersionRecord` (`detail: ByteArray`) and `CheckMarkRecord`, and those 1a records are already landed. | 4a maps only the four records; 4d adds the plan and check-mark mapping, storing `detail` as strict UTF-8 text. | 4a |
| OQ-A129 | (4a-17) iOS `DeviceIdentityStore` shares one in-flight task, so concurrent callers see the same error; a `Mutex` makes each waiting caller retry in turn. | `Mutex`; the observable contract (one write, no caching of a failure) is the same. | 4a |
| OQ-A130 | (4a-18) Should `:core:model` stay free of `kotlinx.coroutines`? 4d needs `SyncEpoch.check()` to see coroutine cancellation. | Keep the existing `api(libs.kotlinx.coroutines.core)` in `:core:model`; Konsist rule 2 forbids only `android.*` and Compose there. | 4a |
| OQ-A131 | DataStore 1.3.0 bundles `libdatastore_shared_counter.so`, but all app stores are single-process. Exclude it? | Exclude `**/libdatastore_shared_counter.so` from app JNI packaging while stores remain single-process. Konsist forbids `MultiProcessDataStoreFactory`, `createMultiProcessCoordinator` and `MultiProcessCoordinator`; introducing multi-process storage requires revisiting the exclusion before using that factory. Keep OQ-0a-22's native-library allow-list unchanged. | 4a |
| OQ-A132 | (4b-5) `DataSource.send` needs `OutboxChange` in 4b, but 0d's hand-off gives `OutboxChange`/`CheckOffChange` to 4d. | 4b adds the in-memory types (only `ProfileChange` is `@Serializable`, because it is a request body); 4d adds the storage codec (including `@Serializable` on `CheckOffChange`, status omitted when null, 4d OQ-4d-3) and the outbox round-trip test. | 4b |
| OQ-A133 | (4b-1) Where do `TripWizardErrors` and `SettingsPatchValidation` live (iOS: `DataAccess`)? | `:core:model` package `api`, so the mock, auth and wizard modules use them without OkHttp. | 4b |
| OQ-A134 | (4b-2) iOS sends a `..` path segment as `%2E%2E`; OkHttp resolves `.`/`..` and their `%2E` forms as dot segments. | Refuse `.` and `..` segments with `Server(null)` before sending (ids are UUIDs in practice); otherwise encode exactly as iOS, `.` → `%2E`. | 4b |
| OQ-A135 | (4b-9) `DataSourceFactory.testing` (iOS forces mock under tests). | Port the parameter; 4e passes `false` in the app (instrumented tests run the debug build, which defaults to mock). | 4b |
| OQ-A136 | (4b-7) A 401 or 403 without a decodable envelope maps to `Server(status)` on iOS, so it never wipes or gates. | Keep iOS parity; 4d decides retry/refusal from the status. | 4b |
| OQ-A137 | (4b-11) `Retry-After` header grammar: Swift `Double()` also accepts exponents. | Digits with an optional fraction, then RFC 1123 date, then 60 s. | 4b |
| OQ-A138 | (4b-3) OkHttp retries some failed connections silently; iOS makes exactly one attempt ("no automatic retry"). | `retryOnConnectionFailure(false)`. | 4b |
| OQ-A139 | (4b-4) A bearer token with characters OkHttp rejects in a header. | `Unauthorized(401)` without sending. | 4b |
| OQ-A140 | (4b-10) A 204 that carries a body: iOS gives `Server(204)`; OkHttp may raise a protocol error first. OkHttp also uses `ProtocolException` for a body cut short by a dropped connection. | A `ProtocolException` raised before a `Response` exists → `Server(null)`; any `IOException` while reading the body, `ProtocolException` included → `Offline` (iOS: transport failure → offline). The 204 test accepts any `Server`. | 4b |
| OQ-A141 | (4b-12) User-Agent: OkHttp sends `okhttp/5.5.1`. | Keep the default; it carries no personal data. | 4b |
| OQ-A142 | (4b-14) Plan A6 says per-call `withTimeout`; iOS races the request against an injectable `sleep`. | Race against an injectable `ApiDeadline` (default `delay`): same semantics (the deadline cancels the OkHttp call and yields `Offline`; the caller's cancellation stays cancellation), and tests never wait on wall time. | 4b |
| OQ-A143 | (4b-15) plan.md 4b "ApiClient" lists "the device id" among the request headers, but `03 §2`, iOS `APIClient.swift:68-76` and the web API (no controller reads such a header at `e36368ec`) have no device-id header; `device_id` is a body key of login, passkey login and register. | No device-id header (iOS parity); `device_id` only in those bodies. `headersAreExact` asserts no header name contains `device`. | 4b |
| OQ-A144 | (4b-8) `DataSourceParityTests` compare mock and API. | Port all of it in 4c, which has the mock; 4b ports only API-only cases. | 4b |
| OQ-A145 | (4b-6) Can a release build be pointed at another server or the mock? | No: `release` is fixed to `api` at `https://api.chronoflow.net/v1` (A6); only debug reads `-Pcf.dataSource` / `-Pcf.apiBaseUrl`. | 4b |
| OQ-A146 | (4b-13) Which data source do the perf variants (`benchmarkRelease`, `nonMinifiedRelease`) use? A6 says only "release is always api". | `mock`, fixed: they carry `:core:data-mock` and measure seeded data (4c OQ-4c-4), and iOS's Perf configuration inherits `CHRONOFLOW_DATA_SOURCE = mock` (`Config/Base.xcconfig`). Not overridable. | 4b |
| OQ-A147 | Can an injected OkHttp authenticator or proxy authenticator retry with credentials? | Clear both to `Authenticator.NONE`, matching iOS's nil credential storage. A public 401 makes exactly one unauthenticated request even with an injected authenticator; the injected client remains unchanged. | 4b |
| OQ-A148 | Does removing OkHttp 5.5.1's `PlatformInitializer` require manual context initialization? | Remove its manifest metadata under A21/OQ-A47. The cached Android sources show it stores the application context and forces `Platform.get()`; context is used only by `AssetPublicSuffixList`, reached by cookie parsing and `HttpUrl.topPrivateDomain()`. Our client uses `NO_COOKIES` and neither API, so no explicit initialization is needed. Android TLS, trust managers, logging and cleartext policy still initialize lazily during the first client build on IO. Robolectric proves construction on Android without the initializer or context. Before adding a context-dependent API, provide `OkHttp.initialize(applicationContext)` at that same lazy IO boundary; never in `Application.onCreate`. | 4b |
| OQ-A149 | Can OkHttp retry a 503 response despite `retryOnConnectionFailure(false)`? | Strip `Retry-After` only from 503 responses with the sole internal network interceptor, installed after clearing injected interceptors. OkHttp 5.5.1 retries 503 with `Retry-After: 0` independently of the connection-retry flag; removing that header preserves the original `Server(503)` and one request. Leave 429 headers untouched for throttling. | 4b |
| OQ-A150 | Stale pooled connections after idle, sleep or network changes? | Keep a dedicated `ConnectionPool(5, 300, SECONDS)`, the default five idle connections and 300 s keep-alive, below Cloudflare's 400 s client-side limit for HTTP/1.1 and HTTP/2 (enabled by default). The real tier is Cloudflare → nginx → Caddy/FrankenPHP, with no Node tier. OkHttp measures pool idle with `System.nanoTime()` (awake time), so a short cap cannot cover deep sleep. 4e awaits the suspending `ApiClient.evictIdleConnections()` as ON_START step 0 to cover sleep and network changes while backgrounded. It reads the volatile already-built client reference, does nothing if unbuilt, and never takes the initialization Mutex. It closes idle sockets inside `withContext(io)`: TLS close can send `close_notify` and must never run on main (review round 3). Keep retries off (OQ-A138/A149); the user may revisit. | 4b, 4e |
| OQ-A151 | May the debug/perf mock main sources reuse 1b’s fixture helpers despite `testingNotInMain`? | Allow only `Fixtures`, `Template` and `LargeTimelineFixtures` in `MockAppSeed.kt`, `Template` in `MockTripFixtures.kt`, and `MockTripTemplates`/`Template` in `MockTripGeneration.kt`, within `core/data-mock/src/main`. Keep every other existing boundary and prove both allowed and rejected imports with controls. | 4c |
| OQ-A152 | (4c-3) How does ordinary mock latency differ from seeded runs? | Follow iOS: clamp the default random delay to 0.3–0.8 s; 4e supplies `MockLatency.exact(CF_MOCK_DELAY)` for seeded runs (default 0). JVM tests inject recording sleep or virtual time. | 4c, 4e |
| OQ-A153 | (4c-5) Does MockMethod have 26 cases as research 02 says? | No: iOS at the pin and landed DataSource each have 25 methods. ApiClient’s lifecycle eviction hook adds no mock case. | 4c |
| OQ-A154 | (4c-12) How are API 29 JDK calls enforced in the JVM mock module? | Review the ART API surface and grep for newer JDK calls; no desugaring or Animal Sniffer dependency. Keep the existing standalone lint plugin; it has no Android minSdk model. | 4c |
| OQ-A155 | (4c-6) Must the mock state JSON be byte-compatible with iOS? | No. Keep the Swift property names and schema 2 with required non-optional fields; allow nullable optional additions. Android reseeds incompatible state in debug. Claims accept legacy plaintext only on decode and always write a hash. | 4c |
| OQ-A156 | (4c-2) Does mock seeding also depend on 1b? | Yes, already landed: reuse top-level Template, memoized MockTripTemplates, mock-seed.json and LargeTimelineFixtures; do not duplicate the fixture generator. | 4c |
| OQ-A157 | (4c-4) How do perf variants select demo seeding? | MockDataSource takes debugAppSeed, default true; 4e passes false in perf. Like iOS Perf without DEBUG, that path restores state or starts empty unless given an explicit seed. | 4c, 4e |
| OQ-A158 | (4c-9) Can fixture decoding exceed the JVM case budget? | Memoize decoded fixtures per JVM and warm them in BeforeClass. Keep every test below 1 s with no PerfBudget exemption; report any miss. | 4c |
| OQ-A159 | (4c-10) Which zone defines the daily seed anchor? | Inject ZoneId, defaulting to the device zone at production entry signatures only. Tests pin Europe/Amsterdam. Rebase calendar days in each card’s origin zone. | 4c |
| OQ-A160 | (4c-11) Rename demo passkeys for Android? | Keep iPhone 16 and MacBook Pro for pinned iOS parity; the mock never ships in release. Revisit if the designer changes capture data. | 4c |
| OQ-A161 | (4c-7) Must resend and verify use rate windows? | Port iOS exactly: forgot and the other named limiters use windows; resend has a 60 s resendAvailableAt bound; verify has no separate window. | 4c |
| OQ-A162 | (4c-8) Are preparatory contracts missing after 4b? | Rechecked against the landed tree: all required contracts and helpers exist. Reuse their exact types and signatures; no earlier-phase behavior changes or preparatory contract commit. | 4c |
| OQ-A163 | (4c-14) Should MockClock and scenario helpers be shared with later modules now? | Keep them in data-mock/src/test. A consumer phase may move them to a java-test-fixtures source set; never duplicate them into another module. | 4c, 4d, 5, 9 |
| OQ-A164 | (4c-15) How do string limits and password classes retain Swift Unicode semantics? | Apply one shared BreakIterator(Locale.ROOT) grapheme counter to every Swift String.count limit, including passwords, passkey names/challenges/assertions/claim fields and airport queries. Classify password code points with Character.getType; use codePointCount only for Swift unicodeScalars.count (device and sign-up names). Verify emoji and decomposed accents at limits, Ä as uppercase, and rejection of all-uppercase passwords. | 4c |
| OQ-A165 | (4c-1) Split guest mock review and registration claim into 11a/11b? | Port the full intertwined MockDataSource now, including sign-in/passkey claim replay and guest registration. 11a/11b verify the mock and retain their UI/API work. | 4c, 11a, 11b |
| OQ-A166 | (4c-13) Should 4e repeat mock-class release lookups? | No: 4c adds mockDataSourceIsAbsent for MockDataSource, MockAppSeed, MockServerState and MockScenarioValues. 4e retains its other release-readiness assertions and reuses this proof. | 4c, 4e |
| OQ-0a-22 | Native libraries in the bundle? | Allow only `libandroidx.graphics.path.so` (Compose graphics-path; excluding it breaks Path conversion below API 34); fail on any other `.so` | 0a, 13 |
| OQ-0b-1 | Where does `PlanMetrics` live? `08 §3.1` lists it inside `CfDimens`, but its consumer `:core:plan` is a JVM module that cannot depend on `:core:designsystem` | `core/plan/…/generated/PlanMetrics.generated.kt` (Double, dp), as iOS keeps it in `PlanCore` | 0b, 1b, 1c |
| OQ-0b-2 | Port all iOS TokenGen outputs now (Auth, Settings, Terms, Welcome, PlanUI, Press, LaunchPaint, TypeMetrics), or only colors, sky and type? | All of them now (the iOS files are exact goldens and Phases 2, 3a, 3c, 5a, 9 and 10 need them) | 0b |
| OQ-0b-3 | The iOS moon SVGs use `color(display-p3 …)`, which rsvg-convert 2.63.4 renders blank | Substitute the sRGB `dawn` hex (`#E78A45` / `#F2AD73`) into a committed copy, then rasterize | 0b |
| OQ-0b-4 | The plan's acceptance asks for the moon "on the system splash", but the splash theme that shows `launch_moon` is Phase 3a | 0b records the launcher icon and the default system splash (adaptive icon); the `launch_moon` splash screenshot is 3a's acceptance | 0b, 3a |
| OQ-0b-5 | Dimension values as raw `Float` or Compose `Dp`/`TextUnit`? | Raw `const val Float` in dp or sp as named; Phase 2 wraps them (several iOS `CGFloat` metrics are font sizes) | 0b, 2 |
| OQ-0b-6 | Generate the token `screenDayTitle` (18/28) next to `planDayTitle` (18/26)? | Yes, as iOS does; the day header uses `planDayTitle` (OQ-A9) | 0b, 2 |
| OQ-0b-7 | Android-only derived colors | Generate `systemSelected` (for `toM3()`) and `wizardPickerFill` (iOS emits it); no `pickerSelected` or glass colors (OQ-A6) | 0b, 2 |
| OQ-0b-8 | Open-source licences screen for the OFL fonts | Ship the OFL texts in `assets/licenses/`; no screen until a board exists | 0b |
| OQ-0b-9 | `UnusedResources` findings for outputs consumed later (fonts, `launch_moon`, `launch_sky_top`, `ic_stat_moon`) | Path-scoped ignores in `lint.xml`, each naming its consuming phase (2, 3a, 12b) and removed by that phase | 0b, 2, 3a, 12b |
| OQ-0b-10 | The brief asked for a port of fdlibm `erf`; the sources are not available offline | `Erf.kt` uses the all-positive power series 2/sqrt(pi)·exp(−x²)·Σ 2ⁿx²ⁿ⁺¹/(2n+1)!!, which matches libm to 1e-15 (`ErfTest`, 8 points) and the iOS `LaunchPaint` goldens to 1e-9. Replace it with fdlibm if bit-exactness is ever needed | 0b |
| OQ-0c-1 | Emit iOS keys that an Android overlay entry replaces, or omit them? | **Omit.** Their lock names stay reserved, and `CopyIndex` redirects single-key replacements. | 0c, 5b, 10c, 12c |
| OQ-0c-2 | Which iOS "notifications blocked" steps does the one Android step replace? | `Open your phone's Settings.`, `Tap Notifications, then ChronoFlow.` and `Turn on Allow notifications.` (the list in iOS `Settings/RemindersView.swift:116`). Keep `Come back here. Reminders switch on by themselves.` (Phase 12c re-checks the permission on resume, as iOS does). | 12c |
| OQ-0c-3 | Should `CopyIndex` cover every key, or only an allowlist of runtime keys? | **Every emitted main key.** Ported models carry English keys as on iOS; the cost is one generated `when` per table and no allocation at load. | 0c, 5a–5d, 6d, 7, 9c–9d |
| OQ-0c-4 | Where does the debug-only `Debug route: %@` live? The 4e brief (§5.8, OQ-4e-6, its release test) names it `app_debug_route` in `app/src/debug/res/values{,-nl}/`. | **`app_debug_route_x` in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`**, generated (the lock name; `10` OQ-L8). 4e uses `net.chronoflow.core.ui.R.string.app_debug_route_x` from `app/src/debug` and its release test asserts that `net.chronoflow.core.ui.R.string` has no field `app_debug_route_x`. The orchestrator corrects the 3b brief (its `RoutePlaceholder` lives in `app/src/main` and says to find the name in `strings_ios.xml`: the debug text must move to `app/src/debug`, because main code cannot reference a debug-only resource), the 4e brief, and 9c's reference to "the 4e `app_debug_route` pattern", before 3b starts. | 3b, 4e, 9c |
| OQ-0c-5 | Does AGP's generated locale config list only en and nl? | Expected yes. If AAR locales appear, set `androidResources.localeFilters` to `en`, `nl` in `:app` so the system per-app language list shows exactly en and nl (A17, `10 §9.9`) and Material components never show a third language; record the extra locales here and in `docs/l10n.md`. | 0c, 13 |
| OQ-0d-1 | `number` becomes `Int` (32-bit) for generated types, while Swift `Int` is 64-bit | `Int`; no fixture value exceeds 2^31-1. Hand-written exceptions use `Long`: `SignedVerificationInput.expires`, `ApiPasskey.id`. | 0d, 4b |
| OQ-0d-2 | Generated enum shape: sealed interface with `data object`s and `Unknown(raw)`, or an `enum class Known` nested in a sealed interface | Sealed interface with `data object`s (reads like Swift `.flight`). Revisit if `make size` warns. | 0d, 13 |
| OQ-0d-3 | Redact secrets in `toString()` (Android-only; iOS has no equivalent) | Yes, `***`: passwords, session and reset tokens, guest secrets, signed-link hash and signature, 2FA proofs, challenge ids, TOTP secret, otpauth URL and recovery codes. | 0d |
| OQ-0d-4 | A18 names a Gradle `contractsCheck` task | Only the `make contracts-check` target; no Gradle task. | 0d |
| OQ-0d-5 | `plans/` and `trips/` fixtures were built by iOS at web `afd80dc0`, while the generator is pinned at `e36368ec` | Copy as is (iOS decodes them under the same pin); both pins are recorded in `fixtures/SOURCE.md`. | 0d |
| OQ-0d-6 | `GET trips` envelope type | Hand-written `TripListResponse` in `TripContracts.kt` (iOS keeps an equivalent private type). | 0d, 4b |
| OQ-0d-7 | Type the `ApiTripInput` string fields (`trip_end_kind`, `seat_window`, `bed_available_mode`, `transfer_mode`) as enums | No: they are `string` in TypeScript and `String` on iOS. | 0d |
| OQ-0d-8 | The 0d "Hand-written DTOs" box lists "the settings sections", but iOS keeps `SettingsSection` beside `OutboxChange` and 4b owns it | `SettingsSection` and the PATCH `JsonObject` diffs land in 4b; 0d ticks the box once auth, account, passkey, wizard-input and error-envelope DTOs exist. | 0d, 4b |
| OQ-0d-9 | `fixtures.sha256` is shared with 1a (`intl-cases.json`) and 1b (`mock-seed.json`) | One manifest; each script owns its own lines; `FixtureManifestTest` checks every line. | 0d, 1a, 1b |
| OQ-0d-10 | The brief names `api/GuestTripReviewInput.kt`, but ktlint `filename` requires a file with one class to carry its name | `api/ApiGuestTripReviewInput.kt`. The test-only `contracts/PlanParts.kt` keeps its brief name with a file-level suppression of that rule. | 0d |
| OQ-0d-11 | A TypeScript reference to a type outside the graph (`Array<string>`) raises `KeyError` in the iOS generator | `ValueError("unsupported type: ...")`, as the brief asks for every unsupported syntax. | 0d |
| OQ-0d-12 | Is the guest draft token a secret? | Yes: it can claim a guest trip, so it is redacted from `toString` and logs and kept out of saved state like other secrets. | 0d |
| OQ-0e-1 | A3 makes `:core:testing` JVM (Konsist rule 2 forbids Android imports there), but guards need Compose UI test and Robolectric, and the tier filter needs the JUnit Platform API. Phase 2's brief says `core/testing/…/Guards.kt` | Add `:core:testing-compose` (Android test helpers: `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`) and `:core:testing-junit` (JVM: filter and listener). `:core:testing` keeps annotations, profiles and fixtures. Later briefs read "Guards.kt" as `:core:testing-compose` | 0e |
| OQ-0e-2 | Plan: "annotations … and a Gradle test listener". JUnit 4 categories cannot read custom annotations, and a Gradle listener cannot see annotations | JUnit Platform + Vintage with a `PostDiscoveryFilter` and a `TestExecutionListener` registered by `ServiceLoader`; `run_tier.py` merges the listener files. Fallback: `@Category` mapped to tags. Adds `@Logic` for Robolectric tests in quick (1d OQ-1d-4) | 0e |
| OQ-0e-3 | The first Robolectric test on each SDK in a JVM pays the sandbox start | 20 s `warmup` cap for that test; 5 s for all others. Record the measured warm-up | 0e |
| OQ-0e-4 | Region after coercion: iOS drops it, the plan keeps it | Keep the phone's region only when its language is the resolved one (`nl-BE` → `nl-BE`, `de-DE` → `en`), so picker week starts follow the phone | 0e |
| OQ-0e-5 | `device-config` format is unspecified | UTF-8 `language=<system\|en\|nl>\ntheme=<system\|light\|dark>\n`, ≤ 64 B, defaults on anything invalid | 0e |
| OQ-0e-6 | Do `cf.*` extras need `CF_AUTH_TEST_ID`? | No. Only `CF_*` keys are gated, as iOS gates the environment but not launch arguments. Release reads none | 0e |
| OQ-0e-7 | Plan: `ScreenTest` "launches through the receiver" | `ScreenTest` calls the same `TestSeeds.apply` in-process, then `ActivityScenario<MainActivity>` (`13 §5.2.3`), which gives `scenario.close()`. `SeedReceiverActivityTest` covers the receiver | 0e |
| OQ-0e-8 | Plan says `ConfigurationPolicy.wrap(base)`; an override survives in-place `uiMode`/size changes more reliably | `applyOverrideConfiguration` in `MainActivity`; `wrap` kept for non-activity contexts. T10's device test decides | 0e |
| OQ-0e-9 | Committing a stored reference before approval | Only the harness probe `HarnessProbe_default_pixel8_en_light.png`; no screen reference until a person approves it | 0e |
| OQ-0e-10 | 0d generates a wire enum `Language` in `:core:model`; the plan names the UI language `Language` too | The resolved UI language is `AppLanguage { En, Nl }` in `…model.config`; a mapping to the wire enum is added when a phase needs it | 0e |
| OQ-0e-11 | `CompilationMode.Partial()` defaults to `BaselineProfileMode.Require`, but no profile exists until 1e | `UseIfAvailable` in 0e; 1e switches `StartupBenchmark` to `Require` when it commits the profiles | 0e |
| OQ-0e-12 | `cf.freezeAt` means splash ms on iOS; 1d (OQ-1d-2) also reads an ISO instant from it for the plan clock | `TestSeed` parses ms (0…60000) into `freezeAtMs` and keeps `freezeAtRaw`; 1d parses the instant itself | 0e |
| OQ-0e-13 | No profile covers 280 dp or split screen (`14 §14`: `split-narrow` 320 × 700, `landscape-short` 800 × 360) | Keep the plan's five; add them when a screen phase needs them | 0e |
| OQ-0e-14 | AGENTS says `make perf` runs "cold and warm" | Cold only in 0e, per the phase entry; warm arrives with 3b | 0e |
| OQ-0e-15 | Low-end device models for `budgets.json` | The script passes the class (`DEVICE_CLASS`); no model list | 0e |
| OQ-0e-16 | 1e's brief names `Scripts/test_check_benchmarks.py`, but 0a discovers only `Scripts/tests/test_*.py` | The test lives at `Scripts/tests/test_check_benchmarks.py`; fixtures at `Scripts/fixtures/benchmarks/` as 1e says. The 1e brief already uses this path (its revision of 2026-11-16); plan.md's Phase 0e "Results checker" bullet still says `Scripts/test_check_benchmarks.py`, so T17 corrects that path in plan.md | 0e |
| OQ-0e-17 | Gradle 9 fails a test task that has test sources but discovers no tests, which tier filtering causes | `failOnNoDiscoveredTests = false` in the convention; missing evidence is still caught by run-tier (no XML or no timing file → exit 2) | 0e |
| OQ-0e-18 | Who builds `make captures DEVICE=1`? | Phase 2 Task 18 (its brief, OQ-2-21, cites this question). 0e ships the Robolectric path and a stub that exits 1 naming Phase 2 | 0e |
| OQ-0e-19 | 1d also compiles `src/perf` into `debug`; the plan says perf variants only | 0e wires the two perf variants only; 1d adds `debug` if it still needs to | 0e |
| OQ-0e-20 | Earlier versions of later briefs drifted from 0e's names: 1d (P8, item 10) and Phase 2 (§5 item 5, Task 18) said package `net.chronoflow.core.testing.tier` / one `Tiers.kt`; Phase 2 passed `Language.En` to `ProfileRule` | 0a fixed `net.chronoflow.core.testing.tiers` (`Smoke.kt`, Makefile `SMOKE`), so 0e keeps `tiers` with one file per annotation, and `ProfileRule` takes `AppLanguage` (OQ-0e-10). Both briefs are already corrected (1d P8 and item 10 name `…testing.tiers`; Phase 2's revision of 2026-11-16 uses `tiers` and `AppLanguage`); do not add aliases | 0e |
| OQ-0e-21 | The 1 s JVM budget now fails `make quick` (T3), but 0a's Konsist classes parse the whole project in their first test, which may take more than 1 s | Exempt only a measured over-budget `:konsist` class with class-level `@PerfBudget` (a structure check over the tree, not a unit test), and record the ms. Every other JVM test keeps 1 s | 0e |
| OQ-0e-22 | Wall-clock budget tests (1b, 1c) flake under the parallel test JVMs of `make quick` (review 2026-11-16) | `@TimingGate` plus a serial `timingTest` task, run by `make timing` as the last step of `make quick` and `make test`; `@PerfBudget` stays a duration-budget exemption only | 0e |
| OQ-0e-23 | `make quick` ran `:build-logic:convention:test` in the same tier call as the other modules, so its XML had no listener file | It runs as its own `build-logic` tier call (no budget); the quick globs name `konsist`, `core/*` and `tools/*` | 0e |
| OQ-0e-24 | The brief says `assertTouchTargets` reads `node.size`, but Material's `minimumInteractiveComponentSize()` is a layout modifier outside the semantics node, so a 48 dp button reports its content size there | The guard reads `node.layoutInfo` (the whole modifier chain, still layout size, never `touchBoundsInRoot`) | 0e |
| OQ-0e-25 | Lint `AppBundleLocaleChanges` fires on `Configuration.setLocales` although the app convention sets `bundle.language.enableSplit = false` | Ignored in `lint.xml` with that reason | 0e |
| OQ-0e-26 | The baseline profile plugin adds the perf build types after `app/build.gradle.kts` runs, so `"benchmarkReleaseImplementation"(…)` fails with `Configuration … not found` | The `configurations.matching { … }.configureEach` fallback from the brief, guarded by `ModuleGraphTest.dataMockOnlyInDebugAndPerf` | 0e |
| OQ-0e-27 | `:core:testing-compose` needs Material 3 for `Text` and `minimumInteractiveComponentSize` in its probes and self-tests | `api(libs.compose.material3)`; it is a test-helper module and is never a main dependency | 0e |
| OQ-0e-28 | The brief's tests loop profiles × languages × themes | `LayoutMatrix` in `:core:testing-compose` is the default (user, 2026-11-16): light only; `small` and `largeFont` in nl, `pixel8` once in en; `LayoutMatrix.wide` adds `foldInner` and `landscapeSideNav` in en | 0e |
| OQ-0e-29 | Applying a seeded app locale recreates the running `SeedReceiverActivity`, which applied the seed twice | The receiver declares `configChanges="locale|layoutDirection|uiMode|fontScale"` in the debug manifest | 0e |
| OQ-1d-16 | The brief's safe-area guard has no exemption, but blocks below the fold failed it: `performScrollTo` aligns a node's bottom with the scroll container's bottom, and `trip.scroll` fills the window under `Scaffold(contentWindowInsets = 0)`, so that bottom lies inside the navigation inset. The real UI is fine (bottom content padding = safe inset + gutter). A long sleep tile's hit frame is also taller than the safe window | `Guards.settledBounds` scrolls on until the node lies inside the viewport and the safe area; a node taller than that must keep its cross axis inside the safe area and at least 48 dp visible. `TimelinePageGuardTest` has no safe-area exemption and exempts the `trip.block.*` tags only from the overlap guard (OQ-1d-5); the touch-target guard has no exemption. It runs the phone matrix (`small`-nl, `largeFont`-nl, `pixel8`-en) because the page has no wide layout before 6e | 1d |
| OQ-1d-17 | The brief says the `release` source set is not seen by `benchmarkRelease` and `nonMinifiedRelease`; it is (a duplicate `TripPageEntry` failed to compile) | The release stub lives in `app/src/releaseNoPerf/kotlin`, wired to the `release` variant only through `onVariants` | 1d |
| OQ-1d-18 | The brief wants the repository, ViewModel and clock logic in plain JVM tests, but `android.os.Trace` is unmocked there | `TimelineTracer` (Android, None) is the seam; the draw lambdas call `android.os.Trace` directly (no allocation), and `androidx.tracing` is not added | 1d |
| OQ-1d-19 | The brief prewarms drawings at a 411.0 dp window, but the page reads the whole pixel width (`pixel8`: 1078 px / 2.625 = 410.67 dp), so keys differ by 1 px | `TimelineTestHost.prewarm` takes the window width the page sees | 1d |
| OQ-1d-20 | `ReleaseReadinessTest` cannot assert `LargeTimelineFixtures` is absent because `testImplementation(:core:testing)` puts it on the release unit-test classpath | Acceptance #4 (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath \| grep -c core:testing` = 0) proves the absence | 1d |
| OQ-1e-15 | Historical `timelineRecomposeCount` counted `Recomposer:recompose` frames, not compositions. DayMenu also read never-equal `layoutInfo` in a composition-read `derivedStateOf`, invalidating its scope each scroll frame | Count child `Compose:recompose` slices of `Recomposer:recompose`. Compute DayMenu's scrolled boolean in `TimelinePage`'s `snapshotFlow`; publish only threshold changes. `TimelineInvalidationTest.scrollingWithinThresholdDoesNotRecomposeDayMenu` guards this source; anchor changes remain expected. Review the corrected count on each run | 1e, 6b |
| OQ-1e-16 | `make baseline-profile` takes about 590 s on `pixel` and an earlier attempt was stopped by `run-tier`'s 600 s idle cap; `timelineLarge60` also needs more than the 300 s test cap | `timeout_msec=900000` in `Scripts/baseline-profile.sh`; the Makefile gives only `baseline-profile` a `TIER_IDLE_TIMEOUT=1200` cap. Run through make; bounded gestures remove the scroll-event waits | 1e, 13 |
| OQ-1e-17 | UiAutomator `fling` added about 5 s of idle accessibility-event wait per gesture and the elapsed-window hitch denominator understated hitches | Use explicit bounded `UiDevice.swipe` steps plus `waitForIdle`. Hitch divides positive overrun by active frame time (`frameOverrunMs` sample count × 1000 / Hz), never the elapsed scroll window. CPU percentiles cover produced frames | 1e, 13 |
| OQ-1e-18 | AGP 9 ignores the profile plugin's keep-all rules, so `nonMinifiedRelease` came out renamed and inlined; `-dontshrink` breaks resources | `app/src/nonMinifiedRelease/keepRules/profile.keep` with `-dontobfuscate -dontoptimize` | 1e, 13 |
| OQ-1e-19 | Placeholder draw count is episodes, not frames: a cached layer may display one draw for several frames (corrects brief OQ-1e-3) | Keep the existing episode-count gate; report `timelinePlaceholderToReplayMaxMs` from the first placeholder through first replay per day lifetime and `timelinePlaceholderUnreplayedCount` for intervals without replay. Zero duration with zero episodes means no placeholders, not a measured zero-length wait | 1e, 13 |

## Tasks

**How a phase works.**
- One phase is one Sonnet worker session on its own branch, `feat/phase-<id>-<slug>`. Its brief, `docs/phases/phase-<id>-<slug>.md`, was written in advance (2026-11-16); before the phase starts, the orchestrator refreshes it against the stack top and commits it on the phase branch (CLAUDE.md step 1, 0a OQ-0a-19). The brief copies the phase entry below, quotes the cited research sections and iOS references, lists the boards, and points to AGENTS.md "Tests".
- **Branches stack.** A phase branches from the stack top once every phase it depends on is in the stack. Phases that run in parallel get one worktree each (CLAUDE.md). When one finishes, the orchestrator rebases it onto the stack top (`git rebase --onto`), in completion order, and runs `make check`. Only then do the phases that depend on it start. There are no merge commits and no pushes.
- **Boards** are `_design/canvas/<Name>.dc.html`. Where a `<Name>DarkNL` wrapper exists, it is listed too and is in scope. Never build these:
  - superseded boards (`14` Appendix A): `TripEditor`, `TripEditorErrors`, `EditorStates`, `TripReview*`, `ReviewAnswer`, `ReviewStates`, `TripDiscard*`, `TripCalculating`, `TripNoCredits`, `DarkTripEditor`, `DarkTripReview`, `Guest{Flight,Health,Sleep,Review,States}`, `WizardOutboundStopNew*`;
  - retired health questions: `WizardQ{Bipolar,Caffeine,Clot,Eye,Pregnant}*`;
  - out of scope (A24): `Calendar*`, `LiveActivity`, `DataDownload*`, `SetData`, `SetPaywall`, `SetCancel`, `TripDelete`, `AuthEmailChanged`, `PasswordPromptEmail`, `WizardCheckPractice*`.
- **iOS references** point into `chronoflow-ios` at `f391a1fa`:
  - `plan.md:<line>` followed by the exact heading or sub-phase label;
  - repository paths, relative to the iOS root.

  Ported code keeps the Swift type names, and ported tests keep the Swift test names (R24).
- **Paths.** `core/plan/…/Timeline.kt` stands for `core/plan/src/main/kotlin/net/chronoflow/core/plan/Timeline.kt`. Tests live in the module's `src/test` (JVM and Robolectric) or `src/androidTest` (emulator).

**Standard acceptance for every phase** (not repeated below):
1. `make check` passes on the phase branch with the `pixel` emulator booted. The orchestrator runs the emulator tiers when the worker sandbox cannot.
2. Each task is ticked in the commit that completes it. Commits are atomic Conventional Commits with no trailers.
3. Tests follow AGENTS.md "Tests": the cheapest layer, the budgets (`build/test-results/*/timings.json`), and at most one emulator journey per feature.
4. Every new visible string exists in en and nl, and no literal reaches `Text`. No raw color, `MaterialTheme.colorScheme` or device geometry appears outside `:core:designsystem` and generated files (Konsist, Lint).
5. **UI phases:**
   - Every board state has a Robolectric guard in light only, because theme never changes geometry: at `small` and `largeFont` in nl (the longest copy), and once at `pixel8` in en. `foldInner` and `landscapeSideNav` run only for screens with a wide or side-navigation layout. Light and dark, and every other combination, are covered by the captures (user, 2026-11-16: test smart, not exhaustively; this overrides the wider matrix in briefs written earlier, and the orchestrator trims each brief when it refreshes it).
   - `make captures ONLY=<screens>` was compared with the boards by the Opus reviewer.
   - Each difference is fixed, or recorded with its board name.
6. Debug runs are StrictMode-clean: no disk or network work on the main thread.
7. Measured numbers and decisions go into the docs the phase names. A new question goes to "Open questions for the user" with a default.

**Milestones.**
- **M0, tooling:** 0a–0e.
- **M1, timeline proven on synthetic data:** 1a–1e. The gate: `docs/perf-timeline.md` records that the emulator gates pass, plus the architecture decision. No trip-page phase (6b onward) starts before M1.
- **M2, launch, data and auth:** 2, 3a–3c, 4a–4e, 5a–5d.
- **M3, day to day:** 6a–6e, 7, 8.
- **M4, create and edit trips:** 9a–9h.
- **M5, settings, guest and reminders:** 10a–10d, 11a–11b, 12a–12c.
- **M6, release and verification:** 13, 14.

**Single-lane order** (one worker at a time): the order below, which is a valid dependency order.

**Parallel waves.** A phase can start in the first wave after all of its dependencies; phases in the same wave can run in parallel worktrees.

| Wave | Phases that can run in parallel |
|---|---|
| 1 | 0a |
| 2 | 0b, 0c, 0d, 0e |
| 3 | 1a, 2, 4a, 9a |
| 4 | 1b, 3a, 4b, 12a |
| 5 | 1c, 3b, 4c |
| 6 | 1d, 3c, 4d |
| 7 | 1e, 4e |
| 8 | 5a, 6a, 9b |
| 9 | 5b, 5c, 5d, 6b, 8, 9c, 10a |
| 10 | 6c, 9d, 10c |
| 11 | 6d, 9e, 10d |
| 12 | 6e, 7, 9f, 10b, 12b |
| 13 | 9g |
| 14 | 9h, 11a, 12c |
| 15 | 11b |
| 16 | 13 |
| 17 | 14 |

The longest dependency chain has 17 waves: 0a → 0d → 1a → 4b → 4c → 4d → 4e → 6a → 9c → 9d → 9e → 9f → 9g → 11a → 11b → 13 → 14. Paths through 0c instead of 0d, 1b instead of 4b, or 9b instead of 6a are as long. The data chain (0d → 1a → 4b, and 1b → 4c → 4d) now decides when 4e starts: the launch chain reaches 3b in wave 5, one wave before 4d, so a slip in 1a, 1b or a data phase delays every phase after 4e.

### Phase 0a: Scaffold, build logic and tiers (`feat/phase-0a-scaffold`)

- **Brief:** [`docs/phases/phase-0a-scaffold.md`](docs/phases/phase-0a-scaffold.md), written by the orchestrator before the phase starts.
- **Depends on:** none (first phase). **Wave:** 1.
- **Research:**
  - `docs/research/11-android-stack.md` §1–§9
  - `docs/research/13-testing-tooling.md` §7–§8
  - `docs/research/14-gaps.md` §6, §11
  - `docs/research/01-ios-plan-reuse.md` §8
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:634` "Phase 0: Scaffold and tooling (`feat/phase-0-scaffold`)"
  - `plan.md:68` "Test tiers (user decision 2026-11-06)"
  - `plan.md:938` "Tooling: test speed (user request, 2026-11-06)"
  - `plan.md:974` "Tooling round 3: test tiers (user decision, 2026-11-06)"
  - `plan.md:123` "Constraints and working rules"
  - `AGENTS.md`
  - `CLAUDE.md`
  - `Makefile`
  - `Scripts/ci.sh`
  - `Docs/tooling.md`
  - `Docs/ui-test-tiers.md`

**Delivers.** Creates the git repository; its first commit holds only plan.md, AGENTS.md, CLAUDE.md, docs/research/, _design/ and .gitignore. Adds the Gradle 9.9.1 wrapper, the version catalog and the build-logic convention plugins, and creates every module of the A3 graph as a skeleton so the Konsist rules guard module boundaries from day one. A minimal :app (Application, MainActivity, manifest allowlist, debug StrictMode, debug network config) installs and launches as net.chronoflow.app.native.debug. Spotless with ktlint, Lint as errors, Konsist, verifyReleaseManifest and the size gate are wired, together with the Makefile, Scripts/ci.sh, run-tier.sh, emulator.sh and measure-tiers.sh. The phase measures make quick and make check, records the numbers, and proves which tiers the worker sandbox can run. It adds no product code.

**Tasks**
- [x] **Repository.**
  - Run `git init -b main` and add `.gitignore` (`build/`, `.gradle/`, `.kotlin/`, `local.properties`, `.idea/`, `*.iml`, `captures/`, `*.hprof`, `*.jks` except `app/debug.keystore`, `.claude/*.lock`, `.claude/settings.local.json`).
  - The first commit, `docs: add android plan, agent rules, research and boards`, holds `plan.md`, `AGENTS.md`, `CLAUDE.md`, `docs/research/`, `_design/` and `.gitignore`. Then create `feat/phase-0a-scaffold`.
  - Add `tools/ios-pin.txt` (`f391a1fa`) and `tools/web-pin.txt` (`e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19`).
- [x] **Wrapper.**
  - Bootstrap with the cached distribution: `~/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle wrapper --gradle-version 9.9.1 --distribution-type bin --gradle-distribution-sha256-sum d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`. After that, use only `./gradlew`.
  - If AGP 9.5.1 rejects 9.9.1, use 9.6.0 with its SHA-256 from A1 and record it (OQ-A35).
- [x] **Settings and catalog.**
  - `settings.gradle.kts`: `includeBuild("build-logic")`, the foojay resolver 1.0.0, `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, and every module of A3.
  - `gradle/libs.versions.toml` holds every A1 version.
  - `gradle.properties`: the values from "Android spec: build", plus `android.onlyEnableUnitTestForTheTestedBuildType=false` so that `testReleaseUnitTest` exists. Verify the flag; if AGP rejects it, enable release unit tests in `:app` only.
  - Test: `./gradlew buildEnvironment | grep kotlin-gradle-plugin` prints 2.4.21, and a second `./gradlew help` reuses the configuration cache.
- [x] **Convention plugins.** Add the ten plugins of "Android spec: build" in `build-logic/convention`. Each module's `build.gradle.kts` applies exactly one of them and sets its A2 namespace. Test: `./gradlew assembleDebug` builds every module.
- [x] **Minimal app.**
  - `ChronoFlowApp`: debug StrictMode per A21, and a lazy `AppGraph` placeholder that does no I/O.
  - `MainActivity` with an empty Compose root.
  - The manifest per "Android spec: manifest and permissions", with only `INTERNET` and `ACCESS_NETWORK_STATE` for now.
  - An exclude-everything `res/xml/data_extraction_rules.xml`.
  - In `app/src/debug/`, a `network_security_config.xml` that allows cleartext to `10.0.2.2` only.
  - `version.properties` with the A2 `versionCode` formula, and a committed `app/debug.keystore`.
  - Test: `AppLaunchSmokeTest` (`@Smoke`, emulator) launches under StrictMode `penaltyDeath`. `adb shell pm list packages` must show `net.chronoflow.app.native.debug`, which proves the `native` segment works (OQ-A1, Risk 14).
- [x] **Quality gates.**
  - Spotless with ktlint 1.8.0 and the Compose rules 0.6.8 (`.editorconfig`).
  - Lint with `abortOnError`, `warningsAsErrors` and `checkDependencies`, configured in `lint.xml`.
  - Kotlin `allWarningsAsErrors`, plus `stability_config.conf` and the compiler reports behind `-PcomposeReports=true`.
  - `:app:analyzeReleaseR8Config` in `check`, if AGP 9.5.1 provides it (`11` names it); otherwise record its absence.
  - The `:konsist` module enforces the A3 rules. Each rule has a planted violation under `konsist/src/test/resources/violations/` that `KonsistRulesTest` must reject.
- [x] **Release gates.**
  - `verifyReleaseManifest` is a build-logic task wired into `check`. It fails on each of the six cases in "Android spec: manifest and permissions". `VerifyReleaseManifestTest` feeds it one planted manifest per case.
  - `measureBundleSize` (`make size`) runs `bundletool build-apks --device-spec` (arm64, xxhdpi, en) and `get-size total`. It warns above 8 MB, fails above 12 MB, and fails on any `.so` file except the OQ-0a-22 allow-list.
- [x] **Makefile and scripts.**
  - Every target in AGENTS.md "Commands". A target whose phase has not landed prints the phase that adds it and exits 1.
  - `Scripts/ci.sh`: `make generate check`, then `git diff --exit-code`.
  - `Scripts/run-tier.sh`:
    - writes logs and results to `build/test-results/<tier>/`;
    - kills the run after 600 s with no log output or 3,600 s in total, with TERM, then KILL 5 s later, to the process group, followed by `./gradlew --stop`;
    - fails a tier that wrote no result file.
    - with `--device`, holds the host-wide device lock (`Scripts/device_lock.py`, `~/.android/cf-emulator.lock`) for the whole tier, so device tiers from parallel worktrees run one at a time; waiting for the lock does not count toward the caps (0a OQ-0a-21).
  - `Scripts/emulator.sh`: `ensure | down | wipe | create <avd> | prep-functional | prep-perf | busy`. Every subcommand except `busy` runs under the device lock.
    - `ensure` boots a headless `pixel` with `-no-window -no-audio -no-boot-anim -gpu host` from its Quick Boot snapshot and applies the device prep from `13 §7.3`.
    - Functional prep sets the animator scales to 0; perf prep sets them to 1.
    - `busy` exits 0 while the device lock is held, a Gradle test task or `run_tier.py` runs on the host, or `am instrument` runs on the device; `wipe` refuses while `busy` says so.
    - `create` makes a T2 AVD on request only: `fold` (`pixel_9_pro_fold`) and `small` (`small_phone`) on the installed API 36 image, and `api29` (`pixel_8`) on `system-images;android-29;google_apis;arm64-v8a`, which `sdkmanager` installs on first use.
  - `Scripts/measure-tiers.sh` writes `build/tier-timings.json`.
  - Test: `Scripts/test_scripts.sh` drives `run-tier.sh` with short caps and a hanging stand-in command.
- [x] **Measure and record.** In `docs/tooling.md`, record:
  - the warm `make quick` and `make check` times, three runs each;
  - what the worker sandbox can do: the Gradle daemon, `adb`, the emulator and `.git/index.lock`;
  - the commands the orchestrator must therefore run outside the sandbox.

**Acceptance**
- `./gradlew --version` reports Gradle 9.9.1 (or 9.6.0, with OQ-A35 recorded) and KGP 2.4.21, and the second run reuses the configuration cache.
- Warm, with `pixel` booted, `make quick` takes < 60 s and `make check` < 5 min; the times are in `docs/tooling.md`.
- `net.chronoflow.app.native.debug` installs and launches, and `AppLaunchSmokeTest` passes under `penaltyDeath`.
- Every Konsist rule and every manifest rule fails on its planted violation.
- `main` holds exactly one commit, the docs commit.

### Phase 0b: Token generator, fonts and icons (`feat/phase-0b-tokens`)

- **Brief:** [`docs/phases/phase-0b-tokens.md`](docs/phases/phase-0b-tokens.md), written by the orchestrator before the phase starts.
- **Depends on:** 0a. **Wave:** 2.
- **Research:**
  - `docs/research/08-design-system.md` §2–§6, §9, §11
  - `docs/research/01-ios-plan-reuse.md` §3
- **Boards:** `AppIcon`, `SplashSpec`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:663` "Phase 1: Design tokens, fonts and launch assets (`feat/phase-1-design-system`)"
  - `plan.md:25` "Decisions" (D10, D11)
  - `plan.md:141` "Tokens (TokenGen output; P3 as `displayP3` r g b)"
  - `plan.md:173` "Typography"
  - `Tools/TokenGen/Sources/TokenGenCore/ColorMath.swift`
  - `Tools/TokenGen/Sources/TokenGenCore/Source.swift`
  - `Tools/TokenGen/TokenGenReport.md`
  - `Tools/TokenGen/{Plan,PlanUI,Settings}.source.json`
  - `Tools/generate_fonts.py`
  - `Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts`
  - `Assets/moon/launch-moon-{light,dark}.svg`
  - `Docs/app-icon.md`

**Delivers.** Ports iOS TokenGen to a JVM tool (:tools:tokengen) that converts the OKLCH values in tokens.json to sRGB with the exact iOS math. It writes the committed Kotlin tokens (colors, sky ramp, type scale, dimensions, the JVM timeline palette), the launch color resources and a TokenGenReport.md. The iOS report is the golden test for all 84 hex values, and verifyTokens joins make generators-check. The phase also copies the ten Figtree and Fraunces TTFs with a hash manifest, builds the adaptive launcher icon from _design/canvas/icon/android and the notification icon, and rasterizes the light and night launch_moon PNGs from the iOS moon SVGs. It adds no UI.

**Tasks**
- [x] **TokenGen port** (`tools/tokengen`, `:tools:tokengen`, a JVM application).
  - `ColorMath.kt`: OKLCH → OKLab → linear sRGB → gamma-encoded sRGB, with the iOS gamut and rounding rules.
  - `Source.kt`: the derived colors of iOS `Source.swift:66-113`, and the sky ramp.
  - Inputs: `_design/canvas/ds/chronoflow/tokens.json`, plus the three iOS `*.source.json` files read at the pin.
  - Outputs, committed and headed "generated, do not edit":
    - `core/designsystem/…/generated/CfColors.generated.kt`, `CfSky.generated.kt`, `CfTypeScale.generated.kt` (dayTitle 18/26, OQ-A9) and `CfDimens.generated.kt`;
    - `core/plan/…/generated/TimelinePalette.generated.kt` (ARGB ints);
    - `app/src/main/res/values{,-night}/colors_launch.xml`;
    - `tools/tokengen/TokenGenReport.md`, with the sRGB hex values and the P3 values for reference (OQ-A7).
- [x] **Golden tests** (`tools/tokengen/src/test`):
  - all 84 hex values equal the iOS `Tools/TokenGen/TokenGenReport.md`;
  - the sky ramp is within ±0.25/255 per channel at 1,001 points;
  - two runs give byte-identical output.

  `verifyTokens` regenerates into `build/` and diffs; it is part of `make generators-check`.
- [x] **Fonts.** Copy the ten TTFs into `core/designsystem/src/main/res/font/`, named `figtree_400.ttf` … `fraunces_500_opsz48.ttf`:
  - Figtree 400, 500, 600 and 700;
  - Fraunces 400 and 500 at opsz 20, 32 and 48 (OQ-A10).

  `fonts.sha256` records the pin, source path and hash of each; `FontHashTest` checks them.
- [x] **Launcher icon.**
  - `app/src/main/res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`: adaptive, with background, foreground and monochrome layers.
  - The layers are vector drawables converted unchanged from `_design/canvas/icon/android/*.svg` (OQ-A43), with colors from `_design/canvas/icon/colors.json`. Record the conversion method in `docs/assets.md`.
  - Test: `AdaptiveIconTest` checks the 108 dp viewport, all three layers, and a single-color monochrome layer.
- [x] **Notification icon** `ic_stat_moon`: 24 dp, white, alpha only, from the moon outline. Lint `IconColors` passes.
- [x] **Launch moon.**
  - `make assets` rasterizes iOS `Assets/moon/launch-moon-light.svg` and `-dark.svg` (read at the pin) with `/opt/homebrew/bin/rsvg-convert` at 1152 × 1152 px.
  - The outputs are `app/src/main/res/drawable-xxxhdpi/launch_moon.png` and `drawable-night-xxxhdpi/launch_moon.png`.
  - `assets.sha256` records input and output hashes. `generators-check` checks the manifest and never re-rasterizes.
- [x] **Make targets** `tokens`, `fonts` and `assets`; `generators-check` covers all three.

**Acceptance**
- All 84 of 84 hex values match, the ramp is within tolerance, and the output is deterministic.
- A hand edit to a generated file fails `make generators-check`. Prove this once with a temporary edit, then revert it.
- The adaptive icon and the moon appear on the `pixel` launcher and system splash, with one screenshot each in `docs/assets.md`.

### Phase 0c: String pipeline (en + nl) (`feat/phase-0c-strings`)

- **Brief:** [`docs/phases/phase-0c-strings.md`](docs/phases/phase-0c-strings.md), written by the orchestrator before the phase starts.
- **Depends on:** 0a. **Wave:** 2.
- **Research:**
  - `docs/research/10-copy-l10n.md` §1–§9
  - `docs/research/01-ios-plan-reuse.md` §6.8
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:296` "Copy (String Catalog; English is the key)"
  - `plan.md:505` "Auth copy (String Catalog; English sentence keys)"
  - `plan.md:1625` "14a: Plan copy catalog and formatting"
  - `plan.md:2376` "Open questions for the user" (OQ 13, 28, 34)
  - `App/Localizable.xcstrings`
  - `App/InfoPlist.xcstrings`
  - `Packages/ChronoFlowKit/Sources/*/Resources/*.xcstrings`
  - `Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json`
  - `Tools/PlanCopyGen`

**Delivers.** Builds tools/l10n/xcstrings2android.py from the research prototype. It reads the 13 iOS String Catalogs at the pinned commit and writes namespaced en and nl string resources with plurals, a stable name lock and an Android-only overlay. The phase copies PlanCopy.json verbatim with a SHA-256 check, generates CopyIndex.kt for keys resolved at runtime, enables generateLocaleConfig and turns the translation Lint checks into errors. After this phase no visible string can exist without its Dutch twin.

**Tasks**
- [x] **Generator** `tools/l10n/xcstrings2android.py` (stdlib, `python3 -I`), built from the `10` Appendix A prototype (`10-copy-l10n.md:430`).
  - It reads the 13 catalogs at `tools/ios-pin.txt` through `git -C $IOS_REPO show`.
  - It writes `core/ui/src/main/res/values/strings_ios.xml` and `values-nl/strings_ios.xml`.
  - Names are `<table>_<slug>`, capped at 48 characters, with a sha1-6 suffix on collision. They are frozen in the append-only `tools/l10n/names.lock.json`.
  - Placeholders: `%@` and `%d` become positional `%1$s` and `%1$d`. Plurals use `one` and `other`. It escapes `'`, `"`, `@`, `?`, `\n` and `%%`.
  - It has a `--check` mode.
  - Test: `tools/l10n/test_xcstrings2android.py` covers positional arguments, plurals, escaping, collisions, lock stability, and the 9 keys that share English but differ in Dutch per table.
- [x] **Android-only overlay.**
  - `tools/l10n/android-only.json`: each entry has a name, en, nl, `replaces`, and `review: "needs review"`.
  - It generates `strings_android.xml` in en and nl with the four OQ-A12 strings.
  - `core/ui/src/main/res/values/strings_untranslatable.xml` holds `app_name` "ChronoFlow".
- [x] **Plan copy.** Copy `PlanCopy.json` verbatim to `core/plan/src/main/resources/plan_copy.json`, with its hash in `tools/l10n/plan_copy.sha256`. Add `make plan-copy` and its check.
- [x] **CopyIndex.kt** (generated, `:core:ui`): maps an iOS key to its `R.string` or `R.plurals` id per table. It serves keys resolved at runtime: server error codes, reminder kinds and enum labels.
- [x] **Locale wiring.**
  - `app_name` already sits in `core/ui/src/main/res/values/strings_untranslatable.xml` (done in 0a, OQ-0a-4).
  - `:app` sets `androidResources { generateLocaleConfig = true }` and adds `app/src/main/res/resources.properties` (`unqualifiedResLocale=en`). (done in 0a)
  - Lint errors: `MissingTranslation`, `ExtraTranslation`, `StringFormatInvalid`, `StringFormatCount` and `PluralsCandidate`.
  - A Konsist rule forbids a string literal as the first argument of `Text(` outside tests and previews.
- [x] **StringParityTest** (JVM, parses the XML): every en name has an nl twin with the same placeholders and plural quantities. Record the counts.

**Acceptance**
- The generator reproduces the committed XML, and `--check` fails on drift.
- Each language has 928 strings and 31 plurals with 0 placeholder mismatches, as in the prototype; any difference is listed in `docs/l10n.md`.
- A planted en-only string fails Lint. Prove this once, then remove it.

### Phase 0d: API contract generator and fixtures (`feat/phase-0d-contracts`)

- **Brief:** [`docs/phases/phase-0d-contracts.md`](docs/phases/phase-0d-contracts.md), written by the orchestrator before the phase starts.
- **Depends on:** 0a. **Wave:** 2.
- **Research:**
  - `docs/research/03-api-contract.md` §2–§5, §16
  - `docs/research/02-ios-architecture.md` §4.12, §5.6
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1555` "Phase 12: Typed trip contracts (`feat/phase-12-trip-model`), no UI, no blockers"
  - `plan.md:435` "API contract and offline sync"
  - `plan.md:1323` "1.1 How the API handles trips"
  - `plan.md:1448` "1.3 What iOS already has"
  - `Tools/contracts-gen/generate.py`
  - `Tools/contracts-check`
  - `Packages/ChronoFlowKit/Sources/AppCore/Contracts/`
  - `Packages/ChronoFlowKit/Sources/AppCore/DataFailure.swift`
  - `Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/`

**Delivers.** Writes tools/contracts-gen/generate_kotlin.py, a Kotlin-emitting twin of the iOS generator that reads the web TypeScript types at e36368ec with the same roots, overrides and overlays. It adds the hand-written auth, account, passkey, wizard-input and error-envelope DTOs and the DataFailure type in :core:model. The iOS fixtures (account, auth, errors, S1–S6 plans, trips, wizard wire) are copied into :core:testing with a checksum manifest. Tests prove that every fixture decodes and that every request body encodes with the right keys.

**Tasks**
- [x] **Generator** `tools/contracts-gen/generate_kotlin.py` (stdlib).
  - It reads `resources/js/types/App/**/index.ts` at `tools/web-pin.txt` through `git -C $WEB_REPO show`.
  - Roots: `ApiTripListItem`, `ApiTripDetail`, `ApiTripInput`, `ApiTripReview`, `ApiTripSaveResult`, `ApiTripInputSnapshot`, `ApiAirportSearchResults` and their enums.
  - It applies the iOS overrides and overlays, including `ScreeningQuestion = 'shift_work'`.
  - Output in `core/model/…/contracts/*.generated.kt`:
    - `@Serializable` classes with `@SerialName`;
    - enums with `Unknown(raw)`;
    - `BlockView` keyed by `type` (20 cases);
    - `CopyArg` told apart from `CaffeineCopyArg` by the `drinks` key;
    - `answer: Map<String, JsonPrimitive>`.
  - It has a `--check` mode.
  - Test: `tools/contracts-gen/test_generate_kotlin.py`; a planted TS change must fail `--check`.
- [x] **Hand-written DTOs** in `core/model/…/api/`: auth, account, the settings sections, passkeys, wizard input and the error envelope. `core/model/…/DataFailure.kt` mirrors the cases of iOS `DataFailure.swift`.
- [x] **ApiJson.** Decoding uses `Json { ignoreUnknownKeys = true; explicitNulls = false }`. Request encoders keep explicit nulls where null matters (check `status`), and PATCH diffs are `JsonObject`.
- [x] **Fixtures.**
  - Copy iOS `Tests/DataAccessTests/Fixtures/{account,auth,errors,plans,trips,wizard}/*.json`, including S1–S6 and S6-RETURN-10N, into `core/testing/src/main/resources/fixtures/`.
  - `fixtures.sha256` records the pin, source path and hash of each; `FixtureManifestTest` checks them.
- [x] **Decode and encode tests** (`core/model/src/test`):
  - every fixture decodes;
  - every request DTO encodes to exactly its fixture's key set (`review-request.json`, `save-request.json`, `guest-review-request.json`);
  - unknown enum values become `Unknown(raw)`;
  - `unknown-field.json` decodes.
- [x] **Make targets** `contracts` and `contracts-check`; the check is part of `generators-check`.

**Acceptance**
- The generated contracts are fresh, and every fixture decodes.
- Request bodies match their fixture keys exactly.

### Phase 0e: Test harness, device profiles and perf wiring (`feat/phase-0e-test-harness`)

- **Brief:** [`docs/phases/phase-0e-test-harness.md`](docs/phases/phase-0e-test-harness.md), written by the orchestrator before the phase starts.
- **Depends on:** 0a. **Wave:** 2.
- **Research:**
  - `docs/research/13-testing-tooling.md` §3–§7
  - `docs/research/11-android-stack.md` §7, §9–§10
  - `docs/research/14-gaps.md` §5, §14
  - `docs/research/12-built-apps.md` §2
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:68` "Test tiers (user decision 2026-11-06)"
  - `plan.md:25` "Decisions" (D1c)
  - `plan.md:974` "Tooling round 3: test tiers (user decision, 2026-11-06)"
  - `AGENTS.md` (## Tests)
  - `Packages/ChronoFlowKit/Tests/SnapshotSupport/profiles.json`
  - `Docs/ui-test-tiers.md`
  - `Docs/perf-timeline.md`
  - `App/LaunchArguments.swift`
  - `App/Debug/`

**Delivers.** Sets up every test tier before product code. It commits profiles.json with the measured pixel AVD insets, Robolectric and Roborazzi with reference-free guards, a tier registry with per-test budgets, and the debug seed harness (SeedReceiverActivity, TestSeeds, the ScreenTest base) with a release-readiness test. It adds the :benchmark module with the perf variants, a cold-start benchmark on the empty app, and check-benchmarks.py with its failing fixture. It runs the ConfigurationPolicy spike (font cap 1.3, en/nl locale coercion, LTR, the device-config read below API 33) and verifies instrumentation timeouts, recording both outcomes. make quick stays under 60 s.

**Tasks**
- [x] **Profiles.**
  - `core/testing/src/main/resources/profiles.json` holds `small`, `pixel8`, `foldInner`, `landscapeSideNav` and `largeFont` (`13 §4.4`).
  - Measure the `pixel` AVD's status-bar and gesture insets once with `adb shell dumpsys window | grep -i inset`, cross-check them against a debug log of `WindowInsets.safeDrawing`, and commit them.
  - Add a `DeviceProfiles.kt` loader.
  - `LocalSafeInsets` in `:core:ui` is fed from `WindowInsets.safeDrawing` at the root in production and from the profile in tests.
- [x] **Robolectric and Roborazzi** through `chronoflow.roborazzi`.
  - A JDK 21 test launcher through foojay (fallback: OQ-A27), with `@GraphicsMode(NATIVE)` and `@Config(sdk = [36])`.
  - `ProfileRule` applies size, density, font scale, locale and night mode.
  - Stored references are off by default; `SNAPSHOT_RECORD=1 make snapshots` records them.
- [x] **Reference-free guards** (`core/testing/…/Guards.kt`), each with a self-test on a planted bad layout:
  - every text node lies inside the window and is not truncated unless marked as ellipsizable;
  - touch targets are at least 48 dp;
  - interactive nodes lie inside `safeDrawing` and do not overlap;
  - headings are marked.
- [x] **Tier registry.**
  - Annotations `@Smoke`, `@Layout`, `@Snapshot`, `@Capture` and `@Perf`. `TierRegistryTest` checks that every Robolectric and emulator test class has exactly one.
  - A Gradle test listener writes per-test durations to `build/test-results/<tier>/timings.json`. It fails a JVM test over 1 s or a Robolectric test over 5 s, unless the test is annotated `@PerfBudget`. Wall-clock budget tests are annotated `@TimingGate` instead: they run only in the serial `timingTest` task (`make timing`, the last step of `make quick` and `make test`), in one test JVM on one Gradle worker, after every other test task.
- [x] **Seed harness**, debug only, in `app/src/debug/kotlin/net/chronoflow/debug/`.
  - An exported `SeedReceiverActivity`.
  - `TestSeeds.apply` handles `CF_AUTH_TEST_ID`, `CF_UI_TEST_PROFILE`, `cf.settings.theme`, `cf.settings.language`, `cf.reduceMotion` and `cf.freezeAt`. Account and route seeds arrive in 4e.
  - A `ScreenTest` base class in `app/src/androidTest` launches through the receiver.
  - The root sets `testTagsAsResourceId = true`.
  - `ReleaseReadinessTest` in `app/src/testRelease` asserts that `Class.forName("net.chronoflow.debug.SeedReceiverActivity")` throws.
- [x] **ConfigurationPolicy spike** (`app/…/ConfigurationPolicy.kt`).
  - Font scale becomes `min(system, 1.3f)`, the locale is coerced to en or nl keeping the region, and the layout direction is LTR.
  - Below API 33 it reads `device-config` under a scoped StrictMode allowance.
  - Robolectric tests at API 29, 32, 33 and 36: `ar-EG` → en and LTR; `de-DE` → en; `nl-BE` → nl; font scale 2.0 → 1.3, so `largeFont` equals the cap.
  - Record whether `LocaleManager` plus this wrapper holds; if it does not, apply the OQ-A24 fallback and record that.
- [x] **Benchmark module** `:benchmark` (`com.android.test` with `androidx.baselineprofile` 1.5.0).
  - Perf variants `benchmarkRelease` and `nonMinifiedRelease` take extra sources from `app/src/perf/`. Verify this works; otherwise use the OQ-A28 fallback and record it.
  - `StartupBenchmark`: cold start, 10 iterations, `CompilationMode.Partial`, `StartupTimingMetric`.
  - `make perf` appends rows to `docs/perf-startup.md`.
- [x] **Results checker** `Scripts/check-benchmarks.py`, with `benchmark/budgets.json` (metric → device class → hard and aim values).
  - It reads every `*-benchmarkData.json` under `build/outputs/connected_android_test_additional_output/`.
  - It fails on a missing, non-finite or over-budget value.
  - It computes jank % and the hitch ratio from the per-frame runs, and prints `Startup metric: …` and `Timeline metric: …` lines.
  - Its test, `Scripts/tests/test_check_benchmarks.py`, runs in quick: the 0.81 s first-frame fixture must fail, and so must a missing metric.
- [x] **Hang caps.**
  - Verify that the runner honors `timeout_msec=60000` (OQ-A29): a temporary 70 s test must fail at about 60 s; then delete it.
  - Gradle `Test` tasks time out after 10 min.
  - The convention is `runTest(timeout = 60.seconds)`.

**Acceptance**
- The guards fail on their planted violations, and the tier registry rejects an untagged test.
- The 0.81 s fixture fails the checker, and `make perf` records a cold start on `pixel`.
- `docs/tooling.md` records three decisions: the ConfigurationPolicy outcome (OQ-A24), the perf variants (OQ-A28) and the timeouts (OQ-A29).
- `make quick` stays under 60 s.

### Phase 1a: PlanCore port: decode, copy and check state (`feat/phase-1a-plan-core`)

- **Brief:** [`docs/phases/phase-1a-plan-core.md`](docs/phases/phase-1a-plan-core.md), written by the orchestrator before the phase starts.
- **Depends on:** 0c, 0d. **Wave:** 3.
- **Research:**
  - `docs/research/04-timeline.md` §2.1–§2.4, §2.14, §9.2–§9.3
  - `docs/research/10-copy-l10n.md` §5–§8
  - `docs/research/09-rn-android-lessons.md` §3
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1623` "Phase 14: Plan copy and timeline logic (`feat/phase-14-plan-logic`), no UI"
  - `plan.md:1625` "14a: Plan copy catalog and formatting"
  - `plan.md:1639` "14b: Timeline logic and observation models"
  - `Packages/ChronoFlowKit/Sources/PlanCore/CopyCatalog.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/CopyLineFormatter.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/CheckState.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/PlanLogic.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/PlanClock.swift`
  - `Packages/ChronoFlowKit/Tests/PlanCoreTests/` (incl. Fixtures/intl-cases.json)

**Delivers.** Ports the non-geometry half of iOS PlanCore into the JVM module :core:plan. It covers plan-detail decoding from raw bytes, CopyCatalog over plan_copy.json with an explicit Language, CopyLineFormatter with fixed month and weekday tables and the 12/24-hour rules, check keys and mark merging, optional and hidden-step logic, and the plan clock. All dates use java.time with cached zone rules and formatters. The iOS test cases (44 intl goldens, caffeine tables, key resolution) are ported and pass on the JVM. It adds no UI.

**Tasks**
- [x] **Plan decoding.** `core/plan/…/PlanDecoder.kt` decodes plan-detail bytes into the 0d contracts, with tolerant enums. It keeps the raw bytes for `plan_versions.detail`. Test: S1–S6 decode.
- [x] **CopyCatalog.kt.**
  - It loads `plan_copy.json` once, lazily and off the main thread.
  - It resolves keys with an explicit `Language`, including the `CopyArg` and `CaffeineCopyArg` arguments and the caffeine tables.
  - Test: the ported `CopyCatalogTests`.
- [x] **CopyLineFormatter.kt.** Fixed month and weekday tables (A17), the 12/24 h rule from the account `timeFormat`, the U+2212 minus, and durations. Test: the 44 cases of `intl-cases.json`, copied to `core/testing/…/fixtures/intl-cases.json` with its hash.
- [x] **CheckState, PlanLogic and PlanClock.**
  - `CheckState.kt`: check keys, and mark merging with pending marks.
  - `PlanLogic.kt`: optional and hidden steps; `show_optional_items` is never sent.
  - `PlanClock.kt`: an interface, with `FakePlanClock` in `:core:testing`.
- [x] **Time hygiene.**
  - A `ZoneRules` cache and cached `DateTimeFormatter`s.
  - A Konsist rule forbids `Locale.getDefault`, `TimeZone.getDefault`, `ZoneId.systemDefault` and `System.currentTimeMillis` in `:core:plan`, `:core:sync`, `:core:reminders` and `:core:wizard`.
- [x] **Ported tests.** Port every non-geometry case of iOS `Tests/PlanCoreTests`, with the Swift names; geometry follows in 1b.

**Acceptance**
- Every ported case passes in `make quick`.
- `:core:plan` has no Android or Compose import (Konsist).

### Phase 1b: Timeline layout, geometry and large fixtures (`feat/phase-1b-timeline-layout`)

- **Brief:** [`docs/phases/phase-1b-timeline-layout.md`](docs/phases/phase-1b-timeline-layout.md), written by the orchestrator before the phase starts.
- **Depends on:** 1a. **Wave:** 4.
- **Research:**
  - `docs/research/04-timeline.md` §2.5–§2.13, §5, §10.1, §12
  - `docs/research/09-rn-android-lessons.md` §4
- **Boards:** `Plan`, `PlanTimelineTop`, `PlanWide`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1705` "15d: Timeline drawing"
  - `plan.md:1713` "15 performance extension (user request, worktree B)"
  - `plan.md:1730` "15 performance enforcement (worktree B, 2026-11-08)"
  - `Packages/ChronoFlowKit/Sources/PlanCore/Timeline.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/TimelineGeometry.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/TimelineLabels.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/TimelineAccessibilityGeometry.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/PreparedPlan.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageState.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageLayout.swift`
  - `Docs/perf-timeline.md`

**Delivers.** Ports the timeline geometry exactly: day bucketing, layoutDay, packLane, ticks, zones, the now position, accessibility geometry with traversal order, and hit frames of at least 48 dp. It adds PlanPreparer and PreparedPlan with per-input caching and stable day identity, and the trip-page state math (resolve, openingDay, replacementAnchor, wide list widths). The large30/large60 generator in :core:testing reproduces the iOS ids, hashes and labels. JVM budget tests hold plan preparation to 40, 100 and 200 ms for S3, large30 and large60 on every sample.

**Tasks**
- [x] **Timeline.kt.**
  - Bucketing (`04 §2.5`).
  - `layoutDay` (`§2.6`): the skip rules; a marker's top is exactTop + 12, de-overlapped by 24 dp; tile height is `min(dayHeight, max(24, …))`; a tile is tiny at ≤ 56; `continuesBefore` and `continuesAfter`.
  - `packLane` (`§2.7`).
  - Zones, ticks, now, overnight flights, multi-timezone days and DST (`§2.8–§2.11`).
- [x] **TimelineGeometry.kt** (`§2.12`).
  - `width = max(minWidth, available)` and `laneWidth = (width − 44)/4 − 4`; columns; tile x and width.
  - Hit frames are at least 48 × 48 dp (iOS uses 44 pt), centred and clamped. Marker and zone-change targets follow `§2.12` and `§5` with the same minimum.
  - When `minWidth > available`, the day scrolls horizontally.
- [x] **Labels and accessibility geometry.** `TimelineLabels.kt` and `TimelineAccessibilityGeometry.kt`: labels, traversal order, and accessibility labels joined as in `§2.13`.
- [x] **PlanPreparer.kt and PreparedPlan.kt.**
  - `revision = inputHash|detailVersion`, cached per input, with stable day ids.
  - Blocks ordered by `startUtc`, then id.
  - Progress, notices, the zone instruction and the comparison copy.
- [x] **Trip-page state** (`core/plan/…/trippage/`): `TripPageState.resolve`, `openingDay`, `replacementAnchor`, and `TripPageLayout` (list column 280 dp from 600 dp, 320 dp from 900 dp).
- [x] **Large fixtures:** `core/testing/…/LargeTimelineFixtures.kt`, per `04 §12`.
  - The S3 six-day cycle with the `-tour-<i>` suffix, plus the S1 caffeine overlay with `-caffeine-day-<i>`.
  - Trip id `0199a3c0-0000-7000-8000-0000000000<count>`; hash = SHA-256 of `timeline-<count>-S3-prefix6-S1-caffeine-v2`.
  - `LargeTimelineFixtureTests` asserts:
    - the `§12` invariants: total height 54,000 dp for 60 days, last label "Day 2 · Tue 8 Dec", ≥ 3 zones, ≥ count/6 zone changes, each lane ≥ count blocks, one overnight flight, contiguous days, every copy key resolves;
    - ids and hashes equal to the iOS values.
- [x] **Budget and geometry tests.**
  - `TimelinePreparationBudgetTest` (`@TimingGate`, run serially by `make timing`) times decode + layout + index + prepare. After three warm-ups, each of 10 samples must stay within S3 ≤ 40 ms, large30 ≤ 100 ms and large60 ≤ 200 ms.
  - `TimelineGeometryProfilesTest`: every target is ≥ 48 dp and inside bounds at content widths 288, 358 and 637 dp and at every profile width.
  - The ported iOS timeline invariants.

**Acceptance**
- The budgets hold on every sample.
- The large fixtures meet the invariants and match the iOS ids and hashes.
- The geometry tests pass at every profile.

### Phase 1c: DayDrawing builder and replay (`feat/phase-1c-timeline-drawing`)

- **Brief:** [`docs/phases/phase-1c-timeline-drawing.md`](docs/phases/phase-1c-timeline-drawing.md), written by the orchestrator before the phase starts.
- **Depends on:** 1b, 0b. **Wave:** 5.
- **Research:**
  - `docs/research/04-timeline.md` §3.7–§3.13, §7, §9.4, §9.6, §9.17
  - `docs/research/08-design-system.md` §4, §11.1
- **Boards:** `Plan`, `PlanDark`, `PlanTimelineTop`, `PlanLegend`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1705` "15d: Timeline drawing"
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TimelineRenderer.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayTimelineView.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/PlanAppearance.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/PlanMetrics.generated.swift`

**Delivers.** Implements the per-day static drawing as an immutable struct-of-arrays op list (DayDrawing), built by a pure JVM builder in :core:plan behind a TextMetrics interface. The builder follows the iOS renderer's draw order, category styles and tile content rules. The phase adds the Android TextPaint metrics, the hit index, the accessibility target list, the replay onto a native Canvas with shared resources (hatch and dot shaders, parsed lucide paths) and the bounded DayDrawingCache. JVM and Robolectric tests hold the build budgets (1 ms per day, 5 ms per width, 512 KiB per drawing) and reference-free render guards in light and dark.

**Tasks**
- [x] **DayDrawing.kt** (`:core:plan`): op codes in a `ByteArray`, coordinates in a `FloatArray`, colors in an `IntArray`, plus text runs and path ids. It is immutable and reports `estimatedBytes`.
- [x] **DayDrawingBuilder.kt.**
  - Same draw order as iOS `TimelineRenderer.swift`.
  - Category styles (`04 §3.8`) from `TimelinePalette.generated.kt`, and the tile content rules (`§3.9`).
  - The compact rule: no text inside tiles below a 640 dp window width, or in tiny tiles.
  - Group alpha is pre-composited.
  - It draws markers, zone bands, ticks, and hatch and dot fills.
  - It measures through a `TextMetrics` interface (`measure`, `ellipsize`, `lineHeight`) and emits `HitIndex` and `TimelineTargets`.
- [x] **Android side** (`feature/trip/…/timeline/`).
  - `TextPaintMetrics`: one `TextPaint` per build job, the 0b fonts, font scale ≤ 1.3.
  - `TimelineResources`: the hatch as a `LinearGradient` with `Shader.TileMode.REPEAT`, a 7 dp dot-cell `BitmapShader` (iOS `dotStep`, OQ-1c-1), and lucide paths parsed once.
  - `DrawScope.replay(drawing, resources)` draws on `drawContext.canvas.nativeCanvas` and never measures.
- [x] **DayDrawingCache.kt.** An LRU keyed by (dayId, revision, widthPx, dark, fontScale, language). Limits: ≤ 4 MiB, ≤ 512 KiB per drawing, ≤ 8 entries; it evicts by bytes, then by count.
- [x] **Tests.**
  - `DayDrawingBudgetTest` (`@TimingGate`, run serially by `make timing`, fake metrics): after 3 warm-up passes, the median of 5 measured passes stays ≤ 1 ms per day and ≤ 5 ms per width for large60 at 360, 411 and 841 dp (the max is reported, never gated); ≤ 512 KiB per drawing.
  - `DayDrawingCacheTest`: equal keys reuse the entry; dark, width and font scale miss; the caps hold.
  - `DrawOrderGoldenTest`: the S3 day-1 op list against a reviewed golden.
  - `ReplayGuardTest` (Robolectric, real `TextPaint`): S3 days at 360, 411, 639, 640 and 841 dp, in en and nl, light and dark, at font scale 1.0 and 1.3. No text leaves its tile, no tile leaves its lane, and the palette equals the tokens.
  - `PrecompositedAlphaTest`: pixels match a `saveLayer` reference within 1/255.

**Acceptance**
- The build budgets hold, and the cache never exceeds its caps.
- The S3 golden has been reviewed against `Plan` and `PlanDark`.

### Phase 1d: Timeline host page, clock and invalidation probe (`feat/phase-1d-timeline-host`)

- **Brief:** [`docs/phases/phase-1d-timeline-host.md`](docs/phases/phase-1d-timeline-host.md), written by the orchestrator before the phase starts.
- **Depends on:** 1c, 0e. **Wave:** 6.
- **Research:**
  - `docs/research/04-timeline.md` §4.1–§4.3, §6, §9.5–§9.9, §9.15, §10.2
- **Boards:** `Plan`, `PlanScrolled`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1705` "15d: Timeline drawing"
  - `plan.md:1713` "15 performance extension (user request, worktree B)"
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayTimelineView.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageView.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripScrollTracker.swift`
  - `Packages/ChronoFlowKit/Sources/PlanCore/PlanClock.swift`
  - `Docs/perf-timeline.md`

**Delivers.** Hosts the timeline in Compose without UI polish. A LazyColumn holds one keyed item per day; drawings are built off the main thread behind a placeholder and replayed in drawWithCache inside their own graphics layer, with one pointerInput per day, the isolated NowLine and the invisible semantics overlay. The phase adds the system plan clock (a minute ticker plus time, zone and date broadcasts) and a debug TimelineProbe whose Robolectric tests prove the invalidation gates (scroll 0/0, minute 0/1, day boundary 0/2, a check-off redraws one day). A perf entry opens S1–S6, large30 and large60 from fixtures through the cf.trip-page extra and emits the cf.timeline.firstFrame trace.

**Tasks**
- [x] **TimelinePage** (`feature/trip/…/timeline/TimelinePage.kt`).
  - A `LazyColumn` with one item per day (`key = dayId`, `contentType = "day"`).
  - A plain sticky day header from the generated type and color tokens; the designed header comes in 6b.
  - A minimal M3 day menu (`trip.dayMenu.day.<n>`) and a plain end item (`trip.plan.end`).
- [x] **DayTimeline composable.**
  - Its size comes from constraints only.
  - It requests its drawing from `TimelineDrawingRepository`, which builds on `Dispatchers.Default.limitedParallelism(2)`, prefetches ±2 days and uses the 1c cache.
  - Until the drawing is ready it draws a placeholder (background and ticks). It then replays it in `Modifier.drawWithCache` inside `graphicsLayer {}`.
  - One `pointerInput` resolves taps through `HitIndex`; for now it only invokes a callback, and the sheet comes in 6d.
  - `TimelineTargets` become invisible semantics nodes with labels, `traversalIndex` and `onClick`.
- [x] **NowLine.** It is the only clock reader. It is placed with `Modifier.offset { }` from the minute state, so a tick invalidates only the NowLine.
- [x] **SystemPlanClock** (`core/data/…/clock/`).
  - Minute ticks aligned to the wall minute.
  - `ACTION_TIME_TICK`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED` receivers, registered only while the app is started.
  - `cf.freezeAt` freezes it in debug and perf builds.
- [x] **TimelineProbe.**
  - A `staticCompositionLocalOf<TimelineProbe?> { null }`. The debug implementation counts compositions and static draws per day key.
  - `TimelineInvalidationTest` (Robolectric, in `make quick`, OQ-A30) asserts the `04 §10.2` table over all 60 days of large60.
- [x] **Perf entry**, in `app/src/perf/` and `app/src/debug/`.
  - `cf.trip-page=S1…S6|large30|large60` opens `TimelinePage` from fixtures through `FixtureTimelineSource`. `:core:testing` is a perf and debug dependency only.
  - Traces: `cf.timeline.firstFrame` (async, from ViewModel start to two frames after the first non-placeholder draw), `cf.timeline.build`, `cf.timeline.replay`, and the counter `cf.timeline.placeholder`.
  - Test tags `trip.page` and `trip.timeline.ready`.

**Acceptance**
- The invalidation table holds.
- S1–S6 and large60 open on `pixel` through `adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60`.
- A Perfetto trace shows `cf.timeline.build` on worker threads only, and the run is StrictMode-clean.

### Phase 1e: Timeline Macrobenchmarks, gates and baseline profile (`feat/phase-1e-timeline-perf`)

- **Brief:** [`docs/phases/phase-1e-timeline-perf.md`](docs/phases/phase-1e-timeline-perf.md), written by the orchestrator before the phase starts.
- **Depends on:** 1d. **Wave:** 7.
- **Research:**
  - `docs/research/04-timeline.md` §10.3–§10.5, §11, §15
  - `docs/research/13-testing-tooling.md` §4.5
  - `docs/research/11-android-stack.md` §7
  - `docs/research/12-built-apps.md` §4.2
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `Docs/perf-timeline.md`
  - `plan.md:1020` "Phase 7: Performance and comparison with RN (`feat/phase-7-performance`)"
  - `plan.md:2286` "Performance budget"
  - `plan.md:1730` "15 performance enforcement (worktree B, 2026-11-08)"
  - `plan.md:1741` "15 performance runner repair (worktree B, 2026-11-08)"

**Delivers.** Proves the timeline architecture before any UI polish. It adds the timelineFirstFrame, timelineScroll (exactly 15 flings × 3 iterations on large60) and timelineLastDay Macrobenchmarks with frame, memory, trace-section and placeholder metrics. It generates and commits the baseline and startup profiles and extends check-benchmarks.py with the timeline budgets for the emulator and for devices. The first run and a Perfetto review are recorded in docs/perf-timeline.md, which also records, with numbers, whether design B stands or a fallback (Picture, strips, custom View) is needed.

**Tasks**
- [x] **TimelineBenchmark** (`benchmark/…/TimelineBenchmark.kt`). All three use `CompilationMode.Partial(BaselineProfileMode.Require)` and `androidx.benchmark.suppressErrors=EMULATOR` on the AVD.
  - `timelineFirstFrame`: cold start, 10 iterations, extra `cf.trip-page=large60`, with `TraceSectionMetric("cf.timeline.firstFrame")` and `StartupTimingMetric`.
  - `timelineScroll`: the setup launches and waits for `trip.timeline.ready`. The measured part is exactly 15 flings on `trip.page` with a width/5 gesture margin, using `FrameTimingMetric`, `MemoryUsageMetric(Mode.Max)` and the placeholder count. It runs 3 iterations, each from a fresh launch, then asserts that the day label advanced.
  - `timelineLastDay`: the menu, then `trip.dayMenu.day.59`, must reach `trip.plan.end` within 6 flings.
- [x] **Baseline profile.**
  - `BaselineProfileGenerator`: a startup journey to the first frame, and `cf.trip-page=large60` with 15 flings.
  - Commit `app/src/main/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt`; add `make baseline-profile`.
- [x] **Budgets.**
  - Add the timeline rows of "Performance budget" to `benchmark/budgets.json`. The emulator gates are those of "Where gates apply": a first frame ≤ 800 ms, a peak RSS anon ≤ 150 MiB, builds on main = 0, at most 6 placeholders per iteration and the CPU band; the device rows carry hard and aim values.
  - `make perf-timeline` runs `Scripts/emulator.sh prep-perf`, then `:benchmark:connectedBenchmarkReleaseAndroidTest` filtered to `TimelineBenchmark`, then the checker.
- [x] **JankStats** in the debug and perf variants only: `PerformanceMetricsState.putState("timeline", "day=$i;mode=$mode")`, and jank frames logged.
- [x] **Run and review** on `pixel` with `-gpu host` and animator scale 1.
  - Perfetto, per `04 §11`: no `Recomposer:recompose` and no GC during steady flings, and builds on worker threads only.
  - The compiler reports show `DayTimeline`, `NowLine` and the day header as skippable, with stable parameters.
- [x] **Record** in `docs/perf-timeline.md`:
  - date, commit, device, OS, refresh rate, font scale, theme and build type;
  - the raw P50/P90/P99, jank %, hitch ratio, placeholder count and peak RSS, plus trace links.

  Then re-set 1c's provisional `DayDrawingBudgetTest` limits from three `make timing` runs (about twice the measured medians, tighten only); 1b's limits are budget rows and stay (1e OQ-1e-4).
- [x] **Decide.**
  - Either design B stands, or each missed gate goes to the fix its evidence names (the first-frame breakdown, the cache, the host). Fallback B′, then C, then D is tried only on replay or scroll evidence (A12), each as a bounded spike measured with the same benchmarks. Record the decision in `docs/perf-timeline.md` and amend A12.
  - With a device attached (OQ-A22), run the device rows; otherwise mark them "open: device only".

**Acceptance**
- `make perf-timeline` passes the emulator gates and prints every metric, and the numbers are recorded.
- The profiles are committed, and `BaselineProfileMode.Require` passes.
- The decision on design B is recorded.

### Phase 2: Theme, primitives and system-control tinting (`feat/phase-2-design-system`)

- **Brief:** [`docs/phases/phase-2-design-system.md`](docs/phases/phase-2-design-system.md), written by the orchestrator before the phase starts.
- **Depends on:** 0b, 0e. **Wave:** 3.
- **Research:**
  - `docs/research/08-design-system.md` §5–§12
- **Boards:** `Main`, `AuthStates`, `SetStates`, `SheetStates`, `ItemDetail`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:663` "Phase 1: Design tokens, fonts and launch assets (`feat/phase-1-design-system`)"
  - `plan.md:25` "Decisions" (D1b)
  - `plan.md:59` "Navigation titles (user decision 2026-11-10)"
  - `Packages/ChronoFlowKit/Sources/DesignSystem/`
  - `Docs/settings-board-fidelity.md`
  - `Docs/auth-shadow-round-4.md`

**Delivers.** Builds ChronoFlowTheme on the generated tokens: CompositionLocals for colors, type, shapes, spacing and Reduce Motion, plus the toM3() mapping so Material 3 system components are tinted by tokens. It adds cfPress, the soft shadows, the sky brush, the designed primitives (CfButton in five sizes, the text-field decoration, cards, rows, pills, badges, banners, skeletons) and thin wrappers for system components (top app bars, switch, sheet, alert, snackbar, segmented buttons, pull to refresh, progress). It generates the lucide and Material Symbols vectors, adds the haptics interface and contrast tests, and adds Konsist rules that keep MaterialTheme.colorScheme and raw colors out of feature code.

**Tasks**
- [x] **Theme** (`core/designsystem/…/theme/`).
  - `ChronoFlowTheme(darkTheme)`, with `CfColors` provided through a static `LocalCfColors`.
  - `CfType`: one `FontFamily` per Fraunces optical size, chosen by text size: ≥ 40 → opsz 48, ≥ 26 → opsz 32, else opsz 20.
  - `LocalCfShapes`, `LocalCfSpacing`, `LocalReduceMotion`, and the ripple and text-selection colors.
  - `warmFonts()` runs off the main thread after the first frame.
- [x] **Material mapping.**
  - `toM3()` per "Android spec: Material mapping"; `ToM3Test` pins every role.
  - `ContrastTest` checks the text and background pairs used on the boards, in light and dark, against WCAG AA (4.5:1 for body text, 3:1 for large text and UI). A board pair that fails becomes a design question; it is not changed.
- [x] **Effects.**
  - `Modifier.cfPress()`: a `Modifier.Node` with scale 0.97, alpha 0.85, 120 ms and cubic-bezier(0.25, 0.1, 0.25, 1); alpha only with Reduce Motion.
  - `softShadow1` and `softShadow2` through `Modifier.dropShadow`, from the tokens.
  - `Brush.sky()` from `CfSky`. No blur.
- [x] **Designed primitives** (`core/designsystem/…/component/`):
  - `CfButton`: 5 sizes; primary, secondary, destructive and quiet styles; a loading state; a ≥ 48 dp target;
  - `CfTextFieldDecoration`, for `BasicTextField(TextFieldState)` and `BasicSecureTextField`;
  - `CfCard`;
  - `CfRow`: title, subtitle, badge, chevron and a disabled state;
  - `CfPill`, `CfBadge`;
  - `CfBanner`: error, info and offline;
  - `CfSkeleton`, static with Reduce Motion.
- [x] **System-component wrappers:**
  - `CfLargeTopAppBar`, with `exitUntilCollapsedScrollBehavior` and the system font in bold;
  - `CfSwitch`;
  - `CfModalBottomSheet` (`sheetMaxWidth = 640.dp`);
  - `CfAlertDialog`;
  - `CfSnackbarHost`: inverse tokens, at the bottom (OQ-A5);
  - `CfSegmentedButtons`, `CfPullToRefreshBox`, `CfLinearProgress`;
  - theming for `CfDatePickerDialog` and `CfTimePickerDialog`.
- [x] **Icons.**
  - `tools/icons/generate_icons.py` emits `ImageVector`s for the lucide glyphs in use, from a pinned release; record the version and the ISC license in `docs/third-party.md`.
  - It also emits the Material Symbols Rounded glyphs the boards and the mapping need: `space_dashboard`, `flight`, `settings`, `more_vert`, and so on.
  - No icon font and no `material-icons-extended`.
- [x] **Haptics.**
  - A `Haptics` interface, with `AndroidHaptics` (the constants table) and `NoHaptics`.
  - A Konsist rule forbids haptic calls in `:feature:launch`, so splash and Welcome stay silent.
- [x] **Konsist.**
  - `MaterialTheme.colorScheme`, `Color(0x…)` and named `Color.*` constants are forbidden outside `:core:designsystem`.
  - Feature dp values come from `CfDimens`, `CfSpacing`, or a screen `*Metrics` object that names its board.
- [x] **Shadow fidelity** (Risk 19). Render `CfCard` with `softShadow1` in light and dark and compare it with the `Main` and `SetStates` board renders. Record the result and any adjustment in `docs/design-system.md` before more cards are built.
- [x] **Guards** for every primitive: light-only `LayoutMatrix.phone` (`small`-nl, `largeFont`-nl, `pixel8`-en), plus wide native-window checks; en/nl and light/dark gallery captures. Stored Roborazzi references are opt-in, and for the primitives only.

**Acceptance**
- The role and contrast tests pass.
- The Konsist rules fail on planted violations.
- The shadow comparison is recorded, and every primitive has guards.

### Phase 3a: System splash and launch overlay (`feat/phase-3a-launch-overlay`)

- **Brief:** [`docs/phases/phase-3a-launch-overlay.md`](docs/phases/phase-3a-launch-overlay.md), written by the orchestrator before the phase starts.
- **Depends on:** 2. **Wave:** 4.
- **Research:**
  - `docs/research/05-launch-auth.md` §3.1–§3.3, §3.6–§3.9, §15.1–§15.3, §15.10–§15.11
  - `docs/research/12-built-apps.md` §4.1
  - `docs/research/08-design-system.md` §11.4
- **Boards:** `SplashSpec`, `SplashScreen`, `AppIcon`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:685` "Phase 2: System launch screen (`feat/phase-2-launch-screen`)"
  - `plan.md:694` "Phase 3a: Pure timeline (`feat/phase-3a-timeline`)"
  - `plan.md:717` "Phase 3b: Core Animation scene (`feat/phase-3b-scene`)"
  - `plan.md:735` "Phase 3c: Splash snapshots (`feat/phase-3c-snapshots`)"
  - `plan.md:753` "Phase 3b review fixes"
  - `plan.md:922` "Phase 3c review fixes (after Phase 5)"
  - `plan.md:188` "Splash layer tree and geometry (any `w × h`)"
  - `plan.md:205` "Timeline: shared opening (all modes)"
  - `plan.md:277` "Reduce Motion"
  - `plan.md:285` "Appearance"
  - `plan.md:289` "Accessibility"
  - `Packages/ChronoFlowKit/Sources/Launch/LaunchTimeline.swift`
  - `Packages/ChronoFlowKit/Sources/Launch/LaunchLayout.swift`
  - `Packages/ChronoFlowKit/Sources/Launch/FlightPath.swift`
  - `Packages/ChronoFlowKit/Sources/Launch/StatusLineLayout.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/CubicBezier.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/LaunchClock.swift`
  - `Packages/ChronoFlowKit/Tests/LaunchTests/`

**Delivers.** Implements the Android 12+ system splash (sky color, 288 dp launch_moon) and the Compose launch overlay whose frame 0 equals the system splash pixel for pixel. It ports CubicBezier, LaunchLayout, FlightPath and the LaunchTimeline opening as pure Kotlin with the iOS reference values, and draws the scene in one Canvas on a wall clock with no per-frame allocation, radial gradients instead of blur and a vector wordmark. It wires the activity order (installSplashScreen, the keep condition released on layout, edge-to-edge), Reduce Motion, accessibility, the launch and first-commit traces, and the cold-start and launch-jank benchmarks.

**Tasks**
- [x] **Splash theme** (`app/src/main/res/values{,-night}/themes.xml`). `Theme.App.Starting` sets:
  - `windowSplashScreenBackground` = `@color/launch_sky_top` (`#1F285A`, night `#06091C`);
  - `windowSplashScreenAnimatedIcon` = `@drawable/launch_moon`;
  - `windowSplashScreenBehavior` = `icon_preferred`;
  - `postSplashScreenTheme`, whose `windowBackground` is the sky top.

  API 29–30 go through core-splashscreen (OQ-A23).
- [x] **Activity order** (`MainActivity.kt`):
  1. `installSplashScreen()`, then `super.onCreate()`;
  2. the process-death check (a stub until 3b);
  3. `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT))` and `isNavigationBarContrastEnforced = false`;
  4. `setContent { CfRoot() }`;
  5. `setKeepOnScreenCondition { !launch.overlayPlaced }`, where `overlayPlaced` is set from `Modifier.onPlaced` and never from a draw;
  6. `setOnExitAnimationListener { it.remove() }`.
- [x] **Bezier and clock** (3a Task 1): port the easing algorithm and injectable clocks; guard model imports.
- [x] **Layout and flight path** (3a Task 2): port centred geometry, arc-length lookup and status spacing.
- [x] **Wordmark vector** (3a Task 4): generate hash-checked Fraunces outlines with the iOS advance and tracking.
- [x] **Pure ports** (`feature/launch/…/model/`, JVM-tested):
  - `CubicBezier`;
  - `LaunchLayout`, for any w × h, with the composition `h − k → h/2 + (422 − k)`;
  - `FlightPath`: 128 samples, strokeEnd by arc length;
  - `StatusLineLayout`;
  - the `LaunchTimeline` opening with `sample(t)` (keyframes in `12 §3`);
  - `LaunchClock`.

  Port the iOS `Tests/LaunchTests` cases with the same reference values.
- [x] **LaunchOverlay** (`feature/launch/…/LaunchOverlay.kt`).
  - One `Canvas`, driven through `withFrameNanos` by a wall clock that starts at `t0` (`System.nanoTime` at the first draw).
  - Paths, brushes and paints are built once per size. Radial gradients replace blur, the wordmark is a vector path, and the sky comes from `CfSky`.
  - Reduce Motion follows "### Reduce Motion".
  - Semantics: a single node, "ChronoFlow is opening", with `paneTitle`. App content is hidden with `hideFromAccessibility()` until the hand-off ends.
- [x] **Konsist numeric dp/sp constants** (3a Task 8b): reject unapproved numeric constants; keep the timeline marker at one physical pixel.
- [x] **System-bar icons (OQ-A67).** Apply themed icon styles on theme and overlay changes; retain light status icons on sky.
- [x] **Frame-0 parity.** `LaunchFrameZeroTest` (`@Smoke`, emulator): in light and dark, the 192 dp system `iconView` and 288 dp frame-0 moon canvas are concentric within 1 px and centred on the display; the centre and visible-circle pixels match.
- [x] **Traces and benchmarks.**
  - Traces `launch` (async, from process start) and `first-commit` (the first overlay draw).
  - The signed-out cold `StartupBenchmark` now measures the overlay.
  - `LaunchJankBenchmark`: cold start, a 3.5 s capture, `FrameTimingMetric`.
  - `TraceSectionMetric("first-commit")`.
  - Budget rows in `budgets.json`.
  - [x] App Startup: keep `ProcessLifecycleInitializer` and `ProfileInstallerInitializer` (A21), record each one's cost from a cold-start trace in `docs/perf-startup.md`, remove every other merged initializer (for example `EmojiCompatInitializer`) with `tools:node="remove"`, and add the `STARTUP_INITIALIZER` rule to `verifyReleaseManifest` (OQ-A47).
- [x] **Guards.** `LaunchTimeline.sample(t)` frames at 0, 0.3, 1.0 and 1.5 s at every profile, in light and dark: the moon stays inside the window, and the status line inside `safeDrawing`.
- [x] **StrictMode.** `penaltyDeath` for disk reads on the launch path; `LaunchStrictModeTest` (emulator) survives a cold launch.

**Acceptance**
- Frame 0 matches the system splash in light and dark.
- The first animated change comes at 300 ms ± 1 frame (JVM).
- A 60 fps `adb shell screenrecord`, reviewed frame by frame, shows no flash.
- `first-commit` and the cold TTID are recorded in `docs/perf-startup.md`.

**Validation (2026-11-17):** The final targeted allocation test, `make quick` and `make check` pass. The allocation guard uses the orchestrator-approved five-round minimum rule to ignore one-time VM costs while retaining a zero-allocation steady-state requirement. The required `make perf-timeline` found the 1 px marker and completed its functional cases, but the orchestrator declared its timings **INVALID for judgement** because the host was busy with other builds and simulators. Its CPU band check failed both measurements; no timeline-performance conclusion is drawn from them.


### Phase 3b: Readiness reducer, endings and process policy (`feat/phase-3b-launch-readiness`)

- **Brief:** [`docs/phases/phase-3b-launch-readiness.md`](docs/phases/phase-3b-launch-readiness.md), written by the orchestrator before the phase starts.
- **Depends on:** 3a. **Wave:** 5.
- **Research:**
  - `docs/research/05-launch-auth.md` §3.4–§3.5, §15.1–§15.2
  - `docs/research/14-gaps.md` §3
  - `docs/research/02-ios-architecture.md` §4.5
- **Boards:** `SplashSlow`, `SplashScreen`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:767` "Phase 4: Readiness and hand-off (`feat/phase-4-readiness`)"
  - `plan.md:909` "Phase 4 review fixes (after Phase 5)"
  - `plan.md:221` "Endings"
  - `plan.md:267` "Readiness rules (RN Phase 21, with the RN bug fixed)"
  - `plan.md:25` "Decisions" (D5, D12, D14)
  - `plan.md:2376` "Open questions for the user" (OQ 7, 16)
  - `Packages/ChronoFlowKit/Sources/AppCore/LaunchReadiness.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/LaunchCoordinator.swift`
  - `Packages/ChronoFlowKit/Sources/Launch/LaunchTimeline+Ready.swift`
  - `App/Readiness/StubReadiness.swift`

**Delivers.** Ports the launch readiness reducer with its 1500/2000/8000 ms and never-cut-short rules on an injectable clock, and the endings: the ready exit, the slow status with dots, and the forced signed-out exit at 8 s. It adds a stub readiness seam, driven by debug and perf extras until the real session lands in 4e, the process-level LaunchController (overlay once per process), the 300 ms warm-start fade and the process-death cold-launch policy. It adds the ready and exit traces, the reportFullyDrawn placement and a main-thread-stall test that proves the overlay follows the wall clock.

**Tasks**
- [x] **LaunchReadiness.kt** (pure).
  - [x] **Seams and reducer** (3b Task 1): core readiness interfaces and the pure reducer with the ported JVM cases.
  - Events: session, gate, stored data, moon anchor, reached time and exit completion. No failure event (OQ-3b-15); unresolved readiness uses the 8 s rule.
  - The 1500/2000/8000 ms rules; an ending is never cut short.
  - Port the iOS readiness cases on a fake clock.
- [x] **Ending timelines** (3b Task 3): ready exit, slow status and interim welcome container fade.
- [x] **Endings**, ported from `LaunchTimeline+Ready`:
  - the ready exit, ending at 1900 ms when readiness arrives before 1500 ms;
  - the slow status with dots (`StatusLineLayout`);
  - the forced signed-out exit at 8 s.

  Known endings are scheduled on the frame clock ahead of time.
- [x] **StubReadiness** (debug and perf only), driven by the iOS `cf.session`, `cf.gate`, `cf.stored` and `cf.readyDelay` extras. `cf.readyAt` belongs to fixed slow mode. 4e replaces the stub everywhere except perf.
- [x] **LaunchController**, process-level, in `AppGraph`.
  - The overlay plays once per process (D14), and a warm start gets a 300 ms fade.
  - It detects a process-death restore (A8) and turns it into a cold launch.
  - It owns `overlayPlaced`.
- [x] **Traces** `ready` and `exit`. Place `reportFullyDrawn()` per spec step 6; while signed in, it follows the first app frame until Today lands in 7. Add `StartupBenchmark` in WARM mode.
- [x] **Stall test.** `LaunchStallTest` (emulator): with `cf.mainThreadStall=500`, the overlay position after the stall equals the wall-clock position, read through the debug `LaunchController.frameLog` ring buffers. The animation is never stretched (OQ-A19).
- [x] **Hand-off targets.** Signed in goes to the app root placeholder; signed out goes to the Welcome placeholder until 3c.

- [x] **Captures, status guards and docs** (3b Task 10): frozen ready/slow endings, both languages across all five status profiles, and the launch seed/OQ documentation.

**Acceptance**
- When readiness arrives before 1500 ms, the hand-off ends at 1900 ms ± 1 frame from `t0` (trace), and the forced exit comes at 8 s.
- A warm start shows no overlay.
- A killed process cold-launches with the splash: on the emulator, `am kill` in the background, then relaunch.
- The warm TTID is recorded.

### Phase 3c: Welcome (`feat/phase-3c-welcome`)

- **Brief:** [`docs/phases/phase-3c-welcome.md`](docs/phases/phase-3c-welcome.md), written by the orchestrator before the phase starts.
- **Depends on:** 3b. **Wave:** 6.
- **Research:**
  - `docs/research/05-launch-auth.md` §4, §15.2, §15.12
  - `docs/research/12-built-apps.md` §4.1
- **Boards:** `Main`, `SplashWelcome`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:792` "Phase 5: Welcome, intro and moon hand-over (`feat/phase-5-welcome`)"
  - `plan.md:819` "Phase 5 fix round 1"
  - `plan.md:852` "Phase 5 user decisions and fix round 2 (2026-11-06)"
  - `plan.md:872` "Phase 5 fix round 3 (2026-11-06)"
  - `plan.md:892` "User decision: board composition on every height (2026-11-06)"
  - `plan.md:901` "User decision: plain sky without a dot pattern (2026-11-06)"
  - `plan.md:930` "Phase 5 review fixes (after Phase 5)"
  - `plan.md:243` "Welcome intro (first launch of the process only)"
  - `plan.md:258` "Welcome layout (on `cf-sky`)"
  - `Packages/ChronoFlowKit/Sources/Welcome/`
  - `Packages/ChronoFlowKit/Sources/AppCore/LaunchIntro.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/MoonAnchorStore.swift`
  - `Packages/ChronoFlowKit/Sources/Launch/LaunchTimeline+Welcome.swift`

**Delivers.** Builds Welcome on the plain sky with the board composition at every window size, the 560 dp copy card on wide windows, and the first-launch intro whose stagger comes from the shared LaunchClock. The overlay moon lands on Welcome's measured moon anchor, and Reduce Motion skips the intro. Plan my trip and Sign in route to debug seams until their destinations exist. Tests cover the composition formula across profiles, the 2550 ms interactive and 3200 ms last-item timing, accessibility order, and reference-free guards in both languages and themes.

**Tasks**
- [x] **Welcome decisions and documentation** (3c Task 10): OQ-A101–112, seed seams, font gate, contrast exceptions and validation record in `docs/phase-3c-validation.md`. Manual TalkBack focus (wordmark first) and quiet-host performance passed 2026-11-18.
- [x] **Welcome timing instrumentation** (3c Task 9): trace-based moments, per-iteration emulator gates and full-intro benchmark/profile waits; quiet-host measurements and regenerated profiles remain with the orchestrator.
- [x] **Welcome board captures** (3c Task 8): 42 final, intro and measured hand-off PNGs in build/captures; standalone overlay fixtures supply profile insets.
- [x] **Diagnostic Welcome mode** (3c Task 6): enable fixed/frozen Welcome in debug and perf, including measured anchors.
- [x] **Intro rendering** (3c Task 4): layer-only sampling of the shared clock, first-appearance claim, frozen frames and a debug-only composition probe.
- [x] **Welcome ending and geometry** (3c Task 2): port the exit, fallback target and composition geometry with JVM coverage.
- [x] **Intro and anchor models** (3c Task 1): port `LaunchIntro` and `MoonAnchorStore` with the Swift-named JVM tests.
- [x] **WelcomeScreen** (`feature/launch/…/welcome/`).
  - The plain sky, with the board composition for any w × h (OQ 11).
  - From 600 dp, the copy card is at most 560 dp wide and centred. Insets come from `LocalSafeInsets`.
  - Headings and button roles, using the iOS test ids (`welcome.signIn`, …).
  - Plan my trip and Sign in call back into `:app`, which routes to debug seams until 11a and 5a.
- [x] **Intro.**
  - It plays on the first Welcome of the process only.
  - The stagger is 2300/2400/2450/2600/2700/2750 ms, from the shared `LaunchClock`. Welcome is interactive at 2550 ms, and the last item lands at 3200 ms.
  - It emits the `intro` trace. With Reduce Motion, the final state shows at once.
- [x] **Moon hand-over.** Welcome reports its moon anchor (`onGloballyPositioned`) into a non-snapshot `MoonAnchorStore`, which sends MoonAnchor to the coordinator and rebuilds the hand-off sampler before the deadline; the overlay reads that sampler at draw time. A measured size never feeds back into composition (`01 §8`).
- [x] **Teardown.** Overlay accessibility hiding lifts on removal; first-node TalkBack focus awaits the manual OQ-A103 check. `reportFullyDrawn()` fires once on the following frame through `ReportDrawnWhen`.
- [x] **Tests.**
  - `WelcomeCompositionTest` (JVM): every profile, and heights from 568 to 1,400 dp.
  - `WelcomeIntroTimingTest`, on a fake clock.
  - Guards in en and nl, light and dark, at every profile.
  - One emulator journey: a cold launch with the `launch` profile → Welcome → Sign in → the seam.

**Acceptance**
- Through the `exit` and `intro` traces relative to `first-commit`, Welcome is interactive at 2550 ms ± 1 frame and the last item lands at 3200 ms ± 1 frame. Quiet-host measurement is pending.
- The composition matches `Main` and `SplashWelcome` at `pixel8` and `foldInner`.

### Phase 4a: Account store, session secrets and device identity (`feat/phase-4a-store`)

- **Brief:** [`docs/phases/phase-4a-store.md`](docs/phases/phase-4a-store.md), written by the orchestrator before the phase starts.
- **Depends on:** 0d. **Wave:** 3.
- **Research:**
  - `docs/research/02-ios-architecture.md` §4.1–§4.4, §4.9–§4.10, §7
  - `docs/research/14-gaps.md` §3
  - `docs/research/09-rn-android-lessons.md` §5
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1064` "Phase 9a: Store, session and device identity (`feat/phase-9a-store`)"
  - `plan.md:382` "Account schema v1 and observation"
  - `plan.md:25` "Decisions" (D18, D19, D21, D22)
  - `plan.md:2376` "Open questions for the user" (OQ 26)
  - `Packages/ChronoFlowKit/Sources/LocalStore/`
  - `Packages/ChronoFlowKit/Sources/AppCore/SessionStore.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/StorageNamespace.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/DeviceIdentityStore.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/SessionMutationPermit.swift`
  - `App/Readiness/KeychainSessionStore.swift`
  - `Packages/ChronoFlowKit/Tests/LocalStoreTests/`

**Delivers.** Adds the per-account SQLDelight database with the iOS schema v1 verbatim plus two local reminder tables, the validated-UUID path layout under noBackupFilesDir, WAL and foreign keys, one writer dispatcher, and transactions guarded by the session epoch and mutation permit. It adds the AndroidKeyStore AES-GCM session store, the UUIDv7 device identity with the device-name rule, DataStore device preferences, the device-config file, the wipe marker and the backup exclusion. The iOS LocalStore cases are ported as JVM tests, and the Keystore cases run as one small instrumented suite.

**Tasks**
- [x] **Schema** (`core/database/src/main/sqldelight/net/chronoflow/core/database/`).
  - The DDL of iOS "### Account schema v1 and observation", verbatim, in `.sq` files.
  - Indexes `outbox(created_at, id)` and `trips(deleted_at)`.
  - The local `scheduled_reminder` and `reminder_ledger` tables.
  - `SchemaParityTest` compares the normalized DDL with `core/database/src/test/resources/ios-schema-v1.sql`, which is extracted from iOS `Packages/ChronoFlowKit/Sources/LocalStore/SchemaV1.swift` at the pin with its hash recorded.
- [x] **Account database factory.**
  - Path: `noBackupFilesDir/<ns>/Accounts/<uuid>/account-<uuid>.db`. Only a validated UUID becomes a path component.
  - `AndroidSqliteDriver` with WAL and `foreign_keys = ON`.
  - One writer on `Dispatchers.IO.limitedParallelism(1)`, writing as `epoch.whileValid { permit?.whileValid { transaction { body() } } }`.
  - Reads go through `AccountDatabase.observe` (SQLDelight table listeners, conflated, fetched on io; completes on close).
- [x] **Ports:** `StorageNamespace` (Api, Mock), `SyncEpoch`, `SessionMutationPermit`, and `DeviceIdentityStore` (UUIDv7 and the device-name rule).
- [x] **KeystoreSessionStore** (`core/datastore/…`).
  - AES-256-GCM under alias `<ns>.session`, written through `AtomicFile` to `noBackupFilesDir/<ns>/session.bin`.
  - An invalidated key or a failed decrypt deletes the session and signs out; an `IOException` retries.
  - Test: the instrumented `KeystoreSessionStoreTest`, at most 6 tests.
- [x] **Device files.**
  - `DevicePreferences`: DataStore `device_prefs`, with `api.deviceId`, `cf.settings.language`, `cf.settings.theme` and `notificationPermissionRequested`.
  - `DeviceConfigFile`: ≤ 64 B, written on language and theme changes.
  - `WipeMarker`: `pending-wipe.json`.
- [x] **Backup exclusion.** Verify `allowBackup=false` and the exclude-everything `data_extraction_rules.xml`; `BackupRulesTest` parses the merged manifest and the rules.
- [x] **Ported tests.** Port the iOS `Tests/LocalStoreTests` cases for the database, schema and four-record mapping as JVM tests; retain their Swift test names. Repository and `AccountModel` cases move to 4d, draft halves to 9b, and wiper, session-database and readiness cases to 4e (OQ-A123). Run database tests on SQLDelight's file-backed JDBC driver (WAL, sidecars and the corrupt-file cases need a file), with the same names.

**Acceptance**
- The schema matches iOS.
- The epoch and permit cases pass: a write after its epoch ended is refused.
- The Keystore suite passes on `pixel`, and a debug run shows no main-thread database access.

### Phase 4b: API client and error mapping (`feat/phase-4b-api-client`)

- **Brief:** [`docs/phases/phase-4b-api-client.md`](docs/phases/phase-4b-api-client.md), written by the orchestrator before the phase starts.
- **Depends on:** 0d, 1a. **Wave:** 4.
- **Research:**
  - `docs/research/03-api-contract.md` §2–§13
  - `docs/research/09-rn-android-lessons.md` §7
  - `docs/research/02-ios-architecture.md` §5.1–§5.3
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1082` "Phase 9b: Data-source contract, mock and API (`feat/phase-9b-data-source`)"
  - `plan.md:435` "API contract and offline sync"
  - `plan.md:25` "Decisions" (D20)
  - `Packages/ChronoFlowKit/Sources/DataAccess/`
  - `Packages/ChronoFlowKit/Sources/AppCore/DataSource.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/DataFailure.swift`
  - `Packages/ChronoFlowKit/Tests/DataAccessTests/`

**Delivers.** Implements the DataSource contract (25 methods) and its OkHttp ApiDataSource: no redirects, no cookies, no cache, per-call timeouts (15 s; 90 s for generation, save and register; 3 s for logout), origin checks and HTTPS-only release builds. It maps the error envelope and status codes to DataFailure, including field errors, throttling with retry-after and email-unverified. Deny-by-default MockWebServer tests pin every endpoint's method, path, body keys and decoding.

**Tasks**
- [x] **DataSource interface** (`core/model/…/DataSource.kt`): the 25 iOS methods as suspend functions. An expected failure surfaces as `DataFailure`, never as a raw `IOException`.
- [x] **ApiClient** (`core/network/…`).
  - OkHttp per A6: no redirects, no cookies, no cache.
  - The base URL and the HTTPS rule come from `:app`.
  - Headers per `03 §2`: the bearer token, `Accept: application/json`; no device-id header and no `Accept-Language` (OQ-4b-15).
  - Per-call cancellable `ApiDeadline` race (OQ-4b-14):
    - 15 s by default;
    - 90 s for `POST trips`, `POST trips/review`, `POST trips/{id}/review`, `PUT trips/{id}`, `POST guest/trips/review` and `POST auth/register`;
    - 3 s for logout.
  - Every response's origin is checked.
- [x] **ApiDataSource:** all 25 methods, using the 0d DTOs and `ApiJson`.
- [x] **ApiErrorMapper** (`03 §3–§4`).
  - The error envelope maps to `DataFailure`: validation with its field errors, throttling with `retry-after` from the header or the body, a 403 with `email_unverified`, and 401, 404, 405, 409, 413, 5xx and 503.
  - Timeouts and I/O errors become `Offline`.
  - Each of the 13 `errors/*.json` fixtures maps to one expected case.
- [x] **MockWebServer suite** (`core/network/src/test`, mockwebserver3, deny-by-default dispatcher).
  - It pins the method, path, query, body keys and decoding of all 25 methods.
  - Redirects are not followed, `Set-Cookie` is ignored, timeouts become `Offline`, and a response from another origin is rejected.

**Acceptance**
- All 25 of 25 methods and all 13 of 13 error fixtures are covered.
- The ported iOS DataAccess API cases pass.

### Phase 4c: Mock data source and seeds (`feat/phase-4c-mock-data-source`)

- **Brief:** [`docs/phases/phase-4c-mock-data-source.md`](docs/phases/phase-4c-mock-data-source.md), written by the orchestrator before the phase starts.
- **Depends on:** 4b, 1b. **Wave:** 5.
- **Research:**
  - `docs/research/02-ios-architecture.md` §5.4–§5.6
  - `docs/research/13-testing-tooling.md` §3.3, §5.3
  - `docs/research/01-ios-plan-reuse.md` §6.10
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1082` "Phase 9b: Data-source contract, mock and API (`feat/phase-9b-data-source`)"
  - `plan.md:1555` "Phase 12: Typed trip contracts (`feat/phase-12-trip-model`), no UI, no blockers"
  - `Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockDataSource.swift`
  - `Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockServerState.swift`
  - `Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockAppSeed.swift`

**Delivers.** Ports the mock server: MockDataSource with injectable latency (zero in tests), a one-shot failNext for every method, offline mode, rate windows and a link inbox; MockServerState schema 2 persisted under the mock namespace; and MockAppSeed with the demo and two-factor accounts, seven rebased trips and large30/large60. It is a debug and perf-only dependency, and release-readiness tests prove it never ships. The iOS mock tests are ported.

**Tasks**
- [x] **MockDataSource** (`core/data-mock/…`, JVM). It implements `DataSource`, with:
  - latency from `CF_MOCK_DELAY`, default 0;
  - `failNext(method, failure)`;
  - an offline switch;
  - rate windows for forgot, resend and verify on the injected clock;
  - a link inbox for reset and verify links.
- [x] **MockServerState**, schema 2, at `noBackupFilesDir/ChronoFlowMock/Server/state.json`: atomic writes, a tolerant read, and a reset on a schema mismatch.
- [x] **MockAppSeed.**
  - Accounts: `demo@example.com` / `password`, and `twofactor@example.com` / `password` with code `123456` and recovery code `AbCdEf1234-GhIjKl5678`.
  - Account ids `0199a3c0-0000-7000-8000-000000000000` and `…0001`.
  - Seven trips rebased to the seed clock: S1–S6 and S6-RETURN-10N.
  - large30 and large60 from 1b.
- [x] **Wiring.** The mock is a `debugImplementation` and perf-variant dependency only. `ReleaseReadinessTest` asserts that no mock class is on the release classpath.
- [x] **Ported tests.** Port the iOS mock tests (the `MockScenarios` and `MockScenarioValues` cases) with the same names.

**Acceptance**
- The ported cases pass.
- A release build contains no `net.chronoflow.core.datamock` class (checked with `apkanalyzer dex packages`).

### Phase 4d: Sync engine and outbox (`feat/phase-4d-sync`)

- **Brief:** [`docs/phases/phase-4d-sync.md`](docs/phases/phase-4d-sync.md), written by the orchestrator before the phase starts.
- **Depends on:** 4a, 4c. **Wave:** 6.
- **Research:**
  - `docs/research/02-ios-architecture.md` §6
  - `docs/research/09-rn-android-lessons.md` §6
  - `docs/research/07-day-to-day-settings.md` §11
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1104` "Phase 9c: Full pull and durable outbox (`feat/phase-9c-sync`)"
  - `plan.md:25` "Decisions" (D23, D24)
  - `plan.md:2320` "Deferred fixes"
  - `plan.md:2376` "Open questions for the user" (OQ 31)
  - `Packages/ChronoFlowKit/Sources/Sync/SyncEngine.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/OutboxChange.swift`
  - `Packages/ChronoFlowKit/Tests/SyncTests/`

**Delivers.** Ports SyncEngine and the outbox rules: the full pull in the iOS order, one flight per epoch with coalescing, the 1 s debounce and refreshNow, backoff with retry-after, FIFO chains, rekeying, the stale-plan rule, final refusal with undo, and parked unsupported kinds. It adds the repositories whose mutations write their outbox row in the same transaction, and plan retention leases with eviction. It builds the login upsert that preserves pending local edits, which iOS still lists as a deferred fix. The iOS sync suite (35+ cases) is ported as JVM tests.

**Tasks**
- [x] **Task 0: shared store failure and JDBC test driver.** Move StoreFailure to :core:model and the JDBC helpers to test-only :core:database-testing; guard imports with Konsist.
- [x] **Task 1: stored outbox payloads.**
- [x] **Task 2: sync values, failure policy and plan retention.**
- [ ] **Task 3b: repository observation and AccountModel.**
- [ ] **Task 4: full pull.**
- [ ] **Task 6: sync engine flights.**
- [ ] **Task 9: network path monitor.**

- [ ] **SyncEngine** (`core/sync/…`).
  - Pull order: users/me → account → trips → one transaction → details.
  - One flight per epoch, with coalescing, a 1 s debounce, and `refreshNow`.
  - Backoff is `min(1 s × 2^n, 5 min)`, or a larger `retryAfterSeconds` from the server.
  - Every response is checked against the epoch.
- [ ] **Outbox**, ported from `OutboxChange`.
  - FIFO by `(created_at, id)`, with chains per `settings:<section>`, `trip:<tripId>` and kind.
  - Rekeying, and the stale-plan rule.
  - A final refusal is recorded in `settings.syncFailure`, and the change is undone (`StoreApplication.refuse`). Final means any 4xx except 401, 408, 425 and 429, and not email-unverified. Walkthrough rows are never refused. A stale check-off (409, or 422 on `plan_input_hash` or with the `form` sentence "This plan has changed. Refresh it before marking this step.") is kept for rekeying, never refused. A refused check-off with a notice sets its trip's `plan_versions.detail_version` to NULL (iOS `OutboxFlusher.swift:93–107`).
  - Unsupported kinds are parked (OQ 31).
  - A 401 signals a forced wipe, except that a walkthrough POST 401 keeps its row.
  - A 403 `email_unverified` goes to the verify gate and never wipes.
- [x] **Repositories** (`core/data/…/repository/`): trips, plans, marks, settings and walkthrough. Every mutation writes its domain row and its outbox row in one transaction. Plans have retention leases and eviction. Also port `AccountModel` and the LocalStore cases moved from 4a: `RecordsTests.allRecordsRoundTripUnknownJSONNullsAndCompositeKeys` and `repositoriesBindKeysAndOrderOutboxByTimestampThenID` (without their draft halves), and all six `RepositoryObservationTests`. Build on 4a's `RecordMapping` and `AccountDatabase.observe`.
- [ ] **Preserving login upsert** (OQ-A40), a port of iOS `SessionAccountDatabase.upsert` (`SessionAccountDatabase.swift:29–48`). Server data never overwrites a field that has a pending outbox change. Tests cover the RN `store-account.ts:10–23,46–64` scenario and its reverse.
- [ ] **Ported tests.** Port iOS `Tests/SyncTests` (35+ cases) to the JVM with `FakeDataSource`, a fake clock and the JDBC driver, keeping the names.

**Acceptance**
- Every ported case and the upsert cases pass.
- Konsist forbids `androidx.work` outside the reminders package of `:core:data`.

### Phase 4e: Session controller, app graph, gate and routing (`feat/phase-4e-session`)

- **Brief:** [`docs/phases/phase-4e-session.md`](docs/phases/phase-4e-session.md), written by the orchestrator before the phase starts.
- **Depends on:** 4d, 3b. **Wave:** 7.
- **Research:**
  - `docs/research/02-ios-architecture.md` §4.3–§4.6, §4.11, §8–§11
  - `docs/research/14-gaps.md` §3
  - `docs/research/13-testing-tooling.md` §3, §5
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1134` "Phase 9d: Sync lifecycle, sign-out wipe and launch readiness (`feat/phase-9d-session-lifecycle`)"
  - `plan.md:25` "Decisions" (D17, D21, D25, D26)
  - `plan.md:311` "Core seams"
  - `plan.md:2376` "Open questions for the user" (OQ 8, 16, 29)
  - `Packages/ChronoFlowKit/Sources/AppCore/SessionController.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/SessionGateRouter.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/PendingLaunchIntent.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/Route.swift`
  - `App/Session/NativeSessionController.swift`
  - `App/Session/AccountWiper.swift`
  - `App/Readiness/StoredDataReadiness.swift`
  - `App/LaunchArguments.swift`
  - `App/Debug/`

**Delivers.** Ports SessionController (accept order, generation, sign-out with needsConfirmation, the forced 401 wipe, and the exact teardown order with a 3 s logout bound and the wipe-marker retry). It wires AppGraph and the per-epoch SessionGraph with the foreground sync triggers. It replaces the stub launch readiness with stored-data readiness and adds the session gate (verify, terms, walkthrough, app) with its router, the Nav3 routes, RootNavigator and pending launch intents. It extends TestSeeds with the session, account and route seeds, proves that a reseed equals a cold seed, and proves with release-readiness tests that no seed or mock code ships.

**Tasks**
- [ ] **SessionController** (`core/sync/…`; its Android seams are implemented in `:core:data`).
  - Accept order: DB upsert → secure write → publish.
  - A generation counter.
  - `signOut` asks for confirmation when the outbox is not empty.
  - A forced wipe on 401.
  - Teardown in the order of iOS `SessionController.performSignOut` (`SessionController.swift:275–335`) and `AccountWiper.swift`; the 4e brief §5.3 is the binding step list:
    1. a voluntary sign-out only: flush once and recount, and return `NeedsConfirmation(n)` with nothing touched when rows remain;
    2. write the wipe marker first (`beginWipe`);
    3. clear the stored session and publish signed-out;
    4. stop sync (the reminder-cancel hook first; a no-op until 12b);
    5. call logout, bounded at 3 s, only when this is not a 401 wipe;
    6. detach the session UI;
    7. close the database;
    8. erase: the account directory, then the guest secrets, then `guest-wizard.json`;
    9. delete the session credentials;
    10. clear the wipe marker. If erase or delete fails, the marker stays and the next start retries.
- [ ] **Graphs** (`app/…/AppGraph.kt`, `SessionGraph.kt`).
  - `AppGraph` is process-wide and lazy.
  - A `SessionGraph` per epoch holds the database, repositories and `SyncEngine`.
  - `ProcessLifecycleOwner`: ON_START syncs; ON_STOP only disables sync (no flush, no source call), as iOS `setActive(false)`.
  - ViewModel factories read the graph from `CreationExtras`.
- [ ] **StoredDataReadiness** replaces `StubReadiness` in debug and release; the stub stays in perf. It emits the `session` trace.
- [ ] **Gate and routes.**
  - A `SessionGateRouter` port: verify → terms → walkthrough → app. Missing policies keep the gate loading.
  - The routes and sheets of "Android spec: navigation and routes", in `:core:navigation`.
  - `RootNavigator`, exposing `StateFlow<RootState(route, revision)>`.
  - Nav3 `NavDisplay` in `:app`, plus `PendingLaunchIntent` and a debug route probe for tests.
- [ ] **Seeds.** `TestSeeds.apply` gains `CF_AUTH_TEST_SIGNED_IN`, `CF_AUTH_TEST_WALKTHROUGH_SEEN`, `CF_AUTH_TEST_AUDIT_SCREEN`/`STATE`, `CF_PLAN_TEST_ROUTE`, `CF_PLAN_TEST_POSITION`, `CF_PLAN_TEST_SHEET`, `CF_SETTINGS_ROUTE`, `CF_WIZARD_AUTO_OPEN`, `CF_WIZARD_BOARD_CAPTURE` and `CF_MOCK_DELAY`. It follows `13 §5` steps 1–7. Test: `ReseedEqualsColdSeedTest`.
- [ ] **Release readiness.** No seed or mock class ships in release, and `verifyReleaseManifest` finds no exported debug component.
- [ ] **Tests.**
  - The ported iOS session cases: teardown order, generation, and D23 (an old 401 never wipes a new session).
  - The gate router cases.
  - A Robolectric launch test per gate state.
  - One emulator smoke test: a seeded signed-in cold launch reaches the app root.

**Acceptance**
- The teardown order and D23 tests pass.
- Cold launches route correctly: signed in to the app root, signed out to Welcome.
- The `session` trace is reported in `docs/perf-startup.md`.

### Phase 5a: Auth shell and sign-in (`feat/phase-5a-sign-in`)

- **Brief:** [`docs/phases/phase-5a-sign-in.md`](docs/phases/phase-5a-sign-in.md), written by the orchestrator before the phase starts.
- **Depends on:** 4e, 3c. **Wave:** 8.
- **Research:**
  - `docs/research/05-launch-auth.md` §5, §8, §9.1, §15.3–§15.9, §15.13
- **Boards:** `AuthSignIn`, `AuthSignInError`, `AuthStates`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1176` "Phase 10a: Native Auth shell, controls and Sign in (`feat/phase-10-sign-in`)"
  - `plan.md:1192` "Phase 10a fixes and acceptance (2026-11-07)"
  - `plan.md:471` "Auth geometry, tokens and typography"
  - `plan.md:494` "Form behavior, routing and accessibility"
  - `plan.md:505` "Auth copy (String Catalog; English sentence keys)"
  - `plan.md:2234` "Auth submit feedback fix (2026-11-08, `fix/auth-error-banners`)"
  - `plan.md:25` "Decisions" (D27)
  - `plan.md:2376` "Open questions for the user" (OQ 23)
  - `Packages/ChronoFlowKit/Sources/Auth/`
  - `Packages/ChronoFlowKit/Sources/AppCore/LockoutWait.swift`
  - `Docs/auth-gate.md`

**Delivers.** Builds the shared auth shell on the plain sky (reference frame, insets, card, a 560 dp column on wide windows), the designed fields and banners including the offline banner, and the sign-in screen with validation, server field errors, lockout waits, autofill and IME handling. A successful sign-in hands over to the session gate. The passkey button and divider stay hidden behind the capability. One emulator journey signs in the demo account; everything else is tested on the JVM or in Robolectric.

**Tasks**
- [ ] **Auth shell** (`feature/auth/…/shell/`).
  - The plain sky, with the values of "### Auth geometry, tokens and typography" plus the Android delta: insets `safeTop + 16`, 16 at the sides and 24 at the bottom; card padding 20, radius 26, gap 14.
  - A 560 dp column from 600 dp.
  - `auth.back`, and `auth.banner` for error, info and offline ("Needs a connection" / "Verbinding nodig").
- [ ] **Fields.**
  - `CfTextFieldDecoration` with autofill `contentType` (Username, Password).
  - IME actions run email → password → Go.
  - `imePadding()` and `BringIntoViewRequester` keep the focused field and the primary button visible.
  - The password lives only in `TextFieldState` and is never saved.
- [ ] **SignInViewModel**, porting `SignInModel` and `AuthFormModel`.
  - Validation, and the `05 §8.4` failure mapping to the banner or a field.
  - The `LockoutWait` countdown.
  - Email-unverified goes to the verify route, and two-factor to the 5b route.
  - Success calls `SessionController.accept`, then the gate. Autofill commits only after success.
- [ ] **Passkey entry.** The passkey button and divider exist but stay hidden while `PasskeyCapability` is false.
- [ ] **Tests.**
  - The ported iOS sign-in and form cases (JVM).
  - Guards for the `AuthSignIn`, `AuthSignInError` and `AuthStates` states.
  - On the emulator: one journey (a typed demo sign-in → the app root), and one IME check (the password field and submit stay visible with the keyboard up).
  - A recreation test proves the password is gone.

**Acceptance**
- The screens match the boards, and the journey takes under 10 s.
- No secret appears in saved state.

### Phase 5b: Two-factor and passkey sign-in (`feat/phase-5b-two-factor-passkeys`)

- **Brief:** [`docs/phases/phase-5b-two-factor-passkeys.md`](docs/phases/phase-5b-two-factor-passkeys.md), written by the orchestrator before the phase starts.
- **Depends on:** 5a. **Wave:** 9.
- **Research:**
  - `docs/research/05-launch-auth.md` §9.2–§9.3, §15.7–§15.8
  - `docs/research/03-api-contract.md` §7, §9
- **Boards:** `AuthTwoFactor`, `AuthStates`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1210` "Phase 10b: Two-factor code/recovery and auth acceptance (`feat/phase-10b-two-factor`)"
  - `plan.md:2376` "Open questions for the user" (OQ 20)
  - `plan.md:2055` "Passkey management addendum (2026-11-12, user request: build it now though it cannot be verified yet)"
  - `Packages/ChronoFlowKit/Sources/Auth/`

**Delivers.** Adds the two-factor step with code and recovery entry (numeric and ASCII keyboards, one-time-code autofill), throttling and errors, then the hand-over to the gate. It adds passkey sign-in through Credential Manager behind PasskeyCapability, which stays false until server items S1–S3 are live, with a silent cancel and mapped errors. The iOS two-factor cases are ported, and both two-factor variants get seeded screen tests.

**Tasks**
- [ ] **TwoFactorScreen and TwoFactorViewModel**, porting `TwoFactorModel`.
  - A 6-digit code (`KeyboardType.Number`, `SmsOtpCode` autofill) and a recovery code (`KeyboardType.Ascii`), with a mode switch.
  - Throttling and errors; success goes to the gate.
- [ ] **Passkey sign-in** (`feature/auth/…/passkey/`).
  - `PasskeyCapability` is false until S1–S3 are live, with a debug override for tests.
  - `CredentialManager.getCredential(GetPublicKeyCredentialOption(requestJson))`, built from the server options: rpId `app.chronoflow.net`, timeout 60000, challenge valid for 120 s.
  - Response fields are encoded as unpadded base64url. A cancel is silent, and errors are mapped per `05 §9.2`.
  - A `CredentialClient` wrapper lets the tests run on the JVM.
- [ ] **Copy.** The Android-only 2FA intro (OQ-A12) comes from `android-only.json`.
- [ ] **Tests.**
  - The ported iOS two-factor cases.
  - A passkey JSON round-trip and error mapping with a fake `CredentialClient`, using the `auth/passkey-options.json` fixture.
  - Guards for both two-factor variants.
  - One emulator journey: the two-factor account → code → the app root.

**Acceptance**
- The two-factor cases and the passkey JSON tests pass.
- Every passkey entry point stays hidden while the capability is false.

### Phase 5c: Password reset and App Links (`feat/phase-5c-password-reset-links`)

- **Brief:** [`docs/phases/phase-5c-password-reset-links.md`](docs/phases/phase-5c-password-reset-links.md), written by the orchestrator before the phase starts.
- **Depends on:** 5a. **Wave:** 9.
- **Research:**
  - `docs/research/05-launch-auth.md` §9.4–§9.5, §11, §15.6
  - `docs/research/03-api-contract.md` §8
- **Boards:** `AuthForgot`, `AuthReset`, `AuthResetDone`, `AuthResetExpired`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1232` "Phase 11a: Forgot password (`feat/phase-11-account`)"
  - `plan.md:1255` "Phase 11b: Reset, expired and done (`feat/phase-11b-reset`)"
  - `plan.md:2376` "Open questions for the user" (OQ 22, 25, 27)
  - `Packages/ChronoFlowKit/Sources/AppCore/AuthLink.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/PendingLaunchIntent.swift`

**Delivers.** Builds Forgot, Reset, Reset done and Reset expired with the honest reset copy and no session from the 204. It adds verified App Link intent filters for the reset and verify URLs, an exact port of the AuthLink parser, and pending launch intents so a reset link waits until the user is signed out. Tests port the parser cases and drive one link journey with adb on the emulator.

**Tasks**
- [ ] **Forgot** (`AuthForgot`): an email field, a rate-limited send, and the honest confirmation copy (OQ 22, 27).
- [ ] **Reset** (`AuthReset`), **Reset done** (`AuthResetDone`) and **Reset expired** (`AuthResetExpired`). The password field shows the policy hint. The 204 creates no session. An expired link offers a new one.
- [ ] **App Links.**
  - `autoVerify` intent filters on `MainActivity` for `https://app.chronoflow.net/reset-password/…` and `/email/verify/…`, handled in `onNewIntent` under `singleTask`.
  - `AuthLink.kt`, an exact port. A verify link has 5 segments, a UUID id, a 40-hex hash, digit-only `expires` and a 64-hex signature. A reset link has 3 segments and exactly one `email`.
  - `PendingLaunchIntent`: a reset link waits until the user is signed out, and verify links go to the per-account inbox.
- [ ] **docs/app-links.md:** the `assetlinks.json` content for server item S1 (`handle_all_urls`, the release fingerprint only, OQ-A38), and how to test with `adb shell pm verify-app-links`.
- [ ] **Tests.**
  - The ported `AuthLink` cases.
  - Routing cases: signed in versus signed out, cold versus warm.
  - Guards.
  - One emulator journey: forgot → a link from the mock inbox, opened with `adb shell am start -a android.intent.action.VIEW -d <link>` → reset → done.

**Acceptance**
- The `AuthLink` and routing cases pass, and the journey reaches Reset done.
- `docs/app-links.md` lists what the server must publish.

### Phase 5d: Sign-up and email verification (`feat/phase-5d-sign-up-verify`)

- **Brief:** [`docs/phases/phase-5d-sign-up-verify.md`](docs/phases/phase-5d-sign-up-verify.md), written by the orchestrator before the phase starts.
- **Depends on:** 5a. **Wave:** 9.
- **Research:**
  - `docs/research/05-launch-auth.md` §9.6–§9.7, §11.3
  - `docs/research/03-api-contract.md` §8
- **Boards:** `AuthSignUp`, `AuthVerify`, `AuthVerifyFailed`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1271` "Phase 11c: Sign up (`feat/phase-11c-sign-up`)"
  - `plan.md:1287` "Phase 11d: Verify email waiting/failed and recovery acceptance (`feat/phase-11d-verify`)"
  - `plan.md:2376` "Open questions for the user" (OQ 21)
  - `Packages/ChronoFlowKit/Sources/Auth/`

**Delivers.** Builds sign-up: name, email and password with the server policy hint, and two consent links that open the policy pages in a Custom Tab. It builds the verify gate: waiting with a rate-limited resend, open-the-email-app, the per-account verify-link inbox, the failed state, and Verify back as sign-out. Tests port the iOS sign-up and verify cases, and one emulator journey goes from sign-up to a verified account through the mock link inbox.

**Tasks**
- [ ] **SignUpScreen and ViewModel**, porting `SignUpModel`.
  - Name, email, and a password with the policy hint (OQ 21); server field errors.
  - Two consent links open `https://chronoflow.net/terms` and `/privacy` in a Custom Tab (OQ-A32).
  - Register (90 s), then the verify gate.
- [ ] **Verify gate**, porting `VerifyEmailModel`.
  - Waiting, and a resend with its rate limit and countdown.
  - Open the email app with `Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)`.
  - Consumes links from the per-account inbox.
  - The failed state; Verify back means sign out.
- [ ] **Tests.**
  - The ported cases.
  - Guards for `AuthSignUp`, `AuthVerify` and `AuthVerifyFailed`.
  - One emulator journey: sign up → verify through the mock link → the gate.

**Acceptance**
- The ported cases pass, and the journey reaches the gate.
- Verify back leaves the user signed out, with the account data wiped.

### Phase 6a: Tab shell, My trips and pull to refresh (`feat/phase-6a-shell-trips`)

- **Brief:** [`docs/phases/phase-6a-shell-trips.md`](docs/phases/phase-6a-shell-trips.md), written by the orchestrator before the phase starts.
- **Depends on:** 4e, 2. **Wave:** 8.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §2, §4, §10
  - `docs/research/08-design-system.md` §10
  - `docs/research/04-timeline.md` §9.14
- **Boards:** `Main`, `Trips`, `TripsEmpty`, `TripsLoading`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1663` "15a: Board import and the tab shell"
  - `plan.md:1674` "15b: My trips"
  - `plan.md:2252` "Pull to refresh and sync feedback (user, 2026-11-09)"
  - `plan.md:2376` "Open questions for the user" (OQ 38)
  - `App/AppTabs.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/Trips/`

**Delivers.** Adds the three-tab shell with NavigationSuiteScaffold (a bar below 600 dp, a rail from 600 dp, never hidden on scroll), one back stack per tab, reselect-to-root and the back rules. It builds My trips with its sections, cards, empty and loading states and the New trip entry, and pull to refresh with the designed result capsule. Today and Settings show debug route seams until Phases 7 and 10a.

**Tasks**
- [ ] **Shell** (`app/…/shell/AppShell.kt`).
  - `NavigationSuiteScaffold`: a bar below 600 dp and a rail from 600 dp. The bar never hides.
  - Tabs: Today, My trips and Settings, with Material Symbols per OQ-A4 and the tint per OQ-A3.
  - One Nav3 back stack per tab. Reselecting a tab pops it to its root and scrolls to the top. Back from a non-Today root goes to Today, then exits.
- [ ] **My trips** (`feature/trips/…`).
  - `TripsViewModel` over the trips flow, with the sections and sort order of iOS 15b.
  - Cards, `TripsEmpty`, and a `TripsLoading` skeleton.
  - New trip goes to a wizard seam until 9c. A card opens `TripPage(tripId)`, a placeholder until 6b.
- [ ] **Pull to refresh.**
  - `CfPullToRefreshBox` calls `refreshNow`.
  - The designed capsule shows at the top inset with its copy for about 2 s, and fades instead with Reduce Motion.
  - Test tag `refresh.feedback`, plus a debug "was presented" counter.
- [ ] **Seams.** The Today and Settings tabs show debug seams until 7 and 10a.
- [ ] **Tests.**
  - ViewModel cases: sections, sorting, empty.
  - Guards for `Trips`, `TripsEmpty` and `TripsLoading` at `pixel8` (bar) and `foldInner` (rail).
  - On the emulator: system and predictive back across tabs, tab reselect, and pull to refresh through the counter.

**Acceptance**
- The back rules and reselect behave as in iOS 15a.
- My trips matches `Trips`, `TripsEmpty` and `TripsLoading`, and the shell matches `Main`.

### Phase 6b: Trip page: data pipeline and timeline mode (`feat/phase-6b-trip-page`)

- **Brief:** [`docs/phases/phase-6b-trip-page.md`](docs/phases/phase-6b-trip-page.md), written by the orchestrator before the phase starts.
- **Depends on:** 6a, 1e. **Wave:** 9.
- **Research:**
  - `docs/research/04-timeline.md` §3.1–§3.6, §3.10, §3.15, §4.1–§4.3, §9.3, §9.5, §9.8, §9.10
- **Boards:** `Plan`, `PlanDark`, `PlanScrolled`, `PlanScrolledDark`, `PlanTimelineTop`, `PlanDayMenu`, `PlanEnd`, `PlanEndNoText`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1692` "15c: Trip page frame and list view"
  - `plan.md:1705` "15d: Timeline drawing"
  - `plan.md:1722` "15 fix round 1 (worktree B, 2026-11-08)"
  - `plan.md:1758` "15 integration fixes (physical iPhone 16 Pro, 2026-11-08)"
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageModel.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageView.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayHeader.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripHeaderText.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageComponents.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/PageCopy.swift`
  - `App/TripPageDestination.swift`

**Delivers.** Replaces the Phase 1d host with the real trip page fed by the database. Its ViewModel combines the plan, marks and settings flows, prepares off the main thread with a revision guard, and holds a retention lease. The phase adds the top bar, summary card, sticky day headers, compact header line, day menu and pills, the end card and the opening-day anchor. The perf entry now opens the real page, and the Phase 1e benchmarks are re-run through it without losing their budgets.

**Tasks**
- [ ] **TripPageViewModel** (`feature/trip/…`).
  - A `TripPageSource` seam, DB-backed in the app and fixture-backed in perf and tests, combines plan, marks and settings.
  - `PlanPreparer` runs off the main thread behind a revision guard, so stale results are dropped.
  - It holds a retention lease while the page is open.
  - The anchor `(dayId, offsetInDay)` goes into `SavedStateHandle`, for configuration changes only.
- [ ] **Page chrome**, per `Plan`, `PlanScrolled` and `PlanTimelineTop`: the top bar, summary card, designed sticky `DayHeader`, the compact header line on scroll, lane headings, the `NowLine` placement, the day menu and pills (`PlanDayMenu`), and jumps.
- [ ] **End of plan.** The end card (`PlanEnd`, `PlanEndNoText`) and the hidden-steps notice.
- [ ] **Opening position.** `openingDay`, and the summary-position seed `CF_PLAN_TEST_POSITION=summary`.
- [ ] **Perf.**
  - The perf entry now opens this page instead of the 1d host.
  - Re-run `make perf-timeline`. Every emulator gate of "Where gates apply" must pass; this first run of the real page (Page = `trip`) starts the baseline that 6c–6e are compared with. Trend flags are listed in the commit body and do not fail; record the rows in `docs/perf-timeline.md`.
  - Regenerate the baseline profile.
- [ ] **Tests.**
  - ViewModel cases: the revision guard, lease release and the anchor.
  - The 1d invalidation test, re-run on the real page.
  - Guards for each board.
  - A seeded emulator screen test that opens the page at the summary position.

**Acceptance**
- The timeline budgets hold through the real page.
- The page matches `Plan`, `PlanDark`, `PlanScrolled`, `PlanScrolledDark`, `PlanTimelineTop`, `PlanDayMenu`, `PlanEnd` and `PlanEndNoText`.

### Phase 6c: Trip page: list mode and states (`feat/phase-6c-trip-states-list`)

- **Brief:** [`docs/phases/phase-6c-trip-states-list.md`](docs/phases/phase-6c-trip-states-list.md), written by the orchestrator before the phase starts.
- **Depends on:** 6b. **Wave:** 10.
- **Research:**
  - `docs/research/04-timeline.md` §3.14, §3.18, §4.7, §9.11, §9.14
- **Boards:** `PlanList`, `PlanListScrolled`, `PlanLoading`, `PlanNoPlan`, `PlanRemote`, `PlanRemoteLoadingNL`, `PlanRemoteOffline`, `PlanRemoteOfflineNL`, `ErrorNotFound`, `ErrorServer`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1692` "15c: Trip page frame and list view"
  - `plan.md:1498` "Trips gaps and risks"
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayListView.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripDayRow.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageLoadingBar.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPagePathMonitor.swift`

**Delivers.** Adds list mode with the view toggle (not persisted), list rows and zone rows. It adds every non-ready state of the trip page: loading, no plan, plan not on this phone (fetch, loading, offline), not found and server error. A trip that is not on the phone uses ErrorNotFound, as on iOS. Tests cover state resolution on the JVM and each state with seeded guards.

**Tasks**
- [ ] **List mode**, porting `DayListView`.
  - List rows (`TripDayRow`) and zone rows.
  - A view toggle, not persisted; the anchor is kept when toggling.
- [ ] **States:**
  - loading (`PlanLoading`, with `TripPageLoadingBar`);
  - no plan (`PlanNoPlan`);
  - plan not on this phone (`PlanRemote`, `PlanRemoteLoadingNL`, `PlanRemoteOffline`, `PlanRemoteOfflineNL`), with the fetch action;
  - not found (`ErrorNotFound`);
  - server error (`ErrorServer`).

  A `TripPagePathMonitor` port supplies the offline hint.
- [ ] **List benchmark.** `listScroll`: 15 flings over the 740 large60 rows, with the `timelineScroll` metrics and budgets.
- [ ] **Tests.** The `TripPageState.resolve` cases, and guards for each state.

**Acceptance**
- `listScroll` meets the timeline emulator gates.
- Every state matches its board.

### Phase 6d: Step sheet, check-offs, legend and trip actions (`feat/phase-6d-trip-interactions`)

- **Brief:** [`docs/phases/phase-6d-trip-interactions.md`](docs/phases/phase-6d-trip-interactions.md), written by the orchestrator before the phase starts.
- **Depends on:** 6c. **Wave:** 11.
- **Research:**
  - `docs/research/04-timeline.md` §3.19, §4.4–§4.5, §9.13
  - `docs/research/07-day-to-day-settings.md` §5
- **Boards:** `ItemDetail`, `TodayRowStates`, `PlanLegend`, `SheetStates`, `TripActions`, `TripActionsNoPlan`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1911` "17a: Step sheet, check-off and legend"
  - `plan.md:1927` "17c: Trip actions sheet"
  - `Packages/ChronoFlowKit/Sources/PlanUI/Steps/`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripActionsSheet.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripActionsState.swift`

**Delivers.** Routes taps from the timeline and the list to the step sheet (priority, Now badge, interval, time left, reasons, instructions). Actionable steps get Mark as done, Skip and Undo, written through the outbox, with the refused-change message and the step-gone state. The phase adds the legend sheet and the trip actions sheet (Edit and Duplicate, disabled offline or when not editable; no calendar, complete or delete). It adds TalkBack custom actions and keyboard and D-pad support; a check-off has no haptic, as on iOS.

**Tasks**
- [ ] **Step sheet** (`feature/trip/…/step/`, `ItemDetail`).
  - Priority, the Now badge, interval, time left, reasons and instructions.
  - Actionable steps get Mark as done, Skip and Undo, through the marks repository and the outbox.
  - A refused change shows a Snackbar; a step that is gone shows the `SheetStates` state.
  - The sheet is part of the route key.
- [ ] **Legend sheet** (`PlanLegend`).
- [ ] **Trip actions sheet** (`TripActions`, `TripActionsNoPlan`). Edit and Duplicate go to wizard seams until 9h, and are disabled offline or when the trip is not editable. There is no calendar, complete or delete.
- [ ] **Accessibility and input.**
  - TalkBack custom actions (Mark as done, Skip) on timeline targets and list rows, and a defined focus order.
  - Keyboard and D-pad traversal and activation.
  - No haptic on check-off.
- [ ] **Tests.**
  - The ported 17a and 17c cases: mark, undo, refusal, and the 422/429/408 handling.
  - Guards for each sheet state.
  - One emulator journey: open a step → Done → Undo.

**Acceptance**
- Mark, Skip and Undo survive an app restart through the outbox.
- TalkBack offers the custom actions, and the sheets match their boards.

### Phase 6e: Wide and foldable trip page (`feat/phase-6e-trip-wide`)

- **Brief:** [`docs/phases/phase-6e-trip-wide.md`](docs/phases/phase-6e-trip-wide.md), written by the orchestrator before the phase starts.
- **Depends on:** 6d. **Wave:** 12.
- **Research:**
  - `docs/research/04-timeline.md` §3.16, §4.8, §9.12
  - `docs/research/14-gaps.md` §3.3–§3.4, §4
- **Boards:** `PlanWide`, `PlanWideDark`, `PlanWideScrolled`, `PlanWideDayMenu`, `PlanRemoteWide`, `PlanRemoteWideOffline`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1765` "15e: Wide trip page"
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripWindowSizeHost.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/WidePlanSkeleton.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripScrollTracker.swift`

**Delivers.** Adds the side-by-side trip page from 600 dp (a 280 dp list column, 320 dp from 900 dp, no view toggle) and its remote states. Folding, unfolding, rotating and resizing keep the anchor (day and offset in the day) and any open sheet without recreating the activity. Tests use window-size fakes and the foldInner and landscapeSideNav profiles; the fold AVD check runs on request.

**Tasks**
- [ ] **Wide layout** (`TripPageLayout`). From 600 dp:
  - a 280 dp list column (320 dp from 900 dp) beside the timeline, with no view toggle;
  - the wide day menu (`PlanWideDayMenu`);
  - the remote states (`PlanRemoteWide`, `PlanRemoteWideOffline`);
  - `WidePlanSkeleton`.
- [ ] **In-place size changes** (A8 `configChanges`). Folding, unfolding, rotating and resizing keep `(dayId, offsetInDay)` and any open sheet. Port `TripScrollTracker`.
- [ ] **Tests.**
  - Robolectric width changes 411 → 841 → 411 dp keep the anchor.
  - Guards at `foldInner` and `landscapeSideNav`.
  - The `fold` AVD check runs only on request (T2).

**Acceptance**
- The anchor and any open sheet survive every size change without recreating the activity.
- The wide page matches the six `PlanWide*` and `PlanRemoteWide*` boards.

### Phase 7: Today (`feat/phase-7-today`)

- **Brief:** [`docs/phases/phase-7-today.md`](docs/phases/phase-7-today.md), written by the orchestrator before the phase starts.
- **Depends on:** 6d. **Wave:** 12.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §3
- **Boards:** `Today`, `TodayEmpty`, `TodayFuture`, `TodayLoading`, `TodayCompleted`, `TodayRowStates`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1920` "17b: Today"
  - `Packages/ChronoFlowKit/Sources/PlanCore/Today.swift`
  - `Packages/ChronoFlowKit/Sources/PlanUI/Today/`

**Delivers.** Ports the Today logic (trip selection, display status, current step, the next four steps, the next step and minutes-until rules). It builds the Today tab in its five states with the hero card, the Right now and Up next panels, and row states that keep done and skipped steps in place. Tapping a row opens the step sheet over Today, where check-offs go through the outbox; the hero card opens the trip page (OQ-A44). The minute clock is read only in leaf composables, and wide windows center the cards at 640 dp.

**Tasks**
- [ ] **Today.kt** (`:core:plan`): trip selection, display status, the current step, the next four steps, the next step and minutes-until. Port the iOS Today cases.
- [ ] **Today tab** (`feature/today/…`).
  - States: active, empty, future, loading and completed.
  - The hero card, Right now and Up next.
  - Row states keep done and skipped steps in place (`TodayRowStates`). Tapping a row opens 6d's step sheet over Today (Mark as done, Skip, Undo, through the outbox); tapping the hero card opens `TripPage(tripId)` without a block (OQ-A44).
  - The minute clock is read only in leaf composables.
  - From 600 dp, cards are at most 640 dp wide.
- [ ] **TTFD.** `reportFullyDrawn()` fires after the first Today content frame on a signed-in launch.
- [ ] **Tests.**
  - Guards for each state.
  - An invalidation probe: a minute tick recomposes only the leaf.
  - One emulator journey: Today → trip page → back (system and predictive back).

**Acceptance**
- The five states match their boards.
- TTFD on a signed-in launch is recorded in `docs/perf-startup.md`.

### Phase 8: Walkthrough and terms (`feat/phase-8-walkthrough-terms`)

- **Brief:** [`docs/phases/phase-8-walkthrough-terms.md`](docs/phases/phase-8-walkthrough-terms.md), written by the orchestrator before the phase starts.
- **Depends on:** 6a. **Wave:** 9.
- **Research:**
  - `docs/research/05-launch-auth.md` §6–§7
  - `docs/research/07-day-to-day-settings.md` §1
- **Boards:** `Walkthrough`, `Walkthrough2`, `Walkthrough3`, `WalkthroughReplay`, `TermsUpdate`, `TermsFailed`, `PolicyReader`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1967` "17f: Walkthrough"
  - `plan.md:1980` "17g: Terms update"
  - `plan.md:2376` "Open questions for the user" (OQ 39)
  - `Docs/phase-17f-walkthrough.md`
  - `Docs/phase-17g-terms.md`
  - `Packages/ChronoFlowKit/Sources/PlanUI/Walkthrough/`
  - `App/WalkthroughDestination.swift`
  - `App/TermsDestination.swift`

**Delivers.** Builds the three walkthrough pages. Skip and Done commit locally and enqueue the walkthrough change without waiting for sync; the first run ends on Today, and the replay variant returns to its caller. The phase builds the terms-update gate with its accept and failed states and the policy pages in a Custom Tab, with the gate order terms before walkthrough.

**Tasks**
- [ ] **Walkthrough** (`feature/onboarding/…`).
  - Three pages.
  - Skip and Done commit locally and enqueue the walkthrough change without waiting for sync.
  - The first run ends on Today. `Walkthrough(replay = true)` returns to its caller (`WalkthroughReplay`).
- [ ] **Terms update gate** (`TermsUpdate`, `TermsFailed`).
  - Accept posts the change, with a failed state and retry.
  - Policy pages (`PolicyReader`) open in a Custom Tab (`androidx.browser`, OQ-A32).
  - Terms come before the walkthrough, and missing policies keep the gate loading.
- [ ] **Tests.**
  - The ported 17f and 17g cases, including a walkthrough POST 401 that keeps its row.
  - Guards.
  - One emulator journey: accept terms → walkthrough → Today.

**Acceptance**
- The gate order is terms, then walkthrough, then the app.
- The replay returns to its caller, and the screens match their boards.

### Phase 9a: Wizard model (JVM) (`feat/phase-9a-wizard-model`)

- **Brief:** [`docs/phases/phase-9a-wizard-model.md`](docs/phases/phase-9a-wizard-model.md), written by the orchestrator before the phase starts.
- **Depends on:** 0c, 0d. **Wave:** 3.
- **Research:**
  - `docs/research/06-wizard.md` §3–§6, §11
- **Boards:** `WizardSpec`, `WizardCopy`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1787` "Phase 16: Member trip wizard, create (`feat/phase-16-wizard`), M2"
  - `plan.md:1791` "16a: Model, Where and Outbound"
  - `plan.md:1822` "16b.2: Route first, then times (canvas redesign, 2026-11-09)"
  - `Packages/ChronoFlowKit/Sources/TripWizard/Model/`
  - `Docs/wizard-16a.md`
  - `Docs/wizard-16b2.md`
  - `Docs/wizard-fidelity.md`
  - `Packages/ChronoFlowKit/Tests/TripWizardTests/`

**Delivers.** Ports the wizard model into the JVM module :core:wizard: modes and entries, the step graph (route first, then times), transitions, the form and its mapping to the review input, route-editing rules, the strict wall-clock date and time model, picker bounds, client validation, progress values and the recovery record. It applies the no-health-data rules: only age band and shift work block. The model-level iOS wizard tests are ported.

**Tasks**
- [ ] **Model** (`core/wizard/…/model/`):
  - modes (create, edit, duplicate, guest) and entries (`06 §4.1`);
  - steps and transitions, route first, then times (`§4.2–§4.3`);
  - `WizardForm` → `ApiTripReviewInput` (`§5.1`);
  - the route-editing rules, verbatim (`§5.2`);
  - the wall-clock date and time model (`§5.3`) and the picker bounds (`§5.4`);
  - client validation (`§6.1`);
  - progress values (`§4.6`) and the recovery record (`§4.7`).
- [ ] **No-health-data rules.** Only `age_band` and `shift_work` block. `exclude_caffeine` and `flight_movement_breaks` default to false.
- [ ] **Discrepancies.** Resolve each source discrepancy in `06 §11` as recorded there, and name each resolution in a test.
- [ ] **Ported tests.** Port the model-level suites of iOS `TripWizardTests`, keeping the names; the brief lists how many.

**Acceptance**
- Every ported model case passes on the JVM.
- `:core:wizard` has no Android or Compose import.

### Phase 9b: Wizard generation, airport search and recovery (`feat/phase-9b-wizard-data`)

- **Brief:** [`docs/phases/phase-9b-wizard-data.md`](docs/phases/phase-9b-wizard-data.md), written by the orchestrator before the phase starts.
- **Depends on:** 9a, 4e. **Wave:** 8.
- **Research:**
  - `docs/research/06-wizard.md` §4.7, §7, §9.1, §9.3, §9.7
  - `docs/research/03-api-contract.md` §11
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1580` "Phase 13: Plan generation through the API, no UI (`feat/phase-13-trip-generation`), no blockers"
  - `plan.md:1791` "16a: Model, Where and Outbound"
  - `plan.md:1800` "16b: Return, Sleep, Checking and Questions"
  - `plan.md:2376` "Open questions for the user" (OQ 40)
  - `Packages/ChronoFlowKit/Sources/TripWizard/Generation/`
  - `Packages/ChronoFlowKit/Sources/TripWizard/Model/AirportSearchService.swift`
  - `Packages/ChronoFlowKit/Sources/TripWizard/Model/MemberWizardStore.swift`

**Delivers.** Adds the generation service (review, save, edit input) with a 90 s HTTP timeout, the review operation capped at 30 s and saves never retried automatically, plus the checking timers and the 250 ms choice delay. It adds airport search with 2–60 characters, a 350 ms debounce, request pacing of at least 1.02 s and an in-memory cache, and the recovery row (one fixed-id drafts row, deleted on close, save or expiry). It adds the TripWizardViewModel scaffold with its conflated writer, tested against mock scenarios.

**Tasks**
- [ ] **Generation** (`core/wizard/…/generation/`).
  - Review, save and edit-input calls, with a 90 s HTTP timeout and the review operation capped at 30 s. Saves are never retried automatically.
  - An in-flight map keyed by `(instance, fingerprint, token, reviewRevision, edit)`, guarded by a `Mutex`, and the expiry retry.
  - Checking timers: ticks at 0.4, 0.8 and ≥ 1.2 s, a 0.3 s pause, slow at 10 s, failure at 30 s.
- [ ] **Airport search**, porting `AirportSearchService`.
  - The query is trimmed and must be 2–60 characters long.
  - A 350 ms debounce, with stale searches cancelled.
  - Requests are spaced at least 1.02 s apart, with a wait after a 429.
  - An in-memory LRU cache.
  - The search states as a sealed type.
- [ ] **Recovery**, porting `MemberWizardStore`: one fixed-id `drafts` row (OQ 40), written through a conflated writer and deleted on close, save or expiry (30 min). Add the draft halves of iOS `RecordsTests` (the `DraftRecord` round trip and the draft upsert/remove).
- [ ] **TripWizardViewModel scaffold** (`feature/wizard/…`). The state holder, plus the cancellable jobs `checkJob`, `reviewJob`, `saveJob`, `choiceJob` (250 ms) and `retryCountdownJob`.
- [ ] **Tests** against `MockDataSource` scenarios and the `wizard/*.json` fixtures: throttled, capacity, expired, needs-answers and unknown-field.

**Acceptance**
- The timer, pacing and recovery cases pass on a fake clock.
- A save is never retried without a user action.

### Phase 9c: Wizard shell and Where (`feat/phase-9c-wizard-shell-where`)

- **Brief:** [`docs/phases/phase-9c-wizard-shell-where.md`](docs/phases/phase-9c-wizard-shell-where.md), written by the orchestrator before the phase starts.
- **Depends on:** 9b, 6a. **Wave:** 9.
- **Research:**
  - `docs/research/06-wizard.md` §2.1–§2.3, §3.1–§3.4, §5.2, §9.2, §9.4, §9.6–§9.7, §9.9
- **Boards:** `Wizard`, `WizardCopy`, `WizardWhere`, `WizardWhereDark`, `WizardWhereNL`, `WizardWhereEmpty`, `WizardWhereEmptyDarkNL`, `WizardWhereSame`, `WizardWhereStop`, `WizardWhereStopDarkNL`, `WizardWhereStops`, `WizardWhereStopsDarkNL`, `WizardHomeRoute`, `WizardHomeRouteDarkNL`, `WizardHomeRouteStop`, `WizardHomeRouteStopDarkNL`, `WizardAirportFrom`, `WizardAirportFromDarkNL`, `WizardAirportHome`, `WizardAirportHomeDarkNL`, `WizardStopSearch`, `WizardStopSearchDarkNL`, `WizardStopSearchSecond`, `WizardStopSearchSecondDarkNL`, `WizardStopSearchHome`, `WizardStopSearchHomeDarkNL`, `TripAirportSearch`, `TripSearchStates`, `WizardDiscard`, `WizardDiscardDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1822` "16b.2: Route first, then times (canvas redesign, 2026-11-09)"
  - `plan.md:1791` "16a: Model, Where and Outbound"
  - `plan.md:1807` "16a–b board-fidelity follow-up (2026-11-09)"
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`
  - `Docs/wizard-fidelity.md`

**Delivers.** Builds the full-screen wizard shell: a centered top bar with close and back, the 4 dp dawn progress bar at the board percentages, a fixed footer with IME padding and a 520 dp column on wide windows, plus the discard alert and the quiet recovery. It builds Where with From, To, stops, swap, the same-airport error and the flight-home route, and the full-height airport and stop search sheets with all search states. New trip on My trips opens it.

**Tasks**
- [ ] **Shell** (`feature/wizard/…/shell/`).
  - A full-screen route that replaces the tab shell.
  - A centred top bar with close and back; the first screen has no back.
  - A 4 dp `CfLinearProgress` at the board percentages.
  - A fixed footer with `imePadding()`.
  - A 520 dp column from 669 dp (`WizardCopy`).
  - The discard alert (`WizardDiscard`), and quiet recovery when the wizard is reopened.
- [ ] **Where** (`WizardWhere*`): From, To, stops, swap, the same-airport error, and the flight-home route (`WizardHomeRoute`, `WizardHomeRouteStop`).
- [ ] **Search sheets.** Full-height `CfModalBottomSheet`s for airport and stop search (`WizardAirportFrom`, `WizardAirportHome`, `WizardStopSearch`, `WizardStopSearchSecond`, `WizardStopSearchHome`, `TripAirportSearch`), with every `TripSearchStates` state.
- [ ] **Entry.** New trip on My trips opens the wizard. The `CF_WIZARD_AUTO_OPEN` and `CF_WIZARD_BOARD_CAPTURE` seeds work.
- [ ] **Tests.**
  - ViewModel cases for route editing through the UI state.
  - Guards for each board and its DarkNL wrapper.
  - On the emulator: the search sheet with the IME up keeps the field and the results visible.

**Acceptance**
- Every Where and search board, with its DarkNL wrapper, matches.
- Discard and quiet recovery behave as in WizardSpec.

### Phase 9d: Flight times and return (`feat/phase-9d-wizard-flights`)

- **Brief:** [`docs/phases/phase-9d-wizard-flights.md`](docs/phases/phase-9d-wizard-flights.md), written by the orchestrator before the phase starts.
- **Depends on:** 9c. **Wave:** 10.
- **Research:**
  - `docs/research/06-wizard.md` §2.1, §4.3, §5.3–§5.4, §9.5
- **Boards:** `WizardOutbound`, `WizardOutboundDark`, `WizardOutboundNL`, `WizardOutboundEmpty`, `WizardOutboundEmptyDarkNL`, `WizardOutboundErrors`, `WizardOutboundErrorsDarkNL`, `WizardOutboundTwoErrors`, `WizardOutboundTwoErrorsDarkNL`, `WizardOutboundStop`, `WizardOutboundStopDarkNL`, `WizardOutboundStopEmpty`, `WizardOutboundStopEmptyDarkNL`, `WizardOutboundOvernight`, `WizardOutboundOvernightDarkNL`, `WizardOutboundThree`, `WizardOutboundThreeDarkNL`, `WizardOutboundPast`, `WizardOutboundPastDarkNL`, `WizardReturn`, `WizardReturnDark`, `WizardReturnNL`, `WizardReturnFlight`, `WizardReturnFlightDarkNL`, `WizardReturnFlightPast`, `WizardReturnFlightPastDarkNL`, `PickerDateTime`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1791` "16a: Model, Where and Outbound"
  - `plan.md:1800` "16b: Return, Sleep, Checking and Questions"
  - `plan.md:2376` "Open questions for the user" (OQ 46)
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`

**Delivers.** Builds the outbound and flight-home times steps. Separate date and time pills open a DatePickerDialog bounded to the airport's wall dates and a time dialog in which only Done writes; the steps add flight-time, connection and overnight chips, errors, three-flight scrolling with a fixed Continue, and the past-arrival banner. It builds the Coming back? choice. Pickers follow the phone's 12/24-hour setting while displayed values follow the app.

**Tasks**
- [ ] **Outbound and flight-home times** (`WizardOutbound*`, `WizardReturnFlight*`).
  - Separate date and time pills.
  - `CfDatePickerDialog` is bounded to the airport's wall dates. In the time dialog only Done writes, at minute precision.
  - Flight-time, connection and overnight chips.
  - Errors (`WizardOutboundErrors`, `WizardOutboundTwoErrors`).
  - Three flights scroll under a fixed Continue (`WizardOutboundThree`).
  - The past-arrival banner (`WizardOutboundPast`, `WizardReturnFlightPast`).
- [ ] **Coming back?** (`WizardReturn*`): it auto-advances after 250 ms, and taps are ignored while it is pending.
- [ ] **Picker formats.** Pickers follow `DateFormat.is24HourFormat`, displayed values follow the account `timeFormat`, and the dialogs use the app language (OQ 46, `PickerDateTime`).
- [ ] **Tests.**
  - Date and time bounds on the JVM.
  - Guards for each board.
  - On the emulator: the date and time dialogs keep the app language and the phone's 12/24-hour setting.

**Acceptance**
- The bounds and the 12/24-hour rules hold.
- Every outbound and return board, with its DarkNL wrapper, matches.

### Phase 9e: Sleep and questions (`feat/phase-9e-wizard-sleep-questions`)

- **Brief:** [`docs/phases/phase-9e-wizard-sleep-questions.md`](docs/phases/phase-9e-wizard-sleep-questions.md), written by the orchestrator before the phase starts.
- **Depends on:** 9d. **Wave:** 11.
- **Research:**
  - `docs/research/06-wizard.md` §2.1, §4.3, §9.5
  - `docs/research/07-day-to-day-settings.md` §6.5
- **Boards:** `WizardSleep`, `PickerTime`, `WizardSheetFreeSleep`, `WizardSheetFreeSleepDarkNL`, `WizardQAge`, `WizardQAgeDarkNL`, `WizardQShift`, `WizardQShiftNL`, `WizardQShiftLarge`, `WizardExcluded`, `WizardExcludedDarkNL`, `WizardExcludedShift`, `WizardExcludedShiftDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1800` "16b: Return, Sleep, Checking and Questions"
  - `plan.md:2477` "No health data — server Phase 19 (user, 2026-11-10)"
  - `Docs/no-health-data.md`
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`

**Delivers.** Builds the sleep step: usual bedtime and wake time with 5-minute rounding, and the free-day times sheet. It builds the only remaining questions, age band and shift work, including the large-text variant, and the excluded states for guests, saved shift-work answers and edits. Retired health questions are never built.

**Tasks**
- [ ] **Sleep** (`WizardSleep`).
  - Usual bedtime and wake time use the M3 `TimePicker` in a sheet, rounded to 5 min on Done (OQ-A8, `PickerTime`).
  - The free-day times sheet (`WizardSheetFreeSleep`).
- [ ] **Questions.** Age band (`WizardQAge`) and shift work (`WizardQShift`, `WizardQShiftNL`, `WizardQShiftLarge`). They auto-advance after 250 ms and use `role = RadioButton`.
- [ ] **Excluded states** (`WizardExcluded`, `WizardExcludedShift`), for guests, saved shift-work answers and edits.
- [ ] **No retired questions.** A test proves that no retired health question is referenced; `WizardQBipolar`, `WizardQCaffeine`, `WizardQClot`, `WizardQEye` and `WizardQPregnant` stay unbuilt.
- [ ] **Tests.** Rounding and validation, plus guards for each board.

**Acceptance**
- Values round to 5 min.
- Only the two remaining questions exist, and each board matches.

### Phase 9f: Checking and fine-tune (`feat/phase-9f-wizard-checking-finetune`)

- **Brief:** [`docs/phases/phase-9f-wizard-checking-finetune.md`](docs/phases/phase-9f-wizard-checking-finetune.md), written by the orchestrator before the phase starts.
- **Depends on:** 9e. **Wave:** 12.
- **Research:**
  - `docs/research/06-wizard.md` §2.1–§2.2, §3.5–§3.7, §9.6, §9.8
- **Boards:** `WizardChecking`, `WizardCheckingDark`, `WizardCheckingNL`, `WizardCheckingAgain`, `WizardCheckingAgainDarkNL`, `WizardCheckingSlow`, `WizardCheckingSlowDarkNL`, `WizardCheckOffline`, `WizardCheckOfflineDarkNL`, `WizardCheckServer`, `WizardCheckServerDarkNL`, `WizardCheckThrottle`, `WizardCheckThrottleDarkNL`, `WizardFineTune`, `WizardFineTuneDarkNL`, `WizardFineTuneDark`, `WizardFineTuneNL`, `WizardFineTuneMulti`, `WizardFineTuneMultiDarkNL`, `WizardFineTuneSleep`, `WizardFineTuneSleepDarkNL`, `WizardFineTuneBusy`, `WizardFineTuneBusyDarkNL`, `WizardFineTuneNone`, `WizardFineTuneNoneDarkNL`, `WizardFineTuneSheet`, `WizardFineTuneSheetDarkNL`, `WizardSheet`, `WizardSheetRoom`, `WizardSheetRoomDarkNL`, `WizardSheetCheckout`, `WizardSheetCheckoutDarkNL`, `WizardSheetWake`, `WizardSheetWakeDarkNL`, `WizardSheetBusy`, `WizardSheetBusyDarkNL`, `WizardSheetDrinks`, `WizardSheetDrinksDarkNL`, `WizardSheetDrinksError`, `WizardSheetDrinksErrorDarkNL`, `WizardSheetLight`, `WizardSheetLightDarkNL`, `WizardSheetBlue`, `WizardSheetBlueDarkNL`, `WizardSheetBlueError`, `WizardSheetBlueErrorDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1800` "16b: Return, Sleep, Checking and Questions"
  - `plan.md:1866` "16c: Fine-tune, Ready, Save and create acceptance"
  - `Docs/wizard-checking-t10.md`
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`

**Delivers.** Builds the Checking screen (canvas arc, tick timing, the slow line, the again ordering) and its offline, server and throttle failure cards with a countdown. It builds Fine-tune with its member rows, per-stay and per-flight rows, the different-sleep-times switch, and the busy and none states. It builds every fine-tune sheet (seat, room, checkout, wake, busy hours, drinks with steppers, light, blue blockers with rating), each with one nullable selection and its validation errors.

**Tasks**
- [ ] **Checking** (`WizardChecking*`).
  - A canvas arc driven by the 9b tick timing, the slow line at 10 s, and the again ordering (`WizardCheckingAgain`).
  - Failure cards for offline, server and throttle, with a countdown (`WizardCheckOffline`, `WizardCheckServer`, `WizardCheckThrottle`).
  - No back while checking.
  - TalkBack hears progress through a polite live region.
- [ ] **Fine-tune** (`WizardFineTune*`):
  - member rows, plus per-stay and per-flight rows;
  - the different-sleep-times switch (`WizardFineTuneSleep`);
  - the busy (`WizardFineTuneBusy`), none (`WizardFineTuneNone`) and multi (`WizardFineTuneMulti`) states.
- [ ] **Sheets** (`WizardSheet*`, `WizardFineTuneSheet`): seat, room, checkout, wake, busy hours, drinks with steppers (and its error), light, and blue blockers with a rating (and its error). Each holds one nullable selection and its validation error.
- [ ] **Haptics** per the WizardSpec "Haptics" section, through the `Haptics` interface.
- [ ] **Tests.** Timing on the JVM with a fake clock, plus guards for all 26 boards and their DarkNL wrappers.

**Acceptance**
- The checking timing matches `06 §3.5`.
- Every fine-tune board and sheet matches.

### Phase 9g: Ready and save (`feat/phase-9g-wizard-ready-save`)

- **Brief:** [`docs/phases/phase-9g-wizard-ready-save.md`](docs/phases/phase-9g-wizard-ready-save.md), written by the orchestrator before the phase starts.
- **Depends on:** 9f. **Wave:** 13.
- **Research:**
  - `docs/research/06-wizard.md` §2.1, §3.6, §4.4, §7.2–§7.3
- **Boards:** `WizardReady`, `WizardReadyDark`, `WizardReadyNL`, `WizardReadyReturn`, `WizardReadyReturnDarkNL`, `WizardReadyUnlimited`, `WizardReadyUnlimitedDarkNL`, `WizardReadyFree`, `WizardReadyFreeDarkNL`, `WizardReadyOneFree`, `WizardReadyOneFreeDarkNL`, `WizardReadyAdjusted`, `WizardReadyAdjustedDarkNL`, `WizardReadyChecking`, `WizardReadyCheckingDarkNL`, `WizardReadyNoCredits`, `WizardReadyNoCreditsDarkNL`, `WizardSaveOffline`, `WizardSaveOfflineDarkNL`, `WizardSaveServer`, `WizardSaveServerDarkNL`, `WizardCalculating`, `WizardCalculatingDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1866` "16c: Fine-tune, Ready, Save and create acceptance"
  - `plan.md:1888` "16c first simulator-run repairs (2026-11-10)"
  - `plan.md:1898` "16c wizard test harness rework (2026-11-10)"
  - `plan.md:2376` "Open questions for the user" (OQ 41, 43)
  - `Docs/wizard-save-journey-t8.md`
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`

**Delivers.** Builds Ready with every price state from the server credits (paid, unlimited, free, one free, adjusted, re-checking, no credits without any hand-off). It builds the save flow through Calculating, with the offline and server errors and the same-identity resume after process death. A saved trip opens its trip page awaiting sync and marks the first-trip reminders offer pending. One emulator journey creates a trip on the mock.

**Tasks**
- [ ] **Ready** (`WizardReady*`). The price states come from the server credits: paid, unlimited, free, one free, adjusted, re-checking, and no credits (with no website button, A14). Also the return variant.
- [ ] **Save.**
  - `WizardCalculating`, with the offline and server errors (`WizardSaveOffline`, `WizardSaveServer`).
  - After process death, the save resumes in Calculating with the same identity (`14 §3.4`).
  - No automatic retry.
- [ ] **After save.** Open the trip page with `awaitingSync = true`, and set the first-trip reminders offer to `pending`; 12c consumes it.
- [ ] **Tests.**
  - Price-state mapping from `wizard/credits.json` and the related fixtures.
  - Process-death resume: Robolectric recreates the screen with a fresh graph and the drafts row.
  - Guards.
  - One emulator journey: with `CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney`, a one-way review → save → the trip page.

**Acceptance**
- The journey creates a trip on the mock, and it opens awaiting sync.
- Every Ready and save board matches.

### Phase 9h: Edit and duplicate (`feat/phase-9h-wizard-edit-duplicate`)

- **Brief:** [`docs/phases/phase-9h-wizard-edit-duplicate.md`](docs/phases/phase-9h-wizard-edit-duplicate.md), written by the orchestrator before the phase starts.
- **Depends on:** 9g, 6d. **Wave:** 14.
- **Research:**
  - `docs/research/06-wizard.md` §2.1, §4.5
  - `docs/research/03-api-contract.md` §11
- **Boards:** `WizardEdit`, `WizardEditDarkNL`, `WizardEditChanged`, `WizardEditChangedDarkNL`, `WizardEditNoReturn`, `WizardEditNoReturnDarkNL`, `WizardEditReturn`, `WizardEditReturnNoCredits`, `WizardEditReturnNoCreditsDarkNL`, `WizardEditWeb`, `WizardEditWebDarkNL`, `WizardReturnWeb`, `WizardReturnWebDarkNL`, `WizardCheckingEdit`, `WizardCheckingEditDarkNL`, `WizardCheckOfflineEdit`, `WizardCheckOfflineEditDarkNL`, `WizardCheckServerEdit`, `WizardCheckServerEditDarkNL`, `WizardExcludedEdit`, `WizardExcludedEditDarkNL`, `WizardDiscardEdit`, `WizardDiscardEditDarkNL`, `WizardDuplicate`, `WizardDuplicateDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1933` "17d: Edit and Duplicate"
  - `Packages/ChronoFlowKit/Sources/TripWizardUI/`
  - `Packages/ChronoFlowKit/Sources/TripWizard/Model/`

**Delivers.** Wires Edit and Duplicate from the trip actions sheet. Edit loads the trip input, reviews it with the source revision and saves with PUT, covering every edit variant (changed elsewhere, return removed or added, no credits, web trip ending), the edit checking and failure cards, excluded-on-edit and the discard-changes alert. Duplicate opens on the times step with empty dates and the copied-from line.

**Tasks**
- [ ] **Edit**, from the trip actions sheet.
  - Load the input snapshot, review it with the source revision, and save with `PUT trips/{id}`.
  - Variants: `WizardEdit`, `WizardEditChanged`, `WizardEditNoReturn`, `WizardEditReturn`, `WizardEditReturnNoCredits`, `WizardEditWeb` and `WizardReturnWeb`.
  - The edit checking and failure cards (`WizardCheckingEdit`, `WizardCheckOfflineEdit`, `WizardCheckServerEdit`) and `WizardExcludedEdit`.
  - The discard-changes alert (`WizardDiscardEdit`).
- [ ] **Duplicate** (`WizardDuplicate`): opens on the times step with empty dates and the copied-from line.
- [ ] **Tests.**
  - The ported 17d cases.
  - Guards.
  - One emulator journey: edit a trip → save → the trip page shows the new revision.

**Acceptance**
- Edit uses the source revision, and a changed-elsewhere trip shows `WizardEditChanged`.
- Duplicate starts with empty dates.

### Phase 10a: Settings home, sign-out and account pages (`feat/phase-10a-settings-home`)

- **Brief:** [`docs/phases/phase-10a-settings-home.md`](docs/phases/phase-10a-settings-home.md), written by the orchestrator before the phase starts.
- **Depends on:** 6a, 4e. **Wave:** 9.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §6.1–§6.3
  - `docs/research/05-launch-auth.md` §7
- **Boards:** `SetHome`, `SetHomeNL`, `DarkSetHome`, `SetStates`, `SetStatesMore`, `SignOutPending`, `SignOutPendingSettings`, `SetAccount`, `SetProfile`, `SetLanguage`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1942` "17e: Settings home and sign-out"
  - `plan.md:1993` "17h: Settings pages"
  - `plan.md:2040` "Settings board fidelity (2026-11-09)"
  - `plan.md:2376` "Open questions for the user" (OQ 29)
  - `Packages/ChronoFlowKit/Sources/Settings/SettingsHomeState.swift`
  - `Docs/phase-17e-settings.md`
  - `Docs/phase-17h-settings-pages.md`
  - `Docs/settings-board-fidelity.md`

**Delivers.** Builds the Settings tab home (the initials card, the live sync line with its age table, rows with subtitles and badges, the sign-out pill) and the sign-out-with-pending-changes sheet. It adds the shared settings states: loading, account not on this phone, saved and failed messages, refused change, and rows disabled offline. It adds the Account, Profile and Language pages; a language change goes through LocaleManager or the device-config file and recreates the activity. Wide windows use a 560 dp column.

**Tasks**
- [ ] **Home** (`feature/settings/…/home/`, a `SettingsHomeState` port).
  - The initials card.
  - The live sync line with its age table (`SettingsHomeState.swift:113-140`).
  - Rows with subtitles and badges, and the sign-out pill.
  - From 600 dp, a 560 dp column (OQ-A37).
- [ ] **Sign out with pending changes** (`SignOutPending`, `SignOutPendingSettings`): Stay or Discard, through `SessionController.signOut`.
- [ ] **Shared states** (`SetStates`, `SetStatesMore`): loading, account not on this phone, saved and failed messages (Snackbar), refused change, and rows disabled offline.
- [ ] **Account** (`SetAccount`), **Profile** (`SetProfile`) and **Language** (`SetLanguage`).
  - The language choice is `system`, `en` or `nl`. It goes through `LocaleManager` on API 33+, or the `device-config` file below 33, and recreates the activity.
  - With `system`, a later change of the phone language is followed (OQ-A25).
- [ ] **Tests.**
  - The ported 17e and 17h cases.
  - Guards.
  - One emulator journey: sign out with a pending change → Stay → Discard.
  - One emulator check that a language change switches the copy.

**Acceptance**
- Sign-out with pending changes matches both `SignOutPending` boards.
- The language switch works on API 29 and 36, through Robolectric and the `pixel` emulator.

### Phase 10b: Preferences and body clock (`feat/phase-10b-settings-preferences-body-clock`)

- **Brief:** [`docs/phases/phase-10b-settings-preferences-body-clock.md`](docs/phases/phase-10b-settings-preferences-body-clock.md), written by the orchestrator before the phase starts.
- **Depends on:** 10a, 8, 9e. **Wave:** 12.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §6.5–§6.6
  - `docs/research/03-api-contract.md` §13
- **Boards:** `SetPreferences`, `SetPreferencesNL`, `SetBodyClock`, `DarkSetBodyClock`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1993` "17h: Settings pages"
  - `plan.md:2477` "No health data — server Phase 19 (user, 2026-11-10)"
  - `Docs/no-health-data.md`
  - `Packages/ChronoFlowKit/Sources/Settings/SettingsPreferencesView.swift`
  - `Packages/ChronoFlowKit/Sources/Settings/SettingsDevicePreferences.swift`

**Delivers.** Builds Preferences: theme pills stored per device, the account time format and hidden categories saved through the outbox, the reminders row and Replay walkthrough. It builds Body clock with its readiness variants and sections (about, sleep, caffeine, light, health), validation, pending-value protection and the needs-an-answer badges. Everything follows the no-health-data rules.

**Tasks**
- [ ] **Preferences** (`SetPreferences`, `SetPreferencesNL`).
  - Theme pills: a device setting in DataStore, mirrored to `UiModeManager` on API 31+.
  - Time format and hidden categories: account settings, saved through the outbox.
  - The reminders row, which routes to 12c (a placeholder until then).
  - Replay walkthrough, through `Walkthrough(replay = true)`.
- [ ] **Body clock** (`SetBodyClock`, `DarkSetBodyClock`).
  - Readiness variants and the sections about, sleep, caffeine, light and health, under the no-health-data rules.
  - Validation.
  - Pending values are protected from a server overwrite (the 4d upsert).
  - The needs-an-answer badges.
- [ ] **Tests.**
  - The ported cases.
  - Guards.
  - One emulator check: a theme change applies without a restart, and on API 31+ the next cold start's splash matches it.

**Acceptance**
- Theme, time format and hidden categories persist across a restart, and the account values sync.
- The Body clock sections and badges match their boards.

### Phase 10c: Password, password prompt and two-factor (`feat/phase-10c-settings-security`)

- **Brief:** [`docs/phases/phase-10c-settings-security.md`](docs/phases/phase-10c-settings-security.md), written by the orchestrator before the phase starts.
- **Depends on:** 10a. **Wave:** 10.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §6.4
  - `docs/research/05-launch-auth.md` §13
  - `docs/research/03-api-contract.md` §7
  - `docs/research/14-gaps.md` §1.2–§1.3
- **Boards:** `SetSecurity`, `SetPassword`, `PasswordPrompt`, `PasswordPrompt2FAOn`, `PasswordPromptChecking`, `PasswordPromptForgot`, `PasswordPromptLocked`, `PasswordPromptNewCodes`, `PasswordPromptNL`, `PasswordPromptWide`, `PasswordPromptWrong`, `Set2FA`, `Set2FANL`, `Set2FASetup`, `Set2FAOn`, `Set2FAManage`, `Set2FAManageNL`, `Set2FAManageWide`, `Set2FACodes`, `Set2FAWide`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1993` "17h: Settings pages"
  - `plan.md:2040` "Settings board fidelity (2026-11-09)"
  - `Packages/ChronoFlowKit/Sources/Settings/`

**Delivers.** Builds Security, the password change, and the shared Confirm-it's-you password prompt in every state (checking, wrong, locked countdown, forgot), shown as a sheet or as a 560 dp dialog on wide windows. It builds two-factor setup, on, manage and recovery codes, including the wide variants. Secret screens use FLAG_SECURE, copied codes go to a sensitive clipboard entry cleared after 60 s, and Download saves the codes through the document picker.

**Tasks**
- [ ] **Security** (`SetSecurity`) and **password change** (`SetPassword`), with the server password policy (OQ 21).
- [ ] **Password prompt** (`PasswordPrompt*`).
  - A sheet on phones; on wide windows a 560 dp dialog, at most 90% of the height (`PasswordPromptWide`).
  - States: checking, wrong, locked with a countdown, forgot, 2FA on, new codes, and NL.
- [ ] **Two-factor** (`Set2FA*`).
  - The intro, with the Android copy (OQ-A12).
  - Setup: the QR code, drawn on the device from `otpauthUrl` on a white tile (`03 §7`), and the setup key.
    - Add `com.google.zxing:core` (Apache-2.0) to the catalog, pinned to the latest stable release that is at least two weeks old.
  - The on and manage states, and the recovery codes.
  - `FLAG_SECURE` on setup and the codes (OQ-A13).
  - Copy uses `ClipData` with `EXTRA_IS_SENSITIVE` on API 33+, cleared after 60 s.
  - Download uses `CreateDocument("text/plain")` with the name "Recovery codes.txt", UTF-8 without a BOM and with LF line ends; the fallback is `ACTION_SEND`.
  - The wide variants.
- [ ] **Secrets.** Nothing secret goes into saved state. Setup and recovery codes live only in the ViewModel (`14 §3.4`).
- [ ] **Tests.**
  - The ported cases.
  - Guards for all 20 boards.
  - On the emulator: saving through the document picker, and the `FLAG_SECURE` window flag.

**Acceptance**
- No secret survives in a `Bundle`; this is tested with a recreation.
- `FLAG_SECURE` is set on the setup and codes screens.
- All 20 boards match.

### Phase 10d: Passkeys, delete account and plan & credits (`feat/phase-10d-settings-account-plan`)

- **Brief:** [`docs/phases/phase-10d-settings-account-plan.md`](docs/phases/phase-10d-settings-account-plan.md), written by the orchestrator before the phase starts.
- **Depends on:** 10c, 5b. **Wave:** 11.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §6.3–§6.4, §6.7
  - `docs/research/03-api-contract.md` §9.3, §14
- **Boards:** `SetPasskeys`, `SetPasskeysEmpty`, `SetDelete`, `PasswordPromptDelete`, `SetPlan`, `SetPlanCredits`, `SetPlanLoading`, `SetPlanNoLink`, `SetPlanOffline`, `DarkSetPlan`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2055` "Passkey management addendum (2026-11-12, user request: build it now though it cannot be verified yet)"
  - `plan.md:1993` "17h: Settings pages"
  - `plan.md:2376` "Open questions for the user" (OQ 41)
  - `Packages/ChronoFlowKit/Sources/Settings/`

**Delivers.** Builds passkey management behind PasskeyCapability: the list, the empty state, adding through Credential Manager, removing with the password, and its messages and errors. It builds account deletion: the sheet, the password prompt, the DELETE call, the wipe and the billing error copy. It builds Plan & credits in all its states, with the NoLink copy and no purchase or website hand-off.

**Tasks**
- [ ] **Passkeys** (`SetPasskeys`, `SetPasskeysEmpty`), behind `PasskeyCapability`.
  - The list and its empty state.
  - Add through `CredentialManager.createCredential(CreatePublicKeyCredentialRequest(json))` (rpId `app.chronoflow.net`).
  - Remove after the password prompt.
  - Messages, errors, and the Android copy (OQ-A12).
- [ ] **Delete account** (`SetDelete`, `PasswordPromptDelete`).
  - The sheet → the password prompt → `DELETE` → a wipe through `SessionController` → Welcome.
  - The billing error copy.
  - The verified-email limit (Risk 4, S4).
- [ ] **Plan & credits** (`SetPlan`, `SetPlanCredits`, `SetPlanLoading`, `SetPlanNoLink`, `SetPlanOffline`, `DarkSetPlan`).
  - Balances from the server.
  - The NoLink copy, with no purchase or website hand-off (A14).
  - Pull to refresh.
- [ ] **Tests.**
  - The ported cases.
  - Passkey JSON with the fake `CredentialClient`.
  - Guards.
  - On the emulator: the delete flow on the mock ends on Welcome, with the account directory gone.

**Acceptance**
- Deleting the account wipes it and ends on Welcome.
- Every passkey entry stays hidden while the capability is false.
- Plan & credits shows no purchase path.

### Phase 11a: Guest wizard (`feat/phase-11a-guest-wizard`)

- **Brief:** [`docs/phases/phase-11a-guest-wizard.md`](docs/phases/phase-11a-guest-wizard.md), written by the orchestrator before the phase starts.
- **Depends on:** 9g, 3c. **Wave:** 14.
- **Research:**
  - `docs/research/06-wizard.md` §10
  - `docs/research/05-launch-auth.md` §12
  - `docs/research/03-api-contract.md` §10
- **Boards:** `WizardWhereGuest`, `WizardOutboundGuest`, `WizardOutboundPastGuest`, `WizardOutboundPastGuestDarkNL`, `WizardCheckingGuest`, `WizardFineTuneGuest`, `WizardReadyGuest`, `WizardReadyGuestDarkNL`, `WizardReadyGuestFree`, `WizardReadyGuestFreeDarkNL`, `WizardDiscardGuest`, `WizardDiscardGuestDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2062` "Phase 18: Guest wizard and claim (`feat/phase-18-guest-wizard`)"
  - `plan.md:2094` "18b: Guest review contract and API"
  - `plan.md:2104` "18c: Mock guest review"
  - `plan.md:2124` "18e: Guest secret store"
  - `plan.md:2128` "18f: Guest recovery store"
  - `plan.md:2137` "18g: Wizard mode logic"
  - `plan.md:2147` "18h: Guest generation"
  - `plan.md:2172` "18k: Guest screens, Where to Questions"
  - `plan.md:2179` "18l: Guest screens, Fine-tune and Ready"
  - `plan.md:2187` "18m: Quiet recovery from Welcome (OQ 17)"
  - `plan.md:2376` "Open questions for the user" (OQ 17, 44)
  - `Packages/ChronoFlowKit/Sources/AppCore/GuestSecretStore.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/GuestSessionContinuation.swift`
  - `App/GuestWizardDestination.swift`
  - `App/Readiness/KeychainGuestSecretStore.swift`

**Delivers.** Opens the wizard in guest mode from Welcome's Plan my trip, with every guest variant, the guest review endpoint, the guest secret in the Keystore and the guest draft file. Discard returns to Welcome, and guest-draft recovery happens quietly after the Welcome intro. Tests port the iOS guest-mode logic and guard every guest board.

**Tasks**
- [ ] **Guest mode** (`:core:wizard` and `feature/wizard`).
  - Entered from Welcome's Plan my trip.
  - The guest variants: `WizardWhereGuest`, `WizardOutboundGuest`, `WizardOutboundPastGuest`, `WizardCheckingGuest`, `WizardFineTuneGuest`, `WizardReadyGuest`, `WizardReadyGuestFree` and `WizardDiscardGuest`.
  - Questions are excluded for guests, per 9e.
- [ ] **Guest review endpoint.** `POST guest/trips/review` (90 s) in `ApiDataSource` and in the mock (iOS 18b, 18c; mock side ported in 4c, OQ-A165).
- [ ] **GuestSecretStore:** Keystore alias `<ns>.guest` plus a DataStore key (06 OQ 12). The guest draft file is `noBackupFilesDir/<ns>/guest-wizard.json` (18f).
- [ ] **Discard and recovery.** Discard returns to Welcome. The guest draft is recovered quietly after the Welcome intro (OQ 17, 18m).
- [ ] **Tests.** The ported 18b–18h and 18k–18m cases, plus guards for every guest board and its DarkNL wrapper.

**Acceptance**
- A guest reaches guest Ready on the mock.
- The guest draft survives a process kill and is recovered quietly.

### Phase 11b: Guest sign-up and claim (`feat/phase-11b-guest-signup-claim`)

- **Brief:** [`docs/phases/phase-11b-guest-signup-claim.md`](docs/phases/phase-11b-guest-signup-claim.md), written by the orchestrator before the phase starts.
- **Depends on:** 11a, 5d. **Wave:** 15.
- **Research:**
  - `docs/research/05-launch-auth.md` §12
  - `docs/research/03-api-contract.md` §10
  - `docs/research/06-wizard.md` §10
- **Boards:** `GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`, `GuestExcluded`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2062` "Phase 18: Guest wizard and claim (`feat/phase-18-guest-wizard`)"
  - `plan.md:2115` "18d: Mock registration claim"
  - `plan.md:2158` "18i: Claim on sign-up, sign-in and passkey"
  - `plan.md:2165` "18j: Accepting a session with or without a trip"
  - `plan.md:2192` "18n: Integration and acceptance"
  - `Packages/ChronoFlowKit/Sources/AppCore/GuestClaim.swift`
  - `Packages/ChronoFlowKit/Sources/AppCore/GuestSessionContinuation.swift`
  - `App/AppTabs.swift` (l.341–369)

**Delivers.** Builds the guest sign-up sheet over guest Ready (a 520 dp column on wide windows) with its failed, retry and review states and the excluded guest state. Registration carries the claim, and the guest continuation is consumed after verification so the claimed trip opens. Credits always come from the server, and one emulator journey goes from the guest wizard to a claimed trip.

**Tasks**
- [ ] **Guest sign-up sheet** (`GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`) over guest Ready, with a 520 dp column on wide windows; plus `GuestExcluded`.
- [ ] **Claim**, porting `GuestClaim` (mock side ported in 4c, OQ-A165).
  - Register with the claim.
  - Claim on sign-in and on passkey sign-in (18i).
  - Consume `GuestSessionContinuation` after verification, so the claimed trip opens (18j; iOS `AppTabs.swift:341–369`).
- [ ] **Credits** come only from the server (OQ-A18).
- [ ] **Tests.**
  - The ported 18d, 18i, 18j and 18n cases.
  - Guards.
  - One emulator journey: guest wizard → sign up → verify through the mock inbox → the claimed trip page.

**Acceptance**
- The claimed trip opens after verification.
- The guest secret and draft are gone afterwards.

### Phase 12a: Reminder engine (JVM) (`feat/phase-12a-reminders-engine`)

- **Brief:** [`docs/phases/phase-12a-reminders-engine.md`](docs/phases/phase-12a-reminders-engine.md), written by the orchestrator before the phase starts.
- **Depends on:** 1a. **Wave:** 4.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §7.1–§7.2
  - `docs/research/09-rn-android-lessons.md` §9
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2212` "Phase 19: Local reminders (`feat/phase-19-reminders`)" (19b (l.2217))
  - `Docs/phase-19-reminders.md`
  - `Packages/ChronoFlowKit/Sources/Reminders/ReminderSelection.swift`
  - `Packages/ChronoFlowKit/Sources/Reminders/ReminderSchedule.swift`
  - `Packages/ChronoFlowKit/Sources/Reminders/ReminderLedger.swift`
  - `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json`
  - `Tools/ReminderParity/export-selection.cjs`

**Delivers.** Ports the reminder engine into :core:reminders. It covers selection over the 36 h lookback and the 48 h window, kinds and copy keys, 15 min coalescing, at most 6 a day and 60 in total, identifiers, the ledger and the account-level switches. The 110 iOS parity vectors pass unchanged on the JVM.

**Tasks**
- [ ] **Engine** (`core/reminders/…`).
  - `ReminderSelection`: a 36 h lookback and a 48 h window; kinds and copy keys; 15 min coalescing; at most 6 a day and 60 in total; a 5 s margin; expiry after 20 min.
  - `ReminderSchedule`.
  - `ReminderLedger`: identifiers and replay protection.
  - The account-level switches and the lead time (15, 30 or 60 min).
- [ ] **Parity.** Copy `selection.json` (110 vectors, 1.0 MB) into `core/testing/…/fixtures/reminders/` with its hash. `ReminderParityTest` runs every vector unchanged. Record the exporter command (`Tools/ReminderParity/export-selection.cjs`) for a re-pin.
- [ ] **Copy.** Notification copy in en and nl, through the 0c strings and the account time format.

**Acceptance**
- 110 of 110 parity vectors pass on the JVM.
- `:core:reminders` has no Android import.

### Phase 12b: Reminder scheduling and delivery (`feat/phase-12b-reminders-scheduler`)

- **Brief:** [`docs/phases/phase-12b-reminders-scheduler.md`](docs/phases/phase-12b-reminders-scheduler.md), written by the orchestrator before the phase starts.
- **Depends on:** 12a, 4e, 5c, 6d. **Wave:** 12.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §7.2–§7.3
  - `docs/research/14-gaps.md` §6–§7
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2212` "Phase 19: Local reminders (`feat/phase-19-reminders`)" (19c (l.2218))
  - `Packages/ChronoFlowKit/Sources/Reminders/ReminderService.swift`
  - `Packages/ChronoFlowKit/Sources/Reminders/SystemReminderCenter.swift`
  - `Packages/ChronoFlowKit/Sources/Reminders/ReminderTap.swift`
  - `App/Session/ReminderTimeObservers.swift`
  - `App/AppTabs.swift` (l.370–383)

**Delivers.** Schedules the engine's selection with AlarmManager alarms (exact on API 29–30, inexact on 31+; A13) recorded in the account database, and posts notifications on the reminders channel with a 20 min timeout. A 12 h refill and expedited rescheduling on boot, time, zone, locale and package changes keep the window full. Taps route to the trip page and block after an account check, the sign-out teardown cancels everything, and the permission state model is exposed to the UI.

**Tasks**
- [ ] **Scheduler** (`core/data/…/reminders/`).
  - `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` on API 29–30 and `setAndAllowWhileIdle(RTC_WAKEUP, …)` on API 31+ (A13) for each selected reminder, with a `scheduled_reminder` row and a request code that stays stable per identifier.
  - Replace-all scheduling runs serialized after each full sync, check-off, undo and settings change, never on the main thread.
- [ ] **Delivery.**
  - `ReminderAlarmReceiver` posts on channel `reminders` ("Reminders on this phone", `IMPORTANCE_DEFAULT`, re-localized at start), with `notify(tag = identifier, id = requestCode)`, `setTimeoutAfter(20 min)` and `ic_stat_moon`.
  - Inside `goAsync`, it deletes the row and appends to the ledger.
- [ ] **Upkeep.**
  - Receivers for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` and `LOCALE_CHANGED` enqueue the expedited unique work `reminders-reschedule` (REPLACE).
  - A 12 h periodic refill (OQ-A14).
  - WorkManager is initialized on demand through `Configuration.Provider`, with the default initializer removed, so it adds no startup work.
- [ ] **Taps.** An explicit `PendingIntent` to `MainActivity` carries `tripId`, `blockId`, `inputHash` and `accountId`. It is routed after an account check, as in iOS `AppTabs.swift:370–383`.
- [ ] **Teardown.** Sign-out cancels every alarm and notification and erases the ledger, through the 4e hook.
- [ ] **Permission model.** `ReminderPermissionState` (granted, ask, blocked, unsupported), from `POST_NOTIFICATIONS`, `NotificationManagerCompat.areNotificationsEnabled()` and, below API 33, the in-app opt-in `notificationPermissionRequested` (A13).
- [ ] **Tests.**
  - JVM, with a fake alarm scheduler: selection → alarms, replacement, and the caps.
  - Robolectric `ShadowAlarmManager` and `ShadowNotificationManager` for delivery and teardown.
  - On the emulator: one alarm fires, and its tap opens the trip page at the block.

**Acceptance**
- Reboot, time-zone and time changes reschedule.
- Sign-out leaves no alarm or notification behind.
- The release manifest adds only `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and `WAKE_LOCK`.

### Phase 12c: Reminders page and first-trip offer (`feat/phase-12c-reminders-ui`)

- **Brief:** [`docs/phases/phase-12c-reminders-ui.md`](docs/phases/phase-12c-reminders-ui.md), written by the orchestrator before the phase starts.
- **Depends on:** 12b, 10b, 9g. **Wave:** 14.
- **Research:**
  - `docs/research/07-day-to-day-settings.md` §7.3–§7.4, §6.8
- **Boards:** `SetReminders`, `SetRemindersAsk`, `SetRemindersBlocked`, `DarkSetReminders`, `WizardReminders`, `WizardRemindersDarkNL`.
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:2212` "Phase 19: Local reminders (`feat/phase-19-reminders`)" (19d (l.2219), 19f (l.2223))
  - `Packages/ChronoFlowKit/Sources/Reminders/FirstTripRemindersOffer.swift`
  - `App/RemindersDestination.swift`

**Delivers.** Builds the Connectivity & reminders page in its on, ask, blocked and unsupported states: the master switch, the kinds, the lead time of 15, 30 or 60 min, and Open phone settings. It adds the POST_NOTIFICATIONS request flow. It adds the first-trip Turn on reminders? sheet over the trip page, consumed before it is shown.

**Tasks**
- [ ] **Connectivity & reminders page** (`feature/reminders/…`; `SetReminders`, `SetRemindersAsk`, `SetRemindersBlocked`, `DarkSetReminders`).
  - States: on, ask, blocked and unsupported.
  - The master switch, the kinds, and the lead time of 15, 30 or 60 min (native picker, per the 2026-11-09 decision).
  - Open phone settings, through `Settings.ACTION_APP_NOTIFICATION_SETTINGS`, with the blocked-step copy (OQ-A12).
  - Reached from Preferences (10b).
- [ ] **Permission flow.** `RequestPermission(POST_NOTIFICATIONS)` on API 33+, remembered in `notificationPermissionRequested`. Below API 33 there is no system dialog: **Turn on reminders** (the Reminders page or the first-trip offer) writes `notificationPermissionRequested`, which is the in-app opt-in; until then the state is "ask" and nothing is scheduled, as on iOS (A13).
- [ ] **First-trip offer** (`WizardReminders`), porting `FirstTripRemindersOffer`.
  - The settings row moves from `pending` to `seen` before the sheet shows.
  - The sheet shows once, over the first saved trip's page.
- [ ] **Tests.**
  - The ported 19d and 19f cases.
  - Guards.
  - On the emulator: the system permission dialog from the ask state, driven with UiAutomator.

**Acceptance**
- Every page state matches its board.
- The offer shows at most once, even after a process kill.

### Phase 13: Release build, profiles and performance gates (`feat/phase-13-release-perf`)

- **Brief:** [`docs/phases/phase-13-release-perf.md`](docs/phases/phase-13-release-perf.md), written by the orchestrator before the phase starts.
- **Depends on:** 5b, 5c, 6e, 7, 9h, 10d, 11b, 12c. **Wave:** 16.
- **Research:**
  - `docs/research/14-gaps.md` §9–§11
  - `docs/research/11-android-stack.md` §7
  - `docs/research/13-testing-tooling.md` §4.5
  - `docs/research/04-timeline.md` §10.5, §11
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1020` "Phase 7: Performance and comparison with RN (`feat/phase-7-performance`)"
  - `plan.md:2286` "Performance budget"
  - `plan.md:2248` "Release device entitlements fix (2026-11-09)"
  - `plan.md:2427` "Build and runner speed audit (user, 2026-11-09)"
  - `Docs/perf-timeline.md`

**Delivers.** Makes the release build shippable: signing placeholders, versioning, the R8 keep-rule review, the final manifest allowlist, no language splits and the size gate. It regenerates the baseline and startup profiles across the main journeys and runs the full performance budget (cold and warm start, launch jank, timeline, memory) on the emulator, and on devices when attached, compared with RN. It records the Data safety answers and the Play checklist status.

**Tasks**
- [ ] **Release build.**
  - Signing from `CF_UPLOAD_*` in `~/.gradle/gradle.properties`; no key in the repository.
  - A version bump rule in `version.properties`.
  - Review the R8 keep rules in `src/main/keepRules/*.keep` (serialization, Nav3 keys), with `-assumenosideeffects` for `Log.v`, `Log.d` and `Log.i`.
  - `bundle { language { enableSplit = false } }` (OQ-A39).
  - The final `verifyReleaseManifest` allowlist.
  - `make size`: at most 8 MB download, no `.so` except the OQ-0a-22 allow-list.
- [ ] **Profiles.** Extend `BaselineProfileGenerator` with the startup journey (launch → Today), the trip journey (`04 §10.5`), and the wizard up to Ready. Regenerate and commit; `BaselineProfileMode.Require` must pass.
- [ ] **Performance budget.**
  - Run every row of "Performance budget" on `pixel`, and on a reference and a low-end device when attached (OQ-A22).
  - Measure the RN release APK with the same `StartupBenchmark` harness (`12 §1`).
  - Record the results in `docs/perf-startup.md` and `docs/perf-timeline.md`.
  - A missed hard gate becomes a fix task in this phase, not a waiver.
- [ ] **Play readiness.** In `docs/play.md`:
  - the Data safety answers (`14 §9.2`, OQ-A41);
  - the status of checklist items P1–P11 (`14 §10`);
  - the `assetlinks.json` content for S1, with the release fingerprint only (OQ-A38).
- [ ] **Release readiness.**
  - `testReleaseUnitTest` and `verifyReleaseManifest` pass.
  - No debug strings or seeds ship (checked with `apkanalyzer dex`).
  - `ApplicationExitInfo` logging stays debug-only.

**Acceptance**
- Every functional and emulator row of the budget passes.
- Device rows pass, or are marked "open: device only". The pre-upload checklist in `docs/play.md` lists one passing `DEVICE_CLASS=reference make perf-timeline` (120 Hz) and one passing `DEVICE_CLASS=lowEnd` run, or the user's recorded waiver, as a blocker for the first production upload (OQ-A45).
- The bundle is within the size gate.
- `docs/play.md` is complete.

### Phase 14: Audit and verification (`feat/phase-14-audit-verification`)

- **Brief:** [`docs/phases/phase-14-audit-verification.md`](docs/phases/phase-14-audit-verification.md), written by the orchestrator before the phase starts.
- **Depends on:** 13. **Wave:** 17.
- **Research:**
  - `docs/research/14-gaps.md` §13–§14
  - `docs/research/13-testing-tooling.md` §6
  - `docs/research/12-built-apps.md` §3
- **Boards:** none (no UI).
- **iOS references** (`chronoflow-ios` at `f391a1fa`):
  - `plan.md:1013` "Phase 6: Audit (`feat/phase-6-audit`)"
  - `plan.md:1037` "Phase 8: Verification (`feat/phase-8-verification`)"
  - `plan.md:2040` "Settings board fidelity (2026-11-09)"
  - `Docs/settings-board-fidelity.md`
  - `Docs/wizard-fidelity.md`

**Delivers.** Audits the whole app against the boards and the iOS reference screenshots in docs/research/screens/ios, with seeded captures in both languages and themes, and checks the system chrome against the Material mapping. It runs the accessibility audit (TalkBack, Switch Access, keyboard, contrast, font scales 1.0, 1.3 and 2.0), the process-death and fold/split checks, the T2 device matrix once, a StrictMode-clean full run and the open device gates. It ends with a verification record and the list of remaining server dependencies.

**Tasks**
- [ ] **Board audit.**
  - `make captures` for every screen state, in en and nl, light and dark, at `pixel8`, `foldInner` and `largeFont`.
  - Compare against the boards and against `docs/research/screens/ios/`.
  - Fix each difference, or list it in `docs/audit.md` with its board and the reason.
- [ ] **Material chrome.** Capture every system component in light and dark, en and nl, for the designer to confirm (OQ-A42).
- [ ] **Accessibility.**
  - Compose accessibility checks (`enableAccessibilityChecks()`) on every screen.
  - Manual TalkBack, Switch Access and keyboard passes over the plan, auth and wizard screens.
  - Contrast.
  - Font scales 1.0, 1.3 and 2.0, where 2.0 must render as 1.3.
- [ ] **Robustness.**
  - Process death (`adb shell am kill` while in the background) on every top-level route ends in a cold launch.
  - Wizard recovery and save resume.
  - Fold, split and resize on the `fold` AVD.
  - A StrictMode-clean full emulator run.
  - The T2 matrix (`fold`, `small`, `api29`), once.
- [ ] **Verification record** `docs/verification.md`: the commands and results, the open device gates, the remaining server dependencies S1–S6, and the open questions still on their defaults.

**Acceptance**
- Every board state was captured and compared, and `docs/audit.md` lists every remaining difference.
- `docs/verification.md` is complete, and the T2 matrix passed.

## Review log (2026-11-16)

Lead-architect triage of the 30 adversarial findings on `plan.md`, AGENTS.md, CLAUDE.md and the 49 phase briefs. Every finding was checked against its sources: iOS at `f391a1fa`, the briefs, the Makefile and script specs, AOSP alarm behaviour, and the cached AGP 9.5.1 Lint checks.

**Verdict:** all 30 accepted, none rejected outright.
- Accepted only in part: F3, F22 and F29.
- Accepted with a changed fix: F6, F9, F15 and F18.

**Other outcomes:**
- Waves were recomputed from the new Depends lines. There are now 17, and the single-lane order stays valid.
- New open questions: OQ-A44–A47 in plan.md, OQ-0a-21 in 0a and OQ-0e-22 in 0e. The next free plan number is OQ-A48.

- **F1 (major, parity): accepted.**
  - Problem: below API 33 the mapping returned Granted with no user action, so reminders were scheduled at sign-in.
  - iOS schedules only when authorized and enabled (`ReminderSelection.swift:28`), and authorization starts out undetermined. The plan's "switch alone" rule was an Android-only deviation with no open question.
  - Fix: below API 33, `notificationPermissionRequested` becomes the in-app opt-in. Changed in A13 and the plan's 12b/12c entries. In 12b: the mapping, truth-table rows 8–12 and `ReminderPermissionsTest`. In 12c: the table, the request start, the first-trip edge, OQ-12c-8 and OQ-12c-15, and a new `PermissionRequestStartTest`.
- **F2 (minor, parity): accepted.**
  - iOS opens the step sheet when a row is tapped and opens the trip page from the hero card (`AppTabs.swift:443–448`).
  - Fix: corrected Phase 7's "Delivers" and Tasks in the plan, added OQ-A44, and pointed 7's OQ-7-1 to it.
- **F3 (major, perf): accepted in part.**
  - The gap is real: no emulator gate checked scroll smoothness, and device rows could stay open through release.
  - Added:
    - an emulator placeholder gate (≤ 6 per iteration);
    - a CPU P90/P99 regression band: the baseline is the median of the last three PASS rows of the same configuration and page, the limit is max(1.25×, +1 ms), there is one automatic re-run, and the checker exits 4 on a band-only miss;
    - per-thread report rows;
    - A12's 8 ms claim, marked as an aim;
    - OQ-A45: the first production upload needs one reference and one low-end run, or the user's waiver. Wired into 13's `docs/play.md` and 14's verification.
  - Rejected part: emulator gates on GC count and replay max. No measured baseline exists, and replay above 1 ms is a low-end trigger (OQ-T27), so both stay report-only or trend.
- **F4 (major, perf): accepted.**
  - B′ and C cannot fix a first-frame, memory or build-on-main miss.
  - New first-frame breakdown sections: `cf.timeline.decode`, `cf.timeline.prepare` and `cf.timeline.typeface`.
  - 1e Task 9 now maps each miss to the fix its evidence names. Fallbacks run only on replay or scroll evidence (A12, Risk 1, the plan's 1e entry, Task 10, OQ-1e-9).
  - 6b keeps the new sections.
- **F5 (major, perf): accepted.**
  - Fix: "Where gates apply" now holds the one rule: the gates, the band, and a ±25 % flag that never fails, measured against a per-page baseline.
  - The 1e checker gains a Page column and `timelinePage`. 6b sets it to `trip` and starts that baseline.
  - 6c, 6d, 6e, 7 and 13 copy the rule instead of a ±25 % gate.
- **F6 (major, perf): accepted with a changed fix.**
  - Wall-clock tests move to a serial `@TimingGate` tier: `make timing`, one test JVM on one Gradle worker, run last by `make quick` and `make test` (0e OQ-0e-22).
  - 1b keeps every-sample asserts, because its limits are budget rows, and gets three warm-ups.
  - 1c gates the medians of five passes; the max is reported only.
  - 1e re-sets 1c's limits from those medians.
- **F7 (minor, perf): accepted.**
  - Fix: 6b wraps `alpha` in `rememberUpdatedState` inside the derived state.
  - New test: `compactHeaderSemanticsFollowTheAnchor`.
- **F8 (minor, perf): accepted.**
  - The allowlist is `ProcessLifecycleInitializer` (A7's triggers; the plan's launch sequence registers its observer) and `ProfileInstallerInitializer`.
  - 3a measures both, removes the rest (for example EmojiCompat) and adds a `STARTUP_INITIALIZER` manifest rule.
  - Aligned: AGENTS, A21, the spec, 0a OQ-0a-10, 1d and 13. OQ-A47 records the emoji trade-off.
- **F9 (minor, perf): accepted.**
  - Fix: reference overrun P99 is now hard ≤ 2 ms, aim ≤ 0, so the 3 % jank limit can bind.
  - Changed in the plan, the 1e table and `budgets.json`.
- **F10 (minor, perf): accepted.**
  - New `cf.timeline.targets` section and `timelineTargetsMaxMs` metric, with a low-end aim of ≤ 1 ms.
  - Research `04` OQ-T25 is now plan OQ-A46.
- **F11 (major, data): accepted.**
  - On iOS, ON_STOP only disables sync: `SceneDelegate.swift:697–708` → `NativeSessionController.swift:152–163` → `SessionSync.swift:99–102`.
  - Fix: changed A7, the plan's 4e entry, OQ-A17, and the 4e brief (ON_START steps, ON_STOP, `SessionActivationTest`, acceptance 14). OQ-4e-9 is withdrawn.
- **F12 (minor, data): accepted.**
  - Added to A7 and the plan's 4d entry, from iOS `OutboxFlusher.swift:93–107` and `isStale` (l.143–150):
    - walkthrough rows are never refused;
    - stale check-offs are kept for rekeying;
    - `detail_version` is reset.
  - The 4d brief already had all three.
- **F13 (minor, data): accepted.**
  - Fix: the plan's 4e teardown now lists the iOS steps (`SessionController.swift:275–335`) and names 4e §5.3 as binding. 4e's mapping table is updated.
- **F14 (minor, data): accepted.**
  - The premise is stale: iOS at the pin already keeps pending edits (`SessionAccountDatabase.swift:29–48`, `StoreApplication.swift:51–60`).
  - Fix: updated "Login upsert", OQ-A40, the plan's 4d task, and the 4d brief (the Task 8 note and OQ-4d-4).
- **F15 (blocker, executability): accepted with a changed fix.**
  - Fix: 12b now depends on 5c and 6d, which puts it in wave 12.
  - Chosen over a fallback in the brief, because 12b's journey needs 6d's `step.sheet`.
- **F16 (major, executability): accepted.**
  - Fix: 9b depends on 4e (wave 8) and uses 4e's `AppGraphKey` factory pattern, not 5a's.
- **F17 (major, executability): accepted.**
  - Fix: 4c depends on 4b and 1b (wave 5). OQ-4c-2 is resolved.
- **F18 (major, executability): accepted with a changed fix.**
  - 1a owns `CheckOffChange`, and 4b depends on 1a (wave 4). 4b uses the type and never redeclares it.
  - 9e owns the time picker, and 10b depends on 9e (wave 12). 10b's Task 5 is removed.
  - CLAUDE.md gains a duplicate-symbol restack rule.
  - Not chosen: making 1a depend on 4b, because that would delay M1.
- **F19 (major, executability): accepted.** Fix: CLAUDE.md step 6 gains conflict rules:
  - generated outputs are regenerated, never merged by hand;
  - for additive wiring, both sides are kept;
  - for a duplicate symbol, the owning phase wins;
  - anything else goes back to the worker.
- **F20 (major, executability): accepted.**
  - Fix: a host-wide `fcntl` device lock (0a OQ-0a-21): `Scripts/device_lock.py`, `run_tier.py --device`, an `emulator.sh` re-exec, and a Makefile test.
  - Applied to every device tier in 0a, 0e, 1e, 2, 12c, 13, 14 and CLAUDE.md.
- **F21 (major, executability): accepted.**
  - Fix: the 1d fallback adds `:feature:trip:connectedDebugAndroidTest` and its results glob to `make check`'s smoke run.
  - Its acceptance checks that the test appears in `timings.json`.
- **F22 (major, executability): accepted in part.**
  - Already settled: where briefs are committed (0a OQ-0a-19). The 0a step 5 edit now also covers the "Delivers" line.
  - New: CLAUDE.md step 1 and the plan's "Tasks" preamble require each pre-written brief to be refreshed against the stack top before the worker starts.
- **F23 (minor, executability): accepted.**
  - The old `pgrep` could not see `run_tier.py`.
  - Fix: `Scripts/emulator.sh busy` replaces it, and it also checks the lock.
- **F24 (minor, executability): accepted.**
  - Fix: `make perf-timeline` is required from Phase 1e on. Changed in AGENTS, CLAUDE.md's review list and CLAUDE.md's Commands.
- **F25 (minor, executability): accepted.**
  - Fix: `landscapeSideNav` added to the plan's standard UI acceptance and to the 6c and 6d guard lists. The other briefs already had it.
- **F26 (minor, executability): accepted.**
  - Fix: CLAUDE.md step 2 requires absolute worktree paths and a branch preflight.
- **F27 (major, platform): accepted.**
  - lint-checks 32.x reports a fixed orientation as `DiscouragedApi`, which is on by default and becomes an error under `warningsAsErrors`.
  - Fix: 0a adds it to `tools:ignore` and records it.
- **F28 (major, platform): accepted.**
  - Fix: 12b keeps androidx.core's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (0a OQ-0a-20, 13) and removes only `FOREGROUND_SERVICE`.
- **F29 (major, platform): accepted in part.**
  - Fix:
    - API 29–30 use exact `setExactAndAllowWhileIdle`: no permission is needed there, and inexact alarms have no cap.
    - API 31+ stay inexact. The loss of up to 60 min is stated, and the expiry check drops a late reminder.
    - Changed in A13, the trade-offs, OQ-A15 and 12b.
  - Rejected part: editing research `14 §7`, which is read-only. A13 now says it supersedes that bullet.
- **F30 (minor, platform): accepted.**
  - `invisibleToUser` is deprecated in Compose UI 1.12.1 and fails `allWarningsAsErrors`.
  - Fix: `hideFromAccessibility()` replaces it in plan l.263 and in the plan's Phase 3a entry.
