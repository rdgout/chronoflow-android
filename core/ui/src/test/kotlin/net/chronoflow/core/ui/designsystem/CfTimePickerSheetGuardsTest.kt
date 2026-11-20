@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.system.CfTimePickerSheet
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
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
class CfTimePickerSheetGuardsTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private var state = TimePickerState(22, 30, true)
    private val supporting = mutableStateOf(true)
    private var confirmed: Triple<Int, Int, Boolean>? = null
    private var dismissed = 0

    private fun show(
        initialHour: Int = 22,
        initialMinute: Int = 30,
        restoration: StateRestorationTester? = null,
    ) {
        val content: @Composable () -> Unit = {
            val pickerState =
                if (restoration == null) state else rememberTimePickerState(initialHour, initialMinute, true)
            SideEffect { state = pickerState }
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false, reduceMotion = true) {
                    CfTimePickerSheet(
                        title = "Title",
                        subtitle = if (supporting.value) "Subtitle" else null,
                        footnote = if (supporting.value) "Footnote" else null,
                        initialHour = initialHour,
                        initialMinute = initialMinute,
                        is24Hour = true,
                        confirmLabel = "Done",
                        dismissLabel = "Cancel",
                        onConfirm = { hour, minute, touched -> confirmed = Triple(hour, minute, touched) },
                        onDismiss = { dismissed++ },
                        confirmTag = "custom.done",
                        dismissTag = "custom.cancel",
                        state = pickerState,
                    )
                }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    @Test fun optionalSlotsAndHeadingAreStructural() {
        show()
        compose.onNodeWithTag("time-sheet.title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("time-sheet.subtitle").assertExists()
        compose.onNodeWithTag("time-sheet.footnote").assertExists()
        compose.runOnIdle { supporting.value = false }
        compose.onNodeWithTag("time-sheet.subtitle").assertDoesNotExist()
        compose.onNodeWithTag("time-sheet.footnote").assertDoesNotExist()
    }

    @Test fun untouchedConfirmKeepsTheOriginalMinutes() {
        show()
        compose.onNodeWithTag("custom.done").performClick()
        assertEquals(Triple(22, 30, false), confirmed)
        assertEquals(0, dismissed)
    }

    @Test fun changedStateIsPassedWithoutRounding() {
        show()
        compose.runOnIdle {
            state.hour = 7
            state.minute = 13
        }
        compose.onNodeWithTag("custom.done").performClick()
        assertEquals(Triple(7, 13, true), confirmed)
    }

    @Test fun returningToTheInitialNoonStillCountsAsInteraction() {
        state.hour = 12
        state.minute = 0
        show(initialHour = 12, initialMinute = 0)
        compose.runOnIdle { state.hour = 13 }
        compose.runOnIdle { state.hour = 12 }
        compose.onNodeWithTag("custom.done").performClick()
        assertEquals(Triple(12, 0, true), confirmed)
    }

    @Test fun changedDialKeepsInteractionAfterRestore() {
        val restoration = StateRestorationTester(compose)
        show(restoration = restoration)
        val original = state
        compose.runOnIdle {
            state.hour = 7
            state.minute = 13
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("custom.done").performClick()
        assertNotSame(original, state)
        assertEquals(Triple(7, 13, true), confirmed)
    }

    @Test fun untouchedDialStaysUntouchedAfterRestore() {
        val restoration = StateRestorationTester(compose)
        show(restoration = restoration)
        val original = state
        compose.runOnIdle { supporting.value = false }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("custom.done").performClick()
        assertNotSame(original, state)
        assertEquals(Triple(22, 30, false), confirmed)
    }

    @Test fun dismissCallsOnlyOnDismiss() {
        show()
        compose.onNodeWithTag("custom.cancel").performClick()
        assertEquals(1, dismissed)
        assertEquals(null, confirmed)
    }

    @Test fun bothActionsHaveFullTargetsAndTheStateKeepsTheHourCycle() {
        show()
        compose.onNodeWithTag("custom.done").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("custom.cancel").assertHeightIsAtLeast(48.dp)
        assertTrue(state.is24hour)
        assertFalse(TimePickerState(7, 15, false).is24hour)
    }

    @Test fun nativePeriodButtonsHaveFullHitAreas() {
        state.is24hour = false
        show()
        val period =
            hasAnyAncestor(hasTestTag("time-sheet")) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        compose.onAllNodes(period).assertCountEquals(2)
        val selected =
            compose
                .onNode(period and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
                .fetchSemanticsNode()
        val unselected =
            compose
                .onNode(period and SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
                .fetchSemanticsNode()
        val minimum = 48f * compose.density.density - .5f
        assertTrue(selected.touchBoundsInRoot.width >= minimum)
        assertTrue(selected.touchBoundsInRoot.height >= minimum)
        assertTrue(unselected.touchBoundsInRoot.width >= minimum)
        assertTrue(unselected.touchBoundsInRoot.height >= minimum)
    }
}
