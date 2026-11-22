package net.chronoflow.feature.wizard

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
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
class TripSearchStatesCaptureTest(
    private val search: SearchBoardState,
    private val cell: BoardCaptureCell,
) {
    private val profile = cell.profile
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, cell.language, cell.dark)).around(compose)

    @Test fun capturesTheSearchState() {
        lateinit var state: WizardUiState
        var keyboard: SoftwareKeyboardController? = null
        runTest(timeout = 60.seconds) {
            state =
                boardState("WizardStopSearch", Locale.forLanguageTag(cell.language.tag), search)
        }
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = cell.dark, reduceMotion = true) {
                    val controller = LocalSoftwareKeyboardController.current
                    SideEffect { keyboard = controller }
                    WizardScreen(state, {})
                }
            }
        }
        compose.onNodeWithTag("wizard.airport.query").assertIsFocused()
        compose.runOnIdle { keyboard?.hide() }
        compose.waitForIdle()
        val name = if (search == SearchBoardState.Failed) "error" else search.name.lowercase(Locale.ROOT)
        captureWizard(compose, "TripSearchStates_${name}_${cell.suffix}", true)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases() =
            SearchBoardState.entries.flatMap { search ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(search, cell) }
            }
    }
}
