package net.chronoflow.feature.wizard.calculating

import androidx.compose.runtime.Immutable
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.feature.wizard.ready.ReadyUiMapper
import net.chronoflow.feature.wizard.ui.WizardCopy

@Immutable
data class CalculatingUi(
    val route: String,
    val days: Int,
) {
    val title: WizardCopy get() = WizardCopy("Calculating your plan…")
    val subtitle: WizardCopy get() = WizardCopy("This can take up to a minute. You can keep this screen open.")

    companion object {
        fun from(draft: MemberWizardDraft): CalculatingUi? =
            if (draft.state.step == MemberWizardStep.calculating) {
                CalculatingUi(
                    ReadyUiMapper.routeSummary(draft.form),
                    WizardPresentation.planDayCount(draft.state.review?.summary) ?: 0,
                )
            } else {
                null
            }
    }
}
