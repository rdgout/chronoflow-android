# Phase 14 verification

## 1. Summary

2026-11-23. Branch `feat/phase-14-audit-verification`; resumed at `b30594b9`, on the Phase 13 stack top `92781204`. Task 16 commit `9b98e2fe` records the final runs; this document is completed by `docs(verification): record phase 14 verification`. The two final test corrections are `2d980d75` (explicit starting language) and `47b9fa52` (loaded-content readiness). These change the test harness only. [The audit](audit.md) retains earlier focused runs and their fixes without relabelling their original failures.

Observed device: `pixel`, `emulator-5554`, API 36, `google_apis` arm64 image, 1080 × 2400 px, 420 dpi, system font scale 1.0. Only this emulator is attached. The final locked probe confirms it remains running, all three animator scales restored to 0 and font scale restored to 1.0 (`build/phase14/final/final-state.json`). The `fold`, `small` and `api29` T2 runs remain pending; their configured images and exact handoff are in [device-matrix.md](device-matrix.md). No physical-device result is claimed. Android runs here use debug/mock builds; layout/capture runs use Robolectric. Performance numbers supplied by the user below are `benchmarkRelease` emulator measurements, separate from the pending physical gates.

No new production screen, state, string, token, icon, dependency or accessibility suppression is added by the resumed work. The shipped passkey capability stays false. The source reference pins remain iOS `f391a1fa` and API contract web `e36368ec`. The server-state recheck in §5 reads the newer **local** `origin/main` at `c0dbbb53`; it does not change the contract pin or prove deployment.

## 2. Commands and results

[The command ledger](verification-commands.md) records the retained Tasks 1–16 invocations, dates, exits, measured wall times and result paths. Outer command and inner tier times are separate measurements, never added together. Missing historical wall times are marked unavailable. The interrupted pre-pause audit suite has no final verdict; its one fresh replacement is recorded below. Raw output, PNGs, XML, logcat and recordings stay under `build/`.

