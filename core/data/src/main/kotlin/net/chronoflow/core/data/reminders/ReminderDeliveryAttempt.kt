package net.chronoflow.core.data.reminders

import android.database.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import net.chronoflow.core.datastore.PermanentSessionKeyFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

internal suspend fun attemptReminderDelivery(deliver: suspend () -> Unit) {
    try {
        deliver()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
    } catch (_: GeneralSecurityException) {
    } catch (_: ProviderException) {
    } catch (_: SecurityException) {
    } catch (_: PermanentSessionKeyFailure) {
    } catch (_: StoreFailure) {
    } catch (_: SQLiteException) {
    }
}
