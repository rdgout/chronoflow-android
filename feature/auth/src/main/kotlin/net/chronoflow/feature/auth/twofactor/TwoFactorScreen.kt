package net.chronoflow.feature.auth.twofactor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.TwoFactorModel
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.resolve

@Composable
fun TwoFactorScreen(viewModel: TwoFactorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(Unit) { viewModel.validateChallenge() }
    BackHandler { viewModel.back() }
    state?.let { current ->
        val field = remember(viewModel.currentModel, current.mode) { TextFieldState(current.code) }
        LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect(viewModel::onCodeEdited) }
        LaunchedEffect(field, current.code) {
            if (field.text.toString() != current.code) field.setTextAndPlaceCursorAtEnd(current.code)
        }
        TwoFactorContent(
            current,
            field,
            viewModel::submit,
            viewModel::toggleMode,
            viewModel::back,
            takeAnnouncement = { viewModel.takeAnnouncement { it.resolve(resources) } },
        )
    }
}

@Composable
fun TwoFactorContent(
    state: TwoFactorUiState,
    field: TextFieldState,
    onSubmit: () -> Unit,
    onToggle: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    AuthScreen(
        scrollTag = "two-factor.scroll",
        title = stringResource(R.string.auth_enter_your_code),
        description = stringResource(R.string.auth_open_your_authenticator_app_and_type_371676),
        onBack = onBack,
        modifier = modifier,
        announcementRevision = state.form.announcementRevision,
        takeAnnouncement = takeAnnouncement,
    ) {
        val resources = LocalResources.current
        val focus = LocalFocusManager.current
        val requester = remember(state.mode) { FocusRequester() }
        val banner = remember { BringIntoViewRequester() }
        val coordinates = LocalAuthCoordinates.current
        var focused by remember { mutableStateOf<String?>(null) }
        val enabled = !state.form.busy
        AuthFailureFeedback(state.form, listOf(state.field to requester), banner)
        AuthKeyboardReveal(focused, state.form.announcementRevision)
        state.form.banner?.let {
            AuthBanner(
                it,
                Modifier.bringIntoViewRequester(banner),
                AuthFormContext.TwoFactor,
            )
        }
        key(state.mode) {
            val spec = remember(state.mode) { TwoFactorFieldSpec.forMode(state.mode) }
            CfTextField(
                field,
                stringResource(spec.label),
                Modifier
                    .focusRequester(requester)
                    .onFocusChanged {
                        focused = if (it.isFocused) state.field else null
                    }.onPlaced { coordinates.fields[state.field] = it },
                testTag = spec.tag,
                placeholder = spec.placeholder?.let { stringResource(it) },
                hint = spec.hint?.let { stringResource(it) },
                error =
                    state.form.fields[state.field]
                        ?.let(AuthFieldCopy::field)
                        ?.resolve(resources),
                enabled = enabled,
                keyboardOptions = spec.keyboardOptions,
                onKeyboardAction = { onSubmit() },
                contentType = spec.contentType,
                inputTransformation =
                    remember(state.mode) {
                        if (state.mode == TwoFactorModel.Mode.Code) TwoFactorCodeTransformation() else null
                    },
            )
        }
        val verify = stringResource(R.string.auth_verify)
        CfButton(
            verify,
            onSubmit,
            Modifier.testTag("two-factor.submit").onPlaced { coordinates.submit = it },
            style = CfButtonStyle.Primary,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = enabled,
            loading = state.form.pending == AuthFormModel.Action.Verify,
            loadingDescription = verify,
        )
        CfButton(
            stringResource(
                if (state.mode == TwoFactorModel.Mode.Code) {
                    R.string.auth_use_a_recovery_code_instead
                } else {
                    R.string.auth_use_your_authenticator_app
                },
            ),
            {
                focus.clearFocus()
                onToggle()
            },
            Modifier.testTag("two-factor.toggle"),
            style = CfButtonStyle.Ghost,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = enabled,
        )
    }
}
