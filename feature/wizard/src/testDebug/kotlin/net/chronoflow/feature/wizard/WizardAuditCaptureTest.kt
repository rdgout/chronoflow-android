package net.chronoflow.feature.wizard

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.times.WizardPicker
import net.chronoflow.feature.wizard.times.WizardPickerKind
import net.chronoflow.feature.wizard.times.WizardTimesUi
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
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WizardAuditCaptureTest(
    private val board: String,
    private val variant: String,
    private val cell: BoardCaptureCell,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain.outerRule(ProfileRule(cell.profile, cell.language, cell.dark)).around(compose)

    @Test fun capturesTheExistingSearchOrPicker() {
        val locale = Locale.forLanguageTag(cell.language.tag)
        lateinit var state: WizardUiState
        lateinit var formatter: net.chronoflow.core.plan.CopyLineFormatter
        runTest(timeout = 60.seconds) {
            state = boardState(if (board == "TripAirportSearch") "WizardAirportFrom" else "WizardOutbound", locale)
            if (board == "PickerDateTime") {
                val kind = if (variant == "date") WizardPickerKind.Date else WizardPickerKind.Time
                val picker = WizardPicker(0, false, kind)
                state =
                    state.copy(
                        times =
                            WizardTimesUi.from(
                                WizardBoardCaptureSeed.draft("WizardOutbound", locale),
                                home = false,
                                now = WizardBoardCaptureSeed.now,
                                picker = picker,
                            ),
                    )
            }
            formatter = wizardCopyFormatter()
        }
        compose.setContent {
            ProfileHost(cell.profile) {
                ChronoFlowTheme(darkTheme = cell.dark, reduceMotion = true) { WizardScreen(state, formatter, {}) }
            }
        }
        if (board == "TripAirportSearch") compose.onNodeWithTag("wizard.airport.query").assertIsFocused()
        captureWizard(compose, "${board}_${variant}_${cell.suffix}", overlay = true)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "TripAirportSearch" to "results",
                "PickerDateTime" to "date",
                "PickerDateTime" to "time",
            ).flatMap { (board, variant) ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(board, variant, cell) }
            }
    }
}
