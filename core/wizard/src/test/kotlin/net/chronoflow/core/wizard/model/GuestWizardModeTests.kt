@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.wizard.wizardAirport
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
class GuestWizardModeTests {
    class Codec {
        @Test fun legacyStateDecodesAsMemberAndModeRoundTrips() {
            val state = MemberWizardState()
            val encoded = ApiJson.response.encodeToJsonElement(MemberWizardState.serializer(), state) as JsonObject
            assertEquals(
                WizardMode.member,
                ApiJson.response
                    .decodeFromString<MemberWizardState>(
                        JsonObject(encoded - "mode").toString(),
                    ).mode,
            )
            val guest = state.copy(mode = WizardMode.guest)
            assertEquals(
                guest,
                ApiJson.response.decodeFromString<MemberWizardState>(ApiJson.response.encodeToString(guest)),
            )
        }
    }

    class GuestPaths {
        @Test fun questionsRowReopensSupportedAnswersAndReturnsToReady() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true, ready = true)
                val model = fixture.model
                model.state =
                    model.state.copy(
                        profile =
                            JsonObject(
                                model.state.profile +
                                    mapOf("age_band" to JsonPrimitive("18_64"), "shift_work" to JsonPrimitive(false)),
                            ),
                    )
                assertEquals(2, model.guestAnsweredQuestionCount)
                model.revisitGuestQuestions()
                assertEquals(MemberWizardStep.question, model.state.step)
                assertEquals(0, model.state.questionIndex)
                assertEquals(listOf(listOf("age_band"), listOf("shift_work")), model.state.questions?.map { it.fields })
                assertEquals(JsonPrimitive("18_64"), model.state.profile["age_band"])
                assertEquals(JsonPrimitive(false), model.state.profile["shift_work"])
                assertEquals(true, model.state.returnToReady)
                fixture.close()
            }

        @Test fun readyWaitsForTheSignupSheetAndDismissalAllowsAnotherRequest() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true, ready = true)
                val model = fixture.model
                model.savePlan()
                assertEquals(WizardSaveOutcome.signUpRequested, model.saveOutcome)
                model.setGuestAuthenticationPresented(true)
                assertNull(model.saveOutcome)
                model.state = model.state.copy(reviewedAt = null)
                model.readyTick(true)
                model.savePlan()
                assertEquals(MemberWizardStep.ready, model.state.step)
                assertNull(model.saveOutcome)
                model.setGuestAuthenticationPresented(false)
                model.readyTick()
                assertEquals(MemberWizardStep.checking, model.state.step)
                fixture.close()
            }

        @Test fun guestPathBackTargetsAndSleepAreMandatory() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true)
                val model = fixture.model
                model.go(MemberWizardStep.outbound)
                model.finishOutboundStep()
                assertEquals(MemberWizardStep.sleep, model.state.step)
                assertEquals(MemberWizardStep.sleep, model.lastInputStep)
                assertEquals(
                    listOf(MemberWizardDestination.outbound, MemberWizardDestination.sleep),
                    model.navigationPath,
                )
                model.go(MemberWizardStep.checking)
                var path = model.navigationPath
                model.popNavigation(path.dropLast(1), path)
                assertEquals(MemberWizardStep.sleep, model.state.step)
                model.state = model.state.copy(questions = listOf(wizardPrompt()))
                model.go(MemberWizardStep.question)
                assertEquals(
                    listOf(
                        MemberWizardDestination.outbound,
                        MemberWizardDestination.sleep,
                        MemberWizardDestination.question(0),
                    ),
                    model.navigationPath,
                )
                path = model.navigationPath
                model.popNavigation(path.dropLast(1), path)
                assertEquals(MemberWizardStep.sleep, model.state.step)
                model.go(MemberWizardStep.question)
                model.backFromQuestion()
                assertEquals(MemberWizardStep.sleep, model.state.step)
                model.state = model.state.copy(sleepCompleted = false)
                model.beginChecking()
                assertEquals(MemberWizardStep.sleep, model.state.step)
                model.editForm { it.addReturn().copy(override = WizardSleepOverride(enabled = true)) }
                assertEquals("one_way", model.form.end.kind)
                assertEquals(1, model.form.legs.size)
                assertFalse(model.form.override.enabled)
                assertTrue(model.state.sleepNeeded)
                assertFalse(model.showsReturnRow)
                assertFalse(model.allowsSleepOverride)
                fixture.close()
            }

        @Test fun liveReadyRequestsSignUpWithoutSaving() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true, ready = true)
                fixture.clock.advance(1799.999)
                val model = fixture.model
                model.savePlan()
                assertEquals(WizardSaveOutcome.signUpRequested, model.saveOutcome)
                assertEquals(MemberWizardStep.ready, model.state.step)
                assertFalse(model.closed)
                assertNull(model.createdTripID)
                assertNull(model.state.savedTripId)
                assertEquals(0, fixture.generation.saves)
                fixture.close()
            }

        @Test fun exclusionResetKeepsSleepAndPreferences() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true)
                val model = fixture.model
                model.state =
                    model.state.copy(
                        profile =
                            JsonObject(
                                model.state.profile +
                                    mapOf(
                                        "age_band" to JsonPrimitive("under_18"),
                                        "shift_work" to JsonPrimitive(true),
                                        "exclude_caffeine" to JsonPrimitive(true),
                                    ),
                            ),
                    )
                model.go(MemberWizardStep.excluded)
                model.changeExcludedAnswers()
                assertEquals(JsonNull, model.state.profile["age_band"])
                assertEquals(JsonNull, model.state.profile["shift_work"])
                assertEquals(JsonPrimitive("23:00"), model.state.profile["habitual_bed"])
                assertEquals(JsonPrimitive(true), model.state.profile["exclude_caffeine"])
                assertEquals(MemberWizardStep.question, model.state.step)
                assertEquals(listOf("age_band"), model.currentQuestion?.fields)
                fixture.close()
            }

        @Test fun guestPastArrivalCopyAndMemberNavigationStayDistinct() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true)
                val model = fixture.model
                assertTrue(
                    WizardFlightRules
                        .errors(
                            model.form,
                            emptyMap(),
                            Instant.parse("2100-01-01T00:00:00Z"),
                            true,
                            WizardMode.guest,
                        ).isEmpty(),
                )
                val airports =
                    mapOf(
                        "AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"),
                        "HND" to wizardAirport("HND", "Tokyo", "Asia/Tokyo"),
                    )
                assertEquals(
                    "A new plan needs an upcoming arrival. Check your flight dates.",
                    WizardFlightRules
                        .errors(
                            model.form,
                            airports,
                            Instant.parse("2100-01-01T00:00:00Z"),
                            true,
                            WizardMode.guest,
                        )["review"]
                        ?.key,
                )
                model.state = model.state.copy(mode = WizardMode.member)
                model.go(MemberWizardStep.outbound)
                model.finishOutboundStep()
                assertEquals(MemberWizardStep.returnChoice, model.state.step)
                assertEquals(
                    listOf(MemberWizardDestination.outbound, MemberWizardDestination.returnChoice),
                    model.navigationPath,
                )
                assertTrue(model.showsReturnRow)
                assertTrue(model.allowsSleepOverride)
                fixture.close()
            }
    }

    @RunWith(Parameterized::class)
    class Invalidations(
        private val reason: String,
    ) {
        @Test fun readyRechecksEveryInvalidation() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true, ready = true)
                val model = fixture.model
                assertFalse(model.needsReadyReview)
                when (reason) {
                    "identity" -> {
                        model.form = model.form.copy(identity = null)
                    }

                    "emptyToken" -> {
                        model.form = model.form.copy(identity = model.form.identity?.copy(token = ""))
                    }

                    "failure" -> {
                        model.checking = model.checking.copy(failure = DataFailure.Offline)
                    }

                    "saveFailure" -> {
                        model.state =
                            model.state.copy(saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.server))
                    }

                    "fingerprint" -> {
                        model.editForm { it.copy(preparation = it.preparation.copy(days = 1)) }
                    }

                    "expired" -> {
                        fixture.clock.advance(1800.0)
                    }

                    else -> {
                        model.state = model.state.copy(reviewedAt = null)
                    }
                }
                assertTrue(model.needsReadyReview)
                model.savePlan()
                assertEquals(MemberWizardStep.checking, model.state.step)
                assertNull(model.saveOutcome)
                assertEquals(true, model.state.returnToReady)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "reason={0}")
            fun cases() =
                listOf("identity", "emptyToken", "failure", "saveFailure", "fingerprint", "expired", "unchecked")
        }
    }

    class BoardProgress {
        @Test fun guestWeightsAndForwardProgressFloor() =
            runTest(timeout = 60.seconds) {
                // 06 §11.1: guest progress follows the board values.
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true)
                val model = fixture.model
                model.form = model.form.updateLeg(0) { it.copy(toIata = "") }
                assertEquals(0.09, model.progress, 0.0)
                model.form = model.form.updateLeg(0) { it.copy(toIata = "HND") }
                assertEquals(0.09, model.progress, 0.0)
                model.go(MemberWizardStep.outbound)
                assertEquals(0.18, model.progress, 0.0)
                model.go(MemberWizardStep.sleep)
                assertEquals(0.30, model.progress, 0.0)
                model.go(MemberWizardStep.checking)
                assertEquals(0.42, model.progress, 0.0)
                model.go(MemberWizardStep.question)
                assertEquals(0.54, model.progress, 0.0)
                model.state = model.state.copy(questionIndex = 1)
                assertEquals(0.64, model.progress, 0.0)
                model.state = model.state.copy(checkingAnswers = true)
                model.go(MemberWizardStep.checking)
                assertEquals(0.64, model.progress, 0.0)
                model.go(MemberWizardStep.fineTune)
                assertEquals(0.86, model.progress, 0.0)
                model.go(MemberWizardStep.ready)
                assertEquals(0.95, model.progress, 0.0)
                model.go(MemberWizardStep.checking)
                assertEquals(0.95, model.progress, 0.0)
                fixture.close()
            }
    }
}
