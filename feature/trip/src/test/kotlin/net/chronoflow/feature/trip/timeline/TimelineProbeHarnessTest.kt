package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the Robolectric harness can show about draws and layers, before the invalidation test relies on it. */
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineProbeHarnessTest {
    @get:Rule val rule = createComposeRule()

    private fun settle() {
        rule.waitForIdle()
        rule.onRoot().captureToImage()
    }

    @Test fun drawsRunAndAStateWriteRedrawsOnce() {
        var state by mutableIntStateOf(0)
        var count = 0
        rule.setContent {
            Box(
                Modifier.size(20.dp).drawWithCache {
                    onDrawBehind {
                        @Suppress("UNUSED_VARIABLE")
                        val read = state
                        count++
                    }
                },
            )
        }
        settle()
        val before = count
        assertTrue("the first draw ran", before >= 1)
        rule.runOnIdle { state = 1 }
        settle()
        assertEquals(before + 1, count)
    }

    @Test fun aGraphicsLayerIsolatesItsDrawFromItsParent() {
        var parentState by mutableIntStateOf(0)
        var isolated = 0
        var shared = 0
        rule.setContent {
            Box(
                Modifier.size(100.dp).drawBehind {
                    @Suppress("UNUSED_VARIABLE")
                    val read = parentState
                },
            ) {
                Box(
                    Modifier.size(20.dp).graphicsLayer().drawWithCache {
                        onDrawBehind { isolated++ }
                    },
                )
                Box(Modifier.size(20.dp).drawWithCache { onDrawBehind { shared++ } })
            }
        }
        settle()
        val isolatedBefore = isolated
        val sharedBefore = shared
        rule.runOnIdle { parentState = 1 }
        settle()
        assertEquals("the layer's content is replayed, not redrawn", isolatedBefore, isolated)
        assertTrue("the negative control redraws with its parent", shared > sharedBefore)
    }

    @Test fun movingALayerDoesNotRedrawIt() {
        var x by mutableIntStateOf(0)
        var count = 0
        rule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(
                    Modifier
                        .offset { IntOffset(x, 0) }
                        .size(20.dp)
                        .graphicsLayer()
                        .drawWithCache { onDrawBehind { count++ } },
                )
            }
        }
        settle()
        val before = count
        rule.runOnIdle { x = 40 }
        settle()
        assertEquals(before, count)
    }
}
