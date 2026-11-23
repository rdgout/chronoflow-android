@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class MemberWizardReviewTest {
    class Reviews {
        @Test fun foreignCancellationOnPersistIsNotAPersistenceFailure() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.store.failure = CancellationException("stale")
                try {
                    fixture.model.go(MemberWizardStep.outbound)
                    fixture.model.flush()
                    assertFalse(fixture.model.persistenceFailed)
                    fixture.model.go(MemberWizardStep.returnChoice)
                    fixture.model.flush()
                    assertEquals(MemberWizardStep.returnChoice, fixture.model.state.step)
                    assertFalse(fixture.model.persistenceFailed)
                } finally {
                    fixture.close()
                }
            }

        @Test fun callerCancellationStillFlushesAndDiscards() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val gate = CompletableDeferred<Unit>()
                fixture.store.beforePersist = { gate.await() }
                fixture.model.go(MemberWizardStep.outbound)
                runCurrent()
                val closing = backgroundScope.launch { fixture.model.discard() }
                runCurrent()
                closing.cancel()
                gate.complete(Unit)
                runCurrent()
                closing.join()
                assertTrue(closing.isCancelled)
                assertEquals(listOf(fixture.model.state.instance), fixture.store.discarded)
                assertTrue(fixture.model.closed)
            }

        @Test fun persistenceFailureIsPublishedInTheSnapshot() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.store.failure = net.chronoflow.core.model.store.StoreFailure.Closed
                fixture.model.go(MemberWizardStep.outbound)
                fixture.model.flush()
                assertTrue(fixture.model.snapshots.value.persistenceFailed)
                fixture.model.go(MemberWizardStep.returnChoice)
                assertEquals(MemberWizardStep.outbound, fixture.model.state.step)
                fixture.close()
            }

        @Test fun reviewWithBlockingPromptsAsksThem() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val review =
                    ApiJson.response.decodeFromString<ApiTripReview>(
                        Fixtures.text("wizard/needs-answers.json"),
                    )
                fixture.generation.reviews.add(
                    Result.success(TripGenerationReviewResult(fixture.model.reviewInput, review)),
                )
                fixture.model.beginChecking()
                runCurrent()
                fixture.checking.advance(1200)
                fixture.checking.advance(1500)
                assertEquals(MemberWizardStep.question, fixture.model.state.step)
                assertEquals(0, fixture.model.state.questionIndex)
                fixture.close()
            }

        @Test fun needsAnswersWithOnlyRetiredPromptsIsAServerFailure() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val review =
                    generationReview().copy(
                        status = PlanStatus.NeedsAnswers,
                        prompts = listOf(wizardPrompt(kind = PromptKind.Screening, fields = listOf("pregnancy"))),
                    )
                fixture.model.applyReview(TripGenerationReviewResult(fixture.model.reviewInput, review))
                assertEquals(DataFailure.Server(null), fixture.model.checking.failure)
                fixture.close()
            }

        @Test fun noPlanGoesToExcluded() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.model.applyReview(
                    TripGenerationReviewResult(
                        fixture.model.reviewInput,
                        generationReview().copy(status = PlanStatus.NoPlan),
                    ),
                )
                assertEquals(MemberWizardStep.excluded, fixture.model.state.step)
                fixture.close()
            }

        @Test fun answerAdvancesAfterTwoHundredFiftyMilliseconds() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                model.state =
                    model.state.copy(
                        step = MemberWizardStep.question,
                        questions = listOf(wizardPrompt(), wizardPrompt(id = "age2")),
                        questionIndex = 0,
                    )
                model.answerQuestion(JsonPrimitive("18_64"))
                runCurrent()
                model.answerQuestion(JsonPrimitive("under_18"))
                fixture.checking.advance(249)
                assertEquals(0, model.state.questionIndex)
                assertTrue(model.choicePending)
                assertEquals(JsonPrimitive("18_64"), model.state.profile["age_band"])
                fixture.checking.advance(250)
                assertEquals(1, model.state.questionIndex)
                assertEquals(MemberWizardStep.question, model.state.step)
                assertFalse(model.choicePending)
                fixture.close()
            }

        @Test fun unsupportedAnswersAreIgnored() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                model.state =
                    model.state.copy(
                        step = MemberWizardStep.question,
                        questions = listOf(wizardPrompt()),
                        questionIndex = 0,
                    )
                model.answerQuestion(JsonPrimitive("42"))
                assertTrue(model.state.profile.isEmpty())
                model.state =
                    model.state.copy(
                        questions =
                            listOf(
                                wizardPrompt(
                                    kind = PromptKind.Screening,
                                    fields = listOf("shift_work"),
                                    shape = ApiReviewPromptShape(ReviewAnswerShape.Boolean, emptyList(), null, null),
                                ),
                            ),
                    )
                model.answerQuestion(JsonPrimitive("yes"))
                assertTrue(model.state.profile.isEmpty())
                model.state = model.state.copy(questions = listOf(wizardPrompt(kind = PromptKind.Cups)))
                model.answerQuestion(JsonPrimitive("18_64"))
                assertTrue(model.state.profile.isEmpty())
                assertFalse(model.choicePending)
                fixture.close()
            }

        @Test fun throttledFailureCountsDownWithCeiling() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.generation.reviews.add(Result.failure(DataFailure.Throttled(41.2, 429)))
                val model = fixture.model
                model.beginChecking()
                runCurrent()
                assertEquals(42, model.retrySeconds)
                model.beginChecking()
                runCurrent()
                assertEquals(1, fixture.generation.inputs.size)
                fixture.checking.advance(41_199)
                model.beginChecking()
                assertEquals(1, model.retrySeconds)
                fixture.checking.advance(41_200)
                assertEquals(0, model.retrySeconds)
                model.beginChecking()
                runCurrent()
                assertEquals(2, fixture.generation.inputs.size)
                fixture.close()
            }

        @Test fun saveIsNeverRetriedAutomatically() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
                fixture.generation.outcomes.add(TripGenerationSaveResult.Outcome.Failed(DataFailure.Offline))
                fixture.model.savePlan()
                runCurrent()
                assertEquals(MemberWizardStep.ready, fixture.model.state.step)
                assertEquals(
                    WizardSaveFailure.Kind.offline,
                    fixture.model.state.saveFailure
                        ?.kind,
                )
                fixture.checking.advance(60_000)
                assertEquals(1, fixture.generation.saves)
                fixture.close()
            }

        @Test fun lateCancelledReviewCannotChangeTheNewStep() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val gate = CompletableDeferred<Unit>()
                val result = TripGenerationReviewResult(fixture.model.reviewInput, generationReview())
                fixture.generation.reviewHandler = {
                    withContext(NonCancellable) { gate.await() }
                    result
                }
                fixture.model.beginChecking()
                runCurrent()
                fixture.model.go(MemberWizardStep.outbound)
                gate.complete(Unit)
                runCurrent()
                fixture.checking.advance(30_000)
                assertEquals(MemberWizardStep.outbound, fixture.model.state.step)
                assertNull(fixture.model.state.review)
                fixture.close()
                assertEquals(0, fixture.checking.waiterCount)
            }

        @Test fun persistenceNeverGoesBackwardsAndEndsOnTheLatestRevision() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val gate = CompletableDeferred<Unit>()
                fixture.store.beforePersist = { gate.await() }
                fixture.model.go(MemberWizardStep.outbound)
                fixture.model.go(MemberWizardStep.returnChoice)
                runCurrent()
                assertTrue(fixture.store.persisted.isEmpty())
                gate.complete(Unit)
                fixture.model.flush()
                val revisions = fixture.store.persisted.map { it.state.revision }
                assertTrue(revisions.zipWithNext().all { (previous, next) -> previous < next })
                assertEquals(2, revisions.lastOrNull())
                fixture.close()
            }

        @Test fun dependencyCancellationIsFailureWhileCallerRemainsActive() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
                fixture.generation.reviewHandler = { throw CancellationException() }
                fixture.model.readyTick(true)
                runCurrent()
                assertEquals(MemberWizardStep.checking, fixture.model.state.step)
                assertEquals(DataFailure.Server(null), fixture.model.checking.failure)
                assertFalse(fixture.model.reviewing)
                fixture.close()
            }
    }

    @RunWith(Parameterized::class)
    class ForeignSaveCancellation(
        private val duringCreate: Boolean,
    ) {
        @Test fun savePlanForeignCancellationReturnsToReadyWithServerFailure() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
                if (duringCreate) {
                    fixture.generation.outcomes.add(TripGenerationSaveResult.Outcome.Saved("created-trip"))
                    fixture.store.beforePersist = {
                        if (it.state.savedTripId != null) throw CancellationException("foreign")
                    }
                } else {
                    fixture.generation.saveFailure = CancellationException("foreign")
                }
                try {
                    fixture.model.savePlan()
                    assertEquals(MemberWizardStep.calculating, fixture.model.state.step)
                    runCurrent()
                    assertEquals(MemberWizardStep.ready, fixture.model.state.step)
                    assertEquals(
                        WizardSaveFailure.Kind.server,
                        fixture.model.state.saveFailure
                            ?.kind,
                    )
                    assertFalse(fixture.model.persistenceFailed)
                    assertFalse(fixture.model.closed)
                    assertNull(fixture.model.saveJob)
                    assertTrue(fixture.model.canSave)
                    fixture.checking.advance(60_000)
                    assertEquals(1, fixture.generation.saves)
                } finally {
                    fixture.close()
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "duringCreate={0}")
            fun cases() = listOf(false, true)
        }
    }

    @RunWith(Parameterized::class)
    class ValidationRouting(
        private val path: String,
        private val step: MemberWizardStep,
        private val mapped: String,
    ) {
        @Test fun validationErrorsRouteToTheirStep() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.model.applyFailure(DataFailure.Validation(mapOf(path to FieldMessage("error")), 422))
                assertEquals(step, fixture.model.state.step)
                assertEquals(setOf(mapped), fixture.model.draft.fieldErrors.keys)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "path={0}")
            fun cases() =
                listOf(
                    arrayOf<Any>(
                        "outbound.legs.0.stay.check_out_local",
                        MemberWizardStep.fineTune,
                        "stays.0.checkOutLocal",
                    ),
                    arrayOf<Any>("profile.habitual_bed", MemberWizardStep.sleep, "profile.habitualBed"),
                    arrayOf<Any>("outbound.legs.0.to_iata", MemberWizardStep.whereTo, "legs.0.toIata"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class SubmitFailures(
        private val failure: Throwable,
    ) {
        @Test fun checkedAndRuntimeFailuresStayInTheModel() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
                fixture.generation.reviewHandler = { throw failure }
                fixture.model.readyTick(true)
                runCurrent()
                assertEquals(DataFailure.Server(null), fixture.model.checking.failure)
                assertFalse(fixture.model.reviewing)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "failure={index}")
            fun cases() =
                listOf(
                    net.chronoflow.core.model.session.SessionFailure.CredentialsUnavailable,
                    net.chronoflow.core.model.store.StoreFailure.Closed,
                    net.chronoflow.core.model.device.DeviceIdentityFailure.InvalidPersistedIdentity,
                    java.io.IOException(),
                    java.security.GeneralSecurityException(),
                    IllegalStateException(),
                    CancellationException(),
                    DataFailure.Server(null),
                )
        }
    }
}
