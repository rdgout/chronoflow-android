package net.chronoflow.feature.settings.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.formatPlanDayMonth
import net.chronoflow.core.ui.R
import java.time.ZoneId

@Composable
fun SettingsSyncLine(
    state: SettingsHomeState,
    syncing: Boolean,
    offline: Boolean,
    clock: PlanClock,
    zone: ZoneId,
    formatter: CopyLineFormatter?,
    modifier: Modifier = Modifier,
) {
    val line = rememberSettingsSyncLine(state, syncing, offline, clock, zone) ?: return
    val language =
        if (LocalResources.current.configuration.locales[0]
                .language == "nl"
        ) {
            Language.Nl
        } else {
            Language.En
        }
    val text =
        when (line) {
            SyncLine.Syncing -> {
                stringResource(R.string.settings_syncing)
            }

            is SyncLine.Pending -> {
                pluralStringResource(R.plurals.settings_n_changes_waiting_to_sync, line.n, line.n)
            }

            SyncLine.Offline -> {
                stringResource(R.string.settings_saved_on_this_phone_syncs_when_y_4fb8b8)
            }

            SyncLine.JustNow -> {
                stringResource(R.string.settings_synced_just_now)
            }

            is SyncLine.Minutes -> {
                pluralStringResource(R.plurals.settings_synced_n_min_ago, line.n, line.n)
            }

            is SyncLine.Hours -> {
                pluralStringResource(R.plurals.settings_synced_n_hours_ago, line.n, line.n)
            }

            SyncLine.Yesterday -> {
                stringResource(R.string.settings_synced_yesterday)
            }

            is SyncLine.Days -> {
                pluralStringResource(R.plurals.settings_synced_n_days_ago, line.n, line.n)
            }

            is SyncLine.On -> {
                val date =
                    formatter?.formatDayMonth(line.raw, language, zone.id)
                        ?: formatPlanDayMonth(line.raw, language, zone.id)
                stringResource(R.string.settings_synced_on_date, date)
            }
        }
    val glyph =
        when (line) {
            SyncLine.Syncing -> LucideGlyphs.LoaderCircle
            is SyncLine.Pending -> LucideGlyphs.CloudUpload
            SyncLine.Offline -> LucideGlyphs.CloudOff
            else -> LucideGlyphs.CloudCheck
        }
    Row(
        modifier.testTag("settings.sync").padding(horizontal = PlanUITokens.space1.dp),
        horizontalArrangement = Arrangement.spacedBy(SettingsTokens.pillGap.dp),
    ) {
        Icon(glyph.imageVector(), null, Modifier.size(AuthTokens.cardGap.dp), tint = CfTheme.colors.mutedForeground)
        Text(text, style = CfTheme.type.style(CfTypeScale.settingsHint).tnum(), color = CfTheme.colors.mutedForeground)
    }
}

@Composable
internal fun rememberSettingsSyncLine(
    state: SettingsHomeState,
    syncing: Boolean,
    offline: Boolean,
    clock: PlanClock,
    zone: ZoneId,
): SyncLine? {
    val initial = remember(clock) { clock.now }
    val ticks = remember(clock) { clock.values() }
    val now by ticks.collectAsStateWithLifecycle(initial)
    return state.syncLine(now, syncing, offline, zone)
}
