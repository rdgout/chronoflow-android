package net.chronoflow.core.plan.drawing

import net.chronoflow.core.testing.serialize
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * S3 day 1 as an op list, en, light, density 1.0. The goldens are reviewed against the `Plan` and `PlanDark` boards and
 * the iOS screenshots; a mismatch writes the actual output under `build/golden/` and fails, it is never re-recorded
 * to make the test pass.
 */
class DrawOrderGoldenTest {
    @Test fun s3Day1At411KeepsTheIosDrawOrder() = check(411)

    @Test fun s3Day1At841KeepsTheIosDrawOrder() = check(841)

    private fun check(window: Int) {
        val fixture = DrawingFixtures.prepared("S3")
        val day = fixture.plan.days.first()
        val drawing =
            DrawingFixtures.build(
                day,
                fixture.layouts.getValue(day.id),
                DrawingFixtures.key(day, fixture.plan.revision, window),
            )
        val actual = drawing.serialize()
        val name = "S3-day1-W$window.txt"
        val expected =
            DrawOrderGoldenTest::class.java.getResourceAsStream("/golden/drawing/$name")?.use {
                it.readBytes().decodeToString()
            }
        if (expected == actual) return
        val out = File(System.getProperty("user.dir"), "build/golden/$name")
        out.parentFile.mkdirs()
        out.writeText(actual)
        fail("$name ${if (expected == null) "is missing" else "differs"}; actual written to ${out.path}")
    }
}
