# 11 — Android technology stack and tooling (verified 2026-11-15)

Scope: the toolchain, libraries, build layout, test/perf tooling, minSdk/targetSdk and the device/test matrix for the native Android ChronoFlow app (Kotlin + Jetpack Compose), which must behave exactly like the native iOS app at `/Users/rickgout/Sites/chronoflow-ios`. The trip **timeline** is the performance-critical screen.

Legend used throughout:

- **[V-web]** verified on 2026-11-15 against an official page or the artifact's Maven metadata (`dl.google.com/android/maven2`, `repo1.maven.org`, `plugins.gradle.org`) or GitHub releases.
- **[V-src]** verified in a local source file (path and line given).
- **[V-local]** verified on this Mac with a read-only command (`sdkmanager --list`, `avdmanager list device`, `java -version`, file listings).
- **[R]** recommendation for Android (my judgement, with the reason).
- **[OQ]** open question; the default in bold is what the plan should proceed with.

Nothing in the source repos (`chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile`) was modified. `sdkmanager --list` and `avdmanager list device` were run read-only.

---

## 0. Answer first: what the iOS research/plan gives us that is directly reusable for the Android stack

The user asked whether the iOS research/plan contains things that can be reused. For this topic (stack, tooling, tests, perf) the answer is yes; these are the parts to carry over almost one-to-one:

| iOS source [V-src] | What it decides | Android reuse [R] |
|---|---|---|
| `chronoflow-ios/AGENTS.md:30-58` "Tests" (user rule 2026-11-10) | Cheapest layer that can fail; logic in host tests, layout in hosted tests, XCUITest only for real-app things; one UI journey per feature; UI tests launch **seeded**; exact queries, backoff waits, no sleeps; budgets (UI test < 10 s, hosted < 5 s, 60 s hang allowance); run only the failing method while fixing | Same policy, mapped: JVM unit tests (pure Kotlin modules) → Robolectric/Compose UI tests on the JVM → instrumented Compose/UIAutomator tests on the emulator. Seeded launch via debug-only intent extras (see §9.4). Copy the budget numbers. |
| `chronoflow-ios/plan.md:29` D1c + `AGENTS.md:79` (user decision 2026-11-06: "skip the 7 simulator matrix unless asked for") | Device fit verified with pure device-table tests and per-profile snapshots inside **one** simulator; a small smoke set; full matrix only on request | Same: device-profile table tests + Roborazzi snapshots per profile on the JVM; one default emulator (`pixel`, API 36); foldable/small/minSdk AVDs opt-in (§10). |
| `chronoflow-ios/AGENTS.md:64-76` make targets (`quick`, `check`, `lint`, `format`, `test`, `snapshots`, `targeted`, `perf`, `matrix`, `duo`) | Inner-loop vs commit-gate tiers with time targets (<1 min warm / <5 min) | Same names as a `Makefile` wrapping Gradle tasks (§9.6), so agents and the user keep one vocabulary across both apps. |
| `chronoflow-ios/Docs/perf-timeline.md:69-97,157-186` | Timeline budgets: host prep S3 **40 ms**, 30 days **100 ms**, 60 days **200 ms** (≈2× headroom); hitch ratio aim **≤ 1 ms/s**; peak memory **150 MiB**; 512 KiB per-day drawing ceiling, 4 MiB/4-entry cache; custom signpost `time-to-first-timeline-frame`; fixtures `S1–S6`, `large60`, debug launch arg `-cf.trip-page S3` | Same budgets and fixtures. Signpost → `androidx.tracing` section measured by Macrobenchmark `TraceSectionMetric`; hitch ratio → `FrameTimingMetric` (`frameOverrunMs`) + JankStats; peak memory → `MemoryUsageMetric`. (§7) |
| `chronoflow-ios/plan.md:29` D9 ("`swift format` (toolchain). No SwiftLint.") | One formatter, no heavy linter | Spotless + ktlint (+ Compose rules) and Android Lint; **no detekt** until 2.0 is stable (§8). |
| `chronoflow-ios/plan.md:29` D10 (TokenGen from `tokens.json`, committed output, staleness test) | Generated design tokens, test fails when stale | Same pattern: a Gradle task/JVM tool reading `_design/canvas/ds/chronoflow/tokens.json` → Kotlin token file, committed, with a staleness test. |
| `chronoflow-ios/plan.md:29` D18–D25 | GRDB, one DB per account at a validated-UUID path, WAL + foreign keys, backup exclusion, mock vs API data source switch, Keychain session `{token, accountId}` WhenUnlockedThisDeviceOnly, sync engine, outbox, sign-out wipe | Library choices in §4 are made so each of these maps 1:1 (SQLDelight or Room, DataStore, Tink/Keystore, WorkManager, OkHttp with deny-by-default test interceptor). |
| `chronoflow-ios/plan.md:440-455` auth + passkey API contract | Password + passkey (`rpId:"app.chronoflow.net"`) login, 2FA, register, verify | Credential Manager (§4.6). Needs an Android `assetlinks.json` that does not exist yet (OQ-8). |
| `chronoflow-ios/plan.md:29` D15 (debug bundle `net.chronoflow.app.native` installs next to RN `net.chronoflow.app`) | Side-by-side install for measuring against RN | Same idea for `applicationId` (OQ-9). |

What does **not** carry over: XcodeGen, Swift concurrency annotations, Core Animation splash mechanics (D2–D5, D13), Liquid Glass (D1b). Android equivalents are listed where relevant.

---

## 1. Version table (stable as of 2026-11-15)

All versions below are **[V-web]** unless marked otherwise. "Pick" is the **[R]** version to pin in `gradle/libs.versions.toml`.

### 1.1 Build toolchain

| Component | Latest stable (date) | Newer pre-release | Pick | Notes |
|---|---|---|---|---|
| Android Gradle Plugin | **9.5.1** (9.5.0 → 9.5.1, Nov 2026) | — | 9.5.1 | AGP 9.5: max API **37**, min/default Gradle **9.6.0**, Build Tools 36.0.0, JDK 17 min/default, default NDK 28.2.13676358 (developer.android.com/build/releases/gradle-plugin). |
| Gradle | **9.9.1** (2026-11-10); 9.9.0 (11-06); 9.8.2 (10-19) | — | 9.9.1 (fallback 9.6.0) | AGP 9.5 lists 9.6.0 as min/default; 9.5.0-alpha09 fixed Gradle 9.8 deprecation noise, so 9.8+/9.9 is expected to work. Wrapper SHA-256 for 9.9.1: `d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`; 9.6.0: `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`. |
| Kotlin (K2) | **2.4.21** (2026-10-08) | — | 2.4.21 | Release cadence: 2.4.0 (06-03), 2.4.10 (07-14), 2.4.20 (09-07), 2.4.21 (10-08). K2 is the only compiler since 2.0. |
| KSP | **2.3.14** (2026-11-13) | — | 2.3.14 | KSP2 numbering is decoupled from Kotlin since 2.3.0; 2.3.10 notes "works with Kotlin 2.4.0 default module names" and fixes R-class resolution with AGP 9 built-in Kotlin; 2.3.12 raised min AGP to 8.12. |
| Compose compiler | = Kotlin version (plugin `org.jetbrains.kotlin.plugin.compose` 2.4.21) | — | 2.4.21 | Strong skipping on by default since Kotlin 2.0.20. |
| JDK | Local: **Zulu 17.0.21** only (`/Library/Java/JavaVirtualMachines/zulu-17.jdk`) [V-local] | — | Build on 17; **tests that use Robolectric SDK 36 need JDK 21** | Robolectric 4.16 notes: "you need to use JDK 21 if running tests with SDK 36 target". See OQ-3. |
| Android SDK | Platforms installed: `android-36`, `android-37.0`; build-tools 36.0.0, 37.0.0; emulator 37.2.13; platform-tools 37.0.1; cmdline-tools `latest` (23.0 available) [V-local] | `platforms;android-37.1`, `37.2` available | compileSdk **37**, targetSdk **36** | See §3. |
| NDK | `ndk;27.1.12297006` installed (from RN) [V-local] | — | not needed | App should have **no native code** (see §4.3 on framework SQLite) → no 16 KB page-size alignment work. |

