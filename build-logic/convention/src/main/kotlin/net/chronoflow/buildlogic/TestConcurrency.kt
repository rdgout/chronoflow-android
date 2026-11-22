package net.chronoflow.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

abstract class TestWorkerSlots : BuildService<BuildServiceParameters.None>

internal fun Project.configureTestConcurrency() {
    val slots =
        gradle.sharedServices.registerIfAbsent("cfTestWorkerSlots", TestWorkerSlots::class.java) {
            maxParallelUsages.set((Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4))
        }
    tasks.withType<Test>().configureEach {
        // Each fork has its own Robolectric/Compose caches; extra forks multiply cold setup and CPU contention.
        maxParallelForks = 1
        usesService(slots)
    }
}
