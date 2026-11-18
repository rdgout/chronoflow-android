plugins {
    id("chronoflow.sqldelight")
}

android {
    namespace = "net.chronoflow.core.database"
}

sqldelight {
    databases {
        create("AccountDb") { packageName.set("net.chronoflow.core.database") }
    }
}

dependencies {
    testImplementation(project(":core:database-testing"))
    api(project(":core:model"))
    implementation(libs.sqldelight.android.driver)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.sqldelight.sqlite.driver)
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/sqldelight"))
}
