package net.chronoflow.feature.wizard.checking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardCheckFailure(ui: WizardCheckingUi) {
    val kind = ui.failure ?: return
    val c = CfTheme.colors
    val glyph =
        when (kind) {
            CheckFailureKind.Offline -> LucideGlyphs.CloudOff
            CheckFailureKind.Server -> LucideGlyphs.CircleAlert
            CheckFailureKind.Throttle -> LucideGlyphs.Clock
        }
    val tag = "wizard.checking.${kind.tag}"
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            }.drawWithCache {
                val border = AuthTokens.borderWidth.dp.toPx()
                val dash = PlanUITokens.wizardFailureDashLength.dp.toPx()
                val stroke = Stroke(width = border, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
                val radius = CornerRadius(PlanUITokens.radius3Xl.dp.toPx())
                val inset = Offset(border / 2, border / 2)
                val area = Size((size.width - border).coerceAtLeast(0f), (size.height - border).coerceAtLeast(0f))
                onDrawBehind { drawRoundRect(c.border, inset, area, radius, style = stroke) }
            }.padding(horizontal = PlanUITokens.space5.dp, vertical = PlanUITokens.wizardFailurePadding.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .testTag("$tag.icon")
                .size(PlanUITokens.wizardFailureIconCircle.dp)
                .background(c.muted, CircleShape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                glyph.imageVector(),
                null,
                Modifier.size(PlanUITokens.wizardFailureIconSize.dp),
                tint = c.mutedForeground,
            )
        }
        Text(
            ui.failureTitle.resolve(),
            color = c.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardFailureTitle),
            textAlign = TextAlign.Center,
        )
        Text(
            ui.failureBody.resolve(),
            color = c.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardFailureBody),
            textAlign = TextAlign.Center,
        )
    }
}
