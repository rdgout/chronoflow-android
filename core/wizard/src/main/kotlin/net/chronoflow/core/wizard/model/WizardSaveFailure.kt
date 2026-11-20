@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

@Serializable
data class WizardSaveFailure(
    val kind: Kind,
    @Serializable(with = InstantMillisSerializer::class) val retryAt: Instant? = null,
) {
    enum class Kind { offline, server, throttle }

    fun seconds(at: Instant): Int {
        val retryAt = retryAt ?: return 0
        return ceil(Duration.between(at, retryAt).toMillis().coerceAtLeast(0) / 1000.0).toInt()
    }
}

enum class WizardSaveOutcome { signUpRequested }
