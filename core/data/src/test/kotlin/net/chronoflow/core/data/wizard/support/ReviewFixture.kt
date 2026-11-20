@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.wizard.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.storing
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import org.junit.Assert.assertTrue
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal class ReviewFixture(
    val model: MemberWizardModel,
    val source: GenerationSource,
    val clock: CheckingTestClock,
    val databaseStore: WizardStore,
    val store: MemberWizardStore,
    val scope: CoroutineScope,
    val generation: TripGenerationService,
    val now: () -> Instant,
) {
    val database get() = databaseStore.database

    fun model(
        draft: MemberWizardDraft,
        generation: MemberWizardGeneration = this.generation,
    ): MemberWizardModel = MemberWizardModel(draft, store, scope, now, generation, clock.clock)

    suspend fun close() =
        withContext(NonCancellable) {
            generation.cancel()
            scope.coroutineContext.job.cancelAndJoin()
            databaseStore.close()
        }
}

internal suspend fun TestScope.reviewFixture(
    sleepNeeded: Boolean = false,
    effects: TripGenerationEffects? = null,
    source: GenerationSource = GenerationSource(),
): ReviewFixture {
    val databaseStore = WizardStore(StandardTestDispatcher(testScheduler))
    val wall = WizardTestClock()
    val store = MemberWizardStore(databaseStore.database, now = { wall.now }, io = databaseStore.io)
    val initial = store.start()
    val draft =
        initial.copy(
            form = WizardForm.from(generationInput()),
            state = initial.state.copy(step = MemberWizardStep.returnChoice, sleepNeeded = sleepNeeded),
        )
    val scope =
        CoroutineScope(
            backgroundScope.coroutineContext +
                SupervisorJob(
                    backgroundScope.coroutineContext.job,
                ),
        )
    val epoch = SyncEpoch(AccountID.of(WIZARD_ACCOUNT), 1)
    val stored =
        effects ?: TripGenerationEffects.storing(
            databaseStore.database,
            source,
            epoch,
            requestSync = {},
            now = { wall.now },
        )
    val generation =
        TripGenerationService(
            source,
            stored,
            parentContext = scope.coroutineContext + Dispatchers.Default,
        )
    val clock = CheckingTestClock(testScheduler)
    val now = { Instant.parse("0001-01-01T00:00:00Z") }
    val model = MemberWizardModel(draft, store, scope, now, generation, clock.clock)
    return ReviewFixture(model, source, clock, databaseStore, store, scope, generation, now)
}

internal suspend fun awaitWizard(
    detail: () -> String = { "Wizard condition" },
    condition: suspend () -> Boolean,
) {
    // Stay runnable so JDBC/service waits cannot auto-advance the fake checking clock.
    val started = TimeSource.Monotonic.markNow()
    while (started.elapsedNow() < 500.milliseconds) {
        if (condition()) return
        yield()
    }
    assertTrue(detail(), condition())
}

internal suspend fun finishCheck(
    fixture: ReviewFixture,
    count: Int = 1,
    start: Long = 0,
) {
    awaitWizard(
        { "Review $count / waiters; clock=${fixture.clock.now}, deadlines=${fixture.clock.deadlines}" },
    ) {
        fixture.source.count("review") == count && fixture.clock.waiterCount >= 2
    }
    fixture.clock.advance(start + 1200)
    awaitWizard({ "Tick 3 / pause at ${fixture.clock.now}: ${fixture.clock.deadlines}" }) {
        fixture.model.checking.ticks == 3 && start + 1500 in fixture.clock.deadlines
    }
    fixture.clock.advance(start + 1500)
    awaitWizard {
        fixture.model.draft.state.step != MemberWizardStep.checking || fixture.model.checking.failure != null
    }
}

internal fun screeningPrompt(field: String): ApiReviewPrompt =
    ApiReviewPrompt(
        "screening:$field",
        PromptKind.Screening,
        true,
        true,
        CopyLine(
            "In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule?",
            emptyList(),
        ),
        ApiReviewPromptScope(null, null, null),
        ReviewPromptTarget.Profile,
        listOf(field),
        emptyMap(),
        ApiReviewPromptShape(ReviewAnswerShape.Boolean, emptyList(), null, null),
        null,
        null,
    )

internal fun agePrompt(id: String = "age_band"): ApiReviewPrompt =
    ApiReviewPrompt(
        id,
        PromptKind.AgeBand,
        true,
        true,
        CopyLine("Which age group are you in?", emptyList()),
        ApiReviewPromptScope(null, null, null),
        ReviewPromptTarget.Profile,
        listOf("age_band"),
        emptyMap(),
        ApiReviewPromptShape(ReviewAnswerShape.Options, listOf("under_18", "18_64", "65_plus"), null, null),
        null,
        null,
    )
