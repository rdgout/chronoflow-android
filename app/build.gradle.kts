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
    onVariants(selector().withBuildType("benchmarkRelease")) { addPerfSources(it) }
    onVariants(selector().withBuildType("nonMinifiedRelease")) { addPerfSources(it) }
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

    lint {
        checkDependencies = true
    }
}

dependencies {
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

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:testing"))

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.assertk)
    androidTestImplementation(project(":core:testing"))
}

// The baseline profile plugin adds these build types after this script runs, so their configurations appear late.
configurations
    .matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }
    .configureEach { project.dependencies.add(name, project(":core:data-mock")) }