| Command / evidence | UTC date | Exit | Wall (s) | Result |
| --- | --- | ---: | ---: | --- |
| `make audit-suite` | 2026-11-23T14:23:43.666504+00:00 | 2 | 430.859 | Failed: 365 passed / 11 skipped / 3 failed; isolated permission passes. Two AM/PM findings; language no-op corrected in focused rerun. `build/phase14/final/audit-suite/` |
| `env ORG_GRADLE_PROJECT_android.testInstrumentationRunnerArguments.cf.audit.language=nl ORG_GRADLE_PROJECT_android.testInstrumentationRunnerArguments.cf.audit.theme=dark ORG_GRADLE_PROJECT_android.testInstrumentationRunnerArguments.cf.audit.reduceMotion=1 make targeted ONLY=app:SettingsLanguageDeviceTest#languageChoiceUpdatesAppAndReturnsToAccount` (nl/dark/reduced-motion audit project properties) | 2026-11-23T14:42:11.688983+00:00 | 0 | 12.909 | Passed: explicit English starting state; actual full argv is in the ledger. `build/phase14/final/language-fix/` |
| `make strictmode-run` | 2026-11-23T14:31:07.813111+00:00 | 2 | 633.107 | Failed native tier: app 366 passed / 11 skipped / 2 AM/PM failures; core UI 52 passed. Permission passes; scanner ours 0 / third-party 4; parity 57 captured. `build/phase14/final/strictmode/` |
| `make a11y FULL=1` | 2026-11-23T14:44:43.141136+00:00 | 2 | 785.543 | Failed: 901 passed / 44 skipped / 9 failed. Eight AM/PM cases; one readiness race fixed below. No overruns/hangs. `build/phase14/final/a11y-full/` |
| `make targeted ONLY="app:PlanAuditTest#testTodayPrimaryState app:PlanAuditTest#testTripsPrimaryState"` with FULL audit property | 2026-11-23T15:00:59.120625+00:00 | 2 | 7.567 | Rejected: no cases selected without parameter suffixes. `build/phase14/final/plan-ready-fix/` |
| `make targeted ONLY="app:PlanAuditTest#testTodayPrimaryState[variant] app:PlanAuditTest#testTripsPrimaryState[variant]"` (all four exact suffixes; full argv in ledger) | 2026-11-23T15:02:46.769587+00:00 | 0 | 16.690 | Passed: eight loaded-content variants. `build/phase14/final/plan-ready-parameters/` |
| `make captures` | 2026-11-23T15:04:10.336171+00:00 | 0 | 113.917 | Passed: 2,116 Robolectric capture cases, no overruns/hangs. `build/phase14/final/captures/` |
| `make captures DEVICE=1` | 2026-11-23T15:07:41.124882+00:00 | 0 | 142.177 | Passed: 149 device PNGs, including Material chrome. `build/phase14/final/device-captures/` |
| `make font-scale-check` | 2026-11-23T15:10:09.869700+00:00 | 0 | 246.285 | Passed: nine screens × three scales; 2.0 equals 1.3, 1.0 differs; restored scale 1.0. `build/phase14/final/font-scale/` |
| `make process-death` | 2026-11-23T15:14:53.948850+00:00 | 0 | 397.752 | Passed: all nine cases in one fresh sweep, cold overlay and durable route; AMS/HND recovery. `build/phase14/final/process-death/` |
| `make audit-coverage` | 2026-11-23T15:18:04.212529+00:00 | 0 | 0.072 | Passed: 194 groups / 188 required screen groups / 1,450 required cells, none missing. `build/phase14/final/coverage/` |
| `make targeted ONLY="core:testing:AuditDefaultsTest app:AuditScreensTest"` | 2026-11-23T14:39:28.718903+00:00 | 0 | 7.426 | Passed: audit defaults and manifest registry guards. `build/phase14/final/task16-targeted/` |
| Python `test_makefile.py` | 2026-11-23T14:43:55.855813+00:00 | 0 | 0.107 | Passed: seven Makefile guards, including locking and scoped results. `build/phase14/final/task16-result-scope/` |
| Python `test_matrix_result.py` | 2026-11-23T14:33:55.288120+00:00 | 0 | 0.092 | Passed: eight result validation/negative cases. `build/phase14/final/task16-matrix-cli/` |
| `make format` | 2026-11-23T14:40:57.807107+00:00 | 0 | 27.468 | Passed: retained import ordering from the Medium wrapper rename. `build/phase14/final/format/` |
| `Scripts/run-tier.sh layout … -- ./gradlew --console=plain testDebugUnitTest -Pcf.tier=layout` | 2026-11-23T15:10:44.446780+00:00 | 0 | 88.174 | Passed: 3,318 / one pre-existing API-36 locale skip; zero overruns/hangs. `build/phase14/final/layout/` |
| `./gradlew --console=plain :app:lintDebug` plus all touched Android modules (full argv in ledger) | 2026-11-23T15:12:16.340358+00:00 | 0 | 45.071 | Passed; no new warnings or suppressions. `build/phase14/final/lint/` |
| `make du` | 2026-11-23T15:13:42.189006+00:00 | 0 | 31.780 | Passed: caches 36 GB / AVD 18 GB / build 14 GB / checkout 18 GB. No download took place. `build/phase14/final/du/` |
| Python `test_verification_questions.py` | 2026-11-23T15:22:48.862722+00:00 | 0 | 0.137 | Passed: four parser/legacy/stale-register guards. `build/phase14/final/task17-python/` |
| `make targeted ONLY="app:AuditScreensTest core:testing:AuditPolicyTest"` | 2026-11-23T15:22:49.067963+00:00 | 0 | 6.416 | Passed. `build/phase14/final/task17-targeted/` |
| `make quick` (one final invocation) | 2026-11-23T15:23:32.599748+00:00 | 0 | 89.914 | Passed checks: JVM/logic, build-logic, tooling and serial timing tiers. Total wall exceeds the 60 s warm target; no repeat. `build/phase14/final/quick/` |

The one final `make quick` passes 6,389 JVM/logic cases, 64 build-logic cases, 435 Python tooling cases and seven serial timing cases, with zero per-test overruns/hangs. Its total **89.914 s** exceeds the 60 s warm target: the main tier is 54.020 s, build-logic 2.790 s, Python script suite 30.783 s plus the smaller generator suites, and timing 1.610 s. These component measurements overlap the outer command; no repeat was run. The final warm-time gate remains with the orchestrator’s tier measurement.

The original full accessibility and Dutch-suite exits remain failed. The isolated fixes passed without repeating hundreds of unrelated cases. OQ-A618 remains visible: the native 12-hour picker has a 40 dp AM/PM control, with two wake-sheet aliases in each variant. No new suppression was added. Full-suite acceptance cannot be inferred by subtracting failures from a prior run.

