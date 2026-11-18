package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockGuestPossessionTests(
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
                arrayOf("missing"),
                arrayOf("wrong"),
                arrayOf("token-as-secret"),
                arrayOf("foreign-device"),
                arrayOf("revision"),
                arrayOf("review-revision"),
            )
    }

    @Test fun missingWrongOrForeignPossessionExpires() =
        runTest(timeout = 60.seconds) {
            val source = guestMockSource()
            val first = source.reviewGuestTrip(guestReviewInput())
            val request = continuedGuestInput(first)
            val changed =
                when (kind) {
                    "missing" -> request.copy(guestSecret = null)
                    "wrong" -> request.copy(guestSecret = "0".repeat(64))
                    "token-as-secret" -> request.copy(guestSecret = first.identity.token)
                    "foreign-device" -> request.copy(deviceID = twoFactorID)
                    "revision" -> request.copy(identity = request.identity!!.copy(inputRevision = 0))
                    else -> request.copy(identity = request.identity!!.copy(reviewRevision = 0))
                }
            assertEquals(DataFailure.ReviewExpired, failure { source.reviewGuestTrip(changed) })
        }
}
