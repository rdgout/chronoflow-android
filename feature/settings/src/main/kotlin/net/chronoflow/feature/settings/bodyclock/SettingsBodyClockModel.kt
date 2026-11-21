package net.chronoflow.feature.settings.bodyclock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.feature.settings.settingsAttempt
import kotlin.math.floor

enum class BodyClockGroup(
    val raw: String,
    val title: String,
    val fields: List<String>,
) {
    About("about", "About you", listOf("chronotype", "ageBand")),
    Sleep(
        "sleep",
        "Sleep schedule",
        listOf("habitualBed", "habitualWake", "freeDayStatus", "freeDayBed", "freeDayWake"),
    ),
    Caffeine(
        "caffeine",
        "Caffeine",
        listOf(
            "caffeineHabit",
            "cupsFilterCoffee",
            "cupsTea",
            "cupsCola",
            "cupsEnergyDrink",
            "caffeineSensitive",
            "caffeineNonUserOptIn",
            "excludeCaffeine",
        ),
    ),
    Light("light", "Light equipment", listOf("lightDevice", "blueBlockers", "blueBlockerMdfd")),
    Health("health", "Safety checks", listOf("shiftWork", "flightMovementBreaks")),
    ;

    companion object {
        fun requestKey(field: String): String =
            buildString {
                field.forEach { letter ->
                    if (letter.isUpperCase()) append('_')
                    append(letter.lowercaseChar())
                }
            }
    }
}

