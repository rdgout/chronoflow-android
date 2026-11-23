package net.chronoflow.feature.auth.verify

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
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
    private val cell: BoardCaptureCell,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(cell.profile, cell.language, cell.dark),
            ).around(compose)

    @Test fun capturesTheBoardState() {
        val signUp = name.contains("SignUp")
        var verify: VerifySampleState? = null
        if (!signUp) {
            runTest(timeout = 60.seconds) { verify = verifySnapshotModel(state) }
            if (name == "AuthVerify" || name.startsWith("AuthVerify_")) {
                val sample = checkNotNull(verify)
                verify =
                    sample.copy(remaining = 42, ui = sample.ui.copy(canResend = false))
            }
        }
        compose.setContent {
            ProfileHost(cell.profile) {
                ChronoFlowTheme(darkTheme = cell.dark) {
                    if (signUp) SignUpSample(state, board = true) else VerifySample(checkNotNull(verify))
                }
            }
        }
        BoardCapture.capture(compose, "${name}_${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "AuthSignUp_none" to "form",
                "AuthSignUp_taken" to "taken",
                "AuthSignUp_server" to "server",
                "AuthVerify" to "countdown",
                "AuthVerify_server" to "server",
                "AuthVerifyFailed" to "failed",
                "auth/SignUpOffline" to "offline",
                "auth/VerifyWaiting" to "waiting",
                "auth/VerifySignOutError" to "signOutError",
            ).flatMap { (name, state) ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(name, state, cell) }
            }
    }
}
