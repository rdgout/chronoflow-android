package net.chronoflow.feature.launch

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
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchCaptureTest(
    private val p: DeviceProfile,
    private val dark: Boolean,
    private val ms: Int,
    private val reduced: Boolean,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(p, language, dark)).around(compose)

    @Test fun capturesTheFrozenOpening() {
        val controller = launchController(compose, ms, reduced)
        compose.setContent { LaunchOverlay(controller) }
        firstDraw(compose, controller)
        val state = if (reduced) "reduceMotion" else "ready"
        val time = ms.toString().padStart(4, '0')
        val theme = if (dark) "dark" else "light"
        BoardCapture.capture(compose, "SplashScreen_$state-${time}_${p.name}_${language.tag}_$theme")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0},dark={1},ms={2},reduce={3},{4}")
        fun parameters(): Collection<Array<Any>> =
            listOf(DeviceProfiles.pixel8, DeviceProfiles.small).flatMap { p ->
                listOf(false, true).flatMap { dark ->
                    listOf(0, 300, 700, 1000, 1100, 1400, 1500).map { ms ->
                        arrayOf<Any>(p, dark, ms, false, AppLanguage.En)
                    } +
                        listOf(arrayOf<Any>(p, dark, 0, true, AppLanguage.En))
                }
            } + listOf(arrayOf<Any>(DeviceProfiles.pixel8, false, 0, false, AppLanguage.Nl))
    }
}
