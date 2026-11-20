@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.testing.wizard.EffectProbe
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.expiredIdentity
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.wizardPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class TripGenerationTests {
    class Operations {
        @Test fun reviewDeduplicatesOnlyMatchingInstanceFingerprintTokenAndRevision() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    val gate = GenerationGate()
                    source.configure(beforeReview = { gate.wait() })
                    val original =
                        ApiTripReviewInput(
                            generationInput(),
                            profile = JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))),
                        )
                    val first = async { service.review("one", original) }
                    source.waitForReviews(1)
                    val duplicate = async { service.review("one", original) }
                    runCurrent()
                    val differentInstance = async { service.review("two", original) }
                    source.waitForReviews(2)
                    val changed = original.copy(input = original.input.copy(preparationDays = 2))
                    val differentInput = async { service.review("one", changed) }
                    source.waitForReviews(3)
                    val token = changed.copy(identity = expiredIdentity().copy(token = "token"))
                    val differentToken = async { service.review("one", token) }
                    source.waitForReviews(4)
                    val differentRevision =
                        async {
                            service.review(
                                "one",
                                token.copy(identity = token.identity!!.copy(reviewRevision = 2)),
                            )
                        }
                    source.waitForReviews(5)
                    gate.open()
                    assertEquals(
                        6,
                        awaitAll(
                            first,
                            duplicate,
                            differentInstance,
                            differentInput,
                            differentToken,
                            differentRevision,
                        ).size,
                    )
                    assertEquals(5, source.count("review"))
                    assertEquals(5, probe.merges)
                    assertEquals(5, probe.syncs)
                    service.review("one", original)
                    assertEquals(6, source.count("review"))
                }
            }

        @Test fun reviewDeadlineCancelsRequestAndDoesNotRetry() =
            runTest(timeout = 60.seconds) {
                val source = GenerationSource()
                source.configure(beforeReview = { awaitCancellation() })
                val probe = EffectProbe()
                val service =
                    TripGenerationService(
                        source,
                        probe.effects(),
                        sleep = {
                            assertEquals(
                                30.0,
                                it,
                                0.0,
                            )
                            ; source.waitForReviews(1)
                        },
                    )
                try {
                    assertEquals(
                        DataFailure.Server(null),
                        generationFailure {
                            service.review(
                                "one",
                                ApiTripReviewInput(generationInput()),
                            )
                        },
                    )
                    assertEquals(1, source.count("review"))
                    assertEquals(0, probe.merges)
                    assertEquals(0, probe.syncs)
                } finally {
                    service.cancel()
                }
            }

        @Test fun saveCachesIdentityAndCallerSavedTripIdSkipsPostEvenWhenPullFails() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    probe.failPull()
                    val initial = generationState()
                    val first = service.save(initial)
                    assertEquals(TripGenerationSaveResult.Outcome.Saved("created"), first.outcome)
                    assertEquals("created", first.state.savedTripId)
                    assertEquals(first.outcome, service.save(initial).outcome)
                    val recovered = TripGenerationState("recovered", initial.input, savedTripId = "already-saved")
                    assertEquals(
                        TripGenerationSaveResult.Outcome.Saved("already-saved"),
                        service.save(recovered).outcome,
                    )
                    assertEquals(1, source.count("save"))
                    assertEquals(listOf("created", "created", "already-saved"), probe.pulls)
                    assertEquals(3, probe.syncs)
                }
            }

        @Test fun concurrentSaveUsesOnePostAndEditUsesUpdate() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val gate = GenerationGate()
                    source.configure(beforeSave = { gate.wait() })
                    val state = generationState()
                    val first = async { service.save(state) }
                    source.waitForSaves(1)
                    val second = async { service.save(state) }
                    runCurrent()
                    gate.open()
                    assertEquals(first.await().outcome, second.await().outcome)
                    assertEquals(1, source.count("save"))
                    val edit = TripEditContext("trip", "revision")
                    service.review("edit", state.input, edit)
                    assertEquals(edit, source.lastEdit)
                    service.save(generationState("edit", edit))
                    assertEquals(1, source.count("update"))
                }
            }

        @Test fun failedReviewMakesARequestThenCallerMayRetry() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    source.configure(reviewFailure = DataFailure.Offline)
                    assertEquals(
                        DataFailure.Offline,
                        generationFailure {
                            service.review(
                                "one",
                                ApiTripReviewInput(generationInput()),
                            )
                        },
                    )
                    assertEquals(1, source.count("review"))
                    source.configure()
                    service.review("one", ApiTripReviewInput(generationInput()))
                    assertEquals(2, source.count("review"))
                }
            }

        @Test fun answeringPromptsPreservesExplicitNulls() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val input = generationInput()
                    val review =
                        generationReview().copy(
                            prompts =
                                listOf(
                                    wizardPrompt(
                                        id = "busy",
                                        kind = PromptKind.PreDepartureBusyHours,
                                        blocking = false,
                                        fields = listOf("pre_departure_busy_answered"),
                                        target = ReviewPromptTarget.Trip,
                                    ),
                                ),
                        )
                    val result =
                        service.answerPrompts(
                            "one",
                            ApiTripReviewInput(input),
                            review,
                            mapOf("busy" to mapOf("pre_departure_busy_answered" to JsonPrimitive(true))),
                        )
                    val expected =
                        (
                            ApiJson.request.encodeToJsonElement(
                                ApiTripInput.serializer(),
                                input,
                            ) as JsonObject
                        ).toMutableMap()
                    expected["pre_departure_busy_answered"] = JsonPrimitive(true)
                    assertEquals(
                        JsonObject(expected),
                        ApiJson.request.encodeToJsonElement(
                            ApiTripInput.serializer(),
                            source.reviewInputs.first().input,
                        ),
                    )
                    assertEquals(source.reviewInputs.first().input, result.input.input)
                    assertTrue(expected.values.contains(JsonNull))
                }
            }

        @Test fun answersFollowReturnLegStayAndTopLevelScopes() =
            runTest(timeout = 60.seconds) {
                withGeneration { _, _, service ->
                    val original = generationInput()
                    val input = original.copy(`return` = original.outbound)
                    val scope = ApiReviewPromptScope(TripDirection.Return, 0, 0)
                    val review =
                        generationReview().copy(
                            prompts =
                                listOf(
                                    wizardPrompt(
                                        "seat",
                                        PromptKind.SeatSide,
                                        false,
                                        listOf("seat_window"),
                                        ReviewPromptTarget.Trip,
                                        scope = scope,
                                    ),
                                    wizardPrompt(
                                        "room",
                                        PromptKind.Room,
                                        false,
                                        listOf("stay.bed_available_mode"),
                                        ReviewPromptTarget.Trip,
                                        scope = scope,
                                    ),
                                    wizardPrompt(
                                        "busy",
                                        PromptKind.PreDepartureBusyHours,
                                        false,
                                        listOf(
                                            "pre_departure_busy_answered",
                                            "pre_departure_busy_intervals",
                                        ),
                                        ReviewPromptTarget.Trip,
                                    ),
                                ),
                        )
                    val intervals =
                        ApiJson.request.parseToJsonElement(
                            """[{"start_local":"2026-11-09T09:00","end_local":"2026-11-09T10:00"}]""",
                        )
                    val result =
                        service.answerPrompts(
                            "one",
                            ApiTripReviewInput(input),
                            review,
                            mapOf(
                                "seat" to mapOf("seat_window" to JsonPrimitive("right")),
                                "room" to mapOf("stay.bed_available_mode" to JsonPrimitive("from_time")),
                                "busy" to
                                    mapOf(
                                        "pre_departure_busy_answered" to JsonPrimitive(true),
                                        "pre_departure_busy_intervals" to intervals,
                                    ),
                            ),
                        )
                    assertEquals(
                        "right",
                        result.input.input.`return`!!
                            .legs[0]
                            .seatWindow,
                    )
                    assertEquals(
                        "from_time",
                        result.input.input.`return`!!
                            .legs[0]
                            .stay.bedAvailableMode,
                    )
                    assertNull(
                        result.input.input.outbound.legs[0]
                            .seatWindow,
                    )
                    assertTrue(result.input.input.preDepartureBusyAnswered)
                    assertEquals(1, result.input.input.preDepartureBusyIntervals.size)
                    assertEquals(result.review.identity, result.input.identity)
                }
            }

        @Test fun cancellationStopsInFlightReviewAndEffects() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    source.configure(beforeReview = { awaitCancellation() })
                    val task =
                        backgroundScope.async {
                            service.review(
                                "one",
                                ApiTripReviewInput(
                                    generationInput(),
                                    profile = JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))),
                                ),
                            )
                        }
                    source.waitForReviews(1)
                    service.cancel()
                    assertTrue(generationFailure { task.await() } is CancellationException)
                    assertEquals(0, probe.merges)
                    assertEquals(0, probe.syncs)
                }
            }

        @Test fun cancelledWaiterDoesNotEvictOrCancelSharedReview() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val gate = GenerationGate()
                    source.configure(beforeReview = { gate.wait() })
                    val input = ApiTripReviewInput(generationInput())
                    val first = backgroundScope.async { service.review("one", input) }
                    source.waitForReviews(1)
                    first.cancelAndJoin()
                    val second = async { service.review("one", input) }
                    runCurrent()
                    gate.open()
                    second.await()
                    assertEquals(1, source.count("review"))
                }
            }
    }

    @RunWith(Parameterized::class)
    class SaveFailures(
        private val failure: DataFailure,
    ) {
        @Test fun saveFailuresKeepOrClearIdentityAndNeverRetry() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    source.configure(saveFailure = failure)
                    val initial = generationState()
                    val result = service.save(initial)
                    if (failure == DataFailure.ReviewExpired || failure == DataFailure.NotEnoughCredits) {
                        assertNull(result.state.identity)
                        assertNull(result.state.input.identity)
                        assertEquals(
                            TripGenerationSaveResult.Outcome.ReReviewNeeded(
                                if (failure == DataFailure.ReviewExpired) {
                                    TripGenerationSaveResult.Reason.reviewExpired
                                } else {
                                    TripGenerationSaveResult.Reason.notEnoughCredits
                                },
                            ),
                            result.outcome,
                        )
                    } else {
                        assertEquals(initial.identity, result.state.identity)
                        assertEquals(TripGenerationSaveResult.Outcome.Failed(failure), result.outcome)
                    }
                    assertEquals(1, source.count("save"))
                    assertEquals(0, probe.syncs)
                    source.configure()
                    assertEquals(TripGenerationSaveResult.Outcome.Saved("created"), service.save(initial).outcome)
                    assertEquals(2, source.count("save"))
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {0}")
            fun failures(): List<Array<Any>> =
                listOf(
                    DataFailure.ReviewExpired,
                    DataFailure.NotEnoughCredits,
                    DataFailure.Throttled(
                        31.0,
                        429,
                    ),
                    DataFailure.Offline,
                    DataFailure.SaveCapacity,
                    DataFailure.EmailUnverified(403),
                ).map { arrayOf(it) }
        }
    }
}
