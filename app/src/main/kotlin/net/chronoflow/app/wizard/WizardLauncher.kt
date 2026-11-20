package net.chronoflow.app.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.trip.actions.TripWizardPrepareOutcome
import net.chronoflow.feature.trip.actions.TripWizardPreparer
import net.chronoflow.feature.wizard.TripWizardDependencies
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean

internal class WizardLauncher(
    private val overlay: () -> Route?,
    private val push: (Route) -> Unit,
    private val deps: TripWizardDependencies? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : TripWizardPreparer {
    constructor(overlay: () -> Route?, push: (Route) -> Unit) : this(overlay, push, null, Dispatchers.IO)

    private val pending = AtomicBoolean(false)

    fun open() {
        if (overlay() != null || !pending.compareAndSet(false, true)) return
        push(Route.Wizard(WizardMode.Create))
    }

    override suspend fun prepare(
        tripId: String,
        action: TripAction,
    ): TripWizardPrepareOutcome {
        val dependencies = deps ?: return TripWizardPrepareOutcome.Ignored
        if (overlay() != null || !pending.compareAndSet(false, true)) return TripWizardPrepareOutcome.Ignored
        return try {
            withContext(io) {
                dependencies.startTrip(
                    tripId,
                    if (action == TripAction.Edit) TripWizardAction.edit else TripWizardAction.duplicate,
                )
            }
            TripWizardPrepareOutcome.Ready
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            TripWizardPrepareOutcome.Failed(false)
        } catch (error: DataFailure) {
            TripWizardPrepareOutcome.Failed(error == DataFailure.Offline)
        } catch (_: SessionFailure) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: DeviceIdentityFailure) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: StoreFailure) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: RecoveryRejected) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: IOException) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: GeneralSecurityException) {
            TripWizardPrepareOutcome.Failed(false)
        } catch (_: RuntimeException) {
            TripWizardPrepareOutcome.Failed(false)
        } finally {
            pending.set(false)
        }
    }

    fun overlayChanged() {
        pending.set(false)
    }
}
