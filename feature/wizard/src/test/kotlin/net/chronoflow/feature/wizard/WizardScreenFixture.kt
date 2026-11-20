@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.Composable
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.ui.WizardUiState

internal fun screenState(draft: MemberWizardDraft = routeDraft()): WizardUiState {
    val home = draft.state.step == MemberWizardStep.returnRoute
    val path =
        if (home) {
            listOf(
                MemberWizardDestination.outbound,
                MemberWizardDestination.returnChoice,
                MemberWizardDestination.returnRoute,
            )
        } else if (draft.state.step == MemberWizardStep.outbound) {
            listOf(MemberWizardDestination.outbound)
        } else {
            emptyList()
        }
    val progress =
        if (home) {
            0.50
        } else if (draft.state.step ==
            MemberWizardStep.outbound
        ) {
            0.30
        } else if (draft.form.legs[0]
                .fromIata
                .isEmpty()
        ) {
            0.08
        } else {
            0.15
        }
    return WizardUiState.from(TripWizardUiState.Active(MemberWizardSnapshot(draft), progress, path))
}

@Composable
internal fun WizardTestHost(
    profile: DeviceProfile = DeviceProfiles.pixel8,
    content: @Composable () -> Unit,
) {
    ProfileHost(profile) { ChronoFlowTheme(darkTheme = false, reduceMotion = true, content = content) }
}
