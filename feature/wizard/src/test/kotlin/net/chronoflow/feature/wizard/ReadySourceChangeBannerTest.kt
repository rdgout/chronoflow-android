package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ready.ReadyBannerUi
import net.chronoflow.feature.wizard.ready.ReadyUiMapper
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadySourceChangeBannerTest {
    private fun changed(
        pricePresent: Boolean = true,
        previousCost: Int = 1,
        fields: List<WizardPresentation.SourceChange.Field> =
            listOf(WizardPresentation.SourceChange.Field.returnFlight),
    ): TripWizardUiState.Active {
        val active = editReadyActive("EditChanged")
        val draft = active.wizard.draft
        val price =
            draft.state.review!!
                .price!!
                .copy(cost = if (fields.size == 2) 0 else 1)
        return active.copy(
            wizard =
                active.wizard.copy(
                    draft =
                        draft.copy(
                            state =
                                draft.state.copy(
                                    sourceChange =
                                        WizardPresentation.SourceChange(
                                            draft.form,
                                            fields,
                                            price.takeIf { pricePresent },
                                            price.copy(cost = previousCost),
                                        ),
                                ),
                        ),
                ),
        )
    }

    @Test fun sourceChangedUsesCatalogCopiesAndEnglishDate() {
        val banner = changed().ready().banner as ReadyBannerUi.SourceChanged
        assertEquals(
            WizardCopy(
                "On another device: the return flight moved to {{date}}.",
                mapOf("date" to "Thu 22 Oct"),
            ),
            banner.body.copies[0],
        )
        assertEquals(WizardCopy("{{count}} credits", mapOf("count" to "1")), banner.body.copies[1])
        assertEquals("The price is still {{price}}.", banner.body.copies[2].key)
    }

    @Test fun sourceChangedUsesTheDutchDate() {
        val banner = ReadyUiMapper.map(changed(), Language.Nl)!!.banner as ReadyBannerUi.SourceChanged
        assertEquals("do 22 okt", banner.body.copies[0].arguments["date"])
    }

    @Test fun saveFailureBeatsSourceChange() {
        val active = changed()
        val draft = active.wizard.draft
        val failed =
            active.copy(
                wizard =
                    active.wizard.copy(
                        draft =
                            draft.copy(
                                state =
                                    draft.state.copy(saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.offline)),
                            ),
                    ),
            )
        assertTrue(failed.ready().banner is ReadyBannerUi.SaveFailed)
    }

    @Test fun noPriceFallsThroughToAdjusted() {
        val active = changed(pricePresent = false)
        val draft = active.wizard.draft
        val modified =
            active.copy(
                wizard =
                    active.wizard.copy(
                        draft =
                            draft.copy(
                                state =
                                    draft.state.copy(review = draft.state.review!!.copy(status = PlanStatus.Modified)),
                            ),
                    ),
            )
        assertTrue(modified.ready().banner is ReadyBannerUi.Adjusted)
    }

    @Test fun noPriceFallsThroughToUpdated() {
        val active = changed(pricePresent = false)
        val draft = active.wizard.draft
        val updated =
            active.copy(
                wizard = active.wizard.copy(draft = draft.copy(state = draft.state.copy(updated = true))),
            )
        assertTrue(updated.ready().banner is ReadyBannerUi.Updated)
    }

    @Test fun changedCostUsesNow() {
        val banner = changed(previousCost = 0).ready().banner as ReadyBannerUi.SourceChanged
        assertEquals(
            "The price is now {{price}}.",
            banner.body.copies
                .last()
                .key,
        )
    }

    @Test fun routeAndUsualSleepKeepTheirOrderAndFreePrice() {
        val banner =
            changed(
                previousCost = 0,
                fields =
                    listOf(
                        WizardPresentation.SourceChange.Field.route,
                        WizardPresentation.SourceChange.Field.sleep,
                    ),
            ).ready().banner as ReadyBannerUi.SourceChanged
        assertEquals(
            listOf(
                "On another device: the route changed to {{from}} → {{to}}.",
                "On another device: the trip now uses your usual sleep times.",
                "Free",
                "The price is still {{price}}.",
            ),
            banner.body.copies.map { it.key },
        )
    }
}
