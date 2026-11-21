package net.chronoflow.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
internal fun SettingsLockHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(SettingsTokens.pillGap.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            LucideGlyphs.Lock.imageVector(),
            null,
            Modifier.size(AuthTokens.icon.dp),
            tint = CfTheme.colors.mutedForeground,
        )
        Text(
            text,
            style = CfTheme.type.style(CfTypeScale.settingsHint),
            color = CfTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
