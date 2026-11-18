package net.chronoflow.core.datamock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Android helper coverage (new). */
class MockTimeTest {
    @Test fun isoTruncatesFractions() =
        assertEquals(
            "2026-11-06T00:00:00Z",
            iso(Instant.parse("2026-11-06T00:00:00.789Z")),
        )

    @Test fun instantAddsSeconds() = assertEquals(Instant.parse("2026-10-14T12:30:00Z"), instant("2026-10-14T12:30Z"))

    @Test fun instantPreservesSeconds() =
        assertEquals(
            Instant.parse("2026-10-14T12:30:12Z"),
            instant("2026-10-14T12:30:12Z"),
        )

    @Test fun secondsIncludesMilliseconds() = assertEquals(0.789, seconds(Instant.ofEpochMilli(789)), 0.0)
}
