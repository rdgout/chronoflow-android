package net.chronoflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("chronoflow.android.library")
            configureCompose()
        }
}
