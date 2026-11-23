package net.chronoflow.konsist

import assertk.all
import assertk.assertFailure
import assertk.assertions.messageContains
import org.junit.Test
import java.io.File

class ModuleGraphTest : KonsistTest() {
    private fun designSystemDependsOnNoProjectModule(root: File) {
        val file = File(root, "core/designsystem/build.gradle.kts")
        val bad =
            file.readLines().filter {
                it.contains("project(") &&
                    it.trim() != "testImplementation(project(\":core:testing\"))"
            }
        check(bad.isEmpty()) { "designSystemDependsOnNoProjectModule: $bad" }
    }

    @Test fun designSystemDependsOnNoProjectModule() = designSystemDependsOnNoProjectModule(projectRoot())

    @Test fun designSystemDependsOnNoProjectModuleRejectsPlantedViolation() {
        for (kind in listOf("ui", "testingCompose")) {
            assertFailure { designSystemDependsOnNoProjectModule(File(planted, "designsystem/$kind")) }
                .messageContains("designSystemDependsOnNoProjectModule")
        }
    }

    @Test fun designSystemDependsOnNoProjectModuleAllowsControl() =
        designSystemDependsOnNoProjectModule(
            File(projectRoot(), "konsist/src/test/resources/allowed/moduleGraph/designsystem"),
        )

    private val featureDependency = Regex("""project\(\s*":feature:|projects\.feature\.""")

    private fun walk(
        root: File,
        skipResources: Boolean,
    ): List<File> =
        root
            .walkTopDown()
            .onEnter { dir ->
                val rel = dir.relativeTo(root).invariantSeparatorsPath
                dir.name !in setOf("build", ".gradle", ".kotlin", ".git") &&
                    !(skipResources && rel == "konsist/src/test/resources")
            }.filter { it.isFile }
            .toList()

    private fun featuresDoNotDependOnFeatures(
        root: File,
        skipResources: Boolean,
    ) {
        val bad =
            walk(root, skipResources)
                .filter {
                    it.name == "build.gradle.kts" &&
                        it
                            .relativeTo(
                                root,
                            ).invariantSeparatorsPath
                            .contains("feature/")
                }.filter { featureDependency.containsMatchIn(it.readText()) }
                .map { it.relativeTo(root).path }
        check(bad.isEmpty()) { "feature modules depend on feature modules: $bad" }
    }

    private fun onlyAppDependsOnFeatures(
        root: File,
        skipResources: Boolean,
    ) {
        val bad =
            walk(root, skipResources)
                .filter { file ->
                    val rel = file.relativeTo(root).invariantSeparatorsPath
                    (file.name == "build.gradle.kts" && !rel.startsWith("app/")) ||
                        (rel.startsWith("build-logic/") && file.extension == "kt")
                }.filter { featureDependency.containsMatchIn(it.readText()) }
                .map { it.relativeTo(root).path }
        check(bad.isEmpty()) { "modules other than app depend on feature modules: $bad" }
    }

    private val planted get() = File(projectRoot(), "konsist/src/test/resources/violations/moduleGraph")

    @Test
    fun featureModulesDoNotDependOnFeatureModules() = featuresDoNotDependOnFeatures(projectRoot(), skipResources = true)

    @Test
    fun featureModulesDoNotDependOnFeatureModulesRejectsPlantedViolation() {
        assertFailure { featuresDoNotDependOnFeatures(planted, skipResources = false) }
            .messageContains("feature modules depend on feature modules")
    }

    @Test
    fun onlyAppDependsOnFeatureModules() = onlyAppDependsOnFeatures(projectRoot(), skipResources = true)

    @Test
    fun onlyAppDependsOnFeatureModulesRejectsPlantedViolation() {
        assertFailure { onlyAppDependsOnFeatures(planted, skipResources = false) }.all {
            messageContains("modules other than app depend on feature modules")
            messageContains("core/ui/build.gradle.kts".replace('/', File.separatorChar))
            messageContains("feature/today/build.gradle.kts".replace('/', File.separatorChar))
        }
    }

    private val dataMockLine = Regex("""project\(\s*":core:data-mock"\s*\)""")
    private val dataMockAllowed =
        listOf(
            Regex("""^debugImplementation\("""),
            Regex("""^"benchmarkReleaseImplementation"\("""),
            Regex("""^"nonMinifiedReleaseImplementation"\("""),
            Regex("""^"benchmarkImplementation"\("""),
            Regex("""^\.configureEach \{ project\.dependencies\.add\(name, project\(":core:data-mock"\)\) }$"""),
            Regex("""^(testImplementation|androidTestImplementation)\("""),
        )

