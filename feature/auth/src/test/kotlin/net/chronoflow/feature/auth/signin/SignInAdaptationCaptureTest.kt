package net.chronoflow.feature.auth.signin

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
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
class SignInAdaptationCaptureTest(
    private val state: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(authBoard, dark = dark)).around(compose)

    @Test fun capturesThePlatformAdaptation() {
        compose.setContent {
            ProfileHost(authBoard) {
                ChronoFlowTheme(darkTheme = dark) {
                    SignInSample(
                        state,
                        passkey =
                            state != "none",
                    )
                }
            }
        }
        val label =
            when (state) {
                "offline" -> "Offline"
                "none" -> "PasskeyOff"
                else -> "FieldError"
            }
        BoardCapture.capture(compose, "auth/SignIn${label}_en_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-dark={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("offline", false),
                arrayOf("offline", true),
                arrayOf("none", false),
                arrayOf("fieldError", false),
            )
    }
}
