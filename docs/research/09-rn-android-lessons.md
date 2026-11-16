# 09 — What the React Native app teaches about Android

Researched 2026-11-15 for the native Android (Kotlin + Jetpack Compose) ChronoFlow app.
Source: `/Users/rickgout/Sites/chronoflow-mobile` (read-only; nothing was built, prebuilt or git-modified there).

Conventions in this document:

- **Fact** = verified in source; a path (and line numbers where useful) is given. `RN` = `/Users/rickgout/Sites/chronoflow-mobile`, `iOS` = `/Users/rickgout/Sites/chronoflow-ios`, `W` = `/Users/rickgout/Sites/chronoflow-new`.
- **Rec** = my recommendation for the Android app.
- **OQ-A9.n** = open question, with the default the plan should proceed on.

---

## 0. Status of the RN app (context for everything below)

- **Fact.** Expo SDK 58, React Native 0.88.1, React 19.3, New Architecture, Hermes, React Compiler on (`RN package.json`, `RN app.config.ts:112-115`). Checked-out branch `feat/plan-compact`, HEAD `e24872a` (2026-11-09), 517 commits. Last phase is 25 (compact trip page).
- **Fact.** Android was a second-class target. It was *never* checked on a phone. The only Android runtime check recorded is the Phase 21 splash on the emulator ("The emulator starts too slowly to judge smoothness", `RN plan.md:1162`). Every gate item that needs Android is still unticked: the Maestro run on Android (`plan.md:442`), TalkBack (`docs/plan-gate.md:1340`), font scale 1.3 (`:1342`), and the mid-range Android performance check (`plan.md:449-451`; `docs/plan-gate.md:1353-1365`: "Status: pending … no Android SDK or device was available. Results: not yet measured.").
- **Fact.** A release APK exists from 5 Nov: `RN android/app/build/outputs/apk/release/app-release.apk`, **125,310,093 bytes** (all four ABIs, not split, signed with the debug keystore).
- **Fact.** iOS decided early that RN keeps Android and stays the shipping app until native reaches parity (`iOS plan.md` OQ 19, "Yes"). The new Android app replaces that.

**What this means.** RN is useful for its *data layer, sync rules, timeline geometry, reminders algorithm and copy rules*, which are platform-neutral and were reviewed many times. It is *not* evidence that anything works or performs well on Android. Treat every RN Android behaviour as unverified.

---

## 1. Top lessons (short list)

1. **The timeline must be one drawn surface per day, not a view per block.** RN tried seven React Native optimisations and kept none. The winner was a native iOS view that draws the whole day body (Core Graphics layers) from one JSON payload. **Android never got a native body**: it still renders the slow RN path (`RN modules/timeline-canvas/README.md`: "Android uses the React Native body"). See §4.
2. **The data and sync design carries over unchanged.** SQLite with one file per account, the plan stored as one JSON value per `(trip_id, input_hash)`, an outbox committed in the same transaction as the local write, list-driven `keepOffline` prefetch, and re-keying check-offs after a re-plan. iOS ported the same design (GRDB). Room maps onto it 1:1. See §5 and §6.
3. **Android needs explicit insets.** RN found that Android never adds the header height or the bottom navigation inset to a scroll view (`RN src/navigation/useHeaderInset.ts:6-16`), and hard-coded an 80 dp tab-bar inset (`src/navigation/native-tab-bar.ts:3`). In Compose, use `Scaffold` padding and `WindowInsets` and never hard-code bar heights.
4. **Android 13 notification permission state is misreported by Expo.** RN works around `denied` being reported before the first prompt (`RN src/settings/useNotificationPermission.ts:27-34`). Use `shouldShowRequestPermissionRationale` plus a "requested once" flag natively. See §3.
5. **Hermes `Intl` cannot be trusted** (unit formatting broken before Android 7.1 and unreliable on 11; 12-hour two-digit hours and Dutch day periods ignored). RN builds minutes, hours and 12-hour times from locale strings (`RN plan.md:172,175`). Native `java.time` + ICU (`android.icu.text`, API 24+) removes this class of bug, but **keep RN's golden en/nl/DST cases** (`RN src/plan/intl-cases.ts`) as unit tests.
6. **Optional steps are never shown** (decision 32, `RN plan.md:1458`), including in reminders and calendar export. The rule is `isOptionalStep` (`RN src/plan/optional-items.ts:3-18`).
7. **Do not copy RN's wizard health questions.** The server's Phase 19 "No health data" (10 Nov) removed the bipolar, pregnancy, VTE ("clot") and eye questions (`W docs/ui-plan/phase-19-no-health-data.md` P19-D1). RN still has those routes (`RN src/app/(app)/trip/wizard/question-{bipolar,pregnancy,clot,eye}.tsx`). iOS follows the server (`iOS plan.md:2477-2527`). **Follow iOS.**
8. **RN's API mode refuses several flows the server now supports.** Reset, verify, passkeys (`notOnTheApiYet`, `RN src/data/api/ApiDataSource.ts:46,200-208,386-399`), but `W routes/api.php:48-93` (HEAD `1f4a2684`, 2026-11-12) now has register, forgot/reset password, email verification and passkey routes. **Use iOS and the current web routes as the API reference, not RN's `ApiDataSource`.**
9. **Several RN features have no API endpoint**, so they are mock-only: delete trip, complete trip, duplicate as a server call, data export, billing hand-off, airport search via the old method (`ApiDataSource.ts:171-173,351-364`). iOS does not build them (OQ 31, 41, 42). See §8.
10. **Splash on Android 12+ is the system splash icon.** The moon must fit the 192 dp mask inside a 288 dp icon (`RN plan.md:1164`). RN's light background is `#1f285a`, dark `#06091c` (`RN android/app/src/main/res/values{,-night}/colors.xml`).
11. **Release hygiene RN never did:** ABI splits / AAB, real signing, R8 rules, no Firebase. RN's APK pulls in Firebase Messaging through `expo-notifications` even though push is unused (merged manifest, §2.2).
12. **Predictive back was switched off in RN** (`predictiveBackGestureEnabled: false`, `RN app.config.ts:66`; `android:enableOnBackInvokedCallback="false"` in the manifest). With targetSdk 36, a native app should support it. See §2.4.

---

## 2. Android build and manifest facts

### 2.1 Gradle and SDK levels (Fact)

| Item | Value | Source |
|---|---|---|
| applicationId / namespace | `net.chronoflow.app` | `RN android/app/build.gradle`, `RN app.config.ts:57-58` |
| minSdk | **24** | merged manifest `RN android/app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml` (`<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>`) |
| targetSdk / compileSdk | **36** | same |
| Gradle wrapper | 9.4.1 | `RN android/gradle/wrapper/gradle-wrapper.properties` |
| AGP opt-outs | `android.builtInKotlin=false`, `android.newDsl=false` ("Opt out of built-in kotlin and new DSL behavior that ships with AGP 9") | `RN android/gradle.properties` |
| R8 | `android.enableMinifyInReleaseBuilds=true`; shrinkResources default false | `RN android/gradle.properties`, `app/build.gradle` |
| ABIs | `armeabi-v7a,arm64-v8a,x86,x86_64`, universal APK | `RN android/gradle.properties` |
| Edge-to-edge | `edgeToEdgeEnabled=true` | `RN android/gradle.properties` |
| Release signing | debug keystore (`signingConfig signingConfigs.debug`) | `RN android/app/build.gradle` |
| Baseline profiles | present in output (API 28–30, 31+) — from libraries, not app-authored | `RN android/app/build/outputs/apk/release/output-metadata.json` |
| Version | versionCode 1, versionName 1.0.0 | `RN android/app/build.gradle` |

`android/` is generated by `npx expo prebuild` and gitignored (`RN CLAUDE.md` "Builds are local only"). The Kotlin in it is template code only: `MainActivity.kt` (splash registration plus the Android S back behaviour) and `MainApplication.kt`. **RN has no hand-written Kotlin.** The only native module, `modules/timeline-canvas`, is iOS-only (`expo-module.config.json`: `{"platforms": ["apple"]}`).

