package net.chronoflow.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import net.chronoflow.buildlogic.manifest.ReleaseManifestAllowlist
import net.chronoflow.buildlogic.manifest.VerifyReleaseManifestTask
import net.chronoflow.buildlogic.signing.UploadSigningResolver
import net.chronoflow.buildlogic.size.MeasureBundleSizeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("androidx.baselineprofile")
            configureKotlin()
            configureTestLauncher()
            val version =
                VersionProperties.parse(
                    providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText.get(),
                )
            val uploadSigning =
                UploadSigningResolver.resolve(
                    values =
                        UploadSigningResolver.KEYS.associateWith { key ->
                            providers.gradleProperty(key).orElse(providers.environmentVariable(key)).orNull
                        },
                    repoRoot = rootProject.projectDir,
                    home = File(providers.systemProperty("user.home").get()),
                    exists = File::isFile,
                )
            if (uploadSigning == null) logger.lifecycle("release is unsigned: CF_UPLOAD_* not set")
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
                val uploadConfig =
                    uploadSigning?.let { upload ->
                        signingConfigs.create("upload") {
                            storeFile = upload.storeFile
                            storePassword = upload.storePassword
                            keyAlias = upload.keyAlias
                            keyPassword = upload.keyPassword
                            storeType = "PKCS12"
                        }
                    }
                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                    }
                    release {
                        optimization.enable = true
                        signingConfig = uploadConfig
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
            val bundletool = configurations.create("bundletool") { isCanBeConsumed = false }
            addDependency("bundletool", libs.library("bundletool"))
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants(selector().withBuildType("release")) { variant ->
                    verifyTasks +=
                        tasks.register("verifyReleaseManifest", VerifyReleaseManifestTask::class.java) {
                            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                            applicationId.set(variant.applicationId)
                            allowedExportedComponents.set(emptySet())
                            requiredPermissions.set(ReleaseManifestAllowlist.PERMISSIONS_REQUIRED)
                            minSdk.set(29)
                            targetSdk.set(36)
                            versionCode.set(version.code)
                            versionName.set(version.name)
                            report.set(layout.buildDirectory.file("reports/verifyReleaseManifest.txt"))
                        }
                    tasks.register("measureBundleSize", MeasureBundleSizeTask::class.java) {
                        bundle.set(variant.artifacts.get(SingleArtifact.BUNDLE))
                        bundletoolClasspath.from(bundletool)
                        aapt2.set(sdkComponents.aapt2.flatMap { it.executable })
                        outputDir.set(layout.buildDirectory.dir("outputs/size"))
                    }
                }
            }
            tasks.named("check").configure { dependsOn(provider { verifyTasks.toList() }) }
        }
}
