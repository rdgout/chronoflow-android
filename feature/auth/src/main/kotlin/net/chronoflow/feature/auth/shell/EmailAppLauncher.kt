package net.chronoflow.feature.auth.shell

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

class EmailAppLauncher(
    private val context: Context,
) {
    fun open(): Boolean =
        try {
            context.startActivity(
                Intent
                    .makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}
