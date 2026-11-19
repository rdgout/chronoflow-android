package net.chronoflow.feature.trip.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.generated.PlanMetrics
import kotlin.math.max

@Composable
fun HeadingZoneLayout(
    heading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    zone: @Composable () -> Unit,
) {
    Layout(content = {
        heading()
        zone()
    }, modifier = modifier) { measurables, constraints ->
        val gap = PlanMetrics.smallGap.dp.roundToPx()
        val zonePlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val row =
            measurables[0].maxIntrinsicWidth(Constraints.Infinity).toLong() + gap + zonePlaceable.width <=
                constraints.maxWidth
        val headingPlaceable =
            measurables[0].measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth =
                        if (row) {
                            (constraints.maxWidth - gap - zonePlaceable.width).coerceAtLeast(
                                0,
                            )
                        } else {
                            constraints.maxWidth
                        },
                ),
            )
        if (row) {
            val headingBaseline =
                headingPlaceable[FirstBaseline].takeUnless { it == AlignmentLine.Unspecified }
                    ?: headingPlaceable.height
            val zoneBaseline =
                zonePlaceable[FirstBaseline].takeUnless { it == AlignmentLine.Unspecified } ?: zonePlaceable.height
            val baseline = max(headingBaseline, zoneBaseline)
            val height =
                constraints.constrainHeight(
                    max(
                        baseline - headingBaseline + headingPlaceable.height,
                        baseline - zoneBaseline + zonePlaceable.height,
                    ),
                )
            layout(constraints.maxWidth, height) {
                headingPlaceable.placeRelative(0, baseline - headingBaseline)
                zonePlaceable.placeRelative(constraints.maxWidth - zonePlaceable.width, baseline - zoneBaseline)
            }
        } else {
            val height = constraints.constrainHeight(headingPlaceable.height + gap + zonePlaceable.height)
            layout(constraints.maxWidth, height) {
                headingPlaceable.placeRelative(0, 0)
                zonePlaceable.placeRelative(0, headingPlaceable.height + gap)
            }
        }
    }
}
