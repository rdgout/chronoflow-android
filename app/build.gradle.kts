import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.Variant

plugins {
    id("chronoflow.android.application.compose")
}

fun addPerfSources(variant: Variant) {
    variant.sources.kotlin?.addStaticSourceDirectory("src/perf/kotlin")
    variant.buildConfigFields?.put("PERF_ENTRY", BuildConfigField("boolean", "true", null))
}

androidComponents {
    onVariants { variant ->
        val source =
            when (val type = variant.buildType) {
                "debug" -> {
                    providers.gradleProperty("cf.dataSource").orElse("mock").map { value ->
                        if (value !in setOf("mock", "api")) {
                            throw GradleException("cf.dataSource must be mock or api, was $value")
                        }
                        value
                    }
                }

                "release" -> {
                    providers.provider { "api" }
                }

                "benchmarkRelease", "nonMinifiedRelease" -> {
                    providers.provider { "mock" }
                }

                else -> {
                    throw GradleException("No DATA_SOURCE rule for build type $type")
                }
            }
        val baseUrl =
            if (variant.buildType == "debug") {
                providers.gradleProperty("cf.apiBaseUrl").orElse("https://api.chronoflow.net/v1").map { value ->
                    if ('"' in value || '\\' in value) {
                        throw GradleException("cf.apiBaseUrl must not contain quotes or backslashes")
                    }
                    value
                }
            } else {
                providers.provider { "https://api.chronoflow.net/v1" }
            }
        variant.buildConfigFields?.put("DATA_SOURCE", source.map { BuildConfigField("String", "\"$it\"", null) })
        variant.buildConfigFields?.put("API_BASE_URL", baseUrl.map { BuildConfigField("String", "\"$it\"", null) })
    }
    // The release source set is also compiled into the two perf variants, so the release stub lives in its own folder.
    onVariants(selector().withBuildType("release")) {
        it.sources.kotlin?.addStaticSourceDirectory("src/releaseNoPerf/kotlin")
    }
    onVariants(selector().withBuildType("debug")) { it.sources.kotlin?.addStaticSourceDirectory("src/perf/kotlin") }
    onVariants(selector().withBuildType("benchmarkRelease")) { addPerfSources(it) }
    onVariants(selector().withBuildType("nonMinifiedRelease")) { addPerfSources(it) }
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
}

android {
    namespace = "net.chronoflow.app"

    defaultConfig {
        applicationId = "net.chronoflow.app.native"
        buildConfigField("boolean", "PERF_ENTRY", "false")
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            // Single-process DataStore does not use the native shared counter (OQ-A131).
            excludes += "**/libdatastore_shared_counter.so"
        }
    }

    lint {
        checkDependencies = true
    }
}

dependencies {
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(project(":core:database-testing"))
    testImplementation(testFixtures(project(":feature:launch")))
    testImplementation(testFixtures(project(":core:sync")))
    implementation(project(":feature:launch"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:today"))
    implementation(project(":feature:trips"))
    implementation(project(":feature:trip"))
    implementation(project(":feature:wizard"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:reminders"))
    implementation(project(":core:model"))
    implementation(project(":core:plan"))
    implementation(project(":core:sync"))
    implementation(project(":core:reminders"))
    implementation(project(":core:wizard"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    debugImplementation(project(":core:data-mock"))
    debugImplementation(project(":core:testing"))
    debugImplementation(libs.androidx.metrics.performance)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing-junit"))
    testImplementation(project(":core:testing-compose"))

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.assertk)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.datastore.preferences)
    androidTestImplementation(libs.sqldelight.android.driver)
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing-compose")) {
        exclude(group = "org.robolectric")
        exclude(group = "io.github.takahirom.roborazzi")
    }
}

// The baseline profile plugin adds these build types after this script runs, so their configurations appear late.
configurations
    .matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }
    .configureEach { project.dependencies.add(name, project(":core:data-mock")) }
configurations
    .matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }
    .configureEach { project.dependencies.add(name, project(":core:testing")) }
configurations
    .matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }
    .configureEach { project.dependencies.addProvider(name, libs.androidx.metrics.performance) }
