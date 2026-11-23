# Phase 0a brief: scaffold, build logic and tiers

Branch `feat/phase-0a-scaffold`. Depends on nothing. Wave 1. Plan entry: `plan.md:682` "Phase 0a: Scaffold, build logic and tiers".

Tags: **[F]** is a fact verified in a source file or on this Mac (path or command given). **[R]** is a decision this brief makes for Android. **[D]** is a default chosen for an open question (listed at the end). Where this brief and `plan.md` differ, `plan.md` wins unless the brief names an open question that changes it.

You are a Sonnet worker. You have not seen the research. Everything you need is here or in a file this brief names. Read AGENTS.md "Tests", "Commands", "Git" and "Traps" before you write anything.

---

## 1. Goal

When this phase ends:

1. `/Users/rickgout/Sites/chronoflow-android` is a git repository. `main` holds exactly one commit, the docs commit. `feat/phase-0a-scaffold` stacks the phase's commits on top of it.
2. `./gradlew` (Gradle 9.9.1) configures and builds every module of the A3 module graph, with the configuration cache reused on the second run, and with KGP 2.4.21 on the build classpath.
3. A minimal `:app` installs and launches as `net.chronoflow.app.debug` on the `pixel` emulator, under debug StrictMode with `penaltyDeath` for main-thread disk or network work on the launch path.
4. Spotless (ktlint 1.8.0 + Compose rules 0.6.8), Lint as errors, Kotlin `allWarningsAsErrors`, the Konsist rules, `verifyReleaseManifest` and the bundle-size gate are wired, and each rule fails on a planted violation.
5. The `Makefile`, `Scripts/ci.sh`, `Scripts/run-tier.sh`, `Scripts/emulator.sh` and `Scripts/measure-tiers.sh` exist, with tests.
6. `docs/tooling.md` records the warm times of `make quick` and `make check` (three runs each) and what the worker sandbox can and cannot run.

**No product code.** No tokens, no copy beyond `app_name`, no routes, no screens, no theme beyond a platform placeholder, no data layer.

---

## 2. Inputs to read

Read these, in this order. Line numbers are for the files as they are now.

**This repository**
- `AGENTS.md` (whole file; "Tests", "Commands", "Git", "Traps" are binding).
- `CLAUDE.md` ("Orchestration", "Cleanup").
- `plan.md`:
  - `:84` "Decisions": A1 (toolchain and versions), A2 (app identity), A3 (module graph and Konsist rules), A19 (test tiers), A20 (code quality), A21 (performance rules).
  - `:219` "Constraints and working rules".
  - `:282` "Android spec: build and repository layout" (file tree, `gradle.properties`, convention plugins, build types).
  - `:450` "Android spec: manifest and permissions".
  - `:473` "Android spec: release" (size gate).
  - `:491` "Performance budget" (rows "Main thread before hand-off", "Download size", "`make quick`", "`make check`").
  - `:570` "Open questions for the user": OQ-A1, OQ-A2, OQ-A27, OQ-A31, OQ-A35.
  - `:682` "Phase 0a" (the task list you tick).
- `docs/research/11-android-stack.md` §1 (version table), §2 (AGP 9 facts), §6 (Gradle setup), §8 (code quality), §9.5–§9.6 (commands), §10.3 (headless emulator).
- `docs/research/13-testing-tooling.md` §1.4 (incident log I6, I7, I11, I12, I15, I17), §4.6 (hang caps), §4.8 (release readiness), §7 (Makefile and Gradle), §8 (Phase-0 checklist).
- `docs/research/14-gaps.md` §6 (permissions and merged manifest), §11 (signing, versions, bundle, build hygiene).
- `docs/research/01-ios-plan-reuse.md` §8 (lessons learned).

**iOS reference** (read-only, at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)
- `plan.md:634` "Phase 0: Scaffold and tooling (`feat/phase-0-scaffold`)": the shape of this phase (first docs commit, branch, scaffold, Makefile, `ci.sh`, a guard probe proved once and removed, verified results recorded).
- `plan.md:68` "Test tiers (user decision 2026-11-06)": quick < 1 min warm, targeted, check < 5 min, the matrix only on request, `Scripts/ci.sh` runs `make generate check`.
- `plan.md:938` "Tooling: test speed (user request, 2026-11-06)" and `plan.md:974` "Tooling round 3: test tiers": why tiers are measured from day one (iOS check measured 408 s, 552 s, 603 s; never met 300 s).
- `plan.md:123` "Constraints and working rules".
- `AGENTS.md` ("Tests", "Traps"), `CLAUDE.md`.
- `Makefile`: the target vocabulary (`quick`, `targeted`, `check`, `matrix`, `generators-check`, `.NOTPARALLEL:`).
- `Scripts/ci.sh`: `cd "$(dirname "$0")/.."; exec make generate check`.
- `Docs/tooling.md:179` "Test tiers — user decision 2026-11-06", `:476` "Smoke set and isolation", `:570` "Phase 0 verified results" (the record format you mirror in `docs/tooling.md`).
- `Docs/ui-test-tiers.md` (first section only: seeded launch, tiers).

**Toolchain on this Mac** [F, checked 2026-11-16]
- `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`: platforms `android-36`, `android-37.0`; build-tools `36.0.0`, `37.0.0`; `cmdline-tools/latest/bin/{avdmanager,sdkmanager,apkanalyzer,lint,r8}`; system image `android-36` (google_apis arm64); NDK 27 present but unused.
- AVD `pixel` (`~/.android/avd/pixel.avd`, Pixel 8, API 36).
- JDK: only Zulu 17.0.21 (`/usr/libexec/java_home -v 17`). JDK 21 for test tasks comes from the foojay resolver.
- No `gradle` and no `bundletool` on `PATH`. Gradle 9.4.1 is cached at `~/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle`. bundletool jars 1.1.0, 1.8.2, 1.15.2 and 1.18.3 are in `~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/bundletool/` (only 1.18.3 is used).
- `avdmanager list device -c` offers `pixel_8`, `pixel_9_pro_fold` and `small_phone` (used by `emulator.sh create`).
- `python3` 3.15.2 at `/opt/homebrew/bin/python3` (stdlib only, always `python3 -I`).
- Pins: iOS `f391a1fac33eb0f0ea9e3f565206b91118e793d0`; web `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19` (both resolve).

**Boards:** none. This phase has no UI. Do not create a theme, colors, icons or copy.

**API endpoints:** none.

---

## 3. What this phase reuses from iOS

The answer to "does the iOS plan contain things we can re-use" for this phase is yes: the tooling rules transfer almost unchanged.

| iOS (at `f391a1fa`) | Android form in this phase |
|---|---|
| Make vocabulary `quick` / `targeted` / `check` / `matrix` (user, 2026-11-06) | Same target names (Task 9) |
| `Scripts/ci.sh` = `make generate check` | Same, plus `git diff --exit-code` and an untracked-file check |
| Runner idle cap 600 s on log growth, total cap 3,600 s, TERM → 5 s → KILL to the process group (I17) | `Scripts/run-tier.sh` |
| Missing results fail the pass (I11) | run-tier fails a tier with no result file |
| Diagnostics off by default, `TEST_DIAGNOSTICS=1` opts in (I12) | run-tier collects `logcat` only with `TEST_DIAGNOSTICS=1` |
| `make sim-clean` refuses while tests run (I6) | `Scripts/emulator.sh wipe` refuses while tests run |
| Headless keyboard minimisation (I7) | `show_ime_with_hard_keyboard 1` in the functional prep |
| Sandbox could not run simulators; the orchestrator ran them (I15) | Task 10 records exactly what the sandbox can run |
| Catalog guard probe: planted, failed, removed (iOS Phase 0) | Each Konsist and manifest rule has a planted violation; one Lint probe is proved and reverted |
| `Docs/tooling.md` "Phase 0 verified results" | `docs/tooling.md` "Phase 0a verified results" |

---

## 4. Out of scope

Each of these belongs to a later phase. Do not start them, even partly.

- Tokens, fonts, launcher icon, notification icon, launch moon (0b). Theme beyond the platform placeholder in Task 5 (2, 3a).
- String generation, `values-nl`, `CopyIndex`, Lint translation checks (0c). Only `app_name` lands here.
- Contracts, DTOs, fixtures (0d).
- `profiles.json`, Robolectric, Roborazzi guards, the tier registry and per-test budget listener, `SeedReceiverActivity`, `TestSeeds`, `ReleaseReadinessTest`, `ConfigurationPolicy`, the `:benchmark` perf variants and `StartupBenchmark`, `check-benchmarks.py`, the `timeout_msec` verification (0e). The `chronoflow.roborazzi` plugin is defined here but applied in 0e.
- System splash, `enableEdgeToEdge`, launch overlay, trace sections, `reportFullyDrawn` (3a).
- `AppDispatchers`, `BuildConfig.DATA_SOURCE` / `API_BASE_URL` (4a–4e).
- LeakCanary, JankStats (later; LeakCanary adds an eager initializer).
- Konsist rules other than the A3 rules (Phase 2 adds the color rules; 0c the `Text(` literal rule; 1a the clock rule).
- Release signing with an upload key (13). Release stays unsigned on a fresh clone.

---

## 5. Fixed values

Use exactly these. Do not "upgrade" any version.

### 5.1 Module graph, plugins and namespaces (A2, A3) [F plan.md:89–90]

Every module's `build.gradle.kts` applies **exactly one** module-kind convention plugin and nothing else from `chronoflow.*`. `chronoflow.spotless` is applied once, in the root `build.gradle.kts`. `chronoflow.roborazzi` is an add-on that 0e applies.

| Module | Directory | Plugin | `namespace` / Kotlin package root |
|---|---|---|---|
| `:app` | `app/` | `chronoflow.android.application.compose` | `net.chronoflow.app` |
| `:core:model` | `core/model/` | `chronoflow.jvm.library` | `net.chronoflow.core.model` |
| `:core:plan` | `core/plan/` | `chronoflow.jvm.library` | `net.chronoflow.core.plan` |
| `:core:sync` | `core/sync/` | `chronoflow.jvm.library` | `net.chronoflow.core.sync` |
| `:core:reminders` | `core/reminders/` | `chronoflow.jvm.library` | `net.chronoflow.core.reminders` |
| `:core:wizard` | `core/wizard/` | `chronoflow.jvm.library` | `net.chronoflow.core.wizard` |
| `:core:network` | `core/network/` | `chronoflow.jvm.library` | `net.chronoflow.core.network` |
| `:core:data-mock` | `core/data-mock/` | `chronoflow.jvm.library` | `net.chronoflow.core.datamock` |
| `:core:testing` | `core/testing/` | `chronoflow.jvm.library` | `net.chronoflow.core.testing` |
| `:core:database` | `core/database/` | `chronoflow.sqldelight` (see OQ-0a-6) | `net.chronoflow.core.database` |
| `:core:datastore` | `core/datastore/` | `chronoflow.android.library` | `net.chronoflow.core.datastore` |
| `:core:data` | `core/data/` | `chronoflow.android.library` | `net.chronoflow.core.data` |
| `:core:designsystem` | `core/designsystem/` | `chronoflow.android.library.compose` | `net.chronoflow.core.designsystem` |
| `:core:navigation` | `core/navigation/` | `chronoflow.android.library.compose` | `net.chronoflow.core.navigation` |
| `:core:ui` | `core/ui/` | `chronoflow.android.library.compose` | `net.chronoflow.core.ui` |
| `:feature:launch` … `:feature:reminders` (9: `launch`, `auth`, `onboarding`, `today`, `trips`, `trip`, `wizard`, `settings`, `reminders`) | `feature/<name>/` | `chronoflow.android.feature` | `net.chronoflow.feature.<name>` |
| `:benchmark` | `benchmark/` | `chronoflow.android.test` | `net.chronoflow.benchmark` |
| `:konsist` | `konsist/` | `chronoflow.jvm.library` | `net.chronoflow.konsist` |
| `:tools:tokengen` | `tools/tokengen/` | `chronoflow.jvm.library` | `net.chronoflow.tools.tokengen` |

