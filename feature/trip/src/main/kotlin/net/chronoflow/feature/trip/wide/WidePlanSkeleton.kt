package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.system.CfCircularProgress
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

@Composable
fun WidePlanSkeleton(
    frame: TripPageFrame,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.plan_downloading_this_trips_plan)
    val colors = CfTheme.colors
    val rowShape = RoundedCornerShape(PlanMetrics.rowRadius.dp)
    Column(
        modifier.testTag("trip.wide.loading").clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(PlanMetrics.blockIcon.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CfCircularProgress(PlanMetrics.emphasisBorder.dp, Modifier.size(PlanMetrics.icon.dp))
            Text(label, style = CfTheme.type.authHint, color = colors.mutedForeground)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp)) {
            repeat(4) {
                Spacer(
                    Modifier
                        .size(PlanMetrics.wideSkeletonPillWidth.dp, PlanMetrics.touch.dp)
                        .background(colors.muted, CircleShape),
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().height(PlanMetrics.wideSkeletonDayHeight.dp).background(colors.muted, rowShape))
        Row(
            horizontalArrangement = Arrangement.spacedBy(PlanMetrics.wideColumnGap.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                Modifier.weight(1f).height(PlanMetrics.wideSkeletonHeight.dp).testTag("trip.wide.loading.timeline"),
                horizontalArrangement = Arrangement.spacedBy(PlanMetrics.laneGap.dp),
            ) {
                Spacer(Modifier.width(PlanMetrics.gutterWidth.dp))
                val heights =
                    listOf(
                        PlanMetrics.skeletonSummary,
                        PlanMetrics.wideSkeletonLightHeight,
                        PlanMetrics.prioritySampleWidth,
                        PlanMetrics.wideSkeletonTravelHeight,
                    )
                val offsets =
                    listOf(
                        0.0,
                        PlanMetrics.wideSkeletonLightOffset,
                        PlanMetrics.wideSkeletonCaffeineOffset,
                        PlanMetrics.wideSkeletonTravelOffset,
                    )
                repeat(4) { lane ->
                    Box(Modifier.weight(1f).fillMaxSize().background(colors.surface2)) {
                        Spacer(
                            Modifier
                                .padding(
                                    top = offsets[lane].dp,
                                    start = PlanMetrics.smallGap.dp,
                                    end = PlanMetrics.smallGap.dp,
                                ).fillMaxWidth()
                                .height(heights[lane].dp)
                                .background(colors.muted, RoundedCornerShape(PlanMetrics.textPlateRadius.dp)),
                        )
                    }
                }
            }
            Column(
                Modifier.width(checkNotNull(frame.listWidthDp).dp).testTag("trip.wide.loading.list"),
                verticalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp),
            ) {
                repeat(5) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(
                                PlanMetrics.tinyTileHeight.dp,
                            ).background(colors.muted, rowShape),
                    )
                }
            }
        }
    }
}
