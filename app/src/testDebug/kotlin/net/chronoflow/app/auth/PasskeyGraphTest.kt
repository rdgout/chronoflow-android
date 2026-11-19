package net.chronoflow.app.auth

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraph
import net.chronoflow.app.MainActivity
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class PasskeyGraphTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var graph: AppGraph
    private var scenario: org.robolectric.android.controller.ActivityController<MainActivity>? = null

    private fun launch() {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                    TestSeed.PROFILE_KEY to "screen",
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in",
                    "cf.passkeyCapability" to "1",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_MOCK_DELAY" to "0",
                ),
            )
        runTest(dispatcher, timeout = 60.seconds) {
            TestSeeds.apply(
                app,
                seed,
                Dispatchers.IO,
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            )
        }
        graph = app.graph
        scenario =
            org.robolectric.Robolectric
                .buildActivity(
                    MainActivity::class.java,
                    TestSeeds.forwardIntent(app, seed),
                ).create()
                .start()
                .resume()
    }

    @Test fun activityAnchorAttachesAndDetaches() {
        launch()
        assertTrue(graph.passkeyCapability.enabled)
        assertTrue(graph.auth.signIn.passkeyCapability.enabled)
        assertSame(scenario!!.get(), graph.passkeyAnchor.current())
        scenario!!.pause().stop().destroy()
        scenario = null
        assertNull(graph.passkeyAnchor.current())
    }

    @After fun close() {
        scenario?.pause()?.stop()?.destroy()
        if (::graph.isInitialized) runTest(dispatcher, timeout = 60.seconds) { graph.close() }
    }
}
