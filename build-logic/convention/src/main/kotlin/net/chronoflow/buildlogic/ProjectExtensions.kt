package net.chronoflow.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

internal fun Project.addDependency(
    configuration: String,
    notation: Any,
) {
    dependencies.add(configuration, notation)
}

internal fun Project.addDependency(
    configuration: String,
    notation: Provider<*>,
) {
    dependencies.add(configuration, notation)
}
