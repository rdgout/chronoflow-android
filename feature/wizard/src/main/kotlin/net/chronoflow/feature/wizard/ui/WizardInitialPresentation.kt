package net.chronoflow.feature.wizard.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewmodel.CreationExtras

@Immutable
data class WizardInitialPresentation(
    val query: String? = null,
    val discardDialogVisible: Boolean = false,
    val timeSheet: String? = null,
    val presentSignUp: Boolean = false,
)

object WizardInitialPresentationKey : CreationExtras.Key<WizardInitialPresentation>
