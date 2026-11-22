package net.chronoflow.feature.wizard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WizardStepperAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onlyTheAdjustableParentIsExposedToAccessibility() {
        compose.setContent { StepperHost() }
        compose.onNodeWithTag("stepper").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        compose
            .onNodeWithTag("stepper.minus", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        compose
            .onNodeWithTag("stepper.plus", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
    }
}

@Composable
private fun StepperHost() {
    ChronoFlowTheme(reduceMotion = true) {
        WizardStepper(2, 0..4, stringResource(R.string.wizard_preparation_days), {}, "stepper")
    }
}
