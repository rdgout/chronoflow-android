package net.chronoflow.feature.settings.components

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.settings.SettingsPage
import net.chronoflow.feature.settings.bodyclock.BodyClockReadiness
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsReadinessLayoutTest(
    dark: Boolean,
) : SettingsScreenTest(dark = dark) {
    @Test fun firstMissingButtonFillsTheCardContentWidth() {
        showReadiness(BodyClockReadiness.NeedsAnswers(2, 2, 0))
        val card = compose.onNodeWithTag("settings.bodyClock.readiness").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("settings.bodyClock.firstMissing").fetchSemanticsNode().boundsInRoot
        val padding = PlanUITokens.space5 * profile.density
        assertEquals(card.left + padding, button.left, 1f)
        assertEquals(card.right - padding, button.right, 1f)
    }

    @Test fun readyCheckDiscIsCenteredOnItsCopy() = checkDiscCenter(adjusted = false)

    @Test fun adjustedCheckDiscIsCenteredOnItsCopy() = checkDiscCenter(adjusted = true)

    private fun checkDiscCenter(adjusted: Boolean) {
        showReadiness(BodyClockReadiness.Ready(adjusted, emptyList()))
        val check = compose.onNodeWithTag("settings.bodyClock.readiness.check", true).fetchSemanticsNode().boundsInRoot
        val details =
            compose
                .onNodeWithTag(
                    "settings.bodyClock.readiness.details",
                    true,
                ).fetchSemanticsNode()
                .boundsInRoot
        assertEquals(details.center.y, check.center.y, 1f)
    }

    private fun showReadiness(state: BodyClockReadiness) {
        show {
            SettingsPage(R.string.settings_body_clock, {}, "page") {
                SettingsReadinessView(state, {})
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