    private fun dataMockOnlyInDebugAndPerf(
        root: File,
        skipResources: Boolean,
    ) {
        val bad =
            walk(root, skipResources)
                .filter { it.name == "build.gradle.kts" }
                .filter { !it.relativeTo(root).invariantSeparatorsPath.startsWith("core/data-mock/") }
                .flatMap { file ->
                    val lines = file.readLines()
                    lines.indices
                        .filter { dataMockLine.containsMatchIn(lines[it]) }
                        .filterNot { index ->
                            val line = lines[index].trim()
                            val inApp = file.relativeTo(root).invariantSeparatorsPath == "app/build.gradle.kts"
                            val fallback =
                                line.startsWith(".configureEach") &&
                                    lines.getOrNull(index - 1)?.trim()?.let {
                                        it.startsWith(".matching") &&
                                            it.contains("benchmarkReleaseImplementation") &&
                                            it.contains("nonMinifiedReleaseImplementation")
                                    } == true
                            dataMockAllowed.any { it.containsMatchIn(line) } && (inApp || !line.startsWith("debug")) &&
                                (!line.startsWith(".configureEach") || fallback)
                        }.map { "${file.relativeTo(root).path}:${it + 1}" }
                }.toList()
        check(bad.isEmpty()) { "core:data-mock is used outside debug and the perf variants: $bad" }
    }

    @Test
    fun dataMockOnlyInDebugAndPerf() = dataMockOnlyInDebugAndPerf(projectRoot(), skipResources = true)

    @Test
    fun dataMockOnlyInDebugAndPerfRejectsPlantedViolation() {
        assertFailure { dataMockOnlyInDebugAndPerf(File(planted, "dataMock"), skipResources = false) }
            .messageContains("core:data-mock is used outside debug and the perf variants")
    }

    private val metricsLine = Regex("""metrics[.-]performance""")

    private fun metricsPerformanceOnlyInDebugAndPerf(
        root: File,
        skipResources: Boolean,
    ) {
        val bad =
            walk(root, skipResources)
                .filter { it.name == "build.gradle.kts" }
                .flatMap { file ->
                    val lines = file.readLines()
                    lines.indices
                        .filter { metricsLine.containsMatchIn(lines[it]) }
                        .filterNot { index ->
                            val line = lines[index].trim()
                            val inApp = file.relativeTo(root).invariantSeparatorsPath == "app/build.gradle.kts"
                            val fallback =
                                line.startsWith(".configureEach") &&
                                    lines.getOrNull(index - 1)?.trim()?.let {
                                        it.startsWith(".matching") &&
                                            it.contains("benchmarkReleaseImplementation") &&
                                            it.contains("nonMinifiedReleaseImplementation")
                                    } == true
                            inApp && (line.startsWith("debugImplementation(") || fallback)
                        }.map { "${file.relativeTo(root).path}:${it + 1}" }
                }.toList()
        check(bad.isEmpty()) { "metrics-performance is used outside debug and the perf variants: $bad" }
    }

    @Test
    fun metricsPerformanceOnlyInDebugAndPerf() =
        metricsPerformanceOnlyInDebugAndPerf(projectRoot(), skipResources = true)

    @Test
    fun metricsPerformanceOnlyInDebugAndPerfRejectsPlantedViolation() {
        assertFailure {
            metricsPerformanceOnlyInDebugAndPerf(
                File(planted, "metricsPerformance"),
                skipResources = false,
            )
        }.messageContains("metrics-performance is used outside debug and the perf variants")
    }

    private val testingLine = Regex("""project\(\s*":core:testing"\s*\)""")
    private val testingAllowed =
        listOf(
            Regex("""^(testImplementation|androidTestImplementation|debugImplementation)\("""),
            Regex("""^\.configureEach \{ project\.dependencies\.add\(name, project\(":core:testing"\)\) }$"""),
        )

    /** `:core:testing` holds the fixtures: `:app` takes it only for tests, debug and the perf variants (plan.md 1d). */
    private fun testingOnlyInTestDebugAndPerf(
        root: File,
        skipResources: Boolean,
    ) {
        val bad =
            walk(root, skipResources)
                .filter { it.relativeTo(root).invariantSeparatorsPath == "app/build.gradle.kts" }
                .flatMap { file ->
                    val lines = file.readLines()
                    lines.indices
                        .filter { testingLine.containsMatchIn(lines[it]) }
                        .filterNot { index ->
                            val line = lines[index].trim()
                            val fallback =
                                line.startsWith(".configureEach") &&
                                    lines.getOrNull(index - 1)?.trim()?.let {
                                        it.startsWith(".matching") &&
                                            it.contains("benchmarkReleaseImplementation") &&
                                            it.contains("nonMinifiedReleaseImplementation")
                                    } == true
                            testingAllowed.any { it.containsMatchIn(line) } &&
                                (!line.startsWith(".configureEach") || fallback)
                        }.map { "${file.relativeTo(root).path}:${it + 1}" }
                }.toList()
        check(bad.isEmpty()) { "core:testing is used outside tests, debug and the perf variants: $bad" }
    }

    @Test
    fun testingOnlyInTestDebugAndPerf() = testingOnlyInTestDebugAndPerf(projectRoot(), skipResources = true)

    @Test
    fun testingOnlyInTestDebugAndPerfRejectsPlantedViolation() {
        assertFailure { testingOnlyInTestDebugAndPerf(File(planted, "testing"), skipResources = false) }
            .messageContains("core:testing is used outside tests, debug and the perf variants")
    }
}
