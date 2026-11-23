package net.chronoflow.feature.wizard

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCheckingProgress
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.checking.CheckFailureKind
import net.chronoflow.feature.wizard.checking.CheckLine
import net.chronoflow.feature.wizard.checking.CheckLineState
import net.chronoflow.feature.wizard.checking.WizardCheckingUi
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun checkingActive(
    ticks: Int = 0,
    answers: Boolean = false,
    guest: Boolean = false,
    failure: DataFailure? = null,
    retry: Int = 0,
    editing: Boolean = false,
): TripWizardUiState.Active {
    val draft =
        routeDraft().let {
            it.copy(
                state =
                    it.state.copy(
                        step = MemberWizardStep.checking,
                        checkingAnswers = answers,
                        mode = if (guest) WizardMode.guest else WizardMode.member,
                    ),
            )
        }
    return TripWizardUiState.Active(
        MemberWizardSnapshot(
            draft,
            checking = WizardCheckingProgress(ticks, failure = failure),
            retrySeconds = retry,
        ),
        0.6,
        emptyList(),
        isEditing = editing,
    )
}

class WizardCheckingUiTest {
    @Test fun ticksZero() = assertTicks(0, listOf(CheckLineState.Busy, CheckLineState.Open, CheckLineState.Open))

    @Test fun ticksOne() = assertTicks(1, listOf(CheckLineState.Done, CheckLineState.Busy, CheckLineState.Open))

    @Test fun ticksTwo() = assertTicks(2, listOf(CheckLineState.Done, CheckLineState.Done, CheckLineState.Busy))

    @Test fun ticksThree() = assertTicks(3, listOf(CheckLineState.Done, CheckLineState.Done, CheckLineState.Done))

    private fun assertTicks(
        ticks: Int,
        states: List<CheckLineState>,
    ) {
        val ui = WizardCheckingUi.from(checkingActive(ticks))!!
        assertEquals(states, ui.lines.map { it.second })
        assertEquals(
            listOf(
                CheckLine.FlightsAndTimeZones,
                CheckLine.YourSavedAnswers,
                CheckLine.WhatWeCanTailor,
            ),
            ui.lines.map {
                it.first
            },
        )
        assertEquals("AMS", ui.from)
        assertEquals("NRT", ui.to)
        assertEquals("Checking your trip", ui.title.key)
    }

    @Test fun againOrdersAnswersFirst() {
        val ui = WizardCheckingUi.from(checkingActive(answers = true))!!
        assertEquals(
            listOf(
                CheckLine.YourAnswers,
                CheckLine.FlightsAndTimeZones,
                CheckLine.WhatWeCanTailor,
            ),
            ui.lines.map {
                it.first
            },
        )
        assertEquals("Checking your answers", ui.title.key)
    }

    @Test fun guestUsesYourAnswers() =
        assertEquals(CheckLine.YourAnswers, WizardCheckingUi.from(checkingActive(guest = true))!!.lines[1].first)

    @Test fun editingUsesChangesTitleAndBodies() {
        val offline = WizardCheckingUi.from(checkingActive(failure = DataFailure.Offline, editing = true))!!
        assertEquals("Checking your changes", offline.title.key)
        assertEquals(
            "Connect to the internet and try again. If you close now, your trip stays as it was.",
            offline.failureBody.key,
        )
        val server = WizardCheckingUi.from(checkingActive(failure = DataFailure.Server(500), editing = true))!!
        assertEquals(
            "Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was.",
            server.failureBody.key,
        )
    }

    @Test fun offlineMapsToOffline() = assertFailure(DataFailure.Offline, CheckFailureKind.Offline)

    @Test fun serverMapsToServer() = assertFailure(DataFailure.Server(500), CheckFailureKind.Server)

    @Test fun validationMapsToServer() =
        assertFailure(DataFailure.Validation(mapOf("x" to FieldMessage("error")), 422), CheckFailureKind.Server)

    private fun assertFailure(
        failure: DataFailure,
        kind: CheckFailureKind,
    ) {
        val active = checkingActive(failure = failure)
        assertEquals(kind, WizardCheckingUi.from(active)!!.failure)
        assertEquals("checking", WizardUiState.from(active).chrome.footerId)
        assertNull(WizardUiState.from(checkingActive()).chrome.footerId)
    }

    @Test fun throttleSeconds() = assertThrottle(42, "seconds", 42)

    @Test fun throttleOneSecond() = assertThrottle(1, "seconds", 1)

    @Test fun throttleRoundsMinutesUp() = assertThrottle(61, "minutes", 2)

    @Test fun throttleRoundsHoursUp() = assertThrottle(3601, "hours", 2)

    private fun assertThrottle(
        seconds: Int,
        unit: String,
        count: Int,
    ) {
        val active = checkingActive(failure = DataFailure.Throttled(seconds.toDouble(), 429), retry = seconds)
        val ui = WizardCheckingUi.from(active)!!
        assertEquals(CheckFailureKind.Throttle, ui.failure)
        assertEquals("Too many attempts. Try again in %lld $unit.", ui.failureBody.key)
        assertEquals(count.toString(), ui.failureBody.arguments["count"])
        assertEquals("Try again in {{count}} s", ui.button.key)
        assertEquals(seconds.toString(), ui.button.arguments["count"])
        assertFalse(WizardUiState.from(active).chrome.continueEnabled)
    }

    @Test fun noWaitAllowsRetry() {
        val active = checkingActive(failure = DataFailure.Throttled(0.0, 429))
        val ui = WizardCheckingUi.from(active)!!
        assertEquals("Too many attempts. Please try again later.", ui.failureBody.key)
        assertEquals("Try again", ui.button.key)
        assertTrue(WizardUiState.from(active).chrome.continueEnabled)
    }

    @Test fun lineLabelsCarryStateAndTranslatedArguments() {
        val ui = WizardCheckingUi.from(checkingActive(1))!!
        assertEquals("{{line}}, done", ui.lineLabel(0, "translated").key)
        assertEquals("{{line}}, in progress", ui.lineLabel(1, "translated").key)
        assertEquals("{{line}}", ui.lineLabel(2, "translated").key)
        assertEquals("translated", ui.lineLabel(0, "translated").arguments["line"])
    }
}