- `applicationId = "net.chronoflow.app"`; debug `applicationIdSuffix = ".debug"`. **`native` is a Java keyword**: never use it as a namespace or Kotlin package segment. Final id: user, 2026-11-23 (A2/OQ-A1).
- `data-mock` has a hyphen; its package is `datamock`.
- Skeleton modules contain only `build.gradle.kts`. No placeholder classes.

### 5.2 Versions (A1) [F plan.md:88, 11 §1]

`gradle/libs.versions.toml` must contain exactly these versions. Library entries that no module uses yet are fine; they are never resolved until used.

```toml
[versions]
agp = "9.5.1"
kotlin = "2.4.21"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
coreKtx = "1.19.1"
coreSplashscreen = "1.2.0"
lifecycle = "2.11.0"
navigation3 = "1.2.1"
adaptiveNavigation3 = "1.3.0"
window = "1.5.1"
datastore = "1.3.0"
work = "2.12.0"
tracing = "2.0.5"
profileinstaller = "1.4.1"
metricsPerformance = "1.0.0"
credentials = "1.6.0"
sqldelight = "2.4.1"
kotlinxSerialization = "1.13.0"
coroutines = "1.11.0"
okhttp = "5.5.1"
benchmark = "1.5.0"
uiautomator = "2.4.1"
androidxTestExtJunit = "1.3.0"
androidxTest = "1.7.0"          # runner + core; see OQ-0a-17
junit4 = "4.13.2"               # see OQ-0a-17
robolectric = "4.18"
roborazzi = "1.79.0"
turbine = "1.2.1"
assertk = "0.28.1"
mockk = "1.14.12"
leakcanary = "2.14"
spotless = "8.11.0"
ktlint = "1.8.0"
composeRules = "0.6.8"
konsist = "0.18.1"
bundletool = "1.18.3"           # see OQ-0a-7
```

Remove the `#` comments when you write the file; they are only for this brief.

Libraries (group:name, version ref):
- Compose (BOM-managed, no version): `androidx.compose:compose-bom` (`composeBom`), `androidx.compose.ui:ui`, `androidx.compose.ui:ui-graphics`, `androidx.compose.ui:ui-tooling`, `androidx.compose.ui:ui-tooling-preview`, `androidx.compose.ui:ui-test-junit4`, `androidx.compose.ui:ui-test-manifest`, `androidx.compose.foundation:foundation`, `androidx.compose.runtime:runtime`, `androidx.compose.material3:material3`, `androidx.compose.material3.adaptive:adaptive`, `androidx.compose.material3:material3-adaptive-navigation-suite`.
- AndroidX: `androidx.activity:activity-compose`, `androidx.core:core-ktx`, `androidx.core:core-splashscreen`, `androidx.lifecycle:lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, `lifecycle-process`, `lifecycle-viewmodel-navigation3` (all `lifecycle`), `androidx.navigation3:navigation3-runtime`, `navigation3-ui`, `androidx.compose.material3.adaptive:adaptive-navigation3` (`adaptiveNavigation3`), `androidx.window:window`, `androidx.datastore:datastore`, `datastore-preferences`, `androidx.work:work-runtime`, `androidx.tracing:tracing`, `androidx.tracing:tracing-ktx`, `androidx.profileinstaller:profileinstaller`, `androidx.metrics:metrics-performance`, `androidx.credentials:credentials`, `credentials-play-services-auth`.
- Kotlin/other: `org.jetbrains.kotlinx:kotlinx-serialization-json`, `kotlinx-coroutines-core`, `kotlinx-coroutines-android`, `kotlinx-coroutines-test`, `app.cash.sqldelight:android-driver`, `sqlite-driver`, `coroutines-extensions`, `com.squareup.okhttp3:okhttp`, `mockwebserver3`.
- Tests and tooling: `androidx.benchmark:benchmark-macro-junit4`, `androidx.test.uiautomator:uiautomator`, `androidx.test.ext:junit` (`androidxTestExtJunit`), `androidx.test:runner`, `androidx.test:core` (`androidxTest`), `junit:junit` (`junit4`), `org.robolectric:robolectric`, `io.github.takahirom.roborazzi:roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`, `app.cash.turbine:turbine`, `com.willowtreeapps.assertk:assertk`, `io.mockk:mockk`, `com.squareup.leakcanary:leakcanary-android`, `com.lemonappdev:konsist`, `com.android.tools.build:bundletool`.
- Build-logic only (`compileOnly`): `com.android.tools.build:gradle` (`agp`), `org.jetbrains.kotlin:kotlin-gradle-plugin` (`kotlin`), `org.jetbrains.kotlin:compose-compiler-gradle-plugin` (`kotlin`), `com.diffplug.spotless:spotless-plugin-gradle` (`spotless`), `app.cash.sqldelight:gradle-plugin` (`sqldelight`), `io.github.takahirom.roborazzi:roborazzi-gradle-plugin` (`roborazzi`).

Plugins: `android-application` (`com.android.application`), `android-library` (`com.android.library`), `android-test` (`com.android.test`), `android-lint` (`com.android.lint`), all `agp`; `kotlin-jvm` (`org.jetbrains.kotlin.jvm`), `compose-compiler` (`org.jetbrains.kotlin.plugin.compose`), `kotlin-serialization` (`org.jetbrains.kotlin.plugin.serialization`), all `kotlin`; `sqldelight` (`app.cash.sqldelight`); `roborazzi` (`io.github.takahirom.roborazzi`); `spotless` (`com.diffplug.spotless`); `baselineprofile` (`androidx.baselineprofile`, `benchmark`).

The foojay settings plugin `org.gradle.toolchains.foojay-resolver-convention` `1.0.0` is written literally in `settings.gradle.kts` (the catalog is not available there).

No `+`, no ranges, no `-alpha`, `-beta` or `-rc`.

### 5.3 SDK and JVM levels [F plan.md:88, AGENTS Traps]

- `compileSdk = 37`, `minSdk = 29`, `targetSdk = 36` set **explicitly** in the application and test plugins, and as `testOptions.targetSdk = 36` and `lint.targetSdk = 36` in the library plugins. Left unset, AGP 9 uses 37.
- Java 17 source and target; Kotlin `jvmTarget = 17`. Gradle runs on JDK 17. Every `Test` task uses a JDK 21 launcher.
- Never apply `org.jetbrains.kotlin.android`. Never set `android.builtInKotlin=false` or `android.newDsl=false`. No kapt, no KSP. Variant work uses `androidComponents { onVariants { } }`, never `applicationVariants` and never `afterEvaluate`.

### 5.4 `gradle.properties` [F plan.md:298–309, :718]

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
android.onlyEnableUnitTestForTheTestedBuildType=false
```

Never add `org.gradle.java.home` or any machine path. If AGP reports one of these as unknown or deprecated, remove that line and record it (OQ-0a-12).

### 5.5 Permissions and manifest [F plan.md:450–464, 14 §6]

- Release allowlist (`<uses-permission>` names): `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, `android.permission.POST_NOTIFICATIONS`, `android.permission.RECEIVE_BOOT_COMPLETED`, `android.permission.WAKE_LOCK`, and `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (androidx.core declares and uses it, signature level; not in the `plan.md:453` list, see OQ-0a-20).
- Phase 0a's own manifest declares only `INTERNET` and `ACCESS_NETWORK_STATE`.
- `MainActivity.configChanges` (A8, exact): `orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode`.

### 5.6 Version code (A2, 14 §11.3)

`version.properties`:
```properties
versionMajor=0
versionMinor=1
versionPatch=0
versionBuild=0
```
`versionName = "$major.$minor.$patch"` = `0.1.0`. `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build` = `10000`. `minor`, `patch` and `build` must each be 0–99, and `major` 0–2099; anything else fails configuration with a message naming the key.

---

## 6. Tasks

One task is one commit, unless a task says it is split. The commit that completes a `plan.md` checkbox ticks it (`- [ ]` → `- [x]`) in the same commit. Commit messages are Conventional Commits with a lower-case imperative subject and **no trailer of any kind** (no `Co-Authored-By`, whatever a tool suggests).

If your sandbox cannot write `.git/index.lock`, do the work, stage nothing, and put the exact commit message for each task in your hand-back; the orchestrator commits (CLAUDE.md).

Paths: `app/…/X.kt` means `app/src/main/kotlin/net/chronoflow/app/X.kt`. Source directories are `src/<set>/kotlin`, never `src/<set>/java`.

### Task 1. Repository (`plan.md` checkbox "Repository")

Usually done by the orchestrator before the worker starts. First check: `git -C /Users/rickgout/Sites/chronoflow-android log --oneline --all`. If `main` has exactly the docs commit and `feat/phase-0a-scaffold` exists with this brief committed, only verify the steps below and go on to Task 2.

1. `git init -b main`.
2. `.gitignore`, exactly:
   ```gitignore
   build/
   .gradle/
   .kotlin/
   local.properties
   .idea/
   *.iml
   captures/
   *.hprof
   .DS_Store
   *.jks
   *.keystore
   !app/debug.keystore
   .claude/*.lock
   .claude/settings.local.json
   ```
