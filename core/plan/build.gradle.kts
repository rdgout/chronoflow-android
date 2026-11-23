plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

tasks.test {
    systemProperty("chronoflow.repoRoot", rootDir.absolutePath)
    inputs
        .file(rootProject.file("tools/l10n/plan_copy.sha256"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
