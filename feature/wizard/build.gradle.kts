plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.wizard"
}

dependencies {
    implementation(project(":core:wizard"))
    implementation(libs.androidx.navigation3.ui)
    testImplementation(project(":core:testing-compose"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
