package net.chronoflow.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.app.AppGraph
import net.chronoflow.feature.onboarding.OnboardingMainKey
import net.chronoflow.feature.onboarding.OnboardingSessionsKey
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughRoute
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughViewModel

@Composable
internal fun onboardingExtras(graph: AppGraph): CreationExtras {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
    val sessions = graph.onboardingSessions
    return remember(owner, sessions, graph) {
        MutableCreationExtras(owner.defaultViewModelCreationExtras).apply {
            set(OnboardingSessionsKey, sessions)
            set(OnboardingMainKey, graph.dispatchers.main)
        }
    }
}

@Composable
internal fun WalkthroughEntry(
    graph: AppGraph,
    replay: Boolean,
    onFinish: () -> Unit = {},
) {
    val factory = remember(replay) { WalkthroughViewModel.factory(replay) }
    WalkthroughRoute(viewModel(factory = factory, extras = onboardingExtras(graph)), onFinish)
}
