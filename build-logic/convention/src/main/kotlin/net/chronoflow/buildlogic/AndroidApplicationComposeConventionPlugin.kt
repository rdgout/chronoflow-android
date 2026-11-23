package net.chronoflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("chronoflow.android.application")
            configureCompose()
        }
}
