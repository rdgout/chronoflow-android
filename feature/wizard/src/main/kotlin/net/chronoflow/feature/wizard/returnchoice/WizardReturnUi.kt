package net.chronoflow.feature.wizard.returnchoice

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardPresentation
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
    val web: WizardReturnChoiceUi? = null,
) {
    val title: WizardCopy get() = WizardCopy("Coming back?")

    companion object {
        fun from(
            draft: MemberWizardDraft,
            choicePending: Boolean,
            enabled: Boolean = true,
            language: Language = Language.En,
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
                    WizardCopy(
                        if (draft.state.edit != null && draft.state.originalForm
                                ?.end
                                ?.kind != "return_legs"
                        ) {
                            "We plan the shift back home too · 1 credit"
                        } else {
                            "We plan the shift back home too"
                        },
                    ),
                    choicePending && form.end.kind == "return_legs",
                    enabled && !choicePending &&
                        (form.end.kind == "return_legs" || form.legs.size < WizardForm.maxFlights),
                ),
                web =
                    if (form.end.kind in setOf("nights", "check_out")) {
                        WizardReturnChoiceUi(
                            form.end.kind,
                            webTitle(draft, language),
                            WizardCopy("Set on the website. Keep it, or add your flight home."),
                            selected = true,
                            enabled = enabled && !choicePending,
                        )
                    } else {
                        null
                    },
            )
        }

        private fun webTitle(
            draft: MemberWizardDraft,
            language: Language,
        ): WizardCopy {
            if (draft.form.end.kind == "nights") {
                return WizardCopy(
                    "Ends after {{count}} nights",
                    mapOf("count" to (draft.form.end.nights ?: 0).toString()),
                )
            }
            var copy = WizardCopy("One way")
            WizardPresentation.returnSummary(
                draft.form,
                draft.state.review
                    ?.summary
                    ?.stayEnd ?: draft.state.snapshotStayEnd,
                language,
            ) { key, args ->
                copy = WizardCopy(key, args)
                ""
            }
            return copy
        }
    }
}
