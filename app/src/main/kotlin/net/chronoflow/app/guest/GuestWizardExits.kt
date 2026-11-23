package net.chronoflow.app.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.RecoveryRejected
import java.io.IOException
import java.security.GeneralSecurityException

internal class GuestWizardExits(
    private val navigator: RootNavigator,
    private val revision: Long,
    private val presence: () -> SessionPresence?,
    private val store: GuestWizardStore,
    private val secrets: GuestSecretStore,
    private val dispatchers: AppDispatchers,
) {
    private val mutex = Mutex()
    private var handled = false

    private fun current(): Boolean =
        navigator.state.value.let {
            it.route == Route.GuestWizard && it.revision == revision && presence() == SessionPresence.SignedOut
        }

    suspend fun close(
        draft: MemberWizardDraft?,
        discard: suspend () -> Unit = {},
    ) {
        mutex.withLock { leave(draft, discard) }
    }

    suspend fun onForeground(
        draft: MemberWizardDraft?,
        discard: suspend () -> Unit,
    ) {
        mutex.withLock {
            if (handled || !current()) return
            var exists = true
            quietly { exists = store.recover() != null }
            if (!exists) leave(draft, discard)
        }
    }

    private suspend fun leave(
        draft: MemberWizardDraft?,
        discard: suspend () -> Unit,
    ) {
        if (handled || !current()) return
        handled = true
        withContext(dispatchers.main) { quietly(discard) }
        withContext(dispatchers.io) {
            draft?.let { quietly { store.discard(it.state.instance) } }
            draft
                ?.form
                ?.identity
                ?.token
                ?.let { token -> quietly { secrets.delete(token) } }
        }
        withContext(dispatchers.main) { if (current()) navigator.navigate(Route.Welcome) }
    }
}

internal suspend fun quietly(block: suspend () -> Unit) {
    try {
        block()
    } catch (_: CancellationException) {
        currentCoroutineContext().ensureActive()
    } catch (_: DataFailure) {
    } catch (_: SessionFailure) {
    } catch (_: DeviceIdentityFailure) {
    } catch (_: StoreFailure) {
    } catch (_: RecoveryRejected) {
    } catch (_: IOException) {
    } catch (_: GeneralSecurityException) {
    } catch (_: RuntimeException) {
    }
}
