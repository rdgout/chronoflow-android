package net.chronoflow.feature.settings.passkeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsError
import net.chronoflow.feature.settings.components.SettingsLoadingRows
import net.chronoflow.feature.settings.components.SettingsNeedsConnection

@Composable
fun PasskeysContent(
    state: PasskeysScreenState,
    onEvent: (PasskeysEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PlanUITokens.space6.dp)) {
        Text(
            stringResource(R.string.android_passkeys_intro),
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            color = CfTheme.colors.mutedForeground,
        )
        val security = state.security
        val passkeys = security.passkeys
        when {
            passkeys == null && !security.passkeysUnavailable -> {
                SettingsLoadingRows()
            }

            passkeys == null && !security.offline -> {
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                    SettingsError(stringResource(R.string.settings_something_went_wrong))
                    CfButton(
                        stringResource(R.string.settings_try_again),
                        { onEvent(PasskeysEvent.Refresh) },
                        Modifier.testTag("settings.passkeys.retry"),
                        style = CfButtonStyle.Outline,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                    )
                }
            }

            passkeys != null && passkeys.isEmpty() -> {
                PasskeysEmpty()
            }

            passkeys != null -> {
                val title = stringResource(R.string.settings_your_passkeys)
                val listModifier =
                    Modifier
                        .testTag("settings.passkeys.list")
                        .semantics { contentDescription = title }
                CfCard(listModifier, rows = true) {
                    passkeys.forEachIndexed { index, passkey ->
                        key(passkey.id) {
                            if (index > 0) CfDivider()
                            PasskeyRow(passkey, state.meta[passkey.id], !security.busy && !security.offline) {
                                onEvent(PasskeysEvent.Remove(passkey))
                            }
                        }
                    }
                }
            }
        }
        val needsConnection = stringResource(R.string.settings_needs_a_connection)
        CfButton(
            stringResource(R.string.settings_add_a_passkey),
            { onEvent(PasskeysEvent.Add) },
            Modifier
                .testTag(
                    "settings.passkeys.add",
                ).semantics { if (security.offline) stateDescription = needsConnection },
            enabled =
                state.canRegister && !security.busy && !security.offline &&
                    security.retryAt?.isAfter(state.now()) != true,
            fillWidth = true,
            minHeight = PlanUITokens.actionMinHeight.dp,
            size = CfButtonSize.Lg,
            leadingIcon = LucideGlyphs.Plus.imageVector(),
            leadingIconSize = PlanUITokens.space4.dp,
        )
        if (security.offline) SettingsNeedsConnection(testTag = "settings.passkeys.offline")
    }
}

@Composable
private fun PasskeyDisc(
    size: Float,
    icon: Float,
) {
    Box(
        Modifier.size(size.dp).background(CfTheme.colors.muted, CfTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LucideGlyphs.KeyRound.imageVector(), null, Modifier.size(icon.dp), tint = CfTheme.colors.foreground)
    }
}

@Composable
private fun PasskeysEmpty() {
    val border = CfTheme.colors.border
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("settings.passkeys.empty")
            .semantics(mergeDescendants = true) {}
            .drawWithCache {
                val radius = CornerRadius(PlanUITokens.radius3Xl.dp.toPx())
                val dash = PlanUITokens.space2.dp.toPx()
                val stroke =
                    Stroke(
                        SettingsTokens.selectionWidth.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    )
                onDrawBehind { drawRoundRect(border, cornerRadius = radius, style = stroke) }
            }.padding(horizontal = PlanUITokens.space5.dp, vertical = PlanUITokens.space8.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PasskeyDisc(PlanUITokens.actionMinHeight, PlanUITokens.space5)
        Text(
            stringResource(R.string.settings_no_passkeys_yet),
            style = CfTheme.type.style(CfTypeScale.settingsStrong),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.settings_add_one_on_this_phone_and_you_ca_c57475),
            style = CfTheme.type.screenBody,
            color = CfTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PasskeyRow(
    passkey: net.chronoflow.core.model.api.ApiPasskey,
    meta: String?,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsTokens.rowHeight.dp)
            .padding(
                start = PlanUITokens.space4.dp,
                end = PlanUITokens.space2.dp,
                top = PlanUITokens.space2.dp,
                bottom = PlanUITokens.space2.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PasskeyDisc(SettingsTokens.rowDisc, PlanUITokens.space4)
        Column(Modifier.weight(1f)) {
            Text(passkey.name, style = CfTheme.type.style(CfTypeScale.settingsRow))
            meta?.let { meta ->
                Text(
                    meta,
                    style = CfTheme.type.screenBody.copy(fontFeatureSettings = "tnum"),
                    color = CfTheme.colors.mutedForeground,
                )
            }
        }
        CfIconButton(
            LucideGlyphs.Trash.imageVector(),
            stringResource(R.string.settings_remove_passkey_name, passkey.name),
            onRemove,
            modifier = Modifier.testTag("settings.passkeys.remove"),
            enabled = enabled,
            iconSize = PasskeysMetrics.TRASH.dp,
        )
    }
}
