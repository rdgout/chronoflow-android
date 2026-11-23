package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
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
class WelcomeAccessibilityTest(
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8, language)).around(compose)

    @Test fun readingOrderRolesAndCallbacks() {
        var plans = 0
        var signs = 0
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) { WelcomeScreen(null, null, { plans++ }, { signs++ }) }
            }
        }
        for (tag in welcomeTags) compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).assertCountEquals(1)
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        compose.onAllNodes(heading, useUnmergedTree = true).assertCountEquals(1)
        compose.onNode(heading and hasTestTag(WelcomeTags.HEADLINE)).assertIsDisplayed()
        for (tag in listOf(WelcomeTags.PLAN, WelcomeTags.SIGN_IN)) {
            compose
                .onNodeWithTag(
                    tag,
                ).assertHasClickAction()
                .assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
                ).performClick()
        }
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
                useUnmergedTree = true,
            ).assertCountEquals(0)
        val tops = welcomeTags.map { compose.bounds(it).top.value }
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
        assertEquals(1, plans)
        assertEquals(1, signs)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun languages(): List<Array<Any>> = AppLanguage.entries.map { arrayOf<Any>(it) }
    }
}
