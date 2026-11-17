package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope
import java.io.File

private fun checkDesignRule(
    name: String,
    violations: List<String>,
) {
    if (violations.isNotEmpty()) throw AssertionError("$name violated:\n${violations.joinToString("\n")}")
}

private fun KoScope.designSources() =
    files.filter {
        sourceSetOf(it.path) == "main" &&
            !it.path.endsWith(".generated.kt") &&
            !it.path.replace('\\', '/').contains("/generated/")
    }

fun noMaterialColorSchemeOutsideDesignSystem(scope: KoScope) {
    val bad =
        scope
            .designSources()
            .filter { file ->
                val path = file.path.replace('\\', '/')
                val allowed =
                    moduleOf(path) == "core/designsystem" &&
                        (path.contains("/designsystem/theme/") || path.contains("/designsystem/system/"))
                !allowed &&
                    Regex("""MaterialTheme\s*\.\s*colorScheme""").containsMatchIn(file.text)
            }.map { it.path }
    checkDesignRule("noMaterialColorSchemeOutsideDesignSystem", bad)
}

fun noRawColorsOutsideDesignSystem(scope: KoScope) {
    val constructor = Regex("""\bColor\s*\(\s*(0[xX][\da-fA-F]+|[+\-\d.]|(?:red|green|blue)\s*=)""")
    val named = Regex("""\bColor\.(\w+)""")
    val bad =
        scope
            .designSources()
            .filter { moduleOf(it.path) != "core/designsystem" }
            .filter { file ->
                constructor.containsMatchIn(file.text) ||
                    named.findAll(file.text).any {
                        it.groupValues[1] !in setOf("Transparent", "Unspecified")
                    }
            }.map { it.path }
    checkDesignRule("noRawColorsOutsideDesignSystem", bad)
}

fun featureDpLiteralsComeFromBoardMetrics(scope: KoScope) {
    val literal = Regex("""(?<![\w.])(\d+(?:\.\d+)?)\.(dp|sp)\b""")
    val allowed =
        scope.objects().filter {
            it.name.endsWith("Metrics") &&
                it.hasAnnotationWithName("BoardMetrics")
        }
    val features =
        scope.designSources().filter {
            moduleOf(it.path)?.startsWith("feature/") == true || moduleOf(it.path) == "core/ui"
        }
    val bad =
        features.flatMap { file ->
            var text = file.text
            allowed.filter { it.path == file.path }.forEach { text = text.replace(it.text, "") }
            literal
                .findAll(text)
                .filter { it.groupValues[1].toDouble() != 0.0 }
                .map { "${file.path}: ${it.value}" }
                .toList()
        }
    checkDesignRule("featureDpLiteralsComeFromBoardMetrics", bad)
}

fun boardMetricsNamesExist(scope: KoScope) {
    val pattern = Regex("""@(?:[\w.]+\.)?BoardMetrics\(([^)]*)\)""")
    val quoted = Regex("\"([^\"]+)\"")
    val bad =
        scope.designSources().flatMap { file ->
            pattern
                .findAll(file.text)
                .flatMap { annotation ->
                    quoted.findAll(annotation.groupValues[1]).map { it.groupValues[1] }
                }.filter { !File(projectRoot(), "_design/canvas/$it.dc.html").exists() }
                .map { "${file.path}: $it" }
                .toList()
        }
    checkDesignRule("boardMetricsNamesExist", bad)
}

fun noMaterialIconsLibrary(scope: KoScope) {
    val bad =
        scope.files
            .filter { file ->
                file.imports.any { it.name.startsWith("androidx.compose.material.icons") }
            }.map { it.path }
    checkDesignRule("noMaterialIconsLibrary", bad)
}

fun secureFieldStateIsNeverSaved(scope: KoScope) {
    val bad =
        scope
            .designSources()
            .filter { file ->
                file.text.contains("CfSecureTextField(") &&
                    listOf(
                        "rememberTextFieldState(",
                        "rememberSaveable",
                        "TextFieldState.Saver",
                    ).any { file.text.contains(it) }
            }.map { it.path }
    checkDesignRule("secureFieldStateIsNeverSaved", bad)
}

fun designSystemPrimitivesHaveGuards(
    scope: KoScope,
    guards: File = File(projectRoot(), "core/ui/src/test/kotlin/net/chronoflow/core/ui/designsystem"),
) {
    val coverage =
        if (guards.exists()) {
            guards.walkTopDown().filter { it.name.endsWith("GuardsTest.kt") }.joinToString("\n") { it.readText() }
        } else {
            ""
        }
    val bad =
        scope
            .functions()
            .filter { function ->
                val path = function.path.replace('\\', '/')
                sourceSetOf(path) == "main" &&
                    moduleOf(path) == "core/designsystem" &&
                    (path.contains("/component/") || path.contains("/system/")) &&
                    function.hasAnnotationWithName("Composable") &&
                    function.name.startsWith("Cf") &&
                    !function.hasPrivateModifier &&
                    !function.hasInternalModifier
            }.filter { !coverage.contains(it.name) }
            .map { "${it.path}: ${it.name}" }
    checkDesignRule("designSystemPrimitivesHaveGuards", bad)
}
