package net.chronoflow.core.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.HandDrainedDispatcher
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SyncEpoch
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class AccountDatabaseCallerThreadTests {
    private val root = TestRoots.temporaryStoreRoot()

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun observeReturnsWhileCloseHoldsTheLifecycleWriteLock() =
        runTest(timeout = 60.seconds) {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val factory =
                AccountDriverFactory { file ->
                    val driver = JdbcAccountDriverFactory().open(file)
                    object : SqlDriver by driver {
                        override fun close() {
                            entered.countDown()
                            check(release.await(1, TimeUnit.SECONDS))
                            driver.close()
                        }
                    }
                }
            val store = AccountDatabase(root, driverFactory = factory, io = Dispatchers.IO)
            store.open(accountA)
            val caller = Executors.newSingleThreadExecutor()
            val closing = backgroundScope.async(Dispatchers.IO) { store.close() }
            try {
                assertTrue(entered.await(1, TimeUnit.SECONDS))
                val returned = caller.submit<Flow<Unit>> { store.observe("account") { Unit } }
                assertNotNull(returned.get(250, TimeUnit.MILLISECONDS))
                assertFalse(closing.isCompleted)
            } finally {
                release.countDown()
                closing.await()
                caller.shutdownNow()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validateDispatchesItsLifecycleCheckToIo() =
        runTest(timeout = 60.seconds) {
            val io = HandDrainedDispatcher()
            val store = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)

            suspend fun drainIo(operation: suspend () -> Unit) {
                val pending = async { operation() }
                runCurrent()
                io.runAll()
                runCurrent()
                pending.await()
            }
            drainIo { store.open(accountA) }
            try {
                val validation =
                    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        store.validate(SyncEpoch(AccountID.of(accountA), 1))
                    }
                assertFalse(validation.isCompleted)
                io.runAll()
                runCurrent()
                validation.join()
            } finally {
                drainIo { store.close() }
            }
        }
}
