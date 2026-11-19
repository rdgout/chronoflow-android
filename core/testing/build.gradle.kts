plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    api(project(":core:plan"))
}
