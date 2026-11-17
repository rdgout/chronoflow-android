package net.chronoflow.feature.launch

import net.chronoflow.core.designsystem.generated.WelcomeTokens
import net.chronoflow.core.model.launch.LaunchEnding
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.LaunchHandOffTimeline
import net.chronoflow.feature.launch.model.LaunchState
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.MoonTarget
import net.chronoflow.feature.launch.model.withWelcomeExit
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
    fun timeline(
        opening: LaunchTimeline,
        safeAreaInsets: MoonTarget.Insets = MoonTarget.Insets(0.0, 0.0, 0.0, 0.0),
        rowHeight: Double = WelcomeTokens.moonSize.toDouble(),
    ): LaunchTimeline =
        when (mode) {
            null -> {
                opening
            }

            LaunchMode.Welcome -> {
                opening.withWelcomeExit(
                    1600.milliseconds,
                    opening.size,
                    MoonTarget.fallback(opening.size, safeAreaInsets, rowHeight),
                )
            }

            LaunchMode.Ready, LaunchMode.Slow -> {
                LaunchHandOffTimeline.build(
                    opening,
                    LaunchState(
                        ending = LaunchEnding.App,
                        statusStart = if (mode == LaunchMode.Slow) 2.seconds else null,
                        exitStart = if (mode == LaunchMode.Ready) 1500.milliseconds else readyAt,
                        didCommitExit = true,
                    ),
                )
            }
        }

    companion object {
        val None = LaunchOverrides()
    }
}

enum class LaunchMode { Ready, Slow, Welcome }
