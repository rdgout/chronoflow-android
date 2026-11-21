@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SensitiveClipboardTests {
    private class Port : ClipboardPort {
        var stamp: Long? = 0L
        var clears = 0
        var lastSensitive = false

        override fun set(
            label: String,
            text: String,
            sensitive: Boolean,
        ): Long {
            lastSensitive = sensitive
            return (checkNotNull(stamp) + 1).also { stamp = it }
        }

        override fun currentTimestamp() = stamp

        override fun clear() {
            clears++
            stamp = 0L
        }
    }

    @Test fun clearsItsOwnClipAfterSixtySeconds() =
        runTest(timeout = 60.seconds) {
            val port = Port()
            val clipboard = SensitiveClipboard(port, backgroundScope, "ChronoFlow")
            clipboard.copy("secret")
            runCurrent()
            assertTrue(port.lastSensitive)
            advanceTimeBy(59.seconds)
            runCurrent()
            assertEquals(0, port.clears)
            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(1, port.clears)
        }

    @Test fun leavesAForeignClipAlone() =
        runTest(timeout = 60.seconds) {
            val port = Port()
            SensitiveClipboard(port, backgroundScope, "ChronoFlow").copy("secret")
            runCurrent()
            port.stamp = 9L
            advanceTimeBy(60.seconds)
            runCurrent()
            assertEquals(0, port.clears)
        }

    @Test fun unreadableAtExpiryClearsOnNextForeground() =
        runTest(timeout = 60.seconds) {
            val port = Port()
            val clipboard = SensitiveClipboard(port, backgroundScope, "ChronoFlow")
            clipboard.copy("secret")
            runCurrent()
            val ours = port.stamp
            port.stamp = null
            advanceTimeBy(60.seconds)
            runCurrent()
            assertEquals(0, port.clears)
            port.stamp = ours
            clipboard.foreground()
            assertEquals(1, port.clears)
        }

    @Test fun aSecondCopyRestartsTheTimer() =
        runTest(timeout = 60.seconds) {
            val port = Port()
            val clipboard = SensitiveClipboard(port, backgroundScope, "ChronoFlow")
            clipboard.copy("first")
            runCurrent()
            advanceTimeBy(30.seconds)
            clipboard.copy("second")
            runCurrent()
            advanceTimeBy(30.seconds)
            runCurrent()
            assertEquals(0, port.clears)
            advanceTimeBy(30.seconds)
            runCurrent()
            assertEquals(1, port.clears)
        }

    @Test fun textIsNeverInToString() =
        runTest(timeout = 60.seconds) {
            val clipboard = SensitiveClipboard(Port(), backgroundScope, "ChronoFlow")
            clipboard.copy("secret")
            assertFalse(clipboard.toString().contains("secret"))
        }
}
