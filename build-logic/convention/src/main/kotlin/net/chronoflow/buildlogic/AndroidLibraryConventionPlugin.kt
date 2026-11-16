package net.chronoflow.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")
            configureKotlin()
            configureTestLauncher()
            extensions.configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig.minSdk = 29
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                testOptions {
                    targetSdk = 36
                    unitTests.isIncludeAndroidResources = true
                }
                lint.targetSdk = 36
                configureLint(lint)
            }
        }
}
