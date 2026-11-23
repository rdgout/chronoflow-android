@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.reminders

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun TestScope.initializeReminderWork(): Pair<TestReminderApplication, FakeReminderRuntime> {
    val app = ApplicationProvider.getApplicationContext<TestReminderApplication>()
    val runtime = FakeReminderRuntime(app, StandardTestDispatcher(testScheduler), backgroundScope)
    app.reminderRuntime = runtime
    WorkManagerTestInitHelper.initializeTestWorkManager(
        app,
        Configuration
            .Builder()
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .setWorkerCoroutineContext(UnconfinedTestDispatcher(testScheduler))
            .build(),
    )
    return app to runtime
}

fun closeReminderWork(app: TestReminderApplication) {
    WorkManager
        .getInstance(app)
        .cancelAllWork()
        .result
        .get()
    WorkManagerTestInitHelper.closeWorkDatabase()
}
