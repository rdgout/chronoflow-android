package net.chronoflow.core.ui.trip

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.clock.MinuteClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class TripCardInvalidationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun minuteTickRecomposesOnlyTheEyebrow() {
        val start = Instant.parse("2026-11-08T10:30:00Z")
        val source = mutableStateOf(start.minusSeconds(1800))
        val clock = MinuteClock(source)
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) {
            formatter =
                CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
        }
        val counts = mutableMapOf<String, Int>()
        val probe = TripCardProbe { id -> counts[id] = (counts[id] ?: 0) + 1 }
        val card =
            TripCardUi(
                "hero",
                TripStatus.Active,
                null,
                "+7 h",
                AirportUi("AMS", "Amsterdam"),
                AirportUi("NRT", "Tokyo"),
                "12 Oct 14:40 → 13 Oct 09:15",
                NextStepUi(BlockType.Sleep, start, "Sleep"),
            )
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CompositionLocalProvider(LocalTripCardProbe provides probe) { TripCard(card, {}, clock, formatter) }
            }
        }
        compose.waitForIdle()
        val before = counts.toMap()
        compose.runOnIdle { source.value = source.value.plusSeconds(60) }
        compose.waitForIdle()
        assertEquals(before.getValue("card.hero"), counts.getValue("card.hero"))
        assertEquals(before.getValue("eyebrow.hero") + 1, counts.getValue("eyebrow.hero"))
    }
}
