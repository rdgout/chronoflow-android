@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class WizardSearchCopyTest(
    private val language: String,
    private val id: Int,
    private val expected: String?,
) {
    @Test fun resolvesTheSpecifiedResource() {
        RuntimeEnvironment.setQualifiers(language)
        val value = RuntimeEnvironment.getApplication().resources.getString(id, 15, 15)
        assertTrue(value.isNotBlank())
        expected?.let { assertEquals(it, value) }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}:{0}:{1}:{2}")
        fun cases(): List<Array<out Any?>> =
            listOf(
                arrayOf<Any?>("en", R.string.android_wizard_airport_field, "Airport or city"),
                arrayOf<Any?>("nl", R.string.android_wizard_airport_field, "Luchthaven of stad"),
                arrayOf<Any?>("en", R.string.android_wizard_search_hint, "Type at least 2 letters to search."),
                arrayOf<Any?>("nl", R.string.android_wizard_search_hint, "Typ minstens 2 letters om te zoeken."),
                arrayOf<Any?>("en", R.string.android_wizard_search_searching, "Searching airports…"),
                arrayOf<Any?>("nl", R.string.android_wizard_search_searching, "Luchthavens zoeken…"),
                arrayOf<Any?>("en", R.string.android_wizard_search_empty_title, "No airports found"),
                arrayOf<Any?>("nl", R.string.android_wizard_search_empty_title, "Geen luchthavens gevonden"),
                arrayOf<Any?>(
                    "en",
                    R.string.android_wizard_search_empty_body,
                    "Try the city name, like “Tokyo”, or the 3-letter code on your ticket, like “NRT”.",
                ),
                arrayOf<Any?>(
                    "nl",
                    R.string.android_wizard_search_empty_body,
                    "Probeer de naam van de stad, zoals “Tokio”, of de code van 3 letters op je ticket, zoals “NRT”.",
                ),
                arrayOf<Any?>("en", R.string.android_wizard_search_error_title, "We couldn't search airports."),
                arrayOf<Any?>("nl", R.string.android_wizard_search_error_title, "We konden geen luchthavens zoeken."),
                arrayOf<Any?>("en", R.string.android_wizard_search_error_body, "Check your connection and try again."),
                arrayOf<Any?>(
                    "nl",
                    R.string.android_wizard_search_error_body,
                    "Controleer je verbinding en probeer het opnieuw.",
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "New trip")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "New trip")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Close")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Close")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Continue")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Continue")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Progress, {{pct}} percent")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Progress, {{pct}} percent")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where are you flying?")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where are you flying?")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Pick the airports from your booking.")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Pick the airports from your booking.")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "From")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "From")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "To")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "To")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Stop")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Stop")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Stop {{number}}")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Stop {{number}}")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Choose an airport")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Choose an airport")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "From your last trip")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "From your last trip")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(
                        CopyIndex.find(CopyTable.Wizard, "From and To are the same airport. Choose where you land."),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(
                        CopyIndex.find(CopyTable.Wizard, "From and To are the same airport. Choose where you land."),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Swap airports")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Swap airports")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Do you change planes on the way?")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Do you change planes on the way?")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "No, direct")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "No, direct")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Yes")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Yes")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Add another stop")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Add another stop")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "That's the most we can plan: 16 flights.")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "That's the most we can plan: 16 flights.")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Remove stop in {{city}}")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Remove stop in {{city}}")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Your flight home")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Your flight home")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(
                        CopyIndex.find(CopyTable.Wizard, "It leaves from where you landed. Add any stops on the way."),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(
                        CopyIndex.find(CopyTable.Wizard, "It leaves from where you landed. Add any stops on the way."),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where you landed")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where you landed")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "Leaves from {{city}}, where you landed. You can't change this.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "Leaves from {{city}}, where you landed. You can't change this.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Between {{from}} and {{to}}")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Between {{from}} and {{to}}")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Cancel")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Cancel")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Try again")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Try again")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Discard this trip?")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Discard this trip?")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "What you entered is not saved.")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "What you entered is not saved.")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Keep editing")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Keep editing")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Discard")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Discard")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "ChronoFlow is intended for adults with a regular nightly sleep pattern. It does not offer a plan if you have recently done shift work, or do not usually sleep in one regular nightly episode.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "ChronoFlow is intended for adults with a regular nightly sleep pattern. It does not offer a plan if you have recently done shift work, or do not usually sleep in one regular nightly episode.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "ChronoFlow is not suitable if you have bipolar disorder or have had a manic or hypomanic episode: sleep loss, bright light and long journeys can trigger mood episodes. Talk to your doctor before you travel.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(
                        CopyIndex.find(
                            CopyTable.Wizard,
                            "ChronoFlow is not suitable if you have bipolar disorder or have had a manic or hypomanic episode: sleep loss, bright light and long journeys can trigger mood episodes. Talk to your doctor before you travel.",
                        ),
                    ).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly from?")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly from?")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly to?")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly to?")).resId, null),
                arrayOf<Any?>(
                    "en",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly home to?")).resId,
                    null,
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where do you fly home to?")).resId,
                    null,
                ),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where's your stop?")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where's your stop?")).resId, null),
                arrayOf<Any?>("en", checkNotNull(CopyIndex.find(CopyTable.Auth, "Back")).resId, null),
                arrayOf<Any?>("nl", checkNotNull(CopyIndex.find(CopyTable.Auth, "Back")).resId, null),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Where are you flying?")).resId,
                    "Waar vlieg je heen?",
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Do you change planes on the way?")).resId,
                    "Stap je onderweg over?",
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Discard this trip?")).resId,
                    "Deze reis weggooien?",
                ),
                arrayOf<Any?>(
                    "nl",
                    checkNotNull(CopyIndex.find(CopyTable.Wizard, "Progress, {{pct}} percent")).resId,
                    "Voortgang, 15 procent",
                ),
            )
    }
}
