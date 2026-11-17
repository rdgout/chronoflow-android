package net.chronoflow.core.plan.drawing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HitIndexTest {
    private fun index(
        tiles: List<Pair<String, FloatArray>> = emptyList(),
        markers: List<Pair<String, Float>> = emptyList(),
        changes: List<Pair<String, Float>> = emptyList(),
    ) = HitIndex(
        tiles.map { it.first }.toTypedArray(),
        tiles.flatMap { it.second.toList() }.toFloatArray(),
        markers.map { it.first }.toTypedArray(),
        markers.map { it.second }.toFloatArray(),
        changes.map { it.first }.toTypedArray(),
        changes.map { it.second }.toFloatArray(),
        gutterPx = 44f,
        halfTouchPx = 24f,
    )

    @Test fun overlappingTilesResolveToTheNearestMidYAndTiesGoToTheFirst() {
        val frames = listOf("a" to floatArrayOf(0f, 0f, 100f, 100f, 30f), "b" to floatArrayOf(0f, 0f, 100f, 100f, 70f))
        assertEquals("b", index(frames).resolve(50f, 60f))
        assertEquals("a", index(frames).resolve(50f, 50f))
    }

    @Test fun theMarkerBandHitsAtTheGutterEdgeAndMissesBeyondIt() {
        val index = index(markers = listOf("m" to 200f))
        assertEquals("m", index.resolve(44f, 200f + 24f))
        assertNull(index.resolve(45f, 200f))
        assertNull(index.resolve(10f, 200f + 25f))
    }

    @Test fun theZoneChangeBandHitsAtAnyX() {
        val index = index(changes = listOf("z" to 300f))
        assertEquals("z", index.resolve(500f, 310f))
        assertNull(index.resolve(500f, 330f))
    }

    @Test fun aTileWinsOverAMarkerAndAMissReturnsNull() {
        val index = index(listOf("t" to floatArrayOf(0f, 180f, 100f, 260f, 220f)), markers = listOf("m" to 200f))
        assertEquals("t", index.resolve(10f, 200f))
        assertNull(index(emptyList()).resolve(1f, 1f))
    }

    @Test fun theHitFrameIsHalfOpen() {
        val index = index(listOf("t" to floatArrayOf(10f, 10f, 100f, 100f, 55f)))
        assertEquals("t", index.resolve(10f, 10f))
        assertNull(index.resolve(100f, 50f))
        assertNull(index.resolve(50f, 100f))
    }
}
