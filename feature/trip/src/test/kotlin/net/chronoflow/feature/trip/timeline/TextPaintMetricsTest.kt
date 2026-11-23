package net.chronoflow.feature.trip.timeline

import android.content.Context
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TextPaintMetricsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val metrics by lazy { TextPaintMetrics(TimelineTestFonts.typefaces(context)) }

    @Test fun tickDigitsAreTabular() {
        assertEquals(
            metrics.measure(TimelineTextStyle.TICK, "00:00", 11f),
            metrics.measure(TimelineTextStyle.TICK, "11:11", 11f),
            0f,
        )
    }

    @Test fun anEllipsizedResultFitsAndEndsWithTheEllipsis() {
        val text = "A long tile title that cannot fit"
        val max = 60f
        val shown = metrics.ellipsize(TimelineTextStyle.TITLE, text, 11f, max)
        assertTrue(shown.endsWith("…"))
        assertTrue(metrics.measure(TimelineTextStyle.TITLE, shown, 11f) <= max)
        assertEquals("Fits", metrics.ellipsize(TimelineTextStyle.TITLE, "Fits", 11f, 200f))
    }

    @Test fun ascentIsPositiveAndBelowTheLineHeight() {
        val ascent = metrics.ascent(TimelineTextStyle.TITLE, 11f)
        assertTrue("ascent $ascent", ascent > 0f && ascent < 14f)
    }

    @Test fun textScaleIsOneAtScaleOneAndStaysUnderTheCapAtOnePointThree() {
        RuntimeEnvironment.setFontScale(1.0f)
        assertEquals(1f, timelineTextScale(Density(context)), 1e-3f)
        RuntimeEnvironment.setFontScale(1.3f)
        val scale = timelineTextScale(Density(context))
        assertTrue("scale $scale", scale > 1.0f && scale <= 1.3f + 1e-3f)
    }
}
