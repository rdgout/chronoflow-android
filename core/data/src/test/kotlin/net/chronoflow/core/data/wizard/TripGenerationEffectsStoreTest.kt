package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.support.WIZARD_ACCOUNT
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TripGenerationEffectsStoreTest {
    @Test fun pendingRowsWinInFifoOrderAndOnlySleepPatchesMarkSleepAnswered() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                fixture.database.write {
                    it.saveSetting(SettingsRecord("bodyClock", "{}"))
                    it.saveSetting(SettingsRecord("sleepAnswered", "false"))
                    it.saveSetting(SettingsRecord("readiness", "{}"))
                }
                val outbox = OutboxRepository(fixture.database)

                fun patch(value: String) = JsonObject(mapOf("habitual_bed" to JsonPrimitive(value)))
                outbox.upsert(
                    OutboxPayload.record(OutboxChange.Settings(SettingsSection.BodyClock, patch("23:30")), "b", "same"),
                )
                outbox.upsert(
                    OutboxPayload.record(OutboxChange.Settings(SettingsSection.BodyClock, patch("22:30")), "a", "same"),
                )
                val effects =
                    TripGenerationEffects.storing(
                        fixture.database,
                        GenerationSource(),
                        SyncEpoch(AccountID.of(WIZARD_ACCOUNT), 1),
                        requestSync = {},
                        now = { Instant.EPOCH },
                    )
                effects.mergeProfile(JsonObject(mapOf("age_band" to JsonPrimitive("65_plus"))))
                val settings = SettingsRepository(fixture.database)
                val first = settings.settings().associateBy { it.section }
                val body = ApiJson.response.parseToJsonElement(first.getValue("bodyClock").value).jsonObject
                assertEquals(JsonPrimitive("23:30"), body["habitualBed"])
                assertEquals(JsonPrimitive("65_plus"), body["ageBand"])
                assertEquals("false", first.getValue("sleepAnswered").value)
                assertFalse("readiness" in first)
                effects.mergeProfile(patch("21:00"))
                val last = settings.settings().associateBy { it.section }
                assertEquals("true", last.getValue("sleepAnswered").value)
                val finalBody = ApiJson.response.parseToJsonElement(last.getValue("bodyClock").value).jsonObject
                assertEquals(JsonPrimitive("23:30"), finalBody["habitualBed"])
            } finally {
                fixture.close()
            }
        }
}
