package net.chronoflow.feature.trip.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.feature.trip.timeline.TimelineTestPlans
import org.junit.Rule
import org.junit.rules.RuleChain

abstract class PageComponentTest {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    protected val prepared get() = TimelineTestPlans.prepared("S1").plan

    protected fun host(content: @Composable () -> Unit) {
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { ChronoFlowTheme(reduceMotion = true, content = content) }
        }
    }
}
