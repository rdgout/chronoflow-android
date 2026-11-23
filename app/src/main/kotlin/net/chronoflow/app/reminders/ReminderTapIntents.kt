package net.chronoflow.app.reminders

import android.content.Intent
import net.chronoflow.core.data.reminders.ACTION_OPEN
import net.chronoflow.core.data.reminders.EXTRA_ACCOUNT_ID
import net.chronoflow.core.data.reminders.EXTRA_BLOCK_ID
import net.chronoflow.core.data.reminders.EXTRA_INPUT_HASH
import net.chronoflow.core.data.reminders.EXTRA_TRIP_ID

internal fun sanitizedLaunchIntent(intent: Intent): Intent =
    Intent(intent).setData(null).apply {
        removeExtra(EXTRA_TRIP_ID)
        removeExtra(EXTRA_BLOCK_ID)
        removeExtra(EXTRA_ACCOUNT_ID)
        removeExtra(EXTRA_INPUT_HASH)
        if (action == ACTION_OPEN) action = null
    }
