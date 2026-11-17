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

    @Test fun exitUsesWallTimeAfterAStallWithDifferentFrameClockBases() =
        runTest(timeout = 60.seconds) {
            val origin = 9_000_000_000_000L
            var wall = origin
            val frames = Channel<Unit>(Channel.UNLIMITED)
            var elapsed = -1L
            var exit = -1L
            val sampler =
                net.chronoflow.feature.launch.model.LaunchFrameSampler(
                    net.chronoflow.feature.launch.model.LaunchTimeline.opening(
                        net.chronoflow.feature.launch.model
                            .LaunchSize(390.0, 844.0),
                        false,
                    ),
                )
            val job =
                backgroundScope.launch {
                    runLaunchOpening(
                        CompletableDeferred(origin),
                        { wall },
                        {
                            frames.receive()
                            7L
                        },
                        0L,
                        planProvider = { HandOffPlan(sampler, 1.9, 1) },
                        onExit = { exit = it },
                        onElapsed = { elapsed = it },
                    )
                }
            runCurrent()
            wall += 800_000_000
            frames.send(Unit)
            runCurrent()
            assertEquals(800_000_000, elapsed)
            assertEquals(-1L, exit)
            wall = origin + 1_900_000_000
            frames.send(Unit)
            runCurrent()
            assertEquals(1_900_000_000, exit)
            assertTrue(job.isCompleted)
        }

    @Test fun settledOpeningWaitsForRevisionThenPlaysTheLateExit() =
        runTest(timeout = 60.seconds) {
            val origin = 9_000_000_000_000L
            var wall = origin
            val frames = Channel<Unit>(Channel.UNLIMITED)
            val revisions = Channel<Long>(Channel.UNLIMITED)
            var requests = 0
            var exit = -1L
            val sampler =
                net.chronoflow.feature.launch.model.LaunchFrameSampler(
                    net.chronoflow.feature.launch.model.LaunchTimeline.opening(
                        net.chronoflow.feature.launch.model
                            .LaunchSize(390.0, 844.0),
                        false,
                    ),
                )
            var plan = HandOffPlan(sampler, null, 0)
            val job =
                backgroundScope.launch {
                    runLaunchOpening(
                        CompletableDeferred(origin),
                        { wall },
                        {
                            requests++
                            frames.receive()
                            7L
                        },
                        0L,
                        planProvider = { plan },
                        awaitRevision = { revisions.receive() },
                        onExit = { exit = it },
                        onElapsed = {},
                    )
                }
            runCurrent()
            wall += 1_500_000_000
            frames.send(Unit)
            runCurrent()
            assertEquals(1, requests)
            assertFalse(job.isCompleted)
            plan = plan.copy(exitEndSeconds = 3.9, revision = 1)
            wall = origin + 3_500_000_000
            revisions.send(1)
            runCurrent()
            assertEquals(2, requests)
            frames.send(Unit)
            runCurrent()
            assertEquals(-1L, exit)
            wall = origin + 3_900_000_000
            frames.send(Unit)
            runCurrent()
            assertEquals(3_900_000_000, exit)
            assertTrue(job.isCompleted)
        }
}
