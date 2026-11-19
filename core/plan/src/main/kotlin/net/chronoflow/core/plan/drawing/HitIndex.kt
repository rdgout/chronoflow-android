package net.chronoflow.core.plan.drawing

import kotlin.math.abs

/** Tap resolution of one day over primitive arrays, in px; the order is iOS `DayTimelineView` (l.102-113). */
class HitIndex internal constructor(
    private val tileIds: Array<String>,
    /** Per tile: hit frame l, t, r, b and the tile frame's midY. */
    private val tileFrames: FloatArray,
    private val markerIds: Array<String>,
    private val markerTops: FloatArray,
    private val changeIds: Array<String>,
    private val changeTops: FloatArray,
    private val gutterPx: Float,
    private val halfTouchPx: Float,
) {
    val estimatedBytes: Int
        get() =
            4 * (tileFrames.size + markerTops.size + changeTops.size) +
                8 * (tileIds.size + markerIds.size + changeIds.size) +
                (tileIds.sumOf { 40 + 2 * it.length }) + (markerIds.sumOf { 40 + 2 * it.length }) +
                (changeIds.sumOf { 40 + 2 * it.length }) + 64

    fun resolve(
        x: Float,
        y: Float,
    ): String? {
        var best = -1
        var bestDistance = Float.MAX_VALUE
        for (i in tileIds.indices) {
            val o = i * TILE_STRIDE
            if (x >= tileFrames[o] && x < tileFrames[o + 2] && y >= tileFrames[o + 1] && y < tileFrames[o + 3]) {
                val distance = abs(tileFrames[o + 4] - y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = i
                }
            }
        }
        if (best >= 0) return tileIds[best]
        if (x <= gutterPx) {
            for (i in markerIds.indices) if (abs(markerTops[i] - y) <= halfTouchPx) return markerIds[i]
        }
        for (i in changeIds.indices) if (abs(changeTops[i] - y) <= halfTouchPx) return changeIds[i]
        return null
    }

    companion object {
        internal const val TILE_STRIDE = 5
        val Empty =
            HitIndex(emptyArray(), FloatArray(0), emptyArray(), FloatArray(0), emptyArray(), FloatArray(0), 0f, 0f)
    }
}
