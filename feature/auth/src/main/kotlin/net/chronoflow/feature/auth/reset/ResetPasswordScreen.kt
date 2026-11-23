package net.chronoflow.feature.auth.reset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.forgot.ForgotPasswordRoute
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.ResetPasswordModel
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthHeadingGlyph
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.TextFieldSecret
import net.chronoflow.feature.auth.shell.UserEditReporter
import net.chronoflow.feature.auth.shell.resolve

data class ResetPasswordUiState(
    val form: AuthFormState = AuthFormState(),
    val state: ResetPasswordModel.State = ResetPasswordModel.State.Form,
)

sealed interface ResetPasswordEvent {
    data object EditPassword : ResetPasswordEvent

    data object EditConfirmation : ResetPasswordEvent

    data object Submit : ResetPasswordEvent

    data object Back : ResetPasswordEvent
}

@Composable
fun ResetPasswordRoute(
    model: ResetPasswordModel,
    openEmailApp: () -> Boolean,
) {
    val state by model.state.collectAsStateWithLifecycle()
    if (state == ResetPasswordModel.State.Expired) {
        ForgotPasswordRoute(model.recovery, expired = true, openEmailApp)
        return
    }
    val form by model.form.state.collectAsStateWithLifecycle()
    val password = remember(model) { TextFieldState() }
    val confirmation = remember(model) { TextFieldState() }
    val secret = remember(password) { TextFieldSecret(password) }
    val confirmed = remember(confirmation) { TextFieldSecret(confirmation) }
    val scope = rememberCoroutineScope()
    var request by remember(model) { mutableStateOf<Job?>(null) }
    var announcedDone by remember(model) { mutableStateOf(state == ResetPasswordModel.State.Done) }
    val focus = LocalFocusManager.current
    val resources = LocalResources.current
    LaunchedEffect(state) { focus.clearFocus() }
    DisposableEffect(model) {
        onDispose {
            request?.cancel()
            secret.clear()
            confirmed.clear()
        }
    }
    ResetPasswordScreen(
        ResetPasswordUiState(form, state),
        password,
        confirmation,
        onEvent = { event ->
            when (event) {
                ResetPasswordEvent.EditPassword -> {
                    model.editPassword()
                }

                ResetPasswordEvent.EditConfirmation -> {
                    model.editConfirmation()
                }

                ResetPasswordEvent.Submit -> {
                    if (request == null && !model.form.busy) {
                        val job =
                            scope.launch(start = CoroutineStart.LAZY) {
                                try {
                                    model.submit(secret, confirmed)
                                } finally {
                                    request = null
                                }
                            }
                        request = job
                        job.start()
                    }
                }

                ResetPasswordEvent.Back -> {
                    request?.cancel()
                    request = null
                    secret.clear()
                    confirmed.clear()
                    focus.clearFocus()
                    model.backToSignIn()
                }
            }
        },
        takeAnnouncement = {
            if (state == ResetPasswordModel.State.Done && !announcedDone) {
                announcedDone = true
                resources.getString(R.string.auth_password_updated_sign_in_with_your_n_12f248)
            } else {
                model.form.takeAnnouncement { it.resolve(resources) }
            }
        },
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    passwordState: TextFieldState,
    confirmationState: TextFieldState,
    onEvent: (ResetPasswordEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val done = state.state == ResetPasswordModel.State.Done
    val enabled = !state.form.busy
    val back = { onEvent(ResetPasswordEvent.Back) }
    BackHandler(onBack = back)
    AuthScreen(
        scrollTag = "reset.scroll",
        title = stringResource(if (done) R.string.auth_password_saved else R.string.auth_choose_a_new_password),
        description =
            stringResource(
                if (done) {
                    R.string.auth_password_updated_sign_in_with_your_n_12f248
                } else {
                    R.string.auth_sign_in_with_your_new_password_once_28729e
                },
            ),
        onBack = back,
        modifier = modifier,
        showsBack = false,
        headingGlyph = if (done) AuthHeadingGlyph.Check else null,
        enabled = enabled,
        announcementRevision = state.form.announcementRevision + if (done) 1 else 0,
        takeAnnouncement = takeAnnouncement,
    ) {
        if (done) {
            Text(
                stringResource(R.string.auth_other_devices_are_signed_out_if_you_49c60c),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.authHint),
            )
            CfButton(
                stringResource(R.string.auth_continue),
                back,
                Modifier.testTag("reset.continue"),
                size = CfButtonSize.Lg,
                fillWidth = true,
                minHeight = AuthTokens.footerHeight.dp,
            )
        } else {
            val resources = LocalResources.current
            val passwordFocus = remember { FocusRequester() }
            val confirmationFocus = remember { FocusRequester() }
            val banner = remember { BringIntoViewRequester() }
            val coordinates = LocalAuthCoordinates.current
            var focused by remember { mutableStateOf<String?>(null) }
            AuthFailureFeedback(
                state.form,
                listOf("password" to passwordFocus, "password_confirmation" to confirmationFocus),
                banner,
            )
            AuthKeyboardReveal(focused, state.form.announcementRevision)
            state.form.banner?.let { AuthBanner(it, Modifier.bringIntoViewRequester(banner), AuthFormContext.Reset) }
            CfSecureTextField(
                passwordState,
                stringResource(R.string.auth_new_password),
                Modifier
                    .focusRequester(passwordFocus)
                    .onFocusChanged {
                        if (it.isFocused) {
                            focused = "password"
                        } else if (focused == "password") {
                            focused = null
                        }
                    }.onPlaced { coordinates.fields["password"] = it },
                testTag = "reset.password",
                hint = AuthPasswordPolicy.hint(),
                error =
                    state.form.fields["password"]
                        ?.let(AuthFieldCopy::field)
                        ?.resolve(resources),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                onKeyboardAction = { if (enabled) confirmationFocus.requestFocus() },
                contentType = ContentType.NewPassword,
                inputTransformation =
                    remember(
                        onEvent,
                    ) { UserEditReporter { onEvent(ResetPasswordEvent.EditPassword) } },
            )
            CfSecureTextField(
                confirmationState,
                stringResource(R.string.auth_confirm_new_password),
                Modifier
                    .focusRequester(confirmationFocus)
                    .onFocusChanged {
                        if (it.isFocused) {
                            focused = "confirmation"
                        } else if (focused == "confirmation") {
                            focused = null
                        }
                    }.onPlaced { coordinates.fields["confirmation"] = it },
                testTag = "reset.confirmation",
                error =
                    state.form.fields["password_confirmation"]
                        ?.let(AuthFieldCopy::field)
                        ?.resolve(resources),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                onKeyboardAction = { if (enabled) onEvent(ResetPasswordEvent.Submit) },
                contentType = ContentType.NewPassword,
                inputTransformation =
                    remember(onEvent) {
                        UserEditReporter { onEvent(ResetPasswordEvent.EditConfirmation) }
                    },
            )
            val label = stringResource(R.string.auth_save_new_password)
            CfButton(
                label,
                { onEvent(ResetPasswordEvent.Submit) },
                Modifier.testTag("reset.submit").onPlaced { coordinates.submit = it },
                size = CfButtonSize.Lg,
                fillWidth = true,
                enabled = enabled,
                loading = state.form.busy,
                loadingDescription = label,
            )
        }
    }
}
