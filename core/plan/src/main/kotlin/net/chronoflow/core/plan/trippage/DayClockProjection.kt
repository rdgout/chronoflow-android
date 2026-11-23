package net.chronoflow.core.plan.trippage

import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.nowLabel
import net.chronoflow.core.plan.nowLineOffset
import java.time.Instant

/** The now line of one day: [offset] in dp from the day's top and the clock [text] in the zone containing now. */
data class NowLineValue(
    val offset: Double,
    val text: String,
)

/**
 * Port of iOS `TripPlanModel.updateDayClocks` with `PlanDecoder.nowTexts`. Offset and text are computed together, so a
 * tick is one write per changed day (iOS writes `now`, then `text`). Days that do not contain [now] are absent.
 */
fun projectDayClocks(
    layouts: Map<String, DayLayout>,
    now: Instant,
    formatter: CopyLineFormatter,
    format: CopyFormat,
): Map<String, NowLineValue> {
    val result = HashMap<String, NowLineValue>()
    for ((id, layout) in layouts) {
        val offset = nowLineOffset(layout.day, now) ?: continue
        result[id] = NowLineValue(offset, formatter.nowLabel(layout, now, format))
    }
    return result
}

/** Ids whose value differs, including ids present in only one map. */
fun changedDayIds(
    old: Map<String, NowLineValue>,
    new: Map<String, NowLineValue>,
): Set<String> {
    val changed = HashSet<String>()
    for ((id, value) in new) if (old[id] != value) changed.add(id)
    for (id in old.keys) if (id !in new) changed.add(id)
    return changed
}
