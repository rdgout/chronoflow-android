@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.storing
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreTime
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.ZoneOffset

internal enum class SaveHold { None, BeforeRequest, AfterReceipt, DuringPull }

internal class WizardSaveSource(
    private val remote: DataSource,
    private val hold: SaveHold,
) : DataSource by remote {
    val gate = CompletableDeferred<Unit>()
    val requests = mutableListOf<ApiTripSaveInput>()
    var reviews = 0
    var created: String? = null

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview {
        reviews++
        return remote.reviewTrip(input)
    }

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult {
        requests.add(identity)
        if (hold == SaveHold.BeforeRequest) gate.await()
        val result = remote.saveTrip(identity)
        created = result.tripId
        if (hold == SaveHold.AfterReceipt) {
            gate.await()
            throw DataFailure.Offline
        }
        return result
    }
}

internal class WizardSaveGraphFixture private constructor(
    val database: AccountDatabase,
    val store: MemberWizardStore,
    val source: WizardSaveSource,
    val remote: MockDataSource,
    val model: TripWizardViewModel,
    private val owner: ViewModelStore,
    private val service: TripGenerationService,
    private val scope: CoroutineScope,
    private val epoch: SyncEpoch,
) {
    suspend fun kill() =
        withContext(NonCancellable) {
            owner.clear()
            model.viewModelScope.coroutineContext.job
                .cancelAndJoin()
            service.cancel()
            scope.coroutineContext.job.cancelAndJoin()
            epoch.invalidate()
            database.close()
        }

    companion object {
        suspend fun open(
            test: TestScope,
            root: File,
            clock: WizardTestClock,
            hold: SaveHold = SaveHold.None,
            seedReady: Boolean = false,
            existingTrip: Boolean = true,
        ): WizardSaveGraphFixture {
            val io = StandardTestDispatcher(test.testScheduler)
            val scope =
                CoroutineScope(
                    test.backgroundScope.coroutineContext + SupervisorJob(test.backgroundScope.coroutineContext.job),
                )
            val seed = MockAppSeed.make(clock.now, zone = ZoneOffset.UTC)
            val account = seed.accounts.values.first { it.twoFactorCode == null }
            val remote =
                MockDataSource(store = FileMockStateStore(root), seed = seed, now = {
                    clock.now
                }, delay = { 0.0 }, sleep = {}, zone = ZoneOffset.UTC, io = io)
            remote.signIn(
                PasswordSignInInput(
                    account.snapshot.email,
                    account.password,
                    "019a562b-8000-7000-8000-000000000099",
                    "Tests",
                ),
            )
            val source = WizardSaveSource(remote, hold)
            val database =
                AccountDatabase(
                    root,
                    driverFactory = AndroidAccountDriverFactory(RuntimeEnvironment.getApplication()),
                    io = io,
                )
            database.open(account.snapshot.id)
            val settings = SettingsRepository(database)
            if (seedReady) {
                val snapshot = remote.fetchAccount()
                settings.upsert(
                    SettingsRecord(
                        "bodyClock",
                        ApiJson.response.encodeToString(
                            kotlinx.serialization.json.JsonObject
                                .serializer(),
                            snapshot.settings.bodyClock,
                        ),
                    ),
                )
                settings.upsert(SettingsRecord("sleepAnswered", "true"))
                if (existingTrip) {
                    val cards = remote.fetchTrips()
                    database.write { TripRepository.applyCards(cards, StoreTime.seconds(clock.now), it) }
                }
            }
            val store = MemberWizardStore(database, now = { clock.now }, io = io)
            if (seedReady) {
                val original = store.start()
                store.persist(
                    original.copy(
                        form = WizardForm.from(generationInput()),
                        state =
                            original.state.copy(
                                step = MemberWizardStep.ready,
                                revision =
                                    original.state.revision + 1,
                            ),
                    ),
                )
            }
            val epoch = SyncEpoch(checkNotNull(database.accountID), 1)
            val storing = TripGenerationEffects.storing(database, source, epoch, requestSync = {}, now = { clock.now })
            val effects =
                TripGenerationEffects(storing.mergeProfile, { id ->
                    if (hold == SaveHold.DuringPull) source.gate.await()
                    storing.pullCreatedTrip(id)
                }, storing.requestSync)
            val service = TripGenerationService(source, effects, parentContext = scope.coroutineContext)
            val deps =
                object : TripWizardDependencies {
                    override val store = store
                    override val source = source
                    override val writerScope = scope
                    override val checkingClock = CheckingTestClock(test.testScheduler).clock
                    override val now = { clock.now }
                    override val airportSearch = AirportSearchService({ source.searchAirports(it, null) }, now, {})

                    override suspend fun start() = store.start()

                    override suspend fun startTrip(
                        id: String,
                        action: TripWizardAction,
                    ) = store.startTrip(id, action, source)

                    override fun newGeneration() = service
                }
            val owner = ViewModelStore()
            val model =
                ViewModelProvider.create(
                    owner,
                    TripWizardViewModel.factory(
                        WizardMode.Create,
                    ),
                    MutableCreationExtras().apply {
                        set(TripWizardDependenciesKey, deps)
                    },
                )[TripWizardViewModel::class.java]
            return WizardSaveGraphFixture(database, store, source, remote, model, owner, service, scope, epoch)
        }
    }
}
