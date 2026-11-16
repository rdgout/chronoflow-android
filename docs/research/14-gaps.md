# 14 — Completeness review: gaps in research 01–13, and the research that fills them

Written 2026-11-15 by the completeness critic. Every source repo was only read; nothing in them was built,
run or modified.

Labels used throughout:

- **[F]** a fact checked in source for this document, with a path and line;
- **[P]** a platform or store-policy fact from official documentation (searched 2026-11-15; check it again in
  Play Console when submitting);
- **[R]** a recommendation for Android;
- **OQ-G*n*** an open question with the default the plan proceeds on (§16).

Path prefixes: `I/` = `/Users/rickgout/Sites/chronoflow-ios`, `K/` = `I/Packages/ChronoFlowKit/Sources`,
`W/` = `/Users/rickgout/Sites/chronoflow-new` (branch `feat/mobile-api-passkey-management`, `1f4a2684`),
`M/` = `/Users/rickgout/Sites/chronoflow-mobile` (`e24872a`), `C/` = `_design/canvas`, and `01`–`13` = the
sibling research files.

---

## 0. Answer first

**Does the iOS research and plan contain things that can be reused?** Yes. Files 01–13 show it in detail, and
this audit confirms their coverage is close to complete:

- **Design boards.** The 334 boards in `canvas.json` resolve to 74 base boards (small wrapper boards were
  resolved to the base they `<dc-import>`). 330 of them sit under a base that a research file already
  describes. Only three bases, holding 4 boards (`GuestHealth`, `PasswordPromptWide`, `PlanRemoteWide` and
  its offline wrapper), are never named; §1.2 maps them.
- **API.** Every endpoint the iOS app calls is already in `03` §5: rows #1–#39 all list an iOS caller, and
  iOS calls nothing outside them. No endpoint is missing.
- **iOS features.** All of them are covered. 117 of the 367 iOS Swift files are never named in 01–13. They
  are views, generated tokens or debug seeds whose behaviour the docs specify through the models. §1.3 fills
  in the one behaviour detail missing in that set: the recovery-code `.txt` download.

The real gaps are in **Android platform, store and operations** concerns. 01–13 either did not cover them or
**contradict each other** on them. This document fills those gaps:

| § | Gap | Status before | Filled with |
|---|---|---|---|
| 2 | Six cross-document contradictions: DB library, DI, wide-window policy, process death, exact alarms, applicationId | Conflicting defaults | One resolved default each |
| 3 | App-wide state survival (process death vs config change) | Per screen only, conflicting | Policy plus per-screen table |
| 4 | Window sizes, foldables, multi-window for every screen | Trip page and Settings only | Per-screen table |
| 5 | RTL and unsupported phone locales | Not addressed | Layout and format policy |
| 6 | Permissions and merged-manifest allowlist | Scattered | One manifest spec plus release checks |
| 7 | Background limits, boot, Direct Boot, battery | Mostly covered | Consolidated rules |
| 8 | Crash reporting, analytics, diagnostics | **Not addressed** | iOS facts and an Android default |
| 9 | Privacy and the Play Data safety form | **Not addressed** | Data inventory and answers |
| 10 | Play policy checklist (account deletion, Health apps declaration, App access, 16 KB, target audience, …) | **Mostly missing** | Checklist with one real blocker |
| 11 | Signing, applicationId, versioning, release tracks, AAB language splits, R8, size | **Mostly missing** | Release pipeline spec |
| 12 | Server work Android depends on | Scattered over 03/05/11 | One list |
| 13 | Accessibility beyond TalkBack (keyboard, Switch Access, scanner) | Partly | Additions |
| 14 | Test and device gaps | Partly | Additions |
| 15 | iOS features that do **not** exist, so Android must not add them | Implicit | Verified list |

---

## 1. Coverage audit

### 1.1 Method [F]

- **Boards.** `C/canvas.json` has 334 board entries. They resolve to 74 base boards: every board under
  3 KB is a wrapper `<dc-import name="Base" …props>`. I searched for each base and each wrapper name in
  01–13 with word boundaries. Four `.dc.html` files are not in `canvas.json`: `Wizard`, `WizardSheet` (the
  imported bases) and `WizardOutboundStopNew(+DarkNL)`. The note `wizStopsRoute` says "WizardOutboundStopNew
  is removed (that state no longer exists)", so that board must **not** be built.
- **Canvas notes.** 95 notes. 61 are never named by id, but every one of them is either a row label or a
  request marked "Done" whose content 05/06/07 already carry. I spot-checked these:
  - `gapSyncAge`: the "Synced just now" table is in `07`:216–221.
  - `gapTodayDone`: covered at `07`:102.
  - `wizStopsRoute`: the 16-flight line is at `06`:285.
  - `wizTodo3` #14 (practice mode): `06`:74 records it as not built.
- **iOS sources.** 367 Swift files under `I/App` and `K/`. 117 are never named in 01–13. I opened the ones
  that hold behaviour (§1.3).
- **API.** I extracted every `APIEndpoint(` from `K/DataAccess/API/APIDataSource*.swift` and compared it with
  `03` §5, table rows #1–#39. Everything matches. The only server routes iOS does not call are web-only (§12).

### 1.2 The three base boards no research file names

| Board (canvas title) | What it is [F] | iOS status [F] | Android [R] |
|---|---|---|---|
| `GuestHealth` ("Guest · 3 Health & age · superseded") | The old guest health step: "Bright light and caffeine timing aren't safe for everyone…" with health questions | Never built. It is superseded by WizardSpec, and the health questions were retired by `I/Docs/no-health-data.md` | Do not build. Only `age_band` and `shift_work` block (`06` §1). |
| `PasswordPromptWide` ("Prompt · wide (form sheet)", 669×951) | The shared password prompt (`PasswordPrompt`) drawn as a centred form sheet on a wide window. Same strings: cancel, title, desc, field, msg, forgot, button, plus the forgot alert. | **Not built.** No wide handling exists in `K/Settings/SettingsPasswordPrompt.swift`, and `I/plan.md` never names the board. | ≥600 dp: show the prompt as a centred `Dialog` with `usePlatformDefaultWidth=false`, width 560 dp, max height 90%. Below that, keep the phone treatment `05` §13 describes. |
| `PlanRemoteWide` (+`PlanRemoteWideOffline`) ("Plan downloading · wide" / "not available offline · wide") | The wide trip page when the plan is not on the phone: back, route title, dates, eyebrow, shift, home/there clocks, downloading line, offline title/body, Retry, "your other trips still work" | **Built** in Phase 15e (`I/plan.md:1765–1766`): "Downloading uses the wide lane/list skeleton; offline retains the known summary and full-width retry card." | Covered by `04` §3.16/§3.18 (the wide layout plus the remote states). Add both boards to the 04 snapshot list at 669 and 951 dp. |

`Set2FAWide` and `Set2FAManageWide` (560 pt content column) are named in `07`:74. Note **[F]** that iOS has
**not** built them either: no 560 column exists in `K/Settings`, and the only column token is
`PlanUITokens.wizardColumnWidth = 520` (`K/DesignSystem/PlanUITokens.generated.swift:71`), used by the wizard
and `GuestSignupView.swift:78`. See §4 for the policy.

