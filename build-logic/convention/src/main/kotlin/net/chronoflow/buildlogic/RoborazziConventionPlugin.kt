package net.chronoflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class RoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            listOf("robolectric", "roborazzi", "roborazzi-compose", "roborazzi-junit-rule", "compose-ui-test-junit4")
                .forEach { addDependency("testImplementation", libs.library(it)) }
        }
}
