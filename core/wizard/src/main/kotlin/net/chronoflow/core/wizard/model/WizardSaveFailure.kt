@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data class WizardSaveFailure(
    val kind: Kind,
    @Serializable(with = InstantMillisSerializer::class) val retryAt: Instant? = null,
) {
    enum class Kind { offline, server, throttle }

    fun seconds(at: Instant): Int {
        val retryAt = retryAt ?: return 0
        val remaining = Duration.between(at, retryAt)
        if (remaining.isNegative || remaining.isZero) return 0
        return (remaining.seconds + if (remaining.nano > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}

enum class WizardSaveOutcome { signUpRequested }
