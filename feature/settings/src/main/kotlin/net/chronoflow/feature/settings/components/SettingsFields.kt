package net.chronoflow.feature.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
fun SettingsReadOnlyField(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(AuthTokens.fieldGap.dp),
    ) {
        Text(label, style = CfTheme.type.authLabel)
        SelectionContainer {
            Text(
                value,
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = AuthTokens.touch.dp)
                    .border(
                        AuthTokens.borderWidth.dp,
                        CfTheme.colors.input,
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(AuthTokens.fieldRadius.dp),
                    ).padding(horizontal = AuthTokens.inset.dp, vertical = AuthTokens.fieldGap.dp),
                style = CfTheme.type.authBody,
            )
        }
    }
}

@Composable
fun SettingsOfflineHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp)) {
        Icon(
            LucideGlyphs.CloudOff.imageVector(),
            null,
            Modifier.size(AuthTokens.cardGap.dp),
            tint = CfTheme.colors.mutedForeground,
        )
        Text(text, style = CfTheme.type.style(CfTypeScale.settingsHint), color = CfTheme.colors.mutedForeground)
    }
}

@Composable
fun SettingsNeedsConnection(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.settings_needs_a_connection),
        modifier.testTag("settings.needsConnection"),
        style = CfTheme.type.screenBody,
        color = CfTheme.colors.mutedForeground,
    )
}
