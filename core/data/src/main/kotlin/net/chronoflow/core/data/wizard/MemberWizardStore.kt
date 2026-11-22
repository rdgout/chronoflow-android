package net.chronoflow.core.data.wizard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import net.chronoflow.core.data.reminders.FirstTripRemindersOffer
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.DraftRecord
import net.chronoflow.core.model.store.StoreTime
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardRecord
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardSleepOverride
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.loadTrip
import java.time.Instant

class MemberWizardStore(
    database: AccountDatabase,
    private val permit: SessionMutationPermit? = null,
    private val now: () -> Instant,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : WizardDraftStore {
    private val drafts = DraftRepository(database)
    private val trips = TripRepository(database)
    private val settings = SettingsRepository(database)
    private val remindersOffer = FirstTripRemindersOffer(database, permit)

    suspend fun recover(): MemberWizardDraft? =
        withContext(io) {
            val record = drafts.recovery(now(), permit) ?: return@withContext null
            val draft =
                MemberWizardRecord.decode(
                    MemberWizardRecord.Payload(
                        form = record.form,
                        state = requireNotNull(record.wizardState),
                        fieldErrors = record.fieldErrors ?: "{}",
                    ),
                )
            if (draft == null) drafts.remove(record.id, permit)
            draft
        }

    suspend fun start(): MemberWizardDraft =
        withContext(io) {
            recover()?.let { return@withContext it }
            val draft = initialDraft()
            drafts.createRecovery(record(draft), permit)
            draft
        }

    suspend fun start(seed: MemberWizardDraft): MemberWizardDraft =
        withContext(io) {
            val initial = initialDraft()
            val draft =
                initial.copy(
                    form = seed.form.copy(identity = null, override = WizardSleepOverride()),
                    state =
                        initial.state.copy(
                            airports = seed.airports,
                            entered = true,
                            fromLastTrip = false,
                            step = MemberWizardStep.checking,
                            returnToReady = true,
                            profile = JsonObject(emptyMap()),
                        ),
                )
            drafts.replaceRecovery(record(draft), permit)
            draft
        }

    suspend fun startTrip(
        id: String,
        action: TripWizardAction,
        source: DataSource,
    ): MemberWizardDraft =
        withContext(io) {
            val snapshot = source.tripInput(id)
            currentCoroutineContext().ensureActive()
            val draft = initialDraft().loadTrip(snapshot, id, action)
            drafts.replaceRecovery(record(draft), permit)
            draft
        }

    private suspend fun initialDraft(): MemberWizardDraft {
        val latest = trips.trips().lastOrNull { it.deletedAt == null && !it.deletedLocally }
        val origin =
            latest?.let {
                try {
                    ApiJson.response.decodeFromString<TripCardView>(it.card).from
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        val values = settings.settings()

        fun section(name: String) =
            values.firstOrNull { it.section == name }?.let {
                try {
                    ApiJson.response.parseToJsonElement(it.value)
                } catch (_: SerializationException) {
                    null
                }
            }
        val profile = section("bodyClock") as? JsonObject
        val sleepAnswered = (section("sleepAnswered") as? JsonPrimitive)?.booleanOrNull
        return MemberWizardDrafts.memberInitial(latest != null, origin, profile, sleepAnswered)
    }

    override suspend fun persist(draft: MemberWizardDraft) =
        withContext(io) {
            drafts.updateRecovery(record(draft), draft.state.instance, draft.state.revision, permit)
        }

    override suspend fun discard(instance: String) {
        drafts.deleteRecovery(instance, permit)
    }

    override suspend fun didSave(
        instance: String,
        firstTrip: Boolean,
    ) {
        remindersOffer.queue(firstTrip)
        discard(instance)
    }

    private fun record(draft: MemberWizardDraft): DraftRecord {
        val payload = MemberWizardRecord.encode(draft)
        val state = draft.state.discardRetiredValues()
        return DraftRecord(
            id = MemberWizardRecord.ID,
            tripId = state.edit?.tripID,
            form = payload.form,
            baseForm = state.originalForm?.let { MemberWizardRecord.encode(draft.copy(form = it)).form },
            dirty = draft.hasChanges,
            token = draft.form.identity?.token,
            inputRevision = draft.form.identity?.inputRevision ?: 0,
            reviewRevision = draft.form.identity?.reviewRevision,
            wizardSourceRevision = state.edit?.sourceRevision,
            furthestStep = state.step.name,
            wizardState = payload.state,
            fieldErrors = payload.fieldErrors,
            updatedAt = StoreTime.millis(now()),
        )
    }
}
