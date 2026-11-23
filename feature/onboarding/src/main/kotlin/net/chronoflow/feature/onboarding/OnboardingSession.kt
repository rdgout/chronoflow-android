package net.chronoflow.feature.onboarding

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.sync.session.SessionController

class SessionChangedException : Exception(null, null, false, false)

class OnboardingSession(
    val snapshot: SessionSnapshot,
    val enqueue: suspend (OutboxChange) -> Unit,
    val policies: Flow<AccountPolicies?>,
    val signOut: suspend (discardPending: Boolean) -> SignOutResult,
) {
    companion object {
        fun from(
            controller: SessionController,
            snapshot: SessionSnapshot,
            permit: SessionMutationPermit,
            outbox: OutboxRepository,
            settings: SettingsRepository,
        ): OnboardingSession =
            OnboardingSession(
                snapshot,
                enqueue = { change ->
                    if (controller.activeSession() != snapshot) throw SessionChangedException()
                    outbox.enqueue(change, permit)
                },
                policies =
                    settings
                        .values()
                        .map { values ->
                            values.firstOrNull { it.section == "policies" }?.let {
                                ApiJson.response.decodeFromString(AccountPolicies.serializer(), it.value)
                            }
                        }.distinctUntilChanged(),
                signOut = { discard -> controller.signOut(expected = snapshot, discardPending = discard) },
            )
    }
}

fun interface OnboardingSessions {
    fun current(): Flow<OnboardingSession?>
}

object OnboardingSessionsKey : CreationExtras.Key<OnboardingSessions>
