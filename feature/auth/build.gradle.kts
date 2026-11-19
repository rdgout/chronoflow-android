plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.auth"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    testImplementation(project(":core:data-mock"))
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(libs.kotlinx.coroutines.test)
}