class SettingsBodyClockModel(
    answers: JsonObject,
    private val save: suspend (JsonObject) -> Unit,
) {
    data class State(
        val answers: JsonObject,
        val errors: Map<String, String> = emptyMap(),
        val saved: Set<BodyClockGroup> = emptySet(),
        val failure: Boolean = false,
    )

    private val mutableState = MutableStateFlow(State(ProfilePreferences.bodyClock(answers)))
    val state = mutableState.asStateFlow()
    private val edits = mutableMapOf<String, JsonElement>()
    private val revisions = mutableMapOf<BodyClockGroup, Long>()
    private val saves = Mutex()

    fun receive(value: JsonObject) {
        edits.entries.removeAll { (key, entry) -> value[key] == entry }
        mutableState.update { it.copy(answers = JsonObject(ProfilePreferences.bodyClock(value) + edits)) }
    }

    suspend fun change(
        group: BodyClockGroup,
        patch: Map<String, JsonElement>,
    ) {
        if (!group.fields.containsAll(patch.keys)) return
        val answers = (state.value.answers + patch).toMutableMap()
        edits.putAll(patch)
        normalize(group, answers)
        val revision = (revisions[group] ?: 0) + 1
        revisions[group] = revision
        val value = JsonObject(answers)
        val invalid = validate(group, value)
        mutableState.update {
            it.copy(
                answers = value,
                saved = it.saved - group,
                errors =
                    it.errors.filterKeys { key -> key !in group.fields } + invalid,
            )
        }
        if (invalid.isNotEmpty()) return
        val body = JsonObject(group.fields.associate { BodyClockGroup.requestKey(it) to (value[it] ?: JsonNull) })
        saves.withLock {
            settingsAttempt(
                failed = { mutableState.update { it.copy(failure = true) } },
                cancelled = {},
            ) {
                save(body)
                mutableState.update {
                    it.copy(saved = if (revisions[group] == revision) it.saved + group else it.saved, failure = false)
                }
            }
        }
    }

    private fun normalize(
        group: BodyClockGroup,
        answers: MutableMap<String, JsonElement>,
    ) {
        fun set(
            key: String,
            value: JsonElement,
        ) {
            answers[key] = value
            edits[key] = value
        }
        if (group == BodyClockGroup.About && answers["ageBand"] == JsonPrimitive("unanswered")) set("ageBand", JsonNull)
        if (group == BodyClockGroup.Caffeine &&
            answers["caffeineHabit"] == JsonPrimitive("unknown")
        ) {
            set("caffeineHabit", JsonNull)
        }
        if (group == BodyClockGroup.Sleep && answers["freeDayStatus"].stringValue() != "declared") {
            set("freeDayBed", JsonNull)
            set("freeDayWake", JsonNull)
        }
        if (group == BodyClockGroup.Caffeine) {
            cups.forEach { key ->
                if (answers["caffeineHabit"].stringValue() != "cups" ||
                    (answers[key] as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull == 0.0
                ) {
                    set(key, JsonNull)
                }
            }
        }
        if (group == BodyClockGroup.Light &&
            answers["blueBlockers"].stringValue() != "rated"
        ) {
            set("blueBlockerMdfd", JsonNull)
        }
        val drafts = (answers["invalidDrafts"] as? JsonObject).orEmpty().toMutableMap()
        group.fields.filter { it in edits }.forEach { drafts.remove(BodyClockGroup.requestKey(it)) }
        answers["invalidDrafts"] = JsonObject(drafts)
    }

    companion object {
        val cups = listOf("cupsFilterCoffee", "cupsTea", "cupsCola", "cupsEnergyDrink")
        private val choices =
            mapOf(
                "chronotype" to setOf("early", "intermediate", "late"),
                "ageBand" to setOf("under_18", "18_64", "65_plus"),
                "freeDayStatus" to setOf("same_as_workdays", "declared", "unconfirmed"),
                "caffeineHabit" to setOf("cups", "non_user"),
                "lightDevice" to setOf("none", "box", "glasses"),
                "blueBlockers" to setOf("no", "unrated", "rated"),
            )
        private val clockPattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        private val ratingPattern = Regex("^\\d+(?:\\.\\d)?$")
        const val CHOICE_ERROR = "Choose one of the available options."
        const val SLEEP_ERROR = "Check your bedtime and wake-up time."
        const val CUPS_ERROR = "Add at least one drink to save this answer."
        const val RATING_ERROR = "Enter a rating between 0.1 and 9.9."

        fun validate(
            group: BodyClockGroup,
            answers: JsonObject,
        ): Map<String, String> {
            val errors = mutableMapOf<String, String>()
            group.fields.forEach { key ->
                val value = answers[key]
                if (value != null && value != JsonNull &&
                    choices[key]?.contains(value.stringValue()) == false
                ) {
                    errors[key] = CHOICE_ERROR
                }
            }
            if (group == BodyClockGroup.Sleep) {
                val pairs =
                    listOf("habitualBed" to "habitualWake") +
                        if (answers["freeDayStatus"].stringValue() ==
                            "declared"
                        ) {
                            listOf("freeDayBed" to "freeDayWake")
                        } else {
                            emptyList()
                        }
                pairs.forEach { (bed, wake) ->
                    if (sleepMinutes(answers[bed].stringValue(), answers[wake].stringValue()) !in 180..840) {
                        errors[bed] = SLEEP_ERROR
                        errors[wake] = SLEEP_ERROR
                    }
                }
            }
            if (group == BodyClockGroup.Caffeine) {
                val counts = cups.map { answers[it] ?: JsonNull }
                val valid =
                    counts.all { value ->
                        value == JsonNull ||
                            (
                                (value as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull?.let {
                                    it in 0.0..20.0 &&
                                        it == floor(it)
                                } ==
                                    true
                            )
                    }
                if (!valid) {
                    errors["cupsFilterCoffee"] = CHOICE_ERROR
                } else if (answers["caffeineHabit"].stringValue() == "cups" &&
                    counts.none { (it as? JsonPrimitive)?.doubleOrNull?.let { n -> n > 0 } == true }
                ) {
                    errors["cupsFilterCoffee"] = CUPS_ERROR
                }
            }
            if (group == BodyClockGroup.Light && answers["blueBlockers"].stringValue() == "rated") {
                val value = answers["blueBlockerMdfd"].stringValue().orEmpty()
                if (!ratingPattern.matches(value) ||
                    (value.toDoubleOrNull() ?: 0.0) !in 0.1..9.9
                ) {
                    errors["blueBlockerMdfd"] = RATING_ERROR
                }
            }
            group.fields.forEach { key ->
                if (BodyClockGroup.requestKey(key) in (answers["invalidDrafts"] as? JsonObject).orEmpty()) {
                    errors[key] = if (group == BodyClockGroup.Sleep) SLEEP_ERROR else CHOICE_ERROR
                }
            }
            return errors
        }

        fun sleepMinutes(
            bed: String?,
            wake: String?,
        ): Int? {
            fun minutes(value: String?): Int? {
                if (value == null || !clockPattern.matches(value)) return null
                val parts = value.split(":")
                return parts[0].toInt() * 60 + parts[1].toInt()
            }
            return ((minutes(wake) ?: return null) - (minutes(bed) ?: return null) + 1440) % 1440
        }
    }
}

internal fun JsonElement?.stringValue(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
