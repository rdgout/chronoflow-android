@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfSkeletonTest : PrimitiveTestFixture() {
    @Test fun skeletonIsStaticAndHasNoAccessibleContent() {
        host { CfSkeleton(Modifier.size(100.dp, 24.dp).testTag("skeleton")) }
        val node = compose.onNodeWithTag("skeleton").fetchSemanticsNode()
        assertTrue(node.children.isEmpty())
        assertFalse(node.config.contains(SemanticsProperties.Text))
        assertFalse(node.config.contains(SemanticsProperties.ContentDescription))
        val first = compose.onNodeWithTag("skeleton").captureToImage()
        compose.mainClock.advanceTimeBy(1000)
        val second = compose.onNodeWithTag("skeleton").captureToImage()
        val a = IntArray(first.width * first.height)
        val b = IntArray(second.width * second.height)
        first.readPixels(a)
        second.readPixels(b)
        assertTrue(a.contentEquals(b))
    }
}
