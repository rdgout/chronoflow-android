package net.chronoflow.app

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class PendingLaunchIntentsTest {
    @Test fun viewAndReminderIntentsAreHeldUntilLaunchFinishes() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val graph =
                AppGraph(
                    app,
                    overrides =
                        GraphOverrides(
                            sessionCipher = SoftwareSessionCipher(),
                            dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                        ),
                )
            try {
                val uri = "https://app.chronoflow.net/x"
                graph.pendingLaunchIntents.capture(
                    Intent(Intent.ACTION_VIEW, Uri.parse(uri)),
                    restored = false,
                )
                graph.pendingLaunchIntents.capture(
                    Intent(net.chronoflow.core.data.reminders.ACTION_OPEN).putExtra("cf.reminder.tripId", "trip"),
                    restored = false,
                )
                graph.handlePendingLaunchIntents()
                val held = graph.pendingLaunchIntents.consume()!!
                assertEquals(setOf(uri), held.links)
                assertEquals("trip", held.reminder?.tripId)
                graph.pendingLaunchIntents.capture(Intent(Intent.ACTION_VIEW, Uri.parse(uri)), restored = false)
                graph.finishLaunch()
                assertNull(graph.pendingLaunchIntents.consume())
            } finally {
                graph.closeAndJoinForTest()
            }
        }

    @Test fun reminderExtrasNeedTheOpenActionAndAreNotRestored() {
        val pending = PendingLaunchIntents()
        val intent = Intent(Intent.ACTION_VIEW).putExtra("cf.reminder.tripId", "trip")
        pending.capture(intent, restored = false)
        assertNull(pending.consume())
        intent.action = net.chronoflow.core.data.reminders.ACTION_OPEN
        pending.capture(intent, restored = true)
        assertNull(pending.consume())
        pending.capture(intent, restored = false)
        assertEquals("trip", pending.consume()?.reminder?.tripId)
        val stripped =
            net.chronoflow.app.reminders
                .sanitizedLaunchIntent(intent)
        assertNull(stripped.action)
        assertNull(stripped.getStringExtra("cf.reminder.tripId"))
        assertEquals(
            Intent.ACTION_MAIN,
            net.chronoflow.app.reminders
                .sanitizedLaunchIntent(Intent(Intent.ACTION_MAIN))
                .action,
        )
    }
}
