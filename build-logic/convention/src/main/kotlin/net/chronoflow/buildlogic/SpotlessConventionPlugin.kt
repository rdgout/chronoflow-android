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
                    target("**/*.kt")
                    targetExclude(
                        "**/build/**",
                        "**/generated/**",
                        "**/*.generated.kt",
                        "konsist/src/test/resources/**",
                        "_design/**",
                        "docs/**",
                    )
                    ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
                }
                kotlinGradle {
                    target("**/*.kts")
                    targetExclude("**/build/**", "konsist/src/test/resources/**")
                    ktlint(ktlintVersion)
                }
            }
        }
}
