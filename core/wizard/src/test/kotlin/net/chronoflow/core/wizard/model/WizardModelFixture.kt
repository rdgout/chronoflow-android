package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.FakeWizardGeneration
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview

internal class WizardModelFixture(
    scope: CoroutineScope,
    scheduler: TestCoroutineScheduler,
    guest: Boolean = false,
    ready: Boolean = false,
) {
    val store = InMemoryWizardDraftStore()
    val clock = WizardTestClock()
    val checking = CheckingTestClock(scheduler)
    val generation = FakeWizardGeneration()
    val model: MemberWizardModel

    init {
        var draft = if (guest) MemberWizardDrafts.guest() else MemberWizardDrafts.memberInitial(false, null, null, true)
        draft = draft.copy(form = WizardForm.from(generationInput()), state = draft.state.copy(sleepCompleted = true))
        if (ready) {
            draft =
                draft.copy(
                    form = draft.form.copy(identity = generationReview().identity),
                    state =
                        draft.state.copy(
                            review = generationReview(),
                            reviewedAt = clock.now,
                            step = MemberWizardStep.ready,
                        ),
                )
        }
        model = MemberWizardModel(draft, store, scope, { clock.now }, generation, checking.clock)
        if (ready) model.state = model.state.copy(checkedInput = model.inputFingerprint)
    }

    suspend fun close() {
        model.flush()
        model.discard()
    }
}
