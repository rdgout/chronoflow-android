package net.chronoflow.feature.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException

internal suspend fun settingsAttempt(
    failed: (Throwable) -> Unit,
    block: suspend () -> Unit,
) {
    try {
        block()
    } catch (failure: CancellationException) {
        if (!currentCoroutineContext().isActive) throw failure
        failed(failure)
    } catch (failure: SessionFailure) {
        failed(failure)
    } catch (failure: DataFailure) {
        failed(failure)
    } catch (failure: DeviceIdentityFailure) {
        failed(failure)
    } catch (failure: StoreFailure) {
        failed(failure)
    } catch (failure: LocalMutationFailure) {
        failed(failure)
    } catch (failure: IOException) {
        failed(failure)
    } catch (failure: GeneralSecurityException) {
        failed(failure)
    } catch (failure: SQLException) {
        failed(failure)
    } catch (failure: RuntimeException) {
        failed(failure)
    }
}
