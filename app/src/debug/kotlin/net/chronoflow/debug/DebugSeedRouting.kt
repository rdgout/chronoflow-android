package net.chronoflow.debug

enum class SeedAccount(
    val raw: String,
) {
    Demo("demo"),
    TwoFactor("twofactor"),
}

sealed interface PlanRouteSeed {
    data object Trips : PlanRouteSeed

    data class Trip(
        val tripId: String,
        val list: Boolean = false,
    ) : PlanRouteSeed
}

object DebugSeedRouting {
    @Volatile var current: TestSeed? = null
        internal set
}
