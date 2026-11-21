package net.chronoflow.app.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.ConfigurationPolicy
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 36], application = android.app.Application::class, qualifiers = "en-rUS")
class AppLanguageApplierTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun preferences() = DevicePreferences(context, MemoryLanguageStore(), Dispatchers.IO)

    @Test
    @Config(sdk = [29])
    fun chooseNlRecreatesWithTheSelectedLocale() = chooseNl()

    @Ignore("needs ShadowLocaleManager")
    @Test
    @Config(sdk = [36])
    fun chooseNlRecreatesWithTheSelectedLocaleApi36() = chooseNl()

    private fun chooseNl() =
        runTest(timeout = 60.seconds) {
            val applier = AppLanguageApplier(context, preferences(), Dispatchers.IO, LanguageFeedback())
            val host = Robolectric.buildActivity(LanguagePolicyActivity::class.java).setup()
            try {
                val recreate = applier.apply(LanguageChoice.Nl)
                assertEquals(Build.VERSION.SDK_INT < 33, recreate)
                host.recreate()
                assertEquals(
                    "nl",
                    host
                        .get()
                        .resources.configuration.locales[0]
                        .language,
                )
                if (Build.VERSION.SDK_INT < 33) {
                    val contents =
                        withContext(Dispatchers.IO) {
                            File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME).readText()
                        }
                    assertTrue(contents.contains("language=nl"))
                } else {
                    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
                    assertEquals("nl", locales[0].language)
                }
            } finally {
                host.pause().stop().destroy()
            }
        }

    @Test fun systemFollowsAChangedPhoneLanguage() =
        runTest(timeout = 60.seconds) {
            val applier = AppLanguageApplier(context, preferences(), Dispatchers.IO, LanguageFeedback())
            applier.apply(LanguageChoice.System)
            val host = Robolectric.buildActivity(LanguagePolicyActivity::class.java).setup()
            try {
                assertEquals(
                    "en",
                    host
                        .get()
                        .resources.configuration.locales[0]
                        .language,
                )
                RuntimeEnvironment.setQualifiers("nl-rNL")
                host.recreate()
                assertEquals(
                    "nl",
                    host
                        .get()
                        .resources.configuration.locales[0]
                        .language,
                )
                assertEquals(LanguageChoice.System, applier.current.first())
                assertEquals("nl", applier.phoneLanguage())
                if (Build.VERSION.SDK_INT >= 33) {
                    assertTrue(context.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty)
                }
            } finally {
                host.pause().stop().destroy()
            }
        }

    @Test
    @Config(sdk = [36])
    fun localeManagerIsTheSourceOfTruthAndReconcilesPreferences() =
        runTest(timeout = 60.seconds) {
            val preferences = preferences()
            val applier = AppLanguageApplier(context, preferences, Dispatchers.IO, LanguageFeedback())
            assertFalse(applier.apply(LanguageChoice.Nl))
            assertEquals("nl", context.getSystemService(LocaleManager::class.java).applicationLocales[0].language)
            preferences.setLanguage(LanguageChoice.En)
            assertEquals(LanguageChoice.Nl, applier.current.first())
            assertEquals(LanguageChoice.Nl, preferences.language.first())
        }
}

internal class LanguagePolicyActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))
    }
}

private class MemoryLanguageStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}
