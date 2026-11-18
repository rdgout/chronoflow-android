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

fun launchModelImportsNoAndroid(scope: KoScope) {
    val violations =
        scope.files
            .filter {
                it.packagee?.name in
                    setOf("net.chronoflow.feature.launch.model", "net.chronoflow.core.model.launch")
            }.flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
                    .map { "${file.path}: ${it.name}" }
            }
    fail("launchModelImportsNoAndroid", violations)
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

private val multiProcessDataStoreRegex =
    Regex("\\b(?:MultiProcessDataStoreFactory|createMultiProcessCoordinator|MultiProcessCoordinator)\\b")

fun noMultiProcessDataStore(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { multiProcessDataStoreRegex.containsMatchIn(it.text) }
            .map { it.path }
    fail("noMultiProcessDataStore", violations)
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
    // Skip the hierarchy walk when the scope has no NavKey root or reference.
    if (scope.files.none { "NavKey" in it.text }) return

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
        normalized.endsWith("app/src/main/kotlin/net/chronoflow/app/CfRoot.kt") ||
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

private val mockFixtureImports =
    mapOf(
        "MockAppSeed.kt" to setOf("Fixtures", "Template", "LargeTimelineFixtures"),
        "MockTripFixtures.kt" to setOf("Template"),
        "MockTripGeneration.kt" to setOf("MockTripTemplates", "Template"),
    )

fun testingNotInMain(scope: KoScope) {
    val prefix = "net.chronoflow.core.testing"
    val violations =
        scope
            .moduleFiles()
            .filter { it.sourceSet == "main" && !it.module.startsWith("core/testing") }
            .flatMap { file ->
                file.imports
                    .filter { it == prefix || it.startsWith("$prefix.") }
                    .filterNot { imported ->
                        file.module == "core/data-mock" &&
                            imported.removePrefix("$prefix.") in
                            mockFixtureImports[file.path.substringAfterLast('/')].orEmpty()
                    }.map { "${file.path}: $it" }
            }
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

private class LocaleCall(
    val regex: Regex,
    val needsTopLevelComma: Boolean,
)

private val localeRequiredCalls =
    listOf(
        LocaleCall(Regex("DateTimeFormatter\\.ofPattern\\("), needsTopLevelComma = true),
        LocaleCall(Regex("NumberFormat\\.get\\w*Instance\\("), needsTopLevelComma = false),
        LocaleCall(Regex("DecimalFormatSymbols\\("), needsTopLevelComma = false),
        LocaleCall(Regex("DecimalFormatSymbols\\.getInstance\\("), needsTopLevelComma = false),
    )

private fun callsWithoutLocale(
    text: String,
    call: LocaleCall,
): List<String> =
    call.regex
        .findAll(text)
        .mapNotNull { match ->
            var depth = 1
            var topLevelComma = false
            var i = match.range.last + 1
            while (i < text.length && depth > 0) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> depth--
                    ',' -> if (depth == 1) topLevelComma = true
                }
                i++
            }
            val args = text.substring(match.range.last + 1, i - 1)
            val allowed =
                if (call.needsTopLevelComma) {
                    topLevelComma || text.startsWith(".withLocale(", i)
                } else {
                    args.isNotBlank()
                }
            if (allowed) null else match.value + ")"
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

fun okhttpOnlyInCoreNetwork(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module != "core/network" && it.sourceSet != null && !isTestSourceSet(it.sourceSet) }
            .filter { file -> file.imports.any { it == "okhttp3" || it.startsWith("okhttp3.") } }
            .map { it.path }
    fail("okhttpOnlyInCoreNetwork", violations)
}

fun noHttpLoggingInterceptor(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { file -> file.imports.any { it == "okhttp3.logging" || it.startsWith("okhttp3.logging.") } }
            .map { it.path }
    fail("noHttpLoggingInterceptor", violations)
}

fun onlyDebugAndPerfImportTheMock(scope: KoScope) {
    val prefix = "net.chronoflow.core.datamock"
    val violations =
        scope
            .moduleFiles()
            .filterNot { file ->
                file.module == "core/data-mock" || file.sourceSet?.let(::isTestSourceSet) == true ||
                    (file.module == "app" && file.sourceSet in setOf("debug", "perf"))
            }.flatMap { file ->
                file.imports.filter { it == prefix || it.startsWith("$prefix.") }.map { "${file.path}: $it" }
            }
    fail("onlyDebugAndPerfImportTheMock", violations)
}

fun databaseTestingIsTestOnly(scope: KoScope) {
    val violations =
        scope
            .moduleFiles()
            .filter { it.module != "core/database-testing" && it.sourceSet != null && !isTestSourceSet(it.sourceSet) }
            .flatMap { file ->
                file.imports.importsFrom("net.chronoflow.core.database.testing").map { "${file.path}: $it" }
            }
    fail("databaseTestingIsTestOnly", violations)
}
