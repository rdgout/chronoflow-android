package net.chronoflow.app.wizard

import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation

interface WizardStartOverride {
    val presentation: WizardInitialPresentation

    fun dependencies(session: TripWizardDependencies): TripWizardDependencies
}

internal class WizardStartOverrides {
    var wizardStartOverride: WizardStartOverride? = null
}
