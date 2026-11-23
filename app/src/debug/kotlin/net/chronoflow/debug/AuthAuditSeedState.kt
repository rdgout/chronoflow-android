package net.chronoflow.debug

import net.chronoflow.core.model.DataFailure
import net.chronoflow.feature.auth.model.ForgotPasswordModel
import net.chronoflow.feature.auth.model.ResetPasswordModel
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.VerifyEmailModel

internal object AuthAuditSeedState {
    fun failure(state: String): DataFailure? =
        when (state) {
            "server", "failedServer" -> DataFailure.Server(500)
            "throttle", "failedThrottle" -> DataFailure.Throttled(60.0, 429)
            "offline", "failedOffline" -> DataFailure.Offline
            else -> null
        }

    suspend fun forgot(
        model: ForgotPasswordModel,
        state: String,
    ) {
        if (state == "form") return
        model.editEmail(if (state == "field") "invalid" else "unknown@example.com")
        val failure = failure(state)
        if (failure != null) model.form.receive(failure) else model.submit()
    }

    suspend fun reset(
        model: ResetPasswordModel,
        state: String,
    ) {
        when (state) {
            "form", "expired" -> Unit
            "password" -> model.submit(AuditSecret("short"), AuditSecret("short"))
            "confirmation" -> model.submit(AuditSecret("NewPassword123!"), AuditSecret("different"))
            "done" -> model.submit(AuditSecret("NewPassword123!"), AuditSecret("NewPassword123!"))
            else -> model.form.receive(checkNotNull(failure(state)) { "unknown reset audit state" })
        }
    }

    suspend fun verify(
        model: VerifyEmailModel,
        state: String,
    ) {
        if (state.startsWith("failed")) {
            model.receiveLink(null)
            model.activate()
        }
        if (state in setOf("countdown", "throttle", "failedThrottle")) model.resend()
        failure(state)?.let {
            model.form.receive(it)
            model.tick()
        }
    }
}

private class AuditSecret(
    private var value: String,
) : SecretField {
    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "AuditSecret(***)"
}
