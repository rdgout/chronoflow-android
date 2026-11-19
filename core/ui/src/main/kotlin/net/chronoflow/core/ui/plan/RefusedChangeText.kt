package net.chronoflow.core.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.plan.stepLabelKey
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.ui.R

@Composable
fun refusedChangeText(notice: SyncRefusalNotice): String? =
    when (notice) {
        SyncRefusalNotice.SleepTimes -> {
            stringResource(R.string.screens_couldn_t_save_your_sleep_times_so_bbbd9b)
        }

        is SyncRefusalNotice.StepDone -> {
            stepLabelKey(notice.type)?.let {
                stringResource(R.string.screens_couldn_t_save_step_as_done_so_we_687bfc, planScreenText(it))
            }
        }
    }
