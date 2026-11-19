package net.chronoflow.app

import net.chronoflow.core.model.launch.Gate
import net.chronoflow.feature.launch.LaunchMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class LaunchDiagnostics(
    val mode: LaunchMode? = null,
    val readyAt: Duration = 4600.milliseconds,
    val mainThreadStall: Duration? = null,
    val reduceMotion: Boolean? = null,
    val stub: StubConfig? = null,
) {
    companion object {
        val None = LaunchDiagnostics()
    }
}

data class StubConfig(
    val session: StubSession = StubSession.Native,
    val gate: Gate = Gate.App,
    val stored: StubStored = StubStored.Yes,
    val readyDelay: Duration = Duration.ZERO,
)

enum class StubSession { Native, SignedIn, SignedOut, Never }

enum class StubStored { Yes, No, Never }
