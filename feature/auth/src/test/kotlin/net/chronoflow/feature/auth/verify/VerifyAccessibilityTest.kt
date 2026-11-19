package net.chronoflow.feature.auth.verify

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class VerifyAccessibilityTest : VerifyTestFixture() {
    @Test fun failedPaneChangesAndEachFailedActionAnnouncesOnceWithoutCountdownSpeech() {
        host()
        val pane = SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)
        val waiting = compose.onNode(pane).fetchSemanticsNode().config[SemanticsProperties.PaneTitle]
        act { model.resend() }
        assertNull(
            compose
                .onNodeWithTag(
                    "verify.countdown",
                ).fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.LiveRegion),
        )
        act {
            base.failNext(MockMethod.VerificationStatus, DataFailure.Server(503))
            model.check()
        }
        val failed = compose.onNode(pane).fetchSemanticsNode().config[SemanticsProperties.PaneTitle]
        assertNotEquals(waiting, failed)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertCountEquals(1)
        val first =
            compose
                .onNodeWithTag(
                    "auth.announcement",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertNull(model.takeAnnouncement { it.key })
        compose.mainClock.advanceTimeByFrame()
        assertEquals(
            first,
            compose
                .onNodeWithTag(
                    "auth.announcement",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription],
        )
        act {
            base.failNext(MockMethod.VerificationStatus, DataFailure.Server(503))
            model.check()
        }
        val second =
            compose
                .onNodeWithTag(
                    "auth.announcement",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertNotEquals(first, second)
        assertNull(model.takeAnnouncement { it.key })
    }
}
