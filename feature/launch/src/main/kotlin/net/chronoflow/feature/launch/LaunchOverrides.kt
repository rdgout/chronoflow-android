package net.chronoflow.feature.launch

import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.withReadyExit
import net.chronoflow.feature.launch.model.withStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class LaunchOverrides(
    val reduceMotion: Boolean? = null,
    val freezeAt: Duration? = null,
    val holdSplash: Boolean = false,
    val skipOverlay: Boolean = false,
    val mode: LaunchMode? = null,
    val readyAt: Duration = 4600.milliseconds,
    val mainThreadStall: Duration? = null,
    val readiness: ((LaunchClock) -> LaunchReadiness)? = null,
) {
    fun timeline(opening: LaunchTimeline): LaunchTimeline =
        when (mode) {
            LaunchMode.Ready -> opening.withReadyExit(1500.milliseconds)
            LaunchMode.Slow -> opening.withStatus(2.seconds).withReadyExit(readyAt)
            null -> opening
        }

    companion object {
        val None = LaunchOverrides()
    }
}

enum class LaunchMode { Ready, Slow }
