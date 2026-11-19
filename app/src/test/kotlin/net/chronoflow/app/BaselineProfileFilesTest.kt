package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isGreaterThan
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards against an empty or stale committed profile, not against coverage drift. */
class BaselineProfileFilesTest {
    private val dir = File("src/main/generated/baselineProfiles")

    @Test
    fun baselineProfileCoversTheStartupAndTimelinePaths() {
        val lines = File(dir, "baseline-prof.txt").readLines()
        listOf(
            "Lnet/chronoflow/app/MainActivity;",
            "Lnet/chronoflow/feature/trip/page/TripPageKt;",
            "Lnet/chronoflow/feature/trip/page/TripPageScreenKt;",
            "Lnet/chronoflow/feature/trip/page/DayHeaderKt;",
            "Lnet/chronoflow/feature/trip/page/SummaryCardKt;",
            "Lnet/chronoflow/core/plan/drawing/DayDrawingBuilder;",
            "Lnet/chronoflow/feature/trip/timeline/DayDrawingReplayKt;",
        ).forEach { name -> assertTrue("$name is missing from baseline-prof.txt", lines.any { it.contains(name) }) }
    }

    @Test
    fun baselineProfileCoversTheListModePath() {
        val profile = File(dir, "baseline-prof.txt").readText()
        assertTrue(
            "DayListRowKt is missing from baseline-prof.txt",
            profile.contains("Lnet/chronoflow/feature/trip/list/DayListRowKt;"),
        )
    }

    @Test
    fun startupProfileHoldsTheMainActivity() {
        val file = File(dir, "startup-prof.txt")
        assertThat(file.length()).isGreaterThan(0L)
        assertTrue(file.readLines().any { it.contains("Lnet/chronoflow/app/MainActivity;") })
    }
}
