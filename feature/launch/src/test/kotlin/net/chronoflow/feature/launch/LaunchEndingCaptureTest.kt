package net.chronoflow.feature.launch

import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchEndingCaptureTest(
    private val profile: DeviceProfile,
    private val dark: Boolean,
    private val mode: LaunchMode,
    private val ms: Int,
    private val reduced: Boolean,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language, dark)).around(compose)

    @Test fun capturesTheFrozenHandOff() {
        val controller =
            launchController(
                compose,
                reduced = reduced,
                overrides = LaunchOverrides(mode = mode, freezeAt = ms.milliseconds),
            )
        try {
            compose.setContent {
                Box(Modifier.fillMaxSize().background(controller.appearance.colors.background)) {
                    LaunchOverlay(controller)
                }
            }
            firstDraw(compose, controller)
            if (mode == LaunchMode.Slow) {
                compose.waitUntil(2000) {
                    shadowOf(Looper.getMainLooper()).idle()
                    controller.statusRaster != null
                }
            }
            val board = if (mode == LaunchMode.Ready) "SplashScreen" else "SplashSlow"
            val state =
                when {
                    mode == LaunchMode.Ready -> "ready"
                    reduced -> "slowReduceMotion"
                    else -> "slow"
                }
            val theme = if (dark) "dark" else "light"
            BoardCapture.capture(compose, "${board}_$state-${ms}_${profile.name}_${language.tag}_$theme")
        } finally {
            controller.onActivityDestroyed()
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0},dark={1},{2},{3}ms,reduce={4},{5}")
        fun cases(): List<Array<Any>> {
            val frames =
                listOf(
                    Triple(LaunchMode.Ready, 1700, false),
                    Triple(LaunchMode.Ready, 1900, false),
                    Triple(LaunchMode.Slow, 2000, false),
                    Triple(LaunchMode.Slow, 2600, false),
                    Triple(LaunchMode.Slow, 4900, false),
                    Triple(LaunchMode.Slow, 2600, true),
                )
            return listOf(DeviceProfiles.pixel8, DeviceProfiles.small).flatMap { profile ->
                listOf(false, true).flatMap { dark ->
                    frames.map { (mode, ms, reduced) -> arrayOf<Any>(profile, dark, mode, ms, reduced, AppLanguage.En) }
                }
            } + listOf(arrayOf<Any>(DeviceProfiles.pixel8, false, LaunchMode.Slow, 2600, false, AppLanguage.Nl))
        }
    }
}
