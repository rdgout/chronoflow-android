package net.chronoflow.core.ui.trip

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.ui.R

@Composable
fun tripsStatusText(status: TripStatus): String =
    stringResource(
        when (status) {
            TripStatus.Active -> R.string.screens_active
            TripStatus.Completed -> R.string.screens_completed
            else -> R.string.screens_upcoming
        },
    )
