package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/** Port of iOS `TimelineAlphaTests.coreBarDoesNotFadeTheTitleOrInterval`. */
@Layout
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineAlphaPixelTests {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = 3f

    private fun render(priority: Priority): Pair<Bitmap, TimelineTestPlans.SingleTile> {
        val typefaces = TimelineTestFonts.typefaces(context)
        val tile =
            TimelineTestPlans.singleTile(
                priority = priority,
                top = 40.0,
                height = 120.0,
                cols = 2,
                window = 390,
                density = density,
                compact = false,
                layoutHeight = 200.0,
                firstOfTypeSleep = true,
            )
        val drawing: DayDrawing = TimelineTestPlans.build(tile.day, tile.layout, tile.key, TextPaintMetrics(typefaces))
        val resources = TimelineResources(density, false, TimelineTestPlans.palette(false), typefaces)
        val bitmap = Bitmap.createBitmap(drawing.widthPx, drawing.heightPx, Bitmap.Config.ARGB_8888)
        TimelineTestPlans.render(drawing, resources, bitmap, TimelineTestPlans.palette(false).background)
        return bitmap to tile
    }

    @Test fun coreBarDoesNotFadeTheTitleOrInterval() {
        val (core, tile) = render(Priority.Core)
        val (plain, _) = render(Priority.Recommended)
        val minX = PlanMetrics.gutterWidth + PlanMetrics.laneGap + PlanMetrics.tileInset
        val from = ((minX - 1) * density).roundToInt()
        val to = ((minX + PlanMetrics.coreBorder + 1) * density).roundToInt()
        var outside = 0
        var inside = 0
        for (y in 0 until core.height) {
            for (x in 0 until core.width) {
                if (core.getPixel(x, y) == plain.getPixel(x, y)) continue
                if (x in from..to) inside++ else outside++
            }
        }
        assertTrue("${tile.placed.block.id}: $outside pixels differ outside the core bar", outside == 0)
        assertTrue("the core bar changed nothing", inside > 0)
    }
}
