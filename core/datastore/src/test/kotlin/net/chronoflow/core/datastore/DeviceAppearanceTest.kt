package net.chronoflow.core.datastore

import android.app.UiModeManager
import android.content.Context
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceAppearanceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var job: Job = SupervisorJob()
    private var dataStore = store()
    private val modes = mutableListOf<Int>()
    private val sink =
        NightModeSink {
            assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            modes += it
        }

    private fun store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { DevicePreferences.file(context.noBackupFilesDir) },
        )

    private fun appearance(sdk: Int = 36) =
        DeviceAppearance(
            DevicePreferences(context, dataStore, Dispatchers.IO),
            sink,
            Dispatchers.IO,
            sdk,
        )

    @After fun close() =
        runTest(timeout = 60.seconds) {
            withContext(NonCancellable) { job.cancelAndJoin() }
        }

    @Test fun sdk36WritesDataStoreAndMirrorsNightMode() =
        runTest(timeout = 60.seconds) {
            val appearance = appearance()
            appearance.setTheme(ThemeChoice.Dark)
            assertEquals(ThemeChoice.Dark, appearance.theme.first())
            appearance.setTheme(ThemeChoice.Light)
            appearance.setTheme(ThemeChoice.System)
            assertEquals(
                listOf(UiModeManager.MODE_NIGHT_YES, UiModeManager.MODE_NIGHT_NO, UiModeManager.MODE_NIGHT_AUTO),
                modes,
            )
        }

    @Test fun sdk30WritesDeviceConfigAndNeverCallsTheSink() =
        runTest(timeout = 60.seconds) {
            appearance(30).setTheme(ThemeChoice.Dark)
            assertEquals(
                "language=system\ntheme=dark\n",
                File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME).readText(),
            )
            assertTrue(modes.isEmpty())
        }

    @Test fun unknownStoredValueReadsAsSystem() =
        runTest(timeout = 60.seconds) {
            dataStore.edit { it[stringPreferencesKey("cf.settings.theme")] = "future" }
            assertEquals(ThemeChoice.System, appearance().theme.first())
        }

    @Test fun themePersistsAcrossInstances() =
        runTest(timeout = 60.seconds) {
            appearance().setTheme(ThemeChoice.Dark)
            job.cancelAndJoin()
            job = SupervisorJob()
            dataStore = store()
            assertEquals(ThemeChoice.Dark, appearance().theme.first())
        }
}
