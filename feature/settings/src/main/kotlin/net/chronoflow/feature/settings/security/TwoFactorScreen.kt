package net.chronoflow.feature.settings.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfRow
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.secure.SecureWindow
import net.chronoflow.feature.settings.components.SettingsEyebrow
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.SettingsStoredState
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPrompt
import net.chronoflow.feature.settings.security.prompt.SettingsPasswordPromptHost
import java.time.Instant

class TwoFactorInputs {
    val password = TextFieldState()
    val code = TextFieldState()

    override fun toString(): String = "TwoFactorInputs(***)"
}

data class TwoFactorScreenState(
    val ui: SecurityUiState,
    val inputs: TwoFactorInputs,
    val now: () -> Instant,
    val email: String = "",
    val qr: QrMatrix? = null,
)

sealed interface TwoFactorEvent {
    data object Back : TwoFactorEvent

    data object Retry : TwoFactorEvent

    data class Prompt(
        val action: SettingsSecurityPrompt,
    ) : TwoFactorEvent

    data object CancelPrompt : TwoFactorEvent

    class SubmitPrompt(
        val password: SecretField,
    ) : TwoFactorEvent

    class Confirm(
        val code: SecretField,
    ) : TwoFactorEvent

    data object Reset : TwoFactorEvent

    data object CopySecret : TwoFactorEvent

    data object CopyCodes : TwoFactorEvent

    data object Download : TwoFactorEvent
}

@Composable
fun TwoFactorScreen(
    state: TwoFactorScreenState,
    onEvent: (TwoFactorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val security = state.ui.security
    val enabled = state.ui.summary?.twoFactorEnabled == true || security.codes.isNotEmpty()
    SecureWindow(security.setup != null || security.codes.isNotEmpty())
    SettingsPage(
        R.string.settings_two_factor_authentication,
        { onEvent(TwoFactorEvent.Back) },
        "settings.twoFactor.screen",
        modifier,
        spacing = PlanUITokens.space5.dp,
    ) {
        if (state.ui.summary == null) {
            SettingsStoredState(
                state.ui.pages.settingsLoaded,
                state.ui.pages.failed,
                { onEvent(TwoFactorEvent.Retry) },
            )
        } else if (security.setup != null) {
            TwoFactorSetup(state, onEvent)
        } else if (enabled) {
            TwoFactorManage(state, onEvent)
        } else {
            TwoFactorIntroduction { onEvent(TwoFactorEvent.Prompt(SettingsSecurityPrompt.Enable)) }
        }
        SettingsSecurityLock(security.retryAt, state.now)
    }
    SettingsPasswordPromptHost(
        security.prompt != null,
        security.busy,
        { onEvent(TwoFactorEvent.CancelPrompt) },
    ) {
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
                { onEvent(TwoFactorEvent.CancelPrompt) },
                { onEvent(TwoFactorEvent.SubmitPrompt(TextFieldSecret(state.inputs.password))) },
                { onEvent(TwoFactorEvent.Reset) },
                errorArgs = security.errorArguments["current_password"].orEmpty(),
            )
        }
    }
}

@Composable
private fun ColumnScope.TwoFactorIntroduction(onEnable: () -> Unit) {
    CfBadge(securityText("Off"))
    CfCard(spacing = PlanUITokens.radiusLg.dp) {
        Text(
            securityText(
                "Ask for a code from an authenticator app each time you sign in with your password, " +
                    "so a stolen password isn't enough.",
            ),
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
        )
        Column(verticalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp)) {
            IntroBullet("You need an authenticator app, like the one built into Passwords on iPhone.")
            IntroBullet("You get recovery codes for when you lose your phone.")
            IntroBullet("Signing in with a passkey doesn't ask for a code.")
        }
        CfButton(
            securityText("Turn on two-factor"),
            onEnable,
            Modifier.testTag("settings.twoFactor.enable"),
            fillWidth = true,
            minHeight = PlanUITokens.actionMinHeight.dp,
        )
        LockHint("You'll be asked for your password first.", centered = true)
    }
}

@Composable
private fun IntroBullet(key: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp)) {
        Box(
            Modifier
                .padding(top = PlanUITokens.space2.dp)
                .size(AuthTokens.borderWidth.dp * PlanUITokens.space1)
                .background(CfTheme.colors.mutedForeground, CfTheme.shapes.full),
        )
        Text(securityText(key), style = CfTheme.type.screenBody, color = CfTheme.colors.mutedForeground)
    }
}

