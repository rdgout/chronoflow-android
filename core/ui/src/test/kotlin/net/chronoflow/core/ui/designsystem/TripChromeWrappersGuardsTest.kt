package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfCenterAlignedTopAppBar
import net.chronoflow.core.designsystem.system.CfDropdownMenu
import net.chronoflow.core.designsystem.system.CfDropdownMenuItem
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
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
class TripChromeWrappersGuardsTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun nativeBarAndMenuFitTheProfile() {
        var picks = 0
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme {
                    var expanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag("wrappers.root")) {
                        CfCenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.screens_my_trips)) },
                            navigationIcon = {
                                CfBackButton(
                                    stringResource(R.string.screens_my_trips),
                                    {},
                                    Modifier.testTag("wrappers.back"),
                                )
                            },
                            actions = {
                                Box {
                                    CfIconButton(
                                        MaterialSymbols.MoreVert,
                                        stringResource(R.string.plan_trip_actions),
                                        { expanded = true },
                                        Modifier.testTag("wrappers.open"),
                                    )
                                    CfDropdownMenu(expanded, { expanded = false }) {
                                        CfDropdownMenuItem(
                                            onClick = {
                                                picks++
                                                expanded = false
                                            },
                                            modifier = Modifier.testTag("wrappers.item"),
                                            text = { Text(stringResource(R.string.plan_go_to_a_day)) },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile)
        Guards.assertNoInteractiveOverlap(compose)
        compose.onNodeWithTag("wrappers.open").performClick()
        compose.onNodeWithTag("wrappers.item").assertIsDisplayed()
        val item = compose.onNodeWithTag("wrappers.item").getUnclippedBoundsInRoot()
        assertTrue((item.bottom - item.top).value >= 48)
        val popup = compose.onNode(isPopup()).fetchSemanticsNode()
        assertTrue(popup.layoutInfo.width <= case.profile.widthDp * case.profile.density + 1)
        assertTrue(popup.layoutInfo.height <= case.profile.heightDp * case.profile.density + 1)
        compose.onNodeWithTag("wrappers.item").performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        assertEquals(1, picks)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.phone)
    }
}
