package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope

private const val ROOT = "net.chronoflow"

private val jvmCoreModules =
    setOf(
        "core/model",
        "core/plan",
        "core/sync",
        "core/reminders",
        "core/wizard",
        "core/network",
        "core/data-mock",
        "core/testing",
        "core/testing-junit",
    )

private fun fail(
    rule: String,
    violations: List<String>,
) {
    if (violations.isNotEmpty()) {
        throw AssertionError("$rule violated:\n" + violations.joinToString("\n") { "  $it" })
    }
}

private class ScopedFile(
    val path: String,
    val module: String,
    val sourceSet: String?,
    val imports: List<String>,
    val text: String,
)

private fun KoScope.moduleFiles(): List<ScopedFile> =
    files.mapNotNull { file ->
        val module = moduleOf(file.path) ?: return@mapNotNull null
        ScopedFile(file.path, module, sourceSetOf(file.path), file.imports.map { it.name }, file.text)
    }

private fun List<String>.importsFrom(prefix: String) = filter { it.startsWith("$prefix.") }

fun featuresDoNotImportOtherFeatures(scope: KoScope) {
    val violations =
        scope.moduleFiles().filter { it.module.startsWith("feature/") }.flatMap { file ->
            val own = file.module.removePrefix("feature/").replace("-", "")
            file.imports
                .importsFrom("$ROOT.feature")
                .filterNot { it.startsWith("$ROOT.feature.$own.") }
                .map { "${file.path}: $it" }
        }
    fail("featuresDoNotImportOtherFeatures", violations)
}

fun jvmCoreModulesImportNoAndroidOrCompose(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module in jvmCoreModules || it.module.startsWith("tools/") }
            .flatMap { file ->
                file.imports
                    .filter { it.startsWith("android.") || it.startsWith("androidx.compose.") }
                    .map { "${file.path}: $it" }
            }
    fail("jvmCoreModulesImportNoAndroidOrCompose", violations)
}

fun onlyAppImportsFeatures(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module != "app" && !it.module.startsWith("feature/") }
            .flatMap { file -> file.imports.importsFrom("$ROOT.feature").map { "${file.path}: $it" } }
    fail("onlyAppImportsFeatures", violations)
}

fun noGlobalScope(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { file ->
                file.imports.any { it == "kotlinx.coroutines.GlobalScope" } ||
                    file.text.contains("GlobalScope.")
            }.map { it.path }
    fail("noGlobalScope", violations)
}

private val runBlockingRegex = Regex("\\brunBlocking\\b")

fun noRunBlockingOutsideTests(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet != null && !isTestSourceSet(it.sourceSet) }
            .filter { file ->
                file.imports.contains("kotlinx.coroutines.runBlocking") ||
                    runBlockingRegex.containsMatchIn(file.text)
            }.map { it.path }
    fail("noRunBlockingOutsideTests", violations)
}

fun noSleepInTests(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet != null && isTestSourceSet(it.sourceSet) }
            .filter { it.text.contains("Thread.sleep(") || it.text.contains("SystemClock.sleep(") }
            .map { it.path }
    fail("noSleepInTests", violations)
}

fun viewModelsEndWithViewModel(scope: KoScope) {
    val violations =
        scope
            .classes()
            .filter { moduleOf(it.path) != null }
            .filter { it.hasParentWithName("ViewModel") || it.hasParentWithName("AndroidViewModel") }
            .filterNot { it.name.endsWith("ViewModel") }
            .map { "${it.path}: ${it.name}" }
    fail("viewModelsEndWithViewModel", violations)
}

fun routesAreSerializable(scope: KoScope) {
    class Candidate(
        val path: String,
        val name: String,
        val isRoute: Boolean,
        val serializable: Boolean,
    )

    val candidates =
        scope.classes().map {
            Candidate(
                it.path,
                it.name,
                it.hasParentWithName("NavKey", indirectParents = true),
                it.hasAnnotationWithName("Serializable"),
            )
        } +
            scope.interfaces().map {
                Candidate(
                    it.path,
                    it.name,
                    it.hasParentWithName("NavKey", indirectParents = true),
                    it.hasAnnotationWithName("Serializable"),
                )
            } +
            scope.objects().map {
                Candidate(
                    it.path,
                    it.name,
                    it.hasParentWithName("NavKey", indirectParents = true),
                    it.hasAnnotationWithName("Serializable"),
                )
            }
    val violations =
        candidates
            .filter { moduleOf(it.path) != null && it.isRoute && !it.serializable }
            .map { "${it.path}: ${it.name}" }
    fail("routesAreSerializable", violations)
}

private val textCallRegex = Regex("""\b(Basic)?Text\(""")
private val textLiteralArgRegex = Regex("""\A(text\s*=\s*)?("|\"\"\")""")
private val namedTextLiteralRegex = Regex("""\btext\s*=\s*("|\"\"\")""")

