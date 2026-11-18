package net.chronoflow.app

import kotlinx.coroutines.CoroutineScope
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.sync.SessionSync

data class SessionGraph(
    val epoch: Long,
    val snapshot: SessionSnapshot,
    val database: AccountDatabase,
    val accounts: AccountRepository,
    val trips: TripRepository,
    val settings: SettingsRepository,
    val outbox: OutboxRepository,
    val sync: SessionSync,
    val dataSource: DataSource,
    val scope: CoroutineScope,
)
