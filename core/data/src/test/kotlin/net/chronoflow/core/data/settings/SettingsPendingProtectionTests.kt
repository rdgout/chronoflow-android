package net.chronoflow.core.data.settings

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.syncDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsPendingProtectionTests {
    @Test fun offlineQueueAndPullsPreservePendingValuesInOtherGroups() =
        runTest(timeout = 60.seconds) {
            withStore { f ->
                f.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        json("""{"chronotype":null,"age_band":"65_plus"}"""),
                    ),
                )
                val sleep =
                    json(
                        """{
                        "habitual_bed":"23:15", "habitual_wake":"07:45", "free_day_status":null,
                        "free_day_bed":null, "free_day_wake":null
                    }""",
                    )
                f.enqueue(OutboxChange.Settings(SettingsSection.BodyClock, sleep), 1)
                assertOffline(f)
                f.database.write { sql ->
                    StoreApplication.setting("bodyClock", json("""{"ageBand":"18_64"}"""), sql)
                }
                val rows = SettingsRepository(f.database).settings().associate { it.section to it.value }
                val body = json(checkNotNull(rows["bodyClock"]))
                assertEquals(JsonPrimitive("65_plus"), body["ageBand"])
                assertEquals(JsonPrimitive("23:15"), body["habitualBed"])
                assertEquals(JsonPrimitive("07:45"), body["habitualWake"])
                assertEquals("true", rows["sleepAnswered"])
                assertEquals(2, OutboxRepository(f.database).pending().size)
            }
        }

    @Test fun offlineChangesRemainPendingDuringPull() =
        runTest(timeout = 60.seconds) {
            withStore { f ->
                f.enqueue(OutboxChange.Settings(SettingsSection.Preferences, json("""{"time_format":"12"}""")))
                assertOffline(f)
                f.database.write { sql ->
                    StoreApplication.setting("preferences", json("""{"timeFormat":"24"}"""), sql)
                }
                val value = SettingsRepository(f.database).settings().single { it.section == "preferences" }.value
                assertEquals(JsonPrimitive("12"), json(value)["timeFormat"])
                assertEquals(1, OutboxRepository(f.database).pending().size)
            }
        }

    private suspend fun assertOffline(f: SyncStore) {
        val source = ScriptedSource().apply { configure(failMethod = "send", methodFailure = DataFailure.Offline) }
        assertTrue(OutboxFlusher(f.sync, source, now = { syncDate }).run(f.epoch).offline)
    }

    private suspend fun TestScope.withStore(block: suspend (SyncStore) -> Unit) {
        val f = SyncStore(StandardTestDispatcher(testScheduler))
        try {
            block(f)
        } finally {
            withContext(NonCancellable) { f.close() }
        }
    }

    companion object {
        private fun json(value: String): JsonObject = Json.parseToJsonElement(value) as JsonObject

        @JvmStatic @BeforeClass
        fun warmJdbc() {
            measureClassWarmup(SettingsPendingProtectionTests::class.java, 0, "jdbc") {
                runTest(timeout = 60.seconds) {
                    SyncStore(StandardTestDispatcher(testScheduler)).close()
                }
            }
        }
    }
}
