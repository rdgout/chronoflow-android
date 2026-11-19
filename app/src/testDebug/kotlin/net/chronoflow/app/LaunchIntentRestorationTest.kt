package net.chronoflow.app

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.DebugAppHooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/** Android-only regression for activity restoration and Recents replaying a handled launch link. */
@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class LaunchIntentRestorationTest {
    private suspend fun TestScope.withGraph(check: suspend (SessionTestApplication, AppGraph) -> Unit) {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        DebugAppHooks.resetGraph(
            app,
            GraphOverrides(
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            ),
        )
        try {
            check(app, app.graph)
        } finally {
            withContext(NonCancellable) { app.graph.close() }
        }
    }

    private fun link(app: SessionTestApplication): Intent =
        Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://app.chronoflow.net/x"))
            .putExtra("cf.mode", "ready")
            .putExtra("cf.freezeAt", "2300")

    @Test fun handledLinkIsNotCapturedAgainAfterRecreation() =
        runTest(timeout = 60.seconds) {
            withGraph { app, graph ->
                withLinkActivity(link(app)) { controller ->
                    graph.finishLaunch()
                    assertNull(graph.pendingLaunchIntents.consume())
                    controller.recreate()
                    assertNull(graph.pendingLaunchIntents.consume())
                }
            }
        }

    @Test fun historyLaunchDoesNotCaptureTheOriginalLink() =
        runTest(timeout = 60.seconds) {
            withGraph { app, graph ->
                val intent = link(app).addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                withLinkActivity(intent) {
                    assertNull(graph.pendingLaunchIntents.consume())
                }
            }
        }

    @Test fun freshLaunchStillCapturesItsLink() =
        runTest(timeout = 60.seconds) {
            withGraph { app, graph ->
                withLinkActivity(link(app)) {
                    assertEquals(setOf("https://app.chronoflow.net/x"), graph.pendingLaunchIntents.consume()?.links)
                }
            }
        }
}
