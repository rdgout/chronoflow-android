package net.chronoflow.feature.reminders

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.reminders.support.RemindersScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemindersContentTest : RemindersScreenTest() {
    @Test fun grantedAndActiveShowsOneEnabledSwitchAndLead() {
        display(ReminderPermission.Granted)
        structure(enabled = true, lead = true)
        compose.onNodeWithTag("reminders.enabled").assertIsOn().assertIsEnabled()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertCountEquals(1)
    }

    @Test fun grantedButOffHidesLead() {
        display(ReminderPermission.Granted, ReminderSettings().settingEnabled(false))
        structure(enabled = true)
        compose.onNodeWithTag("reminders.enabled").assertIsOff().assertIsEnabled()
    }

    @Test fun askShowsOnlyTheRequestAction() {
        display(ReminderPermission.Undetermined)
        structure(ask = true)
    }

    @Test fun unsupportedShowsOnlyTheInfoNotice() {
        display(ReminderPermission.Unsupported)
        structure(unsupported = true)
    }

    @Test fun blockedSwitchIsOffAndDisabledWithSettingsAction() {
        display(ReminderPermission.Blocked)
        structure(enabled = true, blocked = true)
        compose.onNodeWithTag("reminders.enabled").assertIsOff().assertIsNotEnabled()
    }

    @Test fun missingSettingsDisableAsk() {
        display(ReminderPermission.Undetermined, null)
        structure(ask = true)
        compose.onNodeWithTag("reminders.ask").assertIsNotEnabled()
    }

    @Test fun stepNumbersAreDecorative() {
        display(ReminderPermission.Blocked)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).assertCountEquals(7)
    }

    @Test fun unreadPermissionShowsOnlyTheHeader() {
        display(null)
        structure()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), true).assertCountEquals(2)
    }

    @Test fun leadOptionReportsSixtyMinutes() {
        var lead: Int? = null
        show {
            RemindersContent(
                RemindersUiState(ReminderSettings(), ReminderPermission.Granted),
                {},
                {},
                { lead = it },
                {},
            )
        }
        compose.onNodeWithTag("reminders.lead.2").performClick()
        assertEquals(60, lead)
    }

    private fun display(
        permission: ReminderPermission?,
        settings: ReminderSettings? = ReminderSettings(),
    ) {
        show { RemindersContent(RemindersUiState(settings, permission), {}, {}, {}, {}) }
    }

    private fun structure(
        ask: Boolean = false,
        unsupported: Boolean = false,
        enabled: Boolean = false,
        blocked: Boolean = false,
        lead: Boolean = false,
    ) {
        exists("reminders.ask", ask)
        exists("reminders.unsupported", unsupported)
        exists("reminders.enabled", enabled)
        exists("reminders.blocked", blocked)
        exists("reminders.openSettings", blocked)
        exists("reminders.lead", lead)
        compose.onAllNodesWithTag("reminders.card").assertCountEquals(1)
    }

    private fun exists(
        tag: String,
        expected: Boolean,
    ) {
        if (expected) compose.onNodeWithTag(tag).assertExists() else compose.onNodeWithTag(tag).assertDoesNotExist()
    }
}
