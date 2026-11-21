package net.chronoflow.feature.settings.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import net.chronoflow.core.model.auth.LockoutWait
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.settings.components.settingsText

@Composable
internal fun securityText(
    key: String,
    args: Map<String, String> = emptyMap(),
): String = settingsText(key, args)

@Composable
internal fun lockoutText(seconds: Int): String {
    val wait = LockoutWait(seconds)
    val entry = checkNotNull(CopyIndex.find(CopyTable.Settings, wait.localizationKey))
    return pluralStringResource(entry.resId, wait.count, wait.count)
}
