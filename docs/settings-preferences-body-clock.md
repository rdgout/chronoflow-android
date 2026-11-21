# Phase 10b acceptance — 2026-11-21

Preferences and Body clock are implemented on `feat/phase-10b-settings-preferences-body-clock`. Models and ported test names follow iOS `f391a1fa`; the current boards supply the surfaces, subject to the brief's no-health-data corrections. Settings home exposes both completed pages in release.

Preferences keeps theme on this device and writes time format and hidden categories through the existing permitted outbox. Unknown categories survive edits. Reminders opens its existing seam without a subtitle; replay creates a fresh walkthrough without another completion write. Phase 12c owns the reminders subtitle.

Body clock validates and saves each complete group, preserves edits against incoming values, and suppresses stale-session save failures as iOS does. The ViewModel retains time sheets and uniquely identified scroll requests until acknowledgement. An older acknowledgement cannot consume a newer request. Successful saves request scrolling even when successive saves belong to the same group. Reduce Motion uses immediate scrolling. The mDFD field follows external replacements without echoing them into a save or overwriting newer typing; Dutch commas become decimal points. Done always writes a rounded five-minute time, including an untouched, initially unset sheet.

Theme changes use injected IO for DataStore, the existing device-config mirror and the platform night-mode sink. The root starts collecting only after a frame. Below API 31, its initial choice comes from the existing, bounded device-config read; no additional pre-frame read or Application work was added. The original root font-gate wiring remains exercised by its existing tests. The timeline, trip page, plan renderer, drawing caches, clock ownership and benchmark journeys are unchanged.

No Android-only copy, shipping dependency, permission, test allow-list entry or release seed code was added. All new board seeds remain under `src/debug`. All generated glyph output is committed with its manifest.

## Brief corrections and defaults

The initial refresh commit records R12: use `make vectors`, preserve the existing component layout tests, and treat explicit JSON null free-day times like absent values when copying habitual times. Later corrections are in their task commits:

- R13 / OQ-A499: the specified iOS 128 dp adaptive minimum produces two time columns on small, pixel8 and largeFont. The 280 dp minimum stacks them.
- R14 / OQ-A500: at minimum width, drink labels wrap above the controls only when their measured intrinsic width cannot fit beside the native targets. Board-width rows retain their horizontal layout.
- R15: fixture snapshots use the public sanitizing factories because their Kotlin constructors and copy functions are private.
- R16 / OQ-A501: the public SDK has a per-app night-mode setter but no getter. Device tests start in Light and assert dark pixels, effective activity night configuration, no recreation, DataStore persistence and relaunch without a theme seed. The JVM recording sink checks the exact platform mode. The connected runner removes its APK afterward, so the separate cold-start capture installs it and seeds Dark first.

OQ-A484–A498 copy all fifteen brief defaults in order; OQ-A499–A501 record the additional defaults above. OQ-A68 remains unused. Task 5 stays removed and has no commit.

## Verification

Wall times are command/tier times unless explicitly marked as case times. Targeted records are retained in `build/phase-10b/`; final logs are `/private/tmp/chronoflow-10b-*.log`. Every executed tier below produced result evidence.

