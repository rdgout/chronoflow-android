package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.sleepComplete
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WizardSleepUiDerivationTest(
    private val chronotype: String?,
) {
    @Test fun sleepUiDerivation() =
        runTest(timeout = 60.seconds) {
            val base = sleepDraft()
            val profile =
                JsonObject(
                    (base.state.profile - "chronotype") +
                        when (chronotype) {
                            null -> emptyMap()
                            "unsure" -> mapOf("chronotype" to JsonNull)
                            else -> mapOf("chronotype" to JsonPrimitive(chronotype))
                        },
                )
            val draft =
                base.copy(
                    state = base.state.copy(profile = profile),
                    fieldErrors =
                        mapOf(
                            "profile.habitual_bed" to FieldMessage("bed"),
                            "review" to FieldMessage("review"),
                        ),
                )
            val model =
                MemberWizardModel(
                    draft,
                    InMemoryWizardDraftStore(),
                    backgroundScope,
                    { Instant.EPOCH },
                )
            try {
                val active =
                    TripWizardUiState.Active(
                        model.snapshots.value,
                        model.progress,
                        model.navigationPath,
                        sleepComplete = model.sleepComplete,
                    )
                val ui = WizardUiState.from(active)
                assertEquals("On a work day. We save it for next time.", ui.sleep!!.subtitle.key)
                assertEquals("23:30", ui.sleep.bed)
                assertEquals("07:15", ui.sleep.wake)
                assertEquals(chronotype, ui.sleep.selectedChronotype)
                assertEquals(setOf("profile.habitual_bed"), ui.sleep.errors.keys)
                assertEquals(.5f, ui.chrome.progress)
                assertEquals(chronotype != null, ui.chrome.continueEnabled)
                val guest = draft.copy(state = draft.state.copy(mode = MemberWizardDrafts.guest().state.mode))
                val guestUi = WizardUiState.from(active.copy(wizard = active.wizard.copy(draft = guest)))
                assertEquals("On a normal work day.", guestUi.sleep!!.subtitle.key)
            } finally {
                model.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "chronotype-{0}")
        fun cases(): List<String?> = listOf("early", "intermediate", "late", "unsure", null)
    }
}
