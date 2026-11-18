package net.chronoflow.core.database

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class ObservationTests {
    private val root = TestRoots.temporaryStoreRoot()
    private val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)

    @After fun cleanup() {
        root.deleteRecursively()
    }

    private suspend fun opened(body: suspend () -> Unit) {
        db.open(accountA)
        try {
            body()
        } finally {
            db.close()
        }
    }

    @Test fun fetchRunsOnTheInjectedDispatcher() =
        runTest(timeout = 60.seconds) {
            Executors.newSingleThreadExecutor { Thread(it, "store-io") }.asCoroutineDispatcher().use { io ->
                val store = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)
                store.open(accountA)
                try {
                    store.observe("account") { Thread.currentThread().name.substringBefore(" @coroutine") }.test {
                        assertEquals("store-io", awaitItem())
                        cancelAndIgnoreRemainingEvents()
                    }
                } finally {
                    store.close()
                }
            }
        }

    @Test fun initialValueThenOnlyCommittedValues() =
        runTest(timeout = 60.seconds) {
            opened {
                db
                    .observe("settings") {
                        it.queries.settingsQueries.selectSettings().executeAsList().map { row ->
                            row.value_
                        }
                    }.test {
                        assertEquals(emptyList<String>(), awaitItem())
                        db.write {
                            it.queries.settingsQueries.insertSetting("value", "half")
                            it.queries.settingsQueries.updateSetting("final", "value")
                        }
                        assertEquals(listOf("final"), awaitItem())
                        try {
                            db.write {
                                it.queries.settingsQueries.updateSetting("rolled-back", "value")
                                error("rollback")
                            }
                        } catch (
                            _: IllegalStateException,
                        ) {
                        }
                        expectNoEvents()
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    @Test fun nullValuesAreEmitted() =
        runTest(timeout = 60.seconds) {
            opened {
                db
                    .observe("account") {
                        it.queries.accountQueries
                            .selectAccount(accountA)
                            .executeAsOneOrNull()
                    }.test {
                        assertNull(awaitItem())
                        db.write { it.queries.accountQueries.insertAccount(accountA, "name", "email", 1, null, 0) }
                        assertEquals(accountA, awaitItem()!!.id)
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    @Test fun closeCompletesTheFlowAndClearsObservers() =
        runTest(timeout = 60.seconds) {
            opened {
                db
                    .observe("account") {
                        it.queries.accountQueries
                            .selectAccount(accountA)
                            .executeAsOneOrNull()
                    }.test {
                        awaitItem()
                        assertEquals(1, db.observationCount)
                        db.close()
                        awaitComplete()
                        assertEquals(0, db.observationCount)
                    }
            }
        }

    @Test fun closeCompletesTwoUnconfinedObserversAndClearsTheDatabase() =
        runTest(timeout = 60.seconds) {
            opened {
                db.observe("account") { Unit }.test {
                    awaitItem()
                    db.observe("settings") { Unit }.test {
                        awaitItem()
                        assertEquals(2, db.observationCount)
                        db.close()
                        awaitComplete()
                        assertNull(db.accountID)
                    }
                    awaitComplete()
                    assertEquals(0, db.observationCount)
                }
            }
        }

    @Test fun observeOnAClosedDatabaseThrowsClosed() {
        assertThrows(StoreFailure.Closed::class.java) { db.observe("account") { Unit } }
    }

    @Test fun cancellingTheCollectorUnregisters() =
        runTest(timeout = 60.seconds) {
            opened {
                db.observe("account") { Unit }.test {
                    awaitItem()
                    assertEquals(1, db.observationCount)
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(0, db.observationCount)
            }
        }
}
