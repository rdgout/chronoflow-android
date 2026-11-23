package net.chronoflow.konsist

import assertk.all
import assertk.assertFailure
import assertk.assertions.messageContains
import org.junit.Test
import java.io.File

class ModuleGraphTest {
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
}
