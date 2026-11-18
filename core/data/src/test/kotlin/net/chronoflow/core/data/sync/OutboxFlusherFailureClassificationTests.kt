package net.chronoflow.core.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.sync.CheckOffReconciler
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFailurePolicy
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncOther
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class OutboxFlusherFailureClassificationTests(
    private val label: String,
    private val failure: DataFailure,
    private val final: Boolean,
    private val attempts: Int,
    private val lastError: String?,
) {
    @Test fun failureClassification() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                store.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.Preferences,
                        JsonObject(mapOf("time_format" to JsonPrimitive("12"))),
                    ),
                )
                val source = ScriptedSource()
                source.configure(failures = listOf(failure))
                val flusher = OutboxFlusher(store.sync, source, { syncDate })
                if (failure is DataFailure.Unauthorized || failure is DataFailure.EmailUnverified) {
                    try {
                        flusher.run(store.epoch)
                        error("expected protected failure")
                    } catch (
                        actual: DataFailure,
                    ) {
                        assertEquals(label, failure, actual)
                    }
                } else {
                    val result = flusher.run(store.epoch)
                    assertEquals(label, if (final) 1 else 0, result.refusals.size)
                    if (failure is DataFailure.Throttled) assertEquals(900.0, result.retryAfter)
                }
                val pending = OutboxRepository(store.database).pending()
                assertEquals(label, if (final) 0 else 1, pending.size)
                if (!final) {
                    assertEquals(attempts, pending.first().attempts)
                    assertEquals(lastError, pending.first().lastError)
                }
                val settings = SettingsRepository(store.database).settings()
                assertEquals(final, settings.any { it.section == "syncFailure" })
                assertFalse(settings.toString().contains("secret response"))
            } finally {
                store.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any?>> = failureClassificationRows()
    }
}
