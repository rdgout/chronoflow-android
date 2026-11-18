package net.chronoflow.feature.auth.verify

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class VerifyCopyTest(
    private val language: String,
) {
    @Test fun generatedCopyFormatsTheCountdownEmailAndSignOutFailures() {
        RuntimeEnvironment.setQualifiers(language)
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Je kunt de link over 0:42 opnieuw versturen."
            } else {
                "You can resend in 0:42."
            },
            resources.getString(R.string.auth_you_can_resend_in_x, countdownArgument(42)),
        )
        assertEquals("10:00", countdownArgument(600))
        assertEquals(
            if (language ==
                "nl"
            ) {
                "We hebben een link naar rick@example.com gestuurd. Open die op deze telefoon om het af te ronden."
            } else {
                "We sent a link to rick@example.com. Open it on this phone to finish."
            },
            resources.getString(R.string.auth_we_sent_a_link_to_x_open_it_on_this_694644, "rick@example.com"),
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Verbinding nodig"
            } else {
                "Needs a connection"
            },
            resources.getString(signOutErrorResource(DataFailure.Offline)),
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Er ging iets mis"
            } else {
                "Something went wrong"
            },
            resources.getString(signOutErrorResource(DataFailure.Server(503))),
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf("en", "nl")
    }
}
