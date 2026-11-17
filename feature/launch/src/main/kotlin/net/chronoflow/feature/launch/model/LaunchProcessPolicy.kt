package net.chronoflow.feature.launch.model

enum class LaunchKind { Cold, ProcessDeathRestore, Recreated, Warm }

object LaunchProcessPolicy {
    fun classify(
        hasSavedState: Boolean,
        savedProcessToken: String?,
        processToken: String,
        firstActivityOfProcess: Boolean,
    ): LaunchKind =
        when {
            hasSavedState && savedProcessToken == processToken -> LaunchKind.Recreated
            hasSavedState -> LaunchKind.ProcessDeathRestore
            firstActivityOfProcess -> LaunchKind.Cold
            else -> LaunchKind.Warm
        }

    fun claimsOverlay(kind: LaunchKind): Boolean = kind == LaunchKind.Cold || kind == LaunchKind.ProcessDeathRestore
}

enum class SplashExit { RemoveNow, Fade300 }

object SplashExitPolicy {
    fun forActivity(
        kind: LaunchKind,
        overlayClaimed: Boolean,
    ): SplashExit =
        if (!overlayClaimed &&
            kind == LaunchKind.Warm
        ) {
            SplashExit.Fade300
        } else {
            SplashExit.RemoveNow
        }
}
