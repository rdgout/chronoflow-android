package net.chronoflow.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
fun SettingsStoredState(
    loaded: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!loaded && !failed) {
        SettingsLoadingRows(modifier)
    } else {
        Column(
            modifier.testTag("settings.missing"),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
        ) {
            Text(
                stringResource(R.string.settings_your_account_isn_t_on_this_phone_yet),
                Modifier.testTag("settings.missing.title").semantics {
                    heading()
                },
                style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            )
            Text(
                stringResource(R.string.settings_we_need_a_connection_once_to_loa_a9f557),
                style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                color = CfTheme.colors.mutedForeground,
            )
            CfButton(
                stringResource(R.string.settings_try_again),
                onRetry,
                Modifier.testTag("settings.stored.retry"),
                style = CfButtonStyle.Outline,
                minHeight = PlanUITokens.actionMinHeight.dp,
            )
        }
    }
}

@Composable
fun SettingsLoadingRows(modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.settings_loading)
    CfCard(modifier.testTag("settings.loading").semantics { contentDescription = loading }, rows = true) {
        SettingsTokens.skeletonRowWidths.forEachIndexed { index, width ->
            if (index > 0) CfDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = SettingsTokens.counterHeight.dp,
                    ).padding(horizontal = PlanUITokens.space4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
            ) {
                CfSkeleton(Modifier.size(SettingsTokens.rowDisc.dp), shape = CircleShape)
                CfSkeleton(Modifier.width(width.dp).height(PlanUITokens.space4.dp))
            }
        }
    }
}
