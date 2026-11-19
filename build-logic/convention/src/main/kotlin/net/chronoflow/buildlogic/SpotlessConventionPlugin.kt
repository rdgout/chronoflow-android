package net.chronoflow.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
            val composeRules = libs.library("compose-rules-ktlint").get().let { "${it.group}:${it.name}:${it.version}" }
            extensions.configure<SpotlessExtension> {
                kotlin {
                    // A pruned file tree: scanning build directories races with compile tasks writing into them.
                    target(
                        fileTree(projectDir) {
                            include("**/*.kt")
                            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "_design/**", "docs/**")
                        },
                    )
                    targetExclude(
                        "**/generated/**",
                        "**/*.generated.kt",
                        "konsist/src/test/resources/**",
                    )
                    ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
                }
                kotlinGradle {
                    target(
                        fileTree(projectDir) {
                            include("**/*.kts")
                            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "_design/**", "docs/**")
                        },
                    )
                    targetExclude("konsist/src/test/resources/**")
                    ktlint(ktlintVersion)
                }
            }
        }
}
