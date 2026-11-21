package net.chronoflow.feature.settings.security

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsPage
import java.time.Instant

class PasswordInputs {
    val current = TextFieldState()
    val new = TextFieldState()
    val confirmation = TextFieldState()

    override fun toString(): String = "PasswordInputs(***)"
}

data class PasswordScreenState(
    val security: SecurityState,
    val inputs: PasswordInputs,
    val now: () -> Instant,
)

sealed interface PasswordEvent {
    data object Back : PasswordEvent

    class Save(
        val inputs: PasswordInputs,
    ) : PasswordEvent {
        override fun toString(): String = "Save(***)"
    }
}

@Composable
fun PasswordScreen(
    state: PasswordScreenState,
    onEvent: (PasswordEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val security = state.security
    val keyboard = LocalSoftwareKeyboardController.current
    SettingsPage(R.string.settings_password, { onEvent(PasswordEvent.Back) }, "settings.password.screen", modifier) {
        if (security.errors.isNotEmpty()) {
            CfNotice(
                securityText("Check the highlighted field."),
                securityText("Your new password isn't saved yet."),
                modifier = Modifier.testTag("settings.password.notice"),
                tone = CfNoticeTone.Warning,
            )
        }
        CfCard {
            CfSecureTextField(
                state.inputs.current,
                securityText("Current password"),
                testTag = "settings.password.current",
                error = security.errors["current_password"]?.let { securityText(it) },
                enabled = !security.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                contentType = ContentType.Password,
            )
            CfSecureTextField(
                state.inputs.new,
                securityText("New password"),
                testTag = "settings.password.new",
                error =
                    security.errors["password"]?.let {
                        securityText(it, security.errorArguments["password"].orEmpty())
                    },
                enabled = !security.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                contentType = ContentType.NewPassword,
            )
            CfSecureTextField(
                state.inputs.confirmation,
                securityText("Confirm new password"),
                testTag = "settings.password.confirmation",
                error = security.errors["password_confirmation"]?.let { securityText(it) },
                enabled = !security.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                onKeyboardAction = { keyboard?.hide() },
                contentType = ContentType.NewPassword,
            )
        }
        CfButton(
            securityText("Change password"),
            {
                keyboard?.hide()
                onEvent(PasswordEvent.Save(state.inputs))
            },
            Modifier.testTag("settings.password.save"),
            enabled = !security.busy && security.retryAt == null,
            loading = security.busy,
            loadingDescription = securityText("Checking…"),
            fillWidth = true,
            minHeight = PlanUITokens.actionMinHeight.dp,
        )
        SettingsSecurityLock(security.retryAt, state.now)
    }
}

@Composable
fun PasswordRoute(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val security by viewModel.state.collectAsStateWithLifecycle()
    val inputs = remember { PasswordInputs() }
    SecurityFeedbackHost(viewModel.ui, viewModel::feedbackPresented, viewModel::clearSecrets, modifier) {
        PasswordScreen(PasswordScreenState(security, inputs, viewModel.now), { event ->
            when (event) {
                PasswordEvent.Back -> {
                    onBack()
                }

                is PasswordEvent.Save -> {
                    viewModel.changePassword(
                        TextFieldSecret(event.inputs.current),
                        TextFieldSecret(event.inputs.new),
                        TextFieldSecret(event.inputs.confirmation),
                    )
                }
            }
        })
    }
}