Reference point **[V-src]**: the RN prebuild uses AGP 9.2.1, Kotlin 2.2.0, minSdk 24, targetSdk 36, compileSdk 37, build-tools 37.0.0 (`chronoflow-mobile/node_modules/react-native/gradle/libs.versions.toml:3-9,33`), Gradle 9.4.1 (`chronoflow-mobile/android/gradle/wrapper/gradle-wrapper.properties:3`), and opts out of AGP 9 built-in Kotlin and the new DSL (`chronoflow-mobile/android/gradle.properties:73-75`). The native app should **not** copy those opt-outs.

### 1.2 Libraries

| Area | Artifact | Stable | Pick / note |
|---|---|---|---|
| Compose BOM | `androidx.compose:compose-bom` | **2026.09.00** | Maps ui/foundation/animation/runtime/ui-test-junit4 **1.12.1**, material3 **1.4.0**, material3-adaptive **1.3.0**, material3-adaptive-navigation-suite **1.4.0** (read from the BOM POM). Compose 1.13.0-rc01 and material3 1.5.0-rc01 exist (2026-11-04) — do not adopt RCs. |
| Activity | `androidx.activity:activity-compose` | 1.13.0 | — |
| Core | `androidx.core:core-ktx` | 1.19.1 | — |
| Splash | `androidx.core:core-splashscreen` | 1.2.0 | System splash on API 31+, compat below. |
| Lifecycle | `androidx.lifecycle:*` | **2.11.0** (2026-10-21) | `lifecycle-runtime-compose` (`collectAsStateWithLifecycle`), `lifecycle-viewmodel-compose`, `lifecycle-viewmodel-navigation3` 2.11.0, `lifecycle-process`. |
| Navigation 3 | `androidx.navigation3:navigation3-runtime` / `-ui` | **1.2.1** (2026-11-04) | 1.3.0-alpha04 exists. |
| Navigation Compose 2.x | `androidx.navigation:navigation-compose` | 2.10.2 (2026-09-23) | Not chosen (§5). |
| Adaptive | `androidx.compose.material3.adaptive:adaptive`, `adaptive-navigation3` | 1.3.0 | `currentWindowAdaptiveInfo()`. |
| Navigation suite | `androidx.compose.material3:material3-adaptive-navigation-suite` | 1.4.0 | `NavigationSuiteScaffold`: bottom bar on phones, rail on wide windows (the Android analogue of iOS Duo's side rail, D1c). |
| Window | `androidx.window:window` | 1.5.1 | Fold posture if ever needed. |
| DI | `com.google.dagger:hilt-android` / `hilt-compiler` (KSP) | **2.60.1** | Hilt plugin requires AGP 9 + Gradle 9.1+ (since 2.59); 2.60 raised Hilt minSdk to **23** and built against Kotlin 2.3.21. |
| DI glue | `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.4.0 | `hiltViewModel()` without Navigation 2.x. `hilt-work` 1.4.0 for workers. |
| Alt DI | `io.insert-koin:koin-android` 4.2.2; `dev.zacsweers.metro` 1.5.2 | — | Not chosen (§4.1). |
| Database (option A) | `app.cash.sqldelight:android-driver` + Gradle plugin | **2.4.1** | **[R] default** (§4.3). |
| Database (option B) | `androidx.room3:room3-runtime` / `room3-compiler` (KSP) + plugin `androidx.room3` | **3.0.3** (3.0.0 stable 2026-07-01) | Room 2.x line: `androidx.room` 2.8.5. |
| SQLite | `androidx.sqlite:sqlite` | 2.7.1 | Only if Room 3. |
| Preferences | `androidx.datastore:datastore` / `datastore-preferences` | **1.3.0** (2026-10-21) | — |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.13.0** | Plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21. |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-*` | **1.11.0** | — |
| HTTP | `com.squareup.okhttp3:okhttp` | **5.5.1** | + `mockwebserver3` 5.5.1 for tests. |
| HTTP (optional) | `com.squareup.retrofit2:retrofit` / `converter-kotlinx-serialization` | **3.0.0** | Optional (§4.4). |
| HTTP (alt) | `io.ktor:ktor-client-okhttp` | 3.6.1 | Not chosen. |
| Credentials | `androidx.credentials:credentials` + `credentials-play-services-auth` | **1.6.0** | 1.7.0-alpha03 exists. |
| Crypto | `com.google.crypto.tink:tink-android` | **1.24.0** | Replaces EncryptedSharedPreferences. |
| Deprecated | `androidx.security:security-crypto` | 1.1.0 (2025-07-30) | **All APIs deprecated** since 1.1.0-alpha07 "in favour of existing platform APIs and direct use of Android Keystore". Do not use. |
| Background | `androidx.work:work-runtime-ktx` | **2.12.0** | — |
| Tracing | `androidx.tracing:tracing(-ktx)` | 2.0.5 | Custom trace sections (iOS signpost analogue). |
| JankStats | `androidx.metrics:metrics-performance` | **1.0.0** (2025-10-08) | — |
| Profile installer | `androidx.profileinstaller:profileinstaller` | 1.4.1 | — |
| Benchmark | `androidx.benchmark:benchmark-macro-junit4`, `benchmark-junit4`; plugin `androidx.baselineprofile` | **1.5.0** (2026-10-07) | 1.6.0-alpha01 exists. |
| UIAutomator | `androidx.test.uiautomator:uiautomator` | **2.4.1** (2026-11-04) | New `uiAutomator { onElement { … } }` DSL. |
| AndroidX Test | `androidx.test.ext:junit` | 1.3.0 | — |
| Robolectric | `org.robolectric:robolectric` | **4.18** (2026-11-13) | 4.17 added SDK 37; 4.18 drops SDK 23 (min 24), native resources on SDK 36+, Linux arm64 native runtime. |
| Roborazzi | `io.github.takahirom.roborazzi:*` | **1.79.0** (2026-11-12) | 1.78.0 changed golden PNG bytes once (RGB for opaque images) — record goldens on ≥1.78. |
| Preview scanner | `io.github.sergio-sastre.ComposablePreviewScanner:android` | 0.10.0 | Optional, for preview-driven snapshots. |
| Paparazzi | `app.cash.paparazzi` | 1.3.5 | Not chosen. |
| Google screenshot | Compose Preview Screenshot Testing engine | **0.0.1-alpha16** | Standalone plugin `com.android.compose.screenshot` is **"no longer maintained"**; replacement is AGP **test suites** (AGP ≥ 9.5, experimental). Not chosen (§9.2). |
| Flow tests | `app.cash.turbine:turbine` | 1.2.1 | — |
| Assertions | `com.willowtreeapps.assertk:assertk` 0.28.1 or `com.google.truth:truth` 1.4.5 | — | Pick assertk (Kotlin-first). |
| Mocks | `io.mockk:mockk` | 1.14.12 | Prefer fakes; MockK only at edges. |
| Leak checks | `com.squareup.leakcanary:leakcanary-android` | 2.14 | Debug only. |
| Format | `com.diffplug.spotless` 8.11.0; ktlint `com.pinterest.ktlint:ktlint-cli` **1.8.0**; Compose rules `io.nlopez.compose.rules:ktlint` 0.6.8 | — | — |
| detekt | `io.gitlab.arturbosch.detekt` **1.23.8** (2025-02-20, built on Kotlin 2.0.21); `dev.detekt` **2.0.0-alpha.6** (2026-08-04, Kotlin 2.4.10, AGP 9.3.1) | — | Not adopted (§8). |
| Architecture rules | `com.lemonappdev:konsist` | **0.18.1** (built with Kotlin 2.4.21); 1.0.0-beta.1 (2026-11-04) | 0.18.1 (§8). |
| Dependency hygiene | `com.autonomousapps.dependency-analysis` | 3.9.0 | Optional. |
| JDK provisioning | `org.gradle.toolchains.foojay-resolver-convention` | 1.0.0 | For the JDK 21 test toolchain (OQ-3). |
| Journeys (CLI) | Maestro | **2.12.1** (2026-11-02), installed at `/opt/homebrew/bin/maestro` [V-local] | Opt-in only (§9.5). |

---

## 2. AGP 9 facts that shape the build [V-web]

From the AGP 9.0 release notes (developer.android.com/build/releases/agp-9-0-0-release-notes) and later notes:

- **Built-in Kotlin** is on by default (`android.builtInKotlin` false → true). Do **not** apply `org.jetbrains.kotlin.android`; it fails with "no longer required". Configure with `kotlin { compilerOptions { … } }`; `jvmTarget` defaults to `android.compileOptions.targetCompatibility`. The opt-out disappears in AGP 10.
- **`kotlin-kapt` is incompatible** with built-in Kotlin → KSP only (Hilt, Room 3 are KSP-capable).
- **New DSL** on by default (`android.newDsl` → true): the legacy variant API (`applicationVariants`) is gone; use `androidComponents { onVariants { } }`.
- Default flips worth knowing: `android.uniquePackageNames=true` (every module needs a distinct namespace), `android.enableAppCompileTimeRClass=true`, `android.sdk.defaultTargetSdkToCompileSdkIfUnset=true` (so **always set targetSdk explicitly**, or it becomes 37), `android.onlyEnableUnitTestForTheTestedBuildType=true`, `android.proguard.failOnMissingFiles=true`, `android.r8.optimizedResourceShrinking=true`, `android.r8.strictFullModeForKeepRules=true` (`-keep class A` no longer keeps `<init>()`), `resValues` and `shaders` build features off by default, `getDefaultProguardFile()` only accepts `proguard-android-optimize.txt`.
- AGP 9 requires consumers to use a compileSdk ≥ each library's `minCompileSdk` → keep compileSdk at the newest installed platform (37).
- **AGP 9.3+** adds `optimization { enable = true }` on the release build type (turns on code optimization + optimized resource shrinking; no default keep file needed) and keep rules as source sets: `src/main/keepRules/*.keep`. The 9.3 bug "optimization DSL does not set isMinifyEnabled on ApplicationVariant" (issue 556343682) is listed fixed in **9.5.0-alpha06**. 9.3 also added `./gradlew :app:analyzeReleaseR8Config`.
- AGP 9.5.0-alpha09 adds experimental automated **backup/restore tests** (`android.experimental.testSuiteSupport=true`, `android.experimental.enableBackupTest=true`, `testOptions.backupTests`) — useful to prove the D19-style backup exclusion (§4.5).
- R8 full mode has been the default since AGP 8.0; keep it.

**KGP version under built-in Kotlin [R, verify at bootstrap]:** AGP 9.0 had a runtime dependency on KGP 2.2.10. To compile with Kotlin 2.4.21, put KGP 2.4.21 on the build classpath by declaring the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose` version 2.4.21, `apply false`) and the serialization plugin in the root/`build-logic` classpath; Gradle resolves the highest KGP. Verify with `./gradlew buildEnvironment | grep kotlin-gradle-plugin` in the first phase and record the result.

---

## 3. minSdk, targetSdk, compileSdk

### 3.1 Facts [V-web]

Cumulative device reach (Android Studio distribution data, 2026-10-01, via composables.com/android-distribution-chart):

| API | Version | Reach (this or newer) |
|---|---|---|
| 24 | 7.0 | 99.4% |
| 26 | 8.0 | 99.0% |
| 28 | 9 | 97.0% |
| 29 | 10 | 93.9% |
| 30 | 11 | 88.2% |
| 31 | 12 | 77.7% |
| 33 | 13 | 68.1% |
| 34 | 14 | 56.3% |
| 35 | 15 | 41.0% |
| 36 | 16 | 26.3% |
| 37 | 17 | 0.9% |

- AndroidX default `minSdk` is **24** ("meant to cover 99% of Android users"); Hilt 2.60 minSdk **23**; Robolectric 4.18 tests SDK ≥ **24**.
- **Play target API requirement:** new apps and app updates must target **Android 16 (API 36)** since **2026-08-31** (Play Console help 11926878; developer.android.com/google/play/requirements/target-sdk). API 37 is expected for Aug 2027 (not yet announced).
- **Passkeys** via Credential Manager need **Android 9 (API 28)+** (developer.android.com/identity/passkeys).
- Targeting API 36 (developer.android.com/about/versions/16/behavior-changes-16): edge-to-edge opt-out (`windowOptOutEdgeToEdgeEnforcement`) is disabled on Android 16; predictive-back system animations are on, `onBackPressed` is no longer called and `KEYCODE_BACK` is not dispatched; **on displays with smallest width ≥ 600 dp, `screenOrientation`, `resizeableActivity=false`, min/maxAspectRatio and `setRequestedOrientation()` are ignored** (temporary opt-out `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`, which stops working when targeting 37); the local-network permission is opt-in now and will be enforced later (relevant to LAN dev servers).

### 3.2 What each candidate minSdk unlocks

| minSdk | Reach | Unlocks (relevant to ChronoFlow) | Cost |
|---|---|---|---|
| 26 | 99.0% | `java.time` natively (no desugaring), adaptive icons (the canvas ships `icon/android/adaptive-{background,foreground,monochrome}`), notification channels | Must branch for passkeys (<28), no system dark mode toggle (<29), tzdata only updated by OEM OTA |
| 28 | 97.0% | Passkeys everywhere; Cloud Profiles (Play) | Same dark-mode/tzdata gaps |
| **29** | **93.9%** | Everything above, plus: system-wide **dark theme** (matches the iOS "both appearances follow the system"), **gesture navigation** and the modern inset model (iOS D1c "all four insets"), **TLS 1.3** by default, and **time-zone rules shipped as an updatable Mainline module** (APEX `com.android.tzdata`, Android 10+) — important for a jet-lag planner that converts between zones | Drops ~5% of devices (mostly Android 8–9) |
| 31 | 77.7% | Native SplashScreen API, `RenderEffect` blur, stretch overscroll, `dataExtractionRules`, exact-alarm permission model | Drops ~22% of devices — too many for a consumer travel app |

### 3.3 Recommendation [R]

- **minSdk 29** (OQ-1). Analogous to iOS D1 (iOS 17, dropping iPhone 8/X): a modern floor that removes whole branches (passkey availability by OS, dark-mode emulation, outdated tz rules) while keeping ~94% reach. Use `Build.VERSION.SDK_INT` branches only for 31+ (system splash, blur, data-extraction rules) and 33+ (per-app language, themed monochrome icon, notification permission).
- **targetSdk 36** (Play requirement). Plan for the sw ≥ 600 dp rule: the iOS app is portrait-only iPhone (D16), but on Android a foldable's inner display or a tablet will ignore a portrait lock. **Do not ship the temporary opt-out**; build layouts from window size (like D1c) and treat the unfolded foldable as a "wide phone" (centred max-width column + navigation rail), see OQ-5.
- **compileSdk 37** (installed; required by AGP 9 `minCompileSdk` rules as libraries move; RN already uses 37).
- Predictive back: implement with Navigation 3 (`NavDisplay` handles it) and `NavigationBackHandler`; never intercept `onBackPressed`.

---

## 4. Library decisions

### 4.1 Dependency injection: **Hilt** [R]

- Options: Hilt 2.60.1 (compile-time Dagger graph, Google default, KSP), Koin 4.2.2 (runtime service locator), Metro 1.5.2 (compiler-plugin DI), manual DI.
- Choose **Hilt**: graph errors at compile time; first-party integrations we need (`hiltViewModel()` from `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.4.0, `@HiltWorker` via `androidx.hilt:hilt-work` 1.4.0); test replacement with `@TestInstallIn`/`@UninstallModules` to swap the D20-style `DataSource` (mock vs API) without production code knowing. No runtime reflection cost at startup.
- Constraints: Hilt Gradle plugin ≥ 2.59 needs AGP 9 + Gradle 9.1; use `ksp("com.google.dagger:hilt-compiler")` (no kapt under built-in Kotlin). Keep pure-Kotlin core modules free of Hilt; they expose constructors annotated with `@Inject` (`javax.inject`) only, or are wired by `@Provides` in the app/data modules.
- ViewModels keyed by a Navigation 3 key: assisted injection, `hiltViewModel<TripViewModel, TripViewModel.Factory>(creationCallback = { it.create(key.tripId) })`, inside the `rememberViewModelStoreNavEntryDecorator()` so each back-stack entry owns its ViewModel store.
- Fallback if KSP/Hilt build time hurts the inner loop: manual composition root (the iOS app is effectively manual DI). Konsist rule keeps Hilt annotations out of `core:*` JVM modules so this stays possible.

