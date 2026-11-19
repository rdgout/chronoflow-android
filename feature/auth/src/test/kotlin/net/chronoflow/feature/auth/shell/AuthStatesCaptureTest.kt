package net.chronoflow.feature.auth.shell

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.auth.signin.authBoard
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
    private val dark: Boolean,
) {
    private val profile = authBoard.copy(name = "authStatesBoard", heightDp = 1900)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, dark = dark)).around(compose)

    @Test fun capturesEveryAuthBannerContext() {
        compose.setContent { ProfileHost(profile) { ChronoFlowTheme(darkTheme = dark) { AuthStatesSample() } } }
        BoardCapture.capture(compose, "AuthStates_en_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases() = listOf(arrayOf(false), arrayOf(true))
    }
}
