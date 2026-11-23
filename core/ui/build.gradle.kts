plugins {
    id("chronoflow.android.library.compose")
}

android {
    namespace = "net.chronoflow.core.ui"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
    api(project(":core:designsystem"))
    implementation(project(":core:plan"))
    implementation(project(":core:sync"))
    implementation(libs.kotlinx.coroutines.core)
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.foundation)

    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(project(":core:testing-compose"))
    androidTestImplementation(project(":core:testing-compose")) {
        exclude(group = "org.robolectric")
        exclude(group = "io.github.takahirom.roborazzi")
    }
    androidTestImplementation(libs.androidx.test.runner)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
