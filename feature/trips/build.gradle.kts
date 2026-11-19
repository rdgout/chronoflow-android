plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.trips"
}

dependencies {
    implementation(project(":core:plan"))
    implementation(project(":core:sync"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(libs.kotlinx.coroutines.test)
}
