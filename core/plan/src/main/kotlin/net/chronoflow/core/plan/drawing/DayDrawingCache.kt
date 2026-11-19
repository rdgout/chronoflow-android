package net.chronoflow.core.plan.drawing

import net.chronoflow.core.plan.PreparedDay

/**
 * A bounded, thread-safe LRU of day drawings. A hit needs an equal key and the same [PreparedDay] by reference or by
 * value (the iOS full-equality guard, `TimelineRenderer.swift:41-43`); the layout is not compared because the day is
 * derived from it (OQ-1c-18).
 */
class DayDrawingCache(
    val maxBytes: Long = 4L * 1024 * 1024,
    val maxEntries: Int = 8,
    val maxDrawingBytes: Int = 512 * 1024,
) {
    private class Entry(
        val day: PreparedDay,
        val drawing: DayDrawing,
    )

    private val lock = Any()
    private val entries = LinkedHashMap<DayDrawingKey, Entry>()
    private var bytes = 0L
    private var builds = 0

    val totalBytes: Long get() = synchronized(lock) { bytes }
    val size: Int get() = synchronized(lock) { entries.size }
    val buildCount: Int get() = synchronized(lock) { builds }

    fun get(
        key: DayDrawingKey,
        day: PreparedDay,
    ): DayDrawing? =
        synchronized(lock) {
            val entry = entries[key]
            if (entry == null || !(entry.day === day || entry.day == day)) return null
            entries.remove(key)
            entries[key] = entry
            entry.drawing
        }

    /** Like [get] without touching the LRU order. */
    fun peek(
        key: DayDrawingKey,
        day: PreparedDay,
    ): DayDrawing? =
        synchronized(lock) {
            val entry = entries[key]
            if (entry == null || !(entry.day === day || entry.day == day)) null else entry.drawing
        }

    /** A drawing over [maxDrawingBytes] is not stored; the caller still has it. */
    fun put(
        key: DayDrawingKey,
        day: PreparedDay,
        drawing: DayDrawing,
    ) {
        synchronized(lock) { putLocked(key, day, drawing) }
    }

    /** Never runs [build] under the lock; on a race the first stored entry wins. */
    fun getOrBuild(
        key: DayDrawingKey,
        day: PreparedDay,
        build: () -> DayDrawing,
    ): DayDrawing {
        get(key, day)?.let { return it }
        val built = build()
        return synchronized(lock) {
            builds++
            val raced = entries[key]
            if (raced != null && (raced.day === day || raced.day == day)) {
                entries.remove(key)
                entries[key] = raced
                raced.drawing
            } else {
                putLocked(key, day, built)
                built
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            bytes = 0
        }
    }

    private fun putLocked(
        key: DayDrawingKey,
        day: PreparedDay,
        drawing: DayDrawing,
    ) {
        entries.remove(key)?.let { bytes -= it.drawing.estimatedBytes }
        if (drawing.estimatedBytes > maxDrawingBytes) return
        entries[key] = Entry(day, drawing)
        bytes += drawing.estimatedBytes
        val iterator = entries.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            bytes -=
                iterator
                    .next()
                    .value.drawing.estimatedBytes
            iterator.remove()
        }
        while (entries.size > maxEntries && iterator.hasNext()) {
            bytes -=
                iterator
                    .next()
                    .value.drawing.estimatedBytes
            iterator.remove()
        }
    }
}
