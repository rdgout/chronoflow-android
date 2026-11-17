package net.chronoflow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme

// The heights are the bundle Button size variants; the padding remains the iOS AuthButton padding.
private enum class CfButtonMetrics(
    val height: Float,
    val horizontal: Float,
) {
    Sm(PlanUITokens.space8, PlanUITokens.space3),
    Default(ButtonMetrics.DEFAULT_HEIGHT, PlanUITokens.space4),
    Lg(PlanUITokens.walkthroughDoneHeight, PlanUITokens.space6),
    Icon(ButtonMetrics.DEFAULT_HEIGHT, 0f),
    Touch(AuthTokens.touch, AuthTokens.spacer),
}

@net.chronoflow.core.designsystem.layout.BoardMetrics("ItemDetail", "SheetStates")
private object ButtonMetrics {
    const val DEFAULT_HEIGHT = 36f
}

enum class CfButtonStyle { Primary, Secondary, Outline, Ghost, Destructive, Dawn, OnSky }

enum class CfButtonSize { Sm, Default, Lg, Icon, Touch }

@Composable
fun CfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CfButtonStyle = CfButtonStyle.Primary,
    size: CfButtonSize = CfButtonSize.Default,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingDescription: String? = null,
    leadingIcon: ImageVector? = null,
    fillWidth: Boolean = false,
    minHeight: Dp = Dp.Unspecified,
    textStyle: TextStyle = CfTheme.type.authLabel,
    contentPadding: PaddingValues? = null,
) {
    require(!loading || loadingDescription != null) { "A loading button needs a localized loadingDescription" }
    val c = CfTheme.colors
    val shape = CfTheme.shapes.full
    val metrics = CfButtonMetrics.entries[size.ordinal]
    val container =
        when (style) {
            CfButtonStyle.Primary -> c.primary
            CfButtonStyle.Secondary -> c.secondary
            CfButtonStyle.Outline -> c.background
            CfButtonStyle.Ghost -> Color.Transparent
            CfButtonStyle.Destructive -> c.destructive
            CfButtonStyle.Dawn -> c.dawn
            CfButtonStyle.OnSky -> c.skyCopyBackground
        }
    val ink =
        when (style) {
            CfButtonStyle.Primary -> c.primaryForeground
            CfButtonStyle.Secondary -> c.secondaryForeground
            CfButtonStyle.Destructive -> c.destructiveForeground
            CfButtonStyle.Dawn -> c.dawnForeground
            CfButtonStyle.OnSky -> c.skyCopy
            else -> c.foreground
        }
    val active = enabled && !loading
    CompositionLocalProvider(LocalContentColor provides ink) {
        Row(
            modifier
                .minimumInteractiveComponentSize()
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = if (minHeight == Dp.Unspecified) metrics.height.dp else minHeight)
                .alpha(if (active) 1f else AuthTokens.disabledOpacity)
                .cfClickable(onClick, enabled = active, focusShape = shape)
                .semantics {
                    if (!active) disabled()
                    if (loading) stateDescription = requireNotNull(loadingDescription)
                }.background(container, shape)
                .then(
                    if (style ==
                        CfButtonStyle.Outline
                    ) {
                        Modifier.border(AuthTokens.borderWidth.dp, c.input, shape)
                    } else {
                        Modifier
                    },
                ).padding(
                    contentPadding
                        ?: PaddingValues(horizontal = metrics.horizontal.dp, vertical = AuthTokens.fieldGap.dp),
                ),
            horizontalArrangement = Arrangement.spacedBy(AuthTokens.fieldGap.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) CfSpinner()
            if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(AuthTokens.icon.dp))
            Text(text, Modifier.weight(1f, fill = false), color = ink, style = textStyle, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CfIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CfButtonStyle = CfButtonStyle.Ghost,
    size: CfButtonSize = CfButtonSize.Touch,
    enabled: Boolean = true,
) {
    val c = CfTheme.colors
    val shape = CfTheme.shapes.full
    val container =
        when (style) {
            CfButtonStyle.Primary -> c.primary
            CfButtonStyle.Secondary -> c.secondary
            CfButtonStyle.Outline -> c.background
            CfButtonStyle.Destructive -> c.destructive
            CfButtonStyle.Dawn -> c.dawn
            CfButtonStyle.OnSky -> c.skyCopyBackground
            CfButtonStyle.Ghost -> Color.Transparent
        }
    val ink =
        when (style) {
            CfButtonStyle.Primary -> c.primaryForeground
            CfButtonStyle.Secondary -> c.secondaryForeground
            CfButtonStyle.Destructive -> c.destructiveForeground
            CfButtonStyle.Dawn -> c.dawnForeground
            CfButtonStyle.OnSky -> c.skyCopy
            else -> c.foreground
        }
    androidx.compose.foundation.layout.Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(CfButtonMetrics.entries[size.ordinal].height.dp)
            .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
            .cfClickable(onClick, enabled = enabled, focusShape = shape)
            .background(container, shape)
            .then(
                if (style ==
                    CfButtonStyle.Outline
                ) {
                    Modifier.border(AuthTokens.borderWidth.dp, c.input, shape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(AuthTokens.icon.dp), tint = ink) }
}

@Composable
fun CfSpinner(
    modifier: Modifier = Modifier,
    size: Dp = AuthTokens.icon.dp,
    color: Color = LocalContentColor.current,
) {
    CircularProgressIndicator(
        modifier.size(size).clearAndSetSemantics {
        },
        color = color,
        strokeWidth = AuthTokens.focusWidth.dp,
        trackColor = Color.Transparent,
    )
}
