package net.chronoflow.core.datastore

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.DeviceConfig
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DevicePreferencesTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var job: Job = SupervisorJob()
    private var dataStoreDelegate = lazy { createStore() }
    private val dataStore get() = dataStoreDelegate.value
    private val preferences get() = DevicePreferences(context, dataStore, Dispatchers.IO)

    private fun createStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { DevicePreferences.file(context.noBackupFilesDir) },
        )

    @After fun cleanup() = runTest(timeout = 60.seconds) { job.cancelAndJoin() }

    private suspend fun restart() {
        job.cancelAndJoin()
        job = SupervisorJob()
        dataStoreDelegate = lazy { createStore() }
    }

    private fun config() = File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME).readText()

    @Test fun realPreferencesPersistOutsideAccountNamespaces() =
        runTest(timeout = 60.seconds) {
            preferences.setLanguage(LanguageChoice.Nl)
            val id = DeviceIdentityStore(preferences).identity()
            restart()
            assertEquals(id, DeviceIdentityStore(preferences).identity())
            assertEquals(id, preferences.readDeviceID())
            assertEquals(LanguageChoice.Nl, preferences.language.first())
            assertEquals(id, dataStore.data.first()[stringPreferencesKey("api.deviceId")])
            dataStore.edit { it[intPreferencesKey("api.deviceId")] = 42 }
            try {
                DeviceIdentityStore(preferences).identity()
                fail()
            } catch (
                _: DeviceIdentityFailure.InvalidPersistedIdentity,
            ) {
            }
        }

    @Test fun unknownStoredValuesReadAsSystem() =
        runTest(timeout = 60.seconds) {
            dataStore.edit {
                it[stringPreferencesKey("cf.settings.language")] = "future"
                it[stringPreferencesKey("cf.settings.theme")] = "future"
            }
            assertEquals(LanguageChoice.System, preferences.language.first())
            assertEquals(ThemeChoice.System, preferences.theme.first())
            assertFalse(preferences.notificationPermissionRequested.first())
            preferences.setNotificationPermissionRequested(true)
            assertTrue(preferences.notificationPermissionRequested.first())
        }

    @Test fun languageAndThemeMirrorToDeviceConfigOnlyBelowTheirThresholds() =
        runTest(timeout = 60.seconds) {
            preferences.setLanguage(LanguageChoice.Nl, 32)
            assertEquals("language=nl\ntheme=system\n", config())
            preferences.setTheme(ThemeChoice.Dark, 30)
            assertEquals("language=nl\ntheme=dark\n", config())
            preferences.setLanguage(LanguageChoice.En, 33)
            preferences.setTheme(ThemeChoice.Light, 31)
            assertEquals("language=nl\ntheme=dark\n", config())
            assertEquals(LanguageChoice.En, preferences.language.first())
            assertEquals(ThemeChoice.Light, preferences.theme.first())
        }

    @Test fun reconcileRewritesDeviceConfigFromDataStore() =
        runTest(timeout = 60.seconds) {
            preferences.setLanguage(LanguageChoice.Nl, 33)
            preferences.setTheme(ThemeChoice.Dark, 31)
            DeviceConfigFile.write(context, DeviceConfig(LanguageChoice.En, ThemeChoice.Light), Dispatchers.IO)
            preferences.reconcileDeviceConfig(32)
            assertEquals("language=nl\ntheme=light\n", config())
            preferences.reconcileDeviceConfig(30)
            assertEquals("language=nl\ntheme=dark\n", config())
        }

    @Test fun corruptFileResetsToDefaults() =
        runTest(timeout = 60.seconds) {
            DevicePreferences
                .file(
                    context.noBackupFilesDir,
                ).writeBytes(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
            assertEquals(LanguageChoice.System, preferences.language.first())
            assertEquals(ThemeChoice.System, preferences.theme.first())
            assertFalse(preferences.notificationPermissionRequested.first())
            preferences.setLanguage(LanguageChoice.Nl)
            assertEquals(LanguageChoice.Nl, preferences.language.first())
        }

    @Test fun sharedIsOnePerApplication() {
        assertSame(DevicePreferences.shared(context), DevicePreferences.shared(ContextWrapper(context)))
    }
}
