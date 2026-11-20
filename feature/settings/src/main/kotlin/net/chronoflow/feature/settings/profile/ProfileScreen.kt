package net.chronoflow.feature.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsOfflineHint
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.SettingsReadOnlyField
import net.chronoflow.feature.settings.components.SettingsStoredState

data class ProfileScreenState(
    val ui: ProfileUiState,
    val name: TextFieldState,
)

enum class ProfileEvent { Back, Save, Retry }

@Composable
fun ProfileScreen(
    state: ProfileScreenState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    SettingsPage(R.string.settings_profile, { onEvent(ProfileEvent.Back) }, "settings.profile.screen", modifier) {
        val editor = state.ui.editor
        if (state.ui.pages.account == null || editor == null) {
            SettingsStoredState(state.ui.pages.loaded, state.ui.pages.failed, { onEvent(ProfileEvent.Retry) })
        } else {
            CfCard {
                CfTextField(
                    state.name,
                    stringResource(R.string.settings_name),
                    testTag = "settings.profile.name",
                    enabled = !editor.busy,
                    error =
                        editor.error?.let {
                            stringResource(
                                if (it ==
                                    ProfileError.EnterName
                                ) {
                                    R.string.settings_enter_your_name
                                } else {
                                    R.string.settings_we_couldn_t_save_your_change_ple_354da2
                                },
                            )
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    contentType = ContentType.PersonFullName,
                    onKeyboardAction = {
                        focus.clearFocus()
                        onEvent(ProfileEvent.Save)
                    },
                )
                SettingsReadOnlyField(stringResource(R.string.settings_email), editor.email, "settings.profile.email")
            }
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                val save = stringResource(R.string.settings_save_changes)
                CfButton(
                    save,
                    {
                        focus.clearFocus()
                        onEvent(ProfileEvent.Save)
                    },
                    Modifier.testTag("settings.profile.save"),
                    enabled = !editor.busy,
                    loading = editor.busy,
                    loadingDescription = save,
                    fillWidth = true,
                    minHeight = PlanUITokens.actionMinHeight.dp,
                )
                if (editor.saved) {
                    SettingsOfflineHint(
                        stringResource(R.string.settings_saved_on_this_phone_syncs_when_y_4fb8b8),
                        Modifier.testTag("settings.profile.saved"),
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val field = remember(viewModel, ui.editor != null) { TextFieldState(ui.editor?.name.orEmpty()) }
    if (ui.editor !=
        null
    ) {
        LaunchedEffect(viewModel, field) { snapshotFlow { field.text.toString() }.collect(viewModel::edit) }
    }
    LaunchedEffect(ui.editor?.name) {
        ui.editor?.name?.let { if (field.text.toString() != it) field.setTextAndPlaceCursorAtEnd(it) }
    }
    ProfileScreen(ProfileScreenState(ui, field), {
        when (it) {
            ProfileEvent.Back -> onBack()
            ProfileEvent.Save -> viewModel.submit()
            ProfileEvent.Retry -> viewModel.retry()
        }
    }, modifier)
}
