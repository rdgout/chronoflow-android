package net.chronoflow.feature.reminders

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.reminders.support.RemindersScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FirstTripRemindersSheetTest : RemindersScreenTest() {
    @Test fun buttonsEmitTheDistinctActionsAndTheTitleIsAHeading() {
        var enables = 0
        var dismisses = 0
        show {
            FirstTripRemindersSheetContent(
                RemindersUiState(settings = ReminderSettings()),
                { enables++ },
                { dismisses++ },
            )
        }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)).assertCountEquals(0)
        compose.onAllNodesWithTag("reminders.firstTrip.enable").assertCountEquals(1)
        compose.onAllNodesWithTag("reminders.firstTrip.notNow").assertCountEquals(1)
        compose.onNodeWithTag("reminders.firstTrip.enable").performClick()
        compose.onNodeWithTag("reminders.firstTrip.notNow").performClick()
        assertEquals(1, enables)
        assertEquals(1, dismisses)
        compose
            .onNodeWithTag(
                "reminders.firstTrip.title",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test fun missingSettingsDisableEnable() {
        show { FirstTripRemindersSheetContent(RemindersUiState(), {}, {}) }
        compose.onNodeWithTag("reminders.firstTrip.enable").assertIsNotEnabled()
    }

    @Test fun busyDisablesEnable() {
        show { FirstTripRemindersSheetContent(RemindersUiState(settings = ReminderSettings(), busy = true), {}, {}) }
        compose.onNodeWithTag("reminders.firstTrip.enable").assertIsNotEnabled()
    }
}
