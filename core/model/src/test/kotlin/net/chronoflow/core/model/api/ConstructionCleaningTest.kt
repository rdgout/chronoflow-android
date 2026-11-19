package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import org.junit.Test

class ConstructionCleaningTest {
    @Test
    fun accountSettingsBuiltInCodeCleansBodyClock() {
        val settings =
            AccountSettings(
                bodyClock = JsonObject(mapOf("invalidDrafts" to JsonArray(emptyList()))),
                sleepAnswered = true,
                preferences = JsonObject(emptyMap()),
                reminders = JsonObject(emptyMap()),
                planCredits = JsonObject(emptyMap()),
                policies = AccountPolicies("1", "1"),
            )
        assertThat(settings.bodyClock["invalidDrafts"]).isEqualTo(JsonObject(emptyMap()))
        assertThat(settings.bodyClock["excludeCaffeine"]).isEqualTo(JsonPrimitive(false))
    }

    @Test
    fun reviewPromptBuiltInCodeDropsAnswersOutsideItsFields() {
        val prompt =
            ApiReviewPrompt(
                id = "p",
                kind = PromptKind.AgeBand,
                blocking = false,
                required = false,
                text = CopyLine("k", emptyList()),
                scope = ApiReviewPromptScope(null, null, null),
                target = ReviewPromptTarget.Profile,
                fields = listOf("age_band"),
                answer = mapOf("age_band" to JsonPrimitive("a"), "other" to JsonPrimitive("b")),
                shape = ApiReviewPromptShape(ReviewAnswerShape.Options, emptyList(), null, null),
                side = null,
                deviceMin = null,
            )
        assertThat(prompt.answer).containsOnly("age_band" to JsonPrimitive("a"))
    }
}
