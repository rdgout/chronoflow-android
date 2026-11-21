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

The original `make captures ONLY=SetPreferences,SetPreferencesNL,SetBodyClock,DarkSetBodyClock` run wrote all 40 expected PNGs into `build/captures/`. Names/dimensions are recorded in `build/phase-10b/task-11/capture-manifest.json`. Review round 1 below compares the supplied host board renders with the regenerated captures and records the retained differences by board name. The worker did not launch Chromium.

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

## Review round 1

The host reported `make check` passing in 3:48 on the implementation above. This round addresses all eight review findings without changing the timeline, plan engine, shared switch wrapper, copy or dependencies. Phase 10b now also lists the completed Task 8 and Task 9 rows explicitly. No OQ or brief patch was needed: the review supplies the decisions.

The readiness action fills the production card's content width. Ready and adjusted check discs are vertically centered on their title/subtitle block. Six light/dark geometry cases measure the production component; removing the width and alignment fixes makes all six fail, and restoration passes.

The existing 9f `RootThemeTest` now composes `CfRoot` with a stored Dark choice on API 29 and 30 while the system is Light. Reverting the root to `isSystemInDarkTheme()` fails both pixel checks. The configuration cases dispatch a real `ActivityController.configurationChange` through `MainActivity`, retain the activity, and observe the root's existing system-bar callback calling the actual activity setter. After the configuration and next frame they require another Dark application, even though the resolved choice never changed. Replacing `SideEffect` with `LaunchedEffect(night)` fails both cases. Window flags alone had been insufficient evidence; the callback recorder proves the reapplication.

Overlay observation and the launch-finish effect now live inside the theme content lambda. A tracing guard observes the overlay disappearing and zero further `CfRoot` compositions. Its paused-clock fixture explicitly publishes snapshot notifications; this prevents a false zero while the UI is still waiting to update. Restoring the original broad reads fails with `expected:<0> but was:<1>`. All graph/launch dispatchers in each new fixture share one test scheduler, and their scopes are joined during cleanup. No allow-list or main-source test hook was added.

First-missing navigation reads layout coordinates for the question within its section and scrolls to that item plus offset. Coordinates stay in the screen's layout tracker, outside ViewModel state; every placement refreshes them. Section-save navigation still targets the heading. The small-phone Health/`shiftWork` case checks the question's unclipped position against the padded viewport top and its request acknowledgement. Removing the offset scroll makes it fail. Existing Body clock structure and section recomposition cases remain green.

Theme read failures now report a static diagnostic and fall back to System, without setting the save-failure flag. A gated JVM flow first emits Dark, then fails; the test observes System, one diagnostic, no banner failure and no outbox row. Theme write failures continue to show the save error. The former `replayTwiceStartsFreshAfterSkipOrDone` test is renamed `preferencesSaveQueuesOnlySettingsAndReplayTargetsTheReplayRoute`: it checks the settings row and replay destination. It makes no claim about navigation-owner disposal or a second entry's ViewModel store.

### Board comparison

Compared the supplied `build/board-renders/{SetPreferences,SetPreferencesNL,SetBodyClock}.png` with the regenerated corresponding PNGs in `build/captures/`. The host PNGs are 1170 × 2532; their templates use a 375 CSS-pixel board, while the app captures use the pixel8 profile at 411 dp. Comparisons account for that width and the native chrome. The supplied Body clock image covers the initial viewport; its later sections were also checked against the board template, the pinned iOS source and the complete section captures. No browser renderer was launched by the worker.

