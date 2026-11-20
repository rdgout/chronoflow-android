package net.chronoflow.feature.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.AirportSearchSession
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.resumeCompletion
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import net.chronoflow.core.navigation.WizardMode as WizardEntry

class TripWizardViewModel(
    entry: WizardEntry,
    deps: TripWizardDependencies?,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TripWizardUiState>(TripWizardUiState.Loading)
    val state: StateFlow<TripWizardUiState> = mutableState.asStateFlow()
    private var generation: TripGenerationService? = null
    private var discarded = false
    var wizard: MemberWizardModel? = null
        private set
    val search =
        AirportSearchSession(
            deps?.airportSearch ?: AirportSearchService(
                {
                    throw DataFailure.Unauthorized(
                        401,
                    )
                },
                { Instant.EPOCH },
                {},
            ),
            viewModelScope,
            deps?.now ?: { Instant.EPOCH },
        )

    init {
        if (deps == null) {
            mutableState.value = TripWizardUiState.LoadFailed(DataFailure.Unauthorized(401))
        } else {
            viewModelScope.launch {
                try {
                    val draft =
                        when (entry) {
                            WizardEntry.Create -> deps.start()
                            is WizardEntry.Edit -> deps.startTrip(entry.tripId, TripWizardAction.edit)
                            is WizardEntry.Duplicate -> deps.startTrip(entry.tripId, TripWizardAction.duplicate)
                        }
                    currentCoroutineContext().ensureActive()
                    val service = deps.newGeneration().also { generation = it }
                    val model =
                        MemberWizardModel(
                            draft,
                            deps.store,
                            viewModelScope,
                            deps.now,
                            service,
                            deps.checkingClock,
                            writerScope = deps.writerScope,
                        )
                    wizard = model
                    model.resumeChecking()
                    model.resumeCompletion()
                    model.snapshots.collect { snapshot ->
                        val finished = snapshot.closed && snapshot.createdTripID != null
                        mutableState.value =
                            if (discarded || finished) {
                                TripWizardUiState.Closed(snapshot.createdTripID)
                            } else {
                                TripWizardUiState.Active(snapshot, model.progress, model.navigationPath)
                            }
                    }
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    loadFailed(DataFailure.Server(null))
                } catch (failure: DataFailure) {
                    loadFailed(failure)
                } catch (_: SessionFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: DeviceIdentityFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: StoreFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: RecoveryRejected) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: IOException) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: GeneralSecurityException) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: RuntimeException) {
                    loadFailed(DataFailure.Server(null))
                }
            }
        }
    }

    private fun loadFailed(failure: DataFailure) {
        generation?.cancelNow()
        mutableState.value = TripWizardUiState.LoadFailed(failure)
    }

    suspend fun discard() {
        val model = wizard ?: return
        model.discard()
        if (model.closed) {
            discarded = true
            mutableState.value = TripWizardUiState.Closed(model.createdTripID)
        }
    }

    override fun onCleared() {
        generation?.cancelNow()
    }

    companion object {
        fun factory(entry: WizardEntry): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TripWizardViewModel(entry, this[TripWizardDependenciesKey]) }
            }
    }
}
