plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:plan"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:testing"))
}
