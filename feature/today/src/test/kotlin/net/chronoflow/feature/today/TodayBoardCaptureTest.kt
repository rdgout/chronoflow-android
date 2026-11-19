package net.chronoflow.feature.today

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayScreen
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class TodayBoardCaptureTest(
    private val board: String,
    private val language: Language,
    private val dark: Boolean,
) {
    private val profile = DeviceProfiles.pixel8.copy(widthDp = 390, heightDp = 844)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(
                    profile,
                    if (language ==
                        Language.Nl
                    ) {
                        AppLanguage.Nl
                    } else {
                        AppLanguage.En
                    },
                    dark,
                ),
            ).around(compose)

    @Test fun captureBoard() {
        lateinit var formatter: CopyLineFormatter
        lateinit var state: TodayUiState
        lateinit var row: TodayRowUi
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            val name =
                when (board) {
                    "TodayEmpty" -> "empty"
                    "TodayFuture" -> "future"
                    "TodayLoading" -> "loading"
                    "TodayCompleted" -> "completed"
                    else -> "active"
                }
            state = TodayBoardFixture.state(name, formatter, language)
            row = TodayBoardFixture.row(formatter, language)
        }
        val clock = MinuteClock(mutableStateOf(if (board == "TodayRowStates") TodayBoardFixture.rowNow else todayNow))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                    if (board == "TodayRowStates") {
                        TodayRows(row, clock, language, formatter)
                    } else {
                        val safe = LocalSafeInsets.current
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
                        val contentInsets = safe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        CfNavigationSuiteScaffold(CfNavLayout.Bar, items, 0, {
                        }, Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom))) {
                            CompositionLocalProvider(LocalSafeInsets provides contentInsets) {
                                TodayScreen(state, clock, language, formatter, TimeFormat.Value24, noTodayActions, {})
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(compose, "${board}_pixel8_${language.raw}_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): Collection<Array<Any>> {
            val boards =
                listOf("Today", "TodayEmpty", "TodayFuture", "TodayLoading", "TodayCompleted", "TodayRowStates")
            return boards.flatMap { board ->
                listOf(Language.En, Language.Nl).flatMap { lang ->
                    listOf(false, true).map { dark -> arrayOf<Any>(board, lang, dark) }
                }
            }
        }
    }
}
