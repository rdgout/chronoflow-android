package net.chronoflow.buildlogic.signing

object PerfSigning {
    val buildTypes = setOf("nonMinifiedRelease", "benchmarkRelease")

    fun verify(
        buildType: String?,
        signingConfig: String?,
    ) {
        if (buildType in buildTypes) {
            check(signingConfig == "debug") { "$buildType must use debug signing" }
        }
    }
}
