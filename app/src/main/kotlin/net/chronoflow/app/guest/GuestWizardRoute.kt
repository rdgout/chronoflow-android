package net.chronoflow.app.guest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher
import net.chronoflow.feature.wizard.shell.WizardRoot
import net.chronoflow.feature.wizard.ui.WizardInitialPresentationKey

@Composable
internal fun GuestWizardRoute(
    graph: AppGraph,
    revision: Long,
) {
    val factory =
        remember(graph, revision) {
            viewModelFactory {
                initializer {
                    val dependencies =
                        GuestWizardDependencies(
                            graph.guestWizardStore,
                            LazyPublicDataSource { graph.dataSources.public() },
                            graph.guestSecrets,
                            { graph.deviceIdentity.identity() },
                            { graph.controller.presence },
                            graph.dispatchers,
                            CoroutineScope(graph.appScope.coroutineContext + graph.dispatchers.io),
                            { graph.planClock.now },
                            GuestSignUpLauncher { onDismissed ->
                                LaunchPlatform.guestSignUpRequested()
                                onDismissed()
                            },
                            graph.copyCatalogs,
                        )
                    GuestWizardHolder(
                        dependencies,
                        GuestWizardExits(graph.rootNavigator, revision, {
                            graph.controller.presence
                        }, graph.guestWizardStore, graph.guestSecrets, graph.dispatchers),
                        graph.appScope,
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, holder, model) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event ==
                    Lifecycle.Event.ON_START
                ) {
                    holder.onStart(model)
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WizardRoot(model) { holder.close(model) }
}
