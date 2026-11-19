package net.chronoflow.core.designsystem.effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors

private val lightShadow =
    Shadow(
        radius = (2 * AuthTokens.lightShadowRadius).dp,
        color = CfLightColors.authShadow.copy(alpha = 1f),
        spread = AuthTokens.lightShadowSpread.dp,
        offset = DpOffset(0.dp, AuthTokens.lightShadowY.dp),
        alpha = CfLightColors.authShadow.alpha,
    )
private val darkShadow =
    Shadow(
        radius = (2 * AuthTokens.darkShadowRadius).dp,
        color = CfDarkColors.authShadow.copy(alpha = 1f),
        spread = AuthTokens.darkShadowSpread.dp,
        offset = DpOffset(0.dp, AuthTokens.darkShadowY.dp),
        alpha = CfDarkColors.authShadow.alpha,
    )

fun softShadowFor(dark: Boolean): Shadow = if (dark) darkShadow else lightShadow

fun Modifier.softShadow(
    shape: Shape,
    dark: Boolean,
): Modifier = dropShadow(shape, softShadowFor(dark))
