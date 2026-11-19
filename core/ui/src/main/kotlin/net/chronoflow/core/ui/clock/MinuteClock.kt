package net.chronoflow.core.ui.clock

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import java.time.Instant

@Stable
class MinuteClock(
    private val source: State<Instant>,
) {
    val now: Instant get() = source.value
}
