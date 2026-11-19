package net.chronoflow.core.plan

import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.AssumedReturnBasis
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.EndState
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.NoteRole
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import java.time.Instant

data class PreparedBlock(
    val block: BlockView,
    val title: String,
    val interval: String,
    val priority: String,
    val reason: String?,
    val instructions: String,
    val accessibilityLabel: String,
    val status: CheckStatus?,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean,
    val zoneInstruction: String?,
    val zoneComparison: String?,
) {
    val id: String get() = block.id
}

data class PreparedNotice(
    val text: String,
    val warning: Boolean,
)

data class PreparedDay(
    val day: DayView,
    val title: String,
    val date: String,
    val weekday: String,
    val dayMonth: String,
    val zone: String,
    val status: String?,
    val summary: String,
    val headline: String?,
    val notices: List<PreparedNotice>,
    val warningCount: Int,
    val progress: String,
    val blocks: List<PreparedBlock>,
    val dividerLabels: List<String>,
    val ticks: List<String>,
) {
    val id: String get() = day.id

    fun shownNotices(
        expanded: Boolean,
        notesOpen: Boolean,
    ): List<PreparedNotice> = if (expanded && (warningCount > 0 || notesOpen)) notices else emptyList()
}

data class PreparedPlan(
    val revision: String,
    val days: List<PreparedDay>,
    val title: String,
    val subtitle: String,
    val spokenDates: String,
    val shift: String?,
    val summary: String,
    val endText: String,
    val assumedEnd: String?,
    val returnDayID: String?,
)

private class Ordered(
    val block: BlockView,
    val start: Instant?,
)

private val byStartThenId =
    Comparator<Ordered> { a, b ->
        if (a.block.startUtc == b.block.startUtc) {
            a.block.id.compareTo(b.block.id)
        } else {
            compareValues(a.start, b.start)
        }
    }

