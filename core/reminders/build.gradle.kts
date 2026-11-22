plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

tasks.test {
    systemProperty("chronoflow.repoRoot", rootDir.absolutePath)
    inputs
        .files(
            rootProject.file("tools/l10n/names.lock.json"),
            rootProject.file("core/ui/src/main/res/values/strings_ios.xml"),
            rootProject.file("core/ui/src/main/res/values-nl/strings_ios.xml"),
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:plan"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:testing"))
}