The StrictMode collector initially included eight stale benchmark XML cases. No benchmark executed. The audited native counts are recomputed from the archived app/core-UI **debug** results only; `build/phase14/final/strictmode/native-scoped-summary.json` preserves the corrected counts and original nonzero exit. The Makefile now scopes future collection to the modules it actually runs. StrictMode found **ours 0, third-party 4**: `DiskReadViolation`, first external frame `com.google.android.apps.common.testing.accessibility.framework.strings.AndroidXMLResourceBundle.getDocument(AndroidXMLResourceBundle.java:69)`. These are the test-only ATF XML loader, with no ChronoFlow frame; application policies are unchanged.

The 57 parity entries comprise 56 iOS reference pairs and launcher. Launch frame alignment uses the first black-to-content boundary where observable, otherwise the explicitly recorded recording-start fallback. These comparisons establish structure and appearance evidence, not pixel equality between rendering platforms.

### Review round 1 (2026-11-23)

The orchestrator reports the pre-review `3ee86fd2` gates passed: `make check` in
295 s, size 4.16 MB, `make release-check` and `make release-smoke` (their wall times
were not supplied). These are baseline results, not reruns on the review fixes.

F3 first created `backup/2026-11-23/phase-14-pre-r1`, then used a scripted,
non-interactive, signed rebase. At the rewritten verification-record head (now `854693f9` after round 2), **`git diff backup/2026-11-23/phase-14-pre-r1 HEAD` was empty (exit 0)** and the tree object was
identical. Only subsequent F1/F2/F4/F5/F6 fixes change that tree. The separate
`exp/fraunces-titles` ref was not touched by the worker; the orchestrator later
rebased it onto `d4f5faa0` (now `55162a54`, backup `backup/2026-11-23/exp-fraunces-pre-rebase`). Evidence: `build/review-r1/rewrite-result.json`.

| History repair | Rewritten commit |
| --- | --- |
| `9b18c4d1`, including the import ordering correction | `66982aa1` |
| `356b78f0` Today title benchmark | `e7a6775d` |
| `1265c16b` bounded return to top | `535b86a6` |
| `b30594b9` bar/refresh ordering | `d4f5faa0` |
| `2d980d75` language test setup | `a432d4d1` |
| `47b9fa52` loaded-content waits | `ca24c1c4` |
| Tooling extracted from `9b98e2fe` | `5fc0dbc0` — `test(audit): record matrix results per avd`, with its StrictMode glob fix split into `f0cbb77b` in round 2 |
| Documentation remaining from `9b98e2fe` | `6f3e4f6a` — `docs(audit): record the matrix, strictmode and device runs` |
| `3ee86fd2` verification record | `854693f9` |

F1 leaves Robustness unchecked until T2/fold/resize acceptance is observed. F2
checks all four Today title metrics from the fresh archive, with REPORT-only
thresholds for every device class. F4 uses app-only node ordinals; the regression test fails before the fix, then correctly rejects unchanged
app bounds with only SystemUI changes as “scale not applied”. The nine saved 1.0/1.3/2.0 XML sets also pass re-evaluation
with the corrected classifier (`build/review-r1/f4-saved-dumps.json`); this is
an offline recheck, not a new device sweep. F5 shuts down the selected T2 AVD
before returning a failed matrix status. No T2 device was started to test it.

F6 uses generated 24/12/16 dp start/end/top padding. The exact pinned Material 3
1.5.0-beta01 source (`DatePicker.kt:2625–2626`) confirms those title values and
the unchanged native headline's 24/12/12 dp start/end/bottom values. The source
artifact stays under `build/review-r1/`. The production-component guard measures
actual tagged title/container bounds in all four profiles; it rejects 24 dp end
padding before the fix and passes with 12 dp. No copy assertion, new token or
new screen is introduced.

