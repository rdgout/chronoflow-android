plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.trip"
}

dependencies {
    implementation(project(":core:plan"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
