plugins {
    `kotlin-dsl`
}

group = "net.chronoflow.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "3g"
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.sqldelight.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
}

gradlePlugin {
    plugins {
        fun plugin(
            id: String,
            className: String,
        ) {
            register(id) {
                this.id = id
                implementationClass = "net.chronoflow.buildlogic.$className"
            }
        }
        plugin("chronoflow.android.application", "AndroidApplicationConventionPlugin")
        plugin("chronoflow.android.application.compose", "AndroidApplicationComposeConventionPlugin")
        plugin("chronoflow.android.library", "AndroidLibraryConventionPlugin")
        plugin("chronoflow.android.library.compose", "AndroidLibraryComposeConventionPlugin")
        plugin("chronoflow.android.feature", "AndroidFeatureConventionPlugin")
        plugin("chronoflow.jvm.library", "JvmLibraryConventionPlugin")
        plugin("chronoflow.sqldelight", "SqlDelightConventionPlugin")
        plugin("chronoflow.android.test", "AndroidTestConventionPlugin")
        plugin("chronoflow.roborazzi", "RoborazziConventionPlugin")
        plugin("chronoflow.spotless", "SpotlessConventionPlugin")
    }
}
