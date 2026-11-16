package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Test

class ErfTest {
    @Test
    fun matchesLibmReferenceValues() {
        assertEquals(0.5204998778130465, erf(0.5), 1e-15)
        assertEquals(0.8427007929497149, erf(1.0), 1e-15)
        assertEquals(0.9953222650189527, erf(2.0), 1e-15)
        assertEquals(-0.8427007929497149, erf(-1.0), 1e-15)
        assertEquals(0.9999999845827421, erf(4.0), 1e-15)
        assertEquals(0.9999779095030015, erf(3.0), 1e-15)
        assertEquals(0.9999999999999927, erf(5.5), 1e-15)
        assertEquals(0.011283415555849616, erf(0.01), 1e-15)
        assertEquals(0.9998656672600594, erf(2.7), 1e-15)
        assertEquals(0.0, erf(0.0), 0.0)
    }
}
