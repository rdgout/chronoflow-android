package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ButtonTextLayoutTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.small, AppLanguage.Nl),
            ).around(compose)

    @Test fun buttonLabelHasACompleteLineBox() {
        compose.setContent {
            ChronoFlowTheme {
                Column {
                    CfButton(
                        stringResource(R.string.screens_mark_as_done),
                        {
                        },
                        Modifier.testTag(
                            "probe",
                        ),
                        size = CfButtonSize.Lg,
                        leadingIcon = LucideGlyphs.Check.imageVector(),
                    )
                    CfButton(
                        stringResource(R.string.screens_skip),
                        {},
                        Modifier.testTag("probe.sm"),
                        size = CfButtonSize.Sm,
                    )
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("probe").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(1, results.size)
        val result = results.single()
        assertTrue(!result.multiParagraph.didExceedMaxLines)
        for (line in 0 until result.lineCount) {
            assertTrue(result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1f)
            assertTrue(result.getLineBottom(line) <= result.size.height + 1f)
        }
        val small = compose.onNodeWithTag("probe.sm").fetchSemanticsNode()
        assertEquals(32f * DeviceProfiles.small.density, small.boundsInRoot.height, 1f)
        assertTrue(small.layoutInfo.height >= 48f * DeviceProfiles.small.density)
    }
}
