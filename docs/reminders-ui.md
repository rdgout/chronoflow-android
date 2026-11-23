# Phase 12c: reminders UI evidence

Final validation note: the single requested `make quick` invocation failed in a new Robolectric seed fixture (SQLite JDBC class-loader registration). The fixture now uses the production Android database driver; all five focused cases pass. `make quick` was not rerun, as instructed. The remaining build-logic, tooling and serial timing steps passed separately. A full commit-gate run remains with the orchestrator.

The page and first-trip content use the generated English and Dutch resources. No copy was added. The first-trip row is consumed transactionally before presentation; the JVM reopen/concurrent-consume test covers process death and duplicate save completion. The host test uses the production coordinator with fake dependencies and a real retained activity owner, without booting the application graph.

## Board captures (2026-11-22)

Command, run once:

```sh
make captures ONLY=SetReminders,SetRemindersAsk,SetRemindersBlocked,DarkSetReminders,WizardReminders,WizardRemindersDarkNL
```

Result: `BUILD SUCCESSFUL in 30s`; `captures: 7 PNGs in build/captures`. Profile: pixel8, Robolectric SDK 36, debug, native graphics. These are JVM renders, not device screenshots. All seven images were visually inspected against the boards' layout, type, colors and state specifications.

Capture review found a left-aligned drag handle in the capture fixture. The production M3 sheet already centers it. The fixture now centers the native handle; only the two sheet images were refreshed with:

```sh
./gradlew --console=plain :feature:reminders:testDebugUnitTest -Pcf.tier=capture -Pcf.captureOnly=WizardReminders --tests '*.RemindersCaptureTest'
```

Result: `BUILD SUCCESSFUL in 7s`.

| Board / capture | Differences from the raw board |
|---|---|
| `SetReminders` / `SetReminders_on_pixel8_en_light` | Native large app bar and icon back button replace the Fraunces page title and labelled iOS back button. Native dawn-tinted switch and joined M3 segmented buttons replace the drawn switch and separate pills. Live Activity and practice cards are omitted (A24). Window and safe insets determine the geometry. |
| `SetReminders` unsupported / `SetReminders_unsupported_pixel8_en_light` | Same native app bar, window geometry and omitted cards; only the header and unsupported notice remain, as specified for this status. |
| `SetRemindersAsk_pixel8_en_light` | Same native app bar, window geometry and omitted cards. The request stays behind the designed 48 dp action. |
| `SetRemindersBlocked_pixel8_en_light` | Same native app bar, window geometry and omitted cards. Four iOS instruction rows become two Android rows using the existing blocked-step copy. The disabled native switch uses system disabled colors rather than a CSS opacity. |
| `DarkSetReminders_pixel8_en_dark` | Same native app bar, omitted cards and native controls as the on state; native selected fill/shared borders replace the outlined board pills. All dark colors come from the generated theme. |
| `WizardReminders_pixel8_en_light` | Content capture omits the underlying trip and scrim. The native drag handle and its system hit area replace the board's 36 × 5 grabber/padding. Body is the nearest generated 16/24 style rather than 16/23; title is the board's 24/30. Android safe insets replace the 34 px iPhone home-indicator padding. Production uses the M3 modal window and scrim. |
| `WizardRemindersDarkNL_pixel8_nl_dark` | Same sheet differences. Catalog “Nu niet” replaces board “Niet nu”. Dark colors and the 16% bell tint match the generated theme/board values. |

The local board files reference a `support.js` runtime that is absent from this checkout; comparisons use their committed HTML/CSS specifications and the generated Android images.

## Layout and regression evidence

- `RemindersGuardsTest` and `FirstTripRemindersSheetGuardsTest`: small, pixel8, foldInner, landscapeSideNav and largeFont, plus minimum280 and small at font scale 1.3; English/Dutch and light/dark, one parameterized case per combination. Blocked and active page states and sheet content are checked.
- M3 deliberately overlaps adjacent segment borders by 1 dp. Text fit, target size and safe-area checks remain active for every segment. Explicit containment/shared-border checks replace the generic non-overlap assertion for that native group; other controls keep the generic assertion.
- Initial layout run: 84 cases, 28 active-state failures from the generic shared-border assertion. Focused corrected active-state run: passed, `BUILD SUCCESSFUL in 9s`. The blocked and sheet cases passed in the initial run.
- Negative control: temporarily restoring the old selected-index assertion makes `CfSegmentedButtonsTest.minusOneAllowsNoSelectedOptionAndATapSelectsAValidOne` fail with `IllegalArgumentException` (`BUILD FAILED in 5s`). Restoring the implementation makes the same method pass (`BUILD SUCCESSFUL in 4s`). No mutation remains.

## Size baseline

`make size`: `Bundle size: 4131623 bytes (4.13 MB), arm64-v8a/xxhdpi/en: OK`; `BUILD SUCCESSFUL in 1m 1s`. No new native library was introduced.

## Native permission and sheet evidence

