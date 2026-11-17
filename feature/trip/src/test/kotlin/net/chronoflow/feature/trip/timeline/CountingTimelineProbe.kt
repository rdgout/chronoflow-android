package net.chronoflow.feature.trip.timeline

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Thread-safe counts of compositions and draws per (kind, day id). */
class CountingTimelineProbe : TimelineProbe {
    data class Cell(
        val kind: TimelineProbe.Kind,
        val dayId: String,
    )

    class Counts(
        val composed: Map<Cell, Int>,
        val drew: Map<Cell, Int>,
    ) {
        /** What happened after [since]: per-cell differences, zero cells left out. */
        fun delta(since: Counts): Counts = Counts(diff(composed, since.composed), diff(drew, since.drew))

        fun composed(
            kind: TimelineProbe.Kind,
            dayId: String,
        ): Int = composed[Cell(kind, dayId)] ?: 0

        fun drew(
            kind: TimelineProbe.Kind,
            dayId: String,
        ): Int = drew[Cell(kind, dayId)] ?: 0

        fun composedIds(kind: TimelineProbe.Kind): Set<String> =
            composed.filter { it.key.kind == kind && it.value > 0 }.keys.mapTo(HashSet()) { it.dayId }

        fun drewIds(kind: TimelineProbe.Kind): Set<String> =
            drew.filter { it.key.kind == kind && it.value > 0 }.keys.mapTo(HashSet()) { it.dayId }

        private fun diff(
            now: Map<Cell, Int>,
            before: Map<Cell, Int>,
        ): Map<Cell, Int> =
            now
                .mapNotNull { (cell, count) ->
                    (count - (before[cell] ?: 0)).takeIf { it != 0 }?.let { cell to it }
                }.toMap()
    }

    private val composed = ConcurrentHashMap<Cell, AtomicInteger>()
    private val drew = ConcurrentHashMap<Cell, AtomicInteger>()

    override fun composed(
        kind: TimelineProbe.Kind,
        dayId: String,
    ) {
        composed.getOrPut(Cell(kind, dayId)) { AtomicInteger() }.incrementAndGet()
    }

    override fun drew(
        kind: TimelineProbe.Kind,
        dayId: String,
    ) {
        drew.getOrPut(Cell(kind, dayId)) { AtomicInteger() }.incrementAndGet()
    }

    fun snapshot(): Counts = Counts(composed.mapValues { it.value.get() }, drew.mapValues { it.value.get() })
}
