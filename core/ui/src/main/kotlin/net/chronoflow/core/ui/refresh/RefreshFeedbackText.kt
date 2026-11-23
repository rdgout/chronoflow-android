package net.chronoflow.core.ui.refresh

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.sync.refresh.RefreshFeedback
import net.chronoflow.core.sync.refresh.RefreshFeedbackKey
import net.chronoflow.core.ui.R

@Composable
fun refreshFeedbackText(feedback: List<RefreshFeedback>): String {
    val entries =
        feedback.map { entry ->
            when (entry.key) {
                RefreshFeedbackKey.UpToDate -> {
                    stringResource(R.string.screens_up_to_date)
                }

                RefreshFeedbackKey.NeedsConnection -> {
                    stringResource(R.string.screens_needs_a_connection)
                }

                RefreshFeedbackKey.SomethingWentWrong -> {
                    stringResource(R.string.screens_something_went_wrong)
                }

                RefreshFeedbackKey.TripsAdded -> {
                    pluralStringResource(
                        R.plurals.screens_n_trips_added,
                        entry.count ?: 0,
                        entry.count ?: 0,
                    )
                }

                RefreshFeedbackKey.TripsUpdated -> {
                    pluralStringResource(
                        R.plurals.screens_n_trips_updated,
                        entry.count ?: 0,
                        entry.count ?: 0,
                    )
                }

                RefreshFeedbackKey.TripsRemoved -> {
                    pluralStringResource(
                        R.plurals.screens_n_trips_removed,
                        entry.count ?: 0,
                        entry.count ?: 0,
                    )
                }
            }
        }
    return entries.joinToString(stringResource(R.string.ui_refresh_feedback_separator))
}
