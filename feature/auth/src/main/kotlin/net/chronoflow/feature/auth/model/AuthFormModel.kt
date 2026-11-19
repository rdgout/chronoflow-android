package net.chronoflow.feature.auth.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage

data class AuthFormState(
    val pending: AuthFormModel.Action? = null,
    val banner: AuthBannerKind? = null,
    val fields: Map<String, FieldMessage> = emptyMap(),
    val failure: DataFailure? = null,
    val retryAfterSeconds: Double? = null,
    val announcementRevision: Int = 0,
) {
    val busy: Boolean get() = pending != null
}

class AuthFormModel(
    private var displayedFields: Set<String> = setOf("email", "password"),
    val context: AuthFormContext = AuthFormContext.SignIn,
) {
    enum class Action {
        Password,
        Passkey,
        Verify,
        SendResetLink,
        Reset,
        Register,
        CheckVerification,
        ResendVerification,
        SignOut,
    }

    private val mutableState = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = mutableState.asStateFlow()
    val busy: Boolean get() = state.value.busy
    private var announcedRevision = 0

    fun showFields(fields: Set<String>) {
        if (busy) return
        displayedFields = fields
        clear()
    }

    fun begin(action: Action): Boolean {
        if (busy) return false
        mutableState.value = state.value.copy(pending = action)
        clear()
        return true
    }

    fun finish() {
        mutableState.value = state.value.copy(pending = null)
    }

    fun edit(field: String) {
        if (busy) return
        mutableState.value = state.value.copy(banner = null, failure = null, fields = state.value.fields - field)
    }

    fun clear() {
        mutableState.value =
            state.value.copy(banner = null, fields = emptyMap(), failure = null, retryAfterSeconds = null)
    }

    fun receive(failure: DataFailure) {
        val mapped = AuthFailureMapping(failure, displayedFields, context)
        mutableState.value =
            state.value.copy(
                failure = failure,
                banner = mapped.banner,
                fields = mapped.fields,
                retryAfterSeconds = mapped.retryAfterSeconds,
                announcementRevision =
                    state.value.announcementRevision + 1,
            )
    }

    fun takeAnnouncement(resolve: (AuthText) -> String): String? {
        val current = state.value
        if (announcedRevision == current.announcementRevision) return null
        val messages = mutableListOf<String>()
        current.banner?.let { banner ->
            messages += resolve(AuthText(banner.title(context)))
            val body = banner.body(context)
            if (body.isNotEmpty()) messages += resolve(AuthText(body))
        }
        messages +=
            current.fields.keys
                .sorted()
                .mapNotNull { current.fields[it]?.let(AuthFieldCopy::field)?.let(resolve) }
        if (messages.isEmpty()) return null
        announcedRevision = current.announcementRevision
        return messages.joinToString(" ")
    }
}
