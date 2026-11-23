package net.chronoflow.debug

import java.util.concurrent.atomic.AtomicInteger

object GuestWizardDebugCounters {
    val signUpRequested = AtomicInteger()

    fun requested() {
        signUpRequested.incrementAndGet()
    }
}
