package net.chronoflow.core.ui.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedback
import net.chronoflow.core.sync.refresh.RefreshFeedbackKey
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RefreshFeedbackCapsuleTest {
    @get:Rule val compose = createComposeRule()
    private val feedback = listOf(RefreshFeedback(RefreshFeedbackKey.UpToDate))
    private val state = mutableStateOf(RefreshFeedbackState(feedback = feedback, presentationId = 1))
    private val dismissed = mutableListOf<Long>()
    private val presented = mutableListOf<String>()
    private val presentationTimes = mutableListOf<Long>()
    private val probe =
        object : RefreshDebugProbe {
            override fun accepted() = Unit

            override fun outcome(value: String) = Unit

            override fun presented(text: String) {
                presented += text
                presentationTimes += compose.mainClock.currentTime
            }
        }

    private fun content(reduceMotion: Boolean = true) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.fillMaxSize()) { RefreshFeedbackCapsule(state.value, dismissed::add, probe) }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun onePoliteNodeIsPresentedOnce() {
        content()
        compose.onAllNodesWithTag("refresh.feedback").assertCountEquals(1)
        val node =
            compose
                .onNodeWithTag(
                    "refresh.feedback",
                ).assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
                ).fetchSemanticsNode()
        assertEquals(listOf(node.config[SemanticsProperties.ContentDescription].single()), presented)
        compose.runOnIdle { state.value = state.value.copy(refreshing = true) }
        compose.waitForIdle()
        assertEquals(1, presented.size)
    }

    @Test fun dismissesAfterExactlyTwoSeconds() {
        content()
        compose.mainClock.advanceTimeBy(1999, ignoreFrameDuration = true)
        compose.runOnIdle { assertTrue(dismissed.isEmpty()) }
        compose.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(listOf(1L), dismissed) }
    }

    @Test fun keepsTheLastDescriptionDuringExit() {
        content()
        compose.mainClock.advanceTimeBy(400)
        val before =
            compose
                .onNodeWithTag("refresh.feedback")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertTrue(before.single().isNotEmpty())
        compose.runOnIdle { state.value = state.value.copy(feedback = emptyList()) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(150)
        val duringExit =
            compose
                .onNodeWithTag("refresh.feedback")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(before, duringExit)
    }

    @Test fun aNewPresentationRestartsTheTimer() {
        content()
        compose.mainClock.advanceTimeBy(1000, ignoreFrameDuration = true)
        val frames = mutableListOf<String>()

        fun record(stage: String) {
            frames += "$stage: time=${compose.mainClock.currentTime}, presentations=$presentationTimes"
        }
        record("before write")
        compose.runOnIdle { state.value = state.value.copy(presentationId = 2) }
        record("after write")
        compose.waitForIdle()
        record("after snapshot apply")
        compose.mainClock.advanceTimeByFrame()
        record("after frame")
        compose.waitForIdle()
        record("after idle")
        assertEquals(frames.joinToString("; "), 2, presentationTimes.size)
        val elapsed = compose.mainClock.currentTime - presentationTimes.last()
        compose.mainClock.advanceTimeBy(1999 - elapsed, ignoreFrameDuration = true)
        compose.runOnIdle { assertTrue(dismissed.isEmpty()) }
        compose.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(listOf(2L), dismissed) }
    }

    private fun motion(reduceMotion: Boolean) {
        state.value = RefreshFeedbackState()
        content(reduceMotion)
        compose.runOnIdle { state.value = RefreshFeedbackState(feedback = feedback, presentationId = 1) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        val animatedTop =
            compose
                .onNodeWithTag("refresh.feedback")
                .fetchSemanticsNode()
                .boundsInRoot.top
        compose.mainClock.advanceTimeBy(400)
        val finalTop =
            compose
                .onNodeWithTag("refresh.feedback")
                .fetchSemanticsNode()
                .boundsInRoot.top
        if (reduceMotion) assertEquals(finalTop, animatedTop) else assertTrue(animatedTop < finalTop)
    }

    @Test fun reduceMotionOnlyFades() = motion(true)

    @Test fun normalMotionSlides() = motion(false)
}
