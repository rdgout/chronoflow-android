package net.chronoflow.core.navigation

data class PendingLaunchIntent(
    val links: Set<String> = emptySet(),
    val reminder: ReminderTapExtras? = null,
) {
    override fun toString(): String = "PendingLaunchIntent(links=<redacted>, reminder=<redacted>)"

    class Buffer {
        private var pending: PendingLaunchIntent? = null

        @Synchronized fun capture(intent: PendingLaunchIntent) {
            if (intent.links.isEmpty() && intent.reminder == null) return
            pending =
                PendingLaunchIntent((pending?.links ?: emptySet()) + intent.links, intent.reminder ?: pending?.reminder)
        }

        @Synchronized fun consume(): PendingLaunchIntent? = pending.also { pending = null }
    }
}

data class ReminderTapExtras(
    val tripId: String,
    val blockId: String? = null,
    val accountId: String? = null,
    val inputHash: String? = null,
) {
    companion object {
        fun parse(
            tripId: String?,
            blockId: String?,
            accountId: String?,
            inputHash: String?,
        ): ReminderTapExtras? {
            if (tripId.isNullOrEmpty() || tripId.any { it == '/' || it == '?' || it == '#' }) return null
            return ReminderTapExtras(tripId, blockId, accountId, inputHash)
        }
    }
}

fun interface LaunchIntentHandler {
    suspend fun handle(intent: PendingLaunchIntent)
}