The complete board index (74 bases, wrapper counts, which research file covers each) is in **Appendix A**.

### 1.3 iOS behaviour found only by this audit [F]

- **Recovery codes "Download".** `K/Settings/SettingsSecurityView.swift:257–262` offers a `ShareLink` with
  `SettingsRecoveryDocument` (`K/Settings/SettingsRecoveryDocument.swift`). It is plain text (codes joined by
  `\n`), file name `t("Recovery codes") + ".txt"`, and the button label is "Download". "Copy"
  (`:289–291`) writes `localOnly` with a 60 s expiry; `07`:371 already maps that.
  **[R]** "Download" becomes `ActivityResultContracts.CreateDocument("text/plain")` with the suggested name
  `Recovery codes.txt` (Dutch from the catalogue). Write UTF-8, no BOM, LF line endings. Fallback when no
  document provider exists: `Intent.ACTION_SEND` with `text/plain` through the share sheet. Do **not** use a
  `FileProvider` cache file, which would leave the codes on disk.
- **Haptics.** `UIImpactFeedbackGenerator`/`UISelection…`/`UINotification…` appear only in `K/TripWizardUI/*`
  (20 call sites), never in Today, the trip page or Settings. `08` §12 maps them. A check-off has **no**
  haptic on iOS, so Android must not add one.
- **Foreground notifications.** `I/App/AppDelegate.swift:39–42` presents `[.banner, .list]` while the app is
  open, with no sound option. `07`:501 covers this.
- **Clock observers.** `I/App/Session/ReminderTimeObservers.swift` reschedules on `significantTimeChange` and
  `NSSystemTimeZoneDidChange`. `07` §7.2 and `10` §8.2 cover this.

---

## 2. Cross-document contradictions, resolved

These defaults replace the conflicting ones in 01–13 for planning purposes.

| # | Topic | What the docs say | Resolved default and why |
|---|---|---|---|
| C1 | **Local DB library** | `02` §7.5 / OQ-7: Room. `09` §5.2: Room. `01` OQ-A4: SQLDelight. `11` OQ-2: SQLDelight 2.4.1. `06` §4.7 says "one Room row". | **SQLDelight 2.4.1 with the framework SQLite driver.** (1) The v1 DDL goes into `.sq` files **verbatim** (`02` §7.2), so parity with iOS/RN is a text diff. (2) The iOS write model holds an epoch check and a session permit *inside* a synchronous transaction (`02` §7.5). SQLDelight's `transaction {}` is blocking. `11` records Room 3.0.3 as coroutine-only, which makes `02`'s `runInTransaction` design unavailable without falling back to Room 2.x. (3) No native `.so`, which keeps 16 KB compliance trivial (§11.6). Read "Room row" in `06` as "a `drafts` table row". |
| C2 | **DI** | `02` §11.3 / OQ-13: manual `AppGraph` + per-epoch `SessionGraph`, no Hilt. `11` OQ-11: Hilt 2.60.1. | **Manual DI** (`02`). The per-epoch `SessionGraph` is the safety mechanism that ports iOS D23 ("an old 401 must never wipe a newly signed-in session"). Hilt has no natural scope for "one session epoch", and manual DI adds no annotation processing and no startup work. Konsist (from `11`) still guards the module boundaries. |
| C3 | **Wide windows** | `11` OQ-5: "a centred max-width phone column + rail" everywhere. `04` §3.16/§9.12 and `07`:83: trip page side by side from 600, Settings in a 560 column. | **Per-screen policy in §4.** `11` OQ-5 is superseded: iOS *built* a real wide trip page (Phase 15e). |
| C4 | **Process death** | `05`:999: "always the splash, no navigation restore (OQ 16)". `04`:1230: keep the timeline anchor in `SavedStateHandle` "(process death)". | **§3: follow iOS OQ 16** (`I/plan.md:2395`: "Cold start after a background kill: always the splash"). `SavedStateHandle` keeps state only across **configuration recreation**. After process death, saved navigation and anchor state is discarded on purpose. |
| C5 | **Reminder alarm exactness** | `02` OQ-17: "Exact when allowed, else ≤ 10 min window". `07` §7.2: `setAndAllowWhileIdle`, no `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`. `11` risk 9. | **`07` wins: inexact `setAndAllowWhileIdle`, no exact-alarm permission.** "When allowed" would require declaring `SCHEDULE_EXACT_ALARM`. That permission is denied by default on API 33+ and is a Play policy declaration for a non-alarm-clock app [P]. Reminders already tolerate drift: they expire at `min(end, start+20 min)` and are coalesced within 15 min (`07` §7.1). |
| C6 | **applicationId** | `02` OQ-12 and `01` OQ-A2: `net.chronoflow.app.native`. `11` OQ-9: native for debug, "release id decided at store time". | **`net.chronoflow.app.native` for every variant until the first Play upload**, plus a `.debug` suffix on the debug build type. Before the first upload, changing it costs nothing because no installed user data exists. After the upload, it is permanent. The final choice is OQ-G1 (§11.1). |
| C7 | Memory gate | `01`: growth ≤ 40 MiB (`I/plan.md:1733`). `13` §2.5: peak ≤ 150 MiB (`I/Docs/perf-timeline.md`). | **Gate on the 150 MiB peak** (as `13` does) **and report growth over the 15-fling run.** Growth over 40 MiB is a warning, not a failure. |

Not a contradiction, but flagged: `03`:366 and `07` OQ-12 add `FLAG_SECURE` to the recovery-code and 2FA
setup screens. iOS does **not** block screenshots there: `SettingsSecurityView.swift` has no
`privacySensitive` and no screen-capture handling. This is a deliberate Android-only hardening, recorded as
OQ-G2 (default: keep it).

---

## 3. App-wide state survival: config changes vs process death

### 3.1 Facts [F]

- iOS restores no UI state: no `SceneStorage`, `restorationIdentifier` or `stateRestoration` anywhere in
  `I/App` or `K/`. The only `NSUserActivity` is a synthetic browsing activity in `SceneDelegate.swift:526`
  for UI tests.
- iOS OQ 16 (`I/plan.md:2395`): after a background kill, the app always starts at the splash.
- The launch overlay plays only on the first scene connection of a process (D14, `02`:113). Reconnecting
  does not replay it (`05`:233).
