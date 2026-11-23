@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.mockWizardSource
import net.chronoflow.core.data.wizard.support.noGenerationEffects
import net.chronoflow.core.data.wizard.support.withCompletion
import net.chronoflow.core.datamock.MockWizardPrompts
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.generation.TripGenerationState
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardState
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.credits
import net.chronoflow.core.wizard.model.needsReadyReview
import net.chronoflow.core.wizard.model.resumeCompletion
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.revisit
import net.chronoflow.core.wizard.model.savePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardSaveRecoveryTests {
    class Recovery {
        @Test fun recoveryDeleteFailureStillCompletesTheSavedTrip() =
            runTest(timeout = 60.seconds) {
                withCompletion(firstTrip = false) {
                    database.write {
                        it.driver.execute(
                            null,
                            "CREATE TRIGGER fail_recovery_delete BEFORE DELETE ON drafts BEGIN SELECT RAISE(FAIL, 'injected delete failure'); END",
                            0,
                        )
                    }
                    model.savePlan()
                    awaitWizard { model.createdTripID == "created" }
                    assertTrue(model.closed)
                    assertNull(model.draft.state.saveFailure)
                    assertEquals(1, source.count("save"))
                    val recovery = requireNotNull(store.recover())
                    assertEquals("created", recovery.state.savedTripId)
                    assertEquals(MemberWizardStep.calculating, recovery.state.step)
                    database.write { it.driver.execute(null, "DROP TRIGGER fail_recovery_delete", 0) }
                    val restarted = model(recovery, newService())
                    restarted.resumeCompletion()
                    awaitWizard { restarted.createdTripID == "created" }
                    assertEquals(1, source.count("save"))
                    assertNull(store.recover())
                }
            }

        @Test fun transientSavedIDWriteFailurePersistsKnownTripBeforeRecovery() =
            runTest(
                timeout = 60.seconds,
            ) {
                withCompletion {
                    val m = model(model.draft, FailingSavedIDWriteGeneration(service))
                    m.savePlan()
                    awaitWizard { m.draft.state.saveFailure != null }
                    m.flush()
                    val saved = requireNotNull(store.recover())
                    assertEquals("created", saved.state.savedTripId)
                    assertEquals(MemberWizardStep.ready, saved.state.step)
                    assertFalse(m.needsReadyReview)
                    val restarted = model(saved, newService())
                    restarted.resumeCompletion()
                    assertFalse(restarted.needsReadyReview)
                    assertFalse(restarted.reviewing)
                    restarted.savePlan()
                    awaitWizard { restarted.createdTripID == "created" }
                    assertEquals(1, source.count("save"))
                    assertEquals(0, source.count("review"))
                }
            }

        @Test fun savedTripIDIsDurableBeforePullAndRecoverySkipsCreate() =
            runTest(timeout = 60.seconds) {
                val gate = GenerationGate()
                try {
                    withCompletion(effects = TripGenerationEffects({}, { gate.wait() }, {})) {
                        model.savePlan()
                        var recovered: MemberWizardDraft? = null
                        awaitWizard {
                            recovered = store.recover()
                            recovered?.state?.savedTripId != null
                        }
                        val saved = requireNotNull(recovered)
                        val settings = SettingsRepository(database)
                        assertNull(settings.settings().firstOrNull { it.section == "wizardReminders" })
                        assertEquals(MemberWizardStep.calculating, saved.state.step)
                        assertEquals("created", saved.state.savedTripId)
                        assertEquals(1, source.count("save"))
                        assertNull(model.createdTripID)
                        val next = model(saved, newService())
                        next.resumeCompletion()
                        awaitWizard { next.createdTripID == "created" }
                        assertEquals(1, source.count("save"))
                        assertNull(store.recover())
                        assertEquals(
                            "\"pending\"",
                            settings.settings().first { it.section == "wizardReminders" }.value,
                        )
                        gate.open()
                        awaitWizard { model.createdTripID == "created" }
                        assertEquals(
                            "\"pending\"",
                            settings.settings().first { it.section == "wizardReminders" }.value,
                        )
                    }
                } finally {
                    gate.open()
                }
            }

        @Test fun recoveryRetainsStepOpenSheetFailuresAndCheckedInput() =
            runTest(timeout = 60.seconds) {
                withCompletion(optional = true) {
                    val initial = model.draft
                    val draft =
                        initial.copy(
                            state =
                                initial.state.copy(
                                    step = MemberWizardStep.fineTune,
                                    promptSheetID =
                                        initial.state.review
                                            ?.prompts
                                            ?.firstOrNull()
                                            ?.id,
                                    saveFailure =
                                        WizardSaveFailure(
                                            WizardSaveFailure.Kind.throttle,
                                            clock.now.plusSeconds(
                                                42,
                                            ),
                                        ),
                                    revisit = initial.state.checkedInput,
                                    revision = initial.state.revision + 1,
                                ),
                        )
                    store.persist(draft)
                    assertEquals(draft, store.recover())
                    val json = ApiJson.response.encodeToJsonElement(MemberWizardState.serializer(), draft.state)
                    assertEquals(draft.state, ApiJson.response.decodeFromJsonElement<MemberWizardState>(json))
                }
            }

        @Test fun oldContinuationDraftDecodesAndBecomesReadyOrFineTune() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val original = model.draft
                    val state = original.state.copy(step = MemberWizardStep.continuation)
                    val fields =
                        ApiJson.response
                            .encodeToJsonElement(
                                MemberWizardState.serializer(),
                                state,
                            ).jsonObject
                    val retired =
                        setOf(
                            "checkedInput",
                            "reviewedAt",
                            "promptSheetID",
                            "saveFailure",
                            "returnToReady",
                            "revisit",
                            "updated",
                        )
                    val decoded =
                        ApiJson.response.decodeFromJsonElement<MemberWizardState>(
                            JsonObject(
                                fields - retired,
                            ),
                        )
                    var draft = original.copy(state = decoded.copy(revision = decoded.revision + 1))
                    store.persist(draft)
                    val old = requireNotNull(store.recover())
                    assertEquals(MemberWizardStep.ready, old.state.step)
                    assertNull(old.state.checkedInput)
                    assertTrue(model(old).needsReadyReview)
                    val prompts = MockWizardPrompts.make(model.reviewInput.input, JsonObject(emptyMap()))
                    draft =
                        draft.copy(
                            state =
                                draft.state.copy(
                                    review =
                                        draft.state.review?.copy(
                                            prompts = prompts,
                                        ),
                                    revision = draft.state.revision + 1,
                                ),
                        )
                    store.persist(draft)
                    assertEquals(MemberWizardStep.fineTune, store.recover()?.state?.step)
                }
            }

        @Test fun lostResponseAndManualRetryCreateOneMockTripAndSpendOnce() =
            runTest(timeout = 60.seconds) {
                val mock =
                    mockWizardSource(
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                        Instant.ofEpochSecond(
                            1_793_750_400,
                        ),
                    )
                val source = GenerationSource(mock)
                val service =
                    TripGenerationService(
                        source,
                        noGenerationEffects(),
                        parentContext = backgroundScope.coroutineContext,
                    )
                val restarted =
                    TripGenerationService(
                        source,
                        noGenerationEffects(),
                        parentContext = backgroundScope.coroutineContext,
                    )
                try {
                    val reviewed = service.review("lost", ApiTripReviewInput(generationInput()))
                    val initial = TripGenerationState("lost", reviewed.input, review = reviewed.review)
                    val before = mock.fetchTrips().size
                    source.loseSaveResponseOnce()
                    val lost = service.save(initial)
                    assertEquals(TripGenerationSaveResult.Outcome.Failed(DataFailure.Offline), lost.outcome)
                    assertNull(lost.state.savedTripId)
                    assertEquals(before + 1, mock.fetchTrips().size)
                    val retry = restarted.save(lost.state)
                    assertEquals(reviewed.review.identity.token, retry.state.savedTripId)
                    assertEquals(before + 1, mock.fetchTrips().size)
                    assertEquals(2, source.count("save"))
                    val expected = requireNotNull(reviewed.review.price).let { it.balance - it.cost }
                    assertEquals(JsonPrimitive(expected), mock.fetchAccount().settings.planCredits["credits"])
                } finally {
                    service.cancel()
                    restarted.cancel()
                }
            }

        @Test fun foreignDidCreateCancellationRetryDoesNotCreateASecondTrip() =
            runTest(
                timeout = 60.seconds,
            ) {
                withCompletion {
                    var reject = true
                    val wrapped =
                        object : WizardDraftStore by store {
                            override suspend fun persist(draft: MemberWizardDraft) {
                                if (reject && draft.state.savedTripId != null) {
                                    reject = false
                                    throw StaleSessionException()
                                }
                                store.persist(draft)
                            }
                        }
                    val m = model(model.draft, store = wrapped)
                    m.savePlan()
                    awaitWizard { m.draft.state.step == MemberWizardStep.ready && m.draft.state.saveFailure != null }
                    assertEquals(
                        WizardSaveFailure.Kind.server,
                        m.draft.state.saveFailure
                            ?.kind,
                    )
                    assertEquals("created", m.draft.state.savedTripId)
                    m.flush()
                    assertEquals("created", store.recover()?.state?.savedTripId)
                    assertEquals(1, source.count("save"))
                    assertEquals(0, m.retrySeconds)
                    m.savePlan()
                    awaitWizard { m.createdTripID == "created" }
                    assertEquals(1, source.count("save"))
                    assertTrue(m.closed)
                }
            }
    }

    @RunWith(Parameterized::class)
    class Reminders(
        private val firstTrip: Boolean,
    ) {
        @Test fun onlySuccessfulFirstTripSaveQueuesReminders() =
            runTest(timeout = 60.seconds) {
                withCompletion(firstTrip = firstTrip) {
                    val settings = SettingsRepository(database)
                    assertNull(settings.settings().firstOrNull { it.section == "wizardReminders" })
                    source.configure(saveFailure = DataFailure.Offline)
                    model.savePlan()
                    awaitWizard {
                        model.draft.state.saveFailure
                            ?.kind == WizardSaveFailure.Kind.offline
                    }
                    assertNull(settings.settings().firstOrNull { it.section == "wizardReminders" })
                    source.configure()
                    model.savePlan()
                    awaitWizard { model.createdTripID == "created" }
                    val value = settings.settings().firstOrNull { it.section == "wizardReminders" }?.value
                    assertEquals(if (firstTrip) "\"pending\"" else null, value)
                    assertEquals(
                        value,
                        settings.settings().firstOrNull { it.section == "wizardReminders" }?.value,
                    )
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "firstTrip={0}",
            )
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf(
                        true,
                    ),
                    arrayOf(
                        false,
                    ),
                )
        }
    }
}

private class FailingSavedIDWriteGeneration(
    private val service: TripGenerationService,
) : MemberWizardGeneration by service {
    override suspend fun save(
        state: TripGenerationState,
        didCreate: suspend (
            String,
        ) -> Unit,
    ): TripGenerationSaveResult = service.save(state) { throw StoreFailure.Unavailable() }
}
