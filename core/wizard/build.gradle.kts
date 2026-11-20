plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.test {
    systemProperty("chronoflow.repoRoot", rootDir.absolutePath)
    inputs
        .files(
            rootProject.file("tools/l10n/names.lock.json"),
            rootProject.file("tools/l10n/android-only.json"),
            rootProject.file("core/plan/src/main/resources/plan_copy.json"),
        ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/kotlin").withPathSensitivity(PathSensitivity.RELATIVE)
}
