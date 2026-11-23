package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget

internal fun wizardPrompt(
    id: String = "age_band",
    kind: PromptKind = PromptKind.AgeBand,
    blocking: Boolean = true,
    fields: List<String> = listOf("age_band"),
    target: ReviewPromptTarget = ReviewPromptTarget.Profile,
    shape: ApiReviewPromptShape =
        ApiReviewPromptShape(ReviewAnswerShape.Options, listOf("under_18", "18_64", "65_plus"), null, null),
    scope: ApiReviewPromptScope = ApiReviewPromptScope(null, null, null),
    answer: Map<String, JsonPrimitive> = emptyMap(),
    required: Boolean = blocking,
): ApiReviewPrompt =
    ApiReviewPrompt(
        id,
        kind,
        blocking,
        required,
        CopyLine("question", emptyList()),
        scope,
        target,
        fields,
        answer,
        shape,
        null,
        null,
    )
