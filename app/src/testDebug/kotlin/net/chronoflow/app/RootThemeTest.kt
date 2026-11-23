@file:OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    androidx.compose.runtime.InternalComposeTracingApi::class,
)

package net.chronoflow.app

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.LaunchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
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
    fun themeContentProvidesReduceMotionAndUsesTheSuppliedFontBarrier() =
        runTest(timeout = 60.seconds) {
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
            val mainDispatcher = StandardTestDispatcher(testScheduler)
            val graph =
                object : RootThemeGraph {
                    override val fontPreloader = preloader
                    override val dispatchers = AppDispatchers(main = mainDispatcher, io = dispatcher)
                    override val launch = launch
                }
            val inputs = graph.rootThemeInputs(true, commit)
            var motion = false
            try {
                composition.setContent {
                    CompositionLocalProvider(
                        LocalFontFamilyResolver provides resolver,
                        LocalHapticFeedback provides NoHapticFeedback,
                    ) {
                        RootThemeContent(false, inputs) {
                            val current = LocalReduceMotion.current
                            SideEffect { motion = current }
                        }
                    }
                }
                runCurrent()
                frameClock.sendFrame(0L)
                runCurrent()
                assertEquals(0, preloader.calls.get())
                assertTrue(commit.awaited.get())
                assertTrue(motion)
                assertFalse(launch.fontsReady)
                completed.complete(Unit)
                runCurrent()
                assertEquals(4, preloader.calls.get())
                assertTrue(launch.fontsReady)
            } finally {
                withContext(NonCancellable) {
                    composition.dispose()
                    recomposer.cancel()
                    recomposing.cancelAndJoin()
                }
            }
        }

    @Config(application = Application::class)
    @Test
    fun cfRootForwardsMotionAndFirstCommit() {
        compose.mainClock.autoAdvance = false
        assertCfRootBindings(reduceMotion = true)
        assertCfRootBindings(reduceMotion = false)
    }

    @Config(sdk = [29, 30], application = Application::class)
    @Test
    fun cfRootAppliesTheStoredThemeWhenTheSystemIsLight() {
        withStoredDarkRoot { _, _ ->
            val pixel = compose.onNodeWithTag("app.root").captureToImage().toPixelMap()[0, 0]
            assertEquals(CfDarkColors.background, pixel)
        }
    }

    @Test
    @Config(application = Application::class)
    fun endingTheOverlayDoesNotRecomposeCfRoot() {
        var roots = 0
        val tracer =
            object : CompositionTracer {
                override fun isTraceInProgress() = true

                override fun traceEventStart(
                    key: Int,
                    dirty1: Int,
                    dirty2: Int,
                    info: String,
                ) {
                    if (".CfRoot (" in info) roots++
                }

                override fun traceEventEnd() = Unit
            }
        Composer.setTracer(tracer)
        try {
            withStoredDarkRoot(overlayShowing = true) { graph, _ ->
                assertTrue(roots > 0)
                assertTrue(graph.launch.overlayActive)
                compose.onNodeWithTag("launch.opening").assertExists()
                roots = 0
                compose.runOnIdle {
                    graph.launch.overlayShowing = false
                    androidx.compose.runtime.snapshots.Snapshot
                        .sendApplyNotifications()
                }
                compose.mainClock.advanceTimeByFrame()
                compose.mainClock.advanceTimeByFrame()
                compose.onNodeWithTag("launch.opening").assertDoesNotExist()
                compose.runOnIdle { assertEquals(0, roots) }
            }
        } finally {
            Composer.setTracer(null)
        }
    }

    @Config(sdk = [29, 30])
    @Test
    fun configurationCallbackIsFollowedByTheStoredThemeSystemBarEffect() {
        compose.mainClock.autoAdvance = false
        RuntimeEnvironment.setQualifiers("+night")
        val app = RuntimeEnvironment.getApplication() as RecordingApplication
        runTest(app.mainDispatcher, timeout = 60.seconds) { app.graph.deviceAppearance.setTheme(ThemeChoice.Dark) }
        val host = Robolectric.buildActivity(MainActivity::class.java, intent(app)).setup()
        val activity = host.get()
        val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val applied = mutableListOf<Boolean>()
        try {
            activity.setContent {
                CfRoot(
                    app.graph,
                    false,
                    true,
                    CompletableDeferred(),
                    null,
                    destinations = RoutedWelcomeDestinations(app.graph.rootNavigator),
                    onSystemBars = { night, overlay, sky ->
                        applied += night
                        ReflectionHelpers.callInstanceMethod<Unit>(
                            activity,
                            "applySystemBars",
                            ClassParameter.from(Boolean::class.javaPrimitiveType, night),
                            ClassParameter.from(Boolean::class.javaPrimitiveType, overlay),
                            ClassParameter.from(Boolean::class.javaPrimitiveType, sky),
                        )
                    },
                ) {}
            }
            compose.mainClock.advanceTimeByFrame()
            app.mainDispatcher.scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertTrue(applied.last()) }
            assertFalse(bars.isAppearanceLightNavigationBars)
            val before = applied.size
            val configuration =
                Configuration(activity.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
                }
            compose.runOnIdle {
                host.configurationChange(configuration)
                assertSame(activity, host.get())
                assertEquals(
                    Configuration.UI_MODE_NIGHT_NO,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
                )
            }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                assertTrue(
                    "A configuration change must reapply the stored choice even when it remains Dark",
                    applied.size > before,
                )
                assertTrue(applied.last())
                assertFalse(bars.isAppearanceLightNavigationBars)
            }
        } finally {
            host.pause().stop().destroy()
            runTest(app.mainDispatcher, timeout = 60.seconds) {
                withContext(NonCancellable) { app.graph.closeAndJoinForTest() }
            }
        }
    }

    private fun withStoredDarkRoot(
        overlayShowing: Boolean = false,
        block: (AppGraph, ActivityController<ComponentActivity>) -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        RuntimeEnvironment.setQualifiers("+notnight")
        val main = StandardTestDispatcher()
        val launchScope = CoroutineScope(SupervisorJob() + main)
        val app = RuntimeEnvironment.getApplication()
        val launch =
            LaunchController(moon = {
                androidx.compose.ui.graphics
                    .ImageBitmap(1, 1)
            }, scope = launchScope)
        launch.captureAppearance(app.resources.configuration)
        launch.captureReduceMotion(false, null)
        launch.overlayShowing = overlayShowing
        val graph =
            AppGraph(
                app,
                RecordingFontPreloader(),
                launchProvider = { launch },
                overrides = GraphOverrides(dispatchers = AppDispatchers(main = main, io = main, default = main)),
            )
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            runTest(main, timeout = 60.seconds) { graph.deviceAppearance.setTheme(ThemeChoice.Dark) }
            host.get().setContent {
                CfRoot(
                    graph,
                    false,
                    true,
                    CompletableDeferred(),
                    null,
                    destinations = RoutedWelcomeDestinations(graph.rootNavigator),
                ) {
                }
            }
            compose.mainClock.advanceTimeByFrame()
            main.scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            main.scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertEquals(
                Configuration.UI_MODE_NIGHT_NO,
                host
                    .get()
                    .resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            )
            block(graph, host)
        } finally {
            host.pause().stop().destroy()
            runTest(main, timeout = 60.seconds) {
                withContext(NonCancellable) {
                    graph.closeAndJoinForTest()
                    launchScope.coroutineContext.job.cancelAndJoin()
                }
            }
        }
    }

    private fun assertCfRootBindings(reduceMotion: Boolean) {
        val main = StandardTestDispatcher()
        val io = StandardTestDispatcher(main.scheduler)
        val preloader = RecordingFontPreloader().apply { expectedDispatcher = io }
        val launchScope = CoroutineScope(SupervisorJob() + main)
        val launch = LaunchController(moon = { error("root wiring test loads no images") }, scope = launchScope)
        val graph =
            AppGraph(
                RuntimeEnvironment.getApplication(),
                preloader,
                launchProvider = { launch },
                overrides = GraphOverrides(dispatchers = AppDispatchers(main = main, io = io)),
            )
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val firstCommit = CompletableDeferred<Unit>()
        var observed: Boolean? = null
        try {
            host.get().setContent {
                CfRoot(
                    graph,
                    false,
                    reduceMotion,
                    firstCommit,
                    null,
                    destinations = RoutedWelcomeDestinations(graph.rootNavigator),
                ) {
                    val current = LocalReduceMotion.current
                    SideEffect { observed = current }
                }
            }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            io.scheduler.runCurrent()
            compose.runOnIdle {
                assertEquals(reduceMotion, observed)
                assertEquals(0, preloader.calls.get())
                firstCommit.complete(Unit)
            }
            compose.waitForIdle()
            io.scheduler.runCurrent()
            compose.runOnIdle {
                assertEquals(4, preloader.calls.get())
                assertTrue(launch.fontsReady)
            }
        } finally {
            try {
                host.pause().stop().destroy()
            } finally {
                runTest(main, timeout = 60.seconds) {
                    withContext(NonCancellable) {
                        graph.closeAndJoinForTest()
                        launchScope.coroutineContext.job.cancelAndJoin()
                    }
                }
            }
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
