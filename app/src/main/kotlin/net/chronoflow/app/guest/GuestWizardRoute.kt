package net.chronoflow.app.guest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import net.chronoflow.app.AppGraph
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.shell.WizardRoot
import net.chronoflow.feature.wizard.ui.WizardInitialPresentationKey

@Composable
internal fun GuestWizardRoute(
    graph: AppGraph,
    revision: Long,
) {
    val locale = LocalResources.current.configuration.locales[0]
    val factory =
        remember(graph, revision, locale) {
            viewModelFactory {
                initializer {
                    val signup =
                        GuestSignupCoordinator(
                            graph.auth,
                            graph.rootNavigator,
                            revision,
                            LaunchPlatform.guestSignupSeed,
                            LaunchPlatform.guestSignupPrefill,
                        )
                    val dependencies =
                        GuestWizardDependencies(
                            graph.guestWizardStore,
                            LazyPublicDataSource { graph.dataSources.public() },
                            graph.guestSecrets,
                            { graph.deviceIdentity.identity() },
                            { graph.controller.presence },
                            graph.dispatchers,
                            CoroutineScope(graph.appScope.coroutineContext + graph.dispatchers.io),
                            { LaunchPlatform.guestWizardInstant ?: graph.planClock.now },
                            signup,
                            graph.copyCatalogs,
                            seed =
                                if (LaunchPlatform.guestWizardSeedEnabled) {
                                    { LaunchPlatform.seedGuestWizard(it, locale, graph.planClock.now) }
                                } else {
                                    null
                                },
                            uiOverride = LaunchPlatform.guestWizardUiOverride,
                        )
                    GuestWizardHolder(
                        dependencies,
                        GuestWizardExits(graph.rootNavigator, revision, {
                            graph.controller.presence
                        }, graph.guestWizardStore, graph.guestSecrets, graph.dispatchers),
                        graph.appScope,
                        LaunchPlatform.guestWizardPresentation,
                        signup,
                    )
                }
            }
        }
    val holder: GuestWizardHolder = viewModel(key = "guestWizardHolder:$revision", factory = factory)
    GuestWizardRoute(holder, revision)
}

@Composable
internal fun GuestWizardRoute(
    holder: GuestWizardHolder,
    revision: Long,
) {
    val extras =
        remember(holder) {
            MutableCreationExtras().apply {
                set(TripWizardDependenciesKey, holder.dependencies)
                set(WizardInitialPresentationKey, holder.presentation)
            }
        }
    val model: TripWizardViewModel =
        viewModel(
            key = "guestWizard:$revision",
            factory = TripWizardViewModel.factory(WizardMode.Create),
            extras = extras,
        )
    LaunchedEffect(holder, model) {
        holder.observeForeground(model, ProcessLifecycleOwner.get().lifecycle)
    }
    WizardRoot(model) { holder.close(model) }
    holder.signup?.let { coordinator ->
        val shown by coordinator.presentation.collectAsStateWithLifecycle()
        shown?.let { presentation -> key(presentation.id) { GuestSignupRoute(presentation, coordinator) } }
    }
}
