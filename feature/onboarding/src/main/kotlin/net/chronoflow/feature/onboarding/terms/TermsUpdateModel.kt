package net.chronoflow.feature.onboarding.terms

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.onboarding.SessionChangedException
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException

class TermsUpdateModel(
    policies: AccountPolicies? = null,
    private val save: suspend (String) -> Unit,
) {
    data class State(
        val policies: AccountPolicies?,
        val busy: Boolean = false,
        val completed: Boolean = false,
        val failed: Boolean = false,
    ) {
        val canAccept: Boolean get() =
            policies != null && !busy && !completed &&
                policies.acceptedTermsVersion != policies.termsVersion
    }

    private val mutableState = MutableStateFlow(State(policies))
    val state = mutableState.asStateFlow()

    fun update(policies: AccountPolicies?) {
        mutableState.update {
            if (it.policies?.termsVersion != policies?.termsVersion) {
                it.copy(policies = policies, completed = false, failed = false)
            } else {
                it.copy(policies = policies)
            }
        }
    }

    suspend fun accept() {
        val value = state.value
        if (!value.canAccept) return
        val policies = checkNotNull(value.policies)
        mutableState.update { it.copy(busy = true, failed = false) }
        try {
            save(policies.termsVersion)
            mutableState.update { it.copy(completed = it.policies?.termsVersion == policies.termsVersion) }
        } catch (_: SessionChangedException) {
        } catch (_: StaleSessionException) {
        } catch (failure: CancellationException) {
            if (!currentCoroutineContext().isActive) throw failure
            failed()
        } catch (_: SessionFailure) {
            failed()
        } catch (_: DataFailure) {
            failed()
        } catch (_: DeviceIdentityFailure) {
            failed()
        } catch (_: StoreFailure) {
            failed()
        } catch (_: LocalMutationFailure) {
            failed()
        } catch (_: IOException) {
            failed()
        } catch (_: GeneralSecurityException) {
            failed()
        } catch (_: SQLException) {
            failed()
        } catch (_: RuntimeException) {
            failed()
        } finally {
            mutableState.update { it.copy(busy = false) }
        }
    }

    private fun failed() {
        mutableState.update { it.copy(failed = true) }
    }
}
