package net.chronoflow.core.ui.plan

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
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PlanLegendCaptureTest(
    private val state: String,
    private val cell: BoardCaptureCell,
) {
    private val case = SheetCase(state, cell.profile, cell.language, cell.dark)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun capture() {
        compose.setContent {
            SheetFixture(case) {
                if (state == "full") LegendSheetContent({}) else PriorityLegendContent({})
            }
        }
        BoardCapture.capture(compose, "PlanLegend_${if (state == "priorities") "priorities_" else ""}${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf("full", "priorities").flatMap { state ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(state, cell) }
            }
    }
}
