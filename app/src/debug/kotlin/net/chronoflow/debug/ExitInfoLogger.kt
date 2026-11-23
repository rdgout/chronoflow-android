package net.chronoflow.debug

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

internal object ExitInfoLogger {
    fun logPrevious(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val am = context.getSystemService(ActivityManager::class.java) ?: return
        val info = am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull() ?: return
        Log.i(
            "CfExit",
            "reason=${info.reason} status=${info.status} importance=${info.importance} " +
                "pss=${info.pss} rss=${info.rss} timestamp=${info.timestamp} description=${info.description}",
        )
    }
}
