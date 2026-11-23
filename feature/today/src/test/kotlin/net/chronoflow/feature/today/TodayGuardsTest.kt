package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayScreen
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
class TodayGuardsTest(
    private val case: LayoutCase,
    private val stateName: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun stateFitsTheProfileAndSafeArea() {
        val language = if (case.language == AppLanguage.Nl) Language.Nl else Language.En
        lateinit var formatter: CopyLineFormatter
        lateinit var state: TodayUiState
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            state = TodayBoardFixture.state(stateName, formatter, language)
        }
        val clock = MinuteClock(mutableStateOf(todayNow))
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayShellFixture {
                        TodayScreen(state, clock, language, formatter, TimeFormat.Value24, noTodayActions, {})
                    }
                }
            }
        }
        Guards.assertAll(
            compose,
            case.profile,
            emptySet(),
            inkChannelDifference =
                if (stateName ==
                    "loading"
                ) {
                    2
                } else {
                    16
                },
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): Collection<Array<Any>> =
            LayoutMatrix.phone.flatMap { case ->
                listOf("active", "empty", "future", "loading", "completed").map { state -> arrayOf<Any>(case, state) }
            }
    }
}
