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
class WizardSleepCompletenessTest(
    private val missing: String,
) {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun continueIsOffUntilBedWakeAndChronotypeExist() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = sleepDraft()
                draft = base.copy(state = base.state.copy(profile = JsonObject(base.state.profile - missing)))
            }) { fixture ->
                runCurrent()
                assertFalse(
                    fixture.model.uiState.value!!
                        .chrome.continueEnabled,
                )
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "missing-{0}")
        fun cases() = listOf("habitual_bed", "habitual_wake", "chronotype")
    }
}
