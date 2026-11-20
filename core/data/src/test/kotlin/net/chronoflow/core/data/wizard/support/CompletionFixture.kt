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
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.datamock.MockWizardPrompts
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.reviewInput

internal fun noGenerationEffects() = TripGenerationEffects({}, {}, {})

internal class CompletionFixture(
    val model: MemberWizardModel,
    val source: GenerationSource,
    val service: TripGenerationService,
    val store: MemberWizardStore,
    val databaseStore: WizardStore,
    val clock: WizardTestClock,
    val checking: CheckingTestClock,
    val review: ApiTripReview,
    val scope: CoroutineScope,
) {
    val database get() = databaseStore.database

    fun model(
        draft: MemberWizardDraft,
        generation: MemberWizardGeneration = service,
        store: WizardDraftStore = this.store,
    ) = MemberWizardModel(draft, store, scope, { clock.now }, generation, checking.clock)

    fun newService() =
        TripGenerationService(
            source,
            noGenerationEffects(),
            parentContext = scope.coroutineContext + Dispatchers.Default,
        )

    suspend fun close() =
        withContext(NonCancellable) {
            service.cancel()
            scope.coroutineContext.job.cancelAndJoin()
            databaseStore.close()
        }
}

internal suspend fun TestScope.completionFixture(
    optional: Boolean = false,
    effects: TripGenerationEffects = noGenerationEffects(),
    firstTrip: Boolean = true,
): CompletionFixture {
    val databaseStore = WizardStore(StandardTestDispatcher(testScheduler))
    val clock = WizardTestClock()
    val store = MemberWizardStore(databaseStore.database, now = { clock.now }, io = databaseStore.io)
    val initial = store.start()
    var form = WizardForm.from(generationInput())
    form =
        form.copy(
            legs =
                form.legs.map {
                    it.copy(
                        departLocal = "2028-10-12T14:40",
                        arriveLocal = "2028-10-13T09:15",
                    )
                },
        )
    var review = generationReview()
    if (optional) {
        review =
            review.copy(prompts = MockWizardPrompts.make(form.reviewInput(null).input, JsonObject(emptyMap())))
    }
    var draft =
        initial.copy(
            form = form.copy(identity = review.identity),
            state =
                initial.state.copy(
                    firstTrip = firstTrip,
                    review = review,
                    sleepNeeded = false,
                    step = MemberWizardStep.ready,
                    reviewedAt = clock.now,
                ),
        )
    draft = draft.copy(state = draft.state.copy(checkedInput = draft.inputFingerprint))
    store.persist(draft)
    val source = GenerationSource()
    val scope =
        CoroutineScope(
            backgroundScope.coroutineContext +
                SupervisorJob(
                    backgroundScope.coroutineContext.job,
                ),
        )
    val service =
        TripGenerationService(
            source,
            effects,
            parentContext = scope.coroutineContext + Dispatchers.Default,
        )
    val checking = CheckingTestClock(testScheduler)
    val model = MemberWizardModel(draft, store, scope, { clock.now }, service, checking.clock)
    return CompletionFixture(model, source, service, store, databaseStore, clock, checking, review, scope)
}

internal suspend fun TestScope.withCompletion(
    optional: Boolean = false,
    firstTrip: Boolean = true,
    effects: TripGenerationEffects = noGenerationEffects(),
    block: suspend CompletionFixture.() -> Unit,
) {
    val fixture = completionFixture(optional, effects, firstTrip)
    try {
        fixture.block()
    } finally {
        fixture.close()
    }
}
