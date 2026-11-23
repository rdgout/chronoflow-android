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
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "_design/**", "docs/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}
