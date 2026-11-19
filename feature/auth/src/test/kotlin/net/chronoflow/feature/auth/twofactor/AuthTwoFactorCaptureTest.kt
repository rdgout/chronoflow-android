package net.chronoflow.feature.auth.twofactor

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
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
class AuthTwoFactorCaptureTest(
    private val state: String,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(authBoard, language, dark)).around(compose)

    @Test fun capturesTheBoardState() {
        compose.setContent { ProfileHost(authBoard) { ChronoFlowTheme(darkTheme = dark) { TwoFactorSample(state) } } }
        BoardCapture.capture(compose, "AuthTwoFactor_${state}_${language.tag}_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> =
            twoFactorStates.flatMap { state ->
                listOf(false, true).map { dark ->
                    arrayOf<Any>(state, AppLanguage.En, dark)
                }
            } + listOf("code-fieldError", "recovery-none").map { arrayOf<Any>(it, AppLanguage.Nl, false) }
    }
}
