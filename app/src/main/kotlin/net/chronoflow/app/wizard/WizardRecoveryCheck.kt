package net.chronoflow.app.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.wizard.model.RecoveryRejected
import java.io.IOException
import java.security.GeneralSecurityException

internal class WizardRecoveryCheck(
    private val dispatchers: AppDispatchers,
    private val recover: suspend () -> Boolean,
    private val captureSeed: () -> String?,
) {
    private val mutex = Mutex()
    private val checkedEpochs = mutableSetOf<Long>()

    suspend fun shouldOpen(epoch: Long): Boolean =
        withContext(dispatchers.io) {
            mutex.withLock {
                if (captureSeed() != null || !checkedEpochs.add(epoch)) return@withLock false
                try {
                    recover()
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    failed()
                } catch (_: DataFailure) {
                    failed()
                } catch (_: SessionFailure) {
                    failed()
                } catch (_: DeviceIdentityFailure) {
                    failed()
                } catch (_: StoreFailure) {
                    failed()
                } catch (_: RecoveryRejected) {
                    failed()
                } catch (_: IOException) {
                    failed()
                } catch (_: GeneralSecurityException) {
                    failed()
                } catch (_: RuntimeException) {
                    failed()
                }
            }
        }

    private fun failed(): Boolean {
        java.util.logging.Logger
            .getLogger("Wizard")
            .warning("Wizard recovery could not be checked")
        return false
    }
}
