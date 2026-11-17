package net.chronoflow.konsist

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration

private val robolectricTiers = setOf("Logic", "Layout", "Snapshot", "Capture")
private val allTiers = robolectricTiers + setOf("Smoke", "Perf")

private fun fail(
    rule: String,
    violations: List<String>,
) {
    if (violations.isNotEmpty()) {
        throw AssertionError("$rule violated:\n" + violations.joinToString("\n") { "  $it" })
    }
}

private fun KoClassDeclaration.annotationNames(): Set<String> = annotations.map { it.name }.toSet()

private fun KoClassDeclaration.isRobolectric(): Boolean {
    val runWith = annotations.firstOrNull { it.name == "RunWith" }?.text.orEmpty()
    return "Robolectric" in runWith ||
        "AndroidJUnit4" in runWith ||
        annotations.any { it.name == "Config" || it.name == "GraphicsMode" }
}

private fun KoClassDeclaration.hasTests(): Boolean = functions().any { fn -> fn.annotations.any { it.name == "Test" } }

private fun unitTestClasses(scope: KoScope): List<KoClassDeclaration> =
    scope.classes().filter { cls ->
        moduleOf(cls.path) != null &&
            sourceSetOf(cls.path)?.startsWith("test") == true &&
            !cls.hasModifier(KoModifier.ABSTRACT)
    }

private val inheritableTestAnnotations = allTiers + setOf("TimingGate", "RunWith", "Config", "GraphicsMode")

fun abstractTestClassesCarryNoTier(scope: KoScope) {
    val violations =
        scope
            .classes()
            .filter { cls ->
                moduleOf(cls.path) != null &&
                    sourceSetOf(cls.path)?.let(::isTestSourceSet) == true &&
                    cls.hasModifier(KoModifier.ABSTRACT)
            }.filter { it.annotationNames().intersect(inheritableTestAnnotations).isNotEmpty() }
            .map {
                "${it.path}: abstract ${it.name} carries ${it.annotationNames().intersect(
                    inheritableTestAnnotations,
                )}; " +
                    "annotate each concrete class instead"
            }
    fail("abstractTestClassesCarryNoTier", violations)
}

fun robolectricClassesHaveOneTier(scope: KoScope) {
    val violations =
        unitTestClasses(scope).filter { it.isRobolectric() }.mapNotNull { cls ->
            val names = cls.annotationNames()
            val tiers = names.intersect(robolectricTiers)
            when {
                tiers.size != 1 -> "${cls.path}: ${cls.name} has tiers $tiers, needs exactly one of $robolectricTiers"
                "TimingGate" in names -> "${cls.path}: ${cls.name} is Robolectric and also TimingGate"
                else -> null
            }
        }
    fail("robolectricClassesHaveOneTier", violations)
}

fun emulatorClassesHaveSmokeOrCapture(scope: KoScope) {
    val violations =
        scope
            .classes()
            .filter { cls ->
                moduleOf(cls.path) != null &&
                    sourceSetOf(cls.path)?.startsWith("androidTest") == true &&
                    cls.hasTests()
            }.filter { cls -> cls.annotationNames().intersect(setOf("Smoke", "Capture")).size != 1 }
            .map { "${it.path}: ${it.name} needs exactly one of @Smoke, @Capture" }
    fail("emulatorClassesHaveSmokeOrCapture", violations)
}

fun benchmarkClassesArePerf(scope: KoScope) {
    val violations =
        scope
            .classes()
            .filter { it.path.replace('\\', '/').contains("/benchmark/src/main/") && it.hasTests() }
            .filter { cls -> cls.annotationNames().intersect(allTiers + "TimingGate") != setOf("Perf") }
            .map { "${it.path}: ${it.name} needs @Perf and no other tier" }
    fail("benchmarkClassesArePerf", violations)
}

fun plainJvmClassesHaveNoTier(scope: KoScope) {
    val violations =
        unitTestClasses(scope)
            .filter { !it.isRobolectric() && it.hasTests() }
            .filter { it.annotationNames().intersect(allTiers).isNotEmpty() }
            .map {
                "${it.path}: ${it.name} is a plain JVM test and carries ${it.annotationNames().intersect(
                    allTiers,
                )}"
            }
    fail("plainJvmClassesHaveNoTier", violations)
}

private fun isTestFile(path: String): Boolean {
    val sourceSet = sourceSetOf(path)
    return sourceSet != null && isTestSourceSet(sourceSet)
}

private val runTestRegex = Regex("""\brunTest\b\s*[({]""")

fun runTestHasTimeout(scope: KoScope) {
    val violations =
        scope.files.filter { isTestFile(it.path) }.flatMap { file ->
            runTestRegex.findAll(file.text).mapNotNull { match ->
                val args =
                    if (file.text[match.range.last] == '(') {
                        var depth = 1
                        var i = match.range.last + 1
                        while (i < file.text.length && depth > 0) {
                            if (file.text[i] == '(') depth++
                            if (file.text[i] == ')') depth--
                            i++
                        }
                        file.text.substring(match.range.last + 1, i)
                    } else {
                        ""
                    }
                if ("timeout = 60.seconds" in args) null else "${file.path}: runTest without timeout = 60.seconds"
            }
        }
    fail("runTestHasTimeout", violations)
}

fun noTimeoutRuleOutsideEmulatorTests(scope: KoScope) {
    val violations =
        scope.files
            .filter { file -> sourceSetOf(file.path)?.startsWith("test") == true }
            .filter { file -> file.imports.any { it.name == "org.junit.rules.Timeout" } }
            .map { "${it.path}: imports org.junit.rules.Timeout" }
    fail("noTimeoutRuleOutsideEmulatorTests", violations)
}
