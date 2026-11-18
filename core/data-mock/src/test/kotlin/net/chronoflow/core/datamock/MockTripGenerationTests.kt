package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockTripGenerationTests {
    /** Combining marks do not consume a second query character (new). */
    @Test fun searchAirportQueryLengthCountsGraphemes() =
        runTest(timeout = 60.seconds) {
            assertTrue(mockSource().searchAirports("e\u0301".repeat(31), null).isEmpty())
        }

    /** A two-code-unit grapheme still falls below the two-character minimum (new). */
    @Test fun searchAirportsRejectsOneGrapheme() =
        runTest(timeout = 60.seconds) {
            assertEquals(
                DataFailure.Validation(mapOf("query" to FieldMessage("Choose one of the available options.")), 422),
                failure { mockSource().searchAirports("e\u0301", null) },
            )
        }

    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun unknownAirportIsAFieldErrorBeforePlanGenerationOrigin() =
        unknownAirportIsAFieldErrorBeforePlanGeneration(
            false,
        )

    @Test fun unknownAirportIsAFieldErrorBeforePlanGenerationDestination() =
        unknownAirportIsAFieldErrorBeforePlanGeneration(true)

    private fun unknownAirportIsAFieldErrorBeforePlanGeneration(destination: Boolean) =
        runTest(timeout = 60.seconds) {
            val source = wizardMockSource()
            login(source)
            val input =
                wizardInput().firstLeg {
                    if (destination) {
                        it.copy(
                            toIata = "NRT",
                        )
                    } else {
                        it.copy(fromIata = "NRT")
                    }
                }
            assertEquals(
                DataFailure.Validation(
                    mapOf(
                        (if (destination) "legs.0.toIata" else "legs.0.fromIata") to
                            FieldMessage("Choose an airport from the search results."),
                    ),
                    422,
                ),
                failure { source.reviewTrip(ApiTripReviewInput(input)) },
            )
        }

    @Test fun routeChoiceCreditsAndIdempotentReceiptSurviveRestoration() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = wizardMockSource(store)
            login(source)
            val before = source.fetchTrips().size
            val input = wizardInput()
            val review = source.reviewTrip(ApiTripReviewInput(input, profile = wizardProfile()))
            assertTrue(review.canSave)
            assertEquals(1, review.price?.cost)
            assertNotNull(review.summary)
            assertEquals(
                660,
                review.itinerary.legs
                    .first()
                    .flightMin,
            )
            val identity = ApiTripSaveInput.from(review.identity)
            val saved = source.saveTrip(identity)
            assertEquals(saved, source.saveTrip(identity))
            assertEquals(before + 1, source.fetchTrips().size)
            val detail = source.fetchTripDetail(saved.tripId).detail
            assertEquals("HND", detail.trip.to.iata)
            assertTrue(detail.plan.blocks.isNotEmpty())
            assertTrue(detail.plan.blocks.any { (it.params as? BlockParams.Flight)?.value?.toIata == "HND" })
            assertEquals(input, source.tripInput(saved.tripId).input)
            val balance = source.fetchAccount().settings.planCredits
            val restored = wizardMockSource(store)
            login(restored)
            assertEquals(saved, restored.saveTrip(identity))
            assertEquals(balance, restored.fetchAccount().settings.planCredits)
            val west = input.firstLeg { it.copy(toIata = "JFK", arriveLocal = "2026-11-10T14:00") }
            assertEquals(
                AdaptationDirection.Later,
                source
                    .reviewTrip(ApiTripReviewInput(west, profile = wizardProfile()))
                    .summary
                    ?.adaptation
                    ?.direction,
            )
        }

    @Test fun missingAnswersAgeAndScreeningExclusion() =
        runTest(timeout = 60.seconds) {
            val source = wizardMockSource()
            login(source)
            val first =
                source.reviewTrip(
                    ApiTripReviewInput(
                        wizardInput(),
                        profile =
                            JsonObject(
                                mapOf("age_band" to JsonNull, "shift_work" to JsonNull),
                            ),
                    ),
                )
            assertEquals(PlanStatus.NeedsAnswers, first.status)
            assertEquals(2, first.prompts.size)
            assertFalse(first.canSave)
            assertNull(first.price)
            assertEquals(PromptKind.AgeBand, first.prompts[0].kind)
            assertEquals(PromptKind.Screening, first.prompts[1].kind)
            val answered = source.reviewTrip(ApiTripReviewInput(wizardInput(), first.identity, wizardProfile()))
            assertEquals(first.identity.token, answered.identity.token)
            assertEquals(2, answered.identity.inputRevision)
            assertEquals(PlanStatus.Ok, answered.status)
            assertTrue(answered.canSave)
            val excluded =
                source.reviewTrip(
                    ApiTripReviewInput(
                        wizardInput(),
                        profile =
                            JsonObject(
                                mapOf("shift_work" to JsonPrimitive(true)),
                            ),
                    ),
                )
            assertEquals(PlanStatus.NoPlan, excluded.status)
            assertTrue(excluded.prompts.isEmpty())
            assertNull(excluded.summary)
            assertNull(excluded.price)
            val underage =
                source.reviewTrip(
                    ApiTripReviewInput(
                        wizardInput(),
                        profile =
                            JsonObject(
                                mapOf("age_band" to JsonPrimitive("under_18")),
                            ),
                    ),
                )
            assertEquals(PlanStatus.NoPlan, underage.status)
            assertFalse(underage.canSave)
            assertNull(underage.price)
            assertEquals(
                DataFailure.ReviewExpired,
                failure { source.saveTrip(ApiTripSaveInput.from(underage.identity)) },
            )
        }

    @Test fun editSnapshotAndRevisionAndAirportSearch() =
        runTest(timeout = 60.seconds) {
            val source = wizardMockSource()
            login(source)
            assertEquals("AMS", source.searchAirports("Schiphol", null).first().iata)
            assertTrue(source.searchAirports("zz", null).isEmpty())
            val review = source.reviewTrip(ApiTripReviewInput(wizardInput(), profile = wizardProfile()))
            val saved = source.saveTrip(ApiTripSaveInput.from(review.identity))
            val detail = source.fetchTripDetail(saved.tripId).detail
            val key =
                detail.plan.blocks
                    .first {
                        it.actionable &&
                            it.checkKey != null
                    }.checkKey!!
            source.send(
                OutboxChange.CheckOff(
                    CheckOffChange(
                        saved.tripId,
                        detail.plan.meta.inputHash,
                        key.type,
                        key.localDate,
                        key.ordinal,
                        CheckStatus.Done,
                    ),
                ),
            )
            val snapshot = source.tripInput(saved.tripId)
            val input = snapshot.input.copy(preparationDays = 1)
            assertEquals(
                DataFailure.ReviewExpired,
                failure {
                    source.reviewTripEdit(ApiTripReviewInput(input), saved.tripId, "stale")
                },
            )
            val edit = source.reviewTripEdit(ApiTripReviewInput(input), saved.tripId, snapshot.sourceRevision)
            assertEquals(0, edit.price?.cost)
            assertTrue(edit.canSave)
            val identity = ApiTripSaveInput.from(edit.identity)
            assertEquals(saved, source.updateTrip(saved.tripId, identity, snapshot.sourceRevision))
            assertEquals(saved, source.updateTrip(saved.tripId, identity, snapshot.sourceRevision))
            assertEquals(1, source.tripInput(saved.tripId).input.preparationDays)
            assertNotEquals(snapshot.sourceRevision, source.tripInput(saved.tripId).sourceRevision)
            assertTrue(
                source.fetchTripDetail(saved.tripId).detail.checkState.marks.any {
                    it.key == key &&
                        it.status == CheckStatus.Done
                },
            )
        }

    @Test fun seededTripInputsAndExpiredDrafts() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock, seed = MockAppSeed.make(clock.now(), zone = mockZone))
            login(source)
            val trip = source.fetchTrips().first()
            val snapshot = source.tripInput(trip.card.id)
            assertTrue(
                snapshot.input.outbound.legs
                    .isNotEmpty(),
            )
            assertTrue(snapshot.airports.isNotEmpty())
            source.reviewTripEdit(ApiTripReviewInput(snapshot.input), trip.card.id, snapshot.sourceRevision)
            val review = source.reviewTrip(ApiTripReviewInput(wizardInput(), profile = wizardProfile()))
            clock.advance(1801.0)
            assertEquals(DataFailure.ReviewExpired, failure { source.saveTrip(ApiTripSaveInput.from(review.identity)) })
        }

    @Test fun profileFailureAndOfflineAreRequestFailures() =
        runTest(timeout = 60.seconds) {
            val source = wizardMockSource()
            login(source)
            assertTrue(
                failure {
                    source.reviewTrip(
                        ApiTripReviewInput(
                            wizardInput(),
                            profile =
                                JsonObject(
                                    mapOf("age_band" to JsonPrimitive("invalid")),
                                ),
                        ),
                    )
                } is DataFailure.ProfileInvalid,
            )
            source.setOffline(true)
            assertEquals(DataFailure.Offline, failure { source.reviewTrip(ApiTripReviewInput(wizardInput())) })
        }
}
