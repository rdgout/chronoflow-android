plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.platform.launcher)
}
