@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.reminders

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderLedgerEntry
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.reminders.from
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderBlock
import net.chronoflow.core.testing.reminders.reminderNow
import net.chronoflow.core.testing.reminders.reminderPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ReminderServiceTests(
    private val authorization: ReminderPermission,
) {
    @Test fun schedulingNeverRequestsAuthorization() =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            val alarms = FakeReminderAlarms()
            var reads = 0
            var language: Language = Language.En
            val service =
                reminderService(alarms, permission = {
                    reads++
                    authorization
                }, language = { language })
            try {
                store.store(reminderPlan())
                service.attach(store.database)
                service.reschedule()
                language = Language.Nl
                service.reschedule()
                assertTrue(reads > 0)
                assertEquals(
                    if (authorization ==
                        ReminderPermission.Granted
                    ) {
                        1
                    } else {
                        0
                    },
                    ReminderStore.rows(store.database).size,
                )
                assertFalse(ReminderService::class.java.methods.any { it.name.contains("requestPermission") })
            } finally {
                withContext(NonCancellable) {
                    service.close()
                    store.close()
                }
            }
        }

    @Test fun fakeCenterCapsAt60AndKeepsUnchangedRequests() =
        fixture { f ->
            repeat(11) { index ->
                val blocks = List(6) { reminderBlock((it * 3 + 1).toDouble(), ordinal = it + 1) }
                f.store.store(reminderPlan(blocks, id = "trip-$index"))
            }
            f.start()
            assertEquals(60, ReminderStore.rows(f.store.database).size)
            f.service.reschedule()
            assertEquals(60, f.alarms.sets.size)
            assertEquals(60, ReminderStore.ledger(f.store.database).size)
        }

    @Test fun replaceRemovePermissionAndFailures() =
        fixture { f ->
            val blocks = listOf(reminderBlock(), reminderBlock(2.0, ordinal = 2), reminderBlock(3.0, ordinal = 3))
            f.store.store(reminderPlan(blocks))
            f.alarms.reject(reminderBlock(2.0).id)
            f.start()
            assertEquals(2, f.rows().size)
            f.service.reschedule()
            assertEquals(2, f.alarms.sets.size)
            val removed = f.rows().first { it.blockId == reminderBlock().id }.identifier
            f.store.settings(ReminderSettings(leadMin = 60))
            advanceUntilIdle()
            assertEquals(listOf(reminderNow.plusSeconds(7200)), f.rows().map { it.sendAt })
            assertTrue(removed in f.alarms.cancels)
            f.permission = ReminderPermission.Blocked
            f.service.reschedule()
            assertTrue(f.rows().isEmpty())
            f.permission = ReminderPermission.Granted
            f.service.reschedule()
            assertEquals(1, f.rows().size)
            SettingsRepository(f.store.database).remove("reminders")
            advanceUntilIdle()
            assertTrue(f.rows().isEmpty())
        }

    @Test fun overlappingRequestsRerunWithLatestSettings() =
        fixture { f ->
            f.store.store(reminderPlan(listOf(reminderBlock(2.0), reminderBlock(3.0, ordinal = 2))))
            f.alarms.holdNext()
            f.service.attach(f.store.database)
            runCurrent()
            assertTrue(f.alarms.setting)
            f.store.settings(ReminderSettings(leadMin = 60))
            val first = launch { f.service.reschedule() }
            val second = launch { f.service.reschedule() }
            runCurrent()
            f.alarms.release()
            first.join()
            second.join()
            f.service.reschedule()
            assertEquals(1, f.alarms.maximumConcurrentSets)
            assertEquals(
                listOf(reminderNow.plusSeconds(3600), reminderNow.plusSeconds(7200)),
                f.rows().map { it.sendAt }.sorted(),
            )
        }

    @Test fun serializedRerunAndSignOutDrain() =
        fixture { f ->
            f.store.store(reminderPlan(listOf(reminderBlock(), reminderBlock(2.0, ordinal = 2))))
            f.alarms.holdNext()
            f.service.attach(f.store.database)
            runCurrent()
            assertTrue(f.alarms.setting)
            val rerun = launch { f.service.reschedule() }
            val wipe = launch { f.service.detach() }
            runCurrent()
            f.alarms.release()
            rerun.join()
            wipe.join()
            assertTrue(f.rows().isEmpty())
            assertTrue(ReminderStore.ledger(f.store.database).isEmpty())
            assertTrue(f.alarms.active.isEmpty())
        }

    @Test fun checkOffUndoDisabledAndRelaunchLedger() =
        fixture { f ->
            f.store.store(reminderPlan(listOf(reminderBlock(), reminderBlock(1.1, BlockType.AvoidLight))))
            f.start()
            val request = f.rows().single().toRequest()
            assertEquals(2, request.memberKeys.size)
            val outbox = OutboxRepository(f.store.database)
            outbox.enqueue(
                OutboxChange.CheckOff(
                    CheckOffChange("trip", "hash", BlockType.SeekLight, "2026-10-14", 1, CheckStatus.Done),
                ),
                now = reminderNow,
            )
            advanceUntilIdle()
            assertEquals(
                1,
                f
                    .rows()
                    .single()
                    .memberKeys.size,
            )
            outbox.enqueue(
                OutboxChange.CheckOff(CheckOffChange("trip", "hash", BlockType.SeekLight, "2026-10-14", 1, null)),
                now = reminderNow.plusMillis(1),
            )
            advanceUntilIdle()
            assertEquals(
                2,
                f
                    .rows()
                    .single()
                    .memberKeys.size,
            )
            outbox.enqueue(
                OutboxChange.Settings(SettingsSection.Reminders, ReminderSettings.enabledPatch(false)),
                now = reminderNow.plusMillis(2),
            )
            advanceUntilIdle()
            assertTrue(f.rows().isEmpty())
            f.service.detach()
            ReminderStore.replaceLedger(f.store.database, listOf(ReminderLedgerEntry.from(request)))
            f.store.settings()
            val relaunched = reminderService(f.alarms, clock = { request.sendAt.plusSeconds(1) })
            try {
                relaunched.attach(f.store.database)
                relaunched.reschedule()
                assertTrue(f.rows().isEmpty())
            } finally {
                relaunched.close()
            }
        }

    @Test fun currentOfflinePlansOnlyAndAccountSwitchClears() =
        fixture { f ->
            f.store.store(reminderPlan())
            f.start()
            val repo = TripRepository(f.store.database)
            val original = repo.trips().single()
            repo.upsert(original.copy(currentInputHash = "changed"))
            advanceUntilIdle()
            assertTrue(f.rows().isEmpty())
            repo.upsert(original.copy(keepOffline = false))
            f.service.reschedule()
            assertTrue(f.rows().isEmpty())
            repo.upsert(original.copy(deletedAt = reminderNow.toString()))
            f.service.reschedule()
            assertTrue(f.rows().isEmpty())
            repo.upsert(original)
            advanceUntilIdle()
            assertEquals(1, f.rows().size)
            val next = ReminderTestStore(StandardTestDispatcher(testScheduler), "01900000-0000-7000-8000-000000000002")
            try {
                f.service.attach(next.database)
                f.service.reschedule()
                assertTrue(f.alarms.active.isEmpty())
                assertTrue(f.rows().isEmpty())
                assertTrue(ReminderStore.ledger(f.store.database).isEmpty())
                f.service.detach()
            } finally {
                next.close()
            }
        }

    @Test fun dueRowsAreKeptAndExpiredRowsDropped() =
        fixture { f ->
            f.store.store(reminderPlan())
            f.start()
            val row = f.rows().single()
            val due =
                row.copy(
                    identifier = "due",
                    requestCode = 11,
                    sendAt = reminderNow.minusSeconds(60),
                    expiresAt = reminderNow.plusSeconds(600),
                )
            ReminderStore.save(f.store.database, due)
            ReminderStore.save(
                f.store.database,
                due.copy(identifier = "expired", requestCode = 12, expiresAt = reminderNow.minusSeconds(1)),
            )
            f.service.reschedule()
            assertTrue(f.rows().any { it.identifier == "due" })
            assertFalse(f.rows().any { it.identifier == "expired" })
            assertFalse("due" in f.alarms.cancels)
            f.store.settings(ReminderSettings().settingEnabled(false))
            f.service.reschedule()
            assertTrue(f.rows().isEmpty())
            assertTrue("due" in f.alarms.cancels)
        }

    @Test fun firstPassInAProcessRearmsUnchangedRows() =
        fixture { f ->
            f.store.store(reminderPlan())
            f.start()
            val alarms = FakeReminderAlarms()
            val second = reminderService(alarms)
            try {
                second.attach(f.store.database)
                second.reschedule()
                assertEquals(1, alarms.sets.size)
                second.reschedule()
                assertEquals(1, alarms.sets.size)
            } finally {
                second.close()
            }
        }

    @Test fun detachWithoutAttachTouchesNothing() =
        fixture { f ->
            f.service.detach()
            assertEquals(0, f.notices.removed)
            assertTrue(f.alarms.cancels.isEmpty())
        }

    @Test fun deliveredDeletesTheRowAndRecordsTheLedger() =
        fixture { f ->
            f.store.store(reminderPlan())
            f.start()
            val row = f.rows().single()
            f.service.delivered("other-account", row.identifier)
            assertEquals(1, f.rows().size)
            f.service.delivered(reminderAccount, row.identifier)
            assertTrue(f.rows().isEmpty())
            assertEquals(row.memberKeys, ReminderStore.ledger(f.store.database).single().memberKeys)
        }

    private class Fixture(
        val store: ReminderTestStore,
        val service: ReminderService,
        val alarms: FakeReminderAlarms,
        val notices: FakeReminderNotices,
        val setPermission: (ReminderPermission) -> Unit,
    ) {
        var permission = ReminderPermission.Granted
            set(value) {
                field = value
                setPermission(value)
            }

        suspend fun rows() = ReminderStore.rows(store.database)

        suspend fun start() {
            service.attach(store.database)
            service.reschedule()
        }
    }

    private fun fixture(test: suspend TestScope.(Fixture) -> Unit) =
        runTest(timeout = 60.seconds) {
            val store = ReminderTestStore(StandardTestDispatcher(testScheduler))
            val alarms = FakeReminderAlarms()
            val notices = FakeReminderNotices()
            var permission = ReminderPermission.Granted
            val service = reminderService(alarms, notices, permission = { permission })
            try {
                test(Fixture(store, service, alarms, notices) { permission = it })
            } finally {
                withContext(NonCancellable) {
                    service.close()
                    store.close()
                }
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun permissions(): List<Array<ReminderPermission>> = ReminderPermission.entries.map { arrayOf(it) }
    }
}
