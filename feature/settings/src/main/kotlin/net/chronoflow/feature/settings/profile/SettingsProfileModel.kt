package net.chronoflow.feature.settings.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.feature.settings.settingsAttempt

enum class ProfileError { EnterName, CouldNotSave }

data class ProfileNameReplacement(
    val id: Long,
    val previous: String,
    val value: String,
)

data class SettingsProfileState(
    val name: String,
    val email: String,
    val busy: Boolean = false,
    val saved: Boolean = false,
    val error: ProfileError? = null,
    val nameReplacement: ProfileNameReplacement? = null,
)

class SettingsProfileModel(
    account: AccountRecord,
    draft: String? = null,
    private val save: suspend (String) -> Unit,
) {
    private var storedName = account.name
    private val mutableUi = MutableStateFlow(SettingsProfileState(draft ?: account.name, account.email))
    val ui = mutableUi.asStateFlow()

    fun edit(name: String) {
        mutableUi.update { it.copy(name = name) }
    }

    fun receive(account: AccountRecord) {
        mutableUi.update {
            val received = if (it.name == storedName) it.replacingName(account.name) else it
            received.copy(
                saved = it.saved && account.name == storedName,
                email = account.email,
            )
        }
        storedName = account.name
    }

    suspend fun submit() {
        if (ui.value.busy) return
        val value = ui.value.name.trim()
        if (value.isEmpty() || value.codePointCount(0, value.length) > 255) {
            mutableUi.update { it.copy(error = ProfileError.EnterName, saved = false) }
            return
        }
        mutableUi.update { it.copy(busy = true, saved = false) }
        try {
            settingsAttempt(
                failed = { mutableUi.update { it.copy(error = ProfileError.CouldNotSave) } },
                cancelled = {},
            ) {
                save(value)
                storedName = value
                mutableUi.update { it.replacingName(value).copy(error = null, saved = true) }
            }
        } finally {
            mutableUi.update { it.copy(busy = false) }
        }
    }
}

private fun SettingsProfileState.replacingName(value: String): SettingsProfileState =
    if (value == name) {
        this
    } else {
        copy(
            name = value,
            nameReplacement = ProfileNameReplacement((nameReplacement?.id ?: 0) + 1, name, value),
        )
    }
