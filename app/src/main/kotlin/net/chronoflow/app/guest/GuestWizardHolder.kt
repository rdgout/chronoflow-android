package net.chronoflow.app.guest

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation

internal class GuestWizardHolder(
    val dependencies: TripWizardDependencies,
    val exits: GuestWizardExits,
    private val scope: CoroutineScope,
    val presentation: WizardInitialPresentation = WizardInitialPresentation(),
) : ViewModel() {
    private var started = false

    fun close(model: TripWizardViewModel) {
        scope.launch { exits.close(model.wizard?.draft) { model.discard() } }
    }

    fun onStart(model: TripWizardViewModel) {
        if (!started) {
            started = true
            return
        }
        scope.launch { exits.onForeground(model.wizard?.draft) { model.discard() } }
    }
}
