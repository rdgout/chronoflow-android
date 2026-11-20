@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.withCompletion
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.beginChecking
import net.chronoflow.core.wizard.model.canSave
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.finishFlightInput
import net.chronoflow.core.wizard.model.readyTick
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
class WizardRetryTests {
    class Retry {
        @Test fun checkingThrottleClearsWhenLeavingAndDoesNotBlockContinue() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(reviewFailure = DataFailure.Throttled(30.0, 429))
                    model.go(MemberWizardStep.outbound)
                    model.beginChecking()
                    awaitWizard { model.retrySeconds == 30 }
                    model.go(MemberWizardStep.outbound)
                    assertEquals(0, model.retrySeconds)
                    assertNull(model.checking.failure)
                    assertNull(model.checking.retryAt)
                    source.configure()
                    model.beginChecking()
                    awaitWizard { source.count("review") == 2 }
                    assertEquals(MemberWizardStep.checking, model.draft.state.step)
                    assertNull(model.checking.failure)
                }
            }

        @Test fun saveThrottleTicksWithoutReadyViewAndExpires() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = DataFailure.Throttled(3.0, 429))
                    model.savePlan()
                    awaitWizard { model.retrySeconds == 3 && checking.waiterCount > 0 }
                    clock.advance(1.0)
                    checking.advance(1000)
                    assertEquals(2, model.retrySeconds)
                    assertFalse(model.canSave)
                    clock.advance(2.0)
                    checking.advance(3000)
                    assertEquals(0, model.retrySeconds)
                    assertNull(
                        model.draft.state.saveFailure
                            ?.retryAt,
                    )
                    assertTrue(model.canSave)
                    assertEquals(1, source.count("save"))
                }
            }

        @Test fun reviewClearsActiveSaveThrottleAndRevisitRestoresOnlyRemainingTime() =
            runTest(
                timeout = 60.seconds,
            ) {
                withCompletion {
                    source.configure(saveFailure = DataFailure.Throttled(42.0, 429))
                    model.savePlan()
                    awaitWizard { model.retrySeconds == 42 }
                    model.revisit(MemberWizardStep.fineTune)
                    clock.advance(40.0)
                    model.finishFineTune()
                    assertEquals(2, model.retrySeconds)
                    source.configure()
                    model.readyTick(true)
                    awaitWizard { !model.reviewing && model.draft.state.saveFailure == null }
                    assertEquals(0, model.retrySeconds)
                    assertTrue(model.canSave)
                    checking.advance(100_000)
                    assertEquals(0, model.retrySeconds)
                }
            }
    }

    @RunWith(Parameterized::class)
    class Inline(
        private val fromFineTune: Boolean,
    ) {
        @Test fun inlineReviewThrottleTicksAndAllowsManualRetry() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(reviewFailure = DataFailure.Throttled(3.0, 429))
                    if (fromFineTune) {
                        model.revisit(MemberWizardStep.fineTune)
                        model.editForm { it.copy(preparation = it.preparation.copy(days = 0)) }
                        model.finishFineTune()
                    } else {
                        model.readyTick(true)
                    }
                    awaitWizard {
                        model.draft.state.step == MemberWizardStep.checking && model.retrySeconds == 3 &&
                            checking.waiterCount > 0
                    }
                    model.beginChecking()
                    assertEquals(1, source.count("review"))
                    checking.advance(1000)
                    assertEquals(2, model.retrySeconds)
                    checking.advance(3000)
                    assertEquals(0, model.retrySeconds)
                    assertNull(model.checking.retryAt)
                    source.configure()
                    model.beginChecking()
                    awaitWizard { source.count("review") == 2 }
                    assertNull(model.checking.failure)
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "fromFineTune={0}",
            )
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf(
                        false,
                    ),
                    arrayOf(
                        true,
                    ),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Leaving(
        private val step: MemberWizardStep,
    ) {
        @Test fun leavingSaveThrottleAllowsChangedInputToContinue() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = DataFailure.Throttled(42.0, 429))
                    model.savePlan()
                    awaitWizard { model.retrySeconds == 42 }
                    model.revisit(step)
                    assertEquals(0, model.retrySeconds)
                    source.configure()
                    model.editForm { it.copy(preparation = it.preparation.copy(days = 0)) }
                    if (step == MemberWizardStep.fineTune) {
                        model.finishFineTune()
                    } else {
                        model.finishFlightInput()
                        awaitWizard { 1200L in checking.deadlines }
                        checking.advance(1200)
                        awaitWizard { 1500L in checking.deadlines }
                        checking.advance(1500)
                    }
                    awaitWizard { model.draft.state.step == MemberWizardStep.ready && !model.reviewing }
                    assertNull(model.draft.state.saveFailure)
                    assertEquals(0, model.retrySeconds)
                    assertTrue(model.canSave)
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "step={0}",
            )
            fun steps(): List<Array<Any>> =
                listOf(
                    MemberWizardStep.fineTune,
                    MemberWizardStep.whereTo,
                    MemberWizardStep.outbound,
                ).map {
                    arrayOf(
                        it,
                    )
                }
        }
    }

    @RunWith(Parameterized::class)
    class Updated(
        private val userEdited: Boolean,
    ) {
        @Test fun updatedOnlyDescribesServerChangesForUnchangedInput() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val changed = review.copy(price = review.price?.let { it.copy(cost = it.cost + 1) })
                    source.queueReviews(listOf(Result.success(changed)))
                    if (userEdited) {
                        model.revisit(MemberWizardStep.fineTune)
                        model.editForm { it.copy(preparation = it.preparation.copy(days = 0)) }
                        model.finishFineTune()
                    } else {
                        model.readyTick(true)
                    }
                    awaitWizard { !model.reviewing }
                    assertEquals(!userEdited, model.draft.state.updated)
                    model.revisit(MemberWizardStep.fineTune)
                    assertEquals(false, model.draft.state.updated)
                    model.finishFineTune()
                    assertEquals(false, model.draft.state.updated)
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "userEdited={0}",
            )
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf(
                        false,
                    ),
                    arrayOf(
                        true,
                    ),
                )
        }
    }
}
