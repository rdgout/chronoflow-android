package net.chronoflow.feature.launch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchRadialPaintTest {
    @Test fun haloHasClearCentreAndSymmetricFalloffAtDensity2() = halo(2f)

    @Test fun haloHasClearCentreAndSymmetricFalloffAtDensity3() = halo(3f)

    @Test fun sunHasTheSpecifiedRadialFalloffAtDensity2() = sun(2f)

    @Test fun sunHasTheSpecifiedRadialFalloffAtDensity3() = sun(3f)

    private fun halo(density: Float) {
        val image = ImageBitmap((500 * density).toInt(), (500 * density).toInt())
        val radius = 250 * density
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(image), Size(radius * 2, radius * 2)) {
            drawCircle(LaunchScenePainter.haloBrush(LaunchAppearance(false), density), radius, Offset(radius, radius))
        }
        val bitmap = image.asAndroidBitmap()

        fun alpha(
            r: Int,
            vertical: Boolean = false,
        ): Int =
            bitmap.getPixel(
                (radius + if (vertical) 0f else r * density).roundToInt(),
                (radius + if (vertical) r * density else 0f).roundToInt(),
            ) ushr
                24
        assertEquals(0, alpha(0))
        assertEquals(0, alpha(85))
        assertTrue(alpha(100) > 40)
        assertTrue(alpha(200) < alpha(100))
        for (r in listOf(0, 85, 100, 200)) assertEquals(alpha(r).toDouble(), alpha(r, true).toDouble(), 1.0)
    }

    private fun sun(density: Float) {
        val radius = 127.3f * density
        val image = ImageBitmap((radius * 2).toInt(), (radius * 2).toInt())
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(image), Size(radius * 2, radius * 2)) {
            drawCircle(
                LaunchScenePainter.sunBrush(LaunchAppearance(false), density),
                90 * density,
                Offset(radius, radius),
            )
        }
        val bitmap = image.asAndroidBitmap()

        fun alpha(r: Int): Int = bitmap.getPixel((radius + r * density).roundToInt(), radius.roundToInt()) ushr 24
        assertEquals(255, alpha(60))
        assertTrue(alpha(80) in 100..160)
        assertTrue(alpha(89) < 40)
    }
}
