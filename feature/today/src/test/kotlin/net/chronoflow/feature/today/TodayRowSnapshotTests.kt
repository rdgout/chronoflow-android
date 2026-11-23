package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.clock.MinuteClock
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class TodayRowSnapshotTests(
    private val profile: DeviceProfile,
    private val language: Language,
    private val dark: Boolean,
) {
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

    @Test fun testRunningDoneAndSkippedRowsAsOnTheRowStatesBoard() {
        lateinit var formatter: CopyLineFormatter
        lateinit var row: TodayRowUi
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            row = TodayBoardFixture.row(formatter, language)
        }
        val clock = MinuteClock(mutableStateOf(TodayBoardFixture.rowNow))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) { TodayRows(row, clock, language, formatter) }
            }
        }
        Guards.assertAll(compose, profile, emptySet())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): Collection<Array<Any>> =
            listOf(DeviceProfiles.small, DeviceProfiles.largeFont).flatMap { p ->
                listOf(Language.En, Language.Nl).flatMap { lang ->
                    listOf(false, true).map { dark -> arrayOf<Any>(p, lang, dark) }
                }
            }
    }
}
