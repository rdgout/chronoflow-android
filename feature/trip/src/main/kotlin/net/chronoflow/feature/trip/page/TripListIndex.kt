package net.chronoflow.feature.trip.page

class TripListIndex(
    dayIds: List<String>,
    hasHidden: Boolean,
) : PageIndex {
    private val ids = dayIds.toList()
    private val positions = ids.withIndex().associate { it.value to it.index }
    override val summary = 0
    override val days = 1

    override fun headerIndex(dayId: String): Int = 2 + 2 * requireNotNull(positions[dayId])

    fun timelineIndex(dayId: String): Int = headerIndex(dayId) + 1

    override val end = 2 + 2 * ids.size
    override val hidden: Int? = if (hasHidden) end + 1 else null
    override val itemCount = end + 1 + if (hasHidden) 1 else 0

    override fun dayAt(index: Int): String? = if (index in 2 until end) ids[(index - 2) / 2] else null
}
