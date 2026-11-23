package net.chronoflow.app.wizard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.storing
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchCache
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.TripWizardDependencies
import java.time.Instant
import kotlin.math.roundToLong

class SessionTripWizardDependencies(
    private val session: SessionGraph,
    private val permit: SessionMutationPermit,
    private val io: CoroutineDispatcher,
    private val catalogs: CopyCatalogProvider? = null,
) : TripWizardDependencies {
    private val formatterMutex = Mutex()
    private var cachedFormatter: CopyLineFormatter? = null

    override suspend fun copyFormatter(): CopyLineFormatter? =
        formatterMutex.withLock {
            cachedFormatter ?: catalogs?.catalog()?.let(::CopyLineFormatter)?.also { cachedFormatter = it }
        }

    override val timeFormat =
        session.settings
            .values()
            .map { records ->
                val raw = records.firstOrNull { it.section == "preferences" }?.value
                val value =
                    try {
                        raw?.let { ApiJson.response.parseToJsonElement(it) as? JsonObject }
                    } catch (_: SerializationException) {
                        null
                    }
                TimeFormat.of((value?.get("timeFormat") as? JsonPrimitive)?.contentOrNull ?: "24")
            }.flowOn(io)
    override val now: () -> Instant = { Instant.now() }
    override val store = MemberWizardStore(session.database, permit, now, io)
    override val source get() = session.dataSource
    override val airportSearch =
        AirportSearchService(
            source,
            now,
            sleep = { delay((it * 1000).roundToLong()) },
            cache = AirportSearchCache(),
        )
    override val writerScope = CoroutineScope(session.scope.coroutineContext + io)

    override suspend fun recover() = store.recover()

    override suspend fun start() = store.start()

    override suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ) = store.startTrip(id, action, source)

    override fun newGeneration() =
        TripGenerationService(
            source,
            TripGenerationEffects.storing(
                session.database,
                source,
                SyncEpoch(AccountID.of(session.snapshot.session.accountId), session.snapshot.epoch),
                permit,
                requestSync = { session.sync.requestSync() },
                now = now,
            ),
            parentContext = session.scope.coroutineContext + io,
        )
}
