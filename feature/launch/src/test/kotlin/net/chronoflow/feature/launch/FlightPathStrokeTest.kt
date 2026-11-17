package net.chronoflow.feature.launch

import androidx.compose.ui.graphics.PathMeasure
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.FlightPath
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlightPathStrokeTest {
    @Test fun shapeLayerStrokeTipMatchesArcLengthEvery10Milliseconds() {
        val size = LaunchSize(390.0, 844.0)
        val measure = PathMeasure().apply { setPath(LaunchScenePainter.flightPath(FlightPath(size), 3f), false) }
        val sampler = LaunchFrameSampler(LaunchTimeline.opening(size, false))
        val frame = LaunchFrame()
        for (ms in 500..1500 step 10) {
            sampler.sampleInto(ms / 1000.0, frame)
            val point = measure.getPosition((measure.length * frame.strokeEnd).toFloat())
            assertTrue(hypot(point.x / 3.0 - frame.planeX, point.y / 3.0 - frame.planeY) < 0.02)
        }
    }
}
