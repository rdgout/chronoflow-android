# Local store porting map

The reference pin is `f391a1fa`. Phase 4a delivers storage primitives; the repository and session lifecycle cases stay with their owning phases.

| iOS test | Kotlin class (module) | Tier | Task |
|---|---|---|---|
| `AppCoreTests/SessionStoreTests` (2) | `SessionStoreTests` (`:core:model`), same names | JVM | 1 |
| `AppCoreTests/DeviceIdentityTests.uuidv7Encodes…` | `UUIDv7Tests` (`:core:model`) | JVM | 2 |
| `AppCoreTests/DeviceIdentityTests` (5 more) except `realPreferences…` | `DeviceIdentityTests` (`:core:datastore`), same names | JVM | 8 |
| `AppCoreTests/DeviceIdentityTests.realPreferencesPersistOutsideAccountNamespaces` | `DevicePreferencesTests` (`:core:datastore`), same name | Robolectric `@Logic` | 8 |
| `LocalStoreTests/AccountDatabaseTests` (3) | `AccountDatabaseTests` (`:core:database`), same names | JVM | 4 |
| `LocalStoreTests/SchemaV1Tests` (2) | `SchemaV1Tests` (`:core:database`), same names | JVM | 4 |
| `AppTests/AccountStorageLifecycleTests` (1) | `AccountStorageLifecycleTests` (`:core:database`), name without `test` | Robolectric `@Logic` | 4 |
| `LocalStoreTests/RecordsTests.invalidJSONCannotEnterARecord` | `RecordMappingTests` (`:core:database`), same name | JVM | 6 |
| `LocalStoreTests/RecordsTests` (other 2) | — | — | 4d (draft halves 9b) |
| `LocalStoreTests/RepositoryObservationTests` (6) | — (Android-only `ObservationTests` cover the database layer in Task 5) | — | 4d |
| `AppTests/KeychainSessionStoreTests` (2) | `KeystoreSessionStoreTest` (`app/src/androidTest`), renamed `hostedKeystore…`/`legacyShapeKeyProperties…` | `@Smoke` | 7 |
| `LocalStoreTests/AccountWiperTests` (4), `SessionAccountDatabaseTests` (5), `AccountReadinessTests` (1) | — | — | 4e |
| `LocalStoreTests/NoHealthDataMigrationTests` (1) | not ported (OQ-4a-8) | — | — |


The parameterized cases keep their Swift method names in `MalformedSessionStoreTests`, `InvalidAccountPathsTests`, `InvalidDeviceIdentityTests` and `InvalidDeviceClockTests`. The corresponding primary classes contain the other ported methods. Android-only coverage lives in `SchemaParityTest`, `SqlSafetyTest`, `SyncEpochTests`, `SessionMutationPermitTests`, `StorageNamespaceTests`, `JSONDocumentTests`, `AccountDatabaseGuardTests`, `ObservationTests`, `KeystoreSessionStoreFileTests`, `AndroidDeviceNameTests`, `WipeMarkerTests`, `BackupRulesTest`, `StoragePathsTest` and `StoreThreadingSmokeTest`.

Database work is dispatched to IO. Guarded writes hold epoch → permit → transaction through commit. Saves validate raw JSON, update first, then insert when `changes()` is zero. Observation uses table listeners, fetches on IO, supports null and completes on close.

`JdbcAccountDriverFactory`, `TestRoots`, account constants and `SqlProbe` currently live in database test sources. Phase 4d moves these to its database-testing module before other modules consume them. Phase 4d also adds plan/check mapping; 9b adds draft queries. Device identity and preferences remain outside account namespaces. Production wiring belongs to 4e.
