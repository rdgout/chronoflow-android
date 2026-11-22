plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.settings"
}

dependencies {
    implementation(libs.zxing.core)
    implementation(project(":core:reminders"))
    implementation(project(":core:plan"))
    implementation(project(":core:sync"))
    implementation(project(":core:data"))
    implementation(project(":core:wizard"))
    implementation(libs.androidx.activity.compose)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-junit"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(project(":core:data-mock"))
    testImplementation(project(":core:database-testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
