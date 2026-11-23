package net.chronoflow.app.store

import android.os.StrictMode
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.DeviceIdentityStore
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Smoke
class StoreThreadingSmokeTest {
    @Test fun storeCallsFromMainDoNoMainThreadDiskIo() =
        runTest(timeout = 60.seconds) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val suffix = UUID.randomUUID().toString()
            val root = File(context.noBackupFilesDir, "store-smoke-$suffix")
            val alias = "test.$suffix.session"
            val accountA = "019a562b-8000-7000-8000-000000000001"
            val db = AccountDatabase(root, StorageNamespace.Mock, AndroidAccountDriverFactory(context), Dispatchers.IO)
            val cipher = AndroidKeystoreSessionCipher()
            val session = KeystoreSessionStore(File(root, "session.bin"), alias, cipher, Dispatchers.IO)
            val storeJob = SupervisorJob()
            val store =
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + storeJob),
                    produceFile = { DevicePreferences.file(root) },
                )
            val preferences = DevicePreferences(context, store, Dispatchers.IO)
            val identity = DeviceIdentityStore(preferences)
            val marker = WipeMarker(File(root, StorageNamespace.Mock.directoryName), Dispatchers.IO)
            try {
                val version =
                    withContext(Dispatchers.Main) {
                        val old = StrictMode.getThreadPolicy()
                        StrictMode.setThreadPolicy(
                            StrictMode.ThreadPolicy
                                .Builder()
                                .detectDiskReads()
                                .detectDiskWrites()
                                .penaltyDeath()
                                .build(),
                        )
                        try {
                            db.open(accountA)
                            db.write { it.saveAccount(AccountRecord(accountA, "Test", "test@example.test")) }
                            check(db.read { it.fetchAccount(accountA) } != null)
                            check(db.observe("account") { it.fetchAccount(accountA) }.first() != null)
                            session.write(StoredSession("test", accountA).encode())
                            session.read()
                            session.delete()
                            preferences.setTheme(ThemeChoice.Dark)
                            preferences.language.first()
                            identity.identity()
                            marker.write(AccountID.of(accountA))
                            marker.read()
                            marker.clear()
                            db.read { sql ->
                                sql.driver
                                    .executeQuery(null, "SELECT sqlite_version()", { cursor ->
                                        check(cursor.next().value)
                                        app.cash.sqldelight.db.QueryResult
                                            .Value(requireNotNull(cursor.getString(0)))
                                    }, 0)
                                    .value
                            }
                        } finally {
                            StrictMode.setThreadPolicy(old)
                        }
                    }
                Log.i("StoreThreadingSmoke", "sqlite_version=$version")
            } finally {
                withContext(NonCancellable + Dispatchers.IO) {
                    db.close()
                    storeJob.cancelAndJoin()
                    cipher.deleteKey(alias)
                    root.deleteRecursively()
                }
            }
        }
}
