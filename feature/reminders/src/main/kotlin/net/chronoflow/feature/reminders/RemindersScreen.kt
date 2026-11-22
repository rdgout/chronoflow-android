package net.chronoflow.feature.reminders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.settings.SettingsPage

sealed interface RemindersEvent {
    data object Back : RemindersEvent

    data object Ask : RemindersEvent

    data object OpenSettings : RemindersEvent

    data class Toggle(
        val enabled: Boolean,
    ) : RemindersEvent

    data class Lead(
        val minutes: Int,
    ) : RemindersEvent
}

@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onEvent: (RemindersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(R.string.reminders_connectivity_reminders, {
        onEvent(RemindersEvent.Back)
    }, "reminders.screen", modifier) {
        RemindersContent(
            state,
            { onEvent(RemindersEvent.Ask) },
            { onEvent(RemindersEvent.Toggle(it)) },
            { onEvent(RemindersEvent.Lead(it)) },
            { onEvent(RemindersEvent.OpenSettings) },
        )
    }
}

@Composable
fun RemindersContent(
    state: RemindersUiState,
    onAsk: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onLead: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    CfCard(modifier.testTag("reminders.card")) {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
            Text(
                stringResource(R.string.reminders_reminders_on_this_phone),
                Modifier.semantics { heading() },
                style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
            )
            Text(
                stringResource(R.string.reminders_a_nudge_before_each_step_in_you_fde8f2),
                style = CfTheme.type.style(CfTypeScale.screenBody),
                color = colors.mutedForeground,
            )
        }
        when (state.permission) {
            ReminderPermission.Undetermined -> {
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                    Text(
                        stringResource(R.string.reminders_reminders_are_off_when_you_turn_968d52),
                        style = CfTheme.type.style(CfTypeScale.screenBody),
                        color = colors.foreground,
                    )
                    CfButton(
                        stringResource(R.string.reminders_turn_on_reminders),
                        onAsk,
                        Modifier.testTag("reminders.ask"),
                        size = CfButtonSize.Lg,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                        fillWidth = true,
                        enabled = !state.busy && state.settings != null,
                    )
                }
            }

            ReminderPermission.Unsupported -> {
                CfNotice(
                    stringResource(R.string.reminders_this_device_can_t_show_reminders),
                    stringResource(R.string.reminders_you_can_still_follow_every_step_6dddec),
                    Modifier.testTag("reminders.unsupported"),
                )
            }

            ReminderPermission.Granted, ReminderPermission.Blocked -> {
                val enabled = state.permission == ReminderPermission.Granted && !state.busy && state.settings != null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("reminders.enabled")
                        .cfToggleable(state.active, onToggle, enabled)
                        .heightIn(min = SettingsTokens.promptFieldHeight.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                ) {
                    Text(
                        stringResource(R.string.reminders_push_reminders),
                        Modifier.weight(1f),
                        style = CfTheme.type.settingsRow,
                    )
                    CfSwitch(state.active, null, enabled = enabled)
                }
            }

            null -> {}
        }
        if (state.permission == ReminderPermission.Blocked) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                CfNotice(
                    stringResource(R.string.reminders_notifications_are_blocked_for_c_210a5a),
                    stringResource(R.string.reminders_allow_them_in_your_phone_s_sett_2852cd),
                    Modifier.testTag("reminders.blocked"),
                    CfNoticeTone.Warning,
                )
                Column(
                    Modifier.padding(start = PlanUITokens.space1.dp),
                    verticalArrangement = Arrangement.spacedBy(SettingsTokens.rowVertical.dp),
                ) {
                    ReminderInstruction(1, stringResource(R.string.android_notifications_blocked_step))
                    ReminderInstruction(2, stringResource(R.string.reminders_come_back_here_reminders_switch_9fb58c))
                }
                CfButton(
                    stringResource(R.string.reminders_open_phone_settings),
                    onOpenSettings,
                    Modifier.testTag("reminders.openSettings"),
                    style = CfButtonStyle.Outline,
                    size = CfButtonSize.Lg,
                    minHeight = PlanUITokens.actionMinHeight.dp,
                    fillWidth = true,
                )
            }
        }
        if (state.active) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp)) {
                CfDivider()
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                    val label = stringResource(R.string.reminders_remind_me_ahead_by)
                    Text(label, style = CfTheme.type.settingsRow)
                    CfSegmentedButtons(
                        listOf(
                            stringResource(R.string.reminders_15_min),
                            stringResource(R.string.reminders_30_min),
                            stringResource(R.string.reminders_1_hour),
                        ),
                        listOf(15, 30, 60).indexOf(state.settings?.leadMin),
                        { onLead(listOf(15, 30, 60)[it]) },
                        Modifier.fillMaxWidth().semantics { contentDescription = label },
                        testTag = "reminders.lead",
                        optionEnabled = { !state.busy },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderInstruction(
    number: Int,
    instruction: String,
) {
    val strong = CfTheme.type.style(CfTypeScale.screenLabel).toSpanStyle()
    val words =
        listOf(stringResource(R.string.reminders_open_phone_settings), stringResource(R.string.reminders_chronoflow))
    val text =
        buildAnnotatedString {
            append(instruction)
            words.forEach { word ->
                var start = instruction.indexOf(word)
                while (start >= 0) {
                    addStyle(strong, start, start + word.length)
                    start = instruction.indexOf(word, start + word.length)
                }
            }
        }
    Row(horizontalArrangement = Arrangement.spacedBy(SettingsTokens.rowVertical.dp)) {
        Box(
            Modifier
                .size(
                    PlanUITokens.space6.dp,
                ).background(CfTheme.colors.muted, CircleShape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.reminders_count, number.toString()),
                style =
                    CfTheme.type
                        .style(
                            CfTypeScale.screenMeta,
                        ).copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            )
        }
        Text(text, Modifier.weight(1f), style = CfTheme.type.style(CfTypeScale.screenBody))
    }
}

@Composable
fun RemindersRoute(
    vm: RemindersViewModel,
    main: CoroutineDispatcher,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReminderPermissionLauncher(vm.permissionLaunches, vm::onPermissionResult, vm::acknowledgePermissionLaunch, main)
    DisposableEffect(vm) {
        vm.start()
        onDispose { vm.stop() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermission() }
    RemindersScreen(state, { event ->
        when (event) {
            RemindersEvent.Back -> onBack()
            RemindersEvent.Ask -> vm.requestPermission()
            RemindersEvent.OpenSettings -> onOpenSettings()
            is RemindersEvent.Toggle -> vm.setEnabled(event.enabled)
            is RemindersEvent.Lead -> vm.setLead(event.minutes)
        }
    }, modifier)
}

@Composable
fun ReminderPermissionLauncher(
    launches: kotlinx.coroutines.flow.StateFlow<Long?>,
    onResult: () -> Unit,
    onLaunch: (Long) -> Unit,
    main: CoroutineDispatcher,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    val currentLauncher by rememberUpdatedState(launcher)
    val currentOnLaunch by rememberUpdatedState(onLaunch)
    LaunchedEffect(launches, main) {
        withContext(main) {
            launches.filterNotNull().collect { id ->
                if (Build.VERSION.SDK_INT >= 33) {
                    currentLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                currentOnLaunch(id)
            }
        }
    }
}
