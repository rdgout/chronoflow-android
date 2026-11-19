plugins {
    id("chronoflow.jvm.library")
}

tasks.test {
    systemProperty("chronoflow.repoRoot", rootDir.absolutePath)
    inputs
        .file(rootProject.file("tools/l10n/plan_copy.sha256"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
