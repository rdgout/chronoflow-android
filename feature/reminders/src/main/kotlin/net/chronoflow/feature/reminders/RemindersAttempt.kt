package net.chronoflow.feature.reminders

import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException

internal suspend fun remindersAttempt(
    failed: (Throwable) -> Unit,
    block: suspend () -> Unit,
) {
    try {
        block()
    } catch (failure: CancellationException) {
        throw failure
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
