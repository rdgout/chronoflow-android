plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(project(":core:testing"))
}

tasks.test {
    inputs
        .files(
            fileTree(rootDir) {
                include("**/*.kt", "**/*.kts", "**/src/*/res/values*/*.xml")
                exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "_design/**", "docs/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}
