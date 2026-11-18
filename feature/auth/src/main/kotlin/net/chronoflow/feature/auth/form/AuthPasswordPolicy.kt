package net.chronoflow.feature.auth.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable

object AuthPasswordPolicy {
    const val HINT_KEY =
        "Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. " +
            "Choose a password that has not appeared in a data leak."

    @Composable
    fun hint(): String = LocalResources.current.getString(checkNotNull(CopyIndex.find(CopyTable.Auth, HINT_KEY)).resId)
}
