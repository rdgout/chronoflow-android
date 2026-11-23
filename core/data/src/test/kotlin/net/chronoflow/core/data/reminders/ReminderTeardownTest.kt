package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import net.chronoflow.core.testing.reminders.reminderPlan
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderTeardownTest {
    @Test fun signOutLeavesNoAlarmOrNotification() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<TestReminderApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val root = TestRoots.temporaryStoreRoot()
            val database = AccountDatabase(root, driverFactory = AndroidAccountDriverFactory(app), io = dispatcher)
            database.open(reminderAccount)
            val alarms = AndroidReminderAlarms(app, dispatcher)
            val notices = AndroidReminderNotices(app, dispatcher) { channelName(app, it) }
            val service = reminderService(alarms, notices)
            val runtime = FakeReminderRuntime(app, dispatcher, backgroundScope)
            app.reminderRuntime = runtime
            try {
                val plan = reminderPlan()
                TripRepository(database).upsert(
                    TripRecord(
                        plan.card.id,
                        ApiJson.response.encodeToString(TripCardView.serializer(), plan.card),
                        currentInputHash = "hash",
                        keepOffline = true,
                        departureUtc = plan.card.departureUtc,
                    ),
                )
                TripRepository(database).store(
                    PlanVersionRecord(
                        plan.card.id,
                        "hash",
                        ApiJson.response.encodeToString(TripDetail.serializer(), plan.detail).encodeToByteArray(),
                        reminderNow.toString(),
                    ),
                    emptyList(),
                )
                val settings = """{"light":true,"avoid":true,"caffeine":true,"winddown":true,"leadMin":30}"""
                SettingsRepository(database).upsert(SettingsRecord("reminders", settings))
                service.attach(database)
                service.reschedule()
                val alarmManager = checkNotNull(app.getSystemService(AlarmManager::class.java))
                val notificationManager = checkNotNull(app.getSystemService(NotificationManager::class.java))
                assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
                val row = ReminderStore.rows(database).single()
                runtime.clock = { row.sendAt.plusSeconds(1) }
                shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
                val intent = alarms.alarmIntent(row.toRequest(), row.requestCode)
                ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
                assertEquals(1, notificationManager.activeNotifications.size)
                service.detach()
                assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
                assertTrue(notificationManager.activeNotifications.isEmpty())
                assertTrue(ReminderStore.rows(database).isEmpty())
                assertTrue(ReminderStore.ledger(database).isEmpty())
            } finally {
                withContext(NonCancellable) {
                    service.close()
                    database.close()
                    check(root.deleteRecursively())
                }
            }
        }
}
