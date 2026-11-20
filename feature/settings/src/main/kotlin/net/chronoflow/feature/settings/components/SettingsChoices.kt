package net.chronoflow.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

data class SettingsOption(
    val id: String,
    val title: String,
    val hint: String,
)

@Composable
fun SettingsRadioCards(
    options: List<SettingsOption>,
    selection: String,
    testTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        options.forEach { option ->
            SettingsChoiceCard(
                option.title,
                option.hint,
                selection == option.id,
                "$testTag.${option.id}",
                { onSelect(option.id) },
            )
        }
    }
}

@Composable
fun SettingsChoiceCard(
    title: String,
    hint: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val shape = CfTheme.shapes.x2l
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SettingsTokens.pillHeight.dp)
            .testTag(testTag)
            .background(
                colors.card,
                shape,
            ).border(SettingsTokens.selectionWidth.dp, if (selected) colors.dawn else colors.border, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(PlanUITokens.space4.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((AuthTokens.borderWidth * 2).dp)) {
            Text(title, style = CfTheme.type.style(CfTypeScale.settingsStrong))
            Text(hint, style = CfTheme.type.screenBody, color = colors.mutedForeground)
        }
        Box(
            Modifier
                .padding(top = PlanUITokens.space1.dp)
                .size(PlanUITokens.space5.dp)
                .background(if (selected) colors.dawn else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .border(
                    SettingsTokens.selectionWidth.dp,
                    if (selected) colors.dawn else colors.mutedForeground.copy(alpha = AuthTokens.disabledOpacity),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    LucideGlyphs.Check.imageVector(),
                    null,
                    Modifier.size(PlanUITokens.space3.dp),
                    tint = colors.dawnForeground,
                )
            }
        }
    }
}
