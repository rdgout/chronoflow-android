package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchEnding
import net.chronoflow.core.model.session.SessionPresence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class LaunchRoute { Welcome, Today, Verify, Terms, Walkthrough }

fun Gate.launchRoute(): LaunchRoute =
    when (this) {
        Gate.Verify -> LaunchRoute.Verify
        Gate.Terms -> LaunchRoute.Terms
        Gate.Walkthrough -> LaunchRoute.Walkthrough
        Gate.App -> LaunchRoute.Today
    }

data class LaunchState(
    val pendingIntent: net.chronoflow.core.navigation.PendingLaunchIntent =
        net.chronoflow.core.navigation
            .PendingLaunchIntent(),
    val ending: LaunchEnding? = null,
    val session: SessionPresence? = null,
    val gate: Gate? = null,
    val hasStoredData: Boolean? = null,
    val isReady: Boolean = false,
    val showsStatus: Boolean = false,
    val statusStart: Duration? = null,
    val exitStart: Duration? = null,
    val moonTarget: LaunchRect? = null,
    val now: Duration = Duration.ZERO,
    val didForceReady: Boolean = false,
    val didCommitExit: Boolean = false,
    val isComplete: Boolean = false,
    val fallbackTarget: LaunchRect = LaunchRect(0.0, 0.0, 0.0, 0.0),
) {
    val route: LaunchRoute? get() =
        when (session) {
            SessionPresence.SignedOut -> LaunchRoute.Welcome
            is SessionPresence.SignedIn -> (gate ?: Gate.App.takeIf { didForceReady })?.launchRoute()
            null -> null
        }
    val welcomeDeadline: Duration?
        get() =
            if (ending == LaunchEnding.Welcome) {
                exitStart?.let { maxOf(Duration.ZERO, it - 50.milliseconds) }
            } else {
                null
            }
}

sealed interface LaunchEvent {
    data class Session(
        val presence: SessionPresence,
    ) : LaunchEvent

    data class GateKnown(
        val gate: Gate,
    ) : LaunchEvent

    data class StoredData(
        val stored: Boolean,
    ) : LaunchEvent

    data class MoonAnchor(
        val rect: LaunchRect,
    ) : LaunchEvent

    data class Reached(
        val time: Duration,
    ) : LaunchEvent

    data object ExitCompleted : LaunchEvent
}

sealed interface LaunchEffect {
    data class CommitStatus(
        val at: Duration,
    ) : LaunchEffect

    data object CancelStatus : LaunchEffect

    data class CommitExit(
        val ending: LaunchEnding,
        val at: Duration,
        val moonTarget: LaunchRect?,
    ) : LaunchEffect

    data object ForceReady : LaunchEffect
}

data class LaunchStep(
    val state: LaunchState,
    val effects: List<LaunchEffect>,
)

fun reduce(
    state: LaunchState,
    event: LaunchEvent,
    reduceMotion: Boolean,
): LaunchStep {
    if (state.isComplete) return LaunchStep(state, emptyList())
    var next = state
    val effects = mutableListOf<LaunchEffect>()
    var anchorChanged = false
    when (event) {
        is LaunchEvent.Session -> {
            if (next.session == null) {
                val signedOut = event.presence == SessionPresence.SignedOut
                next =
                    next.copy(
                        session = event.presence,
                        ending = if (signedOut) LaunchEnding.Welcome else LaunchEnding.App,
                    )
                if (signedOut) {
                    next =
                        next.copy(
                            exitStart = if (reduceMotion) next.now else maxOf(1600.milliseconds, next.now),
                        )
                }
            }
        }

        is LaunchEvent.GateKnown -> {
            if (!next.didForceReady) next = next.copy(gate = event.gate)
        }

        is LaunchEvent.StoredData -> {
            if (!next.didForceReady) next = next.copy(hasStoredData = event.stored)
        }

        is LaunchEvent.Reached -> {
            next = next.copy(now = maxOf(next.now, event.time))
        }

        is LaunchEvent.MoonAnchor -> {
            val rect = event.rect
            if (rect.width > 0 && rect.height > 0 &&
                listOf(rect.x, rect.y, rect.width, rect.height).all { it.isFinite() } &&
                (next.exitStart == null || next.now < requireNotNull(next.exitStart))
            ) {
                anchorChanged = next.moonTarget != rect
                next = next.copy(moonTarget = rect)
            }
        }

        LaunchEvent.ExitCompleted -> {
            return LaunchStep(next.copy(isComplete = true), emptyList())
        }
    }
    if (next.statusStart?.let { next.now >= it } == true) next = next.copy(showsStatus = true)
    if (next.now >= 8.seconds && !next.isReady) {
        next = next.copy(didForceReady = true)
        if (next.session == null) {
            next =
                next.copy(
                    session = SessionPresence.SignedOut,
                    ending = LaunchEnding.Welcome,
                    exitStart = if (reduceMotion) next.now else maxOf(1600.milliseconds, next.now),
                )
        }
        next = next.copy(isReady = true)
        if (next.session is SessionPresence.SignedIn) effects += LaunchEffect.ForceReady
    }
    next =
        next.copy(
            isReady =
                next.isReady || next.session == SessionPresence.SignedOut ||
                    (
                        next.session != null && next.gate != null &&
                            (next.gate != Gate.App || next.hasStoredData == true)
                    ),
        )
    if (next.isReady && !next.showsStatus && next.statusStart != null) {
        next = next.copy(statusStart = null)
        effects += LaunchEffect.CancelStatus
    }
    if (!next.isReady && next.hasStoredData == false && next.session != null && next.statusStart == null) {
        val start = maxOf(2.seconds, next.now)
        next = next.copy(statusStart = start, showsStatus = next.now >= start)
        effects += LaunchEffect.CommitStatus(start)
    }
    if (next.isReady) {
        when (next.ending) {
            LaunchEnding.App -> {
                if (!next.didCommitExit) {
                    val start = if (reduceMotion) next.now else maxOf(1500.milliseconds, next.now)
                    next = next.copy(exitStart = start, didCommitExit = true)
                    effects += LaunchEffect.CommitExit(LaunchEnding.App, start, null)
                }
            }

            LaunchEnding.Welcome -> {
                val start = requireNotNull(next.exitStart)
                val fallbackDue = next.now >= maxOf(Duration.ZERO, start - 50.milliseconds)
                if ((!next.didCommitExit || (anchorChanged && next.now < start)) &&
                    (reduceMotion || next.moonTarget != null || fallbackDue)
                ) {
                    next = next.copy(didCommitExit = true)
                    effects +=
                        LaunchEffect.CommitExit(
                            LaunchEnding.Welcome,
                            start,
                            next.moonTarget ?: next.fallbackTarget,
                        )
                }
            }

            null -> {}
        }
    }
    return LaunchStep(next, effects)
}
