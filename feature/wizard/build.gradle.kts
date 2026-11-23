plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.wizard"
}

dependencies {
    implementation(project(":core:wizard"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
