plugins {
    id("chronoflow.jvm.library")
}

dependencies {
    testImplementation(libs.konsist)
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
