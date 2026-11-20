package net.chronoflow.core.wizard.model

object WizardClockRounding {
    const val STEP_MINUTES = 5

    fun roundToStep(
        hour: Int,
        minute: Int,
    ): String? {
        if (hour !in 0..23 || minute !in 0..59) return null
        val rounded = ((hour * 60 + minute + STEP_MINUTES / 2) / STEP_MINUTES) * STEP_MINUTES % 1440
        val h = rounded / 60
        val m = rounded % 60
        return "${h / 10}${h % 10}:${m / 10}${m % 10}"
    }

    fun change(
        stored: String?,
        hour: Int,
        minute: Int,
        touched: Boolean,
    ): String? = if (!touched) null else roundToStep(hour, minute)?.takeUnless { it == stored }
}
