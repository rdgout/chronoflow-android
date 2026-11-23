package net.chronoflow.feature.trip.actions

import java.util.concurrent.atomic.AtomicInteger

object TripActionsDebugCounters {
    val failurePresented = AtomicInteger()

    fun presented() {
        failurePresented.incrementAndGet()
    }
}
