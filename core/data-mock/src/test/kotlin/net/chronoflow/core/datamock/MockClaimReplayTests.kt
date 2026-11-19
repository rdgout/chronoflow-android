package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockClaimReplayTests(
    private val kind: String,
) {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<String>> =
            listOf(
                arrayOf("matching"),
                arrayOf("no-claim"),
                arrayOf("wrong-secret"),
                arrayOf("missing-secret"),
                arrayOf("other-account"),
                arrayOf("expired"),
            )
    }

    @Test fun loginReplaysOnlyLiveMatchingReceipt() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = guestMockSource(clock, store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val registration = guestRegistration(review)
            source.signUp(registration)
            var input =
                PasswordSignInInput(
                    registration.email,
                    registration.password,
                    twoFactorID,
                    "Test phone",
                    review.identity.token,
                    review.identity.guestSecret,
                )
            when (kind) {
                "no-claim" -> {
                    input = input.copy(draftToken = null, guestSecret = null)
                }

                "wrong-secret" -> {
                    input = input.copy(guestSecret = "2".repeat(64))
                }

                "missing-secret" -> {
                    input = input.copy(guestSecret = null)
                }

                "other-account" -> {
                    val other =
                        registration.copy(
                            email = "stranger@example.com",
                            draftToken = null,
                            guestSecret = null,
                            inputRevision = null,
                            reviewRevision = null,
                        )
                    source.signUp(other)
                    input = input.copy(email = other.email)
                }

                "expired" -> {
                    clock.advance(7 * 86400.0)
                }
            }
            val session = (guestMockSource(clock, store).signIn(input) as SignInResult.Session).result
            assertEquals(if (kind == "matching") review.identity.token else null, session.tripId)
        }
}
