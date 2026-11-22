plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    api(project(":core:plan"))
    implementation(project(":core:wizard"))
    implementation(project(":core:reminders"))
    api(libs.kotlinx.coroutines.test)
}
