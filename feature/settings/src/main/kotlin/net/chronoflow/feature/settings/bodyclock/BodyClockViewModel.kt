package net.chronoflow.feature.settings.bodyclock

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.wizard.model.WizardClockRounding
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.section

@Immutable
data class BodyClockSectionState(
    val group: BodyClockGroup,
    val answers: JsonObject,
    val errors: Map<String, String> = emptyMap(),
    val saved: Boolean = false,
    val missing: Set<String> = emptySet(),
)

@Immutable
data class BodyClockScroll(
    val id: Long,
    val group: BodyClockGroup,
    val fieldKey: String? = null,
)

@Immutable
data class BodyClockTimeSheet(
    val field: String,
    val initialHour: Int,
    val initialMinute: Int,
)

@Immutable
data class BodyClockUiState(
    val pages: SettingsPagesState = SettingsPagesState(),
    val editor: SettingsBodyClockModel.State? = null,
    val sections: Map<BodyClockGroup, BodyClockSectionState> = emptyMap(),
    val readiness: BodyClockReadiness? = null,
    val scroll: BodyClockScroll? = null,
    val timeSheet: BodyClockTimeSheet? = null,
)

sealed interface BodyClockEvent {
    data object Back : BodyClockEvent

    data object Retry : BodyClockEvent

    data object FirstMissing : BodyClockEvent

    data object DismissTime : BodyClockEvent

    data class Choice(
        val group: BodyClockGroup,
        val field: String,
        val id: String,
    ) : BodyClockEvent

    data class Preference(
        val group: BodyClockGroup,
        val field: String,
        val checked: Boolean,
    ) : BodyClockEvent

    data class Drink(
        val field: String,
        val count: Int,
    ) : BodyClockEvent

    data class Rating(
        val value: String,
    ) : BodyClockEvent

    data class OpenTime(
        val field: String,
    ) : BodyClockEvent

    data class ConfirmTime(
        val field: String,
        val hour: Int,
        val minute: Int,
    ) : BodyClockEvent

    data class ScrollHandled(
        val id: Long,
    ) : BodyClockEvent
}

class BodyClockViewModel(
    deps: SettingsDependencies,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private var model: SettingsBodyClockModel? = null
    private var page = SettingsPagesState()
    private var sectionAvailable = false
    private var readiness: JsonObject? = null
    private var scrollId = 0L
    private val mutableUi = MutableStateFlow(BodyClockUiState())
    val ui = mutableUi.asStateFlow()

    init {
        pages.start()
        viewModelScope.launch {
            pages.ui.collect { value ->
                page = value
                readiness = value.section("readiness")
                val section = value.section("bodyClock")
                sectionAvailable = section != null
                if (section != null) {
                    val editor = model
                    if (editor == null) {
                        val created =
                            SettingsBodyClockModel(section, onSaved = ::saved) { patch ->
                                pages.enqueueSettings(SettingsSection.BodyClock, patch)
                            }
                        model = created
                        viewModelScope.launch { created.state.collect { render() } }
                    } else {
                        editor.receive(section)
                    }
                }
                render()
            }
        }
    }

    fun event(event: BodyClockEvent) {
        when (event) {
            BodyClockEvent.Back -> {}

            BodyClockEvent.Retry -> {
                viewModelScope.launch { pages.retry() }
            }

            BodyClockEvent.FirstMissing -> {
                firstMissing()
            }

            BodyClockEvent.DismissTime -> {
                mutableUi.update { it.copy(timeSheet = null) }
            }

            is BodyClockEvent.Choice -> {
                val answers = model?.state?.value?.answers ?: return
                change(
                    event.group,
                    if (event.field in setOf("shiftWork", "caffeineSensitive")) {
                        BodyClockChoice.yesNoPatch(event.field, event.id)
                    } else {
                        BodyClockChoice.patch(answers, event.field, event.id)
                    },
                )
            }

            is BodyClockEvent.Preference -> {
                change(event.group, mapOf(event.field to JsonPrimitive(event.checked)))
            }

            is BodyClockEvent.Drink -> {
                if (event.field in SettingsBodyClockModel.cups) {
                    change(BodyClockGroup.Caffeine, mapOf(event.field to JsonPrimitive(event.count)))
                }
            }

            is BodyClockEvent.Rating -> {
                change(
                    BodyClockGroup.Light,
                    mapOf("blueBlockerMdfd" to JsonPrimitive(event.value.replace(',', '.'))),
                )
            }

            is BodyClockEvent.OpenTime -> {
                openTime(event.field)
            }

            is BodyClockEvent.ConfirmTime -> {
                confirmTime(event)
            }

            is BodyClockEvent.ScrollHandled -> {
                mutableUi.update {
                    if (it.scroll?.id == event.id) it.copy(scroll = null) else it
                }
            }
        }
    }

    fun change(
        group: BodyClockGroup,
        patch: Map<String, JsonElement>,
    ) {
        if (!group.fields.containsAll(patch.keys)) return
        viewModelScope.launch {
            val editor = model ?: return@launch
            editor.change(group, patch)
        }
    }

    private fun saved(group: BodyClockGroup) {
        render()
        val request = BodyClockScroll(++scrollId, group)
        mutableUi.update { it.copy(scroll = request) }
    }

    private fun firstMissing() {
        val field = BodyClockReadiness.firstMissing(readiness)
        val group = BodyClockGroup.entries.firstOrNull { field in it.fields } ?: BodyClockGroup.About
        val request = BodyClockScroll(++scrollId, group, field)
        mutableUi.update { it.copy(scroll = request) }
    }

    private fun openTime(field: String) {
        if (field !in timeFields) return
        val editor = model ?: return
        val stored =
            editor.state.value.answers[field]
                .stringValue()
        val value = stored?.takeIf { SettingsBodyClockModel.sleepMinutes(it, it) != null } ?: "12:00"
        val sheet = BodyClockTimeSheet(field, value.substring(0, 2).toInt(), value.substring(3, 5).toInt())
        mutableUi.update { it.copy(timeSheet = sheet) }
    }

    private fun confirmTime(event: BodyClockEvent.ConfirmTime) {
        if (ui.value.timeSheet?.field != event.field) return
        val rounded = WizardClockRounding.roundToStep(event.hour, event.minute) ?: return
        mutableUi.update { it.copy(timeSheet = null) }
        change(BodyClockGroup.Sleep, mapOf(event.field to JsonPrimitive(rounded)))
    }

    private fun render() {
        val state = model?.state?.value.takeIf { sectionAvailable }
        mutableUi.update {
            it.copy(
                pages = page,
                editor = state,
                sections =
                    state
                        ?.let { state ->
                            sections(state, BodyClockReadiness.missingFields(readiness).toSet())
                        }.orEmpty(),
                readiness = BodyClockReadiness.of(readiness),
            )
        }
    }

    override fun onCleared() {
        pages.stop()
    }

    companion object {
        private val timeFields = setOf("habitualBed", "habitualWake", "freeDayBed", "freeDayWake")

        internal fun sections(
            state: SettingsBodyClockModel.State,
            missing: Set<String>,
        ): Map<BodyClockGroup, BodyClockSectionState> =
            BodyClockGroup.entries.associateWith { group ->
                BodyClockSectionState(
                    group,
                    JsonObject(state.answers.filterKeys { it in group.fields }),
                    state.errors.filterKeys { it in group.fields },
                    group in state.saved,
                    missing.intersect(group.fields.toSet()),
                )
            }

        val Factory =
            viewModelFactory {
                initializer {
                    BodyClockViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
