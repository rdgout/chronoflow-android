package net.chronoflow.feature.launch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchFrameLoopTests {
    @Test fun openingUsesWallTimeAndWaitsForTheFirstDrawWithoutRequestingFrames() =
        runTest(timeout = 60.seconds) {
            val firstDraw = CompletableDeferred<Long>()
            val frames = Channel<Unit>(Channel.UNLIMITED)
            var wall = 9_000_000_000_000L
            var requests = 0
            var elapsed = -1L
            val opening =
                launch {
                    runLaunchOpening(firstDraw, { wall }, {
                        requests++
                        frames.receive()
                        requests * 16_000_000L
                    }, 1_500_000_000L) { elapsed = it }
                }
            runCurrent()
            assertEquals(0, requests)
            assertEquals(-1L, elapsed)
            assertFalse(opening.isCompleted)
            firstDraw.complete(wall)
            runCurrent()
            assertEquals(1, requests)
            wall += 1_499_000_000L
            frames.send(Unit)
            runCurrent()
            assertEquals(1_499_000_000L, elapsed)
            assertFalse(opening.isCompleted)
            assertEquals(2, requests)
            wall += 1_000_000L
            frames.send(Unit)
            runCurrent()
            assertEquals(1_500_000_000L, elapsed)
            assertTrue(opening.isCompleted)
            assertEquals(2, requests)
            frames.send(Unit)
            runCurrent()
            assertEquals(2, requests)
        }
}
