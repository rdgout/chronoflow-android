@file:Suppress("ktlint:standard:max-line-length")

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardRoot
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardSavedExitTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(ProfileRule(DeviceProfiles.pixel8))
            .around(WizardMainDispatcherRule())
            .around(compose)

    @Test fun savedExitReplaysAfterRecreationWithoutASecondSuccessHaptic() =
        runTest(timeout = 60.seconds) {
            val deps = FakeWizardDependencies(this).apply { draft = checkedDraft(MemberWizardStep.fineTune) }
            val fixture = WizardViewModelFixture(deps, WizardMode.Create)
            val exits = mutableListOf<WizardExit>()
            val haptics = RecordingHaptics()
            val incarnation = mutableIntStateOf(0)
            try {
                runCurrent()
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
                runCurrent()
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertEquals(WizardExit.Saved("created"), fixture.model.exit.value)
                compose.setContent {
                    WizardTestHost {
                        CompositionLocalProvider(LocalHaptics provides haptics) {
                            key(incarnation.intValue) { WizardRoot(fixture.model, exits::add) }
                        }
                    }
                }
                compose.runOnIdle {
                    assertEquals(listOf(WizardExit.Saved("created")), exits)
                    incarnation.intValue++
                }
                compose.runOnIdle {
                    assertEquals(
                        listOf(WizardExit.Saved("created"), WizardExit.Saved("created")),
                        exits,
                    )
                }
                assertEquals(listOf(HapticEvent.Success), haptics.events)
            } finally {
                withContext(NonCancellable) {
                    fixture.clear()
                    deps.close()
                }
            }
        }
}
