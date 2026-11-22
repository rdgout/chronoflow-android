package net.chronoflow.app.guest

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

internal class GuestWizardForeground(
    private val sweep: () -> Unit,
) : LifecycleEventObserver {
    private var lifecycle: Lifecycle? = null
    private var stopped = false

    fun observe(process: Lifecycle) {
        if (lifecycle === process) return
        check(lifecycle == null)
        lifecycle = process
        process.addObserver(this)
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                stopped = true
            }

            Lifecycle.Event.ON_START -> {
                if (stopped) {
                    stopped = false
                    sweep()
                }
            }

            else -> {}
        }
    }

    fun close() {
        lifecycle?.removeObserver(this)
        lifecycle = null
        stopped = false
    }
}
