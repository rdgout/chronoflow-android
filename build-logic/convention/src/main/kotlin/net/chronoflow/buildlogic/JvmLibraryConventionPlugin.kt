package net.chronoflow.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("com.android.lint")
            configureKotlin()
            configureTestLauncher()
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            extensions.configure<Lint> { configureLint(this) }
            val testSources = extensions.getByType<SourceSetContainer>().getByName("test")
            tasks.register<Test>("timingTest") {
                group = "verification"
                description = "Runs the @TimingGate wall-clock budget tests serially."
                testClassesDirs = testSources.output.classesDirs
                classpath = testSources.runtimeClasspath
                maxParallelForks = 1
                mustRunAfter(tasks.withType<Test>().matching { it.name != "timingTest" })
            }
            addDependency("testImplementation", libs.library("junit4"))
            addDependency("testImplementation", libs.library("assertk"))
        }
}
