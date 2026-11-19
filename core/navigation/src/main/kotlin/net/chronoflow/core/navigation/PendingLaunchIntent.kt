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
)

fun interface LaunchIntentHandler {
    suspend fun handle(intent: PendingLaunchIntent)
}
