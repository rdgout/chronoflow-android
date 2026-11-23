package net.chronoflow.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch

class BoundedProfileInputTest {
    @Test fun blockedBinderLikeInputFailsWithoutWaitingForTheWorkerToReturn() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            BoundedProfileInput(stepTimeoutMillis = 100).use { input ->
                val failure =
                    assertThrows(AssertionError::class.java) {
                        input.step("timeline swipe 1/15") {
                            entered.countDown()
                            try {
                                release.await()
                            } catch (_: InterruptedException) {
                                release.await()
                            }
                        }
                    }
                assertEquals(0, entered.count)
                assertEquals(1, release.count)
                assertTrue(failure.message!!.contains("timeline swipe 1/15"))
            }
        } finally {
            release.countDown()
        }
    }

    @Test fun failedInputKeepsItsCause() {
        val original = IllegalStateException("input rejected")
        BoundedProfileInput().use { input ->
            val failure = assertThrows(IllegalStateException::class.java) { input.step("click") { throw original } }
            assertSame(original, failure)
        }
    }

    @Test fun successfulInputReturnsItsResult() {
        BoundedProfileInput().use { input -> assertEquals(42, input.step("query") { 42 }) }
    }

    @Test fun expiredJourneyRejectsTheNextStep() {
        var now = 0L
        BoundedProfileInput(journeyTimeoutMillis = 1, nowNanos = { now }).use { input ->
            now = 1_000_000L
            var ran = false
            val failure = assertThrows(AssertionError::class.java) { input.step("query") { ran = true } }
            assertTrue(failure.message!!.contains("journey deadline"))
            assertEquals(false, ran)
        }
    }
}
