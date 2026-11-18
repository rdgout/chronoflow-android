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
class OutboxFlusherPolicyClassificationTests(
    private val label: String,
    private val failure: DataFailure,
    private val final: Boolean,
) {
    @Test fun policyClassification() {
        assertEquals(label, final, OutboxFailurePolicy.isFinal(failure))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any?>> = failureClassificationRows("invalid").map { arrayOf(it[0], it[1], it[2]) }
    }
}
