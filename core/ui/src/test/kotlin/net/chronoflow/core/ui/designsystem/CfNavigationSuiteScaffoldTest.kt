package net.chronoflow.core.ui.designsystem

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class CfNavigationSuiteScaffoldTest(
    private val layout: CfNavLayout,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun layoutHasATabPerItemAndOnlyTheRequestedChrome() {
        compose.setContent { ChronoFlowTheme { CfNavigationSuiteScaffold(layout, navigationItems(), 0, {}) {} } }
        compose.onNodeWithTag("app.tabs").assertExists()
        compose.onNodeWithTag(if (layout == CfNavLayout.Bar) "cf.nav.bar" else "cf.nav.rail").assertExists()
        compose.onNodeWithTag(if (layout == CfNavLayout.Bar) "cf.nav.rail" else "cf.nav.bar").assertDoesNotExist()
        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.onNodeWithTag("tab.today").assert(tab).assertIsSelected()
        compose.onNodeWithTag("tab.trips").assert(tab)
        compose.onNodeWithTag("tab.settings").assert(tab)
    }

    @Test fun selectedAndUnselectedIconsUseTheTokens() {
        val tints = mutableMapOf<String, Color>()
        var primary = Color.Unspecified
        var unselected = Color.Unspecified
        compose.setContent {
            ChronoFlowTheme {
                primary = CfTheme.colors.primary
                unselected = CfTheme.colors.mutedForeground
                CfNavigationSuiteScaffold(layout, navigationItems(), 0, {}, icon = { item, _ ->
                    val tint = LocalContentColor.current
                    SideEffect { tints[item.tag] = tint }
                }) {}
            }
        }
        compose.runOnIdle {
            assertEquals(primary, tints["tab.today"])
            assertEquals(unselected, tints["tab.trips"])
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf(arrayOf(CfNavLayout.Bar), arrayOf(CfNavLayout.Rail))
    }
}
