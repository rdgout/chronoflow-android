package net.chronoflow.feature.trip.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

@Composable
fun TripPageSkeleton(
    downloading: Boolean,
    modifier: Modifier = Modifier,
) {
    val loading = stringResource(R.string.plan_loading_your_plan)
    Column(
        modifier.testTag("trip.skeleton").clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
    ) {
        if (downloading) {
            Text(
                stringResource(R.string.plan_downloading_this_trips_plan),
                Modifier.testTag("trip.skeleton.downloading"),
                style = CfTheme.type.authHint,
                color = CfTheme.colors.foreground,
            )
        }
        CfSkeleton(
            Modifier.fillMaxWidth().height(PlanMetrics.skeletonSummary.dp),
            RoundedCornerShape(PlanMetrics.rowRadius.dp),
        )
        CfSkeleton(Modifier.fillMaxWidth().height(PlanMetrics.touch.dp), CircleShape)
        CfSkeleton(
            Modifier.fillMaxWidth().height(PlanMetrics.skeletonTimeline.dp),
            RoundedCornerShape(PlanMetrics.rowRadius.dp),
        )
    }
}
