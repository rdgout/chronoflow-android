package net.chronoflow.debug

import net.chronoflow.app.diagnostics.LaunchPathViolationPolicy
import java.util.concurrent.atomic.AtomicInteger

object StrictModeViolationCounter {
    private val violations = AtomicInteger()
    val count: Int get() = violations.get()

    fun reset() {
        violations.set(0)
    }

    fun record(
        isDiskOrNetwork: Boolean,
        frames: List<String>,
    ) {
        if (LaunchPathViolationPolicy.shouldCrash(true, isDiskOrNetwork, frames)) violations.incrementAndGet()
    }
}
