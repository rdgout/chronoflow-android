package net.chronoflow.feature.launch.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class LaunchIntro(
    val start: Duration,
    val clock: LaunchClock,
    val frozenTime: Duration? = null,
) {
    var hasPlayed = false
        private set

    fun claim(): Boolean {
        if (hasPlayed) return false
        hasPlayed = true
        return true
    }

    fun progress(
        index: Int,
        time: Duration,
    ): Double = CubicBezier.intro.value(((time - start - offset(index)) / ITEM_DURATION).coerceIn(0.0, 1.0))

    fun delay(index: Int): Double = (start + offset(index) - clock.now).toDouble(DurationUnit.SECONDS)

    companion object {
        private val offsets = intArrayOf(0, 100, 150, 300, 400, 450)
        private val ITEM_DURATION = 450.milliseconds

        fun offset(index: Int): Duration {
            require(index in offsets.indices)
            return offsets[index].milliseconds
        }
    }
}
