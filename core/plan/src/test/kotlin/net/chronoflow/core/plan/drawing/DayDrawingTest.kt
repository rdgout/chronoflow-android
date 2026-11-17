package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayDrawingTest {
    private val key = DayDrawingKey("d", "r", 300, 1f, 1f, dark = false, compact = false, language = Language.En)

    private fun drawing(
        ops: ByteArray = ByteArray(0),
        coords: FloatArray = FloatArray(0),
        texts: Array<String> = emptyArray(),
        targets: List<TimelineTarget> = emptyList(),
    ) = DayDrawing(
        key,
        300,
        100,
        ops,
        coords,
        IntArray(0),
        IntArray(0),
        texts,
        ByteArray(texts.size),
        ByteArray(0),
        FloatArray(0),
        targets,
        HitIndex.Empty,
    )

    @Test fun emptyDayReportsTheFormulaBytes() {
        val empty = drawing()
        assertEquals(HitIndex.Empty.estimatedBytes + 128, empty.estimatedBytes)
    }

    @Test fun bytesCountEveryArrayTextAndTarget() {
        val target = TimelineTarget(TimelineTarget.Kind.BLOCK, "b", "label", 0f, 0f, 1f, 1f, 0f, "trip.block.b")
        val d = drawing(ByteArray(3), FloatArray(8), arrayOf("abcd"), listOf(target))
        val expected =
            3 + 4 * 8 + 1 + (40 + 2 * 4) + (64 + 2 * ("label".length + "trip.block.b".length)) +
                HitIndex.Empty.estimatedBytes + 128
        assertEquals(expected, d.estimatedBytes)
        assertTrue(d.estimatedBytes > drawing().estimatedBytes)
    }
}
