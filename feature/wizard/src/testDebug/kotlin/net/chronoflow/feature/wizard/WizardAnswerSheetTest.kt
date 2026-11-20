package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.Fraunces20
import net.chronoflow.core.designsystem.theme.toTextStyle
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.sleep.WizardTimeSheetUi
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardAnswerSheetTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val haptics = RecordingHaptics()
    private val events = mutableListOf<WizardEvent>()

    private fun state(): WizardUiState {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) { state = boardState("WizardSheetFreeSleep", Locale.ENGLISH) }
        return state
    }

    private fun show(state: WizardUiState = state()) {
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
        if (state.answerSheet?.openTime == null) {
            compose.onNodeWithTag("wizard.answer.title").assertIsFocused()
        } else {
            compose.onNodeWithTag("wizard.time-sheet.title").assertIsFocused()
        }
    }

    @Test fun freeDaySheetHasQuestionActionsAndSelectedRadioRows() {
        show()
        compose.onNodeWithTag("wizard.answer.free_day_times").assertExists()
        compose.onNodeWithTag("wizard.answer.question").assertExists()
        compose.onNodeWithTag("wizard.answer.cancel").assertExists()
        compose.onNodeWithTag("wizard.answer.done").assertExists()
        compose
            .onNodeWithTag("wizard.answer.option.0")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag("wizard.answer.option.1")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag("wizard.answer.options")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        compose.onNodeWithTag("wizard.answer.time.free_day_bed").assertExists()
        compose.onNodeWithTag("wizard.answer.time.free_day_wake").assertExists()
    }

    @Test fun headerTitleIsCenteredBetweenTheSheetEdges() {
        show()
        val sheet = compose.onNodeWithTag("wizard.answer.free_day_times").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag("wizard.answer.title").getUnclippedBoundsInRoot()
        assertEquals(
            (sheet.left.value + sheet.right.value) / 2,
            (title.left.value + title.right.value) / 2,
            .5f,
        )
    }

    @Test fun headerUsesTheBoardDisplayStyle() {
        show()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("wizard.answer.title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        val expected = CfTypeScale.wizardSheetTitle.toTextStyle()
        val actual = layouts.single().layoutInput.style
        assertEquals(Fraunces20, actual.fontFamily)
        assertEquals(FontWeight.W500, actual.fontWeight)
        assertEquals(FontSynthesis.None, actual.fontSynthesis)
        assertEquals(expected.fontSize, actual.fontSize)
        assertEquals(expected.lineHeight, actual.lineHeight)
        assertEquals(expected.letterSpacing, actual.letterSpacing)
    }

    @Test fun sameAsWorkdaysHidesTheTimes() {
        val base = state()
        val sheet = base.answerSheet!!
        show(
            base.copy(
                answerSheet =
                    sheet.copy(
                        state =
                            sheet.state.update(
                                "free_day_status",
                                JsonPrimitive("same_as_workdays"),
                            ),
                    ),
            ),
        )
        compose.onNodeWithTag("wizard.answer.time.free_day_bed").assertDoesNotExist()
        compose.onNodeWithTag("wizard.answer.time.free_day_wake").assertDoesNotExist()
    }

    @Test fun optionTapEmitsUpdateAndSelection() {
        show()
        compose.onNodeWithTag("wizard.answer.option.0").performClick()
        assertEquals(listOf(WizardEvent.UpdateAnswer("free_day_status", JsonPrimitive("same_as_workdays"))), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun declaredWithoutTimesDisablesDone() {
        val base = state()
        val sheet = base.answerSheet!!
        show(base.copy(answerSheet = sheet.copy(state = sheet.state.update("free_day_bed", JsonNull))))
        compose.onNodeWithTag("wizard.answer.done").assertIsNotEnabled()
    }

    @Test fun bedRowEmitsOpenTimeSheet() {
        show()
        compose.onNodeWithTag("wizard.answer.time.free_day_bed").performClick()
        assertEquals(listOf(WizardEvent.OpenTimeSheet("free_day_bed")), events)
    }

    @Test fun stackedTimeSheetHasFootnoteWithoutSubtitle() {
        val base = state()
        show(base.copy(answerSheet = base.answerSheet!!.copy(openTime = WizardTimeSheetUi("free_day_bed", "00:30"))))
        compose.onNodeWithTag("wizard.time-sheet.footnote").assertExists()
        compose.onNodeWithTag("wizard.time-sheet.subtitle").assertDoesNotExist()
    }
}
