package net.chronoflow.core.wizard.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import java.util.UUID
import kotlin.math.roundToLong

val MemberWizardModel.lastInputStep: MemberWizardStep get() {
    if (mode == WizardMode.guest) return MemberWizardStep.sleep
    state.lastInputStep?.let { return it }
    if (state.sleepNeeded) return MemberWizardStep.sleep
    return if (form.end.kind == "return_legs") MemberWizardStep.returnFlight else MemberWizardStep.returnChoice
}
val MemberWizardModel.returnComplete: Boolean get() =
    form.end.kind == "return_legs" &&
        form.legs.drop(form.returnBoundary).all {
            it.toIata.isNotEmpty() && WizardDateTime.date(it.departLocal) != null &&
                WizardDateTime.date(it.arriveLocal) != null
        }
val MemberWizardModel.sleepComplete: Boolean get() =
    WizardDateTime.isTime(state.profile["habitual_bed"].stringValue) &&
        WizardDateTime.isTime(state.profile["habitual_wake"].stringValue) && state.profile["chronotype"] != null
val MemberWizardModel.currentQuestion: ApiReviewPrompt? get() = state.questions?.getOrNull(state.questionIndex ?: 0)
val MemberWizardModel.progress: Double get() = maxOf(state.progressFloor ?: 0.0, stepProgress)
internal val MemberWizardModel.stepProgress: Double get() {
    if (mode == WizardMode.guest) return guestStepProgress
    return when (state.step) {
        MemberWizardStep.whereTo -> if (whereComplete) 0.15 else 0.08
        MemberWizardStep.outbound -> if (outboundComplete) 0.30 else 0.22
        MemberWizardStep.returnChoice, MemberWizardStep.returnRoute, MemberWizardStep.returnFlight -> 0.45
        MemberWizardStep.sleep -> 0.50
        MemberWizardStep.checking, MemberWizardStep.excluded -> if (state.checkingAnswers == true) 0.70 else 0.52
        MemberWizardStep.question -> 0.54 + 0.16 * (state.questionIndex ?: 0) / maxOf(1, state.questions?.size ?: 1)
        MemberWizardStep.continuation, MemberWizardStep.fineTune -> 0.80
        MemberWizardStep.ready -> 0.95
        MemberWizardStep.calculating -> 1.0
    }
}

fun MemberWizardModel.chooseReturn(kind: String) {
    if (mode != WizardMode.member || closed || choicePending || state.step != MemberWizardStep.returnChoice) return
    if (kind == "return_legs") {
        if (form.end.kind != kind) {
            form = form.copy(end = form.end.copy(nights = null, checkOutDate = null)).addReturn()
            state = state.copy(returnRouteConfirmed = false, homeChanged = false)
            val first = form.legs[0]
            form = form.updateLeg(form.legs.lastIndex) { it.copy(toIata = first.fromIata, toCity = first.fromCity) }
        }
        if (form.end.kind != kind) return
    } else if (kind == "one_way") {
        form = form.removeReturn().form.copy(end = WizardEnd())
        state = state.copy(returnRouteConfirmed = null, homeChanged = false)
    } else if (kind != form.end.kind || kind !in listOf("nights", "check_out")) {
        return
    }
    changed()
    advanceChoice { if (kind == "return_legs") it.go(MemberWizardStep.returnRoute) else it.finishFlightInput() }
}

fun MemberWizardModel.finishReturn(): Boolean {
    if (closed || persistenceFailed || !returnComplete) return false
    draft = draft.copy(fieldErrors = WizardFlightRules.errors(form, draft.airports, now(), false))
    persist()
    return draft.fieldErrors.isEmpty()
}

fun MemberWizardModel.setSleep(
    field: String,
    value: JsonElement,
) {
    if (field !in listOf("habitual_bed", "habitual_wake", "chronotype") || closed) return
    state = state.copy(profile = JsonObject(state.profile + (field to value)), sleepCompleted = false)
    changed()
}

fun MemberWizardModel.finishSleep(): Boolean {
    if (closed || persistenceFailed || !sleepComplete) return false
    val bed = state.profile["habitual_bed"].stringValue ?: return false
    val wake = state.profile["habitual_wake"].stringValue ?: return false

    fun minutes(value: String): Int = value.take(2).toInt() * 60 + value.takeLast(2).toInt()
    val duration = (minutes(wake) - minutes(bed) + 1440) % 1440
    if (duration !in 180..840) {
        draft =
            draft.copy(
                fieldErrors =
                    draft.fieldErrors + (
                        "profile.habitual_bed" to
                            FieldMessage(
                                "Check your bedtime and wake-up time.",
                            )
                    ),
            )
        persist()
        return false
    }
    state = state.copy(sleepCompleted = true)
    beginChecking()
    return true
}

fun MemberWizardModel.finishFlightInput() {
    if (state.revisit != null && state.revisit == inputFingerprint) {
        state = state.copy(revisit = null, returnToReady = null)
        go(MemberWizardStep.ready)
        return
    }
    if (state.sleepNeeded) go(MemberWizardStep.sleep) else beginChecking()
}

