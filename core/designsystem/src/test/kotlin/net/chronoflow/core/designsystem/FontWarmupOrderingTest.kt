package net.chronoflow.core.designsystem

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.warmFontsAfterFirstFrame
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FontWarmupOrderingTest {
    @Test fun noFontPreloadsBeforeTheFirstFrameCommits() =
        runTest(timeout = 60.seconds) {
            val firstFrame = CompletableDeferred<Unit>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            var calls = 0
            val warmup = launch { warmFontsAfterFirstFrame(firstFrame, { calls++ }, dispatcher) }
            runCurrent()
            assertEquals(0, calls)
            firstFrame.complete(Unit)
            warmup.join()
            assertEquals(4, calls)
        }
}
