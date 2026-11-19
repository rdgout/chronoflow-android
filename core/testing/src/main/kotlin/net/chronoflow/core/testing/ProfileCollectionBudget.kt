package net.chronoflow.core.testing

class ProfileCollectionBudget(
    val maxIterations: Int = 3,
    val stableIterations: Int = 2,
    val journeyTimeoutMillis: Long = 120_000,
    val iterationOverheadMillis: Long = 30_000,
    val setupTimeoutMillis: Long = 60_000,
) {
    init {
        require(stableIterations in 2..maxIterations)
        require(journeyTimeoutMillis > 0 && iterationOverheadMillis > 0 && setupTimeoutMillis > 0)
    }

    val collectorTimeoutMillis =
        setupTimeoutMillis + maxIterations * (journeyTimeoutMillis + iterationOverheadMillis)
}
