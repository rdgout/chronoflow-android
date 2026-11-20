package net.chronoflow.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.datastore.SessionCipher
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = android.app.Application::class)
class StoragePathsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val root = context.noBackupFilesDir
    private val accountA = AccountID.of("019a562b-8000-7000-8000-000000000001")
    private val db = AccountDatabase(root, StorageNamespace.Api, AndroidAccountDriverFactory(context), Dispatchers.IO)
    private val cipher =
        object : SessionCipher {
            override fun encrypt(
                alias: String,
                plaintext: ByteArray,
            ): SessionCipher.Sealed = error("unused")

            override fun decrypt(
                alias: String,
                sealed: SessionCipher.Sealed,
            ): ByteArray = error("unused")

            override fun deleteKey(alias: String) = Unit
        }

    @Test fun everyStoreFileLivesUnderNoBackupFilesDir() {
        val files =
            listOf(
                db.file(accountA),
                KeystoreSessionStore.forNamespace(root, StorageNamespace.Api, cipher, Dispatchers.IO).file,
                WipeMarker(File(root, StorageNamespace.Api.directoryName), Dispatchers.IO).file,
                DevicePreferences.file(root),
                File(root, DeviceConfigFile.FILE_NAME),
            )
        files.forEach { assertTrue(it.path, it.canonicalPath.startsWith(root.canonicalPath + "/")) }
    }

    @Test fun accountPathsUseTheValidatedId() {
        val expected = File(root, "ChronoFlow/Accounts/${accountA.rawValue}/account-${accountA.rawValue}.db")
        assertEquals(expected, db.file(accountA))
        assertEquals(expected, db.file(AccountID.of(accountA.rawValue.uppercase())))
    }
}
