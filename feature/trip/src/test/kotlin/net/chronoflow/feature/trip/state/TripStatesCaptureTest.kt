package net.chronoflow.feature.trip.state

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
class TripStatesCaptureTest(
    private val case: TripStatesBoardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private lateinit var fixture: TripStatesFixture

    @After fun close() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test fun capturesTheBoardState() {
        fixture = case.fixture()
        fixture.start(compose, navigation = true)
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(compose, "${case.board}_${case.profile.name}_${case.language.tag}_${case.theme}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripStatesBoardCase.captureCases().map { arrayOf(it) }
    }
}
