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
