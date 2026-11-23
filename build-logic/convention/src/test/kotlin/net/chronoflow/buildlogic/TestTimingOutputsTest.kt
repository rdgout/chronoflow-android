package net.chronoflow.buildlogic

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.gradle.api.tasks.testing.Test as GradleTest

class TestTimingOutputsTest {
    private val project = ProjectBuilder.builder().build()

    @Test
    fun executingATestReplacesOldTimingsAndMarksExecution() {
        project.configureTestTimingOutputs()
        val task = project.tasks.register("test", GradleTest::class.java).get()
        val directory =
            project.layout.buildDirectory
                .dir("cf-timings/test")
                .get()
                .asFile
        directory.mkdirs()
        val oldTiming = directory.resolve("old.json").apply { writeText("{}") }
        val marker = directory.resolve("execution.started")
        assertThat(marker.exists()).isFalse()

        task.actions.first().execute(task)

        assertThat(oldTiming.exists()).isFalse()
        assertThat(marker.isFile).isTrue()
    }

    @Test
    fun timingMeasurementsCannotBeRestoredFromTheBuildCache() {
        val existing = project.tasks.register("test", GradleTest::class.java).get()
        project.configureTestTimingOutputs()
        val registeredLater = project.tasks.register("testReleaseUnitTest", GradleTest::class.java).get()

        for (task in listOf(existing, registeredLater)) {
            val outputs = task.outputs as TaskOutputsInternal
            val taskInternal = task as TaskInternal
            val cachingAllowed =
                outputs.cacheIfSpecs.all { it.isSatisfiedBy(taskInternal) } &&
                    outputs.doNotCacheIfSpecs.none { it.isSatisfiedBy(taskInternal) }
            assertThat(cachingAllowed).isFalse()
            assertThat(outputs.upToDateSpec.isSatisfiedBy(taskInternal)).isTrue()
            assertThat(
                outputs.files.contains(
                    project.layout.buildDirectory
                        .dir("cf-timings/${task.name}")
                        .get()
                        .asFile,
                ),
            ).isTrue()
        }
    }
}
