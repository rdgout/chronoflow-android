package net.chronoflow.core.data.wizard

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.GuestWizardGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class GuestGenerationMockTest {
    @Test fun firstReviewMintsA64HexSecretAndBalanceIsThree() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val result = service.review("wizard", input)
                val secret = checkNotNull(secrets.get(result.review.identity.token))
                assertTrue(Regex("^[0-9a-f]{64}$").matches(secret))
                assertEquals(3, result.review.price?.balance)
                assertEquals(1, result.review.price?.cost)
                assertNull(result.review.identity.guestSecret)
                assertTrue(result.review.canSave)
            }
        }

    @Test fun secondReviewReusesTheStoredSecret() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val first = service.review("wizard", input)
                val secret = secrets.get(first.review.identity.token)
                val second = service.review("wizard", first.input)
                assertNull(rawReviews.last().identity.guestSecret)
                assertEquals(secret, secrets.get(second.review.identity.token))
                assertEquals(secret, source.guestInputs.last().guestSecret)
            }
        }

    @Test fun forgottenSecretExpiresThenMintsAFreshDraft() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val first = service.review("wizard", input)
                val secret = secrets.get(first.review.identity.token)
                secrets.delete(first.review.identity.token)
                val fresh = service.review("wizard", first.input)
                assertNotEquals(first.review.identity.token, fresh.review.identity.token)
                assertNotEquals(secret, secrets.get(fresh.review.identity.token))
                assertEquals(3, source.guestInputs.size)
                assertNull(source.guestInputs.last().identity)
            }
        }

    @Test fun onlyAgeAndShiftPromptsBlock() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val missing =
                    input.copy(
                        profile = JsonObject(checkNotNull(input.profile) - setOf("age_band", "shift_work")),
                    )
                val review = service.review("wizard", missing).review
                assertEquals(listOf(listOf("age_band"), listOf("shift_work")), review.prompts.map { it.fields })
                assertTrue(review.prompts.all { it.blocking })
            }
        }

    @Test fun pastArrivalUsesGuestCopy() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val pastLeg =
                    input.input.outbound.legs.first().copy(
                        departLocal = "2026-11-01T11:00",
                        arriveLocal = "2026-11-02T06:00",
                    )
                val past =
                    input.copy(
                        input = input.input.copy(outbound = input.input.outbound.copy(legs = listOf(pastLeg))),
                    )
                try {
                    service.review("wizard", past)
                    fail("past arrival accepted")
                } catch (failure: DataFailure.Validation) {
                    val expected = FieldMessage("A new plan needs an upcoming arrival. Check your flight dates.")
                    assertEquals(mapOf("review" to expected), failure.fields)
                }
            }
        }

    private class Fixture(
        scope: TestScope,
    ) {
        val clock = WizardTestClock().apply { now = Instant.parse("2026-11-05T10:00:00Z") }
        private val io = StandardTestDispatcher(scope.testScheduler)
        private val mock =
            MockDataSource(
                store = MemoryMockStateStore(),
                seed = MockServerState(),
                now = { clock.now },
                delay = { 0.0 },
                io = io,
            )
        val source = GenerationSource()
        val rawReviews = mutableListOf<ApiTripReview>()
        val secrets = MemoryGuestSecretStore()
        val service =
            GuestWizardGeneration(
                source,
                secrets,
                { "0199a3c0-0000-7000-8000-000000000001" },
                parentContext =
                    scope.backgroundScope.coroutineContext + io,
            )
        val input =
            ApiTripReviewInput(
                input = generationInput(),
                profile =
                    JsonObject(
                        mapOf(
                            "habitual_bed" to JsonPrimitive("23:00"),
                            "habitual_wake" to JsonPrimitive("07:00"),
                            "chronotype" to JsonPrimitive("intermediate"),
                            "age_band" to JsonPrimitive("18_64"),
                            "shift_work" to JsonPrimitive(false),
                        ),
                    ),
            )

        init {
            source.guestHandler = { mock.reviewGuestTrip(it).also { review -> rawReviews += review } }
        }
    }

    private suspend fun TestScope.withGuest(block: suspend Fixture.() -> Unit) {
        val fixture = Fixture(this)
        try {
            fixture.block()
        } finally {
            withContext(NonCancellable) { fixture.service.cancel() }
        }
    }
}
