package net.chronoflow.core.data.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@Suppress("DEPRECATION") // Robolectric exposes the pending intent only through this field.
class AndroidReminderAlarmsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = checkNotNull(context.getSystemService(AlarmManager::class.java))

    @Test fun setUsesRtcWakeupAllowWhileIdleAtTheInstant() =
        runTest(timeout = 60.seconds) {
            val alarms = AndroidReminderAlarms(context, StandardTestDispatcher(testScheduler))
            val request = adapterRequest()
            alarms.set(request, 7)
            val alarm = shadowOf(manager).scheduledAlarms.single()
            assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
            assertEquals(request.sendAt.toEpochMilli(), alarm.triggerAtMs)
            assertTrue(alarm.isAllowWhileIdle)
            val pending = shadowOf(alarm.operation)
            assertTrue(pending.isBroadcast)
            val intent = pending.savedIntent
            assertEquals("net.chronoflow.core.data.reminders.ReminderAlarmReceiver", intent.component?.className)
            assertEquals(ACTION_ALARM, intent.action)
            assertEquals(request.identifier, intent.identifier)
            assertEquals(request.identifier, intent.getStringExtra(EXTRA_IDENTIFIER))
            assertEquals(7, intent.getIntExtra(EXTRA_REQUEST_CODE, -1))
            assertEquals(request.title, intent.getStringExtra(EXTRA_TITLE))
            assertEquals(request.body, intent.getStringExtra(EXTRA_BODY))
            assertEquals(request.tripID, intent.getStringExtra(EXTRA_TRIP_ID))
            assertEquals(request.blockID, intent.getStringExtra(EXTRA_BLOCK_ID))
            assertEquals(request.inputHash, intent.getStringExtra(EXTRA_INPUT_HASH))
            assertEquals(request.accountID, intent.getStringExtra(EXTRA_ACCOUNT_ID))
            assertEquals(request.expiresAt.toEpochMilli(), intent.getLongExtra(EXTRA_EXPIRES_AT, -1))
            assertTrue(checkNotNull(alarm.operation).isImmutable)
        }

    @Test fun exactBelowApi31() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            ShadowAlarmManager.setCanScheduleExactAlarms(true)
            AndroidReminderAlarms(context, dispatcher, sdkInt = 30).set(adapterRequest(), 7)
            val exact = shadowOf(manager).scheduledAlarms.single()
            assertEquals(ShadowAlarmManager.WINDOW_EXACT, exact.windowLengthMs)
            assertTrue(exact.isAllowWhileIdle)
            assertEquals(AlarmManager.RTC_WAKEUP, exact.type)
            assertEquals(adapterRequest().sendAt.toEpochMilli(), exact.triggerAtMs)
            AndroidReminderAlarms(context, dispatcher).set(adapterRequest(), 7)
            assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, shadowOf(manager).scheduledAlarms.single().windowLengthMs)
        }

    @Test fun absoluteTriggersSurviveDSTAndDeviceZoneChanges() =
        runTest(timeout = 60.seconds) {
            val alarms = AndroidReminderAlarms(context, StandardTestDispatcher(testScheduler))

            suspend fun assertInstant(value: String) {
                val instant = Instant.parse(value)
                val request = adapterRequest().copy(sendAt = instant)
                val original = TimeZone.getDefault()
                try {
                    TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
                    alarms.set(request, 7)
                    assertEquals(instant.toEpochMilli(), shadowOf(manager).scheduledAlarms.single().triggerAtMs)
                    TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
                    alarms.set(request, 7)
                    assertEquals(instant.toEpochMilli(), shadowOf(manager).scheduledAlarms.single().triggerAtMs)
                } finally {
                    TimeZone.setDefault(original)
                }
            }
            assertInstant("2027-03-28T01:30:00Z")
            assertInstant("2027-10-31T01:30:00Z")
            assertInstant("2027-11-07T06:30:00Z")
        }

    @Test fun sameIdentifierReplacesAndCancelRemoves() =
        runTest(timeout = 60.seconds) {
            val alarms = AndroidReminderAlarms(context, StandardTestDispatcher(testScheduler))
            val request = adapterRequest()
            alarms.set(request, 7)
            alarms.set(request.copy(body = "Updated"), 7)
            assertEquals(
                "Updated",
                shadowOf(shadowOf(manager).scheduledAlarms.single().operation).savedIntent.getStringExtra(EXTRA_BODY),
            )
            alarms.set(request.copy(identifier = "other"), 8)
            assertEquals(2, shadowOf(manager).scheduledAlarms.size)
            alarms.cancel(request.identifier, 7)
            assertEquals(1, shadowOf(manager).scheduledAlarms.size)
            alarms.cancel("unknown", 99)
            assertEquals(1, shadowOf(manager).scheduledAlarms.size)
        }

    @Test fun refusalIsWrapped() {
        val state = IllegalStateException()
        try {
            refusing { throw state }
            fail()
        } catch (error: ReminderAlarmRefused) {
            assertSame(state, error.cause)
        }
        val security = SecurityException()
        try {
            refusing { throw security }
            fail()
        } catch (
            error: ReminderAlarmRefused,
        ) {
            assertSame(security, error.cause)
        }
    }
}
