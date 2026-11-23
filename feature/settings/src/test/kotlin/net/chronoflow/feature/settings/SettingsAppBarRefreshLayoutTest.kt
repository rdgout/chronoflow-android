package net.chronoflow.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.passkeys.PasskeysEvent
import net.chronoflow.feature.settings.passkeys.PasskeysInputs
import net.chronoflow.feature.settings.passkeys.PasskeysScreen
import net.chronoflow.feature.settings.passkeys.PasskeysScreenState
import net.chronoflow.feature.settings.plan.PlanEvent
import net.chronoflow.feature.settings.plan.PlanScreen
import net.chronoflow.feature.settings.plan.PlanScreenState
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.support.settingsNow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsAppBarRefreshLayoutTest : SettingsScreenTest() {
    @Test fun planExpandsTheBarBeforePullToRefresh() =
        check("settings.plan.screen") { refresh ->
            PlanScreen(PlanScreenState(), { if (it == PlanEvent.Refresh) refresh() })
        }

    @Test fun passkeysExpandsTheBarBeforePullToRefresh() =
        check("settings.passkeys.screen") { refresh ->
            PasskeysScreen(
                PasskeysScreenState(inputs = PasskeysInputs(), now = { settingsNow }),
                { if (it == PasskeysEvent.Refresh) refresh() },
            )
        }

    private fun check(
        tag: String,
        content: @Composable (() -> Unit) -> Unit,
    ) {
        var refreshes = 0
        show { content { refreshes++ } }

        fun barHeight() =
            compose
                .onNodeWithTag("$tag.appbar")
                .fetchSemanticsNode()
                .boundsInRoot.height
        val screen = compose.onNodeWithTag(tag)
        val expanded = barHeight()
        screen.performTouchInput { swipeUp(startY = height * .8f, endY = height * .3f) }
        val collapsed = barHeight()
        assertTrue("the upward swipe must collapse the bar", collapsed < expanded)
        screen.performTouchInput {
            swipeDown(startY = height * .35f, endY = height * .35f + (expanded - collapsed) * 1.5f)
        }
        assertEquals("the bar must expand before refresh consumes the drag", expanded, barHeight(), 1f)
        assertEquals(0, refreshes)
        screen.performTouchInput { swipeDown(startY = height * .35f, endY = height * .9f) }
        compose.waitForIdle()
        assertEquals("pull-to-refresh must still work with the bar expanded", 1, refreshes)
    }
}
