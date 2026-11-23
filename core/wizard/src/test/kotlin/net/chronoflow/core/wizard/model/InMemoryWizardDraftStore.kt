package net.chronoflow.core.wizard.model

class InMemoryWizardDraftStore : WizardDraftStore {
    val persisted = mutableListOf<MemberWizardDraft>()
    val last: MemberWizardDraft? get() = persisted.lastOrNull()
    val discarded = mutableListOf<String>()
    val saves = mutableListOf<Pair<String, Boolean>>()
    var failure: Throwable? = null
    var beforePersist: suspend (MemberWizardDraft) -> Unit = {}

    override suspend fun persist(draft: MemberWizardDraft) {
        beforePersist(draft)
        failure?.let { throw it }
        persisted.add(draft)
    }

    override suspend fun discard(instance: String) {
        discarded.add(instance)
    }

    override suspend fun didSave(
        instance: String,
        firstTrip: Boolean,
    ) {
        saves.add(instance to firstTrip)
    }
}
