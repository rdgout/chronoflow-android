package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.ready.WizardReadyStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import org.junit.Assert.assertEquals
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
class ReadyForegroundTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun onlyASecondStartSendsForeground() {
        val owner =
            object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
            }
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        val events = mutableListOf<WizardEvent>()
        val ui = readyScreen().ready!!
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    WizardReadyStep(ui, null, events::add)
                }
            }
        }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        compose.runOnIdle { assertEquals(0, events.count { it == WizardEvent.ReadyForeground }) }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        compose.runOnIdle { assertEquals(1, events.count { it == WizardEvent.ReadyForeground }) }
    }
}
