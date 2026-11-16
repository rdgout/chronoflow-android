package net.chronoflow.tools.tokengen

import java.nio.file.Path

fun welcomeTokens(root: Path): TokenSet {
    val board = root.design("Main.dc.html")
    val rules =
        listOf(
            Triple("topPadding", """padding: (\d+)px 24px 40px""", "64"),
            Triple("gutter", """padding: 64px (\d+)px 40px""", "24"),
            Triple("bottomPadding", """padding: 64px 24px (\d+)px""", "40"),
            Triple("groupGap", """column; gap: (16)px;""", "16"),
            Triple("actionGap", """column; gap: (12)px;""", "12"),
            Triple("brandGap", """align-items: center; gap: (\d+)px;""", "8"),
            Triple("moonSize", """aria-hidden="true" width="(\d+)"""", "28"),
            Triple("moonStroke", """stroke-width="(\d+)"""", "2"),
            Triple("moonViewBox", """viewBox="0 0 (\d+) 24"""", "24"),
            Triple("eyebrowMargin", """margin: (\d+)px 0 0""", "56"),
            Triple("cardVerticalPadding", """padding: (\d+)px 20px""", "16"),
            Triple("cardHorizontalPadding", """padding: 16px (\d+)px""", "20"),
            Triple("cardRadius", """border-radius: (22)px""", "22"),
            Triple("buttonHeight", """height: (52)px""", "52"),
            Triple("introRise", """translateY\((\d+)px\)""", "14"),
        )
    return TokenSet(
        rules.map { (name, pattern, expected) ->
            val values = captures(pattern, board)
            if (values.isEmpty() ||
                !values.all { it == expected }
            ) {
                throw GenerationError("Welcome board differs from plan: $name")
            }
            Metric(name, expected.toDouble())
        },
    )
}
