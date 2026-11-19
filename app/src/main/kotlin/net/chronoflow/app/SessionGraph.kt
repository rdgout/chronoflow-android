package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline

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
) {
    private var planDecoder: PlanDecoder? = null
    private var planPreparer: PlanPreparer? = null

    @Synchronized
    fun tripPlanPipeline(
        compute: CoroutineDispatcher,
        catalogs: CopyCatalogProvider,
    ): DefaultTripPlanPipeline {
        val decoder = planDecoder ?: PlanDecoder(compute).also { planDecoder = it }
        val preparer = planPreparer ?: PlanPreparer(decoder, catalogs, compute).also { planPreparer = it }
        return DefaultTripPlanPipeline(decoder, preparer)
    }
}
