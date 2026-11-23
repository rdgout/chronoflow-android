package net.chronoflow.feature.wizard.checking

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.auth.LockoutWait
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.TripWizardUiState
import net.chronoflow.feature.wizard.ui.WizardCopy

enum class CheckLineState { Done, Busy, Open }

enum class CheckLine(
    val key: String,
) {
    YourAnswers("Your answers"),
    FlightsAndTimeZones("Flights and time zones"),
    YourSavedAnswers("Your saved answers"),
    WhatWeCanTailor("What we can tailor"),
}

enum class CheckFailureKind(
    val tag: String,
) {
    Offline("offline"),
    Server("server"),
    Throttle("throttle"),
}

enum class CheckingOutcome { Success, Warning, Error }

@Immutable
data class WizardHapticCue(
    val outcome: CheckingOutcome,
    val sequence: Int,
)

@Immutable
data class WizardCheckingUi(
    val title: WizardCopy,
    val from: String,
    val to: String,
    val lines: List<Pair<CheckLine, CheckLineState>>,
    val slow: Boolean,
    val failure: CheckFailureKind?,
    val retrySeconds: Int,
    val editing: Boolean,
) {
    val subtitle: WizardCopy? get() = if (failure == null) WizardCopy("This takes a few seconds.") else null
    val button: WizardCopy get() =
        if (retrySeconds > 0) {
            WizardCopy("Try again in {{count}} s", mapOf("count" to retrySeconds.toString()))
        } else {
            WizardCopy("Try again")
        }
    val failureTitle: WizardCopy get() =
        WizardCopy(
            when (failure) {
                CheckFailureKind.Offline -> "Checking needs a connection"
                CheckFailureKind.Throttle -> "Wait a moment"
                else -> "We couldn't check your trip"
            },
        )
    val failureBody: WizardCopy get() =
        when (failure) {
            CheckFailureKind.Offline -> {
                WizardCopy(
                    if (editing) {
                        "Connect to the internet and try again. If you close now, your trip stays as it was."
                    } else {
                        "Connect to the internet and try again."
                    },
                )
            }

            CheckFailureKind.Throttle -> {
                if (retrySeconds > 0) {
                    val wait = LockoutWait(retrySeconds)
                    WizardCopy(wait.localizationKey, mapOf("count" to wait.count.toString()))
                } else {
                    WizardCopy("Too many attempts. Please try again later.")
                }
            }

            else -> {
                WizardCopy(
                    if (editing) {
                        "Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was."
                    } else {
                        "Something went wrong on our side. Try again in a moment."
                    },
                )
            }
        }

    fun lineLabel(
        index: Int,
        translatedLine: String,
    ): WizardCopy =
        WizardCopy(
            when (lines[index].second) {
                CheckLineState.Done -> "{{line}}, done"
                CheckLineState.Busy -> "{{line}}, in progress"
                CheckLineState.Open -> "{{line}}"
            },
            mapOf("line" to translatedLine),
        )

    companion object {
        fun from(active: TripWizardUiState.Active): WizardCheckingUi? {
            val draft = active.wizard.draft
            if (draft.state.step != MemberWizardStep.checking) return null
            val progress = active.wizard.checking
            val lines =
                if (draft.state.checkingAnswers == true) {
                    listOf(CheckLine.YourAnswers, CheckLine.FlightsAndTimeZones, CheckLine.WhatWeCanTailor)
                } else {
                    listOf(
                        CheckLine.FlightsAndTimeZones,
                        if (draft.state.mode == WizardMode.guest) CheckLine.YourAnswers else CheckLine.YourSavedAnswers,
                        CheckLine.WhatWeCanTailor,
                    )
                }
            return WizardCheckingUi(
                WizardCopy(
                    active.checkingTitle ?: when {
                        active.isEditing -> "Checking your changes"
                        draft.state.checkingAnswers == true -> "Checking your answers"
                        else -> "Checking your trip"
                    },
                ),
                draft.form.legs
                    .firstOrNull()
                    ?.fromIata
                    .orEmpty(),
                draft.form.legs
                    .getOrNull(draft.form.returnBoundary - 1)
                    ?.toIata
                    .orEmpty(),
                lines.mapIndexed { i, line ->
                    line to
                        when {
                            progress.ticks > i -> CheckLineState.Done
                            progress.ticks == i -> CheckLineState.Busy
                            else -> CheckLineState.Open
                        }
                },
                progress.slow,
                when (progress.failure) {
                    null -> null
                    DataFailure.Offline -> CheckFailureKind.Offline
                    is DataFailure.Throttled -> CheckFailureKind.Throttle
                    else -> CheckFailureKind.Server
                },
                active.wizard.retrySeconds,
                active.isEditing,
            )
        }
    }
}

internal fun checkingOutcome(
    previous: net.chronoflow.core.wizard.model.MemberWizardSnapshot?,
    next: net.chronoflow.core.wizard.model.MemberWizardSnapshot,
): CheckingOutcome? {
    if (previous == null) return null
    if (next.draft.state.saveFailure != null && previous.draft.state.saveFailure != next.draft.state.saveFailure) {
        return CheckingOutcome.Error
    }
    if (previous.draft.state.step == MemberWizardStep.checking && previous.checking.ticks == 3 &&
        previous.checking.failure == null && next.draft.state.step != MemberWizardStep.checking
    ) {
        return if (next.draft.state.step ==
            MemberWizardStep.excluded
        ) {
            CheckingOutcome.Warning
        } else {
            CheckingOutcome.Success
        }
    }
    return if (previous.checking.failure == null && next.checking.failure != null) CheckingOutcome.Error else null
}
