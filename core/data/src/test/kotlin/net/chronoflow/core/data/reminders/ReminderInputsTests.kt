package net.chronoflow.core.data.reminders

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.database.store.saveCheckMark
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderInputsTests {
    @Test fun currentOfflinePlanIncludesOnlyDoneAndSkippedMarks() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val store = SyncStore(dispatcher)
            try {
                val repository = TripRepository(store.database)
                repository.upsert(repository.trips().single().copy(keepOffline = true))
                repository.store(version(), listOf(mark("done", 1), mark("skipped", 2), mark("pending", 3)))
                store.database.write { it.saveCheckMark(mark("done", 4).copy(inputHash = "other")) }
                val inputs = store.database.read { ReminderInputs.fetch(it) }
                val plan = inputs.plans(PlanDecoder(dispatcher)).single()
                assertEquals(listOf(CheckStatus.Done, CheckStatus.Skipped), plan.marks.map { it.status })
                assertEquals(1, inputs.versions.size)
                assertEquals(2, inputs.checks.size)
                assertEquals(inputs, store.database.read { ReminderInputs.fetch(it) })
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun notDownloadedIsExcluded() = excluded { it.copy(keepOffline = false) }

    @Test fun deletedIsExcluded() = excluded { it.copy(deletedAt = "2026-10-14T00:00:00Z") }

    @Test fun locallyDeletedIsExcluded() = excluded { it.copy(deletedLocally = true) }

    @Test fun changedHashIsExcluded() = excluded { it.copy(currentInputHash = "new") }

    @Test fun absentHashIsExcluded() = excluded { it.copy(currentInputHash = null) }

    @Test fun completedCardIsExcluded() =
        excluded { trip ->
            val card = card().card
            trip.copy(
                card =
                    ApiJson.response.encodeToString(
                        TripCardView.serializer(),
                        card.copy(progress = card.progress.copy(status = TripStatus.Completed)),
                    ),
            )
        }

    @Test fun undecodableCardIsExcluded() = excluded { it.copy(card = "{}") }

    @Test fun undecodableDetailIsExcluded() = excluded(badDetail = true) { it }

    @Test fun settingsAreReadAndAbsentValuesAreNull() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val empty = store.database.read { ReminderInputs.fetch(it) }
                assertNull(empty.reminders)
                assertNull(empty.preferences)
                SettingsRepository(store.database).upsert(SettingsRecord("reminders", "{\"light\":true}"))
                SettingsRepository(store.database).upsert(SettingsRecord("preferences", "{\"timeFormat\":\"12\"}"))
                val inputs = store.database.read { ReminderInputs.fetch(it) }
                assertEquals("{\"light\":true}", inputs.reminders)
                assertEquals("{\"timeFormat\":\"12\"}", inputs.preferences)
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    private fun excluded(
        badDetail: Boolean = false,
        transform: (TripRecord) -> TripRecord,
    ) = runTest(timeout = 60.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = SyncStore(dispatcher)
        try {
            val repository = TripRepository(store.database)
            repository.upsert(transform(repository.trips().single().copy(keepOffline = true)))
            repository.store(version(badDetail), emptyList())
            assertTrue(
                store.database
                    .read { ReminderInputs.fetch(it) }
                    .plans(PlanDecoder(dispatcher))
                    .isEmpty(),
            )
        } finally {
            withContext(NonCancellable) { store.close() }
        }
    }

    private fun version(bad: Boolean = false): PlanVersionRecord =
        PlanVersionRecord(
            syncTrip,
            "hash",
            if (bad) {
                "{}".encodeToByteArray()
            } else {
                ApiJson.response
                    .encodeToString(
                        TripDetail.serializer(),
                        detail(),
                    ).encodeToByteArray()
            },
            "2026-10-14T00:00:00Z",
            "1",
        )

    private fun mark(
        status: String,
        ordinal: Int,
    ): CheckMarkRecord =
        CheckMarkRecord(syncTrip, "hash", "seek_light", "2026-11-10", ordinal, status, "2026-10-14T00:00:00Z")
}
