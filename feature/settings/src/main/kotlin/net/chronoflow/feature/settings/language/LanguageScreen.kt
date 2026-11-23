package net.chronoflow.feature.settings.language

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.settings.SettingsPage
import net.chronoflow.feature.settings.SettingsDebugCounters
import net.chronoflow.feature.settings.components.SettingsOption
import net.chronoflow.feature.settings.components.SettingsRadioCards

sealed interface LanguageEvent {
    data object Back : LanguageEvent

    data class Select(
        val choice: LanguageChoice,
    ) : LanguageEvent
}

@Composable
fun LanguageScreen(
    state: LanguageUiState,
    onEvent: (LanguageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(R.string.settings_language, { onEvent(LanguageEvent.Back) }, "settings.language.screen", modifier) {
        Text(
            stringResource(R.string.settings_chronoflow_follows_your_phone_s_ca480e),
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            color = CfTheme.colors.mutedForeground,
        )
        SettingsRadioCards(
            listOf(
                SettingsOption(
                    "system",
                    stringResource(R.string.settings_phone_language),
                    stringResource(
                        R.string.settings_now_language_changes_with_your_phone,
                        stringResource(
                            if (state.phone ==
                                "nl"
                            ) {
                                R.string.settings_nederlands
                            } else {
                                R.string.settings_english
                            },
                        ),
                    ),
                ),
                SettingsOption(
                    "en",
                    stringResource(R.string.settings_english),
                    stringResource(R.string.settings_always_english),
                ),
                SettingsOption(
                    "nl",
                    stringResource(R.string.settings_nederlands),
                    stringResource(R.string.settings_always_dutch),
                ),
            ),
            state.choice?.raw.orEmpty(),
            "settings.language.choice",
            { raw ->
                if (!state.busy) LanguageChoice.fromRaw(raw)?.let { onEvent(LanguageEvent.Select(it)) }
            },
        )
    }
}

@Composable
fun LanguageRoute(
    viewModel: LanguageSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, activity) {
        viewModel.arrived()
        viewModel.ui.map { it.recreate }.filterNotNull().distinctUntilChanged().collect { id ->
            activity?.let {
                it.recreate()
                viewModel.recreated(id)
            }
        }
    }
    LaunchedEffect(viewModel, snackbar, resources) {
        viewModel.ui.map { it.notice }.filterNotNull().distinctUntilChanged().collect { notice ->
            snackbar.showSnackbar(
                resources.getString(
                    when (notice.choice) {
                        LanguageChoice.System -> R.string.settings_saved_chronoflow_follows_your_phone
                        LanguageChoice.En -> R.string.settings_saved_the_app_is_in_english
                        LanguageChoice.Nl -> R.string.settings_saved_the_app_is_in_dutch
                    },
                ),
                duration = SnackbarDuration.Short,
            )
        }
    }
    Box(modifier.fillMaxSize()) {
        LanguageScreen(ui, {
            when (it) {
                LanguageEvent.Back -> onBack()
                is LanguageEvent.Select -> viewModel.select(it.choice)
            }
        })
        CfSnackbarHost(
            snackbar,
            Modifier
                .align(
                    Alignment.BottomCenter,
                ).testTag("settings.language.saved"),
            onPresented = {
                viewModel.ui.value.notice?.let { notice ->
                    SettingsDebugCounters.presented()
                    viewModel.presented(notice.id)
                }
            },
        )
    }
}