| Check | Result | Wall time |
|---|---|---:|
| `SettingsBodyClockTests`, `BodyClockCupsValidationTest` | 14 passed | 3.64 s |
| `BodyClockReadinessTests` | 7 passed | 11.92 s |
| `SettingsPreferencesTests` | 3 passed | 4.61 s |
| `SettingsPendingProtectionTests` | 2 passed; real SQL/outbox/offline pull protection | 6.03 s |
| `ThemeChoiceResolveTest` | 6 passed | 4 s build |
| `DeviceAppearanceTest` | 4 passed | 4 s build |
| Existing `RootThemeTest` | 4 passed | initial app invocation |
| `RootThemeChoiceTest` | 3 passed, including API 30 | 19.74 s with SDK resolution |
| Restored `noDataStoreReadBeforeFirstFrame` | passed after deliberate barrier-removal mutation failed | 4.05 s |
| Preference component/copy/layout classes and existing components | 33 passed | 7.39 s |
| Preferences ViewModel, screen, home, replay and shell seed classes | all passed | 16.10 s |
| Preferences time-format method and shell seed follow-up | passed | 6.74 s |
| Body clock models, ViewModel, screen, store, binding, invalidation and existing home | 42 passed | 9.98 s |
| Actual rating binding effect and shell seed follow-up | passed | 9.98 s |
| Recomposition guard after formatting the section scopes | passed | 5.77 s |
| Board seed parser, state, existing settings seed and cold/reseed classes | 80 passed | 6.74 s |
| `SettingsBoardSeedRunnerTest` | passed; before-sign-in fixture reaches the real SQL store | 4.17 s |
| Preferences and Body clock guards | 48 passed | 11.94 s |
| Touched-module layout tier | 505 passed, 1 existing API-36 LocaleManager-shadow ignore; zero overruns | 28.99 s |
| Board captures, once | 40 passed / 40 PNGs | 21.04 s |
| `PreferencesThemeDeviceTest`, pixel API 36, once | 3 passed | 11.29 s tier / 11.36 s process |
| Manual Body clock native IO journey | all five sections saved; process-filtered StrictMode output empty | 40.52 s for the final four-section continuation |
| App and touched feature/data/datastore/design-system lint | passed after the annotation correction below | 65.90 s initial; app-only correction 6.68 s |
| Single `make quick` main stage | 5,472 passed, 1 stale glyph-count assertion; 5,302 measured cases, zero overruns or hangs | 48.87 s |
| Corrected `LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached` | passed | 3.67 s |
| Remaining quick build-logic stage | 37 passed, certified cached results | 2.78 s |
| Remaining quick tooling stage | 171 harness + 25 contracts + 49 strings + 11 vectors passed | 20.90 s total |
| Remaining quick serial timing stage | 7 passed; freshly executed metrics recorded in `docs/perf-timeline.md` | 3.75 s |
| Release unit tests, manifest verification and R8 analysis | 202 passed; both analyses passed; zero overruns | 22.76 s |
| `make generators-check` | passed, including glyphs, strings, contracts, fixtures and assets | 6.61 s |
| `make size`, before | 3,808,028 bytes (3.81 MB), arm64-v8a / xxhdpi / en | 43 s build, sequential retry |
| `make size`, after | 3,853,357 bytes (3.85 MB); native-library gate passed | 90.72 s |

The release bundle grew by 45,329 bytes (1.19%). No new `.so` appeared; the existing graphics-path allow-list is unchanged.

The final process wall times can differ slightly from the tier's own measurement. Warm-up costs are kept separate: pending-protection JDBC 204 ms; theme native-font runtime 697 ms on API 36 and 249 ms on API 30; body-clock store fixture 335 ms. Initial native UI cases used the existing cold allowances: root theme 1.525 s, component layout 2.082 s, rating binding 1.174 s, and body guards 2.869 s. In the concurrent quick run the rating-binding class's first SDK case took 5.517 s under its 20 s allowance. The body-clock recomposition guard was near the warm budget: 929 ms targeted and 986 ms in quick; it was not repeated to seek a faster number. All budgets remain unchanged.

The no-pre-frame-read test failed when its actual frame barrier was deliberately removed, then passed after restoration. Production section composition counters prove a chronotype edit recomposes About and leaves Caffeine and Light untouched. Quick also passed all 13 timeline invalidation and 124 list invalidation cases unchanged.

Development corrections were narrow. The first body guard run included fake native-rail controls outside Robolectric's modeled inset region; the corrected guard measures the actual Settings screen, and all 48 cases pass. The initial API-30 theme class could not write Robolectric's default Maven cache; the writable-cache configuration below resolved it. The initial device-test compile referenced the nonexistent public getter and did not execute any device case; R16 corrected the check before the one actual device run. Lint rejected a redundant `@RequiresApi` on the test; `@SdkSuppress` is retained. The single quick run found the existing Kotlin glyph count still at 55; it now expects 58 and the corrected method passes. The Python count had already been updated with the glyph generator. Quick was not rerun; its three remaining stages were completed separately.

The first `make size` stalled in R8 while another Gradle invocation was active. After three minutes, `./gradlew --stop` was run once and the sequential retry passed. Its exact terminal failure was:

```text
Gradle build daemon has been stopped: stop command received
make: *** [size] Error 1
```

Subsequent Gradle work ran sequentially. No test hung and no remaining command is blocked by the sandbox, adb or signing. The pixel emulator is left running.

## Commands and evidence

