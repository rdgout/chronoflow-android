package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PendingLaunchIntentTest {
    @Test fun capturesMergeLinksAndConsumeOnce() {
        val buffer = PendingLaunchIntent.Buffer()
        val tap = ReminderTapExtras("trip", "block", "account", "hash")
        buffer.capture(PendingLaunchIntent(setOf("https://app.chronoflow.net/first"), tap))
        buffer.capture(PendingLaunchIntent(setOf("https://app.chronoflow.net/second")))
        assertEquals(
            PendingLaunchIntent(setOf("https://app.chronoflow.net/first", "https://app.chronoflow.net/second"), tap),
            buffer.consume(),
        )
        assertNull(buffer.consume())
    }

    @Test fun pendingIntentDiagnosticsRedactLinks() {
        assertFalse(PendingLaunchIntent(setOf("secret-link-value")).toString().contains("secret-link-value"))
    }
}
