package net.chronoflow.feature.wizard

import android.provider.Settings
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import net.chronoflow.feature.wizard.times.PickerIs24Hour
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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
    private val language = if (board.endsWith("NL") || board == "WizardQShiftLarge") AppLanguage.Nl else AppLanguage.En
    private val dark = "Dark" in board
    private var previousHourCycle: String? = null
    private val profile =
        when (board) {
            "PickerTime" -> wizardBoardProfile.copy(widthDp = 375, heightDp = 812)
            "WizardQShiftLarge" -> wizardBoardProfile.copy(fontScale = 1.3f)
            else -> wizardBoardProfile
        }

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(profile, language, dark),
            ).around(compose)

    @Before fun useTheBoardsPhoneHourCycle() {
        if (board == "PickerTime" || board.startsWith("WizardSheetWake")) {
            val resolver = RuntimeEnvironment.getApplication().contentResolver
            previousHourCycle = Settings.System.getString(resolver, Settings.System.TIME_12_24)
            Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        }
    }

    @After fun restoreThePhoneHourCycle() {
        if (board == "PickerTime" || board.startsWith("WizardSheetWake")) {
            Settings.System.putString(
                RuntimeEnvironment.getApplication().contentResolver,
                Settings.System.TIME_12_24,
                previousHourCycle,
            )
        }
    }

    @Test fun capturesTheBoardState() {
        lateinit var state: WizardUiState
        lateinit var formatter: net.chronoflow.core.plan.CopyLineFormatter
        var keyboard: SoftwareKeyboardController? = null
        runTest(timeout = 60.seconds) {
            state = boardState(board, Locale.forLanguageTag(language.tag))
            formatter = wizardCopyFormatter()
        }
        val motion = board.startsWith("WizardCalculating") || board.startsWith("WizardReadyChecking")
        if (motion) compose.mainClock.autoAdvance = false
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = !motion) {
                    val controller = LocalSoftwareKeyboardController.current
                    SideEffect { keyboard = controller }
                    WizardScreen(state, formatter, {})
                }
            }
        }
        // Focus is granted only after SheetValue.Expanded, so it also waits out the native expansion.
        if (state.sheet != null) compose.onNodeWithTag("wizard.airport.query").assertIsFocused()
        if (state.sleep?.openTime != null) compose.onNodeWithTag("wizard.time-sheet.title").assertIsFocused()
        if (board == "PickerTime" || board.startsWith("WizardSheetWake")) {
            compose
                .onNodeWithTag(if (board == "PickerTime") "wizard.time-sheet" else "wizard.answer.latest_wake")
                .assert(SemanticsMatcher.expectValue(PickerIs24Hour, true))
        }
        if (state.answerSheet != null) compose.onNodeWithTag("wizard.answer.title").assertIsFocused()
        compose.runOnIdle { keyboard?.hide() }
        compose.waitForIdle()
        if (motion) {
            // M3 1.4.0 keyframes place both arc endpoints closest to the boards at this offset.
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(2_936, ignoreFrameDuration = true)
        }
        captureWizard(
            compose,
            if (board in
                setOf(
                    "WizardSaveThrottle",
                    "WizardWhereLimit",
                    "WizardOutboundScrollErrors",
                    "WizardSleepMember",
                    "WizardSleepError",
                    "WizardQAgeMember",
                )
            ) {
                "${board}_noBoard"
            } else {
                board
            },
            state.sheet != null || state.discardDialogVisible || state.sleep?.openTime != null ||
                state.answerSheet != null,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = WizardBoardCaptureSeed.names
    }
}
