plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.foundation)

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
