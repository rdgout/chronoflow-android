package net.chronoflow.feature.wizard.returnchoice

import androidx.compose.runtime.Immutable
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.feature.wizard.ui.WizardCopy

@Immutable
data class WizardReturnChoiceUi(
    val kind: String,
    val title: WizardCopy,
    val hint: WizardCopy,
    val selected: Boolean,
    val enabled: Boolean,
)

@Immutable
data class WizardReturnUi(
    val oneWay: WizardReturnChoiceUi,
    val addReturn: WizardReturnChoiceUi,
) {
    val title: WizardCopy get() = WizardCopy("Coming back?")

    companion object {
        fun from(
            draft: MemberWizardDraft,
            choicePending: Boolean,
            enabled: Boolean = true,
        ): WizardReturnUi {
            val form = draft.form
            return WizardReturnUi(
                WizardReturnChoiceUi(
                    "one_way",
                    WizardCopy("One way"),
                    WizardCopy("Just this flight"),
                    form.end.kind == "one_way",
                    enabled && !choicePending,
                ),
                WizardReturnChoiceUi(
                    "return_legs",
                    WizardCopy("Add my return flight"),
                    WizardCopy("We plan the shift back home too"),
                    choicePending && form.end.kind == "return_legs",
                    enabled && !choicePending &&
                        (form.end.kind == "return_legs" || form.legs.size < WizardForm.maxFlights),
                ),
            )
        }
    }
}
