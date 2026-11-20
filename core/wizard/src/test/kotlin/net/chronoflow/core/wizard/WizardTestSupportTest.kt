package net.chronoflow.core.wizard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class WizardTestSupportTest {
    @Test fun fixturesDecode() {
        assertEquals("AMS", generationInput().outbound.legs[0].fromIata)
        assertTrue(generationReview().identity.token.isNotBlank())
    }

    @Test fun testClockAdvancesInMilliseconds() {
        val clock = WizardTestClock()
        val start = clock.now
        clock.advance(1799.999)
        assertEquals(1_799_999L, Duration.between(start, clock.now).toMillis())
    }
}
