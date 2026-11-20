@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.storing
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreTime
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.trip.actions.TripWizardPreparer
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageSource
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.timeline.TimelineTracer
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

internal class TripActionsWizardFixture private constructor(
    val remote: MockDataSource,
    val database: AccountDatabase,
    val trips: TripRepository,
    val deps: TripWizardDependencies,
    val io: kotlinx.coroutines.CoroutineDispatcher,
    private val services: MutableList<TripGenerationService>,
    private val scope: CoroutineScope,
    private val epoch: SyncEpoch,
) {
    val now: Instant get() = deps.now()
    private val pageOwner = ViewModelStore()
    private val wizardOwner = ViewModelStore()
    private val models = mutableListOf<androidx.lifecycle.ViewModel>()

    fun track(model: androidx.lifecycle.ViewModel) {
        if (model !in models) models.add(model)
    }

    fun wizard(mode: WizardMode): TripWizardViewModel =
        ViewModelProvider
            .create(
                wizardOwner,
                TripWizardViewModel.factory(mode),
                MutableCreationExtras().apply { set(TripWizardDependenciesKey, deps) },
            )[TripWizardViewModel::class.java]
            .also { if (it !in models) models.add(it) }

    fun page(
        id: String,
        preparer: TripWizardPreparer,
    ): TripPageViewModel {
        val catalogs = CopyCatalogProvider(io)
        val decoder = PlanDecoder(io)
        val plans = PlanPreparer(decoder, catalogs, io)
        val retention = PlanRetention()
        val source =
            object : TripPageSource {
                override fun snapshots(tripId: String) = trips.detailValues(tripId)

                override fun preferences() = flowOf(PlanPreferences())

                override fun hold(tripId: String) = retention.hold(tripId)

                override suspend fun refresh() = null
            }
        return ViewModelProvider(
            pageOwner,
            viewModelFactory {
                initializer {
                    TripPageViewModel(
                        TripPageArgs(id),
                        source,
                        FakePlanClock(now),
                        DefaultTripPlanPipeline(decoder, plans, catalogs, io, TimelineTracer.None) {},
                        SavedStateHandle(),
                        Language.En,
                        io,
                        io,
                        { FakeTextMetrics() },
                        formatter = { CopyLineFormatter(catalogs.catalog()) },
                        tracer = TimelineTracer.None,
                        preparer = preparer,
                    )
                }
            },
        )[TripPageViewModel::class.java].also { models.add(it) }
    }

    suspend fun close() =
        withContext(NonCancellable) {
            pageOwner.clear()
            wizardOwner.clear()
            models.forEach {
                it.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
            services.forEach { it.cancel() }
            scope.coroutineContext.job.cancelAndJoin()
            epoch.invalidate()
            database.close()
        }

    companion object {
        const val ACTIVE = "0199a3c0-0000-7000-8000-000000000001"
        const val UPCOMING = "0199a3c0-0000-7000-8000-000000000002"
        val instant: Instant = Instant.parse("2026-11-20T12:00:00Z")

        suspend fun open(
            test: TestScope,
            root: File,
            id: String,
        ): TripActionsWizardFixture {
            val io = StandardTestDispatcher(test.testScheduler)
            val scope =
                CoroutineScope(
                    test.backgroundScope.coroutineContext +
                        SupervisorJob(test.backgroundScope.coroutineContext.job) + io,
                )
            val seed = MockAppSeed.make(instant, zone = ZoneOffset.UTC)
            val account = seed.accounts.values.first { it.twoFactorCode == null }
            val remote =
                MockDataSource(
                    store = FileMockStateStore(root),
                    seed = seed,
                    now = { instant },
                    delay = { 0.0 },
                    sleep = {},
                    zone = ZoneOffset.UTC,
                    io = io,
                )
            remote.signIn(
                PasswordSignInInput(
                    account.snapshot.email,
                    account.password,
                    "019a562b-8000-7000-8000-000000000099",
                    "Tests",
                ),
            )
            val database =
                AccountDatabase(
                    root,
                    driverFactory = AndroidAccountDriverFactory(RuntimeEnvironment.getApplication()),
                    io = io,
                )
            database.open(account.snapshot.id)
            val bodyClock = remote.fetchAccount().settings.bodyClock
            val settings = SettingsRepository(database)
            settings.upsert(
                SettingsRecord("bodyClock", ApiJson.response.encodeToString(JsonObject.serializer(), bodyClock)),
            )
            settings.upsert(SettingsRecord("sleepAnswered", "true"))
            val cards = remote.fetchTrips()
            val detail = remote.fetchTripDetail(id)
            val at = StoreTime.seconds(instant)
            database.write { sql ->
                TripRepository.applyCards(cards, at, sql)
                TripRepository.applyDetail(detail, id, at, sql)
            }
            val store = MemberWizardStore(database, now = { instant }, io = io)
            val epoch = SyncEpoch(checkNotNull(database.accountID), 1)
            val services = mutableListOf<TripGenerationService>()
            val deps =
                object : TripWizardDependencies {
                    override val store = store
                    override val source = remote
                    override val writerScope = scope
                    override val now = { instant }
                    override val checkingClock = CheckingTestClock(test.testScheduler).clock
                    override val airportSearch = AirportSearchService({ remote.searchAirports(it, null) }, now, {})

                    override suspend fun recover() = store.recover()

                    override suspend fun start() = store.start()

                    override suspend fun startTrip(
                        id: String,
                        action: TripWizardAction,
                    ) = store.startTrip(id, action, source)

                    override fun newGeneration() =
                        TripGenerationService(
                            source,
                            TripGenerationEffects.storing(database, source, epoch, requestSync = {}, now = now),
                            parentContext = scope.coroutineContext,
                        ).also { services.add(it) }
                }
            return TripActionsWizardFixture(
                remote,
                database,
                TripRepository(database),
                deps,
                io,
                services,
                scope,
                epoch,
            )
        }
    }
}
