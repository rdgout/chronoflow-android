package net.chronoflow.feature.onboarding.walkthrough

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class PreferencesWalkthroughReplayTests(
    private val skip: Boolean,
) {
    @Test fun replaySkipAndDoneReturnToPreferencesWithoutAnotherCompletionWrite() =
        runTest(timeout = 60.seconds) {
            var presented = true
            var writes = 0
            val model = WalkthroughModel(WalkthroughModel.Mode.Replay, { writes++ }, { presented = false })
            finish(model)
            assertFalse(presented)
            assertTrue(model.state.value.completed)
            assertEquals(0, writes)
        }

    @Test fun replayTwiceStartsFreshAfterSkipOrDone() =
        runTest(timeout = 60.seconds) {
            var finishes = 0
            var writes = 0
            val first = WalkthroughModel(WalkthroughModel.Mode.Replay, { writes++ }, { finishes++ })
            finish(first)
            assertTrue(first.state.value.completed)
            val second = WalkthroughModel(WalkthroughModel.Mode.Replay, { writes++ }, { finishes++ })
            assertNotSame(first, second)
            assertEquals(WalkthroughModel.Mode.Replay, second.mode)
            assertEquals(WalkthroughModel.Page.Timeline, second.state.value.page)
            assertFalse(second.state.value.completed)
            assertFalse(second.state.value.busy)
            assertNull(second.state.value.failure)
            second.advance()
            assertEquals(WalkthroughModel.Page.Categories, second.state.value.page)
            second.advance()
            assertEquals(WalkthroughModel.Page.Progress, second.state.value.page)
            second.advance()
            assertTrue(second.state.value.completed)
            assertEquals(2, finishes)
            assertEquals(0, writes)
        }

    private suspend fun finish(model: WalkthroughModel) {
        if (skip) {
            model.skip()
        } else {
            model.advance()
            model.advance()
            model.advance()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "skip={0}")
        fun cases() = listOf(arrayOf(true), arrayOf(false))
    }
}
