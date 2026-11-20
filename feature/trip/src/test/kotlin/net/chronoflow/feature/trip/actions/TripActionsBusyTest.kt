package net.chronoflow.feature.trip.actions

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
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
class TripActionsBusyTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, AppLanguage.Nl),
            ).around(compose)

    @Test fun busyDisablesActionsCancelAndBackDismissal() {
        var dismissed = 0
        lateinit var activity: ComponentActivity
        val ui = actionsUi().let { it.copy(availability = it.availability.copy(busy = true)) }
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val current = LocalContext.current as ComponentActivity
                    SideEffect { activity = current }
                    TripActionsWindow(ui, null, {}, { dismissed++ })
                }
            }
        }
        compose.onNodeWithTag("trip.actions.edit").assertIsNotEnabled()
        compose.onNodeWithTag("trip.actions.duplicate").assertIsNotEnabled()
        compose.onNodeWithTag("trip.actions.cancel").assertIsNotEnabled()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("trip.actions").assertExists()
        assertEquals(0, dismissed)
    }

    @Test fun failureIsPresentedInsideTheSheetOnce() {
        TripActionsDebugCounters.failurePresented.set(0)
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TripActionsWindow(actionsUi(), TripActionFailure.Duplicate, {}, {})
                }
            }
        }
        compose.waitUntil(4_000) { TripActionsDebugCounters.failurePresented.get() == 1 }
        compose.onNodeWithTag("trip.actions.failure").assertExists()
        compose.onNodeWithTag("trip.actions").assertExists()
        assertEquals(1, TripActionsDebugCounters.failurePresented.get())
    }
}
