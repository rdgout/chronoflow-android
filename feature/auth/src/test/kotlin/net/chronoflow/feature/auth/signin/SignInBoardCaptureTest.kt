package net.chronoflow.feature.auth.signin

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
class SignInBoardCaptureTest(
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
        compose.setContent {
            ProfileHost(
                cell.profile,
            ) { ChronoFlowTheme(darkTheme = cell.dark) { SignInSample(state) } }
        }
        val name = if (state == "wrong") "AuthSignInError" else "AuthSignIn_$state"
        BoardCapture.capture(compose, "${name}_${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                "none",
                "locked",
                "server",
                "wrong",
            ).flatMap { state -> BoardCaptureCell.standard.map { cell -> arrayOf(state, cell) } }
    }
}
