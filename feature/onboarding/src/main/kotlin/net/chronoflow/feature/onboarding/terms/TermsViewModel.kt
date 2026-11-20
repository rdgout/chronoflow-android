package net.chronoflow.feature.onboarding.terms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.onboarding.OnboardingMainKey
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.OnboardingSessions
import net.chronoflow.feature.onboarding.OnboardingSessionsKey
import net.chronoflow.feature.onboarding.SessionChangedException
import net.chronoflow.feature.onboarding.retryOnboardingLoad
import java.io.IOException
import java.security.GeneralSecurityException

data class TermsUiState(
    val policies: AccountPolicies? = null,
    val busy: Boolean = false,
    val completed: Boolean = false,
    val failed: Boolean = false,
    val signOutBusy: Boolean = false,
    val signOutFailed: Boolean = false,
    val pendingSignOutCount: Int? = null,
) {
    val canAccept: Boolean get() =
        policies != null && !busy && !completed &&
            policies.acceptedTermsVersion != policies.termsVersion
    val acceptEnabled: Boolean get() = canAccept && !signOutBusy
    val signOutEnabled: Boolean get() = !busy && !completed && !signOutBusy
}

enum class TermsEvent { Accept, SignOut, Stay, Discard }

class TermsViewModel(
    sessions: OnboardingSessions,
    main: CoroutineDispatcher,
) : ViewModel(
        CoroutineScope(SupervisorJob() + main),
    ) {
    private data class SignOutState(
        val busy: Boolean = false,
        val failed: Boolean = false,
        val count: Int? = null,
    )

    private val signOutState = MutableStateFlow(SignOutState())
    private var session: OnboardingSession? = null
    private var model: TermsUpdateModel? = null
    private val mutableState = MutableStateFlow<TermsUiState?>(null)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            var boundSession: SessionSnapshot? = null
            retryOnboardingLoad(sessions::current)
                .transformWhile { current ->
                    val unchanged = boundSession == null || current?.snapshot == boundSession
                    if (current != null && boundSession == null) boundSession = current.snapshot
                    emit(current.takeIf { unchanged })
                    unchanged
                }.distinctUntilChangedBy { it?.snapshot }
                .collectLatest { current ->
                    session = current
                    if (current == null) {
                        model = null
                    } else {
                        observePolicies(current)
                    }
                }
        }
    }

    private suspend fun observePolicies(session: OnboardingSession) =
        coroutineScope {
            val terms = TermsUpdateModel { session.enqueue(OutboxChange.Policies(it)) }
            model = terms
            launch {
                combine(terms.state, signOutState) { termsState, signOut ->
                    TermsUiState(
                        termsState.policies,
                        termsState.busy,
                        termsState.completed,
                        termsState.failed,
                        signOut.busy,
                        signOut.failed,
                        signOut.count,
                    )
                }.collect { mutableState.value = it }
            }
            retryOnboardingLoad { session.policies }.collect(terms::update)
        }

    fun onEvent(event: TermsEvent) {
        when (event) {
            TermsEvent.Accept -> accept()
            TermsEvent.SignOut -> signOut()
            TermsEvent.Stay -> stay()
            TermsEvent.Discard -> signOut(discardPending = true)
        }
    }

    fun accept() {
        viewModelScope.launch { if (!signOutState.value.busy) model?.accept() }
    }

    fun stay() {
        if (!signOutState.value.busy) signOutState.update { it.copy(count = null) }
    }

    fun signOut(discardPending: Boolean = false) {
        viewModelScope.launch {
            val session = session ?: return@launch
            val value = signOutState.value
            if (value.busy || (discardPending && value.count == null)) return@launch
            signOutState.update { it.copy(busy = true, failed = false) }
            try {
                when (val result = session.signOut(discardPending)) {
                    SignOutResult.SignedOut -> signOutState.update { it.copy(count = null) }
                    is SignOutResult.NeedsConfirmation -> signOutState.update { it.copy(count = result.pendingCount) }
                }
            } catch (_: SessionChangedException) {
            } catch (_: StaleSessionException) {
            } catch (failure: CancellationException) {
                if (!currentCoroutineContext().isActive) throw failure
                signOutFailed()
            } catch (_: SessionFailure) {
                signOutFailed()
            } catch (_: DataFailure) {
                signOutFailed()
            } catch (_: DeviceIdentityFailure) {
                signOutFailed()
            } catch (_: StoreFailure) {
                signOutFailed()
            } catch (_: LocalMutationFailure) {
                signOutFailed()
            } catch (_: IOException) {
                signOutFailed()
            } catch (_: GeneralSecurityException) {
                signOutFailed()
            } catch (_: RuntimeException) {
                signOutFailed()
            } finally {
                signOutState.update { it.copy(busy = false) }
            }
        }
    }

    private fun signOutFailed() {
        signOutState.update { it.copy(failed = true) }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    TermsViewModel(
                        checkNotNull(this[OnboardingSessionsKey]),
                        this[OnboardingMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
