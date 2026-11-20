package net.chronoflow.feature.wizard

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardBoardCaptureTest(
    private val board: String,
) {
    private val compose = createComposeRule()
    private val language = if (board.endsWith("NL")) AppLanguage.Nl else AppLanguage.En
    private val dark = "Dark" in board

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(wizardBoardProfile, language, dark),
            ).around(compose)

    @Test fun capturesTheBoardState() {
        lateinit var state: WizardUiState
        var keyboard: SoftwareKeyboardController? = null
        runTest(timeout = 60.seconds) { state = boardState(board, Locale.forLanguageTag(language.tag)) }
        compose.setContent {
            ProfileHost(wizardBoardProfile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                    val controller = LocalSoftwareKeyboardController.current
                    SideEffect { keyboard = controller }
                    WizardScreen(state, {})
                }
            }
        }
        // Focus is granted only after SheetValue.Expanded, so it also waits out the native expansion.
        if (state.sheet != null) compose.onNodeWithTag("wizard.airport.query").assertIsFocused()
        compose.runOnIdle { keyboard?.hide() }
        compose.waitForIdle()
        captureWizard(
            compose,
            if (board ==
                "WizardWhereLimit"
            ) {
                "WizardWhereLimit_noBoard"
            } else {
                board
            },
            state.sheet != null || state.discardDialogVisible,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = WizardBoardCaptureSeed.names
    }
}
