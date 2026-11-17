package net.chronoflow.feature.launch.model

import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.generated.LaunchWordmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.max

@Logic
class LaunchWordmarkTests {
    @Test fun wordmarkUsesCoreTextAdvanceBaselineAndOverhang() {
        with(LaunchWordmark) {
            assertEquals(180.048, ADVANCE_DP.toDouble(), 0.001)
            assertEquals(31.568, BASELINE_DP.toDouble(), 0.001)
            val origin = (WIDTH_DP - ADVANCE_DP) / 2
            val overhang = max(0f, max(-(INK_LEFT_DP - origin), INK_RIGHT_DP - origin - ADVANCE_DP))
            assertEquals(ceil(ADVANCE_DP + 2 * overhang), WIDTH_DP)
            assertTrue(INK_LEFT_DP >= 0 && INK_RIGHT_DP <= WIDTH_DP)
            assertTrue(INK_TOP_DP >= 0 && INK_BOTTOM_DP <= HEIGHT_DP)
            assertTrue(PATH_DATA.count { it == 'M' } >= 10)
            assertTrue(Regex("[-MLCZ0-9. ]+").matches(PATH_DATA))
        }
    }
}