Gradle runs used `ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process`. Tests needing Robolectric SDK resolution used:

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_TOOL_OPTIONS='-Dmaven.repo.local=/Users/rickgout/.gradle/chronoflow-robolectric -Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2'
```

Each targeted invocation used `make targeted ONLY="…"`. The task selectors were:

```text
feature:settings:SettingsBodyClockTests feature:settings:BodyClockCupsValidationTest
feature:settings:BodyClockReadinessTests
feature:settings:SettingsPreferencesTests
core:data:SettingsPendingProtectionTests
core:model:ThemeChoiceResolveTest core:datastore:DeviceAppearanceTest app:RootThemeTest app:RootThemeChoiceTest
feature:settings:SettingsPreferenceComponentsTest feature:settings:SettingsPreferenceCopyTest feature:settings:SettingsComponentsLayoutTest feature:settings:SettingsComponentsTest
feature:settings:PreferencesViewModelTest feature:settings:PreferencesScreenTest feature:settings:SettingsHomeScreenTest feature:onboarding:PreferencesWalkthroughReplayTests app:ShellNavSettingsSeedTest
feature:settings:SettingsBodyClockTests feature:settings:SettingsBodyClockStoreTests feature:settings:BodyClockViewModelTest feature:settings:BodyClockScreenTest feature:settings:BodyClockRatingBindingTest feature:settings:BodyClockRecompositionTest feature:settings:SettingsHomeScreenTest
app:SettingsBoardStateSeedTest app:TestSeedSessionKeysTest app:SettingsTestSeedsTest app:ReseedEqualsColdSeedTest
app:SettingsBoardSeedRunnerTest
feature:settings:PreferencesGuardsTest feature:settings:BodyClockGuardsTest
app:PreferencesThemeDeviceTest
core:designsystem:LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached
```

Focused follow-ups used `app:RootThemeChoiceTest#noDataStoreReadBeforeFirstFrame`, `feature:settings:PreferencesViewModelTest#timeFormatSavesOnlyItsChangedKey`, `feature:settings:BodyClockRatingBindingTest`, `feature:settings:BodyClockRecompositionTest#chronotypeTapLeavesCaffeineAndLightUnchanged` and `app:ShellNavSettingsSeedTest`, as recorded in their task logs. Walkthrough logic lives in onboarding JVM tests, and preferences persistence in its ViewModel tests; no app-root replay test or allow-list exception was introduced.

The touched-module layout tier selects `:app:testDebugUnitTest`, `:core:data:testDebugUnitTest`, `:core:datastore:testDebugUnitTest`, `:core:designsystem:testDebugUnitTest`, `:feature:settings:testDebugUnitTest` and `:feature:onboarding:testDebugUnitTest` with `-Pcf.tier=layout`. `Scripts/run-tier.sh layout-10b` scopes results and timings to those six modules. Data and datastore have no layout cases; their logic is tested separately. Evidence: `build/test-results/layout-10b/timings.json`.

Lint used `./gradlew :app:lintDebug :feature:settings:lintDebug :feature:onboarding:lintDebug :core:data:lintDebug :core:datastore:lintDebug :core:designsystem:lintDebug`; only `:app:lintDebug` needed the annotation follow-up. Release used `:app:testReleaseUnitTest -Pcf.tier=quick :app:verifyReleaseManifest :app:analyzeReleaseR8Config` through `Scripts/run-tier.sh release-10b`.

## Native appearance and cold start

`make targeted ONLY="app:PreferencesThemeDeviceTest"` ran once under the device lock. Case times were 1.012 s for live change without recreation, 3.123 s for recreation/relaunch persistence, and 0.960 s for the separately seeded trip-screen theme change. Each checks the existing debug IO violation counter. `adb logcat -d -s StrictMode` produced 516 lines of system/other-process history and no `net.chronoflow` frames; the retained output is `build/phase-10b/task-12/strictmode.log`.

The first manual start ran after the connected runner had removed the APK:

```text
$ adb shell am start -W -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity
Starting: Intent { cmp=net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity }
Error type 3
Error: Activity class {net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity} does not exist.
```

After `adb install -r app/build/outputs/apk/debug/app-debug.apk`, the debug receiver seeded Preferences/Dark with a fresh test id. The separate manual cold-start check then ran under the device lock:

```text
$ adb shell am force-stop net.chronoflow.app.native.debug
$ adb shell am start -W -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity
Starting: Intent { cmp=net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity }
Status: ok
LaunchState: COLD
Activity: net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity
TotalTime: 483
WaitTime: 484
Complete
$ adb exec-out screencap -p > build/phase-10b/task-12/splash.png
1080x2400; centre-top pixel (540,200) = #06091C
```

The image shows the dark splash and moon. This checks appearance, not startup performance. It does not replace the host's physical-device or benchmark measurements.

The additional manual Body clock journey seeded `bodyReady` on pixel API 36 and changed chronotype, confirmed the native bedtime sheet, enabled the caffeine exclusion preference, selected unrated blockers and enabled flight movement breaks. Each section's structural Saved tag appeared. The host helper initially applied a button-height threshold to the Saved text and used a fast fling that skipped the sleep card; inspecting its bounds and using slow swipes corrected the helper. The same journey continued from its current position, without replaying the completed About save. No product code changed. The final four-section continuation took 40.52 s of host command time; it is a manual diagnostic, not an instrumented test case. Evidence is `build/phase-10b/manual-body-clock/{run,resume,remaining}.log` and the empty, process-filtered `strictmode.log`:

