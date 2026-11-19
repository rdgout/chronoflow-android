package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

private val rows =
    listOf(
        Triple(Priority.Core, R.string.plan_important, R.string.plan_the_steps_that_matter_most_for_shift_107475),
        Triple(Priority.Recommended, R.string.plan_recommended, R.string.plan_helpful_when_they_fit_your_day),
        Triple(Priority.Optional, R.string.plan_optional, R.string.plan_extra_support_if_you_want_it),
        Triple(Priority.Info, R.string.plan_information, R.string.plan_useful_context_not_a_task),
    )

@Composable
fun PriorityKey(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp)) {
        Text(
            stringResource(R.string.plan_priorities),
            Modifier.testTag("legend.priorities.title").semantics {
                heading()
            },
            color = CfTheme.colors.foreground,
            style = CfTheme.type.planTitle,
        )
        rows.forEach { (priority, title, description) ->
            Row(
                Modifier.testTag("legend.priority.${priority.raw}").semantics(mergeDescendants = true) {
                },
                horizontalArrangement =
                    Arrangement.spacedBy(
                        PlanMetrics.rowGap.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriorityDrawing(priority, true)
                Column {
                    Text(stringResource(title), color = CfTheme.colors.foreground, style = CfTheme.type.authStrong)
                    Text(
                        stringResource(description),
                        color = CfTheme.colors.foreground,
                        style = CfTheme.type.planSubtitle,
                    )
                }
            }
        }
    }
}
