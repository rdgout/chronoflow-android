package net.chronoflow.core.sync

import net.chronoflow.core.model.DataFailure
import kotlin.math.pow

object OutboxFailurePolicy {
    fun isFinal(failure: DataFailure): Boolean =
        failure !is DataFailure.EmailUnverified &&
            failure.status?.let { it in 400..499 && it !in setOf(401, 408, 425, 429) } == true

    fun sanitized(failure: DataFailure): String =
        when (failure) {
            DataFailure.ReviewExpired -> "review_expired"
            DataFailure.NotEnoughCredits -> "not_enough_credits"
            DataFailure.SaveCapacity -> "save_capacity"
            is DataFailure.ProfileInvalid -> "profile_invalid"
            DataFailure.Offline -> "offline"
            is DataFailure.Unauthorized -> "unauthorized"
            is DataFailure.EmailUnverified -> "email_unverified"
            is DataFailure.Validation -> "validation"
            is DataFailure.Throttled -> "throttled"
            is DataFailure.Conflict -> "conflict"
            is DataFailure.Server -> "server"
            is DataFailure.Unsupported -> "unsupported"
        }

    fun retryDelay(
        attempts: Int,
        retryAfter: Double = 0.0,
    ): Double = maxOf(2.0.pow(attempts.coerceIn(0, 9)).coerceAtMost(300.0), retryAfter)
}
