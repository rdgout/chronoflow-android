package net.chronoflow.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestLauncher() {
    val toolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<Test>().configureEach {
        javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        useJUnit()
    }
}
