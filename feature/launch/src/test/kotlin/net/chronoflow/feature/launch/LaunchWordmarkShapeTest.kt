package net.chronoflow.feature.launch

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.generated.LaunchWordmark
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchWordmarkShapeTest {
    @Test fun shapedOutlineAgreesWithAndroidFontInk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = context.resources.getFont(net.chronoflow.core.designsystem.R.font.fraunces_400_opsz32)
                textSize = 32f
            }
        val word = "ChronoFlow"
        val advances = FloatArray(word.length)
        paint.getTextRunAdvances(word, 0, word.length, 0, word.length, false, advances, 0)
        val path = Path()
        var x = (LaunchWordmark.WIDTH_DP - LaunchWordmark.ADVANCE_DP) / 2
        for (i in word.indices) {
            val glyph = Path()
            paint.getTextPath(word, i, i + 1, x, LaunchWordmark.BASELINE_DP, glyph)
            path.addPath(glyph)
            x += advances[i] - 0.32f
        }
        val expected = RectF()
        path.computeBounds(expected, true)
        val actual = PathParser().parsePathString(LaunchWordmark.PATH_DATA).toPath().getBounds()
        assertEquals(expected.left, actual.left, 0.75f)
        assertEquals(expected.top, actual.top, 0.75f)
        assertEquals(expected.right, actual.right, 0.75f)
        assertEquals(expected.bottom, actual.bottom, 0.75f)
    }
}
