package net.chronoflow.app.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.takeWhile
import net.chronoflow.core.data.guest.GuestContinuationRepository
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

class GuestContinuationRunner(
    private val values: () -> Flow<GuestSessionContinuation?>,
    private val consume: suspend (Gate) -> GuestSessionContinuation?,
    private val gate: suspend () -> Gate,
    private val isCurrent: suspend () -> Boolean,
    private val onTrip: (String) -> Unit,
    private val onTrips: () -> Unit,
    private val onMemberWizard: () -> Unit,
) {
    constructor(
        repository: GuestContinuationRepository,
        gate: suspend () -> Gate,
        isCurrent: suspend () -> Boolean,
        onTrip: (String) -> Unit,
        onTrips: () -> Unit,
        onMemberWizard: () -> Unit,
    ) : this(repository::values, repository::consume, gate, isCurrent, onTrip, onTrips, onMemberWizard)

    suspend fun run() {
        try {
            values().takeWhile { isCurrent() }.collect { value ->
                if (value == null) return@collect
                val currentGate = gate()
                if (!isCurrent()) return@collect
                when (val destination = consume(currentGate)) {
                    is GuestSessionContinuation.Trip -> onTrip(destination.tripId)
                    GuestSessionContinuation.Trips -> onTrips()
                    GuestSessionContinuation.MemberWizard -> onMemberWizard()
                    null -> Unit
                }
            }
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
        } catch (_: StoreFailure) {
        } catch (_: DataFailure) {
        } catch (_: SessionFailure) {
        } catch (_: IOException) {
        } catch (_: GeneralSecurityException) {
        } catch (_: RuntimeException) {
        }
    }
}