```sh
make targeted ONLY='app:RemindersPermissionDeviceTest'
```

First run on `pixel` (`emulator-5554`), debug: **PASS**, `OK (1 test)`, runner **3.241 s**, complete tier **13.430 s** including APK install. The real system dialog is absent before Ask and appears only after tapping it. Allow turns the switch on, shows the lead control and persists `notificationPermissionRequested`. The debug ChronoFlow I/O violation counter is **0**. Results: `build/test-results/targeted-permission/timings.json` and `build/reminders-permission/result.xml`.

The main smoke command excludes this class and keeps the APKs installed. Its following locked tier verifies installation, revokes permission/clears user-set flags outside instrumentation, and runs this class alone. The isolated runner checks actual JUnit success and the time metric, emits XML, and propagates failures even if `adb` itself exits 0. The same runner backs the targeted command. Its parser, missing-APK, lock, sequencing and targeted-selection tests pass (20 tooling cases total).

The native first-trip sheet was captured over the trip page, with its native handle, scrim and navigation inset: `build/captures-device/WizardReminders_device.png`. After force-stop and the specified seeded relaunch without `cf.firstTripReminders`, `trip.page` is present and `reminders.firstTrip` is absent; image: `build/captures-device/WizardReminders_after_relaunch_device.png`. Database persistence across a real close/reopen is proved separately by the JVM test, without reseeding.

Commands (each device operation uses the device lock):

```sh
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 shell am start -W \
  -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 01900000-0000-7000-8000-0000000012c0 --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
  --es cf.notificationAuthorization undetermined --es cf.firstTripReminders 1 \
  --es CF_PLAN_TEST_ROUTE trip:0199a3c0-0000-7000-8000-000000000001 \
  --es cf.settings.language en --es cf.settings.theme light --es cf.reduceMotion 1
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 exec-out screencap -p > build/captures-device/WizardReminders_device.png
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 shell am force-stop net.chronoflow.app.debug
# Repeat the seeded am start above, omitting --es cf.firstTripReminders 1.
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 exec-out screencap -p > build/captures-device/WizardReminders_after_relaunch_device.png
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 logcat -d -s StrictMode
```

Both seeded launches returned `Status: ok`, `LaunchState: COLD`; the StrictMode command produced no output. No other emulator test tier, performance tier or profile generation ran. `pixel` remains running.

## Final verification

Commands below use the repository's JDK/SDK configuration. Gradle times include configuration/compilation; individual test budgets come from the tier timing reports. Complete command logs are in `build/phase-12c/logs/`.

