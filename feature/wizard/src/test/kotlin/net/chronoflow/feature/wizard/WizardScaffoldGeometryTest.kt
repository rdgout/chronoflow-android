@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardScaffoldGeometryTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot().let {
            Rect(it.left.value, it.top.value, it.right.value, it.bottom.value)
        }

    private fun near(
        expected: Float,
        actual: Float,
    ) = assertEquals(expected, actual, 0.5f)

    @Test fun shellMatchesTheBoardGeometry() {
        compose.setContent { WizardTestHost(profile) { WizardScreen(screenState(), {}) } }
        val window = bounds("wizard.window")
        val header = bounds("wizard.header")
        near(profile.insets.top, header.top)
        near(54f, header.height)
        near(window.width - profile.insets.right - 34f, bounds("wizard.close").center.x)
        compose.onNodeWithTag("wizard.back").assertDoesNotExist()
        val width = if (profile.widthDp >= 669) 520f else window.width
        val left = (window.width - width) / 2
        val progress = bounds("wizard.progress")
        near(profile.insets.top + 60f, progress.top)
        near(4f, progress.height)
        near(left + 20, progress.left)
        near(left + width - 20, progress.right)
        near(profile.insets.top + 88f, bounds("wizard.title").top)
        val button = bounds("wizard.where.continue")
        assertTrue(button.height >= 52f)
        near(left + 20, button.left)
        near(window.height - profile.insets.bottom - 12f, button.bottom)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() =
            listOf(
                DeviceProfiles.small,
                DeviceProfiles.pixel8,
                DeviceProfiles.foldInner,
                DeviceProfiles.landscapeSideNav,
                DeviceProfiles.largeFont,
                DeviceProfiles.pixel8.copy(name = "boundary668", widthDp = 668, heightDp = 900),
                DeviceProfiles.pixel8.copy(name = "boundary669", widthDp = 669, heightDp = 900),
            )
    }
}
