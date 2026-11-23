package net.chronoflow.feature.auth.shell

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AuthStatesCaptureTest(
    private val cell: BoardCaptureCell,
) {
    private val profile = cell.profile.copy(heightDp = 1900)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, cell.language, cell.dark)).around(compose)

    @Test fun capturesEveryAuthBannerContext() {
        compose.setContent { ProfileHost(profile) { ChronoFlowTheme(darkTheme = cell.dark) { AuthStatesSample() } } }
        BoardCapture.capture(compose, "AuthStates_${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = BoardCaptureCell.standard.map { arrayOf(it) }
    }
}