| Check | Exit | Wall (s) | Outcome / evidence |
| --- | ---: | ---: | --- |
| F1 question-register check | 0 | 0.034 | pass; `build/review-r1/f1-docs/` |
| F2 benchmark checker tests | 0 | 2.868 | pass; `build/review-r1/f2-benchmarks/` |
| F2 Makefile guards | 0 | 0.089 | pass; `build/review-r1/f2-makefile/` |
| F2 existing title archive, offline check | 0 | 0.040 | pass; no benchmark executed; `build/review-r1/f2-archived/` |
| F4 SystemUI negative control before fix | 1 | 0.056 | expected failure: DeviceFailure not raised; `build/review-r1/f4-red/` |
| F4 font-scale guards after fix | 0 | 0.054 | pass; `build/review-r1/f4-font-scale/` |
| F5 shutdown-order guard before fix | 1 | 0.059 | expected failure: shutdown followed exit; `build/review-r1/f5-red/` |
| F5 Makefile guards after fix | 0 | 0.093 | pass; `build/review-r1/f5-makefile/` |
| F6 exact-inset guard before fix | 2 | 17.008 | expected failure in all four profiles: end inset 24 instead of 12; `build/review-r1/f6-red/` |
| F6 picker guards after fix | 0 | 9.323 | 10 passed; `build/review-r1/f6-targeted/` |
| Design-system/core-UI layout tier | 0 | 18.884 | 282 passed, no overruns/hangs; `build/review-r1/layout/` |
| Formatting | 0 | 23.804 | pass; `build/review-r1/format/` |
| App/design-system/core-UI lint | 0 | 41.492 | pass; `build/review-r1/lint/` |
| PickerDateTime capture refresh | 0 | 49.468 | 16 passed / 2,100 unrelated skipped; 16 fresh JVM PNGs; `build/review-r1/captures/` |
| Final make quick (once) | 0 | 91.507 | 6,389 JVM/logic + 64 build-logic + 441 Python + seven timing cases passed; no overruns/hangs; `build/review-r1/quick/` |

The review quick run passes all checks, but **91.507 s** exceeds the 60 s warm
target; it was not repeated. Its main tier is 53.040 s; the Script tests are
31.210 s. Capture outputs remain uncommitted under `build/captures/` and no
emulator/performance/T2/profile tier ran in this review. No new OQ or suppression
was added. Robustness and Accessibility remain open pending their stated gates.

Phase 13 final gates below are **pending (orchestrator/user)** on the post-review stack. Historical Phase 13 passes do not close the launch-clock/profile refresh after `89f3dd7f`, the Medium bar change or the timeline-adjacent expansion fix. The worker did not run these tiers:

```sh
make check
make test
make size
make release-check
make release-smoke
Scripts/measure-tiers.sh
make baseline-profile
make perf
make perf-today-title
make perf-timeline
THEME=dark make perf-timeline
FONT_SCALE=1.3 make perf-timeline
```

Record the actual final commit, warm wall time, metric/result directory and verdict for each. `make size` must reject a newly introduced `.so`; no new dependency was added here. No budget or waiver is inferred from a missing run.

### Review round 2 (2026-11-23)

Round 2 found that `make matrix` could still leave a T2 emulator running when `ensure` failed, and that two commits mixed concerns. After `backup/2026-11-23/phase-14-pre-r2`, a non-interactive signed rebase split them; `git diff backup/2026-11-23/phase-14-pre-r2 cfa1eebf` is empty. Round 1's fix commits were renamed by that rebase: F1 `46f43558`, F2 `6c52a84d`, F4 `b364dd2f`, F5 `80cecd65`, F6 `31c449e1` (code and test) with its review record in `cfa1eebf`; the StrictMode results-glob fix is `f0cbb77b`, and the failed-boot shutdown is `4a1fb9d0`. Host `make check` passed at `edd5ef8b` (273 s), the pre-round-2 tree of `cfa1eebf`. Host `make check` also passed at `4a1fb9d0`. Round 3 found only this record's stale hashes, fixed here.

### User-requested side work during the pause

The user's 2026-11-22 decision, “The medium topbar is much better in my eyes, please keep that.”, is implemented by `9b18c4d1`: all wrapper users use M3 MediumTopAppBar (112 dp expanded, 64 dp collapsed) and `exitUntilCollapsedScrollBehavior`. This supersedes the former LargeTopAppBar mapping. `356b78f0` adds `make perf-today-title`; `1265c16b` bounds repeated downward flings until the list returns to its top. `b30594b9` orders the bar's nested-scroll connection inside pull-to-refresh, so the title expands before refresh consumes the remaining drag. This is committed side work and was not redone here.

The separate `exp/fraunces-titles` experiment remains untouched and unmerged. The user's instruction was “Do it. If it works fine we'll have a look at iOS to use it too”. The experiment uses the existing Fraunces page-title token (30/36, −0.01em), compared with the board's 34/41, −0.01em. The user reports these pixel emulator comparisons (system baseline versus Fraunces repeats):

