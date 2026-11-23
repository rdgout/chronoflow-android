package net.chronoflow.core.plan.trippage

data class TripPageInputs(
    val hasLoaded: Boolean,
    val recordPresent: Boolean,
    val keepOffline: Boolean?,
    val tombstoned: Boolean,
    val stored: Boolean,
    val noPlan: Boolean,
    val syncCompleted: Boolean,
    val awaitingSync: Boolean,
    val online: Boolean,
    val fetch: TripPageFetch,
) {
    fun state(): TripPageState =
        TripPageState.resolve(
            keepOffline = keepOffline ?: true,
            stored = stored,
            noPlan = noPlan,
            online = online,
            fetch = fetch,
            missingAfterSync = hasLoaded && !recordPresent && syncCompleted,
            tombstoned = tombstoned,
            awaitingSync = awaitingSync,
        )
}
