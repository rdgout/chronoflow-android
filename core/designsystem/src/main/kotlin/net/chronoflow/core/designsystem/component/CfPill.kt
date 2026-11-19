package net.chronoflow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme

enum class CfBadgeVariant { Default, Secondary, Outline, Dawn }

@Composable
fun CfBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: CfBadgeVariant = CfBadgeVariant.Secondary,
) {
    val c = CfTheme.colors
    val container =
        when (variant) {
            CfBadgeVariant.Default -> c.primary
            CfBadgeVariant.Secondary -> c.secondary
            CfBadgeVariant.Outline -> Color.Transparent
            CfBadgeVariant.Dawn -> c.dawn
        }
    val ink =
        when (variant) {
            CfBadgeVariant.Default -> c.primaryForeground
            CfBadgeVariant.Secondary -> c.secondaryForeground
            CfBadgeVariant.Outline -> c.foreground
            CfBadgeVariant.Dawn -> c.dawnForeground
        }
    Text(
        text,
        modifier
            .ellipsizable()
            .background(container, CfTheme.shapes.full)
            .border(
                AuthTokens.borderWidth.dp,
                if (variant ==
                    CfBadgeVariant.Outline
                ) {
                    c.border
                } else {
                    Color.Transparent
                },
                CfTheme.shapes.full,
            ).padding(horizontal = PlanUITokens.space2.dp, vertical = (2 * AuthTokens.borderWidth).dp),
        color = ink,
        style = CfTheme.type.settingsBadge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun CfPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val c = CfTheme.colors
    val shape = CfTheme.shapes.full
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = SettingsTokens.pillHeight.dp)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .cfClickable(onClick, role = Role.RadioButton, focusShape = shape)
            .semantics { this.selected = selected }
            .background(
                c.card,
                shape,
            ).border(SettingsTokens.selectionWidth.dp, if (selected) c.dawn else c.border, shape)
            .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
        horizontalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) Icon(LucideGlyphs.Check.imageVector(), null, Modifier.size(AuthTokens.icon.dp), tint = c.dawnText)
        Text(
            text,
            Modifier.weight(1f, fill = false),
            color = c.foreground,
            style = if (selected) CfTheme.type.screenLabel else CfTheme.type.settingsPill,
        )
    }
}

data class CfPillOption(
    val id: String,
    val title: String,
)

@Composable
fun CfPillGroup(
    options: List<CfPillOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.selectableGroup().testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        options.forEach { option ->
            CfPill(option.title, option.id == selectedId, { onSelect(option.id) }, testTag = "$testTag.${option.id}")
        }
    }
}
