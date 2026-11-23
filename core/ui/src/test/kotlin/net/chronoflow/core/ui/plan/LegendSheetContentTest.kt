package net.chronoflow.core.ui.plan

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LegendSheetContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun closeAndSections() {
        var closed = false
        compose.setContent { ChronoFlowTheme { LegendSheetContent({ closed = true }) } }
        compose.onNodeWithTag("legend.title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("legend.group.Sleep").assertExists()
        compose.onNodeWithTag("legend.group.Light").performScrollTo().assertExists()
        compose.onNodeWithTag("legend.group.Caffeine").performScrollTo().assertExists()
        compose.onNodeWithTag("legend.group.Travel").performScrollTo().assertExists()
        compose.onNodeWithTag("legend.type.avoid_light").performScrollTo().assertExists()
        compose.onNodeWithTag("legend.importance").performScrollTo().assertExists()
        compose.onNodeWithTag("legend.close").performClick()
        assertTrue(closed)
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchStructure() {
        compose.setContent { ChronoFlowTheme { LegendSheetContent({}) } }
        compose.onNodeWithTag("legend.title").assertExists()
    }
}
