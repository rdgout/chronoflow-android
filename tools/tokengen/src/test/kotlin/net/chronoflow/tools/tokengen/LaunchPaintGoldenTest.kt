package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchPaintGoldenTest {
    private val paint by lazy { launchPaint(repoRoot.design("SplashScreen.dc.html")) }
    private val golden by lazy { joined(iosGolden("CfSky.generated.swift")) }

    @Test
    fun haloStopsMatchIOS() {
        val body = golden.substringAfter("haloStops: [(Double, Double)] { [").substringBefore("] }")
        val pairs =
            Regex("\\(([\\d.]+), ([\\d.]+)\\)")
                .findAll(body)
                .map {
                    it.groupValues[1].toDouble() to
                        it.groupValues[2].toDouble()
                }.toList()
        assertEquals(83, pairs.size)
        assertEquals(83, paint.haloStops.size)
        for ((index, pair) in pairs.withIndex()) {
            assertEquals("position $index", pair.first, paint.haloStops[index].first, 1e-9)
            assertEquals("alpha $index", pair.second, paint.haloStops[index].second, 1e-9)
        }
    }

    @Test
    fun planeAlphasMatchIOS() {
        val body = golden.substringAfter("planeAlphas: [Double] { [").substringBefore("] }")
        val values = Regex("[\\d.]+").findAll(body).map { it.value.toDouble() }.toList()
        assertEquals(49, values.size)
        assertEquals(49, paint.planeAlphas.size)
        for ((index, value) in values.withIndex()) assertEquals("plane $index", value, paint.planeAlphas[index], 1e-9)
    }

    @Test
    fun pressTokensComeFromTheTheme() {
        val press = pressTokens(repoRoot.resolve(THEME_PATH).readText())
        assertEquals(0.97, press.scale, 0.0)
        assertEquals(0.85, press.opacity, 0.0)
        assertEquals(120, press.durationMillis)
        val sky = String(tokenOutputs(repoRoot).getValue(SKY_PATH))
        assertEquals(true, sky.contains("val curve = floatArrayOf(0.25f, 0.1f, 0.25f, 1f)"))
    }
}
