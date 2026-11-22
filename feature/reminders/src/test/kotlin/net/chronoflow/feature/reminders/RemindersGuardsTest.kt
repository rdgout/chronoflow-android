package net.chronoflow.feature.reminders

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.settings.SettingsWideMetrics
import net.chronoflow.feature.reminders.support.RemindersScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemindersGuardsTest(
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : RemindersScreenTest(profile, language, dark) {
    @Test fun blockedFitsTheProfile() = guard(ReminderPermission.Blocked)

    @Test fun activeFitsTheProfile() = guard(ReminderPermission.Granted)

    private fun guard(permission: ReminderPermission) {
        show { RemindersScreen(RemindersUiState(ReminderSettings(), permission), {}) }
        Guards.assertTextFits(compose, profile)
        Guards.assertTouchTargets(compose, profile)
        Guards.assertInteractiveInSafeArea(compose, profile, rootTag = "reminders.screen")
        // M3 joins adjacent segments by overlapping their one-dp borders.
        Guards.assertNoInteractiveOverlap(
            compose,
            setOf("reminders.lead.0", "reminders.lead.1", "reminders.lead.2"),
            "reminders.screen",
        )
        if (permission == ReminderPermission.Granted) {
            val group = compose.onNodeWithTag("reminders.lead").fetchSemanticsNode().boundsInRoot
            val first = compose.onNodeWithTag("reminders.lead.0").fetchSemanticsNode().boundsInRoot
            val second = compose.onNodeWithTag("reminders.lead.1").fetchSemanticsNode().boundsInRoot
            val third = compose.onNodeWithTag("reminders.lead.2").fetchSemanticsNode().boundsInRoot
            val border = AuthTokens.borderWidth * profile.density
            assertTrue(first.left >= group.left - 1f && third.right <= group.right + 1f)
            assertTrue(abs(first.right - second.left) <= border + 1f)
            assertTrue(abs(second.right - third.left) <= border + 1f)
            assertTrue(first.right < third.left)
        }
        Guards.assertInk(compose, profile)
        if (profile.widthDp >= 600) {
            val bounds = compose.onNodeWithTag("reminders.screen.content").fetchSemanticsNode().boundsInRoot
            assertEquals(SettingsWideMetrics.COLUMN * profile.density, bounds.width, 1f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "profile={0}-language={1}-dark={2}")
        fun cases() = remindersGuardCases()
    }
}
