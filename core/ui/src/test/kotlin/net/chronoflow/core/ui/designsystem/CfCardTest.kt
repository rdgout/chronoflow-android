@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfCardTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun borderCanBeSuppressedWithoutChangingTheCardFill() {
        host { CfCard(Modifier.height(100.dp).testTag("card"), border = false, shadow = false) {} }
        val p = pixels("card")
        assertTokenColor(p[p.width / 2, 0], colors.card)
        assertTokenColor(p[p.width / 2, p.height / 2], colors.card)
    }

    @Test fun rowCardClipsItsChildrenAndNeverDrawsAShadow() {
        host {
            CfCard(Modifier.height(100.dp).testTag("card"), rows = true) {
                Box(Modifier.fillMaxWidth().height(100.dp).background(colors.destructive))
            }
        }
        val p = pixels("card")
        assertTokenColor(p[0, 0], colors.background)
        assertTokenColor(p[p.width / 2, p.height / 2], colors.destructive)
        val bounds = compose.onNodeWithTag("card").fetchSemanticsNode().boundsInRoot
        val scene = pixels("fixture")
        assertTokenColor(
            scene[bounds.center.x.toInt(), (bounds.bottom + 2 * compose.density.density).toInt()],
            colors.background,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
