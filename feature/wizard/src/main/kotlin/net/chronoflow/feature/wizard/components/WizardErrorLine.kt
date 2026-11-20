@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardMetrics

@Composable
internal fun WizardErrorLine(
    body: String,
    tag: String,
) {
    Row(
        Modifier.testTag(tag).semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.wizardControlGap.dp),
    ) {
        Icon(
            LucideGlyphs.CircleAlert.imageVector(),
            null,
            Modifier.padding(top = WizardMetrics.errorGlyphTop.dp).size(WizardMetrics.errorGlyph.dp),
            tint = CfTheme.colors.destructive,
        )
        Text(
            body,
            Modifier.weight(1f),
            color = CfTheme.colors.destructive,
            style = CfTheme.type.style(CfTypeScale.screenBody),
        )
    }
}
