package net.chronoflow.core.ui.plan

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RefusedChangeTextTest {
    @get:Rule val compose = createComposeRule()

    private fun resolve(notice: SyncRefusalNotice): String? {
        var result: String? = null
        compose.setContent {
            val text = refusedChangeText(notice)
            SideEffect { result = text }
        }
        compose.waitForIdle()
        return result
    }

    @Test fun serverRefusalProducesTheDrawnMessage() {
        assertEquals(
            "Couldn't save \"Avoid bright light\" as done, so we undid it.",
            resolve(SyncRefusalNotice.StepDone(BlockType.AvoidLight)),
        )
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchStep() {
        assertEquals(
            "\"Vermijd fel licht\" kon niet als gedaan worden opgeslagen, dus hebben we dat teruggedraaid.",
            resolve(SyncRefusalNotice.StepDone(BlockType.AvoidLight)),
        )
    }

    @Test fun englishSleep() {
        assertEquals("Couldn't save your sleep times, so we undid the change.", resolve(SyncRefusalNotice.SleepTimes))
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchSleep() {
        assertEquals(
            "Je slaaptijden konden niet worden opgeslagen, dus hebben we de wijziging teruggedraaid.",
            resolve(SyncRefusalNotice.SleepTimes),
        )
    }

    @Test fun unknownStepHasNoText() {
        assertEquals(null, resolve(SyncRefusalNotice.StepDone(BlockType.Unknown("future"))))
    }
}
