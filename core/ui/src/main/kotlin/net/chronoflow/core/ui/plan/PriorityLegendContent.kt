package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

@Composable
fun PriorityLegendContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("legend.priorities")) {
        LegendHeader(stringResource(R.string.plan_legend), stringResource(R.string.plan_done), onClose, "legend.title")
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(PlanMetrics.gutter.dp)) {
            PriorityKey()
        }
    }
}
