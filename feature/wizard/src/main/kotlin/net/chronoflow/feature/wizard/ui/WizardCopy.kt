package net.chronoflow.feature.wizard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable

@Composable
internal fun WizardCopy.resolve(): String {
    val entry = checkNotNull(CopyIndex.find(CopyTable.Wizard, key))
    return LocalResources.current.getString(entry.resId, *entry.args.map { arguments[it].orEmpty() }.toTypedArray())
}
