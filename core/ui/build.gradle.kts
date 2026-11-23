plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.ui"
}

dependencies {
    implementation(libs.compose.runtime)

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