| Metric | System | Fraunces |
| --- | ---: | ---: |
| Today scroll P90 frame CPU | 18.5 ms | 18.6 / 18.7 ms |
| Today scroll P99 frame overrun | 14.8 ms | 10.7 / 14.8 ms |
| Signed-in cold TTID | 120.4 ms | 123.0 ms |

There is no measurable incremental cost in these supplied emulator results. They are not physical-device claims or a user decision to retain Fraunces. That decision remains pending. To compare future system/Fraunces commits, the orchestrator runs `make perf` and `make perf-today-title` on each chosen commit under the existing device lock, retaining actual SHAs and artifacts; this phase does not switch to the experiment.

## 3. Audit outcome

`make audit-coverage` passes: **334 canvas entries, 42 excluded, 292 in-scope boards, 194 state groups**. All **188 required screen groups / 1,450 cells** are covered, with zero missing cells or exemptions. Four non-screen specification/icon groups have no capture matrix; GuestExcluded and PolicyReader stay unbuilt with reasons. The report names 1,940 matching PNG paths in `build/audit/coverage.{json,md}`.

Every state group has the brief's Canvas title / Screen / Checks / verdict format in [audit.md](audit.md). Group-level verdicts overlap: **Fixed 9**, **Open 0 board groups**, **Recorded decision 194**, **Needs a device 10**. There are nine fixed bullets, 380 recorded-decision bullets and ten device bullets. “Open 0 board groups” does not close the global OQ-A618 native-control finding, OQ-A619 contrast decisions or the manual/device gates.

[Material chrome](material-chrome.md) records the four language/theme variants and the native-component decisions. Designer confirmation remains **awaiting designer (OQ-A42)**. The user must export board PNGs from the Design artifact for a font-accurate board comparison (OQ-14-1 / OQ-A620); local HTML lacks the canvas runtime and blob fonts/images. The audit reads the base branch selected by wrapper props, and does not claim a successful wrapper browser render.

## 4. Open device gates

Every manual row below is **pending (orchestrator/user)**. No package probe, automated traversal or semantics assertion is substituted for observing a service or a physical device.

| Gate and source | Why open | Procedure that closes it |
| --- | --- | --- |
| T2 matrix, real fold/split/resize ([device matrix](device-matrix.md)) | User owns AVD creation and these device runs. | Run the exact locked commands below; retain per-AVD counts, dates, wall times, screenshots and real split method. |
| Reference and low-end performance (OQ-A22/A45; [startup](perf-startup.md), [timeline](perf-timeline.md)) | No physical device attached. First production upload requires both timeline rows or the user's explicit waiver. | Run the physical commands below for each actual device; record model, build, refresh rate and results. |
| 120 Hz pacing / haptics (A9; [timeline](perf-timeline.md), [audit](audit.md)) | Emulator cannot prove actual refresh pacing or haptic feel. | Use a cooled reference 120 Hz phone, battery saver off and airplane mode; run timeline flings and inspect tracing/frame distribution. Activate the existing check-off/selection gestures and record the observed haptics against iOS, without adding feedback. |
| Passkey sign-in and registration ([passkeys](passkeys.md), S1–S3) | Shipped capability false; Android association/origin and production acceptance unverified. | After server gates and an explicit capability decision, use a Play-signed build: verify RP, system prompt, registration/sign-in, cancellation/no-credential and designed server failure. Record redacted outcomes and deployed revision, never assertions/challenges. |
| Automatic App Links ([app-links](app-links.md), S1) | Explicit-package seed delivery does not prove the real domain. | After assetlinks ships, install the Play-signed build and open real reset/verification email links. Record read-only `pm get-app-links` and UI routing. Never reset/force verification outside the dedicated test that needs it. |
| TalkBack first focus, errors, timeline actions, wizard updates (OQ-A103; [audit](audit.md)) | Service installed, manual pass not observed. | Enable TalkBack in Accessibility settings; cold launch and verify Welcome wordmark-first reading order, one Sign in error announcement with focus retained, timeline day/time order and Done/Skip actions, then Checking live-region changes/result focus. Restore prior service settings. |
| Switch Access and hardware keyboard ([audit](audit.md), Task 9) | Automated focus/ESC tests do not prove manual service use. | Enable Switch Access; traverse/activate plan, auth and wizard controls, dismiss/reopen windows. With a hardware keyboard, Tab/Shift-Tab through the same routes, activate controls, verify visible focus, and Esc every dismissible sheet/dialog. Record device and order; restore settings. |
| Native AM/PM target and contrast defaults (OQ-A618/A619) | Native 40 dp control and below-target designed pairs are retained under existing defaults. | User decides whether to change native policy/board tokens. If approved, make a scoped change and rerun the exact failing wake aliases / JVM contrast checks, then full accessibility once. No blanket suppression. |
| Reminder delivery under Doze ([reminders](reminders-ui.md)) | JVM scheduling/mock tests do not measure OEM delivery latency. | On a physical device enable the existing reminders, record scheduled times, enter Doze in the dedicated test session, observe delivery/timing and resume/reschedule behavior; restore idle state afterwards. |
| Native/RN hardware comparison ([release](release.md), [startup](perf-startup.md)) | Only historical emulator comparisons exist. | Run `make perf-rn` with the SHA-verified existing RN release APK on each physical class. Do not build or modify the RN repository. |
| Icon/system chrome and 280 dp/native windows ([audit](audit.md), [material chrome](material-chrome.md)) | Captures do not prove launcher masks, live system appearance or every real window arrangement. | Review launcher normal/themed icon and every recorded Needs-a-device board on the target devices, including native picker/sheet placement and preserved trip anchor in split/fold. Designer confirms the Material rows. |

