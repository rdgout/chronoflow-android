package net.chronoflow.app.testing

import android.app.Activity
import android.content.Intent
import android.view.Choreographer
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.MainActivity
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import net.chronoflow.debug.UITestTimeline
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

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
     * Language and theme persist across tests. A null override omits a default key when checking persistence.
     */
    protected fun launch(overrides: Map<String, String?> = emptyMap()): ActivityScenario<MainActivity> {
        hideKeyboard()
        val launched = ActivityScenario.launch<MainActivity>(seededIntent(overrides))
        scenario = launched
        compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)
        UITestTimeline.record("seeded-screen-visible", seed)
        return launched
    }

    protected fun launchWithoutIdle(overrides: Map<String, String>) {
        hideKeyboard()
        targetContext.startActivity(seededIntent(overrides))
    }

    private fun seededIntent(overrides: Map<String, String?>): Intent {
        val extras =
            mapOf(
                TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                TestSeed.PROFILE_KEY to "screen",
                "cf.settings.language" to "system",
                "cf.settings.theme" to "system",
            ).plus(overrides).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        seed = TestSeed.parse(extras)
        runBlocking { TestSeeds.apply(targetContext, seed, Dispatchers.IO) }
        return TestSeeds.forwardIntent(targetContext, seed)
    }

    protected fun waitFor(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean,
    ) = compose.waitUntil(timeoutMillis, condition)

    @Before
    fun hideKeyboardBeforeTest() = hideKeyboard()

    private fun hideKeyboard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activities = mutableListOf<Activity>()
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            activities +=
                listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED)
                    .flatMap { monitor.getActivitiesInStage(it) }
                    .filter { !it.isDestroyed && it.window.decorView.isShown && it.window.decorView.isAttachedToWindow }
        }
        activities.forEach { activity ->
            lateinit var decor: View
            instrumentation.runOnMainSync { decor = activity.window.decorView }
            waitForImeToSettle(decor, visible = false) {
                activity.currentFocus?.clearFocus()
                decor.clearFocus()
                WindowInsetsControllerCompat(activity.window, decor).hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    protected fun waitForImeToSettle(
        submitTag: String,
        windowDecor: View? = null,
    ) {
        val submit = compose.onNodeWithTag(submitTag).fetchSemanticsNode()
        lateinit var decor: View
        if (windowDecor == null) {
            checkNotNull(scenario).onActivity { decor = it.window.decorView }
        } else {
            decor = windowDecor
        }
        waitForImeToSettle(decor, visible = true, bounds = { submit.boundsInRoot })
    }

    private fun waitForImeToSettle(
        decor: View,
        visible: Boolean,
        bounds: () -> Rect = { Rect(0f, 0f, decor.width.toFloat(), decor.height.toFloat()) },
        changeVisibility: () -> Unit = {},
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val settled = AtomicBoolean(false)
        lateinit var frames: Choreographer
        var imeAnimating = false
        var previousBounds: Rect? = null
        val animations =
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        imeAnimating = true
                        settled.set(false)
                        previousBounds = null
                    }
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    imeAnimating = runningAnimations.any { it.typeMask and WindowInsetsCompat.Type.ime() != 0 }
                    if (imeAnimating) settled.set(false)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) imeAnimating = false
                }
            }
        val sample =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    // A finishing window stops receiving insets once it detaches.
                    if (!visible && !decor.isAttachedToWindow) {
                        settled.set(true)
                        return
                    }
                    val insets = ViewCompat.getRootWindowInsets(decor)
                    val bottom = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: -1
                    val visibilityMatches =
                        insets?.isVisible(WindowInsetsCompat.Type.ime()) == visible &&
                            if (visible) bottom > 0 else bottom == 0
                    val currentBounds = bounds()
                    if (visibilityMatches && !imeAnimating && currentBounds == previousBounds) {
                        settled.set(true)
                    } else {
                        settled.set(false)
                        previousBounds = if (visibilityMatches && !imeAnimating) currentBounds else null
                    }
                    frames.postFrameCallback(this)
                }
            }
        instrumentation.runOnMainSync { frames = Choreographer.getInstance() }
        try {
            instrumentation.runOnMainSync {
                ViewCompat.setWindowInsetsAnimationCallback(decor, animations)
                changeVisibility()
                frames.postFrameCallback(sample)
            }
            compose.waitUntil(5_000) { settled.get() }
        } finally {
            instrumentation.runOnMainSync {
                frames.removeFrameCallback(sample)
                ViewCompat.setWindowInsetsAnimationCallback(decor, null)
            }
        }
    }

    @After
    fun closeScenarioAndResetConfiguration() {
        try {
            hideKeyboard()
        } finally {
            try {
                scenario?.close()
            } finally {
                scenario = null
                val reset = TestSeed.parse(mapOf("cf.settings.language" to "system", "cf.settings.theme" to "system"))
                runBlocking { TestSeeds.apply(targetContext, reset, Dispatchers.IO) }
            }
        }
    }
}
