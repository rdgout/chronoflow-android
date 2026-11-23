@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WizardSleepBoundaryTest(
    private val bed: String,
    private val wake: String,
) {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun boundaryNightsPass() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = sleepDraft(bed, wake) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Sleep))
                runCurrent()
                assertEquals(
                    MemberWizardStep.checking,
                    fixture.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases() = listOf(arrayOf("23:00", "02:00"), arrayOf("20:00", "10:00"))
    }
}
