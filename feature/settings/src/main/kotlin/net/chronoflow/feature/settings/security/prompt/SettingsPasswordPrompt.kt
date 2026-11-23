package net.chronoflow.feature.settings.security.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfAlertDialog
import net.chronoflow.core.designsystem.system.CfDialogAction
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.settings.security.lockoutText
import net.chronoflow.feature.settings.security.remainingSeconds
import net.chronoflow.feature.settings.security.securityText
import java.time.Instant

@Composable
internal fun SettingsPasswordPrompt(
    detail: String,
    button: String,
    destructive: Boolean,
    password: TextFieldState,
    busy: Boolean,
    retryAt: Instant?,
    now: () -> Instant,
    error: String?,
    message: String?,
    email: String,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    detailArgs: Map<String, String> = emptyMap(),
    errorArgs: Map<String, String> = emptyMap(),
    identifier: String = "settings.security.prompt",
    fieldTag: String = "$identifier.password",
    submitTag: String = "$identifier.submit",
    errorTag: String = "$identifier.error",
) {
    val title = securityText("Confirm it's you")
    var showingReset by remember { mutableStateOf(false) }
    val remaining = promptRemaining(retryAt, now)
    val locked by remember(remaining) { derivedStateOf { remaining.value > 0 } }
    Column(
        Modifier.fillMaxWidth().testTag(identifier).semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.radiusLg.dp),
    ) {
        Layout(
            modifier = Modifier.fillMaxWidth().heightIn(min = PlanUITokens.actionMinHeight.dp),
            content = {
                TextButton(
                    onCancel,
                    Modifier
                        .defaultMinSize(
                            minWidth = PlanUITokens.actionMinHeight.dp,
                            minHeight = PlanUITokens.actionMinHeight.dp,
                        ).testTag("$identifier.cancel"),
                    enabled = !busy,
                    contentPadding = PaddingValues(vertical = PlanUITokens.space2.dp),
                ) {
                    Text(
                        securityText("Cancel"),
                        Modifier.widthIn(min = PlanUITokens.actionMinHeight.dp).testTag("$identifier.cancel.label"),
                        color = CfTheme.colors.dawnText,
                    )
                }
                Text(
                    title,
                    Modifier.semantics { heading() },
                    style = CfTheme.type.style(CfTypeScale.settingsStrong),
                )
            },
        ) { children, constraints ->
            val cancel = children[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val heading =
                children[1].measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxWidth = (constraints.maxWidth - 2 * cancel.width).coerceAtLeast(0),
                    ),
                )
            val height = maxOf(constraints.minHeight, cancel.height, heading.height)
            layout(constraints.maxWidth, height) {
                cancel.placeRelative(0, (height - cancel.height) / 2)
                heading.placeRelative((constraints.maxWidth - heading.width) / 2, (height - heading.height) / 2)
            }
        }
        Text(
            securityText(detail, detailArgs),
            style = CfTheme.type.style(CfTypeScale.settingsPromptBody),
            color = CfTheme.colors.mutedForeground,
        )
        val alert = if (locked) securityText("Too many attempts.") else error?.let { securityText(it, errorArgs) }
        val enabled = !busy && !locked
        val canSubmit = enabled && password.text.isNotEmpty()
        val label = securityText("Password")
        val keyboard = LocalSoftwareKeyboardController.current
        Column(verticalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp)) {
            val shape = RoundedCornerShape(PlanUITokens.radiusMd.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag("$identifier.field")
                    .alpha(if (enabled) 1f else SettingsTokens.dimmedFieldOpacity)
                    .background(CfTheme.colors.surface2, shape)
                    .then(
                        if (alert == null) {
                            Modifier
                        } else {
                            Modifier.border(
                                SettingsTokens.selectionWidth.dp,
                                CfTheme.colors.destructive,
                                shape,
                            )
                        },
                    ).heightIn(min = SettingsTokens.promptFieldHeight.dp)
                    .padding(horizontal = PlanUITokens.space4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PasswordPromptMetrics.fieldGap),
            ) {
                BasicSecureTextField(
                    state = password,
                    modifier =
                        Modifier
                            .weight(
                                1f,
                            ).heightIn(
                                min = SettingsTokens.promptFieldHeight.dp,
                            ).testTag(fieldTag)
                            .semantics {
                                contentDescription = label
                                contentType = ContentType.Password
                                if (alert != null) error(alert)
                            },
                    enabled = enabled,
                    textStyle =
                        CfTheme.type
                            .style(
                                CfTypeScale.screenBodyLg,
                            ).copy(color = CfTheme.colors.cardForeground),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    onKeyboardAction = {
                        if (canSubmit) {
                            keyboard?.hide()
                            onSubmit()
                        }
                    },
                    textObfuscationMode = TextObfuscationMode.RevealLastTyped,
                    cursorBrush = SolidColor(CfTheme.colors.ring),
                    decorator = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (password.text.isEmpty()) {
                                Text(
                                    label,
                                    style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                                    color = CfTheme.colors.mutedForeground,
                                )
                            }
                            inner()
                        }
                    },
                )
                Icon(
                    LucideGlyphs.Lock.imageVector(),
                    null,
                    Modifier.size(PasswordPromptMetrics.lockSize),
                    tint = CfTheme.colors.mutedForeground,
                )
            }
            if (alert != null) {
                Row(
                    Modifier.testTag(errorTag).semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp),
                ) {
                    Icon(
                        LucideGlyphs.CircleAlert.imageVector(),
                        null,
                        Modifier.size(AuthTokens.icon.dp),
                        tint = CfTheme.colors.destructive,
                    )
                    if (locked) {
                        PromptLockoutText { remaining.value }
                    } else {
                        Text(alert, style = CfTheme.type.screenBody, color = CfTheme.colors.destructive)
                    }
                }
            }
            TextButton(
                { showingReset = true },
                Modifier
                    .defaultMinSize(
                        minWidth = PlanUITokens.actionMinHeight.dp,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                    ).minimumInteractiveComponentSize()
                    .testTag("$identifier.forgot"),
                enabled = enabled,
                contentPadding = PaddingValues(vertical = PlanUITokens.space2.dp),
            ) {
                Text(
                    securityText("Forgot password?"),
                    Modifier.widthIn(min = PlanUITokens.actionMinHeight.dp).testTag("$identifier.forgot.label"),
                    style =
                        CfTheme.type.screenBody.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                        ),
                    color = CfTheme.colors.foreground,
                )
            }
        }
        if (message != null && !locked) {
            if (message == "Couldn't turn on two-factor.") {
                CfBanner(
                    securityText(message),
                    securityText("Nothing changed on your account. Try again in a moment."),
                    testTag = "$identifier.feedback",
                )
            } else {
                Text(
                    securityText(message),
                    Modifier.testTag("$identifier.feedback"),
                    style = CfTheme.type.screenBody,
                    color = CfTheme.colors.mutedForeground,
                )
            }
        }
        CfButton(
            securityText(if (busy) "Checking…" else button),
            {
                keyboard?.hide()
                onSubmit()
            },
            modifier = Modifier.testTag(submitTag),
            style = if (destructive) CfButtonStyle.Destructive else CfButtonStyle.Primary,
            enabled = canSubmit,
            loading = busy,
            loadingDescription = securityText("Checking…"),
            fillWidth = true,
            minHeight = SettingsTokens.promptButtonHeight.dp,
            textStyle = CfTheme.type.style(CfTypeScale.settingsStrong),
            disabledAlpha = SettingsTokens.disabledOpacity,
        )
    }
    if (showingReset) {
        CfAlertDialog(
            title = securityText("Reset your password?"),
            text =
                securityText(
                    "We'll email a reset link to {{email}}. You stay signed in on this phone.",
                    mapOf("email" to email),
                ),
            confirm =
                CfDialogAction(securityText("Send link"), {
                    showingReset = false
                    onReset()
                }),
            onDismissRequest = { showingReset = false },
            dismiss = CfDialogAction(securityText("Cancel"), { showingReset = false }),
            testTag = "$identifier.forgotAlert",
        )
    }
}

@Composable
internal fun PromptAlertLine(
    retryAt: Instant?,
    now: () -> Instant,
    content: @Composable (Int) -> Unit,
) {
    val remaining by promptRemaining(retryAt, now)
    content(remaining)
}

@Composable
private fun PromptLockoutText(remaining: () -> Int) {
    Text(lockoutText(remaining()), style = CfTheme.type.screenBody, color = CfTheme.colors.destructive)
}

@Composable
private fun promptRemaining(
    retryAt: Instant?,
    now: () -> Instant,
): State<Int> =
    produceState(remainingSeconds(retryAt, now()), retryAt, now) {
        do {
            value = remainingSeconds(retryAt, now())
            if (value == 0) break
            delay(1_000)
        } while (true)
    }
