@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Stable
class PoliteAnnouncer {
    internal var text by mutableStateOf("")
        private set
    private var alternate = false

    fun announce(value: String) {
        alternate = !alternate
        text = value + if (alternate) "" else "\u200B"
    }
}

val LocalPoliteAnnouncer = staticCompositionLocalOf<PoliteAnnouncer?> { null }

@Composable
fun PoliteAnnouncerHost(
    announcer: PoliteAnnouncer,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(0.dp).testTag("plan.announcer").semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = announcer.text
        },
    )
}
