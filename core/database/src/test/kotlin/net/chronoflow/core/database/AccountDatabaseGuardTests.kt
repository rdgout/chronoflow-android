package net.chronoflow.core.database

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.database.testing.accountB
import net.chronoflow.core.database.testing.long
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.StoreFailure
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class AccountDatabaseGuardTests {
    private val root = TestRoots.temporaryStoreRoot()
    private val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)
    private val epoch = SyncEpoch(AccountID.of(accountA), 1)

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

    private fun insert(sql: AccountSql) {
        sql.queries.outboxQueries.insertOutboxRow("row", "future", "{}", "now", 0, null)
    }

    private suspend fun refused(body: suspend () -> Unit) {
        try {
            body()
            fail()
        } catch (_: StaleSessionException) {
        }
        assertEquals(0L, db.read { it.long("SELECT count(*) FROM outbox") })
    }

    @Test fun writeAfterItsEpochEndedIsRefused() =
        runTest(timeout = 60.seconds) {
            opened {
                epoch.invalidate()
                refused { db.write(epoch) { insert(it) } }
            }
        }

    @Test fun childEpochOfAnEndedParentIsRefused() =
        runTest(timeout = 60.seconds) {
            opened {
                val child = epoch.child()
                epoch.invalidate()
                refused { db.write(child) { insert(it) } }
            }
        }

    @Test fun epochForAnotherAccountIsRejected() =
        runTest(timeout = 60.seconds) {
            opened {
                val other = SyncEpoch(AccountID.of(accountB), 1)
                try {
                    db.write(other) { insert(it) }
                    fail()
                } catch (_: StoreFailure.InvalidAccountID) {
                }
                assertEquals(0L, db.read { it.long("SELECT count(*) FROM outbox") })
            }
        }

    @Test fun invalidPermitRefusesAndLeavesNoRow() =
        runTest(timeout = 60.seconds) {
            opened {
                val permit = SessionMutationPermit().apply { invalidate() }
                refused { db.write(permit) { insert(it) } }
            }
        }

    @Test fun invalidPermitDoesNotEndTheEpoch() =
        runTest(timeout = 60.seconds) {
            opened {
                refused { db.write(epoch, SessionMutationPermit().apply { invalidate() }) { insert(it) } }
                assertTrue(epoch.isValid)
            }
        }

    @Test fun invalidationWaitsForTheAdmittedCommit() =
        runTest(timeout = 60.seconds) {
            opened {
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val write =
                    async(Dispatchers.IO) {
                        db.write(epoch) {
                            insert(it)
                            entered.countDown()
                            check(release.await(1, TimeUnit.SECONDS))
                        }
                    }
                var invalidator: Thread? = null
                try {
                    assertTrue(entered.await(1, TimeUnit.SECONDS))
                    invalidator = Thread { epoch.invalidate() }.apply { start() }
                    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
                    while (
                        invalidator.state != Thread.State.WAITING &&
                        invalidator.isAlive && System.nanoTime() < deadline
                    ) {
                        Thread.onSpinWait()
                    }
                    assertEquals(Thread.State.WAITING, invalidator.state)
                    release.countDown()
                    write.await()
                    invalidator.join(1000)
                    assertFalse(invalidator.isAlive)
                    assertEquals(1L, db.read { it.long("SELECT count(*) FROM outbox") })
                    try {
                        db.write(epoch) { }
                        fail()
                    } catch (_: StaleSessionException) {
                    }
                } finally {
                    release.countDown()
                    invalidator?.join(1000)
                }
            }
        }

    @Test fun cancelledEpochWriteInvalidatesTheEpoch() =
        runTest(timeout = 60.seconds) {
            opened {
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val job =
                    launch(Dispatchers.IO) {
                        db.write(epoch) {
                            entered.countDown()
                            check(release.await(1, TimeUnit.SECONDS))
                        }
                    }
                try {
                    assertTrue(entered.await(1, TimeUnit.SECONDS))
                    job.cancel()
                } finally {
                    release.countDown()
                }
                job.join()
                assertFalse(epoch.isValid)
            }
        }

    @Test fun writesAreSerial() =
        runTest(timeout = 60.seconds) {
            opened {
                db.write { it.queries.settingsQueries.insertSetting("counter", "0") }
                val active = AtomicInteger()
                val maximum = AtomicInteger()
                List(20) {
                    async(Dispatchers.IO) {
                        db.write { sql ->
                            maximum.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                            try {
                                val value =
                                    sql.queries.settingsQueries
                                        .selectSetting("counter")
                                        .executeAsOne()
                                        .value_
                                        .toInt()
                                sql.queries.settingsQueries.updateSetting((value + 1).toString(), "counter")
                            } finally {
                                active.decrementAndGet()
                            }
                        }
                    }
                }.awaitAll()
                assertEquals(1, maximum.get())
                assertEquals(
                    "20",
                    db.read {
                        it.queries.settingsQueries
                            .selectSetting("counter")
                            .executeAsOne()
                            .value_
                    },
                )
            }
        }

    @Test fun rollbackLeavesNoRow() =
        runTest(timeout = 60.seconds) {
            opened {
                try {
                    db.write {
                        insert(it)
                        error("rollback")
                    }
                    fail()
                } catch (_: IllegalStateException) {
                }
                assertEquals(0L, db.read { it.long("SELECT count(*) FROM outbox") })
            }
        }
}
