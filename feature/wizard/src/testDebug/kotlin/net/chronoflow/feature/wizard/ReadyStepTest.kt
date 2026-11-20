@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ready.ReadyBannerUi
import net.chronoflow.feature.wizard.ready.ReadyRow
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

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ReadyStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun show(state: WizardUiState) {
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
    }

    @Test fun noCreditsHasOnlyTheCloseActionInItsFooter() {
        val review = generationReview()
        show(
            readyScreen(
                review.copy(
                    canSave = false,
                    saveBlockedReason = "Not enough credits",
                    price = review.price!!.copy(balance = 0),
                ),
            ),
        )
        compose.onNodeWithTag("wizard.ready.continue").assertDoesNotExist()
        compose.onNodeWithTag("wizard.ready.no-credits").assertHasClickAction().performClick()
        compose.onAllNodes(hasAnyAncestor(hasTestTag("wizard.ready.footer")) and hasClickAction()).assertCountEquals(1)
        assertEquals(listOf(WizardEvent.CloseFromFooter), events.filter { it != WizardEvent.ReadyTick })
    }

    @Test fun checkingButtonIsDisabledAndIndeterminate() {
        show(readyScreen(reviewing = true))
        compose
            .onNodeWithTag("wizard.ready.continue")
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
    }

    @Test fun saveFailureHasItsBannerTag() {
        val state = readyScreen()
        show(
            state.copy(
                ready = state.ready!!.copy(banner = ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.offline, 0)),
            ),
        )
        compose.onNodeWithTag("wizard.save.offline").assertExists()
    }

    @Test fun routeTapSendsTheRowIntentAndSelectionHaptic() {
        show(readyScreen())
        compose.onNodeWithTag("wizard.ready.route").performScrollTo().performClick()
        assertEquals(
            listOf(WizardEvent.RevisitFromReady(ReadyRow.Route)),
            events.filter { it != WizardEvent.ReadyTick },
        )
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun makePlanTapSendsContinueAndMediumHaptic() {
        show(readyScreen())
        compose.onNodeWithTag("wizard.ready.continue").performClick()
        assertEquals(listOf(HapticEvent.Medium), haptics.events)
        assertEquals(WizardEvent.Continue(net.chronoflow.feature.wizard.shell.WizardStepKey.Ready), events.last())
    }
}
