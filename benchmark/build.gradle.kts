plugins {
    id("chronoflow.android.test")
}

android {
    namespace = "net.chronoflow.benchmark"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(project(":core:testing"))
}
