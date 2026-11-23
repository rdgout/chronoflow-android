package net.chronoflow.app.shell

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class, qualifiers = "w411dp-h914dp-420dpi")
class ShellSeedTest : ShellActivityFixture() {
    @Test fun tripsSeedSelectsMyTrips() {
        launch()
        compose.onNodeWithTag("trips.screen").assertExists()
        compose.onNodeWithTag("tab.trips").assertIsSelected()
    }

    @Test fun tripSeedPushesOntoToday() {
        launch("trip:0199a3c0-0000-7000-8000-000000000010")
        compose.onNodeWithTag("route.tripPage").assertExists()
        compose.onNodeWithTag("tab.today").assertIsSelected()
    }

    @Test fun invalidSeedIsIgnored() {
        launch("invalid")
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("tab.today").assertIsSelected()
    }

    @Test fun secondCompositionDoesNotReapplyTheSeed() {
        launch()
        compose.onNodeWithTag("tab.today").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("tab.today").assertIsSelected()
        assertNull(LaunchPlatform.shellStart())
    }
}
