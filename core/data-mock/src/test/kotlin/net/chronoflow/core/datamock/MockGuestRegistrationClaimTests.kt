package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockGuestRegistrationClaimTests {
    @Test fun legacyClaimDecodesAndReencodesWithoutPlaintextSecret() {
        val old = """{"accountID":"owner","tripID":"trip","secret":"legacy-secret"}"""
        val claim = MockJson.decodeFromString(MockClaim.serializer(), old)
        assertEquals(MockGuestReview.hash("legacy-secret"), claim.secretHash)
        val stored = MockJson.encodeToString(MockClaim.serializer(), claim)
        assertFalse(stored.contains("legacy-secret"))
    }

    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun claimCreatesAccountProfileTripAndSpendsSignupCredits() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = guestMockSource(clock, store)
            var input = guestReviewInput()
            input =
                input.copy(
                    profile =
                        JsonObject(
                            input.profile +
                                mapOf(
                                    "habitual_bed" to JsonPrimitive("23:30"),
                                    "habitual_wake" to JsonPrimitive("07:30"),
                                    "chronotype" to JsonPrimitive("late"),
                                ),
                        ),
                )
            val review = source.reviewGuestTrip(input)
            val session = source.signUp(guestRegistration(review))
            assertEquals(review.identity.token, session.tripId)
            assertFalse(session.account.hasVerifiedEmail)
            val state = store.load()!!
            val account = state.accounts.getValue(session.account.id)
            assertEquals(1, state.accounts.size)
            assertEquals(1, state.sessions.size)
            assertEquals(1, state.mail.size)
            assertEquals(listOf(review.identity.token), account.trips.map { it.card.id })
            assertEquals(review.identity.token, account.details[review.identity.token]?.trip?.id)
            assertEquals(JsonPrimitive("23:30"), account.snapshot.settings.bodyClock["habitualBed"])
            assertEquals(JsonPrimitive("07:30"), account.snapshot.settings.bodyClock["habitualWake"])
            assertEquals(JsonPrimitive("late"), account.snapshot.settings.bodyClock["chronotype"])
            assertEquals(JsonPrimitive("18_64"), account.snapshot.settings.bodyClock["ageBand"])
            assertTrue(account.snapshot.settings.sleepAnswered)
            assertEquals(JsonPrimitive(2), account.snapshot.settings.planCredits["credits"])
            assertNull(state.guestDrafts?.get(review.identity.token))
            val receipt = state.claims.getValue(review.identity.token)
            assertEquals(session.account.id, receipt.accountID)
            assertEquals(review.identity.token, receipt.tripID)
            assertEquals(seconds(clock.now()) + 7 * 86400, receipt.expiresAt!!, 0.0)
            assertEquals(input.deviceID, receipt.deviceID)
            assertEquals(
                "23:30",
                state.wizardInputs
                    ?.get(session.account.id + ":" + review.identity.token)
                    ?.input
                    ?.override
                    ?.habitualBed,
            )
            assertFalse(
                MockJson.encodeToString(MockServerState.serializer(), state).contains(review.identity.guestSecret!!),
            )
        }

    @Test fun freeTripKeepsThreeSignupCredits() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val input =
                guestReviewInput().let {
                    it.copy(
                        input =
                            it.input.firstLeg { leg ->
                                leg.copy(toIata = "DXB", arriveLocal = "2026-11-10T20:00")
                            },
                    )
                }
            val review = source.reviewGuestTrip(input)
            assertEquals(0, review.price?.cost)
            assertTrue(review.canSave)
            val session = source.signUp(guestRegistration(review))
            assertEquals(
                JsonPrimitive(3),
                store
                    .load()!!
                    .accounts
                    .getValue(session.account.id)
                    .snapshot.settings.planCredits["credits"],
            )
        }

    @Test fun blockedReviewCannotCreateAccount() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val request = guestReviewInput()
            val review =
                source.reviewGuestTrip(
                    request.copy(
                        profile =
                            JsonObject(request.profile - "shift_work"),
                    ),
                )
            assertFalse(review.canSave)
            assertEquals(expiredGuestClaim, failure { source.signUp(guestRegistration(review)) })
            assertTrue(store.load()!!.accounts.isEmpty())
            assertTrue(store.load()!!.claims.isEmpty())
        }

    @Test fun claimedDraftCannotCreateAnotherAccount() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val input = guestRegistration(review)
            source.signUp(input)
            assertEquals(expiredGuestClaim, failure { source.signUp(input.copy(email = "second@example.com")) })
            assertEquals(1, store.load()!!.accounts.size)
            assertEquals(1, store.load()!!.claims.size)
        }

    @Test fun accountFieldsStillValidateBeforeAnyClaimEffects() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val review = source.reviewGuestTrip(guestReviewInput())
            assertEquals(
                DataFailure.Validation(
                    mapOf("password_confirmation" to FieldMessage("The passwords don't match yet.")),
                    422,
                ),
                failure { source.signUp(guestRegistration(review).copy(passwordConfirmation = "different")) },
            )
            assertTrue(store.load()!!.accounts.isEmpty())
            assertTrue(store.load()!!.claims.isEmpty())
        }

    @Test fun replayReturnsSameTripAndAccountWithoutAnotherChargeAfterReload() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = guestMockSource(clock, store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val input = guestRegistration(review)
            val first = source.signUp(input)
            val second = guestMockSource(clock, store).signUp(input.copy(email = " TRAVELLER@example.com "))
            assertEquals(first.tripId, second.tripId)
            assertEquals(first.account.id, second.account.id)
            assertTrue(first.token != second.token)
            val state = store.load()!!
            assertEquals(1, state.accounts.size)
            assertEquals(1, state.claims.size)
            assertEquals(1, state.sessions.size)
            assertEquals(1, state.mail.size)
            assertNull(state.sessions[first.token])
            assertEquals(
                1,
                state.accounts
                    .getValue(first.account.id)
                    .trips.size,
            )
            assertEquals(
                JsonPrimitive(2),
                state.accounts
                    .getValue(first.account.id)
                    .snapshot.settings.planCredits["credits"],
            )
        }

    @Test fun retryMustProveAccountAndPossessionPassword() = retryMustProveAccountAndPossession("password")

    @Test fun retryMustProveAccountAndPossessionSecret() = retryMustProveAccountAndPossession("secret")

    @Test fun retryMustProveAccountAndPossessionTwoFactor() = retryMustProveAccountAndPossession("two-factor")

    private fun retryMustProveAccountAndPossession(kind: String) =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val review = source.reviewGuestTrip(guestReviewInput())
            var input = guestRegistration(review)
            val first = source.signUp(input)
            when (kind) {
                "password" -> {
                    input =
                        input.copy(password = "AnotherPassword12!", passwordConfirmation = "AnotherPassword12!")
                }

                "secret" -> {
                    input = input.copy(guestSecret = "1".repeat(64))
                }

                else -> {
                    store.save(store.load()!!.updateAccount(first.account.id) { it.copy(twoFactorCode = "123456") })
                }
            }
            assertEquals(
                DataFailure.Validation(
                    mapOf("email" to FieldMessage("This email address is already in use.")),
                    422,
                ),
                failure {
                    source.signUp(input)
                },
            )
            assertEquals(1, store.load()!!.accounts.size)
            assertEquals(1, store.load()!!.sessions.size)
        }

    @Test fun receiptKeepsDeviceSecretKnownForFreshReview() =
        runTest(timeout = 60.seconds) {
            val source = guestMockSource()
            val review = source.reviewGuestTrip(guestReviewInput())
            source.signUp(guestRegistration(review))
            val fresh = source.reviewGuestTrip(guestReviewInput().copy(guestSecret = review.identity.guestSecret))
            assertNull(fresh.identity.guestSecret)
            assertNotEquals(review.identity.token, fresh.identity.token)
        }

    @Test fun plainRegistrationLeavesGuestDraftAndSignupCredits() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = guestMockSource(store = store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val session = source.signUp(registration())
            assertNull(session.tripId)
            val state = store.load()!!
            assertNotNull(state.guestDrafts?.get(review.identity.token))
            assertTrue(state.claims.isEmpty())
            assertTrue(
                state.accounts
                    .getValue(session.account.id)
                    .trips
                    .isEmpty(),
            )
            assertEquals(
                JsonPrimitive(3),
                state.accounts
                    .getValue(session.account.id)
                    .snapshot.settings.planCredits["credits"],
            )
        }
}
