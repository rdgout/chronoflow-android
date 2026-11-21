package net.chronoflow.feature.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import kotlin.math.max

@Composable
fun SettingsDrinkCounter(
    counts: Map<String, Int>,
    onChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(
            stringResource(R.string.settings_what_do_you_drink_on_a_normal_day),
            style = CfTheme.type.style(CfTypeScale.settingsRow),
        )
        Column(Modifier.border(AuthTokens.borderWidth.dp, CfTheme.colors.border, CfTheme.shapes.x2l)) {
            val drinks =
                listOf(
                    "cupsFilterCoffee" to R.string.settings_coffee,
                    "cupsTea" to R.string.settings_tea,
                    "cupsCola" to R.string.settings_soft_drinks,
                    "cupsEnergyDrink" to R.string.settings_energy_drinks,
                )
            drinks.forEachIndexed { index, (key, title) ->
                if (index > 0) CfDivider()
                DrinkRow(stringResource(title), counts[key] ?: 0, key, { onChange(key, it) })
            }
        }
    }
}

@Composable
private fun DrinkRow(
    title: String,
    count: Int,
    key: String,
    onChange: (Int) -> Unit,
) {
    val locale = LocalResources.current.configuration.locales[0]
    val fewer = stringResource(R.string.settings_fewer_drink, title.lowercase(locale))
    val more = stringResource(R.string.settings_more_drink, title.lowercase(locale))
    Layout(
        content = {
            Text(title, style = CfTheme.type.style(CfTypeScale.screenBodyLg))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CounterButton(false, count > 0, fewer, "$key.fewer") { onChange(count - 1) }
                Text(
                    stringResource(R.string.settings_count_a_day, count.toString()),
                    Modifier.widthIn(min = PlanUITokens.space16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                    style = CfTheme.type.screenBody.copy(fontFeatureSettings = "tnum"),
                )
                CounterButton(true, count < 20, more, "$key.more") { onChange(count + 1) }
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsTokens.counterHeight.dp)
                .testTag("settings.bodyClock.$key")
                .padding(start = PlanUITokens.space4.dp, end = PlanUITokens.space2.dp)
                .padding(vertical = PlanUITokens.space2.dp),
    ) { children, constraints ->
        val gap = PlanUITokens.space1.dp.roundToPx()
        val controls = children[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val remaining = (constraints.maxWidth - controls.width - gap).coerceAtLeast(0)
        val stacked = children[0].minIntrinsicWidth(Constraints.Infinity) > remaining
        val label = children[0].measure(Constraints(maxWidth = if (stacked) constraints.maxWidth else remaining))
        val height = if (stacked) label.height + gap + controls.height else max(label.height, controls.height)
        layout(constraints.maxWidth, constraints.constrainHeight(height)) {
            label.placeRelative(0, if (stacked) 0 else (height - label.height) / 2)
            controls.placeRelative(
                constraints.maxWidth - controls.width,
                if (stacked) label.height + gap else (height - controls.height) / 2,
            )
        }
    }
}

@Composable
private fun CounterButton(
    increase: Boolean,
    enabled: Boolean,
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    val shape = CfTheme.shapes.full
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(AuthTokens.touch.dp)
            .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
            .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape)
            .cfClickable(onClick, enabled = enabled, role = Role.Button, focusShape = shape)
            .semantics { this.contentDescription = label }
            .testTag("settings.bodyClock.$tag"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            (if (increase) LucideGlyphs.Plus else LucideGlyphs.Minus).imageVector(),
            null,
            Modifier.size(AuthTokens.icon.dp),
            tint = CfTheme.colors.foreground,
        )
    }
}
