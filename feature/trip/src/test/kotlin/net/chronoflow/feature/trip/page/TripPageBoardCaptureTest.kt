
package net.chronoflow.feature.trip.page

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.After
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
class TripPageBoardCaptureTest(
    private val case: TripPageBoardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private lateinit var fixture: TripPageBoardFixture

    @After fun close() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test fun capturesTheBoardState() {
        fixture = TripPageBoardFixture(case)
        fixture.start(compose, navigation = true)
        if (case.board == "PlanDayMenu") fixture.openMenu(compose)
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(fixture.bitmap(compose), "${case.board}_pixel8_${case.language.tag}_${case.theme}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripPageBoardFixture.captureCases().map { arrayOf(it) }
    }
}
