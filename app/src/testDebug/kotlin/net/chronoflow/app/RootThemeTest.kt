package net.chronoflow.app

import android.os.Looper
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowValueAnimator
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalTestApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi", application = RootThemeTest.RecordingApplication::class)
class RootThemeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun activityProvidesReduceMotionAndPreloadsOnlyAfterItsFrameCommit() {
        ReflectionHelpers.callStaticMethod<Unit>(
            ShadowValueAnimator::class.java,
            "setDurationScale",
            ClassParameter.from(Float::class.javaPrimitiveType, 0f),
        )
        val app = RuntimeEnvironment.getApplication() as RecordingApplication
        val calls = app.preloader.calls
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        try {
            controller
                .create()
                .start()
                .resume()
                .visible()
            compose.waitForIdle()
            compose.runOnIdle {
                val root = compose.onNodeWithTag("app.root").fetchSemanticsNode()
                val locals =
                    ReflectionHelpers.callInstanceMethod<CompositionLocalMap>(
                        root.layoutInfo,
                        "getCompositionLocalMap",
                    )
                assertTrue(locals[LocalReduceMotion])
                assertEquals(0, calls.get())
                val callbacks =
                    ReflectionHelpers.callInstanceMethod<ArrayList<Runnable>>(
                        activity.window.decorView.viewTreeObserver,
                        "captureFrameCommitCallbacks",
                    )
                assertTrue("MainActivity must register its real frame-commit callback", callbacks.isNotEmpty())
                callbacks.forEach(Runnable::run)
            }
            compose.waitUntil(5000) { calls.get() == 4 }
        } finally {
            controller.pause().stop().destroy()
            ReflectionHelpers.callStaticMethod<Unit>(
                ShadowValueAnimator::class.java,
                "setDurationScale",
                ClassParameter.from(Float::class.javaPrimitiveType, 1f),
            )
        }
    }

    class RecordingApplication : ChronoFlowApp() {
        val preloader = RecordingFontPreloader()

        override fun createGraph(): AppGraph = AppGraph(this, preloader)
    }

    class RecordingFontPreloader : FontPreloader {
        val calls = AtomicInteger()

        override suspend fun preload(
            resolver: FontFamily.Resolver,
            family: FontFamily,
        ) {
            assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            calls.incrementAndGet()
        }
    }
}
