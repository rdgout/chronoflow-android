plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.onboarding"
}

dependencies {
    implementation(project(":core:sync"))
    implementation(project(":core:data"))
    implementation(libs.androidx.activity.compose)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(project(":core:data-mock"))
    testImplementation(project(":core:database-testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
