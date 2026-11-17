package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope

fun noHapticsInLaunch(scope: KoScope) {
    val forbidden =
        listOf(
            "androidx.compose.ui.hapticfeedback",
            "net.chronoflow.core.designsystem.haptics",
            "android.view.HapticFeedbackConstants",
        )
    val bad =
        scope.files
            .filter {
                moduleOf(it.path) == "feature/launch" && sourceSetOf(it.path) == "main"
            }.filter { file ->
                file.imports.any { imported -> forbidden.any { imported.name.startsWith(it) } } ||
                    file.text.contains("performHapticFeedback(") || file.text.contains("LocalHaptics")
            }.map { it.path }
    if (bad.isNotEmpty()) throw AssertionError("noHapticsInLaunch violated:\n${bad.joinToString("\n")}")
}
