@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.settings.delete

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsLockHint
import net.chronoflow.feature.settings.security.SecretField
import net.chronoflow.feature.settings.security.TextFieldSecret
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPrompt
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPromptHost
import java.time.Instant

class DeleteAccountInputs {
    val password = TextFieldState()

    override fun toString() = "DeleteAccountInputs(***)"
}

data class DeleteAccountScreenState(
    val ui: DeleteAccountUiState = DeleteAccountUiState(),
    val inputs: DeleteAccountInputs,
    val now: () -> Instant,
)

sealed interface DeleteAccountEvent {
    data object Confirm : DeleteAccountEvent

    data object Dismiss : DeleteAccountEvent

    class Submit(
        val password: SecretField,
    ) : DeleteAccountEvent

    data object Reset : DeleteAccountEvent
}

@Composable
fun DeleteAccountSheet(
    state: DeleteAccountScreenState,
    onEvent: (DeleteAccountEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.ui.delete.stage) {
        SettingsDeleteModel.Stage.Confirmation -> {
            CfModalBottomSheet(
                onDismissRequest = { onEvent(DeleteAccountEvent.Dismiss) },
                modifier = modifier.testTag("settings.delete.screen"),
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !state.ui.busy),
            ) {
                DeleteAccountContent(
                    onEvent,
                    Modifier
                        .windowInsetsPadding(
                            LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        ).verticalScroll(rememberScrollState()),
                )
            }
        }

        SettingsDeleteModel.Stage.Password -> {
            SettingsPasswordPromptHost(
                true,
                state.ui.busy,
                { onEvent(DeleteAccountEvent.Dismiss) },
            ) {
                Column(modifier.testTag("settings.delete.screen")) { DeleteAccountPrompt(state, onEvent) }
            }
        }

        SettingsDeleteModel.Stage.Deleted -> {
            return
        }
    }
}

@Composable
fun DeleteAccountContent(
    onEvent: (DeleteAccountEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = PlanUITokens.space5.dp)
            .padding(top = PlanUITokens.space3.dp, bottom = PlanUITokens.space8.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            Text(
                stringResource(R.string.settings_delete_your_account),
                Modifier.semantics { heading() },
                style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            )
            Text(
                stringResource(R.string.settings_your_trips_plans_body_clock_answ_6d6c52),
                style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                color = CfTheme.colors.mutedForeground,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            CfButton(
                stringResource(R.string.settings_delete_my_account),
                { onEvent(DeleteAccountEvent.Confirm) },
                Modifier.testTag("settings.delete.confirm"),
                style = CfButtonStyle.Destructive,
                fillWidth = true,
                minHeight = PlanUITokens.actionMinHeight.dp,
            )
            SettingsLockHint(stringResource(R.string.settings_you_ll_be_asked_for_your_password_next))
            CfButton(
                stringResource(R.string.settings_keep_my_account),
                { onEvent(DeleteAccountEvent.Dismiss) },
                Modifier.testTag("settings.delete.keep"),
                style = CfButtonStyle.Ghost,
                fillWidth = true,
                minHeight = PlanUITokens.walkthroughDoneHeight.dp,
            )
        }
    }
}

@Composable
internal fun DeleteAccountPrompt(
    state: DeleteAccountScreenState,
    onEvent: (DeleteAccountEvent) -> Unit,
) {
    SettingsPasswordPrompt(
        "Enter your password to delete your account. This can't be undone.",
        "Delete account",
        true,
        state.inputs.password,
        state.ui.busy,
        state.ui.retryAt,
        state.now,
        state.ui.delete.errorKey,
        state.ui.reset.message,
        state.ui.email,
        { onEvent(DeleteAccountEvent.Dismiss) },
        { onEvent(DeleteAccountEvent.Submit(TextFieldSecret(state.inputs.password))) },
        { onEvent(DeleteAccountEvent.Reset) },
        identifier = "settings.delete.prompt",
        fieldTag = "settings.delete.password",
        submitTag = "settings.delete.submit",
        errorTag = "settings.delete.error",
    )
}

@Composable
fun DeleteAccountRoute(
    viewModel: DeleteAccountViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val inputs = remember { DeleteAccountInputs() }
    val activity = LocalActivity.current
    val leaving by rememberUpdatedState(viewModel::stop)
    DisposableEffect(viewModel, activity) {
        onDispose {
            inputs.password.clearText()
            if (activity?.isChangingConfigurations != true) leaving()
        }
    }
    DeleteAccountSheet(DeleteAccountScreenState(ui, inputs, viewModel.now), { event ->
        viewModel.event(event)
        if (event == DeleteAccountEvent.Dismiss) onDismiss()
    }, modifier)
}
