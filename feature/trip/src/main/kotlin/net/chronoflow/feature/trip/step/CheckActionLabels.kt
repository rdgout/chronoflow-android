package net.chronoflow.feature.trip.step

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.ui.R

@Immutable
data class CheckActionLabels(
    val done: String,
    val skip: String,
    val undo: String,
)

@Composable
fun rememberCheckActionLabels(): CheckActionLabels {
    val done = stringResource(R.string.screens_mark_as_done)
    val skip = stringResource(R.string.screens_skip)
    val undo = stringResource(R.string.screens_undo)
    return remember(done, skip, undo) { CheckActionLabels(done, skip, undo) }
}

fun checkAccessibilityActions(
    actions: StepActions,
    blockId: String,
    labels: CheckActionLabels,
    onCheck: (String, CheckStatus?) -> Unit,
): List<CustomAccessibilityAction> =
    when (actions) {
        StepActions.None -> {
            emptyList()
        }

        StepActions.Open -> {
            listOf(
                CustomAccessibilityAction(labels.done) {
                    onCheck(blockId, CheckStatus.Done)
                    true
                },
                CustomAccessibilityAction(labels.skip) {
                    onCheck(blockId, CheckStatus.Skipped)
                    true
                },
            )
        }

        StepActions.Marked -> {
            listOf(
                CustomAccessibilityAction(labels.undo) {
                    onCheck(blockId, null)
                    true
                },
            )
        }
    }
