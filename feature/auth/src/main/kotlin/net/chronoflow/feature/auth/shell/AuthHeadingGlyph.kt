package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

enum class AuthHeadingGlyph { Problem, Check }

@Composable
internal fun AuthHeadingBadge(
    glyph: AuthHeadingGlyph,
    modifier: Modifier = Modifier,
) {
    val check = glyph == AuthHeadingGlyph.Check
    val colors = CfTheme.colors
    Box(
        modifier
            .size(AuthTokens.headingBadge.dp)
            .background(if (check) colors.dawn else colors.card, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (check) {
                LucideGlyphs.Check.imageVector(
                    AuthTokens.headingCheckStroke,
                )
            } else {
                LucideGlyphs.TriangleAlert.imageVector()
            },
            null,
            Modifier.size(AuthTokens.headingGlyph.dp),
            tint = if (check) colors.dawnForeground else colors.destructive,
        )
    }
}
