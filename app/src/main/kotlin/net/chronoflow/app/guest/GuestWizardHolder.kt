package net.chronoflow.app.guest

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val signup: GuestSignupCoordinator? = null,
) : ViewModel() {
    private var foreground: GuestWizardForeground? = null

    init {
        signup?.bind(viewModelScope)
    }

    fun close(model: TripWizardViewModel) {
        scope.launch { exits.close(model.wizard?.draft) { model.discard() } }
    }

    fun observeForeground(
        model: TripWizardViewModel,
        process: Lifecycle,
    ) {
        val observer =
            foreground ?: GuestWizardForeground {
                scope.launch { exits.onForeground(model.wizard?.draft) { model.discard() } }
            }.also { foreground = it }
        observer.observe(process)
    }

    override fun onCleared() {
        signup?.clear()
        foreground?.close()
    }
}
