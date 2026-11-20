package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.wizard.support.withCompletion
import net.chronoflow.core.datamock.MockWizardPrompts
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.mode
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.reviewInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestWizardModeTests {
    @Test fun guestFineTuneUsesOnlyTheDrawnRowsAndLeavesMemberRowsAvailable() =
        runTest(
            timeout = 60.seconds,
        ) {
            withCompletion {
                val input = model.reviewInput.input
                val prompts =
                    MockWizardPrompts.make(
                        input.copy(
                            `return` = input.outbound,
                        ),
                        JsonObject(
                            emptyMap(),
                        ),
                    )
                val draft =
                    model.draft.let {
                        it.copy(
                            state =
                                it.state.copy(
                                    mode = WizardMode.guest,
                                    review =
                                        review.copy(
                                            prompts = prompts,
                                        ),
                                ),
                        )
                    }
                val guest = model(draft)
                assertEquals(
                    setOf(
                        PromptKind.Room,
                        PromptKind.LatestWake,
                        PromptKind.SeatSide,
                        PromptKind.Cups,
                        PromptKind.LightDevice,
                        PromptKind.BlueBlockers,
                    ),
                    guest.optionalPrompts.map { it.kind }.toSet(),
                )
                val member = model(draft.copy(state = draft.state.copy(mode = WizardMode.member)))
                assertTrue(member.optionalPrompts.any { it.kind == PromptKind.LateCheckOut })
                assertTrue(member.optionalPrompts.any { it.kind == PromptKind.PreDepartureBusyHours })
                assertTrue(member.optionalPrompts.any { it.kind == PromptKind.FreeDayTimes })
            }
        }
}
