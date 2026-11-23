package net.chronoflow.debug

import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import java.util.concurrent.atomic.AtomicInteger

object DebugRefreshProbe : RefreshDebugProbe {
    val accepted = AtomicInteger()

    @Volatile var outcome = "none"

    @Volatile var presented = "none"

    override fun accepted() {
        accepted.incrementAndGet()
    }

    override fun outcome(value: String) {
        outcome = value
    }

    override fun presented(text: String) {
        presented = text
    }

    fun reset() {
        accepted.set(0)
        outcome = "none"
        presented = "none"
    }
}
