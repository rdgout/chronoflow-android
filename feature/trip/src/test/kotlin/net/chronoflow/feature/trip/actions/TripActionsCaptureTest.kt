package net.chronoflow.feature.trip.actions

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
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
        val board =
            when (case.state) {
                "busy" -> "TripActionsBusy_noBoard"
                "failure" -> "TripActionsFailure_noBoard"
                "noPlan" -> "TripActionsNoPlan"
                "offline" -> "TripActions_offline"
                else -> "TripActions"
            }
        BoardCapture.capture(
            compose,
            "${board}_${case.profile.name}_${case.language.tag}_${if (case.dark) "dark" else "light"}",
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf("online", "noPlan", "offline", "busy", "failure").flatMap { state ->
                BoardCaptureCell.standard.map { cell ->
                    arrayOf<Any>(ActionsCase(state, cell.profile, cell.language, cell.dark))
                }
            }
    }
}
