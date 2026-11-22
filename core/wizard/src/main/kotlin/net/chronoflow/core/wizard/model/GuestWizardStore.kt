package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.store.StoreFailure
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.GeneralSecurityException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit.MILLIS

class GuestWizardStore(
    private val file: suspend () -> File,
    private val io: CoroutineDispatcher,
    private val now: () -> Instant,
) : WizardDraftStore {
    private class Record(
        val draft: MemberWizardDraft,
        val updatedAt: Instant,
    )

    private val mutex = Mutex()

    suspend fun start(): MemberWizardDraft =
        withContext(io) {
            mutex.withLock {
                recoverRecord()?.draft ?: record(MemberWizardDrafts.guest()).also { write(it) }.draft
            }
        }

    suspend fun recover(): MemberWizardDraft? = withContext(io) { mutex.withLock { recoverRecord()?.draft } }

    suspend fun claim(secrets: GuestSecretStore): GuestClaim? =
        try {
            val draft = recover()
            val identity = draft?.form?.identity
            val revision = identity?.reviewRevision
            if (draft == null || identity == null || identity.token.isEmpty() || revision == null) {
                null
            } else {
                val secret = secrets.get(identity.token)
                if (secret.isNullOrEmpty()) {
                    null
                } else {
                    val current = recover()
                    if (current?.state?.instance == draft.state.instance && current.form.identity == identity) {
                        GuestClaim(identity.token, identity.inputRevision, revision, secret)
                    } else {
                        null
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            null
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: StoreFailure) {
            null
        } catch (_: RuntimeException) {
            null
        }

    suspend fun reviewExpired(claim: GuestClaim) =
        withContext(io) {
            mutex.withLock {
                val draft = recoverRecord()?.draft ?: return@withLock
                val identity = draft.form.identity ?: return@withLock
                if (identity.token != claim.draftToken || identity.inputRevision != claim.inputRevision ||
                    identity.reviewRevision != claim.reviewRevision
                ) {
                    return@withLock
                }
                val expired = draft.returnToReadyAfterExpiredClaim()
                write(record(expired.copy(state = expired.state.copy(revision = expired.state.revision + 1))))
            }
        }

    suspend fun recoveryDecision(
        handoffFinished: Boolean,
        introFinished: Boolean,
    ): GuestRecoveryGate.Decision =
        withContext(io) {
            mutex.withLock {
                val record = recoverRecord() ?: return@withLock GuestRecoveryGate.Decision.none
                GuestRecoveryGate.decision(
                    record.updatedAt,
                    now(),
                    handoffFinished,
                    introFinished,
                    record.draft.state.signingIn == true,
                )
            }
        }

    override suspend fun persist(draft: MemberWizardDraft) =
        withContext(io) {
            mutex.withLock {
                val current = recoverRecord()?.draft
                if (current == null || current.state.instance != draft.state.instance) throw RecoveryRejected()
                if (current.state.revision <= draft.state.revision) write(record(draft))
            }
        }

    override suspend fun discard(instance: String) =
        withContext(io) {
            mutex.withLock {
                if (recoverRecord()?.draft?.state?.instance == instance) Files.deleteIfExists(file().toPath())
                Unit
            }
        }

    override suspend fun didSave(
        instance: String,
        firstTrip: Boolean,
    ) = discard(instance)

    private suspend fun recoverRecord(): Record? {
        val target = file()
        if (!target.exists()) return null
        val bytes = target.readBytes()
        val decoded =
            try {
                val json =
                    ApiJson.response
                        .parseToJsonElement(
                            ProfilePreferences.cleanLegacyJSON(bytes).decodeToString(),
                        ).jsonObject
                val payload =
                    MemberWizardRecord.Payload(
                        json.getValue("form").toString(),
                        json.getValue("state").toString(),
                        json.getValue("fieldErrors").toString(),
                    )
                MemberWizardRecord
                    .decode(
                        payload,
                    )?.let { Record(it, Instant.parse(json.getValue("updatedAt").jsonPrimitive.content)) }
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: NoSuchElementException) {
                null
            } catch (_: DateTimeParseException) {
                null
            }
        if (decoded == null || decoded.draft.form.end.kind != "one_way" ||
            GuestRecoveryGate.decision(decoded.updatedAt, now(), false, false, decoded.draft.state.signingIn == true) ==
            GuestRecoveryGate.Decision.expire
        ) {
            Files.deleteIfExists(target.toPath())
            return null
        }
        return record(decoded.draft, decoded.updatedAt).also { write(it) }
    }

    private fun record(
        draft: MemberWizardDraft,
        updatedAt: Instant = now().truncatedTo(MILLIS),
    ): Record {
        val cleaned =
            draft.copy(
                state =
                    draft.state
                        .copy(
                            mode = WizardMode.guest,
                            lastInputStep = MemberWizardStep.sleep,
                            sleepNeeded = true,
                            savedProfile = null,
                            firstTrip = false,
                        ).discardRetiredValues(),
            )
        return Record(checkNotNull(MemberWizardRecord.decode(MemberWizardRecord.encode(cleaned))), updatedAt)
    }

    private suspend fun write(record: Record) {
        val payload = MemberWizardRecord.encode(record.draft)
        val bytes =
            ProfilePreferences.cleanLegacyJSON(
                JsonObject(
                    mapOf(
                        "form" to ApiJson.response.parseToJsonElement(payload.form),
                        "state" to ApiJson.response.parseToJsonElement(payload.state),
                        "fieldErrors" to ApiJson.response.parseToJsonElement(payload.fieldErrors),
                        "updatedAt" to JsonPrimitive(record.updatedAt.toString()),
                    ),
                ).toString().encodeToByteArray(),
            )
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.path + ".tmp")
        FileOutputStream(temporary).use {
            it.write(bytes)
            it.fd.sync()
        }
        Files.move(temporary.toPath(), target.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
    }
}
