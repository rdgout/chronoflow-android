package net.chronoflow.feature.trip.list

import net.chronoflow.core.plan.trippage.DayListRows
import net.chronoflow.feature.trip.page.PageIndex

class ListModeIndex(
    dayIds: List<String>,
    rows: List<DayListRows>,
    hasHidden: Boolean,
) : PageIndex {
    override val summary = 0
    override val days = 1
    private val headers = HashMap<String, Int>()
    private val positions = HashMap<String, Int>()
    private val dayByIndex: Array<String?>
    override val end: Int
    override val hidden: Int?
    override val itemCount: Int

    init {
        val rowsByDay = rows.associateBy { it.dayId }
        val ids = ArrayList<String?>()
        ids.add(null)
        ids.add(null)
        dayIds.forEach { id ->
            headers[id] = ids.size
            ids.add(id)
            rowsByDay[id]?.rows?.forEach { row ->
                positions[row.key] = ids.size
                ids.add(id)
            }
        }
        end = ids.size
        hidden = if (hasHidden) end + 1 else null
        itemCount = end + 1 + if (hasHidden) 1 else 0
        dayByIndex = ids.toTypedArray()
    }

    override fun headerIndex(dayId: String): Int = requireNotNull(headers[dayId])

    fun rowIndex(key: String): Int = requireNotNull(positions[key])

    override fun dayAt(index: Int): String? = dayByIndex.getOrNull(index)
}
