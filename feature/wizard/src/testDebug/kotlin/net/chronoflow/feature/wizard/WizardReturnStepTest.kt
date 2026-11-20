@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class WizardReturnStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private fun state(): WizardUiState {
        lateinit var result: WizardUiState
        runTest(timeout = 60.seconds) { result = boardState("WizardReturn", Locale.ENGLISH) }
        return result
    }

    @Test fun choicesHaveRadioSemanticsAndOneWaySelected() {
        val state = state()
        compose.setContent { WizardTestHost { WizardScreen(state, {}) } }
        compose
            .onNodeWithTag(
                "wizard.choice.one-way",
            ).assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag(
                "wizard.choice.add-return",
            ).assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag(
                "wizard.return.choices",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        compose.onNodeWithTag("wizard.return.hint").assertExists()
        compose.onNodeWithTag("wizard.return.continue").assertDoesNotExist()
    }

    @Test fun pendingDisablesBothCardsAndKeepsSelection() {
        val state =
            state().let {
                it.copy(
                    returnChoice =
                        it.returnChoice!!.let { ui ->
                            ui.copy(
                                oneWay = ui.oneWay.copy(enabled = false),
                                addReturn = ui.addReturn.copy(enabled = false),
                            )
                        },
                )
            }
        compose.setContent { WizardTestHost { WizardScreen(state, {}) } }
        compose.onNodeWithTag("wizard.choice.one-way").assertIsNotEnabled().assertIsSelected()
        compose.onNodeWithTag("wizard.choice.add-return").assertIsNotEnabled().assertIsNotSelected()
    }

    @Test fun tapSendsOneChoiceAndOneSelectionHaptic() {
        val state = state()
        val haptics = RecordingHaptics()
        val events = mutableListOf<WizardEvent>()
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(
                    LocalHaptics provides haptics,
                ) { WizardScreen(state, events::add) }
            }
        }
        compose.onNodeWithTag("wizard.choice.add-return").performClick()
        compose.runOnIdle {
            assertEquals(listOf(WizardEvent.ChooseReturn("return_legs")), events)
            assertEquals(listOf(HapticEvent.Selection), haptics.events)
        }
    }
}
