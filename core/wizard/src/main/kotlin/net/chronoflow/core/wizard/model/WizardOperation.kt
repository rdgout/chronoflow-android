package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

internal suspend fun wizardOperation(
    operation: suspend () -> Unit,
    failed: (DataFailure) -> Unit,
) {
    try {
        operation()
    } catch (failure: CancellationException) {
        currentCoroutineContext().ensureActive()
        failed(DataFailure.Server(null))
    } catch (failure: DataFailure) {
        failed(failure)
    } catch (_: SessionFailure) {
        failed(DataFailure.Server(null))
    } catch (_: DeviceIdentityFailure) {
        failed(DataFailure.Server(null))
    } catch (_: StoreFailure) {
        failed(DataFailure.Server(null))
    } catch (_: IOException) {
        failed(DataFailure.Server(null))
    } catch (_: GeneralSecurityException) {
        failed(DataFailure.Server(null))
    } catch (_: RuntimeException) {
        failed(DataFailure.Server(null))
    }
}