- What survives a kill on iOS lives in the database:
  - the member/guest wizard recovery row (30 min; `06` §4.7);
  - the outbox;
  - the guest continuation (`K/AppCore/GuestSessionContinuation.swift`, consumed in `I/App/AppTabs.swift:341–369`);
  - the first-trip reminders offer (`K/Reminders/FirstTripRemindersOffer.swift`: the `wizardReminders`
    settings row moves `pending` → `seen` *before* the sheet shows, "so recovery and Not now cannot offer
    the sheet again").

### 3.2 Policy [R]

1. **Detect a process-death restore.** In `MainActivity.onCreate`, a restore has `savedInstanceState != null`
   while the process-level `AppGraph` is newly created (`AppGraph.processStartedAt` is after the bundle's
   saved stamp). In that case:
   - ignore the saved back stack;
   - play the cold launch: the system splash, then the overlay, then readiness, as on iOS;
   - let the DB-backed records (wizard recovery, guest continuation, outbox) do the resuming, exactly as on
     iOS.
2. **Configuration recreation** (locale, font scale, any config not handled in place, see 3.3) must look
   seamless:
   - back stacks via Navigation 3's saveable back stack (`rememberNavBackStack` with `@Serializable` keys);
   - ViewModels survive;
   - the trip page keeps `(dayId, offsetInDay)` in `SavedStateHandle` (`04`:1230);
   - open sheets are recorded as part of the route key, so they reopen;
   - the launch overlay is **not** replayed, because a process-level flag `launchOverlayPlayed` in `AppGraph`
     is already set (D14).
3. **Secrets never go into a saved state Bundle.** That means no password, 2FA code, recovery code, setup key
   or token. `05` §15.13 already says this for auth forms; it applies equally to the Settings password
   prompt, 2FA setup and the passkey name field. These fields use plain `remember { TextFieldState() }`.

### 3.3 Config changes handled in place [R]

`android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode"`
on `MainActivity`:

- **Size and orientation** (folds, rotation, split screen): handled in place. Recreating would drop frames
  on the timeline and risk the anchor (`04` risk 15). Compose re-lays out from `LocalConfiguration` and
  window size classes.
- **`uiMode`** (system dark mode): Compose recomposes. The in-app theme override (`07`:416) wins over it.
- **Not listed: `locale` and `fontScale`.** These recreate the activity, so `attachBaseContext` reapplies
  the configuration policy (§5.3: font-scale cap, locale coercion, LTR).

### 3.4 Per-screen survival table [R]

| Screen / state | Rotation, fold, split (in place) | Locale or font-scale recreation | Process death |
|---|---|---|---|
| Launch overlay | Re-lays out with the composition formula for the new w×h (`05` §3.2) | Not replayed | Replayed (cold launch) |
| Welcome intro | Stays settled | Intro not replayed (first Welcome per process only, `05` §4.2) | Cold launch |
| Auth forms | Field text kept (`remember`) | Text kept except password fields, which clear | Empty form at the root route |
| Wizard | ViewModel | ViewModel; step from the saved key | Recovery row (≤ 30 min) → resume, or the save resumes in Calculating with the same identity (`06` §4.7) |
| Trip page | Anchor kept and re-applied after re-layout (`04` §4.8) | Anchor from `SavedStateHandle`; sheet route reopens | Cold launch → Today |
| Step sheet / legend / trip actions | Stays open | Reopens from the route key | Gone |
| Settings subpage | Kept | Kept | Cold launch → Today tab root |
| Password prompt | Stays open, field kept | Reopens **empty** | Gone |
| 2FA setup (QR + key) | Kept (ViewModel memory only) | Kept (ViewModel) | Gone. The server keeps setup pending, and re-entering is idempotent (`03` #20) |
| Recovery codes just shown | Kept (ViewModel) | Kept (ViewModel) | Gone. "Show codes" asks for the password again (`03` #24) |
| Sign-out with pending changes sheet | Kept | Kept | Gone. The outbox still holds the changes, so nothing is lost |

---

## 4. Window sizes, foldables and multi-window

### 4.1 Facts

- **[F]** iOS:
  - iPhone only (`TARGETED_DEVICE_FAMILY: "1"`, `I/project.yml`), portrait only (`I/App/Info.plist`
    `UISupportedInterfaceOrientations` = portrait).
  - Wide layouts appear only on a foldable iPhone's inner screen (669 and 951 pt) and only for the trip page
    (`I/plan.md:1765–1766`).
  - The wizard and guest sign-up use a 520 pt column.
  - Every other screen stretches to the full width.
- **[P]** Apps targeting API 36 have orientation and resizability restrictions ignored on displays with
  smallest width ≥ 600 dp (`11`:164, `07`:83). Split screen can give the app windows narrower than any phone
  (≈ 300–360 dp), and very short ones in landscape.

### 4.2 Per-screen policy [R] (breakpoint: window width 600 dp, as iOS 600 pt)

| Screen | < 600 dp | ≥ 600 dp | Board |
|---|---|---|---|
| Launch / Welcome | Formula for any w×h (`05` §3.2) | Same formula; Welcome copy card max 560 dp, centred | none (formula) |
| Walkthrough, Terms update, auth screens, guest sign-up | Phone layout | Content column max 560 dp, centred (guest sign-up 520 like iOS) | none, except `GuestSignup` uses the iOS 520 |
| Shell | `NavigationBar` | `NavigationRail` (`07` §2.2) | `dirNative` |
| Today, My trips | Phone | Full width, cards max 640 dp centred | none |
| Trip page | Phone timeline/list toggle | Side by side: list 280 dp → 320 dp from 900 dp; no view toggle (`04` §3.16) | `PlanWide*`, `PlanRemoteWide*` |
| Wizard | Phone | Column max 520 dp, centred from 669 dp (`06`:995) | WizardCopy layout |
| Settings home and subpages | Phone | Column max 560 dp (`07` OQ-11) | `Set2FAWide`, `Set2FAManageWide` |
| Password prompt | Phone treatment | 560 dp dialog (§1.2) | `PasswordPromptWide` |
| Bottom sheets (step, legend, trip actions, wizard sheets) | `ModalBottomSheet` | `ModalBottomSheet` with `sheetMaxWidth = 640.dp` (the M3 default is 640) | — |

Further rules [R]:

- **Height-constrained windows** (landscape phone in split screen, under 480 dp tall):
  - Every screen scrolls vertically. The wizard's "no scrolling" rule (`06` §9.9) applies only when it fits;
    otherwise the content scrolls and the primary button stays fixed.
  - The launch overlay formula still holds, because it is defined for any w×h.
- **Minimum width.** Lay out correctly down to **280 dp** wide. Narrower than the timeline's minimum lane
  widths (252–268 pt content, `04`:269), the timeline day scrolls horizontally, as on iOS.
- **Phones stay portrait.** Set `screenOrientation="portrait"` (iOS D16). It is honoured below sw600 and
  ignored on large screens. No `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` opt-out (`11`:164).
- **No posture-specific UI.** Tabletop and book postures, and hinge avoidance, are not used: iOS has no
  equivalent. A fold behaves like a width change. The only fold requirements are the anchor and sheet
  survival from §3.3.
- **Not targeted:** desktop windowing and ChromeOS freeform. They work through the same size classes but get
  no dedicated testing (OQ-G3).

---

## 5. RTL, unsupported locales and the configuration policy

### 5.1 Facts

- **[F]** The app ships English and Dutch only. With the `system` choice, any phone language except `nl`
  resolves to English (`10` §7; `K/Settings/SettingsDevicePreferences.swift`).
- **[F]** iOS layout direction follows the app's resolved localisation. With only en and nl bundled, an
  Arabic or Hebrew phone gets the **English, left-to-right** app.
- **[P]** Android derives layout direction from the configuration locale. With `android:supportsRtl="true"`,
  an Arabic phone mirrors the layout while showing English strings. Locale-sensitive formatting through
  `String.format`/`stringResource` with the configuration locale can also produce non-Latin digits for some
  locales. Neither 01–13 addresses this; `10` §8.2 fixes only the plan formatter's locale.

### 5.2 Default [R]

- `android:supportsRtl="false"` in the manifest. It covers platform views and dialogs.
- At the Compose root: `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`.
- **Coerce the configuration locale** to the resolved app language (`en` or `nl`, keeping the phone's region
  when it is one of ours) whenever the choice is `system` and the phone's language is neither. Do it in
  `attachBaseContext` (§5.3). Then `stringResource`, plurals, `NumberFormat` and Material date/time pickers
  all format in en or nl, as iOS does.
- The 12/24-hour setting is independent of locale on Android (`DateFormat.is24HourFormat(context)` reads
  `Settings.System.TIME_12_24`). The phone hour cycle is therefore kept automatically, matching
  `resolvedLocale` on iOS (`10` §7).
- Test: one Robolectric test with qualifiers `ar-rEG` and one with `de-rDE`. Assert LTR, English strings
  and Latin digits in a plan line and a Settings date.

### 5.3 One `ConfigurationPolicy`, applied in `Application` and `MainActivity.attachBaseContext` [R]

It sets three things:

1. `fontScale = min(system, 1.3f)`: the iOS xxLarge cap (`05` §15.9, `08` §5.4);
2. the locale, coerced as in §5.2;
3. `setLayoutDirection(Locale.ENGLISH)`.

The explicit language choice is stored as `LanguageChoice` in DataStore (`10` OQ-L5) and mirrored to
`LocaleManager.applicationLocales` on API 33+, so the system per-app language screen stays in sync (`10`
§9.9).

`10` §9.9 uses `AppCompatDelegate.setApplicationLocales`. On API < 33 that requires `MainActivity` to extend
`AppCompatActivity`, and its own `attachBaseContext2` wrapper then competes with this override. **Default:**
apply the stored choice through this policy on every API level, and use `LocaleManager` only on 33+, which
removes the AppCompat dependency. Spike this in phase 0 (OQ-G4). If the override misbehaves, fall back to
capping font scale through the Compose root `LocalDensity`.

---

## 6. Permissions and the merged manifest

### 6.1 Allowed permissions (release) [R]

| Permission | Why | Source |
|---|---|---|
| `INTERNET` | API | — |
| `ACCESS_NETWORK_STATE` | `ConnectivityManager` callbacks (NWPathMonitor parity, `I/App/Session/NetworkPathMonitor.swift`) | `02` §6.6 |
| `POST_NOTIFICATIONS` | Reminders; asked only from Ask or the first-trip sheet | `07` §7.3 |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after reboot | `07` §7.2 |
| `WAKE_LOCK` | Only if WorkManager merges it (it does) | merged |

**Must not appear:** `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` (C5), any `FOREGROUND_SERVICE*`, calendar
permissions (calendar export is deferred, `07` §8), `CAMERA` (the QR code is shown, never scanned),
`READ_PHONE_STATE`, `AD_ID` (no ads SDK; add `tools:node="remove"` in case a library merges it), Firebase and
c2dm permissions, and launcher-badge permissions. RN pulled in the last two through `expo-notifications`
(`09` §2.2).

### 6.2 Manifest attributes [R]

- `android:allowBackup="false"` (`02` OQ-6). Also a `dataExtractionRules` that excludes everything, as a
  second layer for device-to-device transfer.
- `android:usesCleartextTraffic="false"`. Debug may add a `network_security_config` that allows only
  `10.0.2.2` for a local server.
- `android:supportsRtl="false"` (§5).
- `android:enableOnBackInvokedCallback="true"` (`07` §2.2).
- `android:localeConfig` generated (`10` §9.9).
- `MainActivity`: `launchMode="singleTask"` (App Links with `onNewIntent`, `05` §15.6), `exported=true`, the
  `configChanges` from §3.3, `screenOrientation="portrait"`, `windowSoftInputMode="adjustResize"`.
- No `directBootAware` anywhere (§7).
- No custom URL scheme in release (iOS D15; `13` §4.8).

### 6.3 Extend `verifyReleaseManifest` (`13` §4.8) [R]

Besides the scheme and debuggable checks already listed, it fails on:

- any permission outside §6.1;
- `allowBackup=true`;
- `usesCleartextTraffic=true`;
- any `directBootAware=true`;
- an exported component other than `MainActivity`, the boot/time receivers (exported, with system-only
  intent actions) and the WorkManager and AndroidX startup internals.

This mirrors iOS `ReleaseReadinessTests.testReleaseIgnoresUITestSeedsAndHasNoTestURLRegistration`
(`I/ReleaseReadinessTests/ReleaseReadinessTests.swift:30`).

---

## 7. Background execution, battery, boot

- **[F]** iOS has no background modes. `Info.plist` has no `UIBackgroundModes`, there is no
  `BGTaskScheduler` and no remote notifications (a grep of `I/App` and `K/` for them finds nothing). Sync runs
  only while the scene is active (`I/App/SceneDelegate.swift:697–709`, `setActive`).
- **[R]** Android parity:
  - Sync is driven by `ProcessLifecycleOwner` ON_START/ON_STOP, with no WorkManager sync (`02` OQ-5).
  - The only background work is reminder upkeep (`07` §7.2): alarms plus a 12 h periodic refill job.
  - No foreground service.
  - App Standby buckets can delay the periodic job for rarely used apps. That is acceptable, because the
    48 h window is also refilled on every app start and on every sync.
- **Boot and Direct Boot [R].** Do not declare `directBootAware`.
  - Alarms are cleared on reboot. `BOOT_COMPLETED` arrives only after the first unlock [P], so reminders
    due between reboot and the first unlock are lost.
  - iOS has the same limit in practice: the account DB uses file protection, and readiness retries on
    `protectedDataDidBecomeAvailable` (`I/App/SceneDelegate.swift:373–375`). Accept it and record it as a
    known difference.
- **Doze [P].** `setAndAllowWhileIdle` alarms fire at most about once every 9 minutes per app while idle.
  The 15-minute coalescing (`07` §7.1) keeps reminders within that limit.
- **Battery [R].**
  - No wake locks of our own.
  - The minute ticker for the now line runs only while the trip page or Today is started (`04` §9.15).
  - The connectivity callback is registered only while started.

---

## 8. Crash reporting, analytics and diagnostics

### 8.1 Facts [F]

- iOS has **no** analytics, crash or telemetry SDK. `K/../Package.swift:25–28` declares only two
  dependencies: `swift-snapshot-testing` (from 1.19.0) and `GRDB.swift` (exact 7.8.0). A search of `I/App`
  and `K/` for Sentry, Crashlytics, Firebase, MetricKit, TelemetryDeck, Bugsnag, Amplitude, Mixpanel and
  PostHog finds nothing.
- Logging is `OSSignposter` launch intervals only:
  - intervals `launch`, `first-commit`, `session`, `ready`, `exit`, `intro`;
  - events `scene-connect`, `overlay-first-commit`, `root-first-layout`, `overlay-removed`,
    `font-registration-failed`;
  - all in `K/AppCore/LaunchSignposts.swift`, plus debug-only `Logger` use in `I/App/Debug/*`.
- MetricKit is listed only as "tracked after release … (later)" (`I/plan.md:2303`).
- The RN app pulled in Firebase Messaging but used no crash SDK (`09`:37).

### 8.2 Android default [R] (OQ-G5)

- **No third-party SDK in v1**, for parity and to keep the Data safety form free of diagnostics data.
  Crashes, ANRs, startup time, slow and frozen frames and excessive wake-ups come from **Play Console
  Android vitals**. Those are collected by Google Play services, need no SDK, and are not a developer data
  collection to declare.
- [P] Android vitals "bad behaviour" thresholds to stay under:
  - user-perceived crash rate 1.09%;
  - user-perceived ANR rate 0.47% overall;
  - 8% on any single phone model.

  Exceeding them reduces store visibility. Check the exact values in the Console.
- **Trace sections** with the same names as the iOS signposts:
  - `Trace.beginAsyncSection("launch")` and so on through `androidx.tracing`, plus `cf.timeline.firstFrame`;
  - Macrobenchmark `TraceSectionMetric` reads them (`02`:839, `13` §4.5);
  - they cost nothing in release when no tracer is attached.
- **Debug builds only:**
  - `StrictMode.ThreadPolicy.detectAll().penaltyLog()` plus `penaltyDeath` on disk reads in the
    launch path. This enforces the iOS rule "the readiness store performs no synchronous I/O on the main
    actor" (`I/plan.md:780`).
  - `VmPolicy.detectLeakedClosableObjects()`.
  - On API 30+, log the last `ApplicationExitInfo` at startup.
- **Release logging.** R8 strips `Log.v/d/i` with `-assumenosideeffects`. Never log tokens, emails, guest
  secrets, codes or plan contents.

---

## 9. Privacy: data inventory and the Play Data safety form

### 9.1 What the app sends and stores [F, from `03` §5 and §13 and `02` §4.9]

| Data | Where it comes from | Sent to server | Stored on device |
|---|---|---|---|
| Name, email, password | Sign-up / sign-in / profile | Yes (`auth/register`, `login`, `users/me`) | Name and email in the account DB; the password never |
| Account id (UUID) | Server | — | Account DB, Keystore-sealed session `{token, accountId}` |
| Device id (UUIDv7, app-generated) + device model name (≤ 255 chars) | `DeviceIdentityStore` (`I/plan.md:52`, D22) | Yes, on every login/register | Device preferences (no backup) |
| Body clock: bed/wake times, free-day times, chronotype, `age_band`, `shift_work`, caffeine habit and cups, caffeine sensitivity, light device, blue blockers (`03` §13.2) | Wizard Sleep step, Settings → Body clock | Yes (`PATCH account/body-clock`, review `profile`) | Account DB settings |
| Trips: airports, flight times, stops, stays, answers, check-offs | Wizard, trip page | Yes | Account DB |
| Preferences, reminder settings, language, theme | Settings | Preferences and reminders yes. Language and theme no (device only, `07`). | DB / DataStore |
| Credits and subscription status | Server (`GET account`) | — (read only) | Account DB |
| Passkeys | Credential Manager | Public-key credential only | Never the private key |

Not collected: location (airports are typed, never derived from GPS), contacts, photos, calendar, audio,
messages, financial details (billing is web-only, `03` §14), advertising ID, analytics or crash logs (§8).
The retired health keys (bipolar, eye disease, pregnancy, VTE, …) are not asked and are ignored by PATCH
(`03` §13.2; `I/Docs/no-health-data.md`).

### 9.2 Data safety answers [R] (OQ-G6: one shared answer set with the iOS privacy label, which is still undecided, `I/plan.md:2424`)

| Play category → type | Collected | Shared | Optional | Purposes |
|---|---|---|---|---|
| Personal info → Name | Yes | No | No | App functionality, Account management |
| Personal info → Email address | Yes | No | No | App functionality, Account management |
| Personal info → User IDs | Yes | No | No | App functionality, Account management |
| Personal info → Other info (age band) | Yes | No | No (it blocks plans for under-18s) | App functionality |
| Health and fitness → Health info (sleep schedule, shift work, caffeine habits) | **Yes (conservative)** | No | No | App functionality |
| App activity → Other user-generated content (trips, check-offs) | Yes | No | No | App functionality |
| Device or other IDs (app-generated device id) | Yes | No | No | App functionality, Fraud prevention / security (one token per device) |

Security section:

- Data is encrypted in transit: yes (HTTPS only, §6.2).
- Users can request deletion: yes. In the app: Settings → Account → Delete account (`03` #25). On the web:
  see §10.1.
- Committed to the Families policy: no (§10).

Privacy policy URL: `https://chronoflow.net/privacy`. Dutch is at `/nl/privacy` (`W/routes/www.php`). iOS
links the English URL in both languages (`K/Auth/SignUpModel.swift:134–137`), and Android keeps that parity.

Encryption at rest:

- **[P]** File-based encryption is mandatory on devices that launch with Android 10 or later, which matches
  minSdk 29 (`11` OQ-1). The account DB in credential-encrypted storage therefore matches iOS file protection
  (`02` §7.4).
- **[R]** No SQLCipher (parity with iOS, which has no custom encryption).

---

## 10. Play policy checklist

| # | Requirement | Status / what to do |
|---|---|---|
| P1 | **Target API**: targetSdk 36 required since 2026-08-31 (`11` §3.1) [P] | targetSdk 36, compileSdk 37 (`11`) |
| P2 | **16 KB memory pages**: new apps and updates targeting Android 15+ must support 16 KB pages [P] | With C1 (framework SQLite) and no NDK code, the app ships **no `.so`**. Assert "no native libs" in `verifyReleaseManifest`'s sibling bundle check. If a native lib is ever added, check with `zipalign -c -P 16`. |
| P3 | **Account deletion**: an app that creates accounts must offer deletion in the app **and** a web link in the Data safety form [P] | In the app: Settings → Delete account. Web: `https://app.chronoflow.net/settings/account` (`W/routes/settings.php:18`, page with `DELETE settings/profile`, `:26`). **Gap [F]:** both deletion routes require a **verified** email: API `Route::delete('account', …)->middleware(['verified', …])` at `W/routes/api.php:93–94`; web `DELETE settings/profile` sits in the `['auth','verified']` group (`W/routes/settings.php:22–26`). An account that never verifies cannot be deleted by its owner, and the unverified gate offers only "Use a different email" (sign-out). **OQ-G7** default: ask the server to drop `verified` from both delete routes, and add "Delete account" to the Verify screen's secondary actions on Android **only if** the server ships it. Until then, list `support@` deletion on the web page. |
| P4 | **Health apps declaration**: every app must complete it, even with no health features; required since 2024-08-31 [P] | Declare a sleep/wellness feature (the closest listed category, e.g. sleep management). The app is **not** a medical device. No Health Connect use. Wording must stay within the "no health data" decision. |
| P5 | **App access**: reviewers need credentials when the app is behind login [P] | A production demo account with 2FA **off**, a verified email, accepted terms, at least one active trip and credits. The iOS mock demo user exists only in the mock (`13` §3.3). **OQ-G8**: who creates it on production. |
| P6 | **Target audience and content** | Target audience **18+**: the server excludes `under_18` from plans (`06` §1), which keeps the app out of the Families policy. Content rating: IARC questionnaire, all "no". Ads declaration: **no ads**. |
| P7 | **Payments** | No Play Billing and no purchase or link-out in the app. Show credits read-only and the `SetPlanNoLink` variant (`07` §6.7, `03` §14). The wizard's no-credits state shows Close only (`06` §12 default). [P] US external-link programmes exist after 2025, but default to no link (OQ-G9). |
| P8 | **Exact alarms / notifications** | No exact-alarm permission (C5). `POST_NOTIFICATIONS` asked in context (`07` §7.3). |
| P9 | **Data safety** | §9.2 |
| P10 | **Credential Manager / passkeys** | No policy form. Needs the server `assetlinks.json` (§12). |
| P11 | **Pre-launch report** | Runs Robo on Firebase Test Lab devices. With App access credentials it signs in. Read its accessibility and stability findings before every promotion (§14). |

---

## 11. Signing, identity, versioning and release pipeline

### 11.1 applicationId (OQ-G1)

- **[F]**
  - iOS uses `net.chronoflow.app.native` (`I/project.yml`, D15); it is unconfirmed for the App Store (iOS
    OQ 4).
  - RN uses `net.chronoflow.app` on both platforms (`M/app.config.ts:59,62`). Its Android release is signed
    with the **debug** keystore (`M/android/app/build.gradle:100–115`).
  - No sign of a store submission exists in RN: no `eas.json`, no submit config.
  - The AASA already lists both iOS ids (`I/plan.md:455`).
- **[R]** Build as `net.chronoflow.app.native` (debug `…native.debug`) until the Play listing is created.
  Default at that point: **`net.chronoflow.app`**, if the user confirms the RN app was never published to
  Play. It is the canonical product id and keeps one id across RN, web and Android. Changing the id later
  forces new assetlinks fingerprints and package names. It also changes the passkey `android:apk-key-hash`
  origin.

### 11.2 Keys [R]

- **Play App Signing** (mandatory for AAB). Google holds the app signing key. We hold an **upload key**:
  - a PKCS12 keystore outside the repo, path and passwords in `~/.gradle/gradle.properties` (`CF_UPLOAD_*`)
    or the environment;
  - CI reads secrets from the environment;
  - the release `signingConfig` is wired only when those properties exist, so `assembleRelease` on a fresh
    clone builds unsigned instead of failing.
- **SHA-256 fingerprints needed by the server (§12):**
  - the **app signing key** (from Play Console → App integrity), for production assetlinks and the passkey
    origin;
  - the **upload key**, only if sideloaded release builds must verify links;
  - the internal app sharing key, if that channel is used.
- **Debug key.** Commit a team debug keystore (`app/debug.keystore`, as RN did) so every machine produces the
  same debug signature and the debug package `…native.debug` is stable. **Never** put the debug key's
  fingerprint or `apk-key-hash` into the **production** `assetlinks.json` or `passkeys.allowed_origins`. A
  committed key is effectively public: anyone could sign an app that Credential Manager would treat as ours
  for the `app.chronoflow.net` relying party. Use it only on a staging or local server (OQ-G10).

### 11.3 Versions [R]

- **[F]** iOS: `MARKETING_VERSION: "0.1.0"`, `CURRENT_PROJECT_VERSION: "1"` (`I/project.yml`).
- `versionName` mirrors the iOS marketing version (`0.1.0`).
- `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build` (build 0–99), computed in
  `build-logic` from one `version.properties`. It increases strictly for every upload.

### 11.4 Bundle and language splits [P → R]

- **Facts.**
  - Play splits an AAB's resources by language by default.
  - A user whose phone is English and who picks Dutch **in the app** would get no `values-nl` strings,
    because that split is not installed.
  - 01–13 do not mention this, and the in-app language picker (`07`, `10` §9.9) depends on it.
- **Fix:** `android { bundle { language { enableSplit = false } } }`. The cost is negligible: two languages,
  about 959 strings each (`10`).
- Keep the density and ABI splits on. With no native libs, the ABI split is moot.

### 11.5 Release tracks [P → R] (OQ-G11)

| Track | Use |
|---|---|
| Internal testing (≤ 100 testers, minutes to publish) | Every merge to the release branch; the developer's own phones |
| Closed testing | **Required before production for personal developer accounts created after 2023-11-13: at least 12 testers opted in for the 14 continuous days before applying for production access.** Organisation accounts are exempt [P]. The account type is unknown: OQ-G11. |
| Open testing | Not used (it does not count toward the 12-tester rule [P]) |
| Production | Staged rollout 10% → 50% → 100%, with a halt rule on the vitals thresholds (§8.2) |

### 11.6 Build hygiene [R]

- **R8.** Use AGP 9.3+ `optimization { enable = true }` (`11` §2). Keep rules:
  - kotlinx.serialization ships its own;
  - our `@Serializable` Navigation 3 keys need `-keep` only if accessed reflectively (avoid that);
  - `-assumenosideeffects` for `android.util.Log` v/d/i.
- **Baseline and startup profiles** (`11` §7, `13`). Ship both in release.
- **Size budget.**
  - Fonts: 10 TTFs, ≈ 598 KB total ([F] `K/DesignSystem/Resources/Fonts/*.ttf`).
  - The 680 KB mock seed ships only in the `mock` flavor (`02`).
  - Plan copy JSON ≈ 142 KB (`10`).
  - Target: AAB download size ≤ 8 MB. Gate: fail CI above 12 MB (`bundletool get-size total`). RN's
    universal APK was 125 MB (`09`).

---

## 12. Server work Android depends on (consolidated from 03 §9.4, 05 §15.6–15.7 and A16, 11 OQ-8)

| # | Change in `chronoflow-new` | Needed for | Without it |
|---|---|---|---|
| S1 | Serve `https://app.chronoflow.net/.well-known/assetlinks.json` with statements `delegate_permission/common.handle_all_urls` **and** `delegate_permission/common.get_login_creds` for the Android package and the app-signing SHA-256 (§11.2). **[F]** Today only the AASA (webcredentials) and `/.well-known/passkey-endpoints` exist (`W/routes/settings.php:48–53`). | App Links (reset, verify) and passkeys | Links open the web app, which works (the reset page exists). Passkeys are unavailable, so hide the passkey button and the Security → Passkeys rows when Credential Manager reports no provider or an RP mismatch. |
| S2 | Add `android:apk-key-hash:<base64url(sha256(cert))>` to `passkeys.allowed_origins`. `CheckAllowedOrigins` already accepts that form (`05` §0). | Passkey sign-in and registration | As S1 |
| S3 | Merge `feat/mobile-api-passkey-management` (`03` §0; routes #26–#29) | Passkey management | Security → Passkeys hidden (iOS behaves the same before the merge) |
| S4 | Drop `verified` from `DELETE /v1/account` and the web delete route (§10 P3) | Play account-deletion policy for unverified accounts | Review risk only |
| S5 | A production demo account (§10 P5) | Play review | Review rejection |
| S6 | (iOS too) AASA `applinks` entry | iOS universal links | — |

Nothing else is needed. Plan generation, sync, check-offs and settings are all on the existing `/v1` API
(`03`).

---

## 13. Accessibility beyond TalkBack

01–13 cover TalkBack, live regions, `paneTitle`, the font-scale cap, Reduce Motion and the timeline semantics
overlay (`04` §5, §9.9; `05` §15.11; `08` §12). Additions [R]:

- **Hardware keyboard and D-pad** (tablets, foldables with a keyboard, ChromeOS). Every interactive element
  must be reachable with Tab and arrows in reading order:
  - The timeline's semantics overlay nodes must be focusable, in the same order TalkBack uses.
  - Enter/Space activates.
  - Esc closes sheets and dialogs, as back does.
  - The wizard's 250 ms auto-advance must not fire when focus moves with the keyboard, only on activation.
- **Switch Access.** It is covered automatically if the keyboard rules hold, because it uses the same
  focusable/clickable semantics. Add one smoke pass to the release checklist.
- **Accessibility Scanner / pre-launch report.** Expect warnings for:
  - text size, from the 1.3 cap: an intended difference that mirrors iOS xxLarge;
  - contrast on the Welcome eyebrow (still open on iOS, `05` §4.5).

  Do not "fix" either away from iOS without a design decision.
- **Touch targets.** 48 dp (`04`, `08`). The iOS minimum is 44 pt.
- **Text magnification.** Android 14+ scales non-linearly up to 200%. The cap at 1.3 means non-linear
  scaling never kicks in beyond 1.3. Test at system 1.0, 1.3 and 2.0 (the 2.0 run should look identical to
  1.3).

---

## 14. Test and device gaps

- **No physical Android device** is known on this Mac (`11` §10.1, OQ-6). Hitches, memory and the RN
  comparison on Android were never measured on hardware (`04`, `09`, `12`).

  Default: the emulator gates stay functional plus trend. Before the first production rollout:
  - one closed-testing build with a JankStats summary written to a debug-only screen (internal track only);
  - Android vitals slow-frame data after release.

  Both feed `04` §10.
- **Profiles to add to `13` §4.4's `profiles.json`:**
  - `split-narrow`: 320×700 dp;
  - `landscape-short`: 800×360 dp;
  - `locale-ar` (§5);
  - `fontscale-2.0` (§13).
- **Fold transitions:** the AVD `pixel_9_pro_fold` is installable (`11` §10.1). Use it for the §3.3 in-place
  fold test, with the anchor and an open step sheet (iOS did the same with the Duo hinge, `I/plan.md:1777`).
- **Process death test:** `adb shell am kill net.chronoflow.app.native.debug` while the app is in the
  background, then relaunch from recents. Assert a cold launch (overlay shown), the Today root, and wizard
  recovery when it is within 30 min (§3).
- **Release-variant checks:** §6.3 plus §11.4 (an `nl` string resolves in a bundle installed on an `en` phone:
  `bundletool build-apks --local-testing`, install, switch the language in the app).

---

## 15. iOS features that do not exist, so Android must not add them [F]

Verified absent in `I/App`, `K/` and `I/project.yml`:

- **No app extensions.** Targets are only `ChronoFlow`, `AppTests`, `ReleaseReadinessTests`, `RenderTests`,
  `AppUITests` and `PerfUITests`. That rules out:
  - widgets (Android: no Glance widgets);
  - Live Activity (the board exists but was not built; `07` §9 defers it);
  - share or intents extensions.
- **No** home-screen quick actions (`UIApplicationShortcutItems`). Android: no app shortcuts.
- **No** Spotlight, App Intents, Siri or Handoff.
- **No** in-app review prompt (`SKStoreReviewController`/`requestReview`). Android: no Play In-App Review.
- **No** remote push. Android: no FCM.
- **No** analytics or crash SDK (§8).
- **No** background modes (§7).
- **No** in-app update prompt. Android: no Play In-App Updates in v1 (OQ-G12).
- **No** notification actions or categories: reminders are tap-to-open only
  (`K/Reminders/SystemReminderCenter.swift` asks only `[.alert, .sound]`). Android: no "Mark as done" action
  on the notification.
- **No** certificate pinning. Android: none either.

Built on iOS but easy to forget on Android:

- the passkey management page (Phase 20);
- the wide trip page (15e);
- reminder taps routed to the trip page with `blockID`, `notificationInputHash` and an account check
  (`I/App/AppTabs.swift:370–383`);
- the guest continuation after a claim (`AppTabs.swift:341–369`);
- the first-trip reminders offer consumed before presentation (§3.1).

---

## 16. Open questions (each with the default the plan proceeds on)

| # | Question | Default |
|---|---|---|
| OQ-G1 | Final applicationId at store setup | Build as `net.chronoflow.app.native` now. Switch to `net.chronoflow.app` before the first Play upload if RN never shipped on Play. |
| OQ-G2 | `FLAG_SECURE` on recovery codes and 2FA setup (iOS has none) | Yes (Android-only hardening; already `07` OQ-12) |
| OQ-G3 | Desktop windowing and ChromeOS as targets | Not targeted. They work through window size classes, with no dedicated tests. |
| OQ-G4 | Locale and font-scale override in `attachBaseContext` vs AppCompat | One `ConfigurationPolicy` plus `LocaleManager` on 33+, no AppCompat. Phase-0 spike. Fallback: cap font scale through Compose `LocalDensity`. |
| OQ-G5 | Crash reporting SDK | None. Use Play vitals, trace sections and debug StrictMode. |
| OQ-G6 | Data safety: declare sleep and caffeine answers as Health info? | Yes (conservative). Use the same answers for the iOS privacy label. |
| OQ-G7 | Unverified accounts cannot be deleted (server requires `verified`) | Ask the server for S4. No Android UI change until it ships. Web page lists a support-email path. |
| OQ-G8 | Who creates the production demo account for store review | The user / server owner. The plan lists it as a release prerequisite. |
| OQ-G9 | External purchase link where Play allows it | No link (NoLink variant), parity with iOS |
| OQ-G10 | Debug-key fingerprint in production assetlinks / allowed_origins | Never. Staging/local server only. |
| OQ-G11 | Play developer account type (personal → 12 testers × 14 days) | Plan a 14-day closed test before production in the release phase |
| OQ-G12 | Play In-App Updates / In-App Review | Neither (iOS has neither) |
| OQ-G13 | Language splits | `enableSplit = false` |
| OQ-G14 | RTL | Forced LTR plus locale coercion (§5) |

---

## 17. Risks

1. **Play account-deletion review (P3).** It could block the first production release if a reviewer creates
   an account and does not verify it. Mitigation: S4 plus the web-page text.
2. **Closed-testing rule (P5/§11.5).** It adds at least 14 days to the first launch on a personal account.
   Start the closed test as soon as the first feature-complete internal build exists.
3. **The language split bug (§11.4)** only shows in Play-delivered installs, never in Gradle installs. Without
   the bundletool local-testing check, it would ship unnoticed.
4. **Forced LTR plus locale coercion (§5)** touches the same `attachBaseContext` hook as the font-scale cap.
   A mistake there affects every screen. Mitigation: the Robolectric locale tests and the phase-0 spike.
5. **Passkeys and App Links depend on S1/S2** and on the final applicationId and signing key. Doing these in
   the wrong order (assetlinks published before the id is final) means redoing the server change.
6. **Process-death policy (§3)** deliberately discards Android's restored back stack. Android reviewers or
   users may see this as unusual, but it is the iOS behaviour (OQ 16). The wizard's 30-minute recovery keeps
   the main data-loss case covered.
7. **Health apps declaration wording (P4).** Overstating health features invites medical-device scrutiny.
   Understating them risks a policy strike. Keep the declaration consistent with the "no health data"
   decision and the web's science/see-a-doctor pages.

---

## Appendix A — Base board index (74 bases from 334 `canvas.json` entries) [F]

"Boards" counts the base plus every wrapper that imports it. "Docs" lists the research files that name the
base. Superseded boards are described in `06` §1 / `05` §2.2 and must not be built. `WizardOutboundStopNew`
is not in `canvas.json` and is removed.

| Base board | Boards (incl. wrappers) | Canvas page | Superseded? | Research docs naming it |
|---|---|---|---|---|
| `AppIcon` | 1 | core |  | 08 |
| `AuthSignIn` | 2 | more |  | 01, 05, 08 |
| `AuthSignUp` | 1 | more |  | 05 |
| `AuthStates` | 1 | more |  | 01, 05, 08 |
| `AuthTwoFactor` | 8 | more |  | 05 |
| `CalendarExport` | 4 | more |  | 07, 08 |
| `EditorStates` | 1 | trip | yes | 06 |
| `ErrorNotFound` | 2 | more |  | 05, 08, 09 |
| `GuestExcluded` | 1 | trip |  | 05, 08 |
| `GuestFlight` | 1 | trip | yes | 05, 06 |
| `GuestHealth` | 1 | trip | yes | **none → §1.2** |
| `GuestReview` | 1 | trip | yes | 06 |
| `GuestSignup` | 4 | trip |  | 05, 06, 08 |
| `GuestSleep` | 1 | trip | yes | 06 |
| `GuestStates` | 1 | trip | yes | 05, 08 |
| `ItemDetail` | 1 | more |  | 04, 07, 08 |
| `LiveActivity` | 1 | core |  | 07, 08 |
| `Main` | 1 | core |  | 01, 02, 04, 05, 06, 07, 08 |
| `PasswordPrompt` | 10 | settings |  | 05, 07, 08 |
| `PasswordPromptWide` | 1 | settings |  | **none → §1.2** |
| `PickerDateTime` | 1 | more |  | 06, 08 |
| `PickerTime` | 1 | more |  | 06, 08 |
| `Plan` | 13 | core |  | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13 |
| `PlanLegend` | 1 | core |  | 04, 08 |
| `PlanRemote` | 4 | core |  | 08 |
| `PlanRemoteWide` | 2 | core |  | **none → §1.2** |
| `PlanWide` | 4 | core |  | 01, 04, 08, 09 |
| `PolicyReader` | 1 | more |  | 05, 07, 08 |
| `ReviewAnswer` | 1 | trip | yes | 06, 08 |
| `ReviewStates` | 1 | trip | yes | 06, 08 |
| `Set2FA` | 2 | settings |  | 07 |
| `Set2FAManage` | 2 | settings |  | 07 |
| `Set2FAManageWide` | 1 | settings |  | 07 |
| `Set2FAOn` | 2 | settings |  | 07 |
| `Set2FASetup` | 1 | settings |  | 07, 08 |
| `Set2FAWide` | 1 | settings |  | 07 |
| `SetAccount` | 1 | settings |  | 07 |
| `SetBodyClock` | 2 | settings |  | 01, 07, 13 |
| `SetCancel` | 1 | settings |  | 07 |
| `SetData` | 3 | settings |  | 07 |
| `SetDelete` | 1 | settings |  | 07 |
| `SetHome` | 2 | settings |  | 07, 08 |
| `SetHomeNL` | 1 | settings |  | 07 |
| `SetLanguage` | 1 | settings |  | 07 |
| `SetPasskeys` | 2 | settings |  | 07 |
| `SetPassword` | 1 | settings |  | 07 |
| `SetPaywall` | 1 | settings |  | 06, 07, 08 |
| `SetPlanCredits` | 6 | settings |  | 07 |
| `SetPreferences` | 2 | settings |  | 01, 05, 07, 08 |
| `SetProfile` | 1 | settings |  | 07 |
| `SetReminders` | 4 | settings |  | 07, 08 |
| `SetSecurity` | 1 | settings |  | 07 |
| `SetStates` | 1 | settings |  | 07 |
| `SetStatesMore` | 1 | settings |  | 07 |
| `SheetStates` | 1 | more |  | 07, 08 |
| `SignOutPending` | 2 | more |  | 01, 05, 07, 09 |
| `SplashScreen` | 3 | core |  | 01, 02, 05, 07, 08, 09, 11, 12, 13 |
| `SplashSpec` | 1 | core |  | 05, 08 |
| `TermsUpdate` | 2 | more |  | 05 |
| `Today` | 5 | core |  | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13 |
| `TodayRowStates` | 1 | core |  | 07, 08 |
| `TripActions` | 2 | more |  | 07, 08, 12 |
| `TripAirportSearch` | 1 | trip |  | 06, 08 |
| `TripDiscard` | 2 | trip | yes | 06, 08 |
| `TripEditor` | 2 | trip | yes | 06, 08 |
| `TripEditorErrors` | 1 | trip | yes | 06 |
| `TripReview` | 7 | trip | yes | 06, 08 |
| `TripSearchStates` | 1 | trip |  | 06 |
| `Trips` | 3 | core |  | 01, 02, 03, 07, 08 |
| `Walkthrough` | 4 | more |  | 01, 02, 03, 05, 07, 09, 10, 12, 13 |
| `Wizard` | 146 | trip |  | 01, 02, 03, 06, 07, 08, 10, 12, 13 |
| `WizardCopy` | 1 | trip |  | 06, 08, 10 |
| `WizardSheet` | 34 | trip |  | 01, 06, 07, 08 |
| `WizardSpec` | 1 | trip |  | 01, 06, 08 |

This table was generated by resolving `<dc-import name=…>` in every board under 3 KB (§1.1). The `Wizard` and
`WizardSheet` rows count every wrapper that 06 §2 maps by `screen`/`v` and `kind`/`field`.

---

Sources for [P] facts (searched 2026-11-15):

- Google Play Console Help, Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Google Play Console Help, testing requirements for new personal developer accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Target API, 16 KB pages, Android 16 large-screen behaviour and Credential Manager: see the sources list in `11-android-stack.md`.
