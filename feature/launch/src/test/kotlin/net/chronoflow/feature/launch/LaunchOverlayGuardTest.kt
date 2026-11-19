package net.chronoflow.feature.launch

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchOverlayGuardTest(
    private val case: LayoutCase,
    private val ms: Int,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun overlayFillsTheWindowWithoutFontDependentGeometry() {
        val controller = launchController(compose, ms)
        val fontScale = mutableStateOf(case.profile.fontScale.coerceAtMost(1.3f))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(case.profile.density, fontScale.value)) {
                ProfiledLaunchOverlay(controller, case.profile)
            }
        }
        firstDraw(compose, controller)
        val bitmap = compose.onNodeWithTag("launch.opening").captureToImage().asAndroidBitmap()
        if (ms == 0 || ms == 300) assertFrameZero(bitmap, controller)
        if (case.profile.name == "largeFont") {
            compose.runOnIdle { fontScale.value = 1f }
            val pixel8 = compose.onNodeWithTag("launch.opening").captureToImage().asAndroidBitmap()
            assertTrue("largeFont must render exactly like pixel8", bitmap.sameAs(pixel8))
        }
        val p = case.profile
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        val f = LaunchFrame()
        LaunchFrameSampler(
            LaunchTimeline.opening(LaunchSize(p.widthDp.toDouble(), p.heightDp.toDouble()), false),
        ).sampleInto(ms / 1000.0, f)
        assertTrue(f.moonX.isFinite() && f.moonY.isFinite() && f.moonScale > 0)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0},ms={1}")
        fun parameters(): Collection<Array<Any>> =
            LayoutMatrix.wide.flatMap { case -> listOf(0, 300, 1000, 1500).map { arrayOf<Any>(case, it) } }
    }
}