**Rec.** minSdk: see OQ-A9.1. targetSdk/compileSdk 36 (installed platform `android-36`; `android-37.0` is also installed, compile against 36 until 37 is stable in AGP). Ship an AAB (Play splits ABIs). Add an app-specific Baseline Profile and Startup Profile covering cold start → Today and opening a trip → scrolling the timeline (the Macrobenchmark module generates them). Enable `isShrinkResources = true` with R8 full mode.

### 2.2 Permissions (Fact: merged release manifest)

Declared directly in `RN android/app/src/main/AndroidManifest.xml`:

| Permission | Why it was there | Native app |
|---|---|---|
| `INTERNET` | API | **Keep** |
| `READ_CALENDAR`, `WRITE_CALENDAR` | Add to calendar (`expo-calendar`) | Only if Add to calendar is built (OQ-A9.6) |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (maxSdk 32) | Expo file system / sharing defaults | **Drop**: use `FileProvider` + share intent for the data export or recovery codes |
| `SYSTEM_ALERT_WINDOW` | React Native dev menu (template; also in main) | **Drop** |
| `VIBRATE` | `expo-haptics` | Not needed for `HapticFeedback`/`View.performHapticFeedback`; drop unless `Vibrator` is used |

Added by libraries (merged manifest):

| Permission / component | Library | Native app |
|---|---|---|
| `POST_NOTIFICATIONS` | expo-notifications | **Keep** (API 33+ runtime permission) |
| `RECEIVE_BOOT_COMPLETED` | expo-notifications (re-arms scheduled notifications after reboot) | **Keep**, with a boot receiver that reschedules reminders from the DB |
| `WAKE_LOCK` | expo-notifications / Firebase | Not needed with `AlarmManager` + `BroadcastReceiver`; drop unless WorkManager's foreground work needs it (WorkManager declares its own) |
| `com.google.android.c2dm.permission.RECEIVE`, `FirebaseMessagingService`, `FirebaseInitProvider`, `datatransport` | Firebase via expo-notifications | **Drop**: there is no push to the app (§7) |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | expo-secure-store | Drop unless biometrics are used |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | NetInfo | Keep `ACCESS_NETWORK_STATE` (ConnectivityManager callback); drop WIFI |
| Badge permissions (Samsung, HTC, Sony, Huawei, OPPO…) | ShortcutBadger in expo-notifications | **Drop** |
| `BIND_GET_INSTALL_REFERRER_SERVICE` | expo / Play referrer | Drop |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX core | Automatic |

Not declared anywhere: `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`. Expo's scheduler therefore ran inexactly on Android 12+. See OQ-A9.4.

### 2.3 Activity, deep links, cleartext, backup (Fact)

- One `MainActivity`, `launchMode="singleTask"`, `screenOrientation="portrait"`, `windowSoftInputMode="adjustResize"`, and a long `configChanges` list (`keyboard|keyboardHidden|orientation|screenSize|screenLayout|uiMode|smallestScreenSize|assetsPaths|locale|layoutDirection`) so the activity is never recreated (`RN android/app/src/main/AndroidManifest.xml`).
- Deep links: only custom schemes `chronoflow://` and `exp+chronoflow://` (dev client). **No App Links / `autoVerify`.** Notification taps open `chronoflow://trip/<tripId>` (`RN src/reminders/notifications.ts:17-28`, regex `^chronoflow:\/\/trip\/[^/?#]+$`).
- Decided on 2 Nov but never built: email links become universal links / **Android App Links on `app.chronoflow.net`**, the web pages as fallback, and no `chronoflow://` hand-off, "since another app can claim that scheme" (`RN docs/web-api-requests.md` §1). iOS built this: `applinks:app.chronoflow.net` with the exact reset (token + email) and signed verify (`{id, hash, expires, signature}`) URL shapes (`iOS plan.md` OQ 25).
- Cleartext: `usesCleartextTraffic="false"` in main, `"true"` only in `src/debug` (`RN plugins/with-local-api.js:13-46`). Release builds refuse an HTTP API URL at config time (`RN app.config.ts:31-43`).
- Backup: `allowBackup="true"` with `fullBackupContent="@xml/secure_store_backup_rules"` and `dataExtractionRules="@xml/secure_store_data_extraction_rules"`. Those rules come from expo-secure-store and exclude only its own prefs, because Keystore keys do not survive a restore. The account SQLite files *were* backed up.
- `expo.modules.updates.ENABLED=false` (no OTA).

**Rec.**
- Keep `singleTask` (notification and App Link taps land in the existing task). **Do not** copy the giant `configChanges`: Compose with `ViewModel` + `rememberSaveable` survives recreation, and handling `uiMode`/`locale` manually breaks the per-app language and dark theme switching.
- Declare App Links for `https://app.chronoflow.net` (paths for reset and verify as iOS does) with `android:autoVerify="true"`. The server must publish `/.well-known/assetlinks.json` with the release signing SHA-256 (OQ-A9.3). Keep `chronoflow://trip/{id}` only as an *internal* `PendingIntent` target. Better still, use an explicit `PendingIntent` with extras, so no exported scheme is needed for notifications.
- Network security config: cleartext only in the debug source set (`res/xml/network_security_config.xml` in `src/debug`, allowing `10.0.2.2` and Valet hosts). Release is HTTPS-only.
- Backup: **exclude the per-account database directory, DataStore files holding the session and the Keystore-encrypted prefs** from both `full-backup-content` and `data-extraction-rules` (cloud-backup and device-transfer). iOS decided the same (OQ 26, D19: "exclude the entire account directory… document that device loss/restore loses unsent changes").

### 2.4 Splash, icon, theme, back (Fact)

- `Theme.App.SplashScreen` (parent `Theme.SplashScreen`): `windowSplashScreenBackground=@color/splashscreen_background`, `windowSplashScreenAnimatedIcon=@drawable/splashscreen_logo`, `postSplashScreenTheme=@style/AppTheme`, `android:windowSplashScreenBehavior=icon_preferred` (`RN android/app/src/main/res/values/styles.xml`).
- Colours: light `#1f285a`, dark `#06091c` (`values/colors.xml`, `values-night/colors.xml`). These are `palettes.light.skyTop` / `palettes.dark.skyTop` (`RN app.config.ts:96-104`). `iconBackground #0a0e1f`; `colorPrimary #023c69` is a template leftover.
- The moon PNG is rendered from the board's SVG (`M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`, 24 viewBox, stroke 1.6, round caps) at 104 pt, with an 18 px CSS blur (Gaussian sigma 9). The PNG is 1152 px; `imageWidth: 288` "keeps the 104 pt moon and glow inside Android's 192 dp mask" (`RN plan.md:1153,1164`).
- The JS `LaunchOverlay` then plays the canvas animation (0–1500 ms; hand-off scale 1.08 + fade 400 ms; slow status at 2 s; give up at 8 s; Reduce Motion 300 ms cross-fade) (`RN plan.md:1154-1161`). On Android the slow status is announced with `AccessibilityInfo.announceForAccessibility` (`RN src/components/shell/LaunchOverlay.tsx:273-283`).
- Before Phase 21, "the last Android build still showed Expo's defaults, because `android/` was prebuilt before the splash config existed" (`RN plan.md:1151`). That was a CNG staleness trap and does not apply natively.
- Adaptive icon: background, foreground and **monochrome** layers (`RN app.config.ts:59-65`; `res/mipmap-anydpi-v26/ic_launcher.xml`). The design canvas now supplies Android layers: `_design/canvas/icon/android/adaptive-{background,foreground,monochrome}.{svg,png}`. **Use the canvas layers**, not RN's.
- Back: `predictiveBackGestureEnabled: false` (`RN app.config.ts:66`) became `android:enableOnBackInvokedCallback="false"`. `MainActivity.invokeDefaultOnBackPressed` moves the root task to the back on API ≤ 30 (template).

