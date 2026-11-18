plugins {
    id("chronoflow.android.library")
}

android {
    namespace = "net.chronoflow.core.database.testing"
}

dependencies {
    api(project(":core:database"))
    implementation(libs.sqldelight.sqlite.driver)
}
