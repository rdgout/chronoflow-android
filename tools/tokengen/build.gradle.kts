plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

val repoRoot = rootProject.layout.projectDirectory
val designInputs =
    repoRoot.asFileTree.matching {
        include("_design/canvas/*.dc.html", "_design/canvas/theme.css", "_design/canvas/ds/chronoflow/**")
    }
val iosInputs = layout.projectDirectory.dir("ios")

tasks.withType<Test>().configureEach {
    systemProperty("cf.repoRoot", repoRoot.asFile.absolutePath)
    inputs.files(designInputs)
    inputs.dir(iosInputs)
}
