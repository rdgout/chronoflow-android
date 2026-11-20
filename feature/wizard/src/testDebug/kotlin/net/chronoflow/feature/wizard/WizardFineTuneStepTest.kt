package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardFineTuneStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()
    private var dawnText = Color.Unspecified

    private fun show(state: WizardUiState = completionBoard("WizardFineTune")) {
        compose.setContent {
            WizardTestHost {
                dawnText = CfTheme.colors.dawnText
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
    }

    @Test fun rowsAndRequiredStarsFollowThePromptOrder() {
        val state = completionBoard("WizardFineTune")
        show(state)
        val rows = state.fineTune!!.rows

        fun assertRow(row: net.chronoflow.feature.wizard.finetune.ExtraRowUi) {
            compose.onNodeWithTag(row.tag).assertExists()
            if (row.required) {
                compose.onNodeWithTag("${row.tag}.star", useUnmergedTree = true).assertExists()
            } else {
                compose.onNodeWithTag("${row.tag}.star", useUnmergedTree = true).assertDoesNotExist()
            }
        }
        assertRow(rows[0])
        assertRow(rows[1])
        assertRow(rows[2])
        assertRow(rows[3])
        assertRow(rows[4])
        assertRow(rows[5])
        assertTrue(
            compose.onNodeWithTag(rows[0].tag).getUnclippedBoundsInRoot().top <
                compose.onNodeWithTag(rows[1].tag).getUnclippedBoundsInRoot().top,
        )
        compose.onNodeWithTag("wizard.fine-tune.continue").assertIsEnabled()
    }

    @Test fun unansweredValueUsesDawnText() {
        val base = completionBoard("WizardFineTune")
        val row =
            base.fineTune!!
                .rows
                .first()
                .copy(answer = null)
        show(base.copy(fineTune = base.fineTune.copy(rows = listOf(row))))
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithTag("${row.tag}.value", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(
            dawnText,
            layouts
                .single()
                .layoutInput.style.color,
        )
    }

    @Test fun busyDisablesRowsAndExposesIndeterminateProgress() {
        val state = completionBoard("WizardFineTuneBusy")
        show(state)
        compose
            .onNodeWithTag("wizard.fine-tune.continue")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        compose
            .onNodeWithTag(
                state.fineTune!!
                    .rows
                    .first()
                    .tag,
            ).assertIsNotEnabled()
        compose.onNodeWithTag("wizard.preparation").assertIsNotEnabled()
    }

    @Test fun noneHasNoExtras() {
        show(completionBoard("WizardFineTuneNone"))
        compose.onNodeWithTag("wizard.extras").assertDoesNotExist()
        compose.onNodeWithTag("wizard.fine-tune.continue").assertIsEnabled()
    }

    @Test fun rowTapOpensThePromptWithoutHaptics() {
        val state = completionBoard("WizardFineTune")
        show(state)
        val row = state.fineTune!!.rows.first()
        compose.onNodeWithTag(row.tag).performClick()
        assertEquals(listOf(WizardEvent.OpenPrompt(row.promptId)), events)
        assertTrue(haptics.events.isEmpty())
    }

    @Test fun plusEmitsTheNextDayAndSelection() {
        show()
        compose
            .onNodeWithTag(
                "wizard.preparation.plus",
                useUnmergedTree = true,
            ).performScrollTo()
            .performTouchInput { click() }
        assertEquals(listOf(WizardEvent.SetPreparationDays(4)), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun plusAtFourIsDisabled() {
        val base = completionBoard("WizardFineTune")
        show(base.copy(fineTune = base.fineTune!!.copy(preparationDays = 4)))
        compose.onNodeWithTag("wizard.preparation.plus", useUnmergedTree = true).assertIsNotEnabled()
    }

    @Test fun preparationIsAdjustable() {
        show()
        compose.onNodeWithTag("wizard.preparation").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertEquals(listOf(WizardEvent.SetPreparationDays(1)), events)
    }

    @Test fun switchEmitsToggleOn() {
        show()
        compose.onNodeWithTag("wizard.sleep-override").performScrollTo().performClick()
        assertEquals(listOf(WizardEvent.SetSleepOverride(true)), events)
        assertEquals(listOf(HapticEvent.ToggleOn), haptics.events)
    }

    @Test fun preferenceEmitsItsField() {
        show()
        compose.onNodeWithTag("wizard.preference.exclude_caffeine").performScrollTo().performClick()
        assertEquals(listOf(WizardEvent.SetPreference("exclude_caffeine", true)), events)
        assertEquals(listOf(HapticEvent.ToggleOn), haptics.events)
    }

    @Test fun sleepBoardOpensTheOverridePicker() {
        show(completionBoard("WizardFineTuneSleep"))
        compose.onNodeWithTag("wizard.override.wake").assertExists()
        compose.onNodeWithTag("wizard.override.bed").performScrollTo().performClick()
        assertEquals(listOf(WizardEvent.OpenTimeSheet("override.habitual_bed")), events)
    }

    @Test fun guestHasNoSleepOverride() {
        val base = completionBoard("WizardFineTune")
        show(base.copy(fineTune = base.fineTune!!.copy(showSleepSwitch = false)))
        compose.onNodeWithTag("wizard.sleep-override").assertDoesNotExist()
    }

    @Test fun overrideErrorHasItsFieldTag() {
        val base = completionBoard("WizardFineTune")
        show(
            base.copy(
                fineTune =
                    base.fineTune!!.copy(
                        fineTuneErrors =
                            listOf(
                                "override.habitualBed" to FieldMessage("Check your bedtime and wake-up time."),
                            ),
                    ),
            ),
        )
        compose.onNodeWithTag("wizard.fine-tune.error.override.habitualBed").performScrollTo().assertExists()
    }
}
