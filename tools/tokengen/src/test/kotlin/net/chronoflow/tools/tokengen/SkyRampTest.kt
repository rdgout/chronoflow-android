package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class SkyRampTest {
    private val light =
        "#1F285A #222A5C #252B5F #282D61 #2A2F63 #2D3066 #303268 #33336A #36356C #39366F #3C3871 #3F3A73 #413B76 " +
            "#443D78 #473E7A #4A407D #4D427F #56487F #5F4E7F #68537F #71597F #7B5E7E #84647D #8E697C #986E7B #A27379 " +
            "#AC7878 #B67E76 #C18373 #CB8871 #D58D6E #E0926A #EB9666"
    private val dark =
        "#06091C #070A1E #090B20 #0B0C22 #0C0D24 #0E0E27 #100F29 #12102B #13112D #151330 #171432 #191534 #1B1637 " +
            "#1C1739 #1E183B #20193E #221A40 #291F41 #312341 #392742 #402B42 #492F42 #513342 #593741 #623B41 #6A3F40 " +
            "#73423F #7C463D #854A3C #8E4D3A #975137 #A05434 #AA5830"
    private val samples =
        mapOf(
            "light" to "#1F285A #272D61 #303167 #38366E #403B75 #493F7C #5D4C7F #7E607E #A1737A #C58572 #EB9666",
            "dark" to "#06091C #0A0C22 #0F0F27 #13112D #181434 #1D173A #221A40 #402B42 #623B41 #854A3C #AA5830",
        )

    @Test
    fun perSegmentRampMatchesTrueOKLab() {
        for ((theme, expectedStops) in mapOf("light" to light, "dark" to dark)) {
            val mid = sources.midStop(theme)
            val ramp = sources.ramp(theme)
            assertEquals(theme, expectedStops, ramp.joinToString(" ") { it.color.hex })
            assertEquals(33, ramp.size)
            assertEquals("mid stop appears once", 1, ramp.count { it.location == mid })
            val top = sources.color("sky-top", theme)
            val middle = sources.color("sky-mid", theme)
            val end = sources.color("sky-horizon", theme)
            assertEquals(middle.rgb(p3 = false).hex, ramp.first { it.location == mid }.color.hex)
            val expectedSamples = samples.getValue(theme).split(" ")
            for (i in 0..1000) {
                val x = i / 1000.0
                val lab = if (x <= mid) top.mixed(middle, x / mid) else middle.mixed(end, (x - mid) / (1 - mid))
                val hi = ramp.indexOfFirst { it.location >= x }
                val lo = max(0, hi - 1)
                val f = if (hi == lo) 0.0 else (x - ramp[lo].location) / (ramp[hi].location - ramp[lo].location)
                val interpolated = ramp[lo].color.mixed(ramp[hi].color, f)
                val expected = lab.rgb(p3 = false)
                interpolated.values.zip(expected.values).forEach { (a, b) ->
                    assertTrue(
                        "$theme x=$x",
                        abs(a - b) <= 0.25 / 255.0,
                    )
                }
                if (i % 100 == 0) assertEquals("$theme sample $i", expectedSamples[i / 100], expected.hex)
            }
        }
    }

    @Test
    fun segmentLocationsAreComputedLikeIOS() {
        val ramp = sources.ramp("light")
        assertEquals(0.034375, ramp[1].location, 0.0)
        assertEquals(0.55, ramp[16].location, 0.0)
        assertEquals(0.55 + 0.028125, ramp[17].location, 1e-15)
        val darkRamp = sources.ramp("dark")
        assertEquals(0.6, darkRamp[16].location, 0.0)
        assertEquals(1.0, darkRamp.last().location, 0.0)
    }
}
