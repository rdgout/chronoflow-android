@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.withCompletion
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.wizard.generation.TripGenerationAnswers
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.answerOptional
import net.chronoflow.core.wizard.model.canSave
import net.chronoflow.core.wizard.model.credits
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.finishFlightInput
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.needsReadyReview
import net.chronoflow.core.wizard.model.openPrompt
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.popNavigation
import net.chronoflow.core.wizard.model.readyTick
import net.chronoflow.core.wizard.model.resumeChecking
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
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardCompletionTests {
    class Completion {
        @Test fun readyExpiresAtThirtyMinutesAndChangedInputsInvalidateIt() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    assertFalse(model.needsReadyReview)
                    clock.advance(1799.999)
                    assertFalse(model.needsReadyReview)
                    clock.advance(0.001)
                    assertTrue(model.needsReadyReview)
                    model.readyTick()
                    awaitWizard { !model.reviewing && !model.needsReadyReview }
                    assertEquals(1, source.count("review"))
                    model.editForm { it.copy(preparation = it.preparation.copy(days = 1)) }
                    assertTrue(model.needsReadyReview)
                    model.savePlan()
                    awaitWizard { !model.reviewing && !model.needsReadyReview }
                    assertEquals(0, source.count("save"))
                    assertEquals(2, source.count("review"))
                    assertEquals(MemberWizardStep.ready, model.draft.state.step)
                }
            }

        @Test fun unchangedRevisitReturnsToReadyWithoutChecking() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    model.revisit(MemberWizardStep.outbound)
                    assertEquals(true, model.draft.state.returnToReady)
                    model.finishFlightInput()
                    assertEquals(MemberWizardStep.ready, model.draft.state.step)
                    assertEquals(0, source.count("review"))
                    model.revisit(MemberWizardStep.fineTune)
                    model.finishFineTune()
                    assertEquals(MemberWizardStep.ready, model.draft.state.step)
                    assertEquals(0, source.count("review"))
                }
            }

        @Test fun optionalAnswerIsScopedAndContinueReReviews() =
            runTest(timeout = 60.seconds) {
                withCompletion(optional = true) {
                    model.revisit(MemberWizardStep.fineTune)
                    val seat = model.optionalPrompts.first { it.kind == PromptKind.SeatSide }
                    model.openPrompt(seat.id)
                    model.answerOptional(seat, mapOf("seat_window" to JsonPrimitive("right")))
                    assertEquals(
                        "right",
                        model.draft.form.legs[0]
                            .seatWindow,
                    )
                    assertNull(model.draft.state.promptSheetID)
                    assertTrue(model.needsReadyReview)
                    model.finishFineTune()
                    awaitWizard { model.draft.state.step == MemberWizardStep.ready && !model.reviewing }
                    assertEquals(1, source.count("review"))
                    assertEquals(
                        "right",
                        source.reviewInputs
                            .last()
                            .input.outbound.legs[0]
                            .seatWindow,
                    )
                    val caffeine = review.prompts.first { it.kind == PromptKind.Cups }
                    val input =
                        TripGenerationAnswers.applyingAnswers(
                            model.reviewInput,
                            review,
                            mapOf(
                                caffeine.id to
                                    mapOf(
                                        "caffeine_habit" to
                                            JsonPrimitive(
                                                "cups",
                                            ),
                                        "cups_tea" to
                                            JsonPrimitive(
                                                2,
                                            ),
                                    ),
                            ),
                        )
                    assertEquals(JsonPrimitive(2), input.profile?.get("cups_tea"))
                }
            }

        @Test fun fineTuneWithoutChangesOpensReadyAndErrorsWaitForManualRetry() =
            runTest(
                timeout = 60.seconds,
            ) {
                withCompletion(optional = true) {
                    model.revisit(MemberWizardStep.fineTune)
                    model.finishFineTune()
                    assertEquals(MemberWizardStep.ready, model.draft.state.step)
                    model.revisit(MemberWizardStep.fineTune)
                    model.editForm { it.copy(preparation = it.preparation.copy(days = 0)) }
                    source.configure(reviewFailure = DataFailure.Offline)
                    model.finishFineTune()
                    awaitWizard { model.checking.failure == DataFailure.Offline }
                    model.resumeChecking()
                    assertEquals(1, source.count("review"))
                    assertEquals(MemberWizardStep.checking, model.draft.state.step)
                }
            }

        @Test fun saveFailureDoesNotRetryAndManualRetryKeepsIdentity() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val identity = model.draft.form.identity
                    source.configure(saveFailure = DataFailure.Offline)
                    model.savePlan()
                    awaitWizard {
                        model.draft.state.saveFailure
                            ?.kind == WizardSaveFailure.Kind.offline
                    }
                    repeat(10) { model.readyTick() }
                    assertEquals(MemberWizardStep.ready, model.draft.state.step)
                    assertEquals(identity, model.draft.form.identity)
                    assertEquals(1, source.count("save"))
                    assertEquals(0, source.count("review"))
                    source.configure()
                    model.savePlan()
                    awaitWizard { model.createdTripID == "created" }
                    assertEquals(2, source.count("save"))
                    assertNull(store.recover())
                }
            }

        @Test fun failedSaveStillAllowsTheThirtyMinuteReviewRefresh() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = DataFailure.Offline)
                    model.savePlan()
                    awaitWizard { model.draft.state.saveFailure != null }
                    clock.advance(1800.0)
                    model.readyTick()
                    awaitWizard { !model.reviewing && model.draft.state.saveFailure == null }
                    assertEquals(1, source.count("review"))
                    assertEquals(1, source.count("save"))
                }
            }

        @Test fun throttleCountdownUsesCeilingAndGuardsManualRetry() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = DataFailure.Throttled(42.0, 429))
                    model.savePlan()
                    awaitWizard { model.retrySeconds == 42 }
                    model.savePlan()
                    clock.advance(41.01)
                    model.readyTick()
                    assertEquals(1, model.retrySeconds)
                    assertFalse(model.canSave)
                    assertEquals(1, source.count("save"))
                    clock.advance(0.99)
                    model.readyTick()
                    assertEquals(0, model.retrySeconds)
                    assertTrue(model.canSave)
                    assertEquals(1, source.count("save"))
                    source.configure()
                    model.savePlan()
                    awaitWizard { model.createdTripID == "created" }
                }
            }

        @Test fun returningToForegroundCanRefreshNoCreditsWithoutSaving() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val unavailable =
                        review.copy(
                            canSave = false,
                            price =
                                review.price?.copy(
                                    balance = 0,
                                ),
                            saveBlockedReason = "Not enough credits",
                        )
                    source.queueReviews(listOf(Result.success(unavailable)))
                    model.readyTick(true)
                    awaitWizard { !model.reviewing }
                    assertEquals(WizardCredits.unavailable, model.credits)
                    assertFalse(model.canSave)
                    model.savePlan()
                    assertEquals(0, source.count("save"))
                    model.readyTick(true)
                    awaitWizard { !model.reviewing }
                    assertEquals(WizardCredits.credits, model.credits)
                    assertTrue(model.canSave)
                    assertEquals(2, source.count("review"))
                    assertEquals(0, source.count("save"))
                }
            }

        @Test fun calculatingRejectsBackCloseAndDoubleSave() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val gate = GenerationGate()
                    try {
                        source.configure(beforeSave = { gate.wait() })
                        model.savePlan()
                        awaitWizard { source.count("save") == 1 }
                        val path = model.navigationPath
                        model.popNavigation(emptyList(), path)
                        model.go(MemberWizardStep.whereTo)
                        model.discard()
                        model.savePlan()
                        assertEquals(MemberWizardStep.calculating, model.draft.state.step)
                        assertFalse(model.closed)
                        assertEquals(1, source.count("save"))
                        gate.open()
                        awaitWizard { model.createdTripID == "created" }
                    } finally {
                        gate.open()
                    }
                }
            }
    }

    @RunWith(Parameterized::class)
    class Expiry(
        private val failure: DataFailure,
    ) {
        @Test fun expiredOrCreditFailureClearsIdentityBeforeReadyRecheck() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val gate = GenerationGate()
                    try {
                        source.configure(saveFailure = failure, beforeReview = { gate.wait() })
                        model.savePlan()
                        awaitWizard { model.reviewing }
                        assertEquals(MemberWizardStep.ready, model.draft.state.step)
                        assertNull(model.draft.form.identity)
                        assertNull(model.draft.state.savedTripId)
                        assertNull(model.draft.state.saveFailure)
                        gate.open()
                        awaitWizard { !model.reviewing }
                        assertEquals(1, source.count("review"))
                        assertEquals(1, source.count("save"))
                        assertNull(source.reviewInputs.last().identity)
                    } finally {
                        gate.open()
                    }
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "{0}",
            )
            fun failures(): List<Array<Any>> =
                listOf(
                    DataFailure.ReviewExpired,
                    DataFailure.NotEnoughCredits,
                ).map {
                    arrayOf(
                        it,
                    )
                }
        }
    }
}
