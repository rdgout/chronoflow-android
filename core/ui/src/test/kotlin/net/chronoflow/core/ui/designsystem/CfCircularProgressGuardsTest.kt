package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.system.CfCircularProgress
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CfCircularProgressGuardsTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.small)).around(compose)

    @Test fun nativeProgressFitsTheSkeletonSlot() {
        compose.setContent {
            ProfileHost(DeviceProfiles.small) {
                ChronoFlowTheme {
                    Box(Modifier.fillMaxSize()) {
                        CfCircularProgress(
                            PlanMetrics.emphasisBorder.dp,
                            Modifier.size(PlanMetrics.icon.dp).testTag("progress"),
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("progress").assertIsDisplayed()
    }
}
