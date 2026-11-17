package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.launch.LaunchEnding

object LaunchHandOffTimeline {
    fun build(
        opening: LaunchTimeline,
        state: LaunchState,
    ): LaunchTimeline {
        var timeline = opening
        state.statusStart?.let { timeline = timeline.withStatus(it) }
        val exit = state.exitStart
        if (state.didCommitExit && exit != null) {
            timeline =
                when (state.ending) {
                    LaunchEnding.App -> {
                        timeline.withReadyExit(exit)
                    }

                    LaunchEnding.Welcome -> {
                        timeline.withWelcomeExit(
                            exit,
                            opening.size,
                            state.moonTarget ?: state.fallbackTarget,
                        )
                    }

                    null -> {
                        timeline
                    }
                }
        }
        return timeline
    }

    fun exitEndSeconds(t: LaunchTimeline): Double? =
        t.tracks
            .firstOrNull { it.layer == LayerID.Container && it.keyPath == KeyPath.OPACITY }
            ?.takeIf { it.scalarAt(Double.POSITIVE_INFINITY) == 0.0 }
            ?.keyTimes
            ?.last()
}
