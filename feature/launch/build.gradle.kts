plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.launch"
    testFixtures { enable = true }
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(libs.androidx.core.splashscreen)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(platform(libs.compose.bom))
    testFixturesImplementation(libs.compose.runtime)
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
