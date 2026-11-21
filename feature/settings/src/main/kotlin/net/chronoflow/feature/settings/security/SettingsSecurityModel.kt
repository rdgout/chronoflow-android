package net.chronoflow.feature.settings.security

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.session.StaleSessionException
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

enum class SettingsSecurityPrompt(
    val detail: String,
    val button: String,
) {
    Enable("Enter your password to turn on two-factor authentication.", "Continue"),
    Disable("Enter your password to turn off two-factor authentication.", "Turn off"),
    Reveal("Enter your password to show your recovery codes.", "Show codes"),
    Regenerate("Enter your password to make new recovery codes. Your current codes stop working.", "Make new codes"),
    AddPasskey("Enter your password to add a passkey on this phone.", "Add a passkey"),
    RemovePasskey(
        "Are you sure you want to remove the “{{name}}” passkey? " +
            "You will no longer be able to use it to sign in.",
        "Remove passkey",
    ),
    ;

    val destructive: Boolean get() = this == Disable

    fun change(password: String): AccountSecurityChange? =
        when (this) {
            Enable -> AccountSecurityChange.EnableTwoFactor(password)
            Disable -> AccountSecurityChange.DisableTwoFactor(password)
            Reveal -> AccountSecurityChange.RevealRecoveryCodes(password)
            Regenerate -> AccountSecurityChange.RegenerateRecoveryCodes(password)
            AddPasskey, RemovePasskey -> null
        }
}

data class SecurityState(
    val prompt: SettingsSecurityPrompt? = null,
    val setup: ApiTwoFactorSetup? = null,
    val codes: List<String> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val errors: Map<String, String> = emptyMap(),
    val errorArguments: Map<String, Map<String, String>> = emptyMap(),
    val retryAt: Instant? = null,
    val offline: Boolean = false,
    val feedbackRevision: Long = 0,
) {
    val twoFactorActivationFailed: Boolean get() = message == "Couldn't turn on two-factor."

    override fun toString(): String =
        "SecurityState(prompt=$prompt, setup=***, codes=***, busy=$busy, retryAt=$retryAt)"
}

