package net.chronoflow.core.plan.trippage

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class DualClockValuesTest {
    private val from = AirportRef("AMS", "Amsterdam", "Europe/Amsterdam")
    private val to = AirportRef("SYD", "Sydney", "Australia/Sydney")
    private val now = Instant.parse("2026-10-18T10:00:00Z")

    @Test fun destinationAndHomeZonesSelectTheirClock() =
        runTest(timeout = 60.seconds) {
            val formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            val destination = dualClockValues(from, to, now, to.zone, false, formatter, CopyFormat(Language.En))
            assertEquals(ClockLabel.HomeTime, destination[0].label)
            assertEquals(ClockLabel.YoureHere, destination[1].label)
            assertFalse(destination[0].active)
            assertTrue(destination[1].active)
            assertEquals(
                ClockLabel.YoureHere,
                dualClockValues(from, to, now, from.zone, false, formatter, CopyFormat(Language.En))[0].label,
            )
        }

    @Test fun sameZoneActivatesOnlyTheFirstClock() =
        runTest(timeout = 60.seconds) {
            val formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            val values = dualClockValues(from, from, now, from.zone, false, formatter, CopyFormat(Language.En))
            assertEquals(listOf(true, false), values.map { it.active })
        }

    @Test fun remoteLabelsDoNotInventAnActiveZone() =
        runTest(timeout = 60.seconds) {
            val formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            val values = dualClockValues(from, to, now, null, true, formatter, CopyFormat(Language.En))
            assertEquals(listOf(ClockLabel.Home, ClockLabel.There), values.map { it.label })
            assertEquals(listOf(false, false), values.map { it.active })
        }

    @Test fun outsideThePlanHasNoActiveZone() {
        assertNull(activeZone(emptyList(), now))
    }
}
