package net.chronoflow.feature.settings.passkeys

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@Logic
class SettingsPasskeyTextPluralTest {
    @Test fun countUsesThePluralCatalogInBothLanguages() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        fun resources(language: String) =
            context
                .createConfigurationContext(
                    Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) },
                ).resources
        assertEquals("1 passkey", SettingsPasskeyText.count(resources("en"), 1))
        assertEquals("2 passkeys", SettingsPasskeyText.count(resources("en"), 2))
        assertEquals("0 passkeys", SettingsPasskeyText.count(resources("nl"), 0))
        assertEquals("1 passkey", SettingsPasskeyText.count(resources("nl"), 1))
    }
}
