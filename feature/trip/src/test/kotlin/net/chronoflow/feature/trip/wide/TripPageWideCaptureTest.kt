package net.chronoflow.feature.trip.wide

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.trip.page.tripBoardBitmap
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
class TripPageWideCaptureTest(
    private val case: TripPageWideCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private var fixture: TripPageWideFixture? = null

    @After fun close() {
        fixture?.close()
    }

    @Test fun capturesTheBoardState() {
        val host = TripPageWideFixture(case).also { fixture = it }
        host.start(compose)
        val menu = case.board == "PlanWideDayMenu"
        if (menu) compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(tripBoardBitmap(compose, TripPageWideFixture.ROOT, menu), case.name)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripPageWideCase.captures().map { arrayOf(it) }
    }
}
