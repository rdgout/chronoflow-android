package net.chronoflow.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.app.AppGraph
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.account.AccountRoute
import net.chronoflow.feature.settings.account.AccountViewModel
import net.chronoflow.feature.settings.components.SettingsLoadingRows
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.home.SettingsHomeRoute
import net.chronoflow.feature.settings.home.SettingsHomeViewModel
import net.chronoflow.feature.settings.language.LanguageDependenciesKey
import net.chronoflow.feature.settings.language.LanguageRoute
import net.chronoflow.feature.settings.language.LanguageSettingsViewModel
import net.chronoflow.feature.settings.profile.ProfileRoute
import net.chronoflow.feature.settings.profile.ProfileViewModel
import java.io.IOException

@Composable
internal fun SettingsEntry(
    graph: AppGraph,
    session: SessionGraph?,
    route: Route,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
    val languageExtras =
        remember(owner, graph) {
            MutableCreationExtras(owner.defaultViewModelCreationExtras).apply {
                set(LanguageDependenciesKey, graph.languageApplier)
                set(SettingsMainKey, graph.dispatchers.main)
            }
        }
    if (route == Route.Language) {
        LanguageRoute(viewModel(factory = LanguageSettingsViewModel.Factory, extras = languageExtras), onBack, modifier)
        return
    }
    if (session == null) {
        SettingsPage(
            when (route) {
                Route.Account -> R.string.settings_account
                Route.Profile -> R.string.settings_profile
                else -> R.string.settings_settings
            },
            onBack.takeUnless { route == Route.Settings },
            "settings.loading.screen",
            modifier,
        ) { SettingsLoadingRows() }
        return
    }
    val deps = remember(graph, session) { SessionSettingsDependencies(graph, session) }
    val extras =
        remember(owner, deps) {
            MutableCreationExtras(owner.defaultViewModelCreationExtras).apply {
                set(SettingsDependenciesKey, deps)
                set(SettingsMainKey, graph.dispatchers.main)
            }
        }
    when (route) {
        Route.Settings -> {
            LaunchedEffect(graph) {
                try {
                    graph.devicePreferences.reconcileDeviceConfig()
                } catch (_: IOException) {
                }
            }
            SettingsHomeRoute(
                viewModel(factory = SettingsHomeViewModel.Factory, extras = extras),
                deps,
                LaunchPlatform.routeLabel != null,
                onNavigate,
                modifier,
            )
        }

        Route.Account -> {
            val languageModel: LanguageSettingsViewModel =
                viewModel(
                    key = "account.language",
                    factory = LanguageSettingsViewModel.Factory,
                    extras = languageExtras,
                )
            val language by languageModel.ui.collectAsStateWithLifecycle()
            AccountRoute(
                viewModel(factory = AccountViewModel.Factory, extras = extras),
                language.choice ?: LanguageChoice.System,
                language.phone,
                onBack,
                onNavigate,
                modifier,
            )
        }

        Route.Profile -> {
            ProfileRoute(viewModel(factory = ProfileViewModel.Factory, extras = extras), onBack, modifier)
        }

        else -> {
            error("Unsupported settings entry")
        }
    }
}
