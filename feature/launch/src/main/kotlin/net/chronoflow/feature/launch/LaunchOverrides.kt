package net.chronoflow.feature.launch

import kotlin.time.Duration

data class LaunchOverrides(
    val reduceMotion: Boolean? = null,
    val freezeAt: Duration? = null,
    val holdSplash: Boolean = false,
    val skipOverlay: Boolean = false,
) {
    companion object {
        val None = LaunchOverrides()
    }
}
