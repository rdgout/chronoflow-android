package net.chronoflow.feature.wizard.questions

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.ui.WizardCopy

@Immutable
data class WizardQuestionOption(
    val value: JsonPrimitive,
    val title: WizardCopy?,
    val rawTitle: String? = null,
) {
    val tag: String get() = "wizard.choice.question.${value.content}"
}

@Immutable
data class WizardQuestionUi(
    val index: Int,
    val title: CopyLine,
    val field: String?,
    val options: List<WizardQuestionOption>,
    val selected: JsonElement?,
    val why: WizardCopy?,
    val disclaimer: Boolean,
    val pending: Boolean,
    val enabled: Boolean,
) {
    val eyebrow: WizardCopy get() = WizardCopy("A few quick questions")

    companion object {
        fun options(prompt: ApiReviewPrompt): List<WizardQuestionOption> {
            if (prompt.target != ReviewPromptTarget.Profile) return emptyList()
            val field = prompt.fields.singleOrNull()
            return when {
                prompt.kind == PromptKind.AgeBand && field == "age_band" -> {
                    prompt.shape.options.map { value ->
                        val label =
                            when (value) {
                                "under_18" -> "Under 18"
                                "18_64" -> "18–64"
                                "65_plus" -> "65 or older"
                                else -> null
                            }
                        WizardQuestionOption(
                            JsonPrimitive(value),
                            label?.let(::WizardCopy),
                            value.takeIf { label == null },
                        )
                    }
                }

                prompt.kind == PromptKind.Screening && field == "shift_work" &&
                    prompt.shape.kind == ReviewAnswerShape.Boolean -> {
                    listOf(
                        WizardQuestionOption(JsonPrimitive(false), WizardCopy("No")),
                        WizardQuestionOption(JsonPrimitive(true), WizardCopy("Yes")),
                    )
                }

                else -> {
                    emptyList()
                }
            }
        }

        fun from(
            draft: MemberWizardDraft,
            pending: Boolean,
            enabled: Boolean,
        ): WizardQuestionUi? {
            val index = draft.state.questionIndex ?: 0
            val prompt = draft.state.questions?.getOrNull(index) ?: return null
            val field = prompt.fields.firstOrNull()
            return WizardQuestionUi(
                index,
                prompt.text,
                field,
                options(prompt),
                draft.state.profile[field],
                when (field) {
                    "age_band" -> WizardCopy("Light and sleep advice differs by age.")
                    "shift_work" -> WizardCopy("Shift work changes how we can plan your clock.")
                    else -> null
                },
                draft.state.mode == WizardMode.guest && field == "age_band",
                pending,
                enabled,
            )
        }
    }
}
