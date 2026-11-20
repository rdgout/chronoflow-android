package net.chronoflow.feature.trip.actions

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TripActionsCaptureTest(
    private val case: ActionsCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun capture() {
        if (case.state == "failure") TripActionsDebugCounters.failurePresented.set(0)
        compose.setContent { ActionsFixture(case) }
        if (case.state == "failure") compose.waitUntil(4_000) { TripActionsDebugCounters.failurePresented.get() == 1 }
        val name =
            when (case.state) {
                "busy" -> "TripActionsBusy_noBoard_pixel8_en_light"
                "failure" -> "TripActionsFailure_noBoard_pixel8_nl_light"
                "noPlan" -> "TripActionsNoPlan_pixel8_en_light"
                "offline" -> "TripActions_offline_pixel8_nl_dark"
                else -> "TripActions_pixel8_en_light"
            }
        BoardCapture.capture(compose, name)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(ActionsCase("online", DeviceProfiles.pixel8, AppLanguage.En, false)),
                arrayOf(ActionsCase("noPlan", DeviceProfiles.pixel8, AppLanguage.En, false)),
                arrayOf(ActionsCase("offline", DeviceProfiles.pixel8, AppLanguage.Nl, true)),
                arrayOf(ActionsCase("busy", DeviceProfiles.pixel8, AppLanguage.En, false)),
                arrayOf(ActionsCase("failure", DeviceProfiles.pixel8, AppLanguage.Nl, false)),
            )
    }
}
