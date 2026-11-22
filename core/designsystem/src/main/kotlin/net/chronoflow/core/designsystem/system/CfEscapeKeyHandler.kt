package net.chronoflow.core.designsystem.system

import android.view.KeyEvent
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
internal fun CfEscapeKeyHandler(
    enabled: Boolean = true,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val active = rememberUpdatedState(enabled)
    val dismiss = rememberUpdatedState(onDismiss)
    val window =
        generateSequence(view as View?) { it.parent as? View }
            .filterIsInstance<DialogWindowProvider>()
            .firstOrNull()
            ?.window
    DisposableEffect(window) {
        val original = window?.callback
        if (window == null || original == null) {
            onDispose {}
        } else {
            // Native dialogs can receive keys before Compose has a focused descendant.
            val callback =
                object : Window.Callback by original {
                    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                        if (!active.value || event.keyCode != KeyEvent.KEYCODE_ESCAPE) {
                            return original.dispatchKeyEvent(event)
                        }
                        if (event.action == KeyEvent.ACTION_UP) dismiss.value()
                        return true
                    }
                }
            window.callback = callback
            onDispose { if (window.callback === callback) window.callback = original }
        }
    }
}
