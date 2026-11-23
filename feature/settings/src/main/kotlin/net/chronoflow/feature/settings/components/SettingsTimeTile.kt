package net.chronoflow.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import kotlin.math.max

@Composable
fun SettingsTimeTile(
    label: String,
    value: String?,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = CfTheme.shapes.x2l
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = SettingsTokens.timeHeight.dp)
            .background(CfTheme.colors.surface2, shape)
            .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape)
            .cfClickable(onClick, role = Role.Button, focusShape = shape)
            .semantics(mergeDescendants = true) {}
            .testTag(testTag)
            .padding(horizontal = PlanUITokens.space4.dp, vertical = SettingsTokens.timeVertical.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
    ) {
        Text(label, style = CfTheme.type.screenBody, color = CfTheme.colors.mutedForeground)
        Text(
            value ?: stringResource(R.string.settings_not_set),
            style = CfTheme.type.style(CfTypeScale.settingsTime).copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
fun SettingsTimeTilePair(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box { first() }
            Box { second() }
        },
        modifier = modifier.fillMaxWidth(),
    ) { children, constraints ->
        val gap = PlanUITokens.space3.dp.roundToPx()
        val width = constraints.maxWidth
        val columns = (width - gap) / 2 >= (PlanUITokens.space16 * 2).dp.roundToPx()
        val tileWidth = if (columns) (width - gap) / 2 else width
        val first = children[0].measure(Constraints.fixedWidth(tileWidth))
        val second = children[1].measure(Constraints.fixedWidth(if (columns) width - gap - tileWidth else width))
        val height = if (columns) max(first.height, second.height) else first.height + gap + second.height
        layout(width, height) {
            first.placeRelative(0, 0)
            second.placeRelative(if (columns) first.width + gap else 0, if (columns) 0 else first.height + gap)
        }
    }
}