private fun hasLiteralTextArgument(source: String): Boolean =
    textCallRegex.findAll(source).any { call ->
        val args = source.substring(call.range.last + 1)
        if (textLiteralArgRegex.containsMatchIn(args.trimStart())) return@any true
        var depth = 1
        var index = 0
        while (index < args.length && depth > 0) {
            when (args[index]) {
                '(' -> {
                    depth++
                }

                ')' -> {
                    depth--
                }

                else -> {
                    if (depth == 1 && namedTextLiteralRegex.matchAt(args, index) != null &&
                        (index == 0 || !args[index - 1].isLetterOrDigit())
                    ) {
                        return@any true
                    }
                }
            }
            index++
        }
        false
    }

fun noLiteralTextInText(scope: KoScope) {
    val violations =
        scope
            .functions()
            .filter { function ->
                val sourceSet = sourceSetOf(function.path)
                sourceSet != null && !isTestSourceSet(sourceSet)
            }.filterNot { function ->
                function.annotations.any {
                    it.name.startsWith("Preview") ||
                        it.name.endsWith("Preview")
                }
            }.filter { hasLiteralTextArgument(it.text) }
            .map { "${it.path}: ${it.name}" }
    fail("noLiteralTextInText", violations)
}

private val insetsReadRegex =
    Regex(
        """\bWindowInsets\.(Companion\.)?(safeDrawing|safeContent|systemBars|statusBars|navigationBars|displayCutout)\b""",
    )
private val insetsImportRegex =
    Regex(
        """^androidx\.compose\.foundation\.layout\.(safeDrawing|safeContent|systemBars|statusBars|navigationBars|displayCutout)$""",
    )

private fun mayReadWindowInsets(path: String): Boolean {
    val normalized = path.replace('\\', '/')
    return normalized.endsWith("app/src/main/kotlin/net/chronoflow/app/MainActivity.kt") ||
        normalized.contains("/core/testing-compose/") ||
        normalized.contains("/src/androidTest")
}

fun insetsOnlyAtRoot(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filterNot { mayReadWindowInsets(it.path) }
            .filter { file ->
                insetsReadRegex.containsMatchIn(file.text) ||
                    file.imports.any { insetsImportRegex.matches(it) }
            }.map { it.path }
    fail("insetsOnlyAtRoot", violations)
}

fun testingNotInMain(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet == "main" && !it.module.startsWith("core/testing") }
            .filter { file ->
                file.imports.any {
                    it == "net.chronoflow.core.testing" ||
                        it.startsWith("net.chronoflow.core.testing.")
                }
            }.map { it.path }
    fail("testingNotInMain", violations)
}

private val timeHygieneModules = setOf("core/plan", "core/sync", "core/reminders", "core/wizard")
private val timeHygienePatterns =
    listOf(
        "Locale.getDefault",
        "TimeZone.getDefault",
        "ZoneId.systemDefault",
        "System.currentTimeMillis",
        "Instant.now(",
        "LocalDate.now(",
        "LocalDateTime.now(",
        "LocalTime.now(",
        "DecimalFormat(",
        "ZonedDateTime.now(",
        "OffsetDateTime.now(",
        "Clock.systemDefaultZone",
        "Clock.systemUTC",
        "SimpleDateFormat",
        "Calendar.getInstance",
        "DateTimeFormatter.ofLocalized",
        "String.format(",
    )
private val localeRequiredCalls =
    listOf(
        Regex("DateTimeFormatter\\.ofPattern\\("),
        Regex("NumberFormat\\.get\\w*Instance\\("),
        Regex("DecimalFormatSymbols\\("),
    )

private fun callsWithoutLocale(
    text: String,
    call: Regex,
): List<String> =
    call
        .findAll(text)
        .mapNotNull { match ->
            var depth = 1
            var i = match.range.last + 1
            while (i < text.length && depth > 0) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                i++
            }
            val args = text.substring(match.range.last + 1, i)
            if (args.contains("Locale")) null else match.value + ")"
        }.toList()

private val literalFormatRegex = Regex("\"\\s*\\.format\\(")

fun noDefaultLocaleZoneOrClockInLogic(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module in timeHygieneModules && it.sourceSet == "main" }
            .filterNot { it.path.endsWith(".generated.kt") || it.path.replace('\\', '/').contains("/generated/") }
            .flatMap { file ->
                val literal =
                    timeHygienePatterns.filter { file.text.contains(it) } +
                        localeRequiredCalls.flatMap { callsWithoutLocale(file.text, it) }
                val formatted =
                    if (literalFormatRegex.containsMatchIn(
                            file.text,
                        )
                    ) {
                        listOf("\"...\".format(")
                    } else {
                        emptyList()
                    }
                (literal + formatted).map { "${file.path}: $it" }
            }
    fail("noDefaultLocaleZoneOrClockInLogic", violations)
}