| Board / section | Comparison and disposition |
|---|---|
| `SetPreferences` | Native M3 back control, system large title, safe insets and bottom navigation replace the board's glass labelled back pill and Fraunces page title (A9). This shifts the first card and the visible fold. The app has the category swatches required by iOS `SettingsPreferencesView.swift:104`; the board rows show text alone. Swatches and their label inset are retained as iOS parity. Reminders and replay remain below the initial app viewport and are reachable by scrolling. |
| `SetPreferences` time format | The board wraps “12-hour” below “24-hour”. Both implementations use intrinsic wrapping, not a forced line break. Production geometry tests prove wrapping at the board's 375 dp width in English and Dutch. At the wider 411 dp pixel8 width, the two English pills fit beside one another. This responsive difference is retained; no fixed board-width cap was introduced. |
| `SetPreferencesNL` | Same native chrome and swatch differences. Dutch time-format pills wrap at both 375 and 411 dp, matching the supplied board's two rows. Copy uses catalog “Slapen” instead of board “Slaap” (OQ-A498). The larger native chrome and wrapped copy put Travel and the links farther below the fold; they remain scrollable. |
| `SetPreferences`, `SetPreferencesNL` switches | The board's `ios-switch` CSS paints white thumbs and a borderless translucent off track. Android retains the Material 3 switch through `CfSwitch` (A9 / OQ-A489): dawn checked track, `dawnForeground` checked thumb, and `input`/`mutedForeground` off track, outline and thumb. Those dark thumbs and outlined off tracks are an explicit retained native-control difference. The shared wrapper and earlier-phase controls were not changed. |
| `SetPreferences`, `SetPreferencesNL` link rows | The reminders subtitle is empty until 12c; the board includes its example status. Replay has its catalog subtitle and no trailing chevron, matching iOS. Native navigation occupies layout space, so these rows are not necessarily visible in the initial capture. |
| `SetBodyClock` intro / needs answers | The full-width readiness pill is fixed and matches the board's width rule. The app's progress track, checkmark and “0/2 done” metadata are iOS parity (`SettingsBodyClockView.swift:425–436`) and remain despite their absence from the board. The app asks for two required answers, replacing the board's legacy count of three. Native page chrome and 411 dp wrapping shift the card and later sections. |
| `SetBodyClock` About | The supplied host render leaves the chronotype option area blank; the template and iOS define four cards. The app paints those cards, their hints and selection controls, which makes About taller. Age bands remain Under 18 / 18–64 / 65+, replacing the board's four legacy bands. Missing-answer decoration appears on age and shift work only when missing. These are pinned iOS / no-health-data rules. |
| `SetBodyClock` ready / adjusted / excluded captures | The provided host PNG shows needs answers. Ready and adjusted discs are now centered on their copy, as the pinned iOS HStack specifies; both regenerated variants were inspected and have geometry guards. Their copy, adjustment lines and excluded state follow server readiness and the existing §4 overrides, not the board's legacy complete/blocked wording. |
| `SetBodyClock` Sleep | Native captures use the seed's 23:00 / 07:00 and same-as-workdays choice. Optional free-day tiles appear only when declared. The time sheet remains a native M3 dial with five-minute rounding; it is a separate window outside these static section captures. Adaptive tile width, not a fixed phone layout, controls stacking. |
| `SetBodyClock` Caffeine | Retains boolean sensitivity, the neutral exclusion preference and catalog drink labels; the removed medical questions and third sensitivity answer are absent. Native switches have the same deliberate Material appearance described above. Static seeds show no Saved marker until a local edit succeeds; the board's Caffeine Saved example is not manufactured. |
| `SetBodyClock` Light / Safety | One unified light-device question and its caution replace separate light-box/visor questions. Safety contains shift work, the neutral flight-movement preference and the two notices, replacing the medical questionnaire and its required-health caption. Switch appearance remains native M3. The notices are information, not collected health answers. |
| `SetBodyClock` section PNGs / `DarkSetBodyClock` | Non-intro PNGs isolate the complete production card with its page padding and phone-height background; they do not repeat the app bar or shell. Dark captures received the same width/centering fixes and pass light/dark geometry guards. No dark host render was supplied, so no side-by-side dark match is claimed. |

The English and Dutch pill-width cases use the real Preferences screen and inspect tags and geometry only. No copy is asserted on a screen. The regenerated missing-answer intro visibly has the full-width action, and the ready/adjusted intros visibly have centered discs.

### Review verification

Targeted checks used `make targeted ONLY="…"`, with the existing in-process Kotlin compiler setting and the writable Robolectric Maven cache from the initial record. Times are tier wall times unless labelled otherwise. All deliberate mutations were restored before commits.

