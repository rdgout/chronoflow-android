@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import android.app.Application
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.config.resolve
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RootThemeChoiceTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun darkChoiceRendersDarkTokensWithoutRecreate() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val themes = MutableStateFlow(ThemeChoice.Light)
        try {
            host.get().setContent {
                val theme = rememberDeviceTheme(ThemeChoice.System) { themes }
                ChronoFlowTheme(darkTheme = theme.resolve(false), reduceMotion = true) {
                    Box(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag("theme"))
                }
            }
            compose.waitForIdle()
            assertEquals(CfLightColors.background, compose.onNodeWithTag("theme").captureToImage().toPixelMap()[0, 0])
            compose.runOnIdle { themes.value = ThemeChoice.Dark }
            compose.waitForIdle()
            assertEquals(CfDarkColors.background, compose.onNodeWithTag("theme").captureToImage().toPixelMap()[0, 0])
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test fun noDataStoreReadBeforeFirstFrame() =
        runTest(timeout = 60.seconds) {
            val clock = BroadcastFrameClock()
            val recomposer = Recomposer(coroutineContext + clock)
            val composition = Composition(NoUiApplier(), recomposer)
            val runner = backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
            var collections = 0
            var factories = 0
            try {
                composition.setContent {
                    rememberDeviceTheme(ThemeChoice.System) {
                        factories++
                        flow {
                            collections++
                            emit(ThemeChoice.Dark)
                        }
                    }
                }
                runCurrent()
                assertEquals(0, factories)
                assertEquals(0, collections)
                clock.sendFrame(1)
                runCurrent()
                assertEquals(1, factories)
                assertEquals(1, collections)
            } finally {
                withContext(NonCancellable) {
                    composition.dispose()
                    recomposer.cancel()
                    runner.cancelAndJoin()
                }
            }
        }

    @Config(sdk = [30])
    @Test
    fun deviceConfigThemeAndBarsFollowChoiceAcrossSystemAppearanceChanges() {
        val context = RuntimeEnvironment.getApplication() as Application
        File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME).writeText("language=system\ntheme=dark\n")
        val initial = ConfigurationPolicy.forLaunch(context, 30).initialTheme
        assertEquals(ThemeChoice.Dark, initial)
        assertEquals(ThemeChoice.System, ConfigurationPolicy.forLaunch(context, 32).initialTheme)
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val systemDark = mutableStateOf(false)
        val calls = mutableListOf<Boolean>()
        try {
            host.get().setContent {
                val theme = rememberDeviceTheme(initial) { emptyFlow() }
                rememberDeviceNight(theme, systemDark.value, false, false) { dark, _, _ -> calls += dark }
            }
            compose.waitForIdle()
            assertTrue(calls.last())
            val before = calls.size
            compose.runOnIdle { systemDark.value = true }
            compose.waitForIdle()
            assertTrue(calls.size > before)
            assertTrue(calls.last())
        } finally {
            host.pause().stop().destroy()
        }
    }

    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun onClear() = Unit

        override fun insertTopDown(
            index: Int,
            instance: Unit,
        ) = error("No UI nodes")

        override fun insertBottomUp(
            index: Int,
            instance: Unit,
        ) = error("No UI nodes")

        override fun remove(
            index: Int,
            count: Int,
        ) = error("No UI nodes")

        override fun move(
            from: Int,
            to: Int,
            count: Int,
        ) = error("No UI nodes")
    }

    companion object {
        @JvmStatic @BeforeClass
        fun warmNativeRuntime() {
            measureClassWarmup(RootThemeChoiceTest::class.java, Build.VERSION.SDK_INT, "native-font-runtime") {
                org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
                    .injectAndLoad()
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }
        }
    }
}