@Composable
private fun ColumnScope.TwoFactorSetup(
    state: TwoFactorScreenState,
    onEvent: (TwoFactorEvent) -> Unit,
) {
    val security = state.ui.security
    val setup = checkNotNull(security.setup)
    val keyboard = LocalSoftwareKeyboardController.current
    Text(
        securityText("When it's on, you sign in with your password and a code from an authenticator app."),
        style = CfTheme.type.style(CfTypeScale.screenBodyLg),
        color = CfTheme.colors.mutedForeground,
    )
    CfCard {
        SetupStep("1 · Scan this code with your authenticator app", '1')
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) { SettingsAuthenticatorQr(state.qr) }
        Text(
            securityText("Using this phone? Copy the setup key into the app instead."),
            style = CfTheme.type.screenBody,
            color = CfTheme.colors.mutedForeground,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    CfTheme.colors.surface2,
                    RoundedCornerShape(PlanUITokens.radiusLg.dp),
                ).padding(SettingsTokens.pillGap.dp)
                .padding(start = PlanUITokens.space2.dp),
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(
                    setup.secretKey,
                    Modifier.testTag("settings.twoFactor.secret"),
                    style = CfTheme.type.style(CfTypeScale.settingsCodes).copy(fontFamily = FontFamily.Monospace),
                )
            }
            CfButton(
                securityText("Copy"),
                { onEvent(TwoFactorEvent.CopySecret) },
                Modifier.testTag("settings.twoFactor.copySecret"),
                style = CfButtonStyle.Outline,
                minHeight = PlanUITokens.walkthroughDoneHeight.dp,
                leadingIcon = LucideGlyphs.Copy.imageVector(),
            )
        }
    }
    CfCard {
        SetupStep("2 · Enter the 6-digit code it shows", '2')
        CfTextField(
            state.inputs.code,
            securityText("6-digit code"),
            testTag = "settings.twoFactor.code",
            placeholder = securityText("123456"),
            error = security.errors["code"]?.let { securityText(it) },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
            onKeyboardAction = { keyboard?.hide() },
            contentType = ContentType.SmsOtpCode,
            inputTransformation = InputTransformation.maxLength(6),
        )
    }
    if (security.twoFactorActivationFailed) {
        CfBanner(
            securityText("Couldn't turn on two-factor."),
            securityText("Nothing changed on your account. Try again in a moment."),
            testTag = "settings.twoFactor.failure",
        )
    }
    CfButton(
        securityText(if (security.twoFactorActivationFailed) "Try again" else "Turn on two-factor"),
        {
            keyboard?.hide()
            onEvent(TwoFactorEvent.Confirm(TextFieldSecret(state.inputs.code)))
        },
        Modifier.testTag("settings.twoFactor.confirm"),
        enabled = !security.busy && security.retryAt == null,
        loading = security.busy,
        loadingDescription = securityText("Checking…"),
        fillWidth = true,
        minHeight = PlanUITokens.actionMinHeight.dp,
    )
}

@Composable
private fun SetupStep(
    key: String,
    digit: Char,
) {
    val text = securityText(key)
    val color = CfTheme.colors.dawnText
    Text(
        buildAnnotatedString {
            append(text)
            val index = text.indexOf(digit)
            if (index >= 0) addStyle(SpanStyle(color = color), index, index + 1)
        },
        style = CfTheme.type.screenLabel,
    )
}

@Composable
private fun ColumnScope.TwoFactorManage(
    state: TwoFactorScreenState,
    onEvent: (TwoFactorEvent) -> Unit,
) {
    val codes = state.ui.security.codes
    Row(
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CfBadge(securityText("On"), variant = CfBadgeVariant.Dawn)
        if (state.ui.since != null) {
            Text(
                securityText(
                    "Since {{date}}",
                    mapOf("date" to state.ui.since),
                ),
                Modifier.testTag("settings.twoFactor.since"),
                style = CfTheme.type.screenBody.copy(fontFeatureSettings = "tnum"),
                color = CfTheme.colors.mutedForeground,
            )
        }
    }
    if (codes.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            SettingsEyebrow(
                securityText("Recovery codes"),
                Modifier.padding(horizontal = PlanUITokens.space1.dp),
            )
            CfCard(rows = true) {
                PromptRow(
                    SettingsSecurityPrompt.Reveal,
                    "Show recovery codes",
                    "Each code works once and is then replaced",
                    onEvent,
                )
            }
        }
    } else {
        RecoveryCodes(codes, onEvent)
    }
    CfCard(rows = true) {
        PromptRow(
            SettingsSecurityPrompt.Regenerate,
            "Make new codes",
            "Your current codes stop working",
            onEvent,
        )
        CfDivider()
        PromptRow(
            SettingsSecurityPrompt.Disable,
            "Turn off two-factor",
            "Sign in with only your password or passkey",
            onEvent,
        )
    }
    LockHint("Each of these asks for your password first.")
}

