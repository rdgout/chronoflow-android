package net.chronoflow.core.data.reminders

import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.reminders.reminderAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class FirstTripRemindersOfferTests(
    private val permission: ReminderPermission,
    private val enabled: Boolean,
) {
    @Test fun permissionAndEnabledKindsGateTheOneTimeOffer() =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            try {
                store.settings(ReminderSettings(false, false, enabled, false))
                val offer = FirstTripRemindersOffer(store.database)
                assertEquals(false, offer.consume(permission))
                offer.queue(false)
                assertEquals(false, offer.consume(permission))
                offer.queue(true)
                assertEquals(permission == ReminderPermission.Undetermined && enabled, offer.consume(permission))
                offer.queue(true)
                assertEquals(false, offer.consume(ReminderPermission.Undetermined))
            } finally {
                store.close()
            }
        }

    @Test fun missingSettingsWaitWithoutConsumingTheOffer() =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            try {
                SettingsRepository(store.database).remove("reminders")
                val offer = FirstTripRemindersOffer(store.database)
                offer.queue(true)
                assertNull(offer.consume(ReminderPermission.Undetermined))
                store.settings()
                assertEquals(true, offer.consume(ReminderPermission.Undetermined))
            } finally {
                store.close()
            }
        }

    @Test fun pendingAndSeenSurviveReopeningAndDuplicateSaveCompletion() =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            try {
                FirstTripRemindersOffer(store.database).queue(true)
                store.database.close()
                store.database.open(reminderAccount)
                val recovered = FirstTripRemindersOffer(store.database)
                val first = async { recovered.consume(ReminderPermission.Undetermined) }
                val duplicate = async { recovered.consume(ReminderPermission.Undetermined) }
                assertEquals(1, listOf(first.await(), duplicate.await()).count { it == true })
                store.database.close()
                store.database.open(reminderAccount)
                val shown = FirstTripRemindersOffer(store.database)
                shown.queue(true)
                assertEquals(false, shown.consume(ReminderPermission.Undetermined))
            } finally {
                store.close()
            }
        }

    @Test fun anotherAccountDoesNotConsumeThePendingOffer() =
        runTest(timeout = 60.seconds) {
            val first = ReminderTestStore(StandardTestDispatcher(testScheduler))
            val other = ReminderTestStore(StandardTestDispatcher(testScheduler), "01900000-0000-7000-8000-000000000002")
            try {
                FirstTripRemindersOffer(first.database).queue(true)
                assertEquals(false, FirstTripRemindersOffer(other.database).consume(ReminderPermission.Undetermined))
                assertEquals(true, FirstTripRemindersOffer(first.database).consume(ReminderPermission.Undetermined))
            } finally {
                first.close()
                other.close()
            }
        }

    @Test fun seenIsSavedWithoutReplace() =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            try {
                val before = store.database.read { it.fetchSetting("reminders") }
                val offer = FirstTripRemindersOffer(store.database)
                offer.queue(true)
                assertEquals(true, offer.consume(ReminderPermission.Undetermined))
                assertEquals("\"seen\"", store.database.read { it.fetchSetting("wizardReminders")?.value })
                assertEquals(before, store.database.read { it.fetchSetting("reminders") })
            } finally {
                store.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "permission={0}-enabled={1}")
        fun cases(): List<Array<Any>> =
            ReminderPermission.entries.flatMap { p ->
                listOf(arrayOf(p, true), arrayOf(p, false))
            }
    }
}
