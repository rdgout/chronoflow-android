@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.DebugAppHooks
import net.chronoflow.feature.launch.model.LaunchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/** Android-only regression (new): a fresh activity in a warm process hands over its captured intent. */
@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class WarmLaunchIntentTest {
    private fun warmActivity(reminder: Boolean) =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            DebugAppHooks.resetGraph(
                app,
                GraphOverrides(
                    sessionCipher = SoftwareSessionCipher(),
                    dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
                ),
            )
            val graph = app.graph
            try {
                val plain =
                    Intent(app, MainActivity::class.java)
                        .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                        .putExtra("CF_UI_TEST_PROFILE", "screen")
                ActivityScenario.launch<MainActivity>(plain).use {
                    graph.finishLaunch()
                    graph.rootNavigator.navigate(Route.Terms)
                }
                val incoming =
                    Intent(app, MainActivity::class.java).apply {
                        if (reminder) {
                            putExtra("cf.reminder.tripId", "trip")
                            putExtra("cf.reminder.blockId", "block")
                        } else {
                            action = Intent.ACTION_VIEW
                            data = Uri.parse("https://app.chronoflow.net/x")
                        }
                    }

                suspend fun checkHandoff() {
                    assertEquals(LaunchKind.Warm, graph.launch.lastLaunchKind)
                    // The mount callback calls finishLaunch when no overlay remains.
                    graph.finishLaunch()
                    runCurrent()
                    assertNull("warm activity intent was left buffered", graph.pendingLaunchIntents.consume())
                    assertEquals(Route.Terms, graph.rootNavigator.state.value.route)
                }
                if (reminder) {
                    ActivityScenario.launch<MainActivity>(incoming).use { checkHandoff() }
                } else {
                    withLinkActivity(incoming) { checkHandoff() }
                }
            } finally {
                withContext(NonCancellable) { graph.close() }
            }
        }

    @Test fun warmFreshActivityHandsItsLinkOver() = warmActivity(reminder = false)

    @Test fun warmFreshActivityHandsItsReminderOver() = warmActivity(reminder = true)
}