### 4.2 Navigation: **Navigation 3 (1.2.1)** [R]

- Facts [V-web]: Navigation 3 is stable at 1.2.1 (2026-11-04). The back stack is app-owned state (`rememberNavBackStack(...)` of `NavKey`s, serializable), rendered by `NavDisplay` with an `entryProvider`; 1.1.0 added shared elements between scenes, `SceneDecoratorStrategy`, a type-safe metadata DSL, `OverlayScene.onRemoved` exit animations, and `List<SceneStrategy>`; 1.2.0 added `NavigationBackHandler` and `rememberNavigationEventState` for predictive back, a `Scene` equality lint, and `DeepLinkSerializer`. Entries under overlays are capped at `STARTED`. Navigation Compose 2.10.2 is still maintained.
- Why Nav3: it matches iOS D17 (a `Route` enum + `Navigator` protocol; "UI tests assert the route") directly — a `sealed interface Route : NavKey` plus a small `Navigator` that mutates a `SnapshotStateList<Route>`, testable on the JVM without a `NavController`. Scenes give the foldable/rail layout (`material3-adaptive-navigation3` 1.3.0) for free. Dialogs/bottom sheets (sign-out pending sheet, step sheets) become `OverlayScene`s.
- Tabs (Today / My trips / Settings, per RN `src/app/(app)/(tabs)/_layout.tsx` [V-src: `chronoflow-mobile/plan.md:939`]): one back stack per tab inside `NavigationSuiteScaffold`, preserving each tab's stack on reselect — mirrors iOS `AppTabs`.

