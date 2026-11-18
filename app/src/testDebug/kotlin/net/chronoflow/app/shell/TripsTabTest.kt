package net.chronoflow.app.shell

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class, qualifiers = "w411dp-h914dp-420dpi")
class TripsTabTest : ShellActivityFixture() {
    @Test fun demoShowsTheActiveTrip() {
        launch()
        loaded()
        compose.onNodeWithTag("trips.screen").assertExists()
        compose.onNodeWithTag("trip.$activeTripId").assertExists()
    }

    @Test fun cardOpensTheTripPageSeam() {
        launch()
        loaded()
        compose.onNodeWithTag("trip.$activeTripId").performClick()
        compose.onNodeWithTag("route.tripPage").assertExists()
    }

    @Test fun createOpensTheWizardSeam() {
        launch()
        compose.onNodeWithTag("trips.create").performClick()
        compose.onNodeWithTag("route.wizard").assertExists()
    }
}
