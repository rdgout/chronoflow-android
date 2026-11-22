package net.chronoflow.feature.settings.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfRow
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.settings.SettingsPage
import net.chronoflow.feature.settings.components.SettingsStoredState

data class SecurityScreenState(
    val summary: SecuritySummary? = null,
    val loaded: Boolean = false,
    val failed: Boolean = false,
    val passkeysEnabled: Boolean = false,
)

sealed interface SecurityEvent {
    data object Back : SecurityEvent

    data object Retry : SecurityEvent

    data class Navigate(
        val route: Route,
    ) : SecurityEvent
}

@Composable
fun SecurityScreen(
    state: SecurityScreenState,
    onEvent: (SecurityEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(R.string.settings_security, { onEvent(SecurityEvent.Back) }, "settings.security.screen", modifier) {
        val summary = state.summary
        if (summary == null) {
            SettingsStoredState(state.loaded, state.failed, { onEvent(SecurityEvent.Retry) })
        } else {
            CfCard(rows = true) {
                CfRow(
                    securityText("Password"),
                    subtitle = securityText("Change your password"),
                    icon = LucideGlyphs.Lock.imageVector(),
                    testTag = "settings.security.password",
                    onClick = { onEvent(SecurityEvent.Navigate(Route.Password)) },
                )
                CfDivider()
                if (summary.passkeyCount != null && state.passkeysEnabled) {
                    CfRow(
                        securityText("Passkeys"),
                        subtitle =
                            pluralStringResource(
                                R.plurals.settings_n_passkeys,
                                summary.passkeyCount,
                                summary.passkeyCount,
                            ),
                        icon = LucideGlyphs.KeyRound.imageVector(),
                        testTag = "settings.security.passkeys",
                        onClick = { onEvent(SecurityEvent.Navigate(Route.Passkeys)) },
                    )
                    CfDivider()
                }
                CfRow(
                    securityText("Two-factor authentication"),
                    subtitle = securityText("A code from an app when you sign in"),
                    icon = LucideGlyphs.Smartphone.imageVector(),
                    badge = securityText(if (summary.twoFactorEnabled) "On" else "Off"),
                    badgeDawn = summary.twoFactorEnabled,
                    testTag = "settings.security.twoFactor",
                    onClick = { onEvent(SecurityEvent.Navigate(Route.TwoFactorSettings)) },
                )
            }
        }
    }
}

@Composable
fun SecurityRoute(
    viewModel: SecurityViewModel,
    passkeysEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    SecurityScreen(
        SecurityScreenState(ui.summary, ui.pages.settingsLoaded, ui.pages.failed, passkeysEnabled),
        { event ->
            when (event) {
                SecurityEvent.Back -> onBack()
                SecurityEvent.Retry -> viewModel.retry()
                is SecurityEvent.Navigate -> onNavigate(event.route)
            }
        },
        modifier,
    )
}
