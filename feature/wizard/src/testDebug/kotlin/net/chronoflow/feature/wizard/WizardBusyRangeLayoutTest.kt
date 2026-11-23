package net.chronoflow.feature.wizard

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardBusyRangeLayoutTest {
    private val compose = createComposeRule()
    private val profile = DeviceProfiles.pixel8

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun rangeTextAndRemoveButtonShareTheirVerticalCenter() {
        val state = completionBoard("WizardSheetBusy")
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val range = hasTestTag("wizard.answer.busy-range.0")
        val label =
            compose.onNode(
                (range or hasAnyAncestor(range)) and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        val results = mutableListOf<TextLayoutResult>()
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val paragraph = results.single()
        val textBounds = label.getUnclippedBoundsInRoot()
        val textCenter =
            textBounds.top.value +
                (paragraph.getLineTop(0) + paragraph.getLineBottom(paragraph.lineCount - 1)) / (2 * profile.density)
        val remove = compose.onNodeWithTag("wizard.answer.remove-busy.0").getUnclippedBoundsInRoot()
        val removeCenter = (remove.top.value + remove.bottom.value) / 2
        assertTrue("text center=$textCenter remove center=$removeCenter", abs(textCenter - removeCenter) <= 1)
    }
}
