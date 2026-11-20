@file:OptIn(androidx.compose.runtime.InternalComposeTracingApi::class)

package net.chronoflow.feature.wizard

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.checking.CheckLineState
import net.chronoflow.feature.wizard.checking.WizardCheckingStep
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
class WizardCheckingRecompositionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private class Counts : CompositionTracer {
        var arcs = 0
        var lines = 0

        override fun isTraceInProgress() = true

        override fun traceEventStart(
            key: Int,
            dirty1: Int,
            dirty2: Int,
            info: String,
        ) {
            if (info.startsWith("net.chronoflow.feature.wizard.checking.WizardCheckingArc ")) arcs++
            if (info.startsWith("net.chronoflow.feature.wizard.checking.WizardCheckLine ")) lines++
        }

        override fun traceEventEnd() = Unit
    }

    @Test fun animationAndTicksDoNotRecomposeTheArc() {
        val initial = completionBoard("WizardChecking").checking!!
        val state =
            mutableStateOf(
                initial.copy(
                    lines =
                        initial.lines.mapIndexed { i, line ->
                            line.first to
                                if (i == 0) {
                                    CheckLineState.Done
                                } else if (i == 1) {
                                    CheckLineState.Busy
                                } else {
                                    CheckLineState.Open
                                }
                        },
                ),
            )
        val counts = Counts()
        Composer.setTracer(counts)
        try {
            compose.setContent {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(darkTheme = false, reduceMotion = false) { WizardCheckingStep(state.value) }
                }
            }
            compose.runOnIdle {
                assertTrue("The tracer must observe the production arc", counts.arcs > 0)
                assertTrue("The tracer must observe all production lines", counts.lines >= 3)
                counts.arcs = 0
                counts.lines = 0
            }
            compose.mainClock.advanceTimeBy(2000)
            compose.runOnIdle {
                assertEquals(0, counts.arcs)
                assertEquals(0, counts.lines)
                state.value = initial
            }
            compose.runOnIdle {
                assertEquals(0, counts.arcs)
                assertEquals(2, counts.lines)
            }
        } finally {
            Composer.setTracer(null)
        }
    }
}
