@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.system.CfLinearProgress
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.CfTopAppBar
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class SystemTintTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun nativeSwitchSegmentsAndProgressUseTheExplicitOverrides() {
        host {
            Column {
                CfSwitch(true, {}, Modifier.testTag("on"))
                CfSwitch(false, {}, Modifier.testTag("off"))
                CfSegmentedButtons(
                    listOf(
                        stringResource(R.string.settings_light),
                        stringResource(R.string.settings_dark),
                    ),
                    0,
                    {
                    },
                    testTag = "segments",
                )
                CfLinearProgress({ .5f }, Modifier.testTag("progress"))
            }
        }
        val on = pixels("on")
        val off = pixels("off")
        assertTokenColor(on[on.width / 4, on.height / 2], colors.dawn)
        assertTokenColor(on[on.width * 7 / 10, on.height / 2], colors.dawnForeground)
        assertTokenColor(off[off.width * 3 / 4, off.height / 2], colors.input)
        val segment = pixels("segments.0")
        assertTokenColor(segment[segment.width / 2, (4 * compose.density.density).toInt()], colors.systemSelected)
        val progress = pixels("progress")
        assertTokenColor(progress[progress.width / 4, progress.height / 2], colors.dawn)
        assertTokenColor(progress[progress.width * 3 / 4, progress.height / 2], colors.muted)
    }

    @Test fun appBarUsesTheScrolledContainerAfterItsNativeStateChanges() {
        lateinit var behavior: TopAppBarScrollBehavior
        host {
            behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            CfTopAppBar(
                stringResource(R.string.settings_change_password),
                Modifier.testTag("bar"),
                scrollBehavior = behavior,
            )
        }
        var p = pixels("bar")
        assertTokenColor(p[p.width / 2, 0], colors.background)
        compose.runOnIdle {
            behavior.state.contentOffset = -1000f
            behavior.state.heightOffset =
                behavior.state.heightOffsetLimit
        }
        compose.mainClock.advanceTimeBy(1000)
        p = pixels("bar")
        assertTokenColor(p[p.width / 2, 0], colors.popover)
    }

    @Test fun refreshingIndicatorUsesThePopoverContainer() {
        host { CfPullToRefreshBox(true, {}, Modifier.size(160.dp).testTag("pull")) {} }
        compose.mainClock.advanceTimeBy(1000)
        assertTrue(pixels("pull").hasInk(colors.popover))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
