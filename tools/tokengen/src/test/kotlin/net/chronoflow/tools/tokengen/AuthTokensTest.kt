package net.chronoflow.tools.tokengen

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AuthTokensTest {
    private val board = repoRoot.design("AuthSignIn.dc.html")
    private val bundle = repoRoot.design("ds/chronoflow/components/bundle.js")
    private val css = repoRoot.design("ds/chronoflow/components/bundle.css")
    private val tokensText = repoRoot.design("ds/chronoflow/tokens.json")

    private fun contrast(
        a: OKLab,
        b: OKLab,
    ): Double {
        fun luminance(color: OKLab): Double {
            val channels =
                color.rgb(p3 = false).values.map {
                    if (it <=
                        0.04045
                    ) {
                        it / 12.92
                    } else {
                        ((it + 0.055) / 1.055).pow(2.4)
                    }
                }
            return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
        }
        val first = luminance(a)
        val second = luminance(b)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    @Test
    fun authMixUsesOKLabAndPreservesBothAppearances() {
        for (theme in listOf("light", "dark")) {
            for (
            (text, background) in
            listOf(
                "card-foreground" to "card",
                "card-foreground" to "auth-error-background",
                "muted-foreground" to "card",
                "primary-foreground" to "primary",
                "dawn-text" to "card",
                "destructive" to "card",
                "foreground" to "card",
                "foreground" to "background",
            )
            ) {
                val ratio = contrast(sources.color(text, theme), sources.color(background, theme))
                assertTrue("$text on $background, $theme: $ratio", ratio >= 4.5)
            }
            for ((border, background) in listOf("input" to "card", "input" to "background", "border" to "card")) {
                val ratio = contrast(sources.color(border, theme), sources.color(background, theme))
                assertTrue(
                    "Re-review contrast accepted, OQ 33 (user 2026-11-06), when board borders change: $ratio",
                    ratio < 3,
                )
            }
            val expected = sources.color("card", theme).mixed(sources.color("destructive", theme), 0.1)
            assertEquals(expected.rgb(p3 = true).hex, sources.color("auth-error-background", theme).rgb(p3 = true).hex)
            assertEquals(0.35, sources.color("auth-error-border", theme).alpha, 0.0)
            assertNotEquals(
                sources.color("card", theme).rgb(p3 = true).hex,
                sources.color("card-foreground", theme).rgb(p3 = true).hex,
            )
        }
    }

    @Test
    fun authGuestFooterExceptionStillMatchesTheBoardPairAndTypography() {
        val footer = captures("""<a href="GuestFlight.dc.html" style="([^"]+)"""", board)
        assertEquals(1, footer.size)
        for (declaration in listOf(
            "background: var(--sky-copy-background)",
            "color: var(--sky-copy)",
            "font-size: 14px",
            "line-height: 20px",
            "font-weight: 500",
        )) {
            assertTrue(
                "Re-review sign-in.guest contrast accepted, OQ 33 (user 2026-11-06), when its design changes",
                footer[0].contains(declaration),
            )
        }
    }

    private enum class Source { Board, Bundle, Css }

    @Test
    fun authSourceChangesCannotHideBehindUnrelatedDeclarations() {
        val json = parseJson(tokensText).obj()
        val original = authTokens(board, bundle, css, json)
        val mutations =
            listOf(
                Triple(Source.Board, "column; gap: 8px; margin-top: 32px", "column; gap: 10px; margin-top: 32px"),
                Triple(Source.Board, "space-between; gap: 8px", "space-between; gap: 10px"),
                Triple(Source.Board, "space-between; height: 44px", "space-between; height: 46px"),
                Triple(Source.Board, "width: 44px; height: 44px", "width: 46px; height: 44px"),
                Triple(Source.Board, "gap: 6px; font-family", "gap: 7px; font-family"),
                Triple(Source.Board, "width=\"16\" height=\"16\"", "width=\"17\" height=\"16\""),
                Triple(Source.Board, "padding: 20px; border-radius: 26px", "padding: 21px; border-radius: 26px"),
                Triple(Source.Board, "margin-top: 1px; color", "margin-top: 3px; color"),
                Triple(Source.Board, "font-size: 18px; line-height: 24px", "font-size: 19px; line-height: 24px"),
                Triple(Source.Board, "font-size: 36px; line-height: 40px", "font-size: 38px; line-height: 40px"),
                Triple(Source.Board, "font-size: 16px; line-height: 24px", "font-size: 17px; line-height: 24px"),
                Triple(
                    Source.Board,
                    "font-size: 14px; line-height: 20px; font-weight: 500",
                    "font-size: 14px; line-height: 22px; font-weight: 500",
                ),
                Triple(
                    Source.Board,
                    "font-weight: 600; color: var(--dawn-text)",
                    "font-weight: 700; color: var(--dawn-text)",
                ),
                Triple(Source.Board, "font-size: 12px; line-height: 16px", "font-size: 13px; line-height: 16px"),
                Triple(Source.Board, "letter-spacing: -0.01em", "letter-spacing: -0.02em"),
                Triple(
                    Source.Board,
                    "font-family: var(--font-display); font-weight: 400",
                    "font-family: var(--font-display); font-weight: 500",
                ),
                Triple(Source.Board, "font-family: var(--font-sans)", "font-family: var(--font-display)"),
                Triple(
                    Source.Bundle,
                    "focus-visible:ring-2 focus-visible:ring-ring",
                    "focus-visible:ring-3 focus-visible:ring-ring",
                ),
                Triple(Source.Bundle, "mt-0.5 size-4 shrink-0", "mt-1 size-4 shrink-0"),
                Triple(Source.Bundle, "disabled:opacity-50", "disabled:opacity-40"),
                Triple(
                    Source.Bundle,
                    "flex items-center gap-1.5 text-sm text-destructive",
                    "flex items-center gap-2 text-sm text-destructive",
                ),
                Triple(Source.Css, "--font-weight-normal:400", "--font-weight-normal:500"),
            )
        for ((source, before, after) in mutations) {
            val input =
                when (source) {
                    Source.Board -> board
                    Source.Bundle -> bundle
                    Source.Css -> css
                }
            assertTrue("Mutation must change its source: $before", input.contains(before))
            val changed = input.replace(before, after)
            try {
                val output =
                    authTokens(
                        if (source == Source.Board) changed else board,
                        if (source == Source.Bundle) changed else bundle,
                        if (source == Source.Css) changed else css,
                        json,
                    )
                assertNotEquals("Undetected auth drift: $before", original, output)
            } catch (_: GenerationError) {
                // Rejected, which is the other accepted outcome.
            }
        }
    }

    @Test
    fun familyRenamesAreRejected() {
        for ((before, after) in listOf("'Figtree'," to "'OtherSans',", "'Fraunces'," to "'OtherDisplay',")) {
            assertTrue(tokensText.contains(before))
            val changed: JsonObject = parseJson(tokensText.replace(before, after)).obj()
            assertThrows(GenerationError::class.java) { authTokens(board, bundle, css, changed) }
        }
    }
}
