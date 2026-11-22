package net.chronoflow.buildlogic

import org.gradle.api.internal.TaskInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.gradle.api.tasks.testing.Test as GradleTest

class TestConcurrencyTest {
    @Test fun jvmAndAndroidTasksInDifferentProjectsShareTheSameWorkerLimit() {
        val root = ProjectBuilder.builder().build()
        val child =
            ProjectBuilder
                .builder()
                .withParent(root)
                .withName("android")
                .build()
        val jvm = root.tasks.register("test", GradleTest::class.java).get()
        jvm.maxParallelForks = 7
        jvm.maxHeapSize = "512m"
        root.configureTestConcurrency()
        child.configureTestConcurrency()
        val android = child.tasks.register("testDebugUnitTest", GradleTest::class.java).get()
        val timing = root.tasks.register("timingTest", GradleTest::class.java).get()
        assertEquals(1, jvm.maxParallelForks)
        assertEquals(1, android.maxParallelForks)
        assertEquals("3g", jvm.maxHeapSize)
        assertEquals("3g", android.maxHeapSize)
        assertEquals("3g", timing.maxHeapSize)
        val slot =
            root.gradle.sharedServices.registrations
                .getByName("cfTestWorkerSlots")
        assertEquals((Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4), slot.maxParallelUsages.get())
        assertTrue((jvm as TaskInternal).requiredServices.isServiceRequired(slot.service))
        assertTrue((android as TaskInternal).requiredServices.isServiceRequired(slot.service))
    }
}
