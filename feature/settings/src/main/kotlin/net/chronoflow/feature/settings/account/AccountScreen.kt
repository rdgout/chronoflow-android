package net.chronoflow.feature.settings.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.components.SettingsDestinationRow
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.SettingsStoredState

data class AccountScreenState(
    val pages: SettingsPagesState,
    val language: LanguageChoice = LanguageChoice.System,
    val phone: String = "en",
)

enum class AccountEvent { Back, Profile, Language, Retry }

@Composable
fun AccountScreen(
    state: AccountScreenState,
    onEvent: (AccountEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(R.string.settings_account, { onEvent(AccountEvent.Back) }, "settings.account.screen", modifier) {
        val account = state.pages.account
        if (account ==
            null
        ) {
            SettingsStoredState(state.pages.loaded, state.pages.failed, { onEvent(AccountEvent.Retry) })
        } else {
            CfCard(rows = true) {
                SettingsDestinationRow(
                    stringResource(R.string.settings_profile),
                    LucideGlyphs.User,
                    "settings.account.profile",
                    { onEvent(AccountEvent.Profile) },
                    subtitle = "${account.name} · ${account.email}",
                    singleLineSubtitle = true,
                )
                CfDivider()
                SettingsDestinationRow(
                    stringResource(R.string.settings_language),
                    LucideGlyphs.Globe,
                    "settings.account.language",
                    { onEvent(AccountEvent.Language) },
                    subtitle = languageSubtitle(state.language, state.phone),
                )
            }
        }
    }
}

@Composable
fun languageSubtitle(
    choice: LanguageChoice,
    phone: String,
): String =
    when (choice) {
        LanguageChoice.System -> {
            stringResource(
                R.string.settings_phone_language_language,
                stringResource(if (phone == "nl") R.string.settings_nederlands else R.string.settings_english),
            )
        }

        LanguageChoice.En -> {
            stringResource(R.string.settings_english)
        }

        LanguageChoice.Nl -> {
            stringResource(R.string.settings_nederlands)
        }
    }

@Composable
fun AccountRoute(
    viewModel: AccountViewModel,
    language: LanguageChoice,
    phone: String,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    AccountScreen(AccountScreenState(ui, language, phone), { event ->
        when (event) {
            AccountEvent.Back -> onBack()
            AccountEvent.Profile -> onNavigate(Route.Profile)
            AccountEvent.Language -> onNavigate(Route.Language)
            AccountEvent.Retry -> viewModel.retry()
        }
    }, modifier)
}
