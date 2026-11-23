package net.chronoflow.feature.onboarding.walkthrough

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "nl")
class WalkthroughCopyTests {
    @Test fun everyBoardStringHasPairedRNDutch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nl = context.resources
        assertEquals("Overslaan", nl.getString(R.string.walkthrough_skip))
        assertEquals("Volgende", nl.getString(R.string.walkthrough_next))
        assertEquals("Klaar", nl.getString(R.string.walkthrough_done))
        assertEquals("2 van 3", nl.getString(R.string.walkthrough_current_of_total, "2", "3"))
        assertEquals("Je plan op één tijdlijn", nl.getString(R.string.walkthrough_your_plan_on_one_timeline))
        assertEquals("Herken elke stap in één oogopslag", nl.getString(R.string.walkthrough_know_each_step_at_a_glance))
        assertEquals("Vink af wat je hebt gedaan", nl.getString(R.string.walkthrough_tick_off_what_you_did))
        val en =
            context
                .createConfigurationContext(
                    Configuration(nl.configuration).apply {
                        setLocale(Locale.ENGLISH)
                    },
                ).resources
        R.string::class.java.fields.filter { it.name.startsWith("walkthrough_") }.forEach { field ->
            assertTrue(field.name, nl.getString(field.getInt(null)).isNotBlank())
            assertTrue(field.name, en.getString(field.getInt(null)).isNotBlank())
        }
    }
}
