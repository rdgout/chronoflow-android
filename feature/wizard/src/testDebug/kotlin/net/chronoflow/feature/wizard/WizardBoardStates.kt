package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.TestScope
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.showsBack
import net.chronoflow.feature.wizard.debug.FixedAirportSearch
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.WizardUiState
import java.util.Locale

internal val wizardBoardProfile =
    DeviceProfiles.pixel8.copy(
        name = "wizardBoard",
        widthDp = 390,
        heightDp = 844,
        density = 3f,
        fontScale = 1f,
        insets = SafeInsetsDp(0f, 0f, 0f, 0f),
    )

enum class SearchBoardState { Hint, Searching, Empty, Failed, Throttled }

internal fun TestScope.boardState(
    board: String,
    locale: Locale,
    search: SearchBoardState? = null,
): WizardUiState {
    val draft = WizardBoardCaptureSeed.draft(board, locale)
    val model = MemberWizardModel(draft, WizardMemoryDraftStore(), backgroundScope, { WizardBoardCaptureSeed.now })
    try {
        val presentation = WizardBoardCaptureSeed.presentation(board)
        val active = TripWizardUiState.Active(model.snapshots.value, model.progress, model.navigationPath)
        val ui = WizardUiState.from(active, dialog = presentation.discardDialogVisible)
        val sheet = ui.sheet
        val results =
            sheet
                ?.let {
                    FixedAirportSearch(board)
                        .results(presentation.query.orEmpty())
                        .filter { airport -> model.airportIsAllowed(airport.iata, it.sheet) }
                        .map(AirportRowUi::from)
                }.orEmpty()
        val searchUi =
            when (search) {
                SearchBoardState.Hint -> AirportSearchUi.Hint
                SearchBoardState.Searching -> AirportSearchUi.Searching
                SearchBoardState.Empty -> AirportSearchUi.Empty
                SearchBoardState.Failed -> AirportSearchUi.Failed(null)
                SearchBoardState.Throttled -> AirportSearchUi.Failed(WizardBoardCaptureSeed.now.plusSeconds(60))
                null -> if (sheet == null) AirportSearchUi.Hint else AirportSearchUi.Results(results)
            }
        val query =
            when (search) {
                SearchBoardState.Hint -> ""
                SearchBoardState.Empty -> "Tokio Nrita"
                else -> presentation.query
            }
        return ui.copy(
            chrome = ui.chrome.copy(showBack = model.showsBack),
            sheet = sheet?.copy(seedQuery = query),
            search = searchUi,
            retryEnabled = search != SearchBoardState.Throttled,
        )
    } finally {
        model.close()
    }
}
