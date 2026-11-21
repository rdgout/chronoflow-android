package net.chronoflow.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.component.CfPillGroup
import net.chronoflow.core.designsystem.component.CfPillOption
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable

@Composable
internal fun settingsText(key: String): String =
    CopyIndex.find(CopyTable.Settings, key)?.let { stringResource(it.resId) } ?: key

@Composable
fun SettingsPills(
    title: String,
    options: List<CfPillOption>,
    selection: String,
    testTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(title, style = CfTheme.type.style(CfTypeScale.settingsRow))
        CfPillGroup(options, selection, onSelect, testTag)
    }
}

@Composable
fun SettingsEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    dawn: Boolean = false,
) {
    val locale = LocalResources.current.configuration.locales[0]
    Text(
        text.uppercase(locale),
        modifier.semantics { heading() },
        style = CfTheme.type.style(if (dawn) CfTypeScale.screenEyebrow else CfTypeScale.settingsEyebrow),
        color = if (dawn) CfTheme.colors.dawnText else CfTheme.colors.mutedForeground,
    )
}

@Composable
fun SettingsError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Icon(
            LucideGlyphs.CircleAlert.imageVector(),
            null,
            Modifier.size(AuthTokens.icon.dp),
            tint = CfTheme.colors.destructive,
        )
        Text(text, style = CfTheme.type.screenBody, color = CfTheme.colors.destructive)
    }
}

@Composable
fun SettingsSavedLabel(
    group: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.semantics(mergeDescendants = true) {}.testTag("settings.bodyClock.saved.$group"),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            LucideGlyphs.Check.imageVector(),
            null,
            Modifier.size(SettingsTokens.timeVertical.dp),
            tint = CfTheme.colors.dawnText,
        )
        Text(
            stringResource(R.string.settings_saved),
            style = CfTheme.type.style(CfTypeScale.screenMeta),
            color = CfTheme.colors.mutedForeground,
        )
    }
}

@Composable
fun SettingsQuestion(
    title: String,
    missing: Boolean,
    anchorKey: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = CfTheme.colors
    val shape = CfTheme.shapes.x2l
    val box =
        if (missing) {
            Modifier
                .layout { measurable, constraints ->
                    val outset = PlanUITokens.space3.dp.roundToPx()
                    val child = measurable.measure(constraints.offset(horizontal = 2 * outset))
                    layout(constraints.constrainWidth(child.width - 2 * outset), child.height) {
                        child.placeRelative(-outset, 0)
                    }
                }.background(colors.settingsWarningBackground, shape)
                .border(AuthTokens.borderWidth.dp, colors.settingsWarningBorder, shape)
                .padding(PlanUITokens.space3.dp)
        } else {
            Modifier
        }
    Column(
        modifier.testTag("$anchorKey.question").then(box),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        Text(title, Modifier.testTag("$anchorKey.title"), style = CfTheme.type.style(CfTypeScale.settingsRow))
        if (missing) {
            CfBadge(
                stringResource(R.string.settings_needs_an_answer),
                Modifier.testTag("$anchorKey.missing"),
                variant = CfBadgeVariant.Dawn,
            )
        }
        if (hint != null) Text(hint, style = CfTheme.type.screenBody, color = colors.mutedForeground)
        Column(Modifier.fillMaxWidth().testTag("$anchorKey.content")) { content() }
        if (error != null) SettingsError(error, Modifier.testTag("$anchorKey.error"))
    }
}

@Composable
fun SettingsPreferenceSwitch(
    title: String,
    hint: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().cfToggleable(checked, onChange).testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
            Text(title, style = CfTheme.type.style(CfTypeScale.settingsRow))
            Text(hint, style = CfTheme.type.screenBody, color = CfTheme.colors.mutedForeground)
        }
        CfSwitch(checked, null)
    }
}
