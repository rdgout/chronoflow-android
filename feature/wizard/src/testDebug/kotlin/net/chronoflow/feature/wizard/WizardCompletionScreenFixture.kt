package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.wizard.ui.WizardUiState
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

internal fun completionBoard(
    board: String,
    locale: Locale = Locale.ENGLISH,
): WizardUiState {
    lateinit var state: WizardUiState
    runTest(timeout = 60.seconds) { state = boardState(board, locale) }
    return state
}
