package net.chronoflow.konsist

import org.junit.Test
import java.io.File

class StringResourceLocationTest : KonsistTest() {
    private val allowed =
        setOf(
            "core/ui/src/main/res/values/strings_ios.xml",
            "core/ui/src/main/res/values-nl/strings_ios.xml",
            "core/ui/src/main/res/values/strings_android.xml",
            "core/ui/src/main/res/values-nl/strings_android.xml",
            "core/ui/src/main/res/values/strings_untranslatable.xml",
            "core/ui/src/debug/res/values/strings_ios_debug.xml",
            "core/ui/src/debug/res/values-nl/strings_ios_debug.xml",
        )
    private val skipped = setOf("build", ".gradle", ".kotlin", "_design", ".git")
    private val valuesFile = Regex(".*/src/[^/]+/res/(values[^/]*)/[^/]+\\.xml")
    private val stringElement = Regex("<(string|plurals|string-array)[ >]")

    @Test fun stringsLiveOnlyInTheGeneratedCoreUiFiles() {
        val root = projectRoot()
        val offenders =
            root
                .walkTopDown()
                .onEnter { dir ->
                    val rel = dir.relativeTo(root).invariantSeparatorsPath
                    dir.name !in skipped && rel != "konsist/src/test/resources"
                }.filter { it.isFile && valuesFile.matches(it.relativeTo(root).invariantSeparatorsPath) }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .filter { stringElement.containsMatchIn(File(root, it).readText()) }
                .filterNot { it in allowed }
                .toList()
        check(offenders.isEmpty()) { "string resources outside the generated core/ui files: $offenders" }
        val badLocales =
            allowed.map { it.substringBefore("/res/") + "/res" }.toSet().flatMap { res ->
                File(root, res).listFiles().orEmpty().map { it.name }.filter {
                    it.startsWith("values-") && it != "values-nl" && it != "values-night"
                }
            }
        check(badLocales.isEmpty()) { "core/ui holds strings for locales other than nl: $badLocales" }
    }
}
