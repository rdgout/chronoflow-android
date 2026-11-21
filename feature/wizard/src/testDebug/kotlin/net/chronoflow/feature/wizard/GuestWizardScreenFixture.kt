package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.allowsSleepOverride
import net.chronoflow.core.wizard.model.checkingTitle
import net.chronoflow.core.wizard.model.exclusionWasSaved
import net.chronoflow.core.wizard.model.firstScreen
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.preference
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.showsBack
import net.chronoflow.core.wizard.model.sleepComplete
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.ui.WizardUiState
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

internal fun TestScope.guestBoardState(
    board: String,
    locale: Locale,
): WizardUiState {
    val draft = GuestWizardBoardSeed.draft(MemberWizardDrafts.guest(), board, locale)
    val model = MemberWizardModel(draft, InMemoryWizardDraftStore(), backgroundScope, { WizardBoardCaptureSeed.now })
    try {
        val active =
            TripWizardUiState.Active(
                model.snapshots.value,
                model.progress,
                model.navigationPath,
                sleepComplete = model.sleepComplete,
                exclusionWasSaved = model.exclusionWasSaved,
                hasChanges = model.hasChanges,
                firstScreen = model.firstScreen,
                optionalPrompts = model.optionalPrompts,
                checkingTitle = model.checkingTitle,
                allowsSleepOverride = model.allowsSleepOverride,
                preferences = listOf("exclude_caffeine", "flight_movement_breaks").associateWith(model::preference),
            )
        val ui =
            WizardUiState.from(
                active,
                dialog = GuestWizardBoardSeed.presentation(board).discardDialogVisible,
                now = WizardBoardCaptureSeed.now,
                uiOverride = GuestWizardBoardSeed.uiOverride(board),
                language = Language.of(locale.language),
            )
        return ui.copy(
            chrome =
                ui.chrome.copy(
                    showBack =
                        if (ui.checking ==
                            null
                        ) {
                            model.showsBack
                        } else {
                            ui.chrome.showBack
                        },
                ),
        )
    } finally {
        model.close()
    }
}

internal fun guestBoard(
    board: String,
    locale: Locale = Locale.ENGLISH,
): WizardUiState {
    lateinit var result: WizardUiState
    runTest(timeout = 60.seconds) { result = guestBoardState(board, locale) }
    return result
}
