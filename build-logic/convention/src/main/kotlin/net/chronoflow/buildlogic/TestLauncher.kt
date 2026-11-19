package net.chronoflow.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import java.time.Duration

internal class CfTestArguments(
    @get:Input val tier: Provider<String>,
    @get:Input val hangCapSeconds: Provider<String>,
    @get:Input val captureOnly: Provider<String>,
    @get:Internal val timingsDir: Provider<String>,
    @get:Internal val capturesDir: Provider<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf(
            "-Dcf.tier=${tier.get()}",
            "-Dcf.testHangCapSeconds=${hangCapSeconds.get()}",
            "-Dcf.captureOnly=${captureOnly.get()}",
            "-Dcf.timingsDir=${timingsDir.get()}",
            "-Dcf.capturesDir=${capturesDir.get()}",
            "-Drobolectric.graphicsMode=NATIVE",
            "-Drobolectric.pixelCopyRenderMode=hardware",
            "-Drobolectric.alwaysIncludeVariantMarkersInTestName=true",
        )
}

internal fun Project.configureTestLauncher() {
    configureTestTimingOutputs()
    val toolchains = extensions.getByType<JavaToolchainService>()
    val requestedTier = providers.gradleProperty("cf.tier").orElse("quick")
    val hangCapSeconds =
        providers
            .systemProperty("cf.testHangCapSeconds")
            .orElse(providers.gradleProperty("cf.testHangCapSeconds"))
            .orElse("60")
    val captureOnly = providers.gradleProperty("cf.captureOnly").orElse("")
    val capturesDir =
        rootProject.layout.buildDirectory
            .dir("captures")
            .map { it.asFile.absolutePath }
    tasks.withType<Test>().configureEach {
        val taskName = name
        javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        if (taskName.endsWith("UnitTest")) maxParallelForks = 1
        useJUnitPlatform()
        failOnNoDiscoveredTests.set(false)
        timeout.set(Duration.ofMinutes(10))
        val timings = layout.buildDirectory.dir("cf-timings/$taskName")
        jvmArgumentProviders.add(
            CfTestArguments(
                tier = if (taskName == "timingTest") providers.provider { "timing" } else requestedTier,
                hangCapSeconds = hangCapSeconds,
                captureOnly = captureOnly,
                timingsDir = timings.map { it.asFile.absolutePath },
                capturesDir = capturesDir,
            ),
        )
    }
    val bom = libs.library("junit-bom")
    addDependency("testRuntimeOnly", dependencies.platform(bom))
    addDependency("testRuntimeOnly", libs.library("junit-vintage-engine"))
    addDependency("testRuntimeOnly", libs.library("junit-platform-launcher"))
    if (path != ":core:testing-junit") addDependency("testRuntimeOnly", project(":core:testing-junit"))
}

internal fun Project.configureTestTimingOutputs() {
    tasks.withType<Test>().configureEach {
        val timings = layout.buildDirectory.dir("cf-timings/$name")
        outputs.dir(timings)
        outputs.doNotCacheIf("Test timings measure this host and must not be replayed from the build cache") { true }
        doFirst {
            val directory = timings.get().asFile
            directory.deleteRecursively()
            directory.mkdirs()
            // Prove execution even when the listener or JUnit report is missing.
            directory.resolve("execution.started").writeText("")
        }
    }
}
