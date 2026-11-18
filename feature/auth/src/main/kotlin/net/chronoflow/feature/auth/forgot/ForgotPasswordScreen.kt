package net.chronoflow.feature.auth.forgot

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.ForgotPasswordModel
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthHeadingGlyph
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.UserEditReporter
import net.chronoflow.feature.auth.shell.resolve

data class ForgotPasswordUiState(
    val form: AuthFormState = AuthFormState(),
    val sentEmail: String? = null,
    val expired: Boolean = false,
)

sealed interface ForgotPasswordEvent {
    data class EditEmail(
        val value: String,
    ) : ForgotPasswordEvent

    data object Submit : ForgotPasswordEvent

    data object OpenEmail : ForgotPasswordEvent

    data object Back : ForgotPasswordEvent
}

@Composable
fun ForgotPasswordRoute(
    model: ForgotPasswordModel,
    expired: Boolean,
    openEmailApp: () -> Boolean,
) {
    val form by model.form.state.collectAsStateWithLifecycle()
    val sent by model.sentEmail.collectAsStateWithLifecycle()
    val email = remember(model) { TextFieldState(model.email.value) }
    val scope = rememberCoroutineScope()
    var request by remember(model) { mutableStateOf<Job?>(null) }
    var announcedSent by remember(model) { mutableStateOf(model.sentEmail.value != null) }
    val resources = LocalResources.current
    val focus = LocalFocusManager.current
    LaunchedEffect(sent) { if (sent != null) focus.clearFocus() }
    ForgotPasswordScreen(
        ForgotPasswordUiState(form, sent, expired),
        email,
        onEvent = { event ->
            when (event) {
                is ForgotPasswordEvent.EditEmail -> {
                    model.editEmail(event.value)
                }

                ForgotPasswordEvent.Submit -> {
                    if (request == null && !model.form.busy) {
                        val job =
                            scope.launch(start = CoroutineStart.LAZY) {
                                try {
                                    model.submit()
                                } finally {
                                    request = null
                                }
                            }
                        request = job
                        job.start()
                    }
                }

                ForgotPasswordEvent.OpenEmail -> {
                    scope.launch { model.openEmail(openEmailApp) }
                }

                ForgotPasswordEvent.Back -> {
                    request?.cancel()
                    request = null
                    focus.clearFocus()
                    model.backToSignIn()
                }
            }
        },
        takeAnnouncement = {
            val recipient = sent
            if (recipient != null && !announcedSent) {
                announcedSent = true
                resources.getString(R.string.auth_check_your_inbox_78cde4) + " " +
                    resources.getString(R.string.auth_if_x_has_an_account_a_link_to_choose_484150, recipient)
            } else {
                model.form.takeAnnouncement { it.resolve(resources) }
            }
        },
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    emailState: TextFieldState,
    onEvent: (ForgotPasswordEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val enabled = !state.form.busy
    val expired = state.expired && state.sentEmail == null
    val back = { onEvent(ForgotPasswordEvent.Back) }
    BackHandler(onBack = back)
    AuthScreen(
        scrollTag = "forgot.scroll",
        title = stringResource(if (expired) R.string.auth_this_link_has_expired else R.string.auth_reset_your_password),
        description =
            stringResource(
                if (expired) {
                    R.string.auth_reset_links_work_once_and_expire_aft_059606
                } else {
                    R.string.auth_enter_your_email_and_we_ll_send_you_bf365b
                },
            ),
        onBack = back,
        modifier = modifier,
        headingGlyph = if (expired) AuthHeadingGlyph.Problem else null,
        enabled = enabled,
        announcementRevision = state.form.announcementRevision + if (state.sentEmail != null) 1 else 0,
        takeAnnouncement = takeAnnouncement,
    ) {
        if (state.sentEmail != null) {
            CfNotice(
                stringResource(R.string.auth_check_your_inbox_78cde4),
                stringResource(R.string.auth_if_x_has_an_account_a_link_to_choose_484150, state.sentEmail),
                Modifier.testTag("forgot.notice"),
            )
            CfButton(stringResource(R.string.auth_open_email_app), {
                onEvent(ForgotPasswordEvent.OpenEmail)
            }, Modifier.testTag("forgot.open-email"), size = CfButtonSize.Lg, fillWidth = true, enabled = enabled)
        } else {
            val resources = LocalResources.current
            val emailFocus = remember { FocusRequester() }
            val banner = remember { BringIntoViewRequester() }
            var focused by remember { mutableStateOf(false) }
            val coordinates = LocalAuthCoordinates.current
            AuthFailureFeedback(state.form, listOf("email" to emailFocus), banner)
            AuthKeyboardReveal(if (focused) "email" else null, state.form.announcementRevision)
            state.form.banner?.let { AuthBanner(it, Modifier.bringIntoViewRequester(banner), AuthFormContext.Forgot) }
            CfTextField(
                emailState,
                stringResource(R.string.auth_email),
                Modifier.focusRequester(emailFocus).onFocusChanged { focused = it.isFocused }.onPlaced {
                    coordinates.fields["email"] =
                        it
                },
                testTag = "forgot.email",
                error =
                    state.form.fields["email"]
                        ?.let(AuthFieldCopy::field)
                        ?.resolve(resources),
                enabled = enabled,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Send,
                    ),
                onKeyboardAction = { if (enabled) onEvent(ForgotPasswordEvent.Submit) },
                contentType = ContentType.EmailAddress,
                inputTransformation =
                    remember(
                        onEvent,
                    ) { UserEditReporter { onEvent(ForgotPasswordEvent.EditEmail(it)) } },
            )
            val label = stringResource(if (expired) R.string.auth_send_a_new_link else R.string.auth_send_reset_link)
            CfButton(
                label,
                { onEvent(ForgotPasswordEvent.Submit) },
                Modifier
                    .testTag("forgot.submit")
                    .onPlaced {
                        coordinates.submit = it
                    }.then(if (expired) Modifier.heightIn(min = AuthTokens.footerHeight.dp) else Modifier),
                size = CfButtonSize.Lg,
                fillWidth = true,
                enabled = enabled,
                loading = state.form.busy,
                loadingDescription = label,
            )
        }
        if (state.sentEmail != null || expired) {
            CfButton(
                stringResource(R.string.auth_back_to_sign_in),
                back,
                Modifier.testTag("forgot.back"),
                style = CfButtonStyle.Ghost,
                size = CfButtonSize.Lg,
                fillWidth = true,
                enabled = enabled,
            )
        }
    }
}