| Check | Result / wall time |
|---|---|
| Task 1 targeted scaffold/component classes and `KonsistRulesTest` | Passed; Gradle 31 s + 7 s. |
| `make targeted ONLY='core:data:FirstTripRemindersOfferTests core:data:ReminderPermissionMappingTest'` | Passed; 13 s. The mapping was unchanged. |
| `make targeted ONLY='feature:reminders:RemindersModelTests feature:reminders:RemindersViewModelTest'` | Passed; 3 s after the test fake's return type was corrected. The final model-only follow-up also passed in 3 s. |
| Task 4 `RemindersContentTest`, `CfSegmentedButtonsTest`, `NotificationSettingsIntentsTest`, `RemindersSeedTest`, `ReminderPermissionTests`, `PermissionRequestStartTest`, `ShellNavSettingsSeedTest` | Passed; successful module runs 19 s / 8 s / 24 s. Focused structural/SDK follow-ups passed. |
| `make targeted ONLY='feature:settings:RemindersSubtitleTest feature:settings:PreferencesRemindersRowTest feature:settings:PreferencesScreenTest feature:settings:PreferencesViewModelTest'` | Passed; 18 s. App compile/seed-route follow-up passed in 6 s. |
| `make targeted ONLY='feature:reminders:FirstTripRemindersOfferViewModelTest feature:reminders:FirstTripRemindersSheetTest app:FirstTripRemindersHostTest'` | Passed; 6 s + 10 s. Final focused host/sheet checks passed in 11 s / 6 s. |
| Cancellation distinction (`callerCancellationNeverSetsFailureAndResetsBusy`, `foreignCancellationKeepsTheOfferOpenAndResetsBusy`, `cancellationNeverSetsFailure`) | Passed; 4 s. Caller/stale cancellation stays silent; active dependency cancellation keeps the offer open as a failure. |
| Layout tier for `:app`, `:feature:reminders`, `:feature:settings`, `:core:ui`, `:core:designsystem`, `:core:data` | Passed with scoped evidence globs; 11.460 s, 169 newly measured cases, 0 budget overruns. Across current/certified module XML: 1,689 passed, one pre-existing locale case skipped. `:core:data` has no layout cases. |
| `:app:lintDebug`, both touched feature `lintDebug` tasks, `:core:ui:lintDebug`, `:core:designsystem:lintDebug`, `:core:data:lintDebug` | Passed. Initial InlinedApi findings required explicit API 33 guards at the launcher and native permission read. Focused fixes passed (5 s / 11 s); final app/reminders Lint passed in 20 s after the cancellation change. |
| `make targeted ONLY='app:ReleaseReadinessTest'` | Passed; 14 cases, 0.079 s test time / 11 s Gradle. The debug fake is absent from release. |
| `:app:verifyReleaseManifest` | Passed/up-to-date; no new permission or manifest component. |
| `make quick` — exactly one invocation | Failed in 45.800 s: one new seed-row fixture case, `No suitable driver found for jdbc:sqlite:…`. All 602 other app cases passed; overall 4,378 cases newly measured, 0 timing overruns and no hangs. No second quick invocation. |
| `make targeted ONLY='app:RemindersSeedTest#firstTripSeedQueuesThePendingRow'` after the fixture fix | Passed; all five parameters, 6.480 s tier / 6 s Gradle. Cold SDK case 1,545 ms (20 s allowance); remaining cases 15–23 ms. |
| Remaining quick step: `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | Passed; 37 cases, 3.010 s tier / 2 s Gradle. |
| Remaining quick step: `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new Scripts/test_scripts.sh` | Passed; 269 cases: scripts 183 (18.175 s), contracts 26 (0.113 s), strings 49 (0.016 s), icons 11 (0.086 s). A direct invocation without Makefile's exported source paths initially failed the parity precondition; supplying those paths fixed the invocation. |
| Remaining quick step: `make timing` | Passed; seven certified cached budget cases, 3.140 s tier / 2 s Gradle. |
| `make size` after | Passed; 4,147,969 bytes (4.15 MB), arm64-v8a/xxhdpi/en; 61 s. Increase: 16,346 bytes (0.40%). No new `.so`. |

The first layout command used the Makefile's repository-wide result globs with only the six touched test tasks. Gradle passed in 70 s with 0 timing overruns, but the collector rejected stale stamps from three untouched modules. Restricting the result/timing globs to the six selected modules fixed collection. The subsequent run also covered the final cancellation change. Its exact command is preserved in `build/phase-12c/logs/phase-12c-final-layout-scoped.log`.

Regular Phase 12c JVM/Robolectric cases in the quick run peaked at 224 ms. The first SDK 36 Compose case in `:feature:reminders` took 6,844 ms and was recorded as a cold SDK warm-up against the existing 20 s allowance. Native font warm-up remains recorded through `measureClassWarmup`; no budget was loosened.

The original quick failure XML/log are retained in `build/phase-12c/quick-first-failure/`. The fix changes the Robolectric fixture only; pure JVM persistence tests keep JDBC. Because quick was restricted to one invocation, the full aggregate quick/check gate has not been rerun after this fix. `make check`, `make perf`, `make perf-timeline`, `make baseline-profile` and all other emulator tiers were deliberately not run. This phase adds no performance claim.

## Brief and plan bookkeeping

- One signed preflight refresh corrects the single-file scaffold move and its four same-package test imports, and records the binding fake-host, acknowledged-event and single-quick instructions (R18).
- Task commits record the iOS two-reschedule behaviour/public pull adapter (R19), direct seed/request test boundaries and single-writer seeding callback (R20), native shared-border guards (R21), isolated device-run evidence (R22), the binding cancellation distinction (R23), and native database fixture (R24).
- Plan adds OQ-12c-1 through OQ-12c-15 and OQ-A588 through OQ-A590 with their supplied defaults. No further OQ number was needed; OQ-A68 remains unused.
- All nine task-commit boxes and the four original phase boxes are checked. The three debug seed keys and runnable examples are documented in the plan and tooling guide. No Android-only copy was added.
- All commits were created with `git commit -S`; their commit objects contain GPG signature blocks. No amend, rebase, merge, push or force-push ran.

## Environment limitations

All requested build, test, ADB and capture commands could run. Kotlin occasionally emitted a sandbox warning such as:

```text
e: java.nio.file.FileSystemException: /Users/rickgout/Library/Application Support/kotlin/daemon/kotlin-daemon-client-tsmarker….tmp: Operation not permitted
```

Compilation and the associated checks still completed. No Gradle invocation stalled for three minutes, so no daemon stop/retry was needed.

Optional cryptographic status inspection stalled without output and was interrupted (exit 130):

```sh
git log --reverse --format='%h %G? %s' 8b720f5f^..HEAD
```

Signing itself succeeded for every commit; signature blocks were inspected directly with `git cat-file commit`. Process inspection while diagnosing the read-only status command was denied:

```sh
ps -eo pid,ppid,etime,command | rg 'git log --reverse|gpg.*verify|gpgsm|dirmngr'
```

```text
zsh:1: operation not permitted: ps
```

Interactive board rendering was unavailable because the referenced local runtime is absent:

```sh
head -n 25 _design/canvas/support.js
```

```text
head: _design/canvas/support.js: No such file or directory
```

The committed board specifications, seven JVM images and native sheet image were inspected instead. The `pixel` emulator remains running.
