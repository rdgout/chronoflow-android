package net.chronoflow.feature.trip.wide

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinnedDayListFontScaleTest {
    private val compose = createComposeRule()
    private val profile = DeviceProfiles.foldInner.copy(fontScale = 2f)

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun bandHeightFollowsTheFontScale() {
        val host = TripPageTestRig("S3")
        try {
            compose.setContent { ProfileHost(profile) { ChronoFlowTheme { host.Content() } } }
            compose.waitForIdle()
            val band = compose.onNodeWithTag("trip.compactHeader").getUnclippedBoundsInRoot()
            assertEquals(1.3f, compose.density.fontScale, 0.001f)
            assertEquals(46.8f, band.height.value, 1f / compose.density.density)
        } finally {
            host.close()
        }
    }
}
