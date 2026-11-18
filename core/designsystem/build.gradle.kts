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
    api(libs.compose.material3.adaptive.navigation.suite)
    constraints {
        // Standalone chrome tests and Lint must resolve the same Lifecycle pin as the app.
        implementation(libs.androidx.lifecycle.runtime.compose)
    }
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
