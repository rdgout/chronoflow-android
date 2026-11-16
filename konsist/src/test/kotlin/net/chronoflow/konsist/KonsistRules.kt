package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope

private const val ROOT = "net.chronoflow"

private val jvmCoreModules =
    setOf(
        "core/model",
        "core/plan",
        "core/sync",
        "core/reminders",
        "core/wizard",
        "core/network",
        "core/data-mock",
        "core/testing",
    )

private fun fail(
    rule: String,
    violations: List<String>,
) {
    if (violations.isNotEmpty()) {
        throw AssertionError("$rule violated:\n" + violations.joinToString("\n") { "  $it" })
    }
}

private class ScopedFile(
    val path: String,
    val module: String,
    val sourceSet: String?,
    val imports: List<String>,
    val text: String,
)

private fun KoScope.moduleFiles(): List<ScopedFile> =
    files.mapNotNull { file ->
        val module = moduleOf(file.path) ?: return@mapNotNull null
        ScopedFile(file.path, module, sourceSetOf(file.path), file.imports.map { it.name }, file.text)
    }

private fun List<String>.importsFrom(prefix: String) = filter { it.startsWith("$prefix.") }

fun featuresDoNotImportOtherFeatures(scope: KoScope) {
    val violations =
        scope.moduleFiles().filter { it.module.startsWith("feature/") }.flatMap { file ->
            val own = file.module.removePrefix("feature/").replace("-", "")
            file.imports
                .importsFrom("$ROOT.feature")
                .filterNot { it.startsWith("$ROOT.feature.$own.") }
                .map { "${file.path}: $it" }
        }
    fail("featuresDoNotImportOtherFeatures", violations)
}

fun jvmCoreModulesImportNoAndroidOrCompose(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module in jvmCoreModules || it.module.startsWith("tools/") }
            .flatMap { file ->
                file.imports
                    .filter { it.startsWith("android.") || it.startsWith("androidx.compose.") }
                    .map { "${file.path}: $it" }
            }
    fail("jvmCoreModulesImportNoAndroidOrCompose", violations)
}

fun onlyAppImportsFeatures(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module != "app" && !it.module.startsWith("feature/") }
            .flatMap { file -> file.imports.importsFrom("$ROOT.feature").map { "${file.path}: $it" } }
    fail("onlyAppImportsFeatures", violations)
}

fun noGlobalScope(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { file ->
                file.imports.any { it == "kotlinx.coroutines.GlobalScope" } ||
                    file.text.contains("GlobalScope.")
            }.map { it.path }
    fail("noGlobalScope", violations)
}

private val runBlockingRegex = Regex("\\brunBlocking\\b")

fun noRunBlockingOutsideTests(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet != null && !isTestSourceSet(it.sourceSet) }
            .filter { file ->
                file.imports.contains("kotlinx.coroutines.runBlocking") ||
                    runBlockingRegex.containsMatchIn(file.text)
            }.map { it.path }
    fail("noRunBlockingOutsideTests", violations)
}

fun noSleepInTests(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet != null && isTestSourceSet(it.sourceSet) }
            .filter { it.text.contains("Thread.sleep(") || it.text.contains("SystemClock.sleep(") }
            .map { it.path }
    fail("noSleepInTests", violations)
}

fun viewModelsEndWithViewModel(scope: KoScope) {
    val violations =
        scope
            .classes()
            .filter { moduleOf(it.path) != null }
            .filter { it.hasParentWithName("ViewModel") || it.hasParentWithName("AndroidViewModel") }
            .filterNot { it.name.endsWith("ViewModel") }
            .map { "${it.path}: ${it.name}" }
    fail("viewModelsEndWithViewModel", violations)
}

fun routesAreSerializable(scope: KoScope) {
    class Candidate(
        val path: String,
        val name: String,
        val isRoute: Boolean,
        val serializable: Boolean,
    )

    val candidates =
        scope.classes().map {
            Candidate(
                it.path,
                it.name,
                it.hasParentWithName("NavKey", indirectParents = true),
                it.hasAnnotationWithName("Serializable"),
            )
        } +
            scope.interfaces().map {
                Candidate(
                    it.path,
                    it.name,
                    it.hasParentWithName("NavKey", indirectParents = true),
                    it.hasAnnotationWithName("Serializable"),
                )
            } +
            scope.objects().map {
                Candidate(
                    it.path,
                    it.name,
                    it.hasParentWithName("NavKey", indirectParents = true),
                    it.hasAnnotationWithName("Serializable"),
                )
            }
    val violations =
        candidates
            .filter { moduleOf(it.path) != null && it.isRoute && !it.serializable }
            .map { "${it.path}: ${it.name}" }
    fail("routesAreSerializable", violations)
}

private val literalTextRegex = Regex("""\b(Basic)?Text\(\s*(text\s*=\s*)?("|\"\"\")""")

fun noLiteralTextInText(scope: KoScope) {
    val violations =
        scope
            .functions()
            .filter { function ->
                val sourceSet = sourceSetOf(function.path)
                sourceSet != null && !isTestSourceSet(sourceSet)
            }.filterNot { function ->
                function.annotations.any {
                    it.name.startsWith("Preview") ||
                        it.name.endsWith("Preview")
                }
            }.filter { literalTextRegex.containsMatchIn(it.text) }
            .map { "${it.path}: ${it.name}" }
    fail("noLiteralTextInText", violations)
}
