package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class NoHealthDataTests {
    private val retired =
        listOf("bipolar", "eye_disease", "pregnancy", "vte", "sleep_phase_disorder", "oral_contraception")
    private val yes = JsonPrimitive(true)
    private val no = JsonPrimitive(false)

    private fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))

    @Test
    fun preferencesDefaultFalseAndOnlyExplicitMovementPreferenceCarriesOver() {
        val defaults = ProfilePreferences.bodyClock(obj("vte" to yes))
        assertThat(defaults["flightMovementBreaks"]).isEqualTo(no)
        assertThat(defaults["excludeCaffeine"]).isEqualTo(no)
        val legacy = ProfilePreferences.bodyClock(obj("caffeineRuledOut" to yes))
        assertThat(legacy["excludeCaffeine"]).isEqualTo(yes)
        assertThat(legacy["caffeineRuledOut"]).isNull()
        val current =
            ProfilePreferences.bodyClock(
                obj("excludeCaffeine" to no, "caffeineRuledOut" to yes, "flightMovementBreaks" to yes),
            )
        assertThat(current["excludeCaffeine"]).isEqualTo(no)
        assertThat(current["flightMovementBreaks"]).isEqualTo(yes)
    }

    @Test
    fun legacyReviewDecodingDiscardsRetiredPrompts() {
        val raw = fixtureObject("wizard/needs-answers.json")
        val prompts = (raw["prompts"] as JsonArray).toMutableList()
        val shift = (prompts[1] as JsonObject).with("answer", obj("shift_work" to no, "pregnancy" to no))
        prompts[1] = shift
        for (key in retired + "caffeine_ruled_out") {
            prompts +=
                shift
                    .with("id", JsonPrimitive("screening:$key"))
                    .with("fields", JsonArray(listOf(JsonPrimitive(key))))
                    .with("answer", obj(key to no))
        }
        val text = ApiJson.response.encodeToString(JsonElement.serializer(), raw.with("prompts", JsonArray(prompts)))
        val review = ApiJson.response.decodeFromString(ApiTripReview.serializer(), text)
        assertThat(review.prompts.map { it.fields }).containsExactly(listOf("age_band"), listOf("shift_work"))
        assertThat(review.prompts[1].answer).isEqualTo(mapOf("shift_work" to no))
        val encoded =
            ApiJson.response.parseToJsonElement(
                ApiJson.response.encodeToString(ApiTripReview.serializer(), review),
            ) as JsonObject
        assertThat(encoded["prompts"] as JsonArray).hasSize(2)
    }

    @Test
    fun tripCacheDropsRetiredValuesAndPromptsWhilePreservingUnknownPlanFields() {
        val raw = fixtureObject("trips/detail.json")
        val retiredPrompt =
            obj(
                "kind" to JsonPrimitive("screening"),
                "question" to JsonPrimitive("bipolar"),
                "text" to obj("key" to JsonPrimitive("retired"), "args" to JsonArray(emptyList())),
                "required" to yes,
            )
        val plan =
            (raw["plan"] as JsonObject)
                .with("futureField", obj("keep" to JsonPrimitive("value"), "pregnancy" to no))
                .with("prompts", JsonArray(listOf(retiredPrompt)))
        val bytes =
            ApiJson.response
                .encodeToString(
                    JsonElement.serializer(),
                    raw.with("plan", plan),
                ).encodeToByteArray()
        val response = TripDetailResponse.decode(bytes)
        val cached = ApiJson.response.parseToJsonElement(response.data.decodeToString()) as JsonObject
        val cachedPlan = cached["plan"] as JsonObject
        assertThat(cachedPlan["futureField"]).isEqualTo(obj("keep" to JsonPrimitive("value")))
        assertThat(cachedPlan["prompts"]).isEqualTo(JsonArray(emptyList()))
        assertThat(response.detail.plan.prompts).isEmpty()
        val clean = Fixtures.bytes("trips/detail.json")
        assertThat(TripDetailResponse.decode(clean).data).isSameInstanceAs(clean)
    }
}
