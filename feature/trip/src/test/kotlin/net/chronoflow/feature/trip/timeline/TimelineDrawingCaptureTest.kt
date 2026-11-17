package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** S3 days 1 and 2 at the board width (390) and 411, and day 3 (divider, continued flight, hatch, core tile) at 841, light and dark, for review beside `Plan` and `PlanDark`. */
@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineDrawingCaptureTest(
    private val dayIndex: Int,
    private val window: Int,
    private val dark: Boolean,
    @Suppress("unused") private val label: String,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun capturesTheDay() {
        val typefaces = TimelineTestFonts.typefaces(context)
        val fixture = TimelineTestPlans.prepared("S3", Language.En)
        val day = fixture.plan.days[dayIndex]
        val key = TimelineTestPlans.key(day, fixture.plan.revision, window, dark = dark)
        val drawing = TimelineTestPlans.build(day, fixture.layouts.getValue(day.id), key, TextPaintMetrics(typefaces))
        val palette = TimelineTestPlans.palette(dark)
        val resources = TimelineResources(TimelineTestPlans.DENSITY, dark, palette, typefaces)
        val bitmap = Bitmap.createBitmap(drawing.widthPx, drawing.heightPx, Bitmap.Config.ARGB_8888)
        TimelineTestPlans.render(drawing, resources, bitmap, palette.background)
        BoardCapture.capture(bitmap, "timeline/S3-day${dayIndex + 1}-W$window-${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{3}")
        fun cases(): List<Array<Any>> =
            (listOf(0, 1).flatMap { day -> listOf(390, 411).map { day to it } } + (2 to 841)).flatMap { (day, window) ->
                listOf(false, true).map { dark ->
                    arrayOf<Any>(day, window, dark, "day${day + 1} W$window ${if (dark) "dark" else "light"}")
                }
            }
    }
}
