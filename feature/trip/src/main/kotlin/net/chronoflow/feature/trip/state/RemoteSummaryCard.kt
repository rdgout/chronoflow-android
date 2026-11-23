package net.chronoflow.feature.trip.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.page.DualClocks
import net.chronoflow.feature.trip.page.PlanCard
import net.chronoflow.feature.trip.page.SummaryClockState
import net.chronoflow.feature.trip.page.TripPageData

@Composable
fun RemoteSummaryCard(
    page: TripPageData,
    clocks: SummaryClockState,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.plan_plan_summary)
    PlanCard(
        modifier.testTag("trip.remote.summary").semantics {
            contentDescription = label
            isTraversalGroup = true
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp)) {
            Text(stringResource(R.string.plan_your_plan), style = CfTheme.type.eyebrow, color = CfTheme.colors.dawnText)
            page.clockShift?.let { Text(it, style = CfTheme.type.welcomeWordmark, color = CfTheme.colors.foreground) }
            if (page.card != null) DualClocks(clocks)
        }
    }
}
