package net.chronoflow.core.data.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderRescheduleWorkerTest {
    @Test fun runsTheBackgroundPass() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                val worker = TestListenableWorkerBuilder<ReminderRescheduleWorker>(app).build()
                assertEquals(ListenableWorker.Result.success(), worker.doWork())
                assertEquals(1, runtime.passes)
            } finally {
                closeReminderWork(app)
            }
        }

    @Test fun storeFailureDoesNotRetry() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                runtime.pass = { throw StoreFailure.Closed }
                assertEquals(
                    ListenableWorker.Result.success(),
                    TestListenableWorkerBuilder<ReminderRescheduleWorker>(app).build().doWork(),
                )
            } finally {
                closeReminderWork(app)
            }
        }

    @Test fun ioFailureDoesNotRetry() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                runtime.pass = { throw IOException() }
                assertEquals(
                    ListenableWorker.Result.success(),
                    TestListenableWorkerBuilder<ReminderRescheduleWorker>(app).build().doWork(),
                )
            } finally {
                closeReminderWork(app)
            }
        }

    @Test fun cancellationIsRethrown() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                runtime.pass = { throw CancellationException() }
                try {
                    TestListenableWorkerBuilder<ReminderRescheduleWorker>(app).build().doWork()
                    fail()
                } catch (
                    _: CancellationException,
                ) {
                }
            } finally {
                closeReminderWork(app)
            }
        }
}
