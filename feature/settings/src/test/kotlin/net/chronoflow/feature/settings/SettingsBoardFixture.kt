package net.chronoflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.account.AccountScreen
import net.chronoflow.feature.settings.account.AccountScreenState
import net.chronoflow.feature.settings.components.SettingsDestinationRow
import net.chronoflow.feature.settings.components.SettingsLoadingRows
import net.chronoflow.feature.settings.components.SettingsNeedsConnection
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.components.SettingsStoredState
import net.chronoflow.feature.settings.home.PendingChange
import net.chronoflow.feature.settings.home.PendingText
import net.chronoflow.feature.settings.home.SettingsHomeScreen
import net.chronoflow.feature.settings.home.SettingsHomeScreenState
import net.chronoflow.feature.settings.home.SettingsHomeState
import net.chronoflow.feature.settings.home.SettingsHomeUi
import net.chronoflow.feature.settings.home.SettingsSyncLine
import net.chronoflow.feature.settings.language.LanguageScreen
import net.chronoflow.feature.settings.language.LanguageUiState
import net.chronoflow.feature.settings.profile.ProfileError
import net.chronoflow.feature.settings.profile.ProfileScreen
import net.chronoflow.feature.settings.profile.ProfileScreenState
import net.chronoflow.feature.settings.profile.ProfileUiState
import net.chronoflow.feature.settings.profile.SettingsProfileState
import net.chronoflow.feature.settings.signout.SignOutPendingContent
import java.time.Instant
import java.time.ZoneOffset

internal object SettingsBoardFixture {
    val now: Instant = Instant.parse("2026-10-24T12:02:00Z")
    val account = AccountRecord("account", "Rick Gout", "rick@example.com", syncedAt = now.minusSeconds(120).toString())
    val pending =
        listOf(
            PendingChange("check", BlockType.AvoidLight, PendingText.CheckedOff(BlockType.AvoidLight)),
            PendingChange("sleep", BlockType.Sleep, PendingText.Sleep),
        )
    val home = SettingsHomeState(account = account, missingAnswers = 3, credits = 2)
}

@Composable
internal fun SettingsBoardFixture(
    state: String,
    wide: Boolean = false,
    nativeSheet: Boolean = false,
    shell: Boolean = false,
) {
    val clock = remember { FakePlanClock(SettingsBoardFixture.now) }
    val safe = LocalSafeInsets.current
    if (state == "Sheet") {
        Box(Modifier.fillMaxSize().background(CfTheme.colors.card).windowInsetsPadding(safe)) {
            SignOutPendingContent(SettingsBoardFixture.pending, false, {}, {})
        }
        return
    }
    val layout =
        if (!shell) {
            CfNavLayout.None
        } else if (wide) {
            CfNavLayout.Rail
        } else {
            CfNavLayout.Bar
        }
    val contentInsets =
        if (!shell) {
            safe
        } else {
            safe.only(
                if (wide) {
                    WindowInsetsSides.Top + WindowInsetsSides.End +
                        WindowInsetsSides.Bottom
                } else {
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                },
            )
        }
    val items =
        listOf(
            CfNavItem(
                stringResource(R.string.screens_today),
                MaterialSymbols.SpaceDashboard,
                MaterialSymbols.SpaceDashboardFill,
                "tab.today",
            ),
            CfNavItem(
                stringResource(R.string.screens_my_trips),
                MaterialSymbols.Flight,
                MaterialSymbols.FlightFill,
                "tab.trips",
            ),
            CfNavItem(
                stringResource(R.string.screens_settings),
                MaterialSymbols.Settings,
                MaterialSymbols.SettingsFill,
                "tab.settings",
            ),
        )
    Box(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag("settings.capture.window")) {
        CfNavigationSuiteScaffold(
            layout,
            items,
            2,
            {},
            if (shell) {
                Modifier.windowInsetsPadding(
                    safe.only(if (wide) WindowInsetsSides.Start else WindowInsetsSides.Bottom),
                )
            } else {
                Modifier
            },
        ) {
            CompositionLocalProvider(LocalSafeInsets provides contentInsets) {
                when {
                    state.startsWith("Home") || state == "PendingSheet" -> {
                        val home =
                            SettingsBoardFixture.home.let {
                                if (state ==
                                    "HomeNoAccount"
                                ) {
                                    it.copy(account = null)
                                } else {
                                    it
                                }
                            }
                        val pending =
                            if (state == "HomePending" ||
                                state == "PendingSheet"
                            ) {
                                home.copy(pendingCount = 2, pending = SettingsBoardFixture.pending)
                            } else {
                                home
                            }
                        SettingsHomeScreen(
                            SettingsHomeScreenState(
                                SettingsHomeUi(
                                    pending,
                                    syncing =
                                        state == "HomeSyncing",
                                    offline = state == "HomeOffline",
                                    showsPending = state == "PendingSheet" && nativeSheet,
                                ),
                                clock,
                                ZoneOffset.UTC,
                                showUnbuilt = true,
                            ),
                            {},
                        )
                    }

                    state == "Account" || state == "AccountMissing" -> {
                        AccountScreen(
                            AccountScreenState(
                                SettingsPagesState(
                                    account = SettingsBoardFixture.account.takeUnless { state == "AccountMissing" },
                                    loaded = true,
                                ),
                            ),
                            {},
                        )
                    }

                    state.startsWith("Profile") -> {
                        val account = SettingsBoardFixture.account
                        val editor =
                            SettingsProfileState(
                                account.name,
                                account.email,
                                saved = state == "ProfileSaved",
                                error = ProfileError.CouldNotSave.takeIf { state == "ProfileError" },
                            )
                        val field = remember { TextFieldState(account.name) }
                        ProfileScreen(
                            ProfileScreenState(
                                ProfileUiState(SettingsPagesState(account = account, loaded = true), editor),
                                field,
                            ),
                            {
                            },
                        )
                    }

                    state.startsWith("Language") -> {
                        LanguageScreen(
                            LanguageUiState(
                                choice =
                                    when (state) {
                                        "LanguageEn" -> LanguageChoice.En
                                        "LanguageNl" -> LanguageChoice.Nl
                                        else -> LanguageChoice.System
                                    },
                            ),
                            {},
                        )
                    }

                    state == "SyncStates" -> {
                        SettingsPage(R.string.settings_settings, null, "settings.states") {
                            SettingsSyncLine(SettingsBoardFixture.home, false, false, clock, ZoneOffset.UTC, null)
                            SettingsSyncLine(
                                SettingsBoardFixture.home.copy(pendingCount = 2),
                                false,
                                false,
                                clock,
                                ZoneOffset.UTC,
                                null,
                            )
                            SettingsSyncLine(SettingsBoardFixture.home, true, false, clock, ZoneOffset.UTC, null)
                            SettingsSyncLine(SettingsBoardFixture.home, false, true, clock, ZoneOffset.UTC, null)
                            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
                                CfCard(Modifier.testTag("settings.disabled.card"), rows = true) {
                                    SettingsDestinationRow(
                                        stringResource(
                                            R.string.settings_add_a_passkey,
                                        ),
                                        LucideGlyphs.KeyRound,
                                        "settings.disabled",
                                        {
                                        },
                                        enabled = false,
                                    )
                                }
                                SettingsNeedsConnection()
                            }
                        }
                    }

                    else -> {
                        SettingsPage(R.string.settings_account, {}, "settings.states") {
                            SettingsLoadingRows()
                            if (state == "StoredStates") SettingsStoredState(true, false, {})
                        }
                    }
                }
            }
        }
    }
}