| Check | Result | Wall time |
|---|---|---:|
| `feature:settings:SettingsReadinessLayoutTest` | 6 passed; original width/alignment mutation fails all 6; restored cases pass | 8.24 s initial / 4.40 s restored |
| `app:RootThemeTest app:CfRootTest app:CfRootHandOffTest app:RootThemeChoiceTest` | 21 passed; legacy stored-theme, real configuration reapplication and overlay scope controls proven by mutations | 8.56 s final |
| `feature:settings:BodyClockQuestionAnchorTest feature:settings:BodyClockScreenTest feature:settings:BodyClockRecompositionTest` | 12 passed; Health anchor mutation fails and restoration passes | 6.92 s; restored anchor 4.19 s |
| Anchor after layout-callback API cleanup | passed | 4.93 s |
| `feature:settings:PreferencesViewModelTest` | 10 passed, including gated read failure and unchanged write-error handling | 3.12 s |
| Renamed replay model method | passed | 2.73 s process |
| `feature:settings:PreferencesPillWrapTest` | 4 profile/language cases passed | 5.44 s |
| `make captures ONLY=SetPreferences,SetBodyClock,DarkSetBodyClock` (once) | 40 passed / 40 PNGs | 19.67 s |
| App/settings layout tier (once) | 480 passed, 1 existing LocaleManager-shadow ignore; no overruns | 26.63 s |
| `./gradlew :app:lintDebug :feature:settings:lintDebug` (once) | passed | 17.19 s process |
| Final `make format` | passed | 10 s build |
| Single review `make quick`, main stage | 5,480 assertions passed; one untouched Terms first-case timing overrun; no missing evidence or hangs | 38.82 s |
| Remaining build-logic stage | 37 passed, certified cached results | 1.72 s |
| Remaining tooling stage | 171 harness + 25 contracts + 49 strings + 11 vectors passed | 20.17 s process |
| Final serial `make timing` stage | 7 passed, certified cached results for unchanged budget inputs | 1.61 s |

Cold first cases remain separate from the warm budgets: readiness 1.904 s, root group 2.181 s, anchor 2.055 s and pill wrapping 2.135 s. The first API-29 configuration case took 1.113 s under that SDK's cold allowance; its other legacy theme case took 46 ms. No budget was loosened. Result evidence is retained in `build/phase-10b-r1/` and `build/test-results/layout-10b-r1/`; logs use `/private/tmp/chronoflow-10b-r1-*.log`.

In concurrent quick execution, the first API-29 root case took 1.540 s and the first SDK-36 settings anchor case took 6.155 s; both used the existing 20 s SDK cold allowance. The body-clock section recomposition case took 204 ms. All 13 timeline invalidation and 124 list invalidation cases passed unchanged.

Quick ran once and stopped at its timing gate. The unchanged Terms case exceeded its class's first-case allowance by 46 ms; per the user's instruction it was not retried, edited or given more time. The stages after that early exit were completed separately, with `IOS_REPO` and `WEB_REPO` supplied to the tooling script as Make normally does. The serial timing task reused certified output, so no fresh timeline budget measurements are claimed. Exact gate output:

```text
run-tier: over budget: robolectric net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed 3046 ms > 3000 ms
make: *** [quick] Error 3
```

This review does not run emulator tiers, `make check`, `make baseline-profile`, `make perf` or `make perf-timeline`. The host will regenerate profiles and measure startup after this round. The pixel emulator is left running. No command remains blocked by permissions or signing.

Review commits (the following documentation commit records their acceptance):

| SHA | Subject |
|---|---|
| `d2cd44ad` | `fix(settings): match readiness card geometry` |
| `d508037f` | `docs(settings): list the completed preferences and body clock tasks` |
| `6080248b` | `fix(app): scope root appearance effects and prove legacy theme wiring` |
| `0f0d8aa4` | `fix(settings): anchor the first missing question at the top` |
| `d9cdd107` | `fix(settings): keep theme read failures off the save error line` |
| `90cf9b59` | `test(settings): describe the replay model assertions accurately` |
| `48abcc30` | `test(settings): pin time format wrapping at board and phone widths` |
| `5a01cc92` | `refactor(settings): register question positions from layout callbacks` |
| `1d3039fe` | `style(settings): format the review changes` |
