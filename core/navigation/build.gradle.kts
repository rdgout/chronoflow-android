plugins {
    alias(libs.plugins.kotlin.serialization)
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.navigation"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit4)
}
