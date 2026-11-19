package net.chronoflow.konsist

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class TodayTestBoundaryTest {
    @Test fun todayActionsKeepNavigationOnTheJvm() {
        val source =
            File(
                projectRoot(),
                "app/src/test/kotlin/net/chronoflow/app/shell/TodayActionsTest.kt",
            ).readText()
        assertFalse("Today action logic must not boot AppGraph", source.contains("import net.chronoflow.app.AppGraph"))
        assertFalse(
            "The tab boundary does not need RootContent",
            source.contains("import net.chronoflow.app.RootContent"),
        )
        assertFalse("Today actions must not use a Robolectric runner", source.contains("org.robolectric"))
        assertFalse("Today actions must not use Compose tests", source.contains("androidx.compose.ui.test"))
    }
}
