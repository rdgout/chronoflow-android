@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.onboarding

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class OnboardingLoadTests {
    @Test fun sqliteExceptionRetriesAfter100Milliseconds() = assertFailureRetries { SQLiteException() }

    @Test fun sqliteDiskIOExceptionRetriesAfter100Milliseconds() = assertFailureRetries { SQLiteDiskIOException() }

    @Test fun sqliteDatabaseLockedExceptionRetriesAfter100Milliseconds() =
        assertFailureRetries { SQLiteDatabaseLockedException() }

    @Test fun androidSqlExceptionRetriesAfter100Milliseconds() =
        assertFailureRetries {
            android.database.SQLException()
        }

    @Test fun jdbcSqlExceptionRetriesAfter100Milliseconds() = assertFailureRetries { java.sql.SQLException() }

    @Test fun foreignCancellationRetriesAfter100Milliseconds() =
        assertFailureRetries {
            CancellationException(
                "foreign",
            )
        }

    @Test fun callerCancellationPropagatesWithoutResubscribing() =
        runTest(timeout = 60.seconds) {
            var subscriptions = 0
            var propagated: CancellationException? = null
            val callerCancellation = CancellationException("caller")
            val job =
                backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
                    try {
                        retryOnboardingLoad {
                            flow<Unit> {
                                subscriptions++
                                awaitCancellation()
                            }
                        }.collect {}
                    } catch (failure: CancellationException) {
                        propagated = failure
                        throw failure
                    }
                }
            runCurrent()
            assertEquals(1, subscriptions)
            job.cancel(callerCancellation)
            job.join()
            advanceTimeBy(1_000)
            runCurrent()
            assertTrue(propagated === callerCancellation || propagated?.cause === callerCancellation)
            assertTrue(job.isCancelled)
            assertEquals(1, subscriptions)
        }

    private fun assertFailureRetries(failure: () -> Throwable) =
        runTest(timeout = 60.seconds) {
            var subscriptions = 0
            val observed = mutableListOf<Int>()
            val job =
                backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
                    retryOnboardingLoad {
                        flow {
                            subscriptions++
                            emit(subscriptions)
                            if (subscriptions == 1) throw failure()
                            awaitCancellation()
                        }
                    }.collect { observed += it }
                }
            runCurrent()
            assertEquals(listOf(1), observed)
            advanceTimeBy(99)
            runCurrent()
            assertEquals(1, subscriptions)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, subscriptions)
            assertEquals(listOf(1, 2), observed)
            job.cancelAndJoin()
        }
}
