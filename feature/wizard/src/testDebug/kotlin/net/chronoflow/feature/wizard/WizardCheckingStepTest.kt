package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class WizardCheckingStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()

    private fun show(
        board: String = "WizardChecking",
        reduceMotion: Boolean = true,
    ) {
        val state = completionBoard(board)
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    WizardScreen(state, events::add)
                }
            }
        }
    }

    @Test fun checkingHasThreeLinesAndNoFooterOrBack() {
        show()
        compose.onNodeWithTag("wizard.checking").assertExists()
        compose.onNodeWithTag("wizard.checking.card").assertExists()
        compose.onNodeWithTag("wizard.checking.line.0").assertExists()
        compose.onNodeWithTag("wizard.checking.line.1").assertExists()
        compose.onNodeWithTag("wizard.checking.line.2").assertExists()
        compose.onNodeWithTag("wizard.checking.slow").assertDoesNotExist()
        compose.onNodeWithTag("wizard.checking.footer").assertDoesNotExist()
        compose.onNodeWithTag("wizard.back").assertDoesNotExist()
    }

    @Test fun slowBoardHasThePoliteSlowLine() {
        show("WizardCheckingSlow")
        compose
            .onNodeWithTag("wizard.checking.slow")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun failureHasNoSubtitle() {
        show("WizardCheckOffline")
        compose.onNodeWithTag("wizard.subtitle").assertDoesNotExist()
    }

    @Test fun linesAnnouncePolitely() {
        show()

        fun assertLine(index: Int) {
            compose
                .onNodeWithTag("wizard.checking.line.$index")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        }
        assertLine(0)
        assertLine(1)
        assertLine(2)
    }

    @Test fun reduceMotionShowsAStaticDot() {
        show()
        compose.onNodeWithTag("wizard.checking.line.2.dot", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("wizard.checking.line.2.spinner", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun motionShowsTheSpinner() {
        show(reduceMotion = false)
        compose.onNodeWithTag("wizard.checking.line.2.spinner", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("wizard.checking.line.2.dot", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun arcAndCodesAreHidden() {
        show()
        compose.onNodeWithTag("wizard.checking.arc").onChildren().assertCountEquals(0)
    }

    @Test fun systemBackEmitsBack() {
        show()
        Espresso.pressBack()
        assertEquals(listOf(WizardEvent.Back), events)
    }
}