### 4.3 Local database: **SQLDelight 2.4.1** (default) vs Room 3.0.3 [R + OQ-2]

- Facts [V-web]: Room 3.0 (package `androidx.room3`, stable 2026-07-01, now 3.0.3) is KSP-only, Kotlin-codegen-only, coroutines-only, built on `SQLiteDriver` (no `SupportSQLite` unless `room3-sqlite-wrapper`), Flow-based `InvalidationTracker.createFlow()`, `@ColumnTypeConverter` (renamed), `withoutRowId`, FTS5, schema export via plugin `androidx.room3 { schemaDirectory(...) }`. Room 2.x stays at 2.8.5.
- iOS reasoning [V-src `chronoflow-ios/plan.md:29` D18]: GRDB chosen for "explicit SQL parity, transactions and observation", native `v1` schema **equal to RN's final schema**, never replaying RN migrations 0000–0014, `journal_mode=WAL`, `foreign_keys=ON` on every connection, one DB per account at a validated-UUID path.
- **Default: SQLDelight** — schema is literally SQL (`.sq` files: paste RN's final `CREATE TABLE` statements), migrations as `.sqm` with `verifyMigrations`, type-safe Kotlin from SQL, `asFlow().mapToList(dispatcher)` observation, no KSP (faster builds, configuration-cache friendly). Use `AndroidSqliteDriver` (framework SQLite, **no native `.so`**, so no 16 KB page-size concern) with a `SupportSQLiteOpenHelper.Callback` that enables WAL (`setWriteAheadLoggingEnabled(true)`) and `setForeignKeyConstraintsEnabled(true)` in `onConfigure`. One driver per account file `files/accounts/<uuid>/account-<uuid>.db` (validated UUID only).
- Choose Room 3 instead only if the plan wants Google-first tooling (auto-migrations, Database Inspector integration is equal for both). Do **not** use Room's `BundledSQLiteDriver` (native lib).

### 4.4 Networking + JSON [R]

- **OkHttp 5.5.1 + kotlinx.serialization 1.13.0**, with a hand-written `ApiClient` per endpoint group (the iOS `APIDataSource` maps many per-endpoint error semantics: 422 envelopes, `retryAfterSeconds`/Retry-After on 429, 401 forced sign-out, challenge expiry — `chronoflow-ios/plan.md:440-455`). Retrofit 3.0.0 (+ official `converter-kotlinx-serialization` 3.0.0) is acceptable if the surface grows, but adds reflection/proxy and R8 rules for little gain. Ktor 3.6.1: no reason on Android-only.
- Parity with iOS D20/D21 [V-src `plan.md:437`]: client built with `followRedirects(false)`, `followSslRedirects(false)`, `CookieJar.NO_COOKIES` (OkHttp default), **no `Cache`**, bearer added by an interceptor scoped to the API host only; Release rejects non-HTTPS base URLs. Tests: an application interceptor that **denies every request by default** (the URLProtocol analogue), plus `mockwebserver3` for contract tests. Base URL default `https://api.chronoflow.net/v1`, selected at build time via `BuildConfig` fields (`CHRONOFLOW_DATA_SOURCE = mock|api`, `CHRONOFLOW_API_BASE_URL`), mock default in debug/tests, no silent fallback.
- Network monitoring (NWPathMonitor analogue): `ConnectivityManager.registerDefaultNetworkCallback` as a hint only; scene-active analogue: `ProcessLifecycleOwner` `ON_START`.

### 4.5 Preferences, secrets and backup [R]

- **DataStore 1.3.0** (Preferences or a typed `Serializer` with kotlinx.serialization) for device preferences: `api.deviceId` (UUIDv7, survives sign-out, D22), language, appearance.
- **Session secret** `{token, accountId}` (iOS: Keychain `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`, D21): **Tink 1.24.0** `Aead` (AES256-GCM keyset) wrapped by an Android Keystore master key (`android-keystore://…`, non-exportable; never leaves the device), ciphertext stored in its own DataStore file. Alternative without Tink: a Keystore AES/GCM key used directly through `javax.crypto.Cipher`. **Never** `EncryptedSharedPreferences` (deprecated, see §1.2).
- Storage class: default app storage is credential-encrypted (available after first unlock) = iOS `completeUntilFirstUserAuthentication` (D19). Do not use device-protected storage.
- Backup exclusion (D19 parity, OQ-26 on iOS): exclude the account directory, the session DataStore and the Tink keyset prefs from Auto Backup and device transfer via `android:dataExtractionRules` (API 31+) and `android:fullBackupContent` (API 29–30). Restored ciphertext without its Keystore key is unreadable anyway — treat as signed-out. Prove it with AGP 9.5's experimental backup tests (§2) once stable enough.

### 4.6 Credential Manager: passwords + passkeys [R + OQ-8]

- `androidx.credentials:credentials:1.6.0` + `credentials-play-services-auth:1.6.0` (Play-services provider needed below Android 14). Sign-in: `GetCredentialRequest` with `GetPasswordOption()` and `GetPublicKeyCredentialOption(requestJson)` built from `POST auth/passkey/options` (`rpId: "app.chronoflow.net"`, `userVerification: "required"`, `allowCredentials: []`, 60 s timeout) → `POST auth/passkey/login` with the assertion JSON (`id`, `rawId`, `type`, `response{clientDataJSON, authenticatorData, signature, userHandle?}`) [V-src `chronoflow-ios/plan.md:444-445`]. After password sign-in/registration, offer `CreatePasswordRequest` (Google Password Manager save) — the Android analogue of iOS shared web credentials.
- **Blocking dependency:** Android passkeys for RP `app.chronoflow.net` require `https://app.chronoflow.net/.well-known/assetlinks.json` declaring `delegate_permission/common.get_login_creds` (and `handle_all_urls` if App Links are added) for the app's package + signing-cert SHA-256. The web repo has no `.well-known` assetlinks file or route (only AASA is described, served from `app.chronoflow.net`) [V-src: `chronoflow-new/public/.well-known` absent; `chronoflow-ios/plan.md:455`]. Note `clientDataJSON.origin` on Android is `android:apk-key-hash:<base64url-sha256>`, which the Laravel WebAuthn verifier must accept — verify in `chronoflow-new` before the passkey phase.
- Emulator: Credential Manager with Google Password Manager needs Google Play services and a signed-in account; the existing `pixel` AVD is a `google_apis` image (no Play Store) — passkey flows are **device-only** verification, mirroring iOS ("Open: device only").

### 4.7 Background work, reminders, time [R]

- **WorkManager 2.12.0**: outbox flush/sync retry with `NetworkType.CONNECTED` constraint and exponential backoff (iOS D23: `min(1 s × 2^n, 5 min)` in-process; WorkManager's minimum backoff is 10 s, so keep the in-process retry loop as on iOS and use WorkManager only for "process died with pending outbox"). Unique work per account epoch; cancel on sign-out wipe.
- Reminders (iOS Phase 19, RN schedules 48 h / ≤ 60 notifications [V-src `chronoflow-mobile/plan.md:435`]): `AlarmManager.setAndAllowWhileIdle` (inexact) + `POST_NOTIFICATIONS` runtime permission (API 33+); avoid `SCHEDULE_EXACT_ALARM` unless the product requires minute precision (Play restricts `USE_EXACT_ALARM` to alarm/calendar apps). Reschedule on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`.
- Time: `java.time` (native at minSdk 29, no desugaring); no kotlinx-datetime needed.

### 4.8 UI kit [R]

- Compose BOM 2026.09.00; Material 3 1.4.0 only for platform-native pieces (dialogs, bottom sheets, date/time pickers, navigation suite, snackbars). Brand surfaces come from generated tokens (`_design/canvas/ds/chronoflow/tokens.json`), not from dynamic color. Fonts (Figtree 400/500/600/700, Fraunces 400/500 opsz cuts, iOS D11) as bundled `res/font` static TTFs (no downloadable fonts: offline and first-frame determinism).
- Edge-to-edge always (`enableEdgeToEdge()`), insets from `WindowInsets` only — never constants (D1c).

---

## 5. Navigation 3 vs Navigation Compose 2.x — decision record [R]

| Criterion | Navigation 3 1.2.1 | Navigation Compose 2.10.2 |
|---|---|---|
| Back stack ownership | App-owned `List<NavKey>` (state) | `NavController` internal |
| Parity with iOS D17 `Route` + `Navigator` | Direct | Indirect (routes as serializable objects, but controller-driven) |
| JVM testability of navigation | Assert the list | Needs `TestNavHostController` |
| Adaptive (rail/list-detail/foldable) | Scenes + `adaptive-navigation3` | Separate scaffolds |
| Predictive back | Built in (`NavDisplay`, `NavigationBackHandler`) | Built in |
| Maturity | Stable since 1.0 (2025); 1.2.x now | Very mature |

Decision: **Navigation 3**.

---

## 6. Gradle setup best practices [R]

### 6.1 Files

```
settings.gradle.kts          # pluginManagement { includeBuild("build-logic") }, foojay plugin, typesafe accessors
gradle/libs.versions.toml    # single version catalog (all pins from §1)
gradle/gradle-daemon-jvm.properties   # pins daemon JVM (Gradle daemon toolchain)
gradle/wrapper/gradle-wrapper.properties  # 9.9.1 + distributionSha256Sum
build-logic/convention/      # included build with precompiled/binary convention plugins
stability_config.conf        # Compose stability config (initially empty or java.time.*)
Makefile                     # iOS-compatible command vocabulary (§9.6)
```

Bootstrap without gradle on PATH: generate the wrapper with the Gradle 9.4.1 distribution already cached by the RN project — `~/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle wrapper --gradle-version 9.9.1 --distribution-type bin --gradle-distribution-sha256-sum d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228` run inside `chronoflow-android` (this only reads the cache; it does not touch the RN repo) [V-local: path exists].

### 6.2 `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx8g -XX:+UseParallelGC -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx4g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true
org.gradle.configuration-cache.problems=fail
kotlin.code.style=official
android.useAndroidX=true
# AGP 9 defaults stay on: builtInKotlin, newDsl, nonTransitiveRClass, uniquePackageNames
```

(Machine: Apple M4 Max, 36 GiB RAM [V-local] — 8 GiB daemon heap is comfortable.) Do not set `android.builtInKotlin=false` / `android.newDsl=false` (the RN opt-outs).

### 6.3 Convention plugins (build-logic)

`chronoflow.android.application`, `chronoflow.android.application.compose`, `chronoflow.android.library`, `chronoflow.android.library.compose`, `chronoflow.android.feature` (library + compose + hilt + nav3 + lifecycle + designsystem deps), `chronoflow.jvm.library` (`org.jetbrains.kotlin.jvm`, for pure-Kotlin modules), `chronoflow.hilt`, `chronoflow.sqldelight` (or `.room3`), `chronoflow.android.test` (benchmark module), `chronoflow.spotless`, `chronoflow.roborazzi`. Each sets `compileSdk = 37`, `minSdk = 29`, `targetSdk = 36` (app/test), Java 17 compatibility, `kotlin { compilerOptions { allWarningsAsErrors = true; freeCompilerArgs += "-Xconsistent-data-class-copy-visibility" … } }`, `composeCompiler { stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf")); reportsDestination/metricsDestination only when -PcomposeReports=true }`, and test options (`isIncludeAndroidResources = true`, JDK 21 launcher for `Test` tasks, OQ-3).

Release build type: `optimization { enable = true }` (AGP 9.3+ DSL) with keep rules in `src/main/keepRules/*.keep`; a `benchmark` build type (`initWith(release)`, debuggable false, signed with debug key, `matchingFallbacks += "release"`) created by the baseline-profile plugin.

### 6.4 Module layout for fast builds

Keep the performance-critical timeline engine and all business rules in **pure JVM modules** (fast compile, JVM tests, no Robolectric), and keep feature modules thin and parallel:

```
:app                          Application, MainActivity, NavDisplay, Hilt root, seeded-launch seam (debug)
:core:model          (JVM)    domain types: trips, plan blocks, preferences, Route keys
:core:timeline       (JVM)    PlanDecoder analogue: decode, day bucketing, layout math, label prep, now-line projection
:core:sync           (JVM)    SyncEngine/outbox state machines, retry policy, readiness reducer (iOS D12, D23–D25)
:core:testing        (JVM)    fixtures S1–S6, large60, fakes, test clock
:core:network        (Android lib) OkHttp client, DTOs, kotlinx.serialization, mock data source
:core:database       (Android lib) SQLDelight schema/queries, per-account driver factory
:core:datastore      (Android lib) DataStore + Tink session store + device identity
:core:data           (Android lib) repositories exposing Flows (UI observes only the DB, iOS D24)
:core:designsystem   (Android lib, compose) generated tokens, fonts, primitives
:core:ui             (Android lib, compose) shared composables, test host with simulated insets
:feature:launch | :feature:auth | :feature:today | :feature:trips | :feature:trip (timeline UI)
:feature:wizard | :feature:settings | :feature:walkthrough | :feature:reminders | :feature:terms
:benchmark           (com.android.test) Macrobenchmarks + BaselineProfile generator (UIAutomator)
:microbenchmark      (Android lib, androidx.benchmark) timeline engine on device
:konsist             (JVM, tests only) architecture rules
build-logic/convention
```

Rules: features never depend on features; `:core:timeline`/`:core:sync`/`:core:model` have no Android or Compose dependency; only `:app` knows all features. With configuration cache + build cache + parallel, a change in one feature recompiles only that module and `:app`.

---

## 7. Performance tooling (timeline first) [R, versions V-web]

| Need (iOS equivalent) | Android tool | How |
|---|---|---|
| Startup and first-use speed | **Baseline Profiles + Startup Profiles** (plugin `androidx.baselineprofile` 1.5.0, `BaselineProfileRule`, `profileinstaller` 1.4.1) | `:benchmark` module generates profiles for: cold start → Today, open trip S3 → scroll timeline (fling down/up), jump days, list toggle; `includeInStartupProfile = true` only for the start journey. Generate from a non-minified variant; ship in R8-optimized release. Google cites ~30% faster code paths from first launch; startup profiles (DEX layout) add ~15%. |
| `time-to-first-timeline-frame` signpost (perf-timeline.md:180-186) | `androidx.tracing` `trace("cf:timeline:firstFrame") { … }` + Macrobenchmark `TraceSectionMetric("cf:timeline:firstFrame")`; `reportFullyDrawn()` → `StartupTimingMetric` `timeToFullDisplayMs` | Same 3 iterations, CompilationMode.Partial (baseline profile) vs None. |
| Hitch ratio ≤ 1 ms/s (perf-timeline.md:97) | Macrobenchmark `FrameTimingMetric` (`frameDurationCpuMs`, `frameOverrunMs` P50/P90/P95/P99) | Budget: `frameOverrunMs` P95 ≤ 0 on a 60 Hz reference device during a scripted fling; record P99. |
| In-app jank capture | **JankStats** (`androidx.metrics:metrics-performance` 1.0.0) | Debug/perf builds only; state tags `timeline:day=<n>`, logs jank frames for agent diagnosis. |
| Peak memory 150 MiB | Macrobenchmark `MemoryUsageMetric(Mode.Max)` | Same budget; plus a host test bounding the drawing cache (4 MiB / 4 entries) like iOS. |
| Host budgets S3 40 / 30d 100 / 60d 200 ms (perf-timeline.md:157-163) | JVM tests in `:core:timeline` (coarse, 2× headroom) **and** Microbenchmark (`benchmark-junit4` 1.5.0) in `:microbenchmark` for real numbers | Microbenchmark on emulator is for trends only; device numbers are authoritative. |
| Recomposition control | Compose compiler **reports/metrics** (`composeCompiler { reportsDestination; metricsDestination }` behind `-PcomposeReports`), strong skipping (default), `stability_config.conf` | Guidance [V-web developer.android.com/develop/ui/compose/performance/stability/fix]: under strong skipping, prefer **preserving list instances** over `ImmutableList`/`@Immutable` on collection-holding models (those force element-wise `equals`). Defer fast-changing reads (scroll offset, now-line) to layout/draw phases (`Modifier.offset { }`, `drawWithCache`, `graphicsLayer { }`). |
| Timeline drawing (vector PDF display lists on iOS) | Compose `drawWithCache` + `Canvas` per day strip in a `LazyColumn`, static content recorded once (`Picture`/`GraphicsLayer.record`), text pre-measured off-main with `TextMeasurer` caches | Details belong to the timeline research doc; the stack implication is: keep layout math in `:core:timeline` (JVM), and draw-phase-only invalidation for the minute tick. |
| Leaks | LeakCanary 2.14 (debug) | — |
| R8 | Full mode + `optimization { enable = true }`; `:app:analyzeReleaseR8Config` | Measure APK/AAB size and startup on the release+profile build only. |

Emulator caveat [R]: Macrobenchmark refuses emulators unless `androidx.benchmark.suppressErrors=EMULATOR`; emulator numbers are regression signals, not device claims (the iOS doc says the same about simulator budgets, perf-timeline.md:95-96). A physical mid-range Android phone is needed for authoritative numbers (the RN plan also never ran this: `chronoflow-mobile/plan.md:449-451`) → OQ-6.

---

## 8. Code quality tooling [R]

- **Spotless 8.11.0 + ktlint 1.8.0 + Compose rules 0.6.8 (ktlint flavour)**: `make format` = `spotlessApply`, `make lint` = `spotlessCheck` + Android Lint (`lint { warningsAsErrors = true; checkDependencies = true }` on `:app`). `.editorconfig` with `ktlint_code_style = ktlint_official`.
- **detekt: not adopted.** 1.23.8 (latest stable) is built against Kotlin 2.0.21 and predates Kotlin 2.4 syntax; 2.0 (`dev.detekt`) is still alpha (alpha.6). Mirrors iOS "No SwiftLint". Revisit when 2.0.0 is stable (OQ-7).
- **Konsist 0.18.1** (built with Kotlin 2.4.21) in `:konsist` JVM tests: features don't import features; `core:model|timeline|sync` import no `android.*`/`androidx.compose.*`; ViewModels end with `ViewModel` and live in `feature:*`; no `GlobalScope`; no `runBlocking` outside tests; no `Thread.sleep` in tests; test classes named `*Test`; `Route` implementations are `@Serializable` data objects/classes. (1.0.0-beta.1 exists; stay on 0.18.1 until 1.0.0.)
- Optional: dependency-analysis 3.9.0 (`buildHealth`) once module count is > 15.

---

## 9. Test strategy and tooling (iOS policy mapped) [R]

### 9.1 Layers ("pick the cheapest layer that can fail", AGENTS.md:32)

| iOS layer | Android layer | Tooling |
|---|---|---|
| SwiftPM host tests (logic, timing, retries, persistence, computed values) | **JVM unit tests** in pure modules; SQLDelight with the JVM `JdbcSqliteDriver` in-memory for query/migration tests | JUnit4 4.13.2, assertk 0.28.1, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0 (virtual time = injectable clock), mockwebserver3 5.5.1 |
| Hosted AppTests (layout, geometry, profile snapshots) | **Robolectric 4.18 + Compose UI test + Roborazzi 1.79.0** on the JVM, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w<W>dp-h<H>dp-<density>")` per device profile; simulated insets via a test host that injects `WindowInsets` (same trick as iOS `AuthTestHost`) | `recordRoborazziDebug` / `verifyRoborazziDebug` / `compareRoborazziDebug` |
| XCUITest (native controls, navigation, system UI, appearance) | **Instrumented Compose UI tests** (`createAndroidComposeRule<MainActivity>()`) on the `pixel` emulator, launched **seeded**; UIAutomator 2.4.1 for system UI (permission dialogs, notifications, Credential Manager sheet presence) | `connectedDebugAndroidTest` or Gradle Managed Devices |
| Release launch/perf scheme | **Macrobenchmark + Baseline Profile** module | `:benchmark:connectedBenchmarkAndroidTest`, `:app:generateBaselineProfile` |

Budgets carried over: JVM/Robolectric test < 5 s, instrumented UI test < 10 s, per-test timeout 60 s (JUnit `@get:Rule val timeout = Timeout.seconds(60)`); split loops over profiles × languages × sizes into one test per profile; exact semantics queries (`onNodeWithTag("id")`, `useUnmergedTree` only when needed), `waitUntil(timeoutMillis)` instead of sleeps.

### 9.2 Screenshot testing choice: **Roborazzi** [R]

| | Roborazzi 1.79.0 | Paparazzi 1.3.5 | Compose Preview Screenshot Testing (alpha16 engine) |
|---|---|---|---|
| Engine | Robolectric (native graphics) | layoutlib | layoutlib |
| Real composables with state, Hilt, Nav3, interactions | Yes | Limited (no Activity) | Previews only |
| Status | Active (weekly releases) | Slower cadence | Standalone plugin **unmaintained**; replacement via AGP 9.5 test suites is **experimental** (`android.experimental.enableScreenshotTest=true`, `android.experimental.testSuiteSupport=true`, `testOptions.screenshotTests.create("screenshotTest") { engineVersion = "0.0.1-alpha16" … }`, tasks `update/test{Suite}{Target}{Variant}TestSuite`) |
| Device profiles × locales × font scale × dark | Qualifiers per test | DeviceConfig | `@Preview` params |

Roborazzi lets one Robolectric harness serve both "hosted layout assertions" and "reference snapshots", which is exactly the iOS split (hosted checks always on; reference comparison opt-in until approved: `make snapshots`, `SNAPSHOT_RECORD=1 make snapshots`). Goldens recorded on this Mac only; never named after a device that ran them.

### 9.3 Journeys

- **UIAutomator 2.4.1** is required anyway for Macrobenchmark/Baseline Profile journeys; reuse the same journey helpers for the "one UI journey per feature" smoke tests.
- **Maestro 2.12.1** (installed) stays **opt-in** for exploratory/agent screenshots and RN-parity flows (`chronoflow-mobile/.maestro/`); Compose nodes become addressable by id with `Modifier.semantics { testTagsAsResourceId = true }` at the root. Not part of `make check`.

### 9.4 Seeded launch (iOS `CF_AUTH_TEST_SIGNED_IN`, `CF_SETTINGS_ROUTE`, `-cf.trip-page S3`)

Debug-only intent extras read in `MainActivity` after the first frame commits, e.g. `adb shell am start -n net.chronoflow.app.native/.MainActivity --es cf.seed demo --es cf.route settings/security --es cf.trip-page S3 --ez cf.walkthroughSeen true`. Release builds compile the seam out (source set `src/debug`). Instrumented tests pass the same extras via `ActivityScenario.launch(intent)`.

### 9.5 Commands on this Mac (no gradle on PATH; always `./gradlew`)

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### 9.6 Makefile vocabulary (mirrors chronoflow-ios/AGENTS.md:64-76)

| Target | Runs | Target time |
|---|---|---|
| `make quick` | `spotlessCheck` + JVM unit tests of all modules + Konsist + Robolectric screen-assertion tests (no goldens) | < 1 min warm |
| `make check` | quick + `lintDebug` + `assembleRelease` (R8) + instrumented smoke on `pixel` (seeded, headless) | < 5 min |
| `make format` / `make lint` | `spotlessApply` / `spotlessCheck lintDebug` | — |
| `make test ONLY=<Class#method>` | single JVM test (`--tests`) | seconds |
| `make targeted DEVICES="pixel fold" ONLY=…` | instrumented subset on named AVDs | — |
| `make snapshots` / `SNAPSHOT_RECORD=1 make snapshots` | `verifyRoborazziDebug` / `recordRoborazziDebug` | — |
| `make perf` / `make perf-timeline` | Macrobenchmarks (emulator: regression only) | — |
| `make baseline-profile` | `:app:generateBaselineProfile` | — |
| `make matrix` | on request only: all AVDs (§10) | — |
| `make emu-clean` | wipe AVD data when large (opt-in, refuses during a run) | — |

---

## 10. Device / emulator matrix (analogue of iOS D1c policy) [R]

### 10.1 Installed / installable [V-local]

- Installed AVD: **`pixel`** → device `pixel_8`, image `system-images;android-36;google_apis;arm64-v8a` (rev 7), arm64, 4 cores, Play Store disabled.
- Device profiles available in `avdmanager`: `pixel_9_pro_fold`, `pixel_fold`, `pixel_10_pro_fold`, `small_phone`, `medium_phone`, `pixel_9a`, `pixel_8`, `pixel_10`, tablets, etc.
- Installable arm64 images include: `android-26/28/29/30/31 google_apis`, `android-29 google_apis_playstore` (rev 9), `android-36 google_apis_playstore`, `android-36 google_apis_ps16k`, **ATD images** (`aosp_atd`/`google_atd`) for API 30–37 including `system-images;android-36;google_atd;arm64-v8a`, and `android-37.0`/`37.1`/`37.2` images.

### 10.2 Tiers (cheap first; mirrors "skip the matrix unless asked")

| Tier | When | What |
|---|---|---|
| T0 JVM | every change | Device-profile table tests (pure Kotlin: window sizes × insets × density for: small phone 360×640 dp, Pixel 8 412×915, Pixel 9a, large phone 448×998, foldable outer, foldable inner ~ 851×881 sw≥600, 3-button vs gesture nav insets) + Roborazzi per profile |
| T1 default emulator | `make check` | `pixel` (Pixel 8, API 36), headless, seeded smoke |
| T2 opt-in AVDs | only when the change touches that dimension | `fold` (Pixel 9 Pro Fold, API 36) — inner display is sw ≥ 600 dp, so the Android 16 orientation rule applies; `small` (`small_phone`, API 36); `api29` (minSdk floor, `google_apis` arm64 rev 13) |
| T3 manual/device | before release | physical mid-range phone (perf numbers, passkeys, Google Password Manager), `google_apis_playstore` AVD for Credential Manager smoke |

Creation commands (to run in the Android project phase; they write only to `~/.android` and the SDK):

```bash
sdkmanager "system-images;android-29;google_apis;arm64-v8a"
avdmanager create avd -n fold   -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_9_pro_fold
avdmanager create avd -n small  -k "system-images;android-36;google_apis;arm64-v8a" -d small_phone
avdmanager create avd -n api29  -k "system-images;android-29;google_apis;arm64-v8a" -d medium_phone
```

Optionally declare the same as **Gradle Managed Devices** using ATD images (`systemImageSource = "google_atd"`, `apiLevel = 36`) for reproducible headless runs (`./gradlew pixel8Api36DebugAndroidTest`); ATD images are not available below API 30, so the minSdk run uses a plain AVD.

### 10.3 Headless emulator, CI-like (this Mac)

```bash
emulator -avd pixel -no-window -no-audio -no-boot-anim -no-snapshot-save -gpu swiftshader_indirect -port 5554 &
adb -s emulator-5554 wait-for-device
until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -s emulator-5554 shell settings put global $k 0; done
adb -s emulator-5554 shell input keyevent 82          # dismiss keyguard
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
adb -s emulator-5554 emu kill
```

Notes: `-read-only` allows parallel instances of one AVD (no snapshot saving); use `-port 5556`, `5558` for more emulators; use `-gpu host` (with a window or `-qt-hide-window`) when measuring frame timing, because software GL distorts jank numbers; `-wipe-data` for a clean first-run state; the host machine is reachable from the emulator at `10.0.2.2` (dev API server) — under Android 16's upcoming local-network permission, keep dev traffic to `10.0.2.2`/HTTPS hosts. The flags above exist in emulator 37.2.13 [V-local `emulator -help`].

---

## 11. Open questions (proceed on the bold default)

| # | Question | Default |
|---|---|---|
| OQ-1 | minSdk? | **29** (93.9% reach; dark theme, gesture insets, TLS 1.3, Mainline tzdata, passkeys). Alternative 28 (+3.1%) if the user wants max reach. |
| OQ-2 | Database library? | **SQLDelight 2.4.1** with framework SQLite (SQL-first parity with the iOS GRDB/RN schema). Alternative Room 3.0.3. |
| OQ-3 | JDK 21 for Robolectric SDK 36 tests (only JDK 17 installed)? | **Provision JDK 21 via Gradle toolchains (foojay-resolver-convention 1.0.0) for `Test` tasks only**; build stays on JDK 17. Alternative: install Zulu 21 with Homebrew (user action). |
| OQ-4 | Gradle 9.9.1 vs AGP-default 9.6.0? | **9.9.1**, fall back to 9.6.0 if AGP 9.5.1 warns or fails. |
| OQ-5 | Large screens: iOS is iPhone-portrait-only (D16) but targetSdk 36 ignores orientation locks on sw ≥ 600 dp. | **Support resizing/rotation; on wide windows show a centred max-width phone column + navigation rail; no opt-out property.** No board covers this — ask the designer later. |
| OQ-6 | Physical Android device for perf numbers/passkeys? | **Emulator budgets as regression gates; device verification marked "open: device only"** (as iOS does for passkeys). |
| OQ-7 | detekt? | **No** (ktlint + Compose rules + Android Lint + Konsist). Revisit at detekt 2.0.0 stable. |
| OQ-8 | `assetlinks.json` for `app.chronoflow.net` and Android origin acceptance in the WebAuthn verifier | **Treat passkeys as blocked on the web repo**; ship password sign-in + Credential Manager password save first; draft the assetlinks content once the package name and signing key are known. |
| OQ-9 | `applicationId`? RN uses `net.chronoflow.app` (`chronoflow-mobile/plan.md:41,1487`); iOS native debug uses `net.chronoflow.app.native` (D15). | **Debug: `net.chronoflow.app.native`** (installs next to the RN dev client); release id decided at store time (`net.chronoflow.app` to replace the RN listing). |
| OQ-10 | Retrofit or plain OkHttp? | **Plain OkHttp + hand-written client** (mirrors iOS APIDataSource error mapping). |
| OQ-11 | Hilt or manual DI? | **Hilt 2.60.1 (KSP)**; Konsist keeps core modules DI-agnostic so manual DI remains possible. |
| OQ-12 | Material 3 1.5 (rc01) / Compose 1.13 (rc01)? | **Stay on BOM 2026.09.00** (Compose 1.12.1, M3 1.4.0) until a BOM ships them stable. |

---

## 12. Risks

1. **JDK mismatch**: Robolectric/Roborazzi with targetSdk 36 need JDK 21 while the machine has only JDK 17 → first `make quick` fails unless OQ-3 is applied in phase 0.
2. **Kotlin 2.4.21 under AGP 9.5 built-in Kotlin**: the KGP version is decided by classpath resolution; a mismatch with KSP 2.3.14/Hilt 2.60.1 surfaces as obscure KSP errors. Verify with `buildEnvironment` and a Hilt+KSP smoke module in phase 0.
3. **Android 16 large-screen rule** makes the portrait-only iOS layout incomplete on foldables/tablets; every screen must lay out from window size (already the D1c rule).
4. **Passkeys blocked** on assetlinks + server Android-origin support (OQ-8); also emulator can't fully verify.
5. **Emulator perf numbers** (software GL, no thermal model) can mislead; never present them as device claims.
6. **Screenshot determinism**: Roborazzi goldens change with Robolectric/Roborazzi upgrades (1.78.0 changed PNG bytes); pin versions and re-record deliberately.
7. **Experimental Google tooling** (screenshot test suites, backup tests) can change DSL between AGP minors — keep them out of the gate.
8. **detekt absence** shifts static analysis to Lint/Konsist; Compose-specific misuse is covered only by the ktlint Compose rules.
9. **Reminders precision**: inexact alarms may fire minutes late in Doze; if the product needs exact times, exact-alarm permission policy becomes a store-review risk.

---

## Sources

- AGP release notes (9.5): https://developer.android.com/build/releases/gradle-plugin ; AGP 9.0: https://developer.android.com/build/releases/agp-9-0-0-release-notes ; AGP 9.3: https://developer.android.com/build/releases/agp-9-3-0-release-notes ; built-in Kotlin: https://developer.android.com/build/migrate-to-built-in-kotlin
- AndroidX versions table (updated 2026-11-09): https://developer.android.com/jetpack/androidx/versions
- Navigation 3 releases: https://developer.android.com/jetpack/androidx/releases/navigation3
- Room 3 releases: https://developer.android.com/jetpack/androidx/releases/room3
- Security (security-crypto deprecation): https://developer.android.com/jetpack/androidx/releases/security
- Compose BOM: https://developer.android.com/develop/ui/compose/bom/bom-mapping (+ BOM POM on dl.google.com)
- Compose stability: https://developer.android.com/develop/ui/compose/performance/stability/fix
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Compose Preview Screenshot Testing: https://developer.android.com/studio/preview/compose-screenshot-testing and https://developer.android.com/studio/preview/compose-screenshot-testing-with-testsuites
- Credential Manager: https://developer.android.com/identity/sign-in/credential-manager ; passkeys: https://developer.android.com/identity/passkeys
- Android 16 behaviour changes: https://developer.android.com/about/versions/16/behavior-changes-16
- Play target API: https://developer.android.com/google/play/requirements/target-sdk ; https://support.google.com/googleplay/android-developer/answer/11926878
- Distribution (2026-10-01): https://composables.com/android-distribution-chart
- Kotlin releases: https://kotlinlang.org/docs/releases.html ; Gradle releases: https://gradle.org/releases/
- KSP: https://github.com/google/ksp/releases ; Dagger/Hilt: https://github.com/google/dagger/releases ; Robolectric: https://github.com/robolectric/robolectric/releases ; Roborazzi: https://github.com/takahirom/roborazzi/releases ; detekt: https://github.com/detekt/detekt/releases ; Konsist: https://github.com/LemonAppDev/konsist/releases ; Maestro: https://github.com/mobile-dev-inc/Maestro/releases
- Maven metadata (versions): dl.google.com/android/maven2, repo1.maven.org/maven2, plugins.gradle.org/m2 (queried 2026-11-15)
