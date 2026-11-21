package net.chronoflow.core.ui.secure

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.WeakHashMap

@Composable
fun SecureWindow(enabled: Boolean) {
    val window = LocalActivity.current?.window
    DisposableEffect(window, enabled) {
        if (enabled && window != null) SecureWindowCounter.acquire(window)
        onDispose {
            if (enabled && window != null) SecureWindowCounter.release(window)
        }
    }
}

internal object SecureWindowCounter {
    private val uses = WeakHashMap<Window, Int>()

    fun acquire(window: Window) {
        val previous = uses[window] ?: 0
        uses[window] = previous + 1
        if (previous == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun release(window: Window) {
        val previous = uses[window] ?: return
        if (previous == 1) {
            uses.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            uses[window] = previous - 1
        }
    }
}