**Rec.**
- `androidx.core:core-splashscreen` with `installSplashScreen()`. Keep the system splash on screen with `setKeepOnScreenCondition { !sessionResolved }`, then hand over to a Compose launch scene that starts from the identical frame (moon centred on `skyTop`). Port the timeline from the iOS spec (`iOS plan.md` §"Splash layer tree and geometry", §"Readiness rules (RN Phase 21, with the RN bug fixed)"). iOS documents an RN readiness bug it fixed; read that section before porting.
- **Enable predictive back** (`enableOnBackInvokedCallback="true"`, Navigation Compose's built-in predictive-back animations, `PredictiveBackHandler` for the wizard). The RN reason for turning it off was never recorded. targetSdk 36 makes the system back-to-home animation default-on.
- Themed (monochrome) icon from the canvas `adaptive-monochrome`.

---

## 3. Android-specific bugs and workarounds found in RN code (Fact, then Rec)

| # | Area | What RN does on Android | Source | Native lesson |
|---|---|---|---|---|
| 1 | Header inset | Android never adds the native header height to the first scroll view, and a transparent header leaves no room. A hook supplies `top`, `scrollTop` and `bottom` (the nav bar "Android draws content under"); resting scroll offset is `0` on Android vs `-headerHeight` on iOS | `src/navigation/useHeaderInset.ts:6-37` | Use `Scaffold(topBar=…){ padding -> }` and `contentPadding` on `LazyColumn`; nested scroll with `TopAppBarScrollBehavior`. |
| 2 | Tab bar inset | Hard-coded **80 dp** Material bar + bottom inset, added once at the end of lists | `src/navigation/native-tab-bar.ts:3-8`; `TripScreen.tsx:231,290` | `NavigationBar` inside `Scaffold.bottomBar`; read padding, never a constant. |
| 3 | Toast + TalkBack | The toast is wrapped in a full-screen `accessibilityLiveRegion="polite"` view, "lifted above the navigator" (commit `9dfa131`); on iOS it is announced instead | `src/components/shell/Toast.tsx:171-183` | Use `SnackbarHost` in the `Scaffold`; it announces itself. Keep the toast above the bottom bar. |
| 4 | ErrorBanner announce | Explicit announcement only on iOS; Android relies on `accessibilityLiveRegion="polite"` + role alert | `src/components/ui/ErrorBanner.tsx:28-40` | `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on banners. |
| 5 | Notification permission | Expo reports `denied` with `canAskAgain` before Android 13's first prompt; RN treats that as `undetermined` until its own kv flag `notificationPermissionRequested` is set | `src/settings/useNotificationPermission.ts:27-34,84-88` | Track "requested once" in DataStore; combine with `shouldShowRequestPermissionRationale`. Below API 33, check `NotificationManagerCompat.areNotificationsEnabled()`. |
| 6 | Notification channel | Channel `reminders`, name `t('Reminders on this phone')`, `IMPORTANCE_DEFAULT`, created **at startup** (commit `9972307` "create the android reminders channel at startup and use it") and before asking permission | `src/reminders/notifications.ts:9-15`; `useNotificationPermission.ts:83` | Create the channel in `Application.onCreate` (needed before the API 33 prompt can show). Re-create on locale change so its name follows the app language. |
| 7 | Blocked-notification copy | The blocked state names iOS's "Settings → Notifications → ChronoFlow" path on every platform; no Android board | `docs/plan-gate.md:961` | Open `Settings.ACTION_APP_NOTIFICATION_SETTINGS`; copy needs an Android variant (OQ-A9.10). |
| 8 | Open email app | `ACTION_MAIN` + `CATEGORY_APP_EMAIL` via expo-intent-launcher; iOS uses `message://` | `src/components/auth/openEmailApp.ts:13-17` | `Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_EMAIL)` with `FLAG_ACTIVITY_NEW_TASK`; handle `ActivityNotFoundException`. |
| 9 | Keyboard | Auth: `KeyboardAvoidingView` enabled *only on Android* (`behavior="height"`); `keyboardDismissMode` `on-drag` on Android vs `interactive` on iOS; settings pages disable KAV on Android; wizard uses `height` on Android | `src/components/auth/AuthScreen.tsx:46-64`; `SettingsPage.tsx:56-79`; `trip-wizard/WizardShell.tsx:76` | `enableEdgeToEdge()` + `Modifier.imePadding()` / `WindowInsets.ime`; `BringIntoViewRequester` for focused fields (RN's sign-up bug: "focusing a field scrolls it fully above the keyboard … also brings the submit button into view", `plan.md:819`). |
| 10 | Disabled toolbar button | Android drops `accessibilityHint`, so RN folds "Needs a connection" into the label on Android | `src/components/trips/TripsScreen.tsx:120-128`; `src/test/pressable-stack-toolbar.tsx:24` | Put the reason in `contentDescription`/`stateDescription`; Compose has no hint either. |
| 11 | Calendar picker | iOS uses `Calendar.presentPicker()`; Android focuses a hidden zero-size `@react-native-picker/picker` in dialog mode, hidden from accessibility | `src/components/plan/CalendarExportSheet.tsx:325-370`; `docs/plan-gate.md:1319` | A Compose `AlertDialog` with radio rows of writable calendars (`CalendarContract.Calendars` where `CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`); default to `IS_PRIMARY`. |
| 12 | Default calendar | iOS uses the default calendar; Android uses `isPrimary` | `src/plan/calendar-service.ts:28-38` | Same rule. |
| 13 | Toolbar icons | Android's toolbar draws only image sources, so RN shipped 96×96 RGBA PNGs (`toolbar-{more,list,timeline,back,restart,plus,close}.png`) and a vector `trip-title-chevron.xml` | `src/navigation/toolbar-icons.ts:4-23`; `assets/icons/` | Use Material Symbols vector drawables (or `Icons.*`) tinted from the theme; no PNGs. |
| 14 | Tab icons | Material Symbols `today` / `flight` / `settings` (iOS `sun.horizon` / `airplane` / `gearshape`) | `src/app/(app)/(tabs)/_layout.tsx:26-50`; `plan.md:939` | Same three symbols (outlined/filled pair for selected). Note iOS later changed Today to "the board dashboard glyph" (`iOS plan.md:1670`); check the canvas (OQ-A9.9). |
| 15 | Bottom sheets | Expo UI Compose `ModalBottomSheet`, `skipPartiallyExpanded`, `containerColor=card`, content `maxHeight = 85%` of the window, scrollable; form-sheet routes get `sheetCornerRadius: radius['3xl']` on Android only | `src/components/ui/NativeSheet.android.tsx`; `src/navigation/sheet-options.ts:6-15` | Material 3 `ModalBottomSheet(skipPartiallyExpanded = true)`, shape from the `3xl` radius token, content capped at 85% and scrollable (RN: "so large text sizes and landscape keep the buttons reachable", `plan.md:997`). |
| 16 | Stepper | "the installed Expo UI exposes Stepper only on iOS"; Android uses increment/decrement buttons | `plan.md:1380`; `src/components/trip-wizard/NativeControls.android.tsx` | Build a small Compose stepper (− value +) with 48 dp targets and `semantics { progressBarRangeInfo / setProgress }`. |
| 17 | Day menu in the title | Compose `DropdownMenu` + `SelectableDropdownMenuItem` with supporting text (date), opened from a `TextButton` title with a chevron; Figtree 600 17/22 title and 400 12/15 subtitle | `src/components/plan/TripTitle.android.tsx` | Same structure in a custom `TopAppBar` title; `DropdownMenuItem` with `trailingIcon`/check for the selected day. |
| 18 | Compact header blur | "Android's BlurView without a `blurTarget` may only tint, leaving the timeline visible underneath" | `plan.md:1554` (OQ 46) | `Modifier.blur` works on API 31+ only and blurs the node itself, not what is behind it. Use an opaque `surface` (or a 90–95% alpha tint) for the compact line on Android. See OQ-A9.8. |
| 19 | Clipboard secrets | Recovery codes are copied with `isSensitive: true` | `src/components/settings/TwoFactorScreen.tsx:64` | `ClipData.description.extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)` (API 33+). |
| 20 | Monospace | Codes use `monospace` on Android, `Menlo` on iOS | `src/theme/type.ts:36` | `FontFamily.Monospace`. |
| 21 | Shadows | The design's soft shadow cannot be expressed; RN approximates with `elevation: 10` ("React Native cannot represent CSS spread") | `src/theme/tokens.generated.ts:135-143` | Compose `Modifier.dropShadow` (Compose UI 1.9+, supports spread and colour) or `shadow(ambientColor, spotColor)`; verify on device. |
| 22 | Intl | `formatMinutes` avoids `Intl.NumberFormat` unit style; 12-hour times are assembled by hand | `plan.md:172,175`; `src/plan/copy-format.ts` | Use `java.time` + `DateTimeFormatter.ofPattern` per locale; keep RN's golden cases as JVM unit tests (they include DST across `Europe/Amsterdam`, `Asia/Tokyo`, `America/New_York`). |
| 23 | Header behaviour | iOS minimises the tab bar on scroll (iOS 26); Android keeps the full Material bar | `src/app/(app)/(tabs)/_layout.tsx:17-22`; `plan.md:1460` | Optionally hide the `NavigationBar` on scroll down; default **keep it visible** (Material guidance; matches RN). |
| 24 | Live region on the launch overlay | Android uses `announceForAccessibility` (deprecated on 16) | `LaunchOverlay.tsx:278-282` | Use a live-region `Text` (`semantics { liveRegion = Polite }`), not `announceForAccessibility`, which is deprecated from API 36. |

---

## 4. The timeline: performance problems, causes and what worked

### 4.1 Architecture in RN (Fact)

- **One continuous vertical list** of days (Phase 17, user request 2026-11-02): a `FlatList` with one item per day, `initialNumToRender=3`, `maxToRenderPerBatch=3`, `windowSize=5`, `scrollEventThrottle=16`, a custom `CellRendererComponent` that measures cell frames (`RN src/components/plan/TripScreen.tsx:437-470`; `useDayScroll.tsx`).
- Each day = `DayHeader` (pinned while its day scrolls; collapsed by default, with a notes count) + `DayTimeline` (Timeline view) or `DayList` (List view) (`plan.md:1018-1037`).
- `DayTimeline` = a **horizontal `ScrollView`** (a day's minimum grid width can exceed the screen) containing lane headings, the gutter and four lanes (`DayTimeline.tsx:69-136`).
- Body: on **iOS** a native `TimelineCanvas` view (Expo module, Swift). On **Android** `ReactNativeDayBody`: gutter `Text` per visible tick, one `Svg` path per lane for the hour grid, one `TimelineBlock` (View tree with icon, title, interval, pattern, borders) per placed block, an SVG dashed zone divider and a pill per zone change (`DayTimeline.tsx:121-131,139-260`).
- Day selection: measured cell tops below the header, viewability as a fallback, programmatic jumps that "hold their selection through intermediate days, retry outside the render window, and release after settling or a user drag" (`plan.md:1029`).
- Phase 25 compact header: a 36 pt glass line driven by a Reanimated UI-thread worklet (`compactShift`), not mounted with a screen reader; the title subtitle shows the top day without per-frame React state (`plan.md:1469-1470,1477`).

### 4.2 Problems measured and their causes (Fact)

- `6e7cc0b perf(plan): keep the timeline smooth while scrolling` (2026-11-05): "Rasterize the pinned day header so iOS stops redrawing its shadow on every scroll frame, and draw each lane's hour grid as one SVG path instead of a view per line. On the simulator this cut dropped frames over twelve swipes **from about 40 to about 15** and the longest stall **from about 330 ms to 125 ms**." Causes: (a) a shadow re-rendered every frame on a moving pinned header; (b) one View per grid line.
- A frame probe (`src/dev/frame-probe.ts` on branch `perf/timeline-base`, commit `2653af4`) logs UI-thread frames, dropped frames (`delta > 25 ms`, counted as `round(delta/16.7) − 1`) and the worst frame every 2 s.
- Seven experiment branches tried to make the RN body cheaper. **None was merged** (`git merge-base --is-ancestor` fails for each):
  - `perf/exp-1-core-bar` (`bab9874`): draw the core-priority bar as its own view.
  - `perf/exp-2-icon-wrappers` (`1e9dcd8`): draw the no-caffeine slash in the icon and drop the hiding wrappers.
  - `perf/exp-3-grid-gradient` (`a5a1b89`): draw the hour grid with background gradients.
  - `perf/exp-4-pattern-gradient` (`7813f88`): draw tile hatch and dot patterns with gradients instead of SVG `Pattern`.
  - `perf/exp-6-icon-font` (`2189a57`): draw icons from the lucide font instead of SVG components.
  - `perf/exp-7-staged-mount` (`ebea13a`): mount a day's tiles lane by lane after its grid.
  - `perf/exp-8-native-day` (`2d9ea94` + `6d9294b`): **merged.** A native iOS view draws the whole day body.
- Cause, in summary: **per-block view trees** (each tile has borders, a pattern, an icon, up to 3 text runs and a tag), **SVG patterns and icons**, **text layout of every tile on mount** and **shadows on moving views**. With 3–5 days mounted and 10–40 tiles per day this exceeds the frame budget during fast flings. The commit messages carry no numbers for exp-1…7; only the exp-8 report survives.

### 4.3 What the native iOS day body does (Fact, `RN git show 2d9ea94:modules/timeline-canvas/README.md`, and the current module)

- JS resolves **all** geometry, token colours, copy, glyphs and accessibility labels into a `DayCanvasPayload` (`RN modules/timeline-canvas/src/TimelineCanvas.types.ts`): `width, height, fontScale, lanes[{x,width}], laneFill, laneRadius, gridColor, gridLines[], tickColor, ticks[{y,label}], divider{line,pill,text}, dividers[{y,label}], tiles[CanvasTile]`. Each `CanvasTile` has `frame`, `hit` (≥ 44 pt), per-corner `radii`, `padding`, `fill`, per-side `borderWidths`/`borderColors`, `borderDashed`, `pattern{kind: hatch|dots, color, opacity}`, `opacity`, `iconColor`, `glyph`, `slash`, `status{glyph,size,narrow}`, `continuesBefore/After`, `iconOnly`, `textColor`, `textBackground`, `title`, `interval`, `tag{label,color,background,border}`, `reason`, `accessibilityLabel`.
- **The now line is a separate prop** (`NowLine {y, label, accessibilityLabel, color, textColor}`): "`now` changes update only the clock layers and the existing clock accessibility element" (`modules/timeline-canvas/README.md`). A minute tick never rebuilds the tiles.
- Drawing: small bitmap-backed tile, tick and pill layers, solid lane layers and shape layers for lines; lucide **font** glyphs for icons; CoreText for text; a port of RN's border geometry (rounded per-side borders, dashed borders).
- Hit testing in reverse tile order; one accessibility element per tile on a child view.
- Memory estimate: at most **3.79 MiB/day** of tile backing stores at @3x (S2, 12 tiles); S6 2.34–3.48 MiB/day; **7–17.4 MiB** for 3–5 mounted days.
- Never compiled or measured by RN itself ("No native build, Swift compilation… or device measurement was run"). The native iOS app later re-implemented the drawing as **bounded SwiftUI `Canvas` strips sharing one immutable vector drawing per day**, cached by content/width/appearance/type size/scale (4 entries / 4 MiB cap), with the now line as a separate view, and budgets enforced in tests: first visible timeline frame ≤ 0.8 s, hitch ratio ≤ 5 ms/s, memory growth ≤ 40 MiB over a 60-day traversal; host decode + layout budgets S3 40 ms, 30 days 100 ms, 60 days 200 ms (`iOS plan.md:1705-1739`; `iOS Docs/perf-timeline.md`).

### 4.4 Geometry constants (Fact, `RN src/plan/timeline.ts:13-31`, tokens `src/theme/tokens.generated.ts:124-134,212`)

| Constant | Value |
|---|---|
| Lane order | `sleep, light, caffeine, travel` (the `info` lane is not a column; its blocks become gutter markers) |
| `PX_PER_HOUR` | `space(10)` = **40** pt |
| `MIN_TILE_HEIGHT` | `space-6` = **24** |
| `TINY_TILE_HEIGHT` | `space-14` = **56** (below this a tile is "tiny", icon-only) |
| `GUTTER_WIDTH` | `space(11)` = **44** |
| `GUTTER_MARKER_SIZE` | 24 |
| `LANE_GAP` | `space-1` = **4** |
| `TILE_INSET` | `space(0.5)` = 2 |
| `MIN_LANE_WIDTH` | 44; `MIN_COLUMN_WIDTH` 30; `MIN_TICK_SPACING` 30 |
| Compact tiles | window width < `space(160)` = **640** → title/time/details hidden (`DayTimeline.tsx:116,128`; iOS confirms, `iOS plan.md:1760`) |
| Day height | from the UTC duration (days can be 23, 25 or 30+ h), never 24 × 40 |
| Grid line | `stroke = border @ 60% alpha`, width 1, at `tick.top + 0.5` |
| Zone divider | dashed `catTravel` line, width 2, dash `[2, 4]`, round caps, pill "{{zone}} time from here" |

The server supplies day bounds, zones and ticks. The client never computes days (`iOS plan.md:1436`).

### 4.5 Rec: the Android timeline

1. **Pure Kotlin layout module** (`:core:plan`, no Android deps), a port of `layoutPlan`/`layoutDay`/`indexTimeline`/`nowLineOffset` with RN's S1–S6 fixtures as golden tests (iOS did the same in Phase 14). Run it **off the main thread** once per `(tripId, inputHash, detailVersion, width class, font scale, theme)` and cache it. iOS budgets: S3 ≤ 40 ms, 60 days ≤ 200 ms on host.
2. **One Compose `Canvas`/`drawBehind` per day body** (or per bounded vertical strip of a day, as iOS does), never a composable per tile:
   - Build an immutable per-day **draw list** off-main (rects, rounded rects, paths for borders/patterns, pre-measured `TextLayoutResult`s via `TextMeasurer`, icon `ImageVector`→`Path`/`Painter`).
   - Record it once into a `GraphicsLayer` (`rememberGraphicsLayer()` + `layer.record { … }`) or a `Picture`, and replay that each frame. Scrolling then costs a layer draw, not a re-layout.
   - Patterns (hatch/dots): pre-built `ShaderBrush`/`ImageShader` from a small tile bitmap per (pattern, colour, density), clipped to the tile's rounded rect.
   - Icons: lucide as vector `ImageVector`s (or the lucide font, as iOS RN did) drawn via `Painter` into the layer; never one `Icon()` composable per tile.
3. **The now line is a separate small composable/layer** above the day canvas, driven by a minute-aligned clock flow. A tick invalidates only it (iOS: "minute ticks affect one clock child").
4. **Hit testing and accessibility:** one `pointerInput` per day that maps a tap to the tile with the hit rect (≥ 48 dp on Android; RN used 44 pt) in reverse draw order. For TalkBack, expose **virtual semantics children**: a `Box` per tile with `Modifier.semantics { contentDescription = tile.accessibilityLabel; onClick {…} }`, sized to the hit rect, or `Modifier.semantics` on a full overlay with `CustomAccessibilityAction`s. iOS chose one element per block in time order, plus "Plan timeline" and "Now, {{time}}" elements.
5. **The list:** `LazyColumn` with `key = day.id` and `contentType = view mode`, `stickyHeader` for day headers, **no nested vertical scrollers**. Horizontal lane overflow (wide grids) uses one `horizontalScroll` per day row. Keep the scroll position in `LazyListState`; derive the selected day with `derivedStateOf { firstVisibleItemIndex … }`, never per-frame recomposition of the whole page.
6. **Compact header:** read the scroll offset inside `graphicsLayer { }` lambdas (`Modifier.graphicsLayer { alpha = …; translationY = … }`) so it changes in the draw phase only. iOS's final rule: fade in over the last 24 pt, instant swap under Reduce Motion (`iOS plan.md:1490,1693`).
7. **Avoid what hurt RN:** no elevation shadow on a moving sticky header (draw a flat 1 dp divider or bake the shadow into the recorded layer); no SVG; no per-tile `Text`; no work in `onScroll` beyond reading offsets.
8. **Measure from day one:** Macrobenchmark `FrameTimingMetric` on scrolling the S3 trip and synthetic 30/60-day trips (iOS built `large30`/`large60` fixtures from repeated S3 travel days + S1 caffeine blocks, `iOS plan.md:1714`), `StartupTimingMetric` to Today, JankStats in debug. Adopt iOS's budgets: first timeline frame ≤ 0.8 s, < 5 ms/s hitch time (≈ janky-frame ratio < 1% at 120 Hz), memory growth ≤ 40 MiB over a 60-day traversal. Run on the Pixel 8 AVD *and* (when available) a mid-range physical phone; RN's pending procedure (S6 at frozen `2026-10-26T12:00:00Z`, cold start → Today and first trip render, median of 5) is a good protocol to copy (`RN docs/plan-gate.md:1357-1363`).

---

## 5. Local database (useful for the Room schema)

### 5.1 RN schema at HEAD (Fact, `RN src/db/schema.ts`, migrations `0000`–`0014`)

One SQLite file per account, `account-<accountId>.db`, WAL, `foreign_keys = ON`, change listener on (`RN src/db/account-database.ts:17-24`). It is deleted on logout, on account deletion and on a 401.

| Table | Columns (SQL name, type) | Key | Notes |
|---|---|---|---|
| `account` | `id` text, `name` text, `email` text, `has_verified_email` int bool default 1, `synced_at` text ISO, `walkthrough_seen` int bool default 0 | `id` | one row |
| `trips` | `id` text, `card` JSON `TripCardView`, `adaptation` JSON `{goal,direction,shiftMin}`?, `form` JSON `TripFormView`?, `revision` JSON `TripFormRevisionView`?, `current_input_hash` text?, `keep_offline` bool default 0, `departure_utc` text, `plan_start_utc`?, `plan_start_zone`?, `preparation_days` int?, `plan_end_utc`?, `plan_end_date`?, `deleted_at` text?, `deleted_locally` bool default 0 | `id` | summary columns come from the list item, so Today works without a stored plan (Phase 19b/20); `deleted_at` = tombstone when missing from the list |
| `plan_versions` | `trip_id` → trips cascade, `input_hash`, `detail` JSON `StoredTripDetail = Omit<ApiTripDetail,'checkState'\|'detailVersion'>` (`{trip, plan, notices, timeline}`), `received_at`, `detail_version` text? | (`trip_id`,`input_hash`) | upserted for the same hash when `detailVersion` changes |
| `check_marks` | `trip_id`, `input_hash`, `type` (BlockType), `local_date`, `ordinal` int, `status` (`done`\|`skipped`), `marked_at` | (`trip_id`,`input_hash`,`type`,`local_date`,`ordinal`) | identity string `type\|localDate\|ordinal` (`src/plan/check-state.ts:8-10`) |
| `settings` | `section` text, `value` JSON | `section` | sections `bodyClock, preferences, reminders, planCredits, policies` + derived/aux `sleepAnswered, readiness, billing, profileChange, dataExport, security, securityChange, securitySecrets, syncFailure` |
| `drafts` | `id`, `trip_id`?, `form` JSON, `base_form`?, `dirty` bool, `review`?, `token`?, `input_revision` int default 0, `review_revision`?, `wizard_source_revision`?, `wizard_review` JSON?, `furthest_step`?, `wizard_state` JSON?, `source_revision` JSON?, `save_failure` JSON?, `field_errors` JSON?, `updated_at` | `id` | after Phase 24 used only as the hidden **30-minute wizard recovery** row (fixed id `member-wizard`), never listed |
| `outbox` | `id` (UUIDv7), `kind`, `payload` JSON, `created_at`, `attempts` int default 0, `last_error`? | `id` | kinds `check-off, settings, complete-trip, profile, data-export, security, walkthrough` |

Outside SQLite:

- **kv-store** (`expo-sqlite/kv-store`, survives logout): `appearance` (`system|light|dark`), `language` (override), `api.deviceId` (UUIDv7), `notificationPermissionRequested`, `walkthroughSeen` (mock only), `reminderLedger:*` (per account; cleared on sign-out), and the guest keys `guest-draft`, `guest-draft-continuation`, `guest-trip-continuation`, `guest-claim-lookup` (`src/trips/guest-draft-store.ts:11-14`).
- **SecureStore**: `session` (token + account id), `profile-input-<accountId>`, `security-input-<accountId>`, `security-secrets-<accountId>`, and the guest possession secret keyed by draft token. **Passwords never land in SQLite or the outbox** (`plan.md:1199,1263`).
- **Zustand (memory only)**: sync status, toast, practice mode, trip-page title/day store. `resetUiStores()` runs on sign-out.

Why this design (Fact, `RN docs/research/local-database.md`): the plan is display-ready and never changes once built (one JSON per version, not a row per block); a check-off and its outbox row must commit together; stale plan versions need version checks; logout = delete the file. Size: a trip page is 35–115 KB of JSON (≤ 240 KB); 50 trips ≈ 5 MB. "Performance work belongs in the timeline, not the database."

### 5.2 Rec: Room schema

- **Room 2.7+ with KSP**, `RoomDatabase.Builder.setJournalMode(WRITE_AHEAD_LOGGING)`, one database **per account** (`account-<uuid>.db` under `noBackupFilesDir` or a dedicated dir excluded from backup), opened by a session-scoped DI component; `close()` then `context.deleteDatabase(name)` on logout.
- Same seven tables, same keys. Store JSON columns as `TEXT` holding **the server's raw bytes** for `plan_versions.detail` (iOS lesson: re-encoding a decoded struct loses unknown fields; iOS stores raw response bytes, `iOS plan.md:1503`). Decode with kotlinx.serialization (`ignoreUnknownKeys = true`, tolerant enums with an `Unknown(raw)` fallback skipped at render time).
- Add an index on `outbox(created_at, id)`, and on `trips(deleted_at)`.
- Drop RN's legacy draft columns (`form`, `base_form`, `dirty`, `review`, `save_failure`, `field_errors`) that served the deleted editor. Keep one `wizard_recovery` table or a `drafts` row with `{id, updated_at, state JSON, token, input_revision, review_revision, source_revision, furthest_step, review JSON}`.
- Expose reads as `Flow`s (Room invalidation tracker = RN's live queries). Screens read **only** from Room.
- Device prefs (theme, language override, device id, notification-asked flag, reminder ledger) in **DataStore** (Preferences). Secrets (token, guest secret, pending password-bearing inputs) in DataStore encrypted with an **Android Keystore AES-GCM key** (Jetpack Security's `EncryptedSharedPreferences` is deprecated). Exclude both from backup.
- Migrations: start at schema v1 with Room's exported schemas + `AutoMigration` tests. There is no data to migrate from RN (different install, OQ-A9.2).

---

## 6. Sync semantics (Fact: `RN src/sync/engine.ts`, `triggers.ts`, `outbox-failures.ts`, `plan-window.ts`, `actions.ts`, `src/db/store-account.ts`, `src/session/sign-out.ts`)

### 6.1 `syncNow` order (`engine.ts:46-113`)

1. If NetInfo says offline → `{state: offline}` (connectivity is only a hint; iOS D23 dropped the pre-check and maps failed requests to offline instead).
2. **Flush the outbox** in `created_at, id` order (below).
3. `fetchAccount` (`GET users/me`; if verified, `GET account`) → `upsertAccount`, which **keeps pending local edits**: a queued walkthrough forces `walkthroughSeen = true`, a queued profile keeps the local name, and a settings section with a queued change is **not overwritten** (`store-account.ts:7-80`).
4. If `!hasVerifiedEmail` → stop (no trip pulls, `syncedAt` unchanged, no backoff).
5. `GET trips` → `upsertTripCards` in one transaction: trips missing from the list get `deleted_at = now`; each item upserts card, `adaptation`, `current_input_hash = planInputHash`, the five summary columns and `keep_offline`. A queued `complete-trip` overrides the card to `status: completed, canComplete: false`. `setWhere deleted_locally = false`.
6. `account.synced_at = now` (so one failed detail cannot strand first-load skeletons).
7. Evict plans and marks of tombstoned trips (unless an outbox row references them).
8. For each item: if `!keepOffline` and the trip is not **held** (an open trip screen holds it, `plan-window.ts`) → evict its plan and marks (unless the outbox references it). Else if the stored plan for `planInputHash` has the same `detailVersion` → just `replaceCheckMarks(item.checkState)`. Else → `pullPlanIfNew` (`GET trips/{id}`), deduplicated per trip with an in-flight promise map.
9. Detail errors: offline/401/unverified/429 abort the sync; 404 is skipped; others set `retry = true` and the others still pull.
10. `requestReminderScheduling()`.

`fetchAndStorePlan` (`engine.ts:557-606`): upsert `plan_versions(trip_id, input_hash)` with detail, `received_at`, `detail_version`; set `trips.current_input_hash`, and `form`/`revision` when present; `replaceCheckMarks`, which is **skipped while any check-off for that trip is queued** (`engine.ts:608-641`).

### 6.2 Outbox flush (`engine.ts:115-293`)

- **Dependency chains:** `settings:<section>`, `trip:<tripId>` (check-off and complete-trip), otherwise the kind. After a retryable failure the chain is **blocked** for the rest of this flush, so the order within a chain is kept; other chains continue.
- Check-off / complete-trip for a trip that is gone, deleted locally or tombstoned → row deleted.
- **Re-keying** (`rekeyCheckOff`, `:329-443`): if the change's `planInputHash` ≠ the trip's current hash, pull the current plan if missing, find the actionable block with the same `checkKey` identity, rewrite the payload's hash (attempts reset) and move the local mark, unless a newer queued change or a newer local mark exists. No match → drop the row.
- Check-off send: `PUT trips/{id}/checks`, body exactly `{plan_input_hash, type, local_date, ordinal, status}` (`status: null` = undo). The 200 response `{checkState, detailVersion}` → delete the row and `replaceCheckMarks` in one transaction; if `detailVersion` differs from the stored one → pull the detail.
- **Stale plan** (422 with `errors.plan_input_hash`, or `errors.form == "This plan has changed. Refresh it before marking this step."`): pull once, retry once.
- Settings: `PATCH account/{body-clock|preferences|reminders}` with a partial snake_case body (absent = unchanged, `null` clears, `hidden_categories` replaces the list; the body clock sends its whole section while incomplete); `policies` → `POST terms/acceptance {accepted: true, version}`; the returned `ApiAccountSnapshot` is stored with the row's deletion in one transaction (`ApiDataSource.ts:113-148`).
- Walkthrough → `POST walkthrough`.
- **Final vs retry** (`outbox-failures.ts:20-44`): 4xx except 401/408/425/429 is final; security and data-export are final for anything but offline/401. Final → row deleted, `settings.syncFailure` written, and a toast with the change's "refused" copy (`change-labels.ts`). Retryable → `attempts + 1`, `last_error`, chain blocked. `NotOnTheApiYetError` → attempts reset, chain blocked silently.
- Offline / 401 / `email_unverified` abort the whole flush.

### 6.3 Triggers and backoff (`triggers.ts`)

- `requestSync()` is debounced **1 s**; `refreshNow()` runs at once (pull-to-refresh) and coalesces with an in-flight run (re-run loop).
- Triggers: sign-in / database open, app foreground (`AppState active`), reconnect (NetInfo offline → online), pull-to-refresh, after every outbox write.
- Retry backoff: `min(1000 × 2^max(consecutiveFailures, max(outbox.attempts)), 5 min)`.
- 401 → `cancelPendingSync()` + the unauthorized handler (sign-out with `unauthorized: true`, which skips the logout call). Unverified → cancel, no backoff.
- A generation counter discards results from a database that was swapped mid-sync.

### 6.4 Local writes (`actions.ts`)

`markStep`: in one transaction upsert/delete `check_marks` **and** insert the outbox row (`id = uuidv7()`, `created_at = now`), then `requestSync()` and reminder rescheduling. The same pattern serves settings sections, profile, walkthrough and complete-trip.

### 6.5 Sign-out (`src/session/sign-out.ts:32-79`)

Coalesced. If outbox rows exist and discard was not requested: try one sync; if rows remain → `{needsConfirmation: true}` (the SignOutPending sheet lists the pending changes). Otherwise: cancel sync → best-effort `POST auth/logout` (3 s, skipped on 401) → set signed-out and navigate → wait ≤ 500 ms for the DB provider to unmount → cancel reminders and clear the ledger → delete export files → close and delete the DB file → clear security secrets → clear the session → reset UI stores.

### 6.6 Rec: sync on Android

- Port these semantics **exactly** (iOS did: FullPull, OutboxFlusher, CheckOffReconciler, PlanRetention lease). Implement as a `SyncEngine` in a session-scoped coroutine scope with a `Mutex`/single-flight, `Flow`-driven triggers: `ProcessLifecycleOwner` ON_START, a `ConnectivityManager.NetworkCallback` (validated network), pull-to-refresh, after local writes.
- **Background:** add a `WorkManager` `OneTimeWorkRequest` with `NetworkType.CONNECTED` when the outbox is non-empty and the app goes to the background, so queued check-offs drain without the app open. RN had no background sync. Optionally add a periodic (≥ 15 min, battery-not-low) list refresh so `keepOffline` plans are ready before departure. See OQ-A9.5.
- `PlanRetention` = a ref-counted hold taken by the trip screen's ViewModel (RN `holdTripPlan`).
- Replace RN's NetInfo pre-check with iOS's rule: send, and map transport errors/timeouts to offline.

---

## 7. API client behaviour worth keeping (Fact, `RN src/data/api/client.ts`)

- Base URL `https://api.chronoflow.net/v1` (`.env`); locally `https://api.chronoflow-new.test/v1` (Valet; the emulator needs the Valet CA or a debug network config).
- Headers: `Accept: application/json`, `Content-Type: application/json` with a body, `Authorization: Bearer <token>` except on public paths (`airports`, `guest/trips/review`, `auth/register`). `credentials: omit`, `cache: no-store`, **`redirect: error`**, and a response from another origin is rejected (`client.ts:85-115`).
- Timeouts: **15 s** default; **90 s** for wizard writes (`POST trips`, `POST trips/review`, `POST trips/{id}/review`, `PUT trips/{id}`, `POST guest/trips/review`, `POST auth/register`); **3 s** for logout. A timeout counts as `OfflineError`.
- Error envelope `ApiError {code, message, errors?, args?, retryAfterSeconds?}` → `email_unverified` (403), `unauthenticated`, `review_expired` (also `errors.review_expired`), `validation_failed` (422 without field errors → server error), `conflict` (409), `throttled` (`retryAfterSeconds`, else the `Retry-After` header as seconds or an HTTP date, else 60), other → `ServerError(status, message)`. Messages are **untranslated English sentence keys**, translated on the device.
- `Accept-Language` was added in Phase 20 and **removed** in Phase 22: the API is language-free; plans hold no language-specific text; caffeine amounts arrive as data (`kind: "milligrams"`, `value`, `drinks: [{drink, halves}]`) (`plan.md:1180-1181`).
- Device identity: `device_id` = UUIDv7 kept on the device, `device_name` from `Device.deviceName` → model → OS (≤ 255 code points) (`src/data/api/device.ts`). It is sent with login, register and guest review.
- Never log the token, password, challenge id or codes (`plan.md:1060`).

**Rec.** OkHttp + Retrofit (or Ktor) with kotlinx.serialization; `followRedirects(false)`; per-call timeouts via a call-level `timeout()`; an interceptor for bearer and `Accept`; map to a sealed `ApiFailure`. `device_name` from `Settings.Global.DEVICE_NAME` (API 25+) falling back to `Build.MODEL`. Redact secrets in any logging interceptor (or ship none in release).

### 7.1 Endpoints RN uses (Fact) and the current server (Fact, `W routes/api.php` @ `1f4a2684`)

| Endpoint | RN | Server now |
|---|---|---|
| `POST auth/login`, `POST auth/two-factor`, `POST auth/logout`, `GET users/me` | yes | yes |
| `POST auth/register` (with guest claim) | yes | yes |
| `POST auth/forgot-password`, `POST auth/reset-password`, `POST email/verification-notification`, `POST email/verify` | **refused** (`notOnTheApiYet`) | **yes** |
| `POST auth/passkey/options`, `POST auth/passkey/login`; `GET/POST/DELETE account/passkeys…` | hidden in API mode | **yes** (iOS builds sign-in + management) |
| `GET account`, `PATCH account/{body-clock,preferences,reminders}`, `PUT account/password`, `PATCH users/me`, `DELETE account {password}` | yes | yes |
| `POST account/two-factor` (+`/confirmation`, `DELETE`, `/recovery-codes`, `/recovery-codes/reveal`) | yes | yes |
| `POST terms/acceptance`, `POST walkthrough` | yes | yes |
| `GET trips`, `GET trips/{id}`, `GET trips/{id}/input`, `PUT trips/{id}/checks` | yes | yes |
| `GET airports?query&limit`, `POST trips/review`, `POST trips`, `POST trips/{id}/review`, `PUT trips/{id}`, `POST guest/trips/review` | yes | yes |
| Delete / complete / reopen / duplicate trip, data export, billing hand-off | mock only | **no endpoint** |

Web-only (session routes, not API): `trips/{id}/calendar.ics`, `trips/{id}/plan.csv`, calendar feed, web push devices, calendar prompt (`W routes/trips.php:32-35`, `routes/settings.php:23-40`).

---

## 8. Feature inventory and parity gaps

### 8.1 Features in RN that iOS also has (Fact; iOS phases cited)

Splash animation and Welcome intro (iOS 2–5); sign in, two-factor, forgot/reset, sign-up, verify email (iOS 10–11); session gate verify → terms → walkthrough → app (iOS 15a, 17g); system tab bar with Today, My trips, Settings (15a); My trips with Active/Upcoming/Completed and pull-to-refresh (15b); trip page as one continuous scroll, timeline/list toggle in the bar, day menu in the title, compact header, remote/loading/offline states, `awaitingSync` (15c–d); step sheet with Mark as done/Skip/Undo, legend (17a); Today (17b); trip actions (17c, without delete/complete/calendar); edit and duplicate through the wizard (17d); Settings home, sign-out with pending changes (17e); walkthrough and replay (17f, 17h); terms update (17g); Profile (name only), Body clock, Preferences (theme, language, time format, hidden categories), Security (password, two-factor), Plan & credits (read-only), Delete account (17h); member wizard with quiet 30-minute recovery (16); guest wizard and claim (18); local reminders with the first-trip permission sheet (19).

### 8.2 Features in RN or web but missing from iOS (parity gaps to decide)

| Feature | RN | Web | iOS | Default for Android |
|---|---|---|---|---|
| **Add to calendar** (local, writes device calendar events; scopes important/all/flights; alerts none/start/15 min) | built, unverified (`plan.md:414`) | `.ics` export + calendar feed | **deferred** (OQ 42) | **Defer**, as iOS (OQ-A9.6) |
| **Practice in-flight mode** (client-only toggle that blocks review/save) | built (`src/state/ui.ts`, `plan.md:429`) | yes | **not planned** | **Skip** |
| **Email change** with the password prompt | built (`plan.md:1197-1201`) | yes | **deferred** (email read-only, `iOS plan.md` 17h) | Follow iOS (read-only) unless the user wants it (OQ-A9.7) |
| **Your data & policies / data export** | built (mock only) | yes | omitted | **Skip** (no API endpoint) |
| **Billing hand-off** (Buy/Cancel → signed-in web link) | built (mock, behind `EXPO_PUBLIC_BILLING_LINK_OUT`) | yes | read-only Plan & credits; no-credits Ready without an action (OQ 41) | Follow iOS |
| **Delete / complete trip** | built (mock) | yes | not planned (no endpoint) | **Skip** until endpoints exist |
| **Passkey sign-in and management** | hidden in API mode | yes | **built** (Credential Manager equivalent) | **Build with Credential Manager** (OQ-A9.11) |
| **Wide layout** (≥ 600 pt: timeline + 280–320 pt list side by side) | phone layout everywhere | n/a | **built** (15e) | Build for `WindowWidthSizeClass` ≥ Medium (600 dp): foldables and tablets (OQ-A9.12) |
| **TripNotOnPhone** state | built (no board) | n/a | not built (no board) | Skip, as iOS; show ErrorNotFound |
| **Health screening questions** | still present (stale) | **removed** (Phase 19) | removed | **Do not build**; ask only age band + shift work; caffeine and movement-break preferences |
| Optional-items preference | removed (decision 32) | still a server field | removed | Never show optional steps; never send `show_optional_items` |
| Live Activity / ongoing notification | deferred | n/a | not planned | Skip (an Android "ongoing" or Live Update notification is a future idea) |
| Calendar feed, pocket guide, trip filters, reopen | deferred | web | not planned | Skip |
| Web push reminders | n/a (local) | web push | local only | Local only |

---

## 9. Reminders (Fact: `RN src/reminders/selection.ts`, `schedule.ts`; iOS kept the same numbers, `iOS plan.md:2214`)

- Eligible kinds: `seek_light`, `avoid_light` (not the "if you can" ones), `caffeine_cutoff`, `wind_down` (main sleep only). Optional steps are excluded (decision 32). Ticked-off steps are not scheduled.
- `sendAt = start − leadMin` (lead 15/30/60). A send time inside a sleep moves to that sleep's end. Drafts within **15 min coalesce** (`COALESCE_MINUTES = 15`). At most **6 a day** (`DAILY_CAP`), core before recommended, then earlier. `EXPIRY_GRACE_MINUTES = 20`.
- Scheduling window **48 h**, cap **60** pending, selection looks back **36 h** (so already-fired reminders still count toward the daily cap), margin **5 s**. A per-account ledger stops an announced member from being sent again. Only `keepOffline` (window) plans are scheduled, not a transiently opened plan.
- Reschedule after each sync, on foreground, after check/undo, on settings change, and (iOS addition) on significant time or time-zone change.
- Copy: the title is the trip route; the body has one sentence per kind (`'Seek bright light from {{time}}.'`, `'Avoid bright light from {{time}}.'`, `'Last caffeine before {{time}}.'`, `'Wind down for bed: sleep from {{time}}.'`), in start order, with a zone label when a time's zone differs; 12/24 h from the preference (`schedule.ts:33-38`).
- The tap opens the trip (`chronoflow://trip/<tripId>`). iOS opens the step on a warm tap.

**Rec.** `AlarmManager.setAndAllowWhileIdle` (inexact, allowed in Doze; see OQ-A9.4) with a `BroadcastReceiver` that posts on the `reminders` channel; stable request codes from `(tripId, sendAt, kinds)`; reschedule on `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED`; cancel all on sign-out. Port the selection as pure Kotlin and reuse RN's 110 selection vectors (iOS ported them, `iOS plan.md:2224`).

---

## 10. Things in RN not to copy

- The hand-built JS look-alikes that were replaced anyway (floating glass tab bar, custom headers, `ModalSheet`, JS date wheels): Phases 15–16 moved all of them to native controls (`CLAUDE.md` "use native components"). On Android: Material 3 `NavigationBar`, `TopAppBar`, `ModalBottomSheet`, `DatePicker`/`TimePicker`, `Switch`, `SegmentedButton`, `AlertDialog`, tinted with brand tokens. Designed surfaces (trip actions sheet, theme pills, answer lists) stay custom inside native containers.
- The mock `DataSource` as the runtime default. Keep a **mock for tests and debug fixtures only** (iOS: Debug default; Release never).
- Unistyles. In Compose, the theme comes from `CompositionLocal`s/`MaterialTheme`; a theme switch recomposes only the readers. Read colours in draw lambdas for the timeline layer.
- The old trip editor, review and guest wizard (deleted in 24f), and the old `drafts` columns.
- Health screening routes (see §8.2).
- `Accept-Language` on requests.
- `useFrameProbe`-style manual frame logging. Use JankStats/Macrobenchmark.

---

## 11. Open questions (with the default the plan proceeds on)

- **OQ-A9.1 minSdk.** RN used 24. *Default:* **minSdk 26** (adaptive icons, `java.time` without desugaring edge cases, notification channels everywhere; covers ~97% of active devices). Use core-library desugaring anyway for safety.
- **OQ-A9.2 Migration from the RN install.** Same package `net.chronoflow.app`? RN was never released (no production users; the server notes "There is no production data, only Rick's own registration", P19-D13). *Default:* use `net.chronoflow.app` for the native app, with **no migration** of RN's SQLite, kv-store or SecureStore. A fresh sign-in is required. Use a `.debug` applicationIdSuffix for debug builds so they sit beside release.
- **OQ-A9.3 App Links.** Needs `assetlinks.json` on `app.chronoflow.net` with the release certificate fingerprint, and a signing key that does not exist yet. *Default:* declare `autoVerify` App Links for reset and verify (same paths as iOS), and fall back to the web pages until the server publishes `assetlinks.json`. Record the server request.
- **OQ-A9.4 Exact alarms.** `SCHEDULE_EXACT_ALARM` is denied by default for new installs on Android 14+; `USE_EXACT_ALARM` is Play-restricted to alarm and calendar apps. *Default:* **inexact `setAndAllowWhileIdle`** (Doze may delay by minutes; the 15/30/60-minute lead absorbs that). Do not request exact alarms.
- **OQ-A9.5 Background sync.** RN and iOS sync only in the foreground. *Default:* WorkManager one-off flush of a non-empty outbox when connected, plus a 6-hourly periodic list/plan refresh (constraints: connected, battery not low) so the 7-day `keepOffline` window and reminders stay current. Reminders are rescheduled after the worker's sync.
- **OQ-A9.6 Add to calendar.** *Default:* defer, as iOS OQ 42; leave the row out of Trip actions (a known deviation from the board). No calendar permissions in the manifest.
- **OQ-A9.7 Email change.** *Default:* follow iOS: email read-only and name-only Save; revisit with the password prompt later.
- **OQ-A9.8 Compact header surface on Android.** Real behind-content blur is unavailable in Compose without a library (and API 31+). *Default:* an opaque `background` (`card`/`background` token at ~94% alpha) for the compact line and pinned headers, and ask the designer to confirm. Haze-style libraries are not adopted without a device measurement.
- **OQ-A9.9 Tab icons.** RN: Material `today` / `flight` / `settings`; iOS changed Today to the board's dashboard glyph. *Default:* use the canvas's glyphs mapped to Material Symbols (dashboard / flight / settings), outlined when unselected and filled when selected.
- **OQ-A9.10 Android-specific copy** (notification-blocked instructions, "Open phone settings"). *Default:* reuse the existing keys but open the app's notification settings directly; add an Android variant key only if the copy names an iOS path ("Settings → Notifications → ChronoFlow"), and flag it for the designer.
- **OQ-A9.11 Passkeys.** *Default:* build passkey sign-in and management with Credential Manager (`androidx.credentials`), gated on the server's Digital Asset Links (`assetlinks.json` with `delegate_permission/common.get_login_creds`); hide the button when the provider is unavailable.
- **OQ-A9.12 Wide layout.** *Default:* phone layout below 600 dp; iOS's wide layout (timeline + list column 280 dp, 320 dp from 900 dp) at ≥ 600 dp, since the PlanWide boards exist. Portrait-only is **not** enforced (Android 16 ignores orientation locks on large screens for targetSdk 36).
- **OQ-A9.13 Predictive back in the wizard.** RN disabled predictive back; the wizard must block back during Calculating (`plan.md:1331`). *Default:* predictive back on everywhere; `BackHandler(enabled = calculating)` swallows it during Calculating; × still asks "Discard this trip?" when something was entered.
