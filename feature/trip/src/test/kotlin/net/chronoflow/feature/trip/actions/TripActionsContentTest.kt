package net.chronoflow.feature.trip.actions

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class TripActionsContentTest {
    @get:Rule val compose = createComposeRule()
    private var rig: TripPageTestRig? = null

    @After fun close() {
        rig?.close()
    }

    @Test fun offlineDisablesBothWithNeedsConnection() {
        compose.setContent { ChronoFlowTheme { TripActionsContent(actionsUi("offline"), {}, {}) } }
        compose.onNodeWithTag("trip.actions.edit").assertIsNotEnabled()
        compose.onNodeWithTag("trip.actions.duplicate").assertIsNotEnabled()
        compose.onNodeWithTag("trip.actions.edit.subtitle", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("trip.actions.duplicate.subtitle", useUnmergedTree = true).assertExists()
    }

    @Test fun onlineEnabledDuplicateShowsSubtitle() {
        compose.setContent { ChronoFlowTheme { TripActionsContent(actionsUi(), {}, {}) } }
        compose.onNodeWithTag("trip.actions.edit").assertIsEnabled()
        compose.onNodeWithTag("trip.actions.duplicate").assertIsEnabled()
        compose.onNodeWithTag("trip.actions.duplicate.subtitle", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("trip.actions.edit.subtitle", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun cardFlagFalseDisablesOnlyThatAction() {
        val ui = actionsUi().let { it.copy(availability = it.availability.copy(canEdit = false)) }
        compose.setContent { ChronoFlowTheme { TripActionsContent(ui, {}, {}) } }
        compose.onNodeWithTag("trip.actions.edit").assertIsNotEnabled()
        compose.onNodeWithTag("trip.actions.duplicate").assertIsEnabled()
    }

    private fun navigate(edit: Boolean) {
        val page =
            TripPageTestRig("S1", wizardPreparer = TripWizardPreparer { _, _ -> TripWizardPrepareOutcome.Ready }).also {
                rig =
                    it
            }
        val snapshot = page.source.snapshot.value
        val card = LargeTimelineFixtures.fixture("S1").card.copy(canEdit = true, canDuplicate = true)
        page.source.snapshot.value =
            snapshot.copy(
                trip =
                    snapshot.trip!!.copy(
                        card = ApiJson.response.encodeToString(TripCardView.serializer(), card),
                    ),
            )
        val received = mutableListOf<WizardMode>()
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TripPage(page.vm, {}, null, onWizard = {
                        assertNull(page.vm.sheet.value)
                        received.add(it)
                    })
                }
            }
        }
        compose.onNodeWithTag("trip.actions.open").performClick()
        compose.onNodeWithTag(if (edit) "trip.actions.edit" else "trip.actions.duplicate").performClick()
        compose.waitForIdle()
        assertEquals(
            listOf(if (edit) WizardMode.Edit(page.vm.args.tripId) else WizardMode.Duplicate(page.vm.args.tripId)),
            received,
        )
        compose.onNodeWithTag("trip.actions").assertDoesNotExist()
    }

    @Test fun editClickNavigatesToWizardSeam() = navigate(true)

    @Test fun duplicateClickNavigatesToWizardSeam() = navigate(false)

    @Test fun noCardDisablesOpenButton() {
        val page = TripPageTestRig("S1").also { rig = it }
        compose.setContent { ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme { TripPage(page.vm, {}, null) } } }
        compose.onNodeWithTag("trip.actions.open").assertIsNotEnabled()
    }
}
