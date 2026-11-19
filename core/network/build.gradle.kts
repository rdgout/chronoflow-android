plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.kotlinx.coroutines.test)
}
