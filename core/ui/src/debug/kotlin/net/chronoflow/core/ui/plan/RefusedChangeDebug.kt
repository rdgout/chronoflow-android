package net.chronoflow.core.ui.plan

import java.util.concurrent.atomic.AtomicInteger

object RefusedChangeDebug {
    val presentedCount = AtomicInteger()

    fun presented() {
        presentedCount.incrementAndGet()
    }
}
