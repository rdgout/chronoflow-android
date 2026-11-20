package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.feature.wizard.ui.WizardUiState

internal fun readyScreen(
    review: ApiTripReview = generationReview(),
    reviewing: Boolean = false,
): WizardUiState = WizardUiState.from(readyActive(review, reviewing).copy(path = listOf(MemberWizardDestination.ready)))
