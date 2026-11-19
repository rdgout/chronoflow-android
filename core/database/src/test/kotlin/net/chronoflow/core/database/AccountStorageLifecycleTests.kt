package net.chronoflow.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.database.testing.exec
import net.chronoflow.core.database.testing.long
import net.chronoflow.core.database.testing.string
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
class AccountStorageLifecycleTests {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val root = context.noBackupFilesDir
    private val db = AccountDatabase(root, driverFactory = AndroidAccountDriverFactory(context), io = Dispatchers.IO)

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun directoryDatabaseAndSidecarsRetainProtectionAndBackupExclusionAfterReopen() =
        runTest(timeout = 60.seconds) {
            repeat(2) { pass ->
                db.open(accountA)
                try {
                    if (pass ==
                        0
                    ) {
                        db.write {
                            it.exec(
                                "INSERT INTO account (id,name,email) VALUES ('$accountA','Test','test@example.test')",
                            )
                        }
                    } else {
                        assertEquals("Test", db.read { it.string("SELECT name FROM account") })
                        db.write { it.exec("UPDATE account SET name='Updated'") }
                    }
                    val file = db.file(AccountID.of(accountA))
                    listOf(
                        db.directory(AccountID.of(accountA)),
                        file,
                        File(file.path + "-wal"),
                        File(file.path + "-shm"),
                    ).forEach {
                        assertTrue(it.path, it.exists())
                        assertTrue(it.canonicalPath.startsWith(root.canonicalPath + "/"))
                    }
                    assertEquals(1L, db.read { it.long("PRAGMA foreign_keys") })
                    assertEquals("wal", db.read { it.string("PRAGMA journal_mode") })
                } finally {
                    db.close()
                }
            }
        }

    @Test fun everyPooledConnectionHasForeignKeys() =
        runTest(timeout = 60.seconds) {
            db.open(accountA)
            try {
                coroutineScope {
                    List(6) {
                        async(Dispatchers.IO) {
                            assertEquals(1L, db.read { it.long("PRAGMA foreign_keys") })
                        }
                    }.awaitAll()
                }
            } finally {
                db.close()
            }
        }

    @Test fun corruptFileIsKeptAndReportedUnavailable() =
        runTest(timeout = 60.seconds) {
            val file = db.file(AccountID.of(accountA))
            file.parentFile!!.mkdirs()
            val bytes = "not a sqlite database".encodeToByteArray()
            file.writeBytes(bytes)
            try {
                db.open(accountA)
                fail()
            } catch (_: StoreFailure.Unavailable) {
            }
            assertArrayEquals(bytes, file.readBytes())
        }

    @Test fun failedCreationLeavesNoPartialTable() =
        runTest(timeout = 60.seconds) {
            val broken =
                AccountDatabase(
                    root,
                    driverFactory = AndroidAccountDriverFactory(context, brokenSchema),
                    io = Dispatchers.IO,
                )
            try {
                broken.open(accountA)
                fail()
            } catch (_: StoreFailure.Migration) {
            }
            android.database.sqlite.SQLiteDatabase
                .openDatabase(
                    broken.file(AccountID.of(accountA)).absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                ).use { database ->
                    database.rawQuery("SELECT count(*) FROM sqlite_master WHERE name='partial'", null).use {
                        assertTrue(it.moveToFirst())
                        assertEquals(0L, it.getLong(0))
                    }
                    database.rawQuery("PRAGMA user_version", null).use {
                        assertTrue(it.moveToFirst())
                        assertEquals(0L, it.getLong(0))
                    }
                }
        }

    @Test fun frameworkSqliteIsAtLeastTheApi29Floor() =
        runTest(timeout = 60.seconds) {
            db.open(accountA)
            try {
                val version = db.read { it.string("SELECT sqlite_version()") }!!
                val numbers = version.split('.').map(String::toInt)
                assertTrue(version, numbers[0] > 3 || (numbers[0] == 3 && numbers[1] >= 22))
                File(requireNotNull(System.getProperty("user.dir")), "build/sqlite-version.txt").writeText(version)
            } finally {
                db.close()
            }
        }
}