fun MemberWizardModel.beginChecking() {
    updateRetrySeconds()
    if (closed || persistenceFailed || choicePending || retrySeconds != 0) return
    if (state.step == MemberWizardStep.checking && checkJob != null && checking.failure == null) return
    if (state.sleepNeeded && state.sleepCompleted != true) {
        go(MemberWizardStep.sleep)
        return
    }
    if (state.step in
        listOf(MemberWizardStep.returnChoice, MemberWizardStep.returnFlight, MemberWizardStep.sleep)
    ) {
        state =
            state.copy(lastInputStep = state.step)
    }
    go(MemberWizardStep.checking)
    runCheck()
}

fun MemberWizardModel.resumeChecking() {
    if (state.step != MemberWizardStep.checking || checkJob != null || checking.failure != null) return
    if (state.sleepNeeded && state.sleepCompleted != true) {
        go(MemberWizardStep.sleep)
        return
    }
    runCheck()
}

fun MemberWizardModel.cancelCheck() {
    checkID = UUID.randomUUID().toString()
    checkJob?.cancel()
    checkJob = null
    val instance = checkInstance ?: return
    checkInstance = null
    scope.launch { generation?.cancelReviews(instance) }
}

private fun MemberWizardModel.runCheck() {
    cancelCheck()
    checking = WizardCheckingProgress()
    state = state.copy(sourceChange = null)
    restartRetryCountdown()
    val id = UUID.randomUUID().toString()
    checkID = id
    val input = form.reviewInput(form.identity, state.profile.takeIf { it.isNotEmpty() })
    val instance = "${state.instance}/$id"
    checkInstance = instance
    val edit = state.edit
    checkJob =
        scope.launch {
            try {
                currentCoroutineContext().ensureActive()
                WizardChecking.run(checkingClock, {
                    val service = generation ?: throw DataFailure.Server(null)
                    service.review(instance, input, edit)
                }) { event ->
                    if (checkID == id && !closed && state.step == MemberWizardStep.checking) {
                        when (event) {
                            is WizardChecking.Event.Tick -> {
                                checking =
                                    checking.copy(ticks = maxOf(checking.ticks, event.count))
                            }

                            WizardChecking.Event.Slow -> {
                                checking = checking.copy(slow = true)
                            }

                            is WizardChecking.Event.Complete -> {
                                applyReview(event.result)
                            }

                            is WizardChecking.Event.Failed -> {
                                applyFailure(event.failure)
                            }
                        }
                    }
                }
            } finally {
                if (checkID == id) checkJob = null
            }
        }
}

internal fun MemberWizardModel.applyReview(result: TripGenerationReviewResult) {
    try {
        val unchangedInput = state.checkedInput == inputFingerprint
        result.sourceSnapshot?.let { acceptSourceReload(it, result.review.price) }
        result.edit?.let { state = state.copy(edit = it) }
        form = WizardForm.from(result.input, retained = form).copy(itinerary = result.review.itinerary)
        state = state.copy(profile = ProfilePreferences.request(result.input.profile ?: JsonObject(emptyMap())))
        val previous = state.review
        state =
            state.copy(
                review = result.review,
                updated =
                    unchangedInput && previous != null &&
                        (
                            previous.summary != result.review.summary ||
                                previous.price?.cost != result.review.price?.cost ||
                                previous.price?.state != result.review.price?.state ||
                                previous.price?.unlimited != result.review.price?.unlimited ||
                                previous.price?.freeDirections != result.review.price?.freeDirections
                        ),
                reviewedAt = now(),
                saveFailure = null,
            )
        state = state.copy(checkedInput = inputFingerprint)
        checking = WizardCheckingProgress()
        restartRetryCountdown()
        if (mode == WizardMode.member && state.sleepCompleted == true) state = state.copy(sleepNeeded = false)
        draft = draft.copy(fieldErrors = emptyMap())
        val errors = WizardFlightRules.errors(form, draft.airports, now(), true, mode)
        if (errors.isNotEmpty()) {
            draft = draft.copy(fieldErrors = errors)
            go(WizardJourney.errorStep(errors, form))
            return
        }
        val questions = result.review.prompts.filter { it.blocking && ProfilePreferences.isSupported(it) }
        state = state.copy(questions = questions, questionIndex = 0)
        when {
            result.review.status == PlanStatus.NeedsAnswers && questions.isEmpty() -> {
                applyFailure(DataFailure.Server(null))
            }

            result.review.status == PlanStatus.NoPlan -> {
                go(MemberWizardStep.excluded)
            }

            questions.isNotEmpty() -> {
                go(MemberWizardStep.question)
            }

            else -> {
                val destination =
                    if (state.returnToReady == true ||
                        (mode == WizardMode.member && optionalPrompts.isEmpty())
                    ) {
                        MemberWizardStep.ready
                    } else {
                        MemberWizardStep.fineTune
                    }
                state = state.copy(returnToReady = null, revisit = null)
                go(destination)
            }
        }
    } catch (_: DataFailure) {
        applyFailure(DataFailure.Server(null))
    }
}

