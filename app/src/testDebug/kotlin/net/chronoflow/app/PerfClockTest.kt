package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.app.perf.perfClockInstant
import org.junit.Test
import java.time.Instant

class PerfClockTest {
    @Test fun anIsoInstantFreezesTheClock() {
        assertThat(perfClockInstant("2026-10-13T08:00:00Z")).isEqualTo(Instant.parse("2026-10-13T08:00:00Z"))
    }

    @Test fun aNumberKeepsTheLaunchTimelineMeaning() {
        assertThat(perfClockInstant("1500")).isNull()
    }

    @Test fun anythingElseIsNotFrozen() {
        for (extra in listOf("", "abc", "2026-10-13", null)) assertThat(perfClockInstant(extra)).isNull()
    }
}
