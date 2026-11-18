import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:testing"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:network"))
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.kotlinx.coroutines.test)
}
