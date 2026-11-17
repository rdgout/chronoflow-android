package net.chronoflow.app

import android.content.Intent

internal object RootOverrides {
    fun reduceMotion(intent: Intent?): Boolean? = LaunchPlatform.overrides(intent).reduceMotion

    internal fun parseFlag(raw: String?): Boolean? =
        when (raw) {
            "1", "true" -> true
            "0", "false" -> false
            else -> null
        }
}
