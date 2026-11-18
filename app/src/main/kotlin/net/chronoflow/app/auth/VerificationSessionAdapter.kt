package net.chronoflow.app.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppGraph
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.auth.verify.StoredVerificationAccount
import net.chronoflow.feature.auth.verify.VerificationSessionPort
import java.io.IOException
import java.security.GeneralSecurityException

class VerificationSessionAdapter(
    private val graph: AppGraph,
) : VerificationSessionPort {
    override suspend fun epoch(): Long = graph.controller.epoch

    override suspend fun presence() = graph.controller.presence

    override suspend fun activeSession() = graph.controller.activeSession()

    override suspend fun storedAccount(): StoredVerificationAccount? =
        withContext(graph.dispatchers.io) {
            try {
                val database = graph.sessionAccounts.activeAccountDatabase() ?: return@withContext null
                val account = AccountRepository(database).account() ?: return@withContext null
                StoredVerificationAccount(account.id, account.email, account.hasVerifiedEmail)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SessionFailure) {
                null
            } catch (_: DataFailure) {
                null
            } catch (_: DeviceIdentityFailure) {
                null
            } catch (_: StoreFailure) {
                null
            } catch (_: IOException) {
                null
            } catch (_: GeneralSecurityException) {
                null
            } catch (_: RuntimeException) {
                null
            }
        }

    override suspend fun source(snapshot: SessionSnapshot) = graph.dataSources.forSession(snapshot.session)

    override suspend fun confirm(
        expected: SessionSnapshot,
        email: String,
    ): Boolean =
        withContext(graph.dispatchers.io) {
            graph.sessionSync.stop()
            graph.controller.confirmEmailVerification(expected, email)
        }

    override suspend fun signOutDiscarding(expected: SessionSnapshot) {
        graph.controller.signOut(expected, discardPending = true)
    }

    override fun updates() = graph.controller.updates.filterNotNull()
}
