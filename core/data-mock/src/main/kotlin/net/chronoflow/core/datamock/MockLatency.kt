package net.chronoflow.core.datamock

import kotlinx.coroutines.delay
import kotlin.math.roundToLong

object MockLatency {
    fun exact(seconds: Double): suspend (Double) -> Unit =
        {
            if (seconds > 0) delay((seconds * 1000).roundToLong())
        }
}
