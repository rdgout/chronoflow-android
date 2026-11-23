package net.chronoflow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfCard(
    modifier: Modifier = Modifier,
    rows: Boolean = false,
    shadow: Boolean = true,
    border: Boolean = true,
    spacing: Dp = PlanUITokens.space4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CfTheme.colors
    val shape = if (rows) CfTheme.shapes.x2l else CfTheme.shapes.x3l
    CompositionLocalProvider(LocalContentColor provides c.cardForeground) {
        Column(
            modifier
                .fillMaxWidth()
                .then(if (shadow && !rows) Modifier.softShadow(shape, c.isDark) else Modifier)
                .background(c.card, shape)
                .then(if (border) Modifier.border(AuthTokens.borderWidth.dp, c.border, shape) else Modifier)
                .then(if (rows) Modifier.clip(shape) else Modifier)
                .padding(if (rows) 0.dp else PlanUITokens.space5.dp),
            verticalArrangement = Arrangement.spacedBy(if (rows) 0.dp else spacing),
            content = content,
        )
    }
}

@Composable
fun CfDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(
                AuthTokens.borderWidth.dp,
            ).background(CfTheme.colors.border)
            .clearAndSetSemantics {
            },
    )
}
