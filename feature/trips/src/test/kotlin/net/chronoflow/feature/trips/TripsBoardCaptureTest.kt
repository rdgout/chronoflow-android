package net.chronoflow.feature.trips

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
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
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.core.ui.R
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TripsBoardCaptureTest(
    private val board: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, dark = dark),
            ).around(compose)

    @Test fun capturesTheBoardState() {
        var state: TripsUiState = TripsUiState.Loading
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) {
            formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            state =
                when (board) {
                    "Trips" -> {
                        TripsBoardFixture.content(
                            formatter,
                            Language.En,
                        )
                    }

                    "TripsEmpty" -> {
                        TripsUiState.Empty
                    }

                    else -> {
                        TripsUiState.Loading
                    }
                }
        }
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
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
                    CfNavigationSuiteScaffold(
                        CfNavLayout.Bar,
                        items,
                        1,
                        {},
                        Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom)),
                    ) {
                        CompositionLocalProvider(
                            LocalSafeInsets provides safe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        ) {
                            TripsContent(
                                state,
                                RefreshFeedbackState(),
                                Language.En,
                                {},
                                {},
                                {},
                                {},
                                formatter = formatter,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(compose, "${board}_en_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("Trips", false),
                arrayOf("Trips", true),
                arrayOf("TripsEmpty", false),
                arrayOf("TripsEmpty", true),
                arrayOf("TripsLoading", false),
                arrayOf("TripsLoading", true),
            )
    }
}
