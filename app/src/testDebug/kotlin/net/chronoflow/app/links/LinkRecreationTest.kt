@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.links

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraph
import net.chronoflow.app.MainActivity
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.app.closeAndJoinForTest
import net.chronoflow.core.model.auth.AuthLink
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.testing.AuthLinkUrls
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class LinkRecreationTest {
    private val link = ResetPasswordLink("cold-link-secret", "demo@example.com")
    private val url = AuthLinkUrls.reset(link.token, link.email)
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var graph: AppGraph
    private lateinit var controller: ActivityController<MainActivity>

    private fun launch(
        link: String? = null,
        flags: Int = 0,
        extras: Map<String, String> = emptyMap(),
    ) {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                    TestSeed.PROFILE_KEY to "screen",
                    "CF_MOCK_DELAY" to "0",
                ) + extras,
            )
        runTest(dispatcher, timeout = 60.seconds) {
            TestSeeds.apply(
                app,
                seed,
                Dispatchers.IO,
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            )
            graph = app.graph
            assertEquals(SessionPresence.SignedOut, graph.controller.restore())
        }
        val intent = TestSeeds.forwardIntent(app, seed).addFlags(flags)
        if (link != null) intent.setAction(Intent.ACTION_VIEW).setData(Uri.parse(link))
        // Intent capture and recreation need lifecycle callbacks, not an attached Compose window.
        controller =
            Robolectric
                .buildActivity(MainActivity::class.java, intent)
                .create()
                .start()
                .resume()
        scheduler.runCurrent()
    }

    @After fun close() {
        if (::controller.isInitialized) controller.pause().stop().destroy()
        if (::graph.isInitialized) {
            runTest(dispatcher, timeout = 60.seconds) {
                withContext(NonCancellable) { graph.closeAndJoinForTest() }
            }
        }
    }

    private fun capturedLink(): AuthLink? =
        graph.pendingLaunchIntents
            .consume()
            ?.links
            ?.singleOrNull()
            ?.let(AuthLink::parse)

    @Test fun coldViewIsCapturedAndRecreationDoesNotReplayIt() {
        launch(link = url)
        assertEquals(AuthLink.Reset(link), capturedLink())
        assertNull(controller.get().intent.data)
        controller.recreate()
        scheduler.runCurrent()
        assertNull(capturedLink())
        assertNull(controller.get().intent.data)
    }

    @Test fun historyRelaunchDoesNotCaptureTheLink() {
        launch(link = url, flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertNull(graph.pendingLaunchIntents.consume())
        assertNull(controller.get().intent.data)
    }

    @Test fun warmViewKeepsTheLaunchIntentAndCapturesTheLinkOnce() {
        launch()
        val activity = controller.get()
        val original = activity.intent
        val incoming = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "onNewIntent",
            ClassParameter.from(Intent::class.java, incoming),
        )
        scheduler.runCurrent()
        assertSame(original, activity.intent)
        assertNull(activity.intent.data)
        assertEquals(AuthLink.Reset(link), capturedLink())
        controller.recreate()
        scheduler.runCurrent()
        assertNull(capturedLink())
    }

    @Test fun debugSeedCapturesInMemoryAndDoesNotForwardTheSecret() {
        launch(extras = mapOf("CF_AUTH_TEST_LINK" to url))
        assertEquals(AuthLink.Reset(link), capturedLink())
        assertFalse(controller.get().intent.hasExtra("CF_AUTH_TEST_LINK"))
    }
}