3. First commit on `main`: `git add plan.md AGENTS.md CLAUDE.md docs/research _design .gitignore`. **Do not add `docs/phases/`**: briefs are committed on their phase branch (CLAUDE.md "Brief"); OQ-0a-19. Message: `docs: add android plan, agent rules, research and boards`.
4. `git switch -c feat/phase-0a-scaffold`. Commit `docs/phases/phase-0a-scaffold.md`: `docs(phase-0a): add implementation brief`.
5. `tools/ios-pin.txt` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0` and `tools/web-pin.txt` = `e36368ec82e6c44c0a5b1c624324e2a4fb5ddd19`, each one line with a trailing newline (OQ-0a-9). Tick "Repository". Commit: `chore: pin ios and web source commits`.

In the step 5 commit, also apply OQ-0a-19's `plan.md` edit (`docs/` → `docs/research/`) in both places that name the first commit: the 0a "Delivers" sentence ("its first commit holds only plan.md, AGENTS.md, CLAUDE.md, docs/, …") and the first-commit bullet (`plan.md:710`).

Check: `git rev-list --count main` = `1`; `git show --stat main` lists no `docs/phases/` file; `git check-ignore -q app/debug.keystore; echo $?` prints `1` (not ignored; note that `git check-ignore -v` *does* print the matching `!app/debug.keystore` negation line and exits 0, so do not use `-v` for this check); `git check-ignore -q x.jks; echo $?` prints `0`.

### Task 2. Wrapper (checkbox "Wrapper")

1. Create a minimal `settings.gradle.kts` with only `rootProject.name = "chronoflow-android"` if the wrapper task refuses to run without one.
2. Bootstrap from the cached distribution (it only reads `~/.gradle`; it does not touch the RN repo):
   ```sh
   ~/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle wrapper \
     --gradle-version 9.9.1 --distribution-type bin \
     --gradle-distribution-sha256-sum d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228
   ```
3. Run `./gradlew wrapper --gradle-version 9.9.1 --distribution-type bin --gradle-distribution-sha256-sum d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228` once more, so that `gradle-wrapper.jar` and the scripts come from 9.9.1 itself.
4. From now on use only `./gradlew`.

Files: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (with `distributionSha256Sum`).

Edge: if AGP 9.5.1 (Task 3) rejects Gradle 9.9.1, regenerate the wrapper with `--gradle-version 9.6.0 --gradle-distribution-sha256-sum bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`, amend this task's commit before pushing on, and record the exact AGP error under OQ-A35 in `docs/tooling.md`.

Check: `./gradlew --version` prints `Gradle 9.9.1` and JVM 17. Commit: `build: add gradle 9.9.1 wrapper`.

### Task 3. Settings and catalog (checkbox "Settings and catalog")

Files:
- `settings.gradle.kts`:
  - `pluginManagement { includeBuild("build-logic"); repositories { google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }; mavenCentral(); gradlePluginPortal() } }`
  - `plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }`
  - `dependencyResolutionManagement { repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS; repositories { google { same content filter }; mavenCentral() } }`
  - `rootProject.name = "chronoflow-android"`
  - `include(...)` every module of §5.1 (`:app`, `:benchmark`, `:konsist`, the 14 `:core:*`, the 9 `:feature:*`, `:tools:tokengen`).
- `gradle/libs.versions.toml` (§5.2).
- `gradle.properties` (§5.4).
- `build-logic/settings.gradle.kts`: repositories (google, mavenCentral, gradlePluginPortal), `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`, `rootProject.name = "build-logic"`, `include(":convention")`.
- `build-logic/convention/build.gradle.kts`: `plugins { `kotlin-dsl` }`, Java/Kotlin target 17, `compileOnly` the build-logic libraries of §5.2, `testImplementation(libs.junit4)`, `testImplementation(libs.assertk)`. No plugins registered yet.
- Root `build.gradle.kts`: `plugins { alias(...) apply false }` for every plugin alias of §5.2 except `android-lint` (`android-application`, `android-library`, `android-test`, `kotlin-jvm`, `compose-compiler`, `kotlin-serialization`, `sqldelight`, `roborazzi`, `spotless`, `baselineprofile`). Declaring the Kotlin plugins (`kotlin-jvm`, `compose-compiler`, `kotlin-serialization`, all 2.4.21) here is what puts KGP 2.4.21 on the build classpath; AGP 9 alone brings an older KGP and Gradle resolves the highest (11 §2).

Module directories may not have build files yet; Gradle treats them as empty projects.

Tests (commands, no code):
- `./gradlew buildEnvironment | grep kotlin-gradle-plugin` shows the resolved version `2.4.21` (a line such as `2.2.10 -> 2.4.21` is fine).
- `./gradlew help` twice: the second run prints `Reusing configuration cache.`

Edge: if `android.onlyEnableUnitTestForTheTestedBuildType=false` is rejected, remove it and instead enable release unit tests in `:app` only (in Task 4), and record it.

Commit: `build: add settings, version catalog and gradle properties`.

### Task 4. Convention plugins and module skeletons (checkbox "Convention plugins")

Package `net.chronoflow.buildlogic` in `build-logic/convention/src/main/kotlin/net/chronoflow/buildlogic/`. Register the ten plugins in `build-logic/convention/build.gradle.kts` (`gradlePlugin { plugins { register(...) { id = ...; implementationClass = "net.chronoflow.buildlogic.<Class>" } } }`):

| Plugin id | Class | Does |
|---|---|---|
| `chronoflow.android.application` | `AndroidApplicationConventionPlugin` | Applies `com.android.application`. `compileSdk 37`, `minSdk 29`, `targetSdk 36`, Java 17, Kotlin options (below), `buildFeatures.buildConfig = true`, version from `version.properties` (§5.6), debug `applicationIdSuffix = ".debug"`, debug `signingConfig` from `rootProject.file("app/debug.keystore")` (store and key password `android`, alias `androiddebugkey`), release `optimization { enable = true }` with no `signingConfig`, `bundle { language { enableSplit = false } }` (OQ-0a-16), `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`, `testInstrumentationRunnerArguments["timeout_msec"] = "60000"`, `testOptions.animationsDisabled = true`, `testOptions.unitTests.isIncludeAndroidResources = true`, Lint options (Task 6), test launcher (below). The `applicationId` is set in `app/build.gradle.kts`, not here. |
| `chronoflow.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | Applies `chronoflow.android.application` + Compose setup (below). |
| `chronoflow.android.library` | `AndroidLibraryConventionPlugin` | Applies `com.android.library`. `compileSdk 37`, `minSdk 29`, `testOptions.targetSdk 36`, `lint.targetSdk 36`, Java 17, Kotlin options, `testOptions.unitTests.isIncludeAndroidResources = true`, Lint options, test launcher. |
| `chronoflow.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | Applies `chronoflow.android.library` + Compose setup. |
| `chronoflow.android.feature` | `AndroidFeatureConventionPlugin` | Applies `chronoflow.android.library.compose`; adds `implementation` of `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:model`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, `navigation3-runtime`. **Never a `:feature:*` project.** |
| `chronoflow.jvm.library` | `JvmLibraryConventionPlugin` | Applies `org.jetbrains.kotlin.jvm` and `com.android.lint` (so `:app`'s `checkDependencies` lints JVM modules). Java 17, Kotlin options, `testImplementation(junit4, assertk)`, test launcher. |
| `chronoflow.sqldelight` | `SqlDelightConventionPlugin` | Applies `chronoflow.android.library` + `app.cash.sqldelight`. Configures no database (4a does). See OQ-0a-6. |
| `chronoflow.android.test` | `AndroidTestConventionPlugin` | Applies `com.android.test`. `compileSdk 37`, `minSdk 29`, `targetSdk 36`, Java 17, Kotlin options, `targetProjectPath = ":app"`, runner as above. |
| `chronoflow.spotless` | `SpotlessConventionPlugin` | Root only. Task 6. |
| `chronoflow.roborazzi` | `RoborazziConventionPlugin` | Applies `io.github.takahirom.roborazzi`; adds `testImplementation` of `robolectric`, `roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`, `ui-test-junit4`. Applied in 0e, not now. |

Shared helpers (internal, same package): `KotlinOptions.kt` (`configureKotlin()`), `ComposeSetup.kt` (`configureCompose()`), `TestLauncher.kt` (`configureTestLauncher()`), `LintSetup.kt` (Task 6), `VersionProperties.kt`, `ProjectExtensions.kt` (`val Project.libs`).

- **Kotlin options**, for every `KotlinJvmCompile` task: `compilerOptions { jvmTarget.set(JvmTarget.JVM_17); allWarningsAsErrors.set(true) }`.
- **Compose setup:** apply `org.jetbrains.kotlin.plugin.compose`; `buildFeatures.compose = true`; `implementation(platform(compose-bom))`, `androidTestImplementation(platform(compose-bom))`; `composeCompiler { stabilityConfigurationFiles.add(<root>/stability_config.conf) }`; when the Gradle property `composeReports` is `true` (read through `providers.gradleProperty("composeReports")`), set `reportsDestination` and `metricsDestination` to `build/compose_compiler`.
- **Test launcher:** `tasks.withType<Test>().configureEach { javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }); maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1) }`. JVM tests use JUnit 4 (`useJUnit()`).
- **Version properties:** read `version.properties` through `providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText` and parse it in a pure function `VersionProperties.parse(text: String): AppVersion` (`AppVersion(name: String, code: Int)`), which throws `IllegalArgumentException` naming the key on a missing or out-of-range value (§5.6).
- **Configuration cache:** read every Gradle property and environment variable through `providers`. No `System.getenv`, no `project.findProperty`, no `File.readText` during configuration.

Module files:
- Every module of §5.1 gets a `build.gradle.kts` with its one plugin and, for Android modules, `android { namespace = "<namespace>" }`.
- `app/build.gradle.kts`: `android { namespace = "net.chronoflow.app"; defaultConfig { applicationId = "net.chronoflow.app" } }`, and `implementation(project(...))` of every `:feature:*` module (only `:app` knows the features) and of every `:core:*` module **except two**: `:core:data-mock` is `debugImplementation` only (A6: release contains no mock code; 0e adds the perf configurations), and `:core:testing` is never a main dependency (`testImplementation` / `androidTestImplementation` only; Task 5 adds the latter). For this task, `app/src/main/AndroidManifest.xml` is `<manifest />`; Task 5 fills it.
- `stability_config.conf` at the root, one line: `java.time.*`.
- `version.properties` (§5.6).
- `app/debug.keystore`, generated once:
  ```sh
  keytool -genkeypair -v -keystore app/debug.keystore -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  ```
  It is committed (14 §11.2). Its fingerprint must never go into a production `assetlinks.json` (OQ-A38).

Tests:
- `VersionPropertiesTest` (build-logic, JVM, `build-logic/convention/src/test/kotlin/net/chronoflow/buildlogic/`): `0/1/0/0` → `0.1.0`, `10000`; `1/2/3/4` → `1.2.3`, `1020304`; a missing key, a non-number, `minor=100`, `build=100` each throw with the key in the message.
- Command: `./gradlew assembleDebug` builds every module (`:app` produces `app/build/outputs/apk/debug/app-debug.apk`).
- Command: `./gradlew :build-logic:convention:test` passes.

Edges:
- If `compileSdk = 37` fails to find the `android-37.0` platform, use `compileSdk { version = release(37) { minorApiLevel = 0 } }` and record it.
- If SQLDelight 2.4.1 fails to configure under AGP 9 built-in Kotlin, or demands a database block, apply the fallback in OQ-0a-6.
- If a skeleton Android module (most likely `:benchmark`, `com.android.test`) fails because `src/main/AndroidManifest.xml` is missing, add a one-line `<manifest />` there and record it. Add nothing else.

Commit: `build: add convention plugins and module skeletons`.

### Task 5. Minimal app (checkbox "Minimal app")

Files in `:app`:
- `app/…/ChronoFlowApp.kt`, `class ChronoFlowApp : Application()`:
  - `val launchPath = LaunchPath()`
  - `lateinit var graph: AppGraph` with a private setter.
  - `onCreate()`: `if (BuildConfig.DEBUG) DebugStrictMode.install(this, launchPath)` (OQ-0a-1), then `super.onCreate()`, then `graph = AppGraph()`. Nothing else: no I/O, no DataStore, no channels, no library init.
- `app/…/AppGraph.kt`: `class AppGraph` with no members. Its members arrive in later phases, each `by lazy` (A4).
- `app/…/MainActivity.kt`, `class MainActivity : ComponentActivity()`:
  - `onCreate`: `super.onCreate(savedInstanceState)`; `setContent { Box(Modifier.fillMaxSize()) }`; then `window.decorView.viewTreeObserver.registerFrameCommitCallback { (application as ChronoFlowApp).launchPath.end() }` (API 29, one-shot).
  - No splash, no edge-to-edge, no theme code (3a).
- `app/…/diagnostics/LaunchPath.kt`: `class LaunchPath { @Volatile var isActive: Boolean = true; private set; fun end() { isActive = false } }`. Not an `object`: no process-wide static state.
- `app/…/diagnostics/LaunchPathViolationPolicy.kt`: `internal object LaunchPathViolationPolicy { fun shouldCrash(launchPathActive: Boolean, isDiskOrNetwork: Boolean, stackClassNames: List<String>): Boolean }` = `launchPathActive && isDiskOrNetwork && stackClassNames.any { it.startsWith("net.chronoflow.") }` (OQ-0a-2). Pure Kotlin, no Android types.
- `app/…/diagnostics/DebugStrictMode.kt`: `internal object DebugStrictMode { fun install(app: Application, launchPath: LaunchPath) }`:
  - `StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyLog().penaltyListener(app.mainExecutor) { v -> … }.build())`.
  - The listener computes `isDiskOrNetwork = v is DiskReadViolation || v is DiskWriteViolation || v is NetworkViolation` and the stack class names `v.stackTrace.map { it.className }`; when `LaunchPathViolationPolicy.shouldCrash(launchPath.isActive, …)` is true it throws `LaunchPathViolation(v)` (`class LaunchPathViolation(cause: Violation) : RuntimeException("Main-thread disk or network work on the launch path", cause)`), which crashes the debug app on the main thread.
  - `StrictMode.setVmPolicy(VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().build())`.
- `core/ui/src/main/res/values/strings_untranslatable.xml`: `<string name="app_name" translatable="false">ChronoFlow</string>` (OQ-0a-4).
- `app/src/main/res/values/themes.xml`: `<style name="Theme.ChronoFlow" parent="android:Theme.Material.Light.NoActionBar" />`; `app/src/main/res/values-night/themes.xml`: same name, parent `android:Theme.Material.NoActionBar`. A placeholder with no colors; 3a replaces it.
- `app/src/main/res/resources.properties`: `unqualifiedResLocale=en`; and `androidResources { generateLocaleConfig = true }` in `app/build.gradle.kts` (OQ-0a-3).
- `app/src/main/res/xml/data_extraction_rules.xml`: `<data-extraction-rules>` with `<cloud-backup>` and `<device-transfer>`, each holding only `<exclude domain="X" path="."/>` for X in `root`, `file`, `database`, `sharedpref`, `external`, `device_root`, `device_file`, `device_database`, `device_sharedpref`. No `<include>`. If Lint rejects a domain, drop only that domain and record it.
- `app/src/main/AndroidManifest.xml`:
  - `<uses-permission>` `INTERNET` and `ACCESS_NETWORK_STATE` only.
  - `<application android:name=".ChronoFlowApp" android:label="@string/app_name" android:theme="@style/Theme.ChronoFlow" android:allowBackup="false" android:dataExtractionRules="@xml/data_extraction_rules" android:usesCleartextTraffic="false" android:enableOnBackInvokedCallback="true" android:supportsRtl="false" tools:targetApi="33">`. No `android:icon` (0b). `localeConfig` is injected by `generateLocaleConfig`.
  - `<activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask" android:configChanges="<§5.5>" android:screenOrientation="portrait" android:windowSoftInputMode="adjustResize" tools:ignore="DiscouragedApi,LockedOrientationActivity">` with the `MAIN`/`LAUNCHER` intent filter. No App Link filters (5c). No `directBootAware`.
