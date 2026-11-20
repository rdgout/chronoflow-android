package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardRoot
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import net.chronoflow.feature.wizard.ui.WizardInitialPresentationKey

@Composable
internal fun WizardOverlay(
    mode: WizardMode,
    deps: TripWizardDependencies?,
    presentation: WizardInitialPresentation?,
    onExit: (WizardExit) -> Unit,
) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val extras =
        remember(owner, deps, presentation) {
            MutableCreationExtras(
                (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty,
            ).apply {
                deps?.let { set(TripWizardDependenciesKey, it) }
                presentation?.let { set(WizardInitialPresentationKey, it) }
            }
        }
    WizardRoot(rememberOverlayViewModel(mode, TripWizardViewModel.factory(mode), extras), onExit)
}
