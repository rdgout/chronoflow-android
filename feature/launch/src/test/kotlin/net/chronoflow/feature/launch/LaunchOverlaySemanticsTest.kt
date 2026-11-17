package net.chronoflow.feature.launch

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchOverlaySemanticsTest(
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8, language)).around(compose)

    @Test fun overlayExposesOneNamedImagePaneWithoutChildren() {
        val controller = launchController(compose)
        compose.setContent { ProfiledLaunchOverlay(controller) }
        compose.onAllNodesWithTag("launch.opening").assertCountEquals(1)
        val node = compose.onNodeWithTag("launch.opening").fetchSemanticsNode()
        assertEquals(Role.Image, node.config[SemanticsProperties.Role])
        assertTrue(node.config[SemanticsProperties.ContentDescription].single().isNotBlank())
        assertEquals(
            node.config[SemanticsProperties.ContentDescription].single(),
            node.config[SemanticsProperties.PaneTitle],
        )
        assertTrue(node.children.isEmpty())
    }

    companion object {
        @JvmStatic @Parameters
        fun parameters(): Collection<Array<Any>> = AppLanguage.entries.map { arrayOf<Any>(it) }
    }
}
