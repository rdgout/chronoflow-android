package net.chronoflow.feature.wizard.shell

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.core.wizard.model.MemberWizardStep

@Serializable
data class WizardStepKey(
    val step: MemberWizardStep,
    val questionIndex: Int? = null,
) : NavKey {
    val debugName: String get() = if (step == MemberWizardStep.question) "question${questionIndex ?: 0}" else step.name

    companion object {
        val Sleep = WizardStepKey(MemberWizardStep.sleep)
        val Outbound = WizardStepKey(MemberWizardStep.outbound)
        val ReturnChoice = WizardStepKey(MemberWizardStep.returnChoice)
        val ReturnFlight = WizardStepKey(MemberWizardStep.returnFlight)
        val Where = WizardStepKey(MemberWizardStep.whereTo)
        val ReturnRoute = WizardStepKey(MemberWizardStep.returnRoute)

        fun from(destination: MemberWizardDestination) =
            WizardStepKey(destination.step, (destination as? MemberWizardDestination.question)?.index)
    }
}

enum class WizardExit { Trips }
