package net.chronoflow.app.guest

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class GuestWizardForegroundTest {
    @Test fun configurationChangeRestartDoesNotSweep() {
        val process = Owner().apply { start() }
        var sweeps = 0
        val observer = GuestWizardForeground { sweeps++ }
        observer.observe(process.lifecycle)
        val activity = Owner().apply { start() }
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        Owner().start()
        observer.observe(process.lifecycle)
        assertEquals(0, sweeps)
        assertEquals(1, process.registry.observerCount)
        observer.close()
    }

    @Test fun backgroundToForegroundReturnSweepsOnce() {
        val process = Owner().apply { start() }
        var sweeps = 0
        val observer = GuestWizardForeground { sweeps++ }
        observer.observe(process.lifecycle)
        assertEquals(0, sweeps)
        process.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertEquals(0, sweeps)
        process.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertEquals(1, sweeps)
        observer.observe(process.lifecycle)
        assertEquals(1, sweeps)
        observer.close()
    }

    @Test fun leavingTheEntryStopsObserving() {
        val process = Owner().apply { start() }
        var sweeps = 0
        val observer = GuestWizardForeground { sweeps++ }
        observer.observe(process.lifecycle)
        observer.close()
        process.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        process.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertEquals(0, sweeps)
        assertEquals(0, process.registry.observerCount)
    }

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry

        fun start() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
    }
}
