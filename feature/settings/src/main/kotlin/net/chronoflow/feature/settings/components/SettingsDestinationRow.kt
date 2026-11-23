package net.chronoflow.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun SettingsDestinationRow(
    title: String,
    glyph: LucideGlyph,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    badgeTag: String = "$testTag.badge",
    enabled: Boolean = true,
    chevron: Boolean = enabled,
    singleLineSubtitle: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .testTag(testTag)
            .heightIn(min = SettingsTokens.rowHeight.dp)
            .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
            .cfClickable(onClick, enabled = enabled)
            .padding(horizontal = PlanUITokens.space4.dp, vertical = SettingsTokens.rowVertical.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(SettingsTokens.rowDisc.dp).background(CfTheme.colors.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(glyph.imageVector(), null, Modifier.size(PlanUITokens.space4.dp), tint = CfTheme.colors.foreground)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = CfTheme.type.settingsRow)
            if (subtitle != null) {
                Text(
                    subtitle,
                    Modifier
                        .testTag("$testTag.subtitle")
                        .then(if (singleLineSubtitle) Modifier.ellipsizable() else Modifier),
                    style = CfTheme.type.screenBody,
                    color = CfTheme.colors.mutedForeground,
                    maxLines = if (singleLineSubtitle) 1 else Int.MAX_VALUE,
                    overflow = if (singleLineSubtitle) TextOverflow.Ellipsis else TextOverflow.Clip,
                )
            }
            if (badge != null) {
                Box(Modifier.padding(top = SettingsTokens.pillGap.dp)) {
                    CfBadge(badge, Modifier.testTag(badgeTag), variant = CfBadgeVariant.Dawn)
                }
            }
        }
        if (chevron) {
            Icon(
                LucideGlyphs.ChevronRight.imageVector(),
                null,
                Modifier.size(PlanUITokens.space5.dp).testTag("$testTag.chevron"),
                tint = CfTheme.colors.mutedForeground,
            )
        }
    }
}
