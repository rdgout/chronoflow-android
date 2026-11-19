package net.chronoflow.app.trip

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.plan.trippage.openingDay
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TripListSmokeTest : ScreenTest() {
    @Test fun seededListTogglesToTheSameDayTimeline() {
        val started = System.nanoTime()
        val (day, block) =
            runBlocking(Dispatchers.IO) {
                val now = Instant.now()
                val detail =
                    MockAppSeed
                        .make(now)
                        .accounts.values
                        .first()
                        .details
                        .getValue(TRIP)
                val day = checkNotNull(openingDay(detail.plan.days, detail.plan.blocks, null, null, null, now)).id
                val block =
                    when (day) {
                        "day-29864490" -> OPENING_PREP_FIRST_BLOCK
                        "day-29865930" -> OPENING_TRAVEL_FIRST_BLOCK
                        "day-29866935" -> OPENING_ARRIVAL_FIRST_BLOCK
                        else -> error("the rebased mock opening day changed")
                    }
                day to block
            }
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_PLAN_TEST_ROUTE" to "trip:$TRIP:list",
                "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to "en",
            ),
        )
        compose.waitUntilExactlyOneExists(
            hasTestTag("trip.viewToggle") and hasContentDescription("Show as timeline"),
            15_000,
        )
        compose.waitUntilExactlyOneExists(hasTestTag("trip.block.$block"), 15_000)
        compose.onNodeWithTag("trip.viewToggle").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.day.$day"), 5_000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("list journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    private companion object {
        const val TRIP = "0199a3c0-0000-7000-8000-000000000001"
        const val OPENING_PREP_FIRST_BLOCK = "caffeine-caffeine_routine-29864490"
        const val OPENING_TRAVEL_FIRST_BLOCK = "caffeine-caffeine_serving-29865930"
        const val OPENING_ARRIVAL_FIRST_BLOCK = "travel-flight-29866350"
    }
}
