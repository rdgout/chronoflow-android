@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.wizard.support.ReviewFixture
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.mockWizardSource
import net.chronoflow.core.data.wizard.support.reviewFixture
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.testing.wizard.EffectProbe
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.wizardAirport
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.beginChecking
import net.chronoflow.core.wizard.model.canSave
import net.chronoflow.core.wizard.model.checkingTitle
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.needsReadyReview
import net.chronoflow.core.wizard.model.readyTick
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.savePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class TripWizardActionsTests {
    class Actions {
        @Test fun editStartsAtCheckingReviewsImmediatelyAndUntouchedEditCannotSave() =
            runTest(
                timeout = 60.seconds,
            ) {
                val source = GenerationSource()
                val f = reviewFixture(source = source, effects = EffectProbe().effects())
                try {
                    val draft = f.store.startTrip("trip", TripWizardAction.edit, source)
                    assertEquals(MemberWizardStep.checking, draft.state.step)
                    assertFalse(draft.state.sleepNeeded)
                    assertFalse(draft.hasChanges)
                    assertEquals(true, draft.state.returnToReady)
                    assertNull(draft.form.identity)
                    assertEquals(draft, f.store.recover())
                    val m = f.model(draft)
                    m.resumeChecking()
                    awaitWizard { source.count("review") == 1 && f.clock.waiterCount >= 2 }
                    assertEquals(listOf("input", "input", "review"), source.operations)
                    assertEquals("Checking your changes", m.checkingTitle)
                    completeEditCheck(f, m)
                    assertFalse(m.hasChanges)
                    assertFalse(m.canSave)
                    m.savePlan()
                    assertEquals(0, source.count("update"))
                    assertEquals(0, source.count("save"))
                    m.discard()
                    assertNull(f.store.recover())
                } finally {
                    f.close()
                }
            }

        @Test fun changedSourceReloadsInputAirportsBaselineAndRevisionBeforeUpdate() =
            runTest(
                timeout = 60.seconds,
            ) {
                val source = GenerationSource()
                val initial = generationInput().copy(preparationDays = 3)
                val original = ApiTripInputSnapshot(initial, "one", emptyMap(), null)
                source.queueSnapshots(listOf(original, original))
                val f = reviewFixture(source = source, effects = EffectProbe().effects())
                try {
                    val draft = f.store.startTrip("trip", TripWizardAction.edit, source)
                    val m = f.model(draft)
                    m.resumeChecking()
                    awaitWizard { source.count("review") == 1 && f.clock.waiterCount >= 2 }
                    completeEditCheck(f, m)
                    m.editForm { it.copy(preparation = it.preparation.copy(days = 1)) }
                    assertTrue(m.hasChanges)
                    val changedLegs =
                        initial.outbound.legs.mapIndexed { index, leg ->
                            if (index == 0) leg.copy(flightNumber = "NEW123") else leg
                        }
                    val changed =
                        initial.copy(
                            preparationDays = 2,
                            outbound = initial.outbound.copy(legs = changedLegs),
                        )
                    val airport = wizardAirport("HND", "Tokio", "Asia/Tokyo")
                    val updated = ApiTripInputSnapshot(changed, "two", mapOf("HND" to airport), null)
                    source.queueSnapshots(listOf(updated))
                    m.readyTick(true)
                    awaitWizard {
                        !m.reviewing && m.draft.state.edit
                            ?.sourceRevision == "two"
                    }
                    assertEquals(changed, m.reviewInput.input)
                    assertEquals(airport, m.draft.airports["HND"])
                    assertEquals(
                        "NEW123",
                        m.draft.form.legs[0]
                            .flightNumber,
                    )
                    assertFalse(m.hasChanges)
                    assertFalse(m.canSave)
                    assertEquals(
                        true,
                        m.draft.state.sourceChange?.fields?.contains(
                            WizardPresentation.SourceChange.Field.flight,
                        ),
                    )
                    assertNull(source.reviewInputs.last().identity)
                    m.flush()
                    val recovered = requireNotNull(f.store.recover())
                    assertEquals(m.draft.state.edit, recovered.state.edit)
                    assertEquals(m.draft.state.sourceChange, recovered.state.sourceChange)
                    assertFalse(recovered.hasChanges)
                    source.queueSnapshots(listOf(updated))
                    m.editForm { it.copy(preparation = it.preparation.copy(days = 1)) }
                    m.readyTick()
                    awaitWizard { !m.reviewing && !m.needsReadyReview }
                    assertTrue(m.canSave)
                    assertTrue(m.hasChanges)
                    assertNull(m.draft.state.sourceChange)
                    m.savePlan()
                    awaitWizard { m.createdTripID == "trip" }
                    assertEquals(1, source.count("update"))
                    assertEquals(0, source.count("save"))
                    assertEquals(TripEditContext("trip", "two"), source.lastEdit)
                } finally {
                    f.close()
                }
            }

        @Test fun duplicateStartsOutboundAndSavesFreshWithoutEditingOriginalAgainstMock() =
            runTest(
                timeout = 60.seconds,
            ) {
                val mock =
                    mockWizardSource(
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                        Instant.ofEpochSecond(
                            1_793_750_400,
                        ),
                    )
                val trip = mock.fetchTrips().first()
                val original = mock.tripInput(trip.card.id)
                val f = reviewFixture(source = GenerationSource(mock), effects = EffectProbe().effects())
                try {
                    val draft = f.store.startTrip(trip.card.id, TripWizardAction.duplicate, mock)
                    assertEquals(MemberWizardStep.outbound, draft.state.step)
                    assertNull(draft.state.edit)
                    assertNull(draft.form.identity)
                    assertNotNull(draft.state.copiedRoute)
                    assertTrue(draft.hasChanges)
                    assertFalse(draft.state.firstTrip)
                    assertTrue(draft.form.legs.all { it.departLocal.isEmpty() && it.arriveLocal.isEmpty() })
                    val home =
                        original.input.`return`
                            ?.legs
                            .orEmpty()
                    val originalLegs = original.input.outbound.legs + home
                    val dated =
                        draft.form.copy(
                            legs =
                                draft.form.legs.mapIndexed { index, leg ->
                                    leg.copy(
                                        departLocal =
                                            WizardDateTime.string(
                                                requireNotNull(
                                                    WizardDateTime.date(
                                                        originalLegs[index].departLocal,
                                                    ),
                                                ).plusDays(
                                                    30,
                                                ),
                                            ),
                                        arriveLocal =
                                            WizardDateTime.string(
                                                requireNotNull(
                                                    WizardDateTime.date(
                                                        originalLegs[index].arriveLocal,
                                                    ),
                                                ).plusDays(
                                                    30,
                                                ),
                                            ),
                                    )
                                },
                        )
                    val answered =
                        draft.copy(
                            form = dated,
                            state =
                                draft.state.copy(
                                    sleepNeeded = false,
                                    profile =
                                        JsonObject(
                                            mapOf(
                                                "age_band" to
                                                    JsonPrimitive(
                                                        "18_64",
                                                    ),
                                                "shift_work" to
                                                    JsonPrimitive(
                                                        false,
                                                    ),
                                            ),
                                        ),
                                ),
                        )
                    val m = f.model(answered)
                    assertNull(m.reviewInput.identity)
                    val reviewed = f.generation.review("duplicate", m.reviewInput)
                    f.source.queueReviews(listOf(Result.success(reviewed.review)))
                    m.beginChecking()
                    awaitWizard { f.source.count("review") == 2 && f.clock.waiterCount >= 2 }
                    f.clock.advance(1200)
                    awaitWizard { 1500L in f.clock.deadlines }
                    f.clock.advance(1500)
                    awaitWizard { m.draft.state.step != MemberWizardStep.checking }
                    if (m.draft.state.step == MemberWizardStep.fineTune) m.finishFineTune()
                    assertEquals(MemberWizardStep.ready, m.draft.state.step)
                    assertTrue(m.canSave)
                    m.savePlan()
                    awaitWizard { m.createdTripID != null }
                    val created = requireNotNull(m.createdTripID)
                    assertTrue(created != trip.card.id)
                    assertEquals(original, mock.tripInput(trip.card.id))
                    assertEquals(m.reviewInput.input, mock.tripInput(created).input)
                } finally {
                    f.close()
                }
            }
    }

    @RunWith(Parameterized::class)
    class SaveFailure(
        private val failure: DataFailure,
    ) {
        @Test fun editSaveFailureKeepsChangesAndRetryUpdatesTheSameTrip() =
            runTest(timeout = 60.seconds) {
                val source = GenerationSource()
                val f = reviewFixture(source = source, effects = EffectProbe().effects())
                try {
                    val draft = f.store.startTrip("trip", TripWizardAction.edit, source)
                    val m = f.model(draft)
                    m.editForm { it.copy(preparation = it.preparation.copy(days = 1)) }
                    m.resumeChecking()
                    awaitWizard { source.count("review") == 1 && f.clock.waiterCount >= 2 }
                    completeEditCheck(f, m)
                    source.configure(saveFailure = failure)
                    m.savePlan()
                    awaitWizard { m.draft.state.saveFailure != null }
                    assertEquals(MemberWizardStep.ready, m.draft.state.step)
                    assertTrue(m.hasChanges)
                    assertNull(m.createdTripID)
                    val kind =
                        if (failure == DataFailure.Offline) {
                            WizardSaveFailure.Kind.offline
                        } else {
                            WizardSaveFailure.Kind.server
                        }
                    assertEquals(
                        kind,
                        m.draft.state.saveFailure
                            ?.kind,
                    )
                    assertEquals(0, source.count("save"))
                    source.configure()
                    m.savePlan()
                    awaitWizard { m.createdTripID == "trip" }
                    assertEquals(2, source.count("update"))
                } finally {
                    f.close()
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "{0}",
            )
            fun failures(): List<Array<Any>> =
                listOf(
                    DataFailure.Offline,
                    DataFailure.Server(
                        500,
                    ),
                ).map {
                    arrayOf(
                        it,
                    )
                }
        }
    }
}

private suspend fun completeEditCheck(
    f: ReviewFixture,
    model: MemberWizardModel,
) {
    f.clock.advance(1200)
    awaitWizard { 1500L in f.clock.deadlines }
    f.clock.advance(1500)
    awaitWizard { model.draft.state.step == MemberWizardStep.ready }
}
