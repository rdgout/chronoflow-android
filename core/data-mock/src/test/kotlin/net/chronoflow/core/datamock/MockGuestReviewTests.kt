package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.contracts.PlanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockGuestReviewTests {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun firstReviewMintsHashedSecretWithoutCreatingAccountData() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val secret = review.identity.guestSecret!!
            assertTrue(Regex("^[0-9a-f]{64}$").matches(secret))
            assertEquals(1, review.identity.inputRevision)
            assertEquals(1, review.identity.reviewRevision)
            assertNull(review.identity.sourceRevision)
            assertEquals(3, review.price?.balance)
            assertEquals(1, review.price?.cost)
            assertEquals(false, review.price?.unlimited)
            assertTrue(review.canSave)
            assertNull(review.saveBlockedReason)
            val state = store.load()!!
            assertTrue(state.accounts.isEmpty())
            assertTrue(state.sessions.isEmpty())
            assertTrue(state.claims.isEmpty())
            assertEquals(MockGuestReview.hash(secret), state.guestDrafts!![review.identity.token]!!.secretHash)
            assertFalse(MockJson.encodeToString(MockServerState.serializer(), state).contains(secret))
        }

    @Test fun laterReviewsReuseTokenAndNeverReturnSecretAgain() =
        runTest(timeout = 60.seconds) {
            val source = guestMockSource()
            val first = source.reviewGuestTrip(guestReviewInput())
            val second = source.reviewGuestTrip(continuedGuestInput(first))
            assertEquals(first.identity.token, second.identity.token)
            assertEquals(2, second.identity.inputRevision)
            assertEquals(2, second.identity.reviewRevision)
            assertNull(second.identity.guestSecret)
            assertTrue(second.canSave)
        }

    @Test fun freshReviewReusesOnlyAKnownDeviceSecretKnown() = freshReviewReusesOnlyAKnownDeviceSecret("known")

    @Test fun freshReviewReusesOnlyAKnownDeviceSecretUnknown() = freshReviewReusesOnlyAKnownDeviceSecret("unknown")

    @Test fun freshReviewReusesOnlyAKnownDeviceSecretForeignDevice() =
        freshReviewReusesOnlyAKnownDeviceSecret("foreign-device")

    private fun freshReviewReusesOnlyAKnownDeviceSecret(kind: String) =
        runTest(timeout = 60.seconds) {
            val source = guestMockSource()
            val first = source.reviewGuestTrip(guestReviewInput())
            var request = guestReviewInput().copy(guestSecret = first.identity.guestSecret)
            if (kind == "unknown") request = request.copy(guestSecret = "a".repeat(64))
            if (kind == "foreign-device") request = request.copy(deviceID = twoFactorID)
            val second = source.reviewGuestTrip(request)
            assertNotEquals(first.identity.token, second.identity.token)
            if (kind ==
                "known"
            ) {
                assertNull(second.identity.guestSecret)
            } else {
                assertNotNull(second.identity.guestSecret)
                assertTrue(first.identity.guestSecret != second.identity.guestSecret)
            }
        }

    @Test fun guestSleepIsRequiredHabitualBed() = guestSleepIsRequired("habitual_bed")

    @Test fun guestSleepIsRequiredHabitualWake() = guestSleepIsRequired("habitual_wake")

    private fun guestSleepIsRequired(field: String) =
        runTest(timeout = 60.seconds) {
            val request = guestReviewInput()
            val expected =
                DataFailure.ProfileInvalid(
                    mapOf(
                        "profile." + TripWizardErrors.camel(field) to
                            FieldMessage("Check your bedtime and wake-up time."),
                    ),
                )
            assertEquals(
                expected,
                failure {
                    guestMockSource().reviewGuestTrip(
                        request.copy(
                            profile =
                                JsonObject(request.profile + (field to JsonNull)),
                        ),
                    )
                },
            )
        }

    @Test fun sleepBecomesOverrideAndOnlyAgeAndShiftWorkPrompt() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            var request = guestReviewInput()
            request =
                request.copy(
                    profile =
                        JsonObject(
                            (request.profile - setOf("age_band", "shift_work")) +
                                mapOf(
                                    "habitual_bed" to JsonPrimitive("01:00"),
                                    "habitual_wake" to JsonPrimitive("09:00"),
                                    "vte" to JsonPrimitive(true),
                                ),
                        ),
                )
            val review = source.reviewGuestTrip(request)
            assertEquals(PlanStatus.NeedsAnswers, review.status)
            assertFalse(review.canSave)
            assertNull(review.saveBlockedReason)
            assertNull(review.price)
            assertNull(review.summary)
            assertEquals(listOf("age_band:::", "screening:shift_work::"), review.prompts.map { it.id })
            assertTrue(review.prompts.all { it.blocking })
            assertEquals(
                mapOf("age_band" to JsonNull),
                review.prompts[0].answer,
            )
            val draft = store.load()!!.guestDrafts!![review.identity.token]!!.draft
            assertTrue(draft.input.override.use)
            assertEquals("01:00", draft.input.override.habitualBed)
            assertEquals("09:00", draft.input.override.habitualWake)
            assertNull(draft.profile["vte"])
            val ready =
                source.reviewGuestTrip(
                    request.copy(
                        profile =
                            JsonObject(
                                request.profile +
                                    mapOf(
                                        "age_band" to JsonPrimitive("18_64"),
                                        "shift_work" to JsonPrimitive(false),
                                    ),
                            ),
                    ),
                )
            assertTrue(ready.canSave)
            assertTrue(ready.prompts.isEmpty())
        }

    @Test fun exclusionGivesNoPlanOrPromptsAgeBand() = exclusionGivesNoPlanOrPrompts("age_band")

    @Test fun exclusionGivesNoPlanOrPromptsShiftWork() = exclusionGivesNoPlanOrPrompts("shift_work")

    private fun exclusionGivesNoPlanOrPrompts(field: String) =
        runTest(timeout = 60.seconds) {
            val request = guestReviewInput()
            val review =
                guestMockSource().reviewGuestTrip(
                    request.copy(
                        profile =
                            JsonObject(
                                request.profile + (
                                    field to
                                        if (field == "age_band") {
                                            JsonPrimitive(
                                                "under_18",
                                            )
                                        } else {
                                            JsonPrimitive(true)
                                        }
                                ),
                            ),
                    ),
                )
            assertEquals(PlanStatus.NoPlan, review.status)
            assertFalse(review.canSave)
            assertTrue(review.prompts.isEmpty())
            assertNull(review.summary)
            assertNull(review.price)
        }

    @Test fun pastArrivalUsesGuestMessage() =
        runTest(timeout = 60.seconds) {
            val request =
                guestReviewInput().let {
                    it.copy(
                        input =
                            it.input.firstLeg { leg ->
                                leg.copy(departLocal = "2026-11-01T11:00", arriveLocal = "2026-11-02T06:00")
                            },
                    )
                }
            assertEquals(
                DataFailure.Validation(
                    mapOf("review" to FieldMessage("A new plan needs an upcoming arrival. Check your flight dates.")),
                    422,
                ),
                failure { guestMockSource().reviewGuestTrip(request) },
            )
        }

    @Test fun journeyErrorsMapToWizardFields() =
        runTest(timeout = 60.seconds) {
            val request =
                guestReviewInput().let {
                    it.copy(
                        input =
                            it.input.firstLeg { leg ->
                                leg.copy(toIata = "AMS")
                            },
                    )
                }
            assertEquals(
                DataFailure.Validation(
                    mapOf("legs.0.toIata" to FieldMessage("Choose different departure and arrival airports.")),
                    422,
                ),
                failure { guestMockSource().reviewGuestTrip(request) },
            )
        }

    @Test fun sixthDraftEvictsOldestForThatDeviceAcrossSecrets() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = guestMockSource(clock, store)
            val first = source.reviewGuestTrip(guestReviewInput())
            repeat(5) {
                clock.advance(61.0)
                source.reviewGuestTrip(guestReviewInput())
            }
            assertEquals(5, store.load()!!.guestDrafts!!.size)
            assertEquals(DataFailure.ReviewExpired, failure { source.reviewGuestTrip(continuedGuestInput(first)) })
            source.reviewGuestTrip(guestReviewInput().copy(deviceID = twoFactorID))
            assertEquals(6, store.load()!!.guestDrafts!!.size)
        }

    @Test fun expirySlidesForThirtyMinutesAndThenSecretIsUnknown() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = guestMockSource(clock)
            val first = source.reviewGuestTrip(guestReviewInput())
            clock.advance(29 * 60.0)
            val second = source.reviewGuestTrip(continuedGuestInput(first))
            clock.advance(29 * 60.0)
            var request = continuedGuestInput(second).copy(guestSecret = first.identity.guestSecret)
            val third = source.reviewGuestTrip(request)
            clock.advance(30 * 60.0)
            request = request.copy(identity = third.identity)
            assertEquals(DataFailure.ReviewExpired, failure { source.reviewGuestTrip(request) })
            val fresh = source.reviewGuestTrip(request.copy(identity = null))
            assertNotNull(fresh.identity.guestSecret)
            assertTrue(first.identity.guestSecret != fresh.identity.guestSecret)
        }

    @Test fun guestRouteIgnoresBearerAndMemberCannotSaveGuestDraft() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val clock = MockClock()
            val publicSource =
                MockDataSource(store, MockServerState(), now = clock::now, sleep = {
                }, bearer = { throw DataFailure.Unauthorized(401) }, zone = mockZone, io = Dispatchers.Unconfined)
            val review = publicSource.reviewGuestTrip(guestReviewInput())
            assertEquals(3, review.price?.balance)
            val member = mockSource(store = store)
            store.save(store.load()!!.copy(accounts = scenarioState().accounts))
            login(member)
            assertEquals(DataFailure.ReviewExpired, failure { member.saveTrip(ApiTripSaveInput.from(review.identity)) })
        }

    @Test fun sixthAttemptThrottlesEvenInvalidRequestsAndResetsPerDevice() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = guestMockSource(clock)
            val request = guestReviewInput().copy(profile = JsonObject(emptyMap()))
            repeat(5) { failure { source.reviewGuestTrip(request) } }
            clock.advance(10.0)
            assertEquals(
                DataFailure.Throttled(50.0, 429),
                failure {
                    source.reviewGuestTrip(request.copy(deviceID = request.deviceID.uppercase(java.util.Locale.ROOT)))
                },
            )
            source.reviewGuestTrip(guestReviewInput().copy(deviceID = twoFactorID))
            clock.advance(51.0)
            source.reviewGuestTrip(guestReviewInput())
        }
}
