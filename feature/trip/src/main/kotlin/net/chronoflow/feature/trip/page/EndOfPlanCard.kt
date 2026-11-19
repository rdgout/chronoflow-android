package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

@Composable
fun EndOfPlanCard(
    prepared: PreparedPlan,
    onReturnPlan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    PlanCard(modifier.testTag("trip.plan.end")) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(PlanMetrics.touch.dp).background(colors.dawn, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    LucideGlyphs.Moon.imageVector(),
                    null,
                    Modifier.size(TripPageMetrics.endIcon),
                    tint = colors.dawnForeground,
                )
            }
            Text(
                stringResource(R.string.plan_end_of_this_plan),
                Modifier.semantics { heading() }.testTag("trip.plan.end.title"),
                style = CfTheme.type.planCardTitle,
                color = colors.foreground,
                textAlign = TextAlign.Center,
            )
            Text(
                prepared.endText,
                Modifier.testTag("trip.plan.end.text"),
                style = CfTheme.type.authBody,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            prepared.assumedEnd?.let {
                Text(
                    it,
                    Modifier.testTag("trip.plan.end.assumed"),
                    style = CfTheme.type.planSubtitle,
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                )
            }
            prepared.returnDayID?.let { id ->
                CfButton(
                    stringResource(R.string.plan_see_your_return_plan),
                    { onReturnPlan(id) },
                    Modifier.testTag("trip.plan.end.return"),
                    size = CfButtonSize.Lg,
                    fillWidth = true,
                )
            }
        }
    }
}
