@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.ready.ReadyRow
import net.chronoflow.feature.wizard.ready.ReadyUiMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadyRowsUiTest {
    @Test fun memberRowsIncludeReturnChoiceEvenForOneWay() {
        assertEquals(
            listOf(
                ReadyRow.Route,
                ReadyRow.Flight,
                ReadyRow.Return,
                ReadyRow.Sleep,
                ReadyRow.Extras,
            ),
            readyActive().ready().rows.map {
                it.kind
            },
        )
    }

    @Test fun latestWakeRequiresModifiedAndAnOptionalPrompt() {
        val review =
            generationReview().copy(
                status = PlanStatus.Modified,
                prompts = listOf(completionPrompt(PromptKind.LatestWake)),
            )
        assertEquals(
            ReadyRow.LatestWake,
            readyActive(review)
                .ready()
                .rows
                .last()
                .kind,
        )
        assertEquals(
            ReadyRow.Extras,
            readyActive(review.copy(prompts = emptyList()))
                .ready()
                .rows
                .last()
                .kind,
        )
    }

    @Test fun returnIsNotShownForGuest() {
        val base = readyActive()
        val draft = base.wizard.draft
        assertFalse(
            base
                .copy(
                    wizard = base.wizard.copy(draft = draft.copy(state = draft.state.copy(mode = WizardMode.guest))),
                ).ready()
                .rows
                .any {
                    it.kind ==
                        ReadyRow.Return
                },
        )
    }

    @Test fun routeStopsAtTheOutboundDestination() {
        val form = routeDraft(home = true).form
        assertEquals("AMS → NRT", ReadyUiMapper.routeSummary(form))
    }

    @Test fun sleepUsesProfileBeforeSavedProfileAndOmitsMissingDash() {
        val base = fineTuneDraft()
        val draft =
            base.copy(
                state =
                    base.state.copy(
                        profile =
                            JsonObject(
                                mapOf("habitual_bed" to JsonPrimitive("23:30")),
                            ),
                        savedProfile =
                            JsonObject(
                                mapOf(
                                    "habitualBed" to JsonPrimitive("22:00"),
                                    "habitualWake" to JsonPrimitive("07:15"),
                                ),
                            ),
                    ),
            )
        assertEquals("23:30–07:15", ReadyUiMapper.sleepSummary(draft).pattern)
        assertEquals(
            "23:30",
            ReadyUiMapper.sleepSummary(draft.copy(state = draft.state.copy(savedProfile = null))).pattern,
        )
        val override =
            draft.copy(
                form =
                    draft.form.copy(
                        override =
                            draft.form.override.copy(
                                enabled = true,
                                habitualBed = "00:30",
                                habitualWake = "08:30",
                            ),
                    ),
            )
        assertEquals(
            "00:30–08:30",
            ReadyUiMapper
                .sleepSummary(override)
                .copies
                .single()
                .arguments["times"],
        )
    }

    @Test fun extrasRetainAllCatalogCallsAndReuseIdenticalKeys() {
        val value =
            readyActive()
                .ready()
                .rows
                .last()
                .value
        assertTrue(value.copies.any { it.key == "{{count}} days prep" })
        assertFalse(value.pattern.contains("days prep"))
    }
}
