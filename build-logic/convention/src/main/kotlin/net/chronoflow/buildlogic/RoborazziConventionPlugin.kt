package net.chronoflow.buildlogic

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class RoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            listOf(
                "robolectric",
                "roborazzi",
                "roborazzi-compose",
                "roborazzi-junit-rule",
                "compose-ui-test-junit4",
                "androidx-test-ext-junit",
            ).forEach { addDependency("testImplementation", libs.library(it)) }
            addDependency("debugImplementation", libs.library("compose-ui-test-manifest"))
            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/snapshots"))
            }
        }
}