fun preparePlan(
    detail: TripDetail,
    layouts: Map<String, DayLayout>,
    formatter: CopyLineFormatter,
    pageCopy: PageCopyTable,
    language: Language,
    timeFormat: TimeFormat = TimeFormat.Value24,
    marks: List<CheckMark> = emptyList(),
    pendingChecks: List<CheckOffChange> = emptyList(),
): PreparedPlan {
    val copy = PageCopy(language, pageCopy)
    val options = CopyFormat(language, timeFormat, trip = detail.trip)
    val plan = detail.plan
    val trip = detail.trip
    val dayBlocks = timelineBlocksByDay(withoutOptionalSteps(plan))
    val statuses = HashMap<String, CheckStatus>()
    for (mark in detail.checkState.marks + marks) statuses[checkIdentity(mark.key)] = mark.status
    for (change in pendingChecks) {
        val identity = checkIdentity(CheckKeyView(change.type, change.localDate, change.ordinal))
        val status = change.status
        if (status == null) statuses.remove(identity) else statuses[identity] = status
    }

    fun format(line: CopyLine): String = formatter.format(line, options)
    val days = ArrayList<PreparedDay>(plan.days.size)
    for (day in plan.days) {
        val layout = layouts[day.id] ?: continue
        val placed = HashMap<String, PlacedBlock>()
        for (lane in timelineLanes) for (tile in layout.placed[lane].orEmpty()) placed.putIfAbsent(tile.block.id, tile)
        val candidates = ArrayList<Ordered>(placed.size + layout.markers.size + layout.changes.size)
        for (lane in timelineLanes) {
            for (tile in layout.placed[lane].orEmpty()) {
                candidates.add(
                    Ordered(tile.block, planDate(tile.block.startUtc)),
                )
            }
        }
        for (marker in layout.markers) candidates.add(Ordered(marker.block, planDate(marker.block.startUtc)))
        for (change in layout.changes) candidates.add(Ordered(change.block, planDate(change.block.startUtc)))
        candidates.sortWith(byStartThenId)
        val blocks =
            candidates.map {
                prepareBlock(
                    it.block,
                    placed[it.block.id],
                    detail,
                    statuses,
                    copy,
                    formatter,
                    options,
                    language,
                )
            }
        val notices = noticeItems(plan, detail.notices, day)
        val summary =
            listOf(BlockType.Sleep, BlockType.SeekLight).joinToString(" · ") { type ->
                val intervals = summaryIntervals(day, dayBlocks[day.id].orEmpty(), type)
                val noLight = day.noLightText
                if (intervals.isEmpty() && type == BlockType.SeekLight && noLight != null) {
                    format(noLight)
                } else {
                    copy(if (type == BlockType.Sleep) "Sleep" else "Light") + " " +
                        intervals.joinToString(", ") {
                            formatter.formatInstant(it.start, options, it.tz) + "–" +
                                formatter.formatInstant(it.end, options, it.tz)
                        }
                }
            }
        val actionable = blocks.filter { it.block.actionable && it.block.checkKey != null }
        val count = day.postArrivalIndex ?: 0
        days.add(
            PreparedDay(
                day = day,
                title =
                    formatter.catalog.text(
                        dayLabelKey(day.labelPhase).orEmpty(),
                        language,
                        mapOf("count" to count.toString()),
                        count.toDouble(),
                    ),
                date = formatter.dayDateLabel(day, language),
                weekday = formatter.formatWeekday(day.labelDate, language),
                dayMonth = formatter.formatDayMonth(day.labelDate, language),
                zone = dayZoneLabel(day, trip),
                status = day.status?.let(::dayStatusLabelKey)?.let { formatter.catalog.text(it, language) },
                summary = summary,
                headline = day.headline?.let(::format),
                notices =
                    notices.map { entry ->
                        PreparedNotice(entry.note.lines.joinToString(" ") { format(it) }, entry.warning)
                    },
                warningCount = notices.count { it.warning },
                progress =
                    copy(
                        "%@ of %@ steps done",
                        actionable.count { it.status == CheckStatus.Done }.toString(),
                        actionable.size.toString(),
                    ),
                blocks = blocks,
                dividerLabels = layout.dividers.map { copy("%@ time from here", zoneLabel(it.after.tz, trip)) },
                ticks = layout.ticks.map { formatter.formatInstant(it.tick.atUtc, options, it.tick.tz) },
            ),
        )
    }
    val hasReturn = trip.legs.any { it.returnLeg }
    val endNote = plan.days.flatMap { it.notes }.lastOrNull { it.role == NoteRole.End }
    val endSummary = if (hasReturn) null else plan.summary
    val endLines = endNote?.lines ?: endSummary?.endCopy ?: emptyList()
    val end = listOfNotNull(endSummary?.expectedAdaptedText) + endLines.filter { it.key != "How this plan ends" }
    val stateKey: String? =
        when (endNote?.endState ?: endSummary?.endState) {
            EndState.Adapted -> if (endSummary?.expectedAdaptedText == null) "Expected adapted" else null
            EndState.LikelyAdapted -> "Likely adapted"
            EndState.StillAdapting -> "Still adjusting"
            EndState.PartialTarget -> "Partial shift"
            EndState.SmallShift -> "Keep your usual rhythm"
            EndState.ReturnPlanStarts -> "Return plan starts"
            EndState.HomeTime -> "Stay closer to home time"
            EndState.Hold -> "Holding this shift"
            else -> null
        }
    val ending = end.distinct().joinToString(" ") { format(it) }
    val endText = listOfNotNull(stateKey?.let { copy(it) }, ending.ifEmpty { null }).joinToString(" · ")
    val zone =
        plan.days
            .lastOrNull()
            ?.zones
            ?.lastOrNull()
            ?.tz ?: trip.to.zone
    val assumed = if (hasReturn) null else plan.meta.assumedReturn
    val assumedEnd =
        assumed?.let {
            listOf(
                copy("Assumed trip end"),
                formatter.formatDate(it.leaveUtc, language, zone),
                formatter.formatInstant(it.leaveUtc, options, zone),
                zoneLabel(zone, trip),
                if (it.basis == AssumedReturnBasis.CheckOut) {
                    copy("Based on your check-out date")
                } else {
                    copy("Based on %@ nights", it.nights.toString())
                },
            ).joinToString(" · ")
        }
    val summary =
        listOfNotNull(
            plan.summary?.rationale,
            plan.summary?.expectedAdaptedText,
        ).joinToString(" ") { format(it) }
    val lastLabelDate = plan.days.lastOrNull()?.labelDate
    return PreparedPlan(
        revision = plan.meta.inputHash + "|" + detail.detailVersion,
        days = days,
        title = "${trip.from.city ?: trip.from.iata} → ${trip.to.city ?: trip.to.iata}",
        subtitle =
            tripMetaText(
                trip.departLocal,
                lastLabelDate,
                trip.legs.filter { !it.returnLeg }.mapNotNull { it.flightNumber },
                language,
                copy,
            ),
        spokenDates = tripDatesText(trip.departLocal, lastLabelDate, language, long = true, pageCopy = copy),
        shift = plan.summary?.let { clockShiftText(it.adaptation, formatter, options, copy) },
        summary = summary,
        endText = endText.ifEmpty { copy("There are no more steps in this plan.") },
        assumedEnd = assumedEnd,
        returnDayID = returnDayID(detail),
    )
}

