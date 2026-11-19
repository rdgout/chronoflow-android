package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TypefaceResolutionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun replayTitleRun(resources: TimelineResources) {
        val tile =
            TimelineTestPlans.singleTile(
                priority = Priority.Recommended,
                top = 0.0,
                height = 120.0,
                cols = 1,
                window = 640,
            )
        val typefaces = TimelineTestFonts.typefaces(context)
        val drawing = TimelineTestPlans.build(tile.day, tile.layout, tile.key, TextPaintMetrics(typefaces))
        val bitmap = Bitmap.createBitmap(drawing.widthPx, drawing.heightPx, Bitmap.Config.ARGB_8888)
        TimelineTestPlans.render(drawing, resources, bitmap, 0)
    }

    @Test fun replayUsesTheTypefacesGivenToTheResources() {
        val typefaces = TimelineTestFonts.typefaces(context)
        val resources = TimelineResources(TimelineTestPlans.DENSITY, false, TimelineTestPlans.palette(false), typefaces)
        replayTitleRun(resources)
        assertSame(typefaces.semibold, resources.textPaints[TimelineTextStyle.TITLE.ordinal].typeface)
    }

    @Test fun replayFallsBackToTheLoadedTypefaces() {
        val typefaces = TimelineTestFonts.typefaces(context)
        val resources = TimelineResources(TimelineTestPlans.DENSITY, false, TimelineTestPlans.palette(false))
        replayTitleRun(resources)
        assertSame(typefaces.semibold, resources.textPaints[TimelineTextStyle.TITLE.ordinal].typeface)
    }

    @Test fun loadingTheTimelineResourcesParsesTheGlyphPathsOffTheCallingThread() {
        TimelineGlyphPaths.resetForTest()
        runBlocking { TimelineTypefaces.load(context, Dispatchers.IO) }
        assertTrue(TimelineGlyphPaths.isParsed)
        assertNotSame(Thread.currentThread(), TimelineGlyphPaths.parsedOnThread)
    }
}