- `app/src/debug/AndroidManifest.xml`: `<application android:networkSecurityConfig="@xml/network_security_config" />`.
- `app/src/debug/res/xml/network_security_config.xml`:
  ```xml
  <network-security-config>
      <base-config cleartextTrafficPermitted="false" />
      <domain-config cleartextTrafficPermitted="true">
          <domain includeSubdomains="false">10.0.2.2</domain>
      </domain-config>
  </network-security-config>
  ```
- `core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/Smoke.kt`: `@Retention(AnnotationRetention.RUNTIME) @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION) annotation class Smoke`. 0e adds the other tier annotations next to it.
- `app/build.gradle.kts` dependencies: `activity-compose`, `ui`, `foundation`, `core-ktx`; `androidTestImplementation` of `ui-test-junit4`, `androidx.test.ext:junit`, `androidx.test:runner`, `assertk`, `project(":core:testing")`; `testImplementation(junit4, assertk)`.

Tests (cheapest tier first; follow AGENTS.md "Tests"):
- `LaunchPathViolationPolicyTest` (JVM, `app/src/test/kotlin/net/chronoflow/app/diagnostics/`): active + I/O + a `net.chronoflow.app.X` frame → `true`; inactive → `false`; not I/O → `false`; I/O with only `android.*`/`java.*` frames → `false`. Plus `LaunchPathTest`: starts active, `end()` makes it inactive, a second `end()` is harmless.
- `DataExtractionRulesTest` (JVM, `app/src/test/…/`): parses `src/main/res/xml/data_extraction_rules.xml` (the test's working directory is `app/`); both sections exist, contain no `<include>`, and exclude `root` and `device_root` at `.`.
- `DebugNetworkSecurityConfigTest` (JVM, `app/src/testDebug/kotlin/net/chronoflow/app/`): parses `src/debug/res/xml/network_security_config.xml`; base config is cleartext `false`; exactly one domain config, cleartext `true`, exactly one domain `10.0.2.2` with `includeSubdomains="false"`.
- `DebugAppIdentityTest` (JVM, `app/src/testDebug/…`): `BuildConfig.APPLICATION_ID == "net.chronoflow.app.debug"`, `BuildConfig.DEBUG` is `true`.
- `ReleaseAppIdentityTest` (JVM, `app/src/testRelease/…`): `BuildConfig.APPLICATION_ID == "net.chronoflow.app"`, `BuildConfig.DEBUG` is `false`. This also proves `testReleaseUnitTest` exists and runs.
- `AppLaunchSmokeTest` (emulator, `@Smoke` **on the class**, because 0e's `TierRegistryTest` counts class-level tier annotations only; `app/src/androidTest/kotlin/net/chronoflow/app/`), one method `launchesAsNativeDebugUnderStrictMode`:
  - `@get:Rule val compose = createAndroidComposeRule<MainActivity>()`;
  - `compose.waitUntil(5_000) { !(compose.activity.application as ChronoFlowApp).launchPath.isActive }`;
  - asserts `compose.activity.packageName == "net.chronoflow.app.debug"` and the lifecycle is at least `RESUMED`.
  - A launch-path violation crashes the process, which fails the run. Under 10 s. No `Thread.sleep`.

Commands (record outputs in `docs/tooling.md`, Task 10):
```sh
./gradlew :app:installDebug
$ANDROID_HOME/platform-tools/adb -s emulator-5554 shell pm list packages | grep net.chronoflow.app.debug
$ANDROID_HOME/platform-tools/adb -s emulator-5554 shell am start -W -n net.chronoflow.app.debug/net.chronoflow.app.MainActivity
$ANDROID_HOME/platform-tools/adb -s emulator-5554 shell pidof net.chronoflow.app.debug
$ANDROID_HOME/build-tools/37.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | grep -E "^package|sdkVersion|targetSdkVersion"
```
Expected: package listed; `Status: ok`; a pid; `package: name='net.chronoflow.app.debug'`, `sdkVersion:'29'`, `targetSdkVersion:'36'`.

Edges:
- If the smoke test crashes on a violation whose stack has no `net.chronoflow.` frame, the policy is wrong; fix the policy, not the test.
- If a violation does have our frame, our code did main-thread I/O: fix the code. Exception: if the only `net.chronoflow.` frames are our lifecycle overrides (`ChronoFlowApp.onCreate`, `MainActivity.onCreate`) and the I/O happens inside a framework or AndroidX call they make (`super.onCreate`, `setContent`), do not weaken the policy and do not add an allowlist: stop, record the full stack in `docs/tooling.md`, and report it in the hand-back for the orchestrator to decide.
- The final id is `net.chronoflow.app` (user, 2026-11-23; OQ-A1); the earlier Java-keyword install fallback is retired.

Commit: `feat(app): add minimal app shell with debug strictmode`.

### Task 6. Quality gates (checkbox "Quality gates"; two commits, tick in 6b)

**6a. Spotless, Lint and compiler gates.** Commit: `build: add spotless, lint and compiler gates`.
- `SpotlessConventionPlugin` (root): `kotlin { target("**/*.kt"); targetExclude("**/build/**", "**/generated/**", "**/*.generated.kt", "konsist/src/test/resources/**", "_design/**", "docs/**"); ktlint("1.8.0" from the catalog).customRuleSets(listOf("io.nlopez.compose.rules:ktlint:0.6.8")) }` and `kotlinGradle { target("**/*.kts"); targetExclude("**/build/**", "konsist/src/test/resources/**"); ktlint(...) }`.
- `.editorconfig`:
  ```ini
  root = true

  [*]
  charset = utf-8
  end_of_line = lf
  insert_final_newline = true
  trim_trailing_whitespace = true
  indent_style = space
  indent_size = 4

  [*.{kt,kts}]
  ktlint_code_style = ktlint_official
  max_line_length = 120
  ktlint_function_naming_ignore_when_annotated_with = Composable
  ```
- Lint (`LintSetup.kt`, applied by the application, library and test plugins and, through `com.android.lint`, the JVM plugin): `abortOnError = true`, `warningsAsErrors = true`, `lintConfig = rootProject.file("lint.xml")`, `xmlReport = true`, `htmlReport = true`; in `:app` also `checkDependencies = true`. No baseline file (A20 allows one only for generated code; there is none yet).
- `lint.xml` at the root disables only these issue ids (`severity="ignore"`), each recorded with its reason in `docs/tooling.md`: `GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion` (versions are pinned by A1, not by Lint), `MissingApplicationIcon` (0b adds the icon and removes this line; OQ-0a-5). Every other finding is fixed. A finding that contradicts a plan decision is suppressed at the element with `tools:ignore` (as `DiscouragedApi` and `LockedOrientationActivity` on `MainActivity` in Task 5, A10/D16: lint-checks 32.x reports a fixed `screenOrientation` as `DiscouragedApi`, a default-enabled warning that `warningsAsErrors` turns into an error, while `LockedOrientationActivity` comes from the ChromeOS detector, which is off by default) and recorded.
- Kotlin `allWarningsAsErrors` is already in Task 4; nothing to add.
- `:app:analyzeReleaseR8Config`: run `./gradlew :app:tasks --all | grep -i analyzeReleaseR8Config`. If it exists, `make check` runs it (Task 9). If not, record its absence in `docs/tooling.md`.
- `app/src/main/keepRules/app.keep`:
  ```
  -assumenosideeffects class android.util.Log {
      public static int v(...);
      public static int d(...);
      public static int i(...);
  }
  ```
- Run `./gradlew spotlessApply`, then `./gradlew spotlessCheck :app:lintDebug :app:assembleRelease` must pass.
- **Probes, each proved once and reverted (not committed):**
  1. Lint: wrap the existing `setContent { … }` call in `MainActivity.onCreate` in `if (android.os.Build.VERSION.SDK_INT >= 21) { … }` (a statement, so Kotlin reports no unused-expression warning that would fail compilation first); `./gradlew :app:lintDebug` must fail with `ObsoleteSdkInt` reported as an error.
  2. Compiler: add `val unused = 1` inside `onCreate`; `./gradlew :app:compileDebugKotlin` must fail on the unused-variable warning.
  3. Spotless: add two trailing spaces to the end of one line of `MainActivity.kt`; `./gradlew spotlessCheck` must fail naming that file.

  Revert all three. Record the commands, exit codes and the issue lines.

**6b. Konsist.** Commit: `test(konsist): enforce module boundary rules`. Tick "Quality gates".

`:konsist` (JVM module, tests only). Files in `konsist/src/test/kotlin/net/chronoflow/konsist/`:
- `ProjectPaths.kt`: `fun moduleOf(path: String): String?` returns the **last** match of `/(app|feature/[a-z0-9-]+|core/[a-z0-9-]+|tools/[a-z0-9-]+)/src/` in a path (so planted files under `konsist/src/test/resources/violations/<rule>/feature/today/src/…` map to `feature/today`); `fun sourceSetOf(path: String): String?` returns the segment after that `/src/`; `fun isTestSourceSet(name)` = name starts with `test` or `androidTest`.
- `KonsistRules.kt`: one function per rule, each `fun <rule>(scope: KoScope)` that throws (Konsist `assertTrue`/`assertFalse`, or `KoAssertionFailedException`) on a violation.
- `KonsistRulesTest.kt`: for each rule, two tests: `<rule>()` runs it on the project scope and must pass; `<rule>RejectsPlantedViolation()` runs it on `Konsist.scopeFromDirectory("konsist/src/test/resources/violations/<rule>")` and must throw (assertk `assertFailure { … }`).
- `ModuleGraphTest.kt`: text rules over Gradle files (Konsist parses Kotlin, not the dependency graph).

Project scope: `Konsist.scopeFromProject()` minus every file whose path contains `/konsist/` (both the planted resources and the rule sources themselves: `KonsistRules.kt` holds `"GlobalScope."`, `runBlocking` and `Thread.sleep(` as string literals, so a text rule run over it would fail on itself). Build it once per test class (a `companion object` `by lazy`), not per test.

Every rule considers only files for which `moduleOf(path) != null` (the `app`, `feature/*`, `core/*` and `tools/*` modules). Files outside those, such as `build-logic/**` and any `build.gradle.kts`, are skipped. For rule 5, "non-test source set" means `sourceSetOf(path) != null && !isTestSourceSet(it)`.

Rules (A3). Import checks compare **whole package segments**: `net.chronoflow.feature.trips.X` is not inside `net.chronoflow.feature.trip`. Compare `import.startsWith("$prefix.")`, never `startsWith(prefix)`.

| # | Test name | Rule | Planted violation (under `konsist/src/test/resources/violations/<rule>/`) |
|---|---|---|---|
| 1 | `featuresDoNotImportOtherFeatures` | A file in `feature/<x>` imports nothing from `net.chronoflow.feature.<y>.` for `y != x` | `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/TripImportsTrips.kt` imports `net.chronoflow.feature.trips.TripsList` (this catches the prefix bug) |
| 2 | `jvmCoreModulesImportNoAndroidOrCompose` | Files in `core/{model,plan,sync,reminders,wizard,network,data-mock,testing}` and `tools/*` import nothing starting with `android.` or `androidx.compose.` | `core/plan/src/main/kotlin/net/chronoflow/core/plan/UsesCompose.kt` imports `androidx.compose.ui.graphics.Color`; `core/model/src/main/kotlin/net/chronoflow/core/model/UsesLog.kt` imports `android.util.Log` |
| 3 | `onlyAppImportsFeatures` | Files outside `app` and `feature/*` import nothing from `net.chronoflow.feature.` | `core/ui/src/main/kotlin/net/chronoflow/core/ui/ImportsFeature.kt` imports `net.chronoflow.feature.today.TodayScreen` |
| 4 | `noGlobalScope` | No file imports `kotlinx.coroutines.GlobalScope` or contains `GlobalScope.` | `core/sync/src/main/kotlin/net/chronoflow/core/sync/Fire.kt` calls `GlobalScope.launch { }` |
| 5 | `noRunBlockingOutsideTests` | No file in a non-test source set imports `kotlinx.coroutines.runBlocking` or matches `\brunBlocking\b` | `core/data/src/main/kotlin/net/chronoflow/core/data/Blocks.kt` calls `runBlocking { }` |
| 6 | `noSleepInTests` | No file in a test source set contains `Thread.sleep(` or `SystemClock.sleep(` | `core/plan/src/test/kotlin/net/chronoflow/core/plan/SleepsTest.kt` calls `Thread.sleep(10)`; `app/src/androidTest/kotlin/net/chronoflow/app/SleepsUiTest.kt` calls `SystemClock.sleep(10)` |
| 7 | `viewModelsEndWithViewModel` | Every class with a parent named `ViewModel` or `AndroidViewModel` has a name ending in `ViewModel` | `feature/today/src/main/kotlin/net/chronoflow/feature/today/TodayModel.kt`: `class TodayModel : ViewModel()` |
| 8 | `routesAreSerializable` | Every class or object with a direct or indirect parent named `NavKey` (`indirectParents = true`) is annotated `@Serializable` | `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/Routes.kt`: `sealed interface Route : NavKey` and `data object Today : Route` without `@Serializable` |

Allowed controls (must **pass**), under `konsist/src/test/resources/allowed/<rule>/`, each with its own test `<rule>AllowsControl()`:
- rule 1: `feature/trips/src/main/kotlin/net/chronoflow/feature/trips/OwnImport.kt` imports `net.chronoflow.feature.trips.TripsList`;
- rule 5: `core/data/src/test/kotlin/net/chronoflow/core/data/BlocksTest.kt` calls `runBlocking { }`;
- rule 8: the same `Routes.kt` with `@Serializable` on `Today`.

`ModuleGraphTest` (reads files with `java.io.File` from the project root, found by walking up from the working directory to the directory that contains `settings.gradle.kts` and `gradlew`; the real-project walk skips `konsist/src/test/resources/` — whose planted `build.gradle.kts` files would otherwise fail it — and every `build/`, `.gradle/` and `.kotlin/` directory):
- `featureModulesDoNotDependOnFeatureModules`: no `feature/*/build.gradle.kts` matches `project\(\s*":feature:` or `projects\.feature\.`.
- `onlyAppDependsOnFeatureModules`: no `build.gradle.kts` outside `app/` and no `build-logic/**/*.kt` matches those patterns.
- `…RejectsPlantedViolation` for each, run over `konsist/src/test/resources/violations/moduleGraph/` (`feature/today/build.gradle.kts` with `implementation(project(":feature:trip"))`; `core/ui/build.gradle.kts` with `implementation(project(":feature:today"))`).

`konsist/build.gradle.kts`: `testImplementation(libs.konsist)`. Because Konsist reads files Gradle does not know about, declare them as inputs, or the test stays UP-TO-DATE on a violation:
```kotlin
tasks.test {
    inputs.files(
        fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "_design/**", "docs/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
}
```

Planted files are resources: they are parsed, never compiled, and excluded from Spotless (6a). They must still be syntactically valid Kotlin.

Command: `./gradlew :konsist:test` passes (all rules on the project, all planted violations rejected, all controls allowed), under 10 s warm.

### Task 7. Release gates (checkbox "Release gates"; two commits, tick in 7b)

**7a. `verifyReleaseManifest`.** Commit: `build: verify the release manifest`.

- Pure rules in `build-logic/convention/src/main/kotlin/net/chronoflow/buildlogic/manifest/ManifestRules.kt`: `fun check(manifestXml: String, applicationId: String, allowedExportedComponents: Set<String>): List<ManifestViolation>`, where `ManifestViolation(rule: Rule, detail: String)` and `enum class Rule`. Parse with `DocumentBuilderFactory` (namespace-aware; `http://apache.org/xml/features/disallow-doctype-decl` = true). Android namespace `http://schemas.android.com/apk/res/android`. AGP's merged manifest writes fully qualified `android:name` values, and its `package` attribute is the applicationId (`net.chronoflow.app`), **not** the code namespace, so never resolve names against `package`. Write every planted manifest with fully qualified names; if a name still starts with `.`, resolve it against `net.chronoflow.app` (the `:app` namespace).

| Rule | Fails when |
|---|---|
| `PERMISSION` | a `<uses-permission>` or `<uses-permission-sdk-23>` name is not in the §5.5 allowlist (`<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` included) |
| `CUSTOM_SCHEME` | any `<data android:scheme>` is not `https` |
| `EXPORTED_COMPONENT` | an `activity`, `activity-alias`, `service`, `receiver` or `provider` has `android:exported="true"` and is not `net.chronoflow.app.MainActivity`, is not guarded by `android:permission="android.permission.DUMP"`, and is not in `allowedExportedComponents` (empty in 0a; 12b adds the reminder receivers). This is how an exported `net.chronoflow.debug.*` component fails. |
| `DEBUGGABLE_OR_TEST_ONLY` | `<application android:debuggable="true">` or `android:testOnly="true"` |
| `CLEARTEXT` | `android:usesCleartextTraffic` is not exactly `false`, or `android:networkSecurityConfig` is present |
| `MISSING_LOCALE_CONFIG` | `<application>` has no `android:localeConfig` |
| `BACKUP` | `android:allowBackup` is not exactly `false` (OQ-0a-15) |
| `DIRECT_BOOT` | any element has `android:directBootAware="true"` (OQ-0a-15) |

- Task `VerifyReleaseManifestTask` (`@CacheableTask`): `@InputFile @PathSensitive(NONE) mergedManifest`, `@Input applicationId`, `@Input allowedExportedComponents`, `@OutputFile report` (`build/reports/verifyReleaseManifest.txt`, `OK` or the violations). Fails with every violation listed, one per line, as `RULE: detail`.
- Wiring in `AndroidApplicationConventionPlugin`: `androidComponents.onVariants(selector().withBuildType("release")) { v -> register "verifyReleaseManifest" with mergedManifest = v.artifacts.get(SingleArtifact.MERGED_MANIFEST) and applicationId = v.applicationId }`; `check` depends on it.
- Planted manifests in `build-logic/convention/src/test/resources/manifests/`: `valid.xml` (MainActivity, the profileinstaller receiver with `android.permission.DUMP`, the DYNAMIC_RECEIVER permission, `localeConfig`), and one file per rule: `permission.xml` (`android.permission.CAMERA`), `custom-scheme.xml` (`chronoflow-uitest`), `exported-debug.xml` (`net.chronoflow.debug.SeedReceiverActivity` exported), `debuggable.xml`, `test-only.xml`, `cleartext.xml` (`usesCleartextTraffic="true"`), `network-security-config.xml`, `missing-locale-config.xml`, `backup.xml`, `direct-boot.xml`.
- `VerifyReleaseManifestTest` (build-logic, JVM): `valid.xml` → no violation; each planted file → exactly its rule (one violation).
- Command: `./gradlew :app:verifyReleaseManifest` passes on the real app.

**7b. `measureBundleSize`.** Commit: `build: add the release bundle size gate`. Tick "Release gates".

- Pure helpers in `build-logic/…/size/BundleSize.kt`:
  - `fun parseGetSizeTotal(csv: String): Long` (bundletool prints `MIN,MAX` then `<min>,<max>`; return MAX);
  - `fun verdict(bytes: Long): Verdict` = `OK` ≤ 8,000,000; `WARN` > 8,000,000; `FAIL` > 12,000,000 (decimal MB, OQ-0a-8);
  - `fun nativeLibraries(entryNames: List<String>): List<String>` = entries ending in `.so`.
- Task `MeasureBundleSizeTask` on `:app`, registered for the release variant with `bundle = v.artifacts.get(SingleArtifact.BUNDLE)` and a `bundletool` configuration resolving `com.android.tools.build:bundletool` (§5.2). Steps:
  1. List the AAB's zip entries; fail on any `.so`, naming each.
  2. Write `build/outputs/size/device-spec.json`: `{"supportedAbis":["arm64-v8a"],"supportedLocales":["en"],"screenDensity":480,"sdkVersion":36}` (arm64, xxhdpi, en).
  3. `javaexec` `com.android.tools.build.bundletool.BundleToolMain build-apks --bundle=<aab> --output=build/outputs/size/app.apks --device-spec=<spec> --overwrite`.
  4. `get-size total --apks=build/outputs/size/app.apks --device-spec=<spec>`; parse.
  5. Print `Bundle size: <bytes> bytes (<x.xx> MB), arm64-v8a/xxhdpi/en: OK|WARN|FAIL`, write it to `build/outputs/size/size-report.txt`, warn (log, exit 0) on `WARN`, fail on `FAIL`.
- `BundleSizeTest` (build-logic, JVM): parser on `MIN,MAX\n123,456\n` → 456 and on a malformed string → throws; verdict at 8,000,000 (OK), 8,000,001 (WARN), 12,000,001 (FAIL); `.so` detection on `base/lib/arm64-v8a/libx.so`.
- Command: `make size` (or `./gradlew :app:measureBundleSize`) prints the line; record the number.

### Task 8. Scripts (first half of checkbox "Makefile and scripts"; ticked in Task 9)

Commit: `build: add tier runner, emulator script and their tests`.

All scripts are POSIX `sh` or `bash` with `set -eu`, executable, and resolve the repo root as `"$(cd "$(dirname "$0")/.." && pwd)"`. Python helpers use the stdlib only and run with `python3 -I`.

**`Scripts/run-tier.sh`** → `exec python3 -I "$ROOT/Scripts/run_tier.py" "$@"`.

`Scripts/run_tier.py`, usage: `run-tier.sh <tier> [--device] --results <glob> [--results <glob> …] -- <command…>`.
- Creates `build/test-results/<tier>/` (removing its previous contents), tees the command's combined stdout and stderr to the terminal and to `build/test-results/<tier>/run.log`.
- Starts the command in a new session (`start_new_session=True`), so it leads its own process group.
- **Idle cap:** if the log has not grown for `TIER_IDLE_TIMEOUT` seconds (default 600), stop. **Total cap:** after `TIER_TOTAL_TIMEOUT` seconds (default 3600), stop. To stop: `SIGTERM` to the process group, wait `TIER_KILL_GRACE` seconds (default 5), then `SIGKILL` to the group, then run `TIER_STOP_CMD` (default `./gradlew --stop`). Print `run-tier: <tier> stopped after <n> s (idle cap)` or `(total cap)` to stderr **and** append the same line to `run.log` (the tests read it there). Exit `124`.
- After the command ends: collect every file matching the `--results` globs (relative to the repo root, `**` allowed), copy them into `build/test-results/<tier>/junit/`, and write `build/test-results/<tier>/timings.json`:
  ```json
  {"tier": "quick", "command": ["…"], "started": "2026-11-16T10:00:00Z", "wallSeconds": 41.2,
   "exitCode": 0, "tests": [{"class": "…", "name": "…", "seconds": 0.12, "status": "passed|failed|skipped"}]}
  ```
  parsed from JUnit XML `<testcase classname name time>` with `<failure>`/`<error>` → `failed`, `<skipped>` → `skipped`.
- Exit codes: the command's non-zero code if it failed (results are still collected); `2` with `run-tier: <tier> produced no result files` if the command succeeded but no file matched (AGENTS Tests rule 9); `124` on a cap; `75` when `--device` timed out waiting for the device lock (the command never ran); else `0`.
- Results are collected regardless of file age: a Gradle test task that is UP-TO-DATE keeps its last XML, which is valid for the current inputs. `connectedAndroidTest` deletes its old results itself.
- `TEST_DIAGNOSTICS=1` and a failed tier whose name starts with `smoke` or `emu`: also save `adb -s $ANDROID_SERIAL logcat -d` to `build/test-results/<tier>/logcat.txt`. Off by default (I12).
- **`--device`** (OQ-0a-21): before it starts the command and the caps, take the host-wide device lock with `Scripts/device_lock.py`'s `acquire()` (load it with `importlib.util.spec_from_file_location`, because `python3 -I` leaves the script directory off `sys.path`), so waiting never counts toward the idle or total cap. Skip the lock when `CF_DEVICE_LOCK_HELD=1` is already set (re-entry). Run the command with `CF_DEVICE_LOCK_HELD=1`, and release the lock when the tier ends. On a lock timeout, exit 75 without running the command.

**`Scripts/device_lock.py`** (stdlib, `python3 -I`; OQ-0a-21), usage `device_lock.py [--wait <s>] -- <command…>` or `device_lock.py --probe`:
- The lock file is `$CF_DEVICE_LOCK`, default `~/.android/cf-emulator.lock` (create the directory).
- `acquire(path, wait_s)`: open the file and try `fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)` once a second until it succeeds or `wait_s` passes. macOS has no `flock(1)`, so this is Python. After locking, write the holder's pid and command into the file; while waiting, print `device-lock: waiting for <holder>` once.
- `--wait` defaults to `$TIER_LOCK_TIMEOUT`, else 3600. On timeout, print `device-lock: timed out after <s> s` and exit 75.
- With the lock held, run the command as a child with `CF_DEVICE_LOCK_HELD=1` and exit with its code. When `CF_DEVICE_LOCK_HELD=1` is already set, run the command without locking (re-entry).
- The lock descriptor is not inheritable (the Python default; never pass it on), so an emulator started under the lock does not keep holding it.
- `--probe` exits 0 when another process holds the lock and 1 when it is free; it never waits.

**`Scripts/emulator.sh`**, subcommands `ensure [avd] | down [avd] | wipe | create <avd> | prep-functional [serial] | prep-perf [serial] | busy`. Every subcommand except `busy` and `wipe` re-executes itself under the device lock (`exec python3 -I "$ROOT/Scripts/device_lock.py" -- "$0" "$@"`) unless `CF_DEVICE_LOCK_HELD=1` is set (OQ-0a-21).
- Tools: `ANDROID_HOME` (default `/opt/homebrew/share/android-commandlinetools`), `ADB=${ADB:-$ANDROID_HOME/platform-tools/adb}`, `EMULATOR=${EMULATOR:-$ANDROID_HOME/emulator/emulator}`, `AVDMANAGER=${AVDMANAGER:-$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager}`, `SDKMANAGER=${SDKMANAGER:-…/sdkmanager}`, `PGREP=${PGREP:-pgrep}` (overridable for tests).
- Ports: `pixel` 5554, `fold` 5556, `small` 5558, `api29` 5560; serial `emulator-<port>`. Unknown AVD → exit 2.
- `ensure [avd=pixel]`: if `$ADB devices` lists the serial as `device` and `getprop sys.boot_completed` is `1`, apply the functional prep and exit 0. Otherwise start `$EMULATOR -avd <avd> -port <port> -no-window -no-audio -no-boot-anim -gpu host` in the background with `nohup`, output to `build/emulator/<avd>.log`, loading the Quick Boot snapshot (no `-no-snapshot-load`); `$ADB -s <serial> wait-for-device`; poll `sys.boot_completed` every 2 s up to 180 s (fail with the last 20 log lines); then the functional prep. Record the GPU mode the log reports (OQ-0a-11).
- `prep-functional [serial=emulator-5554]` (each command per device, never host-global; 13 §7.3, 11 §10.3): `settings put global window_animation_scale 0`, `transition_animation_scale 0`, `animator_duration_scale 0`; `settings put secure show_ime_with_hard_keyboard 1`; `settings put secure autofill_service null`; `cmd uimode night no`; `input keyevent 82`.
- `prep-perf [serial]`: the three animator scales set to `1`.
- `down [avd]`: with an AVD, `$ADB -s <serial> emu kill`; without, every `emulator-*` serial in `$ADB devices`. Wait up to 60 s for each to leave `adb devices`.
- `busy`: exit 0 (busy) when `device_lock.py --probe` reports the lock held by another process (skipped when `CF_DEVICE_LOCK_HELD=1`), when `$PGREP -f 'GradleWorkerMain|run_tier.py'` matches on the host, or when `$ADB -s emulator-5554 shell pgrep -f instrument` matches on the device (I6); otherwise exit 1. It never waits for the lock.
- `wipe`: refuse with exit 1 and `emulator.sh: tests are running; not wiping` when `busy` exits 0. Otherwise, under the lock taken with `device_lock.py --wait 0`, `down pixel`, then boot `pixel` once with `-wipe-data` (same flags as `ensure`), wait for boot, apply the functional prep.
- `create <avd>`: only `fold`, `small`, `api29`; refuse if `$AVDMANAGER list avd -c` already lists it. `fold`: `-k "system-images;android-36;google_apis;arm64-v8a" -d pixel_9_pro_fold`; `small`: same image, `-d small_phone`; `api29`: first `$SDKMANAGER "system-images;android-29;google_apis;arm64-v8a"`, then `-d pixel_8`. Each through `echo no | $AVDMANAGER create avd -n <avd> -k … -d …`.

**Tests**, run by `Scripts/test_scripts.sh` (`python3 -I -m unittest discover -s "$ROOT/Scripts/tests" -p 'test_*.py'`), in quick, total under 20 s:
- `Scripts/tests/test_run_tier.py` (temporary repo root per test; `TIER_STOP_CMD=true`; `TIER_KILL_GRACE=1`):
  - `test_idle_cap_stops_a_silent_command`: command `sh -c 'trap "" TERM; sleep 30'`, `TIER_IDLE_TIMEOUT=1` → exit 124 within 4 s; the log contains `idle cap`.
  - `test_total_cap_stops_a_chatty_command`: a command printing a line every 0.2 s forever, `TIER_TOTAL_TIMEOUT=2`, idle 10 → exit 124 within 5 s; `total cap`.
  - `test_kills_the_whole_process_group`: the command starts a background `sleep 300`, writes its pid to a file, then hangs; after the cap, that pid no longer exists.
  - `test_missing_results_fail`: `true` with a glob that matches nothing → exit 2.
  - `test_collects_results_and_timings`: the command writes a JUnit XML fixture with one passed and one failed case → files copied, `timings.json` lists both with `seconds` and `status`.
  - `test_command_failure_propagates`: `sh -c 'exit 3'` with a matching result file → exit 3, timings still written.
  - `test_device_lock_timeout`: a helper process holds the lock (`CF_DEVICE_LOCK` in the temp root); `--device` with `TIER_LOCK_TIMEOUT=1` → exit 75 within 3 s, and the command never ran (its marker file is absent).
- `Scripts/tests/test_emulator.py` (fake `adb`, `emulator`, `avdmanager`, `pgrep` shims in a temp dir, recording their arguments):
  - `test_wipe_refuses_while_gradle_tests_run` (fake `pgrep` matches) → exit 1, `emulator` never called;
  - `test_ensure_skips_boot_when_booted` (fake `adb devices` lists `emulator-5554 device`, `boot_completed` 1) → `emulator` never called, the seven prep commands (three animator scales, `show_ime_with_hard_keyboard`, `autofill_service`, `cmd uimode night no`, `input keyevent 82`) each sent with `-s emulator-5554`;
  - `test_create_rejects_unknown_avd` → exit 2;
  - `test_create_api29_installs_the_image_first` → `sdkmanager` called before `avdmanager`.
  - `test_busy_while_the_lock_is_held` (a helper process holds the lock in a temp `CF_DEVICE_LOCK`) → `busy` exits 0; `wipe` exits 1 and `emulator` is never called.
- `Scripts/tests/test_device_lock.py` (`CF_DEVICE_LOCK` in a temp dir; a helper process holds the lock where needed):
  - `test_runs_when_free`: `-- sh -c 'echo $CF_DEVICE_LOCK_HELD'` prints `1` and exits 0;
  - `test_waits_then_times_out`: with the lock held, `--wait 1 -- true` prints `waiting for` and `timed out` and exits 75 within 3 s;
  - `test_reentry_does_not_wait`: with the lock held and `CF_DEVICE_LOCK_HELD=1` set, `--wait 1 -- true` exits 0 at once;
  - `test_probe`: `--probe` exits 1 when the lock is free and 0 while it is held.

No `sleep` in a test body beyond what the stand-in commands do; no test waits more than its cap plus grace.

### Task 9. Makefile, ci.sh, targeted and measure-tiers (completes checkbox "Makefile and scripts")

Commit: `build: add makefile, ci and tier measurement`. Tick "Makefile and scripts".

**`Makefile`** header:
```make
SHELL := /bin/bash
.DEFAULT_GOAL := quick
.NOTPARALLEL:
export ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17)
export ANDROID_SERIAL ?= emulator-5554
GRADLE := ./gradlew --console=plain
RUN_TIER := Scripts/run-tier.sh
JVM_MODULES := core:model core:plan core:sync core:reminders core:wizard core:network core:data-mock core:testing konsist tools:tokengen
SMOKE := net.chronoflow.core.testing.tiers.Smoke
GENERATORS :=
GENERATOR_CHECKS :=
```

Targets (all `.PHONY`):

| Target | Recipe in 0a |
|---|---|
| `quick` | `$(RUN_TIER) quick --results '*/build/test-results/test/*.xml' --results '*/*/build/test-results/test/*.xml' --results 'build-logic/convention/build/test-results/test/*.xml' -- $(GRADLE) spotlessCheck $(JVM_MODULES:%=:%:test) :build-logic:convention:test`, then `Scripts/test_scripts.sh`. 0e adds the Robolectric logic tier. |
| `check` | `quick`, then `$(RUN_TIER) check --results 'app/build/test-results/**/*.xml' -- $(GRADLE) :app:lintDebug :app:testDebugUnitTest :app:testReleaseUnitTest :app:verifyReleaseManifest :app:analyzeReleaseR8Config` (drop the last task if it does not exist, Task 6a), then `generators-check`, then `Scripts/emulator.sh ensure`, then `$(RUN_TIER) smoke --device --results 'app/build/outputs/androidTest-results/connected/**/*.xml' -- $(GRADLE) :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=$(SMOKE)`. 0e adds the Robolectric layout tier. |
| `test` | `$(GRADLE) test` (every JVM and unit test, no emulator). |
| `format` | `$(GRADLE) spotlessApply` |
| `lint` | `$(GRADLE) spotlessCheck :app:lintDebug :konsist:test` |
| `targeted` | `python3 -I Scripts/targeted.py $(ONLY)` (reads `REUSE_BUILD` from the environment) |
| `generate` | `$(GENERATORS)`; with none, prints `generate: no generators yet (0b, 0c, 0d add them)` and exits 0 |
| `generators-check` | `$(GENERATOR_CHECKS)`; with none, prints `generators-check: nothing to check yet` and exits 0 |
| `size` | `$(GRADLE) :app:measureBundleSize` |
| `emu` / `emu-down` / `emu-clean` | `Scripts/emulator.sh ensure` / `down` / `wipe` |
| `avd-create` | `Scripts/emulator.sh create $(AVD)` (fails with usage if `AVD` is empty) |
| `matrix` | On request only. For each of `fold small api29`: `Scripts/emulator.sh ensure <avd>`, then the smoke run with `ANDROID_SERIAL=emulator-<port>` through `$(RUN_TIER) smoke-<avd> --device`, then `Scripts/emulator.sh down <avd>`. Fails early, naming `make avd-create AVD=<avd>`, when an AVD does not exist. |
| `du` | `du -sh ~/.gradle/caches ~/.android/avd build 2>/dev/null`, then `du -sh` of every path in `git worktree list --porcelain` |
| `tokens` `fonts` `assets` | `@echo "make $@: lands in Phase 0b (feat/phase-0b-tokens)" >&2; exit 1` |
| `strings` `plan-copy` | same, Phase 0c (`feat/phase-0c-strings`) |
| `contracts` | same, Phase 0d (`feat/phase-0d-contracts`) |
| `snapshots` `captures` `perf` | same, Phase 0e (`feat/phase-0e-test-harness`) |
| `perf-timeline` `baseline-profile` | same, Phase 1e (`feat/phase-1e-timeline-perf`) |

`check` is the iOS commit gate: it fails if any step fails; it never hides a failed step behind a later one.

**`Scripts/targeted.py`**: `ONLY="<module>:<Class>[#method] …"`, where `<module>` is a Gradle path without the leading colon (`konsist`, `core:plan`, `app`) and `<Class>` a simple name.
- The class is instrumented if `<module dir>/src/androidTest/**/<Class>.kt` exists; its fully qualified name comes from that file's `package` line.
- Instrumented, default: `$(GRADLE) :<module>:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>[#method]` (several classes comma-separated) through `run-tier.sh targeted-emu --device`.
- Instrumented with `REUSE_BUILD=1`: `adb -s $ANDROID_SERIAL shell am instrument -w -r -e class <fqcn>[#method] -e timeout_msec 60000 net.chronoflow.app.debug.test/androidx.test.runner.AndroidJUnitRunner` (no build, no install), run under the device lock as `python3 -I Scripts/device_lock.py -- adb …` (OQ-0a-21).
- Otherwise: `:<module>:test` for JVM modules, `:<module>:testDebugUnitTest` for Android modules, with `--tests '*.<Class>[.<method>]'`, through `run-tier.sh targeted`.
- Selectors for the same module and kind share one Gradle invocation. An unknown module or class exits 2 before anything runs. `--print` prints the commands instead of running them.
- `Scripts/tests/test_targeted.py` (with `--print`): `konsist:KonsistRulesTest#noGlobalScope` → `:konsist:test --tests '*.KonsistRulesTest.noGlobalScope'`; `app:AppLaunchSmokeTest` → `connectedDebugAndroidTest` with `class=net.chronoflow.app.AppLaunchSmokeTest` through `run-tier.sh targeted-emu --device`; the same with `REUSE_BUILD=1` → the `am instrument` line prefixed with `python3 -I Scripts/device_lock.py --`; `nope:X` → exit 2.
- `Scripts/tests/test_makefile.py::test_device_recipes_take_the_lock` (OQ-0a-21): every Makefile recipe (continuation lines joined) that names a `connected…AndroidTest` task, an `androidTest-results/connected` glob or `am instrument` also contains `--device` or `device_lock.py`.

**`Scripts/ci.sh`**:
```sh
#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
make generate check
git diff --exit-code
test -z "$(git status --porcelain)" || { git status --porcelain; echo "ci.sh: untracked or changed files after generate" >&2; exit 1; }
```
(The untracked-file check is OQ-0a-14.)

**`Scripts/measure-tiers.sh [runs=3]`**: `make emu`; one unmeasured warm-up of `make quick` and `make check`; then `runs` measured runs of each, with wall time measured in seconds to two decimals (`python3 -I -c 'import time; print(time.time())'` before and after each run). Writes `build/tier-timings.json`:
```json
{"date": "2026-11-16", "commit": "<git rev-parse --short HEAD>",
 "host": {"cpu": "<sysctl -n machdep.cpu.brand_string>", "memGiB": 36},
 "emulator": "pixel (Pixel 8, API 36)", "runs": {"quick": [s, s, s], "check": [s, s, s]},
 "median": {"quick": s, "check": s}, "budget": {"quick": 60, "check": 300}}
```
`memGiB` is computed (`sysctl -n hw.memsize` ÷ 2^30, rounded), never typed in. Exits 1 and names the tier when a median is over budget.

Commands:
- `make quick` passes. `make check` passes with `pixel` booted.
- `make tokens; echo $?` prints the Phase 0b line and `1`. `make generate; echo $?` prints `0`.
- `make targeted ONLY="konsist:KonsistRulesTest#noGlobalScope"` runs one test.
- `Scripts/ci.sh` passes on a clean clone: `git clone /Users/rickgout/Sites/chronoflow-android "$TMPDIR/cf-ci" && cd "$TMPDIR/cf-ci" && git switch feat/phase-0a-scaffold && Scripts/ci.sh`. Then delete the clone and its `build/`.

### Task 10. Measure and record (checkbox "Measure and record")

Commit: `docs: record phase 0a tooling results`. Tick "Measure and record".

`docs/tooling.md`, mirroring iOS `Docs/tooling.md` "Phase 0 verified results". Each entry gives who ran it (worker sandbox or orchestrator), the command, the exit code and the relevant output lines.

Sections:
1. **Toolchain:** `./gradlew --version` (Gradle, JVM), the KGP line from `buildEnvironment`, AGP, the JDK 21 path foojay provisioned (`~/.gradle/jdks/…`), whether 9.9.1 was accepted (OQ-A35), `compileSdk` form used.
2. **Sandbox capabilities** (one row each, `yes`/`no` plus the exact output when `no`): Gradle daemon (`./gradlew help`), dependency download, foojay JDK 21 download, JVM tests (`./gradlew :konsist:test`), `python3 -I` scripts (`Scripts/test_scripts.sh`), `adb devices`, `Scripts/emulator.sh ensure`, `:app:connectedDebugAndroidTest`, `bundletool` via Gradle (`make size`), `git commit` (`.git/index.lock`).
3. **Commands the orchestrator must run outside the sandbox**, derived from section 2.
4. **Tier timings:** the contents of `build/tier-timings.json` (three warm runs each, median, budget), with the date, commit, host and emulator. Budgets: quick < 60 s, check < 300 s. Over budget is a failure to fix before 0b–0e start, not a number to accept.
5. **Gates:** Lint probe (Task 6a), `allWarningsAsErrors` probe, the `lint.xml` ignores and their reasons, the `tools:ignore` ids on `MainActivity` (`DiscouragedApi`, `LockedOrientationActivity`) with A10 as their reason, `analyzeReleaseR8Config` (present and its output, or absent), `verifyReleaseManifest` on the real app, `make size` line, the AAB entry count and that no `.so` exists.
6. **App:** the Task 5 commands and outputs, `AppLaunchSmokeTest` result and duration, any StrictMode log lines (`adb logcat -d -s StrictMode`) during launch.
7. **Merged manifest inventory:** every `androidx.startup.InitializationProvider` `<meta-data>` entry and every other library component in `app/build/intermediates/merged_manifest/release/…/AndroidManifest.xml` (OQ-0a-10).
8. **Deviations and open questions:** every OQ-0a default you applied, and any fallback taken (SQLDelight, Gradle version, a property removed, a data-extraction domain dropped).

If a command cannot run in your sandbox, write the command and its exact output under "Pending: orchestrator" and do not claim a result.

Also run `make du` and paste its output, then `./gradlew --stop` and `make emu-down` (CLAUDE.md "Cleanup").

---

## 7. Acceptance criteria

All must hold on `feat/phase-0a-scaffold`. The orchestrator runs what the sandbox cannot.

1. `git rev-list --count main` is `1`; that commit has no `docs/phases/` file. The branch holds Tasks 1–10 as atomic Conventional Commits with no trailers, and `plan.md` Phase 0a has all nine checkboxes ticked.
2. `./gradlew --version` reports Gradle 9.9.1 (or 9.6.0 with OQ-A35 recorded). `./gradlew buildEnvironment | grep kotlin-gradle-plugin` resolves 2.4.21. A second `./gradlew help` prints `Reusing configuration cache.`
3. `./gradlew assembleDebug` builds every module; `./gradlew :app:assembleRelease` builds with R8.
4. `aapt2 dump badging` shows `net.chronoflow.app.debug`, `sdkVersion:'29'`, `targetSdkVersion:'36'`. `adb shell pm list packages` lists it. `AppLaunchSmokeTest` passes under the launch-path `penaltyDeath` policy.
5. `./gradlew :konsist:test` passes, and every rule's planted violation is rejected and every control allowed. `./gradlew :build-logic:convention:test` passes, with every planted manifest failing exactly its rule. `./gradlew :app:verifyReleaseManifest` passes on the real app.
6. `make size` prints the bundle line with verdict `OK` and no `.so`.
7. Warm, with `pixel` booted: median `make quick` < 60 s and median `make check` < 300 s over three runs, recorded in `docs/tooling.md` with `build/tier-timings.json`.
8. `Scripts/ci.sh` exits 0 on a clean clone of the branch.
9. A not-yet-landed target (`make tokens`) prints its phase and exits 1. `make generate` and `make generators-check` exit 0.
10. Verify the final applicationId and debug suffix in the identity tests and manifest fixtures. StorageNamespace service/alias strings retain the pinned iOS names (OQ-A629); they are independent of the applicationId.
11. `docs/tooling.md` has all eight sections; every deviation names its OQ.

---

## 8. Review checklist (Opus reviewer)

Verify each item against the diff and by running the command; quote file and line for each finding.

**Build**
- No `org.jetbrains.kotlin.android`, `kotlin-kapt`, KSP, `android.builtInKotlin=false` or `android.newDsl=false` anywhere (`grep -rn` over `*.kts`, `*.kt`, `gradle.properties`).
- `targetSdk = 36` is explicit in the application and test plugins, and `testOptions.targetSdk`/`lint.targetSdk` are 36 in the library plugin. Confirm with `aapt2 dump badging`.
- `libs.versions.toml` equals §5.2: no extra or missing version, no `+`, no pre-release.
- Every namespace is distinct and none contains `native`; `data-mock` uses `datamock`.
- Every module applies exactly one module-kind `chronoflow.*` plugin. No feature convention or feature module adds a `:feature:*` dependency.
- No `afterEvaluate`, no `applicationVariants`, no `System.getenv`, `project.findProperty` or `File(...).readText()` at configuration time. Run `./gradlew help` twice and check for `Reusing configuration cache.`; `--configuration-cache-problems=fail` is set.
- `version.properties` is read through `providers.fileContents`; `VersionPropertiesTest` covers the bounds.
- The `Test` launcher is JDK 21 (`./gradlew :konsist:test --info | grep -i "jdk\|java home"`).
- No `org.gradle.java.home` or other machine path is committed.

**App**
- `ChronoFlowApp.onCreate` does only StrictMode (debug), `super.onCreate()` and `AppGraph()`. No library initialization, no I/O.
- `DebugStrictMode` is reached only behind `BuildConfig.DEBUG`; it is absent from release: `grep -c DebugStrictMode app/build/outputs/mapping/release/mapping.txt` is `0` (R8 removed it).
- The penalty listener runs on `mainExecutor` and crashes only when `LaunchPathViolationPolicy` says so; the policy's test covers the four cases; the launch path ends on the first frame commit.
- The release merged manifest has no `networkSecurityConfig`; the debug one has it, and the debug config allows cleartext only to `10.0.2.2`.
- The manifest matches Task 5: `allowBackup="false"`, `dataExtractionRules`, `usesCleartextTraffic="false"`, `enableOnBackInvokedCallback="true"`, `supportsRtl="false"`, `singleTask`, the exact `configChanges`, `portrait`, `adjustResize`, no `directBootAware`, no App Links, only two permissions.
- `app/debug.keystore` is committed and not ignored; no other keystore is.

**Gates**
- Konsist import rules compare whole segments (the `trip`/`trips` planted case exists and fails; the `trips` control passes).
- Planted files are outside every compiled source set, excluded from Spotless, and excluded from the project scope; the `:konsist:test` inputs include all `*.kt` and `*.kts` (edit a planted rule into a real module and see the test re-run and fail; revert).
- `verifyReleaseManifest` reads the merged release manifest artifact (not `src/main/AndroidManifest.xml`), is wired into `check`, and has one planted manifest per rule plus `valid.xml`.
- `lint.xml` ignores only the four listed ids; any `tools:ignore` names its plan decision in `docs/tooling.md`.
- `measureBundleSize` uses bundletool from the Gradle configuration (not `PATH`), the exact device spec, and decimal thresholds; the `.so` check reads the AAB.

**Scripts**
- run-tier kills the process group (not only the child pid), honours the grace, returns 124 on caps and 2 on missing results, and writes `timings.json`.
- `emulator.sh` sends every `adb` command with `-s <serial>`, never changes host-global settings, refuses `wipe` while tests run, and `create` refuses unknown AVDs.
- Every not-landed target prints its phase and exits 1; `generate` and `generators-check` are aggregates that exit 0 when empty.
- `ci.sh` ends with `git diff --exit-code` and the untracked check.
- `Scripts/test_scripts.sh` runs in `make quick` and takes under 20 s.

**Tests and process**
- Tests follow AGENTS.md "Tests": cheapest layer (JVM for the policy, rules, parser, version code; emulator only for the launch), no `Thread.sleep`/`SystemClock.sleep`, one emulator test, each under its budget in `timings.json`.
- No product code: no tokens, colors, copy other than `app_name`, routes or screens.
- Commits are atomic, Conventional, trailer-free, and each ticks its `plan.md` box; `main` has one commit.
- `docs/tooling.md` gives numbers with command, host, emulator and build type, and every "no" in the sandbox table has the exact output.

**Typical mistakes to look for**
- `startsWith("net.chronoflow.feature.trip")` without the trailing dot.
- `compileSdk` set but `targetSdk` missing, so it silently becomes 37.
- The size or manifest task reading a hard-coded path under `build/intermediates` instead of the variant artifact.
- `make check` calling `connectedDebugAndroidTest` without the `@Smoke` annotation filter.
- `Konsist.scopeFromProject()` including `konsist/` (its planted resources or its own rule sources), or `ModuleGraphTest` walking into `konsist/src/test/resources/`, so the real-project tests fail.
- `:app` depending on `:core:data-mock` outside `debugImplementation`, or on `:core:testing` outside test configurations (A6).
- `git check-ignore -v` used to prove the keystore is not ignored (it prints the negation line and exits 0).
- run-tier filtering results by modification time, which fails every warm run whose tests are UP-TO-DATE.
- A Makefile aggregate that calls a not-yet-landed target and therefore always exits 1.
- The brief committed on `main`.

---

## 9. Open questions (proceed on the default)

Each default is what you build. If one turns out wrong, record it in `docs/tooling.md` and in your hand-back; add a user-facing question to `plan.md` "Open questions for the user" only where noted.

| # | Question | Default |
|---|---|---|
| OQ-0a-1 | AGENTS.md puts debug code in `src/debug`, but `ChronoFlowApp` in `src/main` must call the StrictMode installer, and the perf build types (0e) do not inherit `src/release`. | `DebugStrictMode` lives in `src/main` behind `if (BuildConfig.DEBUG)`, a compile-time constant that R8 removes from release; the mapping check in §8 proves it. Seed and mock code still go to `src/debug` (0e). |
| OQ-0a-2 | Which launch-path violations are fatal? A21 says `penaltyDeath` for disk reads on the launch path; framework code may read disk on its own. | Fatal: disk read, disk write or network on the main thread while the launch path is active, **and** a `net.chronoflow.` frame in the stack. Framework-only violations are logged and listed in `docs/tooling.md`. The launch path ends at the first frame commit; 3a/3b move the end to the hand-off. |
| OQ-0a-3 | `verifyReleaseManifest` must fail on a missing `localeConfig`, but `plan.md` puts `generateLocaleConfig` in 0c. | Enable `generateLocaleConfig` and `resources.properties` (`unqualifiedResLocale=en`) here, so `check` is green. In the Task 5 commit, append "(done in 0a)" to the 0c "Locale wiring" bullet in `plan.md`; 0c keeps the Lint and Konsist parts. |
| OQ-0a-4 | Where does `app_name` live before 0c? | At its final place, `core/ui/src/main/res/values/strings_untranslatable.xml`; 0c finds it there. Annotate the 0c bullet the same way. |
| OQ-0a-5 | No icon until 0b, and `MissingApplicationIcon` is a Lint warning (an error here). | Ignore that one id in `lint.xml` until 0b, which removes the line. No placeholder icon (no design without a board). |
| OQ-0a-6 | SQLDelight 2.4.1 may not configure under AGP 9 built-in Kotlin, or may require a database block. | Apply `chronoflow.sqldelight` to `:core:database`. If configuration fails, apply `chronoflow.android.library` instead, keep the plugin class, record the exact error, and add a row to `plan.md` "Open questions for the user" for Phase 4a with default "keep SQLDelight; move the `.sq` schema into a JVM module if the plugin needs the Kotlin JVM plugin". |
| OQ-0a-7 | bundletool is not in A1 and not on `PATH`. | `com.android.tools.build:bundletool:1.18.3` (already in the local Gradle cache) through a Gradle configuration. If `./gradlew buildEnvironment` shows AGP 9.5.1 bringing a newer stable bundletool, pin that one instead and record it. |
| OQ-0a-8 | "8 MB / 12 MB": decimal or binary? | Decimal: 8,000,000 and 12,000,000 bytes, as Play reports sizes. |
| OQ-0a-9 | `tools/ios-pin.txt`: short `f391a1fa` (plan) or the full SHA? | The full SHA `f391a1fac33eb0f0ea9e3f565206b91118e793d0`; it names the same commit and cannot become ambiguous. |
| OQ-0a-10 | "No library gets an eager initializer", but Compose and AndroidX merge `androidx.startup` initializers (ProfileInstaller, possibly EmojiCompat and ProcessLifecycle). | Record the inventory only. Phase 3a keeps `ProcessLifecycleInitializer` and `ProfileInstallerInitializer` (A21), removes every other entry and adds the `STARTUP_INITIALIZER` manifest rule (plan.md OQ-A47). Do not remove anything in 0a. |
| OQ-0a-11 | Does `-gpu host` work with `-no-window` on this emulator? | Keep the plan's flags. If the emulator falls back to another renderer, record the mode from its log; perf runs (0e, 1e) decide. |
| OQ-0a-12 | AGP 9 may flag `android.useAndroidX=true` or another property as deprecated. | Remove only the flagged line and record it. Never add an opt-out. |
| OQ-0a-13 | `compileSdk = 37` vs the installed `android-37.0` platform. | Try `compileSdk = 37`; fall back to `compileSdk { version = release(37) { minorApiLevel = 0 } }`. Record which worked. |
| OQ-0a-14 | `plan.md` says `ci.sh` runs `git diff --exit-code`, which misses new untracked generated files. | Also fail on a non-empty `git status --porcelain`. |
| OQ-0a-15 | `plan.md` names six manifest cases; research `14 §6.3` adds `allowBackup` and `directBootAware`, which the plan's manifest spec also requires. | Check eight rules (the six plus `BACKUP` and `DIRECT_BOOT`). |
| OQ-0a-16 | `bundle { language { enableSplit = false } }` is in "Android spec: release" without a phase. | Set it in 0a, so the size gate measures the bundle that ships. |
| OQ-0a-17 | A1 lists `androidx.test.ext:junit` 1.3.0 but not the runner, `androidx.test:core` or JUnit. | `androidx.test:runner` and `androidx.test:core` 1.7.0 (the release train of ext:junit 1.3.0) and `junit:junit` 4.13.2. If 1.7.0 does not resolve, use the runner version that `androidx.test.ext:junit:1.3.0`'s POM depends on, and record it. |
| OQ-0a-18 | `make check` needs a booted emulator. Should it boot one? | Yes: `check` calls `Scripts/emulator.sh ensure`, which returns at once when `pixel` is already booted. Budgets are measured with `pixel` already booted. |
| OQ-0a-19 | `plan.md:710` puts all of `docs/` in the first commit, but CLAUDE.md "Brief" commits each brief on its phase branch, and dozens of briefs already sit in `docs/phases/`. | The first commit takes `docs/research/` (with `screens/`) but not `docs/phases/`. The other briefs stay untracked in the main checkout until the orchestrator commits each one on its own phase branch. In the Task 1 step 5 commit (`chore: pin ios and web source commits`), change `docs/` to `docs/research/` in the `plan.md:710` bullet. |
| OQ-0a-20 | `plan.md:453` allows five permissions, but androidx.core (≥ 1.9, so 1.19.1) merges `<uses-permission android:name="<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>`, so a strict five-name allowlist fails the real app. | Allow that one app-scoped, signature-level name in `ManifestRules` (§5.5) and list it in `docs/tooling.md` section 7; do not remove it with `tools:node="remove"` (androidx.core's `ContextCompat.registerReceiver` needs it). In the Task 7a commit, append "plus androidx.core's `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (OQ-0a-20)" to the `plan.md:453` bullet. |
| OQ-0a-21 | Parallel worktrees share one `pixel` (`emulator-5554`) and one applicationId, so two device tiers at once overwrite each other's APKs and interleave instrumentation. | A host-wide `fcntl` lock (`Scripts/device_lock.py`, `~/.android/cf-emulator.lock`) serializes every device tier (`run_tier.py --device`) and every `emulator.sh` subcommand except `busy`; JVM and Robolectric tiers stay parallel (review 2026-11-16). |

Inherited user questions that touch this phase keep their `plan.md` defaults: OQ-A1 (resolved: `net.chronoflow.app`, user, 2026-11-23), OQ-A2 (minSdk 29), OQ-A27 (foojay JDK 21; fallback: the user installs `zulu@21`, or Robolectric `sdk = 34` on JDK 17), OQ-A31 (no remote, never push), OQ-A35 (Gradle 9.9.1, else 9.6.0).
