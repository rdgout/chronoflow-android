package net.chronoflow.feature.onboarding.terms

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
class TermsCopyTests {
    @Test fun catalogPairsEveryBoardStringAndRetainsRNDutch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nl = context.resources
        assertEquals("Akkoord en verder", nl.getString(R.string.terms_accept_and_continue))
        assertEquals("We hebben onze voorwaarden bijgewerkt", nl.getString(R.string.terms_we_ve_updated_our_terms))
        assertEquals("Opnieuw proberen", nl.getString(R.string.terms_try_again))
        val en =
            context
                .createConfigurationContext(
                    Configuration(nl.configuration).apply {
                        setLocale(Locale.ENGLISH)
                    },
                ).resources
        R.string::class.java.fields.filter { it.name.startsWith("terms_") }.forEach { field ->
            assertTrue(field.name, nl.getString(field.getInt(null)).isNotBlank())
            assertTrue(field.name, en.getString(field.getInt(null)).isNotBlank())
        }
    }
}
