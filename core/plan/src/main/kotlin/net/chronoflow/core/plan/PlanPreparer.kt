package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.TripDetail
import java.time.Instant

/**
 * The `layouts`, `prepare`, `nowTexts` and `evict` half of iOS `PlanDecoder`, on top of [PlanDecoder].
 * Everything runs on [worker]; one entry per trip is cached per input, and unchanged days keep their instance.
 */
class PlanPreparer(
    private val decoder: PlanDecoder,
    private val catalogs: CopyCatalogProvider,
    private val worker: CoroutineDispatcher,
    private val loadPageCopy: () -> PageCopyTable = { PageCopyTable.load() },
) {
    private data class Key(
        val trip: String,
        val inputHash: String,
        val version: String,
    )

    private class LayoutEntry(
        val key: Key,
        val plan: PlanView,
        val timeline: TimelinePresentationView,
        val hidden: List<BlockType>,
        val layouts: Map<String, DayLayout>,
    )

    private class PreparedEntry(
        val key: Key,
        val detail: TripDetail,
        val language: Language,
        val timeFormat: TimeFormat,
        val marks: List<CheckMark>,
        val hidden: List<BlockType>,
        val pendingChecks: List<CheckOffChange>,
        val prepared: PreparedPlan,
    )

    private val mutex = Mutex()
    private val layoutEntries = HashMap<String, LayoutEntry>()
    private val preparedEntries = HashMap<String, PreparedEntry>()
    private var formatter: CopyLineFormatter? = null
    private var table: PageCopyTable? = null

    @Volatile
    var layoutCount: Int = 0
        private set

    @Volatile
    var prepareCount: Int = 0
        private set

    suspend fun layouts(
        detail: TripDetail,
        hidden: List<BlockType>,
    ): Map<String, DayLayout> = withContext(worker) { mutex.withLock { layoutsLocked(detail, hidden) } }

    suspend fun prepare(
        detail: TripDetail,
        hidden: List<BlockType>,
        language: Language,
        timeFormat: TimeFormat,
        marks: List<CheckMark>,
        pendingChecks: List<CheckOffChange> = emptyList(),
    ): PreparedPlan =
        withContext(worker) {
            mutex.withLock {
                val key = keyOf(detail)
                val cached = preparedEntries[detail.trip.id]
                if (cached != null && cached.key == key && cached.language == language &&
                    cached.timeFormat == timeFormat && cached.hidden == hidden && cached.marks == marks &&
                    cached.pendingChecks == pendingChecks && (cached.detail === detail || cached.detail == detail)
                ) {
                    return@withLock cached.prepared
                }
                val layouts = layoutsLocked(detail, hidden)
                val formatter = formatterLocked()
                val fresh =
                    preparePlan(detail, layouts, formatter, tableLocked(), language, timeFormat, marks, pendingChecks)
                prepareCount++
                val prepared = stable(fresh, cached?.prepared)
                preparedEntries[detail.trip.id] =
                    PreparedEntry(key, detail, language, timeFormat, marks, hidden, pendingChecks, prepared)
                prepared
            }
        }

    suspend fun nowTexts(
        layouts: Map<String, DayLayout>,
        now: Instant,
        language: Language,
        timeFormat: TimeFormat,
    ): Map<String, String> =
        withContext(worker) {
            mutex.withLock {
                val formatter = formatterLocked()
                val options = CopyFormat(language, timeFormat)
                val result = HashMap<String, String>()
                for ((id, layout) in layouts) {
                    if (nowLineOffset(layout.day, now) != null) result[id] = formatter.nowLabel(layout, now, options)
                }
                result
            }
        }

    suspend fun evict(tripID: String) {
        mutex.withLock {
            layoutEntries.remove(tripID)
            preparedEntries.remove(tripID)
        }
        decoder.evict(tripID)
    }

    /** The page copy for [language], built from the table this preparer loads on [worker]. */
    suspend fun pageCopy(language: Language): PageCopy =
        withContext(worker) { mutex.withLock { PageCopy(language, tableLocked()) } }

    private fun keyOf(detail: TripDetail) = Key(detail.trip.id, detail.plan.meta.inputHash, detail.detailVersion)

    private suspend fun formatterLocked(): CopyLineFormatter =
        formatter ?: CopyLineFormatter(catalogs.catalog()).also { formatter = it }

    private fun tableLocked(): PageCopyTable = table ?: loadPageCopy().also { table = it }

    private fun layoutsLocked(
        detail: TripDetail,
        hidden: List<BlockType>,
    ): Map<String, DayLayout> {
        val key = keyOf(detail)
        val cached = layoutEntries[detail.trip.id]
        if (cached != null && cached.key == key && cached.hidden == hidden &&
            (cached.plan === detail.plan || cached.plan == detail.plan) && cached.timeline == detail.timeline
        ) {
            return cached.layouts
        }
        val layouts = layoutPlan(withoutOptionalSteps(detail.plan), detail.timeline, hidden)
        layoutCount++
        layoutEntries[detail.trip.id] = LayoutEntry(key, detail.plan, detail.timeline, hidden, layouts)
        return layouts
    }

    /** Days equal to the previous revision's day keep that instance; an entirely equal plan keeps the previous plan. */
    private fun stable(
        fresh: PreparedPlan,
        previous: PreparedPlan?,
    ): PreparedPlan {
        if (previous == null) return fresh
        val before = HashMap<String, PreparedDay>(previous.days.size * 2)
        for (day in previous.days) before[day.id] = day
        val days = fresh.days.map { day -> before[day.id]?.takeIf { it == day } ?: day }
        val merged = fresh.copy(days = days)
        return if (merged == previous) previous else merged
    }
}
