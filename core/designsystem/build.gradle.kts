plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.designsystem"
}

dependencies {
    debugImplementation(libs.compose.ui.test.manifest)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.graphics)

    testImplementation(libs.junit4)
}
