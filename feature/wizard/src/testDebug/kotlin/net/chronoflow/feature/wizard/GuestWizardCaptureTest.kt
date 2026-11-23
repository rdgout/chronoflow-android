package net.chronoflow.feature.wizard

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class GuestWizardCaptureTest(
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
        val state = guestBoard(board, Locale.forLanguageTag(language.tag))
        lateinit var formatter: net.chronoflow.core.plan.CopyLineFormatter
        runTest(timeout = 60.seconds) { formatter = wizardCopyFormatter() }
        val motion = board == "WizardCheckingGuest"
        if (motion) compose.mainClock.autoAdvance = false
        compose.setContent {
            ProfileHost(wizardBoardProfile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = !motion) { WizardScreen(state, formatter, {}) }
            }
        }
        if (motion) {
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(2_936, ignoreFrameDuration = true)
        }
        captureWizard(compose, board, state.discardDialogVisible)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = GuestWizardBoardSeed.names
    }
}
