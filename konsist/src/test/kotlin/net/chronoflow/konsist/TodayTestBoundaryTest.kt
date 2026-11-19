package net.chronoflow.konsist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class TodayTestBoundaryTest {
    @Test fun todayNavigationKeepsOnlyTheHeroBackScreenBoundary() {
        val source =
            File(
                projectRoot(),
                "app/src/testDebug/kotlin/net/chronoflow/app/shell/TodayNavigationTest.kt",
            ).readText()
        assertFalse("Today action logic must not boot AppGraph", source.contains("import net.chronoflow.app.AppGraph"))
        assertFalse(
            "The tab boundary does not need RootContent",
            source.contains("import net.chronoflow.app.RootContent"),
        )
        val cases = Regex("@Test\\s+fun\\s+(\\w+)").findAll(source).map { it.groupValues[1] }.toList()
        assertEquals(listOf("heroPushesOnTodayAndSystemBackReturns"), cases)
    }
}
