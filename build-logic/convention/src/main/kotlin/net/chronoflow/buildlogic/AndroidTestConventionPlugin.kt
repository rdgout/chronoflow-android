package net.chronoflow.buildlogic

import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")
        configureKotlin()
        extensions.configure<TestExtension> {
            compileSdk = 37
            defaultConfig {
                minSdk = 29
                targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testInstrumentationRunnerArguments["timeout_msec"] = "60000"
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            targetProjectPath = ":app"
            configureLint(lint)
        }
    }
}
