package net.chronoflow.feature.today

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.LocalTodayProbe
import net.chronoflow.feature.today.ui.TodayProbe.Kind
import net.chronoflow.feature.today.ui.TodayScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import net.chronoflow.core.ui.trip.NextStepUi as CardNextStepUi

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayInvalidationTest {
    private val profile = DeviceProfiles.pixel8.copy(name = "todayInvalidation", heightDp = 1600)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)
    private val probe = CountingTodayProbe()
    private val source = mutableStateOf(todayNow)
    private val clock = MinuteClock(source)

    private fun key(
        kind: Kind,
        id: String,
    ) = kind to id

    @Test fun aQuietMinuteRecomposesOnlyTheSixChangingLeaves() {
        lateinit var formatter: CopyLineFormatter
        lateinit var state: TodayUiState.Active
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            val original = TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active
            val panel = original.panel!!
            val next = panel.upcoming.first()
            val marked = panel.current.single()
            state =
                original.copy(
                    hero = original.hero.copy(next = CardNextStepUi(next.type, next.target, next.title)),
                    panel =
                        panel.copy(
                            current =
                                panel.current +
                                    listOf(
                                        marked.copy(blockId = "marked.done", mark = CheckStatus.Done),
                                        marked.copy(blockId = "marked.skipped", mark = CheckStatus.Skipped),
                                    ),
                        ),
                )
        }
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    CompositionLocalProvider(LocalTodayProbe provides probe) {
                        TodayScreen(state, clock, Language.En, formatter, TimeFormat.Value24, noTodayActions, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        val before = probe.snapshot()
        val structure =
            setOf(
                key(Kind.SCREEN, "today"),
                key(Kind.HERO, "hero"),
                key(Kind.PANEL, "panel"),
                key(Kind.ROW, "row0"),
                key(Kind.ROW, "row1"),
                key(Kind.ROW, "row2"),
                key(Kind.ROW, "row3"),
                key(Kind.ROW, "row4"),
                key(Kind.ROW, "marked.done"),
                key(Kind.ROW, "marked.skipped"),
            )
        assertTrue("the full fixture must be composed", before.keys.containsAll(structure))
        assertFalse(before.containsKey(key(Kind.LEAF, "row.marked.done")))
        assertFalse(before.containsKey(key(Kind.LEAF, "row.marked.skipped")))
        compose.runOnIdle { source.value = todayNow.plusSeconds(60) }
        compose.waitForIdle()
        val delta = probe.snapshot().mapValues { (key, value) -> value - (before[key] ?: 0) }.filterValues { it != 0 }
        assertEquals(
            mapOf(
                key(Kind.LEAF, "row.row0") to 1,
                key(Kind.LEAF, "row.row1") to 1,
                key(Kind.LEAF, "row.row2") to 1,
                key(Kind.LEAF, "row.row3") to 1,
                key(Kind.LEAF, "row.row4") to 1,
                key(Kind.LEAF, "next.hero") to 1,
            ),
            delta,
        )
    }

    @Test fun passingTheInstantIntoTheRowMakesTheProbeFailTheIsolationRule() {
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    PlainInstantRow(clock.now, probe)
                }
            }
        }
        compose.waitForIdle()
        val before = probe.snapshot().getValue(key(Kind.ROW, "plain"))
        compose.runOnIdle { source.value = todayNow.plusSeconds(60) }
        compose.waitForIdle()
        assertEquals(before + 1, probe.snapshot().getValue(key(Kind.ROW, "plain")))
    }
}

@Composable
private fun PlainInstantRow(
    now: Instant,
    probe: CountingTodayProbe,
) {
    SideEffect { probe.composed(Kind.ROW, "plain") }
    Text(now.toString())
}
