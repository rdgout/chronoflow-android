package net.chronoflow.konsist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KeepRulesTest {
    private fun keepFiles(root: File): List<File> =
        File(root, "app/src")
            .listFiles()
            .orEmpty()
            .filter { it.name != "nonMinifiedRelease" }
            .flatMap { File(it, "keepRules").walkTopDown().filter { file -> file.isFile }.toList() }

    private fun directives(file: File) =
        file.readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }

    private fun logStripping(file: File) {
        val block =
            Regex("""-assumenosideeffects\s+class\s+android\.util\.Log\s*\{([^}]+)}""")
                .find(directives(file).joinToString("\n"))
                ?.groupValues
                ?.get(1)
                .orEmpty()
        check(
            listOf("v", "d", "i").all { Regex("""\b$it\(\.\.\.\);""").containsMatchIn(block) } &&
                !Regex("""\b[we]\s*\(""").containsMatchIn(block),
        ) { "logStrippingIsPresent: ${file.path}" }
    }

    private fun noEscapes(files: List<File>) {
        val escape =
            Regex("""-(dontoptimize|dontshrink|dontobfuscate|ignorewarnings)\b|-keepattributes\s+\*|-dontwarn\s+\*\*""")
        val bad = files.filter { escape.containsMatchIn(directives(it).joinToString("\n")) }
        check(bad.isEmpty()) { "noOptimisationEscapes: $bad" }
    }

    private fun noBroadKeeps(files: List<File>) {
        val broad = Regex("""-keep[a-z]*\s+class\s+net\.chronoflow\.\*\*""")
        val bad = files.filter { broad.containsMatchIn(directives(it).joinToString("\n")) }
        check(bad.isEmpty()) { "noBroadAppKeeps: $bad" }
    }

    private val planted get() = File(projectRoot(), "konsist/src/test/resources/violations/keepRules")

    @Test fun keepRuleFilesAreKnown() {
        val root = projectRoot()
        assertEquals(
            setOf("app/src/main/keepRules/app.keep", "app/src/main/keepRules/credentials.keep"),
            keepFiles(root).map { it.relativeTo(root).invariantSeparatorsPath }.toSet(),
        )
    }

    @Test fun logStrippingIsPresent() = logStripping(File(projectRoot(), "app/src/main/keepRules/app.keep"))

    @Test fun noOptimisationEscapes() = noEscapes(keepFiles(projectRoot()))

    @Test fun noBroadAppKeeps() = noBroadKeeps(keepFiles(projectRoot()))

    @Test fun logStrippingIsPresentRejectsPlantedViolation() {
        assertThrows(
            IllegalStateException::class.java,
        ) { logStripping(File(planted, "app/src/main/keepRules/log.keep")) }
    }

    @Test fun noOptimisationEscapesRejectsPlantedViolation() {
        assertThrows(IllegalStateException::class.java) { noEscapes(keepFiles(planted)) }
    }

    @Test fun noBroadAppKeepsRejectsPlantedViolation() {
        assertThrows(IllegalStateException::class.java) { noBroadKeeps(keepFiles(planted)) }
    }

    @Test fun nonMinifiedProfileKeepIsExact() {
        val dir = File(projectRoot(), "app/src/nonMinifiedRelease/keepRules")
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        assertEquals(listOf("profile.keep"), files.map { it.relativeTo(dir).invariantSeparatorsPath })
        assertEquals(listOf("-dontobfuscate", "-dontoptimize"), directives(files.single()))
        assertTrue(files.single().length() > 0)
    }
}