class SettingsSecurityModel(
    private val perform: suspend (AccountSecurityChange) -> AccountSecurityResult,
    private val now: () -> Instant,
) {
    private val mutableState = MutableStateFlow(SecurityState())
    val state = mutableState.asStateFlow()
    private var generation = 0L
    val locked: Boolean get() = state.value.retryAt?.isAfter(now()) == true

    fun remaining(at: Instant): Int = remainingSeconds(state.value.retryAt, at)

    fun refreshLock() {
        if (state.value.retryAt?.isAfter(now()) == false) mutableState.update { it.copy(retryAt = null) }
    }

    suspend fun changePassword(
        current: SecretField,
        new: SecretField,
        confirmation: SecretField,
    ) {
        if (state.value.busy || locked) return
        mutableState.update { it.copy(errors = emptyMap()) }
        val old = current.read()
        val next = new.read()
        val repeated = confirmation.read()
        if (old.isEmpty() || next.isEmpty()) {
            fieldError("password", "Enter your password.")
            return
        }
        if (next != repeated) {
            fieldError("password_confirmation", "The passwords don't match.")
            return
        }
        current.clear()
        new.clear()
        confirmation.clear()
        if (run(AccountSecurityChange.Password(old, next, repeated))) message("Password updated")
    }

    fun showPrompt(action: SettingsSecurityPrompt) {
        if (state.value.busy) return
        mutableState.update { it.copy(errors = emptyMap(), message = null, prompt = action) }
    }

    fun cancelPrompt() {
        if (state.value.busy) return
        mutableState.update { it.copy(prompt = null, errors = emptyMap(), message = null) }
    }

    suspend fun submitPrompt(password: SecretField) {
        val action = state.value.prompt
        val value = password.read()
        if (action == null || value.isEmpty() || state.value.busy || locked) return
        password.clear()
        val change = action.change(value) ?: return
        if (run(change)) mutableState.update { it.copy(prompt = null) }
    }

    suspend fun confirm(code: SecretField) {
        if (state.value.busy || locked) return
        val value = code.read()
        if (value.length != 6 || value.any { it !in '0'..'9' }) {
            fieldError("code", "Enter the 6-digit code from your authenticator app.")
            return
        }
        code.clear()
        if (run(AccountSecurityChange.ConfirmTwoFactor(value))) {
            message("Two-factor is on. Save your recovery codes now.")
        }
    }

    suspend fun sendReset(email: String) {
        if (state.value.busy || locked) return
        if (run(AccountSecurityChange.RequestPasswordReset(email))) message("Check your inbox.")
    }

    private suspend fun run(change: AccountSecurityChange): Boolean {
        val started = generation
        mutableState.update { it.copy(busy = true, errors = emptyMap(), message = null) }
        try {
            val result = perform(change)
            currentCoroutineContext().ensureActive()
            if (generation != started) return false
            mutableState.update { old ->
                val next = old.copy(retryAt = null, offline = false)
                when (result) {
                    is AccountSecurityResult.Setup -> next.copy(setup = result.setup, codes = emptyList())
                    is AccountSecurityResult.Codes -> next.copy(codes = result.codes.recoveryCodes, setup = null)
                    is AccountSecurityResult.Snapshot -> next.copy(setup = null, codes = emptyList())
                    else -> next
                }
            }
            return true
        } catch (_: StaleSessionException) {
            currentCoroutineContext().ensureActive()
            return false
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: DataFailure) {
            if (generation != started) return false
            when (failure) {
                DataFailure.Offline -> {
                    mutableState.update { it.copy(offline = true) }
                    message("Needs a connection.")
                }

                is DataFailure.Throttled -> {
                    val retryAt =
                        now().plus(
                            failure.retryAfterSeconds
                                .coerceAtLeast(0.0)
                                .seconds
                                .toJavaDuration(),
                        )
                    mutableState.update { it.copy(retryAt = retryAt) }
                    message("Too many attempts.")
                }

                is DataFailure.Validation -> {
                    failure.fields.forEach { (field, value) -> validation(field, value) }
                    if (failure.fields.isEmpty()) message(failureMessage(change))
                }

                else -> {
                    message(failureMessage(change))
                }
            }
            return false
        } finally {
            if (generation == started) mutableState.update { it.copy(busy = false) }
        }
    }

    private fun validation(
        field: String,
        value: FieldMessage,
    ) {
        when (field) {
            "credential", "name" -> {
                message(REGISTRATION_FAILURE)
                return
            }

            "challengeId" -> {
                message("The passkey registration request expired. Please try again.")
                return
            }
        }
        when (value.key) {
            "The provided password was incorrect.", "Use both uppercase and lowercase letters.",
            "Use at least one number.", "Use at least one symbol.",
            -> {
                fieldError(field, value.key)
            }

            "Use at least :min characters." -> {
                fieldError(field, "Use at least {{min}} characters.")
                value.arguments["min"]?.takeUnless { it.isString }?.content?.toBigDecimalOrNull()?.let { min ->
                    val arguments = mapOf("min" to min.stripTrailingZeros().toPlainString())
                    mutableState.update { it.copy(errorArguments = it.errorArguments + (field to arguments)) }
                }
            }

            "The passwords don't match yet." -> {
                fieldError(field, "The passwords don't match.")
            }

            "Two-factor authentication is already on.",
            "Two-factor authentication is off.",
            "Start two-factor setup again.",
            -> {
                message(value.key)
                mutableState.update { it.copy(setup = null, codes = emptyList()) }
            }

            "The authentication code was not accepted. Try again." -> {
                fieldError(
                    field,
                    "That code didn't work. Codes change every 30 seconds; try the newest one.",
                )
            }

            else -> {
                fieldError(field, "Check the highlighted field.")
            }
        }
    }

    private fun fieldError(
        field: String,
        key: String,
    ) {
        mutableState.update { it.copy(errors = it.errors + (field to key)) }
    }

    internal fun message(key: String) {
        mutableState.update { it.copy(message = key, feedbackRevision = it.feedbackRevision + 1) }
    }

    fun clearSecrets() {
        generation++
        mutableState.update {
            it.copy(
                busy = false,
                message = null,
                errors = emptyMap(),
                errorArguments = emptyMap(),
                setup = null,
                codes = emptyList(),
                prompt = null,
            )
        }
    }

    private fun failureMessage(change: AccountSecurityChange): String =
        when (change) {
            is AccountSecurityChange.EnableTwoFactor,
            is AccountSecurityChange.ConfirmTwoFactor,
            -> "Couldn't turn on two-factor."

            is AccountSecurityChange.PasskeyCreationOptions, is AccountSecurityChange.AddPasskey -> REGISTRATION_FAILURE

            else -> "Something went wrong"
        }

    companion object {
        private const val REGISTRATION_FAILURE = "We couldn't register that passkey. Please try again."
    }
}

internal fun remainingSeconds(
    retryAt: Instant?,
    at: Instant,
): Int =
    retryAt?.let {
        ceil(Duration.between(at, it).toNanos().toDouble() / 1_000_000_000)
            .coerceIn(0.0, Int.MAX_VALUE.toDouble())
            .toInt()
    } ?: 0
