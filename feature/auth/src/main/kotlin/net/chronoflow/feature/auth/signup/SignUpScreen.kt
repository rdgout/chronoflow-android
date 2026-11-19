package net.chronoflow.feature.auth.signup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.AuthMetrics
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.TextFieldSecret
import net.chronoflow.feature.auth.shell.UserEditReporter
import net.chronoflow.feature.auth.shell.resolve

@Composable
fun SignUpRoute(
    vm: SignUpViewModel,
    policyOpener: PolicyOpener,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val name = remember(vm) { TextFieldState(vm.uiState.value.name) }
    val email = remember(vm) { TextFieldState(vm.uiState.value.email) }
    val password = remember(vm) { TextFieldState() }
    val confirmation = remember(vm) { TextFieldState() }
    val secret = remember(password) { TextFieldSecret(password) }
    val confirmed = remember(confirmation) { TextFieldSecret(confirmation) }
    val autofill = LocalAutofillManager.current
    val resources = LocalResources.current
    DisposableEffect(vm) {
        onDispose {
            vm.leave()
            secret.clear()
            confirmed.clear()
        }
    }
    SignUpScreen(state, name, email, password, confirmation, policyOpener, onEvent = { event ->
        when (event) {
            is SignUpEvent.EditName -> {
                vm.editName(event.value)
            }

            is SignUpEvent.EditEmail -> {
                vm.editEmail(event.value)
            }

            SignUpEvent.EditPassword -> {
                vm.editPassword()
            }

            SignUpEvent.EditConfirmation -> {
                vm.editConfirmation()
            }

            SignUpEvent.TogglePassword -> {
                vm.togglePassword()
            }

            SignUpEvent.Submit -> {
                vm.submit(
                    secret,
                    confirmed,
                    if (resources.configuration.locales[0].language ==
                        "nl"
                    ) {
                        AuthLanguage.Nl
                    } else {
                        AuthLanguage.En
                    },
                ) { autofill?.commit() }
            }

            SignUpEvent.Back -> {
                secret.clear()
                confirmed.clear()
                vm.backToSignIn()
            }
        }
    }, takeAnnouncement = { vm.takeAnnouncement { it.resolve(resources) } })
}

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    nameState: TextFieldState,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    confirmationState: TextFieldState,
    policyOpener: PolicyOpener,
    onEvent: (SignUpEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    BackHandler { onEvent(SignUpEvent.Back) }
    SignUpContent(
        state,
        nameState,
        emailState,
        passwordState,
        confirmationState,
        policyOpener,
        onEvent,
        modifier,
        takeAnnouncement,
    )
}

