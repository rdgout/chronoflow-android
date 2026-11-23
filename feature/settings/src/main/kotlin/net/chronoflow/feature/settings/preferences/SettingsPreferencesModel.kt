package net.chronoflow.feature.settings.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.feature.settings.settingsAttempt

data class SettingsTimelineGroup(
    val title: String,
    val types: List<String>,
) {
    companion object {
        val all =
            listOf(
                SettingsTimelineGroup("Sleep", listOf("sleep", "nap", "rest")),
                SettingsTimelineGroup("Light", listOf("seek_light", "avoid_light", "light_ok", "light_neutral")),
                SettingsTimelineGroup(
                    "Caffeine",
                    listOf("caffeine_serving", "caffeine_routine", "caffeine_ok", "no_caffeine"),
                ),
                SettingsTimelineGroup(
                    "Travel",
                    listOf("flight", "layover", "transfer", "movement_break", "zone_change"),
                ),
            )
    }
}

class SettingsPreferencesModel(
    value: JsonObject,
    private val save: suspend (JsonObject) -> Unit,
) {
    data class State(
        val value: JsonObject,
        val saved: Boolean = false,
        val failure: Boolean = false,
    ) {
        val timeFormat: String
            get() = (value["timeFormat"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "24"
        val hidden: List<String> get() =
            (value["hiddenCategories"] as? JsonArray)
                ?.mapNotNull {
                    (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content
                }.orEmpty()

        fun shown(group: SettingsTimelineGroup): Boolean = !group.types.all { it in hidden }
    }

    private val mutableState = MutableStateFlow(State(value))
    val state = mutableState.asStateFlow()
    private val edits = mutableMapOf<String, JsonElement>()
    private val saves = Mutex()
    val hidden: List<String> get() = state.value.hidden

    fun shown(group: SettingsTimelineGroup): Boolean = state.value.shown(group)

    fun receive(value: JsonObject) {
        edits.entries.removeAll { (key, entry) -> value[key] == entry }
        mutableState.update { it.copy(value = JsonObject(value + edits)) }
    }

    suspend fun setTimeFormat(format: String) {
        if (format !in setOf("12", "24")) {
            mutableState.update { it.copy(failure = true) }
            return
        }
        change("timeFormat", JsonPrimitive(format), "time_format")
    }

    suspend fun setShown(
        shown: Boolean,
        group: SettingsTimelineGroup,
    ) {
        val next = hidden.filter { it !in group.types } + if (shown) emptyList() else group.types
        change("hiddenCategories", JsonArray(next.map(::JsonPrimitive)), "hidden_categories")
    }

    private suspend fun change(
        key: String,
        entry: JsonElement,
        request: String,
    ) {
        edits[key] = entry
        mutableState.update { it.copy(value = JsonObject(it.value + (key to entry)), saved = false) }
        saves.withLock {
            settingsAttempt(
                failed = { mutableState.update { it.copy(failure = true) } },
                cancelled = {},
            ) {
                save(JsonObject(mapOf(request to entry)))
                mutableState.update { it.copy(saved = true, failure = false) }
            }
        }
    }
}
