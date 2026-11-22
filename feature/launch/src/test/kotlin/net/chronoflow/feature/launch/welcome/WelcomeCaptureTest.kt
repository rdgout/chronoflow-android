package net.chronoflow.feature.launch.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.launch.LaunchMode
import net.chronoflow.feature.launch.LaunchOverlay
import net.chronoflow.feature.launch.LaunchOverrides
import net.chronoflow.feature.launch.firstDraw
import net.chronoflow.feature.launch.launchController
import net.chronoflow.feature.launch.model.LaunchIntro
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

internal data class WelcomeCaptureCase(
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean,
    val state: String,
    val ms: Int = 0,
) {
    val name: String get() {
        val board = if (state == "welcome") "SplashWelcome" else "Main"
        val moment = if (state == "final") state else "$state-$ms"
        return "${board}_${moment}_${profile.name}_${language.tag}_${if (dark) "dark" else "light"}"
    }

    override fun toString(): String = name
}

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WelcomeCaptureTest internal constructor(
    private val case: WelcomeCaptureCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun capturesTheWelcomeBoardState() {
        if (case.state == "welcome") {
            val controller =
                launchController(
                    compose,
                    overrides = LaunchOverrides(mode = LaunchMode.Welcome, freezeAt = case.ms.milliseconds),
                )
            controller.markFontsReady()
            try {
                compose.setContent {
                    ProfileHost(case.profile) {
                        ChronoFlowTheme(darkTheme = case.dark) {
                            Box(Modifier.fillMaxSize()) {
                                if (controller.route == LaunchRoute.Welcome) {
                                    WelcomeScreen(controller.launchIntro, controller.moonAnchorStore, {}, {})
                                }
                                LaunchOverlay(controller)
                            }
                        }
                    }
                }
                firstDraw(compose, controller)
                compose.waitForIdle()
                compose.onRoot().captureToImage()
                checkNotNull(controller.moonAnchorStore.frame)
                BoardCapture.capture(compose, case.name)
            } finally {
                controller.onActivityDestroyed()
            }
        } else {
            val intro =
                if (case.state ==
                    "intro"
                ) {
                    LaunchIntro(2300.milliseconds, ManualLaunchClock(), case.ms.milliseconds)
                } else {
                    null
                }
            compose.setContent {
                ProfileHost(case.profile) {
                    ChronoFlowTheme(darkTheme = case.dark) { WelcomeScreen(intro, null, {}, {}) }
                }
            }
            BoardCapture.capture(compose, case.name)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val finals =
                listOf(
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.small,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    AppLanguage.entries.flatMap { language ->
                        listOf(false, true).map { dark ->
                            WelcomeCaptureCase(profile, language, dark, "final")
                        }
                    }
                }
            val intros =
                listOf(false, true).flatMap { dark ->
                    listOf(2400, 2450, 2700, 3200, 3300).map { ms ->
                        WelcomeCaptureCase(DeviceProfiles.pixel8, AppLanguage.En, dark, "intro", ms)
                    }
                }
            val flights =
                listOf(DeviceProfiles.pixel8, DeviceProfiles.foldInner).flatMap { profile ->
                    listOf(false, true).flatMap { dark ->
                        listOf(1500, 1950, 2300, 2400, 3300).map { ms ->
                            WelcomeCaptureCase(profile, AppLanguage.En, dark, "welcome", ms)
                        }
                    }
                }
            val auditFlights =
                BoardCaptureCell.standard.map { cell ->
                    WelcomeCaptureCase(cell.profile, cell.language, cell.dark, "welcome", 2300)
                }
            return (finals + intros + flights + auditFlights).distinct().map { arrayOf<Any>(it) }
        }
    }
}
