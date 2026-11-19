package net.chronoflow.core.plan.drawing

import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.decode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KernedEllipsisTest {
    /** Measures an ellipsized run [overshoot] px wider than the budget the ellipsizer worked with, like a kerned pair at the cut. */
    private class KernedMetrics(
        private val overshoot: Float,
    ) : TextMetrics {
        private val base = FakeTextMetrics()

        override fun measure(
            style: TimelineTextStyle,
            text: String,
            sizePx: Float,
        ): Float = base.measure(style, text, sizePx) + if (text.length > 1 && text.endsWith("…")) overshoot else 0f

        override fun ellipsize(
            style: TimelineTextStyle,
            text: String,
            sizePx: Float,
            maxWidthPx: Float,
        ): String = base.ellipsize(style, text, sizePx, maxWidthPx)

        override fun ascent(
            style: TimelineTextStyle,
            sizePx: Float,
        ): Float = base.ascent(style, sizePx)
    }

    private fun titles(metrics: TextMetrics): List<String> {
        val fixture = DrawingFixtures.singleTile(title = "A title that is far too long for the tile", tileWidth = 120.0)
        return fixture
            .build(metrics)
            .decode()
            .filter { it.code == DrawOp.TEXT }
            .mapNotNull { it.text }
    }

    @Test fun aKernedOvershootShortensTheRunInsteadOfDroppingIt() {
        val texts = titles(KernedMetrics(overshoot = 2f))
        assertTrue(texts.toString(), texts.any { it.startsWith("A title") && it.endsWith("…") })
    }

    @Test fun anOvershootThatSurvivesTheSecondPassDropsTheRun() {
        assertTrue(titles(KernedMetrics(overshoot = 1000f)).none { it.startsWith("A title") })
    }

    @Test fun theFakeReturnsEmptyWhenNothingFitsBesideTheEllipsis() {
        val fake = FakeTextMetrics()
        assertEquals("", fake.ellipsize(TimelineTextStyle.TITLE, "Long text", 10f, 5f))
    }
}
