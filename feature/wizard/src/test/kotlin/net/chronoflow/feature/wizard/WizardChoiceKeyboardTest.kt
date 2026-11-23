package net.chronoflow.feature.wizard

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.returnchoice.WizardReturnStep
import net.chronoflow.feature.wizard.returnchoice.WizardReturnUi
import net.chronoflow.feature.wizard.shell.WizardEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WizardChoiceKeyboardTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val expected = listOf("wizard.choice.one-way", "wizard.choice.add-return")

    @Test fun focusDoesNotChooseAndEnterActivatesTheFocusedChoice() {
        val events = mutableListOf<WizardEvent>()
        val ui = WizardReturnUi.from(flightDraft(), false)
        compose.mainClock.autoAdvance = false
        lateinit var keyboardMode: () -> Unit
        compose.setContent {
            val input = LocalInputModeManager.current
            keyboardMode = { input.requestInputMode(InputMode.Keyboard) }
            WizardTestHost { WizardReturnStep(ui, events::add) }
        }
        compose.runOnIdle { keyboardMode() }
        compose.onNodeWithTag(expected[0]).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag(expected[0]).performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag(expected[1]).assertIsFocused().assertIsNotSelected()
        compose.mainClock.advanceTimeBy(250)
        assertEquals(emptyList<WizardEvent>(), events)
        compose.onNodeWithTag(expected[1]).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(WizardEvent.ChooseReturn("return_legs")), events)
    }
}
