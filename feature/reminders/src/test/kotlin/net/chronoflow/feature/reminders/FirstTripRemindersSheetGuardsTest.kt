package net.chronoflow.feature.reminders

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.reminders.support.RemindersScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FirstTripRemindersSheetGuardsTest(
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : RemindersScreenTest(profile, language, dark) {
    @Test fun sheetFitsTheProfile() {
        show { RemindersSheetFixture(RemindersUiState(settings = ReminderSettings())) }
        Guards.assertAll(
            compose,
            profile,
            headingTags = setOf("reminders.firstTrip.title"),
            interactiveRootTag = "reminders.firstTrip",
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "profile={0}-language={1}-dark={2}")
        fun cases() = remindersGuardCases()
    }
}
