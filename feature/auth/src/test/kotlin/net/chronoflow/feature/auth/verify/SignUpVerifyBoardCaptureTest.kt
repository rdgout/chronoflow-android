package net.chronoflow.feature.auth.verify

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.auth.signin.authBoard
import net.chronoflow.feature.auth.signup.SignUpSample
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpVerifyBoardCaptureTest(
    private val name: String,
    private val state: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(authBoard, dark = dark)).around(compose)

    @Test fun capturesTheBoardState() {
        val signUp = name.contains("SignUp")
        var verify: VerifySampleState? = null
        if (!signUp) {
            runTest(timeout = 60.seconds) { verify = verifySnapshotModel(state) }
            if (name.startsWith("AuthVerify_")) {
                val sample = checkNotNull(verify)
                verify =
                    sample.copy(remaining = 42, ui = sample.ui.copy(canResend = false))
            }
        }
        compose.setContent {
            ProfileHost(authBoard) {
                ChronoFlowTheme(darkTheme = dark) {
                    if (signUp) SignUpSample(state, board = true) else VerifySample(checkNotNull(verify))
                }
            }
        }
        BoardCapture.capture(compose, name)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("AuthSignUp_none_en_light", "form", false),
                arrayOf("AuthSignUp_none_en_dark", "form", true),
                arrayOf("AuthSignUp_taken_en_light", "taken", false),
                arrayOf("AuthSignUp_taken_en_dark", "taken", true),
                arrayOf("AuthSignUp_server_en_light", "server", false),
                arrayOf("AuthSignUp_server_en_dark", "server", true),
                arrayOf("AuthVerify_en_light", "countdown", false),
                arrayOf("AuthVerify_en_dark", "countdown", true),
                arrayOf("AuthVerify_server_en_light", "server", false),
                arrayOf("AuthVerifyFailed_en_light", "failed", false),
                arrayOf("AuthVerifyFailed_en_dark", "failed", true),
                arrayOf("auth/SignUpOffline_en_light", "offline", false),
                arrayOf("auth/VerifyWaiting_en_light", "waiting", false),
                arrayOf("auth/VerifySignOutError_en_light", "signOutError", false),
            )
    }
}
