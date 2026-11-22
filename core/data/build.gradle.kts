plugins {
    id("chronoflow.android.library")
}

android {
    namespace = "net.chronoflow.core.data"
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(project(":core:datastore"))
    testImplementation(testFixtures(project(":core:sync")))
    testImplementation(project(":core:database-testing"))
    api(project(":core:plan"))
    api(project(":core:reminders"))
    api(project(":core:sync"))
    api(project(":core:database"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.assertk)
    testImplementation(libs.turbine)
    testImplementation(libs.sqldelight.sqlite.driver)
    testImplementation(project(":core:network"))
    testImplementation(project(":core:data-mock"))
    testImplementation(libs.okhttp.mockwebserver3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:ui"))
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(project(":core:testing-junit"))
    implementation(project(":core:wizard"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
