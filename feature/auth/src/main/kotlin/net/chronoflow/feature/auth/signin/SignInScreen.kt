package net.chronoflow.feature.auth.signin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
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
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormModel.Action
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.AuthLink
import net.chronoflow.feature.auth.shell.AuthLinkRow
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.TextFieldSecret
import net.chronoflow.feature.auth.shell.UserEditReporter
import net.chronoflow.feature.auth.shell.resolve

@Composable
fun SignInScreen(
    vm: SignInViewModel,
    router: SignInRouter,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val email = remember(vm) { TextFieldState(vm.email) }
    val password = remember { TextFieldState() }
    val secret = remember(password) { TextFieldSecret(password) }
    val autofill = LocalAutofillManager.current
    val resources = LocalResources.current
    DisposableEffect(vm, secret) {
        onDispose {
            vm.leave()
            secret.clear()
        }
    }
    BackHandler { router.back() }
    SignInContent(state, email, password, onEvent = { event ->
        when (event) {
            is SignInEvent.EditEmail -> vm.editEmail(event.value)
            SignInEvent.EditPassword -> vm.editPassword()
            SignInEvent.Password -> vm.submitPassword(secret) { autofill?.commit() }
            SignInEvent.Passkey -> vm.submitPasskey(secret)
            SignInEvent.Back -> router.back()
            SignInEvent.Forgot -> router.forgot()
            SignInEvent.SignUp -> router.signUp()
            SignInEvent.Guest -> router.guest()
        }
    }, takeAnnouncement = { vm.takeAnnouncement { it.resolve(resources) } })
}

@Composable
fun SignInContent(
    state: SignInUiState,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    onEvent: (SignInEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val enabled = !state.form.busy
    AuthScreen(
        scrollTag = "sign-in.scroll",
        title = stringResource(R.string.auth_welcome_back),
        description = stringResource(R.string.auth_sign_in_to_see_your_trips_and_plans),
        onBack = { onEvent(SignInEvent.Back) },
        modifier = modifier,
        enabled = enabled,
        announcementRevision = state.form.announcementRevision,
        takeAnnouncement = takeAnnouncement,
        footer = {
            CfButton(
                stringResource(R.string.auth_new_here_plan_a_trip_first),
                {
                    onEvent(SignInEvent.Guest)
                },
                Modifier.testTag(
                    "sign-in.guest",
                ),
                style = CfButtonStyle.OnSky,
                fillWidth = true,
                minHeight = AuthTokens.footerHeight.dp,
                enabled = enabled,
                textStyle = CfTheme.type.authLabel,
            )
        },
    ) {
        val resources = LocalResources.current
        val emailFocus = remember { FocusRequester() }
        val passwordFocus = remember { FocusRequester() }
        val banner = remember { BringIntoViewRequester() }
        var focused by remember { mutableStateOf<String?>(null) }
        val coordinates = LocalAuthCoordinates.current
        AuthFailureFeedback(state.form, listOf("email" to emailFocus, "password" to passwordFocus), banner)
        AuthKeyboardReveal(focused, state.form.announcementRevision)
        state.form.banner?.let { AuthBanner(it, Modifier.bringIntoViewRequester(banner)) }
        if (state.passkeyVisible) {
            val label = stringResource(R.string.auth_sign_in_with_a_passkey)
            CfButton(
                label,
                {
                    onEvent(SignInEvent.Passkey)
                },
                Modifier.testTag("sign-in.passkey"),
                size = CfButtonSize.Lg,
                fillWidth = true,
                leadingIcon = LucideGlyphs.KeyRound.imageVector(),
                enabled = enabled,
                loading =
                    state.form.pending == Action.Passkey,
                loadingDescription = label,
            )
            SignInDivider()
        }
        CfTextField(
            emailState,
            stringResource(R.string.auth_email),
            Modifier
                .focusRequester(emailFocus)
                .onFocusChanged {
                    if (it.isFocused) {
                        focused =
                            "email"
                    } else if (focused ==
                        "email"
                    ) {
                        focused = null
                    }
                }.onPlaced { coordinates.fields["email"] = it },
            testTag = "sign-in.email",
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
                    imeAction = ImeAction.Next,
                ),
            onKeyboardAction = { if (enabled) passwordFocus.requestFocus() },
            contentType =
                ContentType.Username + ContentType.EmailAddress,
            inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignInEvent.EditEmail(it)) } },
        )
        CfSecureTextField(
            passwordState,
            stringResource(R.string.auth_password),
            Modifier
                .focusRequester(passwordFocus)
                .onFocusChanged {
                    if (it.isFocused) {
                        focused =
                            "password"
                    } else if (focused ==
                        "password"
                    ) {
                        focused = null
                    }
                }.onPlaced { coordinates.fields["password"] = it },
            testTag = "sign-in.password",
            error =
                state.form.fields["password"]
                    ?.let(AuthFieldCopy::field)
                    ?.resolve(resources),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            onKeyboardAction = { onEvent(SignInEvent.Password) },
            contentType = ContentType.Password,
            inputTransformation = remember(onEvent) { UserEditReporter { onEvent(SignInEvent.EditPassword) } },
        )
        val signIn = stringResource(R.string.auth_sign_in)
        CfButton(
            signIn,
            { onEvent(SignInEvent.Password) },
            Modifier.testTag("sign-in.submit").onPlaced {
                coordinates.submit =
                    it
            },
            style = CfButtonStyle.Outline,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = enabled,
            loading =
                state.form.pending == Action.Password,
            loadingDescription = signIn,
        )
        AuthLinkRow(forgot = {
            AuthLink(stringResource(R.string.auth_forgot_your_password), {
                onEvent(SignInEvent.Forgot)
            }, Modifier.testTag("sign-in.forgot"), enabled = enabled)
        }, signUp = {
            AuthLink(stringResource(R.string.auth_create_an_account), {
                onEvent(SignInEvent.SignUp)
            }, Modifier.testTag("sign-in.signup"), strong = true, enabled = enabled)
        })
    }
}

@Composable
private fun SignInDivider() {
    Row(
        Modifier.fillMaxWidth().testTag("sign-in.divider").clearAndSetSemantics {
        },
        horizontalArrangement =
            Arrangement.spacedBy(
                AuthTokens.inset.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(AuthTokens.borderWidth.dp).background(CfTheme.colors.border))
        Text(
            stringResource(R.string.auth_or_with_email),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.authMeta),
        )
        Box(Modifier.weight(1f).height(AuthTokens.borderWidth.dp).background(CfTheme.colors.border))
    }
}
