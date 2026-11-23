plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.testing.compose"
}

dependencies {
    compileOnly(project(":core:testing-junit"))
    api(project(":core:testing"))
    api(project(":core:ui"))
    api(project(":core:model"))
    api(libs.junit4)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.roborazzi.junit.rule)
    api(libs.compose.material3)
    api(libs.compose.ui.test.junit4)
    api(libs.androidx.test.ext.junit)
    testImplementation(libs.assertk)
}
