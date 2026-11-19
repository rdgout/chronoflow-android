package net.chronoflow.konsist

private val moduleRegex = Regex("/(app|feature/[a-z0-9-]+|core/[a-z0-9-]+|tools/[a-z0-9-]+)/src/")

fun moduleOf(path: String): String? =
    moduleRegex
        .findAll(path.replace('\\', '/'))
        .lastOrNull()
        ?.groupValues
        ?.get(1)

fun sourceSetOf(path: String): String? {
    val normalized = path.replace('\\', '/')
    val match = moduleRegex.findAll(normalized).lastOrNull() ?: return null
    return normalized.substring(match.range.last + 1).substringBefore('/').ifEmpty { null }
}

fun isTestSourceSet(name: String): Boolean = name.startsWith("test") || name.startsWith("androidTest")

fun projectRoot(): java.io.File {
    var dir: java.io.File? = java.io.File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        if (java.io.File(dir, "settings.gradle.kts").exists() && java.io.File(dir, "gradlew").exists()) return dir
        dir = dir.parentFile
    }
    error("project root not found")
}

fun projectScope(): com.lemonappdev.konsist.api.container.KoScope {
    val root = projectRoot()
    val files =
        com.lemonappdev.konsist.api.Konsist
            .scopeFromProject()
            .files
            .map {
                java.io
                    .File(it.path)
                    .relativeTo(root)
                    .invariantSeparatorsPath
            }.filterNot { "/$it".contains("/konsist/") }
    return com.lemonappdev.konsist.api.Konsist
        .scopeFromFiles(files)
}

// Resolve the shared graph during class setup so rule timings do not depend on execution order.
fun resolvedProjectScope(): com.lemonappdev.konsist.api.container.KoScope =
    projectScope().apply {
        classesAndInterfacesAndObjects().forEach { declaration ->
            declaration.parents(indirectParents = false).forEach { it.sourceDeclaration }
        }
    }
