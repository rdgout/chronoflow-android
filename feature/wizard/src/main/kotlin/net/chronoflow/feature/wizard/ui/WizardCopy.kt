package net.chronoflow.feature.wizard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable

@Composable
internal fun WizardCopy.resolve(): String {
    val resources = LocalResources.current
    val entry =
        CopyIndex.find(CopyTable.Wizard, key)
            ?: return resources.getString(R.string.wizard_something_went_wrong_on_our_side_t_208515)
    val count = arguments["count"] ?: arguments["%1"]
    val values =
        entry.args
            .map { argument ->
                if (argument.startsWith(
                        "%",
                    )
                ) {
                    (arguments[argument] ?: count)?.toIntOrNull() ?: 0
                } else {
                    arguments[argument].orEmpty()
                }
            }.toTypedArray<Any>()
    return if (entry.isPlural) {
        resources.getQuantityString(entry.resId, if (count == "1") 1 else 2, *values)
    } else {
        resources.getString(entry.resId, *values)
    }
}

internal fun FieldMessage.wizardCopy(): WizardCopy = WizardCopy(key, arguments.mapValues { it.value.content })
