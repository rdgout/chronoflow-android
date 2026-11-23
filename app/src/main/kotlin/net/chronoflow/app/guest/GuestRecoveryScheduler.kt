package net.chronoflow.app.guest

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.wizard.model.GuestRecoveryGate.Decision
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.feature.launch.model.LaunchIntro
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class GuestRecoveryScheduler(
    private val dispatchers: AppDispatchers,
    private val store: GuestWizardStore,
    private val navigator: RootNavigator,
    private val presence: suspend () -> SessionPresence?,
    private val epoch: () -> Long?,
    private val allowed: () -> Boolean = { true },
) {
    private val checked = AtomicBoolean()

    suspend fun recover(
        handoffFinished: Boolean,
        intro: LaunchIntro?,
    ) {
        if (!handoffFinished || intro?.frozenTime != null || !allowed()) return
        if (navigator.state.value.route != Route.Welcome) return
        val revision = navigator.state.value.revision
        val startedEpoch = epoch()

        fun current(): Boolean =
            navigator.state.value.let {
                it.route == Route.Welcome && it.revision == revision && epoch() == startedEpoch
            }
        if (presence() == null || !current() || !checked.compareAndSet(false, true)) return
        quietly {
            intro?.let { it.clock.sleepUntil(it.start + 900.milliseconds) }
            currentCoroutineContext().ensureActive()
            if (!current() || presence() != SessionPresence.SignedOut) return@quietly
            val decision = withContext(dispatchers.io) { store.recoveryDecision(true, true) }
            withContext(dispatchers.main) {
                currentCoroutineContext().ensureActive()
                if (current() && presence() == SessionPresence.SignedOut) {
                    when (decision) {
                        Decision.wizard -> {
                            navigator.navigate(Route.GuestWizard)
                        }

                        Decision.signIn -> {
                            navigator.navigate(Route.SignIn)
                        }

                        Decision.none, Decision.wait, Decision.expire -> {}
                    }
                }
            }
        }
    }
}
