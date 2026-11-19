package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Files

class DesignSourceTest {
    private val sourceFiles = listOf(TOKENS_PATH, THEME_PATH)

    private inline fun <T> withTempRoot(
        noinline change: (String, String) -> String,
        block: (java.nio.file.Path) -> T,
    ): T {
        val root = tempRoot(sourceFiles, change)
        try {
            return block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun conflictingDesignSourcesAreRejected() {
        withTempRoot({ file, text ->
            if (file == THEME_PATH) text.replace("--dawn: oklch(0.72 0.14 55)", "--dawn: oklch(0.73 0.14 55)") else text
        }) { root ->
            assertThrows(GenerationError::class.java) { DesignSource(root) }
        }
    }

    @Test
    fun skyMidStopChangeIsRejected() {
        withTempRoot({ file, text ->
            if (file == THEME_PATH) text.replaceFirst("--sky-mid-stop: 55%;", "--sky-mid-stop: 50%;") else text
        }) { root ->
            assertThrows(GenerationError::class.java) { DesignSource(root).midStop("light") }
        }
    }

    @Test
    fun aliasCycleIsRejected() {
        withTempRoot({ file, text ->
            if (file == TOKENS_PATH) {
                text.replace(
                    Regex("\"name\": \"ring\",\\s*\"value\": \\{[^}]*\\}"),
                    "\"name\": \"ring\", \"value\": \"{sidebar-ring}\"",
                )
            } else {
                text
            }
        }) { root ->
            assertThrows(GenerationError::class.java) { DesignSource(root) }
        }
    }

    @Test
    fun wizardExclusionSurfacesUseTheBoardsOKLabMixInBothAppearances() {
        for (appearance in listOf("light", "dark")) {
            val dawn = sources.color("dawn", appearance)
            val expected = sources.color("card", appearance).mixed(dawn, 0.1)
            val background = sources.color("wizard-notice-background", appearance)
            val border = sources.color("wizard-notice-border", appearance)
            assertEquals(expected.rgb(p3 = false).hex, background.rgb(p3 = false).hex)
            assertEquals(dawn.rgb(p3 = false).hex, border.rgb(p3 = false).hex)
            assertEquals(0.4, border.alpha, 0.0)
        }
    }

    @Test
    fun camelCaseMatchesFoundationCapitalized() {
        assertEquals("skyCopyBackground", camelCase("sky-copy-background"))
        assertEquals("surface2", camelCase("surface-2"))
        assertEquals("radius2Xl", camelCase("radius-2xl"))
        assertEquals("screenBodyLg", camelCase("screen-body-lg"))
    }

    @Test
    fun invalidOklchIsRejected() {
        assertThrows(GenerationError::class.java) { OKLab.fromCss("oklch(0.5 0.1)") }
        assertEquals(true, Files.exists(repoRoot.resolve(TOKENS_PATH)))
    }
}