@Composable
private fun RecoveryCodes(
    codes: List<String>,
    onEvent: (TwoFactorEvent) -> Unit,
) {
    val title = securityText("Recovery codes")
    CfCard {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
            Text(
                title,
                Modifier.semantics { heading() },
                style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
            )
            Text(
                securityText(
                    "If you lose your phone, sign in with one of these. Each code works once and is then replaced. " +
                        "Keep them somewhere safe.",
                ),
                style = CfTheme.type.screenBody,
                color = CfTheme.colors.mutedForeground,
            )
        }
        SelectionContainer {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        CfTheme.colors.surface2,
                        RoundedCornerShape(PlanUITokens.radiusLg.dp),
                    ).padding(PlanUITokens.space3.dp)
                    .semantics {
                        contentDescription = title
                        collectionInfo = CollectionInfo((codes.size + 1) / 2, 2)
                    },
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            ) {
                codes.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                        row.forEach { code ->
                            Text(
                                code,
                                Modifier.weight(1f).testTag("settings.twoFactor.recoveryCode"),
                                textAlign = TextAlign.Center,
                                style =
                                    CfTheme.type
                                        .style(
                                            CfTypeScale.settingsCodes,
                                        ).copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            CfButton(
                securityText("Copy"),
                { onEvent(TwoFactorEvent.CopyCodes) },
                Modifier.weight(1f).testTag("settings.twoFactor.copyCodes"),
                style = CfButtonStyle.Outline,
                minHeight = PlanUITokens.actionMinHeight.dp,
                fillWidth = true,
            )
            CfButton(
                securityText("Download"),
                { onEvent(TwoFactorEvent.Download) },
                Modifier.weight(1f).testTag("settings.twoFactor.download"),
                style = CfButtonStyle.Outline,
                minHeight = PlanUITokens.actionMinHeight.dp,
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun PromptRow(
    action: SettingsSecurityPrompt,
    title: String,
    subtitle: String,
    onEvent: (TwoFactorEvent) -> Unit,
) {
    CfRow(
        securityText(title),
        subtitle = securityText(subtitle),
        lock = true,
        destructive = action.destructive,
        testTag = "settings.twoFactor.${action.name.lowercase(java.util.Locale.ROOT)}",
        onClick = { onEvent(TwoFactorEvent.Prompt(action)) },
    )
}

@Composable
private fun LockHint(
    key: String,
    centered: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PlanUITokens.space1.dp),
        horizontalArrangement =
            Arrangement.spacedBy(
                SettingsTokens.pillGap.dp,
                if (centered) Alignment.CenterHorizontally else Alignment.Start,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            LucideGlyphs.Lock.imageVector(),
            null,
            Modifier.size(AuthTokens.icon.dp),
            tint = CfTheme.colors.mutedForeground,
        )
        Text(
            securityText(key),
            style = CfTheme.type.style(CfTypeScale.settingsHint),
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            color = CfTheme.colors.mutedForeground,
        )
    }
}

@Composable
fun TwoFactorRoute(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val qr by viewModel.qr.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val inputs = remember { TwoFactorInputs() }
    val language =
        if (LocalResources.current.configuration.locales[0]
                .language == "nl"
        ) {
            Language.Nl
        } else {
            Language.En
        }
    val download = rememberRecoveryCodesDownload(ui.security.codes, viewModel::download)
    LaunchedEffect(viewModel) { viewModel.startTwoFactor() }
    LaunchedEffect(viewModel, language) { viewModel.setLanguage(language) }
    LaunchedEffect(ui.security.prompt) { inputs.password.clearText() }
    SecurityFeedbackHost(viewModel.ui, viewModel::feedbackPresented, viewModel::clearSecrets, modifier) {
        TwoFactorScreen(TwoFactorScreenState(ui, inputs, viewModel.now, email, qr), { event ->
            when (event) {
                TwoFactorEvent.Back -> onBack()
                TwoFactorEvent.Retry -> viewModel.retry()
                is TwoFactorEvent.Prompt -> viewModel.showPrompt(event.action)
                TwoFactorEvent.CancelPrompt -> viewModel.cancelPrompt()
                is TwoFactorEvent.SubmitPrompt -> viewModel.submitPrompt(event.password)
                is TwoFactorEvent.Confirm -> viewModel.confirm(event.code)
                TwoFactorEvent.Reset -> viewModel.sendReset()
                TwoFactorEvent.CopySecret -> viewModel.copySecret()
                TwoFactorEvent.CopyCodes -> viewModel.copyCodes()
                TwoFactorEvent.Download -> download()
            }
        })
    }
}
