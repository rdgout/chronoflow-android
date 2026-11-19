package net.chronoflow.feature.trip.page

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class DayJumpFocusTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var rig: TripPageTestRig

    @After fun close() {
        if (::rig.isInitialized) rig.close()
    }

    private fun host(list: Boolean = false) {
        rig = TripPageTestRig("large60", initialTimeline = !list)
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { ChronoFlowTheme(reduceMotion = true) { rig.Content() } }
        }
    }

    private fun jump() {
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.onNodeWithTag("trip.dayMenu.day.3").performClick()
        compose.onNodeWithTag("trip.dayHeader.${rig.model.prepared.days[3].id}").assertIsFocused()
    }

    @Test fun menuJumpFocusesTargetHeading() {
        host()
        jump()
    }

    @Test fun listMenuJumpFocusesTargetHeading() {
        host(list = true)
        jump()
    }

    @Test fun replacementMovesNoFocus() {
        host()
        val detail = rig.model.detail
        rig.replace(detail.copy(plan = detail.plan.copy(days = detail.plan.days.drop(1))))
        compose.waitForIdle()
        compose.onNodeWithTag("trip.dayHeader.${rig.model.prepared.days.first().id}").assertIsNotFocused()
    }
}
