package net.chronoflow.feature.auth.shell

import android.content.res.Resources
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.auth.model.AuthText

fun AuthText.resolve(resources: Resources): String {
    val entry = checkNotNull(CopyIndex.find(CopyTable.Auth, key))
    return if (count == null) {
        resources.getString(entry.resId)
    } else if (entry.isPlural) {
        resources.getQuantityString(entry.resId, count.toInt(), count)
    } else {
        resources.getString(entry.resId, count)
    }
}
