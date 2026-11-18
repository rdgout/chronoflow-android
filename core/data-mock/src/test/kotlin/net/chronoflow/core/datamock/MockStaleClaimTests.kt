package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockStaleClaimTests(
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
                arrayOf("input-revision"),
                arrayOf("review-revision"),
                arrayOf("wrong-secret"),
                arrayOf("missing-secret"),
                arrayOf("foreign-device"),
                arrayOf("unknown-token"),
                arrayOf("missing-token"),
                arrayOf("missing-review-revision"),
                arrayOf("missing-input-revision"),
                arrayOf("expired"),
            )
    }

    @Test fun staleClaimsCreateNothing() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = guestMockSource(clock, store)
            val review = source.reviewGuestTrip(guestReviewInput())
            val input = guestRegistration(review)
            val changed =
                when (kind) {
                    "input-revision" -> {
                        input.copy(inputRevision = 5)
                    }

                    "review-revision" -> {
                        input.copy(reviewRevision = 4)
                    }

                    "wrong-secret" -> {
                        input.copy(guestSecret = "0".repeat(64))
                    }

                    "missing-secret" -> {
                        input.copy(guestSecret = null)
                    }

                    "foreign-device" -> {
                        input.copy(deviceID = twoFactorID)
                    }

                    "unknown-token" -> {
                        input.copy(draftToken = twoFactorID)
                    }

                    "missing-token" -> {
                        input.copy(draftToken = null)
                    }

                    "missing-review-revision" -> {
                        input.copy(reviewRevision = null)
                    }

                    "missing-input-revision" -> {
                        input.copy(inputRevision = null)
                    }

                    else -> {
                        clock.advance(31 * 60.0)
                        input
                    }
                }
            assertEquals(expiredGuestClaim, failure { source.signUp(changed) })
            val state = store.load()!!
            assertTrue(state.accounts.isEmpty())
            assertTrue(state.sessions.isEmpty())
            assertTrue(state.claims.isEmpty())
            assertTrue(state.mail.isEmpty())
            assertNull(state.wizardInputs)
            assertNull(state.wizardPaidDirections)
        }
}
