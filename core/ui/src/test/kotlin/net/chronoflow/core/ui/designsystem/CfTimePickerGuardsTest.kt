@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.system.CfTimePicker
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
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
class CfTimePickerGuardsTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun inlinePickerFitsAndCompactUsesEditableFields() {
        val compact = case.profile.heightDp < 640
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(darkTheme = false, reduceMotion = true) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CfTimePicker(rememberTimePickerState(8, 30, true), compact = compact)
                    }
                }
            }
        }
        compose.onAllNodes(hasSetTextAction(), useUnmergedTree = true).assertCountEquals(if (compact) 2 else 0)
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            LayoutMatrix.parameters(
                listOf(
                    LayoutCase(DeviceProfiles.small, AppLanguage.Nl),
                    LayoutCase(DeviceProfiles.pixel8, AppLanguage.En),
                    LayoutCase(DeviceProfiles.largeFont, AppLanguage.Nl),
                    LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl),
                ),
            )
    }
}
