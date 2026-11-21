package net.chronoflow.feature.settings

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.sync.SyncEvent
import java.time.ZoneId

interface SettingsDependencies {
    val snapshot: SessionSnapshot

    fun account(): Flow<AccountRecord?>

    fun settings(): Flow<List<SettingsRecord>>

    fun outbox(): Flow<List<OutboxRecord>>

    suspend fun pendingOutbox(): List<OutboxRecord>

    fun syncEvents(): Flow<SyncEvent>

    fun sessionUpdates(): Flow<SessionUpdate>

    suspend fun isCurrent(): Boolean

    suspend fun signOut(discardPending: Boolean): SignOutResult

    suspend fun enqueueProfile(
        name: String,
        email: String,
    )

    suspend fun requestSync()

    val clock: PlanClock

    suspend fun formatter(): CopyLineFormatter

    val zone: ZoneId

    suspend fun enqueueSettings(
        section: SettingsSection,
        patch: JsonObject,
    )
}

object SettingsDependenciesKey : CreationExtras.Key<SettingsDependencies>

object SettingsMainKey : CreationExtras.Key<CoroutineDispatcher>
