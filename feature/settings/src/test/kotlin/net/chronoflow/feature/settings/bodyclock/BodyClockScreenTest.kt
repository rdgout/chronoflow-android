package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal fun bodyClockUi(
    body: String = DEFAULT_BODY,
    readiness: String? = MISSING_BODY_READINESS,
    saved: Set<BodyClockGroup> = emptySet(),
): BodyClockUiState {
    val editor = SettingsBodyClockModel.State(ProfilePreferences.bodyClock(json(body)), saved = saved)
    val value = readiness?.let(::json)
    return BodyClockUiState(
        pages = SettingsPagesState(settingsLoaded = true),
        editor = editor,
        sections = BodyClockViewModel.sections(editor, BodyClockReadiness.missingFields(value).toSet()),
        readiness = BodyClockReadiness.of(value),
    )
}

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyClockScreenTest : SettingsScreenTest() {
    private fun showBody(
        ui: BodyClockUiState = bodyClockUi(),
        onEvent: (BodyClockEvent) -> Unit = {},
    ) {
        val rating =
            TextFieldState(
                ui.editor
                    ?.answers
                    ?.get("blueBlockerMdfd")
                    .stringValue()
                    .orEmpty(),
            )
        show { BodyClockScreen(BodyClockScreenState(ui, rating), onEvent) }
    }

    @Test fun missingBadgesAppearOnAboutAndSafetyQuestions() {
        showBody()
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(2)
        compose.onNodeWithTag("settings.bodyClock.ageBand.missing", true).assertExists()
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(6)
        compose.onNodeWithTag("settings.bodyClock.shiftWork.missing", true).assertExists()
    }

    @Test fun firstMissingButtonEmitsItsOwnEvent() {
        val events = mutableListOf<BodyClockEvent>()
        showBody(onEvent = { events += it })
        compose.onNodeWithTag("settings.bodyClock.firstMissing").performClick()
        assertEquals(listOf(BodyClockEvent.FirstMissing), events)
    }

    @Test fun scrollRequestBringsTheQuestionIntoViewAndAcknowledgesItsId() {
        val events = mutableListOf<BodyClockEvent>()
        showBody(
            bodyClockUi().copy(scroll = BodyClockScroll(41, BodyClockGroup.About, "ageBand")),
            { events += it },
        )
        compose.onNodeWithTag("settings.bodyClock.ageBand").assertIsDisplayed()
        assertEquals(listOf(BodyClockEvent.ScrollHandled(41)), events)
    }

    @Test fun declaredScheduleShowsBothFreeDayTiles() {
        showBody(bodyClockUi("""{"freeDayStatus":"declared","freeDayBed":"23:00","freeDayWake":"07:00"}"""))
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(3)
        compose.onNodeWithTag("settings.bodyClock.freeDayBed").assertExists()
        compose.onNodeWithTag("settings.bodyClock.freeDayWake").assertExists()
    }

    @Test fun successfulSaveShowsItsSectionLabel() {
        showBody(bodyClockUi(readiness = null, saved = setOf(BodyClockGroup.About)))
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(1)
        compose.onNodeWithTag("settings.bodyClock.saved.about").assertExists()
    }

    @Test fun excludedReadinessShowsItsLines() {
        showBody(bodyClockUi(readiness = """{"variant":"excluded","exclusions":[{"key":"unknown exclusion"}]}"""))
        compose.onNodeWithTag("settings.bodyClock.exclusion.0", true).assertExists()
    }

    @Test fun adjustedReadinessShowsItsLines() {
        showBody(bodyClockUi(readiness = """{"variant":"adjusted","adjustments":[{"key":"unknown adjustment"}]}"""))
        compose.onNodeWithTag("settings.bodyClock.adjustment.0", true).assertExists()
    }

    @Test fun noticesArePresentBesideLightEquipmentAndSafetyChecks() {
        showBody()
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(5)
        compose.onNodeWithTag("settings.bodyClock.lightNotice", true).assertExists()
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(6)
        compose.onNodeWithTag("settings.bodyClock.audienceNotice", true).assertExists()
    }

    @Test fun openTimeSheetUsesTheSharedPickerWindow() {
        showBody(bodyClockUi().copy(timeSheet = BodyClockTimeSheet("habitualBed", 23, 0)))
        compose.onNodeWithTag("settings.bodyClock.time-sheet", true).assertExists()
    }

    @Test fun noSectionShowsStoredState() {
        showBody(BodyClockUiState(pages = SettingsPagesState(settingsLoaded = true)))
        compose.onNodeWithTag("settings.missing").assertExists()
    }
}
