# Phase 4a validation

Completed 2026-11-18 in the main checkout on `feat/phase-4a-store`. The branch preflight printed exactly `feat/phase-4a-store`. All seven Phase 4a plan boxes are complete; the brief refresh and each of Tasks 1–10 have their own GPG-signed Conventional Commit. Production wiring remains owned by 4e. No UI, repository, draft record, session controller or account wiper was added.

## Brief refresh

The refresh commit records each mechanical patch in its body:

- Replace the old parallel-worktree setup with the single lane from the 3c top.
- Replace stale Android line citations with landed paths/headings and current dependency names.
- Confirm landed 1a record signatures and leave their files and mapping ownership unchanged.
- Preserve the existing model coroutine API and app store dependencies; all required catalog aliases already exist.
- Include the additional device-config imports in `DeviceConfigStrictModeTest` and `ConfigurationPolicyTest`, and the actual `testDebug/…/debug/TestSeedsApplyTest.kt` path.
- Confirm SQLDelight without fallback, tier infrastructure, and all nine backup domains.
- Map the brief's 18 local questions to OQ-A113–130, leaving OQ-A68 unused.
- Apply the user's targeted-test policy, one final quick run, and signed-commit requirement.

No earlier-phase behaviour required a preflight implementation change. The later device-config move preserves its launch read and existing tests. Parameterized Swift cases use separate classes so unrelated tests are not repeated for every argument; the method names are preserved. The shared-preferences test fixture creates no DataStore for its singleton-only test.

## Tests

Each task used `make targeted`. Table durations are Gradle's reported elapsed wall times (rounded by Gradle); precise tier times are listed below where saved. Full local command/output logs are linked in the last column. Failed compile/test attempts are retained, with their corrections described below; no unresolved failure remains.

| Task/run | Result | Gradle wall time | Exact log |
|---|---|---|---|
| 1 — session primitives | pass | 3s | [`task1.log`](../build/phase4a/task1.log) |
| 2 — initial compile | corrected; see below | 1s | [`task2.log`](../build/phase4a/task2.log) |
| 2 — import correction and UUID/JSON cases | pass | 2s | [`task2-fix1.log`](../build/phase4a/task2-fix1.log) |
| 3 — initial compile | corrected; see below | 8s | [`task3.log`](../build/phase4a/task3.log) |
| 3 — schema and SQL safety | pass | 1s | [`task3-fix1.log`](../build/phase4a/task3-fix1.log) |
| 4 — initial lifecycle run | corrected; see below | 5s | [`task4.log`](../build/phase4a/task4.log) |
| 4 — framework rollback probe | pass | 3s | [`task4-fix1.log`](../build/phase4a/task4-fix1.log) |
| 5 — initial compile | corrected; see below | 1s | [`task5.log`](../build/phase4a/task5.log) |
| 5 — guards and observation | corrected; see below | 4s | [`task5-fix1.log`](../build/phase4a/task5-fix1.log) |
| 5 — two observer corrections | pass | 1s | [`task5-fix2.log`](../build/phase4a/task5-fix2.log) |
| 6 — record mapping | pass | 2s | [`task6.log`](../build/phase4a/task6.log) |
| 7 — session files | pass | 6s | [`task7.log`](../build/phase4a/task7.log) |
| 7 — real Keystore | pass | 10s | [`task7-device.log`](../build/phase4a/task7-device.log) |
| 8 — identity, preferences, markers and existing app config/seeds | pass | 3s + 5s | [`task8.log`](../build/phase4a/task8.log) |
| 8 — lazy preferences fixture | pass | 2s | [`task8-lazy.log`](../build/phase4a/task8-lazy.log) |
| 9 — backup and paths | pass | 3s | [`task9.log`](../build/phase4a/task9.log) |
| 10 — main-thread disk guard | pass | 5s | [`task10-device.log`](../build/phase4a/task10-device.log) |

The task selectors were:

- Task 1: `make targeted ONLY="core:model:SessionStoreTests core:model:MalformedSessionStoreTests core:model:SyncEpochTests core:model:SessionMutationPermitTests core:model:StorageNamespaceTests"`.
- Task 2: `make targeted ONLY="core:model:UUIDv7Tests core:model:JSONDocumentTests"`.
- Task 3: `make targeted ONLY="core:database:SchemaParityTest core:database:SqlSafetyTest"`.
- Task 4: `make targeted ONLY="core:database:AccountDatabaseTests core:database:InvalidAccountPathsTests core:database:SchemaV1Tests core:database:AccountStorageLifecycleTests"`.
- Task 5: `make targeted ONLY="core:database:AccountDatabaseGuardTests core:database:ObservationTests"`.
- Task 6: `make targeted ONLY="core:database:RecordMappingTests"`.
- Task 7: `make targeted ONLY="app:KeystoreSessionStoreTest"`.
- Task 8: `make targeted ONLY="core:datastore:DeviceIdentityTests core:datastore:InvalidDeviceIdentityTests core:datastore:InvalidDeviceClockTests core:datastore:DevicePreferencesTests core:datastore:AndroidDeviceNameTests core:datastore:WipeMarkerTests app:ConfigurationPolicyTest app:TestSeedsApplyTest"`.
- Task 9: `make targeted ONLY="app:BackupRulesTest app:StoragePathsTest"`.
- Task 10: `make targeted ONLY="app:StoreThreadingSmokeTest"`.
- Task 7 also ran `make targeted ONLY="core:datastore:KeystoreSessionStoreFileTests"`.
- Corrections ran only the affected class/methods, as recorded in the linked logs. `make format` handled formatting throughout.

