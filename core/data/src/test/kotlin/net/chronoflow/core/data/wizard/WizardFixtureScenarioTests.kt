@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.finishCheck
import net.chronoflow.core.data.wizard.support.reviewFixture
import net.chronoflow.core.data.wizard.support.withCompletion
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardJourney
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.beginChecking
import net.chronoflow.core.wizard.model.credits
import net.chronoflow.core.wizard.model.readyTick
import net.chronoflow.core.wizard.model.savePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardFixtureScenarioTests {
    class Scenarios {
        @Test fun throttledReviewCountsDownFromFixture() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    f.source.configure(
                        reviewFailure =
                            fixtureFailure(
                                "throttled",
                                DataFailure.Throttled(
                                    42.0,
                                    429,
                                ),
                            ),
                    )
                    f.model.beginChecking()
                    awaitWizard { f.model.retrySeconds == 42 }
                    assertEquals(DataFailure.Throttled(42.0, 429), f.model.checking.failure)
                    f.source.configure()
                    f.model.beginChecking()
                    assertEquals(1, f.source.count("review"))
                    f.clock.advance(41_999)
                    f.model.beginChecking()
                    assertEquals(1, f.source.count("review"))
                    f.clock.advance(42_000)
                    assertEquals(0, f.model.retrySeconds)
                    f.model.beginChecking()
                    awaitWizard { f.source.count("review") == 2 }
                } finally {
                    f.close()
                }
            }

        @Test fun throttledSaveCountsDownWithoutRetry() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = fixtureFailure("throttled", DataFailure.Throttled(42.0, 429)))
                    model.savePlan()
                    awaitWizard { model.retrySeconds == 42 }
                    assertEquals(
                        WizardSaveFailure.Kind.throttle,
                        model.draft.state.saveFailure
                            ?.kind,
                    )
                    clock.advance(42.0)
                    checking.advance(42_000)
                    assertEquals(0, model.retrySeconds)
                    assertEquals(1, source.count("save"))
                }
            }

        @Test fun capacitySaveIsServerFailureWithoutRetry() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    source.configure(saveFailure = fixtureFailure("capacity", DataFailure.SaveCapacity))
                    model.savePlan()
                    awaitWizard { model.draft.state.saveFailure != null }
                    assertEquals(
                        WizardSaveFailure.Kind.server,
                        model.draft.state.saveFailure
                            ?.kind,
                    )
                    repeat(10) { model.readyTick() }
                    assertEquals(1, source.count("save"))
                }
            }

        @Test fun expiredSaveClearsIdentityAndReReviews() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val gate = GenerationGate()
                    try {
                        source.configure(
                            saveFailure =
                                fixtureFailure(
                                    "expired",
                                    DataFailure.ReviewExpired,
                                ),
                            beforeReview = {
                                gate.wait()
                            },
                        )
                        model.savePlan()
                        awaitWizard { model.reviewing && source.count("review") == 1 }
                        assertNull(model.draft.form.identity)
                        assertEquals(1, source.count("save"))
                    } finally {
                        gate.open()
                    }
                }
            }

        @Test fun creditsSaveClearsIdentityAndReReviews() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    val gate = GenerationGate()
                    try {
                        source.configure(
                            saveFailure =
                                fixtureFailure(
                                    "credits",
                                    DataFailure.NotEnoughCredits,
                                ),
                            beforeReview = {
                                gate.wait()
                            },
                        )
                        model.savePlan()
                        awaitWizard { model.reviewing && source.count("review") == 1 }
                        assertNull(model.draft.form.identity)
                        assertEquals(1, source.count("save"))
                    } finally {
                        gate.open()
                    }
                }
            }

        @Test fun doubleExpiredReviewShowsFailure() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val failure = fixtureFailure("expired", DataFailure.ReviewExpired)
                    f.source.queueReviews(listOf(Result.failure(failure), Result.failure(failure)))
                    f.model.beginChecking()
                    awaitWizard { f.model.checking.failure != null }
                    assertEquals(DataFailure.ReviewExpired, f.model.checking.failure)
                    assertEquals(2, f.source.count("review"))
                } finally {
                    f.close()
                }
            }

        @Test fun needsAnswersFixtureAsksAgeThenShift() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val review =
                        ApiJson.response.decodeFromString<ApiTripReview>(
                            Fixtures.text(
                                "wizard/needs-answers.json",
                            ),
                        )
                    f.source.queueReviews(listOf(Result.success(review)))
                    f.model.beginChecking()
                    finishCheck(f)
                    assertEquals(MemberWizardStep.question, f.model.draft.state.step)
                    assertEquals(
                        listOf(
                            "age_band:::",
                            "screening:shift_work::",
                        ),
                        f.model.draft.state.questions
                            ?.map { it.id },
                    )
                } finally {
                    f.close()
                }
            }

        @Test fun unknownFieldReturnsToOwningStep() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val fields = mapOf("legs.0.surprise" to FieldMessage("This setting cannot be changed here."))
                    f.source.configure(
                        reviewFailure =
                            fixtureFailure(
                                "unknown-field",
                                DataFailure.Validation(
                                    fields,
                                    422,
                                ),
                            ),
                    )
                    f.model.beginChecking()
                    awaitWizard {
                        f.model.draft.fieldErrors
                            .isNotEmpty()
                    }
                    assertEquals(fields["legs.0.surprise"], f.model.draft.fieldErrors["legs.0.surprise"])
                    assertEquals(
                        WizardJourney.errorStep(
                            f.model.draft.fieldErrors,
                            f.model.draft.form,
                        ),
                        f.model.draft.state.step,
                    )
                    assertTrue(f.model.draft.state.step != MemberWizardStep.checking)
                } finally {
                    f.close()
                }
            }
    }

    @RunWith(Parameterized::class)
    class NoAutomaticRetry(
        private val failure: DataFailure,
    ) {
        @Test fun saveIsNeverRetriedWithoutUserAction() =
            runTest(timeout = 60.seconds) {
                withCompletion {
                    if (failure == DataFailure.SaveCapacity) fixtureFailure("capacity", failure)
                    if (failure is DataFailure.Throttled) fixtureFailure("throttled", failure)
                    source.configure(saveFailure = failure)
                    model.savePlan()
                    awaitWizard { model.draft.state.saveFailure != null }
                    repeat(60) {
                        clock.advance(60.0)
                        checking.advance(checking.now + 60_000)
                        model.readyTick()
                        if (model.reviewing) awaitWizard { !model.reviewing }
                    }
                    assertEquals(1, source.count("save"))
                    assertEquals(0, model.retrySeconds)
                    source.configure()
                    model.savePlan()
                    awaitWizard { model.createdTripID == "created" }
                    assertEquals(2, source.count("save"))
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "{index}: {0}",
            )
            fun failures(): List<Array<Any>> =
                listOf(
                    DataFailure.Offline,
                    DataFailure.Server(
                        500,
                    ),
                    DataFailure.Throttled(
                        31.0,
                        429,
                    ),
                    DataFailure.SaveCapacity,
                    DataFailure.EmailUnverified(
                        403,
                    ),
                ).map {
                    arrayOf(
                        it,
                    )
                }
        }
    }
}

private fun fixtureFailure(
    name: String,
    failure: DataFailure,
): DataFailure {
    val document = ApiJson.response.parseToJsonElement(Fixtures.text("wizard/$name.json")).jsonObject
    assertTrue(document.containsKey("code"))
    return failure
}
