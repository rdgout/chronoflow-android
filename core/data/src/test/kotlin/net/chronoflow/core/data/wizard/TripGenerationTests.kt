package net.chronoflow.core.data.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.testing.wizard.EffectProbe
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.generation.TripGenerationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

class TripGenerationTests {
    @Test fun profileReviewAnswersAndSavedPlanReachSQLite() =
        runTest(timeout = 60.seconds) {
            val now = Instant.ofEpochSecond(1_793_750_400)
            val io = StandardTestDispatcher(testScheduler)
            val seed = MockAppSeed.make(now, zone = ZoneOffset.UTC)
            val account = seed.accounts.values.first { it.twoFactorCode == null }
            val mock = MockDataSource(seed = seed, now = { now }, sleep = {}, zone = ZoneOffset.UTC, io = io)
            mock.signIn(
                PasswordSignInInput(
                    account.snapshot.email,
                    account.password,
                    "019a562b-8000-7000-8000-000000000099",
                    "Tests",
                ),
            )
            val source = GenerationSource(mock)
            val fixture = WizardStore(io, account.snapshot.id)
            val epoch = SyncEpoch(AccountID.of(account.snapshot.id), 1)
            val probe = EffectProbe()
            val effects =
                TripGenerationEffects.storing(
                    fixture.database,
                    source,
                    epoch,
                    requestSync = { probe.synced() },
                    now = { now },
                )
            val service = TripGenerationService(source, effects)
            try {
                val snapshot = mock.fetchAccount()
                val identity = mock.fetchIdentity()
                fixture.database.write(epoch) { sql ->
                    StoreApplication.identity(identity, epoch.accountID, sql)
                    StoreApplication.snapshot(snapshot, epoch.accountID, sql)
                }
                val input =
                    ApiTripReviewInput(
                        generationInput(),
                        profile = JsonObject(mapOf("age_band" to JsonNull, "shift_work" to JsonNull)),
                    )
                val first = service.review("one", input).review
                assertEquals(PlanStatus.NeedsAnswers, first.status)
                val age = first.prompts.first { it.kind == PromptKind.AgeBand }
                val screening = first.prompts.first { it.fields == listOf("shift_work") }
                val answered =
                    service.answerPrompts(
                        "one",
                        input,
                        first,
                        mapOf(
                            age.id to mapOf("age_band" to JsonPrimitive("18_64")),
                            screening.id to mapOf("shift_work" to JsonPrimitive(false)),
                        ),
                    )
                assertEquals(first.identity.inputRevision + 1, answered.review.identity.inputRevision)
                val settings = SettingsRepository(fixture.database)
                val body = settings.settings().first { it.section == "bodyClock" }
                val bodyJson = ApiJson.response.parseToJsonElement(body.value).jsonObject
                assertEquals(JsonPrimitive("18_64"), bodyJson["ageBand"])
                assertEquals(2, probe.syncs)
                val sleepPatch = JsonObject(mapOf("habitual_bed" to JsonPrimitive("22:00")))
                val reviewed = service.review("one", answered.input.copy(profile = sleepPatch))
                assertTrue(reviewed.review.canSave)
                val result = service.save(TripGenerationState("one", reviewed.input, reviewed.edit, reviewed.review))
                val tripID = requireNotNull(result.state.savedTripId)
                val repository = TripRepository(fixture.database)
                val trip = repository.trips().first { it.id == tripID }
                assertNotNull(repository.plan(tripID, requireNotNull(trip.currentInputHash)))
                assertEquals(1, source.count("save"))
                assertEquals(1, source.count("trips"))
                assertEquals(1, source.count("detail"))
                service.save(result.state)
                assertEquals(1, source.count("save"))
                OutboxRepository(fixture.database).enqueue(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        JsonObject(mapOf("habitual_bed" to JsonPrimitive("22:30"))),
                    ),
                    now = now,
                    randomBytes = ByteArray(10),
                )
                effects.mergeProfile(
                    JsonObject(mapOf("habitual_bed" to JsonPrimitive("21:00"), "age_band" to JsonPrimitive("65_plus"))),
                )
                val pendingBody = settings.settings().first { it.section == "bodyClock" }
                val json = ApiJson.response.parseToJsonElement(pendingBody.value).jsonObject
                assertEquals(JsonPrimitive("22:30"), json["habitualBed"])
                assertEquals(JsonPrimitive("65_plus"), json["ageBand"])
                epoch.invalidate()
                try {
                    effects.mergeProfile(JsonObject(mapOf("age_band" to JsonPrimitive("65_plus"))))
                    error("Expected stale epoch")
                } catch (_: CancellationException) {
                }
            } finally {
                service.cancel()
                fixture.close()
            }
        }
}
