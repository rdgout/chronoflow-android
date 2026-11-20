@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.LaunchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi", application = RootThemeTest.RecordingApplication::class)
class RootThemeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun activityPreloadsOnlyAfterItsFrameCommit() {
        compose.mainClock.autoAdvance = false
        val app = RuntimeEnvironment.getApplication() as RecordingApplication
        val calls = app.preloader.calls
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent(app))
        val activity = controller.get()
        var ready = false
        try {
            controller.create()
            val firstCommit = ObservedCommit(ReflectionHelpers.getField<Deferred<Unit>>(activity, "firstCommit"))
            // Exercise the root's font effect without composing navigation, screens or the app theme.
            activity.setContent { RootFontWarmup(firstCommit, app.preloader) { ready = true } }
            controller.start().resume().visible()
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            compose.waitUntil(5000) { firstCommit.awaited.get() }
            compose.runOnIdle {
                assertEquals(0, calls.get())
                assertFalse(ready)
                val callbacks =
                    ReflectionHelpers.callInstanceMethod<ArrayList<Runnable>>(
                        activity.window.decorView.viewTreeObserver,
                        "captureFrameCommitCallbacks",
                    )
                assertTrue("MainActivity must register its real frame-commit callback", callbacks.isNotEmpty())
                callbacks.forEach(Runnable::run)
            }
            compose.waitUntil(5000) { calls.get() == 4 }
            compose.runOnIdle { assertTrue(ready) }
        } finally {
            controller.pause().stop().destroy()
            app.graph.appScope.cancel()
            runTest(app.mainDispatcher, timeout = 60.seconds) { app.graph.closeAndJoinForTest() }
        }
    }

    @Config(application = Application::class)
    @Test
    fun rootProvidesReduceMotionAndUsesTheSuppliedFontBarrier() =
        runTest(timeout = 60.seconds) {
            var checkpoint = System.nanoTime()

            fun mark(step: String) {
                val now = System.nanoTime()
                println("rootTheme step=$step ms=${(now - checkpoint) / 1_000_000.0}")
                checkpoint = now
            }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val preloader = RecordingFontPreloader().apply { expectedDispatcher = dispatcher }
            val launch =
                LaunchController(moon = { error("font test must not load launch images") }, scope = backgroundScope)
            val resolver = createFontFamilyResolver(RuntimeEnvironment.getApplication())
            val frameClock = BroadcastFrameClock()
            val recomposer = Recomposer(coroutineContext + frameClock)
            val composition = Composition(NoUiApplier(), recomposer)
            val recomposing = backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
            val completed = CompletableDeferred<Unit>()
            val commit = ObservedCommit(completed)
            var motion = false
            mark("runtime-fixture")
            try {
                composition.setContent {
                    CompositionLocalProvider(
                        LocalFontFamilyResolver provides resolver,
                        LocalHapticFeedback provides NoHapticFeedback,
                    ) {
                        RootThemeContent(false, true, commit, preloader, dispatcher, launch::markFontsReady) {
                            val current = LocalReduceMotion.current
                            SideEffect { motion = current }
                        }
                    }
                }
                runCurrent()
                mark("set-content")
                frameClock.sendFrame(0L)
                runCurrent()
                mark("first-frame")
                assertEquals(0, preloader.calls.get())
                assertTrue(commit.awaited.get())
                assertTrue(motion)
                assertFalse(launch.fontsReady)
                completed.complete(Unit)
                runCurrent()
                assertEquals(4, preloader.calls.get())
                assertTrue(launch.fontsReady)
                mark("commit-and-font-assertions")
            } finally {
                withContext(NonCancellable) {
                    composition.dispose()
                    recomposer.cancel()
                    recomposing.cancelAndJoin()
                }
                mark("teardown")
            }
        }

    @Test fun activityRefreshesTheSystemMotionSettingWhenResumed() {
        val app = RuntimeEnvironment.getApplication() as RecordingApplication
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent(app))
        try {
            durationScale(0f)
            controller.create()
            val motion = ReflectionHelpers.getField<State<Boolean>>(controller.get(), "reduceMotion\$delegate")
            assertTrue(motion.value)
            durationScale(1f)
            controller.start().resume()
            assertFalse(motion.value)
        } finally {
            controller.destroy()
            app.graph.appScope.cancel()
            runTest(app.mainDispatcher, timeout = 60.seconds) { app.graph.closeAndJoinForTest() }
            durationScale(1f)
        }
    }

    private fun intent(app: RecordingApplication) =
        Intent(app, MainActivity::class.java)
            .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
            .putExtra("CF_UI_TEST_PROFILE", "screen")
            .putExtra("cf.mode", "ready")

    private fun durationScale(scale: Float) {
        ReflectionHelpers.callStaticMethod<Unit>(
            ShadowValueAnimator::class.java,
            "setDurationScale",
            ClassParameter.from(Float::class.javaPrimitiveType, scale),
        )
    }

    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun onClear() = Unit

        override fun insertTopDown(
            index: Int,
            instance: Unit,
        ): Unit = error("theme fixture emits no UI nodes")

        override fun insertBottomUp(
            index: Int,
            instance: Unit,
        ): Unit = error("theme fixture emits no UI nodes")

        override fun remove(
            index: Int,
            count: Int,
        ): Unit = error("theme fixture emits no UI nodes")

        override fun move(
            from: Int,
            to: Int,
            count: Int,
        ): Unit = error("theme fixture emits no UI nodes")
    }

    private object NoHapticFeedback : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }

    @OptIn(kotlinx.coroutines.InternalForInheritanceCoroutinesApi::class)
    private class ObservedCommit(
        private val actual: Deferred<Unit>,
    ) : Deferred<Unit> by actual {
        val awaited = AtomicBoolean()

        override suspend fun await() {
            awaited.set(true)
            actual.await()
        }
    }

    class RecordingApplication : ChronoFlowApp() {
        val preloader = RecordingFontPreloader()
        val mainDispatcher = StandardTestDispatcher()
        val ioDispatcher = StandardTestDispatcher(mainDispatcher.scheduler)
        private val controller by lazy { newLaunchController(graph) }

        override fun createGraph(): AppGraph =
            AppGraph(
                this,
                preloader,
                launchProvider = { controller },
                overrides = GraphOverrides(dispatchers = AppDispatchers(main = mainDispatcher, io = ioDispatcher)),
            )
    }

    class RecordingFontPreloader : FontPreloader {
        val calls = AtomicInteger()
        var expectedDispatcher: CoroutineDispatcher? = null

        override suspend fun preload(
            resolver: FontFamily.Resolver,
            family: FontFamily,
        ) {
            val expected = expectedDispatcher
            if (expected == null) {
                assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            } else {
                assertEquals(expected, currentCoroutineContext()[ContinuationInterceptor])
            }
            calls.incrementAndGet()
        }
    }
}
