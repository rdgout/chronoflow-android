package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.R
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfFace
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.generated.CfTypeMetrics
import net.chronoflow.core.designsystem.generated.PlanTypeMetrics

/**
 * The few text styles this host needs, built from the generated tokens. Phase 2's theme replaces this file; until then
 * it is the only place that turns tokens into Compose styles.
 */
internal object TimelineStyles {
    private val figtree400 = FontFamily(Font(R.font.figtree_400, FontWeight.Normal))
    private val figtree600 = FontFamily(Font(R.font.figtree_600, FontWeight.SemiBold))

    val planNow: TextStyle =
        style(PlanTypeMetrics.planNow, PlanTypeMetrics.planNowFace).copy(fontFeatureSettings = TABULAR_FIGURES_COMPOSE)
    val planListTitle: TextStyle = style(PlanTypeMetrics.planListTitle, PlanTypeMetrics.planListTitleFace)
    val planSubtitle: TextStyle = style(PlanTypeMetrics.planSubtitle, PlanTypeMetrics.planSubtitleFace)

    private fun style(
        metrics: CfTypeMetrics,
        face: CfFace,
    ) = TextStyle(
        fontFamily = if (face == CfFace.Figtree600) figtree600 else figtree400,
        fontWeight = if (face == CfFace.Figtree600) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = metrics.size.sp,
        lineHeight = metrics.lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = metrics.letterSpacingEm.em,
    )
}

private const val TABULAR_FIGURES_COMPOSE = "tnum"

@Composable
internal fun timelineColors(): CfColors = if (isSystemInDarkTheme()) CfDarkColors else CfLightColors
