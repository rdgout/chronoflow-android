plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.designsystem"
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.graphics)

    testImplementation(libs.junit4)
}
