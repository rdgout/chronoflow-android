package net.chronoflow.app.diagnostics

internal object LaunchPathViolationPolicy {
    fun shouldCrash(
        launchPathActive: Boolean,
        isDiskOrNetwork: Boolean,
        stackClassNames: List<String>,
    ): Boolean = launchPathActive && isDiskOrNetwork && stackClassNames.any { it.startsWith("net.chronoflow.") }
}
