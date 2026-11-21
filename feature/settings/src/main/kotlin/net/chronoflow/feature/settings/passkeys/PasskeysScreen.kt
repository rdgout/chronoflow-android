package net.chronoflow.feature.settings.passkeys

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.settingsFormatArguments
import net.chronoflow.feature.settings.security.SecretField
import net.chronoflow.feature.settings.security.SecurityFeedbackHost
import net.chronoflow.feature.settings.security.SecurityState
import net.chronoflow.feature.settings.security.SettingsSecurityLock
import net.chronoflow.feature.settings.security.TextFieldSecret
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPrompt
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPromptHost
import java.time.Instant

class PasskeysInputs {
    val password = TextFieldState()

    override fun toString() = "PasskeysInputs(***)"
}

data class PasskeysScreenState(
    val security: SecurityState = SecurityState(),
    val meta: Map<Long, String> = emptyMap(),
    val canRegister: Boolean = true,
    val refreshing: Boolean = false,
    val email: String = "",
    val inputs: PasskeysInputs,
    val now: () -> Instant,
)

sealed interface PasskeysEvent {
    data object Back : PasskeysEvent

    data object Refresh : PasskeysEvent

    data object Add : PasskeysEvent

    data class Remove(
        val passkey: ApiPasskey,
    ) : PasskeysEvent

    data object Cancel : PasskeysEvent

    class Submit(
        val password: SecretField,
    ) : PasskeysEvent

    data object Reset : PasskeysEvent
}

@Composable
fun PasskeysScreen(
    state: PasskeysScreenState,
    onEvent: (PasskeysEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CfPullToRefreshBox(state.refreshing, { onEvent(PasskeysEvent.Refresh) }, modifier) {
        SettingsPage(R.string.settings_passkeys, { onEvent(PasskeysEvent.Back) }, "settings.passkeys.screen") {
            PasskeysContent(state, onEvent)
            SettingsSecurityLock(state.security.retryAt, state.now)
        }
    }
    val security = state.security
    SettingsPasswordPromptHost(security.prompt != null, security.busy, { onEvent(PasskeysEvent.Cancel) }) {
        security.prompt?.let { prompt ->
            SettingsPasswordPrompt(
                prompt.detail,
                prompt.button,
                prompt.destructive,
                state.inputs.password,
                security.busy,
                security.retryAt,
                state.now,
                security.errors["current_password"],
                security.message,
                state.email,
                { onEvent(PasskeysEvent.Cancel) },
                { onEvent(PasskeysEvent.Submit(TextFieldSecret(state.inputs.password))) },
                { onEvent(PasskeysEvent.Reset) },
                detailArgs = mapOf("name" to security.removal?.name.orEmpty()),
                errorArgs = security.errorArguments["current_password"].orEmpty(),
                identifier = "settings.passkeys.prompt",
            )
        }
    }
}

@Composable
fun PasskeysRoute(
    viewModel: PasskeysViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val back by rememberUpdatedState(onBack)
    if (!viewModel.passkeysEnabled) {
        LaunchedEffect(viewModel) { back() }
        return
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val language = Language.of(resources.configuration.locales[0].language)
    LaunchedEffect(viewModel, resources, language) {
        viewModel.setLanguage(language) { key, args ->
            val entry = checkNotNull(CopyIndex.find(CopyTable.Settings, key))
            resources.getString(entry.resId, *settingsFormatArguments(entry.args, args))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    val inputs = remember { PasskeysInputs() }
    LaunchedEffect(ui.security.prompt) { inputs.password.clearText() }
    SecurityFeedbackHost(viewModel.ui, viewModel::feedbackPresented, {
        inputs.password.clearText()
        viewModel.clearSecrets()
    }) {
        PasskeysScreen(
            PasskeysScreenState(
                ui.security,
                meta,
                viewModel.canRegisterPasskey,
                refreshing,
                ui.pages.account
                    ?.email
                    .orEmpty(),
                inputs,
                viewModel.now,
            ),
            { event ->
                if (event == PasskeysEvent.Back) onBack() else viewModel.event(event)
            },
            modifier,
        )
    }
}
