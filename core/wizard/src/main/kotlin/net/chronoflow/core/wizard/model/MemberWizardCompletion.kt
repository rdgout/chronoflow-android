package net.chronoflow.core.wizard.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.generation.TripGenerationAnswers
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import net.chronoflow.core.wizard.generation.TripGenerationState
import java.time.Duration
import java.util.UUID
import kotlin.math.roundToLong

val MemberWizardModel.reviewInput: ApiTripReviewInput get() =
    form.reviewInput(
        form.identity,
        state.profile.takeIf {
            it.isNotEmpty()
        },
    )
val MemberWizardModel.inputFingerprint: String get() = draft.inputFingerprint
val MemberWizardModel.optionalPrompts: List<ApiReviewPrompt> get() =
    (state.review?.prompts ?: emptyList())
        .filter {
            !it.blocking && ProfilePreferences.isSupported(it) &&
                !(it.kind == PromptKind.Cups && preference("exclude_caffeine")) &&
                (
                    mode != WizardMode.guest ||
                        it.kind in
                        listOf(
                            PromptKind.Room,
                            PromptKind.LatestWake,
                            PromptKind.SeatSide,
                            PromptKind.Cups,
                            PromptKind.LightDevice,
                            PromptKind.BlueBlockers,
                        )
                )
        }.sortedBy { !it.required }

val MemberWizardModel.needsReadyReview: Boolean get() {
    val expired = state.reviewedAt?.let { Duration.between(it, now()).toMillis() >= 1_800_000 } != false
    if (mode ==
        WizardMode.guest
    ) {
        return form.identity?.token.isNullOrEmpty() || checking.failure != null ||
            state.saveFailure != null ||
            state.checkedInput != inputFingerprint ||
            expired
    }
    if (state.savedTripId != null) return false
    return form.identity == null || state.checkedInput != inputFingerprint || expired
}
val MemberWizardModel.canSave: Boolean get() =
    !reviewing && !persistenceFailed && !closed && retrySeconds == 0 &&
        (!isEditing || hasChanges) && (state.savedTripId != null || state.review?.canSave == true)

fun MemberWizardModel.preference(field: String): Boolean {
    if (field !in listOf("exclude_caffeine", "flight_movement_breaks")) return false
    return (state.profile[field] ?: state.savedProfile?.get(ProfilePreferences.camel(field))) == JsonPrimitive(true)
}

fun MemberWizardModel.setPreference(
    field: String,
    value: Boolean,
) {
    if (closed || reviewing || state.step != MemberWizardStep.fineTune ||
        field !in listOf("exclude_caffeine", "flight_movement_breaks")
    ) {
        return
    }
    state = state.copy(profile = JsonObject(state.profile + (field to JsonPrimitive(value))))
    changed()
}

fun MemberWizardModel.editForm(update: (WizardForm) -> WizardForm) {
    if (closed || reviewing || state.step == MemberWizardStep.calculating) return
    form = update(form)
    changed()
}

fun MemberWizardModel.openPrompt(id: String?) {
    if (closed || reviewing) return
    state = state.copy(promptSheetID = id)
    persist()
}

fun MemberWizardModel.answerOptional(
    prompt: ApiReviewPrompt,
    values: Map<String, JsonElement>,
) {
    val review = state.review ?: return
    if (closed || reviewing || prompt !in optionalPrompts) return
    val input = TripGenerationAnswers.applyingAnswers(reviewInput, review, mapOf(prompt.id to values))
    form = WizardForm.from(input, retained = form)
    state = state.copy(profile = input.profile ?: JsonObject(emptyMap()), promptSheetID = null)
    changed()
}

fun MemberWizardModel.revisit(
    step: MemberWizardStep,
    sleep: Boolean = false,
) {
    if (reviewing || state.step != MemberWizardStep.ready) return
    state = state.copy(updated = false, sourceChange = null, revisit = inputFingerprint, returnToReady = true)
    if (mode == WizardMode.member && sleep && !form.override.enabled) {
        form =
            form.copy(
                override =
                    form.override.copy(
                        enabled = true,
                        habitualBed =
                            form.override.habitualBed ?: state.profile["habitual_bed"].stringValue
                                ?: state.savedProfile?.get("habitualBed").stringValue,
                        habitualWake =
                            form.override.habitualWake ?: state.profile["habitual_wake"].stringValue
                                ?: state.savedProfile?.get("habitualWake").stringValue,
                    ),
            )
    }
    go(if (mode == WizardMode.guest && sleep) MemberWizardStep.sleep else step)
}

fun MemberWizardModel.finishRoute(home: Boolean = false) {
    if (state.revisit != null && outboundComplete &&
        (form.end.kind != "return_legs" || returnComplete)
    ) {
        finishFlightInput()
    } else if (home) {
        finishReturnRoute()
    } else {
        go(MemberWizardStep.outbound)
    }
}

fun MemberWizardModel.finishFineTune() {
    if (closed || reviewing) return
    if (state.checkedInput == inputFingerprint) {
        state = state.copy(revisit = null, returnToReady = null)
        go(MemberWizardStep.ready)
    } else {
        reviewInPlace()
    }
}

