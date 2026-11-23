package net.chronoflow.core.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datastore.DeviceIdentityStore
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DevicePreferencesWipeTests {
    @Test fun devicePreferencesSurviveDeletingAllAccountFiles() =
        runTest(timeout = 60.seconds) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = DevicePreferences.shared(context)
            preferences.setLanguage(LanguageChoice.Nl)
            preferences.setTheme(ThemeChoice.Dark)
            val identity = DeviceIdentityStore(preferences).identity()
            val root = withContext(Dispatchers.IO) { context.noBackupFilesDir }
            val database =
                AccountDatabase(
                    root,
                    driverFactory = AndroidAccountDriverFactory(context),
                    io = Dispatchers.IO,
                )
            database.open(syncAccount)
            val directory = database.directory(AccountID.of(syncAccount))
            database.close()
            withContext(Dispatchers.IO) { assertTrue(directory.deleteRecursively()) }
            assertEquals(identity, DeviceIdentityStore(preferences).identity())
            assertEquals(LanguageChoice.Nl, preferences.language.first())
            assertEquals(ThemeChoice.Dark, preferences.theme.first())
        }
}
