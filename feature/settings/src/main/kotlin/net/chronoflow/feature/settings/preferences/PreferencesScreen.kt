package net.chronoflow.feature.settings.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfPillOption
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.core.ui.plan.CategorySwatchSize
import net.chronoflow.feature.settings.components.SettingsDestinationRow
import net.chronoflow.feature.settings.components.SettingsError
import net.chronoflow.feature.settings.components.SettingsEyebrow
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.SettingsPills
import net.chronoflow.feature.settings.components.SettingsStoredState
import net.chronoflow.feature.settings.components.settingsText

sealed interface PreferencesEvent {
    data object Back : PreferencesEvent

    data object Retry : PreferencesEvent

    data object Reminders : PreferencesEvent

    data object Replay : PreferencesEvent

    data class Theme(
        val id: String,
    ) : PreferencesEvent

    data class TimeFormat(
        val format: String,
    ) : PreferencesEvent

    data class Shown(
        val group: SettingsTimelineGroup,
        val shown: Boolean,
    ) : PreferencesEvent

    fun destination(): Route? =
        when (this) {
            Reminders -> Route.Reminders
            Replay -> Route.Walkthrough(replay = true)
            else -> null
        }
}

@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    onEvent: (PreferencesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(
        R.string.settings_preferences,
        { onEvent(PreferencesEvent.Back) },
        "settings.preferences.screen",
        modifier,
    ) {
        val editor = state.editor
        if (editor == null) {
            SettingsStoredState(state.pages.settingsLoaded, state.pages.failed, { onEvent(PreferencesEvent.Retry) })
            return@SettingsPage
        }
        CfCard {
            SettingsEyebrow(stringResource(R.string.settings_appearance))
            SettingsPills(
                stringResource(R.string.settings_theme),
                listOf(
                    CfPillOption("system", stringResource(R.string.settings_system)),
                    CfPillOption("light", stringResource(R.string.settings_light)),
                    CfPillOption("dark", stringResource(R.string.settings_dark)),
                ),
                state.theme.raw,
                "settings.preferences.theme",
                { onEvent(PreferencesEvent.Theme(it)) },
            )
            SettingsPills(
                stringResource(R.string.settings_time_format),
                listOf(
                    CfPillOption("24", stringResource(R.string.settings_24_hour_14_30)),
                    CfPillOption("12", stringResource(R.string.settings_12_hour_2_30_pm)),
                ),
                editor.timeFormat,
                "settings.preferences.timeFormat",
                { onEvent(PreferencesEvent.TimeFormat(it)) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                SettingsEyebrow(stringResource(R.string.settings_timeline_categories))
                Text(
                    stringResource(R.string.settings_hide_a_category_to_keep_your_tim_9de107),
                    style = CfTheme.type.screenBody,
                    color = CfTheme.colors.mutedForeground,
                )
            }
            CfCard(rows = true) {
                SettingsTimelineGroup.all.forEachIndexed { index, group ->
                    if (index > 0) CfDivider()
                    val shown = editor.shown(group)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = SettingsTokens.counterHeight.dp)
                            .cfToggleable(shown, { onEvent(PreferencesEvent.Shown(group, it)) })
                            .testTag("settings.preferences.${group.title}")
                            .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
                        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategorySwatch(
                            when (group.title) {
                                "Sleep" -> BlockType.Sleep
                                "Light" -> BlockType.SeekLight
                                "Caffeine" -> BlockType.CaffeineServing
                                else -> BlockType.Flight
                            },
                            size = CategorySwatchSize.Small,
                        )
                        Text(
                            settingsText(group.title),
                            Modifier.weight(1f),
                            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                        )
                        CfSwitch(shown, null)
                    }
                }
            }
        }
        CfCard(rows = true) {
            SettingsDestinationRow(
                stringResource(R.string.reminders_connectivity_reminders),
                LucideGlyphs.Bell,
                "settings.preferences.reminders",
                { onEvent(PreferencesEvent.Reminders) },
                chevron = true,
            )
            CfDivider()
            SettingsDestinationRow(
                stringResource(R.string.settings_replay_walkthrough),
                LucideGlyphs.RotateCcw,
                "settings.preferences.replay",
                { onEvent(PreferencesEvent.Replay) },
                subtitle = stringResource(R.string.settings_the_short_tour_from_your_first_trip),
                chevron = false,
            )
        }
        if (state.failure) {
            SettingsError(
                stringResource(R.string.settings_we_couldn_t_save_your_change_ple_354da2),
                Modifier.testTag("settings.preferences.failure"),
            )
        }
    }
}

@Composable
fun PreferencesRoute(
    viewModel: PreferencesViewModel,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val event: (PreferencesEvent) -> Unit =
        remember(viewModel, onBack, onNavigate) {
            { event ->
                when (event) {
                    PreferencesEvent.Back -> onBack()
                    PreferencesEvent.Retry -> viewModel.retry()
                    is PreferencesEvent.Theme -> viewModel.setTheme(event.id)
                    is PreferencesEvent.TimeFormat -> viewModel.setTimeFormat(event.format)
                    is PreferencesEvent.Shown -> viewModel.setShown(event.shown, event.group)
                    PreferencesEvent.Reminders, PreferencesEvent.Replay -> event.destination()?.let(onNavigate)
                }
            }
        }
    PreferencesScreen(state, event, modifier)
}
