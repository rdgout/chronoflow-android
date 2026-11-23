plugins {
    `java-test-fixtures`
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    testImplementation(project(":core:testing"))
    testImplementation(libs.turbine)
    testFixturesApi(project(":core:model"))
    testFixturesImplementation(project(":core:testing"))
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