Orchestrator T2/fold sequence (not run by the worker):

```sh
make du
make avd-create AVD=fold
make avd-create AVD=small
make avd-create AVD=api29
make du
make matrix
Scripts/emulator.sh ensure fold
Scripts/fold-check.sh
python3 -I Scripts/device_lock.py -- python3 -I Scripts/resize-check.py
```

Create only missing AVDs. A failed AVD is rerun once after its scoped fix with `make matrix ONLY=fold` (or `small` / `api29`). The matrix validates and locks the complete sequence and writes `build/audit/matrix/<avd>.json`; all-skipped, missing, failed, over-budget or hung evidence cannot pass. `resize-check.py` tests overrides; real split must also be entered using `am start --windowingMode 3` if supported, otherwise Overview, and the actual method/screenshot recorded. The trip anchor and open sheet must survive; an override alone is not split-screen evidence.

Physical performance commands (select the one actual attached device as required by the existing runner; replace the serial, do not use an emulator as `reference`):

```sh
PERF_CLEAR=1 DEVICE_CLASS=reference ANDROID_SERIAL=YOUR_REFERENCE_SERIAL make perf
PERF_CLEAR=1 DEVICE_CLASS=reference ANDROID_SERIAL=YOUR_REFERENCE_SERIAL make perf-timeline
PERF_CLEAR=1 DEVICE_CLASS=reference ANDROID_SERIAL=YOUR_REFERENCE_SERIAL make perf-rn
PERF_CLEAR=1 DEVICE_CLASS=lowEnd ANDROID_SERIAL=YOUR_LOW_END_SERIAL make perf
PERF_CLEAR=1 DEVICE_CLASS=lowEnd ANDROID_SERIAL=YOUR_LOW_END_SERIAL make perf-timeline
PERF_CLEAR=1 DEVICE_CLASS=lowEnd ANDROID_SERIAL=YOUR_LOW_END_SERIAL make perf-rn
```

## 5. Server dependencies S1–S6

Reverified read-only on 2026-11-23, local web `origin/main` **c0dbbb53**; raw commands/stdout/exits: `build/phase14/final/server-readonly.json`. No fetch, server mutation or sibling build was performed. Source observations do not prove production deployment.

| Item | Current evidence | Status / closing evidence |
| --- | --- | --- |
| S1 Android assetlinks | `git grep -n -i assetlinks origin/main -- routes app public` finds no route/file. | Open: publish the correct package/certificate association and verify the deployed HTTPS response and a Play-signed device. |
| S2 Android passkey origin | `origin/main:config/fortify.php:150` still allows only `config('app.url')`. | Open: deploy the Play certificate's `android:apk-key-hash` origin and verify an Android assertion. |
| S3 passkey management merge/deploy | Old feature tip `1f4a2684` is not an ancestor, because PR #114 was squash-merged as **b5977837**. That squash is an ancestor of `origin/main`; API routes/controllers and relevant passkey tests have an empty diff against the feature tip. | Merge resolved by source evidence. Deployment and Play-signed management acceptance remain pending; do not report the feature as unmerged. |
| S4 deletion without verification | `origin/main:routes/api.php:93–94` still requires `verified`; web `settings/profile` delete is inside the `auth, verified` group in `routes/settings.php`. | Open server discrepancy. Android follows its current policy; no server fix attempted here. |
| S5 production demo account | Production data cannot be established from local source. Debug process-death success uses the mock only. | Open: authorized production acceptance by the owner, with no credentials in the record. |
| S6 iOS AASA applinks | `origin/main:config/services.php` lists Apple `webcredentials` apps, with no `applinks` section. | Open iOS/server dependency; no sibling change. |

