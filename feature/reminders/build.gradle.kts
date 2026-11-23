plugins {
    id("chronoflow.android.feature")
}

android {
    namespace = "net.chronoflow.feature.reminders"
}

dependencies {
    implementation(project(":core:reminders"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-junit"))
    testImplementation(project(":core:testing-compose"))
    testImplementation(project(":core:database-testing"))
    testImplementation(project(":core:data"))
    testImplementation(libs.kotlinx.coroutines.test)
}
