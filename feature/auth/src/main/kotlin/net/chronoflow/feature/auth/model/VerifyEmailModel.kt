package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException
import kotlin.math.ceil
import kotlin.math.max

data class VerificationContext(
    val accountId: String,
    val email: String,
    val epoch: Long,
)

enum class VerifyState { Waiting, Failed }

data class VerifyUiState(
    val state: VerifyState = VerifyState.Waiting,
    val form: AuthFormState = AuthFormState(),
    val signOutFailure: DataFailure? = null,
    val sessionFailure: SessionFailure? = null,
    val hasStatus: Boolean = false,
    val deadline: Double? = null,
    val now: Double,
) {
    val remaining: Int get() = max(0, ceil((deadline ?: now) - now).toInt())
    val canResend: Boolean get() = !form.busy && remaining == 0 && (hasStatus || state == VerifyState.Failed)
}

class VerifyEmailModel(
    val context: VerificationContext,
    private val source: DataSource,
    private val nowSeconds: () -> Double,
    private val current: suspend () -> VerificationContext?,
    private val persist: suspend (VerificationContext) -> Boolean,
    private val route: suspend () -> Unit,
    private val discard: suspend () -> Unit,
    private val mail: (suspend () -> SignedVerificationInput?)? = null,
    scope: CoroutineScope,
    main: CoroutineDispatcher,
    private val io: CoroutineDispatcher,
) {
    val scope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]) + main)
    val form = AuthFormModel(emptySet(), AuthFormContext.Verify)
    private val mutableUiState = MutableStateFlow(VerifyUiState(now = nowSeconds()))
    val uiState = mutableUiState.asStateFlow()
    val state: VerifyState get() = uiState.value.state
    val remaining: Int get() = uiState.value.remaining
    val canResend: Boolean get() =
        !form.busy && uiState.value.remaining == 0 &&
            (uiState.value.hasStatus || state == VerifyState.Failed)
    val isActive: Boolean get() = active
    private var input: SignedVerificationInput? = null
    private var invalidLink = false
    private var active = true
    private var revision = 0L
    private var linkPending = false
    private var completed = false

    fun receiveLink(input: SignedVerificationInput?) {
        if (!active) return
        revision++
        this.input = input
        invalidLink = input == null
        linkPending = true
    }

    fun tick() {
        update { it.copy(now = nowSeconds()) }
    }

    private suspend fun isCurrent(revision: Long): Boolean {
        if (!active || this.revision != revision || !currentCoroutineContext().isActive) return false
        val value = current()
        return active && this.revision == revision && currentCoroutineContext().isActive && value == context
    }

    suspend fun activate() {
        tick()
        check(silent = input == null && !invalidLink)
    }

    suspend fun check(silent: Boolean = false) {
        if (!active || completed || !form.begin(AuthFormModel.Action.CheckVerification)) return
        update { it.copy(signOutFailure = null, sessionFailure = null) }
        val revision = revision
        val submitted = input
        val malformed = invalidLink
        linkPending = false
        try {
            attempt(onFailure = { failure ->
                if (isCurrent(revision)) {
                    if (failure is SessionFailure) update { it.copy(sessionFailure = failure) }
                    if (!silent ||
                        uiState.value.sessionFailure != null
                    ) {
                        form.receive(failure as? DataFailure ?: DataFailure.Server(null))
                    }
                    if (!silent) update { it.copy(state = VerifyState.Failed) }
                }
            }) {
                if (!isCurrent(revision)) return@attempt
                if (malformed) {
                    invalidLink = false
                    update { it.copy(state = VerifyState.Failed) }
                    return@attempt
                }
                val status =
                    if (submitted != null) {
                        try {
                            withContext(io) { source.verifyEmail(submitted) }
                        } catch (failure: DataFailure.Validation) {
                            if (isCurrent(revision)) input = null
                            throw failure
                        }
                    } else {
                        withContext(io) { source.verificationStatus() }
                    }
                if (!isCurrent(revision) || status.email != context.email) return@attempt
                input = null
                accept(status, revision)
                if (!isCurrent(revision)) return@attempt
                update { it.copy(state = VerifyState.Waiting) }
            }
        } finally {
            form.finish()
            update { it }
            if (active && linkPending) scope.launch { check() }
        }
    }

    suspend fun resend() {
        tick()
        if (!active || completed || !canResend || !form.begin(AuthFormModel.Action.ResendVerification)) return
        update { it.copy(signOutFailure = null, sessionFailure = null) }
        val revision = revision
        try {
            attempt(onFailure = { failure ->
                if (isCurrent(revision)) {
                    if (failure is SessionFailure) update { it.copy(sessionFailure = failure) }
                    val mapped = failure as? DataFailure ?: DataFailure.Server(null)
                    if (mapped is DataFailure.Throttled) {
                        tick()
                        update {
                            it.copy(
                                deadline =
                                    max(
                                        it.deadline ?: it.now,
                                        it.now + max(0.0, mapped.retryAfterSeconds),
                                    ),
                            )
                        }
                    }
                    form.receive(mapped)
                }
            }) {
                if (!isCurrent(revision)) return@attempt
                val status = withContext(io) { source.resendVerificationEmail() }
                if (!isCurrent(revision) || status.email != context.email) return@attempt
                accept(status, revision)
                if (!isCurrent(revision)) return@attempt
                update { it.copy(state = VerifyState.Waiting) }
            }
        } finally {
            form.finish()
            update { it }
        }
    }

    private suspend fun accept(
        status: EmailVerificationStatus,
        revision: Long,
    ) {
        if (!isCurrent(revision) || status.email != context.email) return
        update { it.copy(hasStatus = true) }
        tick()
        if (!status.hasVerifiedEmail) {
            update { it.copy(deadline = max(it.deadline ?: it.now, it.now + max(0, status.resendAfterSeconds))) }
            return
        }
        val identity = withContext(io) { source.fetchIdentity() }
        if (!isCurrent(revision) || identity.id != context.accountId || identity.email != context.email ||
            !identity.hasVerifiedEmail
        ) {
            return
        }
        if (!persist(context) || !isCurrent(revision)) return
        completed = true
        route()
    }

    suspend fun openEmail(open: suspend () -> Boolean) {
        if (!active || form.busy) return
        if (mail != null) {
            mail()?.let {
                receiveLink(it)
                check()
            }
        } else if (open()) {
            check(silent = true)
        }
    }

    suspend fun signOut() {
        if (!active || form.busy || current() != context || !active || !form.begin(AuthFormModel.Action.SignOut)) return
        update { it.copy(signOutFailure = null, sessionFailure = null) }
        revision++
        try {
            attempt(onFailure = { failure ->
                if (active) {
                    val mapped = failure as? DataFailure ?: DataFailure.Server(null)
                    update { it.copy(signOutFailure = mapped, sessionFailure = failure as? SessionFailure) }
                    form.receive(mapped)
                }
            }) {
                discard()
                cancel()
            }
        } finally {
            form.finish()
            update { it }
        }
    }

    fun cancel() {
        active = false
        revision++
        input = null
        invalidLink = false
        form.clear()
        update { it }
        scope.cancel()
    }

    fun takeAnnouncement(resolve: (AuthText) -> String): String? =
        form.takeAnnouncement(resolve)?.let { message ->
            uiState.value.signOutFailure?.let { failure ->
                resolve(AuthText(if (failure == DataFailure.Offline) "Needs a connection" else "Something went wrong"))
            } ?: message
        }

    private inline fun update(transform: (VerifyUiState) -> VerifyUiState) {
        mutableUiState.value = transform(uiState.value).copy(form = form.state.value)
    }

    private suspend fun attempt(
        onFailure: suspend (Throwable) -> Unit,
        action: suspend () -> Unit,
    ) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: SessionFailure) {
            onFailure(failure)
        } catch (failure: DataFailure) {
            onFailure(failure)
        } catch (failure: DeviceIdentityFailure) {
            onFailure(failure)
        } catch (failure: StoreFailure) {
            onFailure(failure)
        } catch (failure: IOException) {
            onFailure(failure)
        } catch (failure: GeneralSecurityException) {
            onFailure(failure)
        } catch (failure: RuntimeException) {
            onFailure(failure)
        }
    }
}
