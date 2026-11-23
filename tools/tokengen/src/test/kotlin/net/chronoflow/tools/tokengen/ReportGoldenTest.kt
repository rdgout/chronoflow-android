package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportGoldenTest {
    private val report by lazy { String(tokenOutputs(repoRoot).getValue(REPORT_PATH)) }

    @Test
    fun reportMatchesTheIOSReportRowForRow() {
        val ios = parseReport(iosGolden("TokenGenReport.md"))
        val ours = parseReport(report)
        assertEquals(50, ios.size)
        for ((index, row) in ios.withIndex()) {
            val mine = ours[index]
            assertEquals(row.name, mine.name)
            assertEquals(row.name, row.lightHex, mine.lightHex)
            assertEquals(row.name, row.darkHex, mine.darkHex)
            p3Channels(row.lightP3).zip(p3Channels(mine.lightP3)).forEach { (a, b) ->
                assertEquals(row.name, a, b, 1e-9)
            }
            p3Channels(row.darkP3).zip(p3Channels(mine.darkP3)).forEach { (a, b) -> assertEquals(row.name, a, b, 1e-9) }
        }
        assertEquals(listOf("systemSelected", "wizardPickerFill"), ours.drop(50).map { it.name })
        assertEquals(52, ours.size)
        assertTrue(report.contains("Light sky: 33 stops, including exact mid 0.55;"))
        assertTrue(report.contains("Dark sky: 33 stops, including exact mid 0.6;"))
    }
}
