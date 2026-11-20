@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.OnboardingSessions

internal class AppOnboardingSessions(
    private val controller: SessionController,
    private val graphs: StateFlow<SessionGraph?>,
) : OnboardingSessions {
    override fun current(): Flow<OnboardingSession?> =
        combine(controller.updates, graphs) { _, graph -> graph }.mapLatest { graph ->
            val snapshot = controller.activeSession() ?: return@mapLatest null
            if (graph == null || graph.epoch != snapshot.epoch) return@mapLatest null
            val permit = controller.mutationPermit(snapshot) ?: return@mapLatest null
            OnboardingSession.from(controller, snapshot, permit, graph.outbox, graph.settings)
        }
}