Read-only repro commands use `/Users/rickgout/Sites/chronoflow-new` with `git -C`: `rev-parse origin/main`, `grep -n -i assetlinks origin/main -- routes app public`, `show origin/main:config/fortify.php`, `show origin/main:routes/api.php`, `show origin/main:routes/settings.php`, `show origin/main:config/services.php`, `show --stat b5977837`, `merge-base --is-ancestor b5977837 origin/main`, and `diff --stat 1f4a2684 origin/main -- app/Http/Controllers/Api routes/api.php tests/Feature/Api/AccountPasskeyDestroyTest.php tests/Feature/Api/AccountPasskeyOptionsTest.php tests/Feature/Api/AccountPasskeyStoreTest.php tests/Feature/Settings/PasskeyAttestationTest.php`. Use local refs only.

## 6. Open questions on their defaults

[The complete question register](verification-questions.md) reproduces **every** recorded plan OQ default and its affected phases, including legacy phase identifiers; historical resolved rows are not reopened. It is regenerated with `python3 -I Scripts/verification-questions.py`; `--check` rejects stale output. OQ-A68 remains intentionally unused.

| Brief id | Plan id | Default / evidence |
| --- | --- | --- |
| OQ-14-1 | OQ-A620 | Compare against the base board's HTML values (read the branch the wrapper's props select) and the iOS screenshots; headless renders of base boards are structural previews only. Ask the user for PNG exports in `docs/verification.md`. |
| OQ-14-2 | OQ-A621 | 8 cells (§5.2): all four at `pixel8`, `nl_light` and `en_dark` at `foldInner` and `largeFont`; wide and large-font-only groups per §5.1. |
| OQ-14-3 | OQ-A622 | Sweep process death on the unseeded debug mock build, signing in once through the form (an audit procedure, like 13's OQ-13-8). If the unseeded mock rejects the demo account, stop and report. |
| OQ-14-4 | OQ-A623 | Yes for `PlanAuditTest` (5) and `AuthAuditTest` (3), en/light, `@Smoke` (13 §4.7); everything else `@Audit` via `make a11y`. Move them to `@Audit` if `make check` exceeds 5 min. |
| OQ-14-5 | OQ-A624 | Not swept on the device; proven by 9g's Robolectric `app:WizardProcessDeathTest`, re-run in Task 13; recorded as "not swept on device" with that reason. |
| OQ-14-6 | OQ-A625 | Add the iOS key in `app/src/debug` only if the screen already renders the state; otherwise `@Ignore` with the reason and list it. |
| OQ-14-7 | OQ-A626 | Stays unbuilt; listed in `docs/audit.md` "Not built" with phase and reason; never built here. |
| OQ-14-8 | OQ-A627 | Do the manual passes where the services exist; otherwise "open: device only (OQ-A22)". The automated semantics checks still run. |
| OQ-14-9 | OQ-A628 | Not added: it installs an eager initializer, which AGENTS.md "Performance" forbids; record in `docs/verification.md`. |

Evidence resolves the local execution questions without silently changing their policy: process-death mock login works (OQ-14-3), nine restored routes are verified, and save-resume remains a JVM proof (OQ-14-5). The eight primary auth/plan smoke audits stay in `make check` until the orchestrator's measured gate demonstrates whether its five-minute budget holds (OQ-14-4). Ten auth cases per full variant remain explicitly skipped for absent designed/seeded states; capability-gated Passkeys also skips (OQ-14-6). GuestExcluded and PolicyReader remain unbuilt (OQ-14-7). TalkBack and Switch Access are present, but their manual passes are pending (OQ-14-8). LeakCanary is not added: its eager initializer conflicts with the existing launch policy (OQ-14-9 / OQ-A610).

OQ-A617 (native navigation-strip scrim), OQ-A618 (native AM/PM target) and OQ-A619 (computed contrast) were allocated during earlier tasks and are not renumbered. Their defaults and evidence remain in plan.md/audit.md. The Medium bar decision is already recorded in plan.md; the Fraunces experiment remains a separate pending user decision.

## 7. Tooling notes

| Brief `[verify]` / implementation point | Observed outcome |
| --- | --- |
| App locale command for launch frames | API 36 accepts the set-app-locales command in the unseeded recovery procedure (en-US). Parity attempts nl-NL without requiring success and always supplies the explicit Dutch debug extra as the API-independent fallback; captures do not independently identify which path selected Dutch. |
| `ffmpeg` frame alignment / pairing | The implementation uses `blackdetect` (rather than the proposed `blackframe`) and produces all 56 pairs plus launcher. No initial black interval uses the recorded start-relative fallback; no frame-accurate/pixel-identical claim is made. |
| ESC routing | The platform-only removal proof fails for three native sheets while the alert passes (38.603 s). The minimal shared sheet-window handler remains and respects disabled back/dismiss; the alert uses Android's unhandled ESC→BACK fallback. The focused corrected run passes (14.280 s). Tests wait with Compose `waitUntilDoesNotExist`, since UiAutomator can retain a closed dialog's node. |
| Real split shell command | Not executed here by user boundary. `am start --windowingMode 3` is pending; use Overview fallback and record the real method in device-matrix.md. |
| TalkBack / Switch Access packages | Read-only `pm list packages` confirms both installed on pixel. This is not a manual pass. |
| App Links probe | Latest locked metadata probe reports `Error: package net.chronoflow.app.debug unavailable` (exit 1) after connected-test uninstall. Earlier debug state 1024 is historical, not server verification; no reset or forced verifier command ran. |
| Missing/stale results | Empty emulator suites fail. Matrix summaries reject missing, all-skipped, failed, over-budget or hang evidence; strictmode collection selects only executed debug modules. |
| Long audit progress | Runner reports fresh TestRunner finished markers, retaining all existing idle/per-test caps. No heartbeat hides a hang. |
| Suppression scope | Only the brief's named Welcome pairs, disabled native targets, font cap and OQ-A617 native navigation strip apply; a genuine undersized clickable control remains a failing negative control. OQ-A618 is not suppressed. |
| Final generated-doc formatting | The pre-commit whitespace check found an extra blank line at the register EOF. The generator now emits one final newline; its four guards pass again (0.115 s), `make format` passes (19.458 s), and no quick tier was repeated. |
| Commit signing | New commits use `git commit -S` (successful) and carry PGP signature headers. An optional read-only `git log -2 --format='%h %G? %s'` verification query produced no output and was interrupted (exit 130); no unsigned fallback was used. |
| Side effects / source integrity | Tracked changes in all three read-only source repositories remain unchanged (empty before and after); their pre-existing untracked files were left alone. No board PNGs, captures, raw logs or videos were committed. No perf, baseline, check, T2, fold or resize device run was executed by the worker. |

## 8. Outcome of Phase 14

**Outcome of Phase 14.** Board coverage is complete for all 188 required screen groups (194 total groups, 1,450 required cells), with every difference fixed or recorded; fresh JVM/Robolectric captures, pixel captures, 57 parity entries, the nine-screen font cap and all nine process-death cases are verified. StrictMode reports zero ChronoFlow violations and four explained test-library reads. The pre-review quick run passes every case without per-test overruns/hangs, but its 89.914 s total exceeds the 60 s warm target. Review round 1 also passes every check, in 91.507 s, with no retry; the warm-time target remains open. The full native audit exits remain failed on the retained AM/PM target finding (OQ-A618); isolated language/readiness corrections pass. Computed contrast defaults (OQ-A619), designer approval, board PNG exports, manual TalkBack/Switch Access/keyboard, T2/fold/real split, final release/check/profile/performance and physical-device gates remain open with closing procedures. S3 is source-merged; server deployment and S1/S2/S4–S6 remain unverified or open. The committed Medium bar and refresh fix are retained; the separate Fraunces experiment has no measurable incremental emulator cost in the user-supplied results and awaits the user’s decision. This completes the audit implementation and verification record, not production acceptance; no missing, failed or manual gate is counted as a pass.
