package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import kotlin.math.max

@Composable
fun AuthLinkRow(
    forgot: @Composable () -> Unit,
    signUp: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(content = {
        forgot()
        signUp()
    }, modifier = modifier.testTag("auth.links")) { measurables, constraints ->
        val gap = AuthTokens.fieldGap.dp.roundToPx()
        val horizontal = measurables.sumOf { it.maxIntrinsicWidth(constraints.maxHeight) } + gap <= constraints.maxWidth
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val height = if (horizontal) max(children[0].height, children[1].height) else children.sumOf { it.height }
        layout(width, height) {
            children[0].placeRelative(0, 0)
            children[1].placeRelative(
                if (horizontal) width - children[1].width else 0,
                if (horizontal) 0 else children[0].height,
            )
        }
    }
}

@Composable
fun AuthLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .heightIn(
                min = AuthMetrics.TOUCH.dp,
            ).alpha(
                if (enabled) 1f else AuthTokens.disabledOpacity,
            ).cfClickable(onClick, enabled = enabled)
            .padding(horizontal = AuthTokens.fieldGap.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            color = if (strong) CfTheme.colors.dawnText else CfTheme.colors.foreground,
            style = if (strong) CfTheme.type.authStrong else CfTheme.type.authLabel,
            textDecoration = TextDecoration.Underline,
        )
    }
}