internal fun MemberWizardModel.applyFailure(failure: DataFailure) {
    val fields =
        when (failure) {
            is DataFailure.Validation -> failure.fields
            is DataFailure.ProfileInvalid -> failure.fields
            else -> null
        }
    if (fields != null) {
        draft = draft.copy(fieldErrors = WizardJourney.errors(fields, form))
        val keys = draft.fieldErrors.keys
        if (keys.any {
                it.startsWith("override.") || it.startsWith("preparation.") || it.startsWith("stays.") ||
                    it.contains("seatWindow")
            }
        ) {
            go(MemberWizardStep.fineTune)
        } else if (keys.any { it.startsWith("profile.") }) {
            val sleepFields = listOf("profile.habitual_bed", "profile.habitual_wake", "profile.chronotype")
            go(
                if (keys.any { key ->
                        sleepFields.any { key == it || key == WizardJourney.errorPath(it, form) }
                    }
                ) {
                    MemberWizardStep.sleep
                } else {
                    MemberWizardStep.fineTune
                },
            )
        } else {
            go(WizardJourney.errorStep(draft.fieldErrors, form))
        }
    } else {
        val seconds = (failure as? DataFailure.Throttled)?.retryAfterSeconds
        checking =
            checking.copy(
                failure = failure,
                retryAt =
                    seconds?.takeIf { it.isFinite() && it > 0 }?.let {
                        checkingClock.nowMillis() +
                            (it * 1000).roundToLong()
                    },
            )
        restartRetryCountdown()
        persist()
    }
}

fun MemberWizardModel.answerQuestion(value: JsonElement) {
    val prompt = currentQuestion ?: return
    val field = prompt.fields.firstOrNull() ?: return
    if (closed || choicePending || prompt.fields.size != 1 || prompt.target != ReviewPromptTarget.Profile) return
    val supported =
        (
            prompt.kind == PromptKind.Screening && field == "shift_work" &&
                prompt.shape.kind == ReviewAnswerShape.Boolean &&
                value in listOf(JsonPrimitive(true), JsonPrimitive(false))
        ) ||
            (prompt.kind == PromptKind.AgeBand && field == "age_band" && value.stringValue in prompt.shape.options)
    if (!supported) return
    state = state.copy(profile = JsonObject(state.profile + (field to value)))
    changed()
    advanceChoice { model ->
        val index = (model.state.questionIndex ?: 0) + 1
        if (index < (model.state.questions?.size ?: 0)) {
            model.state = model.state.copy(questionIndex = index)
            model.go(MemberWizardStep.question)
        } else {
            model.state = model.state.copy(checkingAnswers = true)
            model.beginChecking()
        }
    }
}

fun MemberWizardModel.navigateToQuestion(index: Int) {
    if (index !in (state.questions ?: emptyList()).indices) return
    state = state.copy(questionIndex = index)
    go(MemberWizardStep.question)
}

fun MemberWizardModel.backFromQuestion() {
    val index = state.questionIndex ?: 0
    if (index > 0) {
        state = state.copy(questionIndex = index - 1)
        go(MemberWizardStep.question)
    } else {
        go(lastInputStep)
    }
}

val MemberWizardModel.exclusionWasSaved: Boolean get() =
    (
        state.profile["age_band"] == null &&
            state.savedProfile?.get("ageBand") == JsonPrimitive("under_18")
    ) ||
        (state.profile["shift_work"] == null && state.savedProfile?.get("shiftWork") == JsonPrimitive(true))

fun MemberWizardModel.changeExcludedAnswers() {
    if (mode == WizardMode.guest) {
        state =
            state.copy(
                profile = JsonObject(state.profile + mapOf("age_band" to JsonNull, "shift_work" to JsonNull)),
                checkingAnswers = false,
                returnToReady = null,
                questions = guestScreeningQuestions,
                questionIndex = 0,
            )
        go(MemberWizardStep.question)
        return
    }
    val profile = state.profile.toMutableMap()
    for ((field, stored) in listOf("age_band" to "ageBand", "shift_work" to "shiftWork")) {
        val value = profile[field] ?: state.savedProfile?.get(stored)
        if (value == JsonPrimitive(true) ||
            (field == "age_band" && value == JsonPrimitive("under_18"))
        ) {
            profile[field] = JsonNull
        }
    }
    state = state.copy(profile = JsonObject(profile), checkingAnswers = true)
    beginChecking()
}

private fun MemberWizardModel.advanceChoice(advance: (MemberWizardModel) -> Unit) {
    choicePending = true
    val deadline = checkingClock.nowMillis() + 250
    choiceJob =
        scope.launch {
            checkingClock.sleepUntilMillis(deadline)
            currentCoroutineContext().ensureActive()
            if (!closed) {
                choicePending = false
                advance(this@advanceChoice)
            }
        }
}

internal val JsonElement?.stringValue: String? get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
