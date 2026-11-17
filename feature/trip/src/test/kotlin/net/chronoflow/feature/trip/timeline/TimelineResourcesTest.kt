package net.chronoflow.feature.trip.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.plan.drawing.TimelineGlyph
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@Logic
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineResourcesTest {
    private val density = 2.625f
    private val resources = TimelineResources(density, false, TimelinePalette.Light)

    @Test fun everyGlyphParsesInsideTheTwentyFourUnitBox() {
        for (glyph in TimelineGlyph.all) {
            val paths = TimelineGlyphPaths.paths[glyph.ordinal]
            assertTrue("${glyph.name} has no path", paths.isNotEmpty())
            for (path in paths) {
                val bounds = RectF()
                path.computeBounds(bounds, true)
                assertFalse("${glyph.name} is empty", path.isEmpty)
                assertTrue(
                    "${glyph.name} $bounds",
                    bounds.left >= -1f && bounds.top >= -1f && bounds.right <= 25f && bounds.bottom <= 25f,
                )
            }
        }
    }

    @Test fun dotsLandOnA7DpGridAnchoredAtTheOrigin() {
        val size = (70 * density).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        resources.dotPaint.color = OPAQUE
        Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), resources.dotPaint)
        var covered = 0
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                val x = ((PlanMetrics.dotStep * i + PlanMetrics.dotStep / 2) * density).roundToInt()
                val y = ((PlanMetrics.dotStep * j + PlanMetrics.dotStep / 2) * density).roundToInt()
                if (bitmap.getPixel(x, y) ushr 24 > 0) covered++
            }
        }
        assertEquals(100, covered)
        assertEquals(0, bitmap.getPixel(0, 0) ushr 24)
    }

    @Test fun theHatchStripeIsTwoDpWideFromTheCornerLine() {
        val size = (100 * density).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), resources.hatchPaints[0])

        fun alphaAt(distanceDp: Double): Int {
            val axis = (distanceDp / Math.sqrt(2.0) * density).roundToInt()
            return bitmap.getPixel(axis, axis) ushr 24
        }
        assertTrue(alphaAt(1.0) > 0)
        assertEquals(0, alphaAt(5.0))
    }

    @Test fun theCacheKeysOnDensityAndDark() {
        val a = TimelineResources.Cache.get(density, false, TimelinePalette.Light)
        assertSame(a, TimelineResources.Cache.get(density, false, TimelinePalette.Light))
        assertNotSame(a, TimelineResources.Cache.get(density, true, TimelinePalette.Dark))
    }

    private companion object {
        const val OPAQUE = -1
    }
}