fun MemberWizardModel.readyTick(force: Boolean = false) {
    if (guestAuthenticationPresented) return
    updateRetrySeconds()
    if (state.step != MemberWizardStep.ready || reviewing || state.savedTripId != null ||
        (!force && !needsReadyReview)
    ) {
        return
    }
    if (mode == WizardMode.guest) recheckGuestReady() else reviewInPlace()
}

fun MemberWizardModel.resumeCompletion() {
    if (resumed) return
    resumed = true
    if (state.step == MemberWizardStep.calculating) {
        savePlan(recovering = true)
    } else if (state.step == MemberWizardStep.ready) {
        readyTick(force = true)
    }
}

fun MemberWizardModel.cancelInlineReview() {
    reviewJob?.cancel()
    reviewJob = null
    reviewing = false
    cancelCheck()
}

private fun MemberWizardModel.reviewInPlace() {
    if (reviewing || closed) return
    reviewing = true
    state = state.copy(sourceChange = null, returnToReady = true)
    val input = reviewInput
    val edit = state.edit
    val fingerprint = inputFingerprint
    val id = UUID.randomUUID().toString()
    checkID = id
    val instance = "${state.instance}/$id"
    checkInstance = instance
    persist()
    reviewJob =
        scope.launch {
            wizardOperation({
                val service = generation ?: throw DataFailure.Server(null)
                val result = service.review(instance, input, edit)
                currentCoroutineContext().ensureActive()
                if (checkID == id && !closed) {
                    reviewing = false
                    reviewJob = null
                    if (inputFingerprint != fingerprint) readyTick() else applyReview(result)
                }
            }, { failure ->
                if (checkID == id && !closed) {
                    reviewing = false
                    reviewJob = null
                    go(MemberWizardStep.checking)
                    applyFailure(failure)
                }
            })
        }
}

fun MemberWizardModel.savePlan(recovering: Boolean = false) {
    if (mode == WizardMode.guest) {
        if (closed || persistenceFailed || reviewing || guestAuthenticationPresented || retrySeconds != 0 ||
            state.step != MemberWizardStep.ready
        ) {
            return
        }
        if (needsReadyReview) {
            recheckGuestReady()
        } else if (canSave) {
            saveOutcome = WizardSaveOutcome.signUpRequested
        }
        return
    }
    if (closed || saveJob != null || (!recovering && (state.step != MemberWizardStep.ready || !canSave))) return
    if (!recovering && needsReadyReview) {
        reviewInPlace()
        return
    }
    state = state.copy(saveFailure = null, step = MemberWizardStep.calculating)
    restartRetryCountdown()
    state = state.copy(promptSheetID = null)
    persist()
    val snapshot = draft
    val generationState =
        TripGenerationState(
            snapshot.state.instance,
            reviewInput,
            review = snapshot.state.review,
            edit = snapshot.state.edit,
            savedTripId = snapshot.state.savedTripId,
        )
    saveJob =
        scope.launch {
            try {
                flush()
                if (persistenceFailed) {
                    state = state.copy(step = MemberWizardStep.ready)
                    return@launch
                }
                wizardOperation({
                    val service = generation ?: throw DataFailure.Server(null)
                    val result =
                        service.save(generationState) { id ->
                            val saved =
                                snapshot.copy(
                                    state =
                                        snapshot.state.copy(
                                            savedTripId = id,
                                            revision =
                                                snapshot.state.revision + 1,
                                        ),
                                )
                            store.persist(saved)
                        }
                    currentCoroutineContext().ensureActive()
                    if (!closed) {
                        state = state.copy(savedTripId = result.state.savedTripId)
                        when (val outcome = result.outcome) {
                            is TripGenerationSaveResult.Outcome.Saved -> {
                                wizardOperation(
                                    { store.didSave(snapshot.state.instance, snapshot.state.firstTrip) },
                                    {},
                                )
                                currentCoroutineContext().ensureActive()
                                if (!closed) {
                                    createdTripID = outcome.tripId
                                    closed = true
                                }
                            }

                            is TripGenerationSaveResult.Outcome.ReReviewNeeded -> {
                                form = form.copy(identity = null)
                                state =
                                    state.copy(
                                        savedTripId = null,
                                        reviewedAt = null,
                                        checkedInput = null,
                                        step = MemberWizardStep.ready,
                                    )
                                persist()
                                readyTick(force = true)
                            }

                            is TripGenerationSaveResult.Outcome.Failed -> {
                                saveFailed(outcome.failure)
                            }
                        }
                    }
                }, { saveFailed(it) })
            } finally {
                saveJob = null
            }
        }
}

private fun MemberWizardModel.saveFailed(failure: DataFailure) {
    val value =
        when (failure) {
            DataFailure.Offline -> {
                WizardSaveFailure(WizardSaveFailure.Kind.offline)
            }

            is DataFailure.Throttled -> {
                WizardSaveFailure(
                    WizardSaveFailure.Kind.throttle,
                    failure.retryAfterSeconds.takeIf { it.isFinite() && it > 0 }?.let {
                        now().plusMillis(
                            (it * 1000).roundToLong(),
                        )
                    },
                )
            }

            else -> {
                WizardSaveFailure(WizardSaveFailure.Kind.server)
            }
        }
    state = state.copy(saveFailure = value, step = MemberWizardStep.ready)
    restartRetryCountdown()
    persist()
}

val MemberWizardModel.credits: WizardCredits? get() = state.review?.let(WizardCredits::of)
