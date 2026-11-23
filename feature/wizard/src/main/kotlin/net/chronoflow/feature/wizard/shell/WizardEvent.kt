package net.chronoflow.feature.wizard.shell

import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.WizardAirportSheet

sealed interface WizardEvent {
    data class RevisitFromReady(
        val row: net.chronoflow.feature.wizard.ready.ReadyRow,
    ) : WizardEvent

    data object ReadyTick : WizardEvent

    data object ReadyForeground : WizardEvent

    data class LanguageChanged(
        val language: net.chronoflow.core.model.contracts.Language,
    ) : WizardEvent

    data class OpenPrompt(
        val id: String?,
    ) : WizardEvent

    data class SetPreparationDays(
        val days: Int,
    ) : WizardEvent

    data class SetSleepOverride(
        val enabled: Boolean,
    ) : WizardEvent

    data class SetPreference(
        val field: String,
        val value: Boolean,
    ) : WizardEvent

    data object CloseFromFooter : WizardEvent

    data object CloseNow : WizardEvent

    data class AnswerQuestion(
        val value: kotlinx.serialization.json.JsonElement,
    ) : WizardEvent

    data class UpdateAnswer(
        val field: String,
        val value: kotlinx.serialization.json.JsonElement,
    ) : WizardEvent

    data object AnswerDone : WizardEvent

    data object CancelAnswer : WizardEvent

    data class SetSleep(
        val field: String,
        val value: kotlinx.serialization.json.JsonElement,
    ) : WizardEvent

    data class OpenTimeSheet(
        val field: String,
    ) : WizardEvent

    data object DismissTimeSheet : WizardEvent

    data class ConfirmTimeSheet(
        val field: String,
        val hour: Int,
        val minute: Int,
        val touched: Boolean,
    ) : WizardEvent

    data class OpenFlightPicker(
        val picker: net.chronoflow.feature.wizard.times.WizardPicker,
    ) : WizardEvent

    data object DismissFlightPicker : WizardEvent

    data class ChooseReturn(
        val kind: String,
    ) : WizardEvent

    data class SetDatePart(
        val index: Int,
        val arrival: Boolean,
        val date: java.time.LocalDate,
    ) : WizardEvent

    data class SetTimePart(
        val index: Int,
        val arrival: Boolean,
        val hour: Int,
        val minute: Int,
    ) : WizardEvent

    data class OpenAirport(
        val sheet: WizardAirportSheet,
    ) : WizardEvent

    data object CancelAirport : WizardEvent

    data class ChooseAirport(
        val airport: ApiAirportSuggestion,
        val sheet: WizardAirportSheet,
    ) : WizardEvent

    data object SwapAirports : WizardEvent

    data class RemoveStop(
        val routeIndex: Int,
    ) : WizardEvent

    data class MakeDirect(
        val home: Boolean,
    ) : WizardEvent

    data class Continue(
        val step: WizardStepKey,
    ) : WizardEvent

    data object Back : WizardEvent

    data object RequestClose : WizardEvent

    data object ConfirmDiscard : WizardEvent

    data object DismissDiscard : WizardEvent

    data class Query(
        val text: String,
    ) : WizardEvent

    data object RetrySearch : WizardEvent
}
