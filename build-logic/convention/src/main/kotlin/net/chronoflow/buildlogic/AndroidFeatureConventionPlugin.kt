package net.chronoflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("chronoflow.android.library.compose")
            listOf(":core:designsystem", ":core:ui", ":core:navigation", ":core:model").forEach {
                addDependency("implementation", project(it))
            }
            listOf(
                "androidx-lifecycle-runtime-compose",
                "androidx-lifecycle-viewmodel-compose",
                "androidx-navigation3-runtime",
            ).forEach { addDependency("implementation", libs.library(it)) }
        }
}