@Composable
fun SignUpContent(
    state: SignUpUiState,
    nameState: TextFieldState,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    confirmationState: TextFieldState,
    policyOpener: PolicyOpener,
    onEvent: (SignUpEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val enabled = !state.form.busy
    AuthScreen(
        scrollTag = "sign-up.scroll",
        title = stringResource(R.string.auth_create_an_account),
        description = stringResource(R.string.auth_save_your_trips_and_get_reminders_on_a0f4f9),
        onBack = { onEvent(SignUpEvent.Back) },
        modifier = modifier,
        compact = true,
        enabled = enabled,
        announcementRevision = state.form.announcementRevision,
        takeAnnouncement = takeAnnouncement,
        footer = {
            CfButton(
                stringResource(R.string.auth_already_have_an_account_sign_in),
                { onEvent(SignUpEvent.Back) },
                Modifier.testTag("sign-up.back"),
                style = CfButtonStyle.OnSky,
                fillWidth = true,
                minHeight = AuthTokens.footerHeight.dp,
                enabled = enabled,
            )
        },
    ) {
        val resources = LocalResources.current
        val nameFocus = remember { FocusRequester() }
        val emailFocus = remember { FocusRequester() }
        val passwordFocus = remember { FocusRequester() }
        val confirmationFocus = remember { FocusRequester() }
        val banner = remember { BringIntoViewRequester() }
        val coordinates = LocalAuthCoordinates.current
        var focused by remember { mutableStateOf<String?>(null) }

        fun fieldModifier(
            field: String,
            focus: FocusRequester,
        ): Modifier =
            Modifier
                .testTag("sign-up.$field.group")
                .focusRequester(focus)
                .onFocusChanged {
                    if (it.isFocused) {
                        focused = field
                    } else if (focused == field) {
                        focused = null
                    }
                }.onPlaced { coordinates.fields[field] = it }

        fun error(field: String): String? =
            state.form.fields[field]
                ?.let(AuthFieldCopy::field)
                ?.resolve(resources)
        AuthFailureFeedback(
            state.form,
            listOf(
                "name" to nameFocus,
                "email" to emailFocus,
                "password" to passwordFocus,
                "password_confirmation" to confirmationFocus,
            ),
            banner,
        )
        AuthKeyboardReveal(focused, state.form.announcementRevision)
        state.form.banner?.let { AuthBanner(it, Modifier.bringIntoViewRequester(banner), AuthFormContext.SignUp) }
        CfTextField(
            nameState,
            stringResource(R.string.auth_name),
            fieldModifier("name", nameFocus),
            testTag = "sign-up.name",
            error = error("name"),
            enabled = enabled,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            onKeyboardAction = { if (enabled) emailFocus.requestFocus() },
            contentType = ContentType.PersonFullName,
            inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignUpEvent.EditName(it)) } },
        )
        CfTextField(
            emailState,
            stringResource(R.string.auth_email),
            fieldModifier("email", emailFocus),
            testTag = "sign-up.email",
            error = error("email"),
            enabled = enabled,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            onKeyboardAction = { if (enabled) passwordFocus.requestFocus() },
            contentType = ContentType.EmailAddress + ContentType.NewUsername,
            inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignUpEvent.EditEmail(it)) } },
        )
        CfSecureTextField(
            passwordState,
            stringResource(R.string.auth_password),
            fieldModifier("password", passwordFocus),
            testTag = "sign-up.password",
            revealed = state.showsPassword,
            hint = AuthPasswordPolicy.hint(),
            error = error("password"),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            onKeyboardAction = { if (enabled) confirmationFocus.requestFocus() },
            contentType = ContentType.NewPassword,
            inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignUpEvent.EditPassword) } },
        )
        Column(verticalArrangement = Arrangement.spacedBy(AuthMetrics.CONFIRMATION_TO_TOGGLE.dp)) {
            CfSecureTextField(
                confirmationState,
                stringResource(R.string.auth_confirm_password),
                fieldModifier("confirmation", confirmationFocus),
                testTag = "sign-up.confirmation",
                revealed = state.showsPassword,
                error = error("password_confirmation"),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                onKeyboardAction = { if (enabled) onEvent(SignUpEvent.Submit) },
                contentType = ContentType.NewPassword,
                inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignUpEvent.EditConfirmation) } },
            )
            SignUpVisibility(state.showsPassword, enabled) { onEvent(SignUpEvent.TogglePassword) }
        }
        val label = stringResource(R.string.auth_create_account)
        CfButton(
            label,
            { onEvent(SignUpEvent.Submit) },
            Modifier.testTag("sign-up.submit").onPlaced { coordinates.submit = it },
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = enabled,
            loading = state.form.pending == AuthFormModel.Action.Register,
            loadingDescription = label,
        )
        AuthConsent(policyOpener, enabled)
    }
}

@Composable
private fun SignUpVisibility(
    showsPassword: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val label = stringResource(R.string.auth_show_password)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = AuthMetrics.TOUCH.dp)
            .testTag("sign-up.visibility")
            .cfToggleable(showsPassword, { onToggle() }, enabled)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(AuthMetrics.TOGGLE_GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            LucideGlyphs.Eye.imageVector(),
            null,
            Modifier.size(AuthMetrics.TOGGLE_GLYPH.dp),
            tint = CfTheme.colors.foreground,
        )
        Text(
            stringResource(if (showsPassword) R.string.auth_hide_password else R.string.auth_show_password),
            Modifier.weight(1f).clearAndSetSemantics {},
            color = CfTheme.colors.foreground,
            style = CfTheme.type.authLabel,
        )
        CfSwitch(showsPassword, null, Modifier.clearAndSetSemantics {}, enabled)
    }
}
