package net.chronoflow.app.reminders

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.settings.AppLanguageApplier
import net.chronoflow.app.settings.LanguageFeedback
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class AppLanguageSourceTest {
    @Test fun systemChoiceUsesDutchPhoneLanguage() = checkLanguage("", "nl-rNL", Language.Nl)

    @Test fun englishChoiceOverridesDutchPhone() = checkLanguage("en", "nl-rNL", Language.En)

    @Test fun dutchChoiceOverridesEnglishPhone() = checkLanguage("nl", "en-rUS", Language.Nl)

    private fun checkLanguage(
        locales: String,
        qualifiers: String,
        expected: Language,
    ) = runTest(timeout = 60.seconds) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        val app = ApplicationProvider.getApplicationContext<Application>()
        checkNotNull(app.getSystemService(LocaleManager::class.java)).applicationLocales =
            LocaleList.forLanguageTags(locales)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferences = DevicePreferences(app, MemoryLanguagePreferences(), dispatcher)
        val applier = AppLanguageApplier(app, preferences, dispatcher, LanguageFeedback())
        assertEquals(expected, AppLanguageSource(applier, dispatcher).current())
    }
}

private class MemoryLanguagePreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}
