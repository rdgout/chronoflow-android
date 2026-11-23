package net.chronoflow.core.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.database.testing.accountB
import net.chronoflow.core.database.testing.exec
import net.chronoflow.core.database.testing.long
import net.chronoflow.core.database.testing.string
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.StorageNamespace
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal val brokenSchema =
    object : SqlSchema<QueryResult.Value<Unit>> {
        override val version = 1L

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(null, "CREATE TABLE partial (id TEXT)", 0)
            throw StoreFailure.Migration()
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ) = QueryResult.Value(Unit)
    }

class AccountDatabaseTests {
    private val root = TestRoots.temporaryStoreRoot()

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun walForeignKeysIsolationAndBackupExclusion() =
        runTest(timeout = 60.seconds) {
            for (namespace in StorageNamespace.entries) {
                for (id in listOf(accountA, accountB)) {
                    val db = AccountDatabase(root, namespace, JdbcAccountDriverFactory(), Dispatchers.IO)
                    try {
                        db.open(id.uppercase())
                        val directory = db.directory(AccountID.of(id))
                        assertTrue(directory.canonicalPath.startsWith(root.canonicalPath + "/"))
                        assertTrue(directory.path.endsWith("${namespace.directoryName}/Accounts/$id"))
                        assertEquals("wal", db.read { it.string("PRAGMA journal_mode") })
                        coroutineScope {
                            List(6) {
                                async(Dispatchers.IO) {
                                    assertEquals(1L, db.read { it.long("PRAGMA foreign_keys") })
                                }
                            }.awaitAll()
                        }
                        assertEquals(1L, db.write { it.long("PRAGMA foreign_keys") })
                        assertEquals(0L, db.read { it.long("SELECT count(*) FROM account") })
                        db.write { it.exec("INSERT INTO account (id,name,email) " + "VALUES ('$id','Name','email')") }
                        db.open(id)
                        try {
                            db.open(
                                if (id ==
                                    accountA
                                ) {
                                    accountB
                                } else {
                                    accountA
                                },
                            )
                            fail()
                        } catch (_: StoreFailure.AlreadyOpen) {
                        }
                        db.close()
                        db.close()
                        assertNull(db.accountID)
                        try {
                            db.read { }
                            fail()
                        } catch (_: StoreFailure.Closed) {
                        }
                        try {
                            db.open(
                                if (id ==
                                    accountA
                                ) {
                                    accountB
                                } else {
                                    accountA
                                },
                            )
                            fail()
                        } catch (_: StoreFailure.AlreadyOpen) {
                        }
                    } finally {
                        db.close()
                    }
                }
            }
        }

    @Test fun failedMigrationRollsBackWithoutDeletingExistingFile() =
        runTest(timeout = 60.seconds) {
            val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(brokenSchema), io = Dispatchers.IO)
            val file = db.file(AccountID.of(accountA))
            file.parentFile!!.mkdirs()
            JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").use { driver ->
                driver.execute(null, "CREATE TABLE sentinel (value TEXT)", 0)
                driver.execute(null, "INSERT INTO sentinel VALUES ('preserved')", 0)
            }
            try {
                db.open(accountA)
                fail()
            } catch (_: StoreFailure.Migration) {
            }
            assertTrue(file.isFile)
            JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").use { driver ->
                assertEquals("preserved", driver.rows("SELECT value FROM sentinel", 1).single().single())
                assertEquals(
                    "0",
                    driver.rows("SELECT count(*) FROM sqlite_master WHERE name='partial'", 1).single().single(),
                )
                assertEquals("0", driver.rows("PRAGMA user_version", 1).single().single())
            }
            val recovered = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)
            recovered.open(accountA)
            recovered.close()
            val corrupt =
                AccountDatabase(File(root, "corrupt"), driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)
            val corruptFile = corrupt.file(AccountID.of(accountA))
            corruptFile.parentFile!!.mkdirs()
            val bytes = "not a sqlite database".encodeToByteArray()
            corruptFile.writeBytes(bytes)
            try {
                corrupt.open(accountA)
                fail()
            } catch (_: StoreFailure.Unavailable) {
            }
            assertArrayEquals(bytes, corruptFile.readBytes())
        }
}