Corrections during targeted work:

- An import expansion mistakenly included the local `assertInvalidationWaits` helper among JUnit imports. Removing those imports fixed compilation; the affected epoch/permit and UUID/JSON classes passed.
- The root helper needed a non-null `user.dir` for Kotlin's Java nullability warning. The schema tests then passed.
- Robolectric's rollback probe initially used JDBC and failed with `java.sql.SQLException: No suitable driver found for jdbc:sqlite:…`. The probe now uses framework SQLite; only `AccountStorageLifecycleTests#failedCreationLeavesNoPartialTable` was rerun and passed.
- SQLDelight generates the settings `value` property as `value_`; correcting the new tests fixed compilation.
- Observation tests exposed the missing channel-close block in `close()` (`TurbineAssertionError: No value produced in 3s`) and coroutine debug suffixes in the injected thread name. The close block and assertion were corrected; only `ObservationTests#closeCompletesTheFlowAndClearsObservers` and `#fetchRunsOnTheInjectedDispatcher` were rerun and passed. The final quick run also passed every guard and observation method.

The single `make quick` invocation passed in **30.97 s** overall (log creation to final output):

| Stage | Cases | Wall time | Budget overruns |
|---|---:|---:|---:|
| quick Gradle tier, including Spotless and Konsist | 1,243 | 14.60 s | 0 |
| build-logic | 26 | 1.06 s | 0 |
| Python suites | 190 | 12.965 s reported suite time | — |
| serial timing tier | 6 | 1.93 s | 0 |

Quick includes 42 database cases across nine classes and 32 datastore cases across seven classes. `build/test-results/quick/timings.json` contains those classes and `overBudget: 0`. Snapshots of the tier JSONs and the complete quick log are in `build/phase4a/`.

Both instrumented classes ran once through `make targeted`, under the device lock, on the existing API 36 `pixel` emulator:

| Class | Cases | Case time total | Host tier wall time | Overruns |
|---|---:|---:|---:|---:|
| KeystoreSessionStoreTest | 5 | 0.122 s | 10.43 s | 0 |
| StoreThreadingSmokeTest | 1 | 0.103 s | 5.52 s | 0 |

The main-thread disk policy survived every store call. SQLite reported **3.44.3** in Robolectric native SQLite and **3.44.3** on `pixel`. JDK 21's `BreakIterator` returned the full 11 UTF-16 units for the family emoji, and the Unicode-name test passed. These are harness/functionality results; this phase records no device performance claim.

## Other checks and limits

- Both pinned schema-resource SHA-256 hashes match the brief, and `SchemaParityTest` compares the created `sqlite_master` DDL.
- The backup tests passed against AGP's actual merged manifest and resource rules; no manifest/rule fix was needed.
- `git diff --check` passes; the new production store/session sources contain no logging, `runBlocking`, saved-state secret storage or native JDBC dependency. SQLDelight's sqlite-driver remains test-only.
- No push, merge, performance run, timeline run or profile regeneration was performed. Per the requested run policy, broad `make check`, the standalone release/Lint gates and `make size` were not run. The brief calls for no new startup/timeline performance numbers in 4a.
- Optional `git log -11 --format='%h %G? %s'` signature-status lookup produced no output and was interrupted (exit 130). Detached verification using `gpg --no-options --no-auto-check-trustdb --no-auto-key-retrieve --lock-never --trust-model always --verify <signature> <payload>` succeeds for every new commit. Signing itself succeeded on every commit; none was committed unsigned.

See [porting/local-store.md](porting/local-store.md) for the hand-off map and [tooling.md](tooling.md#phase-4a) for SQLite and timing notes.


## DataStore native-counter packaging follow-up

The user reported `make check` passing on `0256a32`, followed by `make size` rejecting DataStore 1.3.0's `libdatastore_shared_counter.so`. Source search confirmed that the production store and test stores use only `PreferenceDataStoreFactory.create`, with no multi-process DataStore references. The app now excludes the shared-counter library from JNI packaging. OQ-A131 records that this exclusion depends on keeping stores single-process; OQ-0a-22 and its native-library allow-list are unchanged.

Konsist's `noMultiProcessDataStore` checks app/core/feature/tool source files and rejects `MultiProcessDataStoreFactory`, including imported, aliased and fully qualified references. Three targeted guard tests passed in 2.25 s, covering the current project, all three planted violations and a single-process control.

`make size` ran once and passed in **2.69 s**: **1,462,463 bytes (1.46 MB)** for arm64-v8a/xxhdpi/en. Direct inspection of the release AAB found only `libandroidx.graphics.path.so` under arm64-v8a, armeabi-v7a, x86 and x86_64; the DataStore counter is absent.

The subsequent single `make quick` run passed in **21.21 s** overall: 1,246 quick-tier cases, 26 build-logic cases, 190 Python cases and 6 serial timing cases, with zero budget overruns. Exact logs and JSON snapshots are in `build/phase4a/shared-counter-*`.
