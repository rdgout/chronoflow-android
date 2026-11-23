package net.chronoflow.feature.wizard.excluded

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.feature.wizard.ui.WizardCopy

enum class ExcludedNote { Default, Saved, Edit }

@Immutable
data class WizardExcludedUi(
    val reasons: List<CopyLine>,
    val note: ExcludedNote,
) {
    val title: WizardCopy get() = WizardCopy("No plan available")
    val noteCopy: WizardCopy get() =
        WizardCopy(
            when (note) {
                ExcludedNote.Edit -> "Your trip stays as it was."
                ExcludedNote.Saved -> "You gave this answer on an earlier trip. Change it to plan this trip."
                ExcludedNote.Default -> "If you answered by mistake, you can change your answers."
            },
        )
}
