plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.wizard"
}

dependencies {
    implementation(project(":core:wizard"))
    implementation(project(":core:plan"))
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.activity.compose)
    testImplementation(project(":core:testing-compose"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:data-mock"))
    testImplementation(libs.kotlinx.coroutines.test)
}
