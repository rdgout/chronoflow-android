package net.chronoflow.core.data.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.wizard.model.GuestWizardStore
import java.time.Instant

class GuestSessionAcceptance(
    private val guest: GuestWizardStore,
    private val secrets: GuestSecretStore,
    private val now: () -> Instant,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun routeAcceptedSession(
        result: SessionResult,
        method: AuthenticationMethod,
        controller: SessionController,
        accounts: SessionAccountDatabase,
        readiness: StoredDataReadiness,
        scope: CoroutineScope,
        navigate: (Gate) -> Unit,
        verification: suspend (Long) -> Unit,
    ) {
        scope
            .async {
                val snapshot = controller.activeSession() ?: throw GuestAcceptanceSuperseded()
                if (AccountID.of(snapshot.session.accountId) != AccountID.of(result.account.id) ||
                    snapshot.session.token != result.token
                ) {
                    throw GuestAcceptanceSuperseded()
                }
                val database = accounts.activeAccountDatabase() ?: throw GuestAcceptanceSuperseded()
                val permit = controller.mutationPermit(snapshot) ?: throw GuestAcceptanceSuperseded()
                accept(result, method, database, permit)
                if (controller.activeSession() != snapshot) return@async
                readiness.routeAcceptedSession(navigate, verification)
            }.await()
    }

    suspend fun accept(
        result: SessionResult,
        method: AuthenticationMethod,
        database: AccountDatabase,
        permit: SessionMutationPermit? = null,
    ) {
        if (database.accountID != AccountID.of(result.account.id)) throw GuestAcceptanceSuperseded()
        val draft = guest.recover()
        val continuation = GuestContinuationRepository(database, permit)
        val tripId = result.tripId
        if (tripId != null) {
            continuation.store(GuestSessionContinuation.Trip(tripId))
            secrets.delete(tripId)
        } else if (draft != null) {
            when (method) {
                AuthenticationMethod.Password, AuthenticationMethod.Passkey -> {
                    MemberWizardStore(database, permit, now, io).start(draft)
                    continuation.store(GuestSessionContinuation.MemberWizard)
                }

                AuthenticationMethod.TwoFactor -> {
                    continuation.store(GuestSessionContinuation.Trips)
                }

                AuthenticationMethod.SignUp -> {
                    return
                }
            }
        }
        if (draft != null) {
            draft.form.identity
                ?.token
                ?.let { secrets.delete(it) }
            guest.discard(draft.state.instance)
        }
    }
}

class GuestAcceptanceSuperseded : CancellationException("guest acceptance superseded")
