package net.chronoflow.core.model.auth

class LockoutWait(
    seconds: Int,
) {
    enum class Unit { Seconds, Minutes, Hours }

    val unit: Unit
    val count: Int

    init {
        val s = seconds.coerceAtLeast(0)
        val divisor: Int
        when {
            s >= 3600 -> {
                unit = Unit.Hours
                divisor = 3600
            }

            s >= 60 -> {
                unit = Unit.Minutes
                divisor = 60
            }

            else -> {
                unit = Unit.Seconds
                divisor = 1
            }
        }
        count = s / divisor + if (s % divisor > 0) 1 else 0
    }

    val localizationKey: String
        get() =
            when (unit) {
                Unit.Seconds -> "Too many attempts. Try again in %lld seconds."
                Unit.Minutes -> "Too many attempts. Try again in %lld minutes."
                Unit.Hours -> "Too many attempts. Try again in %lld hours."
            }
}
