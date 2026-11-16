package net.chronoflow.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import net.chronoflow.buildlogic.manifest.VerifyReleaseManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            configureKotlin()
            configureTestLauncher()
            val version =
                VersionProperties.parse(
                    providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText.get(),
                )
            extensions.configure<ApplicationExtension> {
                compileSdk = 37
                defaultConfig {
                    minSdk = 29
                    targetSdk = 36
                    versionCode = version.code
                    versionName = version.name
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    testInstrumentationRunnerArguments["timeout_msec"] = "60000"
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                buildFeatures.buildConfig = true
                signingConfigs.getByName("debug") {
                    storeFile = rootProject.file("app/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                    }
                    release {
                        optimization.enable = true
                    }
                }
                bundle.language.enableSplit = false
                testOptions {
                    animationsDisabled = true
                    unitTests.isIncludeAndroidResources = true
                }
                configureLint(lint)
            }
            val verifyTasks = mutableListOf<org.gradle.api.tasks.TaskProvider<VerifyReleaseManifestTask>>()
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants(selector().withBuildType("release")) { variant ->
                    verifyTasks +=
                        tasks.register("verifyReleaseManifest", VerifyReleaseManifestTask::class.java) {
                            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                            applicationId.set(variant.applicationId)
                            allowedExportedComponents.set(emptySet())
                            report.set(layout.buildDirectory.file("reports/verifyReleaseManifest.txt"))
                        }
                }
            }
            tasks.named("check").configure { dependsOn(provider { verifyTasks.toList() }) }
        }
}
