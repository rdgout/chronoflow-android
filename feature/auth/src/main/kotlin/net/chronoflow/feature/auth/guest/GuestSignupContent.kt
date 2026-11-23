package net.chronoflow.feature.auth.guest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import net.chronoflow.feature.auth.model.AuthFieldCopy
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthText
import net.chronoflow.feature.auth.model.GuestSignupState
import net.chronoflow.feature.auth.shell.AuthAnnouncer
import net.chronoflow.feature.auth.shell.AuthCoordinates
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthKeyboardReveal
import net.chronoflow.feature.auth.shell.LocalAuthCoordinates
import net.chronoflow.feature.auth.shell.UserEditReporter
import net.chronoflow.feature.auth.shell.resolve
import net.chronoflow.feature.auth.signup.AuthConsent
import net.chronoflow.feature.auth.signup.PolicyOpener

@Composable
fun GuestSignupContent(
    state: GuestSignupUiState,
    fields: GuestSignupFields,
    policyOpener: PolicyOpener,
    onEvent: (GuestSignupEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val coordinates = remember { AuthCoordinates() }
    val banner = remember { BringIntoViewRequester() }
    val nameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val confirmationFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val resources = LocalResources.current
    var focused by remember { mutableStateOf<String?>(null) }
    val enabled = !state.form.busy
    val guest = state.guestState
    LaunchedEffect(state.name) {
        if (fields.name.text.toString() != state.name) fields.name.setTextAndPlaceCursorAtEnd(state.name)
    }
    LaunchedEffect(state.email) {
        if (fields.email.text.toString() != state.email) fields.email.setTextAndPlaceCursorAtEnd(state.email)
    }
    LaunchedEffect(guest) {
        if (guest != GuestSignupState.Ready) {
            focus.clearFocus()
            keyboard?.hide()
            withFrameNanos { }
            banner.bringIntoView()
        }
    }

    fun fieldModifier(
        field: String,
        requester: FocusRequester,
    ): Modifier =
        Modifier
            .focusRequester(requester)
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

    fun submit() {
        if (enabled && guest != GuestSignupState.Review) {
            focus.clearFocus()
            onEvent(GuestSignupEvent.Submit)
        }
    }

    CompositionLocalProvider(LocalAuthCoordinates provides coordinates) {
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
        Box(
            modifier
                .fillMaxWidth()
                .imePadding()
                .windowInsetsPadding(
                    LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ).onPlaced { coordinates.viewport = it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .onPlaced { coordinates.content = it }
                    .bringIntoViewRequester(coordinates.requester)
                    .testTag("guest-signup.scroll"),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                        .widthIn(max = PlanUITokens.wizardColumnWidth.dp)
                        .padding(horizontal = PlanUITokens.space5.dp)
                        .padding(bottom = PlanUITokens.space8.dp),
                    verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                        Text(
                            stringResource(R.string.auth_save_your_plan),
                            Modifier.testTag("guest-signup.title").semantics { heading() },
                            color = CfTheme.colors.foreground,
                            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
                        )
                        Text(
                            stringResource(R.string.auth_create_an_account_to_save_it_and_ope_a14c8c),
                            color = CfTheme.colors.mutedForeground,
                            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                        )
                    }
                    val reviewLabel =
                        if (guest == GuestSignupState.Review) {
                            stringResource(R.string.auth_review_my_trip)
                        } else {
                            null
                        }
                    guest.titleKey?.let { title ->
                        CfBanner(
                            AuthText(title).resolve(resources),
                            guest.messageKey?.let { AuthText(it).resolve(resources) },
                            Modifier.bringIntoViewRequester(banner),
                            actionLabel = reviewLabel,
                            onAction =
                                if (guest ==
                                    GuestSignupState.Review
                                ) {
                                    ({ onEvent(GuestSignupEvent.Review) })
                                } else {
                                    null
                                },
                            announce = false,
                            testTag = "auth.banner",
                            actionTestTag = "auth.banner.action",
                            actionEnabled = enabled,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                        CfTextField(
                            fields.name,
                            stringResource(R.string.auth_name),
                            fieldModifier(
                                "name",
                                nameFocus,
                            ),
                            testTag = "guest-signup.name",
                            error = error("name"),
                            enabled = enabled,
                            keyboardOptions =
                                KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next,
                                ),
                            onKeyboardAction = { if (enabled) emailFocus.requestFocus() },
                            contentType = ContentType.PersonFullName,
                            inputTransformation =
                                remember(
                                    onEvent,
                                ) { UserEditReporter { onEvent(GuestSignupEvent.EditName(it)) } },
                        )
                        CfTextField(
                            fields.email,
                            stringResource(R.string.auth_email),
                            fieldModifier(
                                "email",
                                emailFocus,
                            ),
                            testTag = "guest-signup.email",
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
                            inputTransformation =
                                remember(
                                    onEvent,
                                ) { UserEditReporter { onEvent(GuestSignupEvent.EditEmail(it)) } },
                        )
                        CfSecureTextField(
                            fields.password,
                            stringResource(R.string.auth_password),
                            fieldModifier(
                                "password",
                                passwordFocus,
                            ),
                            testTag = "guest-signup.password",
                            revealed = false,
                            hint = AuthPasswordPolicy.hint(),
                            error = error("password"),
                            enabled = enabled,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next,
                                ),
                            onKeyboardAction = { if (enabled) confirmationFocus.requestFocus() },
                            contentType = ContentType.NewPassword,
                            inputTransformation =
                                remember(
                                    onEvent,
                                ) { UserEditReporter { onEvent(GuestSignupEvent.EditPassword) } },
                        )
                        CfSecureTextField(
                            fields.confirmation,
                            stringResource(R.string.auth_confirm_password),
                            fieldModifier(
                                "password_confirmation",
                                confirmationFocus,
                            ),
                            testTag = "guest-signup.confirmation",
                            revealed = false,
                            error = error("password_confirmation"),
                            enabled = enabled,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Go,
                                ),
                            onKeyboardAction = { submit() },
                            contentType = ContentType.NewPassword,
                            inputTransformation =
                                remember(
                                    onEvent,
                                ) { UserEditReporter { onEvent(GuestSignupEvent.EditConfirmation) } },
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                        val label = stringResource(R.string.auth_create_account_and_save)
                        CfButton(
                            label,
                            { submit() },
                            Modifier.testTag("guest-signup.submit").onPlaced { coordinates.submit = it },
                            size = CfButtonSize.Lg,
                            fillWidth = true,
                            enabled = enabled && guest != GuestSignupState.Review,
                            loading = state.form.pending == AuthFormModel.Action.Register,
                            loadingDescription = label,
                        )
                        CfButton(
                            stringResource(R.string.auth_i_already_have_an_account),
                            { onEvent(GuestSignupEvent.SignInInstead) },
                            Modifier.testTag("guest-signup.sign-in"),
                            style = CfButtonStyle.Ghost,
                            size = CfButtonSize.Lg,
                            fillWidth = true,
                            enabled = enabled,
                        )
                    }
                    AuthConsent(
                        policyOpener,
                        enabled,
                        textResource = GuestSignupCopy.consentResource,
                        testTag = "guest-signup.consent",
                    )
                }
            }
        }
        AuthAnnouncer(
            state.form.announcementRevision * GuestSignupState.entries.size + guest.ordinal,
            take = {
                if (guest == GuestSignupState.Ready) {
                    takeAnnouncement()
                } else {
                    guest.titleKey?.let {
                        val title = AuthText(it).resolve(resources)
                        val message = AuthText(checkNotNull(guest.messageKey)).resolve(resources)
                        "$title $message"
                    }
                }
            },
        )
    }
}
