package net.chronoflow.feature.launch

import android.content.Context
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.StatusLineLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchStatusRasterTest(
    private val fontScale: Float,
) {
    @get:Rule val profile = ProfileRule(DeviceProfiles.pixel8)

    @Test fun statusRasterUsesIoAndFitsAtCappedFontScales() =
        runTest(timeout = 60.seconds) {
            var dispatched = false
            val io =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        Dispatchers.IO.dispatch(context) {
                            check(Thread.currentThread() != Looper.getMainLooper().thread)
                            dispatched = true
                            block.run()
                        }
                    }
                }
            val context = ApplicationProvider.getApplicationContext<Context>()
            val density = DeviceProfiles.pixel8.density
            val raster =
                prepareStatusRaster(
                    context,
                    context.getString(net.chronoflow.core.ui.R.string.launch_getting_your_plan_ready),
                    density,
                    fontScale,
                    LaunchAppearance(false).colors.skyTitle.toArgb(),
                    io,
                )
            assertTrue(dispatched)
            assertEquals(ceil(20 * fontScale * density).toInt(), raster.image.height)
            assertEquals(ceil(raster.textWidthDp * density).toInt(), raster.image.width)
            val layout =
                StatusLineLayout(
                    DeviceProfiles.pixel8.widthDp.toDouble(),
                    raster.textWidthDp,
                    raster.ascentDp,
                    raster.descentDp,
                    raster.lineHeightDp,
                    density.toDouble(),
                )
            assertTrue(layout.ink.minX >= 0 && layout.ink.maxX <= DeviceProfiles.pixel8.widthDp)
        }

    companion object {
        @JvmStatic
        @Parameters(name = "fontScale={0}")
        fun cases() = listOf(arrayOf(1f), arrayOf(1.3f))
    }
}
