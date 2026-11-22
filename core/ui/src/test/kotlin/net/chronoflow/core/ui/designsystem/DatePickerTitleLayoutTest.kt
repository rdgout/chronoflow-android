@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfDatePickerDialog
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
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
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatePickerTitleLayoutTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun customTitleKeepsItsNativeInsets() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme {
                    CfDatePickerDialog(
                        rememberDatePickerState(initialDisplayedMonthMillis = 1_791_158_400_000L),
                        {},
                        {},
                        stringResource(R.string.settings_done),
                        stringResource(R.string.wizard_cancel),
                        title = stringResource(R.string.wizard_departure_date),
                        supporting = stringResource(R.string.wizard_time_in_city, "Amsterdam"),
                    )
                }
            }
        }
        val dialog = compose.onNode(isDialog()).getUnclippedBoundsInRoot()
        val supporting = compose.onNodeWithTag("cf.date.supporting", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val pixel = with(compose.density) { 1.toDp() }
        assertTrue(
            "PickerDateTime title must clear the rounded left corner",
            supporting.left >= dialog.left + PlanUITokens.space6.dp - pixel,
        )
        assertTrue(
            "PickerDateTime title must clear the rounded right corner",
            supporting.right <= dialog.right - PlanUITokens.space6.dp + pixel,
        )
        assertTrue(
            "PickerDateTime title must clear the top corner",
            supporting.top >= dialog.top + PlanUITokens.space4.dp,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            (LayoutMatrix.phone + LayoutCase(DeviceProfiles.foldInner, net.chronoflow.core.model.config.AppLanguage.Nl))
                .map { arrayOf<Any>(it) }
    }
}
