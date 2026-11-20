package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.wizard.expiredIdentity
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.wizardPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class TripGenerationRecoveryTests {
    class Recovery {
        @Test fun localSavedIDWriteFailureRetainsIdentityAcrossServiceRecreation() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    val initial = generationState("write-failure")
                    val failed = service.save(initial) { throw StoreFailure.Unavailable() }
                    assertEquals(TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null)), failed.outcome)
                    assertEquals("created", failed.state.savedTripId)
                    assertEquals(initial.identity, failed.state.identity)
                    assertTrue(probe.pulls.isEmpty())
                    val restarted = TripGenerationService(source, probe.effects())
                    try {
                        assertEquals(
                            TripGenerationSaveResult.Outcome.Saved("created"),
                            restarted.save(failed.state).outcome,
                        )
                        assertEquals(1, source.count("save"))
                        assertEquals(listOf("created"), probe.pulls)
                    } finally {
                        restarted.cancel()
                    }
                }
            }

        @Test fun expiredReviewRetriesOnceWithoutIdentity() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    val fresh = generationReview()
                    source.queueReviews(listOf(Result.failure(DataFailure.ReviewExpired), Result.success(fresh)))
                    val input =
                        ApiTripReviewInput(
                            generationInput(),
                            expiredIdentity(),
                            JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))),
                        )
                    val result = service.review("one", input)
                    assertEquals(listOf(input.identity, null), source.reviewInputs.map { it.identity })
                    assertTrue(source.reviewInputs.all { it.input == input.input && it.profile == input.profile })
                    assertEquals(fresh, result.review)
                    assertEquals(fresh.identity, result.input.identity)
                    assertEquals(1, probe.merges)
                    assertEquals(1, probe.syncs)
                }
            }

        @Test fun secondExpirySurfacesTypedWithoutAnotherRetry() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, probe, service ->
                    source.queueReviews(
                        listOf(
                            Result.failure(DataFailure.ReviewExpired),
                            Result.failure(DataFailure.ReviewExpired),
                        ),
                    )
                    assertEquals(
                        DataFailure.ReviewExpired,
                        generationFailure {
                            service.review(
                                "one",
                                ApiTripReviewInput(
                                    generationInput(),
                                    expiredIdentity(),
                                ),
                            )
                        },
                    )
                    assertEquals(listOf(expiredIdentity(), null), source.reviewInputs.map { it.identity })
                    assertEquals(0, probe.merges)
                    assertEquals(0, probe.syncs)
                }
            }

        @Test fun editExpiryRefreshesSourceAndReturnsUpdatedDraftForSave() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val updated = generationInput().copy(preparationDays = 1)
                    source.queueSnapshots(
                        listOf(
                            ApiTripInputSnapshot(
                                generationInput(),
                                "old",
                                emptyMap(),
                                null,
                            ),
                            ApiTripInputSnapshot(
                                updated,
                                "new",
                                emptyMap(),
                                null,
                            ),
                        ),
                    )
                    val fresh = generationReview().let { it.copy(identity = it.identity.copy(sourceRevision = "new")) }
                    source.queueReviews(listOf(Result.failure(DataFailure.ReviewExpired), Result.success(fresh)))
                    val input =
                        ApiTripReviewInput(
                            generationInput().copy(preparationDays = 2),
                            expiredIdentity(),
                            JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))),
                        )
                    val result = service.review("one", input, TripEditContext("trip", "old"))
                    assertEquals(listOf("input", "review", "input", "review"), source.operations)
                    assertEquals(listOf("old", "new"), source.editRequests.map { it.sourceRevision })
                    assertEquals(input, source.reviewInputs[0])
                    assertNull(source.reviewInputs[1].identity)
                    assertEquals(updated, source.reviewInputs[1].input)
                    assertNull(source.reviewInputs[1].profile)
                    assertNull(result.input.profile)
                    assertEquals(updated, result.input.input)
                    assertEquals(fresh.identity, result.input.identity)
                    assertEquals(TripEditContext("trip", "new"), result.edit)
                    assertNotNull(result.sourceSnapshot)
                    val saved = service.save(TripGenerationState("one", result.input, result.edit, result.review))
                    assertEquals(TripGenerationSaveResult.Outcome.Saved("trip"), saved.outcome)
                    assertEquals(result.edit, source.lastEdit)
                }
            }

        @Test fun editRefreshesChangedSourceBeforeFirstReview() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val updated = generationInput().copy(preparationDays = 1)
                    source.queueSnapshots(listOf(ApiTripInputSnapshot(updated, "new", emptyMap(), null)))
                    val result =
                        service.review(
                            "one",
                            ApiTripReviewInput(
                                generationInput(),
                                expiredIdentity(),
                            ),
                            TripEditContext(
                                "trip",
                                "old",
                            ),
                        )
                    assertEquals(updated, source.reviewInputs.first().input)
                    assertNull(source.reviewInputs.first().identity)
                    assertEquals("new", result.edit?.sourceRevision)
                    assertEquals(listOf("input", "review"), source.operations)
                }
            }

        @Test fun answeringPromptsAfterExpiryReturnsFreshIdentity() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val fresh = generationReview()
                    source.queueReviews(
                        listOf(
                            Result.failure(DataFailure.ReviewExpired),
                            Result.success(fresh),
                            Result.success(fresh),
                        ),
                    )
                    val stale = fresh.copy(identity = expiredIdentity(), prompts = listOf(wizardPrompt("age")))
                    val result =
                        service.answerPrompts(
                            "one",
                            ApiTripReviewInput(generationInput()),
                            stale,
                            mapOf("age" to mapOf("age_band" to JsonPrimitive("18_64"))),
                        )
                    assertEquals(fresh.identity, result.input.identity)
                    assertEquals(JsonPrimitive("18_64"), result.input.profile?.get("age_band"))
                    service.review("one", result.input)
                    assertEquals(listOf(stale.identity, null, fresh.identity), source.reviewInputs.map { it.identity })
                    assertTrue(source.reviewInputs.all { it.profile == result.input.profile })
                }
            }
    }

    @RunWith(Parameterized::class)
    class CleanupFailures(
        private val failure: DataFailure,
    ) {
        @Test fun localCleanupFailureCannotInvalidateCreatedTrip() =
            runTest(timeout = 60.seconds) {
                withGeneration { _, _, service ->
                    val result = service.save(generationState("write-failure")) { throw failure }
                    assertEquals("created", result.state.savedTripId)
                    assertNotNull(result.state.identity)
                    assertEquals(TripGenerationSaveResult.Outcome.Failed(failure), result.outcome)
                }
            }

        @Test fun saveRecoveryInputReviewsWithoutDeadIdentity() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    source.configure(saveFailure = failure)
                    val input =
                        ApiTripReviewInput(
                            generationInput(),
                            profile = JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))),
                        )
                    val saved = service.save(TripGenerationState("one", input, review = generationReview()))
                    assertEquals(
                        TripGenerationSaveResult.Outcome.ReReviewNeeded(
                            if (failure == DataFailure.ReviewExpired) {
                                TripGenerationSaveResult.Reason.reviewExpired
                            } else {
                                TripGenerationSaveResult.Reason.notEnoughCredits
                            },
                        ),
                        saved.outcome,
                    )
                    assertNull(saved.state.review)
                    assertNull(saved.state.input.identity)
                    assertEquals(input.input, saved.state.input.input)
                    assertEquals(input.profile, saved.state.input.profile)
                    service.review(saved.state.instance, saved.state.input, saved.state.edit)
                    assertEquals(listOf(null), source.reviewInputs.map { it.identity })
                    assertEquals(1, source.count("save"))
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {0}")
            fun failures(): List<Array<Any>> =
                listOf(
                    DataFailure.ReviewExpired,
                    DataFailure.NotEnoughCredits,
                ).map { arrayOf(it) }
        }
    }

    @RunWith(Parameterized::class)
    class RevisionMismatch(
        private val repeatedMismatch: Boolean,
    ) {
        @Test fun editRevisionMismatchReviewsAgainAndRejectsRepeatedMismatch() =
            runTest(timeout = 60.seconds) {
                withGeneration { source, _, service ->
                    val updated = generationInput().copy(preparationDays = 1)
                    source.queueSnapshots(
                        listOf(
                            ApiTripInputSnapshot(
                                generationInput(),
                                "old",
                                emptyMap(),
                                null,
                            ),
                            ApiTripInputSnapshot(
                                updated,
                                "new",
                                emptyMap(),
                                null,
                            ),
                        ),
                    )
                    val mismatched =
                        generationReview().let {
                            it.copy(identity = it.identity.copy(sourceRevision = "other"))
                        }
                    val fresh =
                        generationReview().let {
                            val revision = if (repeatedMismatch) "other" else "new"
                            it.copy(identity = it.identity.copy(sourceRevision = revision))
                        }
                    source.queueReviews(listOf(Result.success(mismatched), Result.success(fresh)))
                    val input = ApiTripReviewInput(generationInput(), expiredIdentity())
                    if (repeatedMismatch) {
                        assertEquals(
                            DataFailure.ReviewExpired,
                            generationFailure {
                                service.review(
                                    "one",
                                    input,
                                    TripEditContext(
                                        "trip",
                                        "old",
                                    ),
                                )
                            },
                        )
                    } else {
                        val result = service.review("one", input, TripEditContext("trip", "old"))
                        assertEquals(fresh, result.review)
                        assertEquals(updated, result.input.input)
                        assertEquals("new", result.edit?.sourceRevision)
                    }
                    assertEquals(listOf("input", "review", "input", "review"), source.operations)
                    assertEquals(listOf(input.identity, null), source.reviewInputs.map { it.identity })
                    assertEquals(listOf("old", "new"), source.editRequests.map { it.sourceRevision })
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "repeatedMismatch={0}")
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf(false),
                    arrayOf(true),
                )
        }
    }
}
