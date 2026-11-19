package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.Priority
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
class PlanComponentsTest {
    @get:Rule val compose = createComposeRule()

    private fun content() {
        compose.setContent {
            ChronoFlowTheme {
                Column {
                    PriorityTag(stringResource(R.string.screens_important), Modifier.testTag("tag"))
                    PrioritySample(Priority.Core)
                    PriorityKey()
                }
            }
        }
    }

    @Test fun tagAndFourMergedRows() {
        content()
        compose.onNodeWithTag("tag").assertExists()
        compose
            .onNodeWithTag(
                "legend.priorities.title",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("legend.priority.core").assertExists()
        compose.onNodeWithTag("legend.priority.recommended").assertExists()
        compose.onNodeWithTag("legend.priority.optional").assertExists()
        compose.onNodeWithTag("legend.priority.info").assertExists()
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchStructure() {
        content()
        compose.onNodeWithTag("legend.priorities.title").assertExists()
    }
}
