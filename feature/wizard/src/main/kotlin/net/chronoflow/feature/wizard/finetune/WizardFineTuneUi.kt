package net.chronoflow.feature.wizard.finetune

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerializationException
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardJourney
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.TripWizardUiState
import net.chronoflow.feature.wizard.answers.WizardPromptLabels
import net.chronoflow.feature.wizard.sleep.WizardTimeSheetUi
import net.chronoflow.feature.wizard.ui.WizardCopy

@Immutable
sealed interface AnswerText {
    data class Literal(
        val text: String,
    ) : AnswerText

    data class Copy(
        val copy: WizardCopy,
    ) : AnswerText
}

@Immutable
data class ExtraRowUi(
    val promptId: String,
    val kind: PromptKind,
    val index: Int,
    val labelKey: String,
    val suffix: String?,
    val required: Boolean,
    val answer: AnswerText?,
    val error: FieldMessage?,
) {
    val tag: String get() = "wizard.extra.${kind.raw}.$index"
}

internal fun promptLegIndex(
    prompt: ApiReviewPrompt,
    form: WizardForm,
): Int =
    (if (prompt.scope.journey == TripDirection.Return) form.returnBoundary else 0) +
        (prompt.scope.legIndex ?: prompt.scope.stayIndex ?: 0)

internal fun promptLabel(
    prompt: ApiReviewPrompt,
    form: WizardForm,
): Pair<String, String?> {
    val label = WizardPromptLabels.key(prompt)
    val leg = form.legs.getOrNull(promptLegIndex(prompt, form))
    if (form.legs.size <= 1 || leg == null) return label to null
    return when (prompt.kind) {
        PromptKind.SeatSide -> label to " · ${leg.fromIata} → ${leg.toIata}"
        PromptKind.Room -> "Dark room" to " · ${leg.toCity ?: leg.toIata}"
        PromptKind.LateCheckOut -> label to " · ${leg.toCity ?: leg.toIata}"
        else -> label to null
    }
}

@Immutable
data class WizardFineTuneUi(
    val rows: List<ExtraRowUi>,
    val preparationDays: Int,
    val showSleepSwitch: Boolean,
    val sleepOverride: Boolean,
    val overrideBed: String?,
    val overrideWake: String?,
    val preferences: Map<String, Boolean>,
    val fineTuneErrors: List<Pair<String, FieldMessage>>,
    val reviewing: Boolean,
    val timeFormat: TimeFormat,
    val openTime: WizardTimeSheetUi? = null,
) {
    val none: Boolean get() = rows.isEmpty()
    val title: WizardCopy get() = WizardCopy("Fine-tune your plan")
    val subtitle: WizardCopy get() =
        WizardCopy(
            if (none) {
                "No extra questions for this trip. You can still change these."
            } else {
                "All optional. The ones marked help your plan most."
            },
        )
    val button: WizardCopy get() =
        WizardCopy(
            if (reviewing) {
                "Checking…"
            } else if (none) {
                "Done"
            } else {
                "Continue"
            },
        )

    companion object {
        fun from(
            active: TripWizardUiState.Active,
            format: TimeFormat,
            openTime: String?,
        ): WizardFineTuneUi? {
            val draft = active.wizard.draft
            if (draft.state.step != MemberWizardStep.fineTune) return null
            val form = draft.form
            val input = form.reviewInput(form.identity, draft.state.profile.takeIf { it.isNotEmpty() })
            val rows =
                active.optionalPrompts.mapIndexed { index, prompt ->
                    val answer = answerText(prompt, input)
                    val (label, suffix) = promptLabel(prompt, form)
                    val error =
                        WizardPresentation.promptFieldError(
                            prompt,
                            form,
                            WizardJourney.errors(draft.fieldErrors, form),
                        )
                    ExtraRowUi(prompt.id, prompt.kind, index, label, suffix, prompt.required, answer, error)
                }
            return WizardFineTuneUi(
                rows = rows,
                preparationDays = form.preparation.days,
                showSleepSwitch = active.allowsSleepOverride ?: (draft.state.mode == WizardMode.member),
                sleepOverride = form.override.enabled,
                overrideBed = form.override.habitualBed,
                overrideWake = form.override.habitualWake,
                preferences = active.preferences,
                fineTuneErrors =
                    draft.fieldErrors
                        .filterKeys {
                            it.startsWith("override.") || it.startsWith("preparation.")
                        }.toSortedMap()
                        .toList(),
                reviewing = active.wizard.reviewing,
                timeFormat = format,
                openTime =
                    openTime
                        ?.takeIf {
                            it in setOf("override.habitual_bed", "override.habitual_wake")
                        }?.let { field ->
                            val stored =
                                if (field.endsWith("habitual_bed")) {
                                    form.override.habitualBed
                                } else {
                                    form.override.habitualWake
                                }
                            WizardTimeSheetUi(field, stored, minutePrecision = true)
                        },
            )
        }
    }
}

private fun answerText(
    prompt: ApiReviewPrompt,
    input: net.chronoflow.core.model.api.ApiTripReviewInput,
): AnswerText? =
    try {
        var translated: WizardCopy? = null
        val raw =
            WizardPresentation.promptAnswerText(prompt, input, Language.En) { key, args ->
                translated = WizardCopy(key, args)
                key
            }
        val copy = translated
        when {
            copy == null -> AnswerText.Literal(raw)
            copy.key == "Add" -> null
            else -> AnswerText.Copy(copy)
        }
    } catch (_: DataFailure) {
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
