import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("chronoflow.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(project(":core:testing"))
}
