package net.chronoflow.app.testing

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.MainActivity
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import net.chronoflow.debug.UITestTimeline
import org.junit.After
import org.junit.Rule
import java.util.UUID

/**
 * Base for emulator tests that seed a state and check one screen. It launches seeded (never through a form) and
 * applies the seed in-process, then starts MainActivity through [ActivityScenario]. The compose rule is empty, so no
 * activity starts before the seed is applied.
 */
@OptIn(ExperimentalTestApi::class)
abstract class ScreenTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    /** The seed of the last [launch]. */
    protected lateinit var seed: TestSeed
        private set

    /**
     * Language and theme persist across tests, so every launch resets them to `system` unless [overrides] sets them.
     */
    protected fun launch(overrides: Map<String, String> = emptyMap()): ActivityScenario<MainActivity> {
        val launched = ActivityScenario.launch<MainActivity>(seededIntent(overrides))
        scenario = launched
        compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)
        UITestTimeline.record("seeded-screen-visible", seed)
        return launched
    }

    protected fun launchWithoutIdle(overrides: Map<String, String>) {
        targetContext.startActivity(seededIntent(overrides))
    }

    private fun seededIntent(overrides: Map<String, String>): Intent {
        val extras =
            mapOf(
                TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                TestSeed.PROFILE_KEY to "screen",
                "cf.settings.language" to "system",
                "cf.settings.theme" to "system",
            ) + overrides
        seed = TestSeed.parse(extras)
        runBlocking { TestSeeds.apply(targetContext, seed, Dispatchers.IO) }
        return TestSeeds.forwardIntent(targetContext, seed)
    }

    protected fun waitFor(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean,
    ) = compose.waitUntil(timeoutMillis, condition)

    @After
    fun closeScenarioAndResetConfiguration() {
        scenario?.close()
        scenario = null
        val reset = TestSeed.parse(mapOf("cf.settings.language" to "system", "cf.settings.theme" to "system"))
        runBlocking { TestSeeds.apply(targetContext, reset, Dispatchers.IO) }
    }
}