private fun prepareBlock(
    block: BlockView,
    tile: PlacedBlock?,
    detail: TripDetail,
    statuses: Map<String, CheckStatus>,
    copy: PageCopy,
    formatter: CopyLineFormatter,
    options: CopyFormat,
    language: Language,
): PreparedBlock {
    val trip = detail.trip
    val title = formatter.format(block.text, options)
    val interval =
        if (block.type == BlockType.ZoneChange) {
            formatter.formatInstant(block.startUtc, options, block.displayTz)
        } else {
            formatter.blockInterval(block, options)
        }
    val priority = copy(priorityKey(block.priority))
    val checkKey = block.checkKey
    val status = if (block.actionable && checkKey != null) statuses[checkIdentity(checkKey)] else null
    val before = tile?.continuesBefore ?: false
    val after = tile?.continuesAfter ?: false
    val label =
        listOfNotNull(
            title,
            priority,
            interval,
            status?.let { copy(if (it == CheckStatus.Done) "Marked done" else "Skipped") },
            if (before) copy("Continues from the previous plan day") else null,
            if (after) copy("Continues into the next plan day") else null,
        ).joinToString(", ")
    var instruction: String? = null
    var comparison: String? = null
    val params = block.params
    if (params is BlockParams.ZoneChange) {
        val from = block.secondaryTz ?: trip.from.zone
        val to = block.displayTz
        val destination = zoneLabel(to, trip)
        instruction =
            copy(
                "From here on all times are %@ time. Set your watch to %@.",
                destination,
                formatter.formatInstant(block.startUtc, options, to),
            )
        comparison =
            "${zoneLabel(from, trip)} ${formatter.formatInstant(block.startUtc, options, from)} → $destination " +
            "$interval · ${formatter.formatOffset(params.value.offsetMin.toDouble(), language)}"
    }
    val secondary = block.secondaryText
    return PreparedBlock(
        block = block,
        title = title,
        interval = interval,
        priority = priority,
        reason =
            tile
                ?.presentation
                ?.reasons
                ?.firstOrNull()
                ?.let { formatter.format(it, options) },
        instructions =
            (
                listOf(
                    block.text,
                ) + listOfNotNull(secondary)
            ).joinToString(" ") { formatter.format(it, options) },
        accessibilityLabel = label,
        status = status,
        continuesBefore = before,
        continuesAfter = after,
        zoneInstruction = instruction,
        zoneComparison = comparison,
    )
}

fun priorityKey(priority: Priority): String =
    when (priority) {
        Priority.Core -> "Important"
        Priority.Recommended -> "Recommended"
        Priority.Optional -> "Optional"
        else -> "Information"
    }

private fun returnDayID(detail: TripDetail): String? {
    val positions =
        detail.trip.legs
            .filter { it.returnLeg }
            .map { it.position }
            .toSet()
    val flight =
        detail.plan.blocks.firstOrNull {
            val params = it.params
            params is BlockParams.Flight && params.value.legIndex in positions
        } ?: return null
    val start = planDate(flight.startUtc) ?: return null
    return detail.plan.days
        .firstOrNull {
            val dayStart = planDate(it.startUtc)
            val dayEnd = planDate(it.endUtc)
            dayStart != null && dayEnd != null && dayStart <= start && start < dayEnd
        }?.id
}

fun clockShiftText(
    adaptation: AdaptationView,
    formatter: CopyLineFormatter,
    options: CopyFormat,
    copy: PageCopy,
): String {
    if (adaptation.direction == AdaptationDirection.None) return copy("No clock shift planned")
    val duration =
        if (adaptation.shiftMin % 60 == 0) {
            copy("%@ hours", (adaptation.shiftMin / 60).toString())
        } else {
            formatter.formatMinutes(adaptation.shiftMin.toDouble(), options.language, duration = true)
        }
    return copy(
        if (adaptation.direction ==
            AdaptationDirection.Earlier
        ) {
            "We advance your clock %@"
        } else {
            "We delay your clock %@"
        },
        duration,
    )
}
