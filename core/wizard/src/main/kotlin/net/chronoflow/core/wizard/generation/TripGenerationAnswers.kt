package net.chronoflow.core.wizard.generation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.ReviewPromptTarget

object TripGenerationAnswers {
    fun applyingAnswers(
        input: ApiTripReviewInput,
        review: ApiTripReview,
        answers: Map<String, Map<String, JsonElement>>,
    ): ApiTripReviewInput {
        var body = ApiJson.request.encodeToJsonElement(ApiTripInput.serializer(), input.input)
        var profile = input.profile

        fun invalid(): Nothing =
            throw DataFailure.Validation(
                mapOf("review" to FieldMessage("Choose one of the available options.")),
                422,
            )
        for ((id, values) in answers) {
            val prompt = review.prompts.firstOrNull { it.id == id } ?: invalid()
            if (!ProfilePreferences.isSupported(prompt) || !prompt.fields.containsAll(values.keys)) invalid()
            if (prompt.target is ReviewPromptTarget.Unknown) invalid()
            for ((field, value) in ProfilePreferences.promptAnswers(values)) {
                when (prompt.target) {
                    ReviewPromptTarget.Profile -> {
                        profile = JsonObject((profile ?: emptyMap()) + (field to value))
                    }

                    ReviewPromptTarget.Trip -> {
                        var path = field.split('.')
                        val index = prompt.scope.legIndex ?: prompt.scope.stayIndex
                        val journey = prompt.scope.journey
                        if (index != null &&
                            journey != null
                        ) {
                            path = listOf(journey.raw, "legs", index.toString()) + path
                        }
                        body = set(value, path, body)
                    }

                    is ReviewPromptTarget.Unknown -> {
                        invalid()
                    }
                }
            }
        }
        return input.copy(
            input = ApiJson.response.decodeFromJsonElement(ApiTripInput.serializer(), body),
            profile = profile?.let(ProfilePreferences::request),
            identity = review.identity,
        )
    }

    private fun set(
        value: JsonElement,
        path: List<String>,
        root: JsonElement,
    ): JsonElement {
        val first = path.firstOrNull() ?: return value
        return when (root) {
            is JsonObject -> {
                JsonObject(root + (first to set(value, path.drop(1), root[first] ?: JsonNull)))
            }

            is JsonArray -> {
                val index = first.toIntOrNull()?.takeIf { it in root.indices } ?: throw DataFailure.ReviewExpired
                JsonArray(root.mapIndexed { i, child -> if (i == index) set(value, path.drop(1), child) else child })
            }

            else -> {
                throw DataFailure.ReviewExpired
            }
        }
    }
}
