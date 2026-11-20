@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    private fun assertActions() {
        compose.onNodeWithTag("wizard.answer.cancel").assertExists()
        compose.onNodeWithTag("wizard.answer.done").assertExists()
    }

    private fun selectedOption(index: Int) {
        compose
            .onNodeWithTag("wizard.answer.option.$index")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag("wizard.answer.options")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test fun seatHasSelectedOptionsAndUpdatesTheAnswer() {
        show(completionBoard("WizardSheet"))
        assertActions()
        selectedOption(0)
        compose.onNodeWithTag("wizard.answer.option.1").performClick()
        assertEquals(listOf(WizardEvent.UpdateAnswer("seat_window", JsonPrimitive("right"))), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun roomHasItsTimeOnlyWhenDeclared() {
        show(completionBoard("WizardSheetRoom"))
        assertActions()
        selectedOption(1)
        compose.onNodeWithTag("wizard.answer.room-time").assertExists().performClick()
        assertEquals(listOf(WizardEvent.OpenTimeSheet("stay.bed_available_from_local")), events)
    }

    @Test fun roomAtArrivalHidesTheTime() {
        val base = completionBoard("WizardSheetRoom")
        val sheet = base.answerSheet!!
        show(
            base.copy(
                answerSheet =
                    sheet.copy(
                        state = sheet.state.update("stay.bed_available_mode", JsonPrimitive("arrival")),
                    ),
            ),
        )
        compose.onNodeWithTag("wizard.answer.room-time").assertDoesNotExist()
    }

    @Test fun checkoutUsesBooleanOptions() {
        show(completionBoard("WizardSheetCheckout"))
        assertActions()
        selectedOption(1)
        compose.onNodeWithTag("wizard.answer.option.0").performClick()
        assertEquals(listOf(WizardEvent.UpdateAnswer("stay.late_check_out_or_day_room", JsonPrimitive(true))), events)
    }

    @Test fun wakeHasTheInlinePicker() {
        show(completionBoard("WizardSheetWake"))
        assertActions()
        compose
            .onNodeWithTag("wizard.answer.latest_wake")
            .assert(SemanticsMatcher.keyIsDefined(net.chronoflow.feature.wizard.times.PickerIs24Hour))
    }

    @Test fun busyHasTwoLargeRemoveTargetsAndAdd() {
        show(completionBoard("WizardSheetBusy"))
        assertActions()
        compose.onNodeWithTag("wizard.answer.remove-busy.0").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("wizard.answer.remove-busy.1").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("wizard.answer.add-busy").assertExists()
    }

    @Test fun busyPickingUsesTheFourDialogsAndWritesOnlyAtTheEnd() {
        show(completionBoard("WizardSheetBusy"))
        compose.onNodeWithTag("wizard.answer.busy-range.0").performClick()
        compose.onNodeWithTag("wizard.date-dialog").assertExists()
        compose.onNodeWithTag("wizard.picker.done").performClick()
        compose.onNodeWithTag("wizard.time-dialog").assertExists()
        compose.onNodeWithTag("wizard.picker.done").performClick()
        compose.onNodeWithTag("wizard.date-dialog").assertExists()
        compose.onNodeWithTag("wizard.picker.done").performClick()
        compose.onNodeWithTag("wizard.time-dialog").assertExists()
        assertEquals(emptyList<WizardEvent>(), events)
        compose.onNodeWithTag("wizard.picker.done").performClick()
        assertEquals(1, events.size)
        assertEquals("pre_departure_busy_intervals", (events.single() as WizardEvent.UpdateAnswer).field)
    }

    @Test fun busyToAtOrBeforeFromDisablesConfirm() {
        val start = java.time.LocalDateTime.of(2026, 10, 9, 9, 0)
        val picker = androidx.compose.material3.TimePickerState(9, 0, true)
        compose.setContent {
            WizardTestHost {
                net.chronoflow.feature.wizard.answers.WizardBusyTimeDialog(
                    picker,
                    start,
                    start,
                    true,
                    "",
                    "",
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithTag("wizard.picker.done").assertIsNotEnabled()
        compose.runOnIdle { picker.hour = 8 }
        compose.onNodeWithTag("wizard.picker.done").assertIsNotEnabled()
        compose.runOnIdle { picker.hour = 10 }
        compose.onNodeWithTag("wizard.picker.done").assertIsEnabled()
    }

    @Test fun drinksHaveSteppersAndNoRing() {
        show(completionBoard("WizardSheetDrinks"))
        assertActions()
        compose.onNodeWithTag("wizard.answer.cups_filter_coffee.minus", useUnmergedTree = true).assertIsEnabled()
        compose.onNodeWithTag("wizard.answer.cups_tea.minus", useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag("wizard.answer.drinks.ring").assertDoesNotExist()
    }

    @Test fun drinksErrorHasARingAndDisabledDone() {
        show(completionBoard("WizardSheetDrinksError"))
        assertActions()
        compose.onNodeWithTag("wizard.answer.drinks.ring").assertExists()
        compose.onNodeWithTag("wizard.answer.error").assertExists()
        compose.onNodeWithTag("wizard.answer.done").assertIsNotEnabled()
    }

    @Test fun lightHasItsNoticeAndSelectedRadio() {
        show(completionBoard("WizardSheetLight"))
        assertActions()
        selectedOption(1)
        compose.onNodeWithTag("wizard.answer.lightNotice").assertExists()
    }

    @Test fun blueHasTheRatingFieldWithoutARing() {
        show(completionBoard("WizardSheetBlue"))
        assertActions()
        selectedOption(1)
        compose.onNodeWithTag("wizard.answer.rating").assertExists()
        compose.onNodeWithTag("wizard.answer.rating.ring").assertDoesNotExist()
    }

    @Test fun blueErrorHasARingAndDisabledDone() {
        show(completionBoard("WizardSheetBlueError"))
        compose.onNodeWithTag("wizard.answer.rating.ring").assertExists()
        compose.onNodeWithTag("wizard.answer.error").assertExists()
        compose.onNodeWithTag("wizard.answer.done").assertIsNotEnabled()
    }

    @Test fun dismissIsCancel() {
        show(completionBoard("WizardSheet"))
        androidx.test.espresso.Espresso
            .pressBack()
        compose.waitForIdle()
        assertEquals(listOf(WizardEvent.CancelAnswer), events)
        assertEquals(emptyList<HapticEvent>(), haptics.events)
    }

    @Test fun ratingSurvivesRestoration() {
        val current = androidx.compose.runtime.mutableStateOf(completionBoard("WizardSheetBlue"))
        val restore =
            androidx.compose.ui.test.junit4
                .StateRestorationTester(compose)
        restore.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) {
                    WizardScreen(current.value) { event ->
                        events.add(event)
                        if (event is WizardEvent.UpdateAnswer) {
                            val sheet = current.value.answerSheet!!
                            current.value =
                                current.value.copy(
                                    answerSheet = sheet.copy(state = sheet.state.update(event.field, event.value)),
                                )
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("wizard.answer.rating").performTextReplacement("2")
        compose.waitForIdle()
        restore.emulateSavedInstanceStateRestore()
        assertEquals(
            "2",
            compose
                .onNodeWithTag(
                    "wizard.answer.rating",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(listOf(WizardEvent.UpdateAnswer("blue_blocker_mdfd", JsonPrimitive("2"))), events)
    }
}
