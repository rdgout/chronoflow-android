package net.chronoflow.konsist

import com.lemonappdev.konsist.api.Konsist
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class TodaySessionKeyTest {
    @Test fun todayTabPassesTheSessionEpochKeyToItsModel() {
        val initializer = functionCode.substringAfter("valmodel:TodayViewModel=", "")
        assertTrue("TodayTab must obtain its model through viewModel", initializer.startsWith("viewModel("))
        val opening = initializer.indexOf('(')
        var depth = 1
        var end = opening + 1
        while (end < initializer.length && depth > 0) {
            when (initializer[end]) {
                '(' -> depth++
                ')' -> depth--
            }
            end++
        }
        val arguments = initializer.substring(opening + 1, end - 1)
        val key = Regex("""(?:^|,)key=todayViewModelKey\(session\?\.snapshot\?\.epoch\)(?=,|$)""")
        assertTrue("TodayTab must pass its current session epoch as the model key", key.containsMatchIn(arguments))
    }

    companion object {
        private lateinit var functionCode: String

        @BeforeClass @JvmStatic
        fun parseTodayModelInitializer() {
            val tab =
                Konsist
                    .scopeFromFile("app/src/main/kotlin/net/chronoflow/app/shell/TodayTab.kt")
                    .functions()
                    .single { it.name == "TodayTab" }
            val lexer = KotlinLexer()
            lexer.start(tab.text)
            functionCode =
                buildString {
                    while (lexer.tokenType != null) {
                        val token = lexer.tokenType
                        if (!KtTokens.COMMENTS.contains(token) && token != KtTokens.REGULAR_STRING_PART &&
                            token != KtTokens.ESCAPE_SEQUENCE
                        ) {
                            append(lexer.tokenText)
                        }
                        lexer.advance()
                    }
                }.filterNot(Char::isWhitespace)
        }
    }
}
