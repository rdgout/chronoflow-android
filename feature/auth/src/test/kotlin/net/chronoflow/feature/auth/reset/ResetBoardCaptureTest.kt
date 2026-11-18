package net.chronoflow.feature.auth.reset

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.auth.forgot.ForgotSample
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
class ResetBoardCaptureTest(
    private val name: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(authBoard, dark = dark)).around(compose)

    @Test fun capturesTheBoardState() {
        compose.setContent {
            ProfileHost(authBoard) {
                ChronoFlowTheme(darkTheme = dark) {
                    when (name) {
                        "AuthForgot" -> ForgotSample("form")
                        "AuthReset" -> ResetSample("form")
                        "AuthResetDone" -> ResetSample("done")
                        "AuthResetExpired" -> ForgotSample("form", expired = true)
                        "auth/ForgotSent" -> ForgotSample("sent")
                        "auth/ExpiredSent" -> ForgotSample("sent", expired = true)
                        "auth/ResetFieldErrors" -> ResetSample("fieldErrors")
                    }
                }
            }
        }
        BoardCapture.capture(compose, "${name}_en_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_dark={1}")
        fun cases(): List<Array<Any>> =
            listOf("AuthForgot", "AuthReset", "AuthResetDone", "AuthResetExpired").flatMap { name ->
                listOf(false, true).map { arrayOf<Any>(name, it) }
            } +
                listOf(
                    arrayOf("auth/ForgotSent", false),
                    arrayOf("auth/ExpiredSent", false),
                    arrayOf("auth/ResetFieldErrors", false),
                )
    }
}
