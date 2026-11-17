package net.chronoflow.app

import android.os.Looper
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
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
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class RootThemeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun activityProvidesReduceMotionAndPreloadsOnlyAfterItsFrameCommit() {
        ReflectionHelpers.callStaticMethod<Unit>(
            ShadowValueAnimator::class.java,
            "setDurationScale",
            ClassParameter.from(Float::class.javaPrimitiveType, 0f),
        )
        val calls = AtomicInteger()
        var reduceMotion: Boolean? = null
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.fontPreloadOverride = {
            assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            calls.incrementAndGet()
        }
        activity.themeProbe = {
            val value = LocalReduceMotion.current
            SideEffect { reduceMotion = value }
        }
        try {
            controller
                .create()
                .start()
                .resume()
                .visible()
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(true, reduceMotion)
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
}