```text
Observed settings.bodyClock.saved.about
Observed settings.bodyClock.saved.sleep
Observed settings.bodyClock.saved.caffeine
Observed settings.bodyClock.saved.light
Observed settings.bodyClock.saved.health
All five native sections saved; no ChronoFlow StrictMode frames for pid 27908
```

## Board capture handoff

`make captures ONLY=SetPreferences,SetPreferencesNL,SetBodyClock,DarkSetBodyClock` ran once and wrote all 40 expected PNGs into `build/captures/`. Names/dimensions are recorded in `build/phase-10b/task-11/capture-manifest.json`. Preferences and the complete missing-answer About card were inspected directly. No Chromium render or side-by-side host comparison was attempted; visual board acceptance remains pending that comparison.

| Board / capture stems | Recorded differences from the raw board |
|---|---|
| `SetPreferences`, `SetPreferencesNL` (both appearances) | Native M3 large title and system/navigation chrome; dawn switch tracks through `CfSwitch`; no reminders subtitle until 12c. Native bars occupy layout space, so lower links may require scrolling. Dutch uses catalog “Slapen”, rather than board “Slaap”. |
| `SetBodyClock_*-intro`, `DarkSetBodyClock_*-intro` | Native title/chrome. Readiness uses the server's two required answers and invalid-section count. Excluded replaces the board's medical “blocked” wording; ready/adjusted use the pinned iOS copy. |
| `SetBodyClock_*-about`, `DarkSetBodyClock_*-about` | Age bands are under 18 / 18–64 / 65+, with missing badges only for genuinely missing answers. Chronotype retains the empty “Not sure” id. |
| `SetBodyClock_*-sleep`, `DarkSetBodyClock_*-sleep` | iOS free-day choices and 128 dp adaptive time tiles. The separate native time sheet uses a dial and five-minute steps; the static section PNG does not show that window. |
| `SetBodyClock_*-caffeine`, `DarkSetBodyClock_*-caffeine` | Boolean sensitivity choices; medical questions removed; neutral “Leave caffeine out” switch. Drink controls retain 48 dp targets and wrap their label only when required at minimum width. |
| `SetBodyClock_*-light`, `DarkSetBodyClock_*-light` | One unified light-device question with its eye-caution caption, rather than separate light-box/visor questions. Rated blockers use the mDFD field and catalog hint. |
| `SetBodyClock_*-health`, `DarkSetBodyClock_*-health` | “Safety checks”: shift work, neutral flight-movement preference and the two notices. Medical questions and the board's required-health caption are removed. |

Light Body clock captures cover `bodyMissing`, `bodyReady`, `bodyAdjusted` and `bodyExcluded`, six sections each. Dark captures cover `bodyMissing` and `bodyReady`, six sections each. Non-intro section captures isolate the complete production card with page padding/background rather than repeating the app bar and shell in every PNG. Saved feedback belongs to every successfully saved section; untouched static seed captures do not manufacture it. There are no claimed unreviewed pixel matches.

## Performance and remaining host work

The theme root changes the launch path. Per the user's instruction, no emulator performance/profile command, `make check`, T2 AVD or other emulator tier ran. Run these on the host and record the startup numbers in `docs/perf-startup.md`:

```sh
make baseline-profile
make perf
```

Record cold/warm startup, first-frame/TTFD, frame CPU/jank and RSS from the normal startup benchmark, and commit the regenerated baseline/startup profiles. No timeline, trip-page or plan source changed, so `make perf-timeline` is not required by R10. The serial JVM budget measurements are recorded separately and are not device claims.

## Commits

All implementation commits are GPG-signed with no trailers. No push, rebase or merge was performed. Task 7's amendment occurred before Task 8 started; subsequent commits were not rewritten.

| SHA | Subject |
|---|---|
| `9e7fa3c8` | `docs(phases): refresh the 10b brief against 10a` |
| `c1a52de3` | `feat(settings): port the body clock model` |
| `45962581` | `feat(settings): add body clock readiness and choice rules` |
| `3b0c3e4c` | `feat(settings): port the preferences model` |
| `95b2a690` | `test(data): pin pending protection for settings sections` |
| `6a936140` | `feat(settings): apply the theme per device` |
| `254ea329` | `feat(settings): add body clock and preference components` |
| `bb12b8b8` | `feat(settings): build preferences` |
| `5947f277` | `feat(settings): build body clock` |
| `3e1c558a` | `test(debug): seed body clock board states` |
| `2905ba42` | `test(settings): add guards and board captures` |
| `885e31df` | `test(settings): check the theme on the emulator` |

Task 13 commits this acceptance record as `docs(settings): record phase 10b results and open questions`.
