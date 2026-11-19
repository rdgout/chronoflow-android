package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cbrt
import kotlin.math.pow

class CubicBezierTests {
    private val presets = listOf(CubicBezier.linear, CubicBezier.easeOut, CubicBezier.easeInOut, CubicBezier.intro)

    private fun reference(
        curve: CubicBezier,
        progress: Double,
    ): Double {
        fun coordinate(
            t: Double,
            a: Double,
            b: Double,
        ): Double {
            val p = t * a
            val q = (1 - t) * a + t * b
            val r = (1 - t) * b + t
            return (1 - t) * ((1 - t) * p + t * q) + t * ((1 - t) * q + t * r)
        }
        var low = 0.0
        var high = 1.0
        repeat(60) {
            val t = (low + high) / 2
            if (coordinate(t, curve.x1, curve.x2) < progress) low = t else high = t
        }
        return coordinate((low + high) / 2, curve.y1, curve.y2)
    }

    @Test fun presetsAgreeWithIndependentBisectionToOneTenThousandth() {
        for (curve in presets) {
            for (step in 0..1000) {
                val p = step / 1000.0
                assertEquals(reference(curve, p), curve.value(p), 0.0001)
            }
        }
    }

    @Test fun introNearEndpointAgreesWithIndependentBisection() {
        for (p in listOf(0.9, 0.95, 0.99, 0.999, 0.999999)) {
            assertEquals(reference(CubicBezier.intro, p), CubicBezier.intro.value(p), 1e-12)
            assertTrue(CubicBezier.intro.value(p) < 1)
        }
        assertEquals(0.9997243836889174, CubicBezier.intro.value(0.95), 1e-12)
    }

    @Test fun cssReferenceValues() {
        val inputs = listOf(0.1, 0.25, 0.5, 0.75, 0.9)
        val references =
            listOf(
                listOf(0.1605721542, 0.3781381308, 0.6846431874, 0.9065353493, 0.9829733903),
                listOf(0.0197224535, 0.1291619310, 0.5, 0.8708380690, 0.9802775465),
                listOf(0.3966935490, 0.7672843007, 0.9460794615, 0.9911081873, 0.9988302593),
            )
        for ((curve, values) in listOf(CubicBezier.easeOut, CubicBezier.easeInOut, CubicBezier.intro).zip(references)) {
            for ((p, expected) in inputs.zip(values)) assertEquals(expected, curve.value(p), 1e-4)
        }
        for (curve in presets) {
            assertEquals(0.0, curve.value(-1.0), 0.0)
            assertEquals(1.0, curve.value(2.0), 0.0)
        }
    }

    @Test fun flatDerivativesAndOvershootingY() {
        val cubeRoot = CubicBezier(0.0, 1.0 / 3, 0.0, 2.0 / 3)
        for (p in listOf(1e-9, 0.001, 0.125, 0.9)) assertEquals(p.pow(1.0 / 3), cubeRoot.value(p), 1e-5)
        val flat = CubicBezier(1.0, 0.0, 0.0, 1.0)
        for (p in listOf(0.499999, 0.5, 0.500001)) {
            val t = 0.5 + cbrt((p - 0.5) / 4)
            assertEquals(3 * t * t - 2 * t * t * t, flat.value(p), 1e-5)
        }
        assertTrue(CubicBezier(0.3, -1.0, 0.7, 2.0).value(0.1) < 0)
        assertTrue(CubicBezier(0.3, -1.0, 0.7, 2.0).value(0.9) > 1)
    }

    @Test fun presetControlsAndMonotonicity() {
        assertEquals(CubicBezier(0.0, 0.0, 0.58, 1.0), CubicBezier.easeOut)
        assertEquals(CubicBezier(0.42, 0.0, 0.58, 1.0), CubicBezier.easeInOut)
        assertEquals(CubicBezier(0.2, 0.8, 0.2, 1.0), CubicBezier.intro)
        assertEquals(CubicBezier(0.0, 0.0, 1.0, 1.0), CubicBezier.linear)
        for (curve in presets) {
            val values = (0..1000).map { curve.value(it / 1000.0) }
            assertTrue(values.zipWithNext().all { (a, b) -> a <= b })
        }
        assertThrows(IllegalArgumentException::class.java) { CubicBezier(-1.0, 0.0, 1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { CubicBezier.linear.value(Double.NaN) }
    }
}
