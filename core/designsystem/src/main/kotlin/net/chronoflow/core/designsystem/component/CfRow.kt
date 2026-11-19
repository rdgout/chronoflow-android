package net.chronoflow.core.designsystem.component

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badge: String? = null,
    badgeDawn: Boolean = false,
    chevron: Boolean = true,
    lock: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
) {
    val c = CfTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SettingsTokens.rowHeight.dp)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
            .then(if (onClick == null) Modifier else Modifier.cfClickable(onClick, enabled = enabled))
            .semantics(mergeDescendants = true) { if (!enabled) disabled() }
            .padding(horizontal = PlanUITokens.space4.dp, vertical = SettingsTokens.rowVertical.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon !=
            null
        ) {
            Box(
                Modifier.size(SettingsTokens.rowDisc.dp).background(c.muted, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(AuthTokens.icon.dp), tint = c.cardForeground)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (destructive) c.destructive else c.cardForeground, style = CfTheme.type.settingsRow)
            if (subtitle != null) Text(subtitle, color = c.mutedForeground, style = CfTheme.type.screenBody)
        }
        if (badge != null) CfBadge(badge, variant = if (badgeDawn) CfBadgeVariant.Dawn else CfBadgeVariant.Secondary)
        if (lock) {
            Icon(
                LucideGlyphs.Lock.imageVector(),
                null,
                Modifier.size(AuthTokens.icon.dp).testTag("cf.row.lock"),
                tint = c.mutedForeground,
            )
        }
        if (chevron) {
            Icon(
                LucideGlyphs.ChevronRight.imageVector(),
                null,
                Modifier.size(PlanUITokens.space5.dp).testTag("cf.row.chevron"),
                tint = c.mutedForeground,
            )
        }
    }
}
