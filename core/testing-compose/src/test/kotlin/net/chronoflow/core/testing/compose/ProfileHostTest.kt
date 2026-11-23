package net.chronoflow.core.testing.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.window.LocalWindowSize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ProfileHostTest(
    private val name: String,
    private val expected: List<Int>,
    private val windowPx: Pair<Int, Int>,
) {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles[name])).around(composeRule)

    @Test
    fun hostProvidesTheProfileInsetsAndWindowSize() {
        val profile = DeviceProfiles[name]
        var actual: List<Int> = emptyList()
        var window = DpSize.Zero
        composeRule.setContent {
            val density = LocalDensity.current
            ProfileHost(profile) {
                window = LocalWindowSize.current
                val inner = LocalSafeInsets.current
                actual =
                    listOf(
                        inner.getTop(density),
                        inner.getLeft(density, LayoutDirection.Ltr),
                        inner.getBottom(density),
                        inner.getRight(density, LayoutDirection.Ltr),
                    )
                Box(Modifier.fillMaxSize())
            }
        }
        composeRule.waitForIdle()
        assertThat(actual).isEqualTo(expected)
        assertThat(window).isEqualTo(DpSize(profile.widthDp.dp, profile.heightDp.dp))
        val size = composeRule.onRoot().fetchSemanticsNode().size
        assertThat(size.width).isBetween(windowPx.first - 1, windowPx.first + 1)
        assertThat(size.height).isBetween(windowPx.second - 1, windowPx.second + 1)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun profiles(): Collection<Array<Any>> =
            listOf(
                arrayOf("foldInner", listOf(132, 0, 63, 0), 2208 to 1840),
                arrayOf("small", listOf(48, 0, 96, 0), 720 to 1280),
                arrayOf("landscapeSideNav", listOf(63, 0, 0, 126), 2399 to 1079),
            )
    }
}
