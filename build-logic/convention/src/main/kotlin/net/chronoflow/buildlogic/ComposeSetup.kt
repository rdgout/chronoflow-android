package net.chronoflow.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureCompose() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.getByType<CommonExtension>().buildFeatures.compose = true
    val bom = libs.library("compose-bom")
    addDependency("implementation", dependencies.platform(bom))
    addDependency("androidTestImplementation", dependencies.platform(bom))
    val reports = providers.gradleProperty("composeReports").map { it == "true" }.orElse(false)
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
        if (reports.get()) {
            reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
            metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        }
    }
}
