package net.chronoflow.feature.today

import net.chronoflow.feature.today.ui.TodayProbe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class CountingTodayProbe : TodayProbe {
    private val counts = ConcurrentHashMap<Pair<TodayProbe.Kind, String>, AtomicInteger>()

    override fun composed(
        kind: TodayProbe.Kind,
        id: String,
    ) {
        counts.getOrPut(kind to id) { AtomicInteger() }.incrementAndGet()
    }

    fun snapshot(): Map<Pair<TodayProbe.Kind, String>, Int> = counts.mapValues { it.value.get() }
}
