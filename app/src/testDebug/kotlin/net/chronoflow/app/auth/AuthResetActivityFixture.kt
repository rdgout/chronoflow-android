package net.chronoflow.app.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
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
import net.chronoflow.core.navigation.Route
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.After
import org.junit.Rule
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
abstract class AuthResetActivityFixture {
    @get:Rule val compose = createEmptyComposeRule()
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    protected lateinit var graph: AppGraph
    protected lateinit var scenario: ActivityScenario<MainActivity>

    protected fun launch(
        audit: String? = null,
        extras: Map<String, String> = emptyMap(),
        link: String? = null,
        flags: Int = 0,
    ) {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val values =
            mutableMapOf(
                TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                TestSeed.PROFILE_KEY to "screen",
                "CF_MOCK_DELAY" to "0",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
            )
        if (audit != null) values["CF_AUTH_TEST_AUDIT_SCREEN"] = audit
        values.putAll(extras)
        val seed = TestSeed.parse(values)
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
        val intent = TestSeeds.forwardIntent(app, seed)
        if (link != null) intent.setAction(Intent.ACTION_VIEW).setData(Uri.parse(link))
        intent.addFlags(flags)
        compose.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch(intent)
        commitFrame()
        waitFor {
            val route = graph.rootNavigator.state.value.route
            when (audit) {
                "forgot" -> route == Route.Forgot
                "reset" -> if (extras["CF_AUTH_TEST_AUDIT_STATE"] == "expired") route != null else route is Route.Reset
                "sign-in" -> route == Route.SignIn
                else -> route != null
            }
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    protected fun commitFrame() {
        scenario.onActivity { activity ->
            ReflectionHelpers
                .callInstanceMethod<ArrayList<Runnable>?>(
                    activity.window.decorView.viewTreeObserver,
                    "captureFrameCommitCallbacks",
                )?.forEach {
                    it.run()
                }
        }
    }

    protected fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            condition()
        }
        compose.waitForIdle()
    }

    protected fun savedBundle(): Bundle =
        Bundle().also { out ->
            scenario.onActivity {
                ReflectionHelpers.callInstanceMethod<Unit>(
                    it,
                    "onSaveInstanceState",
                    ClassParameter.from(Bundle::class.java, out),
                )
            }
        }

    protected fun recreate(route: Route) {
        scenario.recreate()
        commitFrame()
        waitFor { graph.rootNavigator.state.value.route == route }
    }

    @After fun closeGraph() {
        if (::scenario.isInitialized) scenario.close()
        if (::graph.isInitialized) runTest(dispatcher, timeout = 60.seconds) { graph.close() }
    }
}
