package net.chronoflow.feature.trip.page

class TripListIndex(
    dayIds: List<String>,
    hasHidden: Boolean,
) {
    private val ids = dayIds.toList()
    private val positions = ids.withIndex().associate { it.value to it.index }
    val summary = 0
    val days = 1

    fun headerIndex(dayId: String): Int = 2 + 2 * requireNotNull(positions[dayId])

    fun timelineIndex(dayId: String): Int = headerIndex(dayId) + 1

    val end = 2 + 2 * ids.size
    val hidden: Int? = if (hasHidden) end + 1 else null
    val itemCount = end + 1 + if (hasHidden) 1 else 0

    fun dayAt(index: Int): String? = if (index in 2 until end) ids[(index - 2) / 2] else null
}
