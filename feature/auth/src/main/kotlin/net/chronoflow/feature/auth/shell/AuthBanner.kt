package net.chronoflow.feature.auth.shell

import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthText

@Composable
fun AuthBanner(
    kind: AuthBannerKind,
    modifier: Modifier = Modifier,
    context: AuthFormContext = AuthFormContext.SignIn,
) {
    val resources = LocalContext.current.resources
    CfBanner(
        AuthText(kind.title(context)).resolve(resources),
        kind.body(context).takeIf { it.isNotEmpty() }?.let { AuthText(it).resolve(resources) },
        modifier,
        announce = false,
        testTag = "auth.banner",
    )
}

@Composable
internal fun AuthAnnouncer(
    revision: Int,
    take: () -> String?,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf<String?>(null) }
    var odd by remember { mutableStateOf(false) }
    val currentTake by rememberUpdatedState(take)
    LaunchedEffect(revision) {
        val announcement = currentTake() ?: return@LaunchedEffect
        odd = !odd
        text = announcement
    }
    Box(
        modifier.testTag("auth.announcement").semantics {
            liveRegion = LiveRegionMode.Polite
            text?.let { contentDescription = if (odd) it + "\u200B" else it }
        },
    )
}

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
