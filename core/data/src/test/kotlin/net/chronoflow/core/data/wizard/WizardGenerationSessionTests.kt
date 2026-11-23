package net.chronoflow.core.data.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardGenerationSessionTests {
    @Test fun reviewEffectsRequireTheOriginalSessionPermitThroughCommit() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val controller =
                SessionController(MemorySessionStore(), RecordingAccountStore(), SessionTeardown(sleep = {}), io)
            controller.accept(sessionResult())
            val snapshot = requireNotNull(controller.activeSession())
            val permit = requireNotNull(controller.mutationPermit(snapshot))
            val fixture = WizardStore(io, snapshot.session.accountId)
            try {
                val epoch = SyncEpoch(AccountID.of(snapshot.session.accountId), snapshot.epoch)
                val effects =
                    TripGenerationEffects.storing(
                        fixture.database,
                        GenerationSource(),
                        epoch,
                        permit,
                        requestSync = {},
                        now = { java.time.Instant.EPOCH },
                    )
                effects.mergeProfile(JsonObject(mapOf("habitual_bed" to JsonPrimitive("23:30"))))
                controller.signOut(discardPending = true)
                try {
                    effects.mergeProfile(JsonObject(mapOf("habitual_bed" to JsonPrimitive("21:00"))))
                    error("Expected revoked permit")
                } catch (_: CancellationException) {
                }
                val body = SettingsRepository(fixture.database).settings().first { it.section == "bodyClock" }
                assertEquals(
                    JsonPrimitive("23:30"),
                    ApiJson.response.parseToJsonElement(body.value).jsonObject["habitualBed"],
                )
            } finally {
                controller.close()
                fixture.close()
            }
        }
}
