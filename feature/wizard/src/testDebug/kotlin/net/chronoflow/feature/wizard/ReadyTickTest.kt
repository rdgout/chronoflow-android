@file:OptIn(androidx.compose.runtime.InternalComposeTracingApi::class)

package net.chronoflow.feature.wizard

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.ready.ReadyButtonLabel
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import net.chronoflow.feature.wizard.ready.WizardReadyStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ReadyTickTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun ticksStopWhenTheStepLeavesComposition() {
        val visible = mutableStateOf(true)
        val events = mutableListOf<WizardEvent>()
        val ui = readyScreen().ready!!
        compose.mainClock.autoAdvance = false
        compose.setContent { WizardTestHost { if (visible.value) WizardReadyStep(ui, null, events::add) } }
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertTrue(events.count { it == WizardEvent.ReadyTick } in 3..4) }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("wizard.ready").assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        var before = 0
        compose.runOnIdle { before = events.size }
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals(before, events.size) }
    }

    @Test fun countdownDoesNotRecomposeRows() {
        val initial = readyScreen()

        fun countdown(seconds: Int) =
            initial.copy(
                ready =
                    initial.ready!!.copy(
                        footer = ReadyFooterUi.Primary(ReadyButtonLabel.Countdown(seconds), false, false),
                    ),
                chrome =
                    initial.chrome.copy(
                        primaryLabel = ReadyButtonLabel.Countdown(seconds).copy,
                        continueEnabled = false,
                        routeComplete = false,
                    ),
            )
        val state = mutableStateOf(countdown(42))
        var rows = 0
        val counts =
            object : CompositionTracer {
                override fun isTraceInProgress() = true

                override fun traceEventStart(
                    key: Int,
                    dirty1: Int,
                    dirty2: Int,
                    info: String,
                ) {
                    if (info.startsWith("net.chronoflow.feature.wizard.ready.ReadyRowsCard ")) rows++
                }

                override fun traceEventEnd() = Unit
            }
        Composer.setTracer(counts)
        try {
            compose.setContent { WizardTestHost { WizardScreen(state.value, {}) } }
            compose.runOnIdle {
                assertTrue(rows > 0)
                rows = 0
                state.value = countdown(41)
            }
            compose.runOnIdle { state.value = countdown(40) }
            compose.runOnIdle { assertEquals(0, rows) }
        } finally {
            Composer.setTracer(null)
        }
    }
}
