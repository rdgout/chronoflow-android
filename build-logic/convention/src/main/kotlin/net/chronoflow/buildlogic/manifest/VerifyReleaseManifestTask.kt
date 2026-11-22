package net.chronoflow.buildlogic.manifest

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VerifyReleaseManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val allowedExportedComponents: SetProperty<String>

    @get:Input
    abstract val requiredPermissions: SetProperty<String>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val targetSdk: Property<Int>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations =
            ManifestRules.check(
                manifestXml = mergedManifest.get().asFile.readText(),
                applicationId = applicationId.get(),
                allowedExportedComponents = allowedExportedComponents.get(),
                requiredPermissions = requiredPermissions.get(),
                minSdk = minSdk.get(),
                targetSdk = targetSdk.get(),
                versionCode = versionCode.get(),
                versionName = versionName.get(),
            )
        val text = if (violations.isEmpty()) "OK" else violations.joinToString("\n")
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(text + "\n")
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Release manifest violations:\n$text")
        }
    }
}
