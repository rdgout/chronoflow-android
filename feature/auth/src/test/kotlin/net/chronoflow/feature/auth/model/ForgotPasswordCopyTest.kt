package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.shell.resolve
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class ForgotPasswordCopyTest(
    private val language: String,
    private val expected: String,
    private val throttle: String,
) {
    @Test fun noticeUsesTypedInterpolationInEnglishAndDutch() {
        RuntimeEnvironment.setQualifiers(language)
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals(
            expected,
            resources.getString(R.string.auth_if_x_has_an_account_a_link_to_choose_484150, "person+100%@example.com"),
        )
        val form = AuthFormModel(setOf("email"), AuthFormContext.Forgot)
        form.receive(DataFailure.Throttled(73.0, 429))
        assertEquals(throttle, form.takeAnnouncement { it.resolve(resources) })
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf(
                    "en",
                    "If person+100%@example.com has an account, a link to choose a new password is on its way.",
                    "Too many requests. Wait a few minutes before you ask for another link.",
                ),
                arrayOf(
                    "nl",
                    "Als er een account bestaat voor person+100%@example.com, is er een link onderweg om een nieuw wachtwoord te kiezen.",
                    "Te veel verzoeken. Wacht een paar minuten voordat je een nieuwe link aanvraagt.",
                ),
            )
    }
}
