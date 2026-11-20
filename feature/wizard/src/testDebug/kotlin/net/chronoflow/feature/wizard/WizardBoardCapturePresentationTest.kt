@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class WizardBoardCapturePresentationTest {
    @get:Rule val main: TestWatcher = WizardMainDispatcherRule()

    @Test fun theSeedQueryReachesTheSheetThroughCreationExtras() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeWizardDependencies(this).apply {
                    draft =
                        WizardBoardCaptureSeed.draft("WizardStopSearch", Locale.ENGLISH)
                }
            val fixture =
                WizardViewModelFixture(deps, WizardMode.Create, WizardBoardCaptureSeed.presentation("WizardStopSearch"))
            try {
                runCurrent()
                assertEquals(
                    "Du",
                    fixture.model.uiState.value!!
                        .sheet!!
                        .seedQuery,
                )
                assertEquals(deps.draft, fixture.model.wizard!!.draft)
            } finally {
                withContext(NonCancellable) {
                    fixture.clear()
                    deps.close()
                }
            }
        }

    @Test fun theSeedDiscardAlertReachesOnlyThePresentationState() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeWizardDependencies(this).apply {
                    draft =
                        WizardBoardCaptureSeed.draft("WizardDiscard", Locale.ENGLISH)
                }
            val fixture =
                WizardViewModelFixture(deps, WizardMode.Create, WizardBoardCaptureSeed.presentation("WizardDiscard"))
            try {
                runCurrent()
                assertTrue(
                    fixture.model.uiState.value!!
                        .discardDialogVisible,
                )
                assertEquals(deps.draft, fixture.model.wizard!!.draft)
            } finally {
                withContext(NonCancellable) {
                    fixture.clear()
                    deps.close()
                }
            }
        }
}
