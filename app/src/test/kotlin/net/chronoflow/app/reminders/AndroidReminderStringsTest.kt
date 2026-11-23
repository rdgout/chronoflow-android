package net.chronoflow.app.reminders

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderKind
import net.chronoflow.core.testing.reminders.FakeReminderStrings
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AndroidReminderStringsTest(
    private val kind: ReminderKind,
) {
    @Test fun englishAndDutchMatchEngineFixture() {
        val strings = AndroidReminderStrings(ApplicationProvider.getApplicationContext())
        val expected = FakeReminderStrings()
        assertEquals(
            expected.text(kind.copyKey, Language.En, "01:00"),
            strings.text(kind.copyKey, Language.En, "01:00"),
        )
        assertEquals(
            expected.text(kind.copyKey, Language.Nl, "01:00"),
            strings.text(kind.copyKey, Language.Nl, "01:00"),
        )
    }

    @Test fun unknownKeyAndChannelNames() {
        val strings = AndroidReminderStrings(ApplicationProvider.getApplicationContext())
        assertEquals("Unknown 01:00", strings.text("Unknown {{time}}", Language.En, "01:00"))
        assertEquals("Reminders on this phone", strings.channelName(Language.En))
        assertEquals("Herinneringen op deze telefoon", strings.channelName(Language.Nl))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun kinds(): List<Array<ReminderKind>> = ReminderKind.entries.map { arrayOf(it) }
    }
}
