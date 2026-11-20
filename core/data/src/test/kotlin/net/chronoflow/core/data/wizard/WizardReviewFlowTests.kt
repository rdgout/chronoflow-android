@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.support.agePrompt
import net.chronoflow.core.data.wizard.support.awaitWizard
import net.chronoflow.core.data.wizard.support.finishCheck
import net.chronoflow.core.data.wizard.support.reviewFixture
import net.chronoflow.core.data.wizard.support.screeningPrompt
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockWizardPrompts
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.testing.wizard.EffectProbe
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.expiredIdentity
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.testing.wizard.wizardAirport
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.answerQuestion
import net.chronoflow.core.wizard.model.backFromQuestion
import net.chronoflow.core.wizard.model.beginChecking
import net.chronoflow.core.wizard.model.canSave
import net.chronoflow.core.wizard.model.changeExcludedAnswers
import net.chronoflow.core.wizard.model.chooseReturn
import net.chronoflow.core.wizard.model.currentQuestion
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.finishFlightInput
import net.chronoflow.core.wizard.model.finishRoute
import net.chronoflow.core.wizard.model.finishSleep
import net.chronoflow.core.wizard.model.from
import net.chronoflow.core.wizard.model.lastInputStep
import net.chronoflow.core.wizard.model.navigateToQuestion
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.popNavigation
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.readyTick
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.resumeCompletion
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.route
import net.chronoflow.core.wizard.model.savePlan
import net.chronoflow.core.wizard.model.setSleep
import net.chronoflow.core.wizard.model.sleepComplete
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardReviewFlowTests {
    class Flow {
        @Test fun stopOneWayUnsureSleepAndScreeningReachFineTuneThenSave() =
            runTest(timeout = 60.seconds) {
                val probe = EffectProbe()
                val f = reviewFixture(sleepNeeded = true, effects = probe.effects())
                try {
                    val m = f.model
                    m.go(MemberWizardStep.whereTo)
                    m.chooseAirport(
                        wizardAirport(
                            "AMS",
                            "Amsterdam",
                            "Europe/Amsterdam",
                        ),
                        WizardAirportSheet(
                            0,
                            WizardAirportSheet.Side.from,
                        ),
                    )
                    m.chooseAirport(
                        wizardAirport(
                            "HND",
                            "Tokyo",
                            "Asia/Tokyo",
                        ),
                        WizardAirportSheet(
                            0,
                            WizardAirportSheet.Side.to,
                        ),
                    )
                    m.chooseAirport(
                        wizardAirport(
                            "DXB",
                            "Dubai",
                            "Asia/Dubai",
                        ),
                        WizardAirportSheet(
                            0,
                            WizardAirportSheet.Side.stop,
                        ),
                    )
                    assertEquals(
                        listOf("AMS", "DXB", "HND"),
                        m.draft.form
                            .route()
                            .map { it.iata },
                    )
                    m.finishRoute()
                    assertEquals(MemberWizardStep.outbound, m.draft.state.step)
                    m.setDateTime("2030-10-12T14:40", 0, false)
                    m.setDateTime("2030-10-12T23:25", 0, true)
                    m.setDateTime("2030-10-13T02:05", 1, false)
                    m.setDateTime("2030-10-13T16:35", 1, true)
                    assertTrue(m.finishOutbound())
                    m.go(MemberWizardStep.returnChoice)
                    m.chooseReturn("one_way")
                    awaitWizard { 250L in f.clock.deadlines }
                    f.clock.advance(250)
                    assertEquals(MemberWizardStep.sleep, m.draft.state.step)
                    assertEquals("one_way", m.draft.form.end.kind)
                    assertEquals(2, m.draft.form.legs.size)
                    m.setSleep("habitual_bed", JsonPrimitive("23:30"))
                    m.setSleep("habitual_wake", JsonPrimitive("07:15"))
                    m.setSleep("chronotype", JsonNull)
                    val first = generationReview().copy(prompts = listOf(agePrompt()))
                    val final =
                        generationReview().copy(
                            prompts =
                                MockWizardPrompts.make(
                                    m.reviewInput.input,
                                    JsonObject(
                                        emptyMap(),
                                    ),
                                ),
                        )
                    f.source.queueReviews(listOf(Result.success(first), Result.success(final)))
                    assertTrue(m.finishSleep())
                    finishCheck(f, start = 250)
                    assertEquals(MemberWizardStep.question, m.draft.state.step)
                    m.answerQuestion(JsonPrimitive("18_64"))
                    awaitWizard { 2000L in f.clock.deadlines }
                    f.clock.advance(2000)
                    finishCheck(f, count = 2, start = 2000)
                    assertEquals(MemberWizardStep.fineTune, m.draft.state.step)
                    assertTrue(m.optionalPrompts.isNotEmpty())
                    val reviewed = f.source.reviewInputs.last()
                    assertEquals(2, reviewed.input.outbound.legs.size)
                    assertEquals("one_way", reviewed.input.tripEndKind)
                    assertEquals(JsonNull, reviewed.profile?.get("chronotype"))
                    assertEquals(JsonPrimitive("18_64"), reviewed.profile?.get("age_band"))
                    m.finishFineTune()
                    assertEquals(MemberWizardStep.ready, m.draft.state.step)
                    assertEquals(2, f.source.count("review"))
                    m.savePlan()
                    awaitWizard { m.createdTripID == "created" }
                    assertEquals(1, f.source.count("save"))
                    assertEquals(listOf("created"), probe.pulls)
                    assertNull(f.store.recover())
                } finally {
                    f.close()
                }
            }

        @Test fun sleepIsRequiredUntilAnsweredAndNotSureCounts() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture(sleepNeeded = true)
                try {
                    val m = f.model
                    m.finishFlightInput()
                    assertEquals(MemberWizardStep.sleep, m.draft.state.step)
                    m.beginChecking()
                    assertEquals(MemberWizardStep.sleep, m.draft.state.step)
                    assertEquals(0, f.source.count("review"))
                    m.setSleep("habitual_bed", JsonPrimitive("23:30"))
                    m.setSleep("habitual_wake", JsonPrimitive("07:15"))
                    assertFalse(m.sleepComplete)
                    m.setSleep("chronotype", JsonNull)
                    assertTrue(m.sleepComplete)
                    assertTrue(m.finishSleep())
                    finishCheck(f)
                    assertEquals(MemberWizardStep.ready, m.draft.state.step)
                    assertFalse(m.draft.state.sleepNeeded)
                    assertEquals(MemberWizardStep.sleep, m.lastInputStep)
                    assertEquals(
                        JsonNull,
                        f.source.reviewInputs
                            .last()
                            .profile
                            ?.get("chronotype"),
                    )
                    val values = SettingsRepository(f.database).settings().associateBy { it.section }
                    assertEquals("true", values["sleepAnswered"]?.value)
                    val saved =
                        ApiJson.response
                            .parseToJsonElement(
                                values
                                    .getValue(
                                        "bodyClock",
                                    ).value,
                            ).jsonObject
                    assertEquals(JsonPrimitive("23:30"), saved["habitualBed"])
                    assertEquals(JsonPrimitive("07:15"), saved["habitualWake"])
                } finally {
                    f.close()
                }
            }

        @Test fun invalidSleepDurationStaysAtSleep() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture(sleepNeeded = true)
                try {
                    val m = f.model
                    m.go(MemberWizardStep.sleep)
                    m.setSleep("habitual_bed", JsonPrimitive("23:30"))
                    m.setSleep("habitual_wake", JsonPrimitive("00:30"))
                    m.setSleep("chronotype", JsonPrimitive("early"))
                    assertFalse(m.finishSleep())
                    val error = m.draft.fieldErrors["profile.habitual_bed"]?.key
                    assertEquals("Check your bedtime and wake-up time.", error)
                    assertEquals(0, f.source.count("review"))
                } finally {
                    f.close()
                }
            }

        @Test fun failedRequestShowsOfflineAndManualRetryChecksAgain() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    f.source.configure(reviewFailure = DataFailure.Offline)
                    f.model.beginChecking()
                    awaitWizard { f.model.checking.failure == DataFailure.Offline }
                    assertEquals(MemberWizardStep.checking, f.model.draft.state.step)
                    assertEquals(1, f.source.count("review"))
                    f.source.configure()
                    f.model.beginChecking()
                    finishCheck(f, count = 2)
                    assertEquals(MemberWizardStep.ready, f.model.draft.state.step)
                } finally {
                    f.close()
                }
            }

        @Test fun repeatedReviewExpiredShowsFailureUntilManualRetry() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val m = f.model
                    val identity = expiredIdentity()
                    m.editForm { it.copy(identity = identity) }
                    f.source.configure(reviewFailure = DataFailure.ReviewExpired)
                    m.beginChecking()
                    awaitWizard { m.checking.failure == DataFailure.ReviewExpired && f.clock.waiterCount == 0 }
                    assertEquals(MemberWizardStep.checking, m.draft.state.step)
                    assertTrue(m.draft.fieldErrors.isEmpty())
                    assertEquals(0, m.checking.ticks)
                    assertEquals(listOf(identity, null), f.source.reviewInputs.map { it.identity })
                    m.resumeChecking()
                    m.resumeCompletion()
                    f.clock.advance(1_801_000)
                    m.readyTick(true)
                    m.resumeChecking()
                    runCurrent()
                    assertEquals(DataFailure.ReviewExpired, m.checking.failure)
                    assertEquals(2, f.source.count("review"))
                    f.source.configure()
                    m.beginChecking()
                    finishCheck(f, count = 3, start = 1_801_000)
                    assertEquals(MemberWizardStep.ready, m.draft.state.step)
                } finally {
                    f.close()
                }
            }

        @Test fun activeCheckingCannotBeRestartedByDuplicateStarts() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                val gate = GenerationGate()
                try {
                    f.source.configure(beforeReview = { gate.wait() })
                    f.model.beginChecking()
                    awaitWizard { f.source.count("review") == 1 && 400L in f.clock.deadlines }
                    f.clock.advance(400)
                    assertEquals(1, f.model.checking.ticks)
                    f.model.beginChecking()
                    f.model.resumeChecking()
                    assertEquals(1, f.model.checking.ticks)
                    gate.open()
                    finishCheck(f)
                    assertEquals(MemberWizardStep.ready, f.model.draft.state.step)
                    assertEquals(1, f.source.count("review"))
                } finally {
                    gate.open()
                    f.close()
                }
            }

        @Test fun needsAnswersWithoutSupportedPromptsIsAServerFailure() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val review =
                        generationReview().copy(
                            status = PlanStatus.NeedsAnswers,
                            price = null,
                            summary = null,
                            canSave = false,
                            prompts =
                                emptyList(),
                        )
                    f.source.queueReviews(listOf(Result.success(review)))
                    f.model.beginChecking()
                    finishCheck(f)
                    assertEquals(DataFailure.Server(null), f.model.checking.failure)
                    assertTrue(
                        f.model.draft.state.step !in
                            listOf(
                                MemberWizardStep.excluded,
                                MemberWizardStep.ready,
                            ),
                    )
                } finally {
                    f.close()
                }
            }

        @Test fun needsAnswersWithSupportedPromptsAsksThem() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val review =
                        generationReview().copy(
                            status = PlanStatus.NeedsAnswers,
                            price = null,
                            summary = null,
                            canSave = false,
                            prompts =
                                listOf(
                                    screeningPrompt(
                                        "shift_work",
                                    ),
                                ),
                        )
                    f.source.queueReviews(listOf(Result.success(review)))
                    f.model.beginChecking()
                    finishCheck(f)
                    assertEquals(MemberWizardStep.question, f.model.draft.state.step)
                    assertEquals(
                        1,
                        f.model.draft.state.questions
                            ?.size,
                    )
                } finally {
                    f.close()
                }
            }

        @Test fun unknownMockAirportReturnsToWhereBeforeFirstCheckingTick() =
            runTest(timeout = 60.seconds) {
                val now = Instant.ofEpochSecond(1_793_750_400)
                val seed = MockAppSeed.make(now, zone = ZoneOffset.UTC)
                val account = seed.accounts.values.first { it.twoFactorCode == null }
                val mock =
                    MockDataSource(
                        seed = seed,
                        now = { now },
                        sleep = {},
                        zone = ZoneOffset.UTC,
                        io =
                            StandardTestDispatcher(
                                testScheduler,
                            ),
                    )
                mock.signIn(
                    PasswordSignInInput(
                        account.snapshot.email,
                        account.password,
                        "019a562b-8000-7000-8000-000000000099",
                        "Tests",
                    ),
                )
                val source = GenerationSource(mock)
                val f = reviewFixture(source = source)
                try {
                    val m = f.model
                    m.chooseAirport(
                        wizardAirport(
                            "NRT",
                            "Tokyo",
                            "Asia/Tokyo",
                        ),
                        WizardAirportSheet(
                            0,
                            WizardAirportSheet.Side.to,
                        ),
                    )
                    m.setDateTime("2030-10-12T14:40", 0, false)
                    m.setDateTime("2030-10-13T16:35", 0, true)
                    m.go(MemberWizardStep.outbound)
                    assertTrue(m.finishOutbound())
                    m.go(MemberWizardStep.returnChoice)
                    m.beginChecking()
                    assertEquals(MemberWizardStep.checking, m.draft.state.step)
                    val checkingPath = m.navigationPath
                    awaitWizard({ "Airport step ${m.draft.state.step} at ${f.clock.now}" }) {
                        m.draft.state.step == MemberWizardStep.whereTo
                    }
                    assertTrue(m.navigationPath.isEmpty())
                    assertEquals(
                        mapOf(
                            "legs.0.toIata" to
                                FieldMessage(
                                    "Choose an airport from the search results.",
                                ),
                        ),
                        m.draft.fieldErrors,
                    )
                    assertNull(m.checking.failure)
                    assertEquals(0, m.checking.ticks)
                    assertEquals(0L, f.clock.now)
                    m.popNavigation(checkingPath.dropLast(1), checkingPath)
                    m.resumeChecking()
                    assertEquals(MemberWizardStep.whereTo, m.draft.state.step)
                    assertTrue(m.navigationPath.isEmpty())
                    assertEquals(1, source.count("review"))
                    m.flush()
                    val recovered = requireNotNull(f.store.recover())
                    assertEquals(MemberWizardStep.whereTo, recovered.state.step)
                    assertEquals(m.draft.fieldErrors, recovered.fieldErrors)
                } finally {
                    f.close()
                }
            }

        @Test fun staleQuestionStackCannotCancelCheckingAndCurrentBackStillWorks() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val review = generationReview().copy(prompts = listOf(screeningPrompt("shift_work")))
                    f.source.queueReviews(listOf(Result.success(review)))
                    f.model.beginChecking()
                    finishCheck(f)
                    val oldPath = f.model.navigationPath
                    f.source.configure(reviewFailure = DataFailure.Offline)
                    f.model.answerQuestion(JsonPrimitive(false))
                    awaitWizard { 1750L in f.clock.deadlines }
                    f.clock.advance(1750)
                    assertEquals(MemberWizardStep.checking, f.model.draft.state.step)
                    f.model.popNavigation(oldPath.dropLast(1), oldPath)
                    assertEquals(MemberWizardStep.checking, f.model.draft.state.step)
                    awaitWizard { f.model.checking.failure == DataFailure.Offline }
                    val path = f.model.navigationPath
                    f.model.popNavigation(path.dropLast(1), path)
                    assertEquals(MemberWizardStep.returnChoice, f.model.draft.state.step)
                } finally {
                    f.close()
                }
            }

        @Test fun throttleUsesServerDeadlineAndBlocksRetry() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    f.source.configure(reviewFailure = DataFailure.Throttled(2.4, 429))
                    f.model.beginChecking()
                    awaitWizard { f.model.retrySeconds == 3 }
                    f.model.beginChecking()
                    assertEquals(1, f.source.count("review"))
                    awaitWizard { 1000L in f.clock.deadlines }
                    f.clock.advance(1000)
                    assertEquals(2, f.model.retrySeconds)
                    awaitWizard { 2000L in f.clock.deadlines }
                    f.clock.advance(2000)
                    assertEquals(1, f.model.retrySeconds)
                    awaitWizard { 2400L in f.clock.deadlines }
                    f.clock.advance(2400)
                    assertEquals(0, f.model.retrySeconds)
                    assertEquals(1, f.source.count("review"))
                } finally {
                    f.close()
                }
            }

        @Test fun questionAnswersReReviewWithIdentityAndReachSeam() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val first = generationReview().copy(prompts = listOf(screeningPrompt("shift_work")))
                    f.source.queueReviews(listOf(Result.success(first), Result.success(generationReview())))
                    f.model.beginChecking()
                    finishCheck(f)
                    assertEquals(listOf("shift_work"), f.model.currentQuestion?.fields)
                    assertEquals(MemberWizardStep.question, f.model.draft.state.step)
                    val progress = f.model.progress
                    f.model.answerQuestion(JsonPrimitive(false))
                    f.model.answerQuestion(JsonPrimitive(true))
                    awaitWizard { 1750L in f.clock.deadlines }
                    assertTrue(f.model.choicePending)
                    assertEquals(JsonPrimitive(false), f.model.draft.state.profile["shift_work"])
                    f.clock.advance(1750)
                    assertEquals(MemberWizardStep.checking, f.model.draft.state.step)
                    assertTrue(f.model.progress >= progress)
                    finishCheck(f, count = 2, start = 1750)
                    assertEquals(MemberWizardStep.ready, f.model.draft.state.step)
                    val input = f.source.reviewInputs.last()
                    assertEquals(JsonPrimitive(false), input.profile?.get("shift_work"))
                    assertEquals(first.identity, input.identity)
                } finally {
                    f.close()
                }
            }

        @Test fun screeningExclusionCanClearAnswersAndReviewAgain() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val first = generationReview().copy(prompts = listOf(screeningPrompt("shift_work")))
                    val excluded =
                        first.copy(
                            status = PlanStatus.NoPlan,
                            prompts =
                                emptyList(),
                            reasons =
                                listOf(
                                    CopyLine(
                                        "Plans are only available for people aged 18 and over.",
                                        emptyList(),
                                    ),
                                ),
                        )
                    f.source.queueReviews(
                        listOf(
                            Result.success(
                                first,
                            ),
                            Result.success(
                                excluded,
                            ),
                            Result.success(
                                first,
                            ),
                        ),
                    )
                    f.model.beginChecking()
                    finishCheck(f)
                    f.model.answerQuestion(JsonPrimitive(true))
                    awaitWizard { 1750L in f.clock.deadlines }
                    f.clock.advance(1750)
                    finishCheck(f, count = 2, start = 1750)
                    assertEquals(MemberWizardStep.excluded, f.model.draft.state.step)
                    f.model.changeExcludedAnswers()
                    finishCheck(f, count = 3, start = 3250)
                    assertEquals(MemberWizardStep.question, f.model.draft.state.step)
                    assertEquals(
                        JsonNull,
                        f.source.reviewInputs
                            .last()
                            .profile
                            ?.get("shift_work"),
                    )
                } finally {
                    f.close()
                }
            }

        @Test fun returnChoiceRetainsWebEndOrReplacesItAndGuardsDoubleTap() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val draft =
                        f.model.draft.let {
                            it.copy(form = it.form.copy(end = it.form.end.copy(kind = "nights", nights = 9)))
                        }
                    val m = f.model(draft)
                    m.chooseReturn("return_legs")
                    m.chooseReturn("one_way")
                    assertEquals("return_legs", m.draft.form.end.kind)
                    assertNull(m.draft.form.end.nights)
                    assertEquals(
                        "",
                        m.draft.form.legs
                            .last()
                            .departLocal,
                    )
                    assertEquals(
                        "",
                        m.draft.form.legs
                            .last()
                            .arriveLocal,
                    )
                    assertEquals(
                        m.draft.form.legs[m.draft.form.returnBoundary - 1]
                            .toIata,
                        m.draft.form.legs
                            .last()
                            .fromIata,
                    )
                    assertEquals(
                        m.draft.form.legs[0]
                            .fromIata,
                        m.draft.form.legs
                            .last()
                            .toIata,
                    )
                    awaitWizard { 250L in f.clock.deadlines }
                    f.clock.advance(250)
                    assertEquals(MemberWizardStep.returnRoute, m.draft.state.step)
                } finally {
                    f.close()
                }
            }

        @Test fun backCancelsCheckingAndRecoveryKeepsQuestionsAndLastInput() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val entered = CompletableDeferred<Unit>()
                    f.source.configure(beforeReview = {
                        entered.complete(Unit)
                        awaitCancellation()
                    })
                    f.model.beginChecking()
                    entered.await()
                    f.model.go(MemberWizardStep.returnChoice)
                    f.source.configure()
                    val review =
                        generationReview().copy(
                            prompts =
                                listOf(
                                    agePrompt(
                                        "screening:age_band",
                                    ),
                                    screeningPrompt(
                                        "shift_work",
                                    ),
                                ),
                        )
                    f.source.queueReviews(listOf(Result.success(review)))
                    f.model.beginChecking()
                    finishCheck(f, count = 2)
                    val m = f.model
                    assertEquals(MemberWizardStep.returnChoice, m.lastInputStep)
                    m.answerQuestion(JsonPrimitive("18_64"))
                    awaitWizard { 1750L in f.clock.deadlines }
                    f.clock.advance(1750)
                    assertEquals(1, m.draft.state.questionIndex)
                    m.backFromQuestion()
                    assertEquals(MemberWizardStep.question, m.draft.state.step)
                    assertEquals(0, m.draft.state.questionIndex)
                    m.flush()
                    val recovered = requireNotNull(f.store.recover())
                    assertEquals(review, recovered.state.review)
                    assertEquals(review.prompts, recovered.state.questions)
                    assertEquals(JsonPrimitive("18_64"), recovered.state.profile["age_band"])
                    m.backFromQuestion()
                    assertEquals(MemberWizardStep.returnChoice, m.draft.state.step)
                } finally {
                    f.close()
                }
            }
    }

    @RunWith(Parameterized::class)
    class RequestCancellation(
        private val inline: Boolean,
    ) {
        @Test fun requestCancellationShowsFailureWithoutAutomaticRestart() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                try {
                    val m =
                        if (inline) {
                            val review = generationReview()
                            val initial = f.model.draft
                            val form = initial.form.copy(identity = review.identity)
                            val ready =
                                initial.copy(
                                    form = form,
                                    state =
                                        initial.state.copy(
                                            step = MemberWizardStep.ready,
                                            review = review,
                                            reviewedAt =
                                                f.now(),
                                        ),
                                )
                            f.model(ready)
                        } else {
                            f.model
                        }
                    f.source.configure(beforeReview = { throw CancellationException() })
                    if (inline) m.readyTick(true) else m.beginChecking()
                    awaitWizard { m.checking.failure == DataFailure.Server(null) }
                    assertEquals(MemberWizardStep.checking, m.draft.state.step)
                    assertFalse(m.reviewing)
                    m.resumeChecking()
                    m.readyTick(true)
                    runCurrent()
                    assertEquals(1, f.source.count("review"))
                } finally {
                    f.close()
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "inline={0}")
            fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
        }
    }

    @RunWith(Parameterized::class)
    class ResumedQuestion(
        private val suspendCancellation: Boolean,
    ) {
        @Test fun resumedQuestionChecksOfflineWithoutWaitingForPreviousCancellation() =
            runTest(timeout = 60.seconds) {
                val f = reviewFixture()
                val generation = SuspendedReviewCancellation(f.generation)
                try {
                    val review = generationReview().copy(prompts = listOf(screeningPrompt("shift_work")))
                    val initial = f.model.draft
                    val draft =
                        initial.copy(
                            form = initial.form.copy(identity = review.identity),
                            state =
                                initial.state.copy(
                                    step = MemberWizardStep.question,
                                    lastInputStep = MemberWizardStep.sleep,
                                    sleepCompleted = true,
                                    review = review,
                                    questions = review.prompts,
                                    questionIndex = 0,
                                    revision = initial.state.revision + 1,
                                ),
                        )
                    f.store.persist(draft)
                    val recovered = requireNotNull(f.store.recover())
                    f.source.configure(reviewFailure = DataFailure.Offline)
                    val m = f.model(recovered, generation)
                    if (suspendCancellation) {
                        m.beginChecking()
                        awaitWizard { m.checking.failure == DataFailure.Offline }
                        m.navigateToQuestion(0)
                        awaitWizard { generation.cancellations.size == 1 }
                    }
                    m.answerQuestion(JsonPrimitive(false))
                    awaitWizard { 250L in f.clock.deadlines }
                    f.clock.advance(250)
                    awaitWizard {
                        m.checking.failure == DataFailure.Offline && m.draft.state.step == MemberWizardStep.checking
                    }
                    assertEquals(if (suspendCancellation) 2 else 1, f.source.count("review"))
                    assertEquals(250L, f.clock.now)
                    assertFalse(m.checking.slow)
                    if (suspendCancellation) {
                        assertEquals(2, generation.instances.size)
                        assertTrue(generation.instances[0] != generation.instances[1])
                        assertEquals(listOf(generation.instances[0]), generation.cancellations)
                    } else {
                        assertEquals(1, generation.instances.size)
                        assertTrue(generation.cancellations.isEmpty())
                    }
                    assertEquals(
                        JsonPrimitive(false),
                        f.source.reviewInputs
                            .last()
                            .profile
                            ?.get("shift_work"),
                    )
                    assertEquals(
                        review.identity,
                        f.source.reviewInputs
                            .last()
                            .identity,
                    )
                } finally {
                    generation.releaseCancellations()
                    f.close()
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "suspendCancellation={0}")
            fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
        }
    }
}

private class SuspendedReviewCancellation(
    private val service: TripGenerationService,
) : MemberWizardGeneration by service {
    val instances = mutableListOf<String>()
    val cancellations = mutableListOf<String>()
    private val released = CompletableDeferred<Unit>()

    override suspend fun review(
        instance: String,
        input: ApiTripReviewInput,
        edit: TripEditContext?,
    ): TripGenerationReviewResult {
        instances.add(instance)
        return service.review(instance, input, edit)
    }

    override suspend fun cancelReviews(instance: String) {
        cancellations.add(instance)
        released.await()
        service.cancelReviews(instance)
    }

    fun releaseCancellations() {
        released.complete(Unit)
    }
}
