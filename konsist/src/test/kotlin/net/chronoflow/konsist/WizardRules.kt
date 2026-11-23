package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

private val retiredBoards = setOf("WizardQBipolar", "WizardQCaffeine", "WizardQClot", "WizardQEye", "WizardQPregnant")
private val retiredFields =
    setOf(
        "bipolar",
        "eye_disease",
        "pregnancy",
        "vte",
        "sleep_phase_disorder",
        "oral_contraception",
        "caffeine_ruled_out",
    )

private class WizardLiteral {
    val text = StringBuilder()
    var interpolated = false
}

private fun retiredLiterals(source: String): Set<String> {
    val lexer = KotlinLexer()
    val literals = mutableListOf<WizardLiteral>()
    val matches = mutableSetOf<String>()
    lexer.start(source)
    while (lexer.tokenType != null) {
        val text = source.substring(lexer.tokenStart, lexer.tokenEnd)
        when (lexer.tokenType) {
            KtTokens.OPEN_QUOTE -> {
                literals.add(WizardLiteral())
            }

            KtTokens.REGULAR_STRING_PART -> {
                literals.lastOrNull()?.text?.append(text)
            }

            KtTokens.ESCAPE_SEQUENCE -> {
                val value =
                    if (text.startsWith("\\u")) {
                        text
                            .drop(2)
                            .toIntOrNull(16)
                            ?.toChar()
                            ?.toString()
                    } else {
                        null
                    }
                literals.lastOrNull()?.text?.append(value ?: text)
            }

            KtTokens.SHORT_TEMPLATE_ENTRY_START, KtTokens.LONG_TEMPLATE_ENTRY_START -> {
                literals.lastOrNull()?.interpolated = true
            }

            KtTokens.CLOSING_QUOTE -> {
                val literal = literals.removeLastOrNull()
                if (literal != null && !literal.interpolated && literal.text.toString() in retiredFields) {
                    matches.add(literal.text.toString())
                }
            }
        }
        lexer.advance()
    }
    return matches
}

fun noRetiredHealthQuestions(scope: KoScope) {
    val violations =
        scope.files
            .filter { file ->
                val module = moduleOf(file.path)
                val source = sourceSetOf(file.path)
                (module?.startsWith("feature/") == true && source in setOf("main", "debug")) ||
                    (module == "core/wizard" && source == "main")
            }.flatMap { file ->
                val names = retiredBoards.filter { it in file.text } + retiredLiterals(file.text)
                names.map { "${file.path}: $it" }
            }
    if (violations.isNotEmpty()) {
        throw AssertionError("noRetiredHealthQuestions violated:\n${violations.sorted().joinToString("\n")}")
    }
}
