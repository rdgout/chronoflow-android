package net.chronoflow.app.reminders

import android.content.ComponentName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.chronoflow.core.data.reminders.ReminderRuntime
import net.chronoflow.core.datastore.CorruptWipeMarkerException
import net.chronoflow.core.datastore.PermanentSessionKeyFailure
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StoredSession
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class AppReminderRuntime(
    override val io: CoroutineDispatcher,
    override val scope: CoroutineScope,
    override val notificationIcon: Int,
    private val component: () -> ComponentName,
    override val clock: () -> Instant,
    private val sessionStore: SessionStore,
    private val wipeMarker: suspend () -> WipeMarker,
    private val prepareBackground: suspend () -> Unit,
    private val startSessionGraphs: () -> Unit,
    private val session: suspend () -> SessionPresence,
    private val attachedAccount: StateFlow<String?>,
    private val reschedule: suspend () -> Unit,
    private val delivered: suspend (String, String) -> Unit,
    private val ensureChannel: suspend () -> Unit,
) : ReminderRuntime {
    override val tapComponent: ComponentName get() = component()

    override suspend fun storedAccountId(): String? =
        withContext(io) {
            try {
                if (wipeMarker().read() != null) return@withContext null
                val bytes = sessionStore.read() ?: return@withContext null
                StoredSession.parse(bytes)?.accountId?.lowercase(Locale.ROOT)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: CorruptWipeMarkerException) {
                null
            } catch (_: IOException) {
                null
            } catch (_: GeneralSecurityException) {
                null
            } catch (_: ProviderException) {
                null
            } catch (_: SecurityException) {
                null
            } catch (_: PermanentSessionKeyFailure) {
                null
            }
        }

    override suspend fun ensureChannel() = withContext(io) { ensureChannel.invoke() }

    override suspend fun delivered(
        accountId: String,
        identifier: String,
    ) = delivered.invoke(accountId, identifier)

    override suspend fun backgroundPass() {
        prepareBackground()
        startSessionGraphs()
        val presence = withTimeoutOrNull(20.seconds) { session() } ?: return
        val id = (presence as? SessionPresence.SignedIn)?.accountId?.lowercase(Locale.ROOT) ?: return
        withTimeoutOrNull(10.seconds) { attachedAccount.first { it == id } } ?: return
        reschedule()
    }
}
