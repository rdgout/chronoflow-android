# Phase 12c: reminders UI evidence

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
  -n net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 01900000-0000-7000-8000-0000000012c0 --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
  --es cf.notificationAuthorization undetermined --es cf.firstTripReminders 1 \
  --es CF_PLAN_TEST_ROUTE trip:0199a3c0-0000-7000-8000-000000000001 \
  --es cf.settings.language en --es cf.settings.theme light --es cf.reduceMotion 1
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 exec-out screencap -p > build/captures-device/WizardReminders_device.png
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 shell am force-stop net.chronoflow.app.native.debug
# Repeat the seeded am start above, omitting --es cf.firstTripReminders 1.
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 exec-out screencap -p > build/captures-device/WizardReminders_after_relaunch_device.png
python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 logcat -d -s StrictMode
```

Both seeded launches returned `Status: ok`, `LaunchState: COLD`; the StrictMode command produced no output. No other emulator test tier, performance tier or profile generation ran. `pixel` remains running.
